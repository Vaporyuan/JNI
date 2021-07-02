package com.example.demo.utils;

import android.util.Log;

import org.bouncycastle.util.encoders.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;


public class AuthSign {
    private static final String TAG = "AuthSign";
    private static final String caCertPath = "/etc/uauthstore";
    private static final String tmpPath = "/tmp/authsign.tmp";
    private static final String PUBLICK_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCKpEAPIxWZ720kpez1xUsBnsMXXr01Q4omTAbnZqnrSVTXRZ5YMqyhVfqtIaI6vC8YTd+hibhTiQz+s6NDfQBngGba/OOJ5EWcjLGCx4Y7KqZ0uUQVE74TZ5qaV8xaNRGvejEvvKG8CR0ZjYSnxjLDKQsPufoxrwkNuspOFRcPWQIDAQAB";

    private static final int MAX_DECRYPT_BLOCK = 128;

    static {
        try {
            System.loadLibrary("authsignapkjni");
        } catch (Error var1) {
            var1.printStackTrace();
        }

    }

    public AuthSign() {
    }

    private native int authSignApknative(String var1, String var2);

    public boolean authSignApk(String strSignFilePath) throws Exception {
        writeFile(tmpPath, decryptByPublicKey(readFile(caCertPath), PUBLICK_KEY));
        int ret = this.authSignApknative(strSignFilePath, tmpPath);
        File file = new File(tmpPath);
        if(file.exists()) file.delete();
        switch(ret) {
            case 0:
                Log.i(TAG,"Executes successfully!");
                break;
            case 1:
            default:
                throw new Exception("Executes failed!");
            case 2:
                throw new Exception("Parameters error!");
            case 3:
                throw new Exception("Open file failed!");
            case 4:
                throw new Exception("Allocate memory failed!");
            case 5:
                throw new Exception("Failed to read file data");
        }

        return true;
    }

    private static byte[] readFile(String filename){
        try {
            FileInputStream fis = new FileInputStream(filename);
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int num;
            while((num = fis.read(buffer)) != -1){
                bos.write(buffer, 0, num);
            }
            byte[] result = bos.toByteArray();
            fis.close();
            bos.close();
            return result;
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void writeFile(String fileName, byte[] data) {
        try {
            File file = new File(fileName);
            if (!file.exists())
                file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] decryptByPublicKey(byte[] encryptedData, String publicKey)
            throws Exception {
        byte[] keyBytes = Base64.decode(publicKey);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Key publicK = keyFactory.generatePublic(x509KeySpec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, publicK);
        int inputLen = encryptedData.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }
}
