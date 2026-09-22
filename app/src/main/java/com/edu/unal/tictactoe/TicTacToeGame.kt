package com.edu.unal.tictactoe

import java.util.Arrays
import java.util.Random

class TicTacToeGame {
    private val mBoard = CharArray(BOARD_SIZE)
    private val noMove = -1

    // Seed the random number generator
    private val mRand: Random = Random()

    var computerDifficultyLevel: DifficultyLevel

    init {
        clearBoard()
        this.computerDifficultyLevel = DifficultyLevel.Expert
    }

    /**
     * Clear the board of all X's and O's by setting all spots to OPEN_SPOT.
     */
    fun clearBoard() {
        Arrays.fill(mBoard, OPEN_SPOT)
    }

    /**
     * Set the given player at the given location on the game board.
     * The location must be available, or the board will not be changed.
     * 
     * @param player   - The HUMAN_PLAYER or COMPUTER_PLAYER
     * @param location - The location (0-8) to place the move
     */
    fun setMove(player: Char, location: Int) {
        if ((location in 0..<BOARD_SIZE) && mBoard[location] == OPEN_SPOT) {
            mBoard[location] = player
        }
    }

    /**
     * Return the occupant of the given location on the game board.
     * 
     * @param location - The location (0-8)
     * @return The character occupant ('X', 'O', or ' ')
     */
    fun getBoardOccupant(location: Int): Char {
        if (location in 0..<BOARD_SIZE) {
            return mBoard[location]
        }
        return OPEN_SPOT
    }

    /**
     * Return the best move for the computer to make. You must call setMove()
     * to actually make the computer move to that location.
     *
     * @return The best move for the computer to make (0-8).
     */
    fun getComputerMove(): Int {
        var move: Int = noMove

        if (computerDifficultyLevel == DifficultyLevel.Easy) {
            move = getRandomMove()
        } else if (computerDifficultyLevel == DifficultyLevel.Harder) {
            move = getBlockingMove()

            if (move == noMove) {
                move = getRandomMove()
            }
        } else if (computerDifficultyLevel == DifficultyLevel.Expert) {
            move = getWinningMove()

            if (move == noMove) {
                move = getBlockingMove()
            }

            if (move == noMove) {
                move = getRandomMove()
            }
        }

        return move
    }

    private fun getWinningMove(): Int {
        for (i in 0..<BOARD_SIZE) {
            if (mBoard[i] == OPEN_SPOT) {
                mBoard[i] = COMPUTER_PLAYER
                if (checkForWinner() == Winner.O) {
                    mBoard[i] = OPEN_SPOT
                    return i
                }
                mBoard[i] = OPEN_SPOT
            }
        }
        return noMove
    }

    private fun getBlockingMove(): Int {
        for (i in 0..<BOARD_SIZE) {
            if (mBoard[i] == OPEN_SPOT) {
                mBoard[i] = HUMAN_PLAYER
                if (checkForWinner() == Winner.X) {
                    mBoard[i] = OPEN_SPOT
                    return i
                }
                mBoard[i] = OPEN_SPOT
            }
        }
        return noMove
    }

    private fun getRandomMove(): Int {
        val openSpots = mutableListOf<Int>()
        for (i in 0..<BOARD_SIZE) {
            if (mBoard[i] == OPEN_SPOT) {
                openSpots.add(i)
            }
        }
        if (openSpots.isEmpty()) return noMove
        return openSpots[mRand.nextInt(openSpots.size)]
    }

    /**
     * Check for a winner and return a status value indicating who has won.
     * 
     * @return The winner of the current match, if any. Human player represents X, computer player
     * represents O, and ties are their own enum entry.
     */
    fun checkForWinner(): Winner {
        // Check horizontal wins
        run {
            var i = 0
            while (i <= 6) {
                if (mBoard[i] == HUMAN_PLAYER && mBoard[i + 1] == HUMAN_PLAYER && mBoard[i + 2] == HUMAN_PLAYER) return Winner.X
                if (mBoard[i] == COMPUTER_PLAYER && mBoard[i + 1] == COMPUTER_PLAYER && mBoard[i + 2] == COMPUTER_PLAYER) return Winner.O
                i += 3
            }
        }

        // Check vertical wins
        for (i in 0..2) {
            if (mBoard[i] == HUMAN_PLAYER && mBoard[i + 3] == HUMAN_PLAYER && mBoard[i + 6] == HUMAN_PLAYER) return Winner.X
            if (mBoard[i] == COMPUTER_PLAYER && mBoard[i + 3] == COMPUTER_PLAYER && mBoard[i + 6] == COMPUTER_PLAYER) return Winner.O
        }

        // Check diagonal wins
        if ((mBoard[0] == HUMAN_PLAYER && mBoard[4] == HUMAN_PLAYER && mBoard[8] == HUMAN_PLAYER) ||
            (mBoard[2] == HUMAN_PLAYER && mBoard[4] == HUMAN_PLAYER && mBoard[6] == HUMAN_PLAYER)
        ) return Winner.X

        if ((mBoard[0] == COMPUTER_PLAYER && mBoard[4] == COMPUTER_PLAYER && mBoard[8] == COMPUTER_PLAYER) ||
            (mBoard[2] == COMPUTER_PLAYER && mBoard[4] == COMPUTER_PLAYER && mBoard[6] == COMPUTER_PLAYER)
        ) return Winner.O

        // Check for tie
        for (i in 0..<BOARD_SIZE) {
            // If we find an open spot, game is not over yet
            if (mBoard[i] == OPEN_SPOT) return Winner.NOBODY
        }

        // If no winner and no open spots, it's a tie
        return Winner.TIE
    }

    companion object {
        const val BOARD_SIZE: Int = 9

        // Characters used to represent the human, computer, and open spots
        const val HUMAN_PLAYER: Char = 'X'
        const val COMPUTER_PLAYER: Char = 'O'
        const val OPEN_SPOT: Char = ' '
    }
}
