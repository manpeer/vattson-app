package ee.vattson.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import ee.vattson.data.model.AppTheme
import ee.vattson.data.model.PriceResolution
import ee.vattson.ui.components.ButtonGroupItemPosition
import ee.vattson.ui.components.clickableWithScale
import ee.vattson.ui.components.filledToggleableItem
import ee.vattson.ui.viewmodel.MainUiState
import ee.vattson.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onAppBarVisibilityChange: (Float) -> Unit
) {
    SettingsScreenContent(
        uiState = uiState,
        onShowVatChange = { viewModel.setShowVat(it) },
        onPriceResolutionChange = { viewModel.setPriceResolution(it) },
        onAppThemeChange = { viewModel.setAppTheme(it) },
        onAppBarVisibilityChange = onAppBarVisibilityChange
    )
}

@Composable
private fun SettingsScreenContent(
    uiState: MainUiState,
    onShowVatChange: (Boolean) -> Unit,
    onPriceResolutionChange: (PriceResolution) -> Unit,
    onAppThemeChange: (AppTheme) -> Unit,
    onAppBarVisibilityChange: (Float) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    // Report App Bar visibility to parent
    val appBarVisibleFraction by androidx.compose.runtime.remember(scrollBehavior.state.collapsedFraction) {
        androidx.compose.runtime.derivedStateOf { 1f - scrollBehavior.state.collapsedFraction }
    }
    androidx.compose.runtime.LaunchedEffect(appBarVisibleFraction) {
        onAppBarVisibilityChange(appBarVisibleFraction)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Material 3 Expressive App Bar
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Seaded",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = Bold
                    )
                },
                navigationIcon = {
                    // Spacer to reserve room for the shared root-level pivot button
                    Spacer(modifier = Modifier.size(48.dp))
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
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            SettingsSection(title = "Üldised seaded") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableWithScale { onShowVatChange(!uiState.showVat) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hinnad käibemaksuga",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = uiState.showVat,
                        onCheckedChange = { onShowVatChange(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Hinna resolutsioon",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    ButtonGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        overflowIndicator = { menuState ->
                            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                        },
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        PriceResolution.entries.forEachIndexed { index, resolution ->
                            val isSelected = uiState.priceResolution == resolution
                            val position = when (index) {
                                0 -> ButtonGroupItemPosition.Leading
                                PriceResolution.entries.size - 1 -> ButtonGroupItemPosition.Trailing
                                else -> ButtonGroupItemPosition.Middle
                            }
                            filledToggleableItem(
                                checked = isSelected,
                                label = if (resolution == PriceResolution.HOUR_1) "1 tund" else "15 minutit",
                                onCheckedChange = { onPriceResolutionChange(resolution) },
                                weight = 1f,
                                position = position
                            )
                        }
                    }
                }
            }

            SettingsSection(title = "Rakenduse välimus") {
                val appThemeOptions =
                    AppTheme.entries
                ButtonGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(vertical = 8.dp),
                    overflowIndicator = { menuState ->
                        ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                    },
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    appThemeOptions.forEachIndexed { index, theme ->
                        val isSelected = uiState.appTheme == theme
                        val position = when (index) {
                            0 -> ButtonGroupItemPosition.Leading
                            appThemeOptions.size - 1 -> ButtonGroupItemPosition.Trailing
                            else -> ButtonGroupItemPosition.Middle
                        }
                        filledToggleableItem(
                            checked = isSelected,
                            label = theme.label,
                            onCheckedChange = { onAppThemeChange(theme) },
                            weight = 1f,
                            position = position
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = Bold
        )
        content()
    }
}

@FormFactorPreviews
@Composable
fun SettingsScreenPreview() {
    ee.vattson.ui.theme.VattsonTheme {
        SettingsScreenContent(
            uiState = MainUiState(
                showVat = true,
                priceResolution = PriceResolution.HOUR_1,
                appTheme = AppTheme.SYSTEM
            ),
            onShowVatChange = {},
            onPriceResolutionChange = {},
            onAppThemeChange = {},
            onAppBarVisibilityChange = {}
        )
    }
}
