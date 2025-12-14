package com.healthcare.cas;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import android.graphics.drawable.BitmapDrawable;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.healthcare.cas.utils.ImageUtils;

import com.healthcare.cas.database.HCasDatabaseHelper;
import com.healthcare.cas.models.Employee;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * AdminProfileFragment handles admin profile management functionality.
 * Allows admin to view and update their personal information including profile picture.
 */
public class AdminProfileFragment extends Fragment {

    // UI Components
    private ImageView profileImageView;
    private TextView adminNameTextView;
    private TextView adminRoleTextView;
    private TextView adminIdTextView;
    
    // Personal Information Fields
    private TextInputEditText inputFirstName;
    private TextInputEditText inputLastName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPhone;
    private TextInputEditText inputUsername;
    
    // Buttons
    private MaterialButton editPersonalInfoButton;
    private MaterialButton changePasswordButton;
    private MaterialButton saveProfileButton;
    private MaterialButton changeProfilePictureButton;
    
    // Data
    private Employee currentAdmin;
    private HCasDatabaseHelper databaseHelper;
    private String loggedInUsername;
    private String loggedInEmployeeId;
    
    // Edit States
    private boolean isPersonalInfoEditable = false;
    
    // Profile Picture
    private Uri imageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize admin profile functionality
        initializeViews(view);
        initializeData();
        setupImagePicker();
        setupClickListeners();
        loadAdminProfile();
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
                        // Check if it's from camera (has extra data)
                        if (data.getExtras() != null && data.getExtras().get("data") != null) {
                            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                            if (bitmap != null) {
                                // Convert bitmap to URI
                                imageUri = getImageUri(bitmap);
                                if (imageUri != null) {
                                    uploadProfilePicture(imageUri);
                                }
                            }
                        } else {
                            // It's from gallery
                            Uri selectedImageUri = data.getData();
                            if (selectedImageUri != null) {
                                uploadProfilePicture(selectedImageUri);
                            }
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
        adminNameTextView = view.findViewById(R.id.adminNameTextView);
        adminRoleTextView = view.findViewById(R.id.adminRoleTextView);
        adminIdTextView = view.findViewById(R.id.adminIdTextView);
        
        // Personal information fields
        inputFirstName = view.findViewById(R.id.inputFirstName);
        inputLastName = view.findViewById(R.id.inputLastName);
        inputEmail = view.findViewById(R.id.inputEmail);
        inputPhone = view.findViewById(R.id.inputPhone);
        inputUsername = view.findViewById(R.id.inputUsername);
        
        // Buttons
        editPersonalInfoButton = view.findViewById(R.id.editPersonalInfoButton);
        changePasswordButton = view.findViewById(R.id.changePasswordButton);
        saveProfileButton = view.findViewById(R.id.saveProfileButton);
        changeProfilePictureButton = view.findViewById(R.id.changeProfilePictureButton);
    }

    /**
     * Initialize data from database
     */
    private void initializeData() {
        databaseHelper = new HCasDatabaseHelper(getContext());
        
        // Get admin username (default is "admin")
        loggedInUsername = "admin";
        loggedInEmployeeId = "ADMIN001";
        
        // Create current admin object
        currentAdmin = new Employee();
        currentAdmin.setEmployeeId(loggedInEmployeeId);
        currentAdmin.setUsername(loggedInUsername);
        currentAdmin.setRole("Administrator");
    }

    /**
     * Setup click listeners for all interactive elements
     */
    private void setupClickListeners() {
        // Edit Personal Info Button
        editPersonalInfoButton.setOnClickListener(v -> togglePersonalInfoEdit());
        
        // Change Password Button
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        
        // Save Profile Button
        saveProfileButton.setOnClickListener(v -> saveProfileChanges());
        
        // Profile Picture Click
        profileImageView.setOnClickListener(v -> showProfilePictureDialog());
        
        // Change Profile Picture Button
        changeProfilePictureButton.setOnClickListener(v -> showProfilePictureDialog());
    }

    /**
     * Load admin profile data from database
     */
    private void loadAdminProfile() {
        try {
            // Load admin data from database
            Employee adminData = databaseHelper.getEmployeeByUsername(loggedInUsername);
            if (adminData != null) {
                currentAdmin = adminData;
                
                // Update header information
                adminNameTextView.setText(currentAdmin.getFullName());
                adminRoleTextView.setText(currentAdmin.getRole());
                adminIdTextView.setText("Employee ID: " + currentAdmin.getEmployeeId());
                
                // Load profile picture if available
                if (currentAdmin.getProfilePictureUrl() != null && !currentAdmin.getProfilePictureUrl().isEmpty()) {
                    loadProfilePicture(currentAdmin.getProfilePictureUrl());
                }
                
                // Load personal information
                inputFirstName.setText(currentAdmin.getFirstName());
                inputLastName.setText(currentAdmin.getLastName());
                inputEmail.setText(currentAdmin.getEmail());
                inputPhone.setText(currentAdmin.getPhone() != null ? currentAdmin.getPhone() : "");
                inputUsername.setText(currentAdmin.getUsername());
                
                // Set fields as read-only initially
                setPersonalInfoEditable(false);
                
            } else {
                showToast("Error loading admin profile data");
            }
        } catch (Exception e) {
            showToast("Error loading profile: " + e.getMessage());
        }
    }

    /**
     * Load profile picture from base64 string or URL (backward compatibility)
     */
    private void loadProfilePicture(String imageData) {
        if (imageData == null || imageData.isEmpty() || profileImageView == null) {
            if (profileImageView != null) {
                profileImageView.setImageResource(R.drawable.ic_admin_avatar);
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
    
    /**
     * Load profile picture from base64 string
     */
    private void loadProfilePictureFromBase64(String base64String) {
        if (base64String == null || base64String.isEmpty() || profileImageView == null) {
            if (profileImageView != null) {
                profileImageView.setImageResource(R.drawable.ic_admin_avatar);
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
                            profileImageView.setImageResource(R.drawable.ic_admin_avatar);
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("AdminProfile", "❌ Error loading base64 image: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (profileImageView != null) {
                            profileImageView.setImageResource(R.drawable.ic_admin_avatar);
                        }
                    });
                }
            }
        }).start();
    }
    
    /**
     * Load profile picture from URL (backward compatibility)
     */
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
                                profileImageView.setImageResource(R.drawable.ic_admin_avatar);
                            }
                        });
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (profileImageView != null) {
                                profileImageView.setImageResource(R.drawable.ic_admin_avatar);
                            }
                        });
                    }
                }
            }).start();
        } catch (Exception e) {
            profileImageView.setImageResource(R.drawable.ic_admin_avatar);
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
            loadAdminProfile();
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
        // Username should not be editable
        inputUsername.setEnabled(false);
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
        if (currentAdmin.getProfilePictureUrl() != null && !currentAdmin.getProfilePictureUrl().isEmpty()) {
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
                        if (options.size() > 1 && currentAdmin.getProfilePictureUrl() != null) {
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
     * Convert Bitmap to Uri
     */
    private Uri getImageUri(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    /**
     * Upload profile picture to Firebase Storage
     */
    private void uploadProfilePicture(Uri imageUri) {
        if (imageUri == null || getContext() == null) {
            showToast("Error: Invalid image");
            return;
        }

        showToast("Converting and uploading profile picture...");
        
        // Convert image to base64 string in background thread
        new Thread(() -> {
            try {
                String base64Image = ImageUtils.convertImageUriToBase64(getContext(), imageUri);
                
                if (base64Image == null || base64Image.isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("❌ Failed to convert image. Please try again.");
                        });
                    }
                    return;
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        boolean updated = databaseHelper.updateEmployeeProfilePicture(loggedInEmployeeId, base64Image);
                        
                        if (updated) {
                            currentAdmin.setProfilePictureUrl(base64Image);
                            loadProfilePicture(base64Image);
                            syncProfilePictureToFirebase(base64Image);
                            showToast("✅ Profile picture updated successfully!");
                        } else {
                            showToast("❌ Failed to update profile picture in database");
                        }
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("AdminProfile", "❌ Error converting image: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("❌ Error: " + e.getMessage());
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
                    android.util.Log.d("AdminProfile", "✅ Profile picture synced to Firebase RTDB");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AdminProfile", "❌ Failed to sync profile picture to Firebase: " + e.getMessage());
                });
        } catch (Exception e) {
            android.util.Log.e("AdminProfile", "❌ Error syncing to Firebase: " + e.getMessage(), e);
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
                currentAdmin.setProfilePictureUrl(null);
                profileImageView.setImageResource(R.drawable.ic_admin_avatar);
                showToast("✅ Profile picture removed");
            } else {
                showToast("❌ Failed to remove profile picture");
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Save profile changes
     */
    private void saveProfileChanges() {
        if (!isPersonalInfoEditable) {
            showToast("Please click 'Edit' to make changes");
            return;
        }

        try {
            // Get updated values
            String firstName = getText(inputFirstName);
            String lastName = getText(inputLastName);
            String email = getText(inputEmail);
            String phone = getText(inputPhone);
            
            // Validation
            if (firstName.isEmpty() || lastName.isEmpty()) {
                showToast("First name and last name are required");
                return;
            }
            
            if (email.isEmpty() || !email.contains("@")) {
                showToast("Please enter a valid email address");
                return;
            }
            
            // Update admin object
            currentAdmin.setFirstName(firstName);
            currentAdmin.setLastName(lastName);
            currentAdmin.setEmail(email);
            currentAdmin.setPhone(phone);
            
            // Update database
            boolean updated = databaseHelper.updateEmployee(currentAdmin);
            
            if (updated) {
                // Update UI
                adminNameTextView.setText(currentAdmin.getFullName());
                
                // Disable edit mode
                togglePersonalInfoEdit();
                
                showToast("✅ Profile updated successfully!");
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

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        EditText currentPasswordInput = dialogView.findViewById(R.id.inputCurrentPassword);
        EditText newPasswordInput = dialogView.findViewById(R.id.inputNewPassword);
        EditText confirmPasswordInput = dialogView.findViewById(R.id.inputConfirmPassword);

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
     * Validate password change
     */
    private boolean validatePasswordChange(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty()) {
            showToast("Current password is required");
            return false;
        }
        if (newPassword.isEmpty()) {
            showToast("New password is required");
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
     * Change password using Firebase Authentication (like forgot password feature)
     */
    private void changePassword(String currentPassword, String newPassword) {
        try {
            // Get user's email from current admin data
            String email = currentAdmin.getEmail();
            if (email == null || email.isEmpty()) {
                // Try to get from input field
                email = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : null;
            }
            
            if (email == null || email.isEmpty() || !email.contains("@")) {
                showToast("❌ Email address is required for password change. Please update your email first.");
                return;
            }
            
            // Create final copies for lambda expressions
            final String finalEmail = email;
            final String finalNewPassword = newPassword;
            
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth == null) {
                showToast("❌ Firebase Authentication is not available. Please try again later.");
                return;
            }
            
            // Show loading state
            showToast("Changing password...");
            
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
                                        showToast("❌ Failed to update password: " + errorMsg);
                                        auth.signOut();
                                    }
                                });
                        } else {
                            showToast("❌ User not found");
                            auth.signOut();
                        }
                    } else {
                        // Current password is incorrect
                        Exception e = task.getException();
                        String errorMsg = e != null && e.getMessage() != null ? e.getMessage() : "Current password is incorrect";
                        if (errorMsg.contains("wrong-password") || errorMsg.contains("invalid-credential")) {
                            showToast("❌ Current password is incorrect");
                        } else {
                            showToast("❌ Error: " + errorMsg);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showToast("❌ Error: " + e.getMessage());
                });
        } catch (Exception e) {
            showToast("Error changing password: " + e.getMessage());
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
                                            showToast("✅ Password changed successfully!");
                                        })
                                        .addOnFailureListener(e -> {
                                            showToast("✅ Password changed in Firebase Auth, but failed to sync to database. Please contact administrator.");
                                        });
                                    return;
                                }
                            }
                        }
                    }
                    // Email not found in RTDB - password still updated in Firebase Auth
                    showToast("✅ Password changed in Firebase Auth, but email not found in database.");
                }
                
                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {
                    showToast("✅ Password changed in Firebase Auth, but failed to sync to database.");
                }
            });
        } catch (Exception e) {
            showToast("✅ Password changed in Firebase Auth, but failed to sync to database.");
        }
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
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}

