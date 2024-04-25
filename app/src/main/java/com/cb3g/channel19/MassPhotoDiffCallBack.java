package com.cb3g.channel19;

import androidx.recyclerview.widget.DiffUtil;

import com.google.gson.Gson;

import java.util.List;

public class MassPhotoDiffCallBack extends DiffUtil.Callback{
    private final List<Photo> oldList;
    private final List<Photo> newList;

    private final Gson gson = new Gson();

    public MassPhotoDiffCallBack(List<Photo> oldList, List<Photo> newList) {
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
        return oldList.get(oldItemPosition).getKey().equals(newList.get(newItemPosition).getKey());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return gson.toJson(oldList.get(oldItemPosition)).equals(gson.toJson(newList.get(newItemPosition)));
    }
}
