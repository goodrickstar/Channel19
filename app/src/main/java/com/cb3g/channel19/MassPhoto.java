package com.cb3g.channel19;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.android.multidex.myapplication.R;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;
public class MassPhoto extends DialogFragment implements View.OnClickListener {
    private final RecyclerViewAdapter adapter = new RecyclerViewAdapter();
    private Context context;
    private List<String> savedIds = new ArrayList<>();
    private final List<User> working = new ArrayList<>();
    private RecyclerView recyclerView;
    private String uri;
    private TextView send;
    private ImageView preview;
    private RadioButton selector;
    private MI MI;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Window window = requireDialog().getWindow();
        if (window != null) window.setGravity(Gravity.CENTER);
        return inflater.inflate(R.layout.mass_photo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Window window = Objects.requireNonNull(getDialog()).getWindow();
        if (window != null)
            window.getAttributes().windowAnimations = R.style.photoAnimation;
        RadioService.occupied.set(true);
        preview = view.findViewById(R.id.photo_preview);
        selector = view.findViewById(R.id.selector);
        uri = requireArguments().getString("data");
        Glide.with(this).load(uri).into(preview);
        Set<String> set = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getStringSet("massIds", null);
        if (set != null) savedIds = new ArrayList<>(set);
        recyclerView = view.findViewById(R.id.multi_select);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setAlpha(0);
        send = view.findViewById(R.id.close);
        final TextView cancel = view.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);
        send.setVisibility(View.GONE);
        send.setOnClickListener(this);
        selector.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        context.sendBroadcast(new Intent("nineteenClickSound"));
        int id = v.getId();
        if (id == R.id.close) {
            Set<String> set = new HashSet<>(savedIds);
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putStringSet("massIds", set).apply();
            final List<String> sendingIds = new ArrayList<>();
            for (User user : working) {
                if (user.isChecked) sendingIds.add(user.id);
            }
            if (!sendingIds.isEmpty()) {
                context.sendBroadcast(new Intent("upload").putExtra("uri", uri).putExtra("mode", 3737).putExtra("caption", "").putExtra("sendToId", RadioService.gson.toJson(sendingIds)).putExtra("sendToHandle", "").putExtra("height", preview.getHeight()).putExtra("width", preview.getWidth()));
                if (MI != null)
                    MI.showSnack(new Snack("Mass Photo Sent", Snackbar.LENGTH_SHORT));
                dismiss();
            }
        } else if (id == R.id.selector) {
            if (allChecked()) {
                for (int x = 0; x < working.size(); x++) {
                    working.get(x).setChecked(false);
                }
            } else {
                for (int x = 0; x < working.size(); x++) {
                    working.get(x).setChecked(true);
                }
            }
            adapter.notifyDataSetChanged();
        } else dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        final String data = Jwts.builder()
                .setHeader(RadioService.header)
                .claim("channel", Objects.requireNonNull(RadioService.operator.getChannel()).getChannel())
                .claim("userId", RadioService.operator.getUser_id())
                .signWith(SignatureAlgorithm.HS256, RadioService.operator.getKey())
                .compact();
        final Request request = new Request.Builder()
                .url(RadioService.SITE_URL + "user_mass_photo_list.php")
                .post(new FormBody.Builder().add("data", data).build())
                .build();
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful() && isAdded()) {
                    assert response.body() != null;
                    final String data = response.body().string();
                    recyclerView.post(() -> {
                        try {
                            working.clear();
                            JSONArray users = new JSONArray(data);
                            for (int x = 0; x < users.length(); x++) {
                                JSONObject user = users.getJSONObject(x);
                                final String userId = user.getString("user_id");
                                if (!userIsGhost(userId))
                                    working.add(new User(userId, user.getString("radio_hanlde"), user.getString("profileLink"), savedIds.contains(userId)));
                            }
                            adapter.notifyDataSetChanged();
                            recyclerView.post(() -> {
                                recyclerView.animate().alpha(1.0f).setDuration(1200);
                                send.setVisibility(View.VISIBLE);
                            });
                        } catch (JSONException e) {
                            LOG.e(e.getMessage());
                        }
                    });
                }
            }
        });
    }

    private boolean userIsGhost(String id) {
        for (FBentry entry : RadioService.ghostUsers) {
            if (entry.getUserId().equals(id)) return true;
        }
        return false;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }

    private boolean allChecked() {
        for (User user : working) {
            if (!user.isChecked) return false;
        }
        return true;
    }

    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.Holder> {
        @NotNull
        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.mass_photo_row, parent, false));
        }

        @Override
        public void onBindViewHolder(final Holder holder, final int position) {
            final User user = working.get(position);
            holder.handle.setText(user.handle);
            if (user.isChecked) {
                holder.profile.setImageResource(R.drawable.sender);
            } else {
                Glide.with(context).load(user.profileLink).apply(RadioService.profileOptions).thumbnail(0.1f).transition(withCrossFade()).into(holder.profile);
            }
            holder.itemView.setOnClickListener(v -> {
                user.isChecked = !user.isChecked;
                Utils.vibrate(v);
                if (user.isChecked) {
                    if (!savedIds.contains(user.id)) savedIds.add(user.id);
                } else savedIds.remove(user.id);
                adapter.notifyItemChanged(holder.getAdapterPosition());
                questionChecks();
            });
            questionChecks();
        }

        private void questionChecks() {
            selector.setChecked(allChecked());
        }

        private boolean allChecked() {
            for (User user : working) {
                if (!user.isChecked) return false;
            }
            return true;
        }

        @Override
        public int getItemCount() {
            return working.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView handle;
            ImageView profile;

            private Holder(View itemView) {
                super(itemView);
                handle = itemView.findViewById(R.id.handle);
                profile = itemView.findViewById(R.id.option_image_view);
            }
        }
    }
    private static class User {
        private final String id;
        private final String handle;
        private final String profileLink;
        private boolean isChecked;

        private User(String id, String handle, String profileLink, boolean checked) {
            this.id = id;
            this.isChecked = checked;
            this.handle = handle;
            this.profileLink = profileLink;
        }

        public void setChecked(boolean checked) {
            isChecked = checked;
        }
    }
}

