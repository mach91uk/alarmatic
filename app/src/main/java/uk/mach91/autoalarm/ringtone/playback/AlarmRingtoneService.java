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
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import uk.mach91.autoalarm.alarms.misc.AlarmController;
import uk.mach91.autoalarm.alarms.misc.AlarmPreferences;
import uk.mach91.autoalarm.util.DurationUtils;
import uk.mach91.autoalarm.util.ParcelableUtil;
import uk.mach91.autoalarm.util.TimeFormatUtils;
import uk.mach91.autoalarm.R;
import uk.mach91.autoalarm.alarms.Alarm;

import static uk.mach91.autoalarm.util.TimeFormatUtils.formatTime;

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

        if (mCancelDueToHoliday) {
            final byte[] bytes = intent.getByteArrayExtra(EXTRA_RINGING_OBJECT);
            if (bytes != null) {
                boolean skip_holiday = ParcelableUtil.unmarshall(bytes, getParcelableCreator()).skip_holiday();
                //if (lable.toLowerCase().equals(AlarmPreferences.cancelAlarmHolidayLabel(this).toLowerCase())) {
                if (skip_holiday) {
                    AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
                    mCalendarRunnable = new CalendarRunnable();
                    Handler mCalendarHandler = new Handler();
                    audio.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
                    mCalendarHandler.postDelayed(mCalendarRunnable, 900);
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
        return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    public void onCreate() {
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
                    Log.i("Compass MainActivity", "Registerered for ORIENTATION Sensor");
                } else {
                    Log.e("Compass MainActivity", "Registerered for ORIENTATION Sensor");
                    Toast.makeText(this, "ORIENTATION Sensor not found",
                            Toast.LENGTH_LONG).show();
                }
            } else if (mFlipShakeAction > 0) {
                mShakeSensor = mSensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (mShakeSensor != null) {
                    mSensorService.registerListener(mySensorEventListener, mShakeSensor,
                            SensorManager.SENSOR_DELAY_NORMAL);
                    Log.i("Compass MainActivity", "Registerered for ORIENTATION Sensor");
                } else {
                    Log.e("Compass MainActivity", "Registerered for ORIENTATION Sensor");
                    Toast.makeText(this, "ACCELEROMETER Sensor not found",
                            Toast.LENGTH_LONG).show();
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
                            } else if (mFlipAction == AlarmPreferences.FLIP_ACTION_DISMISS){
                                mAlarmController.cancelAlarm(getRingingObject(), false, true);
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
                                } else  if (mFlipAction == AlarmPreferences.FLIP_ACTION_DISMISS) {
                                    mAlarmController.cancelAlarm(getRingingObject(), false, true);
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
        mAlarmController.cancelAlarm(getRingingObject(), false, true);
    }

    @Override
    protected Uri getRingtoneUri() {
        String ringtone = getRingingObject().ringtone();
        // can't be null...
        if (ringtone.isEmpty()) {
            return Settings.System.DEFAULT_ALARM_ALERT_URI;
        }
        return Uri.parse(ringtone);
    }

    @Override
    protected Notification getForegroundNotification() {
        String title = getRingingObject().label().isEmpty()
                ? getString(R.string.alarm)
                : getRingingObject().label();

        String channelId = getString(R.string.channel_ID_alarm);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelName = getString(R.string.channel_name_alarm);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
            mChannel.setSound(null, null);

            final NotificationManager nm = (NotificationManager)
                    this.getSystemService(Context.NOTIFICATION_SERVICE);

            nm.createNotificationChannel(mChannel);
        }
        Notification.Builder builder = new Notification.Builder(this)
                    // Required contents
                    .setSmallIcon(R.drawable.ic_alarm_24dp)
                    .setContentTitle(title)
                    .setContentText(TimeFormatUtils.formatTime(this, System.currentTimeMillis()))
                    .addAction(R.drawable.ic_snooze_24dp,
                            getString(R.string.snooze),
                            getPendingIntent(ACTION_SNOOZE, getRingingObject().getIntId()))
                    .addAction(R.drawable.ic_dismiss_alarm_24dp,
                            getString(R.string.dismiss),
                            getPendingIntent(ACTION_DISMISS, getRingingObject().getIntId()));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder.setChannelId(channelId);
        }

        return builder.build();
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