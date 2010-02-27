package com.hanhuy.android.a2dp.volume;

import android.content.BroadcastReceiver;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;

public class BluetoothHeadsetBroadcastReceiver extends BroadcastReceiver {
    final static String ACTION_HEADSET_STATE_CHANGED =
        "android.bluetooth.headset.action.STATE_CHANGED";
    final static String PRE_2_0_ACTION_HEADSET_STATE_CHANGED =
        "android.bluetooth.intent.action.HEADSET_STATE_CHANGED";

    final static String TAG = "BluetoothHeadsetBroadcastReceiver";

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
        SharedPreferences.Editor editor = prefs.edit();
        AudioManager am = (AudioManager) c.getSystemService(
                Context.AUDIO_SERVICE);

        boolean showUI = prefs.getBoolean(
                c.getString(R.string.key_show_ui_flag), false);

        Bundle extras = i.getExtras();
        int state, oldState;
        if (extras.keySet().contains(EXTRA_STATE)) {
            state = extras.getInt(EXTRA_STATE);
            oldState = extras.getInt(EXTRA_PREVIOUS_STATE);
        } else {
            state = extras.getInt(PRE_2_0_EXTRA_STATE);
            oldState = extras.getInt(PRE_2_0_EXTRA_PREVIOUS_STATE);
        }
        switch (state) {
        case STATE_CONNECTED:
            editor.putBoolean(
                    c.getString(R.string.pref_has_bt_headset), true);
            toggleRingerVolume(false, c, prefs, editor, am, showUI);
            toggleCallVolume(true, c, prefs, editor, am, showUI);
            break;
        case STATE_DISCONNECTED:
            if (oldState == STATE_CONNECTING) // failed connection, don't flip
                break;
            editor.putBoolean(
                    c.getString(R.string.pref_has_bt_headset), false);
            toggleRingerVolume(true, c, prefs, editor, am, showUI);
            toggleCallVolume(false, c, prefs, editor, am, showUI);
            break;
        }
    }
    private static void toggleRingerVolume(boolean on, Context c,
            SharedPreferences prefs, SharedPreferences.Editor editor,
            AudioManager am, boolean showUI) {
        if (!prefs.getBoolean(
                c.getString(R.string.key_silence_ringer_flag), false)) {
            return;
        }
        int stream = AudioManager.STREAM_RING;
        int _volume, volume;

        if (on) {
            volume = prefs.getInt(c.getString(R.string.pref_ringer_normal), -1);

            if (volume != -1) {
                BroadcastUtil.changeVolume(c, am, showUI, stream, volume);
            }
        } else {
            _volume = am.getStreamVolume(stream);
            editor.putInt(c.getString(R.string.pref_ringer_normal), _volume);
            editor.commit();

            BroadcastUtil.changeVolume(c, am, showUI, stream, 0);
        }
    }
    private static void toggleCallVolume(boolean on, Context c,
            SharedPreferences prefs, SharedPreferences.Editor editor,
            AudioManager am, boolean showUI) {
        int stream = AudioManager.STREAM_VOICE_CALL;
        boolean hasHeadset = prefs.getBoolean(
                c.getString(R.string.pref_has_headset), false);
        String key = hasHeadset ? c.getString(R.string.pref_call_wired) :
            c.getString(R.string.pref_call_normal);
        int volume, _volume;
        ContentResolver cr = c.getContentResolver();
        if (on) {
            volume = prefs.getInt(
                    c.getString(R.string.pref_call_bluetooth), -1);
            _volume = am.getStreamVolume(stream);

            editor.putInt(key, _volume);
            editor.commit();

            if (volume != -1) {
                am.setStreamVolume(stream,
                        volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
                Settings.System.putInt(cr,
                        Settings.System.VOLUME_VOICE, volume);
            }
        } else {
            volume = prefs.getInt(key, -1);
            _volume = am.getStreamVolume(stream);

            editor.putInt(c.getString(R.string.pref_call_bluetooth), _volume);
            editor.commit();

            if (volume != -1) {
                am.setStreamVolume(stream,
                        volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
                Settings.System.putInt(cr,
                        Settings.System.VOLUME_VOICE, volume);
            }
        }
    }
}
