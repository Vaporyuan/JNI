package com.example.base64;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class MainClass {
    public static void main(String[] args) throws UnsupportedEncodingException {

        System.out.println("------------------Base64------------------");
        final Base64.Decoder decoder = Base64.getDecoder();
        //System.out.println(new String(decoder.decode("uqxpmqPgkmHRsmfTLU0tQ4tCLdz3VQ/OVd8Q+8Ekx5A="), "UTF-8"));
        System.out.println(new String(decoder.decode("YTEyMzQ1Ng=="), "UTF-8"));
    }


    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            stringBuilder.append("0x");

            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
            if (i != src.length-1) {
                stringBuilder.append(",");

            }
        }
        return stringBuilder.toString();
    }
}