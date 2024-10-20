package com.cb3g.channel19;


import static com.cb3g.channel19.RadioService.operator;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.threeten.bp.Duration;
import org.threeten.bp.Instant;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Callback;

class Utils {

    static void clickSound(Context context){
        context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
    }

    static void usersInChannel(final Callback callback){
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", operator.getUser_id());
        claims.put("handle", operator.getHandle());
        if (operator.getChannel()!= null) claims.put("channel", operator.getChannel().getChannel());
        new OkUtil().call("user_in_channel.php", claims, callback);
    }

    static DatabaseReference control() {
        return getDatabase().getReference().child("controlling");
    }

    static String getKey() {
        return getDatabase().getReference().push().getKey();
    }

    static ColorFilter colorFilter(int color) {
        return new BlendModeColorFilter(color, BlendMode.SRC_ATOP);
    }

    static boolean alreadyFlagged(String userId) {
        return RadioService.flaggedIds.contains(userId);
    }

    static boolean alreadySaluted(String userId) {
        return RadioService.salutedIds.contains(userId);
    }

    static void launchUrl(Context context, String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        context.startActivity(i);
    }

    static Bitmap fetchImage(String url) {
        Bitmap image = null;
        try {
            InputStream inputStream = new URL(url).openStream();
            image = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            Log.e("image", e.getMessage());
        }
        return image;
    }

    static String[] getStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
    }

    static String[] getLocationPermissions() {
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    }

    static String[] getBluetoothPermissions() {
        return new String[]{Manifest.permission.BLUETOOTH_CONNECT};
    }

    static String[] getAudioPermissions() {
        return new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE};
    }

    static void requestPermission(Activity activity, String[] permissionSet, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissionSet, requestCode);
    }

    static boolean permissionsAccepted(Context context, String[] permissionSet) {
        for (String permission : permissionSet) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    static void showRewardAd(Context context, String userId, int tokens, boolean chosen) {
        Intent rewardIntent = new Intent(context, ActivityReward.class);
        rewardIntent.putExtra("userId", userId);
        rewardIntent.putExtra("tokens", tokens);
        rewardIntent.putExtra("chosen", chosen);
        context.startActivity(rewardIntent);
    }

    static String formatLocalAudioFileLocation(String directory, long stamp) {
        return directory + stamp + ".m4a";
    }

    static String formatLocalAudioFileLocation(String directory, String userId, long stamp) {
        return directory + userId + "-" + stamp + ".m4a";
    }

    static String parseRankUrl(String rank) {
        return RadioService.SITE_URL + "drawables/stars/" + "star" + rank + ".png";
    }

    static boolean serviceAlive(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RadioService.class.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }

    private static FirebaseDatabase mDatabase;

    static FirebaseDatabase getDatabase() {
        if (mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance();
            //mDatabase.setPersistenceEnabled(true);
        }
        return mDatabase;
    }

    static DatabaseReference getTokens(String userId) {
        return getDatabase().getReference().child("tokens").child(userId);
    }

    static DatabaseReference getShop() {
        return getDatabase().getReference().child("rewards");
    }

    public static void vibrate(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
    }

    public static int getScreenWidth(@NonNull Activity activity) {
        WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
        Insets insets = windowMetrics.getWindowInsets()
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
        return windowMetrics.getBounds().width() - insets.left - insets.right;
    }

    static void showKeyboard(Context context, View view) {
        InputMethodManager methodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        view.requestFocus();
        view.postDelayed(() -> methodManager.showSoftInput(view, 0), 200);
    }

    static void hideKeyboard(Context context, View view) {
        InputMethodManager methodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        view.requestFocus();
        view.postDelayed(() -> methodManager.hideSoftInputFromWindow(view.getWindowToken(), 0), 200);
    }

    static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    static double round(double value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    static String formatDiff(Duration duration, boolean justNow) {
        String response = "";
        if (justNow) response = "just now";
        if (duration.toDays() == 1) response = "yesterday";
        else if (duration.toDays() > 1) response = duration.toDays() + " days ago";
        else {
            if (duration.toMinutes() > 120) response = duration.toHours() + " hours ago";
            else {
                if (duration.toMinutes() == 1) response = duration.toMinutes() + "  min ago";
                else if (duration.toMinutes() > 1) response = duration.toMinutes() + "  mins ago";
            }
        }
        return response;
    }

    static String timeOnline(Duration duration) {
        int hours = (int) duration.toHours();
        int minutes = (int) duration.toMinutes();
        if (hours > 23) {
            final int days = hours / 24;
            if (days == 1 || hours == 24) return "One day ";
            return days + " days ";
        }
        if (hours > 0) {
            if (hours == 1) return "One hour ";
            return hours + " hours ";
        }
        if (minutes == 1) return "One min ";
        return minutes + " mins";
    }

    static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format(Locale.getDefault(), "%d:%02d", absSeconds / 3600, (absSeconds % 3600) / 60);
        return seconds < 0 ? "-" + positive : positive;
    }

    static Duration timeDifferance(long then) {
        return Duration.between(Instant.ofEpochSecond(then), Instant.now());
    }

    static Duration timeDifferance(long then, Instant now) {
        return Duration.between(Instant.ofEpochSecond(then), now);
    }

    static String showElapsed(long stamp, boolean justNow) {
        return formatDiff(timeDifferance(stamp), justNow);
    }

    static String showElapsed(long stamp) {
        return formatDiff(timeDifferance(stamp), false);
    }

    static String showElapsed(long stamp, Instant now) {
        return formatDuration(timeDifferance(stamp, now));
    }

    static long UTC() {
        return Instant.now().getEpochSecond();
    }

}
