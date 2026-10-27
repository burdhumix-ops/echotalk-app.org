package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ArcadeGameState
import com.example.data.model.GameChallengeData
import com.example.data.repository.EchoTalkRepository
import com.example.di.DatabaseModule
import com.example.ui.screens.ChessEngine
import com.example.ui.screens.LudoEngine
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
}
