package com.jw.activities;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.pickerview.TimePickerView;
import com.jw.AnimCubeLib.AnimCube;
import com.jw.alarmmanager.clock.SimpleDialog;
import com.jw.alarmmanager.clock.view.SelectRemindCyclePopup;
import com.jw.alarmmanager.clock.view.SelectRemindWayPopup;
import com.jw.blesample.comm.Observer;
import com.jw.fastble.BleManager;
import com.jw.fastble.callback.BleNotifyCallback;
import com.jw.fastble.data.BleDevice;
import com.jw.fastble.exception.BleException;
import com.jw.alarmmanager.clock.AlarmManagerUtil;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;


public class AlarmSettingActivity extends AppCompatActivity implements
        View.OnClickListener,
        AnimCube.OnCubeModelUpdatedListener,
        AnimCube.OnCubeAnimationFinishedListener,
        Observer {

    public static final String KEY_DATA = "key_data";
    public static final String Anim_Cube = "AnimCube_data";
    public static String TAG = "Alarm";
    private static final UUID uuidService = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID uuidCharacter = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private TextView date_tv;
    private TimePickerView pvTime;
    private RelativeLayout repeat_rl, ring_rl;
    private TextView tv_repeat_value, tv_ring_value;
    private LinearLayout allLayout;
    private Button set_btn;
    private String time;
    private int cycle;
    private int ring = 0;
    private Toolbar toolbar;
    private AnimCube animCube;

    private BleDevice bleDevice;
    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic characteristic;
    private int charaProp;
    public boolean anim_available = false;

    private Queue<String> move_seqs;
    public static AlarmHandler alarmHandler;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private boolean timing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "oncreate");
        setContentView(R.layout.activity_alarm);
        initData();
        initView();
        if (MainActivity.Debug_alarming == false){
        initBLE();
        }
        //时间选择后回调
        pvTime.setOnTimeSelectListener(new TimePickerView.OnTimeSelectListener() {

            @Override
            public void onTimeSelect(Date date) {
                time = getTime(date);
                date_tv.setText(time);
            }
        });

        date_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pvTime.show();
            }
        });
    }

    @Override
    public void disConnected(BleDevice device) {
        if (device != null && bleDevice != null && device.getKey().equals(bleDevice.getKey())) {
            finish();
        }
    }

    private void initData(){
        alarmHandler = new AlarmHandler(this);
        move_seqs = new LinkedList<>();
        animCube = (AnimCube) findViewById(R.id.animcube);
        animCube.setOnCubeModelUpdatedListener(this);
        animCube.setOnAnimationFinishedListener(this);
        animCube.setParent(this);
        animCube.setHandler(alarmHandler);
    }

    private void initBLE() {
        bleDevice = getIntent().getParcelableExtra(KEY_DATA);
        if (bleDevice == null)
            finish();
        BluetoothGatt gatt = BleManager.getInstance().getBluetoothGatt(bleDevice);
        bluetoothGattService = gatt.getService(uuidService);
        characteristic = bluetoothGattService.getCharacteristic(uuidCharacter);
        charaProp = characteristic.getProperties();
        BleManager.getInstance().notify(
                bleDevice,
                characteristic.getService().getUuid().toString(),
                characteristic.getUuid().toString(),
                new BleNotifyCallback() {

                    @Override
                    public void onNotifySuccess() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
                    }

                    @Override
                    public void onNotifyFailure(final BleException exception) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!anim_available)
                                    return;

//                                String seq = HexUtil.formatHexString(characteristic.getValue());
                                String seq = animCube.decode(characteristic.getValue()[0]);
//                                int code = characteristic.getValue()[0] & random.nextInt();
                                //receive BLE data from the cube
//                                int code = random.nextInt(16);
//                                code = code % 16;
//                                code = (code < 0) ? code + 16 : code;
//                                code = (code < 12) ? available_codes[code] : 0;
//                                String seq;
//                                seq = animCube.decode(code);
//                                if (!seq.equals("\n") || (message.getText().charAt(message.getText().length() - 1) == '\n')){
//                                    setMessage(seq);
//                                }
//                                if (!seq.equals("") && !isPlaying){
//                                    startTiming();
//                                }
                                move_seqs.offer(seq);
                                runTurn();

//                                setMessage(HexUtil.formatHexString(characteristic.getValue()));
                            }
                        });
                    }
                });
    }

    public void initView() {
        toolbar = (Toolbar) findViewById(R.id.alarmToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        allLayout = (LinearLayout) findViewById(R.id.all_layout);
        set_btn = (Button) findViewById(R.id.set_btn);
        set_btn.setOnClickListener(this);
        date_tv = (TextView) findViewById(R.id.date_tv);
        repeat_rl = (RelativeLayout) findViewById(R.id.repeat_rl);
        repeat_rl.setOnClickListener(this);
        ring_rl = (RelativeLayout) findViewById(R.id.ring_rl);
        ring_rl.setOnClickListener(this);
        tv_repeat_value = (TextView) findViewById(R.id.tv_repeat_value);
        tv_ring_value = (TextView) findViewById(R.id.tv_ring_value);
        tv_ring_value.setText("振动");
        pvTime = new TimePickerView(this, TimePickerView.Type.HOURS_MINS);
        pvTime.setTime(new Date());
        pvTime.setCyclic(false);
        pvTime.setCancelable(true);
    }

    public static String getTime(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        return format.format(date);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.repeat_rl:
                selectRemindCycle();
                break;
            case R.id.ring_rl:
                selectRingWay();
                break;
            case R.id.set_btn:
                setScramble();
                break;
            default:
                break;
        }
    }

    @Override
    public void onAnimationFinished() {
        Log.d(TAG, "Cube AnimationFinished!");
    }

    private void setClock() {
//        AlarmManagerUtil.setAnimCube(animCube);
        Log.e(TAG, "setClock");
        timing = true;
        if (cycle == -2){
            AlarmManagerUtil.setAlarm(this, -1, 0, 0, 0, 0, "闹钟响了", ring);
            date_tv.setText(R.string.after10);
        }
        if (time != null && time.length() > 0) {
            Log.e("TAG", "time = " + time);
            String[] times = time.split(":");
            if (cycle == 0) {//是每天的闹钟
                Log.e(TAG, "Everyday");
                AlarmManagerUtil.setAlarm(this, 1, Integer.parseInt(times[0]), Integer.parseInt
                        (times[1]), 0, 0, "闹钟响了", ring);
            } else if (cycle == -1) {//是只响一次的闹钟
                Log.e(TAG, "Once");
                AlarmManagerUtil.setAlarm(this, 0, Integer.parseInt(times[0]), Integer.parseInt
                        (times[1]), 0, 0, "闹钟响了", ring);
            } else {//多选，周几的闹钟
                Log.e(TAG, "Custom");
                String weeksStr = parseRepeat(cycle, 1);
                String[] weeks = weeksStr.split(",");
                for (int i = 0; i < weeks.length; i++) {
                    AlarmManagerUtil.setAlarm(this, 2, Integer.parseInt(times[0]), Integer
                            .parseInt(times[1]), i, Integer.parseInt(weeks[i]), "闹钟响了", ring);
                }
            }
            Toast.makeText(this, "闹钟设置成功", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onCubeModelUpdate(int[][] newCubeModel) {
        Log.d(TAG, "Cube model updated!");
        printCubeModel(newCubeModel);
    }

    void printCubeModel(int[][] cube) {
        Log.d(TAG, "Cube model:");
        StringBuilder stringBuilder = new StringBuilder("");
        for (int i = 0; i < cube.length; i++) {
            stringBuilder.append("\n");
            stringBuilder.append(i);
            stringBuilder.append(":\n");
            for (int j = 0; j < cube[i].length; j++) {
                stringBuilder.append(" ");
                stringBuilder.append(cube[i][j]);
                if ((j + 1) % 3 == 0) {
                    stringBuilder.append("\n");
                } else {
                    stringBuilder.append(", ");
                }
            }
        }
        Log.d(TAG, stringBuilder.toString());
    }


    public void selectRemindCycle() {
        Log.e(TAG, "select Remind Cycle");
        final SelectRemindCyclePopup fp = new SelectRemindCyclePopup(this);
        fp.showPopup(allLayout);
        fp.setOnSelectRemindCyclePopupListener(new SelectRemindCyclePopup
                .SelectRemindCyclePopupOnClickListener() {

            @Override
            public void obtainMessage(int flag, String ret) {
                Log.e(TAG, "obtainMessaage flag = " + flag);
                switch (flag) {
                    // 星期一
                    case 0:

                        break;
                    // 星期二
                    case 1:

                        break;
                    // 星期三
                    case 2:

                        break;
                    // 星期四
                    case 3:

                        break;
                    // 星期五
                    case 4:

                        break;
                    // 星期六
                    case 5:

                        break;
                    // 星期日
                    case 6:

                        break;
                    // 确定
                    case 7:
                        int repeat = Integer.valueOf(ret);
                        tv_repeat_value.setText(parseRepeat(repeat, 0));
                        cycle = repeat;
                        fp.dismiss();
                        break;
                    case 8:
                        tv_repeat_value.setText("每天");
                        cycle = 0;
                        fp.dismiss();
                        break;
                    case 9:
                        tv_repeat_value.setText("只响一次");
                        cycle = -1;
                        fp.dismiss();
                        break;
                    case 10:
                        tv_repeat_value.setText("10s之后");
                        cycle = -2;
                        date_tv.setText("10s之后");
                        fp.dismiss();
                        break;
                    default:
                        break;
                }
            }
        });
    }


    public void selectRingWay() {
        SelectRemindWayPopup fp = new SelectRemindWayPopup(this);
        fp.showPopup(allLayout);
        fp.setOnSelectRemindWayPopupListener(new SelectRemindWayPopup
                .SelectRemindWayPopupOnClickListener() {
            @Override
            public void obtainMessage(int flag) {
                switch (flag) {
                    // 振动
                    case 0:
                        tv_ring_value.setText("振动");
                        ring = 0;
                        break;
                    // 铃声
                    case 1:
                        tv_ring_value.setText("铃声");
                        ring = 1;
                        break;
                    default:
                        break;
                }
            }
        });
    }

    /**
     * @param repeat 解析二进制闹钟周期
     * @param flag   flag=0返回带有汉字的周一，周二cycle等，flag=1,返回weeks(1,2,3)
     * @return
     */
    public static String parseRepeat(int repeat, int flag) {
        String cycle = "";
        String weeks = "";
        if (repeat == 0) {
            repeat = 127;
        }
        if (repeat % 2 == 1) {
            cycle = "周一";
            weeks = "1";
        }
        if (repeat % 4 >= 2) {
            if ("".equals(cycle)) {
                cycle = "周二";
                weeks = "2";
            } else {
                cycle = cycle + "," + "周二";
                weeks = weeks + "," + "2";
            }
        }
        if (repeat % 8 >= 4) {
            if ("".equals(cycle)) {
                cycle = "周三";
                weeks = "3";
            } else {
                cycle = cycle + "," + "周三";
                weeks = weeks + "," + "3";
            }
        }
        if (repeat % 16 >= 8) {
            if ("".equals(cycle)) {
                cycle = "周四";
                weeks = "4";
            } else {
                cycle = cycle + "," + "周四";
                weeks = weeks + "," + "4";
            }
        }
        if (repeat % 32 >= 16) {
            if ("".equals(cycle)) {
                cycle = "周五";
                weeks = "5";
            } else {
                cycle = cycle + "," + "周五";
                weeks = weeks + "," + "5";
            }
        }
        if (repeat % 64 >= 32) {
            if ("".equals(cycle)) {
                cycle = "周六";
                weeks = "6";
            } else {
                cycle = cycle + "," + "周六";
                weeks = weeks + "," + "6";
            }
        }
        if (repeat / 64 == 1) {
            if ("".equals(cycle)) {
                cycle = "周日";
                weeks = "7";
            } else {
                cycle = cycle + "," + "周日";
                weeks = weeks + "," + "7";
            }
        }

        return flag == 0 ? cycle : weeks;
    }

    private void setScramble() {
        anim_available = true;
        /* @setIcon 设置对话框图标
         * @setTitle 设置对话框标题
         * @setMessage 设置对话框消息提示
         * setXXX方法返回Dialog对象，因此可以链式设置属性
         */
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(AlarmSettingActivity.this);
//        normalDialog.setIcon(R.drawable.ic_launcher_background);
        normalDialog.setTitle("请打乱魔方");
        normalDialog.setMessage("打乱结束后点击确定，放弃打乱点击取消");
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!animCube.checkFinished()) {
                            anim_available = false;
                            setClock();
                        }
                        else{
                            date_tv.setText("闹钟未设置");
                        }
                    }
                });
        normalDialog.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        animCube.resetToInitialState();
                        anim_available = false;
                    }
                });
        // 显示
        normalDialog.show();
}

    private void runTurn(){
        if (!anim_available) {
            move_seqs.clear();
            return;
        }
        while (!move_seqs.isEmpty()) {
            String move = move_seqs.poll();
            animCube.runMoveSequence(move);
        }
    }
    public static class AlarmHandler extends Handler {
        private WeakReference<AlarmSettingActivity> mActivity;

        public AlarmHandler(AlarmSettingActivity activity) {
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            AlarmSettingActivity asa = mActivity.get();
            if (msg.what == 4) {
                Log.e("TimeOut", "TimeOut");
                asa.timeOut();
            }
            if (msg.what == 5){
                asa.finishAlarm();
            }
        }
    }

    public void timeOut(){
        anim_available = true;
        if (ring == 0){
            //vibrate
            vibrator = (Vibrator) this.getSystemService(Service.VIBRATOR_SERVICE);
            vibrator.vibrate(new long[]{100, 10, 100, 600}, 0);
        }
        else{
            //ring
            mediaPlayer = MediaPlayer.create(this, R.raw.in_call_alarm);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }

        final AlertDialog.Builder timeOutDialog = new AlertDialog.Builder(AlarmSettingActivity.this);
//        normalDialog.setIcon(R.drawable.ic_launcher_background);
        timeOutDialog.setTitle("闹钟响了");
        timeOutDialog.setMessage("魔方复原后闹钟才会停止哦~~");
        timeOutDialog.setPositiveButton(" ",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishAlarm();
                    }
                });
        // 显示
        timeOutDialog.show();
    }


    private void finishAlarm(){
        Log.e("finished", String.valueOf(timing));
        if (!timing)
            return;
        if (ring == 0){
            vibrator.cancel();
        }
        else{
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        timing = false;
    }
}