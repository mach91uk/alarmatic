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

package uk.mach91.autoalarm.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

import uk.mach91.autoalarm.BaseActivity;
import uk.mach91.autoalarm.R;

/**
 * Created by Phillip Hsu on 6/6/2016.
 */
public class SettingsActivity extends BaseActivity {
    public static final String EXTRA_THEME_CHANGED = "uk.mach91.autoalarm.settings.extra.THEME_CHANGED";

    private String mInitialTheme;
    private SharedPreferences mPrefs;

    private boolean mInitialSkipIfOnHoliday;
    private int mInitalFirstDayOfTheWeek;

    public static final int  MODE_STANDARD = 0;
    public static final int  MODE_HOLIDAY = 1;
    public static final int  MODE_SCREENSAVER = 2;
    public static final int  MODE_LAST = MODE_SCREENSAVER;

    public int mMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        int pref_mode = intent.getIntExtra(getString(R.string.prefs_mode), MODE_SCREENSAVER);
        if (pref_mode >=MODE_STANDARD && pref_mode <=MODE_LAST) {
            mMode = pref_mode;
        } else {
            mMode = MODE_SCREENSAVER;
        }

        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mInitialTheme = getSelectedTheme();
        mInitialSkipIfOnHoliday = getSkipIfOnHoliday();
        mInitalFirstDayOfTheWeek = getFirstDayOfTheWeek();
    }

    @Override
    protected int layoutResId() {
        return R.layout.activity_settings;
    }

    @Override
    protected int menuResId() {
        return 0;
    }

    @Override
    protected boolean isDisplayShowTitleEnabled() {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mMode == MODE_SCREENSAVER) {
                    super.onBackPressed();
                    return true;
                } else {
                    setThemeResult(getSelectedTheme(), getSkipIfOnHoliday(), getFirstDayOfTheWeek());
                    return false; // Don't capture, proceed as usual
                }
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        setThemeResult(getSelectedTheme(), getSkipIfOnHoliday(), getFirstDayOfTheWeek());
        super.onBackPressed();
    }

    private String getSelectedTheme() {
        return mPrefs.getString(getString(R.string.key_theme), "");
    }

    private boolean getSkipIfOnHoliday() {
        return mPrefs.getBoolean(getString(R.string.key_cancel_alarm_holiday), false);
    }

    private int getFirstDayOfTheWeek() {
        String value = mPrefs.getString(getString(R.string.key_first_day_of_week), null);
        return null == value ? 1 : Integer.parseInt(value);
    }

    private void setThemeResult(String selectedTheme, boolean skipIfOnHoliday, int firstDayOfWeek) {
        Intent result = new Intent();
        boolean recreateLayout = false;
        if (!selectedTheme.equals(mInitialTheme) ||
                skipIfOnHoliday != mInitialSkipIfOnHoliday ||
                firstDayOfWeek != mInitalFirstDayOfTheWeek) {
            recreateLayout = true;
        }
        result.putExtra(EXTRA_THEME_CHANGED, recreateLayout);
        setResult(Activity.RESULT_OK, result);
    }
}