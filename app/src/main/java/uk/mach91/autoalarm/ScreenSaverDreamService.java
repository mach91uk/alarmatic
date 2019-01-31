/*
 * Copyright 2017 Mach91
 *
 * This file is part of Alarmatic.
 *
 * Alarmatic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alarmatic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alarmatic.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package uk.mach91.autoalarm;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.service.dreams.DreamService;
import android.view.WindowManager;


import uk.mach91.autoalarm.alarms.misc.AlarmPreferences;
import uk.mach91.autoalarm.util.ScreenSaverUtils;

/**
 * Created by Mach91 on 23/09/2018.
 */
public class ScreenSaverDreamService extends DreamService {

    private ScreenSaverUtils mScreenSaverUtils;

    /**
     * Receiver to alarm clock changes.
     */
    private final BroadcastReceiver mAlarmChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            wakeUp();
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Exit dream upon user touch
        setInteractive(false);
        // Hide system UI
        setFullscreen(true);

        setScreenBright(!AlarmPreferences.screensaverNightMode(this));

    }

    @Override
    public void onWakeUp() {
        super.onWakeUp();
        finish();
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        mScreenSaverUtils = new ScreenSaverUtils(this);
        mScreenSaverUtils.addTimeView(this, null);

        setContentView(mScreenSaverUtils);

        // Setup handlers for time reference changes and date updates.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            registerReceiver(mAlarmChangedReceiver,
                    new IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED));
        }

    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        // Tear down handlers for time reference changes and date updates.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            unregisterReceiver(mAlarmChangedReceiver);
        }
    }

    @Override
    public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
        super.onWindowAttributesChanged(attrs);
    }

}