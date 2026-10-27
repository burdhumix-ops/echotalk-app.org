package com.example.ui.screens

import com.example.data.model.ArcadeGameState
import com.example.di.DatabaseModule
import kotlin.math.abs
import kotlin.math.max

object ChessEngine {

    val initialBoard: Array<Array<String>>
        get() = arrayOf(
            arrayOf("r", "n", "b", "q", "k", "b", "n", "r"),
            arrayOf("p", "p", "p", "p", "p", "p", "p", "p"),
            arrayOf("", "", "", "", "", "", "", ""),
            arrayOf("", "", "", "", "", "", "", ""),
            arrayOf("", "", "", "", "", "", "", ""),
            arrayOf("", "", "", "", "", "", "", ""),
            arrayOf("P", "P", "P", "P", "P", "P", "P", "P"),
            arrayOf("R", "N", "B", "Q", "K", "B", "N", "R")
        )

    val pieceSymbols = mapOf(
        "K" to "♔", "Q" to "♕", "R" to "♖", "B" to "♗", "N" to "♘", "P" to "♙",
        "k" to "♚", "q" to "♛", "r" to "♜", "b" to "♝", "n" to "♞", "p" to "♟"
    )

    fun boardToList(board: Array<Array<String>>): List<String> {
        val list = mutableListOf<String>()
        for (r in 0..7) {
            for (c in 0..7) {
                list.add(board[r][c])
            }
        }
        return list
    }

    fun listToBoard(list: List<String>): Array<Array<String>> {
        val board = Array(8) { Array(8) { "" } }
        if (list.size != 64) return board
        for (r in 0..7) {
            for (c in 0..7) {
                board[r][c] = list[r * 8 + c]
            }
        }
        return board
    }

    fun isPathClear(board: Array<Array<String>>, fromR: Int, fromC: Int, toR: Int, toC: Int): Boolean {
        val dr = (toR - fromR).coerceIn(-1, 1)
        val dc = (toC - fromC).coerceIn(-1, 1)

        var curR = fromR + dr
        var curC = fromC + dc
        while (curR != toR || curC != toC) {
            if (board[curR][curC].isNotEmpty()) return false
            curR += dr
            curC += dc
        }
        return true
    }

    fun isValidMove(
        board: Array<Array<String>>,
        fromR: Int,
        fromC: Int,
        toR: Int,
        toC: Int,
        currentTurn: String
    ): Boolean {
        if (fromR !in 0..7 || fromC !in 0..7 || toR !in 0..7 || toC !in 0..7) return false
        if (fromR == toR && fromC == toC) return false

        val piece = board[fromR][fromC]
        if (piece.isEmpty()) return false

        val isWhite = piece.first().isUpperCase()
        if (currentTurn == "White" && !isWhite) return false
        if (currentTurn == "Black" && isWhite) return false

        val dest = board[toR][toC]
        if (dest.isNotEmpty()) {
            val destIsWhite = dest.first().isUpperCase()
            if (isWhite == destIsWhite) return false // Cannot capture friendly piece
        }

        val dr = toR - fromR
        val dc = toC - fromC
        val absDr = abs(dr)
        val absDc = abs(dc)

        return when (piece.uppercase()) {
            "P" -> {
                if (isWhite) {
                    // White Pawn moves up (dr < 0)
                    if (dc == 0 && dr == -1 && dest.isEmpty()) true
                    else if (dc == 0 && dr == -2 && fromR == 6 && dest.isEmpty() && board[5][fromC].isEmpty()) true
                    else if (absDc == 1 && dr == -1 && dest.isNotEmpty() && !dest.first().isUpperCase()) true
                    else false
                } else {
                    // Black Pawn moves down (dr > 0)
                    if (dc == 0 && dr == 1 && dest.isEmpty()) true
                    else if (dc == 0 && dr == 2 && fromR == 1 && dest.isEmpty() && board[2][fromC].isEmpty()) true
                    else if (absDc == 1 && dr == 1 && dest.isNotEmpty() && dest.first().isUpperCase()) true
                    else false
                }
            }
            "N" -> (absDr == 2 && absDc == 1) || (absDr == 1 && absDc == 2)
            "B" -> absDr == absDc && isPathClear(board, fromR, fromC, toR, toC)
            "R" -> (absDr == 0 || absDc == 0) && isPathClear(board, fromR, fromC, toR, toC)
            "Q" -> ((absDr == absDc) || (absDr == 0 || absDc == 0)) && isPathClear(board, fromR, fromC, toR, toC)
            "K" -> {
                if (max(absDr, absDc) == 1) true
                else if (isWhite && fromR == 7 && fromC == 4) {
                    // Castling White
                    if (toR == 7 && toC == 6 && board[7][5].isEmpty() && board[7][6].isEmpty() && board[7][7] == "R") true
                    else if (toR == 7 && toC == 2 && board[7][1].isEmpty() && board[7][2].isEmpty() && board[7][3].isEmpty() && board[7][0] == "R") true
                    else false
                } else if (!isWhite && fromR == 0 && fromC == 4) {
                    // Castling Black
                    if (toR == 0 && toC == 6 && board[0][5].isEmpty() && board[0][6].isEmpty() && board[0][7] == "r") true
                    else if (toR == 0 && toC == 2 && board[0][1].isEmpty() && board[0][2].isEmpty() && board[0][3].isEmpty() && board[0][0] == "r") true
                    else false
                } else false
            }
            else -> false
        }
    }

    /**
     * Executes move including Castling and Pawn Promotion
     */
    fun executeMove(
        board: Array<Array<String>>,
        fromR: Int,
        fromC: Int,
        toR: Int,
        toC: Int
    ): Pair<String?, String> {
        val piece = board[fromR][fromC]
        val dest = board[toR][toC]
        var captured: String? = if (dest.isNotEmpty()) dest else null

        // Handle Castling Rook repositioning
        if (piece == "K" && fromR == 7 && fromC == 4) {
            if (toR == 7 && toC == 6) {
                board[7][5] = "R"
                board[7][7] = ""
            } else if (toR == 7 && toC == 2) {
                board[7][3] = "R"
                board[7][0] = ""
            }
        } else if (piece == "k" && fromR == 0 && fromC == 4) {
            if (toR == 0 && toC == 6) {
                board[0][5] = "r"
                board[0][7] = ""
            } else if (toR == 0 && toC == 2) {
                board[0][3] = "r"
                board[0][0] = ""
            }
        }

        // Move piece
        board[toR][toC] = piece
        board[fromR][fromC] = ""

        // Pawn Promotion: White pawn to 8th rank promotes to Q, Black pawn to 1st rank promotes to q
        if (piece == "P" && toR == 0) {
            board[toR][toC] = "Q"
        } else if (piece == "p" && toR == 7) {
            board[toR][toC] = "q"
        }

        val cols = listOf("a", "b", "c", "d", "e", "f", "g", "h")
        val moveDesc = "${pieceSymbols.getOrDefault(piece, piece)} ${cols[fromC]}${8 - fromR} ➔ ${cols[toC]}${8 - toR}"
        return Pair(captured, moveDesc)
    }

    fun findKing(board: Array<Array<String>>, isWhite: Boolean): Pair<Int, Int>? {
        val target = if (isWhite) "K" else "k"
        for (r in 0..7) {
            for (c in 0..7) {
                if (board[r][c] == target) return Pair(r, c)
            }
        }
        return null
    }

    fun isCheck(board: Array<Array<String>>, isWhiteTurn: Boolean): Boolean {
        val kingPos = findKing(board, isWhiteTurn) ?: return false
        val oppTurn = if (isWhiteTurn) "Black" else "White"

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece.isNotEmpty() && (piece.first().isUpperCase() != isWhiteTurn)) {
                    if (isValidMove(board, r, c, kingPos.first, kingPos.second, oppTurn)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}

object LudoEngine {
    val SAFE_ZONES = setOf(0, 8, 15)
    const val TRACK_LENGTH = 20

    data class MoveResult(
        val updatedTokens: List<Int>,
        val capturedOpponentTokenIndex: Int?,
        val isFinished: Boolean,
        val description: String
    )

    fun moveToken(
        tokens: List<Int>,
        tokenIndex: Int,
        diceValue: Int,
        opponentTokens: List<Int>
    ): MoveResult {
        val mutable = tokens.toMutableList()
        val currentPos = mutable[tokenIndex]
        var capturedIndex: Int? = null

        val newPos: Int
        val desc: String

        if (currentPos == -1) {
            // Launch from base yard on rolling a 6
            if (diceValue == 6) {
                newPos = 0
                desc = "Launched Token ${tokenIndex + 1} onto track!"
            } else {
                newPos = -1
                desc = "Need a 6 to launch Token ${tokenIndex + 1}."
            }
        } else {
            newPos = (currentPos + diceValue).coerceAtMost(TRACK_LENGTH)
            desc = "Token ${tokenIndex + 1} moved +$diceValue to square $newPos"
        }

        mutable[tokenIndex] = newPos

        // Check capture against opponent tokens
        if (newPos in 0 until TRACK_LENGTH && !SAFE_ZONES.contains(newPos)) {
            val oppHit = opponentTokens.indexOfFirst { it == newPos }
            if (oppHit != -1) {
                capturedIndex = oppHit
            }
        }

        val allHome = mutable.all { it >= TRACK_LENGTH }
        return MoveResult(
            updatedTokens = mutable,
            capturedOpponentTokenIndex = capturedIndex,
            isFinished = allHome,
            description = desc
        )
    }
}
