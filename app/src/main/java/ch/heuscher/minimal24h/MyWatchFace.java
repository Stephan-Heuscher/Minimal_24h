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
 */
public class MyWatchFace extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

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
        private Paint mBackgroundPaint;
        private Paint mHandPaint;
        private Typeface mLight = Typeface.create("sans-serif-thin", Typeface.NORMAL);
        private Typeface mNormal = Typeface.create("sans-serif", Typeface.NORMAL);

        private float mHourHandLength;
        private int mWidth;
        private int mHeight;
        private float mCenterX;
        private float mCenterY;

        // Cached system services for performance
        private BatteryManager mBatteryManager;
        private AlarmManager mAlarmManager;
        private WifiManager mWifiManager;
        private ConnectivityManager mConnectivityManager;
        private LocationManager mLocationManager;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this).
                    setShowUnreadCountIndicator(true).
                    setHideStatusBar(true).build());

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);
            mBackgroundPaint.setAntiAlias(true);

            mHandPaint = new Paint();
            mHandPaint.setStrokeWidth(STROKE_WIDTH);
            mHandPaint.setAntiAlias(true);
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);
            mHandPaint.setTextSize(TEXT_SIZE);
            mHandPaint.setTypeface(mNormal);
            mCalendar = Calendar.getInstance();

            // Cache system services for performance (avoid repeated getSystemService calls)
            mBatteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
            mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
            mWidth = width;
            mHeight = height;
            /*
             * Find the coordinates of the center point on the screen.
             * Ignore the window insets so that, on round watches
             * with a "chin", the watch face is centered on the entire screen,
             * not just the usable portion.
             */
            mCenterX = mWidth / 2f;
            mCenterY = mHeight / 2f;
            /*
             * Calculate the lengths of the watch hands and store them in member variables.
             */
            mHourHandLength = mCenterX - EDGE_RESERVE;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());

            // Draw the background.
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);

            final float hoursRotation = getDegreesFromNorth(mCalendar);

            int batteryCharge = DEFAULT_BATTERY_LEVEL;
            if (mBatteryManager != null) {
                batteryCharge = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }
            mHandPaint.setColor(Color.WHITE);
            // Color red when battery is low
            if (batteryCharge <= LOW_BATTERY_THRESHOLD) {
                mHandPaint.setColor(Color.RED);
            }

            // Hour hand indicator (minute position marker)
            drawCircle(hoursRotation, mHourHandLength, canvas, mCenterX / CIRCLE_RADIUS_DIVISOR, mHandPaint);
            // 24h orientation marker (midnight/north indicator)
            drawTextUprightFromCenter(0, mHourHandLength - TEXT_SIZE / TEXT_OFFSET_DIVISOR, SYMBOL_HOUR_MARKER, mHandPaint, canvas, null);

            // Center orientation circle
            drawCircle(hoursRotation, 0, canvas, mCenterX / CIRCLE_RADIUS_DIVISOR, mHandPaint);
            // Display status indicators: DND, no connection, notifications, WiFi, GPS
            String specials = getSpecials(canvas);
            // Fill center with black when status indicators are active
            if (specials != null && specials.length() > 0) {
                drawCircle(hoursRotation, 0, canvas, mCenterX / CIRCLE_RADIUS_DIVISOR - CENTER_CIRCLE_FILL_ADJUSTMENT, mBackgroundPaint);
            }

            float alarmDistanceFromCenter = mHourHandLength;
            Calendar time = Calendar.getInstance();
            if (mAlarmManager != null) {
                AlarmManager.AlarmClockInfo nextAlarmClock = mAlarmManager.getNextAlarmClock();
                if (nextAlarmClock != null && nextAlarmClock.getTriggerTime() - TimeUnit.HOURS.toMillis(ALARM_DISPLAY_THRESHOLD_HOURS) < mCalendar.getTimeInMillis()) {
                    time.setTimeInMillis(nextAlarmClock.getTriggerTime());
                    drawTextUprightFromCenter(getDegreesFromNorth(time),
                            alarmDistanceFromCenter, SYMBOL_ALARM, mHandPaint, canvas, null);
                }
            }
        }

        private String getSpecials(Canvas canvas) {
            String specials = "";
            try {
                if (mWifiManager != null && mWifiManager.isWifiEnabled()) {
                    specials += SYMBOL_WIFI;
                }
                if (getUnreadCount() > 0) {
                    specials += SYMBOL_UNREAD;
                }
                else if (getNotificationCount() > 0) {
                    specials += SYMBOL_NOTIFICATION;
                }
                if (getInterruptionFilter() != INTERRUPTION_FILTER_PRIORITY) {
                    specials += SYMBOL_DND;
                }
                if (Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON) == 1) {
                    specials += SYMBOL_AIRPLANE;
                }
                else {
                    if (mConnectivityManager != null) {
                        Network activeNetwork = mConnectivityManager.getActiveNetwork();
                        if (activeNetwork == null) {
                            specials += SYMBOL_NO_CONNECTION;
                        }
                    }
                }
                if (mLocationManager != null && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    specials += SYMBOL_GPS;
                }
            }
            catch (SecurityException | Settings.SettingNotFoundException e) {
                // Display error message on watch face for debugging
                String errorMsg = e.getClass().getSimpleName();
                drawTextUprightFromCenter(0, 0, errorMsg, mHandPaint, canvas, null);
            }
            return specials;
        }

        private void drawCircle(float rotationFromNorth, float distanceFromCenter, Canvas canvas, float radius, Paint paint) {
            if (radius == 0) return;
            canvas.save();
            canvas.rotate(rotationFromNorth, mCenterX, mCenterY);
            canvas.drawCircle(mCenterX, mCenterY - distanceFromCenter, radius, paint);
            canvas.restore();
        }

        private void drawTextUprightFromCenter(float degreesFromNorth, float radiusCenter, String text, Paint paint, Canvas canvas, Typeface typeface)
        {
            float textLengthX = paint.measureText(text);
            float textLengthY = paint.getTextSize();
            // Center text horizontally and vertically around the calculated position
            float x = mCenterX - textLengthX / 2 + radiusCenter *
                    (float) Math.cos(Math.toRadians(degreesFromNorth - ROTATION_OFFSET_DEGREES));
            float y = mCenterY + textLengthY * TEXT_VERTICAL_CENTER_RATIO +
                    radiusCenter *
                            (float) Math.sin(Math.toRadians(degreesFromNorth - ROTATION_OFFSET_DEGREES));
            if (typeface != null) {
                Typeface prevTypeface = paint.getTypeface();
                paint.setTypeface(typeface);
                canvas.drawText(text, x, y ,paint);
                paint.setTypeface(prevTypeface);
            }
            else {
                canvas.drawText(text, x, y ,paint);
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

    private float getDegreesFromNorth(Calendar time) {
        return time.get(Calendar.HOUR_OF_DAY) * DEGREES_PER_HOUR + time.get(Calendar.MINUTE) / MINUTES_TO_DEGREES_DIVISOR;
    }
}