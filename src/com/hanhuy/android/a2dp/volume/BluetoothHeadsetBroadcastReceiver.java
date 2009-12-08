package com.hanhuy.android.a2dp.volume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class BluetoothHeadsetBroadcastReceiver extends BroadcastReceiver {
    final static String ACTION_HEADSET_STATE_CHANGED =
        "android.bluetooth.headset.action.STATE_CHANGED";
    final static String PRE_2_0_ACTION_HEADSET_STATE_CHANGED =
        "android.bluetooth.intent.action.HEADSET_STATE_CHANGED";

    final static String TAG = "BluetoothHeadsetBroadcastReceiver";

    private final static String RINGER_VOLUME_KEY = "speaker_ringer_volume";
 
    // from android/bluetooth/BluetoothHeadset.java
    private final static int STATE_CONNECTED = 2;
    private final static int STATE_CONNECTING = 1;
    private final static int STATE_DISCONNECTED = 0;

    final static String EXTRA_STATE =
        "android.bluetooth.headset.extra.STATE";
    final static String EXTRA_PREVIOUS_STATE =
        "android.bluetooth.headset.extra.PREVIOUS_STATE";

    final static String PRE_2_0_EXTRA_STATE =
        "android.bluetooth.intent.HEADSET_STATE";
    final static String PRE_2_0_EXTRA_PREVIOUS_STATE =
        "android.bluetooth.intent.HEADSET_PREVIOUS_STATE";

    @Override
    public void onReceive(Context c, Intent i) {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(c);
        if (!prefs.getBoolean(
                c.getString(R.string.key_silence_ringer_flag), false)) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();

        boolean showUI = prefs.getBoolean(
                c.getString(R.string.key_show_ui_flag), false);

        AudioManager am = (AudioManager) c.getSystemService(
                Context.AUDIO_SERVICE);
        Bundle extras = i.getExtras();
        int state, oldState;
        int _volume, volume;
        if (extras.keySet().contains(EXTRA_STATE)) {
            state = extras.getInt(EXTRA_STATE);
            oldState = extras.getInt(EXTRA_PREVIOUS_STATE);
        } else {
            state = extras.getInt(PRE_2_0_EXTRA_STATE);
            oldState = extras.getInt(PRE_2_0_EXTRA_PREVIOUS_STATE);
        }
        switch (state) {
        case STATE_CONNECTED:
            _volume = am.getStreamVolume(AudioManager.STREAM_RING);
            editor.putInt(RINGER_VOLUME_KEY, _volume);
            editor.commit();

            am.setStreamVolume(AudioManager.STREAM_RING,
                    0, showUI ? AudioManager.FLAG_SHOW_UI : 0);
            break;
        case STATE_DISCONNECTED:
            if (oldState == STATE_CONNECTING) // failed connection, don't flip
                break;
            volume = prefs.getInt(RINGER_VOLUME_KEY, -1);

            if (volume != -1) {
                am.setStreamVolume(AudioManager.STREAM_RING,
                        volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
            }
            break;
        }
    }
}
