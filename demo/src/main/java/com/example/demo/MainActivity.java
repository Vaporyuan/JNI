package com.example.demo;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;

import android.content.pm.PackageManager;
import android.device.DeviceManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.system.StructUtsname;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.VisibleForTesting;
import androidx.core.app.ActivityCompat;

import com.malio.server.pm.CITICApkFile;
import com.malio.server.pm.demo.MalioApkFile;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSequence;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private String TAG = "Demo";
    private Context context;
    private byte[] acquirer_root;
    private NotificationManager manager;
    DeviceManager mDevice;
    EditText editText;
    private static String signAppName = "/sdcard/AppForPOC.apk"; //"/sdcard/iluncher_signedV3.apk";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    public static void verifyStoragePermissions(Activity activity) {
        try {
            //???????????????????????????
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // ???????????????????????????????????????????????????????????????
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * ???????????????????????????
     */
    public static String stampToDate(String s){
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long lt = new Long(s);
        Date date = new Date(lt);
        res = simpleDateFormat.format(date);
        //Log.i("VerityApkFile", "stampToDate----------->: " + res);
        return res;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // ...
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        context = MainActivity.this;
        acquirer_root = readFileByBytes("/sdcard/urovo-v3.crt");//????????????crt
        mDevice = new DeviceManager();
        verifyStoragePermissions(this);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);
        Button mButton1 = findViewById(R.id.button);
        mButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //verifyAPKDoubleV2Sign(signAppName);

                //V3????????????
                /*VerityApkFile verityApkFile = new VerityApkFile(signAppName);
                verityApkFile.verifyApkSign();*/

                MalioApkFile malioApkFile = new MalioApkFile(signAppName);
                malioApkFile.verifyUbxApkSign();

                /*Intent intent = new Intent("malio.intent.se.trigger");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                MainActivity.this.startActivity(intent);*/
            }
        });

        Button mButton2 = findViewById(R.id.button1);
        mButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CITICApkFile citicApkFile = new CITICApkFile(signAppName);
                citicApkFile.verifyCITICApkSign();
                /*try {
                    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                    Certificate caCer = certificateFactory.generateCertificate(new ByteArrayInputStream(acquirer_root));
                    Log.i(TAG, "verifyAPKDoubleV2Sign caCer: " + caCer);
                }catch (Exception e){
                    e.printStackTrace();
                }*/

                //SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");//??????????????????
                //Log.i("VerityApkFile", "0----------->: " + df.format(new Date()));
                /*String ufstime = mDevice.getSettingProperty("ro.ufs.build.date.utc");
                Log.i("VerityApkFile", "ufstime----------->: " + ufstime);
                long millisecond = Long.parseLong(ufstime);
                Date date = new Date(millisecond);
                SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Log.i("VerityApkFile", "date----------->: " + stampToDate(ufstime));*/
                //Log.i("VerityApkFile", "date001----------->: " + stampToDate("1613906690000"));
                //signatureInfoSubject();

                //??????ANR
                /*try {
                    Thread.sleep(1000000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
            }
        });

        //??????????????????
        //mHandler.sendMessageDelayed(Message.obtain(), 60000*5);
    }


    /**
     * ?????????????????????id??????
     *
     * @return
     */
    private static String getEthernetRouteIpAddress() {


        String ip = "";
        String ipHave = "";
        try {
            Process p = Runtime.getRuntime().exec("ip route show");
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;

            //????????????????????????ip??????????????????
            //default via 192.168.2.1 dev eth0
            //default via 192.168.2.1 dev eth0  metric 206
            //192.168.2.0/24 dev eth0  scope link
            //192.168.2.0/24 dev eth0  proto kernel  scope link  src 192.168.2.21  metric 206
            //192.168.2.0 dev eth0  scope link

            while ((line = in.readLine()) != null
                    && !line.equals("null")) {
                if (line.contains("default via ")) {
                    ipHave += line;
                    break;
                }
            }


            /*if (!"".equals(ipHave)) {
                //????????????ip??????
                String patternS = "default via (\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}) dev eth0 ";
                Pattern p1 = Pattern.compile(patternS);
                Matcher matcher = p1.matcher(ipHave);
                if (matcher.matches())
                    ip = matcher.group(1);
                return ip;//??????ip????????????
            }*/
        } catch (IOException e) {
            e.printStackTrace();
        }


        return ipHave;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //??????null????????????????????????Message???Runnable
        //mHandler.removeCallbacksAndMessages(null);
    }

    public static ByteArrayOutputStream signatureInfoSubject() {
        ByteArrayOutputStream outputStream = null;
        try {
            outputStream = new ByteArrayOutputStream();
            //??????ByteArrayOutputStream????????????????????????byte???
            DEROutputStream derOutputStream = new DEROutputStream(outputStream);
            // 1.??????????????????, "ACQUIRER-SGN-INFO"
            derOutputStream.writeObject(new DERPrintableString("ACQUIRER-SGN-INFO"));

            //????????????Vector
            ASN1EncodableVector mainList = new ASN1EncodableVector();
            // 2. ??????????????????
            ASN1EncodableVector signSubtList = new ASN1EncodableVector();//??????????????????Vector
            /* 2.1 ???????????? */
            ASN1EncodableVector strVerDeVector = new ASN1EncodableVector();
            strVerDeVector.add(new ASN1Integer(32));//0x20-??????????????????
            DERSequence strVerSequence = new DERSequence(strVerDeVector);
            signSubtList.add(strVerSequence);
            /* 2.2 ????????????????????????ID??? */
            ASN1EncodableVector idDeVector = new ASN1EncodableVector();
            idDeVector.add(new DERPrintableString("6050700040"));//????????????????????????ID???
            DERSequence idSequence = new DERSequence(idDeVector);
            signSubtList.add(idSequence);
            /* 2.3 ?????????????????? */
            ASN1EncodableVector algorithmDeVector = new ASN1EncodableVector();
            algorithmDeVector.add(new ASN1ObjectIdentifier("1.2.840.113549.1.1.5"));//?????????????????? sha1RSA
            DERSequence algorithmSequence = new DERSequence(algorithmDeVector);
            signSubtList.add(algorithmSequence);
            /* 2.4 ???????????? */
            ASN1EncodableVector dateDeVectorDeVector = new ASN1EncodableVector();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String signDate = df.format(new Date());
            dateDeVectorDeVector.add(new DERPrintableString(signDate));//????????????
            DERSequence dateSequence = new DERSequence(dateDeVectorDeVector);
            signSubtList.add(dateSequence);
            /* 2.5 ???????????????????????? */
            ASN1EncodableVector hashVector = new ASN1EncodableVector();
            hashVector.add(new ASN1Integer(32));//????????????hash??????
            byte[] byteHash = {70, -114, 54, 84, -100, 114, -74, -1, 5, 55, 12, 111, -50, -75, 70, 93, -76, 65, 4, 42, 8, 35, -50, -61, -92, 56, 122, 31, -92, 71, 73, 96};
            String appHashValue = new String(byteHash);
            hashVector.add(new DERPrintableString(appHashValue));//????????????hash???
            DERSequence hashSequence = new DERSequence(hashVector);
            signSubtList.add(hashSequence);

            /* 2.6 ?????????????????? */
            ASN1EncodableVector extVector = new ASN1EncodableVector();
            /*DERSequence perSequenceHead = new DERSequence();
            extVector.add(perSequenceHead);//??????0300 ??????a3XX ????????????*/
            ASN1EncodableVector ext0Vector = new ASN1EncodableVector();//??????????????????
            ASN1EncodableVector perVector = new ASN1EncodableVector();//????????????????????????
            /* 2.6.1 ?????????????????? ???????????????????????? */
            ASN1EncodableVector perMarkVector = new ASN1EncodableVector();
            perMarkVector.add(new DERPrintableString("EPAY-FILE-DESC"));//????????????????????????
            DERSequence perMarkSequence = new DERSequence(perMarkVector);
            perVector.add(perMarkSequence);
            /* 2.6.2 ?????????????????? ???????????? */
            ASN1EncodableVector perInfoVector = new ASN1EncodableVector();
            perInfoVector.add(new ASN1Integer(permissionInfo().getBytes()));//??????????????????
            DERSequence perInfoSequence = new DERSequence(perInfoVector);
            perVector.add(perInfoSequence);
            DERSequence per0Sequence = new DERSequence(perVector);
            ext0Vector.add(per0Sequence);//????????????????????????????????????

            DERSequence ext0Sequence = new DERSequence(ext0Vector);
            extVector.add(ext0Sequence);//??????????????????????????????????????????

            DERSequence extSequence = new DERSequence(extVector);
            signSubtList.add(extSequence);//??????????????????

            DERSequence signSubSequence = new DERSequence(signSubtList);
            mainList.add(signSubSequence);//???????????????????????????????????????

            //3.????????????
            ASN1EncodableVector signInfoAllVector = new ASN1EncodableVector();
            signInfoAllVector.add(new DERBitString(0)); //BIT STRING ??????(03),????????? 71(47),??????0 ???????????????0(00)
            //?????????RS
            ASN1EncodableVector rsPerVector = new ASN1EncodableVector();
            byte[] byteRS = {70, -114, 54, 84, -100, 114, -74, -1, 5, 55, 12, 111, -50}; //64???byte
            rsPerVector.add(new ASN1Integer(byteRS));//?????????RS
            DERSequence rsSequence = new DERSequence(rsPerVector);
            signInfoAllVector.add(rsSequence);

            DERSequence signInfoAllSequence = new DERSequence(signInfoAllVector);
            mainList.add(signInfoAllSequence);//?????????????????????????????????

            //4.????????????
            //BIT STRING ??????(03),????????? 838(03 46),?????? 0 ??????????????? 0(00),/???
            String publicKeyHex = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100be7337da4178ebd330769c63cc133f91b3c419ded0b75b3f9251cbc7aa92c17b08944f7e14d84f2d7eaeef961024d64ccae9559f671add46772143736030dda649ecfa194ad0988c21661ba685b14c7585f798c9321825473d07c54cd6435818b3ceed41e1a6005c9c738aafd8c97aef79fda4fe757e1a03ccb6129c68b6b2eed7c4a3a2dfac7d7ebb93fc53605c5d95e78f2ab52fea09e795d599d93829f46c95f5a374ab9a9e3abc69de98e4c13c79902984ae59e17bfbb3139efaebe66d29129567fa7461c236239d2d196c7b03fc3cd5db7910089f78f7509db72b5a5966504f916f1fe62436f3a66f7261431ba51cfc47418b06c3c00bc2d24ce24820130203010001";
            ASN1EncodableVector certVector = new ASN1EncodableVector();
            certVector.add(new DERBitString(hexStringToByte(publicKeyHex)));

            DERSequence certSequence = new DERSequence(certVector);
            mainList.add(certSequence);//???????????????????????????

            DERSequence mainSequence = new DERSequence(mainList);//????????????Sequence
            derOutputStream.writeObject(mainSequence);
            derOutputStream.flush();
            //Log.i("VerityApkFile", "signatureInfoSubject---->: " + ApkUtils.bytesToHexString(outputStream.toByteArray()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outputStream;
    }

    /**
     * ????????????????????????String???
     *
     * @return
     */
    private static String permissionInfo() {
        String permissionInfo = null;
        InputStream is = null;
        try {
            is = new FileInputStream("/sdcard/permissions.xml");
            permissionInfo = new VerityApkFile(signAppName).readInfoStream(is);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Log.i("VerityApkFile", "permissionInfo---->: " + permissionInfo());
        return permissionInfo;
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

    public static int toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }

    /*??????????????????????????????????????????????????????app??????????????????????????????????????????????????????????????????ic??????picc printer pinpad???????????????
     *V2????????????????????????Android V2?????????????????????????????????????????????????????????
     */
    public static final String OID_PERMISSION = "2.5.29.99";

    private boolean verifyAPKDoubleV2Sign(String filePath) {
        if (filePath != null) {
            Log.i(TAG, "verifyAPKDoubleV2Sign filePath: " + filePath);
            try {
                X509Certificate[][] isV2Cert = ApkSignatureSchemeV2VerifierEx.verify(filePath);
                Log.i(TAG, "verifyAPKDoubleV2Sign isV2Cert: " + isV2Cert);
                if (isV2Cert != null && isV2Cert.length > 0) {
                    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                    Certificate caCer = certificateFactory.generateCertificate(new ByteArrayInputStream(acquirer_root));
                    Log.i(TAG, "verifyAPKDoubleV2Sign caCer: " + caCer);
                    PublicKey caKey = caCer.getPublicKey();
                    for (int i = 0; i < isV2Cert.length; i++) {
                        try {
                            isV2Cert[i][0].verify(caKey);
                            Log.i(TAG, "Signature V2: pass");
                            boolean checkPerm = true;
                            //SystemProperties.getBoolean("ro.perm.enforce", false);
                            /*if(checkPerm) {
                                try{
                                    byte[] perms = isV2Cert[i][0].getExtensionValue(OID_PERMISSION);
                                    if (perms != null && perms.length > 2) {
                                        int length = (int) perms[1];
                                        for (int k = 0; k < length; k++) {
                                            int indexPerm = (byte) (perms[k + 2] >> 3);
                                            if(indexPerm >= 0 && indexPerm < PosConstant.definitionPerms.length) {
                                                String permString = PosConstant.definitionPerms[indexPerm];
                                                if (pkg.requestedPermissions.indexOf(permString) == -1) {
                                                    pkg.requestedPermissions.add(permString.intern());
                                                    //pkg.requestedPermissionsRequired.add(Boolean.TRUE);
                                                }
                                            }
                                        }
                                    }
                                }catch(Exception e){
                                    //e.printStackTrace();
                                    Log.i(TAG, "parse Permissions V2: error");
                                }
                            }
                            for (String posPerm : pkg.requestedPOSPermissions) {
                                Log.d(TAG,"posPerm add to system--------------->"+posPerm);
                                if (pkg.requestedPermissions.indexOf(posPerm) == -1) {
                                    pkg.requestedPermissions.add(posPerm.intern());
                                }
                            }*/
                            return true;
                        } catch (Exception e) {
                            Log.e(TAG, "java.security.SignatureException V2: error");
                        }
                    }
                }
                Log.e(TAG, "SignatureSchemeV2 faile, return false");
            } catch (Exception e) {
                //e.printStackTrace();
                Log.e(TAG, "SignatureSchemeV2: error");
            }
            return false;
        }
        return false;
    }

    private static byte[] readFileByBytes(String fileName) {
        InputStream in = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            in = new java.io.FileInputStream(fileName);
            byte[] buf = new byte[1024];
            int length = 0;
            while ((length = in.read(buf)) != -1) {
                out.write(buf, 0, length);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return out.toByteArray();
    }


    public static String bytesToHexString(byte[] bytes) {
        return ((bytes == null) ? "null!" : bytesToHexString(bytes, bytes.length));
    }

    public static String bytesToHexString(byte[] bytes, int len) {
        return ((bytes == null) ? "null!" : bytesToHexString(bytes, 0, len));
    }

    public static String bytesToHexString(byte[] bytes, int offset, int len) {
        if (bytes == null)
            return "null!";

        StringBuilder ret = new StringBuilder(2 * len);

        for (int i = 0; i < len; ++i) {
            int b = 0xF & bytes[(offset + i)] >> 4;
            ret.append("0123456789abcdef".charAt(b));
            b = 0xF & bytes[(offset + i)];
            ret.append("0123456789abcdef".charAt(b));
        }

        return ret.toString();
    }

    /**
     * ASCII??????????????????????????????
     *
     * @param
     * @return ?????????
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
     * ?????????????????????????????????
     *
     * @param hex ?????????????????????
     * @return ???????????????
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

    public static String stringToAscii(String value) {
        StringBuffer sbu = new StringBuffer();
        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (i != chars.length - 1) {
                sbu.append((int) chars[i]).append(" ");
            } else {
                sbu.append((int) chars[i]);
            }
        }
        return sbu.toString();
    }

    @VisibleForTesting
    static String formatKernelVersion(Context context, StructUtsname uname) {
        if (uname == null) {
            return "Unavailable";
        }
        // Example:
        // 4.9.29-g958411d
        // #1 SMP PREEMPT Wed Jun 7 00:06:03 CST 2017
        final String VERSION_REGEX =
                "(#\\d+) " +              /* group 1: "#1" */
                        "(?:.*?)?" +              /* ignore: optional SMP, PREEMPT, and any CONFIG_FLAGS */
                        "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; /* group 2: "Thu Jun 28 11:02:39 PDT 2012" */
        Matcher m = Pattern.compile(VERSION_REGEX).matcher(uname.version);
        if (!m.matches()) {
            return "Unavailable";
        }

        // Example output:
        // 4.9.29-g958411d
        // #1 Wed Jun 7 00:06:03 CST 2017
        return new StringBuilder().append(uname.release)
                .append("\n")
                .append(m.group(1))
                .append(" ")
                .append(m.group(2)).toString();
    }

    /**
     * ????????????
     *
     * @param pathname
     * @return
     * @throws IOException
     */
    public boolean deleteFile(String pathname) {
        boolean result = false;
        File file = new File(pathname);
        Log.e(TAG, "deleteFile file.exists() = " + file.exists());
        if (file.exists()) {
            file.delete();
            result = true;
            Log.e(TAG, "deleteFile pathname = " + pathname);
        }
        return result;
    }

    private void bytes2File(byte[] buf, File file) throws Exception {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(buf);
        } catch (Exception e) {
            throw new Exception("bytes2File Error:" + e.toString());
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    throw new IOException("bytes2File Error:" + e.toString());
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    throw new IOException("bytes2File Error:" + e.toString());
                }
            }
        }
    }

}
