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

package uk.mach91.autoalarm.alarms.background;


import android.content.Context;
import android.content.Intent;
import uk.mach91.autoalarm.alarms.data.AlarmCursor;
import uk.mach91.autoalarm.alarms.data.AlarmsTableManager;
import uk.mach91.autoalarm.alarms.Alarm;
import uk.mach91.autoalarm.alarms.misc.AlarmController;
import androidx.core.app.JobIntentService;
import androidx.annotation.NonNull;

/**
 * Created by Mach91 on 22/09/2018.
 */
public class OnBootUpAlarmJobScheduler extends JobIntentService {

    public static final int JOB_ID = 0x01;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, OnBootUpAlarmJobScheduler.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
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