package com.cb3g.channel19;

import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ProgressBar;

class ProgressAnimation extends Animation {
    private final ProgressBar progressBar;
    private final float from;
    private final float  to;

    ProgressAnimation(ProgressBar progressBar, float from) {
        super();
        this.progressBar = progressBar;
        this.from = from;
        this.to = (float) 0;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        float value = from + (to - from) * interpolatedTime;
        progressBar.setProgress((int) value);
    }

}