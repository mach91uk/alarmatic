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

package uk.mach91.autoalarm.alarms.misc;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;

import uk.mach91.autoalarm.R;

/**
 * Created by Phillip Hsu on 6/3/2016.
 *
 * Utilities for reading alarm preferences.
 */
public final class AlarmPreferences {
    private static final String TAG = "AlarmPreferences";

    private AlarmPreferences() {}

    public static int snoozeDuration(Context c) {
        return readPreference(c, R.string.key_snooze_duration, 5);
    }

    // TODO: Consider renaming to hoursToNotifyInAdvance()
    public static int hoursBeforeUpcoming(Context c) {
        return readPreference(c, R.string.key_notify_me_of_upcoming_alarms, 2);
    }

    public static int flipAction(Context c) {
        return readPreference(c, R.string.key_flip_action, 0);
    }

    public static int fadeVolume(Context c) {
        return readPreference(c, R.string.key_fade_volume, 1);
    }

    public static int longClick(Context c) {
        return readPreference(c, R.string.key_long_click, 0);
    }


    public static int minutesToSilenceAfter(Context c) {
        return readPreference(c, R.string.key_silence_after, 5);
    }

    public static int firstDayOfWeek(Context c) {
        return readPreference(c, R.string.key_first_day_of_week, 1 /* Monday */);
    }

 //   public static int screensaverBrightness(Context c) {
//       return PreferenceManager.getDefaultSharedPreferences(c).getInt(c.getString(R.string.key_screensaver_text_brightness), 95);
//    }

    public static int screensaverTextColour(Context c) {
       return PreferenceManager.getDefaultSharedPreferences(c).getInt(c.getString(R.string.key_screensaver_colour), 0xffff0000);
    }

    public static int screensaverTextSize(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getInt(c.getString(R.string.key_screensaver_size), 10);
    }

    public static boolean screensaverNightMode(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean( c.getString(R.string.key_screensaver_dim), true);
    }

    public static boolean mustDismissTwice(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean( c.getString(R.string.key_dismiss_twice), false);
    }

    public static boolean screensaverShowDate(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean( c.getString(R.string.key_screensaver_date), true);
    }

    public static boolean screensaverShowNextAlarm(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean( c.getString(R.string.key_screensaver_nextalarm), true);
    }

    public static boolean cancelAlarmHoliday(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean( c.getString(R.string.key_cancel_alarm_holiday), c.getResources().getBoolean(R.bool.default_cancel_alarm_holiday));
    }

    public static boolean notifyOfTimeZoneChange(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean( c.getString(R.string.key_notify_time_zone_change), c.getResources().getBoolean(R.bool.default_notify_time_zone_change));
    }

    public static String cancelAlarmHolidayCalendar(Context c) {
        String value = PreferenceManager.getDefaultSharedPreferences(c).getString(c.getString(R.string.key_cancel_alarm_calendar), c.getString(R.string.default_cancel_alarm_calendar));
        return null == value ? "" : value;
    }

    public static String cancelAlarmBankHolidayCalendar(Context c) {
        String value = PreferenceManager.getDefaultSharedPreferences(c).getString(c.getString(R.string.key_cancel_alarm_bank_calendar), c.getString(R.string.default_cancel_alarm_bank_calendar));
        return null == value ? "" : value;
    }

//    public static String cancelAlarmHolidayLabel(Context c) {
//        String value = PreferenceManager.getDefaultSharedPreferences(c).getString(c.getString(R.string.key_cancel_alarm_label), c.getString(R.string.default_cancel_alarm_label));
//        return null == value ? "" : value.equals(c.getString(R.string.disabled)) ? "" : value;
//    }

    public static String cancelAlarmHolidayTitle(Context c) {
        String value = PreferenceManager.getDefaultSharedPreferences(c).getString(c.getString(R.string.key_cancel_alarm_title), c.getString(R.string.default_cancel_alarm_title));
        return null == value ? "" : value.equals(c.getString(R.string.disabled)) ? "" : value;
    }

    public static int readPreference(Context c, @StringRes int key, int defaultValue) {
        String value = PreferenceManager.getDefaultSharedPreferences(c).getString(c.getString(key), null);
        return null == value ? defaultValue : Integer.parseInt(value);
    }
}
