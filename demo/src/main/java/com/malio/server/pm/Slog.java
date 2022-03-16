package com.malio.server.pm;

import android.util.Log;

public class Slog {
    public static void i(String tag, String info) {
        Log.i(tag, info);
    }

    public static void d(String tag, String info) {
        Log.d(tag, info);
    }

    public static void e(String tag, String info) {
        Log.e(tag, info);
    }
}
