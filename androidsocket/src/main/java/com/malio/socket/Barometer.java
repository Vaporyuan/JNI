package com.malio.socket;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;


public class Barometer implements SensorEventListener {
    private static final String TAG = MainActivity.TAG;

    private final MainActivity mContext;
    private final SensorManager mSensorManager;
    private Sensor mSensor;

    private static final ArrayList<PressData> mDataList = new ArrayList<PressData>();

    public Barometer(MainActivity context) {
        mContext = context;

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (mSensor != null) {
            assert mSensorManager != null;
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            mContext.DisplayMessage("未检测到气压计！！！");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        PressData data;
        int val;

        val = (int) (event.values[0] * 100);
        Log.d(TAG, "onSensorChanged: PV=" + event.values.length + " val is " + val);

        //通知主界面显示
        mContext.sendMessage(MainActivity.CUR_PRESS_VAL, val);

        synchronized (mDataList) {
            for (int i = mDataList.size() - 1; i >= 0; i--) {
                data = mDataList.get(i);
                synchronized (data) {
                    data.saveData(val);
                    if (data.isFull()) {
                        mDataList.remove(data);
                        data.notify();
                    }
                }
            }
        }
    }

    public void startSample(PressData data) {
        if (data == null) {
            return;
        }
        synchronized (mDataList) {
            mDataList.add(data);
        }

    }

    public void exit() {
        if (mSensorManager != null && mSensor != null) {
            mSensorManager.unregisterListener(this, mSensor);
        }
    }
}
