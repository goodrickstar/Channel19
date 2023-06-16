package com.cb3g.channel19;

import static com.cb3g.channel19.RadioService.databaseReference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.ListOptionNewBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.Instant;

import java.util.ArrayList;

public class UserListOptionsNew extends DialogFragment {
    private options_adapter optionsAdapter = new options_adapter();
    private Context context;
    private MI MI;
    private ListOptionNewBinding binding;
    private UserListEntry user = new UserListEntry();
    private Coordinates coordinates = null;
    private ArrayList<UserOption> options = new ArrayList<>();
    private final int cancel = 0;
    private final int text = 1;
    private final int photo = 2;
    private final int locate = 3;
    private final int history = 4;
    private final int salute = 5;
    private final int block = 6;
    private final int flag = 7;
    private final int silence = 8;
    private final int unsilence = 9;
    private final int autoskip = 10;
    private final int blockR = 11;
    private final int blockT = 12;
    private final int blockP = 13;
    private final int info = 14;
    private final int flagOut = 15;
    private final int bann = 16;
    private final int pauseOrplay = 17;
    private final int kickUser = 18;
    private final int longflag = 19;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = ListOptionNewBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NotNull View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        RadioService.occupied.set(true);
        if (!getArguments().isEmpty())
            user = new Gson().fromJson(getArguments().getString("user"), UserListEntry.class);
        View.OnClickListener dismisser = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.sendBroadcast(new Intent("nineteenVibrate"));
                dismiss();
            }
        };
        binding.handle.setText(user.getRadio_hanlde());
        binding.handle.setOnClickListener(dismisser);
        if (user.getStamp() != 0)
            binding.timeOnline.setText("Online: " + Utils.timeOnline(Utils.timeDifferance(user.getStamp())));
        else binding.timeOnline.setText("Offline");
        binding.deviceName.setText(user.getDeviceName());
        new GlideImageLoader(context, binding.largeProfile).load(user.getProfileLink(), RadioService.largeProfileOptions);
        binding.nearbyLimitBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        binding.nearbyLimitBar.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
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
                context.sendBroadcast(new Intent("nineteenVibrate"));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                context.sendBroadcast(new Intent("nineteenVibrate"));
            }
        });
        binding.largeProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.sendBroadcast(new Intent("nineteenVibrate"));
                context.sendBroadcast(new Intent("nineteenBoxSound"));
                if (MI != null) MI.streamFile(user.getProfileLink());
            }
        });
        binding.optionMenu.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        binding.optionMenu.setHasFixedSize(true);
        binding.optionMenu.setAdapter(optionsAdapter);
        if (user.getUser_id().equals(RadioService.operator.getUser_id()) || user.getUser_id().equals("JJ7SAoyqRsS7GQixEL8pbziWguV2") || userIsGhost(user.getUser_id()) || RadioService.operator.getSilenced())
            return;
        buildOptions();
    }

    private void buildOptions() {
        options.clear();
        if ((RadioService.operator.getCount() > 49 && RadioService.operator.getBlocking()) || RadioService.operator.getAdmin()) {
            options.add(new UserOption(block, "Block"));
        }
        if (!RadioService.operator.getHinderTexts()) {
            options.add(new UserOption(text, "Send Message"));
        }
        if (!RadioService.operator.getHinderPhotos()) {
            options.add(new UserOption(photo, "Send Photo"));
        }
        if (!RadioService.operator.getHinderPhotos() || RadioService.operator.getHinderTexts()) {
            options.add(new UserOption(history, "Chat History"));
        }
        if (RadioService.operator.getSalutes() > 79) {
            if (!Utils.alreadySaluted(user.getUser_id()) && findPaused(user.getUser_id()) == null)
                options.add(new UserOption(salute, "Salute"));
            if (!Utils.alreadyFlagged(user.getUser_id()) && !RadioService.operator.getFlagsEnabled()) {
                options.add(new UserOption(flag, "Flag"));
                if (RadioService.operator.getSalutes() > 2000) {
                    options.add(new UserOption(longflag, "Long Flag"));
                }
            }
        }
        if ((RadioService.operator.getSalutes() > 159 && RadioService.operator.getSilencing()) || RadioService.operator.getAdmin()) {
            if (!user.getSilenced()) options.add(new UserOption(silence, "Silence"));
            else options.add(new UserOption(unsilence, "Un-Silence"));
        }
        if ((RadioService.operator.getSubscribed()) || RadioService.operator.getAdmin()) {
            if (!RadioService.autoSkip.contains(user.getUser_id()))
                options.add(new UserOption(autoskip, "Start Auto Skip"));
            else options.add(new UserOption(autoskip, "Stop Auto Skip"));
        }
        for (Coordinates x : RadioService.coordinates) {
            if (x.getUserId().equals(user.getUser_id())) coordinates = x;
        }
        if (coordinates != null) options.add(new UserOption(locate, "Show On Map"));
        optionsAdapter.notifyDataSetChanged();
    }

    private void buildBlockingOptions() {
        options.clear();
        options.add(new UserOption(cancel, "Cancel"));
        options.add(new UserOption(blockR, "Block Radio"));
        options.add(new UserOption(blockT, "Block Messages"));
        options.add(new UserOption(blockP, "Block Photos"));
//Admin Options
        if (RadioService.operator.getAdmin()) {
            options.add(new UserOption(info, "User Info"));
            options.add(new UserOption(pauseOrplay, "Pause Or Play"));
            options.add(new UserOption(kickUser, "Kick User"));
            options.add(new UserOption(flagOut, "Flag Out"));
            options.add(new UserOption(bann, "Ban User"));
        }
        optionsAdapter.notifyDataSetChanged();
    }

    private FBentry findPaused(String userId) {
        for (FBentry user : RadioService.pausedUsers) {
            if (user.getUserId().equals(userId)) return user;
        }
        return null;
    }

    private boolean userIsGhost(String id) {
        if (RadioService.operator.getAdmin()) return false;
        for (FBentry entry : RadioService.ghostUsers) {
            if (entry.getUserId().equals(id)) return true;
        }
        return false;
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
        view.setBackgroundColor(getResources().getColor(R.color.main_black_transparent));
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
            switch (option.getId()) {
                case text:
                case blockT:
                    holder.icon.setImageResource(R.drawable.messages);
                    break;
                case photo:
                case blockP:
                    holder.icon.setImageResource(R.drawable.photo);
                    break;
                case history:
                    holder.icon.setImageResource(R.drawable.history);
                    break;
                case block:
                    holder.icon.setImageResource(R.drawable.block);
                    break;
                case silence:
                    holder.icon.setImageResource(R.drawable.silence);
                    break;
                case unsilence:
                    holder.icon.setImageResource(R.drawable.unsilence);
                    break;
                case autoskip:
                    holder.icon.setImageResource(R.drawable.autoskip);
                    break;
                case locate:
                    holder.icon.setImageResource(R.drawable.gps);
                    break;
                case salute:
                    holder.icon.setImageResource(R.drawable.like);
                    break;
                case flag:
                    holder.icon.setImageResource(R.drawable.flag);
                    break;
                case blockR:
                    holder.icon.setImageResource(R.drawable.radio);
                    break;
                case cancel:
                    holder.icon.setImageResource(R.drawable.back);
                    break;
//Admin Actions
                case info:
                    holder.icon.setImageResource(R.drawable.info);
                    break;
                case flagOut:
                case kickUser:
                case pauseOrplay:
                case longflag:
                    holder.icon.setImageResource(R.drawable.flag_red);
                    break;
                case bann:
                    holder.icon.setImageResource(R.drawable.delete_red);
                    break;
            }
            holder.itemView.setTag(holder.itemView.getId(), option);
            holder.itemView.setOnClickListener(listener);
            //holder.mail.setTag(R.id.mail, entry);
        }


        private View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.sendBroadcast(new Intent("nineteenVibrate"));
                context.sendBroadcast(new Intent("nineteenBoxSound"));
                if (MI == null) return;
                UserOption option = (UserOption) v.getTag(v.getId());
                switch (option.getId()) {
                    case cancel:
                        buildOptions();
                        break;
                    case block:
                        if (user.getSilenced() && !RadioService.operator.getAdmin()) {
                            showSnack(new Snack("You can not block_b a silenced user", Snackbar.LENGTH_LONG));
                            return;
                        }
                        buildBlockingOptions();
                        break;
                    case text:
                        MI.createPm(user);
                        dismiss();
                        break;
                    case photo:
                        MI.sendPhoto(user.getUser_id(), user.getRadio_hanlde());
                        dismiss();
                        break;
                    case history:
                        MI.displayChat(user, false, false);
                        dismiss();
                        break;
                    case silence:
                        MI.silence(user);
                        dismiss();
                        break;
                    case unsilence:
                        MI.unsilence(user);
                        dismiss();
                        break;
                    case autoskip:
                        if (RadioService.autoSkip.contains(user.getUser_id()))
                            databaseReference.child("autoSkip").child(RadioService.operator.getUser_id()).child(user.getUser_id()).removeValue();
                        else {
                            databaseReference.child("autoSkip").child(RadioService.operator.getUser_id()).child(user.getUser_id()).setValue(Instant.now().getEpochSecond());
                            context.sendBroadcast(new Intent("purgeUser").putExtra("data", user.getUser_id()));
                        }
                        dismiss();
                        break;
                    case locate:
                        dismiss();
                        MI.findUser(coordinates);
                        break;
                    case salute:
                        MI.saluteThisUser(user);
                        dismiss();
                        break;
                    case flag:
                        MI.flagThisUser(user);
                        dismiss();
                        break;
                    case longflag:
                        MI.longFlagUser(user);
                        dismiss();
                        break;
                    case blockR:
                        MI.blockThisUser(user.getUser_id(), user.getRadio_hanlde(), true);
                        dismiss();
                        break;
                    case blockP:
                        MI.blockPhoto(user.getUser_id(), user.getRadio_hanlde(), true);
                        dismiss();
                        break;
                    case blockT:
                        MI.blockText(user.getUser_id(), user.getRadio_hanlde(), true);
                        dismiss();
                        break;
                    //Admin Actions
                    case info:
                        context.sendBroadcast(new Intent("fetchInformation").putExtra("data", user.getUser_id()));
                        dismiss();
                        break;
                    case flagOut:
                        MI.flagOut(user.getUser_id());
                        dismiss();
                        break;
                    case pauseOrplay:
                        MI.pauseOrplay(user);
                        dismiss();
                        break;
                    case kickUser:
                        MI.kickUser(user);
                        dismiss();
                        break;
                    case bann:
                        MI.bannUser(user.getUser_id());
                        dismiss();
                        break;
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
                icon = itemView.findViewById(R.id.option_image_view);
                description = itemView.findViewById(R.id.option_name);
            }
        }
    }
}

