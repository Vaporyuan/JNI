package com.example;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MessengerService extends Service {

    private String TAG = "MessengerService";
    private InstallBinder mBinder = new InstallBinder();

    public class InstallBinder extends Binder {
        public void startInstall(String a) {
            a.equals("");
            Log.d(TAG, "startDownload executed");
        }

        public int getProgress() {
            Log.d(TAG, "getProgress executed");
            return 0;
        }

    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return mBinder;
    }

}
