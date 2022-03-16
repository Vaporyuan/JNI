package com.malio.server.pm;

import android.util.Log;
//import android.util.Slog;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ComputeDigests {
    private static final String TAG = "ComputeDigests";
    private static final boolean DEBUG = false;
    private static final int CONTENT_DIGEST_CHUNKED_SHA256 = 0;
    private static final int CONTENT_DIGEST_CHUNKED_SHA512 = 1;
    private static final int CONTENT_DIGESTED_CHUNK_MAX_SIZE_BYTES = 1024 * 1024;
    public static final int SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256 = 0x0103; //259

    private ByteBuffer[] contents;

    public ComputeDigests(ByteBuffer[] contents) {
        this.contents = contents;
    }

    public byte[] computeContentDigests(int sigAlgorithm, int digestAlgorithm, ByteBuffer[] contents) throws DigestException {
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
        logI("computeContentDigests sigAlgorithm = " + sigAlgorithm +
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

    private void logI(String msg) {
        if (DEBUG) Slog.i(TAG,msg);
    }

    private void logD(String msg) {
        if (DEBUG) Slog.d(TAG,msg);
    }

    private void logE(String msg) {
        if (DEBUG) Slog.e(TAG,msg);
    }
}
