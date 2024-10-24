package com.cb3g.channel19;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;

public class SendMessage extends DialogFragment {
    private String id;
    private EditText messageET;
    private Context context;

    private GlideImageLoader glideImageLoader;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.send_pm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioService.occupied.set(true);
        context = getContext();
        final TextView handleTV = view.findViewById(R.id.black_handle_tv);
        final ImageView starIV = view.findViewById(R.id.black_star_iv);
        final TextView send = view.findViewById(R.id.send);
        final TextView cancel = view.findViewById(R.id.order);
        final ImageView profile = view.findViewById(R.id.black_profile_picture_iv);
        final TextView count = view.findViewById(R.id.count);
        messageET = view.findViewById(R.id.messagebox);
        final Bundle bundle = requireArguments();
        id = bundle.getString("userId");
        handleTV.setText(bundle.getString("handle"));
        glideImageLoader.load(profile, bundle.getString("profileLink"), RadioService.profileOptions);
        glideImageLoader.load(starIV, Utils.parseRankUrl(bundle.getString("rank")));
        messageET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                count.setText(String.valueOf(s.length()));
            }
        });
        final View.OnClickListener listener = v -> {
            Utils.hideKeyboard(context, v);
            final String output = messageET.getText().toString().trim();
            Utils.vibrate(v);
            context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
            if (!output.isEmpty() && v.getId() == R.id.send)
                context.sendBroadcast(new Intent("nineteenSendPM").setPackage("com.cb3g.channel19").putExtra("id", id).putExtra("text", output));
            dismiss();
        };
        send.setOnClickListener(listener);
        cancel.setOnClickListener(listener);
        Utils.showKeyboard(context, messageET);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        glideImageLoader = new GlideImageLoader(context);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages").setPackage("com.cb3g.channel19"));
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages").setPackage("com.cb3g.channel19"));
    }
}

