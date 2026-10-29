package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ArcadeGameState
import com.example.data.model.GameChallengeData
import com.example.data.repository.EchoTalkRepository
import com.example.di.DatabaseModule
import com.example.ui.screens.ChessEngine
import com.example.ui.screens.LudoEngine
import com.example.data.local.AppRoomDatabase
import com.example.data.local.OfflineQueueManager
import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.entity.LocalChatMessageEntity
import com.example.data.model.ChatMessage
import com.example.data.storage.ConflictResolutionEngine
import com.example.data.storage.DriveBackupPayload
import com.example.data.storage.LocalDataSnapshot
import com.example.data.storage.StorageClassificationMatrix
import com.example.data.storage.StorageTarget
import com.example.data.storage.UserPreferencesBackup
import com.example.fcm.FcmNotificationManager
import com.example.ui.components.SpringPhysics
import com.example.ui.screens.ChessBoardManager
import com.example.ui.theme.ThemeManager
import com.example.utils.PoliteSoundSynthesizer
import com.example.worker.SecurityVaultScanWorker
import kotlinx.coroutines.runBlocking
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("EchoTalk", appName)
  }

  @Test
  fun `read firestore_database_id from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbId = context.getString(R.string.firestore_database_id)
    assertEquals("(default)", dbId)
  }

  @Test
  fun `verify conversation ID generation is deterministic`() {
    val id1 = DatabaseModule.getConversationId("alice", "bob")
    val id2 = DatabaseModule.getConversationId("bob", "alice")
    assertEquals("alice_bob", id1)
    assertEquals("alice_bob", id2)
  }

  @Test
  fun `verify echobot action parsing extracts actions and cleans text`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
      com.google.firebase.FirebaseApp.initializeApp(context)
    }

    val viewModel = com.example.ai.EchoBotViewModel()
    val sampleResponse = "Honestly, don't overthink it! Here is a great message to send Lucas: [ACTION:COPY_TEXT:Hey Lucas, up for chess later?] [ACTION:NAVIGATE:arcade]"
    val (cleanedText, actions) = viewModel.parseActionsFromText(sampleResponse)

    assertEquals("Honestly, don't overthink it! Here is a great message to send Lucas:", cleanedText)
    assertEquals(2, actions.size)
    assertTrue(actions.any { it is com.example.ai.EchoAction.CopyText })
    assertTrue(actions.any { it is com.example.ai.EchoAction.Navigate })
  }

  @Test
  fun `verify quick tasks are defined with prompts`() {
    val tasks = com.example.ai.EchoQuickTask.entries
    assertTrue(tasks.isNotEmpty())
    assertTrue(tasks.any { it == com.example.ai.EchoQuickTask.TEXT_COACH })
    assertTrue(tasks.all { it.prompt.isNotBlank() })
  }

  @Test
  fun `verify NavTab contains DISCOVER and excludes legacy CREW`() {
    val tabs = com.example.ui.components.NavTab.entries
    assertTrue(tabs.any { it.name == "DISCOVER" })
    assertTrue(tabs.none { it.name == "CREW" })
  }

  // =========================================================================
  // SIMULATION SUITE - PERSONA 1: STANDARD NEW USER
  // =========================================================================

  @Test
  fun `persona 1 - onboarding profile completion and custom attributes`() {
    val repo = EchoTalkRepository.getInstance()
    repo.updateCurrentUserProfile(
      name = "Zavier Sound",
      handle = "zavier_sf",
      bio = "Custom spatial acoustic bio",
      gender = "Male",
      age = 29,
      country = "United States",
      countryCode = "US",
      photos = listOf("https://example.com/p1.jpg", "https://example.com/p2.jpg"),
      bannerUrl = "https://example.com/banner.jpg",
      audioPreviewTitle = "Modular Synth Solo (5s)"
    )

    val currentUser = repo.currentUser.value
    assertEquals("Zavier Sound", currentUser.name)
    assertEquals("@zavier_sf", currentUser.handle)
    assertEquals("Custom spatial acoustic bio", currentUser.bio)
    assertEquals(29, currentUser.age)
    assertEquals(2, currentUser.photos.size)
    assertEquals("https://example.com/p1.jpg", currentUser.photos[0])
    assertEquals("Modular Synth Solo (5s)", currentUser.audioPreviewTitle)
  }

  @Test
  fun `persona 1 - discover multi-filtering and profile liking`() {
    val repo = EchoTalkRepository.getInstance()
    val allUsers = repo.users.value

    // Multi-filtering logic validation
    val maleUsers = allUsers.filter { it.gender.equals("Male", ignoreCase = true) }
    val femaleUsers = allUsers.filter { it.gender.equals("Female", ignoreCase = true) }
    assertTrue(maleUsers.isNotEmpty())
    assertTrue(femaleUsers.isNotEmpty())

    // Like toggle with counter adjustment
    val lucas = repo.users.value.first { it.id == "u-lucas" }
    val initialLikes = lucas.likesCount
    val wasLiked = lucas.isLiked

    val isNowLiked = repo.toggleLikeUser("u-lucas")
    val updatedLucas = repo.users.value.first { it.id == "u-lucas" }
    assertEquals(!wasLiked, isNowLiked)
    assertEquals(if (isNowLiked) initialLikes + 1 else initialLikes - 1, updatedLucas.likesCount)

    // Friend request state machine (none -> pending -> accepted)
    val elena = repo.users.value.first { it.id == "u-elena" }
    assertEquals("none", elena.friendStatus)

    repo.sendFriendRequest("u-elena")
    val pendingElena = repo.users.value.first { it.id == "u-elena" }
    assertEquals("pending", pendingElena.friendStatus)

    repo.sendFriendRequest("u-elena")
    val acceptedElena = repo.users.value.first { it.id == "u-elena" }
    assertEquals("accepted", acceptedElena.friendStatus)
    assertTrue(acceptedElena.isFriend)
  }

  @Test
  fun `persona 1 - lead to request 3-message limit cap enforcement`() {
    val repo = EchoTalkRepository.getInstance()
    // Find or set a non-friend user
    val nonFriend = repo.users.value.first { !it.isFriend }

    // First 3 messages should succeed
    val (ok1, err1) = repo.sendDM(nonFriend.id, "Hello 1")
    assertTrue(ok1)
    assertNull(err1)

    val (ok2, err2) = repo.sendDM(nonFriend.id, "Hello 2")
    assertTrue(ok2)
    assertNull(err2)

    val (ok3, err3) = repo.sendDM(nonFriend.id, "Hello 3")
    assertTrue(ok3)
    assertNull(err3)

    // 4th consecutive message must be blocked by lead-to-request cap
    val (ok4, err4) = repo.sendDM(nonFriend.id, "Hello 4")
    assertFalse(ok4)
    assertNotNull(err4)
    assertTrue(err4!!.contains("3-message limit reached"))
  }

  // =========================================================================
  // SIMULATION SUITE - PERSONA 2: GAMER & ARCADE PLAYER
  // =========================================================================

  @Test
  fun `persona 2 - chess legal move validation and pawn promotion`() {
    val board = ChessEngine.initialBoard

    // White Pawn e2 -> e4 (from rank 6 to 4 in 0-indexed 8x8)
    assertTrue(ChessEngine.isValidMove(board, 6, 4, 4, 4, "White"))
    // Illegal pawn backwards move
    assertFalse(ChessEngine.isValidMove(board, 6, 4, 7, 4, "White"))

    // Knight b1 -> c3 (rank 7 col 1 to rank 5 col 2)
    assertTrue(ChessEngine.isValidMove(board, 7, 1, 5, 2, "White"))

    // Execute move
    val (captured, desc) = ChessEngine.executeMove(board, 6, 4, 4, 4)
    assertNull(captured)
    assertTrue(desc.contains("e4"))
    assertEquals("P", board[4][4])
    assertEquals("", board[6][4])

    // Pawn Promotion: White pawn reaching rank 0 promotes to Queen
    val promoBoard = Array(8) { Array(8) { "" } }
    promoBoard[1][0] = "P"
    ChessEngine.executeMove(promoBoard, 1, 0, 0, 0)
    assertEquals("Q", promoBoard[0][0])
  }

  @Test
  fun `persona 2 - chess castling and check detection`() {
    val board = Array(8) { Array(8) { "" } }
    board[7][4] = "K"
    board[7][7] = "R"
    // White Kingside Castling: e1 -> g1 (7,4 -> 7,6) with rook at 7,7
    assertTrue(ChessEngine.isValidMove(board, 7, 4, 7, 6, "White"))

    ChessEngine.executeMove(board, 7, 4, 7, 6)
    assertEquals("K", board[7][6])
    assertEquals("R", board[7][5])
    assertEquals("", board[7][4])
    assertEquals("", board[7][7])

    // Check detection: Place opponent Queen on the same file
    board[2][6] = "q"
    assertTrue(ChessEngine.isCheck(board, isWhiteTurn = true))
  }

  @Test
  fun `persona 2 - ludo dice track traversal opponent capture and win`() {
    val tokens = listOf(-1, -1)
    val opponentTokens = listOf(4, 10)

    // Launch token on rolling 6
    val launchRes = LudoEngine.moveToken(tokens, 0, 6, opponentTokens)
    assertEquals(0, launchRes.updatedTokens[0])
    assertTrue(launchRes.description.contains("Launched"))

    // Move token along track to square 4 (where opponent token sits on non-safe square)
    val activeTokens = listOf(0, -1)
    val captureRes = LudoEngine.moveToken(activeTokens, 0, 4, opponentTokens)
    assertEquals(4, captureRes.updatedTokens[0])
    assertEquals(0, captureRes.capturedOpponentTokenIndex) // opponent token at index 0 captured!

    // Finish match
    val winningTokens = listOf(19, 20)
    val winRes = LudoEngine.moveToken(winningTokens, 0, 2, opponentTokens)
    assertEquals(20, winRes.updatedTokens[0])
    assertTrue(winRes.isFinished)
  }

  @Test
  fun `persona 2 - arcade game state serialization for firestore multiplayer`() {
    val state = ArcadeGameState(
      gameId = "chess_test_1",
      gameType = "chess",
      challenger = "Zavier",
      opponent = "Lucas",
      currentTurn = "White",
      chessBoard = ChessEngine.boardToList(ChessEngine.initialBoard),
      lastMoveDescription = "White opened e4"
    )
    assertEquals(64, state.chessBoard.size)
    assertEquals("chess", state.gameType)
    assertEquals("White", state.currentTurn)
  }

  // =========================================================================
  // SIMULATION SUITE - PERSONA 3: ACTIVE DM & CALLER
  // =========================================================================

  @Test
  fun `persona 3 - view once flame media single view disintegration`() {
    val repo = EchoTalkRepository.getInstance()
    val friend = repo.users.value.first { it.isFriend }

    repo.sendDM(
      recipientId = friend.id,
      text = "Secret view-once photo",
      isViewOnce = true,
      mediaUrl = "https://example.com/secret.jpg"
    )

    val thread = repo.dms.value[friend.id] ?: emptyList()
    val viewOnceMsg = thread.last { it.isViewOnce }
    assertFalse(viewOnceMsg.viewed)

    // Simulate opening view once
    repo.openViewOnce(friend.id, viewOnceMsg.id)

    val updatedThread = repo.dms.value[friend.id] ?: emptyList()
    val openedMsg = updatedThread.first { it.id == viewOnceMsg.id }
    assertTrue(openedMsg.viewed)
  }

  @Test
  fun `persona 3 - direct game challenge message dispatch`() {
    val repo = EchoTalkRepository.getInstance()
    val friend = repo.users.value.first { it.isFriend }

    val challenge = GameChallengeData(
      gameId = "chess",
      gameTitle = "Grandmaster Chess",
      challenger = "Zavier",
      opponent = friend.name
    )

    repo.sendDM(
      recipientId = friend.id,
      text = "Let's play Grandmaster Chess!",
      challengeData = challenge
    )

    val thread = repo.dms.value[friend.id] ?: emptyList()
    val chalMsg = thread.last()
    assertTrue(chalMsg.isChallenge)
    assertNotNull(chalMsg.challengeData)
    assertEquals("chess", chalMsg.challengeData?.gameId)
  }

  // =========================================================================
  // SIMULATION SUITE - PERSONA 4: OWNER & PLATFORM ADMIN
  // =========================================================================

  @Test
  fun `persona 4 - admin quarantine vault catches view-once and flagged media`() {
    val repo = EchoTalkRepository.getInstance()
    val initialVaultCount = repo.adminVault.value.size

    val friend = repo.users.value.first { it.isFriend }
    repo.sendDM(
      recipientId = friend.id,
      text = "Classified image for audit",
      isViewOnce = true,
      mediaUrl = "https://example.com/classified.png"
    )

    val vault = repo.adminVault.value
    val latestVaultEntry = vault.first()
    assertEquals("Encrypted View-Once image payload", latestVaultEntry.caption)
    assertEquals("https://example.com/classified.png", latestVaultEntry.mediaUrl)

    // Admin can delete entry
    repo.deleteVaultEntry(latestVaultEntry.id)
    assertEquals(initialVaultCount, repo.adminVault.value.size)
  }

  // =========================================================================
  // ROOM DATABASE OFFLINE QUEUE MANAGER TESTS
  // =========================================================================

  @Test
  fun `offline queue manager room persistence and auto flush`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = AppRoomDatabase.createInMemory(context)
    val dao = inMemoryDb.pendingQueueDao()
    val queueManager = OfflineQueueManager.createForTesting(context, dao)

    // Initially queue is empty
    assertEquals(0, queueManager.getPendingCount())

    // Enqueue pending message
    val task = queueManager.enqueueMessage(
      recipientId = "u-elena",
      senderName = "Zavier",
      text = "Queued offline message test",
      isViewOnce = false
    )
    assertNotNull(task.id)
    assertEquals(1, queueManager.getPendingCount())

    // Check entity persisted in Room
    val stored = dao.getById(task.id)
    assertNotNull(stored)
    assertEquals("Queued offline message test", stored!!.text)
    assertEquals("PENDING", stored.status)

    // Flush queue
    var deliveredMessage: ChatMessage? = null
    val flushedCount = queueManager.flushQueue { _, msg ->
      deliveredMessage = msg
    }

    assertEquals(1, flushedCount)
    assertEquals(0, queueManager.getPendingCount())
    assertNotNull(deliveredMessage)
    assertEquals("delivered", deliveredMessage!!.status)

    inMemoryDb.close()
  }

  // =========================================================================
  // SYS_SEC_VAULT_QUEUE & WORKER PIXEL ANALYSIS TESTS
  // =========================================================================

  @Test
  fun `security vault scan worker pixel analysis detection`() {
    // 1. Clean synthetic buffer (blue sky / cool neutral)
    val cleanBuffer = IntArray(100) { Color.rgb(30, 80, 180) }
    val cleanResult = SecurityVaultScanWorker.analyzePixels(cleanBuffer)
    assertFalse(cleanResult.isViolation)
    assertEquals(0f, cleanResult.skinToneRatio, 0.01f)

    // 2. High skin-chromaticity synthetic buffer
    // R > 95, G > 40, B > 20, max-min > 15, abs(R-G) > 15, R > G, R > B
    val skinBuffer = IntArray(100) { Color.rgb(210, 140, 100) }
    val flaggedResult = SecurityVaultScanWorker.analyzePixels(skinBuffer)
    assertTrue(flaggedResult.isViolation)
    assertTrue(flaggedResult.riskScore >= 35)
    assertTrue(flaggedResult.skinToneRatio >= 0.8f)
    assertTrue(flaggedResult.summary.contains("Flagged"))
  }

  // =========================================================================
  // STORAGE CLASSIFICATION MATRIX & DATA ISOLATION TESTS
  // =========================================================================

  @Test
  fun `storage classification matrix enforces server db vs google drive separation`() {
    // Server DB mappings
    assertEquals(StorageTarget.SERVER_DB, StorageClassificationMatrix.getAuthorizedTarget("profiles"))
    assertEquals(StorageTarget.SERVER_DB, StorageClassificationMatrix.getAuthorizedTarget("friendships"))
    assertEquals(StorageTarget.SERVER_DB, StorageClassificationMatrix.getAuthorizedTarget("admin_quarantine_vault"))
    assertEquals(StorageTarget.SERVER_DB, StorageClassificationMatrix.getAuthorizedTarget("active_game_sessions"))

    // Google Drive appDataFolder mappings
    assertEquals(StorageTarget.GOOGLE_DRIVE_APP_DATA, StorageClassificationMatrix.getAuthorizedTarget("chat_history_backup"))
    assertEquals(StorageTarget.GOOGLE_DRIVE_APP_DATA, StorageClassificationMatrix.getAuthorizedTarget("profile_photos_carousel_2_to_5"))
    assertEquals(StorageTarget.GOOGLE_DRIVE_APP_DATA, StorageClassificationMatrix.getAuthorizedTarget("user_settings_and_theme"))
    assertEquals(StorageTarget.GOOGLE_DRIVE_APP_DATA, StorageClassificationMatrix.getAuthorizedTarget("offline_message_queue"))

    // Zero cross-syncing validation
    assertTrue(StorageClassificationMatrix.validateZeroCrossSync("chat_history_backup", StorageTarget.GOOGLE_DRIVE_APP_DATA))
    assertFalse(StorageClassificationMatrix.validateZeroCrossSync("chat_history_backup", StorageTarget.SERVER_DB))
    assertTrue(StorageClassificationMatrix.validateZeroCrossSync("profiles", StorageTarget.SERVER_DB))
    assertFalse(StorageClassificationMatrix.validateZeroCrossSync("profiles", StorageTarget.GOOGLE_DRIVE_APP_DATA))
  }

  // =========================================================================
  // BACKUP CONFLICT RESOLUTION TESTS
  // =========================================================================

  @Test
  fun `backup conflict resolution union merge by id and status priority`() {
    val localMsgs = listOf(
      ChatMessage(id = "msg-1", text = "Hello", timestamp = "10:00 AM", status = "sent"),
      ChatMessage(id = "msg-2", text = "Local only", timestamp = "10:05 AM", status = "delivered")
    )

    val driveMsgs = listOf(
      ChatMessage(id = "msg-1", text = "Hello", timestamp = "10:00 AM", status = "seen"), // higher priority status
      ChatMessage(id = "msg-3", text = "Drive only", timestamp = "10:10 AM", status = "seen")
    )

    val merged = ConflictResolutionEngine.mergeMessageLists(localMsgs, driveMsgs)

    // Total 3 unique messages
    assertEquals(3, merged.size)
    val msg1 = merged.find { it.id == "msg-1" }
    assertNotNull(msg1)
    // Kept "seen" status (seen > delivered > sent)
    assertEquals("seen", msg1!!.status)

    // Both local-only and drive-only are present
    assertNotNull(merged.find { it.id == "msg-2" })
    assertNotNull(merged.find { it.id == "msg-3" })
  }

  @Test
  fun `backup conflict resolution user preferences latest timestamp wins`() {
    val olderLocal = UserPreferencesBackup(
      themeId = "classic",
      lastUpdatedIso = "2026-10-27T10:00:00.000Z"
    )

    val newerDrive = UserPreferencesBackup(
      themeId = "velvet_rose_gold",
      lastUpdatedIso = "2026-10-28T05:00:00.000Z"
    )

    val resolved = ConflictResolutionEngine.resolvePreferencesConflict(olderLocal, newerDrive)
    assertEquals("velvet_rose_gold", resolved.themeId)

    // In reverse (local is newer), local should be retained
    val newerLocal = UserPreferencesBackup(
      themeId = "midnight_emerald",
      lastUpdatedIso = "2026-10-28T09:00:00.000Z"
    )
    val resolvedLocalWins = ConflictResolutionEngine.resolvePreferencesConflict(newerLocal, newerDrive)
    assertEquals("midnight_emerald", resolvedLocalWins.themeId)
  }

  @Test
  fun `backup conflict resolution offline queue protection preserves un-sent items`() {
    val pendingOfflineMsg = ChatMessage(id = "offline-1", text = "Draft unsent", status = "sent")

    val localData = LocalDataSnapshot(
      messages = listOf(ChatMessage(id = "m-1", text = "Historic", status = "seen")),
      chatHistory = mapOf("u-chloe" to listOf(ChatMessage(id = "m-1", text = "Historic", status = "seen"))),
      preferences = UserPreferencesBackup(),
      offlineQueue = listOf(pendingOfflineMsg)
    )

    val driveData = DriveBackupPayload(
      chatHistory = mapOf("u-chloe" to listOf(ChatMessage(id = "m-2", text = "From Cloud", status = "seen")))
    )

    val result = ConflictResolutionEngine.resolveDataConflict(localData, driveData)

    // Offline queue MUST be preserved
    assertEquals(1, result.preservedOfflineQueue.size)
    assertEquals("offline-1", result.preservedOfflineQueue[0].id)

    // Chloe thread union merged
    val chloeThread = result.mergedChatHistory["u-chloe"]
    assertNotNull(chloeThread)
    assertEquals(2, chloeThread!!.size)
  }

  @Test
  fun `fcm notification token management and simulation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    FcmNotificationManager.initialize(context)

    val token = FcmNotificationManager.fcmToken.value
    assertNotNull(token)
    assertTrue(token!!.isNotEmpty())

    // Simulated notification execution does not crash
    FcmNotificationManager.simulateIncomingPush(
      context = context,
      senderName = "Chloe Dubois",
      message = "Hey Zavier, check this out!"
    )
  }

  @Test
  fun `firebase auth manager handles simulated google sign-in and passes token to drive manager`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = com.example.auth.FirebaseAuthManager.getInstance(context)

    // Reset state
    authManager.signOut()
    assertFalse(authManager.isSignedIn.value)
    assertNull(authManager.userEmail.value)

    // Simulate Google Sign-In
    authManager.simulateSignIn(
      userId = "user_google_123",
      email = "zavier.echotalk@gmail.com",
      displayName = "Zavier Sound",
      mockToken = "mock_drive_token_oauth_xyz"
    )

    assertTrue(authManager.isSignedIn.value)
    assertEquals("zavier.echotalk@gmail.com", authManager.userEmail.value)
    assertEquals("Zavier Sound", authManager.userDisplayName.value)
    assertTrue(authManager.promptRestoreDialog.value)

    // DriveManager token should now be configured
    val driveManager = com.example.data.storage.DriveManager.getInstance(context)
    assertEquals("mock_drive_token_oauth_xyz", driveManager.accessToken.value)
    assertTrue(driveManager.isConfigured())

    // Dismiss restore prompt
    authManager.dismissRestorePrompt()
    assertFalse(authManager.promptRestoreDialog.value)

    // Sign out clears both auth and DriveManager token
    authManager.signOut()
    assertFalse(authManager.isSignedIn.value)
    assertNull(driveManager.accessToken.value)
    assertFalse(driveManager.isConfigured())
  }

  @Test
  fun `echo talk repository resolveDataConflict merges cloud backup and protects offline queue`() {
    val repository = EchoTalkRepository.getInstance()

    // Setup an initial state with a local thread
    val initialMsg = ChatMessage(id = "msg-sync-1", sender = "Lucas Silva", text = "Original", status = "sent")
    repository.setDMsDirect(mapOf("u-lucas" to listOf(initialMsg)))

    // Remote backup has msg-sync-1 marked as 'seen' and a brand new message msg-sync-2
    val remoteUpdatedMsg = ChatMessage(id = "msg-sync-1", sender = "Lucas Silva", text = "Original", status = "seen")
    val remoteNewMsg = ChatMessage(id = "msg-sync-2", sender = "Lucas Silva", text = "Cloud added", status = "seen")

    repository.setTheme(com.example.data.repository.AppTheme.IOS_GLASS)

    val driveBackup = DriveBackupPayload(
      backupVersion = 1,
      timestamp = System.currentTimeMillis(),
      lastUpdatedIso = "2026-12-31T23:59:59.000Z",
      userPreferences = UserPreferencesBackup(
        themeId = "velvet_rose_gold",
        lastUpdatedIso = "2026-12-31T23:59:59.000Z"
      ),
      chatHistory = mapOf("u-lucas" to listOf(remoteUpdatedMsg, remoteNewMsg)),
      driveFileId = "drive-backup-cuj-test"
    )

    val mergeResult = repository.resolveDataConflict(driveBackup)

    // Merged thread for u-lucas should have 2 messages
    val lucasThread = repository.dms.value["u-lucas"]
    assertNotNull(lucasThread)
    assertEquals(2, lucasThread!!.size)

    // Status conflict was resolved to 'seen' (seen > sent)
    val msg1 = lucasThread.find { it.id == "msg-sync-1" }
    assertNotNull(msg1)
    assertEquals("seen", msg1!!.status)

    // Theme should be updated to velvet_rose_gold from drive backup
    assertEquals(com.example.data.repository.AppTheme.VELVET_ROSE_GOLD, repository.theme.value)
  }

  @Test
  fun `drive manager crud operations for appDataFolder and status indicator`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val driveManager = com.example.data.storage.DriveManager.getInstance(context)

    // Configure token
    driveManager.setAccessToken("test_access_token_appdata")
    assertTrue(driveManager.isConfigured())

    val payload = DriveBackupPayload(
      backupVersion = 1,
      timestamp = System.currentTimeMillis(),
      lastUpdatedIso = "2026-10-28T08:00:00.000Z",
      userPreferences = UserPreferencesBackup(themeId = "velvet_rose_gold"),
      chatHistory = mapOf(
        "u-chloe" to listOf(
          ChatMessage(id = "dm-c1", sender = "Chloe Dubois", text = "Testing Drive appDataFolder CRUD", status = "seen")
        )
      ),
      driveFileId = "drive-crud-file-1"
    )

    // Save backup pipeline updates cached payload and status string
    val saveResult = driveManager.saveBackup(payload)
    assertTrue(saveResult.isSuccess)
    assertNotNull(driveManager.cachedPayload.value)
    assertEquals("drive-crud-file-1", driveManager.cachedPayload.value!!.driveFileId)

    // Status indicator verification
    val statusText = driveManager.lastBackupStatus.value
    assertTrue(statusText.startsWith("Last backed up to Google Drive:"))

    // Fetch latest backup pipeline retrieves cached or remote
    val fetchResult = driveManager.fetchLatestBackup()
    assertTrue(fetchResult.isSuccess)
    val fetchedPayload = fetchResult.getOrNull()
    assertNotNull(fetchedPayload)
    assertEquals(1, fetchedPayload!!.chatHistory["u-chloe"]?.size)
  }

  @Test
  fun `polite sound synthesizer generates valid mathematical PCM 16-bit buffers without external assets`() {
    // Verify send tone buffer (140ms at 44100Hz = 6174 samples)
    val sendTone = PoliteSoundSynthesizer.generateSendTone()
    assertNotNull(sendTone)
    assertEquals(6174, sendTone.size)

    // Verify peak amplitude does not clip or overflow Short range
    var maxSendAmp = 0
    for (sample in sendTone) {
      val absVal = kotlin.math.abs(sample.toInt())
      if (absVal > maxSendAmp) maxSendAmp = absVal
    }
    assertTrue("Send tone must produce audible amplitude", maxSendAmp > 500)
    assertTrue("Send tone must not clip 16-bit PCM ceiling", maxSendAmp <= Short.MAX_VALUE)

    // Verify receive tone buffer (180ms at 44100Hz = 7938 samples)
    val receiveTone = PoliteSoundSynthesizer.generateReceiveTone()
    assertNotNull(receiveTone)
    assertEquals(7938, receiveTone.size)

    var maxReceiveAmp = 0
    for (sample in receiveTone) {
      val absVal = kotlin.math.abs(sample.toInt())
      if (absVal > maxReceiveAmp) maxReceiveAmp = absVal
    }
    assertTrue("Receive tone must produce audible amplitude", maxReceiveAmp > 400)
    assertTrue("Receive tone must not clip 16-bit PCM ceiling", maxReceiveAmp <= Short.MAX_VALUE)

    // Verify asynchronous trigger calls execute safely without exception
    PoliteSoundSynthesizer.playPoliteSendSound()
    PoliteSoundSynthesizer.playPoliteReceiveSound()
  }

  @Test
  fun `spring physics cubic bezier curves calculate boundary and overshoot values`() {
    // BounceCurve cubic-bezier(0.34, 1.56, 0.64, 1.0)
    val bounceStart = SpringPhysics.BounceCurve.transform(0.0f)
    val bounceEnd = SpringPhysics.BounceCurve.transform(1.0f)
    assertEquals(0.0f, bounceStart, 0.001f)
    assertEquals(1.0f, bounceEnd, 0.001f)

    // BounceCurve overshoots or elevates above linear at midpoint
    val bounceMid = SpringPhysics.BounceCurve.transform(0.5f)
    assertTrue("BounceCurve midpoint should be advanced above 0.5f", bounceMid > 0.5f)

    // SoftCurve cubic-bezier(0.175, 0.885, 0.32, 1.275)
    val softStart = SpringPhysics.SoftCurve.transform(0.0f)
    val softEnd = SpringPhysics.SoftCurve.transform(1.0f)
    assertEquals(0.0f, softStart, 0.001f)
    assertEquals(1.0f, softEnd, 0.001f)

    val softMid = SpringPhysics.SoftCurve.transform(0.5f)
    assertTrue("SoftCurve midpoint should be elevated above 0.5f", softMid > 0.5f)
  }

  @Test
  fun `theme manager handles Velvet Rose Gold and Midnight Slate with dynamic color injection`() {
    val repository = EchoTalkRepository.getInstance()

    // Test Velvet Rose Gold
    ThemeManager.setTheme(com.example.data.repository.AppTheme.VELVET_ROSE_GOLD)
    assertEquals(com.example.data.repository.AppTheme.VELVET_ROSE_GOLD, repository.theme.value)
    val roseGoldColors = ThemeManager.getCustomColors(com.example.data.repository.AppTheme.VELVET_ROSE_GOLD)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFE8A5C8), roseGoldColors.primaryAccent)
    assertTrue(roseGoldColors.isDark)
    assertNotNull(roseGoldColors.gradientBrush)
    val roseGoldScheme = ThemeManager.getColorScheme(com.example.data.repository.AppTheme.VELVET_ROSE_GOLD)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFE8A5C8), roseGoldScheme.primary)

    // Test Midnight Slate
    ThemeManager.setTheme(com.example.data.repository.AppTheme.MIDNIGHT_SLATE)
    assertEquals(com.example.data.repository.AppTheme.MIDNIGHT_SLATE, repository.theme.value)
    val slateColors = ThemeManager.getCustomColors(com.example.data.repository.AppTheme.MIDNIGHT_SLATE)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF38BDF8), slateColors.primaryAccent)
    assertTrue(slateColors.isDark)
    assertNotNull(slateColors.gradientBrush)
    val slateScheme = ThemeManager.getColorScheme(com.example.data.repository.AppTheme.MIDNIGHT_SLATE)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF38BDF8), slateScheme.primary)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF0F172A), slateScheme.background)
  }

  @Test
  fun `chess board state manager parses FEN and validates turn-based moves`() {
    val startingState = ChessBoardManager.parseFen(ChessBoardManager.STARTING_FEN)
    assertEquals("w", startingState.activeColor)
    assertEquals("KQkq", startingState.castlingRights)
    assertEquals("-", startingState.enPassantTarget)
    assertEquals(0, startingState.halfmoveClock)
    assertEquals(1, startingState.fullmoveNumber)
    assertFalse(startingState.isCheck)
    assertFalse(startingState.isCheckmate)

    // White pawn moves: e2 (row 6, col 4) to e4 (row 4, col 4) is valid
    assertTrue(ChessBoardManager.isValidMove(startingState, 6, 4, 4, 4))
    // e2 to e5 is invalid
    assertFalse(ChessBoardManager.isValidMove(startingState, 6, 4, 3, 4))
    // Black pawn move on White's turn is invalid
    assertFalse(ChessBoardManager.isValidMove(startingState, 1, 4, 3, 4))

    // Execute 1. e4
    val move1 = ChessBoardManager.executeMove(startingState, 6, 4, 4, 4)
    assertNotNull(move1)
    assertEquals("b", move1!!.newState.activeColor)
    assertEquals("e3", move1.newState.enPassantTarget)
    assertEquals(0, move1.newState.halfmoveClock)
    assertEquals(1, move1.newState.fullmoveNumber)
    assertFalse(move1.isCheck)

    // Black responds 1... e5 (row 1, col 4 to row 3, col 4)
    val move2 = ChessBoardManager.executeMove(move1.newState, 1, 4, 3, 4)
    assertNotNull(move2)
    assertEquals("w", move2!!.newState.activeColor)
    assertEquals("e6", move2.newState.enPassantTarget)
    assertEquals(2, move2.newState.fullmoveNumber)

    // Board conversion round-trip
    val roundTripFen = ChessBoardManager.toFen(
      board = move2.newState.board,
      activeColor = move2.newState.activeColor,
      castlingRights = move2.newState.castlingRights,
      enPassantTarget = move2.newState.enPassantTarget,
      halfmoveClock = move2.newState.halfmoveClock,
      fullmoveNumber = move2.newState.fullmoveNumber
    )
    assertEquals(move2.newState.fen, roundTripFen)
  }

  @Test
  fun `chess board state manager handles castling and check detection`() {
    // Setup position ready for White kingside castling:
    // White: King on e1 (7,4), Rook on h1 (7,7), empty f1 and g1
    val castlingFen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
    val state = ChessBoardManager.parseFen(castlingFen)
    assertTrue("White King on e1 should be eligible to castle kingside", state.castlingRights.contains("K"))
    assertTrue("O-O move from (7,4) to (7,6) must be legal", ChessBoardManager.isValidMove(state, 7, 4, 7, 6))

    val castleResult = ChessBoardManager.executeMove(state, 7, 4, 7, 6)
    assertNotNull(castleResult)
    assertEquals("O-O", castleResult!!.moveNotation)
    assertEquals("K", castleResult.newState.board[7][6])
    assertEquals("R", castleResult.newState.board[7][5])
    assertEquals("", castleResult.newState.board[7][4])
    assertEquals("", castleResult.newState.board[7][7])
    assertFalse(castleResult.newState.castlingRights.contains("K"))
  }

  @Test
  fun `room database chat messages schema defines local caching and persistence`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppRoomDatabase.createInMemory(context)
    val dao = db.chatMessageDao()

    val incomingMsg = LocalChatMessageEntity(
      id = "msg-in-1",
      conversationId = "u-lucas",
      sender = "Lucas Silva",
      text = "Hey! Let's play Chess tonight.",
      timestamp = "10:30 AM",
      timestampMillis = 1000L,
      status = "seen",
      isViewOnce = false,
      isAudio = true,
      audioDuration = "0:05",
      audioWaveformCsv = "0.2,0.5,0.8,0.3"
    )

    val outgoingMsg = LocalChatMessageEntity(
      id = "msg-out-1",
      conversationId = "u-lucas",
      sender = "Zavier",
      text = "Sure! I'm ready.",
      timestamp = "10:32 AM",
      timestampMillis = 2000L,
      status = "sent",
      isViewOnce = true,
      viewed = false
    )

    dao.insertMessage(incomingMsg)
    dao.insertMessage(outgoingMsg)

    val messages = dao.getMessagesForConversation("u-lucas")
    assertEquals(2, messages.size)
    assertEquals("msg-in-1", messages[0].id)
    assertEquals("Lucas Silva", messages[0].sender)
    assertEquals("0:05", messages[0].audioDuration)
    val domainMsg = messages[0].toChatMessage()
    assertEquals(4, domainMsg.audioWaveform.size)
    assertEquals(0.8f, domainMsg.audioWaveform[2], 0.01f)

    // Verify view-once mark viewed
    dao.markViewOnceBurned("msg-out-1")
    val updatedOut = dao.getMessageById("msg-out-1")
    assertNotNull(updatedOut)
    assertTrue(updatedOut!!.viewed)

    // Verify status update
    dao.updateMessageStatus("msg-out-1", "delivered")
    val deliveredOut = dao.getMessageById("msg-out-1")
    assertEquals("delivered", deliveredOut!!.status)

    db.close()
  }

  @Test
  fun `repository offline-first dm caching persists incoming and outgoing messages to room`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppRoomDatabase.createInMemory(context)
    val repository = EchoTalkRepository.getInstance()
    repository.setRoomDatabaseSuspend(db)

    val incomingMsg = ChatMessage(
      id = "dm-room-in-101",
      sender = "Chloe Dubois",
      text = "Testing offline-first caching via Room database schema",
      timestamp = "11:00 AM",
      status = "seen"
    )

    // Cache incoming message
    repository.cacheIncomingMessageSuspend("u-chloe", incomingMsg)

    // Assert message is cached in Room database
    val cachedMessages = db.chatMessageDao().getMessagesForConversation("u-chloe")
    val found = cachedMessages.find { it.id == "dm-room-in-101" }
    assertNotNull(found)
    assertEquals("Testing offline-first caching via Room database schema", found!!.text)

    // Send outgoing message and verify persistence in Room
    repository.sendDM(
      recipientId = "u-chloe",
      text = "Confirmed Room cache storage!"
    )
    kotlinx.coroutines.delay(200)

    val updatedThread = db.chatMessageDao().getMessagesForConversation("u-chloe")
    val outgoingFound = updatedThread.find { it.text == "Confirmed Room cache storage!" }
    assertNotNull(outgoingFound)
    assertEquals("u-chloe", outgoingFound!!.conversationId)

    db.close()
  }

  @Test
  fun `polite sound synthesizer generates Messenger pop chime buffer with expected sampling parameters`() {
    val popBuffer = PoliteSoundSynthesizer.generatePopTone()
    assertNotNull(popBuffer)
    assertTrue(popBuffer.isNotEmpty())
    // 75ms at 44100Hz = 3307 samples
    assertTrue(popBuffer.size in 3000..3600)

    val sendBuffer = PoliteSoundSynthesizer.generateSendTone()
    assertNotNull(sendBuffer)
    assertTrue(sendBuffer.isNotEmpty())

    val receiveBuffer = PoliteSoundSynthesizer.generateReceiveTone()
    assertNotNull(receiveBuffer)
    assertTrue(receiveBuffer.isNotEmpty())
  }

  @Test
  fun `arcade hub firestore state sync data model retains chess and ludo multi-game states`() {
    val chessState = com.example.data.model.ArcadeGameState(
      gameId = "chess_match_lucas",
      gameType = "chess",
      challenger = "You",
      opponent = "Lucas Silva",
      currentTurn = "White",
      chessBoard = List(64) { "P" },
      capturedWhite = listOf("p", "n"),
      capturedBlack = listOf("P"),
      lastMoveDescription = "e4 to e5",
      winner = null,
      updatedAt = System.currentTimeMillis()
    )

    assertEquals("chess_match_lucas", chessState.gameId)
    assertEquals("chess", chessState.gameType)
    assertEquals(64, chessState.chessBoard.size)
    assertEquals(2, chessState.capturedWhite.size)

    val ludoState = com.example.data.model.ArcadeGameState(
      gameId = "ludo_match_lucas",
      gameType = "ludo",
      challenger = "You",
      opponent = "Lucas Silva",
      currentTurn = "Red",
      ludoDiceValue = 6,
      ludoRedTokens = listOf(0, -1),
      ludoBlueTokens = listOf(-1, -1),
      lastMoveDescription = "Red launched token from base",
      winner = null,
      updatedAt = System.currentTimeMillis()
    )

    assertEquals("ludo_match_lucas", ludoState.gameId)
    assertEquals(6, ludoState.ludoDiceValue)
    assertEquals(0, ludoState.ludoRedTokens[0])
  }
}



