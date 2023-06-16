package com.cb3g.channel19;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.android.multidex.myapplication.R;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class ImageSearch extends DialogFragment {
    private List<Giph> giphs = new ArrayList<>();
    private Context context;
    private int columnWidth = 0;
    private RecyclerView recyclerView;
    private RecycleAdapter recycleAdapter = new RecycleAdapter();
    private String id;
    private boolean hd = false;

    private void giphySearch(final String search) {
        Request request;
        if (search.isEmpty()) {
            request = new Request.Builder()
                    .url(getString(R.string.GIPHY_TRENDING) + getString(R.string.GIPHY_API_KEY) + "&limit=" + RadioService.operator.getSearchLimit() + "&rating=R")
                    .build();
        } else {
            request = new Request.Builder()
                    .url(getString(R.string.GIPHY_SEARCH) + search + getString(R.string.GIPHY_API_KEY) + "&limit=" + RadioService.operator.getSearchLimit() + "&rating=R")
                    .build();
        }
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Logger.INSTANCE.e("onFailure " + e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String info = response.body().string();
                        context.getSharedPreferences("giphy", Context.MODE_PRIVATE).edit().putString("popular", info).apply();
                        final JSONObject object = new JSONObject(info);
                        recyclerView.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONArray data = new JSONArray(object.getString("data"));
                                    giphs.clear();
                                    recycleAdapter.notifyDataSetChanged();
                                    if (data.length() > 0) {
                                        for (int i = 0; i < data.length(); i++) {
                                            JSONObject topObject = new JSONObject(data.getString(i));
                                            JSONObject giphObject = new JSONObject(topObject.getString("images"));
                                            Giph giph = RadioService.gson.fromJson(giphObject.toString(), Giph.class);
                                            giph.setId(topObject.getString("id"));
                                            giphs.add(giph);
                                            recycleAdapter.notifyItemInserted(i);
                                        }
                                    }
                                    recyclerView.smoothScrollToPosition(0);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (JSONException e) {
                        Logger.INSTANCE.e("JSONException", e.getMessage());
                    } finally {
                        response.close();
                    }
                } else Logger.INSTANCE.e("giphySearch onResponse Fail " + response.message());
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.image_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) id = getArguments().getString("data");
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setAdapter(recycleAdapter);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        columnWidth = (int) ((displaymetrics.widthPixels) / 2) - 10;
        SearchView gifSearch = view.findViewById(R.id.gif_search);
        ImageView closeButton = (ImageView) gifSearch.findViewById(R.id.search_close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.sendBroadcast(new Intent("nineteenVibrate"));
                Utils.hideKeyboard(context, gifSearch);
                giphySearch("");
                gifSearch.setQuery("", false);
            }
        });
        gifSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                context.sendBroadcast(new Intent("nineteenVibrate"));
                giphySearch(query.trim());
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                /*
                if (newText.isEmpty()) {
                    context.sendBroadcast(new Intent("nineteenVibrate"));
                    Utils.hideKeyboard(context, gifSearch);
                    giphySearch("popular");
                }
                 */
                return false;
            }
        });
        String info = context.getSharedPreferences("giphy", Context.MODE_PRIVATE).getString("popular", null);
        if (info != null) {
            try {
                context.getSharedPreferences("giphy", Context.MODE_PRIVATE).edit().putString("popular", info).apply();
                final JSONObject object = new JSONObject(info);
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONArray data = new JSONArray(object.getString("data"));
                            giphs.clear();
                            recycleAdapter.notifyDataSetChanged();
                            int index = 0;
                            if (data.length() > 0) {
                                for (int i = 0; i < data.length(); i++) {
                                    JSONObject topObject = new JSONObject(data.getString(i));
                                    JSONObject giphObject = new JSONObject(topObject.getString("images"));
                                    Giph giph = RadioService.gson.fromJson(giphObject.toString(), Giph.class);
                                    giph.setId(topObject.getString("id"));
                                    giphs.add(giph);
                                    recycleAdapter.notifyItemInserted(i);
                                    if (id != null) {
                                        if (giph.getId().equals(id)) index = i;
                                    }
                                }
                            }
                            if (index != 0) {
                                RecyclerView.SmoothScroller smoothScroller = new
                                        LinearSmoothScroller(context) {
                                            @Override
                                            protected int getVerticalSnapPreference() {
                                                return LinearSmoothScroller.SNAP_TO_START;
                                            }
                                        };
                                smoothScroller.setTargetPosition(index);
                                recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else
            giphySearch("");
    }

    private class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.GifHolder> implements View.OnClickListener, View.OnLongClickListener {

        @NonNull
        @Override
        public GifHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new GifHolder(getLayoutInflater().inflate(R.layout.gif_column, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(GifHolder holder, int position) {
            int positiion = holder.getAdapterPosition();
            final Giph giph = giphs.get(positiion);
            Gif activeGif;
            if (hd) activeGif = giph.getDownsized();
            else activeGif = giph.getFixed_width_downsampled();
            scaleImageView(holder.imageView, activeGif.getHeight(), activeGif.getWidth(), columnWidth);
            new GlideImageLoader(context, holder.imageView, holder.loading).load(activeGif.getUrl());
            holder.itemView.setTag(giph);
            holder.itemView.setOnClickListener(this);
            holder.itemView.setOnLongClickListener(this);
        }

        private void scaleImageView(ImageView imageView, int imageHeight, int imageWidth, int desiredWidth) {
            imageView.getLayoutParams().width = desiredWidth;
            imageView.getLayoutParams().height = (imageHeight * desiredWidth) / imageWidth;
        }

        @Override
        public int getItemCount() {
            return giphs.size();
        }

        @Override
        public void onClick(View view) {
            context.sendBroadcast(new Intent("nineteenVibrate"));
            Giph giph = (Giph) view.getTag();
            Gif gif = giph.getDownsized();
            gif.setId(giph.getId());
            context.sendBroadcast(new Intent("nineteenGifChosen").putExtra("data", RadioService.gson.toJson(gif)));
            dismiss();
        }

        @Override
        public boolean onLongClick(View view) {
            context.sendBroadcast(new Intent("nineteenVibrate"));
            Giph giph = (Giph) view.getTag();
            Gif gif = giph.getDownsized();
            GifShowCase showCase = new GifShowCase();
            Bundle bundle = new Bundle();
            bundle.putString("data", gif.getUrl());
            showCase.setArguments(bundle);
            showCase.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.full_screen);
            showCase.show(getFragmentManager(), "showcase");
            return true;
        }

        class GifHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ProgressBar loading;
            TextView error;

            GifHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.show);
                loading = itemView.findViewById(R.id.loading);
                error = itemView.findViewById(R.id.error);
            }
        }
    }
}
