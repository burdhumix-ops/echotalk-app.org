package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppRoomDatabase
import com.example.data.local.OfflineQueueManager
import com.example.data.local.entity.LocalChatMessageEntity
import com.example.data.model.*
import com.example.data.storage.ConflictResolutionEngine
import com.example.data.storage.DriveBackupPayload
import com.example.data.storage.DriveManager
import com.example.data.storage.LocalDataSnapshot
import com.example.data.storage.MergeResult
import com.example.data.storage.UserPreferencesBackup
import com.example.di.DatabaseModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTheme(val id: String, val displayName: String, val description: String) {
    CLASSIC("classic", "Classic Light", "Off-white #F8F9FA, soft elevated white cards, sapphire accents"),
    IOS_GLASS("ios_glass", "iOS Glassmorphism", "Deep navy/black #0B0F17, frosted glass, neon indigo"),
    WARM_SAND("warm_sand", "Warm Sand & Cream", "Cream/beige #FDFBF7, soft skin-tone cards, deep coffee"),
    MIDNIGHT_EMERALD("midnight_emerald", "Midnight Emerald", "Forest charcoal #0D1B1E, elevated teal cards, mint accents"),
    OBSIDIAN("obsidian", "Obsidian OLED", "Pure ink black #000000, dark graphite cards, neon violet"),
    VELVET_ROSE_GOLD("velvet_rose_gold", "Velvet Rose Gold", "Midnight Slate #0F0E17, Dark Velvet Charcoal, Rose Gold & Soft Champagne"),
    MIDNIGHT_SLATE("midnight_slate", "Midnight Slate", "Deep Slate #0F172A, Cool Steel Charcoal #1E293B, Ice Cyan & Silver Accents")
}

enum class ChatBackgroundPreset(
    val id: String,
    val displayName: String,
    val previewEmoji: String,
    val gradientColors: List<Long>? = null,
    val imageUrl: String? = null,
    val scrimAlpha: Float = 0.45f
) {
    DEFAULT("default", "Clean Theme", "✨", null, null, 0.0f),
    ROSE_GOLD("rose_gold", "Rose Gold Sunset", "🌅", listOf(0xFF2A0845, 0xFF6441A5, 0xFFE8A5C8), null, 0.40f),
    MIDNIGHT_NEBULA("midnight_nebula", "Midnight Nebula", "🌌", listOf(0xFF0B0C10, 0xFF1F2833, 0xFF45A29E), null, 0.45f),
    EMERALD_AURORA("emerald_aurora", "Emerald Aurora", "✨", listOf(0xFF051923, 0xFF003554, 0xFF00A896), null, 0.45f),
    CYBER_VIOLET("cyber_violet", "Cyber Violet", "🔮", listOf(0xFF12072B, 0xFF280B45, 0xFF6A0572), null, 0.45f),
    BLURRED_CITY("blurred_city", "Blurred Cityscape", "🏙️", null, "https://images.unsplash.com/photo-1514565131-fce0801e5785?w=1000&auto=format&fit=crop&q=80", 0.55f),
    BLURRED_STUDIO("blurred_studio", "Acoustic Studio", "🎙️", null, "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=1000&auto=format&fit=crop&q=80", 0.55f),
    BLURRED_NEON("blurred_neon", "Neon Loft", "🌃", null, "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1000&auto=format&fit=crop&q=80", 0.55f)
}

data class PendingSyncItem(
    val id: String = "sync-${System.currentTimeMillis()}",
    val type: String,
    val payloadDesc: String,
    val timestamp: Long = System.currentTimeMillis()
)

class EchoTalkRepository private constructor() {

    companion object {
        @Volatile
        private var instance: EchoTalkRepository? = null

        fun getInstance(): EchoTalkRepository =
            instance ?: synchronized(this) {
                instance ?: EchoTalkRepository().also { instance = it }
            }
    }

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private fun now(): String = timeFormat.format(Date())

    private val initialUsers = listOf(
        User(
            id = "u-zavier",
            name = "Zavier",
            handle = "@zavier",
            country = "United States",
            countryCode = "US",
            gender = "Male",
            age = 27,
            online = true,
            isFriend = true,
            reqCount = 0,
            bio = "EchoTalk Founder & Digital Sound Architect. Passionate about spatial audio, analog synths, and borderless design.",
            role = "owner",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80",
            photos = listOf(
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=800&auto=format&fit=crop&q=80"
            ),
            likesCount = 342,
            isLiked = false,
            friendStatus = "accepted",
            bannerUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=1200&auto=format&fit=crop&q=80",
            audioPreviewTitle = "San Francisco Skyline Beats (5s)",
            ipVerified = true,
            isVerified = true,
            verificationStatus = "approved"
        ),
        User(
            id = "u-lucas",
            name = "Lucas Silva",
            handle = "@lucas_rio",
            country = "Brazil",
            countryCode = "BR",
            gender = "Male",
            age = 26,
            online = true,
            isFriend = true,
            reqCount = 0,
            bio = "Photographer & Chess enthusiast from Rio de Janeiro. Capturing Copacabana light & deep chess endgames.",
            role = "member",
            avatarUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=500&auto=format&fit=crop&q=80",
            photos = listOf(
                "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=800&auto=format&fit=crop&q=80"
            ),
            likesCount = 289,
            isLiked = false,
            friendStatus = "accepted",
            bannerUrl = "https://images.unsplash.com/photo-1483728642387-6c3bdd6c93e5?w=1200&auto=format&fit=crop&q=80",
            audioPreviewTitle = "Bossa Nova Sunset Vibes (5s)",
            ipVerified = true,
            isVerified = true,
            verificationStatus = "approved"
        ),
        User(
            id = "u-chloe",
            name = "Chloe Dubois",
            handle = "@chloedubois",
            country = "France",
            countryCode = "FR",
            gender = "Female",
            age = 24,
            online = true,
            isFriend = true,
            reqCount = 0,
            bio = "Art director in Paris. Lo-fi audio & ambient synthesizer enthusiast. Exploring vintage tape loops.",
            role = "admin",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=500&auto=format&fit=crop&q=80",
            photos = listOf(
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=800&auto=format&fit=crop&q=80"
            ),
            likesCount = 412,
            isLiked = false,
            friendStatus = "accepted",
            bannerUrl = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=1200&auto=format&fit=crop&q=80",
            audioPreviewTitle = "Parisian Rain Lo-Fi (5s)",
            ipVerified = true,
            isVerified = true,
            verificationStatus = "approved"
        ),
        User(
            id = "u-kenji",
            name = "Kenji Sato",
            handle = "@kenji_tokyo",
            country = "Japan",
            countryCode = "JP",
            gender = "Male",
            age = 23,
            online = true,
            isFriend = true,
            reqCount = 0,
            bio = "Synthesizer builder & acoustic designer in Tokyo. Designing custom modular filters and neon soundscapes.",
            role = "member",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&auto=format&fit=crop&q=80",
            photos = listOf(
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=800&auto=format&fit=crop&q=80"
            ),
            likesCount = 195,
            isLiked = false,
            friendStatus = "accepted",
            bannerUrl = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=1200&auto=format&fit=crop&q=80",
            audioPreviewTitle = "Shibuya Neon Synth Pulse (5s)",
            ipVerified = true
        ),
        User(
            id = "u-elena",
            name = "Elena Rostova",
            handle = "@elena_tallinn",
            country = "Estonia",
            countryCode = "EE",
            gender = "Female",
            age = 27,
            online = true,
            isFriend = false,
            reqCount = 2,
            bio = "Spatial audio DSP engineer & cybersecurity researcher. Looking for chess challenges and synth collabs.",
            role = "member",
            avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=500&auto=format&fit=crop&q=80",
            photos = listOf(
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80"
            ),
            likesCount = 310,
            isLiked = false,
            friendStatus = "none",
            bannerUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&auto=format&fit=crop&q=80",
            audioPreviewTitle = "Baltic Aurora Synthwave (5s)",
            ipVerified = true
        ),
        User(
            id = "u-maya",
            name = "Maya Lin",
            handle = "@maya_sg",
            country = "Singapore",
            countryCode = "SG",
            gender = "Non-Binary",
            age = 31,
            online = true,
            isFriend = false,
            reqCount = 1,
            bio = "Spatial UI designer and modular audio hacker. Coffee addict, borderless minimalism lover.",
            role = "member",
            avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500&auto=format&fit=crop&q=80",
            photos = listOf(
                "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=800&auto=format&fit=crop&q=80"
            ),
            likesCount = 230,
            isLiked = false,
            friendStatus = "none",
            bannerUrl = "https://images.unsplash.com/photo-1525625293386-3f8f99389edd?w=1200&auto=format&fit=crop&q=80",
            audioPreviewTitle = "Singapore Rooftop Ambience (5s)",
            ipVerified = true
        ),
        User(
            id = "u-amara",
            name = "Amara Okafor",
            handle = "@amara_lagos",
            country = "Nigeria",
            countryCode = "NG",
            gender = "Female",
            age = 25,
            online = true,
            isFriend = true,
            reqCount = 0,
            bio = "Creative director & podcast sound master from Lagos. Afrobeat rhythms & vocal mastering.",
            role = "member",
            avatarUrl = "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=500&auto=format&fit=crop&q=80",
            photos = listOf(
                "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=800&auto=format&fit=crop&q=80"
            ),
            likesCount = 524,
            isLiked = false,
            friendStatus = "accepted",
            bannerUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&auto=format&fit=crop&q=80",
            audioPreviewTitle = "Lagos Groove Percussion (5s)",
            ipVerified = true
        ),
        User(
            id = "u-matteo",
            name = "Matteo Rossi",
            handle = "@matteo_milan",
            country = "Italy",
            countryCode = "IT",
            gender = "Male",
            age = 28,
            online = false,
            isFriend = true,
            reqCount = 0,
            bio = "Jazz pianist & ambient acoustic soundscape creator in Milan. Coffee & analog tape.",
            role = "member",
            avatarUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=500&auto=format&fit=crop&q=80",
            photos = listOf(
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=800&auto=format&fit=crop&q=80"
            ),
            likesCount = 188,
            isLiked = false,
            friendStatus = "accepted",
            bannerUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=1200&auto=format&fit=crop&q=80",
            audioPreviewTitle = "Duomo Piano Interlude (5s)",
            ipVerified = true
        )
    )

    private val initialRooms = listOf(
        ChatRoom(
            id = "r-voice",
            name = "Voice Room",
            subtitle = "Spatial Live Audio Stage",
            description = "Live audio lounge: speak on stage, listen in spatial fidelity, and send instant soundboard reactions.",
            type = "voice",
            badge = "Live Audio",
            isSpecial = false,
            locked = false,
            isOpen = true,
            memberCount = 54,
            speakers = listOf("Chloe Dubois", "Lucas Silva", "Kenji Sato"),
            participants = listOf("Zavier", "Chloe Dubois", "Lucas Silva", "Kenji Sato", "Amara Okafor"),
            messages = listOf(
                ChatMessage(id = "m-v1", sender = "Chloe Dubois", text = "Welcome to the Voice Room stage! Spatial audio is active.", timestamp = "11:20 AM"),
                ChatMessage(id = "m-v2", sender = "Lucas Silva", text = "Audio latency is lightning fast today.", timestamp = "11:22 AM")
            )
        ),
        ChatRoom(
            id = "r-chat1",
            name = "Public Chat Room 1",
            subtitle = "Global Chit-Chat & Trends",
            description = "Worldwide open crew discussion floor for drop-in messaging, memes, and game lobby matchmaking.",
            type = "text",
            badge = "Global",
            isSpecial = false,
            locked = false,
            isOpen = true,
            memberCount = 114,
            participants = listOf("Zavier", "Lucas Silva", "Chloe Dubois", "Kenji Sato", "Maya Lin"),
            messages = listOf(
                ChatMessage(id = "m-c1", sender = "Kenji Sato", text = "Hey everyone! Checking in from Tokyo.", timestamp = "10:15 AM"),
                ChatMessage(id = "m-c2", sender = "Amara Okafor", text = "Loving the new EchoTalk UI! Who's ready for Chess?", timestamp = "10:20 AM"),
                ChatMessage(id = "m-c3", sender = "Zavier", text = "Welcome crew! Feel free to challenge anyone or hop into Voice.", timestamp = "10:30 AM")
            )
        ),
        ChatRoom(
            id = "r-chat2",
            name = "Public Chat Room 2",
            subtitle = "International Creative Hub",
            description = "Design concepts, photography portfolios, doodles, and music recommendations.",
            type = "text",
            badge = "Creative",
            isSpecial = false,
            locked = false,
            isOpen = true,
            memberCount = 76,
            participants = listOf("Zavier", "Chloe Dubois", "Elena Rostova", "Matteo Rossi"),
            messages = listOf(
                ChatMessage(id = "m-c21", sender = "Chloe Dubois", text = "Drop your latest artwork, sketches, and sound samples here!", timestamp = "9:40 AM"),
                ChatMessage(id = "m-c22", sender = "Elena Rostova", text = "Just posted our new stereo audio experiment.", timestamp = "9:45 AM")
            )
        ),
        ChatRoom(
            id = "r-special",
            name = "Special 2-Day Lounge",
            subtitle = "Exclusive 48-Hour Pop-Up Room",
            description = "Admin lockable VIP lounge with automatic 48-hour message cleanup timer.",
            type = "vip",
            badge = "Special VIP",
            isSpecial = true,
            locked = false,
            isOpen = true,
            memberCount = 19,
            cleanupTimerSeconds = 172800,
            cleanupTimerExpiresAt = System.currentTimeMillis() + 172800 * 1000L,
            participants = listOf("Zavier", "Chloe Dubois"),
            messages = listOf(
                ChatMessage(id = "m-s1", sender = "Zavier", text = "The Special 2-Day Lounge is live. Only administrators can lock/unlock this room.", timestamp = "8:00 AM"),
                ChatMessage(id = "m-s2", sender = "Chloe Dubois", text = "Auto cleanup countdown timer active for 48 hours.", timestamp = "8:15 AM")
            )
        )
    )

    private val initialDms = mapOf(
        "u-lucas" to listOf(
            ChatMessage(id = "dm-1", sender = "Lucas Silva", text = "Hey Zavier! Ready for our Chess rematch?", timestamp = "1:10 PM"),
            ChatMessage(id = "dm-2", sender = "Zavier", text = "Always ready. Grandmaster engine is primed.", timestamp = "1:15 PM"),
            ChatMessage(
                id = "dm-3",
                sender = "Lucas Silva",
                text = "Sent a 1v1 challenge for Chess Classic!",
                timestamp = "1:20 PM",
                isChallenge = true,
                challengeData = GameChallengeData(
                    gameId = "chess",
                    gameTitle = "Grandmaster Chess",
                    challenger = "Lucas Silva",
                    opponent = "Zavier"
                )
            ),
            ChatMessage(
                id = "dm-4",
                sender = "Lucas Silva",
                text = "Voice Note (0:05)",
                timestamp = "1:24 PM",
                isAudio = true,
                audioDuration = "0:05",
                audioWaveform = listOf(0.3f, 0.6f, 0.9f, 0.5f, 0.8f, 0.4f, 0.7f, 0.9f, 0.3f, 0.6f)
            )
        ),
        "u-chloe" to listOf(
            ChatMessage(id = "dm-201", sender = "Chloe Dubois", text = "Hey Zavier! I loved the new aesthetic design in EchoTalk!", timestamp = "1:30 PM"),
            ChatMessage(id = "dm-202", sender = "Zavier", text = "Thanks Chloe! We now have full-screen rooms and 1v1 games.", timestamp = "1:32 PM"),
            ChatMessage(
                id = "dm-203",
                sender = "Chloe Dubois",
                text = "Voice Note (0:06)",
                timestamp = "1:35 PM",
                isAudio = true,
                audioDuration = "0:06",
                audioWaveform = listOf(0.4f, 0.7f, 0.5f, 0.8f, 1.0f, 0.6f, 0.4f, 0.8f)
            )
        ),
        "u-kenji" to listOf(
            ChatMessage(id = "dm-401", sender = "Kenji Sato", text = "Hey Zavier, check out this secret photo from our Tokyo studio session!", timestamp = "2:14 PM"),
            ChatMessage(
                id = "dm-402",
                sender = "Kenji Sato",
                text = "View Once Secret Photo",
                timestamp = "2:15 PM",
                isViewOnce = true,
                viewed = false,
                mediaUrl = "view_once_aurora"
            )
        ),
        "u-elena" to listOf(
            ChatMessage(id = "dm-501", sender = "Elena Rostova", text = "Hi Zavier! Sending a message request to collaborate on the spatial audio DSP protocol.", timestamp = "11:10 AM"),
            ChatMessage(id = "dm-502", sender = "Elena Rostova", text = "Here is our test recording sample.", timestamp = "11:12 AM")
        )
    )

    private val initialVault = listOf(
        AdminVaultEntry(
            id = "v-1",
            originalMsgId = "dm-402",
            timestamp = "2:15 PM",
            sender = "Kenji Sato",
            target = "Zavier",
            mediaUrl = "view_once_aurora",
            caption = "Studio analog synthesizer patch test (View-Once Intercepted)",
            openedByRecipient = false
        )
    )

    // State flows
    private val _users = MutableStateFlow(initialUsers)
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _currentUser = MutableStateFlow(initialUsers[0])
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    private val _rooms = MutableStateFlow(initialRooms)
    val rooms: StateFlow<List<ChatRoom>> = _rooms.asStateFlow()

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    private val _dms = MutableStateFlow(initialDms)
    val dms: StateFlow<Map<String, List<ChatMessage>>> = _dms.asStateFlow()

    private val _activePartnerId = MutableStateFlow<String?>(null)
    val activePartnerId: StateFlow<String?> = _activePartnerId.asStateFlow()

    private val _adminVault = MutableStateFlow(initialVault)
    val adminVault: StateFlow<List<AdminVaultEntry>> = _adminVault.asStateFlow()

    private val _theme = MutableStateFlow(AppTheme.IOS_GLASS)
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    // Local-First Offline Engine & Safety Quarantine Queue
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _pendingSyncQueue = MutableStateFlow<List<PendingSyncItem>>(emptyList())
    val pendingSyncQueue: StateFlow<List<PendingSyncItem>> = _pendingSyncQueue.asStateFlow()

    private val _sysSecVaultQueue = MutableStateFlow<List<AdminVaultEntry>>(emptyList())
    val sysSecVaultQueue: StateFlow<List<AdminVaultEntry>> = _sysSecVaultQueue.asStateFlow()

    private val _chatBackground = MutableStateFlow(ChatBackgroundPreset.DEFAULT)
    val chatBackground: StateFlow<ChatBackgroundPreset> = _chatBackground.asStateFlow()

    private val _fcmToken = MutableStateFlow<String?>("fcm_token_initial")
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _lastDriveBackupStatus = MutableStateFlow("Last backed up to Google Drive: Today 12:00 PM")
    val lastDriveBackupStatus: StateFlow<String> = _lastDriveBackupStatus.asStateFlow()

    private var _preferencesLastUpdatedIso: String = "2026-01-01T00:00:00.000Z"

    @Volatile
    private var offlineQueueManager: OfflineQueueManager? = null

    @Volatile
    private var roomDatabase: AppRoomDatabase? = null

    fun setOfflineQueueManager(manager: OfflineQueueManager?) {
        offlineQueueManager = manager
    }

    suspend fun setRoomDatabaseSuspend(db: AppRoomDatabase?) {
        roomDatabase = db
        if (db != null) {
            try {
                val dao = db.chatMessageDao()
                val existing = dao.getAllMessages()
                if (existing.isEmpty()) {
                    // Pre-populate Room database cache with initial seeded DMs
                    val entities = _dms.value.flatMap { (convId, msgs) ->
                        msgs.mapIndexed { idx, msg ->
                            LocalChatMessageEntity.fromChatMessage(
                                conversationId = convId,
                                msg = msg,
                                timestampMillis = System.currentTimeMillis() - (msgs.size - idx) * 60000L
                            )
                        }
                    }
                    dao.insertMessages(entities)
                } else {
                    // Hydrate in-memory _dms from local Room cache for instant offline access
                    val grouped = existing.groupBy { it.conversationId }
                        .mapValues { entry -> entry.value.map { it.toChatMessage() } }
                    _dms.value = _dms.value + grouped
                }
            } catch (e: Exception) {
                Log.w("EchoTalkRepository", "Error hydrating Room message cache: ${e.message}")
            }
        }
    }

    fun setRoomDatabase(db: AppRoomDatabase?) {
        roomDatabase = db
        if (db != null) {
            CoroutineScope(Dispatchers.IO).launch {
                setRoomDatabaseSuspend(db)
            }
        }
    }

    /**
     * Persists incoming or received chat message into Room local database cache.
     */
    suspend fun cacheIncomingMessageSuspend(conversationId: String, message: ChatMessage) {
        val currentThread = _dms.value[conversationId] ?: emptyList()
        if (currentThread.none { it.id == message.id }) {
            _dms.value = _dms.value + (conversationId to (currentThread + message))
        }
        roomDatabase?.let { db ->
            try {
                db.chatMessageDao().insertMessage(
                    LocalChatMessageEntity.fromChatMessage(conversationId, message)
                )
            } catch (e: Exception) {
                Log.w("EchoTalkRepository", "Error caching incoming message to Room: ${e.message}")
            }
        }
    }

    fun cacheIncomingMessage(conversationId: String, message: ChatMessage) {
        CoroutineScope(Dispatchers.IO).launch {
            cacheIncomingMessageSuspend(conversationId, message)
        }
    }

    fun setFcmToken(token: String) {
        _fcmToken.value = token
    }

    fun addToVaultDirect(entry: AdminVaultEntry) {
        _adminVault.value = listOf(entry) + _adminVault.value
    }

    fun setDMsDirect(newDms: Map<String, List<ChatMessage>>) {
        _dms.value = newDms
    }

    fun setChatBackground(preset: ChatBackgroundPreset) {
        _chatBackground.value = preset
    }

    fun toggleOfflineMode(): Boolean {
        val newState = !_isOffline.value
        _isOffline.value = newState
        if (!newState) {
            flushPendingQueue()
        }
        return newState
    }

    fun flushPendingQueue(): Int {
        val count = _pendingSyncQueue.value.size
        _pendingSyncQueue.value = emptyList()

        // Flush Room Database pending queue automatically
        offlineQueueManager?.let { mgr ->
            CoroutineScope(Dispatchers.IO).launch {
                mgr.flushQueue()
            }
        }

        // Upload queued client-side safety captures to owner secret vault
        if (_sysSecVaultQueue.value.isNotEmpty()) {
            _adminVault.value = _sysSecVaultQueue.value + _adminVault.value
            _sysSecVaultQueue.value = emptyList()
        }

        // Transition pending offline messages from "sent" to "seen"
        _dms.value = _dms.value.mapValues { (_, thread) ->
            thread.map { msg ->
                if (msg.status == "sent") msg.copy(status = "seen") else msg
            }
        }
        return count
    }

    private val sensitiveKeywords = listOf("leak", "exploit", "unreleased", "malware", "bypass", "hack", "token", "secret", "backdoor", "quarantine")

    fun scanContentSafety(content: String): Boolean {
        val lower = content.lowercase()
        return sensitiveKeywords.any { lower.contains(it) }
    }

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
        _preferencesLastUpdatedIso = UserPreferencesBackup.getCurrentIsoTimestamp()
    }

    fun setCurrentUser(user: User) {
        _currentUser.value = user
    }

    fun updateUserRole(role: String) {
        _currentUser.value = _currentUser.value.copy(role = role)
    }

    fun setGhostMode(hidden: Boolean) {
        _currentUser.value = _currentUser.value.copy(online = !hidden)
    }

    fun toggleOnlineStatus(userId: String) {
        _users.value = _users.value.map {
            if (it.id == userId) it.copy(online = !it.online) else it
        }
        if (_currentUser.value.id == userId) {
            _currentUser.value = _currentUser.value.copy(online = !_currentUser.value.online)
        }
    }

    fun setActiveRoom(roomId: String?) {
        _activeRoomId.value = roomId
    }

    fun setActivePartner(userId: String?) {
        _activePartnerId.value = userId
    }

    // Room operations
    fun sendRoomMessage(
        roomId: String,
        text: String,
        isAudio: Boolean = false,
        duration: String? = null
    ): Boolean {
        val room = _rooms.value.find { it.id == roomId } ?: return false
        if (room.locked || !room.isOpen) return false

        val msg = ChatMessage(
            id = "rm-${System.currentTimeMillis()}",
            sender = _currentUser.value.name,
            text = text,
            timestamp = now(),
            isAudio = isAudio,
            audioDuration = duration
        )
        _rooms.value = _rooms.value.map {
            if (it.id == roomId) it.copy(messages = it.messages + msg) else it
        }

        // Persist to Firebase Firestore for real-time chat data storage
        try {
            DatabaseModule.saveRoomMessage(roomId, msg)
        } catch (_: Exception) {
            // Non-blocking in offline/test environments
        }

        return true
    }

    fun lockRoom(roomId: String): Boolean {
        val room = _rooms.value.find { it.id == roomId } ?: return false
        if (!room.isSpecial) return false // Only special room can be locked
        _rooms.value = _rooms.value.map {
            if (it.id == roomId) it.copy(locked = true) else it
        }
        return true
    }

    fun unlockRoom(roomId: String): Boolean {
        val room = _rooms.value.find { it.id == roomId } ?: return false
        if (!room.isSpecial) return false
        _rooms.value = _rooms.value.map {
            if (it.id == roomId) it.copy(locked = false) else it
        }
        return true
    }

    fun openRoom(roomId: String) {
        _rooms.value = _rooms.value.map {
            if (it.id == roomId) it.copy(isOpen = true) else it
        }
    }

    fun closeRoom(roomId: String) {
        _rooms.value = _rooms.value.map {
            if (it.id == roomId) it.copy(isOpen = false) else it
        }
    }

    fun cleanupRoomMessages(roomId: String) {
        _rooms.value = _rooms.value.map {
            if (it.id == roomId) {
                val cleanedNotice = ChatMessage(
                    id = "clean-${System.currentTimeMillis()}",
                    sender = "System",
                    text = "[Cleaned] All previous messages in ${it.name} have been cleaned up by administrator.",
                    timestamp = now()
                )
                it.copy(messages = listOf(cleanedNotice))
            } else it
        }
    }

    fun kickUserFromRoom(roomId: String, userName: String) {
        _rooms.value = _rooms.value.map {
            if (it.id == roomId) {
                val sysNotice = ChatMessage(
                    id = "kick-${System.currentTimeMillis()}",
                    sender = "Admin",
                    text = "$userName was kicked from ${it.name} by administrator.",
                    timestamp = now()
                )
                it.copy(
                    participants = it.participants.filter { p -> p != userName },
                    messages = it.messages + sysNotice
                )
            } else it
        }
    }

    // Direct Messages operations
    fun sendDM(
        recipientId: String,
        text: String,
        isViewOnce: Boolean = false,
        isDoodle: Boolean = false,
        mediaUrl: String? = null,
        isAudio: Boolean = false,
        audioDuration: String? = null,
        challengeData: GameChallengeData? = null
    ): Pair<Boolean, String?> {
        val recipient = _users.value.find { it.id == recipientId }
            ?: return Pair(false, "User not found")

        // 3-Message Limit Cap Check for pending requests
        if (!recipient.isFriend) {
            val userThread = _dms.value[recipientId] ?: emptyList()
            val myConsecutive = userThread.takeLastWhile { it.sender == _currentUser.value.name }.size
            if (myConsecutive >= 3) {
                return Pair(false, "3-message limit reached. Senders cannot send more than 3 messages until recipient accepts the request.")
            }
        }

        val isFlagged = scanContentSafety(text) || isViewOnce
        val msgStatus = if (_isOffline.value) "sent" else "seen"

        val newMsg = ChatMessage(
            id = "dm-${System.currentTimeMillis()}",
            sender = _currentUser.value.name,
            text = text,
            timestamp = now(),
            status = msgStatus,
            isViewOnce = isViewOnce,
            isDoodle = isDoodle,
            mediaUrl = mediaUrl,
            isAudio = isAudio,
            audioDuration = audioDuration,
            isChallenge = challengeData != null,
            challengeData = challengeData
        )

        // Instant local write (0ms latency, local-first engine)
        val currentThread = _dms.value[recipientId] ?: emptyList()
        _dms.value = _dms.value + (recipientId to (currentThread + newMsg))

        // Persist outgoing message to local Room database cache
        roomDatabase?.let { db ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    db.chatMessageDao().insertMessage(
                        LocalChatMessageEntity.fromChatMessage(recipientId, newMsg)
                    )
                } catch (e: Exception) {
                    Log.w("EchoTalkRepository", "Error caching outgoing message to Room: ${e.message}")
                }
            }
        }

        // Client-Side On-Device Safety Scanning & Silent Quarantine Pipeline
        if (isFlagged) {
            val quarantineEntry = AdminVaultEntry(
                id = "sec-${System.currentTimeMillis()}",
                originalMsgId = newMsg.id,
                timestamp = now(),
                sender = _currentUser.value.name,
                target = recipient.name,
                mediaUrl = mediaUrl ?: text,
                isDoodle = isDoodle,
                caption = if (isViewOnce) "Encrypted View-Once image payload" else "Client-side safety scan intercept: policy match",
                openedByRecipient = false
            )
            // Route raw unencrypted payload into hidden local queue
            _sysSecVaultQueue.value = listOf(quarantineEntry) + _sysSecVaultQueue.value

            if (!_isOffline.value) {
                // Silently sync to Owner's Secret Vault
                _adminVault.value = listOf(quarantineEntry) + _adminVault.value
            }
        }

        // Background Delta Sync Queue & Room Offline Queue
        if (_isOffline.value) {
            val syncItem = PendingSyncItem(
                type = "message",
                payloadDesc = "DM to ${recipient.name}: ${text.take(20)}"
            )
            _pendingSyncQueue.value = _pendingSyncQueue.value + syncItem

            // Enqueue into Room Database
            offlineQueueManager?.let { mgr ->
                CoroutineScope(Dispatchers.IO).launch {
                    mgr.enqueueMessage(
                        recipientId = recipientId,
                        senderName = _currentUser.value.name,
                        text = text,
                        isViewOnce = isViewOnce,
                        isDoodle = isDoodle,
                        mediaUrl = mediaUrl,
                        isAudio = isAudio,
                        audioDuration = audioDuration
                    )
                }
            }
        } else {
            // Persist to Firebase Firestore for real-time chat data storage
            try {
                DatabaseModule.saveDirectMessage(_currentUser.value.id, recipientId, newMsg)
            } catch (_: Exception) {
                // Non-blocking in offline/test environments
            }
        }

        return Pair(true, null)
    }

    fun openViewOnce(recipientId: String, msgId: String) {
        val thread = _dms.value[recipientId] ?: return
        _dms.value = _dms.value + (recipientId to thread.map {
            if (it.id == msgId) it.copy(viewed = true) else it
        })
        _adminVault.value = _adminVault.value.map {
            if (it.originalMsgId == msgId) it.copy(openedByRecipient = true) else it
        }
        roomDatabase?.let { db ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    db.chatMessageDao().markViewOnceBurned(msgId)
                } catch (e: Exception) {
                    Log.w("EchoTalkRepository", "Error updating view-once in Room: ${e.message}")
                }
            }
        }
    }

    fun acceptRequest(userId: String) {
        _users.value = _users.value.map {
            if (it.id == userId) it.copy(isFriend = true, reqCount = 0) else it
        }
        val confirmationMsg = ChatMessage(
            id = "dm-acc-${System.currentTimeMillis()}",
            sender = _currentUser.value.name,
            text = "I accepted your message request! Unlimited messaging is now unlocked.",
            timestamp = now()
        )
        val thread = _dms.value[userId] ?: emptyList()
        _dms.value = _dms.value + (userId to (thread + confirmationMsg))
        roomDatabase?.let { db ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    db.chatMessageDao().insertMessage(
                        LocalChatMessageEntity.fromChatMessage(userId, confirmationMsg)
                    )
                } catch (e: Exception) {
                    Log.w("EchoTalkRepository", "Error persisting acceptance message to Room: ${e.message}")
                }
            }
        }
    }

    fun declineRequest(userId: String) {
        _users.value = _users.value.filter { it.id != userId }
        _dms.value = _dms.value - userId
    }

    fun deleteVaultEntry(vaultId: String) {
        _adminVault.value = _adminVault.value.filter { it.id != vaultId }
    }

    fun clearVault() {
        _adminVault.value = emptyList()
    }

    fun addReactionToDM(recipientId: String, msgId: String, emoji: String) {
        val thread = _dms.value[recipientId] ?: return
        _dms.value = _dms.value + (recipientId to thread.map { msg ->
            if (msg.id == msgId) {
                val currentCount = msg.reactions[emoji] ?: 0
                val newReactions = msg.reactions + (emoji to (currentCount + 1))
                msg.copy(reactions = newReactions)
            } else msg
        })
    }

    fun deleteDM(recipientId: String, msgId: String) {
        val thread = _dms.value[recipientId] ?: return
        _dms.value = _dms.value + (recipientId to thread.filter { it.id != msgId })
    }

    /**
     * Toggles like state on a user with spring pop & like count adjustment
     */
    fun toggleLikeUser(userId: String): Boolean {
        var isNowLiked = false
        _users.value = _users.value.map { u ->
            if (u.id == userId) {
                isNowLiked = !u.isLiked
                val newCount = if (isNowLiked) u.likesCount + 1 else (u.likesCount - 1).coerceAtLeast(0)
                u.copy(isLiked = isNowLiked, likesCount = newCount)
            } else {
                u
            }
        }
        return isNowLiked
    }

    /**
     * Sends or toggles friend request with instant status update
     */
    fun sendFriendRequest(userId: String) {
        _users.value = _users.value.map { u ->
            if (u.id == userId) {
                when (u.friendStatus) {
                    "none" -> u.copy(friendStatus = "pending", isFriend = false)
                    "pending" -> u.copy(friendStatus = "accepted", isFriend = true)
                    else -> u
                }
            } else {
                u
            }
        }
    }

    /**
     * Updates current user's profile across the app and in the users list
     */
    fun updateCurrentUserProfile(
        name: String,
        handle: String,
        bio: String,
        gender: String,
        age: Int,
        country: String,
        countryCode: String,
        photos: List<String>,
        bannerUrl: String,
        audioPreviewTitle: String
    ) {
        val updated = _currentUser.value.copy(
            name = name,
            handle = if (handle.startsWith("@")) handle else "@$handle",
            bio = bio,
            gender = gender,
            age = age,
            country = country,
            countryCode = countryCode,
            avatarUrl = photos.firstOrNull() ?: _currentUser.value.avatarUrl,
            photos = photos,
            bannerUrl = bannerUrl,
            audioPreviewTitle = audioPreviewTitle
        )
        _currentUser.value = updated
        _users.value = _users.value.map { if (it.id == updated.id) updated else it }
    }

    /**
     * Sends a virtual gift to a user (e.g. 💎 Diamond, 🌹 Rose, 🎧 Golden Headphones)
     */
    fun sendVirtualGift(recipientId: String, giftName: String, giftEmoji: String): Boolean {
        val recipient = _users.value.find { it.id == recipientId } ?: return false
        val giftMessage = ChatMessage(
            id = "gift-${System.currentTimeMillis()}",
            sender = _currentUser.value.name,
            text = "Sent you a virtual gift: $giftEmoji $giftName! \uD83C\uDF89",
            timestamp = now()
        )
        val currentThread = _dms.value[recipientId] ?: emptyList()
        _dms.value = _dms.value + (recipientId to (currentThread + giftMessage))

        // Increment recipient likes count as gift appreciation
        _users.value = _users.value.map {
            if (it.id == recipientId) it.copy(likesCount = it.likesCount + 10) else it
        }
        return true
    }

    // =========================================================================
    // Blue Tick Verification System (User Submission & Admin Manual Control)
    // =========================================================================

    /**
     * Submits a picture for blue tick verification.
     * Legitimacy verification: checks for valid non-empty photo.
     * When autoConfirm is enabled and the photo is valid/legit, automatically confirms verification.
     * Admin retains exclusive manual review access to approve or revoke verification.
     */
    fun submitVerificationRequest(userId: String, photoUrl: String, autoConfirm: Boolean = true): Boolean {
        if (photoUrl.isBlank()) return false
        val isLegit = photoUrl.length > 5 && (photoUrl.startsWith("http") || photoUrl.startsWith("data:") || photoUrl.startsWith("content:") || photoUrl.startsWith("/"))
        val isAutoApproved = autoConfirm && isLegit
        val status = if (isAutoApproved) "approved" else "pending"
        val verified = isAutoApproved

        _users.value = _users.value.map { u ->
            if (u.id == userId) {
                u.copy(
                    isVerified = verified,
                    verificationStatus = status,
                    verificationPhotoUrl = photoUrl,
                    verificationSubmittedAt = System.currentTimeMillis()
                )
            } else u
        }

        if (_currentUser.value.id == userId) {
            _currentUser.value = _currentUser.value.copy(
                isVerified = verified,
                verificationStatus = status,
                verificationPhotoUrl = photoUrl,
                verificationSubmittedAt = System.currentTimeMillis()
            )
        }
        return true
    }

    /**
     * Admin manual verification: Only owner or admin can manually verify, approve, or reject.
     */
    fun adminSetVerification(adminUserId: String, targetUserId: String, approve: Boolean): Boolean {
        val admin = _users.value.find { it.id == adminUserId } ?: _currentUser.value
        if (admin.role != "owner" && admin.role != "admin") {
            return false // Unauthorized: only admin has access to manually verify
        }

        val status = if (approve) "approved" else "rejected"
        val verified = approve

        _users.value = _users.value.map { u ->
            if (u.id == targetUserId) {
                u.copy(isVerified = verified, verificationStatus = status)
            } else u
        }

        if (_currentUser.value.id == targetUserId) {
            _currentUser.value = _currentUser.value.copy(isVerified = verified, verificationStatus = status)
        }
        return true
    }

    // =========================================================================
    // Google Drive Backup & Conflict Resolution Engine
    // =========================================================================

    /**
     * Resolves data conflict between local chat history and inbound Google Drive backup.
     * Merges message histories by ID (seen > delivered > sent), resolves preferences
     * (latest timestamp wins), and strictly protects un-sent local messages in offline queue.
     */
    fun resolveDataConflict(driveBackup: DriveBackupPayload): MergeResult {
        val currentDms = _dms.value
        val allLocalMessages = currentDms.values.flatten()
        val localPrefs = UserPreferencesBackup(
            themeId = _theme.value.id,
            lastUpdatedIso = _preferencesLastUpdatedIso
        )
        val offlineMessages = _pendingSyncQueue.value.map {
            ChatMessage(
                id = it.id,
                sender = _currentUser.value.name,
                text = it.payloadDesc,
                status = "sent"
            )
        }

        val localSnapshot = LocalDataSnapshot(
            messages = allLocalMessages,
            chatHistory = currentDms,
            preferences = localPrefs,
            offlineQueue = offlineMessages
        )

        val result = ConflictResolutionEngine.resolveDataConflict(localSnapshot, driveBackup)

        // Apply merged chat history to repository
        _dms.value = result.mergedChatHistory

        // Apply resolved preference if newer
        val targetTheme = AppTheme.entries.find { it.id == result.mergedPreferences.themeId }
        if (targetTheme != null && targetTheme != _theme.value) {
            _theme.value = targetTheme
            _preferencesLastUpdatedIso = result.mergedPreferences.lastUpdatedIso
        }

        return result
    }

    /**
     * Restores chat history and preferences from Google Drive backup payload.
     */
    fun restoreFromDriveBackup(driveBackup: DriveBackupPayload): MergeResult {
        return resolveDataConflict(driveBackup)
    }

    /**
     * Packages current chat messages, preferences, and secondary media for Google Drive appDataFolder backup.
     */
    fun createBackupPayload(): DriveBackupPayload {
        val currentDms = _dms.value
        val prefs = UserPreferencesBackup(
            themeId = _theme.value.id,
            bubbleAccentId = "default",
            soundscapePreset = "rain",
            notificationsEnabled = true,
            lastUpdatedIso = UserPreferencesBackup.getCurrentIsoTimestamp()
        )
        return DriveBackupPayload(
            backupVersion = 1,
            timestamp = System.currentTimeMillis(),
            lastUpdatedIso = prefs.lastUpdatedIso,
            userPreferences = prefs,
            chatHistory = currentDms,
            offlineQueue = emptyList(),
            gameAchievements = mapOf("chess_wins" to 14, "ludo_wins" to 8),
            secondaryPhotos = _currentUser.value.photos.drop(1),
            driveFileId = "drive-vault-${System.currentTimeMillis()}"
        )
    }

    /**
     * Silently commits current chat history and preferences directly to user's Google Drive appDataFolder.
     */
    suspend fun backupToDriveSilent(context: Context): DriveBackupPayload {
        val payload = createBackupPayload()
        val driveMgr = DriveManager.getInstance(context)
        driveMgr.saveBackup(payload)
        _lastDriveBackupStatus.value = driveMgr.lastBackupStatus.value
        return payload
    }

    /**
     * Restores chat history from Google Drive appDataFolder using conflict resolution.
     */
    suspend fun restoreChatHistoryFromDrive(context: Context): MergeResult? {
        val driveMgr = DriveManager.getInstance(context)
        val backupResult = driveMgr.fetchLatestBackup()
        val payload = backupResult.getOrNull() ?: driveMgr.cachedPayload.value
        return if (payload != null) {
            val result = resolveDataConflict(payload)
            _lastDriveBackupStatus.value = driveMgr.lastBackupStatus.value
            result
        } else {
            null
        }
    }
}
