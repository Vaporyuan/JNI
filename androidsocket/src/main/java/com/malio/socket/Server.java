package com.malio.socket;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;


public class Server extends Thread {
    private static final String CMD_HELLO = "HELLO";
    private static final String CMD_GET_DATA = "GETDATA";
    private static final String CMD_OVER = "OVER";

    private MainActivity mContext;
    private int mPort;

    private ServerSocket mServerSocket;
    private boolean mExit = false;

    public Server(MainActivity context, int port) {
        mContext = context;
        mPort = port;
    }

    public InetAddress getIpAddress() {

        InetAddress inetAddress;
        InetAddress hotspotIP = null;
        //NetworkInterface.getNetworkInterfaces(获取主机上所有已知的网络接口，然后遍历每个NI的地址)
        try {
            for (Enumeration<NetworkInterface> networkInterface = NetworkInterface.getNetworkInterfaces(); networkInterface.hasMoreElements();) {
                NetworkInterface singleInterface = networkInterface.nextElement();
                for (Enumeration<InetAddress> IpAddresses = singleInterface.getInetAddresses(); IpAddresses.hasMoreElements();) {
                    inetAddress = IpAddresses.nextElement();
                    mContext.DisplayMessage("displayName = " + singleInterface.getDisplayName() + " ,inetAddress = " + inetAddress);
                    if (!inetAddress.isLoopbackAddress()
                            && (singleInterface.getDisplayName().contains("wlan0")
                            || singleInterface.getDisplayName().contains("eth0")
                            || singleInterface.getDisplayName().contains("ap0"))) {
                        hotspotIP = inetAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return hotspotIP;
    }

    @Override
    public void run() {
        // 1.新建ServerSocket对象，创建指定端口的连接
        try {
            mContext.DisplayMessage("local host: " + getIpAddress().getHostAddress());
            mServerSocket = new ServerSocket(mPort);
            mContext.DisplayMessage("建立服务器Socket成功！");
        } catch (IOException e) {
            e.printStackTrace();
            mContext.DisplayMessage("建立服务器Socket失败！");
            return;
        }

        // 2.进行监听
        mContext.DisplayMessage("服务器开始监听......");
        Socket socket;
        while (!mExit) {
            // 开始监听端口，并接收到此套接字的连接。
            try {
                socket = mServerSocket.accept();
                new ClientWork(socket).start();
            } catch (IOException e) {
                e.printStackTrace();
                mContext.DisplayMessage("服务器监听出错");
            }
        }
    }

    public void exit() {
        mExit = true;
        try {
            mServerSocket.close();// 关闭ServerSocket
        } catch (IOException e) {
        }
    }

    private class ClientWork extends Thread {
        private Socket mSocket;
        private InputStream mIn;
        private InputStreamReader mReader;
        private BufferedReader mBufReader;
        private OutputStream mOut;
        private PrintWriter mWriter;

        public ClientWork(Socket s) {
            mSocket = s;
            try {
                mIn = mSocket.getInputStream();
                mReader = new InputStreamReader(mIn);
                mBufReader = new BufferedReader(mReader);
                mOut = mSocket.getOutputStream();
                mWriter = new PrintWriter(mOut);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            InetAddress address = mSocket.getInetAddress();
            String ip = address.getHostAddress();
            int port = mSocket.getPort();
            String tag = "[" + ip + "(" + port + ")]";

            mContext.DisplayMessage("已建立与" + tag + ")的通讯");
            try {
                // 等待数据
                boolean exit = false;
                while (!exit) {
                    String rsp;
                    String req = receive(tag);
                    // 收到握手信号
                    if (CMD_HELLO.equals(req)) {
                        rsp = CMD_HELLO + ":OK";
                        send(tag, rsp);
                    } else if (CMD_GET_DATA.equals(req)) {
                        if (!getPressVal(tag)) {
                            exit = true;
                        }
                    } else if (CMD_OVER.equals(req)) {
                        exit = true;
                    } else {
                        mContext.DisplayMessage(tag + "读错误！");
                        exit = true;
                    }
                }

                mContext.DisplayMessage("[" + ip + "]关闭Socket");
                mSocket.shutdownInput();
                mSocket.shutdownOutput();
                mOut.close();
                mBufReader.close();
                mReader.close();
                mIn.close();

                // 关闭socket
                mSocket.close();
            } catch (IOException e) {
                mContext.DisplayMessage(tag + "：服务器端异常！");
                e.printStackTrace();
            }
        }

        private String receive(String tag) {
            String s = null;

            try {
                while (!mExit && (s == null)) {
                    s = mBufReader.readLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            mContext.DisplayMessage(tag + "收到：" + s);

            return s;
        }

        private void send(String tag, String cmd) {
            mContext.DisplayMessage(tag + "发送：" + cmd);
            mWriter.println(cmd);
            mWriter.flush();
        }

        private boolean getPressVal(String tag) {
            // 启动气压计读数据
            PressData data = PressData.getInstance();
            mContext.sendMessage(mContext.GET_PRESSURE_VAL, data);

            //等待数据
            int val = -1;
            synchronized (data) {
                try {
                    data.wait(15000);
                    val = data.average();
                    mContext.DisplayMessage("可读取标准值：" + val);
                } catch (InterruptedException e) {
                }
            }
            PressData.freeData(data);
            if (val < 0) {
                return false;
            }

            //发送标准值给客户端
            String rsp = CMD_GET_DATA + ":" + val;
            send(tag, rsp);

            return true;
        }
    }
}
