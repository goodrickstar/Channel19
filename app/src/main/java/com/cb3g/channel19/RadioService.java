package com.cb3g.channel19;

import android.Manifest;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.Observable;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.android.multidex.myapplication.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.vdurmont.emoji.EmojiParser;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.Instant;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

@SuppressWarnings("SpellCheckingInspection")
public class RadioService extends Service implements ValueEventListener, AudioManager.OnAudioFocusChangeListener, ChildEventListener {
    static final OkHttpClient client = new OkHttpClient();

    final GlideImageLoader loader = new GlideImageLoader(this);
    private final List<String> nearby = new ArrayList<>();
    static final String SITE_URL = "http://23.111.159.2/~channel1/";
    static RequestOptions profileOptions = new RequestOptions().circleCrop();
    static RequestOptions largeProfileOptions = new RequestOptions().centerInside();
    static boolean phoneIdle = true, paused = false, recording = false, bluetoothEnabled = false, mute = false;
    static ObservableBoolean occupied = new ObservableBoolean(false);
    static ObservableField<String> chat = new ObservableField<>("0");
    static String language;
    static DatabaseReference databaseReference;
    static FirebaseStorage storage;
    static Operator operator = new Operator();
    static List<String> ghostUsers = new ArrayList<>();
    static List<String> pausedUsers = new ArrayList<>();
    static List<String> onCallUsers = new ArrayList<>();
    static List<String> silencedUsers = new ArrayList<>();
    static List<String> autoSkip = new ArrayList<>();
    static List<UserVolume> volumes = new ArrayList<>();
    static List<Coordinates> coordinates = new ArrayList<>();
    static Gson gson = new Gson();
    static List<User> users = new ArrayList<>();
    static List<UserListEntry> userList = new ArrayList<>();
    static List<Block> blockedIDs = new ArrayList<>();
    static List<Block> photoIDs = new ArrayList<>();
    static List<Snack> snacks = new ArrayList<>();
    static List<Block> textIDs = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final IBinder mBinder = new LocalBinder();
    private final MediaPlayer player = new MediaPlayer();
    private final Uri KICK_URI = Uri.parse("android.resource://com.cb3g.channel19/" + R.raw.kicked);
    private final Uri WELCOME_URI = Uri.parse("android.resource://com.cb3g.channel19/" + R.raw.welcome);
    private final Map<String, String> STATE_MAP = new HashMap<>();
    private final List<Photo> photos = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();
    private LocationManager locManager;
    private SoundPool sp;
    private int glass, confirm, type, chain, click, clicktwo, newthree, clickthree, skip, purge, mic, talkie, pauseLimit, register, wrong, interrupt;
    private AudioManager audioManager;
    private boolean playing = false, bluetooth, poor = false;
    private List<Inbound> inbounds = new ArrayList<>();
    static List<String> salutedIds = new ArrayList<>();
    static List<String> flaggedIds = new ArrayList<>();
    private String saveDirectory, onlineStatus = "Online";
    private SharedPreferences widget, settings;
    private BluetoothAdapter bluetoothAdapter;
    private Locale locale;
    private Context context;
    private MI MI;
    private NotificationCompat.Builder mBuilder;
    private RemoteViews notifyview;
    private FirebaseStorage temporaryStorage;
    private ChildEventListener audioListener;
    private final OkUtil okUtil = new OkUtil();
    static AppOptionsObject appOptions = new AppOptionsObject();
    private long enterStamp = Instant.now().getEpochSecond();
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction() == null) return;
            switch (intent.getAction()) {
                case "interrupt":
                    if (MI != null) MI.stopRecorder(false);
                    break;
                case "locationUpdate":
                    final String locationString = intent.getStringExtra("data");
                    if (locationString != null) {
                        operator.setUserLocationString(locationString);
                        if (operator.getSharing()) uploadLocation(operator.getUserLocationString());
                        if (MI != null) MI.updateLocationDisplay(operator.getUserLocationString());
                    }
                    break;
                case "listUpdate":
                    final String flagData = intent.getStringExtra("data");
                    flaggedIds.clear();
                    flaggedIds = gson.fromJson(flagData, new TypeToken<List<String>>() {
                    }.getType());
                    flaggedIds = gson.fromJson(flagData, new TypeToken<FlagObject>() {
                    }.getType());
                    settings.edit().putString("flaggedIDs", flagData).apply();
                    break;
                case "longFlag":
                    String flagSenderId = intent.getStringExtra("userId");
                    String flagSenderHandle = intent.getStringExtra("handle");
                    Log.i("Animate", flagSenderId + " " + flagSenderHandle);
                    settings.edit().putBoolean("flagDue", true).putString("flagSenderId", flagSenderId).putString("flagSenderHandle", flagSenderHandle).apply();
                    if (MI != null) MI.displayLongFlag(flagSenderId, flagSenderHandle);
                    break;
                case "confirmInterrupt":
                    sp.play(interrupt, .1f, .1f, 1, 0, 1f);
                    break;
                case "wrong":
                    sp.play(wrong, .1f, .1f, 1, 0, 1f);
                    break;
                case "register":
                    sp.play(register, 1f, 1f, 1, 0, 1f);
                    break;
                case "ghost":
                    ghost();
                    break;
                case "checkForMessages":
                    checkForMessages();
                    break;
                case "fetch_users":
                    getUsersOnChannel();
                    break;
                case "pauseLimitChange":
                    pauseLimit = intent.getIntExtra("data", 200);
                    settings.edit().putInt("pauseLimit", pauseLimit).apply();
                    break;
                case "play":
                    if (recording || playing || paused || inbounds.isEmpty()) return;
                    play();
                    break;
                case "savePhotoToDisk":
                    try {
                        downloadImage(intent.getStringExtra("url"));
                    } catch (MalformedURLException e) {
                        Logger.INSTANCE.e("savePhotoToDisk", e.getMessage());
                    }
                    break;
                case "nineteenTransmit":
                    transmit(intent.getStringExtra("data"), intent.getBooleanExtra("talkback", false), intent.getLongExtra("duration", 0), intent.getLongExtra("stamp", 0));
                    break;
                case "nineteenScroll":
                    int count = inbounds.size() - intent.getIntExtra("data", 0);
                    if (!inbounds.isEmpty()) {
                        sp.play(purge, .1f, .1f, 1, 0, 1f);
                        int size = inbounds.size();
                        if (size <= count) emptyPlayer(false);
                        else {
                            if (count > 1) {
                                inbounds.subList(1, count).clear();
                            }
                            removeZeros();
                        }
                    }
                    break;
                case "nineteenPlayPause":
                    sp.play(clicktwo, .1f, .1f, 1, 0, 1f);
                    if (!paused) pause_playback();
                    else resumePlayback();
                    break;
                case "nineteenPlay":
                    if (paused) resumePlayback();
                    break;
                case "nineteenPause":
                    if (!paused) pause_playback();
                    break;
                case "nineteenSkip":
                    if (!recording) skip();
                    break;
                case "purgeNineTeen":
                    purge();
                    break;
                case "nineteenEmptyPlayer":
                    emptyPlayer(false);
                    break;
                case "nineteenClickSound":
                    sp.play(clicktwo, .1f, .1f, 1, 0, 1f);
                    break;
                case "nineteenMicSound":
                    sp.play(mic, .03f, .03f, 1, 0, 1f);
                    break;
                case "nineteenChatSound":
                    sp.play(type, .1f, .1f, 1, 0, 1f);
                    break;
                case "nineteenTabSound":
                    sp.play(click, .1f, .1f, 1, 0, 1f);
                    break;
                case "nineteenBoxSound":
                    sp.play(newthree, .1f, .1f, 1, 0, 1f);
                    break;
                case "nineteenStaticSound":
                    sp.play(talkie, .1f, .1f, 1, 0, 1f);
                    break;
                case "nineteenUpdateBlocks":
                    blockedIDs = returnBlockListObjectFromJson(intent.getStringExtra("blockedIDs"));
                    photoIDs = returnBlockListObjectFromJson(intent.getStringExtra("photoIDs"));
                    textIDs = returnBlockListObjectFromJson(intent.getStringExtra("textIDs"));
                    update_block_list(blockedIDs, "blockedIDs");
                    update_block_list(photoIDs, "photoIDs");
                    update_block_list(textIDs, "textIDs");
                    break;
                case "fetchInformation":
                    user_info_lookup(intent.getStringExtra("data"));
                    break;
                case "exitChannelNineTeen":
                    stopSelf();
                    break;
                case "muteChannelNineTeen":
                    sp.play(clickthree, .1f, .1f, 1, 0, 1f);
                    mute = !mute;
                    mute();
                    break;
                case "nineteenSendPM":
                    sendPrivate(intent.getStringExtra("text"), intent.getStringExtra("id"));
                    break;
                case "nineteenBluetoothSettingChange":
                    bluetooth = intent.getBooleanExtra("data", false);
                    settings.edit().putBoolean("bluetooth", bluetooth).apply();
                    bluetoothEnabled = headsetActive();
                    sendBroadcast(new Intent("tooth").setPackage("com.cb3g.channel19").putExtra("data", bluetoothEnabled));
                    break;
                case "removeAllOf":
                    removeAllOf(intent.getStringExtra("data"), false, operator.getPurgeLimit());
                    break;
                case "purgeUser":
                    removeAllOf(intent.getStringExtra("data"), false, 0);
                    break;
                case LocationManager.PROVIDERS_CHANGED_ACTION:
                    boolean gps = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    if (!gps) {
                        operator.setUserLocationString("");
                        if (MI != null) MI.updateLocationDisplay("");
                    }
                    if (MI != null) MI.startOrStopGPS(gps);
                    break;
                case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                    bluetoothEnabled = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED) == BluetoothHeadset.STATE_CONNECTED && bluetooth;
                    sendBroadcast(new Intent("tooth").setPackage("com.cb3g.channel19").putExtra("data", bluetoothEnabled));
                    if (MI != null) MI.stopRecorder(false);
                    break;
                case AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED:
                    switch (intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_DISCONNECTED)) {
                        case AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                            if (MI != null) {
                                MI.startTransmit();
                            }
                        }
                        case AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                            if (MI != null) MI.stopRecorder(false);
                        }
                    }
                    break;
            }
        }
    };
    private AudioFocusRequest micFocus, listenFocus;
    private ExecutorService executor;

    private boolean callStateListenerRegistered = false;

    private final ValueEventListener autoSkipEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            autoSkip.clear();
            for (DataSnapshot child : snapshot.getChildren()) {
                autoSkip.add(child.getKey());
            }
            userList = settleUserList(users);
            if (MI != null) MI.updateUserList(userList);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };

    private final ChildEventListener volumeChildListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            UserVolume child = dataSnapshot.getValue(UserVolume.class);
            volumes.add(child);
            assert child != null;
            if (isUpdatedVolumeUserZero(child.getId())) {
                float volume = scaleVolume(returnUserVolume(child.getId()));
                player.setVolume(volume, volume);
            }
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            UserVolume child = dataSnapshot.getValue(UserVolume.class);
            for (int i = 0; i < volumes.size(); i++) {
                assert child != null;
                if (volumes.get(i).getId().equals(child.getId()))
                    volumes.get(i).setVolume(child.getVolume());
            }
            assert child != null;
            if (isUpdatedVolumeUserZero(child.getId())) {
                float volume = scaleVolume(returnUserVolume(child.getId()));
                player.setVolume(volume, volume);
            }
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerCallStateListener();
        registerDefaultNetworkCallback();
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        executor = ExecutorUtils.newSingleThreadExecutor();
        temporaryStorage = FirebaseStorage.getInstance("gs://nineteen-temporary");
        widget = getSharedPreferences("widget", Context.MODE_PRIVATE);
        settings = getSharedPreferences("settings", MODE_PRIVATE);
        locale = Locale.getDefault();
        language = locale.getDisplayLanguage();
        context = getApplicationContext();
        storage = FirebaseStorage.getInstance();
        databaseReference = Utils.getDatabase().getReference();
        databaseReference.child("reservoir").keepSynced(true);
        operator.setKey(settings.getString("keychain", ""));
        operator.setUser_id(settings.getString("userId", "0"));
        operator.setHandle(settings.getString("handle", "default"));
        operator.setCarrier(settings.getString("carrier", ""));
        operator.setRank(settings.getString("rank", "f"));
        operator.setTown(settings.getString("town", ""));
        operator.setProfileLink(settings.getString("profileLink", SITE_URL + "drawables/default.png"));
        operator.setSubscribed(settings.getBoolean("active", false));
        operator.setInvisible(settings.getBoolean("invisible", false));
        operator.setLimit(settings.getInt("limit", 50));
        operator.setPurgeLimit(settings.getInt("purgeLimit", 20));
        operator.setNearbyLimit(settings.getInt("nearbyLimit", 50));
        operator.setAdmin(settings.getBoolean("admin", false));
        operator.setStamp(settings.getString("latest", ""));
        operator.setNewbie(settings.getInt("newbie", 0));
        operator.setCount(settings.getInt("count", 0));
        operator.setSalutes(settings.getInt("salutes", 0));
        operator.setSharing(settings.getBoolean("sharing", ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED));
        operator.setUserLocationString("");
        operator.setChannel(null);
        operator.getLocationEnabled().set(settings.getBoolean("locationEnabled", ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED));
        if (operator.getSubscribed()) blockLimit = 30;
        enableListeners();
        pauseLimit = settings.getInt("pauseLimit", 150);
        bluetooth = settings.getBoolean("bluetooth", true);
        onlineStatus = "Online";
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = manager.getAdapter();
        bluetoothEnabled = headsetActive();
        sendBroadcast(new Intent("tooth").setPackage("com.cb3g.channel19").putExtra("data", bluetoothEnabled));
        soundPool();
        saveDirectory = getCacheDir() + "/";
        STATE_MAP.put("Alabama", "AL");
        STATE_MAP.put("Alaska", "AK");
        STATE_MAP.put("Alberta", "AB");
        STATE_MAP.put("Arizona", "AZ");
        STATE_MAP.put("Arkansas", "AR");
        STATE_MAP.put("British Columbia", "BC");
        STATE_MAP.put("California", "CA");
        STATE_MAP.put("Colorado", "CO");
        STATE_MAP.put("Connecticut", "CT");
        STATE_MAP.put("Delaware", "DE");
        STATE_MAP.put("District Of Columbia", "DC");
        STATE_MAP.put("Florida", "FL");
        STATE_MAP.put("Georgia", "GA");
        STATE_MAP.put("Guam", "GU");
        STATE_MAP.put("Hawaii", "HI");
        STATE_MAP.put("Idaho", "ID");
        STATE_MAP.put("Illinois", "IL");
        STATE_MAP.put("Indiana", "IN");
        STATE_MAP.put("Iowa", "IA");
        STATE_MAP.put("Kansas", "KS");
        STATE_MAP.put("Kentucky", "KY");
        STATE_MAP.put("Louisiana", "LA");
        STATE_MAP.put("Maine", "ME");
        STATE_MAP.put("Manitoba", "MB");
        STATE_MAP.put("Maryland", "MD");
        STATE_MAP.put("Massachusetts", "MA");
        STATE_MAP.put("Michigan", "MI");
        STATE_MAP.put("Minnesota", "MN");
        STATE_MAP.put("Mississippi", "MS");
        STATE_MAP.put("Missouri", "MO");
        STATE_MAP.put("Montana", "MT");
        STATE_MAP.put("Nebraska", "NE");
        STATE_MAP.put("Nevada", "NV");
        STATE_MAP.put("New Brunswick", "NB");
        STATE_MAP.put("New Hampshire", "NH");
        STATE_MAP.put("New Jersey", "NJ");
        STATE_MAP.put("New Mexico", "NM");
        STATE_MAP.put("New York", "NY");
        STATE_MAP.put("Newfoundland", "NF");
        STATE_MAP.put("North Carolina", "NC");
        STATE_MAP.put("North Dakota", "ND");
        STATE_MAP.put("Northwest Territories", "NT");
        STATE_MAP.put("Nova Scotia", "NS");
        STATE_MAP.put("Nunavut", "NU");
        STATE_MAP.put("Ohio", "OH");
        STATE_MAP.put("Oklahoma", "OK");
        STATE_MAP.put("Ontario", "ON");
        STATE_MAP.put("Oregon", "OR");
        STATE_MAP.put("Pennsylvania", "PA");
        STATE_MAP.put("Prince Edward Island", "PE");
        STATE_MAP.put("Puerto Rico", "PR");
        STATE_MAP.put("Quebec", "QC");
        STATE_MAP.put("Rhode Island", "RI");
        STATE_MAP.put("Saskatchewan", "SK");
        STATE_MAP.put("South Carolina", "SC");
        STATE_MAP.put("South Dakota", "SD");
        STATE_MAP.put("Tennessee", "TN");
        STATE_MAP.put("Texas", "TX");
        STATE_MAP.put("Utah", "UT");
        STATE_MAP.put("Vermont", "VT");
        STATE_MAP.put("Virgin Islands", "VI");
        STATE_MAP.put("Virginia", "VA");
        STATE_MAP.put("Washington", "WA");
        STATE_MAP.put("West Virginia", "WV");
        STATE_MAP.put("Wisconsin", "WI");
        STATE_MAP.put("Wyoming", "WY");
        STATE_MAP.put("Yukon Territory", "YT");
        ContextCompat.registerReceiver(this, receiver, returnFilter(), ContextCompat.RECEIVER_NOT_EXPORTED);
        if (settings.getBoolean("welcomesound", true)) {
            playing = true;
            try {
                player.setDataSource(RadioService.this, WELCOME_URI);
                player.setOnPreparedListener(mp -> {
                    Inbound inbound = new Inbound();
                    inbound.setStamp(Instant.now().getEpochSecond());
                    inbound.setUser_id(operator.getUser_id());
                    inbounds.add(inbound);
                    updateDisplay();
                    mp.start();
                });
                player.setOnCompletionListener(mp -> {
                    mp.reset();
                    playing = false;
                    if (!inbounds.isEmpty()) inbounds.remove(0);
                    if (inbounds.isEmpty()) {
                        updateDisplay();
                    } else {
                        if (paused || recording) updateDisplay();
                        else sendBroadcast(new Intent("play").setPackage("com.cb3g.channel19"));
                    }
                });
                player.prepare();
            } catch (IOException e) {
                playing = false;
                LOG.e(String.valueOf(e));
            }
        }
        photoIDs = returnBlockListObjectFromJson(settings.getString("photoIDs", "[]"));
        textIDs = returnBlockListObjectFromJson(settings.getString("textIDs", "[]"));
        blockedIDs = returnBlockListObjectFromJson(settings.getString("blockedIDs", "[]"));
        salutedIds = returnStringListObjectFromJson(settings.getString("salutedIDs", "[]"));
        flaggedIds = returnStringListObjectFromJson(settings.getString("flaggedIDs", "[]"));
        getUsersOnChannel();
        mBuilder = new NotificationCompat.Builder(this, "19");
        notifyview = new RemoteViews("com.cb3g.channel19", R.layout.skip_notify);
        buildNotification();
        chat.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (Objects.equals(chat.get(), "0")) checkForMessages();
            }
        });
        audioListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                proccessInbound(Objects.requireNonNull(dataSnapshot.getValue(Inbound.class)));
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        AudioAttributes micAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        AudioAttributes listenAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        micFocus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE).setAudioAttributes(micAttributes).setAcceptsDelayedFocusGain(false).setOnAudioFocusChangeListener(this).build();
        listenFocus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(listenAttributes).setAcceptsDelayedFocusGain(false).setOnAudioFocusChangeListener(this).build();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                final Map<String, Object> claims = new HashMap<>();
                claims.put("userId", operator.getUser_id());
                okUtil.call("user_pulse_response.php", claims, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        if (response.isSuccessful()) {
                            try (response; response) {
                                assert response.body() != null;
                                final String flagData = response.body().string();
                                flaggedIds.clear();
                                flaggedIds = gson.fromJson(flagData, new TypeToken<List<String>>() {
                                }.getType());
                                settings.edit().putString("flaggedIDs", flagData).apply();
                            } catch (IOException e) {
                                LOG.e("user_pulse_response.php", e.getMessage());
                            }
                        }
                    }
                });

            }
        }, 300000, 300000);
        if (!operator.getSubscribed()) timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendBroadcast(new Intent("advertise").setPackage("com.cb3g.channel19"));
            }
        }, 900000, 900000);
    }

    private void enableListeners() {
        databaseReference.child("paused").child(operator.getUser_id()).removeValue();
        databaseReference.child("onCall").child(operator.getUser_id()).removeValue();
        databaseReference.child("autoSkip").child(operator.getUser_id()).removeValue();
        databaseReference.child("keychain").addValueEventListener(this);
        databaseReference.child("ghost").addValueEventListener(this);
        databaseReference.child("paused").addValueEventListener(this);
        databaseReference.child("onCall").addValueEventListener(this);
        databaseReference.child("silenced").addValueEventListener(this);
        databaseReference.child("blockedFromReservoir").addValueEventListener(this);
        databaseReference.child("disableProfile").addValueEventListener(this);
        databaseReference.child("hinderTexts").addValueEventListener(this);
        databaseReference.child("hinderPhotos").addValueEventListener(this);
        databaseReference.child("autoSkip").child(operator.getUser_id()).addValueEventListener(autoSkipEventListener);
        databaseReference.child("volumes").child(operator.getUser_id()).addChildEventListener(volumeChildListener);
        databaseReference.child("controlling").child(operator.getUser_id()).addChildEventListener(this);
        databaseReference.child("appOptions").addValueEventListener(appOptionsListener);

        databaseReference.child("blocking").addValueEventListener(this);
        databaseReference.child("ghostModeAvailible").addValueEventListener(this);
        databaseReference.child("flagsEnabled").addValueEventListener(this);
        databaseReference.child("radioShopOpen").addValueEventListener(this);
        databaseReference.child("silencing").addValueEventListener(this);
    }

    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
        final ControlCode control = snapshot.child("control").getValue(ControlCode.class);
        final String key = snapshot.getKey();
        if (key != null)
            databaseReference.child("controlling").child(operator.getUser_id()).child(key).removeValue();
        if (control != null) {
            switch (control) {
                case ALERT: {
                    if (MI != null) {
                        snacks.add(new Snack(snapshot.child("data").getValue(String.class), Snackbar.LENGTH_INDEFINITE));
                        checkForMessages();
                    }
                }
                break;
                case TOAST:
                    if (MI != null) {
                        snacks.add(new Snack(snapshot.child("data").getValue(String.class), Snackbar.LENGTH_LONG));
                        checkForMessages();
                    }
                    break;
                case PRIVATE_MESSAGE:
                    final Message message = snapshot.child("data").getValue(Message.class);
                    assert message != null;
                    if (settings.getBoolean("pmenabled", true)) {
                        if (!RadioService.blockListContainsId(textIDs, message.getUser_id())) {
                            if (Objects.equals(chat.get(), message.getUser_id())) {
                                if (MI != null) MI.displayChat(null, true, false);
                                else
                                    sendBroadcast(new Intent("nineteenChatSound").setPackage("com.cb3g.channel19"));
                            } else {
                                if (!recording) sp.play(chain, .1f, .1f, 1, 0, 1f);
                                messages.add(message);
                                checkForMessages();
                            }
                        }
                    } else {
                        if (!RadioService.blockListContainsId(textIDs, message.getUser_id())) {
                            snacks.add(new Snack(message.getHandle() + " sent you a private message", Snackbar.LENGTH_SHORT));
                            checkForMessages();
                        }
                    }
                    break;
                case PRIVATE_PHOTO: //Private Photo
                    final Photo privatePhoto = snapshot.child("data").getValue(Photo.class);
                    assert privatePhoto != null;
                    if (RadioService.blockListContainsId(photoIDs, privatePhoto.getSenderId()))
                        return;
                    loader.preload(privatePhoto.getUrl(), new RequestListener<>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<File> target, boolean isFirstResource) {
                            snacks.add(new Snack("Failed to download image", Snackbar.LENGTH_SHORT));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(@NonNull File resource, @NonNull Object model, Target<File> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                            if (Objects.equals(chat.get(), privatePhoto.getSenderId())) {
                                if (MI != null) MI.displayChat(null, true, false);
                                else
                                    sendBroadcast(new Intent("nineteenChatSound").setPackage("com.cb3g.channel19"));
                            } else {
                                if (!recording) sp.play(glass, .1f, .1f, 1, 0, 1f);
                                if (settings.getBoolean("photos", true))
                                    photos.add(privatePhoto);
                                else
                                    snacks.add(new Snack(privatePhoto.getSenderHandle() + " sent you a private photo", Snackbar.LENGTH_SHORT));
                                checkForMessages();
                            }
                            return true;
                        }
                    });
                    break;
                case MASS_PHOTO: //Mass Photo
                    final Photo massPhoto = snapshot.child("data").getValue(Photo.class);
                    assert massPhoto != null;
                    if (RadioService.blockListContainsId(photoIDs, massPhoto.getSenderId())) return;
                    loader.preload(massPhoto.getUrl(), new RequestListener<>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<File> target, boolean isFirstResource) {
                            snacks.add(new Snack("Failed to download image", Snackbar.LENGTH_SHORT));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(@NonNull File resource, @NonNull Object model, Target<File> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                            if (!recording && MI != null)
                                sp.play(glass, .1f, .1f, 1, 0, 1f);
                            if (settings.getBoolean("photos", true) && !paused)
                                photos.add(massPhoto);
                            else if (MI != null)
                                snacks.add(new Snack(massPhoto.getSenderHandle() + " sent you a mass photo", Snackbar.LENGTH_SHORT));
                            Utils.getDatabase().getReference().child("mass history").child(operator.getUser_id()).push().setValue(massPhoto);
                            checkForMessages();
                            return true;
                        }
                    });
                    break;
                case SALUTE:
                    final ReputationMark salute = snapshot.child("data").getValue(ReputationMark.class);
                    assert salute != null;
                    if (MI != null) {
                        MI.showSnack(new Snack(salute.getHandle() + " sent you a SALUTE!", Snackbar.LENGTH_INDEFINITE));
                    }
                    break;
                case FLAG:
                    final ReputationMark flag = snapshot.child("data").getValue(ReputationMark.class);
                    assert flag != null;
                    sendBroadcast(new Intent("bird").putExtra("userId", flag.getUserId()).setPackage("com.cb3g.channel19"));
                    if (MI != null)
                        MI.showSnack(new Snack(flag.getHandle() + " sent you a FLAG!", Snackbar.LENGTH_LONG));
                    checkFlagOut();
                    break;
                case LONG_FLAG:
                    final ReputationMark longFlag = snapshot.child("data").getValue(ReputationMark.class);
                    assert longFlag != null;
                    sendBroadcast(new Intent("longFlag").putExtra("userId", longFlag.getUserId()).putExtra("handle", longFlag.getHandle()).setPackage("com.cb3g.channel19"));
                    checkFlagOut();
                    break;
                case KICK_USER:
                    stopSelf();
                    break;
                case CLEAR_BLOCK_LISTS:
                    blockedIDs.clear();
                    photoIDs.clear();
                    textIDs.clear();
                    salutedIds.clear();
                    flaggedIds.clear();
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("blockedIDs", "[]").apply();
                    editor.putString("photoIDs", "[]").apply();
                    editor.putString("textIDs", "[]").apply();
                    editor.putString("salutedIDs", "[]").apply();
                    editor.putString("flaggedIDs", "[]").apply();
                    editor.apply();
                    if (MI != null) {
                        snacks.add(new Snack("Block Lists Cleared", Snackbar.LENGTH_INDEFINITE));
                        checkForMessages();
                    }
                    break;
            }
        }
    }

    private final ValueEventListener appOptionsListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            appOptions = snapshot.getValue(AppOptionsObject.class);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
        }
    };

    private void removeListeners() {
        databaseReference.child("controlling").child(operator.getUser_id()).removeEventListener((ChildEventListener) this);
        databaseReference.child("appOptions").removeEventListener(appOptionsListener);
        databaseReference.child("keychain").removeEventListener((ValueEventListener) this);
        databaseReference.child("hinderPhotos").removeEventListener((ValueEventListener) this);
        databaseReference.child("hinderTexts").removeEventListener((ValueEventListener) this);
        databaseReference.child("disableProfile").removeEventListener((ValueEventListener) this);
        databaseReference.child("blockedFromReservoir").removeEventListener((ValueEventListener) this);
        databaseReference.child("silenced").removeEventListener((ValueEventListener) this);
        databaseReference.child("paused").removeEventListener((ValueEventListener) this);
        databaseReference.child("ghost").removeEventListener((ValueEventListener) this);
        databaseReference.child("onCall").removeEventListener((ValueEventListener) this);

        databaseReference.child("autoSkip").child(operator.getUser_id()).removeEventListener(autoSkipEventListener);
        databaseReference.child("volumes").child(operator.getUser_id()).removeEventListener(volumeChildListener);

        databaseReference.child("autoSkip").child(operator.getUser_id()).removeValue();
        databaseReference.child("paused").child(operator.getUser_id()).removeValue();
        databaseReference.child("onCall").child(operator.getUser_id()).removeValue();

        if (operator.getChannel() != null)
            databaseReference.child("audio").child(operator.getChannel().getChannel_name()).removeEventListener(audioListener);
        listenForCoordinates(false);
    }

    void registerDefaultNetworkCallback() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {

                connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        makePoor(false);
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        makePoor(true);
                    }

                });
            }
        } catch (Exception e) {
            Log.e("network", Objects.requireNonNull(e.getMessage()));
        }
    }

    private int blockLimit = 20;

    private void proccessInbound(Inbound inbound) {
        if (inbound.getStamp() < enterStamp) return;
        if (inbound.getChannel() != Objects.requireNonNull(operator.getChannel()).getChannel())
            return;
        if (inbound.getUser_id().equals(operator.getUser_id()) && !inbound.getTalkback()) return;
        if (blockListContainsId(blockedIDs, inbound.getUser_id())) return;
        List<String> blockLists = gson.fromJson(inbound.getBlockList(), new TypeToken<List<String>>() {
        }.getType());
        if (blockLists.contains(operator.getUser_id())) return;
        File file = new File(Utils.formatLocalAudioFileLocation(saveDirectory, inbound.getUser_id(), inbound.getStamp()));
        file.deleteOnExit();
        temporaryStorage.getReferenceFromUrl(inbound.getDownloadUrl()).getFile(file).addOnSuccessListener(taskSnapshot -> {
            inbounds.add(inbound);
            if (paused) {
                if (inbounds.size() > pauseLimit + 50 && !operator.getAdmin()) {
                    removeZeros();
                    return;
                }
                if (MI != null) {
                    if (!playing) updateDisplay();
                    MI.updateQueue(inbounds.size(), paused, poor);
                    notification();
                }
                return;
            }
            if (playing || recording) {
                if (MI != null) MI.updateQueue(inbounds.size(), false, poor);
            } else sendBroadcast(new Intent("play").setPackage("com.cb3g.channel19"));
        }).addOnFailureListener(exception -> Logger.INSTANCE.e("download task error", exception.getMessage()));
    }

    public void rewind() {
        inbounds.clear();
        removeZeros();
        if (operator.getChannel() != null) {
            databaseReference.child("audio").child(operator.getChannel().getChannel_name()).removeEventListener(audioListener);
            if (operator.getAdmin()) enterStamp = Instant.now().getEpochSecond() - 7200;
            else enterStamp = Instant.now().getEpochSecond() - 300;
            databaseReference.child("audio").child(operator.getChannel().getChannel_name()).addChildEventListener(audioListener);
        }
        if (paused) resumePlayback();
    }

    private void play() {
        if (!inbounds.isEmpty() && !player.isPlaying()) {
            final Inbound inbound = inbounds.get(0);
            if (autoSkip.contains(inbound.getUser_id())) {
                removeZeros();
                if (MI != null)
                    MI.showSnack(new Snack("Auto-skipped " + inbound.getHandle(), Snackbar.LENGTH_SHORT));
            } else {
                playing = true;
                try {
                    float volume = scaleVolume(returnUserVolume(inbound.getUser_id()));
                    player.setDataSource(Utils.formatLocalAudioFileLocation(saveDirectory, inbound.getUser_id(), inbound.getStamp()));
                    player.setOnPreparedListener(mp -> {
                        updateDisplay();
                        if (!mute) {
                            mp.setVolume(volume, volume);
                        }
                        if (!recording) mp.start();
                    });
                    player.setOnCompletionListener(mp -> removeZeros());
                    player.prepareAsync();
                    startFadeIn();
                } catch (IOException | IllegalStateException e) {
                    Logger.INSTANCE.e("play()", e);
                    removeZeros();
                }
            }

        }
    }

    float faderVolume = 0;

    private void startFadeIn() {
        final int FADE_DURATION = 2000;
        final int FADE_INTERVAL = 250;
        int numberOfSteps = FADE_DURATION / FADE_INTERVAL;
        final float deltaVolume = 1 / (float) numberOfSteps;
        final Timer timer = new Timer(true);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                fadeInStep(deltaVolume);
                if (faderVolume >= 1f) {
                    timer.cancel();
                    timer.purge();
                }
            }
        };
        timer.schedule(timerTask, FADE_INTERVAL, FADE_INTERVAL);
    }

    private void fadeInStep(float deltaVolume) {
        player.setVolume(faderVolume, faderVolume);
        faderVolume += deltaVolume;
    }

    private void removeZeros() {
        try {
            if (playing) {
                if (player.isPlaying()) player.stop();
                player.reset();
            }
        } catch (IllegalStateException e) {
            Logger.INSTANCE.e("removeZeros() IllegalStateException" + e);
        } finally {
            playing = false;
            if (!inbounds.isEmpty()) {
                Inbound inbound = inbounds.get(0);
                final File file = new File(Utils.formatLocalAudioFileLocation(saveDirectory, inbound.getUser_id(), inbound.getStamp()));
                if (!file.delete()) LOG.e("Failed to delete");
                inbounds.remove(inbound);
            }
            if (inbounds.isEmpty()) {
                updateDisplay();
            } else {
                if (paused || recording) updateDisplay();
                else sendBroadcast(new Intent("play").setPackage("com.cb3g.channel19"));
            }
        }
    }

    private void transmit(final String fileLocation, boolean talkback, long duration, long stamp) {
        final File file = new File(fileLocation);
        if (!file.exists()) return;
        StorageReference reference = temporaryStorage.getReference().child("audio").child(operator.getUser_id() + "-" + stamp + ".m4a");
        StorageMetadata metadata = new StorageMetadata.Builder().setContentType("audio/m4a").setCustomMetadata("user", operator.getUser_id()).setCustomMetadata("stamp", String.valueOf(stamp)).build();
        UploadTask uploadTask = reference.putFile(Uri.fromFile(file), metadata);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                if (MI != null) MI.showSnack(new Snack("Slow Connection", Snackbar.LENGTH_SHORT));
            }
            return reference.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful() && operator.getChannel() != null) {
                List<String> blockList = new ArrayList<>();
                for (Block block : blockedIDs) {
                    blockList.add(block.getI());
                }
                final String downloadUri = task.getResult().toString();
                Inbound inbound = new Inbound();
                inbound.setDownloadUrl(downloadUri);
                inbound.setChannel(operator.getChannel().getChannel());
                inbound.setStamp(stamp);
                inbound.setDuration(duration);
                inbound.setTalkback(talkback);
                inbound.setAdmin(operator.getAdmin());
                inbound.setBlockList(gson.toJson(blockList));
                inbound.setUser_id(operator.getUser_id());
                inbound.setProfileLink(operator.getProfileLink());
                inbound.setHandle(operator.getHandle());
                inbound.setCarrier(operator.getCarrier());
                inbound.setRank(operator.getRank());
                if (operator.getSharing() && !operator.getUserLocationString().isEmpty())
                    inbound.setTown(operator.getUserLocationString());
                else inbound.setTown(operator.getTown());
                databaseReference.child("audio").child(operator.getChannel().getChannel_name()).push().setValue(inbound).addOnCompleteListener(task1 -> {
                    if (!recording) sp.play(confirm, .1f, .1f, 1, 0, 1f);
                    if (MI != null)
                        MI.showSnack(new Snack(getString(R.string.checkmark), Snackbar.LENGTH_SHORT));
                });
                if (!file.delete()) Log.e("transmit()", "Failed to delete file!");
                final Map<String, Object> claims = new HashMap<>();
                claims.put("userId", operator.getUser_id());
                okUtil.call("user_key_count.php", claims, new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        try (response; response) {
                            if (response.isSuccessful()) {
                                assert response.body() != null;
                                JSONObject data = new JSONObject(response.body().string());
                                operator.setCount(data.getInt("count"));
                                operator.setSalutes(data.getInt("salutes"));
                                operator.setRank(data.getString("rank"));
                            }
                        } catch (JSONException | IOException e) {
                            LOG.e(String.valueOf(e));
                        }
                    }
                });
            }
        });
    }

    static boolean blockListContainsId(final List<Block> blockList, final String id) {
        for (Block block : blockList) {
            if (block.getI().equals(id)) return true;
        }
        return false;
    }

    static boolean isInChannel(String userId) {
        for (User user : users) {
            if (user.getUser_id().equals(userId)) return true;
        }
        return false;
    }

    public void listenForCoordinates(boolean listen) {
        if (listen) {
            if (operator.getLocationEnabled().get()) {
                RadioService.databaseReference.child("locations").addValueEventListener(this);
            }
        } else {
            RadioService.databaseReference.child("locations").removeEventListener((ValueEventListener) this);
            databaseReference.child("locations").child(operator.getUser_id()).removeValue();
            coordinates.clear();
            nearby.clear();
        }
    }

    public int returnUserVolume(String id) {
        for (int i = 0; i < volumes.size(); i++) {
            UserVolume volume = volumes.get(i);
            if (volume.getId().equals(id)) return volume.getVolume();
        }
        return 100;
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        switch (Objects.requireNonNull(dataSnapshot.getKey())) {
            case "paused" -> {
                pausedUsers.clear();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    pausedUsers.add(child.getKey());
                }
                userList = settleUserList(users);
                if (MI != null) MI.updateUserList(userList);
            }
            case "onCall" -> {
                onCallUsers.clear();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    onCallUsers.add(child.getKey());
                }
                userList = settleUserList(users);
                if (MI != null) MI.updateUserList(userList);
            }
            case "silenced" -> {
                silencedUsers.clear();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    silencedUsers.add(child.getKey());
                }
                operator.setSilenced(silencedUsers.contains(operator.getUser_id()));
                if (MI != null) {
                    MI.updateDisplay(getlatest(), inbounds.size(), getDuration(), paused, poor, getlatestStamp());
                }
                userList = settleUserList(users);
                if (MI != null) MI.updateUserList(userList);
            }
            case "ghost" -> {
                ghostUsers.clear();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    ghostUsers.add(child.getKey());
                }
                userList = settleUserList(users);
                if (MI != null) MI.updateUserList(userList);
            }
            case "hinderPhotos" -> {
                List<String> ids = new ArrayList<>();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    ids.add(child.getKey());
                }
                operator.setHinderPhotos(ids.contains(operator.getUser_id()));
            }
            case "hinderTexts" -> {
                List<String> ids = new ArrayList<>();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    ids.add(child.getKey());
                }
                operator.setHinderTexts(ids.contains(operator.getUser_id()));
            }
            case "disableProfile" -> {
                List<String> ids = new ArrayList<>();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    ids.add(child.getKey());
                }
                operator.setDisableProfile(ids.contains(operator.getUser_id()));
            }
            case "blockedFromReservoir" -> {
                List<String> ids = new ArrayList<>();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    ids.add(child.getKey());
                }
                operator.setBlockedFromReservoir(ids.contains(operator.getUser_id()));
            }
            case "locations" -> {
                coordinates.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    coordinates.add(snapshot.getValue(Coordinates.class));
                }
            }
            case "keychain" -> {
                operator.setKey(Objects.requireNonNull(dataSnapshot.getValue(String.class)));
                settings.edit().putString("keychain", operator.getKey()).apply();
            }
        }
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot snapshot) {

    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
    }

    public void locationUpdated(final Location inputLocation) {
        if (inputLocation != null) {
            executor.execute(() -> {
                try {
                    final Geocoder geoCoder = new Geocoder(RadioService.this, locale);
                    final List<Address> addressList = geoCoder.getFromLocation(inputLocation.getLatitude(), inputLocation.getLongitude(), 1);
                    if (addressList != null) {
                        for (Address address : addressList) {
                            final String city = address.getLocality();
                            final String state = address.getAdminArea();
                            String locationString = null;
                            final String country_code = address.getCountryCode();
                            if (country_code != null) {
                                if (city != null && state != null) {
                                    if (address.getCountryCode().equals("US"))
                                        locationString = city.trim() + ", " + getAbbreviationFromUSState(state);
                                    else
                                        locationString = address.getLocality() + ", " + address.getCountryCode();
                                }
                                if (locationString != null) {
                                    locationString = locationString.replaceAll("[0-9]", "").trim();
                                    locationString = locationString.replace("New ", "").replace("North ", "N ").replace("South ", "S ").replace("East ", "E ").replace("Port ", "").replace("Bridge ", "").replace("West ", "W ").replace("Upper", "").replace("Lower", "").replace("Township", "").replace("Court House", "").replace("Charter", "").replace(" ,", ",").replace(".", "").replace(" ,", ",").trim();
                                    if (locationString.length() > 19)
                                        locationString = locationString.replace("N ", "").replace("S ", "").replace("E ", "").replace("W ", "").replace("St ", "").trim();
                                    if (locationString.length() > 19 && country_code.equals("US"))
                                        locationString = locationString.substring(0, locationString.length() - 4).trim();
                                    locationString = locationString + EmojiParser.parseToUnicode(" :globe_with_meridians:");
                                    if (!locationString.equals(operator.getUserLocationString()))
                                        sendBroadcast(new Intent("locationUpdate").setPackage("com.cb3g.channel19").putExtra("data", locationString));
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    LOG.e("geoCoder EXCEPTION " + e);
                }
                if (operator.getChannel() != null || operator.getNearbyLimit() != 0) {
                    for (Coordinates otherUser : new ArrayList<>(coordinates)) {
                        if (!otherUser.getUserId().equals(operator.getUser_id()) && !nearby.contains(otherUser.getUserId()) && isInChannel(otherUser.getUserId())) {
                            int distance = returnDistance(inputLocation, otherUser.getLatitude(), otherUser.getLongitude());
                            if (distance < operator.getNearbyLimit()) {
                                snacks.add(new Snack(otherUser.getHandle() + " is nearby " + "(" + distance + "m)", Snackbar.LENGTH_INDEFINITE));
                                nearby.add(otherUser.getUserId());
                                sendBroadcast(new Intent("checkForMessages").setPackage("com.cb3g.channel19"));
                            }
                        }
                    }
                }
            });
        }
    }

    private int returnDistance(Location from, double latitude, double longitude) {
        Location des = new Location("providername");
        des.setLatitude(latitude);
        des.setLongitude(longitude);
        return (int) (0.621371 * from.distanceTo(des)) / 1000;
    }

    private void getUsersOnChannel() {
        if (operator.getChannel() == null) return;
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", operator.getUser_id());
        claims.put("channel", operator.getChannel().getChannel());
        claims.put("language", language);
        okUtil.call("user_list.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                LOG.e("get_users_on_channel IOException " + e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull final Response response) {
                if (response.isSuccessful()) {
                    try (response; response) {
                        assert response.body() != null;
                        users = returnFilteredList(returnUserListObjectFromJson(response.body().string()));
                        userList = settleUserList(users);
                        handler.post(() -> {
                            if (MI != null) MI.updateUserList(userList);
                        });
                    } catch (IOException e) {
                        LOG.e("get_users_on_channel", e.getMessage());
                    }
                }
            }
        });
    }

    private List<UserListEntry> settleUserList(List<User> users) {
        List<UserListEntry> newList = new ArrayList<>();
        for (User user : users) {
            String id = user.getUser_id();
            newList.add(new UserListEntry(user, silencedUsers.contains(id), ghostUsers.contains(id), pausedUsers.contains(id), onCallUsers.contains(id), autoSkip.contains(id)));
        }
        newList.sort((one, two) -> {
            if ((pausedUsers.contains(one.getUser().getUser_id()) || onCallUsers.contains(one.getUser().getUser_id())) && (pausedUsers.contains(two.getUser().getUser_id()) || onCallUsers.contains(two.getUser().getUser_id())))
                return 0;
            if ((!pausedUsers.contains(one.getUser().getUser_id()) && !onCallUsers.contains(one.getUser().getUser_id())) && (pausedUsers.contains(two.getUser().getUser_id()) || onCallUsers.contains(two.getUser().getUser_id())))
                return -1;
            if ((pausedUsers.contains(one.getUser().getUser_id()) || onCallUsers.contains(one.getUser().getUser_id())) && (!pausedUsers.contains(two.getUser().getUser_id()) && !onCallUsers.contains(two.getUser().getUser_id())))
                return 1;
            return 0;
        });
        return newList;
    }

    void recording(final boolean isRecording) {
        recording = isRecording;
        if (MI != null) {
            if (recording) {
                pre_key_up();
                if (headsetActive()) {
                    if (Utils.permissionsAccepted(RadioService.this, Utils.getBluetoothPermissions())) {
                        useSco(true);
                    } else if (MI != null) MI.requestBluetoothPermission();
                } else MI.startTransmit();
            } else {
                if (headsetActive()) useSco(false);
                post_key_up();
            }
        }
    }

    private boolean headsetActive() {
        if (bluetooth && bluetoothAdapter != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                return bluetoothAdapter.isEnabled() && bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothAdapter.STATE_CONNECTED;
            }
        }
        return false;
    }

    public void useSco(final boolean sco) {
        if (sco) {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
        } else {
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
    }

    private void pre_key_up() {
        audioManager.abandonAudioFocusRequest(listenFocus);
        if (!bluetoothEnabled) audioManager.requestAudioFocus(micFocus);
        if (playing) player.pause();
    }

    public void post_key_up() {
        if (!bluetoothEnabled) audioManager.abandonAudioFocusRequest(micFocus);
        audioManager.requestAudioFocus(listenFocus);
        if (playing) {
            if (!paused) player.start();
        } else {
            sendBroadcast(new Intent("play").setPackage("com.cb3g.channel19"));
            checkForMessages();
        }
        updateDisplay();
    }

    private void updateDisplay() {
        if (MI == null) return;
        if (recording) {
            MI.updateQueue(inbounds.size(), paused, poor);
        } else {
            MI.updateDisplay(getlatest(), inbounds.size(), getDuration(), paused, poor, getlatestStamp());
            notification();
        }
    }

    int[] returnAnimationMax() {
        if (!playing || paused) return new int[]{0, 0};
        else return new int[]{getDuration(), player.getCurrentPosition()};
    }

    User returnTalkerEntry() {
        if (!inbounds.isEmpty()) {
            Inbound inbound = inbounds.get(0);
            String currentId = inbound.getUser_id();
            for (User user : users) {
                if (user.getUser_id().equals(currentId)) return user;
            }
            User entry = new User();
            entry.setUser_id(currentId);
            entry.setRadio_hanlde(inbound.getHandle());
            entry.setProfileLink(inbound.getProfileLink());
            return entry;
        }
        return null;
    }

    ProfileDisplay getlatest() {
        ProfileDisplay data = new ProfileDisplay();
        data.setHandle(onlineStatus);
        if (!inbounds.isEmpty()) {
            data = inboundObjectToProfileDisplayObject(inbounds.get(0));
            data.setDuration(getDuration() / 1000);
            return data;
        }
        return data;
    }

    private ProfileDisplay inboundObjectToProfileDisplayObject(Inbound inbound) {
        ProfileDisplay display = new ProfileDisplay();
        display.setHandle(inbound.getHandle());
        display.setCarrier(inbound.getCarrier());
        display.setTown(inbound.getTown());
        display.setRank(inbound.getRank());
        display.setProfileLink(inbound.getProfileLink());
        return display;
    }

    long getlatestStamp() {
        if (!inbounds.isEmpty()) return inbounds.get(0).getStamp();
        else return Instant.now().getEpochSecond();
    }

    int getDuration() {
        if (!playing) return 0;
        return player.getDuration() - player.getCurrentPosition();
    }

    public void entered(Channel channel) {
        enterStamp = Instant.now().getEpochSecond();
        if (operator.getChannel() != null)
            databaseReference.child("audio").child(channel.getChannel_name()).removeEventListener(audioListener);
        operator.setChannel(channel);
        databaseReference.child("audio").child(channel.getChannel_name()).addChildEventListener(audioListener);
        getUsersOnChannel();
        if (operator.getInvisible() || ghostUsers.contains(operator.getUser_id()) || operator.getAdmin())
            return;
        Utils.usersInChannel(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    if (response.isSuccessful()) {
                        Log.i("logging", "response is succesfull.php");
                        assert response.body() != null;
                        final String data = response.body().string();
                        Log.i("logging", "user_in_channel.php");
                        Log.i("logging", data);
                        final ArrayList<String> ids = new Gson().fromJson(data, new TypeToken<ArrayList<String>>() {
                        }.getType());
                        if (!ids.isEmpty()) {
                            UtilsKKt.sendControl(ids, new ControlObject(ControlCode.TOAST, operator.getHandle() + " has entered the channel"));
                        }
                    }
                } catch (IOException e) {
                    if (e.getMessage() != null) Log.e("user_in_channel.php", e.getMessage());
                }
            }
        });
    }

    private boolean isUpdatedVolumeUserZero(String id) {
        if (!playing) return false;
        if (inbounds.isEmpty()) return false;
        return inbounds.get(0).getUser_id().equals(id);
    }

    private void update_block_list(final List<Block> blockList, final String field) {
        settings.edit().putString(field, gson.toJson(blockList)).apply();
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", operator.getUser_id());
        claims.put("list", gson.toJson(blockList));
        claims.put("field", field);
        okUtil.call("user_update_block_list.php", claims);
    }

    private List<User> returnFilteredList(final List<User> inbound) {
        if (operator.getAdmin()) return inbound;
        List<User> returnedList = new ArrayList<>();
        for (User child : inbound) {
            if (!blockListContainsId(blockedIDs, child.getUser_id()) && !ghostUsers.contains(child.getUser_id()))
                returnedList.add(child);
        }
        return returnedList;
    }

    public String returnLocation() {
        return operator.getUserLocationString();
    }

    private void uploadLocation(final String location_string) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", operator.getUser_id());
        claims.put("location", location_string);
        okUtil.call("user_change_location.php", claims);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        File directory = new File(saveDirectory);
        File[] files = directory.listFiles(file -> (file.getPath().endsWith(".m4a")));
        executor.execute(() -> {
            assert files != null;
            for (File file : files) {
                if (!file.delete()) Log.e("onDestroy()", "Failed to delete file!");
            }
            ExecutorUtils.shutdown(executor);
        });
        logOut();
    }

    public void logOut() {
        removeListeners();
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", operator.getUser_id());
        okUtil.call("user_log_out.php", claims);
        emptyPlayer(true);
        sp.release();
        if (settings.getBoolean("kicksound", true)) {
            try {
                player.setDataSource(context, KICK_URI);
                player.setOnPreparedListener(MediaPlayer::start);
                player.setOnCompletionListener(mp -> {
                    mp.reset();
                    mp.release();
                });
                player.prepare();
            } catch (IOException e) {
                LOG.e(String.valueOf(e));
            }
        } else {
            player.reset();
            player.release();
            //audioManager.abandonAudioFocus(null);
        }
        final SharedPreferences.Editor edit = widget.edit();
        edit.putString("handle", "");
        edit.putString("carrier", "Keep The Shiny Side Up Driver");
        edit.putString("status", "Logged Out");
        edit.putString("channel", "");
        edit.putString("rank", "f");
        edit.putString("title", " ");
        edit.apply();
        final int[] WIDGETS = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, Channel19.class));
        if (WIDGETS.length != 0)
            sendBroadcast(new Intent(context, Channel19.class).setPackage("com.cb3g.channel19").setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, WIDGETS).setPackage("com.cb3g.channel19"));
        if (callStateListenerRegistered) {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            telephonyManager.unregisterTelephonyCallback(callStateListener);
        }
        settings.edit().putBoolean("exiting", true).apply();
        unregisterReceiver(receiver);
        sendBroadcast(new Intent("killActivity").setPackage("com.cb3g.channel19"));
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @NonNull
    private List<String> returnStringListObjectFromJson(String json) {
        try {
            List<String> list = gson.fromJson(json, new TypeToken<List<String>>() {
            }.getType());
            if (list != null) return list;
        } catch (JsonSyntaxException e) {
            LOG.e("returnStringListObjectFromJson", e.getMessage());
        }
        return new ArrayList<>();
    }

    @NonNull
    private List<Block> returnBlockListObjectFromJson(String json) {
        try {
            List<Block> list = gson.fromJson(json, new TypeToken<List<Block>>() {
            }.getType());
            if (list != null) return list;
        } catch (JsonSyntaxException e) {
            LOG.e("returnBlockListObjectFromJson", e.getMessage());
        }
        return new ArrayList<>();
    }

    @NonNull
    private List<User> returnUserListObjectFromJson(String json) {
        try {
            List<User> list = gson.fromJson(json, new TypeToken<List<User>>() {
            }.getType());
            if (list != null) return list;
        } catch (JsonSyntaxException e) {
            LOG.e("returnUserListObjectFromJson", e.getMessage());
        }
        return new ArrayList<>();
    }

    public void main_activity_callbacks(MI callbacks) {
        MI = callbacks;
    }

    private String getAbbreviationFromUSState(String state) {
        return STATE_MAP.getOrDefault(state, state);
    }

    private void user_info_lookup(final String id) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", id);
        okUtil.call("user_info_lookup.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response; response) {
                    if (response.isSuccessful()) {
                        assert response.body() != null;
                        final JSONObject object = new JSONObject(response.body().string());
                        String content = object.getString("login") + "\n" + "Total Keyups: " + object.getString("email") + "\n" + "Salutes: " + object.getString("salutes") + ", Flags: " + object.getString("flags") + "\n" + "Version: " + object.getString("version_name") + " (" + object.getString("version") + ")" + "\n" + "Android Version: " + object.getString("build_number") + "\n" + "Donations: " + object.getString("donations") + "\n" + "UserId: " + object.getString("user_id");
                        final String created = object.getString("created");
                        if (!created.equals("2015-01-01 00:00:00"))
                            content += "\n" + "Created: " + created;
                        sendBroadcast(new Intent("show_result").setPackage("com.cb3g.channel19").putExtra("title", object.getString("radio_hanlde")).putExtra("content", content));
                    }
                } catch (JSONException | IOException e) {
                    LOG.e(String.valueOf(e));
                }
            }
        });
    }

    private void uploadListToDB(@NonNull final JSONArray list) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", operator.getUser_id());
        claims.put("list", list.toString());
        claims.put("field", "salutedIDs");
        okUtil.call("user_list_update.php", claims);
    }

    private float scaleVolume(int sliderValue) {
        return (float) (1 - (Math.log(100 - sliderValue) / Math.log(100)));
    }

    private void soundPool() {
        sp = new SoundPool.Builder().build();
        glass = sp.load(this, R.raw.glass, 1);
        register = sp.load(this, R.raw.register, 1);
        interrupt = sp.load(this, R.raw.interrupt, 1);
        wrong = sp.load(this, R.raw.wrong, 1);
        confirm = sp.load(this, R.raw.confirm, 1);
        chain = sp.load(this, R.raw.chian, 1);
        click = sp.load(this, R.raw.click, 1);
        clicktwo = sp.load(this, R.raw.clicktwo, 1);
        clickthree = sp.load(this, R.raw.clickthree, 1);
        newthree = sp.load(this, R.raw.newthree, 1);
        talkie = sp.load(this, R.raw.talkie, 1);
        mic = sp.load(this, R.raw.mic, 1);
        purge = sp.load(this, R.raw.purge, 1);
        skip = sp.load(this, R.raw.newskip, 1);
        type = sp.load(this, R.raw.type, 1);
    }

    public int getQueue() {
        return inbounds.size();
    }

    @NonNull
    private IntentFilter returnFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        filter.addAction("interrupt");
        filter.addAction("locationUpdate");
        filter.addAction("listUpdate");
        filter.addAction("token");
        filter.addAction("confirmInterrupt");
        filter.addAction("longFlag");
        filter.addAction("wrong");
        filter.addAction("register");
        filter.addAction("ghost");
        filter.addAction("purgeUser");
        filter.addAction("giphyupload");
        filter.addAction("checkForMessages");
        filter.addAction("fetch_users");
        filter.addAction("keyUpdate");
        filter.addAction("pauseLimitChange");
        filter.addAction("play");
        filter.addAction("savePhotoToDisk");
        filter.addAction("removeAllOf");
        filter.addAction("fetchInformation");
        filter.addAction("nineteenScroll");
        filter.addAction("nineteenQueChange");
        filter.addAction("nineteenNewChannel");
        filter.addAction("nineteenInZone");
        filter.addAction("nineteenTransmit");
        filter.addAction("nineteenPlayPause");
        filter.addAction("nineteenPause");
        filter.addAction("nineteenPlay");
        filter.addAction("nineteenMessage");
        filter.addAction("exitChannelNineTeen");
        filter.addAction("purgeNineTeen");
        filter.addAction("muteChannelNineTeen");
        filter.addAction("nineteenSendPM");
        filter.addAction("nineteenSkip");
        filter.addAction("nineteenSkipTwo");
        filter.addAction("nineteenUpdateBlocks");
        filter.addAction("nineteenClickSound");
        filter.addAction("nineteenTabSound");
        filter.addAction("nineteenBoxSound");
        filter.addAction("nineteenMicSound");
        filter.addAction("nineteenChatSound");
        filter.addAction("nineteenStaticSound");
        filter.addAction("nineteenEmptyPlayer");
        filter.addAction("nineteenBluetoothSettingChange");
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        return filter;
    }

    void makePoor(final boolean poor) {
        this.poor = poor;
        if (!poor) {// good signal
            onlineStatus = "Online";
        } else {// signal lost
            onlineStatus = "Dead Zone";
        }
        checkForMessages();
        if (MI != null) {
            MI.adjustColors(poor);
            updateDisplay();
        }
    }

    public boolean returnPoor() {
        return poor;
    }

    private void notification() {
        if (inbounds.isEmpty()) {
            updateNotification(null);
            updateWidget(null);
        } else {
            updateNotification(inbounds.get(0));
            updateWidget(inbounds.get(0));
        }
    }

    private void updateNotification(final Inbound inbound) {
        String handle, carrier;
        if (inbound != null) {
            handle = inbound.getHandle();
            carrier = inbound.getCarrier();
        } else {
            handle = onlineStatus;
            carrier = "";
        }
        if (mute || paused) {
            if (mute && !paused) handle = "MUTED";
            if (!mute) handle = "PAUSED";
            if (mute && paused) handle = "MUTED & PAUSED";
            carrier = "";
        }
        int color;
        if (onlineStatus.equals("Online")) color = Color.parseColor("#007aff");
        else color = Color.parseColor("#990000");
        int led = 0;
        mBuilder.setLights(color, led, 0);
        notifyview.setTextViewText(R.id.black_handle_tv, handle);
        notifyview.setTextViewText(R.id.black_carrier_tv, carrier);
        mBuilder.setNumber(inbounds.size());
        startForeground(19, mBuilder.build());
    }

    private void updateWidget(Inbound object) {
        SharedPreferences.Editor edit = widget.edit();
        if (object != null) {
            edit.putString("handle", object.getHandle());
            edit.putString("carrier", object.getCarrier());
            edit.putString("title", object.getTitle());
        } else {
            edit.putString("handle", onlineStatus);
            edit.putString("carrier", "");
            edit.putString("rank", "f");
            edit.putString("title", " ");
        }
        edit.putString("status", onlineStatus);
        if (mute || paused) {
            if (mute && !paused) edit.putString("channel", "MUTED");
            if (!mute && paused) edit.putString("channel", "PAUSED");
            if (mute && paused) edit.putString("channel", "MUTED & PAUSED");
        } else edit.putString("channel", "");
        edit.apply();
        sendBroadcast(new Intent(RadioService.this, Channel19.class).setPackage("com.cb3g.channel19").setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(RadioService.this).getAppWidgetIds(new ComponentName(RadioService.this, Channel19.class))));
    }

    private void buildNotification() {
        PendingIntent skipping = PendingIntent.getBroadcast(this, 0, new Intent("nineteenSkip").setPackage("com.cb3g.channel19"), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pausing = PendingIntent.getBroadcast(this, 0, new Intent("nineteenPlayPause").setPackage("com.cb3g.channel19"), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent opench19 = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setPackage("com.cb3g.channel19"), PendingIntent.FLAG_IMMUTABLE);
        notifyview.setOnClickPendingIntent(R.id.skipping, skipping);
        notifyview.setTextViewText(R.id.black_handle_tv, onlineStatus);
        notifyview.setOnClickPendingIntent(R.id.exitstrategy, pausing);
        mBuilder.setContentIntent(opench19);
        mBuilder.setSmallIcon(R.drawable.nineteen);
        mBuilder.setAutoCancel(false);
        mBuilder.setOngoing(true);
        mBuilder.setContent(notifyview);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("19", getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setShowBadge(false);
        channel.setSound(null, null);
        channel.enableLights(true);
        channel.setShowBadge(true);
        mNotificationManager.createNotificationChannel(channel);
        startForeground(19, mBuilder.build());
        if (widget.getBoolean("availible", false)) {
            SharedPreferences.Editor edit = widget.edit();
            edit.putString("status", onlineStatus);
            if (!mute) edit.putString("channel", "");
            else edit.putString("channel", "Muted");
            edit.putString("handle", "");
            edit.putString("carrier", "");
            edit.apply();
            sendBroadcast(new Intent(this, Channel19.class).setPackage("com.cb3g.channel19").setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, Channel19.class))));
        }
    }

    void flag_out(final String targetId) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", targetId);
        claims.put("adminId", operator.getUser_id());
        okUtil.call("user_flag_out.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    if (response.isSuccessful()) {
                        sendBroadcast(new Intent("fetch_users").setPackage("com.cb3g.channel19"));
                        UtilsKKt.sendControl(targetId, new ControlObject(ControlCode.KICK_USER, targetId));
                    }
                }
            }
        });
    }

    void bannUser(final String targetId) {
        if (operator.getSilenced()) return;
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", targetId);
        claims.put("adminId", operator.getUser_id());
        okUtil.call("user_ban.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    if (response.isSuccessful()) {
                        sendBroadcast(new Intent("fetch_users").setPackage("com.cb3g.channel19"));
                        kick(targetId);
                    }
                }
            }
        });
    }

    void silence(final String targetId, final String handle) {
        if (operator.getAdmin()) return;
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", targetId);
        claims.put("userHandle", handle);
        claims.put("adminId", operator.getUser_id());
        claims.put("adminHandle", operator.getHandle());
        okUtil.call("user_silence_new.php", claims);
    }

    void ghost() {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", operator.getUser_id());
        okUtil.call("user_ghost_new.php", claims);
    }

    void unsilence(final String targetId, final String handle) {
        if (operator.getAdmin()) return;
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", targetId);
        claims.put("userHandle", handle);
        claims.put("adminId", operator.getUser_id());
        claims.put("adminHandle", operator.getHandle());
        okUtil.call("user_unsilence_new.php", claims);
    }

    void saluteUser(final User target) {
        if (operator.getSilenced()) return;
        if (salutedIds.contains(target.getUser_id()) && !operator.getAdmin()) {
            snacks.add(new Snack("Already Saluted", Snackbar.LENGTH_SHORT));
            checkForMessages();
            return;
        }
        UtilsKKt.sendControl(target.getUser_id(), new ControlObject(ControlCode.SALUTE, new ReputationMark(operator.getUser_id(), operator.getHandle())));
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", target.getUser_id());
        claims.put("handle", operator.getHandle());
        okUtil.call("user_salute_new.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    if (response.isSuccessful()) {
                        snacks.add(new Snack("Saluted " + target.getRadio_hanlde() + "!", Snackbar.LENGTH_SHORT));
                        checkForMessages();
                        if (!operator.getAdmin()) {
                            salutedIds.add(target.getUser_id());
                            uploadListToDB(new JSONArray(salutedIds));
                        }
                    }
                }
            }
        });
    }

    void flagUser(User target) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", target.getUser_id());
        claims.put("operatorId", operator.getUser_id());
        claims.put("handle", operator.getHandle());
        okUtil.call("user_flag.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    if (response.isSuccessful()) {
                        snacks.add(new Snack("Flagged " + target.getRadio_hanlde() + "!", Snackbar.LENGTH_SHORT));
                        checkForMessages();
                        if (!operator.getAdmin()) {
                            flaggedIds.add(target.getUser_id());
                            checkFlagOut();
                        }
                    }
                }
            }
        });
    }

    public void longFlagUser(User target) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", target.getUser_id());
        claims.put("operatorId", operator.getUser_id());
        claims.put("handle", operator.getHandle());
        okUtil.call("user_long_flag.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    if (response.isSuccessful()) {
                        snacks.add(new Snack("LONG Flagged " + target.getRadio_hanlde() + "!", Snackbar.LENGTH_SHORT));
                        checkForMessages();
                        if (!operator.getAdmin()) {
                            flaggedIds.add(target.getUser_id());
                        }
                        UtilsKKt.sendControl(target.getUser_id(), new ControlObject(ControlCode.LONG_FLAG, new ReputationMark(operator.getUser_id(), operator.getHandle())));
                    }
                }
            }
        });
    }

    public void keyUpWasInterupted(String userId) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        okUtil.call("interupted.php", claims);
    }

    void pauseOrplay(@NonNull User user) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUser_id());
        okUtil.call("user_play_pause.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                getUsersOnChannel();
            }
        });
    }

    void kickUser(@NonNull User user) {
        kick(user.getUser_id());
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUser_id());
        okUtil.call("user_kick.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                getUsersOnChannel();
            }
        });
    }

    private void kick(String targetId) {
        UtilsKKt.sendControl(targetId, new ControlObject(ControlCode.KICK_USER, targetId));
    }

    void blockAll(String id, String handle) {
        if (blockedIDs.size() >= blockLimit && photoIDs.size() >= blockLimit && textIDs.size() >= blockLimit) {
            snacks.add(new Snack("Block Lists Full!", Snackbar.LENGTH_SHORT));
            checkForMessages();
            return;
        }
        if (!blockListContainsId(blockedIDs, id) || !blockListContainsId(photoIDs, id) || !blockListContainsId(textIDs, id)) {
            if (!blockListContainsId(blockedIDs, id) && blockedIDs.size() <= blockLimit)
                blockNewId(id, handle, false);
            if (!blockListContainsId(photoIDs, id) && photoIDs.size() <= blockLimit)
                blockNewPhoto(id, handle, false);
            if (!blockListContainsId(textIDs, id) && textIDs.size() <= blockLimit)
                blockNewText(id, handle, false);
        } else {
            snacks.add(new Snack("Already blocked", Snackbar.LENGTH_SHORT));
            checkForMessages();
        }
    }

    void blockNewId(final String id, final String handle, boolean toast) {
        if (blockedIDs.size() >= blockLimit && !operator.getAdmin()) {
            snacks.add(new Snack("Block List Full!", Snackbar.LENGTH_SHORT));
            checkForMessages();
            return;
        }
        if (operator.getSilenced()) {
            snacks.add(new Snack("Cannot perform while silenced", Snackbar.LENGTH_SHORT));
            checkForMessages();
            return;
        }
        if (!blockListContainsId(blockedIDs, id)) {
            blockedIDs.add(new Block(id, handle));
            databaseReference.child("autoSkip").child(operator.getUser_id()).child(id).removeValue();
            RadioService.databaseReference.child("volumes").child(RadioService.operator.getUser_id()).child(id).removeValue();
            update_block_list(blockedIDs, "blockedIDs");
            removeAllOf(id, false, 0);
            if (toast) snacks.add(new Snack("Radio blocked From " + handle, Snackbar.LENGTH_SHORT));
        } else {
            if (toast) snacks.add(new Snack("Already blocked", Snackbar.LENGTH_SHORT));
        }
        checkForMessages();
    }

    void blockNewPhoto(final String id, final String handle, boolean toast) {
        if (photoIDs.size() >= blockLimit && !operator.getAdmin()) {
            snacks.add(new Snack("Block List Full!", Snackbar.LENGTH_SHORT));
            checkForMessages();
            return;
        }
        if (operator.getSilenced()) {
            snacks.add(new Snack("Cannot perform while silenced", Snackbar.LENGTH_SHORT));
            checkForMessages();
            return;
        }
        if (!blockListContainsId(photoIDs, id)) {
            photoIDs.add(new Block(id, handle));
            update_block_list(photoIDs, "photoIDs");
            if (toast)
                snacks.add(new Snack("Photos blocked From " + handle, Snackbar.LENGTH_SHORT));
        } else {
            if (toast) snacks.add(new Snack("Already blocked", Snackbar.LENGTH_SHORT));
        }
        checkForMessages();
    }

    void blockNewText(final String id, final String handle, boolean toast) {
        if (textIDs.size() >= blockLimit && !operator.getAdmin()) {
            snacks.add(new Snack("Block List Full!", Snackbar.LENGTH_SHORT));
            checkForMessages();
            return;
        }
        if (operator.getSilenced()) {
            snacks.add(new Snack("Cannot perform while silenced", Snackbar.LENGTH_SHORT));
            checkForMessages();
            return;
        }
        if (!blockListContainsId(textIDs, id)) {
            textIDs.add(new Block(id, handle));
            update_block_list(textIDs, "textIDs");
            if (MI != null) MI.updateUserList(userList);
            if (toast)
                snacks.add(new Snack("Messages blocked From " + handle, Snackbar.LENGTH_SHORT));
        } else {
            if (toast) snacks.add(new Snack("Already blocked " + handle, Snackbar.LENGTH_SHORT));
        }
        checkForMessages();
    }

    void checkForMessages() {
        if (recording || occupied.get() || !phoneIdle || MI == null) return;
        if (!Objects.equals(chat.get(), "0")) {
            MI.displayChat(null, false, false);
        }
        if (!photos.isEmpty()) {
            MI.displayPhoto(photos.get(0));
            photos.remove(0);
            return;
        }
        if (!messages.isEmpty()) {
            Message message = messages.get(0);
            messages.remove(0);
            MI.displayPm(new String[]{message.getUser_id(), message.getHandle(), message.getMessageText(), message.getRank(), message.getProfileLink()});
        }
        if (!snacks.isEmpty()) {
            Snack snack = snacks.get(0);
            MI.showSnack(snack);
        }
    }

    boolean updateMute() {
        return mute;
    }

    private void skip() {
        if (inbounds.isEmpty()) return;
        sp.play(skip, .1f, .1f, 1, 0, 1f);
        removeZeros();
    }

    private void removeAllOf(final String id, final boolean toast, final int limit) {
        if (inbounds.isEmpty()) return;
        boolean isOperator = inbounds.get(0).getUser_id().equals(id);
        Callable<Boolean> callable = () -> {
            if (isOperator && !paused) player.pause();
            int size = inbounds.size();
            if (size > 1) {
                List<Inbound> indexList = new ArrayList<>();
                for (int i = size - 1; i >= 1; i--) {
                    final Inbound entry = inbounds.get(i);
                    if (entry.getUser_id().equals(id)) {
                        indexList.add(entry);
                    }
                }
                indexList.sort(Comparator.comparingLong(Inbound::getStamp));
                if (!indexList.isEmpty()) {
                    List<Inbound> deletionArray = new ArrayList<>();
                    if (indexList.size() > limit - 1 && limit != 0) {
                        for (int i = 0; i <= limit - 1; i++) {
                            deletionArray.add(indexList.get(i));
                        }
                    } else deletionArray.addAll(indexList);
                    int x = deletionArray.size();
                    if (x > 1 && toast) {
                        snacks.add(new Snack("Purged " + x, Snackbar.LENGTH_SHORT));
                        checkForMessages();
                    }
                    for (Inbound inboud : deletionArray) {
                        if (!new File(Utils.formatLocalAudioFileLocation(saveDirectory, inboud.getUser_id(), inboud.getStamp())).delete())
                            Log.e("removeAllOf()", "Failed to delete file!");
                        inbounds.remove(inboud);
                    }
                }
            }
            return isOperator;
        };
        if (ExecutorUtils.getFutureBoolean(executor, callable)) removeZeros();
        else updateDisplay();
    }

    private void purge() {
        if (inbounds.isEmpty()) return;
        sp.play(purge, .1f, .1f, 1, 0, 1f);
        removeAllOf(inbounds.get(0).getUser_id(), true, operator.getPurgeLimit());
    }

    private void emptyPlayer(boolean shutdown) {
        player.reset();
        playing = false;
        inbounds = new ArrayList<>();
        if (!shutdown) updateDisplay();
    }

    private void sendPrivate(final String messageTxt, final String reciever) {
        final Message message = new Message(operator.getUser_id(), operator.getHandle(), messageTxt, operator.getRank(), operator.getProfileLink());
        UtilsKKt.sendControl(reciever, new ControlObject(ControlCode.PRIVATE_MESSAGE, message));
        final Map<String, Object> claims = new HashMap<>();
        claims.put("to", reciever);
        claims.put("text", messageTxt);
        claims.put("from", operator.getUser_id());
        claims.put("handle", operator.getHandle());
        claims.put("rank", operator.getRank());
        claims.put("silenced", String.valueOf(operator.getSilenced()));
        claims.put("profileLink", operator.getProfileLink());
        okUtil.call("user_send_pm.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    if (response.isSuccessful()) {
                        handler.post(() -> {
                            if (MI != null) {
                                if (Objects.equals(chat.get(), "0")) {
                                    snacks.add(new Snack("Message Sent", Snackbar.LENGTH_SHORT));
                                    checkForMessages();
                                } else MI.displayChat(null, false, false);
                            }
                        });
                    }
                }
            }
        });
    }

    String[] queCheck(int index) {
        if (!inbounds.isEmpty()) {
            if (index == 0) return new String[]{"Clear Queue", "", "", "", "f", "none"};
            int size = inbounds.size();
            Inbound object;
            final int check = size - index;
            if (index == size || check < 0) object = inbounds.get(0);
            else object = inbounds.get(check);
            return new String[]{object.getHandle(), object.getCarrier(), object.getTown(), object.getTitle(), object.getRank(), object.getProfileLink()};
        }
        return new String[]{onlineStatus, "", "", "", "", "none"};
    }

    long queStamp(int index) {
        if (!inbounds.isEmpty() && index != 0) {
            int size = inbounds.size();
            Inbound object;
            final int check = size - index;
            if (index == size || check < 0) object = inbounds.get(0);
            else object = inbounds.get(check);
            return object.getStamp();
        }
        return 0;
    }

    private void mute() {
        if (playing && !inbounds.isEmpty()) {
            if (mute) player.setVolume(0f, 0f);
            else {
                float volume = scaleVolume(returnUserVolume(inbounds.get(0).getUser_id()));
                player.setVolume(volume, volume);
            }
        }
        sendBroadcast(new Intent("setMute").setPackage("com.cb3g.channel19").putExtra("data", mute));
        notification();
    }

    private void pause_playback() {
        databaseReference.child("paused").child(operator.getUser_id()).setValue(Instant.now().getEpochSecond());
        if (paused) return;
        paused = true;
        if (playing) player.pause();
        if (MI != null) MI.resumeAnimation(new int[]{0, 0});
        sendBroadcast(new Intent("switchToPlay").setPackage("com.cb3g.channel19"));
        updateDisplay();
        if (MI != null) {
            snacks.add(new Snack("Paused", Snackbar.LENGTH_SHORT));
            checkForMessages();
        }
    }

    void resumePlayback() {
        if (!inbounds.isEmpty()) {
            Inbound q = inbounds.get(0);
            inbounds.remove(0);
            inbounds.sort(Comparator.comparingLong(Inbound::getStamp));
            inbounds.add(0, q);
            Log.i("logging", "sorted");
        }
        databaseReference.child("paused").child(operator.getUser_id()).removeValue();
        paused = false;
        if (playing) {
            player.start();
            sendBroadcast(new Intent("nineteenUpdateMax").putExtra("data", new int[]{player.getDuration(), player.getCurrentPosition()}));
        } else {
            sendBroadcast(new Intent("play"));
        }
        checkForMessages();
        sendBroadcast(new Intent("switchToPause"));
        updateDisplay();
    }

    boolean returnedPaused() {
        return paused;
    }

    private void downloadImage(final String link) throws MalformedURLException {
        if (MI != null) MI.showSnack(new Snack("Image Downloading", Snackbar.LENGTH_SHORT));
        String fileName = FilenameUtils.getName(new URL(link).getPath());
        fileName = fileName.replace("%", "");
        fileName = fileName.replace("reservoir", "");
        fileName = fileName.replace("photos", "");
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(link));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }

    private void checkFlagOut() {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", operator.getUser_id());
        okUtil.call("user_check_flags.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                LOG.e("user_check_flags.php" + e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull final Response response) {
                if (response.isSuccessful()) {
                    try (response; response; response) {
                        assert response.body() != null;
                        int flags = Integer.parseInt(response.body().string());
                        if (flags >= 20 && !operator.getAdmin()) {
                            Utils.usersInChannel(new Callback() {
                                @Override
                                public void onFailure(@NonNull Call call, @NonNull IOException e) {

                                }

                                @Override
                                public void onResponse(@NonNull Call call, @NonNull Response response) {
                                    try (response) {
                                        if (response.isSuccessful()) {
                                            assert response.body() != null;
                                            final String data = response.body().string();
                                            final ArrayList<String> ids = new Gson().fromJson(data, new TypeToken<ArrayList<String>>() {
                                            }.getType());
                                            if (!ids.isEmpty())
                                                UtilsKKt.sendControl(ids, new ControlObject(ControlCode.ALERT, operator.getHandle() + " was flagged out!"));
                                        }
                                    } catch (IOException e) {
                                        if (e.getMessage() != null)
                                            Log.e("checkFlagOut() - user_in_channel.php", e.getMessage());
                                    }
                                }
                            });

                            stopSelf();
                        }
                    } catch (IOException e) {
                        LOG.e("user_check_flags.php", e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (MI != null && recording) MI.stopRecorder(false);
                snacks.add(new Snack("Key-up interrupted!", Snackbar.LENGTH_SHORT));
                checkForMessages();
            }
        }
    }

    private void registerCallStateListener() {
        if (!callStateListenerRegistered) {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                telephonyManager.registerTelephonyCallback(getMainExecutor(), callStateListener);
                callStateListenerRegistered = true;
            }
        }
    }

    private static abstract class CallStateListener extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        @Override
        abstract public void onCallStateChanged(int state);
    }

    // Handle call state change
    private boolean onCall = false;
    private final CallStateListener callStateListener = new CallStateListener() {
        @Override
        public void onCallStateChanged(int state) {
            switch (state) {
                case TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> {
                    if (phoneIdle) {
                        phoneIdle = false;
                        if (MI != null) MI.stopRecorder(false);
                        if (!paused) {
                            onCall = true;
                            pause_playback();
                            databaseReference.child("onCall").child(operator.getUser_id()).setValue(System.currentTimeMillis());
                        }
                    }
                }
                case TelephonyManager.CALL_STATE_IDLE -> {
                    phoneIdle = true;
                    if (onCall) {
                        onCall = false;
                        resumePlayback();
                        databaseReference.child("onCall").child(operator.getUser_id()).removeValue();
                    }
                }
            }
        }
    };

    class LocalBinder extends Binder {
        RadioService getService() {
            return RadioService.this;
        }
    }


}

