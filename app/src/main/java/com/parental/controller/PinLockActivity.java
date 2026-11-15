package com.parental.controller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class PinLockActivity extends Activity {
    private EditText pinInput;
    private TextView error;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AppPrefs.hasPin(this)) {
            openMain();
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#F2F4F7"));
        layout.setPadding(64, 64, 64, 64);

        TextView title = new TextView(this);
        title.setText("Social Media Controller");
        title.setTextSize(20);
        title.setTextColor(Color.parseColor("#1B2A4A"));
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Enter PIN to continue");
        sub.setTextSize(14);
        sub.setTextColor(Color.parseColor("#55607A"));
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sublp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sublp.topMargin = 8;
        layout.addView(sub, sublp);

        pinInput = new EditText(this);
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setGravity(Gravity.CENTER);
        pinInput.setBackgroundResource(R.drawable.bg_edittext);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 32;
        lp.bottomMargin = 12;
        layout.addView(pinInput, lp);

        error = new TextView(this);
        error.setTextColor(Color.parseColor("#CC3333"));
        error.setGravity(Gravity.CENTER);
        layout.addView(error);

        Button unlock = new Button(this);
        unlock.setText("Unlock");
        unlock.setAllCaps(false);
        unlock.setBackgroundResource(R.drawable.bg_button_primary);
        unlock.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.topMargin = 24;
        layout.addView(unlock, blp);
        unlock.setOnClickListener(v -> tryUnlock());

        setContentView(layout);
    }

    private void tryUnlock() {
        String pin = pinInput.getText().toString();
        if (AppPrefs.checkPin(this, pin)) {
            openMain();
        } else {
            error.setText("Incorrect PIN, try again.");
            pinInput.setText("");
        }
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
