package com.example.ecowaste.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.example.ecowaste.R;
import com.example.ecowaste.models.NotificationModel;
import com.example.ecowaste.models.PickupModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RequestPickupActivity extends AppCompatActivity {

    private Spinner spinnerCategory, spinnerType;
    private EditText etDescription, etWeight, etAddress, etArea;
    private Button btnSubmit;
    private ImageView ivCategoryPreview;
    private MaterialToolbar toolbar;

    private String[] categories = {"Mobile", "Laptop", "Television", "Battery", "Charger", "Other Electronics"};
    private Map<String, List<String>> typeMap = new HashMap<>();
    private Map<String, Integer> imageMap = new HashMap<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_pickup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeData();
        initViews();
        setupSpinners();
        setupClickListeners();
    }

    private void initializeData() {
        // Dynamic Type Mapping
        typeMap.put("Mobile", Arrays.asList("Smartphone", "Feature Phone", "Broken Phone", "Mobile Battery"));
        typeMap.put("Laptop", Arrays.asList("Gaming Laptop", "Business Laptop", "Old Laptop", "Laptop Battery"));
        typeMap.put("Television", Arrays.asList("LED TV", "LCD TV", "CRT TV", "Remote Control"));
        typeMap.put("Battery", Arrays.asList("Lead Acid", "Lithium Ion", "Dry Cell", "UPS Battery"));
        typeMap.put("Charger", Arrays.asList("Mobile Charger", "Laptop Adapter", "Power Cable"));
        typeMap.put("Other Electronics", Arrays.asList("Headphones", "Keyboard/Mouse", "Printers", "Scrap Wires"));

        // Static Image Mapping with new realistic icons
        imageMap.put("Mobile", R.drawable.ic_mobile_realistic);
        imageMap.put("Laptop", R.drawable.ic_laptop_realistic);
        imageMap.put("Television", R.drawable.ic_tv_realistic);
        imageMap.put("Battery", R.drawable.ic_battery_realistic);
        imageMap.put("Charger", R.drawable.ic_charger_realistic);
        imageMap.put("Other Electronics", R.drawable.other_waste);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerType = findViewById(R.id.spinnerType);
        etDescription = findViewById(R.id.etDescription);
        etWeight = findViewById(R.id.etWeight);
        etAddress = findViewById(R.id.etAddress);
        etArea = findViewById(R.id.etArea);
        btnSubmit = findViewById(R.id.btnSubmit);
        ivCategoryPreview = findViewById(R.id.ivCategoryPreview);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categories[position];
                updateTypeSpinner(selectedCategory);
                updateImagePreview(selectedCategory);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateTypeSpinner(String category) {
        List<String> types = typeMap.get(category);
        if (types != null) {
            ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerType.setAdapter(typeAdapter);
        }
    }

    private void updateImagePreview(String category) {
        Integer resId = imageMap.get(category);
        if (resId != null) {
            ivCategoryPreview.setImageResource(resId);
            // Add a small pop-in animation for the preview
            ivCategoryPreview.setScaleX(0.8f);
            ivCategoryPreview.setScaleY(0.8f);
            ivCategoryPreview.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
        }
    }

    private void setupClickListeners() {
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void validateAndSubmit() {
        String description = etDescription.getText().toString().trim();
        String weight = etWeight.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String area = etArea.getText().toString().trim();

        if (TextUtils.isEmpty(weight) || TextUtils.isEmpty(address) || TextUtils.isEmpty(area)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        submitData(description, weight, address, area);
    }

    private void submitData(String desc, String weight, String addr, String area) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        db.collection("users").document(user.getUid()).get().addOnSuccessListener(doc -> {
            PickupModel pickup = new PickupModel();
            pickup.setId(UUID.randomUUID().toString());
            pickup.setUserId(user.getUid());
            pickup.setUserName(doc.getString("name"));
            pickup.setUserContact(doc.getString("phone"));
            pickup.setCategory(spinnerCategory.getSelectedItem().toString());
            pickup.setType(spinnerType.getSelectedItem().toString());
            pickup.setDescription(desc);
            pickup.setEstimatedWeight(Double.parseDouble(weight));
            pickup.setAddress(addr);
            pickup.setArea(area);
            pickup.setStatus("PENDING_REVIEW");
            pickup.setImageResId(imageMap.get(pickup.getCategory()));
            pickup.setCreatedAt(new Date());
            
            String pin = String.format(Locale.getDefault(), "%04d", new Random().nextInt(10000));
            pickup.setPickupPin(pin);

            db.collection("pickups").document(pickup.getId()).set(pickup)
                    .addOnSuccessListener(aVoid -> {
                        String title = "Request Submitted";
                        String msg = "Your request has been successfully sent to RecycleIT.";
                        sendPushAndInAppNotification(user.getUid(), title, msg, pickup.getId());
                        
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit Request");
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void sendPushAndInAppNotification(String targetUserId, String title, String message, String pickupId) {
        String id = UUID.randomUUID().toString();
        NotificationModel n = new NotificationModel();
        n.setId(id); n.setUserId(targetUserId); n.setTitle(title); n.setMessage(message);
        n.setTimestamp(new Date()); n.setRead(false);
        
        db.collection("notifications").document(id).set(n);
        showRealPopupNotification(title, message, pickupId);
    }

    private void showRealPopupNotification(String title, String message, String pickupId) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String cid = "ecowaste_pro_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(cid, "EcoWaste Live Alerts", NotificationManager.IMPORTANCE_HIGH));
        }
        Intent i = new Intent(this, PickupDetailActivity.class);
        i.putExtra("pickupId", pickupId);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, cid)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setContentIntent(pi).setAutoCancel(true);
        nm.notify((int) System.currentTimeMillis(), b.build());
    }
}