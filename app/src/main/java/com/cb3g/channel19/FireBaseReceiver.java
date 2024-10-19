package com.cb3g.channel19;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

public class FireBaseReceiver extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.i("logging", "GCM Received");
        JSONObject messageData = new JSONObject(remoteMessage.getData());
        try {
            switch (messageData.getString("control")) {
                case "listUpdate" ->
                        sendBroadcast(new Intent("listUpdate").putExtra("data", messageData.getString("data")).setPackage("com.cb3g.channel19"));
                case "review" ->
                        sendBroadcast(new Intent("review").setPackage("com.cb3g.channel19"));
                case "nineteenPlayPause" ->
                        sendBroadcast(new Intent("nineteenPlayPause").setPackage("com.cb3g.channel19"));
                case "background" ->
                        sendBroadcast(new Intent("background").putExtra("data", messageData.getString("data")).setPackage("com.cb3g.channel19"));
                case "confirmInterrupt" ->
                        sendBroadcast(new Intent("confirmInterrupt").setPackage("com.cb3g.channel19"));
            }
        } catch (JSONException e) {
            Logger.INSTANCE.e("FCM JSON Error", e.getMessage());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        sendBroadcast(new Intent("token").putExtra("token", token).setPackage("com.cb3g.channel19"));
    }
}