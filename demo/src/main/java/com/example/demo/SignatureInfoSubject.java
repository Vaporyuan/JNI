package com.example.demo;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERExternal;
import org.bouncycastle.asn1.DERSequence;

public class SignatureInfoSubject extends ASN1Object {
    /**
     * 主体其他部分
     */
    private SignInfoDERSequence signInfo;
    /**
     * 权限部分
     */
    private PerDERSequence perDERSequence;
    private DERExternal external; //扩展部分

    public void setSignInfoDERSequence(SignInfoDERSequence signInfo) {
        this.signInfo = signInfo;
    }

    public SignInfoDERSequence getSignInfoDERSequence() {
        return signInfo;
    }

    public void setPerDERSequence(PerDERSequence perDERSequence) {
        this.perDERSequence = perDERSequence;
    }

    public PerDERSequence getPerDERSequence() {
        return perDERSequence;
    }

    public SignatureInfoSubject(SignInfoDERSequence signInfo, PerDERSequence perDERSequence) {
        this.signInfo = signInfo;
        this.perDERSequence = perDERSequence;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector vector = new ASN1EncodableVector();
        vector.add(signInfo);
        vector.add(perDERSequence);
        return new DERSequence(vector);
    }
}
