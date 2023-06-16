package com.cb3g.channel19;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class CreateChannel extends DialogFragment {
    private Context context;
    private MI MI;
    private TextView close;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Window window = getDialog().getWindow();
        if (window != null) window.setGravity(Gravity.CENTER);
        return inflater.inflate(R.layout.create_channel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Window window = getDialog().getWindow();
        if (window != null)
            window.getAttributes().windowAnimations = R.style.photoAnimation;
        RadioService.occupied.set(true);
        final EditText titleET = view.findViewById(R.id.title_et);
        final EditText pinET = view.findViewById(R.id.pin_et);
        final CheckBox lockCB = view.findViewById(R.id.lock_option);
        lockCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) pinET.setVisibility(View.VISIBLE);
                else pinET.setVisibility(View.INVISIBLE
                );
            }
        });
        close = view.findViewById(R.id.close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.sendBroadcast(new Intent("nineteenVibrate"));
                context.sendBroadcast(new Intent("nineteenClickSound"));
                String name = titleET.getText().toString().trim();
                if (name.length() < 5) {
                    titleET.setError("5 char min");
                    return;
                }
                if (!name.replaceAll("[^\\p{Alpha}\\s]", "").equals(name)) {
                    titleET.setError("Use chars A-Z");
                    return;
                }
                int pin = 0;
                if (lockCB.isChecked()) {
                    String work = pinET.getText().toString().trim();
                    if (work.length() < 4) {
                        pinET.setError("4-digit");
                        return;
                    }
                    pin = Integer.parseInt(work);
                }
                create_channel(name, pin);
            }
        });
    }

    private void create_channel(final String channelName, final int pin) {
        close.setEnabled(false);
        final String data = Jwts.builder()
                .setHeader(RadioService.header)
                .claim("userId", RadioService.operator.getUser_id())
                .claim("channelName", channelName)
                .claim("pin", pin)
                .signWith(SignatureAlgorithm.HS256, RadioService.operator.getKey())
                .compact();
        final Request request = new Request.Builder()
                .url(RadioService.SITE_URL + "user_create_channel.php")
                .post(new FormBody.Builder().add("data", data).build())
                .build();
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful() && isAdded()) {
                    try {
                        final JSONObject data = new JSONObject(response.body().string());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (data.getBoolean("success")) {
                                        SharedPreferences saved = context.getSharedPreferences("channels", Context.MODE_PRIVATE);
                                        List<Integer> channels = RadioService.gson.fromJson(saved.getString("channels", "[]"), new TypeToken<List<Integer>>() {
                                        }.getType());
                                        if (channels == null) channels = new ArrayList<>();
                                        channels.add(data.getInt("channel"));
                                        saved.edit().putString("channels", RadioService.gson.toJson(channels)).apply();
                                        if (MI != null) {
                                            Channel channel = new Channel();
                                            channel.setChannel(data.getInt("channel"));
                                            channel.setChannel_name(channelName);
                                            channel.setPin(pin);
                                            MI.launchChannel(channel);
                                        }
                                        dismiss();
                                    } else {
                                        LOG.i("FAIL", data.getString("channel"));
                                    }
                                } catch (JSONException e) {
                                    LOG.e("create_channel", e.getMessage());
                                }
                            }
                        });
                    } catch (JSONException e) {
                        LOG.e("create_channel", e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }
}

