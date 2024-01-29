package com.cb3g.channel19;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
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

import com.bumptech.glide.request.RequestOptions;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("SpellCheckingInspection")
public class RadioService extends Service implements ValueEventListener {
    static final OkHttpClient client = new OkHttpClient();
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
    static User operator = new User();
    static Map<String, Object> header = new HashMap<>();
    static List<FBentry> ghostUsers = new ArrayList<>();
    static List<FBentry> pausedUsers = new ArrayList<>();
    static List<String> silencedUsers = new ArrayList<>();
    static List<String> autoSkip = new ArrayList<>();
    static List<UserVolume> volumes = new ArrayList<>();
    static List<Coordinates> coordinates = new ArrayList<>();
    static Gson gson = new Gson();
    static List<UserListEntry> users = new ArrayList<>();
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
    private long enterStamp = Instant.now().getEpochSecond();
    private final TelephonyReceiver telephonyReceiver = new TelephonyReceiver();
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case "token"://TODO: update token on server
                    break;
                case "locationUpdate":
                    Log.i("logging", "location broadcast received");
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
                case "clear":
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
                case "alert":
                    if (blockListContainsId(blockedIDs, intent.getStringExtra("userId")) || MI == null)
                        return;
                    snacks.add(gson.fromJson(intent.getStringExtra("data"), Snack.class));
                    checkForMessages();
                    break;
                case "snack":
                    snacks.add(gson.fromJson(intent.getStringExtra("data"), Snack.class));
                    checkForMessages();
                    break;
                case "background":
                    try {
                        JSONObject backgrounds = new JSONObject(Objects.requireNonNull(intent.getStringExtra("data")));
                        settings.edit().putString("main_backdrop", backgrounds.getString("one")).apply();
                        settings.edit().putString("settings_backdrop", backgrounds.getString("two")).apply();
                        if (MI != null) MI.changeBackground(backgrounds.getString("one"));
                    } catch (JSONException e) {
                        LOG.e(e.getMessage());
                    }
                    break;
                case "checkForMessages":
                    checkForMessages();
                    break;
                case "fetch_users":
                    get_users_on_channel();
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
                case "nineteenPulse":
                    final String data = Jwts.builder().setHeader(header).claim("userId", operator.getUser_id()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
                    client.newCall(new Request.Builder().url(SITE_URL + "user_pulse_response.php").post(new FormBody.Builder().add("data", data).build()).build()).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) {
                        }
                    });
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
                case "nineteenPause":
                    if (!paused) pause_playback();
                    break;
                case "nineteenPhotoReceive":
                    final Photo photo = gson.fromJson(intent.getStringExtra("data"), Photo.class);
                    if (RadioService.blockListContainsId(photoIDs, photo.getSenderId())) return;
                    switch (photo.getMass()) {
                        case 0 -> { //private photo
                            if (settings.getBoolean("photos", true)) {
                                if (Objects.equals(chat.get(), photo.getSenderId())) {
                                    if (MI != null) MI.displayChat(null, true, false);
                                    else sendBroadcast(new Intent("nineteenChatSound"));
                                } else {
                                    if (!recording) sp.play(glass, .1f, .1f, 1, 0, 1f);
                                    photos.add(photo);
                                }
                            }
                        }
                        case 1 -> { //mass
                            if (!recording && MI != null) sp.play(glass, .1f, .1f, 1, 0, 1f);
                            if (settings.getBoolean("photos", true) && !paused) photos.add(photo);
                            else if (MI != null)
                                snacks.add(new Snack(photo.getHandle() + " sent you a mass photo", Snackbar.LENGTH_SHORT));
                            Utils.getDatabase().getReference().child("mass history").child(operator.getUser_id()).push().setValue(photo);
                        }
                    }

                    checkForMessages();
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
                    sp.play(mic, .1f, .1f, 1, 0, 1f);
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
                case "nineteenReceivePM":
                    Message message = gson.fromJson(intent.getStringExtra("data"), Message.class);
                    if (settings.getBoolean("pmenabled", true)) {
                        if (!RadioService.blockListContainsId(textIDs, message.getUser_id())) {
                            if (Objects.equals(chat.get(), message.getUser_id())) {
                                if (MI != null) MI.displayChat(null, true, false);
                                else sendBroadcast(new Intent("nineteenChatSound"));
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
                case "nineteenBluetoothSettingChange":
                    bluetooth = intent.getBooleanExtra("data", false);
                    settings.edit().putBoolean("bluetooth", bluetooth).apply();
                    bluetoothEnabled = headsetActive();
                    sendBroadcast(new Intent("tooth").putExtra("data", bluetoothEnabled));
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
                    sendBroadcast(new Intent("tooth").putExtra("data", bluetoothEnabled));
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
    private AudioFocusRequest focusRequest;
    private boolean prePaused = false;
    private ExecutorService executor;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ContextCompat.registerReceiver(this, telephonyReceiver, returnTelephoneFilter(), ContextCompat.RECEIVER_NOT_EXPORTED);
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
        databaseReference.child("paused").child(operator.getUser_id()).removeValue();
        databaseReference.child("keychain").addValueEventListener(this);
        databaseReference.child("blocking").addValueEventListener(this);
        databaseReference.child("ghostModeAvailible").addValueEventListener(this);
        databaseReference.child("flagsEnabled").addValueEventListener(this);
        databaseReference.child("radioShopOpen").addValueEventListener(this);
        databaseReference.child("silencing").addValueEventListener(this);
        databaseReference.child("ghost").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ghostUsers.clear();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String key = child.getKey();
                    Long stamp = child.getValue(Long.class);
                    if (key != null && stamp != null) {
                        FBentry entry = new FBentry(key, stamp);
                        ghostUsers.add(entry);
                    }
                }
                if (MI != null) MI.updateUserList();
                get_users_on_channel();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        databaseReference.child("paused").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                pausedUsers.clear();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String key = child.getKey();
                    Long stamp = child.getValue(Long.class);
                    if (key != null && stamp != null) {
                        FBentry entry = new FBentry(key, stamp);
                        if (isInChannel(entry.getUserId())) pausedUsers.add(entry);
                    }
                }
                if (MI != null) MI.updateUserList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        databaseReference.child("silenced").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                silencedUsers.clear();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    silencedUsers.add(child.getKey());
                }
                boolean silenced = false;
                for (String user : silencedUsers) {
                    if (user.equals(RadioService.operator.getUser_id())) silenced = true;
                }
                operator.setSilenced(silenced);
                if (MI != null) {
                    MI.updateDisplay(getlatest(), inbounds.size(), getDuration(), paused, poor, getlatestStamp());
                    itterateSilenced();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        databaseReference.child("volumes").child(operator.getUser_id()).addChildEventListener(new ChildEventListener() {
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
        });
        databaseReference.child("blockedFromReservoir").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> ids = new ArrayList<>();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    ids.add(child.getKey());
                }
                operator.setBlockedFromReservoir(ids.contains(operator.getUser_id()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        databaseReference.child("disableProfile").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> ids = new ArrayList<>();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    ids.add(child.getKey());
                }
                operator.setDisableProfile(ids.contains(operator.getUser_id()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        databaseReference.child("hinderTexts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> ids = new ArrayList<>();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    ids.add(child.getKey());
                }
                operator.setHinderTexts(ids.contains(operator.getUser_id()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        databaseReference.child("hinderPhotos").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> ids = new ArrayList<>();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    ids.add(child.getKey());
                }
                operator.setHinderPhotos(ids.contains(operator.getUser_id()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        databaseReference.child("autoSkip").child(operator.getUser_id()).removeValue();
        databaseReference.child("autoSkip").child(operator.getUser_id()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                autoSkip.clear();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    autoSkip.add(child.getKey());
                }
                if (MI != null) MI.updateUserList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        pauseLimit = settings.getInt("pauseLimit", 150);
        bluetooth = settings.getBoolean("bluetooth", true);
        onlineStatus = "Online";
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = manager.getAdapter();
        header.put("typ", Header.JWT_TYPE);
        bluetoothEnabled = headsetActive();
        sendBroadcast(new Intent("tooth").putExtra("data", bluetoothEnabled));
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
                        else sendBroadcast(new Intent("play"));
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
        get_users_on_channel();
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
        AudioAttributes focusAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE).setAudioAttributes(focusAttributes).setAcceptsDelayedFocusGain(false).setOnAudioFocusChangeListener(i -> {
        }).build();
    }

    void registerDefaultNetworkCallback() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {

                connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull android.net.Network network) {
                        super.onAvailable(network);
                        makePoor(false);
                    }

                    @Override
                    public void onLost(@NonNull android.net.Network network) {
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
                if (MI != null) MI.updateQueue(inbounds.size(), paused, poor);
            } else sendBroadcast(new Intent("play"));
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
                else sendBroadcast(new Intent("play"));
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
                file.delete();
                final String data = Jwts.builder().setHeader(header).claim("userId", operator.getUser_id()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
                final Request request = new Request.Builder().url(SITE_URL + "user_key_count.php").post(new FormBody.Builder().add("data", data).build()).build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        try (response) {
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

    public void listenForCoordinates(boolean listen) {
        if (listen) {
            if (operator.getLocationEnabled().get()) {
                RadioService.databaseReference.child("locations").addValueEventListener(this);
            }
        } else {
            RadioService.databaseReference.child("locations").removeEventListener(this);
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
            case "locations" -> {
                coordinates.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    coordinates.add(snapshot.getValue(Coordinates.class));
                }
            }
            case "blocking" ->
                    operator.setBlocking(Boolean.TRUE.equals(dataSnapshot.getValue(Boolean.class)));
            case "flagsEnabled" ->
                    operator.setFlagsEnabled(Boolean.TRUE.equals(dataSnapshot.getValue(Boolean.class)));
            case "ghostModeAvailible" ->
                    operator.setGhostModeAvailible(Boolean.TRUE.equals(dataSnapshot.getValue(Boolean.class)));
            case "radioShopOpen" ->
                    operator.setRadioShopOpen(Boolean.TRUE.equals(dataSnapshot.getValue(Boolean.class)));
            case "silencing" ->
                    operator.setSilencing(Boolean.TRUE.equals(dataSnapshot.getValue(Boolean.class)));
            case "keychain" -> {
                operator.setKey(Objects.requireNonNull(dataSnapshot.getValue(String.class)));
                settings.edit().putString("keychain", operator.getKey()).apply();
            }
        }
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
                                        sendBroadcast(new Intent("locationUpdate").putExtra("data", locationString).setPackage("com.cb3g.channel19"));
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

    private boolean isInChannel(String userId) {
        for (UserListEntry user : users) {
            if (user.getUser_id().equals(userId)) return true;
        }
        return false;
    }

    private int returnDistance(Location from, double latitude, double longitude) {
        Location des = new Location("providername");
        des.setLatitude(latitude);
        des.setLongitude(longitude);
        return (int) (0.621371 * from.distanceTo(des)) / 1000;
    }

    private void get_users_on_channel() {
        if (operator.getChannel() == null) return;
        final String data = Jwts.builder().setHeader(header).claim("userId", operator.getUser_id()).claim("channel", operator.getChannel().getChannel()).claim("language", language).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        client.newCall(new Request.Builder().url(SITE_URL + "user_list.php").post(new FormBody.Builder().add("data", data).build()).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                LOG.e("get_users_on_channel IOException " + e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull final Response response) {
                if (response.isSuccessful()) {
                    try (response) {
                        assert response.body() != null;
                        users = itterateSilenced(returnFilteredList(returnUserListObjectFromJson(response.body().string())));
                        handler.post(() -> {
                            if (MI != null) MI.updateUserList();
                        });
                    } catch (IOException e) {
                        LOG.e("get_users_on_channel", e.getMessage());
                    }
                }
            }
        });
    }

    void recording(final boolean recording) {
        if (MI != null) {
            RadioService.recording = recording;
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

    public void useSco(final boolean sco) {
        if (sco) {
            audioManager.setBluetoothScoOn(true);
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.startBluetoothSco();
        } else {
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
    }

    private void pre_key_up() {
        audioManager.requestAudioFocus(focusRequest);
        if (playing) player.pause();
    }

    public void post_key_up() {
        audioManager.abandonAudioFocusRequest(focusRequest);
        if (playing) {
            if (!paused) player.start();
        } else {
            sendBroadcast(new Intent("play"));
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

    UserListEntry returnTalkerEntry() {
        if (!inbounds.isEmpty()) {
            Inbound inbound = inbounds.get(0);
            String currentId = inbound.getUser_id();
            for (UserListEntry user : users) {
                if (user.getUser_id().equals(currentId)) return user;
            }
            UserListEntry entry = new UserListEntry();
            entry.setUser_id(currentId);
            entry.setRadio_hanlde(inbound.getHandle());
            entry.setProfileLink(inbound.getProfileLink());
            entry.setSilenced(silencedUsers.contains(currentId));
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
        get_users_on_channel();
        if (operator.getInvisible() || userIsGhost(operator.getUser_id())) return;
        final String data = Jwts.builder().setHeader(header).claim("userId", operator.getUser_id()).claim("handle", operator.getHandle()).claim("channel", channel.getChannel()).claim("language", language).claim("profileLink", operator.getProfileLink()).claim("channelName", channel.getChannel_name()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        client.newCall(new Request.Builder().url(SITE_URL + "user_entered_channel.php").post(new FormBody.Builder().add("data", data).build()).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
            }
        });
    }

    private boolean isUpdatedVolumeUserZero(String id) {
        if (!playing) return false;
        if (inbounds.isEmpty()) return false;
        return inbounds.get(0).getUser_id().equals(id);
    }

    private List<UserListEntry> itterateSilenced(@NonNull List<UserListEntry> users) {
        for (int i = 0; i < users.size(); i++) {
            users.get(i).setSilenced(silencedUsers.contains(users.get(i).getUser_id()));
        }
        return users;
    }

    public void itterateSilenced() {
        for (int i = 0; i < users.size(); i++) {
            users.get(i).setSilenced(silencedUsers.contains(users.get(i).getUser_id()));
        }
        if (MI != null) MI.updateUserList();
    }

    private void update_block_list(final List<Block> blockList, final String field) {
        settings.edit().putString(field, gson.toJson(blockList)).apply();
        final String data = Jwts.builder().setHeader(header).claim("userId", operator.getUser_id()).claim("list", gson.toJson(blockList)).claim("field", field).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_update_block_list.php").post(new FormBody.Builder().add("data", data).build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
            }
        });
    }

    private List<UserListEntry> returnFilteredList(final List<UserListEntry> inbound) {
        if (operator.getAdmin()) return inbound;
        List<UserListEntry> returnedList = new ArrayList<>();
        for (UserListEntry child : inbound) {
            if (!blockListContainsId(blockedIDs, child.getUser_id()) && !userIsGhost(child.getUser_id()))
                returnedList.add(child);
        }
        return returnedList;
    }

    static boolean userIsGhost(String id) {
        for (FBentry entry : ghostUsers) {
            if (entry.getUserId().equals(id)) return true;
        }
        return false;
    }

    public String returnLocation() {
        return operator.getUserLocationString();
    }

    private void uploadLocation(final String location_string) {
        final String data = Jwts.builder().setHeader(header).claim("userId", operator.getUser_id()).claim("location", location_string).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_change_location.php").post(new FormBody.Builder().add("data", data).build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        File directory = new File(saveDirectory);
        File[] files = directory.listFiles(file -> (file.getPath().endsWith(".m4a")));
        executor.execute(() -> {
            assert files != null;
            for (File file : files) {
                file.delete();
            }
            ExecutorUtils.shutdown(executor);
        });
        logOut();
    }

    public void logOut() {
        removeListeners();
        final String data = Jwts.builder().setHeader(header).claim("userId", operator.getUser_id()).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_log_out.php").post(new FormBody.Builder().add("data", data).build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
            }
        });
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
            sendBroadcast(new Intent(context, Channel19.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, WIDGETS).setPackage("com.cb3g.channel19"));
        unregisterReceiver(receiver);
        unregisterReceiver(telephonyReceiver);
        settings.edit().putBoolean("exiting", true).apply();
        sendBroadcast(new Intent("exitChannelNineTeen").setPackage("com.cb3g.channel19"));
    }

    private void removeListeners() {
        if (operator.getChannel() != null)
            databaseReference.child("audio").child(operator.getChannel().getChannel_name()).removeEventListener(audioListener);
        databaseReference.child("blocking").removeEventListener(this);
        databaseReference.child("ghostModeAvailible").removeEventListener(this);
        databaseReference.child("flagsEnabled").removeEventListener(this);
        databaseReference.child("radioShopOpen").removeEventListener(this);
        databaseReference.child("silencing").removeEventListener(this);
        databaseReference.child("keychain").removeEventListener(this);
        databaseReference.child("autoSkip").child(operator.getUser_id()).removeValue();
        databaseReference.child("paused").child(operator.getUser_id()).removeValue();
        listenForCoordinates(false);
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
    private List<UserListEntry> returnUserListObjectFromJson(String json) {
        try {
            List<UserListEntry> list = gson.fromJson(json, new TypeToken<List<UserListEntry>>() {
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
        final String data = Jwts.builder().setHeader(header).claim("userId", id).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_info_lookup.php").post(new FormBody.Builder().add("data", data).build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    if (response.isSuccessful()) {
                        assert response.body() != null;
                        final JSONObject object = new JSONObject(response.body().string());
                        String content = object.getString("login") + "\n" + "Total Keyups: " + object.getString("email") + "\n" + "Salutes: " + object.getString("salutes") + ", Flags: " + object.getString("flags") + "\n" + "Version: " + object.getString("version_name") + " (" + object.getString("version") + ")" + "\n" + "Android Version: " + object.getString("build_number") + "\n" + "Donations: " + object.getString("donations") + "\n" + "UserId: " + object.getString("user_id");
                        final String created = object.getString("created");
                        if (!created.equals("2015-01-01 00:00:00"))
                            content += "\n" + "Created: " + created;
                        sendBroadcast(new Intent("show_result").putExtra("title", object.getString("radio_hanlde")).putExtra("content", content));
                    }
                } catch (JSONException | IOException e) {
                    LOG.e(String.valueOf(e));
                }
            }
        });
    }

    private void uploadListToDB(final String field, @NonNull final JSONArray list) {
        final String data = Jwts.builder().setHeader(header).claim("userId", operator.getUser_id()).claim("list", list.toString()).claim("field", field).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_list_update.php").post(new FormBody.Builder().add("data", data).build()).tag("none").build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                response.close();
            }
        });
    }

    private float scaleVolume(int sliderValue) {
        return (float) (1 - (Math.log(100 - sliderValue) / Math.log(100)));
    }

    private void soundPool() {
        sp = new SoundPool.Builder().build();
        glass = sp.load(this, R.raw.glass, 1);
        register = sp.load(this, R.raw.register, 1);
        interrupt = sp.load(this, R.raw.interrupt, 1); //TODO:
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
        filter.addAction("clear");
        filter.addAction("alert");
        filter.addAction("snack");
        filter.addAction("background");
        filter.addAction("checkForMessages");
        filter.addAction("fetch_users");
        filter.addAction("keyUpdate");
        filter.addAction("pauseLimitChange");
        filter.addAction("play");
        filter.addAction("savePhotoToDisk");
        filter.addAction("removeAllOf");
        filter.addAction("nineteenPulse");
        filter.addAction("fetchInformation");
        filter.addAction("nineteenScroll");
        filter.addAction("nineteenQueChange");
        filter.addAction("nineteenNewChannel");
        filter.addAction("nineteenInZone");
        filter.addAction("nineteenTransmit");
        filter.addAction("nineteenPlayPause");
        filter.addAction("nineteenPause");
        filter.addAction("nineteenMessage");
        filter.addAction("nineteenPhotoReceive");
        filter.addAction("exitChannelNineTeen");
        filter.addAction("purgeNineTeen");
        filter.addAction("muteChannelNineTeen");
        filter.addAction("nineteenSendPM");
        filter.addAction("nineteenReceivePM");
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

    @NonNull
    private IntentFilter returnTelephoneFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PHONE_STATE");
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
        sendBroadcast(new Intent(RadioService.this, Channel19.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(RadioService.this).getAppWidgetIds(new ComponentName(RadioService.this, Channel19.class))));
    }

    private void buildNotification() {
        PendingIntent skipping = PendingIntent.getBroadcast(this, 0, new Intent("nineteenSkip"), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pausing = PendingIntent.getBroadcast(this, 0, new Intent("nineteenPlayPause"), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent opench19 = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
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
            sendBroadcast(new Intent(this, Channel19.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, Channel19.class))));
        }
    }

    void flag_out(final String id) {
        if (operator.getSilenced()) return;
        final String data = Jwts.builder().setHeader(header).claim("userId", id).claim("adminId", operator.getUser_id()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_flag_out.php").post(new FormBody.Builder().add("data", data).build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    sendBroadcast(new Intent("fetch_users"));
                }
                response.close();
            }
        });
    }

    void bannUser(final String id) {
        if (operator.getSilenced()) return;
        final String data = Jwts.builder().setHeader(header).claim("userId", id).claim("adminId", operator.getUser_id()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_ban.php").post(new FormBody.Builder().add("data", data).build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) sendBroadcast(new Intent("fetch_users"));
                response.close();
            }
        });
    }

    void silence(final String id, final String handle) {
        if (operator.getAdmin()) return;
        final String data = Jwts.builder().setHeader(header).claim("userId", id).claim("userHandle", handle).claim("adminId", operator.getUser_id()).claim("adminHandle", operator.getHandle()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_silence_new.php").post(new FormBody.Builder().add("data", data).build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
            }
        });
    }

    void ghost() {
        final String data = Jwts.builder().setHeader(header).claim("userId", operator.getUser_id()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_ghost_new.php").post(new FormBody.Builder().add("data", data).build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
            }
        });
    }

    void unsilence(final String id, final String handle) {
        if (operator.getAdmin()) return;
        final String data = Jwts.builder().setHeader(header).claim("userId", id).claim("userHandle", handle).claim("adminId", operator.getUser_id()).claim("adminHandle", operator.getHandle()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_unsilence_new.php").post(new FormBody.Builder().add("data", data).build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
            }
        });
    }

    void saluteUser(final UserListEntry entry) {
        if (operator.getSilenced()) return;
        if (salutedIds.contains(entry.getUser_id()) && !operator.getAdmin()) {
            snacks.add(new Snack("Already Saluted", Snackbar.LENGTH_SHORT));
            checkForMessages();
            return;
        }
        final String data = Jwts.builder().setHeader(header).claim("userId", entry.getUser_id()).claim("handle", operator.getHandle()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_salute_new.php").post(new FormBody.Builder().add("data", data).build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    snacks.add(new Snack("Saluted " + entry.getRadio_hanlde() + "!", Snackbar.LENGTH_SHORT));
                    checkForMessages();
                    if (!operator.getAdmin()) {
                        salutedIds.add(entry.getUser_id());
                        uploadListToDB("salutedIDs", new JSONArray(salutedIds));
                    }
                }
                response.close();
            }
        });
    }

    void flagUser(UserListEntry entry) {
        final String data = Jwts.builder().setHeader(header).claim("userId", entry.getUser_id()).claim("operatorId", operator.getUser_id()).claim("handle", operator.getHandle()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_flag.php").post(new FormBody.Builder().add("data", data).build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    snacks.add(new Snack("Flagged " + entry.getRadio_hanlde() + "!", Snackbar.LENGTH_SHORT));
                    checkForMessages();
                    if (!operator.getAdmin()) {
                        flaggedIds.add(entry.getUser_id());
                        uploadListToDB("flaggedIDs", new JSONArray(flaggedIds));
                    }
                }
                response.close();
            }
        });
    }

    public void longFlagUser(UserListEntry user) {
        final String data = Jwts.builder().setHeader(header).claim("userId", user.getUser_id()).claim("operatorId", operator.getUser_id()).claim("handle", operator.getHandle()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_long_flag.php").post(new FormBody.Builder().add("data", data).build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    snacks.add(new Snack("LONG Flagged " + user.getRadio_hanlde() + "!", Snackbar.LENGTH_SHORT));
                    checkForMessages();
                    if (!operator.getAdmin()) {
                        flaggedIds.add(user.getUser_id());
                        uploadListToDB("flaggedIDs", new JSONArray(flaggedIds));
                    }
                }
                response.close();
            }
        });
    }

    public void keyUpWasInterupted(String userId) {
        Log.i("Interrupt", "Calling interrupted.php " + userId);
        final String data = Jwts.builder().setHeader(header).claim("userId", userId).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        client.newCall(new Request.Builder().url(SITE_URL + "interupted.php").post(new FormBody.Builder().add("data", data).build()).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
            }
        });
    }

    void pauseOrplay(@NonNull UserListEntry userListEntry) {
        final String data = Jwts.builder().setHeader(header).claim("userId", userListEntry.getUser_id()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_play_pause.php").post(new FormBody.Builder().add("data", data).build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                get_users_on_channel();
            }
        });
    }

    void kickUser(@NonNull UserListEntry userListEntry) {
        final String data = Jwts.builder().setHeader(header).claim("userId", userListEntry.getUser_id()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        final Request request = new Request.Builder().url(SITE_URL + "user_kick.php").post(new FormBody.Builder().add("data", data).build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                get_users_on_channel();
            }
        });
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
            if (MI != null) MI.updateUserList();
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

    @SuppressLint("MissingPermission")
    private boolean headsetActive() {
        if (bluetoothAdapter == null) return false;
        return (bluetoothAdapter.getProfileConnectionState(1) == BluetoothAdapter.STATE_CONNECTED) && bluetooth;
    }

    private void skip() {
        if (inbounds.isEmpty()) return;
        sp.play(skip, .1f, .1f, 1, 0, 1f);
        removeZeros();
    }

    @SuppressLint("StaticFieldLeak")
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
                if (indexList.size() > 0) {
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
                        new File(Utils.formatLocalAudioFileLocation(saveDirectory, inboud.getUser_id(), inboud.getStamp())).delete();
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
        final String data = Jwts.builder().setHeader(header).claim("to", reciever).claim("text", messageTxt).claim("from", operator.getUser_id()).claim("handle", operator.getHandle()).claim("rank", operator.getRank()).claim("silenced", String.valueOf(operator.getSilenced())).claim("profileLink", operator.getProfileLink()).signWith(SignatureAlgorithm.HS256, operator.getKey()).compact();
        client.newCall(new Request.Builder().url(SITE_URL + "user_send_pm.php").post(new FormBody.Builder().add("data", data).build()).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
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
                response.close();
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
        sendBroadcast(new Intent("setMute").putExtra("data", mute));
        notification();
    }

    private void pause_playback() {
        databaseReference.child("paused").child(operator.getUser_id()).setValue(Instant.now().getEpochSecond());
        if (paused) return;
        paused = true;
        if (playing) player.pause();
        if (MI != null) MI.resumeAnimation(new int[]{0, 0});
        sendBroadcast(new Intent("switchToPlay"));
        updateDisplay();
        if (MI != null) {
            snacks.add(new Snack("Paused", Snackbar.LENGTH_SHORT));
            checkForMessages();
        }
    }

    void resumePlayback() {
        databaseReference.child("paused").child(operator.getUser_id()).removeValue();
        paused = false;
        if (playing) {
            player.start();
            sendBroadcast(new Intent("nineteenUpdateMax").putExtra("data", new int[]{player.getDuration(), player.getCurrentPosition()}));
        } else sendBroadcast(new Intent("play"));
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

    class LocalBinder extends Binder {
        RadioService getService() {
            return RadioService.this;
        }
    }

    public class TelephonyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            registerCustomTelephonyCallback(context);
        }

        class CustomTelephonyCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener {
            private final CallBack mCallBack;

            public CustomTelephonyCallback(CallBack callBack) {
                mCallBack = callBack;
            }

            @Override
            public void onCallStateChanged(int state) {

                mCallBack.callStateChanged(state);

            }
        }

        public void registerCustomTelephonyCallback(Context context) {
            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            telephony.registerTelephonyCallback(context.getMainExecutor(), new CustomTelephonyCallback(state -> {
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE -> {
                        phoneIdle = true;
                        updateDisplay();
                        if (prePaused) {
                            prePaused = false;
                            resumePlayback();
                        }
                    }
                    case TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> {
                        phoneIdle = false;
                        if (MI != null && recording) MI.stopRecorder(false);
                        if (!paused) {
                            prePaused = true;
                            pause_playback();
                        }
                    }
                }

            }));


        }

    }

    interface CallBack {
        void callStateChanged(int state);
    }
}

