package com.example.demo;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSequence;

public class SignInfoDERSequence extends DERSequence {
    /**
     * 结构版本
     * 整数类型(02),长度 1(01), 0x20-国密签名算法
     */
    private ASN1Integer strVer;

    /**
     * 签名该文件的证书ID号
     * PrintableString 类型(13),长度 12(0c),字符串,最后一位为 0
     */
    private DERPrintableString signatureId;

    /**
     * 数字签名算法
     * OBJECT IDENTIFIER 类型(06),长度为 8(08),签名算法为 SM3 和 SM2
     * OID 1.2.156.10197.1.501 :SM3withSM2
     */
    private ASN1ObjectIdentifier digitalSignatureAlgorithm;

    /**
     * 签名时间
     * PrintableString 类 型 (13) , 长 度 16(10), ”YYYY-MM-DD hh:mm”
     */
    private DERPrintableString signDate;

    /**
     * 原始文件的哈希值头
     * 整数类型(02),长度 32(20)
     */
    private ASN1Integer appHash;

    /**
     * 原始文件的哈希值
     * PrintableString 字符串
     */
    private DERPrintableString appHashValue;

    public SignInfoDERSequence(int strVer, String signatureId, String dsa, String signDate, int appHash, String appHashValue) {
        this.strVer = new ASN1Integer(strVer);
        this.signatureId = new DERPrintableString(signatureId);
        this.digitalSignatureAlgorithm = new ASN1ObjectIdentifier(dsa);
        this.signDate = new DERPrintableString(signDate);
        this.appHash = new ASN1Integer(appHash);
        this.appHashValue = new DERPrintableString(appHashValue);
    }
}
