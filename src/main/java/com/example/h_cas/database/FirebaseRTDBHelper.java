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
                                usernameMatches = storedEmail.equalsIgnoreCase(username);
                            } else {
                                usernameMatches = storedUsername.equals(username);
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
                            
                            if (storedPassword.equals(password) && isActive) {
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
    public void getAllPatients(PatientsCallback callback) {
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
                                        patients.add(patient);
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
     */
    public void addPatient(Patient patient, OperationCallback callback) {
        if (rootRef == null || patient == null || patient.getPatientId() == null) {
            if (callback != null) callback.onResult(false);
            return;
        }
        
        Map<String, Object> patientData = patientToMap(patient);
        
        rootRef.child(PATH_PATIENTS).child(patient.getPatientId())
                .setValue(patientData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Patient added: " + patient.getPatientId());
                    if (callback != null) callback.onResult(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to add patient: " + patient.getPatientId(), e);
                    if (callback != null) callback.onResult(false);
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
        patient.setFullAddress(getString(data, "full_address"));
        patient.setPhoneNumber(getString(data, "phone_number"));
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
        return patient;
    }
    
    private Prescription mapToPrescription(String prescriptionId, Map<String, Object> data) {
        Prescription prescription = new Prescription();
        prescription.setPrescriptionId(prescriptionId);
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
        return prescription;
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
        map.put("created_date", patient.getCreatedDate());
        map.put("last_updated", System.currentTimeMillis());
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
}

