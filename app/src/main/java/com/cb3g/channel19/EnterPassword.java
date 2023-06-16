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

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.EnterPasswordBinding;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class EnterPassword extends DialogFragment {
    private Context context;
    private MI MI;

    private EnterPasswordBinding binding;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Window window = getDialog().getWindow();
        if (window != null) {
            window.setGravity(Gravity.CENTER);
            window.getAttributes().windowAnimations = R.style.photoAnimation;
        }
        binding = EnterPasswordBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioService.occupied.set(true);
        final Channel channel = RadioService.gson.fromJson(getArguments().getString("data"), Channel.class);
        binding.top.setText(channel.getChannel_name());
        binding.accept.setOnClickListener(v -> {
            Utils.vibrate(v);
            context.sendBroadcast(new Intent("nineteenClickSound"));
            if (binding.pinEt.getText().length() > 0){
                if (Integer.parseInt(binding.pinEt.getText().toString().trim().replace(" ", "")) - channel.getPin() == 0) {
                    SharedPreferences saved = context.getSharedPreferences("channels", Context.MODE_PRIVATE);
                    List<Integer> channels = RadioService.gson.fromJson(saved.getString("channels", "[]"), new TypeToken<List<Integer>>() {}.getType());
                    if (channels == null) channels = new ArrayList<>();
                    channels.add(channel.getChannel());
                    saved.edit().putString("channels", RadioService.gson.toJson(channels)).apply();
                    if (MI != null) MI.launchChannel(channel);
                    dismiss();
                } else
                    binding.pinEt.setError("incorrect");
            }else binding.pinEt.setError("enter pin");
        });
        binding.cancel.setOnClickListener(v -> {
            Utils.vibrate(v);
            context.sendBroadcast(new Intent("nineteenClickSound"));
            if (MI != null) MI.selectChannel(false);
            dismiss();
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }
}

