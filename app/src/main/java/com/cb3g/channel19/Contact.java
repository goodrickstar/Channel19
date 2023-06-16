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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;

public class Contact extends DialogFragment implements View.OnClickListener {
    Context context;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Window window = getDialog().getWindow();
        if (window != null) window.getAttributes().windowAnimations = R.style.photoAnimation;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.contact, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView like = view.findViewById(R.id.like);
        TextView web = view.findViewById(R.id.com);
        TextView email = view.findViewById(R.id.emailus);
        like.setOnClickListener(this);
        web.setOnClickListener(this);
        email.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        context.sendBroadcast(new Intent("nineteenVibrate"));
        context.sendBroadcast(new Intent("nineteenClickSound"));
        switch (v.getId()) {
            case R.id.like:
                String FBpage = "100287816995904";
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/" + FBpage)).addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://touch.facebook.com/pages/x/" + FBpage)).addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
                }
                break;
            case R.id.com:
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://detailsid=" + context.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/detailsid=" + context.getPackageName())));
                }
                break;
            case R.id.emailus:
                Intent intent = new Intent(Intent.ACTION_SEND).setType("text/email");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"3gcb19@gmail.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, RadioService.operator.getHandle() + " - " + RadioService.operator.getUser_id());
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(Intent.createChooser(intent, "Send mail to Developer:"));
                break;
        }
        dismiss();
    }
}
