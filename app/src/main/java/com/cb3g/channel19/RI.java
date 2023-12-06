package com.cb3g.channel19;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.net.MalformedURLException;

interface RI {

    void launchSearch();

    void showSnack(Snack snack);

    void open_remarks(Post post);

    void choose_photo_remark();

    void action_view(String imageLink);

    void choose_photo_entry();

    void simple_post(Gif gif, String content, boolean upload) throws MalformedURLException;

    File returnFileFromUri(String uri, String fileName);

    String return_timestamp_string();

    String returnFileTypeFromUri(String uri);

    void edit_post(String title, String postId, String remarkId, String content);

    void createNewPoll(String caption, String postId);

    DatabaseReference databaseReference();

    StorageReference storageReference();
}
