package com.cb3g.channel19;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.ContactBinding;

import java.util.Objects;

public class Contact extends DialogFragment implements View.OnClickListener {
    private Context context;
    private ContactBinding binding;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Window window = Objects.requireNonNull(getDialog()).getWindow();
        if (window != null) window.getAttributes().windowAnimations = R.style.photoAnimation;
        binding = ContactBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.like.setOnClickListener(this);
        binding.com.setOnClickListener(this);
        binding.emailus.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Utils.vibrate(v);
        context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
        int id = v.getId();
        if (id == R.id.like) {
            String FBPage = "100287816995904";
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/" + FBPage)).addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://touch.facebook.com/pages/x/" + FBPage)).addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
            }
        } else if (id == R.id.com) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://detailsid=" + context.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/detailsid=" + context.getPackageName())));
            }
        } else if (id == R.id.emailus) {
            Intent intent = new Intent(Intent.ACTION_SEND).setType("text/email");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"3gcb19@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, RadioService.operator.getHandle() + " - " + RadioService.operator.getUser_id());
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(Intent.createChooser(intent, "Send mail to Developer:"));
        }
        dismiss();
    }
}
