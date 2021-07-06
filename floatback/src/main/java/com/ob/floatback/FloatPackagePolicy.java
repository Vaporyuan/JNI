package com.ob.floatback;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class FloatPackagePolicy {
    private static final String TAG = "FloatPackagePolicy";
    private static FileInputStream is = null;
    private static ByteArrayOutputStream os = null;
    private static final boolean DEBUG = false;

    private static String vang_policy = "{\n" +
            "\t\"whitePackages\": [ \n" +
            "\t\t\"com.android.settings\"],\n" +
            "\t\"blackPackages\": [ \n" +
            "\t\t\"com.ob.fangchai\"\n" +
            "\t]\n" +
            "}";

    private static JSONObject json = null;

    static {
        File file = new File("/system/etc/float_back_list");
        //File file = new File("/sdcard/float_back_list");
        if (!file.exists()) {
            Log.i(TAG, "ant_policy use new vendor context:" + vang_policy);
        }
        String content = "";
        if (file.isFile() && file.exists()) {
            try {
                is = new FileInputStream(file);
                os = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len = 0;
                while ((len = is.read(buf)) != -1) {
                    os.write(buf, 0, len);
                }
                byte[] data = os.toByteArray();
                content = new String(data);
                vang_policy = content.trim();
                Log.e(TAG, "vang_policy use new context:" + vang_policy);
            } catch (IOException e) {
                Log.e(TAG, "Can't read: vang policy" + e.toString());
            } catch (Exception e) {
                Log.e(TAG, "Can't read ex: vang policy" + e.toString());
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        try {
            json = new JSONObject(vang_policy);
        } catch (JSONException e) {
            Log.e(TAG, "vang_policy json err!");
        }

        Log.e(TAG, "vang_policy init okey!");
    }

    public static List<String> readWhitePackages() {
        List<String> list = new ArrayList<String>() {
        };

        try {
            //JSONObject json = new JSONObject(vang_policy);
            if (DEBUG)
                Log.d(TAG, "readWhitePackages:" + json.getJSONArray("whitePackages").toString());
            JSONArray jarr = json.getJSONArray("whitePackages");
            for (int i = 0; i < jarr.length(); i++) {
                if (DEBUG) Log.d(TAG, "whitePackages:" + i + ":" + jarr.get(i));
                list.add(((String) jarr.get(i)).trim());
            }
        } catch (JSONException e) {
            //e.printStackTrace();
            Log.d(TAG, "whitePackages JSONException!");
        }

        return list;

    }

    public static List<String> readBlackPackages() {
        List<String> list = new ArrayList<String>() {
        };
        try {
            //JSONObject json = new JSONObject(vang_policy);
            if (DEBUG) Log.d(TAG, "blackPackages:" + json.getJSONArray("blackPackages").toString());
            JSONArray jarr = json.getJSONArray("blackPackages");
            for (int i = 0; i < jarr.length(); i++) {
                if (DEBUG) Log.d(TAG, "blackPackages:" + i + ":" + jarr.get(i));
                list.add(((String) jarr.get(i)).trim());
            }
        } catch (JSONException e) {
            //e.printStackTrace();
            if (DEBUG) Log.d(TAG, "blackPackages JSONException!");
        }
        return list;
    }

    public static boolean isWhitePackages(String packageName) {
        List<String> whitePackages = readWhitePackages();
        return whitePackages.contains(packageName);
    }

    public static boolean isBlackPackages(String packageName) {
        List<String> blackPackages = readBlackPackages();
        return blackPackages.contains(packageName);
    }
}
