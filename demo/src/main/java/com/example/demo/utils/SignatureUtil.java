package com.example.demo.utils;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

@RequiresApi(api = Build.VERSION_CODES.O)
public class SignatureUtil {
    /**
     * 签名算法
     */
    public static final String SIGN_ALGORITHMS = "SHA1WithRSA";

    static final Base64.Decoder decoder = Base64.getDecoder();
    static final Base64.Encoder encoder = Base64.getEncoder();

    /**
     * RSA签名
     *
     * @param content    待签名数据
     * @param privateKey 私钥
     * @param encode     字符集编码
     * @return 签名值
     */
    public static String sign(String content, PrivateKey privateKey, String encode) {
        try {
            Signature signature = Signature.getInstance(SIGN_ALGORITHMS);
            signature.initSign(privateKey);
            signature.update(content.getBytes(encode));
            byte[] signed = signature.sign();
            return encoder.encodeToString(signed);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 验签
     *
     * @param content 待签名数据
     * @param sign    签名值
     * @param pubKey  公钥
     * @return 失败时，返回false。
     */
    public static boolean verifySignature(String content, String sign, PublicKey pubKey) {
        if (null == content || null == sign || "".equals(content) || "".equals(sign)) {
            return false;
        }
        try {
            byte[] signed = decoder.decode(sign);
            Signature signatureChecker = Signature.getInstance(SIGN_ALGORITHMS);
            signatureChecker.initVerify(pubKey);
            signatureChecker.update(content.getBytes());
            // 验证签名是否正常
            return signatureChecker.verify(signed);
        } catch (Exception e) {
            return false;
        }
    }
}
