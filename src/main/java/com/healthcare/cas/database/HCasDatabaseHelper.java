package com.healthcare.cas.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.healthcare.cas.models.Employee;

import java.util.ArrayList;
import java.util.List;

/**
 * HCasDatabaseHelper manages the SQLite database for the H-CAS healthcare system.
 * Handles all database operations for employees, cases, and system data.
 */
public class HCasDatabaseHelper extends SQLiteOpenHelper {

    // Database information
    private static final String DATABASE_NAME = "hcas_healthcare.db";
    private static final int DATABASE_VERSION = 8; // Updated to add middle_name and date_of_birth columns

    // Employee table
    private static final String TABLE_EMPLOYEES = "employees";
    private static final String COLUMN_EMPLOYEE_ID = "employee_id";
    private static final String COLUMN_FIRST_NAME = "first_name";
    private static final String COLUMN_MIDDLE_NAME = "middle_name";
    private static final String COLUMN_LAST_NAME = "last_name";
    private static final String COLUMN_DATE_OF_BIRTH = "date_of_birth";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PHONE = "phone";
    private static final String COLUMN_ROLE = "role";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_CREATED_DATE = "created_date";
    private static final String COLUMN_IS_ACTIVE = "is_active";
    private static final String COLUMN_PROFILE_PICTURE_URL = "profile_picture_url";

    // Cases table
    private static final String TABLE_CASES = "healthcare_cases";
    private static final String COLUMN_CASE_ID = "case_id";
    private static final String COLUMN_PATIENT_NAME = "patient_name";
    private static final String COLUMN_CASE_TYPE = "case_type";
    private static final String COLUMN_PRIORITY = "priority";
    private static final String COLUMN_STATUS = "status";

    // Medicine inventory table constants
    private static final String TABLE_MEDICINES = "medicines";
    private static final String COLUMN_MEDICINE_ID = "medicine_id";
    private static final String COLUMN_MEDICINE_NAME = "medicine_name";
    private static final String COLUMN_MEDICINE_DOSAGE = "dosage";
    private static final String COLUMN_STOCK_QUANTITY = "stock_quantity";
    private static final String COLUMN_UNIT = "unit";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_EXPIRY_DATE = "expiry_date";
    private static final String COLUMN_PRICE = "price";
    private static final String COLUMN_SUPPLIER = "supplier";

    // RFID data table constants
    private static final String TABLE_RFID_DATA = "rfid_data";
    private static final String COLUMN_RFID_TAG_ID = "rfid_tag_id";
    private static final String COLUMN_IS_DISPENSED = "is_dispensed";
    private static final String COLUMN_DISPENSED_DATE = "dispensed_date";
    private static final String COLUMN_PHARMACIST_NAME = "pharmacist_name";
    private static final String COLUMN_ASSIGNED_EMPLOYEE_ID = "assigned_employee_id";
    private static final String COLUMN_CASE_DATE = "case_date";

    // Patients table
    private static final String TABLE_PATIENTS = "patients";
    private static final String COLUMN_PATIENT_ID = "patient_id";
    private static final String COLUMN_PATIENT_FIRST_NAME = "first_name";
    private static final String COLUMN_PATIENT_LAST_NAME = "last_name";
    private static final String COLUMN_PATIENT_DOB = "date_of_birth";
    private static final String COLUMN_PATIENT_GENDER = "gender";
    private static final String COLUMN_PATIENT_ADDRESS = "address";
    private static final String COLUMN_PATIENT_PHONE = "phone";
    private static final String COLUMN_PATIENT_EMAIL = "email";
    private static final String COLUMN_PATIENT_EMERGENCY_NAME = "emergency_contact_name";
    private static final String COLUMN_PATIENT_EMERGENCY_PHONE = "emergency_contact_phone";
    private static final String COLUMN_PATIENT_CREATED_DATE = "created_date";
    
    // Extended patient information columns
    private static final String COLUMN_PATIENT_SUFFIX = "suffix";
    private static final String COLUMN_PATIENT_FULL_NAME = "full_name";
    private static final String COLUMN_PATIENT_BIRTH_PLACE = "birth_place";
    private static final String COLUMN_PATIENT_AGE = "age";
    private static final String COLUMN_PATIENT_FULL_ADDRESS = "full_address";
    private static final String COLUMN_PATIENT_PHONE_NUMBER = "phone_number";
    private static final String COLUMN_PATIENT_ALLERGIES = "allergies";
    private static final String COLUMN_PATIENT_MEDICATIONS = "medications";
    private static final String COLUMN_PATIENT_MEDICAL_HISTORY = "medical_history";
    private static final String COLUMN_PATIENT_PULSE_RATE = "pulse_rate";
    private static final String COLUMN_PATIENT_BLOOD_PRESSURE = "blood_pressure";
    private static final String COLUMN_PATIENT_TEMPERATURE = "temperature";
    private static final String COLUMN_PATIENT_BLOOD_SUGAR = "blood_sugar";
    private static final String COLUMN_PATIENT_PAIN_SCALE = "pain_scale";
    private static final String COLUMN_PATIENT_NFC_UID = "nfc_uid";

    // Prescriptions table constants
    private static final String TABLE_PRESCRIPTIONS = "prescriptions";
    private static final String COLUMN_PRESCRIPTION_ID = "prescription_id";
    private static final String COLUMN1_PATIENT_ID = "patient_id";
    private static final String COLUMN1_PATIENT_NAME = "patient_name";
    private static final String COLUMN_MEDICATION = "medication";
    private static final String COLUMN_DOSAGE = "dosage";
    private static final String COLUMN_FREQUENCY = "frequency";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_INSTRUCTIONS = "instructions";
    private static final String COLUMN_DOCTOR_ID = "doctor_id";
    private static final String COLUMN_DOCTOR_NAME = "doctor_name";
    private static final String COLUMN1_CREATED_DATE = "created_date";
    private static final String COLUMN1_STATUS = "status";
    private static final String COLUMN_PATIENT_SYMPTOMS_DESCRIPTION = "symptoms_description";

    // Create table statements
    private static final String CREATE_EMPLOYEES_TABLE = 
        "CREATE TABLE " + TABLE_EMPLOYEES + " (" +
        COLUMN_EMPLOYEE_ID + " TEXT PRIMARY KEY, " +
        COLUMN_FIRST_NAME + " TEXT NOT NULL, " +
        COLUMN_MIDDLE_NAME + " TEXT, " +
        COLUMN_LAST_NAME + " TEXT NOT NULL, " +
        COLUMN_DATE_OF_BIRTH + " TEXT, " +
        COLUMN_EMAIL + " TEXT UNIQUE NOT NULL, " +
        COLUMN_PHONE + " TEXT NOT NULL, " +
        COLUMN_ROLE + " TEXT NOT NULL, " +
        COLUMN_USERNAME + " TEXT UNIQUE NOT NULL, " +
        COLUMN_PASSWORD + " TEXT NOT NULL, " +
        COLUMN_CREATED_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1, " +
        COLUMN_PROFILE_PICTURE_URL + " TEXT" +
        ")";

    private static final String CREATE_CASES_TABLE = 
        "CREATE TABLE " + TABLE_CASES + " (" +
        COLUMN_CASE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_PATIENT_NAME + " TEXT NOT NULL, " +
        COLUMN_CASE_TYPE + " TEXT NOT NULL, " +
        COLUMN_PRIORITY + " TEXT NOT NULL, " +
        COLUMN_STATUS + " TEXT NOT NULL, " +
        COLUMN_ASSIGNED_EMPLOYEE_ID + " TEXT, " +
        COLUMN_CASE_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        "FOREIGN KEY(" + COLUMN_ASSIGNED_EMPLOYEE_ID + ") REFERENCES " + 
        TABLE_EMPLOYEES + "(" + COLUMN_EMPLOYEE_ID + ")" +
        ")";

    private static final String CREATE_PATIENTS_TABLE =
        "CREATE TABLE " + TABLE_PATIENTS + " (" +
        COLUMN_PATIENT_ID + " TEXT PRIMARY KEY, " +
        COLUMN_PATIENT_FIRST_NAME + " TEXT NOT NULL, " +
        COLUMN_PATIENT_LAST_NAME + " TEXT NOT NULL, " +
        COLUMN_PATIENT_DOB + " TEXT, " +
        COLUMN_PATIENT_GENDER + " TEXT, " +
        COLUMN_PATIENT_ADDRESS + " TEXT, " +
        COLUMN_PATIENT_PHONE + " TEXT, " +
        COLUMN_PATIENT_EMAIL + " TEXT, " +
        COLUMN_PATIENT_EMERGENCY_NAME + " TEXT, " +
        COLUMN_PATIENT_EMERGENCY_PHONE + " TEXT, " +
        COLUMN_PATIENT_CREATED_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_PATIENT_SUFFIX + " TEXT, " +
        COLUMN_PATIENT_FULL_NAME + " TEXT, " +
        COLUMN_PATIENT_BIRTH_PLACE + " TEXT, " +
        COLUMN_PATIENT_AGE + " TEXT, " +
        COLUMN_PATIENT_FULL_ADDRESS + " TEXT, " +
        COLUMN_PATIENT_PHONE_NUMBER + " TEXT, " +
        COLUMN_PATIENT_ALLERGIES + " TEXT, " +
        COLUMN_PATIENT_MEDICATIONS + " TEXT, " +
        COLUMN_PATIENT_MEDICAL_HISTORY + " TEXT, " +
        COLUMN_PATIENT_PULSE_RATE + " TEXT, " +
        COLUMN_PATIENT_BLOOD_PRESSURE + " TEXT, " +
        COLUMN_PATIENT_TEMPERATURE + " TEXT, " +
        COLUMN_PATIENT_BLOOD_SUGAR + " TEXT, " +
        COLUMN_PATIENT_PAIN_SCALE + " TEXT, " +
        COLUMN_PATIENT_SYMPTOMS_DESCRIPTION + " TEXT, " +
        COLUMN_PATIENT_NFC_UID + " TEXT" +
        ")";

    private static final String CREATE_PRESCRIPTIONS_TABLE =
        "CREATE TABLE " + TABLE_PRESCRIPTIONS + " (" +
        COLUMN_PRESCRIPTION_ID + " TEXT PRIMARY KEY, " +
        COLUMN_PATIENT_ID + " TEXT NOT NULL, " +
        COLUMN_PATIENT_NAME + " TEXT NOT NULL, " +
        COLUMN_MEDICATION + " TEXT NOT NULL, " +
        COLUMN_DOSAGE + " TEXT NOT NULL, " +
        COLUMN_FREQUENCY + " TEXT NOT NULL, " +
        COLUMN_DURATION + " TEXT NOT NULL, " +
        COLUMN_INSTRUCTIONS + " TEXT, " +
        COLUMN_DOCTOR_ID + " TEXT NOT NULL, " +
        COLUMN_DOCTOR_NAME + " TEXT NOT NULL, " +
        COLUMN_CREATED_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_STATUS + " TEXT DEFAULT 'Active'" +
        ")";

    private static final String CREATE_MEDICINES_TABLE =
        "CREATE TABLE " + TABLE_MEDICINES + " (" +
        COLUMN_MEDICINE_ID + " TEXT PRIMARY KEY, " +
        COLUMN_MEDICINE_NAME + " TEXT NOT NULL, " +
        COLUMN_MEDICINE_DOSAGE + " TEXT, " +
        COLUMN_STOCK_QUANTITY + " INTEGER DEFAULT 0, " +
        COLUMN_UNIT + " TEXT, " +
        COLUMN_CATEGORY + " TEXT, " +
        COLUMN_DESCRIPTION + " TEXT, " +
        COLUMN_EXPIRY_DATE + " TEXT, " +
        COLUMN_PRICE + " REAL DEFAULT 0.0, " +
        COLUMN_SUPPLIER + " TEXT" +
        ")";

    private static final String CREATE_RFID_DATA_TABLE =
        "CREATE TABLE " + TABLE_RFID_DATA + " (" +
        COLUMN_RFID_TAG_ID + " TEXT PRIMARY KEY, " +
        COLUMN_PATIENT_ID + " TEXT NOT NULL, " +
        COLUMN_PATIENT_NAME + " TEXT NOT NULL, " +
        COLUMN_PRESCRIPTION_ID + " TEXT NOT NULL, " +
        COLUMN_MEDICATION + " TEXT NOT NULL, " +
        COLUMN_DOSAGE + " TEXT NOT NULL, " +
        COLUMN_FREQUENCY + " TEXT NOT NULL, " +
        COLUMN_DURATION + " TEXT NOT NULL, " +
        COLUMN_INSTRUCTIONS + " TEXT, " +
        COLUMN_DOCTOR_NAME + " TEXT NOT NULL, " +
        COLUMN_CREATED_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_IS_DISPENSED + " INTEGER DEFAULT 0, " +
        COLUMN_DISPENSED_DATE + " TEXT, " +
        COLUMN_PHARMACIST_NAME + " TEXT" +
        ")";

    private Context context;
    private static FirebaseSyncManager syncManager;
    private static boolean syncManagerInitializationAttempted = false;
    
    public HCasDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        
        // Initialize sync manager only once (prevent circular dependency)
        // Firebase sync is optional - app works fine without it
        if (syncManager == null && !syncManagerInitializationAttempted && context != null) {
            syncManagerInitializationAttempted = true;
            
            try {
                // Check if Firebase is available before initializing
                try {
                    com.google.firebase.FirebaseApp.getInstance();
                    // Initialize sync manager in background thread to avoid blocking
                    new Thread(() -> {
                        try {
                            syncManager = new FirebaseSyncManager(context);
                            if (syncManager != null) {
                                Log.d("HCasDatabaseHelper", "FirebaseSyncManager initialized");
                            }
                        } catch (Exception e) {
                            Log.e("HCasDatabaseHelper", "Failed to initialize FirebaseSyncManager", e);
                            syncManager = null;
                        }
                    }).start();
                } catch (IllegalStateException e) {
                    // Firebase not available - skip sync initialization
                    Log.w("HCasDatabaseHelper", "Firebase not available - sync disabled", e);
                    syncManager = null;
                } catch (Exception e) {
                    Log.e("HCasDatabaseHelper", "Failed to check Firebase availability", e);
                    syncManager = null;
                }
            } catch (Exception e) {
                Log.e("HCasDatabaseHelper", "Error checking Firebase availability", e);
                syncManager = null;
            }
        }
    }
    
    /**
     * Helper method to sync to Firebase (non-blocking)
     */
    private void syncToFirebase(String type, Object data) {
        if (syncManager == null) {
            // Try to initialize sync manager if not already initialized
            if (context != null) {
                try {
                    syncManager = new FirebaseSyncManager(context);
                } catch (Exception e) {
                    Log.e("HCasDatabaseHelper", "Failed to initialize syncManager", e);
                    return;
                }
            } else {
                return;
            }
        }
        
        if (context == null) {
            return;
        }
        
        try {
            // Run sync in background thread to avoid blocking
            new Thread(() -> {
                try {
                    if (data instanceof com.healthcare.cas.models.Medicine) {
                        syncManager.syncMedicine((com.healthcare.cas.models.Medicine) data);
                    } else if (data instanceof com.healthcare.cas.models.Prescription) {
                        syncManager.syncPrescription((com.healthcare.cas.models.Prescription) data);
                    } else if (data instanceof com.healthcare.cas.models.Patient) {
                        syncManager.syncPatient((com.healthcare.cas.models.Patient) data);
                    } else if (data instanceof Employee) {
                        syncManager.syncEmployee((Employee) data);
                    }
                } catch (Exception e) {
                    Log.e("HCasDatabaseHelper", "Firebase sync failed for " + type, e);
                }
            }).start();
        } catch (Exception e) {
            Log.e("HCasDatabaseHelper", "Error initiating Firebase sync", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tables
        db.execSQL(CREATE_EMPLOYEES_TABLE);
        db.execSQL(CREATE_CASES_TABLE);
        db.execSQL(CREATE_PATIENTS_TABLE);
        db.execSQL(CREATE_PRESCRIPTIONS_TABLE);
        db.execSQL(CREATE_MEDICINES_TABLE);
        db.execSQL(CREATE_RFID_DATA_TABLE);
        
        // Insert default admin account
        insertDefaultAdmin(db);
        
        // Insert sample medicines
        insertSampleMedicines(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("HCasDatabaseHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);
        
        // Handle upgrades from different versions
        if (oldVersion < newVersion) {
            // Drop existing tables
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CASES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMPLOYEES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PATIENTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRESCRIPTIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICINES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RFID_DATA);
            
            // Recreate tables
            onCreate(db);
            Log.d("HCasDatabaseHelper", "Database upgraded successfully");
        }
    }
    
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("HCasDatabaseHelper", "Downgrading database from version " + oldVersion + " to " + newVersion);
        Log.w("HCasDatabaseHelper", "⚠️ WARNING: Database downgrade detected. This may cause data loss.");
        
        // Handle downgrade by recreating tables (data will be lost)
        // This is necessary to prevent the "Can't downgrade database" error
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CASES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMPLOYEES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PATIENTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRESCRIPTIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICINES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RFID_DATA);
            
            // Recreate tables with new version
            onCreate(db);
            Log.d("HCasDatabaseHelper", "Database downgraded and recreated successfully");
        } catch (Exception e) {
            Log.e("HCasDatabaseHelper", "Error during database downgrade", e);
            throw e; // Re-throw to let SQLite handle it
        }
    }

    /**
     * Insert default admin account
     */
    private void insertDefaultAdmin(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMPLOYEE_ID, "ADMIN001");
        values.put(COLUMN_FIRST_NAME, "System");
        values.put(COLUMN_LAST_NAME, "Administrator");
        values.put(COLUMN_EMAIL, "admin@hcas.com");
        values.put(COLUMN_PHONE, "0000000000");
        values.put(COLUMN_ROLE, "Administrator");
        values.put(COLUMN_USERNAME, "admin");
        values.put(COLUMN_PASSWORD, "admin123");
        values.put(COLUMN_IS_ACTIVE, 1);
        
        db.insert(TABLE_EMPLOYEES, null, values);
        
        // Insert test staff accounts for development/testing
        insertTestStaffAccounts(db);
    }
    
    /**
     * Insert test staff accounts for development and testing
     */
    private void insertTestStaffAccounts(SQLiteDatabase db) {
        // Test Doctor
        ContentValues doctorValues = new ContentValues();
        doctorValues.put(COLUMN_EMPLOYEE_ID, "DOC001");
        doctorValues.put(COLUMN_FIRST_NAME, "Dr. John");
        doctorValues.put(COLUMN_LAST_NAME, "Smith");
        doctorValues.put(COLUMN_EMAIL, "john.smith@hcas.com");
        doctorValues.put(COLUMN_PHONE, "1234567890");
        doctorValues.put(COLUMN_ROLE, "Doctor");
        doctorValues.put(COLUMN_USERNAME, "doctor");
        doctorValues.put(COLUMN_PASSWORD, "doctor123");
        doctorValues.put(COLUMN_IS_ACTIVE, 1);
        db.insert(TABLE_EMPLOYEES, null, doctorValues);
        
        // Test Nurse
        ContentValues nurseValues = new ContentValues();
        nurseValues.put(COLUMN_EMPLOYEE_ID, "NUR001");
        nurseValues.put(COLUMN_FIRST_NAME, "Jane");
        nurseValues.put(COLUMN_LAST_NAME, "Doe");
        nurseValues.put(COLUMN_EMAIL, "jane.doe@hcas.com");
        nurseValues.put(COLUMN_PHONE, "0987654321");
        nurseValues.put(COLUMN_ROLE, "Nurse");
        nurseValues.put(COLUMN_USERNAME, "nurse");
        nurseValues.put(COLUMN_PASSWORD, "nurse123");
        nurseValues.put(COLUMN_IS_ACTIVE, 1);
        db.insert(TABLE_EMPLOYEES, null, nurseValues);
        
        // Test Pharmacist
        ContentValues pharmacistValues = new ContentValues();
        pharmacistValues.put(COLUMN_EMPLOYEE_ID, "PHA001");
        pharmacistValues.put(COLUMN_FIRST_NAME, "Mike");
        pharmacistValues.put(COLUMN_LAST_NAME, "Johnson");
        pharmacistValues.put(COLUMN_EMAIL, "mike.johnson@hcas.com");
        pharmacistValues.put(COLUMN_PHONE, "1122334455");
        pharmacistValues.put(COLUMN_ROLE, "Pharmacist");
        pharmacistValues.put(COLUMN_USERNAME, "pharmacist");
        pharmacistValues.put(COLUMN_PASSWORD, "pharmacist123");
        pharmacistValues.put(COLUMN_IS_ACTIVE, 1);
        db.insert(TABLE_EMPLOYEES, null, pharmacistValues);
    }

    // Employee operations

    /**
     * Add a new employee to the database
     * CRITICAL: This method prevents deleted employees from being restored
     * Since we use HARD DELETE, deleted employees won't exist in SQLite
     * If an employee with the same ID exists, it means it wasn't deleted - allow update instead
     */
    public boolean addEmployee(Employee employee) {
        if (employee == null || employee.getEmployeeId() == null) {
            return false;
        }
        
        SQLiteDatabase db = this.getWritableDatabase();
        
        // CRITICAL PROTECTION #1: Check if employee already exists
        // If it exists, it means it wasn't deleted - but we should use updateEmployee instead
        Employee existing = getEmployeeById(employee.getEmployeeId());
        if (existing != null) {
            // Employee exists - this means it wasn't deleted (hard delete removes records)
            // If it's inactive, don't restore it
            if (!existing.isActive()) {
                Log.d("HCasDatabaseHelper", "⚠️ Employee " + employee.getEmployeeId() + " exists but is inactive - NOT restoring via addEmployee");
                Log.d("HCasDatabaseHelper", "   → Deleted employees should stay deleted and never be restored");
                return false; // Don't add/restore deleted employees
            }
            // If it exists and is active, this is an update scenario - should use updateEmployee
            Log.d("HCasDatabaseHelper", "⚠️ Employee " + employee.getEmployeeId() + " already exists - use updateEmployee instead");
            return false; // Don't add existing employees
        }
        
        // CRITICAL PROTECTION #2: Don't add employees with is_active=false
        // This prevents adding employees that are marked as deleted in Firebase
        if (!employee.isActive()) {
            Log.d("HCasDatabaseHelper", "⚠️ Incoming employee " + employee.getEmployeeId() + " has is_active=false - NOT adding");
            Log.d("HCasDatabaseHelper", "   → Employee is marked as deleted - will NOT be added");
            return false; // Don't add deleted employees
        }
        
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_EMPLOYEE_ID, employee.getEmployeeId());
        values.put(COLUMN_FIRST_NAME, employee.getFirstName());
        values.put(COLUMN_MIDDLE_NAME, employee.getMiddleName());
        values.put(COLUMN_LAST_NAME, employee.getLastName());
        values.put(COLUMN_DATE_OF_BIRTH, employee.getDateOfBirth());
        values.put(COLUMN_EMAIL, employee.getEmail());
        values.put(COLUMN_PHONE, employee.getPhone());
        values.put(COLUMN_ROLE, employee.getRole());
        values.put(COLUMN_USERNAME, employee.getUsername());
        values.put(COLUMN_PASSWORD, employee.getPassword());
        values.put(COLUMN_IS_ACTIVE, employee.isActive() ? 1 : 0);
        
        long result = db.insert(TABLE_EMPLOYEES, null, values);
        // Don't close database - reuse connection
        
        // Invalidate cache when employee is added
        if (result != -1) {
            invalidateEmployeeCache();
        }
        
        return result != -1;
    }

    // Cache for frequently accessed data
    private List<Employee> cachedEmployees;
    private long employeesCacheTime = 0;
    private static final long CACHE_DURATION = 30000; // 30 seconds

    /**
     * Get all employees (excluding administrators)
     * Uses caching to avoid repeated database queries
     */
    public List<Employee> getAllEmployees() {
        // Return cached data if still valid
        if (cachedEmployees != null && (System.currentTimeMillis() - employeesCacheTime) < CACHE_DURATION) {
            return new ArrayList<>(cachedEmployees);
        }

        List<Employee> employees = new ArrayList<>();
        // Only select needed columns for better performance
        String query = "SELECT " + COLUMN_EMPLOYEE_ID + ", " + COLUMN_FIRST_NAME + ", " + COLUMN_LAST_NAME + ", " +
                      COLUMN_EMAIL + ", " + COLUMN_PHONE + ", " + COLUMN_ROLE + ", " + COLUMN_USERNAME + ", " +
                      COLUMN_IS_ACTIVE + ", " + COLUMN_PROFILE_PICTURE_URL + 
                      " FROM " + TABLE_EMPLOYEES + 
                      " WHERE " + COLUMN_IS_ACTIVE + " = 1 AND " + COLUMN_ROLE + " != 'Administrator'";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            
            if (cursor.moveToFirst()) {
                do {
                    Employee employee = new Employee();
                    employee.setEmployeeId(cursor.getString(0));
                    employee.setFirstName(cursor.getString(1));
                    employee.setLastName(cursor.getString(2));
                    employee.setEmail(cursor.getString(3));
                    employee.setPhone(cursor.getString(4));
                    employee.setRole(cursor.getString(5));
                    employee.setUsername(cursor.getString(6));
                    // Don't load password for security and performance
                    employee.setActive(cursor.getInt(7) == 1);
                    // Handle profile picture URL
                    if (cursor.getColumnCount() > 8 && !cursor.isNull(8)) {
                        employee.setProfilePictureUrl(cursor.getString(8));
                    }
                    
                    employees.add(employee);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }

        // Update cache
        cachedEmployees = new ArrayList<>(employees);
        employeesCacheTime = System.currentTimeMillis();
        
        return employees;
    }
    
    /**
     * Invalidate employee cache (call this when employees are added/updated/deleted)
     */
    public void invalidateEmployeeCache() {
        cachedEmployees = null;
        employeesCacheTime = 0;
    }

    /**
     * Get employees by role (excluding administrators)
     */
    public List<Employee> getEmployeesByRole(String role) {
        List<Employee> employees = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_EMPLOYEES + 
                      " WHERE " + COLUMN_ROLE + " = ? AND " + COLUMN_IS_ACTIVE + " = 1 AND " + COLUMN_ROLE + " != 'Administrator'";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{role});
        
        if (cursor.moveToFirst()) {
            do {
                Employee employee = new Employee();
                employee.setEmployeeId(cursor.getString(cursor.getColumnIndex(COLUMN_EMPLOYEE_ID)));
                employee.setFirstName(cursor.getString(cursor.getColumnIndex(COLUMN_FIRST_NAME)));
                int middleNameIndex = cursor.getColumnIndex(COLUMN_MIDDLE_NAME);
                if (middleNameIndex >= 0 && !cursor.isNull(middleNameIndex)) {
                    employee.setMiddleName(cursor.getString(middleNameIndex));
                }
                employee.setLastName(cursor.getString(cursor.getColumnIndex(COLUMN_LAST_NAME)));
                int dobIndex = cursor.getColumnIndex(COLUMN_DATE_OF_BIRTH);
                if (dobIndex >= 0 && !cursor.isNull(dobIndex)) {
                    employee.setDateOfBirth(cursor.getString(dobIndex));
                }
                employee.setEmail(cursor.getString(cursor.getColumnIndex(COLUMN_EMAIL)));
                employee.setPhone(cursor.getString(cursor.getColumnIndex(COLUMN_PHONE)));
                employee.setRole(cursor.getString(cursor.getColumnIndex(COLUMN_ROLE)));
                employee.setUsername(cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME)));
                employee.setPassword(cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD)));
                int createdDateIndex = cursor.getColumnIndex(COLUMN_CREATED_DATE);
                if (createdDateIndex >= 0 && !cursor.isNull(createdDateIndex)) {
                    employee.setCreatedDate(cursor.getString(createdDateIndex));
                }
                employee.setActive(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_ACTIVE)) == 1);
                int profilePicIndex = cursor.getColumnIndex(COLUMN_PROFILE_PICTURE_URL);
                if (profilePicIndex >= 0 && !cursor.isNull(profilePicIndex)) {
                    employee.setProfilePictureUrl(cursor.getString(profilePicIndex));
                }
                
                employees.add(employee);
            } while (cursor.moveToNext());
        }
        
        if (cursor != null) {
            cursor.close();
        }
        // Don't close database - reuse connection
        return employees;
    }

    /**
     * Authenticate user login
     * Supports both username and email authentication
     * Only returns active employees (isActive = 1) - deleted employees cannot login
     */
    public Employee authenticateUser(String username, String password) {
        System.out.println("DEBUG: Database authenticateUser called with username: " + username);
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Employee employee = null;
        
        // Check if input is an email (contains @)
        boolean isEmail = username != null && username.contains("@");
        
        String query;
        if (isEmail) {
            // Authenticate by email - only active employees (isActive = 1)
            query = "SELECT * FROM " + TABLE_EMPLOYEES + 
                   " WHERE " + COLUMN_EMAIL + " = ? AND " + COLUMN_PASSWORD + " = ? AND " + COLUMN_IS_ACTIVE + " = 1";
            System.out.println("DEBUG: Authenticating by email: " + username);
        } else {
            // Authenticate by username - only active employees (isActive = 1)
            query = "SELECT * FROM " + TABLE_EMPLOYEES + 
                   " WHERE " + COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ? AND " + COLUMN_IS_ACTIVE + " = 1";
            System.out.println("DEBUG: Authenticating by username: " + username);
        }
        
        System.out.println("DEBUG: Query: " + query);
        System.out.println("DEBUG: Username/Email: " + username + ", Password: " + password);
        
        cursor = db.rawQuery(query, new String[]{username, password});
        
        System.out.println("DEBUG: Cursor count: " + cursor.getCount());
        
        if (cursor.moveToFirst()) {
            System.out.println("DEBUG: Found employee in database");
            employee = new Employee();
            employee.setEmployeeId(cursor.getString(cursor.getColumnIndex(COLUMN_EMPLOYEE_ID)));
            employee.setFirstName(cursor.getString(cursor.getColumnIndex(COLUMN_FIRST_NAME)));
            int middleNameIndex = cursor.getColumnIndex(COLUMN_MIDDLE_NAME);
            if (middleNameIndex >= 0 && !cursor.isNull(middleNameIndex)) {
                employee.setMiddleName(cursor.getString(middleNameIndex));
            }
            employee.setLastName(cursor.getString(cursor.getColumnIndex(COLUMN_LAST_NAME)));
            int dobIndex = cursor.getColumnIndex(COLUMN_DATE_OF_BIRTH);
            if (dobIndex >= 0 && !cursor.isNull(dobIndex)) {
                employee.setDateOfBirth(cursor.getString(dobIndex));
            }
            employee.setEmail(cursor.getString(cursor.getColumnIndex(COLUMN_EMAIL)));
            employee.setPhone(cursor.getString(cursor.getColumnIndex(COLUMN_PHONE)));
            employee.setRole(cursor.getString(cursor.getColumnIndex(COLUMN_ROLE)));
            employee.setUsername(cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME)));
            employee.setPassword(cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD)));
            int createdDateIndex = cursor.getColumnIndex(COLUMN_CREATED_DATE);
            if (createdDateIndex >= 0 && !cursor.isNull(createdDateIndex)) {
                employee.setCreatedDate(cursor.getString(createdDateIndex));
            }
            employee.setActive(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_ACTIVE)) == 1);
            int profilePicIndex = cursor.getColumnIndex(COLUMN_PROFILE_PICTURE_URL);
            if (profilePicIndex >= 0 && !cursor.isNull(profilePicIndex)) {
                employee.setProfilePictureUrl(cursor.getString(profilePicIndex));
            }
            
            System.out.println("DEBUG: Employee role: " + employee.getRole());
            System.out.println("DEBUG: Employee active: " + employee.isActive());
        } else {
            System.out.println("DEBUG: No employee found with these credentials");
        }
        
        if (cursor != null) {
            cursor.close();
        }
        // Don't close database - reuse connection
        return employee;
    }

    /**
     * Debug method to check if employees exist in database
     */
    public void debugCheckEmployees() {
        System.out.println("DEBUG: Checking employees in database...");
        
        String query = "SELECT * FROM " + TABLE_EMPLOYEES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            
            System.out.println("DEBUG: Total employees in database: " + cursor.getCount());
            
            if (cursor.moveToFirst()) {
                do {
                    System.out.println("DEBUG: Employee - ID: " + cursor.getString(0) + 
                                     ", Username: " + cursor.getString(6) + 
                                     ", Password: " + cursor.getString(7) + 
                                     ", Role: " + cursor.getString(5) + 
                                     ", Active: " + cursor.getInt(9));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection for better performance
        }
    }

    /**
     * Validate employee login credentials
     */
    public boolean validateEmployeeLogin(String username, String password) {
        String query = "SELECT COUNT(*) FROM " + TABLE_EMPLOYEES + 
                      " WHERE " + COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ? AND " + COLUMN_IS_ACTIVE + " = 1";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{username, password});
            
            boolean isValid = false;
            if (cursor.moveToFirst()) {
                isValid = cursor.getInt(0) > 0;
            }
            return isValid;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection for better performance
        }
    }

    /**
     * Check if username already exists
     */
    public boolean isUsernameExists(String username) {
        String query = "SELECT COUNT(*) FROM " + TABLE_EMPLOYEES + " WHERE " + COLUMN_USERNAME + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{username});
            
            boolean exists = false;
            if (cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }
            return exists;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection for better performance
        }
    }

    /**
     * Check if email already exists
     */
    public boolean isEmailExists(String email) {
        String query = "SELECT COUNT(*) FROM " + TABLE_EMPLOYEES + " WHERE " + COLUMN_EMAIL + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{email});
            
            boolean exists = false;
            if (cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }
            return exists;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection for better performance
        }
    }

    /**
     * Check if an employee with the same first name, last name, and date of birth already exists
     * This prevents duplicate employee entries
     */
    public boolean isDuplicateEmployee(String firstName, String lastName, String dateOfBirth) {
        if (firstName == null || lastName == null || dateOfBirth == null) {
            return false;
        }
        
        String query = "SELECT COUNT(*) FROM " + TABLE_EMPLOYEES + 
                      " WHERE LOWER(TRIM(" + COLUMN_FIRST_NAME + ")) = LOWER(TRIM(?))" +
                      " AND LOWER(TRIM(" + COLUMN_LAST_NAME + ")) = LOWER(TRIM(?))" +
                      " AND " + COLUMN_DATE_OF_BIRTH + " = ?";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{
                firstName.trim(), 
                lastName.trim(), 
                dateOfBirth.trim()
            });
            
            boolean exists = false;
            if (cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }
            return exists;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection for better performance
        }
    }

    /**
     * Get total count of employees (excluding administrators) - optimized
     */
    public int getTotalEmployeesCount() {
        String query = "SELECT COUNT(*) FROM " + TABLE_EMPLOYEES + " WHERE " + COLUMN_IS_ACTIVE + " = 1 AND " + COLUMN_ROLE + " != 'Administrator'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            
            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            return count;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
    }

    /**
     * Get count of employees by role (excluding administrators) - optimized
     */
    public int getEmployeesCountByRole(String role) {
        String query = "SELECT COUNT(*) FROM " + TABLE_EMPLOYEES + 
                      " WHERE " + COLUMN_ROLE + " = ? AND " + COLUMN_IS_ACTIVE + " = 1 AND " + COLUMN_ROLE + " != 'Administrator'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{role});
            
            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            return count;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
    }

    /**
     * Generate next available employee ID based on role
     * Format: NUR001, DOC001, PHA001, etc.
     * @param role The role of the employee (Nurse, Doctor, Pharmacist)
     * @return The next available employee ID for the given role
     */
    public String generateNextEmployeeId(String role) {
        String prefix = "";
        
        // Determine prefix based on role
        switch (role) {
            case "Nurse":
                prefix = "NUR";
                break;
            case "Doctor":
                prefix = "DOC";
                break;
            case "Pharmacist":
                prefix = "PHA";
                break;
            default:
                // Fallback: use first 3 letters of role in uppercase
                prefix = role.length() >= 3 ? role.substring(0, 3).toUpperCase() : role.toUpperCase();
                break;
        }
        
        // Find the highest existing employee ID for this role prefix
        String query = "SELECT " + COLUMN_EMPLOYEE_ID + " FROM " + TABLE_EMPLOYEES + 
                       " WHERE " + COLUMN_EMPLOYEE_ID + " LIKE ?";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int nextNumber = 1;
        
        try {
            cursor = db.rawQuery(query, new String[]{prefix + "%"});
            
            // Find the highest number by iterating through all matching IDs
            while (cursor.moveToNext()) {
                String employeeId = cursor.getString(0);
                if (employeeId != null && employeeId.length() >= 4 && employeeId.startsWith(prefix)) {
                    try {
                        // Extract the number part (after the 3-letter prefix)
                        String numberPart = employeeId.substring(3);
                        int currentNumber = Integer.parseInt(numberPart);
                        if (currentNumber >= nextNumber) {
                            nextNumber = currentNumber + 1;
                        }
                    } catch (NumberFormatException e) {
                        // If parsing fails, skip this ID
                        continue;
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
        
        // Format: PREFIX + 3-digit number (e.g., NUR001, DOC002, PHA003)
        return String.format("%s%03d", prefix, nextNumber);
    }

    /**
     * Get today's cases count - returns total number of patients (total cases) - optimized
     */
    public int getTodaysCasesCount() {
        // Use optimized count query instead of loading all patients
        return getTotalPatientsCount();
    }

    /**
     * Get pending reviews count - returns count of patients who don't have prescriptions yet
     * Optimized query using LEFT JOIN for better performance
     */
    public int getPendingReviewsCount() {
        // Optimized query using LEFT JOIN instead of NOT IN (much faster)
        String query = "SELECT COUNT(*) FROM " + TABLE_PATIENTS + " p " +
                      "LEFT JOIN " + TABLE_PRESCRIPTIONS + " pr ON p." + COLUMN_PATIENT_ID + " = pr." + COLUMN_PATIENT_ID +
                      " WHERE pr." + COLUMN_PATIENT_ID + " IS NULL";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            
            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            return count;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
    }
    
    /**
     * Get patients without prescriptions (optimized single query)
     */
    public List<com.healthcare.cas.models.Patient> getPatientsWithoutPrescriptions() {
        List<com.healthcare.cas.models.Patient> patients = new ArrayList<>();
        
        // Optimized query: Get patients who don't have prescriptions in a single query
        String query = "SELECT p.* FROM " + TABLE_PATIENTS + " p " +
                      "LEFT JOIN " + TABLE_PRESCRIPTIONS + " pr ON p." + COLUMN_PATIENT_ID + " = pr." + COLUMN_PATIENT_ID +
                      " WHERE pr." + COLUMN_PATIENT_ID + " IS NULL";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            
            if (cursor.moveToFirst()) {
                do {
                    com.healthcare.cas.models.Patient patient = new com.healthcare.cas.models.Patient();
                    patient.setPatientId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATIENT_ID)));
                    patient.setFirstName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATIENT_FIRST_NAME)));
                    patient.setLastName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATIENT_LAST_NAME)));
                    patient.setDateOfBirth(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATIENT_DOB)));
                    patient.setGender(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATIENT_GENDER)));
                    patient.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATIENT_ADDRESS)));
                    patient.setPhone(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATIENT_PHONE)));
                    patient.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATIENT_EMAIL)));
                    patient.setEmergencyContactName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATIENT_EMERGENCY_NAME)));
                    patient.setEmergencyContactPhone(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATIENT_EMERGENCY_PHONE)));
                    
                    // Extended fields
                    if (cursor.getColumnCount() > 10) {
                        int suffixIdx = cursor.getColumnIndex(COLUMN_PATIENT_SUFFIX);
                        int fullNameIdx = cursor.getColumnIndex(COLUMN_PATIENT_FULL_NAME);
                        int birthPlaceIdx = cursor.getColumnIndex(COLUMN_PATIENT_BIRTH_PLACE);
                        int ageIdx = cursor.getColumnIndex(COLUMN_PATIENT_AGE);
                        int fullAddressIdx = cursor.getColumnIndex(COLUMN_PATIENT_FULL_ADDRESS);
                        int phoneNumberIdx = cursor.getColumnIndex(COLUMN_PATIENT_PHONE_NUMBER);
                        int allergiesIdx = cursor.getColumnIndex(COLUMN_PATIENT_ALLERGIES);
                        int medicationsIdx = cursor.getColumnIndex(COLUMN_PATIENT_MEDICATIONS);
                        int medicalHistoryIdx = cursor.getColumnIndex(COLUMN_PATIENT_MEDICAL_HISTORY);
                        int pulseRateIdx = cursor.getColumnIndex(COLUMN_PATIENT_PULSE_RATE);
                        int bloodPressureIdx = cursor.getColumnIndex(COLUMN_PATIENT_BLOOD_PRESSURE);
                        int temperatureIdx = cursor.getColumnIndex(COLUMN_PATIENT_TEMPERATURE);
                        int bloodSugarIdx = cursor.getColumnIndex(COLUMN_PATIENT_BLOOD_SUGAR);
                        int painScaleIdx = cursor.getColumnIndex(COLUMN_PATIENT_PAIN_SCALE);
                        int symptomsIdx = cursor.getColumnIndex(COLUMN_PATIENT_SYMPTOMS_DESCRIPTION);
                        
                        if (suffixIdx >= 0) patient.setSuffix(cursor.getString(suffixIdx));
                        if (fullNameIdx >= 0) patient.setFullName(cursor.getString(fullNameIdx));
                        if (birthPlaceIdx >= 0) patient.setBirthPlace(cursor.getString(birthPlaceIdx));
                        if (ageIdx >= 0) patient.setAge(cursor.getString(ageIdx));
                        if (fullAddressIdx >= 0) patient.setFullAddress(cursor.getString(fullAddressIdx));
                        if (phoneNumberIdx >= 0) patient.setPhoneNumber(cursor.getString(phoneNumberIdx));
                        if (allergiesIdx >= 0) patient.setAllergies(cursor.getString(allergiesIdx));
                        if (medicationsIdx >= 0) patient.setMedications(cursor.getString(medicationsIdx));
                        if (medicalHistoryIdx >= 0) patient.setMedicalHistory(cursor.getString(medicalHistoryIdx));
                        if (pulseRateIdx >= 0) patient.setPulseRate(cursor.getString(pulseRateIdx));
                        if (bloodPressureIdx >= 0) patient.setBloodPressure(cursor.getString(bloodPressureIdx));
                        if (temperatureIdx >= 0) patient.setTemperature(cursor.getString(temperatureIdx));
                        if (bloodSugarIdx >= 0) patient.setBloodSugar(cursor.getString(bloodSugarIdx));
                        if (painScaleIdx >= 0) patient.setPainScale(cursor.getString(painScaleIdx));
                        if (symptomsIdx >= 0) patient.setSymptomsDescription(cursor.getString(symptomsIdx));
                    }
                    
                    patients.add(patient);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
        
        return patients;
    }

    /**
     * Delete employee (HARD DELETE - permanently removes from database)
     * This completely removes the employee record from SQLite database
     */
    public boolean deleteEmployee(String employeeId) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // HARD DELETE: Actually remove the employee record from the database
        int result = db.delete(TABLE_EMPLOYEES, COLUMN_EMPLOYEE_ID + " = ?", new String[]{employeeId});
        // Don't close database - reuse connection
        
        // Invalidate cache when employee is deleted
        if (result > 0) {
            invalidateEmployeeCache();
            Log.d("HCasDatabaseHelper", "✅ Employee permanently deleted from SQLite: " + employeeId);
        } else {
            Log.w("HCasDatabaseHelper", "⚠️ Failed to delete employee from SQLite: " + employeeId + " (employee may not exist)");
        }
        
        return result > 0;
    }

    /**
     * Get employee by username (including admin)
     */
    public Employee getEmployeeByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_EMPLOYEES + " WHERE " + COLUMN_USERNAME + " = ? AND " + COLUMN_IS_ACTIVE + " = 1";
        
        Cursor cursor = db.rawQuery(query, new String[]{username});
        
        Employee employee = null;
        if (cursor.moveToFirst()) {
            employee = new Employee();
            employee.setEmployeeId(cursor.getString(cursor.getColumnIndex(COLUMN_EMPLOYEE_ID)));
            employee.setFirstName(cursor.getString(cursor.getColumnIndex(COLUMN_FIRST_NAME)));
            int middleNameIndex = cursor.getColumnIndex(COLUMN_MIDDLE_NAME);
            if (middleNameIndex >= 0 && !cursor.isNull(middleNameIndex)) {
                employee.setMiddleName(cursor.getString(middleNameIndex));
            }
            employee.setLastName(cursor.getString(cursor.getColumnIndex(COLUMN_LAST_NAME)));
            int dobIndex = cursor.getColumnIndex(COLUMN_DATE_OF_BIRTH);
            if (dobIndex >= 0 && !cursor.isNull(dobIndex)) {
                employee.setDateOfBirth(cursor.getString(dobIndex));
            }
            employee.setEmail(cursor.getString(cursor.getColumnIndex(COLUMN_EMAIL)));
            employee.setPhone(cursor.getString(cursor.getColumnIndex(COLUMN_PHONE)));
            employee.setRole(cursor.getString(cursor.getColumnIndex(COLUMN_ROLE)));
            employee.setUsername(cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME)));
            employee.setPassword(cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD)));
            int createdDateIndex = cursor.getColumnIndex(COLUMN_CREATED_DATE);
            if (createdDateIndex >= 0 && !cursor.isNull(createdDateIndex)) {
                employee.setCreatedDate(cursor.getString(createdDateIndex));
            }
            employee.setActive(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_ACTIVE)) == 1);
            int profilePicIndex = cursor.getColumnIndex(COLUMN_PROFILE_PICTURE_URL);
            if (profilePicIndex >= 0 && !cursor.isNull(profilePicIndex)) {
                employee.setProfilePictureUrl(cursor.getString(profilePicIndex));
            }
        }
        
        if (cursor != null) {
            cursor.close();
        }
        // Don't close database - reuse connection
        return employee;
    }

    /**
     * Update employee password
     */
    public boolean updateEmployeePassword(String username, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PASSWORD, newPassword);
        
        int result = db.update(TABLE_EMPLOYEES, values, COLUMN_USERNAME + " = ?", new String[]{username});
        // Don't close database - reuse connection
        
        return result > 0;
    }

    /**
     * Update employee information
     * IMPORTANT: This method preserves the is_active status. If an employee was deleted (is_active = 0),
     * it will NOT be restored even if the incoming employee data has is_active = true.
     */
    public boolean updateEmployee(Employee employee) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // Check if employee exists and was previously deleted
        Employee existing = getEmployeeById(employee.getEmployeeId());
        boolean wasDeleted = existing != null && !existing.isActive();
        
        // If employee was deleted, don't restore it - preserve deletion
        if (wasDeleted) {
            Log.d("HCasDatabaseHelper", "Employee " + employee.getEmployeeId() + " was deleted - not restoring");
            return false; // Don't update deleted employees
        }
        
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_FIRST_NAME, employee.getFirstName());
        if (employee.getMiddleName() != null) {
            values.put(COLUMN_MIDDLE_NAME, employee.getMiddleName());
        }
        values.put(COLUMN_LAST_NAME, employee.getLastName());
        if (employee.getDateOfBirth() != null) {
            values.put(COLUMN_DATE_OF_BIRTH, employee.getDateOfBirth());
        }
        values.put(COLUMN_EMAIL, employee.getEmail());
        values.put(COLUMN_PHONE, employee.getPhone());
        values.put(COLUMN_ROLE, employee.getRole());
        values.put(COLUMN_USERNAME, employee.getUsername());
        values.put(COLUMN_PASSWORD, employee.getPassword());
        // Preserve is_active status - don't restore deleted employees
        if (existing != null) {
            values.put(COLUMN_IS_ACTIVE, existing.isActive() ? 1 : 0);
        } else {
            values.put(COLUMN_IS_ACTIVE, employee.isActive() ? 1 : 0);
        }
        if (employee.getProfilePictureUrl() != null) {
            values.put(COLUMN_PROFILE_PICTURE_URL, employee.getProfilePictureUrl());
        }
        
        int result = db.update(TABLE_EMPLOYEES, values, COLUMN_EMPLOYEE_ID + " = ?", 
                             new String[]{employee.getEmployeeId()});
        // Don't close database - reuse connection
        
        // Invalidate cache when employee is updated
        if (result > 0) {
            invalidateEmployeeCache();
        }
        
        return result > 0;
    }
    
    /**
     * Get employee by employee ID (including admin)
     */
    public Employee getEmployeeById(String employeeId) {
        String query = "SELECT * FROM " + TABLE_EMPLOYEES + " WHERE " + COLUMN_EMPLOYEE_ID + " = ?";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{employeeId});
        
        Employee employee = null;
        if (cursor.moveToFirst()) {
            employee = new Employee();
            employee.setEmployeeId(cursor.getString(cursor.getColumnIndex(COLUMN_EMPLOYEE_ID)));
            employee.setFirstName(cursor.getString(cursor.getColumnIndex(COLUMN_FIRST_NAME)));
            int middleNameIndex = cursor.getColumnIndex(COLUMN_MIDDLE_NAME);
            if (middleNameIndex >= 0 && !cursor.isNull(middleNameIndex)) {
                employee.setMiddleName(cursor.getString(middleNameIndex));
            }
            employee.setLastName(cursor.getString(cursor.getColumnIndex(COLUMN_LAST_NAME)));
            int dobIndex = cursor.getColumnIndex(COLUMN_DATE_OF_BIRTH);
            if (dobIndex >= 0 && !cursor.isNull(dobIndex)) {
                employee.setDateOfBirth(cursor.getString(dobIndex));
            }
            employee.setEmail(cursor.getString(cursor.getColumnIndex(COLUMN_EMAIL)));
            employee.setPhone(cursor.getString(cursor.getColumnIndex(COLUMN_PHONE)));
            employee.setRole(cursor.getString(cursor.getColumnIndex(COLUMN_ROLE)));
            employee.setUsername(cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME)));
            employee.setPassword(cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD)));
            int createdDateIndex = cursor.getColumnIndex(COLUMN_CREATED_DATE);
            if (createdDateIndex >= 0 && !cursor.isNull(createdDateIndex)) {
                employee.setCreatedDate(cursor.getString(createdDateIndex));
            }
            employee.setActive(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_ACTIVE)) == 1);
            int profilePicIndex = cursor.getColumnIndex(COLUMN_PROFILE_PICTURE_URL);
            if (profilePicIndex >= 0 && !cursor.isNull(profilePicIndex)) {
                employee.setProfilePictureUrl(cursor.getString(profilePicIndex));
            }
        }
        
        if (cursor != null) {
            cursor.close();
        }
        // Don't close database - reuse connection
        return employee;
    }
    
    /**
     * Update employee profile picture URL
     */
    public boolean updateEmployeeProfilePicture(String employeeId, String profilePictureUrl) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROFILE_PICTURE_URL, profilePictureUrl);
        
        int result = db.update(TABLE_EMPLOYEES, values, COLUMN_EMPLOYEE_ID + " = ?", 
                             new String[]{employeeId});
        // Don't close database - reuse connection
        
        return result > 0;
    }

    // =====================
    // Patients operations
    // =====================

    /**
     * Add a new patient to the database
     */
    public boolean addPatient(com.healthcare.cas.models.Patient patient) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_PATIENT_ID, patient.getPatientId());
        values.put(COLUMN_PATIENT_FIRST_NAME, patient.getFirstName());
        values.put(COLUMN_PATIENT_LAST_NAME, patient.getLastName());
        values.put(COLUMN_PATIENT_DOB, patient.getDateOfBirth());
        values.put(COLUMN_PATIENT_GENDER, patient.getGender());
        values.put(COLUMN_PATIENT_ADDRESS, patient.getAddress());
        values.put(COLUMN_PATIENT_PHONE, patient.getPhone());
        values.put(COLUMN_PATIENT_EMAIL, patient.getEmail());
        values.put(COLUMN_PATIENT_EMERGENCY_NAME, patient.getEmergencyContactName());
        values.put(COLUMN_PATIENT_EMERGENCY_PHONE, patient.getEmergencyContactPhone());
        
        // Extended patient information
        values.put(COLUMN_PATIENT_SUFFIX, patient.getSuffix());
        values.put(COLUMN_PATIENT_FULL_NAME, patient.getFullName());
        values.put(COLUMN_PATIENT_AGE, patient.getAge());
        values.put(COLUMN_PATIENT_FULL_ADDRESS, patient.getFullAddress());
        values.put(COLUMN_PATIENT_PHONE_NUMBER, patient.getPhoneNumber());
        values.put(COLUMN_PATIENT_ALLERGIES, patient.getAllergies());
        values.put(COLUMN_PATIENT_MEDICATIONS, patient.getMedications());
        values.put(COLUMN_PATIENT_MEDICAL_HISTORY, patient.getMedicalHistory());
        values.put(COLUMN_PATIENT_PULSE_RATE, patient.getPulseRate());
        values.put(COLUMN_PATIENT_BLOOD_PRESSURE, patient.getBloodPressure());
        values.put(COLUMN_PATIENT_TEMPERATURE, patient.getTemperature());
        values.put(COLUMN_PATIENT_BLOOD_SUGAR, patient.getBloodSugar());
        values.put(COLUMN_PATIENT_PAIN_SCALE, patient.getPainScale());
        values.put(COLUMN_PATIENT_SYMPTOMS_DESCRIPTION, patient.getSymptomsDescription());
        values.put(COLUMN_PATIENT_BIRTH_PLACE, patient.getBirthPlace());
        values.put(COLUMN_PATIENT_NFC_UID, patient.getNfcUid());
        
        // Set created_date - use patient's createdDate if available, otherwise use current timestamp
        if (patient.getCreatedDate() != null && !patient.getCreatedDate().isEmpty()) {
            values.put(COLUMN_PATIENT_CREATED_DATE, patient.getCreatedDate());
        } else {
            // Fallback to current timestamp if not set
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            values.put(COLUMN_PATIENT_CREATED_DATE, sdf.format(new java.util.Date()));
        }

        long result = db.insert(TABLE_PATIENTS, null, values);
        // Don't close database - reuse connection
        
        // Sync to Firebase in background thread to avoid blocking
        if (result != -1) {
            final com.healthcare.cas.models.Patient patientToSync = patient;
            new Thread(() -> {
                syncToFirebase("patient", patientToSync);
            }).start();
        }
        
        return result != -1;
    }

    // getAllPatients implemented below

    /**
     * Get all patients
     */
    public List<com.healthcare.cas.models.Patient> getAllPatients() {
        List<com.healthcare.cas.models.Patient> patients = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_PATIENTS + " ORDER BY " + COLUMN_PATIENT_CREATED_DATE + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                com.healthcare.cas.models.Patient patient = new com.healthcare.cas.models.Patient();
                patient.setPatientId(cursor.getString(0));
                patient.setFirstName(cursor.getString(1));
                patient.setLastName(cursor.getString(2));
                patient.setDateOfBirth(cursor.getString(3));
                patient.setGender(cursor.getString(4));
                patient.setAddress(cursor.getString(5));
                patient.setPhone(cursor.getString(6));
                patient.setEmail(cursor.getString(7));
                patient.setEmergencyContactName(cursor.getString(8));
                patient.setEmergencyContactPhone(cursor.getString(9));
                patient.setCreatedDate(cursor.getString(10)); // created_date column
                
                // Extended patient information (new columns)
                if (cursor.getColumnCount() > 10) {
                    patient.setSuffix(cursor.getString(11));
                    patient.setFullName(cursor.getString(12));
                    patient.setBirthPlace(cursor.getString(13));
                    patient.setAge(cursor.getString(14));
                    patient.setFullAddress(cursor.getString(15));
                    patient.setPhoneNumber(cursor.getString(16));
                    patient.setAllergies(cursor.getString(17));
                    patient.setMedications(cursor.getString(18));
                    patient.setMedicalHistory(cursor.getString(19));
                    patient.setPulseRate(cursor.getString(20));
                    patient.setBloodPressure(cursor.getString(21));
                    patient.setTemperature(cursor.getString(22));
                    patient.setBloodSugar(cursor.getString(23));
                    patient.setPainScale(cursor.getString(24));
                    patient.setSymptomsDescription(cursor.getString(25));
                    // NFC UID column (index 26)
                    if (cursor.getColumnCount() > 26) {
                        patient.setNfcUid(cursor.getString(26));
                    }
                }
                
                patients.add(patient);
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }
        // Don't close database - reuse connection
        return patients;
    }
    
    /**
     * Get total count of patients (optimized)
     */
    public int getTotalPatientsCount() {
        String query = "SELECT COUNT(*) FROM " + TABLE_PATIENTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            
            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            return count;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
    }

    /**
     * Add a new prescription
     */
    public boolean addPrescription(com.healthcare.cas.models.Prescription prescription) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_PRESCRIPTION_ID, prescription.getPrescriptionId());
        values.put(COLUMN_PATIENT_ID, prescription.getPatientId());
        values.put(COLUMN_PATIENT_NAME, prescription.getPatientName());
        values.put(COLUMN_MEDICATION, prescription.getMedication());
        values.put(COLUMN_DOSAGE, prescription.getDosage());
        values.put(COLUMN_FREQUENCY, prescription.getFrequency());
        values.put(COLUMN_DURATION, prescription.getDuration());
        values.put(COLUMN_INSTRUCTIONS, prescription.getInstructions());
        values.put(COLUMN_DOCTOR_ID, prescription.getDoctorId());
        values.put(COLUMN_DOCTOR_NAME, prescription.getDoctorName());
        values.put(COLUMN_CREATED_DATE, prescription.getCreatedDate());
        values.put(COLUMN_STATUS, prescription.getStatus());
        
        long result = db.insert(TABLE_PRESCRIPTIONS, null, values);
        // Don't close database - reuse connection
        
        // Sync to Firebase in background thread to avoid blocking
        if (result != -1) {
            final com.healthcare.cas.models.Prescription prescriptionToSync = prescription;
            new Thread(() -> {
                syncToFirebase("prescription", prescriptionToSync);
            }).start();
        }
        
        return result != -1;
    }

    /**
     * Get prescription by ID
     */
    public com.healthcare.cas.models.Prescription getPrescriptionById(String prescriptionId) {
        String query = "SELECT * FROM " + TABLE_PRESCRIPTIONS + " WHERE " + COLUMN_PRESCRIPTION_ID + " = ?";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{prescriptionId});
            
            com.healthcare.cas.models.Prescription prescription = null;
            if (cursor.moveToFirst()) {
                prescription = new com.healthcare.cas.models.Prescription();
                prescription.setPrescriptionId(cursor.getString(0));
                prescription.setPatientId(cursor.getString(1));
                prescription.setPatientName(cursor.getString(2));
                prescription.setMedication(cursor.getString(3));
                prescription.setDosage(cursor.getString(4));
                prescription.setFrequency(cursor.getString(5));
                prescription.setDuration(cursor.getString(6));
                prescription.setInstructions(cursor.getString(7));
                prescription.setDoctorId(cursor.getString(8));
                prescription.setDoctorName(cursor.getString(9));
                prescription.setCreatedDate(cursor.getString(10));
                prescription.setStatus(cursor.getString(11));
            }
            return prescription;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
    }
    
    /**
     * Update prescription in database
     */
    public boolean updatePrescription(com.healthcare.cas.models.Prescription prescription) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_PATIENT_ID, prescription.getPatientId());
        values.put(COLUMN_PATIENT_NAME, prescription.getPatientName());
        values.put(COLUMN_MEDICATION, prescription.getMedication());
        values.put(COLUMN_DOSAGE, prescription.getDosage());
        values.put(COLUMN_FREQUENCY, prescription.getFrequency());
        values.put(COLUMN_DURATION, prescription.getDuration());
        values.put(COLUMN_INSTRUCTIONS, prescription.getInstructions());
        values.put(COLUMN_DOCTOR_ID, prescription.getDoctorId());
        values.put(COLUMN_DOCTOR_NAME, prescription.getDoctorName());
        values.put(COLUMN_CREATED_DATE, prescription.getCreatedDate());
        values.put(COLUMN_STATUS, prescription.getStatus());
        
        int result = db.update(TABLE_PRESCRIPTIONS, values, COLUMN_PRESCRIPTION_ID + " = ?",
                              new String[]{prescription.getPrescriptionId()});
        // Don't close database - reuse connection
        
        // Sync to Firebase in background thread to avoid blocking
        if (result > 0) {
            final com.healthcare.cas.models.Prescription prescriptionToSync = prescription;
            new Thread(() -> {
                syncToFirebase("prescription", prescriptionToSync);
            }).start();
        }
        
        return result > 0;
    }
    
    /**
     * Get all prescriptions
     */
    public List<com.healthcare.cas.models.Prescription> getAllPrescriptions() {
        List<com.healthcare.cas.models.Prescription> prescriptions = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_PRESCRIPTIONS + " ORDER BY " + COLUMN_CREATED_DATE + " DESC";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        com.healthcare.cas.models.Prescription prescription = new com.healthcare.cas.models.Prescription();
                        prescription.setPrescriptionId(cursor.isNull(0) ? null : cursor.getString(0));
                        prescription.setPatientId(cursor.isNull(1) ? null : cursor.getString(1));
                        prescription.setPatientName(cursor.isNull(2) ? null : cursor.getString(2));
                        prescription.setMedication(cursor.isNull(3) ? null : cursor.getString(3));
                        prescription.setDosage(cursor.isNull(4) ? null : cursor.getString(4));
                        prescription.setFrequency(cursor.isNull(5) ? null : cursor.getString(5));
                        prescription.setDuration(cursor.isNull(6) ? null : cursor.getString(6));
                        prescription.setInstructions(cursor.isNull(7) ? null : cursor.getString(7));
                        prescription.setDoctorId(cursor.isNull(8) ? null : cursor.getString(8));
                        prescription.setDoctorName(cursor.isNull(9) ? null : cursor.getString(9));
                        prescription.setCreatedDate(cursor.isNull(10) ? null : cursor.getString(10));
                        prescription.setStatus(cursor.isNull(11) ? null : cursor.getString(11));
                        
                        prescriptions.add(prescription);
                    } catch (Exception e) {
                        Log.e("HCasDatabaseHelper", "Error parsing prescription row: " + e.getMessage(), e);
                        // Continue with next row
                    }
                } while (cursor.moveToNext());
            }
            
            Log.d("HCasDatabaseHelper", "getAllPrescriptions: Found " + prescriptions.size() + " prescriptions");
        } catch (Exception e) {
            Log.e("HCasDatabaseHelper", "Error getting all prescriptions: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
        return prescriptions;
    }

    /**
     * Get patient by ID
     */
    public com.healthcare.cas.models.Patient getPatientById(String patientId) {
        String query = "SELECT * FROM " + TABLE_PATIENTS + " WHERE " + COLUMN_PATIENT_ID + " = ?";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{patientId});
            
            com.healthcare.cas.models.Patient patient = null;
            if (cursor.moveToFirst()) {
                patient = new com.healthcare.cas.models.Patient();
                patient.setPatientId(cursor.getString(0));
                patient.setFirstName(cursor.getString(1));
                patient.setLastName(cursor.getString(2));
                patient.setDateOfBirth(cursor.getString(3));
                patient.setGender(cursor.getString(4));
                patient.setAddress(cursor.getString(5));
                patient.setPhone(cursor.getString(6));
                patient.setEmail(cursor.getString(7));
                patient.setEmergencyContactName(cursor.getString(8));
                patient.setEmergencyContactPhone(cursor.getString(9));
                // Skip created date as Patient model doesn't have setCreatedDate method
                
                // Extended fields
                if (cursor.getColumnCount() > 10) {
                    patient.setSuffix(cursor.getString(11));
                    patient.setFullName(cursor.getString(12));
                    patient.setBirthPlace(cursor.getString(13));
                    patient.setAge(cursor.getString(14));
                    patient.setFullAddress(cursor.getString(15));
                    patient.setPhoneNumber(cursor.getString(16));
                    patient.setAllergies(cursor.getString(17));
                    patient.setMedications(cursor.getString(18));
                    patient.setMedicalHistory(cursor.getString(19));
                    patient.setPulseRate(cursor.getString(20));
                    patient.setBloodPressure(cursor.getString(21));
                    patient.setTemperature(cursor.getString(22));
                    patient.setBloodSugar(cursor.getString(23));
                    patient.setPainScale(cursor.getString(24));
                    patient.setSymptomsDescription(cursor.getString(25));
                    // NFC UID column (index 26)
                    if (cursor.getColumnCount() > 26) {
                        patient.setNfcUid(cursor.getString(26));
                    }
                }
                // Set created date
                if (cursor.getColumnCount() > 10) {
                    patient.setCreatedDate(cursor.getString(10));
                }
            }
            return patient;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
    }

    /**
     * Get patient by NFC UID (CRUD - Read)
     */
    public com.healthcare.cas.models.Patient getPatientByNfcUid(String nfcUid) {
        if (nfcUid == null || nfcUid.isEmpty()) {
            return null;
        }
        
        String query = "SELECT * FROM " + TABLE_PATIENTS + " WHERE " + COLUMN_PATIENT_NFC_UID + " = ?";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{nfcUid});
            
            com.healthcare.cas.models.Patient patient = null;
            if (cursor.moveToFirst()) {
                patient = new com.healthcare.cas.models.Patient();
                patient.setPatientId(cursor.getString(0));
                patient.setFirstName(cursor.getString(1));
                patient.setLastName(cursor.getString(2));
                patient.setDateOfBirth(cursor.getString(3));
                patient.setGender(cursor.getString(4));
                patient.setAddress(cursor.getString(5));
                patient.setPhone(cursor.getString(6));
                patient.setEmail(cursor.getString(7));
                patient.setEmergencyContactName(cursor.getString(8));
                patient.setEmergencyContactPhone(cursor.getString(9));
                patient.setCreatedDate(cursor.getString(10));
                
                // Extended fields
                if (cursor.getColumnCount() > 10) {
                    patient.setSuffix(cursor.getString(11));
                    patient.setFullName(cursor.getString(12));
                    patient.setBirthPlace(cursor.getString(13));
                    patient.setAge(cursor.getString(14));
                    patient.setFullAddress(cursor.getString(15));
                    patient.setPhoneNumber(cursor.getString(16));
                    patient.setAllergies(cursor.getString(17));
                    patient.setMedications(cursor.getString(18));
                    patient.setMedicalHistory(cursor.getString(19));
                    patient.setPulseRate(cursor.getString(20));
                    patient.setBloodPressure(cursor.getString(21));
                    patient.setTemperature(cursor.getString(22));
                    patient.setBloodSugar(cursor.getString(23));
                    patient.setPainScale(cursor.getString(24));
                    patient.setSymptomsDescription(cursor.getString(25));
                    // NFC UID column (index 26)
                    if (cursor.getColumnCount() > 26) {
                        patient.setNfcUid(cursor.getString(26));
                    }
                }
            }
            return patient;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
    }
    
    /**
     * Update patient NFC UID (CRUD - Update)
     */
    public boolean updatePatientNfcUid(String patientId, String nfcUid) {
        if (patientId == null || patientId.isEmpty()) {
            return false;
        }
        
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PATIENT_NFC_UID, nfcUid);
        
        int result = db.update(TABLE_PATIENTS, values, COLUMN_PATIENT_ID + " = ?", 
                             new String[]{patientId});
        // Don't close database - reuse connection
        
        // Sync to Firebase if successful
        if (result > 0) {
            com.healthcare.cas.models.Patient patient = getPatientById(patientId);
            if (patient != null) {
                syncToFirebase("patient", patient);
            }
        }
        
        return result > 0;
    }
    
    /**
     * Remove patient NFC UID (CRUD - Delete)
     */
    public boolean removePatientNfcUid(String patientId) {
        return updatePatientNfcUid(patientId, null);
    }
    
    /**
     * Check if NFC UID is already assigned to another patient
     */
    public boolean isNfcUidAssigned(String nfcUid, String excludePatientId) {
        if (nfcUid == null || nfcUid.isEmpty()) {
            return false;
        }
        
        String query = "SELECT COUNT(*) FROM " + TABLE_PATIENTS + 
                      " WHERE " + COLUMN_PATIENT_NFC_UID + " = ?";
        String[] args = {nfcUid};
        
        if (excludePatientId != null && !excludePatientId.isEmpty()) {
            query += " AND " + COLUMN_PATIENT_ID + " != ?";
            args = new String[]{nfcUid, excludePatientId};
        }
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, args);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0) > 0;
            }
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
    }

    /**
     * Get prescriptions by patient ID
     */
    public java.util.List<com.healthcare.cas.models.Prescription> getPrescriptionsByPatientId(String patientId) {
        java.util.List<com.healthcare.cas.models.Prescription> prescriptions = new java.util.ArrayList<>();
        String query = "SELECT * FROM " + TABLE_PRESCRIPTIONS + 
                       " WHERE " + COLUMN_PATIENT_ID + " = ? AND " + COLUMN_STATUS + " != 'Dispensed'" +
                       " ORDER BY " + COLUMN_CREATED_DATE + " DESC";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{patientId});
            
            if (cursor.moveToFirst()) {
                do {
                    com.healthcare.cas.models.Prescription prescription = new com.healthcare.cas.models.Prescription();
                    prescription.setPrescriptionId(cursor.getString(0));
                    prescription.setPatientId(cursor.getString(1));
                    prescription.setPatientName(cursor.getString(2));
                    prescription.setMedication(cursor.getString(3));
                    prescription.setDosage(cursor.getString(4));
                    prescription.setFrequency(cursor.getString(5));
                    prescription.setDuration(cursor.getString(6));
                    prescription.setInstructions(cursor.getString(7));
                    prescription.setDoctorId(cursor.getString(8));
                    prescription.setDoctorName(cursor.getString(9));
                    prescription.setCreatedDate(cursor.getString(10));
                    prescription.setStatus(cursor.getString(11));
                    
                    prescriptions.add(prescription);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
        
        return prescriptions;
    }

    /**
     * Get prescriptions count (optimized)
     */
    public int getPrescriptionsCount() {
        String query = "SELECT COUNT(*) FROM " + TABLE_PRESCRIPTIONS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            
            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            return count;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
    }

    // Medicine Management Methods
    /**
     * Add a new medicine to inventory
     */
    public boolean addMedicine(com.healthcare.cas.models.Medicine medicine) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_MEDICINE_ID, medicine.getMedicineId());
        values.put(COLUMN_MEDICINE_NAME, medicine.getMedicineName());
        values.put(COLUMN_MEDICINE_DOSAGE, medicine.getDosage());
        values.put(COLUMN_STOCK_QUANTITY, medicine.getStockQuantity());
        values.put(COLUMN_UNIT, medicine.getUnit());
        values.put(COLUMN_CATEGORY, medicine.getCategory());
        values.put(COLUMN_DESCRIPTION, medicine.getDescription());
        values.put(COLUMN_EXPIRY_DATE, medicine.getExpiryDate());
        values.put(COLUMN_PRICE, medicine.getPrice());
        values.put(COLUMN_SUPPLIER, medicine.getSupplier());
        
        long result = db.insert(TABLE_MEDICINES, null, values);
        // Don't close database - reuse connection
        
        return result != -1;
    }

    /**
     * Get medicine by name
     */
    public com.healthcare.cas.models.Medicine getMedicineByName(String medicineName) {
        String query = "SELECT * FROM " + TABLE_MEDICINES + " WHERE " + COLUMN_MEDICINE_NAME + " = ?";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{medicineName});
        
        com.healthcare.cas.models.Medicine medicine = null;
        if (cursor.moveToFirst()) {
            medicine = new com.healthcare.cas.models.Medicine();
            medicine.setMedicineId(cursor.getString(0));
            medicine.setMedicineName(cursor.getString(1));
            medicine.setDosage(cursor.getString(2));
            medicine.setStockQuantity(cursor.getInt(3));
            medicine.setUnit(cursor.getString(4));
            medicine.setCategory(cursor.getString(5));
            medicine.setDescription(cursor.getString(6));
            medicine.setExpiryDate(cursor.getString(7));
            medicine.setPrice(cursor.getDouble(8));
            medicine.setSupplier(cursor.getString(9));
        }
        
        cursor.close();
        // Don't close database - SQLiteOpenHelper manages connection pool automatically
        return medicine;
    }

    /**
     * Update medicine stock
     */
    public boolean updateMedicineStock(String medicineName, int newStock) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STOCK_QUANTITY, newStock);
        
        int result = db.update(TABLE_MEDICINES, values, COLUMN_MEDICINE_NAME + " = ?", new String[]{medicineName});
        // Don't close database - reuse connection
        
        return result > 0;
    }

    // RFID Management Methods
    /**
     * Write prescription data to RFID
     */
    public boolean writePrescriptionToRFID(String rfidTagId, com.healthcare.cas.models.Prescription prescription) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_RFID_TAG_ID, rfidTagId);
        values.put(COLUMN_PATIENT_ID, prescription.getPatientId());
        values.put(COLUMN_PATIENT_NAME, prescription.getPatientName());
        values.put(COLUMN_PRESCRIPTION_ID, prescription.getPrescriptionId());
        values.put(COLUMN_MEDICATION, prescription.getMedication());
        values.put(COLUMN_DOSAGE, prescription.getDosage());
        values.put(COLUMN_FREQUENCY, prescription.getFrequency());
        values.put(COLUMN_DURATION, prescription.getDuration());
        values.put(COLUMN_INSTRUCTIONS, prescription.getInstructions());
        values.put(COLUMN_DOCTOR_NAME, prescription.getDoctorName());
        values.put(COLUMN_CREATED_DATE, prescription.getCreatedDate());
        values.put(COLUMN_IS_DISPENSED, 0); // Not dispensed yet
        
        long result = db.insert(TABLE_RFID_DATA, null, values);
        // Don't close database - reuse connection
        
        return result != -1;
    }

    /**
     * Read prescription data from RFID
     */
    public com.healthcare.cas.models.RFIDData readPrescriptionFromRFID(String rfidTagId) {
        String query = "SELECT * FROM " + TABLE_RFID_DATA + " WHERE " + COLUMN_RFID_TAG_ID + " = ? AND " + COLUMN_IS_DISPENSED + " = 0";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{rfidTagId});
        
        com.healthcare.cas.models.RFIDData rfidData = null;
        if (cursor.moveToFirst()) {
            rfidData = new com.healthcare.cas.models.RFIDData();
            rfidData.setRfidTagId(cursor.getString(0));
            rfidData.setPatientId(cursor.getString(1));
            rfidData.setPatientName(cursor.getString(2));
            rfidData.setPrescriptionId(cursor.getString(3));
            rfidData.setMedicineName(cursor.getString(4));
            rfidData.setDosage(cursor.getString(5));
            rfidData.setFrequency(cursor.getString(6));
            rfidData.setDuration(cursor.getString(7));
            rfidData.setInstructions(cursor.getString(8));
            rfidData.setDoctorName(cursor.getString(9));
            rfidData.setPrescriptionDate(cursor.getString(10));
            rfidData.setDispensed(cursor.getInt(11) == 1);
            rfidData.setDispensedDate(cursor.getString(12));
            rfidData.setPharmacistName(cursor.getString(13));
        }
        
        cursor.close();
        // Don't close database - SQLiteOpenHelper manages connection pool automatically
        return rfidData;
    }

    /**
     * Mark prescription as dispensed
     */
    public boolean markPrescriptionAsDispensed(String rfidTagId, String pharmacistName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_DISPENSED, 1);
        values.put(COLUMN_DISPENSED_DATE, getCurrentDateTime());
        values.put(COLUMN_PHARMACIST_NAME, pharmacistName);
        
        int result = db.update(TABLE_RFID_DATA, values, COLUMN_RFID_TAG_ID + " = ?", new String[]{rfidTagId});
        // Don't close database - reuse connection
        
        return result > 0;
    }

    /**
     * Get current date time
     */
    private String getCurrentDateTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    /**
     * Insert sample medicines for testing
     */
    private void insertSampleMedicines(SQLiteDatabase db) {
        // Sample medicines
        String[][] medicines = {
            {"MED001", "Paracetamol", "500mg", "100", "tablets", "Pain Relief", "Pain reliever and fever reducer", "2025-12-31", "5.00", "MedSupply Co."},
            {"MED002", "Amoxicillin", "250mg", "50", "capsules", "Antibiotic", "Antibiotic for bacterial infections", "2025-11-30", "15.00", "PharmaCorp"},
            {"MED003", "Ibuprofen", "400mg", "75", "tablets", "Pain Relief", "Anti-inflammatory pain reliever", "2025-10-31", "8.50", "MedSupply Co."},
            {"MED004", "Metformin", "500mg", "30", "tablets", "Diabetes", "Diabetes medication", "2025-09-30", "12.00", "DiabeticCare"},
            {"MED005", "Lisinopril", "10mg", "40", "tablets", "Cardiovascular", "Blood pressure medication", "2025-08-31", "18.00", "CardioPharm"}
        };

        for (String[] medicine : medicines) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_MEDICINE_ID, medicine[0]);
            values.put(COLUMN_MEDICINE_NAME, medicine[1]);
            values.put(COLUMN_MEDICINE_DOSAGE, medicine[2]);
            values.put(COLUMN_STOCK_QUANTITY, Integer.parseInt(medicine[3]));
            values.put(COLUMN_UNIT, medicine[4]);
            values.put(COLUMN_CATEGORY, medicine[5]);
            values.put(COLUMN_DESCRIPTION, medicine[6]);
            values.put(COLUMN_EXPIRY_DATE, medicine[7]);
            values.put(COLUMN_PRICE, Double.parseDouble(medicine[8]));
            values.put(COLUMN_SUPPLIER, medicine[9]);
            
            db.insert(TABLE_MEDICINES, null, values);
        }
    }
    
    /**
     * Update patient information
     */
    public boolean updatePatient(com.healthcare.cas.models.Patient patient) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        // Update all patient fields
        values.put(COLUMN_PATIENT_FIRST_NAME, patient.getFirstName());
        values.put(COLUMN_PATIENT_LAST_NAME, patient.getLastName());
        values.put(COLUMN_PATIENT_DOB, patient.getDateOfBirth());
        values.put(COLUMN_PATIENT_GENDER, patient.getGender());
        values.put(COLUMN_PATIENT_ADDRESS, patient.getAddress());
        values.put(COLUMN_PATIENT_PHONE, patient.getPhone());
        values.put(COLUMN_PATIENT_EMAIL, patient.getEmail());
        values.put(COLUMN_PATIENT_EMERGENCY_NAME, patient.getEmergencyContactName());
        values.put(COLUMN_PATIENT_EMERGENCY_PHONE, patient.getEmergencyContactPhone());
        
        // Extended patient information
        values.put(COLUMN_PATIENT_SUFFIX, patient.getSuffix());
        values.put(COLUMN_PATIENT_FULL_NAME, patient.getFullName());
        values.put(COLUMN_PATIENT_AGE, patient.getAge());
        values.put(COLUMN_PATIENT_FULL_ADDRESS, patient.getFullAddress());
        values.put(COLUMN_PATIENT_PHONE_NUMBER, patient.getPhoneNumber());
        values.put(COLUMN_PATIENT_ALLERGIES, patient.getAllergies());
        values.put(COLUMN_PATIENT_MEDICATIONS, patient.getMedications());
        values.put(COLUMN_PATIENT_MEDICAL_HISTORY, patient.getMedicalHistory());
        values.put(COLUMN_PATIENT_PULSE_RATE, patient.getPulseRate());
        values.put(COLUMN_PATIENT_BLOOD_PRESSURE, patient.getBloodPressure());
        values.put(COLUMN_PATIENT_TEMPERATURE, patient.getTemperature());
        values.put(COLUMN_PATIENT_BLOOD_SUGAR, patient.getBloodSugar());
        values.put(COLUMN_PATIENT_PAIN_SCALE, patient.getPainScale());
        values.put(COLUMN_PATIENT_SYMPTOMS_DESCRIPTION, patient.getSymptomsDescription());
        values.put(COLUMN_PATIENT_BIRTH_PLACE, patient.getBirthPlace());
        values.put(COLUMN_PATIENT_NFC_UID, patient.getNfcUid());
        
        // Preserve created_date - only update if patient object has a new created_date, otherwise keep existing
        if (patient.getCreatedDate() != null && !patient.getCreatedDate().isEmpty()) {
            values.put(COLUMN_PATIENT_CREATED_DATE, patient.getCreatedDate());
        }
        // If created_date is null/empty, don't update it (preserve original registration date)
        
        int result = db.update(TABLE_PATIENTS, values, COLUMN_PATIENT_ID + " = ?", 
                             new String[]{patient.getPatientId()});
        // Don't close database - SQLiteOpenHelper manages connection pool automatically
        
        // Sync to Firebase if successful
        if (result > 0) {
            syncToFirebase("patient", patient);
        }
        
        return result > 0;
    }

    // ==================== MEDICINE MANAGEMENT METHODS ====================

    /**
     * Add a new medicine to the inventory
     */
    public boolean addMedicine1(com.healthcare.cas.models.Medicine medicine) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_MEDICINE_ID, medicine.getMedicineId());
        values.put(COLUMN_MEDICINE_NAME, medicine.getMedicineName());
        values.put(COLUMN_MEDICINE_DOSAGE, medicine.getDosage());
        values.put(COLUMN_STOCK_QUANTITY, medicine.getStockQuantity());
        values.put(COLUMN_UNIT, medicine.getUnit());
        values.put(COLUMN_CATEGORY, medicine.getCategory());
        values.put(COLUMN_DESCRIPTION, medicine.getDescription());
        values.put(COLUMN_EXPIRY_DATE, medicine.getExpiryDate());
        values.put(COLUMN_PRICE, medicine.getPrice());
        values.put(COLUMN_SUPPLIER, medicine.getSupplier());
        
        long result = db.insert(TABLE_MEDICINES, null, values);
        // Don't close database - reuse connection
        
        return result != -1;
    }

    /**
     * Get all medicines from inventory
     */
    public List<com.healthcare.cas.models.Medicine> getAllMedicines() {
        List<com.healthcare.cas.models.Medicine> medicines = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_MEDICINES + " ORDER BY " + COLUMN_MEDICINE_NAME;
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                com.healthcare.cas.models.Medicine medicine = new com.healthcare.cas.models.Medicine();
                medicine.setMedicineId(cursor.isNull(0) ? null : cursor.getString(0));
                medicine.setMedicineName(cursor.isNull(1) ? null : cursor.getString(1));
                medicine.setDosage(cursor.isNull(2) ? null : cursor.getString(2));
                medicine.setStockQuantity(cursor.isNull(3) ? 0 : cursor.getInt(3));
                medicine.setUnit(cursor.isNull(4) ? null : cursor.getString(4));
                medicine.setCategory(cursor.isNull(5) ? null : cursor.getString(5));
                medicine.setDescription(cursor.isNull(6) ? null : cursor.getString(6));
                medicine.setExpiryDate(cursor.isNull(7) ? null : cursor.getString(7));
                medicine.setPrice(cursor.isNull(8) ? 0.0 : cursor.getDouble(8));
                medicine.setSupplier(cursor.isNull(9) ? null : cursor.getString(9));
                
                medicines.add(medicine);
            } while (cursor.moveToNext());
        }
        
        if (cursor != null) {
            cursor.close();
        }
        // Don't close database - reuse connection
        return medicines;
    }

    /**
     * Get medicines with low stock (10 or less)
     */
    public List<com.healthcare.cas.models.Medicine> getLowStockMedicines() {
        List<com.healthcare.cas.models.Medicine> medicines = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_MEDICINES + 
                      " WHERE " + COLUMN_STOCK_QUANTITY + " <= 10 ORDER BY " + COLUMN_STOCK_QUANTITY;
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                com.healthcare.cas.models.Medicine medicine = new com.healthcare.cas.models.Medicine();
                medicine.setMedicineId(cursor.isNull(0) ? null : cursor.getString(0));
                medicine.setMedicineName(cursor.isNull(1) ? null : cursor.getString(1));
                medicine.setDosage(cursor.isNull(2) ? null : cursor.getString(2));
                medicine.setStockQuantity(cursor.isNull(3) ? 0 : cursor.getInt(3));
                medicine.setUnit(cursor.isNull(4) ? null : cursor.getString(4));
                medicine.setCategory(cursor.isNull(5) ? null : cursor.getString(5));
                medicine.setDescription(cursor.isNull(6) ? null : cursor.getString(6));
                medicine.setExpiryDate(cursor.isNull(7) ? null : cursor.getString(7));
                medicine.setPrice(cursor.isNull(8) ? 0.0 : cursor.getDouble(8));
                medicine.setSupplier(cursor.isNull(9) ? null : cursor.getString(9));
                
                medicines.add(medicine);
            } while (cursor.moveToNext());
        }
        
        if (cursor != null) {
            cursor.close();
        }
        // Don't close database - reuse connection
        return medicines;
    }

    /**
     * Get medicines expiring soon (within 30 days)
     */
    public List<com.healthcare.cas.models.Medicine> getExpiringSoonMedicines() {
        List<com.healthcare.cas.models.Medicine> medicines = new ArrayList<>();
        // For demo purposes, we'll consider medicines expiring in 2024 as expiring soon
        String query = "SELECT * FROM " + TABLE_MEDICINES + 
                      " WHERE " + COLUMN_EXPIRY_DATE + " LIKE '%2024%' ORDER BY " + COLUMN_EXPIRY_DATE;
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                com.healthcare.cas.models.Medicine medicine = new com.healthcare.cas.models.Medicine();
                medicine.setMedicineId(cursor.isNull(0) ? null : cursor.getString(0));
                medicine.setMedicineName(cursor.isNull(1) ? null : cursor.getString(1));
                medicine.setDosage(cursor.isNull(2) ? null : cursor.getString(2));
                medicine.setStockQuantity(cursor.isNull(3) ? 0 : cursor.getInt(3));
                medicine.setUnit(cursor.isNull(4) ? null : cursor.getString(4));
                medicine.setCategory(cursor.isNull(5) ? null : cursor.getString(5));
                medicine.setDescription(cursor.isNull(6) ? null : cursor.getString(6));
                medicine.setExpiryDate(cursor.isNull(7) ? null : cursor.getString(7));
                medicine.setPrice(cursor.isNull(8) ? 0.0 : cursor.getDouble(8));
                medicine.setSupplier(cursor.isNull(9) ? null : cursor.getString(9));
                
                medicines.add(medicine);
            } while (cursor.moveToNext());
        }
        
        if (cursor != null) {
            cursor.close();
        }
        // Don't close database - reuse connection
        return medicines;
    }

    /**
     * Update medicine information
     */
    /**
     * Update medicine in database and sync to Firebase
     */
    public boolean updateMedicine(com.healthcare.cas.models.Medicine medicine) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_MEDICINE_NAME, medicine.getMedicineName());
        values.put(COLUMN_MEDICINE_DOSAGE, medicine.getDosage());
        values.put(COLUMN_STOCK_QUANTITY, medicine.getStockQuantity());
        values.put(COLUMN_UNIT, medicine.getUnit());
        values.put(COLUMN_CATEGORY, medicine.getCategory());
        values.put(COLUMN_DESCRIPTION, medicine.getDescription());
        values.put(COLUMN_EXPIRY_DATE, medicine.getExpiryDate());
        values.put(COLUMN_PRICE, medicine.getPrice());
        values.put(COLUMN_SUPPLIER, medicine.getSupplier());
        
        int result = db.update(TABLE_MEDICINES, values, COLUMN_MEDICINE_ID + " = ?",
                              new String[]{medicine.getMedicineId()});
        // Don't close database - reuse connection
        
        // Sync to Firebase in background thread to avoid blocking
        if (result > 0) {
            final com.healthcare.cas.models.Medicine medicineToSync = medicine;
            new Thread(() -> {
                syncToFirebase("medicine", medicineToSync);
            }).start();
        }
        
        return result > 0;
    }

    /**
     * Delete medicine from inventory
     */
    public boolean deleteMedicine(String medicineId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_MEDICINES, COLUMN_MEDICINE_ID + " = ?", new String[]{medicineId});
        // Don't close database - reuse connection
        
        return result > 0;
    }

    /**
     * Get medicine by ID
     */
    public com.healthcare.cas.models.Medicine getMedicineById(String medicineId) {
        String query = "SELECT * FROM " + TABLE_MEDICINES + " WHERE " + COLUMN_MEDICINE_ID + " = ?";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{medicineId});
        
        com.healthcare.cas.models.Medicine medicine = null;
        if (cursor.moveToFirst()) {
            medicine = new com.healthcare.cas.models.Medicine();
            medicine.setMedicineId(cursor.getString(0));
            medicine.setMedicineName(cursor.getString(1));
            medicine.setDosage(cursor.getString(2));
            medicine.setStockQuantity(cursor.getInt(3));
            medicine.setUnit(cursor.getString(4));
            medicine.setCategory(cursor.getString(5));
            medicine.setDescription(cursor.getString(6));
            medicine.setExpiryDate(cursor.getString(7));
            medicine.setPrice(cursor.getDouble(8));
            medicine.setSupplier(cursor.getString(9));
        }
        
        cursor.close();
        // Don't close database - SQLiteOpenHelper manages connection pool automatically
        return medicine;
    }

    /**
     * Get total count of medicines
     */
    public int getTotalMedicinesCount() {
        String query = "SELECT COUNT(*) FROM " + TABLE_MEDICINES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        // Don't close database - SQLiteOpenHelper manages connection pool automatically
        return count;
    }

    /**
     * Get count of low stock medicines (optimized - uses database query instead of loading all)
     */
    public int getLowStockMedicinesCount(int minimumStock) {
        String query = "SELECT COUNT(*) FROM " + TABLE_MEDICINES + 
                      " WHERE " + COLUMN_STOCK_QUANTITY + " <= ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{String.valueOf(minimumStock)});
            
            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            return count;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
    }
    
    /**
     * Get count of low stock medicines (default threshold of 10)
     */
    public int getLowStockMedicinesCount() {
        return getLowStockMedicinesCount(10);
    }
    
    /**
     * Get count of expiring medicines (optimized - uses database query)
     */
    public int getExpiringSoonMedicinesCount(int thresholdMonths) {
        // Calculate expiry date threshold
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.MONTH, thresholdMonths);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String thresholdDate = sdf.format(cal.getTime());
        
        String query = "SELECT COUNT(*) FROM " + TABLE_MEDICINES + 
                      " WHERE " + COLUMN_EXPIRY_DATE + " <= ? AND " + COLUMN_EXPIRY_DATE + " IS NOT NULL";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{thresholdDate});
            
            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            return count;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close database - reuse connection
        }
    }
}

