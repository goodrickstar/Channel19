package com.cb3g.channel19;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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

import static android.content.Context.CLIPBOARD_SERVICE;

public class Chat extends DialogFragment implements View.OnClickListener {
    private final recycle_adapter adapter = new recycle_adapter();
    private InputMethodManager methodManager;
    private EditText editBox;
    private List<ChatRow> list = new ArrayList<>();
    private Context context;
    private RecyclerView chat_view;
    private TextView stamp;
    private MI MI;
    private boolean launchHistory = false;
    private ImageView starIV;
    private int screenWidth = 0;
    private UserListEntry user;
    private ProgressBar loading;


    @Override
    public void onClick(View v) {
        context.sendBroadcast(new Intent("nineteenVibrate"));
        context.sendBroadcast(new Intent("nineteenClickSound"));
        if (MI != null) {
            if (editBox.getText().length() != 0) reply();
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
                chat_view.post(new Runnable() {
                    @Override
                    public void run() {
                        loading.setVisibility(View.GONE);
                    }
                });
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
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        gather_history(false, false);
        methodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        RadioService.chat.set("0");
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.chat.set("0");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Window window = getDialog().getWindow();
        if (window != null) window.getAttributes().windowAnimations = R.style.photoAnimation;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        user = RadioService.gson.fromJson(getArguments().getString("data"), UserListEntry.class);
        RadioService.chat.set(user.getUser_id());
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
        methodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        title.setText(user.getRadio_hanlde());
        editBox = v.findViewById(R.id.editBox);
        final ImageView image_selector = v.findViewById(R.id.imageBox);
        image_selector.setOnClickListener(this);
        editBox.addTextChangedListener(new TextWatcher() {
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
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenWidth = displaymetrics.widthPixels;
    }

    private void reply() {
        final String text = editBox.getText().toString().trim();
        if (text.isEmpty()) return;
        editBox.setText("");
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
                    chat_view.post(new Runnable() {
                        @Override
                        public void run() {
                            gather_history(false, false);
                        }
                    });
                }
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                if (MI != null && launchHistory) MI.display_message_history();
                dismiss();
            }
        };
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
                        context.sendBroadcast(new Intent("nineteenVibrate"));
                        final PopupMenu popupMenu = new PopupMenu(context, v, Gravity.CENTER, 0, R.style.PopupMenu);
                        popupMenu.getMenu().add(1, R.id.copy_text, 1, "Copy Text");
                        popupMenu.getMenu().add(1, R.id.delete_message, 2, "Delete");
                        popupMenu.setOnMenuItemClickListener(item -> {
                            context.sendBroadcast(new Intent("nineteenVibrate"));
                            switch (item.getItemId()) {
                                case R.id.delete_message:
                                    delete_message(chatRow.getMessage_id(), null);
                                    return true;
                                case R.id.copy_text:
                                    final String handle = chatRow.getF_handle();
                                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
                                    ClipData clip = ClipData.newPlainText(handle, chatRow.getText());
                                    clipboard.setPrimaryClip(clip);
                                    return true;
                            }
                            return false;
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
                        context.sendBroadcast(new Intent("nineteenVibrate"));
                        if (MI != null)
                            MI.streamFile(chatRow.getUrl());
                    });
                    photoHolder.image.setOnLongClickListener(v -> {
                        context.sendBroadcast(new Intent("nineteenVibrate"));
                        final PopupMenu popupMenu = new PopupMenu(context, v, Gravity.END, 0, R.style.PopupMenu);
                        popupMenu.getMenu().add(1, R.id.save_photo, 1, "Save Image");
                        popupMenu.getMenu().add(1, R.id.delete_message, 2, "Delete");
                        popupMenu.setOnMenuItemClickListener(item -> {
                            context.sendBroadcast(new Intent("nineteenVibrate"));
                            switch (item.getItemId()) {
                                case R.id.delete_message:
                                    delete_message(chatRow.getMessage_id(), chatRow.getUrl());
                                    return true;
                                case R.id.save_photo:
                                    context.sendBroadcast(new Intent("savePhotoToDisk").putExtra("url", chatRow.getUrl()));
                                    return true;
                            }
                            return false;
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

    public class MyDiffCallback extends DiffUtil.Callback {

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

