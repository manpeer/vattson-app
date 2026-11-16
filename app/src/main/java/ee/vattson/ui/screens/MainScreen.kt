@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package ee.vattson.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ee.vattson.R
import ee.vattson.data.model.AppTheme
import ee.vattson.data.model.CurrencyPreference
import ee.vattson.data.model.DatePreference
import ee.vattson.data.model.ElectricityPrice
import ee.vattson.data.model.GraphType
import ee.vattson.data.model.PriceResolution
import ee.vattson.data.repository.EleringRepository
import ee.vattson.data.repository.SettingsRepository
import ee.vattson.ui.components.ButtonGroupItemPosition
import ee.vattson.ui.components.PriceCard
import ee.vattson.ui.components.PriceChart
import ee.vattson.ui.components.PriceTableHeader
import ee.vattson.ui.components.PriceTableRow
import ee.vattson.ui.components.SummaryItem
import ee.vattson.ui.components.choiceBoxClickable
import ee.vattson.ui.components.filledToggleableItem
import ee.vattson.ui.components.pressScaleEffect
import ee.vattson.ui.theme.VattsonTheme
import ee.vattson.ui.viewmodel.MainUiState
import ee.vattson.ui.viewmodel.MainViewModel
import ee.vattson.ui.viewmodel.SummaryData
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    val iconResId: Int? = null
) {
    object Prices : Screen("prices", "Hinnad", icon = Icons.Default.Euro)
    object Notifications : Screen("notifications", "Teavitused", Icons.Default.Notifications)
    object Widget : Screen("widget", "Vidin", iconResId = R.drawable.ic_widgets)
    object Settings : Screen("settings", "Seaded", Icons.Default.Settings)
}

class MainViewModelFactory(
    private val application: Application,
    private val eleringRepository: EleringRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, eleringRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun MainScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val screens = listOf(Screen.Prices, Screen.Notifications, Screen.Widget, Screen.Settings)

    val configuration = LocalConfiguration.current
    val isExpandedWindow = configuration.screenWidthDp >= 600

    // Root-level manual expansion state to allow layering button ABOVE rail on compact screens
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    
    // Track the visibility of the App Bar to hide the menu button when scrolled
    var appBarVisibleFraction by rememberSaveable { mutableFloatStateOf(1f) }

    if (isExpandedWindow) {
        Row(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(200.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 16.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Vattson",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 16.dp)
                    )
                    screens.forEach { screen ->
                        WideNavigationRailItem(
                            modifier = Modifier.pressScaleEffect(),
                            icon = {
                                if (screen.icon != null) {
                                    Icon(screen.icon, contentDescription = null)
                                } else if (screen.iconResId != null) {
                                    Icon(
                                        painterResource(screen.iconResId),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            },
                            railExpanded = true,
                            iconPosition = NavigationItemIconPosition.Start
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Prices.route,
                    enterTransition = { fadeIn(animationSpec = tween(220)) },
                    exitTransition = { fadeOut(animationSpec = tween(180)) }
                ) {
                    composable(Screen.Prices.route) {
                        MainScreenContent(
                            uiState = uiState,
                            onCurrencyChange = { viewModel.setCurrencyPreference(it) },
                            onResolutionChange = { viewModel.setPriceResolution(it) },
                            onDatePreferenceChange = { viewModel.setDatePreference(it) },
                            onGraphTypeChange = { viewModel.setGraphType(it) },
                            onFullscreenToggle = { viewModel.setChartFullscreen(it) },
                            onCheapestWindowDurationChange = { viewModel.setCheapestWindowDuration(it) },
                            onRefresh = { viewModel.refreshPrices() },
                            onOpenSettings = {
                                navController.navigate(Screen.Settings.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            },
                            onAppBarVisibilityChange = { appBarVisibleFraction = it }
                        )
                    }
                    composable(Screen.Notifications.route) {
                        NotificationScreen(
                            notificationsEnabled = uiState.notificationsEnabled,
                            priceNotifications = uiState.priceNotifications,
                            currencyPreference = uiState.currencyPreference,
                            onNotificationsEnabledChange = { viewModel.setNotificationsEnabled(it) },
                            onAddNotification = { threshold, type, unit -> viewModel.addPriceNotification(threshold, type, unit) },
                            onRemoveNotification = { id -> viewModel.removePriceNotification(id) },
                            onAppBarVisibilityChange = { appBarVisibleFraction = it }
                        )
                    }
                    composable(Screen.Widget.route) {
                        WidgetScreen(
                            uiState = uiState,
                            onRefreshWidget = { viewModel.refreshPrices() },
                            onWidgetThemeChange = { viewModel.setWidgetTheme(it) },
                            onAppBarVisibilityChange = { appBarVisibleFraction = it }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onAppBarVisibilityChange = { appBarVisibleFraction = it }
                        )
                    }
                }
            }
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Prices.route,
                enterTransition = { fadeIn(animationSpec = tween(220)) + slideInHorizontally { it / 4 } },
                exitTransition = { fadeOut(animationSpec = tween(180)) + slideOutHorizontally { -it / 4 } },
                popEnterTransition = { fadeIn(animationSpec = tween(220)) + slideInHorizontally { -it / 4 } },
                popExitTransition = { fadeOut(animationSpec = tween(180)) + slideOutHorizontally { it / 4 } }
            ) {
                composable(Screen.Prices.route) {
                    MainScreenContent(
                        uiState = uiState,
                        onCurrencyChange = { viewModel.setCurrencyPreference(it) },
                        onResolutionChange = { viewModel.setPriceResolution(it) },
                        onDatePreferenceChange = { viewModel.setDatePreference(it) },
                        onGraphTypeChange = { viewModel.setGraphType(it) },
                        onFullscreenToggle = { viewModel.setChartFullscreen(it) },
                        onCheapestWindowDurationChange = { viewModel.setCheapestWindowDuration(it) },
                        onRefresh = { viewModel.refreshPrices() },
                        onOpenSettings = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        onAppBarVisibilityChange = { appBarVisibleFraction = it }
                    )
                }
                composable(Screen.Notifications.route) {
                    NotificationScreen(
                        notificationsEnabled = uiState.notificationsEnabled,
                        priceNotifications = uiState.priceNotifications,
                        currencyPreference = uiState.currencyPreference,
                        onNotificationsEnabledChange = { viewModel.setNotificationsEnabled(it) },
                        onAddNotification = { threshold, type, unit -> viewModel.addPriceNotification(threshold, type, unit) },
                        onRemoveNotification = { id -> viewModel.removePriceNotification(id) },
                        onAppBarVisibilityChange = { appBarVisibleFraction = it }
                    )
                }
                composable(Screen.Widget.route) {
                    WidgetScreen(
                        uiState = uiState,
                        onRefreshWidget = { viewModel.refreshPrices() },
                        onWidgetThemeChange = { viewModel.setWidgetTheme(it) },
                        onAppBarVisibilityChange = { appBarVisibleFraction = it }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onAppBarVisibilityChange = { appBarVisibleFraction = it }
                    )
                }
            }

            // Shared Scrim Overlay
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isExpanded = false }
                )
            }

            // Root-level Custom Rail UI (Layered BELOW stationary button)
            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
                ) { -it },
                exit = slideOutHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
                ) { -it }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(200.dp),
                    shape = WideNavigationRailDefaults.modalExpandedShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 76.dp) // Clearance for the stationary root button
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp)
                    ) {
                        screens.forEach { screen ->
                            WideNavigationRailItem(
                                modifier = Modifier.pressScaleEffect(),
                                icon = {
                                    if (screen.icon != null) {
                                        Icon(screen.icon, contentDescription = null)
                                    } else if (screen.iconResId != null) {
                                        Icon(
                                            painterResource(screen.iconResId),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                label = { Text(screen.label) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    isExpanded = false
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                },
                                railExpanded = true,
                                iconPosition = NavigationItemIconPosition.Start
                            )
                        }
                    }
                }
            }

            // Root-level Stationary Menu Button (Highest layer)
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 4.dp, top = 8.dp)
                    .graphicsLayer {
                        // Hide the button when App Bar is scrolled away, but keep it visible if rail is open
                        val fraction = if (isExpanded) 1f else appBarVisibleFraction
                        alpha = fraction
                        scaleX = fraction.coerceAtLeast(0.7f)
                        scaleY = fraction.coerceAtLeast(0.7f)
                        translationY = (1f - fraction) * -20.dp.toPx()
                    }
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                    label = "MenuButtonScale"
                )

                val isMenuButtonEnabled by remember {
                    derivedStateOf { isExpanded || appBarVisibleFraction > 0.5f }
                }

                IconToggleButton(
                    checked = isExpanded,
                    onCheckedChange = { isExpanded = it },
                    interactionSource = interactionSource,
                    enabled = isMenuButtonEnabled,
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        checkedContainerColor = Color.Transparent,
                        checkedContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shapes = IconButtonDefaults.toggleableShapes(),
                    modifier = Modifier.graphicsLayer {
                        scaleX *= scale
                        scaleY *= scale
                    }
                ) {
                    AnimatedMenuIcon(isExpanded)
                }
            }
        }
    }
}

@Composable
fun AnimatedMenuIcon(isExpanded: Boolean) {
    val rotationState = animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = Spring.StiffnessLow
        ),
        label = "MenuRotation"
    )
    
    Box(modifier = Modifier.graphicsLayer { rotationZ = rotationState.value }) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) togetherWith 
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) using 
                        SizeTransform(clip = false)
            },
            label = "MenuIconAnimation"
        ) { expanded ->
            Icon(
                imageVector = if (expanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                contentDescription = if (expanded) "Sulge menüü" else "Ava menüü",
                modifier = if (expanded) Modifier.graphicsLayer { rotationZ = -180f } else Modifier
            )
        }
    }
}

private data class TableRowData(
    val price: ElectricityPrice,
    val previousPrice: ElectricityPrice?,
    val isInCheapestWindow: Boolean,
    val isCheapestPrice: Boolean
)

private val estonianSymbols = DecimalFormatSymbols(Locale.forLanguageTag("et-EE")).apply {
    decimalSeparator = ','
}
private val summaryPriceFormatter = DecimalFormat("#,##0.00", estonianSymbols)

@SuppressLint("LocalContextResourcesRead")
@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onCurrencyChange: (CurrencyPreference) -> Unit,
    onResolutionChange: (PriceResolution) -> Unit,
    onDatePreferenceChange: (DatePreference) -> Unit,
    onGraphTypeChange: (GraphType) -> Unit,
    onFullscreenToggle: (Boolean) -> Unit,
    onCheapestWindowDurationChange: (Int) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onAppBarVisibilityChange: (Float) -> Unit
) {
    // Memoized format tools to avoid recomposition overhead
    val priceFormatter = remember { DecimalFormat("#,##0.000", estonianSymbols) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // Memoized lastUpdatedText to avoid expensive formatting on every recomposition
    val lastUpdatedText = remember(uiState.lastFetchTimestamp) {
        if (uiState.lastFetchTimestamp == 0L) "Pole uuendatud"
        else {
            val lastUpdated = Instant.ofEpochMilli(uiState.lastFetchTimestamp).atZone(ZoneId.systemDefault())
            val today = java.time.LocalDate.now()
            val formatter = if (lastUpdated.toLocalDate() == today) {
                DateTimeFormatter.ofPattern("HH:mm")
            } else {
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            }
            lastUpdated.format(formatter)
        }
    }


    val tableMinPrice = remember(uiState.chartData, uiState.currencyPreference, uiState.showVat) {
        uiState.chartData.minOfOrNull { it.resolvedPrice(uiState.currencyPreference, uiState.showVat) }
    }
    val tableMaxPrice = remember(uiState.chartData, uiState.currencyPreference, uiState.showVat) {
        uiState.chartData.maxOfOrNull { it.resolvedPrice(uiState.currencyPreference, uiState.showVat) }
    }
    val cheapestPriceIndices = remember(uiState.chartData, uiState.currencyPreference, uiState.showVat) {
        if (uiState.chartData.isEmpty()) emptySet()
        else uiState.chartData
            .map { it.localDateTime.toLocalDate() }
            .distinct()
            .flatMap { d ->
                val rows = uiState.chartData.filter { it.localDateTime.toLocalDate() == d }
                if (rows.isEmpty()) emptyList()
                else listOf(
                    uiState.chartData.indexOf(
                        rows.minByOrNull { it.resolvedPrice(uiState.currencyPreference, uiState.showVat) }!!
                    )
                )
            }
            .toSet()
    }

    val tableSections = remember(
        uiState.chartData,
        uiState.cheapestWindowStarts,
        uiState.cheapestWindowSize,
        cheapestPriceIndices
    ) {
        if (uiState.chartData.isEmpty()) {
            emptyList<Pair<java.time.LocalDate, List<TableRowData>>>()
        } else {
            val windowStarts = uiState.cheapestWindowStarts
            val windowSize = uiState.cheapestWindowSize
            val chartDataIndices = uiState.chartData.indices

            uiState.chartData.mapIndexed { flatIndex, p ->
                val previousPrice = if (flatIndex > 0) uiState.chartData[flatIndex - 1] else null
                val isInCheapestWindow = windowStarts.any { start ->
                    start in chartDataIndices && flatIndex >= start && flatIndex < start + windowSize
                }
                val isCheapest = flatIndex in cheapestPriceIndices
                TableRowData(p, previousPrice, isInCheapestWindow, isCheapest)
            }
            .groupBy { it.price.localDateTime.toLocalDate() }
            .toList()
        }
    }

    val listState = rememberLazyListState()
    val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isDarkTheme = when (uiState.appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM, AppTheme.DYNAMIC -> isSystemInDarkTheme()
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    // Report App Bar visibility to parent
    val appBarVisibleFraction by remember {
        derivedStateOf { 1f - scrollBehavior.state.collapsedFraction }
    }
    androidx.compose.runtime.LaunchedEffect(appBarVisibleFraction) {
        onAppBarVisibilityChange(appBarVisibleFraction)
    }

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenHeightDp = with(density) { windowInfo.containerSize.height.toDp() }

    val isPortraitBar = uiState.selectedGraphType == GraphType.BAR && !isLandscape && uiState.datePreference != DatePreference.BOTH

    // Reset App Bar expansion when switching to a non-scrollable view (Portrait Chart)
    androidx.compose.runtime.LaunchedEffect(isPortraitBar) {
        if (isPortraitBar) {
            scrollBehavior.state.heightOffset = 0f
            scrollBehavior.state.contentOffset = 0f
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Elektri Börsihinnad",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Viimati uuendatud: $lastUpdatedText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val infiniteTransition = rememberInfiniteTransition(label = "RefreshRotation")
                            val refreshRotationAngle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "RefreshAngle"
                            )

                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier.size(18.dp).pressScaleEffect()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Värskenda",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .graphicsLayer {
                                            rotationZ = if (uiState.isLoading) refreshRotationAngle else 0f
                                        }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    // Spacer to reserve room for the shared root-level pivot button
                    Spacer(modifier = Modifier.size(48.dp))
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Seaded")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                userScrollEnabled = true
            ) {
                // Unified Item structure for smoothness: 
                // Header is always in the first logical item key.
                item(key = "top_header") {
                    Column(
                        modifier = if (isPortraitBar) Modifier.fillParentMaxHeight() else Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        MainScreenHeader(
                            currentPrice = uiState.currentPrice,
                            nextPrice = uiState.nextPrice,
                            currencyPreference = uiState.currencyPreference,
                            showVat = uiState.showVat,
                            datePreference = uiState.datePreference,
                            priceResolution = uiState.priceResolution,
                            selectedGraphType = uiState.selectedGraphType,
                            todaySummary = uiState.todaySummary,
                            tomorrowSummary = uiState.tomorrowSummary,
                            onCurrencyChange = onCurrencyChange,
                            onResolutionChange = onResolutionChange,
                            onDatePreferenceChange = onDatePreferenceChange,
                            onGraphTypeChange = onGraphTypeChange,
                            onFullscreenToggle = onFullscreenToggle,
                            onCheapestWindowDurationChange = onCheapestWindowDurationChange,
                            timeFormatter = timeFormatter
                        )

                        if (isPortraitBar) {
                            PriceChart(
                                data = uiState.chartData,
                                currencyPreference = uiState.currencyPreference,
                                showVat = uiState.showVat,
                                appTheme = uiState.appTheme,
                                cheapestWindowStarts = uiState.cheapestWindowStarts,
                                cheapestWindowSize = uiState.cheapestWindowSize,
                                isChartFullscreen = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )

                            uiState.error?.let { error ->
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                if (!isPortraitBar) {
                    if (uiState.selectedGraphType == GraphType.BAR) {
                        if (uiState.datePreference == DatePreference.BOTH || isLandscape) {
                            item(key = "chart_both") {
                                PriceChart(
                                    data = uiState.chartData,
                                    currencyPreference = uiState.currencyPreference,
                                    showVat = uiState.showVat,
                                    appTheme = uiState.appTheme,
                                    cheapestWindowStarts = uiState.cheapestWindowStarts,
                                    cheapestWindowSize = uiState.cheapestWindowSize,
                                    isChartFullscreen = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(
                                            if (uiState.datePreference == DatePreference.BOTH) 300.dp else (screenHeightDp - 32.dp).coerceIn(
                                                100.dp,
                                                310.dp
                                            )
                                        )
                                )
                            }

                            uiState.error?.let { error ->
                                item {
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.selectedGraphType == GraphType.TABLE) {
                        if (uiState.chartData.isEmpty()) {
                            item(key = "table_empty") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Hinnad puuduvad",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            tableSections.forEach { (date, rows) ->
                                stickyHeader(key = "table_date_header_${date}") {
                                    PriceTableHeader(
                                        currencyPreference = uiState.currencyPreference,
                                        showVat = uiState.showVat,
                                        date = date
                                    )
                                }

                                itemsIndexed(
                                    rows,
                                    key = { _, item -> item.price.timestamp.toEpochMilli() },
                                    contentType = { _, _ -> "price_row" }
                                ) { _, item ->
                                    PriceTableRow(
                                        p = item.price,
                                        currencyPreference = uiState.currencyPreference,
                                        showVat = uiState.showVat,
                                        currentPrice = uiState.currentPrice,
                                        previousPrice = item.previousPrice,
                                        minPrice = tableMinPrice,
                                        maxPrice = tableMaxPrice,
                                        isInCheapestWindow = item.isInCheapestWindow,
                                        isCheapestPrice = item.isCheapestPrice,
                                        formatter = priceFormatter,
                                        timeFormatter = timeFormatter
                                    )
                                }
                            }
                        }

                        uiState.error?.let { error ->
                            item {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showButton && uiState.selectedGraphType == GraphType.TABLE,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val fabScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                    label = "FabScale"
                )

                FilledIconButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .height(32.dp)
                        .pressScaleEffect()
                        .graphicsLayer {
                            scaleX = fabScale
                            scaleY = fabScale
                        }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_upward),
                        contentDescription = "Tagasi üles",
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (uiState.isChartFullscreen) {
                FullscreenChartDialog(
                    uiState = uiState,
                    isDarkTheme = isDarkTheme,
                    onFullscreenToggle = onFullscreenToggle
                )
            }
        }
    }
}

@Composable
private fun FullscreenChartDialog(
    uiState: MainUiState,
    isDarkTheme: Boolean,
    onFullscreenToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }

    var isMarkerNearExitButton by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }
    Dialog(
        onDismissRequest = { onFullscreenToggle(false) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        val activity = context.findActivity()
        val view = LocalView.current
        DisposableEffect(activity, view, isDarkTheme) {
            val window = (view.parent as? DialogWindowProvider)?.window ?: activity?.window
            if (window != null) {
                val params = window.attributes
                params.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                window.attributes = params
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !isDarkTheme
            }
            onDispose {}
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .displayCutoutPadding()
            ) {
                PriceChart(
                    data = uiState.chartData,
                    currencyPreference = uiState.currencyPreference,
                    showVat = uiState.showVat,
                    appTheme = uiState.appTheme,
                    cheapestWindowStarts = uiState.cheapestWindowStarts,
                    cheapestWindowSize = uiState.cheapestWindowSize,
                    isChartFullscreen = true,
                    onMarkerTargetsChanged = { targets ->
                        val buttonAreaWidth = 120.dp // Buffer area near the top-right
                        
                        isMarkerNearExitButton = !targets.isNullOrEmpty() && targets.any { target ->
                            val xDp = with(density) { target.canvasX.toDp() }
                            xDp > (screenWidth - buttonAreaWidth)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            AnimatedVisibility(
                visible = !isMarkerNearExitButton,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 4.dp, end = 12.dp)
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.85f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                    label = "ExitButtonScale"
                )

                IconButton(
                    onClick = { onFullscreenToggle(false) },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fullscreen_exit),
                        contentDescription = "Sulge täisekraan",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreenHeader(
    currentPrice: ElectricityPrice?,
    nextPrice: ElectricityPrice?,
    currencyPreference: CurrencyPreference,
    showVat: Boolean,
    datePreference: DatePreference,
    priceResolution: PriceResolution,
    selectedGraphType: GraphType,
    todaySummary: SummaryData?,
    tomorrowSummary: SummaryData?,
    onCurrencyChange: (CurrencyPreference) -> Unit,
    onResolutionChange: (PriceResolution) -> Unit,
    onDatePreferenceChange: (DatePreference) -> Unit,
    onGraphTypeChange: (GraphType) -> Unit,
    onFullscreenToggle: (Boolean) -> Unit,
    onCheapestWindowDurationChange: (Int) -> Unit,
    timeFormatter: DateTimeFormatter
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    HorizontalDivider(
        thickness = 1.5.dp,
        color = if (isDark) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary
    )

    Column(modifier = Modifier.padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        PriceCard(
            price = currentPrice,
            nextPrice = nextPrice,
            currencyPreference = currencyPreference,
            showVat = showVat,
            onCurrencyChange = onCurrencyChange,
            onResolutionChange = onResolutionChange
        )

        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth(0.95f),
            thickness = 1.dp,
            color = if (isDark) MaterialTheme.colorScheme.outline.copy(0.2f) else MaterialTheme.colorScheme.outline.copy(
                0.4f
            )
        )

        ButtonGroup(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .height(34.dp)
                .fillMaxWidth(0.8f),
            overflowIndicator = { menuState ->
                ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
            },
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            DatePreference.entries.forEachIndexed { index, pref ->
                val isSelected = datePreference == pref || datePreference == DatePreference.BOTH
                val position = when (index) {
                    0 -> ButtonGroupItemPosition.Leading
                    DatePreference.entries.size - 1 -> ButtonGroupItemPosition.Trailing
                    else -> ButtonGroupItemPosition.Middle
                }
                filledToggleableItem(
                    checked = isSelected,
                    onCheckedChange = { onDatePreferenceChange(pref) },
                    label = if (pref == DatePreference.BOTH) "" else pref.label,
                    menuLabel = pref.label,
                    icon = if (pref == DatePreference.BOTH) {
                        { 
                            Icon(
                                imageVector = if (datePreference == DatePreference.BOTH) Icons.Outlined.Link else Icons.Outlined.AddLink, 
                                contentDescription = "Mõlemad",
                                modifier = Modifier.size(26.dp)
                            ) 
                        }
                    } else null,
                    weight = if (pref == DatePreference.BOTH) 0.5f else 1f,
                    position = position
                )
            }
        }

        if (datePreference == DatePreference.BOTH) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "TÄNA",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
            SummarySection(
                summaryData = todaySummary,
                currencyPreference = currencyPreference,
                showVat = showVat,
                priceResolution = priceResolution,
                timeFormatter = timeFormatter,
                onCheapestWindowDurationChange = onCheapestWindowDurationChange,
                isDark = isDark
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 8.dp, bottom = 2.dp),
                thickness = 1.dp,
                color = if (isDark) MaterialTheme.colorScheme.outline.copy(0.1f) else MaterialTheme.colorScheme.outline.copy(
                    0.2f
                )
            )
            Text(
                text = "HOMME",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
            SummarySection(
                summaryData = tomorrowSummary,
                currencyPreference = currencyPreference,
                showVat = showVat,
                priceResolution = priceResolution,
                timeFormatter = timeFormatter,
                onCheapestWindowDurationChange = onCheapestWindowDurationChange,
                isDark = isDark
            )
        } else {
            val summary = when (datePreference) {
                DatePreference.TODAY -> todaySummary
                DatePreference.TOMORROW -> tomorrowSummary
            }; if (datePreference == DatePreference.TODAY) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TÄNA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (datePreference == DatePreference.TOMORROW) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "HOMME",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            SummarySection(
                summaryData = summary,
                currencyPreference = currencyPreference,
                showVat = showVat,
                priceResolution = priceResolution,
                timeFormatter = timeFormatter,
                onCheapestWindowDurationChange = onCheapestWindowDurationChange,
                isDark = isDark
            )
        }

        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth(0.95f),
            thickness = 1.dp,
            color = if (isDark) MaterialTheme.colorScheme.outline.copy(0.2f) else MaterialTheme.colorScheme.outline.copy(
                0.4f
            )
        )

        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val types = remember { listOf(GraphType.TABLE, GraphType.BAR) }
            ButtonGroup(
                modifier = Modifier
                    .height(34.dp)
                    .fillMaxWidth(0.6f),
                overflowIndicator = { menuState ->
                    ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                },
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                types.forEachIndexed { index, type ->
                    val isSelected = selectedGraphType == type
                    val position = when (index) {
                        0 -> ButtonGroupItemPosition.Leading
                        types.size - 1 -> ButtonGroupItemPosition.Trailing
                        else -> ButtonGroupItemPosition.Middle
                    }
                    filledToggleableItem(
                        checked = isSelected,
                        onCheckedChange = { onGraphTypeChange(type) },
                        label = type.label,
                        weight = 1f,
                        position = position
                    )
                }
            }

            if (selectedGraphType == GraphType.BAR) {
                IconButton(
                    onClick = { onFullscreenToggle(true) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fullscreen),
                        contentDescription = "Täisekraan",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
fun SummarySection(
    summaryData: SummaryData?,
    currencyPreference: CurrencyPreference,
    showVat: Boolean,
    priceResolution: PriceResolution,
    timeFormatter: DateTimeFormatter,
    onCheapestWindowDurationChange: (Int) -> Unit,
    isDark: Boolean
) {
    if (summaryData == null) return

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp)
                .then(
                    if (isDark) Modifier.border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    ) else Modifier
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isDark) Modifier.border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        ) else Modifier.border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp)
                        )
                    )
                    .height(IntrinsicSize.Max),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryItem(
                    label = "Kõrgeim",
                    value = summaryData.maxPriceItem?.let { p ->
                        if (currencyPreference == CurrencyPreference.SENT_KWH) {
                            if (showVat) p.priceWithVatCentsKwh else p.priceCentsKwh
                        } else {
                            if (showVat) p.priceWithVatEurMwh else p.priceEurMwh
                        }
                    },
                    currencyPreference = currencyPreference,
                    timePeriod = summaryData.maxPriceItem?.let { p ->
                        val start = p.localDateTime
                        val end =
                            if (p.resolution == PriceResolution.HOUR_1) start.plusHours(1) else start.plusMinutes(
                                15
                            )
                        "${timeFormatter.format(start)}-${timeFormatter.format(end)}"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isDark) Color(0xFFC62828).copy(alpha = 0.05f) else Color(
                                0xFFFFEBEE
                            ).copy(alpha = 0.5f)
                        )
                )

                VerticalDivider(
                    modifier = Modifier,
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                SummaryItem(
                    label = "Madalaim",
                    value = summaryData.minPriceItem?.let { p ->
                        if (currencyPreference == CurrencyPreference.SENT_KWH) {
                            if (showVat) p.priceWithVatCentsKwh else p.priceCentsKwh
                        } else {
                            if (showVat) p.priceWithVatEurMwh else p.priceEurMwh
                        }
                    },
                    currencyPreference = currencyPreference,
                    timePeriod = summaryData.minPriceItem?.let { p ->
                        val start = p.localDateTime
                        val end =
                            if (p.resolution == PriceResolution.HOUR_1) start.plusHours(1) else start.plusMinutes(
                                15
                            )
                        "${timeFormatter.format(start)}-${timeFormatter.format(end)}"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isDark) Color(0xFF2E7D32).copy(alpha = 0.1f) else Color(0xFFE8F5E9)
                        )
                )

                VerticalDivider(
                    modifier = Modifier,
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                SummaryItem(
                    label = "Keskmine",
                    value = summaryData.avgPrice,
                    currencyPreference = currencyPreference,
                    timePeriod = summaryData.dateString,
                    modifier = Modifier
                        .weight(1f)
                )
            }
        }

        if (summaryData.cheapestWindowTimeString != null) {
            var durationMenuExpanded by remember { mutableStateOf(false) }
            val displayDuration = if (priceResolution == PriceResolution.HOUR_1) summaryData.cheapestWindowSize else summaryData.cheapestWindowSize / 4
            
            ElevatedCard(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .then(
                        if (isDark) Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(24.dp)
                        ) else Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(24.dp)
                        )
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isDark) Color(0xFF1B2D1B) else Color(0xFFE8F5E9)
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp)
                        )
                        Text(text = "Soodsaimad ", style = MaterialTheme.typography.bodyMedium)

                        Box {
                            Row(
                                modifier = Modifier
                                    .background(
                                        color = if (isDark) Color(0xFF1B2D1B) else Color(
                                            0xFFE8F5E9
                                        ), shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .choiceBoxClickable { durationMenuExpanded = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$displayDuration",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_drop_down),
                                    contentDescription = "Muuda kestust",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(expanded = durationMenuExpanded, onDismissRequest = { durationMenuExpanded = false }) {
                                (2..6).forEach { duration ->
                                    DropdownMenuItem(text = { Text("$duration tundi") }, onClick = { onCheapestWindowDurationChange(duration); durationMenuExpanded = false })
                                }
                            }
                        }

                        Text(
                            text = buildAnnotatedString {
                                append(" tundi: ")
                                withStyle(
                                    SpanStyle(
                                        fontWeight = Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    append(summaryData.cheapestWindowTimeString)
                                }
                            },
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    summaryData.cheapestWindowAvgPrice?.let { avg ->
                        val avgPriceText = remember(avg, currencyPreference) {
                            synchronized(summaryPriceFormatter) {
                                summaryPriceFormatter.format(avg)
                            }
                        }
                        
                        Text(
                            text = buildAnnotatedString {
                                append("Keskmine hind sel perioodil: ")
                                withStyle(
                                    SpanStyle(
                                        fontWeight = Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    append("$avgPriceText ${currencyPreference.shortLabel}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@FormFactorPreviews
@Composable
fun MainScreenPreview() {
    val now = Instant.now()
    val dummyPrice = ElectricityPrice(
        timestamp = now,
        resolution = PriceResolution.HOUR_1,
        priceEurMwh = 100.0,
        priceWithVatEurMwh = 124.0,
        priceCentsKwh = 10.0,
        priceWithVatCentsKwh = 12.4
    )
    val dummyNextPrice = ElectricityPrice(
        timestamp = now.plusSeconds(3600),
        resolution = PriceResolution.HOUR_1,
        priceEurMwh = 150.0,
        priceWithVatEurMwh = 186.0,
        priceCentsKwh = 15.0,
        priceWithVatCentsKwh = 18.6
    )

    val dummySummary = SummaryData(
        minPriceItem = dummyPrice,
        maxPriceItem = dummyNextPrice,
        avgPrice = 12.5,
        cheapestWindowStart = 0,
        cheapestWindowSize = 4,
        cheapestWindowTimeString = "00:00-04:00",
        cheapestWindowAvgPrice = 5.0,
        dateString = "06.08 Neljapäev"
    )

    val dummyState = MainUiState(
        currentPrice = dummyPrice,
        nextPrice = dummyNextPrice,
        todaySummary = dummySummary,
        tomorrowSummary = dummySummary.copy(dateString = "07.08 Reede"),
        priceResolution = PriceResolution.HOUR_1,
        datePreference = DatePreference.BOTH,
        selectedGraphType = GraphType.BAR,
        chartData = listOf(dummyPrice, dummyNextPrice),
        cheapestWindowStarts = listOf(0),
        cheapestWindowSize = 4,
        isLoading = false,
        error = null
    )
    VattsonTheme {
        MainScreenContent(
            uiState = dummyState,
            onCurrencyChange = {},
            onResolutionChange = {},
            onDatePreferenceChange = {},
            onGraphTypeChange = {},
            onFullscreenToggle = {},
            onCheapestWindowDurationChange = {},
            onRefresh = {},
            onOpenSettings = {},
            onAppBarVisibilityChange = {}
        )
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview(showBackground = true, apiLevel = 37)
@Composable
fun MainScreenSmallPreview() {
    val now = Instant.now()
    val dummyPrice = ElectricityPrice(
        timestamp = now,
        resolution = PriceResolution.HOUR_1,
        priceEurMwh = 80.0,
        priceWithVatEurMwh = 99.2,
        priceCentsKwh = 8.0,
        priceWithVatCentsKwh = 9.92
    )
    val dummyState = MainUiState(
        currentPrice = dummyPrice,
        todaySummary = SummaryData(
            minPriceItem = dummyPrice,
            maxPriceItem = dummyPrice,
            avgPrice = 8.0,
            cheapestWindowStart = 0,
            cheapestWindowSize = 4,
            cheapestWindowTimeString = "02:00-06:00",
            cheapestWindowAvgPrice = 4.5,
            dateString = "06.08 Neljapäev"
        ),
        datePreference = DatePreference.TODAY
    )
    VattsonTheme {
        MainScreenContent(
            uiState = dummyState,
            onCurrencyChange = {},
            onResolutionChange = {},
            onDatePreferenceChange = {},
            onGraphTypeChange = {},
            onFullscreenToggle = {},
            onCheapestWindowDurationChange = {},
            onRefresh = {},
            onOpenSettings = {},
            onAppBarVisibilityChange = {}
        )
    }
}
