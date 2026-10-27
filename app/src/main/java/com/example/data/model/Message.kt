package com.example.data.model
 
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class GameChallengeData(
    val gameId: String = "", // chess, ludo, mafia
    val gameTitle: String = "",
    val challenger: String = "",
    val opponent: String = "",
    val status: String = "pending" // pending, accepted, declined
)

@IgnoreExtraProperties
data class ChatMessage(
    val id: String = "",
    val sender: String = "",
    val text: String = "",
    val timestamp: String = "",
    val status: String = "seen", // sent, delivered, seen
    val isViewOnce: Boolean = false,
    val viewed: Boolean = false,
    val isDoodle: Boolean = false,
    val mediaUrl: String? = null,
    val isAudio: Boolean = false,
    val audioDuration: String? = null,
    val audioWaveform: List<Float> = emptyList(),
    val isChallenge: Boolean = false,
    val challengeData: GameChallengeData? = null,
    val reactions: Map<String, Int> = emptyMap()
)
