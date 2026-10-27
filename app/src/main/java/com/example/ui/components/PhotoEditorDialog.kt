package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

enum class PhotoEditorTool {
    BRUSH, BLUR, CROP, TEXT
}

@Composable
fun PhotoEditorDialog(
    onSend: (isViewOnce: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var activeTool by remember { mutableStateOf(PhotoEditorTool.BRUSH) }
    var brushColor by remember { mutableStateOf(Color(0xFF6366F1)) }
    var isViewOnce by remember { mutableStateOf(false) }

    val brushPaths = remember { mutableStateListOf<DoodlePath>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val colors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFF6366F1),
        Color(0xFF38BDF8),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFFEC4899)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xF50B0F17)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Text(
                        text = "Telegram-Grade Photo Editor",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Flame View-Once Toggle
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isViewOnce = !isViewOnce },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isViewOnce) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🔥", fontSize = 12.sp)
                            Text(
                                text = if (isViewOnce) "View-Once ON" else "View-Once",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isViewOnce) Color.Black else Color.White
                            )
                        }
                    }
                }

                // Center Image Canvas with Drawing / Blur overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Base Image
                    Image(
                        painter = painterResource(id = R.drawable.ic_echotalk_logo_1792796066052),
                        contentDescription = "Editing Canvas",
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Live Brush / Blur Paths Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(activeTool, brushColor) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPoints = listOf(offset)
                                    },
                                    onDrag = { change, _ ->
                                        currentPoints = currentPoints + change.position
                                    },
                                    onDragEnd = {
                                        if (currentPoints.isNotEmpty()) {
                                            brushPaths.add(
                                                DoodlePath(
                                                    points = currentPoints,
                                                    color = if (activeTool == PhotoEditorTool.BLUR) Color(0xCC0F172A) else brushColor,
                                                    strokeWidth = if (activeTool == PhotoEditorTool.BLUR) 32f else 10f,
                                                    isEraser = activeTool == PhotoEditorTool.BLUR
                                                )
                                            )
                                            currentPoints = emptyList()
                                        }
                                    }
                                )
                            }
                    ) {
                        brushPaths.forEach { pathObj ->
                            if (pathObj.points.size > 1) {
                                val p = Path().apply {
                                    moveTo(pathObj.points.first().x, pathObj.points.first().y)
                                    pathObj.points.drop(1).forEach { pt ->
                                        lineTo(pt.x, pt.y)
                                    }
                                }
                                drawPath(
                                    path = p,
                                    color = pathObj.color,
                                    style = Stroke(
                                        width = pathObj.strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        if (currentPoints.size > 1) {
                            val activeP = Path().apply {
                                moveTo(currentPoints.first().x, currentPoints.first().y)
                                currentPoints.drop(1).forEach { pt ->
                                    lineTo(pt.x, pt.y)
                                }
                            }
                            drawPath(
                                path = activeP,
                                color = if (activeTool == PhotoEditorTool.BLUR) Color(0xCC0F172A) else brushColor,
                                style = Stroke(
                                    width = if (activeTool == PhotoEditorTool.BLUR) 32f else 10f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    if (activeTool == PhotoEditorTool.BLUR) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xCC000000)
                        ) {
                            Text(
                                text = "Blur Brush Active: Drag over sensitive details to redact",
                                fontSize = 11.sp,
                                color = Color(0xFFFBBF24),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Color Swatches Row
                if (activeTool == PhotoEditorTool.BRUSH) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { brushColor = c }
                                    .then(
                                        if (brushColor == c) Modifier.border(2.dp, Color.White, CircleShape)
                                        else Modifier
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Tools Dock (Crop, Brush, Blur Brush, Send)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Brush Tool
                            Button(
                                onClick = { activeTool = PhotoEditorTool.BRUSH },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeTool == PhotoEditorTool.BRUSH) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Brush", fontSize = 11.sp)
                            }

                            // Blur Brush Tool
                            Button(
                                onClick = { activeTool = PhotoEditorTool.BLUR },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeTool == PhotoEditorTool.BLUR) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.1f),
                                    contentColor = if (activeTool == PhotoEditorTool.BLUR) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.BlurOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Blur Brush", fontSize = 11.sp)
                            }

                            // Crop
                            Button(
                                onClick = { activeTool = PhotoEditorTool.CROP },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeTool == PhotoEditorTool.CROP) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Send Button
                        Button(
                            onClick = {
                                onSend(isViewOnce)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
