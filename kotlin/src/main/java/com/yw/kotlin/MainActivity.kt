package com.yw.kotlin

import android.device.DeviceManager
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView

private const val TAG: String = "yuanwei"

class MainActivity : AppCompatActivity() {
    private lateinit var statusTextView: TextView
    private lateinit var btn1: Button
    private var mDeviceManager = DeviceManager()
    private var count: Int = 0
    private var isClick: Boolean = false

    /**
     * https://blog.csdn.net/qq_30983519/article/details/81155698
     */


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusTextView = findViewById(R.id.kl_tv)
        with(statusTextView) {
            textSize = 18F
            text = generateAnswerString()
            setTextColor(Color.GREEN)
            setOnClickListener {
                if (isClick) {
                    isClick = false
                    statusTextView.setTextColor(Color.YELLOW)
                } else {
                    isClick = true
                    statusTextView.setTextColor(getColor(R.color.colorAccent))
                }
                Log.d(TAG, "TextView current text is ${statusTextView.text}")
            }
        }
        btn1 = findViewById(R.id.kl_button)
        with(btn1) {
            setOnClickListener {
                setTextColor(Color.MAGENTA)
                text = mDeviceManager.versionCode.toString()
                Log.d(TAG, "getVal = " + getVal("13", 666))
            }
        }
    }


    private fun generateAnswerString(): String {
        return if (count == 42) {
            "I have the answer."
        } else {
            "The answer eludes me"
        }
    }

    private fun getVal(a: String, b: Int): Int {
        return Integer.valueOf(a) + b
    }

}
