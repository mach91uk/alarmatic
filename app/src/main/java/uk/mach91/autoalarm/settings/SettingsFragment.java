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

package uk.mach91.autoalarm.settings;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.provider.CalendarContract;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import uk.mach91.autoalarm.MainActivity;
import uk.mach91.autoalarm.R;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int  MY_PERMISSIONS_REQUEST_READ_CALENDAR = 10;

    public SettingsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        addPreferencesFromResource(R.xml.preferences);

        // Set ringtone summary
        setSummary(getPreferenceScreen().getSharedPreferences(), getString(R.string.key_timer_ringtone));
        setSummary(getPreferenceScreen().getSharedPreferences(), getString(R.string.key_cancel_alarm_calendar));
        setSummary(getPreferenceScreen().getSharedPreferences(), getString(R.string.key_cancel_alarm_title), getString(R.string.disabled));
        setSummary(getPreferenceScreen().getSharedPreferences(), getString(R.string.key_cancel_alarm_bank_calendar));
//        setSummary(getPreferenceScreen().getSharedPreferences(), getString(R.string.key_cancel_alarm_label), getString(R.string.disabled));

        SwitchPreference p = (SwitchPreference) findPreference(getString(R.string.key_cancel_alarm_holiday));

        SeekBarPreference sbp = (SeekBarPreference) findPreference(getString(R.string.key_screensaver_size));

        sbp.setMaxValue(20);
        sbp.setIncrementSize(1);

        Preference pref = findPreference(getString(R.string.key_cancel_alarm_holiday));
        if (ContextCompat.checkSelfPermission(pref.getContext(), Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            p.setChecked(false);
        }

        EditTextPreference p2 = (EditTextPreference) findPreference(getString(R.string.key_cancel_alarm_title));
        final ListPreference p3 = (ListPreference) findPreference(getString(R.string.key_cancel_alarm_calendar));
        final ListPreference p4 = (ListPreference) findPreference(getString(R.string.key_cancel_alarm_bank_calendar));
//        EditTextPreference p5 = (EditTextPreference) findPreference(getString(R.string.key_cancel_alarm_label));

        setCalendatListPreferenceData(p4);

        p4.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                setCalendatListPreferenceData(p4);
                return false;
            }
        });

        setCalendatListPreferenceData(p3);

        p3.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                setCalendatListPreferenceData(p3);
                return false;
            }
        });



        if (p.isChecked()) {
            p2.setEnabled(true);
            p3.setEnabled(true);
            p4.setEnabled(true);
//            p5.setEnabled(true);
        }

        findPreference(getString(R.string.key_alarm_volume))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AudioManager am = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
                am.adjustStreamVolume(
                        AudioManager.STREAM_ALARM,
                        AudioManager.ADJUST_SAME, // no adjustment
                        AudioManager.FLAG_SHOW_UI); // show the volume toast
                return true;
            }
        });


        PreferenceScreen screen = getPreferenceScreen();
        Preference removeCategory;

        SettingsActivity setAct = ((SettingsActivity) getActivity());
        int mode = setAct.mMode;

        if (!(mode == setAct.MODE_STANDARD || mode == setAct.MODE_HOLIDAY)) {
            removeCategory = findPreference(getString(R.string.key_category_holiday));
            screen.removePreference(removeCategory);
        }
        if (!(mode == setAct.MODE_STANDARD || mode == setAct.MODE_SCREENSAVER)) {
            removeCategory = findPreference(getString(R.string.key_category_screensaver));
            screen.removePreference(removeCategory);
        }
        if (mode != setAct.MODE_STANDARD) {
            removeCategory = findPreference(getString(R.string.key_category_alarms_snooze));
            screen.removePreference(removeCategory);
            removeCategory = findPreference(getString(R.string.key_category_alarms));
            screen.removePreference(removeCategory);
            removeCategory = findPreference(getString(R.string.key_category_general));
            screen.removePreference(removeCategory);
        }
    }


    protected void setCalendatListPreferenceData(ListPreference lp) {
        if (lp.getEntries() == null &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||(ContextCompat.checkSelfPermission(this.getContext(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED))) {
            Cursor cursor;

            List<String> calendarList = new ArrayList<String>();
            List<String> calendarListValues = new ArrayList<String>();

            if (android.os.Build.VERSION.SDK_INT <= 7) {
                cursor = getActivity().getContentResolver().query(Uri.parse("content://calendar/calendars"), new String[]{"_id", "displayName"}, null,
                        null, null);

            } else if (android.os.Build.VERSION.SDK_INT <= 14) {
                cursor = getActivity().getContentResolver().query(Uri.parse("content://com.android.calendar/calendars"),
                        new String[]{"_id", "displayName"}, null, null, null);

            } else {
                Uri uri = CalendarContract.Calendars.CONTENT_URI;
                cursor = getActivity().getContentResolver().query(uri,
                        new String[]{CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME}, null, null, CalendarContract.Calendars.ACCOUNT_NAME + " ASC, " + CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC");

            }

            // Get calendars name
            //Log.i("Cursor count " + cursor.getCount());
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    calendarList.add(cursor.getString(1) + " (" +cursor.getString(2) + ")");
                    //calendarList.add(cursor.getString(1));
                    calendarListValues.add(cursor.getString(1));
                    //Log.i("Calendar Name : " + calendarNames[i]);
                    cursor.moveToNext();
                }
            } else {
                // Log.e("No calendar found in the device");
            }
            cursor.close();

            calendarList.add(0, getString(R.string.disabled));
            CharSequence[] entries = calendarList.toArray(new CharSequence[calendarList.size()]);
            calendarList.remove(0);
            calendarList.add(0, "");
            CharSequence[] entryValues = calendarList.toArray(new CharSequence[calendarListValues.size()]);
            lp.setEntries(entries);
            lp.setDefaultValue("");
            lp.setEntryValues(entryValues);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setSummary(sharedPreferences, key);

        Preference pref = findPreference(key);

        if (pref instanceof SwitchPreference) {
            if (key.equals(getString(R.string.key_cancel_alarm_holiday))){
                boolean val = sharedPreferences.getBoolean(key, false);
                EditTextPreference p2 = (EditTextPreference) findPreference(getString(R.string.key_cancel_alarm_title));
                ListPreference p3 = (ListPreference) findPreference(getString(R.string.key_cancel_alarm_calendar));
                ListPreference p4 = (ListPreference) findPreference(getString(R.string.key_cancel_alarm_bank_calendar));
//                EditTextPreference p5 = (EditTextPreference) findPreference(getString(R.string.key_cancel_alarm_label));

                p2.setEnabled(false);
                p3.setEnabled(false);
                p4.setEnabled(false);
//                p5.setEnabled(false);

                if (val) {
                    SwitchPreference p = (SwitchPreference) findPreference(key);

                    if (ContextCompat.checkSelfPermission(pref.getContext(), Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                        // Permission is not granted
                        // Should we show an explanation?
                        //if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_CALENDAR)) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                        //} else {
                        // No explanation needed; request the permission

                        //ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_CALENDAR}, MY_PERMISSIONS_REQUEST_READ_CALENDAR);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},MY_PERMISSIONS_REQUEST_READ_CALENDAR);
                        }

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                        //}

                    } else {
                        p2.setEnabled(true);
                        p3.setEnabled(true);
                        p4.setEnabled(true);
//                        p5.setEnabled(true);
                    }
                }
            } if (key.equals(getString(R.string.key_timer_vibrate))) {
                boolean val = sharedPreferences.getBoolean(key, false);
                if (val) {
                    Vibrator vibrator = (Vibrator) pref.getContext().getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(300);
                }
            }
        } else if (pref instanceof EditTextPreference) {
            if (key.equals(getString(R.string.key_cancel_alarm_title))){
//                    key.equals(getString(R.string.key_cancel_alarm_label))) {
                EditTextPreference p = (EditTextPreference) findPreference(key);
                String val = sharedPreferences.getString(key, "");
                if (val.isEmpty()) {
                    setSummary(getPreferenceScreen().getSharedPreferences(), key, getString(R.string.disabled));
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CALENDAR: {
                // If request is cancelled, the result arrays are empty.
                SwitchPreference p = (SwitchPreference) findPreference(getString(R.string.key_cancel_alarm_holiday));
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    EditTextPreference p2 = (EditTextPreference) findPreference(getString(R.string.key_cancel_alarm_title));
                    ListPreference p3 = (ListPreference) findPreference(getString(R.string.key_cancel_alarm_calendar));
                    ListPreference p4 = (ListPreference) findPreference(getString(R.string.key_cancel_alarm_bank_calendar));
//                    EditTextPreference p5 = (EditTextPreference) findPreference(getString(R.string.key_cancel_alarm_label));

                    p2.setEnabled(true);
                    p3.setEnabled(true);
                    p4.setEnabled(true);
//                    p5.setEnabled(true);
                    setCalendatListPreferenceData(p3);
                    setCalendatListPreferenceData(p4);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    p.setChecked(false);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void setSummary(SharedPreferences prefs, String key) {
        setSummary(prefs, key, "");
    }

    private void setSummary(SharedPreferences prefs, String key, String defVal) {
        Preference pref = findPreference(key);
        // Setting a ListPreference's summary value to "%s" in XML automatically updates the
        // preference's summary to display the selected value.
        if (pref instanceof RingtonePreference) {
            Uri ringtoneUri = Uri.parse(prefs.getString(key, defVal));
            Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), ringtoneUri);

            String summary = ringtone.getTitle(getActivity());

            if (summary.lastIndexOf("/") >= 0) {
                summary = summary.substring(summary.lastIndexOf("/") + 1);
            }

            pref.setSummary(summary);
        } else if (pref instanceof EditTextPreference) {
            String string =prefs.getString(key, defVal);
            if (!defVal.isEmpty() && string.isEmpty()) {
                string = defVal;
            }
            pref.setSummary(string);
        }
    }
}
