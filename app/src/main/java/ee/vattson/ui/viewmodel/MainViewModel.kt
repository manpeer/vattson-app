package ee.vattson.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.vattson.data.model.AppTheme
import ee.vattson.data.model.CurrencyPreference
import ee.vattson.data.model.DatePreference
import ee.vattson.data.model.ElectricityPrice
import ee.vattson.data.model.GraphType
import ee.vattson.data.model.NotificationType
import ee.vattson.data.model.PriceNotification
import ee.vattson.data.model.PriceResolution
import ee.vattson.data.repository.EleringRepository
import ee.vattson.data.repository.SettingsRepository
import ee.vattson.util.PriceNotificationManager
import ee.vattson.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

data class SummaryData(
    val minPriceItem: ElectricityPrice?,
    val maxPriceItem: ElectricityPrice?,
    val avgPrice: Double?,
    val cheapestWindowStart: Int?,
    val cheapestWindowSize: Int,
    val cheapestWindowTimeString: String? = null,
    val cheapestWindowAvgPrice: Double? = null,
    val dateString: String? = null
)

data class MainUiState(
    val currentPrice: ElectricityPrice? = null,
    val nextPrice: ElectricityPrice? = null,
    val minPriceItemSummary: ElectricityPrice? = null,
    val maxPriceItemSummary: ElectricityPrice? = null,
    val avgPriceSummary: Double? = null,
    val todaySummary: SummaryData? = null,
    val tomorrowSummary: SummaryData? = null,
    val currencyPreference: CurrencyPreference = CurrencyPreference.SENT_KWH,
    val showVat: Boolean = true,
    val priceResolution: PriceResolution = PriceResolution.HOUR_1,
    val datePreference: DatePreference = DatePreference.TODAY,
    val selectedGraphType: GraphType = GraphType.BAR,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val widgetTheme: AppTheme = AppTheme.SYSTEM,
    val chartData: List<ElectricityPrice> = emptyList(),
    val todayChartData: List<ElectricityPrice> = emptyList(),
    val cheapestWindowStarts: List<Int> = emptyList(),
    val cheapestWindowSize: Int = 4,
    val notificationsEnabled: Boolean = false,
    val priceNotifications: List<PriceNotification> = emptyList(),
    val isChartFullscreen: Boolean = false,
    val lastFetchTimestamp: Long = 0L,
    val isLoading: Boolean = false,
    val isSettingsLoaded: Boolean = false,
    val error: String? = null
)

class MainViewModel(
    private val application: Application,
    private val eleringRepository: EleringRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _allPrices = MutableStateFlow<List<ElectricityPrice>>(emptyList())
    private val _hourlyAverages = MutableStateFlow<List<ElectricityPrice>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _isChartFullscreen = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _currentTime = MutableStateFlow(Instant.now())

    private val _settingsState: StateFlow<SettingsState> = combine(
        settingsRepository.currencyPreferenceFlow,
        settingsRepository.showVatFlow,
        settingsRepository.priceResolutionFlow,
        settingsRepository.datePreferenceFlow,
        settingsRepository.graphTypeFlow,
        settingsRepository.appThemeFlow,
        settingsRepository.widgetThemeFlow,
        settingsRepository.cheapestWindowDurationFlow,
        settingsRepository.notificationsEnabledFlow,
        settingsRepository.priceNotificationsFlow,
        settingsRepository.lastFetchTimestampFlow
    ) { arr: Array<Any?> ->
        SettingsState(
            currencyPreference = arr[0] as CurrencyPreference,
            showVat = arr[1] as Boolean,
            priceResolution = arr[2] as PriceResolution,
            datePreference = arr[3] as DatePreference,
            graphType = arr[4] as GraphType,
            appTheme = arr[5] as AppTheme,
            widgetTheme = arr[6] as AppTheme,
            cheapestWindowDuration = arr[7] as Int,
            notificationsEnabled = arr[8] as Boolean,
            priceNotifications = arr[9] as List<PriceNotification>,
            lastFetchTimestamp = arr[10] as Long,
            isLoaded = true
        )
    }.distinctUntilChanged().flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState(
            CurrencyPreference.SENT_KWH, true, PriceResolution.HOUR_1,
            DatePreference.TODAY, GraphType.BAR, AppTheme.SYSTEM, AppTheme.SYSTEM, 4, true,
            emptyList(), 0L
        )
    )

    private val _activePrices: StateFlow<List<ElectricityPrice>> = combine(
        _allPrices,
        _hourlyAverages,
        _settingsState.map { it.priceResolution }.distinctUntilChanged()
    ) { all, hourly, res ->
        if (res == PriceResolution.HOUR_1) hourly else all
    }.distinctUntilChanged().flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val uiState: StateFlow<MainUiState> = combine(
        _activePrices,
        _isLoading,
        _isChartFullscreen,
        _error,
        _settingsState,
        _currentTime
    ) { arr: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val activePrices = arr[0] as List<ElectricityPrice>
        val isLoading = arr[1] as Boolean
        val isChartFullscreen = arr[2] as Boolean
        val error = arr[3] as String?
        val settings = arr[4] as SettingsState
        val now = arr[5] as Instant

        val nowLocal = LocalDate.now()
        val summaryPrices = activePrices.asSequence().filter { price ->
            val date = price.localDateTime.toLocalDate()
            when (settings.datePreference) {
                DatePreference.TODAY -> date == nowLocal
                DatePreference.TOMORROW -> date == nowLocal.plusDays(1)
                DatePreference.BOTH -> date == nowLocal || date == nowLocal.plusDays(1)
            }
        }.toList()

        calculateUiState(summaryPrices, activePrices, isLoading, isChartFullscreen, error, settings, now)
    }
    .distinctUntilChanged()
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(isLoading = true)
    )

    private fun calculateUiState(
        summaryPrices: List<ElectricityPrice>,
        activePrices: List<ElectricityPrice>,
        isLoading: Boolean,
        isChartFullscreen: Boolean,
        error: String?,
        settings: SettingsState,
        now: Instant
    ): MainUiState {
        val currencyPref = settings.currencyPreference
        val showVat = settings.showVat
        val resolution = settings.priceResolution
        val graphType = settings.graphType
        val appTheme = settings.appTheme
        val widgetTheme = settings.widgetTheme
        val duration = settings.cheapestWindowDuration
        val notificationsEnabled = settings.notificationsEnabled
        val priceNotifications = settings.priceNotifications
        val datePref = settings.datePreference
        val isSettingsLoaded = settings.isLoaded

        val stepSeconds = if (resolution == PriceResolution.HOUR_1) 3600L else 900L
        val currentIndex = activePrices.indexOfFirst { p ->
            val start = p.timestamp
            val end = p.timestamp.plusSeconds(stepSeconds)
            !now.isBefore(start) && now.isBefore(end)
        }
        val current = if (currentIndex != -1) activePrices[currentIndex] else null
        val next = if (currentIndex != -1 && currentIndex + 1 < activePrices.size) {
            activePrices[currentIndex + 1]
        } else null

        val windowSize = if (resolution == PriceResolution.HOUR_1) duration else duration * 4

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun getSummaryData(prices: List<ElectricityPrice>): SummaryData? {
            if (prices.isEmpty()) return null
            val minItem = prices.minByOrNull { it.resolvedPrice(currencyPref, showVat) }
            val maxItem = prices.maxByOrNull { it.resolvedPrice(currencyPref, showVat) }
            val avg = prices.map { it.resolvedPrice(currencyPref, showVat) }.average()

            var cheapestStart: Int? = null
            var cheapestAvg: Double? = null
            if (prices.size >= windowSize) {
                var minWindowSum = Double.MAX_VALUE
                for (i in 0..prices.size - windowSize) {
                    var windowSum = 0.0
                    for (j in i until (i + windowSize)) {
                        val p = prices[j]
                        windowSum += p.resolvedPrice(currencyPref, showVat)
                    }
                    if (windowSum < minWindowSum) {
                        minWindowSum = windowSum
                        cheapestStart = i
                        cheapestAvg = windowSum / windowSize
                    }
                }
            }

            val windowTimeString = if (cheapestStart != null && cheapestStart in prices.indices && (cheapestStart + windowSize - 1) in prices.indices) {
                val start = prices[cheapestStart].localDateTime
                val lastItem = prices[cheapestStart + windowSize - 1]
                val end = if (lastItem.resolution == PriceResolution.HOUR_1) {
                    lastItem.localDateTime.plusHours(1)
                } else {
                    lastItem.localDateTime.plusMinutes(15)
                }
                "${timeFormatter.format(start)}-${timeFormatter.format(end)}"
            } else null

            val dateStr = if (prices.isNotEmpty()) {
                val estonianLocale = Locale.forLanguageTag("et-EE")
                val date = prices.first().localDateTime.toLocalDate()
                val dateFormatter = DateTimeFormatter.ofPattern("dd.MM", estonianLocale)
                val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE", estonianLocale)
                val datePart = dateFormatter.format(date)
                val dayPart = dayOfWeekFormatter.format(date)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(estonianLocale) else it.toString() }
                "$datePart $dayPart"
            } else null

            return SummaryData(minItem, maxItem, avg, cheapestStart, windowSize, windowTimeString, cheapestAvg, dateStr)
        }

        val nowLocal = LocalDate.now()
        val todayPrices = activePrices.filter { it.localDateTime.toLocalDate() == nowLocal }.toList()
        val tomorrowPrices = activePrices.filter { it.localDateTime.toLocalDate() == nowLocal.plusDays(1) }.toList()

        val todaySummary = getSummaryData(todayPrices)
        val tomorrowSummary = getSummaryData(tomorrowPrices)

        val overallSummary = getSummaryData(summaryPrices)

        val windowStarts = mutableListOf<Int>()
        if (datePref == DatePreference.BOTH) {
            todaySummary?.cheapestWindowStart?.let { if (it in summaryPrices.indices) windowStarts.add(it) }
            tomorrowSummary?.cheapestWindowStart?.let {
                val idx = todayPrices.size + it
                if (idx in summaryPrices.indices) windowStarts.add(idx)
            }
        } else {
            overallSummary?.cheapestWindowStart?.let { if (it in summaryPrices.indices) windowStarts.add(it) }
        }

        return MainUiState(
            currentPrice = current,
            nextPrice = next,
            minPriceItemSummary = overallSummary?.minPriceItem,
            maxPriceItemSummary = overallSummary?.maxPriceItem,
            avgPriceSummary = overallSummary?.avgPrice,
            todaySummary = todaySummary,
            tomorrowSummary = tomorrowSummary,
            currencyPreference = currencyPref,
            showVat = showVat,
            priceResolution = resolution,
            datePreference = datePref,
            selectedGraphType = graphType,
            appTheme = appTheme,
            widgetTheme = widgetTheme,
            chartData = summaryPrices,
            todayChartData = todayPrices,
            cheapestWindowStarts = windowStarts,
            cheapestWindowSize = windowSize,
            notificationsEnabled = notificationsEnabled,
            priceNotifications = priceNotifications,
            isChartFullscreen = isChartFullscreen,
            lastFetchTimestamp = settings.lastFetchTimestamp,
            isLoading = isLoading,
            isSettingsLoaded = isSettingsLoaded,
            error = error
        )
    }

    private data class SettingsState(
        val currencyPreference: CurrencyPreference,
        val showVat: Boolean,
        val priceResolution: PriceResolution,
        val datePreference: DatePreference,
        val graphType: GraphType,
        val appTheme: AppTheme,
        val widgetTheme: AppTheme,
        val cheapestWindowDuration: Int,
        val notificationsEnabled: Boolean,
        val priceNotifications: List<PriceNotification>,
        val lastFetchTimestamp: Long,
        val isLoaded: Boolean = false
    )

    init {
        loadCachedPrices()
        startClockTicker()

        viewModelScope.launch(Dispatchers.Default) {
            uiState
                .map { state ->
                    val currentPrice = state.currentPrice ?: return@map null

                    val priceValue = currentPrice.resolvedPrice(state.currencyPreference, state.showVat)
                    val suffix = if (state.currencyPreference == CurrencyPreference.SENT_KWH) " s/kWh" else " €/MWh"
                    val priceText = "%.2f%s".format(priceValue, suffix).replace(".", ",")

                    val cheapestText = if (state.cheapestWindowStarts.isNotEmpty() && state.chartData.isNotEmpty()) {
                        val startIdx = state.cheapestWindowStarts.first()
                        val size = state.cheapestWindowSize
                        if (startIdx in state.chartData.indices && (startIdx + size - 1) in state.chartData.indices) {
                            val start = state.chartData[startIdx].localDateTime
                            val lastItem = state.chartData[startIdx + size - 1]
                            val end = if (lastItem.resolution == PriceResolution.HOUR_1) {
                                lastItem.localDateTime.plusHours(1)
                            } else {
                                lastItem.localDateTime.plusMinutes(15)
                            }
                            val formatter = DateTimeFormatter.ofPattern("HH")
                            "${formatter.format(start)}-${formatter.format(end)}"
                        } else "--"
                    } else "--"

                    val now = Instant.now()
                    val nextPricesStr = state.chartData
                        .asSequence()
                        .filter { it.timestamp.isAfter(now) || it.timestamp == currentPrice.timestamp }
                        .take(4)
                        .map { p -> p.resolvedPrice(state.currencyPreference, state.showVat) }
                        .joinToString(",") { "%.2f".format(Locale.US, it) }

                    val todayPricesStr = state.todayChartData
                        .asSequence()
                        .take(if (state.priceResolution == PriceResolution.HOUR_1) 24 else 96)
                        .filterIndexed { index, _ -> state.priceResolution == PriceResolution.HOUR_1 || index % 4 == 0 }
                        .take(24)
                        .map { p -> p.resolvedPrice(state.currencyPreference, state.showVat) }
                        .joinToString(",") { "%.2f".format(Locale.US, it) }

                    val cheapestStart = state.todaySummary?.cheapestWindowStart ?: -1
                    val hourlyCheapestStart = if (cheapestStart != -1 && state.priceResolution == PriceResolution.MINUTE_15) {
                        cheapestStart / 4
                    } else {
                        cheapestStart
                    }
                    val hourlyDuration = state.cheapestWindowSize / (if (state.priceResolution == PriceResolution.HOUR_1) 1 else 4)

                    listOf(priceText, cheapestText, nextPricesStr, todayPricesStr, hourlyCheapestStart.toString(), hourlyDuration.toString())
                }
                .distinctUntilChanged()
                .debounce(1000.milliseconds)
                .collectLatest { data ->
                    if (data != null) {
                        WidgetUpdater.updateAllWidgets(
                            application,
                            data[0],
                            data[1],
                            data[2],
                            data[3],
                            uiState.value.widgetTheme,
                            data[4].toInt(),
                            data[5].toInt()
                        )
                    }
                }
        }
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (isActive) {
                _currentTime.value = Instant.now()
                delay(30_000.milliseconds)
            }
        }
    }

    private fun maybeRefreshPrices(isInitialLoad: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val cachedInMemory = _allPrices.value
            val cached = cachedInMemory.ifEmpty { eleringRepository.getCachedPrices() }
            if (cachedInMemory.isEmpty() && cached.isNotEmpty()) {
                _allPrices.value = cached
                _hourlyAverages.value = eleringRepository.getHourlyAverages(cached)
            }

            val lastFetch = _settingsState.value.lastFetchTimestamp
            val now = Instant.now()
            val oneHourAgo = now.minusSeconds(3600).toEpochMilli()
            val isStale = lastFetch < oneHourAgo
            val hasNowPrice = EleringRepository.hasPriceForNow(cached, now)
            val needsTomorrow = LocalTime.now().hour >= 15 && cached.none {
                it.localDateTime.toLocalDate() == LocalDate.now().plusDays(1)
            }
            if (cached.isEmpty() || !hasNowPrice || isStale || needsTomorrow) {
                refreshPrices(showLoading = cached.isEmpty() && isInitialLoad)
            }
        }
    }

    fun onResume() {
        _currentTime.value = Instant.now()
        maybeRefreshPrices(isInitialLoad = false)
    }

    private fun loadCachedPrices() {
        maybeRefreshPrices(isInitialLoad = true)
    }

    fun refreshPrices(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _isLoading.value = true
            _error.value = null
            try {
                val prices = withContext(Dispatchers.IO) {
                    eleringRepository.getPricesForTodayAndTomorrow()
                }

                _allPrices.value = prices
                _hourlyAverages.value = eleringRepository.getHourlyAverages(prices)

                settingsRepository.updateLastFetchTimestamp(System.currentTimeMillis())

                PriceNotificationManager.checkAndTriggerNotifications(application)

                triggerWidgetUpdate()
            } catch (e: Exception) {
                if (_allPrices.value.isEmpty()) {
                    _error.value = "Viga andmete laadimisel: ${e.message}"
                }
            } finally {
                if (showLoading) _isLoading.value = false
            }
        }
    }

    fun setCurrencyPreference(preference: CurrencyPreference) {
        viewModelScope.launch { settingsRepository.updateCurrencyPreference(preference) }
    }

    fun setShowVat(showVat: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowVat(showVat) }
    }

    fun setPriceResolution(resolution: PriceResolution) {
        viewModelScope.launch { settingsRepository.updatePriceResolution(resolution) }
    }

    fun setDatePreference(pref: DatePreference) {
        viewModelScope.launch {
            settingsRepository.updateDatePreference(pref)
        }
    }

    fun setGraphType(type: GraphType) {
        viewModelScope.launch { settingsRepository.updateGraphType(type) }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.updateAppTheme(theme) }
    }

    fun setWidgetTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.updateWidgetTheme(theme) }
    }

    fun setCheapestWindowDuration(duration: Int) {
        viewModelScope.launch { settingsRepository.updateCheapestWindowDuration(duration) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateNotificationsEnabled(enabled) }
    }

    fun addPriceNotification(threshold: Double, type: NotificationType, unit: CurrencyPreference) {
        viewModelScope.launch {
            val current = _settingsState.value.priceNotifications
            val newList = current + PriceNotification(threshold = threshold, type = type, unit = unit)
            settingsRepository.updatePriceNotifications(newList)
        }
    }

    fun removePriceNotification(id: String) {
        viewModelScope.launch {
            val current = _settingsState.value.priceNotifications
            val newList = current.filter { it.id != id }
            settingsRepository.updatePriceNotifications(newList)
        }
    }

    fun setChartFullscreen(isFullscreen: Boolean) {
        _isChartFullscreen.value = isFullscreen
    }

    fun triggerWidgetUpdate() {
        viewModelScope.launch {
            val state = uiState.value
            val currentPrice = state.currentPrice ?: return@launch

            val priceValue = currentPrice.resolvedPrice(state.currencyPreference, state.showVat)
            val suffix = if (state.currencyPreference == CurrencyPreference.SENT_KWH) " s/kWh" else " €/MWh"
            val priceText = "%.2f%s".format(priceValue, suffix).replace(".", ",")

            val cheapestText = if (state.cheapestWindowStarts.isNotEmpty() && state.chartData.isNotEmpty()) {
                val startIdx = state.cheapestWindowStarts.first()
                val size = state.cheapestWindowSize
                if (startIdx in state.chartData.indices && (startIdx + size - 1) in state.chartData.indices) {
                    val start = state.chartData[startIdx].localDateTime
                    val lastItem = state.chartData[startIdx + size - 1]
                    val end = if (lastItem.resolution == PriceResolution.HOUR_1) {
                        lastItem.localDateTime.plusHours(1)
                    } else {
                        lastItem.localDateTime.plusMinutes(15)
                    }
                    val formatter = DateTimeFormatter.ofPattern("HH")
                    "${formatter.format(start)}-${formatter.format(end)}"
                } else "--"
            } else "--"

            val now = Instant.now()
            val nextPricesStr = state.chartData
                .asSequence()
                .filter { it.timestamp.isAfter(now) || it.timestamp == currentPrice.timestamp }
                .take(4)
                .map { p -> p.resolvedPrice(state.currencyPreference, state.showVat) }
                .joinToString(",") { "%.2f".format(Locale.US, it) }

            val todayPricesStr = state.todayChartData
                .asSequence()
                .take(if (state.priceResolution == PriceResolution.HOUR_1) 24 else 96)
                .filterIndexed { index, _ -> state.priceResolution == PriceResolution.HOUR_1 || index % 4 == 0 }
                .take(24)
                .map { p -> p.resolvedPrice(state.currencyPreference, state.showVat) }
                .joinToString(",") { "%.2f".format(Locale.US, it) }

            val cheapestStart = state.todaySummary?.cheapestWindowStart ?: -1
            val hourlyCheapestStart = if (cheapestStart != -1 && state.priceResolution == PriceResolution.MINUTE_15) {
                cheapestStart / 4
            } else {
                cheapestStart
            }
            val hourlyDuration = state.cheapestWindowSize / (if (state.priceResolution == PriceResolution.HOUR_1) 1 else 4)

            WidgetUpdater.updateAllWidgets(
                application,
                priceText,
                cheapestText,
                nextPricesStr,
                todayPricesStr,
                state.widgetTheme,
                hourlyCheapestStart,
                hourlyDuration
            )
        }
    }
}
