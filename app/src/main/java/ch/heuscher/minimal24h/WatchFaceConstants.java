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

/**
 * Constants used throughout the watch face application.
 * Centralizes magic numbers and configuration values for easier maintenance.
 */
public final class WatchFaceConstants {

    // Prevent instantiation
    private WatchFaceConstants() {
        throw new AssertionError("Cannot instantiate WatchFaceConstants");
    }

    // === Drawing Constants ===

    /** Default text size for watch face elements (in pixels) */
    public static final float TEXT_SIZE = 15f;

    /** Edge reserve/margin for watch face (in pixels) */
    public static final float EDGE_RESERVE = 7.5f;

    /** Stroke width for drawing hands and lines */
    public static final float STROKE_WIDTH = 2f;

    /** Divisor for calculating circle radius from center X coordinate */
    public static final float CIRCLE_RADIUS_DIVISOR = 75f;

    /** Adjustment for filled center circle radius (pixels smaller than outer circle) */
    public static final float CENTER_CIRCLE_FILL_ADJUSTMENT = 1f;

    // === Time Calculation Constants ===

    /** Degrees per hour in 24-hour format (360° / 24 hours = 15°) */
    public static final float DEGREES_PER_HOUR = 15f;

    /** Divisor for converting minutes to degrees (60 minutes / 15° = 4) */
    public static final float MINUTES_TO_DEGREES_DIVISOR = 4f;

    /** Rotation offset for coordinate system alignment (90° counter-clockwise) */
    public static final float ROTATION_OFFSET_DEGREES = 90f;

    // === Battery Constants ===

    /** Battery level threshold for low battery warning (percentage) */
    public static final int LOW_BATTERY_THRESHOLD = 10;

    /** Default battery level when system service is unavailable */
    public static final int DEFAULT_BATTERY_LEVEL = 100;

    // === Alarm Constants ===

    /** Hours before alarm to start displaying alarm indicator */
    public static final long ALARM_DISPLAY_THRESHOLD_HOURS = 18;

    // === Text Positioning Constants ===

    /** Vertical text centering adjustment ratio (7/24 of text height) */
    public static final float TEXT_VERTICAL_CENTER_RATIO = 7f / 24f;

    /** Divisor for minute hand indicator text positioning (half of text size) */
    public static final float TEXT_OFFSET_DIVISOR = 2f;

    // === Status Indicator Symbols ===

    /** Symbol for WiFi enabled status */
    public static final String SYMBOL_WIFI = "W";

    /** Symbol for unread notifications */
    public static final String SYMBOL_UNREAD = "!";

    /** Symbol for other notifications */
    public static final String SYMBOL_NOTIFICATION = "i";

    /** Symbol for Do Not Disturb mode */
    public static final String SYMBOL_DND = "<";

    /** Symbol for airplane mode */
    public static final String SYMBOL_AIRPLANE = ">";

    /** Symbol for no network connection */
    public static final String SYMBOL_NO_CONNECTION = "X";

    /** Symbol for GPS/location enabled */
    public static final String SYMBOL_GPS = "⌖";

    /** Symbol for alarm indicator */
    public static final String SYMBOL_ALARM = "A";

    /** Symbol for hour marker (midnight/north indicator) */
    public static final String SYMBOL_HOUR_MARKER = "l";
}
