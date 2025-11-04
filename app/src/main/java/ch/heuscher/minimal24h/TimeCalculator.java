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

import java.util.Calendar;

import static ch.heuscher.minimal24h.WatchFaceConstants.*;

/**
 * Utility class for time-related calculations in the watch face.
 * Handles conversion between time values and rotation angles for the 24-hour watch face display.
 */
public final class TimeCalculator {

    // Prevent instantiation
    private TimeCalculator() {
        throw new AssertionError("Cannot instantiate TimeCalculator");
    }

    /**
     * Calculates the rotation angle from north (12 o'clock position) for a given time.
     * In a 24-hour watch face, the full 360° rotation represents 24 hours.
     *
     * @param time The time to convert to a rotation angle
     * @return The angle in degrees from north (0° = midnight/north)
     */
    public static float getDegreesFromNorth(Calendar time) {
        if (time == null) {
            throw new IllegalArgumentException("Time cannot be null");
        }

        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);

        return getDegreesFromNorth(hour, minute);
    }

    /**
     * Calculates the rotation angle from north (12 o'clock position) for given hour and minute values.
     * In a 24-hour watch face:
     * - Each hour = 15° (360° / 24 hours)
     * - Each minute = 0.25° (15° / 60 minutes)
     *
     * @param hour   The hour of day (0-23)
     * @param minute The minute of hour (0-59)
     * @return The angle in degrees from north (0° = midnight/north)
     */
    public static float getDegreesFromNorth(int hour, int minute) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be between 0 and 23");
        }
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("Minute must be between 0 and 59");
        }

        return hour * DEGREES_PER_HOUR + minute / MINUTES_TO_DEGREES_DIVISOR;
    }

    /**
     * Converts an angle in degrees to radians with the watch face coordinate system offset applied.
     * The watch face coordinate system is rotated 90° counter-clockwise from standard polar coordinates.
     *
     * @param degreesFromNorth The angle in degrees from north
     * @return The angle in radians, adjusted for the coordinate system
     */
    public static double toRadiansWithOffset(float degreesFromNorth) {
        return Math.toRadians(degreesFromNorth - ROTATION_OFFSET_DEGREES);
    }

    /**
     * Calculates the X coordinate offset from center for a given angle and radius.
     *
     * @param degreesFromNorth The angle in degrees from north
     * @param radius           The radius/distance from center
     * @return The X offset from center
     */
    public static float calculateXOffset(float degreesFromNorth, float radius) {
        return radius * (float) Math.cos(toRadiansWithOffset(degreesFromNorth));
    }

    /**
     * Calculates the Y coordinate offset from center for a given angle and radius.
     *
     * @param degreesFromNorth The angle in degrees from north
     * @param radius           The radius/distance from center
     * @return The Y offset from center
     */
    public static float calculateYOffset(float degreesFromNorth, float radius) {
        return radius * (float) Math.sin(toRadiansWithOffset(degreesFromNorth));
    }
}
