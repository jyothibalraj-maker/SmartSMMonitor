package com.parental.controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int VPN_REQUEST = 401;
    private static final int SMS_REQUEST = 402;
    private EditText phoneInput;
    private final Map<String, String> apps = new LinkedHashMap<>();
    private final Map<String, CheckBox> checks = new LinkedHashMap<>();

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        apps.put("Instagram", "com.instagram.android");
        apps.put("Facebook", "com.facebook.katana");
        apps.put("YouTube", "com.google.android.youtube");
        apps.put("Snapchat", "com.snapchat.android");
        apps.put("WhatsApp", "com.whatsapp");
        apps.put("X (Twitter)", "com.twitter.android");
        apps.put("TikTok", "com.zhiliaoapp.musically");
        apps.put("Reddit", "com.reddit.frontpage");

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        scroll.addView(layout);

        TextView title = new TextView(this);
        title.setText("Social Media Controller");
        title.setTextSize(24);
        layout.addView(title);

        TextView explanation = new TextView(this);
        explanation.setText("Set a combined 5-minute allowance per clock hour. When exceeded, selected apps' internet traffic is blocked until the next hour. This is a visible parental-control app.");
        explanation.setTextSize(15);
        layout.addView(explanation);

        TextView phoneLabel = new TextView(this);
        phoneLabel.setText("\nParent's SMS number (include country code):");
        layout.addView(phoneLabel);
        phoneInput = new EditText(this);
        phoneInput.setSingleLine(true);
        phoneInput.setHint("+91XXXXXXXXXX");
        phoneInput.setText(AppPrefs.phone(this));
        layout.addView(phoneInput);

        TextView appLabel = new TextView(this);
        appLabel.setText("\nSelect applications to monitor:");
        layout.addView(appLabel);

        Set<String> selected = AppPrefs.selected(this);
        for (Map.Entry<String, String> entry : apps.entrySet()) {
            CheckBox cb = new CheckBox(this);
            cb.setText(entry.getKey());
            cb.setChecked(selected.isEmpty() ? isInstalled(entry.getValue()) : selected.contains(entry.getValue()));
            cb.setEnabled(isInstalled(entry.getValue()));
            if (!isInstalled(entry.getValue())) cb.setText(entry.getKey() + " (not installed)");
            checks.put(entry.getValue(), cb);
            layout.addView(cb);
        }

        Button save = new Button(this);
        save.setText("Save settings");
        layout.addView(save);
        save.setOnClickListener(v -> saveSettings());

        Button permissions = new Button(this);
        permissions.setText("Grant required permissions");
        layout.addView(permissions);
        permissions.setOnClickListener(v -> requestSetupPermissions());

        Button start = new Button(this);
        start.setText("Start monitoring");
        layout.addView(start);
        start.setOnClickListener(v -> {
            saveSettings();
            if (!hasUsageAccess()) {
                new AlertDialog.Builder(this).setMessage("Please enable Usage access for Social Media Controller, then return and tap Start monitoring.")
                    .setPositiveButton("Open settings", (d,w) -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))).show();
                return;
            }
            if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, SMS_REQUEST);
                return;
            }
            Intent vpn = VpnService.prepare(this);
            if (vpn != null) startActivityForResult(vpn, VPN_REQUEST);
            else startMonitor();
        });

        Button stop = new Button(this);
        stop.setText("Stop monitoring");
        layout.addView(stop);
        stop.setOnClickListener(v -> {
            AppPrefs.setEnabled(this, false);
            stopService(new Intent(this, MonitorService.class));
            stopService(new Intent(this, SelectiveVpnService.class));
            Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show();
        });

        TextView note = new TextView(this);
        note.setText("\nAndroid requires a persistent system notification while background monitoring is active. VPN and usage access are also visible in Android settings. SMS charges may apply.");
        note.setTextSize(12);
        layout.addView(note);
        setContentView(scroll);
    }

    private boolean isInstalled(String pkg) {
        try { getPackageManager().getPackageInfo(pkg, 0); return true; }
        catch (Exception e) { return false; }
    }

    private void saveSettings() {
        AppPrefs.setPhone(this, phoneInput.getText().toString());
        Set<String> chosen = new HashSet<>();
        for (Map.Entry<String, CheckBox> e : checks.entrySet())
            if (e.getValue().isChecked()) chosen.add(e.getKey());
        AppPrefs.setSelected(this, chosen);
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private boolean hasUsageAccess() {
        AppOpsManager ops = (AppOpsManager)getSystemService(Context.APP_OPS_SERVICE);
        int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void requestSetupPermissions() {
        if (!hasUsageAccess()) startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, SMS_REQUEST);
    }

    private void startMonitor() {
        AppPrefs.setEnabled(this, true);
        Intent i = new Intent(this, MonitorService.class);
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        Toast.makeText(this, "Monitoring started", Toast.LENGTH_LONG).show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST && resultCode == RESULT_OK) startMonitor();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == SMS_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
            Toast.makeText(this, "SMS permission granted. Tap Start monitoring.", Toast.LENGTH_SHORT).show();
    }
}
