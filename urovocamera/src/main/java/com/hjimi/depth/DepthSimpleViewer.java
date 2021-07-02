package com.hjimi.depth;

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


public class DepthSimpleViewer extends Thread {

    private boolean mShouldRun = false;

    private ImiDevice mDevice;
    private ImiStreamType mStreamType;
    private GLPanel mGLPanel;
    private int m_DepthValue = 0;
    private int mFps = 0;
    private int mCount = 0;
    private int m_width = 0;
    private int m_height = 0;

    private byte[] color = new byte[3];

    public DepthSimpleViewer(ImiDevice device, ImiStreamType streamType) {
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
            drawDepth(nextFrame);
        }
    }

    public int getDepthValue() {
        return m_DepthValue;
    }

    public int getFps() {
        return mFps;
    }

    private void drawDepth(ImiFrame nextFrame) {
        ByteBuffer frameData = nextFrame.getData();
        int width = nextFrame.getWidth();
        int height = nextFrame.getHeight();
        m_width = width;
        m_height = height;

        int X = width/2;
        int Y = height/2;
        int curIndex = width * Y + X;

        frameData.position(curIndex*2);
        m_DepthValue = (int) ((frameData.get() & 0xFF) | ((frameData.get() & 0xFF)<<8));

        //get rgb data
        frameData = Utils.depth2RGB888(nextFrame, false, false);

        int index = width * Y + X;

        if(width < 640) {
            frameData.position((index - 1) * 3);
            for(int i = 0; i < 3; ++i) {
                frameData.put(color);
            }

            frameData.position((index - 1 - width) * 3);
            for(int i = 0; i < 3; ++i) {
                frameData.put(color);
            }

            frameData.position((index - 1 + width) * 3);
            for(int i = 0; i < 3; ++i) {
                frameData.put(color);
            }
        }
        else {
            frameData.position((index - 2) * 3);
            for(int i = 0; i < 5; ++i) {
                frameData.put(color);
            }

            int index2 = index - width;
            frameData.position((index2 - 2) * 3);
            for(int i = 0; i < 5; ++i) {
                frameData.put(color);
            }

            index2 -= width;
            frameData.position((index2 - 2) * 3);
            for(int i = 0; i < 5; ++i) {
                frameData.put(color);
            }

            int index3 = index + width;
            frameData.position((index3 - 2) * 3);
            for(int i = 0; i < 5; ++i) {
                frameData.put(color);
            }

            index3 += width;
            frameData.position((index3 - 2) * 3);
            for(int i = 0; i < 5; ++i) {
                frameData.put(color);
            }
        }
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
