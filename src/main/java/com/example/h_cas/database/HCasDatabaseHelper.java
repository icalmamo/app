package com.example.h_cas.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.h_cas.models.Employee;

import java.util.ArrayList;
import java.util.List;

/**
 * HCasDatabaseHelper manages the SQLite database for the H-CAS healthcare system.
 * Handles all database operations for employees, cases, and system data.
 */
public class HCasDatabaseHelper extends SQLiteOpenHelper {

    // Database information
    private static final String DATABASE_NAME = "hcas_healthcare.db";
    private static final int DATABASE_VERSION = 5;

    // Employee table
    private static final String TABLE_EMPLOYEES = "employees";
    private static final String COLUMN_EMPLOYEE_ID = "employee_id";
    private static final String COLUMN_FIRST_NAME = "first_name";
    private static final String COLUMN_LAST_NAME = "last_name";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PHONE = "phone";
    private static final String COLUMN_ROLE = "role";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_CREATED_DATE = "created_date";
    private static final String COLUMN_IS_ACTIVE = "is_active";

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
        COLUMN_LAST_NAME + " TEXT NOT NULL, " +
        COLUMN_EMAIL + " TEXT UNIQUE NOT NULL, " +
        COLUMN_PHONE + " TEXT NOT NULL, " +
        COLUMN_ROLE + " TEXT NOT NULL, " +
        COLUMN_USERNAME + " TEXT UNIQUE NOT NULL, " +
        COLUMN_PASSWORD + " TEXT NOT NULL, " +
        COLUMN_CREATED_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1" +
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
        COLUMN_PATIENT_SYMPTOMS_DESCRIPTION + " TEXT" +
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

    public HCasDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
        // Drop existing tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CASES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMPLOYEES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PATIENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRESCRIPTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICINES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RFID_DATA);
        
        // Recreate tables
        onCreate(db);
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
     */
    public boolean addEmployee(Employee employee) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_EMPLOYEE_ID, employee.getEmployeeId());
        values.put(COLUMN_FIRST_NAME, employee.getFirstName());
        values.put(COLUMN_LAST_NAME, employee.getLastName());
        values.put(COLUMN_EMAIL, employee.getEmail());
        values.put(COLUMN_PHONE, employee.getPhone());
        values.put(COLUMN_ROLE, employee.getRole());
        values.put(COLUMN_USERNAME, employee.getUsername());
        values.put(COLUMN_PASSWORD, employee.getPassword());
        values.put(COLUMN_IS_ACTIVE, employee.isActive() ? 1 : 0);
        
        long result = db.insert(TABLE_EMPLOYEES, null, values);
        db.close();
        
        return result != -1;
    }

    /**
     * Get all employees (excluding administrators)
     */
    public List<Employee> getAllEmployees() {
        List<Employee> employees = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_EMPLOYEES + " WHERE " + COLUMN_IS_ACTIVE + " = 1 AND " + COLUMN_ROLE + " != 'Administrator'";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
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
                employee.setPassword(cursor.getString(7));
                employee.setCreatedDate(cursor.getString(8));
                employee.setActive(cursor.getInt(9) == 1);
                
                employees.add(employee);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return employees;
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
                employee.setEmployeeId(cursor.getString(0));
                employee.setFirstName(cursor.getString(1));
                employee.setLastName(cursor.getString(2));
                employee.setEmail(cursor.getString(3));
                employee.setPhone(cursor.getString(4));
                employee.setRole(cursor.getString(5));
                employee.setUsername(cursor.getString(6));
                employee.setPassword(cursor.getString(7));
                employee.setCreatedDate(cursor.getString(8));
                employee.setActive(cursor.getInt(9) == 1);
                
                employees.add(employee);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return employees;
    }

    /**
     * Authenticate user login
     */
    public Employee authenticateUser(String username, String password) {
        System.out.println("DEBUG: Database authenticateUser called with username: " + username);
        
        String query = "SELECT * FROM " + TABLE_EMPLOYEES + 
                      " WHERE " + COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ? AND " + COLUMN_IS_ACTIVE + " = 1";
        
        System.out.println("DEBUG: Query: " + query);
        System.out.println("DEBUG: Username: " + username + ", Password: " + password);
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{username, password});
        
        System.out.println("DEBUG: Cursor count: " + cursor.getCount());
        
        Employee employee = null;
        if (cursor.moveToFirst()) {
            System.out.println("DEBUG: Found employee in database");
            employee = new Employee();
            employee.setEmployeeId(cursor.getString(0));
            employee.setFirstName(cursor.getString(1));
            employee.setLastName(cursor.getString(2));
            employee.setEmail(cursor.getString(3));
            employee.setPhone(cursor.getString(4));
            employee.setRole(cursor.getString(5));
            employee.setUsername(cursor.getString(6));
            employee.setPassword(cursor.getString(7));
            employee.setCreatedDate(cursor.getString(8));
            employee.setActive(cursor.getInt(9) == 1);
            
            System.out.println("DEBUG: Employee role: " + employee.getRole());
            System.out.println("DEBUG: Employee active: " + employee.isActive());
        } else {
            System.out.println("DEBUG: No employee found with these credentials");
        }
        
        cursor.close();
        db.close();
        return employee;
    }

    /**
     * Debug method to check if employees exist in database
     */
    public void debugCheckEmployees() {
        System.out.println("DEBUG: Checking employees in database...");
        
        String query = "SELECT * FROM " + TABLE_EMPLOYEES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
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
        
        cursor.close();
        db.close();
    }

    /**
     * Validate employee login credentials
     */
    public boolean validateEmployeeLogin(String username, String password) {
        String query = "SELECT COUNT(*) FROM " + TABLE_EMPLOYEES + 
                      " WHERE " + COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ? AND " + COLUMN_IS_ACTIVE + " = 1";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{username, password});
        
        boolean isValid = false;
        if (cursor.moveToFirst()) {
            isValid = cursor.getInt(0) > 0;
        }
        
        cursor.close();
        db.close();
        return isValid;
    }

    /**
     * Check if username already exists
     */
    public boolean isUsernameExists(String username) {
        String query = "SELECT COUNT(*) FROM " + TABLE_EMPLOYEES + " WHERE " + COLUMN_USERNAME + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{username});
        
        boolean exists = false;
        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
        }
        
        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Check if email already exists
     */
    public boolean isEmailExists(String email) {
        String query = "SELECT COUNT(*) FROM " + TABLE_EMPLOYEES + " WHERE " + COLUMN_EMAIL + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{email});
        
        boolean exists = false;
        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
        }
        
        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Get total count of employees (excluding administrators)
     */
    public int getTotalEmployeesCount() {
        String query = "SELECT COUNT(*) FROM " + TABLE_EMPLOYEES + " WHERE " + COLUMN_IS_ACTIVE + " = 1 AND " + COLUMN_ROLE + " != 'Administrator'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        db.close();
        return count;
    }

    /**
     * Get count of employees by role (excluding administrators)
     */
    public int getEmployeesCountByRole(String role) {
        String query = "SELECT COUNT(*) FROM " + TABLE_EMPLOYEES + 
                      " WHERE " + COLUMN_ROLE + " = ? AND " + COLUMN_IS_ACTIVE + " = 1 AND " + COLUMN_ROLE + " != 'Administrator'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{role});
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        db.close();
        return count;
    }

    /**
     * Get today's cases count (placeholder for future implementation)
     */
    public int getTodaysCasesCount() {
        // This would be implemented when cases functionality is added
        return 0;
    }

    /**
     * Get pending reviews count (placeholder for future implementation)
     */
    public int getPendingReviewsCount() {
        // This would be implemented when reviews functionality is added
        return 0;
    }

    /**
     * Delete employee (soft delete)
     */
    public boolean deleteEmployee(String employeeId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_ACTIVE, 0);
        
        int result = db.update(TABLE_EMPLOYEES, values, COLUMN_EMPLOYEE_ID + " = ?", new String[]{employeeId});
        db.close();
        
        return result > 0;
    }

    /**
     * Get employee by username
     */
    public Employee getEmployeeByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_EMPLOYEES + " WHERE " + COLUMN_USERNAME + " = ? AND " + COLUMN_IS_ACTIVE + " = 1";
        
        Cursor cursor = db.rawQuery(query, new String[]{username});
        
        Employee employee = null;
        if (cursor.moveToFirst()) {
            employee = new Employee();
            employee.setEmployeeId(cursor.getString(0));
            employee.setFirstName(cursor.getString(1));
            employee.setLastName(cursor.getString(2));
            employee.setEmail(cursor.getString(3));
            employee.setPhone(cursor.getString(4));
            employee.setRole(cursor.getString(5));
            employee.setUsername(cursor.getString(6));
            employee.setPassword(cursor.getString(7));
            employee.setCreatedDate(cursor.getString(8));
            employee.setActive(cursor.getInt(9) == 1);
        }
        
        cursor.close();
        db.close();
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
        db.close();
        
        return result > 0;
    }

    /**
     * Update employee information
     */
    public boolean updateEmployee(Employee employee) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_FIRST_NAME, employee.getFirstName());
        values.put(COLUMN_LAST_NAME, employee.getLastName());
        values.put(COLUMN_EMAIL, employee.getEmail());
        values.put(COLUMN_PHONE, employee.getPhone());
        values.put(COLUMN_ROLE, employee.getRole());
        values.put(COLUMN_USERNAME, employee.getUsername());
        values.put(COLUMN_PASSWORD, employee.getPassword());
        
        int result = db.update(TABLE_EMPLOYEES, values, COLUMN_EMPLOYEE_ID + " = ?", 
                             new String[]{employee.getEmployeeId()});
        db.close();
        
        return result > 0;
    }

    // =====================
    // Patients operations
    // =====================

    /**
     * Add a new patient to the database
     */
    public boolean addPatient(com.example.h_cas.models.Patient patient) {
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

        long result = db.insert(TABLE_PATIENTS, null, values);
        db.close();
        return result != -1;
    }

    // getAllPatients implemented below

    /**
     * Get all patients
     */
    public List<com.example.h_cas.models.Patient> getAllPatients() {
        List<com.example.h_cas.models.Patient> patients = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_PATIENTS + " ORDER BY " + COLUMN_PATIENT_CREATED_DATE + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                com.example.h_cas.models.Patient patient = new com.example.h_cas.models.Patient();
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
                }
                
                patients.add(patient);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return patients;
    }
    
    /**
     * Get total count of patients
     */
    public int getTotalPatientsCount() {
        String query = "SELECT COUNT(*) FROM " + TABLE_PATIENTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        db.close();
        return count;
    }

    /**
     * Add a new prescription
     */
    public boolean addPrescription(com.example.h_cas.models.Prescription prescription) {
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
        db.close();
        
        return result != -1;
    }

    /**
     * Get all prescriptions
     */
    public List<com.example.h_cas.models.Prescription> getAllPrescriptions() {
        List<com.example.h_cas.models.Prescription> prescriptions = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_PRESCRIPTIONS + " ORDER BY " + COLUMN_CREATED_DATE + " DESC";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                com.example.h_cas.models.Prescription prescription = new com.example.h_cas.models.Prescription();
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
        
        cursor.close();
        db.close();
        return prescriptions;
    }

    /**
     * Get patient by ID
     */
    public com.example.h_cas.models.Patient getPatientById(String patientId) {
        String query = "SELECT * FROM " + TABLE_PATIENTS + " WHERE " + COLUMN_PATIENT_ID + " = ?";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{patientId});
        
        com.example.h_cas.models.Patient patient = null;
        if (cursor.moveToFirst()) {
            patient = new com.example.h_cas.models.Patient();
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
            }
        }
        
        cursor.close();
        db.close();
        return patient;
    }

    /**
     * Get prescriptions count
     */
    public int getPrescriptionsCount() {
        String query = "SELECT COUNT(*) FROM " + TABLE_PRESCRIPTIONS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        db.close();
        return count;
    }

    // Medicine Management Methods
    /**
     * Add a new medicine to inventory
     */
    public boolean addMedicine(com.example.h_cas.models.Medicine medicine) {
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
        db.close();
        
        return result != -1;
    }

    /**
     * Get medicine by name
     */
    public com.example.h_cas.models.Medicine getMedicineByName(String medicineName) {
        String query = "SELECT * FROM " + TABLE_MEDICINES + " WHERE " + COLUMN_MEDICINE_NAME + " = ?";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{medicineName});
        
        com.example.h_cas.models.Medicine medicine = null;
        if (cursor.moveToFirst()) {
            medicine = new com.example.h_cas.models.Medicine();
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
        db.close();
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
        db.close();
        
        return result > 0;
    }

    // RFID Management Methods
    /**
     * Write prescription data to RFID
     */
    public boolean writePrescriptionToRFID(String rfidTagId, com.example.h_cas.models.Prescription prescription) {
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
        db.close();
        
        return result != -1;
    }

    /**
     * Read prescription data from RFID
     */
    public com.example.h_cas.models.RFIDData readPrescriptionFromRFID(String rfidTagId) {
        String query = "SELECT * FROM " + TABLE_RFID_DATA + " WHERE " + COLUMN_RFID_TAG_ID + " = ? AND " + COLUMN_IS_DISPENSED + " = 0";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{rfidTagId});
        
        com.example.h_cas.models.RFIDData rfidData = null;
        if (cursor.moveToFirst()) {
            rfidData = new com.example.h_cas.models.RFIDData();
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
        db.close();
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
        db.close();
        
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
    public boolean updatePatient(com.example.h_cas.models.Patient patient) {
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
        
        int result = db.update(TABLE_PATIENTS, values, COLUMN_PATIENT_ID + " = ?", 
                             new String[]{patient.getPatientId()});
        db.close();
        
        return result > 0;
    }

    // ==================== MEDICINE MANAGEMENT METHODS ====================

    /**
     * Add a new medicine to the inventory
     */
    public boolean addMedicine1(com.example.h_cas.models.Medicine medicine) {
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
        db.close();
        
        return result != -1;
    }

    /**
     * Get all medicines from inventory
     */
    public List<com.example.h_cas.models.Medicine> getAllMedicines() {
        List<com.example.h_cas.models.Medicine> medicines = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_MEDICINES + " ORDER BY " + COLUMN_MEDICINE_NAME;
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                com.example.h_cas.models.Medicine medicine = new com.example.h_cas.models.Medicine();
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
                
                medicines.add(medicine);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return medicines;
    }

    /**
     * Get medicines with low stock (10 or less)
     */
    public List<com.example.h_cas.models.Medicine> getLowStockMedicines() {
        List<com.example.h_cas.models.Medicine> medicines = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_MEDICINES + 
                      " WHERE " + COLUMN_STOCK_QUANTITY + " <= 10 ORDER BY " + COLUMN_STOCK_QUANTITY;
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                com.example.h_cas.models.Medicine medicine = new com.example.h_cas.models.Medicine();
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
                
                medicines.add(medicine);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return medicines;
    }

    /**
     * Get medicines expiring soon (within 30 days)
     */
    public List<com.example.h_cas.models.Medicine> getExpiringSoonMedicines() {
        List<com.example.h_cas.models.Medicine> medicines = new ArrayList<>();
        // For demo purposes, we'll consider medicines expiring in 2024 as expiring soon
        String query = "SELECT * FROM " + TABLE_MEDICINES + 
                      " WHERE " + COLUMN_EXPIRY_DATE + " LIKE '%2024%' ORDER BY " + COLUMN_EXPIRY_DATE;
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                com.example.h_cas.models.Medicine medicine = new com.example.h_cas.models.Medicine();
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
                
                medicines.add(medicine);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return medicines;
    }

    /**
     * Update medicine information
     */
    public boolean updateMedicine(com.example.h_cas.models.Medicine medicine) {
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
        db.close();
        
        return result > 0;
    }

    /**
     * Delete medicine from inventory
     */
    public boolean deleteMedicine(String medicineId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_MEDICINES, COLUMN_MEDICINE_ID + " = ?", new String[]{medicineId});
        db.close();
        
        return result > 0;
    }

    /**
     * Get medicine by ID
     */
    public com.example.h_cas.models.Medicine getMedicineById(String medicineId) {
        String query = "SELECT * FROM " + TABLE_MEDICINES + " WHERE " + COLUMN_MEDICINE_ID + " = ?";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{medicineId});
        
        com.example.h_cas.models.Medicine medicine = null;
        if (cursor.moveToFirst()) {
            medicine = new com.example.h_cas.models.Medicine();
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
        db.close();
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
        db.close();
        return count;
    }

    /**
     * Get count of low stock medicines
     */
    public int getLowStockMedicinesCount() {
        String query = "SELECT COUNT(*) FROM " + TABLE_MEDICINES + " WHERE " + COLUMN_STOCK_QUANTITY + " <= 10";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        db.close();
        return count;
    }

    /**
     * Get count of expiring soon medicines
     */
    public int getExpiringSoonMedicinesCount() {
        String query = "SELECT COUNT(*) FROM " + TABLE_MEDICINES + " WHERE " + COLUMN_EXPIRY_DATE + " LIKE '%2024%'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        db.close();
        return count;
    }
}

