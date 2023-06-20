package com.cb3g.channel19;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.android.multidex.myapplication.R;

import org.jetbrains.annotations.NotNull;
//Hi honey
public class Account extends Fragment {
    private TextView status;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.account, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        status = view.findViewById(R.id.statustv);
    }

    @Override
    public void onResume() {
        super.onResume();
        setStatus(RadioService.operator.getSubscribed());
    }

    public void setStatus(Boolean status) {
        if (status) this.status.setText(R.string.pro_mode);
        else this.status.setText(R.string.peak_tune);
    }
}
