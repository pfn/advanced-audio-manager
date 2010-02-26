package com.hanhuy.android.a2dp.volume;

import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;

import android.app.Service;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
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
                "isInitialStickyBroadcast"); //, (Class[]) null);
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
                    r = (Boolean) isInitialStickyBroadcast.invoke(this);
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
            boolean alreadyHasHeadset = prefs.getBoolean(
                    context.getString(R.string.pref_has_headset), false);
            boolean hasBtHeadset = prefs.getBoolean(
                    context.getString(R.string.pref_has_bt_headset), false);
            SharedPreferences.Editor editor = prefs.edit();
            int state = intent.getIntExtra("state", -1);
            
            AudioManager am = (AudioManager) context.getSystemService(
                    HeadsetVolumeService.AUDIO_SERVICE);
            boolean showUI = prefs.getBoolean(
                    context.getString(R.string.key_show_ui_flag), false) &&
                    !_isInitialStickyBroadcast();
            
            if (state == 0) {
                editor.putBoolean(
                        context.getString(R.string.pref_has_headset), false);
                // don't set volume, will screw up speaker and headset volumes
                if (!alreadyHasHeadset)
                    return;
                toggleMediaVolume(false, context, prefs, editor, am, showUI);
                toggleRingerVolume(false, context, prefs, editor, am,
                        hasBtHeadset, showUI);
                toggleCallVolume(false, context, prefs, editor, am,
                        hasBtHeadset, showUI);
            }
            if (state > 0) { // assume that anything >0 is a media headset (1.5)
                editor.putBoolean(
                        context.getString(R.string.pref_has_headset), true);
                if (alreadyHasHeadset)
                    return;
                toggleMediaVolume(true, context, prefs, editor, am, showUI);
                toggleRingerVolume(true, context, prefs, editor, am,
                        hasBtHeadset, showUI);
                toggleCallVolume(true, context, prefs, editor, am,
                        hasBtHeadset, showUI);
            }
            editor.commit();
        }
        private void toggleMediaVolume(boolean on, Context context,
                SharedPreferences prefs, SharedPreferences.Editor editor,
                AudioManager am, boolean showUI) {
            int stream = AudioManager.STREAM_MUSIC;
            if (on) {
                if (!am.isBluetoothA2dpOn()) {
                    int _volume = am.getStreamVolume(stream);
                    editor.putInt(context.getString(
                            R.string.pref_media_speaker), _volume);
                    int volume = prefs.getInt(
                            context.getString(R.string.pref_media_wired), -1);
                    if (volume != -1) {
                        am.setStreamVolume(stream,
                                volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
                    }
                }
            } else {
                if (!am.isBluetoothA2dpOn()) {
                    int _volume = am.getStreamVolume(stream);
                    editor.putInt(context.getString(R.string.pref_media_wired),
                            _volume);
                    int volume = prefs.getInt(
                            context.getString(R.string.pref_media_speaker), -1);
                    if (!prefs.getBoolean(context.getString(
                            R.string.key_unmute_speaker_flag), false)) {
                        volume = 0;
                    }
                    if (volume != -1) {
                        am.setStreamVolume(stream,
                                volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
                    }
                }
            }
        }
        private void toggleCallVolume(boolean on, Context context,
                SharedPreferences prefs, SharedPreferences.Editor editor,
                AudioManager am, boolean hasBtHeadset, boolean showUI) {
            int stream = AudioManager.STREAM_VOICE_CALL;
            ContentResolver cr = context.getContentResolver();
            if (on) {
                if (!hasBtHeadset) {
                    int _volume = am.getStreamVolume(stream);
                    editor.putInt(context.getString(
                            R.string.pref_call_normal), _volume);
                    int volume = prefs.getInt(
                            context.getString(R.string.pref_call_wired), -1);
                    if (volume != -1) {
                        am.setStreamVolume(stream,
                                volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
                        Settings.System.putInt(cr,
                                Settings.System.VOLUME_VOICE, volume);
                    }
                }
            } else {
                if (!hasBtHeadset) {
                    int _volume = am.getStreamVolume(stream);
                    editor.putInt(context.getString(R.string.pref_call_wired),
                            _volume);
                    int volume = prefs.getInt(
                            context.getString(R.string.pref_call_normal), -1);
                    if (volume != -1) {
                        am.setStreamVolume(stream,
                                volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
                        Settings.System.putInt(cr,
                                Settings.System.VOLUME_VOICE, volume);
                    }
                }
            }
        }
        private void toggleRingerVolume(boolean on, Context context,
                SharedPreferences prefs, SharedPreferences.Editor editor,
                AudioManager am, boolean hasBtHeadset, boolean showUI) {
            int stream = AudioManager.STREAM_RING;
            if (on) {
                if (!hasBtHeadset) {
                    int _volume = am.getStreamVolume(stream);
                    editor.putInt(context.getString(
                            R.string.pref_ringer_normal), _volume);
                    int volume = prefs.getInt(
                            context.getString(R.string.pref_ringer_wired), -1);
                    if (volume != -1) {
                        am.setStreamVolume(stream,
                                volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
                    }
                }
            } else {
                if (!hasBtHeadset) {
                    int _volume = am.getStreamVolume(stream);
                    editor.putInt(context.getString(R.string.pref_ringer_wired),
                            _volume);
                    int volume = prefs.getInt(
                            context.getString(R.string.pref_ringer_normal), -1);
                    if (volume != -1) {
                        am.setStreamVolume(stream,
                                volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
                    }
                }
            }
        }
    }
}