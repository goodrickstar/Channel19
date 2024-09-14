package com.cb3g.channel19;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.ShowPhotoBinding;

import org.jetbrains.annotations.NotNull;

public class ShowPhoto extends DialogFragment {
    private Context context;
    private boolean saved = false;
    private com.cb3g.channel19.MI MI;
    private ShowPhotoBinding binding;
    private final Photo photo;

    public ShowPhoto(Photo photo) {
        this.photo = photo;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = ShowPhotoBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioService.occupied.set(true);
        binding.showPhotoHandleTv.setText(photo.getSenderHandle());
        final View.OnClickListener onClickListener = v -> {
            context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
            Utils.vibrate(v);
            int id = v.getId();
            if (id == R.id.ok) {
                context.sendBroadcast(new Intent("nineteenShowMessages").setPackage("com.cb3g.channel19"));
                dismiss();
            }
            if (id == R.id.save) {
                if (saved) return;
                saved = true;
                context.sendBroadcast(new Intent("savePhotoToDisk").setPackage("com.cb3g.channel19").putExtra("url", photo.getUrl()));
            }
            if (id == R.id.image) {
                if (MI != null)
                    MI.streamFile(photo.getUrl());
            }
            if (id == R.id.ma_chat_history_button) {
                for (User user : RadioService.users) {
                    if (user.getUser_id().equals(photo.getSenderId()) && MI != null) {
                        MI.displayChat(user, false, false);
                        dismiss();
                    }
                }
            }
        };
        binding.ok.setOnClickListener(onClickListener);
        binding.save.setOnClickListener(onClickListener);
        binding.showPhotoChatHistoryButton.setOnClickListener(onClickListener);
        binding.image.setOnClickListener(onClickListener);
        Glide.with(context).load(photo.getUrl()).transition(DrawableTransitionOptions.withCrossFade()).into(binding.image);
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
    }

    @Override
    public void onDismiss(@NotNull DialogInterface dialog) {
        super.onDismiss(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages").setPackage("com.cb3g.channel19"));
    }

    @Override
    public void onCancel(@NotNull DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages").setPackage("com.cb3g.channel19"));
    }
}

