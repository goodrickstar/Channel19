package com.cb3g.channel19;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;

public class CreatePoll extends DialogFragment implements View.OnClickListener {
    private Context context;
    private RI RI;
    private EditText content;
    private final String postId;

    public CreatePoll(String postId) {
        this.postId = postId;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        RI = (RI) getActivity();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.create_poll, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final TextView cancel = view.findViewById(R.id.cancel);
        final TextView finish = view.findViewById(R.id.finish);
        content = view.findViewById(R.id.question);
        cancel.setOnClickListener(this);
        finish.setOnClickListener(this);
        if (postId == null) content.setHint("Enter Poll Question Here..");
        else content.setHint("Enter Poll Option Here..");
        Utils.showKeyboard(context, content);
    }

    @Override
    public void onClick(View v) {
        context.sendBroadcast(new Intent("vibrate"));
        if (v.getId() == R.id.finish) {
            if (!content.getText().toString().trim().isEmpty())
                RI.createNewPoll(content.getText().toString().trim(), postId);
        }
        dismiss();
    }
}
