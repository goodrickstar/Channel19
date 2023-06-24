package com.cb3g.channel19;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;

public class GifShowCase extends DialogFragment implements View.OnClickListener{
    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.gif_showcase, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ImageView content = view.findViewById(R.id.content);
        ConstraintLayout container = view.findViewById(R.id.container);
        content.setOnClickListener(this);
        container.setOnClickListener(this);
        new GlideImageLoader(context, content, view.findViewById(R.id.loading)).load(requireArguments().getString("data"));
    }

    @Override
    public void onClick(View view) {
        dismiss();
    }
}
