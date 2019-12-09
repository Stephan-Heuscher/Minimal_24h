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
import android.os.PowerManager;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.SystemProviders;
import android.support.wearable.provider.WearableCalendarContract;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog 24h watch face. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {

    private static final float TEXT_SIZE = 15;
    private static final float RAND_RESERVE = 7.5f;

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
        private static final float STROKE_WIDTH = 2f;
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

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this).
                    setShowUnreadCountIndicator(true). // so dass Unread-Punkt nicht mehr sichtbar
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
            mHourHandLength = mCenterX - RAND_RESERVE;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());

            // Draw the background.
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);

            final float hoursRotation = getDegreesFromNorth(mCalendar);

            int batteryCharge = 100;
            BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager != null) {
                batteryCharge = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }
            mHandPaint.setColor(Color.WHITE);
            // Farbe rot wenn wenig Batterie
            if (batteryCharge <= 10) {
                mHandPaint.setColor(Color.RED);
            }

            // Minuten-"Zeiger"
            drawCircle(hoursRotation, mHourHandLength, canvas, mCenterX/75, mHandPaint);
            // 24h Orientierung
            drawTextUprightFromCenter(0, mHourHandLength - TEXT_SIZE/2, "l", mHandPaint, canvas, null);

            // Mitte-Orientierung
            drawCircle(hoursRotation, 0, canvas, mCenterX/75, mHandPaint);
            // DND + no Connection + "Message" + Wifi + Power anzeigen
            // ev. anzeigen, wenn aktiv
            String specials = getSpecials(batteryManager, canvas);
            // schwarz füllen, wenn etwas symbolisiert werden soll
            if (specials != null && specials.length()>0) {
                drawCircle(hoursRotation, 0, canvas, mCenterX/75 - 1, mBackgroundPaint);
            }

            float alarmDistanceFromCenter = mHourHandLength;
            Calendar time = Calendar.getInstance();
            AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarm != null) {
                AlarmManager.AlarmClockInfo nextAlarmClock = alarm.getNextAlarmClock();
                if (nextAlarmClock != null && nextAlarmClock.getTriggerTime() - TimeUnit.HOURS.toMillis(18) < mCalendar.getTimeInMillis()) {
                    time.setTimeInMillis(nextAlarmClock.getTriggerTime());
                    String alarmText = "A";//String.format("%tR", time.getTime());
                    drawTextUprightFromCenter(getDegreesFromNorth(time),
                            alarmDistanceFromCenter, alarmText, mHandPaint, canvas, null);
                }
            }
        }

        private String getSpecials(BatteryManager batteryManager, Canvas canvas) {
            String specials = "";
            try {
                WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null && wifiManager.isWifiEnabled()) {
                    specials += "W";
                }
                if (getUnreadCount() > 0) { // entweder ungelesene
                    specials += "!";
                }
                else if (getNotificationCount() > 0) { // oder noch andere
                    specials += "i";
                }
                if (getInterruptionFilter() != INTERRUPTION_FILTER_PRIORITY) {
                    specials += "<";
                }
                if (Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON) == 1) {
                    specials += ">";
                }
                else {
                    ConnectivityManager connectivityManager =(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    Network activeNetwork = connectivityManager.getActiveNetwork();
                    if (activeNetwork == null) {
                        specials += "X";
                    }
                }
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    specials += "⌖";
                }
            }
            catch (Throwable t) {
                drawTextUprightFromCenter(0,0, t.getMessage(), mHandPaint, canvas, null);
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
            //                          center text
            float x = mCenterX - textLengthX/2 + radiusCenter *
                    (float) Math.cos(Math.toRadians(degreesFromNorth - 90f));
            float y = mCenterY + textLengthY/24*7 +
                    radiusCenter *
                            (float) Math.sin(Math.toRadians(degreesFromNorth - 90f));
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
        return time.get(Calendar.HOUR_OF_DAY)*15f + time.get(Calendar.MINUTE)/4f;
    }
}