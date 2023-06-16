package com.cb3g.channel19;


import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Window window = getDialog().getWindow();
        if (window != null) window.getAttributes().windowAnimations = R.style.photoAnimation;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.terms_display, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final TextView ok = view.findViewById(R.id.ok);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("accepted", true).apply();
                getContext().sendBroadcast(new Intent("nineteenProve"));
                dismiss();
            }
        });
        final RecyclerView userlist = view.findViewById(R.id.recyclerView);
        userlist.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        userlist.setHasFixedSize(true);
        userlist.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        OkHttpClient client = new OkHttpClient();
        client.newCall(new Request.Builder().url("http://truckradiosystem.com/~channel1/terms.php").build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull final Response response) {
                        if (response.isSuccessful()) {
                            try {
                                terms = new Gson().fromJson(response.body().string(), new TypeToken<ArrayList<Term>>() {
                                }.getType());
                                Activity activity = getActivity();
                                if (activity != null)
                                    activity.runOnUiThread(adapter::notifyDataSetChanged);
                            } catch (IOException e) {
                                LOG.e("http://truckradiosystem.com/~channel1/terms.php", e.getMessage());
                            }
                        }
                        response.close();
                    }
                });
    }

    class Term {
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

