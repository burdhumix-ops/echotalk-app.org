package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class DustParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val color: Color
)

@Composable
fun DustDissolveEffect(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val particles = remember {
        val colors = listOf(
            Color(0xFF818CF8),
            Color(0xFFA78BFA),
            Color(0xFF60A5FA),
            Color(0xFFE0E7FF),
            Color(0xFFF43F5E),
            Color(0xFFCBD5E1)
        )
        List(140) {
            DustParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 250f + if (Random.nextBoolean()) 80f else -80f,
                vy = -Random.nextFloat() * 180f - 40f,
                radius = Random.nextFloat() * 3f + 1.5f,
                color = colors.random()
            )
        }
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = LinearEasing)
        )
        onComplete()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        val alpha = (1f - t).coerceIn(0f, 1f)

        particles.forEach { p ->
            val px = (p.x * size.width) + (p.vx * t)
            val py = (p.y * size.height) + (p.vy * t) + (t * t * 120f)
            val currentRadius = p.radius * (1f - t * 0.4f)

            if (alpha > 0f) {
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = currentRadius,
                    center = Offset(px, py)
                )
            }
        }
    }
}
