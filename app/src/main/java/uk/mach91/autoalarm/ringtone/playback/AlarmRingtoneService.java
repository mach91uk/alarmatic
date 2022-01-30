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

package uk.mach91.autoalarm.ringtone.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;

//import com.google.firebase.analytics.FirebaseAnalytics;

import uk.mach91.autoalarm.alarms.misc.AlarmController;
import uk.mach91.autoalarm.alarms.misc.AlarmPreferences;
import uk.mach91.autoalarm.ringtone.AlarmActivity;
import uk.mach91.autoalarm.ringtone.RingtoneActivity;
import uk.mach91.autoalarm.timepickers.Utils;
import uk.mach91.autoalarm.util.DurationUtils;
import uk.mach91.autoalarm.util.LocalBroadcastHelper;
import uk.mach91.autoalarm.util.ParcelableUtil;
import uk.mach91.autoalarm.util.TimeFormatUtils;
import uk.mach91.autoalarm.R;
import uk.mach91.autoalarm.alarms.Alarm;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static android.app.PendingIntent.getActivity;
import static uk.mach91.autoalarm.timepickers.Utils.getColorFromThemeAttr;

public class AlarmRingtoneService extends RingtoneService<Alarm> {
    private static final String TAG = "AlarmRingtoneService";
    /* TOneverDO: not private */
    private static final String ACTION_SNOOZE = "uk.mach91.autoalarm.ringtone.action.SNOOZE";
    private static final String ACTION_DISMISS = "uk.mach91.autoalarm.ringtone.action.DISMISS";

    private AlarmController mAlarmController;
    private static SensorManager mSensorService;
    private Sensor mSensor;
    private Sensor mShakeSensor;
    private boolean mFacingDown;
    private boolean mFacingDownSet;

    private Handler mVolumeHandler = null;
    private Runnable mVolumeRunnable = null;
    private Runnable mCalendarRunnable = null;
    private int mMaxVol = 0;
    private int mOrigVol = 0;
    private int mDelayBeforeNextVolUp = 3 * 1000;

    private int mFadeVolume = 0;

    private static final float SHAKE_THRESHOLD = 1.5f; // m/S**2
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 100;
    private long mLastShakeUpdate = 0;
    private int mShakeCount = 0;

    private int mFlipShakeAction =0;
    private int mFlipAction = 0;


    private boolean mCancelDueToHoliday = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We can have this before super because this will only call through
        // WHILE this Service has already been alive.

        final byte[] bytes = intent.getByteArrayExtra(EXTRA_RINGING_OBJECT);
        if (bytes != null) {
            boolean skip_holiday = ParcelableUtil.unmarshall(bytes, getParcelableCreator()).skip_holiday();

            Utils.logFirebaseEvent(this, "ALARM_SETTING", "SKIP_HOLIDAY-" + skip_holiday);
            if (mCancelDueToHoliday) {
                //if (lable.toLowerCase().equals(AlarmPreferences.cancelAlarmHolidayLabel(this).toLowerCase())) {
                if (skip_holiday) {
                    AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
                    mCalendarRunnable = new CalendarRunnable();
                    Handler mCalendarHandler = new Handler();
                    audio.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
                    mCalendarHandler.postDelayed(mCalendarRunnable, 3000);
                    Utils.logFirebaseEvent(this, "ALARM_ACTION", "SKIP_HOLIDAY");
                }
            }
        }

        if (intent.getAction() != null) {
            if (ACTION_SNOOZE.equals(intent.getAction())) {
                mAlarmController.snoozeAlarm(getRingingObject());
            } else if (ACTION_DISMISS.equals(intent.getAction())) {
                mAlarmController.cancelAlarm(getRingingObject(), false, true); // TODO do we really need to cancel the intent and alarm?
            } else {
                throw new UnsupportedOperationException();
            }
            // ==========================================================================
            stopSelf(startId);
            finishActivity();
        }

        int val =  super.onStartCommand(intent, flags, startId);
        mAlarmController.removeUpcomingAlarmNotification(getRingingObject());
        return val;
    }
    
    @Override
    public void onCreate() {
        //startForeground(R.id.ringtone_service_notification, getForegroundNotification());

        super.onCreate();

        mLastShakeUpdate = 0;
        mShakeCount = 0;

        mAlarmController = new AlarmController(this, null);

        //set up flip action
        mFlipShakeAction = AlarmPreferences.flipShakeAction(this);
        mFlipAction = AlarmPreferences.flipAction(this);
        if (mFlipAction > AlarmPreferences.FLIP_ACTION_NOTHING) {
            //Cancel the alarm when the phone is turned over
            mSensorService = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (mFlipShakeAction == 0) {
                mSensor = mSensorService.getDefaultSensor(Sensor.TYPE_GRAVITY);
                if (mSensor != null) {
                    mSensorService.registerListener(mySensorEventListener, mSensor,
                            SensorManager.SENSOR_DELAY_NORMAL);
                    Log.i("AlarmRingtoneService", "Registered for TYPE_GRAVITY Sensor");
                } else {
                    Log.e("AlarmRingtoneService", "Registration failed for TYPE_GRAVITY Sensor");
//                    Toast.makeText(this, "GRAVITY Sensor not found",
//                            Toast.LENGTH_LONG).show();
                    mFlipAction = AlarmPreferences.FLIP_ACTION_NOTHING;
                    LocalBroadcastHelper.sendBroadcast(this, RingtoneActivity.ACTION_SENSOR_NOT_OK);
                }
            } else if (mFlipShakeAction > 0) {
                mShakeSensor = mSensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (mShakeSensor != null) {
                    mSensorService.registerListener(mySensorEventListener, mShakeSensor,
                            SensorManager.SENSOR_DELAY_NORMAL);
                    Log.i("AlarmRingtoneService", "Registered for TYPE_ACCELEROMETER Sensor");
                } else {
                    Log.e("AlarmRingtoneService", "Registration failed for TYPE_ACCELEROMETER Sensor");
//                    Toast.makeText(this, "ACCELEROMETER Sensor not found",
//                            Toast.LENGTH_LONG).show();
                    mFlipAction = AlarmPreferences.FLIP_ACTION_NOTHING;
                    LocalBroadcastHelper.sendBroadcast(this, RingtoneActivity.ACTION_SENSOR_NOT_OK);
                }
            }
        }

        

        //Set volume level
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        mMaxVol = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        mFadeVolume = AlarmPreferences.fadeVolume(this);
        mOrigVol = audio.getStreamVolume(AudioManager.STREAM_ALARM);

        if (mFadeVolume == 1){
            audio.setStreamVolume(AudioManager.STREAM_ALARM, mMaxVol, 0);
        }
        else if (mFadeVolume > 1){
            //Gradually increase the volume
            audio.setStreamVolume(AudioManager.STREAM_ALARM, 1, 0);

            mDelayBeforeNextVolUp = mFadeVolume  * 1000 / (mMaxVol - 1);//seconds between each increment//
            mVolumeHandler = new Handler();
            mVolumeRunnable = new VolumeRunnable(audio, mVolumeHandler);
            mVolumeHandler.postDelayed(mVolumeRunnable, mDelayBeforeNextVolUp);
        }

        mCancelDueToHoliday = DurationUtils.isOnHoliday(this, System.currentTimeMillis());

        Utils.logFirebaseEvent(this, "SETTINGS", "SKIP_HOLIDAY-" + AlarmPreferences.cancelAlarmHoliday(this));

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSensorService != null && (mSensor != null || mShakeSensor != null)) {
            mSensorService.unregisterListener(mySensorEventListener);
        }
        if (mVolumeHandler != null && mVolumeRunnable != null){
            mVolumeHandler.removeCallbacks(mVolumeRunnable);
        }
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        audio.setStreamVolume(AudioManager.STREAM_ALARM, mOrigVol, 0);
    }

    //Cancel the alarm when the phone is turned over
    private SensorEventListener mySensorEventListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            final float factor = 0.85F;

            if (event.sensor == mSensor) {
                if (!mFacingDownSet) {
                    mFacingDown = event.values[2] < 0;
                    mFacingDownSet = true;
                } else {
                    boolean nowDown = event.values[2] < -SensorManager.GRAVITY_EARTH * factor;
                    if (mFacingDown){
                        nowDown = !(event.values[2] > SensorManager.GRAVITY_EARTH * factor);
                    }
                    if (!mFacingDownSet || nowDown != mFacingDown) {
                        if (nowDown) {
                            Log.i(TAG, "DOWN");
                        } else {
                            Log.i(TAG, "UP");
                        }
                        if (mFacingDownSet && mFacingDown != nowDown) {
                            if (mFlipAction == AlarmPreferences.FLIP_ACTION_SNOOZE) {
                                mAlarmController.snoozeAlarm(getRingingObject());
                                Utils.logFirebaseEvent(getApplicationContext(), "ALARM_ACTION", "FLIP_SNOOZE");
                            } else if (mFlipAction == AlarmPreferences.FLIP_ACTION_DISMISS){
                                mAlarmController.cancelAlarm(getRingingObject(), false, true);
                                Utils.logFirebaseEvent(getApplicationContext(), "ALARM_ACTION", "FLIP_DISMISS");
                            }
                            cancelNow();
                        }
                    }
                }
            } else if (event.sensor == mShakeSensor) {
                long curTime = System.currentTimeMillis();
                // only allow one update every 100ms.
                if ((curTime - mLastShakeUpdate) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    if (mLastShakeUpdate != 0) {
                        double acceleration = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;

                        if (acceleration > SHAKE_THRESHOLD) {
                            mShakeCount++;

                            if (mShakeCount >=  mFlipShakeAction) {
                                if (mFlipAction == AlarmPreferences.FLIP_ACTION_SNOOZE) {
                                    mAlarmController.snoozeAlarm(getRingingObject());
                                    Utils.logFirebaseEvent(getApplicationContext(), "ALARM_ACTION", "SHAKE_SNOOZE");
                                } else  if (mFlipAction == AlarmPreferences.FLIP_ACTION_DISMISS) {
                                    mAlarmController.cancelAlarm(getRingingObject(), false, true);
                                    Utils.logFirebaseEvent(getApplicationContext(), "ALARM_ACTION", "SHAKE_DISMISS");
                                }
                                cancelNow();
                            } else {
                                shakeNow();
                            }
                        }
                    }
                    mLastShakeUpdate = curTime;
                }
            }

        }
    };

    //Gradually increase the volume
    public class VolumeRunnable implements Runnable {

        private AudioManager mAudioManager;
        private Handler mHandlerThatWillIncreaseVolume;

        VolumeRunnable (AudioManager audioManager, Handler handler) {
            this.mAudioManager = audioManager;
            this.mHandlerThatWillIncreaseVolume = handler;
        }

        @Override
        public void run() {
            int currentAlarmVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            if (currentAlarmVolume != mMaxVol) { //if we havent reached the max

                //here increase the volume of the alarm stream by adding currentAlarmVolume+someNewFactor
                mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, currentAlarmVolume + 1, 0);
                if (currentAlarmVolume + 1 != mMaxVol) {
                    mHandlerThatWillIncreaseVolume.postDelayed(this, mDelayBeforeNextVolUp); //"recursively call this runnable again with some delay between each increment of the volume, untill the condition above is satisfied.
                }
            }

        }
    }

    //Auto cancel Work
    public class CalendarRunnable implements Runnable {


        CalendarRunnable () {

        }

        @Override
        public void run() {
            mAlarmController.cancelAlarm(getRingingObject(), false, true);
            cancelNow();
        }
    }

    @Override
    protected void onAutoSilenced() {
        // TODO do we really need to cancel the alarm and intent?
        postMissedAlarmNote();
        mAlarmController.cancelAlarm(getRingingObject(), false, true);
    }

    private final void postMissedAlarmNote() {
        String alarmTime = TimeFormatUtils.formatTime(this,
                getRingingObject().hour(), getRingingObject().minutes());

        NotificationManager mNotificationManager;
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

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

    @Override
    protected Uri getRingtoneUri() {
        String ringtone = getRingingObject().ringtone();
        // can't be null...
        if (ringtone.isEmpty()) {
            ringtone = PreferenceManager.getDefaultSharedPreferences(this).getString(this.getString(R.string.key_default_alarm_tone_picker), "");
            if (ringtone.isEmpty()) {
                return Settings.System.DEFAULT_ALARM_ALERT_URI;
            }
        }
        return Uri.parse(ringtone);
    }

    @Override
    protected Notification getForegroundNotification() {
        Notification returnNote;

        String title = (getRingingObject() == null || getRingingObject().label().isEmpty())
                ? getString(R.string.alarm)
                : getRingingObject().label();

        String channelId = getString(R.string.channel_ID_alarm_now);

        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 10 bans GUI starting from background tasks, need to drive it via notification and servce.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelName = getString(R.string.channel_name_alarm_now);
            notificationManager.deleteNotificationChannel(channelId);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
            mChannel.setSound(null, null);

            notificationManager.createNotificationChannel(mChannel);
        }

        Intent intent = new Intent(this, AlarmActivity.class);
        if (getRingingObject() != null) {
            intent.putExtra(AlarmActivity.EXTRA_RINGING_OBJECT, ParcelableUtil.marshall(getRingingObject()));
        }
        int flag = FLAG_CANCEL_CURRENT;

        int alarm_id = -1;
        if (getRingingObject() != null) {
            alarm_id = getRingingObject().getIntId();
        }

        final PendingIntent alarmIntent = getActivity(this, alarm_id, intent, flag);

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

        Utils.setThemeFromPreference(this);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_alarm_24dp)
                        .setContentTitle(title)
                        .setContentText(TimeFormatUtils.formatTime(this, System.currentTimeMillis()))
                        .setColor(getColorFromThemeAttr(this, R.attr.colorPrimary))
                        .setColorized(true)
                        .addAction(R.drawable.ic_snooze_24dp,
                                getString(R.string.snooze),
                                getPendingIntent(ACTION_SNOOZE, alarm_id))
                        .addAction(R.drawable.ic_dismiss_alarm_24dp,
                                getString(R.string.dismiss),
                                getPendingIntent(ACTION_DISMISS, alarm_id))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_ALARM);

        if (getRingingObject() != null) {
            // Use a full-screen intent only for the highest-priority alerts where you
            // have an associated activity that you would like to launch after the user
            // interacts with the notification. Also, if your app targets Android 10
            // or higher, you need to request the USE_FULL_SCREEN_INTENT permission in
            // order for the platform to invoke this notification.
            notificationBuilder.setFullScreenIntent(alarmIntent, true);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(channelId);
        }
        returnNote = notificationBuilder.build();

        /*
        } else {

            // Pre android 10 implementation
            Notification.Builder builder = new Notification.Builder(this)
                        // Required contents
                        .setSmallIcon(R.drawable.ic_alarm_24dp)
                        .setContentTitle(title)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setPriority(Notification.PRIORITY_HIGH)
                    .setContentText(TimeFormatUtils.formatTime(this, System.currentTimeMillis()))
                        .addAction(R.drawable.ic_snooze_24dp,
                                getString(R.string.snooze),
                                getPendingIntent(ACTION_SNOOZE, getRingingObject().getIntId()))
                        .addAction(R.drawable.ic_dismiss_alarm_24dp,
                                getString(R.string.dismiss),
                                getPendingIntent(ACTION_DISMISS, getRingingObject().getIntId()))
                        .setFullScreenIntent(alarmIntent, true);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                builder.setChannelId(channelId);
            }
            returnNote = builder.build();

        }
        */


        return returnNote;
    }

    @Override
    protected boolean doesVibrate() {
        return getRingingObject().vibrates();
    }

    @Override
    protected int minutesToAutoSilence() {
        return AlarmPreferences.minutesToSilenceAfter(this);
    }

    @Override
    protected Parcelable.Creator<Alarm> getParcelableCreator() {
        return Alarm.CREATOR;
    }
}