package com.example.ktaudio

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.RouteInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.net.EthernetManager
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class MainActivity : AppCompatActivity() {
    private lateinit var tv1: TextView
    private lateinit var tv2: TextView
    private lateinit var tv3: TextView
    private lateinit var btn1: Button
    private lateinit var btn2: Button
    private lateinit var btn3: Button
    private lateinit var audioManager: AudioManager
    private lateinit var mEthManager: EthernetManager
    private var count: Int = 0
    private var mService = Intent("com.ob.action.SUSPENSION_BACK_BUTTON")
    val instance by lazy { this }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getHostIP(): String {
        var ipAddress: String = "null"
        var gatewayAddress: String = "null"
        val paramTypes: Array<Class<*>?> = arrayOfNulls(1)
        paramTypes[0] = Integer.TYPE

        val params = arrayOfNulls<Any>(1)
        //params[0] = ConnectivityManager.TYPE_ETHERNET
        params[0] = ConnectivityManager.TYPE_MOBILE;

        var lp: LinkProperties? = null
        val mCM=getSystemService(Context.CONNECTIVITY_SERVICE)
        try {
            val hiddenMethod: Method = mCM.javaClass.getMethod("getLinkProperties", *paramTypes)
            lp = hiddenMethod.invoke(mCM, *params) as LinkProperties?
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
        //LinkProperties lp  = mCM.getLinkProperties(ConnectivityManager.TYPE_ETHERNET);
        //LinkProperties lp  = mCM.getLinkProperties(ConnectivityManager.TYPE_ETHERNET);
        if (lp != null) {
            val linkAddressList = lp.linkAddresses
            if (linkAddressList.isNotEmpty()) {
                val l = linkAddressList[linkAddressList.size - 1]
                val inetAddress = l.address
                ipAddress = inetAddress.hostAddress
                Log.d("yuanwei", "ipAddress = $ipAddress")
            }

            val routeInfoList= lp.routes
            if (routeInfoList.isNotEmpty()) {
                val r: RouteInfo = routeInfoList[routeInfoList.size - 1]
                val routeAddress = r.gateway
                gatewayAddress = if (routeAddress != null) {
                    routeAddress.hostAddress
                }else{
                    ""
                }
                Toast.makeText(this, "gatewayAddress = $gatewayAddress", Toast.LENGTH_LONG).show()
            }
        }
        return ipAddress
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("SetTextI18n", "WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mEthManager = this.getSystemService("ethernet") as EthernetManager
        tv1 = findViewById(R.id.kl_tv)
        tv2 = findViewById(R.id.kl_tv2)
        tv3 = findViewById(R.id.kl_tv3)
        btn1 = findViewById(R.id.kl_button)
        btn1.setOnClickListener {
            /*if (Build.VERSION.SDK_INT >= 23) {
                if (!Settings.canDrawOverlays(this)) {
                    var intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivityForResult(intent, 1);
                } else {
                    //TODO do something you need
                }
            }
            mService.setPackage("com.example.ktaudio")
            startService(mService)
            setVolume(AudioManager.STREAM_MUSIC, 1)
            tv1.text = "当前媒体音量：${getVolume(AudioManager.STREAM_MUSIC)},最大媒体音量：${getMaxVolume(AudioManager.STREAM_MUSIC)}"*/
            tv1.text = getHostIP()
            /*mEthManager.addListener(EthernetManager.Listener {
                iface, isAvailable -> tv1.text = "iface = $iface , isAvailable = $isAvailable"
            })*/

        }
        btn2 = findViewById(R.id.kl_button2)
        btn2.setOnClickListener {
            setVolume(AudioManager.STREAM_SYSTEM, 2)
            tv2.text = "当前系统音量：${getVolume(AudioManager.STREAM_SYSTEM)},最大系统音量：${getMaxVolume(AudioManager.STREAM_SYSTEM)}"
        }
        btn3 = findViewById(R.id.kl_button3)
        btn3.setOnClickListener {
            setVolume(AudioManager.STREAM_ALARM, 3)
            tv3.text = "当前闹钟音量：${getVolume(AudioManager.STREAM_ALARM)},最大闹钟音量：${getMaxVolume(AudioManager.STREAM_ALARM)}"
        }
    }

    private fun setVolume(type: Int, value: Int) {
        audioManager.setStreamVolume(type, value, AudioManager.FLAG_PLAY_SOUND or AudioManager.FLAG_SHOW_UI);
    }

    private fun getVolume(type: Int): Int {
        return audioManager.getStreamVolume(type)
    }

    private fun getMaxVolume(type: Int): Int {
        return audioManager.getStreamMaxVolume(type)
    }
}