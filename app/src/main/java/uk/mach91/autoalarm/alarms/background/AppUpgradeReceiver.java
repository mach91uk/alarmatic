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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import uk.mach91.autoalarm.R;
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
public class AppUpgradeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Note that this will be called when the app is updated, not when the app first launches.
        // We may have a lot of alarms to reschedule, so do this in the background using an IntentService.
        String action=intent.getAction();

        ///*
        final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String content = context.getString(R.string.time_zone_change_notification_message);

        String channelId = context.getString(R.string.channel_ID_timezone);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelName = context.getString(R.string.channel_name_timezone);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
            mChannel.setSound(null, null);
            nm.createNotificationChannel(mChannel);
        }

        Notification.Builder mBuilder = new Notification.Builder(context)
                .setContentTitle(context.getString(R.string.time_zone_change_notification_title))
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_time_zone_chnaged_24dp)
                .setAutoCancel(true);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mBuilder.setChannelId(channelId);
        }

        nm.notify(147, mBuilder.build());
        //*/

        if(action.equalsIgnoreCase(Intent.ACTION_MY_PACKAGE_REPLACED)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                OnBootUpAlarmJobScheduler.enqueueWork(context, new Intent());
            } else {
                context.startService(new Intent(context, OnBootUpAlarmScheduler.class));
            }
        }
    }
}
