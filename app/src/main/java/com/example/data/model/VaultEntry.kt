package com.example.data.model

data class AdminVaultEntry(
    val id: String,
    val originalMsgId: String = "",
    val timestamp: String,
    val sender: String,
    val target: String,
    val mediaUrl: String,
    val isDoodle: Boolean = false,
    val caption: String? = null,
    val openedByRecipient: Boolean = false
)
