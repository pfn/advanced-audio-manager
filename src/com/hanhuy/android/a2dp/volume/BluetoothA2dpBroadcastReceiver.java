package com.hanhuy.android.a2dp.volume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class BluetoothA2dpBroadcastReceiver extends BroadcastReceiver {
    final static String ACTION_A2DP_STATE_CHANGED =
        "android.bluetooth.a2dp.action.SINK_STATE_CHANGED";
    final static String PRE_2_0_ACTION_A2DP_STATE_CHANGED =
        "android.bluetooth.a2dp.intent.action.SINK_STATE_CHANGED";

    final static String TAG = "BluetoothA2dpBroadcastReceiver";

    // from android/bluetooth/BluetoothA2dp.java
    private final static int STATE_CONNECTED = 2;
    private final static int STATE_CONNECTING = 1;
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

        Bundle extras = i.getExtras();
        int state;
        int oldState;
        if (extras.keySet().contains(EXTRA_STATE)) {
            state = extras.getInt(EXTRA_STATE);
            oldState = extras.getInt(EXTRA_PREVIOUS_STATE);
        } else {
            state = extras.getInt(PRE_2_0_EXTRA_STATE);
            oldState = extras.getInt(PRE_2_0_EXTRA_PREVIOUS_STATE);
        }
        switch (state) {
        case STATE_CONNECTED:
            if (oldState == STATE_PLAYING)
                break;
            toggleMediaVolume(true, c, prefs, editor);
            break;
        case STATE_DISCONNECTED:
            if (oldState == STATE_CONNECTING) // a failed connection
                break;
            toggleMediaVolume(false, c, prefs, editor);
            break;
        }
    }

    private static void toggleMediaVolume(boolean on, Context c,
            SharedPreferences prefs, SharedPreferences.Editor editor) {
        boolean hasHeadset = prefs.getBoolean(
                c.getString(R.string.pref_has_headset), false);
        String key = hasHeadset ? c.getString(R.string.pref_media_wired) :
            c.getString(R.string.pref_media_speaker);
        AudioManager am = (AudioManager) c.getSystemService(
                Context.AUDIO_SERVICE);
        boolean showUI = prefs.getBoolean(
                c.getString(R.string.key_show_ui_flag), false);

        int volume, _volume;
        if (on) {
            volume = prefs.getInt(
                    c.getString(R.string.pref_media_bluetooth), -1);
            _volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);

            editor.putInt(key, _volume);
            editor.commit();

            if (volume != -1) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC,
                        volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
            }
        } else {
            volume = prefs.getInt(key, -1);
            _volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);

            editor.putInt(c.getString(R.string.pref_media_bluetooth), _volume);
            editor.commit();

            if (!hasHeadset && !prefs.getBoolean(c.getString(
                    R.string.key_unmute_speaker_flag), false)) {
                volume = 0;
            }
            if (volume != -1) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC,
                        volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
            }

        }
    }
}
