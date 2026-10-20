package com.edu.unal.tictactoe.online

import com.edu.unal.tictactoe.TicTacToeGame
import com.google.firebase.firestore.DocumentSnapshot

/** Signed-in Firebase user, anonymous (guest) or email/password. */
data class Account(
    val uid: String,
    val isAnonymous: Boolean,
    val email: String?,
)

/**
 * A document of `/game_sessions`. The board is decoded from Firestore's
 * `X`/`O`/`-` encoding into [TicTacToeGame]'s characters.
 */
data class GameSession(
    val id: String,
    val x: String,
    val o: String?,
    val xName: String,
    val oName: String?,
    val board: List<Char>,
    val active: Boolean,
    /** `"X"`, `"O"`, `"TIE"` or null (ongoing or cancelled). */
    val winner: String?,
) {
    val isWaitingForOpponent: Boolean get() = active && o == null

    /** Whose turn it is. X always moves first. */
    val nextTurn: Char
        get() = if (board.count { it == 'X' } > board.count { it == 'O' }) 'O' else 'X'

    fun markOf(uid: String): Char? = when (uid) {
        x -> 'X'
        o -> 'O'
        else -> null
    }

    fun isTurnOf(uid: String): Boolean =
        active && o != null && markOf(uid) == nextTurn

    fun opponentNameOf(uid: String): String? = if (uid == x) oName else xName

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): GameSession? {
            val x = snapshot.getString("X") ?: return null
            val encoded = snapshot.getString("board").orEmpty()
            return GameSession(
                id = snapshot.id,
                x = x,
                o = snapshot.getString("O"),
                xName = snapshot.getString("XName") ?: guestName(x),
                oName = snapshot.getString("OName"),
                board = List(TicTacToeGame.BOARD_SIZE) { i ->
                    when (val cell = encoded.getOrNull(i)) {
                        'X', 'O' -> cell
                        // '-' (empty), or anything malformed
                        else -> TicTacToeGame.OPEN_SPOT
                    }
                },
                active = snapshot.getBoolean("active") ?: false,
                winner = snapshot.getString("winner"),
            )
        }
    }
}

/** Name shown for users without a profile: `Guest-` plus 4 uid characters. */
fun guestName(uid: String): String = "Guest-${uid.take(4)}"
