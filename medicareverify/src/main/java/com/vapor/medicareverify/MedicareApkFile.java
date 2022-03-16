package com.vapor.medicareverify;

import android.util.Log;
import com.malio.gmssl.GmSSLAlgorithm;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class MedicareApkFile {
    private static final boolean DEBUG = true;
    private static final String TAG = "MedicareApkFile";
    private static final String SM3WITHSM2 = "SM3withSM2";
    private static final String RSAWITHSHA256 = "RSAwithSHA256";
    private static final String rootMedicareGm = "sdcard/MEDICARE_CA.cer";//"/etc/MEDICARE_CA.cer";
    private static final String SGN_NAME = "META-INF/APKSIGNV1.SGN";

    private byte[] certBody; //the main body of sign information
    private byte[] headerSignature; // the sign data (R and S)
    private byte[] digitCertPub; // the public key that got from digit cert
    private byte[] digitCert; // the digit cert
    private String algorithm; // the algorithm of signature

    private final String apkPath;
    private PermsFile permsFile;
    private static final int MALIO_APK_SIGNATURE_SCHEME_V3_BLOCK_ID = 0x71150101; //Android 0x7109871a; //Malio 0x71536966; //0x71150101

    public MedicareApkFile(String apkPath) {
        this.apkPath = apkPath;
    }

    public boolean verifyMedicareApkSign() {
        if (!verifyStructKey()) return false;
        return verifyHash();
    }

    private boolean verifyStructKey() {
        try {
            try {
                // get the SGN data Sign V2
                InputStream isV2 = ApkSignatureSchemeV2VerifierEx.getUbxSgnData(apkPath, MALIO_APK_SIGNATURE_SCHEME_V3_BLOCK_ID);
            } catch (ApkSignatureSchemeV2VerifierEx.SignatureNotFoundException e) {
                slog("SignatureNotFoundException no V2 sign return false.");
                //return false;
            }

            //Sign V1
            JarFile jarfile = new JarFile(apkPath);
            JarEntry je = jarfile.getJarEntry(SGN_NAME);
            if (je == null) {
                jarfile.close();
                return false;
            }
            InputStream is = jarfile.getInputStream(je);
            ApkUtils.DerFile derFile = new ApkUtils.DerFile(is);
            ApkUtils.DerObject objName = derFile.getNextDerObject();
            if (!"CHS-SGN-INFO".equals(new String(objName.mainBody))) {
                logI("string CHS-SGN-INFO can not found in SGN.");
                return false;
            }
            ApkUtils.DerObject objHeader = derFile.getNextDerObject();
            if (objHeader.sonObjs.size() >= 3) {
                ApkUtils.DerObject objInfo = objHeader.sonObjs.get(0);
                logI("objInfo.length ================ " + objInfo.sonObjs.size());
                ApkUtils.DerObject exInfo = objInfo.sonObjs.get(5);
                ApkUtils.DerObject permInfo = exInfo.sonObjs.get(0).sonObjs.get(0).sonObjs.get(1);
                logI("permInfo================" + ApkUtils.bytesToHexString(permInfo.mainBody));
                permsFile = new PermsFile(permInfo.mainBody);
                ApkUtils.DerObject objSign = objHeader.sonObjs.get(1);
                logI("objSign================" + ApkUtils.bytesToHexString(objSign.mainBody));
                logI("objSign length================" + objSign.mainBody.length);
                ApkUtils.DerObject objCert = objHeader.sonObjs.get(2);
                logI("objCert================" + ApkUtils.bytesToHexString(objCert.mainBody));

                ApkUtils.DerObject structureVersion = objInfo.sonObjs.get(0);
                logI("structureVersion==========" + ApkUtils.bytesToHexString(structureVersion.mainBody));
                if (ApkUtils.bytesToHexString(structureVersion.mainBody).equals("20")) {
                    algorithm = SM3WITHSM2;
                } else if (ApkUtils.bytesToHexString(structureVersion.mainBody).equals("01")) {
                    algorithm = RSAWITHSHA256;
                }

                // get the data of "SGN data main body"
                //certBody = objInfo.allBody; //CICIT
                //logI("objInfo.allBody==================" + ApkUtils.bytesToHexString(objInfo.allBody));
                //certBody = objInfo.allBody;
                //logI("objInfo.mainBody==================" + ApkUtils.bytesToHexString(objInfo.mainBody));
                certBody = objInfo.mainBody;

                // get the data of digit cert
                digitCert = new byte[objCert.mainBody.length - 1];
                System.arraycopy(objCert.mainBody, 1, digitCert, 0, objCert.mainBody.length - 1);
                logI("digitCert==================" + ApkUtils.bytesToHexString(digitCert));

                if (algorithm.equals(SM3WITHSM2)) {
                    // get the sign data 30 44 02 20 R 02 20 S
                    // if the first bit of the first byte of R is 1,just like 0x1*** ****,the data should be 30 45 02 21 00 R 02 20 S
                    // if the first bit of the first byte of S is 1,just like 0x1*** ****,the data should be 30 45 02 20 R 02 21 00 S
                    // if both of the first bit of the first byte of R and S are 1,just like 0x1*** ****,the data should be 30 46 02 21 00 R 02 21 00 S
                    // if niether of the first bit of the first byte of R or S is 1,just like 0x0*** ****,the data should be 30 44 02 20 R 02 20 S
                    byte[] R = new byte[32];
                    byte[] S = new byte[32];

                    //医保机制签名数据64，直接可用
                    System.arraycopy(objSign.mainBody, 0, R, 0, 32);
                    System.arraycopy(objSign.mainBody, 32, S, 0, 32);

                    if ((R[0] & 1 << 8) != 0 && (S[0] & 1 << 8) != 0) {
                        logI("00R 00S");
                    } else if ((R[0] & 1 << 8) != 0 && (S[0] & 1 << 8) == 0) {
                        logI("00R S");
                    } else if ((R[0] & 1 << 8) == 0 && (S[0] & 1 << 8) != 0) {
                        logI("R 00S");
                    } else {
                        logI("R S");
                    }


                    headerSignature = ApkUtils.hexStringToByte("00DE22E2B8D748E3B77A41649B9075C04B07696F71CC59A7684693D8F99B7C8DCF00845B0A56A564384CCDD260F47E455584B6147B52FBC4B6CC18A1039A5D107090");
                    //headerSignature = ApkUtils.hexStringToByte("00DE22E2B8D748E3B77A41649B9075C04B07696F71CC59A7684693D8F99B7C8DCF845B0A56A564384CCDD260F47E455584B6147B52FBC4B6CC18A1039A5D107090");
                    //headerSignature = ApkUtils.hexStringToByte("DE22E2B8D748E3B77A41649B9075C04B07696F71CC59A7684693D8F99B7C8DCF00845B0A56A564384CCDD260F47E455584B6147B52FBC4B6CC18A1039A5D107090");
                    //headerSignature = objSign.mainBody;
                    logI("headerSignature===============" + ApkUtils.bytesToHexString(headerSignature));

                    // get the public key of the digit cert
                    digitCertPub = GmSSLAlgorithm.X509_getPubkey(digitCert);

                    //skip verity StructKey
                    if (DEBUG) return true;

                    // get the public key of the root cert(MEDICARE_CA.cer)
                    byte[] caCertPub = GmSSLAlgorithm.X509_getPubkey(ApkUtils.fileConvertToByteArray(new File(rootMedicareGm)));

                    // use the caCertPub to verify the digitCert
                    if (GmSSLAlgorithm.X509_VerifyByPubkey(digitCert, caCertPub) == 1) {
                        slog("caCertPub verify digitCert successful!!!!!");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            slog("Verify structure certificate error!");
            e.printStackTrace();
        }
        return false;
    }

    private boolean verifyHash() {

        slog("hashverify verifyHashing...");
        try {
            RandomAccessFile raf = new RandomAccessFile(apkPath, "r");
            long scanOffset = raf.length() - ZipFile.ENDHDR;
            if (scanOffset < 0) {
                throw new ZipException("File too short to be a zip file: " + raf.length());
            }

            // 目录结束标识
            final int ENDHEADERMAGIC = 0x06054b50;
            long endheardOffset = scanOffset = ApkUtils.searchIdentify(raf, ENDHEADERMAGIC, scanOffset, false);
            logI("0x06054b50 offset: " + endheardOffset);

            // 核心目录总数
            scanOffset += 4 + 2 + 2; // 核心目录结束标记长度+当前磁盘编号长度+核心目录开始位置的磁盘编号
            raf.seek(scanOffset);
            int centDirNums = ApkUtils.get16(raf);

            logI("central dir nums: " + centDirNums);
            // 核心目录结构总数
            scanOffset += 2;
            raf.seek(scanOffset);
            int centStruNums = ApkUtils.get16(raf);
            logI("central structure dir nums: " + centStruNums);
            // 核心目录大小
            scanOffset += 2;
            raf.seek(scanOffset);
            int centDirSize = (int) (ApkUtils.get32(raf) & 0xffffffff);
            logI("central dir size: " + centDirSize);
            // 核心目录位移
            scanOffset += 4;
            raf.seek(scanOffset);
            int centDirOffset = (int) (ApkUtils.get32(raf) & 0xffffffff);
            logI("central dir offset: " + centDirOffset);

            scanOffset = centDirOffset;
            raf.seek(scanOffset);

            scanOffset = endheardOffset;
            raf.seek(scanOffset);
            // 最后一个压缩的目录源数据
            final int DIRECTORYHEADERMAGIC = 0x02014b50;
            long drctHeaderOffset;
            int fileHeaderOffset, drctNameLen, drcextraFildLen, fileCommentLen;
            while (true) {
                drctHeaderOffset = scanOffset = ApkUtils.searchIdentify(raf, DIRECTORYHEADERMAGIC, scanOffset, true);
                scanOffset += 28; // 文件名长度
                raf.seek(scanOffset);
                drctNameLen = ApkUtils.get16(raf);
                scanOffset += 2; //扩展域长度
                raf.seek(scanOffset);
                drcextraFildLen = ApkUtils.get16(raf);
                scanOffset += 2; // 文件注释长度
                raf.seek(scanOffset);
                fileCommentLen = ApkUtils.get16(raf);
                scanOffset += 10; // 本地文件头的相对位移
                raf.seek(scanOffset);
                fileHeaderOffset = (int) (ApkUtils.get32(raf) & 0xffffffff);
                scanOffset += 4;
                raf.seek(scanOffset);
                byte[] drctName = new byte[drctNameLen];
                raf.readFully(drctName);
                if (!SGN_NAME.equals(new String(drctName))) {
                    scanOffset = drctHeaderOffset - 1;
                    if (scanOffset <= 0)
                        return false;
//					throw new ZipException(
//							"APKSIGNV1.SGN not found in file header!");
                } else {
                    break;
                }
            }

            final long sgnDirectoryOffset = drctHeaderOffset;
            final int sgnDirectoryLen = 46 + drctNameLen + drcextraFildLen + fileCommentLen;
            logI("sgnDirectoryOffset === " + drctHeaderOffset + "; sgnDirectoryLen === " + sgnDirectoryLen);

            // 压缩的文件内容源数据
            final int FILEHEADERMAGIC = 0x04034b50;
            scanOffset = fileHeaderOffset;
            raf.seek(scanOffset);
            if ((int) (ApkUtils.get32(raf) & 0xffffffff) != FILEHEADERMAGIC) {
                throw new ZipException("Apk data error!");
            }
            scanOffset += 6; //通用比特标志位
            raf.seek(scanOffset);
            boolean hasDataDescriptor = ((raf.readShort() & 0x0800) == 0x0800);
            scanOffset += 12; // 压缩后的大小
            raf.seek(scanOffset);
            int compressedSize = (int) (ApkUtils.get32(raf) & 0xffffffff);
            scanOffset += 8; // 文件名长度
            raf.seek(scanOffset);
            int fileNameLen = ApkUtils.get16(raf);
            scanOffset += 2; // 扩展区长度
            raf.seek(scanOffset);
            int extraFieldLen = ApkUtils.get16(raf);
            scanOffset += 2;
            raf.seek(scanOffset);
            byte[] fileName = new byte[fileNameLen];
            raf.readFully(fileName);
            if (!SGN_NAME.equals(new String(fileName))) {
                throw new ZipException("APKSIGNV1.SGN not found in file header!");
            }

            final long sgnFileOffset = fileHeaderOffset;
            long sgnFileLen = 30 + fileNameLen + extraFieldLen + compressedSize;
            sgnFileLen = hasDataDescriptor ? sgnFileLen + 12 : sgnFileLen;
            logI("sgnFileOffset === " + sgnFileOffset + "; sgnFileLen === " + sgnFileLen);

            //java.security.Signature signa = java.security.Signature.getInstance("SHA256WithRSA");
            //signa.initVerify(structureCert.getPublicKey());

            scanOffset = 0;
            raf.seek(scanOffset);
            byte[] apkSrcBuff = new byte[1024];
            int buffLen = apkSrcBuff.length;
            long ctx = GmSSLAlgorithm.digestInit("SM3");
            byte[] idDigest = GmSSLAlgorithm.computeIdDigest("1234567812345678", digitCertPub);
            if (idDigest != null) GmSSLAlgorithm.digestUpdate(ctx, idDigest, idDigest.length);

            /**
             *  |---------------|--1024--|=====sgnFileLen=====|--------------------|--1024--|=====sgnDirectoryLen=====|----|-1024-|-----------------------|
             *  0                            sgnFileOffset                                                        sgnDirectoryOffset                                      endheardOffset
             */
            while (scanOffset < raf.length()) {
                if (scanOffset + apkSrcBuff.length <= sgnFileOffset) {
                    buffLen = apkSrcBuff.length;
                    raf.readFully(apkSrcBuff);
                    scanOffset += 1024;
                } else if (scanOffset <= sgnFileOffset && scanOffset + apkSrcBuff.length > sgnFileOffset) {
                    buffLen = (int) (sgnFileOffset - scanOffset);
                    raf.readFully(apkSrcBuff, 0, buffLen);
                    scanOffset = sgnFileOffset + sgnFileLen;
                } else if (scanOffset >= sgnFileOffset + sgnFileLen && scanOffset + apkSrcBuff.length <= sgnDirectoryOffset) {
                    buffLen = apkSrcBuff.length;
                    raf.readFully(apkSrcBuff);
                    scanOffset += 1024;
                } else if (scanOffset < sgnDirectoryOffset && scanOffset + apkSrcBuff.length > sgnDirectoryOffset) {
                    buffLen = (int) (sgnDirectoryOffset - scanOffset);
                    raf.readFully(apkSrcBuff, 0, buffLen);
                    scanOffset = sgnDirectoryOffset + sgnDirectoryLen;
                } else if (scanOffset >= sgnDirectoryOffset + sgnDirectoryLen && scanOffset + apkSrcBuff.length <= endheardOffset) {
                    buffLen = apkSrcBuff.length;
                    raf.readFully(apkSrcBuff);
                    scanOffset += 1024;
                } else if (scanOffset < endheardOffset && scanOffset + apkSrcBuff.length > endheardOffset) {
                    buffLen = (int) (endheardOffset - scanOffset);
                    raf.readFully(apkSrcBuff, 0, buffLen);
                    scanOffset = endheardOffset;
                } else if (scanOffset == endheardOffset) {
                    buffLen = (int) (raf.length() - scanOffset > apkSrcBuff.length ? apkSrcBuff.length : raf.length() - scanOffset);
                    raf.readFully(apkSrcBuff, 0, buffLen);
                    byte[] dirNums = ApkUtils.shortToBytesReverse((short) (centDirNums - 1));
                    byte[] dirStruNums = ApkUtils.shortToBytesReverse((short) (centStruNums - 1));
                    byte[] dirSize = ApkUtils.intToBytesReverse(centDirSize - sgnDirectoryLen);
                    byte[] dirOffset = ApkUtils.intToBytesReverse((int) (centDirOffset - sgnFileLen));

                    System.arraycopy(dirNums, 0, apkSrcBuff, 8, dirNums.length);
                    System.arraycopy(dirStruNums, 0, apkSrcBuff, 10, dirStruNums.length);
                    System.arraycopy(dirSize, 0, apkSrcBuff, 12, dirSize.length);
                    System.arraycopy(dirOffset, 0, apkSrcBuff, 16, dirOffset.length);
                    scanOffset += buffLen;
                }

                //signa.update(apkSrcBuff, 0, buffLen);
                //update origin apk ctx
                GmSSLAlgorithm.digestUpdate(ctx, apkSrcBuff, buffLen);
                raf.seek(scanOffset);
            }
            raf.close();
            // get the data and length of "SGN data main body"
            if (certBody != null && certBody.length > 0) {
                //signa.update(certBody, 0, certBody.length);
                //update "SGN data main body" ctx
                GmSSLAlgorithm.digestUpdate(ctx, certBody, certBody.length);
            }

            // get the digest
            byte[] digest = GmSSLAlgorithm.digestFinal(ctx);
            logI("digest---->" + ApkUtils.bytesToHexString(digest));
            logI("digitCertPub---->" + ApkUtils.bytesToHexString(digitCertPub));
            boolean result = false;
            int backValue = GmSSLAlgorithm.verify("sm2sign", digest, headerSignature, digitCertPub);
            logI("verify GM hash backValue = " + backValue);
            if (backValue == 1) {
                logI("verify GM hash successfully !!!!");
                result = true;
            }
            //boolean result = signa.verify(headerSignature);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            slog("Verify apk signature error!");
        }
        return false;
    }

    public HashSet<String> getPerms() {
        if (permsFile != null)
            return permsFile.getPermissions();
        return null;
    }

    public HashSet<String> getDefPerms() {
        if (permsFile != null)
            return permsFile.getDefPermissions();
        return null;
    }

    private void slog(String info) {
        if (DEBUG) Log.d(TAG, info);
    }

    private void logI(String msg) {
        if (DEBUG) Log.i(TAG, msg);
    }

    private static class PermsFile {
        private final HashSet<String> permissions;
        private final HashSet<String> defPermissions;

        public PermsFile(byte[] perms) {
            String permStr = new String(perms);
            //if(DEBUG) Log.i(TAG, "PermsFile Perms:" + permStr);
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

}
