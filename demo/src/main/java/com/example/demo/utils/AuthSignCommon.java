package com.example.demo.utils;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/**
 * Created by XJF on 17-6-27.
 */

public class AuthSignCommon {
    private static final String TAG = "AuthSignCommon";
    private static final String encryptedPath = "/tmp/hauthstore";
    private static final String tmpPath = "/tmp/authsign.tmp";
    private static final String DES_PWD = "2017063020170630201706302017063020170630";
    private static final String root_ca = "/etc/liandong_root.crt";

    private static final int MAX_DECRYPT_BLOCK = 128;

    static {
        try {
            System.loadLibrary("authsignapkcommonjni");
        } catch (Error var1) {
            var1.printStackTrace();
        }

    }

    public AuthSignCommon() {
    }

    public void encryptCrt(String crtPath){
        writeFile(encryptedPath, encrypt(readFile(crtPath), DES_PWD));
    }

    private native int authSignApknative(String var1, String var2);

    public boolean authSignApk(String strSignFilePath) throws Exception {
        //writeFile(tmpPath, decrypt(readFile(encryptedPath), DES_PWD));
        int ret = this.authSignApknative(strSignFilePath, root_ca);
        //File file = new File(tmpPath);
        //if(file.exists()) file.delete();
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

    /**
     * 加密
     * @param datasource byte[]
     * @param password String
     * @return byte[]
     */
    public static byte[] encrypt(byte[] datasource, String password) {
        try{
            SecureRandom random = new SecureRandom();
            DESKeySpec desKey = new DESKeySpec(password.getBytes());
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(desKey);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, securekey, random);
            return cipher.doFinal(datasource);
        }catch(Throwable e){
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 解密
     * @param src byte[]
     * @param password String
     * @return byte[]
     * @throws Exception
     */
    public static byte[] decrypt(byte[] src, String password) throws Exception {
        SecureRandom random = new SecureRandom();
        DESKeySpec desKey = new DESKeySpec(password.getBytes());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey securekey = keyFactory.generateSecret(desKey);
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.DECRYPT_MODE, securekey, random);
        return cipher.doFinal(src);
    }
}
