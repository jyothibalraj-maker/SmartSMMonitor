package com.parental.controller;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

public final class AppPrefs {
    private static final String PREFS = "parental_controller";
    private AppPrefs() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String phone(Context c) {
        return prefs(c).getString("parent_phone", "");
    }

    static void setPhone(Context c, String number) {
        prefs(c).edit().putString("parent_phone", number.trim()).apply();
    }

    static Set<String> selected(Context c) {
        return new HashSet<>(prefs(c).getStringSet("selected_apps", new HashSet<String>()));
    }

    static void setSelected(Context c, Set<String> packages) {
        prefs(c).edit().putStringSet("selected_apps", new HashSet<>(packages)).apply();
    }

    static boolean enabled(Context c) {
        return prefs(c).getBoolean("monitor_enabled", false);
    }

    static void setEnabled(Context c, boolean enabled) {
        prefs(c).edit().putBoolean("monitor_enabled", enabled).apply();
    }

    // Configurable allowance, in minutes, before blocking starts. Default 5.
    static int allowanceMinutes(Context c) {
        return prefs(c).getInt("allowance_minutes", 5);
    }

    static void setAllowanceMinutes(Context c, int minutes) {
        prefs(c).edit().putInt("allowance_minutes", Math.max(1, minutes)).apply();
    }

    // Timestamp (millis) when monitoring was last started. Used so usage from
    // before monitoring was turned on this hour doesn't count toward the limit.
    static long monitorStartTime(Context c) {
        return prefs(c).getLong("monitor_start_time", 0L);
    }

    static void setMonitorStartTime(Context c, long time) {
        prefs(c).edit().putLong("monitor_start_time", time).apply();
    }

    // App-lock PIN, stored only as a SHA-256 hash, never in plain text.
    static boolean hasPin(Context c) {
        return prefs(c).contains("pin_hash");
    }

    static void setPin(Context c, String pin) {
        prefs(c).edit().putString("pin_hash", hash(pin)).apply();
    }

    static void clearPin(Context c) {
        prefs(c).edit().remove("pin_hash").apply();
    }

    static boolean checkPin(Context c, String pin) {
        String stored = prefs(c).getString("pin_hash", null);
        return stored != null && stored.equals(hash(pin));
    }

    private static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
