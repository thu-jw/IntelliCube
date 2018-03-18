package com.jw.fastble.callback;


import com.jw.fastble.data.BleDevice;

import java.util.List;

public abstract class BleScanCallback {

    public abstract void onScanStarted(boolean success);

    public abstract void onScanning(BleDevice result);

    public abstract void onScanFinished(List<BleDevice> scanResultList);

    public void onLeScan(BleDevice bleDevice){}
}
