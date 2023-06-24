package com.cb3g.channel19;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.android.multidex.myapplication.R;

final class Toaster {

    static void flipDaBird(final Context context) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") final View layout = inflater.inflate(R.layout.toast_bird, null);
        final Toast toast = new Toast(context);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }


    static void toastlow(final Context context, final String message) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") final View layout = inflater.inflate(R.layout.toast_layout, null);
        final TextView text = layout.findViewById(R.id.toasttext);
        final Toast toast = new Toast(context);
        text.setText(message);
        toast.setGravity(Gravity.BOTTOM, 0, 300);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    static void label(final Context context) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") final View layout = inflater.inflate(R.layout.toast_label, null);
        final TextView text = layout.findViewById(R.id.toasttext);
        final ImageView profile = layout.findViewById(R.id.profileToast);
        profile.setImageResource(R.drawable.app_icon);
        text.setText(R.string.shiny_side_up);
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.BOTTOM, 0, 300);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }


    static void checkMark(final Context context) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") final View layout = inflater.inflate(R.layout.checkmark_toast, null);
        final Toast toast = new Toast(context);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    static void fail(final Context context) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") final View layout = inflater.inflate(R.layout.fail_toast, null);
        final Toast toast = new Toast(context);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    static void labelTwo(final Context context, String message) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") final View layout = inflater.inflate(R.layout.toast_label, null);
        final TextView text = layout.findViewById(R.id.toasttext);
        final Toast toast = new Toast(context);
        text.setText(message);
        toast.setGravity(Gravity.BOTTOM, 0, 300);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    static void online(final Context context, final String message, final String profileLink) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") final View layout = inflater.inflate(R.layout.toast_label, null);
        final TextView text = layout.findViewById(R.id.toasttext);
        final ImageView profile = layout.findViewById(R.id.profileToast);
        Glide.with(context).load(profileLink).apply(RadioService.profileOptions).into(new CustomTarget<Drawable>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                profile.setImageDrawable(resource);
                text.setText(message);
                Toast toast = new Toast(context);
                toast.setGravity(Gravity.BOTTOM, 0, 300);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(layout);
                toast.show();
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }
        });
    }
}