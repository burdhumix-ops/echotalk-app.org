package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User

@Composable
fun TopBar(
    currentUser: User,
    onOpenSettings: () -> Unit,
    onOpenSoundscapes: () -> Unit,
    onOpenVault: () -> Unit,
    onTriggerBackdoor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    // 15x App Logo Tap Gateway Trigger logic
    var logoTapCount by remember { mutableIntStateOf(0) }
    var firstTapTime by remember { mutableLongStateOf(0L) }

    fun handleLogoTap() {
        val now = System.currentTimeMillis()
        if (now - firstTapTime > 5000L) {
            logoTapCount = 1
            firstTapTime = now
        } else {
            logoTapCount += 1
        }

        if (logoTapCount >= 15) {
            logoTapCount = 0
            firstTapTime = 0L
            // Double-pulse haptic feedback
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            view.postDelayed({
                try {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } catch (_: Exception) {}
            }, 100)
            onTriggerBackdoor()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Subtle & Decent App Logo with 15x Secret Tap Gateway
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { handleLogoTap() }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                // Sleek vector signal/waves icon with soft subtle ambient glow
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(10.dp),
                            ambientColor = Color(0x26E8A5C8),
                            spotColor = Color(0x33E8A5C8)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        val barWidth = 2.5.dp.toPx()
                        val spacing = 2.5.dp.toPx()
                        val heights = listOf(0.40f, 0.95f, 0.65f, 0.85f)
                        val startX = (size.width - (heights.size * barWidth + (heights.size - 1) * spacing)) / 2f
                        heights.forEachIndexed { i, hRatio ->
                            val h = size.height * hRatio
                            val x = startX + i * (barWidth + spacing)
                            val y = (size.height - h) / 2f
                            drawRoundRect(
                                color = Color(0xFFE8A5C8),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, h),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                            )
                        }
                    }
                }

                // Refined, medium-weight typography
                Text(
                    text = "EchoTalk",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.02).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Right Zone: Ambient Soundscapes, Clean Profile Pill (identical across all roles), and Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ambient Soundscape button
                IconButton(
                    onClick = onOpenSoundscapes,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Ambient Soundscapes",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clean User Persona Pill (Complete Removal of Visible Role Badges / OWNER pills)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.name.take(1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = currentUser.name.split(" ")[0],
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                // Settings gear
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
