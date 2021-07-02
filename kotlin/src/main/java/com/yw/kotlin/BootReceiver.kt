package com.yw.kotlin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class BootReceiver : BroadcastReceiver() {
    private val actionBoot: String = "android.intent.action.BOOT_COMPLETED"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(actionBoot)) {
            Toast.makeText(context, "Kotlin demo 开机广播接收到啦!", Toast.LENGTH_LONG).show()
        }
    }
}