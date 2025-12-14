package com.healthcare.cas;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.healthcare.cas.database.FirebaseHelper;
import com.healthcare.cas.database.FirebaseRTDBHelper;
import com.healthcare.cas.database.HCasDatabaseHelper;
import com.healthcare.cas.models.Employee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ManageEmployeesFragment allows admins to view, add, edit, and manage all employees.
 */
public class ManageEmployeesFragment extends Fragment {

    // Maximum number of employees to display in the list (3,000 limit)
    private static final int MAX_EMPLOYEES_DISPLAY = 3000;

    private RecyclerView employeesRecyclerView;
    private TextView emptyStateTextView;
    private FloatingActionButton addEmployeeButton;
    private MaterialButton filterAllButton;
    private MaterialButton filterNurseButton;
    private MaterialButton filterDoctorButton;
    private MaterialButton filterPharmacistButton;
    private HCasDatabaseHelper databaseHelper;
    private EmployeeAdapter employeeAdapter;
    private FirebaseHelper firebaseHelper;
    private FirebaseRTDBHelper firebaseRTDBHelper;
    private com.google.firebase.database.ChildEventListener employeesListener;
    private String roleFilter = null; // Filter by role (Nurse, Doctor, Pharmacist) or null for all

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_employees, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Get role filter from arguments (if passed from dashboard card click)
        // If FILTER_ROLE is not in arguments or is null, show all employees
        if (getArguments() != null) {
            String filterFromArgs = getArguments().getString("FILTER_ROLE");
            if (filterFromArgs != null && !filterFromArgs.isEmpty()) {
                roleFilter = filterFromArgs.trim();
                Log.d("ManageEmployees", "🔍 Role filter received from dashboard: '" + roleFilter + "'");
                Log.d("ManageEmployees", "   → Will show only employees with role: " + roleFilter);
                Log.d("ManageEmployees", "   → Filter will be applied when loading employees");
            } else {
                // Explicitly set to null to show all employees (Total Employees card clicked)
                roleFilter = null;
                Log.d("ManageEmployees", "ℹ️ No role filter in arguments - will show ALL active non-admin employees");
                Log.d("ManageEmployees", "   → This corresponds to 'Total Employees' card on dashboard");
            }
        } else {
            // No arguments - show all employees
            roleFilter = null;
            Log.d("ManageEmployees", "ℹ️ No arguments - will show all active non-admin employees");
        }
        
        initializeViews(view);
        initializeDatabase();
        setupRecyclerView();
        setupClickListeners();
        
        // CRITICAL: Update filter button states BEFORE loading employees
        // This ensures the correct filter is applied and buttons show the right state
        updateFilterButtonStates();
        
        // Load employees with the filter applied
        loadEmployees();
        setupRealtimeListener();
    }

    private void initializeViews(View view) {
        employeesRecyclerView = view.findViewById(R.id.employeesRecyclerView);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
        addEmployeeButton = view.findViewById(R.id.addEmployeeButton);
        filterAllButton = view.findViewById(R.id.filterAllButton);
        filterNurseButton = view.findViewById(R.id.filterNurseButton);
        filterDoctorButton = view.findViewById(R.id.filterDoctorButton);
        filterPharmacistButton = view.findViewById(R.id.filterPharmacistButton);
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
        try {
            firebaseHelper = new FirebaseHelper();
        } catch (Exception e) {
            Log.e("ManageEmployees", "Failed to initialize FirebaseHelper", e);
            firebaseHelper = null;
        }
        try {
            firebaseRTDBHelper = new FirebaseRTDBHelper(getContext());
            Log.d("ManageEmployees", "✅ FirebaseRTDBHelper initialized - using same data source as dashboard");
        } catch (Exception e) {
            Log.e("ManageEmployees", "Failed to initialize FirebaseRTDBHelper", e);
            firebaseRTDBHelper = null;
        }
    }

    private void setupRecyclerView() {
        employeeAdapter = new EmployeeAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        employeesRecyclerView.setLayoutManager(layoutManager);
        employeesRecyclerView.setAdapter(employeeAdapter);
        
        // Performance optimizations for smooth scrolling
        employeesRecyclerView.setHasFixedSize(true); // RecyclerView has fixed size (match_parent)
        employeesRecyclerView.setItemViewCacheSize(20); // Cache more views for smoother scrolling
        employeesRecyclerView.setNestedScrollingEnabled(true); // Enable nested scrolling for CoordinatorLayout
        
        // Improve scrolling performance
        employeesRecyclerView.setDrawingCacheEnabled(true);
        employeesRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
    }

    private void setupClickListeners() {
        addEmployeeButton.setOnClickListener(v -> showAddEmployeeDialog());
        setupFilterButtons();
    }
    
    /**
     * Setup filter button listeners and update button states
     */
    private void setupFilterButtons() {
        // Set initial button state based on current filter
        updateFilterButtonStates();
        
        // All button - clear filter
        filterAllButton.setOnClickListener(v -> {
            roleFilter = null;
            Log.d("ManageEmployees", "🔍 Filter changed to: ALL (no filter)");
            updateFilterButtonStates();
            loadEmployees();
        });
        
        // Nurse button
        filterNurseButton.setOnClickListener(v -> {
            roleFilter = "Nurse";
            Log.d("ManageEmployees", "🔍 Filter changed to: Nurse");
            updateFilterButtonStates();
            loadEmployees();
        });
        
        // Doctor button
        filterDoctorButton.setOnClickListener(v -> {
            roleFilter = "Doctor";
            Log.d("ManageEmployees", "🔍 Filter changed to: Doctor");
            updateFilterButtonStates();
            loadEmployees();
        });
        
        // Pharmacist button
        filterPharmacistButton.setOnClickListener(v -> {
            roleFilter = "Pharmacist";
            Log.d("ManageEmployees", "🔍 Filter changed to: Pharmacist");
            updateFilterButtonStates();
            loadEmployees();
        });
    }
    
    /**
     * Update filter button visual states (selected/unselected)
     */
    private void updateFilterButtonStates() {
        if (filterAllButton == null || filterNurseButton == null || filterDoctorButton == null || filterPharmacistButton == null) {
            Log.w("ManageEmployees", "⚠️ Filter buttons not initialized yet");
            return;
        }
        
        // Reset all buttons to unselected state (transparent background, default text color)
        int defaultTextColor = getResources().getColor(R.color.text_primary);
        
        filterAllButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        filterAllButton.setTextColor(defaultTextColor);
        
        filterNurseButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        filterNurseButton.setTextColor(defaultTextColor);
        
        filterDoctorButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        filterDoctorButton.setTextColor(defaultTextColor);
        
        filterPharmacistButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        filterPharmacistButton.setTextColor(defaultTextColor);
        
        // Set selected button based on current filter
        int selectedBgColor = getResources().getColor(R.color.admin_deep_purple_light);
        int selectedTextColor = getResources().getColor(R.color.white);
        
        Log.d("ManageEmployees", "🎨 Updating filter button states - current filter: " + (roleFilter != null ? roleFilter : "ALL"));
        
        if (roleFilter == null || roleFilter.isEmpty()) {
            // All selected
            filterAllButton.setBackgroundColor(selectedBgColor);
            filterAllButton.setTextColor(selectedTextColor);
            Log.d("ManageEmployees", "   ✅ 'All' button selected");
        } else if ("Nurse".equalsIgnoreCase(roleFilter)) {
            // Nurse selected
            filterNurseButton.setBackgroundColor(selectedBgColor);
            filterNurseButton.setTextColor(selectedTextColor);
            Log.d("ManageEmployees", "   ✅ 'Nurses' button selected");
        } else if ("Doctor".equalsIgnoreCase(roleFilter)) {
            // Doctor selected
            filterDoctorButton.setBackgroundColor(selectedBgColor);
            filterDoctorButton.setTextColor(selectedTextColor);
            Log.d("ManageEmployees", "   ✅ 'Doctors' button selected");
        } else if ("Pharmacist".equalsIgnoreCase(roleFilter)) {
            // Pharmacist selected
            filterPharmacistButton.setBackgroundColor(selectedBgColor);
            filterPharmacistButton.setTextColor(selectedTextColor);
            Log.d("ManageEmployees", "   ✅ 'Pharmacists' button selected");
        } else {
            Log.w("ManageEmployees", "   ⚠️ Unknown role filter: " + roleFilter);
        }
    }

    /**
     * Load employees - uses SAME method as AdminDashboardFragment (FirebaseRTDBHelper.getAllEmployees)
     * Filters: isActive == true AND role != "Administrator"
     */
    private void loadEmployees() {
        // Show loading state
        emptyStateTextView.setVisibility(View.GONE);
        employeesRecyclerView.setVisibility(View.GONE);
        
        // Use FirebaseRTDBHelper.getAllEmployees() - SAME as AdminDashboardFragment
        if (firebaseRTDBHelper != null) {
            Log.d("ManageEmployees", "🔥 Loading employees using FirebaseRTDBHelper (same as dashboard)");
            firebaseRTDBHelper.getAllEmployees(employees -> {
                if (employees == null) {
                    employees = new ArrayList<>();
                }
                
                Log.d("ManageEmployees", "🔥 Firebase data received: " + employees.size() + " total employees from Firebase");
                if (employees.size() > 0) {
                    Log.d("ManageEmployees", "   📋 First few employees from Firebase: ");
                    for (int i = 0; i < Math.min(5, employees.size()); i++) {
                        Employee emp = employees.get(i);
                        if (emp != null) {
                            Log.d("ManageEmployees", "      [" + (i+1) + "] ID: " + emp.getEmployeeId() + 
                                  ", Name: " + emp.getFullName() + 
                                  ", Role: " + (emp.getRole() != null ? emp.getRole() : "NULL") + 
                                  ", isActive: " + emp.isActive());
                        }
                    }
                    if (employees.size() > 5) {
                        Log.d("ManageEmployees", "      ... and " + (employees.size() - 5) + " more");
                    }
                } else {
                    Log.w("ManageEmployees", "   ⚠️ WARNING: No employees returned from Firebase! Check Firebase connection and data.");
                    Log.w("ManageEmployees", "   ⚠️ If you can log in, employees exist - check Firebase RTDB path 'employees'");
                }
                Log.d("ManageEmployees", "🔍 Current role filter: " + (roleFilter != null ? roleFilter : "ALL (no filter)"));
                
                // Apply same filtering logic as AdminDashboardFragment (includes role filter if set)
                // This ensures the count matches exactly with the dashboard card count
                List<Employee> filteredEmployees = filterActiveNonAdminEmployees(employees);
                
                int filteredCount = filteredEmployees.size();
                Log.d("ManageEmployees", "✅ Filtered to: " + filteredCount + " employees (filter: " + (roleFilter != null ? roleFilter : "ALL") + ")");
                
                // Validation logging
                if (roleFilter != null && !roleFilter.isEmpty()) {
                    // Role filter is active - should match specific role count
                    Log.d("ManageEmployees", "🎯 Role filter active: '" + roleFilter + "' - Showing " + filteredCount + " employees");
                    Log.d("ManageEmployees", "   → This count SHOULD MATCH the dashboard card count for " + roleFilter);
                    Log.d("ManageEmployees", "   → All employees shown have role='" + roleFilter + "', isActive=true, and are not administrators");
                } else {
                    // No role filter - should match Total Employees count
                    Log.d("ManageEmployees", "📊 Showing ALL active non-admin employees: " + filteredCount);
                    Log.d("ManageEmployees", "   → This count SHOULD MATCH 'Total Employees' count on dashboard: " + filteredCount);
                    Log.d("ManageEmployees", "   → All employees shown have isActive=true and role != 'Administrator'");
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (getContext() == null || getView() == null) {
                            return; // Fragment is detached
                        }
                        
                        if (filteredEmployees.isEmpty()) {
                            emptyStateTextView.setVisibility(View.VISIBLE);
                            employeesRecyclerView.setVisibility(View.GONE);
                        } else {
                            emptyStateTextView.setVisibility(View.GONE);
                            employeesRecyclerView.setVisibility(View.VISIBLE);
                            
                            // Limit display to MAX_EMPLOYEES_DISPLAY (3,000 employees)
                            List<Employee> employeesToDisplay = filteredEmployees;
                            if (filteredEmployees.size() > MAX_EMPLOYEES_DISPLAY) {
                                employeesToDisplay = new ArrayList<>(filteredEmployees.subList(0, MAX_EMPLOYEES_DISPLAY));
                                Log.w("ManageEmployees", "⚠️ Employee list exceeds " + MAX_EMPLOYEES_DISPLAY + " limit!");
                                Log.w("ManageEmployees", "   → Displaying first " + MAX_EMPLOYEES_DISPLAY + " employees out of " + filteredEmployees.size() + " total");
                                showToast("Showing first " + MAX_EMPLOYEES_DISPLAY + " employees (total: " + filteredEmployees.size() + ")");
                            }
                            
                            employeeAdapter.setEmployees(employeesToDisplay);
                            Log.d("ManageEmployees", "✅ UI updated with " + employeesToDisplay.size() + " employees displayed (filtered: " + filteredCount + " total)");
                        }
                    });
                }
            });
        } else {
            // No FirebaseRTDBHelper - use SQLite fallback
            Log.w("ManageEmployees", "⚠️ FirebaseRTDBHelper not available, using SQLite fallback");
            loadEmployeesFromSQLite();
        }
    }
    
    /**
     * Load employees from SQLite (fallback)
     * Uses same filtering: isActive = 1 AND role != 'Administrator'
     */
    private void loadEmployeesFromSQLite() {
        // Load employees in background thread to avoid blocking UI
        com.healthcare.cas.utils.DatabaseExecutor.getInstance().execute(() -> {
            List<Employee> employees = databaseHelper.getAllEmployees();
            
            // Apply same filtering logic as AdminDashboardFragment for consistency
            List<Employee> filteredEmployees = filterActiveNonAdminEmployees(employees);
            
            // Update UI on main thread
            com.healthcare.cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                if (getContext() == null || getView() == null) {
                    return; // Fragment is detached
                }
                
                if (filteredEmployees.isEmpty()) {
                    emptyStateTextView.setVisibility(View.VISIBLE);
                    employeesRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateTextView.setVisibility(View.GONE);
                    employeesRecyclerView.setVisibility(View.VISIBLE);
                    
                    // Limit display to MAX_EMPLOYEES_DISPLAY (3,000 employees)
                    List<Employee> employeesToDisplay = filteredEmployees;
                    if (filteredEmployees.size() > MAX_EMPLOYEES_DISPLAY) {
                        employeesToDisplay = new ArrayList<>(filteredEmployees.subList(0, MAX_EMPLOYEES_DISPLAY));
                        Log.w("ManageEmployees", "⚠️ Employee list exceeds " + MAX_EMPLOYEES_DISPLAY + " limit!");
                        Log.w("ManageEmployees", "   → Displaying first " + MAX_EMPLOYEES_DISPLAY + " employees out of " + filteredEmployees.size() + " total");
                    }
                    
                    employeeAdapter.setEmployees(employeesToDisplay);
                }
            });
        });
    }
    
    /**
     * Filter employees - EXACT SAME LOGIC as AdminDashboardFragment.calculateAndUpdateStatistics()
     * Filters: isActive == true AND role != "Administrator"
     * If roleFilter is set, also filters by specific role (Nurse, Doctor, Pharmacist)
     */
    private List<Employee> filterActiveNonAdminEmployees(List<Employee> employees) {
        if (employees == null) {
            employees = new ArrayList<>();
        }
        
        Log.d("ManageEmployees", "🔍 Filtering " + employees.size() + " employees...");
        Log.d("ManageEmployees", "   → Filter criteria: isActive=true AND role != 'Administrator'");
        if (roleFilter != null && !roleFilter.isEmpty()) {
            Log.d("ManageEmployees", "   → Additional filter: role = '" + roleFilter + "'");
        }
        
        List<Employee> filtered = new ArrayList<>();
        int skippedNull = 0;
        int skippedInactive = 0;
        int skippedAdmin = 0;
        int skippedRoleFilter = 0;
        
        for (Employee emp : employees) {
            if (emp == null) {
                skippedNull++;
                continue;
            }
            
            // CRITICAL: Check if employee is active
            // Deleted employees have is_active = false and should NEVER appear in the list
            // This is the PRIMARY protection against deleted employees coming back
            // Note: isActive() returns boolean (primitive), so it can never be null
            // Only skip if explicitly set to false (deleted/inactive)
            boolean isActive = emp.isActive();
            if (!isActive) {
                // Employee is deleted or inactive - skip it permanently
                skippedInactive++;
                Log.d("ManageEmployees", "   ⏭️ Skipped deleted/inactive employee: " + emp.getEmployeeId() + " (isActive=false) - will NOT appear in list");
                continue; // Skip inactive/deleted employees - they are permanently filtered out
            }
            // If isActive is true (default), include the employee
            
            // Check if employee is not an administrator - SAME LOGIC as AdminDashboardFragment
            String role = emp.getRole();
            if (role == null) {
                skippedAdmin++;
                Log.d("ManageEmployees", "   ⏭️ Skipped employee " + emp.getEmployeeId() + " - null role");
                continue; // Skip null role
            }
            
            String roleTrimmed = role.trim();
            // SAME CHECK as AdminDashboardFragment: "Administrator".equals(roleTrimmed)
            if (roleTrimmed.isEmpty() || "Administrator".equals(roleTrimmed)) {
                skippedAdmin++;
                Log.d("ManageEmployees", "   ⏭️ Skipped administrator: " + emp.getEmployeeId() + " (role: '" + roleTrimmed + "')");
                continue; // Skip administrators
            }
            
            // Apply role filter if specified (from dashboard card click or filter button)
            // This ensures only employees of the selected role are shown (Nurse, Doctor, or Pharmacist)
            if (roleFilter != null && !roleFilter.isEmpty()) {
                // Case-insensitive role matching (e.g., "Nurse" matches "nurse", "Doctor" matches "doctor")
                String roleFilterTrimmed = roleFilter.trim();
                String roleTrimmedLower = roleTrimmed.toLowerCase();
                String filterLower = roleFilterTrimmed.toLowerCase();
                
                if (!filterLower.equals(roleTrimmedLower)) {
                    skippedRoleFilter++;
                    Log.d("ManageEmployees", "   ⏭️ Skipped employee " + emp.getEmployeeId() + " - role '" + roleTrimmed + "' doesn't match filter '" + roleFilterTrimmed + "'");
                    continue; // Skip if role doesn't match filter
                } else {
                    Log.d("ManageEmployees", "   ✅ Role match: employee " + emp.getEmployeeId() + " - role '" + roleTrimmed + "' matches filter '" + roleFilterTrimmed + "'");
                }
            }
            
            filtered.add(emp);
            Log.d("ManageEmployees", "   ✅ INCLUDED: " + emp.getEmployeeId() + " (Role: " + roleTrimmed + ", isActive: " + emp.isActive() + ")");
        }
        
        int finalCount = filtered.size();
        Log.d("ManageEmployees", "📊 Filtering results:");
        Log.d("ManageEmployees", "   ✅ Included: " + finalCount + " employees");
        Log.d("ManageEmployees", "   ⏭️ Skipped (null): " + skippedNull);
        Log.d("ManageEmployees", "   ⏭️ Skipped (inactive/deleted): " + skippedInactive);
        Log.d("ManageEmployees", "   ⏭️ Skipped (admin/null role): " + skippedAdmin);
        if (roleFilter != null && !roleFilter.isEmpty()) {
            Log.d("ManageEmployees", "   ⏭️ Skipped (role filter mismatch): " + skippedRoleFilter);
        }
        Log.d("ManageEmployees", "🔍 Filtered employees: " + finalCount + " (role filter: " + (roleFilter != null ? roleFilter : "ALL") + ")");
        
        // Final validation message - show employee IDs for verification
        if (roleFilter != null && !roleFilter.isEmpty()) {
            Log.d("ManageEmployees", "✓✓✓ Final count for role '" + roleFilter + "': " + finalCount + " employees");
            if (finalCount > 0) {
                StringBuilder employeeList = new StringBuilder("   → Employees shown: ");
                for (int i = 0; i < Math.min(filtered.size(), 10); i++) { // Show first 10
                    employeeList.append(filtered.get(i).getEmployeeId());
                    if (i < Math.min(filtered.size(), 10) - 1) {
                        employeeList.append(", ");
                    }
                }
                if (filtered.size() > 10) {
                    employeeList.append("... (and ").append(filtered.size() - 10).append(" more)");
                }
                Log.d("ManageEmployees", employeeList.toString());
            }
        }
        
        return filtered;
    }

    /**
     * Setup real-time listener for employees from Firebase
     * This will automatically update the UI when employees are added/updated/deleted
     * Uses ChildEventListener for instant updates (add, change, remove events)
     */
    private void setupRealtimeListener() {
        if (firebaseHelper == null) {
            Log.w("ManageEmployees", "FirebaseHelper not available - real-time updates disabled");
            return;
        }

        // Use direct Firebase Database reference for better real-time performance
        try {
            com.google.firebase.database.DatabaseReference employeesRef = com.google.firebase.database.FirebaseDatabase.getInstance(
                    "https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("employees");

            // Use ChildEventListener for instant real-time updates
            // All event handlers simply reload employees using loadEmployees() which applies all filters:
            // 1. Active employees only (is_active = true)
            // 2. Non-administrators only
            // 3. Role filter (if set from dashboard card click)
            com.google.firebase.database.ChildEventListener childListener = new com.google.firebase.database.ChildEventListener() {
                @Override
                public void onChildAdded(com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {
                    // New employee added to Firebase
                    // CRITICAL: Check if this employee was previously deleted from SQLite before syncing
                    String employeeId = snapshot.getKey();
                    if (employeeId != null) {
                        // Check if employee exists in SQLite - if not, it was deleted, don't restore
                        Employee existing = databaseHelper.getEmployeeById(employeeId);
                        if (existing == null) {
                            Log.d("ManageEmployees", "⚠️ onChildAdded: Employee " + employeeId + " doesn't exist in SQLite - NOT restoring (was deleted)");
                            Log.d("ManageEmployees", "   → Deleted employees will NOT be restored from Firebase");
                            return; // Don't restore deleted employees
                        }
                    }
                    
                    // Employee exists in SQLite, safe to reload
                    Log.d("ManageEmployees", "🔥 onChildAdded triggered - reloading employees (applying filters: active only, role=" + (roleFilter != null ? roleFilter : "ALL") + ")");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> loadEmployees());
                    }
                }

                @Override
                public void onChildChanged(com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {
                    // Employee data updated (including deletion - is_active set to false)
                    // Reload all employees to ensure deleted employees stay deleted and role filters are applied
                    Log.d("ManageEmployees", "🔥 onChildChanged triggered - reloading employees (deleted employees will be filtered out, role=" + (roleFilter != null ? roleFilter : "ALL") + ")");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> loadEmployees());
                    }
                }

                @Override
                public void onChildRemoved(com.google.firebase.database.DataSnapshot snapshot) {
                    // Employee removed from Firebase - reload all employees to ensure consistency
                    Log.d("ManageEmployees", "🔥 onChildRemoved triggered - reloading employees");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> loadEmployees());
                    }
                }

                @Override
                public void onChildMoved(com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {
                    // Handle if needed (usually not needed for employees)
                }

                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {
                    Log.e("ManageEmployees", "❌ Real-time listener cancelled: " + error.getMessage());
                    // Fallback to SQLite
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> loadEmployees());
                    }
                }
            };

            // Add listener
            employeesListener = employeesRef.addChildEventListener(childListener);
            
            // Initial load is handled by loadEmployees() which uses FirebaseRTDBHelper (same as dashboard)
            // The real-time listener will handle updates
            Log.d("ManageEmployees", "✅ Real-time listener attached - initial load handled by loadEmployees()");

            Log.d("ManageEmployees", "✅ Real-time listener setup complete (ChildEventListener)");
        } catch (Exception e) {
            Log.e("ManageEmployees", "❌ Failed to setup real-time listener", e);
            loadEmployees(); // Fallback to SQLite
        }
    }

    /**
     * Convert Firebase DataSnapshot to Employee object
     */
    private Employee convertSnapshotToEmployee(com.google.firebase.database.DataSnapshot snapshot) {
        try {
            Map<String, Object> data = (Map<String, Object>) snapshot.getValue();
            if (data == null) return null;

            Employee employee = new Employee();
            
            // Get employee_id from data, or use snapshot key as fallback
            if (data.containsKey("employee_id")) {
                employee.setEmployeeId(String.valueOf(data.get("employee_id")));
            } else {
                // Fallback: use snapshot key as employee_id (for backward compatibility)
                employee.setEmployeeId(snapshot.getKey());
            }
            
            if (data.containsKey("first_name")) {
                employee.setFirstName(String.valueOf(data.get("first_name")));
            }
            if (data.containsKey("middle_name")) {
                employee.setMiddleName(String.valueOf(data.get("middle_name")));
            }
            if (data.containsKey("last_name")) {
                employee.setLastName(String.valueOf(data.get("last_name")));
            }
            if (data.containsKey("date_of_birth")) {
                employee.setDateOfBirth(String.valueOf(data.get("date_of_birth")));
            }
            if (data.containsKey("email")) {
                employee.setEmail(String.valueOf(data.get("email")));
            }
            if (data.containsKey("phone")) {
                employee.setPhone(String.valueOf(data.get("phone")));
            }
            if (data.containsKey("role")) {
                employee.setRole(String.valueOf(data.get("role")));
            }
            if (data.containsKey("username")) {
                employee.setUsername(String.valueOf(data.get("username")));
            }
            if (data.containsKey("is_active")) {
                Object isActive = data.get("is_active");
                if (isActive instanceof Boolean) {
                    employee.setActive((Boolean) isActive);
                } else if (isActive instanceof Number) {
                    employee.setActive(((Number) isActive).intValue() == 1);
                } else {
                    employee.setActive(true); // Default to active
                }
            } else {
                employee.setActive(true); // Default to active if not specified
            }

            return employee;
        } catch (Exception e) {
            Log.e("ManageEmployees", "Error converting snapshot to employee: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Sync employee from Firebase to SQLite database
     */
    private void syncEmployeeToSQLite(Employee employee) {
        if (databaseHelper == null || employee == null) return;

        com.healthcare.cas.utils.DatabaseExecutor.getInstance().execute(() -> {
            try {
                // Check if employee already exists
                Employee existing = databaseHelper.getEmployeeById(employee.getEmployeeId());
                
                // CRITICAL PROTECTION: If employee was previously deleted (is_active = false), don't restore it
                // This prevents deleted employees from reappearing when Firebase syncs
                // This is a SECONDARY protection - primary protection is the filtering logic
                if (existing != null && !existing.isActive()) {
                    Log.d("ManageEmployees", "⚠️ Employee " + employee.getEmployeeId() + " was deleted - NOT restoring from Firebase");
                    Log.d("ManageEmployees", "   → Deleted employees should stay deleted and NEVER appear in the list");
                    Log.d("ManageEmployees", "   → Employee will remain filtered out by isActive check");
                    return; // Don't sync deleted employees - they stay deleted permanently
                }
                
                // Also check if the incoming employee data has is_active=false
                // This prevents syncing employees that are already marked as deleted in Firebase
                if (employee != null && !employee.isActive()) {
                    Log.d("ManageEmployees", "⚠️ Incoming employee " + employee.getEmployeeId() + " has is_active=false - NOT syncing");
                    Log.d("ManageEmployees", "   → Employee is marked as deleted in Firebase - will NOT be added/updated");
                    return; // Don't sync employees that are marked as deleted in Firebase
                }
                
                // CRITICAL: Only sync if employee exists in SQLite
                // If employee doesn't exist in SQLite, it was deleted (hard delete) - DON'T restore it
                if (existing == null) {
                    Log.d("ManageEmployees", "⚠️ Employee " + employee.getEmployeeId() + " doesn't exist in SQLite - NOT restoring from Firebase");
                    Log.d("ManageEmployees", "   → Employee was likely deleted - will NOT be restored even if it exists in Firebase");
                    Log.d("ManageEmployees", "   → This prevents deleted employees from reappearing after app restart");
                    return; // Don't restore deleted employees
                }
                
                // Employee exists in SQLite - only update if it's active
                if (existing.isActive() && employee.isActive()) {
                    boolean success = databaseHelper.updateEmployee(employee);
                    if (success) {
                        Log.d("ManageEmployees", "✅ Updated employee in SQLite: " + employee.getEmployeeId());
                    }
                }
                
                // Invalidate cache
                databaseHelper.invalidateEmployeeCache();
            } catch (Exception e) {
                Log.e("ManageEmployees", "❌ Error syncing employee to SQLite", e);
            }
        });
    }

    private void showAddEmployeeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add New Employee");
        
        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_employee, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        
        // Set up button click listeners
        MaterialButton saveButton = dialogView.findViewById(R.id.saveEmployeeButton);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelEmployeeButton);
        
        saveButton.setOnClickListener(v -> {
            // Handle save employee
            saveNewEmployee(dialogView, dialog);
        });
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void saveNewEmployee(View dialogView, AlertDialog dialog) {
        // Get input values
        String firstName = getTextFromView(dialogView, R.id.inputFirstName);
        String lastName = getTextFromView(dialogView, R.id.inputLastName);
        String username = getTextFromView(dialogView, R.id.inputUsername);
        String password = getTextFromView(dialogView, R.id.inputPassword);
        String email = getTextFromView(dialogView, R.id.inputEmail);
        String role = getTextFromView(dialogView, R.id.inputRole);
        
        // Validate inputs
        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || 
            password.isEmpty() || email.isEmpty() || role.isEmpty()) {
            showToast("Please fill in all required fields");
            return;
        }
        
        // Create new employee
        Employee newEmployee = new Employee();
        newEmployee.setEmployeeId("EMP" + System.currentTimeMillis());
        newEmployee.setFirstName(firstName);
        newEmployee.setLastName(lastName);
        newEmployee.setUsername(username);
        newEmployee.setPassword(password);
        newEmployee.setEmail(email);
        newEmployee.setRole(role);
        newEmployee.setActive(true);
        
        // Save to database
        boolean success = databaseHelper.addEmployee(newEmployee);
        
        if (success) {
            showToast("Employee added successfully!");
            dialog.dismiss();
            loadEmployees(); // Refresh the list
        } else {
            showToast("Failed to add employee. Username might already exist.");
        }
    }

    private String getTextFromView(View parentView, int viewId) {
        TextView textView = parentView.findViewById(viewId);
        return textView.getText() != null ? textView.getText().toString().trim() : "";
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Re-read role filter from arguments in case fragment was recreated
        if (getArguments() != null) {
            String newRoleFilter = getArguments().getString("FILTER_ROLE");
            if (newRoleFilter != null && !newRoleFilter.equals(roleFilter)) {
                roleFilter = newRoleFilter;
                Log.d("ManageEmployees", "🔍 Role filter updated from arguments: " + roleFilter);
                // Update filter button states when filter changes
                if (getView() != null) {
                    updateFilterButtonStates();
                }
            } else if (roleFilter == null && newRoleFilter == null) {
                // No filter - that's okay
            }
        }
        
        // Always refresh the employee list when fragment becomes visible
        // This ensures new employees created in CreateEmployeeFragment appear immediately
        Log.d("ManageEmployees", "🔄 Fragment resumed - refreshing employee list (filter: " + (roleFilter != null ? roleFilter : "ALL") + ")");
        
        // Update filter button states
        if (getView() != null) {
            updateFilterButtonStates();
        }
        
        // Invalidate cache to ensure fresh data
        if (databaseHelper != null) {
            databaseHelper.invalidateEmployeeCache();
        }
        
        // Load employees - this will get latest data and apply role filter
        loadEmployees();
        
        // Real-time listener should also handle updates from Firebase
        // If listener isn't set up yet, setupRealtimeListener was called in onViewCreated
    }

    @Override
    public void onPause() {
        super.onPause();
        // Keep listener active even when paused for real-time updates
        // Only remove if fragment is being destroyed
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove real-time listener when view is destroyed
        if (employeesListener != null) {
            try {
                com.google.firebase.database.DatabaseReference employeesRef = com.google.firebase.database.FirebaseDatabase.getInstance(
                        "https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .getReference("employees");
                
                employeesRef.removeEventListener(employeesListener);
                Log.d("ManageEmployees", "✅ Real-time listener removed");
            } catch (Exception e) {
                Log.e("ManageEmployees", "Error removing listener", e);
            }
        }
    }

    // RecyclerView Adapter for employees
    private class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {
        private List<Employee> employees = new ArrayList<>();

        public void setEmployees(List<Employee> newEmployees) {
            if (newEmployees == null) {
                newEmployees = new ArrayList<>();
            }
            
            // Use DiffUtil for efficient updates (only updates changed items)
            androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = 
                androidx.recyclerview.widget.DiffUtil.calculateDiff(new EmployeeDiffCallback(this.employees, newEmployees));
            
            this.employees.clear();
            this.employees.addAll(newEmployees);
            diffResult.dispatchUpdatesTo(this);
        }
        
        /**
         * Remove a specific employee by ID from the list
         * Only removes the employee with the exact matching ID
         */
        /**
         * Remove a specific employee by ID from the list
         * Only removes the employee with the exact matching ID
         * Returns true if employee was found and removed, false otherwise
         */
        public boolean removeEmployeeById(String employeeId) {
            if (employeeId == null || employees == null || employees.isEmpty()) {
                return false;
            }
            
            // Find and remove only the employee with matching ID
            for (int i = 0; i < employees.size(); i++) {
                Employee emp = employees.get(i);
                if (emp != null && emp.getEmployeeId() != null && emp.getEmployeeId().equals(employeeId)) {
                    // Found the employee to remove
                    employees.remove(i);
                    notifyItemRemoved(i);
                    notifyItemRangeChanged(i, employees.size());
                    Log.d("EmployeeAdapter", "✅ Removed employee: " + employeeId + " at position " + i);
                    return true; // Successfully removed
                }
            }
            
            Log.w("EmployeeAdapter", "⚠️ Employee not found in list: " + employeeId);
            return false; // Employee not found
        }
        
        // DiffUtil callback for efficient RecyclerView updates
        private class EmployeeDiffCallback extends androidx.recyclerview.widget.DiffUtil.Callback {
            private List<Employee> oldList;
            private List<Employee> newList;
            
            public EmployeeDiffCallback(List<Employee> oldList, List<Employee> newList) {
                this.oldList = oldList;
                this.newList = newList;
            }
            
            @Override
            public int getOldListSize() {
                return oldList.size();
            }
            
            @Override
            public int getNewListSize() {
                return newList.size();
            }
            
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return oldList.get(oldItemPosition).getEmployeeId().equals(newList.get(newItemPosition).getEmployeeId());
            }
            
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Employee oldEmployee = oldList.get(oldItemPosition);
                Employee newEmployee = newList.get(newItemPosition);
                return oldEmployee.getFirstName().equals(newEmployee.getFirstName()) &&
                       oldEmployee.getLastName().equals(newEmployee.getLastName()) &&
                       oldEmployee.getRole().equals(newEmployee.getRole()) &&
                       oldEmployee.isActive() == newEmployee.isActive();
            }
        }

        @NonNull
        @Override
        public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
            return new EmployeeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
            Employee employee = employees.get(position);
            holder.bind(employee);
        }

        @Override
        public int getItemCount() {
            return employees != null ? employees.size() : 0;
        }

        class EmployeeViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView cardView;
            private TextView employeeIdText;
            private TextView employeeNameText;
            private TextView employeeRoleText;
            private TextView employeeEmailText;
            private TextView employeeStatusText;
            private MaterialButton editButton;
            private MaterialButton deleteButton;

            public EmployeeViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.employeeCardView);
                employeeIdText = itemView.findViewById(R.id.employeeIdText);
                employeeNameText = itemView.findViewById(R.id.employeeNameText);
                employeeRoleText = itemView.findViewById(R.id.employeeRoleText);
                employeeEmailText = itemView.findViewById(R.id.employeeEmailText);
                employeeStatusText = itemView.findViewById(R.id.employeeStatusText);
                editButton = itemView.findViewById(R.id.editEmployeeButton);
                deleteButton = itemView.findViewById(R.id.deleteEmployeeButton);
            }

            public void bind(Employee employee) {
                employeeIdText.setText("ID: " + employee.getEmployeeId());
                employeeNameText.setText(employee.getFullName());
                employeeRoleText.setText("Role: " + employee.getRole());
                employeeEmailText.setText("Email: " + employee.getEmail());
                employeeStatusText.setText("Status: " + (employee.isActive() ? "Active" : "Inactive"));
                
                // Set status color
                if (employee.isActive()) {
                    employeeStatusText.setTextColor(getResources().getColor(R.color.success_green));
                } else {
                    employeeStatusText.setTextColor(getResources().getColor(R.color.error_red));
                }
                
                // Set up click listeners
                editButton.setOnClickListener(v -> showEditEmployeeDialog(employee));
                
                // CRITICAL: ALL displayed employees can be deleted - NO EXCEPTIONS
                // Delete button is visible and enabled for ALL employees in the list
                // When deleted, employee will be permanently removed (is_active = false)
                // Deleted employees will NEVER appear again because filtering logic skips isActive=false
                deleteButton.setOnClickListener(v -> {
                    Log.d("ManageEmployees", "🗑️ Delete button clicked for employee: " + employee.getEmployeeId() + " (" + employee.getFullName() + ")");
                    showDeleteConfirmation(employee);
                });
                deleteButton.setEnabled(true); // Always enabled - ALL employees can be deleted
                deleteButton.setVisibility(View.VISIBLE); // Always visible - no restrictions
            }
            
            private void showEditEmployeeDialog(Employee employee) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Edit Employee");
                
                // Inflate custom dialog layout
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_employee, null);
                builder.setView(dialogView);
                
                // Pre-fill with current data
                setTextInView(dialogView, R.id.inputFirstName, employee.getFirstName());
                setTextInView(dialogView, R.id.inputLastName, employee.getLastName());
                setTextInView(dialogView, R.id.inputUsername, employee.getUsername());
                setTextInView(dialogView, R.id.inputEmail, employee.getEmail());
                setTextInView(dialogView, R.id.inputRole, employee.getRole());
                
                AlertDialog dialog = builder.create();
                
                // Set up button click listeners
                MaterialButton saveButton = dialogView.findViewById(R.id.saveEmployeeButton);
                MaterialButton cancelButton = dialogView.findViewById(R.id.cancelEmployeeButton);
                
                saveButton.setOnClickListener(v -> {
                    updateEmployee(dialogView, dialog, employee);
                });
                
                cancelButton.setOnClickListener(v -> dialog.dismiss());
                
                dialog.show();
            }
            
            private void updateEmployee(View dialogView, AlertDialog dialog, Employee employee) {
                // Get updated values
                String firstName = getTextFromView(dialogView, R.id.inputFirstName);
                String lastName = getTextFromView(dialogView, R.id.inputLastName);
                String username = getTextFromView(dialogView, R.id.inputUsername);
                String email = getTextFromView(dialogView, R.id.inputEmail);
                String role = getTextFromView(dialogView, R.id.inputRole);
                
                // Validate inputs
                if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || 
                    email.isEmpty() || role.isEmpty()) {
                    showToast("Please fill in all required fields");
                    return;
                }
                
                // Update employee
                employee.setFirstName(firstName);
                employee.setLastName(lastName);
                employee.setUsername(username);
                employee.setEmail(email);
                employee.setRole(role);
                
                // Save to database
                boolean success = databaseHelper.updateEmployee(employee);
                
                if (success) {
                    showToast("Employee updated successfully!");
                    dialog.dismiss();
                    loadEmployees(); // Refresh the list
                } else {
                    showToast("Failed to update employee.");
                }
            }
            
            private void showDeleteConfirmation(Employee employee) {
                String employeeIdToDelete = employee.getEmployeeId(); // Store ID to ensure we delete the correct one
                String employeeNameToDelete = employee.getFullName();
                
                // ALL employees can be deleted - no restrictions
                // Deletion will permanently remove the employee (set is_active = false)
                // Deleted employees will disappear and never appear again
                new AlertDialog.Builder(getContext())
                    .setTitle("Delete Employee")
                    .setMessage("Are you sure you want to delete " + employeeNameToDelete + "?\n\nThis action cannot be undone. The employee will be permanently removed.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        Log.d("ManageEmployees", "🗑️ Deleting employee: " + employeeIdToDelete);
                        
                        // STEP 1: PERMANENTLY DELETE from Firebase first (remove the entire record)
                        // This ensures Firebase is updated first, then SQLite
                        try {
                            com.google.firebase.database.DatabaseReference employeesRef = 
                                com.google.firebase.database.FirebaseDatabase.getInstance(
                                    "https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                .getReference("employees").child(employeeIdToDelete);
                            
                            // HARD DELETE: Remove the entire employee record from Firebase
                            employeesRef.removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("ManageEmployees", "✅ Employee permanently deleted from Firebase: " + employeeIdToDelete);
                                    
                                    // STEP 2: PERMANENTLY DELETE from SQLite after Firebase succeeds
                                    boolean success = databaseHelper.deleteEmployee(employeeIdToDelete);
                                    if (success) {
                                        Log.d("ManageEmployees", "✅ Employee permanently deleted from SQLite: " + employeeIdToDelete);
                                        // Invalidate cache immediately
                                        databaseHelper.invalidateEmployeeCache();
                                        
                                        // Remove from adapter immediately (only the specific employee)
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                int sizeBefore = employeeAdapter.getItemCount();
                                                boolean removed = employeeAdapter.removeEmployeeById(employeeIdToDelete);
                                                
                                                if (removed) {
                                                    Log.d("ManageEmployees", "✅ Employee " + employeeIdToDelete + " removed from adapter");
                                                    
                                                    // Update empty state if list is now empty
                                                    if (employeeAdapter.getItemCount() == 0) {
                                                        emptyStateTextView.setVisibility(View.VISIBLE);
                                                        employeesRecyclerView.setVisibility(View.GONE);
                                                    } else {
                                                        // Employee was removed, ensure RecyclerView is visible
                                                        emptyStateTextView.setVisibility(View.GONE);
                                                        employeesRecyclerView.setVisibility(View.VISIBLE);
                                                    }
                                                } else {
                                                    Log.w("ManageEmployees", "⚠️ Employee " + employeeIdToDelete + " not found in adapter, reloading list");
                                                    // If not found in adapter, reload from database as fallback
                                                    loadEmployees();
                                                }
                                                
                                                showToast("Employee deleted successfully! They will not appear in the list anymore.");
                                                
                                                // CRITICAL: Reload employees to ensure deleted employee doesn't come back
                                                // The filtering logic (isActive check) will prevent deleted employees from showing
                                                // This ensures the deleted employee is permanently filtered out
                                                Log.d("ManageEmployees", "🔄 Reloading employees after deletion - deleted employee will be filtered out");
                                                loadEmployees();
                                            });
                                        }
                                    } else {
                                        Log.w("ManageEmployees", "⚠️ Failed to permanently delete employee from SQLite: " + employeeIdToDelete);
                                        showToast("Employee deleted from Firebase, but SQLite deletion failed.");
                                        // Still reload to refresh UI
                                        loadEmployees();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.w("ManageEmployees", "⚠️ Failed to permanently delete employee from Firebase: " + employeeIdToDelete, e);
                                    // Try to delete from SQLite anyway (best effort)
                                    boolean success = databaseHelper.deleteEmployee(employeeIdToDelete);
                                    if (success) {
                                        databaseHelper.invalidateEmployeeCache();
                                        showToast("Employee deleted from local database. Firebase deletion failed.");
                                        loadEmployees();
                                    } else {
                                        showToast("Failed to delete employee. Please try again.");
                                    }
                                });
                        } catch (Exception e) {
                            Log.e("ManageEmployees", "❌ Error deleting employee from Firebase: " + employeeIdToDelete, e);
                            // Fallback: Try SQLite deletion anyway
                            boolean success = databaseHelper.deleteEmployee(employeeIdToDelete);
                            if (success) {
                                databaseHelper.invalidateEmployeeCache();
                                showToast("Employee deleted from local database.");
                                loadEmployees();
                            } else {
                                showToast("Failed to delete employee. Please try again.");
                            }
                        }
                    })  
                    .setNegativeButton("Cancel", null)
                    .show();
            }
            
            private void setTextInView(View parentView, int viewId, String text) {
                TextView textView = parentView.findViewById(viewId);
                textView.setText(text);
            }
            
            private String getTextFromView(View parentView, int viewId) {
                TextView textView = parentView.findViewById(viewId);
                return textView.getText() != null ? textView.getText().toString().trim() : "";
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}