package com.example.h_cas.database;

import android.content.Context;
import android.util.Log;

import com.example.h_cas.models.Employee;
import com.example.h_cas.models.Patient;
import com.example.h_cas.models.Medicine;
import com.example.h_cas.models.Prescription;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * FirebaseRTDBHelper - Primary database using Firebase Realtime Database
 * Replaces SQLite for all database operations
 */
public class FirebaseRTDBHelper {
    
    private static final String TAG = "FirebaseRTDBHelper";
    private final DatabaseReference rootRef;
    private final Context context;
    
    // Database paths
    private static final String PATH_EMPLOYEES = "employees";
    private static final String PATH_PATIENTS = "patients";
    private static final String PATH_MEDICINES = "medicines";
    private static final String PATH_PRESCRIPTIONS = "prescriptions";
    private static final String PATH_CASES = "healthcare_cases";
    private static final String PATH_HISTORY = "history";
    
    public FirebaseRTDBHelper(Context context) {
        this.context = context;
        // Use FirebaseOnlineOnly for pure online mode
        FirebaseOnlineOnly.init();
        this.rootRef = FirebaseOnlineOnly.getRootRef();
        
        if (rootRef == null) {
            Log.e(TAG, "❌ Firebase rootRef is null - database operations will fail");
        } else {
            Log.d(TAG, "✅ FirebaseRTDBHelper initialized - using Firebase RTDB as primary database");
        }
    }
    
    /* ─────────────────────────────────────────────
     * 🔹 AUTHENTICATION METHODS
     * ───────────────────────────────────────────── */
    
    /**
     * Authenticate user login using Firebase RTDB
     * Supports both username and email authentication
     */
    public void authenticateUser(String username, String password, AuthenticationCallback callback) {
        if (rootRef == null) {
            Log.e(TAG, "❌ Cannot authenticate: Firebase rootRef is null");
            if (callback != null) callback.onResult(null);
            return;
        }
        
        // Check if input is an email (contains @)
        boolean isEmail = username != null && username.contains("@");
        
        // OPTIMIZED: Use direct path lookup instead of orderByChild (faster, no index needed)
        // First try direct lookup by employee_id if username matches pattern
        // Otherwise, scan all employees (for small datasets this is acceptable)
        
        Query query;
        if (isEmail) {
            // For email, we need to scan (or create index)
            // Using orderByChild requires index - for now, get all and filter
            query = rootRef.child(PATH_EMPLOYEES);
        } else {
            // For username, try direct lookup first, then scan if needed
            query = rootRef.child(PATH_EMPLOYEES);
        }
        
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Employee employee = null;
                
                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Map<String, Object> data = (Map<String, Object>) child.getValue();
                        if (data != null) {
                            // Check username/email match
                            String storedUsername = getString(data, "username");
                            String storedEmail = getString(data, "email");
                            
                            boolean usernameMatches = false;
                            if (isEmail) {
                                // Check if storedEmail is not null before comparing
                                if (storedEmail != null && username != null) {
                                    usernameMatches = storedEmail.equalsIgnoreCase(username);
                                }
                            } else {
                                // Check if storedUsername is not null before comparing
                                if (storedUsername != null && username != null) {
                                    usernameMatches = storedUsername.equals(username);
                                }
                            }
                            
                            if (!usernameMatches) {
                                continue; // Skip if username/email doesn't match
                            }
                            
                            // Check password and active status
                            String storedPassword = getString(data, "password");
                            Object isActiveObj = data.get("is_active");
                            boolean isActive = true;
                            
                            if (isActiveObj instanceof Boolean) {
                                isActive = (Boolean) isActiveObj;
                            } else if (isActiveObj instanceof Long) {
                                isActive = ((Long) isActiveObj) == 1;
                            } else if (isActiveObj instanceof Integer) {
                                isActive = ((Integer) isActiveObj) == 1;
                            }
                            
                            // Check if storedPassword is not null before comparing
                            if (storedPassword != null && password != null && storedPassword.equals(password) && isActive) {
                                // Create employee object
                                employee = mapToEmployee(child.getKey(), data);
                                Log.d(TAG, "✅ Authentication successful for: " + username);
                                break;
                            } else {
                                Log.d(TAG, "❌ Password mismatch or inactive for: " + username);
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ No employees found in Firebase RTDB - accounts may not be migrated yet");
                }
                
                if (callback != null) {
                    callback.onResult(employee);
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "❌ Authentication query cancelled: " + error.getMessage());
                if (callback != null) callback.onResult(null);
            }
        });
    }
    
    /**
     * Synchronous authentication (for compatibility with existing code)
     * WARNING: This blocks the thread - use async version when possible
     */
    public Employee authenticateUserSync(String username, String password) {
        final Employee[] result = new Employee[1];
        final CountDownLatch latch = new CountDownLatch(1);
        
        authenticateUser(username, password, employee -> {
            result[0] = employee;
            latch.countDown();
        });
        
        try {
            // Wait up to 5 seconds for result
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Authentication interrupted", e);
        }
        
        return result[0];
    }
    
    /* ─────────────────────────────────────────────
     * 🔹 EMPLOYEE METHODS
     * ───────────────────────────────────────────── */
    
    /**
     * Get all employees from Firebase RTDB
     */
    public void getAllEmployees(EmployeesCallback callback) {
        if (rootRef == null) {
            Log.e(TAG, "❌ Cannot get employees: Firebase rootRef is null");
            if (callback != null) callback.onResult(new ArrayList<>());
            return;
        }
        
        rootRef.child(PATH_EMPLOYEES)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Employee> employees = new ArrayList<>();
                        
                        if (snapshot.exists() && snapshot.hasChildren()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                Map<String, Object> data = (Map<String, Object>) child.getValue();
                                if (data != null) {
                                    Employee employee = mapToEmployee(child.getKey(), data);
                                    if (employee != null) {
                                        employees.add(employee);
                                    }
                                }
                            }
                        }
                        
                        if (callback != null) callback.onResult(employees);
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "❌ Get employees cancelled: " + error.getMessage());
                        if (callback != null) callback.onResult(new ArrayList<>());
                    }
                });
    }
    
    /**
     * Get employee by ID
     */
    public void getEmployeeById(String employeeId, EmployeeCallback callback) {
        if (rootRef == null || employeeId == null) {
            if (callback != null) callback.onResult(null);
            return;
        }
        
        rootRef.child(PATH_EMPLOYEES).child(employeeId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Employee employee = null;
                        if (snapshot.exists()) {
                            Map<String, Object> data = (Map<String, Object>) snapshot.getValue();
                            if (data != null) {
                                employee = mapToEmployee(employeeId, data);
                            }
                        }
                        if (callback != null) callback.onResult(employee);
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "❌ Get employee cancelled: " + error.getMessage());
                        if (callback != null) callback.onResult(null);
                    }
                });
    }
    
    /**
     * Add employee to Firebase RTDB
     */
    public void addEmployee(Employee employee, OperationCallback callback) {
        if (rootRef == null || employee == null || employee.getEmployeeId() == null) {
            Log.e(TAG, "❌ Cannot add employee: invalid parameters");
            if (callback != null) callback.onResult(false);
            return;
        }
        
        Map<String, Object> employeeData = employeeToMap(employee);
        
        rootRef.child(PATH_EMPLOYEES).child(employee.getEmployeeId())
                .setValue(employeeData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Employee added: " + employee.getEmployeeId());
                    if (callback != null) callback.onResult(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to add employee: " + employee.getEmployeeId(), e);
                    if (callback != null) callback.onResult(false);
                });
    }
    
    /**
     * Update employee in Firebase RTDB
     */
    public void updateEmployee(Employee employee, OperationCallback callback) {
        if (rootRef == null || employee == null || employee.getEmployeeId() == null) {
            Log.e(TAG, "❌ Cannot update employee: invalid parameters");
            if (callback != null) callback.onResult(false);
            return;
        }
        
        Map<String, Object> employeeData = employeeToMap(employee);
        
        rootRef.child(PATH_EMPLOYEES).child(employee.getEmployeeId())
                .updateChildren(employeeData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Employee updated: " + employee.getEmployeeId());
                    if (callback != null) callback.onResult(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update employee: " + employee.getEmployeeId(), e);
                    if (callback != null) callback.onResult(false);
                });
    }
    
    /**
     * Delete employee (soft delete - set is_active to false)
     */
    public void deleteEmployee(String employeeId, OperationCallback callback) {
        if (rootRef == null || employeeId == null) {
            if (callback != null) callback.onResult(false);
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("is_active", false);
        
        rootRef.child(PATH_EMPLOYEES).child(employeeId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Employee deactivated: " + employeeId);
                    if (callback != null) callback.onResult(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to delete employee: " + employeeId, e);
                    if (callback != null) callback.onResult(false);
                });
    }
    
    /* ─────────────────────────────────────────────
     * 🔹 PATIENT METHODS
     * ───────────────────────────────────────────── */
    
    /**
     * Get all patients from Firebase RTDB
     */
    /**
     * Get all patients from Firebase RTDB
     * @param callback Callback to receive the list of patients
     * @param filterStatus Optional filter by patient_status ("on", "off", or null for all)
     */
    public void getAllPatients(PatientsCallback callback, String filterStatus) {
        if (rootRef == null) {
            if (callback != null) callback.onResult(new ArrayList<>());
            return;
        }
        
        rootRef.child(PATH_PATIENTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Patient> patients = new ArrayList<>();
                        if (snapshot.exists() && snapshot.hasChildren()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                Map<String, Object> data = (Map<String, Object>) child.getValue();
                                if (data != null) {
                                    Patient patient = mapToPatient(child.getKey(), data);
                                    if (patient != null) {
                                        // Filter by patient_status if specified
                                        if (filterStatus == null) {
                                            // No filter - include all
                                            patients.add(patient);
                                        } else {
                                            // Filter by status
                                            String patientStatus = patient.getPatientStatus();
                                            if (patientStatus == null || patientStatus.isEmpty()) {
                                                // Default to "on" for backward compatibility
                                                patientStatus = "on";
                                            }
                                            if (filterStatus.equals(patientStatus)) {
                                                patients.add(patient);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (callback != null) callback.onResult(patients);
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "❌ Get patients cancelled: " + error.getMessage());
                        if (callback != null) callback.onResult(new ArrayList<>());
                    }
                });
    }
    
    /**
     * Get all patients from Firebase RTDB (backward compatibility - returns all patients)
     */
    public void getAllPatients(PatientsCallback callback) {
        getAllPatients(callback, null);
    }
    
    /**
     * Get patient by ID from Firebase RTDB
     */
    public void getPatientById(String patientId, PatientCallback callback) {
        if (rootRef == null || patientId == null || patientId.isEmpty()) {
            if (callback != null) callback.onResult(null);
            return;
        }
        
        rootRef.child(PATH_PATIENTS).child(patientId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Patient patient = null;
                        if (snapshot.exists()) {
                            Map<String, Object> data = (Map<String, Object>) snapshot.getValue();
                            if (data != null) {
                                patient = mapToPatient(snapshot.getKey(), data);
                            }
                        }
                        if (callback != null) callback.onResult(patient);
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "❌ Get patient by ID cancelled: " + error.getMessage());
                        if (callback != null) callback.onResult(null);
                    }
                });
    }
    
    /**
     * Add patient to Firebase RTDB
     * Preserves created_date if patient already exists
     */
    public void addPatient(Patient patient, OperationCallback callback) {
        if (rootRef == null || patient == null || patient.getPatientId() == null) {
            if (callback != null) callback.onResult(false);
            return;
        }
        
        Map<String, Object> patientData = patientToMap(patient);
        
        // Check if patient exists first to preserve created_date
        rootRef.child(PATH_PATIENTS).child(patient.getPatientId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        boolean patientExists = snapshot.exists();
                        boolean dataChanged = false;
                        
                        // Only set created_date if patient doesn't exist (new patient)
                        if (!patientExists) {
                            if (patient.getCreatedDate() != null && !patient.getCreatedDate().isEmpty()) {
                                patientData.put("created_date", patient.getCreatedDate());
                            } else {
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                                patientData.put("created_date", sdf.format(new java.util.Date()));
                            }
                            dataChanged = true; // New patient always counts as change
                        } else {
                            // Patient exists - preserve existing created_date and check if data changed
                            Map<String, Object> existingData = (Map<String, Object>) snapshot.getValue();
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
                            patientData.put("last_updated", System.currentTimeMillis());
                            // Use updateChildren instead of setValue to only update changed fields
                            rootRef.child(PATH_PATIENTS).child(patient.getPatientId())
                                    .updateChildren(patientData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "✅ Patient " + (patientExists ? "updated" : "added") + ": " + patient.getPatientId());
                                        if (callback != null) callback.onResult(true);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "❌ Failed to " + (patientExists ? "update" : "add") + " patient: " + patient.getPatientId(), e);
                                        if (callback != null) callback.onResult(false);
                                    });
                        } else {
                            Log.d(TAG, "⏭️ Patient data unchanged - skipping update: " + patient.getPatientId());
                            if (callback != null) callback.onResult(true); // Still return success
                        }
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "❌ Error checking patient existence: " + patient.getPatientId(), error.toException());
                        // Fallback: assume new patient
                        if (patient.getCreatedDate() != null && !patient.getCreatedDate().isEmpty()) {
                            patientData.put("created_date", patient.getCreatedDate());
                        } else {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                            patientData.put("created_date", sdf.format(new java.util.Date()));
                        }
                        patientData.put("last_updated", System.currentTimeMillis());
                        rootRef.child(PATH_PATIENTS).child(patient.getPatientId())
                                .setValue(patientData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "✅ Patient added (fallback): " + patient.getPatientId());
                                    if (callback != null) callback.onResult(true);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Failed to add patient (fallback): " + patient.getPatientId(), e);
                                    if (callback != null) callback.onResult(false);
                                });
                    }
                });
    }
    
    /* ─────────────────────────────────────────────
     * 🔹 MEDICINE METHODS
     * ───────────────────────────────────────────── */
    
    /**
     * Get all medicines from Firebase RTDB
     */
    public void getAllMedicines(MedicinesCallback callback) {
        if (rootRef == null) {
            if (callback != null) callback.onResult(new ArrayList<>());
            return;
        }
        
        rootRef.child(PATH_MEDICINES)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Medicine> medicines = new ArrayList<>();
                        if (snapshot.exists() && snapshot.hasChildren()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                Map<String, Object> data = (Map<String, Object>) child.getValue();
                                if (data != null) {
                                    Medicine medicine = mapToMedicine(child.getKey(), data);
                                    if (medicine != null) {
                                        medicines.add(medicine);
                                    }
                                }
                            }
                        }
                        if (callback != null) callback.onResult(medicines);
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "❌ Get medicines cancelled: " + error.getMessage());
                        if (callback != null) callback.onResult(new ArrayList<>());
                    }
                });
    }
    
    /* ─────────────────────────────────────────────
     * 🔹 PRESCRIPTION METHODS
     * ───────────────────────────────────────────── */
    
    /**
     * Get all prescriptions from Firebase RTDB
     */
    public void getAllPrescriptions(PrescriptionsCallback callback) {
        if (rootRef == null) {
            if (callback != null) callback.onResult(new ArrayList<>());
            return;
        }
        
        rootRef.child(PATH_PRESCRIPTIONS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Prescription> prescriptions = new ArrayList<>();
                        if (snapshot.exists() && snapshot.hasChildren()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                Map<String, Object> data = (Map<String, Object>) child.getValue();
                                if (data != null) {
                                    Prescription prescription = mapToPrescription(child.getKey(), data);
                                    if (prescription != null) {
                                        prescriptions.add(prescription);
                                    }
                                }
                            }
                        }
                        if (callback != null) callback.onResult(prescriptions);
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "❌ Get prescriptions cancelled: " + error.getMessage());
                        if (callback != null) callback.onResult(new ArrayList<>());
                    }
                });
    }
    
    /**
     * Add prescription to Firebase RTDB
     */
    public void addPrescription(Prescription prescription, OperationCallback callback) {
        if (rootRef == null || prescription == null || prescription.getPrescriptionId() == null) {
            if (callback != null) callback.onResult(false);
            return;
        }
        
        Map<String, Object> prescriptionData = prescriptionToMap(prescription);
        
        rootRef.child(PATH_PRESCRIPTIONS).child(prescription.getPrescriptionId())
                .setValue(prescriptionData, (error, ref) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Failed to add prescription: " + error.getMessage());
                        if (callback != null) callback.onResult(false);
                    } else {
                        Log.d(TAG, "✅ Prescription added: " + prescription.getPrescriptionId());
                        if (callback != null) callback.onResult(true);
                    }
                });
    }
    
    /**
     * Add prescription to history folder for doctors
     */
    public void addPrescriptionToHistory(Prescription prescription, OperationCallback callback) {
        if (rootRef == null || prescription == null || prescription.getPrescriptionId() == null) {
            if (callback != null) callback.onResult(false);
            return;
        }
        
        Map<String, Object> prescriptionData = prescriptionToMap(prescription);
        
        // Save to history path: history/prescription_id
        rootRef.child(PATH_HISTORY).child(prescription.getPrescriptionId())
                .setValue(prescriptionData, (error, ref) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Failed to add prescription to history: " + error.getMessage());
                        if (callback != null) callback.onResult(false);
                    } else {
                        Log.d(TAG, "✅ Prescription added to history: " + prescription.getPrescriptionId());
                        if (callback != null) callback.onResult(true);
                    }
                });
    }
    
    /**
     * Get all prescriptions from history folder
     * Handles both flat and nested structures
     */
    public void getAllPrescriptionsFromHistory(PrescriptionsCallback callback) {
        if (rootRef == null) {
            Log.w(TAG, "⚠️ getAllPrescriptionsFromHistory: rootRef is null");
            if (callback != null) callback.onResult(new ArrayList<>());
            return;
        }
        
        Log.d(TAG, "📥 Fetching prescriptions from history folder...");
        rootRef.child(PATH_HISTORY)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Prescription> prescriptions = new ArrayList<>();
                        Log.d(TAG, "📥 History snapshot exists: " + snapshot.exists());
                        Log.d(TAG, "📥 History snapshot has children: " + (snapshot.exists() && snapshot.hasChildren()));
                        
                        if (snapshot.exists() && snapshot.hasChildren()) {
                            int totalChildren = (int) snapshot.getChildrenCount();
                            Log.d(TAG, "📥 Total children in history snapshot: " + totalChildren);
                            
                            int count = 0;
                            int addedCount = 0;
                            int skippedCount = 0;
                            
                            for (DataSnapshot child : snapshot.getChildren()) {
                                count++;
                                String childKey = child.getKey();
                                Log.d(TAG, "📥 Processing history item " + count + "/" + totalChildren + ": " + childKey);
                                
                                // Try to read as direct prescription first (most common case)
                                Map<String, Object> data = (Map<String, Object>) child.getValue();
                                
                                if (data != null) {
                                    // Check if this is a direct prescription object
                                    if (data.containsKey("patient_id") || data.containsKey("medication") || data.containsKey("prescription_id")) {
                                        // Direct prescription object
                                        Prescription prescription = mapToPrescription(childKey, data);
                                        if (prescription != null) {
                                            prescriptions.add(prescription);
                                            addedCount++;
                                            Log.d(TAG, "✅ Added prescription: " + prescription.getPrescriptionId() + " for patient: " + prescription.getPatientId() + " (Total added: " + addedCount + ")");
                                        } else {
                                            skippedCount++;
                                            Log.w(TAG, "⚠️ Failed to map prescription: " + childKey + " (Skipped: " + skippedCount + ")");
                                        }
                                    } else if (child.hasChildren()) {
                                        // Nested structure - check if children are prescription objects
                                        for (DataSnapshot nestedChild : child.getChildren()) {
                                            Map<String, Object> nestedData = (Map<String, Object>) nestedChild.getValue();
                                            if (nestedData != null && (nestedData.containsKey("patient_id") || nestedData.containsKey("medication"))) {
                                                // This is a prescription object
                                                Prescription prescription = mapToPrescription(nestedChild.getKey(), nestedData);
                                                if (prescription != null) {
                                                    // Ensure prescription ID includes parent key if needed
                                                    if (prescription.getPrescriptionId() == null || prescription.getPrescriptionId().isEmpty() || 
                                                        prescription.getPrescriptionId().equals(nestedChild.getKey())) {
                                                        prescription.setPrescriptionId(childKey + "_" + nestedChild.getKey());
                                                    }
                                                    prescriptions.add(prescription);
                                                    addedCount++;
                                                    Log.d(TAG, "✅ Added nested prescription: " + prescription.getPrescriptionId() + " for patient: " + prescription.getPatientId() + " (Total added: " + addedCount + ")");
                                                }
                                            }
                                        }
                                    } else {
                                        // Data exists but doesn't look like a prescription - try to map anyway
                                        Prescription prescription = mapToPrescription(childKey, data);
                                        if (prescription != null) {
                                            prescriptions.add(prescription);
                                            addedCount++;
                                            Log.d(TAG, "✅ Added prescription (fallback mapping): " + prescription.getPrescriptionId() + " for patient: " + prescription.getPatientId() + " (Total added: " + addedCount + ")");
                                        } else {
                                            skippedCount++;
                                            Log.w(TAG, "⚠️ Data doesn't match prescription format: " + childKey + " (Skipped: " + skippedCount + ")");
                                        }
                                    }
                                } else {
                                    skippedCount++;
                                    Log.w(TAG, "⚠️ History item data is null: " + childKey + " (Skipped: " + skippedCount + ")");
                                }
                            }
                            Log.d(TAG, "📥 Total prescriptions found in history: " + prescriptions.size() + " (Processed: " + count + ", Added: " + addedCount + ", Skipped: " + skippedCount + ")");
                        } else {
                            Log.w(TAG, "⚠️ History folder is empty or doesn't exist");
                        }
                        
                        if (callback != null) {
                            Log.d(TAG, "📥 Calling callback with " + prescriptions.size() + " prescriptions");
                            callback.onResult(prescriptions);
                        }
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "❌ Get history prescriptions cancelled: " + error.getMessage());
                        if (callback != null) callback.onResult(new ArrayList<>());
                    }
                });
    }
    
    /* ─────────────────────────────────────────────
     * 🔹 HELPER METHODS - Data Conversion
     * ───────────────────────────────────────────── */
    
    private Employee mapToEmployee(String employeeId, Map<String, Object> data) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setFirstName(getString(data, "first_name"));
        employee.setLastName(getString(data, "last_name"));
        employee.setEmail(getString(data, "email"));
        employee.setPhone(getString(data, "phone"));
        employee.setRole(getString(data, "role"));
        employee.setUsername(getString(data, "username"));
        employee.setPassword(getString(data, "password"));
        employee.setCreatedDate(getString(data, "created_date"));
        
        Object isActiveObj = data.get("is_active");
        if (isActiveObj instanceof Boolean) {
            employee.setActive((Boolean) isActiveObj);
        } else if (isActiveObj instanceof Long) {
            employee.setActive(((Long) isActiveObj) == 1);
        } else {
            employee.setActive(true); // Default
        }
        
        employee.setProfilePictureUrl(getString(data, "profile_picture_url"));
        return employee;
    }
    
    private Map<String, Object> employeeToMap(Employee employee) {
        Map<String, Object> map = new HashMap<>();
        map.put("employee_id", employee.getEmployeeId());
        map.put("first_name", employee.getFirstName());
        map.put("last_name", employee.getLastName());
        map.put("email", employee.getEmail());
        map.put("phone", employee.getPhone());
        map.put("role", employee.getRole());
        map.put("username", employee.getUsername());
        map.put("password", employee.getPassword());
        map.put("created_date", employee.getCreatedDate());
        map.put("is_active", employee.isActive());
        map.put("profile_picture_url", employee.getProfilePictureUrl());
        return map;
    }
    
    private Patient mapToPatient(String patientId, Map<String, Object> data) {
        Patient patient = new Patient();
        patient.setPatientId(patientId);
        patient.setFirstName(getString(data, "first_name"));
        patient.setLastName(getString(data, "last_name"));
        patient.setSuffix(getString(data, "suffix"));
        patient.setFullName(getString(data, "full_name"));
        patient.setDateOfBirth(getString(data, "date_of_birth"));
        patient.setBirthPlace(getString(data, "birth_place"));
        patient.setGender(getString(data, "gender"));
        patient.setAge(getString(data, "age"));
        // Handle address - check both full_address and address fields
        String fullAddress = getString(data, "full_address");
        if (fullAddress == null || fullAddress.isEmpty()) {
            fullAddress = getString(data, "address");
        }
        patient.setFullAddress(fullAddress);
        patient.setAddress(fullAddress); // Also set legacy address field
        
        // Handle phone - check both phone_number and phone fields
        String phoneNumber = getString(data, "phone_number");
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            phoneNumber = getString(data, "phone");
        }
        patient.setPhoneNumber(phoneNumber);
        patient.setPhone(phoneNumber); // Also set legacy phone field
        
        patient.setEmail(getString(data, "email"));
        patient.setAllergies(getString(data, "allergies"));
        patient.setMedications(getString(data, "medications"));
        patient.setMedicalHistory(getString(data, "medical_history"));
        patient.setPulseRate(getString(data, "pulse_rate"));
        patient.setBloodPressure(getString(data, "blood_pressure"));
        patient.setTemperature(getString(data, "temperature"));
        patient.setBloodSugar(getString(data, "blood_sugar"));
        patient.setPainScale(getString(data, "pain_scale"));
        patient.setSymptomsDescription(getString(data, "symptoms_description"));
        patient.setEmergencyContactName(getString(data, "emergency_contact_name"));
        patient.setEmergencyContactPhone(getString(data, "emergency_contact_phone"));
        patient.setCreatedDate(getString(data, "created_date"));
        patient.setNfcUid(getString(data, "nfc_uid"));
        // Set patient_status - default to "on" if not present (for backward compatibility)
        String patientStatus = getString(data, "patient_status");
        patient.setPatientStatus(patientStatus != null && !patientStatus.isEmpty() ? patientStatus : "on");
        return patient;
    }
    
    private Prescription mapToPrescription(String prescriptionId, Map<String, Object> data) {
        if (data == null) {
            Log.w(TAG, "⚠️ mapToPrescription: data is null for prescriptionId: " + prescriptionId);
            return null;
        }
        
        Prescription prescription = new Prescription();
        // Use the key as prescription ID (more reliable than data field)
        prescription.setPrescriptionId(prescriptionId != null ? prescriptionId : getString(data, "prescription_id"));
        prescription.setPatientId(getString(data, "patient_id"));
        prescription.setPatientName(getString(data, "patient_name"));
        prescription.setMedication(getString(data, "medication"));
        prescription.setDosage(getString(data, "dosage"));
        prescription.setFrequency(getString(data, "frequency"));
        prescription.setDuration(getString(data, "duration"));
        prescription.setInstructions(getString(data, "instructions"));
        prescription.setDoctorId(getString(data, "doctor_id"));
        prescription.setDoctorName(getString(data, "doctor_name"));
        prescription.setCreatedDate(getString(data, "created_date"));
        prescription.setStatus(getString(data, "status"));
        
        // Validate required fields
        if (prescription.getPrescriptionId() == null || prescription.getPrescriptionId().isEmpty()) {
            Log.w(TAG, "⚠️ mapToPrescription: prescription ID is null or empty");
            return null;
        }
        
        // Don't skip prescription even if patient ID is missing - it will be handled in PatientHistoryFragment
        if (prescription.getPatientId() == null || prescription.getPatientId().isEmpty()) {
            Log.w(TAG, "⚠️ mapToPrescription: patient ID is null or empty for prescription: " + prescription.getPrescriptionId() + " - will use fallback");
            // Set a fallback patient ID to prevent skipping
            prescription.setPatientId("UNKNOWN_" + prescription.getPrescriptionId());
        }
        
        Log.d(TAG, "✅ mapToPrescription: Successfully mapped prescription " + prescription.getPrescriptionId() + " for patient " + prescription.getPatientId());
        return prescription;
    }
    
    private Map<String, Object> prescriptionToMap(Prescription prescription) {
        Map<String, Object> map = new HashMap<>();
        map.put("prescription_id", prescription.getPrescriptionId());
        map.put("patient_id", prescription.getPatientId());
        map.put("patient_name", prescription.getPatientName());
        map.put("medication", prescription.getMedication());
        map.put("dosage", prescription.getDosage() != null ? prescription.getDosage() : "");
        map.put("frequency", prescription.getFrequency());
        map.put("duration", prescription.getDuration());
        map.put("instructions", prescription.getInstructions() != null ? prescription.getInstructions() : "");
        map.put("doctor_id", prescription.getDoctorId());
        map.put("doctor_name", prescription.getDoctorName());
        map.put("created_date", prescription.getCreatedDate());
        map.put("status", prescription.getStatus() != null ? prescription.getStatus() : "Active");
        return map;
    }
    
    private Map<String, Object> patientToMap(Patient patient) {
        Map<String, Object> map = new HashMap<>();
        map.put("patient_id", patient.getPatientId());
        map.put("first_name", patient.getFirstName());
        map.put("last_name", patient.getLastName());
        map.put("suffix", patient.getSuffix());
        map.put("full_name", patient.getFullName());
        map.put("date_of_birth", patient.getDateOfBirth());
        map.put("birth_place", patient.getBirthPlace());
        map.put("gender", patient.getGender());
        map.put("age", patient.getAge());
        map.put("full_address", patient.getFullAddress());
        map.put("phone_number", patient.getPhoneNumber());
        map.put("phone", patient.getPhoneNumber()); // Legacy support
        map.put("email", patient.getEmail());
        map.put("address", patient.getFullAddress()); // Legacy support
        map.put("allergies", patient.getAllergies());
        map.put("medications", patient.getMedications());
        map.put("medical_history", patient.getMedicalHistory());
        map.put("pulse_rate", patient.getPulseRate());
        map.put("blood_pressure", patient.getBloodPressure());
        map.put("temperature", patient.getTemperature());
        map.put("blood_sugar", patient.getBloodSugar());
        map.put("pain_scale", patient.getPainScale());
        map.put("symptoms_description", patient.getSymptomsDescription());
        map.put("emergency_contact_name", patient.getEmergencyContactName());
        map.put("emergency_contact_phone", patient.getEmergencyContactPhone());
        map.put("nfc_uid", patient.getNfcUid());
        // Set patient_status - default to "on" if not set
        String patientStatus = patient.getPatientStatus();
        map.put("patient_status", patientStatus != null && !patientStatus.isEmpty() ? patientStatus : "on");
        // NOTE: created_date and last_updated should be handled by the caller
        // to avoid overwriting existing values. Only set if patient is new.
        if (patient.getCreatedDate() != null && !patient.getCreatedDate().isEmpty()) {
            map.put("created_date", patient.getCreatedDate());
        }
        // last_updated should only be set when data actually changes
        // For now, we'll let the caller handle this
        return map;
    }
    
    private Medicine mapToMedicine(String medicineId, Map<String, Object> data) {
        Medicine medicine = new Medicine();
        medicine.setMedicineId(medicineId);
        medicine.setMedicineName(getString(data, "medicine_name"));
        medicine.setDosage(getString(data, "dosage"));
        
        Object stockQty = data.get("stock_quantity");
        if (stockQty instanceof Long) {
            medicine.setStockQuantity(((Long) stockQty).intValue());
        } else if (stockQty instanceof Integer) {
            medicine.setStockQuantity((Integer) stockQty);
        }
        
        medicine.setUnit(getString(data, "unit"));
        medicine.setCategory(getString(data, "category"));
        medicine.setDescription(getString(data, "description"));
        medicine.setExpiryDate(getString(data, "expiry_date"));
        
        Object price = data.get("price");
        if (price instanceof Double) {
            medicine.setPrice((Double) price);
        } else if (price instanceof Long) {
            medicine.setPrice(((Long) price).doubleValue());
        }
        
        medicine.setSupplier(getString(data, "supplier"));
        return medicine;
    }
    
    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
    
    /* ─────────────────────────────────────────────
     * 🔹 CALLBACK INTERFACES
     * ───────────────────────────────────────────── */
    
    public interface AuthenticationCallback {
        void onResult(Employee employee);
    }
    
    public interface EmployeeCallback {
        void onResult(Employee employee);
    }
    
    public interface EmployeesCallback {
        void onResult(List<Employee> employees);
    }
    
    public interface PatientsCallback {
        void onResult(List<Patient> patients);
    }
    
    public interface PatientCallback {
        void onResult(Patient patient);
    }
    
    public interface PatientRegistrationListener {
        void onPatientRegistered(Patient patient);
    }
    
    public interface MedicinesCallback {
        void onResult(List<Medicine> medicines);
    }
    
    public interface PrescriptionsCallback {
        void onResult(List<Prescription> prescriptions);
    }
    
    public interface OperationCallback {
        void onResult(boolean success);
    }
    
    /**
     * Listen for real-time patient registrations (child added events)
     */
    public ChildEventListener listenForNewPatients(PatientRegistrationListener listener) {
        if (rootRef == null || listener == null) {
            return null;
        }
        
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                if (snapshot != null && snapshot.exists()) {
                    Map<String, Object> data = (Map<String, Object>) snapshot.getValue();
                    if (data != null) {
                        Patient patient = mapToPatient(snapshot.getKey(), data);
                        if (patient != null) {
                            listener.onPatientRegistered(patient);
                        }
                    }
                }
            }

            @Override public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(DataSnapshot snapshot) {}
            @Override public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Patient listener cancelled: " + error.getMessage());
            }
        };
        
        rootRef.child(PATH_PATIENTS).addChildEventListener(childEventListener);
        return childEventListener;
    }
    
    /**
     * Remove previously registered patient listener
     */
    public void removePatientListener(ChildEventListener listener) {
        if (rootRef != null && listener != null) {
            rootRef.child(PATH_PATIENTS).removeEventListener(listener);
        }
    }
    
    /**
     * Listen for real-time prescription additions (child added events) from history
     */
    public ChildEventListener listenForNewPrescriptions(PrescriptionRegistrationListener listener) {
        if (rootRef == null || listener == null) {
            return null;
        }
        
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                if (snapshot != null && snapshot.exists()) {
                    Map<String, Object> data = (Map<String, Object>) snapshot.getValue();
                    if (data != null) {
                        Prescription prescription = mapToPrescription(snapshot.getKey(), data);
                        if (prescription != null) {
                            listener.onPrescriptionAdded(prescription);
                        }
                    }
                }
            }

            @Override public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(DataSnapshot snapshot) {}
            @Override public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Prescription listener cancelled: " + error.getMessage());
            }
        };
        
        rootRef.child(PATH_HISTORY).addChildEventListener(childEventListener);
        return childEventListener;
    }
    
    /**
     * Remove previously registered prescription listener
     */
    public void removePrescriptionListener(ChildEventListener listener) {
        if (rootRef != null && listener != null) {
            rootRef.child(PATH_HISTORY).removeEventListener(listener);
        }
    }
    
    /**
     * Get root database reference for direct Firebase operations
     */
    public DatabaseReference getRootRef() {
        return rootRef;
    }
    
    /**
     * Interface for prescription registration listener
     */
    public interface PrescriptionRegistrationListener {
        void onPrescriptionAdded(Prescription prescription);
    }
}

