package com.cb3g.channel19;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;

public class Caption extends DialogFragment {
    private InputMethodManager inputMethodManager;
    private Context context;
    private String caption;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.caption, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        final TextView ok = view.findViewById(R.id.ok);
        final TextView captionET = view.findViewById(R.id.captionET);
        caption = getArguments().getString("data");
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        if (caption != null) {
            captionET.setText(caption);
        }
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                caption = captionET.getText().toString().trim();
                context.sendBroadcast(new Intent("nineteenVibrate"));
                context.sendBroadcast(new Intent("nineteenClickSound"));
                context.sendBroadcast(new Intent("nineteenUpdateCaption").putExtra("data", caption));
                dismiss();
            }
        });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }
}