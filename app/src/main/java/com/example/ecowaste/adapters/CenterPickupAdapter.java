package com.example.ecowaste.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecowaste.R;
import com.example.ecowaste.models.PickupModel;
import com.example.ecowaste.utils.StatusUtils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CenterPickupAdapter extends RecyclerView.Adapter<CenterPickupAdapter.ViewHolder> {

    private Context context;
    private List<PickupModel> pickupList;
    private OnPickupClickListener listener;

    public interface OnPickupClickListener {
        void onPickupClick(PickupModel pickup);
    }

    public CenterPickupAdapter(Context context, List<PickupModel> pickupList, OnPickupClickListener listener) {
        this.context = context;
        this.pickupList = pickupList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_center_pickup, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PickupModel pickup = pickupList.get(position);

        holder.tvUserName.setText(pickup.getUserName() != null ? pickup.getUserName() : "User");
        holder.tvWasteType.setText(pickup.getType());
        holder.tvStatus.setText(StatusUtils.getDisplayStatus(pickup.getStatus()));

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        if (pickup.getCreatedAt() != null) {
            holder.tvDate.setText(sdf.format(pickup.getCreatedAt()));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPickupClick(pickup);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pickupList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvWasteType, tvStatus, tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvWasteType = itemView.findViewById(R.id.tvWasteType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
