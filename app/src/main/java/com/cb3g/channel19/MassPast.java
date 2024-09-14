package com.cb3g.channel19;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.MassPastLayoutBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class MassPast extends DialogFragment implements ValueEventListener {
    private Context context;
    private final DatabaseReference massReference = Utils.getDatabase().getReference().child("mass history").child(RadioService.operator.getUser_id());
    private int screenWidth = 0;

    private MassAdapter adapter;
    private MassPastLayoutBinding binding;
    private MI MI;
    private boolean staged = false;
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
        adapter = new MassAdapter();
        screenWidth = Utils.getScreenWidth(requireActivity());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setAdapter(adapter);
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
        context.sendBroadcast(new Intent("checkForMessages").setPackage("com.cb3g.channel19"));
    }

    @Override
    public void onCancel(@NotNull DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages").setPackage("com.cb3g.channel19"));
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        if (staged) context.sendBroadcast(new Intent("nineteenToast").setPackage("com.cb3g.channel19").putExtra("data", "Photo Received").setPackage("com.cb3g.channel19"));
        else binding.recyclerView.smoothScrollToPosition(0);
        staged = true;
        List<Photo> photos = new ArrayList<>();
        for (DataSnapshot child : dataSnapshot.getChildren()) {
            Photo photo = child.getValue(Photo.class);
            if (photo != null && child.getKey() != null) {
                photo.setKey(child.getKey());
                photos.add(photo);
                photos.sort((one, two) -> Long.compare(two.getStamp(), one.getStamp()));
            }
        }
        if (photos.isEmpty()) {
            context.sendBroadcast(new Intent("nineteenToast").setPackage("com.cb3g.channel19").putExtra("data", "No History Yet").setPackage("com.cb3g.channel19"));
            dismiss();
        }
        else {
            binding.massProgress.setVisibility(View.INVISIBLE);
            adapter.updatePhotos(photos);
        }
        context.getSharedPreferences("massPast", Context.MODE_PRIVATE).edit().putString("history", new Gson().toJson(photos)).apply();
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }

    class MassAdapter extends RecyclerView.Adapter<MassAdapter.MyViewHolder> {
        private List<Photo> photos;

        public MassAdapter() {
            final String data = context.getSharedPreferences("massPast", Context.MODE_PRIVATE).getString("history", "");
            if (!data.isEmpty()) {
                photos = new Gson().fromJson(data, new TypeToken<List<Photo>>() {
                }.getType());
                binding.massProgress.setVisibility(View.INVISIBLE);
            }
            else photos = new ArrayList<>();
        }

        public void updatePhotos(List<Photo> photos) {
            final MassPhotoDiffCallBack diffCallback = new MassPhotoDiffCallBack(this.photos, photos);
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
            this.photos = photos;
            diffResult.dispatchUpdatesTo(this);
        }

        @NonNull
        @Override
        public MassAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(getLayoutInflater().inflate(R.layout.past_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MassAdapter.MyViewHolder holder, int i) {
            Photo photo = photos.get(holder.getAdapterPosition());
            holder.image.getLayoutParams().height = (((photo.getHeight() * screenWidth) / photo.getWidth()));
            holder.handle.setText(photo.getSenderHandle());
            holder.stamp.setText(Utils.showElapsed(photo.getStamp(), true));
            glideImageLoader.load(holder.rank, Utils.parseRankUrl(photo.getSenderRank()));
            glideImageLoader.load(holder.profile, photo.getSenderProfile(), RadioService.profileOptions);
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
                    context.sendBroadcast(new Intent("savePhotoToDisk").setPackage("com.cb3g.channel19").putExtra("url", photo.getUrl()));
                }
            }
        };

        @Override
        public int getItemCount() {
            return photos.size();
        }

        static class MyViewHolder extends RecyclerView.ViewHolder {
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
