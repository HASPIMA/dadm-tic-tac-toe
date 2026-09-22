package com.edu.unal.tictactoe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class BoardView extends View {
    // Width of the board grid lines
    public static final int GRID_WIDTH = 6;

    private Bitmap humanBitmap;
    private Bitmap computerBitmap;

    // Controls the color and thickness of the lines drawn on the board
    private Paint paint;

    public void initialize() {
        humanBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.x_img);
        computerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.o_img);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
}
