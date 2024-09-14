package com.cb3g.channel19;

import androidx.recyclerview.widget.DiffUtil;

import com.google.gson.Gson;

import java.util.List;

public class ChatDiffCallBack extends DiffUtil.Callback{
    private final List<ChatRow> oldList;
    private final List<ChatRow> newList;

    private final Gson gson = new Gson();

    public ChatDiffCallBack(List<ChatRow> oldList, List<ChatRow> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }
    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getFrom_id().equals(newList.get(newItemPosition).getFrom_id());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return gson.toJson(oldList.get(oldItemPosition)).equals(gson.toJson(newList.get(newItemPosition)));
    }
}
