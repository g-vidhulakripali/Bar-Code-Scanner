package com.projects.barcodescanner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class ScannerOverlayView extends View {

    private final Paint boxPaint;
    private final Paint laserPaint;
    private final Paint maskPaint;
    private RectF boxRect;

    // Animation
    private ValueAnimator laserAnimator;
    private float laserY;

    public ScannerOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // Paint for the semi-transparent mask
        maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setColor(Color.parseColor("#99000000")); // Black with ~60% opacity

        // Paint for the viewfinder box border. Using plain white is clean and high-contrast.
        boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setColor(Color.WHITE); // Use a reliable color like white
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8f);

        // Paint for the scanning "laser". Using a standard Android color is safe.
        laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        laserPaint.setColor(ContextCompat.getColor(context, android.R.color.holo_red_light)); // A bright, standard red
        laserPaint.setStrokeWidth(6f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            calculateBoxRect(w, h);
            setupLaserAnimator();
        }
    }

    private void calculateBoxRect(int viewWidth, int viewHeight) {
        // Define the size of the scanning box. Let's make it 80% of the view's width.
        float boxWidth = viewWidth * 0.8f;
        // Make the box a rectangle, e.g., half as tall as it is wide.
        float boxHeight = boxWidth * 0.5f;

        // Center the box in the view
        float left = (viewWidth - boxWidth) / 2;
        float top = (viewHeight - boxHeight) / 2;
        float right = left + boxWidth;
        float bottom = top + boxHeight;

        boxRect = new RectF(left, top, right, bottom);
    }

    private void setupLaserAnimator() {
        if (laserAnimator != null) {
            laserAnimator.cancel();
        }
        laserAnimator = ValueAnimator.ofFloat(boxRect.top, boxRect.bottom);
        laserAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        laserAnimator.setDuration(2000); // 2 seconds for one sweep
        laserAnimator.setRepeatCount(ValueAnimator.INFINITE);
        laserAnimator.setRepeatMode(ValueAnimator.RESTART);
        laserAnimator.addUpdateListener(animation -> {
            laserY = (float) animation.getAnimatedValue();
            // Invalidate only the area of the box to be more efficient
            invalidate((int) boxRect.left, (int) boxRect.top, (int) boxRect.right, (int) boxRect.bottom);
        });
        if (getVisibility() == VISIBLE) {
            laserAnimator.start();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (boxRect == null) {
            return;
        }

        // 1. Save the current canvas state
        int saved = canvas.saveLayer(null, null);

        // 2. Draw the semi-transparent mask over the entire view
        canvas.drawRect(0, 0, getWidth(), getHeight(), maskPaint);

        // 3. "Punch a hole" in the mask by drawing a clear rectangle on top of it
        Paint clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawRect(boxRect, clearPaint);

        // 4. Restore the canvas. The mask with the hole is now drawn.
        canvas.restoreToCount(saved);


        // Draw the border of the viewfinder box
        canvas.drawRoundRect(boxRect, 16f, 16f, boxPaint);

        // Draw the animated laser line
        if (laserAnimator != null && laserAnimator.isRunning()) {
            canvas.drawLine(boxRect.left, laserY, boxRect.right, laserY, laserPaint);
        }
    }

    // Public methods to control the animation from the Activity
    public void startAnimation() {
        if (laserAnimator != null && !laserAnimator.isRunning()) {
            laserAnimator.start();
        }
    }

    public void stopAnimation() {
        if (laserAnimator != null) {
            laserAnimator.cancel();
            // Redraw one last time to remove the laser line
            invalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation(); // Clean up animator when view is removed
    }
}