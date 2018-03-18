package com.jw.blesample.comm;


import com.jw.fastble.data.BleDevice;

public interface Observer {

    void disConnected(BleDevice bleDevice);
}
