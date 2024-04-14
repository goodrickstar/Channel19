package com.cb3g.channel19;

import androidx.recyclerview.widget.DiffUtil;

import com.google.gson.Gson;

import java.util.List;

public class UserDiffCallback extends DiffUtil.Callback {

    private final List<UserListEntry> newUsers;
    private final List<UserListEntry> oldUsers;
    private final Gson gson = new Gson();

    public UserDiffCallback(List<UserListEntry> oldUsers, List<UserListEntry> newUsers) {
        this.oldUsers = oldUsers;
        this.newUsers = newUsers;
    }

    @Override
    public int getOldListSize() {
        return oldUsers.size();
    }

    @Override
    public int getNewListSize() {
        return newUsers.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldUsers.get(oldItemPosition).getUser().getUser_id().equals(newUsers.get(
                newItemPosition).getUser().getUser_id());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        final UserListEntry oldUser = oldUsers.get(oldItemPosition);
        final UserListEntry newUser = newUsers.get(newItemPosition);

        return gson.toJson(oldUser).equals(gson.toJson(newUser));
    }

}