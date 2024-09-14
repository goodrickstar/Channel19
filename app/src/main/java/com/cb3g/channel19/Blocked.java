package com.cb3g.channel19;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Blocked extends DialogFragment {
    private TextView count;
    private Context context;
    private SharedPreferences settings;
    private final List<String> ids = new ArrayList<>();
    private final List<String> handles = new ArrayList<>();
    private List<Block> photo, text, radio;
    private final RecycleAdapter adapter = new RecycleAdapter();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    private List<Block> returnBlockListObjectFromStorage(String key) {
        return RadioService.gson.fromJson(settings.getString(key, "[]"), new TypeToken<List<Block>>() {
        }.getType());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.blocked, container, false);
    }

    private void compileIds(List<Block> list) {
        for (Block block : list) {
            if (!ids.contains(block.getI())) {
                ids.add(block.getI());
                handles.add(block.getH());
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Window window = Objects.requireNonNull(getDialog()).getWindow();
        if (window != null) window.getAttributes().windowAnimations = R.style.photoAnimation;
        settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        radio = returnBlockListObjectFromStorage("blockedIDs");
        text = returnBlockListObjectFromStorage("textIDs");
        photo = returnBlockListObjectFromStorage("photoIDs");
        final View outside = view.findViewById(R.id.outside);
        final TextView cancel = view.findViewById(R.id.order);
        final TextView save = view.findViewById(R.id.save);
        final TextView clear = view.findViewById(R.id.instruct);
        compileIds(radio);
        compileIds(text);
        compileIds(photo);
        count = view.findViewById(R.id.middle);
        count.setText(String.valueOf(ids.size()));
        View.OnClickListener listener = v -> {
            int id = v.getId();
            if (id == R.id.order) {
                Utils.vibrate(v);
                context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
            } else if (id == R.id.save) {
                Utils.vibrate(v);
                context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
                context.sendBroadcast(new Intent("nineteenUpdateBlocks").setPackage("com.cb3g.channel19").putExtra("photoIDs", RadioService.gson.toJson(photo)).putExtra("textIDs", RadioService.gson.toJson(text)).putExtra("blockedIDs", RadioService.gson.toJson(radio)));
            }
            dismiss();
        };
        save.setOnClickListener(listener);
        cancel.setOnClickListener(listener);
        outside.setOnClickListener(listener);
        clear.setOnLongClickListener(v -> {
            Utils.vibrate(v);
            radio.clear();
            photo.clear();
            text.clear();
            ids.clear();
            adapter.notifyDataSetChanged();
            return false;
        });
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    private class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.viewHolder> implements View.OnClickListener {

        private boolean listContainsId(List<Block> list, String id) {
            for (Block block : list) {
                if (block.getI().equals(id)) return true;
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            Utils.vibrate(v);
            String id = (String) v.getTag();
            int x = v.getId();
            if (x == R.id.photo) {
                for (int i = 0; i < photo.size(); i++) {
                    if (photo.get(i).getI().equals(id)) photo.remove(i);
                    adapter.notifyItemChanged(ids.indexOf(id));
                }
            } else if (x == R.id.text) {
                for (int i = 0; i < text.size(); i++) {
                    if (text.get(i).getI().equals(id)) text.remove(i);
                    adapter.notifyItemChanged(ids.indexOf(id));
                }
            } else if (x == R.id.radio) {
                for (int i = 0; i < radio.size(); i++) {
                    if (radio.get(i).getI().equals(id)) radio.remove(i);
                    adapter.notifyItemChanged(ids.indexOf(id));
                }
            }
            if (ids.contains(id) && !listContainsId(photo, id) && !listContainsId(text, id) && !listContainsId(radio, id)) {
                int index = ids.indexOf(id);
                ids.remove(index);
                handles.remove(index);
                adapter.notifyItemRemoved(index);
                count.setText(String.valueOf(ids.size()));
            }
        }

        @NotNull
        @Override
        public viewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
            return new viewHolder(getLayoutInflater().inflate(R.layout.block_list_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull viewHolder holder, int position) {
            holder.handle.setText(handles.get(position));
            String id = ids.get(position);
            holder.photo.setTag(id);
            holder.text.setTag(id);
            holder.radio.setTag(id);
            if (listContainsId(photo, id)) {
                holder.photo.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.photo_w));
                holder.photo.setOnClickListener(this);
            } else {
                holder.photo.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.photo_g));
                holder.photo.setOnClickListener(null);
            }
            if (listContainsId(text, id)) {
                holder.text.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.messages_w));
                holder.text.setOnClickListener(this);
            } else {
                holder.text.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.messages_g));
                holder.text.setOnClickListener(null);
            }
            if (listContainsId(radio, id)) {
                holder.radio.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.radio_w));
                holder.radio.setOnClickListener(this);
            } else {
                holder.radio.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.radio_g));
                holder.radio.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() {
            return ids.size();
        }

        class viewHolder extends RecyclerView.ViewHolder {
            TextView handle;
            ImageView photo, text, radio;

            viewHolder(View itemView) {
                super(itemView);
                handle = itemView.findViewById(R.id.black_handle_tv);
                photo = itemView.findViewById(R.id.photo);
                text = itemView.findViewById(R.id.text);
                radio = itemView.findViewById(R.id.radio);
            }
        }
    }
}

