package com.example.ui.screens

import kotlin.math.abs
import kotlin.math.max

/**
 * ChessBoardState: Comprehensive state snapshot represented by and synchronized with a FEN string.
 */
data class ChessBoardState(
    val fen: String,
    val board: Array<Array<String>>,
    val activeColor: String, // "w" or "b"
    val castlingRights: String, // "KQkq", "-", etc.
    val enPassantTarget: String, // "-", "e3", etc.
    val halfmoveClock: Int,
    val fullmoveNumber: Int,
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChessBoardState
        return fen == other.fen
    }

    override fun hashCode(): Int {
        return fen.hashCode()
    }
}

/**
 * ChessMoveResult: Outcome of an executed move on the chess board.
 */
data class ChessMoveResult(
    val newState: ChessBoardState,
    val capturedPiece: String?,
    val moveNotation: String,
    val isCheck: Boolean,
    val isCheckmate: Boolean,
    val isDraw: Boolean
)

/**
 * ChessBoardManager: Manages Chess game state, turn-based logic, and move validation
 * using Forsyth-Edwards Notation (FEN) strings in the EchoTalk Arcade Hub.
 */
object ChessBoardManager {

    const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    val pieceSymbols = mapOf(
        "K" to "♔", "Q" to "♕", "R" to "♖", "B" to "♗", "N" to "♘", "P" to "♙",
        "k" to "♚", "q" to "♛", "r" to "♜", "b" to "♝", "n" to "♞", "p" to "♟"
    )

    private val cols = listOf("a", "b", "c", "d", "e", "f", "g", "h")

    /**
     * Parses a standard 6-field FEN string into a ChessBoardState.
     */
    fun parseFen(fen: String = STARTING_FEN): ChessBoardState {
        val parts = fen.trim().split(Regex("\\s+"))
        val placement = parts.getOrNull(0) ?: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"
        val activeColor = parts.getOrNull(1) ?: "w"
        val castling = parts.getOrNull(2) ?: "KQkq"
        val enPassant = parts.getOrNull(3) ?: "-"
        val halfmove = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val fullmove = parts.getOrNull(5)?.toIntOrNull() ?: 1

        val board = Array(8) { Array(8) { "" } }
        val ranks = placement.split("/")

        for (r in 0 until minOf(8, ranks.size)) {
            val rankStr = ranks[r]
            var c = 0
            for (char in rankStr) {
                if (c >= 8) break
                if (char.isDigit()) {
                    val emptySquares = char.digitToInt()
                    c += emptySquares
                } else {
                    board[r][c] = char.toString()
                    c++
                }
            }
        }

        val inCheck = isKingInCheck(board, activeColor == "w")
        val legalMovesExist = hasAnyLegalMoves(board, activeColor, castling, enPassant)

        return ChessBoardState(
            fen = fen,
            board = board,
            activeColor = activeColor,
            castlingRights = castling,
            enPassantTarget = enPassant,
            halfmoveClock = halfmove,
            fullmoveNumber = fullmove,
            isCheck = inCheck,
            isCheckmate = inCheck && !legalMovesExist,
            isStalemate = !inCheck && !legalMovesExist
        )
    }

    /**
     * Converts a 2D board array and game state variables into a standard FEN string.
     */
    fun toFen(
        board: Array<Array<String>>,
        activeColor: String,
        castlingRights: String = "KQkq",
        enPassantTarget: String = "-",
        halfmoveClock: Int = 0,
        fullmoveNumber: Int = 1
    ): String {
        val sb = StringBuilder()

        // 1. Piece placement
        for (r in 0..7) {
            var emptyCount = 0
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece.isEmpty()) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(piece)
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            if (r < 7) sb.append("/")
        }

        // 2. Active color
        sb.append(" ").append(activeColor)

        // 3. Castling availability
        sb.append(" ").append(if (castlingRights.isBlank()) "-" else castlingRights)

        // 4. En passant target square
        sb.append(" ").append(if (enPassantTarget.isBlank()) "-" else enPassantTarget)

        // 5. Halfmove clock
        sb.append(" ").append(halfmoveClock)

        // 6. Fullmove number
        sb.append(" ").append(fullmoveNumber)

        return sb.toString()
    }

    /**
     * Checks if a move is legal according to chess rules and current FEN state.
     */
    fun isValidMove(
        state: ChessBoardState,
        fromR: Int,
        fromC: Int,
        toR: Int,
        toC: Int
    ): Boolean {
        if (fromR !in 0..7 || fromC !in 0..7 || toR !in 0..7 || toC !in 0..7) return false
        if (fromR == toR && fromC == toC) return false

        val board = state.board
        val piece = board[fromR][fromC]
        if (piece.isEmpty()) return false

        val isWhite = piece.first().isUpperCase()
        if (state.activeColor == "w" && !isWhite) return false
        if (state.activeColor == "b" && isWhite) return false

        val dest = board[toR][toC]
        if (dest.isNotEmpty()) {
            val destIsWhite = dest.first().isUpperCase()
            if (isWhite == destIsWhite) return false // Cannot capture friendly piece
        }

        val dr = toR - fromR
        val dc = toC - fromC
        val absDr = abs(dr)
        val absDc = abs(dc)

        // Geometric move validation
        val isGeometricallyValid = when (piece.uppercase()) {
            "P" -> isValidPawnMove(board, fromR, fromC, toR, toC, isWhite, state.enPassantTarget)
            "N" -> (absDr == 2 && absDc == 1) || (absDr == 1 && absDc == 2)
            "B" -> absDr == absDc && isPathClear(board, fromR, fromC, toR, toC)
            "R" -> (absDr == 0 || absDc == 0) && isPathClear(board, fromR, fromC, toR, toC)
            "Q" -> ((absDr == absDc) || (absDr == 0 || absDc == 0)) && isPathClear(board, fromR, fromC, toR, toC)
            "K" -> isValidKingMove(board, fromR, fromC, toR, toC, isWhite, state.castlingRights)
            else -> false
        }

        if (!isGeometricallyValid) return false

        // Ensure king does not end up in check after this move
        val clonedBoard = cloneBoard(board)
        applyMoveToBoard(clonedBoard, fromR, fromC, toR, toC, isWhite, state.enPassantTarget)
        if (isKingInCheck(clonedBoard, isWhite)) {
            return false
        }

        return true
    }

    private fun isValidPawnMove(
        board: Array<Array<String>>,
        fromR: Int,
        fromC: Int,
        toR: Int,
        toC: Int,
        isWhite: Boolean,
        enPassantTarget: String
    ): Boolean {
        val dr = toR - fromR
        val dc = toC - fromC
        val absDc = abs(dc)
        val dest = board[toR][toC]

        return if (isWhite) {
            // Forward 1
            if (dc == 0 && dr == -1 && dest.isEmpty()) true
            // Forward 2 from initial rank 6
            else if (dc == 0 && dr == -2 && fromR == 6 && dest.isEmpty() && board[5][fromC].isEmpty()) true
            // Normal diagonal capture
            else if (absDc == 1 && dr == -1 && dest.isNotEmpty() && !dest.first().isUpperCase()) true
            // En passant diagonal capture
            else if (absDc == 1 && dr == -1 && dest.isEmpty() && enPassantTarget != "-") {
                val targetSquare = "${cols[toC]}${8 - toR}"
                targetSquare == enPassantTarget
            } else false
        } else {
            // Black forward 1
            if (dc == 0 && dr == 1 && dest.isEmpty()) true
            // Black forward 2 from initial rank 1
            else if (dc == 0 && dr == 2 && fromR == 1 && dest.isEmpty() && board[2][fromC].isEmpty()) true
            // Black normal diagonal capture
            else if (absDc == 1 && dr == 1 && dest.isNotEmpty() && dest.first().isUpperCase()) true
            // Black en passant diagonal capture
            else if (absDc == 1 && dr == 1 && dest.isEmpty() && enPassantTarget != "-") {
                val targetSquare = "${cols[toC]}${8 - toR}"
                targetSquare == enPassantTarget
            } else false
        }
    }

    private fun isValidKingMove(
        board: Array<Array<String>>,
        fromR: Int,
        fromC: Int,
        toR: Int,
        toC: Int,
        isWhite: Boolean,
        castlingRights: String
    ): Boolean {
        val dr = toR - fromR
        val dc = toC - fromC
        val absDr = abs(dr)
        val absDc = abs(dc)

        // Standard 1 square king step
        if (max(absDr, absDc) == 1) return true

        // Castling
        if (isWhite && fromR == 7 && fromC == 4 && toR == 7) {
            // Kingside castling
            if (toC == 6 && castlingRights.contains("K") &&
                board[7][5].isEmpty() && board[7][6].isEmpty() && board[7][7] == "R" &&
                !isKingInCheck(board, true)
            ) {
                return true
            }
            // Queenside castling
            if (toC == 2 && castlingRights.contains("Q") &&
                board[7][1].isEmpty() && board[7][2].isEmpty() && board[7][3].isEmpty() && board[7][0] == "R" &&
                !isKingInCheck(board, true)
            ) {
                return true
            }
        } else if (!isWhite && fromR == 0 && fromC == 4 && toR == 0) {
            // Black kingside castling
            if (toC == 6 && castlingRights.contains("k") &&
                board[0][5].isEmpty() && board[0][6].isEmpty() && board[0][7] == "r" &&
                !isKingInCheck(board, false)
            ) {
                return true
            }
            // Black queenside castling
            if (toC == 2 && castlingRights.contains("q") &&
                board[0][1].isEmpty() && board[0][2].isEmpty() && board[0][3].isEmpty() && board[0][0] == "r" &&
                !isKingInCheck(board, false)
            ) {
                return true
            }
        }

        return false
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

    /**
     * Executes a move on the board state, returning a rich ChessMoveResult with updated FEN.
     */
    fun executeMove(
        state: ChessBoardState,
        fromR: Int,
        fromC: Int,
        toR: Int,
        toC: Int
    ): ChessMoveResult? {
        if (!isValidMove(state, fromR, fromC, toR, toC)) return null

        val board = cloneBoard(state.board)
        val piece = board[fromR][fromC]
        val dest = board[toR][toC]
        val isWhite = piece.first().isUpperCase()
        var capturedPiece: String? = if (dest.isNotEmpty()) dest else null

        // En passant capture execution
        var isEnPassantCapture = false
        if (piece.equals("P", ignoreCase = true) && abs(toC - fromC) == 1 && dest.isEmpty()) {
            val targetSquare = "${cols[toC]}${8 - toR}"
            if (targetSquare == state.enPassantTarget) {
                val capR = if (isWhite) toR + 1 else toR - 1
                capturedPiece = board[capR][toC]
                board[capR][toC] = ""
                isEnPassantCapture = true
            }
        }

        // Castling rook repositioning
        var isCastlingMove = false
        if (piece == "K" && fromR == 7 && fromC == 4) {
            if (toR == 7 && toC == 6) {
                board[7][5] = "R"
                board[7][7] = ""
                isCastlingMove = true
            } else if (toR == 7 && toC == 2) {
                board[7][3] = "R"
                board[7][0] = ""
                isCastlingMove = true
            }
        } else if (piece == "k" && fromR == 0 && fromC == 4) {
            if (toR == 0 && toC == 6) {
                board[0][5] = "r"
                board[0][7] = ""
                isCastlingMove = true
            } else if (toR == 0 && toC == 2) {
                board[0][3] = "r"
                board[0][0] = ""
                isCastlingMove = true
            }
        }

        // Move piece
        board[toR][toC] = piece
        board[fromR][fromC] = ""

        // Pawn promotion: Promote to Queen by default
        var isPromotion = false
        if (piece == "P" && toR == 0) {
            board[toR][toC] = "Q"
            isPromotion = true
        } else if (piece == "p" && toR == 7) {
            board[toR][toC] = "q"
            isPromotion = true
        }

        // Update castling rights
        var newCastling = state.castlingRights
        if (piece == "K") {
            newCastling = newCastling.replace("K", "").replace("Q", "")
        } else if (piece == "k") {
            newCastling = newCastling.replace("k", "").replace("q", "")
        } else if (piece == "R") {
            if (fromR == 7 && fromC == 7) newCastling = newCastling.replace("K", "")
            if (fromR == 7 && fromC == 0) newCastling = newCastling.replace("Q", "")
        } else if (piece == "r") {
            if (fromR == 0 && fromC == 7) newCastling = newCastling.replace("k", "")
            if (fromR == 0 && fromC == 0) newCastling = newCastling.replace("q", "")
        }
        if (newCastling.isEmpty()) newCastling = "-"

        // Update en passant square (if pawn advanced 2 squares)
        val newEnPassant = if (piece.equals("P", ignoreCase = true) && abs(toR - fromR) == 2) {
            val epRow = if (isWhite) 8 - (fromR - 1) else 8 - (fromR + 1)
            "${cols[fromC]}$epRow"
        } else {
            "-"
        }

        // Halfmove clock (resets on pawn move or capture)
        val newHalfmove = if (piece.equals("P", ignoreCase = true) || capturedPiece != null) {
            0
        } else {
            state.halfmoveClock + 1
        }

        // Fullmove number increments after Black moves
        val nextColor = if (state.activeColor == "w") "b" else "w"
        val newFullmove = if (state.activeColor == "b") state.fullmoveNumber + 1 else state.fullmoveNumber

        val newFen = toFen(
            board = board,
            activeColor = nextColor,
            castlingRights = newCastling,
            enPassantTarget = newEnPassant,
            halfmoveClock = newHalfmove,
            fullmoveNumber = newFullmove
        )

        val nextInCheck = isKingInCheck(board, nextColor == "w")
        val nextHasMoves = hasAnyLegalMoves(board, nextColor, newCastling, newEnPassant)
        val isCheckmate = nextInCheck && !nextHasMoves
        val isStalemate = !nextInCheck && !nextHasMoves
        val isDraw = isStalemate || newHalfmove >= 100 // 50-move rule

        // Notation builder
        val pieceSym = pieceSymbols.getOrDefault(piece, piece)
        val fromNotation = "${cols[fromC]}${8 - fromR}"
        val toNotation = "${cols[toC]}${8 - toR}"
        val captureSymbol = if (capturedPiece != null) "x" else "-"
        val checkSymbol = if (isCheckmate) "#" else if (nextInCheck) "+" else ""
        val promoSymbol = if (isPromotion) "=Q" else ""

        val moveNotation = when {
            isCastlingMove && toC == 6 -> "O-O$checkSymbol"
            isCastlingMove && toC == 2 -> "O-O-O$checkSymbol"
            else -> "$pieceSym $fromNotation$captureSymbol$toNotation$promoSymbol$checkSymbol"
        }

        val newState = ChessBoardState(
            fen = newFen,
            board = board,
            activeColor = nextColor,
            castlingRights = newCastling,
            enPassantTarget = newEnPassant,
            halfmoveClock = newHalfmove,
            fullmoveNumber = newFullmove,
            isCheck = nextInCheck,
            isCheckmate = isCheckmate,
            isStalemate = isStalemate
        )

        return ChessMoveResult(
            newState = newState,
            capturedPiece = capturedPiece,
            moveNotation = moveNotation,
            isCheck = nextInCheck,
            isCheckmate = isCheckmate,
            isDraw = isDraw
        )
    }

    private fun applyMoveToBoard(
        board: Array<Array<String>>,
        fromR: Int,
        fromC: Int,
        toR: Int,
        toC: Int,
        isWhite: Boolean,
        enPassantTarget: String
    ) {
        val piece = board[fromR][fromC]
        if (piece.equals("P", ignoreCase = true) && abs(toC - fromC) == 1 && board[toR][toC].isEmpty()) {
            val targetSquare = "${cols[toC]}${8 - toR}"
            if (targetSquare == enPassantTarget) {
                val capR = if (isWhite) toR + 1 else toR - 1
                board[capR][toC] = ""
            }
        }
        board[toR][toC] = piece
        board[fromR][fromC] = ""
    }

    fun isKingInCheck(board: Array<Array<String>>, isWhite: Boolean): Boolean {
        val kingChar = if (isWhite) "K" else "k"
        var kingR = -1
        var kingC = -1
        for (r in 0..7) {
            for (c in 0..7) {
                if (board[r][c] == kingChar) {
                    kingR = r
                    kingC = c
                    break
                }
            }
            if (kingR != -1) break
        }
        if (kingR == -1) return false // No king found (e.g. captured)

        val oppColor = if (isWhite) "b" else "w"
        val dummyState = ChessBoardState(
            fen = "",
            board = board,
            activeColor = oppColor,
            castlingRights = "-",
            enPassantTarget = "-",
            halfmoveClock = 0,
            fullmoveNumber = 1
        )

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece.isNotEmpty() && (piece.first().isUpperCase() != isWhite)) {
                    val dr = kingR - r
                    val dc = kingC - c
                    val absDr = abs(dr)
                    val absDc = abs(dc)
                    val canAttack = when (piece.uppercase()) {
                        "P" -> if (isWhite) (dr == 1 && absDc == 1) else (dr == -1 && absDc == 1)
                        "N" -> (absDr == 2 && absDc == 1) || (absDr == 1 && absDc == 2)
                        "B" -> absDr == absDc && isPathClear(board, r, c, kingR, kingC)
                        "R" -> (absDr == 0 || absDc == 0) && isPathClear(board, r, c, kingR, kingC)
                        "Q" -> ((absDr == absDc) || (absDr == 0 || absDc == 0)) && isPathClear(board, r, c, kingR, kingC)
                        "K" -> max(absDr, absDc) == 1
                        else -> false
                    }
                    if (canAttack) return true
                }
            }
        }
        return false
    }

    private fun hasAnyLegalMoves(
        board: Array<Array<String>>,
        activeColor: String,
        castling: String,
        enPassant: String
    ): Boolean {
        val isWhite = activeColor == "w"
        val testState = ChessBoardState(
            fen = "",
            board = board,
            activeColor = activeColor,
            castlingRights = castling,
            enPassantTarget = enPassant,
            halfmoveClock = 0,
            fullmoveNumber = 1
        )

        for (fromR in 0..7) {
            for (fromC in 0..7) {
                val piece = board[fromR][fromC]
                if (piece.isNotEmpty() && (piece.first().isUpperCase() == isWhite)) {
                    for (toR in 0..7) {
                        for (toC in 0..7) {
                            if (isValidMove(testState, fromR, fromC, toR, toC)) {
                                return true
                            }
                        }
                    }
                }
            }
        }
        return false
    }

    fun getAllLegalMoves(state: ChessBoardState): List<Pair<Pair<Int, Int>, Pair<Int, Int>>> {
        val moves = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
        val isWhite = state.activeColor == "w"
        for (fromR in 0..7) {
            for (fromC in 0..7) {
                val piece = state.board[fromR][fromC]
                if (piece.isNotEmpty() && (piece.first().isUpperCase() == isWhite)) {
                    for (toR in 0..7) {
                        for (toC in 0..7) {
                            if (isValidMove(state, fromR, fromC, toR, toC)) {
                                moves.add(Pair(Pair(fromR, fromC), Pair(toR, toC)))
                            }
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun cloneBoard(board: Array<Array<String>>): Array<Array<String>> {
        return Array(8) { r -> Array(8) { c -> board[r][c] } }
    }
}
