package com.ob.fangchai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.device.MalioSeManager;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "OB_BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            MalioSeManager malioSeManager = MalioSeManager.getInstance();
            malioSeManager.open();
            boolean isSeTrigger = malioSeManager.getTamperStatus();
            malioSeManager.close();
            if (!isSeTrigger) return;
            Intent mIntent = new Intent("malio.intent.se.trigger");
            mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mIntent);
            Log.d(TAG, "BootReceiver start action malio.intent.se.trigger");
        }
    }
}
