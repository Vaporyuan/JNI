package com.internalkye.im;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
//import android.net.EthernetManager;
import android.net.EthernetManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.MessengerService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;

import vendor.mediatek.hardware.nvram.V1_0.INvram;

public class MainActivity extends Activity {

    private static final String TAG = "yuanwei";
    //MalioDeviceManager malioDeviceManager;
    private static final String[] ABC_PACKAGES = {"com.antutu.abenchmark", "com.antutu.abenchmark"};
    private MessengerService.InstallBinder installBinder;
    private TextToSpeech textToSpeech;
    private NetWorkChangeReceiver netWorkChangeReceiver;
    private IntentFilter intentFilter;
    private TextView mShowTv;
    private EditText mWhiteEv;
    String mWhiteList;
    //private IPackageInstallObserver mIPackageManager;
    //IPackageDeleteObserver
    ConnectivityManager connectivityManager;
    EthernetManager mEthernetManager;
    WifiManager wifiManager;
    TelephonyManager telephonyService;
    boolean isIface;
    ActivityManager activityManager;
    Context mContext;

    //??????
    private void playRunTime(String cmd) throws Exception {
        Process p = Runtime.getRuntime().exec(cmd);
        InputStream is = p.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            mShowTv.append(line + "\n");
        }
        p.waitFor();
        is.close();
        reader.close();
        p.destroy();
    }

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mShowTv = findViewById(R.id.tv_show);
        mWhiteEv = findViewById(R.id.editText);
        //malioDeviceManager = MalioDeviceManager.getInstance();
        // Example of a call to a native method
        Button btn = findViewById(R.id.button);
        //btn.setBackground(getResources().getDrawable(R.drawable.btn_background_round));
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mEthernetManager = (EthernetManager) getSystemService("ethernet");
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        telephonyService = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        mContext = this;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*textToSpeech.speak("??????SQ45????????????",
                        TextToSpeech.QUEUE_ADD, null);*/
                /*mWhiteList = mWhiteEv.getText().toString();
                if (!mWhiteList.equals("")) {
                    mDeviceManager.whiteListsAppInsert(mWhiteList);
                    // mDeviceManager.whiteListAppRemove("com.urovo.devicecontrol");
                    Toast.makeText(MainActivity.this, "????????????????????????", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "???????????????????????????,??????????????????", Toast.LENGTH_SHORT).show();
                }*/
                //malioDeviceManager.enableStatusBar(false);
                //malioDeviceManager.shutdown(true);
                //malioDeviceManager.setLockTaskMode(getPackageName(), true);
                //mWhiteEv.setText(malioDeviceManager.getSettingProperty("persist-persist.tusn.num"));
                /*String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                boolean ret = malioDeviceManager.installApplication(absolutePath + "/weili_tianqi.apk", false, new IPackageInstallObserver.Stub() {
                    @Override
                    public void packageInstalled(String s, int i) throws RemoteException {
                        Log.d(TAG, "packageInstalled pkgname-->"+s+" return---->"+i);
                        //mWhiteEv.setText("packageInstalled pkgname-->"+s+" return---->"+i);
                    }
                });*/

                //sendKeyCode(KeyEvent.KEYCODE_BACK);
                /*Intent intent = new Intent("com.ob.action.SUSPENSION_BACK_BUTTON");
                intent.setPackage("com.ob.floatback");
                startService(intent);*/

                /*Intent mService = new Intent("com.ob.action.BOOT_FLOAT_BACK");
                mService.setPackage("com.ob.floatback");
                startService(mService);*/
                //customNetwork(NetworkCapabilities.TRANSPORT_WIFI);
                /*int value = "".contentEquals(mWhiteEv.getText()) ? 0 : Integer.parseInt(mWhiteEv.getText().toString());
                writeNV(value);*/

                /*PropertyUtils.set("persist.sys.malio.net_priority", mWhiteEv.getText().toString());
                if (mEthernetManager != null) mEthernetManager.updateScoreFilter();
                Settings.Global.putInt(mContext.getContentResolver(), "mobile_score", (int) System.currentTimeMillis());
                if(PropertyUtils.get("persist.sys.malio.net_priority").equals("4G")){
                    if (getMobileDataState()) {
                        setMobileDataState(false);
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        setMobileDataState(true);
                        Toast.makeText(MainActivity.this, "??????mobile??????!", Toast.LENGTH_SHORT).show();
                    }
                }*/

                //????????????????????????
                //PropertyUtils.set("persist.sys.malio.net_priority_ori", PropertyUtils.get("persist.sys.malio.net_priority"));
                //4G??????
                //PropertyUtils.set("persist.sys.malio.net_priority", "4G");
                //??????ip shell ????????????
                /*if (!PropertyUtils.get("persist.sys.boot_malio_sh").equals("1")) {
                    PropertyUtils.set("persist.sys.boot_malio_sh", "1");
                    Toast.makeText(MainActivity.this, "boot_malio_sh??????1", Toast.LENGTH_SHORT).show();
                }*/
                //if(mEthernetManager != null) mEthernetManager.setNetworkCoexist(true);
                //PropertyUtils.set("persist.sys.malio.boot_malio_sh", mWhiteEv.getText().toString());
                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            long before = System.currentTimeMillis();
                            //String cmd0 = "cat /proc/net/arp | busybox awk '{print $4}' | grep \"00:12:\"  | busybox sort | busybox uniq";
                            //playRunTime(cmd0);
                            mShowTv.append("do before???" + before + "\n");
                            //playRunTime(mWhiteEv.getText().toString());
                            //playRunTime("busybox arp -a | grep \\\"\" + mac + \"\\\" | busybox awk '{print $2}' | busybox sed 's/(//;s/)//'\\n");
                            //playRunTime("cat /proc/net/arp | grep \\\"^00:12:31:aa:40:cd\\\" + | busybox awk '{print $2}' | busybox sed 's/(//;s/)//'\\n");

                            String cmd1 = "cat /proc/net/arp  | grep \"00:12:31:aa:40:cd\" | busybox awk '{print $1}' | busybox sed 's/(//;s/)//'";
                            playRunTime(cmd1);
                            long after = System.currentTimeMillis();
                            mShowTv.append("use time???" + (after - before) / 1000 + "\n");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });*/
                //Toast.makeText(MainActivity.this, "setNetworkCoexist true", Toast.LENGTH_SHORT).show();

            }
        });
        btn.setBackgroundColor(Color.GRAY);
        /*textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == textToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.CHINA);
                    if (result != TextToSpeech.LANG_COUNTRY_AVAILABLE
                            && result != TextToSpeech.LANG_AVAILABLE) {
                        Toast.makeText(MainActivity.this, "TTS???????????????????????????????????????",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });*/


        Button btn2 = findViewById(R.id.button2);
        btn2.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {

                //malioDeviceManager.enableStatusBar(true);
                //malioDeviceManager.setLockTaskMode(getPackageName(), false);
                //malioDeviceManager.uninstallApplication("cn.weli.weather");
                /*malioDeviceManager.uninstallApplication("cn.weli.weather", false, new IPackageDeleteObserver.Stub() {
                    @Override
                    public void packageDeleted(String s, int i) throws RemoteException {
                        Log.d(TAG, "packageDeleted pkgname-->"+s+" return---->"+i);
                        //mWhiteEv.setText("packageDeleted pkgname-->"+s+" return---->"+i);
                    }
                });*/
                //??????????????????
                /*Intent intent = new Intent("com.ob.action.SUSPENSION_BACK_BUTTON");
                intent.setPackage("com.ob.floatback");
                stopService(intent);*/

                //???????????? https://blog.csdn.net/weixin_40920751/article/details/114374799
                /*NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                mShowTv.setText("persist.sys.malio.net_priority = " + PropertyUtils.get("persist.sys.malio.net_priority"));
                if (networkCapabilities == null) {
                    mWhiteEv.setText("???????????????");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    mWhiteEv.setText("????????????????????????");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    mWhiteEv.setText("????????????WIFI??????");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    mWhiteEv.setText("???????????????????????????");
                }*/

                //?????????????????????
                //PropertyUtils.set("persist.sys.malio.net_priority", PropertyUtils.get("persist.sys.malio.net_priority_ori"));
                //????????????
                /*if (!PropertyUtils.get("persist.sys.boot_malio_sh").equals("0")) {
                    PropertyUtils.set("persist.sys.boot_malio_sh", "0");
                    Toast.makeText(MainActivity.this, "boot_malio_sh??????0", Toast.LENGTH_SHORT).show();
                }*/
                if(mEthernetManager != null) mEthernetManager.setNetworkCoexist(false);
                Toast.makeText(MainActivity.this, "setNetworkCoexist false", Toast.LENGTH_SHORT).show();

            }
        });

        Button btn3 = findViewById(R.id.button3);
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //??????wifi????????????
                int status = wifiManager.getWifiState();
                if (status == WifiManager.WIFI_STATE_ENABLED) {
                    //wifi?????????????????????
                    wifiManager.setWifiEnabled(false);
                    Toast.makeText(MainActivity.this, "wifi?????????", Toast.LENGTH_SHORT).show();
                } else {
                    //?????????????????????
                    wifiManager.setWifiEnabled(true);
                    Toast.makeText(MainActivity.this, "wifi?????????", Toast.LENGTH_SHORT).show();

                }
            }
        });

        Button btn4 = findViewById(R.id.button4);
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (getMobileDataState()) {
                    //4G?????????????????????
                    setMobileDataState(false);
                    Toast.makeText(MainActivity.this, "Mobile?????????", Toast.LENGTH_SHORT).show();
                } else {
                    //?????????????????????
                    setMobileDataState(true);
                    Toast.makeText(MainActivity.this, "Mobile?????????", Toast.LENGTH_SHORT).show();

                }
            }
        });

        Button btn5 = findViewById(R.id.button5);
        btn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String iFace = "eth0";
                if (!isIface) {
                    if(mEthernetManager != null) mEthernetManager.stop();
                    //if(mEthernetManager != null) mEthernetManager.updateIface(iFace, false);
                    isIface = true;
                    Toast.makeText(MainActivity.this, "Eth?????????", Toast.LENGTH_SHORT).show();
                }else {
                    if(mEthernetManager != null) mEthernetManager.start();
                    //if(mEthernetManager != null) mEthernetManager.updateIface(iFace, true);
                    isIface = false;
                    Toast.makeText(MainActivity.this, "Eth?????????", Toast.LENGTH_SHORT).show();
                }
            }
        });
        intentFilter = new IntentFilter();
        intentFilter.addAction("urovo.rcv.message");
        intentFilter.addAction("android.intent.ACTION_DECODE_DATA");
        netWorkChangeReceiver = new NetWorkChangeReceiver();
        //????????????
        //registerReceiver(netWorkChangeReceiver, intentFilter);

        Button btn6 = findViewById(R.id.button6);
        btn6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Intent mService = new Intent("com.ob.action.SUSPENSION_BACK_BUTTON");
                mService.setPackage("com.ob.floatback");
                MainActivity.this.startService(mService);*/
                //?????????????????????
                String netWorkPriority = mEthernetManager == null ? "" : mEthernetManager.getNetworkPriority();
                String networkCoexist = PropertyUtils.get("persist.sys.malio.eth_4g_malio");
                String netWorkPriorityBf = PropertyUtils.get("persist.sys.malio.net_priority_ori");
                mShowTv.setText("persist.sys.malio.net_priority = " + netWorkPriority
                        + "\npersist.sys.malio.eth_4g_malio = " + networkCoexist
                        + "\npersist.sys.malio.net_priority_ori = " + netWorkPriorityBf);
                //???????????? https://blog.csdn.net/weixin_40920751/article/details/114374799
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (networkCapabilities == null) {
                    mWhiteEv.setText("???????????????");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    mWhiteEv.setText("????????????????????????");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    mWhiteEv.setText("????????????WIFI??????");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    mWhiteEv.setText("???????????????????????????");
                }

                mWhiteEv.setText("eth0 = " + execCommand("cat /sys/class/net/eth0/carrier")
                        + " , flags = " + execCommand("cat /sys/class/net/eth0/flags") );
            }
        });

        Button btn7 = findViewById(R.id.button7);
        btn7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEthernetManager != null)
                    mEthernetManager.setNetworkPriority(mWhiteEv.getText().toString());
            }
        });

    }





    public String execCommand(String command) {
        Runtime runtime;
        Process proc = null;
        StringBuffer stringBuffer = null;
        try {
            runtime = Runtime.getRuntime();
            proc = runtime.exec(command);
            stringBuffer = new StringBuffer();
            if (proc.waitFor() != 0) {
                System.err.println("exit value = " + proc.exitValue());
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    proc.getInputStream()));

            String line = null;
            while ((line = in.readLine()) != null) {
                stringBuffer.append(line + " ");
            }

        } catch (Exception e) {
            System.err.println(e);
        } finally {
            try {
                proc.destroy();
            } catch (Exception e2) {
            }
        }
        return stringBuffer.toString();
    }

    public boolean isEthernetConnected() {
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        mWhiteEv.setText("isEthernetConnected = " + activeNetworkInfo);
        return activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_ETHERNET && activeNetworkInfo.isConnected();
    }

    private void allNetwork() {
        NetworkInfo[] allNetwork = connectivityManager.getAllNetworkInfo();
        if (allNetwork != null) {
            for (int i = 0; i < allNetwork.length; i++) {
                Log.d(TAG, "allNetwork = " + allNetwork[i]);
            }
        }
    }

    public void setMobileDataState(boolean enabled) {
        //TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method setDataEnabled = telephonyService.getClass().getDeclaredMethod("setDataEnabled", boolean.class);
            if (null != setDataEnabled) {
                setDataEnabled.invoke(telephonyService, enabled);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getMobileDataState() {
        //TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method getDataEnabled = telephonyService.getClass().getDeclaredMethod("getDataEnabled");
            if (null != getDataEnabled) {
                return (Boolean) getDataEnabled.invoke(telephonyService);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void writeNV(int value) {
        String filePathName = "/vendor/nvdata/APCFG/APRDEB/TPW_H_FLAG";
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        dataArray.add((byte) value);
        try {
            INvram agent = INvram.getService();
            if (agent != null) {
                byte a = agent.writeFileByNamevec(filePathName, dataArray.size(), dataArray);
                Log.i(TAG, "malio write---->: " + a);
                String buff = agent.readFileByName(filePathName, 1);
                Log.i(TAG, "malio read---->: " + buff);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * NetworkCapabilities.TRANSPORT_CELLULAR    ????????????
     * NetworkCapabilities.TRANSPORT_WIFI        WIFI
     * NetworkCapabilities.TRANSPORT_BLUETOOTH   ??????
     * NetworkCapabilities.TRANSPORT_ETHERNET    ?????????
     * NetworkCapabilities.TRANSPORT_VPN         VPN
     */
    private void customNetwork(int transportType) {
        if (Build.VERSION.SDK_INT >= 21) {
            Log.d(TAG, "enter customNetwork!!");
            final ConnectivityManager connectivityManager =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            builder.addTransportType(transportType);
            NetworkRequest request = builder.build();
            ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {

                /**
                 * ?????????????????????????????????
                 * */
                @Override
                public void onAvailable(Network network) {
                    //Network 101 WIFI
                    //Network 103 CELLULAR
                    Log.d(TAG, "onAvailable: network = " + network);
                    //??????????????????
                    if (Build.VERSION.SDK_INT >= 23) {
                        connectivityManager.bindProcessToNetwork(network);
                    } else {
                        ConnectivityManager.setProcessDefaultNetwork(network);
                    }
                    //??????????????????wifi???????????????????????????????????????ping??????
                    //pingHost("14.215.177.38"); //???????????? ping www.baidu.com
                    mWhiteEv.setText("pingHost ==>" + pingHost("14.215.177.38"));
                }
            };
            connectivityManager.requestNetwork(request, callback);
        }
    }

    /**
     * <uses-permission android:name="android.permission.INTERNET" />
     * @param ip
     * @return ping result
     */
    public static boolean pingHost(String ip) {  //ip??????ping???IP??????
        boolean result = false;
        try {
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 100 " + ip);
            int status = p.waitFor();
            result = status == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "pingHost ==>" + ip + " result==>" + result);
        return result;
    }

    /**
     * ???Runtime??????????????????
     *
     * @param keyCode ????????????(KeyEvent)????????????
     */
    private void sendKeyCode(int keyCode) {
        try {
            String keyCommand = "input keyevent " + keyCode;
            // ??????Runtime??????????????????
            Runtime.getRuntime().exec(keyCommand);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private class NetWorkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d("yuanwei", "0000 NetWorkChangeReceiver= " +  intent.getAction());
            //????????????????????????
            if (intent.getAction().equals("urovo.rcv.message")) {

                String barcodeString = new String(intent.getByteArrayExtra("barocode"), 0, intent.getIntExtra("length", 0));
                Log.d("yuanwei", "1111urovo.rcv.message barocode= " + barcodeString);
            }
            //????????????????????????
            /*if (intent.getAction().equals("android.intent.ACTION_DECODE_DATA")) {

                Log.d("yuanwei", "2222urovo.rcv.message barocode= " + intent.getByteArrayExtra("barcode_string") +
                        " length= " + intent.getIntExtra("length", 0));
            }*/
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //????????????
        //unregisterReceiver(netWorkChangeReceiver);
    }

    private void setNode(String patch, String value) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(patch);
            outputStream.write(value.getBytes());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

}
