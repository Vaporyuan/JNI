package com.malio.secureelementdtest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;

import com.alipay.iot.pal.api.ISecureElement;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private ISecureElement iSecureElement;
    private static final String TAG = "SecureElementTest";
    boolean isBind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        slog("onCreate in ...");

        bind();
        Button btnTest = findViewById(R.id.btn_test);
        btnTest.setOnClickListener(v -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                if (iSecureElement != null) {
                    int openPreventDisassembly = iSecureElement.openPreventDisassembly();
                    slog("onCreate openPreventDisassembly = " + openPreventDisassembly);
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                if (iSecureElement != null) {
                    int getDisassemblyStatus = iSecureElement.getDisassemblyStatus();
                    slog("onCreate getDisassemblyStatus = " + getDisassemblyStatus);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            unBind();
        });

    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            slog("onCreate onServiceConnected ...");
            iSecureElement = ISecureElement.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            slog("onCreate onServiceDisconnected ...");
            iSecureElement = null;
        }
    };

    /**
     * 绑定服务
     */
    private void bind() {
        Intent intent = new Intent();
        intent.setAction("com.alipay.iot.action.SE");
        intent.setPackage("com.alipay.iot.pal");
        isBind = MainActivity.this.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 解绑服务
     */
    private void unBind() {
        if (connection != null && isBind) {
            MainActivity.this.unbindService(connection);
            isBind = false;
        }
    }

    private void slog(String info) {
        Log.d(TAG, info);
    }
}