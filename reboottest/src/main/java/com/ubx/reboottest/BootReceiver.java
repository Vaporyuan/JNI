package com.ubx.reboottest;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.device.DeviceManager;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static String device_start_cation = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("UBX_REBOOT_TEST", "UBX_REBOOT_TEST BootReceiver = " + intent.getAction());
        if (intent.getAction().equals(device_start_cation)) {
            Intent mIntent = new Intent();
            mIntent.setComponent(ComponentName.unflattenFromString("com.ubx.reboottest/.MainActivity"));
            mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mIntent);
            Log.d("UBX_REBOOT_TEST", "UBX_REBOOT_TEST mIntent = " + mIntent.getComponent().flattenToString());
        }
    }
}