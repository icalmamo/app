package com.example.h_cas.database;

import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.example.h_cas.database.FirebaseOnlineOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FirebaseHelper handles sync, read, and listener operations for Firebase Realtime Database
 */
public class FirebaseHelper {


    private static final String TAG = "FirebaseHelper";
    private final FirebaseDatabase database;
    private final DatabaseReference rootRef;
    private final List<ValueEventListener> activeListeners = new ArrayList<>();
    private final List<Query> listenerQueries = new ArrayList<>();
    
    private static final int DEFAULT_LIMIT = 100;

    // Define your table/collection paths
    private static final String PATH_EMPLOYEES = "employees";
    private static final String PATH_PATIENTS = "patients";
    private static final String PATH_PRESCRIPTIONS = "prescriptions";
    private static final String PATH_MEDICINES = "medicines";
    private static final String PATH_CASES = "healthcare_cases";
    private static final String PATH_RFID_DATA = "rfid_data";

    /**
     * Constructor: initializes Firebase connection in pure online mode
     */
    public FirebaseHelper() {
        // Ensure FirebaseOnlineOnly is initialized (pure online mode)
        FirebaseOnlineOnly.init();
        
        // Get database instance (will use pure online mode from FirebaseOnlineOnly)
        database = FirebaseDatabase.getInstance("https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/");
        
        // Get root reference using FirebaseOnlineOnly (ensures no caching)
        DatabaseReference tempRootRef = FirebaseOnlineOnly.getRootRef();
        
        if (tempRootRef == null) {
            // Fallback to direct reference if FirebaseOnlineOnly fails
            tempRootRef = database.getReference();
            tempRootRef.keepSynced(false); // Ensure no caching
            Log.w(TAG, "⚠️ Using fallback rootRef (FirebaseOnlineOnly returned null)");
        }
        
        // Assign to final field (can only be done once)
        rootRef = tempRootRef;
        
        Log.d(TAG, "✅ Firebase initialized in PURE ONLINE MODE");
        Log.d(TAG, "   - No offline cache");
        Log.d(TAG, "   - No persistence");
        Log.d(TAG, "   - Always connected to server");
    }
    
    // ✅ Check if Firebase is available
    public boolean isFirebaseAvailable() {
        try {
            FirebaseApp.getInstance();
            return true;
        } catch (IllegalStateException e) {
            Log.e(TAG, "Firebase not initialized: ", e);
            return false;
        }
    }

    // ✅ Example method to write data
    public void writeData(String node, String key, Object value) {
        if (rootRef == null) {
            Log.w(TAG, "Firebase rootRef is null");
            return;
        }
        rootRef.child(node).child(key).setValue(value)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Data written successfully"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to write data", e));
    }

    // ✅ Example method to read data
    public DatabaseReference getReference(String node) {
        if (rootRef == null) {
            Log.w(TAG, "Firebase rootRef is null");
            return null;
        }
        return rootRef.child(node);
    }

    /* ─────────────────────────────────────────────
     * 🔹 SYNC METHODS
     * ───────────────────────────────────────────── */

    public void syncEmployeeToFirebase(String employeeId, Map<String, Object> employeeData) {
        writeToFirebase(PATH_EMPLOYEES, employeeId, employeeData);
    }

    public void syncPatientToFirebase(String patientId, Map<String, Object> patientData) {
        // Optional: Ensure user is authenticated
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                Log.w(TAG, "⚠️ Anonymous auth recommended for patient sync");
            }
        } catch (Exception e) {
            Log.w(TAG, "Auth check failed", e);
        }
        writeToFirebase(PATH_PATIENTS, patientId, patientData);
    }

    public void syncPrescriptionToFirebase(String prescriptionId, Map<String, Object> prescriptionData) {
        writeToFirebase(PATH_PRESCRIPTIONS, prescriptionId, prescriptionData);
    }

    public void syncMedicineToFirebase(String medicineId, Map<String, Object> medicineData) {
        writeToFirebase(PATH_MEDICINES, medicineId, medicineData);
    }

    public void syncCaseToFirebase(String caseId, Map<String, Object> caseData) {
        writeToFirebase(PATH_CASES, caseId, caseData);
    }

    public void syncRFIDDataToFirebase(String rfidTagId, Map<String, Object> rfidData) {
        writeToFirebase(PATH_RFID_DATA, rfidTagId, rfidData);
    }

    /* ─────────────────────────────────────────────
     * 🔹 CORE WRITE METHOD
     * ───────────────────────────────────────────── */
    private void writeToFirebase(String path, String id, Map<String, Object> data) {
        if (rootRef == null) {
            Log.w(TAG, "❌ Firebase rootRef is null");
            return;
        }

        // Validate input parameters
        if (path == null || path.isEmpty()) {
            Log.e(TAG, "❌ Cannot write: path is null or empty");
            return;
        }
        if (id == null || id.isEmpty()) {
            Log.e(TAG, "❌ Cannot write: id is null or empty for path: " + path);
            return;
        }
        if (data == null) {
            Log.e(TAG, "❌ Cannot write: data is null for path: " + path + "/" + id);
            return;
        }

        // Check authentication status before writing
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth != null && auth.getCurrentUser() != null) {
                // User authenticated - no need to log
            } else {
                Log.w(TAG, "⚠️ No authenticated user - writes may fail if security rules require auth");
                Log.w(TAG, "   Enable Anonymous Authentication in Firebase Console:");
                Log.w(TAG, "   Authentication → Sign-in method → Anonymous → Enable");
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not check auth status", e);
        }

        // Ensure database is online (pure online mode - no offline queue)
        // OPTIMIZED: Only check if cached state shows offline
        if (!FirebaseOnlineOnly.isOnline()) {
            try {
                FirebaseOnlineOnly.ensureOnline();
            } catch (Exception e) {
                Log.w(TAG, "Could not ensure database is online", e);
            }
        }

        // Use FirebaseOnlineOnly for pure online writes (no caching)
        DatabaseReference ref;
        try {
            ref = FirebaseOnlineOnly.ref(path + "/" + id);
            if (ref == null) {
                // Fallback to direct reference
                ref = rootRef.child(path).child(id);
                ref.keepSynced(false); // Ensure no caching
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get FirebaseOnlineOnly ref, using fallback", e);
            ref = rootRef.child(path).child(id);
            ref.keepSynced(false); // Ensure no caching
        }
        // OPTIMIZED: Reduced logging - only log on errors or slow operations
        long writeStartTime = System.currentTimeMillis();
        
        ref.setValue(data)
                .addOnSuccessListener(aVoid -> {
                    long writeDuration = System.currentTimeMillis() - writeStartTime;
                    // Only log if write takes longer than 500ms (performance issue)
                    if (writeDuration > 500) {
                        Log.w(TAG, "⚠️ Slow write to " + path + "/" + id + " took " + writeDuration + "ms");
                    }
                    // Success logging removed to reduce overhead - errors still logged
                })
                .addOnFailureListener(e -> {
                    long writeDuration = System.currentTimeMillis() - writeStartTime;
                    Log.e(TAG, "❌ Failed to sync to Firebase path: " + path + "/" + id);
                    Log.e(TAG, "   Failed after: " + writeDuration + "ms");
                    Log.e(TAG, "   Error: " + e.getMessage());
                    Log.e(TAG, "   Error Code: " + (e.getClass().getSimpleName()));
                    
                    // Detailed error diagnosis
                    String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                    String errorClass = e.getClass().getSimpleName();
                    
                    if (errorMsg.contains("permission") || errorMsg.contains("denied")) {
                        Log.e(TAG, "   ⚠️ PERMISSION DENIED - This is a security rules issue!");
                        Log.e(TAG, "   Solutions:");
                        Log.e(TAG, "   1. Enable Anonymous Authentication in Firebase Console");
                        Log.e(TAG, "   2. Or update security rules to allow writes (for testing)");
                        Log.e(TAG, "   3. Make sure rules are PUBLISHED after editing");
                        Log.e(TAG, "   4. See FIREBASE_REALTIME_DATABASE_RULES.md for details");
                    } else if (errorMsg.contains("network") || errorMsg.contains("connection") || 
                               errorMsg.contains("unreachable") || errorClass.contains("Network")) {
                        Log.e(TAG, "   ⚠️ NETWORK ERROR - Check device internet connection");
                        Log.e(TAG, "   Solutions:");
                        Log.e(TAG, "   1. Check WiFi/mobile data is enabled");
                        Log.e(TAG, "   2. Disable VPN/proxy if enabled");
                        Log.e(TAG, "   3. Check firewall settings");
                        Log.e(TAG, "   4. Try different network");
                    } else if (errorMsg.contains("offline") || errorClass.contains("Offline")) {
                        Log.e(TAG, "   ⚠️ OFFLINE ERROR - Database is offline");
                        Log.e(TAG, "   Solutions:");
                        Log.e(TAG, "   1. Check if goOnline() was called");
                        Log.e(TAG, "   2. Check network connectivity");
                        Log.e(TAG, "   3. Restart app");
                    } else {
                        Log.e(TAG, "   ⚠️ UNKNOWN ERROR - Check full stack trace below");
                    }
                    e.printStackTrace();
                });
        
        // Log that write operation was initiated
        // Write operation initiated
    }

    /* ─────────────────────────────────────────────
     * 🔹 READ METHODS
     * ───────────────────────────────────────────── */

    public void getAllPatientsFromFirebase(FirebaseDataCallback callback) {
        readLimitedFromFirebase(PATH_PATIENTS, DEFAULT_LIMIT, callback);
    }

    public void getAllEmployeesFromFirebase(FirebaseDataCallback callback) {
        readLimitedFromFirebase(PATH_EMPLOYEES, DEFAULT_LIMIT, callback);
    }

    public void getAllMedicinesFromFirebase(FirebaseDataCallback callback) {
        readLimitedFromFirebase(PATH_MEDICINES, DEFAULT_LIMIT, callback);
    }

    public void getLatestFromFirebase(String path, int limit, FirebaseDataCallback callback) {
        readLimitedFromFirebase(path, limit > 0 ? limit : DEFAULT_LIMIT, callback);
    }
    
    private void readLimitedFromFirebase(String path, int limit, FirebaseDataCallback callback) {
        // Validate inputs
        if (path == null || path.isEmpty()) {
            Log.e(TAG, "❌ Cannot read: path is null or empty");
            if (callback != null) callback.onError(new IllegalArgumentException("Path is null or empty"));
            return;
        }
        
        if (rootRef == null) {
            Log.e(TAG, "❌ Cannot read: Firebase rootRef is null");
            if (callback != null) callback.onError(new IllegalStateException("Firebase reference unavailable"));
            return;
        }
        
        // Ensure database is online (pure online mode - no offline cache)
        // OPTIMIZED: Only check if cached state shows offline
        if (!FirebaseOnlineOnly.isOnline()) {
            try {
                FirebaseOnlineOnly.ensureOnline();
            } catch (Exception e) {
                Log.w(TAG, "Could not ensure database is online", e);
            }
        }
        
        // Check authentication status
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth != null && auth.getCurrentUser() != null) {
                // User authenticated - no need to log
            } else {
                Log.w(TAG, "⚠️ No authenticated user - reads may fail if security rules require auth");
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not check auth status for read", e);
        }
        
        Query query = buildLimitedQuery(path, limit, null);
        if (query == null) {
            Log.e(TAG, "❌ Cannot read: query is null for path: " + path);
            if (callback != null) callback.onError(new IllegalStateException("Firebase query unavailable"));
            return;
        }
        
        // OPTIMIZED: Reduced verbose logging - only log errors
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int count = 0;
                if (snapshot.exists()) {
                    if (snapshot.hasChildren()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            try {
                                Map<String, Object> data = (Map<String, Object>) child.getValue();
                                if (data != null) {
                                    if (callback != null) callback.onDataReceived(child.getKey(), data);
                                    count++;
                                } else {
                                    Log.w(TAG, "⚠️ Null data for key: " + child.getKey() + " in path: " + path);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "❌ Error processing child data for key: " + child.getKey(), e);
                            }
                        }
                        // OPTIMIZED: Only log if significant number of items or if empty (potential issue)
                        if (count == 0) {
                            Log.w(TAG, "⚠️ No items found in path: " + path);
                        } else if (count > 50) {
                            Log.d(TAG, "✅ Read " + count + " items from path: " + path);
                        }
                    } else {
                        Log.w(TAG, "⚠️ Path exists but has no children: " + path);
                    }
                } else {
                    Log.w(TAG, "⚠️ Path does not exist in Firebase: " + path);
                    Log.w(TAG, "   Check Firebase Console → Realtime Database → Data → " + path);
                }
                if (callback != null) callback.onComplete();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "❌ Read failed for path: " + path);
                Log.e(TAG, "   Error Code: " + error.getCode());
                Log.e(TAG, "   Error Message: " + error.getMessage());
                
                // Detailed error diagnosis
                String errorMsg = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
                int errorCode = error.getCode();
                
                if (errorCode == DatabaseError.PERMISSION_DENIED) {
                    Log.e(TAG, "   ⚠️ PERMISSION DENIED - Security rules blocking read!");
                    Log.e(TAG, "   Solutions:");
                    Log.e(TAG, "   1. Enable Anonymous Authentication in Firebase Console");
                    Log.e(TAG, "   2. Update security rules to allow reads: \".read\": \"auth != null\"");
                    Log.e(TAG, "   3. Make sure rules are PUBLISHED after editing");
                } else if (errorCode == DatabaseError.NETWORK_ERROR) {
                    Log.e(TAG, "   ⚠️ NETWORK ERROR - Check device internet connection");
                    Log.e(TAG, "   Solutions:");
                    Log.e(TAG, "   1. Check WiFi/mobile data is enabled");
                    Log.e(TAG, "   2. Disable VPN/proxy if enabled");
                    Log.e(TAG, "   3. Try different network");
                } else if (errorMsg.contains("offline")) {
                    Log.e(TAG, "   ⚠️ OFFLINE ERROR - Database is offline");
                    Log.e(TAG, "   Solutions:");
                    Log.e(TAG, "   1. Check if goOnline() was called");
                    Log.e(TAG, "   2. Check network connectivity");
                    Log.e(TAG, "   3. Restart app");
                } else {
                    Log.e(TAG, "   ⚠️ UNKNOWN ERROR - Check full error details below");
                }
                
                if (callback != null) callback.onError(error.toException());
            }
        });
    }

    /* ─────────────────────────────────────────────
     * 🔹 LISTENERS (REAL-TIME UPDATES)
     * ───────────────────────────────────────────── */

    public ValueEventListener listenToPatients(FirebaseDataCallback callback) {
        return listenToPatients(callback, DEFAULT_LIMIT);
    }

    public ValueEventListener listenToPatients(FirebaseDataCallback callback, int limit) {
        return addRealtimeListener(PATH_PATIENTS, callback, limit, null);
    }
    
    public ValueEventListener listenToMedicines(FirebaseDataCallback callback) {
        return listenToMedicines(callback, DEFAULT_LIMIT);
    }

    public ValueEventListener listenToMedicines(FirebaseDataCallback callback, int limit) {
        return addRealtimeListener(PATH_MEDICINES, callback, limit, null);
    }
    
    public ValueEventListener listenToPrescriptions(FirebaseDataCallback callback) {
        return listenToPrescriptions(callback, DEFAULT_LIMIT);
    }

    public ValueEventListener listenToPrescriptions(FirebaseDataCallback callback, int limit) {
        return addRealtimeListener(PATH_PRESCRIPTIONS, callback, limit, null);
    }
    
    public ValueEventListener listenToEmployees(FirebaseDataCallback callback) {
        return listenToEmployees(callback, DEFAULT_LIMIT);
    }

    public ValueEventListener listenToEmployees(FirebaseDataCallback callback, int limit) {
        return addRealtimeListener(PATH_EMPLOYEES, callback, limit, null);
    }
    
    private ValueEventListener addRealtimeListener(String path, FirebaseDataCallback callback, int limit, String orderByChild) {
        // Validate inputs
        if (path == null || path.isEmpty()) {
            Log.e(TAG, "❌ Cannot add listener: path is null or empty");
            return null;
        }
        
        if (rootRef == null) {
            Log.e(TAG, "❌ Cannot add listener: Firebase rootRef is null");
            return null;
        }
        
        // Ensure database is online (pure online mode - no offline cache)
        // OPTIMIZED: Only check if cached state shows offline
        if (!FirebaseOnlineOnly.isOnline()) {
            try {
                FirebaseOnlineOnly.ensureOnline();
            } catch (Exception e) {
                Log.w(TAG, "Could not ensure database is online for listener", e);
            }
        }
        
        // Check authentication status
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth != null && auth.getCurrentUser() != null) {
                // User authenticated - no need to log
            } else {
                Log.w(TAG, "⚠️ No authenticated user - listeners may fail if security rules require auth");
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not check auth status for listener", e);
        }
        
        Query query = buildLimitedQuery(path, limit, orderByChild);
        if (query == null) {
            Log.w(TAG, "Cannot attach listener, query is null for path: " + path);
            return null;
        }
        
        // OPTIMIZED: Reduced verbose logging
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int count = 0;
                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            Map<String, Object> data = (Map<String, Object>) child.getValue();
                            if (data != null) {
                                if (callback != null) callback.onDataReceived(child.getKey(), data);
                                count++;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error processing listener data for key: " + child.getKey(), e);
                        }
                    }
                    // OPTIMIZED: Only log significant updates or issues
                    if (count == 0) {
                        Log.w(TAG, "⚠️ Listener: No items in path: " + path);
                    }
                } else {
                    Log.w(TAG, "⚠️ Listener: Path empty or doesn't exist: " + path);
                }
                if (callback != null) callback.onComplete();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "❌ Listener cancelled for path: " + path);
                Log.e(TAG, "   Error Code: " + error.getCode());
                Log.e(TAG, "   Error Message: " + error.getMessage());
                
                // Detailed error diagnosis
                int errorCode = error.getCode();
                if (errorCode == DatabaseError.PERMISSION_DENIED) {
                    Log.e(TAG, "   ⚠️ PERMISSION DENIED - Security rules blocking listener!");
                    Log.e(TAG, "   Enable Anonymous Authentication and update security rules");
                } else if (errorCode == DatabaseError.NETWORK_ERROR) {
                    Log.e(TAG, "   ⚠️ NETWORK ERROR - Check device internet connection");
                }
                
                if (callback != null) callback.onError(error.toException());
            }
        };

        query.addValueEventListener(listener);
        activeListeners.add(listener);
        listenerQueries.add(query);
        // OPTIMIZED: Reduced verbose logging
        return listener;
    }
    
    private Query buildLimitedQuery(String path, int limit, String orderByChild) {
        if (rootRef == null) {
            return null;
        }
        
        // Use FirebaseOnlineOnly for pure online reads (no caching)
        DatabaseReference ref;
        try {
            ref = FirebaseOnlineOnly.ref(path);
            if (ref == null) {
                // Fallback to direct reference
                ref = rootRef.child(path);
                ref.keepSynced(false); // Ensure no caching
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get FirebaseOnlineOnly ref for query, using fallback", e);
            ref = rootRef.child(path);
            ref.keepSynced(false); // Ensure no caching
        }
        Query query;
        if (orderByChild != null && !orderByChild.isEmpty()) {
            query = ref.orderByChild(orderByChild);
        } else {
            query = ref.orderByKey();
        }
        
        if (limit > 0) {
            query = query.limitToLast(limit);
        }
        
        try {
            query.keepSynced(false); // Prevent syncing entire node to disk
        } catch (Exception e) {
            Log.w(TAG, "Unable to adjust keepSynced for path: " + path, e);
        }
        return query;
    }

    /* ─────────────────────────────────────────────
     * 🔹 DELETE METHOD
     * ───────────────────────────────────────────── */

    public void deleteDocument(String path, String documentId, FirebaseOperationCallback callback) {
        DatabaseReference ref = rootRef.child(path).child(documentId);
        ref.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "🗑️ Deleted: " + path + "/" + documentId);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Delete failed for " + path + "/" + documentId, e);
                    if (callback != null) callback.onError(e);
                });
    }

    /* ─────────────────────────────────────────────
     * 🔹 CLEANUP
     * ───────────────────────────────────────────── */

    public void removeListener(ValueEventListener listener) {
        if (listener == null) return;
        
        int index = activeListeners.indexOf(listener);
        if (index >= 0 && index < listenerQueries.size()) {
            try {
                listenerQueries.get(index).removeEventListener(listener);
                activeListeners.remove(index);
                listenerQueries.remove(index);
                Log.d(TAG, "✅ Listener removed successfully");
            } catch (Exception e) {
                Log.w(TAG, "Error removing listener", e);
            }
        }
    }

    public void stopAllListeners() {
        for (int i = 0; i < activeListeners.size(); i++) {
            try {
                listenerQueries.get(i).removeEventListener(activeListeners.get(i));
            } catch (Exception e) {
                Log.w(TAG, "Error removing listener " + i, e);
            }
        }
        activeListeners.clear();
        listenerQueries.clear();
        Log.d(TAG, "🛑 All Firebase listeners stopped");
    }

    /* ─────────────────────────────────────────────
     * 🔹 CALLBACK INTERFACES
     * ───────────────────────────────────────────── */

    public interface FirebaseDataCallback {
        void onDataReceived(String documentId, Map<String, Object> data);
        void onComplete();
        void onError(Exception e);
    }

    public interface FirebaseOperationCallback {
        void onSuccess();
        void onError(Exception e);
    }
}
