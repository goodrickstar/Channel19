package com.cb3g.channel19;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.Instant;

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
public class MessageHistory extends DialogFragment {
    private final recycler_adapter adapter = new recycler_adapter();
    private Context context;
    private List<History> list = new ArrayList<>();
    private MI MI;
    private RecyclerView history;
    private long now = Instant.now().getEpochSecond();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Window window = getDialog().getWindow();
        if (window != null)
            window.getAttributes().windowAnimations = R.style.photoAnimation;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Window window = getDialog().getWindow();
        if (window != null) window.setGravity(Gravity.CENTER);
        return inflater.inflate(R.layout.message_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioService.occupied.set(true);
        history = view.findViewById(R.id.history);
        history.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        history.setHasFixedSize(true);
        history.setAdapter(adapter);
        history.setAdapter(adapter);
        final TextView close = view.findViewById(R.id.close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.vibrate(v);
                context.sendBroadcast(new Intent("nineteenClickSound"));
                dismiss();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        return_history();
    }

    private void return_history() {
        final String data = Jwts.builder()
                .setHeader(RadioService.header)
                .claim("userId", RadioService.operator.getUser_id())
                .signWith(SignatureAlgorithm.HS256, RadioService.operator.getKey())
                .compact();
        final Request request = new Request.Builder()
                .url(RadioService.SITE_URL + "user_chat_history.php")
                .post(new FormBody.Builder().add("data", data).build())
                .build();
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful() && isAdded()) {
                    final String data = response.body().string();
                    history.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                now = Instant.now().getEpochSecond();
                                JSONObject returnedObject = new JSONObject(data);
                                JSONArray returnedList = returnedObject.getJSONArray("data");
                                JSONArray onlineList = returnedObject.getJSONArray("online");
                                list.clear();
                                for (int i = 0; i < returnedList.length(); i++) {
                                    History entry = RadioService.gson.fromJson(returnedList.get(i).toString(), History.class);
                                    try {
                                        entry.setOnline(onlineList.getInt(i));
                                    } catch (JSONException e) {
                                        Logger.INSTANCE.e("entry.setOnline", String.valueOf(e));
                                    }
                                    list.add(entry);
                                }
                                adapter.notifyDataSetChanged();
                            } catch (JSONException e) {
                                Logger.INSTANCE.e(String.valueOf(e));
                            }
                        }
                    });
                }
            }
        });
    }

    private void delete_chat(String from_id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getFrom_id().equals(from_id)) {
                list.remove(i);
                adapter.notifyItemRemoved(i);
            }
        }
        final Map<String, Object> header = new HashMap<>();
        header.put("typ", Header.JWT_TYPE);
        final String data = Jwts.builder()
                .setHeader(header)
                .claim("userId", RadioService.operator.getUser_id())
                .claim("check", from_id)
                .signWith(SignatureAlgorithm.HS256, RadioService.operator.getKey())
                .compact();
        final Request request = new Request.Builder()
                .url(RadioService.SITE_URL + "user_delete_all_messages.php")
                .post(new FormBody.Builder().add("data", data).build())
                .build();
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful() && isAdded()) {
                    try {
                        JSONArray returnedList = new JSONArray(response.body().string());
                        for (int x = 0; x < data.length(); x++) {
                            try {
                                RadioService.storage.getReferenceFromUrl(returnedList.getString(x)).delete();
                            } catch (IllegalArgumentException e) {
                                Logger.INSTANCE.e("delete_chat() IllegalArgumentException " + e);
                            }
                        }
                    } catch (JSONException e) {
                        Logger.INSTANCE.e("delete_chat() " + e);
                    }
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
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages"));
    }

    private class recycler_adapter extends RecyclerView.Adapter<MessageHistory.recycler_adapter.MyViewHolder> {
        private View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.vibrate(v);
                History history = (History) v.getTag(v.getId());
                int id = v.getId();
                if (id == R.id.option_image_view) {
                    context.sendBroadcast(new Intent("nineteenClickSound"));
                    if (MI != null) MI.streamFile(history.getProfileLink());
                } else if (id == R.id.menu) {
                    final PopupMenu popupMenu = new PopupMenu(context, v, Gravity.END, 0, R.style.PopupMenu);
                    popupMenu.getMenu().add(1, R.id.delete_chat, 1, "Delete");
                    popupMenu.setOnMenuItemClickListener(item -> {
                        context.sendBroadcast(new Intent("nineteenVibrate"));
                        delete_chat(history.getFrom_id());
                        return false;
                    });
                    popupMenu.show();
                } else if (id == R.id.clickPoint) {
                    context.sendBroadcast(new Intent("nineteenClickSound"));
                    UserListEntry user = new UserListEntry();
                    user.setUser_id(history.getFrom_id());
                    user.setRadio_hanlde(history.getF_handle());
                    user.setProfileLink(history.getProfileLink());
                    user.setRank(history.getProfileLink());
                    if (MI != null)
                        MI.displayChat(user, false, true);
                    dismiss();
                }
            }
        };

        @NotNull
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.message_history_row, parent, false));
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            final History object = list.get(position);
            holder.handle.setText(object.getF_handle());
            if (object.getPhoto() == 0) holder.text.setText(object.getText());
            else holder.text.setText(R.string.sent);
            holder.menu.setTag(R.id.menu, object);
            holder.profile.setTag(R.id.option_image_view, object);
            holder.clickPoint.setTag(R.id.clickPoint, object);
            holder.menu.setOnClickListener(listener);
            holder.profile.setOnClickListener(listener);
            holder.clickPoint.setOnClickListener(listener);
            holder.lastOnline.setVisibility(View.VISIBLE);
            if (object.getOnline() == 0) holder.lastOnline.setText("Online");
            else
                holder.lastOnline.setText("Active" + "\n" + Utils.timeOnline(Utils.timeDifferance(object.getOnline())) + " ago");
            new GlideImageLoader(context, holder.profile).load(object.getProfileLink(), RadioService.profileOptions);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView handle, text, lastOnline;
            ImageView profile, menu;
            View clickPoint;

            MyViewHolder(View v) {
                super(v);
                handle = v.findViewById(R.id.handle);
                text = v.findViewById(R.id.text);
                profile = v.findViewById(R.id.option_image_view);
                menu = v.findViewById(R.id.menu);
                clickPoint = v.findViewById(R.id.clickPoint);
                lastOnline = v.findViewById(R.id.last_online);
            }
        }
    }
}

