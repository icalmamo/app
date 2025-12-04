package com.example.h_cas.database;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * FirebaseOnlineOnly - Pure online mode for Firebase Realtime Database
 * 
 * This class ensures Firebase ALWAYS connects directly to the server:
 * - No offline cache
 * - No persistence
 * - No queued writes
 * - Always fetches fresh data from cloud
 */
public class FirebaseOnlineOnly {
    
    private static final String TAG = "FirebaseOnlineOnly";
    private static boolean initialized = false;
    private static FirebaseDatabase database;
    private static com.google.firebase.database.ValueEventListener connectionListener;
    private static boolean isOnline = true; // Cache connection state to avoid frequent checks
    
    /**
     * Initialize Firebase in pure online mode
     * Call this once at app startup (in Application class)
     */
    public static void init() {
        if (initialized) {
            Log.d(TAG, "✅ Already initialized - pure online mode active");
            return;
        }
        
        try {
            // Get database instance with full URL
            database = FirebaseDatabase.getInstance("https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/");
            
            // PURE ONLINE MODE - Disable all offline persistence
            try {
                database.setPersistenceEnabled(false);
                Log.d(TAG, "✅ Persistence disabled - pure online mode");
            } catch (Exception e) {
                // Firebase throws if called twice or after first use - ignore
                Log.w(TAG, "Persistence already set (may be enabled elsewhere): " + e.getMessage());
            }
            
            // FIX ghost-offline Firebase state
            // Force disconnect & reconnect to ensure we're online
            database.goOffline();
            database.goOnline();
            Log.d(TAG, "✅ Forced online connection - no offline mode");
            
            initialized = true;
            Log.d(TAG, "✅ FirebaseOnlineOnly initialized - PURE ONLINE MODE ACTIVE");
            
            // Start connection state monitoring
            startConnectionMonitoring();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize FirebaseOnlineOnly", e);
        }
    }
    
    /**
     * Get a database reference with no caching
     * 
     * @param path Database path (e.g., "patients", "patients/PAT123")
     * @return DatabaseReference with keepSynced(false)
     */
    public static DatabaseReference ref(String path) {
        if (!initialized) {
            Log.w(TAG, "⚠️ Not initialized - calling init() now");
            init();
        }
        
        if (database == null) {
            Log.e(TAG, "❌ Database is null - cannot get reference");
            return null;
        }
        
        DatabaseReference r = database.getReference(path);
        
        // No caching allowed - always fetch from server
        r.keepSynced(false);
        
        return r;
    }
    
    /**
     * Get the root database reference
     */
    public static DatabaseReference getRootRef() {
        return ref("");
    }
    
    /**
     * Monitor connection state to verify we're online
     * OPTIMIZED: Only logs on state change, reduces logging overhead
     */
    private static void startConnectionMonitoring() {
        try {
            DatabaseReference connectedRef = database.getReference(".info/connected");
            connectionListener = new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    Boolean connected = snapshot.getValue(Boolean.class);
                    boolean wasOnline = isOnline;
                    isOnline = Boolean.TRUE.equals(connected);
                    
                    // Only log on state change to reduce logging overhead
                    if (wasOnline != isOnline) {
                        if (isOnline) {
                            Log.d(TAG, "🟢 ONLINE — Connected to Firebase server");
                        } else {
                            Log.w(TAG, "🔴 OFFLINE — Attempting to reconnect...");
                            database.goOnline();
                        }
                    }
                }
                
                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {
                    Log.e(TAG, "❌ Connection monitoring cancelled: " + error.getMessage());
                }
            };
            connectedRef.addValueEventListener(connectionListener);
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start connection monitoring", e);
        }
    }
    
    /**
     * Stop connection monitoring (call on app shutdown to prevent memory leak)
     */
    public static void stopConnectionMonitoring() {
        if (connectionListener != null && database != null) {
            try {
                database.getReference(".info/connected").removeEventListener(connectionListener);
                connectionListener = null;
            } catch (Exception e) {
                Log.w(TAG, "Error stopping connection monitoring", e);
            }
        }
    }
    
    /**
     * Get cached connection state (fast, no network call)
     */
    public static boolean isOnline() {
        return isOnline;
    }
    
    /**
     * Force database to go online (call if connection is lost)
     * OPTIMIZED: Only calls goOnline() if not already online (cached check)
     */
    public static void ensureOnline() {
        if (database != null && !isOnline) {
            database.goOnline();
            // Note: isOnline will be updated by connection listener
        }
    }
    
    /**
     * Check if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
}

