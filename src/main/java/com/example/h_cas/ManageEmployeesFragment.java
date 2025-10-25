package com.example.h_cas;

import android.app.AlertDialog;
import android.os.Bundle;
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

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Employee;

import java.util.List;

/**
 * ManageEmployeesFragment allows admins to view, add, edit, and manage all employees.
 */
public class ManageEmployeesFragment extends Fragment {

    private RecyclerView employeesRecyclerView;
    private TextView emptyStateTextView;
    private FloatingActionButton addEmployeeButton;
    private HCasDatabaseHelper databaseHelper;
    private EmployeeAdapter employeeAdapter;

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
    }

    private void initializeViews(View view) {
        employeesRecyclerView = view.findViewById(R.id.employeesRecyclerView);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
        addEmployeeButton = view.findViewById(R.id.addEmployeeButton);
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupRecyclerView() {
        employeeAdapter = new EmployeeAdapter();
        employeesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        employeesRecyclerView.setAdapter(employeeAdapter);
    }

    private void setupClickListeners() {
        addEmployeeButton.setOnClickListener(v -> showAddEmployeeDialog());
    }

    private void loadEmployees() {
        List<Employee> employees = databaseHelper.getAllEmployees();
        
        if (employees.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            employeesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            employeesRecyclerView.setVisibility(View.VISIBLE);
            employeeAdapter.setEmployees(employees);
        }
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
        loadEmployees(); // Refresh when returning to this screen
    }

    // RecyclerView Adapter for employees
    private class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {
        private List<Employee> employees;

        public void setEmployees(List<Employee> employees) {
            this.employees = employees;
            notifyDataSetChanged();
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