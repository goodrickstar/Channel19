package com.cb3g.channel19;

public interface SI {
    void launchPicker(Gif gif, boolean upload);
    void launchSearch(String id);
    void selectFromDisk();
    void photoChosen(Gif gif, boolean upload);
    void checkBlocked();
}
