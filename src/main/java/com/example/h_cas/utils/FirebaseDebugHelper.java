package com.example.h_cas.utils;

import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Firebase Debug Helper - Para sa testing at debugging ng Firebase Realtime Database
 */
public class FirebaseDebugHelper {
    
    private static final String TAG = "FirebaseDebug";
    
    /**
     * Test manual write - simplest test para malaman kung rules o code ang problema
     * 
     * @param context Context para sa Toast (optional, can be null)
     * @return true kung successful, false kung failed
     */
    public static boolean testManualWrite(android.content.Context context) {
        try {
            DatabaseReference testRef = FirebaseDatabase.getInstance(
                    "https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("test");
            
            testRef.setValue("hello_" + System.currentTimeMillis())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ TEST WRITE OK - Rules are working!");
                        if (context != null) {
                            Toast.makeText(context, "✅ Firebase write test: SUCCESS", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ TEST WRITE FAILED: " + e.getMessage());
                        if (e.getMessage() != null && e.getMessage().toLowerCase().contains("permission")) {
                            Log.e(TAG, "⚠️ PERMISSION DENIED - This is a security rules issue!");
                            Log.e(TAG, "   Solution: Update Firebase Console → Realtime Database → Rules");
                        }
                        if (context != null) {
                            Toast.makeText(context, "❌ Firebase write test: FAILED - " + e.getMessage(), 
                                    Toast.LENGTH_LONG).show();
                        }
                    });
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in test write: " + e.getMessage());
            if (context != null) {
                Toast.makeText(context, "❌ Test error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return false;
        }
    }
    
    /**
     * Check Firebase connection status
     */
    public static void checkFirebaseStatus() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance(
                    "https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/");
            
            Log.d(TAG, "📊 Firebase Status Check:");
            Log.d(TAG, "   Database URL: " + database.getReference().toString());
            Log.d(TAG, "   Persistence Enabled: " + database.getApp().getOptions().getDatabaseUrl());
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Firebase status check failed: " + e.getMessage());
        }
    }
    
    /**
     * Test read from Firebase
     */
    public static void testRead(String path) {
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance(
                    "https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference(path);
            
            ref.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Log.d(TAG, "✅ READ SUCCESS from path: " + path);
                        Log.d(TAG, "   Data: " + snapshot.getValue());
                    } else {
                        Log.w(TAG, "⚠️ Path exists but no data: " + path);
                    }
                }
                
                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {
                    Log.e(TAG, "❌ READ FAILED from path: " + path);
                    Log.e(TAG, "   Error: " + error.getMessage());
                    if (error.getMessage() != null && error.getMessage().toLowerCase().contains("permission")) {
                        Log.e(TAG, "⚠️ PERMISSION DENIED - Check security rules!");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in test read: " + e.getMessage());
        }
    }
}


