package com.malio.server.pm;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
//import android.util.Slog;
import java.io.FileOutputStream;
import android.util.Pair;

import com.example.demo.utils.ApkUtils;
import com.malio.gmssl.GmSSLAlgorithm;
import com.example.demo.utils.ReadApkSignData;
import com.malio.server.ApkSignatureSchemeV2VerifierEx;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by Felix on 2020-12-09
 */
public class CITICApkFile {
    private static final int ZIP64_EOCD_LOCATOR_SIZE = 20;
    private static final int ZIP64_EOCD_LOCATOR_SIG_REVERSE_BYTE_ORDER = 0x504b0607;
    private static final int ZIP_EOCD_CENTRAL_DIR_OFFSET_FIELD_OFFSET = 16;

    private static final String TAG = "CITICApkFile";
    private static final boolean DEBUG = true; //false;

    //private static String rootCertPath = "/etc/CITIC_CA.cer";
    private static final String rootCertPath = "/sdcard/CITIC_CA.cer";
    private final String SGN_NAME = "META-INF/CITICAPKSIGNV1.SGN";
    private byte[] certBody; //the main body of sign information
    private byte[] headerSignature; // the sign data (R and S)
    private byte[] digitCertPem; // the public key that got from digit cert
    private byte[] digitCert; // the digit cert

    private X509Certificate structureCert;
    private final String apkPath;
    private PermsFile permsFile;

    private static String notifierRootPath = "/data/system/"; // hashverify.dat"
    private static String notifierFileName = "hashverify.dat";


    public CITICApkFile(String apkPath) {
        this.apkPath = apkPath;
    }
    
    public HashSet<String> getPerms(){
    	if(permsFile != null)
    		return permsFile.getPermissions();
    	 
    	return null;
    }

    /**
     * get the permissions that defined in SGN Data
     */
    public HashSet<String> getDefPerms(){
        if(permsFile != null)
            return permsFile.getDefPermissions();

        return null;
    }

    private boolean verifyCiticHash() {

        if (DEBUG) Slog.i(TAG, "hashverify verifyHashing..."); //????
		RandomAccessFile apk = null;

        try {
            apk = new RandomAccessFile(apkPath, "r");
            if (DEBUG) Slog.d(TAG,"apk length------->"+apk.length());

            // get the offset of the "End of Central Directory" and the "End of Central Directory" data(ByteBuffer)
            Pair<ByteBuffer, Long> eocdAndOffsetInFile = ApkSignatureSchemeV2VerifierEx.getEocd(apk);
            ByteBuffer eocd = eocdAndOffsetInFile.first; // the data of the "End of Central Directory"
            long eocdOffset = eocdAndOffsetInFile.second; // the offset of the "End of Central Directory"
            long eocdLen = apk.length() - eocdOffset; // the length of the "End of Central Directory"
            if (isZip64EndOfCentralDirectoryLocatorPresent(apk, eocdOffset)) {
                throw new ApkSignatureSchemeV2VerifierEx.SignatureNotFoundException("ZIP64 APK not supported");
            }

            // Find the APK Signing Block. The block immediately precedes the Central Directory.
            // get the offset,length and data of the "Central Directory"
            long centralDirOffset = ApkSignatureSchemeV2VerifierEx.getCentralDirOffset(eocd, eocdOffset); // the offset of "Central Directory"
            long cdLen = eocdOffset - centralDirOffset; // the length of "Central Directory"
            ByteBuffer cdData = ByteBuffer.allocate((int)cdLen); // the data of "Central Directory"
            cdData.order(ByteOrder.LITTLE_ENDIAN);
            apk.seek(centralDirOffset);
            apk.readFully(cdData.array(), cdData.arrayOffset(), cdData.capacity());

            // get the data,length and offset(the length of "Contents of ZIP entries") of "APK Signing Block"
            Pair<ByteBuffer, Long> apkSigningBlockAndOffsetInFile = ApkSignatureSchemeV2VerifierEx.findApkSigningBlock(apk, centralDirOffset);
            ByteBuffer apkSigningBlock = apkSigningBlockAndOffsetInFile.first; // the data of "APK Signing Block"
            long cozeLen = apkSigningBlockAndOffsetInFile.second; // the offset(the length of "Contents of ZIP entries") of "APK Signing Block"
            long asbLen = apkSigningBlock.capacity(); // the length of "APK Signing Block"

            // To know whether the origin apk contains the "APK Signing Block" or not
            boolean hasV2Block = ApkSignatureSchemeV2VerifierEx.hasApkSignatureSchemeV2Block(apkSigningBlock);

            // get the data,length and offset of "SGN data"
            Pair<ByteBuffer, Long> sgnDataAndOffset = ApkSignatureSchemeV2VerifierEx.findCiticApkSignatureSchemeV2Block(apkSigningBlock);
            ByteBuffer sgnData = sgnDataAndOffset.first; // the data of "SGN data"
            long sgnBlockOffset = sgnDataAndOffset.second; // the offset of "SGN data"
            long sgnLength = sgnData.getLong() + 8; // the length of "SGN data"

            // get the data and length of "SGN data main body"
            ByteBuffer sgnMainData = ByteBuffer.wrap(certBody); // the data of "SGN data main body"
            long sgnMainDataLength = certBody.length; // the length of "SGN data main body"

            // delete the ID-value from the "APK Signing Block" and change the "Block Len" to the org value after deleting
            long realAsbLen = asbLen - sgnLength; // the org length of the "APK Signing Block" after deleting the ID-value
            apkSigningBlock.position(0);
            apkSigningBlock.putLong(realAsbLen - 8 /*the "Block Len shouldn't contains the size of itself"*/); // change the first "Block Len"

            byte [] magic = new byte[16]; // the magic of the "APK Signing Block"
            apkSigningBlock.position(apkSigningBlock.capacity() - 16);
            apkSigningBlock.get(magic,0,16);
            apkSigningBlock.position((int)sgnBlockOffset); // replace the ID-value data(means deleting the ID-value data from the "APK Signing Block")
            apkSigningBlock.putLong(realAsbLen - 8 /*the "Block Len shouldn't contains the size of itself"*/); // change the second "Block Len"
            apkSigningBlock.put(magic,0,16);

            if (!hasV2Block) { // the origin apk without v2 block,so we should delete the whole "APK Signing Block"
                realAsbLen = 0;
            }

            // change the CentralDirectoryOffset that saved in "End of Central Directory"
            setZipEocdCentralDirectoryOffset(eocd, hasV2Block ? (centralDirOffset - sgnLength) : (centralDirOffset - asbLen));

            ReadApkSignData readApkSignData = new ReadApkSignData(cozeLen,realAsbLen,cdLen,eocdLen,sgnMainDataLength,apk,
                    apkSigningBlock,cdData,eocd,sgnMainData);

            byte [] buffer = new byte[1024];
            int bufferLen = 1024;
            int totalReaded = 0;
            int ret;
            long ctx = GmSSLAlgorithm.digestInit("SM3");
            byte[] idDigest = GmSSLAlgorithm.computeIdDigest("1234567812345678",digitCertPem);
            if (idDigest != null) {
                GmSSLAlgorithm.digestUpdate(ctx,idDigest,idDigest.length);
            }
            // start read the origin apk data and add the "SGN data main body" to the end of the origin apk data
            while(true) {
                ret = (int) readApkSignData.readBlock(totalReaded,bufferLen,buffer);
                totalReaded += ret;
                //if (DEBUG) Slog.d(TAG, "ret = " + ret);
                if (ret != bufferLen) {
                    if (DEBUG) Slog.d(TAG,"finish reading block !!!!!!!!!!");
                    GmSSLAlgorithm.digestUpdate(ctx,buffer,ret);
                    break;
                }
                GmSSLAlgorithm.digestUpdate(ctx,buffer,bufferLen);
            }

            // get the digest
            byte[] digest = GmSSLAlgorithm.digestFinal(ctx);

            if (DEBUG) Slog.d(TAG,"digest---->"+ApkUtils.bytesToHexString(digest));
            if (DEBUG) Slog.d(TAG, "digitCertPub---->" + com.malio.server.pm.ApkUtils.bytesToHexString(digitCertPem));
            boolean result = false;

            int backValue = GmSSLAlgorithm.verify("sm2sign",digest,headerSignature,digitCertPem);

            if (backValue == 1) {
                if (DEBUG) Slog.d(TAG,"verify citic hash successfully !!!!");
                result = true;
            }

            return result;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            logE("Verify apk signature error!");
            e.printStackTrace();
        }

        return false;
    }

    // UROVO BEGIN
    // Modified for verifying signature & installing concurrently by ag. on Sep. 14th, 2020.
    private static boolean verifyCITICApkSignResult;

    public boolean verifyCITICApkSign() {

        if (!verifyCiticStructKey()) {
            return false;
        }
        verifyCITICApkSignResult = verifyCiticHash();

        return verifyCITICApkSignResult;
    }
    // UROVO END

	private boolean verifyCiticStructKey(){
		InputStream is = null;
        try {
            // get the SGN data
            is = ApkSignatureSchemeV2VerifierEx.getCiticSgnData(apkPath);

            ApkUtils.DerFile derFile = new ApkUtils.DerFile(is);
            ApkUtils.DerObject objName = derFile.getNextDerObject();
            if(!"ACQUIRER-SGN-INFO".equals(new String(objName.mainBody))){
                logI("string ACQUIRER-SGN-INFO can not found in SGN.");
                return false;
            }
            ApkUtils.DerObject objHeader = derFile.getNextDerObject();
            logI("objHeader.sonObjs.size()----------->" + objHeader.sonObjs.size());
            if(objHeader.sonObjs.size() >= 3){
                ApkUtils.DerObject objInfo = objHeader.sonObjs.get(0);
                logI("objInfo.length ================ " + objInfo.sonObjs.size());
                ApkUtils.DerObject exInfo = objInfo.sonObjs.get(5);
                ApkUtils.DerObject permInfo = exInfo.sonObjs.get(0).sonObjs.get(0).sonObjs.get(1);
                logI("permInfo================"+ApkUtils.bytesToHexString(permInfo.mainBody));
                permsFile = new PermsFile(permInfo.mainBody);
                ApkUtils.DerObject objSign = objHeader.sonObjs.get(1);
                logI("objSign================"+ApkUtils.bytesToHexString(objSign.mainBody));
                ApkUtils.DerObject objCert = objHeader.sonObjs.get(2);
                logI("objCert================"+ApkUtils.bytesToHexString(objCert.mainBody));

                // get the data of "SGN data main body"
                certBody = objInfo.allBody;

                // get the data of digit cert
                digitCert = new byte[objCert.mainBody.length-1];
                System.arraycopy(objCert.mainBody, 1, digitCert, 0, objCert.mainBody.length - 1);
                logI("digitCert=================="+ApkUtils.bytesToHexString(digitCert));

                // get the sign data 30 44 02 20 R 02 20 S
                // if the first bit of the first byte of R is 1,just like 0x1*** ****,the data should be 30 45 02 21 00 R 02 20 S
                // if the first bit of the first byte of S is 1,just like 0x1*** ****,the data should be 30 45 02 20 R 02 21 00 S
                // if both of the first bit of the first byte of R and S are 1,just like 0x1*** ****,the data should be 30 46 02 21 00 R 02 21 00 S
                // if niether of the first bit of the first byte of R or S is 1,just like 0x0*** ****,the data should be 30 44 02 20 R 02 20 S
                byte[] R = new byte[32];
                byte[] S = new byte[32];
                System.arraycopy(objSign.mainBody,5,R,0,32);
                System.arraycopy(objSign.mainBody,39,S,0,32);

                if ((R[0] & 1<<8) != 0 && (S[0] & 1<<8) != 0) {
                    headerSignature = new byte[objSign.mainBody.length + 1];
                    System.arraycopy(objSign.mainBody,1,headerSignature,0,2);
                    headerSignature[1] = (byte) (headerSignature[1] + 2);
                    headerSignature[2] = 0x02;
                    headerSignature[3] = 0x21;
                    headerSignature[4] = 0x00;
                    System.arraycopy(R,0,headerSignature,5,R.length);
                    headerSignature[37] = 0x02;
                    headerSignature[38] = 0x21;
                    headerSignature[39] = 0x00;
                    System.arraycopy(S,0,headerSignature,40,S.length);
                } else if ((R[0] & 1<<8) != 0 && (S[0] & 1<<8) == 0) {
                    headerSignature = new byte[objSign.mainBody.length];
                    System.arraycopy(objSign.mainBody,1,headerSignature,0,2);
                    headerSignature[1] = (byte) (headerSignature[1] + 1);
                    headerSignature[2] = 0x02;
                    headerSignature[3] = 0x21;
                    headerSignature[4] = 0x00;
                    System.arraycopy(R,0,headerSignature,5,R.length);
                    System.arraycopy(objSign.mainBody,37,headerSignature,37,34);
                } else if ((R[0] & 1<<8) == 0 && (S[0] & 1<<8) != 0) {
                    headerSignature = new byte[objSign.mainBody.length];
                    System.arraycopy(objSign.mainBody,1,headerSignature,0,2);
                    headerSignature[1] = (byte) (headerSignature[1] + 1);
                    System.arraycopy(objSign.mainBody,3,headerSignature,2,34);
                    headerSignature[36] = 0x02;
                    headerSignature[37] = 0x21;
                    headerSignature[38] = 0x00;
                    System.arraycopy(S,0,headerSignature,39,S.length);
                } else {
                    headerSignature = new byte[objSign.mainBody.length - 1];
                    System.arraycopy(objSign.mainBody, 1,headerSignature,0,objSign.mainBody.length - 1);
                }

                logI("headerSignature==============="+ApkUtils.bytesToHexString(headerSignature));

                // get the public key of the digit cert
                digitCertPem = GmSSLAlgorithm.X509_getPubkey(digitCert);

                // get the public key of the root cert(CITIC_CA.cer)
                byte[] citicCaCertPem = GmSSLAlgorithm.X509_getPubkey(ApkUtils.fileConvertToByteArray(new File(rootCertPath)));

                // use the citicCaCertPem to verify the digitCert
                if (GmSSLAlgorithm.X509_VerifyByPubkey(digitCert,citicCaCertPem) == 1) {
                    Slog.i(TAG, "citicCaCertPem verify digitCert successful!!!!!");
                    return true;
                }
            }

        } catch (Exception e){
            logE("Verify structure certificate error!");
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
    
    private void logI(String msg){
        if(DEBUG)
            Slog.i(TAG, msg);
    }

    private void logE(String msg){
        Slog.e(TAG, msg);
    }

   private static class PermsFile{
	   private final HashSet<String> permissions;
       private final HashSet<String> defPermissions;
	   
	   public PermsFile(byte[] perms){
		   String permStr = new String(perms);
           //Log.i(TAG, "PermsFile Perms:" + permStr);
		   Pattern pattern = Pattern.compile("\\[Uses\\-permission\\-\\d+\\][\r\n]*Name\\=([\\w\\.]*)[\r\n]*");
		   Matcher matcher = pattern.matcher(permStr);
		   permissions = new HashSet<String>();
		   while(matcher.find()){
			   if(matcher.group().length() >1)
				   permissions.add(matcher.group(1));
		   }
		   /*[Permission-11]
           Domain-1=Manager
           Name=com.icbc.smartpos.bankpay.trans.preauth*/
           pattern = Pattern.compile("\\[Permission\\-\\d+\\][\r\n]*Domain-1\\=Manager[\r\n]*Name\\=([\\w\\.]*)[\r\n]*");
           matcher = pattern.matcher(permStr);
           defPermissions = new HashSet<String>();
           while(matcher.find()){
               if(matcher.group().length() >1)
                   defPermissions.add(matcher.group(1));
           }
	   }
	   
	   public HashSet<String> getPermissions(){
		   return permissions;
	   }
       public HashSet<String> getDefPermissions(){
           return defPermissions;
       }
   }

   public static final boolean isZip64EndOfCentralDirectoryLocatorPresent(
            RandomAccessFile zip, long zipEndOfCentralDirectoryPosition) throws IOException {

        // ZIP64 End of Central Directory Locator immediately precedes the ZIP End of Central
        // Directory Record.
        long locatorPosition = zipEndOfCentralDirectoryPosition - ZIP64_EOCD_LOCATOR_SIZE;
        if (locatorPosition < 0) {
            return false;
        }

        zip.seek(locatorPosition);
        // RandomAccessFile.readInt assumes big-endian byte order, but ZIP format uses
        // little-endian.
        return zip.readInt() == ZIP64_EOCD_LOCATOR_SIG_REVERSE_BYTE_ORDER;
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
}
