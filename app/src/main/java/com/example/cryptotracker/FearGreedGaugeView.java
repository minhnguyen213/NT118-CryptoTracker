package com.example.cryptotracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class FearGreedGaugeView extends View {
    private Paint paintBackground;
    private Paint paintValue;
    private RectF rectF;
    private int score = 50;

    public FearGreedGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintBackground = new Paint();
        paintBackground.setAntiAlias(true);
        paintBackground.setStyle(Paint.Style.STROKE);
        paintBackground.setStrokeWidth(30f);
        paintBackground.setStrokeCap(Paint.Cap.ROUND);
        paintBackground.setColor(Color.parseColor("#E0E0E0"));

        paintValue = new Paint();
        paintValue.setAntiAlias(true);
        paintValue.setStyle(Paint.Style.STROKE);
        paintValue.setStrokeWidth(30f);
        paintValue.setStrokeCap(Paint.Cap.ROUND);

        rectF = new RectF();
    }

    public void setScore(int score) {
        this.score = score;
        invalidate();
}

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        rectF.set(40f, 40f, width - 40f, (height * 2) - 40f);

        canvas.drawArc(rectF, 180f, 180f, false, paintBackground);

        if (score <= 25) paintValue.setColor(Color.parseColor("#F44336"));
        else if (score <= 45) paintValue.setColor(Color.parseColor("#FF9800"));
        else if (score <= 55) paintValue.setColor(Color.parseColor("#9E9E9E"));
        else if (score <= 75) paintValue.setColor(Color.parseColor("#8BC34A"));
        else paintValue.setColor(Color.parseColor("#4CAF50"));

        float sweepAngle = (score / 100f) * 180f;
        canvas.drawArc(rectF, 180f, sweepAngle, false, paintValue);
    }
}