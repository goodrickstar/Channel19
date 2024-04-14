package com.cb3g.channel19;

public class UserListEntry {
    private User user;
    private boolean silenced;
    private boolean ghost;
    private boolean paused;
    private boolean onCall;
    private boolean autoSkipped;

    public UserListEntry(User user, boolean silenced, boolean ghost, boolean paused, boolean onCall, boolean autoSkipped) {
        this.user = user;
        this.silenced = silenced;
        this.ghost = ghost;
        this.paused = paused;
        this.onCall = onCall;
        this.autoSkipped = autoSkipped;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isSilenced() {
        return silenced;
    }

    public void setSilenced(boolean silenced) {
        this.silenced = silenced;
    }

    public boolean isGhost() {
        return ghost;
    }

    public void setGhost(boolean ghost) {
        this.ghost = ghost;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isOnCall() {
        return onCall;
    }

    public void setOnCall(boolean onCall) {
        this.onCall = onCall;
    }

    public boolean isAutoSkipped() {
        return autoSkipped;
    }

    public void setAutoSkipped(boolean autoSkipped) {
        this.autoSkipped = autoSkipped;
    }
}
