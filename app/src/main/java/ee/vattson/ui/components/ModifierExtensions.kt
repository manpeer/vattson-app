package ee.vattson.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * A GPU-accelerated press effect that scales the content down when pressed.
 */
@Composable
fun Modifier.pressScaleEffect(
    scale: Float = 0.95f
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "PressScale"
    )

    return this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
}

/**
 * A GPU-accelerated press effect combined with a clickable modifier.
 */
@Composable
fun Modifier.clickableWithScale(
    scale: Float = 0.95f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "ClickableScale"
    )

    return this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Disable default ripple to emphasize the scale effect
            onClick = onClick
        )
}

/**
 * A reactive press-to-grow and release-to-pop effect for choice boxes.
 * - Simple tap: Content "pops" instantly.
 * - Tap and hold: Content grows while held, then settles on release.
 */
@Composable
fun Modifier.choiceBoxClickable(
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()
    
    val scale = remember { Animatable(1f) }

    // Handle the physical press state: Grow on down, settle on up
    LaunchedEffect(isPressed) {
        if (isPressed) {
            // Fast grow to the "pop" size (subtle 1.05f)
            scale.animateTo(1.05f, spring(stiffness = Spring.StiffnessHigh))
        } else {
            // Bouncy settle back to normal
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }

    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                // Pulse effect specifically for the click action to ensure it "pops" 
                // even if the finger was released before the grow finished.
                scope.launch {
                    if (scale.value < 1.03f) {
                        scale.animateTo(1.05f, spring(stiffness = Spring.StiffnessHigh))
                        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                    }
                }
                onClick()
            }
        )
}
