package com.cb3g.channel19;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.ShowPhotoBinding;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

public class ShowPhoto extends DialogFragment {
    private Context context;
    private boolean saved = false;
    private com.cb3g.channel19.MI MI;
    private Photo photo;
    private ShowPhotoBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = ShowPhotoBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioService.occupied.set(true);
        photo = new Gson().fromJson(requireArguments().getString("data"), Photo.class);
        if (!photo.getCaption().isEmpty()) {
            binding.photoCaption.setVisibility(View.VISIBLE);
            binding.photoCaption.setText(photo.getCaption());
        }
        binding.banner.setText(photo.getHandle());
        final View.OnClickListener onClickListener = v -> {
            context.sendBroadcast(new Intent("nineteenClickSound"));
            Utils.vibrate(v);
            int id = v.getId();
            if (id == R.id.ok){
                context.sendBroadcast(new Intent("nineteenShowMessages"));
                dismiss();
            }if (id == R.id.save){
                if (saved) return;
                saved = true;
                context.sendBroadcast(new Intent("savePhotoToDisk").putExtra("url", photo.getUrl()));
            }if (id == R.id.image){
                if (MI != null)
                    MI.streamFile(photo.getUrl());
            }if (id == R.id.history){
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
        binding.history.setOnClickListener(onClickListener);
        binding.loading.setVisibility(View.VISIBLE);
        if (!photo.getUrl().contains(".gif")) {
            //gif
            binding.image.setVisibility(View.VISIBLE);
            Glide.with(ShowPhoto.this).load(photo.getUrl()).thumbnail(0.1f).addListener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    binding.loading.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    binding.loading.setVisibility(View.GONE);
                    return false;
                }
            }).error(R.drawable.no_signal_w).into(binding.image);
        } else {
            binding.gifView.setVisibility(View.VISIBLE);
            Glide.with(ShowPhoto.this).load(photo.getUrl()).thumbnail(0.1f).addListener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    binding.loading.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    binding.loading.setVisibility(View.GONE);
                    return false;
                }
            }).error(R.drawable.no_signal_w).into(binding.gifView);
        }
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

