package com.edu.unal.tictactoe;

import java.util.Arrays;
import java.util.Random;

public class TicTacToeGame {

    public static final int BOARD_SIZE = 9;

    // Characters used to represent the human, computer, and open spots
    public static final char HUMAN_PLAYER = 'X';
    public static final char COMPUTER_PLAYER = 'O';
    public static final char OPEN_SPOT = ' ';

    private char mBoard[] = new char[BOARD_SIZE];
    private Random mRand;

    public TicTacToeGame() {
        // Seed the random number generator
        mRand = new Random();
        clearBoard();
    }

    /** Clear the board of all X's and O's by setting all spots to OPEN_SPOT. */
    public void clearBoard() {
        Arrays.fill(mBoard, OPEN_SPOT);
    }

}
