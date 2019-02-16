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

package uk.mach91.autoalarm.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import uk.mach91.autoalarm.R;
import uk.mach91.autoalarm.alarms.misc.AlarmPreferences;

import java.util.concurrent.TimeUnit;

import java.util.Date;

/**
 * Created by Phillip Hsu on 6/6/2016.
 */
public class DurationUtils {
    public static final int DAYS = 0;
    public static final int HOURS = 1;
    public static final int MINUTES = 2;
    public static final int SECONDS = 3;
    public static final int MILLIS = 4;

    /** Return a string representing the duration, formatted in hours and minutes.
     * TODO: Need to adapt this to represent all time fields eventually
     * TODO: Since this is primarirly used for alarm set toasts, you should make different methods for
     * different use cases. E.g. Timer's duration should have its own method.
     * TODO: Then, rename this method to something about alarm toasts. */
    public static String toString(Context context, long millis, boolean abbreviate) {
        long[] fields = breakdown(millis);
        long numDays = fields[DAYS];
        long numHours = fields[HOURS];
        long numMins = fields[MINUTES];
        long numSecs = fields[SECONDS]; // only considered for rounding of minutes
        if (numSecs >= 31) {
            numMins++;
            numSecs = 0; // Not totally necessary since it won't be considered any more
            if (numMins == 60) {
                numHours++;
                numMins = 0;
                if (numHours == 24) {
                    numDays++;
                    numHours = 0;
                }
            }
        }

        int res;
        if (abbreviate) {
            res = getAbbreviatedStringRes(numDays, numHours, numMins);
        } else {
            res = getStringRes(numDays, numHours, numMins);
        }
        
        return context.getString(res, numDays, numHours, numMins);
    }

    /**
     * Equivalent to
     * {@link #breakdown(long, TimeUnit, boolean)
     * breakdown(millis, TimeUnit.MILLISECONDS, true)},
     * which rounds milliseconds. Callers who use this are probably not
     * concerned about displaying the milliseconds value.
     */
    public static long[] breakdown(long millis) {
        return breakdown(millis, TimeUnit.MILLISECONDS, true);
    }

    /**
     * Equivalent to
     * {@link #breakdown(long, TimeUnit, boolean) breakdown(t, unit, false)},
     * i.e. does not round milliseconds.
     */
    public static long[] breakdown(long t, @NonNull TimeUnit unit) {
        return breakdown(t, unit, false);
    }

    /**
     * Returns a breakdown of a given time into its values
     * in hours, minutes, seconds and milliseconds.
     * @param t the time to break down
     * @param unit the {@link TimeUnit} the given time is expressed in
     * @param roundMillis whether rounding of milliseconds is desired
     * @return a {@code long[]} of the values in hours, minutes, seconds
     *         and milliseconds in that order
     */
    public static long[] breakdown(long t, @NonNull TimeUnit unit, boolean roundMillis) {
        long days = unit.toDays(t);
        long hours = unit.toHours(t) % 24;
        long minutes = unit.toMinutes(t) % 60;
        long seconds = unit.toSeconds(t) % 60;
        long msecs = unit.toMillis(t) % 1000;
        if (roundMillis) {
            if (msecs >= 500) {
                seconds++;
                msecs = 0;
                if (seconds == 60) {
                    minutes++;
                    seconds = 0;
                    if (minutes == 60) {
                        hours++;
                        minutes = 0;
                        if (hours == 24) {
                            days++;
                            hours = 0;
                        }
                    }
                }
            }
        }
        return new long[] { days, hours, minutes, seconds, msecs };
    }

    /**
     * Works out if for the provided time there is a holiday marked in teh calendar.
     * @param context the current context
     * @param alarmAt   the time the alarm is going to rint
     * @return true is there is a holiday at the time specified, otherwise false.
     */
    public static boolean isOnHoliday(Context context, long alarmAt) {
        boolean cancelDueToHoliday = false;

        if (AlarmPreferences.cancelAlarmHoliday(context) &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                //!AlarmPreferences.cancelAlarmHolidayLabel(context).isEmpty() &&
                ((!AlarmPreferences.cancelAlarmHolidayTitle(context).isEmpty() && !AlarmPreferences.cancelAlarmHolidayCalendar(context).isEmpty()) ||
                        !AlarmPreferences.cancelAlarmBankHolidayCalendar(context).isEmpty())){

            Uri uri = CalendarContract.Events.CONTENT_URI;

            String[] projection =
                    {
                            "_id",
                            CalendarContract.Events.TITLE,
                            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
                            CalendarContract.Events.ACCOUNT_NAME,
                            CalendarContract.Events.DESCRIPTION,
                            CalendarContract.Events.AVAILABILITY
                    };

            String selection = CalendarContract.Events.DTSTART + " <= ? AND "
                    + CalendarContract.Events.DTEND + ">= ? ";

            String[] selectionArgs = new String[2];
            selectionArgs[0] = String.valueOf(alarmAt);
            selectionArgs[1] = String.valueOf(alarmAt);

            Cursor cur = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);

            while (cur.moveToNext() && !cancelDueToHoliday) {
                String title = cur.getString(cur.getColumnIndex(CalendarContract.Events.TITLE));
                String calName = cur.getString(cur.getColumnIndex(CalendarContract.Events.CALENDAR_DISPLAY_NAME));
                String accName = cur.getString(cur.getColumnIndex(CalendarContract.Events.ACCOUNT_NAME));

                String combinedName = calName.toLowerCase() + " ("  + accName.toLowerCase() + ")";
                if ((combinedName.equals(AlarmPreferences.cancelAlarmHolidayCalendar(context).toLowerCase()) &&
                        title.toLowerCase().contains(AlarmPreferences.cancelAlarmHolidayTitle(context).toLowerCase())) ||
                        combinedName.toLowerCase().equals(AlarmPreferences.cancelAlarmBankHolidayCalendar(context).toLowerCase())) {
                    cancelDueToHoliday = true;
                }
            }
            cur.close();
        }
        return cancelDueToHoliday;
    }

    public static String nextCalendatEvent(Context context, long startAr, long endAt) {
        String nextCalendarEvent = "";
        long nextCalendarEventTime = -1;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {

            Uri uri = CalendarContract.Events.CONTENT_URI;

            String[] projection =
                    {
                            "_id",
                            CalendarContract.Events.TITLE,
                            CalendarContract.Events.DTSTART,
                            CalendarContract.Events.DTEND
                    };

            String selection = CalendarContract.Events.DTSTART + " >= ? AND "
                    + CalendarContract.Events.DTSTART + "<= ? ";

            String[] selectionArgs = new String[2];
            selectionArgs[0] = String.valueOf(startAr);
            selectionArgs[1] = String.valueOf(endAt);

            Cursor cur = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);

            while (cur.moveToNext()) {
                String title = cur.getString(cur.getColumnIndex(CalendarContract.Events.TITLE));
                String alarmStartTimeStr = cur.getString(cur.getColumnIndex(CalendarContract.Events.DTSTART));
                String alarmEndTimeStr = cur.getString(cur.getColumnIndex(CalendarContract.Events.DTEND));
                long alarmStartTime = -1;
                long alarmEndTime = -1;
                try {
                    alarmStartTime = Long.parseLong(alarmStartTimeStr);
                    alarmEndTime = Long.parseLong(alarmEndTimeStr);
                } catch (NumberFormatException nfe) {
                }

                if (alarmStartTime != -1 && alarmEndTime != -1 &&
                        (nextCalendarEventTime == -1 || alarmStartTime < nextCalendarEventTime)) {
                    DateFormat df = new DateFormat();
                    Date alarmStartDate = new Date(alarmStartTime);
                    Date alarmEndDate = new Date(alarmEndTime);
                    String timneText = "";

                    timneText += "Next event: " + df.format("HH:mm", alarmStartDate) + " - " + df.format("HH:mm", alarmEndDate) + "\n";
                    if (title.length() > 23) {
                        title = title.substring(0, 20) + "...";
                    }

                    nextCalendarEventTime = alarmStartTime;
                    nextCalendarEvent = timneText + title;
                }
            }
            cur.close();
        }

        return nextCalendarEvent;
    }

    @StringRes
    private static int getStringRes(long numDays, long numHours, long numMins) {
        int res;
        if (numDays == 0) {
            if (numHours == 0) {
                if (numMins == 0) {
                    res = R.string.less_than_one_minute;
                } else if (numMins == 1) {
                    res = R.string.minute;
                } else {
                    res = R.string.minutes;
                }
            } else if (numHours == 1) {
                if (numMins == 0) {
                    res = R.string.hour;
                } else if (numMins == 1) {
                    res = R.string.hour_and_minute;
                } else {
                    res = R.string.hour_and_minutes;
                }
            } else {
                if (numMins == 0) {
                    res = R.string.hours;
                } else if (numMins == 1) {
                    res = R.string.hours_and_minute;
                } else {
                    res = R.string.hours_and_minutes;
                }
            }
        } else if (numDays == 1) {
            if (numHours == 0) {
                if (numMins == 0) {
                    res = R.string.day;
                } else if (numMins == 1) {
                    res = R.string.day_and_minute;
                } else {
                    res = R.string.day_and_minutes;
                }
            } else if (numHours == 1) {
                if (numMins == 0) {
                    res = R.string.day_and_hour;
                } else if (numMins == 1) {
                    res = R.string.day_hour_and_minute;
                } else {
                    res = R.string.day_hour_and_minutes;
                }
            } else {
                if (numMins == 0) {
                    res = R.string.day_and_hours;
                } else if (numMins == 1) {
                    res = R.string.day_hours_and_minute;
                } else {
                    res = R.string.day_hours_and_minutes;
                }
            }
        } else {
            if (numHours == 0) {
                if (numMins == 0) {
                    res = R.string.days;
                } else if (numMins == 1) {
                    res = R.string.days_and_minute;
                } else {
                    res = R.string.days_and_minutes;
                }
            } else if (numHours == 1) {
                if (numMins == 0) {
                    res = R.string.days_and_hour;
                } else if (numMins == 1) {
                    res = R.string.days_hour_and_minute;
                } else {
                    res = R.string.days_hour_and_minutes;
                }
            } else {
                if (numMins == 0) {
                    res = R.string.days_and_hours;
                } else if (numMins == 1) {
                    res = R.string.days_hours_and_minute;
                } else {
                    res = R.string.days_hours_and_minutes;
                }
            }
        }
        return res;
    }

    @StringRes
    private static int getAbbreviatedStringRes(long numDays, long numHours, long numMins) {
        int res;
        if (numDays == 0) {
            if (numHours == 0) {
                if (numMins == 0) {
                    res = R.string.abbrev_less_than_one_minute;
                } else {
                    res = R.string.abbrev_minutes;
                }
            } else {
                if (numMins == 0) {
                    res = R.string.abbrev_hours;
                } else {
                    res = R.string.abbrev_hours_and_minutes;
                }
            }
        } else {
            if (numHours == 0) {
                if (numMins == 0) {
                    res = R.string.abbrev_days;
                } else {
                    res = R.string.abbrev_days_and_minutes;
                }
            } else {
                if (numMins == 0) {
                    res = R.string.abbrev_days_and_hours;
                } else {
                    res = R.string.abbrev_days_hours_and_minutes;
                }
            }
        }
        return res;
    }


}
