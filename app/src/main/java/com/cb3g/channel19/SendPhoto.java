package com.cb3g.channel19;


import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.android.multidex.myapplication.R;

public class SendPhoto extends DialogFragment {
    private Context context;
    private TextView captionTV;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Window window = getDialog().getWindow();
        if (window != null) window.getAttributes().windowAnimations = R.style.photoAnimation;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.send_photo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        RadioService.occupied.set(true);
        final String[] photoArray = getArguments().getStringArray("data");
        final TextView title = v.findViewById(R.id.banner);
        final TextView ok = v.findViewById(R.id.send);
        final TextView cancel = v.findViewById(R.id.order);
        final TextView plus = v.findViewById(R.id.plus);
        final ImageView image = v.findViewById(R.id.image);
        captionTV = v.findViewById(R.id.captionTV);
        assert photoArray != null;
        title.setText(photoArray[2]);
        final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switch (v.getId()) {
                    case R.id.send:
                        context.sendBroadcast(new Intent("nineteenVibrate"));
                        context.sendBroadcast(new Intent("nineteenClickSound"));
                        context.sendBroadcast(new Intent("upload").putExtra("uri", photoArray[0]).putExtra("mode", 2345).putExtra("caption", captionTV.getText().toString().trim()).putExtra("sendToId", photoArray[1]).putExtra("sendToHandle", photoArray[2]).putExtra("height", image.getHeight()).putExtra("width", image.getWidth()));
                        dismiss();
                        break;
                    case R.id.order:
                        context.sendBroadcast(new Intent("nineteenVibrate"));
                        context.sendBroadcast(new Intent("nineteenClickSound"));
                        dismiss();
                        break;
                    case R.id.plus:
                        context.sendBroadcast(new Intent("nineteenVibrate"));
                        context.sendBroadcast(new Intent("nineteenClickSound"));
                        context.sendBroadcast(new Intent("nineteenAddCaption").putExtra("data", captionTV.getText().toString()));
                        break;
                }
            }
        };
        ok.setOnClickListener(listener);
        plus.setOnClickListener(listener);
        cancel.setOnClickListener(listener);
        Glide.with(SendPhoto.this).load(Uri.parse(photoArray[0])).into(image);
    }

    public void updateCaption(String text) {
        if (text == null) return;
        if (text.equals("")) captionTV.setVisibility(View.GONE);
        else {
            captionTV.setVisibility(View.VISIBLE);
            captionTV.setText(text);
        }
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

