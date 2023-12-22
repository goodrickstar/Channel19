package com.cb3g.channel19;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.net.MalformedURLException;

interface RI {

    void showSnack(Snack snack);

    void open_remarks(Post post);

    void action_view(String imageLink);

    void simple_post(Gif gif, String content, boolean upload) throws MalformedURLException;

    File returnFileFromUri(String uri, String fileName);

    String return_timestamp_string();

    String returnFileTypeFromUri(String uri);

    void createNewPoll(String caption, String postId);

    DatabaseReference databaseReference();

    StorageReference storageReference();
}
