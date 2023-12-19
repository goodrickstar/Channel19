package com.cb3g.channel19;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.android.multidex.myapplication.R;


public class ProfilePhotoPicker extends DialogFragment implements View.OnClickListener {
    private Context context;
    private final Gif gif = new Gif();
    private ImageView preview;
    private SI SI;
    private boolean upload = false;
    private ProgressBar loading;

    private final FragmentManager fragmentManager;

    private final ActivityResultLauncher<String> picker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri != null) {
            gif.setId(String.valueOf(System.currentTimeMillis()));
            gif.setUrl(uri.toString());
            setPhoto(gif, true);
        }
    });

    public ProfilePhotoPicker(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        SI = (SI) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RadioService.occupied.set(true);
        return inflater.inflate(R.layout.fragment_photo_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        preview = v.findViewById(R.id.preview);
        ImageView fromDisk = v.findViewById(R.id.fromDisk);
        ImageView fromGiphy = v.findViewById(R.id.fromGiphy);
        TextView accept = v.findViewById(R.id.accept);
        TextView cancel = v.findViewById(R.id.cancel);
        fromDisk.setOnClickListener(this);
        fromGiphy.setOnClickListener(this);
        accept.setOnClickListener(this);
        cancel.setOnClickListener(this);
        gif.setUrl(RadioService.operator.getProfileLink());
        loading = v.findViewById(R.id.loading);
        setPhoto(gif, false);
    }

    @Override
    public void onClick(View v) {
        Utils.vibrate(v);
        int id = v.getId();
        if (id == R.id.accept) {
            FileUpload fileUpload = new FileUpload(gif.getUrl(), RequestCode.PROFILE, gif.getHeight(), gif.getWidth());
            Uploader uploader = new Uploader(context, RadioService.operator, RadioService.client, fileUpload);
            if (upload) uploader.uploadImage();
            else uploader.shareImage();
            dismiss();
        } else if (id == R.id.cancel) {
            dismiss();
        } else if (id == R.id.fromDisk) {
            if (Utils.permissionsAccepted(context, Utils.getStoragePermissions()))
                picker.launch("image/*");
            else
                ActivityCompat.requestPermissions((Activity) SI, Utils.getStoragePermissions(), 2);
        } else if (id == R.id.fromGiphy) {
            ImageSearch imageSearch = (ImageSearch) fragmentManager.findFragmentByTag("imageSearch");
            if (imageSearch == null) {
                imageSearch = new ImageSearch(gif.getId());
                imageSearch.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                imageSearch.show(fragmentManager, "imageSearch");
            }
        }
    }

    public void setPhoto(Gif photo, boolean upload) {
        this.upload = upload;
        loading.setVisibility(View.VISIBLE);
        gif.setUrl(photo.getUrl());
        gif.setId(photo.getId());
        Logger.INSTANCE.i(photo.getId());
        if (photo != null) {
            Glide.with(context).load(photo.getUrl()).addListener(new RequestListener<>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                    loading.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                    loading.setVisibility(View.GONE);
                    if (photo.getHeight() == 0 || photo.getWidth() == 0) {
                        gif.setHeight(resource.getIntrinsicHeight());
                        gif.setWidth(resource.getIntrinsicWidth());
                    } else {
                        gif.setHeight(photo.getHeight());
                        gif.setWidth(photo.getWidth());
                    }
                    return false;
                }
            }).error(R.drawable.no_signal_w).into(preview);
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
