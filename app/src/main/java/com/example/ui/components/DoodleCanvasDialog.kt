package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class DoodlePath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

private val DOODLE_COLORS = listOf(
    Color(0xFFFFFFFF),
    Color(0xFF818CF8),
    Color(0xFF38BDF8),
    Color(0xFF34D399),
    Color(0xFFFBBF24),
    Color(0xFFFB7185),
    Color(0xFFC084FC),
    Color(0xFFF97316)
)

@Composable
fun DoodleCanvasDialog(
    recipientName: String,
    onDismiss: () -> Unit,
    onSendDoodle: (isViewOnce: Boolean, pathCount: Int) -> Unit
) {
    var selectedColor by remember { mutableStateOf(DOODLE_COLORS[1]) }
    var selectedStrokeWidth by remember { mutableFloatStateOf(8f) }
    var isEraser by remember { mutableStateOf(false) }
    var isViewOnce by remember { mutableStateOf(false) }

    val paths = remember { mutableStateListOf<DoodlePath>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF090D16),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Instagram Doodle Canvas",
                            fontSize = 16.sp,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Drawing to $recipientName",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Doodle",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toolbar: Color Swatches & Stroke Sizes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color palette
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DOODLE_COLORS.forEach { color ->
                            val isSelected = !isEraser && selectedColor == color
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable {
                                        selectedColor = color
                                        isEraser = false
                                    }
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                                        else Modifier
                                    )
                            )
                        }
                    }

                    // Stroke width & tools
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Undo
                        IconButton(
                            onClick = {
                                if (paths.isNotEmpty()) paths.removeAt(paths.size - 1)
                            },
                            enabled = paths.isNotEmpty(),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Undo",
                                tint = if (paths.isNotEmpty()) Color.White else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Clear
                        IconButton(
                            onClick = { paths.clear() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear",
                                tint = Color(0xFFFB7185),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Drawing Canvas Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF030712))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isEraser, selectedColor, selectedStrokeWidth) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPoints = listOf(offset)
                                    },
                                    onDrag = { change, _ ->
                                        currentPoints = currentPoints + change.position
                                    },
                                    onDragEnd = {
                                        if (currentPoints.isNotEmpty()) {
                                            paths.add(
                                                DoodlePath(
                                                    points = currentPoints,
                                                    color = if (isEraser) Color(0xFF030712) else selectedColor,
                                                    strokeWidth = selectedStrokeWidth,
                                                    isEraser = isEraser
                                                )
                                            )
                                            currentPoints = emptyList()
                                        }
                                    }
                                )
                            }
                    ) {
                        // Draw established paths
                        paths.forEach { pathObj ->
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

                        // Draw live active stroke
                        if (currentPoints.size > 1) {
                            val activePath = Path().apply {
                                moveTo(currentPoints.first().x, currentPoints.first().y)
                                currentPoints.drop(1).forEach { pt ->
                                    lineTo(pt.x, pt.y)
                                }
                            }
                            drawPath(
                                path = activePath,
                                color = if (isEraser) Color(0xFF030712) else selectedColor,
                                style = Stroke(
                                    width = selectedStrokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Bar: View-Once Toggle & Send Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // View-Once toggle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isViewOnce) Color(0x33FBBF24)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                1.dp,
                                if (isViewOnce) Color(0xFFFBBF24)
                                else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { isViewOnce = !isViewOnce }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isViewOnce) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1",
                                fontSize = 10.sp,
                                color = if (isViewOnce) Color.Black else Color.White
                            )
                        }
                        Text(
                            text = if (isViewOnce) "View-Once: ON" else "View-Once: OFF",
                            fontSize = 12.sp,
                            color = if (isViewOnce) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // Send Button
                    Button(
                        onClick = {
                            onSendDoodle(isViewOnce, paths.size)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Doodle",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Send Doodle", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
