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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.TimeZone;

import uk.mach91.autoalarm.R;
import uk.mach91.autoalarm.TimeZoneChangedActivity;
import uk.mach91.autoalarm.alarms.misc.AlarmPreferences;

/**
 * Created by Mach91 on 22/09/2018.
 */
public class OnTimeZoneChange extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(Intent.ACTION_TIMEZONE_CHANGED) && AlarmPreferences.notifyOfTimeZoneChange(context)) {

            String tzId = intent.getStringExtra("time-zone");

            SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(),Context.MODE_PRIVATE);

            String oldTzId = prefs.getString(context.getString(R.string.timezone), "");

            if (!oldTzId.isEmpty()) {
                TimeZone oldTimeZone = TimeZone.getTimeZone(oldTzId);
                TimeZone newTimeZone = TimeZone.getTimeZone(tzId);

                long now = System.currentTimeMillis();

                if (oldTimeZone.getOffset(now)!= newTimeZone.getOffset(now)){

                    Intent tzIntent = new Intent(context, TimeZoneChangedActivity.class);
                    tzIntent.putExtra(context.getString(R.string.newTimeZone), tzId);
                    tzIntent.putExtra(context.getString(R.string.oldTimeZone), oldTzId);

                    //context.startActivity(i);

                    SharedPreferences.Editor prefsEditor = prefs.edit();
                    prefsEditor.putString(context.getString(R.string.timezone), tzId);
                    prefsEditor.commit();

                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntentWithParentStack(tzIntent);

                    PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                    final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                    String content = context.getString(R.string.time_zone_change_notification_message);

                    String channelId = context.getString(R.string.channel_ID_timezone);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        String channelName = context.getString(R.string.channel_name_timezone);
                        ;
                        int importance = NotificationManager.IMPORTANCE_LOW;
                        NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
                        mChannel.setSound(null, null);
                        nm.createNotificationChannel(mChannel);
                    }

                    Notification.Builder mBuilder = new Notification.Builder(context)
                            .setContentTitle(context.getString(R.string.time_zone_change_notification_title))
                            .setContentText(content)
                            .setSmallIcon(R.drawable.ic_time_zone_chnaged_24dp)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent);

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        mBuilder.setChannelId(channelId);
                    }

                    nm.notify(147, mBuilder.build());
                    try {
                        Log.d("joda-time-android", "TIMEZONE_CHANGED received, changed default timezone to \"" + tzId + "\"");
                    } catch (IllegalArgumentException e) {
                        Log.e("joda-time-android", "Could not recognize timezone id \"" + tzId + "\"", e);
                    }
                }
            } else {
                SharedPreferences.Editor prefsEditor = prefs.edit();
                prefsEditor.putString(context.getString(R.string.timezone), tzId);
                prefsEditor.commit();
            }
        }
    }


}
