package com.cb3g.channel19;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.ShowPhotoBinding;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ShowPhoto extends DialogFragment {
    private Context context;
    private boolean saved = false;
    private com.cb3g.channel19.MI MI;
    private ShowPhotoBinding binding;
    private final Photo photo;
    private final File resource;

    public ShowPhoto(Photo photo, File resource) {
        this.photo = photo;
        this.resource = resource;
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
        binding.showPhotoHandleTv.setText(photo.getHandle());
        final View.OnClickListener onClickListener = v -> {
            context.sendBroadcast(new Intent("nineteenClickSound"));
            Utils.vibrate(v);
            int id = v.getId();
            if (id == R.id.ok) {
                context.sendBroadcast(new Intent("nineteenShowMessages"));
                dismiss();
            }
            if (id == R.id.save) {
                if (saved) return;
                saved = true;
                context.sendBroadcast(new Intent("savePhotoToDisk").putExtra("url", photo.getUrl()));
            }
            if (id == R.id.image) {
                if (MI != null)
                    MI.streamFile(photo.getUrl());
            }
            if (id == R.id.ma_chat_history_button) {
                for (UserListEntry user : RadioService.users) {
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
        Glide.with(context).load(resource).transition(DrawableTransitionOptions.withCrossFade()).into(binding.image);
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
        context.sendBroadcast(new Intent("checkForMessages"));
    }

    @Override
    public void onCancel(@NotNull DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }
}

