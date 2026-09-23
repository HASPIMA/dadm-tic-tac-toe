package com.edu.unal.tictactoe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

public class BoardView extends View {
    // Width of the board grid lines
    public static final int GRID_WIDTH = 6;

    private Bitmap humanBitmap;
    private Bitmap computerBitmap;

    // Controls the color and thickness of the lines drawn on the board
    private Paint paint;

    private TicTacToeGame game;
    private final Rect drawingRect = new Rect();

    public void initialize() {
        humanBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.x_img);
        computerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.o_img);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public void setGame(TicTacToeGame game) {
        this.game = game;
    }

    public int getBoardCellWidth() {
        return getWidth() / 3;
    }

    public int getBoardCellHeight() {
        return getHeight() / 3;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public BoardView(Context context) {
        super(context);
        initialize();
    }

    public BoardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Determine the width and height of the View
        int boardWidth = getWidth();
        int boardHeight = getHeight();

        // Define lines' color and thickness
        paint.setColor(Color.LTGRAY);
        paint.setStrokeWidth(GRID_WIDTH);

        // Draw the two vertical board lines
        int cellWidth = boardWidth / 3;
        canvas.drawLine(
                cellWidth, 0,
                cellWidth, boardHeight,
                paint
        );
        canvas.drawLine(
                cellWidth * 2, 0,
                cellWidth * 2, boardHeight,
                paint
        );

        // Draw the two horizontal board lines
        int cellHeight = boardHeight / 3;
        canvas.drawLine(
                0, cellHeight,
                boardWidth, cellHeight,
                paint
        );
        canvas.drawLine(
                0, cellHeight * 2,
                boardWidth, cellHeight * 2,
                paint
        );

        // Draw all the X and O images
        if (game != null) {
            for (int i = 0; i < TicTacToeGame.BOARD_SIZE; i++) {
                int col = i % 3;
                int row = i / 3;

                int left = col * cellWidth + GRID_WIDTH;
                int top = row * cellHeight + GRID_WIDTH;
                int right = (col + 1) * cellWidth - GRID_WIDTH;
                int bottom = (row + 1) * cellHeight - GRID_WIDTH;

                char occupant = game.getBoardOccupant(i);
                if (occupant == TicTacToeGame.HUMAN_PLAYER) {
                    drawingRect.set(left, top, right, bottom);
                    canvas.drawBitmap(humanBitmap, null, drawingRect, null);
                } else if (occupant == TicTacToeGame.COMPUTER_PLAYER) {
                    drawingRect.set(left, top, right, bottom);
                    canvas.drawBitmap(computerBitmap, null, drawingRect, null);
                }
            }
        }
    }
}
