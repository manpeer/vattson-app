@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package ee.vattson.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Defines the position of an item within a [ButtonGroup] to apply appropriate connected shapes.
 */
enum class ButtonGroupItemPosition {
    Leading, Middle, Trailing, None
}

/**
 * Adds a toggleable item to the [ButtonGroup] with a filled style.
 * 
 * It automatically detects Dark Mode and adjusts colors accordingly.
 *
 * @param checked Whether the item is currently checked.
 * @param label The text label for the item.
 * @param onCheckedChange The action to perform when the item's checked state changes.
 * @param icon An optional composable representing the item's icon.
 * @param weight the weight to be applied to this item.
 * @param enabled Whether the item is enabled.
 * @param colors Custom colors to use in Light Mode (or both if darkColors is null).
 * @param darkColors Custom colors to use specifically in Dark Mode.
 * @param position The position of the item in the group to apply connected shapes.
 * @param menuLabel The text label for the item in the overflow menu.
 */
fun ButtonGroupScope.filledToggleableItem(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
    icon: (@Composable () -> Unit)? = null,
    weight: Float = Float.NaN,
    enabled: Boolean = true,
    colors: ToggleButtonColors? = null,
    darkColors: ToggleButtonColors? = null,
    position: ButtonGroupItemPosition = ButtonGroupItemPosition.None,
    menuLabel: String = label
) {
    customItem(
        buttonGroupContent = {
            val interactionSource = remember { MutableInteractionSource() }
            
            val surfaceColor = MaterialTheme.colorScheme.surface
            val isDark = remember(surfaceColor) { surfaceColor.luminance() < 0.5f }
            
            val contentPadding = remember(icon, label) {
                if (icon != null && label.isEmpty()) {
                    PaddingValues(0.dp)
                } else if (icon != null && label.isNotEmpty()) {
                    ButtonDefaults.ButtonWithIconContentPadding
                } else {
                    ButtonDefaults.ContentPadding
                }
            }

            val resolvedColors = if (isDark) {
                darkColors ?: colors ?: ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                colors ?: ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                )
            }

            val isPressed by interactionSource.collectIsPressedAsState()
            
            // GPU-accelerated scale animation for press and checked states
            val scale by animateFloatAsState(
                targetValue = when {
                    isPressed -> 0.95f
                    checked -> 1.02f
                    else -> 1f
                },
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                label = "ToggleButtonScale"
            )

            val modifier = if (!weight.isNaN()) {
                Modifier.weight(weight)
            } else {
                Modifier
            }.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }

            val shapes = when (position) {
                ButtonGroupItemPosition.Leading -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                ButtonGroupItemPosition.Middle -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                ButtonGroupItemPosition.Trailing -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                ButtonGroupItemPosition.None -> ToggleButtonDefaults.shapesFor(ButtonDefaults.MinHeight)
            }

            ToggleButton(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier,
                interactionSource = interactionSource,
                enabled = enabled,
                shapes = shapes,
                contentPadding = contentPadding,
                colors = resolvedColors
            ) {
                icon?.let {
                    it.invoke()
                    if (label.isNotEmpty()) {
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    }
                }
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                    )
                }
            }
        },
        menuContent = { state ->
            DropdownMenuItem(
                enabled = enabled,
                leadingIcon = icon,
                text = { Text(menuLabel) },
                onClick = {
                    onCheckedChange(!checked)
                    state.dismiss()
                },
            )
        }
    )
}
