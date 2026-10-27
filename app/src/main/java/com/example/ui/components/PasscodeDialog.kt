package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

@Composable
fun PasscodeDialog(
    onDismiss: () -> Unit,
    onRoleGranted: (String) -> Unit
) {
    val view = LocalView.current
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isAuthenticated by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("owner") }

    fun onDigit(d: String) {
        if (pin.length < 4) {
            val nextPin = pin + d
            pin = nextPin
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (nextPin.length == 4) {
                if (nextPin == "8888" || nextPin == "9999" || nextPin == "2024" || nextPin == "0000") {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    isAuthenticated = true
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    errorMessage = "Incorrect Passcode"
                }
            }
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotEmpty()) {
            delay(1200)
            pin = ""
            errorMessage = ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(330.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xF21A1822),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33E8A5C8)),
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x33E8A5C8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isAuthenticated) Icons.Default.Shield else Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFFE8A5C8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isAuthenticated) "Role Gateway" else "Secret Gateway",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isAuthenticated) "Select active privileges" else "Security PIN required",
                                fontSize = 10.sp,
                                color = Color(0xFFC4A1B3)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }

                if (!isAuthenticated) {
                    // PIN Entry State
                    Text(
                        text = "Enter 4-digit master security PIN (Default: 8888)",
                        fontSize = 11.sp,
                        color = Color(0xFFC4A1B3),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // 4 Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { idx ->
                            val filled = pin.length > idx
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (filled) Color(0xFFE8A5C8)
                                        else Color.White.copy(alpha = 0.2f)
                                    )
                            )
                        }
                    }

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            fontSize = 11.sp,
                            color = Color(0xFFFB7185),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Keypad
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("", "0", "DEL")
                        )

                        rows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { digit ->
                                    if (digit.isEmpty()) {
                                        Spacer(modifier = Modifier.size(50.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .clickable {
                                                    if (digit == "DEL") {
                                                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                                    } else {
                                                        onDigit(digit)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = digit,
                                                fontSize = if (digit == "DEL") 11.sp else 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Authenticated State: Role Switcher
                    Text(
                        text = "Authentication confirmed. Switch role immediately to unlock/lock the hidden Owner Command Center in Settings:",
                        fontSize = 11.sp,
                        color = Color(0xFFC4A1B3),
                        lineHeight = 16.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            Triple("owner", "Platform Owner", "Full Owner Command Center, secret vault, and verification authority"),
                            Triple("admin", "Administrator", "Moderation tools and member inspection"),
                            Triple("member", "Standard User", "Standard consumer privileges without elevated panels")
                        ).forEach { (roleKey, title, desc) ->
                            val isSelected = selectedRole == roleKey
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedRole = roleKey
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0x33E8A5C8) else Color.White.copy(alpha = 0.06f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFE8A5C8) else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(desc, fontSize = 10.sp, color = Color(0xFFC4A1B3))
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFE8A5C8), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onRoleGranted(selectedRole)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A5C8)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Role (${selectedRole.uppercase()})", color = Color(0xFF0F0E17), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
