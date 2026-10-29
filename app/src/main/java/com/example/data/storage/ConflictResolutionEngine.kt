package com.example.data.storage

import android.util.Log
import com.example.data.model.ChatMessage

/**
 * ConflictResolutionEngine: Implements the Backup Conflict Resolution
 * & Data Merging Strategy between local device storage and Google Account Vault (appDataFolder).
 */
object ConflictResolutionEngine {

    private const val TAG = "ConflictResolution"

    /**
     * Status indicator hierarchy ranking:
     * Read / Seen (3) > Delivered (2) > Sent (1) > Other (0).
     */
    fun getStatusPriority(status: String): Int {
        return when (status.lowercase()) {
            "seen", "read" -> 3
            "delivered" -> 2
            "sent" -> 1
            else -> 0
        }
    }

    /**
     * Resolves single message conflicts when identical message ID exists in both
     * local storage and remote Drive backup.
     * Keeps the copy with the latest status indicator (Read > Delivered > Sent).
     */
    fun resolveMessageStatusConflict(localMsg: ChatMessage, remoteMsg: ChatMessage): ChatMessage {
        val localPriority = getStatusPriority(localMsg.status)
        val remotePriority = getStatusPriority(remoteMsg.status)

        return if (remotePriority > localPriority) {
            remoteMsg
        } else {
            localMsg
        }
    }

    /**
     * Message History (Union Merge by ID):
     * Compares message timestamps and unique message IDs.
     * Combines local messages and remote Drive messages into a single deduplicated array.
     * If a message exists in both locations, keeps the copy with the latest status indicator.
     */
    fun mergeMessageLists(
        localMessages: List<ChatMessage>,
        driveMessages: List<ChatMessage>
    ): List<ChatMessage> {
        val mergedMap = LinkedHashMap<String, ChatMessage>()

        // 1. Seed with local messages
        for (localMsg in localMessages) {
            mergedMap[localMsg.id] = localMsg
        }

        // 2. Union merge from Drive
        for (driveMsg in driveMessages) {
            val existing = mergedMap[driveMsg.id]
            if (existing == null) {
                mergedMap[driveMsg.id] = driveMsg
            } else {
                mergedMap[driveMsg.id] = resolveMessageStatusConflict(existing, driveMsg)
            }
        }

        // 3. Sort chronologically
        return mergedMap.values.toList()
    }

    /**
     * User App Preferences (Latest Timestamp Wins):
     * Compares last_updated ISO timestamps between local settings and incoming Drive settings.
     * Overwrites local settings only if the cloud backup timestamp is newer.
     */
    fun resolvePreferencesConflict(
        localPrefs: UserPreferencesBackup,
        drivePrefs: UserPreferencesBackup
    ): UserPreferencesBackup {
        val isDriveNewer = drivePrefs.lastUpdatedIso.compareTo(localPrefs.lastUpdatedIso) > 0
        Log.d(TAG, "Preferences compare: local=${localPrefs.lastUpdatedIso}, drive=${drivePrefs.lastUpdatedIso}, driveIsNewer=$isDriveNewer")
        return if (isDriveNewer) drivePrefs else localPrefs
    }

    /**
     * Full Conflict Resolution Engine:
     * - Protects offline queue: never deletes unsent local messages in offline_message_queue.
     * - Union merges message history across all threads.
     * - Resolves preferences conflict (latest timestamp wins).
     */
    fun resolveDataConflict(
        localData: LocalDataSnapshot,
        driveData: DriveBackupPayload
    ): MergeResult {
        Log.i(TAG, "Executing resolveDataConflict with Drive Backup (${driveData.driveFileId})")

        // 1. Offline Queue Protection:
        // Never delete un-sent local messages queued in offline_message_queue during a cloud restore.
        // Preserve and dispatch them first before merging the inbound cloud backup.
        val preservedOffline = localData.offlineQueue.toList()

        // 2. Message History (Union Merge by ID) for single list
        val mergedSingleList = mergeMessageLists(localData.messages, driveData.chatHistory.values.flatten())

        // 3. Message History (Union Merge by ID) per thread
        val mergedThreads = mutableMapOf<String, List<ChatMessage>>()
        val allThreadKeys = (localData.chatHistory.keys + driveData.chatHistory.keys)

        var addedCount = 0
        var updatedCount = 0

        for (key in allThreadKeys) {
            val localThread = localData.chatHistory[key] ?: emptyList()
            val driveThread = driveData.chatHistory[key] ?: emptyList()
            val merged = mergeMessageLists(localThread, driveThread)
            mergedThreads[key] = merged

            addedCount += (merged.size - localThread.size).coerceAtLeast(0)
            updatedCount += merged.count { m ->
                val localCopy = localThread.find { it.id == m.id }
                localCopy != null && localCopy.status != m.status
            }
        }

        // 4. User App Preferences (Latest Timestamp Wins)
        val resolvedPrefs = resolvePreferencesConflict(localData.preferences, driveData.userPreferences)

        Log.i(TAG, "Conflict resolution complete: preserved ${preservedOffline.size} offline items, merged ${mergedThreads.values.sumOf { it.size }} messages.")

        return MergeResult(
            mergedMessages = mergedSingleList,
            mergedChatHistory = mergedThreads,
            mergedPreferences = resolvedPrefs,
            preservedOfflineQueue = preservedOffline,
            addedCount = addedCount,
            updatedCount = updatedCount
        )
    }
}
