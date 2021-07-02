package com.example.demo.utils;

public class CertMaster {
    static  
    {  
        System.loadLibrary("verifycert_jni");  
    }
    public native static int readCert(int type, int index, int coding,byte[] key);
    public native static int writeCert(int type, int index, int coding,byte[] key, int len);
    public native static int verifyCertSign(byte[] msgDigest, int msglen, byte[] sign, int signlen);
    public native static int open();
    public native static int close();
}
