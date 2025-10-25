package com.example.h_cas;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Employee;

/**
 * LoginActivity handles user authentication for the H-CAS healthcare application.
 * Provides a secure login interface with username/email and password fields.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private TextInputLayout usernameTextInputLayout;
    private TextInputLayout passwordTextInputLayout;
    private MaterialButton loginButton;
    private TextView forgotPasswordTextView;
    
    private HCasDatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_login);
            
            // Apply window insets for edge-to-edge display
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginFormContainer), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            initializeViews();
            setupClickListeners();
            initializeDatabase();
        } catch (Exception e) {
            // Log the error and show a simple error message
            System.err.println("Error in LoginActivity.onCreate: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "App initialization error. Please restart the app.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Initialize all view references from the layout
     */
    private void initializeViews() {
        try {
            usernameEditText = findViewById(R.id.usernameEditText);
            passwordEditText = findViewById(R.id.passwordEditText);
            usernameTextInputLayout = findViewById(R.id.usernameTextInputLayout);
            passwordTextInputLayout = findViewById(R.id.passwordTextInputLayout);
            loginButton = findViewById(R.id.loginButton);
            forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
            
            // Check if all views were found
            if (usernameEditText == null || passwordEditText == null || 
                usernameTextInputLayout == null || passwordTextInputLayout == null ||
                loginButton == null || forgotPasswordTextView == null) {
                throw new RuntimeException("One or more views not found in layout");
            }
        } catch (Exception e) {
            System.err.println("Error initializing views: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "View initialization error. Please restart the app.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Initialize database helper
     */
    private void initializeDatabase() {
        try {
            databaseHelper = new HCasDatabaseHelper(this);
            
            // Debug: Check if employees exist in database
            databaseHelper.debugCheckEmployees();
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Database initialization error. Please restart the app.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Setup click listeners for interactive elements
     */
    private void setupClickListeners() {
        // Login button click listener
        loginButton.setOnClickListener(v -> performLogin());

        // Forgot password click listener
        forgotPasswordTextView.setOnClickListener(v -> handleForgotPassword());

    }

    /**
     * Validates input fields and performs login authentication
     */
    private void performLogin() {
        // Clear previous errors
        clearErrors();

        // Get input values
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(username, password)) {
            return;
        }

        // Show loading state
        loginButton.setEnabled(false);
        loginButton.setText("Signing in...");

        // Simulate authentication process
        // In a real app, this would make an API call
        authenticateUser(username, password);
    }

    /**
     * Validates user input fields
     * @param username The username/email input
     * @param password The password input
     * @return true if validation passes, false otherwise
     */
    private boolean validateInputs(String username, String password) {
        boolean isValid = true;

        if (TextUtils.isEmpty(username)) {
            usernameTextInputLayout.setError("Username or email is required");
            isValid = false;
        } else if (username.length() < 3) {
            usernameTextInputLayout.setError("Username must be at least 3 characters");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordTextInputLayout.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            passwordTextInputLayout.setError("Password must be at least 6 characters");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Clears all error states from input fields
     */
    private void clearErrors() {
        usernameTextInputLayout.setError(null);
        passwordTextInputLayout.setError(null);
    }

    /**
     * Authenticates user using database
     * @param username The username/email
     * @param password The password
     */
    private void authenticateUser(String username, String password) {
        // Simulate network delay for better UX
        loginButton.postDelayed(() -> {
            try {
                // Debug: Check if database helper is working
                System.out.println("DEBUG: Attempting to authenticate user: " + username);
                
                // Authenticate user from database
                Employee employee = databaseHelper.authenticateUser(username, password);
                
                System.out.println("DEBUG: Authentication result: " + (employee != null ? "SUCCESS" : "FAILED"));
                
                if (employee != null) {
                    System.out.println("DEBUG: Employee role: " + employee.getRole());
                    System.out.println("DEBUG: Employee active: " + employee.isActive());
                    
                    // Successful login
                    if (employee.isAdmin()) {
                        // Admin login
                        Toast.makeText(this, "Welcome, Administrator!", Toast.LENGTH_SHORT).show();
                        
                        Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Regular employee login - route to role-specific dashboard
                        String welcomeMessage = "Welcome, " + employee.getFullName() + "!";
                        Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show();
                        
                        Intent intent;
                        switch (employee.getRole()) {
                            case "Doctor":
                                intent = new Intent(LoginActivity.this, DoctorDashboardActivity.class);
                                break;
                            case "Nurse":
                                intent = new Intent(LoginActivity.this, NurseDashboardActivity.class);
                                break;
                            case "Pharmacist":
                                intent = new Intent(LoginActivity.this, PharmacistDashboardActivity.class);
                                break;
                            default:
                                // For other roles (Lab Technician, Receptionist, etc.)
                                intent = new Intent(LoginActivity.this, MainActivity.class);
                                break;
                        }
                        // Pass employee data to the dashboard
                        intent.putExtra("EMPLOYEE_ID", employee.getEmployeeId());
                        intent.putExtra("FIRST_NAME", employee.getFirstName());
                        intent.putExtra("LAST_NAME", employee.getLastName());
                        intent.putExtra("FULL_NAME", employee.getFullName());
                        intent.putExtra("USERNAME", employee.getUsername());
                        intent.putExtra("ROLE", employee.getRole());
                        intent.putExtra("EMAIL", employee.getEmail());
                        startActivity(intent);
                        finish();
                    }
                } else {
                    // Failed login
                    Toast.makeText(this, "Invalid credentials. Please try again.", Toast.LENGTH_LONG).show();
                    
                    // Reset button state
                    loginButton.setEnabled(true);
                    loginButton.setText("Sign In");
                }
            } catch (Exception e) {
                System.out.println("DEBUG: Authentication error: " + e.getMessage());
                Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                
                // Reset button state
                loginButton.setEnabled(true);
                loginButton.setText("Sign In");
            }
        }, 1000); // 1 second delay for better UX
    }

    /**
     * Handles forgot password functionality
     */
    private void handleForgotPassword() {
        Toast.makeText(this, "Forgot password functionality will be implemented", Toast.LENGTH_SHORT).show();
        // TODO: Implement forgot password logic
        // This could open a dialog or navigate to a forgot password activity
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Reset button state when returning to this activity
        loginButton.setEnabled(true);
        loginButton.setText("Sign In");
    }

    /**
     * Test login method for debugging
     */
    private void testLogin() {
        // Test with admin credentials
        usernameEditText.setText("admin");
        passwordEditText.setText("admin123");
        performLogin();
    }
}
