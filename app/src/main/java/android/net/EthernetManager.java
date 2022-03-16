package android.net;

public class EthernetManager {
    /*paxsz@2020.04.22 add start*/
    public static final String ETHERNET_IFACE_STATE_CHANGED_ACTION = "android.net.ethernet.ETHERNET_IFACE_STATE_CHANGED";
    public static final String EXTRA_ETHERNET_IFACE_STATE = "ethernet_iface_state";
    public static final int ETH_IFACE_STATE_DOWN = 0;
    public static final int ETH_IFACE_STATE_UP = 1;
    /*paxsz@2020.04.22 add end*/

    public void start() {
    }

    public void stop() {
    }

    public void updateIface(String var1, boolean var2) {
    }

    public void updateScoreFilter() {
    }

    public String getNetworkPriority() {
        return null;
    }

    public void setNetworkPriority(String hignName) {
    }

    public void setNetworkCoexist(boolean coexist) {
    }
}
