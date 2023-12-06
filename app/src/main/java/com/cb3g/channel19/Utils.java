package com.cb3g.channel19;


import android.Manifest;
import android.app.Activity;
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
import java.util.Date;
import java.util.Locale;

class Utils {

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
            return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
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

    static boolean permissionsAccepted(Context context, String permission) {
        return (ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED);
    }

    static void showRewardAd(Context context, String userId, int tokens, boolean chosen) {
        Intent rewardIntent = new Intent(context, ActivityReward.class);
        rewardIntent.putExtra("userId", userId);
        rewardIntent.putExtra("tokens", tokens);
        rewardIntent.putExtra("chosen", chosen);
        context.startActivity(rewardIntent);
    }

    static String formatAudioFileUrl(String url, String userId, long messageNumber) {
        return url + "users/user-" + userId + "/" + messageNumber + ".m4a";
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

    static DatabaseReference getFree(String userId) {
        return getDatabase().getReference().child("free").child(userId);
    }

    static DatabaseReference getShop() {
        return getDatabase().getReference().child("rewards");
    }

    public static void logJSON(String json) {
        int i = 3000;
        while (json.length() > i) {
            Logger.INSTANCE.i(json.substring(0, i));
            json = json.substring(i);
        }
        Logger.INSTANCE.i(json.substring(0, i));
    }

    public static void dLong(String theMsg) {
        final int MAX_INDEX = 4000;
        final int MIN_INDEX = 3000;

        // String to be logged is longer than the max...
        if (theMsg.length() > MAX_INDEX) {
            String theSubstring = theMsg.substring(0, MAX_INDEX);
            int theIndex = MAX_INDEX;

            // Try to find a substring break at a line end.
            theIndex = theSubstring.lastIndexOf('\n');
            if (theIndex >= MIN_INDEX) {
                theSubstring = theSubstring.substring(0, theIndex);
            } else {
                theIndex = MAX_INDEX;
            }

            // Log the substring.
            Logger.INSTANCE.i(theSubstring);

            // Recursively log the remainder.
            dLong(theMsg.substring(theIndex));
        }

        // String to be logged is shorter than the max...
        else {
            Logger.INSTANCE.i(theMsg);
        }
    }

    public static Bitmap returnVideoScreenShot(String fileLocation) {
        MediaMetadataRetriever ret = new MediaMetadataRetriever();
        ret.setDataSource(fileLocation);
        return ret.getFrameAtTime();
    }

    public static int calculateNoOfColumns(Context context, float columnWidthDp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        return (int) (screenWidthDp / columnWidthDp + 0.5);
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


    static String formatInt(int count) {
        return NumberFormat.getNumberInstance(Locale.US).format(count);
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

    static String formatDoubleToCurrency(double value) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        return formatter.format(value);
    }

    static String formatDiff(String text, Duration duration) {
        String response = "";
        if (!text.equals("")) {
            if (duration.toDays() == 1) response = "yesterday";
            else if (duration.toDays() > 1) response = duration.toDays() + " days ago";
            else {
                if (duration.toHours() == 1) response = duration.toHours() + " hour ago";
                else if (duration.toMinutes() > 120) response = duration.toHours() + " hours ago";
                else {
                    if (duration.toMinutes() == 1) response = duration.toMinutes() + "  min ago";
                    else if (duration.toMinutes() > 1)
                        response = duration.toMinutes() + "  mins ago";
                    else response = text + "s";
                }
            }
        }
        return response;
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
        String response = "";
        int days = 0;
        int hours = (int) duration.toHours();
        int minutes = (int) duration.toMinutes();
        int seconds = (int) duration.toMillis() / 1000;
        if (hours > 24) {
            days = hours / 24;
            response = days + " day";
            if (days == 1) response += response + " ";
            else response += response + "s ";
            hours = hours - days * 24;
        }
        if (minutes > 60) minutes = minutes - (hours * 60);
        if (hours > 0) {
            if (hours == 1) response = hours + " hr";
            else response = hours + " hrs";
        }
        if (minutes > 0) {
            response += " ";
            if (minutes == 1) response += minutes + " min";
            else response += minutes + " mins";
        } else {
            if (seconds == 1) response = "1 sec";
            else response = seconds + " secs";
        }
        return response;
    }

    static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format(Locale.getDefault(), "%d:%02d", absSeconds / 3600, (absSeconds % 3600) / 60);
        return seconds < 0 ? "-" + positive : positive;
    }

    static String toTime(long value) {
        DateFormat df2 = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return df2.format(new Date(new Timestamp(value).getTime()));
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

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;
        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }
        return phrase.toString();
    }

    static void returnBitmapFromUrl(Activity activity, String url) {
        Glide.with(activity)
                .asBitmap()
                .load(url)
                .dontTransform()
                .apply(RadioService.profileOptions)
                .thumbnail(0.1f)
                .into(new Target<Bitmap>() {
                    @Override
                    public void onLoadStarted(@Nullable Drawable placeholder) {
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    }

                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        final float scale = activity.getResources().getDisplayMetrics().density;
                        int pixels = (int) (50 * scale + 0.5f);
                        Bitmap bitmap = Bitmap.createScaledBitmap(resource, pixels, pixels, true);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }

                    @Override
                    public void getSize(@NonNull SizeReadyCallback cb) {
                    }

                    @Override
                    public void removeCallback(@NonNull SizeReadyCallback cb) {
                    }

                    @Nullable
                    @Override
                    public Request getRequest() {
                        return null;
                    }

                    @Override
                    public void setRequest(@Nullable Request request) {
                    }

                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onStop() {
                    }

                    @Override
                    public void onDestroy() {
                    }
                });
    }

}
