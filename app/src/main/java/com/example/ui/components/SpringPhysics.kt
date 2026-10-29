package com.example.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Global GPU-Accelerated Spring Physics Motion Engine.
 * Provides custom cubic-bezier spring curves and hardware-composited micro-interactions
 * across EchoTalk without triggering layout reflows.
 */
object SpringPhysics {
    // Bounce Curve: cubic-bezier(0.34, 1.56, 0.64, 1) for dynamic elastic snaps
    val BounceCurve = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)

    // Soft Curve: cubic-bezier(0.175, 0.885, 0.32, 1.275) for smooth transitions and tray pops
    val SoftCurve = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)
}

/**
 * .spring-btn: Scales down smoothly (scale: 0.92) on active touch press and
 * snaps back elastically upon release using the Bounce Curve.
 */
fun Modifier.springButton(
    enabled: Boolean = true,
    pressedScale: Float = 0.92f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1.0f,
        animationSpec = tween(
            durationMillis = if (isPressed) 100 else 240,
            easing = if (isPressed) CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f) else SpringPhysics.BounceCurve
        ),
        label = "springButtonScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
            } else Modifier
        )
}

/**
 * .spring-card: Soft inward compression (scale: 0.96) on touch press with elastic snap-back.
 */
fun Modifier.springCard(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1.0f,
        animationSpec = tween(
            durationMillis = if (isPressed) 110 else 260,
            easing = if (isPressed) CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f) else SpringPhysics.BounceCurve
        ),
        label = "springCardScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
            } else Modifier
        )
}
