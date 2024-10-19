package com.cb3g.channel19;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.CommentsDialogBinding;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import me.shaohui.advancedluban.Luban;
import me.shaohui.advancedluban.OnCompressListener;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class Comments extends DialogFragment implements ChildEventListener, View.OnClickListener {
    private final CommentAdapter commentAdapter = new CommentAdapter();
    private final List<Comment> comments = new ArrayList<>();
    private EditText editBox;
    private com.cb3g.channel19.RI RI;
    private Context context;
    private Post post;
    private CommentsDialogBinding binding;
    private GlideImageLoader glideImageLoader;

    private final FragmentManager fragmentManager;

    public Comments(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    private final ActivityResultLauncher<String> commentPhotoPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri != null) {
            photo_remark(uri.toString());
        }
    });

    private void snapToPosition(int position) {
        RecyclerView.SmoothScroller smoothScroller = new
                LinearSmoothScroller(context) {
                    @Override
                    protected int getVerticalSnapPreference() {
                        return LinearSmoothScroller.SNAP_TO_START;
                    }
                };
        smoothScroller.setTargetPosition(position);
        Objects.requireNonNull(binding.commentsListView.getLayoutManager()).startSmoothScroll(smoothScroller);
    }

    @Override
    public void onClick(View v) {
        context.sendBroadcast(new Intent("vibrate").setPackage("com.cb3g.channel19"));
        int id = v.getId();
        if (id == R.id.imageBox) {
            if (editBox.length() > 0) {
                text_remark(editBox.getText().toString().trim());
                editBox.setText("");
            } else commentPhotoPicker.launch("image/*");
        }
        if (id == R.id.giphyBox) {
            ImageSearch imageSearch = (ImageSearch) fragmentManager.findFragmentByTag("imageSearch");
            if (imageSearch == null) {
                imageSearch = new ImageSearch("");
                imageSearch.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                imageSearch.show(fragmentManager, "imageSearch");
            }
        }
    }

    @Override
    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        comments.add(dataSnapshot.getValue(Comment.class));
        commentAdapter.notifyItemInserted(comments.size() - 1);
        snapToPosition(comments.size() - 1);
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        Comment comment = dataSnapshot.getValue(Comment.class);
        for (int i = 0; i < comments.size(); i++) {
            assert comment != null;
            if (comments.get(i).getRemarkId().equals(comment.getRemarkId())) {
                comments.set(i, comment);
                commentAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
        Comment comment = dataSnapshot.getValue(Comment.class);
        for (int i = 0; i < comments.size(); i++) {
            assert comment != null;
            if (comments.get(i).getRemarkId().equals(comment.getRemarkId())) {
                comments.remove(i);
                commentAdapter.notifyItemRemoved(i);
            }
        }
    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        RI = (com.cb3g.channel19.RI) getActivity();
        glideImageLoader = new GlideImageLoader(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Window window = Objects.requireNonNull(getDialog()).getWindow();
        if (window != null) window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        binding = CommentsDialogBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final TextView title = view.findViewById(R.id.black_title_tv);
        final ImageView back = view.findViewById(R.id.back);
        title.setText(R.string.comments);
        back.setImageResource(R.drawable.upbutton);
        back.setOnClickListener(v -> {
            context.sendBroadcast(new Intent("vibrate").setPackage("com.cb3g.channel19"));
            dismiss();
        });
        post = RadioService.gson.fromJson(requireArguments().getString("post"), Post.class);
        binding.commentsListView.setHasFixedSize(true);
        binding.commentsListView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        binding.commentsListView.setAdapter(commentAdapter);
        editBox = view.findViewById(R.id.editBox);
        if (!RadioService.operator.getBlockedFromReservoir()) {
            final ImageView image_selector = view.findViewById(R.id.imageBox);
            final ImageView giphy_selector = view.findViewById(R.id.giphyBox);
            image_selector.setOnClickListener(this);
            giphy_selector.setOnClickListener(this);
            editBox.setHint(getResources().getString(R.string.leave_a_remark));
            editBox.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() != 0) image_selector.setImageResource(R.drawable.send);
                    else image_selector.setImageResource(R.drawable.gallery);
                }
            });
        }
        RI.databaseReference().child("remarks").child(post.getPostId()).addChildEventListener(this);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        RI.databaseReference().child("remarks").child(post.getPostId()).removeEventListener(this);
    }

    private void commentNotification(final String text, final String extra) {
        List<String> others = new ArrayList<>();
        for (Comment comment : comments) {
            if (!comment.getUserId().equals(post.getFacebookId()) && !comment.getUserId().equals(RadioService.operator.getUser_id()) && !others.contains(comment.getUserId()))
                others.add(comment.getUserId());
        }
        final Map<String, Object> claims = new HashMap<>();
        claims.put("to", post.getFacebookId());
        claims.put("from", RadioService.operator.getUser_id());
        claims.put("handle", RadioService.operator.getHandle());
        claims.put("owner_handle", post.getHandle());
        claims.put("others", others);
        claims.put("text", text);
        claims.put("extra", extra);
        new OkUtil().call("user_comment_notification.php", claims);
        //TODO: show interaction with post to others
    }

    private void text_remark(String content) {
        content = content.replaceAll("\\s+", " ").trim();
        if (content.length() == 0) return;
        final Comment comment = new Comment();
        comment.setPostId(post.getPostId());
        comment.setRemarkId(Objects.requireNonNull(RI.databaseReference().child("remarks").child(post.getPostId()).push().getKey()));
        comment.setContent(content);
        comment.setUserId(RadioService.operator.getUser_id());
        comment.setProfileLink(RadioService.operator.getProfileLink());
        comment.setHandle(RadioService.operator.getHandle());
        comment.setPost_date(RI.return_timestamp_string());
        RI.databaseReference().child("remarks").child(post.getPostId()).child(comment.getRemarkId()).setValue(comment);
        update_latest_comment(content);
        commentNotification(content, "none");
    }

    public void giphy_remark(final Gif gif) {
        if (gif != null) {
            final Comment comment = new Comment();
            comment.setPostId(post.getPostId());
            comment.setRemarkId(Objects.requireNonNull(RI.databaseReference().child("remarks").child(post.getPostId()).push().getKey()));
            comment.setType(1);
            comment.setPost_date(RI.return_timestamp_string());
            comment.setUserId(RadioService.operator.getUser_id());
            comment.setProfileLink(RadioService.operator.getProfileLink());
            comment.setHandle(RadioService.operator.getHandle());
            comment.setImage_height(gif.getHeight());
            comment.setImage_width(gif.getWidth());
            comment.setContent(gif.getUrl());
            RI.databaseReference().child("remarks").child(post.getPostId()).child(comment.getRemarkId()).setValue(comment);
            update_latest_comment("Commented with a giphy.");
            commentNotification("none", "Commented on your post with a Giphy");
        }
    }

    public void photo_remark(final String url) {
        if (url != null) {
            final Comment comment = new Comment();
            comment.setPostId(post.getPostId());
            comment.setRemarkId(Objects.requireNonNull(RI.databaseReference().child("remarks").child(post.getPostId()).push().getKey()));
            comment.setType(1);
            comment.setPost_date(RI.return_timestamp_string());
            comment.setUserId(RadioService.operator.getUser_id());
            comment.setProfileLink(RadioService.operator.getProfileLink());
            comment.setHandle(RadioService.operator.getHandle());
            Luban.compress(context, RI.returnFileFromUri(url, comment.getRemarkId() + RI.returnFileTypeFromUri(url)))
                    .putGear(Luban.THIRD_GEAR)
                    .launch(new OnCompressListener() {
                        @Override
                        public void onStart() {
                        }

                        @Override
                        public void onSuccess(final File file) {
                            final StorageReference ref = RI.storageReference().child(file.getName());
                            UploadTask uploadTask = ref.putFile(Uri.fromFile(file));
                            uploadTask.continueWithTask(task -> {
                                if (!task.isSuccessful()) {
                                    throw Objects.requireNonNull(task.getException());
                                }
                                return ref.getDownloadUrl();
                            }).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Uri downloadUri = task.getResult();
                                    comment.setContent(downloadUri.toString());
                                    update_latest_comment("Commented with a photo.");
                                    editBox.setText("");
                                    ExecutorService executor = ExecutorUtils.newSingleThreadExecutor();
                                    executor.execute(() -> {
                                        try {
                                            Bitmap bitmap = BitmapFactory.decodeStream(new URL(downloadUri.toString()).openConnection().getInputStream());
                                            if (bitmap != null) {
                                                requireActivity().runOnUiThread(() -> {
                                                    comment.setImage_height(bitmap.getHeight());
                                                    comment.setImage_width(bitmap.getWidth());
                                                    bitmap.recycle();
                                                    RI.databaseReference().child("remarks").child(post.getPostId()).child(comment.getRemarkId()).setValue(comment);
                                                });
                                            }
                                        } catch (IOException e) {
                                            Logger.INSTANCE.e("IOException", e.getMessage());
                                            Logger.INSTANCE.e("MalformedURLException", e.getMessage());
                                            comment.setImage_height(500);
                                            comment.setImage_width(500);
                                            RI.databaseReference().child("remarks").child(post.getPostId()).child(comment.getRemarkId()).setValue(comment);
                                        }
                                    });
                                }
                            });
                        }

                        @Override
                        public void onError(Throwable e) {
                            Logger.INSTANCE.e("LUBAN ERROR " + e);
                        }
                    });
            commentNotification("none", "Commented on your post with a photo");
        }
    }

    private void update_latest_comment(final String content) {
        RI.databaseReference().child("posts").child(post.getPostId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final Post post = dataSnapshot.getValue(Post.class);
                assert post != null;
                post.setRemarks(post.getRemarks() + 1);
                post.setLatest_handle(RadioService.operator.getHandle());
                post.setLatest_profileLink(RadioService.operator.getProfileLink());
                post.setLatest_facebookId(RadioService.operator.getUser_id());
                post.setLatest_remark(content.replaceAll("\\s+", " ").trim());
                RI.databaseReference().child("posts").child(post.getPostId()).setValue(post);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public void delete_remark(final Comment comment) {
        Log.i("test", new Gson().toJson(comment));
        try {
            RI.databaseReference().child("remarks").child(post.getPostId()).child(comment.getRemarkId()).removeValue(); //delete the selected comment
            if (comment.getType() == 1)
                FirebaseStorage.getInstance().getReferenceFromUrl(comment.getContent()).delete(); //delete the selected comment image if need be
        } catch (IllegalArgumentException e) {
            Log.e("error", "delete_remark() IllegalArgumentException " + e);
        }
        RI.databaseReference().child("remarks").child(post.getPostId()).limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        final Comment comment = child.getValue(Comment.class);
                        assert comment != null;
                        post.setRemarks(post.getRemarks() - 1);
                        post.setLatest_handle(comment.getHandle());
                        post.setLatest_profileLink(comment.getProfileLink());
                        post.setLatest_facebookId(comment.getUserId());
                        if (comment.getType() == 0)
                            post.setLatest_remark(comment.getContent());
                        else post.setLatest_remark("Commented with a photo");
                    }
                } else {
                    post.setRemarks(0);
                    post.setLatest_handle("none");
                    post.setLatest_profileLink("none");
                    post.setLatest_remark("none");
                    post.setLatest_facebookId("none");
                }
                RI.databaseReference().child("posts").child(post.getPostId()).setValue(post);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private class CommentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                return new TextHolder(LayoutInflater.from(context).inflate(R.layout.text_row, parent, false));
            }
            return new PhotoHolder(LayoutInflater.from(context).inflate(R.layout.image_row, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            final Comment comment = comments.get(position);
            switch (holder.getItemViewType()) {
                case 0 -> {
                    //TEXT
                    TextHolder text_holder = (TextHolder) holder;
                    text_holder.content.setText(comment.getContent());
                    glideImageLoader.load(text_holder.profile, comment.getProfileLink(), RadioService.profileOptions);
                    text_holder.profile.setOnClickListener(v -> {
                        Utils.vibrate(v);
                        if (RI != null) RI.action_view(comment.getProfileLink());
                    });
                    text_holder.name.setText(comment.getHandle());
                    text_holder.stamp.setText(Utils.showElapsed(comment.getStamp()));
                    text_holder.menu.setOnClickListener(v -> {
                        context.sendBroadcast(new Intent("vibrate").setPackage("com.cb3g.channel19"));
                        PopupMenu popupMenu = new PopupMenu(context, v, Gravity.END, 0, R.style.PopupMenu);
                        if (comment.getUserId().equals(RadioService.operator.getUser_id()))
                            popupMenu.getMenu().add(1, R.id.edit_remark, 2, "Edit");
                        if (comment.getUserId().equals(RadioService.operator.getUser_id()) || RadioService.operator.getAdmin())
                            popupMenu.getMenu().add(1, R.id.delete_remark, 2, "Delete");
                        popupMenu.setOnMenuItemClickListener(item -> {
                            context.sendBroadcast(new Intent("vibrate").setPackage("com.cb3g.channel19"));
                            int id = item.getItemId();
                            if (id == R.id.delete_remark) {
                                delete_remark(comment);
                            } else if (id == R.id.edit_remark) {
                                EditPost epd = (EditPost) fragmentManager.findFragmentByTag("epd");
                                if (epd == null) {
                                    epd = new EditPost();
                                    Bundle bundle = new Bundle();
                                    bundle.putString("title", "Edit Comment");
                                    bundle.putString("postId", comment.getPostId());
                                    bundle.putString("remarkId", comment.getRemarkId());
                                    bundle.putString("content", comment.getContent());
                                    epd.setArguments(bundle);
                                    epd.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.DialogTheme);
                                    epd.show(fragmentManager, "epd");
                                }

                            }
                            return true;
                        });
                        popupMenu.show();
                    });
                }
                case 1 -> {
                    //IMAGE
                    LOG.i("PHOTO");
                    PhotoHolder photo_holder = (PhotoHolder) holder;
                    photo_holder.name.setText(comment.getHandle());
                    photo_holder.stamp.setText(Utils.showElapsed(comment.getStamp()));
                    glideImageLoader.load(photo_holder.profile, comment.getProfileLink(), RadioService.profileOptions);
                    photo_holder.content.getLayoutParams().height = (int) (((comment.getImage_height() * ReservoirActivity.screen_width) / comment.getImage_width()) * 0.8);
                    photo_holder.content.getLayoutParams().width = (int) (ReservoirActivity.screen_width * 0.8);
                    glideImageLoader.load(photo_holder.content, photo_holder.loading, comment.getContent());
                    photo_holder.content.setOnClickListener(v -> {
                        Utils.vibrate(v);
                        if (RI != null) RI.action_view(comment.getContent());
                    });
                    photo_holder.profile.setOnClickListener(v -> {
                        Utils.vibrate(v);
                        if (RI != null)
                            RI.action_view(comment.getProfileLink());
                    });
                    photo_holder.menu.setOnClickListener(v -> {
                        context.sendBroadcast(new Intent("vibrate").setPackage("com.cb3g.channel19"));
                        PopupMenu popupMenu = new PopupMenu(context, v, Gravity.END, 0, R.style.PopupMenu);
                        popupMenu.getMenu().add(1, R.id.save_remark, 1, "Save Image");
                        if (comment.getUserId().equals(RadioService.operator.getUser_id()) || RadioService.operator.getAdmin())
                            popupMenu.getMenu().add(1, R.id.delete_remark, 2, "Delete");
                        popupMenu.setOnMenuItemClickListener(item -> {
                            Log.i("test", "menu item selected");
                            context.sendBroadcast(new Intent("vibrate").setPackage("com.cb3g.channel19"));
                            if (item.getItemId() == R.id.save_remark) {
                                context.sendBroadcast(new Intent("savePhotoToDisk").setPackage("com.cb3g.channel19").putExtra("url", comment.getContent()));
                            } else if (item.getItemId() == R.id.delete_remark) {
                                Log.i("test", "delete called");
                                delete_remark(comment);
                            }
                            return true;
                        });
                        popupMenu.show();
                    });
                }
            }
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        @Override
        public int getItemViewType(int position) {
            return comments.get(position).getType();
        }

        private class TextHolder extends RecyclerView.ViewHolder {
            TextView name, content, stamp;
            ImageView profile, menu;

            TextHolder(View v) {
                super(v);
                name = v.findViewById(R.id.profile_name);
                content = v.findViewById(R.id.text_content);
                stamp = v.findViewById(R.id.profile_stamp);
                profile = v.findViewById(R.id.profile_pic);
                menu = v.findViewById(R.id.profile_menu);
            }
        }

        private class PhotoHolder extends RecyclerView.ViewHolder {
            TextView name, stamp;
            ImageView profile, menu, content;
            ProgressBar loading;

            PhotoHolder(View v) {
                super(v);
                name = v.findViewById(R.id.profile_name);
                stamp = v.findViewById(R.id.profile_stamp);
                profile = v.findViewById(R.id.profile_pic);
                content = v.findViewById(R.id.image_content);
                menu = v.findViewById(R.id.profile_menu);
                loading = v.findViewById(R.id.loading);
            }
        }
    }
}
