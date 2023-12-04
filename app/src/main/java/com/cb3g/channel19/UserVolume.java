package com.cb3g.channel19;

public class UserVolume {
    String id;
    int volume;

    public UserVolume() {
    }

    public UserVolume(String id, int volume) {
        this.id = id;
        this.volume = volume;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }
}
