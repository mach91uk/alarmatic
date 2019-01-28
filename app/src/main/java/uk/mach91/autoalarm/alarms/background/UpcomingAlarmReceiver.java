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
 * Copyright 2018 Chris Allenby - content updated for Alarmation
 *
 */

package uk.mach91.autoalarm.alarms.background;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import uk.mach91.autoalarm.alarms.misc.AlarmController;
import uk.mach91.autoalarm.MainActivity;
import uk.mach91.autoalarm.R;
import uk.mach91.autoalarm.alarms.Alarm;
import uk.mach91.autoalarm.alarms.misc.AlarmController;
import uk.mach91.autoalarm.util.ContentIntentUtils;
import uk.mach91.autoalarm.util.ParcelableUtil;

import uk.mach91.autoalarm.alarms.misc.AlarmController;

import static uk.mach91.autoalarm.util.TimeFormatUtils.formatTime;

// TODO: Consider registering this locally instead of in the manifest.
public class UpcomingAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "UpcomingAlarmReceiver";
    /*TOneverDO: not private*/
    private static final String ACTION_DISMISS_NOW = "uk.mach91.autoalarm.action.DISMISS_NOW";

    public static final String ACTION_CANCEL_NOTIFICATION = "uk.mach91.autoalarm.action.CANCEL_NOTIFICATION";
    public static final String ACTION_SHOW_SNOOZING = "uk.mach91.autoalarm.action.SHOW_SNOOZING";
    public static final String EXTRA_ALARM = "uk.mach91.autoalarm.extra.ALARM";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final byte[] alarmBytes = intent.getByteArrayExtra(EXTRA_ALARM);
        // Unmarshall the bytes into a parcel and create our Alarm with it.
        final Alarm alarm = ParcelableUtil.unmarshall(alarmBytes, Alarm.CREATOR);
        if (alarm == null) {
            throw new IllegalStateException("No alarm received");
        }

        final long id = alarm.getId();
        final NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        final boolean actionShowSnoozing = ACTION_SHOW_SNOOZING.equals(intent.getAction());
        if (intent.getAction() == null || actionShowSnoozing) {
            // Prepare notification
            // http://stackoverflow.com/a/15803726/5055032
            // Notifications aren't updated on the UI thread, so we could have
            // done this in the background. However, no lengthy operations are
            // done here, so doing so is a premature optimization.
            String title;
            String text;
            if (actionShowSnoozing) {
                if (!alarm.isSnoozed()) {
                    throw new IllegalStateException("Can't show snoozing notif. if alarm not snoozed!");
                }
                title = alarm.label().isEmpty() ? context.getString(R.string.alarm) : alarm.label();
                text = context.getString(R.string.title_snoozing_until,
                        formatTime(context, alarm.snoozingUntil()));
            } else {
                // No intent action required for default behavior
                title = context.getString(R.string.upcoming_alarm);
                text = formatTime(context, alarm.ringsAt());
                if (!alarm.label().isEmpty()) {
                    text = alarm.label() + ", " + text;
                }
            }

            Intent dismissIntent = new Intent(context, UpcomingAlarmReceiver.class)
                    .setAction(ACTION_DISMISS_NOW)
                    .putExtra(EXTRA_ALARM, ParcelableUtil.marshall(alarm));
            PendingIntent piDismiss = PendingIntent.getBroadcast(context, (int) id,
                    dismissIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            String channelId = context.getString(R.string.channel_ID_alarm);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                String channelName = context.getString(R.string.channel_name_alarm);;
                int importance = NotificationManager.IMPORTANCE_LOW;
                NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
                mChannel.setSound(null, null);
                nm.createNotificationChannel(mChannel);
            }

            Notification.Builder builder = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.ic_alarm_24dp)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setContentIntent(ContentIntentUtils.create(context,
                            MainActivity.PAGE_ALARMS, id))
                    .addAction(R.drawable.ic_dismiss_alarm_24dp,
                            context.getString(R.string.dismiss_now), piDismiss);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                builder.setChannelId(channelId);
            }
            Notification note = builder.build();
            nm.notify(TAG, (int) id, note);
        } else if (ACTION_CANCEL_NOTIFICATION.equals(intent.getAction())) {
            nm.cancel(TAG, (int) id);
        } else if (ACTION_DISMISS_NOW.equals(intent.getAction())) {
            new AlarmController(context, null).cancelAlarm(alarm, false, true);
        }
    }
}
