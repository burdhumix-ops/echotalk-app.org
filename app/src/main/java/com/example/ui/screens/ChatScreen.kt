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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.repository.EchoTalkRepository
import com.example.di.DatabaseModule
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ChatScreen Composable with a LazyColumn for messages and a message input area
 * that saves and listens to messages via Cloud Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    repository: EchoTalkRepository = EchoTalkRepository.getInstance(),
    recipientId: String = "u-lucas",
    recipientName: String = "Lucas Silva",
    recipientAvatar: String = "",
    onBack: (() -> Unit)? = null,
    onViewProfile: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by repository.currentUser.collectAsState()
    val localDms by repository.dms.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }
    var isViewOnce by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var firestoreMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // STEP 1: Listen to real-time messages from Cloud Firestore via DatabaseModule
    LaunchedEffect(recipientId, currentUser.id) {
        try {
            DatabaseModule.provideFirestore(context)
            DatabaseModule.observeDirectMessages(currentUser.id, recipientId).collect { msgs ->
                if (msgs.isNotEmpty()) {
                    firestoreMessages = msgs
                }
            }
        } catch (e: Exception) {
            // Logged gracefully; fallback will combine local repository state
        }
    }

    // Merge Firestore messages with local repository messages (deduplicated by message id or timestamp+sender)
    val localMessages = localDms[recipientId] ?: emptyList()
    val combinedMessages = remember(firestoreMessages, localMessages) {
        val all = (localMessages + firestoreMessages).distinctBy {
            if (it.id.isNotBlank()) it.id else "${it.sender}_${it.timestamp}_${it.text}"
        }
        all.sortedBy { it.timestamp }
    }

    // Scroll to latest message smoothly whenever list size updates
    LaunchedEffect(combinedMessages.size) {
        if (combinedMessages.isNotEmpty()) {
            listState.animateScrollToItem(combinedMessages.size - 1)
        }
    }

    fun sendMessage() {
        val trimmed = messageText.trim()
        if (trimmed.isBlank() || isSending) return

        isSending = true
        val timestamp = timeFormat.format(Date())
        val msgId = "msg-${System.currentTimeMillis()}"
        val newMsg = ChatMessage(
            id = msgId,
            sender = currentUser.name,
            text = trimmed,
            timestamp = timestamp,
            status = "sent",
            isViewOnce = isViewOnce
        )

        // 1. Immediately update local repository for zero-lag UI responsiveness
        repository.sendDM(
            recipientId = recipientId,
            text = trimmed,
            isViewOnce = isViewOnce
        )

        // 2. Persist message to Cloud Firestore via DatabaseModule
        try {
            DatabaseModule.saveDirectMessage(
                senderId = currentUser.id,
                recipientId = recipientId,
                message = newMsg,
                onSuccess = {
                    isSending = false
                    statusMessage = "Message synced to Cloud Firestore"
                },
                onFailure = { err ->
                    isSending = false
                    statusMessage = "Saved locally (${err.message})"
                }
            )
        } catch (e: Exception) {
            isSending = false
        }

        messageText = ""
        isViewOnce = false
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("chat_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clickable { onViewProfile?.invoke(recipientId) }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = recipientName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 16.sp
                            )
                        }
                        Column {
                            Text(
                                text = recipientName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "Online • Cloud Synced",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* Call feature */ }) {
                        Icon(Icons.Default.Phone, contentDescription = "Voice Call", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { /* Video feature */ }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Message Input Area that saves to Cloud Firestore
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    // Optional view-once or status pill
                    AnimatedVisibility(visible = isViewOnce) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x33FBBF24))
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "🔥", fontSize = 12.sp)
                            Text(
                                text = "View-Once mode active: message disintegrates after opening",
                                fontSize = 11.sp,
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // View-Once toggle
                        IconButton(
                            onClick = { isViewOnce = !isViewOnce },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isViewOnce) Color(0xFFFBBF24)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Text(
                                text = "🔥",
                                fontSize = 16.sp
                            )
                        }

                        // Message Text Field
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = {
                                Text(text = "Write a message...", fontSize = 13.sp)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input"),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        // Send Button saving to Cloud Firestore
                        FilledIconButton(
                            onClick = { sendMessage() },
                            enabled = messageText.isNotBlank(),
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("chat_send_button"),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Message",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // LazyColumn for Messages
            if (combinedMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "💬", fontSize = 40.sp)
                        Text(
                            text = "No messages yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Send a message to start real-time chat with $recipientName",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("chat_messages_lazy_column")
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(combinedMessages, key = { it.id.ifBlank { "${it.timestamp}_${it.text.hashCode()}" } }) { msg ->
                        val isMe = msg.sender == currentUser.name
                        ChatMessageBubble(message = msg, isMe = isMe)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isMe: Boolean
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isMe) "You" else message.sender,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• ${message.timestamp}",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

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
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.isViewOnce) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text("🔥", fontSize = 11.sp)
                            Text(
                                "View-Once Message",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )

                    if (isMe) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (message.status == "seen") "✓✓" else "✓",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
