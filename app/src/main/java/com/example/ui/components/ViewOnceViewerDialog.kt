package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun ViewOnceViewerDialog(
    senderName: String,
    mediaUrl: String? = null,
    onBurned: () -> Unit,
    onDismiss: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(10) }
    var isBurning by remember { mutableStateOf(false) }

    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        } else {
            isBurning = true
            delay(500)
            onBurned()
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            onBurned()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = !isBurning,
            exit = fadeOut(tween(400)) + scaleOut(tween(400), targetScale = 0.85f)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xF5000000)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFBBF24)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "1",
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Column {
                                Text(
                                    text = "View-Once Media from $senderName",
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Self-destructs upon closing or countdown expiry",
                                    fontSize = 10.sp,
                                    color = Color(0xFFFCD34D)
                                )
                            }
                        }

                        // Circular Countdown Badge
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FBBF24))
                                .border(2.dp, Color(0xFFFBBF24), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${secondsLeft}s",
                                fontSize = 13.sp,
                                color = Color(0xFFFBBF24),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    // Media Display Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Display generated app logo image or gradient placeholder
                        Image(
                            painter = painterResource(id = R.drawable.ic_echotalk_logo_1792796066052),
                            contentDescription = "View-once secret media",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Fit
                        )

                        // Screen capture security overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "SCREEN CAPTURE PROHIBITED",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Bottom Burn & Destroy Action
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "This media will disappear permanently once dismissed.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Button(
                            onClick = {
                                isBurning = true
                                onBurned()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Destroy and Burn",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DESTROY & DISMISS NOW",
                                fontSize = 13.sp,
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
