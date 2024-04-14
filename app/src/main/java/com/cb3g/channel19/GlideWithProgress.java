package com.cb3g.channel19;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

public class GlideWithProgress {

    private final ImageView mImageView;
    private final ProgressBar mProgressBar;

    public GlideWithProgress(ImageView imageView, ProgressBar progressBar) {
        mImageView = imageView;
        mProgressBar = progressBar;
    }

    public void load(final String url, RequestOptions options) {
        onConnecting();
        NineteenGlideModule.expect(url, new NineteenGlideModule.UIonProgressListener() {
            @Override
            public void onProgress(long bytesRead, long expectedLength) {
                if (mProgressBar != null) {
                    mProgressBar.setProgress((int) (100 * bytesRead / expectedLength));
                }
            }

            @Override
            public float getGranularityPercentage() {
                return 1.0f;
            }
        });
        //Get Image
        Glide.with(mImageView.getContext())
                .load(url)
                .transition(withCrossFade())
                .apply(options.skipMemoryCache(true))
                .listener(new RequestListener<>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                        NineteenGlideModule.forget(url);
                        //onFinished();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                        NineteenGlideModule.forget(url);
                        //onFinished();
                        return false;
                    }
                }).into(mImageView);
    }


    private void onConnecting() {
        if (mProgressBar != null) mProgressBar.setVisibility(View.VISIBLE);
    }

    private void onFinished() {
        if (mProgressBar != null && mImageView != null) {
            //mProgressBar.setVisibility(View.GONE);
            //mImageView.setVisibility(View.VISIBLE);
        }
    }
}