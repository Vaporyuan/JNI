package com.example.demo.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by josin on 17-3-10.
 */

public class PosConstant {
    // 使用密码键盘设备功能的权限
    public static final String PERMISSION_PINPAD = "smartpos.deviceservice.permission.Pinpad";
    public static final int OID_PINPAD = 0;
    // 使用磁条读卡器设备功能的权限
    public static final String PERMISSION_MAGREADER = "smartpos.deviceservice.permission.MagReader";
    public static final int OID_MAGREADER = OID_PINPAD + 1;
    // 使用 IC 卡读卡器设备功能的权限
    public static final String PERMISSION_ICREADER = "smartpos.deviceservice.permission.ICReader";
    public static final int OID_ICREADER = OID_PINPAD + 2;
    // 使用非接触 IC 卡读卡器设备功能的权限
    public static final String PERMISSION_RFREADER = "smartpos.deviceservice.permission.RFReader";
    public static final int OID_RFREADER = OID_PINPAD + 3;
    // 使用打印机设备功能的权限
    public static final String PERMISSION_PRINTER = "smartpos.deviceservice.permission.Printer";
    public static final int OID_PRINTER = OID_PINPAD + 4;
    // 使用条码扫描设备功能的权限
    public static final String PERMISSION_SCANNER = "smartpos.deviceservice.permission.Scanner";
    public static final int OID_SCANNER = OID_PINPAD + 5;
    // 使用钱箱设备功能的权限
    public static final String PERMISSION_CASHBOX = "smartpos.deviceservice.permission.CashBox";
    public static final int OID_CASHBOX = OID_PINPAD + 6;
    // 使用 Modem 设备功能的权限
    public static final String PERMISSION_MODEM = "smartpos.deviceservice.permission.Modem";
    public static final int OID_MODEM = OID_PINPAD + 7;
    // 使用二代证设备读卡功能的权限
    public static final String PERMISSION_SAMV = "smartpos.deviceservice.permission.SAMV";
    public static final int OID_SAMV = OID_PINPAD + 8;
    // 使用蜂鸣器设备功能的权限
    public static final String PERMISSION_BEEPER = "smartpos.deviceservice.permission.Beeper";
    public static final int OID_BEEPER = OID_PINPAD + 9;
    // 调用 PBOC 金融交互流程功能的权限
    public static final String PERMISSION_PBOC = "smartpos.deviceservice.permission.PBOC";
    public static final int OID_PBOC = OID_PINPAD + 10;
    // 调用获取终端设备信息的权限
    public static final String PERMISSION_DEVICEINFO = "smartpos.deviceservice.permission.DeviceInfo";
    public static final int OID_DEVICEINFO = OID_PINPAD + 11;
    // 使用串口设备功能的权限
    public static final String PERMISSION_SERIALPORT = "smartpos.deviceservice.permission.SerialPort";
    public static final int OID_SERIALPORT = OID_PINPAD + 12;
    // 使用 LED 灯设备功能的权限
    public static final int OID_LED = OID_PINPAD + 13;
    public static final String PERMISSION_LED = "smartpos.deviceservice.permission.Led";
    public static final String PERMISSION_DEVICE_LED = "smartpos.deviceservice.permission.LED";
    public static final HashSet<String> definitionAPPPerms = new HashSet<String>() {{
        add(PosConstant.PERMISSION_PINPAD);
        add(PosConstant.PERMISSION_MAGREADER);
        add(PosConstant.PERMISSION_ICREADER);
        add(PosConstant.PERMISSION_RFREADER);
        add(PosConstant.PERMISSION_PRINTER);
        add(PosConstant.PERMISSION_SCANNER);
        add(PosConstant.PERMISSION_CASHBOX);
        add(PosConstant.PERMISSION_MODEM);
        add(PosConstant.PERMISSION_SAMV);
        add(PosConstant.PERMISSION_BEEPER);
        add(PosConstant.PERMISSION_PBOC);
        add(PosConstant.PERMISSION_DEVICEINFO);
        add(PosConstant.PERMISSION_SERIALPORT);
        add(PosConstant.PERMISSION_DEVICE_LED);
    }};
    public static final String[] definitionPerms = new String[] {PERMISSION_PINPAD,
        PERMISSION_MAGREADER,
        PERMISSION_ICREADER,
        PERMISSION_RFREADER,
        PERMISSION_PRINTER,
        PERMISSION_SCANNER,
        PERMISSION_CASHBOX,
        PERMISSION_MODEM,
        PERMISSION_SAMV,
        PERMISSION_BEEPER,
        PERMISSION_PBOC,
        PERMISSION_DEVICEINFO,
        PERMISSION_SERIALPORT,
        PERMISSION_DEVICE_LED
        };

    public static Map<Integer, String> OID_TO_PERMISSION = new HashMap<Integer, String>(){{
        put(OID_PINPAD, PERMISSION_PINPAD);
        put(OID_MAGREADER, PERMISSION_MAGREADER);
        put(OID_ICREADER, PERMISSION_ICREADER);
        put(OID_RFREADER, PERMISSION_RFREADER);
        put(OID_PRINTER, PERMISSION_PRINTER);
        put(OID_SCANNER, PERMISSION_SCANNER);
        put(OID_CASHBOX, PERMISSION_CASHBOX);
        put(OID_MODEM, PERMISSION_MODEM);
        put(OID_SAMV, PERMISSION_SAMV);
        put(OID_BEEPER, PERMISSION_BEEPER);
        put(OID_PBOC, PERMISSION_PBOC);
        put(OID_DEVICEINFO, PERMISSION_DEVICEINFO);
        put(OID_SERIALPORT, PERMISSION_SERIALPORT);
        put(OID_LED, PERMISSION_LED);
    }};



}
