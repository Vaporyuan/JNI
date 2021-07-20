package com.malio.socket;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class Wifi {
    private static final String TAG = MainActivity.TAG;
    private MainActivity mContext;
    private WifiManager mWifiManager;

    private static String mSSID;
    private static int mWifiId;
    private static String mPassword;

    public Wifi(MainActivity context, String ssid, String pswd) {
        mContext = context;
        mSSID = "\"" + ssid + "\"";
        mPassword = "\"" + pswd + "\"";

        mWifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mContext.registerReceiver(mReceiver, filter);
    }

    //判断wifi是否打开
    private void checkWifiEnabled() {
        boolean isWifiEnabled = mWifiManager.isWifiEnabled();
        if (isWifiEnabled) {
            mContext.DisplayMessage("wifi功能已开启");
            mHandler.sendEmptyMessage(CONNECT_HOT_POINT);
            mWifiManager.startScan();
        } else {
            mContext.DisplayMessage("正在开启wifi...");
            mWifiManager.setWifiEnabled(true);
        }
    }

    private boolean checkHotOpen(String ssid) {
        boolean open = false;

        List<ScanResult> hotlist = null;
        hotlist = mWifiManager.getScanResults();
        if (hotlist != null) {
            mContext.DisplayMessage("hotlist.size() = " + hotlist.size());
            for (int i = 0; i < hotlist.size(); i++) {
                ScanResult hot = hotlist.get(i);
                Log.d(TAG, "Hot " + i +": ssid = " + hot.SSID);
                if (ssid.equals("\"" + hot.SSID + "\"")) {
                    open = true;
                    break;
                }
            }
        } else {
            Log.d(TAG, "hotlist == null");
        }

        return open;
    }

    //添加热点
    private int addHotPoint(String ssid, String pwd) {
        Log.i(TAG, "addHotPoint: SSID = " + ssid + ", pwd = " + pwd);

        WifiConfiguration wifiCong = new WifiConfiguration();
        wifiCong.SSID = ssid;
        wifiCong.preSharedKey = pwd;
        wifiCong.hiddenSSID = false;
        wifiCong.status = WifiConfiguration.Status.ENABLED;
        return mWifiManager.addNetwork(wifiCong);
    }

    //连接指定热点
    private void connectHotPoint() {
        // 检查指定热点是否开启
        if (!checkHotOpen(mSSID)) {
            mContext.DisplayMessage("热点" + mSSID + "未扫描到，请确认是否已开启！！！");
            mWifiManager.startScan();
            return;
        }

        //如果已经连上，直接通知结果
        if (NotifyWifiConnected()) {
            return;
        }

        int wifiId = -1;
        /*需要明确的动态权限申请，每次调用都需要检查，不然lint检查不过，故将其封装*/
        if (!(ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(mContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE}, 9999);
        }

        List<WifiConfiguration> wifiConfigList = mWifiManager.getConfiguredNetworks();
        if (wifiConfigList != null && wifiConfigList.size() > 0) {
            for (int i = 0; i < wifiConfigList.size(); i++) {
                WifiConfiguration wifi = wifiConfigList.get(i);
                Log.d(TAG, "CONFIG: No." + i + " SSID = " + wifi.SSID
                        + ", pwd = " + wifi.preSharedKey);
                if (mSSID.equals(wifi.SSID)) {
                    wifiId = wifi.networkId;
                    Log.d(TAG, "mSSID.equals(wifi.SSID): wifiId =  " + wifiId);
                    break;
                }
            }
        }

        if (wifiId == -1) {
            wifiId = addHotPoint(mSSID, mPassword);
        }

        if (wifiId != -1) {
            mContext.DisplayMessage("正在连接热点...：" + mSSID);
            mWifiManager.enableNetwork(wifiId, true);
			mWifiId = wifiId;
        } else {
            Log.e(TAG, "connectHotPoint: wifiId == -1");
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    private static final int CHECK_WIFI_ENABLED = 100;
    private static final int CONNECT_HOT_POINT = 200;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CHECK_WIFI_ENABLED:
                    checkWifiEnabled();
                    break;
                case CONNECT_HOT_POINT:
                    connectHotPoint();
                    break;
                default:
                    break;
            }
        }
    };

    //将获取的int转为真正的ip地址,参考的网上的，修改了下
    private String intToIp(int ip) {
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
    }

    //通知主界面
    private boolean NotifyWifiConnected() {
        boolean notify = false;

        WifiInfo info = mWifiManager.getConnectionInfo();
        if (info != null && mSSID.equals(info.getSSID())) {
            DhcpInfo dhcp = mWifiManager.getDhcpInfo();
            mContext.DisplayMessage("dhcp.serverAddress：" + dhcp.serverAddress);
            int ip = dhcp.serverAddress;
            if (ip != 0) {
                mContext.NotiyWifiConnect(mSSID, intToIp(dhcp.serverAddress));
                notify = true;
            }
        }

        return notify;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "MESSAGE: " + action);
            // WIFI_STATE_CHANGED_ACTION用于监听Android Wifi打开或关闭的状态
            if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                // 这个监听wifi的连接状态，即是否连上了一个有效无线路由，当广播的状态是
                // WifiManager.WIFI_STATE_DISABLING和WIFI_STATE_DISABLED的时候，根本
                // 不会接到这个广播。在上边广播接到广播是WifiManager.WIFI_STATE_ENABLED
                // 状态的同时也会接到这个广播，当然刚打开wifi肯定还没有连接到有效的无线
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_DISABLED:
                        displayMessage("wifiState-WIFI_STATE_DISABLED");
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        displayMessage("wifiState-WIFI_STATE_DISABLING");
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        displayMessage("wifiState-WIFI_STATE_ENABLED");
                        Connect();
                        break;
                    case WifiManager.WIFI_STATE_ENABLING:
                        displayMessage("wifiState-WIFI_STATE_ENABLING");
                        break;
                    default:
                        displayMessage("wifiState-WIFI_STATE_UNKNOWN");
                        break;
                }
            // NETWORK_STATE_CHANGED_ACTION用于判断是否连接到了有效wifi
            } else if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                // 获取联网状态的NetWorkInfo对象
                NetworkInfo networkInfo = (NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (null != networkInfo) {
                    //获取的State对象则代表着连接成功与否等状态
                    NetworkInfo.State state = networkInfo.getState();
                    Log.d(TAG, "wifi.state = " + state);
                    //判断网络是否已经连接
                    if (state == NetworkInfo.State.CONNECTED) {
                        NotifyWifiConnected();
                    }
                }
            } else if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                mHandler.sendEmptyMessage(CONNECT_HOT_POINT);
            }
        }
    };

    private void displayMessage(String msg) {
        Log.d(TAG, msg);
        mContext.DisplayMessage(msg);
    }

    public void Connect() {
        mHandler.removeMessages(CHECK_WIFI_ENABLED);
        mHandler.sendEmptyMessage(CHECK_WIFI_ENABLED);
    }

    public void Disconnect() {
        mContext.unregisterReceiver(mReceiver);
        mHandler.removeMessages(CHECK_WIFI_ENABLED);
        mHandler.removeMessages(CONNECT_HOT_POINT);
		if(mWifiManager != null) {
			WifiInfo info = mWifiManager.getConnectionInfo();
			if(info != null)
				mWifiId = info.getNetworkId();
			mWifiManager.disconnect();
            Log.d(TAG, "Wifi Disconnect() and closem WifiId is " + mWifiId);
			if(mWifiId != -1)
//				mWifiManager.forget(mWifiId, null);
			mWifiManager.setWifiEnabled(false);
            Log.d(TAG, "Wifi Disconnect() and close");
		}
    }
}
