package com.cb3g.channel19;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.android.multidex.myapplication.R;

import java.net.MalformedURLException;

public class CreatePost extends DialogFragment implements View.OnClickListener {
    private Context context;
    private RI RI;
    private ImageView photo_view;
    private EditText content;

    public CreatePost(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    private boolean upload = false;
    private Gif gif = null;

    private final FragmentManager fragmentManager;

    private final ActivityResultLauncher<String> postPhotoPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri != null) { //uri.toString()
            setPhoto(new Gif(uri.toString()), true);
        }
    });



    public void setPhoto(Gif photo, boolean upload) {
        if (photo != null) {
            this.upload = upload;
            gif = photo;
            photo_view.setVisibility(View.VISIBLE);
            Glide.with(context).load(gif.getUrl()).addListener(new RequestListener<>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                    return false;
                }

                @Override
                public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                    gif.setHeight(resource.getIntrinsicHeight());
                    gif.setWidth(resource.getIntrinsicWidth());
                    return false;
                }
            }).into(photo_view);
        } else Logger.INSTANCE.i("photo was null");
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        RI = (RI) getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.new_entry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        photo_view = view.findViewById(R.id.photo_view);
        final TextView cancel = view.findViewById(R.id.cancel);
        final TextView finish = view.findViewById(R.id.finish);
        content = view.findViewById(R.id.question);
        cancel.setOnClickListener(this);
        finish.setOnClickListener(this);
        final ImageView image_selector = view.findViewById(R.id.imageBox);
        final ImageView giphy_selector = view.findViewById(R.id.giphyBox);
        image_selector.setOnClickListener(this);
        giphy_selector.setOnClickListener(this);
        Utils.showKeyboard(context, content);
    }

    @Override
    public void onClick(View v) {
        context.sendBroadcast(new Intent("vibrate").setPackage("com.cb3g.channel19"));
        if (RI == null) return;
        int id = v.getId();
        if (id == R.id.imageBox) {
            if (Utils.permissionsAccepted(context, Utils.getStoragePermissions())) {
                postPhotoPicker.launch("image/*");
            }else {
                Utils.requestPermission(getActivity(), Utils.getStoragePermissions(), 0);
            }
        } else if (id == R.id.giphyBox) {
            ImageSearch imageSearch = (ImageSearch) fragmentManager.findFragmentByTag("imageSearch");
            if (imageSearch == null) {
                imageSearch = new ImageSearch("");
                imageSearch.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                imageSearch.show(fragmentManager, "imageSearch");
            }
        } else if (id == R.id.cancel) {
            dismiss();
        } else if (id == R.id.finish) {
            String content = "none";
            if (!this.content.getText().toString().trim().isEmpty())
                content = this.content.getText().toString().trim();
            if (content.equals("none") && gif == null)
                return;
            if (RI != null) {
                try {
                    RI.simple_post(gif, content, upload);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            this.content.setText("");
            photo_view.setVisibility(View.GONE);
            dismiss();
        }
    }
}
