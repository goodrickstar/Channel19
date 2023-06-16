package com.cb3g.channel19;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.databinding.CaptionBinding;
public class Caption extends DialogFragment {
    private Context context;
    private String caption;
    private CaptionBinding binding;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = CaptionBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        caption = getArguments().getString("data");
        Utils.showKeyboard(context, binding.captionET);
        if (caption != null)
            binding.captionET.setText(caption);
        binding.ok.setOnClickListener(v -> {
            caption = binding.captionET.getText().toString().trim();
            Utils.vibrate(v);
            context.sendBroadcast(new Intent("nineteenClickSound"));
            context.sendBroadcast(new Intent("nineteenUpdateCaption").putExtra("data", caption));
            dismiss();
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        Utils.hideKeyboard(context, binding.captionET);
    }
}