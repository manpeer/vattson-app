package ee.vattson.util

import android.content.Context
import ee.vattson.data.dataStore
import ee.vattson.data.model.CurrencyPreference
import ee.vattson.data.model.NotificationType
import ee.vattson.data.repository.EleringRepository
import ee.vattson.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant

object PriceNotificationManager {

    suspend fun checkAndTriggerNotifications(context: Context) = withContext(Dispatchers.IO) {
        val settingsRepository = SettingsRepository(context.dataStore)
        val eleringRepository = EleringRepository(context)

        // 1. Check if notifications are globally enabled
        val enabled = settingsRepository.notificationsEnabledFlow.first()
        if (!enabled) return@withContext

        // 2. Get saved notifications
        val notifications = settingsRepository.priceNotificationsFlow.first()
        if (notifications.isEmpty()) return@withContext

        // 3. Get current price
        val cachedPrices = eleringRepository.getCachedPrices()
        if (cachedPrices.isEmpty()) return@withContext

        // Hourly averages are usually better for notifications
        val activePrices = eleringRepository.getHourlyAverages(cachedPrices)
        
        val now = Instant.now()
        val currentPrice = activePrices.indexOfFirst { p ->
            val start = p.timestamp
            val end = p.timestamp.plusSeconds(3600)
            !now.isBefore(start) && now.isBefore(end)
        }.let { if (it != -1) activePrices[it] else null } ?: return@withContext

        // 4. Avoid double-notifying for the same price window
        val lastNotified = settingsRepository.lastNotifiedTimestampFlow.first()
        if (lastNotified == currentPrice.timestamp.toEpochMilli()) return@withContext

        // 5. Check each notification
        var triggered = false
        notifications.forEach { notification ->
            @Suppress("SENSELESS_COMPARISON")
            val unit = notification.unit
            
            val priceToCheck = if (unit == CurrencyPreference.SENT_KWH) {
                currentPrice.priceWithVatCentsKwh
            } else {
                currentPrice.priceWithVatEurMwh
            }

            val unitStr = unit.shortLabel
            val isTriggered = when (notification.type) {
                NotificationType.BELOW -> priceToCheck < notification.threshold
                NotificationType.ABOVE -> priceToCheck > notification.threshold
            }

            if (isTriggered) {
                val title = "Elektrihinna teavitus"
                val typeStr = if (notification.type == NotificationType.BELOW) "langes alla" else "tõusis üle"
                val message = "Praegune hind on %.2f %s, mis %s sätitud piiri %.2f %s."
                    .format(priceToCheck, unitStr, typeStr, notification.threshold, unitStr)
                    .replace(".", ",")
                
                NotificationHelper.showPriceNotification(
                    context, 
                    title, 
                    message, 
                    notification.id.hashCode()
                )
                triggered = true
            }
        }

        if (triggered) {
            settingsRepository.updateLastNotifiedTimestamp(currentPrice.timestamp.toEpochMilli())
        }
    }
}
