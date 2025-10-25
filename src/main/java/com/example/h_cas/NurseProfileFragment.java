package com.example.h_cas;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Employee;

/**
 * NurseProfileFragment handles nurse profile management functionality.
 * Allows nurses to view and update their personal and professional information.
 */
public class NurseProfileFragment extends Fragment {

    // UI Components
    private ImageView profileImageView;
    private TextView nurseNameTextView;
    private TextView nurseRoleTextView;
    private TextView nurseIdTextView;
    
    // Personal Information Fields
    private TextInputEditText inputFirstName;
    private TextInputEditText inputLastName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPhone;
    private TextInputEditText inputAddress;
    
    // Professional Information Fields
    private TextInputEditText inputLicenseNumber;
    private TextInputEditText inputSpecialization;
    private TextInputEditText inputExperience;
    private TextInputEditText inputDepartment;
    
    // Buttons
    private MaterialButton editPersonalInfoButton;
    private MaterialButton editProfessionalInfoButton;
    private MaterialButton changePasswordButton;
    private MaterialButton saveProfileButton;
    
    // Data
    private Employee currentNurse;
    private HCasDatabaseHelper databaseHelper;
    private String loggedInFullName;
    private String loggedInUsername;
    private String loggedInRole;
    private String loggedInEmployeeId;
    
    // Edit States
    private boolean isPersonalInfoEditable = false;
    private boolean isProfessionalInfoEditable = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nurse_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize nurse profile functionality
        initializeViews(view);
        initializeData();
        setupClickListeners();
        loadNurseProfile();
    }

    /**
     * Initialize all view references
     */
    private void initializeViews(View view) {
        // Header views
        profileImageView = view.findViewById(R.id.profileImageView);
        nurseNameTextView = view.findViewById(R.id.nurseNameTextView);
        nurseRoleTextView = view.findViewById(R.id.nurseRoleTextView);
        nurseIdTextView = view.findViewById(R.id.nurseIdTextView);
        
        // Personal information fields
        inputFirstName = view.findViewById(R.id.inputFirstName);
        inputLastName = view.findViewById(R.id.inputLastName);
        inputEmail = view.findViewById(R.id.inputEmail);
        inputPhone = view.findViewById(R.id.inputPhone);
        inputAddress = view.findViewById(R.id.inputAddress);
        
        // Professional information fields
        inputLicenseNumber = view.findViewById(R.id.inputLicenseNumber);
        inputSpecialization = view.findViewById(R.id.inputSpecialization);
        inputExperience = view.findViewById(R.id.inputExperience);
        inputDepartment = view.findViewById(R.id.inputDepartment);
        
        // Buttons
        editPersonalInfoButton = view.findViewById(R.id.editPersonalInfoButton);
        editProfessionalInfoButton = view.findViewById(R.id.editProfessionalInfoButton);
        changePasswordButton = view.findViewById(R.id.changePasswordButton);
        saveProfileButton = view.findViewById(R.id.saveProfileButton);
    }

    /**
     * Initialize data from parent activity
     */
    private void initializeData() {
        databaseHelper = new HCasDatabaseHelper(getContext());
        
        // Get employee data from arguments or parent activity
        Bundle args = getArguments();
        if (args != null) {
            loggedInFullName = args.getString("FULL_NAME");
            loggedInUsername = args.getString("USERNAME");
            loggedInRole = args.getString("ROLE");
            loggedInEmployeeId = args.getString("EMPLOYEE_ID");
        }
        
        // Create current nurse object
        currentNurse = new Employee();
        if (loggedInEmployeeId != null) {
            currentNurse.setEmployeeId(loggedInEmployeeId);
        }
        if (loggedInUsername != null) {
            currentNurse.setUsername(loggedInUsername);
        }
        if (loggedInRole != null) {
            currentNurse.setRole(loggedInRole);
        }
    }

    /**
     * Setup click listeners for all interactive elements
     */
    private void setupClickListeners() {
        // Edit Personal Info Button
        editPersonalInfoButton.setOnClickListener(v -> togglePersonalInfoEdit());
        
        // Edit Professional Info Button
        editProfessionalInfoButton.setOnClickListener(v -> toggleProfessionalInfoEdit());
        
        // Change Password Button
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        
        // Save Profile Button
        saveProfileButton.setOnClickListener(v -> saveProfileChanges());
        
        // Profile Picture Click
        profileImageView.setOnClickListener(v -> showProfilePictureDialog());
    }

    /**
     * Load nurse profile data from database
     */
    private void loadNurseProfile() {
        try {
            // Load nurse data from database
            Employee nurseData = databaseHelper.getEmployeeByUsername(loggedInUsername);
            if (nurseData != null) {
                currentNurse = nurseData;
                
                // Update header information - use the exact name from admin registration
                if (loggedInFullName != null && !loggedInFullName.isEmpty()) {
                    nurseNameTextView.setText(loggedInFullName);
                } else {
                    nurseNameTextView.setText(currentNurse.getFullName());
                }
                nurseRoleTextView.setText(currentNurse.getRole());
                nurseIdTextView.setText("Employee ID: " + currentNurse.getEmployeeId());
                
                // Load personal information
                inputFirstName.setText(currentNurse.getFirstName());
                inputLastName.setText(currentNurse.getLastName());
                inputEmail.setText(currentNurse.getEmail());
                inputPhone.setText(currentNurse.getPhone());
                inputAddress.setText(currentNurse.getAddress());
                
                // Load professional information (these might be empty initially)
                inputLicenseNumber.setText(currentNurse.getLicenseNumber() != null ? currentNurse.getLicenseNumber() : "");
                inputSpecialization.setText(currentNurse.getSpecialization() != null ? currentNurse.getSpecialization() : "");
                inputExperience.setText(currentNurse.getExperience() != null ? currentNurse.getExperience() : "");
                inputDepartment.setText(currentNurse.getDepartment() != null ? currentNurse.getDepartment() : "");
                
                // Set fields as read-only initially
                setPersonalInfoEditable(false);
                setProfessionalInfoEditable(false);
                
            } else {
                showToast("Error loading nurse profile data");
            }
        } catch (Exception e) {
            showToast("Error loading profile: " + e.getMessage());
        }
    }

    /**
     * Toggle personal information edit mode
     */
    private void togglePersonalInfoEdit() {
        isPersonalInfoEditable = !isPersonalInfoEditable;
        setPersonalInfoEditable(isPersonalInfoEditable);
        
        if (isPersonalInfoEditable) {
            editPersonalInfoButton.setText("Cancel");
            editPersonalInfoButton.setBackgroundColor(getResources().getColor(R.color.error_red));
        } else {
            editPersonalInfoButton.setText("Edit");
            editPersonalInfoButton.setBackgroundColor(getResources().getColor(R.color.primary_blue));
            // Reload original data
            loadNurseProfile();
        }
    }

    /**
     * Toggle professional information edit mode
     */
    private void toggleProfessionalInfoEdit() {
        isProfessionalInfoEditable = !isProfessionalInfoEditable;
        setProfessionalInfoEditable(isProfessionalInfoEditable);
        
        if (isProfessionalInfoEditable) {
            editProfessionalInfoButton.setText("Cancel");
            editProfessionalInfoButton.setBackgroundColor(getResources().getColor(R.color.error_red));
        } else {
            editProfessionalInfoButton.setText("Edit");
            editProfessionalInfoButton.setBackgroundColor(getResources().getColor(R.color.primary_blue));
            // Reload original data
            loadNurseProfile();
        }
    }

    /**
     * Set personal information fields editable state
     */
    private void setPersonalInfoEditable(boolean editable) {
        inputFirstName.setEnabled(editable);
        inputLastName.setEnabled(editable);
        inputEmail.setEnabled(editable);
        inputPhone.setEnabled(editable);
        inputAddress.setEnabled(editable);
    }

    /**
     * Set professional information fields editable state
     */
    private void setProfessionalInfoEditable(boolean editable) {
        inputLicenseNumber.setEnabled(editable);
        inputSpecialization.setEnabled(editable);
        inputExperience.setEnabled(editable);
        inputDepartment.setEnabled(editable);
    }

    /**
     * Save profile changes to database
     */
    private void saveProfileChanges() {
        try {
            // Update personal information
            if (isPersonalInfoEditable) {
                currentNurse.setFirstName(getText(inputFirstName));
                currentNurse.setLastName(getText(inputLastName));
                currentNurse.setEmail(getText(inputEmail));
                currentNurse.setPhone(getText(inputPhone));
                currentNurse.setAddress(getText(inputAddress));
            }
            
            // Update professional information
            if (isProfessionalInfoEditable) {
                currentNurse.setLicenseNumber(getText(inputLicenseNumber));
                currentNurse.setSpecialization(getText(inputSpecialization));
                currentNurse.setExperience(getText(inputExperience));
                currentNurse.setDepartment(getText(inputDepartment));
            }
            
            // Save to database
            boolean updated = databaseHelper.updateEmployee(currentNurse);
            
            if (updated) {
                showToast("✅ Profile updated successfully!");
                
                // Update header with the exact name from admin registration
                if (loggedInFullName != null && !loggedInFullName.isEmpty()) {
                    nurseNameTextView.setText(loggedInFullName);
                } else {
                    nurseNameTextView.setText(currentNurse.getFullName());
                }
                
                // Reset edit states
                isPersonalInfoEditable = false;
                isProfessionalInfoEditable = false;
                setPersonalInfoEditable(false);
                setProfessionalInfoEditable(false);
                
                // Reset button states
                editPersonalInfoButton.setText("Edit");
                editPersonalInfoButton.setBackgroundColor(getResources().getColor(R.color.primary_blue));
                editProfessionalInfoButton.setText("Edit");
                editProfessionalInfoButton.setBackgroundColor(getResources().getColor(R.color.primary_blue));
                
            } else {
                showToast("❌ Failed to update profile");
            }
            
        } catch (Exception e) {
            showToast("Error saving profile: " + e.getMessage());
        }
    }

    /**
     * Show change password dialog
     */
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Change Password");
        
        // Create input fields
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        EditText currentPasswordInput = dialogView.findViewById(R.id.inputCurrentPassword);
        EditText newPasswordInput = dialogView.findViewById(R.id.inputNewPassword);
        EditText confirmPasswordInput = dialogView.findViewById(R.id.inputConfirmPassword);
        
        builder.setView(dialogView);
        
        builder.setPositiveButton("Change Password", (dialog, which) -> {
            String currentPassword = currentPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();
            
            if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                changePassword(currentPassword, newPassword);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Validate password change inputs
     */
    private boolean validatePasswordChange(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty()) {
            showToast("Please enter current password");
            return false;
        }
        
        if (newPassword.isEmpty()) {
            showToast("Please enter new password");
            return false;
        }
        
        if (newPassword.length() < 6) {
            showToast("New password must be at least 6 characters");
            return false;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showToast("New passwords do not match");
            return false;
        }
        
        return true;
    }

    /**
     * Change password in database
     */
    private void changePassword(String currentPassword, String newPassword) {
        try {
            // Verify current password
            boolean isValid = databaseHelper.validateEmployeeLogin(loggedInUsername, currentPassword);
            
            if (isValid) {
                // Update password
                boolean updated = databaseHelper.updateEmployeePassword(loggedInUsername, newPassword);
                
                if (updated) {
                    showToast("✅ Password changed successfully!");
                } else {
                    showToast("❌ Failed to change password");
                }
            } else {
                showToast("❌ Current password is incorrect");
            }
        } catch (Exception e) {
            showToast("Error changing password: " + e.getMessage());
        }
    }

    /**
     * Show profile picture dialog
     */
    private void showProfilePictureDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Profile Picture");
        builder.setMessage("Profile picture functionality will be implemented in future updates.");
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    /**
     * Get text from TextInputEditText safely
     */
    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    /**
     * Show toast message
     */
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}



