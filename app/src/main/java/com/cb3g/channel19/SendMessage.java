package com.cb3g.channel19;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;

public class SendMessage extends DialogFragment {
    private String id;
    private EditText messageET;
    private InputMethodManager methodManager;
    private Context context;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //Window window = getDialog().getWindow();
        //if (window != null) window.getAttributes().windowAnimations = R.style.pmAnimationTwo;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Window window = getDialog().getWindow();
        if (window != null) window.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        return inflater.inflate(R.layout.send_pm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioService.occupied.set(true);
        context = getContext();
        methodManager = (InputMethodManager) getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        final TextView handleTV = view.findViewById(R.id.handle);
        final ImageView starIV = view.findViewById(R.id.starIV);
        final TextView send = view.findViewById(R.id.send);
        final TextView cancel = view.findViewById(R.id.order);
        id = getArguments().getString("userId");
        handleTV.setText(getArguments().getString("handle"));
        final TextView count = view.findViewById(R.id.count);
        final ImageView profile = view.findViewById(R.id.option_image_view);
        new GlideImageLoader(context, profile).load(getArguments().getString("profileLink"), RadioService.profileOptions);
        new GlideImageLoader(context, starIV).load(Utils.parseRankUrl(getArguments().getString("rank")));
        messageET = view.findViewById(R.id.messagebox);
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
        final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                methodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                final String output = messageET.getText().toString().trim();
                context.sendBroadcast(new Intent("nineteenVibrate"));
                context.sendBroadcast(new Intent("nineteenClickSound"));
                if (!output.isEmpty() && v.getId() == R.id.send)
                    context.sendBroadcast(new Intent("nineteenSendPM").putExtra("id", id).putExtra("text", output));
                dismiss();
            }
        };
        send.setOnClickListener(listener);
        cancel.setOnClickListener(listener);
        Utils.showKeyboard(context, messageET);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
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

