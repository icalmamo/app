package com.example.h_cas.utils;

import android.content.Context;
import android.util.Log;

import com.example.h_cas.database.FirebaseRTDBHelper;
import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Employee;

import java.util.List;

/**
 * DataMigrationHelper - Migrates data from SQLite to Firebase RTDB
 */
public class DataMigrationHelper {
    
    private static final String TAG = "DataMigrationHelper";
    
    /**
     * Migrate all employees from SQLite to Firebase RTDB
     */
    public static void migrateEmployeesToFirebase(Context context, MigrationCallback callback) {
        Log.d(TAG, "🔄 Starting employee migration from SQLite to Firebase RTDB...");
        
        try {
            // Get employees from SQLite
            HCasDatabaseHelper sqliteHelper = new HCasDatabaseHelper(context);
            List<Employee> employees = sqliteHelper.getAllEmployees();
            
            if (employees == null || employees.isEmpty()) {
                Log.w(TAG, "⚠️ No employees found in SQLite database");
                if (callback != null) callback.onComplete(0, 0);
                return;
            }
            
            Log.d(TAG, "📊 Found " + employees.size() + " employees in SQLite");
            
            // Migrate to Firebase RTDB
            FirebaseRTDBHelper firebaseHelper = new FirebaseRTDBHelper(context);
            final int[] successCount = {0};
            final int[] failCount = {0};
            final int total = employees.size();
            
            for (Employee employee : employees) {
                firebaseHelper.addEmployee(employee, success -> {
                    if (success) {
                        successCount[0]++;
                        Log.d(TAG, "✅ Migrated employee: " + employee.getEmployeeId());
                    } else {
                        failCount[0]++;
                        Log.e(TAG, "❌ Failed to migrate employee: " + employee.getEmployeeId());
                    }
                    
                    // Check if all migrations are complete
                    if (successCount[0] + failCount[0] == total) {
                        Log.d(TAG, "✅ Migration complete: " + successCount[0] + " succeeded, " + failCount[0] + " failed");
                        if (callback != null) {
                            callback.onComplete(successCount[0], failCount[0]);
                        }
                    }
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error during migration", e);
            if (callback != null) callback.onError(e);
        }
    }
    
    /**
     * Create default admin account in Firebase RTDB if it doesn't exist
     */
    public static void createDefaultAdminInFirebase(Context context) {
        Log.d(TAG, "🔄 Creating default admin account in Firebase RTDB...");
        
        try {
            FirebaseRTDBHelper firebaseHelper = new FirebaseRTDBHelper(context);
            
            // Check if admin exists
            firebaseHelper.getEmployeeById("ADMIN001", employee -> {
                if (employee == null) {
                    // Create default admin
                    Employee admin = new Employee();
                    admin.setEmployeeId("ADMIN001");
                    admin.setFirstName("System");
                    admin.setLastName("Administrator");
                    admin.setEmail("admin@hcas.com");
                    admin.setPhone("0000000000");
                    admin.setRole("Administrator");
                    admin.setUsername("admin");
                    admin.setPassword("admin123");
                    admin.setCreatedDate("2024-01-01");
                    admin.setActive(true);
                    
                    firebaseHelper.addEmployee(admin, success -> {
                        if (success) {
                            Log.d(TAG, "✅ Default admin account created in Firebase RTDB");
                        } else {
                            Log.e(TAG, "❌ Failed to create default admin account");
                        }
                    });
                } else {
                    Log.d(TAG, "✅ Admin account already exists in Firebase RTDB");
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error creating default admin", e);
        }
    }
    
    /**
     * Create all default test accounts in Firebase RTDB
     */
    public static void createDefaultAccountsInFirebase(Context context) {
        Log.d(TAG, "🔄 Creating default test accounts in Firebase RTDB...");
        
        FirebaseRTDBHelper firebaseHelper = new FirebaseRTDBHelper(context);
        
        // Admin
        Employee admin = new Employee();
        admin.setEmployeeId("ADMIN001");
        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setEmail("admin@hcas.com");
        admin.setPhone("0000000000");
        admin.setRole("Administrator");
        admin.setUsername("admin");
        admin.setPassword("admin123");
        admin.setCreatedDate("2024-01-01");
        admin.setActive(true);
        firebaseHelper.addEmployee(admin, success -> {
            Log.d(TAG, success ? "✅ Admin created" : "❌ Admin creation failed");
        });
        
        // Doctor
        Employee doctor = new Employee();
        doctor.setEmployeeId("DOC001");
        doctor.setFirstName("Dr. John");
        doctor.setLastName("Smith");
        doctor.setEmail("john.smith@hcas.com");
        doctor.setPhone("1234567890");
        doctor.setRole("Doctor");
        doctor.setUsername("doctor");
        doctor.setPassword("doctor123");
        doctor.setCreatedDate("2024-01-01");
        doctor.setActive(true);
        firebaseHelper.addEmployee(doctor, success -> {
            Log.d(TAG, success ? "✅ Doctor created" : "❌ Doctor creation failed");
        });
        
        // Nurse
        Employee nurse = new Employee();
        nurse.setEmployeeId("NUR001");
        nurse.setFirstName("Jane");
        nurse.setLastName("Doe");
        nurse.setEmail("jane.doe@hcas.com");
        nurse.setPhone("0987654321");
        nurse.setRole("Nurse");
        nurse.setUsername("nurse");
        nurse.setPassword("nurse123");
        nurse.setCreatedDate("2024-01-01");
        nurse.setActive(true);
        firebaseHelper.addEmployee(nurse, success -> {
            Log.d(TAG, success ? "✅ Nurse created" : "❌ Nurse creation failed");
        });
        
        // Pharmacist
        Employee pharmacist = new Employee();
        pharmacist.setEmployeeId("PHA001");
        pharmacist.setFirstName("Mike");
        pharmacist.setLastName("Johnson");
        pharmacist.setEmail("mike.johnson@hcas.com");
        pharmacist.setPhone("1122334455");
        pharmacist.setRole("Pharmacist");
        pharmacist.setUsername("pharmacist");
        pharmacist.setPassword("pharmacist123");
        pharmacist.setCreatedDate("2024-01-01");
        pharmacist.setActive(true);
        firebaseHelper.addEmployee(pharmacist, success -> {
            Log.d(TAG, success ? "✅ Pharmacist created" : "❌ Pharmacist creation failed");
        });
        
        Log.d(TAG, "✅ Default accounts creation initiated");
    }
    
    public interface MigrationCallback {
        void onComplete(int successCount, int failCount);
        void onError(Exception e);
    }
}







