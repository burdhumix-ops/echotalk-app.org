package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ArcadeGameState(
    val gameId: String = "",
    val gameType: String = "", // "chess", "ludo", "mafia"
    val challenger: String = "",
    val opponent: String = "",
    val currentTurn: String = "White", // For chess: White/Black. For Ludo: Red/Blue
    val chessBoard: List<String> = emptyList(), // 64 strings representing squares 0..63
    val capturedWhite: List<String> = emptyList(),
    val capturedBlack: List<String> = emptyList(),
    val ludoDiceValue: Int = 1,
    val ludoRedTokens: List<Int> = listOf(-1, -1),
    val ludoBlueTokens: List<Int> = listOf(-1, -1),
    val lastMoveDescription: String = "",
    val winner: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
