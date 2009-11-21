package com.hanhuy.android.a2dp.volume;

import android.content.BroadcastReceiver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {
    final static String ACTION_A2DP_STATE_CHANGED =
        "android.bluetooth.a2dp.action.SINK_STATE_CHANGED";
    final static String PRE_2_0_ACTION_A2DP_STATE_CHANGED =
        "android.bluetooth.a2dp.intent.action.SINK_STATE_CHANGED";

    final static String TAG = "A2DPVolumeSettingsBroadcastReceiver";

    // from android/bluetooth/BluetoothA2dp.java
    private final static int STATE_CONNECTED = 2;
    private final static int STATE_DISCONNECTED = 0;
    private final static int STATE_PLAYING = 4;

    private final static String EXTRA_STATE =
        "android.bluetooth.a2dp.extra.SINK_STATE";
    private final static String EXTRA_PREVIOUS_STATE =
        "android.bluetooth.a2dp.extra.PREVIOUS_SINK_STATE";

    private final static String PRE_2_0_EXTRA_STATE =
            "android.bluetooth.a2dp.intent.SINK_STATE";
    private final static String PRE_2_0_EXTRA_PREVIOUS_STATE =
            "android.bluetooth.a2dp.intent.SINK_PREVIOUS_STATE";

    @Override
    public void onReceive(Context c, Intent i) {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        
        boolean showUI = prefs.getBoolean(
                c.getString(R.string.key_show_ui_flag), false);
        boolean hasHeadset = prefs.getBoolean(
                VolumePreference.HAS_HEADSET, false);
        String key = hasHeadset ? VolumePreference.HEADSET_VOLUME_KEY :
            VolumePreference.SPEAKER_VOLUME_KEY;

        AudioManager am = (AudioManager) c.getSystemService(
                Context.AUDIO_SERVICE);
        Bundle extras = i.getExtras();
        int state;
        int oldState;
        int _volume, volume;
        if (extras.keySet().contains(EXTRA_STATE)) {
            state = extras.getInt(EXTRA_STATE);
            oldState = extras.getInt(EXTRA_PREVIOUS_STATE);
        } else {
            state = extras.getInt(PRE_2_0_EXTRA_STATE);
            oldState = extras.getInt(PRE_2_0_EXTRA_PREVIOUS_STATE);
        }
        if (state != oldState) {
            switch (state) {
            case STATE_CONNECTED:
                if (oldState == STATE_PLAYING)
                    break;
                volume = prefs.getInt(VolumePreference.A2DP_VOLUME_KEY, -1);
                _volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);

                editor.putInt(key, _volume);
                editor.commit();

                if (volume != -1) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC,
                            volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
                }
                break;
            case STATE_DISCONNECTED:
                volume = prefs.getInt(key, -1);
                _volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                
                editor.putInt(VolumePreference.A2DP_VOLUME_KEY, _volume);
                editor.commit();

                if (volume != -1) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC,
                            volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
                }
                break;
            }
        }
    }
}
