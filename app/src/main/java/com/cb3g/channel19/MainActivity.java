package com.cb3g.channel19;

import static android.os.SystemClock.sleep;
import static com.cb3g.channel19.RadioService.databaseReference;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.MainLayoutBinding;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

public class MainActivity extends FragmentActivity implements MI, View.OnClickListener, View.OnLongClickListener, PurchasesUpdatedListener, ValueEventListener {
    public MainLayoutBinding binding;
    private final GlideImageLoader glide = new GlideImageLoader(this);
    public final String SILENCE = "silence";
    public final String UNSILENCE = "unsilence";
    boolean isBound = false;
    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private RadioService RS;
    private ServiceConnection SC;
    private BillingUtils billingUtils;
    private boolean delay = true, longPressed = false, dark = false, overrideUp = false, overrideDown = false;
    private UserList userFragment;
    private Transmitter transmitFragment;
    private SharedPreferences settings;
    private Timer timer;
    private TimerTask blackTask;
    private int tutorial_count = 0;
    private Snackbar snackbar;
    private FusedLocationProviderClient mFusedLocationClient;
    private final LocationCallback locationCallback = new locationCallback();
    private DeveloperPayload silencePayload = null;
    private DeveloperPayload unSilencePayload = null;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case "advertise" -> {
                    if (!RadioService.ghostUsers.contains(RadioService.operator.getUser_id()))
                        showRewardAd();
                }
                case "review" -> {
                    ReviewManager manager = ReviewManagerFactory.create(MainActivity.this);
                    Task<ReviewInfo> request = manager.requestReviewFlow();
                    request.addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            ReviewInfo reviewInfo = task.getResult();
                            Task<Void> flow = manager.launchReviewFlow(MainActivity.this, reviewInfo);
                            flow.addOnCompleteListener(completeTask -> {
                            });
                        }
                    });
                }
                case "nineteenGifChosen" -> {
                    ImagePicker imagePicker = (ImagePicker) fragmentManager.findFragmentByTag("imagePicker");
                    if (imagePicker != null)
                        imagePicker.setPhoto(RadioService.gson.fromJson(intent.getStringExtra("data"), Gif.class), false);
                }
                case "show_result" -> {
                    if (!isFinishing())
                        showResult(intent.getStringExtra("title"), intent.getStringExtra("content"));
                }
                case "exitChannelNineTeen" -> finish();
                case "nineteenLockButtons" -> lockOthers(intent.getBooleanExtra("data", false));
                case "nineteenAddCaption" -> {
                    if (!isFinishing()) {
                        Bundle data = new Bundle();
                        data.putString("data", intent.getStringExtra("data"));
                        Caption cd = new Caption();
                        cd.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                        cd.setArguments(data);
                        cd.show(fragmentManager, "cd");
                    }
                }
                case "nineteenUpdateCaption" -> {
                    SendPhoto sendF = (SendPhoto) fragmentManager.findFragmentByTag("sendF");
                    if (sendF != null) sendF.updateCaption(intent.getStringExtra("data"));
                }
                case "nineteenSetPauseProgress" -> {
                    int[] set = intent.getIntArrayExtra("data");
                    assert set != null;
                    binding.maTimer.clearAnimation();
                    binding.maTimer.setMax(set[0]);
                    binding.maTimer.setProgress(set[0] - set[1]);
                }
                case "switchToPause" -> {
                    if (transmitFragment.isAdded() && RS != null)
                        transmitFragment.updateQueue(RS.getQueue(), RadioService.paused);
                    binding.maPauseButton.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.pause_set));
                }
                case "switchToPlay" -> {
                    if (transmitFragment.isAdded() && RS != null)
                        transmitFragment.updateQueue(RS.getQueue(), RadioService.paused);
                    binding.maPauseButton.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.resume_set));
                }
                case "bird" -> {
                    if (transmitFragment.isAdded()) if (RadioService.recording) {
                        transmitFragment.stopRecorder(false);
                        if (RS != null) RS.keyUpWasInterupted(intent.getStringExtra("userId"));
                    }
                    Toaster.flipDaBird(MainActivity.this);
                }
                case "nineteenToast" -> {
                    if (transmitFragment.isAdded()) if (RadioService.recording) return;
                    Toaster.toastlow(MainActivity.this, intent.getStringExtra("data"));
                }
                case "toasting" -> {
                    if (transmitFragment.isAdded()) if (RadioService.recording) return;
                    Toaster.labelTwo(MainActivity.this, intent.getStringExtra("data"));
                }
                case "nineteenAlert" -> {
                    if (transmitFragment.isAdded()) if (RadioService.recording) return;
                    Toaster.online(MainActivity.this, intent.getStringExtra("data"), intent.getStringExtra("profileLink"));
                }
                case "setMute" -> setMuteFromOutside(intent.getBooleanExtra("data", false));
                case "recordFromMain" -> recordFromMain();
                case "tooth" -> setTooth(intent.getBooleanExtra("data", false));
                case "nineteenAllow" ->
                        ActivityCompat.requestPermissions(MainActivity.this, Utils.getAudioPermissions(), 1);
                case "nineteenCamera" ->
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 3);
                case "nineteenCheck" -> Toaster.checkMark(MainActivity.this);
                case "nineteenCross" -> Toaster.fail(MainActivity.this);
            }
        }
    };
    private ExecutorService executor;

    private final ActivityResultLauncher<String> massPhotoPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri != null) {
            MassPhoto massPhoto = (MassPhoto) fragmentManager.findFragmentByTag("mass");
            if (massPhoto == null) {
                massPhoto = new MassPhoto(uri.toString());
                massPhoto.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                massPhoto.show(fragmentManager, "mass");
            }
        }
    });

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        binding = MainLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        settings = getSharedPreferences("settings", MODE_PRIVATE);
        billingUtils = new BillingUtils(this, this);
        findById();
        RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(1000);
        rotate.setRepeatCount(1);
        rotate.setInterpolator(new LinearInterpolator());
        userFragment = new UserList(fragmentManager);
        transmitFragment = new Transmitter(fragmentManager);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.ma_bottom_frame, transmitFragment, "tf");
        transaction.commit();
        MobileAds.initialize(this, initializationStatus -> {
        });
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Notifications");
                builder.setMessage("In order for Channel 19 to properly operate in the background, a persistant notification is needed");
                builder.setPositiveButton("Sure!", (dialogInterface, i) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissions(List.of(Manifest.permission.POST_NOTIFICATIONS).toArray(new String[0]), 0);
                    }
                });
                builder.setNegativeButton("No Thanks", (dialogInterface, i) -> Utils.vibrate(binding.blackOut));
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(List.of(Manifest.permission.POST_NOTIFICATIONS).toArray(new String[0]), 0);
                }
            }
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (RadioService.recording) return;
                if (dark) {
                    darkChange(false);
                    return;
                }
                if (longPressed) {
                    longPressed = false;
                    sendBroadcast(new Intent("exitChannelNineTeen").setPackage("com.cb3g.channel19"));
                    finish();
                } else {
                    Utils.vibrate(binding.maMapButton);
                    longPressed = true;
                    executor.execute(() -> {
                        sleep(600);
                        longPressed = false;
                        //showSnack(new Snack("double-tap back button to exit", Snackbar.LENGTH_SHORT));
                    });
                }
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
                    binding.maPauseButton.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.resume_set));
                else {
                    binding.maPauseButton.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.pause_set));
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                updateLocationDisplay(RS.returnLocation());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        isBound = bindService(new Intent(this, RadioService.class), SC, BIND_IMPORTANT);
        ContextCompat.registerReceiver(this, receiver, returnFilter(), ContextCompat.RECEIVER_NOT_EXPORTED);
        delay = true;
        if (settings.getBoolean("exiting", false)) {
            startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finishAffinity();
        }
        binding.maSettingsButton.setEnabled(true);
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
        binding.maSettingsButton.setEnabled(false);
        ExecutorUtils.shutdown(executor);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (blackTask != null) blackTask.cancel();
        if (timer != null) timer.purge();
        blackTask = null;
        timer = null;
        lockOthers(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (RadioService.operator == null) {
            finish();
            return;
        }
        timer = new Timer();
        int black = settings.getInt("black", 10);
        if (black != 0) {
            black = (black * 3500) + 10000;
            blackTask = new TimerTask() {
                @Override
                public void run() {
                    if (dark || (transmitFragment.isAdded() && RadioService.recording)) return;
                    if (!delay && RadioService.operator.getChannel() != null) {
                        runOnUiThread(() -> darkChange(true));
                    }
                    delay = false;
                }
            };
            timer.schedule(blackTask, black, black);
        }
        overrideUp = settings.getBoolean("overideup", false);
        overrideDown = settings.getBoolean("overidedown", false);
        tutorial_count = settings.getInt("tutorial", 0);
        if (tutorial_count == 5) finishTutorial(tutorial_count);
        sendBroadcast(new Intent("checkForMessages"));
        if (settings.getBoolean("custom", false)) {
            changeBackground(settings.getString("background", "default"));
        } else changeBackground(settings.getString("main_backdrop", ""));
        if (RadioService.operator.getChannel() != null) {
            binding.maChannelsButton.setText(RadioService.operator.getChannel().getChannel_name());
        } else {
            binding.maChannelsButton.setText(this.getString(R.string.select_channel));
            if (tutorial_count > 5) {
                selectChannel(false);
            }
        }
        if (settings.getBoolean("flagDue", false)) {
            displayLongFlag(settings.getString("flagSenderId", "Unknown"), settings.getString("flagSenderHandle", "Unknown"));
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            if (purchases != null) {
                for (Purchase purchase : purchases) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        billingUtils.handlePurchase(purchase, (billingResult1, s) -> {
                            switch (purchase.getProducts().get(0)) {
                                case SILENCE -> {
                                    finishSilence(silencePayload);
                                    silencePayload = null;
                                }
                                case UNSILENCE -> {
                                    finishUnsilence(unSilencePayload);
                                    unSilencePayload = null;
                                }
                            }
                        });
                    }
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
                LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 120000).setWaitForAccurateLocation(false).setMinUpdateIntervalMillis(60000).setMaxUpdateDelayMillis(600000).build();
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
        if (RS != null) RS.silence(developerPayload.id, developerPayload.handle);
    }

    private void finishUnsilence(DeveloperPayload developerPayload) {
        databaseReference.child("silenced").child(developerPayload.id).removeValue();
        showSnack(new Snack(developerPayload.handle + " has been unsilenced"));
        sendBroadcast(new Intent("register"));
        if (RS != null) RS.unsilence(developerPayload.id, developerPayload.handle);
    }

    @Override
    public void silence(User user) {
        if (RadioService.operator.getAdmin())
            finishSilence(new DeveloperPayload(user.getUser_id(), user.getRadio_hanlde()));
        else {
            if (billingUtils.isConnected()) {
                billingUtils.queryProductDetails(SILENCE, (billingResult, list) -> {
                    silencePayload = new DeveloperPayload(user.getUser_id(), user.getRadio_hanlde());
                    if (!list.isEmpty())
                        billingUtils.purchaseProduct(MainActivity.this, list.get(0));
                });
            }
        }
    }

    @Override
    public void unsilence(User user) {
        if (RadioService.operator.getAdmin())
            finishUnsilence(new DeveloperPayload(user.getUser_id(), user.getRadio_hanlde()));
        else {
            if (billingUtils.isConnected()) {
                billingUtils.queryProductDetails(UNSILENCE, (billingResult, list) -> {
                    unSilencePayload = new DeveloperPayload(user.getUser_id(), user.getRadio_hanlde());
                    if (!list.isEmpty())
                        billingUtils.purchaseProduct(MainActivity.this, list.get(0));
                });
            }
        }
    }

    @Override
    public void requestBluetoothPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, Utils.getBluetoothPermissions(), 1);
    }

    @Override
    public int returnUserVolume(String id) {
        if (RS != null) return RS.returnUserVolume(id);
        else return 85;
    }

    @Override
    public User returnTalkerEntry() {
        if (RS != null) return RS.returnTalkerEntry();
        return null;
    }

    @Override
    public void longFlagUser(User user) {
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
        binding.maChannelsButton.setText(channel.getChannel_name());
        delay = true;
        resetAnimation();
        if (!RadioService.operator.getAdmin()) {
            sendBroadcast(new Intent("nineteenEmptyPlayer"));
            sendBroadcast(new Intent("nineteenStaticSound"));
            if (transmitFragment.isAdded()) {
                transmitFragment.updateDisplay(new ProfileDisplay(), 0);
                transmitFragment.updateQueue(0, RadioService.paused);
            }
        }
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
        if (dark) snackbar = Snackbar.make(binding.blackOut, snack.getMessage(), snack.getLength());
        else
            snackbar = Snackbar.make(findViewById(R.id.ma_bottom_frame), snack.getMessage(), snack.getLength());
        View view = snackbar.getView();
        TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(ContextCompat.getColor(this, R.color.main_white));
        if (dark) view.setBackgroundColor(ContextCompat.getColor(this, R.color.main_black));
        else view.setBackgroundColor(ContextCompat.getColor(this, R.color.main_black_transparent));
        if (snack.getLength() == Snackbar.LENGTH_INDEFINITE) {
            RadioService.occupied.set(false);
            snackbar.setActionTextColor(Color.WHITE);
            snackbar.setAction("10 4", v -> {
                Utils.vibrate(v);
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
                if (!RadioService.snacks.isEmpty()) RadioService.snacks.remove(0);
                RadioService.occupied.set(false);
                if (RS != null) RS.checkForMessages();
            }
        });
        snackbar.show();
    }

    @Override
    public void changeBackground(String link) {
        glide.load(binding.backdrop, link);
    }

    @Override
    public ProfileDisplay getDisplayedText() {
        if (RS != null) return RS.getlatest();
        return new ProfileDisplay();
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
    public void updateDisplay(ProfileDisplay display, final int count, final int duration, final boolean paused, final boolean poor, final long stamp) {
        runOnUiThread(() -> {
            if (transmitFragment.isAdded()) transmitFragment.updateDisplay(display, stamp);
            if (display.getHandle().equals("Welcome...")) return;
            updateDarkDisplay(display, stamp);
            updateQueue(count, paused, poor);
            if (duration >= 0) animateMax(duration);
        });
    }

    @Override
    public void updateQueue(int count, boolean paused, boolean poor) {
        if (transmitFragment.isAdded()) transmitFragment.updateQueue(count, paused);
        if (count == 0 || paused) {
            if (!binding.maTimer.isIndeterminate()) binding.maTimer.setIndeterminate(true);
        } else {
            if (binding.maTimer.isIndeterminate()) binding.maTimer.setIndeterminate(false);
        }
        if (count > 0) binding.blackQueueTv.setText(String.valueOf(count));
        else binding.blackQueueTv.setText(" ");
        adjustColors(poor);
    }

    @Override
    public void adjustColors(boolean poor) {
        runOnUiThread(() -> {
            if (poor) {
                binding.blackQueueTv.setTextColor(Color.RED);
                binding.blackTitleTv.setTextColor(Color.RED);
                binding.blackDurationTv.setTextColor(Color.RED);
                if (dark)
                    binding.maTimer.setProgressDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.red_bar));
            } else {
                binding.blackQueueTv.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.light_blue));
                binding.blackTitleTv.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.light_blue));
                binding.blackDurationTv.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.light_blue));
                if (dark)
                    binding.maTimer.setProgressDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.blue_bar));
            }
            if (!dark)
                binding.maTimer.setProgressDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.white_bar));
            if (binding.maTimer.isIndeterminate()) {
                if (!dark)
                    binding.maTimer.getIndeterminateDrawable().setColorFilter(Utils.colorFilter(Color.WHITE));
                else {
                    if (poor)
                        binding.maTimer.getIndeterminateDrawable().setColorFilter(Utils.colorFilter(Color.RED));
                    else
                        binding.maTimer.getIndeterminateDrawable().setColorFilter(Utils.colorFilter(ContextCompat.getColor(MainActivity.this, R.color.light_blue)));
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
    public long queStamp(int position) {
        if (RS != null) return RS.queStamp(position);
        else return 0;
    }

    private void animateMax(int set) {
        if (set != 0) {
            binding.maTimer.setMax(set);
            binding.maTimer.setProgress(set);
            ProgressAnimation anim = new ProgressAnimation(binding.maTimer, set);
            anim.setDuration(set + 300);
            binding.maTimer.startAnimation(anim);
        } else {
            resetAnimation();
        }
    }

    @Override
    public void updateLocationDisplay(String location) {
        if (location.isEmpty()) {
            binding.maLocatoinTv.setText(R.string.location_unknown);
            //binding.blackLocationTv.setVisibility(View.INVISIBLE);
        } else {
            binding.maLocatoinTv.setText(location);
        }
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
    public void banUser(String id) {
        if (RS != null) RS.bannUser(id);
    }

    @Override
    public void recordChange(boolean recording) {
        Log.i("focus", "recordChange()");
        if (RS != null) {
            RS.recording(recording);
            if (recording) resetAnimation();
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
                binding.maBlurrIv.setVisibility(View.VISIBLE);
                binding.maCancelButton.setVisibility(View.VISIBLE);
                binding.maBlurrIv.startAnimation(fadeIn);
                binding.maCancelButton.startAnimation(fadeIn);
            } else {
                binding.maBlurrIv.clearAnimation();
                binding.maCancelButton.clearAnimation();
                binding.maBlurrIv.setVisibility(View.GONE);
                binding.maCancelButton.setVisibility(View.GONE);
            }
            lockOthers(recording);
        }
    }

    private void darkChange(Boolean show) {
        if (tutorial_count < 6) return;
        dark = show;
        delay = true;
        if (show) {
            binding.blackOut.setVisibility(View.VISIBLE);
            binding.blackOut.setAlpha(0);
            binding.blackOut.animate().alpha(1.0f).setDuration(500);
            if (RS != null) updateDarkDisplay(RS.getlatest(), 0);
        } else {
            binding.blackOut.setVisibility(View.GONE);
        }
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
    public void pauseOrPlay(User user) {
        if (RS != null) RS.pauseOrplay(user);
    }

    @Override
    public void flagThisUser(User user) {
        if (RS != null) RS.flagUser(user);
    }

    @Override
    public void kickUser(User user) {
        if (RS != null) RS.kickUser(user);
    }

    @Override
    public void saluteThisUser(User user) {
        if (RS != null) RS.saluteUser(user);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU -> {
                darkChange(!dark);
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_UP -> {
                if (overrideUp) {
                    event.startTracking();
                    return true;
                } else return super.onKeyDown(keyCode, event);
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (overrideDown) {
                    event.startTracking();
                    return true;
                } else return super.onKeyDown(keyCode, event);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && overrideUp) {
            sendBroadcast(new Intent("nineteenPlayPause"));
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && overrideDown) {
            sendBroadcast(new Intent("purgeNineTeen"));
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (((event.getFlags() & KeyEvent.FLAG_CANCELED_LONG_PRESS) == 0)) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (overrideUp) {
                    recordFromMain();
                    return true;
                } else return super.onKeyUp(keyCode, event);
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (overrideDown) {
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
            if (transmitFragment.isAdded()) transmitFragment.mainRecord();
            else flip();
        }
    }

    private void setTooth(boolean show) {
        if (transmitFragment.isAdded()) transmitFragment.setTooth(show);
    }

    private void setMuteFromOutside(boolean mute) {
        if (transmitFragment.isAdded()) transmitFragment.setMute(mute);
    }

    @Override
    public void onClick(View v) {
        delay = true;
        int id = v.getId();
        if (id == R.id.ma_cancel_button) {
            Utils.vibrate(v);
            if (transmitFragment.isAdded()) transmitFragment.stopRecorder(false);
        } else if (id == R.id.ma_user_list_button) {
            Utils.vibrate(v);
            flip();
        } else if (id == R.id.black_profile_picture_iv_small) {
            User user = returnTalkerEntry();
            if (user != null) {
                Utils.vibrate(v);
                sendBroadcast(new Intent("nineteenBoxSound"));
                streamFile(user.getProfileLink());
            }
        } else if (id == R.id.black_queue_tv) {
            if (RadioService.operator.getChannel() != null) {
                if (RS != null) {
                    if (RadioService.paused)
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                Utils.vibrate(v);
                sendBroadcast(new Intent("nineteenPlayPause"));
            }
        } else if (id == R.id.ma_chat_history_button) {
            if (RadioService.operator.getHinderPhotos() || RadioService.operator.getHinderTexts())
                return;
            Utils.vibrate(v);
            if (RadioService.operator.getSilenced()) {
                showSnack(new Snack("You are currently silenced", Snackbar.LENGTH_SHORT));
                return;
            }
            sendBroadcast(new Intent("nineteenClickSound"));
            Utils.vibrate(v);
            display_message_history();
        } else if (id == R.id.ma_mass_photo_button) {
            if (RadioService.operator.getChannel() == null || RadioService.operator.getHinderPhotos())
                return;
            Utils.vibrate(v);
            if (RadioService.operator.getSilenced()) {
                showSnack(new Snack("You are currently silenced", Snackbar.LENGTH_SHORT));
                return;
            }
            sendBroadcast(new Intent("nineteenClickSound"));
            if (Utils.permissionsAccepted(this, Utils.getStoragePermissions())) {
                massPhotoPicker.launch("image/*");
            } else {
                Utils.requestPermission(this, Utils.getStoragePermissions(), 0);
            }
        } else if (id == R.id.ma_channels_button) {
            Utils.vibrate(v);
            sendBroadcast(new Intent("nineteenClickSound"));
            selectChannel(true);
        } else if (id == R.id.ma_map_button) {
            if (RadioService.operator.getChannel() == null) return;
            Utils.vibrate(v);
            sendBroadcast(new Intent("nineteenClickSound"));
            this.startActivity(new Intent(MainActivity.this, Locations.class));
        } else if (id == R.id.ma_reservoir_button) {
            if (RadioService.operator.getChannel() != null) {
                sendBroadcast(new Intent("nineteenClickSound"));
                Utils.vibrate(v);
                startActivity(new Intent(MainActivity.this, ReservoirActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        } else if (id == R.id.ma_pause_button) {
            if (RadioService.operator.getChannel() == null) return;
            if (RS != null) {
                if (RadioService.paused)
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            sendBroadcast(new Intent("nineteenPlayPause"));
        } else if (id == R.id.black_skip_button) {
            if (RS != null) {
                if (RS.getQueue() != 0) {
                    sendBroadcast(new Intent("nineteenSkip"));
                    Utils.vibrate(v);
                } else darkChange(false);
            }
        } else if (id == R.id.blackOut) {
            darkChange(false);
        } else if (id == R.id.black_profile_picture_iv) {
            darkChange(false);
        } else if (id == R.id.ma_skip_button) {
            Utils.vibrate(v);
            if (RadioService.operator.getChannel() == null) return;
            sendBroadcast(new Intent("nineteenSkip"));
        } else if (id == R.id.ma_settings_button) {
            Utils.vibrate(v);
            sendBroadcast(new Intent("nineteenTabSound"));
            startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
    }

    @Override
    public boolean onLongClick(View v) {
        delay = true;
        int id = v.getId();
        if (id == R.id.ma_mass_photo_button) {
            Utils.vibrate(v);
            sendBroadcast(new Intent("nineteenClickSound"));
            MassPast massPast = new MassPast();
            massPast.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            massPast.show(fragmentManager, "massPast");
        } else if (id == R.id.ma_settings_button) {
            Utils.vibrate(v);
            sendBroadcast(new Intent("nineteenStaticSound"));
            showSnack(new Snack("Rewind Five", Snackbar.LENGTH_SHORT));
            RotateAnimation counterClockwise = new RotateAnimation(360, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            counterClockwise.setDuration(2000);
            binding.maSettingsButton.startAnimation(counterClockwise);
            if (RS != null) RS.rewind();
        } else if (id == R.id.ma_pause_button) {
            if (!isFinishing()) {
                sendBroadcast(new Intent("nineteenClickSound"));
                Utils.vibrate(v);
                QueueDialog qd = (QueueDialog) fragmentManager.findFragmentByTag("qd");
                if (qd == null) {
                    qd = new QueueDialog();
                    qd.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                    qd.show(fragmentManager, "qd");
                }
            }
        } else if (id == R.id.black_skip_button) {
            if (RS != null) {
                if (RS.getQueue() != 0) {
                    sendBroadcast(new Intent("purgeNineTeen"));
                    Utils.vibrate(v);
                } else darkChange(false);
            }
        } else if (id == R.id.ma_user_list_button) {
            Utils.vibrate(v);
            darkChange(true);
        } else if (id == R.id.ma_skip_button) {
            if (RS != null) if (RS.getQueue() != 0) {
                Utils.vibrate(v);
                sendBroadcast(new Intent("purgeNineTeen"));
            }
        } else if (id == R.id.black_profile_picture_iv_small) {
            User user = returnTalkerEntry();
            if (user != null) {
                sendBroadcast(new Intent("nineteenClickSound"));
                Utils.vibrate(v);
                UserListOptionsNew cdf = (UserListOptionsNew) fragmentManager.findFragmentByTag("options");
                if (cdf == null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("user", new Gson().toJson(user));
                    cdf = new UserListOptionsNew(fragmentManager, user);
                    cdf.setArguments(bundle);
                    cdf.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                    cdf.show(fragmentManager, "options");
                }
            }
            return true;
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
    public void displayChat(User user, boolean sound, boolean launch) {
        if (isFinishing()) return;
        Chat chat_dialog = (Chat) fragmentManager.findFragmentByTag("chatD");
        if (chat_dialog == null) {
            Bundle bundle = new Bundle();
            bundle.putString("data", RadioService.gson.toJson(user));
            bundle.putBoolean("launch", launch);
            chat_dialog = new Chat(fragmentManager, user);
            chat_dialog.setArguments(bundle);
            chat_dialog.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            chat_dialog.show(fragmentManager, "chatD");
        } else chat_dialog.gather_history(sound, true);
    }

    @Override
    public void createPm(User user) {
        if (isFinishing()) return;
        SendMessage messageFragment = (SendMessage) fragmentManager.findFragmentByTag("messageFragment");
        if (messageFragment == null) {
            Bundle bundle = new Bundle();
            bundle.putString("userId", user.getUser_id());
            bundle.putString("handle", user.getRadio_hanlde());
            bundle.putString("rank", user.getRank());
            bundle.putString("profileLink", user.getProfileLink());
            messageFragment = new SendMessage();
            messageFragment.setArguments(bundle);
            messageFragment.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            messageFragment.show(fragmentManager, "messageFragment");
        }
    }

    @Override
    public void displayPm(String... data) {
        if (isFinishing()) return;
        ShowMessage showMessageFragment = (ShowMessage) fragmentManager.findFragmentByTag("showMessageFragment");
        if (showMessageFragment == null) {
            Bundle bundle = new Bundle();
            bundle.putStringArray("data", data);
            showMessageFragment = new ShowMessage();
            showMessageFragment.setArguments(bundle);
            showMessageFragment.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            showMessageFragment.show(fragmentManager, "showMessageFragment");
        }
    }

    @Override
    public void displayPhoto(Photo photo) {
        ShowPhoto showPhoto = new ShowPhoto(photo);
        showPhoto.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
        showPhoto.show(fragmentManager, "showPhoto");
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
        if (transmitFragment.isAdded()) if (RadioService.recording) {
            transmitFragment.stopRecorder(false);
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
        if (transmitFragment.isAdded()) transmitFragment.stopRecorder(pass);
    }

    @Override
    public void startTransmit() {
        if (transmitFragment.isAdded()) transmitFragment.transmitStart();
    }

    @Override
    public void showRewardAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-4635898093945616/6205382793", adRequest, new RewardedAdLoadCallback() {
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
                rewardedAd.show(MainActivity.this, rewardItem -> {
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
        ShowcaseView showcaseView = new ShowcaseView.Builder(this).setTarget(new ViewTarget(R.id.ma_skip_button, this)).setContentTitle("SKIP BUTTON").setContentText("Skips the current transmission which consequently lowers the queue").setStyle(R.style.CustomShowcaseTheme).blockAllTouches().build();
        showcaseView.setButtonPosition(tps);
        showcaseView.show();
        showcaseView.overrideButtonClick(v -> {
            Utils.vibrate(v);
            tutorial_count++;
            settings.edit().putInt("tutorial", tutorial_count).apply();
            switch (tutorial_count) {
                case 5 -> {
                    showcaseView.setButtonPosition(lps);
                    showcaseView.setTarget(new ViewTarget(R.id.ma_channels_button, MainActivity.this));
                    showcaseView.setContentTitle("Current Channel");
                    showcaseView.setContentText("Unlimited public and private channels");
                }
                case 6 -> {
                    showcaseView.hide();
                    selectChannel(false);
                    Toaster.toastlow(MainActivity.this, "Join a channel or create a new one");
                }
            }
        });
    }

    @Override
    public void resumeAnimation(int[] data) {
        if (data[0] <= 0 || data[1] <= 0) resetAnimation();
        else {
            int set = data[0] - data[1];
            if (set + 300 > 0) {
                binding.maTimer.setMax(data[0]);
                binding.maTimer.setProgress(set);
                ProgressAnimation anim = new ProgressAnimation(binding.maTimer, set);
                anim.setDuration(set + 300);
                binding.maTimer.startAnimation(anim);
            }
        }
    }

    private void resetAnimation() {
        binding.maTimer.clearAnimation();
        binding.maTimer.setProgress(0);
    }

    @Override
    public void lockOthers(boolean lock) {
        if (binding.maTimer.isIndeterminate() && !lock) binding.maTimer.setIndeterminate(false);
        binding.maUserListButton.setEnabled(!lock);
        binding.maReservoirButton.setEnabled(!lock);
        binding.maSettingsButton.setEnabled(!lock);
        binding.maSkipButton.setEnabled(!lock);
        binding.maPauseButton.setEnabled(!lock);
        binding.maMapButton.setEnabled(!lock);
        binding.maMassPhotoButton.setEnabled(!lock);
        binding.maChatHistoryButton.setEnabled(!lock);
        binding.maChannelsButton.setEnabled(!lock);
    }


    private void updateDarkDisplay(@NonNull final ProfileDisplay display, final long stamp) {
        if (display.getHandle().contains("Online") || display.getHandle().contains("Dead"))
            binding.blackHandleTv.setText(" ");
        else binding.blackHandleTv.setText(display.getHandle());
        binding.blackCarrierTv.setText(display.getCarrier());
        if (stamp == 0) binding.blackTitleTv.setText("");
        else binding.blackTitleTv.setText(Utils.formatDiff(Utils.timeDifferance(stamp), false));
        if (display.getDuration() == 0) binding.blackDurationTv.setText("");
        else binding.blackDurationTv.setText(display.getDuration() + "s");
        glide.load(binding.blackRankIv, Utils.parseRankUrl(display.getRank()));
        if (display.getProfileLink().equals("none") || !dark) {
            binding.blackProfilePictureIv.setImageBitmap(null);
            binding.blackProfilePictureIvSmall.setImageBitmap(null);
        } else {
            glide.load(binding.blackProfilePictureIv, display.getProfileLink());
            glide.load(binding.blackProfilePictureIvSmall, display.getProfileLink(), RadioService.profileOptions);
        }
    }

    @Override
    public void display_message_history() {
        if (isFinishing()) return;
        sendBroadcast(new Intent("nineteenClickSound"));
        MessageHistory history = (MessageHistory) fragmentManager.findFragmentByTag("history");
        if (history == null) {
            history = new MessageHistory();
            history.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            history.show(fragmentManager, "history");
        }
    }

    @Override
    public void updateUserList(List<UserListEntry> userList) {
        if (userFragment.isAdded()) userFragment.update_users_list(userList);
    }

    public void flip() {
        if (RadioService.operator.getChannel() == null) return;
        sendBroadcast(new Intent("nineteenClickSound"));
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (transmitFragment.isAdded()) transaction.replace(R.id.ma_bottom_frame, userFragment);
        else transaction.replace(R.id.ma_bottom_frame, transmitFragment);
        transaction.commit();
        delay = true;
    }

    private void findById() {
        //OnClick
        binding.maChatHistoryButton.setOnClickListener(this);
        binding.maMassPhotoButton.setOnClickListener(this);
        binding.maChannelsButton.setOnClickListener(this);
        binding.maReservoirButton.setOnClickListener(this);
        binding.blackOut.setOnClickListener(this);
        binding.maSettingsButton.setOnClickListener(this);
        binding.blackSkipButton.setOnClickListener(this);
        binding.blackProfilePictureIvSmall.setOnClickListener(this);
        binding.blackSkipButton.setOnClickListener(this);
        binding.maSkipButton.setOnClickListener(this);
        binding.maMapButton.setOnClickListener(this);
        binding.maPauseButton.setOnClickListener(this);
        binding.blackQueueTv.setOnClickListener(this);
        binding.blackProfilePictureIv.setOnClickListener(this);
        binding.maCancelButton.setOnClickListener(this);
        binding.maUserListButton.setOnClickListener(this);

        //LongPress
        binding.maUserListButton.setOnLongClickListener(this);
        binding.blackSkipButton.setOnLongClickListener(this);
        binding.blackSkipButton.setOnLongClickListener(this);
        binding.blackProfilePictureIvSmall.setOnLongClickListener(this);
        binding.maSkipButton.setOnLongClickListener(this);
        binding.maMassPhotoButton.setOnLongClickListener(this);
        binding.maPauseButton.setOnLongClickListener(this);
        if (RadioService.operator.getSubscribed())
            binding.maSettingsButton.setOnLongClickListener(this);
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {

    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {
        Logger.INSTANCE.i("onCancelled()");
    }

    class locationCallback extends LocationCallback {

        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            for (Location location : locationResult.getLocations()) {
                if (RS != null) RS.locationUpdated(location);
                if (RadioService.operator.getSharing())
                    databaseReference.child("locations").child(RadioService.operator.getUser_id()).setValue(new Coordinates(RadioService.operator.getUser_id(), RadioService.operator.getHandle(), RadioService.operator.getProfileLink(), location.getLatitude(), location.getLongitude(), location.getBearing(), location.getSpeed(), location.getAltitude()));

            }
        }
    }

    static class DeveloperPayload {
        String id;
        String handle;

        public DeveloperPayload(String id, String handle) {
            this.id = id;
            this.handle = handle;
        }
    }
}