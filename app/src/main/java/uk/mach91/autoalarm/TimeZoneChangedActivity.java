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

package uk.mach91.autoalarm;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.util.TimeZone;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

/**
 * Created by Chris Allenby on 22/09/2018.
 */
public class TimeZoneChangedActivity extends AppCompatActivity {

    int nOrigNightMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);

         setTheme(getApplicationInfo().theme);

         UiModeManager uiManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
         nOrigNightMode = uiManager.getNightMode();

        final String themeLight = getString(R.string.theme_light);
        String theme = PreferenceManager.getDefaultSharedPreferences(this).getString(
                getString(R.string.key_theme), null);
        if (!themeLight.equals(theme) && (nOrigNightMode != UiModeManager.MODE_NIGHT_YES)) {
             uiManager.setNightMode(UiModeManager.MODE_NIGHT_YES);
         } else {
             nOrigNightMode = -1;
         }

         Intent intent = getIntent();

         String newTZ = intent.getStringExtra(getString(R.string.newTimeZone));

         String oldTzId = intent.getStringExtra(getString(R.string.oldTimeZone));

         TimeZone oldTimeZone = TimeZone.getTimeZone(oldTzId);
         TimeZone newTimeZone = TimeZone.getTimeZone(newTZ);

         View aboutPage  = new AboutPage(this)
                .isRTL(false)
                .setImage(R.drawable.ic_about_title_logo)
                .setDescription(getString(R.string.time_zone_change_description_1))
                .addItem(new Element().setTitle(getString(R.string.time_zone_change_description_2)))
                .addItem(new Element().setTitle(getString(R.string.time_zone_change_description_3)))

                .addGroup(getString(R.string.time_zone_change_old))
                .addItem(new Element().setTitle(oldTimeZone.getDisplayName()))
                .addGroup(getString(R.string.time_zone_change_new))
                .addItem(new Element().setTitle(newTimeZone.getDisplayName()))
                .create();
         setContentView(aboutPage);

         //setContentView(R.layout.activity_time_zone_chnaged);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't do this or we can get into a infinite loop between onCreate and this.
        //if (nOrigNightMode >= 0) {
        //    UiModeManager uiManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        //    uiManager.setNightMode(nOrigNightMode);
        //    nOrigNightMode = -1;
        //}
    }
}
