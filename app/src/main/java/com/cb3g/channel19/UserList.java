package com.cb3g.channel19;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.UserListBinding;
import com.google.android.material.snackbar.Snackbar;
import com.vdurmont.emoji.EmojiParser;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.Instant;

public class UserList extends Fragment {
    private final recycler_adapter adapter = new recycler_adapter();
    private Context context;
    private MI MI;
    private Instant now = Instant.now();
    private UserListBinding binding;

    public void update_users_list() {
        now = Instant.now();
        adapter.notifyDataSetChanged();
        binding.swiper.setRefreshing(false);
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (MI) getActivity();
    }

    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = UserListBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.userlistview.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        binding.userlistview.setHasFixedSize(true);
        binding.userlistview.setAdapter(adapter);
        binding.swiper.setOnRefreshListener(() -> {
            Utils.vibrate(binding.swiper);
            context.sendBroadcast(new Intent("fetch_users"));
        });
        binding.swiper.setRefreshing(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        context.sendBroadcast(new Intent("fetch_users"));
    }

    class recycler_adapter extends RecyclerView.Adapter<recycler_adapter.MyViewHolder> {
        private FBentry findPaused(String userId) {
            for (FBentry user : RadioService.pausedUsers) {
                if (user.getUserId().equals(userId)) return user;
            }
            return null;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(getLayoutInflater().inflate(R.layout.user_list_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int i) {
            final UserListEntry entry = RadioService.users.get(i);
            String title = "";
            if (entry.getSubscribed()) title = EmojiParser.parseToUnicode(":heavy_plus_sign:");
            FBentry pausedUser = findPaused(entry.getUser_id());
            if (pausedUser != null) {
                title = EmojiParser.parseToUnicode(":double_vertical_bar:");
                holder.location.setText("Paused: " + Utils.timeOnline(Utils.timeDifferance(pausedUser.getInstant(), now)));
            } else holder.location.setText(entry.getHometown());
            if (entry.getSilenced()) title = EmojiParser.parseToUnicode(":mute:");
            if (entry.getNewbie() == 1) {
                if (title.isEmpty()) title = EmojiParser.parseToUnicode(":new:");
                else title = EmojiParser.parseToUnicode(":new:") + " " + title;
            }
            if (RadioService.autoSkip.contains(entry.getUser_id()))
                title = title + " " + EmojiParser.parseToUnicode(":fast_forward:");
            if (RadioService.userIsGhost(entry.getUser_id())) holder.title.setText("(G) " + title);
            else holder.title.setText(title);
            holder.handle.setText(entry.getRadio_hanlde());
            holder.carrier.setText(entry.getCarrier());
            new GlideImageLoader(context, holder.profile).load(entry.getProfileLink(), RadioService.profileOptions);
            new GlideImageLoader(context, holder.starsIm).loadRank(entry.getRank());
            holder.profile.setTag(R.id.option_image_view, entry);
            holder.clickPoint.setTag(R.id.clickPoint, entry);
            holder.mail.setTag(R.id.mail, entry);
            holder.profile.setOnClickListener(listener);
            holder.clickPoint.setOnClickListener(listener);
            holder.mail.setOnClickListener(listener);
            if (RadioService.blockListContainsId(RadioService.textIDs, entry.getUser_id()) || RadioService.operator.getHinderTexts())
                holder.mail.setVisibility(View.GONE);
            else holder.mail.setVisibility(View.VISIBLE);
        }

        private final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MI == null) return;
                Utils.vibrate(v);
                context.sendBroadcast(new Intent("nineteenBoxSound"));
                UserListEntry user = (UserListEntry) v.getTag(v.getId());
                int id = v.getId();
                if (id == R.id.mail) {
                    if (RadioService.operator.getSilenced()) {
                        MI.showSnack(new Snack("You are currently silenced", Snackbar.LENGTH_LONG));
                        return;
                    }
                    MI.createPm(user);
                } else if (id == R.id.clickPoint) {
                    if (RadioService.operator.getSilenced()) {
                        MI.showSnack(new Snack("You are currently silenced", Snackbar.LENGTH_LONG));
                        return;
                    }
                    MI.showListOptions(user);
                } else if (id == R.id.option_image_view) {
                    MI.streamFile(user.getProfileLink());
                }
            }
        };

        @Override
        public int getItemCount() {
            return RadioService.users.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView handle, location, carrier, title;
            ImageView starsIm, mail;
            ImageView profile;
            View clickPoint;

            MyViewHolder(View itemView) {
                super(itemView);
                clickPoint = itemView.findViewById(R.id.clickPoint);
                title = itemView.findViewById(R.id.countdown);
                handle = itemView.findViewById(R.id.handle);
                location = itemView.findViewById(R.id.banner);
                carrier = itemView.findViewById(R.id.carrier);
                starsIm = itemView.findViewById(R.id.starIV);
                mail = itemView.findViewById(R.id.mail);
                profile = itemView.findViewById(R.id.option_image_view);
            }
        }
    }
}




