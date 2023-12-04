package com.cb3g.channel19;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
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
import android.widget.TextView;

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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.jaredrummler.android.device.DeviceName;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

public class LoginActivity extends AppCompatActivity implements LI, PurchasesUpdatedListener, ValueEventListener, View.OnClickListener, FirebaseAuth.AuthStateListener {
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseUser googleUser;
    private FirebaseAuth mAuth;
    private OkHttpClient okClient;
    private SharedPreferences settings;
    private String SITE_URL, TOKEN, KEY;
    private LoginBinding binding;
    private float volumeE;
    private SoundPool sp;
    private int clicktwo;
    private RotateAnimation rotate;
    private BroadcastReceiver receiver;
    private Map<String, Object> header;
    private final int RC_SIGN_IN = 777;
    private final Object serial = "unknown";
    private BillingUtils billingUtils;

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        googleUser = firebaseAuth.getCurrentUser();
        if (googleUser == null) {
            Glide.with(this).load(R.drawable.googlelogo).circleCrop().into(binding.googleButton);
            binding.profileName.setText(getString(R.string.click_to_login));
            binding.emailAddress.setText("");
            settings.edit().putString("userId", "0").apply();
        } else {
            Glide.with(this).load(googleUser.getPhotoUrl().toString().replace("96", "400")).circleCrop().into(binding.googleButton);
            binding.profileName.setText(googleUser.getDisplayName());
            binding.emailAddress.setText(googleUser.getEmail());
            settings.edit().putString("userId", googleUser.getUid()).apply();
        }
    }

    @Override
    public void onClick(View v) {
        Utils.vibrate(v);
        int id = v.getId();
        if (id == R.id.googleButton || id == R.id.profileName) {
            if (googleUser == null) {
                logInWithGoogle();
            }
        } else if (id == R.id.logout) {
            if (googleUser != null) {
                logOutWithGoogle();
            }
        } else if (id == R.id.loginIntoServerWithGoogleButton) {
            if (googleUser != null) {
                login(googleUser);
            } else logInWithGoogle();
        }
    }

    private void logInWithGoogle() {
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    private void logOutWithGoogle() {
        if (mAuth != null) mAuth.signOut();
        if (mGoogleSignInClient != null) mGoogleSignInClient.signOut();
        showSnack(new Snack("Logged Out", Snackbar.LENGTH_SHORT));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Logger.INSTANCE.i("firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Logger.INSTANCE.e("Google sign in failed", e.getLocalizedMessage());
                showSnack(new Snack("Google sign in failed", Snackbar.LENGTH_LONG));
            }
        }

    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        showSnack(new Snack("Login succesful", Snackbar.LENGTH_SHORT));
                    } else {
                        showSnack(new Snack("Firebase Authentification failed", Snackbar.LENGTH_LONG));
                        Logger.INSTANCE.e("Firebase Authentification failed", task.getException().getMessage());
                    }
                });
    }

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
        header = new HashMap<>();
        header.put("typ", Header.JWT_TYPE);
        sp = new SoundPool.Builder().build();
        clicktwo = sp.load(this, R.raw.clicktwo, 1);
        binding.backdrop.setScaleType(ImageView.ScaleType.FIT_XY);
        binding.backdrop.setImageResource(R.drawable.login);
        rotate = new RotateAnimation(0, 358, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(650);
        rotate.setRepeatCount(-1);
        rotate.setInterpolator(new LinearInterpolator());
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();
        googleUser = mAuth.getCurrentUser();
        binding.profileName.setOnClickListener(this);
        binding.googleButton.setOnClickListener(this);
        binding.loginIntoServerWithGoogleButton.setOnClickListener(this);
        binding.logout.setOnClickListener(this);
        SITE_URL = settings.getString("siteUrl", "http://truckradiosystem.com/~channel1/");
        KEY = settings.getString("keychain", null);
        TOKEN = settings.getString("token", null);
        Utils.getDatabase().getReference().child("keychain").addValueEventListener(this);
        Utils.getDatabase().getReference().child("siteUrl").addValueEventListener(this);
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        TOKEN = task.getResult();
                        settings.edit().putString("token", TOKEN).apply();
                    } else {
                        showSnack(new Snack("There was an issue registering this device with Firebase"));
                    }
                });
        Logger.INSTANCE.i("Activity Created");
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onStart() {
        super.onStart();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null && KEY != null && SITE_URL != null) {
                    switch (action) {
                        case "nineteenProve" -> {
                            Log.i("logging", "google user is " + googleUser);
                            login(googleUser);
                        }
                        case "nineteenSendProfileToServer" -> {
                            click_sound();
                            rotate_logo();
                            pre_text("Creating Profile...");
                            final String compactJws = Jwts.builder()
                                    .setHeader(header)
                                    .claim("userId", googleUser.getUid())
                                    .claim("radio_handle", intent.getStringExtra("handle"))
                                    .claim("carrier", intent.getStringExtra("carrier"))
                                    .claim("title", intent.getStringExtra("title"))
                                    .claim("hometown", intent.getStringExtra("town"))
                                    .setIssuedAt(new Date(System.currentTimeMillis()))
                                    .setExpiration(new Date(System.currentTimeMillis() + 60000))
                                    .signWith(SignatureAlgorithm.HS256, KEY)
                                    .compact();
                            final Request request = new Request.Builder()
                                    .url(SITE_URL + "user_create_profile.php")
                                    .post(new FormBody.Builder().add("data", compactJws).build())
                                    .build();
                            okClient.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                    show_result("Network Error", e.getMessage());
                                }

                                @Override
                                public void onResponse(@NotNull Call call, @NotNull Response response) {
                                    login(googleUser);
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
        registerReceiver(receiver, filter);
        mAuth.addAuthStateListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.version.setText("v(" + getVersionName() + ")");
        if (serviceAlive() && !settings.getBoolean("exiting", false)) launch_main_activity();
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

    private void login(FirebaseUser user) {
        if (!settings.getBoolean("accepted", false)) {
            showTerms();
            return;
        }
        if (KEY == null || SITE_URL == null || TOKEN == null) return;
        pre_text("Logging in..");
        rotate_logo();
        if (billingUtils.isConnected) {
            billingUtils.queryActiveSubscriptions((billingResult, subscriptions) -> DeviceName.with(LoginActivity.this).request((info, error) -> {
                String compactJws = Jwts.builder()
                        .setHeader(header)
                        .claim("userId", user.getUid())
                        .claim("email", user.getEmail())
                        .claim("name", user.getDisplayName())
                        .claim("reg_id", TOKEN)
                        .claim("deviceId", deviceId())
                        .claim("gsf", returnGSF())
                        .claim("imei", "")
                        .claim("serial", serial)
                        .claim("language", Locale.getDefault().getDisplayLanguage())
                        .claim("deviceName", info.marketName)
                        .claim("active", !subscriptions.isEmpty())
                        .claim("version", String.valueOf(getVersion()))
                        .claim("version_name", getVersionName())
                        .claim("build", getBuildVersion())
                        .setIssuedAt(new Date(System.currentTimeMillis()))
                        .setExpiration(new Date(System.currentTimeMillis() + 60000))
                        .signWith(SignatureAlgorithm.HS256, KEY)
                        .compact();
                Request request = new Request.Builder()
                        .url(SITE_URL + "google_login.php")
                        .post(new FormBody.Builder().add("data", compactJws).build())
                        .build();
                okClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        show_result("Network Error", e.getMessage());
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        String image = response.body().string();
                        runOnUiThread(() -> {
                            try {
                                JSONObject data = new JSONObject(image);
                                if (data.getString("user_id").equals("0")) {
                                    show_result(data.getString("mode"), data.getString("msg"));
                                } else {
                                    final String handle = data.getString("radio_hanlde");
                                    if (handle.equals("default")) {
                                        select_title();
                                        return;
                                    }
                                    final String profile = data.getString("profileLink");
                                    final boolean invisible = Boolean.parseBoolean(data.getString("invisible"));
                                    welcome(handle, profile);
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
                            } finally {
                                response.close();
                            }
                        });
                    }
                });

            }));
        } else {
            runOnUiThread(() -> {
                post_text();
                showSnack(new Snack("There was an issue connected with Google Play"));
            });

        }
    }

    private boolean serviceAlive() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RadioService.class.getName().equals(service.service.getClassName())) return true;
        }
        return false;
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
    public void toast(final String text) {
        Toaster.toastlow(this, text);
    }

    @Override
    public void welcome(final String text, final String profileLink) {
        runOnUiThread(() -> Toaster.online(LoginActivity.this, "Welcome " + text, profileLink));
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
        sp.play(clicktwo, volumeE, volumeE, 1, 0, 1f);
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
            e.printStackTrace();
        }
        return version;
    }

    private String getVersionName() {
        String version = "1.0";
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo("com.cb3g.channel19", PackageManager.GET_META_DATA);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
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
                if (result != null) return result;
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
        TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(ContextCompat.getColor(this, R.color.main_white));
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.main_black));
        if (snack.getLength() == Snackbar.LENGTH_INDEFINITE) {
            snackbar.setActionTextColor(Color.WHITE);
            snackbar.setAction("10 4", v -> {
                Utils.vibrate(v);
                snackbar.dismiss();
            });
        } else {
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        snackbar.show();
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        switch (dataSnapshot.getKey()) {
            case "siteUrl":
                SITE_URL = dataSnapshot.getValue(String.class);
                settings.edit().putString("siteUrl", SITE_URL).apply();
                break;
            case "keychain":
                KEY = dataSnapshot.getValue(String.class);
                settings.edit().putString("keychain", KEY).apply();
                break;
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }


    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {

    }
}