package com.example.h_cas;

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

import com.example.h_cas.database.FirebaseHelper;
import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Employee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ManageEmployeesFragment allows admins to view, add, edit, and manage all employees.
 */
public class ManageEmployeesFragment extends Fragment {

    private RecyclerView employeesRecyclerView;
    private TextView emptyStateTextView;
    private FloatingActionButton addEmployeeButton;
    private HCasDatabaseHelper databaseHelper;
    private EmployeeAdapter employeeAdapter;
    private FirebaseHelper firebaseHelper;
    private com.google.firebase.database.ChildEventListener employeesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_employees, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        initializeDatabase();
        setupRecyclerView();
        setupClickListeners();
        loadEmployees();
        setupRealtimeListener();
    }

    private void initializeViews(View view) {
        employeesRecyclerView = view.findViewById(R.id.employeesRecyclerView);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
        addEmployeeButton = view.findViewById(R.id.addEmployeeButton);
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
        try {
            firebaseHelper = new FirebaseHelper();
        } catch (Exception e) {
            Log.e("ManageEmployees", "Failed to initialize FirebaseHelper", e);
            firebaseHelper = null;
        }
    }

    private void setupRecyclerView() {
        employeeAdapter = new EmployeeAdapter();
        employeesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        employeesRecyclerView.setAdapter(employeeAdapter);
        // Performance optimizations
        employeesRecyclerView.setHasFixedSize(true); // RecyclerView size doesn't change
        employeesRecyclerView.setItemViewCacheSize(20); // Cache more views for smoother scrolling
    }

    private void setupClickListeners() {
        addEmployeeButton.setOnClickListener(v -> showAddEmployeeDialog());
    }

    private void loadEmployees() {
        // Show loading state
        emptyStateTextView.setVisibility(View.GONE);
        employeesRecyclerView.setVisibility(View.GONE);
        
        // Load employees in background thread to avoid blocking UI
        com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
            List<Employee> employees = databaseHelper.getAllEmployees();
            
            // Update UI on main thread
            com.example.h_cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                if (getContext() == null || getView() == null) {
                    return; // Fragment is detached
                }
                
                if (employees.isEmpty()) {
                    emptyStateTextView.setVisibility(View.VISIBLE);
                    employeesRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateTextView.setVisibility(View.GONE);
                    employeesRecyclerView.setVisibility(View.VISIBLE);
                    employeeAdapter.setEmployees(employees);
                }
            });
        });
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
            com.google.firebase.database.ChildEventListener childListener = new com.google.firebase.database.ChildEventListener() {
                private final List<Employee> currentEmployees = new ArrayList<>();

                @Override
                public void onChildAdded(com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {
                    // New employee added - instant update
                    try {
                        Employee employee = convertSnapshotToEmployee(snapshot);
                        if (employee != null && employee.isActive() && !"Administrator".equals(employee.getRole())) {
                            currentEmployees.add(employee);
                            syncEmployeeToSQLite(employee);
                            updateUI();
                            Log.d("ManageEmployees", "✅ Employee added (real-time): " + employee.getEmployeeId());
                        }
                    } catch (Exception e) {
                        Log.e("ManageEmployees", "Error processing added employee", e);
                    }
                }

                @Override
                public void onChildChanged(com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {
                    // Employee data updated - instant update
                    try {
                        Employee updatedEmployee = convertSnapshotToEmployee(snapshot);
                        if (updatedEmployee != null) {
                            // Update in list
                            for (int i = 0; i < currentEmployees.size(); i++) {
                                if (currentEmployees.get(i).getEmployeeId().equals(updatedEmployee.getEmployeeId())) {
                                    currentEmployees.set(i, updatedEmployee);
                                    break;
                                }
                            }
                            
                            if (updatedEmployee.isActive() && !"Administrator".equals(updatedEmployee.getRole())) {
                                syncEmployeeToSQLite(updatedEmployee);
                                updateUI();
                                Log.d("ManageEmployees", "✅ Employee updated (real-time): " + updatedEmployee.getEmployeeId());
                            } else {
                                // Employee deactivated, remove from list
                                currentEmployees.removeIf(emp -> emp.getEmployeeId().equals(updatedEmployee.getEmployeeId()));
                                updateUI();
                            }
                        }
                    } catch (Exception e) {
                        Log.e("ManageEmployees", "Error processing changed employee", e);
                    }
                }

                @Override
                public void onChildRemoved(com.google.firebase.database.DataSnapshot snapshot) {
                    // Employee removed - instant update
                    try {
                        String employeeId = snapshot.child("employee_id").getValue(String.class);
                        if (employeeId != null) {
                            currentEmployees.removeIf(emp -> emp.getEmployeeId().equals(employeeId));
                            updateUI();
                            Log.d("ManageEmployees", "✅ Employee removed (real-time): " + employeeId);
                        }
                    } catch (Exception e) {
                        Log.e("ManageEmployees", "Error processing removed employee", e);
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

                private void updateUI() {
                    if (getActivity() != null && getView() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Filter active employees only
                            List<Employee> activeEmployees = new ArrayList<>();
                            for (Employee emp : currentEmployees) {
                                if (emp.isActive() && !"Administrator".equals(emp.getRole())) {
                                    activeEmployees.add(emp);
                                }
                            }

                            if (activeEmployees.isEmpty()) {
                                emptyStateTextView.setVisibility(View.VISIBLE);
                                employeesRecyclerView.setVisibility(View.GONE);
                            } else {
                                emptyStateTextView.setVisibility(View.GONE);
                                employeesRecyclerView.setVisibility(View.VISIBLE);
                                employeeAdapter.setEmployees(activeEmployees);
                            }
                        });
                    }
                }
            };

            // Add listener
            employeesListener = employeesRef.addChildEventListener(childListener);
            
            // Also load initial data
            employeesRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    List<Employee> initialEmployees = new ArrayList<>();
                    for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                        Employee employee = convertSnapshotToEmployee(child);
                        if (employee != null && employee.isActive() && !"Administrator".equals(employee.getRole())) {
                            initialEmployees.add(employee);
                            syncEmployeeToSQLite(employee);
                        }
                    }
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (initialEmployees.isEmpty()) {
                                emptyStateTextView.setVisibility(View.VISIBLE);
                                employeesRecyclerView.setVisibility(View.GONE);
                            } else {
                                emptyStateTextView.setVisibility(View.GONE);
                                employeesRecyclerView.setVisibility(View.VISIBLE);
                                employeeAdapter.setEmployees(initialEmployees);
                            }
                        });
                    }
                    
                    Log.d("ManageEmployees", "✅ Initial load: " + initialEmployees.size() + " employees");
                }

                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {
                    Log.e("ManageEmployees", "❌ Initial load failed", error.toException());
                    loadEmployees(); // Fallback to SQLite
                }
            });

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
            
            if (data.containsKey("employee_id")) {
                employee.setEmployeeId(String.valueOf(data.get("employee_id")));
            }
            if (data.containsKey("first_name")) {
                employee.setFirstName(String.valueOf(data.get("first_name")));
            }
            if (data.containsKey("last_name")) {
                employee.setLastName(String.valueOf(data.get("last_name")));
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
                }
            }

            return employee;
        } catch (Exception e) {
            Log.e("ManageEmployees", "Error converting snapshot to employee", e);
            return null;
        }
    }

    /**
     * Sync employee from Firebase to SQLite database
     */
    private void syncEmployeeToSQLite(Employee employee) {
        if (databaseHelper == null || employee == null) return;

        com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
            try {
                // Check if employee already exists
                Employee existing = databaseHelper.getEmployeeById(employee.getEmployeeId());
                
                if (existing == null) {
                    // Employee doesn't exist, add it
                    boolean success = databaseHelper.addEmployee(employee);
                    if (success) {
                        Log.d("ManageEmployees", "✅ Synced new employee to SQLite: " + employee.getEmployeeId());
                    }
                } else {
                    // Employee exists, update it
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
        // Real-time listener will handle updates automatically
        // Only load from SQLite as fallback if Firebase is not available
        if (firebaseHelper == null) {
            loadEmployees();
        }
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
                deleteButton.setOnClickListener(v -> showDeleteConfirmation(employee));
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
                new AlertDialog.Builder(getContext())
                    .setTitle("Delete Employee")
                    .setMessage("Are you sure you want to delete " + employee.getFullName() + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        boolean success = databaseHelper.deleteEmployee(employee.getEmployeeId());
                        if (success) {
                            showToast("Employee deleted successfully!");
                            loadEmployees(); // Refresh the list
                        } else {
                            showToast("Failed to delete employee.");
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