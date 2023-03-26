package com.example.firstapp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import com.google.android.gms.vision.face.Face;

public class EyeGraphic extends GraphicOverlay.Graphic {
    private Paint eyePositionPaint;
    private Paint eyeScorePaint;
    private PointF pointF;
    private float openScore;

    private int eyeIndex;

    private static final float EYE_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;

    public EyeGraphic(GraphicOverlay overlay, EyeData eyeData) {
        super(overlay);

        pointF = eyeData.getPointF();
        openScore = eyeData.getOpenScore();
        eyeIndex = eyeData.getEyeIndex();

        eyePositionPaint = new Paint();
        eyePositionPaint.setColor(Color.RED);
        eyePositionPaint.setStyle(Paint.Style.FILL);
        eyePositionPaint.setAntiAlias(true);

        eyeScorePaint = new Paint();
        eyeScorePaint.setColor(Color.WHITE);
        eyeScorePaint.setTextSize(ID_TEXT_SIZE);
        eyeScorePaint.setAntiAlias(true);
    }

    @Override
    public void draw(Canvas canvas) {
        PointF pointF1 = pointF;
        if (pointF1 == null) {
            return;
        }

        // Draws a circle at the position of the detected face
        float x = translateX(pointF1.x);
        float y = translateY(pointF1.y);
        canvas.drawCircle(x, y, EYE_RADIUS, eyePositionPaint);

        canvas.drawText(String.valueOf(openScore), x+20, y+20, eyeScorePaint);
    }

    void updateFace(PointF pointF) {
        pointF = pointF;
        postInvalidate();
    }
}
