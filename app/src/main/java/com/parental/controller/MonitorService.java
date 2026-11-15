package com.parental.controller;

import android.app.*;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.*;
import android.os.*;
import android.telephony.SmsManager;
import android.Manifest;
import android.content.pm.PackageManager;
import java.text.SimpleDateFormat;
import java.util.*;

public class MonitorService extends Service {
    private static final String CHANNEL = "usage_monitor";
    private static final int NOTIFICATION_ID = 1001;
    private Handler handler;
    private long currentHourStart;
    private long windowStart;
    private boolean blocked = false;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!AppPrefs.enabled(MonitorService.this)) { stopSelf(); return; }
            long now = System.currentTimeMillis();
            long hourStart = now - (now % 3600000L);
            if (hourStart != currentHourStart) {
                if (currentHourStart != 0) sendReport(windowStart, hourStart);
                currentHourStart = hourStart;
                windowStart = hourStart;
                blocked = false;
                stopService(new Intent(MonitorService.this, SelectiveVpnService.class));
            }
            long allowanceMs = AppPrefs.allowanceMinutes(MonitorService.this) * 60 * 1000L;
            long usage = getCombinedUsage(windowStart, now);
            if (usage >= allowanceMs && !blocked) {
                blocked = true;
                Intent vpn = new Intent(MonitorService.this, SelectiveVpnService.class);
                vpn.setAction(SelectiveVpnService.ACTION_BLOCK);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(vpn); else startService(vpn);
            }
            updateNotification(usage, allowanceMs);
            handler.postDelayed(this, 15000);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createChannel();
        startForeground(NOTIFICATION_ID, notification("Monitoring social media usage"));
        long now = System.currentTimeMillis();
        currentHourStart = now - (now % 3600000L);
        // Only count usage from when monitoring was actually turned on, so a child
        // who already used an app earlier in the clock hour (before Start monitoring
        // was tapped) isn't blocked immediately.
        long monitorStart = AppPrefs.monitorStartTime(this);
        windowStart = Math.max(currentHourStart, monitorStart);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        handler.removeCallbacks(tick);
        handler.post(tick);
        return START_STICKY;
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Try to come back if the app is swiped away from Recents while monitoring
        // is on. START_STICKY already asks Android to restart the service, this
        // just gives it an immediate nudge.
        if (AppPrefs.enabled(this)) {
            Intent restart = new Intent(getApplicationContext(), MonitorService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(restart); else startService(restart);
        }
    }

    private long getCombinedUsage(long start, long end) {
        if (end <= start) return 0;
        UsageStatsManager manager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        Map<String, UsageStats> stats = manager.queryAndAggregateUsageStats(start, end);
        long total = 0;
        for (String pkg : AppPrefs.selected(this)) {
            UsageStats s = stats.get(pkg);
            if (s != null) total += s.getTotalTimeInForeground();
        }
        return total;
    }

    private void sendReport(long start, long end) {
        String number = AppPrefs.phone(this);
        if (number.isEmpty() || checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return;
        long used = getCombinedUsage(start, end);
        long allowanceMs = AppPrefs.allowanceMinutes(this) * 60 * 1000L;
        StringBuilder report = new StringBuilder("Social media usage ");
        SimpleDateFormat fmt = new SimpleDateFormat("h:mm a", Locale.getDefault());
        report.append(fmt.format(new Date(start))).append(" - ").append(fmt.format(new Date(end))).append("\n");
        for (String pkg : AppPrefs.selected(this)) {
            long appUsage = 0;
            UsageStats s = ((UsageStatsManager) getSystemService(USAGE_STATS_SERVICE))
                    .queryAndAggregateUsageStats(start, end).get(pkg);
            if (s != null) appUsage = s.getTotalTimeInForeground();
            report.append(label(pkg)).append(": ").append(appUsage / 60000).append("m ")
                    .append((appUsage % 60000) / 1000).append("s\n");
        }
        report.append("Combined: ").append(used / 60000).append("m ").append((used % 60000) / 1000)
                .append("s. Limit: ").append(allowanceMs / 60000).append("m. ")
                .append(used >= allowanceMs ? "Limit exceeded." : "Within limit.");
        try {
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(report.toString());
            sms.sendMultipartTextMessage(number, null, parts, null, null);
        } catch (Exception ignored) { }
    }

    private String label(String pkg) {
        if (pkg.equals("com.instagram.android")) return "Instagram";
        if (pkg.equals("com.facebook.katana")) return "Facebook";
        if (pkg.equals("com.google.android.youtube")) return "YouTube";
        if (pkg.equals("com.snapchat.android")) return "Snapchat";
        if (pkg.equals("com.whatsapp")) return "WhatsApp";
        if (pkg.equals("com.twitter.android")) return "X";
        if (pkg.equals("com.zhiliaoapp.musically")) return "TikTok";
        if (pkg.equals("com.reddit.frontpage")) return "Reddit";
        return pkg;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Usage monitoring", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private Notification notification(String text) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setContentTitle("Social Media Controller").setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock).setOngoing(true).build();
    }

    private void updateNotification(long usage, long allowanceMs) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification("Combined usage this hour: " + (usage / 60000) + "m " +
                ((usage % 60000) / 1000) + "s / " + (allowanceMs / 60000) + "m" + (blocked ? " — internet blocked" : "")));
    }

    @Override public void onDestroy() {
        if (handler != null) handler.removeCallbacks(tick);
        super.onDestroy();
    }

    @Override public android.os.IBinder onBind(Intent intent) { return null; }
}
