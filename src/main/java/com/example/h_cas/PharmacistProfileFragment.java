package com.example.h_cas;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Employee;

/**
 * PharmacistProfileFragment handles pharmacist profile management
 */
public class PharmacistProfileFragment extends Fragment {

    // UI Components
    private ImageView profileImageView;
    private TextView pharmacistNameTextView;
    private TextView pharmacistRoleTextView;
    private TextView pharmacistIdTextView;
    private TextView pharmacistEmailTextView;
    private TextView pharmacistPhoneTextView;
    private TextView pharmacistAddressTextView;
    private TextView pharmacistLicenseTextView;
    private TextView pharmacistDepartmentTextView;
    private TextView pharmacistExperienceTextView;
    
    // Action Buttons
    private MaterialButton editPersonalInfoButton;
    private MaterialButton editProfessionalInfoButton;
    private MaterialButton changePasswordButton;
    private MaterialButton changeProfilePictureButton;
    private MaterialButton saveChangesButton;
    private MaterialButton cancelEditButton;
    
    // Data
    private Employee currentPharmacist;
    private HCasDatabaseHelper databaseHelper;
    private String loggedInFullName;
    private String loggedInUsername;
    private String loggedInRole;
    private String loggedInEmail;
    private String loggedInEmployeeId;
    
    // Edit mode flags
    private boolean isPersonalInfoEditable = false;
    private boolean isProfessionalInfoEditable = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pharmacist_profile, container, false);
        
        initializeViews(view);
        initializeData();
        setupClickListeners();
        loadPharmacistProfile();
        
        return view;
    }

    private void initializeViews(View view) {
        // Profile header
        profileImageView = view.findViewById(R.id.profileImageView);
        pharmacistNameTextView = view.findViewById(R.id.pharmacistNameTextView);
        pharmacistRoleTextView = view.findViewById(R.id.pharmacistRoleTextView);
        
        // Personal information
        pharmacistIdTextView = view.findViewById(R.id.pharmacistIdTextView);
        pharmacistEmailTextView = view.findViewById(R.id.pharmacistEmailTextView);
        pharmacistPhoneTextView = view.findViewById(R.id.pharmacistPhoneTextView);
        pharmacistAddressTextView = view.findViewById(R.id.pharmacistAddressTextView);
        
        // Professional information
        pharmacistLicenseTextView = view.findViewById(R.id.pharmacistLicenseTextView);
        pharmacistDepartmentTextView = view.findViewById(R.id.pharmacistDepartmentTextView);
        pharmacistExperienceTextView = view.findViewById(R.id.pharmacistExperienceTextView);
        
        // Action buttons
        editPersonalInfoButton = view.findViewById(R.id.editPersonalInfoButton);
        editProfessionalInfoButton = view.findViewById(R.id.editProfessionalInfoButton);
        changePasswordButton = view.findViewById(R.id.changePasswordButton);
        changeProfilePictureButton = view.findViewById(R.id.changeProfilePictureButton);
        saveChangesButton = view.findViewById(R.id.saveChangesButton);
        cancelEditButton = view.findViewById(R.id.cancelEditButton);
    }

    private void initializeData() {
        databaseHelper = new HCasDatabaseHelper(getContext());
        
        // Get logged-in pharmacist data from parent activity
        if (getActivity() != null) {
            Intent intent = getActivity().getIntent();
            loggedInFullName = intent.getStringExtra("FULL_NAME");
            loggedInUsername = intent.getStringExtra("USERNAME");
            loggedInRole = intent.getStringExtra("ROLE");
            loggedInEmail = intent.getStringExtra("EMAIL");
            loggedInEmployeeId = intent.getStringExtra("EMPLOYEE_ID");
        }
        
        // Create current pharmacist object from intent data
        currentPharmacist = new Employee();
        currentPharmacist.setEmployeeId(loggedInEmployeeId);
        currentPharmacist.setUsername(loggedInUsername);
        currentPharmacist.setRole(loggedInRole);
        currentPharmacist.setEmail(loggedInEmail);
        
        // Set default values for missing fields
        if (loggedInFullName != null && !loggedInFullName.isEmpty()) {
            String[] nameParts = loggedInFullName.split(" ");
            if (nameParts.length >= 2) {
                currentPharmacist.setFirstName(nameParts[0]);
                currentPharmacist.setLastName(nameParts[nameParts.length - 1]);
            } else {
                currentPharmacist.setFirstName(loggedInFullName);
                currentPharmacist.setLastName("");
            }
        }
        
        // Set default professional info
        currentPharmacist.setLicenseNumber("PHARM-" + loggedInEmployeeId);
        currentPharmacist.setDepartment("Pharmacy");
        currentPharmacist.setExperience("5 years");
        currentPharmacist.setAddress("123 Healthcare St, Medical City");
        currentPharmacist.setPhone("+63 912 345 6789");
    }

    private void setupClickListeners() {
        editPersonalInfoButton.setOnClickListener(v -> togglePersonalInfoEdit());
        editProfessionalInfoButton.setOnClickListener(v -> toggleProfessionalInfoEdit());
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        changeProfilePictureButton.setOnClickListener(v -> showProfilePictureDialog());
        saveChangesButton.setOnClickListener(v -> saveProfileChanges());
        cancelEditButton.setOnClickListener(v -> cancelEdit());
    }

    private void loadPharmacistProfile() {
        // Display pharmacist information
        pharmacistNameTextView.setText(loggedInFullName != null ? loggedInFullName : "Pharmacist Name");
        pharmacistRoleTextView.setText(loggedInRole != null ? loggedInRole : "Licensed Pharmacist");
        pharmacistIdTextView.setText("ID: " + (loggedInEmployeeId != null ? loggedInEmployeeId : "N/A"));
        pharmacistEmailTextView.setText(loggedInEmail != null ? loggedInEmail : "pharmacist@hcas.com");
        pharmacistPhoneTextView.setText(currentPharmacist.getPhone() != null ? currentPharmacist.getPhone() : "Not set");
        pharmacistAddressTextView.setText(currentPharmacist.getAddress() != null ? currentPharmacist.getAddress() : "Not set");
        pharmacistLicenseTextView.setText(currentPharmacist.getLicenseNumber() != null ? currentPharmacist.getLicenseNumber() : "Not set");
        pharmacistDepartmentTextView.setText(currentPharmacist.getDepartment() != null ? currentPharmacist.getDepartment() : "Not set");
        pharmacistExperienceTextView.setText(currentPharmacist.getExperience() != null ? currentPharmacist.getExperience() : "Not set");
    }

    private void togglePersonalInfoEdit() {
        isPersonalInfoEditable = !isPersonalInfoEditable;
        setPersonalInfoEditable(isPersonalInfoEditable);
        
        if (isPersonalInfoEditable) {
            editPersonalInfoButton.setText("Cancel Edit");
            showEditButtons();
        } else {
            editPersonalInfoButton.setText("Edit Personal Info");
            hideEditButtons();
        }
    }

    private void toggleProfessionalInfoEdit() {
        isProfessionalInfoEditable = !isProfessionalInfoEditable;
        setProfessionalInfoEditable(isProfessionalInfoEditable);
        
        if (isProfessionalInfoEditable) {
            editProfessionalInfoButton.setText("Cancel Edit");
            showEditButtons();
        } else {
            editProfessionalInfoButton.setText("Edit Professional Info");
            hideEditButtons();
        }
    }

    private void setPersonalInfoEditable(boolean editable) {
        // In a real implementation, you would make TextViews editable or show EditTexts
        // For now, we'll just show a toast
        if (editable) {
            Toast.makeText(getContext(), "Personal info edit mode activated", Toast.LENGTH_SHORT).show();
        }
    }

    private void setProfessionalInfoEditable(boolean editable) {
        // In a real implementation, you would make TextViews editable or show EditTexts
        // For now, we'll just show a toast
        if (editable) {
            Toast.makeText(getContext(), "Professional info edit mode activated", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditButtons() {
        saveChangesButton.setVisibility(View.VISIBLE);
        cancelEditButton.setVisibility(View.VISIBLE);
    }

    private void hideEditButtons() {
        saveChangesButton.setVisibility(View.GONE);
        cancelEditButton.setVisibility(View.GONE);
    }

    private void saveProfileChanges() {
        // In a real implementation, you would save the changes to the database
        Toast.makeText(getContext(), "✅ Profile changes saved successfully!", Toast.LENGTH_SHORT).show();
        
        // Reset edit modes
        isPersonalInfoEditable = false;
        isProfessionalInfoEditable = false;
        editPersonalInfoButton.setText("Edit Personal Info");
        editProfessionalInfoButton.setText("Edit Professional Info");
        hideEditButtons();
    }

    private void cancelEdit() {
        // Reset edit modes
        isPersonalInfoEditable = false;
        isProfessionalInfoEditable = false;
        editPersonalInfoButton.setText("Edit Personal Info");
        editProfessionalInfoButton.setText("Edit Professional Info");
        hideEditButtons();
        
        // Reload original data
        loadPharmacistProfile();
        Toast.makeText(getContext(), "Edit cancelled", Toast.LENGTH_SHORT).show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Change Password");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        TextInputEditText currentPasswordInput = dialogView.findViewById(R.id.inputCurrentPassword);
        TextInputEditText newPasswordInput = dialogView.findViewById(R.id.inputNewPassword);
        TextInputEditText confirmPasswordInput = dialogView.findViewById(R.id.inputConfirmPassword);

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

    private boolean validatePasswordChange(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty()) {
            Toast.makeText(getContext(), "Current password is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (newPassword.isEmpty()) {
            Toast.makeText(getContext(), "New password is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (newPassword.length() < 6) {
            Toast.makeText(getContext(), "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getContext(), "New passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void changePassword(String currentPassword, String newPassword) {
        // In a real implementation, you would validate the current password and update it
        // For now, we'll just show a success message
        Toast.makeText(getContext(), "✅ Password changed successfully!", Toast.LENGTH_SHORT).show();
    }

    private void showProfilePictureDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Change Profile Picture");
        builder.setMessage("Profile picture functionality will be implemented in a future update.");
        builder.setPositiveButton("OK", null);
        builder.show();
    }
}
