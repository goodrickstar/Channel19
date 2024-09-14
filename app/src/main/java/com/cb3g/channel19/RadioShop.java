package com.cb3g.channel19;

import static com.cb3g.channel19.RadioService.databaseReference;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.RadioShopBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.Instant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class RadioShop extends DialogFragment implements View.OnClickListener, ValueEventListener {
    private Context context;
    private RadioShopBinding binding;
    private DatabaseReference tokenReference;
    private int tokens = 0;

    @Override
    public void onClick(View v) {
        Utils.vibrate(v);
        context.sendBroadcast(new Intent("nineteenPause").setPackage("com.cb3g.channel19"));
        Utils.showRewardAd(context, RadioService.operator.getUser_id(), tokens, true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = RadioShopBinding.inflate(inflater);
        tokenReference = Utils.getDatabase().getReference().child("tokens").child(RadioService.operator.getUser_id());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.launchAdd.setOnClickListener(this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(new ShopAdapter(new ArrayList<>()));
        Utils.getShop().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<ShopItem> shopItems = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    shopItems.add(child.getValue(ShopItem.class));
                }
                binding.recycler.setAdapter(new ShopAdapter(shopItems));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        tokenReference.addValueEventListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        tokenReference.removeEventListener(this);
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {
        if (snapshot.exists()) {
            tokens = snapshot.getValue(int.class);
            binding.tokenCount.setText("x " + tokens);
            RotateAnimation counterClockwise = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            counterClockwise.setDuration(500);
            binding.tireIv.startAnimation(counterClockwise);
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {
        Logger.INSTANCE.i("onCancelled()");
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.MyViewHolder> {
        private final ArrayList<ShopItem> items;

        public ShopAdapter(ArrayList<ShopItem> items) {
            this.items = items;
        }

        private final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.vibrate(v);
                ShopItem item = (ShopItem) v.getTag();
                if (tokens < item.getCost()) {
                    context.sendBroadcast(new Intent("wrong").setPackage("com.cb3g.channel19"));
                    return;
                }
                switch (item.getId()) {
                    case 0:
                        spent(item.getCost());
                        saluteSelf();
                        break;
                    case 1:
                        if (RadioService.silencedUsers.contains(RadioService.operator.getUser_id())) {
                            spent(item.getCost());
                            databaseReference.child("silenced").child(RadioService.operator.getUser_id()).removeValue();
                        } else context.sendBroadcast(new Intent("wrong").setPackage("com.cb3g.channel19"));
                        break;
                    case 2:
                        spent(item.getCost());
                        resetTimer();
                        break;
                    case 3:
                        spent(item.getCost());
                        clearFlags();
                        break;
                    case 4:
                        if (RadioService.ghostUsers.contains(RadioService.operator.getUser_id())) {
                            context.sendBroadcast(new Intent("wrong").setPackage("com.cb3g.channel19"));
                            return;
                        }
                        spent(item.getCost());
                        databaseReference.child("ghost").child(RadioService.operator.getUser_id()).setValue(Instant.now().getEpochSecond()).addOnSuccessListener(aVoid -> context.sendBroadcast(new Intent("ghost")));
                        break;
                }
            }
        };

        private void spent(int cost) {
            context.sendBroadcast(new Intent("register").setPackage("com.cb3g.channel19"));
            Utils.getTokens(RadioService.operator.getUser_id()).setValue(tokens - cost);
        }

        @NonNull
        @Override
        public ShopAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ShopAdapter.MyViewHolder(getLayoutInflater().inflate(R.layout.shop_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ShopAdapter.MyViewHolder holder, int i) {
            ShopItem item = items.get(i);
            holder.label.setText(item.getLabel());
            holder.cost.setText(String.valueOf(item.getCost()));
            holder.buy.setTag(item);
            holder.buy.setOnClickListener(listener);
        }


        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).getId();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView label, cost;
            ImageView buy;

            MyViewHolder(View itemView) {
                super(itemView);
                label = itemView.findViewById(R.id.shop_label);
                cost = itemView.findViewById(R.id.cost);
                buy = itemView.findViewById(R.id.icon);
            }
        }
    }

    void saluteSelf() {
        final String data = Jwts.builder().setHeader(RadioService.header).claim("userId", RadioService.operator.getUser_id()).claim("handle", RadioService.operator.getHandle()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, RadioService.operator.getKey()).compact();
        final Request request = new Request.Builder().url(RadioService.SITE_URL + "user_salute_self.php").post(new FormBody.Builder().add("data", data).build()).build();
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                response.close();
            }
        });
    }

    void clearFlags() {
        final String data = Jwts.builder().setHeader(RadioService.header).claim("userId", RadioService.operator.getUser_id()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, RadioService.operator.getKey()).compact();
        final Request request = new Request.Builder().url(RadioService.SITE_URL + "user_clear_flags.php").post(new FormBody.Builder().add("data", data).build()).build();
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                response.close();
            }
        });
    }

    void resetTimer() {
        final String data = Jwts.builder().setHeader(RadioService.header).claim("userId", RadioService.operator.getUser_id()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, RadioService.operator.getKey()).compact();
        final Request request = new Request.Builder().url(RadioService.SITE_URL + "user_reset" + "_timer.php").post(new FormBody.Builder().add("data", data).build()).build();
        RadioService.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                response.close();
            }
        });
    }

}
