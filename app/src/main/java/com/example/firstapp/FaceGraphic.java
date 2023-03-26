package com.example.firstapp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.android.gms.vision.face.Face;

public class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private int mFaceId;
    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;
    private volatile Face mFace;

    public FaceGraphic(GraphicOverlay overlay, Face face) {
        super(overlay);

        mFace = face;
        mFaceId = face.getId();

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(Color.BLUE);
        mFacePositionPaint.setStyle(Paint.Style.FILL);
        mFacePositionPaint.setAntiAlias(true);

        mIdPaint = new Paint();
        mIdPaint.setColor(Color.WHITE);
        mIdPaint.setTextSize(ID_TEXT_SIZE);
        mIdPaint.setAntiAlias(true);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(Color.GREEN);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
        mBoxPaint.setAntiAlias(true);
    }

    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);
        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);

        // Draws the face ID and the bounding box around the face
        canvas.drawText("ID: " + mFaceId, x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint);
        canvas.drawRect(
                translateX(face.getPosition().x),
                translateY(face.getPosition().y),
                translateX(face.getPosition().x + face.getWidth()),
                translateY(face.getPosition().y + face.getHeight()),
                mBoxPaint
        );
    }

    public int getId() {
        return mFaceId;
    }

    public void setId(int id) {
        mFaceId = id;
    }

    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }
}
