package com.cb3g.channel19;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;

public class
ShowMessage extends DialogFragment implements View.OnClickListener {
    private boolean replying = false;
    private Context context;
    private String[] message;
    private TextView left, right, inbound;
    private EditText outbound;
    private MI MI;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*
        Window window = requireDialog().getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.pmAnimation;
            window.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        }
         */
        return inflater.inflate(R.layout.show_pm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioService.occupied.set(true);
        final TextView handle = view.findViewById(R.id.black_handle_tv);
        final ImageView starIV = view.findViewById(R.id.black_star_iv);
        final ImageView profile = view.findViewById(R.id.black_profile_picture_iv);
        final ImageView history = view.findViewById(R.id.ma_chat_history_button);
        outbound = view.findViewById(R.id.messagebox);
        inbound = view.findViewById(R.id.inboundBox);
        left = view.findViewById(R.id.order);
        right = view.findViewById(R.id.send);
        message = requireArguments().getStringArray("data");
        new GlideImageLoader(context, profile).load(message[4], RadioService.profileOptions);
        new GlideImageLoader(context, starIV).load(Utils.parseRankUrl(message[3]));
        handle.setText(message[1]);
        inbound.setText(message[2]);
        left.setOnClickListener(this);
        right.setOnClickListener(this);
        history.setOnClickListener(this);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (MI) getActivity();
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

    @Override
    public void onClick(View v) {
        Utils.vibrate(v);
        context.sendBroadcast(new Intent("nineteenClickSound"));
        int id = v.getId();
        if (id == R.id.order) {
            if (!replying) {
                Utils.showKeyboard(context, outbound);
                replying = true;
                left.setText(R.string.cancel);
                right.setText(R.string.send);
                inbound.setVisibility(View.INVISIBLE);
                outbound.setVisibility(View.VISIBLE);
            } else {
                Utils.hideKeyboard(context, outbound);
                replying = false;
                left.setText(R.string.reply);
                right.setText(R.string.close);
                inbound.setVisibility(View.VISIBLE);
                outbound.setVisibility(View.INVISIBLE);
            }
        } else if (id == R.id.send) {
            if (replying) {
                Utils.hideKeyboard(context, outbound);
                final String output = outbound.getText().toString().trim();
                if (!output.isEmpty())
                    context.sendBroadcast(new Intent("nineteenSendPM").putExtra("id", message[0]).putExtra("text", output));
                dismiss();
            } else dismiss();
        } else if (id == R.id.ma_chat_history_button) {
            for (UserListEntry user : RadioService.users) {
                if (user.getUser_id().equals(message[0]) && MI != null) {
                    MI.displayChat(user, false, false);
                    dismiss();
                }
            }
        }
    }
}


