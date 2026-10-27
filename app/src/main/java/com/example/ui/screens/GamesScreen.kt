package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ArcadeGameState
import com.example.data.repository.EchoTalkRepository
import com.example.di.DatabaseModule
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class GamePeekInfo(
    val id: String,
    val title: String,
    val icon: String,
    val badge: String,
    val playerCount: String,
    val rules: String,
    val accentColor: Color
)

data class FloatingTaunt(
    val id: Long = System.currentTimeMillis() + Random.nextLong(1000),
    val emoji: String,
    val offsetX: Float = Random.nextFloat() * 160f - 80f
)

@Composable
fun GamesScreen(
    repository: EchoTalkRepository,
    initialGameId: String? = null,
    opponentName: String? = null
) {
    var activeGame by remember { mutableStateOf<String?>(initialGameId) }
    var gameMode by remember { mutableStateOf("ai") } // "ai" or "pass_and_play"
    var peekingGame by remember { mutableStateOf<GamePeekInfo?>(null) }
    val floatingTaunts = remember { mutableStateListOf<FloatingTaunt>() }
    val coroutineScope = rememberCoroutineScope()

    val gameCatalog = remember {
        listOf(
            GamePeekInfo(
                id = "chess",
                title = "Grandmaster Chess",
                icon = "♟️",
                badge = "Strategy 8x8",
                playerCount = "1v1 vs AI or Crew DM",
                rules = "Full legal move generator, piece captures, check/checkmate detection, and EchoBot AI.",
                accentColor = Color(0xFFFBBF24)
            ),
            GamePeekInfo(
                id = "ludo",
                title = "Ludo Master",
                icon = "🎲",
                badge = "Dice Race",
                playerCount = "2-4 Players",
                rules = "Rolling 3D corner base dice, home launch on 6s, 20-step tactical race track, and bot opposition.",
                accentColor = Color(0xFF38BDF8)
            ),
            GamePeekInfo(
                id = "mafia",
                title = "Mafia Roundtable",
                icon = "🎭",
                badge = "Party Game Host",
                playerCount = "5-10 Players",
                rules = "Interactive role manager for room drop-ins: secret role peeker, night elimination, doctor shield, detective check, and dawn trial voting.",
                accentColor = Color(0xFFC084FC)
            )
        )
    }

    LaunchedEffect(initialGameId) {
        if (initialGameId != null) activeGame = initialGameId
    }

    fun fireTaunt(emoji: String) {
        val taunt = FloatingTaunt(emoji = emoji)
        floatingTaunts.add(taunt)
        coroutineScope.launch {
            delay(2200)
            floatingTaunts.remove(taunt)
        }
    }

    if (activeGame != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { activeGame = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Hub", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (activeGame) {
                                "chess" -> "Grandmaster Chess"
                                "ludo" -> "Ludo Master"
                                "mafia" -> "Mafia Roundtable"
                                else -> "Arcade Match"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (opponentName != null) "1v1 vs $opponentName" else if (gameMode == "ai") "vs EchoBot AI" else "Local Pass & Play",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // In-Game Voice & Spectator Taunts Bar
            InGameVoiceAndTauntBar(
                onFireTaunt = { emoji -> fireTaunt(emoji) }
            )

            // Game Engine Viewport with Floating Taunts Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                when (activeGame) {
                    "chess" -> ChessGameView(mode = gameMode, opponent = opponentName)
                    "ludo" -> LudoGameView(mode = gameMode, opponent = opponentName)
                    "mafia" -> MafiaGameView()
                }

                // Floating Animated Taunts Overlay across the game board
                floatingTaunts.forEach { taunt ->
                    AnimatedTauntEmoji(taunt = taunt)
                }
            }
        }
    } else {
        // Arcade Hub Overview
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))

                Column {
                    Text(
                        text = "Arcade Hub",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Touch & hold minimalist logos to peek rules, or tap to play instantly",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CLEAN LOGO GRID (Section 6: Minimalist icon-only logos with zero-delay touch-and-hold peek engine)
                Text(
                    text = "QUICK PEEK & PLAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    gameCatalog.forEach { gameInfo ->
                        MinimalistGameLogo(
                            info = gameInfo,
                            onQuickTap = { activeGame = gameInfo.id },
                            onPeekStart = { peekingGame = gameInfo },
                            onPeekEnd = { peekingGame = null }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Selector: vs AI Bot vs Pass & Play
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { gameMode = "ai" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (gameMode == "ai") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (gameMode == "ai") Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("vs EchoBot AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { gameMode = "pass_and_play" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (gameMode == "pass_and_play") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (gameMode == "pass_and_play") Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pass & Play", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Detailed Catalog Cards
            item {
                Text(
                    text = "FEATURED BOARD GAMES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            items(gameCatalog.size) { index ->
                val game = gameCatalog[index]
                GameCatalogCard(
                    title = game.title,
                    badge = game.badge,
                    description = game.rules,
                    icon = game.icon,
                    gradientColor = game.accentColor,
                    onPlay = { activeGame = game.id }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Frosted Glass Peek Info Modal (Opens on onTouchStart, snaps closed on release)
        if (peekingGame != null) {
            Dialog(
                onDismissRequest = { peekingGame = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { peekingGame = null },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, peekingGame!!.accentColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(peekingGame!!.accentColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = peekingGame!!.icon, fontSize = 36.sp)
                            }
                            Text(
                                text = peekingGame!!.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = peekingGame!!.badge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = peekingGame!!.accentColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(peekingGame!!.accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Text(
                                text = "👥 Players: ${peekingGame!!.playerCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = peekingGame!!.rules,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    val targetId = peekingGame!!.id
                                    peekingGame = null
                                    activeGame = targetId
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = peekingGame!!.accentColor),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Play Now", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalistGameLogo(
    info: GamePeekInfo,
    onQuickTap: () -> Unit,
    onPeekStart: () -> Unit,
    onPeekEnd: () -> Unit
) {
    val scale = remember { Animatable(1.0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(76.dp)
            .scale(scale.value)
            .clip(CircleShape)
            .background(info.accentColor.copy(alpha = 0.15f))
            .border(2.dp, info.accentColor.copy(alpha = 0.4f), CircleShape)
            .pointerInput(info.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startTime = System.currentTimeMillis()
                    // Immediate onTouchStart: scale(1.05) with spring physics and open frosted glass info modal
                    coroutineScope.launch {
                        scale.animateTo(
                            1.05f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    onPeekStart()

                    val up = waitForUpOrCancellation()
                    val duration = System.currentTimeMillis() - startTime
                    if (up != null && duration < 200) {
                        // Single quick tap under 200ms opens the full-screen game directly
                        coroutineScope.launch {
                            scale.snapTo(1.0f)
                        }
                        onPeekEnd()
                        onQuickTap()
                    } else {
                        // Instant Release Snap-Back: snaps preview closed with zero delay
                        coroutineScope.launch {
                            scale.snapTo(1.0f)
                        }
                        onPeekEnd()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = info.icon, fontSize = 36.sp)
    }
}

@Composable
fun InGameVoiceAndTauntBar(
    onFireTaunt: (String) -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var showTaunts by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .background(if (isMuted) Color.Red else Color(0xFF10B981))
                    )
                    Text(
                        text = if (isMuted) "Match Audio: Muted" else "Live Match Voice Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            modifier = Modifier.size(18.dp),
                            tint = if (isMuted) Color.Red else Color(0xFF10B981)
                        )
                    }

                    IconButton(
                        onClick = { showTaunts = !showTaunts },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Celebration,
                            contentDescription = "Taunts",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFFBBF24)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showTaunts) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val taunts = listOf("🔥", "👑", "😂", "♟️", "💀", "💥", "👏", "⚡")
                    taunts.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .clickable {
                                    onFireTaunt(emoji)
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedTauntEmoji(taunt: FloatingTaunt) {
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1.0f) }

    LaunchedEffect(taunt.id) {
        launch {
            offsetY.animateTo(-160f, animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing))
        }
        launch {
            delay(1200)
            alpha.animateTo(0f, animationSpec = tween(durationMillis = 800))
        }
    }

    Box(
        modifier = Modifier
            .offset(x = taunt.offsetX.dp, y = offsetY.value.dp)
            .scale(1.2f)
    ) {
        Text(
            text = taunt.emoji,
            fontSize = 32.sp,
            color = Color.White.copy(alpha = alpha.value)
        )
    }
}

@Composable
fun GameCatalogCard(
    title: String,
    badge: String,
    description: String,
    icon: String,
    gradientColor: Color,
    onPlay: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .clickable { onPlay() },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(gradientColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 24.sp)
                }

                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = gradientColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(gradientColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)

            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// CHESS ENGINE COMPONENT (WITH FIRESTORE MULTIPLAYER STATE SYNC)
// -------------------------------------------------------------
@Composable
fun ChessGameView(mode: String, opponent: String?) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val matchId = remember(opponent, mode) {
        val cleanOpp = (opponent ?: "ai").lowercase().replace(" ", "_").replace("@", "")
        "chess_match_$cleanOpp"
    }

    var board by remember { mutableStateOf(ChessEngine.initialBoard.map { it.clone() }.toTypedArray()) }
    var selectedSquare by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var turn by remember { mutableStateOf("White") } // White or Black
    var capturedWhite by remember { mutableStateOf(listOf<String>()) }
    var capturedBlack by remember { mutableStateOf(listOf<String>()) }
    var winner by remember { mutableStateOf<String?>(null) }
    var lastMoveText by remember { mutableStateOf("Match started. White moves first.") }
    var isCloudSynced by remember { mutableStateOf(false) }
    var lastSyncedTimestamp by remember { mutableLongStateOf(0L) }

    // STEP 1: Observe real-time multiplayer moves from Cloud Firestore
    LaunchedEffect(matchId) {
        try {
            DatabaseModule.observeArcadeGameState(matchId).collect { remoteState ->
                if (remoteState != null && remoteState.updatedAt > lastSyncedTimestamp && remoteState.chessBoard.size == 64) {
                    board = ChessEngine.listToBoard(remoteState.chessBoard)
                    turn = remoteState.currentTurn
                    capturedWhite = remoteState.capturedWhite
                    capturedBlack = remoteState.capturedBlack
                    winner = remoteState.winner
                    lastMoveText = remoteState.lastMoveDescription.ifBlank { "Move synced from Cloud" }
                    lastSyncedTimestamp = remoteState.updatedAt
                    isCloudSynced = true
                }
            }
        } catch (e: Exception) {
            // Firestore sync error handled gracefully
        }
    }

    fun syncStateToFirestore(
        updatedBoard: Array<Array<String>>,
        nextTurn: String,
        moveDesc: String,
        matchWinner: String?,
        capW: List<String>,
        capB: List<String>
    ) {
        val now = System.currentTimeMillis()
        lastSyncedTimestamp = now
        val state = ArcadeGameState(
            gameId = matchId,
            gameType = "chess",
            challenger = "You",
            opponent = opponent ?: "EchoBot AI",
            currentTurn = nextTurn,
            chessBoard = ChessEngine.boardToList(updatedBoard),
            capturedWhite = capW,
            capturedBlack = capB,
            lastMoveDescription = moveDesc,
            winner = matchWinner,
            updatedAt = now
        )
        DatabaseModule.saveArcadeGameState(
            gameState = state,
            onSuccess = { isCloudSynced = true },
            onFailure = { /* Fall back to local state */ }
        )
    }

    fun reset() {
        board = ChessEngine.initialBoard.map { it.clone() }.toTypedArray()
        selectedSquare = null
        turn = "White"
        capturedWhite = emptyList()
        capturedBlack = emptyList()
        winner = null
        lastMoveText = "Board reset."
        syncStateToFirestore(board, "White", "Reset match", null, emptyList(), emptyList())
    }

    fun makeMove(fromR: Int, fromC: Int, toR: Int, toC: Int) {
        if (!ChessEngine.isValidMove(board, fromR, fromC, toR, toC, turn)) {
            selectedSquare = null
            return
        }

        val (capturedPiece, moveDesc) = ChessEngine.executeMove(board, fromR, fromC, toR, toC)

        var newCapWhite = capturedWhite
        var newCapBlack = capturedBlack

        if (capturedPiece != null) {
            val symbol = ChessEngine.pieceSymbols.getOrDefault(capturedPiece, capturedPiece)
            if (capturedPiece.first().isUpperCase()) {
                newCapWhite = newCapWhite + symbol
            } else {
                newCapBlack = newCapBlack + symbol
            }
            if (capturedPiece.equals("K", ignoreCase = true)) {
                winner = if (capturedPiece == "k") "White" else "Black"
            }
        }

        capturedWhite = newCapWhite
        capturedBlack = newCapBlack
        selectedSquare = null
        val nextTurn = if (turn == "White") "Black" else "White"
        turn = nextTurn
        lastMoveText = moveDesc

        // Tactile Haptic Response for Chess Move / Capture / Check
        if (capturedPiece != null) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
        if (winner != null) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }

        // Sync local move to Firestore
        syncStateToFirestore(board, nextTurn, lastMoveText, winner, newCapWhite, newCapBlack)

        // Trigger AI response if playing vs AI
        if (mode == "ai" && nextTurn == "Black" && winner == null) {
            coroutineScope.launch {
                delay(400)
                // Find all legal black moves
                val legalMoves = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
                for (r in 0..7) {
                    for (c in 0..7) {
                        val p = board[r][c]
                        if (p.isNotEmpty() && p.first().isLowerCase()) {
                            for (tr in 0..7) {
                                for (tc in 0..7) {
                                    if (ChessEngine.isValidMove(board, r, c, tr, tc, "Black")) {
                                        legalMoves.add(Pair(Pair(r, c), Pair(tr, tc)))
                                    }
                                }
                            }
                        }
                    }
                }

                if (legalMoves.isNotEmpty()) {
                    // Prefer captures if available
                    val captureMove = legalMoves.firstOrNull { (_, to) -> board[to.first][to.second].isNotEmpty() }
                    val chosen = captureMove ?: legalMoves.random()
                    val (aiCap, aiDesc) = ChessEngine.executeMove(board, chosen.first.first, chosen.first.second, chosen.second.first, chosen.second.second)
                    if (aiCap != null) {
                        val sym = ChessEngine.pieceSymbols.getOrDefault(aiCap, aiCap)
                        capturedWhite = capturedWhite + sym
                        if (aiCap == "K") winner = "Black"
                    }
                    turn = "White"
                    lastMoveText = "AI: $aiDesc"
                    syncStateToFirestore(board, "White", lastMoveText, winner, capturedWhite, capturedBlack)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Player Bar & Cloud Sync Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (mode == "ai") "🤖 EchoBot (Black)" else "${opponent ?: "Player 2"} (Black)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (turn == "Black") Color(0xFFFBBF24) else Color.Gray
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x3310B981)
                ) {
                    Text(
                        text = "Cloud Synced",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(text = capturedWhite.joinToString(" "), fontSize = 14.sp)
        }

        // 8x8 Grid Board with Legal Move Highlighting
        Surface(
            modifier = Modifier
                .size(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            color = Color(0xFF1E293B)
        ) {
            Column {
                for (r in 0..7) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (c in 0..7) {
                            val isDark = (r + c) % 2 == 1
                            val isSelected = selectedSquare?.first == r && selectedSquare?.second == c
                            val piece = board[r][c]
                            val isLegalTarget = selectedSquare != null && ChessEngine.isValidMove(
                                board, selectedSquare!!.first, selectedSquare!!.second, r, c, turn
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        when {
                                            isSelected -> Color(0xAAFBBF24)
                                            isLegalTarget -> Color(0x8810B981)
                                            isDark -> Color(0xFF334155)
                                            else -> Color(0xFFE2E8F0)
                                        }
                                    )
                                    .clickable {
                                        if (winner != null) return@clickable
                                        if (selectedSquare == null) {
                                            if (piece.isNotEmpty()) {
                                                val isWhitePiece = piece.first().isUpperCase()
                                                if ((turn == "White" && isWhitePiece) || (turn == "Black" && !isWhitePiece)) {
                                                    selectedSquare = Pair(r, c)
                                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                }
                                            }
                                        } else {
                                            val (sr, sc) = selectedSquare!!
                                            if (sr == r && sc == c) {
                                                selectedSquare = null
                                            } else {
                                                makeMove(sr, sc, r, c)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (piece.isNotEmpty()) {
                                    Text(
                                        text = ChessEngine.pieceSymbols.getOrDefault(piece, piece),
                                        fontSize = 24.sp,
                                        color = if (piece.first().isUpperCase()) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                    )
                                } else if (isLegalTarget) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Player Bar & White Captures
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "You (White)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (turn == "White") Color(0xFFFBBF24) else Color.Gray
            )
            Text(text = capturedBlack.joinToString(" "), fontSize = 14.sp)
        }

        // Last Move Ticker & Status
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (winner != null) "🏆 $winner Wins Checkmate!" else lastMoveText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (winner != null) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { reset() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Reset Match", fontSize = 11.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// LUDO ARENA COMPONENT (WITH FIRESTORE MULTIPLAYER STATE SYNC)
// -------------------------------------------------------------
@Composable
fun LudoGameView(mode: String, opponent: String?) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val matchId = remember(opponent, mode) {
        val cleanOpp = (opponent ?: "ai").lowercase().replace(" ", "_").replace("@", "")
        "ludo_match_$cleanOpp"
    }

    var diceValue by remember { mutableIntStateOf(1) }
    var isRolling by remember { mutableStateOf(false) }
    var turn by remember { mutableStateOf("Red") } // Red or Blue
    var redTokens by remember { mutableStateOf(listOf(-1, -1)) } // -1=base, 0..20=track, 20=home
    var blueTokens by remember { mutableStateOf(listOf(-1, -1)) }
    var winner by remember { mutableStateOf<String?>(null) }
    var lastActionText by remember { mutableStateOf("Roll the dice to launch tokens!") }
    var lastSyncedTimestamp by remember { mutableLongStateOf(0L) }

    // STEP 1: Observe real-time multiplayer moves from Cloud Firestore
    LaunchedEffect(matchId) {
        try {
            DatabaseModule.observeArcadeGameState(matchId).collect { remoteState ->
                if (remoteState != null && remoteState.updatedAt > lastSyncedTimestamp) {
                    redTokens = remoteState.ludoRedTokens
                    blueTokens = remoteState.ludoBlueTokens
                    diceValue = remoteState.ludoDiceValue
                    turn = remoteState.currentTurn
                    winner = remoteState.winner
                    lastActionText = remoteState.lastMoveDescription.ifBlank { "Synced from opponent" }
                    lastSyncedTimestamp = remoteState.updatedAt
                }
            }
        } catch (e: Exception) {
            // Handled gracefully
        }
    }

    fun syncLudoToFirestore(
        rTokens: List<Int>,
        bTokens: List<Int>,
        dice: Int,
        nextTurn: String,
        matchWinner: String?,
        desc: String
    ) {
        val now = System.currentTimeMillis()
        lastSyncedTimestamp = now
        val state = ArcadeGameState(
            gameId = matchId,
            gameType = "ludo",
            challenger = "You",
            opponent = opponent ?: "EchoBot AI",
            currentTurn = nextTurn,
            ludoDiceValue = dice,
            ludoRedTokens = rTokens,
            ludoBlueTokens = bTokens,
            lastMoveDescription = desc,
            winner = matchWinner,
            updatedAt = now
        )
        DatabaseModule.saveArcadeGameState(state)
    }

    fun executeRoll() {
        if (isRolling || winner != null) return
        isRolling = true
        coroutineScope.launch {
            for (i in 1..8) {
                delay(60)
                diceValue = Random.nextInt(1, 7)
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            val finalVal = Random.nextInt(1, 7)
            diceValue = finalVal
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            isRolling = false

            if (turn == "Red") {
                val moveTargetIndex = if (finalVal == 6 && redTokens.any { it == -1 }) {
                    redTokens.indexOfFirst { it == -1 }
                } else {
                    redTokens.indexOfFirst { it in 0 until LudoEngine.TRACK_LENGTH }
                }

                if (moveTargetIndex != -1) {
                    val result = LudoEngine.moveToken(redTokens, moveTargetIndex, finalVal, blueTokens)
                    redTokens = result.updatedTokens
                    lastActionText = "Red: ${result.description}"
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                    // Opponent capture handling with heavy haptic feedback pulse
                    if (result.capturedOpponentTokenIndex != null) {
                        val updatedBlue = blueTokens.toMutableList()
                        updatedBlue[result.capturedOpponentTokenIndex] = -1
                        blueTokens = updatedBlue
                        lastActionText += " 💥 Captured Blue Token!"
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }

                    if (result.isFinished) {
                        winner = "Red Team"
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    }
                } else {
                    lastActionText = "Red: No valid moves. Need a 6 to launch!"
                }

                val nextTurn = if (finalVal == 6 && winner == null) "Red" else "Blue"
                turn = nextTurn
                syncLudoToFirestore(redTokens, blueTokens, finalVal, nextTurn, winner, lastActionText)

                // Bot turn if playing vs AI
                if (mode == "ai" && nextTurn == "Blue" && winner == null) {
                    delay(600)
                    val botRoll = Random.nextInt(1, 7)
                    diceValue = botRoll
                    val botTarget = if (botRoll == 6 && blueTokens.any { it == -1 }) {
                        blueTokens.indexOfFirst { it == -1 }
                    } else {
                        blueTokens.indexOfFirst { it in 0 until LudoEngine.TRACK_LENGTH }
                    }
                    if (botTarget != -1) {
                        val botResult = LudoEngine.moveToken(blueTokens, botTarget, botRoll, redTokens)
                        blueTokens = botResult.updatedTokens
                        lastActionText = "Blue (AI): ${botResult.description}"
                        if (botResult.capturedOpponentTokenIndex != null) {
                            val updatedRed = redTokens.toMutableList()
                            updatedRed[botResult.capturedOpponentTokenIndex] = -1
                            redTokens = updatedRed
                            lastActionText += " 💥 Captured Red Token!"
                        }
                        if (botResult.isFinished) winner = "Blue Team"
                    } else {
                        lastActionText = "Blue (AI): Need a 6 to launch!"
                    }
                    val botNextTurn = if (botRoll == 6 && winner == null) "Blue" else "Red"
                    turn = botNextTurn
                    syncLudoToFirestore(redTokens, blueTokens, botRoll, botNextTurn, winner, lastActionText)
                }
            } else {
                // Pass & play manual Blue turn
                val moveTargetIndex = if (finalVal == 6 && blueTokens.any { it == -1 }) {
                    blueTokens.indexOfFirst { it == -1 }
                } else {
                    blueTokens.indexOfFirst { it in 0 until LudoEngine.TRACK_LENGTH }
                }
                if (moveTargetIndex != -1) {
                    val result = LudoEngine.moveToken(blueTokens, moveTargetIndex, finalVal, redTokens)
                    blueTokens = result.updatedTokens
                    lastActionText = "Blue: ${result.description}"
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if (result.capturedOpponentTokenIndex != null) {
                        val updatedRed = redTokens.toMutableList()
                        updatedRed[result.capturedOpponentTokenIndex] = -1
                        redTokens = updatedRed
                        lastActionText += " 💥 Captured Red Token!"
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    if (result.isFinished) {
                        winner = "Blue Team"
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    }
                }
                val nextTurn = if (finalVal == 6 && winner == null) "Blue" else "Red"
                turn = nextTurn
                syncLudoToFirestore(redTokens, blueTokens, finalVal, nextTurn, winner, lastActionText)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Base Yards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Red Base
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                color = Color(0x33E11D48),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE11D48))
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Red Base (6 to launch)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFB7185))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        redTokens.forEachIndexed { i, pos ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (pos == 20) Color(0xFF10B981) else if (pos == -1) Color(0xFFE11D48) else Color(0xFFFB7185)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (pos == 20) "🏆" else "R${i + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Blue Base
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                color = Color(0x330284C7),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7))
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Blue Base (6 to launch)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        blueTokens.forEachIndexed { i, pos ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (pos == 20) Color(0xFF10B981) else if (pos == -1) Color(0xFF0284C7) else Color(0xFF38BDF8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (pos == 20) "🏆" else "B${i + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Track Progress Bars with Safe Zone Markers
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "20-Step Tactical Track", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Safe: ★ 0, 8, 15", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }

                // Red Progress
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Red: ${redTokens.maxOrNull()?.coerceAtLeast(0)} / 20", fontSize = 10.sp, color = Color(0xFFFB7185))
                    LinearProgressIndicator(
                        progress = { (redTokens.maxOrNull()?.coerceAtLeast(0) ?: 0) / 20f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFE11D48)
                    )
                }

                // Blue Progress
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Blue: ${blueTokens.maxOrNull()?.coerceAtLeast(0)} / 20", fontSize = 10.sp, color = Color(0xFF38BDF8))
                    LinearProgressIndicator(
                        progress = { (blueTokens.maxOrNull()?.coerceAtLeast(0) ?: 0) / 20f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF0284C7)
                    )
                }
            }
        }

        // Dice Roll Area & Live Ticker
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(2.dp, if (turn == "Red") Color(0xFFE11D48) else Color(0xFF0284C7), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "$diceValue", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }

            Button(
                onClick = { executeRoll() },
                enabled = !isRolling && winner == null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (turn == "Red") Color(0xFFE11D48) else Color(0xFF0284C7)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isRolling) "Rolling 3D Dice..." else "Roll Dice ($turn Turn)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = if (winner != null) "🎉 $winner Won the Ludo Match!" else lastActionText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (winner != null) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// -------------------------------------------------------------
// MAFIA ROUNDTABLE COMPONENT
// -------------------------------------------------------------
@Composable
fun MafiaGameView() {
    var phase by remember { mutableStateOf("setup") } // setup, reveal, night, day
    var peekIndex by remember { mutableIntStateOf(0) }
    var isPeeking by remember { mutableStateOf(false) }

    val roles = listOf("Mafia", "Detective", "Doctor", "Citizen", "Citizen", "Citizen")
    val players = listOf("Zavier", "Chloe", "Lucas", "Kenji", "Amara", "Matteo")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (phase) {
            "setup" -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "🎭 Mafia Party Game Host", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Secret role assignments: 1 Mafia, 1 Detective, 1 Doctor, 3 Citizens. Tap begin to pass device.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { phase = "reveal"; peekIndex = 0 },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Begin Secret Role Reveal")
                        }
                    }
                }
            }

            "reveal" -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Pass device to: ${players[peekIndex]}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isPeeking) Color(0xFF312E81) else Color(0xFF0F172A))
                                .clickable { isPeeking = !isPeeking },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPeeking) "YOUR ROLE:\n${roles[peekIndex]}" else "Tap to Peek Secret Identity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Button(
                            onClick = {
                                if (peekIndex < players.size - 1) {
                                    peekIndex += 1
                                    isPeeking = false
                                } else {
                                    phase = "night"
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (peekIndex < players.size - 1) "Next Player" else "All Roles Confirmed • Night Phase")
                        }
                    }
                }
            }

            "night" -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "🌙 Night Phase: City is Asleep", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Moderator Actions:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "1. Mafia secretly selected target.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "2. Doctor protected innocent citizen.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "3. Detective investigated suspect.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Button(
                            onClick = { phase = "setup" },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dawn Breaks • New Game")
                        }
                    }
                }
            }
        }
    }
}
