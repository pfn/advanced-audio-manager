package com.hanhuy.android.a2dp.volume;

import static android.media.AudioManager.STREAM_MUSIC;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class VolumePreference extends DialogPreference
implements SeekBar.OnSeekBarChangeListener, Runnable,
DialogInterface.OnKeyListener {

    final static String TAG = "A2DPVolumePreference";
    
    public final static String HAS_HEADSET = "has_headset";

    public final static String HEADSET_VOLUME_KEY = "wiredvolume";
    public final static String SPEAKER_VOLUME_KEY = "speakervolume";
    public final static String A2DP_VOLUME_KEY    = "a2dpvolume";

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    protected static SeekBar getSeekBar(View dialogView) {
        return (SeekBar) dialogView.findViewById(R.id.volume_seekbar);
    }

    private ContentResolver mContentResolver;
    private Handler mHandler = new Handler();

    private AudioManager mVolume;
    private int mOriginalStreamVolume; 

    private int mLastProgress;
    private SeekBar mSeekBar;
    
    private boolean isCurrentOutput() {
        boolean hasHeadset = getSharedPreferences().getBoolean(
                HAS_HEADSET, false);
        String key = getKey();
        if (A2DP_VOLUME_KEY.equals(key) && mVolume.isBluetoothA2dpOn())
            return true;
        if (HEADSET_VOLUME_KEY.equals(key) &&
                hasHeadset &&
                !mVolume.isBluetoothA2dpOn())
            return true;
        if (SPEAKER_VOLUME_KEY.equals(key) &&
                !mVolume.isBluetoothA2dpOn() &&
                !hasHeadset)
            return true;
        return false;
    }
    private ContentObserver mVolumeObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            if (mSeekBar != null && isCurrentOutput()) {
                mSeekBar.setProgress(Settings.System.getInt(mContentResolver,
                        Settings.System.VOLUME_SETTINGS[STREAM_MUSIC], 0));
            }
        }
    };
    
    public VolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        setPersistent(true);
        
        setDialogLayoutResource(R.layout.volume_layout);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        
        mContentResolver = context.getContentResolver();
        
        mVolume = (AudioManager) context.getSystemService(
                Context.AUDIO_SERVICE);
    }
    
    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        getDialog().setOnKeyListener(this);
    }
    @Override
    public boolean onKey(DialogInterface di, int keyCode,
            KeyEvent event) {
        int action = event.getAction();
        int volume;
        switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            volume = Math.min(mVolume.getStreamMaxVolume(STREAM_MUSIC),
                    mSeekBar.getProgress() - 1);
            break;
        case KeyEvent.KEYCODE_VOLUME_UP:
            volume = Math.max(0, mSeekBar.getProgress() + 1);
            break;
        default:
            return false;
        }
        if (KeyEvent.ACTION_DOWN == action) {
            mSeekBar.setProgress(volume);
            postSetVolume(volume);
        }
        return true;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
    
        final SeekBar seekBar = mSeekBar =
                (SeekBar) view.findViewById(R.id.volume_seekbar);
        seekBar.setMax(mVolume.getStreamMaxVolume(STREAM_MUSIC));
        seekBar.requestFocus();
        mOriginalStreamVolume = getPersistedInt(-1);
        if (mOriginalStreamVolume == -1 || isCurrentOutput()) {
            mOriginalStreamVolume = mVolume.getStreamVolume(STREAM_MUSIC);
        }
        mLastProgress = mOriginalStreamVolume;
        seekBar.setProgress(mOriginalStreamVolume);
        seekBar.setOnSeekBarChangeListener(this);
        
        CharSequence cs = getDialogMessage();
        if (cs != null) {
            TextView tv = (TextView) view.findViewById(R.id.volume_text);
            tv.setText(cs);
        }
        
        mContentResolver.registerContentObserver(Settings.System.getUriFor(
                Settings.System.VOLUME_SETTINGS[STREAM_MUSIC]),
                false, mVolumeObserver);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        
        if (!positiveResult && mOriginalStreamVolume != mLastProgress) {
            setVolume(mOriginalStreamVolume);
        } else {
            persistInt(mLastProgress);
        }
        
        mSeekBar = null;
    }
    
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromTouch) {
        if (!fromTouch) {
            return;
        }

        postSetVolume(progress);
    }

    private void postSetVolume(int progress) {
        // Do the volume changing separately to give responsive UI
        mLastProgress = progress;
        mHandler.removeCallbacks(this);
        if (isCurrentOutput())
            mHandler.post(this);
    }
    
    public void run() {
        setVolume(mLastProgress);
    }
    
    private void setVolume(int value) {
        if (isCurrentOutput())
            mVolume.setStreamVolume(STREAM_MUSIC, value,
                    AudioManager.FLAG_PLAY_SOUND);
    }
}
