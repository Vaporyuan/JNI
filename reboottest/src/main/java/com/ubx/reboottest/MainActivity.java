package com.ubx.reboottest;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.device.DeviceManager;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static String TAG = "UBX_REBOOT_TEST";
    DeviceManager deviceManager = new DeviceManager();
    private TextView mTextView;
    private EditText mEditText;
    private Button btnStart, btnStop, btnReboot;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor editor;
    private int COUNT = 0;
    private boolean isRebootRun = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEditText = findViewById(R.id.ditText);
        mEditText.requestFocus();
        mTextView = findViewById(R.id.textView);
        btnStart = findViewById(R.id.button);
        btnStart.setOnClickListener(this);
        btnStop = findViewById(R.id.button2);
        btnStop.setOnClickListener(this);
        btnReboot = findViewById(R.id.button3);
        btnReboot.setOnClickListener(this);
        mSharedPreferences = getSharedPreferences("reboot_test", Context.MODE_PRIVATE);
        editor = mSharedPreferences.edit();
        deviceManager.clearLock();//清除锁屏

        COUNT = mSharedPreferences.getInt("count", 0);
        log("onCreate COUNT= " + COUNT);
        if (COUNT > 0) {
            mEditText.setText(String.valueOf(COUNT));
            mTextView.setText("还剩下重启次数：" + COUNT);
        }

        isRebootRun = mSharedPreferences.getBoolean("is_reboot_run", false);
        btnReboot.setText(isRebootRun ? "已开启开机自动运行" : "已关闭开机自动运行");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                log("onCreate wait 8s ");
                COUNT = mSharedPreferences.getInt("count", 0);//延时后重新获取一次
                if (COUNT > 0) {
                    COUNT = COUNT - 1;
                    editor.putInt("count", COUNT);
                    editor.commit();
                    deviceManager.shutdown(true);//重启
                }
            }
        }, 8000);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                COUNT = mEditText.getText().toString().equals("") ? 0 : Integer.parseInt(mEditText.getText().toString());
                if (COUNT < 1) {
                    Toast.makeText(this, "测试数次不能少于一次！", Toast.LENGTH_SHORT).show();
                    return;
                }
                COUNT = COUNT - 1;//已开始本次测试，次数减1
                editor.putInt("count", COUNT);
                editor.commit();
                log("onClick COUNT= " + COUNT);
                deviceManager.shutdown(true);//重启
                break;
            case R.id.button2:
                editor.putInt("count", 0);
                editor.commit();
                mEditText.setText("");
                mTextView.setText("已手动取消测试");
                log("onClick cancel test ");
                break;
            case R.id.button3:
                isRebootRun = mSharedPreferences.getBoolean("is_reboot_run", false);//需要重新获取一次
                if (!isRebootRun) {
                    deviceManager.setAutoRunningApp(ComponentName.unflattenFromString("com.ubx.reboottest/.MainActivity"), 1); // 1：添加  2：移除
                    editor.putBoolean("is_reboot_run", true);
                    editor.commit();
                    btnReboot.setText("已开启开机自动运行");
                } else {
                    deviceManager.setAutoRunningApp(ComponentName.unflattenFromString("com.ubx.reboottest/.MainActivity"), 2); // 1：添加  2：移除
                    editor.putBoolean("is_reboot_run", false);
                    editor.commit();
                    btnReboot.setText("已关闭开机自动运行");
                }
                break;
        }
    }

    private void log(String info) {
        Log.d(TAG, info);
    }
}
