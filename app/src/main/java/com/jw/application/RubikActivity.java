package com.jw.application;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.jw.AnimCubeLib.AnimCube;
import com.jw.blesample.comm.Observer;
import com.jw.blesample.comm.ObserverManager;
import com.jw.fastble.BleManager;
import com.jw.fastble.callback.BleNotifyCallback;
import com.jw.fastble.data.BleDevice;
import com.jw.fastble.exception.BleException;
import com.jw.fastble.utils.HexUtil;

import org.w3c.dom.Text;

import java.util.UUID;

public class RubikActivity extends AppCompatActivity implements AnimCube.OnCubeModelUpdatedListener, AnimCube.OnCubeAnimationFinishedListener, Observer{

    public static final String KEY_DATA = "key_data";
    private static final UUID uuidService = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID uuidCharacter = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");



    public static final String ANIM_CUBE_SAVE_STATE_BUNDLE_ID = "animCube";
    private static final String TAG = "AnimCubeActivity";
    private boolean receiving_data = false;
    public int turnCode = 0x1;
    private AnimCube animCube;
    private Bundle state;

    //bluetooth
    private BleDevice bleDevice;
    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic characteristic;
    private int charaProp;

    private Toolbar toolbar;
    private TextView message;

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.play_forward:
                animCube.animateMoveSequence();
                break;
            case R.id.play_backward:
                animCube.animateMoveSequenceReversed();
                break;
            case R.id.stop:
                animCube.stopAnimation();
                break;
            case R.id.one_move_forward:
                animCube.animateMove();
                break;
            case R.id.one_move_backward:
                animCube.animateMoveReversed();
                break;
            case R.id.reset:
                animCube.resetToInitialState();
                break;
            case R.id.save_state:
                state = animCube.saveState();
                break;
            case R.id.restore_state:
                animCube.restoreState(state);
                break;
            case R.id.Test:
                while (true){
                    animCube.runCodedTurn(turnCode);
                    message.setText(String.valueOf(turnCode));
                    turnCode++;
                    while (animCube.isAnimating())
                    {

                    }
                }
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
        bleDevice = getIntent().getParcelableExtra(KEY_DATA);
        if (bleDevice == null)
            finish();
    }

    private void initView() {
        setContentView(R.layout.activity_rubik);
        message = (TextView) findViewById(R.id.message);
        toolbar = findViewById(R.id.cubeToolBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initBLE(){
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
                                setMessage("notify success");
                            }
                        });
                    }

                    @Override
                    public void onNotifyFailure(final BleException exception) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setMessage(exception.toString());
                            }
                        });
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int code = byteArrayToInt(characteristic.getValue());
                                runTurn(code);
                                setMessage(HexUtil.formatHexString(characteristic.getValue()));
                            }
                        });
                    }
                });
    }

    private void setMessage(String m){
        message.setText(m);
    }


    private void runTurn(int code){
        animCube.runCodedTurn(code);
        while(animCube.isAnimating());
    }
    private int byteArrayToInt(byte[] b) {
        return b[0] & 0xFF;
    }
}
