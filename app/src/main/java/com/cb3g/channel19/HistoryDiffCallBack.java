package com.cb3g.channel19;

import androidx.recyclerview.widget.DiffUtil;

import com.google.gson.Gson;

import java.util.List;

public class HistoryDiffCallBack extends DiffUtil.Callback{
    private final List<History> oldList;
    private final List<History> newList;

    private final Gson gson = new Gson();

    public HistoryDiffCallBack(List<History> oldList, List<History> newList) {
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
