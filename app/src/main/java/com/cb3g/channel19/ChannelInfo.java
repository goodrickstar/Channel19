package com.cb3g.channel19;
import java.util.ArrayList;
import java.util.List;
public class ChannelInfo {
    Channel channel;
    List<String> profiles = new ArrayList<>();
    int type = 1;
    boolean unlocked = false;

    public ChannelInfo(int type) {
        this.type = type;
    }

    public ChannelInfo(Channel channel, List<String> profiles) {
        this.channel = channel;
        this.profiles = profiles;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public int getType() {
        return type;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }
}

