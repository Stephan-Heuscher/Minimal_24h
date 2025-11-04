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

import android.app.AlarmManager;
import android.content.ContentResolver;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.provider.Settings;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static ch.heuscher.minimal24h.WatchFaceConstants.*;

/**
 * Provides access to system status information for the watch face.
 * Encapsulates all system service queries and status checks.
 */
public class SystemStatusProvider {

    private final BatteryManager mBatteryManager;
    private final AlarmManager mAlarmManager;
    private final WifiManager mWifiManager;
    private final ConnectivityManager mConnectivityManager;
    private final LocationManager mLocationManager;
    private final ContentResolver mContentResolver;

    /**
     * Creates a new SystemStatusProvider with the given system services.
     *
     * @param batteryManager       The battery manager service (may be null)
     * @param alarmManager         The alarm manager service (may be null)
     * @param wifiManager          The WiFi manager service (may be null)
     * @param connectivityManager  The connectivity manager service (may be null)
     * @param locationManager      The location manager service (may be null)
     * @param contentResolver      The content resolver for system settings
     */
    public SystemStatusProvider(
            BatteryManager batteryManager,
            AlarmManager alarmManager,
            WifiManager wifiManager,
            ConnectivityManager connectivityManager,
            LocationManager locationManager,
            ContentResolver contentResolver) {

        this.mBatteryManager = batteryManager;
        this.mAlarmManager = alarmManager;
        this.mWifiManager = wifiManager;
        this.mConnectivityManager = connectivityManager;
        this.mLocationManager = locationManager;
        this.mContentResolver = contentResolver;
    }

    /**
     * Gets the current battery charge level.
     *
     * @return The battery charge percentage (0-100), or DEFAULT_BATTERY_LEVEL if unavailable
     */
    public int getBatteryCharge() {
        if (mBatteryManager != null) {
            return mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return DEFAULT_BATTERY_LEVEL;
    }

    /**
     * Checks if the battery level is low.
     *
     * @return true if battery is at or below the low threshold
     */
    public boolean isBatteryLow() {
        return getBatteryCharge() <= LOW_BATTERY_THRESHOLD;
    }

    /**
     * Checks if WiFi is currently enabled.
     *
     * @return true if WiFi is enabled, false otherwise
     */
    public boolean isWifiEnabled() {
        return mWifiManager != null && mWifiManager.isWifiEnabled();
    }

    /**
     * Checks if the device is in airplane mode.
     *
     * @return true if airplane mode is enabled, false otherwise
     */
    public boolean isAirplaneModeOn() {
        try {
            return Settings.Global.getInt(mContentResolver, Settings.Global.AIRPLANE_MODE_ON) == 1;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if there is an active network connection.
     *
     * @return true if connected to a network, false otherwise
     */
    public boolean hasNetworkConnection() {
        if (mConnectivityManager != null) {
            Network activeNetwork = mConnectivityManager.getActiveNetwork();
            return activeNetwork != null;
        }
        return false;
    }

    /**
     * Checks if GPS/location services are enabled.
     *
     * @return true if GPS provider is enabled, false otherwise
     */
    public boolean isGpsEnabled() {
        return mLocationManager != null &&
                mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Gets the next scheduled alarm information.
     *
     * @return AlarmInfo containing the alarm time, or null if no alarm is scheduled or available
     */
    public AlarmInfo getNextAlarm() {
        if (mAlarmManager != null) {
            AlarmManager.AlarmClockInfo alarmClockInfo = mAlarmManager.getNextAlarmClock();
            if (alarmClockInfo != null) {
                return new AlarmInfo(alarmClockInfo.getTriggerTime());
            }
        }
        return null;
    }

    /**
     * Checks if the next alarm should be displayed on the watch face.
     * An alarm is displayed if it's within the threshold hours from now.
     *
     * @param currentTime The current time
     * @return true if an alarm should be displayed, false otherwise
     */
    public boolean shouldDisplayAlarm(Calendar currentTime) {
        AlarmInfo alarm = getNextAlarm();
        if (alarm == null) {
            return false;
        }

        long thresholdTime = currentTime.getTimeInMillis() +
                TimeUnit.HOURS.toMillis(ALARM_DISPLAY_THRESHOLD_HOURS);

        return alarm.getTriggerTime() <= thresholdTime;
    }

    /**
     * Container class for alarm information.
     */
    public static class AlarmInfo {
        private final long mTriggerTime;

        public AlarmInfo(long triggerTime) {
            this.mTriggerTime = triggerTime;
        }

        /**
         * Gets the trigger time of the alarm.
         *
         * @return The alarm trigger time in milliseconds since epoch
         */
        public long getTriggerTime() {
            return mTriggerTime;
        }

        /**
         * Gets the alarm time as a Calendar instance.
         *
         * @return Calendar set to the alarm time
         */
        public Calendar getAlarmCalendar() {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(mTriggerTime);
            return calendar;
        }
    }
}
