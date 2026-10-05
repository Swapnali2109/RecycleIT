package com.example.ecowaste.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecowaste.R;
import com.example.ecowaste.adapters.CenterPickupAdapter;
import com.example.ecowaste.models.PickupModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CenterDashboardActivity extends AppCompatActivity {

    private static final String TAG = "CenterDashboard";
    private TextView tvCenterName, tvTotalWaste, tvCO2Saved, tvPickups, tvEmptyState;
    private RecyclerView recyclerView;
    private BottomNavigationView bottomNavigation;
    private FirebaseFirestore db;
    private String currentUserId;
    private CenterPickupAdapter adapter;
    private final List<PickupModel> pickupList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_center_dashboard);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            currentUserId = user.getUid();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        loadDashboardData();
        loadMyInventory();
        loadBulkRequestCount();
        setupBulkNotificationListener();
        setupBottomNavigation();
    }

    private void initViews() {
        tvCenterName = findViewById(R.id.tvCenterName);
        tvTotalWaste = findViewById(R.id.tvTotalWaste);
        tvCO2Saved = findViewById(R.id.tvCO2Saved);
        tvPickups = findViewById(R.id.tvPickups);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        recyclerView = findViewById(R.id.recyclerView);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        
        View cardBulkRequests = findViewById(R.id.cardViewRequests);
        View cardRefresh = findViewById(R.id.cardRefresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CenterPickupAdapter(this, pickupList, pickup -> {
            Intent intent = new Intent(this, PickupDetailActivity.class);
            intent.putExtra("pickupId", pickup.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        findViewById(R.id.profileImage).setOnClickListener(v -> 
                startActivity(new Intent(this, ProfileActivity.class)));
        
        findViewById(R.id.btnNotification).setOnClickListener(v -> 
                startActivity(new Intent(this, NotificationsActivity.class)));

        if (cardBulkRequests != null) {
            cardBulkRequests.setOnClickListener(v -> 
                    startActivity(new Intent(this, BulkSaleRequestsActivity.class)));
        }

        if (cardRefresh != null) {
            cardRefresh.setOnClickListener(v -> {
                loadMyInventory();
                loadDashboardStats();
                loadBulkRequestCount();
            });
        }
    }

    private void loadDashboardData() {
        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                tvCenterName.setText(name != null ? name : "Recycling Center");
            }
        });
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        db.collection("pickups")
                .whereEqualTo("centerId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    
                    double total = 0;
                    int activeTasks = 0;
                    for (QueryDocumentSnapshot doc : value) {
                        PickupModel p = doc.toObject(PickupModel.class);
                        String status = p.getStatus();
                        
                        if ("COMPLETED".equals(status) || "SOLD".equals(status)) {
                            total += p.getEstimatedWeight();
                        }
                        if ("ACCEPTED_BY_COMPANY".equals(status)) {
                            activeTasks++;
                        }
                    }
                    tvTotalWaste.setText(String.format(Locale.getDefault(), "%.1f kg", total));
                    tvCO2Saved.setText(String.format(Locale.getDefault(), "%.1f kg", total * 1.5));
                    tvPickups.setText(String.valueOf(activeTasks));
                });
    }

    private void loadBulkRequestCount() {
        db.collection("bulk_sales")
                .whereArrayContains("targetCenterIds", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        int pendingCount = 0;
                        for (QueryDocumentSnapshot doc : value) {
                            Object obj = doc.get("centerStatuses");
                            if (obj instanceof Map) {
                                Map<String, String> statuses = (Map<String, String>) obj;
                                if (statuses != null && "PENDING".equals(statuses.get(currentUserId))) {
                                    pendingCount++;
                                }
                            }
                        }
                        TextView tvTitle = findViewById(R.id.tvCardTitle);
                        if (tvTitle != null) {
                            tvTitle.setText(pendingCount > 0 ? "Bulk Requests (" + pendingCount + ")" : "Bulk Requests");
                        }
                    }
                });
    }

    private void setupBulkNotificationListener() {
        db.collection("bulk_sales")
                .whereEqualTo("finalCenterId", currentUserId)
                .whereEqualTo("status", "COMPLETED")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    for (DocumentChange dc : value.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED || dc.getType() == DocumentChange.Type.MODIFIED) {
                            showLocalNotification("Bulk Offer Accepted!", "A member has accepted your offer. You can now arrange the pickup.");
                        }
                    }
                });
    }

    private void showLocalNotification(String title, String message) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "center_notifications_channel";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Center Alerts", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void loadMyInventory() {
        db.collection("pickups")
                .whereEqualTo("centerId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Inventory query failed: ", error);
                        return;
                    }

                    pickupList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                PickupModel p = doc.toObject(PickupModel.class);
                                p.setId(doc.getId());
                                String status = p.getStatus();

                                if ("ACCEPTED_BY_COMPANY".equals(status) || 
                                    "SOLD".equals(status) || 
                                    "COMPLETED".equals(status)) {
                                    pickupList.add(p);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing document", e);
                            }
                        }
                    }
                    
                    adapter.notifyDataSetChanged();
                    
                    if (pickupList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        tvEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        tvEmptyState.setVisibility(View.GONE);
                    }
                });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_history) {
                startActivity(new Intent(this, PickupHistoryActivity.class));
                return true;
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }
}
