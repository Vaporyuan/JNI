package com.ob.floatback;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.app.TaskStackListener;
import android.app.ActivityManager.StackInfo;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Android P之监听Process Activity TaskStack状态变化
 * https://blog.csdn.net/Sunxiaolin2016/article/details/101549697
 * service本身不能更新ui
 */
public class SystemActivityMonitoringService extends Service {
    private static final String TAG = "SystemMonitoringService";
    private static final boolean DEBUG = false;
    private static IActivityManager mAm;
    private Intent fxService;

    @Override
    public void onCreate() {
        super.onCreate();
        mAm = IActivityManager.Stub.asInterface(ServiceManager.getService(Context.ACTIVITY_SERVICE));
        registerTaskStackListener();
        fxService = new Intent(this, FxService.class);
        //接收开机广播启动时判断当前状态是否显示悬浮按钮
        if (!isHomeTopActivity()) startFxService();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterTaskStackListener();
        stopFxService();
    }

    TaskStackListener mTaskListener = new TaskStackListener() {
        @Override
        public void onTaskStackChanged() throws RemoteException {
            super.onTaskStackChanged();
            if (DEBUG) Log.i(TAG, "onTaskStackChanged");
            updateTasks();
        }

        @Override
        public void onTaskMovedToFront(int i) throws RemoteException {
            super.onTaskMovedToFront(i);
            Log.i(TAG, "onTaskMovedToFront i = " + i);
        }
    };

    public void registerTaskStackListener() {
        // Monitoring both listeners are necessary as there are cases where one listener cannot
        // monitor activity change.
        try {
            mAm.registerTaskStackListener(mTaskListener);
            if (DEBUG) Log.e(TAG, "registerTaskStackListener mTaskListener");
        } catch (Exception e) {
            Log.e(TAG, "cannot register activity monitoring", e);
            throw new RuntimeException(e);
        }
    }

    public void unRegisterTaskStackListener() {
        try {
            mAm.unregisterTaskStackListener(mTaskListener);
            if (DEBUG) Log.e(TAG, "unRegisterTaskStackListener mTaskListener");
        } catch (Exception e) {
            Log.e(TAG, "cannot unRegister activity monitoring", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Container to hold info on top task in an Activity stack
     */
    public static class TopTaskInfoContainer {
        public final ComponentName topActivity;
        public final int taskId;
        public final StackInfo stackInfo;

        private TopTaskInfoContainer(ComponentName topActivity, int taskId, StackInfo stackInfo) {
            this.topActivity = topActivity;
            this.taskId = taskId;
            this.stackInfo = stackInfo;
        }

        public boolean isMatching(TopTaskInfoContainer taskInfo) {
            return taskInfo != null
                    && Objects.equals(this.topActivity, taskInfo.topActivity)
                    && this.taskId == taskInfo.taskId
                    && this.stackInfo.userId == taskInfo.stackInfo.userId;
        }

        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            return String.format("TaskInfoContainer [topActivity=%s, taskId=%d, stackId=%d, userId=%d",
                    topActivity, taskId, stackInfo.stackId, stackInfo.userId);
        }
    }

    /**
     * K: stack id, V: top task
     */
    private final SparseArray<TopTaskInfoContainer> mTopTasks = new SparseArray<>();

    private void updateTasks() {
        List<StackInfo> infos;
        try {
            infos = mAm.getAllStackInfos();
        } catch (Exception e) {
            Log.e(TAG, "cannot getTasks", e);
            return;
        }
        int focusedStackId = -1;
        try {
            // TODO(b/66955160): Someone on the Auto-team should probably re-work the code in the
            // synchronized block below based on this new API.
            final StackInfo focusedStackInfo = mAm.getFocusedStackInfo();
            if (focusedStackInfo != null) {
                focusedStackId = focusedStackInfo.stackId;
            }
        } catch (Exception e) {
            Log.e(TAG, "cannot getFocusedStackId", e);
            return;
        }

        synchronized (this) {
            for (StackInfo info : infos) {
                int stackId = info.stackId;
                if (info.taskNames.length == 0 || !info.visible) { // empty stack or not shown
                    mTopTasks.remove(stackId);
                    continue;
                }
                TopTaskInfoContainer newTopTaskInfo = new TopTaskInfoContainer(
                        info.topActivity, info.taskIds[info.taskIds.length - 1], info);

                TopTaskInfoContainer currentTopTaskInfo = mTopTasks.get(stackId);

                // if a new task is added to stack or focused stack changes, should notify
                if (currentTopTaskInfo == null ||
                        !currentTopTaskInfo.isMatching(newTopTaskInfo) ||
                        (focusedStackId == stackId)) {
                    mTopTasks.put(stackId, newTopTaskInfo);
                    if (DEBUG) Log.i(TAG, "New top task: " + newTopTaskInfo);
                    //OB ADD begin 当前活动界面是不是桌面
                    String newTaskPackageName = newTopTaskInfo.topActivity.getPackageName();
                    if (getHomes().contains(newTaskPackageName) && !newTaskPackageName.equals("com.android.settings")) {
                        Log.i(TAG, "New top task: is Launcher " + newTaskPackageName);
                        stopFxService();
                    } else {
                        startFxService();
                    }
                    //OB ADD end
                }
            }
        }
    }

    /**
     * 获得属于桌面的应用的应用包名称
     *
     * @return 返回包含所有包名的字符串列表
     */
    private List<String> getHomes() {
        List<String> packages = new ArrayList<String>();
        PackageManager packageManager = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo ri : resolveInfo) {
            packages.add(ri.activityInfo.packageName);
            if (DEBUG) Log.i(TAG, "launcher packageName = " + ri.activityInfo.packageName);
        }
        return packages;
    }

    private boolean isHomeTopActivity() {
        List<ActivityManager.RunningTaskInfo> taskList;
        try {
            taskList = mAm.getTasks(1);
        } catch (RemoteException e) {
            Log.e(TAG, "get the activity stack failed");
            return false;
        }
        if ((taskList != null)
                && (taskList.get(0) != null)
                && (taskList.get(0).topActivity != null)
                && (taskList.get(0).topActivity.getPackageName() != null)
                && getHomes().contains(taskList.get(0).topActivity.getPackageName())
                && !taskList.get(0).topActivity.getPackageName().equals("com.android.settings")) {
            Log.e(TAG, "isHomeTopActivity: " + taskList.get(0).topActivity.getClassName());
            return true;
        }
        return false;
    }

    /**
     * 启动FxService显示悬浮按钮
     */
    private void startFxService() {
        //如果FxService正在运行则不启动，优化流程
        if (!isServiceRunning(FxService.class.getName())) {
            Log.i(TAG, "New top task: not Launcher, FxService don't run.");
            startService(fxService);
        }

    }

    /**
     * 停止FxService移除悬浮按钮
     */
    private void stopFxService() {
        stopService(fxService);
    }

    /**
     * 判断服务是否运行
     */
    private boolean isServiceRunning(final String className) {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> info = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (info == null || info.size() == 0) return false;
        for (ActivityManager.RunningServiceInfo aInfo : info) {
            if (className.equals(aInfo.service.getClassName())) return true;
        }
        return false;
    }
}
