package com.jw.lib.alarmmanager.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;

/**
 * Created by loongggdroid on 2016/3/21.
 */

public class LoongggAlarmReceiver extends BroadcastReceiver {
    public static String TAG = "LAR";
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        String msg = intent.getStringExtra("msg");
        long intervalMillis = intent.getLongExtra("intervalMillis", 0);
        Log.e(TAG, "LAR: intervalMillis:" + String.valueOf(intervalMillis));
        Log.e(TAG, "currentTimeMillis: " + String.valueOf(System.currentTimeMillis()));
        if (intervalMillis != 0) {
            AlarmManagerUtil.setAlarmTime(context, System.currentTimeMillis() + intervalMillis,
                    intent);
        }
        int flag = intent.getIntExtra("soundOrVibrator", 0);
        //here start the clock_alarm_activity
        Intent clockIntent = new Intent(context, ClockAlarmActivity.class);
        clockIntent.putExtra("msg", msg);
        clockIntent.putExtra("flag", flag);
        clockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(clockIntent);
    }


}
