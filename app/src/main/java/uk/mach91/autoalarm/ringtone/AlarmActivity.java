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

package uk.mach91.autoalarm.ringtone;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import uk.mach91.autoalarm.alarms.misc.AlarmController;
import uk.mach91.autoalarm.alarms.misc.AlarmPreferences;
import uk.mach91.autoalarm.ringtone.playback.AlarmRingtoneService;
import uk.mach91.autoalarm.ringtone.playback.RingtoneService;
import uk.mach91.autoalarm.util.TimeFormatUtils;
import uk.mach91.autoalarm.R;
import uk.mach91.autoalarm.alarms.Alarm;
import uk.mach91.autoalarm.alarms.misc.AlarmController;
import uk.mach91.autoalarm.alarms.misc.AlarmPreferences;
import uk.mach91.autoalarm.ringtone.playback.AlarmRingtoneService;
import uk.mach91.autoalarm.ringtone.playback.RingtoneService;
import uk.mach91.autoalarm.util.TimeFormatUtils;

import uk.mach91.autoalarm.alarms.misc.AlarmController;
import uk.mach91.autoalarm.alarms.misc.AlarmPreferences;
import uk.mach91.autoalarm.ringtone.playback.AlarmRingtoneService;
import uk.mach91.autoalarm.ringtone.playback.RingtoneService;
import uk.mach91.autoalarm.util.TimeFormatUtils;

public class AlarmActivity extends RingtoneActivity<Alarm> {
    private static final String TAG = "AlarmActivity";

    final private static int BUTTON_ANIMATION_DELAY = 500;

    private AlarmController mAlarmController;
    private NotificationManager mNotificationManager;

    private int dissmissCount;

    private int mLongClick = 0;
    private int mFlipShakeAction =0;
    private int mFlipAction = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAlarmController = new AlarmController(this, null);
        // TODO: If the upcoming alarm notification isn't present, verify other notifications aren't affected.
        // This could be the case if we're starting a new instance of this activity after leaving the first launch.
        mAlarmController.removeUpcomingAlarmNotification(getRingingObject());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        dissmissCount = 0;

        mLongClick = AlarmPreferences.longClick(this);
        mFlipShakeAction = AlarmPreferences.flipShakeAction(this);
        mFlipAction = AlarmPreferences.flipAction(this);
    }

    @Override
    public void finish() {
        super.finish();
        // If the presently ringing alarm is about to be superseded by a successive alarm,
        // this, unfortunately, will cancel the missed alarm notification for the presently
        // ringing alarm.
        //
        // A workaround is to override onNewIntent() and post the missed alarm notification again,
        // AFTER calling through to its base implementation, because it calls finish().
        mNotificationManager.cancel(TAG, getRingingObject().getIntId());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // -------------- TOneverDO: precede super ---------------
        // Even though the base implementation calls finish() on this instance and starts a new
        // instance, this instance will still be alive with all of its member state intact at
        // this point. So this notification will still refer to the Alarm that was just missed.
        postMissedAlarmNote();
    }

    @Override
    protected Class<? extends RingtoneService> getRingtoneServiceClass() {
        return AlarmRingtoneService.class;
    }

    @Override
    protected CharSequence getHeaderTitle() {
        return getRingingObject().label();
    }

    @Override
    protected void getHeaderContent(ViewGroup parent) {
        // TODO: Consider applying size span on the am/pm label
        getLayoutInflater().inflate(R.layout.content_header_alarm_activity, parent, true);
    }

    @Override
    protected int getAutoSilencedText() {
        return R.string.alarm_auto_silenced_text;
    }

    @Override
    protected int getLeftButtonText() {
        return R.string.snooze;
    }

    @Override
    protected int getRightButtonText() {
        return R.string.dismiss;
    }

    @Override
    protected int getLeftButtonDrawable() {
        return R.drawable.ic_snooze_48dp;
    }

    @Override
    protected int getRightButtonDrawable() {
        return R.drawable.ic_dismiss_alarm_48dp;
    }

    @Override
    protected void onLeftButtonClick() {
        if (mLongClick == 0 || mLongClick == 2) {
            if (!(mFlipAction == 1 && mFlipShakeAction >= 0)) {
                mAlarmController.snoozeAlarm(getRingingObject());
                // Can't call dismiss() because we don't want to also call cancelAlarm()! Why? For example,
                // we don't want the alarm, if it has no recurrence, to be turned off right now.
                stopAndFinish();
            }
        }
    }

    @Override
    protected void onRightButtonClick() {
        if (mLongClick == 0 || mLongClick == 1) {
            if (!(mFlipAction == 2 && mFlipShakeAction >= 0)) {
                // TODO do we really need to cancel the intent and alarm?
                //mAlarmController.cancelAlarm(getRingingObject(), false, true);
                //stopAndFinish();
                dismissButtonPress();
            }
        }
    }

    protected void dismissButtonPress() {
        boolean dismissTwice = AlarmPreferences.mustDismissTwice(this);

        if (dismissTwice) {
            if (dissmissCount == 0) {
                FrameLayout fl_left = findViewById(R.id.btn_left);
                FrameLayout fl_right = findViewById(R.id.btn_right);

                ObjectAnimator animationLeftY = ObjectAnimator.ofFloat(fl_left, "translationY", fl_right.getY() - fl_left.getY());
                ObjectAnimator animationRightY = ObjectAnimator.ofFloat(fl_right, "translationY", fl_left.getY() - fl_right.getY());
                ObjectAnimator animationLeftX = ObjectAnimator.ofFloat(fl_left, "translationX", fl_right.getX() - fl_left.getX());
                ObjectAnimator animationRightX = ObjectAnimator.ofFloat(fl_right, "translationX", fl_left.getX() - fl_right.getX());

                AnimatorSet set  = new AnimatorSet();
                set.playTogether(animationLeftY, animationRightY, animationLeftX, animationRightX);
                set.setDuration(BUTTON_ANIMATION_DELAY);
                set.start();
            }
        }
        dissmissCount++;

        if (!dismissTwice || dissmissCount >=2) {
            mAlarmController.cancelAlarm(getRingingObject(), false, true);
            stopAndFinish();
        }
    }

    @Override
    protected boolean onLeftButtonLongClick() {
        if (!(mFlipAction == 1 && mFlipShakeAction >= 0)) {
            mAlarmController.snoozeAlarm(getRingingObject());
            // Can't call dismiss() because we don't want to also call cancelAlarm()! Why? For example,
            // we don't want the alarm, if it has no recurrence, to be turned off right now.
            stopAndFinish();
        } else {
            Toast.makeText(this, getString (R.string.alarm_must_shake), Toast.LENGTH_LONG).show();
        }
        return true;
    }

    @Override
    protected boolean onRightButtonLongClick() {
        if (!(mFlipAction == 2 && mFlipShakeAction >= 0)) {
            dismissButtonPress();
        } else {
            Toast.makeText(this, getString (R.string.alarm_must_shake), Toast.LENGTH_LONG).show();
        }
        return true;
    }
    @Override
    protected Parcelable.Creator<Alarm> getParcelableCreator() {
        return Alarm.CREATOR;
    }

    // TODO: Consider changing the return type to Notification, and move the actual
    // task of notifying to the base class.
    @Override
    protected void showAutoSilenced() {
        super.showAutoSilenced();
        postMissedAlarmNote();
    }

    private void postMissedAlarmNote() {
        String alarmTime = TimeFormatUtils.formatTime(this,
                getRingingObject().hour(), getRingingObject().minutes());

        String channelId = getString(R.string.channel_ID_missed_alarm);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelName = getString(R.string.channel_name_missed_alarm);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
            mChannel.setSound(null,null);
            mNotificationManager.createNotificationChannel(mChannel);
        }
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.missed_alarm))
                .setContentText(alarmTime)
                .setSmallIcon(R.drawable.ic_alarm_24dp);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder.setChannelId(channelId);
        }

        mNotificationManager.notify(TAG, getRingingObject().getIntId(), builder.build());
    }
}
