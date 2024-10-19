package com.cb3g.channel19;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.MessageHistoryBinding;

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

public class MessageHistory extends DialogFragment {
    private Context context;
    private MI MI;
    private GlideImageLoader glideImageLoader;

    private final HistoryAdapter adapter = new HistoryAdapter();

    private MessageHistoryBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = MessageHistoryBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioService.occupied.set(true);
        binding.historyListView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        binding.historyListView.setHasFixedSize(true);
        binding.historyListView.setAdapter(adapter);
        binding.close.setOnClickListener(v -> {
            Utils.vibrate(v);
            context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
            dismiss();
        });
        String data = context.getSharedPreferences("message_history", Context.MODE_PRIVATE).getString("data", null);
        if (data != null) adapter.updateHistory(parseJsonAndSort(data));
    }

    @Override
    public void onResume() {
        super.onResume();
        return_history();
    }

    private void return_history() {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", RadioService.operator.getUser_id());
        new OkUtil().call("user_chat_history.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && isAdded()) {
                    assert response.body() != null;
                    final String data = response.body().string();
                    context.getSharedPreferences("message_history", Context.MODE_PRIVATE).edit().putString("data", data).apply();
                    List<History> dataset = parseJsonAndSort(data);
                    binding.historyListView.post(() -> adapter.updateHistory(dataset));
                    binding.historyListView.postDelayed(() -> binding.historyListView.smoothScrollToPosition(0), 500);
                }
            }
        });
    }

    private List<History> parseJsonAndSort(String data) {
        try {
            JSONObject returnedObject = new JSONObject(data);
            JSONArray returnedList = returnedObject.getJSONArray("data");
            JSONArray onlineList = returnedObject.getJSONArray("online");
            final int timeStamp = returnedObject.getInt("time");
            final List<History> dataset = new ArrayList<>();
            for (int i = 0; i < returnedList.length(); i++) {
                History entry = RadioService.gson.fromJson(returnedList.get(i).toString(), History.class);
                final int online = onlineList.getInt(i);
                if (online >= timeStamp) entry.setOnline(0);
                else entry.setOnline(onlineList.getInt(i));
                dataset.add(entry);
            }
            dataset.sort((one, two) -> two.getOnline() - one.getOnline());
            dataset.sort((o1, o2) -> {
                if (o1.getOnline() == 0 && o2.getOnline() == 0) return 0;
                if (o1.getOnline() == 0) return -1;
                if (o2.getOnline() == 0) return 1;
                return 0;
            });
            return dataset;

        } catch (JSONException e) {
            Logger.INSTANCE.e(String.valueOf(e));
            return new ArrayList<>();
        }
    }

    private void delete_chat(String from_id) {
        context.getSharedPreferences("message_history", Context.MODE_PRIVATE).edit().remove(from_id).apply();
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", RadioService.operator.getUser_id());
        claims.put("check", from_id);
        new OkUtil().call("user_delete_all_messages.php", claims, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful() && isAdded()) {
                    try (response) {
                        assert response.body() != null;
                        JSONArray returnedList = new JSONArray(response.body().string());
                        for (int x = 0; x < returnedList.length(); x++) {
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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (com.cb3g.channel19.MI) getActivity();
        glideImageLoader = new GlideImageLoader(context);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages").setPackage("com.cb3g.channel19"));
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        RadioService.occupied.set(false);
        context.sendBroadcast(new Intent("checkForMessages").setPackage("com.cb3g.channel19"));
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.MyViewHolder> implements View.OnClickListener {
        final List<History> list = new ArrayList<>();

        public void updateHistory(List<History> data) {
            final HistoryDiffCallBack diffCallback = new HistoryDiffCallBack(list, data);
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
            list.clear();
            list.addAll(data);
            diffResult.dispatchUpdatesTo(this);
        }

        @NotNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(getLayoutInflater().inflate(R.layout.message_history_row, parent, false));
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            final History object = list.get(position);
            holder.handle.setText(object.getF_handle());
            if (object.getPhoto() == 0) holder.text.setText(object.getText());
            else holder.text.setText(R.string.sent);
            holder.menu.setTag(R.id.menu, object);
            holder.profile.setTag(R.id.black_profile_picture_iv, object);
            holder.clickPoint.setTag(R.id.clickPoint, object);
            holder.menu.setOnClickListener(this);
            holder.profile.setOnClickListener(this);
            holder.clickPoint.setOnClickListener(this);
            holder.lastOnline.setVisibility(View.VISIBLE);
            if (object.getOnline() == 0) holder.lastOnline.setText("Online");
            else
                holder.lastOnline.setText("Active" + "\n" + Utils.timeOnline(Utils.timeDifferance(object.getOnline())) + "ago");
            glideImageLoader.load(holder.profile, object.getProfileLink(), RadioService.profileOptions);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public void onClick(View v) {
            Utils.vibrate(v);
            History history = (History) v.getTag(v.getId());
            if (v.getId() == R.id.black_profile_picture_iv) {
                context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
                if (MI != null) MI.streamFile(history.getProfileLink());
            } else if (v.getId() == R.id.menu) {
                final PopupMenu popupMenu = new PopupMenu(context, v, Gravity.END, 0, R.style.PopupMenu);
                popupMenu.getMenu().add(1, R.id.delete_chat, 1, "Delete");
                popupMenu.setOnMenuItemClickListener(item -> {
                    context.sendBroadcast(new Intent("nineteenVibrate").setPackage("com.cb3g.channel19"));
                    delete_chat(history.getFrom_id());
                    List<History> newData = new ArrayList<>(list);
                    newData.remove(history);
                    this.updateHistory(newData);
                    return true;
                });
                popupMenu.show();
            } else if (v.getId() == R.id.clickPoint) {
                context.sendBroadcast(new Intent("nineteenClickSound").setPackage("com.cb3g.channel19"));
                User user = new User();
                user.setUser_id(history.getFrom_id());
                user.setRadio_hanlde(history.getF_handle());
                user.setProfileLink(history.getProfileLink());
                user.setRank(history.getProfileLink());
                if (MI != null)
                    MI.displayChat(user, false, true);
                dismiss();
            }
        }

        static class MyViewHolder extends RecyclerView.ViewHolder {
            TextView handle, text, lastOnline;
            ImageView profile, menu;
            View clickPoint;

            MyViewHolder(View v) {
                super(v);
                handle = v.findViewById(R.id.black_handle_tv);
                text = v.findViewById(R.id.text);
                profile = v.findViewById(R.id.black_profile_picture_iv);
                menu = v.findViewById(R.id.menu);
                clickPoint = v.findViewById(R.id.clickPoint);
                lastOnline = v.findViewById(R.id.last_online);
            }
        }
    }
}

