package com.cb3g.channel19;

import androidx.recyclerview.widget.DiffUtil;

import com.google.gson.Gson;

import java.util.List;

public class PostDiffCallBack extends DiffUtil.Callback{
    private final List<Post> oldList;
    private final List<Post> newList;

    private final Gson gson = new Gson();

    public PostDiffCallBack(List<Post> oldList, List<Post> newList) {
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
        return oldList.get(oldItemPosition).getPostId().equals(newList.get(newItemPosition).getPostId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return gson.toJson(oldList.get(oldItemPosition)).equals(gson.toJson(newList.get(newItemPosition)));
    }
}
