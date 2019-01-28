/*
 * Copyright 2017 Chris Allenby
 *
 * This file is part of Alarmation.
 *
 * Alarmation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alarmation is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alarmation.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package uk.mach91.autoalarm.util;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import uk.mach91.autoalarm.R;
import uk.mach91.autoalarm.alarms.Alarm;
import uk.mach91.autoalarm.alarms.data.AlarmCursor;
import uk.mach91.autoalarm.alarms.data.AlarmsTableManager;
import uk.mach91.autoalarm.alarms.misc.AlarmPreferences;

import static android.content.Context.SENSOR_SERVICE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static java.lang.Math.abs;

/**
 * Created by Chris Allenby on 23/09/2018.
 */

public class ScreenSaverUtils extends FrameLayout implements SensorEventListener {
    public final static long SHOW_ALARM_MILLS = 10 * 60 * 60 * 1000L;
    public final static int MOVE_TIME_FREQUENCY = 30 * 1000;

    private Handler mMoveTimeHandler;
    private Runnable mMoveTimeRunnable;

    private Random random;

    private boolean firstDrawDone = false;

    private boolean mShowDate = true;

    SensorManager mSensorManager;

    TextView mTextView;

    int mScreenRotation = 0;

    public ScreenSaverUtils(Context context) {
        this(context, null);
    }

    public ScreenSaverUtils(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScreenSaverUtils(Context context, AttributeSet attrs, int flags) {
        super(context, attrs, flags);
    }

    public void addTimeView (Context context, View.OnTouchListener onTouchListener) {
        if (mTextView == null) {
            setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

            if (onTouchListener != null) {
                setOnTouchListener(onTouchListener);
            }

            mTextView = new TextView(context);

            int textSize = AlarmPreferences.screensaverTextSize(context);

            mShowDate = AlarmPreferences.screensaverShowDate(context);

            float textSizeFloat = 5 + textSize;

            mTextView.setTextSize(TypedValue.COMPLEX_UNIT_MM, textSizeFloat);

            mTextView.setText(" ");
//            mTextView.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
            mTextView.setTypeface(Typeface.create("sans-serif-lite", Typeface.NORMAL));
            mTextView.setGravity(Gravity.CENTER);
            //int textBrightness = AlarmPreferences.screensaverTextColour(context);

            //float alpha = 255 * (textBrightness + 2) / 16;

//            mTextView.setTextColor(Color.parseColor("#666666"));
            //mTextView.setTextColor(Color.argb (textBrightness, 255, 255, 255));
            mTextView.setTextColor(AlarmPreferences.screensaverTextColour(context));

            addView(mTextView, new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        }
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
   }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int _DATA_X = 0;
        int _DATA_Y = 1;
        int _DATA_Z = 2;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] values = event.values;
            float X = -values[_DATA_X];
            float Y = -values[_DATA_Y];
            float Z = -values[_DATA_Z];
            float magnitude = X*X + Y*Y;
            if (magnitude * 4 >= Z*Z) {
                float OneEightyOverPi = 57.29577957855f;
                float angle = (float) Math.atan2(-Y, X) * OneEightyOverPi;
                int orientation = 90 - (int) Math.round(angle);
                // normalize to 0 - 359 range
                orientation = compensateOrientation(orientation);
                while (orientation >= 360) {
                    orientation -= 360;
                }
                while (orientation < 0) {
                    orientation += 360;
                }
                orientation = Math.round((float) orientation / 90) * 90;
                if (orientation >= 360) {
                    orientation = 0;
                }
                if (orientation != mScreenRotation) {
                    mScreenRotation = orientation;
                    //firstDrawDone = false;
                    updateTimeRunnable();
                    updateTime();
                }
            }
        }
    }

    private int compensateOrientation(int degrees){
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        switch(display.getRotation()){
            case(Surface.ROTATION_270):
                return degrees + 270;
            case(Surface.ROTATION_180):
                return degrees + 180;
            case(Surface.ROTATION_90):
                return degrees + 90;
            default:
                return degrees;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Start the animation when dreaming starts.
     */
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateTimeRunnable();

        mSensorManager = (SensorManager) getContext().getSystemService(SENSOR_SERVICE);
        Sensor orientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Stop animation when dreaming stops.
     */
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mMoveTimeHandler.removeCallbacks(mMoveTimeRunnable);
        if(mSensorManager != null) mSensorManager.unregisterListener(this);
    }

    /**
     * Whenever a view is added, place it randomly.
     */
    @Override
    public void addView(View v, ViewGroup.LayoutParams lp) {
        super.addView(v, lp);
        setupView(v);
    }

    /**
     * Bouncing view setup: random placement, random velocity.
     */
    private void setupView(View v) {
        final TextView tv = (TextView) v;
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);

        if (!firstDrawDone) {
            fadeOut.setDuration(1);
        } else {
            fadeOut.setDuration(1000);
        }
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

                String nightClockText = "";

                Date now = new Date();

                int dateStartIndex = -1;
                int dateEndIndex = -1;

                if (mShowDate) {
                    DateFormat df = new DateFormat();
                    df.format("EEEE, dd MMMM", now);
                    dateStartIndex = nightClockText.length();
                    nightClockText += df.format("EEEE, dd MMMM", now) + "\n";
                    dateEndIndex = nightClockText.length();
                }

                nightClockText += DateFormat.getTimeFormat(getContext()).format(now);

                SpannableString ss1;

                long nextTimeAt= -1;
                AlarmCursor cursor = new AlarmsTableManager(getContext()).queryEnabledAlarms();
                while (cursor.moveToNext()) {
                    Alarm alarm = cursor.getItem();
                    if (!alarm.isEnabled()) {
                        throw new IllegalStateException(
                                "queryEnabledAlarms() returned alarm(s) that aren't enabled");
                    }

                    if (!alarm.isIgnoringUpcomingRingTime()) {
                        long thisTimeAt = alarm.ringsAt();

                        boolean cancelDueToHoliday = false;

                        //if (alarm.label().toLowerCase().equals(AlarmPreferences.cancelAlarmHolidayLabel(getContext()).toLowerCase())) {
                        if (alarm.skip_holiday()) {
                                cancelDueToHoliday = DurationUtils.isOnHoliday(getContext(), thisTimeAt);
                        }

                        if (!cancelDueToHoliday && (nextTimeAt == -1 || nextTimeAt > thisTimeAt)) {
                            nextTimeAt = thisTimeAt;
                        }
                    }
                }
                cursor.close();

                int alarmTextStartIndex = -1;
                int alarmTextEndIndex = -1;
                int alarmTimeEndIndex = -1;

                if (nextTimeAt != -1) {
                    long tillNextAlarm = nextTimeAt - now.getTime();
                    //tillNextAlarm = myTillNextAlarm;
                    Date nextAlarm = new Date(nextTimeAt);

                    if (tillNextAlarm < SHOW_ALARM_MILLS) {
                        alarmTextStartIndex = nightClockText.length();
                        nightClockText += "\n" + getContext().getString(R.string.alarm_screensaver_alarm_in) + " ";
                        alarmTextEndIndex = nightClockText.length();
                        long hours = TimeUnit.MILLISECONDS.toHours(tillNextAlarm);
                        long mins = TimeUnit.MILLISECONDS.toMinutes(tillNextAlarm) % TimeUnit.HOURS.toMinutes(1);
                        long secs = TimeUnit.MILLISECONDS.toSeconds(tillNextAlarm) % TimeUnit.MINUTES.toSeconds(1);
                        if (secs > 0) {
                            mins++;
                            if (mins >= 60) {
                                hours++;
                                mins = 0;
                            }
                        }

                        String hm = String.format(Locale.getDefault(), "%02d:%02d", hours, mins);
                        nightClockText += hm;
                        nightClockText += "\n(@ ";
                        nightClockText += DateFormat.getTimeFormat(getContext()).format(nextAlarm);
                        nightClockText += ")";

                        alarmTimeEndIndex = nightClockText.length();
                    }
                }

                ss1 = new SpannableString(nightClockText);

                if (dateStartIndex >= 0) {
                    ss1.setSpan(new RelativeSizeSpan(0.25f), dateStartIndex, dateEndIndex, 0); // set size
                }
                if (alarmTextStartIndex >= 0) {
                    ss1.setSpan(new RelativeSizeSpan(0.25f), alarmTextStartIndex, alarmTextEndIndex, 0); // set size
                    ss1.setSpan(new RelativeSizeSpan(0.35f), alarmTextEndIndex, alarmTimeEndIndex, 0); // set size
                }


                DisplayMetrics displayMetrics = new DisplayMetrics();
                WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                wm.getDefaultDisplay().getMetrics(displayMetrics);

                int winHeight = displayMetrics.heightPixels;
                int winWidth = displayMetrics.widthPixels;

                tv.setText(ss1);
                tv.setRotation(-mScreenRotation);

                tv.measure(0, 0);

                int textWidth = tv.getMeasuredWidth();
                int textHeight = tv.getMeasuredHeight();

                if (mScreenRotation == 90 || mScreenRotation == 270) {
                    int temp = textHeight;
                    textHeight = textWidth;
                    textWidth = temp;
                }


                int x;
                int y;

                if (!firstDrawDone) {
                    x = (winWidth - textWidth) / 2;
                    y = (winHeight - textHeight) / 2;
                    firstDrawDone = true;
                } else {
                    if (random == null) {
                        random = new Random();
                        random.setSeed(now.getTime());
                    }
                    int randomX = random.nextInt(winWidth - textWidth);
                    int randomY = random.nextInt(winHeight - textHeight);
                    x = randomX;
                    y = randomY;
                }

                if (mScreenRotation == 90 || mScreenRotation == 270) {
                    x-= abs((textWidth - textHeight) / 2);
                    y+= abs((textWidth - textHeight) / 2);
                }

                tv.setX(x);
                tv.setY(y);


                AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(1000);

                tv.startAnimation(fadeIn);
            }
        });
        v.startAnimation(fadeOut);
    }

    protected void updateTimeRunnable() {
        if(mMoveTimeHandler != null) mMoveTimeHandler.removeCallbacks(mMoveTimeRunnable);

        mMoveTimeHandler = new Handler();
        mMoveTimeRunnable = new Runnable() {
            public void run() {
                updateTime();
                mMoveTimeHandler.postDelayed(this, MOVE_TIME_FREQUENCY);
            }
        };
        mMoveTimeHandler.postDelayed(mMoveTimeRunnable, MOVE_TIME_FREQUENCY);

    }

    protected void updateTime() {
        if (getChildCount() > 0) {
            final View view = getChildAt(0);
            setupView (view);
        }
    }
}
