// ISecureElement.aidl
package com.alipay.iot.pal.api;

// Declare any non-default types here with import statements

interface ISecureElement {
    int openPreventDisassembly(); //打开防拆功能
    int getDisassemblyStatus(); //是否触发
    int setAcessKey(in byte[] key);
    byte[] accessKeyEnc(in byte[] key);
    int setCipherKey(in String key);
    String cipherEncrypt(in String content);
}