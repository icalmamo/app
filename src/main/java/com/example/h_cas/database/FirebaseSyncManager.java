package com.example.h_cas.database;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.h_cas.models.Medicine;
import com.example.h_cas.models.Patient;
import com.example.h_cas.models.Prescription;
import com.example.h_cas.models.Employee;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

/**
 * FirebaseSyncManager handles bidirectional sync between SQLite and Firebase Realtime Database
 */
public class FirebaseSyncManager {
    
    private static final String TAG = "FirebaseSyncManager";
    private FirebaseHelper firebaseHelper;
    private HCasDatabaseHelper databaseHelper;
    private Context context;
    private boolean isSyncing = false;
    private static final int SYNC_LIST_LIMIT = 100;
    
    public FirebaseSyncManager(Context context) {
        if (context == null) {
            Log.e(TAG, "Context cannot be null");
            this.firebaseHelper = null;
            this.databaseHelper = null;
            return; // Don't throw - app should continue without sync
        }
        
        this.context = context;
        
        // Always initialize database helper first (required for app functionality)
        try {
            this.databaseHelper = new HCasDatabaseHelper(context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize HCasDatabaseHelper", e);
            this.databaseHelper = null;
            // Still try to continue
        }
        
        // Try to initialize Firebase (optional)
        try {
            // Check if Firebase is available first
            try {
                com.google.firebase.FirebaseApp.getInstance();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Firebase not initialized - sync will be disabled", e);
                this.firebaseHelper = null;
                return; // Continue without Firebase sync
            }
            
            // Initialize FirebaseHelper (will initialize Firestore)
            this.firebaseHelper = new FirebaseHelper();
            if (this.firebaseHelper != null && this.firebaseHelper.isFirebaseAvailable()) {
                // FirebaseHelper initialized successfully
            } else {
                Log.w(TAG, "FirebaseHelper initialized but Firebase not available");
                this.firebaseHelper = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize FirebaseHelper - sync disabled", e);
            this.firebaseHelper = null;
            // Don't throw - app should continue without sync
        }
    }
    
    /**
     * Sync medicine to Firebase Realtime Database
     * OPTIMIZED: Runs on background thread to avoid blocking UI
     * Only updates last_updated if data actually changed to prevent spam
     */
    public void syncMedicine(Medicine medicine) {
        if (medicine == null) return;
        if (firebaseHelper == null) {
            Log.w(TAG, "FirebaseHelper not available - skipping medicine sync");
            return;
        }
        
        // Run on background thread to avoid blocking UI
        com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
            try {
                // Check if medicine exists in Firebase and compare data
                DatabaseReference medicineRef = firebaseHelper.getReference("medicines").child(medicine.getMedicineId());
                if (medicineRef == null) {
                    Log.w(TAG, "⚠️ Cannot get Firebase reference for medicine: " + medicine.getMedicineId());
                    return;
                }
                
                medicineRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
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
                        
                        boolean dataChanged = false;
                        boolean medicineExists = snapshot.exists();
                        
                        if (!medicineExists) {
                            // New medicine - always update
                            dataChanged = true;
                        } else {
                            // Medicine exists - check if data changed
                            Map<String, Object> existingData = (Map<String, Object>) snapshot.getValue();
                            if (existingData != null) {
                                // Compare data to detect actual changes (exclude last_updated from comparison)
                                for (Map.Entry<String, Object> entry : medicineData.entrySet()) {
                                    String key = entry.getKey();
                                    if (!key.equals("last_updated")) {
                                        Object newValue = entry.getValue();
                                        Object oldValue = existingData.get(key);
                                        
                                        // Compare values (handle null cases and type conversions)
                                        if (newValue == null && oldValue != null) {
                                            dataChanged = true;
                                            break;
                                        } else if (newValue != null) {
                                            // Handle number type conversions (Long vs Integer vs Double)
                                            if (newValue instanceof Number && oldValue instanceof Number) {
                                                if (newValue instanceof Long && oldValue instanceof Long) {
                                                    if (!newValue.equals(oldValue)) {
                                                        dataChanged = true;
                                                        break;
                                                    }
                                                } else if (newValue instanceof Integer && oldValue instanceof Integer) {
                                                    if (!newValue.equals(oldValue)) {
                                                        dataChanged = true;
                                                        break;
                                                    }
                                                } else {
                                                    // Convert to double for comparison
                                                    double newDouble = ((Number) newValue).doubleValue();
                                                    double oldDouble = ((Number) oldValue).doubleValue();
                                                    if (Math.abs(newDouble - oldDouble) > 0.001) {
                                                        dataChanged = true;
                                                        break;
                                                    }
                                                }
                                            } else if (!newValue.equals(oldValue)) {
                                                dataChanged = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            } else {
                                // No existing data - treat as new
                                dataChanged = true;
                            }
                        }
                        
                        // Only update last_updated if data actually changed
                        if (dataChanged) {
                            medicineData.put("last_updated", System.currentTimeMillis());
                            firebaseHelper.syncMedicineToFirebase(medicine.getMedicineId(), medicineData);
                            Log.d(TAG, "✅ Medicine data synced (changed): " + medicine.getMedicineId());
                        } else {
                            Log.d(TAG, "⏭️ Medicine data unchanged - skipping update: " + medicine.getMedicineId());
                        }
                    }
                    
                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                        Log.e(TAG, "Error checking medicine existence: " + medicine.getMedicineId(), error.toException());
                        // Fallback: update anyway on error (but log it)
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
                });
            } catch (Exception e) {
                Log.e(TAG, "Error syncing medicine", e);
            }
        });
    }
    
    /**
     * Sync prescription to Firebase Realtime Database
     * OPTIMIZED: Runs on background thread to avoid blocking UI
     */
    public void syncPrescription(Prescription prescription) {
        if (prescription == null) return;
        if (firebaseHelper == null) {
            Log.w(TAG, "FirebaseHelper not available - skipping prescription sync");
            return;
        }
        
        // Run on background thread to avoid blocking UI
        com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
            try {
                Map<String, Object> prescriptionData = new HashMap<>();
                prescriptionData.put("prescription_id", prescription.getPrescriptionId());
                prescriptionData.put("patient_id", prescription.getPatientId());
                prescriptionData.put("patient_name", prescription.getPatientName());
                prescriptionData.put("medication", prescription.getMedication());
                prescriptionData.put("frequency", prescription.getFrequency());
                prescriptionData.put("duration", prescription.getDuration());
                prescriptionData.put("instructions", prescription.getInstructions());
                prescriptionData.put("doctor_id", prescription.getDoctorId());
                prescriptionData.put("doctor_name", prescription.getDoctorName());
                prescriptionData.put("created_date", prescription.getCreatedDate());
                prescriptionData.put("status", prescription.getStatus());
                prescriptionData.put("last_updated", System.currentTimeMillis());
                
                firebaseHelper.syncPrescriptionToFirebase(prescription.getPrescriptionId(), prescriptionData);
            } catch (Exception e) {
                Log.e(TAG, "Error syncing prescription", e);
            }
        });
    }
    
    /**
     * Sync patient to Firebase Realtime Database
     * OPTIMIZED: Runs on background thread to avoid blocking UI
     */
    public void syncPatient(Patient patient) {
        if (patient == null) {
            Log.e(TAG, "❌ Cannot sync patient: patient is null");
            return;
        }
        
        if (firebaseHelper == null) {
            Log.w(TAG, "FirebaseHelper not available - skipping patient sync");
            return;
        }
        
        // Run on background thread to avoid blocking UI
        com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
            try {
                Map<String, Object> patientData = new HashMap<>();
                patientData.put("patient_id", patient.getPatientId() != null ? patient.getPatientId() : "");
                patientData.put("first_name", patient.getFirstName() != null ? patient.getFirstName() : "");
                patientData.put("last_name", patient.getLastName() != null ? patient.getLastName() : "");
                patientData.put("date_of_birth", patient.getDateOfBirth() != null ? patient.getDateOfBirth() : "");
                patientData.put("gender", patient.getGender() != null ? patient.getGender() : "");
                
                // Use getPhone() which returns phoneNumber (legacy support)
                String phone = patient.getPhone() != null ? patient.getPhone() : 
                              (patient.getPhoneNumber() != null ? patient.getPhoneNumber() : "");
                patientData.put("phone", phone);
                
                patientData.put("email", patient.getEmail() != null ? patient.getEmail() : "");
                patientData.put("address", patient.getAddress() != null ? patient.getAddress() : "");
                
                // Add extended fields
                patientData.put("suffix", patient.getSuffix() != null ? patient.getSuffix() : "");
                patientData.put("full_name", patient.getFullName() != null ? patient.getFullName() : "");
                patientData.put("age", patient.getAge() != null ? patient.getAge() : "");
                patientData.put("full_address", patient.getFullAddress() != null ? patient.getFullAddress() : "");
                patientData.put("phone_number", patient.getPhoneNumber() != null ? patient.getPhoneNumber() : "");
                patientData.put("allergies", patient.getAllergies() != null ? patient.getAllergies() : "");
                patientData.put("medications", patient.getMedications() != null ? patient.getMedications() : "");
                patientData.put("medical_history", patient.getMedicalHistory() != null ? patient.getMedicalHistory() : "");
                patientData.put("emergency_contact_name", patient.getEmergencyContactName() != null ? patient.getEmergencyContactName() : "");
                patientData.put("emergency_contact_phone", patient.getEmergencyContactPhone() != null ? patient.getEmergencyContactPhone() : "");
                patientData.put("birth_place", patient.getBirthPlace() != null ? patient.getBirthPlace() : "");
                
                // Add vital signs fields (these were missing and causing incomplete syncs)
                patientData.put("pulse_rate", patient.getPulseRate() != null ? patient.getPulseRate() : "");
                patientData.put("blood_pressure", patient.getBloodPressure() != null ? patient.getBloodPressure() : "");
                patientData.put("temperature", patient.getTemperature() != null ? patient.getTemperature() : "");
                patientData.put("blood_sugar", patient.getBloodSugar() != null ? patient.getBloodSugar() : "");
                patientData.put("pain_scale", patient.getPainScale() != null ? patient.getPainScale() : "");
                patientData.put("symptoms_description", patient.getSymptomsDescription() != null ? patient.getSymptomsDescription() : "");
                
                // Add NFC UID if available
                if (patient.getNfcUid() != null && !patient.getNfcUid().isEmpty()) {
                    patientData.put("nfc_uid", patient.getNfcUid());
                }
                
                // Check if patient exists in Firebase first
                DatabaseReference patientRef = firebaseHelper.getReference("patients");
                if (patientRef == null) {
                    Log.e(TAG, "Cannot get patients reference - Firebase rootRef is null");
                    return;
                }
                patientRef.child(patient.getPatientId())
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                            boolean patientExists = snapshot.exists();
                            boolean dataChanged = false;
                            Map<String, Object> existingData = null; // Declare outside if-else for later use
                            
                            if (!patientExists) {
                                // New patient - set created_date and last_updated
                                if (patient.getCreatedDate() != null && !patient.getCreatedDate().isEmpty()) {
                                    patientData.put("created_date", patient.getCreatedDate());
                                } else {
                                    // Fallback to current timestamp if not set
                                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                                    patientData.put("created_date", sdf.format(new java.util.Date()));
                                }
                                dataChanged = true; // New patient always counts as change
                            } else {
                                // Patient exists - preserve existing created_date and check if data changed
                                existingData = (Map<String, Object>) snapshot.getValue();
                                if (existingData != null) {
                                    if (existingData.containsKey("created_date")) {
                                        patientData.put("created_date", existingData.get("created_date"));
                                    }
                                    
                                    // Compare data to detect actual changes (exclude last_updated and created_date from comparison)
                                    for (Map.Entry<String, Object> entry : patientData.entrySet()) {
                                        String key = entry.getKey();
                                        if (!key.equals("last_updated") && !key.equals("created_date")) {
                                            Object newValue = entry.getValue();
                                            Object oldValue = existingData.get(key);
                                            
                                            // Compare values (handle null cases)
                                            if (newValue == null && oldValue != null) {
                                                dataChanged = true;
                                                break;
                                            } else if (newValue != null && !newValue.equals(oldValue)) {
                                                dataChanged = true;
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    // No existing data - treat as new
                                    dataChanged = true;
                                }
                            }
                            
                            // Only update last_updated if data actually changed
                            if (dataChanged) {
                                // Check if existing data has more fields than our payload (complete data already saved)
                                // If existing data has vital signs but our payload doesn't, skip sync to avoid overwriting
                                boolean existingHasVitals = false;
                                if (patientExists && existingData != null) {
                                    existingHasVitals = existingData.containsKey("pulse_rate") || 
                                                       existingData.containsKey("blood_pressure") || 
                                                       existingData.containsKey("temperature");
                                }
                                boolean ourPayloadHasVitals = (patient.getPulseRate() != null && !patient.getPulseRate().isEmpty()) ||
                                                             (patient.getBloodPressure() != null && !patient.getBloodPressure().isEmpty()) ||
                                                             (patient.getTemperature() != null && !patient.getTemperature().isEmpty());
                                
                                // If existing data has more complete info and our payload is incomplete, skip sync
                                if (patientExists && existingHasVitals && !ourPayloadHasVitals) {
                                    Log.w(TAG, "⚠️ Skipping sync - existing data has vital signs but our payload doesn't. " +
                                          "Complete data was likely saved directly to Firebase. Patient: " + patient.getPatientId());
                                    return; // Don't overwrite complete data with incomplete data
                                }
                                
                                patientData.put("last_updated", System.currentTimeMillis());
                                // Use setValue via syncPatientToFirebase to ensure ALL fields are saved (not just updated ones)
                                firebaseHelper.syncPatientToFirebase(patient.getPatientId(), patientData);
                                Log.d(TAG, "✅ Patient data synced with complete fields: " + patient.getPatientId());
                            } else {
                                Log.d(TAG, "⏭️ Patient data unchanged - skipping update: " + patient.getPatientId());
                            }
                        }
                        
                        @Override
                        public void onCancelled(com.google.firebase.database.DatabaseError error) {
                            Log.e(TAG, "Error checking patient existence: " + patient.getPatientId(), error.toException());
                            // Fallback: assume new patient (always update on error)
                            if (patient.getCreatedDate() != null && !patient.getCreatedDate().isEmpty()) {
                                patientData.put("created_date", patient.getCreatedDate());
                            } else {
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                                patientData.put("created_date", sdf.format(new java.util.Date()));
                            }
                            patientData.put("last_updated", System.currentTimeMillis());
                            firebaseHelper.syncPatientToFirebase(patient.getPatientId(), patientData);
                        }
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error syncing patient: " + patient.getPatientId(), e);
            }
        });
    }
    
    /**
     * Sync employee to Firebase Realtime Database
     * OPTIMIZED: Runs on background thread to avoid blocking UI
     */
    public void syncEmployee(Employee employee) {
        if (employee == null) return;
        if (firebaseHelper == null) {
            Log.w(TAG, "FirebaseHelper not available - skipping employee sync");
            return;
        }
        
        // Run on background thread to avoid blocking UI
        com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
            try {
                Map<String, Object> employeeData = new HashMap<>();
                employeeData.put("employee_id", employee.getEmployeeId());
                employeeData.put("first_name", employee.getFirstName());
                employeeData.put("last_name", employee.getLastName());
                employeeData.put("email", employee.getEmail());
                employeeData.put("phone", employee.getPhone());
                employeeData.put("role", employee.getRole());
                employeeData.put("username", employee.getUsername());
                employeeData.put("password", employee.getPassword()); // CRITICAL: Include password for forgot password feature
                employeeData.put("created_date", employee.getCreatedDate());
                employeeData.put("is_active", employee.isActive());
                employeeData.put("profile_picture_url", employee.getProfilePictureUrl());
                employeeData.put("last_updated", System.currentTimeMillis());
                
                firebaseHelper.syncEmployeeToFirebase(employee.getEmployeeId(), employeeData);
            } catch (Exception e) {
                Log.e(TAG, "Error syncing employee", e);
            }
        });
    }
    
    /**
     * Start listening to Firebase Realtime Database for real-time updates
     */
    public void startListeningToUpdates() {
        if (firebaseHelper == null) {
            Log.w(TAG, "FirebaseHelper not available - cannot start listeners");
            return;
        }
        
        if (isSyncing) {
            return;
        }
        
        isSyncing = true;
        
        // Listen to medicines (limited batch) - OPTIMIZED: Run callbacks on background thread with debouncing
        firebaseHelper.listenToMedicines(new FirebaseHelper.FirebaseDataCallback() {
            @Override
            public void onDataReceived(String documentId, Map<String, Object> data) {
                // Debounce rapid updates to prevent overwhelming the system
                final String medicineId = documentId;
                final Map<String, Object> medicineData = data;
                com.example.h_cas.utils.Debouncer.getInstance().debounce("sync_medicine_" + medicineId, () -> {
                    // Run sync on background thread to avoid blocking UI
                    com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                        syncMedicineFromFirestore(medicineData);
                    });
                }, 200); // 200ms debounce for medicine updates
            }
            
            @Override
            public void onComplete() {
                // Sync complete - no need to log
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error syncing medicines", e);
            }
        }, SYNC_LIST_LIMIT);
        
        // Listen to prescriptions - OPTIMIZED: Run callbacks on background thread with debouncing
        firebaseHelper.listenToPrescriptions(new FirebaseHelper.FirebaseDataCallback() {
            @Override
            public void onDataReceived(String documentId, Map<String, Object> data) {
                // Debounce rapid updates to prevent overwhelming the system
                final String prescriptionId = documentId;
                final Map<String, Object> prescriptionData = data;
                com.example.h_cas.utils.Debouncer.getInstance().debounce("sync_prescription_" + prescriptionId, () -> {
                    // Run sync on background thread to avoid blocking UI
                    com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                        syncPrescriptionFromFirestore(prescriptionData);
                    });
                }, 200); // 200ms debounce for prescription updates
            }
            
            @Override
            public void onComplete() {
                // Sync complete - no need to log
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error syncing prescriptions", e);
            }
        }, SYNC_LIST_LIMIT);
        
        // Listen to patients - OPTIMIZED: Run callbacks on background thread with debouncing
        firebaseHelper.listenToPatients(new FirebaseHelper.FirebaseDataCallback() {
            @Override
            public void onDataReceived(String documentId, Map<String, Object> data) {
                // Debounce rapid updates to prevent overwhelming the system
                final String patientId = documentId;
                final Map<String, Object> patientData = data;
                com.example.h_cas.utils.Debouncer.getInstance().debounce("sync_patient_" + patientId, () -> {
                    // Run sync on background thread to avoid blocking UI
                    com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                        syncPatientFromFirestore(patientData);
                    });
                }, 200); // 200ms debounce for patient updates
            }
            
            @Override
            public void onComplete() {
                // Sync complete - no need to log
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error syncing patients", e);
            }
        }, SYNC_LIST_LIMIT);
    }
    
    /**
     * Stop listening to Firebase Realtime Database updates
     */
    public void stopListening() {
        if (firebaseHelper != null) {
            firebaseHelper.stopAllListeners();
        }
        isSyncing = false;
    }
    
    /**
     * Sync medicine from Firebase Realtime Database to SQLite
     * Optimized to run on background thread for better performance
     * NOTE: This method is already called from background thread, no need to wrap again
     */
    private void syncMedicineFromFirestore(Map<String, Object> data) {
        // Database operations already on background thread
        if (databaseHelper != null) {
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
                    
                } catch (IllegalStateException e) {
                    // SQLite connection pool closed - log but don't crash
                    if (e.getMessage() != null && e.getMessage().contains("connection pool has been closed")) {
                        Log.w(TAG, "⚠️ SQLite connection pool closed - skipping medicine sync: " + data.get("medicine_id"));
                    } else {
                        Log.e(TAG, "Error syncing medicine from Firebase", e);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error syncing medicine from Firebase", e);
                }
        }
    }
    
    /**
     * Sync prescription from Firebase Realtime Database to SQLite
     * Optimized to run on background thread for better performance
     * NOTE: This method is already called from background thread
     */
    private void syncPrescriptionFromFirestore(Map<String, Object> data) {
        // Database operations already on background thread
        if (databaseHelper != null) {
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
                
            } catch (IllegalStateException e) {
                // SQLite connection pool closed - log but don't crash
                if (e.getMessage() != null && e.getMessage().contains("connection pool has been closed")) {
                    Log.w(TAG, "⚠️ SQLite connection pool closed - skipping prescription sync: " + data.get("prescription_id"));
                } else {
                    Log.e(TAG, "Error syncing prescription from Firebase", e);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing prescription from Firebase", e);
            }
        }
    }
    
    /**
     * Sync patient from Firebase Realtime Database to SQLite
     * Optimized to run on background thread for better performance
     * NOTE: This method is already called from background thread
     */
    private void syncPatientFromFirestore(Map<String, Object> data) {
        // Database operations already on background thread
        if (databaseHelper != null) {
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
                
            } catch (IllegalStateException e) {
                // SQLite connection pool closed - log but don't crash
                if (e.getMessage() != null && e.getMessage().contains("connection pool has been closed")) {
                    Log.w(TAG, "⚠️ SQLite connection pool closed - skipping patient sync: " + data.get("patient_id"));
                } else {
                    Log.e(TAG, "Error syncing patient from Firebase", e);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing patient from Firebase", e);
            }
        }
    }
    
    /**
     * Check if sync is active
     */
    public boolean isSyncing() {
        return isSyncing;
    }
}



