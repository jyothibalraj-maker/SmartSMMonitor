package com.parental.controller;

import android.net.VpnService;
import android.os.*;
import android.app.*;
import android.content.Intent;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

public class SelectiveVpnService extends VpnService {
    public static final String ACTION_BLOCK = "com.parental.controller.BLOCK";
    private static final String CHANNEL = "social_vpn";
    private ParcelFileDescriptor tunnel;
    private Thread drainThread;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(2002, buildNotification());
        if (intent != null && ACTION_BLOCK.equals(intent.getAction())) establishBlockingTunnel();
        else stopBlockingTunnel();
        return START_STICKY;
    }

    private void establishBlockingTunnel() {
        if (tunnel != null) return;
        try {
            Builder builder = new Builder()
                    .setSession("Social Media Controller")
                    .addAddress("10.10.10.1", 32)
                    .addRoute("0.0.0.0", 0)
                    .setMtu(1500);
            Set<String> selected = AppPrefs.selected(this);
            if (selected.isEmpty()) { stopSelf(); return; }
            for (String pkg : selected) {
                try { builder.addAllowedApplication(pkg); }
                catch (Exception ignored) { }
            }
            tunnel = builder.establish();
            if (tunnel == null) { stopSelf(); return; }
            final FileInputStream in = new FileInputStream(tunnel.getFileDescriptor());
            drainThread = new Thread(() -> {
                byte[] packet = new byte[32767];
                try {
                    while (!Thread.currentThread().isInterrupted() && tunnel != null) {
                        int n = in.read(packet);
                        if (n < 0) break;
                        // Intentionally do not write packets back: selected apps have no network route.
                    }
                } catch (IOException ignored) { }
            }, "VpnPacketDropper");
            drainThread.start();
        } catch (Exception e) {
            stopBlockingTunnel();
        }
    }

    private void stopBlockingTunnel() {
        try { if (tunnel != null) tunnel.close(); } catch (Exception ignored) { }
        tunnel = null;
        if (drainThread != null) drainThread.interrupt();
        drainThread = null;
        stopForeground(true);
        stopSelf();
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Social app network restriction", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setContentTitle("Social Media Controller")
                .setContentText("Selected social media internet access is restricted")
                .setSmallIcon(android.R.drawable.ic_lock_lock).setOngoing(true).build();
    }

    @Override public void onRevoke() {
        stopBlockingTunnel();
        super.onRevoke();
    }

    @Override public void onDestroy() {
        stopBlockingTunnel();
        super.onDestroy();
    }

    @Override public android.os.IBinder onBind(Intent intent) { return super.onBind(intent); }
}
