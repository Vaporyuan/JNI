package com.example.demo.utils;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.DigestException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipException;

// urovo weiyu add on 2020-12-09 [s]
import java.io.ByteArrayOutputStream;
// urovo weiyu add on 2020-12-09 [e]

public class ApkUtils {
    private static final boolean DEBUG = true;
    private static final String TAG = "ApkUtils";

    public static byte[] intToBytesReverse(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) (value & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[3] = (byte) ((value >> 24) & 0xFF);
        return src;
    }

    public static byte[] shortToBytesReverse(short value) {
        byte[] src = new byte[2];
        src[0] = (byte) (value & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        return src;
    }

    public static long searchIdentify(RandomAccessFile raf, int identify, long scanOffset, boolean scanAll) throws IOException {
        long stopOffset = scanAll ? 0 : scanOffset - 65536;
        if (stopOffset < 0) {
            stopOffset = 0;
        }
        while (true) {
            raf.seek(scanOffset);
            if ((int) (get32(raf) & 0xffffffff) == identify) {
                return scanOffset;
            }

            scanOffset--;
            if (scanOffset < stopOffset) {
                throw new ZipException("Identify " + identify + " not found!");
            }
        }
    }

    public static byte[] read(InputStream is, long offset, long length) throws IOException {
        byte[] buffer = new byte[(int) (length & 0xffffffff)];
        if (offset > 0)
            is.skip(offset);
        if (is.read(buffer) != -1) {
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

    public static int get16(RandomAccessFile raf) throws IOException {
        int b0 = raf.read();
        int b1 = raf.read();
        return (b0 & 0xff) | ((b1 & 0xff) << 8);
    }

    public static long get32(RandomAccessFile raf) throws IOException {
        int b0 = get16(raf);
        int b1 = get16(raf);
        return (b0 & 0xffff) | ((b1 & 0xffff) << 16);
    }

    public static long readLen(InputStream is) throws IOException {
        int length = is.read();
        if ((length & 0x80) == 0x00) {
            return length;
        } else {
            int lenBytes = length & 0x7f;
            byte[] src = read(is, 0, lenBytes);
            long result = 0L;
            for (int i = 0; i < lenBytes; i++) {
                result = (src[i] & 0xff) << ((lenBytes - i - 1) * 8) | result;
            }
            return result;
        }
    }

    public int readInt(InputStream is) throws IOException {
        byte[] src = read(is, 0, 4);
        if (src != null && src.length == 4) {
            return (((src[0] & 0xff) << 24) |
                    ((src[1] & 0xff) << 16) |
                    ((src[2] & 0xff) << 8) |
                    ((src[3] & 0xff) << 0));
        }

        throw new IOException();
    }

    public short readShort(InputStream is) throws IOException {
        byte[] src = read(is, 0, 2);
        if (src != null && src.length == 2) {
            return (short) (((src[0] & 0xff) << 8) |
                    ((src[1] & 0xff) << 0));
        }

        throw new IOException();
    }

    public int read3Bytes(InputStream is) throws IOException {
        byte[] src = read(is, 0, 3);
        if (src != null && src.length == 3) {
            return (((src[0] & 0xff) << 16) |
                    ((src[1] & 0xff) << 8) |
                    ((src[2] & 0xff) << 0));
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

    /*public static String bytesToHexString(byte[] data) {
        if (data == null || data.length <= 0) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            String temp = Integer.toHexString(((int) data[i]) & 0xFF);
            for (int t = temp.length(); t < 2; t++) {
                sb.append("0");
            }
            sb.append(temp);
        }
        return sb.toString();
    }*/

    public static int toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
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

    public class StructureCert {
        public byte[] certData;
        public byte[] certSignature;

        public StructureCert(byte[] certs) throws IOException {
            ByteArrayInputStream bis = new ByteArrayInputStream(certs);
            bis.read(); // 头部分
            readLen(bis);
            bis.read();
            certData = read(bis, 0, readLen(bis));
            bis.read(); // 证书签名算法标识
            bis.skip(readLen(bis));
            int type = bis.read();
            certSignature = read(bis, 1, readLen(bis) - 1); // 1位填充位00
            bis.close();
        }

        public byte[] getCertData() {
            return certData;
        }

        public byte[] getCertSignature() {
            return certSignature;
        }
    }

    private static void LogI(String msg) {
        Log.i("VerityApkFile", msg);
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
                    dObj.lengthBody = new byte[]{(byte) dObj.length};
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
                //LogI("33 dObj.allBody = " + bytesToHexString(dObj.allBody));
                if (isSequenceType(dObj.type)) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(dObj.mainBody);
                    //LogI("44 dObj.mainBody = " + bytesToHexString(dObj.mainBody));
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
            return type == (byte) 0x30 || type == (byte) 0xA3;
        }

    }

    public static class DerObject {
        public byte type;
        public int length;
        public byte[] lengthBody;
        public byte[] mainBody;
        public byte[] allBody;
        public List<DerObject> sonObjs = new ArrayList<DerObject>();
    }

    public static class CaPublickKey {
        private byte[] modulus;
        private byte[] exponent;

        public CaPublickKey(String pblcKy) throws IOException {
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

    /////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////验签App摘要分段加速计算////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    private static final int CONTENT_DIGEST_CHUNKED_SHA256 = 0;
    private static final int CONTENT_DIGEST_CHUNKED_SHA512 = 1;
    private static final int CONTENT_DIGESTED_CHUNK_MAX_SIZE_BYTES = 1024 * 1024;
    public static final int SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256 = 0x0103; //259
    public static byte[] computeContentDigests(int sigAlgorithm, int digestAlgorithm, ByteBuffer[] contents) throws DigestException {
        // For each digest algorithm the result is computed as follows:
        // 1. Each segment of contents is split into consecutive chunks of 1 MB in size.
        //    The final chunk will be shorter iff the length of segment is not a multiple of 1 MB.
        //    No chunks are produced for empty (zero length) segments.
        // 2. The digest of each chunk is computed over the concatenation of byte 0xa5, the chunk's
        //    length in bytes (uint32 little-endian) and the chunk's contents.
        // 3. The output digest is computed over the concatenation of the byte 0x5a, the number of
        //    chunks (uint32 little-endian) and the concatenation of digests of chunks of all
        //    segments in-order.

        int chunkCount = 0;
        for (ByteBuffer input : contents) {
            chunkCount += getChunkCount(input.remaining(), CONTENT_DIGESTED_CHUNK_MAX_SIZE_BYTES);
        }

        int digestOutputSizeBytes = getContentDigestAlgorithmOutputSizeBytes(digestAlgorithm);
        byte[] concatenationOfChunkCountAndChunkDigests = new byte[5 + chunkCount * digestOutputSizeBytes];
        concatenationOfChunkCountAndChunkDigests[0] = 0x5a;
        setUnsignedInt32LittleEngian(chunkCount, concatenationOfChunkCountAndChunkDigests, 1);

        int chunkIndex = 0;
        byte[] chunkContentPrefix = new byte[5];
        chunkContentPrefix[0] = (byte) 0xa5;
        // Optimization opportunity: digests of chunks can be computed in parallel.
        for (ByteBuffer input : contents) {
            while (input.hasRemaining()) {
                int chunkSize = Math.min(input.remaining(), CONTENT_DIGESTED_CHUNK_MAX_SIZE_BYTES);
                final ByteBuffer chunk = getByteBuffer(input, chunkSize);
                String jcaAlgorithmName = getContentDigestAlgorithmJcaDigestAlgorithm(digestAlgorithm);
                MessageDigest md;
                try {
                    md = MessageDigest.getInstance(jcaAlgorithmName);
                } catch (NoSuchAlgorithmException e) {
                    throw new DigestException(
                            jcaAlgorithmName + " MessageDigest not supported", e);
                }
                // Reset position to 0 and limit to capacity. Position would've been modified
                // by the preceding iteration of this loop. NOTE: Contrary to the method name,
                // this does not modify the contents of the chunk.
                chunk.clear();
                setUnsignedInt32LittleEngian(chunk.remaining(), chunkContentPrefix, 1);
                md.update(chunkContentPrefix);
                md.update(chunk);
                int expectedDigestSizeBytes =
                        getContentDigestAlgorithmOutputSizeBytes(digestAlgorithm);
                int actualDigestSizeBytes =
                        md.digest(
                                concatenationOfChunkCountAndChunkDigests,
                                5 + chunkIndex * expectedDigestSizeBytes,
                                expectedDigestSizeBytes);
                if (actualDigestSizeBytes != expectedDigestSizeBytes) {
                    throw new DigestException(
                            "Unexpected output size of " + md.getAlgorithm()
                                    + " digest: " + actualDigestSizeBytes);
                }
                chunkIndex++;
            }
        }

        String jcaAlgorithmName = getContentDigestAlgorithmJcaDigestAlgorithm(digestAlgorithm);
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(jcaAlgorithmName);
        } catch (NoSuchAlgorithmException e) {
            throw new DigestException(jcaAlgorithmName + " MessageDigest not supported", e);
        }
        LogI("computeContentDigests sigAlgorithm = " + sigAlgorithm +
                " digestAlgorithm " + digestAlgorithm +
                " md.digest >>>>>: " + Arrays.toString(md.digest(concatenationOfChunkCountAndChunkDigests)));
        byte[] result = encodeAsSequenceOfLengthPrefixedPairsOfIntAndLengthPrefixedBytes(sigAlgorithm,
                md.digest(concatenationOfChunkCountAndChunkDigests));
        return result;
    }
    private static final int getChunkCount(int inputSize, int chunkSize) {
        return (inputSize + chunkSize - 1) / chunkSize;
    }
    private static void setUnsignedInt32LittleEngian(int value, byte[] result, int offset) {
        result[offset] = (byte) (value & 0xff);
        result[offset + 1] = (byte) ((value >> 8) & 0xff);
        result[offset + 2] = (byte) ((value >> 16) & 0xff);
        result[offset + 3] = (byte) ((value >> 24) & 0xff);
    }
    private static String getContentDigestAlgorithmJcaDigestAlgorithm(int digestAlgorithm) {
        switch (digestAlgorithm) {
            case CONTENT_DIGEST_CHUNKED_SHA256:
                return "SHA-256";
            case CONTENT_DIGEST_CHUNKED_SHA512:
                return "SHA-512";
            default:
                throw new IllegalArgumentException(
                        "Unknown content digest algorthm: " + digestAlgorithm);
        }
    }
    private static int getContentDigestAlgorithmOutputSizeBytes(int digestAlgorithm) {
        switch (digestAlgorithm) {
            case CONTENT_DIGEST_CHUNKED_SHA256:
                return 256 / 8;
            case CONTENT_DIGEST_CHUNKED_SHA512:
                return 512 / 8;
            default:
                throw new IllegalArgumentException(
                        "Unknown content digest algorthm: " + digestAlgorithm);
        }
    }
    public static ByteBuffer getByteBuffer(ByteBuffer source, int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size: " + size);
        }
        int originalLimit = source.limit();
        int position = source.position();
        int limit = position + size;
        if ((limit < position) || (limit > originalLimit)) {
            throw new BufferUnderflowException();
        }
        source.limit(limit);
        try {
            ByteBuffer result = source.slice();
            result.order(source.order());
            source.position(limit);
            return result;
        } finally {
            source.limit(originalLimit);
        }
    }
    public static byte[] encodeAsSequenceOfLengthPrefixedPairsOfIntAndLengthPrefixedBytes(
            int sigAlgorithm, byte[] oriData) {
        int resultSize = 12 + oriData.length;
        ByteBuffer result = ByteBuffer.allocate(resultSize);
        result.order(ByteOrder.LITTLE_ENDIAN);
        byte[] second = oriData;
        result.putInt(8 + second.length);
        result.putInt(sigAlgorithm);
        result.putInt(second.length);
        result.put(second);

        return result.array();
    }

}
