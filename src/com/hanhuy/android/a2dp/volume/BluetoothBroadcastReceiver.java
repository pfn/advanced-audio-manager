package com.hanhuy.android.a2dp.volume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {
    final static String ACTION_A2DP_STATE_CHANGED =
        "android.bluetooth.a2dp.action.SINK_STATE_CHANGED";
    final static String PRE_2_0_ACTION_A2DP_STATE_CHANGED =
        "android.bluetooth.a2dp.intent.action.SINK_STATE_CHANGED";

    final static String TAG = "A2DPVolumeSettingsBroadcastReceiver";

    private final static int STATE_CONNECTED = 2;
    private final static int STATE_DISCONNECTED = 0;

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
        AudioManager am = (AudioManager) c.getSystemService(
                Context.AUDIO_SERVICE);
        Bundle extras = i.getExtras();
        Log.v(TAG, "a2dp state changed");
        int state;
        int oldState;
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
                int maxVolume = am.getStreamMaxVolume(
                        AudioManager.STREAM_MUSIC);
                am.setStreamVolume(AudioManager.STREAM_MUSIC,
                        maxVolume, 0);
                Log.v(TAG, "setting volume to max: " + maxVolume);
                break;
            case STATE_DISCONNECTED:
                Log.v(TAG, "setting volume to 0");
                am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                break;
            }
            Log.v(TAG, String.format(
                    "state changed: %d to %d\n", oldState, state));
        }
    }
}
