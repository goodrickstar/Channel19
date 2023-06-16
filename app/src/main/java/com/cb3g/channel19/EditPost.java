package com.cb3g.channel19;

import static com.cb3g.channel19.ReservoirActivity.channelReservoirReference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

public class EditPost extends DialogFragment {
    private Context context;
    private InputMethodManager methodManager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Window window = getDialog().getWindow();
        if (window != null) window.getAttributes().windowAnimations = R.style.photoAnimation;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Window window = getDialog().getWindow();
        if (window != null) window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return inflater.inflate(R.layout.edit_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final TextView editTitle = view.findViewById(R.id.edit_post_title);
        final TextView cancel = view.findViewById(R.id.cancel);
        final TextView finish = view.findViewById(R.id.finish);
        editTitle.setText(getArguments().getString("title"));
        final EditText editText = view.findViewById(R.id.question);
        String content = getArguments().getString("content");
        if (content.equals("none")) content = "";
        editText.append(content);
        editText.setSelection(editText.getText().length());
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.sendBroadcast(new Intent("vibrate"));
                if (v.getId() == R.id.finish) {
                    final String postId = getArguments().getString("postId");
                    final String remarkId = getArguments().getString("remarkId");
                    String content = editText.getText().toString().trim();
                    if (remarkId.equals("default")) {
                        //edit post
                        if (content.isEmpty()) content = "none";
                        channelReservoirReference.child("posts").child(postId).child("caption").setValue(content);
                    } else {
                        //edit remark
                        if (content.isEmpty()) {
                            //delete remark then update post to show new latest remark
                            channelReservoirReference.child("remarks").child(postId).child(remarkId).removeValue();
                            //return post Object to update
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
                            //update remark then update post to reflect
                            channelReservoirReference.child("remarks").child(postId).child(remarkId).child("content").setValue(content);
                            //return post Object to update

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
            }
        };
        cancel.setOnClickListener(listener);
        finish.setOnClickListener(listener);
        methodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        methodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        methodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }
}
