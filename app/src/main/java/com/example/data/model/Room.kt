package com.example.data.model

data class ChatRoom(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val type: String, // voice, text, vip
    val badge: String,
    val isSpecial: Boolean = false,
    val locked: Boolean = false,
    val isOpen: Boolean = true,
    val memberCount: Int = 12,
    val speakers: List<String> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val participants: List<String> = emptyList(),
    val cleanupTimerSeconds: Int? = null,
    val cleanupTimerExpiresAt: Long? = null
)
