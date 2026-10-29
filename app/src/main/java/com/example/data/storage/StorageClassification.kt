package com.example.data.storage

/**
 * Storage Target classification distinguishing Public/Shared Server DB
 * from User's Private Google Account Vault (appDataFolder).
 */
enum class StorageTarget(val targetName: String, val description: String) {
    SERVER_DB(
        targetName = "Server Database (Public / Social / Real-time)",
        description = "Central app server for public profiles, social graph, live moderation logs, and active lounge rooms."
    ),
    GOOGLE_DRIVE_APP_DATA(
        targetName = "Google Account Vault (appDataFolder / Private Drive Sync)",
        description = "User's hidden Google Drive Application Data folder for complete private DM history, settings, drafts, and offline queues."
    )
}

/**
 * Technical Implementation Directive:
 * Defines the strict mapping between data categories and their authorized storage target.
 */
object StorageClassificationMatrix {

    val STORAGE_ROUTING: Map<StorageTarget, List<String>> = mapOf(
        StorageTarget.SERVER_DB to listOf(
            "profiles",
            "friendships",
            "friend_requests",
            "admin_quarantine_vault",
            "active_game_sessions",
            "webrtc_signaling"
        ),
        StorageTarget.GOOGLE_DRIVE_APP_DATA to listOf(
            "chat_history_backup",
            "profile_photos_carousel_2_to_5",
            "user_settings_and_theme",
            "offline_message_queue",
            "game_achievements"
        )
    )

    fun getAuthorizedTarget(category: String): StorageTarget {
        return if (STORAGE_ROUTING[StorageTarget.SERVER_DB]?.contains(category) == true) {
            StorageTarget.SERVER_DB
        } else {
            StorageTarget.GOOGLE_DRIVE_APP_DATA
        }
    }

    /**
     * Data Isolation Rule:
     * Personal chat backups stored in appDataFolder MUST NOT be readable or routed
     * through public server tables unless explicitly flagged for safety violations by the owner.
     */
    fun validateZeroCrossSync(category: String, destination: StorageTarget): Boolean {
        val authorized = getAuthorizedTarget(category)
        return authorized == destination
    }
}
