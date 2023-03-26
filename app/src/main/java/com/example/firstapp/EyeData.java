package com.example.firstapp;

import android.graphics.PointF;

public class EyeData {
    private PointF pointF;
    private float openScore;

    // 0 for left and 1 for right
    private int eyeIndex;

    public PointF getPointF() {
        return pointF;
    }

    public float getOpenScore() {
        return openScore;
    }

    public int getEyeIndex() {
        return eyeIndex;
    }

    public void setPointF(PointF pointF1) {
        pointF = pointF1;
    }

    public void setOpenScore(float score) {
        openScore = score;
    }

    public void setEyeIndex(int eyeIndex1) {
        eyeIndex = eyeIndex1;
    }

}
