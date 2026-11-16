@file:OptIn(ExperimentalMaterial3Api::class)
package ee.vattson.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.vattson.R
import ee.vattson.data.model.AppTheme
import ee.vattson.data.model.CurrencyPreference
import ee.vattson.ui.components.ButtonGroupItemPosition
import ee.vattson.ui.components.clickableWithScale
import ee.vattson.ui.components.filledToggleableItem
import ee.vattson.ui.components.pressScaleEffect
import ee.vattson.ui.theme.VattsonTheme
import ee.vattson.ui.viewmodel.MainUiState
import ee.vattson.widget.DonutClockWidgetReceiver
import ee.vattson.widget.PriceWidget1x1Receiver
import ee.vattson.widget.PriceWidget2x1Receiver
import ee.vattson.widget.PriceWidget3x1Receiver
import ee.vattson.widget.PriceWidget4x1Receiver
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WidgetSizeOption(
    val label: String,
    val width: Int,
    val height: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetScreen(
    uiState: MainUiState,
    onRefreshWidget: () -> Unit,
    onWidgetThemeChange: (AppTheme) -> Unit,
    onAppBarVisibilityChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Report App Bar visibility to parent
    val appBarVisibleFraction by remember {
        androidx.compose.runtime.derivedStateOf { 1f - scrollBehavior.state.collapsedFraction }
    }
    androidx.compose.runtime.LaunchedEffect(appBarVisibleFraction) {
        onAppBarVisibilityChange(appBarVisibleFraction)
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    val sheetColorCollapsed = if (isDark) {
        MaterialTheme.colorScheme.onSecondary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    
    val isExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded
    var showThemeInfo by rememberSaveable { mutableStateOf(false) }

    if (showThemeInfo) {
        AlertDialog(
            onDismissRequest = { showThemeInfo = false },
            title = { Text(text = "Kujunduse info") },
            text = {
                Column {
                    Text(
                        text = "Dünaamiline - Kasutab värve Teie koduekraani taustapildilt.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Süsteem - Kuvab heledat või tumedat kujundust olenevalt telefoni tumeda režiimi seadetest.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeInfo = false }) {
                    Text("Sain aru")
                }
            }
        )
    }
    
    val currentSheetColor by animateColorAsState(
        targetValue = if (isExpanded) MaterialTheme.colorScheme.background else sheetColorCollapsed,
        label = "SheetColor"
    )
    
    var selectedOption by remember { mutableStateOf<WidgetSizeOption?>(null) }
    var showInfo by remember { mutableStateOf(false) }
    var isInstructionsExpanded by remember { mutableStateOf(false) }
    
    val widgetSizes = listOf(
        WidgetSizeOption("1x1", 1, 1),
        WidgetSizeOption("2x1", 2, 1),
        WidgetSizeOption("3x1", 3, 1),
        WidgetSizeOption("4x1", 4, 1),
        WidgetSizeOption("Ring", 2, 2)
    )

    // Memoize formatted data to avoid heavy recalculation on every recomposition
    val currentPriceText = remember(uiState.currentPrice, uiState.currencyPreference, uiState.showVat) {
        uiState.currentPrice?.let { p ->
            val value = if (uiState.currencyPreference == CurrencyPreference.SENT_KWH) {
                if (uiState.showVat) p.priceWithVatCentsKwh else p.priceCentsKwh
            } else {
                if (uiState.showVat) p.priceWithVatEurMwh else p.priceEurMwh
            }
            "%.2f".format(Locale.US, value).replace(".", ",")
        } ?: "--"
    }
    
    val currencySuffix = remember(uiState.currencyPreference) {
        if (uiState.currencyPreference == CurrencyPreference.SENT_KWH) "s/kWh" else "€/MWh"
    }

    val cheapestWindowText = remember(uiState.cheapestWindowStarts, uiState.chartData) {
        val firstStart = uiState.cheapestWindowStarts.firstOrNull()
        if (firstStart != null && uiState.chartData.isNotEmpty() && (firstStart + uiState.cheapestWindowSize) <= uiState.chartData.size) {
            val size = uiState.cheapestWindowSize
            val start = uiState.chartData[firstStart].localDateTime
            val lastItem = uiState.chartData[firstStart + size - 1]
            val end = lastItem.localDateTime.plusHours(1)
            val formatter = DateTimeFormatter.ofPattern("HH")
            "${formatter.format(start)}-${formatter.format(end)}"
        } else "--"
    }

    val liveNextPrices = remember(uiState.chartData, uiState.currencyPreference, uiState.showVat) {
        uiState.chartData
            .filter { it.timestamp.isAfter(Instant.now()) || it.timestamp == uiState.currentPrice?.timestamp }
            .take(4)
            .map { p ->
                if (uiState.currencyPreference == CurrencyPreference.SENT_KWH) {
                    if (uiState.showVat) p.priceWithVatCentsKwh else p.priceCentsKwh
                } else {
                    if (uiState.showVat) p.priceWithVatEurMwh else p.priceEurMwh
                }
            }
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .statusBarsPadding()
        ) {
            // Material 3 Expressive App Bar
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickableWithScale { showInfo = !showInfo }
                    ) {
                        Text("Vidinad")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                navigationIcon = {
                    // Spacer to reserve room for the shared root-level pivot button
                    Spacer(modifier = Modifier.size(48.dp))
                },
                actions = {
                    IconButton(
                        onClick = onRefreshWidget,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Värskenda vidinat"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { selectedOption = null },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                AnimatedVisibility(
                    visible = showInfo,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Text(
                        text = "Vidin (Widget) võimaldab näha olulisimaid andmeid otse koduekraanil rakendust avamata.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }

                OutlinedCard(
                    onClick = { isInstructionsExpanded = !isInstructionsExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScaleEffect()
                        .then(
                            if (isDark) Modifier.border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            ) else Modifier
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Kuidas lisada vidin koduekraanile?",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased(
                                    minFontSize = 10.sp,
                                    maxFontSize = MaterialTheme.typography.titleLarge.fontSize
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isInstructionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isInstructionsExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(
                            visible = isInstructionsExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "1. Vajuta pikalt koduekraani tühjale alale.\n" +
                                            "2. Vali 'Vidinad' (Widgets).\n" +
                                            "3. Otsi üles 'Vattson'.\n" +
                                            "4. Lohista vidin koduekraanile.\n\n" +
                                            "Või vali alt sobiv suurus ja vajuta 'Lisa ekraanile'.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vidina kujundus",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showThemeInfo = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val themeOptions = listOf(AppTheme.LIGHT, AppTheme.DARK, AppTheme.SYSTEM, AppTheme.DYNAMIC)
                ButtonGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    overflowIndicator = { menuState ->
                        ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                    },
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    themeOptions.forEachIndexed { index, theme ->
                        val isSelected = uiState.widgetTheme == theme
                        val position = when (index) {
                            0 -> ButtonGroupItemPosition.Leading
                            themeOptions.size - 1 -> ButtonGroupItemPosition.Trailing
                            else -> ButtonGroupItemPosition.Middle
                        }
                        filledToggleableItem(
                            checked = isSelected,
                            label = when (theme) {
                                AppTheme.LIGHT -> "Hele"
                                AppTheme.DARK -> "Tume"
                                AppTheme.SYSTEM -> "Süsteem"
                                AppTheme.DYNAMIC -> "Dünaamiline"
                            },
                            onCheckedChange = { onWidgetThemeChange(theme) },
                            weight = 1f,
                            position = position
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }

        BottomSheetScaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 8.dp),
            scaffoldState = scaffoldState,
            sheetPeekHeight = 80.dp,
            sheetShape = RoundedCornerShape(24.dp),
            sheetContainerColor = currentSheetColor,
            sheetTonalElevation = if (isDark) 0.5.dp else 0.dp,
            sheetShadowElevation = 12.dp,
            sheetDragHandle = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(if (isExpanded) R.drawable.ic_keyboard_arrow_down else R.drawable.ic_keyboard_arrow_up),
                        contentDescription = if (isExpanded) "Tõmba alla" else "Tõmba üles",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Vali vidin",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = widgetSizes,
                            key = { it.label },
                            contentType = { "widget_size_option" },
                            span = { GridItemSpan(it.width) }
                        ) { option ->
                            WidgetSizeCard(
                                option = option,
                                currentPrice = currentPriceText,
                                currencySuffix = currencySuffix,
                                cheapestWindow = cheapestWindowText,
                                nextPrices = liveNextPrices,
                                isSelected = selectedOption == option,
                                isDark = isDark,
                                onClick = { selectedOption = option },
                                onAddClick = {
                                    pinWidget(context, option)
                                    selectedOption = null
                                }
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) {
            Box(Modifier.fillMaxSize())
        }
    }
}

@Composable
fun WidgetSizeCard(
    option: WidgetSizeOption,
    currentPrice: String,
    currencySuffix: String,
    cheapestWindow: String,
    nextPrices: List<Double>,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(option.width.toFloat() / option.height.toFloat())
                .then(
                    if (isDark) Modifier.border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ) else Modifier
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                WidgetPreviewContent(
                    option = option,
                    currentPrice = currentPrice,
                    currencySuffix = currencySuffix,
                    cheapestWindow = cheapestWindow
                )
                
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = onAddClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lisa ekraanile", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun pinWidget(context: Context, option: WidgetSizeOption) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val receiverClass = when (option.label) {
        "1x1" -> PriceWidget1x1Receiver::class.java
        "2x1" -> PriceWidget2x1Receiver::class.java
        "3x1" -> PriceWidget3x1Receiver::class.java
        "4x1" -> PriceWidget4x1Receiver::class.java
        "Ring" -> DonutClockWidgetReceiver::class.java
        else -> PriceWidget4x1Receiver::class.java
    }
    val myProvider = ComponentName(context, receiverClass)

    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        appWidgetManager.requestPinAppWidget(myProvider, null, null)
    }
}

@Composable
fun WidgetPreviewContent(
    option: WidgetSizeOption,
    currentPrice: String,
    currencySuffix: String,
    cheapestWindow: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (option.label == "Ring") {
            Icon(
                painter = painterResource(R.drawable.ic_price_change),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            return@Box
        }
        if (option.label == "1x1") {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Hind", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = currentPrice,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = currencySuffix,
                        fontSize = 7.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            return@Box
        }

        val labelSize = 10.sp
        val priceSize = when (option.width) {
            2 -> 14.sp
            3 -> 16.sp
            else -> 18.sp
        }
        val unitSize = (priceSize.value * 0.55f).sp
        
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text("Hind", fontSize = labelSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = currentPrice,
                        fontSize = priceSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = currencySuffix,
                        fontSize = unitSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Soodsaim", fontSize = labelSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = cheapestWindow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WidgetScreenPreview() {
    VattsonTheme {
        WidgetScreen(
            uiState = MainUiState(),
            onRefreshWidget = {},
            onWidgetThemeChange = {},
            onAppBarVisibilityChange = {}
        )
    }
}
