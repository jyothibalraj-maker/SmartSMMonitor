package com.parental.controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && AppPrefs.enabled(context)) {
            Intent service = new Intent(context, MonitorService.class);
            try {
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service);
                else context.startService(service);
            } catch (Exception ignored) {
                // Android 12+ and some OEMs may prevent background service startup.
                // User can reopen the app and tap Start monitoring.
            }
        }
    }
}
