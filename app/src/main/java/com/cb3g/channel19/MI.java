package com.cb3g.channel19;

import java.util.List;

interface MI {

    void requestBluetoothPermission();

    void findUser(Coordinates coordinates);

    void startOrStopGPS(boolean start);

    void showSnack(Snack snack);

    void changeBackground(String background);

    void createChannel();

    void launchChannel(Channel channel);

    void enterPin(Channel channel);

    void display_message_history();

    void updateUserList(List<UserListEntry> userList);

    void streamFile(String url);
    void createPm(User user);

    void displayChat(User user, boolean sound, boolean launch);

    void displayPm(String[] data);

    void displayPhoto(Photo photo);

    void displayLongFlag(String senderId, String senderHandle);

    void stopRecorder(boolean pass);

    void startTransmit();

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

    void flagThisUser(User user);

    void kickUser(User user);

    void pauseOrPlay(User user);

    void adjustColors(boolean poor);

    void flagOut(String id);

    void banUser(String id);

    void saluteThisUser(User user);

    void updateLocationDisplay(String location);

    ProfileDisplay getDisplayedText();

    long getStamp();

    int getQueue();

    long queStamp(int progress);

    void resumeAnimation(int[] data);

    void silence(User user);

    void unsilence(User user);

    void selectChannel(boolean cancelable);

    int returnUserVolume(String id);

    User returnTalkerEntry();

    void longFlagUser(User user);
}