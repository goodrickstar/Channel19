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
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.UserListBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.vdurmont.emoji.EmojiParser;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class UserList extends Fragment {

    private GlideImageLoader glide;
    private final recycler_adapter adapter = new recycler_adapter();
    private Context context;
    private MI MI;
    private UserListBinding binding;
    private final FragmentManager fragmentManager;

    public UserList(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public void update_users_list(List<UserListEntry> userList) {
        adapter.updateUserList(userList);
        binding.swiper.setRefreshing(false);
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
        glide = new GlideImageLoader(context);
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
            context.sendBroadcast(new Intent("fetch_users").setPackage("com.cb3g.channel19"));
        });
        binding.swiper.setRefreshing(true);
        update_users_list(RadioService.userList);
    }

    @Override
    public void onResume() {
        super.onResume();
        context.sendBroadcast(new Intent("fetch_users").setPackage("com.cb3g.channel19"));
    }

    class recycler_adapter extends RecyclerView.Adapter<recycler_adapter.MyViewHolder> {
        List<UserListEntry> userList = new ArrayList<>();

        public void updateUserList(List<UserListEntry> userList) {
            final UserDiffCallback diffCallback = new UserDiffCallback(this.userList, userList);
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
            this.userList.clear();
            this.userList.addAll(userList);
            diffResult.dispatchUpdatesTo(this);
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(getLayoutInflater().inflate(R.layout.user_list_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int i) {
            UserListEntry entry = userList.get(i);
            final User user = entry.getUser();
            String title = "";
            if (entry.isGhost()) title = "(G) ";
            if (user.getSubscribed())
                title += " " + EmojiParser.parseToUnicode(":heavy_plus_sign:");
            if (!entry.isOnCall() && entry.isPaused())
                title += " " + EmojiParser.parseToUnicode(":double_vertical_bar:");
            else if (entry.isOnCall()) title += " " + EmojiParser.parseToUnicode(":telephone:");
            if (user.getNewbie() == 1) title += " " + EmojiParser.parseToUnicode(":new:");
            if (entry.isAutoSkipped())
                title += " " + EmojiParser.parseToUnicode(":fast_forward:");
            if (entry.isSilenced()) title += " " + EmojiParser.parseToUnicode(":mute:");
            holder.title.setText(title);
            holder.location.setText(user.getHometown());
            holder.handle.setText(user.getRadio_hanlde());
            holder.carrier.setText(user.getCarrier());
            glide.load(holder.profile, user.getProfileLink(), RadioService.profileOptions);
            glide.load(holder.starsIm, Utils.parseRankUrl(user.getRank()));
            holder.profile.setTag(R.id.black_profile_picture_iv, user);
            holder.clickPoint.setTag(R.id.clickPoint, user);
            holder.mail.setTag(R.id.mail, user);
            holder.profile.setOnClickListener(listener);
            holder.clickPoint.setOnClickListener(listener);
            holder.mail.setOnClickListener(listener);
            if (RadioService.blockListContainsId(RadioService.textIDs, user.getUser_id()) || RadioService.operator.getHinderTexts())
                holder.mail.setVisibility(View.GONE);
            else holder.mail.setVisibility(View.VISIBLE);
        }

        private final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MI == null) return;
                Utils.vibrate(v);
                context.sendBroadcast(new Intent("nineteenBoxSound").setPackage("com.cb3g.channel19"));
                User user = (User) v.getTag(v.getId());
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
                    UserListOptionsNew cdf = (UserListOptionsNew) fragmentManager.findFragmentByTag("options");
                    if (cdf == null) {
                        Bundle bundle = new Bundle();
                        bundle.putString("user", new Gson().toJson(user));
                        cdf = new UserListOptionsNew(fragmentManager, user);
                        cdf.setArguments(bundle);
                        cdf.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                        cdf.show(fragmentManager, "options");
                    }
                } else if (id == R.id.black_profile_picture_iv) {
                    MI.streamFile(user.getProfileLink());
                }
            }
        };

        @Override
        public int getItemCount() {
            return userList.size();
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
                handle = itemView.findViewById(R.id.black_handle_tv);
                location = itemView.findViewById(R.id.black_banner_tv);
                carrier = itemView.findViewById(R.id.black_carrier_tv);
                starsIm = itemView.findViewById(R.id.black_star_iv);
                mail = itemView.findViewById(R.id.mail);
                profile = itemView.findViewById(R.id.black_profile_picture_iv);
            }
        }
    }
}




