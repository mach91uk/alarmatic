package uk.mach91.autoalarm.util;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FullBackupDataOutput;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;

import uk.mach91.autoalarm.R;

public class BackupAgentUtil extends BackupAgent {

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
    }

    @Override
    public void onRestore(BackupDataInput data,
                          int appVersionCode,
                          ParcelFileDescriptor newState) {
    }

    @Override
    public void onRestoreFinished () {
        SharedPreferences prefSetts = PreferenceManager.getDefaultSharedPreferences(this);

//        boolean askedUserExp = prefSetts.getBoolean(getString(R.string.key_asked_user_experience_program), false);

        SharedPreferences.Editor prefsEditor2 = prefSetts.edit();
//        prefsEditor2.putBoolean(getString(R.string.key_user_experience_program), false);
//        prefsEditor2.remove(getString(R.string.key_user_experience_program));
//        prefsEditor2.remove(getString(R.string.key_asked_user_experience_program));
        prefsEditor2.remove(getString(R.string.key_battery_optimisation));
        prefsEditor2.remove(getString(R.string.key_default_alarm_tone_picker));

        prefsEditor2.commit();

    }

}