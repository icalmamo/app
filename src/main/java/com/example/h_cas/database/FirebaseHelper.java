package com.example.h_cas.database;

import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
    private final List<DatabaseReference> listenerReferences = new ArrayList<>();

    // Define your table/collection paths
    private static final String PATH_EMPLOYEES = "employees";
    private static final String PATH_PATIENTS = "patients";
    private static final String PATH_PRESCRIPTIONS = "prescriptions";
    private static final String PATH_MEDICINES = "medicines";
    private static final String PATH_CASES = "healthcare_cases";
    private static final String PATH_RFID_DATA = "rfid_data";

    /**
     * Constructor: initializes Firebase connection
     */
    public FirebaseHelper() {
        database = FirebaseDatabase.getInstance("https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/");
        
        // Enable persistence only if not already enabled (must be called before first usage)
        try {
            database.setPersistenceEnabled(true); // Enable offline mode
            Log.d(TAG, "✅ Firebase persistence enabled");
        } catch (Exception e) {
            // Persistence already set or database already used - this is OK
            Log.d(TAG, "ℹ️ Persistence already configured or database already initialized");
        }
        
        rootRef = database.getReference();
        Log.d(TAG, "✅ Firebase initialized successfully");
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

        // Check authentication status before writing
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth != null && auth.getCurrentUser() != null) {
                Log.d(TAG, "✅ Authenticated user: " + auth.getCurrentUser().getUid());
            } else {
                Log.w(TAG, "⚠️ No authenticated user - writes may fail if security rules require auth");
                Log.w(TAG, "   Enable Anonymous Authentication in Firebase Console:");
                Log.w(TAG, "   Authentication → Sign-in method → Anonymous → Enable");
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not check auth status", e);
        }

        DatabaseReference ref = rootRef.child(path).child(id);
        Log.d(TAG, "📤 Attempting to write to: " + path + "/" + id);
        ref.setValue(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Data synced successfully to path: " + path + "/" + id);
                    Log.d(TAG, "   Check Firebase Console → Realtime Database → " + path + " → " + id);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to sync to Firebase path: " + path + "/" + id);
                    Log.e(TAG, "   Error: " + e.getMessage());
                    Log.e(TAG, "   Error Code: " + (e.getClass().getSimpleName()));
                    
                    // Check if it's a permission error
                    String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                    if (errorMsg.contains("permission") || errorMsg.contains("denied")) {
                        Log.e(TAG, "   ⚠️ PERMISSION DENIED - This is a security rules issue!");
                        Log.e(TAG, "   Solutions:");
                        Log.e(TAG, "   1. Enable Anonymous Authentication in Firebase Console");
                        Log.e(TAG, "   2. Or update security rules to allow writes (for testing)");
                        Log.e(TAG, "   3. See FIREBASE_REALTIME_DATABASE_RULES.md for details");
                    }
                    e.printStackTrace();
                });
    }

    /* ─────────────────────────────────────────────
     * 🔹 READ METHODS
     * ───────────────────────────────────────────── */

    public void getAllPatientsFromFirebase(FirebaseDataCallback callback) {
        readAllFromFirebase(PATH_PATIENTS, callback);
    }

    public void getAllEmployeesFromFirebase(FirebaseDataCallback callback) {
        readAllFromFirebase(PATH_EMPLOYEES, callback);
    }

    public void getAllMedicinesFromFirebase(FirebaseDataCallback callback) {
        readAllFromFirebase(PATH_MEDICINES, callback);
    }

    private void readAllFromFirebase(String path, FirebaseDataCallback callback) {
        DatabaseReference ref = rootRef.child(path);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Map<String, Object> data = (Map<String, Object>) child.getValue();
                        if (callback != null) callback.onDataReceived(child.getKey(), data);
                    }
                }
                if (callback != null) callback.onComplete();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "❌ Read failed for path: " + path, error.toException());
                if (callback != null) callback.onError(error.toException());
            }
        });
    }

    /* ─────────────────────────────────────────────
     * 🔹 LISTENERS (REAL-TIME UPDATES)
     * ───────────────────────────────────────────── */

    public ValueEventListener listenToPatients(FirebaseDataCallback callback) {
        return addRealtimeListener(PATH_PATIENTS, callback);
    }

    public ValueEventListener listenToMedicines(FirebaseDataCallback callback) {
        return addRealtimeListener(PATH_MEDICINES, callback);
    }

    public ValueEventListener listenToPrescriptions(FirebaseDataCallback callback) {
        return addRealtimeListener(PATH_PRESCRIPTIONS, callback);
    }

    private ValueEventListener addRealtimeListener(String path, FirebaseDataCallback callback) {
        DatabaseReference ref = rootRef.child(path);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> data = (Map<String, Object>) child.getValue();
                    if (callback != null) callback.onDataReceived(child.getKey(), data);
                }
                if (callback != null) callback.onComplete();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (callback != null) callback.onError(error.toException());
            }
        };

        ref.addValueEventListener(listener);
        activeListeners.add(listener);
        listenerReferences.add(ref);
        return listener;
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

    public void stopAllListeners() {
        for (int i = 0; i < activeListeners.size(); i++) {
            try {
                listenerReferences.get(i).removeEventListener(activeListeners.get(i));
            } catch (Exception e) {
                Log.w(TAG, "Error removing listener " + i, e);
            }
        }
        activeListeners.clear();
        listenerReferences.clear();
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
