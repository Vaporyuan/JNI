package android.net;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class EthernetShareUtil {
    public static boolean doEthernetShare(String dns) {
        if (TextUtils.isEmpty(dns)) return false;
        DataOutputStream dataOutputStream = null;
        BufferedReader errorStream = null;
        try {
            Process process = Runtime.getRuntime().exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            String commandStr = "mount -o rw,remount / " + " \n" +
                    "mount -o rw,remount /system " + " \n" +
                    "mkdir -p /var/lib/misc/" + " \n" +
                    "touch /var/lib/misc/udhcpd.leases" + " \n" +
                    "echo 0 > /proc/sys/net/ipv4/ip_forward" + " \n" +
                    "ifconfig eth0 192.168.1.123 netmask 255.255.255.0" + " \n" +
                    "ndc tether interface add eth0" + " \n" +
                    "ndc tether start 192.168.1.2 192.168.1.254" + " \n" +
                    //"ip rule add from all lookup main pref 9999" + " \n" +
                    "ndc nat enable eth0 ccmni0 2 192.168.1.133/32" + " \n" +
                    "echo 1 > /proc/sys/net/ipv4/ip_forward" + " \n" +
                    "iptables -t nat -I PREROUTING -i eth0  -p udp --dport 53 -j DNAT --to-destination " + dns + " \n";


            dataOutputStream.write(commandStr.getBytes(Charset.forName("utf-8")));
            dataOutputStream.flush();
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            process.waitFor();
            errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String msg = "";
            String line;
            // 读取命令的执行结果
            while ((line = errorStream.readLine()) != null) {
                msg += line;
            }
            System.out.println(msg);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (errorStream != null) {
                    errorStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public String getDns() {
        return "8.8.8.8";
    }


    //https://blog.csdn.net/u010559573/article/details/110130229
    public String doEthernetShareS() {

        String result = "Failure";
        DataOutputStream dataOutputStream = null;
        BufferedReader errorStream = null;
        //String networkType = getNetworkType();
        try {
            //Log.d(TAG,"doEthernetShareS.networkType=" + networkType);
            Process process = Runtime.getRuntime().exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            String commandStr =
                    "ndc tether stop " + " \n" +
                            //"sleep 1" + " \n" +
                            "ip rule add from all lookup main pref 9999 " + " \n" +
                            //"ifconfig eth0 down " + " \n" +
                            //"sleep 1" + " \n" +
                            "ifconfig eth0 up" + " \n" +
                            //"sleep 1" + " \n" +
                            "busybox ifconfig eth0 192.168.43.1 " + " \n" +
                            "ndc netd 5003 tether start 192.168.43.2 192.168.43.254" + " \n"  +
                            "ndc netd 7 nat enable eth0 ppp0 2 10.6.194.114/24" + " \n"  +
                            "echo 1 >/proc/sys/net/ipv4/ip_forward" + " \n"  +
                            //"iptables -t nat -I PREROUTING -i eth0 -p udp --dport 53 -j DNAT --to-destination 8.8.8.8" + " \n" +
                            //"ndc tether stop " + " \n" +
                            //"busybox ifconfig eth0 192.168.43.1 " + " \n" +
                            //"ndc netd 5003 tether start 192.168.43.2 192.168.43.254" + " \n"  +
                            //"ndc netd 7 nat enable eth0 ppp0 2 10.6.194.114/24" + " \n"  +
                            //"echo 1 >/proc/sys/net/ipv4/ip_forward" + " \n"  +
                            "iptables -t nat -I PREROUTING -i eth0 -p udp --dport 53 -j DNAT --to-destination 8.8.8.8" + " \n";

            dataOutputStream.write(commandStr.getBytes(Charset.forName("utf-8")));
            dataOutputStream.flush();
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            process.waitFor();
            errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String msg = "";
            String line;
            // 读取命令的执行结果
            while ((line = errorStream.readLine()) != null) {
                msg += line;
            }
            //Log.d(TAG,"msg" +msg);
            result = msg;
        } catch (Exception e) {
            //Log.e(TAG, e.getMessage(), e);
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (errorStream != null) {
                    errorStream.close();
                }
            } catch (IOException e) {
                //Log.e(TAG, e.getMessage(), e);
            }
        }

        return result;

    }

}
