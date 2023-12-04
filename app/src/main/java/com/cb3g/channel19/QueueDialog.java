package com.cb3g.channel19;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.Instant;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class QueueDialog extends DialogFragment implements View.OnClickListener {
    private final ExAdapter adapter = new ExAdapter();
    private Context context;
    private ExpandableListView listView;
    private final List<List<Talker>> talkers = new ArrayList<>();
    private final String[] titles = new String[]{"Recent Events", "Today's Newbies", "Today's Regulars", "Talker Totals", "Most Blocked", "Flag Counts"};
    private Instant now;

    private boolean auto = false;

    private SharedPreferences settings;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.que_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioService.occupied.set(true);
        final TextView ok = view.findViewById(R.id.ok);
        ok.setOnClickListener(this);
        settings = context.getSharedPreferences("settings", MODE_PRIVATE);
        listView = view.findViewById(R.id.talkers);
        listView.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        Utils.vibrate(v);
        context.sendBroadcast(new Intent("nineteenClickSound"));
        dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        gather_data();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
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

    private void gather_data() {
        final Request request = new Request.Builder()
                .url(RadioService.SITE_URL + "user_overall_data.php")
                .build();
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && isAdded()) {
                    try {
                        now = Instant.now();
                        assert response.body() != null;
                        final String data = response.body().string();
                        final JSONObject object = new JSONObject(data);
                        final List<Talker> newbies = new ArrayList<>();
                        final List<Talker> today = new ArrayList<>();
                        final List<Talker> recents = returnTalkerObjects(object.getString("recents"));
                        final List<Talker> both = returnTalkerObjects(object.getString("today"));
                        final List<Talker> totals = returnTalkerObjects(object.getString("totals"));
                        final List<Talker> blocked = returnTalkerObjects(object.getString("blocked"));
                        final List<Talker> flags = returnTalkerObjects(object.getString("flags"));
                        for (Talker child : both) {
                            if (child.newbie == 1) newbies.add(child);
                            else today.add(child);
                        }
                        recents.add(0, new Talker());
                        newbies.add(0, new Talker("0", "Combined Key-ups", combine(newbies), 0));
                        today.add(0, new Talker("0", "Combined Key-ups", combine(today), 0));
                        totals.add(0, new Talker("0", "Combined Key-ups", combine(totals), 0));
                        listView.post(() -> {
                            talkers.clear();
                            talkers.add(recents);
                            talkers.add(newbies);
                            talkers.add(today);
                            talkers.add(totals);
                            talkers.add(blocked);
                            talkers.add(flags);
                            adapter.notifyDataSetChanged();
                            int position = settings.getInt("lastOpen", -1);
                            if (position != -1) {
                                auto = true;
                                listView.expandGroup(position);
                            }
                        });
                    } catch (JSONException e) {
                        LOG.e("gather_data()", e.getMessage());
                    }
                }
            }
        });
    }

    private int combine(List<Talker> list) {
        int count = 0;
        for (Talker child : list) {
            count += child.count;
        }
        return count;
    }

    private List<Talker> returnTalkerObjects(String json) {
        try {
            List<Talker> list = RadioService.gson.fromJson(json, new TypeToken<List<Talker>>() {
            }.getType());
            if (list != null) return list;
        } catch (JsonSyntaxException e) {
            LOG.e("returnUserListObjectFromJson", e.getMessage());
        }
        return new ArrayList<>();
    }

    class ExAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return talkers.size();
        }

        @Override
        public int getChildrenCount(int i) {
            return talkers.get(i).size();
        }

        @Override
        public List<Talker> getGroup(int group) {
            return talkers.get(group);
        }

        @Override
        public Talker getChild(int group, int child) {
            return talkers.get(group).get(child);
        }

        @Override
        public long getGroupId(int i) {
            return i;
        }

        @Override
        public long getChildId(int i, int i1) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void onGroupExpanded(int groupPosition) {
            super.onGroupExpanded(groupPosition);
            if (!auto) {
                Utils.vibrate(listView);
            } else auto = false;
            settings.edit().putInt("lastOpen", groupPosition).apply();
        }

        @Override
        public void onGroupCollapsed(int groupPosition) {
            super.onGroupCollapsed(groupPosition);
            Utils.vibrate(listView);
        }

        @Override
        public View getGroupView(int group, boolean b, View convertView, ViewGroup viewGroup) {
            GroupViewHolder groupViewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.group_view, viewGroup, false);
                groupViewHolder = new GroupViewHolder(convertView);
                convertView.setTag(groupViewHolder);
            }
            groupViewHolder = (GroupViewHolder) convertView.getTag();
            groupViewHolder.title.setText(titles[group]);
            if (group == 0)
                groupViewHolder.count.setText(formatInt(getChildrenCount(group)));
            if (group == 1 || group == 2)
                groupViewHolder.count.setText(formatInt(getChildrenCount(group) - 1));
            else groupViewHolder.count.setText("");
            return convertView;
        }

        @Override
        public View getChildView(final int group, int child, boolean b, View convertView, ViewGroup viewGroup) {
            ChildViewHolder childViewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.child_view, viewGroup, false);
                childViewHolder = new ChildViewHolder(convertView);
                convertView.setTag(childViewHolder);
            }
            childViewHolder = (ChildViewHolder) convertView.getTag();
            childViewHolder.title.setPaintFlags(0);
            childViewHolder.count.setTextColor(Color.WHITE);
            switch (group) {
                case 0:
                    if (child == 0) {
                        childViewHolder.title.setText("Event");
                        childViewHolder.count.setText("Elapsed Time");
                    } else {
                        String elapsed = Utils.showElapsed(Long.parseLong(talkers.get(group).get(child).userId), now);
                        if (elapsed.equals("0:01")) elapsed = "just now";
                        childViewHolder.title.setText(talkers.get(group).get(child).handle);
                        childViewHolder.count.setText(elapsed);
                    }
                    break;
                case 1:
                case 2:
                    childViewHolder.title.setText(talkers.get(group).get(child).handle);
                    if (talkers.get(group).get(child).count > 0 && child != 0)
                        childViewHolder.count.setText(Utils.round((double) (talkers.get(group).get(child).count * 100.f) / talkers.get(group).get(0).count, 1) + "% | " + formatInt(talkers.get(group).get(child).count));
                    else
                        childViewHolder.count.setText(formatInt(talkers.get(group).get(child).count));
                    break;
                case 3:
                    Talker talk = talkers.get(group).get(child);
                    if (child > 9)
                        childViewHolder.title.setText(String.format(Locale.US, "%d) %s", child, talk.handle));
                    else {
                        if (child != 0)
                            childViewHolder.title.setText(String.format(Locale.US, "0%d) %s", child, talk.handle));
                        else
                            childViewHolder.title.setText(talk.handle);
                    }
                    if (talkers.get(group).get(child).count > 0 && child != 0)
                        childViewHolder.count.setText(Utils.round((double) (talkers.get(group).get(child).count * 100.f) / talkers.get(group).get(0).count, 1) + "% | " + formatInt(talkers.get(group).get(child).count));
                    else
                        childViewHolder.count.setText(formatInt(talkers.get(group).get(child).count));
                    break;
                case 4:
                    Talker blocked = talkers.get(group).get(child);
                    childViewHolder.title.setText(blocked.handle);
                    childViewHolder.count.setText(String.valueOf(blocked.count));
                case 5:
                    Talker flagged = talkers.get(group).get(child);
                    childViewHolder.title.setText(flagged.handle);
                    childViewHolder.count.setText(String.valueOf(flagged.count));
                    if (flagged.count > 19) {
                        childViewHolder.title.setPaintFlags(childViewHolder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        childViewHolder.count.setTextColor(Color.RED);
                    }
                    break;
            }
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int i, int i1) {
            return false;
        }

        private String formatInt(int count) {
            return NumberFormat.getNumberInstance(Locale.US).format(count);
        }

        private class GroupViewHolder {
            TextView title, count;

            private GroupViewHolder(View group) {
                title = group.findViewById(R.id.title);
                count = group.findViewById(R.id.count);
            }
        }

        private class ChildViewHolder {
            TextView title, count;

            private ChildViewHolder(View group) {
                title = group.findViewById(R.id.title);
                count = group.findViewById(R.id.count);
            }
        }
    }

    private static class Talker {
        String handle, userId;
        int count, newbie;

        Talker() {
        }

        Talker(String userId, String handle, int count, int newbie) {
            this.userId = userId;
            this.handle = handle;
            this.count = count;
            this.newbie = newbie;
        }
    }
}

