package com.cb3g.channel19;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kedia.ogparser.OpenGraphCacheProvider;
import com.kedia.ogparser.OpenGraphCallback;
import com.kedia.ogparser.OpenGraphParser;
import com.kedia.ogparser.OpenGraphResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.shaohui.advancedluban.Luban;
import me.shaohui.advancedluban.OnCompressListener;

@SuppressWarnings("ALL")
public class ReservoirActivity extends AppCompatActivity implements ChildEventListener, RI {
    static int screen_width;
    private final recycler_adapter containerAdapter = new recycler_adapter();
    private TextView emptyView;
    private final List<Post> posts = new ArrayList<>();
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("nineteenGifChosen".equals(intent.getAction())) {
                Gif gif = RadioService.gson.fromJson(intent.getStringExtra("data"), Gif.class);
                if (gif != null) {
                    CreatePost createPost = (CreatePost) fragmentManager.findFragmentByTag("createPost");
                    if (createPost != null) createPost.setPhoto(gif, false);
                    Comments comments = (Comments) fragmentManager.findFragmentByTag("comments");
                    if (comments != null) comments.giphy_remark(gif);
                }
            }
        }
    };
    private FragmentManager fragmentManager;
    private String CACHE_DIRECTORY;
    static DatabaseReference channelReservoirReference;
    private StorageReference storageReference;
    private final List<String> editing = new ArrayList<>();
    private RecyclerView recyclerView;
    private GlideImageLoader glideImageLoader = new GlideImageLoader(this);

    private void snapToPosition(int position) {
        RecyclerView.SmoothScroller smoothScroller = new
                LinearSmoothScroller(this) {
                    @Override
                    protected int getVerticalSnapPreference() {
                        return LinearSmoothScroller.SNAP_TO_START;
                    }
                };
        smoothScroller.setTargetPosition(position);
        recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
    }

    private void removeAllPosts(String userId) {
        for (Post post : posts) {
            if (post.getFacebookId().equals(userId)) {
                channelReservoirReference.child("posts").child(post.getPostId()).removeValue();
            }
        }
    }

    @Override
    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        Post post = dataSnapshot.getValue(Post.class);
        if (!RadioService.blockListContainsId(RadioService.blockedIDs, post.getFacebookId()) || RadioService.operator.getAdmin())
            posts.add(0, post);
        containerAdapter.notifyItemInserted(0);
        if (posts.isEmpty()) emptyView.setVisibility(View.VISIBLE);
        else {
            emptyView.setVisibility(View.GONE);
            snapToPosition(findPost(post.getPostId()));
        }
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        Post post = dataSnapshot.getValue(Post.class);
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getPostId().equals(post.getPostId())) {
                posts.set(i, post);
                containerAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
        Post post = dataSnapshot.getValue(Post.class);
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getPostId().equals(post.getPostId())) {
                posts.remove(i);
                containerAdapter.notifyItemRemoved(i);
            }
        }
        if (posts.isEmpty()) emptyView.setVisibility(View.VISIBLE);
        else emptyView.setVisibility(View.GONE);
    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
    }

    public void create_new_entry() {
        CreatePost createPost = (CreatePost) fragmentManager.findFragmentByTag("createPost");
        if (createPost == null) {
            createPost = new CreatePost(fragmentManager);
            createPost.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.DialogTheme);
            createPost.show(fragmentManager, "createPost");
        }
    }

    @Override
    public void createNewPoll(String caption, String postId) {
        if (postId == null) { //New Poll
            Post post = new Post(2);
            post.setPostId(channelReservoirReference.child("posts/").push().getKey());
            post.setCaption(caption);
            post.setFacebookId(RadioService.operator.getUser_id());
            post.setHandle(RadioService.operator.getHandle());
            post.setProfileLink(RadioService.operator.getProfileLink());
            post.setLocked(true);
            channelReservoirReference.child("posts/").child(post.getPostId()).setValue(post);
            showSnack(new Snack("Poll Created!", Snackbar.LENGTH_SHORT));
        } else { //Poll Option
            Post single = posts.get(findPost(postId));
            ArrayList<String> votes = new ArrayList<>();
            if (!RadioService.operator.getUser_id().equals(single.getFacebookId()))
                votes.add(RadioService.operator.getUser_id());
            single.getOptions().add(new PollOption(caption, votes));
            channelReservoirReference.child("posts/").child(single.getPostId()).setValue(single);
        }
    }

    @Override
    public DatabaseReference databaseReference() {
        return channelReservoirReference;
    }

    @Override
    public StorageReference storageReference() {
        return storageReference;
    }

    private int findPost(String postId) {
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getPostId().equals(postId)) return i;
        }
        return 0;
    }

    @Override
    public void showSnack(Snack snack) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator), snack.getMessage(), snack.getLength());
        View view = snackbar.getView();
        TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(ContextCompat.getColor(this, R.color.main_white));
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.main_black));
        if (snack.getLength() == Snackbar.LENGTH_INDEFINITE) {
            snackbar.setActionTextColor(Color.WHITE);
            snackbar.setAction("10 4", v -> {
                Utils.vibrate(v);
                snackbar.dismiss();
            });
        } else {
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        snackbar.show();
    }

    @SuppressLint({"HardwareIds", "UseCompatLoadingForDrawables"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CACHE_DIRECTORY = getCacheDir() + "/";
        setContentView(R.layout.reservoir_activity);
        fragmentManager = getSupportFragmentManager();
        screen_width = Utils.getScreenWidth(this);
        recyclerView = findViewById(R.id.container);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(containerAdapter);
        channelReservoirReference = RadioService.databaseReference.child("reservoir").child(String.valueOf(RadioService.operator.getChannel().getChannel()));
        storageReference = FirebaseStorage.getInstance("gs://weeklystorage").getReference();
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        emptyView = findViewById(R.id.hour_glass);
        ImageView backDrop = findViewById(R.id.backdrop);
        String background = settings.getString("settings_backdrop", "");
        if (settings.getBoolean("custom", false))
            background = settings.getString("background", "default");
        glideImageLoader.load(backDrop, background);
        TextView post = findViewById(R.id.post);
        TextView poll = findViewById(R.id.poll);
        if (RadioService.operator.getBlockedFromReservoir()) {
            post.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.post_g), null, null, null);
            poll.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.poll_g), null, null, null);
            post.setTextColor(ContextCompat.getColor(this, R.color.greyed_out));
            poll.setTextColor(ContextCompat.getColor(this, R.color.greyed_out));
        } else {
            post.setOnClickListener(v -> {
                Utils.vibrate(v);
                create_new_entry();
            });
            poll.setOnClickListener(v -> {
                Utils.vibrate(v);
                CreatePoll createPoll = new CreatePoll(null);
                createPoll.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.DialogTheme);
                createPoll.show(fragmentManager, "createPoll");
            });
        }
        channelReservoirReference.child("posts").addChildEventListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(this, receiver, returnFilter(), ContextCompat.RECEIVER_NOT_EXPORTED);
        if (!RadioService.operator.getAdmin())
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        channelReservoirReference.child("posts").removeEventListener(this);
    }

    private String replaceUrls(String caption) {
        for (String url : extractUrls(caption)) {
            if (caption.contains(url)) {
                caption = caption.replace(url, "").trim();
            }
        }
        return caption;
    }

    private List<String> extractUrls(String text) {
        List<String> containedUrls = new ArrayList<>();
        String urlRegex = getString(R.string.extraction);
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(text);
        while (urlMatcher.find()) {
            containedUrls.add(text.substring(urlMatcher.start(0),
                    urlMatcher.end(0)));
        }
        return containedUrls;
    }

    @Override
    public void simple_post(final Gif gif, String content, boolean upload) {
        if (gif == null && content.trim().length() == 0) return;
        Post post = new Post();
        post.setType(1);
        post.setPostId(channelReservoirReference.child("posts").push().getKey());
        post.setFacebookId(RadioService.operator.getUser_id());
        post.setHandle(RadioService.operator.getHandle());
        post.setProfileLink(RadioService.operator.getProfileLink());
        post.setCaption(content.replaceAll("\\s+", " ").trim());
        List<String> webLinks = extractUrls(content);
        if (!webLinks.isEmpty()) { //TODO:
            post.setWebLink(webLinks.get(0));
            post.setCaption(replaceUrls(content).trim());
            OpenGraphParser og = new OpenGraphParser(new OpenGraphCallback() {
                @Override
                public void onPostResponse(@NonNull OpenGraphResult openGraphResult) {
                    String imageLink = openGraphResult.getImage();
                    if (imageLink != null){
                        post.setImageLink(imageLink);
                        post.setWebDescription(openGraphResult.getTitle());
                        if (post.getCaption().isEmpty())
                            post.setCaption(openGraphResult.getDescription());
                        ExecutorUtils.newSingleThreadExecutor().execute(() -> {
                            Bitmap image = Utils.fetchImage(imageLink);
                            if (image != null) {
                                post.setImage_height(image.getHeight());
                                post.setImage_width(image.getWidth());
                                Log.i("image", "Image height: " + image.getHeight());
                                Log.i("image", "Image width: " + image.getWidth());
                                image.recycle();
                            } else Log.i("image", "Image was null");
                            finish_simple_post(post, gif, false);
                        });
                    }
                }

                @Override
                public void onError(@NonNull String s) {
                    Log.e("image", "OGP " + s);
                }
            }, true, new OpenGraphCacheProvider(ReservoirActivity.this));
            og.parse(post.getWebLink());
        } else finish_simple_post(post, gif, upload);
    }

    private void finish_simple_post(final Post post, Gif gif, boolean upload) {
        if (gif != null) {
            if (!upload) {
                post.setImageLink(gif.getUrl());
                post.setImage_height(gif.getHeight());
                post.setImage_width(gif.getWidth());
                channelReservoirReference.child("posts").child(post.getPostId()).setValue(post);
            } else {
                String fileName = post.getPostId() + returnFileTypeFromUri(gif.getUrl());
                Luban.compress(ReservoirActivity.this, returnFileFromUri(gif.getUrl(), post.getPostId()))
                        .putGear(Luban.THIRD_GEAR)
                        .launch(new OnCompressListener() {
                            @Override
                            public void onStart() {
                            }

                            @Override
                            public void onSuccess(final File file) {
                                StorageReference ref = storageReference.child(fileName);
                                UploadTask uploadTask = ref.putFile(Uri.fromFile(file));
                                uploadTask.continueWithTask(task -> {
                                    if (!task.isSuccessful()) {
                                        throw task.getException();
                                    }
                                    return ref.getDownloadUrl();
                                }).addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Uri downloadUri = task.getResult();
                                        post.setImageLink(downloadUri.toString());
                                        post.setImage_height(gif.getHeight());
                                        post.setImage_width(gif.getWidth());
                                        channelReservoirReference.child("posts").child(post.getPostId()).setValue(post);
                                    }
                                });
                            }

                            @Override
                            public void onError(Throwable e) {
                                Logger.INSTANCE.e("LUBAN ERROR " + e);
                            }
                        });
                return;
            }
        }
        channelReservoirReference.child("posts").child(post.getPostId()).setValue(post);
    }

    private void delete_post(final Post post) {
        channelReservoirReference.child("posts").child(post.getPostId()).removeValue();
        try {
            if (!post.getImageLink().equals("none"))
                FirebaseStorage.getInstance().getReferenceFromUrl(post.getImageLink()).delete();
        } catch (IllegalArgumentException e) {
            Logger.INSTANCE.e("delete_post() IllegalArgumentException " + e);
        }
        channelReservoirReference.child("remarks").child(post.getPostId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Comment comment = child.getValue(Comment.class);
                    delete_remark(comment);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public void delete_remark(Comment comment) {
        channelReservoirReference.child("remarks").child(comment.getPostId()).child(comment.getRemarkId()).removeValue();
        try {
            if (comment.getType() == 1)
                FirebaseStorage.getInstance().getReferenceFromUrl(comment.getContent()).delete();
        } catch (IllegalArgumentException e) {
            Logger.INSTANCE.e("delete_post() IllegalArgumentException " + e);
        }
    }

    @Override
    public File returnFileFromUri(final String uri, String fileName) {
        try {
            InputStream in = getContentResolver().openInputStream(Uri.parse(uri));
            File sourceFile = new File(CACHE_DIRECTORY + fileName);
            if (in != null) {
                FileUtils.copyInputStreamToFile(in, sourceFile);
                in.close();
                return sourceFile;
            }
        } catch (IOException e) {
            Logger.INSTANCE.e("return file from uri " + e);
        }
        return null;
    }

    @Override
    public String returnFileTypeFromUri(final String uri) {
        ContentResolver cR = this.getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return "." + mime.getExtensionFromMimeType(cR.getType(Uri.parse(uri)));
    }

    @Override
    public String return_timestamp_string() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd", Locale.getDefault());
        String date = sdf.format(new Date());
        String time = String.format("%tR", System.currentTimeMillis());
        return date + ", " + time;
    }

    private IntentFilter returnFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("nineteenGifChosen");
        return filter;
    }

    @Override
    public void open_remarks(final Post post) {
        if (RadioService.operator.getSilenced()) {
            showSnack(new Snack("You are currently silenced", Snackbar.LENGTH_SHORT));
            return;
        }
        Comments comments = (Comments) fragmentManager.findFragmentByTag("comments");
        if (comments == null) {
            comments = new Comments(fragmentManager);
            comments.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.DialogTheme);
            Bundle bundle = new Bundle();
            bundle.putString("post", RadioService.gson.toJson(post));
            comments.setArguments(bundle);
            comments.show(fragmentManager, "comments");
        }
    }

    @Override
    public void action_view(final String imageLink) {
        FullScreen fullScreen = (FullScreen) fragmentManager.findFragmentByTag("fsf");
        if (fullScreen == null) {
            fullScreen = new FullScreen();
            Bundle bundle = new Bundle();
            bundle.putString("data", imageLink);
            fullScreen.setArguments(bundle);
            fullScreen.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            fullScreen.show(fragmentManager, "fsf");
        }
    }

    class recycler_adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemViewType(int position) {
            return posts.get(position).getType();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case 1:
                    return new PostHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_post, parent, false));
                case 2:
                    return new PollHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.poll_post, parent, false));
                default:
                    return null;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Post post = posts.get(holder.getAdapterPosition());
            boolean ownerOfPost = post.getFacebookId().equals(RadioService.operator.getUser_id());
            switch (holder.getItemViewType()) {
                case 1: //TODO links
                    PostHolder simple_post = (PostHolder) holder;
                    glideImageLoader.load(simple_post.poster_profile_pic, post.getProfileLink(), RadioService.profileOptions);
                    simple_post.poster_profile_pic.setOnClickListener(v -> {
                        Utils.vibrate(v);
                        action_view(post.getProfileLink());
                    });
                    simple_post.poster_profile_name.setText(post.getHandle());
                    simple_post.posting_stamp.setText(Utils.showElapsed(post.getStamp(), true));
                    String caption = post.getCaption();
                    if (caption.equals("none") || caption.length() == 0) {
                        simple_post.caption.setVisibility(View.GONE);
                    } else {
                        simple_post.caption.setVisibility(View.VISIBLE);
                        simple_post.caption.setText(caption);
                        simple_post.caption.setText(caption);
                    }
                    if (!post.getWebLink().equals("none")) {
                        simple_post.webLink.setVisibility(View.VISIBLE);
                        simple_post.webDescription.setText(post.getWebDescription());
                        simple_post.webLink.setOnClickListener(v -> {
                            Utils.vibrate(v);
                            sendBroadcast(new Intent("nineteenPause"));
                            Utils.launchUrl(ReservoirActivity.this, post.getWebLink());
                        });
                    } else {
                        simple_post.webLink.setVisibility(View.GONE);
                    }
                    if (!post.getImageLink().equals("none")) {
                        if (post.getImage_width() != 0 && post.getImage_height() != 0) {
                            simple_post.image_layout.setVisibility(View.VISIBLE);
                            int new_height = (((post.getImage_height() * ReservoirActivity.screen_width) / post.getImage_width()));
                            int new_width = (ReservoirActivity.screen_width);
                            simple_post.image.getLayoutParams().height = new_height;
                            simple_post.image.getLayoutParams().width = new_width;
                            simple_post.loading.setVisibility(View.VISIBLE);
                            glideImageLoader.load(simple_post.image, simple_post.loading, post.getImageLink());
                            if (post.getWebLink().equals("none")) {
                                simple_post.image.setOnClickListener(v -> {
                                    Utils.vibrate(v);
                                    action_view(post.getImageLink());
                                });
                            } else {
                                simple_post.image.setOnClickListener(v -> {
                                    Utils.vibrate(v);
                                    sendBroadcast(new Intent("nineteenPause"));
                                    Utils.launchUrl(ReservoirActivity.this, post.getWebLink());
                                });
                            }
                        }
                    } else {
                        simple_post.whiteLine.setVisibility(View.GONE);
                        simple_post.image_layout.setVisibility(View.GONE);
                    }
                    String data = "No " + getString(R.string.comments);
                    if (post.getRemarks() > 0) {
                        if (post.getRemarks() == 1)
                            data = NumberFormat.getNumberInstance(Locale.US).format(post.getRemarks()) + " " + getString(R.string.comment);
                        else
                            data = NumberFormat.getNumberInstance(Locale.US).format(post.getRemarks()) + " " + getString(R.string.comments);
                    }
                    if (post.getRemarks() > 0)
                        simple_post.likebar.setBackground(ContextCompat.getDrawable(ReservoirActivity.this, R.drawable.black_box_white_outline_square));
                    else
                        simple_post.likebar.setBackground(ContextCompat.getDrawable(ReservoirActivity.this, R.drawable.black_box_white_outline_square_top));
                    simple_post.remarks.setText(data);
                    simple_post.likes.setText(NumberFormat.getNumberInstance(Locale.US).format(post.getLikes()));
                    simple_post.dislikes.setText(NumberFormat.getNumberInstance(Locale.US).format(post.getDislikes()));
                    simple_post.remarks.setOnClickListener(v -> {
                        Utils.vibrate(v);
                        open_remarks(post);
                    });
                    if (!ownerOfPost) {
                        simple_post.like.setOnClickListener(v -> {
                            //like post
                            Utils.vibrate(v);
                            channelReservoirReference.child("posts").child(post.getPostId()).child("likes").setValue(post.getLikes() + 1);
                        });
                        simple_post.dislike.setOnClickListener(v -> {
                            //dislike post
                            Utils.vibrate(v);
                            channelReservoirReference.child("posts").child(post.getPostId()).child("dislikes").setValue(post.getDislikes() + 1);
                        });
                    }
                    if (!post.getLatest_profileLink().equals("none")) {
                        simple_post.remarker_profile_pic.setVisibility(View.VISIBLE);
                        simple_post.remarker_name.setVisibility(View.VISIBLE);
                        simple_post.remarker_text.setVisibility(View.VISIBLE);
                        glideImageLoader.load(simple_post.remarker_profile_pic, post.getLatest_profileLink(), RadioService.profileOptions);
                        simple_post.remarker_name.setText(post.getLatest_handle());
                        simple_post.remarker_text.setText(StringUtils.abbreviate(post.getLatest_remark(), 60));
                        simple_post.remarker_profile_pic.setOnClickListener(v -> {
                            Utils.vibrate(v);
                            open_remarks(post);
                        });
                        simple_post.remarker_name.setOnClickListener(v -> {
                            Utils.vibrate(v);
                            open_remarks(post);
                        });
                        simple_post.remarker_text.setOnClickListener(v -> {
                            Utils.vibrate(v);
                            open_remarks(post);
                        });
                    } else {
                        simple_post.remarker_profile_pic.setVisibility(View.GONE);
                        simple_post.remarker_name.setVisibility(View.GONE);
                        simple_post.remarker_text.setVisibility(View.GONE);
                    }
                    if (post.getLocked())
                        simple_post.menu.setImageDrawable(ContextCompat.getDrawable(ReservoirActivity.this, R.drawable.lock));
                    else
                        simple_post.menu.setImageDrawable(ContextCompat.getDrawable(ReservoirActivity.this, R.drawable.menu));
                    simple_post.menu.setOnClickListener(v -> {
                        Utils.vibrate(v);
                        PopupMenu popupMenu = new PopupMenu(ReservoirActivity.this, v, Gravity.END, 0, R.style.PopupMenu);
                        if (!post.getLocked() && !post.getImageLink().equals("none"))
                            popupMenu.getMenu().add(1, R.id.save_post, 1, "Save Image");
                        if (ownerOfPost) {
                            if (caption.equals("none"))
                                popupMenu.getMenu().add(1, R.id.add_text, 2, "Add Text");
                            else popupMenu.getMenu().add(1, R.id.edit_text, 2, "Edit Text");
                        }
                        if (ownerOfPost || RadioService.operator.getAdmin()) {
                            if (!post.getLocked()) {
                                popupMenu.getMenu().add(1, R.id.lock_post, 3, "Lock");
                            } else popupMenu.getMenu().add(1, R.id.unlock_post, 3, "Unlock");
                            popupMenu.getMenu().add(1, R.id.delete_post, 4, "Delete");
                            if (RadioService.operator.getAdmin()) {
                                popupMenu.getMenu().add(1, R.id.delete_all_post, 5, "Delete All");
                                popupMenu.getMenu().add(1, R.id.bann_user, 6, "Block Posting");
                            }
                        }
                        popupMenu.setOnMenuItemClickListener(item -> {
                            Utils.vibrate(v);
                            int id = item.getItemId();
                            if (id == R.id.delete_all_post) {
                                removeAllPosts(post.getFacebookId());
                            } else if (id == R.id.bann_user) {
                                RadioService.databaseReference.child("blockedFromReservoir").child(post.getFacebookId()).setValue(post.getHandle());
                            } else if (id == R.id.add_text || id == R.id.edit_text) {
                                EditPost epd = (EditPost) fragmentManager.findFragmentByTag("epd");
                                if (epd == null) {
                                    epd = new EditPost();
                                    Bundle bundle = new Bundle();
                                    bundle.putString("title", "Edit Post");
                                    bundle.putString("postId", post.getPostId());
                                    bundle.putString("remarkId", "default");
                                    bundle.putString("content", post.getCaption());
                                    epd.setArguments(bundle);
                                    epd.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.DialogTheme);
                                    epd.show(fragmentManager, "epd");
                                }
                            } else if (id == R.id.delete_post) {
                                delete_post(post);
                            } else if (id == R.id.lock_post) {
                                channelReservoirReference.child("posts").child(post.getPostId()).child("locked").setValue(true);
                            } else if (id == R.id.unlock_post) {
                                channelReservoirReference.child("posts").child(post.getPostId()).child("locked").setValue(false);
                            } else if (id == R.id.save_post) {
                                sendBroadcast(new Intent("savePhotoToDisk").putExtra("url", post.getImageLink()));
                            }
                            return true;
                        });
                        popupMenu.show();
                    });
                    break;
                case 2:
                    PollHolder pollHolder = (PollHolder) holder;
                    glideImageLoader.load(pollHolder.poster_profile_pic, post.getProfileLink(), RadioService.profileOptions);
                    pollHolder.poster_profile_pic.setOnClickListener(v -> {
                        Utils.vibrate(v);
                        action_view(post.getProfileLink());
                    });
                    pollHolder.poster_profile_name.setText(post.getHandle());
                    pollHolder.posting_stamp.setText(Utils.showElapsed(post.getStamp(), true));
                    pollHolder.caption.setText(post.getCaption());
                    if (ownerOfPost) {
                        pollHolder.menu.setVisibility(View.VISIBLE);
                        pollHolder.menu.setOnClickListener(v -> {
                            Utils.vibrate(v);
                            PopupMenu popupMenu = new PopupMenu(ReservoirActivity.this, v, Gravity.END, 0, R.style.PopupMenu);
                            if (ownerOfPost || RadioService.operator.getAdmin()) {
                                popupMenu.getMenu().add(1, R.id.delete_post, 4, "Delete");
                                if (!post.getLocked())
                                    popupMenu.getMenu().add(1, R.id.lock_post, 3, "Lock Options");
                                else
                                    popupMenu.getMenu().add(1, R.id.unlock_post, 3, "Unlock Options");
                            }
                            popupMenu.setOnMenuItemClickListener(item -> {
                                Utils.vibrate(v);
                                int id = item.getItemId();
                                if (id == R.id.delete_post) {
                                    delete_post(post);
                                } else if (id == R.id.lock_post) {
                                    channelReservoirReference.child("posts").child(post.getPostId()).child("locked").setValue(true);
                                } else if (id == R.id.unlock_post) {
                                    channelReservoirReference.child("posts").child(post.getPostId()).child("locked").setValue(false);
                                }
                                return true;
                            });
                            popupMenu.show();
                        });
                    } else {
                        pollHolder.menu.setVisibility(View.INVISIBLE);
                    }
                    int max = returnMax(post.getOptions());
                    boolean alreadyVoted = alreadyVoted(post.getOptions());
                    RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                        @NonNull
                        @Override
                        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                            return new PollOptionHolder(LayoutInflater.from(ReservoirActivity.this).inflate(R.layout.poll_option, viewGroup, false));
                        }

                        @Override
                        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                            PollOptionHolder pollOptionHolder = (PollOptionHolder) viewHolder;
                            PollOption option = post.getOptions().get(viewHolder.getAdapterPosition());
                            pollOptionHolder.label.setText(option.getLabel());
                            pollOptionHolder.count.setText(String.valueOf(option.getVotes().size()));
                            pollOptionHolder.progressBar.setMax(max);
                            pollOptionHolder.progressBar.setProgress(option.getVotes().size());
                            pollOptionHolder.like.setOnClickListener(v -> {
                                for (String vote : option.getVotes()) {
                                    if (vote.equals(RadioService.operator.getUser_id())) return;
                                }
                                Utils.vibrate(v);
                                if (alreadyVoted) {
                                    for (int i1 = 0; i1 < post.getOptions().size(); i1++) {
                                        for (int x = post.getOptions().get(i1).getVotes().size() - 1; x >= 0; x--) {
                                            if (post.getOptions().get(i1).getVotes().get(x).equals(RadioService.operator.getUser_id())) {
                                                post.getOptions().get(i1).getVotes().remove(x);
                                            }
                                        }
                                    }
                                }
                                post.getOptions().get(viewHolder.getAdapterPosition()).getVotes().add(RadioService.operator.getUser_id());
                                channelReservoirReference.child("posts/").child(post.getPostId()).setValue(post);
                                showSnack(new Snack("Voted " + option.getLabel(), Snackbar.LENGTH_SHORT));
                            });
                        }

                        @Override
                        public int getItemCount() {
                            return post.getOptions().size();
                        }
                    };
                    pollHolder.recyclerView.setLayoutManager(new LinearLayoutManager(ReservoirActivity.this, LinearLayoutManager.VERTICAL, false));
                    pollHolder.recyclerView.setHasFixedSize(false);
                    pollHolder.recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    if ((alreadyVoted && !ownerOfPost) || (post.getLocked() && !ownerOfPost)) {
                        pollHolder.add_option.setVisibility(View.GONE);
                        pollHolder.optionView.setVisibility(View.GONE);
                    } else {
                        if (editing.contains(post.getPostId())) {//editing post
                            pollHolder.optionView.setVisibility(View.VISIBLE);
                            pollHolder.add_option.setTextColor(Color.RED);
                            pollHolder.add_option.setText(getString(R.string.cancel));
                            Utils.showKeyboard(ReservoirActivity.this, pollHolder.optionET);
                        } else {//not Editing
                            pollHolder.optionView.setVisibility(View.GONE);
                            pollHolder.add_option.setTextColor(Color.WHITE);
                            pollHolder.add_option.setText(getString(R.string.add_poll_option));
                        }
                        pollHolder.add_option.setOnClickListener(v -> {
                            Utils.vibrate(v);
                            if (editing.contains(post.getPostId())) {
                                editing.remove(post.getPostId());
                                Utils.hideKeyboard(ReservoirActivity.this, pollHolder.optionET);
                            } else editing.add(post.getPostId());
                            containerAdapter.notifyDataSetChanged();
                        });
                        pollHolder.add.setOnClickListener(v -> {
                            Utils.vibrate(v);
                            if (pollHolder.optionET.getText().toString().isEmpty())
                                pollHolder.optionET.setError("empty");
                            else {
                                ArrayList<String> votes = new ArrayList<>();
                                if (!RadioService.operator.getUser_id().equals(post.getFacebookId()))
                                    votes.add(RadioService.operator.getUser_id());
                                post.getOptions().add(new PollOption(pollHolder.optionET.getText().toString().trim(), votes));
                                editing.remove(post.getPostId());
                                channelReservoirReference.child("posts").child(post.getPostId()).setValue(post);
                                Utils.hideKeyboard(ReservoirActivity.this, pollHolder.optionET);
                                pollHolder.optionET.setText("");
                            }
                        });
                    }
                    if (post.getLocked())
                        pollHolder.menu.setImageDrawable(ContextCompat.getDrawable(ReservoirActivity.this, R.drawable.lock));
                    else
                        pollHolder.menu.setImageDrawable(ContextCompat.getDrawable(ReservoirActivity.this, R.drawable.menu));
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return posts.size();
        }

        boolean alreadyVoted(List<PollOption> options) {
            for (PollOption option : options) {
                for (String vote : option.getVotes()) {
                    if (vote.equals(RadioService.operator.getUser_id())) return true;
                }
            }
            return false;
        }

        int returnMax(List<PollOption> options) {
            int max = 0;
            for (PollOption option : options) {
                if (max < option.getVotes().size()) max = option.getVotes().size();
            }
            return max;
        }

        class PostHolder extends RecyclerView.ViewHolder {
            TextView caption, remarks, poster_profile_name, posting_stamp, remarker_text, remarker_name, likes, dislikes, webDescription;
            ImageView poster_profile_pic, remarker_profile_pic, image, menu, like, dislike;
            LinearLayout webLink, likebar;
            View whiteLine;
            ProgressBar loading;
            RelativeLayout image_layout;
            //WebView webView;

            PostHolder(View v) {
                super(v);
                webDescription = v.findViewById(R.id.webDescription);
                webLink = v.findViewById(R.id.webLink);
                caption = v.findViewById(R.id.photoComment);
                remarks = v.findViewById(R.id.remarks);
                poster_profile_name = v.findViewById(R.id.profile_name);
                posting_stamp = v.findViewById(R.id.posting_stamp);
                remarker_text = v.findViewById(R.id.remark_text);
                remarker_name = v.findViewById(R.id.remark_name);
                image_layout = v.findViewById(R.id.image_layout);
                image = v.findViewById(R.id.primary);
                menu = v.findViewById(R.id.post_menu);
                poster_profile_pic = v.findViewById(R.id.profile_pic);
                remarker_profile_pic = v.findViewById(R.id.remark_profile_pic);
                likes = v.findViewById(R.id.likes);
                dislikes = v.findViewById(R.id.dislikes);
                like = v.findViewById(R.id.like);
                dislike = v.findViewById(R.id.dislike);
                likebar = v.findViewById(R.id.likebar);
                whiteLine = v.findViewById(R.id.whiteLine);
                loading = v.findViewById(R.id.loading);
            }
        }

        class PollHolder extends RecyclerView.ViewHolder {
            TextView caption, poster_profile_name, posting_stamp, add_option;
            ImageView poster_profile_pic, menu;
            RecyclerView recyclerView;
            ConstraintLayout optionView;
            EditText optionET;
            ImageView add;

            PollHolder(View v) {
                super(v);
                caption = v.findViewById(R.id.photoComment);
                poster_profile_name = v.findViewById(R.id.profile_name);
                posting_stamp = v.findViewById(R.id.posting_stamp);
                poster_profile_pic = v.findViewById(R.id.profile_pic);
                menu = v.findViewById(R.id.post_menu);
                recyclerView = v.findViewById(R.id.recyclerView);
                add_option = v.findViewById(R.id.add_option);
                optionView = v.findViewById(R.id.optionView);
                optionET = v.findViewById(R.id.optionET);
                add = v.findViewById(R.id.add_button);
            }
        }

        class PollOptionHolder extends RecyclerView.ViewHolder {
            TextView label, count;
            ImageView like;
            ProgressBar progressBar;

            PollOptionHolder(View v) {
                super(v);
                label = v.findViewById(R.id.label);
                count = v.findViewById(R.id.count);
                like = v.findViewById(R.id.like);
                progressBar = v.findViewById(R.id.progressBar);
            }
        }
    }
}


