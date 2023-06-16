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
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class EnterPassword extends DialogFragment {
    private Context context;
    private MI MI;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Window window = getDialog().getWindow();
        if (window != null)
            window.getAttributes().windowAnimations = R.style.photoAnimation;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Window window = getDialog().getWindow();
        if (window != null) window.setGravity(Gravity.CENTER);
        return inflater.inflate(R.layout.enter_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioService.occupied.set(true);
        final Channel channel = RadioService.gson.fromJson(getArguments().getString("data"), Channel.class);
        TextView title = view.findViewById(R.id.top);
        title.setText(channel.getChannel_name());
        final EditText pinET = view.findViewById(R.id.pin_et);
        final TextView accept = view.findViewById(R.id.accept);
        final TextView cancel = view.findViewById(R.id.cancel);
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.sendBroadcast(new Intent("nineteenVibrate"));
                context.sendBroadcast(new Intent("nineteenClickSound"));
                if (pinET.getText().length() > 0){
                    if (Integer.parseInt(pinET.getText().toString().trim().replace(" ", "")) - channel.getPin() == 0) {
                        SharedPreferences saved = context.getSharedPreferences("channels", Context.MODE_PRIVATE);
                        List<Integer> channels = RadioService.gson.fromJson(saved.getString("channels", "[]"), new TypeToken<List<Integer>>() {}.getType());
                        if (channels == null) channels = new ArrayList<>();
                        channels.add(channel.getChannel());
                        saved.edit().putString("channels", RadioService.gson.toJson(channels)).apply();
                        if (MI != null) MI.launchChannel(channel);
                        dismiss();
                    } else
                        pinET.setError("incorrect");
                }else pinET.setError("enter pin");
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.sendBroadcast(new Intent("nineteenVibrate"));
                context.sendBroadcast(new Intent("nineteenClickSound"));
                if (MI != null) MI.selectChannel(false);
                dismiss();
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

