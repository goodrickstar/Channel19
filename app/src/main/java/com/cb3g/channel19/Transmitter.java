package com.cb3g.channel19;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.TransmitBinding;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.Instant;

import java.io.IOException;
import java.util.Locale;

public class Transmitter extends Fragment implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener, View.OnLongClickListener, CheckBox.OnCheckedChangeListener {
    private final String[] recordPermissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE};
    private TransmitBinding binding;
    final private AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
    final private AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
    private int duration = 0, recordTime = 30000, queue = 0;
    private boolean tracking = false, hold = false;
    private MI MI;
    private Context context;
    private RotateAnimation rotate = null;
    private SharedPreferences settings;
    private MediaRecorder recorder = null;
    private String saveDirectory;
    private Locale locale;
    private long msgCount = System.currentTimeMillis();
    private int tutorial_count = 0, queueMax = 9;
    private GlideImageLoader glideImageLoader;
    private final FragmentManager fragmentManager;

    public Transmitter(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
        glideImageLoader = new GlideImageLoader(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        binding = TransmitBinding.inflate(inflater);
        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        locale = Locale.getDefault();
        binding.pin.setImageResource(R.drawable.pinned_white);
        binding.key.setImageResource(R.drawable.center_white);
        binding.ring.setImageResource(R.drawable.ring_white);
        binding.tooth.setImageResource(R.drawable.tooth);
        binding.pbar.getProgressDrawable().setColorFilter(Utils.colorFilter(Color.WHITE));
        binding.pbar.getThumb().setColorFilter(Utils.colorFilter(Color.WHITE));
        settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        binding.talkback.setChecked(settings.getBoolean("talkback", true));
        binding.transmitProfilePictureIv.setOnLongClickListener(this);
        binding.pbar.setMax(queueMax);
        binding.pbar.setOnSeekBarChangeListener(this);
        binding.mute.setOnClickListener(view1 -> {
            if (MI != null) {
                if (!RadioService.mute) {
                    binding.mute.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.muted_set));
                } else {
                    binding.mute.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.muter));
                }
                context.sendBroadcast(new Intent("muteChannelNineTeen"));
            }
        });
        binding.talkback.setOnCheckedChangeListener(this);
        saveDirectory = context.getCacheDir() + "/";
        binding.ring.setOnTouchListener(this);
        fadeOut.setDuration(200);
        fadeIn.setDuration(200);
        fadeOut.setFillAfter(true);
        tutorial_count = settings.getInt("tutorial", 0);
        if (tutorial_count < 4) {
            Activity activity = getActivity();
            if (activity != null) {
                final int margin = ((Number) (getResources().getDisplayMetrics().density * 16)).intValue();
                final RelativeLayout.LayoutParams tps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tps.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                tps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                tps.setMargins(margin, margin, margin, margin);
                tutorial_count = 0;
                final ShowcaseView showcaseView = new ShowcaseView.Builder(getActivity())
                        .setTarget(new ViewTarget(R.id.ring, activity))
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
                        case 1 -> {
                            showcaseView.setTarget(new ViewTarget(R.id.quetv, getActivity()));
                            showcaseView.forceTextPosition(ShowcaseView.BELOW_SHOWCASE);
                            showcaseView.setContentTitle("NO BREAK 1-9 NEEDED");
                            showcaseView.setContentText("Simultaneous transmissions are added to the queue");
                        }
                        case 2 ->
                                showcaseView.setContentText("You will not be able to transmit when your queue goes over 9");
                        case 3 -> {
                            showcaseView.forceTextPosition(ShowcaseView.ABOVE_SHOWCASE);
                            showcaseView.setTarget(new ViewTarget(R.id.talkback, getActivity()));
                            showcaseView.setContentTitle("TALK BACK");
                            showcaseView.setContentText("Repeats your transmission back to you if enabled");
                        }
                        case 4 -> {
                            showcaseView.hide();
                            if (MI != null) MI.finishTutorial(tutorial_count);
                        }
                    }
                });
            }
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
            updateQueue(MI.getQueue(), RadioService.paused);
            setTooth(RadioService.bluetoothEnabled);
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
        Utils.vibrate(binding.ring);
    }

    public void transmitStart() {
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(200);
        AlphaAnimation slightFadeOut = new AlphaAnimation(1f, .3f);
        slightFadeOut.setDuration(200);
        slightFadeOut.setFillAfter(true);
        context.sendBroadcast(new Intent("nineteenMicSound"));
        binding.transmitHandleTv.setText(R.string.transmitting);
        binding.transmitCarrierTv.setText("");
        binding.transmitDurationTv.setText("");
        binding.transmitTitleTv.setText("");
        binding.transmitProfilePictureIv.setVisibility(View.GONE);
        binding.transmitRankIv.setImageDrawable(null);
        if (rotate != null && duration != 0) binding.ring.startAnimation(rotate);
        binding.pin.startAnimation(fadeOut);
        binding.key.setImageResource(R.drawable.center_transmitting);
        binding.key.startAnimation(slightFadeOut);
        binding.maBlurrIv.setVisibility(View.VISIBLE);
        binding.maBlurrIv.startAnimation(fadeIn);
        binding.pbar.setOnSeekBarChangeListener(null);
        binding.pbar.setMax(recordTime);
        binding.mute.setEnabled(false);
        MI.lockOthers(true);
        final ProgressAnimation anim = new ProgressAnimation(binding.pbar, recordTime);
        anim.setDuration(recordTime);
        binding.pbar.startAnimation(anim);
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
            stopRecorder(false);
        }
    }

    public void stopRecorder(final boolean pass) {
        if (!RadioService.recording) return;
        if (recorder != null) {
            recorder.reset();
            recorder.release();
            recorder = null;
            if (pass) {
                final String fileUrl = Utils.formatLocalAudioFileLocation(saveDirectory, msgCount);
                long loggedTime = (Instant.now().getEpochSecond() - msgCount);
                if (loggedTime >= 2) {
                    Utils.vibrate(binding.ring);
                    context.sendBroadcast(new Intent("nineteenMicSound"));
                    context.sendBroadcast(new Intent("nineteenTransmit").putExtra("data", fileUrl).putExtra("stamp", msgCount).putExtra("talkback", binding.talkback.isChecked()).putExtra("duration", loggedTime));

                } else MI.showSnack(new Snack("Too short!", Snackbar.LENGTH_SHORT));
            }
        }
        if (MI != null)
            MI.recordChange(false);
        binding.ring.clearAnimation();
        binding.key.clearAnimation();
        binding.pin.startAnimation(fadeIn);
        binding.maBlurrIv.clearAnimation();
        binding.maBlurrIv.setVisibility(View.GONE);
        binding.key.setImageResource(R.drawable.center_white);
        binding.pbar.clearAnimation();
        binding.pbar.setMax(queueMax);
        binding.pbar.setProgress(queue);
        binding.mute.setEnabled(true);
        binding.pbar.setOnSeekBarChangeListener(Transmitter.this);
    }

    @Override
    public boolean onLongClick(View v) {
        User user = MI.returnTalkerEntry();
        if (user != null) {
            context.sendBroadcast(new Intent("nineteenClickSound"));
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
            binding.mute.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.muted_set));
        else binding.mute.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.muter));
    }

    public void setTooth(boolean show) {
        if (show) {
            binding.tooth.setVisibility(View.VISIBLE);
        } else {
            binding.tooth.setVisibility(View.GONE);
        }
    }

    public void updateDisplay(final ProfileDisplay display, long stamp) {
        if (RadioService.recording || !isResumed()) return;
        binding.transmitHandleTv.setText(display.getHandle());
        binding.transmitCarrierTv.setText(display.getCarrier());
        binding.transmitTitleTv.setText(display.getTown());
        if (stamp == 0)
            binding.transmitTitleTv.setText("");
        else
            binding.transmitTitleTv.setText(Utils.formatDiff(Utils.timeDifferance(stamp), true));
        glideImageLoader.load(binding.transmitRankIv, Utils.parseRankUrl(display.getRank()));
        if (display.getProfileLink().equals("none")) {
            binding.transmitProfilePictureIv.setVisibility(View.GONE);
            binding.transmitProfilePictureIv.setOnClickListener(null);
        } else {
            binding.transmitProfilePictureIv.setVisibility(View.VISIBLE);
            glideImageLoader.load(binding.transmitProfilePictureIv, display.getProfileLink(), RadioService.profileOptions);
            binding.transmitProfilePictureIv.setOnClickListener(v -> {
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

    public void updateQueue(final int count, final boolean paused) {
        queue = count;
        if (RadioService.recording) {
            binding.quetv.setText(String.format(locale, "%,d", queue));
            return;
        }
        if (tracking) {
            if (count > queueMax) binding.pbar.setMax(count);
        } else binding.quetv.setText(String.format(locale, "%,d", queue));
        binding.pbar.setProgress(count);
        if (count >= queueMax || paused || RadioService.operator.getSilenced() || !RadioService.phoneIdle) {
            binding.key.setImageResource(R.drawable.center_black);
            binding.ring.setImageResource(R.drawable.ring_black);
            binding.pin.setImageResource(R.drawable.pinned_black);
        } else {
            binding.ring.setImageResource(R.drawable.ring_white);
            binding.key.setImageResource(R.drawable.center_white);
            binding.pin.setImageResource(R.drawable.pinned_white);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (progress > queue) {
            seekBar.setProgress(queue);
            return;
        }
        if (!fromUser) return;
        binding.quetv.setText(String.format(locale, "%,d", progress));
        final String[] quick = MI.queCheck(progress);
        binding.transmitHandleTv.setText(quick[0]);
        binding.transmitCarrierTv.setText(quick[1]);
        binding.transmitTitleTv.setText(quick[2]);
        long difference = MI.queStamp(progress);
        if (difference == 0) binding.transmitTitleTv.setText("");
        else
            binding.transmitTitleTv.setText(Utils.formatDiff(Utils.timeDifferance(difference), false));
        glideImageLoader.load(binding.transmitRankIv, Utils.parseRankUrl(quick[4]));
        if (quick[5].equals("none")) {
            binding.transmitProfilePictureIv.setOnClickListener(null);
            binding.transmitProfilePictureIv.setVisibility(View.GONE);
        } else {
            binding.transmitProfilePictureIv.setVisibility(View.VISIBLE);
            glideImageLoader.load(binding.transmitProfilePictureIv, quick[5], RadioService.profileOptions);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Utils.vibrate(seekBar);
        binding.ring.setEnabled(false);
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
        binding.quetv.setText(String.format(locale, "%,d", queue));
        binding.ring.setEnabled(true);
        tracking = false;
        if (MI != null) MI.lockOthers(false);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        view.performClick();
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                if (hold) {
                    if (!RadioService.recording) startRecorder();
                } else {
                    if (!RadioService.recording) startRecorder();
                    else {
                        stopRecorder(true);
                    }
                }
            }
            case MotionEvent.ACTION_UP -> {
                if (hold && RadioService.recording) stopRecorder(true);
            }
        }
        return true;
    }
}