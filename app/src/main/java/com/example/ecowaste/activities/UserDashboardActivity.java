package com.example.ecowaste.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecowaste.R;
import com.example.ecowaste.adapters.DashboardPickupAdapter;
import com.example.ecowaste.models.PickupModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserDashboardActivity extends AppCompatActivity {

    private TextView tvUserName, tvEcoPoints, tvRank;
    private CardView cardRequestPickup, cardHistory, cardAwareness, cardGallery;
    private View cardProfile;
    private RecyclerView recyclerView;
    private BottomNavigationView bottomNavigation;
    private LinearLayout emptyState;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DashboardPickupAdapter adapter;
    private List<PickupModel> pickupList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        loadUserData();
        loadUserPickups();
        setupClickListeners();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvEcoPoints = findViewById(R.id.tvEcoPoints);
        tvRank = findViewById(R.id.tvRank);
        cardRequestPickup = findViewById(R.id.cardRequestPickup);
        cardHistory = findViewById(R.id.cardHistory);
        cardAwareness = findViewById(R.id.cardAwareness);
        cardGallery = findViewById(R.id.cardGallery);
        cardProfile = findViewById(R.id.cardProfile);
        recyclerView = findViewById(R.id.recyclerView);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        emptyState = findViewById(R.id.emptyState);

        View btnNotification = findViewById(R.id.btnNotification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        }
    }

    private void setupRecyclerView() {
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new DashboardPickupAdapter(this, pickupList);
            recyclerView.setAdapter(adapter);
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid)
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Log.e("UserDashboard", "Listen failed.", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            Long points = documentSnapshot.getLong("ecoPoints");

                            if (tvUserName != null) {
                                tvUserName.setText(name != null ? name : "User");
                            }
                            if (tvEcoPoints != null) {
                                tvEcoPoints.setText(String.valueOf(points != null ? points : 0));
                            }
                            
                            // Simple Rank Logic
                            if (tvRank != null) {
                                long p = (points != null ? points : 0);
                                if (p > 500) tvRank.setText("Gold");
                                else if (p > 200) tvRank.setText("Silver");
                                else tvRank.setText("Bronze");
                            }
                        }
                    });
        }
    }

    private void loadUserPickups() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            // Note: If data doesn't appear, check Logcat for a link to create a Firestore Index
            db.collection("pickups")
                    .whereEqualTo("userId", uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(5)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.e("UserDashboard", "Error fetching pickups: ", error);
                            return;
                        }
                        if (value != null) {
                            pickupList.clear();
                            for (QueryDocumentSnapshot doc : value) {
                                try {
                                    PickupModel pickup = doc.toObject(PickupModel.class);
                                    pickup.setId(doc.getId());
                                    pickupList.add(pickup);
                                } catch (Exception e) {
                                    Log.e("UserDashboard", "Parsing error", e);
                                }
                            }
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }

                            if (pickupList.isEmpty()) {
                                if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                                if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                            } else {
                                if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                                if (emptyState != null) emptyState.setVisibility(View.GONE);
                            }
                        }
                    });
        }
    }

    private void setupClickListeners() {
        if (cardRequestPickup != null) {
            cardRequestPickup.setOnClickListener(v ->
                    startActivity(new Intent(UserDashboardActivity.this, RequestPickupActivity.class)));
        }
        if (cardHistory != null) {
            cardHistory.setOnClickListener(v ->
                    startActivity(new Intent(UserDashboardActivity.this, PickupHistoryActivity.class)));
        }
        if (cardAwareness != null) {
            cardAwareness.setOnClickListener(v ->
                    startActivity(new Intent(UserDashboardActivity.this, AwarenessActivity.class)));
        }
        if (cardGallery != null) {
            cardGallery.setOnClickListener(v ->
                    startActivity(new Intent(UserDashboardActivity.this, GalleryActivity.class)));
        }
        if (cardProfile != null) {
            cardProfile.setOnClickListener(v ->
                    startActivity(new Intent(UserDashboardActivity.this, ProfileActivity.class)));
        }
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    return true;
                } else if (id == R.id.nav_history) {
                    startActivity(new Intent(UserDashboardActivity.this, PickupHistoryActivity.class));
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(UserDashboardActivity.this, ProfileActivity.class));
                    return true;
                }
                return false;
            });
        }
    }
}
