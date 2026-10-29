package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ai.GeminiChatService
import com.example.ai.configureAppCheck
import com.example.data.model.User
import com.example.data.repository.EchoTalkRepository
import com.example.di.DatabaseModule
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.EchoTalkTheme

class MainActivity : ComponentActivity() {

    private val repository = EchoTalkRepository.getInstance()
    private val aiService = GeminiChatService()
    private val soundscapePlayer = SoundscapePlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch Intent before UI or Firebase operations
        configureAppCheck(this, intent)

        // STEP 2: Initialize Firestore client via DatabaseModule for real-time chat data storage
        DatabaseModule.provideFirestore(this)

        // STEP 3: Initialize Room database for local chat message caching and offline queue
        val roomDb = com.example.data.local.AppRoomDatabase.getInstance(this)
        repository.setRoomDatabase(roomDb)
        val queueMgr = com.example.data.local.OfflineQueueManager.getInstance(this)
        repository.setOfflineQueueManager(queueMgr)
        com.example.fcm.FcmNotificationManager.initialize(this)

        enableEdgeToEdge()

        setContent {
            val activeTheme by repository.theme.collectAsState()
            val currentUser by repository.currentUser.collectAsState()
            val activeRoomId by repository.activeRoomId.collectAsState()
            val activePartnerId by repository.activePartnerId.collectAsState()
            val users by repository.users.collectAsState()
            val vaultEntries by repository.adminVault.collectAsState()

            var currentTab by remember { mutableStateOf(NavTab.DISCOVER) }
            var showSettingsDialog by remember { mutableStateOf(false) }
            var showSoundscapeDialog by remember { mutableStateOf(false) }
            var showVaultDialog by remember { mutableStateOf(false) }
            var showPasscodeDialog by remember { mutableStateOf(false) }
            var showEditProfileDialog by remember { mutableStateOf(false) }
            var selectedProfileUser by remember { mutableStateOf<User?>(null) }

            // Challenge launcher state from DMs or Discover to Arcade
            var challengeGameId by remember { mutableStateOf<String?>(null) }
            var challengeOpponent by remember { mutableStateOf<String?>(null) }

            // Hide bars when in 100% full-screen room or DM chat
            val isImmersive = activeRoomId != null || activePartnerId != null

            // DM Badge: count pending message requests
            val pendingCount = users.count { !it.isFriend }

            EchoTalkTheme(theme = activeTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (!isImmersive) {
                            TopBar(
                                currentUser = currentUser,
                                onOpenSettings = { showSettingsDialog = true },
                                onOpenSoundscapes = { showSoundscapeDialog = true },
                                onOpenVault = { showVaultDialog = true },
                                onTriggerBackdoor = { showPasscodeDialog = true },
                                modifier = Modifier.statusBarsPadding()
                            )
                        }
                    },
                    bottomBar = {
                        if (!isImmersive) {
                            BottomNavBar(
                                activeTab = currentTab,
                                onTabSelected = { tab ->
                                    currentTab = tab
                                    challengeGameId = null
                                },
                                dmBadgeCount = pendingCount
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = if (isImmersive) WindowInsets.statusBars.asPaddingValues().calculateTopPadding() else innerPadding.calculateTopPadding(),
                                bottom = if (isImmersive) WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() else innerPadding.calculateBottomPadding()
                            )
                    ) {
                        Crossfade(targetState = currentTab, label = "TabSwitch") { tab ->
                            when (tab) {
                                NavTab.DISCOVER -> {
                                    DiscoverScreen(
                                        repository = repository,
                                        onStartDM = { user ->
                                            repository.setActivePartner(user.id)
                                            currentTab = NavTab.DMS
                                        },
                                        onViewProfile = { user ->
                                            selectedProfileUser = user
                                        },
                                        onLaunchGameChallenge = { gameId, oppName ->
                                            challengeGameId = gameId
                                            challengeOpponent = oppName
                                            currentTab = NavTab.ARCADE
                                        }
                                    )
                                }
                                NavTab.ROOMS -> {
                                    RoomsScreen(
                                        repository = repository,
                                        onOpenDoodle = {}
                                    )
                                }
                                NavTab.DMS -> {
                                    DMsScreen(
                                        repository = repository,
                                        onLaunchGameChallenge = { gameId, oppName ->
                                            challengeGameId = gameId
                                            challengeOpponent = oppName
                                            currentTab = NavTab.ARCADE
                                        },
                                        onViewProfile = { user ->
                                            selectedProfileUser = user
                                        },
                                        aiService = aiService
                                    )
                                }
                                NavTab.AI -> {
                                    GeminiChatScreen(
                                        onNavigateToTab = { tab ->
                                            currentTab = tab
                                            challengeGameId = null
                                        },
                                        onStartDMWithDraft = { userId, draft ->
                                            repository.setActivePartner(userId)
                                            currentTab = NavTab.DMS
                                        },
                                        onOpenSoundscapes = { showSoundscapeDialog = true },
                                        onOpenVault = { showVaultDialog = true },
                                        onLaunchGame = { gameId ->
                                            challengeGameId = gameId
                                            challengeOpponent = "Friend"
                                            currentTab = NavTab.ARCADE
                                        }
                                    )
                                }
                                NavTab.ARCADE -> {
                                    GamesScreen(
                                        repository = repository,
                                        initialGameId = challengeGameId,
                                        opponentName = challengeOpponent
                                    )
                                }
                                NavTab.PROFILE -> {
                                    ProfileScreen(
                                        repository = repository,
                                        onOpenVault = { showVaultDialog = true },
                                        onOpenSoundscapes = { showSoundscapeDialog = true },
                                        onTriggerBackdoor = { showPasscodeDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }

                // Global Modals
                if (showPasscodeDialog) {
                    PasscodeDialog(
                        onDismiss = { showPasscodeDialog = false },
                        onRoleGranted = { role ->
                            repository.updateUserRole(role)
                        }
                    )
                }

                if (showVaultDialog) {
                    SecretVaultDialog(
                        vaultEntries = vaultEntries,
                        onDeleteEntry = { id -> repository.deleteVaultEntry(id) },
                        onClearAll = { repository.clearVault() },
                        onDismiss = { showVaultDialog = false }
                    )
                }

                if (showSoundscapeDialog) {
                    SoundscapeDialog(
                        player = soundscapePlayer,
                        onDismiss = { showSoundscapeDialog = false }
                    )
                }

                if (showSettingsDialog) {
                    SettingsDialog(
                        repository = repository,
                        onTriggerBackdoor = { showPasscodeDialog = true },
                        onOpenVault = { showVaultDialog = true },
                        onDismiss = { showSettingsDialog = false },
                        onOpenEditProfile = { showEditProfileDialog = true }
                    )
                }

                if (showEditProfileDialog) {
                    EditProfileDialog(
                        currentUser = currentUser,
                        repository = repository,
                        onDismiss = { showEditProfileDialog = false }
                    )
                }

                if (selectedProfileUser != null) {
                    FullBleedProfileDialog(
                        user = selectedProfileUser!!,
                        onStartDM = { user ->
                            repository.setActivePartner(user.id)
                            currentTab = NavTab.DMS
                        },
                        onDismiss = { selectedProfileUser = null },
                        onLaunchGameChallenge = { gameId, oppName ->
                            challengeGameId = gameId
                            challengeOpponent = oppName
                            currentTab = NavTab.ARCADE
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundscapePlayer.stop()
    }
}
