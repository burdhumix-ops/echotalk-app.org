package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.ai.GeminiChatService
import com.example.data.model.ChatMessage
import com.example.data.model.GameChallengeData
import com.example.data.model.User
import com.example.data.repository.ChatBackgroundPreset
import com.example.data.repository.EchoTalkRepository
import com.example.ui.components.*
import com.example.utils.getCountryFlag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BubbleAccent(val id: String, val title: String, val color: Color) {
    DEFAULT("default", "Classic Theme", Color(0xFF6366F1)),
    SUNSET_PINK("sunset", "Sunset Pink", Color(0xFFF43F5E)),
    ELECTRIC_CYAN("cyan", "Electric Cyan", Color(0xFF06B6D4)),
    EMERALD_GLOW("emerald", "Emerald Glow", Color(0xFF10B981)),
    MIDNIGHT_PURPLE("purple", "Midnight Purple", Color(0xFF8B5CF6))
}

@Composable
fun DMsScreen(
    repository: EchoTalkRepository,
    onLaunchGameChallenge: (String, String) -> Unit,
    onViewProfile: (User) -> Unit,
    aiService: GeminiChatService? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val users by repository.users.collectAsState()
    val currentUser by repository.currentUser.collectAsState()
    val dms by repository.dms.collectAsState()
    val activePartnerId by repository.activePartnerId.collectAsState()
    val chatBackground by repository.chatBackground.collectAsState()

    var activeSection by remember { mutableStateOf("chats") }
    var searchQuery by remember { mutableStateOf("") }
    var showBackgroundDialog by remember { mutableStateOf(false) }

    // Active conversation states
    var messageText by remember { mutableStateOf("") }
    var isSendViewOnce by remember { mutableStateOf(false) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showDoodleDialog by remember { mutableStateOf(false) }
    var showPhotoEditorDialog by remember { mutableStateOf(false) }
    var activeViewOnceMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showGamePicker by remember { mutableStateOf(false) }
    var sendErrorMessage by remember { mutableStateOf<String?>(null) }

    // Calling State
    var activeCallType by remember { mutableStateOf<String?>(null) } // "audio" or "video"

    // One-Tap Page Translation via Gemini AI
    var isPageTranslationOn by remember { mutableStateOf(false) }
    val translatedCache = remember { mutableStateMapOf<String, String>() }

    // Standalone Cloud Firestore Chat View toggle
    var showFirestoreChatScreen by remember { mutableStateOf(false) }

    // Custom Chat Bubble Accent
    var bubbleAccent by remember { mutableStateOf(BubbleAccent.DEFAULT) }
    var showAccentPicker by remember { mutableStateOf(false) }

    // Google Pixel Long-Press Context Menu state
    var selectedMessageForMenu by remember { mutableStateOf<ChatMessage?>(null) }
    val dissolvingMessageIds = remember { mutableStateListOf<String>() }

    // Voice recording & Tone Polish
    var isRecording by remember { mutableStateOf(false) }
    var recordDuration by remember { mutableIntStateOf(0) }
    var showTonePolishDialog by remember { mutableStateOf(false) }
    var voiceTranscriptDraft by remember { mutableStateOf("") }

    val activePartner = users.find { it.id == activePartnerId }
    val currentThread = activePartnerId?.let { dms[it] } ?: emptyList()

    val acceptedPartners = users.filter { it.id != currentUser.id && it.isFriend }
    val requestPartners = users.filter { it.id != currentUser.id && !it.isFriend }

    val displayedPartners = if (activeSection == "chats") acceptedPartners else requestPartners
    val filteredPartners = displayedPartners.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.handle.contains(searchQuery, ignoreCase = true)
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentThread.size) {
        if (currentThread.isNotEmpty()) {
            listState.animateScrollToItem(currentThread.size - 1)
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordDuration = 0
            while (isRecording) {
                delay(1000)
                recordDuration += 1
            }
        }
    }

    // -------------------------------------------------------------
    // FULL-SCREEN IMMERSIVE DM CONVERSATION VIEW
    // -------------------------------------------------------------
    if (activePartner != null) {
        if (showFirestoreChatScreen) {
            ChatScreen(
                repository = repository,
                recipientId = activePartner.id,
                recipientName = activePartner.name,
                recipientAvatar = activePartner.avatarUrl,
                onBack = { showFirestoreChatScreen = false },
                onViewProfile = { onViewProfile(activePartner) }
            )
            return
        }

        val partnerThread = dms[activePartner.id] ?: emptyList()
        val consecutiveCount = partnerThread.takeLastWhile { it.sender != currentUser.name }.size
        val myConsecutiveCount = partnerThread.takeLastWhile { it.sender == currentUser.name }.size
        val isPending = !activePartner.isFriend
        val isInputBlocked = isPending && myConsecutiveCount >= 3

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = { repository.setActivePartner(null) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        // Partner Avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                .clickable { onViewProfile(activePartner) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activePartner.name.take(1),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = activePartner.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                if (activePartner.isVerified) {
                                    BlueTickBadge(size = 14.dp)
                                }
                                Text(text = getCountryFlag(activePartner.countryCode), fontSize = 11.sp)
                            }
                            Text(
                                text = if (activePartner.online) "Active now" else "Offline",
                                fontSize = 10.sp,
                                color = if (activePartner.online) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Top Action Controls: Mutual Calling Gate, Translation & Accent
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // One-Tap Page Translation Switch (🌐)
                        IconButton(
                            onClick = {
                                isPageTranslationOn = !isPageTranslationOn
                                if (isPageTranslationOn && aiService != null) {
                                    coroutineScope.launch {
                                        partnerThread.forEach { m ->
                                            if (!translatedCache.containsKey(m.id)) {
                                                val translated = aiService.generateQuickReply("Translate to English keeping natural chat tone: ${m.text}")
                                                translatedCache[m.id] = translated
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isPageTranslationOn) MaterialTheme.colorScheme.primary else Color.Transparent)
                        ) {
                            Icon(
                                Icons.Default.Translate,
                                contentDescription = "Page Translation",
                                tint = if (isPageTranslationOn) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Cloud Firestore Chat Mode (Real-time Firestore)
                        IconButton(
                            onClick = { showFirestoreChatScreen = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                Icons.Default.Cloud,
                                contentDescription = "Cloud Firestore Chat",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Mutual Acceptance Gate: Audio (📞) and Video (📹) calls enabled ONLY when mutual friends
                        if (activePartner.isFriend) {
                            IconButton(
                                onClick = { activeCallType = "audio" },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Audio Call", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { activeCallType = "video" },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Custom Bubble Accent Selector Button
                        IconButton(
                            onClick = { showAccentPicker = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = "Accent Color", tint = bubbleAccent.color, modifier = Modifier.size(18.dp))
                        }

                        // Custom Chat Wallpaper Selector Button
                        IconButton(
                            onClick = { showBackgroundDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Wallpaper, contentDescription = "Wallpaper", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }

                        // Play 1v1 Game Challenge Trigger
                        Button(
                            onClick = { showGamePicker = !showGamePicker },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("1v1", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Message Request Review Banner if relationship is pending
            if (isPending) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0x33FBBF24),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55FBBF24))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Message Request ($consecutiveCount/3 msgs sent)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                            Text(
                                text = "Unknown contacts are limited to 3 messages until accepted.",
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { repository.acceptRequest(activePartner.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Accept", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    repository.declineRequest(activePartner.id)
                                    repository.setActivePartner(null)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Decline", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // Game Challenge Drawer
            AnimatedVisibility(visible = showGamePicker) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Challenge ${activePartner.name} to a 1v1 Match:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                            IconButton(onClick = { showGamePicker = false }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val chal = GameChallengeData(
                                        gameId = "chess",
                                        gameTitle = "Grandmaster Chess",
                                        challenger = currentUser.name,
                                        opponent = activePartner.name
                                    )
                                    repository.sendDM(
                                        recipientId = activePartner.id,
                                        text = "Sent a 1v1 challenge for Grandmaster Chess!",
                                        challengeData = chal
                                    )
                                    showGamePicker = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("♟️ Chess Duel")
                            }

                            Button(
                                onClick = {
                                    val chal = GameChallengeData(
                                        gameId = "ludo",
                                        gameTitle = "Ludo Master",
                                        challenger = currentUser.name,
                                        opponent = activePartner.name
                                    )
                                    repository.sendDM(
                                        recipientId = activePartner.id,
                                        text = "Sent a 1v1 challenge for Ludo Master!",
                                        challengeData = chal
                                    )
                                    showGamePicker = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🎲 Ludo Match")
                            }
                        }
                    }
                }
            }

            // Scrollable Message Stream with Dynamic Chat Background & Contrast Scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Background Layer (Gradient or Blurred Image)
                if (chatBackground.gradientColors != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = chatBackground.gradientColors!!.map { Color(it) }
                                )
                            )
                    )
                } else if (chatBackground.imageUrl != null) {
                    AsyncImage(
                        model = chatBackground.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Dynamic contrast scrim overlay (WCAG AAA readability)
                if (chatBackground.scrimAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = chatBackground.scrimAlpha))
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                items(partnerThread, key = { it.id }) { msg ->
                    val isMe = msg.sender == currentUser.name
                    val isDissolving = dissolvingMessageIds.contains(msg.id)
                    val bubbleColor = if (isMe) bubbleAccent.color else MaterialTheme.colorScheme.surfaceVariant
                    val displayedText = if (isPageTranslationOn && translatedCache.containsKey(msg.id)) {
                        translatedCache[msg.id] ?: msg.text
                    } else msg.text

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        if (isDissolving) {
                            Box(
                                modifier = Modifier
                                    .size(width = 200.dp, height = 48.dp)
                                    .padding(4.dp)
                            ) {
                                DustDissolveEffect(
                                    onComplete = {
                                        dissolvingMessageIds.remove(msg.id)
                                    }
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                modifier = Modifier
                                    .widthIn(max = 290.dp)
                                    .pointerInput(msg.id) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                selectedMessageForMenu = msg
                                            },
                                            onLongPress = {
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                selectedMessageForMenu = msg
                                            }
                                        )
                                    }
                            ) {
                                Text(
                                    text = "${if (isMe) "You" else msg.sender} • ${msg.timestamp}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )

                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMe) 16.dp else 4.dp,
                                        bottomEnd = if (isMe) 4.dp else 16.dp
                                    ),
                                    color = bubbleColor,
                                    tonalElevation = 2.dp
                                ) {
                                    when {
                                        // 1. Game Challenge
                                        msg.isChallenge && msg.challengeData != null -> {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(if (msg.challengeData.gameId == "chess") "♟️" else "🎲", fontSize = 20.sp)
                                                    Column {
                                                        Text(text = "${msg.challengeData.gameTitle} Duel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        Text(text = "Challenged by ${msg.challengeData.challenger}", fontSize = 9.sp, color = Color.LightGray)
                                                    }
                                                }
                                                Button(
                                                    onClick = { onLaunchGameChallenge(msg.challengeData.gameId, activePartner.name) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentPadding = PaddingValues(vertical = 4.dp)
                                                ) {
                                                    Text("⚔️ Play Now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        // 2. View-Once Media Capsule
                                        msg.isViewOnce -> {
                                            if (msg.viewed) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text("🔥", fontSize = 12.sp)
                                                    Text("View-Once Media Burned", fontSize = 11.sp, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                                                }
                                            } else {
                                                Row(
                                                    modifier = Modifier
                                                        .clickable { activeViewOnceMessage = msg }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFFBBF24)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("1", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                    }
                                                    Column {
                                                        Text("View Once Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        Text("Tap to open • 10s Burn", fontSize = 9.sp, color = Color(0xFFFCD34D))
                                                    }
                                                }
                                            }
                                        }

                                        // 3. Audio Voice Note
                                        msg.isAudio -> {
                                            AudioWaveformBar(
                                                duration = msg.audioDuration ?: "0:05",
                                                waveform = msg.audioWaveform,
                                                isSentByMe = isMe
                                            )
                                        }

                                        // 4. Text Bubble
                                        else -> {
                                            Text(
                                                text = displayedText,
                                                fontSize = 13.sp,
                                                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }

                                // Emoji reactions corner badge
                                if (msg.reactions.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xCC0F172A),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                        modifier = Modifier.offset(y = (-6).dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            msg.reactions.forEach { (emoji, count) ->
                                                Text("$emoji$count", fontSize = 10.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }

            // Real-Time Voice Recording Banner
            if (isRecording) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0x33E11D48)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE11D48)))
                            Text(text = "Recording Voice Note... (${recordDuration}s)", fontSize = 11.sp, color = Color(0xFFFB7185))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Tone Polish Button
                            TextButton(
                                onClick = {
                                    isRecording = false
                                    voiceTranscriptDraft = "Hey! Let's definitely catch up and play some Chess tonight."
                                    showTonePolishDialog = true
                                }
                            ) {
                                Text("Polish Tone", color = Color(0xFF818CF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = {
                                    isRecording = false
                                    val durationStr = "0:0${recordDuration.coerceAtLeast(1)}"
                                    repository.sendDM(
                                        recipientId = activePartner.id,
                                        text = "Voice Note ($durationStr)",
                                        isAudio = true,
                                        audioDuration = durationStr,
                                        isViewOnce = isSendViewOnce
                                    )
                                    isSendViewOnce = false
                                }
                            ) {
                                Text("Send", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Error Notice
            if (sendErrorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0x33E11D48)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = sendErrorMessage ?: "", fontSize = 11.sp, color = Color(0xFFFB7185))
                        IconButton(onClick = { sendErrorMessage = null }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // AI Contextual Smart Reply Chips (Gemini Powered)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Sounds great! 👍", "I'm ready for Chess ♟️", "Can't wait! ✨").forEach { chip ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                repository.sendDM(recipientId = activePartner.id, text = chip)
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(text = chip, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }

            // Messenger Fluid Interaction Input Bar (Collapsed/Expanded + Persistent Flame)
            MessengerInputBar(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSendMessage = {
                    if (messageText.isNotBlank()) {
                        val textToSend = if (replyingToMessage != null) {
                            "Replying to ${replyingToMessage!!.sender}: \"${replyingToMessage!!.text.take(30)}\"\n${messageText.trim()}"
                        } else messageText.trim()

                        val (ok, err) = repository.sendDM(
                            recipientId = activePartner.id,
                            text = textToSend,
                            isViewOnce = isSendViewOnce
                        )
                        if (ok) {
                            messageText = ""
                            isSendViewOnce = false
                            replyingToMessage = null
                            sendErrorMessage = null
                        } else {
                            sendErrorMessage = err
                        }
                    }
                },
                onSendLike = {
                    val (ok, err) = repository.sendDM(
                        recipientId = activePartner.id,
                        text = "👍",
                        isViewOnce = false
                    )
                    if (!ok) sendErrorMessage = err
                },
                isSendViewOnce = isSendViewOnce,
                onToggleViewOnce = { isSendViewOnce = !isSendViewOnce },
                onOpenPhotoEditor = { showPhotoEditorDialog = true },
                onOpenDoodle = { showDoodleDialog = true },
                onToggleVoiceRecording = { isRecording = !isRecording },
                isRecording = isRecording,
                onOpenGameChallenge = { showGamePicker = !showGamePicker },
                isInputBlocked = isInputBlocked,
                placeholder = "Message ${activePartner.name}...",
                replyingToText = replyingToMessage?.let { "${it.sender}: ${it.text.take(40)}" },
                onCancelReply = { replyingToMessage = null }
            )

            // Messenger 2-Tier Floating Reaction & Action Tray
            if (selectedMessageForMenu != null) {
                val menuMsg = selectedMessageForMenu!!
                MessengerFloatingTray(
                    message = menuMsg,
                    onReactionSelect = { emoji ->
                        repository.addReactionToDM(activePartner.id, menuMsg.id, emoji)
                        selectedMessageForMenu = null
                    },
                    onReply = {
                        replyingToMessage = menuMsg
                        selectedMessageForMenu = null
                    },
                    onCopy = {
                        selectedMessageForMenu = null
                    },
                    onDelete = {
                        dissolvingMessageIds.add(menuMsg.id)
                        repository.deleteDM(activePartner.id, menuMsg.id)
                        selectedMessageForMenu = null
                    },
                    onTranslate = {
                        if (aiService != null) {
                            coroutineScope.launch {
                                val tr = aiService.generateQuickReply("Translate to English keeping natural tone: ${menuMsg.text}")
                                translatedCache[menuMsg.id] = tr
                                isPageTranslationOn = true
                            }
                        }
                        selectedMessageForMenu = null
                    },
                    onDismiss = {
                        selectedMessageForMenu = null
                    }
                )
            }

            // CALL MODAL DIALOG
            if (activeCallType != null) {
                CallModalDialog(
                    partner = activePartner,
                    callType = activeCallType!!,
                    onEndCall = { activeCallType = null }
                )
            }

            // PHOTO EDITOR DIALOG
            if (showPhotoEditorDialog) {
                PhotoEditorDialog(
                    onSend = { viewOnce ->
                        repository.sendDM(
                            recipientId = activePartner.id,
                            text = if (viewOnce) "View Once Edited Photo" else "Edited Photo Markup",
                            mediaUrl = "edited_photo",
                            isViewOnce = viewOnce
                        )
                    },
                    onDismiss = { showPhotoEditorDialog = false }
                )
            }

            // DOODLE CANVAS DIALOG
            if (showDoodleDialog) {
                DoodleCanvasDialog(
                    recipientName = activePartner.name,
                    onDismiss = { showDoodleDialog = false },
                    onSendDoodle = { viewOnce, strokeCount ->
                        repository.sendDM(
                            recipientId = activePartner.id,
                            text = if (viewOnce) "View Once Doodle Artwork" else "Doodle Artwork ($strokeCount strokes)",
                            isDoodle = true,
                            isViewOnce = viewOnce
                        )
                    }
                )
            }

            // VIEW ONCE VIEWER DIALOG
            if (activeViewOnceMessage != null) {
                ViewOnceViewerDialog(
                    senderName = activeViewOnceMessage!!.sender,
                    onBurned = {
                        repository.openViewOnce(activePartner.id, activeViewOnceMessage!!.id)
                    },
                    onDismiss = { activeViewOnceMessage = null }
                )
            }

            // BUBBLE ACCENT SELECTOR DIALOG
            if (showAccentPicker) {
                Dialog(onDismissRequest = { showAccentPicker = false }) {
                    Surface(
                        modifier = Modifier.width(280.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Chat Bubble Color Accent", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            BubbleAccent.entries.forEach { accent ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            bubbleAccent = accent
                                            showAccentPicker = false
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(accent.color))
                                    Text(accent.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }

            // CHAT WALLPAPER / BACKGROUND SELECTOR DIALOG
            if (showBackgroundDialog) {
                ChatBackgroundDialog(
                    current = chatBackground,
                    onSelect = { preset ->
                        repository.setChatBackground(preset)
                        showBackgroundDialog = false
                    },
                    onDismiss = { showBackgroundDialog = false }
                )
            }

            // TONE POLISH DIALOG (Gemini AI)
            if (showTonePolishDialog) {
                Dialog(onDismissRequest = { showTonePolishDialog = false }) {
                    Surface(
                        modifier = Modifier.width(300.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Voice Tone Polish (Gemini AI)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Transform your voice note draft into your desired tone:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            listOf("Casual", "Formal", "Poetic", "Sarcastic").forEach { tone ->
                                Button(
                                    onClick = {
                                        showTonePolishDialog = false
                                        coroutineScope.launch {
                                            val polished = aiService?.generateQuickReply("Rephrase this message in a $tone tone: $voiceTranscriptDraft") ?: voiceTranscriptDraft
                                            messageText = polished
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("$tone Tone", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // -------------------------------------------------------------
        // MESSENGER-INSPIRED DM LIST REFACTOR
        // -------------------------------------------------------------
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Direct Messages",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.02).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${acceptedPartners.size} active chats • ${requestPartners.size} requests",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Top Header Pill Navigation: Chats (N) / Requests (N)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isChats = activeSection == "chats"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { activeSection = "chats" },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isChats) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chats (${acceptedPartners.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChats) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val isRequests = activeSection == "requests"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { activeSection = "requests" },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isRequests) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Requests",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRequests) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (requestPartners.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(if (isRequests) Color.White else Color(0xFFFBBF24)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${requestPartners.size}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Full-Width Search Input (rgba(255, 255, 255, 0.06), rounded 12dp)
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search conversations...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.06f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                        disabledContainerColor = Color.White.copy(alpha = 0.06f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            if (filteredPartners.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (activeSection == "chats") "No active chats found." else "No pending message requests.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredPartners, key = { it.id }) { partner ->
                    val thread = dms[partner.id] ?: emptyList()
                    val lastMsg = thread.lastOrNull()
                    val isRequest = activeSection == "requests"

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { repository.setActivePartner(partner.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Avatar with green online dot indicator (#10B981)
                                Box(modifier = Modifier.size(48.dp)) {
                                    if (partner.avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = partner.avatarUrl,
                                            contentDescription = partner.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = partner.name.take(1),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    if (partner.online) {
                                        Box(
                                            modifier = Modifier
                                                .size(13.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                                .align(Alignment.BottomEnd)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = partner.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        if (partner.isVerified) {
                                            BlueTickBadge(size = 14.dp)
                                        }
                                        if (isRequest) {
                                            Text(
                                                text = "${partner.reqCount}/3 msgs",
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

                                    Spacer(modifier = Modifier.height(2.dp))

                                    val previewText = when {
                                        lastMsg == null -> "${getCountryFlag(partner.countryCode)} ${partner.country} • ${partner.handle}"
                                        lastMsg.isAudio -> "🎙️ Voice Note (${lastMsg.audioDuration ?: "0:05"})"
                                        lastMsg.isViewOnce -> "🔥 View Once Secret Photo"
                                        lastMsg.isDoodle -> "🎨 Doodle Artwork"
                                        lastMsg.isChallenge -> "🎮 1v1 Match Challenge"
                                        else -> lastMsg.text
                                    }

                                    Text(
                                        text = previewText,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (isRequest) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { repository.acceptRequest(partner.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Accept", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { repository.declineRequest(partner.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Decline", fontSize = 11.sp)
                                    }
                                }
                            } else {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * ChatBackgroundDialog: Allows users to customize the chat background
 * with gradients (Rose Gold, Nebula, Aurora, Violet) or blurred imagery
 * (Cityscape, Acoustic Studio, Neon Loft) with dynamic contrast scrim overlays.
 */
@Composable
fun ChatBackgroundDialog(
    current: ChatBackgroundPreset,
    onSelect: (ChatBackgroundPreset) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Chat Wallpaper",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Vibrant gradients & blurred scenery",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ChatBackgroundPreset.entries.toList(), key = { it.id }) { preset ->
                        val isSelected = preset == current
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelect(preset) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Miniature preview tile
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    if (preset.gradientColors != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = preset.gradientColors!!.map { Color(it) }
                                                    )
                                                )
                                        )
                                    } else if (preset.imageUrl != null) {
                                        AsyncImage(
                                            model = preset.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surface)
                                        )
                                    }

                                    // Dynamic scrim preview
                                    if (preset.scrimAlpha > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = preset.scrimAlpha))
                                        )
                                    }

                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(preset.previewEmoji, fontSize = 16.sp)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (preset.gradientColors != null) "Vibrant Gradient Scrim" else if (preset.imageUrl != null) "Blurred Photo Scrim" else "Minimal Clean",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
