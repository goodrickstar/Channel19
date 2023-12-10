package com.cb3g.channel19;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.android.multidex.myapplication.databinding.DriverBinding;
import com.vdurmont.emoji.EmojiParser;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.Instant;

import java.io.IOException;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class Driver extends Fragment {
    private DriverBinding binding;
    private Context context;
    private SI SI;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        SI = (SI) getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DriverBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!RadioService.operator.getDisableProfile()) {
            binding.driverProfilePictureIv.setOnClickListener(v -> {
                Utils.vibrate(v);
                context.sendBroadcast(new Intent("nineteenClickSound"));
                if (SI != null) SI.launchPicker(null, false);
            });
        }
        setDriverInfo();
        setRankAndStamp();
        updateProfilePicture();
        refreshRank();
        binding.blocked.setOnLongClickListener(v -> {
            if (SI != null) {
                context.sendBroadcast(new Intent("nineteenClickSound"));
                Utils.vibrate(v);
                SI.checkBlocked();
            }
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGhostStatus();
    }

    public void updateGhostStatus() {
        if (RadioService.operator.getSalutes() < 359 || RadioService.operator.getCount() < 1000) {
            binding.ghost.setVisibility(View.INVISIBLE);
            binding.status.setText("");
        } else {
            if (userIsGhost(RadioService.operator.getUser_id())) {
                binding.ghost.setVisibility(View.INVISIBLE);
                for (FBentry entry : RadioService.ghostUsers) {
                    if (entry.getUserId().equals(RadioService.operator.getUser_id())) {
                        String elapsed = Utils.showElapsed(entry.getInstant(), Instant.now());
                        binding.status.setText("Ghost Mode active for " + elapsed);
                    }
                }
            } else {
                binding.status.setText("");
                binding.ghost.setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean userIsGhost(String id) {
        for (FBentry entry : RadioService.ghostUsers) {
            if (entry.getUserId().equals(id)) return true;
        }
        return false;
    }

    public void updateProfilePicture() {
        new GlideImageLoader(context, binding.driverProfilePictureIv).load(RadioService.operator.getProfileLink(), RadioService.profileOptions);
    }

    public void setDriverInfo() {
        binding.driverHandleTv.setText(RadioService.operator.getHandle());
        binding.driverCarrierTv.setText(RadioService.operator.getCarrier());
        binding.driverBannerTv.setText(RadioService.operator.getTown());
        if (RadioService.operator.getSubscribed())
            binding.driverTitleTv.setText(EmojiParser.parseToUnicode(":heavy_plus_sign:"));
        if (!RadioService.operator.getUserLocationString().isEmpty())
            binding.driverBannerTv.setText(RadioService.operator.getUserLocationString());
    }

    public void setRankAndStamp() {
        if (isAdded())
            new GlideImageLoader(context, binding.driverStarIv).load(Utils.parseRankUrl(RadioService.operator.getRank()));
    }

    public void refreshRank() {
        final String data = Jwts.builder()
                .setHeader(RadioService.header)
                .claim("userId", RadioService.operator.getUser_id())
                .setIssuedAt(new Date(System.currentTimeMillis() - 240000))
                .setExpiration(new Date(System.currentTimeMillis() + 240000))
                .signWith(SignatureAlgorithm.HS256, RadioService.operator.getKey())
                .compact();
        final Request request = new Request.Builder()
                .url(RadioService.SITE_URL + "user_rank.php")
                .post(new FormBody.Builder().add("data", data).build())
                .build();
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        assert response.body() != null;
                        final JSONObject data = new JSONObject(response.body().string());
                        RadioService.operator.setRank(data.getString("rank"));
                        RadioService.operator.setStamp(data.getString("stamp").replace("\\", ""));
                        final int donations = data.getInt("donations");
                        if (isAdded()) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (donations > 4)
                                    binding.driverStarIv.setOnClickListener(v -> {
                                        Utils.vibrate(v);
                                        context.sendBroadcast(new Intent("nineteenClickSound"));
                                        Utils.vibrate(v);
                                    });
                                else binding.driverStarIv.setOnClickListener(null);
                                setRankAndStamp();
                            });
                        }
                    }
                } catch (JSONException e) {
                    Logger.INSTANCE.e(String.valueOf(e));
                }
            }
        });
    }
}
