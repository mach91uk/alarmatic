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

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.text.DateFormat;
import java.util.Date;

import uk.mach91.autoalarm.aboutdialog.AboutPage;
import uk.mach91.autoalarm.aboutdialog.Element;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.GnuGeneralPublicLicense30;
import de.psdev.licensesdialog.licenses.MITLicense;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;
import uk.mach91.autoalarm.timepickers.Utils;

/**
 * Created by Mach91 on 22/09/2018.
 */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setThemeFromPreference(this);

        Element versionElement = new Element();
        String version = String.format(getString(R.string.about_version), BuildConfig.VERSION_NAME);
        if (BuildConfig.DEBUG) {
            version += " (DEBUG)";
        }
        Date buildDate = new Date(BuildConfig.TIMESTAMP);

        String buildDateStr = DateFormat.getDateInstance(DateFormat.LONG).format(buildDate);
        String buildTimeStr = DateFormat.getTimeInstance().format(buildDate);

        versionElement.setTitle(version);

        //Element licenseElement = getLicensesElement();
        View aboutPage  = new AboutPage(this, getResources().getColor(R.color.textColorPrimary), getResources().getColor(R.color.textColorHighlight))
                .isRTL(false)
                .setImage(R.drawable.ic_alarmatic_logo)
                .setDescription(getString(R.string.about_description))
                .addItem(versionElement)
                .addItem(new Element().setTitle(String.format(getString(R.string.about_build_date), buildDateStr)))
                .addItem(new Element().setTitle(String.format(getString(R.string.about_build_time), buildTimeStr)))
                .addItem(new Element().setTitle(getString(R.string.about_copyright)))
                .addGroup(getString(R.string.about_connect))
                .addEmail(getString(R.string.about_email))
                .addGitHub("mach91uk/alarmatic")
//                .addPlayStore(getString(R.string.about_playstore))
                .addWebsite("https://htmlpreview.github.io/?https://github.com/mach91uk/alarmatic/blob/master/privacypolicy.html")
                .addGroup(getString(R.string.about_open_source_licenses))
                .addItem(getLicensesElementAlarmatic())
                .addItem(getLicensesElementClockPlus())
                .addItem(getLicensesElementAboutPage())
                .addItem(getLicensesElementLicensesDialog())
                .addItem(getLicensesElementButterKnife())
                .addItem(getLicensesElementHSVAlphaColorPicker())
                .create();

        setContentView(aboutPage);

    }

    private Element getLicensesElementButterKnife() {
        final Notices notices = new Notices();

        //https://github.com/PSDev/LicensesDialog/tree/master/library/src/main/java/de/psdev/licensesdialog/licenses

        notices.addNotice(new Notice("Butter Knife",
                "https://github.com/JakeWharton/butterknife",
                "Copyright 2013 Jake Wharton",
                new  GnuGeneralPublicLicense30()));

        final LicensesDialog ld = new LicensesDialog.Builder(AboutActivity.this)
                .setTitle("Butter Knife")
                .setNotices(notices)
                .setIncludeOwnLicense(false)
                .setShowFullLicenseText(true)
                .setThemeResourceId(Utils.getThemeID(this))
                .build();

        Element element = new Element();
        element.setTitle("Butter Knife");
        element.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ld.show();
            }
        });
        return element;
    }

    private Element getLicensesElementAlarmatic() {
        final Notices notices = new Notices();

        //https://github.com/PSDev/LicensesDialog/tree/master/library/src/main/java/de/psdev/licensesdialog/licenses

        notices.addNotice(new Notice(getString(R.string.app_name),
                getString(R.string.about_url),
                getString(R.string.about_copyright),
                new  GnuGeneralPublicLicense30()));

        final LicensesDialog ld = new LicensesDialog.Builder(AboutActivity.this)
                .setTitle(getString(R.string.app_name))
                .setNotices(notices)
                .setIncludeOwnLicense(false)
                .setShowFullLicenseText(true)
                .setThemeResourceId(Utils.getThemeID(this))
                .build();

        Element element = new Element();
        element.setTitle(getString(R.string.app_name));
        element.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ld.show();
            }
        });
        return element;
    }

    private Element getLicensesElementClockPlus() {
        final Notices notices = new Notices();

        notices.addNotice(new Notice("Clock Plus",
                "https://github.com/philliphsu/ClockPlus",
                "Copyright (c) 2017 Phillip Hsu",
                new  GnuGeneralPublicLicense30()));

        final LicensesDialog ld = new LicensesDialog.Builder(AboutActivity.this)
                .setTitle("Clock Plus")
                .setNotices(notices)
                .setIncludeOwnLicense(false)
                .setShowFullLicenseText(true)
                .setThemeResourceId(Utils.getThemeID(this))
                .build();

        Element element = new Element();
        element.setTitle("Clock Plus");
        element.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ld.show();
            }
        });
        return element;
    }

    private Element getLicensesElementAboutPage() {
        final Notices notices = new Notices();

        notices.addNotice(new Notice("Android About Page",
                "https://github.com/medyo/android-about-page",
                "The MIT License (MIT)\n" +
                        "Copyright (c) 2016 Mehdi Sakout",
                new MITLicense()));

        final LicensesDialog ld = new LicensesDialog.Builder(AboutActivity.this)
                .setTitle("Android About Page")
                .setNotices(notices)
                .setIncludeOwnLicense(false)
                .setShowFullLicenseText(true)
                .setThemeResourceId(Utils.getThemeID(this))
                .build();

        Element element = new Element();
        element.setTitle("Android About Page");
        element.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ld.show();
            }
        });
        return element;
    }

    private Element getLicensesElementHSVAlphaColorPicker() {
        final Notices notices = new Notices();

        notices.addNotice(new Notice("HSV-Alpha Color Picker",
                "https://github.com/martin-stone/hsv-alpha-color-picker-android",
                "Copyright (C) 2015 Martin Stone",
                new ApacheSoftwareLicense20()));

        final LicensesDialog ld = new LicensesDialog.Builder(AboutActivity.this)
                .setTitle("HSV-Alpha Color Picker")
                .setNotices(notices)
                .setIncludeOwnLicense(false)
                .setShowFullLicenseText(true)
                .setThemeResourceId(Utils.getThemeID(this))
                .build();

        Element element = new Element();
        element.setTitle("HSV-Alpha Color Picker");
        element.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ld.show();
            }
        });
        return element;
    }

    private Element getLicensesElementLicensesDialog() {
        final Notices notices = new Notices();

        final LicensesDialog ld = new LicensesDialog.Builder(AboutActivity.this)
                .setTitle("LicensesDialog")
                .setNotices(notices)
                .setIncludeOwnLicense(true)
                .setShowFullLicenseText(true)
                .setThemeResourceId(Utils.getThemeID(this))
                .build();

        Element element = new Element();
        element.setTitle("LicensesDialog");
        element.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ld.show();
            }
        });
        return element;
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
