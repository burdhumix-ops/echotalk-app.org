package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MessengerInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onSendLike: () -> Unit,
    isSendViewOnce: Boolean,
    onToggleViewOnce: () -> Unit,
    onOpenPhotoEditor: () -> Unit,
    onOpenDoodle: () -> Unit,
    onToggleVoiceRecording: () -> Unit,
    isRecording: Boolean,
    onOpenGameChallenge: () -> Unit,
    isInputBlocked: Boolean,
    placeholder: String = "Message",
    replyingToText: String? = null,
    onCancelReply: () -> Unit = {}
) {
    val view = LocalView.current
    var isFocused by remember { mutableStateOf(false) }
    var forceExpandMedia by remember { mutableStateOf(false) }

    // State is collapsed (focused/typing) unless user explicitly pressed '>' to force expand media
    val isTypingOrFocused = isFocused || messageText.isNotEmpty()
    val isCollapsed = isTypingOrFocused && !forceExpandMedia

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        // Reply Quote Banner if replying
        if (replyingToText != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(24.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Column {
                            Text("Replying", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(replyingToText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        }
                    }
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Reply", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Media Action Toolbar or Collapsed Chevron [>]
            if (isCollapsed) {
                // Collapsed state: single left chevron [>]
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        forceExpandMedia = true
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Expand Media",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                // Expanded Media Toolbar: [+] [📷] [🖼️] [🎤]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // [+] Menu / 1v1 challenge trigger
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onOpenGameChallenge()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add / Games", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }

                    // [📷] Camera / Photo Editor
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onOpenPhotoEditor()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    }

                    // [🖼️] Gallery / Doodle Maker
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onOpenDoodle()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Brush, contentDescription = "Doodle", tint = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                    }

                    // [🎤] Voice Note recorder
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onToggleVoiceRecording()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color(0xFFE11D48) else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Voice Note",
                            tint = if (isRecording) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (forceExpandMedia) {
                        // Collapse back chevron
                        IconButton(
                            onClick = { forceExpandMedia = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Collapse", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Message Text Input Field
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    placeholder = {
                        Text(
                            text = if (isInputBlocked) "3-message limit reached" else placeholder,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    },
                    enabled = !isInputBlocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )
            }

            // Persistent View-Once Flame (🔥) Toggle
            // MUST remain accessible at all times in both collapsed and expanded views
            Surface(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onToggleViewOnce()
                    },
                shape = CircleShape,
                color = if (isSendViewOnce) Color(0xFFFBBF24) else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSendViewOnce) Color(0xFFF59E0B) else Color.Transparent
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isSendViewOnce) "🔥" else "1",
                        fontSize = if (isSendViewOnce) 14.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSendViewOnce) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right Action: Morphs between Send (Paper Plane) and Like (👍)
            AnimatedContent(
                targetState = messageText.isNotBlank(),
                transitionSpec = {
                    (scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn()) togetherWith
                            (scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut())
                },
                label = "send_or_like_morph"
            ) { hasText ->
                if (hasText) {
                    // Send Paper Plane button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onSendMessage()
                            forceExpandMedia = false
                        },
                        enabled = !isInputBlocked,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    // Thumbs-up Like quick trigger (Messenger style)
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onSendLike()
                        },
                        enabled = !isInputBlocked,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Text("👍", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
