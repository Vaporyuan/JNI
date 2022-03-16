package com.example.ktaudio;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.input.InputManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

public class FxService extends Service {

    private LinearLayout mFloatLayout;
    private LayoutParams wmParams;
    private WindowManager mWindowManager;
    private Button mFloatView;
    private long mDownTime;
    private static final String TAG = "FxService";
    private boolean moving = false;
    private final boolean DEBUG = false;

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        createFloatView();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        Log.i(TAG, "FxService onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    private void createFloatView() {
        wmParams = new LayoutParams();
        mWindowManager = (WindowManager) getApplication().getSystemService(
                getApplication().WINDOW_SERVICE);
        //如果不设置该TYPE，应用会Crash，报错如下（后面的2002表示设置的type为TYPE_PHONE）：
        //AndroidRuntime: android.view.WindowManager$BadTokenException:
        //Unable to add window android.view.ViewRootImpl$W@c8d1f1a -- permission denied for window type 2002
        wmParams.type = LayoutParams.TYPE_APPLICATION_OVERLAY; //LayoutParams.TYPE_PHONE;
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        wmParams.x = mWindowManager.getDefaultDisplay().getWidth();
        wmParams.y = mWindowManager.getDefaultDisplay().getHeight() / 2;
        wmParams.width = LayoutParams.WRAP_CONTENT;
        wmParams.height = LayoutParams.WRAP_CONTENT;

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.floatbutton, null);
        mWindowManager.addView(mFloatLayout, wmParams);

        if (DEBUG) {
            Log.i(TAG, "mFloatLayout-->left" + mFloatLayout.getLeft());
            Log.i(TAG, "mFloatLayout-->right" + mFloatLayout.getRight());
            Log.i(TAG, "mFloatLayout-->top" + mFloatLayout.getTop());
            Log.i(TAG, "mFloatLayout-->bottom" + mFloatLayout.getBottom());
        }

        mFloatView = mFloatLayout.findViewById(R.id.float_id);
        mFloatView.setFocusableInTouchMode(true);
        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        mFloatView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                int newx = (int) event.getRawX()
                        - mFloatView.getMeasuredWidth() / 2;
                int newy = (int) event.getRawY()
                        - mFloatView.getMeasuredHeight() / 2 - 30;
                int movlev = 100;
                if (newx > wmParams.x + movlev || newx < wmParams.x - movlev
                        || newy > wmParams.y + movlev
                        || newy < wmParams.y - movlev || moving == true) {
                    moving = true;
                    wmParams.x = newx;
                    wmParams.y = newy;
                    if (DEBUG) {
                        Log.i(TAG, "RawX" + event.getRawX());
                        Log.i(TAG, "X" + event.getX());
                        Log.i(TAG, "RawY" + event.getRawY());
                        Log.i(TAG, "Y" + event.getY());
                    }
                    // 刷新
                    mWindowManager.updateViewLayout(mFloatLayout, wmParams);
                }
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    mDownTime = SystemClock.uptimeMillis();
                    mFloatView.setBackgroundResource(R.drawable.btn_background_round_pressed);
                    //sendEvent(KeyEvent.KEYCODE_BACK, KeyEvent.ACTION_DOWN, 0, mDownTime);
                    return false;
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    mFloatView.setBackgroundResource(R.drawable.btn_background_round_normal);
                    if (DEBUG) Log.i(TAG, "onTouch moving = " + moving);
                    if (!moving) {
                        //点击的处理
                        if (DEBUG) Log.i(TAG, "onClick mFloatView");
                        sendKeyCode(KeyEvent.KEYCODE_BACK);
                        //sendEvent(KeyEvent.KEYCODE_BACK, KeyEvent.ACTION_UP, 0);
                    }else{
                        //sendEvent(KeyEvent.KEYCODE_BACK, KeyEvent.ACTION_UP, KeyEvent.FLAG_CANCELED);
                    }
                    moving = false;
                    return false;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    return true;
                }
                return false;
            }
        });

        mFloatView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (mFloatLayout != null) {
            mWindowManager.removeView(mFloatLayout);
        }
    }

    /**
     * 用Runtime模拟按键操作
     *
     * @param keyCode 按键事件(KeyEvent)的按键值
     */
    private void sendKeyCode(int keyCode) {
        if (DEBUG) Log.i(TAG, "sendKeyCode-->" + keyCode);
        try {
            String keyCommand = "input keyevent " + keyCode;
            // 调用Runtime模拟按键操作
            Runtime.getRuntime().exec(keyCommand);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*public void sendEvent(int mCode, int action, int flags) {
        sendEvent(mCode, action, flags, SystemClock.uptimeMillis());
    }

    void sendEvent(int mCode, int action, int flags, long when) {
        final int repeatCount = (flags & KeyEvent.FLAG_LONG_PRESS) != 0 ? 1 : 0;
        final KeyEvent ev = new KeyEvent(mDownTime, when, action, mCode, repeatCount,
                0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                flags | KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY,
                InputDevice.SOURCE_KEYBOARD);
        InputManager.getInstance().injectInputEvent(ev, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }*/
}