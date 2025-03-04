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

package uk.mach91.autoalarm;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
//import com.google.firebase.analytics.FirebaseAnalytics;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.core.content.ContextCompat;
import androidx.loader.content.Loader;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.viewpager.widget.ViewPager;

import android.provider.Settings;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;
import java.util.TimeZone;

import uk.mach91.autoalarm.alarms.misc.AlarmPreferences;
import uk.mach91.autoalarm.alarms.ui.AlarmsFragment;
import uk.mach91.autoalarm.list.RecyclerViewFragment;
import uk.mach91.autoalarm.settings.SettingsActivity;
import uk.mach91.autoalarm.data.BaseItemCursor;
import uk.mach91.autoalarm.timepickers.Utils;

import butterknife.BindView;

public class MainActivity extends BaseActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "MainActivity";

    private static final int  MY_PERMISSIONS_REQUEST_READ_CALENDAR = 11;
    private static final int  MY_PERMISSIONS_REQUEST_NOTIFICATION = 12;

    public static final int    PAGE_ALARMS          = 0;
    public static final int    REQUEST_THEME_CHANGE = 5;
    public static final String EXTRA_SHOW_PAGE      = "uk.mach91.autoalarm.extra.SHOW_PAGE";

    public ToggleButton mSkipHoliday = null;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private Drawable             mAddItemDrawable;

    @BindView(R.id.container)
    ViewPager mViewPager;

    @BindView(R.id.fab)
    FloatingActionButton mFab;

    @BindView(R.id.tabs)
    TabLayout mTabLayout;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        final View rootView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        // http://stackoverflow.com/a/24035591/5055032
        // http://stackoverflow.com/a/3948036/5055032
        // The views in our layout have begun drawing.
        // There is no lifecycle callback that tells us when our layout finishes drawing;
        // in my own test, drawing still isn't finished by onResume().
        // Post a message in the UI events queue to be executed after drawing is complete,
        // so that we may get their dimensions.
        rootView.post(new Runnable() {
            @Override
            public void run() {
                if (mViewPager.getCurrentItem() == mSectionsPagerAdapter.getCount() - 1) {
                    // Restore the FAB's translationX from a previous configuration.
                    mFab.setTranslationX(mViewPager.getWidth() / -2f + getFabPixelOffsetForXTranslation());
                }
            }
        });

        SharedPreferences prefs = this.getSharedPreferences(this.getPackageName(), Context.MODE_PRIVATE);

        String oldTzId = prefs.getString("timezone", "");

        if (oldTzId.isEmpty()) {
            SharedPreferences.Editor prefsEditor = prefs.edit();
            prefsEditor.putString("timezone", TimeZone.getDefault().getID());
            prefsEditor.commit();
        }

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            /**
             * @param position       Either the current page position if the offset is increasing,
             *                       or the previous page position if it is decreasing.
             * @param positionOffset If increasing from [0, 1), scrolling right and position = currentPagePosition
             *                       If decreasing from (1, 0], scrolling left and position = (currentPagePosition - 1)
             */
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Log.d(TAG, String.format("pos = %d, posOffset = %f, posOffsetPixels = %d",
                        position, positionOffset, positionOffsetPixels));
                int pageBeforeLast = mSectionsPagerAdapter.getCount() - 2;
                if (position <= pageBeforeLast) {
                    if (position < pageBeforeLast) {
                        // When the scrolling is due to tab selection between multiple tabs apart,
                        // this callback is called for each intermediate page, but each of those pages
                        // will briefly register a sparsely decreasing range of positionOffsets, always
                        // from (1, 0). As such, you would notice the FAB to jump back and forth between
                        // x-positions as each intermediate page is scrolled through.
                        // This is a visual optimization that ends the translation motion, immediately
                        // returning the FAB to its target position.
                        // TODO: The animation visibly skips to the end. We could interpolate
                        // intermediate x-positions if we cared to smooth it out.
                        mFab.setTranslationX(0);
                    } else {
                        // Initially, the FAB's translationX property is zero because, at its original
                        // position, it is not translated. setTranslationX() is relative to the view's
                        // left position, at its original position; this left position is taken to be
                        // the zero point of the coordinate system relative to this view. As your
                        // translationX value is increasingly negative, the view is translated left.
                        // But as translationX is decreasingly negative and down to zero, the view
                        // is translated right, back to its original position.
                        float translationX = positionOffsetPixels / -2f;
                        // NOTE: You MUST scale your own additional pixel offsets by positionOffset,
                        // or else the FAB will immediately translate by that many pixels, appearing
                        // to skip/jump.
                        translationX += positionOffset * getFabPixelOffsetForXTranslation();
                        mFab.setTranslationX(translationX);
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "onPageSelected");
                if (position < mSectionsPagerAdapter.getCount() - 1) {
                    mFab.setImageDrawable(mAddItemDrawable);
                }
                Fragment f = mSectionsPagerAdapter.getFragment(mViewPager.getCurrentItem());
                // NOTE: This callback is fired after a rotation, right after onStart().
                // Unfortunately, the FragmentManager handling the rotation has yet to
                // tell our adapter to re-instantiate the Fragments, so our collection
                // of fragments is empty. You MUST keep this check so we don't cause a NPE.
                if (f instanceof BaseFragment) {
                    ((BaseFragment) f).onPageSelected();
                }
            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        ColorStateList tabIconColor = ContextCompat.getColorStateList(this, R.color.tab_icon_color);
        setTabIcon(PAGE_ALARMS, R.drawable.ic_alarm_24dp, getString(R.string.app_name), tabIconColor);

        // TODO: @OnCLick instead.
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment f = mSectionsPagerAdapter.getFragment(mViewPager.getCurrentItem());
                if (f instanceof RecyclerViewFragment) {
                    ((RecyclerViewFragment) f).onFabClick();
                }
            }
        });

        mAddItemDrawable = ContextCompat.getDrawable(this, R.drawable.ic_add_24dp);
        handleActionScrollToStableId(getIntent(), false);


        SharedPreferences prefSetts = PreferenceManager.getDefaultSharedPreferences(this);

        /*
        boolean askedUserExp = prefSetts.getBoolean(getString(R.string.key_asked_user_experience_program), false);

        if (!askedUserExp) {

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            SharedPreferences.Editor prefsEditor = prefSetts.edit();
                            prefsEditor.putBoolean(getString(R.string.key_user_experience_program), true);
                            prefsEditor.putBoolean(getString(R.string.key_asked_user_experience_program), true);
                            prefsEditor.commit();
                            //Utils.firebaseOnOff(getBaseContext(), mFirebaseAnalytics, true);
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            SharedPreferences.Editor prefsEditor2 = prefSetts.edit();
                            prefsEditor2.putBoolean(getString(R.string.key_user_experience_program), false);
                            prefsEditor2.putBoolean(getString(R.string.key_asked_user_experience_program), true);
                            prefsEditor2.commit();
                            //Utils.firebaseOnOff(getBaseContext(), mFirebaseAnalytics, false);
                            break;
                    }
                }
            };


            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final AlertDialog alertDialog = builder.setMessage(Html.fromHtml(getString(R.string.asked_user_experience_program)))
                    .setPositiveButton(getString (R.string.yes), dialogClickListener)
                    .setTitle(getString (R.string.user_experience_title))
                    .setNegativeButton(getString (R.string.no), dialogClickListener).show();
            ((TextView)alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());


        }
        */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean askedBatteryOptimised = prefSetts.getBoolean(getString(R.string.key_battery_optimisation), false);

            if (!askedBatteryOptimised) {
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    SharedPreferences.Editor prefsEditor = prefSetts.edit();
                                    prefsEditor.putBoolean(getString(R.string.key_battery_optimisation), true);
                                    prefsEditor.commit();
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                                    startActivity(intent);
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    SharedPreferences.Editor prefsEditor2 = prefSetts.edit();
                                    prefsEditor2.putBoolean(getString(R.string.key_battery_optimisation), true);
                                    prefsEditor2.commit();
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    final AlertDialog alertDialog = builder.setMessage(Html.fromHtml(getString(R.string.ask_battery_optimisation)))
                            .setPositiveButton(getString(R.string.yes), dialogClickListener)
                            .setTitle(getString(R.string.ask_battery_optimisation_title))
                            .setNegativeButton(getString(R.string.no), dialogClickListener).show();
                    ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

                }
            }
        }
        /*if (Build.VERSION.SDK_INT >= 33) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (!nm.areNotificationsEnabled()) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY)) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, MY_PERMISSIONS_REQUEST_NOTIFICATION);
                } else {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, MY_PERMISSIONS_REQUEST_NOTIFICATION);
                }
                //}
            }
        }
        */
    }

    private void setTabIcon(int index, @DrawableRes int iconRes, String title, @NonNull final ColorStateList color) {
        TabLayout.Tab tab = mTabLayout.getTabAt(index);
        Drawable icon = Utils.getTintedDrawable(this, iconRes, color);
        DrawableCompat.setTintList(icon, ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.white)));
        //tab.setIcon(icon);
        //tab.setText(title);

        icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        // Replace blank spaces with image icon
        SpannableString sb = new SpannableString("   " + title);
        ImageSpan imageSpan = new ImageSpan(icon, ImageSpan.ALIGN_BASELINE);
        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tab.setText(sb);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleActionScrollToStableId(intent, true);
    }

    /**
     * Handles a PendingIntent, fired from e.g. clicking a notification, that tells us to
     * set the ViewPager's current item and scroll to a specific RecyclerView item
     * given by its stable ID.
     *
     * @param performScroll Whether to actually scroll to the stable id, if there is one.
     *                      Pass true if you know {@link
     *                      RecyclerViewFragment#onLoadFinished(Loader, BaseItemCursor) onLoadFinished()}
     *                      had previously been called. Otherwise, pass false so that we can
     *                      {@link RecyclerViewFragment#setScrollToStableId(long) setScrollToStableId(long)}
     *                      and let {@link
     *                      RecyclerViewFragment#onLoadFinished(Loader, BaseItemCursor) onLoadFinished()}
     *                      perform the scroll for us.
     */
    private void handleActionScrollToStableId(@NonNull final Intent intent,
                                              final boolean performScroll) {
        if (RecyclerViewFragment.ACTION_SCROLL_TO_STABLE_ID.equals(intent.getAction())) {
            final int targetPage = intent.getIntExtra(EXTRA_SHOW_PAGE, -1);
            if (targetPage >= 0 && targetPage <= mSectionsPagerAdapter.getCount() - 1) {
                // #post() works for any state the app is in, especially robust against
                // cases when the app was not previously in memory--i.e. this got called
                // in onCreate().
                mViewPager.post(new Runnable() {
                    @Override
                    public void run() {
                        mViewPager.setCurrentItem(targetPage, true/*smoothScroll*/);
                        final long stableId = intent.getLongExtra(RecyclerViewFragment.EXTRA_SCROLL_TO_STABLE_ID, -1);
                        if (stableId != -1) {
                            RecyclerViewFragment rvFrag = (RecyclerViewFragment)
                                    mSectionsPagerAdapter.getFragment(targetPage);
                            if (performScroll) {
                                rvFrag.performScrollToStableId(stableId);
                            } else {
                                rvFrag.setScrollToStableId(stableId);
                            }
                        }
                        intent.setAction(null);
                        intent.removeExtra(EXTRA_SHOW_PAGE);
                        intent.removeExtra(RecyclerViewFragment.EXTRA_SCROLL_TO_STABLE_ID);
                    }
                });
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            return;
        // If we get here, either this Activity OR one of its hosted Fragments
        // started a requested Activity for a result. The latter case may seem
        // strange; the Fragment is the one starting the requested Activity, so why
        // does the result end up in its host Activity? Shouldn't it end up in
        // Fragment#onActivityResult()? Actually, the Fragment's host Activity gets the
        // first shot at handling the result, before delegating it to the Fragment
        // in Fragment#onActivityResult().
        //
        // There are subtle points to keep in mind when it is actually the Fragment
        // that should handle the result, NOT this Activity. You MUST start
        // the requested Activity with Fragment#startActivityForResult(), NOT
        // Activity#startActivityForResult(). The former calls
        // FragmentActivity#startActivityFromFragment() to implement its behavior.
        // Among other things (not relevant to the discussion),
        // FragmentActivity#startActivityFromFragment() sets internal bit flags
        // that are necessary to achieve the described behavior (that this Activity
        // should delegate the result to the Fragment). Finally, you MUST call
        // through to the super implementation of Activity#onActivityResult(),
        // i.e. FragmentActivity#onActivityResult(). It is this method where
        // the aforementioned internal bit flags will be read to determine
        // which of this Activity's hosted Fragments started the requested
        // Activity.
        //
        // If you are not careful with these points and instead mistakenly call
        // Activity#startActivityForResult(), THEN YOU WILL ONLY BE ABLE TO
        // HANDLE THE REQUEST HERE; the super implementation of onActivityResult()
        // will not delegate the result to the Fragment, because the requisite
        // internal bit flags are not set with Activity#startActivityForResult().
        //
        // Further reading:
        // http://stackoverflow.com/q/6147884/5055032
        // http://stackoverflow.com/a/24303360/5055032
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_THEME_CHANGE:
                if (data != null && data.getBooleanExtra(SettingsActivity.EXTRA_THEME_CHANGED, false)) {
                    recreate();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CALENDAR: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    //SharedPreferences prefs = this.getSharedPreferences(this.getPackageName(), Context.MODE_PRIVATE);

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                    SharedPreferences.Editor prefsEditor = prefs.edit();
                    prefsEditor.putBoolean(getApplicationContext().getString(R.string.key_cancel_alarm_holiday), true);
                    prefsEditor.commit();

                   if (AlarmPreferences.cancelAlarmBankHolidayCalendar(getApplicationContext()).isEmpty() &&
                           (AlarmPreferences.cancelAlarmHolidayCalendar(getApplicationContext()).isEmpty() ||
                           AlarmPreferences.cancelAlarmHolidayTitle(getApplicationContext()).isEmpty())) {
                       Intent prefsIntent = new Intent(this, SettingsActivity.class);
                       prefsIntent.putExtra(this.getString(R.string.prefs_mode), SettingsActivity.MODE_HOLIDAY);
                       startActivityForResult(prefsIntent, REQUEST_THEME_CHANGE);
                   }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    mSkipHoliday.setChecked(false);

                    SharedPreferences prefs = this.getSharedPreferences(this.getPackageName(), Context.MODE_PRIVATE);

                    SharedPreferences.Editor prefsEditor = prefs.edit();
                    prefsEditor.putBoolean(getApplicationContext().getString(R.string.key_cancel_alarm_holiday), false);
                    prefsEditor.commit();
                }
                mSkipHoliday = null;
                return;
            }

            case MY_PERMISSIONS_REQUEST_NOTIFICATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    //SharedPreferences prefs = this.getSharedPreferences(this.getPackageName(), Context.MODE_PRIVATE)
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }
        }
    }

    @Override
    protected int layoutResId() {
        return R.layout.activity_main;
    }

    @Override
    protected int menuResId() {
        return R.menu.menu_main;
    }

    @Override
    protected boolean isDisplayHomeUpEnabled() {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent prefsIntent = new Intent(this, SettingsActivity.class);
            prefsIntent.putExtra(this.getString(R.string.prefs_mode), SettingsActivity.MODE_STANDARD);
            startActivityForResult(prefsIntent, REQUEST_THEME_CHANGE);
            return true;
        } else if (id == R.id.action_about) {
            startActivity( new Intent(this, AboutActivity.class));
            return true;
        } else if (id == R.id.action_screensaver) {
            Intent i = new Intent(this, ScreenSaverActivity.class);
            i.putExtra("InAppLaunch",true);
            startActivity(i);
            //startActivity( new Intent(this, ScreenSaverActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * @return the positive offset in pixels required to rebase an X-translation of the FAB
     * relative to its center position. An X-translation normally is done relative to a view's
     * left position.
     */
    private float getFabPixelOffsetForXTranslation() {
        final int margin;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Since each side's margin is the same, any side's would do.
            margin = ((ViewGroup.MarginLayoutParams) mFab.getLayoutParams()).rightMargin;
        } else {
            // Pre-Lollipop has measurement issues with FAB margins. This is
            // probably as good as we can get to centering the FAB, without
            // hardcoding some small margin value.
            margin = 0;
        }
        // By adding on half the FAB's width, we effectively rebase the translation
        // relative to the view's center position.
        return mFab.getWidth() / 2f + margin;
    }

    private static class SectionsPagerAdapter extends FragmentPagerAdapter {
        private final SparseArray<Fragment> mFragments = new SparseArray<>(getCount());

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case PAGE_ALARMS:
                    return new AlarmsFragment();
                default:
                    throw new IllegalStateException("No fragment can be instantiated for position " + position);
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            mFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        @Override
        public int getCount() {
            return 1;
        }

        public Fragment getFragment(int position) {
            return mFragments.get(position);
        }
    }
}
