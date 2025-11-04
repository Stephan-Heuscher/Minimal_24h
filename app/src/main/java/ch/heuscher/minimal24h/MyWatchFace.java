/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.provider.Settings;
import androidx.wear.watchface.CanvasWatchFaceService;
import androidx.wear.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static ch.heuscher.minimal24h.WatchFaceConstants.*;

/**
 * Analog 24h watch face. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 *
 * This watch face displays:
 * - A 24-hour rotating indicator showing the current time
 * - Battery status (turns red when low)
 * - Status indicators for WiFi, notifications, DND, airplane mode, and GPS
 * - Next alarm indicator (when within 18 hours)
 */
public class MyWatchFace extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    /**
     * The main watch face engine that handles rendering and lifecycle.
     */
    public class Engine extends CanvasWatchFaceService.Engine {

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        private boolean mRegisteredReceivers = false;
        private Calendar mCalendar;

        // Architecture components
        private WatchFaceRenderer mRenderer;
        private SystemStatusProvider mStatusProvider;
        private StatusIndicatorManager mStatusIndicatorManager;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setShowUnreadCountIndicator(true)
                    .setHideStatusBar(true)
                    .build());

            // Initialize paint objects
            Paint backgroundPaint = createBackgroundPaint();
            Paint handPaint = createHandPaint();

            // Initialize architecture components
            mRenderer = new WatchFaceRenderer(backgroundPaint, handPaint);

            mStatusProvider = new SystemStatusProvider(
                    (BatteryManager) getSystemService(Context.BATTERY_SERVICE),
                    (AlarmManager) getSystemService(Context.ALARM_SERVICE),
                    (WifiManager) getSystemService(Context.WIFI_SERVICE),
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE),
                    (LocationManager) getSystemService(Context.LOCATION_SERVICE),
                    getContentResolver()
            );

            mStatusIndicatorManager = new StatusIndicatorManager(mStatusProvider);
            mCalendar = Calendar.getInstance();
        }

        /**
         * Creates and configures the background paint.
         */
        private Paint createBackgroundPaint() {
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setAntiAlias(true);
            return paint;
        }

        /**
         * Creates and configures the hand/text paint.
         */
        private Paint createHandPaint() {
            Paint paint = new Paint();
            paint.setStrokeWidth(STROKE_WIDTH);
            paint.setAntiAlias(true);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setTextSize(TEXT_SIZE);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            return paint;
        }

        @Override
        public void onDestroy() {
            unregisterReceiver();
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            // Update renderer dimensions
            mRenderer.setDimensions(width, height);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());

            try {
                // Draw background
                mRenderer.drawBackground(canvas);

                // Calculate current time rotation
                final float currentRotation = TimeCalculator.getDegreesFromNorth(mCalendar);

                // Draw hour hand indicator (with battery color indication)
                boolean isLowBattery = mStatusProvider.isBatteryLow();
                mRenderer.drawHourHandIndicator(canvas, currentRotation, isLowBattery);

                // Draw 24-hour orientation marker
                mRenderer.draw24HourMarker(canvas);

                // Draw center circle with status indicators
                boolean hasActiveIndicators = mStatusIndicatorManager.hasActiveIndicators(
                        getUnreadCount(),
                        getNotificationCount(),
                        getInterruptionFilter()
                );
                mRenderer.drawCenterCircle(canvas, currentRotation, hasActiveIndicators);

                // Draw alarm indicator if within threshold
                if (mStatusProvider.shouldDisplayAlarm(mCalendar)) {
                    SystemStatusProvider.AlarmInfo alarm = mStatusProvider.getNextAlarm();
                    if (alarm != null) {
                        float alarmRotation = TimeCalculator.getDegreesFromNorth(alarm.getAlarmCalendar());
                        mRenderer.drawAlarmIndicator(canvas, alarmRotation);
                    }
                }
            } catch (SecurityException e) {
                // Display error message if permissions are missing
                mRenderer.drawError(canvas, "SecurityException");
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }
        }

        private void registerReceiver() {
            if (mRegisteredReceivers) {
                return;
            }
            mRegisteredReceivers = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredReceivers) {
                return;
            }
            mRegisteredReceivers = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }
    }
}