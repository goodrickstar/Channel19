package com.cb3g.channel19;

import static com.cb3g.channel19.RadioService.operator;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.AccountBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class Account extends Fragment {
    private AccountBinding binding;

    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = AccountBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!operator.getAdmin()) return;
        binding.kickUsersButton.setVisibility(View.VISIBLE);
        binding.kickUsersButton.setOnClickListener(v -> {
            Utils.vibrate(v);
            Utils.clickSound(context);
            Utils.usersInChannel(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {

                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try (response) {
                        if (response.isSuccessful()) {
                            final String data = response.body().string();
                            final ArrayList<String> ids = new Gson().fromJson(data, new TypeToken<ArrayList<String>>() {
                            }.getType());
                            for (String id : ids){
                               Utils.control().child(id).child(Utils.getKey()).setValue(new ControlObject(ControlCode.KICK_USER, id));
                            }
                            //Utils.control().child(operator.getUser_id()).child(Utils.getKey()).setValue(new ControlObject(ControlCode.KICK_USER, operator.getUser_id()));
                        }
                    } catch (IOException e) {
                        Log.e("user_in_channel.php", e.getMessage());
                    }
                }
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setStatus(operator.getSubscribed());
    }

    public void setStatus(boolean status) {
        if (status) binding.statustv.setText(R.string.pro_mode);
        else binding.statustv.setText(R.string.peak_tune);
    }
}
