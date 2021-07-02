package com.example.demo.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.zip.ZipException;

import javax.crypto.Cipher;

/**
 * Created by Josin on 16-5-10.
 */
public class AllinPayZipFile {

    public static String rootCertPath = "/etc/PubKey_ROOT_20151205.pem";

    public static final String STR_HDRMRK = "FILE_SIGN_MARK";
    public static final String STR_EXEFLG = "ALLINPAY_SIGN_INFO";

    private static final int TYPE_STRING = 1;
    private static final int TYPE_STRUCTURE = 2;
    private static final int TYPE_INT = 3;

    /**
     *          结构说明            长度             描述
     * CRTINF:  签名信息           256字节
     * CRTLEN:  工作证书长度        4字节
     *          工作证书数据                        用于完成验签的工作证书
     * HDRMRK:  文件头描述符       01 0E ...        01表示字符串，0E长度11,内容为FILE_SIGN_MARK
     * HDRLEN:  头结构长度         03 04 ...        03表示整型，04长度
     * SRCLEN:  源文件长度         03 04 ...        原始 APK 的数据长度
     * EXESTR:  通联签名扩展结构    02 20 ...
     * EXEFLG:  通联签名扩展标识    01 11 ...        ALLINPAY_SIGN_INFO
     * SGNFFS:  签名信息偏移        03 04 ...       签名数据的偏移位置,从文件起始位置计算
     * SGNSIZ:  签名信息大小        03 04 ...
     */
    public static final int SGNSIZ = 6, SGNFFS = 6, EXEFLG = 20, EXESTR = 2, SRCLEN = 6,
            HDRLEN = 6, HDRMRK = 16, CRTLEN = 4, CRTINF = 256;

    private final RandomAccessFile raf;
    private long offsetExeFlg;
    private long offsetHdrMrk;

    public AllinPayZipFile(String apkPath) throws IOException {
        raf = new RandomAccessFile(apkPath, "r");

        long infoOffset = raf.length() - SGNSIZ - SGNFFS - EXEFLG;
        long markOffset = raf.length() - SGNSIZ - SGNFFS - EXEFLG - EXESTR - SRCLEN - HDRLEN - HDRMRK;
        offsetExeFlg = flagSearch(raf, infoOffset, STR_EXEFLG);
        offsetHdrMrk = flagSearch(raf, markOffset, STR_HDRMRK);
    }

    private long flagSearch(RandomAccessFile raf, long offset, String flag) throws IOException {
        if (offset < 0)
            throw new ZipException("File too short to get data at offset " + offset);
        long stopOffset = offset - 65536;
        if (stopOffset < 0) {
            stopOffset = 0;
        }
        int type, size;
        while (true) {
            raf.seek(offset);
            type = raf.readByte() & 0xff;
            size = raf.readByte() & 0xff;

            if (type == TYPE_STRING || size == flag.length()) {
                byte[] data = new byte[size];
                raf.readFully(data);
                if (flag.equals(new String(data))) {
                    return offset;
                }
            }

            offset--;
            if (offset < stopOffset)
                throw new ZipException("Can not find flags of AllinPay!");
        }
    }

    public boolean verifyAllinPaySign() throws Exception {
        // 签名数据域的信息长度
        long scanOffset = offsetExeFlg + EXEFLG + SGNFFS;
        raf.seek(scanOffset);
        if ((raf.readByte() & 0xff) != TYPE_INT || (raf.readByte() & 0xff) != 4)
            return false;
        int sgnSize = Integer.reverseBytes(raf.readInt() & 0xffffffff);

        // 签名数据的偏移位置
        scanOffset = scanOffset - SGNFFS;
        raf.seek(scanOffset);
        if ((raf.readByte() & 0xff) != TYPE_INT || (raf.readByte() & 0xff) != 4)
            return false;
        int sgnOffset = Integer.reverseBytes(raf.readInt() & 0xffffffff);

        // 原始apk数据的长度
        scanOffset = scanOffset - EXEFLG - EXESTR - SRCLEN;
        raf.seek(scanOffset);
        if ((raf.readByte() & 0xff) != TYPE_INT || (raf.readByte() & 0xff) != 4)
            return false;
        long apkSrcSize = Integer.reverseBytes(raf.readInt() & 0xffffffff);

        // 签名信息
        raf.seek(sgnOffset);
        byte[] signInfo = new byte[256];
        raf.readFully(signInfo);

        // 签名信息中的证书
        scanOffset = sgnOffset + 256;
        raf.seek(scanOffset);
        int certSize = Integer.reverseBytes(raf.readInt() & 0xffffffff);
        byte[] cert = new byte[certSize];
        raf.readFully(cert);

        // 获取根证书
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Certificate caCer = certificateFactory.generateCertificate(new FileInputStream(rootCertPath));
        PublicKey caKey = caCer.getPublicKey();

        Certificate allinPayWorkCert = certificateFactory.generateCertificate(new ByteArrayInputStream(cert));
        allinPayWorkCert.verify(caKey);

        // 计算 hash:通过 SHA-256 算法计算的数据 32 字节(256 位)摘要数据
        MessageDigest alg = MessageDigest.getInstance("SHA-256");
        scanOffset = 0;
        raf.seek(scanOffset);
        byte[] apkSrc = new byte[1024];
        while (scanOffset < apkSrcSize) {
            raf.readFully(apkSrc);
            scanOffset = scanOffset + 1024;
            alg.update(apkSrc, 0, scanOffset < sgnOffset ? 1024 : (int) (sgnOffset + 1024 - scanOffset));
        }
        raf.close();
        byte[] message = alg.digest();
        PublicKey workKey = allinPayWorkCert.getPublicKey();

        return verifyDecrypt(message, signInfo, workKey);
    }

    public Certificate getCertificate() throws Exception{

        // 签名数据的偏移位置
        long scanOffset = offsetExeFlg + EXEFLG + SGNFFS - SGNFFS;
        raf.seek(scanOffset);
        if ((raf.readByte() & 0xff) != TYPE_INT || (raf.readByte() & 0xff) != 4)
            return null;
        int sgnOffset = Integer.reverseBytes(raf.readInt() & 0xffffffff);

        // 签名信息中的证书
        scanOffset = sgnOffset + 256;
        raf.seek(scanOffset);
        int certSize = Integer.reverseBytes(raf.readInt() & 0xffffffff);
        byte[] cert = new byte[certSize];
        raf.readFully(cert);

        // 获取根证书
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

        Certificate allinPayWorkCert = certificateFactory.generateCertificate(new ByteArrayInputStream(cert));
        raf.close();
        return allinPayWorkCert;
    }

    /**
     * 验证加密解密
     *
     * @param message     消息摘要
     * @param encryptData 签名信息（此处只进行了加密）
     * @param publicKey   收单机构工作证书
     * @return
     * @throws Exception
     */
    private boolean verifyDecrypt(byte[] message, byte[] encryptData, PublicKey publicKey) throws Exception {
        byte[] decry = decryptByPublicKey(encryptData, publicKey.getEncoded());
        byte[] areaInDecry = new byte[message.length];
        System.arraycopy(decry, decry.length - areaInDecry.length, areaInDecry, 0, areaInDecry.length);
        return Arrays.equals(areaInDecry, message);
    }

    /**
     * 验证签名，文档中要求对填充数据签名，实际apk中只进行了加密
     *
     * @param message   消息摘要
     * @param signed    签名信息
     * @param publicKey 收单机构工作证书
     * @return
     * @throws Exception
     */
    private boolean verifySign(byte[] message, byte[] signed, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        // 填充签名数据(PKCS#1 标准):用于签名的数据为 256 字节,分为三段:前两
        // 个字节填充为 0x00,0x01;中间 222 字节填充为 0xFF;最后 32 字节填充为第
        // 一步生成的 32 字节哈希值
        byte[] msgToSign = new byte[256];
        msgToSign[0] = (byte) 0x00;
        msgToSign[1] = (byte) 0x01;
        for (int i = 2; i < 224; i++) {
            msgToSign[i] = (byte) 0xff;
        }
        System.arraycopy(message, 0, msgToSign, 224, message.length);

        signature.update(msgToSign);
        return signature.verify(signed);
    }

    private byte[] decryptByPublicKey(byte[] encryptedData, byte[] publicKey)
            throws Exception {
        int MAX_DECRYPT_BLOCK = 256;
        byte[] keyBytes = publicKey;
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Key publicK = keyFactory.generatePublic(x509KeySpec);
        Cipher cipher = Cipher.getInstance("RSA");
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
