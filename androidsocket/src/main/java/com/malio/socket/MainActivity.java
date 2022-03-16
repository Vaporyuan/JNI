package com.malio.socket;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends Activity {
    public static final String TAG = "AndSocket";
    private static final int WIFI_SSID_COUNT = 16;
    private static final String WIFI_SSID = "topwise";
    private static final String WIFI_PSWD = "12345678";

    private LinearLayout mLine2;
    private LinearLayout mLine3;
    private TextView mCurPress;
    private TextView mCurTemp;
    private TextView mBenchmarkPress;
    private TextView mSelfPress;
    private TextView mDeltaPress;
    private TextView mMsgView;

    private Wifi mWifi;
    private Barometer mBarometer;
    private Server mServer;
    private Client mClient;

    private int mSelectID = 0;
    private String mIP;
    private int mPort = 12345;
    private boolean mEableBack = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_item);

        Button btnHot;
        btnHot = (Button) findViewById(R.id.benchmark);
        btnHot.setOnClickListener(onClickListener);

        mBarometer = new Barometer(this);
        String name;
        int id;
        for (int i = 0; i < WIFI_SSID_COUNT; i++) {
            name = "hot" + (i + 1);
            id = getResources().getIdentifier(name, "id", getPackageName());
            btnHot = (Button) findViewById(id);
            btnHot.setAllCaps(false);
            btnHot.setTextSize(18);
            btnHot.setOnClickListener(onClickListener);
            btnHot.setText("热点:" + WIFI_SSID + (i + 1) + "\n密码:" + WIFI_PSWD);
        }
    }

    @Override
    public void onBackPressed() {
        if (mEableBack)
            super.onBackPressed();
        else
            DisplayMessage("校准中，请稍后...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mWifi != null) {
            mWifi.Disconnect();
        }
        if (mBarometer != null) {
            mBarometer.exit();
        }
        if (mServer != null) {
            try {
                mServer.exit();
                mServer.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mClient != null) {
            try {
                mClient.exit();
                mClient.join(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 切换为标准机
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onClick(View v) {
            MainActivity context = MainActivity.this;
            mSelectID = v.getId();
            Intent intent = new Intent();
            intent.setAction("com.mediatek.mtklogger.ADB_CMD");
            intent.setPackage("com.mediatek.mtklogger");
            intent.putExtra("cmd_name", "start");
            intent.putExtra("cmd_target", 23);
            MainActivity.this.sendBroadcast(intent);
            /*context.setContentView(R.layout.activity_main);
            mMsgView = (TextView) context.findViewById(R.id.message);
            mLine2 = (LinearLayout) context.findViewById(R.id.line2);
            mLine3 = (LinearLayout) context.findViewById(R.id.line3);
            mCurPress = (TextView) context.findViewById(R.id.cur_press);
            mCurTemp = (TextView) context.findViewById(R.id.cur_temp);
            mBenchmarkPress = (TextView) context.findViewById(R.id.benchmark_press);
            mSelfPress = (TextView) context.findViewById(R.id.product_press);
            mDeltaPress = (TextView) context.findViewById(R.id.delta_press);

            if (mSelectID == R.id.benchmark) {
                mLine2.setVisibility(View.GONE);
                mLine3.setVisibility(View.GONE);
                mServer = new Server(context, mPort);
                mServer.start();
            } else {
                String ssid, pswd;
                String name;
                int id, i;

                for (i = 0; i < WIFI_SSID_COUNT; i++) {
                    name = "hot" + (i + 1);
                    id = context.getResources().getIdentifier(name, "id", context.getPackageName());
                    if (id == mSelectID) {
                        break;
                    }
                }

                ssid = WIFI_SSID + (i + 1);
                pswd = WIFI_PSWD;
                mWifi = new Wifi(context, ssid, pswd);
                mWifi.Connect();
            }*/
        }
    };

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        MainActivity.this.finish();
    }

    //显示信息
    List<String> mLogs = new ArrayList<>();

    private void setMessage(String msg) {
        if (mLogs.size() > 20) {
            mLogs.remove(0);
        }
        mLogs.add(msg);

        StringBuilder sb = new StringBuilder();
        for (String s : mLogs) {
            sb.append("\n" + s);
        }
        mMsgView.setText(sb.toString());

        Log.d(TAG, msg);
    }

    private void setConnect() {
        if (mClient == null) {
            mClient = new Client(this, mIP, mPort);
            mClient.start();
        }
    }

    private void startBarometer(Object obj) {
        if (mBarometer == null) {
            mBarometer = new Barometer(this);
        }

        mBarometer.startSample((PressData) obj);
    }

    private void failtureAlert(int str_id) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);

        dialog.setTitle(R.string.failture_title)
                .setIcon(R.drawable.error)
                .setMessage(str_id)
                .setCancelable(false)
                .setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //...To-do
                                try {
                                    mClient.join();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mClient = null;
                                mWifi.Connect();
                            }
                        })
                .setNegativeButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //...To-do
                                MainActivity.this.finish();
                            }
                        });

        // 显示
        if (!MainActivity.this.isFinishing())
            dialog.show();
    }

    private void successAlert(String Message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);

        dialog.setTitle(R.string.success_title)
                .setIcon(R.drawable.ok)
                .setMessage(Message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //...To-do
                                MainActivity.this.finish();
                            }
                        });

        // 显示
        if (!MainActivity.this.isFinishing())
            dialog.show();
    }

    private void pressCalibrate(Object obj) {
        int[] data = (int[]) obj;
        int standard = data[0];
        int host = data[1];

        setMessage("收到压力值：standard=" + standard + ", host=" + host);
        mBenchmarkPress.setText(String.valueOf(standard));
        mSelfPress.setText(String.valueOf(host));
        mDeltaPress.setText(String.valueOf(host - standard));

        if (standard > 0 && host > 0) {
            int delta = host - standard;
            if (delta > 30 || delta < -30) {
                failtureAlert(R.string.fail_delta_larger);
            } else {
                String msg = MainActivity.this.getString(R.string.suc_calibration);
                //successAlert(msg + delta);
            }
        }
    }

    private int mFailTimes = 0;

    private void sockFail() {
        mClient = null;
        mFailTimes++;
        if (mFailTimes < 5) {
            setConnect();
        }
    }

    public static final int SET_MESSAGE = 100;
    public static final int REMOTE_NV_SERVICE_BIND = 105;
    public static final int SET_CONNECT = 110;
    public static final int SOCK_CONNECT_FAIL = 120;
    public static final int GET_PRESSURE_VAL = 130;
    public static final int BEGIN_PRESS_CAL = 140;
    public static final int CUR_PRESS_VAL = 150;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SET_MESSAGE:
                    setMessage((String) msg.obj);
                    break;
                case SET_CONNECT:
                    setConnect();
                    break;
                case SOCK_CONNECT_FAIL:
                    sockFail();
                    break;
                case GET_PRESSURE_VAL:
                    if (mSelectID != R.id.benchmark)
                        mEableBack = false;
                    startBarometer(msg.obj);
                    break;
                case BEGIN_PRESS_CAL:
                    pressCalibrate(msg.obj);
                    break;
                //校准前先将之前的校准值清0，不然校准值有问题
                case REMOTE_NV_SERVICE_BIND:
                    break;
                case CUR_PRESS_VAL:
                    Integer val = (Integer) msg.obj;
                    if (mCurPress != null) {
                        mCurPress.setText(val.toString());
                        mCurTemp.setText(getTemperatureValueFromPressureSensor() + "");
                    }
                    break;
                default:
                    break;
            }
        }
    };

    //显示信息
    public void DisplayMessage(String msg) {
        Message message = mHandler.obtainMessage(SET_MESSAGE);
        if (message != null) {
            message.obj = msg;
            mHandler.sendMessage(message);
        }
    }

    //热点连上
    public void NotiyWifiConnect(String ssid, String ip) {
        DisplayMessage("Wifi Connected! - ssid = " + ssid + ", ip = " + ip);

        mIP = ip;
        Message message = mHandler.obtainMessage(SET_CONNECT);
        if (message != null) {
//            mHandler.sendMessage(message);
            mHandler.sendMessageDelayed(message, 500);
        }
    }

    public void sendMessage(int msg_id, Object obj) {
        Message message = mHandler.obtainMessage(msg_id);
        if (message != null) {
            message.obj = obj;
            mHandler.sendMessage(message);
        }
    }

    private static final String PRESSURE_TAG = "BRAO:";
    private static final String TEMPERATURE_TAG = "TEMP:";
    private static final int VALUE_LENGTH = 8;

    public float getTemperatureValueFromPressureSensor() {
        float result = 0f;
        try {
            String sensorValue = readFile("/sys/bus/platform/drivers/barotemp/sensordata");
            Log.d(TAG, "**getTemperatureValueFromPressureSensor**: sensorValue=[" + sensorValue + "]");
            if (sensorValue != null) {
                int tempTagIndex = sensorValue.indexOf(TEMPERATURE_TAG);
                Log.d(TAG, "**getTemperatureValueFromPressureSensor**: tempTagIndex=[" + tempTagIndex + "]");
                if (tempTagIndex >= 0) {
                    String removeTagString = sensorValue.substring(tempTagIndex + TEMPERATURE_TAG.length());
                    Log.d(TAG, "**getTemperatureValueFromPressureSensor**: removeTagString=[" + removeTagString + "]");
                    if (removeTagString != null) {
                        int endIndex = removeTagString.indexOf("\n");
                        Log.d(TAG, "**getTemperatureValueFromPressureSensor**: endIndex=[" + endIndex + "]");
                        if (endIndex > 0) {
                            String resultString = removeTagString.substring(0, endIndex);
                            Log.d(TAG, "**getTemperatureValueFromPressureSensor**: resultString=[" + resultString + "]");
                            if (resultString != null) {
                                int intResult = Integer.valueOf(resultString, 16);
                                result = (float) intResult / 100;
                                Log.d(TAG, "**getTemperatureValueFromPressureSensor**: result=[" + result + "]");
                            }
                        }
                    }
                }
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "**getTemperatureValueFromPressureSensor**: e=" + e);
            return result;
        }
    }

    public String readFile(String strFile) {
        Log.i(TAG, "**readFile**: filename=[" + strFile + "]");
        BufferedReader in = null;
        try {
            char[] buf = new char[1024];
            in = new BufferedReader(new FileReader(strFile));
            int len = in.read(buf);
            Log.i(TAG, "**readFile**: " + strFile + ",len=" + len + ", [" + buf + "]");
            return String.valueOf(buf);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "**readFile**: FileNotFoundException start e=" + e);
        } catch (IOException ee) {
            Log.e(TAG, "**readFile**: IOException ee=" + ee);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return "";
    }

}