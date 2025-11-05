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
 * FirebaseSyncManager handles bidirectional sync between SQLite and Firebase Realtime Database
 */
public class FirebaseSyncManager {
    
    private static final String TAG = "FirebaseSyncManager";
    private FirebaseHelper firebaseHelper;
    private HCasDatabaseHelper databaseHelper;
    private Context context;
    private boolean isSyncing = false;
    
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
                Log.d(TAG, "FirebaseHelper initialized successfully");
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
     */
    public void syncMedicine(Medicine medicine) {
        if (medicine == null) return;
        if (firebaseHelper == null) {
            Log.w(TAG, "FirebaseHelper not available - skipping medicine sync");
            return;
        }
        
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error syncing medicine", e);
        }
    }
    
    /**
     * Sync prescription to Firebase Realtime Database
     */
    public void syncPrescription(Prescription prescription) {
        if (prescription == null) return;
        if (firebaseHelper == null) {
            Log.w(TAG, "FirebaseHelper not available - skipping prescription sync");
            return;
        }
        
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error syncing prescription", e);
        }
    }
    
    /**
     * Sync patient to Firebase Realtime Database
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
        
        try {
            Log.d(TAG, "🔄 Starting to sync patient: " + patient.getPatientId());
            
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
            
            patientData.put("last_updated", System.currentTimeMillis());
            
            Log.d(TAG, "📤 Sending patient data to Firebase: " + patientData);
            firebaseHelper.syncPatientToFirebase(patient.getPatientId(), patientData);
            Log.d(TAG, "✅ Patient sync initiated: " + patient.getPatientId());
        } catch (Exception e) {
            Log.e(TAG, "❌ Error syncing patient: " + patient.getPatientId(), e);
            Log.e(TAG, "   Error message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sync employee to Firebase Realtime Database
     */
    public void syncEmployee(Employee employee) {
        if (employee == null) return;
        if (firebaseHelper == null) {
            Log.w(TAG, "FirebaseHelper not available - skipping employee sync");
            return;
        }
        
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error syncing employee", e);
        }
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
            Log.d(TAG, "Already listening to updates");
            return;
        }
        
        isSyncing = true;
        Log.d(TAG, "Starting Firebase real-time listeners...");
        
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
        
        Log.d(TAG, "✅ Firebase real-time listeners started");
    }
    
    /**
     * Stop listening to Firebase Realtime Database updates
     */
    public void stopListening() {
        firebaseHelper.stopAllListeners();
        isSyncing = false;
        Log.d(TAG, "Stopped listening to Firebase updates");
    }
    
    /**
     * Sync medicine from Firebase Realtime Database to SQLite
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
                Log.d(TAG, "Added new medicine from Firebase Realtime Database: " + medicine.getMedicineName());
            } else {
                // Update existing medicine
                databaseHelper.updateMedicine(medicine);
                Log.d(TAG, "Updated medicine from Firebase Realtime Database: " + medicine.getMedicineName());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing medicine from Firebase Realtime Database", e);
        }
    }
    
    /**
     * Sync prescription from Firebase Realtime Database to SQLite
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
                Log.d(TAG, "Added new prescription from Firebase Realtime Database: " + prescription.getPrescriptionId());
            } else {
                databaseHelper.updatePrescription(prescription);
                Log.d(TAG, "Updated prescription from Firebase Realtime Database: " + prescription.getPrescriptionId());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing prescription from Firebase Realtime Database", e);
        }
    }
    
    /**
     * Sync patient from Firebase Realtime Database to SQLite
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
                Log.d(TAG, "Added new patient from Firebase Realtime Database: " + patient.getPatientId());
            } else {
                databaseHelper.updatePatient(patient);
                Log.d(TAG, "Updated patient from Firebase Realtime Database: " + patient.getPatientId());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing patient from Firebase Realtime Database", e);
        }
    }
    
    /**
     * Check if sync is active
     */
    public boolean isSyncing() {
        return isSyncing;
    }
}



