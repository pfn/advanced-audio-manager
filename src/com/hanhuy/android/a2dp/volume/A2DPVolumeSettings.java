package com.hanhuy.android.a2dp.volume;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class A2DPVolumeSettings extends PreferenceActivity {
    final static String TAG = "A2DPVolumeSettings";
    
    private boolean startServicePrefState;
    
    private SharedPreferences prefs;
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        addPreferencesFromResource(R.xml.preferences);
    }
    
    private void startService() {
        ComponentName service = startService(
                new Intent(this, HeadsetVolumeService.class));
        if (service == null)
            Log.e(TAG, "Unable to start service");
    }

    private void stopService() {
        boolean stopped = stopService(
                new Intent(this, HeadsetVolumeService.class));
        if (!stopped)
            Log.e(TAG, "Unable to stop service");
    }
    
    @Override
    public void onResume() {
        super.onResume();
        boolean startService = prefs.getBoolean(
                getString(R.string.key_enable_headset_service_flag), true);
        
        if (startService)
            startService();
        startServicePrefState = startService;
    }

    @Override
    public void onPause() {
        super.onPause();
        boolean startService = prefs.getBoolean(
                getString(R.string.key_enable_headset_service_flag), true);

        if (startService != startServicePrefState) {
            enableReceiver(startService);
            if (startService) {
                startService();
            } else {
                stopService();
            }
        }
    }
    
    private void enableReceiver(boolean enable) {
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(
                "com.hanhuy.android.a2dp.volume",
                ".UserPresentBroadcastReceiver"),
                enable ?  PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
    }
}