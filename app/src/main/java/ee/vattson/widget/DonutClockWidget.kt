package ee.vattson.widget

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
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
import java.util.Calendar

class DonutClockWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val themeName = prefs[PriceWidget.widgetThemeKey] ?: AppTheme.SYSTEM.name
            val theme = try { AppTheme.valueOf(themeName) } catch (_: Exception) { AppTheme.SYSTEM }

            val colorProvider = when (theme) {
                AppTheme.LIGHT -> ColorProviders(light = lightScheme, dark = lightScheme)
                AppTheme.DARK -> ColorProviders(light = darkScheme, dark = darkScheme)
                AppTheme.SYSTEM -> WidgetColors
                else -> WidgetColors // Use WidgetColors as fallback for Dynamic to satisfy non-null
            }

            GlanceTheme(colors = colorProvider) {
                WidgetContent(theme)
            }
        }
    }

    @Composable
    private fun WidgetContent(theme: AppTheme) {
        val context = LocalContext.current
        val size = LocalSize.current
        val prefs = currentState<Preferences>()
        
        val pricesStr = prefs[PriceWidget.todayPricesKey] ?: ""
        val prices = remember(pricesStr) {
            pricesStr.split(",").asSequence().filter { it.isNotEmpty() }.mapNotNull { it.toFloatOrNull() }.toList().toFloatArray()
        }
        val cheapestStart = prefs[PriceWidget.todayCheapestStartKey]?.toIntOrNull() ?: -1
        val cheapestDuration = prefs[PriceWidget.todayCheapestDurationKey]?.toIntOrNull() ?: 0
        
        val fullPriceText = prefs[PriceWidget.currentPriceKey] ?: "-- --"
        val priceParts = fullPriceText.split(" ")
        val currentPrice = priceParts.firstOrNull() ?: "--"
        val priceUnit = priceParts.getOrNull(1) ?: ""

        val density = context.resources.displayMetrics.density
        // Further increased resolution (up to 1536px) for ultra-sharp graphics on 4x4 widgets and high-res displays.
        // We use a 1.5x multiplier over the physical pixel size to ensure sharp downsampling.
        val targetPx = (minOf(size.width, size.height).value * density * 1.5f).toInt()
        val pixelSize = targetPx.coerceIn(400, 768)

        // Resolve colors directly from our theme schemes because GlanceTheme.colors
        // doesn't yet expose all M3 tokens like surfaceContainer
        val isSystemDark = remember(context) {
            (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        val isDark = when (theme) {
            AppTheme.LIGHT -> false
            AppTheme.DARK -> true
            else -> isSystemDark
        }

        val activeScheme = if (isDark) darkScheme else lightScheme
        val bgColor = activeScheme.surface.toArgb()
        val textColor = activeScheme.onSurface.toArgb()
        val primaryColor = activeScheme.onPrimaryContainer.toArgb()

        // Calculate dynamic proportions based on actual widget size (SizeMode.Exact)
        val minDimension = minOf(size.width, size.height).value
        val fontSize = (minDimension * 0.10f).coerceIn(13f, 70f).sp
        val unitFontSize = (fontSize.value * 0.45f).sp
        val innerBoxSize = (minDimension * 0.5f).dp
        
        // 1. Generate Background Donut Chart
        val backgroundBitmap = remember(prices, pixelSize, bgColor, textColor, cheapestStart, cheapestDuration) {
            DonutChartGenerator.generateDonutChart(
                prices = prices, 
                sizePx = pixelSize, 
                backgroundColor = bgColor, 
                textColor = textColor,
                cheapestStart = cheapestStart,
                cheapestDuration = cheapestDuration,
            )
        }

        // 2. Generate Rotated Clock Hand
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val rotationDegrees = (hour * 15f) + (minute * 0.25f) // 360 / 24 = 15 degrees per hour

        val handBitmap = remember(rotationDegrees, pixelSize, primaryColor) {
            val bitmap = createBitmap(pixelSize, pixelSize)
            val canvas = Canvas(bitmap)
            val center = pixelSize / 2f
            val handLength = center * 0.75f // Reaches the start of the price ring

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                style = Paint.Style.FILL
            }

            canvas.withTranslation(center, center) {
                rotate(rotationDegrees)

                val path = Path()
                val hubRadius = pixelSize * 0.032f
                val stemWidthBase = pixelSize * 0.010f
                val stemWidthTop = pixelSize * 0.004f

                // 1. Hub with small inner hole
                val hubPath = Path()
                hubPath.addCircle(0f, 0f, hubRadius, Path.Direction.CW)
                val hubHole = Path()
                hubHole.addRoundRect(
                    -pixelSize * 0.004f, -pixelSize * 0.006f,
                    pixelSize * 0.004f, pixelSize * 0.006f,
                    pixelSize * 0.002f, pixelSize * 0.002f,
                    Path.Direction.CW
                )
                hubPath.op(hubHole, Path.Op.DIFFERENCE)
                drawPath(hubPath, paint)

                // 2. Straight Tapered Hand
                path.moveTo(-stemWidthBase / 2f, -hubRadius * 0.9f)
                path.lineTo(-stemWidthTop / 2f, -handLength)
                path.lineTo(stemWidthTop / 2f, -handLength)
                path.lineTo(stemWidthBase / 2f, -hubRadius * 0.9f)
                path.close()

                drawPath(path, paint)
            }

            bitmap
        }

        // Determine text placement based on hand rotation to avoid overlap
        // 0 deg = Up, 180 deg = Down
        val isPointingDown = rotationDegrees in 90.1f..269.9f
        val textAlignment = if (isPointingDown) Alignment.TopCenter else Alignment.BottomCenter

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable(actionStartActivity(
                    context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        flags = (flags or Intent.FLAG_ACTIVITY_NEW_TASK) and Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED.inv()
                    } ?: Intent(context, MainActivity::class.java)
                )),
            contentAlignment = Alignment.Center
        ) {
            // Background Donut
            Image(
                provider = ImageProvider(backgroundBitmap),
                contentDescription = "Price Chart",
                modifier = GlanceModifier.fillMaxSize()
            )

            // Analog Hand
            Image(
                provider = ImageProvider(handBitmap),
                contentDescription = "Clock Hand",
                modifier = GlanceModifier.fillMaxSize()
            )

            // Central Inner Box (Time/Price)
            Box(
                modifier = GlanceModifier.size(innerBoxSize).padding(4.dp),
                contentAlignment = textAlignment
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentPrice,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (priceUnit.isNotEmpty()) {
                        Text(
                            text = priceUnit,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = unitFontSize
                            )
                        )
                    }
                }
            }
        }
    }

}
