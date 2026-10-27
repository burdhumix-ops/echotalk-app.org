package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatMessage
import kotlinx.coroutines.delay

@Composable
fun MessengerFloatingTray(
    message: ChatMessage,
    onReactionSelect: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onTranslate: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val emojis = listOf("❤️", "😆", "😮", "😢", "😡", "👍", "+")

    // Entrance Spring: cubic-bezier(0.175, 0.885, 0.32, 1.275) over 220ms
    val cubicEasing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)
    var isEntering by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }

    // Staggered emoji launch count: 0 -> 7
    var revealedEmojiCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        isEntering = true
        // Sequential staggered emoji pop-up: stagger 0.03s (30ms)
        for (i in 1..emojis.size) {
            revealedEmojiCount = i
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            try { view.playSoundEffect(SoundEffectConstants.CLICK) } catch (_: Exception) {}
            delay(30)
        }
    }

    val trayScale by animateFloatAsState(
        targetValue = if (isClosing) 0.8f else if (isEntering) 1.0f else 0.85f,
        animationSpec = if (isClosing) tween(150) else tween(220, easing = cubicEasing),
        finishedListener = {
            if (isClosing) onDismiss()
        },
        label = "trayScale"
    )

    val trayAlpha by animateFloatAsState(
        targetValue = if (isClosing) 0f else if (isEntering) 1.0f else 0f,
        animationSpec = tween(if (isClosing) 150 else 200),
        label = "trayAlpha"
    )

    fun dismissWithAnimation() {
        if (!isClosing) {
            isClosing = true
        }
    }

    Dialog(
        onDismissRequest = { dismissWithAnimation() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { dismissWithAnimation() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = trayScale
                        scaleY = trayScale
                        alpha = trayAlpha
                    }
                    .padding(horizontal = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent dismiss */ },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tier 1: Top Pill (Frosted Glass Emoji Bar)
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xF20F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    shadowElevation = 16.dp,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        emojis.forEachIndexed { index, emoji ->
                            val isRevealed = revealedEmojiCount > index
                            val emojiScale by animateFloatAsState(
                                targetValue = if (isRevealed) 1.0f else 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "emojiScale_$emoji"
                            )

                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val pressScale by animateFloatAsState(
                                targetValue = if (isPressed) 1.35f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.5f),
                                label = "pressScale_$emoji"
                            )

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .graphicsLayer {
                                        scaleX = emojiScale * pressScale
                                        scaleY = emojiScale * pressScale
                                        alpha = if (isRevealed) 1f else 0f
                                    }
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        try { view.playSoundEffect(SoundEffectConstants.CLICK) } catch (_: Exception) {}
                                        onReactionSelect(emoji)
                                        dismissWithAnimation()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }
                }

                // Tier 2: Bottom Sheet (Integrated Action Strip)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xF21E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    shadowElevation = 12.dp,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reply
                        ActionStripButton(
                            icon = Icons.AutoMirrored.Filled.Reply,
                            label = "Reply",
                            tint = Color(0xFF38BDF8)
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onReply()
                            dismissWithAnimation()
                        }

                        // Copy
                        ActionStripButton(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy",
                            tint = Color(0xFF818CF8)
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("DM", message.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            onCopy()
                            dismissWithAnimation()
                        }

                        // Translate
                        ActionStripButton(
                            icon = Icons.Default.Translate,
                            label = "Translate",
                            tint = Color(0xFF10B981)
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onTranslate()
                            dismissWithAnimation()
                        }

                        // Delete
                        ActionStripButton(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            tint = Color(0xFFFB7185)
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onDelete()
                            dismissWithAnimation()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionStripButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}
