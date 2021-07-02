package com.example.demo;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSequence;

public class PerDERSequence extends DERSequence {
    /**
     * 权限描述文件标识
     */
    private DERPrintableString permissionHead;

    /**
     * 权限文件长度
     */
    private ASN1Integer permissionInfoLen;
    /**
     * 权限描述文件标识
     */
    private DERPrintableString permissionInfo;

    public PerDERSequence(String permissionHead, int permissionInfoLen, String permissionInfo) {
        this.permissionHead = new DERPrintableString(permissionHead);
        this.permissionInfoLen = new ASN1Integer(permissionInfoLen);
        this.permissionInfo = new DERPrintableString(permissionInfo);
    }
}
