package com.cb3g.channel19;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;


public class RemoteActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        sendBroadcast(new Intent("recordFromMain").setPackage("com.cb3g.channel19"));
        finish();
    }
}
