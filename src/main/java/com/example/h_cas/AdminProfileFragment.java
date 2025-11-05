package com.example.h_cas;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.graphics.drawable.BitmapDrawable;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Employee;

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
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase Storage
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        
        // Initialize admin profile functionality
        initializeViews(view);
        initializeData();
        setupClickListeners();
        loadAdminProfile();
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
     * Load profile picture from URL
     */
    private void loadProfilePicture(String imageUrl) {
        if (getContext() != null && imageUrl != null && !imageUrl.isEmpty()) {
            try {
                // Load image in background thread
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
                                if (bitmap != null) {
                                    profileImageView.setImageBitmap(bitmap);
                                } else {
                                    profileImageView.setImageResource(R.drawable.ic_admin_avatar);
                                }
                            });
                        }
                    } catch (Exception e) {
                        // Fallback to default avatar on error
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                profileImageView.setImageResource(R.drawable.ic_admin_avatar);
                            });
                        }
                    }
                }).start();
            } catch (Exception e) {
                profileImageView.setImageResource(R.drawable.ic_admin_avatar);
            }
        } else {
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
        builder.setItems(new String[]{"Take Photo", "Choose from Gallery", "Remove Picture"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    // Take Photo
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, PICK_IMAGE_REQUEST);
                    }
                    break;
                case 1:
                    // Choose from Gallery
                    Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    pickImageIntent.setType("image/*");
                    startActivityForResult(pickImageIntent, PICK_IMAGE_REQUEST);
                    break;
                case 2:
                    // Remove Picture
                    removeProfilePicture();
                    break;
            }
        });
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK) {
            if (data != null) {
                if (data.getExtras() != null && data.getExtras().get("data") != null) {
                    // Photo taken from camera
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    imageUri = getImageUri(photo);
                } else if (data.getData() != null) {
                    // Image selected from gallery
                    imageUri = data.getData();
                }
                
                if (imageUri != null) {
                    uploadProfilePicture(imageUri);
                }
            }
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

        showToast("Uploading profile picture...");
        
        try {
            // Create reference to profile pictures folder
            String fileName = "profile_" + loggedInEmployeeId + "_" + System.currentTimeMillis() + ".jpg";
            StorageReference profileRef = storageReference.child("profile_pictures/" + fileName);
            
            // Upload file
            UploadTask uploadTask = profileRef.putFile(imageUri);
            
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Get download URL
                profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    
                    // Update database with profile picture URL
                    boolean updated = databaseHelper.updateEmployeeProfilePicture(loggedInEmployeeId, downloadUrl);
                    
                    if (updated) {
                        // Update current admin object
                        currentAdmin.setProfilePictureUrl(downloadUrl);
                        
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
     * Change password
     */
    private void changePassword(String currentPassword, String newPassword) {
        try {
            if (databaseHelper != null && loggedInUsername != null) {
                boolean isValid = databaseHelper.validateEmployeeLogin(loggedInUsername, currentPassword);
                
                if (isValid) {
                    boolean updated = databaseHelper.updateEmployeePassword(loggedInUsername, newPassword);
                    
                    if (updated) {
                        showToast("✅ Password changed successfully!");
                    } else {
                        showToast("❌ Failed to change password");
                    }
                } else {
                    showToast("❌ Current password is incorrect");
                }
            } else {
                showToast("❌ Error: Database not initialized");
            }
        } catch (Exception e) {
            showToast("Error changing password: " + e.getMessage());
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

