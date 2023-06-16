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

public class FCMreciever extends FirebaseMessagingService {

    //TODO: OnTokenChanged()
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        JSONObject messageData = new JSONObject(remoteMessage.getData());
        try {
            switch (messageData.getString("control")) {
                case "review":
                    sendBroadcast(new Intent("review").setPackage("com.cb3g.channel19"));
                    break;
                case "nineteenPlayPause":
                    sendBroadcast(new Intent("nineteenPlayPause").setPackage("com.cb3g.channel19"));
                    break;
                case "background":
                    sendBroadcast(new Intent("background").putExtra("data", messageData.getString("data")).setPackage("com.cb3g.channel19"));
                    break;
                case "advertise":
                    sendBroadcast(new Intent("advertise").setPackage("com.cb3g.channel19"));
                    break;
                case "privateMessage":
                    sendBroadcast(new Intent("nineteenReceivePM").putExtra("data", messageData.toString()).setPackage("com.cb3g.channel19"));
                    break;
                case "photo":
                    sendBroadcast(new Intent("nineteenPhotoReceive").putExtra("data", messageData.toString()).setPackage("com.cb3g.channel19"));
                    break;
                case "exit":
                    sendBroadcast(new Intent("exitChannelNineTeen").setPackage("com.cb3g.channel19"));
                    break;
                case "toast":
                    sendBroadcast(new Intent("nineteenToast").putExtra("data", messageData.getString("content")).setPackage("com.cb3g.channel19"));
                    break;
                case "pulse":
                    sendBroadcast(new Intent("nineteenPulse").setPackage("com.cb3g.channel19"));
                    break;
                case "removeAllOf":
                    sendBroadcast(new Intent("removeAllOf").putExtra("data", messageData.getString("id")).setPackage("com.cb3g.channel19"));
                    break;
                case "alert":
                    sendBroadcast(new Intent("alert").putExtra("data", new Gson().toJson(new Snack(messageData.getString("content"), Snackbar.LENGTH_LONG))).putExtra("userId", messageData.getString("userId")).setPackage("com.cb3g.channel19"));
                    break;
                case "snack":
                    sendBroadcast(new Intent("snack").putExtra("data", new Gson().toJson(new Snack(messageData.getString("content"), Snackbar.LENGTH_LONG))).setPackage("com.cb3g.channel19"));
                    break;
                case "clear":
                    sendBroadcast(new Intent("clear").setPackage("com.cb3g.channel19"));
                    break;
                case "bird":
                    sendBroadcast(new Intent("bird").putExtra("userId", messageData.getString("operatorId")).setPackage("com.cb3g.channel19"));
                    break;
                case "longFlag":
                    Log.i("Animate", messageData.toString());
                    sendBroadcast(new Intent("longFlag").putExtra("userId", messageData.getString("operatorId")).putExtra("handle", messageData.getString("handle")).setPackage("com.cb3g.channel19"));
                    break;
                case "confirmInterrupt":
                    sendBroadcast(new Intent("confirmInterrupt").setPackage("com.cb3g.channel19"));
                    break;
            }
        } catch (JSONException e) {
            Logger.INSTANCE.e("FCM JSON Error", e.getMessage());
        }
    }
}