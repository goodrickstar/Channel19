package com.cb3g.channel19;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;

import java.util.Objects;

public class FullScreen extends DialogFragment {
    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Window window = Objects.requireNonNull(getDialog()).getWindow();
        if (window != null) {
            window.setGravity(Gravity.CENTER);
            window.getAttributes().windowAnimations = R.style.photoAnimation;
        }
        return inflater.inflate(R.layout.full_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioService.occupied.set(true);
        final Bundle bundle = requireArguments();
        final String link = bundle.getString("data");
        if (!link.contains(".gif"))
            new GlideImageLoader(context, view.findViewById(R.id.photo_view), view.findViewById(R.id.loading)).load(link);
        else
            new GlideImageLoader(context, view.findViewById(R.id.gif_view), view.findViewById(R.id.loading)).load(link);
    }
}
