package com.cb3g.channel19;
import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.ChatBinding;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class Chat extends DialogFragment implements View.OnClickListener {
    private final recycle_adapter adapter = new recycle_adapter();
    private final List<ChatRow> list = new ArrayList<>();
    private Context context;
    private RecyclerView chat_view;
    private TextView stamp;
    private MI MI;
    private boolean launchHistory = false;
    private ImageView starIV;
    private int screenWidth = 0;
    private UserListEntry user;
    private ProgressBar loading;

    private ChatBinding binding;


    @Override
    public void onClick(View v) {
        Utils.vibrate(v);
        context.sendBroadcast(new Intent("nineteenClickSound"));
        if (MI != null) {
            if (binding.editBox.getText().length() != 0) reply();
            else MI.sendPhoto(user.getUser_id(), user.getRadio_hanlde());
        }
    }

    public void gather_history(final boolean sound, boolean scroll) {
        final String data = Jwts.builder()
                .setHeader(RadioService.header)
                .claim("userId", RadioService.operator.getUser_id())
                .claim("check", user.getUser_id())
                .signWith(SignatureAlgorithm.HS256, RadioService.operator.getKey())
                .compact();
        final Request request = new Request.Builder()
                .url(RadioService.SITE_URL + "user_chat.php")
                .post(new FormBody.Builder().add("data", data).build())
                .build();
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                chat_view.post(() -> loading.setVisibility(View.GONE));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful() && isAdded()) {
                    if (sound) context.sendBroadcast(new Intent("nineteenChatSound"));
                    final String data = response.body().string();
                    chat_view.post(() -> {
                        loading.setVisibility(View.GONE);
                        try {
                            JSONObject total = new JSONObject(data);
                            new GlideImageLoader(context, starIV).load(Utils.parseRankUrl(total.getString("rank")));
                            stamp.setText("Active " + Utils.timeOnline(Utils.timeDifferance(total.getInt("last_online"))) + " ago");
                            JSONArray quick = new JSONArray(total.getString("history"));
                            List<ChatRow> newList = new ArrayList<>();
                            for (int x = 0; x < quick.length(); x++) {
                                newList.add(RadioService.gson.fromJson(quick.getJSONObject(x).toString(), ChatRow.class));
                            }
                            DiffUtil.DiffResult results = DiffUtil.calculateDiff(new MyDiffCallback(newList, list));
                            list.clear();
                            list.addAll(newList);
                            results.dispatchUpdatesTo(adapter);
                            if (scroll) {
                                RecyclerView.SmoothScroller smoothScroller = new
                                        LinearSmoothScroller(context) {
                                            @Override
                                            protected int getVerticalSnapPreference() {
                                                return LinearSmoothScroller.SNAP_TO_START;
                                            }
                                        };
                                smoothScroller.setTargetPosition(0);
                                chat_view.getLayoutManager().startSmoothScroll(smoothScroller);
                            }
                        } catch (JSONException e) {
                            Logger.INSTANCE.e(String.valueOf(e));
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
        RadioService.chat.set(user.getUser_id());
    }

    @Override
    public void onResume() {
        super.onResume();
        gather_history(false, false);
        Utils.hideKeyboard(context, binding.editBox);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = ChatBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        user = RadioService.gson.fromJson(getArguments().getString("data"), UserListEntry.class);
        final TextView title = v.findViewById(R.id.handle);
        final ImageView profile = v.findViewById(R.id.option_image_view);
        loading = v.findViewById(R.id.loading);
        starIV = v.findViewById(R.id.starIV);
        stamp = v.findViewById(R.id.stamp);
        new GlideImageLoader(context, profile).load(user.getProfileLink(), RadioService.profileOptions);
        chat_view = v.findViewById(R.id.chat_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        linearLayoutManager.setReverseLayout(true);
        chat_view.setLayoutManager(linearLayoutManager);
        chat_view.setHasFixedSize(true);
        chat_view.setAdapter(adapter);
        launchHistory = getArguments().getBoolean("launch");
        title.setText(user.getRadio_hanlde());
        final ImageView image_selector = v.findViewById(R.id.imageBox);
        image_selector.setOnClickListener(this);
        binding.editBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() != 0) image_selector.setImageResource(R.drawable.send);
                else image_selector.setImageResource(R.drawable.gallery);
            }
        });
        screenWidth = Utils.getScreenWidth(getActivity());
    }

    private void reply() {
        final String text = binding.editBox.getText().toString().trim();
        if (text.isEmpty()) return;
        binding.editBox.setText("");
        context.sendBroadcast(new Intent("nineteenSendPM").putExtra("text", text).putExtra("id", user.getUser_id()));
    }

    private void delete_message(final String messageId, final String url) {
        final Map<String, Object> header = new HashMap<>();
        header.put("typ", Header.JWT_TYPE);
        final String data = Jwts.builder()
                .setHeader(header)
                .claim("userId", RadioService.operator.getUser_id())
                .claim("check", user.getUser_id())
                .claim("messageId", messageId)
                .signWith(SignatureAlgorithm.HS256, RadioService.operator.getKey())
                .compact();
        final Request request = new Request.Builder()
                .url(RadioService.SITE_URL + "user_delete_message.php")
                .post(new FormBody.Builder().add("data", data).build())
                .build();
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (response.isSuccessful() && isAdded()) {
                    if (url != null) try {
                        RadioService.storage.getReferenceFromUrl(url).delete();
                    } catch (IllegalArgumentException e) {
                        Logger.INSTANCE.e("delete_message() IllegalArgumentException " + e);
                    }
                    chat_view.post(() -> gather_history(false, false));
                }
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        RadioService.chat.set("0");
        if (MI != null && launchHistory) MI.display_message_history();
    }

    private class recycle_adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NotNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case 0:
                    return new ChatTextHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_row, parent, false));
                case 1:
                    return new ChatPhotoHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_row_photo, parent, false));
            }
            return null;
        }

        @Override
        public int getItemViewType(int position) {
            return list.get(position).getPhoto();
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            final ChatRow chatRow = list.get(position);
            switch (holder.getItemViewType()) {
                case 0:
                    ChatTextHolder textHolder = (ChatTextHolder) holder;
                    textHolder.text.setText(chatRow.getText());
                    textHolder.text.setOnLongClickListener(v -> {
                        Utils.vibrate(v);
                        final PopupMenu popupMenu = new PopupMenu(context, v, Gravity.CENTER, 0, R.style.PopupMenu);
                        popupMenu.getMenu().add(1, R.id.copy_text, 1, "Copy Text");
                        popupMenu.getMenu().add(1, R.id.delete_message, 2, "Delete");
                        popupMenu.setOnMenuItemClickListener(item -> {
                            Utils.vibrate(v);
                            int id = item.getItemId();
                            if (id == R.id.delete_message){
                                delete_message(chatRow.getMessage_id(), null);
                            } else if (id == R.id.copy_text) {
                                final String handle = chatRow.getF_handle();
                                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText(handle, chatRow.getText());
                                clipboard.setPrimaryClip(clip);
                            }
                            return true;
                        });
                        popupMenu.show();
                        return false;
                    });
                    final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    if (chatRow.getFrom_id().equals(RadioService.operator.getUser_id())) {
                        textHolder.border.setBackgroundResource(R.drawable.black_box_white_outline);
                        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        params.setMargins(150, 40, 25, 0);
                    } else {
                        textHolder.border.setBackgroundResource(R.drawable.blue_box_white_outline_transparent);
                        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        params.setMargins(25, 40, 150, 0);
                    }
                    textHolder.border.setPadding(25, 25, 25, 25);
                    textHolder.border.setLayoutParams(params);
                    break;
                case 1:
                    ChatPhotoHolder photoHolder = (ChatPhotoHolder) holder;
                    final int new_height = (int) (((chatRow.getHeight() * screenWidth) / chatRow.getWidth()) * .8);
                    final int new_width = (int) (screenWidth * .8);
                    photoHolder.image.getLayoutParams().height = new_height;
                    photoHolder.image.getLayoutParams().width = new_width;
                    new GlideImageLoader(context, photoHolder.image, photoHolder.loading).load(Uri.parse(chatRow.getUrl()).toString());
                    photoHolder.image.setOnClickListener(v -> {
                        Utils.vibrate(v);
                        if (MI != null)
                            MI.streamFile(chatRow.getUrl());
                    });
                    photoHolder.image.setOnLongClickListener(v -> {
                        Utils.vibrate(v);
                        final PopupMenu popupMenu = new PopupMenu(context, v, Gravity.END, 0, R.style.PopupMenu);
                        popupMenu.getMenu().add(1, R.id.save_photo, 1, "Save Image");
                        popupMenu.getMenu().add(1, R.id.delete_message, 2, "Delete");
                        popupMenu.setOnMenuItemClickListener(item -> {
                            Utils.vibrate(v);
                            int id = item.getItemId();
                            if (id == R.id.delete_message){
                                delete_message(chatRow.getMessage_id(), chatRow.getUrl());
                            } else if (id == R.id.copy_text) {
                                context.sendBroadcast(new Intent("savePhotoToDisk").putExtra("url", chatRow.getUrl()));
                            }
                            return true;
                        });
                        popupMenu.show();
                        return false;
                    });
                    final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    if (chatRow.getFrom_id().equals(RadioService.operator.getUser_id())) {
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        layoutParams.setMargins(150, 40, 25, 0);
                    } else {
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        layoutParams.setMargins(25, 40, 150, 0);
                    }
                    photoHolder.border.setLayoutParams(layoutParams);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        private class ChatPhotoHolder extends RecyclerView.ViewHolder {
            ImageView image;
            RelativeLayout border;
            ProgressBar loading;

            private ChatPhotoHolder(View convertView) {
                super(convertView);
                image = convertView.findViewById(R.id.image);
                border = convertView.findViewById(R.id.border);
                loading = convertView.findViewById(R.id.loading);
            }
        }

        private class ChatTextHolder extends RecyclerView.ViewHolder {
            TextView text;
            RelativeLayout border;

            private ChatTextHolder(View convertView) {
                super(convertView);
                text = convertView.findViewById(R.id.text);
                border = convertView.findViewById(R.id.border);
            }
        }
    }

    public static class MyDiffCallback extends DiffUtil.Callback {

        List<ChatRow> oldMessages;
        List<ChatRow> newMessages;

        public MyDiffCallback(List<ChatRow> newMessages, List<ChatRow> oldMessages) {
            this.newMessages = newMessages;
            this.oldMessages = oldMessages;
        }

        @Override
        public int getOldListSize() {
            return oldMessages.size();
        }

        @Override
        public int getNewListSize() {
            return newMessages.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldMessages.get(oldItemPosition).getMessage_id().equals(newMessages.get(newItemPosition).getMessage_id());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldMessages.get(oldItemPosition).equals(newMessages.get(newItemPosition));
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            //you can return particular field for changed item.
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }
}

