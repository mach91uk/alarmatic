/*
 * Copyright 2019 Mach91
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
 * along with ClockPlus.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package uk.mach91.autoalarm.alarms.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import uk.mach91.autoalarm.alarms.data.AlarmCursor;
import uk.mach91.autoalarm.alarms.data.AlarmsTableManager;
import uk.mach91.autoalarm.alarms.Alarm;

import uk.mach91.autoalarm.ringtone.AlarmActivity;
import uk.mach91.autoalarm.ringtone.playback.AlarmRingtoneService;
import uk.mach91.autoalarm.util.ParcelableUtil;

import static uk.mach91.autoalarm.util.Preconditions.checkNotNull;

/**
 * Used to actual trigger the sounding of an alarm.
 */
// TODO: Consider registering this locally instead of in the manifest.
public class OnAlarm extends BroadcastReceiver {
    // We include the class name in the string to distinguish this constant from the one defined
    // in UpcomingAlarmReceiver.
    public static final String EXTRA_ALARM_ID = "uk.mach91.autoalarm.alarms.background.PendingAlarmScheduler.extra.ALARM_ID";

    @Override
    public void onReceive(final Context context, Intent intent) {
        final long id = intent.getLongExtra(EXTRA_ALARM_ID, -1);
        if (id < 0) {
            throw new IllegalStateException("No alarm id received");
        }
        // Start our own thread to load the alarm instead of:
        //  * using a Loader, because we have no complex lifecycle and thus
        //  BroadcastReceiver has no built-in LoaderManager, AND getting a Loader
        //  to work here might be a hassle, let alone it might not even be appropriate to
        //  use Loaders outside of an Activity/Fragment, since it does depend on LoaderCallbacks.
        //  * using an AsyncTask, because we don't need to do anything on the UI thread
        //  after the background work is complete.
        // TODO: Verify using a Runnable like this won't cause a memory leak.
        // It *probably* won't because a BroadcastReceiver doesn't hold a Context,
        // and it also doesn't have a lifecycle, so it likely won't stick around
        // in memory.
        new Thread(new Runnable() {
            @Override
            public void run() {
                AlarmCursor cursor = new AlarmsTableManager(context).queryItem(id);
                Alarm alarm = checkNotNull(cursor.getItem());
                if (!alarm.isEnabled()) {
                    throw new IllegalStateException("Alarm must be enabled!");
                }

                Intent intent2 = new Intent(context, AlarmRingtoneService.class)
                        .putExtra(AlarmActivity.EXTRA_RINGING_OBJECT, ParcelableUtil.marshall(alarm));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent2);
                } else {
                    context.startService(intent2);
                }
            }
        }).start();
    }
}
