package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room Entity representing a pending task or chat message queued while offline.
 */
@Entity(tableName = "pending_queue")
data class PendingQueueEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val taskType: String = "MESSAGE", // MESSAGE, REACTION, PROFILE_UPDATE, GAME_CHALLENGE
    val recipientId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isViewOnce: Boolean = false,
    val isDoodle: Boolean = false,
    val mediaUrl: String? = null,
    val isAudio: Boolean = false,
    val audioDuration: String? = null,
    val payloadJson: String = "",
    val retryCount: Int = 0,
    val status: String = "PENDING" // PENDING, PROCESSING, FAILED
)
