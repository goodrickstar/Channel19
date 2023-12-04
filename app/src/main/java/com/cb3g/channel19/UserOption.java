package com.cb3g.channel19;

public class UserOption {
    ListOption option;
    String description;

    public UserOption(ListOption option, String description) {
        this.option = option;
        this.description = description;
    }

    public ListOption getOption() {
        return option;
    }

    public void setOption(ListOption option) {
        this.option = option;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
