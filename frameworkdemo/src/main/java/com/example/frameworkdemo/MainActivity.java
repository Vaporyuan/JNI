package com.example.frameworkdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private TextView mTextView;
    private ConnectivityManager mCm;
    Handler handler = new Handler();

    @SuppressLint({"SetTextI18n", "WrongConstant"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = findViewById(R.id.tv_demo);
        EthernetManager ethernetManager = (EthernetManager) getSystemService("ethernet");
        if (ethernetManager == null) {
            slog("EthernetManager is null");
            return;
        }
        ethernetManager.addListener(new EthernetManager.Listener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onAvailabilityChanged(String s, boolean b) {
                mTextView.setText("iface = " + s + " , isAvailable = " + b);
            }
        });

        //ethernetManager.setNetworkCoexist(false);
        handler.postDelayed(() -> {
            handler.obtainMessage(1).sendToTarget();
        },500);

        WifiManager wifiManager = (WifiManager) getSystemService("wifi"/*Context.WIFI_SERVICE*/);
        wifiManager.isWifiEnabled();
    }

    static class MalioHandler extends Handler {

        public MalioHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
        }
    }

    /**
     * 获取网卡节点状态
     * 节点不存在 -1
     * 网线拔出   0
     * 网线插入   1
     */
    public int getEthIfaceState(String mIface) {
        try {
            File file = new File("/sys/class/net/" + mIface + "/flags");
            if (!file.exists()) return -1;
            BufferedReader reader = new BufferedReader(new FileReader(file));
            return Integer.parseInt(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void slog(String infog) {
        Log.d("frameworkdemo", infog);
    }

    /**
     * 写入节点
     */
    private boolean setNodeString(String path, String value) {
        try {
            BufferedWriter bufWriter;
            bufWriter = new BufferedWriter(new FileWriter(path));
            bufWriter.write(value);
            bufWriter.flush();
            bufWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}