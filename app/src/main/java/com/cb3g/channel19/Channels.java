package com.cb3g.channel19;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.SidebandsBinding;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class Channels extends DialogFragment implements View.OnClickListener {
    boolean owned = false;
    private Context context;
    private MI MI;

    private SidebandsBinding binding;

    @Override
    public void onClick(View v) {
        Utils.vibrate(v);
        context.sendBroadcast(new Intent("nineteenClickSound"));
    }

    private void list_sidebands() {
        binding.swiper.setRefreshing(true);
        final String data = Jwts.builder()
                .setHeader(RadioService.header)
                .claim("userId", RadioService.operator.getUser_id())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(SignatureAlgorithm.HS256, RadioService.operator.getKey())
                .compact();
        final Request request = new Request.Builder()
                .url(RadioService.SITE_URL + "user_channels.php")
                .post(new FormBody.Builder().add("data", data).build()).build();
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                binding.swiper.post(() -> binding.swiper.setRefreshing(false));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful() && isAdded()) {
                    try {
                        assert response.body() != null;
                        JSONObject result = new JSONObject(response.body().string());
                        final List<ChannelInfo> responseChannelInfo = groupIntoSidebands(returnDataList(result.getString("users")), returnChannelList(result.getString("channels")));
                        if (responseChannelInfo.isEmpty() || (!owned && !RadioService.operator.getSilenced() && RadioService.operator.getChannel() != null))
                            responseChannelInfo.add(0, new ChannelInfo(0));
                        binding.recyclerView.post(() -> binding.recyclerView.setAdapter(new SideBandAdapter(responseChannelInfo)));
                    } catch (JSONException e) {
                        LOG.e("list_sidebands", e.getMessage());
                    }
                } else {
                    Logger.INSTANCE.e("Error", response.message());
                    assert response.body() != null;
                    Logger.INSTANCE.e("Error", response.body().string());
                }
                binding.swiper.post(() -> binding.swiper.setRefreshing(false));
            }
        });
    }

    private boolean userIsGhost(String id) {
        for (FBentry entry : RadioService.ghostUsers) {
            if (entry.getUserId().equals(id)) return true;
        }
        return false;
    }

    private List<ChannelInfo> groupIntoSidebands(final List<SidebandData> users, final List<Channel> channels) {
        int currentChannel = 0;
        if (RadioService.operator.getChannel() != null)
            currentChannel = RadioService.operator.getChannel().getChannel();
        final List<ChannelInfo> workingList = new ArrayList<>();
        for (Channel channel : channels) {
            List<String> profiles = new ArrayList<>();
            for (SidebandData data : users) {
                if (data.getChannel() == channel.getChannel() && (!RadioService.blockListContainsId(RadioService.blockedIDs, data.getUser_id()) || RadioService.operator.getAdmin())) {
                    if (!userIsGhost(data.getUser_id()) || RadioService.operator.getAdmin())
                        profiles.add(data.getProfileLink());
                }
            }
            workingList.add(new ChannelInfo(channel, profiles));
            if (channel.getUser_id().equals(RadioService.operator.getUser_id())) {
                owned = true;
            }
        }
        SharedPreferences saved = context.getSharedPreferences("channels", Context.MODE_PRIVATE);
        List<Integer> unlockedChannels = RadioService.gson.fromJson(saved.getString("channels", "[]"), new TypeToken<List<Integer>>() {
        }.getType());
        if (unlockedChannels == null) unlockedChannels = new ArrayList<>();
        for (ChannelInfo channelInfo : workingList) {
            for (int number : unlockedChannels) {
                channelInfo.setUnlocked(channelInfo.channel.getChannel() == number);
            }
            if (channelInfo.channel.getPin() == 0) channelInfo.setUnlocked(true);
        }
        if (workingList.size() > 0) {
            for (int i = workingList.size() - 1; i >= 0; i--) {
                ChannelInfo channelInfo = workingList.get(i);
                if (channelInfo.channel.getChannel() == currentChannel) {
                    workingList.remove(i);
                    workingList.add(0, channelInfo);
                }
            }
        }
        return workingList;
    }

    private List<SidebandData> returnDataList(String json) {
        List<SidebandData> list = RadioService.gson.fromJson(json, new TypeToken<List<SidebandData>>() {
        }.getType());
        if (list != null) return list;
        else return new ArrayList<>();
    }

    private List<Channel> returnChannelList(String json) {
        List<Channel> channels = RadioService.gson.fromJson(json, new TypeToken<List<Channel>>() {
        }.getType());
        if (channels != null) return channels;
        else return new ArrayList<>();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
        RadioService.occupied.set(true);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SidebandsBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        Window window = Objects.requireNonNull(getDialog()).getWindow();
        if (window != null) window.getAttributes().windowAnimations = R.style.photoAnimation;
        binding.swiper.setOnRefreshListener(() -> {
            Utils.vibrate(v);
            list_sidebands();
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setHasFixedSize(true);
        TextView close = v.findViewById(R.id.close);
        close.setOnClickListener(view -> {
            Utils.vibrate(v);
            if (RadioService.operator.getChannel() != null) {
                context.sendBroadcast(new Intent("nineteenClickSound"));
                dismiss();
            } else Toaster.toastlow(context, "Join a channel or create a new one");
        });
    }

    public int calculateNoOfColumns(int columnWidthDp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        return (int) (screenWidthDp / columnWidthDp + 0.5);
    }

    @Override
    public void onResume() {
        super.onResume();
        list_sidebands();
    }

    private class SideBandAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
        List<ChannelInfo> channels;

        public SideBandAdapter(List<ChannelInfo> channels) {
            this.channels = channels;
        }


        @Override
        public int getItemViewType(int position) {
            return channels.get(position).getType();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                return new ChannelCreatorHolder(getLayoutInflater().inflate(R.layout.sideband, parent, false));
            }
            return new ChannelViewHolder(getLayoutInflater().inflate(R.layout.sideband_creator, parent, false));
        }

        @Override
        public void onBindViewHolder(@NotNull RecyclerView.ViewHolder holder, int position) {
            final ChannelInfo channelInfo = channels.get(position);
            switch (holder.getItemViewType()) {
                case 0:
                    ChannelCreatorHolder channelCreatorHolder = (ChannelCreatorHolder) holder;
                    channelCreatorHolder.itemView.setOnClickListener(v -> {
                        Utils.vibrate(v);
                        context.sendBroadcast(new Intent("nineteenClickSound"));
                        if (MI != null) MI.createChannel();
                        dismiss();
                    });
                    break;
                case 1:
                    ChannelViewHolder channelViewHolder = (ChannelViewHolder) holder;
                    channelViewHolder.name.setText(channelInfo.channel.getChannel_name());
                    channelViewHolder.count.setText(String.valueOf(channelInfo.getProfiles().size()));
                    if (channelInfo.channel.getPin() != 0)
                        channelViewHolder.lock.setVisibility(View.VISIBLE);
                    else channelViewHolder.lock.setVisibility(View.GONE);
                    channelViewHolder.profileRecyclerView.setLayoutManager(new GridLayoutManager(context, calculateNoOfColumns(76)));
                    channelViewHolder.profileRecyclerView.setHasFixedSize(true);
                    channelViewHolder.profileRecyclerView.setAdapter(new profile_recycle_adapter(channelInfo));
                    channelViewHolder.itemView.setTag(channelInfo);
                    channelViewHolder.itemView.setOnClickListener(this);
                    channelViewHolder.profileRecyclerView.setTag(channelInfo);
                    channelViewHolder.profileRecyclerView.setOnClickListener(this);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return channels.size();
        }

        @Override
        public void onClick(View v) {
            ChannelInfo channelInfo = (ChannelInfo) v.getTag();
            Utils.vibrate(v);
            if (channelInfo.channel.getPin() != 0) MI.enterPin(channelInfo.channel);
            else MI.launchChannel(channelInfo.channel);
            dismiss();
        }

        class ChannelViewHolder extends RecyclerView.ViewHolder {
            TextView name, count;
            RecyclerView profileRecyclerView;
            ImageView lock;
            public ChannelViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.sideband_name);
                count = itemView.findViewById(R.id.user_count);
                profileRecyclerView = itemView.findViewById(R.id.profile_recyclerView);
                lock = itemView.findViewById(R.id.lock);
            }
        }

        class ChannelCreatorHolder extends RecyclerView.ViewHolder {
            TextView action;

            public ChannelCreatorHolder(@NonNull View itemView) {
                super(itemView);
                action = itemView.findViewById(R.id.create_textview);
            }
        }

        private class profile_recycle_adapter extends RecyclerView.Adapter<profile_recycle_adapter.ProfileHolder> {
            ChannelInfo channelInfo;

            private profile_recycle_adapter(ChannelInfo channelInfo) {
                this.channelInfo = channelInfo;
                List<String> newline = new ArrayList<>();
                for (String profile : channelInfo.getProfiles()) {
                    if (!profile.equals("http://truckradiosystem.com/~channel1/drawables/default.png")) {
                        newline.add(profile);
                    }
                }
                int columns = calculateNoOfColumns(76);
                int maxRows = channelInfo.getProfiles().size() / columns;
                while (newline.size() < maxRows * columns) {
                    newline.add("http://truckradiosystem.com/~channel1/drawables/default.png");
                }
                channelInfo.profiles = newline;
            }

            @NonNull
            @Override
            public ProfileHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new ProfileHolder(getLayoutInflater().inflate(R.layout.profile, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ProfileHolder holder, int position) {
                if (channelInfo.isUnlocked() || channelInfo.channel.getUser_id().equals(RadioService.operator.getUser_id()))
                    new GlideImageLoader(context, holder.profile).load(channelInfo.profiles.get(position), RadioService.profileOptions);
                else
                    new GlideImageLoader(context, holder.profile).load("https://firebasestorage.googleapis.com/v0/b/channel-19.appspot.com/o/system%2Fquestion.jpg?alt=media&token=3228922f-e41e-4d10-8edd-b52958086958", RadioService.profileOptions);
                holder.itemView.setTag(channelInfo);
                holder.itemView.setOnClickListener(SideBandAdapter.this);
            }

            @Override
            public int getItemCount() {
                return channelInfo.profiles.size();
            }

            class ProfileHolder extends RecyclerView.ViewHolder {
                ImageView profile;

                private ProfileHolder(View convertView) {
                    super(convertView);
                    profile = convertView.findViewById(R.id.option_image_view);
                }
            }
        }
    }
}

