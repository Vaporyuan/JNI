package com.malio.socket;

import java.util.ArrayList;

public class PressData {
    private int data_num;
    private final int[] data;

    private static final ArrayList<PressData> mIdleList = new ArrayList<PressData>();

    private PressData() {
        data_num = 0;
        data = new int[40];
    }

    public static PressData getInstance() {
        PressData data;

        synchronized (mIdleList) {
            if (mIdleList.isEmpty()) {
                data = new PressData();
            } else {
                data = mIdleList.remove(0);
            }
        }
        return data;
    }

    public static void freeData(PressData data) {
        synchronized (mIdleList) {
            if (data != null) {
                data.data_num = 0;
                mIdleList.add(data);
            }
        }
    }

    public void saveData(int v) {
        if (data_num < data.length) {
            data[data_num] = v;
            data_num++;
        }
    }

    public int average() {
        int sum = 0;

        for (int i = 0; i < data_num; i++) {
            sum += data[i];
        }
        return (sum / data_num);
    }

    public boolean isFull() {
        return (data_num >= data.length);
    }
}