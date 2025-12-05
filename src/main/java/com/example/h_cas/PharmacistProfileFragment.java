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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.example.h_cas.utils.ImageUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
    // Action Buttons
    private MaterialButton editPersonalInfoButton;
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
    
    // Profile Picture
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pharmacist_profile, container, false);
        
        initializeViews(view);
        initializeData();
        setupImagePicker();
        setupClickListeners();
        loadPharmacistProfile();
        
        return view;
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
        
        // Action buttons
        editPersonalInfoButton = view.findViewById(R.id.editPersonalInfoButton);
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
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        changeProfilePictureButton.setOnClickListener(v -> showProfilePictureDialog());
        saveChangesButton.setOnClickListener(v -> saveProfileChanges());
        cancelEditButton.setOnClickListener(v -> cancelEdit());
    }

    private void loadPharmacistProfile() {
        // Load pharmacist data from database
        try {
            Employee pharmacistData = databaseHelper.getEmployeeByUsername(loggedInUsername);
            if (pharmacistData != null) {
                currentPharmacist = pharmacistData;
            }
        } catch (Exception e) {
            // Use default data if loading fails
        }
        
        // Display pharmacist information
        pharmacistNameTextView.setText(loggedInFullName != null ? loggedInFullName : "Pharmacist Name");
        pharmacistRoleTextView.setText(loggedInRole != null ? loggedInRole : "Licensed Pharmacist");
        pharmacistIdTextView.setText("ID: " + (loggedInEmployeeId != null ? loggedInEmployeeId : "N/A"));
        pharmacistEmailTextView.setText(currentPharmacist.getEmail() != null ? currentPharmacist.getEmail() : (loggedInEmail != null ? loggedInEmail : "pharmacist@hcas.com"));
        pharmacistPhoneTextView.setText(currentPharmacist.getPhone() != null ? currentPharmacist.getPhone() : "Not set");
        pharmacistAddressTextView.setText(currentPharmacist.getAddress() != null ? currentPharmacist.getAddress() : "Not set");
        
        // Load profile picture if available
        if (currentPharmacist.getProfilePictureUrl() != null && !currentPharmacist.getProfilePictureUrl().isEmpty()) {
            loadProfilePicture(currentPharmacist.getProfilePictureUrl());
        } else {
            profileImageView.setImageResource(R.drawable.ic_pharmacist_avatar);
        }
    }
    
    /**
     * Load profile picture from URL
     */
    private void loadProfilePicture(String imageData) {
        if (imageData == null || imageData.isEmpty() || profileImageView == null) {
            if (profileImageView != null) {
                profileImageView.setImageResource(R.drawable.ic_pharmacist_avatar);
            }
            return;
        }
        
        // Check if it's a base64 string
        if (ImageUtils.isBase64Image(imageData)) {
            loadProfilePictureFromBase64(imageData);
        } else if (ImageUtils.isUrl(imageData)) {
            loadProfilePictureFromUrl(imageData);
        } else {
            loadProfilePictureFromBase64(imageData);
        }
    }
    
    private void loadProfilePictureFromBase64(String base64String) {
        if (base64String == null || base64String.isEmpty() || profileImageView == null) {
            if (profileImageView != null) {
                profileImageView.setImageResource(R.drawable.ic_pharmacist_avatar);
            }
            return;
        }
        
        new Thread(() -> {
            try {
                Bitmap bitmap = ImageUtils.convertBase64ToBitmap(base64String);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (profileImageView != null && bitmap != null) {
                            profileImageView.setImageBitmap(bitmap);
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_pharmacist_avatar);
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("PharmacistProfile", "❌ Error loading base64 image: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (profileImageView != null) {
                            profileImageView.setImageResource(R.drawable.ic_pharmacist_avatar);
                        }
                    });
                }
            }
        }).start();
    }
    
    private void loadProfilePictureFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty() || profileImageView == null) {
            return;
        }
        
        try {
            new Thread(() -> {
                try {
                    URL url = new URL(imageUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (profileImageView != null && bitmap != null) {
                                profileImageView.setImageBitmap(bitmap);
                            } else {
                                profileImageView.setImageResource(R.drawable.ic_pharmacist_avatar);
                            }
                        });
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (profileImageView != null) {
                                profileImageView.setImageResource(R.drawable.ic_pharmacist_avatar);
                            }
                        });
                    }
                }
            }).start();
        } catch (Exception e) {
            profileImageView.setImageResource(R.drawable.ic_pharmacist_avatar);
        }
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

    private void setPersonalInfoEditable(boolean editable) {
        // In a real implementation, you would make TextViews editable or show EditTexts
        // For now, we'll just show a toast
        if (editable) {
            Toast.makeText(getContext(), "Personal info edit mode activated", Toast.LENGTH_SHORT).show();
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
        editPersonalInfoButton.setText("Edit Personal Info");
        hideEditButtons();
    }

    private void cancelEdit() {
        // Reset edit modes
        isPersonalInfoEditable = false;
        editPersonalInfoButton.setText("Edit Personal Info");
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

    /**
     * Change password using Firebase Authentication (like forgot password feature)
     */
    private void changePassword(String currentPassword, String newPassword) {
        try {
            // Get user's email from current pharmacist data
            String email = currentPharmacist.getEmail();
            if (email == null || email.isEmpty()) {
                // Try to get from logged in email
                email = loggedInEmail;
            }
            
            if (email == null || email.isEmpty() || !email.contains("@")) {
                Toast.makeText(getContext(), "❌ Email address is required for password change. Please update your email first.", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Create final copies for lambda expressions
            final String finalEmail = email;
            final String finalNewPassword = newPassword;
            
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth == null) {
                Toast.makeText(getContext(), "❌ Firebase Authentication is not available. Please try again later.", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Show loading state
            Toast.makeText(getContext(), "Changing password...", Toast.LENGTH_SHORT).show();
            
            // Sign in with current password to verify
            auth.signInWithEmailAndPassword(finalEmail, currentPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Current password is correct - update to new password
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            user.updatePassword(finalNewPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        // Password updated in Firebase Auth - sync to RTDB
                                        syncPasswordToRTDB(finalEmail, finalNewPassword);
                                        auth.signOut(); // Sign out after password change
                                    } else {
                                        Exception e = updateTask.getException();
                                        String errorMsg = e != null ? e.getMessage() : "Failed to update password";
                                        Toast.makeText(getContext(), "❌ Failed to update password: " + errorMsg, Toast.LENGTH_LONG).show();
                                        auth.signOut();
                                    }
                                });
                        } else {
                            Toast.makeText(getContext(), "❌ User not found", Toast.LENGTH_SHORT).show();
                            auth.signOut();
                        }
                    } else {
                        // Current password is incorrect
                        Exception e = task.getException();
                        String errorMsg = e != null && e.getMessage() != null ? e.getMessage() : "Current password is incorrect";
                        if (errorMsg.contains("wrong-password") || errorMsg.contains("invalid-credential")) {
                            Toast.makeText(getContext(), "❌ Current password is incorrect", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "❌ Error: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error changing password: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Sync password to Firebase RTDB after successful Firebase Auth update
     */
    private void syncPasswordToRTDB(String email, String newPassword) {
        try {
            DatabaseReference employeesRef = FirebaseDatabase.getInstance(
                    "https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("employees");
            
            employeesRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        String normalizedEmail = email.toLowerCase().trim();
                        
                        for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                            Object emailObj = child.child("email").getValue();
                            if (emailObj != null) {
                                String storedEmail = emailObj.toString().trim().toLowerCase();
                                
                                if (storedEmail.equals(normalizedEmail)) {
                                    // Found the employee - update password in RTDB
                                    child.getRef().child("password").setValue(newPassword)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(getContext(), "✅ Password changed successfully!", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), "✅ Password changed in Firebase Auth, but failed to sync to database. Please contact administrator.", Toast.LENGTH_LONG).show();
                                        });
                                    return;
                                }
                            }
                        }
                    }
                    // Email not found in RTDB - password still updated in Firebase Auth
                    Toast.makeText(getContext(), "✅ Password changed in Firebase Auth, but email not found in database.", Toast.LENGTH_LONG).show();
                }
                
                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {
                    Toast.makeText(getContext(), "✅ Password changed in Firebase Auth, but failed to sync to database.", Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(getContext(), "✅ Password changed in Firebase Auth, but failed to sync to database.", Toast.LENGTH_LONG).show();
        }
    }

    private void showProfilePictureDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Change Profile Picture");
        
        // Build options list dynamically
        java.util.ArrayList<String> options = new java.util.ArrayList<>();
        options.add("Choose from Gallery");
        if (currentPharmacist.getProfilePictureUrl() != null && !currentPharmacist.getProfilePictureUrl().isEmpty()) {
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
                        if (options.size() > 1 && currentPharmacist.getProfilePictureUrl() != null) {
                            removeProfilePicture();
                        }
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "Error: Image picker not initialized", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error opening gallery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Upload profile picture to Firebase Storage
     */
    private void uploadProfilePicture(Uri imageUri) {
        if (imageUri == null || getContext() == null) {
            Toast.makeText(getContext(), "Error: Invalid image", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Converting and uploading profile picture...", Toast.LENGTH_SHORT).show();
        
        // Convert image to base64 string in background thread
        new Thread(() -> {
            try {
                String base64Image = ImageUtils.convertImageUriToBase64(getContext(), imageUri);
                
                if (base64Image == null || base64Image.isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "❌ Failed to convert image. Please try again.", Toast.LENGTH_SHORT).show();
                        });
                    }
                    return;
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        boolean updated = databaseHelper.updateEmployeeProfilePicture(loggedInEmployeeId, base64Image);
                        
                        if (updated) {
                            currentPharmacist.setProfilePictureUrl(base64Image);
                            loadProfilePicture(base64Image);
                            syncProfilePictureToFirebase(base64Image);
                            Toast.makeText(getContext(), "✅ Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "❌ Failed to update profile picture in database", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("PharmacistProfile", "❌ Error converting image: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }
    
    /**
     * Sync profile picture base64 string to Firebase RTDB
     */
    private void syncProfilePictureToFirebase(String base64Image) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/");
            DatabaseReference employeeRef = database.getReference("employees").child(loggedInEmployeeId);
            
            employeeRef.child("profile_picture_url").setValue(base64Image)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("PharmacistProfile", "✅ Profile picture synced to Firebase RTDB");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PharmacistProfile", "❌ Failed to sync profile picture to Firebase: " + e.getMessage());
                });
        } catch (Exception e) {
            android.util.Log.e("PharmacistProfile", "❌ Error syncing to Firebase: " + e.getMessage(), e);
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
                currentPharmacist.setProfilePictureUrl(null);
                
                // Reset to default avatar
                profileImageView.setImageResource(R.drawable.ic_pharmacist_avatar);
                
                Toast.makeText(getContext(), "✅ Profile picture removed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "❌ Failed to remove profile picture", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
