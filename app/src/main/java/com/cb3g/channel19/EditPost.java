package com.cb3g.channel19;
import static com.cb3g.channel19.ReservoirActivity.channelReservoirReference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.EditPostBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class EditPost extends DialogFragment {
    private Context context;
    private EditPostBinding binding;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Window window = getDialog().getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.getAttributes().windowAnimations = R.style.photoAnimation;
        }
        binding = EditPostBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.editPostTitle.setText(getArguments().getString("title"));
        String content = getArguments().getString("content");
        if (content.equals("none")) content = "";
        binding.question.append(content);
        binding.question.setSelection(binding.question.getText().length());
        View.OnClickListener listener = v -> {
            context.sendBroadcast(new Intent("vibrate"));
            if (v.getId() == R.id.finish) {
                final String postId = getArguments().getString("postId");
                final String remarkId = getArguments().getString("remarkId");
                String content1 = binding.question.getText().toString().trim();
                if (remarkId.equals("default")) {
                    if (content1.isEmpty()) content1 = "none";
                    channelReservoirReference.child("posts").child(postId).child("caption").setValue(content1);
                } else {
                    if (content1.isEmpty()) {
                        channelReservoirReference.child("remarks").child(postId).child(remarkId).removeValue();
                        channelReservoirReference.child("posts").child(postId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                final Post post = dataSnapshot.getValue(Post.class);
                                //return last remark for given post
                                channelReservoirReference.child("remarks").child(postId).limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.hasChildren()) {
                                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                                final Comment comment = child.getValue(Comment.class);
                                                post.setRemarks(post.getRemarks() - 1);
                                                post.setLatest_handle(comment.getHandle());
                                                post.setLatest_profileLink(comment.getProfileLink());
                                                if (comment.getType() == 0)
                                                    post.setLatest_remark(comment.getContent());
                                                else post.setLatest_remark("Commented with a photo");
                                            }
                                        } else {
                                            post.setRemarks(0);
                                            post.setLatest_handle("none");
                                            post.setLatest_profileLink("none");
                                            post.setLatest_remark("none");
                                        }
                                        channelReservoirReference.child("posts").child(postId).setValue(post);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    } else {
                        channelReservoirReference.child("remarks").child(postId).child(remarkId).child("content").setValue(content1);
                        channelReservoirReference.child("posts").child(postId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                final Post post = dataSnapshot.getValue(Post.class);
                                channelReservoirReference.child("remarks").child(postId).limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.hasChildren()) {
                                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                                final Comment comment = child.getValue(Comment.class);
                                                post.setLatest_handle(comment.getHandle());
                                                post.setLatest_profileLink(comment.getProfileLink());
                                                if (comment.getType() == 0)
                                                    post.setLatest_remark(comment.getContent());
                                                else post.setLatest_remark("Commented with a photo");
                                            }
                                        }
                                        channelReservoirReference.child("posts").child(postId).setValue(post);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    }
                }
            }
            dismiss();
        };
        binding.cancel.setOnClickListener(listener);
        binding.finish.setOnClickListener(listener);
        Utils.showKeyboard(context, binding.editPostTitle);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        Utils.hideKeyboard(context, binding.editPostTitle);
    }
}
