package com.hanhuy.android.a2dp.volume;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class A2DPVolumeSettings extends PreferenceActivity {
    final static String TAG = "A2DPVolumeSettings";
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        ComponentName service = startService(
                new Intent(this, HeadsetVolumeService.class));
        if (service == null)
            Log.e(TAG, "Unable to start service");
        
        addPreferencesFromResource(R.xml.preferences);
    }
}