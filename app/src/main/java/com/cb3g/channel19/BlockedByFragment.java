package com.cb3g.channel19;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.multidex.myapplication.R;
import com.example.android.multidex.myapplication.databinding.BlockedByDialogBinding;

import java.util.ArrayList;

public class BlockedByFragment extends DialogFragment {
    private BlockedByDialogBinding binding;
    private Context context;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BlockedByDialogBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ArrayList<String> data = getArguments().getStringArrayList("handles");
        binding.title.setText("Blocked By " + data.size() + " Users");
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setAdapter(new recycler_adapter(data));
        binding.okay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.sendBroadcast(new Intent("nineteenVibrate"));
                context.sendBroadcast(new Intent("nineteenClickSound"));
                dismiss();
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    class recycler_adapter extends RecyclerView.Adapter<recycler_adapter.MyViewHolder> {
        private ArrayList<String> handles;

        public recycler_adapter(ArrayList<String> handles) {
            this.handles = handles;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new recycler_adapter.MyViewHolder(getLayoutInflater().inflate(R.layout.blocked_by_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull recycler_adapter.MyViewHolder holder, int i) {
            holder.handle.setText(handles.get(i));
            Logger.INSTANCE.i(handles.get(i));
        }

        @Override
        public int getItemCount() {
            return handles.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView handle;

            MyViewHolder(View itemView) {
                super(itemView);
                handle = itemView.findViewById(R.id.handle);
            }
        }
    }
}
