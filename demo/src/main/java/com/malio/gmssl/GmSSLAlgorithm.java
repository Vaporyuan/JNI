package com.malio.gmssl;

/*
 * Copyright (C) 2019, Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @Author: rocky
 * @Date: 20-12-8下午11:16
 */
public class GmSSLAlgorithm {
    static public native byte[] generateRandom(int length);
    static public native byte[] sign(String algor, byte[] data, byte[] privateKey);
    static public native int verify(String algor, byte[] digest, byte[] signature, byte[] publicKey);
    static public native int verifyWithSM3(byte[] digest, byte[] signature, byte[] publicKey);
    static public native byte[] privateKeyEncrypt(String algor, byte[] in, byte[] privateKey);
    static public native byte[] privateKeyDecrypt(String algor, byte[] in, byte[] publicKey);
    static public native byte[] publicKeyEncrypt(String algor, byte[] in, byte[] publicKey);
    static public native byte[] publicKeyDecrypt(String algor, byte[] in, byte[] privateKey);
    static public native byte[] digest(String algor, byte[] data);
    /**
     * 根证书验证工作证书合法性
     * @param cert 工作证书
     * @param root_cert 根证书
     * @return
     */
    //static public native int X509_Verify(byte[] cert, byte[] root_cert);

    /**
     * 工作证书中获取公钥der
     * @param cert 工作证书
     * @return
     */
    static public native byte[] X509_getPubkey(byte[] cert);
    /**
     * 公钥验证工作证书
     * @param cert 工作证书
     * @param publicKey 公钥
     * @return
     */
    static public native int X509_VerifyByPubkey(byte[] cert, byte[] publicKey);
    /**
     *分段数据传输摘要计算，初始化
     * @param EVP_MDType  SM3
     * @return 返回 EVP_MD_CTX 上下文指针
     */
    public static native long digestInit(String EVP_MDType);

    /**
     *
     * @param userId
     * @param publicKey
     * @return
     */
    public static native byte[]  computeIdDigest(String userId, byte[] publicKey);
    /**
     *分循环传入需要计算的摘要数据
     * @param EVP_MDType  SM3
     * @return
     */
    public static native int digestUpdate(long cxt, byte[] in, int inlength);

    /**
     *获取摘要计算结果
     * @return
     */
    public static native byte[] digestFinal(long cxt);

    /**
     * load lib
     */
    static {
        System.loadLibrary("GmSSLAlgorithm");
    }
}
