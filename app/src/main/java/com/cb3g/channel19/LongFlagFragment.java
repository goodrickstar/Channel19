package com.cb3g.channel19;

import static android.content.Context.MODE_PRIVATE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.LongflagfayoutBinding;

public class LongFlagFragment extends DialogFragment {
    private LongflagfayoutBinding binding;
    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = LongflagfayoutBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.close2.setVisibility(View.INVISIBLE);
        binding.captiontext.setText("Courtesy of " + requireArguments().getString("data"));
        binding.birdView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.bird));
        flipCard(binding.birdView);
        binding.close2.setOnClickListener(v -> {
            Utils.vibrate(v);
            context.getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("flagDue", false).apply();
            dismiss();
        });
    }

    public void flipCard(final ImageView view) {
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("scaleX", .5f, 1f);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("scaleY", .5f, 1f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, pvhX, pvhY);
        animator.setRepeatCount(10);
        animator.setDuration(1000);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                binding.close2.setVisibility(View.VISIBLE);
            }
        });
        animator.start();
    }

}