package com.parental.controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.VpnService;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int VPN_REQUEST = 401;
    private static final int SMS_REQUEST = 402;
    private EditText phoneInput;
    private EditText durationInput;
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
        scroll.setBackgroundColor(Color.parseColor("#F2F4F7"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 48, 40, 48);
        scroll.addView(layout);

        TextView title = new TextView(this);
        title.setText("Social Media Controller");
        title.setTextSize(24);
        title.setTextColor(Color.parseColor("#1B2A4A"));
        layout.addView(title);

        TextView explanation = new TextView(this);
        explanation.setText("Set a combined usage allowance per clock hour. When it's used up, selected apps' internet access is blocked until the next hour. This is a visible, parent-configured app.");
        explanation.setTextSize(14);
        explanation.setTextColor(Color.parseColor("#55607A"));
        LinearLayout.LayoutParams explp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        explp.topMargin = 12; explp.bottomMargin = 24;
        layout.addView(explanation, explp);

        addCard(layout, buildPhoneCard());
        addCard(layout, buildDurationCard());
        addCard(layout, buildAppsCard());
        addCard(layout, buildPinCard());
        addCard(layout, buildActionsCard());

        TextView note = new TextView(this);
        note.setText("Android requires a persistent notification while background monitoring is active. VPN and usage access are also visible in Android settings. SMS charges may apply.");
        note.setTextSize(12);
        note.setTextColor(Color.parseColor("#8A93A6"));
        LinearLayout.LayoutParams notelp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        notelp.topMargin = 16;
        layout.addView(note, notelp);

        setContentView(scroll);
    }

    private void addCard(LinearLayout parent, View card) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 20;
        parent.addView(card, lp);
    }

    private View buildPhoneCard() {
        LinearLayout card = cardLayout();
        card.addView(label("Parent's SMS number (with country code)"));
        phoneInput = new EditText(this);
        phoneInput.setSingleLine(true);
        phoneInput.setHint("+91XXXXXXXXXX");
        phoneInput.setBackgroundResource(R.drawable.bg_edittext);
        phoneInput.setText(AppPrefs.phone(this));
        card.addView(phoneInput, fieldParams());
        return card;
    }

    private View buildDurationCard() {
        LinearLayout card = cardLayout();
        card.addView(label("Allowed combined usage per hour (minutes)"));
        durationInput = new EditText(this);
        durationInput.setSingleLine(true);
        durationInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        durationInput.setBackgroundResource(R.drawable.bg_edittext);
        durationInput.setText(String.valueOf(AppPrefs.allowanceMinutes(this)));
        card.addView(durationInput, fieldParams());
        return card;
    }

    private View buildAppsCard() {
        LinearLayout card = cardLayout();
        card.addView(label("Apps to monitor"));
        Set<String> selected = AppPrefs.selected(this);
        for (Map.Entry<String, String> entry : apps.entrySet()) {
            CheckBox cb = new CheckBox(this);
            boolean installed = isInstalled(entry.getValue());
            cb.setChecked(selected.isEmpty() ? installed : selected.contains(entry.getValue()));
            cb.setEnabled(installed);
            cb.setText(installed ? entry.getKey() : entry.getKey() + " (not installed)");
            checks.put(entry.getValue(), cb);
            card.addView(cb);
        }
        return card;
    }

    private View buildPinCard() {
        LinearLayout card = cardLayout();
        card.addView(label("App lock"));
        TextView status = new TextView(this);
        status.setText(AppPrefs.hasPin(this) ? "A PIN is set. Required to open the app and to stop monitoring."
                : "No PIN set. Anyone who opens this app can change settings or stop monitoring.");
        status.setTextSize(13);
        status.setTextColor(Color.parseColor("#55607A"));
        card.addView(status);

        Button pinButton = primaryButton(AppPrefs.hasPin(this) ? "Change PIN" : "Set a PIN");
        card.addView(pinButton, buttonParams());
        pinButton.setOnClickListener(v -> promptSetPin());
        return card;
    }

    private View buildActionsCard() {
        LinearLayout card = cardLayout();

        Button save = primaryButton("Save settings");
        card.addView(save, buttonParams());
        save.setOnClickListener(v -> saveSettings());

        Button permissions = secondaryButton("Grant required permissions");
        card.addView(permissions, buttonParams());
        permissions.setOnClickListener(v -> requestSetupPermissions());

        Button start = primaryButton("Start monitoring");
        card.addView(start, buttonParams());
        start.setOnClickListener(v -> {
            saveSettings();
            if (!hasUsageAccess()) {
                new AlertDialog.Builder(this).setMessage("Please enable Usage access for Social Media Controller, then return and tap Start monitoring.")
                        .setPositiveButton("Open settings", (d, w) -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))).show();
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

        Button stop = secondaryButton("Stop monitoring");
        card.addView(stop, buttonParams());
        stop.setOnClickListener(v -> requirePinThen(this::stopMonitoring));

        return card;
    }

    private void stopMonitoring() {
        AppPrefs.setEnabled(this, false);
        stopService(new Intent(this, MonitorService.class));
        stopService(new Intent(this, SelectiveVpnService.class));
        Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show();
    }

    private void requirePinThen(Runnable action) {
        if (!AppPrefs.hasPin(this)) { action.run(); return; }
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter PIN");
        new AlertDialog.Builder(this).setTitle("PIN required").setView(input)
                .setPositiveButton("Confirm", (d, w) -> {
                    if (AppPrefs.checkPin(this, input.getText().toString())) action.run();
                    else Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void promptSetPin() {
        requirePinThen(() -> {
            EditText newPin = new EditText(this);
            newPin.setHint("New 4-6 digit PIN");
            newPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            new AlertDialog.Builder(this).setTitle("Set app lock PIN").setView(newPin)
                    .setPositiveButton("Save", (d, w) -> {
                        String pin = newPin.getText().toString();
                        if (pin.length() < 4) {
                            Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AppPrefs.setPin(this, pin);
                        Toast.makeText(this, "PIN saved", Toast.LENGTH_SHORT).show();
                        recreate();
                    })
                    .setNegativeButton("Cancel", null).show();
        });
    }

    private LinearLayout cardLayout() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(32, 28, 32, 28);
        return card;
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(15);
        t.setTextColor(Color.parseColor("#1B2A4A"));
        return t;
    }

    private Button primaryButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setBackgroundResource(R.drawable.bg_button_primary);
        btn.setTextColor(Color.WHITE);
        return btn;
    }

    private Button secondaryButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setBackgroundResource(R.drawable.bg_button_secondary);
        btn.setTextColor(Color.parseColor("#1B2A4A"));
        return btn;
    }

    private LinearLayout.LayoutParams fieldParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 12;
        return lp;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 14;
        return lp;
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
        int minutes = 5;
        try { minutes = Integer.parseInt(durationInput.getText().toString().trim()); } catch (Exception ignored) {}
        AppPrefs.setAllowanceMinutes(this, minutes);
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private boolean hasUsageAccess() {
        AppOpsManager ops = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
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
        AppPrefs.setMonitorStartTime(this, System.currentTimeMillis());
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
