/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.heuscher.minimal24h;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import static ch.heuscher.minimal24h.WatchFaceConstants.*;

/**
 * Handles all drawing operations for the watch face.
 * Encapsulates canvas rendering logic and provides methods for drawing watch face elements.
 */
public class WatchFaceRenderer {

    private final Paint mBackgroundPaint;
    private final Paint mHandPaint;

    private float mCenterX;
    private float mCenterY;
    private float mHourHandLength;

    /**
     * Creates a new WatchFaceRenderer with the specified paints.
     *
     * @param backgroundPaint The paint for the background
     * @param handPaint       The paint for watch hands and text
     */
    public WatchFaceRenderer(Paint backgroundPaint, Paint handPaint) {
        this.mBackgroundPaint = backgroundPaint;
        this.mHandPaint = handPaint;
    }

    /**
     * Updates the dimensions of the watch face.
     * Should be called when the surface changes size.
     *
     * @param width  The width of the watch face
     * @param height The height of the watch face
     */
    public void setDimensions(int width, int height) {
        mCenterX = width / 2f;
        mCenterY = height / 2f;
        mHourHandLength = mCenterX - EDGE_RESERVE;
    }

    /**
     * Draws the background of the watch face.
     *
     * @param canvas The canvas to draw on
     */
    public void drawBackground(Canvas canvas) {
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
    }

    /**
     * Draws a circle at a specific rotation and distance from center.
     *
     * @param canvas           The canvas to draw on
     * @param rotationDegrees  The rotation angle in degrees from north
     * @param distanceFromCenter The distance from the center point
     * @param radius           The radius of the circle
     * @param paint            The paint to use for drawing
     */
    public void drawCircle(Canvas canvas, float rotationDegrees, float distanceFromCenter,
                           float radius, Paint paint) {
        if (radius == 0) return;

        canvas.save();
        canvas.rotate(rotationDegrees, mCenterX, mCenterY);
        canvas.drawCircle(mCenterX, mCenterY - distanceFromCenter, radius, paint);
        canvas.restore();
    }

    /**
     * Draws text at a specific position, rotated to remain upright relative to the watch face.
     * The text is centered both horizontally and vertically around the calculated position.
     *
     * @param canvas           The canvas to draw on
     * @param degreesFromNorth The angle in degrees from north where to place the text
     * @param radiusFromCenter The distance from center to place the text
     * @param text             The text to draw
     * @param paint            The paint to use for drawing
     * @param typeface         Optional typeface to use (null to use paint's current typeface)
     */
    public void drawTextUpright(Canvas canvas, float degreesFromNorth, float radiusFromCenter,
                                String text, Paint paint, Typeface typeface) {
        float textWidth = paint.measureText(text);
        float textHeight = paint.getTextSize();

        // Calculate position using TimeCalculator utilities
        float x = mCenterX - textWidth / 2 +
                TimeCalculator.calculateXOffset(degreesFromNorth, radiusFromCenter);
        float y = mCenterY + textHeight * TEXT_VERTICAL_CENTER_RATIO +
                TimeCalculator.calculateYOffset(degreesFromNorth, radiusFromCenter);

        // Temporarily change typeface if specified
        Typeface originalTypeface = null;
        if (typeface != null) {
            originalTypeface = paint.getTypeface();
            paint.setTypeface(typeface);
        }

        canvas.drawText(text, x, y, paint);

        // Restore original typeface
        if (originalTypeface != null) {
            paint.setTypeface(originalTypeface);
        }
    }

    /**
     * Draws the hour hand indicator (circle at the current time position).
     *
     * @param canvas      The canvas to draw on
     * @param rotation    The rotation angle for the current time
     * @param isLowBattery Whether battery is low (affects color)
     */
    public void drawHourHandIndicator(Canvas canvas, float rotation, boolean isLowBattery) {
        // Set color based on battery level
        mHandPaint.setColor(isLowBattery ? Color.RED : Color.WHITE);

        // Draw hour hand indicator circle
        drawCircle(canvas, rotation, mHourHandLength,
                mCenterX / CIRCLE_RADIUS_DIVISOR, mHandPaint);
    }

    /**
     * Draws the 24-hour orientation marker (at midnight/north position).
     *
     * @param canvas The canvas to draw on
     */
    public void draw24HourMarker(Canvas canvas) {
        drawTextUpright(canvas, 0, mHourHandLength - TEXT_SIZE / TEXT_OFFSET_DIVISOR,
                SYMBOL_HOUR_MARKER, mHandPaint, null);
    }

    /**
     * Draws the center circle with optional status indicator fill.
     *
     * @param canvas              The canvas to draw on
     * @param rotation            The rotation angle for the current time
     * @param hasStatusIndicators Whether status indicators are active
     */
    public void drawCenterCircle(Canvas canvas, float rotation, boolean hasStatusIndicators) {
        // Draw outer circle
        drawCircle(canvas, rotation, 0, mCenterX / CIRCLE_RADIUS_DIVISOR, mHandPaint);

        // Fill center with black if status indicators are present
        if (hasStatusIndicators) {
            drawCircle(canvas, rotation, 0,
                    mCenterX / CIRCLE_RADIUS_DIVISOR - CENTER_CIRCLE_FILL_ADJUSTMENT,
                    mBackgroundPaint);
        }
    }

    /**
     * Draws the alarm indicator at the alarm time position.
     *
     * @param canvas        The canvas to draw on
     * @param alarmRotation The rotation angle for the alarm time
     */
    public void drawAlarmIndicator(Canvas canvas, float alarmRotation) {
        drawTextUpright(canvas, alarmRotation, mHourHandLength,
                SYMBOL_ALARM, mHandPaint, null);
    }

    /**
     * Draws an error message at the center of the watch face.
     *
     * @param canvas       The canvas to draw on
     * @param errorMessage The error message to display
     */
    public void drawError(Canvas canvas, String errorMessage) {
        drawTextUpright(canvas, 0, 0, errorMessage, mHandPaint, null);
    }

    /**
     * Gets the current hour hand length.
     *
     * @return The hour hand length in pixels
     */
    public float getHourHandLength() {
        return mHourHandLength;
    }

    /**
     * Gets the center X coordinate.
     *
     * @return The X coordinate of the watch face center
     */
    public float getCenterX() {
        return mCenterX;
    }

    /**
     * Gets the center Y coordinate.
     *
     * @return The Y coordinate of the watch face center
     */
    public float getCenterY() {
        return mCenterY;
    }
}
