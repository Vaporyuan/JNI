package com.example.demo;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private final String ACTION_BOOT = "android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(ACTION_BOOT)){
            Log.d("yuanwei", "com.seuic.kysy isAvailable" + "ACTION_BOOT mAppRow.systemApp = ");
        }

    }
}
