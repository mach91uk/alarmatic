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

package uk.mach91.autoalarm.dialogs;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.SwitchPreference;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import uk.mach91.autoalarm.R;
import uk.mach91.autoalarm.ringtone.playback.RingtoneLoop;
import uk.mach91.autoalarm.settings.SettingsActivity;

import static android.app.Activity.RESULT_OK;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

/**
 * Created by Phillip Hsu on 9/3/2016.
 * <p></p>
 * An alternative to the system's ringtone picker dialog. The differences are:
 * (1) this dialog matches the current theme,
 * (2) the selected ringtone URI is delivered via the {@link OnRingtoneSelectedListener
 * OnRingtoneSelectedListener} callback.
 * <p></p>
 * TODO: If a ringtone was playing and the configuration changes, the ringtone is destroyed.
 * Restore the playing ringtone (seamlessly, without the stutter that comes from restarting).
 * Setting setRetainInstance(true) in onCreate() made our app crash (error said attempted to
 * access closed Cursor).
 * We might need to play the ringtone from a Service instead, so we won't have to worry about
 * the ringtone being destroyed on rotation.
 */
public class RingtonePickerDialog extends BaseAlertDialogFragment {
    private static final String TAG = "RingtonePickerDialog";
    private static final String KEY_RINGTONE_URI = "key_ringtone_uri";

    private static final int  MY_RINGTONE_REQUEST_READ_EXTERNAL_STORAGE = 11;

    private RingtoneManager mRingtoneManager;
    private OnRingtoneSelectedListener mOnRingtoneSelectedListener;
    private Uri mRingtoneUri;
    private RingtoneLoop mRingtone;
    String mCustomString;
    ArrayAdapter<String> mRingtonesAdapter;

    final int SELECT_AUDIO_FILE_CODE = 42;

    public interface OnRingtoneSelectedListener {
        void onRingtoneSelected(Uri ringtoneUri);
    }

    /**
     * @param ringtoneUri the URI of the ringtone to show as initially selected
     */
    public static RingtonePickerDialog newInstance(OnRingtoneSelectedListener l, Uri ringtoneUri) {
        RingtonePickerDialog dialog = new RingtonePickerDialog();
        dialog.mOnRingtoneSelectedListener = l;
        dialog.mRingtoneUri = ringtoneUri;
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mRingtoneUri = savedInstanceState.getParcelable(KEY_RINGTONE_URI);
        }
        mRingtoneManager = new RingtoneManager(getActivity());
        mRingtoneManager.setType(RingtoneManager.TYPE_ALARM);
    }

    @Override
    protected AlertDialog createFrom(AlertDialog.Builder builder) {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_RINGTONE_REQUEST_READ_EXTERNAL_STORAGE);
        }

        // TODO: We set the READ_EXTERNAL_STORAGE permission. Verify that this includes the user's
        // custom ringtone files.

        mRingtonesAdapter = null;



        int checkedItem = createRingtoneAdaptor();

        builder.setTitle(R.string.ringtones)
                .setSingleChoiceItems(mRingtonesAdapter, checkedItem, new DialogInterface.OnClickListener() {
                        @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mRingtone != null) {
                            destroyLocalPlayer();
                        }
                         if (which == 0) {
                             //Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);

                             Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

                             intent.addCategory(Intent.CATEGORY_OPENABLE);

                             intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                             intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION);

                             intent.setType("audio/*");

                             startActivityForResult(intent, SELECT_AUDIO_FILE_CODE);
                         } else {
                             // Here, 'which' param refers to the position of the item clicked.
                             mRingtoneUri = mRingtoneManager.getRingtoneUri(which-1);
                             mRingtone = new RingtoneLoop(getActivity(), mRingtoneUri);
                             mRingtone.play();
                         }

                    }
                });
        return super.createFrom(builder);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        destroyLocalPlayer();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_RINGTONE_URI, mRingtoneUri);
    }

    @Override
    protected void onOk() {
        if (mOnRingtoneSelectedListener != null) {
            // Here, 'which' param refers to the position of the item clicked.
            mOnRingtoneSelectedListener.onRingtoneSelected(mRingtoneUri);
        }
        dismiss();
    }

    public void setOnRingtoneSelectedListener(OnRingtoneSelectedListener onRingtoneSelectedListener) {
        mOnRingtoneSelectedListener = onRingtoneSelectedListener;
    }

    private void destroyLocalPlayer() {
        if (mRingtone != null) {
            mRingtone.stop();
            mRingtone = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            return;

        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SELECT_AUDIO_FILE_CODE:
                if (data != null && resultCode == RESULT_OK) {
                    // The document selected by the user won't be returned in the intent.
                    // Instead, a URI to that document will be contained in the return intent
                    // provided to this method as a parameter.
                    // Pull that URI using resultData.getData().
                    Uri uri = null;
                    if (data != null) {
                        uri = data.getData();

                        Log.i(TAG, "Uri: " + uri.toString());

                        //String ringtone = getAlarm().ringtone();

                        String title = RingtoneManager.getRingtone(getContext(), uri).getTitle(getContext());
                        if (title.lastIndexOf("/") >= 0) {
                            title = title.substring(title.lastIndexOf("/") + 1);
                        }
                        mRingtonesAdapter.remove(mCustomString);
                        mCustomString = String.format(getString(R.string.custom_ringtone), title);
                        mRingtonesAdapter.insert(mCustomString, 0);
                        mRingtonesAdapter.notifyDataSetChanged();

                        mRingtoneUri = uri;
                        mRingtone = new RingtoneLoop(getActivity(), mRingtoneUri);
                        mRingtone.play();

                        ContentResolver resolver = this.getActivity().getContentResolver();

                        resolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION);

                        /*
                        Cursor cursor = resolver.query(uri, null, null, null, null, null);

                        try {
                            if (cursor != null && cursor.moveToFirst()) {
                                String displayName = cursor.getString(
                                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                                Log.i(TAG, "Display Name: " + displayName);

                                mRingtonesAdapter.remove(mCustomString);
                                mCustomString = "[custom: " + displayName + "]";
                                mRingtonesAdapter.insert(mCustomString, 0);
                                mRingtonesAdapter.notifyDataSetChanged();

                               // resolver.revokeUriPermission(mRingtoneUri, FLAG_GRANT_READ_URI_PERMISSION);
                                mRingtoneUri = uri;
                                mRingtone = new RingtoneLoop(getActivity(), mRingtoneUri);
                                mRingtone.play();

                            }
                        } finally {
                            cursor.close();
                        }
                        */

                    }
                }

                break;
        }
    }

    private int createRingtoneAdaptor() {
        mRingtoneManager = new RingtoneManager(getActivity());
        mRingtoneManager.setType(RingtoneManager.TYPE_ALARM);

        Cursor cursor = mRingtoneManager.getCursor();
        int checkedItem = mRingtoneManager.getRingtonePosition(mRingtoneUri);
        String labelColumn = cursor.getColumnName(RingtoneManager.TITLE_COLUMN_INDEX);

        List<String> ringtoneList = new ArrayList<String>();

        mCustomString = getString(R.string.select_custom_ringtone);

        if (checkedItem < 0) {
            String title = RingtoneManager.getRingtone(getContext(), mRingtoneUri).getTitle(getContext());
            if (title.lastIndexOf("/") >= 0) {
                title = title.substring(title.lastIndexOf("/") + 1);
            }
            mCustomString = String.format(getString(R.string.custom_ringtone), title);

            checkedItem = 0;
        } else {
            checkedItem += 1;
        }
        ringtoneList.add(mCustomString);

        cursor.moveToFirst();
        while (cursor.isAfterLast() == false)
        {
            ringtoneList.add(cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX));
            cursor.moveToNext();
        }

        if (mRingtonesAdapter == null) {
            mRingtonesAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_single_choice, ringtoneList);
        } else {
            mRingtonesAdapter.clear();
            mRingtonesAdapter.addAll(ringtoneList);
        }
        return checkedItem;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_RINGTONE_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    int checkedItem = createRingtoneAdaptor();
                    mRingtonesAdapter.notifyDataSetChanged();
                    AlertDialog ad = (AlertDialog) getDialog();
                    ListView lv = ad.getListView();
                    lv.setItemChecked(checkedItem, true);
                }
                return;
            }
        }
    }
}
