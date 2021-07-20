package com.malio.socket;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;


public class Client extends Thread {
    private static final String TAG = MainActivity.TAG;
    private static final String CMD_HELLO = "HELLO";
    private static final String CMD_GET_DATA = "GETDATA";
    private static final String CMD_OVER = "OVER";

    private MainActivity mContext;
    private String mIP;
    private int mPort = 12345;
    private boolean mExit = false;

    private Socket mSocket;
    private OutputStream mOut;
    private PrintWriter mWriter;
    private InputStream mIn;
    private InputStreamReader mReader;
    private BufferedReader mBufReader;

    public static final int SOCK_CONNECT_STATUS = 1;

    public Client(MainActivity context, String ip, int port) {
        mContext = context;
        mIP = ip;
        mPort = port;
    }

    @Override
    public void run () {
        String cmd, rsp, right_rsp;
        boolean ok = false;

        do {
            //创建socket
            mContext.DisplayMessage("开始建立链接：" + "(" + mPort + ")...");
            if (!open(mIP, mPort)) {
                break;
            }

            // 握手
            right_rsp = CMD_HELLO + ":OK";
            send(CMD_HELLO);
            rsp = receive();
            if (!right_rsp.equals(rsp)) {
                break;
            }

            // 请求数据
            right_rsp = CMD_GET_DATA + ":";
            send(CMD_GET_DATA);
            // 同时读本地气压计
            PressData data = PressData.getInstance();
            mContext.sendMessage(mContext.GET_PRESSURE_VAL, data);
            synchronized (data) {
                try {
                    data.wait(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            rsp = receive();
            if (rsp == null || !rsp.startsWith(right_rsp)) {
                break;
            }
            String sub_rsp = rsp.substring(right_rsp.length());
            Integer val =  Integer.valueOf(sub_rsp);

            int[] pressVal = new int[2];
            pressVal[0] = val;
            pressVal[1] = data.average();
            mContext.sendMessage(mContext.BEGIN_PRESS_CAL, pressVal);

            //回收PressData
            PressData.freeData(data);

            // 通读结束
            send(CMD_OVER);

            ok = true;
        } while (false);

        if (!ok) {
            mContext.sendMessage(mContext.SOCK_CONNECT_FAIL, null);
        }

        //关闭连接
        close();
    }

    private boolean open(String ip, int port) {
        boolean ret = false;

        try {
            //1.创建监听指定服务器地址以及指定服务器监听的端口号
            mSocket = new Socket(ip, port);
            mOut = mSocket.getOutputStream();
            mWriter = new PrintWriter(mOut);

            mIn = mSocket.getInputStream();
            mReader = new InputStreamReader(mIn);
            mBufReader = new BufferedReader(mReader);

            ret = true;
            mContext.DisplayMessage("建立链接成功!");
        } catch (IOException e) {
            mContext.DisplayMessage("建立链接失败!");
            e.printStackTrace();
        }

        return ret;
    }

    private void close() {
        if (mSocket != null) {
            try {
                mSocket.shutdownInput();
                mSocket.shutdownOutput();
                mOut.close();
                mBufReader.close();
                mReader.close();
                mIn.close();

                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void send(String cmd) {
        mContext.DisplayMessage("发送：" + cmd);
        mWriter.println(cmd);
        mWriter.flush();
    }

    private String receive() {
        String data = null;

        try {
            while (!mExit && (data == null)) {
                data = mBufReader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mContext.DisplayMessage("收到：" + data);

        return data;
    }

    public void exit() {
        mExit = true;
    }
}