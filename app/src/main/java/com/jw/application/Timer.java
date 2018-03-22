package com.jw.application;

/**
 * Created by John Williams on 2018/3/21.
 */


import java.util.TimerTask;


import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

public class Timer {

    public long time, timeStart, timeEnd;
    protected java.util.Timer myTimer;
    protected TimerTask timerTask = null;
    TimeHandler timeh;
    private int mode;
    private AppCompatActivity parent;

    public Timer(AppCompatActivity parent, int mode) {
        timeh = new TimeHandler();
        myTimer = new java.util.Timer();
        this.parent = parent;
        this.mode = mode;
    }

    public void count(){
        if(timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        timerTask = new ClockTask();
        //timeStart = System.currentTimeMillis();
        myTimer.schedule(timerTask, 0, 17);
    }

    private class ClockTask extends TimerTask {
        @Override
        public void run() {
            time = System.currentTimeMillis() - timeStart;
            timeh.sendEmptyMessage(0);
        }
    }


    //---------------------
    private String contime(int i) {
        boolean m = i<0;
        if(m) i = -i;
        int milli = i % 1000;
        i /= 1000;
        int sec = i, min = 0, hour = 0;
        min = sec / 60;
        sec = sec % 60;
        hour = sec / 60;
        sec = sec % 60;
        StringBuilder time = new StringBuilder();
        if(hour == 0) {
            if(min == 0) time.append(""+sec);
            else {
                if(sec<10) time.append(min+":0"+sec);
                else time.append(min+":"+sec);
            }
        }
        else {
            time.append(""+hour);
            if(min<10) time.append(":0"+min);
            else time.append(":"+min);
            if(sec<10) time.append(":0"+sec);
            else time.append(":"+sec);
        }
        time.append("."+milli);
        return time.toString();
    }
    //--------------------

    @SuppressLint("HandlerLeak")
    class TimeHandler extends Handler {
        public void handleMessage (Message msg) {
            if (msg.what == 0) {
                if (mode == 1){
                    ((GameActivity)parent).setTimer(contime((int) time));
                }
                else if (mode == 2){
                    ((RubikActivity)parent).setTimer(contime((int) time));
                }
            }
        }
    }
}

