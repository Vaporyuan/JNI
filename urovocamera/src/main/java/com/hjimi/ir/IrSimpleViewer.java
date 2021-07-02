package com.hjimi.ir;

import android.util.Log;

import com.hjimi.api.iminect.ImiDevice;
import com.hjimi.api.iminect.ImiFrameMode;
import com.hjimi.api.iminect.ImiFrameType;
import com.hjimi.api.iminect.ImiImageFrame;
import com.hjimi.api.iminect.ImiDevice.ImiStreamType;
import com.hjimi.api.iminect.ImiDevice.ImiFrame;
import com.hjimi.api.iminect.Utils;
import com.hjimi.color.GLPanel;

import java.nio.ByteBuffer;


public class IrSimpleViewer extends Thread {

    private boolean mShouldRun = false;

    private ImiDevice mDevice;
    private ImiStreamType mStreamType;
    private GLPanel mGLPanel;
    private int mFps = 0;
    private int mCount = 0;
    private int m_width = 0;
    private int m_height = 0;

    private byte[] color = new byte[3];

    public IrSimpleViewer(ImiDevice device, ImiStreamType streamType) {
        mDevice = device;
        mStreamType = streamType;
    }

    public void setGLPanel(GLPanel GLPanel) {
        this.mGLPanel = GLPanel;
    }

    @Override
    public void run() {
        super.run();

        color[0] = (byte)0xff;
        color[1] = (byte)0x00;
        color[2] = (byte)0x00;

        long startTime = 0;
        long endTime = 0;

        //start read frame.
        while (mShouldRun) {
            ImiFrame nextFrame = mDevice.readNextFrame(mStreamType, 30);

            //frame maybe null, if null, continue.
            if(nextFrame == null){
                continue;
            }

            mCount++;
            if(startTime == 0) {
                startTime = System.currentTimeMillis();
            }

            endTime = System.currentTimeMillis();
            if(endTime - startTime >= 1000) {
                mFps = mCount;
                mCount = 0;
                startTime = endTime;
            }

            //draw color.
            drawIR(nextFrame);
        }
    }

    public int getFps() {
        return mFps;
    }

    private void drawIR(ImiFrame nextFrame) {
        ByteBuffer frameData = nextFrame.getData();
        int width = nextFrame.getWidth();
        int height = nextFrame.getHeight();
        m_width = width;
        m_height = height;

        //get rgb data
        frameData = Utils.ir2RGB888(nextFrame, false);
        //draw depth image.
        mGLPanel.paint(null, frameData, width, height);
    }

    public int getStreamWidth(){
        return m_width;
    }

    public int getStreamHeight(){
        return m_height;
    }

    public void onPause(){
        if(mGLPanel != null){
            mGLPanel.onPause();
        }
    }

    public void onResume(){
        if(mGLPanel != null){
            mGLPanel.onResume();
        }
    }

    public void onStart(){
        if(!mShouldRun){
            mShouldRun = true;

            //start read thread
            this.start();
        }
    }

    public void onDestroy(){
        mShouldRun = false;
    }
}
