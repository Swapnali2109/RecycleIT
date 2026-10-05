package com.example.ecowaste.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecowaste.R;
import com.example.ecowaste.adapters.CollectorPickupAdapter;
import com.example.ecowaste.models.PickupModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CollectorDashboardActivity extends AppCompatActivity {

    private RecyclerView rvAssignedPickups;
    private CollectorPickupAdapter adapter;
    private List<PickupModel> pickupList = new ArrayList<>();
    private FirebaseFirestore db;
    private String collectorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collector_dashboard);

        db = FirebaseFirestore.getInstance();
        collectorId = getIntent().getStringExtra("collectorId");

        if (collectorId == null) {
            Toast.makeText(this, "Session error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        rvAssignedPickups = findViewById(R.id.rvAssignedPickups);

        rvAssignedPickups.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CollectorPickupAdapter(pickupList, pickupId -> {
            Intent intent = new Intent(this, PickupDetailActivity.class);
            intent.putExtra("pickupId", pickupId);
            intent.putExtra("collectorId", collectorId);
            startActivity(intent);
        });
        rvAssignedPickups.setAdapter(adapter);

        loadAssignedPickups();
    }

    private void loadAssignedPickups() {
        db.collection("pickups")
                .whereEqualTo("collectorId", collectorId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    pickupList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            PickupModel pickup = doc.toObject(PickupModel.class);
                            pickup.setId(doc.getId());
                            pickupList.add(pickup);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}