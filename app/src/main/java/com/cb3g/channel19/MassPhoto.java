package com.cb3g.channel19;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.MassPhotoBinding;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MassPhoto extends DialogFragment implements View.OnClickListener {
    private final RecyclerViewAdapter adapter = new RecyclerViewAdapter();
    private MassPhotoBinding binding;
    private Context context;
    private List<String> savedIds = new ArrayList<>();
    private final List<MassPhotoUser> working = new ArrayList<>();
    private final String uri;

    public MassPhoto(String uri) {
        this.uri = uri;
    }

    private Drawable resource;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = MassPhotoBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Window window = Objects.requireNonNull(getDialog()).getWindow();
        if (window != null)
            window.getAttributes().windowAnimations = R.style.photoAnimation;
        RadioService.occupied.set(true);
        Glide.with(this).load(uri).listener(new RequestListener<>() {

            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                dismiss();
                return false;
            }

            @Override
            public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                MassPhoto.this.resource = resource;
                return false;
            }
        }).into(binding.photoPreview);
        Set<String> set = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getStringSet("massIds", null);
        if (set != null) savedIds = new ArrayList<>(set);
        RecyclerView recyclerView = view.findViewById(R.id.multi_select);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setAlpha(0);
        TextView send = view.findViewById(R.id.close);
        final TextView cancel = view.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);
        send.setVisibility(View.GONE);
        send.setOnClickListener(this);
        binding.selector.setOnClickListener(this);
        for (User user : RadioService.users) {
            working.add(convertUserListEntryToUser(user));
        }
        adapter.notifyDataSetChanged();
        recyclerView.animate().alpha(1.0f).setDuration(800);
        binding.photoPreview.animate().alpha(.5f).setDuration(800);
        send.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
        int id = v.getId();
        if (id == R.id.close) {
            Set<String> set = new HashSet<>(savedIds);
            Log.i("logging", "Sending Photo");
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putStringSet("massIds", set).apply();
            final ArrayList<String> sendingIds = new ArrayList<>();
            for (MassPhotoUser user : working) {
                if (user.isChecked) sendingIds.add(user.id);
            }
            if (RadioService.operator.getAdmin())
                sendingIds.add(RadioService.operator.getUser_id());
            if (!sendingIds.isEmpty() && resource != null) {
                FileUpload upload = new FileUpload(RequestCode.MASS_PHOTO, sendingIds, new Photo(Utils.getKey(), uri, resource.getIntrinsicHeight(), resource.getIntrinsicWidth(), Utils.UTC(), RadioService.operator.getUser_id(), RadioService.operator.getProfileLink(), RadioService.operator.getHandle(), RadioService.operator.getRank()));
                Uploader uploader = new Uploader(context, RadioService.operator, RadioService.client, upload, RadioService.operator.getHandle());
                uploader.uploadImage();
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

    private MassPhotoUser convertUserListEntryToUser(User user) {
        return new MassPhotoUser(user.getUser_id(), user.getRadio_hanlde(), user.getProfileLink(), savedIds.contains(user.getUser_id()));
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages").setPackage("com.cb3g.channel19"));
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages").setPackage("com.cb3g.channel19"));
    }

    private boolean allChecked() {
        for (MassPhotoUser user : working) {
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
            final MassPhotoUser user = working.get(position);
            holder.handle.setText(user.handle);
            if (user.isChecked) {
                holder.profile.setImageResource(R.drawable.sender);
            } else {
                Glide.with(context).load(user.profileLink).apply(RadioService.profileOptions).transition(withCrossFade()).into(holder.profile);
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
            binding.selector.setChecked(allChecked());
        }

        private boolean allChecked() {
            for (MassPhotoUser user : working) {
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
                handle = itemView.findViewById(R.id.black_handle_tv);
                profile = itemView.findViewById(R.id.black_profile_picture_iv);
            }
        }
    }

    private static class MassPhotoUser {
        private final String id;
        private final String handle;
        private final String profileLink;
        private boolean isChecked;

        private MassPhotoUser(String id, String handle, String profileLink, boolean checked) {
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

