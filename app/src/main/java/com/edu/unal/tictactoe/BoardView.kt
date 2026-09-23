package com.edu.unal.tictactoe

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View

class BoardView : View {
    private var humanBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.x_img)
    private var computerBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.o_img)

    // Controls the color and thickness of the lines drawn on the board
    private var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var game: TicTacToeGame? = null
    private val drawingRect = Rect()


    fun setGame(game: TicTacToeGame?) {
        this.game = game
    }

    val boardCellWidth: Int
        get() = width / 3

    val boardCellHeight: Int
        get() = height / 3

    override fun performClick(): Boolean {
        Log.d(TAG, "performClick")
        return super.performClick()
    }

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Determine the width and height of the View
        val boardWidth = width
        val boardHeight = height

        // Define lines' color and thickness
        paint.setColor(Color.LTGRAY)
        paint.strokeWidth = GRID_WIDTH.toFloat()

        // Draw the two vertical board lines
        val cellWidth = boardWidth / 3
        canvas.drawLine(
            cellWidth.toFloat(), 0f,
            cellWidth.toFloat(), boardHeight.toFloat(),
            paint
        )
        canvas.drawLine(
            (cellWidth * 2).toFloat(), 0f,
            (cellWidth * 2).toFloat(), boardHeight.toFloat(),
            paint
        )

        // Draw the two horizontal board lines
        val cellHeight = boardHeight / 3
        canvas.drawLine(
            0f, cellHeight.toFloat(),
            boardWidth.toFloat(), cellHeight.toFloat(),
            paint
        )
        canvas.drawLine(
            0f, (cellHeight * 2).toFloat(),
            boardWidth.toFloat(), (cellHeight * 2).toFloat(),
            paint
        )

        // Draw all the X and O images
        if (game != null) {
            for (i in 0..<TicTacToeGame.BOARD_SIZE) {
                val col = i % 3
                val row = i / 3

                val left: Int = col * cellWidth + GRID_WIDTH
                val top: Int = row * cellHeight + GRID_WIDTH
                val right: Int = (col + 1) * cellWidth - GRID_WIDTH
                val bottom: Int = (row + 1) * cellHeight - GRID_WIDTH

                val occupant = game!!.getBoardOccupant(i)
                if (occupant == TicTacToeGame.HUMAN_PLAYER) {
                    drawingRect.set(left, top, right, bottom)
                    canvas.drawBitmap(humanBitmap, null, drawingRect, null)
                } else if (occupant == TicTacToeGame.COMPUTER_PLAYER) {
                    drawingRect.set(left, top, right, bottom)
                    canvas.drawBitmap(computerBitmap, null, drawingRect, null)
                }
            }
        }
    }

    companion object {
        // Width of the board grid lines
        const val GRID_WIDTH: Int = 6
    }
}
