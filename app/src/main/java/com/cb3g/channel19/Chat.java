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
import android.util.Log;
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
import androidx.fragment.app.FragmentManager;
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
import java.util.Objects;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class Chat extends DialogFragment implements View.OnClickListener {
    private final ChatAdapter adapter = new ChatAdapter();
    private Context context;
    private ChatBinding binding;
    private MI MI;
    private boolean launchHistory = false;
    private int screenWidth = 0;
    private final User user;
    private final FragmentManager fragmentManager;
    private GlideImageLoader glideImageLoader;

    public Chat(FragmentManager fragmentManager, User user) {
        this.fragmentManager = fragmentManager;
        this.user = user;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = ChatBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        launchHistory = requireArguments().getBoolean("launch");
        screenWidth = Utils.getScreenWidth(requireActivity());
        glideImageLoader.load(binding.chatProfilePictureIv, user.getProfileLink(), RadioService.profileOptions);
        glideImageLoader.load(binding.chatStarIv, Utils.parseRankUrl(user.getRank()));
        binding.chatHandleTv.setText(user.getRadio_hanlde());
        binding.chatView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true));
        binding.chatView.setHasFixedSize(true);
        binding.chatView.setAdapter(adapter);
        binding.imageBox.setOnClickListener(this);
        binding.editBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() != 0) binding.imageBox.setImageResource(R.drawable.send);
                else binding.imageBox.setImageResource(R.drawable.gallery);
            }
        });
        String data = context.getSharedPreferences("message_history", Context.MODE_PRIVATE).getString(user.getUser_id(), null);
        if (data != null) adapter.updateData(parseJson(data));
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
        RadioService.chat.set(user.getUser_id());
        glideImageLoader = new GlideImageLoader(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        gather_history(false, false);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        RadioService.chat.set("0");
        if (MI != null && launchHistory) MI.display_message_history();
    }

    @Override
    public void onClick(View v) {
        Utils.vibrate(v);
        context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
        if (MI != null) {
            if (binding.editBox.getText().length() != 0) reply();
            else {
                ImagePicker imagePicker = (ImagePicker) fragmentManager.findFragmentByTag("imagePicker");
                if (imagePicker == null) {
                    imagePicker = new ImagePicker(fragmentManager, user, RequestCode.PRIVATE_PHOTO);
                    imagePicker.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
                    imagePicker.show(fragmentManager, "imagePicker");
                }
            }
        }
    }

    public void gather_history(final boolean sound, boolean scroll) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", RadioService.operator.getUser_id());
        claims.put("check", user.getUser_id());
        new OkUtil().call("user_chat.php", claims, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                binding.chatView.post(() -> binding.loading.setVisibility(View.GONE));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful() && isAdded()) {
                    if (sound)
                        context.sendBroadcast(new Intent("nineteenChatSound").setPackage("com.cb3g.channel19"));
                    assert response.body() != null;
                    final String data = response.body().string();
                    context.getSharedPreferences("message_history", Context.MODE_PRIVATE).edit().putString(user.getUser_id(), data).apply();
                    binding.chatView.post(() -> {
                        adapter.updateData(parseJson(data));
                        if (scroll) {
                            RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(context) {
                                @Override
                                protected int getVerticalSnapPreference() {
                                    return LinearSmoothScroller.SNAP_TO_START;
                                }
                            };
                            smoothScroller.setTargetPosition(0);
                            Objects.requireNonNull(binding.chatView.getLayoutManager()).startSmoothScroll(smoothScroller);
                        }
                    });
                }
            }
        });
    }

    private List<ChatRow> parseJson(String data) {
        try {
            JSONObject total = new JSONObject(data);
            if (total.getBoolean("online")) binding.stamp.setText("Online");
            else binding.stamp.setText("Offline");
            JSONArray quick = new JSONArray(total.getString("history"));
            List<ChatRow> newList = new ArrayList<>();
            for (int x = 0; x < quick.length(); x++) {
                newList.add(RadioService.gson.fromJson(quick.getJSONObject(x).toString(), ChatRow.class));
            }
            return newList;
        } catch (JSONException e) {
            Logger.INSTANCE.e(String.valueOf(e));
            return new ArrayList<>();
        }
    }

    private void reply() {
        final String text = binding.editBox.getText().toString().trim();
        if (text.isEmpty()) return;
        binding.editBox.setText("");
        context.sendBroadcast(new Intent("nineteenSendPM").setPackage("com.cb3g.channel19").putExtra("text", text).putExtra("id", user.getUser_id()));
    }

    private class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<ChatRow> list = new ArrayList<>();

        public void updateData(List<ChatRow> data) {
            binding.loading.setVisibility(View.GONE);
            final ChatDiffCallBack diffCallback = new ChatDiffCallBack(list, data);
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
            list.clear();
            list.addAll(data);
            diffResult.dispatchUpdatesTo(this);
            if (list.isEmpty()) Utils.showKeyboard(context, binding.editBox);
        }

        @NotNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                return new ChatTextHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_row, parent, false));
            }
            return new ChatPhotoHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_row_photo, parent, false));
        }

        @Override
        public int getItemViewType(int position) {
            return list.get(position).getPhoto();
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            final ChatRow chatRow = list.get(position);
            switch (holder.getItemViewType()) {
                case 0 -> {
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
                            if (id == R.id.delete_message) {
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
                }
                case 1 -> {
                    ChatPhotoHolder photoHolder = (ChatPhotoHolder) holder;
                    final int new_height = (int) (((chatRow.getHeight() * screenWidth) / chatRow.getWidth()) * .8);
                    final int new_width = (int) (screenWidth * .8);
                    photoHolder.image.getLayoutParams().height = new_height;
                    photoHolder.image.getLayoutParams().width = new_width;
                    glideImageLoader.load(photoHolder.image, photoHolder.loading, Uri.parse(chatRow.getUrl()).toString());
                    photoHolder.image.setOnClickListener(v -> {
                        Utils.vibrate(v);
                        if (MI != null) MI.streamFile(chatRow.getUrl());
                    });
                    photoHolder.image.setOnLongClickListener(v -> {
                        Utils.vibrate(v);
                        final PopupMenu popupMenu = new PopupMenu(context, v, Gravity.END, 0, R.style.PopupMenu);
                        popupMenu.getMenu().add(1, R.id.save_photo, 1, "Save Image");
                        popupMenu.getMenu().add(1, R.id.delete_message, 2, "Delete");
                        popupMenu.setOnMenuItemClickListener(item -> {
                            Utils.vibrate(v);
                            int id = item.getItemId();
                            if (id == R.id.delete_message) {
                                delete_message(chatRow.getMessage_id(), chatRow.getUrl());
                            } else if (id == R.id.copy_text) {
                                context.sendBroadcast(new Intent("savePhotoToDisk").setPackage("com.cb3g.channel19").putExtra("url", chatRow.getUrl()));
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
                }
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        private static class ChatPhotoHolder extends RecyclerView.ViewHolder {
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

        private static class ChatTextHolder extends RecyclerView.ViewHolder {
            TextView text;
            RelativeLayout border;

            private ChatTextHolder(View convertView) {
                super(convertView);
                text = convertView.findViewById(R.id.text);
                border = convertView.findViewById(R.id.border);
            }
        }

        private void delete_message(final String messageId, final String url) {
            final Map<String, Object> claims = new HashMap<>();
            claims.put("userId", RadioService.operator.getUser_id());
            claims.put("check", user.getUser_id());
            claims.put("messageId", messageId);
            new OkUtil().call("user_delete_message.php", claims, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {

                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    if (response.isSuccessful() && isAdded()) {
                        if (url != null) try {
                            RadioService.storage.getReferenceFromUrl(url).delete();
                        } catch (IllegalArgumentException e) {
                            Logger.INSTANCE.e("delete_message() IllegalArgumentException " + e);
                        }
                        binding.chatView.post(() -> gather_history(false, false));
                    }
                }
            });
        }
    }

}

