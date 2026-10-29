package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.ChatMessage
import com.example.data.model.GameChallengeData

/**
 * LocalChatMessageEntity: Room database entity defining the schema for local
 * chat message caching, ensuring incoming messages are persisted locally to
 * support an offline-first DM experience.
 */
@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestampMillis"])
    ]
)
data class LocalChatMessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String, // Recipient ID or conversation thread key
    val sender: String,
    val text: String,
    val timestamp: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val status: String = "seen", // sent, delivered, seen
    val isViewOnce: Boolean = false,
    val viewed: Boolean = false,
    val isDoodle: Boolean = false,
    val mediaUrl: String? = null,
    val isAudio: Boolean = false,
    val audioDuration: String? = null,
    val audioWaveformCsv: String = "",
    val isChallenge: Boolean = false,
    val challengeGameId: String? = null,
    val challengeGameTitle: String? = null,
    val challengeChallenger: String? = null,
    val challengeOpponent: String? = null,
    val challengeStatus: String? = null,
    val reactionsRaw: String = "" // format "emoji:count;emoji:count"
) {
    /**
     * Converts Room database entity into domain ChatMessage model.
     */
    fun toChatMessage(): ChatMessage {
        val waveformList = if (audioWaveformCsv.isBlank()) {
            emptyList()
        } else {
            audioWaveformCsv.split(",").mapNotNull { it.trim().toFloatOrNull() }
        }

        val challenge = if (isChallenge && !challengeGameId.isNullOrBlank()) {
            GameChallengeData(
                gameId = challengeGameId,
                gameTitle = challengeGameTitle ?: "",
                challenger = challengeChallenger ?: "",
                opponent = challengeOpponent ?: "",
                status = challengeStatus ?: "pending"
            )
        } else null

        val reactionMap = mutableMapOf<String, Int>()
        if (reactionsRaw.isNotBlank()) {
            reactionsRaw.split(";").forEach { item ->
                val parts = item.split(":")
                if (parts.size == 2) {
                    val count = parts[1].toIntOrNull() ?: 1
                    reactionMap[parts[0]] = count
                }
            }
        }

        return ChatMessage(
            id = id,
            sender = sender,
            text = text,
            timestamp = timestamp,
            status = status,
            isViewOnce = isViewOnce,
            viewed = viewed,
            isDoodle = isDoodle,
            mediaUrl = mediaUrl,
            isAudio = isAudio,
            audioDuration = audioDuration,
            audioWaveform = waveformList,
            isChallenge = isChallenge,
            challengeData = challenge,
            reactions = reactionMap
        )
    }

    companion object {
        /**
         * Converts domain ChatMessage into Room database entity.
         */
        fun fromChatMessage(
            conversationId: String,
            msg: ChatMessage,
            timestampMillis: Long = System.currentTimeMillis()
        ): LocalChatMessageEntity {
            val waveformCsv = msg.audioWaveform.joinToString(",")
            val reactionsStr = msg.reactions.entries.joinToString(";") { "${it.key}:${it.value}" }

            return LocalChatMessageEntity(
                id = msg.id,
                conversationId = conversationId,
                sender = msg.sender,
                text = msg.text,
                timestamp = msg.timestamp,
                timestampMillis = timestampMillis,
                status = msg.status,
                isViewOnce = msg.isViewOnce,
                viewed = msg.viewed,
                isDoodle = msg.isDoodle,
                mediaUrl = msg.mediaUrl,
                isAudio = msg.isAudio,
                audioDuration = msg.audioDuration,
                audioWaveformCsv = waveformCsv,
                isChallenge = msg.isChallenge,
                challengeGameId = msg.challengeData?.gameId,
                challengeGameTitle = msg.challengeData?.gameTitle,
                challengeChallenger = msg.challengeData?.challenger,
                challengeOpponent = msg.challengeData?.opponent,
                challengeStatus = msg.challengeData?.status,
                reactionsRaw = reactionsStr
            )
        }
    }
}
