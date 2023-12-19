package com.cb3g.channel19;

interface MI {

    void requestBluetoothPermission();

    void photoChosen(Gif gif, boolean upload);

    void findUser(Coordinates coordinates);

    void startOrStopGPS(boolean start);

    void showSnack(Snack snack);

    void changeBackground(String background);

    void createChannel();

    void launchChannel(Channel channel);

    void enterPin(Channel channel);

    void display_message_history();

    void updateUserList();

    void streamFile(String url);
    void createPm(UserListEntry user);

    void displayChat(UserListEntry user, boolean sound, boolean launch);

    void displayPm(String[] data);

    void displayPhoto(Photo photo);

    void displayLongFlag(String senderId, String senderHandle);

    void stopRecorder(boolean pass);

    void startTransmit();

    void postKeyUp();

    void showRewardAd();

    void finishTutorial(int count);

    void recordChange(boolean recording);

    void updateQueue(int count, boolean paused, boolean poor);

    void updateDisplay(ProfileDisplay display, int count, int duration, boolean paused, boolean poor, long stamp);

    void lockOthers(boolean lock);

    String[] queCheck(int location);

    void recordFromMain();

    void blockThisUser(String id, String handle, boolean toast);

    void blockAll(String id, String handle);

    void blockPhoto(String id, String handle, boolean toast);

    void blockText(String id, String handle, boolean toast);

    void flagThisUser(UserListEntry user);

    void kickUser(UserListEntry user);

    void pauseOrPlay(UserListEntry user);

    void adjustColors(boolean poor);

    void flagOut(String id);

    void banUser(String id);

    void saluteThisUser(UserListEntry user);

    void updateLocationDisplay(String location);

    ProfileDisplay getDisplayedText();

    long getStamp();

    int getQueue();

    long queStamp(int progress);

    void resumeAnimation(int[] data);

    void silence(UserListEntry user);

    void unsilence(UserListEntry user);

    void selectChannel(boolean cancelable);

    int returnUserVolume(String id);

    UserListEntry returnTalkerEntry();

    void longFlagUser(UserListEntry user);
}