package com.hanhuy.android.a2dp.volume;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class UserPresentBroadcastReceiver extends BroadcastReceiver {
    final static String TAG = "UserPresentBroadcastReceiver";

    @Override
    public void onReceive(Context c, Intent i) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(c);
        boolean startService = prefs.getBoolean(
                c.getString(R.string.key_enable_headset_service_flag), true);
        if (startService) {
            ComponentName service = c.startService(
                    new Intent(c, HeadsetVolumeService.class));
            if (service == null)
                Log.e(TAG, "Unable to start service");
        }
    }
}
