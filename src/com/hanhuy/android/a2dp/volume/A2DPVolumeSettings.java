package com.hanhuy.android.a2dp.volume;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class A2DPVolumeSettings extends PreferenceActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        addPreferencesFromResource(R.xml.preferences);
    }
}