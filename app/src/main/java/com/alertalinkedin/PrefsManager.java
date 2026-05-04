package com.alertalinkedin;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {
    private static final String PREFS = "alerta_prefs";
    private static final String KEY_LOCATION   = "location";
    private static final String KEY_INTERVAL   = "interval_minutes";
    private static final String KEY_MONITORING = "monitoring_active";
    private static final String KEY_TIME_RANGE = "time_range";

    public static String getLocation(Context ctx) {
        return prefs(ctx).getString(KEY_LOCATION, "Brasil");
    }

    public static void setLocation(Context ctx, String location) {
        prefs(ctx).edit().putString(KEY_LOCATION, location).apply();
    }

    public static long getIntervalMinutes(Context ctx) {
        return prefs(ctx).getLong(KEY_INTERVAL, 5);
    }

    public static void setIntervalMinutes(Context ctx, long minutes) {
        prefs(ctx).edit().putLong(KEY_INTERVAL, minutes).apply();
    }

    public static String getTimeRange(Context ctx) {
        return prefs(ctx).getString(KEY_TIME_RANGE, "r604800");
    }

    public static void setTimeRange(Context ctx, String range) {
        prefs(ctx).edit().putString(KEY_TIME_RANGE, range).apply();
    }

    public static boolean isMonitoringActive(Context ctx) {
        return prefs(ctx).getBoolean(KEY_MONITORING, false);
    }

    public static void setMonitoringActive(Context ctx, boolean active) {
        prefs(ctx).edit().putBoolean(KEY_MONITORING, active).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
