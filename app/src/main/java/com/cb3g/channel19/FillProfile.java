package com.cb3g.channel19;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.FillProfileBinding;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class FillProfile extends DialogFragment {
    private Context context;
    private FillProfileBinding binding;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        binding = FillProfileBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String handle = null;
        String carrier = null;
        String location = null;
        String profileLink = null;
        final ImageView profile = view.findViewById(R.id.profile_photo);
        if (getArguments() != null){
            final Bundle bundle = requireArguments();
            profileLink = bundle.getString("profileLink");
            handle = bundle.getString("handle");
            carrier = bundle.getString("carrier");
            location = bundle.getString("location");
        }
        if (profileLink != null)
            Glide.with(context).load(profileLink).apply(RadioService.profileOptions).into(profile);
        if (handle != null) binding.handleET.setText(handle);
        if (carrier != null) binding.carrierET.setText(carrier);
        if (location != null) binding.townET.setText(location);
        final View.OnClickListener listener = v -> {
            context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
            Utils.vibrate(v);
            final String newHandle = binding.handleET.getText().toString().trim();
            final String newCarrier = binding.carrierET.getText().toString().trim();
            final String newTown = binding.townET.getText().toString().trim();
            int id = v.getId();
            if (id == R.id.save) {
                String minimum = "Min of 4 char";
                String characters = "Use chars A-Z";
                if (preTest(newHandle) || preTest(newCarrier) || preTest(newTown)) {
                    Toaster.toastlow(context, "Reserved");
                    return;
                }
                if (newHandle.trim().isEmpty() || newHandle.trim().length() < 3) {
                    Error(binding.handleET, minimum);
                    return;
                }
                if (newCarrier.trim().isEmpty() || newCarrier.trim().length() < 3) {
                    Error(binding.carrierET, minimum);
                    return;
                }
                if (!newHandle.replaceAll("[^\\p{Alpha}\\s]", "").equals(newHandle)) {
                    Error(binding.handleET, characters);
                    return;
                }
                if (RadioService.operator.getHandle().equals(newHandle) && RadioService.operator.getCarrier().equals(newCarrier) && RadioService.operator.getTown().equals(newTown)) {
                    Toaster.toastlow(context, "No Change");
                    return;
                }
                if (RadioService.operator.getHandle().equals(newHandle)) {
                    updateProfile(newHandle, newCarrier, newTown);
                } else {
                    SharedPreferences settings = context.getSharedPreferences("settings", MODE_PRIVATE);
                    Map<String, Object> header = new HashMap<>();
                    header.put("typ", Header.JWT_TYPE);
                    final String compactJws = Jwts.builder()
                            .setHeader(header)
                            .claim("handle", newHandle)
                            .claim("handle", newHandle)
                            .setIssuedAt(new Date(System.currentTimeMillis()))
                            .setExpiration(new Date(System.currentTimeMillis() + 60000))
                            .signWith(SignatureAlgorithm.HS256, settings.getString("keychain", null))
                            .compact();
                    final Request request = new Request.Builder()
                            .url(RadioService.SITE_URL + "user_handle_match.php")
                            .post(new FormBody.Builder().add("data", compactJws).build())
                            .build();
                    new OkHttpClient().newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            if (response.isSuccessful()) {
                                assert response.body() != null;
                                String data = response.body().string();
                                requireActivity().runOnUiThread(() -> {
                                    try {
                                        JSONObject object = new JSONObject(data);
                                        if (object.getBoolean("match"))
                                            binding.handleET.setError("Already in use");
                                        else
                                            updateProfile(newHandle, newCarrier, newTown);
                                    } catch (JSONException e) {
                                        Logger.INSTANCE.e("Fill Profile JSON error", e.getMessage());
                                    }
                                });
                            }
                        }
                    });
                }
            } else if (id == R.id.cancel) {
                dismiss();
            }
        };
        binding.save.setOnClickListener(listener);
        binding.cancel.setOnClickListener(listener);
        Utils.showKeyboard(context, binding.handleET);
    }

    private void updateProfile(String newHandle, String newCarrier, String newTown) {
        context.sendBroadcast(new Intent("nineteenSendProfileToServer").setPackage("com.cb3g.channel19").putExtra("handle", newHandle).putExtra("carrier", newCarrier).putExtra("town", newTown).setPackage("com.cb3g.channel19"));
        dismiss();
    }

    private void Error(EditText editText, String message) {
        editText.setError(message);
    }

    private boolean preTest(String text) {
        text = text.toLowerCase();
        return text.contains("table") || text.contains("rocky") || text.contains("goodrick") || text.contains("drop") || text.contains("nine") || text.contains("teen") || text.contains("channel") || text.contains("twist") || text.contains("19") || text.contains("⭐️") || text.contains("admin") || text.contains("\uD83C\uDF1F");
    }

}
