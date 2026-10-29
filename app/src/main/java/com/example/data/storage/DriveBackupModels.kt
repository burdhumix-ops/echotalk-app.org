package com.example.data.storage

import com.example.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class UserPreferencesBackup(
    val themeId: String = "velvet_rose_gold",
    val bubbleAccentId: String = "default",
    val soundscapePreset: String = "rain",
    val notificationsEnabled: Boolean = true,
    val lastUpdatedIso: String = getCurrentIsoTimestamp()
) {
    companion object {
        fun getCurrentIsoTimestamp(): String {
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            df.timeZone = TimeZone.getTimeZone("UTC")
            return df.format(Date())
        }
    }
}

data class DriveBackupPayload(
    val backupVersion: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdatedIso: String = UserPreferencesBackup.getCurrentIsoTimestamp(),
    val userPreferences: UserPreferencesBackup = UserPreferencesBackup(),
    val chatHistory: Map<String, List<ChatMessage>> = emptyMap(),
    val offlineQueue: List<ChatMessage> = emptyList(),
    val gameAchievements: Map<String, Int> = emptyMap(),
    val secondaryPhotos: List<String> = emptyList(),
    val driveFileId: String = "appDataFolder_backup_${System.currentTimeMillis()}"
)

data class LocalDataSnapshot(
    val messages: List<ChatMessage>,
    val chatHistory: Map<String, List<ChatMessage>>,
    val preferences: UserPreferencesBackup,
    val offlineQueue: List<ChatMessage>
)

data class MergeResult(
    val mergedMessages: List<ChatMessage>,
    val mergedChatHistory: Map<String, List<ChatMessage>>,
    val mergedPreferences: UserPreferencesBackup,
    val preservedOfflineQueue: List<ChatMessage>,
    val addedCount: Int,
    val updatedCount: Int
)
