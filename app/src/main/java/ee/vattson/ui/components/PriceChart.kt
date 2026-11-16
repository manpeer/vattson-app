package ee.vattson.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.Interaction
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.Position
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.TextComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import ee.vattson.data.model.AppTheme
import ee.vattson.data.model.CurrencyPreference
import ee.vattson.data.model.ElectricityPrice
import ee.vattson.data.model.PriceResolution
import ee.vattson.ui.theme.cheapestWindowGreen
import ee.vattson.ui.theme.currentPeriodDarkBlue
import ee.vattson.ui.theme.defaultBarBlue
import ee.vattson.ui.theme.lowPriceGreen
import ee.vattson.ui.theme.orangeHighlight
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun PriceChart(
    data: List<ElectricityPrice>,
    modifier: Modifier = Modifier,
    currencyPreference: CurrencyPreference,
    showVat: Boolean,
    appTheme: AppTheme,
    cheapestWindowStarts: List<Int> = emptyList(),
    cheapestWindowSize: Int = 4,
    isChartFullscreen: Boolean = false,
    onMarkerTargetsChanged: (List<CartesianMarker.Target>?) -> Unit = {}
) {
    if (data.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = currencyPreference.shortLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp)
        )

        val currentColorScheme = MaterialTheme.colorScheme
        key(appTheme, currentColorScheme) {
        val modelProducer = remember { CartesianChartModelProducer() }

        val estonianSymbols = remember {
            DecimalFormatSymbols(Locale.forLanguageTag("et-EE")).apply {
                decimalSeparator = ','
            }
        }
        val priceFormatter = remember(estonianSymbols) { DecimalFormat("#,##0.00", estonianSymbols) }

        val prices = remember(data, currencyPreference, showVat) {
            data.map { p -> p.resolvedPrice(currencyPreference, showVat) }
        }

        LaunchedEffect(prices) {
            modelProducer.runTransaction {
                columnModel { series(prices) }
            }
        }

        val now = remember(floor(System.currentTimeMillis() / 30000.0).toLong()) { Instant.now() }
        val currentIndex = remember(data, now) {
            data.indexOfFirst { p ->
                val stepSeconds = if (p.resolution == PriceResolution.HOUR_1) 3600L else 900L
                !now.isBefore(p.timestamp) && now.isBefore(p.timestamp.plusSeconds(stepSeconds))
            }
        }

        val minIndex = remember(prices) {
            if (prices.isEmpty()) -1 else prices.indices.minBy { prices[it] }
        }

        val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
        val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM") }

        var isSelectionActive by remember { mutableStateOf(false) }
        val scrollState = rememberVicoScrollState(scrollEnabled = !isSelectionActive)
        var chartWidth by remember { mutableFloatStateOf(0f) }
        var draggedCanvasX by remember { mutableFloatStateOf(-1f) }
        val edgeThreshold = with(LocalDensity.current) { 40.dp.toPx() }

        val markerController = remember {
            LongPressMarkerController(
                onMove = { x -> draggedCanvasX = x },
                onHidden = {
                    draggedCanvasX = -1f
                    isSelectionActive = false
                },
                onLongPress = { isSelectionActive = true }
            )
        }

        LaunchedEffect(edgeThreshold) {
            snapshotFlow { draggedCanvasX to chartWidth }
                .collectLatest { (draggedX, width) ->
                    if (draggedX != -1f && width > 0f) {
                        while (true) {
                            val currentX = draggedCanvasX
                            val currentWidth = chartWidth
                            val scrollDelta = when {
                                currentX < edgeThreshold -> -15f
                                currentX > currentWidth - edgeThreshold -> 15f
                                else -> 0f
                            }
                            if (scrollDelta != 0f) {
                                scrollState.scroll(Scroll.Relative.pixels(scrollDelta))
                            } else {
                                break
                            }
                            withFrameMillis { }
                        }
                    }
                }
        }

        var selectedIndex by remember { mutableIntStateOf(-1) }
        val touchMarker = rememberMarker(
            data = data,
            currencyPreference = currencyPreference,
            showVat = showVat,
            timeFormatter = timeFormatter,
            priceFormatter = priceFormatter,
            cheapestWindowStarts = cheapestWindowStarts,
            cheapestWindowSize = cheapestWindowSize,
            targetIndex = selectedIndex,
            currentIndex = currentIndex,
            minIndex = minIndex,
            isChartFullscreen = isChartFullscreen
        )

        val defaultColor = defaultBarBlue
        val highlightColor = currentPeriodDarkBlue
        val lowColor = lowPriceGreen
        val windowColor = cheapestWindowGreen

        val defaultComponent = rememberLineComponent(Fill(defaultColor), 16.dp)
        val highlightComponent = rememberLineComponent(Fill(highlightColor), 16.dp)
        val lowComponent = rememberLineComponent(Fill(lowColor), 16.dp)
        val windowComponent = rememberLineComponent(Fill(windowColor), 16.dp)

        val columnProvider = remember(
            currentIndex,
            minIndex,
            cheapestWindowStarts,
            cheapestWindowSize,
            defaultComponent,
            highlightComponent,
            lowComponent,
            windowComponent
        ) {
            object : ColumnCartesianLayer.ColumnProvider {
                override fun getColumn(
                    entry: ColumnCartesianLayerModel.Entry,
                    extraStore: ExtraStore
                ): LineComponent {
                    val index = entry.x.roundToInt()
                    val isWindow = cheapestWindowStarts.any { start ->
                        index in start until (start + cheapestWindowSize)
                    }
                    return when {
                        index == currentIndex -> highlightComponent
                        index == minIndex -> lowComponent
                        isWindow -> windowComponent
                        else -> defaultComponent
                    }
                }

                override fun getWidestSeriesColumn(
                    seriesKey: Any,
                    seriesIndex: Int,
                    extraStore: ExtraStore,
                ): LineComponent = defaultComponent
            }
        }

        val spacing = remember(data) {
            val res = data.firstOrNull()?.resolution ?: PriceResolution.HOUR_1
            if (res == PriceResolution.HOUR_1) 2 else 3
        }
        val bottomAxisItemPlacer = remember(spacing) { HorizontalAxis.ItemPlacer.aligned(spacing = { spacing }) }
        val bottomAxisValueFormatter = remember(data, timeFormatter) {
            CartesianValueFormatter { _, value, _ ->
                val index = value.roundToInt()
                data.getOrNull(index)?.let { p ->
                    timeFormatter.format(p.localDateTime)
                } ?: value.roundToInt().toString()
            }
        }

        val axisLabel = rememberTextComponent(
            style = TextStyle(color = MaterialTheme.colorScheme.onSurface)
        )

        val guideline = rememberLineComponent(
            fill = Fill(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
            thickness = 1.dp
        )

        val midnightTransitions = remember(data, dateFormatter) {
            data.indices.filter { i ->
                if (i == 0) false
                else {
                    val prev = data[i - 1].localDateTime.toLocalDate()
                    val curr = data[i].localDateTime.toLocalDate()
                    curr.isAfter(prev)
                }
            }.map { i -> i to dateFormatter.format(data[i].localDateTime.toLocalDate()) }
        }

        val dayLabelComponent = rememberTextComponent(
            style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold),
            background = rememberShapeComponent(
                fill = Fill(orangeHighlight),
                shape = RoundedCornerShape(4.dp)
            ),
            padding = Insets(6.dp, 2.dp)
        )

        val nowLabelComponent = rememberTextComponent(
            style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold),
            background = rememberShapeComponent(
                fill = Fill(currentPeriodDarkBlue),
                shape = RoundedCornerShape(4.dp)
            ),
            padding = Insets(6.dp, 2.dp)
        )

        val chart = rememberCartesianChart(
            rememberColumnCartesianLayer(columnProvider = columnProvider),
            startAxis = VerticalAxis.rememberStart(
                label = axisLabel,
                guideline = guideline,
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                itemPlacer = bottomAxisItemPlacer,
                valueFormatter = bottomAxisValueFormatter,
                label = axisLabel,
                guideline = guideline
            ),
            marker = touchMarker,
            markerController = markerController,
            decorations = listOfNotNull(
                if (currentIndex != -1) {
                    VerticalLine(
                        x = currentIndex.toFloat(),
                        fill = Fill(currentPeriodDarkBlue),
                        label = "Hetkel",
                        labelComponent = nowLabelComponent
                    )
                } else null,
            ) + cheapestWindowStarts.map { start ->
                VerticalBox(
                    xRange = start.toFloat()..(start + cheapestWindowSize - 1).toFloat(),
                    fill = Fill(windowColor.copy(alpha = 0.20f))
                )
            } + midnightTransitions.map { (index, label) ->
                DaySeparator(
                    x = index.toFloat(),
                    dateLabel = "Homme ($label)",
                    labelComponent = dayLabelComponent,
                    lineColor = orangeHighlight
                )
            },
            markerVisibilityListener = remember(onMarkerTargetsChanged) {
                object : CartesianMarkerVisibilityListener {
                    override fun onShown(
                        marker: CartesianMarker,
                        targets: List<CartesianMarker.Target>
                    ) {
                        selectedIndex = targets.firstOrNull()?.x?.roundToInt() ?: -1
                        onMarkerTargetsChanged(targets)
                    }

                    override fun onUpdated(
                        marker: CartesianMarker,
                        targets: List<CartesianMarker.Target>
                    ) {
                        selectedIndex = targets.firstOrNull()?.x?.roundToInt() ?: -1
                        onMarkerTargetsChanged(targets)
                    }

                    override fun onHidden(marker: CartesianMarker) {
                        selectedIndex = -1
                        onMarkerTargetsChanged(null)
                    }
                }
            }
        )

        val currentViewConfiguration = LocalViewConfiguration.current
        val customViewConfiguration = remember(currentViewConfiguration) {
            object : ViewConfiguration by currentViewConfiguration {
                override val longPressTimeoutMillis: Long = 100L
            }
        }

        CompositionLocalProvider(LocalViewConfiguration provides customViewConfiguration) {
            CartesianChartHost(
                chart = chart,
                modelProducer = modelProducer,
                scrollState = scrollState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { chartWidth = it.width.toFloat() }
            )
        }
    }
}
}

@Composable
fun rememberMarker(
    data: List<ElectricityPrice>,
    currencyPreference: CurrencyPreference,
    showVat: Boolean,
    timeFormatter: DateTimeFormatter,
    priceFormatter: DecimalFormat,
    cheapestWindowStarts: List<Int> = emptyList(),
    cheapestWindowSize: Int = 4,
    targetIndex: Int = -1,
    currentIndex: Int = -1,
    minIndex: Int = -1,
    isChartFullscreen: Boolean = false
): CartesianMarker {
    val isCurrent = targetIndex != -1 && targetIndex == currentIndex
    val isMinPrice = targetIndex != -1 && targetIndex == minIndex
    val isWindowPrice = targetIndex != -1 && cheapestWindowStarts.any { start ->
        targetIndex in start until (start + cheapestWindowSize)
    }
    
    val bubbleColor = when {
        isCurrent -> currentPeriodDarkBlue
        isMinPrice -> lowPriceGreen
        isWindowPrice -> cheapestWindowGreen
        else -> defaultBarBlue
    }
    
    val textColor = Color.White

    val labelBackground = rememberShapeComponent(
        fill = Fill(bubbleColor),
        shape = RoundedCornerShape(4.dp)
    )
    val label = rememberTextComponent(
        style = TextStyle(
            color = textColor
        ),
        background = labelBackground,
        padding = Insets(8.dp, 4.dp),
        lineCount = 3
    )
    
    val markerValueFormatter = DefaultCartesianMarker.ValueFormatter { _, targets ->
        val target = targets.firstOrNull() ?: return@ValueFormatter " "
        val xValue = when (target) {
            is ColumnCartesianLayerMarkerTarget -> target.columns.firstOrNull()?.entry?.x
            else -> null
        } ?: return@ValueFormatter " "
        
        val index = xValue.roundToInt()
        if (index in data.indices) {
            val p = data[index]
            val priceValue = p.resolvedPrice(currencyPreference, showVat)
            val start = p.localDateTime
            val end = if (p.resolution == PriceResolution.HOUR_1) start.plusHours(1) else start.plusMinutes(15)
            val timeRange = "${timeFormatter.format(start)}-${timeFormatter.format(end)}"
            
            val formattedPrice = priceFormatter.format(priceValue)
            
            buildAnnotatedString {
                when {
                    index == minIndex -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Madalaim hind")
                        }
                        append("\n")
                    }
                    cheapestWindowStarts.any { start -> index in start until (start + cheapestWindowSize) } -> {
                        val duration = if (p.resolution == PriceResolution.HOUR_1) cheapestWindowSize else cheapestWindowSize / 4
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Soodsaim ${duration}t periood")
                        }
                        append("\n")
                    }
                }
                
                append(timeRange)
                append("\n")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(formattedPrice)
                }
                append(" ${currencyPreference.shortLabel}")
            }
        } else " "
    }


    val guideline = rememberLineComponent(
        fill = Fill(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
        thickness = 1.dp
    )

    return remember(label, guideline, markerValueFormatter) {
        object : DefaultCartesianMarker(
            label = label,
            guideline = guideline,
            valueFormatter = markerValueFormatter
        ) {
            override fun updateLayerMargins(
                context: com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext,
                layerMargins: com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerMargins,
                layerDimensions: com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerDimensions,
                model: com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel,
            ) {
                // Do nothing to keep the chart as big as possible
            }

            override fun drawOverLayers(
                context: com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext,
                targets: List<CartesianMarker.Target>,
            ) {
                with(context) {
                    drawGuideline(targets)
                    
                    val text = valueFormatter.format(context, targets)
                    val targetX = targets.map { it.canvasX }.average().toFloat()
                    val labelBounds = label.getBounds(context, text, layerBounds.width.toInt())
                    
                    val chartWidth = layerBounds.width
                    val maxAllowedHalfWidth = chartWidth / 2f
                    val halfOfTextWidth = min(labelBounds.width / 2f, maxAllowedHalfWidth)
                    
                    val x = when {
                        targetX - halfOfTextWidth < layerBounds.left -> layerBounds.left + halfOfTextWidth
                        targetX + halfOfTextWidth > layerBounds.right -> layerBounds.right - halfOfTextWidth
                        else -> targetX
                    }.coerceIn(layerBounds.left, layerBounds.right)
                    
                    label.draw(
                        context = context,
                        text = text,
                        x = x,
                        y = layerBounds.top,
                        verticalPosition = if (isChartFullscreen) Position.Vertical.Bottom else Position.Vertical.Top,
                        maxWidth = (min(layerBounds.right - x, x - layerBounds.left) * 2f).toInt().coerceAtLeast(0)
                    )
                }
            }
        }
    }
}

private class LongPressMarkerController(
    private val onMove: (Float) -> Unit,
    private val onHidden: () -> Unit,
    private val onLongPress: () -> Unit
) : CartesianMarkerController {
    private var active = false

    override val acceptsLongPress: Boolean = true
    override val consumeMoveEvents: Boolean = true

    override fun shouldAcceptInteraction(
        interaction: Interaction,
        targets: List<CartesianMarker.Target>
    ): Boolean = when (interaction) {
        is Interaction.LongPress -> {
            active = true
            onLongPress()
            onMove(interaction.point.x)
            true
        }
        is Interaction.Move -> {
            if (active) {
                onMove(interaction.point.x)
            }
            active
        }
        is Interaction.Release -> {
            active = false
            onHidden()
            true
        }
        else -> false
    }

    override fun shouldShowMarker(
        interaction: Interaction,
        targets: List<CartesianMarker.Target>
    ): Boolean = active
}

private class VerticalLine(
    private val x: Float,
    private val fill: Fill,
    private val label: String? = null,
    private val labelComponent: TextComponent? = null
) : Decoration {
    override fun drawUnderLayers(context: com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext) {
        with(context) {
            val xStep = ranges.xStep.toFloat()
            val canvasX = layerBounds.left + layerDimensions.startPadding +
                (x - ranges.minX.toFloat()) / xStep * layerDimensions.xSpacing - scroll

            val lineThickness = 2f // Thin line in pixels
            val left = canvasX - lineThickness / 2f
            val right = canvasX + lineThickness / 2f

            if (canvasX in layerBounds.left..layerBounds.right) {
                canvas.drawRect(
                    left = left,
                    top = layerBounds.top,
                    right = right,
                    bottom = layerBounds.bottom,
                    paint = Paint().apply {
                        color = fill.color
                    }
                )
            }
        }
    }

    override fun drawOverLayers(context: com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext) {
        if (label != null && labelComponent != null) {
            with(context) {
                val xStep = ranges.xStep.toFloat()
                val canvasX = layerBounds.left + layerDimensions.startPadding +
                    (x - ranges.minX.toFloat()) / xStep * layerDimensions.xSpacing - scroll

                if (canvasX in layerBounds.left..layerBounds.right) {
                    labelComponent.draw(
                        context = context,
                        text = label,
                        x = canvasX,
                        y = layerBounds.top,
                        verticalPosition = Position.Vertical.Top,
                        horizontalPosition = Position.Horizontal.Center
                    )
                }
            }
        }
    }
}

private class DaySeparator(
    private val x: Float,
    private val dateLabel: String,
    private val labelComponent: TextComponent,
    private val lineColor: Color
) : Decoration {
    override fun drawOverLayers(context: com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext) {
        with(context) {
            val xStep = ranges.xStep.toFloat()
            // Draw line slightly offset to the left so it sits BETWEEN bars
            val canvasX = layerBounds.left + layerDimensions.startPadding +
                (x - 0.5f - ranges.minX.toFloat()) / xStep * layerDimensions.xSpacing - scroll

            if (canvasX in layerBounds.left..layerBounds.right) {
                // 1. Draw Line
                canvas.drawRect(
                    left = canvasX - 1f,
                    top = layerBounds.top,
                    right = canvasX + 1f,
                    bottom = layerBounds.bottom,
                    paint = Paint().apply {
                        color = lineColor
                    }
                )

                // 2. Draw Date Tag
                labelComponent.draw(
                    context = context,
                    text = dateLabel,
                    x = canvasX,
                    y = layerBounds.top,
                    verticalPosition = Position.Vertical.Top,
                    horizontalPosition = Position.Horizontal.Center
                )
            }
        }
    }
}

private class VerticalBox(
    private val xRange: ClosedFloatingPointRange<Float>,
    private val fill: Fill,
) : Decoration {
    override fun drawUnderLayers(context: com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext) {
        with(context) {
            val xStep = ranges.xStep.toFloat()
            val startX = layerBounds.left + layerDimensions.startPadding + 
                (xRange.start - ranges.minX.toFloat()) / xStep * layerDimensions.xSpacing - scroll
            val endX = layerBounds.left + layerDimensions.startPadding + 
                (xRange.endInclusive - ranges.minX.toFloat()) / xStep * layerDimensions.xSpacing - scroll
            
            // Adjust to cover the full width of the bars at the edges
            val halfSpacing = layerDimensions.xSpacing / 2f
            
            val left = startX - halfSpacing
            val right = endX + halfSpacing
            
            canvas.drawRect(
                left = left.coerceAtLeast(layerBounds.left),
                top = layerBounds.top,
                right = right.coerceAtMost(layerBounds.right),
                bottom = layerBounds.bottom,
                paint = Paint().apply {
                    color = fill.color
                }
            )
        }
    }
}
