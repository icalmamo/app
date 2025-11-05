package com.example.h_cas.database;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FirebaseHelper provides methods to sync SQLite data with Firebase Realtime Database
 * This allows for cloud backup and synchronization across devices
 */
public class FirebaseHelper {
    
    private static final String TAG = "FirebaseHelper";
    private FirebaseDatabase database;
    private DatabaseReference rootRef;
    private List<ValueEventListener> activeListeners = new ArrayList<>();
    private List<DatabaseReference> listenerReferences = new ArrayList<>(); // Track which ref each listener is attached to
    
    // Path names matching SQLite tables (Realtime Database uses paths, not collections)
    private static final String PATH_EMPLOYEES = "employees";
    private static final String PATH_PATIENTS = "patients";
    private static final String PATH_PRESCRIPTIONS = "prescriptions";
    private static final String PATH_MEDICINES = "medicines";
    private static final String PATH_CASES = "healthcare_cases";
    private static final String PATH_RFID_DATA = "rfid_data";
    
    public FirebaseHelper() {
        try {
            database = FirebaseDatabase.getInstance();
            if (database != null) {
                rootRef = database.getReference();
                // Enable offline persistence (optional but recommended)
                database.setPersistenceEnabled(true);
                Log.d(TAG, "Firebase Realtime Database initialized successfully");
            } else {
                Log.w(TAG, "FirebaseDatabase instance is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get FirebaseDatabase instance", e);
            database = null;
            rootRef = null;
        }
    }
    
    /**
     * Sync employee data to Firebase Realtime Database
     */
    public void syncEmployeeToFirebase(String employeeId, Map<String, Object> employeeData) {
        if (rootRef == null) {
            Log.w(TAG, "Realtime Database not available - skipping employee sync");
            return;
        }
        
        DatabaseReference employeeRef = rootRef.child(PATH_EMPLOYEES).child(employeeId);
        employeeRef.setValue(employeeData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Employee " + employeeId + " synced to Firebase Realtime Database");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Error syncing employee to Firebase", e);
            });
    }
    
    /**
     * Sync patient data to Firebase Realtime Database
     */
    public void syncPatientToFirebase(String patientId, Map<String, Object> patientData) {
        if (rootRef == null) {
            Log.w(TAG, "Realtime Database not available - skipping patient sync");
            return;
        }
        
        // Check if authenticated
        try {
            com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
            if (auth != null && auth.getCurrentUser() == null) {
                Log.w(TAG, "⚠️ Not authenticated - patient sync may fail. Please enable Anonymous Authentication in Firebase Console.");
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not check authentication status", e);
        }
        
        DatabaseReference patientRef = rootRef.child(PATH_PATIENTS).child(patientId);
        Log.d(TAG, "📤 Attempting to sync patient to path: patients/" + patientId);
        patientRef.setValue(patientData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Patient " + patientId + " synced to Firebase Realtime Database successfully!");
                Log.d(TAG, "   Patient Name: " + patientData.get("first_name") + " " + patientData.get("last_name"));
                Log.d(TAG, "   Full path: " + patientRef.toString());
                Log.d(TAG, "   Check Firebase Console → Realtime Database → patients → " + patientId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Error syncing patient to Firebase", e);
                Log.e(TAG, "   Error code: " + (e.getMessage() != null ? e.getMessage() : "Unknown"));
                Log.e(TAG, "   Patient ID: " + patientId);
                Log.e(TAG, "   Path: " + patientRef.toString());
                Log.e(TAG, "   Possible causes:");
                Log.e(TAG, "   1. Anonymous Authentication not enabled in Firebase Console");
                Log.e(TAG, "   2. Security rules blocking write access");
                Log.e(TAG, "   3. Network connectivity issues");
            });
    }
    
    /**
     * Sync prescription data to Firebase Realtime Database
     */
    public void syncPrescriptionToFirebase(String prescriptionId, Map<String, Object> prescriptionData) {
        if (rootRef == null) {
            Log.w(TAG, "Realtime Database not available - skipping prescription sync");
            return;
        }
        
        DatabaseReference prescriptionRef = rootRef.child(PATH_PRESCRIPTIONS).child(prescriptionId);
        prescriptionRef.setValue(prescriptionData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Prescription " + prescriptionId + " synced to Firebase Realtime Database successfully!");
                Log.d(TAG, "   Patient: " + prescriptionData.get("patient_name"));
                Log.d(TAG, "   Check Firebase Console → Realtime Database → prescriptions → " + prescriptionId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Error syncing prescription to Firebase", e);
            });
    }
    
    /**
     * Sync medicine data to Firebase Realtime Database
     */
    public void syncMedicineToFirebase(String medicineId, Map<String, Object> medicineData) {
        if (rootRef == null) {
            Log.w(TAG, "Realtime Database not available - skipping medicine sync");
            return;
        }
        
        DatabaseReference medicineRef = rootRef.child(PATH_MEDICINES).child(medicineId);
        medicineRef.setValue(medicineData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Medicine " + medicineId + " synced to Firebase Realtime Database successfully!");
                Log.d(TAG, "   Medicine Name: " + medicineData.get("medicine_name"));
                Log.d(TAG, "   Stock: " + medicineData.get("stock_quantity"));
                Log.d(TAG, "   Check Firebase Console → Realtime Database → medicines → " + medicineId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Error syncing medicine to Firebase", e);
                Log.e(TAG, "   Medicine ID: " + medicineId);
                Log.e(TAG, "   Error: " + e.getMessage());
            });
    }
    
    /**
     * Sync case data to Firebase Realtime Database
     */
    public void syncCaseToFirebase(String caseId, Map<String, Object> caseData) {
        if (rootRef == null) {
            Log.w(TAG, "Realtime Database not available - skipping case sync");
            return;
        }
        
        DatabaseReference caseRef = rootRef.child(PATH_CASES).child(caseId);
        caseRef.setValue(caseData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Case " + caseId + " synced to Firebase Realtime Database");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error syncing case to Firebase", e);
            });
    }
    
    /**
     * Sync RFID data to Firebase Realtime Database
     */
    public void syncRFIDDataToFirebase(String rfidTagId, Map<String, Object> rfidData) {
        if (rootRef == null) {
            Log.w(TAG, "Realtime Database not available - skipping RFID data sync");
            return;
        }
        
        DatabaseReference rfidRef = rootRef.child(PATH_RFID_DATA).child(rfidTagId);
        rfidRef.setValue(rfidData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "RFID Data " + rfidTagId + " synced to Firebase Realtime Database");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error syncing RFID data to Firebase", e);
            });
    }
    
    /**
     * Get all employees from Firebase Realtime Database
     */
    public void getAllEmployeesFromFirebase(FirebaseDataCallback callback) {
        if (rootRef == null) {
            Log.w(TAG, "Realtime Database not available");
            if (callback != null) {
                callback.onError(new IllegalStateException("Realtime Database not available"));
            }
            return;
        }
        
        DatabaseReference employeesRef = rootRef.child(PATH_EMPLOYEES);
        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Map<String, Object> data = (Map<String, Object>) child.getValue();
                        if (callback != null) {
                            callback.onDataReceived(child.getKey(), data);
                        }
                    }
                }
                if (callback != null) {
                    callback.onComplete();
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error getting employees from Firebase", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }
    
    /**
     * Get all patients from Firebase Realtime Database
     */
    public void getAllPatientsFromFirebase(FirebaseDataCallback callback) {
        if (rootRef == null) {
            Log.w(TAG, "Realtime Database not available");
            if (callback != null) {
                callback.onError(new IllegalStateException("Realtime Database not available"));
            }
            return;
        }
        
        DatabaseReference patientsRef = rootRef.child(PATH_PATIENTS);
        patientsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Map<String, Object> data = (Map<String, Object>) child.getValue();
                        if (callback != null) {
                            callback.onDataReceived(child.getKey(), data);
                        }
                    }
                }
                if (callback != null) {
                    callback.onComplete();
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error getting patients from Firebase", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }
    
    /**
     * Delete data from Firebase Realtime Database
     */
    public void deleteDocument(String path, String documentId, FirebaseOperationCallback callback) {
        if (rootRef == null) {
            Log.w(TAG, "Realtime Database not available");
            if (callback != null) {
                callback.onError(new IllegalStateException("Realtime Database not available"));
            }
            return;
        }
        
        DatabaseReference ref = rootRef.child(path).child(documentId);
        ref.removeValue()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Document " + documentId + " deleted from Firebase Realtime Database");
                if (callback != null) {
                    callback.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting document from Firebase", e);
                if (callback != null) {
                    callback.onError(e);
                }
            });
    }
    
    /**
     * Check if Firebase Realtime Database is available
     */
    public boolean isFirebaseAvailable() {
        try {
            return database != null && rootRef != null && FirebaseDatabase.getInstance() != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Listen for real-time updates on medicines
     */
    public ValueEventListener listenToMedicines(FirebaseDataCallback callback) {
        if (rootRef == null) {
            Log.w(TAG, "Realtime Database not available - cannot listen to medicines");
            if (callback != null) {
                callback.onError(new IllegalStateException("Realtime Database not available"));
            }
            return null;
        }
        
        DatabaseReference medicinesRef = rootRef.child(PATH_MEDICINES);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Map<String, Object> data = (Map<String, Object>) child.getValue();
                        if (callback != null) {
                            callback.onDataReceived(child.getKey(), data);
                        }
                    }
                    if (callback != null) {
                        callback.onComplete();
                    }
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error listening to medicines", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        };
        
        medicinesRef.addValueEventListener(listener);
        activeListeners.add(listener);
        listenerReferences.add(medicinesRef);
        return listener;
    }
    
    /**
     * Listen for real-time updates on prescriptions
     */
    public ValueEventListener listenToPrescriptions(FirebaseDataCallback callback) {
        if (rootRef == null) {
            Log.w(TAG, "Realtime Database not available - cannot listen to prescriptions");
            if (callback != null) {
                callback.onError(new IllegalStateException("Realtime Database not available"));
            }
            return null;
        }
        
        DatabaseReference prescriptionsRef = rootRef.child(PATH_PRESCRIPTIONS);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Map<String, Object> data = (Map<String, Object>) child.getValue();
                        if (callback != null) {
                            callback.onDataReceived(child.getKey(), data);
                        }
                    }
                    if (callback != null) {
                        callback.onComplete();
                    }
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error listening to prescriptions", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        };
        
        prescriptionsRef.addValueEventListener(listener);
        activeListeners.add(listener);
        listenerReferences.add(prescriptionsRef);
        return listener;
    }
    
    /**
     * Listen for real-time updates on patients
     */
    public ValueEventListener listenToPatients(FirebaseDataCallback callback) {
        if (rootRef == null) {
            Log.w(TAG, "Realtime Database not available - cannot listen to patients");
            if (callback != null) {
                callback.onError(new IllegalStateException("Realtime Database not available"));
            }
            return null;
        }
        
        DatabaseReference patientsRef = rootRef.child(PATH_PATIENTS);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Map<String, Object> data = (Map<String, Object>) child.getValue();
                        if (callback != null) {
                            callback.onDataReceived(child.getKey(), data);
                        }
                    }
                    if (callback != null) {
                        callback.onComplete();
                    }
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error listening to patients", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        };
        
        patientsRef.addValueEventListener(listener);
        activeListeners.add(listener);
        listenerReferences.add(patientsRef);
        return listener;
    }
    
    /**
     * Stop all active listeners
     */
    public void stopAllListeners() {
        try {
            if (activeListeners == null || activeListeners.isEmpty()) {
                return;
            }
            
            // Remove each listener from its corresponding reference
            for (int i = 0; i < activeListeners.size() && i < listenerReferences.size(); i++) {
                try {
                    ValueEventListener listener = activeListeners.get(i);
                    DatabaseReference ref = listenerReferences.get(i);
                    if (listener != null && ref != null) {
                        ref.removeEventListener(listener);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error removing listener", e);
                }
            }
            
            activeListeners.clear();
            listenerReferences.clear();
            Log.d(TAG, "All Firebase Realtime Database listeners stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping listeners", e);
        }
    }
    
    /**
     * Callback interface for Firebase data operations
     */
    public interface FirebaseDataCallback {
        void onDataReceived(String documentId, Map<String, Object> data);
        void onComplete();
        void onError(Exception e);
    }
    
    /**
     * Callback interface for Firebase operations
     */
    public interface FirebaseOperationCallback {
        void onSuccess();
        void onError(Exception e);
    }
}
