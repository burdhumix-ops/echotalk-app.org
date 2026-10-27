package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.repository.EchoTalkRepository
import com.example.ui.components.AudioWaveformBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RoomsScreen(
    repository: EchoTalkRepository,
    onOpenDoodle: () -> Unit
) {
    val rooms by repository.rooms.collectAsState()
    val currentUser by repository.currentUser.collectAsState()
    val activeRoomId by repository.activeRoomId.collectAsState()

    var showAdminPanel by remember { mutableStateOf(false) }
    var showMembersDrawer by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var isMicMuted by remember { mutableStateOf(true) }
    var isHandRaised by remember { mutableStateOf(false) }
    var floatingReactions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showVipPasscodeDialog by remember { mutableStateOf(false) }
    var vipPasscodeInput by remember { mutableStateOf("") }
    var vipPasscodeError by remember { mutableStateOf("") }
    var vipUnlocked by remember { mutableStateOf(currentUser.role == "owner" || currentUser.role == "admin") }

    val isUserAdminOrOwner = currentUser.role == "owner" || currentUser.role == "admin"
    val activeRoom = rooms.find { it.id == activeRoomId }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto scroll to bottom of active room messages
    LaunchedEffect(activeRoom?.messages?.size) {
        if (activeRoom != null && activeRoom.messages.isNotEmpty()) {
            listState.animateScrollToItem(activeRoom.messages.size - 1)
        }
    }

    // -------------------------------------------------------------
    // FULL-SCREEN ROOM VIEW (Dedicated 100% immersive mode)
    // -------------------------------------------------------------
    if (activeRoom != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Bar with Back Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Back to Rooms Button
                        IconButton(
                            onClick = {
                                repository.setActiveRoom(null)
                                showAdminPanel = false
                                showMembersDrawer = false
                            }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Rooms", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = activeRoom.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                if (activeRoom.isSpecial) {
                                    Text(
                                        text = "VIP 48H",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFBBF24),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x33FBBF24))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                if (activeRoom.locked) {
                                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(
                                text = "${activeRoom.participants.size} inside • ${if (activeRoom.isOpen) "Open" else "Closed"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Top Controls: Members Drawer & Admin Button
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Members count button
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showMembersDrawer = !showMembersDrawer },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text(text = "${activeRoom.participants.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Admin Controls Toggle
                        if (isUserAdminOrOwner) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showAdminPanel = !showAdminPanel },
                                color = if (showAdminPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = "Admin",
                                        tint = if (showAdminPanel) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Admin",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (showAdminPanel) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Collapsible Admin Panel
            AnimatedVisibility(visible = showAdminPanel && isUserAdminOrOwner) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Administrator Deck: ${activeRoom.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(text = "Enforcing app_logic rules", fontSize = 10.sp, color = Color.Gray)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Lock / Unlock Button (Only special room can be locked!)
                            Button(
                                onClick = {
                                    if (activeRoom.locked) repository.unlockRoom(activeRoom.id)
                                    else repository.lockRoom(activeRoom.id)
                                },
                                enabled = activeRoom.isSpecial,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeRoom.locked) Color(0xFFFBBF24) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (activeRoom.locked) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(if (activeRoom.locked) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (activeRoom.locked) "Unlock Room" else "Lock Room", fontSize = 11.sp)
                            }

                            // Open / Close Room
                            Button(
                                onClick = {
                                    if (activeRoom.isOpen) repository.closeRoom(activeRoom.id)
                                    else repository.openRoom(activeRoom.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(if (activeRoom.isOpen) "Close Room" else "Open Room", fontSize = 11.sp)
                            }

                            // Cleanup Messages
                            Button(
                                onClick = { repository.cleanupRoomMessages(activeRoom.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FB7185)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFB7185), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cleanup", color = Color(0xFFFB7185), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Members Drawer
            AnimatedVisibility(visible = showMembersDrawer) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Active Room Participants (${activeRoom.participants.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activeRoom.participants.forEach { person ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = person, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        if (isUserAdminOrOwner && person != currentUser.name) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Kick",
                                                tint = Color(0xFFFB7185),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { repository.kickUserFromRoom(activeRoom.id, person) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Room Status Banners
            if (activeRoom.locked) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0x33FBBF24),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55FBBF24))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                        Text(
                            text = "Room Locked: Messaging blocked by administrator. Only Special Room can be locked.",
                            fontSize = 11.sp,
                            color = Color(0xFFFBBF24)
                        )
                    }
                }
            }

            if (!activeRoom.isOpen) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0x33FB7185),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55FB7185))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFB7185), modifier = Modifier.size(16.dp))
                        Text(
                            text = "Room Closed: Closed by administrator.",
                            fontSize = 11.sp,
                            color = Color(0xFFFB7185)
                        )
                    }
                }
            }

            // Voice Room Live Interactive Stage vs Text Chat Stream
            if (activeRoom.type == "voice") {
                // VOICE STAGE VIEW
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Speakers on Stage",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "🔊 HD Spatial Audio",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Speakers Pods Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // User Pod
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp)),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 3.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.size(54.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "You", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(if (isMicMuted) Color(0xFFE11D48) else Color(0xFF10B981))
                                                .align(Alignment.BottomEnd),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                    Text(text = "You", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "Stage Pod", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // Remote Speakers
                            activeRoom.speakers.take(2).forEach { spName ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp)),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = 3.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(modifier = Modifier.size(54.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF818CF8)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = spName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF10B981))
                                                    .align(Alignment.BottomEnd)
                                            )
                                        }
                                        Text(text = spName.split(" ")[0], fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(text = "Speaker", fontSize = 9.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        // Simulated Soundboard Spectrum
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0F172A)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Live Frequency Spectrum: Chloe is speaking",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    listOf(8, 18, 12, 24, 16, 20, 10, 22).forEach { h ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(h.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Voice Controls Dock
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Emoji soundboard reactions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("🔥", "👏", "🎉", "❤️", "⚡", "✨").forEach { emoji ->
                                Surface(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .clickable { floatingReactions = floatingReactions + emoji },
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = emoji, fontSize = 18.sp)
                                    }
                                }
                            }
                        }

                        // Mic Toggle & Hand Raise
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { isMicMuted = !isMicMuted },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isMicMuted) Color(0xFFE11D48) else Color(0xFF10B981)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = if (isMicMuted) "Unmute Mic" else "Mic Live", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { isHandRaised = !isHandRaised },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(text = if (isHandRaised) "✋ Raised" else "✋ Raise Hand", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                // TEXT CHAT STREAM VIEW
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeRoom.messages, key = { it.id }) { msg ->
                        val isMe = msg.sender == currentUser.name
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                        ) {
                            Text(
                                text = "${msg.sender} • ${msg.timestamp}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                ),
                                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 2.dp
                            ) {
                                if (msg.isAudio) {
                                    AudioWaveformBar(
                                        duration = msg.audioDuration ?: "0:05",
                                        isSentByMe = isMe,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                } else {
                                    Text(
                                        text = msg.text,
                                        fontSize = 13.sp,
                                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Input Bar for Text/VIP Room
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = {
                                Text(
                                    if (activeRoom.locked) "Room is locked by administrator"
                                    else "Message ${activeRoom.name}...",
                                    fontSize = 12.sp
                                )
                            },
                            enabled = !activeRoom.locked && activeRoom.isOpen,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    repository.sendRoomMessage(activeRoom.id, messageText.trim())
                                    messageText = ""
                                }
                            },
                            enabled = messageText.isNotBlank() && !activeRoom.locked && activeRoom.isOpen,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    } else {
        // -------------------------------------------------------------
        // ROOMS LIST VIEW (Clean, 4 sleek room cards)
        // -------------------------------------------------------------
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Audio & Text Rooms",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${rooms.size} Active Spaces • Tap to join 100% full-screen",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Exactly 4 sleek room cards
            items(rooms, key = { it.id }) { room ->
                val isVoice = room.type == "voice"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                        .clickable {
                            if (room.id == "r-special" && !vipUnlocked && currentUser.role == "member") {
                                showVipPasscodeDialog = true
                            } else {
                                repository.setActiveRoom(room.id)
                            }
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Icon Box
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isVoice) Color(0x33FB7185)
                                        else if (room.isSpecial) Color(0x33FBBF24)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isVoice) "🎙️" else if (room.isSpecial) "👑" else "💬",
                                    fontSize = 22.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = room.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (room.isSpecial) {
                                        Text(
                                            text = "Special 2-Day",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0x33FBBF24))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = room.subtitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = room.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Join Room",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal for VIP Passcode
    if (showVipPasscodeDialog) {
        Dialog(onDismissRequest = { showVipPasscodeDialog = false }) {
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "VIP Lounge Passcode",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enter 4-digit VIP passkey to unlock (Try 7777 or Owner Code 9999)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = vipPasscodeInput,
                        onValueChange = { vipPasscodeInput = it },
                        placeholder = { Text("7777") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (vipPasscodeError.isNotEmpty()) {
                        Text(text = vipPasscodeError, fontSize = 11.sp, color = Color(0xFFFB7185))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showVipPasscodeDialog = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (vipPasscodeInput == "7777" || vipPasscodeInput == "9999" || vipPasscodeInput == "2024") {
                                    vipUnlocked = true
                                    showVipPasscodeDialog = false
                                    repository.setActiveRoom("r-special")
                                    vipPasscodeError = ""
                                } else {
                                    vipPasscodeError = "Invalid VIP Passcode (Try 7777 or 9999)"
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Unlock")
                        }
                    }
                }
            }
        }
    }
}
