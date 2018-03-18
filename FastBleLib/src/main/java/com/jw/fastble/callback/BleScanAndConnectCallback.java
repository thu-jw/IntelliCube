package com.jw.fastble.callback;


import com.jw.fastble.data.BleDevice;

public abstract class BleScanAndConnectCallback extends BleGattCallback {

    public abstract void onScanStarted(boolean success);

    public abstract void onScanFinished(BleDevice scanResult);

    public void onScanning(BleDevice bleDevice){}

    public void onLeScan(BleDevice bleDevice){}

}
