package com.hanhuy.android.a2dp.volume;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

public class BroadcastUtil {
    
    final static String TAG = "AAMBroadcastUtil";
    
    public final static String ACTION_VOLUME_UPDATE =
            "org.openintents.audio.action_volume_update";
    public final static String EXTRA_AUDIO_STREAM =
            "org.openintents.audio.extra_stream_type";
    public final static String EXTRA_VOLUME_INDEX =
            "org.openintents.audio.extra_volume_index";
    private BroadcastUtil() { } // no constructor
    
    public static void changeVolume(Context ctx, final AudioManager am,
            final boolean showUI, final int stream, final int index) {
        Intent bcast = new Intent(ACTION_VOLUME_UPDATE);
        bcast.putExtra(EXTRA_AUDIO_STREAM, stream);
        bcast.putExtra(EXTRA_VOLUME_INDEX, index);
        
        BroadcastReceiver r = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                am.setStreamVolume(stream,
                        index, showUI ? AudioManager.FLAG_SHOW_UI : 0);
            }
            
        };
        ctx.sendOrderedBroadcast(bcast, null, r,
                null, Activity.RESULT_OK, null, null);
    }
}
