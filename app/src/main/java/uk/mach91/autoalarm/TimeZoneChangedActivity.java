/*
 * Copyright 2017 Mach91
 *
 * This file is part of Alarmatic.
 *
 * Alarmatic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alarmatic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alarmatic.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package uk.mach91.autoalarm;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.util.TimeZone;

import uk.mach91.autoalarm.aboutdialog.AboutPage;
import uk.mach91.autoalarm.aboutdialog.Element;
import uk.mach91.autoalarm.timepickers.Utils;

/**
 * Created by Mach91 on 22/09/2018.
 */
public class TimeZoneChangedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);

         //setTheme(getApplicationInfo().theme);
        Utils.setThemeFromPreference(this);

         Intent intent = getIntent();

         String newTZ = intent.getStringExtra(getString(R.string.newTimeZone));

         String oldTzId = intent.getStringExtra(getString(R.string.oldTimeZone));

         TimeZone oldTimeZone = TimeZone.getTimeZone(oldTzId);
         TimeZone newTimeZone = TimeZone.getTimeZone(newTZ);

         View aboutPage  = new AboutPage(this, getResources().getColor(R.color.textColorPrimary), getResources().getColor(R.color.textColorHighlight))
                .isRTL(false)
                .setImage(R.drawable.ic_alarmatic_logo)
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
    }
}
