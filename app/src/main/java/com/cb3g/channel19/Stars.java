package com.cb3g.channel19;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.StarSelectionBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class Stars extends DialogFragment {
    private Context context;
    private StarSelectionBinding binding;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onResume() {
        super.onResume();
        final Request request = new Request.Builder()
                .url(RadioService.SITE_URL + "user_list_stars.php")
                .build();
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful())
                    binding.selection.setAdapter(new RecyclerAdapter(new Gson().fromJson(response.body().string(), new TypeToken<ArrayList<String>>() {
                    }.getType())));
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = StarSelectionBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NotNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        binding.selection.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        binding.selection.setHasFixedSize(true);
        binding.close.setOnClickListener(v1 -> {
            context.sendBroadcast(new Intent("nineteenClickSound"));
            Utils.vibrate(v1);
            dismiss();
        });
    }

    class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {
        GlideImageLoader glideImageLoader = new GlideImageLoader(context);
        ArrayList<String> stars;

        public RecyclerAdapter(ArrayList<String> stars) {
            this.stars = stars;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(getLayoutInflater().inflate(R.layout.star_selection_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            glideImageLoader.load(holder.star, Utils.parseRankUrl(stars.get(position)));
            holder.itemView.setOnClickListener(v -> {
                context.sendBroadcast(new Intent("setStar").putExtra("data", stars.get(holder.getAdapterPosition())));
                context.sendBroadcast(new Intent("nineteenClickSound"));
                Utils.vibrate(v);
                dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return stars.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            ImageView star;

            MyViewHolder(View itemView) {
                super(itemView);
                star = itemView.findViewById(R.id.star);
            }
        }
    }
}

