package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.User
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CallModalDialog(
    partner: User,
    callType: String, // "audio" or "video"
    onEndCall: () -> Unit
) {
    var callDurationSec by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isVideoOff by remember { mutableStateOf(callType == "audio") }
    var isScreenSharing by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var callStatus by remember { mutableStateOf("Connecting P2P...") }

    LaunchedEffect(Unit) {
        delay(1200)
        callStatus = "Secured (WebRTC E2EE)"
        delay(800)
        callStatus = "Connected"
        while (true) {
            delay(1000)
            callDurationSec += 1
        }
    }

    val formatDuration = remember(callDurationSec) {
        val mins = callDurationSec / 60
        val secs = callDurationSec % 60
        "$mins:${if (secs < 10) "0$secs" else "$secs"}"
    }

    Dialog(
        onDismissRequest = onEndCall,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF020617)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Call Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "$callStatus • $formatDuration",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x336366F1),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                            Text("P2P WebRTC", fontSize = 10.sp, color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Center Stage: Video / Screen Sharing / Audio
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp, top = 70.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScreenSharing) {
                        // Live Screen Sharing Canvas Stream
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .height(280.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                    // Animated live screen graph simulation
                                    val step = size.width / 24
                                    for (i in 0..24) {
                                        val x = i * step
                                        val y = size.height / 2 + (sin(i * 0.8 + callDurationSec * 1.5) * 40).toFloat()
                                        drawCircle(color = Color(0xFF38BDF8), radius = 3.dp.toPx(), center = Offset(x, y))
                                    }
                                }

                                Text(
                                    text = "🖥️ Screen Broadcasting Live (1080p 60fps)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(14.dp)
                                )
                            }
                            Text(
                                text = "Broadcasting your screen live to ${partner.name}",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (isVideoOff) {
                        // High-Definition Audio Call
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.size(120.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color(0xFF6366F1)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = partner.name.take(1),
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(3.dp, Color(0xFF818CF8).copy(alpha = 0.6f), CircleShape)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = partner.name,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isMuted) "Microphone Muted" else "HD Audio Active (Noise Cancelled)",
                                    fontSize = 12.sp,
                                    color = if (isMuted) Color(0xFFFB7185) else Color(0xFF38BDF8)
                                )
                            }
                        }
                    } else {
                        // Full Video Call Mock with Partner Stream
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color(0xFF1E1B4B))
                                .border(2.dp, Color(0xFF818CF8).copy(alpha = 0.4f), RoundedCornerShape(32.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF818CF8)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = partner.name.take(1), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Text(text = "${partner.name} (Camera Stream Active)", fontSize = 13.sp, color = Color.White)
                                Text(text = "1080p HD • 60 FPS", fontSize = 10.sp, color = Color(0xFF10B981))
                            }

                            // Remote partner nametag
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = Color.Black.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = partner.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Floating Self-View (PiP)
                    if (!isVideoOff && !isScreenSharing) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 28.dp, top = 20.dp)
                                .size(width = 96.dp, height = 134.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                            color = Color(0xFF0F172A),
                            tonalElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "You", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = if (isFrontCamera) "Front" else "Rear", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // Bottom Call Controls Dock
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp, start = 20.dp, end = 20.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFF0B1329).copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mic Mute
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) Color(0xFFE11D48) else Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute Mic",
                                tint = Color.White
                            )
                        }

                        // Video Toggle
                        IconButton(
                            onClick = { isVideoOff = !isVideoOff },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isVideoOff) Color.White.copy(alpha = 0.15f) else Color(0xFF6366F1))
                        ) {
                            Icon(
                                imageVector = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                contentDescription = "Video",
                                tint = Color.White
                            )
                        }

                        // Screen Share (🖥️)
                        IconButton(
                            onClick = { isScreenSharing = !isScreenSharing },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isScreenSharing) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = if (isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                                contentDescription = "Screen Share",
                                tint = if (isScreenSharing) Color.Black else Color.White
                            )
                        }

                        // Flip Camera
                        if (!isVideoOff && !isScreenSharing) {
                            IconButton(
                                onClick = { isFrontCamera = !isFrontCamera },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Flip Camera",
                                    tint = Color.White
                                )
                            }
                        }

                        // End Call
                        IconButton(
                            onClick = onEndCall,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
