package com.cb3g.channel19;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.android.multidex.myapplication.R;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class Stars extends DialogFragment {
    private Context context;
    private JSONArray stars = new JSONArray();
    private BaseAdapter adapter;
    private ListView selection;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onStart() {
        super.onStart();
        context.sendBroadcast(new Intent("nineteenOccupied").putExtra("data", true));
    }

    @Override
    public void onResume() {
        super.onResume();
        final Request request = new Request.Builder()
                .url(RadioService.SITE_URL + "user_list_stars.php")
                .build();
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) try {
                    stars = new JSONArray(response.body().string());
                    selection.post(() -> adapter.notifyDataSetChanged());
                } catch (JSONException e) {
                    Logger.INSTANCE.e("JSONException " + e);
                }
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        context.sendBroadcast(new Intent("nineteenOccupied").putExtra("data", false));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = requireDialog().getWindow();
        if (window != null) window.getAttributes().windowAnimations = R.style.photoAnimation;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.star_selection, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return stars.length();
            }

            @Override
            public String getItem(int i) {
                try {
                    return stars.getString(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(final int i, View convertView, final ViewGroup parent) {
                if (convertView == null)
                    convertView = LayoutInflater.from(getActivity()).inflate(R.layout.star_selection_row, parent, false);
                try {
                    new GlideImageLoader(context, convertView.findViewById(R.id.star)).load(Utils.parseRankUrl(stars.getString(i)));
                } catch (JSONException e) {
                    Logger.INSTANCE.i("JSONException" + e);
                }
                return convertView;
            }
        };
        selection = v.findViewById(R.id.selection);
        selection.setAdapter(adapter);
        TextView close = v.findViewById(R.id.close);
        close.setOnClickListener(v1 -> {
            context.sendBroadcast(new Intent("nineteenClickSound"));
            Utils.vibrate(v1);
            dismiss();
        });
        adapter.notifyDataSetChanged();
        selection.setOnItemClickListener((adapterView, view, i, l) -> {
            context.sendBroadcast(new Intent("nineteenClickSound"));
            Utils.vibrate(v);
            try {
                context.sendBroadcast(new Intent("setStar").putExtra("data", stars.getString(i)));
            } catch (JSONException e) {
                Logger.INSTANCE.e(String.valueOf(e));
            }
            dismiss();
        });
    }
}

