package com.example.demo;

import android.annotation.SuppressLint;
import android.util.Base64;
import android.util.Log;

import com.example.demo.utils.ApkUtils;
import com.example.demo.utils.GmSSLAlgorithm;
import com.example.demo.utils.Pair;
import com.example.demo.utils.ReadApkSignData;

import org.bouncycastle.jcajce.provider.asymmetric.rsa.RSAUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import static com.example.demo.utils.ZipUtils.isZip64EndOfCentralDirectoryLocatorPresent;

public class VerityApkFile {
    private static final String TAG = "VerityApkFile";
    private static final boolean DEBUG = true;
    private static final int ZIP64_EOCD_LOCATOR_SIG_REVERSE_BYTE_ORDER = 0x504b0607;
    private static final int ZIP_EOCD_CENTRAL_DIR_OFFSET_FIELD_OFFSET = 16;
    @SuppressLint("SdCardPath")
    //private static String rootCertPath = "/sdcard/orbbec-ca.crt";
    private static String rootCertPath = "/sdcard/urovo-v3.crt";
    private final String SGN_NAME = "META-INF/APKSIGNV1.SGN";
    private byte[] certBody; //the main body of sign information
    private byte[] headerSignature; // the sign data (R and S)
    private byte[] digitCertPem; // the public key that got from digit cert
    private byte[] digitCert; // the digit cert
    private X509Certificate structureCert;
    private String apkPath = null; //"/sdcard/iluncher_signedV3.apk";
    private PermsFile permsFile;

    private byte[] sgnDataAA;
    private void logI(String msg) {
        if (DEBUG) Log.i(TAG, msg);
    }

    public VerityApkFile() {
    }

    public VerityApkFile(String apkPath) {
        this.apkPath = apkPath;
        logI("VerityApkFile apkPath = " + apkPath);
    }

    /**
     * 验证证书
     *
     * @return
     */
    public boolean verifyApkSign() {
        if (!verifyCiticStructKey()) {
            logI("Verify structure certificate false");
            return false;
        }
        return verityRS();
        /*if (!verifyStructKey()) {
            logI("Verify structure certificate false");
            return false;
        }
        return verifyHash();*/
    }

    /**
     * ASCII码字符串转数字字符串
     *
     * @param
     * @return 字符串
     */
    public static String AsciiStringToString(String content) {
        String result = "";
        int length = content.length() / 2;
        for (int i = 0; i < length; i++) {
            String c = content.substring(i * 2, i * 2 + 2);
            int a = hexStringToAlgorism(c);
            char b = (char) a;
            String d = String.valueOf(b);
            result += d;
        }
        return result;
    }

    /**
     * 十六进制字符串装十进制
     *
     * @param hex 十六进制字符串
     * @return 十进制数值
     */
    public static int hexStringToAlgorism(String hex) {
        hex = hex.toUpperCase();
        int max = hex.length();
        int result = 0;
        for (int i = max; i > 0; i--) {
            char c = hex.charAt(i - 1);
            int algorism = 0;
            if (c >= '0' && c <= '9') {
                algorism = c - '0';
            } else {
                algorism = c - 55;
            }
            result += Math.pow(16, max - i) * algorism;
        }
        return result;
    }
    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

    /**
     * Sets the offset of the start of the ZIP Central Directory in the archive.
     *
     * <p>NOTE: Byte order of {@code zipEndOfCentralDirectory} must be little-endian.
     */
    public static void setZipEocdCentralDirectoryOffset(
            ByteBuffer zipEndOfCentralDirectory, long offset) {
        assertByteOrderLittleEndian(zipEndOfCentralDirectory);
        setUnsignedInt32(
                zipEndOfCentralDirectory,
                zipEndOfCentralDirectory.position() + ZIP_EOCD_CENTRAL_DIR_OFFSET_FIELD_OFFSET,
                offset);
    }
    private static void setUnsignedInt32(ByteBuffer buffer, int offset, long value) {
        if ((value < 0) || (value > 0xffffffffL)) {
            throw new IllegalArgumentException("uint32 value of out range: " + value);
        }
        buffer.putInt(buffer.position() + offset, (int) value);
    }

    private static void assertByteOrderLittleEndian(ByteBuffer buffer) {
        if (buffer.order() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalArgumentException("ByteBuffer byte order must be little endian");
        }
    }
    /**
     * 验证hash
     * @return
     */
    private boolean verityRS() {
        RandomAccessFile apk = null;
        FileOutputStream out = null;
        try {
            apk = new RandomAccessFile(apkPath, "r");
            logI("apk length------->" + apk.length());
            // get the offset of the "End of Central Directory" and the "End of Central Directory" data(ByteBuffer)
            com.example.demo.utils.Pair<ByteBuffer, Long> eocdAndOffsetInFile = ApkSignatureSchemeV2VerifierEx.getEocd(apk);
            ByteBuffer eocd = eocdAndOffsetInFile.getFirst(); // the data of the "End of Central Directory"
            //logI("eocd position------->" + eocd.position());
            long eocdOffset = eocdAndOffsetInFile.getSecond(); // the offset of the "End of Central Directory"
            long eocdLen = apk.length() - eocdOffset; // the length of the "End of Central Directory"
            if (isZip64EndOfCentralDirectoryLocatorPresent(apk, eocdOffset)) {
                throw new ApkSignatureSchemeV2VerifierEx.SignatureNotFoundException("ZIP64 APK not supported");
            }

            // Find the APK Signing Block. The block immediately precedes the Central Directory.
            // get the offset,length and data of the "Central Directory"
            long centralDirOffset = ApkSignatureSchemeV2VerifierEx.getCentralDirOffset(eocd, eocdOffset); // the offset of "Central Directory"
            long cdLen = eocdOffset - centralDirOffset; // the length of "Central Directory"
            ByteBuffer cdData = ByteBuffer.allocate((int) cdLen); // the data of "Central Directory"
            cdData.order(ByteOrder.LITTLE_ENDIAN);
            apk.seek(centralDirOffset);
            apk.readFully(cdData.array(), cdData.arrayOffset(), cdData.capacity());
            //logI("cdData position------->" + cdData.position());

            // get the data,length and offset(the length of "Contents of ZIP entries") of "APK Signing Block"
            com.example.demo.utils.Pair<ByteBuffer, Long> apkSigningBlockAndOffsetInFile = ApkSignatureSchemeV2VerifierEx.findApkSigningBlock(apk, centralDirOffset);
            ByteBuffer apkSigningBlock = apkSigningBlockAndOffsetInFile.getFirst(); // the data of "APK Signing Block"
            //logI("apkSigningBlock position------->" + apkSigningBlock.position());
            long cozeLen = apkSigningBlockAndOffsetInFile.getSecond(); // the offset(the length of "Contents of ZIP entries") of "APK Signing Block"
            long asbLen = apkSigningBlock.capacity(); // the length of "APK Signing Block"

            // To know whether the origin apk contains the "APK Signing Block" or not
            boolean hasV2Block = ApkSignatureSchemeV2VerifierEx.hasApkSignatureSchemeV2Block(apkSigningBlock);

            // get the data,length and offset of "SGN data"
            Pair<ByteBuffer, Long> sgnDataAndOffset = ApkSignatureSchemeV2VerifierEx.findCiticApkSignatureSchemeV2Block(apkSigningBlock);
            ByteBuffer sgnData = sgnDataAndOffset.getFirst(); // the data of "SGN data"
            long sgnBlockOffset = sgnDataAndOffset.getSecond(); // the offset of "SGN data"
            long sgnLength = sgnData.getLong() + 8; // the length of "SGN data"

            // get the data and length of "SGN data main body"
            ByteBuffer sgnMainData = ByteBuffer.wrap(certBody); // the data of "SGN data main body"
            long sgnMainDataLength = certBody.length; // the length of "SGN data main body"

            // delete the ID-value from the "APK Signing Block" and change the "Block Len" to the org value after deleting
            long realAsbLen = asbLen - sgnLength; // the org length of the "APK Signing Block" after deleting the ID-value
            apkSigningBlock.position(0);
            apkSigningBlock.putLong(realAsbLen - 8 /*the "Block Len shouldn't contains the size of itself"*/); // change the first "Block Len"

            byte[] magic = new byte[16]; // the magic of the "APK Signing Block"
            apkSigningBlock.position(apkSigningBlock.capacity() - 16);
            apkSigningBlock.get(magic, 0, 16);
            apkSigningBlock.position((int) sgnBlockOffset); // replace the ID-value data(means deleting the ID-value data from the "APK Signing Block")
            apkSigningBlock.putLong(realAsbLen - 8 /*the "Block Len shouldn't contains the size of itself"*/); // change the second "Block Len"
            apkSigningBlock.put(magic, 0, 16);

            if (!hasV2Block) { // the origin apk without v2 block,so we should delete the whole "APK Signing Block"
                realAsbLen = 0;
            }
            ReadApkSignData readApkSignData = new ReadApkSignData(cozeLen, realAsbLen, cdLen, eocdLen, sgnMainDataLength, apk,
                    apkSigningBlock, cdData, eocd, sgnMainData);

            // change the CentralDirectoryOffset that saved in "End of Central Directory"
            setZipEocdCentralDirectoryOffset(eocd, hasV2Block ? (centralDirOffset - sgnLength) : (centralDirOffset - asbLen));

            byte[] beforeApkSigBlockDir = new byte[(int)cozeLen];
            apk.read(beforeApkSigBlockDir, 0,(int)cozeLen);
            ByteBuffer coz = ApkUtils.getByteBuffer(ByteBuffer.wrap(beforeApkSigBlockDir), (int) cozeLen);
            //logI("coz cozeLen------->" + (int) cozeLen);
            //logI("coz position------->" + coz.position());

            ByteBuffer apkSigningBlockAfter = ApkUtils.getByteBuffer(apkSigningBlock, (int) realAsbLen);
            //需要使用remaining重新刷新
            //1.beforeApkSigBlockDir
            coz.position(coz.remaining());
            //2.apkSigBlock
            if(hasV2Block) apkSigningBlockAfter.position(apkSigningBlockAfter.remaining());
            //3.centralDir
            cdData.position(cdData.remaining());
            //4.eocd
            eocd.position(eocd.remaining());

            ByteBuffer[] contents;
            if(hasV2Block){
                contents = new ByteBuffer[]{coz, apkSigningBlockAfter, cdData, eocd, ByteBuffer.wrap(certBody)};
            }else{
                contents = new ByteBuffer[]{coz, cdData, eocd, ByteBuffer.wrap(certBody)};
            }
            //摘要总集合
            byte[] certDigests = ApkUtils.computeContentDigests(ApkUtils.SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256, 0, contents);
            logI("certDigests================" + ApkUtils.bytesToHexString(certDigests));
            java.security.Signature signa = java.security.Signature.getInstance("SHA256WithRSA");
            signa.initVerify(structureCert.getPublicKey());
            signa.update(certDigests);
            boolean result = signa.verify(headerSignature);
            logI("Verify apk signature result = " + result);
            return result;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            logI("Verify apk signature error!");
            e.printStackTrace();
        } finally {
            try {
                if (apk != null) {
                    apk.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean verifyCiticStructKey(){
        try {
            logI("hasApkSignatureSchemeV2Block>>>>> " + ApkSignatureSchemeV2VerifierEx.hasV2Block(apkPath));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ApkSignatureSchemeV2VerifierEx.SignatureNotFoundException e) {
            e.printStackTrace();
        }

        InputStream is = null;
        try {
            // get the SGN data
            is = ApkSignatureSchemeV2VerifierEx.getV3SgnData(apkPath);
            //is = new ByteArrayInputStream(toByteArray(is));
            //is = new ByteArrayInputStream(MainActivity.signatureInfoSubject().toByteArray());
            //logI("string verifyCiticStructKey= " + readInfoStream(is));
            ApkUtils.DerFile derFile = new ApkUtils.DerFile(is);
            ApkUtils.DerObject objName = derFile.getNextDerObject();
            if (!"ACQUIRER-SGN-INFO".equals(new String(objName.mainBody))) {
                logI("string ACQUIRER-SGN-INFO can not found in SGN.");
                return false;
            }
            ApkUtils.DerObject objHeader = derFile.getNextDerObject();
            logI("objHeader.sonObjs.size()----------->" + objHeader.sonObjs.size());
            if (objHeader.sonObjs.size() >= 3) {
                ApkUtils.DerObject objInfo = objHeader.sonObjs.get(0);
                //logI("objInfo.算法00 ================ " + ApkUtils.bytesToHexString(objInfo.sonObjs.get(0).allBody));
                logI("objInfo.length ================ " + objInfo.sonObjs.size());
                logI("objInfo.算法 ================ " + ApkUtils.bytesToHexString(objInfo.sonObjs.get(0).mainBody));
                //logI("objInfo.证书ID00 ================ " + ApkUtils.bytesToHexString(objInfo.sonObjs.get(1).allBody));
                logI("objInfo.证书ID ================ " + ApkUtils.bytesToHexString(objInfo.sonObjs.get(1).mainBody));
                logI("objInfo.数字签名算法 ================ " + ApkUtils.bytesToHexString(objInfo.sonObjs.get(2).mainBody));
                ApkUtils.DerObject signDate = objInfo.sonObjs.get(3).sonObjs.get(0);
                logI("signDate ================" + ApkUtils.bytesToHexString(signDate.mainBody));
                logI("signDate================" + ApkUtils.AsciiStringToString(ApkUtils.bytesToHexString(signDate.mainBody)));
                //logI("objInfo.Hash00 ================ " + ApkUtils.bytesToHexString(objInfo.sonObjs.get(4).allBody));
                logI("objInfo.Hash ================ " + ApkUtils.bytesToHexString(objInfo.sonObjs.get(4).mainBody));
                ApkUtils.DerObject exInfo = objInfo.sonObjs.get(5);
                ApkUtils.DerObject permInfo = exInfo.sonObjs.get(0).sonObjs.get(0).sonObjs.get(1);
                //logI("permInfo================" + ApkUtils.bytesToHexString(permInfo.mainBody));
                logI("permInfo================" + ApkUtils.AsciiStringToString(ApkUtils.bytesToHexString(permInfo.mainBody)));
                permsFile = new PermsFile(permInfo.mainBody);


                ApkUtils.DerObject objSign = objHeader.sonObjs.get(1);
                //logI("objSign00================" + ApkUtils.bytesToHexString(objSign.allBody));
                logI("objSign================" + ApkUtils.bytesToHexString(objSign.mainBody));
                //logI("objSign00================" + ApkUtils.bytesToHexString(objSign.sonObjs.get(1).sonObjs.get(0).allBody));
                //logI("objSign11================" + ApkUtils.bytesToHexString(objSign.sonObjs.get(1).sonObjs.get(0).mainBody));
                //logI("objSign length================" + ApkUtils.bytesToHexString(objSign.sonObjs.get(1).sonObjs.get(0).mainBody).length());
                logI("==========objSign.length======== " + objSign.sonObjs.size());
                //headerSignature = objSign.sonObjs.get(1).sonObjs.get(0).mainBody;//签名数据RS
                //headerSignature = objSign.mainBody;//签名数据RS
                headerSignature = new byte[objSign.mainBody.length - 1];
                System.arraycopy(objSign.mainBody, 1, headerSignature, 0, objSign.mainBody.length - 1);
                logI("==========headerSignature======== " + Arrays.toString(headerSignature));
                //logI("==========headerSignature bytesToHexString======== " + ApkUtils.bytesToHexString(headerSignature));

                ApkUtils.DerObject objCert = objHeader.sonObjs.get(2);
                //logI("objCert================" + ApkUtils.bytesToHexString(objCert.mainBody));
                /*byte[] cert0 =  new byte[objCert.mainBody.length -1]; //需要减去最前面的00
                System.arraycopy(objCert.mainBody, 1, cert0, 0, cert0.length);
                getCertificateKey(cert0);*/
                // get the data of "SGN data main body"
                certBody = objInfo.allBody;
                logI("certBody================" + ApkUtils.bytesToHexString(certBody));

                // get the data of digit cert
                digitCert = new byte[objCert.mainBody.length - 1];
                System.arraycopy(objCert.mainBody, 1, digitCert, 0, objCert.mainBody.length - 1);
                //logI("digitCert==================" + Arrays.toString(digitCert));
                //logI("digitCert==================" + ApkUtils.bytesToHexString(digitCert));
                //getCertificateKey(digitCert);

                //获取证书
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                ByteArrayInputStream bis = new ByteArrayInputStream(objCert.mainBody, 1, objCert.mainBody.length - 1);
                structureCert = (X509Certificate) certificateFactory.generateCertificate(bis);
                bis.close();
                //structureCert.checkValidity();//判断证书是否过期

                // get the root cert
                CertificateFactory certificateFactory1 = CertificateFactory.getInstance("X.509" ,"BC");
                Certificate caCer = certificateFactory1.generateCertificate(new FileInputStream(rootCertPath));
                PublicKey caKey = caCer.getPublicKey();
                structureCert.verify(caKey);
                logI("caKey verify digitCert successful!!!!!");
                return true;




                /*// get the sign data 30 44 02 20 R 02 20 S
                // if the first bit of the first byte of R is 1,just like 0x1*** ****,the data should be 30 45 02 21 00 R 02 20 S
                // if the first bit of the first byte of S is 1,just like 0x1*** ****,the data should be 30 45 02 20 R 02 21 00 S
                // if both of the first bit of the first byte of R and S are 1,just like 0x1*** ****,the data should be 30 46 02 21 00 R 02 21 00 S
                // if niether of the first bit of the first byte of R or S is 1,just like 0x0*** ****,the data should be 30 44 02 20 R 02 20 S
                byte[] R = new byte[32];
                byte[] S = new byte[32];
                System.arraycopy(objSign.mainBody, 5, R, 0, 32);
                System.arraycopy(objSign.mainBody, 39, S, 0, 32);

                if ((R[0] & 1 << 8) != 0 && (S[0] & 1 << 8) != 0) {
                    headerSignature = new byte[objSign.mainBody.length + 1];
                    System.arraycopy(objSign.mainBody, 1, headerSignature, 0, 2);
                    headerSignature[1] = (byte) (headerSignature[1] + 2);
                    headerSignature[2] = 0x02;
                    headerSignature[3] = 0x21;
                    headerSignature[4] = 0x00;
                    System.arraycopy(R, 0, headerSignature, 5, R.length);
                    headerSignature[37] = 0x02;
                    headerSignature[38] = 0x21;
                    headerSignature[39] = 0x00;
                    System.arraycopy(S, 0, headerSignature, 40, S.length);
                } else if ((R[0] & 1 << 8) != 0 && (S[0] & 1 << 8) == 0) {
                    headerSignature = new byte[objSign.mainBody.length];
                    System.arraycopy(objSign.mainBody, 1, headerSignature, 0, 2);
                    headerSignature[1] = (byte) (headerSignature[1] + 1);
                    headerSignature[2] = 0x02;
                    headerSignature[3] = 0x21;
                    headerSignature[4] = 0x00;
                    System.arraycopy(R, 0, headerSignature, 5, R.length);
                    System.arraycopy(objSign.mainBody, 37, headerSignature, 37, 34);
                } else if ((R[0] & 1 << 8) == 0 && (S[0] & 1 << 8) != 0) {
                    headerSignature = new byte[objSign.mainBody.length];
                    System.arraycopy(objSign.mainBody, 1, headerSignature, 0, 2);
                    headerSignature[1] = (byte) (headerSignature[1] + 1);
                    System.arraycopy(objSign.mainBody, 3, headerSignature, 2, 34);
                    headerSignature[36] = 0x02;
                    headerSignature[37] = 0x21;
                    headerSignature[38] = 0x00;
                    System.arraycopy(S, 0, headerSignature, 39, S.length);
                } else {
                    headerSignature = new byte[objSign.mainBody.length - 1];
                    System.arraycopy(objSign.mainBody, 1, headerSignature, 0, objSign.mainBody.length - 1);
                }

                logI("headerSignature===============" + ApkUtils.bytesToHexString(headerSignature));

                // get the public key of the digit cert
                digitCertPem = GmSSLAlgorithm.X509_getPubkey(digitCert);

                // get the public key of the root cert(CITIC_CA.cer)
                byte[] citicCaCertPem = GmSSLAlgorithm.X509_getPubkey(ApkUtils.fileConvertToByteArray(new File(rootCertPath)));

                // use the citicCaCertPem to verify the digitCert
                if (GmSSLAlgorithm.X509_VerifyByPubkey(digitCert,citicCaCertPem) == 1) {
                    logI("citicCaCertPem verify digitCert successful!!!!!");
                    return true;
                }*/
            }

        } catch (Exception e) {
            logI("Verify structure certificate error!");
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    public String getCertificateKey(byte[] publicKeys) {
        CertificateFactory cf = null;
        PublicKey publicKey = null;
        try {
            cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(publicKeys));
            publicKey = cert.getPublicKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] publicKeyString = Base64.encode(publicKey.getEncoded(), Base64.DEFAULT);
        String publickey = new String(publicKeyString);
        logI("-----------------公钥--------------------");
        logI(publickey);
        logI("-----------------公钥--------------------");
        /*System.out.println("-----------------公钥--------------------");
        System.out.println(publickey);
        System.out.println("-----------------公钥--------------------");*/
        return publickey;
    }


    private static final String DEFAULT_ENCODING = "utf-8";//"GBK";//编码
    private static final int PROTECTED_LENGTH = 51200;// 输入流保护 50KB

    public String readInfoStream(InputStream input) throws Exception {
        if (input == null) {
            throw new Exception("输入流为null");
        }
        //字节数组
        byte[] bcache = new byte[2048];
        int readSize = 0;//每次读取的字节长度
        int totalSize = 0;//总字节长度
        ByteArrayOutputStream infoStream = new ByteArrayOutputStream();
        try {
            //一次性读取2048字节
            while ((readSize = input.read(bcache)) > 0) {
                totalSize += readSize;
                if (totalSize > PROTECTED_LENGTH) {
                    throw new Exception("输入流超出50K大小限制");
                }
                //将bcache中读取的input数据写入infoStream
                infoStream.write(bcache, 0, readSize);
            }
        } catch (IOException e1) {
            throw new Exception("输入流读取异常");
        } finally {
            try {
                //输入流关闭
                input.close();
            } catch (IOException e) {
                throw new Exception("输入流关闭异常");
            }
        }

        try {
            return infoStream.toString(DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new Exception("输出异常");
        }
    }

    /**********************************************************
     * 权限文件相关
     **********************************************************/
    private static class PermsFile {
        private final HashSet<String> permissions;
        private final HashSet<String> defPermissions;

        public PermsFile(byte[] perms) {
            String permStr = new String(perms);
            //Log.i(TAG, "PermsFile Perms:" + permStr);
            Pattern pattern = Pattern.compile("\\[Uses\\-permission\\-\\d+\\][\r\n]*Name\\=([\\w\\.]*)[\r\n]*");
            Matcher matcher = pattern.matcher(permStr);
            permissions = new HashSet<String>();
            while (matcher.find()) {
                if (matcher.group().length() > 1)
                    permissions.add(matcher.group(1));
            }
		   /*[Permission-11]
           Domain-1=Manager
           Name=com.icbc.smartpos.bankpay.trans.preauth*/
            pattern = Pattern.compile("\\[Permission\\-\\d+\\][\r\n]*Domain-1\\=Manager[\r\n]*Name\\=([\\w\\.]*)[\r\n]*");
            matcher = pattern.matcher(permStr);
            defPermissions = new HashSet<String>();
            while (matcher.find()) {
                if (matcher.group().length() > 1)
                    defPermissions.add(matcher.group(1));
            }
        }

        public HashSet<String> getPermissions() {
            return permissions;
        }

        public HashSet<String> getDefPermissions() {
            return defPermissions;
        }
    }

    public HashSet<String> getPerms() {
        if (permsFile != null) return permsFile.getPermissions();
        return null;
    }

    /**
     * get the permissions that defined in SGN Data
     */
    public HashSet<String> getDefPerms() {
        if (permsFile != null) return permsFile.getDefPermissions();
        return null;
    }
}
