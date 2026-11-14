package com.parental.controller;

import android.content.Context;
import android.content.SharedPreferences;
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
}
