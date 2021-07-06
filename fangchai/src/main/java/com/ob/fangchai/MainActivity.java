package com.ob.fangchai;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.StatusBarManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.device.MalioSeManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.HexDump;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.List;


public class MainActivity extends Activity {
    private static final String TAG = "OB_FANG_CHAI";
    private Context mContext;
    private MalioSeManager malioSeManager;
    private StatusBarManager statusBarManager;
    private IActivityManager mIActivityManager;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mContext = this;
        malioSeManager = MalioSeManager.getInstance();
        malioSeManager.open();//open se
        if (!malioSeManager.getTamperStatus()) {
            Log.d(TAG, "onCreate getTamperStatus is ok, finish.");
            finish();
            return;
        }
        statusBarManager = (StatusBarManager) mContext.getSystemService(Context.STATUS_BAR_SERVICE);
        if (mIActivityManager == null) {
            mIActivityManager = IActivityManager.Stub.asInterface(ServiceManager.getService(Context.ACTIVITY_SERVICE));
        }

        mTextView = findViewById(R.id.tv_malio_lock);
        updateTextView();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //3S后再更新一次状态，以防误差
                updateTextView();
            }
        }, 3000);

        //屏蔽状态栏，进入霸屏模式
        disableStatusBar();
        Log.d(TAG, "onCreate getPackageName(): " + getPackageName());
        setLockTaskMode(getPackageName(), true);
    }

    private void updateTextView() {
        String tamperStr = "0x" + HexDump.toHexString(malioSeManager.getTamperStatusInt());
        mTextView.setText(String.format(getResStr(R.string.malio_se_lock_mind), tamperStr));
        mTextView.setTextColor(Color.RED);
        Log.d(TAG, "onCreate updateTextView, tamperStr = " + tamperStr);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy...");
        malioSeManager.close();
        setLockTaskMode(getPackageName(), false);
    }

    /**
     * 获取Str
     *
     * @param resId
     * @return
     */
    private String getResStr(int resId) {
        return getResources().getString(resId);
    }

    /**
     * 屏蔽状态栏
     */
    public void disableStatusBar() {
        statusBarManager.disable(StatusBarManager.DISABLE_EXPAND |
                StatusBarManager.DISABLE_NOTIFICATION_ALERTS |
                StatusBarManager.DISABLE_NOTIFICATION_ICONS |
                StatusBarManager.DISABLE_NOTIFICATION_TICKER |
                StatusBarManager.DISABLE_RECENT |
                StatusBarManager.DISABLE_HOME |
                StatusBarManager.DISABLE_SEARCH);
    }

    /**
     * 开启状态栏
     */
    public void enableStatusBar() {
        statusBarManager.disable(StatusBarManager.DISABLE_NONE);
    }

    /**
     * 设置运行该应用后，状态栏／home/back禁用，屏幕固定状态
     *
     * @param packageName
     * @param enable
     */
    public void setLockTaskMode(String packageName, boolean enable) {
        if (TextUtils.isEmpty(packageName)) return;
        try {
            if (enable) {
                String whitePackages = Settings.System.getString(mContext.getContentResolver(), "lockTaskWhitePackages");
                whitePackages = addPackageName(whitePackages, packageName);
                mIActivityManager.updateLockTaskPackages(UserHandle.myUserId(), whitePackages.split(","));
                mIActivityManager.updateLockTaskFeatures(UserHandle.myUserId(),
                        DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD | DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS);
                List<ActivityManager.RunningTaskInfo> taskList;
                try {
                    taskList = mIActivityManager.getTasks(1);
                    if ((taskList != null)
                            && (taskList.get(0) != null)
                            && (taskList.get(0).topActivity != null)) {
                        int stackId = taskList.get(0).id;
                        Log.d(TAG, "setLockTaskMode packageName:" + taskList.get(0).topActivity.getPackageName() + " stackId:" + stackId);
                        if (stackId >= 0) {
                            mIActivityManager.startSystemLockTaskMode(stackId);
                        } else {
                            mIActivityManager.startSystemLockTaskMode(1);
                        }
                    } else {
                        mIActivityManager.startSystemLockTaskMode(1);
                    }
                    Settings.System.putString(mContext.getContentResolver(), "lockTaskPackage", packageName);
                } catch (Exception e) {
                    Log.d(TAG, "setLockTaskMode get the activity stack failed");
                }
            } else {
                Settings.System.putString(mContext.getContentResolver(), "lockTaskPackage", "");
                if (mIActivityManager.isInLockTaskMode()) {
                    Log.d(TAG, "Screen is in isInLockTaskMode, stop it!");
                    mIActivityManager.stopSystemLockTaskMode();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String addPackageName(String packageNames, String packageName) {
        if (TextUtils.isEmpty(packageNames)) {
            return packageName;
        }
        if (!containsPackageName(packageNames, packageName)) {
            if (packageNames.length() > 0) {
                packageNames += ",";
            }
            packageNames += packageName;
        }
        return packageNames;
    }

    private static boolean containsPackageName(String packageNames, String packageName) {
        int index = packageNames.indexOf(packageName);
        if (index < 0) return false;
        if (index > 0 && packageNames.charAt(index - 1) != ',') return false;
        int charAfter = index + packageName.length();
        if (charAfter < packageNames.length() && packageNames.charAt(charAfter) != ',')
            return false;
        return true;
    }
}