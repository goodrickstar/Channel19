package com.cb3g.channel19;

import static android.os.SystemClock.sleep;
import static com.cb3g.channel19.RadioService.databaseReference;
import static com.cb3g.channel19.RadioService.gson;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.example.android.multidex.myapplication.R;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.Instant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

@SuppressWarnings("deprecation")
public class MainActivity extends FragmentActivity implements MI, View.OnClickListener, View.OnLongClickListener, PurchasesUpdatedListener, ValueEventListener {
    static List<Coordinates> coordinates = new ArrayList<>();
    public final String SILENCE = "silence";
    public final String UNSILENCE = "unsilence";
    boolean isBound = false;
    boolean ismBound = false;
    private FragmentManager fragmentManager = getSupportFragmentManager();
    private RadioService RS;
    private ServiceConnection SC;
    private BillingUtils billingUtils;
    private boolean delay = true, longPressed = false, dark = false, overide_up = false, overide_down = false;
    private String photoToHandle = "", photoToId = "";
    private TextView day, clock, upper_location_view, handle, carrier, location, title, quecount, gpsLocation, duration, cancel;
    private ImageView icon, gear, skipper, rank, auto, reservoir, profile, backDrop, locationButton, massPhoto, history, blurr;
    private UserList ufrag;
    private Transmitter tfrag;
    private SharedPreferences settings;
    private ConstraintLayout blackout;
    private ProgressBar timeBar;
    private Timer timer;
    private TimerTask clockTask, blackTask;
    private Locale locale;
    private TextView channelName;
    private int tutorial_count = 0;
    private Snackbar snackbar;
    private InterstitialAd mInterstitialAd;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback locationCallback = new locationCallback();
    private DeveloperPayload silencePayload = null;
    private DeveloperPayload unSilencePayload = null;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case "advertise":
                    showRewardAd();
                    break;
                case "review":
                    ReviewManager manager = ReviewManagerFactory.create(MainActivity.this);
                    com.google.android.play.core.tasks.Task<ReviewInfo> request = manager.requestReviewFlow();
                    request.addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            ReviewInfo reviewInfo = task.getResult();
                            Task<Void> flow = manager.launchReviewFlow(MainActivity.this, reviewInfo);
                            flow.addOnCompleteListener(completeTask -> {
                            });
                        }
                    });
                    break;
                case "nineteenGifChosen":
                    launchPicker(RadioService.gson.fromJson(intent.getStringExtra("data"), Gif.class), false);
                    break;
                case "show_result":
                    if (!isFinishing())
                        showResult(intent.getStringExtra("title"), intent.getStringExtra("content"));
                    break;
                case "nineteenPickProfile":
                    photo_picker(3456);
                    break;
                case "exitChannelNineTeen":
                    finish();
                    break;
                case "nineteenLockButtons":
                    lockOthers(intent.getBooleanExtra("data", false));
                    break;
                case "nineteenAddCaption":
                    if (!isFinishing()) {
                        Bundle data = new Bundle();
                        data.putString("data", intent.getStringExtra("data"));
                        Caption cd = new Caption();
                        cd.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                        cd.setArguments(data);
                        cd.show(fragmentManager, "cd");
                    }
                    break;
                case "nineteenUpdateCaption":
                    SendPhoto sendf = (SendPhoto) fragmentManager.findFragmentByTag("sendf");
                    if (sendf != null)
                        sendf.updateCaption(intent.getStringExtra("data"));
                    break;
                case "nineteenSetPauseProgress":
                    int[] set = intent.getIntArrayExtra("data");
                    timeBar.clearAnimation();
                    timeBar.setMax(set[0]);
                    timeBar.setProgress(set[0] - set[1]);
                    break;
                case "switchToPause":
                    if (tfrag.isAdded() && RS != null)
                        tfrag.updateque(RS.getQueue(), RadioService.paused);
                    auto.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.pause_set));
                    break;
                case "switchToPlay":
                    if (tfrag.isAdded() && RS != null)
                        tfrag.updateque(RS.getQueue(), RadioService.paused);
                    auto.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.resume_set));
                    break;
                case "bird":
                    if (tfrag.isAdded()) if (RadioService.recording) {
                        tfrag.stopRecorder(false);
                        if (RS != null) RS.keyUpWasInterupted(intent.getStringExtra("userId"));
                    }
                    Toaster.flipDaBird(MainActivity.this);
                    break;
                case "nineteenToast":
                    if (tfrag.isAdded()) if (RadioService.recording) return;
                    Toaster.toastlow(MainActivity.this, intent.getStringExtra("data"));
                    break;
                case "toasting":
                    if (tfrag.isAdded()) if (RadioService.recording) return;
                    Toaster.labelTwo(MainActivity.this, intent.getStringExtra("data"));
                    break;
                case "nineteenAlert":
                    if (tfrag.isAdded()) if (RadioService.recording) return;
                    Toaster.online(MainActivity.this, intent.getStringExtra("data"), intent.getStringExtra("profileLink"));
                    break;
                case "setMute":
                    setMuteFromOutside(intent.getBooleanExtra("data", false));
                    break;
                case "recordFromMain":
                    recordFromMain();
                    break;
                case "tooth":
                    settooth(intent.getBooleanExtra("data", false));
                    break;
                case "nineteenAllow":
                    ActivityCompat.requestPermissions(MainActivity.this, Utils.getAudioPermissions(), 1);
                    break;
                case "nineteenCamera":
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 3);
                    break;
                case "nineteenCheck":
                    Toaster.checkMark(MainActivity.this);
                    break;
                case "nineteenCross":
                    Toaster.fail(MainActivity.this);
                    break;
            }
        }
    };
    private ExecutorService executor;

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            for (Purchase purchase : purchases) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    billingUtils.handlePurchase(purchase, new ConsumeResponseListener() {
                        @Override
                        public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String s) {
                            switch (purchase.getProducts().get(0)) {
                                case SILENCE:
                                    finishSilence(silencePayload);
                                    silencePayload = null;
                                    break;
                                case UNSILENCE:
                                    finishUnsilence(unSilencePayload);
                                    unSilencePayload = null;
                                    break;
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // 1111, 3456, 3737
        if (requestCode == 1111 || requestCode == 3456 || requestCode == 3737) {
            if (Utils.permissionsAccepted(MainActivity.this, Utils.getStoragePermissions())) {
                try {
                    startActivityForResult(new Intent().setType("image/*").setAction(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE), requestCode);
                } catch (Exception e) {
                    Log.e("photo_picker", e.getMessage());
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void startOrStopGPS(boolean start) {
        if (start) {
            if (Utils.permissionsAccepted(this, Utils.getLocationPermissions()) && RadioService.operator.getLocationEnabled().get()) {
                if (mFusedLocationClient == null)
                    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000)
                        .setWaitForAccurateLocation(true)
                        .setMinUpdateIntervalMillis(30000)
                        .setMaxUpdateDelayMillis(60000)
                        .build();
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        } else {
            if (mFusedLocationClient != null) {
                mFusedLocationClient.removeLocationUpdates(locationCallback);
                mFusedLocationClient = null;
            }
        }
    }

    private void finishSilence(DeveloperPayload developerPayload) {
        databaseReference.child("silenced").child(developerPayload.id).setValue(Instant.now().getEpochSecond());
        showSnack(new Snack(developerPayload.handle + " has been silenced for an hour"));
        sendBroadcast(new Intent("register"));
        if (RS != null)
            RS.silence(developerPayload.id, developerPayload.handle);
    }

    private void finishUnsilence(DeveloperPayload developerPayload) {
        databaseReference.child("silenced").child(developerPayload.id).removeValue();
        showSnack(new Snack(developerPayload.handle + " has been unsilenced"));
        sendBroadcast(new Intent("register"));
        if (RS != null)
            RS.unsilence(developerPayload.id, developerPayload.handle);
    }

    @Override
    public void silence(UserListEntry user) {
        if (RadioService.operator.getAdmin())
            finishSilence(new DeveloperPayload(user.getUser_id(), user.getRadio_hanlde()));
        else {
            if (billingUtils.isConnected()) {
                billingUtils.queryProductDetails(SILENCE, new ProductDetailsResponseListener() {
                    @Override
                    public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> list) {
                        silencePayload = new DeveloperPayload(user.getUser_id(), user.getRadio_hanlde());
                        if (!list.isEmpty())
                            billingUtils.purchaseProduct(MainActivity.this, list.get(0));
                    }
                });
            }
        }
    }

    @Override
    public void unsilence(UserListEntry user) {
        if (RadioService.operator.getAdmin())
            finishUnsilence(new DeveloperPayload(user.getUser_id(), user.getRadio_hanlde()));
        else {
            if (billingUtils.isConnected()) {
                billingUtils.queryProductDetails(UNSILENCE, new ProductDetailsResponseListener() {
                    @Override
                    public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> list) {
                        unSilencePayload = new DeveloperPayload(user.getUser_id(), user.getRadio_hanlde());
                        if (!list.isEmpty())
                            billingUtils.purchaseProduct(MainActivity.this, list.get(0));
                    }
                });
            }
        }
    }

    @Override
    public void photoChosen(Gif gif, boolean upload) {
        if (upload)
            sendBroadcast(new Intent("upload").putExtra("uri", gif.getUrl()).putExtra("mode", 2345).putExtra("caption", "").putExtra("sendToId", photoToId).putExtra("sendToHandle", photoToHandle).putExtra("height", gif.getHeight()).putExtra("width", gif.getWidth()));
        else
            sendBroadcast(new Intent("giphyupload").putExtra("url", gif.getUrl()).putExtra("mode", 2345).putExtra("caption", "").putExtra("sendToId", photoToId).putExtra("sendToHandle", photoToHandle).putExtra("height", gif.getHeight()).putExtra("width", gif.getWidth()));
    }

    @Override
    public void requestBluetoothPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, Utils.getBluetoothPermissions(), 1);
    }

    @Override
    public void launchPicker(Gif gif, boolean upload) {
        if (isFinishing()) return;
        ImagePicker imagePicker = (ImagePicker) fragmentManager.findFragmentByTag("imagePicker");
        if (imagePicker == null) {
            imagePicker = new ImagePicker();
            Bundle bundle = new Bundle();
            bundle.putString("handle", photoToHandle);
            imagePicker.setArguments(bundle);
            imagePicker.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            imagePicker.show(fragmentManager, "imagePicker");
        } else {
            imagePicker.setPhoto(gif, upload);
        }
    }

    @Override
    public void launchSearch(String id) {
        if (isFinishing()) return;
        ImageSearch imageSearch = (ImageSearch) fragmentManager.findFragmentByTag("imageSearch");
        if (imageSearch == null) {
            imageSearch = new ImageSearch();
            Bundle bundle = new Bundle();
            bundle.putString("data", id);
            imageSearch.setArguments(bundle);
            imageSearch.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            imageSearch.show(fragmentManager, "imageSearch");
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case 1111:
                    Gif gif = new Gif();
                    gif.setUrl(data.getData().toString());
                    launchPicker(gif, true);
                    break;
                case 3737:
                    if (!isFinishing()) {
                        MassPhoto mass = (MassPhoto) fragmentManager.findFragmentByTag("mass");
                        if (mass == null) {
                            mass = new MassPhoto();
                            Bundle bundle = new Bundle();
                            bundle.putString("data", String.valueOf(data.getData()));
                            mass.setArguments(bundle);
                            mass.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                            mass.show(fragmentManager, "mass");
                        }
                    }
                    break;
                default:
                    sendBroadcast(new Intent("upload").putExtra("uri", String.valueOf(data.getData())).putExtra("mode", requestCode).putExtra("caption", "").putExtra("sendToId", photoToId).putExtra("sendToHandle", photoToHandle));
                    photoToHandle = "";
                    photoToId = "";
                    break;
            }
        }
    }

    @Override
    public int returnUserVolume(String id) {
        if (RS != null) return RS.returnUserVolume(id);
        else return 85;
    }

    @Override
    public UserListEntry returnTalkerEntry() {
        if (RS != null) return RS.returnTalkerEntry();
        return null;
    }

    @Override
    public void longFlagUser(UserListEntry user) {
        if (RS != null) RS.longFlagUser(user);
    }

    @Override
    public void selectChannel(boolean cancelable) {
        if (isFinishing()) return;
        Channels channels = (Channels) fragmentManager.findFragmentByTag("channels");
        if (channels == null) {
            channels = new Channels();
            channels.setCancelable(cancelable);
            channels.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
        }
        channels.show(fragmentManager, "channels");
    }

    @Override
    public void launchChannel(Channel channel) {
        if (RadioService.operator.getChannel() != null)
            if (RadioService.operator.getChannel().getChannel() == channel.getChannel()) return;
        channelName.setText(channel.getChannel_name());
        delay = true;
        resetAnimtion();
        if (!RadioService.operator.getAdmin()) {
            sendBroadcast(new Intent("nineteenEmptyPlayer"));
            if (tfrag.isAdded()) {
                tfrag.updateDisplay(new String[]{onlineStatus(), "", "", "", "f", "none"}, 0);
                tfrag.updateque(0, RadioService.paused);
            }
        }
        sendBroadcast(new Intent("nineteenStaticSound"));
        if (RS != null) RS.entered(channel);
    }

    @Override
    public void createChannel() {
        if (isFinishing()) return;
        CreateChannel createChannel = (CreateChannel) fragmentManager.findFragmentByTag("createChannel");
        if (createChannel == null) {
            createChannel = new CreateChannel();
            createChannel.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.mydialog);
            createChannel.show(fragmentManager, "createChannel");
        }
    }

    @Override
    public void enterPin(Channel channel) {
        if (isFinishing()) return;
        SharedPreferences saved = getSharedPreferences("channels", Context.MODE_PRIVATE);
        List<Integer> channels = RadioService.gson.fromJson(saved.getString("channels", "[]"), new TypeToken<List<Integer>>() {
        }.getType());
        if (channels == null) channels = new ArrayList<>();
        if (channels.contains(channel.getChannel()) || RadioService.operator.getAdmin()) {
            launchChannel(channel);
        } else {
            EnterPassword enterPassword = (EnterPassword) fragmentManager.findFragmentByTag("createChannel");
            if (enterPassword == null) {
                enterPassword = new EnterPassword();
                Bundle bundle = new Bundle();
                bundle.putString("data", RadioService.gson.toJson(channel));
                enterPassword.setCancelable(false);
                enterPassword.setArguments(bundle);
                enterPassword.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.mydialog);
                enterPassword.show(fragmentManager, "enterPassword");
            }
        }
    }

    @Override
    public void findUser(Coordinates coordinates) {
        this.startActivity(new Intent(MainActivity.this, Locations.class).putExtra("data", RadioService.gson.toJson(coordinates)));
    }

    @Override
    public void showSnack(Snack snack) {
        if (dark) snackbar = Snackbar.make(blackout, snack.getMessage(), snack.getLength());
        else
            snackbar = Snackbar.make(findViewById(R.id.bottomframe), snack.getMessage(), snack.getLength());
        View view = snackbar.getView();
        TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(ContextCompat.getColor(this, R.color.main_white));
        if (dark) view.setBackgroundColor(ContextCompat.getColor(this, R.color.main_black));
        else view.setBackgroundColor(ContextCompat.getColor(this, R.color.main_black_transparent));
        if (snack.getLength() == Snackbar.LENGTH_INDEFINITE) {
            RadioService.occupied.set(false);
            snackbar.setActionTextColor(Color.WHITE);
            snackbar.setAction("10 4", view1 -> {
                sendBroadcast(new Intent("nineteenVibrate"));
                snackbar.dismiss();
            });
        } else {
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        snackbar.addCallback(new Snackbar.Callback() {

            @Override
            public void onShown(Snackbar snackbar) {
                RadioService.occupied.set(true);
            }

            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                if (RadioService.snacks.size() > 0) RadioService.snacks.remove(0);
                RadioService.occupied.set(false);
                if (RS != null) RS.checkForMessages();

            }
        });
        snackbar.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        settings = getSharedPreferences("settings", MODE_PRIVATE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.main_layout);
        billingUtils = new BillingUtils(this, this);
        locale = Locale.getDefault();
        findbyid();
        RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(1000);
        rotate.setRepeatCount(1);
        rotate.setInterpolator(new LinearInterpolator());
        skipper = findViewById(R.id.skip);
        ufrag = new UserList();
        tfrag = new Transmitter();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.bottomframe, tfrag, "tf");
        transaction.commit();
        backDrop = findViewById(R.id.backdrop);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NotNull InitializationStatus initializationStatus) {
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!RadioService.operator.getAdmin())
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        SC = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                RadioService.LocalBinder binder = (RadioService.LocalBinder) service;
                RS = binder.getService();
                RS.main_activity_callbacks(MainActivity.this);
                RS.listenForCoordinates(true);
                setMuteFromOutside(RS.updateMute());
                updateDisplay(RS.getlatest(), RS.getQueue(), RS.getDuration(), RS.returnedPaused(), RS.returnPoor(), RS.getlatestStamp());
                if (RadioService.paused)
                    auto.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.resume_set));
                else {
                    auto.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.pause_set));
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                updateLocationDisplay(RS.returnLocation());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        isBound = bindService(new Intent(this, RadioService.class), SC, BIND_IMPORTANT);
        registerReceiver(receiver, returnFilter());
        delay = true;
        if (settings.getBoolean("exiting", false)) {
            startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finishAffinity();
        }
        gear.setEnabled(true);
        startOrStopGPS(true);
        executor = ExecutorUtils.newSingleThreadExecutor();
    }

    @Override
    protected void onStop() {
        super.onStop();
        startOrStopGPS(false);
        if (RS != null) {
            RS.listenForCoordinates(false);
            RS.main_activity_callbacks(null);
        }
        if (isBound) unbindService(SC);
        RS = null;
        unregisterReceiver(receiver);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        gear.setEnabled(false);
        ExecutorUtils.shutdown(executor);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (clockTask != null) clockTask.cancel();
        if (blackTask != null) blackTask.cancel();
        if (timer != null) timer.purge();
        blackTask = null;
        clockTask = null;
        timer = null;
        lockOthers(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (RadioService.operator == null) {
            longPressed = true;
            onBackPressed();
            return;
        }
        if (RadioService.operator.getUser_id() == null) {
            longPressed = true;
            onBackPressed();
            return;
        }
        timer = new Timer();
        clockTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sendBroadcast(new Intent("fetch_users"));
                        updateClock();
                    }
                });
            }
        };
        timer.schedule(clockTask, DateUtils.MINUTE_IN_MILLIS - System.currentTimeMillis() % DateUtils.MINUTE_IN_MILLIS, 60000);
        int black = settings.getInt("black", 10);
        if (black != 0) {
            black = (black * 3500) + 10000;
            blackTask = new TimerTask() {
                @Override
                public void run() {
                    if (dark || (tfrag.isAdded() && RadioService.recording)) return;
                    if (!delay && RadioService.operator.getChannel() != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                darkChange(true);
                            }
                        });
                    }
                    delay = false;
                }
            };
            timer.schedule(blackTask, black, black);
        }
        overide_up = settings.getBoolean("overideup", false);
        overide_down = settings.getBoolean("overidedown", false);
        tutorial_count = settings.getInt("tutorial", 0);
        if (tutorial_count == 5) finishTutorial(tutorial_count);
        updateClock();
        sendBroadcast(new Intent("checkForMessages"));
        if (settings.getBoolean("custom", false)) {
            changeBackground(settings.getString("background", "default"));
        } else changeBackground(settings.getString("main_backdrop", ""));
        if (RadioService.operator.getChannel() != null) {
            channelName.setText(RadioService.operator.getChannel().getChannel_name());
        } else {
            channelName.setText(this.getString(R.string.select_channel));
            if (tutorial_count > 5) {
                selectChannel(false);
            }
        }
        if (settings.getBoolean("flagDue", false)) {
            displayLongFlag(settings.getString("flagSenderId", "Unknown"), settings.getString("flagSenderHandle", "Unknown"));
        }
    }

    @Override
    public void changeBackground(String link) {
        new GlideImageLoader(this, backDrop).load(link);
    }

    @SuppressWarnings("deprecation")
    private boolean serviceAlive() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RadioService.class.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }

    @Override
    public String[] getDisplayedText() {
        if (RS != null) return RS.getlatest();
        return new String[]{onlineStatus(), "", "", "", "f", "none"};
    }

    @Override
    public long getStamp() {
        if (RS != null) return RS.getlatestStamp();
        else return 0;
    }

    @Override
    public int getQueue() {
        if (RS != null) return RS.getQueue();
        else return 0;
    }

    @Override
    public void updateDisplay(final String[] display, final int count, final int duration, final boolean paused, final boolean poor, final long stamp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (tfrag.isAdded()) tfrag.updateDisplay(display, stamp);
                if (display[0].equals("Welcome...")) return;
                updateDarkDisplay(display, stamp);
                updateQueu(count, paused, poor);
                if (duration >= 0) animateMax(duration);
            }
        });
    }

    @Override
    public void updateQueu(int count, boolean paused, boolean poor) {
        if (tfrag.isAdded()) tfrag.updateque(count, paused);
        if (count == 0 || paused) {
            if (!timeBar.isIndeterminate()) timeBar.setIndeterminate(true);
        } else {
            if (timeBar.isIndeterminate()) timeBar.setIndeterminate(false);
        }
        if (count > 0) quecount.setText(String.valueOf(count));
        else quecount.setText(" ");
        adjustColors(poor);
    }

    @Override
    public void adjustColors(boolean poor) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (poor) {
                    day.setTextColor(Color.RED);
                    clock.setTextColor(Color.RED);
                    quecount.setTextColor(Color.RED);
                    gpsLocation.setTextColor(Color.RED);
                    title.setTextColor(Color.RED);
                    duration.setTextColor(Color.RED);
                    if (dark)
                        timeBar.setProgressDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.red_bar));
                } else {
                    day.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.light_blue));
                    clock.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.light_blue));
                    quecount.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.light_blue));
                    gpsLocation.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.light_blue));
                    title.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.light_blue));
                    duration.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.light_blue));
                    if (dark)
                        timeBar.setProgressDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.blue_bar));
                }
                if (!dark)
                    timeBar.setProgressDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.white_bar));
                if (timeBar.isIndeterminate()) {
                    if (!dark)
                        timeBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                    else {
                        if (poor)
                            timeBar.getIndeterminateDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
                        else
                            timeBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.light_blue), PorterDuff.Mode.SRC_ATOP);
                    }
                }
            }
        });
    }

    @Override
    public String[] queCheck(int location) {
        if (RS != null) return RS.queCheck(location);
        else return null;
    }

    @Override
    public long queStamp(int postion) {
        if (RS != null) return RS.queStamp(postion);
        else return 0;
    }

    private void animateMax(int set) {
        if (set != 0) {
            timeBar.setMax(set);
            timeBar.setProgress(set);
            ProgressAnimation anim = new ProgressAnimation(timeBar, set);
            anim.setDuration(set + 300);
            timeBar.startAnimation(anim);
        } else {
            resetAnimtion();
        }
    }

    @Override
    public void updateLocationDisplay(String location) {
        gpsLocation.setText(location);
        if (location.equals("")) upper_location_view.setText(R.string.location_unknown);
        else upper_location_view.setText(location);
    }

    @Override
    public void blockAll(String id, String handle) {
        if (RS != null) RS.blockAll(id, handle);
    }

    @Override
    public void blockThisUser(String id, String handle, boolean toast) {
        if (RS != null) RS.blockNewId(id, handle, toast);
    }

    @Override
    public void blockPhoto(String id, String handle, boolean toast) {
        if (RS != null) RS.blockNewPhoto(id, handle, toast);
    }

    @Override
    public void blockText(String id, String handle, boolean toast) {
        if (RS != null) RS.blockNewText(id, handle, toast);
    }

    @Override
    public void flagOut(String id) {
        if (RS != null) RS.flag_out(id);
    }

    @Override
    public void bannUser(String id) {
        if (RS != null) RS.bannUser(id);
    }

    @Override
    public void recordChange(boolean recording) {
        Log.i("focus", "recordChange()");
        if (RS != null) {
            RS.recording(recording);
            if (recording) resetAnimtion();
            else resumeAnimation(RS.returnAnimationMax());
            delay = true;
            if (recording) {
                if (snackbar != null) {
                    if (snackbar.isShown()) {
                        snackbar.dismiss();
                        if (!RadioService.snacks.isEmpty()) RadioService.snacks.remove(0);
                    }
                }
                RadioService.occupied.set(false);
                AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
                fadeIn.setDuration(200);
                blurr.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.VISIBLE);
                blurr.startAnimation(fadeIn);
                cancel.startAnimation(fadeIn);
            } else {
                blurr.clearAnimation();
                cancel.clearAnimation();
                blurr.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);
            }
            lockOthers(recording);
        }
    }

    private void darkChange(Boolean show) {
        if (tutorial_count < 6) return;
        dark = show;
        delay = true;
        if (show) blackout.setVisibility(View.VISIBLE);
        else blackout.setVisibility(View.GONE);
        if (snackbar != null) {
            if (snackbar.isShown()) {
                snackbar.dismiss();
                RadioService.occupied.set(false);
            }
        }
        if (RS != null) {
            adjustColors(RS.returnPoor());
            RS.checkForMessages();
        }
    }

    @Override
    public void pauseOrplay(UserListEntry user) {
        if (RS != null) RS.pauseOrplay(user);
    }

    @Override
    public void flagThisUser(UserListEntry user) {
        if (RS != null) RS.flagUser(user);
    }

    @Override
    public void kickUser(UserListEntry user) {
        if (RS != null) RS.kickUser(user);
    }

    @Override
    public void saluteThisUser(UserListEntry user) {
        if (RS != null) RS.saluteUser(user);
    }

    @Override
    public void sendPhoto(String id, String handle) {
        photoToHandle = handle;
        photoToId = id;
        launchPicker(null, false);
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onBackPressed() {
        if (RadioService.recording) return;
        if (dark) {
            darkChange(false);
            return;
        }
        if (longPressed) {
            longPressed = false;
            sendBroadcast(new Intent("exitChannelNineTeen").setPackage("com.cb3g.channel19"));
            super.onBackPressed();
            //stopService(new Intent(this, RadioService.class));
        } else {
            sendBroadcast(new Intent("nineteenVibrate").setPackage("com.cb3g.channel19"));
            longPressed = true;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    sleep(600);
                    longPressed = false;
                    //showSnack(new Snack("double-tap back button to exit", Snackbar.LENGTH_SHORT));
                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                if (dark) darkChange(false);
                else darkChange(true);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (overide_up) {
                    event.startTracking();
                    return true;
                } else return super.onKeyDown(keyCode, event);
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (overide_down) {
                    event.startTracking();
                    return true;
                } else return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && overide_up) {
            sendBroadcast(new Intent("nineteenPlayPause"));
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && overide_down) {
            sendBroadcast(new Intent("purgeNineTeen"));
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (((event.getFlags() & KeyEvent.FLAG_CANCELED_LONG_PRESS) == 0)) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (overide_up) {
                    recordFromMain();
                    return true;
                } else return super.onKeyUp(keyCode, event);
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (overide_down) {
                    sendBroadcast(new Intent("nineteenSkip"));
                    return true;
                } else return super.onKeyUp(keyCode, event);
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void recordFromMain() {
        if (dark) darkChange(false);
        else {
            if (tfrag.isAdded()) tfrag.mainRecord();
            else flip(null);
        }
    }

    private void settooth(boolean show) {
        if (tfrag.isAdded()) tfrag.settooth(show);
    }

    private void setMuteFromOutside(boolean mute) {
        if (tfrag.isAdded()) tfrag.setMute(mute);
    }

    @Override
    public void onClick(View v) {
        delay = true;
        switch (v.getId()) {
            case R.id.cancel:
                sendBroadcast(new Intent("nineteenVibrate"));
                if (tfrag.isAdded()) tfrag.stopRecorder(false);
                break;
            case R.id.quecount:
                if (RadioService.operator.getChannel() != null) {
                    if (RS != null) {
                        if (RadioService.paused)
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                    sendBroadcast(new Intent("nineteenVibrate"));
                    sendBroadcast(new Intent("nineteenPlayPause"));
                }
                break;
            case R.id.history:
                if (RadioService.operator.getHinderPhotos() || RadioService.operator.getHinderTexts())
                    return;
                sendBroadcast(new Intent("nineteenVibrate"));
                if (RadioService.operator.getSilenced()) {
                    showSnack(new Snack("You are currently silenced", Snackbar.LENGTH_SHORT));
                    return;
                }
                sendBroadcast(new Intent("nineteenClickSound"));
                display_message_history();
                break;
            case R.id.massButton:
                if (RadioService.operator.getChannel() == null || RadioService.operator.getHinderPhotos())
                    return;
                sendBroadcast(new Intent("nineteenVibrate"));
                if (RadioService.operator.getSilenced()) {
                    showSnack(new Snack("You are currently silenced", Snackbar.LENGTH_SHORT));
                    return;
                }
                sendBroadcast(new Intent("nineteenClickSound"));
                photo_picker(3737);
                break;
            case R.id.channel_name:
                sendBroadcast(new Intent("nineteenVibrate"));
                sendBroadcast(new Intent("nineteenClickSound"));
                selectChannel(true);
                break;
            case R.id.locationButton:
                if (RadioService.operator.getChannel() == null) return;
                sendBroadcast(new Intent("nineteenVibrate"));
                sendBroadcast(new Intent("nineteenClickSound"));
                this.startActivity(new Intent(MainActivity.this, Locations.class));
                break;
            case R.id.sideband:
                if (RadioService.operator.getChannel() != null) {
                    sendBroadcast(new Intent("nineteenClickSound"));
                    sendBroadcast(new Intent("nineteenVibrate"));
                    startActivity(new Intent(MainActivity.this, ReservoirActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                }
                break;
            case R.id.auto:
                if (RadioService.operator.getChannel() == null) return;
                if (RS != null) {
                    if (RadioService.paused)
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                sendBroadcast(new Intent("nineteenPlayPause"));
                break;
            case R.id.blackSkip:
                if (RS != null) {
                    if (RS.getQueue() != 0) {
                        sendBroadcast(new Intent("nineteenSkip"));
                        sendBroadcast(new Intent("nineteenVibrate"));
                    } else darkChange(false);
                }
                break;
            case R.id.blackOut:
                darkChange(false);
                break;
            case R.id.skip:
                sendBroadcast(new Intent("nineteenVibrate"));
                if (RadioService.operator.getChannel() == null) return;
                sendBroadcast(new Intent("nineteenSkip"));
                break;
            case R.id.gear:
                sendBroadcast(new Intent("nineteenVibrate"));
                sendBroadcast(new Intent("nineteenTabSound"));
                startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        delay = true;
        switch (v.getId()) {
            case R.id.massButton:
                sendBroadcast(new Intent("nineteenVibrate"));
                sendBroadcast(new Intent("nineteenClickSound"));
                MassPast massPast = new MassPast();
                massPast.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                massPast.show(fragmentManager, "massPast");
                break;
            case R.id.gear:
                sendBroadcast(new Intent("nineteenVibrate"));
                sendBroadcast(new Intent("nineteenStaticSound"));
                showSnack(new Snack("Rewind Five", Snackbar.LENGTH_SHORT));
                RotateAnimation counterClockwise = new RotateAnimation(360, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                counterClockwise.setDuration(2000);
                gear.startAnimation(counterClockwise);
                if (RS != null) RS.rewind();
                break;
            case R.id.auto:
                if (!isFinishing()) {
                    sendBroadcast(new Intent("nineteenClickSound"));
                    sendBroadcast(new Intent("nineteenVibrate"));
                    QueueDialog qd = (QueueDialog) fragmentManager.findFragmentByTag("qd");
                    if (qd == null) {
                        qd = new QueueDialog();
                        qd.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                        qd.show(fragmentManager, "qd");
                    }
                }
                break;
            case R.id.blackSkip:
                if (RS != null) {
                    if (RS.getQueue() != 0) {
                        sendBroadcast(new Intent("purgeNineTeen"));
                        sendBroadcast(new Intent("nineteenVibrate"));
                    } else darkChange(false);
                }
                break;
            case R.id.user:
                sendBroadcast(new Intent("nineteenVibrate"));
                darkChange(true);
                break;
            case R.id.skip:
                if (RS != null)
                    if (RS.getQueue() != 0) {
                        sendBroadcast(new Intent("nineteenVibrate"));
                        sendBroadcast(new Intent("purgeNineTeen"));
                    }
                break;
        }
        return true;
    }

    @Override
    public void streamFile(String imageLink) {
        if (isFinishing()) return;
        FullScreen fullScreen = (FullScreen) fragmentManager.findFragmentByTag("fsf");
        if (fullScreen == null) {
            fullScreen = new FullScreen();
            Bundle bundle = new Bundle();
            bundle.putString("data", imageLink);
            fullScreen.setArguments(bundle);
            fullScreen.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            fullScreen.show(fragmentManager, "fsf");
        }
    }

    @Override
    public void showListOptions(UserListEntry user) {
        if (isFinishing()) return;
        UserListOptionsNew cdf = (UserListOptionsNew) fragmentManager.findFragmentByTag("options");
        if (cdf == null) {
            Bundle bundle = new Bundle();
            bundle.putString("user", new Gson().toJson(user));
            cdf = new UserListOptionsNew();
            cdf.setArguments(bundle);
            cdf.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            cdf.show(fragmentManager, "options");
        }
    }

    private void showResult(String title, String content) {
        if (RadioService.recording || isFinishing()) return;
        Blank bdf = (Blank) fragmentManager.findFragmentByTag("bdf");
        if (bdf == null) {
            Bundle bundle = new Bundle();
            bundle.putString("title", title);
            bundle.putString("content", content);
            bdf = new Blank();
            bdf.setArguments(bundle);
            bdf.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.mydialog);
            bdf.setCancelable(false);
            bdf.show(fragmentManager, "bdf");
        }
    }

    @Override
    public void displayChat(UserListEntry user, boolean sound, boolean launch) {
        if (isFinishing()) return;
        Chat chat_dialog = (Chat) fragmentManager.findFragmentByTag("chatd");
        if (chat_dialog == null) {
            Bundle bundle = new Bundle();
            bundle.putString("data", RadioService.gson.toJson(user));
            bundle.putBoolean("launch", launch);
            chat_dialog = new Chat();
            chat_dialog.setArguments(bundle);
            chat_dialog.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            chat_dialog.show(fragmentManager, "chatd");
        } else chat_dialog.gather_history(sound, true);
    }

    @Override
    public void createPm(UserListEntry user) {
        if (isFinishing()) return;
        SendMessage spmf = (SendMessage) fragmentManager.findFragmentByTag("dpmf");
        if (spmf == null) {
            Bundle bundle = new Bundle();
            bundle.putString("userId", user.getUser_id());
            bundle.putString("handle", user.getRadio_hanlde());
            bundle.putString("rank", user.getRank());
            bundle.putString("profileLink", user.getProfileLink());
            spmf = new SendMessage();
            spmf.setArguments(bundle);
            spmf.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            spmf.show(fragmentManager, "dpmf");
        }
    }

    @Override
    public void displayPm(String... data) {
        if (isFinishing()) return;
        ShowMessage shpmfr = (ShowMessage) fragmentManager.findFragmentByTag("npmf");
        if (shpmfr == null) {
            Bundle bundle = new Bundle();
            bundle.putStringArray("data", data);
            shpmfr = new ShowMessage();
            shpmfr.setArguments(bundle);
            shpmfr.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            shpmfr.show(fragmentManager, "npmf");
        }
    }

    @Override
    public void displayPhoto(Photo photo) {
        if (isFinishing()) return;
        ShowPhoto showPhoto = (ShowPhoto) fragmentManager.findFragmentByTag("showPhoto");
        if (showPhoto == null) {
            Bundle bundle = new Bundle();
            bundle.putString("data", gson.toJson(photo));
            showPhoto = new ShowPhoto();
            showPhoto.setArguments(bundle);
            showPhoto.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            showPhoto.show(fragmentManager, "showPhoto");
        }
    }

    private IntentFilter returnFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("advertise");
        filter.addAction("bird");
        filter.addAction("review");
        filter.addAction("show_result");
        filter.addAction("nineteenPickProfile");
        filter.addAction("exitChannelNineTeen");
        filter.addAction("nineteenLockButtons");
        filter.addAction("recordFromMain");
        filter.addAction("toasting");
        filter.addAction("nineteenAlert");
        filter.addAction("nineteenCross");
        filter.addAction("nineteenCheck");
        filter.addAction("nineteenAddCaption");
        filter.addAction("nineteenSlide");
        filter.addAction("nineteenUpdateCaption");
        filter.addAction("switchToPlay");
        filter.addAction("switchToPause");
        filter.addAction("nineteenSetPauseProgress");
        filter.addAction("setMute");
        filter.addAction("tooth");
        filter.addAction("nineteenToast");
        filter.addAction("nineteenAllow");
        filter.addAction("nineteenCamera");
        filter.addAction("nineteenGifChosen");
        return filter;
    }

    @Override
    public void displayLongFlag(String senderId, String senderHandle) {
        if (tfrag.isAdded()) if (RadioService.recording) {
            tfrag.stopRecorder(false);
            if (RS != null) RS.keyUpWasInterupted(senderId);
        }
        Bundle data = new Bundle();
        data.putString("data", senderHandle);
        LongFlagFragment LFF = (LongFlagFragment) fragmentManager.findFragmentByTag("LFF");
        if (LFF == null) {
            LFF = new LongFlagFragment();
            LFF.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            LFF.setArguments(data);
            LFF.setCancelable(false);
            LFF.show(fragmentManager, "LFF");
        }
    }

    @Override
    public void stopRecorder(boolean pass) {
        if (tfrag.isAdded()) tfrag.stopRecorder(pass);
    }

    @Override
    public void startTransmit() {
        if (tfrag.isAdded()) tfrag.transmitStart();
    }

    @Override
    public void postKeyUp() {
        if (RS != null) RS.post_key_up();
    }

    @Override
    public void showRewardAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-4635898093945616/6205382793",
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Logger.INSTANCE.e(loadAdError.getMessage());
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdShowedFullScreenContent() {
                                sendBroadcast(new Intent("nineteenPause"));
                                stopRecorder(false);
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NotNull AdError adError) {
                                sendBroadcast(new Intent("nineteenPlayPause"));
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                sendBroadcast(new Intent("nineteenPlayPause"));
                            }
                        });
                        rewardedAd.show(MainActivity.this, new OnUserEarnedRewardListener() {
                            @Override
                            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {

                            }
                        });
                    }

                });

    }

    @Override
    public void finishTutorial(int count) {
        tutorial_count = count;
        int margin = ((Number) (getResources().getDisplayMetrics().density * 16)).intValue();
        RelativeLayout.LayoutParams tps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tps.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        tps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        tps.setMargins(margin, margin, margin, margin);
        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lps.setMargins(margin, margin, margin, margin);
        ShowcaseView showcaseView = new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(R.id.skip, this))
                .setContentTitle("SKIP BUTTON")
                .setContentText("Skips the current transmission which consequently lowers the queue")
                .setStyle(R.style.CustomShowcaseTheme)
                .blockAllTouches()
                .build();
        showcaseView.setButtonPosition(tps);
        showcaseView.show();
        showcaseView.overrideButtonClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast(new Intent("nineteenVibrate"));
                tutorial_count++;
                settings.edit().putInt("tutorial", tutorial_count).apply();
                switch (tutorial_count) {
                    case 5:
                        showcaseView.setButtonPosition(lps);
                        showcaseView.setTarget(new ViewTarget(R.id.channel_name, MainActivity.this));
                        showcaseView.setContentTitle("Current Channel");
                        showcaseView.setContentText("Unlimited public and private channels");
                        break;
                    case 6:
                        showcaseView.hide();
                        selectChannel(false);
                        Toaster.toastlow(MainActivity.this, "Join a channel or create a new one");
                        break;
                }
            }
        });
    }

    @Override
    public void resumeAnimation(int[] data) {
        if (data[0] <= 0 || data[1] <= 0) resetAnimtion();
        else {
            int set = data[0] - data[1];
            if (set + 300 > 0) {
                timeBar.setMax(data[0]);
                timeBar.setProgress(set);
                ProgressAnimation anim = new ProgressAnimation(timeBar, set);
                anim.setDuration(set + 300);
                timeBar.startAnimation(anim);
            }
        }
    }

    @Override
    public void photo_picker(int request_code) {
        if (Utils.permissionsAccepted(this, Utils.getStoragePermissions())) {
            try {
                startActivityForResult(new Intent().setType("image/*").setAction(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE), request_code);
            } catch (Exception e) {
                Log.e("photo_picker", e.getMessage());
            }
        } else
            Utils.requestPermission(MainActivity.this, Utils.getStoragePermissions(), request_code);
    }

    private void resetAnimtion() {
        timeBar.clearAnimation();
        timeBar.setProgress(0);
    }

    @Override
    public void lockOthers(boolean lock) {
        if (timeBar.isIndeterminate() && !lock) timeBar.setIndeterminate(false);
        icon.setEnabled(!lock);
        gear.setEnabled(!lock);
        reservoir.setEnabled(!lock);
        skipper.setEnabled(!lock);
        auto.setEnabled(!lock);
        locationButton.setEnabled(!lock);
        massPhoto.setEnabled(!lock);
        history.setEnabled(!lock);
        channelName.setEnabled(!lock);
    }

    @SuppressLint("SetTextI18n")
    private void updateDarkDisplay(String[] display, long stamp) {
        if (display[0].contains("Online") || display[0].contains("Dead")) handle.setText(" ");
        else handle.setText(display[0]);
        carrier.setText(display[1]);
        location.setText(display[2]);
        title.setText(Utils.formatDiff(Utils.timeDifferance(stamp), false));
        if (display[3].equals("0") || display[3].isEmpty()) duration.setText("");
        else duration.setText(display[3] + "s");
        new GlideImageLoader(this, rank).load(Utils.parseRankUrl(display[4]));
        if (display[5].equals("none")) {
            profile.setImageDrawable(null);
            profile.setOnClickListener(null);
        } else {
            profile.setVisibility(View.VISIBLE);
            new GlideImageLoader(this, profile).load(display[5], RadioService.profileOptions);
            profile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendBroadcast(new Intent("nineteenVibrate"));
                    sendBroadcast(new Intent("nineteenBoxSound"));
                    streamFile(display[5]);
                }
            });
            profile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    UserListEntry user = returnTalkerEntry();
                    if (user != null) {
                        sendBroadcast(new Intent("nineteenVibrate"));
                        sendBroadcast(new Intent("nineteenClickSound"));
                        showListOptions(user);
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void updateClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd", locale);
        Date d = new Date();
        day.setText(sdf.format(d));
        clock.setText(String.format("%tR", System.currentTimeMillis()));
        if (RS != null) RS.checkForMessages();
    }

    @Override
    public void display_message_history() {
        if (isFinishing()) return;
        sendBroadcast(new Intent("nineteenVibrate"));
        sendBroadcast(new Intent("nineteenClickSound"));
        MessageHistory history = (MessageHistory) fragmentManager.findFragmentByTag("history");
        if (history == null) {
            history = new MessageHistory();
            history.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            history.show(fragmentManager, "history");
        }
    }

    @Override
    public void updateUserList() {
        if (ufrag.isAdded()) ufrag.update_users_list();
    }

    private String onlineStatus() {
        if (RS != null) return RS.getOnlineStatus();
        else return "Online";
    }

    public void flip(View v) {
        sendBroadcast(new Intent("nineteenVibrate"));
        if (RadioService.operator.getChannel() == null) return;
        sendBroadcast(new Intent("nineteenClickSound"));
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (tfrag.isAdded()) transaction.replace(R.id.bottomframe, ufrag);
        else transaction.replace(R.id.bottomframe, tfrag);
        transaction.commit();
        delay = true;
    }

    private void findbyid() {
        cancel = findViewById(R.id.cancel);
        auto = findViewById(R.id.auto);
        reservoir = findViewById(R.id.sideband);
        profile = findViewById(R.id.option_image_view);
        clock = findViewById(R.id.clock);
        day = findViewById(R.id.day);
        timeBar = findViewById(R.id.timeTimer);
        icon = findViewById(R.id.user);
        quecount = findViewById(R.id.quecount);
        gpsLocation = findViewById(R.id.gpsLocation);
        duration = findViewById(R.id.duration);
        upper_location_view = findViewById(R.id.subtitle);
        gear = findViewById(R.id.gear);
        skipper = findViewById(R.id.skip);
        blackout = findViewById(R.id.blackOut);
        View blackSkip = findViewById(R.id.blackSkip);
        handle = findViewById(R.id.handle);
        carrier = findViewById(R.id.carrier);
        location = findViewById(R.id.banner);
        title = findViewById(R.id.title);
        title.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.light_blue));
        rank = findViewById(R.id.starIV);
        auto.setVisibility(View.VISIBLE);
        skipper.setVisibility(View.VISIBLE);
        locationButton = findViewById(R.id.locationButton);
        channelName = findViewById(R.id.channel_name);
        history = findViewById(R.id.history);
        massPhoto = findViewById(R.id.massButton);
        history.setOnClickListener(this);
        massPhoto.setOnClickListener(this);
        massPhoto.setOnLongClickListener(this);
        channelName.setOnClickListener(this);
        reservoir.setOnClickListener(this);
        icon.setOnLongClickListener(this);
        blackout.setOnClickListener(this);
        gear.setOnClickListener(this);
        if (RadioService.operator.getSubscribed()) gear.setOnLongClickListener(this);
        blackSkip.setOnClickListener(this);
        blackSkip.setOnLongClickListener(this);
        skipper.setOnClickListener(this);
        skipper.setOnLongClickListener(this);
        auto.setOnClickListener(this);
        auto.setOnLongClickListener(this);
        locationButton.setOnClickListener(this);
        quecount.setOnClickListener(this);
        blurr = findViewById(R.id.blurr);
        cancel.setOnClickListener(this);
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {
        if (snapshot.exists()) {
            int tokens = snapshot.getValue(int.class);
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {
        Logger.INSTANCE.i("onCancelled()");
    }

    class locationCallback extends LocationCallback {
        @Override
        public void onLocationResult(@NotNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult != null) {
                for (Location location : locationResult.getLocations()) {
                    if (RS != null) RS.locationUpdated(location);
                    if (RadioService.operator.getSharing())
                        databaseReference.child("locations").child(RadioService.operator.getUser_id()).setValue(new Coordinates(RadioService.operator.getUser_id(), RadioService.operator.getHandle(), RadioService.operator.getProfileLink(), location.getLatitude(), location.getLongitude(), location.getBearing(), location.getSpeed(), location.getAltitude()));
                }
            }
        }
    }

    class DeveloperPayload {
        String id;
        String handle;

        public DeveloperPayload() {

        }

        public DeveloperPayload(String id, String handle) {
            this.id = id;
            this.handle = handle;
        }
    }
}