package com.cb3g.channel19;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.android.multidex.myapplication.databinding.ActivityRewardBinding;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class ActivityReward extends AppCompatActivity {
    private ActivityRewardBinding binding;
    private String userId;
    private int tokens = 0;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRewardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        userId = getIntent().getStringExtra("userId");
        tokens = getIntent().getIntExtra("tokens", 0);
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-4635898093945616/6794827659", adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Logger.INSTANCE.i("onAdFailedToLoad()", loadAdError.getMessage());
                binding.adProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                Logger.INSTANCE.e("onAdLoaded()", "onAdLoaded");
                rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        Logger.INSTANCE.i("onAdShowedFullScreenContent()", "Ad was shown");
                        binding.adProgressBar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        Logger.INSTANCE.i("onAdFailedToShowFullScreenContent()", "Ad failed to show");
                        binding.adProgressBar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        onBackPressed();
                        Logger.INSTANCE.i("onAdDismissedFullScreenContent()", "Ad was dismissed");
                    }
                });
                rewardedAd.show(ActivityReward.this, rewardItem -> {
                    Logger.INSTANCE.i("reward " + (tokens + 1));
                    Utils.getTokens(userId).setValue(tokens + 1);
                });
            }
        });
    }


}
