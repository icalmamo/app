package com.example.h_cas;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.h_cas.database.FirebaseSyncManager;

/**
 * HCasApplication initializes Firebase and other app-wide components
 */
public class HCasApplication extends Application {
    
    private static final String TAG = "HCasApplication";
    private FirebaseSyncManager syncManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase with comprehensive error handling to prevent crashes
        // Firebase initialization is completely optional - app works fine without it
        initializeFirebaseAsync();
    }
    
    /**
     * Initialize Firebase asynchronously to prevent blocking the main thread
     * This method is safe and won't crash the app even if Firebase fails
     */
    private void initializeFirebaseAsync() {
        // Run in background thread to avoid blocking app startup
        new Thread(() -> {
            try {
                // Check if Firebase is already initialized
                try {
                    if (FirebaseApp.getApps(this).isEmpty()) {
                        // Try to initialize Firebase
                        FirebaseApp.initializeApp(this);
                        Log.d(TAG, "Firebase initialized successfully");
                    } else {
                        Log.d(TAG, "Firebase already initialized");
                    }
                    
                    // Enable persistence BEFORE any other FirebaseDatabase usage
                    // This must be done before any getInstance() calls that use the database
                    try {
                        FirebaseDatabase database = FirebaseDatabase.getInstance("https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/");
                        database.setPersistenceEnabled(true);
                        Log.d(TAG, "✅ Firebase Realtime Database persistence enabled");
                    } catch (Exception e) {
                        Log.w(TAG, "Could not enable persistence (may already be set): " + e.getMessage());
                    }
                    
                    // Authenticate with Firebase anonymously (for Realtime Database access)
                    // Wait for authentication to complete before starting sync
                    authenticateFirebaseAnonymouslyAndStartSync();
                    
                    // Test Firebase connectivity (non-blocking)
                    testFirebaseConnection();
                    
                } catch (IllegalStateException e) {
                    // Firebase not configured - this is OK, app works without it
                    Log.w(TAG, "Firebase not configured - app will work without Firebase sync");
                    syncManager = null;
                } catch (Exception e) {
                    Log.e(TAG, "Firebase initialization failed - app will continue without Firebase", e);
                    syncManager = null;
                }
            } catch (Exception e) {
                // Catch-all to prevent any crash
                Log.e(TAG, "Error in Firebase initialization - app continues normally", e);
                syncManager = null;
            }
        }).start();
    }
    
    /**
     * Get Firebase Sync Manager instance
     */
    public FirebaseSyncManager getSyncManager() {
        return syncManager;
    }
    
    /**
     * Authenticate with Firebase anonymously and start sync after authentication
     * This ensures authentication is complete before starting listeners
     */
    private void authenticateFirebaseAnonymouslyAndStartSync() {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth != null) {
                // Check if user is already signed in
                if (auth.getCurrentUser() == null) {
                    // Sign in anonymously, then start sync
                    auth.signInAnonymously()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "✅ Firebase Anonymous Authentication successful");
                                Log.d(TAG, "   User ID: " + (auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "null"));
                                
                                // Now start sync after authentication
                                startFirebaseSync();
                            } else {
                                Log.w(TAG, "⚠️ Firebase Anonymous Authentication failed - sync may not work", task.getException());
                                Log.w(TAG, "   Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"));
                                Log.w(TAG, "   Please enable Anonymous Authentication in Firebase Console:");
                                Log.w(TAG, "   Firebase Console → Authentication → Sign-in method → Anonymous → Enable");
                                // Try to start sync anyway (might work if rules allow)
                                startFirebaseSync();
                            }
                        });
                } else {
                    Log.d(TAG, "✅ Firebase user already authenticated: " + auth.getCurrentUser().getUid());
                    // Already authenticated, start sync immediately
                    startFirebaseSync();
                }
            } else {
                Log.w(TAG, "FirebaseAuth instance is null");
                // Try to start sync anyway
                startFirebaseSync();
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not authenticate with Firebase anonymously", e);
            Log.w(TAG, "   Update Firebase Realtime Database rules to allow unauthenticated access for testing");
            // Try to start sync anyway
            startFirebaseSync();
        }
    }
    
    /**
     * Start Firebase sync manager and listeners
     */
    private void startFirebaseSync() {
        try {
            syncManager = new FirebaseSyncManager(this);
            
            // Start listening to real-time updates
            if (syncManager != null) {
                syncManager.startListeningToUpdates();
                Log.d(TAG, "✅ Firebase real-time sync started");
            } else {
                Log.w(TAG, "⚠️ FirebaseSyncManager is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize FirebaseSyncManager - app will continue without sync", e);
            syncManager = null;
        }
    }
    
    /**
     * Test Firebase connection by attempting to access Firestore
     */
    private void testFirebaseConnection() {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            if (db != null) {
                Log.d(TAG, "Firebase Firestore instance created successfully");
                try {
                    Log.d(TAG, "Firebase Project ID: " + FirebaseApp.getInstance().getOptions().getProjectId());
                    Log.d(TAG, "Firebase App Name: " + FirebaseApp.getInstance().getName());
                    Log.d(TAG, "✅ Firebase is connected and ready!");
                } catch (Exception e) {
                    Log.w(TAG, "Could not get Firebase details", e);
                }
            } else {
                Log.e(TAG, "❌ Firebase Firestore instance is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error testing Firebase connection - app will continue without Firebase", e);
            // Don't crash - app can work without Firebase
        }
    }
}


