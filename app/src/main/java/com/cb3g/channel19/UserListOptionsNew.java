package com.cb3g.channel19;

import static com.cb3g.channel19.RadioService.databaseReference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.ListOptionNewBinding;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.Instant;

import java.util.ArrayList;

public class UserListOptionsNew extends DialogFragment {
    private final options_adapter optionsAdapter = new options_adapter();
    private Context context;
    private MI MI;
    private ListOptionNewBinding binding;
    private final User user;
    private Coordinates coordinates = null;
    private final ArrayList<UserOption> options = new ArrayList<>();

    private final FragmentManager fragmentManager;

    public UserListOptionsNew(FragmentManager fragmentManager, User user) {
        this.user = user;
        this.fragmentManager = fragmentManager;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = ListOptionNewBinding.inflate(inflater);
        return binding.getRoot();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NotNull View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        RadioService.occupied.set(true);
        View.OnClickListener dismisses = v1 -> {
            Utils.vibrate(v1);
            dismiss();
        };
        binding.userListOptionHandleTv.setText(user.getRadio_hanlde());
        binding.userListOptionHandleTv.setOnClickListener(dismisses);
        if (user.getStamp() != 0)
            binding.timeOnline.setText("Online: " + Utils.timeOnline(Utils.timeDifferance(user.getStamp())));
        else binding.timeOnline.setText("Offline");
        binding.deviceName.setText(user.getDeviceName());
        new GlideImageLoader(context).load(binding.largeProfile, user.getProfileLink(), RadioService.largeProfileOptions);
        binding.nearbyLimitBar.getProgressDrawable().setColorFilter(Utils.colorFilter(Color.WHITE));
        binding.nearbyLimitBar.getThumb().setColorFilter(Utils.colorFilter(Color.WHITE));
        if (MI != null)
            binding.nearbyLimitBar.setProgress(MI.returnUserVolume(user.getUser_id()) - 50);
        binding.nearbyLimitBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress += 50;
                if (progress == 100)
                    databaseReference.child("volumes").child(RadioService.operator.getUser_id()).child(user.getUser_id()).removeValue();
                else
                    databaseReference.child("volumes").child(RadioService.operator.getUser_id()).child(user.getUser_id()).setValue(new UserVolume(user.getUser_id(), progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Utils.vibrate(seekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Utils.vibrate(seekBar);
            }
        });
        binding.largeProfile.setOnClickListener(v12 -> {
            Utils.vibrate(v12);
            context.sendBroadcast(new Intent("nineteenBoxSound"));
            if (MI != null) MI.streamFile(user.getProfileLink());
        });
        binding.optionMenu.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        binding.optionMenu.setHasFixedSize(true);
        binding.optionMenu.setAdapter(optionsAdapter);
        if (RadioService.operator.getAdmin()) buildOptions();
        if (user.getUser_id().equals(RadioService.operator.getUser_id()) || RadioService.ghostUsers.contains(user.getUser_id()) || RadioService.operator.getSilenced() || !RadioService.isInChannel(user.getUser_id()))
            return;
        buildOptions();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void buildOptions() {
        options.clear();
        if ((RadioService.operator.getCount() > 19 && RadioService.operator.getBlocking()) || RadioService.operator.getAdmin()) {
            options.add(new UserOption(ListOption.BLOCK, "Block"));
        }
        if (!RadioService.operator.getHinderTexts()) {
            options.add(new UserOption(ListOption.TEXT, "Send Message"));
        }
        if (!RadioService.operator.getHinderPhotos()) {
            options.add(new UserOption(ListOption.PHOTO, "Send Photo"));
        }
        if (!RadioService.operator.getHinderPhotos() || RadioService.operator.getHinderTexts()) {
            options.add(new UserOption(ListOption.HISTORY, "Chat History"));
        }
        if ((RadioService.operator.getFlagsEnabled() && RadioService.operator.getSalutes() > 79) || RadioService.operator.getAdmin()) {
            if ((!RadioService.pausedUsers.contains(user.getUser_id()) && !RadioService.onCallUsers.contains(user.getUser_id()) && !RadioService.silencedUsers.contains(user.getUser_id())) || RadioService.operator.getAdmin()) {
                if (!Utils.alreadySaluted(user.getUser_id()) || RadioService.operator.getAdmin())
                    options.add(new UserOption(ListOption.SALUTE, "Salute"));
                if (!Utils.alreadyFlagged(user.getUser_id()) || RadioService.operator.getAdmin()) {
                    options.add(new UserOption(ListOption.FLAG, "Flag"));
                    if (RadioService.operator.getSalutes() > 2000) {
                        options.add(new UserOption(ListOption.LONG_FLAG, "Long Flag"));
                    }
                }
            }
        }
        if ((RadioService.operator.getSalutes() > 159 && RadioService.operator.getSilencing()) || RadioService.operator.getAdmin()) {
            if ((!RadioService.pausedUsers.contains(user.getUser_id()) && !RadioService.onCallUsers.contains(user.getUser_id())) || RadioService.operator.getAdmin()) {
                if (!RadioService.silencedUsers.contains(user.getUser_id()))
                    options.add(new UserOption(ListOption.SILENCE, "Silence"));
                else options.add(new UserOption(ListOption.UNSILENCE, "Un-Silence"));
            }
        }
        if ((RadioService.operator.getSubscribed()) || RadioService.operator.getAdmin()) {
            if (!RadioService.autoSkip.contains(user.getUser_id()))
                options.add(new UserOption(ListOption.AUTOSKIP, "Start Auto Skip"));
            else options.add(new UserOption(ListOption.AUTOSKIP, "Stop Auto Skip"));
        }
        for (Coordinates x : RadioService.coordinates) {
            if (x.getUserId().equals(user.getUser_id())) coordinates = x;
        }
        if (coordinates != null) options.add(new UserOption(ListOption.LOCATE, "Show On Map"));
        //Admin Options
        if (RadioService.operator.getAdmin()) {
            options.add(new UserOption(ListOption.INFO, "User Info"));
            options.add(new UserOption(ListOption.PAUSE_OR_PLAY, "Pause Or Play"));
            options.add(new UserOption(ListOption.KICK_USER, "Kick User"));
            options.add(new UserOption(ListOption.FLAG_OUT, "Flag Out"));
            options.add(new UserOption(ListOption.BANN_USER, "Ban User"));
        }
        optionsAdapter.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void buildBlockingOptions() {
        options.clear();
        options.add(new UserOption(ListOption.CANCEL, "Cancel"));
        options.add(new UserOption(ListOption.BLOCK_RADIO, "Block Radio"));
        options.add(new UserOption(ListOption.BLOCK_TEXT, "Block Messages"));
        options.add(new UserOption(ListOption.BLOCK_PHOTOS, "Block Photos"));
        optionsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
    }

    @Override
    public void onDismiss(@NotNull DialogInterface dialog) {
        super.onDismiss(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }

    @Override
    public void onCancel(@NotNull DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }

    private void showSnack(Snack snack) {
        Snackbar snackbar = Snackbar.make(binding.topLevel, snack.getMessage(), snack.getLength());
        View view = snackbar.getView();
        TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(ContextCompat.getColor(context, R.color.main_white));
        view.setBackgroundColor(ContextCompat.getColor(context, R.color.main_black_transparent));
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        snackbar.show();
    }

    class options_adapter extends RecyclerView.Adapter<options_adapter.MyViewHolder> {

        @NonNull
        @Override
        public options_adapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new options_adapter.MyViewHolder(getLayoutInflater().inflate(R.layout.user_option_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull options_adapter.MyViewHolder holder, int i) {
            UserOption option = options.get(i);
            holder.description.setText(option.getDescription());
            switch (option.getOption()) {
                case TEXT, BLOCK_TEXT -> holder.icon.setImageResource(R.drawable.messages);
                case PHOTO, BLOCK_PHOTOS -> holder.icon.setImageResource(R.drawable.photo);
                case HISTORY -> holder.icon.setImageResource(R.drawable.history);
                case BLOCK -> holder.icon.setImageResource(R.drawable.block);
                case SILENCE -> holder.icon.setImageResource(R.drawable.silence);
                case UNSILENCE -> holder.icon.setImageResource(R.drawable.unsilence);
                case AUTOSKIP -> holder.icon.setImageResource(R.drawable.autoskip);
                case LOCATE -> holder.icon.setImageResource(R.drawable.gps);
                case SALUTE -> holder.icon.setImageResource(R.drawable.like);
                case FLAG -> holder.icon.setImageResource(R.drawable.flag);
                case BLOCK_RADIO -> holder.icon.setImageResource(R.drawable.radio);
                case CANCEL -> holder.icon.setImageResource(R.drawable.back);
//Admin Actions
                case INFO -> holder.icon.setImageResource(R.drawable.info);
                case FLAG_OUT, KICK_USER, PAUSE_OR_PLAY, LONG_FLAG ->
                        holder.icon.setImageResource(R.drawable.flag_red);
                case BANN_USER -> holder.icon.setImageResource(R.drawable.delete_red);
            }
            holder.itemView.setTag(holder.itemView.getId(), option);
            holder.itemView.setOnClickListener(listener);
            //holder.mail.setTag(R.id.mail, entry);
        }


        private final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.vibrate(v);
                context.sendBroadcast(new Intent("nineteenBoxSound"));
                if (MI == null) return;
                UserOption option = (UserOption) v.getTag(v.getId());
                switch (option.getOption()) {
                    case CANCEL -> buildOptions();
                    case BLOCK -> {
                        if (RadioService.silencedUsers.contains(user.getUser_id()) && !RadioService.operator.getAdmin()) {
                            showSnack(new Snack("You can not block a silenced user", Snackbar.LENGTH_LONG));
                            return;
                        }
                        buildBlockingOptions();
                    }
                    case TEXT -> {
                        MI.createPm(user);
                        dismiss();
                    }
                    case PHOTO -> {
                        ImagePicker imagePicker = (ImagePicker) fragmentManager.findFragmentByTag("imagePicker");
                        if (imagePicker == null) {
                            imagePicker = new ImagePicker(fragmentManager, user, RequestCode.PRIVATE_PHOTO);
                            imagePicker.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                            imagePicker.show(fragmentManager, "imagePicker");
                        }
                    }
                    case HISTORY -> {
                        dismiss();
                        MI.displayChat(user, false, false);
                    }
                    case SILENCE -> {
                        MI.silence(user);
                        dismiss();
                    }
                    case UNSILENCE -> {
                        MI.unsilence(user);
                        dismiss();
                    }
                    case AUTOSKIP -> {
                        if (RadioService.autoSkip.contains(user.getUser_id()))
                            databaseReference.child("autoSkip").child(RadioService.operator.getUser_id()).child(user.getUser_id()).removeValue();
                        else {
                            databaseReference.child("autoSkip").child(RadioService.operator.getUser_id()).child(user.getUser_id()).setValue(Instant.now().getEpochSecond());
                            context.sendBroadcast(new Intent("purgeUser").putExtra("data", user.getUser_id()));
                        }
                        dismiss();
                    }
                    case LOCATE -> {
                        dismiss();
                        MI.findUser(coordinates);
                    }
                    case SALUTE -> {
                        MI.saluteThisUser(user);
                        dismiss();
                    }
                    case FLAG -> {
                        MI.flagThisUser(user);
                        dismiss();
                    }
                    case LONG_FLAG -> {
                        MI.longFlagUser(user);
                        dismiss();
                    }
                    case BLOCK_RADIO -> {
                        MI.blockThisUser(user.getUser_id(), user.getRadio_hanlde(), true);
                        dismiss();
                    }
                    case BLOCK_PHOTOS -> {
                        MI.blockPhoto(user.getUser_id(), user.getRadio_hanlde(), true);
                        dismiss();
                    }
                    case BLOCK_TEXT -> {
                        MI.blockText(user.getUser_id(), user.getRadio_hanlde(), true);
                        dismiss();
                    }
                    //Admin Actions
                    case INFO -> {
                        context.sendBroadcast(new Intent("fetchInformation").putExtra("data", user.getUser_id()));
                        dismiss();
                    }
                    case FLAG_OUT -> {
                        MI.flagOut(user.getUser_id());
                        dismiss();
                    }
                    case PAUSE_OR_PLAY -> {
                        MI.pauseOrPlay(user);
                        dismiss();
                    }
                    case KICK_USER -> {
                        MI.kickUser(user);
                        dismiss();
                    }
                    case BANN_USER -> {
                        MI.banUser(user.getUser_id());
                        dismiss();
                    }
                }
            }
        };

        @Override
        public int getItemCount() {
            return options.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView description;

            MyViewHolder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.black_profile_picture_iv);
                description = itemView.findViewById(R.id.option_name);
            }
        }
    }
}

