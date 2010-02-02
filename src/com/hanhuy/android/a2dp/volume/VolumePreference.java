package com.hanhuy.android.a2dp.volume;

import static android.media.AudioManager.STREAM_MUSIC;
import static android.media.AudioManager.STREAM_RING;
import static android.media.AudioManager.STREAM_VOICE_CALL;
import static android.media.AudioManager.STREAM_NOTIFICATION;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
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
    
    private final static int VOLUME_TYPE_NORMAL    = 0;
    private final static int VOLUME_TYPE_WIRED     = 1;
    private final static int VOLUME_TYPE_BLUETOOTH = 2;
    
    private int streamType;
    private int volumeType;

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
                getContext().getString(R.string.pref_has_headset), false);
        boolean hasBtHeadset = getSharedPreferences().getBoolean(
                getContext().getString(R.string.pref_has_bt_headset), false);

        switch (streamType) {
        case STREAM_MUSIC:
            if (volumeType == VOLUME_TYPE_BLUETOOTH
                    && mVolume.isBluetoothA2dpOn())
                return true;
            if (volumeType == VOLUME_TYPE_WIRED && hasHeadset &&
                    !mVolume.isBluetoothA2dpOn())
                return true;
            if (volumeType == VOLUME_TYPE_NORMAL &&
                    !mVolume.isBluetoothA2dpOn() &&
                    !hasHeadset)
                return true;
            break;
        case STREAM_VOICE_CALL:
            if (volumeType == VOLUME_TYPE_BLUETOOTH && hasBtHeadset)
                return true;
            if (volumeType == VOLUME_TYPE_WIRED && hasHeadset && !hasBtHeadset)
                return true;
            if (volumeType == VOLUME_TYPE_NORMAL && !hasHeadset &&
                    !hasBtHeadset)
                return true;
        case STREAM_RING:
            if (volumeType == VOLUME_TYPE_WIRED && hasHeadset && !hasBtHeadset)
                return true;
            if (volumeType == VOLUME_TYPE_NORMAL && !hasHeadset &&
                    !hasBtHeadset)
                return true;
        }
        return false;
    }
    private ContentObserver mVolumeObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            if (mSeekBar != null && isCurrentOutput()) {
                mSeekBar.setProgress(Settings.System.getInt(mContentResolver,
                        Settings.System.VOLUME_SETTINGS[streamType], 0));
            }
        }
    };
    
    public VolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.VolumePreference);
        int stream = a.getInt(R.styleable.VolumePreference_stream, 0);
        volumeType = a.getInt(R.styleable.VolumePreference_type, 0);
        a.recycle();
        switch (stream) {
        case 0:  streamType = STREAM_MUSIC;        break;
        case 1:  streamType = STREAM_RING;         break;
        case 2:  streamType = STREAM_VOICE_CALL;   break;
        case 3:  streamType = STREAM_NOTIFICATION; break;
        default: streamType = STREAM_MUSIC;
        }
        
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
            volume = Math.min(mVolume.getStreamMaxVolume(streamType),
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
        seekBar.setMax(mVolume.getStreamMaxVolume(streamType));
        seekBar.requestFocus();
        mOriginalStreamVolume = getPersistedInt(-1);
        if (mOriginalStreamVolume == -1 || isCurrentOutput()) {
            mOriginalStreamVolume = mVolume.getStreamVolume(streamType);
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
                Settings.System.VOLUME_SETTINGS[streamType]),
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
            mVolume.setStreamVolume(streamType, value,
                    AudioManager.FLAG_PLAY_SOUND);
    }
}
