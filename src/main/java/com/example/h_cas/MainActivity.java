package com.example.h_cas;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.h_cas.database.FirebaseHelper;

/**
 * MainActivity serves as the main dashboard for regular healthcare staff
 * (non-admin users) after they log in successfully.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FirebaseTest";
    
    private TextView welcomeTextView;
    private Button logoutButton;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Initialize Firebase before using FirebaseHelper
        FirebaseApp.initializeApp(this);
        firebaseHelper = new FirebaseHelper();

        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupClickListeners();
        testFirebaseConnection();
    }
    
    /**
     * Test Firebase Realtime Database connection
     */
    private void testFirebaseConnection() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/");
            DatabaseReference myRef = database.getReference("connectionTest");

            // Write data
            myRef.setValue("Hello Firebase!");

            // Read data
            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String value = dataSnapshot.getValue(String.class);
                    Log.d(TAG, "Value is: " + value);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.w(TAG, "Failed to read value.", error.toException());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error testing Firebase connection", e);
        }
    }

    /**
     * Initialize all view references from the layout
     */
    private void initializeViews() {
        welcomeTextView = findViewById(R.id.welcomeTextView);
        logoutButton = findViewById(R.id.logoutButton);
        
        // Set welcome message
        welcomeTextView.setText("Welcome to HCAS Healthcare Assistant System");
    }

    /**
     * Setup click listeners for interactive elements
     */
    private void setupClickListeners() {
        logoutButton.setOnClickListener(v -> handleLogout());
    }

    /**
     * Handle user logout
     */
    private void handleLogout() {
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}