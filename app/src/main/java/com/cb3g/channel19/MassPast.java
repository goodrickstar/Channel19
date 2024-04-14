package com.cb3g.channel19;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.MassPastLayoutBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.Instant;

import java.util.ArrayList;
import java.util.HashMap;


public class MassPast extends DialogFragment implements ValueEventListener {
    private Context context;
    private final DatabaseReference massReference = Utils.getDatabase().getReference().child("mass history").child(RadioService.operator.getUser_id());
    private final RecyleAdapter recycler_adapter = new RecyleAdapter();
    private final ArrayList<Photo> photoRecords = new ArrayList<>();
    private int screenWidth = 0;
    private final long stamp = Instant.now().getEpochSecond();
    private MassPastLayoutBinding binding;
    private MI MI;

    private GlideImageLoader glideImageLoader;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
        glideImageLoader = new GlideImageLoader(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = MassPastLayoutBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioService.occupied.set(true);
        screenWidth = Utils.getScreenWidth(requireActivity());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setAdapter(recycler_adapter);
        massReference.addValueEventListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        massReference.removeEventListener(this);
    }

    @Override
    public void onDismiss(@NotNull DialogInterface dialog) {
        super.onDismiss(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }

    @Override
    public void onCancel(@NotNull DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        HashMap<String, Object> updates = new HashMap<>();
        photoRecords.clear();
        for (DataSnapshot child : dataSnapshot.getChildren()) {
            Photo photo = child.getValue(Photo.class);
            assert photo != null;
            if (photo.getStamp() < stamp - 86400) {
                updates.put(child.getKey(), null);
            } else photoRecords.add(0, photo);
        }
        recycler_adapter.notifyDataSetChanged();
        massReference.updateChildren(updates);
        binding.hourGlass.setVisibility(View.INVISIBLE);
        if (photoRecords.isEmpty()) dismiss();
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }

    class RecyleAdapter extends RecyclerView.Adapter<RecyleAdapter.MyViewHolder> {

        @NonNull
        @Override
        public RecyleAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyleAdapter.MyViewHolder(getLayoutInflater().inflate(R.layout.past_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyleAdapter.MyViewHolder holder, int i) {
            Photo photo = photoRecords.get(holder.getAdapterPosition());
            holder.image.getLayoutParams().height = (((photo.getHeight() * screenWidth) / photo.getWidth()));
            holder.handle.setText(photo.getHandle());
            holder.stamp.setText(Utils.showElapsed(photo.getStamp(), true));
            glideImageLoader.load(holder.rank, Utils.parseRankUrl(photo.getRank()));
            glideImageLoader.load(holder.profile, photo.getProfileLink(), RadioService.profileOptions);
            glideImageLoader.load(holder.image, holder.progressBar, photo.getUrl(), screenWidth);
            holder.image.setTag(photo);
            holder.save.setTag(photo);
            holder.image.setOnClickListener(listener);
            holder.save.setOnClickListener(listener);
        }

        private final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.vibrate(v);
                Photo photo = (Photo) v.getTag();
                int id = v.getId();
                if (id == R.id.image) {
                    MI.streamFile(photo.getUrl());
                } else if (id == R.id.save) {
                    context.sendBroadcast(new Intent("savePhotoToDisk").putExtra("url", photo.getUrl()));
                }
            }
        };

        @Override
        public int getItemCount() {
            return photoRecords.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            ImageView image, profile, rank, save;
            TextView handle, stamp;
            ProgressBar progressBar;

            MyViewHolder(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.image);
                profile = itemView.findViewById(R.id.profile_pic);
                rank = itemView.findViewById(R.id.black_star_iv);
                handle = itemView.findViewById(R.id.black_handle_tv);
                stamp = itemView.findViewById(R.id.stamp);
                progressBar = itemView.findViewById(R.id.loading);
                save = itemView.findViewById(R.id.save);
            }
        }
    }

}
