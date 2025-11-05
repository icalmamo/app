# Firebase Firestore Sync - Step-by-Step Guide

## Overview
This guide will help you implement real-time data synchronization across all 4 devices using Firebase Firestore.

## Prerequisites
- ✅ Firebase project already set up
- ✅ `google-services.json` file exists
- ✅ Firebase dependencies already in `build.gradle`
- ✅ Internet connection required

---

## Step 1: Enable Firestore Database in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click **"Firestore Database"** in the left menu
4. If not created yet, click **"Create database"**
5. Choose **"Start in test mode"** (for development)
6. Select a location closest to your region
7. Click **"Enable"**

---

## Step 2: Update Firestore Security Rules (For Production)

**Current Test Mode Rules** (allow all read/write):
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

**Production Rules** (recommended after testing):
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Employees collection
    match /employees/{employeeId} {
      allow read, write: if request.auth != null;
    }
    // Patients collection
    match /patients/{patientId} {
      allow read, write: if request.auth != null;
    }
    // Prescriptions collection
    match /prescriptions/{prescriptionId} {
      allow read, write: if request.auth != null;
    }
    // Medicines collection
    match /medicines/{medicineId} {
      allow read, write: if request.auth != null;
    }
    // Cases collection
    match /healthcare_cases/{caseId} {
      allow read, write: if request.auth != null;
    }
    // RFID Data collection
    match /rfid_data/{rfidTagId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

**To update rules:**
1. Go to Firestore Database → Rules tab
2. Paste the rules above
3. Click **"Publish"**

---

## Step 3: Update FirebaseHelper with Real-Time Listeners

The existing `FirebaseHelper.java` already has basic sync methods. We need to add real-time listeners.

### Add Real-Time Listener Methods to FirebaseHelper

Add these methods to `src/main/java/com/example/h_cas/database/FirebaseHelper.java`:

```java
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

// Add these fields at the top of the class
private List<ListenerRegistration> activeListeners = new ArrayList<>();

/**
 * Listen for real-time updates on medicines collection
 */
public ListenerRegistration listenToMedicines(FirebaseDataCallback callback) {
    ListenerRegistration registration = db.collection(COLLECTION_MEDICINES)
        .addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error listening to medicines", error);
                callback.onError(error);
                return;
            }
            
            if (snapshot != null) {
                for (QueryDocumentSnapshot document : snapshot) {
                    Map<String, Object> data = document.getData();
                    callback.onDataReceived(document.getId(), data);
                }
                callback.onComplete();
            }
        });
    
    activeListeners.add(registration);
    return registration;
}

/**
 * Listen for real-time updates on prescriptions collection
 */
public ListenerRegistration listenToPrescriptions(FirebaseDataCallback callback) {
    ListenerRegistration registration = db.collection(COLLECTION_PRESCRIPTIONS)
        .addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error listening to prescriptions", error);
                callback.onError(error);
                return;
            }
            
            if (snapshot != null) {
                for (QueryDocumentSnapshot document : snapshot) {
                    Map<String, Object> data = document.getData();
                    callback.onDataReceived(document.getId(), data);
                }
                callback.onComplete();
            }
        });
    
    activeListeners.add(registration);
    return registration;
}

/**
 * Listen for real-time updates on patients collection
 */
public ListenerRegistration listenToPatients(FirebaseDataCallback callback) {
    ListenerRegistration registration = db.collection(COLLECTION_PATIENTS)
        .addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error listening to patients", error);
                callback.onError(error);
                return;
            }
            
            if (snapshot != null) {
                for (QueryDocumentSnapshot document : snapshot) {
                    Map<String, Object> data = document.getData();
                    callback.onDataReceived(document.getId(), data);
                }
                callback.onComplete();
            }
        });
    
    activeListeners.add(registration);
    return registration;
}

/**
 * Stop all active listeners
 */
public void stopAllListeners() {
    for (ListenerRegistration registration : activeListeners) {
        registration.remove();
    }
    activeListeners.clear();
}
```

---

## Step 4: Create Sync Manager Class

Create a new file: `src/main/java/com/example/h_cas/database/FirebaseSyncManager.java`

This class will handle syncing between SQLite and Firestore.

```java
package com.example.h_cas.database;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.h_cas.models.Medicine;
import com.example.h_cas.models.Patient;
import com.example.h_cas.models.Prescription;
import com.example.h_cas.models.Employee;

import java.util.HashMap;
import java.util.Map;

/**
 * FirebaseSyncManager handles bidirectional sync between SQLite and Firestore
 */
public class FirebaseSyncManager {
    
    private static final String TAG = "FirebaseSyncManager";
    private FirebaseHelper firebaseHelper;
    private HCasDatabaseHelper databaseHelper;
    private Context context;
    private boolean isSyncing = false;
    
    public FirebaseSyncManager(Context context) {
        this.context = context;
        this.firebaseHelper = new FirebaseHelper();
        this.databaseHelper = new HCasDatabaseHelper(context);
    }
    
    /**
     * Sync medicine to Firestore
     */
    public void syncMedicine(Medicine medicine) {
        if (medicine == null) return;
        
        Map<String, Object> medicineData = new HashMap<>();
        medicineData.put("medicine_id", medicine.getMedicineId());
        medicineData.put("medicine_name", medicine.getMedicineName());
        medicineData.put("dosage", medicine.getDosage());
        medicineData.put("stock_quantity", medicine.getStockQuantity());
        medicineData.put("unit", medicine.getUnit());
        medicineData.put("category", medicine.getCategory());
        medicineData.put("description", medicine.getDescription());
        medicineData.put("expiry_date", medicine.getExpiryDate());
        medicineData.put("price", medicine.getPrice());
        medicineData.put("supplier", medicine.getSupplier());
        medicineData.put("last_updated", System.currentTimeMillis());
        
        firebaseHelper.syncMedicineToFirebase(medicine.getMedicineId(), medicineData);
    }
    
    /**
     * Sync prescription to Firestore
     */
    public void syncPrescription(Prescription prescription) {
        if (prescription == null) return;
        
        Map<String, Object> prescriptionData = new HashMap<>();
        prescriptionData.put("prescription_id", prescription.getPrescriptionId());
        prescriptionData.put("patient_id", prescription.getPatientId());
        prescriptionData.put("patient_name", prescription.getPatientName());
        prescriptionData.put("medication", prescription.getMedication());
        prescriptionData.put("dosage", prescription.getDosage());
        prescriptionData.put("frequency", prescription.getFrequency());
        prescriptionData.put("duration", prescription.getDuration());
        prescriptionData.put("instructions", prescription.getInstructions());
        prescriptionData.put("doctor_id", prescription.getDoctorId());
        prescriptionData.put("doctor_name", prescription.getDoctorName());
        prescriptionData.put("created_date", prescription.getCreatedDate());
        prescriptionData.put("status", prescription.getStatus());
        prescriptionData.put("last_updated", System.currentTimeMillis());
        
        firebaseHelper.syncPrescriptionToFirebase(prescription.getPrescriptionId(), prescriptionData);
    }
    
    /**
     * Sync patient to Firestore
     */
    public void syncPatient(Patient patient) {
        if (patient == null) return;
        
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patient_id", patient.getPatientId());
        patientData.put("first_name", patient.getFirstName());
        patientData.put("last_name", patient.getLastName());
        patientData.put("date_of_birth", patient.getDateOfBirth());
        patientData.put("gender", patient.getGender());
        patientData.put("phone", patient.getPhone());
        patientData.put("email", patient.getEmail());
        patientData.put("address", patient.getAddress());
        patientData.put("last_updated", System.currentTimeMillis());
        
        firebaseHelper.syncPatientToFirebase(patient.getPatientId(), patientData);
    }
    
    /**
     * Sync employee to Firestore
     */
    public void syncEmployee(Employee employee) {
        if (employee == null) return;
        
        Map<String, Object> employeeData = new HashMap<>();
        employeeData.put("employee_id", employee.getEmployeeId());
        employeeData.put("first_name", employee.getFirstName());
        employeeData.put("last_name", employee.getLastName());
        employeeData.put("email", employee.getEmail());
        employeeData.put("phone", employee.getPhone());
        employeeData.put("role", employee.getRole());
        employeeData.put("username", employee.getUsername());
        employeeData.put("created_date", employee.getCreatedDate());
        employeeData.put("is_active", employee.isActive());
        employeeData.put("profile_picture_url", employee.getProfilePictureUrl());
        employeeData.put("last_updated", System.currentTimeMillis());
        
        firebaseHelper.syncEmployeeToFirebase(employee.getEmployeeId(), employeeData);
    }
    
    /**
     * Start listening to Firestore for real-time updates
     */
    public void startListeningToUpdates() {
        if (isSyncing) return;
        isSyncing = true;
        
        // Listen to medicines
        firebaseHelper.listenToMedicines(new FirebaseHelper.FirebaseDataCallback() {
            @Override
            public void onDataReceived(String documentId, Map<String, Object> data) {
                syncMedicineFromFirestore(data);
            }
            
            @Override
            public void onComplete() {
                Log.d(TAG, "Medicines sync complete");
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error syncing medicines", e);
            }
        });
        
        // Listen to prescriptions
        firebaseHelper.listenToPrescriptions(new FirebaseHelper.FirebaseDataCallback() {
            @Override
            public void onDataReceived(String documentId, Map<String, Object> data) {
                syncPrescriptionFromFirestore(data);
            }
            
            @Override
            public void onComplete() {
                Log.d(TAG, "Prescriptions sync complete");
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error syncing prescriptions", e);
            }
        });
        
        // Listen to patients
        firebaseHelper.listenToPatients(new FirebaseHelper.FirebaseDataCallback() {
            @Override
            public void onDataReceived(String documentId, Map<String, Object> data) {
                syncPatientFromFirestore(data);
            }
            
            @Override
            public void onComplete() {
                Log.d(TAG, "Patients sync complete");
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error syncing patients", e);
            }
        });
    }
    
    /**
     * Stop listening to Firestore updates
     */
    public void stopListening() {
        firebaseHelper.stopAllListeners();
        isSyncing = false;
    }
    
    /**
     * Sync medicine from Firestore to SQLite
     */
    private void syncMedicineFromFirestore(Map<String, Object> data) {
        try {
            Medicine medicine = new Medicine();
            medicine.setMedicineId((String) data.get("medicine_id"));
            medicine.setMedicineName((String) data.get("medicine_name"));
            medicine.setDosage((String) data.get("dosage"));
            
            Object stockQty = data.get("stock_quantity");
            if (stockQty instanceof Long) {
                medicine.setStockQuantity(((Long) stockQty).intValue());
            } else if (stockQty instanceof Integer) {
                medicine.setStockQuantity((Integer) stockQty);
            }
            
            medicine.setUnit((String) data.get("unit"));
            medicine.setCategory((String) data.get("category"));
            medicine.setDescription((String) data.get("description"));
            medicine.setExpiryDate((String) data.get("expiry_date"));
            
            Object price = data.get("price");
            if (price instanceof Double) {
                medicine.setPrice((Double) price);
            } else if (price instanceof Long) {
                medicine.setPrice(((Long) price).doubleValue());
            }
            
            medicine.setSupplier((String) data.get("supplier"));
            
            // Check if medicine exists in SQLite
            Medicine existingMedicine = databaseHelper.getMedicineById(medicine.getMedicineId());
            if (existingMedicine == null) {
                // Add new medicine
                databaseHelper.addMedicine(medicine);
            } else {
                // Update existing medicine
                databaseHelper.updateMedicine(medicine);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing medicine from Firestore", e);
        }
    }
    
    /**
     * Sync prescription from Firestore to SQLite
     */
    private void syncPrescriptionFromFirestore(Map<String, Object> data) {
        try {
            Prescription prescription = new Prescription();
            prescription.setPrescriptionId((String) data.get("prescription_id"));
            prescription.setPatientId((String) data.get("patient_id"));
            prescription.setPatientName((String) data.get("patient_name"));
            prescription.setMedication((String) data.get("medication"));
            prescription.setDosage((String) data.get("dosage"));
            prescription.setFrequency((String) data.get("frequency"));
            prescription.setDuration((String) data.get("duration"));
            prescription.setInstructions((String) data.get("instructions"));
            prescription.setDoctorId((String) data.get("doctor_id"));
            prescription.setDoctorName((String) data.get("doctor_name"));
            prescription.setCreatedDate((String) data.get("created_date"));
            prescription.setStatus((String) data.get("status"));
            
            // Check if prescription exists
            Prescription existing = databaseHelper.getPrescriptionById(prescription.getPrescriptionId());
            if (existing == null) {
                databaseHelper.addPrescription(prescription);
            } else {
                databaseHelper.updatePrescription(prescription);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing prescription from Firestore", e);
        }
    }
    
    /**
     * Sync patient from Firestore to SQLite
     */
    private void syncPatientFromFirestore(Map<String, Object> data) {
        try {
            Patient patient = new Patient();
            patient.setPatientId((String) data.get("patient_id"));
            patient.setFirstName((String) data.get("first_name"));
            patient.setLastName((String) data.get("last_name"));
            patient.setDateOfBirth((String) data.get("date_of_birth"));
            patient.setGender((String) data.get("gender"));
            patient.setPhone((String) data.get("phone"));
            patient.setEmail((String) data.get("email"));
            patient.setAddress((String) data.get("address"));
            
            // Check if patient exists
            Patient existing = databaseHelper.getPatientById(patient.getPatientId());
            if (existing == null) {
                databaseHelper.addPatient(patient);
            } else {
                databaseHelper.updatePatient(patient);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing patient from Firestore", e);
        }
    }
}
```

---

## Step 5: Update HCasDatabaseHelper to Auto-Sync

Add sync calls to database operations. For example, in `addMedicine()` method:

```java
// In HCasDatabaseHelper.java, update addMedicine method:

public boolean addMedicine(com.example.h_cas.models.Medicine medicine) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = new ContentValues();
    
    // ... existing code ...
    
    long result = db.insert(TABLE_MEDICINES, null, values);
    db.close();
    
    // Sync to Firebase if successful
    if (result != -1) {
        syncToFirebase(medicine);
    }
    
    return result != -1;
}

// Add this helper method
private void syncToFirebase(com.example.h_cas.models.Medicine medicine) {
    try {
        FirebaseSyncManager syncManager = new FirebaseSyncManager(context);
        syncManager.syncMedicine(medicine);
    } catch (Exception e) {
        // Log error but don't fail the operation
        android.util.Log.e("HCasDatabaseHelper", "Firebase sync failed", e);
    }
}
```

---

## Step 6: Initialize Sync in Application Class

Update `src/main/java/com/example/h_cas/HCasApplication.java`:

```java
package com.example.h_cas;

import android.app.Application;
import com.example.h_cas.database.FirebaseSyncManager;

public class HCasApplication extends Application {
    
    private FirebaseSyncManager syncManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase Sync Manager
        syncManager = new FirebaseSyncManager(this);
        
        // Start listening to real-time updates
        syncManager.startListeningToUpdates();
    }
    
    public FirebaseSyncManager getSyncManager() {
        return syncManager;
    }
}
```

---

## Step 7: Test the Sync

1. **Build and run the app on all 4 devices**
2. **Device 1 (Admin)**: Add a new medicine
   - Should appear on all other devices within seconds
3. **Device 2 (Pharmacist)**: Create a prescription
   - Should appear on all other devices
4. **Device 3 (Doctor)**: Add a patient
   - Should appear on all other devices
5. **Device 4 (Nurse)**: Update patient info
   - Should update on all other devices

---

## Step 8: Add Sync Status Indicator (Optional)

Add a sync indicator in your UI to show sync status:

```java
// In your activity/fragment
private void checkSyncStatus() {
    FirebaseSyncManager syncManager = ((HCasApplication) getApplication()).getSyncManager();
    // Show sync status in UI
}
```

---

## Troubleshooting

### Issue: Data not syncing
- Check internet connection
- Verify `google-services.json` is in correct location
- Check Firebase Console for errors
- Verify Firestore rules allow read/write

### Issue: App crashes on sync
- Check Firebase initialization
- Verify all dependencies are added
- Check logcat for specific errors

### Issue: Duplicate data
- Add conflict resolution logic
- Use timestamps to determine latest update

---

## Next Steps

1. ✅ Complete Step 1-8 above
2. ✅ Test on all 4 devices
3. ✅ Update Firestore security rules for production
4. ✅ Add error handling and retry logic
5. ✅ Implement offline support (Firestore has built-in offline persistence)

---

## Summary

After completing these steps:
- ✅ All 4 devices will be connected via Firestore
- ✅ Real-time data sync across devices
- ✅ Changes on one device appear on all devices
- ✅ Offline support (with Firestore persistence)

**Estimated Time**: 2-3 hours for full implementation

**Difficulty**: Intermediate

Good luck! 🚀






