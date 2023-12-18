package com.cb3g.channel19;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.android.multidex.myapplication.R;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.Instant;

import java.io.IOException;
import java.util.Locale;

public class Transmitter extends Fragment implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener, View.OnLongClickListener, CheckBox.OnCheckedChangeListener {
    private final String[] recordPermissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE};
    final private AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
    final private AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
    private TextView location, handle, quetv, carrier, title;
    private int duration = 0, recordTime = 30000, queue = 0;
    private ImageView muteIv, tooth, rank;
    private ImageView ring, key, pin;
    private ImageView profile, blurr;
    private SeekBar progressBar;
    private boolean tracking = false, hold = false;
    private com.cb3g.channel19.MI MI;
    private Context context;
    private CheckBox talkback;
    private RotateAnimation rotate = null;
    private SharedPreferences settings;
    private MediaRecorder recorder = null;
    private String saveDirectory;
    private Locale locale;
    private long msgCount = System.currentTimeMillis();
    private int tutorial_count = 0, queueMax = 9;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        return inflater.inflate(R.layout.transmit, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        locale = Locale.getDefault();
        profile = view.findViewById(R.id.black_profile_picture_iv);
        blurr = view.findViewById(R.id.ma_blurr_iv);
        key = view.findViewById(R.id.key);
        pin = view.findViewById(R.id.pin);
        pin.setImageResource(R.drawable.pinned_white);
        key.setImageResource(R.drawable.center_white);
        ring = view.findViewById(R.id.ring);
        ring.setImageResource(R.drawable.ring_white);
        rank = view.findViewById(R.id.black_star_iv);
        tooth = view.findViewById(R.id.tooth);
        tooth.setImageResource(R.drawable.tooth);
        handle = view.findViewById(R.id.black_handle_tv);
        carrier = view.findViewById(R.id.black_carrier_tv);
        title = view.findViewById(R.id.countdown);
        location = view.findViewById(R.id.black_banner_tv);
        quetv = view.findViewById(R.id.quetv);
        muteIv = view.findViewById(R.id.mute);
        progressBar = view.findViewById(R.id.pbar);
        progressBar.getProgressDrawable().setColorFilter(Utils.colorFilter(Color.WHITE));
        progressBar.getThumb().setColorFilter(Utils.colorFilter(Color.WHITE));
        talkback = view.findViewById(R.id.talkback);
        settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        talkback.setChecked(settings.getBoolean("talkback", true));
        profile.setOnLongClickListener(this);
        progressBar.setMax(queueMax);
        progressBar.setOnSeekBarChangeListener(this);
        muteIv.setOnClickListener(view1 -> {
            if (MI != null) {
                if (!RadioService.mute) {
                    muteIv.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.muted_set));
                } else {
                    muteIv.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.muter));
                }
                context.sendBroadcast(new Intent("muteChannelNineTeen"));
            }
        });
        talkback.setOnCheckedChangeListener(this);
        saveDirectory = context.getCacheDir() + "/";
        ring.setOnTouchListener(this);
        fadeOut.setDuration(200);
        fadeIn.setDuration(200);
        fadeOut.setFillAfter(true);
        tutorial_count = settings.getInt("tutorial", 0);
        if (tutorial_count < 4) {
            final int margin = ((Number) (getResources().getDisplayMetrics().density * 16)).intValue();
            final RelativeLayout.LayoutParams tps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tps.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            tps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            tps.setMargins(margin, margin, margin, margin);
            tutorial_count = 0;
            final ShowcaseView showcaseView = new ShowcaseView.Builder(getActivity())
                    .setTarget(new ViewTarget(R.id.ring, getActivity()))
                    .setContentTitle("TRANSMITTING")
                    .setContentText(getText(R.string.transmitter_tutorial))
                    .setStyle(R.style.CustomShowcaseTheme)
                    .blockAllTouches()
                    .build();
            showcaseView.setButtonPosition(tps);
            showcaseView.show();
            showcaseView.overrideButtonClick(v -> {
                Utils.vibrate(v);
                tutorial_count++;
                settings.edit().putInt("tutorial", tutorial_count).apply();
                switch (tutorial_count) {
                    case 1:
                        showcaseView.setTarget(new ViewTarget(R.id.quetv, getActivity()));
                        showcaseView.forceTextPosition(ShowcaseView.BELOW_SHOWCASE);
                        showcaseView.setContentTitle("NO BREAK 1-9 NEEDED");
                        showcaseView.setContentText("Simultaneous transmissions are added to the queue");
                        break;
                    case 2:
                        showcaseView.setContentText("You will not be able to transmit when your queue goes over 9");
                        break;
                    case 3:
                        showcaseView.forceTextPosition(ShowcaseView.ABOVE_SHOWCASE);
                        showcaseView.setTarget(new ViewTarget(R.id.talkback, getActivity()));
                        showcaseView.setContentTitle("TALK BACK");
                        showcaseView.setContentText("Repeats your transmission back to you if enabled");
                        break;
                    case 4:
                        showcaseView.hide();
                        if (MI != null) MI.finishTutorial(tutorial_count);
                        break;
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRecorder(false);
        stopRecorder(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MI != null) {
            updateDisplay(MI.getDisplayedText(), MI.getStamp());
            updateque(MI.getQueue(), RadioService.paused);
            settooth(RadioService.bluetoothEnabled);
            setMute(RadioService.mute);
        }
        if (RadioService.operator.getSubscribed()) {
            recordTime = 60000;
            queueMax = 19;
        } else {
            recordTime = 30000;
            queueMax = 9;
        }
        duration = settings.getInt("ring", 1500);
        rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration((1500 - duration) + 85);
        rotate.setRepeatCount(-1);
        rotate.setInterpolator(new LinearInterpolator());
        hold = settings.getBoolean("holdmic", true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recorder != null) recorder.release();
    }

    private void startRecorder() {
        if (RadioService.recording || queue >= queueMax || !RadioService.phoneIdle || RadioService.paused || MI == null)
            return;
        if (RadioService.operator.getChannel() == null) {
            MI.selectChannel(false);
            return;
        }
        if (RadioService.operator.getSilenced()) {
            MI.showSnack(new Snack("You are currently silenced", Snackbar.LENGTH_SHORT));
            return;
        }
        if (!Utils.permissionsAccepted(context, recordPermissions)) {
            Log.i("test", "startRecorder() permission request");
            context.sendBroadcast(new Intent("show_result").putExtra("title", getResources().getString(R.string.permission_needed_title)).putExtra("content", getResources().getString(R.string.record_access_info)));
            return;
        }
        Log.i("test", "startRecorder() recordChange(true)");
        MI.recordChange(true);
        Utils.vibrate(ring);
    }

    public void transmitStart() {
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(200);
        AlphaAnimation slightFadeOut = new AlphaAnimation(1f, .3f);
        slightFadeOut.setDuration(200);
        slightFadeOut.setFillAfter(true);
        context.sendBroadcast(new Intent("nineteenMicSound"));
        handle.setText(R.string.transmitting);
        carrier.setText("");
        location.setText("");
        title.setText("");
        profile.setVisibility(View.GONE);
        rank.setImageDrawable(null);
        if (rotate != null && duration != 0) ring.startAnimation(rotate);
        pin.startAnimation(fadeOut);
        key.setImageResource(R.drawable.center_transmitting);
        key.startAnimation(slightFadeOut);
        blurr.setVisibility(View.VISIBLE);
        blurr.startAnimation(fadeIn);
        progressBar.setOnSeekBarChangeListener(null);
        progressBar.setMax(recordTime);
        muteIv.setEnabled(false);
        MI.lockOthers(true);
        final ProgressAnimation anim = new ProgressAnimation(progressBar, recordTime);
        anim.setDuration(recordTime);
        progressBar.startAnimation(anim);
        try {
            msgCount = Instant.now().getEpochSecond();
            recorder = new MediaRecorder(context);
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(Utils.formatLocalAudioFileLocation(saveDirectory, msgCount));
            recorder.setAudioSamplingRate(44100);
            recorder.setAudioEncodingBitRate(96000);
            recorder.setMaxDuration(recordTime);
            recorder.setOnInfoListener((mr, what, extra) -> {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED)
                    stopRecorder(true);
            });
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            Log.e("transmitStart()", e.getMessage());
            stopRecorder(false);
        }
    }

    public void stopRecorder(final boolean pass) {
        if (!RadioService.recording) return;
        if (MI != null)
            MI.recordChange(false);
        if (recorder != null) {
            recorder.reset();
            recorder.release();
            recorder = null;
            if (pass) {
                final String fileUrl = Utils.formatLocalAudioFileLocation(saveDirectory, msgCount);
                long loggedTime = (Instant.now().getEpochSecond() - msgCount);
                if (loggedTime >= 2) {
                    Utils.vibrate(ring);
                    context.sendBroadcast(new Intent("nineteenMicSound"));
                    context.sendBroadcast(new Intent("nineteenTransmit").putExtra("data", fileUrl).putExtra("stamp", msgCount).putExtra("talkback", talkback.isChecked()).putExtra("duration", loggedTime));

                } else MI.showSnack(new Snack("Too short!", Snackbar.LENGTH_SHORT));
            }
        }
        ring.clearAnimation();
        key.clearAnimation();
        pin.startAnimation(fadeIn);
        blurr.clearAnimation();
        blurr.setVisibility(View.GONE);
        key.setImageResource(R.drawable.center_white);
        progressBar.clearAnimation();
        progressBar.setMax(queueMax);
        progressBar.setProgress(queue);
        muteIv.setEnabled(true);
        progressBar.setOnSeekBarChangeListener(Transmitter.this);
        if (MI != null) MI.postKeyUp();
    }

    @Override
    public boolean onLongClick(View v) {
        UserListEntry user = MI.returnTalkerEntry();
        if (user != null) {
            context.sendBroadcast(new Intent("nineteenClickSound"));
            Utils.vibrate(v);
            if (MI != null) MI.showListOptions(user);
            return true;
        }
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (MI != null) {
            if (isChecked) MI.showSnack(new Snack("Talk-Back", Snackbar.LENGTH_SHORT));
            else MI.showSnack(new Snack("Talk-Back Off", Snackbar.LENGTH_SHORT));
        }
        Utils.vibrate(buttonView);
        context.sendBroadcast(new Intent("nineteenBoxSound"));
        settings.edit().putBoolean("talkback", isChecked).apply();
    }

    public void setMute(final boolean mute) {
        if (mute)
            muteIv.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.muted_set));
        else muteIv.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.muter));
    }

    public void settooth(boolean show) {
        if (show) {
            tooth.setVisibility(View.VISIBLE);
        } else {
            tooth.setVisibility(View.GONE);
        }
    }

    public void updateDisplay(final ProfileDisplay display, long stamp) {
        if (RadioService.recording || !isResumed()) return;
        handle.setText(display.getHandle());
        carrier.setText(display.getCarrier());
        location.setText(display.getTown());
        if (stamp == 0)
            title.setText("");
        else
            title.setText(Utils.formatDiff(Utils.timeDifferance(stamp), true));
        new GlideImageLoader(context, rank).load(Utils.parseRankUrl(display.getRank()));
        if (display.getProfileLink().equals("none")) {
            profile.setVisibility(View.GONE);
            profile.setOnClickListener(null);
        } else {
            profile.setVisibility(View.VISIBLE);
            new GlideImageLoader(context, profile).load(display.getProfileLink(), RadioService.profileOptions);
            profile.setOnClickListener(v -> {
                Utils.vibrate(v);
                context.sendBroadcast(new Intent("nineteenBoxSound"));
                if (MI != null)
                    MI.streamFile(display.getProfileLink());
            });
        }
    }

    public void mainRecord() {
        if (!RadioService.recording) startRecorder();
        else stopRecorder(true);
    }

    public void updateque(final int count, final boolean paused) {
        queue = count;
        if (RadioService.recording) {
            quetv.setText(String.format(locale, "%,d", queue));
            return;
        }
        if (tracking) {
            if (count > queueMax) progressBar.setMax(count);
        } else quetv.setText(String.format(locale, "%,d", queue));
        progressBar.setProgress(count);
        if (count >= queueMax || paused || RadioService.operator.getSilenced() || !RadioService.phoneIdle) {
            key.setImageResource(R.drawable.center_black);
            ring.setImageResource(R.drawable.ring_black);
            pin.setImageResource(R.drawable.pinned_black);
        } else {
            ring.setImageResource(R.drawable.ring_white);
            key.setImageResource(R.drawable.center_white);
            pin.setImageResource(R.drawable.pinned_white);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (progress > queue) {
            seekBar.setProgress(queue);
            return;
        }
        if (!fromUser) return;
        quetv.setText(String.format(locale, "%,d", progress));
        final String[] quick = MI.queCheck(progress);
        handle.setText(quick[0]);
        carrier.setText(quick[1]);
        location.setText(quick[2]);
        long difference = MI.queStamp(progress);
        if (difference == 0) title.setText("");
        else title.setText(Utils.formatDiff(Utils.timeDifferance(difference), false));
        new GlideImageLoader(context, rank).load(Utils.parseRankUrl(quick[4]));
        if (quick[5].equals("none")) {
            profile.setOnClickListener(null);
            profile.setVisibility(View.GONE);
        } else {
            profile.setVisibility(View.VISIBLE);
            new GlideImageLoader(context, profile).load(quick[5], RadioService.profileOptions);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Utils.vibrate(seekBar);
        ring.setEnabled(false);
        if (queue > queueMax) seekBar.setMax(queue);
        tracking = true;
        if (MI != null) MI.lockOthers(true);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        final int selection = seekBar.getProgress();
        if (queue != selection) {
            if (queue != 0) Utils.vibrate(seekBar);
            context.sendBroadcast(new Intent("nineteenScroll").putExtra("data", selection));
        }
        seekBar.setMax(queueMax);
        quetv.setText(String.format(locale, "%,d", queue));
        ring.setEnabled(true);
        tracking = false;
        if (MI != null) MI.lockOthers(false);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        view.performClick();
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (hold) {
                    if (!RadioService.recording) startRecorder();
                } else {
                    if (!RadioService.recording) startRecorder();
                    else {
                        stopRecorder(true);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (hold && RadioService.recording) stopRecorder(true);
                break;
        }
        return true;
    }
}