package ee.vattson.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BasePriceWidgetReceiver : GlanceAppWidgetReceiver() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetUpdater.updateAllWidgets(context)
            } finally {
                pendingResult?.finish()
            }
        }
    }
}

class PriceWidget1x1Receiver : BasePriceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PriceWidget1x1()
}

class PriceWidget2x1Receiver : BasePriceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PriceWidget2x1()
}

class PriceWidget3x1Receiver : BasePriceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PriceWidget3x1()
}

class PriceWidget4x1Receiver : BasePriceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PriceWidget4x1()
}

class DonutClockWidgetReceiver : BasePriceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DonutClockWidget()
}
