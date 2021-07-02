package com.example.urovocamera;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hjimi.api.iminect.ImiDevice;
import com.hjimi.api.iminect.ImiDevice.ImiStreamType;
import com.hjimi.api.iminect.ImiDeviceAttribute;
import com.hjimi.api.iminect.ImiDeviceState;
import com.hjimi.api.iminect.ImiFrameMode;
import com.hjimi.api.iminect.ImiFrameType;
import com.hjimi.api.iminect.ImiNect;
import com.hjimi.color.DecodePanel;
import com.hjimi.color.GLPanel;
import com.hjimi.color.SimpleViewer;
import com.hjimi.depth.DepthSimpleViewer;
import com.hjimi.ir.IrSimpleViewer;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private SurfaceView mColorView;
    private GLPanel mGLPanel;
    private GLPanel mDepthGLPanel;
    private GLPanel mIrGLPanel;
    private DecodePanel mDecodePanel;
    private Surface mSurface;
    private TextView mTVAttrs;
    private Button btn_Exit;
    private Button btn_ir;
    private Button btn_pass, btn_fail;

    private TextView tvDepthValue;
    private TextView tvIrValue;
    private ImiDeviceState deviceState = ImiDeviceState.IMI_DEVICE_STATE_CONNECT;

    private boolean bExiting = false;
    private boolean bIR = false;

    private ImiDevice mDevice;
    private MainListener mainlistener;
    private SimpleViewer mViewer;
    private DepthSimpleViewer mDepthViewer;
    private IrSimpleViewer mIrViewer;
    private ImiDeviceAttribute mDeviceAttribute = null;

    private static final int DEVICE_OPEN_SUCCESS = 0;
    private static final int DEVICE_OPEN_FALIED = 1;
    private static final int DEVICE_DISCONNECT = 2;
    private static final int SHOW_DEPTH_VALUE = 3;
    private static final int SHOW_IR_VALUE = 4;
    private static final int MSG_EXIT = 5;

    static class MyHandler extends Handler {
        WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = mActivity.get();
            switch (msg.what) {
                case DEVICE_OPEN_FALIED:
                case DEVICE_DISCONNECT:
                    mainActivity.showMessageDialog((String) msg.obj);
                    break;
                case DEVICE_OPEN_SUCCESS:
                    mainActivity.runViewer();
                    break;
                case SHOW_DEPTH_VALUE:
                    mainActivity.ShowDepthValue();
                    break;
                case SHOW_IR_VALUE:
                    mainActivity.ShowIrValue();
                    break;
                case MSG_EXIT:
                    mainActivity.Exit();
                    break;
            }
        }
    }

    private void ShowDepthValue() {
        tvDepthValue.setText(String.format(Locale.getDefault(), "Depth %dx%d@%dFPS      ", mDepthViewer.getStreamWidth(), mDepthViewer.getStreamHeight(), mDepthViewer.getFps()));
        tvDepthValue.append("Point(" + mDepthViewer.getStreamWidth() / 2 + "," + mDepthViewer.getStreamHeight() / 2 + "): " + mDepthViewer.getDepthValue() + " mm");
        MainHandler.sendEmptyMessageDelayed(SHOW_DEPTH_VALUE, 15);
    }

    private void ShowIrValue() {
        tvIrValue.setText(String.format(Locale.getDefault(), "IR %dx%d@%dFPS      ", mIrViewer.getStreamWidth(), mIrViewer.getStreamHeight(), mIrViewer.getFps()));
        MainHandler.sendEmptyMessageDelayed(SHOW_IR_VALUE, 1000);
    }

    private MyHandler MainHandler = new MyHandler(MainActivity.this);

    private void showMessageDialog(String errMsg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(errMsg);
        builder.setPositiveButton("quit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
                bExiting = true;
                new Thread(new MainActivity.ExitRunnable()).start();
            }
        });
        builder.show();
    }

    private void runViewer() {
        //open device success.
        mDeviceAttribute = mDevice.getAttribute();
        mTVAttrs.setText("Device SerialNumber : " + mDeviceAttribute.getSerialNumber());

        // set frame mode
        ImiFrameMode frameMode = mDevice.getCurrentFrameMode(ImiDevice.ImiStreamType.COLOR);
        mDevice.setFrameMode(ImiDevice.ImiStreamType.COLOR, frameMode);
        Log.d("#####", "Frame mode: " + frameMode.getResolutionX() + ", " + frameMode.getResolutionY());

        mViewer = new SimpleViewer(mDevice, ImiFrameType.COLOR);

        switch (frameMode.getFormat()) {
            case IMI_PIXEL_FORMAT_IMAGE_H264:
                mDecodePanel.initDecoder(mSurface, frameMode.getResolutionX() / 2, frameMode.getResolutionY() / 2);
                mViewer.setDecodePanel(mDecodePanel);
                break;
            default:
                mColorView.setVisibility(View.GONE);
                mGLPanel.setVisibility(View.VISIBLE);
                mViewer.setGLPanel(mGLPanel);
                break;
        }

        ViewGroup.LayoutParams param = mGLPanel.getLayoutParams();
        param.width = frameMode.getResolutionX() / 2;
        param.height = frameMode.getResolutionY() / 2;
        mGLPanel.setLayoutParams(param);
        mViewer.onStart();
        mDevice.startStream(ImiDevice.ImiStreamType.COLOR.toNative());


        //DepthView
        ImiFrameMode depthFrameMode = mDevice.getCurrentFrameMode(ImiStreamType.DEPTH);
        mDevice.setFrameMode(ImiStreamType.DEPTH, depthFrameMode);
        Log.d("#######", "Depth frame mode: " + frameMode.getResolutionX() + ", " + depthFrameMode.getResolutionY());

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mDepthGLPanel.getLayoutParams();
        params.width = frameMode.getResolutionX() / 2;
        params.height = frameMode.getResolutionY() / 2;
        mDepthGLPanel.setLayoutParams(params);
        mDepthViewer = new DepthSimpleViewer(mDevice, ImiStreamType.DEPTH);
        mDepthViewer.setGLPanel(mDepthGLPanel);

        //start viewer
        mDepthViewer.onStart();
        MainHandler.sendEmptyMessage(SHOW_DEPTH_VALUE);
        mDevice.startStream(ImiStreamType.DEPTH.toNative());


        //IRView
        ImiFrameMode curMode = mDevice.getCurrentFrameMode(ImiStreamType.IR);
        mDevice.setFrameMode(ImiStreamType.IR, curMode);
        mDevice.startStream(ImiStreamType.IR.toNative());

        RelativeLayout.LayoutParams irParams = (RelativeLayout.LayoutParams) mIrGLPanel.getLayoutParams();
        irParams.width = curMode.getResolutionX() / 2;
        irParams.height = curMode.getResolutionY() / 2;
        mIrGLPanel.setLayoutParams(irParams);
    }

    private class MainListener implements ImiDevice.OpenDeviceListener {

        @Override
        public void onOpenDeviceSuccess() {
            MainHandler.sendEmptyMessage(DEVICE_OPEN_SUCCESS);
        }

        @Override
        public void onOpenDeviceFailed(String errorMsg) {
            //open device falied.
            MainHandler.sendMessage(MainHandler.obtainMessage(DEVICE_OPEN_FALIED, errorMsg));
        }
    }

    private class OpenDeviceRunnable implements Runnable {

        @Override
        public void run() {
            ImiNect.initialize();
            mDevice = ImiDevice.getInstance();
            mainlistener = new MainListener();
            mDevice.open(MainActivity.this, 0, mainlistener);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //USB 拔插动作, 这个方法都会被调用.
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mColorView = (SurfaceView) findViewById(R.id.color_view);
        mTVAttrs = (TextView) findViewById(R.id.tv_show_attrs);
        tvDepthValue = (TextView) findViewById(R.id.tv_depthvalue);
        mDepthGLPanel = (GLPanel) findViewById(R.id.sv_depth_view);
        tvIrValue = (TextView) findViewById(R.id.tv_ir_value);
        mIrGLPanel = (GLPanel) findViewById(R.id.sv_ir_view);
        mColorView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                mSurface = surfaceHolder.getSurface();
                mDecodePanel = new DecodePanel();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                mDecodePanel.stopDecoder();
            }
        });

        mGLPanel = (GLPanel) findViewById(R.id.sv_color_view);

        btn_Exit = (Button) findViewById(R.id.button_Exit);
        btn_Exit.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bExiting) {
                    Log.d("@@@@@", "EXITING...");
                    return;
                }

                bExiting = true;
                new Thread(new MainActivity.ExitRunnable()).start();
            }
        });

        btn_ir = (Button) findViewById(R.id.button_ir);
        btn_ir.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("@@@@@", "btn_ir bIR = " + bIR);
                if (!bIR) {
                    Log.d("@@@@@", "btn_ir bIR open IR");
                    // stop DEPTH
                    mDevice.stopStream(ImiStreamType.DEPTH.toNative());
                    mDepthViewer.onDestroy();
                    if (mDepthViewer != null) {
                        mDepthViewer.interrupt();
                        mDepthViewer = null;
                    }
                    MainHandler.removeMessages(SHOW_DEPTH_VALUE);
                    //start IR
                    mIrViewer = new IrSimpleViewer(mDevice, ImiStreamType.IR);
                    mIrViewer.setGLPanel(mIrGLPanel);
                    mIrViewer.onStart();
                    MainHandler.sendEmptyMessage(SHOW_IR_VALUE);
                    mDevice.startStream(ImiStreamType.IR.toNative());
                    btn_ir.setText("GO DEPTH");
                    bIR = true;
                } else if (bIR) {
                    Log.d("@@@@@", "btn_ir bIR open DEPTH");
                    // stop IR
                    mDevice.stopStream(ImiStreamType.IR.toNative());
                    mIrViewer.onDestroy();
                    if (mIrViewer != null) {
                        mIrViewer.interrupt();
                        mIrViewer = null;
                    }
                    MainHandler.removeMessages(SHOW_IR_VALUE);
                    //start DEPTH
                    mDepthViewer = new DepthSimpleViewer(mDevice, ImiStreamType.DEPTH);
                    mDepthViewer.setGLPanel(mDepthGLPanel);
                    mDepthViewer.onStart();
                    MainHandler.sendEmptyMessage(SHOW_DEPTH_VALUE);
                    mDevice.startStream(ImiStreamType.DEPTH.toNative());
                    btn_ir.setText("GO IR");
                    bIR = false;
                }
            }
        });

        btn_pass = (Button) findViewById(R.id.btn_pass);
        btn_pass.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                Exit();
            }
        });
        btn_fail = (Button) findViewById(R.id.btn_fail);
        btn_fail.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                Exit();
            }
        });

        new Thread(new MainActivity.OpenDeviceRunnable()).start();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mViewer != null) {
            mViewer.onResume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    private class ExitRunnable implements Runnable {

        @Override
        public void run() {
            if (mViewer != null) {
                mViewer.onPause();
            }

            //destroy viewer.
            if (mViewer != null) {
                mViewer.onDestroy();
            }

            if (mDevice != null) {
                mDevice.close();
                mDevice = null;
                ImiDevice.destroy();
            }

            ImiNect.destroy();

            MainHandler.sendEmptyMessage(MSG_EXIT);
        }
    }

    private void Exit() {

        finish();

        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void onBackPressed() {
        Log.d("@@@@@", "refuse back to exit");
        return;
    }
}
