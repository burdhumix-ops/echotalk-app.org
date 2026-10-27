package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AudioWaveformBar(
    duration: String = "0:05",
    waveform: List<Float> = emptyList(),
    isSentByMe: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    // Fallback waveform if empty
    val bars = remember(waveform) {
        if (waveform.isNotEmpty()) waveform
        else listOf(0.3f, 0.6f, 0.9f, 0.4f, 0.7f, 1.0f, 0.5f, 0.8f, 0.4f, 0.9f, 0.6f, 0.3f, 0.7f, 0.5f, 0.8f)
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val totalSteps = 50
            for (i in 0..totalSteps) {
                progress = i / totalSteps.toFloat()
                delay(100)
            }
            isPlaying = false
            progress = 0f
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val activeBarColor = if (isSentByMe) Color.White else primaryColor
    val inactiveBarColor = if (isSentByMe) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSentByMe) primaryColor.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Play/Pause button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isSentByMe) primaryColor else MaterialTheme.colorScheme.primary)
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause voice note" else "Play voice note",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Waveform Canvas
        Canvas(
            modifier = Modifier
                .width(130.dp)
                .height(28.dp)
        ) {
            val barCount = bars.size
            val barWidth = 4.dp.toPx()
            val totalSpacing = size.width - (barWidth * barCount)
            val gap = totalSpacing / (barCount - 1).coerceAtLeast(1)

            bars.forEachIndexed { index, amp ->
                val x = index * (barWidth + gap)
                val barProgress = index / barCount.toFloat()
                val isPlayed = progress >= barProgress
                val barHeight = (size.height * amp).coerceAtLeast(6.dp.toPx())
                val topY = (size.height - barHeight) / 2f

                drawRoundRect(
                    color = if (isPlayed) activeBarColor else inactiveBarColor,
                    topLeft = Offset(x, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }

        // Duration text
        Text(
            text = if (isPlaying) {
                val currentSec = (progress * 5).toInt()
                "0:0$currentSec"
            } else duration,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
