package com.malio.server.pm;

import android.text.TextUtils;
import android.util.Base64;
//import android.util.Slog;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipException;

public class ApkUtils {
	private static final boolean DEBUG = true;
	private static final String TAG = "ApkUtils";
	
	public static byte[] intToBytesReverse(int value)
    {
        byte[] src = new byte[4];
        src[0] = (byte) (value & 0xFF);
        src[1] = (byte) ((value>>8)& 0xFF);
        src[2] = (byte) ((value>>16)&0xFF);
        src[3] = (byte) ((value>>24) & 0xFF);
        return src;
    }

    public static byte[] shortToBytesReverse(short value)
    {
        byte[] src = new byte[2];
        src[0] = (byte) (value&0xFF);
        src[1] = (byte) ((value>>8)&0xFF);
        return src;
    }

    public static long searchIdentify(RandomAccessFile raf, int identify, long scanOffset, boolean scanAll) throws IOException{
        long stopOffset = scanAll ? 0 : scanOffset - 65536;
        if (stopOffset < 0) {
            stopOffset = 0;
        }
        while (true) {
            raf.seek(scanOffset);
            if ((int)(get32(raf) & 0xffffffff) == identify) {
                return scanOffset;
            }

            scanOffset--;
            if (scanOffset < stopOffset) {
                throw new ZipException("Identify " + identify + " not found!");
            }
        }
    }

    public static byte[] read(InputStream is, long offset, long length) throws IOException{
        byte[] buffer = new byte[(int)(length & 0xffffffff)];
        if(offset > 0)
            is.skip(offset);
        if(is.read(buffer) != -1){
            return buffer;
        }
        return null;

    }

    // urovo weiyu add on 2020-12-09 [s]
    public static byte[] fileConvertToByteArray(File file) {
        byte[] data = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len;
            byte[] buffer = new byte[1024];
            while ((len = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            data = baos.toByteArray();
            fis.close();
            baos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return data;
    }
    // urovo weiyu add on 2020-12-09 [e]
    
    public static int get16(RandomAccessFile raf) throws IOException{
    	int b0 = raf.read();
    	int b1 = raf.read();
    	return (b0 & 0xff) | ((b1 & 0xff) << 8); 
    }
    
    public static long get32(RandomAccessFile raf) throws IOException{
    	int b0 = get16(raf);
    	int b1 = get16(raf);
    	return (b0 & 0xffff) | ((b1 & 0xffff) << 16); 
    }

    public static long readLen(InputStream is) throws IOException{
        int length = is.read();
        if((length & 0x80) == 0x00){
            return length;
        } else {
        	int lenBytes = length & 0x7f;
        	byte[] src = read(is, 0, lenBytes);
        	long result = 0L;
        	for(int i = 0; i < lenBytes; i++){
        		result = (src[i] & 0xff) << ((lenBytes-i-1)*8) | result;
        	}
        	return result;
        }
    }

    public int readInt(InputStream is) throws IOException{
        byte[] src = read(is, 0, 4);
        if(src != null && src.length == 4){
            return (((src[0] & 0xff) << 24) |
                    ((src[1] & 0xff) << 16) |
                    ((src[2] & 0xff) <<  8) |
                    ((src[3 ] & 0xff) <<  0));
        }

        throw new IOException();
    }

    public short readShort(InputStream is) throws IOException{
        byte[] src = read(is, 0, 2);
        if(src != null && src.length == 2){
            return (short)(((src[0] & 0xff) << 8) |
                    ((src[1] & 0xff) << 0));
        }

        throw new IOException();
    }
    
    public int read3Bytes(InputStream is) throws IOException{
        byte[] src = read(is, 0, 3);
        if(src != null && src.length ==3){
            return (((src[0] & 0xff) << 16) |
                    	  ((src[1] & 0xff) <<   8) |
                    	  ((src[2] & 0xff) <<  0));
        }

        throw new IOException();
    }

    public static byte[] hexStringToByte(String hex) {
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;
    }

    public static String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    public static int toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }

    public void logI(String msg){
        if(DEBUG)
            Slog.i(TAG, msg);
    }

    public void logE(String msg){
        Slog.e(TAG, msg);
    }

    public class StructureCert{
        public byte[] certData;
        public byte[] certSignature;

        public StructureCert(byte[] certs) throws IOException{
            ByteArrayInputStream bis = new ByteArrayInputStream(certs);
            bis.read(); // 头部分
            readLen(bis);
            bis.read();
            certData = read(bis, 0, readLen(bis));
            bis.read(); // 证书签名算法标识
            bis.skip(readLen(bis));
            int type = bis.read();
            certSignature = read(bis, 1, readLen(bis)-1); // 1位填充位00
            bis.close();
        }

        public byte[] getCertData(){
            return certData;
        }

        public byte[] getCertSignature(){
            return certSignature;
        }
    }
    
    public static class DerFile {

    	public InputStream mIs;

    	public DerFile(InputStream is) {
    		mIs = is;
    	}

    	public DerObject getNextDerObject() {
    		DerObject dObj = new DerObject();
    		try {
    			dObj.type = (byte) mIs.read();
    			dObj.length = mIs.read();
    			if ((dObj.length & 0x80) == 0x00) {
    				dObj.lengthBody = new byte[] { (byte) dObj.length };
    			} else {
    				int lenBytes = dObj.length & 0x7f;
    				byte[] src = read(mIs, 0, lenBytes);
    				int result = 0;
    				for (int i = 0; i < lenBytes; i++) {
    					result = (src[i] & 0xff) << ((lenBytes - i - 1) * 8)
    							| result;
    				}
    				dObj.lengthBody = new byte[lenBytes + 1];
    				dObj.lengthBody[0] = (byte) dObj.length;
    				System.arraycopy(src, 0, dObj.lengthBody, 1, lenBytes);
    				dObj.length = result;
    			}
    			dObj.mainBody = read(mIs, 0, dObj.length);
    			if (dObj.mainBody != null) {
    				dObj.allBody = new byte[1 + dObj.lengthBody.length + dObj.mainBody.length];
    			} else {
    				dObj.allBody = new byte[1 + dObj.lengthBody.length];
    			}
    			dObj.allBody[0] = dObj.type;
    			System.arraycopy(dObj.lengthBody, 0, dObj.allBody, 1, dObj.lengthBody.length);

    			if (dObj.mainBody != null) {
    				System.arraycopy(dObj.mainBody, 0, dObj.allBody, 1 + dObj.lengthBody.length,
    						dObj.mainBody.length);
    			}
    			if (isSequenceType(dObj.type)) {
    				ByteArrayInputStream bais = new ByteArrayInputStream(dObj.mainBody);
    				DerFile derFile; 
    				while (bais.available() > 0) {
    					derFile = new DerFile(bais);
    					dObj.sonObjs.add(derFile.getNextDerObject());
    				}
    				bais.close();
    			}
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    			return null;
    		}
    		return dObj;
    	}

    	public boolean isSequenceType(byte type) {
    		return type == (byte)0x30 || type == (byte)0xA3;
    	}
    	
    }
    
    public static class DerObject{
		public byte type;
		public int length;
		public byte[] lengthBody;
		public byte[] mainBody;
		public byte[] allBody;
		public List<DerObject> sonObjs = new ArrayList<DerObject>();
	}
    
    public static class CaPublickKey{
        private byte[] modulus;
        private byte[] exponent;

        public CaPublickKey(String pblcKy) throws IOException{
            byte[] key = hexStringToByte(pblcKy);
            ByteArrayInputStream bis = new ByteArrayInputStream(key);
            bis.read(); // 3082010a
            readLen(bis);
            bis.read(); // 0282010100
            modulus = read(bis, 0, readLen(bis));
            bis.read(); // 0203010001
            exponent = read(bis, 0, readLen(bis));
            bis.close();
        }

        public PublicKey getPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
            RSAPublicKeySpec caKeySpec = new RSAPublicKeySpec(new BigInteger(modulus), new BigInteger(exponent));
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePublic(caKeySpec);
        }
    }

    public static PublicKey loadPublicKey(String publicKeyStr) throws Exception {
        byte[] buffer = Base64.decode(publicKeyStr, Base64.DEFAULT);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
        return keyFactory.generatePublic(keySpec);
    }
}
