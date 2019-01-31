package com.jw.activities;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jw.AnimCubeLib.AnimCube;

import com.jw.AnimCubeLib.CubeConstants;
import com.jw.blesample.comm.Observer;
import com.jw.blesample.comm.ObserverManager;
import com.jw.fastble.BleManager;
import com.jw.fastble.callback.BleNotifyCallback;
import com.jw.fastble.data.BleDevice;
import com.jw.fastble.exception.BleException;
import com.jw.min2phase.Search;
import com.jw.min2phase.Tools;
import com.jw.min2phase.CubieCube;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;

public class RubikActivity extends AppCompatActivity implements AnimCube.OnCubeModelUpdatedListener, AnimCube.OnCubeAnimationFinishedListener, Observer{

    public static final String KEY_DATA = "key_data";
    public static final String RUN_MODE = "run_mode";
    private static final UUID uuidService = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID uuidCharacter = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");



    public static final String ANIM_CUBE_SAVE_STATE_BUNDLE_ID = "animCube";
    private static final String TAG = "AnimCubeActivity";
    private boolean anim_available = true;
    private boolean isRestorable = false;
//    public int turnCode = 0x1;
    private AnimCube animCube;
    private Bundle state;
    private boolean solve_start = false;


    //bluetooth
    private BleDevice bleDevice;
    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic characteristic;
    private int charaProp;

    private Toolbar toolbar;
    private TextView message;
    private TextView solver;

    private final int available_codes[] = {1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 13, 14};
    private Random random = new Random(System.currentTimeMillis());
    private Queue<String> move_seqs;
    private int MODE;

    private TextView timer_view;
    private Timer timer = new Timer(this, 2);
    public boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initData();
        initView();
        initBLE();
        Log.d(TAG, "onCreate");
        animCube = (AnimCube) findViewById(R.id.animcube);
        animCube.setOnCubeModelUpdatedListener(this);
        animCube.setOnAnimationFinishedListener(this);
        animCube.setParent(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        if (MODE == 1){
            menuInflater.inflate(R.menu.game_menu, menu);
        }
        else if (MODE == 2){
            menuInflater.inflate(R.menu.menu, menu);
        }
        else if (MODE == 4){
            menuInflater.inflate(R.menu.demo_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (MODE == 1 && item.getItemId() == R.id.reset)
//        {
//            animCube.resetToInitialState();
//            return super.onOptionsItemSelected(item);
//        }
        switch (item.getItemId()) {
            case R.id.start:
                anim_available = true;
                break;
            case R.id.stop:
                anim_available = false;
                animCube.stopAnimation();
                break;
            case R.id.reset:
                message.setText("");
                animCube.resetToInitialState();
                break;
            case R.id.scramble:
                setMessage("Scramble: " + animCube.scramble());
                break;
            case R.id.save_state:
                state = animCube.saveState();
                isRestorable = true;
                break;
            case R.id.restore_state:
                if (!isRestorable){
                    Toast.makeText(RubikActivity.this,this.getResources().getString(R.string.no_valid_state), Toast.LENGTH_LONG).show();
                }
                else {
                    animCube.restoreState(state);
                }
                break;
            case R.id.solve:
                showSolveSteps();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState ");
        outState.putBundle(ANIM_CUBE_SAVE_STATE_BUNDLE_ID, animCube.saveState());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState");
        animCube.restoreState(savedInstanceState.getBundle(ANIM_CUBE_SAVE_STATE_BUNDLE_ID));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        animCube.cleanUpResources();
        Log.d(TAG, "onDestroy: finish");
        BleManager.getInstance().clearCharacterCallback(bleDevice);
        ObserverManager.getInstance().deleteObserver(this);
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

    @Override
    public void onAnimationFinished() {
        Log.d(TAG, "Cube AnimationFinished!");
    }

    @Override
    public void disConnected(BleDevice device) {
        if (device != null && bleDevice != null && device.getKey().equals(bleDevice.getKey())) {
            finish();
        }
    }

    private void initData(){
        MODE = getIntent().getIntExtra(RUN_MODE, 1);
        if (MODE == 1){
            AnimCube.isRotatable = true;
            anim_available = true;
            return;
        }
        move_seqs = new LinkedList<>();
        bleDevice = getIntent().getParcelableExtra(KEY_DATA);
        if (bleDevice == null)
            finish();
        if (MODE == 4){
            CubieCube.initMove();
        }
    }

    private void initView() {
        if (MODE == 2) {
            setContentView(R.layout.activity_rubik);
        }
        else if (MODE == 4){
            setContentView(R.layout.activity_demo);
            solver = (TextView) findViewById(R.id.solver);
            solver.setText("");
            solver.setMovementMethod(ScrollingMovementMethod.getInstance());
            timer_view = (TextView) findViewById(R.id.timer);
            timer_view.setText("");
        }
        message = (TextView) findViewById(R.id.message);
        message.setText("");
        message.setMovementMethod(ScrollingMovementMethod.getInstance());
        if (MODE == 2) {
            timer_view = (TextView) findViewById(R.id.timer);
            timer_view.setText("");
        }
        toolbar = findViewById(R.id.cubeToolBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
                finish();
            }
        });

    }

    private void initBLE(){
        if (MODE == 1){
            return;
        }
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
                                setMessage("");
                            }
                        });
                    }

                    @Override
                    public void onNotifyFailure(final BleException exception) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setMessage(exception.toString());
                                onDestroy();
                            }
                        });
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

//                                int code = characteristic.getValue()[0] & random.nextInt();
                                //receive BLE data from the cube
//                                int code = random.nextInt(16);
//                                code = code % 16;
//                                code = (code < 0) ? code + 16 : code;
//                                code = (code < 12) ? available_codes[code] : 0;
//                                String seq;
//                                 seq = ;
                                int code = characteristic.getValue()[0];
                                String seq = animCube.decode(characteristic.getValue()[0]);
                                seq += " ";
                                Log.e(TAG, String.valueOf(code));
//                                if (!seq.equals("\n") || (message.getText().charAt(message.getText().length() - 1) == '\n')){
//                                    setMessage(seq);
//                                }
                                setMessage(seq);
                                if (!seq.equals(" ") && !isPlaying){
                                    startTiming();
                                }
                                move_seqs.offer(seq);
                                runTurn();

//                                setMessage(HexUtil.formatHexString(characteristic.getValue()));
                            }
                        });
                    }
                });
    }


    private void setMessage(String m){
//        if (!anim_available)
//            return;
        message.append(m);
        int offset=message.getLineCount()*message.getLineHeight();
        if(offset>message.getHeight()){
            message.scrollTo(0,offset-message.getHeight());
        }
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

    public void setTimer(String t){
        if (!isPlaying)
            return;
        timer_view.setText(t);
    }

    public void startTiming(){
        if (MODE == 4 && !solve_start)
            return;
        isPlaying = true;
        Log.e("tag", "start");
        timer.count();
        timer.timeStart = System.currentTimeMillis();
    }

    public void endTiming(){
        if (MODE == 4) {
            if (!solve_start)
                return;
            solve_start = false;
        }
        ShowFinishedSolveDialog();
        Log.e("tag", "endTiming");
        isPlaying = false;
    }

    public void ShowFinishedSolveDialog() {
        ButtonDialogFragment buttonDialogFragment = new ButtonDialogFragment();
        buttonDialogFragment.show("复原成功！", "用时: " + timer_view.getText(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                timer_view.setText("");
                solver.setText("");
                message.setText("");
            }
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                timer_view.setText("");
                solver.setText("");
                message.setText("");
            }
        }, getFragmentManager());
    }

    private void showSolveSteps(){
        String scrambledCube = Tools.fromScramble(message.getText().toString());
        String result = new Search().solution(scrambledCube, 21, 100000000, 0, Search.APPEND_LENGTH);
        solver.setText(result);
        solve_start = true;
    }


}
