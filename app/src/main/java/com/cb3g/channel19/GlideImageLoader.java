package com.cb3g.channel19;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.android.multidex.myapplication.R;

public class GlideImageLoader {

    private ImageView content;
    private ProgressBar loading;
    private Context context;

    public GlideImageLoader(Context context, ImageView imageView, ProgressBar progressBar) {
        content = imageView;
        loading = progressBar;
        this.context = context;
    }

    public GlideImageLoader(Context context, ImageView imageView) {
        content = imageView;
        this.context = context;
    }

    public void load(final String url, RequestOptions options) {
        if (url == null || content == null) return;
        if (loading != null) loading.setVisibility(View.VISIBLE);
        Glide.with(context)
                .load(url)
                .apply(options)
                .thumbnail(0.1f)
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        onFinished();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        onFinished();
                        return false;
                    }
                })
                .into(content);
    }

    public void loadRank(final String rank) {
        if (rank == null || content == null) return;
        Glide.with(context).load(Utils.parseRankUrl(rank)).into(content);
    }

    public void load(final String url) {
        if (url == null || content == null) return;
        if (loading != null) loading.setVisibility(View.VISIBLE);
        Glide.with(context)
                .load(url)
                .error(R.drawable.no_signal_w)
                .thumbnail(0.1f)
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        onFinished();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        onFinished();
                        return false;
                    }
                })
                .into(content);
    }

    private void onFinished() {
        if (loading != null) loading.setVisibility(View.GONE);
        if (content != null) content.setVisibility(View.VISIBLE);
    }
}

