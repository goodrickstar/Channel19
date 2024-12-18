package com.cb3g.channel19;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.bumptech.glide.Glide;
import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.LoginBinding;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.jaredrummler.android.device.DeviceName;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity implements LI, PurchasesUpdatedListener, View.OnClickListener {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private OkHttpClient okClient;
    private SharedPreferences settings;
    private final String SITE_URL = "http://23.111.159.2/~channel1/";
    private String TOKEN;
    private LoginBinding binding;
    private float volumeE;
    private SoundPool sp;
    private int clickTwo;
    private RotateAnimation rotate;
    private BroadcastReceiver receiver;
    private final Object serial = "unknown";
    private BillingUtils billingUtils;

    private FirebaseUser user;

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(new FirebaseAuthUIActivityResultContract(), (result) -> {
        if (result.getResultCode() == RESULT_OK) {
            showSnack(new Snack("Login successful", Snackbar.LENGTH_SHORT));
        } else {
            showSnack(new Snack("Google sign in failed", Snackbar.LENGTH_LONG));
            Log.e("Firebase Auth Error", "Login error code" + result.getResultCode());
        }
        handleAuth(auth.getCurrentUser());
    });

    private void startSignIn() {
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setTheme(R.style.mytheme)
                .setAvailableProviders(List.of(new AuthUI.IdpConfig.GoogleBuilder().build()))
                .build();

        signInLauncher.launch(signInIntent);
    }

    @Override
    public void onClick(View v) {
        Utils.vibrate(v);
        int id = v.getId();
        if (id == R.id.googleButton || id == R.id.profileName) {
            if (user == null) {
                startSignIn();
            }
        } else if (id == R.id.logout) {
            if (user != null) {
                logOutWithGoogle();
            }
        } else if (id == R.id.loginIntoServerWithGoogleButton) {
            login();
        }
    }

    private void logOutWithGoogle() {
        auth.signOut();
        showSnack(new Snack("Logged Out", Snackbar.LENGTH_SHORT));
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        binding = LoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        billingUtils = new BillingUtils(this, this);
        okClient = new OkHttpClient();
        settings = getSharedPreferences("settings", MODE_PRIVATE);
        binding.tvStatus.setText(R.string.TM);
        volumeE = scaleVolume(settings.getInt("eVolume", 50));
        sp = new SoundPool.Builder().build();
        clickTwo = sp.load(this, R.raw.clicktwo, 1);
        binding.backdrop.setScaleType(ImageView.ScaleType.FIT_XY);
        binding.backdrop.setImageResource(R.drawable.login);
        rotate = new RotateAnimation(0, 358, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(650);
        rotate.setRepeatCount(-1);
        rotate.setInterpolator(new LinearInterpolator());
        binding.profileName.setOnClickListener(this);
        binding.googleButton.setOnClickListener(this);
        binding.loginIntoServerWithGoogleButton.setOnClickListener(this);
        binding.logout.setOnClickListener(this);
        TOKEN = settings.getString("token", null);
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        TOKEN = task.getResult();
                        settings.edit().putString("token", TOKEN).apply();
                    } else {
                        showSnack(new Snack("There was an issue registering this device with Firebase"));
                    }
                });
        binding.loginIntoServerWithGoogleButton.setOnLongClickListener(v -> {
            Utils.vibrate(v);
            UtilsKKt.gotoPlayStore(LoginActivity.this);
            return true;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case "nineteenProve" -> login();
                        case "nineteenSendProfileToServer" -> {
                            click_sound();
                            rotate_logo();
                            pre_text("Creating Profile...");
                            Utils.getDatabase().getReference().child("keychain").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    final Map<String, Object> claims = new HashMap<>();
                                    claims.put("userId", user.getUid());
                                    claims.put("radio_handle", intent.getStringExtra("handle"));
                                    claims.put("carrier", intent.getStringExtra("carrier"));
                                    claims.put("title", intent.getStringExtra("title"));
                                    claims.put("hometown", intent.getStringExtra("town"));
                                    new OkUtil().call("user_create_profile.php", claims, new Callback() {
                                        @Override
                                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                            show_result("Network Error", e.getMessage());
                                        }

                                        @Override
                                        public void onResponse(@NotNull Call call, @NotNull Response response) {
                                            login();
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                    }
                }
            }
        };
        final IntentFilter filter = new IntentFilter();
        filter.addAction("nineteenSendProfileToServer");
        filter.addAction("nineteenProve");
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.loginIntoServerWithGoogleButton.setEnabled(true);
        binding.version.setText("v(" + getVersionName() + ")");
        if (Utils.serviceAlive(this) && !settings.getBoolean("exiting", false))
            launch_main_activity();
        handleAuth(auth.getCurrentUser());
        if (!settings.getString("userId", "0").equals("0") && !settings.getBoolean("info1", false)){
            settings.edit().putBoolean("info1", true).apply();
            show_result("Helpful Tips", "Long-press the login button at any time to be directed to the App in the Google Play Store");
        }
    }

    public void handleAuth(FirebaseUser user) {
        this.user = user;
        if (user == null) {
            Glide.with(this).load(R.drawable.googlelogo).circleCrop().into(binding.googleButton);
            binding.profileName.setText(getString(R.string.click_to_login));
            binding.emailAddress.setText("");
            settings.edit().putString("userId", "0").apply();
        } else {
            Glide.with(this).load(Objects.requireNonNull(user.getPhotoUrl()).toString().replace("96", "400")).circleCrop().into(binding.googleButton);
            binding.profileName.setText(user.getDisplayName());
            binding.emailAddress.setText(user.getEmail());
            settings.edit().putString("userId", user.getUid()).apply();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);
    }

    private void launch_main_activity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void login() {
        if (user == null) {
            startSignIn();
            return;
        }
        if (TOKEN == null) {
            return;
        }
        if (!settings.getBoolean("accepted", false)) {
            showTerms();
            return;
        }
        binding.loginIntoServerWithGoogleButton.setEnabled(false);
        pre_text("Logging in..");
        rotate_logo();
        if (billingUtils.isConnected) {
            billingUtils.queryActiveSubscriptions((billingResult, subscriptions) -> DeviceName.with(LoginActivity.this).request((info, error) -> Utils.getDatabase().getReference().child("keychain").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String KEY = snapshot.getValue(String.class);
                    assert KEY != null;
                    settings.edit().putString("keychain", KEY).apply();
                    final Map<String, Object> claims = new HashMap<>();
                    claims.put("userId", user.getUid());
                    claims.put("email", user.getEmail());
                    claims.put("name", user.getDisplayName());
                    claims.put("reg_id", TOKEN);
                    claims.put("deviceId", deviceId());
                    claims.put("gsf", returnGSF());
                    claims.put("imei", "");
                    claims.put("serial", serial);
                    claims.put("language", Locale.getDefault().getDisplayLanguage());
                    claims.put("deviceName", info.marketName);
                    claims.put("active", !subscriptions.isEmpty());
                    claims.put("version", String.valueOf(getVersion()));
                    claims.put("version_name", getVersionName());
                    claims.put("build", getBuildVersion());
                    new OkUtil().call(okClient, SITE_URL + "google_login.php", claims, KEY, new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            show_result("Network Error", e.getMessage());
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            assert response.body() != null;
                            String image = response.body().string();
                            runOnUiThread(() -> {
                                try (response) {
                                    JSONObject data = new JSONObject(image);
                                    if (data.getString("user_id").equals("0")) {
                                        final String mode = data.getString("mode");
                                        final String message = data.getString("mode");
                                        show_result(mode, message);
                                    } else {
                                        final String handle = data.getString("radio_hanlde");
                                        if (handle.equals("default")) {
                                            select_title();
                                            return;
                                        }
                                        final String profile = data.getString("profileLink");
                                        final boolean invisible = Boolean.parseBoolean(data.getString("invisible"));
                                        final SharedPreferences.Editor edit = settings.edit();
                                        edit.putString("email", data.getString("email"));
                                        edit.putString("userId", data.getString("user_id"));
                                        edit.putString("handle", handle);
                                        edit.putString("rank", data.getString("rank"));
                                        edit.putString("carrier", data.getString("carrier"));
                                        edit.putString("town", data.getString("hometown"));
                                        edit.putBoolean("admin", Boolean.parseBoolean(data.getString("admin")));
                                        edit.putBoolean("invisible", invisible);
                                        edit.putString("profileLink", profile);
                                        edit.putInt("newbie", data.getInt("newbie"));
                                        edit.putBoolean("active", data.getBoolean("subscribed"));
                                        edit.putString("photoIDs", data.getString("photoIDs"));
                                        edit.putString("textIDs", data.getString("textIDs"));
                                        edit.putString("blockedIDs", data.getString("blockedIDs"));
                                        edit.putString("salutedIDs", data.getString("salutedIDs"));
                                        edit.putString("flaggedIDs", data.getString("flaggedIDs"));
                                        edit.putString("main_backdrop", data.getString("one"));
                                        edit.putString("settings_backdrop", data.getString("two"));
                                        edit.putInt("count", data.getInt("total_count"));
                                        edit.putInt("salutes", data.getInt("salutes"));
                                        edit.putBoolean("exiting", false);
                                        edit.apply();
                                        finish();
                                        startForegroundService(new Intent(LoginActivity.this, RadioService.class));
                                        launch_main_activity();
                                    }
                                } catch (JSONException e) {
                                    show_result("Login Error", e.getMessage());
                                    Logger.INSTANCE.e("google_login", e.getMessage());
                                    binding.loginIntoServerWithGoogleButton.setEnabled(true);
                                }
                            });
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            })));
        } else {
            runOnUiThread(() -> {
                post_text();
                showSnack(new Snack("There was an issue connected with Google Play"));
                binding.loginIntoServerWithGoogleButton.setEnabled(true);
            });

        }
    }

    private float scaleVolume(int sliderValue) {
        return (float) sliderValue / 100;
    }

    private String deviceId() {
        @SuppressLint("HardwareIds") final String devId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (devId != null) return devId;
        else return "";
    }

    public void showTerms() {
        FragmentManager manager = getSupportFragmentManager();
        TermsOfUse tdd = (TermsOfUse) manager.findFragmentByTag("tdd");
        if (tdd == null) {
            tdd = new TermsOfUse(true);
            tdd.setCancelable(false);
            tdd.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            tdd.show(manager, "tdd");
        }
    }

    @Override
    public void rotate_logo() {
        runOnUiThread(() -> binding.spinner.startAnimation(rotate));
    }

    @Override
    public void show_result(final String title, final String content) {
        if (isFinishing()) return;
        runOnUiThread(() -> {
            post_text();
            final Bundle helpbundle = new Bundle();
            helpbundle.putString("title", title);
            helpbundle.putString("content", content);
            final Blank bdf = new Blank();
            bdf.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.mydialog);
            bdf.setArguments(helpbundle);
            bdf.show(getSupportFragmentManager(), "bdf");
        });
    }

    @Override
    public void post_text() {
        runOnUiThread(() -> {
            binding.tvStatus.setText(R.string.TM);
            binding.spinner.clearAnimation();
        });
    }

    @Override
    public void pre_text(final String text) {
        runOnUiThread(() -> binding.tvStatus.setText(text));
    }

    @Override
    public void click_sound() {
        sp.play(clickTwo, volumeE, volumeE, 1, 0, 1f);
    }

    @Override
    public void select_title() {
        post_text();
        if (isFinishing()) return;
        final FillProfile sdf = new FillProfile();
        sdf.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
        sdf.show(getSupportFragmentManager(), "sdf");
    }

    private String getBuildVersion() {
        return String.valueOf(Build.VERSION.SDK_INT);
    }

    private int getVersion() {
        int version = 0;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo("com.cb3g.channel19", PackageManager.GET_META_DATA);
            version = (int) packageInfo.getLongVersionCode();
        } catch (PackageManager.NameNotFoundException e) {
            if (e.getMessage() != null)
                Log.e("getVersion()", e.getMessage());
        }
        return version;
    }

    private String getVersionName() {
        String version = "1.0";
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo("com.cb3g.channel19", PackageManager.GET_META_DATA);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            if (e.getMessage() != null)
                Log.e("getVersion()", e.getMessage());
        }
        return version;
    }

    private String returnGSF() {
        Uri URI = Uri.parse("content://com.google.android.gsf.gservices");
        String ID_KEY = "android_id";
        String[] params = {ID_KEY};
        Cursor c = this.getContentResolver().query(URI, null, null, params, null);
        if (c != null && (!c.moveToFirst() || c.getColumnCount() < 2)) {
            if (!c.isClosed())
                c.close();
            return "unknown";
        }
        try {
            if (c != null) {
                String result = Long.toHexString(Long.parseLong(c.getString(1)));
                if (!c.isClosed())
                    c.close();
                return result;
            }
        } catch (NumberFormatException e) {
            if (!c.isClosed())
                c.close();
        }
        return "unknown";
    }

    public void showSnack(Snack snack) {
        Snackbar snackbar = Snackbar.make(binding.coordinator, snack.getMessage(), snack.getLength());
        View view = snackbar.getView();
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.main_black));
        snackbar.setActionTextColor(Color.WHITE);
        snackbar.setAction("10 4", v -> {
            Utils.vibrate(v);
            snackbar.dismiss();
        });
        snackbar.show();
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {

    }
}