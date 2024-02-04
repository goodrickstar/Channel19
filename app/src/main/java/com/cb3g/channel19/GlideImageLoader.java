package com.cb3g.channel19;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.io.File;

public class GlideImageLoader {

    private final Context context;

    public GlideImageLoader(Context context) {
        this.context = context;
    }

    public void load(ImageView imageView, final String url) {
        Glide.with(context).load(url).transition(DrawableTransitionOptions.withCrossFade()).into(imageView);
    }

    public void load(ImageView imageView, final String url, RequestOptions options) {
        Glide.with(context).load(url).apply(options).transition(DrawableTransitionOptions.withCrossFade()).into(imageView);
    }

    public void load(ImageView imageView, ProgressBar progressBar, final String url) {
        progressBar.setVisibility(View.VISIBLE);
        Glide.with(context).load(url).transition(DrawableTransitionOptions.withCrossFade()).listener(new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                progressBar.setVisibility(View.INVISIBLE);
                return false;
            }

            @Override
            public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                progressBar.setVisibility(View.INVISIBLE);
                imageView.setVisibility(View.VISIBLE);
                return false;
            }
        }).into(imageView);
    }

    public void load(ImageView imageView, ProgressBar progressBar, final String url, final int screenWidth) {
        progressBar.setVisibility(View.VISIBLE);
        Glide.with(context).load(url).transition(DrawableTransitionOptions.withCrossFade()).listener(new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                progressBar.setVisibility(View.INVISIBLE);
                return false;
            }

            @Override
            public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                progressBar.setVisibility(View.INVISIBLE);
                imageView.getLayoutParams().height = (((resource.getIntrinsicHeight() * screenWidth) / resource.getIntrinsicWidth()));
                imageView.setVisibility(View.VISIBLE);
                return false;
            }
        }).into(imageView);
    }

    public void preload(String url, RequestListener<File> listener) {
        Glide.with(context)
                .downloadOnly()
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .load(url)
                .listener(listener)
                .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
    }
}

