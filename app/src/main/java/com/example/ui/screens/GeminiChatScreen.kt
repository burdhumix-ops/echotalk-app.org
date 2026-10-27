package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.*
import com.example.data.model.GeminiModel
import com.example.ui.components.NavTab

/**
 * Modern iOS-Style ChatScreen Composable with message list and sleek iOS input dock,
 * integrating with the existing Gemini EchoBot ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosChatScreen(
    modifier: Modifier = Modifier,
    viewModel: EchoBotViewModel = viewModel(),
    onNavigateToTab: (NavTab) -> Unit = {},
    onStartDMWithDraft: (userId: String, draft: String) -> Unit = { _, _ -> },
    onOpenSoundscapes: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    onLaunchGame: (gameId: String) -> Unit = {},
    aiService: GeminiChatService? = null
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll on new messages
    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isGenerating = uiState is EchoBotUiState.Loading || uiState is EchoBotUiState.Streaming

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000)) // iOS Pure Dark canvas
    ) {
        // Modern iOS Top Navigation & Engine Control Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xF0121214),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // iOS Bot Avatar with Active Green Glow
                        Box(modifier = Modifier.size(38.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF007AFF),
                                                Color(0xFF5856D6)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "\uD83E\uDD16", fontSize = 18.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34C759))
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "EchoBot",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x2634C759))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Best Friend",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34C759)
                                    )
                                }
                            }
                            Text(
                                text = "AI Wingman • Text Coach • App Navigator",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearConversation() }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Clear Chat",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // iOS-Style Segmented Model Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GeminiModel.entries.forEach { gm ->
                        val isSelected = selectedModel == gm
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF2C2C2E) else Color.Transparent)
                                .clickable { viewModel.setModel(gm) }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (gm) {
                                    GeminiModel.FLASH -> "3.8 Flash"
                                    GeminiModel.ARGON -> "4 Argon"
                                    GeminiModel.FLASH_LITE -> "3.5 Lite"
                                },
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // Quick Task Prompt Carousel (iOS style pills)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0B0B0D)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(EchoQuickTask.entries) { task ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = !isGenerating) {
                                viewModel.runQuickTask(task)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1C1C1E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = task.icon, fontSize = 13.sp)
                            Text(
                                text = task.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Scrollable iOS Messages List
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                // Starter Welcome View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF007AFF),
                                        Color(0xFF5856D6)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "\uD83E\uDD1D", fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Talk to EchoBot",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Your genuine best friend & texting wingman. Need help texting a friend, checking your tone, or finding active voice rooms? I've got you.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Starter Cards
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EchoQuickTask.entries.take(3).forEach { task ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { viewModel.runQuickTask(task) },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1C1C1E),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14FFFFFF))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(text = task.icon, fontSize = 20.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = task.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(text = task.description, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isUser = msg.role == "user"
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            // Timestamp / Sender label
                            Text(
                                text = if (isUser) "Delivered • ${msg.timestamp}" else "EchoBot • ${msg.timestamp}",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )

                            // iOS Message Bubble
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isUser) 18.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 18.dp
                                ),
                                color = if (isUser) Color(0xFF007AFF) else Color(0xFF26252A),
                                modifier = Modifier.widthIn(max = 310.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(
                                        text = msg.text,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        lineHeight = 20.sp
                                    )

                                    if (msg.isStreaming) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            color = Color(0xFF0A84FF),
                                            trackColor = Color(0x33FFFFFF)
                                        )
                                    }
                                }
                            }

                            // Interactive iOS Action Cards attached to bot response
                            if (!isUser && msg.actions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(
                                    modifier = Modifier.widthIn(max = 310.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    msg.actions.forEach { action ->
                                        IosActionCard(
                                            action = action,
                                            onNavigateToTab = onNavigateToTab,
                                            onStartDMWithDraft = onStartDMWithDraft,
                                            onOpenSoundscapes = onOpenSoundscapes,
                                            onOpenVault = onOpenVault,
                                            onLaunchGame = onLaunchGame,
                                            onCopyText = { textToCopy ->
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("EchoBot Draft", textToCopy)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Copied draft to clipboard!", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Error message banner if state is Error
        if (uiState is EchoBotUiState.Error) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                color = Color(0x33FF453A),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF453A), modifier = Modifier.size(16.dp))
                    Text(
                        text = (uiState as EchoBotUiState.Error).message,
                        fontSize = 11.sp,
                        color = Color(0xFFFF453A),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Modern iOS Input Field with Send Button Dock
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF161618),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14FFFFFF))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Plus / Action Button
                Surface(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable {
                            viewModel.sendMessage("Hey EchoBot, give me a quick text wingman tip!")
                        },
                    shape = CircleShape,
                    color = Color(0xFF2C2C2E)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Options", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                // iOS Pill Input Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp, max = 100.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color(0xFF242426))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = "iMessage (EchoBot)",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }

                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF007AFF)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Modern iOS Circular Send Button (Transitions from Gray to Vibrant Blue)
                val canSend = inputText.isNotBlank() && !isGenerating
                Surface(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canSend) {
                            if (canSend) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                    shape = CircleShape,
                    color = if (canSend) Color(0xFF007AFF) else Color(0xFF2C2C2E)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility wrapper for GeminiChatScreen invoking IosChatScreen
 */
@Composable
fun GeminiChatScreen(
    modifier: Modifier = Modifier,
    viewModel: EchoBotViewModel = viewModel(),
    onNavigateToTab: (NavTab) -> Unit = {},
    onStartDMWithDraft: (userId: String, draft: String) -> Unit = { _, _ -> },
    onOpenSoundscapes: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    onLaunchGame: (gameId: String) -> Unit = {},
    aiService: GeminiChatService? = null
) {
    IosChatScreen(
        modifier = modifier,
        viewModel = viewModel,
        onNavigateToTab = onNavigateToTab,
        onStartDMWithDraft = onStartDMWithDraft,
        onOpenSoundscapes = onOpenSoundscapes,
        onOpenVault = onOpenVault,
        onLaunchGame = onLaunchGame,
        aiService = aiService
    )
}

@Composable
private fun IosActionCard(
    action: EchoAction,
    onNavigateToTab: (NavTab) -> Unit,
    onStartDMWithDraft: (userId: String, draft: String) -> Unit,
    onOpenSoundscapes: () -> Unit,
    onOpenVault: () -> Unit,
    onLaunchGame: (gameId: String) -> Unit,
    onCopyText: (String) -> Unit
) {
    when (action) {
        is EchoAction.CopyText -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onCopyText(action.text) },
                shape = RoundedCornerShape(12.dp),
                color = Color(0x33007AFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4D007AFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Copy Suggested Message", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("\"${action.preview}\"", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                    }
                    Text("1-Tap Copy", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                }
            }
        }

        is EchoAction.DmUser -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onStartDMWithDraft(action.userId, action.draftText) },
                shape = RoundedCornerShape(12.dp),
                color = Color(0x3334C759),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4D34C759))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF34C759), modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Send Message to ${action.userName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(if (action.draftText.length > 35) action.draftText.take(32) + "..." else action.draftText, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(14.dp))
                }
            }
        }

        is EchoAction.Navigate -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigateToTab(action.tab) },
                shape = RoundedCornerShape(12.dp),
                color = Color(0x26FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(action.tab.icon, contentDescription = action.title, tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(action.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(action.subtitle, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(14.dp))
                }
            }
        }

        is EchoAction.OpenSoundscape -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenSoundscapes() },
                shape = RoundedCornerShape(12.dp),
                color = Color(0x26FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Headphones, contentDescription = "Soundscape", tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                    Text(action.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                    Text("Play", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                }
            }
        }

        is EchoAction.OpenVault -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenVault() },
                shape = RoundedCornerShape(12.dp),
                color = Color(0x26FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Vault", tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                    Text(action.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                    Text("Open", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                }
            }
        }

        is EchoAction.LaunchArcadeGame -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onLaunchGame(action.gameId) },
                shape = RoundedCornerShape(12.dp),
                color = Color(0x26FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.SportsEsports, contentDescription = "Arcade", tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                    Text("Play ${action.gameName} in Arcade", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
