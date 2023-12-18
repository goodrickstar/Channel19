package com.cb3g.channel19;


import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.TermsDisplayBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class TermsOfUse extends DialogFragment {
    private final recycler_adapter adapter = new recycler_adapter();
    ArrayList<Term> terms = new ArrayList<>();
    private final boolean required;

    private TermsDisplayBinding binding;

    public TermsOfUse(boolean required) {
        this.required = required;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = TermsDisplayBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.ok.setOnClickListener(v -> {
            Utils.vibrate(v);
            getContext().getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("accepted", true).apply();
            getContext().sendBroadcast(new Intent("nineteenProve"));
            dismiss();
        });
        if (required) {
            binding.declineTerms.setVisibility(View.VISIBLE);
            binding.declineTerms.setOnClickListener(v -> {
                Utils.vibrate(v);
                dismiss();
            });
        }
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        OkHttpClient client = new OkHttpClient();
        client.newCall(new Request.Builder().url("http://23.111.159.2/~channel1/terms.php").build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull final Response response) {
                        if (response.isSuccessful()) {
                            try {
                                String data = response.body().string();
                                terms = new Gson().fromJson(data, new TypeToken<ArrayList<Term>>() {
                                }.getType());
                                Activity activity = getActivity();
                                if (activity != null)
                                    activity.runOnUiThread(adapter::notifyDataSetChanged);
                            } catch (IOException e) {
                                LOG.e("terms.php", e.getMessage());
                            }
                        }
                        response.close();
                    }
                });
    }

    static class Term {
        String message;

        public Term() {
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    class recycler_adapter extends RecyclerView.Adapter<recycler_adapter.MyViewHolder> {

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(getLayoutInflater().inflate(R.layout.terms_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int i) {
            holder.textView.setText((i + 1) + ") " + terms.get(i).getMessage());
        }

        @Override
        public int getItemCount() {
            return terms.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            MyViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.textView);
            }
        }
    }
}

