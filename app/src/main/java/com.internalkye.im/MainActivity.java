package com.internalkye.im;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageManager;
import android.device.MalioDeviceManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.MessengerService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import vendor.mediatek.hardware.nvram.V1_0.INvram;

public class MainActivity extends Activity {

    private static final String TAG = "yuanwei";
    MalioDeviceManager malioDeviceManager;
    private static final String[] ABC_PACKAGES = {"com.antutu.abenchmark", "com.antutu.abenchmark"};
    private MessengerService.InstallBinder installBinder;
    private TextToSpeech textToSpeech;
    private NetWorkChangeReceiver netWorkChangeReceiver;
    private IntentFilter intentFilter;
    private TextView mShowTv;
    private EditText mWhiteEv;
    String mWhiteList;
    private IPackageInstallObserver mIPackageManager;
    //IPackageDeleteObserver

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mShowTv = findViewById(R.id.tv_show);
        mWhiteEv = findViewById(R.id.editText);
        malioDeviceManager = MalioDeviceManager.getInstance();
        // Example of a call to a native method
        Button btn = findViewById(R.id.button);
        //btn.setBackground(getResources().getDrawable(R.drawable.btn_background_round));
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*textToSpeech.speak("测试SQ45语音播报",
                        TextToSpeech.QUEUE_ADD, null);*/
                /*mWhiteList = mWhiteEv.getText().toString();
                if (!mWhiteList.equals("")) {
                    mDeviceManager.whiteListsAppInsert(mWhiteList);
                    // mDeviceManager.whiteListAppRemove("com.urovo.devicecontrol");
                    Toast.makeText(MainActivity.this, "设置白名单成功！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "编辑框内容不能为空,请重新输入！", Toast.LENGTH_SHORT).show();
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
                int value = "".contentEquals(mWhiteEv.getText()) ? 0 : Integer.parseInt(mWhiteEv.getText().toString());
                writeNV(value);

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
                        Toast.makeText(MainActivity.this, "TTS暂时不支持这种语音的朗读！",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });*/


        Button btn2 = findViewById(R.id.button2);
        btn2.setOnClickListener(new View.OnClickListener() {
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
                Intent intent = new Intent("com.ob.action.SUSPENSION_BACK_BUTTON");
                intent.setPackage("com.ob.floatback");
                stopService(intent);
            }
        });


        intentFilter = new IntentFilter();
        intentFilter.addAction("urovo.rcv.message");
        intentFilter.addAction("android.intent.ACTION_DECODE_DATA");
        netWorkChangeReceiver = new NetWorkChangeReceiver();
        //注册广播
        //registerReceiver(netWorkChangeReceiver, intentFilter);

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
     * NetworkCapabilities.TRANSPORT_CELLULAR    移动数据
     * NetworkCapabilities.TRANSPORT_WIFI        WIFI
     * NetworkCapabilities.TRANSPORT_BLUETOOTH   蓝牙
     * NetworkCapabilities.TRANSPORT_ETHERNET    以太网
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
                 * 网络可用的回调连接成功
                 * */
                @Override
                public void onAvailable(Network network) {
                    //Network 101 WIFI
                    //Network 103 CELLULAR
                    Log.d(TAG, "onAvailable: network = " + network);
                    //设置默认网络
                    if (Build.VERSION.SDK_INT >= 23) {
                        connectivityManager.bindProcessToNetwork(network);
                    } else {
                        ConnectivityManager.setProcessDefaultNetwork(network);
                    }
                    //连上不能上网wifi，开启数据流量测试是否可以ping成功
                    //pingHost("14.215.177.38"); //测试网络 ping www.baidu.com
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
    public static boolean pingHost(String ip) {  //ip为要ping的IP地址
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
     * 用Runtime模拟按键操作
     *
     * @param keyCode 按键事件(KeyEvent)的按键值
     */
    private void sendKeyCode(int keyCode) {
        try {
            String keyCommand = "input keyevent " + keyCode;
            // 调用Runtime模拟按键操作
            Runtime.getRuntime().exec(keyCommand);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private class NetWorkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d("yuanwei", "0000 NetWorkChangeReceiver= " +  intent.getAction());
            //判断网络是否可用
            if (intent.getAction().equals("urovo.rcv.message")) {

                String barcodeString = new String(intent.getByteArrayExtra("barocode"), 0, intent.getIntExtra("length", 0));
                Log.d("yuanwei", "1111urovo.rcv.message barocode= " + barcodeString);
            }
            //判断网络是否可用
            /*if (intent.getAction().equals("android.intent.ACTION_DECODE_DATA")) {

                Log.d("yuanwei", "2222urovo.rcv.message barocode= " + intent.getByteArrayExtra("barcode_string") +
                        " length= " + intent.getIntExtra("length", 0));
            }*/
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //销毁广播
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
