/*
 * Copyright 2017 Phillip Hsu
 *
 * This file is part of ClockPlus.
 *
 * ClockPlus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ClockPlus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ClockPlus.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2018 Mach91 - content updated for Alarmatic
 *
 */

package uk.mach91.autoalarm.alarms.background;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import uk.mach91.autoalarm.alarms.data.AlarmCursor;
import uk.mach91.autoalarm.alarms.data.AlarmsTableManager;
import uk.mach91.autoalarm.alarms.Alarm;
import uk.mach91.autoalarm.alarms.data.AlarmCursor;
import uk.mach91.autoalarm.alarms.data.AlarmsTableManager;
import uk.mach91.autoalarm.alarms.misc.AlarmController;

import uk.mach91.autoalarm.alarms.data.AlarmCursor;
import uk.mach91.autoalarm.alarms.data.AlarmsTableManager;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class OnBootUpAlarmScheduler extends IntentService {
    public OnBootUpAlarmScheduler() {
        super("OnBootUpAlarmScheduler");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            AlarmController controller = new AlarmController(this, null);
            // IntentService works in a background thread, so this won't hold us up.
            AlarmCursor cursor = new AlarmsTableManager(this).queryEnabledAlarms();
            while (cursor.moveToNext()) {
                Alarm alarm = cursor.getItem();
                if (!alarm.isEnabled()) {
                    throw new IllegalStateException(
                            "queryEnabledAlarms() returned alarm(s) that aren't enabled");
                }

                if (alarm.isIgnoringUpcomingRingTime()) {
                    Alarm newAlarm = alarm.toBuilder().build();
                    alarm.copyMutableFieldsTo(newAlarm);
                    newAlarm.ignoreUpcomingRingTime(false);
                    controller.cancelAlarm(alarm, false, false);
                    controller.scheduleAlarm(newAlarm, false);
                    controller.save(newAlarm);
                } else {
                    controller.scheduleAlarm(alarm, false);
                }
            }
            cursor.close();

        }
    }
}
