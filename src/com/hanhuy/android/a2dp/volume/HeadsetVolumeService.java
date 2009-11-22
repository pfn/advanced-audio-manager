package com.hanhuy.android.a2dp.volume;

import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;

import android.app.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class HeadsetVolumeService extends Service {
    private HeadsetBroadcastReceiver receiver = new HeadsetBroadcastReceiver();

    final static String TAG = "HeadsetVolumeService";

    final static int STATE_HEADSET_NONE    = 0;
    // found in com/android/server/HeadsetObserver.java
    final static int STATE_HEADSET_MIC     = 1;
    final static int STATE_HEADSET_NO_MIC  = 2;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(receiver, filter);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private static class HeadsetBroadcastReceiver extends BroadcastReceiver {

        static {
            Method m = null;
            try {
                m = BroadcastReceiver.class.getDeclaredMethod(
                        "isInitialStickyBroadcast", (Class<?>) null);
            } catch (SecurityException e) {
            } catch (NoSuchMethodException e) {
                Log.i(TAG, "no BroadcastReceiver.isInitialStickyBroadcast()");
            }
            isInitialStickyBroadcast = m;
        }

        private final static Method isInitialStickyBroadcast;

        private boolean _isInitialStickyBroadcast() {
            boolean r = false;
            if (isInitialStickyBroadcast != null) {
                try {
                    r = (Boolean) isInitialStickyBroadcast.invoke(
                            this, (Object) null);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "isInitialStickyBroadcast", e);
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "isInitialStickyBroadcast", e);
                } catch (InvocationTargetException e) {
                    Log.e(TAG, "isInitialStickyBroadcast", e);
                }
            }
            return r;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
            boolean showUI = prefs.getBoolean(
                    context.getString(R.string.key_show_ui_flag), false) &&
                    !_isInitialStickyBroadcast();
            boolean alreadyHasHeadset = prefs.getBoolean(
                    VolumePreference.HAS_HEADSET, false);
            SharedPreferences.Editor editor = prefs.edit();
            AudioManager am = (AudioManager) context.getSystemService(
                    HeadsetVolumeService.AUDIO_SERVICE);
            int state   = intent.getIntExtra("state", -1);
            if (state == 0) {
                // don't set volume, will screw up speaker and headset volumes
                if (!alreadyHasHeadset)
                    return;
                editor.putBoolean(VolumePreference.HAS_HEADSET, false);
                if (!am.isBluetoothA2dpOn()) {
                    int _volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                    editor.putInt(VolumePreference.HEADSET_VOLUME_KEY, _volume);
                    int volume = prefs.getInt(
                            VolumePreference.SPEAKER_VOLUME_KEY, -1);
                    if (volume != -1) {
                        if (!prefs.getBoolean(context.getString(
                                R.string.key_unmute_speaker_flag), false)) {
                            volume = 0;
                        }
                        am.setStreamVolume(AudioManager.STREAM_MUSIC,
                                volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
                    }
                }
            }
            if (state > 0) { // assume that anything >0 is a media headset (1.5)
                if (alreadyHasHeadset)
                    return;
                editor.putBoolean(VolumePreference.HAS_HEADSET, true);
                if (!am.isBluetoothA2dpOn()) {
                    int _volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                    editor.putInt(VolumePreference.SPEAKER_VOLUME_KEY, _volume);
                    int volume = prefs.getInt(
                            VolumePreference.HEADSET_VOLUME_KEY, -1);
                    if (volume != -1) {
                        am.setStreamVolume(AudioManager.STREAM_MUSIC,
                                volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
                    }
                }
            }
            editor.commit();
        }
    }
}