package ee.vattson

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ee.vattson.util.NotificationHelper
import ee.vattson.widget.WidgetUpdateWorker
import ee.vattson.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class VattsonApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        scheduleWidgetUpdates()
    }

    private fun scheduleWidgetUpdates() {
        // Offload WorkManager initialization and database queries off the main thread during app launch
        CoroutineScope(Dispatchers.Default).launch {
            val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                1, TimeUnit.HOURS // Minimum interval for periodic work
            ).build()

            WorkManager.getInstance(this@VattsonApplication).enqueueUniquePeriodicWork(
                "WidgetUpdateWork",
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
                workRequest
            )

            WidgetUpdater.scheduleNextWidgetUpdate(this@VattsonApplication)
        }
    }
}
