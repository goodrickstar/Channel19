package com.cb3g.channel19;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.android.multidex.myapplication.R;

public class ImagePicker extends DialogFragment implements View.OnClickListener {
    private Context context;
    private final Gif gif = new Gif();
    private ImageView preview, placeHolder;
    private MI MI;
    private boolean upload = false;
    private ProgressBar loading;
    private TextView accept;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (MI) getActivity();
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
        placeHolder = v.findViewById(R.id.placeHolder);
        ImageView fromDisk = v.findViewById(R.id.fromDisk);
        ImageView fromGiphy = v.findViewById(R.id.fromGiphy);
        TextView title = v.findViewById(R.id.textView4);
        accept = v.findViewById(R.id.accept);
        TextView cancel = v.findViewById(R.id.cancel);
        fromDisk.setOnClickListener(this);
        fromGiphy.setOnClickListener(this);
        accept.setOnClickListener(this);
        cancel.setOnClickListener(this);
        loading = v.findViewById(R.id.loading);
        placeHolder.setVisibility(View.VISIBLE);
        accept.setVisibility(View.GONE);
        title.setText(requireArguments().getString("handle"));
    }

    @Override
    public void onClick(View v) {
        Utils.vibrate(v);
        int id = v.getId();
        if (id == R.id.accept) {
            MI.photoChosen(gif, upload);
            context.sendBroadcast(new Intent("nineteenClickSound"));
            dismiss();
        } else if (id == R.id.cancel) {
            context.sendBroadcast(new Intent("nineteenClickSound"));
            dismiss();
        } else if (id == R.id.fromDisk) {
            MI.photo_picker(1111);
        } else if (id == R.id.fromGiphy) {
            MI.launchSearch(gif.getId());
        }
    }

    public void setPhoto(Gif photo, boolean upload) {
        this.upload = upload;
        loading.setVisibility(View.VISIBLE);
        placeHolder.setVisibility(View.GONE);
        gif.setUrl(photo.getUrl());
        gif.setId(photo.getId());
        Glide.with(context).load(photo.getUrl()).addListener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                loading.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                loading.setVisibility(View.GONE);
                if (photo.getHeight() == 0 || photo.getWidth() == 0) {
                    gif.setHeight(resource.getIntrinsicHeight());
                    gif.setWidth(resource.getIntrinsicWidth());
                } else {
                    gif.setHeight(photo.getHeight());
                    gif.setWidth(photo.getWidth());
                }
                accept.setVisibility(View.VISIBLE);
                return false;
            }
        }).error(R.drawable.no_signal_w).into(preview);
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }
}
