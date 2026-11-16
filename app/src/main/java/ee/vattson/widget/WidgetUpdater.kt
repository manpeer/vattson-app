package ee.vattson.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ee.vattson.data.dataStore
import ee.vattson.data.model.AppTheme
import ee.vattson.data.model.CurrencyPreference
import ee.vattson.data.model.DatePreference
import ee.vattson.data.model.PriceResolution
import ee.vattson.data.repository.EleringRepository
import ee.vattson.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

object WidgetUpdater {

    /**
     * Efficiently updates all widgets using pre-calculated data.
     * Use this when the app state is already known (e.g. from ViewModel).
     */
    suspend fun updateAllWidgets(
        context: Context,
        priceText: String,
        cheapestText: String,
        nextPricesStr: String,
        todayPricesStr: String = "",
        widgetTheme: AppTheme = AppTheme.SYSTEM,
        todayCheapestStart: Int = -1,
        todayCheapestDuration: Int = 0
    ) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val widgetClasses = listOf(
                PriceWidget1x1::class.java,
                PriceWidget2x1::class.java,
                PriceWidget3x1::class.java,
                PriceWidget4x1::class.java,
                DonutClockWidget::class.java
            )

            widgetClasses.forEach { widgetClass ->
                val glanceIds = manager.getGlanceIds(widgetClass)
                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[PriceWidget.currentPriceKey] = priceText
                            this[PriceWidget.cheapestWindowKey] = cheapestText
                            this[PriceWidget.nextPricesKey] = nextPricesStr
                            this[PriceWidget.todayPricesKey] = todayPricesStr
                            this[PriceWidget.todayCheapestStartKey] = todayCheapestStart.toString()
                            this[PriceWidget.todayCheapestDurationKey] = todayCheapestDuration.toString()
                            this[PriceWidget.widgetThemeKey] = widgetTheme.name
                        }
                    }
                    // Trigger recomposition
                    val widget = when (widgetClass) {
                        PriceWidget1x1::class.java -> PriceWidget1x1()
                        PriceWidget2x1::class.java -> PriceWidget2x1()
                        PriceWidget3x1::class.java -> PriceWidget3x1()
                        PriceWidget4x1::class.java -> PriceWidget4x1()
                        else -> DonutClockWidget()
                    }
                    widget.update(context, glanceId)
                }
            }

            scheduleNextWidgetUpdate(context)
        } catch (e: Exception) {
            Log.e("WidgetUpdater", "Error updating widgets", e)
        }
    }

    /**
     * Re-calculates and updates all widgets.
     * Use this for system-triggered updates (like onUpdate) or when app state is unknown.
     */
    suspend fun updateAllWidgets(context: Context) {
        try {
            val settingsRepository = SettingsRepository(context.dataStore)
            val eleringRepository = EleringRepository(context)

            // Fetch settings
            val currencyPref = settingsRepository.currencyPreferenceFlow.first()
            val showVat = settingsRepository.showVatFlow.first()
            val resolution = settingsRepository.priceResolutionFlow.first()
            val datePref = settingsRepository.datePreferenceFlow.first()
            val duration = settingsRepository.cheapestWindowDurationFlow.first()
            val lastFetch = settingsRepository.lastFetchTimestampFlow.first()
            val widgetTheme = settingsRepository.widgetThemeFlow.first()

            // Fetch data (from cache first)
            var cachedPrices = eleringRepository.getCachedPrices()

            // Smart Refresh: Check if we need to hit the network in the background
            val now = Instant.now()
            val oneHourAgo = now.minusSeconds(3600).toEpochMilli()
            val isStale = lastFetch < oneHourAgo

            val localZone = ZoneId.systemDefault()
            val nowLocal = LocalDate.now(localZone)
            val nowTime = LocalTime.now(localZone)
            val needsTomorrow = nowTime.hour >= 15 && cachedPrices.none {
                it.timestamp.atZone(localZone).toLocalDate() == nowLocal.plusDays(1)
            }
            val hasNowPrice = EleringRepository.hasPriceForNow(cachedPrices, now)

            if (cachedPrices.isEmpty() || !hasNowPrice || isStale || needsTomorrow) {
                try {
                    val freshPrices = eleringRepository.getPricesForTodayAndTomorrow()
                    if (freshPrices.isNotEmpty()) {
                        cachedPrices = freshPrices
                        settingsRepository.updateLastFetchTimestamp(System.currentTimeMillis())
                    }
                } catch (e: Exception) {
                    Log.e("WidgetUpdater", "Failed to refresh prices in background", e)
                }
            }

            if (cachedPrices.isEmpty()) return

            val activePrices = if (resolution == PriceResolution.HOUR_1) {
                eleringRepository.getHourlyAverages(cachedPrices)
            } else {
                cachedPrices
            }

            // Calculate current price
            val stepSeconds = if (resolution == PriceResolution.HOUR_1) 3600L else 900L
            val currentIndex = activePrices.indexOfFirst { p ->
                val start = p.timestamp
                val end = p.timestamp.plusSeconds(stepSeconds)
                !now.isBefore(start) && now.isBefore(end)
            }
            val current = if (currentIndex != -1) activePrices[currentIndex] else null

            // Format price string
            val priceText = if (current != null) {
                val priceValue = current.resolvedPrice(currencyPref, showVat)
                val suffix = if (currencyPref == CurrencyPreference.SENT_KWH) " s/kWh" else " €/MWh"
                "%.2f%s".format(priceValue, suffix).replace(".", ",")
            } else {
                "--"
            }

            // Calculate cheapest window
            val summaryPrices = activePrices.filter { price ->
                val date = price.localDateTime.toLocalDate()
                when (datePref) {
                    DatePreference.TODAY -> date == nowLocal
                    DatePreference.TOMORROW -> date == nowLocal.plusDays(1)
                    DatePreference.BOTH -> date == nowLocal || date == nowLocal.plusDays(1)
                }
            }

            val windowSize = if (resolution == PriceResolution.HOUR_1) duration else duration * 4
            var cheapestText = "--"
            if (summaryPrices.size >= windowSize) {
                var minWindowSum = Double.MAX_VALUE
                var cheapestStart: Int? = null
                for (i in 0..summaryPrices.size - windowSize) {
                    var windowSum = 0.0
                    for (j in i until (i + windowSize)) {
                        val p = summaryPrices[j]
                        val pVal = p.resolvedPrice(currencyPref, showVat)
                        windowSum += pVal
                    }
                    if (windowSum < minWindowSum) {
                        minWindowSum = windowSum
                        cheapestStart = i
                    }
                }

                cheapestStart?.let { startIdx ->
                    val timeFormatter = DateTimeFormatter.ofPattern("HH")
                    val start = summaryPrices[startIdx].localDateTime
                    val lastItem = summaryPrices[startIdx + windowSize - 1]
                    val end = if (lastItem.resolution == PriceResolution.HOUR_1) {
                        lastItem.localDateTime.plusHours(1)
                    } else {
                        lastItem.localDateTime.plusMinutes(15)
                    }
                    cheapestText = "${timeFormatter.format(start)}-${timeFormatter.format(end)}"
                }
            }

            // Get next 4 prices
            val nextPrices = activePrices.filter { it.timestamp.isAfter(now) || (current != null && it.timestamp == current.timestamp) }
                .take(4)
                .map { p -> p.resolvedPrice(currencyPref, showVat) }
            val nextPricesStr = nextPrices.joinToString(",") { "%.2f".format(Locale.US, it) }

            // Get today's 24 hourly prices for Clock Widget (Always use hourly resolution for the donut)
            val hourlyAverages = eleringRepository.getHourlyAverages(cachedPrices)
            val todayPrices = hourlyAverages.filter { it.localDateTime.toLocalDate() == nowLocal }
                .take(24)
            
            var todayCheapestStart = -1
            if (todayPrices.size >= duration) {
                var minSum = Double.MAX_VALUE
                for (i in 0..todayPrices.size - duration) {
                    var sum = 0.0
                    for (j in i until (i + duration)) {
                        val p = todayPrices[j]
                        val v = p.resolvedPrice(currencyPref, showVat)
                        sum += v
                    }
                    if (sum < minSum) {
                        minSum = sum
                        todayCheapestStart = i
                    }
                }
            }

            val todayPricesStr = todayPrices.map { p -> p.resolvedPrice(currencyPref, showVat) }
                .joinToString(",") { "%.2f".format(Locale.US, it) }

            // Delegate to the efficient update method
            updateAllWidgets(context, priceText, cheapestText, nextPricesStr, todayPricesStr, widgetTheme, todayCheapestStart, duration)
        } catch (e: Exception) {
            Log.e("WidgetUpdater", "Error updating widgets", e)
        }
    }

    /**
     * Schedules a precise update at the next top of the hour.
     */
    fun scheduleNextWidgetUpdate(context: Context) {
        try {
            val now = LocalDateTime.now()
            val nextHour = now.plusHours(1).withMinute(0).withSecond(2).withNano(0)
            val delayMillis = Duration.between(now, nextHour).toMillis().coerceAtLeast(1000L)

            val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "NextHourWidgetUpdateWork",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Exception) {
            Log.e("WidgetUpdater", "Failed to schedule next widget update", e)
        }
    }
}