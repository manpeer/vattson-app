package ee.vattson.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ee.vattson.data.model.CurrencyPreference
import ee.vattson.data.model.NotificationType
import ee.vattson.data.model.PriceNotification
import ee.vattson.ui.components.ButtonGroupItemPosition
import ee.vattson.ui.components.filledToggleableItem
import ee.vattson.ui.theme.VattsonTheme

@Composable
fun NotificationScreen(
    notificationsEnabled: Boolean,
    priceNotifications: List<PriceNotification>,
    currencyPreference: CurrencyPreference,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onAddNotification: (Double, NotificationType, CurrencyPreference) -> Unit,
    onRemoveNotification: (String) -> Unit,
    onAppBarVisibilityChange: (Float) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Report App Bar visibility to parent
    val appBarVisibleFraction by remember {
        derivedStateOf { 1f - scrollBehavior.state.collapsedFraction }
    }
    LaunchedEffect(appBarVisibleFraction) {
        onAppBarVisibilityChange(appBarVisibleFraction)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Teavitused") },
                navigationIcon = {
                    Spacer(modifier = Modifier.size(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Teavitused",
                    style = MaterialTheme.typography.titleLarge
                )

                ButtonGroup(
                    overflowIndicator = { menuState ->
                        ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                    },
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    filledToggleableItem(
                        checked = notificationsEnabled,
                        label = "Sees",
                        onCheckedChange = { if (it) onNotificationsEnabledChange(true) },
                        position = ButtonGroupItemPosition.Leading
                    )
                    filledToggleableItem(
                        checked = !notificationsEnabled,
                        label = "Väljas",
                        onCheckedChange = { if (it) onNotificationsEnabledChange(false) },
                        position = ButtonGroupItemPosition.Trailing
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (notificationsEnabled) "Teavitused on sisse lülitatud" else "Teavitused on välja lülitatud",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (notificationsEnabled) {
                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Teavita, kui hind läheb...",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    var belowThreshold by remember { mutableStateOf("") }
                    var aboveThreshold by remember { mutableStateOf("") }
                    
                    var belowUnit by remember { mutableStateOf(currencyPreference) }
                    var aboveUnit by remember { mutableStateOf(currencyPreference) }

                    NotificationInputRow(
                        label = "Alla",
                        selectedUnit = belowUnit,
                        onUnitChange = { belowUnit = it },
                        value = belowThreshold,
                        onValueChange = { belowThreshold = it },
                        onConfirm = { valValue, unit ->
                            val value = valValue.replace(',', '.').toDoubleOrNull()
                            if (value != null) {
                                onAddNotification(value, NotificationType.BELOW, unit)
                                belowThreshold = ""
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NotificationInputRow(
                        label = "Üle",
                        selectedUnit = aboveUnit,
                        onUnitChange = { aboveUnit = it },
                        value = aboveThreshold,
                        onValueChange = { aboveThreshold = it },
                        onConfirm = { valValue, unit ->
                            val value = valValue.replace(',', '.').toDoubleOrNull()
                            if (value != null) {
                                onAddNotification(value, NotificationType.ABOVE, unit)
                                aboveThreshold = ""
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                var isExpanded by rememberSaveable { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sätitud teavitused",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                        ) {
                            if (priceNotifications.isEmpty()) {
                                Text(
                                    text = "Teavitusi pole sätitud",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                priceNotifications.forEachIndexed { index, notification ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        @Suppress("SENSELESS_COMPARISON")
                                        val unitLabel = if (notification.unit == null) {
                                            CurrencyPreference.SENT_KWH.shortLabel
                                        } else {
                                            notification.unit.shortLabel
                                        }
                                        Text(
                                            text = "${if (notification.type == NotificationType.BELOW) "Alla" else "Üle"} ${notification.threshold} $unitLabel",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        IconButton(onClick = { onRemoveNotification(notification.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Kustuta",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun NotificationInputRow(
    label: String,
    selectedUnit: CurrencyPreference,
    onUnitChange: (CurrencyPreference) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: (String, CurrencyPreference) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(60.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                // Maintain formatting consistency: convert periods to commas
                onValueChange(newValue.replace('.', ','))
            },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Sisesta hind") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))

        var unitMenuExpanded by rememberSaveable { mutableStateOf(false) }
        Box {
            FilledToggleableItemLike(
                label = selectedUnit.shortLabel,
                onClick = { unitMenuExpanded = true }
            )
            DropdownMenu(
                expanded = unitMenuExpanded,
                onDismissRequest = { unitMenuExpanded = false }
            ) {
                CurrencyPreference.entries.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text(unit.shortLabel) },
                        onClick = {
                            onUnitChange(unit)
                            unitMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        
        val isValueValid = value.replace(',', '.').toDoubleOrNull() != null
        IconButton(
            onClick = { onConfirm(value, selectedUnit) },
            enabled = isValueValid
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Kinnita",
                tint = if (isValueValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun FilledToggleableItemLike(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(56.dp) // Match TextField height approximately
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
annotation class FormFactorPreviews

@FormFactorPreviews
@Composable
fun NotificationScreenPreview() {
    VattsonTheme {
        NotificationScreen(
            notificationsEnabled = true,
            priceNotifications = listOf(
                PriceNotification(threshold = 5.0, type = NotificationType.BELOW, unit = CurrencyPreference.SENT_KWH),
                PriceNotification(threshold = 15.0, type = NotificationType.ABOVE, unit = CurrencyPreference.EURO_MWH)
            ),
            currencyPreference = CurrencyPreference.SENT_KWH,
            onNotificationsEnabledChange = {},
            onAddNotification = { _, _, _ -> },
            onRemoveNotification = {},
            onAppBarVisibilityChange = {}
        )
    }
}
