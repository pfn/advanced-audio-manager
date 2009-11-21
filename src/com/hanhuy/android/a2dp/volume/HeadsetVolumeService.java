package com.hanhuy.android.a2dp.volume;

import android.app.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class HeadsetVolumeService extends Service {
    private HeadsetBroadcastReceiver receiver = new HeadsetBroadcastReceiver();
    
    final static String TAG = "HeadsetVolumeService";
    
    private final static int STATE_HEADSET_NONE    = 0;
    // found in com/android/server/HeadsetObserver.java
    private final static int STATE_HEADSET_MIC     = 1;
    private final static int STATE_HEADSET_NO_MIC  = 2;
    
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
    
    private class HeadsetBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(context);
            boolean showUI = prefs.getBoolean(
                    context.getString(R.string.key_show_ui_flag), false) &&
                    !isInitialStickyBroadcast();
            boolean alreadyHasHeadset = prefs.getBoolean(
                    VolumePreference.HAS_HEADSET, false);
            SharedPreferences.Editor editor = prefs.edit();
            AudioManager am = (AudioManager) context.getSystemService(
                    AUDIO_SERVICE);
            int state   = intent.getIntExtra("state", -1);
            switch (state) {
            case STATE_HEADSET_NONE:
                editor.putBoolean(VolumePreference.HAS_HEADSET, false);
                if (!am.isBluetoothA2dpOn()) {
                    int _volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                    editor.putInt(VolumePreference.HEADSET_VOLUME_KEY, _volume);
                    int volume = prefs.getInt(
                            VolumePreference.SPEAKER_VOLUME_KEY, -1);
                    if (volume == -1) {
                        volume = _volume;
                    } else {
                        am.setStreamVolume(AudioManager.STREAM_MUSIC,
                                volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
                    }
                }
                break;
            case STATE_HEADSET_MIC:
            case STATE_HEADSET_NO_MIC:
                // don't re-set volume, will screw up speaker volume
                if (alreadyHasHeadset)
                    break;
                editor.putBoolean(VolumePreference.HAS_HEADSET, true);
                if (!am.isBluetoothA2dpOn()) {
                    int _volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                    editor.putInt(VolumePreference.SPEAKER_VOLUME_KEY, _volume);
                    int volume = prefs.getInt(
                            VolumePreference.HEADSET_VOLUME_KEY, -1);
                    if (volume == -1) {
                        volume = _volume;
                    } else {
                        am.setStreamVolume(AudioManager.STREAM_MUSIC,
                                volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
                    }
                }
                break;
            default: // FM_HEADSET and FM_SPEAKER, ignore them
                break;
            }
            editor.commit();
        }
    }
}
