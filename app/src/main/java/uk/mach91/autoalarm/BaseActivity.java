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
 */

package uk.mach91.autoalarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;

//import com.google.firebase.analytics.FirebaseAnalytics;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.mach91.autoalarm.alarms.misc.AlarmPreferences;
import uk.mach91.autoalarm.timepickers.Utils;

/**
 * Created by Phillip Hsu on 5/31/2016.
 *
 * Copyright 2018 Mach91 - content updated for Alarmatic
 *  *
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Nullable
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    private Menu mMenu;

    @LayoutRes protected abstract int layoutResId();
    @MenuRes   protected abstract int menuResId();

//    public FirebaseAnalytics mFirebaseAnalytics;

    @CallSuper
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtain the FirebaseAnalytics instance.
//        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Initialize the associated SharedPreferences file with default values
        // for each preference when the user first opens your application.
        // When false, the system sets the default values only if this method has
        // never been called in the past (or the KEY_HAS_SET_DEFAULT_VALUES in the
        // default value shared preferences file is false).
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // ========================================================================================
        // TOneverDO: Set theme after setContentView()

        Utils.setThemeFromPreference(this);

//        Utils.firebaseOnOff(this, mFirebaseAnalytics, AlarmPreferences.isUserExperianceEnabled(this));

        // ========================================================================================
        setContentView(layoutResId());

        // Direct volume changes to the alarm stream
        setVolumeControlStream(AudioManager.STREAM_ALARM);
        ButterKnife.bind(this);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayHomeAsUpEnabled(isDisplayHomeUpEnabled());
                ab.setDisplayShowTitleEnabled(isDisplayShowTitleEnabled());
            }
        }
    }

    @Override
    public final boolean onCreateOptionsMenu(Menu menu) {
        if (menuResId() != 0) {
            getMenuInflater().inflate(menuResId(), menu);
            mMenu = menu;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Nullable
    public final Menu getMenu() {
        return mMenu;
    }

    protected boolean isDisplayHomeUpEnabled() {
        return true;
    }

    protected boolean isDisplayShowTitleEnabled() {
        return false;
    }
}
