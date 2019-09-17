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

package uk.mach91.autoalarm.alarms.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import androidx.annotation.IdRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import uk.mach91.autoalarm.MainActivity;
import uk.mach91.autoalarm.R;
import uk.mach91.autoalarm.alarms.Alarm;
import uk.mach91.autoalarm.alarms.misc.AlarmController;
import uk.mach91.autoalarm.alarms.misc.AlarmPreferences;
import uk.mach91.autoalarm.alarms.misc.DaysOfWeek;
import uk.mach91.autoalarm.dialogs.RingtonePickerDialog;
import uk.mach91.autoalarm.dialogs.RingtonePickerDialogController;
import uk.mach91.autoalarm.list.OnListItemInteractionListener;
import uk.mach91.autoalarm.settings.SettingsActivity;
import uk.mach91.autoalarm.timepickers.Utils;
import uk.mach91.autoalarm.util.FragmentTagUtils;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.OnClick;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static uk.mach91.autoalarm.MainActivity.REQUEST_THEME_CHANGE;

/**
 * Created by Phillip Hsu on 7/31/2016.
 */
public class ExpandedAlarmViewHolder extends BaseAlarmViewHolder {
    private static final String TAG = "ExpandedAlarmViewHolder";

    private static final int  MY_PERMISSIONS_REQUEST_READ_CALENDAR = 11;

    @BindView(R.id.countdown) AlarmCountdown mCountdown;
    @BindView(R.id.ok) Button mOk;
    @BindView(R.id.delete) Button mDelete;
    @BindView(R.id.ringtone) Button mRingtone;
    @BindView(R.id.vibrate) TempCheckableImageButton mVibrate;
    @BindView(R.id.skip_holiday) ToggleButton mSkipHoliday;
    @BindViews({R.id.day0, R.id.day1, R.id.day2, R.id.day3, R.id.day4, R.id.day5, R.id.day6})
    ToggleButton[] mDays;

    private final ColorStateList mDayToggleColors;
    private final ColorStateList mVibrateColors;
    private final ColorStateList mSkipHolidayColors;
    private final RingtonePickerDialogController mRingtonePickerController;

    public ExpandedAlarmViewHolder(ViewGroup parent, final OnListItemInteractionListener<Alarm> listener,
                                   AlarmController controller) {
        super(parent, R.layout.item_expanded_alarm, listener, controller);
        // Manually bind listeners, or else you'd need to write a getter for the
        // OnListItemInteractionListener in the BaseViewHolder for use in method binding.
        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onListItemDeleted(getAlarm());
            }
        });
        mOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Since changes are persisted as soon as they are made, there's really
                // nothing we have to persist here. Let the listener know we should
                // collapse this VH.
                // While this works, it also makes an update to the DB and thus reschedules
                // the alarm, so the snackbar will show up as well. We want to avoid that..
//                listener.onListItemUpdate(getAlarm(), getAdapterPosition());
                // TODO: This only works because we know what the implementation looks like..
                // This is bad because we just made the proper function of this dependent
                // on the implementation.
                listener.onListItemClick(getAlarm(), getAdapterPosition());
            }
        });

        // https://code.google.com/p/android/issues/detail?id=177282
        // https://stackoverflow.com/questions/15673449/is-it-confirmed-that-i-cannot-use-themed-color-attribute-in-color-state-list-res
        // Programmatically create the ColorStateList for our day toggles using themed color
        // attributes, "since prior to M you can't create a themed ColorStateList from XML but you
        // can from code." (quote from google)
        // The first array level is analogous to an XML node defining an item with a state list.
        // The second level lists all the states considered by the item from the first level.
        // An empty list of states represents the default stateless item.
        int[][] states = {
                /*item 1*/{/*states*/android.R.attr.state_checked},
                /*item 2*/{/*states*/}
        };
        // TODO: Phase out Utils.getColorFromThemeAttr because it doesn't work for text colors.
        // WHereas getTextColorFromThemeAttr works for both regular colors and text colors.
        int[] dayToggleColors = {
                /*item 1*/Utils.getTextColorFromThemeAttr(getContext(), R.attr.colorAccent),
                /*item 2*/Utils.getTextColorFromThemeAttr(getContext(), android.R.attr.textColorHint)
        };
        int[] vibrateColors = {
                /*item 1*/Utils.getTextColorFromThemeAttr(getContext(), R.attr.colorAccent),
                /*item 2*/Utils.getTextColorFromThemeAttr(getContext(), R.attr.themedIconTint)
        };
        int[] skipHolidayColors = {
                /*item 1*/Utils.getTextColorFromThemeAttr(getContext(), R.attr.colorAccent),
                /*item 2*/Utils.getTextColorFromThemeAttr(getContext(), R.attr.themedIconTint)
        };
        mDayToggleColors = new ColorStateList(states, dayToggleColors);
        mVibrateColors = new ColorStateList(states, vibrateColors);
        mSkipHolidayColors = new ColorStateList(states, skipHolidayColors);

        mRingtonePickerController = new RingtonePickerDialogController(mFragmentManager,
                new RingtonePickerDialog.OnRingtoneSelectedListener() {
                    @Override
                    public void onRingtoneSelected(Uri ringtoneUri) {
                        Log.d(TAG, "Selected ringtone: " + ringtoneUri.toString());
                        final Alarm oldAlarm = getAlarm();
                        Alarm newAlarm = oldAlarm.toBuilder()
                                .ringtone(ringtoneUri.toString())
                                .build();
                        oldAlarm.copyMutableFieldsTo(newAlarm);
                        persistUpdatedAlarm(oldAlarm, newAlarm, false);
                    }
                }
        );
    }

    @Override
    public void onBind(Alarm alarm) {
        super.onBind(alarm);
        mRingtonePickerController.tryRestoreCallback(makeTag(R.id.ringtone));
        bindCountdown(alarm.isEnabled(), alarm.ringsAt(true), alarm);
        bindDays(alarm);
        bindRingtone();
        bindVibrate(alarm.vibrates());


        if (!alarm.skip_holiday()) {
            bindSkipHoliday(alarm.skip_holiday());
        } else if (AlarmPreferences.cancelAlarmHoliday( getContext()) &&
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            bindSkipHoliday(alarm.skip_holiday());
        } else {
            Alarm newAlarm = alarm.toBuilder()
                    .skip_holiday(false)
                    .build();
            alarm.copyMutableFieldsTo(newAlarm);
            persistUpdatedAlarm(alarm, newAlarm, false);
            bindSkipHoliday(false);
        }
    }

    private void bindCountdown(boolean enabled, long ringsAt, Alarm alarm) {
        if (enabled) {
            mCountdown.setAlarmSkipHoliday(alarm.skip_holiday());
            mCountdown.setBase(ringsAt);
            mCountdown.start();
            mCountdown.setVisibility(VISIBLE);
        } else {
            mCountdown.stop();
            mCountdown.setVisibility(GONE);
        }
    }

    @Override
    protected void bindLabel(boolean visible, String label) {
        super.bindLabel(true, label);
    }

    @OnClick(R.id.ok)
    void save() {
        // TODO
    }

//    @OnClick(R.id.delete)
//    void delete() {
//        // TODO
//    }

    @OnClick(R.id.ringtone)
    void showRingtonePickerDialog() {
//        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
//        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
//                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
//                // The ringtone to show as selected when the dialog is opened
//                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, getSelectedRingtoneUri())
//                // Whether to show "Default" item in the list
//                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
//        // The ringtone that plays when default option is selected
//        //.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, DEFAULT_TONE);
//        // TODO: This is VERY BAD. Use a Controller/Presenter instead.
//        // The result will be delivered to MainActivity, and then delegated to AlarmsFragment.
//        ((Activity) getContext()).startActivityForResult(intent, AlarmsFragment.REQUEST_PICK_RINGTONE);

        mRingtonePickerController.show(getSelectedRingtoneUri(), makeTag(R.id.ringtone));
    }

    @OnClick(R.id.vibrate)
    void onVibrateToggled() {
        final boolean checked = mVibrate.isChecked();
        if (checked) {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(300);
        }
        final Alarm oldAlarm = getAlarm();
        Alarm newAlarm = oldAlarm.toBuilder()
                .vibrates(checked)
                .build();
        oldAlarm.copyMutableFieldsTo(newAlarm);
        persistUpdatedAlarm(oldAlarm, newAlarm, false);
    }

    @OnClick(R.id.skip_holiday)
    void onSkipHolidayToggled() {
        final boolean checked = mSkipHoliday.isChecked();
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            final Alarm oldAlarm = getAlarm();
            Alarm newAlarm = oldAlarm.toBuilder()
                    .skip_holiday(checked)
                    .build();
            oldAlarm.copyMutableFieldsTo(newAlarm);
            persistUpdatedAlarm(oldAlarm, newAlarm, false);
            if (checked && !AlarmPreferences.cancelAlarmHoliday( getContext())) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

                SharedPreferences.Editor prefsEditor = prefs.edit();
                prefsEditor.putBoolean(getContext().getString(R.string.key_cancel_alarm_holiday), true);
                prefsEditor.commit();
            }
            if (checked &&
                    AlarmPreferences.cancelAlarmBankHolidayCalendar(getContext()).isEmpty() &&
                    (AlarmPreferences.cancelAlarmHolidayCalendar(getContext()).isEmpty() ||
                            AlarmPreferences.cancelAlarmHolidayTitle(getContext()).isEmpty())) {
                Intent prefsIntent = new Intent(getContext(), SettingsActivity.class);
                prefsIntent.putExtra(getContext().getString(R.string.prefs_mode), SettingsActivity.MODE_HOLIDAY);
                ((Activity)getContext()).startActivityForResult(prefsIntent, REQUEST_THEME_CHANGE);
            }
        } else {
            //mSkipHoliday.setChecked(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //ActivityCompat.requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},MY_PERMISSIONS_REQUEST_READ_CALENDAR);
                MainActivity mainActivity = (MainActivity)(Activity) getContext();
                mainActivity.mSkipHoliday = mSkipHoliday;
                ActivityCompat.requestPermissions((Activity) getContext(), new String[]{Manifest.permission.READ_CALENDAR},MY_PERMISSIONS_REQUEST_READ_CALENDAR);
            }
        }
    }

    @OnClick({ R.id.day0, R.id.day1, R.id.day2, R.id.day3, R.id.day4, R.id.day5, R.id.day6 })
    void onDayToggled(ToggleButton view) {
        final Alarm oldAlarm = getAlarm();
        Alarm newAlarm = oldAlarm.toBuilder().build();
        oldAlarm.copyMutableFieldsTo(newAlarm);
        // ---------------------------------------------------------------------------------
        // TOneverDO: precede copyMutableFieldsTo()
        int position = ((ViewGroup) view.getParent()).indexOfChild(view);
        int weekDayAtPosition = DaysOfWeek.getInstance(getContext()).weekDayAt(position);
        Log.d(TAG, "Day toggle #" + position + " checked changed. This is weekday #"
                + weekDayAtPosition + " relative to a week starting on Sunday");
        newAlarm.setRecurring(weekDayAtPosition, view.isChecked());
        // ---------------------------------------------------------------------------------
        persistUpdatedAlarm(oldAlarm, newAlarm, true);
    }

    private void bindDays(Alarm alarm) {
        for (int i = 0; i < mDays.length; i++) {
            mDays[i].setTextColor(mDayToggleColors);
            int weekDay = DaysOfWeek.getInstance(getContext()).weekDayAt(i);
            String label = DaysOfWeek.getLabel(weekDay);
            mDays[i].setTextOn(label);
            mDays[i].setTextOff(label);
            mDays[i].setChecked(alarm.isRecurring(weekDay));
        }
    }

    private void bindRingtone() {
        int iconTint = Utils.getTextColorFromThemeAttr(getContext(), R.attr.themedIconTint);

        Drawable ringtoneIcon = mRingtone.getCompoundDrawablesRelative()[0/*start*/];
        ringtoneIcon = DrawableCompat.wrap(ringtoneIcon.mutate());
        DrawableCompat.setTint(ringtoneIcon, iconTint);
        mRingtone.setCompoundDrawablesRelativeWithIntrinsicBounds(ringtoneIcon, null, null, null);


        String title = Utils.getAlarmToneTitle((Activity)getContext(), getSelectedRingtoneUri());

        if (getAlarm().ringtone().isEmpty()){
            title += " " + getContext().getString(R.string.default_tone);
            SpannableString ss;
            ss = new SpannableString(title);
            ss.setSpan(new RelativeSizeSpan(0.75f), title.length()-2, title.length(), 0); // set size
            ss.setSpan(new SuperscriptSpan(), title.length()-2, title.length(), 0); // set size
            mRingtone.setText(ss);
        } else {
            mRingtone.setText(title);
        }
    }

    private void bindVibrate(boolean vibrates) {
        Utils.setTintList(mVibrate, mVibrate.getDrawable(), mVibrateColors);
        mVibrate.setChecked(vibrates);
    }

    private void bindSkipHoliday(boolean skip_holiday) {
        mSkipHoliday.setTextColor(mSkipHolidayColors);

        String label = getContext().getString(R.string.alarm_skip_if_ok_holiday);
        mSkipHoliday.setTextOn(label);
        mSkipHoliday.setTextOff(label);
        mSkipHoliday.setChecked(skip_holiday);
    }

    private Uri getSelectedRingtoneUri() {
        // If showing an item for "Default" (@see EXTRA_RINGTONE_SHOW_DEFAULT), this can be one
        // of DEFAULT_RINGTONE_URI, DEFAULT_NOTIFICATION_URI, or DEFAULT_ALARM_ALERT_URI to have the
        // "Default" item checked.
        //
        // Otherwise, use RingtoneManager.getActualDefaultRingtoneUri() to get the "actual sound URI".
        //
        // Do not use RingtoneManager.getDefaultUri(), because that just returns one of
        // DEFAULT_RINGTONE_URI, DEFAULT_NOTIFICATION_URI, or DEFAULT_ALARM_ALERT_URI
        // depending on the type requested (i.e. what the docs calls "symbolic URI
        // which will resolved to the actual sound when played").
        String ringtone = getAlarm().ringtone();
        return ringtone.isEmpty() ?
                AlarmPreferences.defaultAlarmTone((getContext()))
                //RingtoneManager.getActualDefaultRingtoneUri(getContext(), RingtoneManager.TYPE_ALARM)
                : Uri.parse(ringtone);
    }

    private String makeTag(@IdRes int viewId) {
        return FragmentTagUtils.makeTag(ExpandedAlarmViewHolder.class, viewId, getItemId());
    }
}
