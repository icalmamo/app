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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
    
    // Profile Picture
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nurse_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase Storage
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        
        // Initialize nurse profile functionality
        initializeViews(view);
        initializeData();
        setupImagePicker();
        setupClickListeners();
        loadNurseProfile();
    }
    
    /**
     * Setup image picker launcher for gallery selection
     */
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri selectedImageUri = data.getData();
                        if (selectedImageUri != null) {
                            uploadProfilePicture(selectedImageUri);
                        }
                    }
                }
            }
        );
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
                
                // Load profile picture if available
                if (currentNurse.getProfilePictureUrl() != null && !currentNurse.getProfilePictureUrl().isEmpty()) {
                    loadProfilePicture(currentNurse.getProfilePictureUrl());
                } else {
                    profileImageView.setImageResource(R.drawable.ic_nurse_avatar);
                }
                
            } else {
                showToast("Error loading nurse profile data");
            }
        } catch (Exception e) {
            showToast("Error loading profile: " + e.getMessage());
        }
    }
    
    /**
     * Load profile picture from URL
     */
    private void loadProfilePicture(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty() || profileImageView == null) {
            return;
        }
        
        try {
            // Use a background thread to load the image
            new Thread(() -> {
                try {
                    URL url = new URL(imageUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    
                    // Update UI on main thread
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (profileImageView != null && bitmap != null) {
                                profileImageView.setImageBitmap(bitmap);
                            } else {
                                profileImageView.setImageResource(R.drawable.ic_nurse_avatar);
                            }
                        });
                    }
                } catch (Exception e) {
                    // Fallback to default avatar on error
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (profileImageView != null) {
                                profileImageView.setImageResource(R.drawable.ic_nurse_avatar);
                            }
                        });
                    }
                }
            }).start();
        } catch (Exception e) {
            profileImageView.setImageResource(R.drawable.ic_nurse_avatar);
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
        builder.setTitle("Change Profile Picture");
        
        // Build options list dynamically
        java.util.ArrayList<String> options = new java.util.ArrayList<>();
        options.add("Choose from Gallery");
        if (currentNurse.getProfilePictureUrl() != null && !currentNurse.getProfilePictureUrl().isEmpty()) {
            options.add("Remove Picture");
        }
        
        String[] optionsArray = options.toArray(new String[0]);
        
        builder.setItems(optionsArray, (dialog, which) -> {
            try {
                switch (which) {
                    case 0: // Choose from Gallery
                        pickImageFromGallery();
                        break;
                    case 1: // Remove Picture (if available)
                        if (options.size() > 1 && currentNurse.getProfilePictureUrl() != null) {
                            removeProfilePicture();
                        }
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Error: " + e.getMessage());
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Pick image from gallery
     */
    private void pickImageFromGallery() {
        try {
            if (imagePickerLauncher == null) {
                showToast("Error: Image picker not initialized");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error opening gallery: " + e.getMessage());
        }
    }
    
    /**
     * Upload profile picture to Firebase Storage
     */
    private void uploadProfilePicture(Uri imageUri) {
        if (imageUri == null || storageReference == null) {
            showToast("Error: Invalid image");
            return;
        }

        showToast("Uploading profile picture...");
        
        try {
            // Create reference to profile pictures folder
            String fileName = "profile_" + loggedInEmployeeId + "_" + System.currentTimeMillis() + ".jpg";
            StorageReference profileRef = storageReference.child("profile_pictures/" + fileName);
            
            // Upload file directly from URI
            UploadTask uploadTask = profileRef.putFile(imageUri);
            
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Get download URL
                profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    
                    // Update database with profile picture URL
                    boolean updated = databaseHelper.updateEmployeeProfilePicture(loggedInEmployeeId, downloadUrl);
                    
                    if (updated) {
                        // Update current nurse object
                        currentNurse.setProfilePictureUrl(downloadUrl);
                        
                        // Load the new image
                        loadProfilePicture(downloadUrl);
                        
                        showToast("✅ Profile picture updated successfully!");
                    } else {
                        showToast("❌ Failed to update profile picture in database");
                    }
                }).addOnFailureListener(e -> {
                    showToast("❌ Error getting download URL: " + e.getMessage());
                });
            }).addOnFailureListener(e -> {
                showToast("❌ Error uploading image: " + e.getMessage());
            });
            
        } catch (Exception e) {
            showToast("❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Remove profile picture
     */
    private void removeProfilePicture() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Remove Profile Picture");
        builder.setMessage("Are you sure you want to remove your profile picture?");
        builder.setPositiveButton("Remove", (dialog, which) -> {
            // Update database
            boolean updated = databaseHelper.updateEmployeeProfilePicture(loggedInEmployeeId, null);
            
            if (updated) {
                currentNurse.setProfilePictureUrl(null);
                
                // Reset to default avatar
                profileImageView.setImageResource(R.drawable.ic_nurse_avatar);
                
                showToast("✅ Profile picture removed");
            } else {
                showToast("❌ Failed to remove profile picture");
            }
        });
        builder.setNegativeButton("Cancel", null);
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



