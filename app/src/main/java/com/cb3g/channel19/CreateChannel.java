package com.cb3g.channel19;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.CreateChannelBinding;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private CreateChannelBinding binding;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
        RadioService.occupied.set(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages").setPackage("com.cb3g.channel19"));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Window window = Objects.requireNonNull(getDialog()).getWindow();
        if (window != null) {
            window.setGravity(Gravity.CENTER);
            window.getAttributes().windowAnimations = R.style.photoAnimation;
        }
        binding = CreateChannelBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.lockOption.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) binding.pinEt.setVisibility(View.VISIBLE);
            else binding.pinEt.setVisibility(View.INVISIBLE
            );
        });
        binding.lockOption.setEnabled(RadioService.appOptions.getPrivate_channels());
        binding.close.setOnClickListener(v -> {
            Utils.vibrate(v);
            context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
            String name = binding.titleEt.getText().toString().trim();
            if (name.length() < 5) {
                binding.titleEt.setError("5 char min");
                return;
            }
            if (!name.replaceAll("[^\\p{Alpha}\\s]", "").equals(name)) {
                binding.titleEt.setError("Use chars A-Z");
                return;
            }
            int pin = 0;
            if (binding.lockOption.isChecked()) {
                String work = binding.pinEt.getText().toString().trim();
                if (work.length() < 4) {
                    binding.pinEt.setError("4-digit");
                    return;
                }
                pin = Integer.parseInt(work);
            }
            create_channel(name, pin);
        });
    }

    private void create_channel(final String channelName, final int pin) {
        binding.close.setEnabled(false);
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
                        assert response.body() != null;
                        final JSONObject data = new JSONObject(response.body().string());
                        requireActivity().runOnUiThread(() -> {
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
                        });
                    } catch (JSONException e) {
                        LOG.e("create_channel", e.getMessage());
                    }
                }
            }
        });
    }
}

