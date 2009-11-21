package com.hanhuy.android.a2dp.volume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UserPresentBroadcastReceiver extends BroadcastReceiver {
    final static String TAG = "UserPresentBroadcastReceiver";

    @Override
    public void onReceive(Context c, Intent i) {
        ComponentName service = c.startService(
                new Intent(c, HeadsetVolumeService.class));
        if (service == null)
            Log.e(TAG, "Unable to start service");
    }
}
