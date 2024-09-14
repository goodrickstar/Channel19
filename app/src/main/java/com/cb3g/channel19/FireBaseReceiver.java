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
                case "advertise" ->
                        sendBroadcast(new Intent("advertise").setPackage("com.cb3g.channel19"));
                case "privateMessage" ->
                        sendBroadcast(new Intent("nineteenReceivePM").putExtra("data", messageData.toString()).setPackage("com.cb3g.channel19"));
                case "photo" ->
                        sendBroadcast(new Intent("nineteenPhotoReceive").putExtra("data", messageData.toString()).setPackage("com.cb3g.channel19"));
                case "exit" ->
                        sendBroadcast(new Intent("exitChannelNineTeen").setPackage("com.cb3g.channel19"));
                case "toast" ->
                        sendBroadcast(new Intent("nineteenToast").putExtra("data", messageData.getString("content")).setPackage("com.cb3g.channel19"));
                case "removeAllOf" ->
                        sendBroadcast(new Intent("removeAllOf").putExtra("data", messageData.getString("id")).setPackage("com.cb3g.channel19"));
                case "alert" -> {
                    String message = messageData.getString("content");
                    sendBroadcast(new Intent("alert").putExtra("data", new Gson().toJson(new Snack(message, Snackbar.LENGTH_LONG))).putExtra("userId", messageData.getString("userId")).setPackage("com.cb3g.channel19"));
                    if (message.contains("entered the channel")) sendBroadcast(new Intent("fetch_users").setPackage("com.cb3g.channel19"));
                }
                case "snack" ->
                        sendBroadcast(new Intent("snack").putExtra("data", new Gson().toJson(new Snack(messageData.getString("content"), Snackbar.LENGTH_LONG))).setPackage("com.cb3g.channel19"));
                case "clear" -> sendBroadcast(new Intent("clear").setPackage("com.cb3g.channel19"));
                case "bird" ->
                        sendBroadcast(new Intent("bird").putExtra("userId", messageData.getString("operatorId")).setPackage("com.cb3g.channel19"));
                case "longFlag" -> {
                    Log.i("Animate", messageData.toString());
                    sendBroadcast(new Intent("longFlag").putExtra("userId", messageData.getString("operatorId")).putExtra("handle", messageData.getString("handle")).setPackage("com.cb3g.channel19"));
                }
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