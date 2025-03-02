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
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.strictmode.FragmentStrictMode;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.DriverBinding;
import com.vdurmont.emoji.EmojiParser;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.Instant;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
    private GlideImageLoader glideImageLoader;
    private final FragmentManager fragmentManager;

    public Driver(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        SI = (SI) getActivity();
        glideImageLoader = new GlideImageLoader(context);
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
                context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
                ImagePicker imagePicker = (ImagePicker) fragmentManager.findFragmentByTag("imagePicker");
                if (imagePicker == null) {
                    imagePicker = new ImagePicker(fragmentManager, null, RequestCode.PROFILE);
                    imagePicker.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                    imagePicker.show(fragmentManager, "imagePicker");
                }
            });
        }
        setDriverInfo();
        setRankAndStamp();
        updateProfilePicture();
        refreshRank();
        binding.blocked.setOnLongClickListener(v -> {
            if (SI != null) {
                context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
                Utils.vibrate(v);
                SI.checkBlocked();
            }
            return true;
        });
        binding.stats.setOnClickListener(v -> {
            context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
            Utils.vibrate(v);
            FlaggingDialog flaggingDialog = (FlaggingDialog) fragmentManager.findFragmentByTag("FlaggingDialog");
            if (flaggingDialog == null) {
                flaggingDialog = new FlaggingDialog();
                flaggingDialog.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                flaggingDialog.show(fragmentManager, "FlaggingDialog");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGhostStatus();
    }

    public void updateGhostStatus() {
        if (RadioService.operator.getSalutes() < 319 || RadioService.operator.getCount() < 1000) {
            binding.ghost.setVisibility(View.INVISIBLE);
            binding.status.setText("");
        } else {
            if (RadioService.ghostUsers.contains(RadioService.operator.getUser_id())) {
                binding.ghost.setVisibility(View.INVISIBLE);
                if (RadioService.ghostUsers.contains(RadioService.operator.getUser_id()))
                    binding.status.setText("Ghost Mode active");
            } else {
                binding.status.setText("");
                binding.ghost.setVisibility(View.VISIBLE);
            }
        }
    }

    public void updateProfilePicture() {
        glideImageLoader.load(binding.driverProfilePictureIv, RadioService.operator.getProfileLink(), RadioService.profileOptions);
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
            glideImageLoader.load(binding.driverStarIv, Utils.parseRankUrl(RadioService.operator.getRank()));
    }

    public void refreshRank() {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", RadioService.operator.getUser_id());
        new OkUtil().call("user_rank.php", claims, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (response) {
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
                                        context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
                                        Stars starSelectionDialog = (Stars) fragmentManager.findFragmentByTag("ssd");
                                        if (starSelectionDialog == null) {
                                            starSelectionDialog = new Stars();
                                            starSelectionDialog.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                                            starSelectionDialog.show(fragmentManager, "ssd");
                                        }
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
