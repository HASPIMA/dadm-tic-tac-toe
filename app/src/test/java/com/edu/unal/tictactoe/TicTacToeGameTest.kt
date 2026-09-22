package com.edu.unal.tictactoe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TicTacToeGameTest {

    private lateinit var game: TicTacToeGame

    @Before
    fun setUp() {
        game = TicTacToeGame()
    }

    @Test
    fun testInitialBoardIsEmpty() {
        game.clearBoard()
        for (i in 0..<TicTacToeGame.BOARD_SIZE) {
            assertEquals(TicTacToeGame.OPEN_SPOT, game.getBoardOccupant(i))
        }
        assertEquals(Winner.NOBODY, game.checkForWinner())
    }

    @Test
    fun testSetMoveAndGetOccupant() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
        assertEquals(TicTacToeGame.HUMAN_PLAYER, game.getBoardOccupant(0))

        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 4)
        assertEquals(TicTacToeGame.COMPUTER_PLAYER, game.getBoardOccupant(4))

        // Cannot overwrite occupied spot
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 4)
        assertEquals(TicTacToeGame.COMPUTER_PLAYER, game.getBoardOccupant(4))
    }

    @Test
    fun testHorizontalWin() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 1)
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 2)

        assertEquals(Winner.X, game.checkForWinner())
    }

    @Test
    fun testVerticalWin() {
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 1)
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 4)
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 7)

        assertEquals(Winner.O, game.checkForWinner())
    }

    @Test
    fun testDiagonalWin() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 4)
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 8)

        assertEquals(Winner.X, game.checkForWinner())
    }

    @Test
    fun testTieGame() {
        // X O X
        // X O O
        // O X X
        val moves = listOf(
            Pair(TicTacToeGame.HUMAN_PLAYER, 0),
            Pair(TicTacToeGame.COMPUTER_PLAYER, 1),
            Pair(TicTacToeGame.HUMAN_PLAYER, 2),
            Pair(TicTacToeGame.HUMAN_PLAYER, 3),
            Pair(TicTacToeGame.COMPUTER_PLAYER, 4),
            Pair(TicTacToeGame.COMPUTER_PLAYER, 5),
            Pair(TicTacToeGame.COMPUTER_PLAYER, 6),
            Pair(TicTacToeGame.HUMAN_PLAYER, 7),
            Pair(TicTacToeGame.HUMAN_PLAYER, 8),
        )

        for ((player, loc) in moves) {
            game.setMove(player, loc)
        }

        assertEquals(Winner.TIE, game.checkForWinner())
    }

    @Test
    fun testComputerEasyMove() {
        game.computerDifficultyLevel = DifficultyLevel.Easy
        val move = game.getComputerMove()
        assertTrue((move in 0..<TicTacToeGame.BOARD_SIZE))
    }

    @Test
    fun testComputerHarderMoveBlocks() {
        game.computerDifficultyLevel = DifficultyLevel.Harder
        // Human is about to win on 0, 1 -> needs 2
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 1)

        val move = game.getComputerMove()
        assertEquals(2, move)
    }

    @Test
    fun testComputerExpertMoveWins() {
        game.computerDifficultyLevel = DifficultyLevel.Expert
        // Computer is about to win on 3, 4 -> needs 5
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 3)
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 4)

        val move = game.getComputerMove()
        assertEquals(5, move)
    }

    @Test
    fun testGetComputerMoveFullBoardReturnsNoMove() {
        for (i in 0..<TicTacToeGame.BOARD_SIZE) {
            game.setMove(TicTacToeGame.HUMAN_PLAYER, i)
        }
        val move = game.getComputerMove()
        assertEquals(-1, move)
    }
}
