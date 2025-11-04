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

import static ch.heuscher.minimal24h.WatchFaceConstants.*;

/**
 * Manages the generation of status indicator symbols for the watch face.
 * Determines which status symbols should be displayed based on system state.
 */
public class StatusIndicatorManager {

    private final SystemStatusProvider mStatusProvider;

    /**
     * Creates a new StatusIndicatorManager.
     *
     * @param statusProvider The system status provider
     */
    public StatusIndicatorManager(SystemStatusProvider statusProvider) {
        if (statusProvider == null) {
            throw new IllegalArgumentException("SystemStatusProvider cannot be null");
        }
        this.mStatusProvider = statusProvider;
    }

    /**
     * Generates a string of status indicator symbols based on current system state.
     * The symbols indicate various system states like WiFi, notifications, DND, etc.
     *
     * @param unreadCount        The number of unread notifications
     * @param notificationCount  The total number of notifications
     * @param interruptionFilter The current interruption filter mode
     * @return A string containing the appropriate status symbols
     */
    public String getStatusIndicators(int unreadCount, int notificationCount, int interruptionFilter) {
        StringBuilder indicators = new StringBuilder();

        // WiFi indicator
        if (mStatusProvider.isWifiEnabled()) {
            indicators.append(SYMBOL_WIFI);
        }

        // Notification indicators
        if (unreadCount > 0) {
            indicators.append(SYMBOL_UNREAD);
        } else if (notificationCount > 0) {
            indicators.append(SYMBOL_NOTIFICATION);
        }

        // Do Not Disturb indicator
        if (interruptionFilter != INTERRUPTION_FILTER_PRIORITY) {
            indicators.append(SYMBOL_DND);
        }

        // Airplane mode / No connection indicators
        if (mStatusProvider.isAirplaneModeOn()) {
            indicators.append(SYMBOL_AIRPLANE);
        } else if (!mStatusProvider.hasNetworkConnection()) {
            indicators.append(SYMBOL_NO_CONNECTION);
        }

        // GPS indicator
        if (mStatusProvider.isGpsEnabled()) {
            indicators.append(SYMBOL_GPS);
        }

        return indicators.toString();
    }

    /**
     * Checks if any status indicators should be displayed.
     *
     * @param unreadCount        The number of unread notifications
     * @param notificationCount  The total number of notifications
     * @param interruptionFilter The current interruption filter mode
     * @return true if any indicators should be shown, false otherwise
     */
    public boolean hasActiveIndicators(int unreadCount, int notificationCount, int interruptionFilter) {
        return !getStatusIndicators(unreadCount, notificationCount, interruptionFilter).isEmpty();
    }
}
