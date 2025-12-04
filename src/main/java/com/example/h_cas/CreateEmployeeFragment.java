package com.example.h_cas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Employee;

/**
 * CreateEmployeeFragment handles the creation of new employee accounts
 * for different healthcare roles (Nurse, Doctor, Pharmacist).
 */
public class CreateEmployeeFragment extends Fragment {

    private TextInputEditText firstNameEditText;
    private TextInputEditText lastNameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText phoneEditText;
    private TextInputEditText employeeIdEditText;
    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private MaterialAutoCompleteTextView roleAutoCompleteTextView;
    private MaterialButton createEmployeeButton;

    private TextInputLayout firstNameLayout;
    private TextInputLayout lastNameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout phoneLayout;
    private TextInputLayout employeeIdLayout;
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout roleLayout;
    
    private HCasDatabaseHelper databaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_employee, container, false);
        
        initializeViews(view);
        setupRoleSpinner();
        setupCreateButton();
        initializeDatabase();
        
        return view;
    }

    private void initializeViews(View view) {
        // Text inputs
        firstNameEditText = view.findViewById(R.id.firstNameEditText);
        lastNameEditText = view.findViewById(R.id.lastNameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        phoneEditText = view.findViewById(R.id.phoneEditText);
        employeeIdEditText = view.findViewById(R.id.employeeIdEditText);
        usernameEditText = view.findViewById(R.id.usernameEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        roleAutoCompleteTextView = view.findViewById(R.id.roleAutoCompleteTextView);
        
        // Text input layouts
        firstNameLayout = view.findViewById(R.id.firstNameLayout);
        lastNameLayout = view.findViewById(R.id.lastNameLayout);
        emailLayout = view.findViewById(R.id.emailLayout);
        phoneLayout = view.findViewById(R.id.phoneLayout);
        employeeIdLayout = view.findViewById(R.id.employeeIdLayout);
        usernameLayout = view.findViewById(R.id.usernameLayout);
        passwordLayout = view.findViewById(R.id.passwordLayout);
        roleLayout = view.findViewById(R.id.roleLayout);
        
        createEmployeeButton = view.findViewById(R.id.createEmployeeButton);
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupRoleSpinner() {
        String[] roles = {"Select Role", "Nurse", "Doctor", "Pharmacist"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, roles);
        roleAutoCompleteTextView.setAdapter(adapter);
        roleAutoCompleteTextView.setText(roles[0], false);
        
        roleAutoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                roleLayout.setError("Please select a valid role");
                employeeIdEditText.setText(""); // Clear employee ID if role is deselected
            } else {
                roleLayout.setError(null);
                // Auto-generate employee ID based on selected role
                String selectedRole = roles[position];
                String generatedId = databaseHelper.generateNextEmployeeId(selectedRole);
                employeeIdEditText.setText(generatedId);
            }
        });
    }

    private void setupCreateButton() {
        createEmployeeButton.setOnClickListener(v -> createEmployee());
    }

    private void createEmployee() {
        if (validateInputs()) {
            // Show loading state
            createEmployeeButton.setEnabled(false);
            createEmployeeButton.setText("Creating Employee...");
            
            // Get input values
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String firstName = firstNameEditText.getText().toString().trim();
            String lastName = lastNameEditText.getText().toString().trim();
            String employeeId = employeeIdEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();
            String role = roleAutoCompleteTextView.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();
            
            // Create employee object for SQLite
            Employee employee = new Employee();
            employee.setEmployeeId(employeeId);
            employee.setFirstName(firstName);
            employee.setLastName(lastName);
            employee.setEmail(email);
            employee.setPhone(phone);
            employee.setRole(role);
            employee.setUsername(username);
            employee.setPassword(password);
            
            // Create Firebase Auth user
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();
                                
                                // Save employee data to Firebase Realtime Database
                                // Use lowercase "employees" to match FirebaseHelper path
                                DatabaseReference ref = FirebaseDatabase.getInstance(
                                        "https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                        .getReference("employees");
                                
                                HashMap<String, Object> employeeData = new HashMap<>();
                                employeeData.put("employee_id", employeeId);
                                employeeData.put("first_name", firstName);
                                employeeData.put("last_name", lastName);
                                employeeData.put("full_name", firstName + " " + lastName);
                                employeeData.put("email", email);
                                employeeData.put("phone", phone);
                                employeeData.put("role", role);
                                employeeData.put("username", username);
                                employeeData.put("password", password); // CRITICAL: Save password for forgot password feature
                                employeeData.put("is_active", true);
                                employeeData.put("created_at", System.currentTimeMillis());
                                employeeData.put("created_by", "admin"); // Or get current admin user
                                
                                ref.child(uid).setValue(employeeData)
                                        .addOnSuccessListener(aVoid -> {
                                            android.util.Log.d("CreateEmployee", "✅ Employee saved to Firebase: " + uid);
                                            
                                            // Also save to SQLite database
                                            boolean sqliteSuccess = databaseHelper.addEmployee(employee);
                                            
                                            if (sqliteSuccess) {
                                                android.util.Log.d("CreateEmployee", "✅ Employee saved to SQLite: " + employee.getEmployeeId());
                                                
                                                // Invalidate cache to refresh employee list
                                                databaseHelper.invalidateEmployeeCache();
                                                
                                                Toast.makeText(getContext(), "Employee created successfully!", Toast.LENGTH_SHORT).show();
                                                clearForm();
                                                
                                                // Refresh employee list if ManageEmployeesFragment is active
                                                // This will be handled by onResume when user navigates back
                                            } else {
                                                android.util.Log.e("CreateEmployee", "❌ Failed to save employee to SQLite");
                                                Toast.makeText(getContext(), "Employee saved to Firebase but failed to save to local database. Please check logs.", Toast.LENGTH_LONG).show();
                                            }
                                            
                                            // Reset button state
                                            createEmployeeButton.setEnabled(true);
                                            createEmployeeButton.setText("Create Employee");
                                        })
                                        .addOnFailureListener(e -> {
                                            android.util.Log.e("CreateEmployee", "❌ Failed to save employee to Firebase: " + e.getMessage());
                                            e.printStackTrace();
                                            
                                            Toast.makeText(getContext(), "Failed to save employee to Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            
                                            // Reset button state
                                            createEmployeeButton.setEnabled(true);
                                            createEmployeeButton.setText("Create Employee");
                                        });
                            }
                        } else {
                            // Firebase Auth creation failed
                            Exception exception = task.getException();
                            String errorMessage = "Failed to create employee account.";
                            
                            if (exception != null) {
                                String exceptionMessage = exception.getMessage();
                                if (exceptionMessage != null) {
                                    if (exceptionMessage.contains("email-already-in-use")) {
                                        errorMessage = "This email is already registered.";
                                    } else if (exceptionMessage.contains("weak-password")) {
                                        errorMessage = "Password is too weak. Please use a stronger password.";
                                    } else if (exceptionMessage.contains("invalid-email")) {
                                        errorMessage = "Invalid email address format.";
                                    } else {
                                        errorMessage = "Error: " + exceptionMessage;
                                    }
                                }
                            }
                            
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                            
                            // Reset button state
                            createEmployeeButton.setEnabled(true);
                            createEmployeeButton.setText("Create Employee");
                        }
                    });
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;
        
        // Clear previous errors
        clearErrors();
        
        // Validate first name
        if (firstNameEditText.getText().toString().trim().isEmpty()) {
            firstNameLayout.setError("First name is required");
            isValid = false;
        }
        
        // Validate last name
        if (lastNameEditText.getText().toString().trim().isEmpty()) {
            lastNameLayout.setError("Last name is required");
            isValid = false;
        }
        
        // Validate email
        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Please enter a valid email");
            isValid = false;
        } else if (databaseHelper.isEmailExists(email)) {
            emailLayout.setError("Email already exists");
            isValid = false;
        }
        
        // Validate phone
        String phone = phoneEditText.getText().toString().trim();
        if (phone.isEmpty()) {
            phoneLayout.setError("Phone number is required");
            isValid = false;
        } else if (phone.length() < 10) {
            phoneLayout.setError("Please enter a valid phone number");
            isValid = false;
        }
        
        // Validate employee ID
        if (employeeIdEditText.getText().toString().trim().isEmpty()) {
            employeeIdLayout.setError("Employee ID is required");
            isValid = false;
        }
        
        // Validate username
        String username = usernameEditText.getText().toString().trim();
        if (username.isEmpty()) {
            usernameLayout.setError("Username is required");
            isValid = false;
        } else if (username.length() < 3) {
            usernameLayout.setError("Username must be at least 3 characters");
            isValid = false;
        } else if (databaseHelper.isUsernameExists(username)) {
            usernameLayout.setError("Username already exists");
            isValid = false;
        }
        
        // Validate password
        String password = passwordEditText.getText().toString().trim();
        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        }
        
        // Validate role
        String selectedRole = roleAutoCompleteTextView.getText().toString().trim();
        if (selectedRole.isEmpty() || selectedRole.equals("Select Role")) {
            roleLayout.setError("Please select a role");
            isValid = false;
        }
        
        return isValid;
    }

    private void clearErrors() {
        firstNameLayout.setError(null);
        lastNameLayout.setError(null);
        emailLayout.setError(null);
        phoneLayout.setError(null);
        employeeIdLayout.setError(null);
        usernameLayout.setError(null);
        passwordLayout.setError(null);
        roleLayout.setError(null);
    }

    private void clearForm() {
        firstNameEditText.setText("");
        lastNameEditText.setText("");
        emailEditText.setText("");
        phoneEditText.setText("");
        employeeIdEditText.setText("");
        usernameEditText.setText("");
        passwordEditText.setText("");
        roleAutoCompleteTextView.setText("Select Role", false);
    }
}
