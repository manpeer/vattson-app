package ee.vattson.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ee.vattson.MainActivity
import ee.vattson.data.model.AppTheme
import ee.vattson.ui.theme.darkScheme
import ee.vattson.ui.theme.lightScheme

val WidgetColors = ColorProviders(
    light = lightScheme,
    dark = darkScheme
)

open class PriceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(60.dp, 60.dp),   // 1x1
            DpSize(120.dp, 60.dp),  // 2x1
            DpSize(180.dp, 60.dp),  // 3x1
            DpSize(240.dp, 60.dp)   // 4x1
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val themeName = prefs[widgetThemeKey] ?: AppTheme.SYSTEM.name
            val theme = try { AppTheme.valueOf(themeName) } catch (_: Exception) { AppTheme.SYSTEM }

            when (theme) {
                AppTheme.LIGHT -> {
                    GlanceTheme(colors = ColorProviders(light = lightScheme, dark = lightScheme)) {
                        WidgetContent()
                    }
                }
                AppTheme.DARK -> {
                    GlanceTheme(colors = ColorProviders(light = darkScheme, dark = darkScheme)) {
                        WidgetContent()
                    }
                }
                AppTheme.SYSTEM -> {
                    GlanceTheme(colors = WidgetColors) {
                        WidgetContent()
                    }
                }
                AppTheme.DYNAMIC -> {
                    GlanceTheme {
                        WidgetContent()
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        val context = LocalContext.current
        val size = LocalSize.current
        val prefs = currentState<Preferences>()
        val currentPrice = prefs[currentPriceKey] ?: "--"
        val cheapestWindow = prefs[cheapestWindowKey] ?: "--"

        // Core GlanceTheme.colors tokens now reflect our custom WidgetColors
        val background = GlanceTheme.colors.surface
        val primary = GlanceTheme.colors.primary
        val onSurfaceVariant = GlanceTheme.colors.onSurface

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(background)
                .padding(8.dp)
                .clickable(actionStartActivity(
                    context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        flags = (flags or Intent.FLAG_ACTIVITY_NEW_TASK) and Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED.inv()
                    } ?: Intent(context, MainActivity::class.java)
                )),
            contentAlignment = Alignment.Center
        ) {
            if (size.width < 100.dp) {
                // 1x1 Style - Price number bold + small currency unit
                val numberPart = currentPrice.split(" ").firstOrNull() ?: "--"
                val unitPart = currentPrice.split(" ").lastOrNull() ?: ""

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Hind", style = TextStyle(fontSize = 9.sp, color = onSurfaceVariant))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = numberPart,
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primary),
                            maxLines = 1
                        )
                        if (unitPart.isNotEmpty()) {
                            Spacer(modifier = GlanceModifier.width(2.dp))
                            Text(
                                text = unitPart,
                                style = TextStyle(fontSize = 7.sp, color = onSurfaceVariant),
                                maxLines = 1
                            )
                        }
                    }
                }
            } else {
                // Multi-info Style (Responsive Row)
                val priceFontSize = when {
                    size.width < 150.dp -> 16.sp // 2x1
                    size.width < 200.dp -> 18.sp // 3x1
                    else -> 20.sp               // 4x1
                }
                val unitFontSize = (priceFontSize.value * 0.55f).sp

                val numberPart = currentPrice.split(" ").firstOrNull() ?: "--"
                val unitPart = currentPrice.split(" ").getOrNull(1) ?: ""

                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Current Price (number bold + smaller unit text)
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("Hind hetkel", style = TextStyle(fontSize = 11.sp, color = onSurfaceVariant))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = numberPart,
                                style = TextStyle(fontSize = priceFontSize, fontWeight = FontWeight.Bold, color = primary),
                                maxLines = 1
                            )
                            if (unitPart.isNotEmpty()) {
                                Spacer(modifier = GlanceModifier.width(2.dp))
                                Text(
                                    text = unitPart,
                                    style = TextStyle(fontSize = unitFontSize, color = onSurfaceVariant),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Soodsaim", style = TextStyle(fontSize = 11.sp, color = onSurfaceVariant))
                        Text(cheapestWindow, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = primary), maxLines = 1)
                    }
                }
            }
        }
    }

    companion object {
        val currentPriceKey = stringPreferencesKey("current_price")
        val cheapestWindowKey = stringPreferencesKey("cheapest_window")
        val nextPricesKey = stringPreferencesKey("next_prices")
        val todayPricesKey = stringPreferencesKey("today_prices")
        val todayCheapestStartKey = stringPreferencesKey("today_cheapest_start")
        val todayCheapestDurationKey = stringPreferencesKey("today_cheapest_duration")
        val widgetThemeKey = stringPreferencesKey("widget_theme")
    }
}

class PriceWidget1x1 : PriceWidget()
class PriceWidget2x1 : PriceWidget()
class PriceWidget3x1 : PriceWidget()
class PriceWidget4x1 : PriceWidget()
