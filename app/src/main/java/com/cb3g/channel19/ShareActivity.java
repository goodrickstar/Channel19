package com.cb3g.channel19;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.ActivityShareLayoutBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kedia.ogparser.OpenGraphCacheProvider;
import com.kedia.ogparser.OpenGraphCallback;
import com.kedia.ogparser.OpenGraphParser;
import com.kedia.ogparser.OpenGraphResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ShareActivity extends FragmentActivity {
    private ActivityShareLayoutBinding binding;
    private final GlideImageLoader glide = new GlideImageLoader(this);

    private void share(List<ShareTarget> shareTargets) {
        if (!shareTargets.isEmpty()) {
            //TODO: Setup sharing server side
            //Work manager?
            finish();
        } else Toast.makeText(this, "None Selected", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShareLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        String data = getSharedPreferences("sharing", Context.MODE_PRIVATE).getString("targets", null);
        if (data != null) {
            List<ShareTarget> shareTargets = new Gson().fromJson(data, new TypeToken<List<ShareTarget>>() {
            }.getType());
            if (!shareTargets.isEmpty()) {
                ShareAdapter adapter = new ShareAdapter(shareTargets);
                LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
                binding.shareRecycler.setLayoutManager(layoutManager);
                binding.shareRecycler.setHasFixedSize(true);
                binding.shareRecycler.setAdapter(adapter);

                binding.sendButton.setOnClickListener(view -> {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                    List<ShareTarget> sharing = new ArrayList<>();
                    for(ShareTarget unit : adapter.getTargets()){
                        if (unit.getSelected()) sharing.add(unit);
                    }
                    share(sharing);
                });
            } else {
                Toast.makeText(this, "No Recipients", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Toast.makeText(this, "No Reciepients", Toast.LENGTH_LONG).show();
            finish();
        }
        final Intent intent = getIntent();
        binding.shareRecycler.setAlpha(0f);
        binding.sendButton.setAlpha(0f);
        binding.description.setVisibility(View.GONE);
        if ("android.intent.action.SEND".equals(Objects.requireNonNull(intent.getAction()))) {
            switch (Objects.requireNonNull(intent.getType())) {
                case "text/plain":
                    String text = intent.getStringExtra("android.intent.extra.TEXT");
                    if (text != null) {
                        OpenGraphParser og = new OpenGraphParser(new OpenGraphCallback() {
                            @Override
                            public void onPostResponse(@NonNull OpenGraphResult openGraphResult) {
                                String title = openGraphResult.getTitle();
                                String description = openGraphResult.getDescription();
                                String imageLink = openGraphResult.getImage();
                                Log.i("logging", title + " " + description);
                                if (imageLink != null)
                                    glide.load(binding.imagePreview, imageLink, new RequestListener<>() {
                                        @Override
                                        public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                                            binding.progressBarLoading.setVisibility(View.INVISIBLE);
                                            return false;
                                        }

                                        @Override
                                        public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                                            binding.progressBarLoading.setVisibility(View.INVISIBLE);
                                            binding.description.setVisibility(View.VISIBLE);
                                            binding.description.setText(description);
                                            binding.shareRecycler.animate().alpha(1.0f).setDuration(500);
                                            binding.sendButton.animate().alpha(1.0f).setDuration(500);
                                            return false;
                                        }
                                    });
                            }

                            @Override
                            public void onError(@NonNull String s) {
                                Log.e("image", "OGP " + s);
                            }
                        }, true, new OpenGraphCacheProvider(this));
                        og.parse(text);
                    }
                    break;
                case "image/jpeg":
                    Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (imageUri != null) {
                        glide.load(binding.imagePreview, imageUri.toString(), new RequestListener<>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                                binding.progressBarLoading.setVisibility(View.INVISIBLE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                                binding.progressBarLoading.setVisibility(View.INVISIBLE);
                                binding.shareRecycler.animate().alpha(1.0f).setDuration(500);
                                binding.sendButton.animate().alpha(1.0f).setDuration(500);
                                return false;
                            }
                        });
                    }
                    break;
            }
        }
    }

    class ShareAdapter extends RecyclerView.Adapter<ShareAdapter.ShareHolder> {
        private final List<ShareTarget> shareTargets;

        public ShareAdapter(List<ShareTarget> shareTargets) {
            this.shareTargets = shareTargets;
        }

        @NonNull
        @Override
        public ShareHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ShareHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.mass_photo_row, parent, false));
        }

        @Override
        public void onBindViewHolder(final ShareHolder holder, final int position) {
            ShareTarget shareTarget = shareTargets.get(position);
            holder.handle.setText(shareTarget.getHandle());
            if (shareTarget.getSelected()) {
                holder.profile.setImageResource(R.drawable.sender);
            } else {
                Glide.with(ShareActivity.this).load(shareTarget.getProfileLink()).apply(new RequestOptions().circleCrop()).transition(withCrossFade()).into(holder.profile);
            }
            holder.itemView.setOnClickListener(v -> {
                Utils.vibrate(v);
                shareTarget.setSelected(!shareTarget.getSelected());
                shareTargets.set(position, shareTarget);
                this.notifyItemChanged(position);
            });
        }

        public List<ShareTarget> getTargets() {
            return shareTargets;
        }

        @Override
        public int getItemCount() {
            return shareTargets.size();
        }

        static class ShareHolder extends RecyclerView.ViewHolder {
            TextView handle;
            ImageView profile;

            private ShareHolder(View itemView) {
                super(itemView);
                handle = itemView.findViewById(R.id.black_handle_tv);
                profile = itemView.findViewById(R.id.black_profile_picture_iv);
            }
        }
    }
}
