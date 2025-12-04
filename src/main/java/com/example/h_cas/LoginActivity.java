package com.example.h_cas;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.database.FirebaseRTDBHelper;
import com.example.h_cas.models.Employee;
import com.example.h_cas.utils.DatabaseExecutor;
import com.example.h_cas.utils.DataMigrationHelper;

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
    
    private HCasDatabaseHelper databaseHelper; // Keep for fallback
    private FirebaseRTDBHelper firebaseRTDBHelper; // Primary database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_login);
            
            // Apply window insets for edge-to-edge display
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginFormContainer), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
                // Add bottom padding when keyboard is visible
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 
                    Math.max(systemBars.bottom, imeInsets.bottom));
                return insets;
            });

            initializeViews();
            // Setup scroll behavior when keyboard appears (after views are initialized)
            setupKeyboardHandling();
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
     * Setup keyboard handling to ensure input fields remain visible
     */
    private void setupKeyboardHandling() {
        if (usernameEditText == null || passwordEditText == null) {
            return; // Views not initialized yet
        }
        
        // Find ScrollView by traversing view hierarchy
        View rootView = findViewById(android.R.id.content);
        ScrollView scrollView = findScrollView(rootView);
        
        if (scrollView != null) {
            // Set up focus change listener to scroll to focused field
            usernameEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    scrollView.post(() -> {
                        int[] location = new int[2];
                        v.getLocationInWindow(location);
                        int scrollY = location[1] - (scrollView.getHeight() / 3);
                        scrollView.smoothScrollTo(0, Math.max(0, scrollY));
                    });
                }
            });
            
            passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    scrollView.post(() -> {
                        int[] location = new int[2];
                        v.getLocationInWindow(location);
                        int scrollY = location[1] - (scrollView.getHeight() / 3);
                        scrollView.smoothScrollTo(0, Math.max(0, scrollY));
                    });
                }
            });
        }
    }
    
    /**
     * Helper method to find ScrollView in view hierarchy
     */
    private ScrollView findScrollView(View view) {
        if (view instanceof android.widget.ScrollView) {
            return (android.widget.ScrollView) view;
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ScrollView result = findScrollView(group.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
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
     * Initialize database helper - Using Firebase RTDB as primary database
     * OPTIMIZED: Initialize in background to not block UI
     */
    private void initializeDatabase() {
        try {
            // Initialize Firebase RTDB as primary database
            firebaseRTDBHelper = new FirebaseRTDBHelper(this);
            
            // Create default accounts in background (non-blocking)
            DatabaseExecutor.getInstance().execute(() -> {
                DataMigrationHelper.createDefaultAccountsInFirebase(this);
            });
            
            // Keep SQLite as fallback (optional)
            try {
                databaseHelper = new HCasDatabaseHelper(this);
            } catch (Exception e) {
                // SQLite fallback not critical - Firebase is primary
            }
        } catch (Exception e) {
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

        // Authenticate user - navigation happens immediately on success
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
        } else {
            // Check if input looks like an email (contains @)
            if (username.contains("@")) {
                // Validate email format
                if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
                    usernameTextInputLayout.setError(getString(R.string.invalid_email));
                    isValid = false;
                }
                // Note: Email existence check moved to authentication to avoid blocking UI during validation
            } else {
                // Not an email format, treat as username
                if (username.length() < 3) {
                    usernameTextInputLayout.setError("Username must be at least 3 characters");
                    isValid = false;
                }
            }
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
     * Authenticates user using Firebase RTDB (primary database)
     * OPTIMIZED: Faster authentication flow
     * If RTDB authentication fails, tries Firebase Auth to handle password resets
     * @param username The username/email
     * @param password The password
     */
    private void authenticateUser(String username, String password) {
        // Use Firebase RTDB for authentication
        if (firebaseRTDBHelper != null) {
            firebaseRTDBHelper.authenticateUser(username, password, employee -> {
                // Direct navigation on main thread - no delays
                runOnUiThread(() -> {
                    if (employee != null) {
                        handleAuthenticationResult(employee);
                    } else {
                        // RTDB authentication failed - try Firebase Auth
                        // This handles cases where password was reset via Firebase Auth
                        tryFirebaseAuthAndSync(username, password);
                    }
                });
            });
        } else {
            // Fallback to SQLite if Firebase not available
            tryFallbackToSQLite(username, password);
        }
    }
    
    /**
     * Tries Firebase Auth authentication and syncs password to RTDB if successful
     * This handles password resets that were done via Firebase Auth
     * @param username The username/email
     * @param password The password
     */
    private void tryFirebaseAuthAndSync(String username, String password) {
        // Check if input is an email
        boolean isEmail = username != null && username.contains("@");
        String email = isEmail ? username : null;
        
        // If not an email, we can't use Firebase Auth (it requires email)
        if (!isEmail || email == null) {
            // Fallback to SQLite
            tryFallbackToSQLite(username, password);
            return;
        }
        
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null) {
            tryFallbackToSQLite(username, password);
            return;
        }
        
        Log.d("LoginActivity", "🔄 RTDB authentication failed, trying Firebase Auth for: " + email);
        
        // Try Firebase Auth authentication
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Firebase Auth authentication succeeded - password was reset
                    Log.d("LoginActivity", "✅ Firebase Auth authentication succeeded - password was reset");
                    Log.d("LoginActivity", "🔄 Syncing new password to RTDB...");
                    
                    // Sign out from Firebase Auth (we use RTDB for authentication, not Firebase Auth)
                    auth.signOut();
                    
                    // Get employee data from RTDB to update password
                    syncPasswordToRTDB(email, password, auth);
                } else {
                    // Firebase Auth also failed - try SQLite fallback
                    Log.d("LoginActivity", "❌ Firebase Auth authentication also failed");
                    tryFallbackToSQLite(username, password);
                }
            })
            .addOnFailureListener(e -> {
                Log.e("LoginActivity", "❌ Firebase Auth authentication error", e);
                tryFallbackToSQLite(username, password);
            });
    }
    
    /**
     * Syncs password to RTDB after successful Firebase Auth authentication
     * @param email The email address
     * @param newPassword The new password (from login input)
     * @param auth FirebaseAuth instance
     */
    private void syncPasswordToRTDB(String email, String newPassword, FirebaseAuth auth) {
        if (firebaseRTDBHelper == null) {
            Log.e("LoginActivity", "❌ Cannot sync password: FirebaseRTDBHelper is null");
            tryFallbackToSQLite(email, newPassword);
            return;
        }
        
        // Find employee in RTDB by email and update password
        DatabaseReference employeesRef = FirebaseDatabase.getInstance(
                "https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("employees");
        
        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean passwordUpdated = false;
                String employeeKey = null;
                
                if (snapshot.exists() && snapshot.hasChildren()) {
                    String normalizedEmail = email.toLowerCase().trim();
                    
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Object emailObj = child.child("email").getValue();
                        if (emailObj != null) {
                            String storedEmail = emailObj.toString().trim().toLowerCase();
                            
                            if (storedEmail.equals(normalizedEmail)) {
                                // Found the employee - update password
                                final String finalEmployeeKey = child.getKey(); // Make final for lambda
                                employeeKey = finalEmployeeKey;
                                child.getRef().child("password").setValue(newPassword)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("LoginActivity", "✅✅✅ Password synced to RTDB successfully ✅✅✅");
                                        Log.d("LoginActivity", "   Email: " + email);
                                        Log.d("LoginActivity", "   Employee key: " + finalEmployeeKey);
                                        
                                        // Now authenticate with RTDB to get employee data
                                        firebaseRTDBHelper.authenticateUser(email, newPassword, employee -> {
                                            runOnUiThread(() -> {
                                                if (employee != null) {
                                                    handleAuthenticationResult(employee);
                                                } else {
                                                    Log.e("LoginActivity", "❌ Failed to authenticate after password sync");
                                                    Toast.makeText(LoginActivity.this, "Password updated but authentication failed. Please try again.", Toast.LENGTH_LONG).show();
                                                    loginButton.setEnabled(true);
                                                    loginButton.setText("Sign In");
                                                }
                                            });
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("LoginActivity", "❌ Failed to sync password to RTDB", e);
                                        Toast.makeText(LoginActivity.this, "Password updated in Firebase Auth but failed to sync to database. Please contact administrator.", Toast.LENGTH_LONG).show();
                                        loginButton.setEnabled(true);
                                        loginButton.setText("Sign In");
                                    });
                                passwordUpdated = true;
                                break;
                            }
                        }
                    }
                }
                
                if (!passwordUpdated) {
                    Log.e("LoginActivity", "❌ Employee not found in RTDB for email: " + email);
                    Toast.makeText(LoginActivity.this, "Account not found in database. Please contact administrator.", Toast.LENGTH_LONG).show();
                    loginButton.setEnabled(true);
                    loginButton.setText("Sign In");
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("LoginActivity", "❌ Error syncing password to RTDB", error.toException());
                Toast.makeText(LoginActivity.this, "Error syncing password. Please try again.", Toast.LENGTH_LONG).show();
                loginButton.setEnabled(true);
                loginButton.setText("Sign In");
            }
        });
    }
    
    /**
     * Fallback authentication using SQLite
     * OPTIMIZED: Faster fallback flow
     */
    private void tryFallbackToSQLite(String username, String password) {
        DatabaseExecutor.getInstance().execute(() -> {
            try {
                Employee employee = databaseHelper != null ? 
                    databaseHelper.authenticateUser(username, password) : null;
                runOnUiThread(() -> {
                    if (employee != null) {
                        handleAuthenticationResult(employee);
                    } else {
                        Toast.makeText(this, "Invalid credentials. Please try again.", Toast.LENGTH_LONG).show();
                        loginButton.setEnabled(true);
                        loginButton.setText("Sign In");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    loginButton.setEnabled(true);
                    loginButton.setText("Sign In");
                });
            }
        });
    }
    
    /**
     * Handle authentication result (common for both Firebase and SQLite)
     * Navigates to loading screen which will then navigate to dashboard
     */
    private void handleAuthenticationResult(Employee employee) {
        try {
            if (employee != null) {
                // Double-check: Ensure employee is active (additional safety check)
                if (!employee.isActive()) {
                    Toast.makeText(this, getString(R.string.account_deleted), Toast.LENGTH_LONG).show();
                    loginButton.setEnabled(true);
                    loginButton.setText("Sign In");
                    return;
                }
                
                // Navigate to LoadingActivity which will show splash screen then navigate to dashboard
                Intent intent = new Intent(LoginActivity.this, LoadingActivity.class);
                
                // Pass employee data to LoadingActivity (with null safety checks)
                if (employee.getEmployeeId() != null) {
                    intent.putExtra("EMPLOYEE_ID", employee.getEmployeeId());
                } else {
                    intent.putExtra("EMPLOYEE_ID", "");
                }
                
                if (employee.getFirstName() != null) {
                    intent.putExtra("FIRST_NAME", employee.getFirstName());
                } else {
                    intent.putExtra("FIRST_NAME", "");
                }
                
                if (employee.getLastName() != null) {
                    intent.putExtra("LAST_NAME", employee.getLastName());
                } else {
                    intent.putExtra("LAST_NAME", "");
                }
                
                String fullName = employee.getFullName();
                if (fullName != null && !fullName.isEmpty()) {
                    intent.putExtra("FULL_NAME", fullName);
                } else {
                    // Fallback: construct full name from first and last name
                    String firstName = employee.getFirstName() != null ? employee.getFirstName() : "";
                    String lastName = employee.getLastName() != null ? employee.getLastName() : "";
                    String constructedFullName = (firstName + " " + lastName).trim();
                    intent.putExtra("FULL_NAME", constructedFullName.isEmpty() ? "User" : constructedFullName);
                }
                
                if (employee.getUsername() != null) {
                    intent.putExtra("USERNAME", employee.getUsername());
                } else {
                    intent.putExtra("USERNAME", "");
                }
                
                if (employee.getRole() != null) {
                    intent.putExtra("ROLE", employee.getRole());
                } else {
                    intent.putExtra("ROLE", "");
                }
                
                if (employee.getEmail() != null) {
                    intent.putExtra("EMAIL", employee.getEmail());
                } else {
                    intent.putExtra("EMAIL", "");
                }
                
                intent.putExtra("IS_ADMIN", employee.isAdmin());
                
                // Add flags for immediate navigation
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                
                // Navigate to loading screen which will then navigate to dashboard
                startActivity(intent);
                finish(); // Close login screen immediately
                
            } else {
                // Failed login - show error and reset button
                loginButton.setEnabled(true);
                loginButton.setText("Sign In");
                Toast.makeText(this, "Invalid credentials. Please try again.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            // Log the full error for debugging
            Log.e("LoginActivity", "Error in handleAuthenticationResult", e);
            e.printStackTrace();
            loginButton.setEnabled(true);
            loginButton.setText("Sign In");
            Toast.makeText(this, "Login error: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Handles forgot password functionality
     * Shows a dialog to enter email and sends password reset email via Firebase Auth
     */
    private void handleForgotPassword() {
        // Create a custom dialog view
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        
        TextInputLayout emailInputLayout = dialogView.findViewById(R.id.emailInputLayout);
        TextInputEditText emailEditText = dialogView.findViewById(R.id.emailEditText);
        
        // Pre-fill email if user entered it in username field
        String usernameOrEmail = usernameEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(usernameOrEmail) && Patterns.EMAIL_ADDRESS.matcher(usernameOrEmail).matches()) {
            emailEditText.setText(usernameOrEmail);
        }
        
        // Build the dialog
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.forgot_password_title)
                .setMessage(R.string.forgot_password_message)
                .setView(dialogView)
                .setPositiveButton(R.string.send_reset_link, null) // Set to null to handle click manually
                .setNegativeButton(R.string.cancel, null)
                .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            MaterialButton positiveButton = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            MaterialButton negativeButton = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            
            positiveButton.setOnClickListener(v -> {
                String email = emailEditText.getText().toString().trim();
                
                // Validate email
                if (TextUtils.isEmpty(email)) {
                    emailInputLayout.setError(getString(R.string.email_required));
                    return;
                }
                
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailInputLayout.setError(getString(R.string.invalid_email));
                    return;
                }
                
                // Clear error
                emailInputLayout.setError(null);
                
                // Disable button to prevent multiple clicks
                positiveButton.setEnabled(false);
                positiveButton.setText("Sending...");
                
                // Send password reset email via Firebase Auth
                sendPasswordResetEmail(email, dialog, positiveButton);
            });
            
            negativeButton.setOnClickListener(v -> dialog.dismiss());
        });
        
        dialog.show();
    }
    
    /**
     * Sends password reset email using Firebase Authentication
     * First checks if email exists in Firebase RTDB, then ensures user exists in Firebase Auth
     * @param email The email address to send reset link to
     * @param dialog The dialog to dismiss on success
     * @param button The button to re-enable on error
     */
    private void sendPasswordResetEmail(String email, AlertDialog dialog, MaterialButton button) {
        try {
            Log.d("LoginActivity", "=== FORGOT PASSWORD REQUEST ===");
            Log.d("LoginActivity", "Email: " + email);
            
            FirebaseAuth auth = FirebaseAuth.getInstance();
            
            if (auth == null) {
                String errorMsg = "Firebase Authentication is not available. Please check your configuration.";
                Log.e("LoginActivity", "❌ " + errorMsg);
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                button.setEnabled(true);
                button.setText(R.string.send_reset_link);
                return;
            }
            
            Log.d("LoginActivity", "✅ FirebaseAuth instance obtained");
            
            // Check Firebase App initialization
            try {
                com.google.firebase.FirebaseApp app = com.google.firebase.FirebaseApp.getInstance();
                Log.d("LoginActivity", "✅ Firebase App initialized: " + app.getName());
            } catch (Exception e) {
                Log.e("LoginActivity", "❌ Firebase App not initialized: " + e.getMessage());
            }
            
            // First, check if email exists in Firebase RTDB
            checkEmailInRTDBAndSendReset(email, auth, dialog, button);
            
        } catch (Exception e) {
            Log.e("LoginActivity", "❌ Error in sendPasswordResetEmail", e);
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            button.setEnabled(true);
            button.setText(R.string.send_reset_link);
        }
    }
    
    /**
     * Checks if email exists in Firebase RTDB, then sends password reset email
     * If user exists in RTDB but not in Firebase Auth, creates Firebase Auth account first
     * SECURITY: This method will REJECT any email that is not found in RTDB
     */
    private void checkEmailInRTDBAndSendReset(String email, FirebaseAuth auth, AlertDialog dialog, MaterialButton button) {
        if (firebaseRTDBHelper == null) {
            Log.e("LoginActivity", "❌ FirebaseRTDBHelper is null - cannot verify email existence");
            Log.e("LoginActivity", "   SECURITY: Rejecting password reset request - cannot verify email");
            String errorMsg = "System configuration error. Please contact administrator.";
            showPasswordResetErrorDialog(errorMsg, "system-error", "FirebaseRTDBHelper is null");
            button.setEnabled(true);
            button.setText(R.string.send_reset_link);
            return;
        }
        
        // Normalize email for comparison (trim and lowercase)
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        if (normalizedEmail.isEmpty()) {
            Log.e("LoginActivity", "❌ Empty email provided");
            Log.e("LoginActivity", "   SECURITY: Rejecting password reset request - empty email");
            showPasswordResetErrorDialog("Please enter a valid email address.", "invalid-email", "Empty email address");
            button.setEnabled(true);
            button.setText(R.string.send_reset_link);
            return;
        }
        
        // Validate email format
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!normalizedEmail.matches(emailPattern)) {
            Log.e("LoginActivity", "❌ Invalid email format: " + normalizedEmail);
            Log.e("LoginActivity", "   SECURITY: Rejecting password reset request - invalid email format");
            showPasswordResetErrorDialog("Invalid email address format. Please check your email and try again.", "invalid-email", "Email format validation failed");
            button.setEnabled(true);
            button.setText(R.string.send_reset_link);
            return;
        }
        
        Log.d("LoginActivity", "🔍🔍🔍 CHECKING EMAIL IN RTDB 🔍🔍🔍");
        Log.d("LoginActivity", "   Normalized email: " + normalizedEmail);
        Log.d("LoginActivity", "   Original email: " + email);
        Log.d("LoginActivity", "   SECURITY: Will ONLY proceed if email is found in RTDB");
        
        // Check Firebase RTDB for the email (scan all employees like authentication does)
        DatabaseReference employeesRef = FirebaseDatabase.getInstance(
                "https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("employees");
        
        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String storedPassword = null;
                boolean emailFound = false;
                String foundEmail = null; // For logging
                int totalEmployees = 0;
                int emailsChecked = 0;
                
                if (snapshot.exists() && snapshot.hasChildren()) {
                    totalEmployees = (int) snapshot.getChildrenCount();
                    Log.d("LoginActivity", "   RTDB contains " + totalEmployees + " employees");
                    Log.d("LoginActivity", "   Scanning all employees for matching email...");
                    
                    // Scan all employees to find matching email
                    for (DataSnapshot child : snapshot.getChildren()) {
                        emailsChecked++;
                        Object emailObj = child.child("email").getValue();
                        if (emailObj != null) {
                            String storedEmail = emailObj.toString().trim();
                            String normalizedStoredEmail = storedEmail.toLowerCase();
                            
                            // Log first 3 emails for debugging (without sensitive data)
                            if (emailsChecked <= 3) {
                                Log.d("LoginActivity", "   [" + emailsChecked + "] Checking: " + storedEmail + " (normalized: " + normalizedStoredEmail + ")");
                            }
                            
                            if (normalizedStoredEmail.equals(normalizedEmail)) {
                                // Email found in RTDB
                                emailFound = true;
                                foundEmail = storedEmail;
                                Object passwordObj = child.child("password").getValue();
                                storedPassword = passwordObj != null ? passwordObj.toString() : null;
                                Log.d("LoginActivity", "✅✅✅ EMAIL FOUND IN RTDB ✅✅✅");
                                Log.d("LoginActivity", "   Found at employee #" + emailsChecked + " of " + totalEmployees);
                                Log.d("LoginActivity", "   Found email: " + foundEmail);
                                Log.d("LoginActivity", "   Normalized match: " + normalizedStoredEmail + " == " + normalizedEmail);
                                Log.d("LoginActivity", "   Password exists: " + (storedPassword != null && !storedPassword.isEmpty()));
                                Log.d("LoginActivity", "   Password length: " + (storedPassword != null ? storedPassword.length() : 0));
                                break;
                            }
                        }
                    }
                    
                    if (!emailFound) {
                        Log.w("LoginActivity", "   Scanned all " + emailsChecked + " employees - email NOT found");
                    }
                } else {
                    Log.w("LoginActivity", "⚠️ No employees found in RTDB snapshot");
                    Log.w("LoginActivity", "   Snapshot exists: " + snapshot.exists());
                    Log.w("LoginActivity", "   Snapshot has children: " + (snapshot.hasChildren()));
                }
                
                // CRITICAL SECURITY CHECK: Only proceed if email is found AND password exists
                // This is the ONLY place where we allow Firebase Auth to be called
                if (emailFound && storedPassword != null && !storedPassword.isEmpty()) {
                    // User exists in RTDB - ensure they exist in Firebase Auth
                    Log.d("LoginActivity", "✅✅✅ EMAIL VERIFIED IN RTDB - PROCEEDING TO FIREBASE AUTH ✅✅✅");
                    Log.d("LoginActivity", "   This is the ONLY code path that calls Firebase Auth");
                    ensureUserInFirebaseAuth(normalizedEmail, storedPassword, auth, dialog, button);
                } else {
                    // Email not found in RTDB or password missing - REJECT the request
                    // DO NOT call Firebase Auth - this prevents emails from being sent to non-existent users
                    if (!emailFound) {
                        Log.e("LoginActivity", "❌❌❌ EMAIL NOT FOUND IN RTDB - REJECTING REQUEST ❌❌❌");
                        Log.e("LoginActivity", "   Searched email: " + normalizedEmail);
                        Log.e("LoginActivity", "   Total employees scanned: " + emailsChecked);
                        Log.e("LoginActivity", "   SECURITY: This email does not exist in the app. Password reset REJECTED.");
                        Log.e("LoginActivity", "   SECURITY: Firebase Auth will NOT be called for this email");
                        String errorMsg = "No account found with this email address. Please check your email and try again.";
                        showPasswordResetErrorDialog(errorMsg, "email-not-found", "Email not found in database");
                    } else {
                        Log.w("LoginActivity", "⚠️ Email found but password is missing or empty");
                        Log.w("LoginActivity", "   Found email: " + foundEmail);
                        Log.w("LoginActivity", "   SECURITY: Cannot proceed without password - rejecting request");
                        String errorMsg = "Your account exists but is missing password information. Please contact your administrator to reset your password.";
                        showPasswordResetErrorDialog(errorMsg, "account-config-error", "Email found in database but password is missing");
                    }
                    button.setEnabled(true);
                    button.setText(R.string.send_reset_link);
                    // CRITICAL: Do NOT call any Firebase Auth methods here - this prevents sending emails to non-existent users
                    Log.d("LoginActivity", "   SECURITY: No Firebase Auth methods called - request rejected");
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("LoginActivity", "❌❌❌ ERROR CHECKING EMAIL IN RTDB ❌❌❌");
                Log.e("LoginActivity", "   Error message: " + error.getMessage());
                Log.e("LoginActivity", "   Error code: " + error.getCode());
                Log.e("LoginActivity", "   SECURITY: Cannot verify email existence - rejecting request");
                Log.e("LoginActivity", "   SECURITY: Firebase Auth will NOT be called");
                // Cannot verify email existence - reject the request for security
                // DO NOT call Firebase Auth - this prevents emails from being sent when we can't verify
                String errorMsg = "Unable to verify email. Please check your connection and try again.";
                showPasswordResetErrorDialog(errorMsg, "database-error", error.getMessage());
                button.setEnabled(true);
                button.setText(R.string.send_reset_link);
            }
        });
    }
    
    /**
     * Ensures user exists in Firebase Auth, then sends password reset email
     */
    private void ensureUserInFirebaseAuth(String email, String password, FirebaseAuth auth, AlertDialog dialog, MaterialButton button) {
        Log.d("LoginActivity", "=== ENSURING USER IN FIREBASE AUTH ===");
        Log.d("LoginActivity", "Email: " + email);
        Log.d("LoginActivity", "Password length: " + (password != null ? password.length() : 0));
        
        // Check Firebase Auth configuration before proceeding
        checkFirebaseAuthConfiguration(auth, email);
        
        // First, try to send password reset directly
        attemptFirebaseAuthReset(email, auth, dialog, button, () -> {
            // If reset fails with "user-not-found", create the user in Firebase Auth
            Log.d("LoginActivity", "⚠️ User not found in Firebase Auth, will create account now...");
            createUserInFirebaseAuth(email, password, auth, dialog, button);
        });
    }
    
    /**
     * Checks Firebase Auth configuration and logs diagnostic information
     */
    private void checkFirebaseAuthConfiguration(FirebaseAuth auth, String email) {
        Log.d("LoginActivity", "=== FIREBASE AUTH CONFIGURATION CHECK ===");
        
        if (auth == null) {
            Log.e("LoginActivity", "❌ FirebaseAuth is NULL");
            return;
        }
        
        try {
            // Check Firebase App
            com.google.firebase.FirebaseApp app = com.google.firebase.FirebaseApp.getInstance();
            Log.d("LoginActivity", "✅ Firebase App: " + app.getName());
            Log.d("LoginActivity", "   App ID: " + app.getOptions().getApplicationId());
            Log.d("LoginActivity", "   Project ID: " + app.getOptions().getProjectId());
        } catch (Exception e) {
            Log.e("LoginActivity", "❌ Firebase App error: " + e.getMessage());
        }
        
        // Check current user
        try {
            com.google.firebase.auth.FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                Log.d("LoginActivity", "✅ Current Firebase Auth user: " + currentUser.getEmail());
                Log.d("LoginActivity", "   User ID: " + currentUser.getUid());
            } else {
                Log.w("LoginActivity", "⚠️ No current Firebase Auth user (this is OK for password reset)");
            }
        } catch (Exception e) {
            Log.w("LoginActivity", "⚠️ Could not get current user: " + e.getMessage());
        }
        
        // Check if email format is valid
        if (email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Log.d("LoginActivity", "✅ Email format is valid: " + email);
        } else {
            Log.e("LoginActivity", "❌ Email format is invalid: " + email);
        }
        
        Log.d("LoginActivity", "");
        Log.d("LoginActivity", "📋 FIREBASE CONSOLE CHECKLIST:");
        Log.d("LoginActivity", "   1. Go to Firebase Console → Authentication → Sign-in method");
        Log.d("LoginActivity", "   2. Enable 'Email/Password' provider");
        Log.d("LoginActivity", "   3. Enable 'Password reset' option (if available)");
        Log.d("LoginActivity", "   4. Go to Authentication → Templates → Password reset");
        Log.d("LoginActivity", "   5. Verify email template is configured");
        Log.d("LoginActivity", "   6. Check if email domain is allowed (some providers block certain domains)");
        Log.d("LoginActivity", "");
    }
    
    /**
     * Attempts to send password reset email via Firebase Auth
     * @param onUserNotFound Callback if user-not-found error occurs
     */
    private void attemptFirebaseAuthReset(String email, FirebaseAuth auth, AlertDialog dialog, MaterialButton button) {
        attemptFirebaseAuthReset(email, auth, dialog, button, null);
    }
    
    /**
     * Attempts to send password reset email via Firebase Auth
     * SECURITY: This method should ONLY be called after email is verified in RTDB
     * @param email Email address (must be verified in RTDB)
     * @param auth FirebaseAuth instance
     * @param dialog Dialog to dismiss on success
     * @param button Button to re-enable on error
     * @param onUserNotFound Callback if user-not-found error occurs (optional)
     */
    private void attemptFirebaseAuthReset(String email, FirebaseAuth auth, AlertDialog dialog, MaterialButton button, Runnable onUserNotFound) {
        Log.d("LoginActivity", "=== ATTEMPTING PASSWORD RESET VIA FIREBASE AUTH ===");
        Log.d("LoginActivity", "   Email: " + email);
        Log.d("LoginActivity", "   SECURITY: This method should only be called for emails verified in RTDB");
        Log.d("LoginActivity", "   FirebaseAuth instance: " + (auth != null ? "OK" : "NULL"));
        
        if (auth == null) {
            Log.e("LoginActivity", "❌ FirebaseAuth is null - cannot send reset email");
            Toast.makeText(LoginActivity.this, "Firebase Authentication error. Please try again later.", Toast.LENGTH_LONG).show();
                button.setEnabled(true);
                button.setText(R.string.send_reset_link);
                return;
            }
        
        // Check current user (for debugging)
        try {
            com.google.firebase.auth.FirebaseUser currentUser = auth.getCurrentUser();
            Log.d("LoginActivity", "Current Firebase Auth user: " + (currentUser != null ? currentUser.getEmail() : "null"));
        } catch (Exception e) {
            Log.d("LoginActivity", "Could not get current user: " + e.getMessage());
        }
        
        Log.d("LoginActivity", "📧 Calling auth.sendPasswordResetEmail() for: " + email);
        Log.d("LoginActivity", "   SECURITY: This email was verified in RTDB before calling Firebase Auth");
        Log.d("LoginActivity", "   Firebase Auth instance: " + (auth != null ? "VALID" : "NULL"));
        
        // Log Firebase App status
        try {
            com.google.firebase.FirebaseApp app = com.google.firebase.FirebaseApp.getInstance();
            Log.d("LoginActivity", "   Firebase App: " + app.getName() + " (initialized)");
        } catch (Exception e) {
            Log.e("LoginActivity", "   Firebase App error: " + e.getMessage());
        }
            
            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                    Log.d("LoginActivity", "=== PASSWORD RESET TASK COMPLETED ===");
                    Log.d("LoginActivity", "   Task successful: " + task.isSuccessful());
                    Log.d("LoginActivity", "   Email: " + email);
                    Log.d("LoginActivity", "   Thread: " + Thread.currentThread().getName());
                    
                        if (task.isSuccessful()) {
                            // Success - show message and dismiss dialog
                        Log.d("LoginActivity", "✅✅✅ PASSWORD RESET EMAIL SENT SUCCESSFULLY ✅✅✅");
                        Log.d("LoginActivity", "   Email sent to: " + email);
                        Log.d("LoginActivity", "   Firebase Auth confirmed the email was sent");
                        Log.d("LoginActivity", "   Timestamp: " + System.currentTimeMillis());
                        Log.d("LoginActivity", "");
                        Log.d("LoginActivity", "📬 IMPORTANT: If you don't receive the email, check:");
                        Log.d("LoginActivity", "   1. ⏰ Wait 2-5 minutes - email delivery can be delayed");
                        Log.d("LoginActivity", "   2. 📧 Check Spam/Junk folder in your email");
                        Log.d("LoginActivity", "   3. 🔍 Verify email address is correct: " + email);
                        Log.d("LoginActivity", "   4. 🌐 Check if email provider is blocking Firebase emails");
                        Log.d("LoginActivity", "   5. ⚙️ Firebase Console → Authentication → Templates → Password reset");
                        Log.d("LoginActivity", "   6. ⚙️ Firebase Console → Authentication → Sign-in method → Email/Password");
                        Log.d("LoginActivity", "");
                        Log.d("LoginActivity", "💡 TROUBLESHOOTING:");
                        Log.d("LoginActivity", "   - Some email providers (Gmail, Yahoo) may delay Firebase emails");
                        Log.d("LoginActivity", "   - Check Firebase Console → Authentication → Users to see if user exists");
                        Log.d("LoginActivity", "   - Try a different email address to test if it's provider-specific");
                        
                        // Show success dialog with detailed information
                        showPasswordResetSuccessDialog(email);
                            dialog.dismiss();
                        } else {
                        // Error - check if it's user-not-found
                            Exception exception = task.getException();
                            String errorMessage = getString(R.string.reset_email_error);
                            String errorCode = "unknown";
                            String exceptionMessage = null;
                            boolean isUserNotFound = false;
                            
                            Log.e("LoginActivity", "❌❌❌ PASSWORD RESET FAILED ❌❌❌");
                            Log.e("LoginActivity", "   Email: " + email);
                            Log.e("LoginActivity", "   This email was verified in RTDB but Firebase Auth failed");
                            
                            if (exception != null) {
                                exceptionMessage = exception.getMessage();
                                errorCode = getFirebaseErrorCode(exception);
                            
                                // Log the full error for debugging
                                Log.e("LoginActivity", "   Error Code: " + errorCode);
                                Log.e("LoginActivity", "   Error Message: " + exceptionMessage);
                                Log.e("LoginActivity", "   Exception Type: " + exception.getClass().getName());
                                Log.e("LoginActivity", "   Exception toString: " + exception.toString());
                                
                                if (exceptionMessage != null) {
                                    // Provide more specific error messages
                                    if (exceptionMessage.contains("user-not-found") || errorCode.equals("auth/user-not-found")) {
                                        isUserNotFound = true;
                                        Log.w("LoginActivity", "⚠️ User not found in Firebase Auth (but exists in RTDB)");
                                        Log.w("LoginActivity", "   This means the user needs to be created in Firebase Auth first");
                                        errorMessage = "No account found with this email address in Firebase Authentication.";
                                    } else if (exceptionMessage.contains("invalid-email") || errorCode.equals("auth/invalid-email")) {
                                        Log.e("LoginActivity", "❌ Invalid email format");
                                        errorMessage = "Invalid email address format.";
                                    } else if (exceptionMessage.contains("network") || errorCode.equals("auth/network-request-failed")) {
                                        Log.e("LoginActivity", "❌ Network error");
                                        errorMessage = "Network error. Please check your internet connection.";
                                    } else if (exceptionMessage.contains("too-many-requests") || errorCode.equals("auth/too-many-requests")) {
                                        Log.e("LoginActivity", "❌ Too many requests");
                                        errorMessage = "Too many requests. Please try again later.";
                                    } else if (exceptionMessage.contains("operation-not-allowed") || errorCode.equals("auth/operation-not-allowed")) {
                                        Log.e("LoginActivity", "❌❌❌ CRITICAL: Email/Password authentication NOT enabled ❌❌❌");
                                        errorMessage = "⚠️ CRITICAL: Email/Password authentication is NOT enabled in Firebase Console!\n\n" +
                                                "To fix:\n" +
                                                "1. Go to Firebase Console → Authentication → Sign-in method\n" +
                                                "2. Enable 'Email/Password' provider\n" +
                                                "3. Enable 'Password reset' option\n" +
                                                "4. Try again";
                                    } else {
                                        // Show the actual error message for debugging
                                        Log.e("LoginActivity", "❌ Unknown error: " + exceptionMessage);
                                        errorMessage = "Error: " + exceptionMessage + "\n\nError Code: " + errorCode;
                                    }
                                } else {
                                    Log.e("LoginActivity", "❌ Exception message is null");
                                }
                            } else {
                                Log.e("LoginActivity", "❌ Exception is null");
                            }
                            
                            // If user-not-found and callback provided, try creating user first
                            if (isUserNotFound && onUserNotFound != null) {
                                Log.d("LoginActivity", "User not found - will attempt to create user in Firebase Auth");
                                onUserNotFound.run();
                            } else {
                                // Show detailed error in a dialog for better visibility
                                showPasswordResetErrorDialog(errorMessage, errorCode, exceptionMessage);
                            button.setEnabled(true);
                            button.setText(R.string.send_reset_link);
                        }
                        }
                    })
                .addOnFailureListener(e -> {
                    String errorCode = getFirebaseErrorCode(e);
                    String errorMessage = e.getMessage();
                    Log.e("LoginActivity", "❌❌❌ PASSWORD RESET FAILED (onFailure) ❌❌❌");
                    Log.e("LoginActivity", "   Email: " + email);
                    Log.e("LoginActivity", "   This email was verified in RTDB but Firebase Auth failed");
                    Log.e("LoginActivity", "   Error Code: " + errorCode);
                    Log.e("LoginActivity", "   Error Message: " + errorMessage);
                    Log.e("LoginActivity", "   Exception Type: " + e.getClass().getName());
                    Log.e("LoginActivity", "   Exception toString: " + e.toString());
                    e.printStackTrace();
                    showPasswordResetErrorDialog("Error: " + errorMessage, errorCode, errorMessage);
                    button.setEnabled(true);
                    button.setText(R.string.send_reset_link);
                });
    }
    
    /**
     * Extracts Firebase error code from exception
     */
    private String getFirebaseErrorCode(Exception e) {
        if (e == null) {
            return "unknown";
        }
        if (e instanceof FirebaseAuthException) {
            return ((FirebaseAuthException) e).getErrorCode();
        }
        String message = e.getMessage();
        if (message != null && message.contains(":")) {
            String[] parts = message.split(":");
            if (parts.length > 0) {
                return parts[0].trim();
            }
        }
        return "unknown";
    }
    
    /**
     * Creates user in Firebase Auth, then sends password reset email
     * This is called when user exists in RTDB but not in Firebase Auth
     */
    private void createUserInFirebaseAuth(String email, String password, FirebaseAuth auth, AlertDialog dialog, MaterialButton button) {
        Log.d("LoginActivity", "=== CREATING USER IN FIREBASE AUTH ===");
        Log.d("LoginActivity", "Email: " + email);
        Log.d("LoginActivity", "Password length: " + (password != null ? password.length() : 0));
        Log.d("LoginActivity", "Reason: User exists in RTDB but not in Firebase Auth");
        
        if (auth == null) {
            Log.e("LoginActivity", "❌ FirebaseAuth is null - cannot create user");
            Toast.makeText(LoginActivity.this, "Firebase Authentication error. Please try again later.", Toast.LENGTH_LONG).show();
            button.setEnabled(true);
            button.setText(R.string.send_reset_link);
            return;
        }
        
        Log.d("LoginActivity", "Calling auth.createUserWithEmailAndPassword() for: " + email);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(createTask -> {
                    Log.d("LoginActivity", "=== USER CREATION TASK COMPLETED ===");
                    Log.d("LoginActivity", "Success: " + createTask.isSuccessful());
                    Log.d("LoginActivity", "Email: " + email);
                    
                    if (createTask.isSuccessful()) {
                        Log.d("LoginActivity", "✅✅✅ USER CREATED IN FIREBASE AUTH SUCCESSFULLY ✅✅✅");
                        com.google.firebase.auth.FirebaseUser newUser = auth.getCurrentUser();
                        if (newUser != null) {
                            Log.d("LoginActivity", "   New user ID: " + newUser.getUid());
                            Log.d("LoginActivity", "   New user email: " + newUser.getEmail());
                            Log.d("LoginActivity", "   Email verified: " + newUser.isEmailVerified());
                        }
                        Log.d("LoginActivity", "Now attempting to send password reset email...");
                        // User created successfully - now send password reset email
                        attemptFirebaseAuthReset(email, auth, dialog, button);
                    } else {
                        // Error creating user
                        Exception exception = createTask.getException();
                        String errorMessage = "Failed to set up password reset. Please contact administrator.";
                        String errorCode = "unknown";
                        String exceptionMessage = null;
                        
                        if (exception != null) {
                            errorCode = getFirebaseErrorCode(exception);
                            exceptionMessage = exception.getMessage();
                        }
                        
                        Log.e("LoginActivity", "❌❌❌ USER CREATION FAILED ❌❌❌");
                        
                        if (exception != null) {
                            Log.e("LoginActivity", "Error Code: " + errorCode);
                            Log.e("LoginActivity", "Error Message: " + exceptionMessage);
                            Log.e("LoginActivity", "Exception Type: " + exception.getClass().getName());
                            
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("email-already-in-use") || errorCode.equals("auth/email-already-in-use")) {
                                    // User already exists - try reset again
                                    Log.w("LoginActivity", "⚠️ User already exists in Firebase Auth (race condition?)");
                                    Log.w("LoginActivity", "   Attempting password reset directly...");
                                    attemptFirebaseAuthReset(email, auth, dialog, button);
                                    return;
                                } else if (exceptionMessage.contains("weak-password") || errorCode.equals("auth/weak-password")) {
                                    Log.e("LoginActivity", "❌ Password is too weak");
                                    errorMessage = "Password is too weak. Please contact administrator.";
                                } else if (exceptionMessage.contains("invalid-email") || errorCode.equals("auth/invalid-email")) {
                                    Log.e("LoginActivity", "❌ Invalid email format");
                                    errorMessage = "Invalid email address format.";
                                } else if (exceptionMessage.contains("operation-not-allowed") || errorCode.equals("auth/operation-not-allowed")) {
                                    Log.e("LoginActivity", "❌❌❌ CRITICAL: Email/Password authentication NOT enabled ❌❌❌");
                                    errorMessage = "⚠️ CRITICAL: Email/Password authentication is NOT enabled in Firebase Console!\n\n" +
                                            "To fix:\n" +
                                            "1. Go to Firebase Console → Authentication → Sign-in method\n" +
                                            "2. Enable 'Email/Password' provider\n" +
                                            "3. Enable 'Password reset' option\n" +
                                            "4. Try again";
                                } else if (exceptionMessage.contains("network") || errorCode.equals("auth/network-request-failed")) {
                                    Log.e("LoginActivity", "❌ Network error");
                                    errorMessage = "Network error. Please check your internet connection.";
                                } else {
                                    Log.e("LoginActivity", "❌ Unknown error: " + exceptionMessage);
                                    errorMessage = "Error: " + exceptionMessage + "\nError Code: " + errorCode;
                                }
                            } else {
                                Log.e("LoginActivity", "❌ Exception message is null");
                            }
                        } else {
                            Log.e("LoginActivity", "❌ Exception is null");
                        }
                        
                        showPasswordResetErrorDialog(errorMessage, errorCode, exceptionMessage);
            button.setEnabled(true);
            button.setText(R.string.send_reset_link);
        }
                })
                .addOnFailureListener(e -> {
                    String errorCode = getFirebaseErrorCode(e);
                    String errorMessage = e.getMessage();
                    Log.e("LoginActivity", "❌❌❌ USER CREATION FAILED (onFailure) ❌❌❌");
                    Log.e("LoginActivity", "Error Code: " + errorCode);
                    Log.e("LoginActivity", "Error Message: " + errorMessage);
                    Log.e("LoginActivity", "Exception Type: " + e.getClass().getName());
                    e.printStackTrace();
                    showPasswordResetErrorDialog("Error: " + errorMessage, errorCode, errorMessage);
                    button.setEnabled(true);
                    button.setText(R.string.send_reset_link);
                });
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Reset button state when returning to this activity
        loginButton.setEnabled(true);
        loginButton.setText("Sign In");
    }

    /**
     * Shows a success dialog for password reset email sent
     */
    private void showPasswordResetSuccessDialog(String email) {
        String message = "✅ Password reset email sent successfully!\n\n" +
                "📧 Email: " + email + "\n\n" +
                "📬 Next Steps:\n" +
                "1. Check your inbox (wait 2-5 minutes)\n" +
                "2. Check spam/junk folder\n" +
                "3. Click the reset link in the email\n" +
                "4. Set your new password\n\n" +
                "⚠️ If you don't receive the email:\n" +
                "• Check Firebase Console → Authentication → Templates\n" +
                "• Verify Email/Password is enabled\n" +
                "• Check spam folder\n" +
                "• Wait 5 minutes (email delivery can be delayed)";
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("✅ Email Sent")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
    
    /**
     * Shows a detailed error dialog for password reset failures
     */
    private void showPasswordResetErrorDialog(String errorMessage, String errorCode, String exceptionMessage) {
        String detailedMessage = errorMessage;
        
        // Add specific instructions based on error code
        if (errorCode != null) {
            if (errorCode.equals("auth/operation-not-allowed")) {
                detailedMessage = "⚠️ CRITICAL ERROR: Email/Password authentication is NOT enabled in Firebase Console!\n\n" +
                        "This is why password reset emails are not being sent.\n\n" +
                        "🔧 TO FIX:\n" +
                        "1. Go to: https://console.firebase.google.com/\n" +
                        "2. Select project: hcas-c83fa\n" +
                        "3. Click: Authentication → Sign-in method\n" +
                        "4. Find: Email/Password\n" +
                        "5. Click: Enable (first toggle)\n" +
                        "6. Click: Enable Password reset (second toggle)\n" +
                        "7. Click: Save\n" +
                        "8. Try password reset again\n\n" +
                        "Error Code: " + errorCode;
            } else if (errorCode.equals("auth/user-not-found")) {
                detailedMessage += "\n\nUser not found in Firebase Auth.\n" +
                        "The app will try to create the user automatically.\n\n" +
                        "Error Code: " + errorCode;
            } else if (errorCode.equals("auth/invalid-email")) {
                detailedMessage += "\n\nInvalid email format. Please check your email address.\n\n" +
                        "Error Code: " + errorCode;
            } else if (errorCode.equals("auth/network-request-failed")) {
                detailedMessage += "\n\nNetwork error. Please check your internet connection.\n\n" +
                        "Error Code: " + errorCode;
            } else {
                detailedMessage += "\n\nError Code: " + errorCode;
                if (exceptionMessage != null && !exceptionMessage.isEmpty()) {
                    detailedMessage += "\nDetails: " + exceptionMessage;
                }
            }
        } else {
            detailedMessage += "\n\n(No error code available)";
        }
        
        // Show in a scrollable dialog for long messages
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText(detailedMessage);
        textView.setPadding(50, 20, 50, 20);
        textView.setTextSize(14);
        scrollView.addView(textView);
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("❌ Password Reset Failed")
                .setView(scrollView)
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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
