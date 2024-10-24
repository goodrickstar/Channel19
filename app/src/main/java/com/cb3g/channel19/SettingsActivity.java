package com.cb3g.channel19;

import static com.cb3g.channel19.RadioService.databaseReference;
import static com.cb3g.channel19.RadioService.operator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.SettingsBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.Instant;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

@SuppressWarnings("ALL")
public class SettingsActivity extends FragmentActivity implements SI, PurchasesUpdatedListener {
    private final Controls confrag = new Controls();
    private final Account accfrag = new Account();
    private final Driver drifrag = new Driver(getSupportFragmentManager());
    private final Bundle userbundle = new Bundle();
    public final String OLD_SUBSCRIPTION = "activate";
    public final String NEW_SUBSCRIPTION = "fivedollars";
    public final String GHOST = "ghost";
    public final String DONATE = "donation";
    private int stage = 1, post = 2;
    private FragmentManager fragmentManager;
    private SettingsBinding binding;

    private GlideImageLoader glideImageLoader = new GlideImageLoader(this);
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) switch (action) {
                case "nineteenGifChosen":
                    ImagePicker imagePicker = (ImagePicker) fragmentManager.findFragmentByTag("imagePicker");
                    if (imagePicker != null)
                        imagePicker.setPhoto(RadioService.gson.fromJson(intent.getStringExtra("data"), Gif.class), false);
                    break;
                case "nineteenToast":
                    Toaster.toastlow(SettingsActivity.this, intent.getStringExtra("data"));
                    break;
                case "setStar":
                    final Map<String, Object> claims = new HashMap<>();
                    claims.put("userId", operator.getUser_id());
                    claims.put("rank", intent.getStringExtra("data"));
                    new OkUtil().call("user_set_star.php", claims, new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {

                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            if (drifrag.isAdded() && response.isSuccessful()) drifrag.refreshRank();
                        }
                    });
                    break;
                case "nineteenUpdateProfile":
                    ImageSearch imageSearch = new ImageSearch("");
                    imageSearch.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                    imageSearch.show(fragmentManager, "imageSearch");
                    break;
                case "browseBackgrounds":
                    if (!Utils.permissionsAccepted(SettingsActivity.this, Utils.getStoragePermissions())) {
                        Utils.requestPermission(SettingsActivity.this, Utils.getStoragePermissions(), 3);
                        return;
                    }
                    try {
                        startActivityForResult(new Intent().setType("image/*").setAction(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE), 9999);
                    } catch (Exception e) {
                        LOG.e(e.getMessage());
                    }
                    break;
                case "updateProfilePicture":
                    if (drifrag.isAdded())
                        drifrag.updateProfilePicture();
                    break;
                case "killActivity":
                    finish();
                    break;
                case "nineteenSendProfileToServer":
                    sendProfileToServer(intent.getStringExtra("handle"), intent.getStringExtra("carrier"), intent.getStringExtra("town"));
                    break;
                case "requestGPS":
                    ActivityCompat.requestPermissions(SettingsActivity.this, Utils.getLocationPermissions(), 1);
                    break;
                case "nineteenShowBlank":
                    showResult(intent.getStringExtra("title"), intent.getStringExtra("content"));
                    break;
            }
        }
    };
    private BillingUtils billingUtils;
    private SharedPreferences settings;

    @Override
    public void checkBlocked() {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", operator.getUser_id());
        new OkUtil().call("user_blocked_by.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try (response) {
                        BlockedByFragment blockedByFragment = (BlockedByFragment) fragmentManager.findFragmentByTag("bbf");
                        if (blockedByFragment == null) {
                            blockedByFragment = new BlockedByFragment();
                            Bundle bundle = new Bundle();
                            bundle.putStringArrayList("handles", new Gson().fromJson(response.body().string(), new TypeToken<List<String>>() {
                            }.getType()));
                            blockedByFragment.setArguments(bundle);
                            blockedByFragment.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                            blockedByFragment.show(fragmentManager, "bbf");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            for (Purchase purchase : purchases) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    switch (purchase.getProducts().get(0)) {
                        case GHOST:
                        case DONATE:
                            billingUtils.handlePurchase(purchase, new ConsumeResponseListener() {
                                @Override
                                public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String s) {
                                    switch (purchase.getProducts().get(0)) {
                                        case GHOST:
                                            databaseReference.child("ghost").child(operator.getUser_id()).setValue(Instant.now().getEpochSecond()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(@NotNull Void aVoid) {
                                                    if (drifrag != null) if (drifrag.isAdded())
                                                        drifrag.updateGhostStatus();
                                                    showResult("Ghost Mode actived", "You will remain invisible and unblockable for 24 hours");
                                                    sendBroadcast(new Intent("ghost").setPackage("com.cb3g.channel19"));
                                                }
                                            });
                                            break;
                                        case DONATE:
                                            showResult("Donation Successfull!", "Thank you kindly for your loyalty and support");
                                            userDonated();
                                            break;
                                    }
                                }
                            });
                            break;
                        case NEW_SUBSCRIPTION:
                        case OLD_SUBSCRIPTION:
                            billingUtils.acknowledgePurchase(purchase, new AcknowledgePurchaseResponseListener() {
                                @Override
                                public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                                    operator.setSubscribed(true);
                                    settings.edit().putBoolean("active", true).apply();
                                    showResult("Peaked And Tuned", "Subscription successful! You will no longer recieve advertising");
                                    final Account af = (Account) fragmentManager.findFragmentByTag("afrag");
                                    if (af != null) af.setStatus(true);
                                    userSubscribed();
                                }
                            });
                            break;
                    }
                }
            }
        }
    }

    private void updateHandleInReservoir(String handle) {
        RadioService.databaseReference.child("reservoir").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //loop each channel
                for (DataSnapshot channel : dataSnapshot.getChildren()) {
                    RadioService.databaseReference.child("reservoir").child(channel.getKey()).child("posts").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            //loop each post
                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                Post post = child.getValue(Post.class);
                                if (post.getFacebookId().equals(operator.getUser_id()))
                                    RadioService.databaseReference.child("reservoir").child(channel.getKey()).child("posts").child(post.getPostId()).child("handle").setValue(handle);
                                if (post.getLatest_facebookId().equals(operator.getUser_id()))
                                    RadioService.databaseReference.child("reservoir").child(channel.getKey()).child("posts").child(post.getPostId()).child("latest_handle").setValue(handle);
                                //loop each remark for this post
                                RadioService.databaseReference.child("reservoir").child(channel.getKey()).child("remarks").child(post.getPostId()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot commentChild : dataSnapshot.getChildren()) {
                                            Comment comment = commentChild.getValue(Comment.class);
                                            if (comment.getUserId().equals(operator.getUser_id()))
                                                RadioService.databaseReference.child("reservoir").child(channel.getKey()).child("remarks").child(post.getPostId()).child(comment.getRemarkId()).child("handle").setValue(handle);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public void buy(View v) {
        sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
        Utils.vibrate(v);
        if (!settings.getBoolean("active", false)) {
            if (billingUtils.isConnected()) {
                billingUtils.querySubscriptionDetails(NEW_SUBSCRIPTION, new ProductDetailsResponseListener() {
                    @Override
                    public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> list) {
                        Log.i("subscribe", String.valueOf(list));
                        if (!list.isEmpty())
                            billingUtils.purchaseSubscription(SettingsActivity.this, list.get(0));
                    }
                });
            }
        } else {
            if (billingUtils.isConnected()) {
                billingUtils.queryProductDetails(DONATE, new ProductDetailsResponseListener() {
                    @Override
                    public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> list) {
                        if (!list.isEmpty())
                            billingUtils.purchaseProduct(SettingsActivity.this, list.get(0));
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        sendBroadcast(new Intent("nineteenTabSound").setPackage("com.cb3g.channel19"));
        super.onBackPressed();
    }

    private void sendProfileToServer(final String radio_handle, final String carrier, final String hometown) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", operator.getUser_id());
        claims.put("radio_handle", radio_handle);
        claims.put("carrier", carrier);
        claims.put("stamp", hometown);
        new OkUtil().call("user_create_profile.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String data = response.body().string();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (new JSONObject(data).getString("success").equals("1")) {
                                    operator.setHandle(radio_handle);
                                    operator.setCarrier(carrier);
                                    operator.setTown(hometown);
                                    refreshLocalProfile();
                                    showResult("Profile Change", "Your profile was updated successfully!");
                                    updateHandleInReservoir(radio_handle);
                                }
                            } catch (JSONException e) {
                                LOG.e("sendProfileToServer", e.getMessage());
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("settings", MODE_PRIVATE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        binding = SettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        billingUtils = new BillingUtils(this, this);
        fragmentManager = getSupportFragmentManager();
        String background = settings.getString("settings_backdrop", "");
        if (settings.getBoolean("custom", false))
            background = settings.getString("background", "default");
        glideImageLoader.load(binding.backdrop, background);
        binding.menu.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Utils.vibrate(view);
                sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
                AppOptionsDialogFragment fragment = new AppOptionsDialogFragment();
                fragment.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.DialogTheme);
                fragment.show(fragmentManager, "appOptions");
                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction("setStar");
        filter.addAction("browseBackgrounds");
        filter.addAction("updateProfilePicture");
        filter.addAction("nineteenUpdateProfile");
        filter.addAction("nineteenSendProfileToServer");
        filter.addAction("requestGPS");
        filter.addAction("nineteenShowBlank");
        filter.addAction("nineteenGifChosen");
        filter.addAction("killActivity");
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        if (settings.getBoolean("exiting", false)) {
            startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
            finish();
            sendBroadcast(new Intent("exitChannelNineTeen").setPackage("com.cb3g.channel19"));
            return;
        }
        chstage();
        binding.tubular.setText(R.string.menu_control_panel);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);
    }

    private void userDonated() {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", operator.getUser_id());
        new OkUtil().call("user_donated.php", claims);
    }

    private void userSubscribed() {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", operator.getUser_id());
        new OkUtil().call("user_subscribed.php", claims);
    }

    private void showResult(String title, String content) {
        if (isFinishing()) return;
        Blank bdf = (Blank) fragmentManager.findFragmentByTag("bdf");
        if (bdf == null) {
            Bundle helpbundle = new Bundle();
            helpbundle.putString("title", title);
            helpbundle.putString("content", content);
            bdf = new Blank();
            bdf.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.mydialog);
            bdf.setArguments(helpbundle);
            bdf.show(fragmentManager, "bdf");
        }
    }

    private void refreshLocalProfile() { //TODO: show user account info
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", operator.getUser_id());
        new OkUtil().call("user_info.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (response) {
                    if (response.isSuccessful()) {
                        try {
                            parseProfile(new JSONObject(response.body().string()));
                        } catch (JSONException e) {
                            LOG.e("refreshLocalProfile", e.getMessage());
                        }
                    }
                }
            }
        });
    }

    private void parseProfile(final JSONObject response) {
        try {
            operator.setHandle(response.getString("radio_hanlde"));
            operator.setCarrier(response.getString("carrier"));
            operator.setStamp(response.getString("stamp"));
            operator.setStamp(response.getString("stamp").replace("\\", ""));
            operator.setRank(response.getString("rank"));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (drifrag.isAdded()) {
                        drifrag.setDriverInfo();
                        drifrag.setRankAndStamp();
                    }
                }
            });
        } catch (JSONException e) {
            LOG.e("parseProfile", e.getMessage());
        }
    }

    private void chstage() {
        if (stage != post) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            switch (post) {
                case 1:
                    binding.tab1.setBackgroundResource(R.drawable.empty_box_white_outline);
                case 2:
                    binding.tab2.setBackgroundResource(R.drawable.empty_box_white_outline);
                case 3:
                    binding.tab3.setBackgroundResource(R.drawable.empty_box_white_outline);
            }
            switch (stage) {
                case 1:
                    transaction.replace(R.id.setswap, confrag, "cfrag");
                    transaction.commit();
                    post = stage;
                    binding.tab1.setBackground(null);
                    binding.tubular.setText(R.string.menu_control_panel);
                    break;
                case 2:
                    transaction.replace(R.id.setswap, drifrag, "dfrag");
                    transaction.commit();
                    post = stage;
                    binding.tab2.setBackground(null);
                    binding.tubular.setText(R.string.menu_driver_info);
                    break;
                case 3:
                    transaction.replace(R.id.setswap, accfrag, "afrag");
                    transaction.commit();
                    post = stage;
                    binding.tab3.setBackground(null);
                    binding.tubular.setText(R.string.menu_account_info);
                    break;
            }
        }
    }

    public void touch(View v) {
        Utils.vibrate(v);
        int id = v.getId();
        if (id == R.id.ghost) {
            clickSound();
            if (!RadioService.appOptions.getGhost_mode()) {
                showResult("Temporarily Closed", "Ghost Mode is temporarily offline");
                return;
            }
            if (billingUtils.isConnected()) {
                billingUtils.queryProductDetails(GHOST, new ProductDetailsResponseListener() {
                    @Override
                    public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> list) {
                        if (!list.isEmpty())
                            billingUtils.purchaseProduct(SettingsActivity.this, list.get(0));
                    }
                });
            }
        } else if (id == R.id.stats) {
            clickSound();
            final Map<String, Object> claims = new HashMap<>();
            claims.put("userId", operator.getUser_id());
            new OkUtil().call("user_stats.php", claims, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {

                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try (response) {
                        if (response.isSuccessful()) {
                            final JSONObject data = new JSONObject(response.body().string());
                            String content = "User ID: " + operator.getUser_id() + " Version: " + data.getString("version_name") + " (" + data.getString("version") + ")";
                            if (!data.getString("created").equals("2015-01-01 00:00:00"))
                                content += "\n" + "Created: " + data.getString("created");
                            content += "Salutes: " + NumberFormat.getNumberInstance(Locale.US).format(data.getInt("salutes")) + ", Flags: " + NumberFormat.getNumberInstance(Locale.US).format(data.getInt("flags"));
                            showResult(operator.getHandle(), content);
                        }
                    } catch (JSONException e) {
                        LOG.e("touch", e.getMessage());
                    }
                }
            });
        } else if (id == R.id.menu) {
            onBackPressed();
        } else if (id == R.id.control) {
            stage = 1;
            if (stage != post) clickSound();
            chstage();
        } else if (id == R.id.driver) {
            stage = 2;
            if (stage != post) clickSound();
            chstage();
        } else if (id == R.id.account) {
            stage = 3;
            if (stage != post) clickSound();
            chstage();
        } else if (id == R.id.update) {
            clickSound();
            if (operator.getAdmin()) {
                FillProfile sdf = (FillProfile) fragmentManager.findFragmentByTag("sdf");
                if (sdf == null) {
                    sdf = new FillProfile();
                    userbundle.putString("profileLink", operator.getProfileLink());
                    userbundle.putString("handle", operator.getHandle());
                    userbundle.putString("carrier", operator.getCarrier());
                    userbundle.putString("location", operator.getTown());
                    sdf.setArguments(userbundle);
                    sdf.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                    sdf.show(fragmentManager, "sdf");
                }
            } else {
                final Map<String, Object> claims = new HashMap<>();
                claims.put("userId", operator.getUser_id());
                new OkUtil().call("user_check_time.php", claims, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful()) {
                            final String data = response.body().string();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try (response) {
                                        final JSONObject object = new JSONObject(data);
                                        if (object.getString("success").equals("1")) {
                                            FillProfile sdf = (FillProfile) fragmentManager.findFragmentByTag("sdf");
                                            if (sdf == null) {
                                                sdf = new FillProfile();
                                                userbundle.putString("profileLink", operator.getProfileLink());
                                                userbundle.putString("handle", operator.getHandle());
                                                userbundle.putString("carrier", operator.getCarrier());
                                                userbundle.putString("location", operator.getTown());
                                                sdf.setArguments(userbundle);
                                                sdf.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                                                sdf.show(fragmentManager, "sdf");
                                            }
                                        } else
                                            showResult(object.getString("title"), object.getString("message"));
                                    } catch (JSONException e) {
                                        LOG.e("update", e.getMessage());
                                    }
                                }
                            });
                        }
                    }
                });
            }
        } else if (id == R.id.blocked) {
            clickSound();
            Blocked bd = (Blocked) fragmentManager.findFragmentByTag("bd");
            if (bd == null) {
                bd = new Blocked();
                bd.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                bd.show(fragmentManager, "bd");
            }
        }
    }

    public void help(View v) {
        Utils.vibrate(v);
        clickSound();
        int id = v.getId();
        if (id == R.id.help41) {
            showResult("Pause Limit", getString(R.string.help41));
        } else if (id == R.id.help2) {
            showResult("Mic Key Behavior", getString(R.string.help2));
        } else if (id == R.id.help3) {
            showResult("Volume Keys", getString(R.string.help3));
        } else if (id == R.id.help7) {
            showResult("Vibration", getString(R.string.help7));
        } else if (id == R.id.help8) {
            showResult("Other Sounds", getString(R.string.help8));
        } else if (id == R.id.help10) {
            showResult("Messaging Options", getString(R.string.help10));
        } else if (id == R.id.help13) {
            showResult("BlueTooth Headset", getString(R.string.help13));
        } else if (id == R.id.help20) {
            showResult("BlackOut", getString(R.string.help20));
        } else if (id == R.id.helpAS) {
            showResult(getString(R.string.mic_animation_speed), getString(R.string.help34));
        } else if (id == R.id.help38) {
            showResult(getString(R.string.share_location), getString(R.string.help38));
        } else if (id == R.id.help40) {
            showResult(getString(R.string.map_theme), getString(R.string.help40));
        } else if (id == R.id.helpPurge) {
            showResult(getString(R.string.purge_limit), getString(R.string.help42));
        } else if (id == R.id.helpNearby) {
            showResult(getString(R.string.nearby_limit), getString(R.string.help43));
        }
    }

    public void displayTerms(View v) {
        Utils.vibrate(v);
        clickSound();
        TermsOfUse tdd = (TermsOfUse) fragmentManager.findFragmentByTag("tdd");
        if (tdd == null) {
            tdd = new TermsOfUse(false);
            tdd.setCancelable(false);
            tdd.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            tdd.show(fragmentManager, "tdd");
        }
    }

    private void clickSound() {
        sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
    }

    public void accountTouch(View v) {
        Utils.vibrate(v);
        clickSound();
        int id = v.getId();
        if (id == R.id.contact) {
            Contact cdf = (Contact) fragmentManager.findFragmentByTag("cdf");
            if (cdf == null) {
                cdf = new Contact();
                cdf.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                cdf.show(fragmentManager, "cdf");
            }
        } else if (id == R.id.shop) {
            if (!RadioService.appOptions.getRadio_shop()) {
                showResult("Temporarily Closed", "The Radio Shop is temporarily offline");
                return;
            }
            RadioShop shop = (RadioShop) fragmentManager.findFragmentByTag("shop");
            if (shop == null) {
                shop = new RadioShop();
                shop.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                Bundle bundle = new Bundle();
                bundle.putString("userId", operator.getUser_id());
                shop.setArguments(bundle);
                shop.show(fragmentManager, "shop");
            }
        }
    }

}




