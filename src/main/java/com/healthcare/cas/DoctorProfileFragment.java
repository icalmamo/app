package com.healthcare.cas;

import android.app.AlertDialog;
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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.healthcare.cas.utils.ImageUtils;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.healthcare.cas.database.HCasDatabaseHelper;
import com.healthcare.cas.models.Employee;

/**
 * DoctorProfileFragment handles doctor profile management functionality.
 * Allows doctors to view and update their personal and professional information.
 */
public class DoctorProfileFragment extends Fragment {

    // UI Components
    private ImageView profileImageView;
    private TextView doctorNameTextView;
    private TextView doctorRoleTextView;
    private TextView doctorIdTextView;
    
    // Personal Information Fields
    private TextInputEditText inputFirstName;
    private TextInputEditText inputLastName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPhone;
    private TextInputEditText inputAddress;
    
    // Buttons
    private MaterialButton editPersonalInfoButton;
    private MaterialButton changePasswordButton;
    private MaterialButton saveProfileButton;
    
    // Data
    private Employee currentDoctor;
    private HCasDatabaseHelper databaseHelper;
    private String loggedInFullName;
    private String loggedInUsername;
    private String loggedInRole;
    private String loggedInEmployeeId;
    
    // Edit States
    private boolean isPersonalInfoEditable = false;
    
    // Profile Picture
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize doctor profile functionality
        initializeViews(view);
        initializeData();
        setupImagePicker();
        setupClickListeners();
        loadDoctorProfile();
    }
    
    /**
     * Setup image picker launcher for gallery selection
     */
    private void setupImagePicker() {
        android.util.Log.d("DoctorProfile", "🔧 Setting up image picker launcher...");
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                android.util.Log.d("DoctorProfile", "📸 Image picker result received. Result code: " + result.getResultCode());
                
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri selectedImageUri = data.getData();
                        android.util.Log.d("DoctorProfile", "📸 Selected image URI: " + selectedImageUri);
                        
                        if (selectedImageUri != null) {
                            android.util.Log.d("DoctorProfile", "✅ Starting image upload...");
                            uploadProfilePicture(selectedImageUri);
                        } else {
                            android.util.Log.e("DoctorProfile", "❌ Selected image URI is null");
                            showToast("Error: No image selected");
                        }
                    } else {
                        android.util.Log.e("DoctorProfile", "❌ Result data is null");
                        showToast("Error: No image data received");
                    }
                } else {
                    android.util.Log.d("DoctorProfile", "⚠️ Image picker cancelled or failed. Result code: " + result.getResultCode());
                    if (result.getResultCode() != android.app.Activity.RESULT_CANCELED) {
                        showToast("Error: Failed to select image");
                    }
                }
            }
        );
        android.util.Log.d("DoctorProfile", "✅ Image picker launcher set up successfully");
    }

    /**
     * Initialize all view references
     */
    private void initializeViews(View view) {
        // Header views
        profileImageView = view.findViewById(R.id.profileImageView);
        doctorNameTextView = view.findViewById(R.id.doctorNameTextView);
        doctorRoleTextView = view.findViewById(R.id.doctorRoleTextView);
        doctorIdTextView = view.findViewById(R.id.doctorIdTextView);
        
        // Personal information fields
        inputFirstName = view.findViewById(R.id.inputFirstName);
        inputLastName = view.findViewById(R.id.inputLastName);
        inputEmail = view.findViewById(R.id.inputEmail);
        inputPhone = view.findViewById(R.id.inputPhone);
        inputAddress = view.findViewById(R.id.inputAddress);
        
        // Buttons
        editPersonalInfoButton = view.findViewById(R.id.editPersonalInfoButton);
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
        
        // Create current doctor object
        currentDoctor = new Employee();
        if (loggedInEmployeeId != null) {
            currentDoctor.setEmployeeId(loggedInEmployeeId);
        }
        if (loggedInUsername != null) {
            currentDoctor.setUsername(loggedInUsername);
        }
        if (loggedInRole != null) {
            currentDoctor.setRole(loggedInRole);
        }
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
    }

    /**
     * Load doctor profile data from database
     */
    private void loadDoctorProfile() {
        try {
            // Load doctor data from database
            Employee doctorData = databaseHelper.getEmployeeByUsername(loggedInUsername);
            if (doctorData != null) {
                currentDoctor = doctorData;
                
                // Update header information - use the exact name from admin registration
                if (loggedInFullName != null && !loggedInFullName.isEmpty()) {
                    doctorNameTextView.setText(loggedInFullName);
                } else {
                    doctorNameTextView.setText(currentDoctor.getFullName());
                }
                doctorRoleTextView.setText(currentDoctor.getRole());
                doctorIdTextView.setText("Employee ID: " + currentDoctor.getEmployeeId());
                
                // Load personal information
                inputFirstName.setText(currentDoctor.getFirstName());
                inputLastName.setText(currentDoctor.getLastName());
                inputEmail.setText(currentDoctor.getEmail());
                inputPhone.setText(currentDoctor.getPhone());
                inputAddress.setText(currentDoctor.getAddress());
                
                // Set fields as read-only initially
                setPersonalInfoEditable(false);
                
                // Load profile picture if available
                if (currentDoctor.getProfilePictureUrl() != null && !currentDoctor.getProfilePictureUrl().isEmpty()) {
                    loadProfilePicture(currentDoctor.getProfilePictureUrl());
                } else {
                    profileImageView.setImageResource(R.drawable.ic_doctor_avatar);
                }
                
            } else {
                showToast("Error loading doctor profile data");
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
                profileImageView.setImageResource(R.drawable.ic_doctor_avatar);
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
                profileImageView.setImageResource(R.drawable.ic_doctor_avatar);
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
                            profileImageView.setImageResource(R.drawable.ic_doctor_avatar);
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("DoctorProfile", "❌ Error loading base64 image: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (profileImageView != null) {
                            profileImageView.setImageResource(R.drawable.ic_doctor_avatar);
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
                                profileImageView.setImageResource(R.drawable.ic_doctor_avatar);
                            }
                        });
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (profileImageView != null) {
                                profileImageView.setImageResource(R.drawable.ic_doctor_avatar);
                            }
                        });
                    }
                }
            }).start();
        } catch (Exception e) {
            profileImageView.setImageResource(R.drawable.ic_doctor_avatar);
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
            loadDoctorProfile();
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
     * Save profile changes to database
     */
    private void saveProfileChanges() {
        try {
            // Update personal information
            if (isPersonalInfoEditable) {
                currentDoctor.setFirstName(getText(inputFirstName));
                currentDoctor.setLastName(getText(inputLastName));
                currentDoctor.setEmail(getText(inputEmail));
                currentDoctor.setPhone(getText(inputPhone));
                currentDoctor.setAddress(getText(inputAddress));
            }
            
            // Save to database
            boolean updated = databaseHelper.updateEmployee(currentDoctor);
            
            if (updated) {
                showToast("✅ Profile updated successfully!");
                
                // Update header with the exact name from admin registration
                if (loggedInFullName != null && !loggedInFullName.isEmpty()) {
                    doctorNameTextView.setText(loggedInFullName);
                } else {
                    doctorNameTextView.setText(currentDoctor.getFullName());
                }
                
                // Reset edit states
                isPersonalInfoEditable = false;
                setPersonalInfoEditable(false);
                
                // Reset button states
                editPersonalInfoButton.setText("Edit");
                editPersonalInfoButton.setBackgroundColor(getResources().getColor(R.color.primary_blue));
                
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
     * Change password using Firebase Authentication (like forgot password feature)
     */
    private void changePassword(String currentPassword, String newPassword) {
        try {
            // Get user's email from current doctor data
            String email = currentDoctor.getEmail();
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
     * Show profile picture dialog
     */
    private void showProfilePictureDialog() {
        try {
            if (getContext() == null) {
                android.util.Log.e("DoctorProfile", "❌ Context is null, cannot show dialog");
                showToast("Error: Unable to show dialog. Please try again.");
                return;
            }
            
            android.util.Log.d("DoctorProfile", "📸 Showing profile picture dialog...");
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Change Profile Picture");
            
            // Build options list dynamically
            java.util.ArrayList<String> options = new java.util.ArrayList<>();
            options.add("Choose from Gallery");
            if (currentDoctor != null && currentDoctor.getProfilePictureUrl() != null && !currentDoctor.getProfilePictureUrl().isEmpty()) {
                options.add("Remove Picture");
            }
            
            String[] optionsArray = options.toArray(new String[0]);
            
            builder.setItems(optionsArray, (dialog, which) -> {
                try {
                    android.util.Log.d("DoctorProfile", "📸 User selected option: " + which);
                    switch (which) {
                        case 0: // Choose from Gallery
                            android.util.Log.d("DoctorProfile", "📸 Opening gallery...");
                            pickImageFromGallery();
                            break;
                        case 1: // Remove Picture (if available)
                            if (options.size() > 1 && currentDoctor != null && currentDoctor.getProfilePictureUrl() != null) {
                                android.util.Log.d("DoctorProfile", "🗑️ Removing profile picture...");
                                removeProfilePicture();
                            }
                            break;
                    }
                } catch (Exception e) {
                    android.util.Log.e("DoctorProfile", "❌ Error in dialog option handler: " + e.getMessage(), e);
                    e.printStackTrace();
                    showToast("Error: " + e.getMessage());
                }
            });
            builder.setNegativeButton("Cancel", null);
            
            AlertDialog dialog = builder.create();
            dialog.show();
            android.util.Log.d("DoctorProfile", "✅ Profile picture dialog shown successfully");
        } catch (Exception e) {
            android.util.Log.e("DoctorProfile", "❌ Error showing profile picture dialog: " + e.getMessage(), e);
            e.printStackTrace();
            showToast("Error showing dialog: " + e.getMessage());
        }
    }
    
    /**
     * Pick image from gallery
     */
    private void pickImageFromGallery() {
        try {
            if (getContext() == null) {
                android.util.Log.e("DoctorProfile", "❌ Context is null, cannot open gallery");
                showToast("Error: Unable to open gallery. Please try again.");
                return;
            }
            
            if (imagePickerLauncher == null) {
                android.util.Log.e("DoctorProfile", "❌ Image picker launcher is null");
                showToast("Error: Image picker not initialized");
                return;
            }
            
            android.util.Log.d("DoctorProfile", "📸 Opening gallery to pick image...");
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            
            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                imagePickerLauncher.launch(intent);
                android.util.Log.d("DoctorProfile", "✅ Gallery intent launched successfully");
            } else {
                android.util.Log.e("DoctorProfile", "❌ No app found to handle gallery intent");
                showToast("Error: No gallery app found. Please install a gallery app.");
            }
        } catch (Exception e) {
            android.util.Log.e("DoctorProfile", "❌ Error opening gallery: " + e.getMessage(), e);
            e.printStackTrace();
            showToast("Error opening gallery: " + e.getMessage());
        }
    }
    
    /**
     * Upload profile picture - Convert to base64 and store in Firebase RTDB
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
                // Convert image URI to base64 string
                String base64Image = ImageUtils.convertImageUriToBase64(getContext(), imageUri);
                
                if (base64Image == null || base64Image.isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("❌ Failed to convert image. Please try again.");
                        });
                    }
                    return;
                }
                
                // Update on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Update database with base64 string
                        boolean updated = databaseHelper.updateEmployeeProfilePicture(loggedInEmployeeId, base64Image);
                        
                        if (updated) {
                            // Update current doctor object
                            currentDoctor.setProfilePictureUrl(base64Image);
                            
                            // Load the new image from base64
                            loadProfilePicture(base64Image);
                            
                            // Sync to Firebase RTDB
                            syncProfilePictureToFirebase(base64Image);
                            
                            showToast("✅ Profile picture updated successfully!");
                        } else {
                            showToast("❌ Failed to update profile picture in database");
                        }
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("DoctorProfile", "❌ Error converting image: " + e.getMessage(), e);
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
            
            // Update profile picture in Firebase
            employeeRef.child("profile_picture_url").setValue(base64Image)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("DoctorProfile", "✅ Profile picture synced to Firebase RTDB");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DoctorProfile", "❌ Failed to sync profile picture to Firebase: " + e.getMessage());
                });
        } catch (Exception e) {
            android.util.Log.e("DoctorProfile", "❌ Error syncing to Firebase: " + e.getMessage(), e);
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
                currentDoctor.setProfilePictureUrl(null);
                
                // Reset to default avatar
                profileImageView.setImageResource(R.drawable.ic_doctor_avatar);
                
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


















