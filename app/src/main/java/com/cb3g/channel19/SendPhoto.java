package com.cb3g.channel19;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.SendPhotoBinding;

public class SendPhoto extends DialogFragment {
    private Context context;
    private SendPhotoBinding binding;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Window window = requireDialog().getWindow();
        if (window != null) window.getAttributes().windowAnimations = R.style.photoAnimation;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SendPhotoBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        RadioService.occupied.set(true);
        final String[] photoArray = requireArguments().getStringArray("data");
        binding.banner.setText(photoArray[2]);
        final View.OnClickListener listener = v1 -> {
            Utils.vibrate(v1);
            context.sendBroadcast(new Intent("nineteenClickSound"));
            int id = v1.getId();
            if (id == R.id.send){
                context.sendBroadcast(new Intent("upload").putExtra("uri", photoArray[0]).putExtra("mode", 2345).putExtra("caption", binding.captionTV.getText().toString().trim()).putExtra("sendToId", photoArray[1]).putExtra("sendToHandle", photoArray[2]).putExtra("height", binding.image.getHeight()).putExtra("width", binding.image.getWidth()));
                dismiss();
            }else if (id == R.id.order){
                dismiss();
            }else if (id == R.id.plus){
                context.sendBroadcast(new Intent("nineteenAddCaption").putExtra("data", binding.captionTV.getText().toString()));
            }
        };
        binding.send.setOnClickListener(listener);
        binding.plus.setOnClickListener(listener);
        binding.order.setOnClickListener(listener);
        Glide.with(SendPhoto.this).load(Uri.parse(photoArray[0])).into(binding.image);
    }

    public void updateCaption(String text) {
        if (text == null) return;
        if (text.equals("")) binding.captionTV.setVisibility(View.GONE);
        else {
            binding.captionTV.setVisibility(View.VISIBLE);
            binding.captionTV.setText(text);
        }
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

