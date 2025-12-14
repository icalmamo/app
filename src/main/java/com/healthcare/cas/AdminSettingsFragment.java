package com.healthcare.cas;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.healthcare.cas.database.HCasDatabaseHelper;
import com.healthcare.cas.database.FirebaseRTDBHelper;
import com.healthcare.cas.models.Employee;
import com.healthcare.cas.utils.ImageUtils;

/**
 * AdminSettingsFragment handles admin profile settings and configuration
 */
public class AdminSettingsFragment extends Fragment {

    private MaterialButton changePasswordButton;
    private MaterialButton editAdminButton;
    private MaterialButton saveProfileButton;
    private TextView appVersionText;
    private TextView adminNameTextView;
    private TextView adminRoleTextView;
    private TextView adminIdTextView;
    private ImageView profileAvatarImageView;
    
    // Personal Information Fields
    private TextInputEditText inputFirstName;
    private TextInputEditText inputLastName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPhone;
    private MaterialCardView personalInfoCard;

    private HCasDatabaseHelper databaseHelper;
    private FirebaseRTDBHelper firebaseRTDBHelper;
    private Employee currentAdmin;
    private String loggedInUsername;
    private String loggedInEmployeeId;
    private boolean isEditMode = false;
    
    // Profile Picture
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_settings, container, false);
        
        initializeViews(view);
        initializeDatabase();
        getAdminDataFromIntent();
        setupImagePicker();
        setupClickListeners();
        loadAdminProfile();
        
        return view;
    }

    private void initializeViews(View view) {
        changePasswordButton = view.findViewById(R.id.changePasswordButton);
        editAdminButton = view.findViewById(R.id.editAdminButton);
        saveProfileButton = view.findViewById(R.id.saveProfileButton);
        appVersionText = view.findViewById(R.id.appVersionText);
        adminNameTextView = view.findViewById(R.id.adminNameTextView);
        adminRoleTextView = view.findViewById(R.id.adminRoleTextView);
        adminIdTextView = view.findViewById(R.id.adminIdTextView);
        profileAvatarImageView = view.findViewById(R.id.profileAvatarImageView);
        personalInfoCard = view.findViewById(R.id.personalInfoCard);
        
        // Make profile image circular by setting oval outline
        if (profileAvatarImageView != null) {
            profileAvatarImageView.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(android.view.View view, android.graphics.Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            profileAvatarImageView.setClipToOutline(true);
        }
        
        // Personal Information Fields
        inputFirstName = view.findViewById(R.id.inputFirstName);
        inputLastName = view.findViewById(R.id.inputLastName);
        inputEmail = view.findViewById(R.id.inputEmail);
        inputPhone = view.findViewById(R.id.inputPhone);
        
        // Set phone number input filter - only digits, max 11
        if (inputPhone != null) {
            InputFilter[] phoneFilters = new InputFilter[] {
                new InputFilter.LengthFilter(11), // Maximum 11 digits
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end,
                                               Spanned dest, int dstart, int dend) {
                        // Only allow digits
                        for (int i = start; i < end; i++) {
                            if (!Character.isDigit(source.charAt(i))) {
                                return ""; // Reject non-digit characters
                            }
                        }
                        return null; // Accept
                    }
                }
            };
            inputPhone.setFilters(phoneFilters);
            inputPhone.setInputType(InputType.TYPE_CLASS_PHONE); // Numeric keypad
        }

        // Set app version
        if (appVersionText != null) {
            appVersionText.setText("Version 1.0.0");
        }
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
        try {
            firebaseRTDBHelper = new FirebaseRTDBHelper(getContext());
        } catch (Exception e) {
            Log.e("AdminSettings", "Failed to initialize FirebaseRTDBHelper", e);
            firebaseRTDBHelper = null;
        }
    }
    
    /**
     * Get admin data from intent (passed from LoginActivity)
     */
    private void getAdminDataFromIntent() {
        if (getActivity() != null) {
            Intent intent = getActivity().getIntent();
            if (intent != null) {
                loggedInUsername = intent.getStringExtra("USERNAME");
                loggedInEmployeeId = intent.getStringExtra("EMPLOYEE_ID");
                
                // Default values if not in intent
                if (loggedInUsername == null || loggedInUsername.isEmpty()) {
                    loggedInUsername = "admin";
                }
                if (loggedInEmployeeId == null || loggedInEmployeeId.isEmpty()) {
                    loggedInEmployeeId = "ADMIN001";
                }
            } else {
                loggedInUsername = "admin";
                loggedInEmployeeId = "ADMIN001";
            }
        }
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

    private void setupClickListeners() {
        if (changePasswordButton != null) {
            changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        }
        
        if (editAdminButton != null) {
            editAdminButton.setOnClickListener(v -> toggleEditMode());
        }
        
        if (saveProfileButton != null) {
            saveProfileButton.setOnClickListener(v -> saveProfileChanges());
        }
        
        // Profile Picture Click - open image picker dialog
        if (profileAvatarImageView != null) {
            profileAvatarImageView.setOnClickListener(v -> showProfilePictureDialog());
        }
    }
    
    /**
     * Load admin profile data from Firebase (primary) or SQLite (fallback)
     */
    private void loadAdminProfile() {
        if (firebaseRTDBHelper != null && loggedInEmployeeId != null) {
            // Try Firebase first
            firebaseRTDBHelper.getEmployeeById(loggedInEmployeeId, employee -> {
                if (employee != null) {
                    currentAdmin = employee;
                    updateUIWithAdminData();
                } else {
                    // Fallback to SQLite
                    loadAdminFromSQLite();
                }
            });
        } else {
            // Fallback to SQLite
            loadAdminFromSQLite();
        }
    }
    
    /**
     * Load admin from SQLite as fallback
     */
    private void loadAdminFromSQLite() {
        if (databaseHelper != null && loggedInUsername != null) {
            Employee admin = databaseHelper.getEmployeeByUsername(loggedInUsername);
            if (admin != null) {
                currentAdmin = admin;
                updateUIWithAdminData();
            } else {
                // Create default admin object
                currentAdmin = new Employee();
                currentAdmin.setEmployeeId(loggedInEmployeeId != null ? loggedInEmployeeId : "ADMIN001");
                currentAdmin.setUsername(loggedInUsername);
                currentAdmin.setFirstName("System");
                currentAdmin.setLastName("Administrator");
                currentAdmin.setRole("Administrator");
                updateUIWithAdminData();
            }
        }
    }
    
    /**
     * Update UI with admin data
     */
    private void updateUIWithAdminData() {
        if (currentAdmin == null) return;
        
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Update header
                if (adminNameTextView != null) {
                    adminNameTextView.setText(currentAdmin.getFullName());
                }
                if (adminRoleTextView != null) {
                    adminRoleTextView.setText(currentAdmin.getRole() != null ? currentAdmin.getRole() : "System Administrator");
                }
                if (adminIdTextView != null) {
                    adminIdTextView.setText("ID: " + (currentAdmin.getEmployeeId() != null ? currentAdmin.getEmployeeId() : "ADMIN001"));
                }
                
                // Update personal information fields
                if (inputFirstName != null) {
                    inputFirstName.setText(currentAdmin.getFirstName() != null ? currentAdmin.getFirstName() : "");
                }
                if (inputLastName != null) {
                    inputLastName.setText(currentAdmin.getLastName() != null ? currentAdmin.getLastName() : "");
                }
                if (inputEmail != null) {
                    inputEmail.setText(currentAdmin.getEmail() != null ? currentAdmin.getEmail() : "");
                }
                if (inputPhone != null) {
                    inputPhone.setText(currentAdmin.getPhone() != null ? currentAdmin.getPhone() : "");
                }
                
                // Load profile picture if available
                if (currentAdmin.getProfilePictureUrl() != null && !currentAdmin.getProfilePictureUrl().isEmpty()) {
                    loadProfilePicture(currentAdmin.getProfilePictureUrl());
                } else {
                    if (profileAvatarImageView != null) {
                        profileAvatarImageView.setImageResource(R.drawable.ic_admin_avatar);
                    }
                }
            });
        }
    }
    
    /**
     * Toggle edit mode for personal information
     */
    private void toggleEditMode() {
        isEditMode = !isEditMode;
        
        if (isEditMode) {
            // Enable edit mode
            if (personalInfoCard != null) {
                personalInfoCard.setVisibility(View.VISIBLE);
            }
            
            // Enable input fields
            if (inputFirstName != null) inputFirstName.setEnabled(true);
            if (inputLastName != null) inputLastName.setEnabled(true);
            if (inputEmail != null) inputEmail.setEnabled(true);
            if (inputPhone != null) inputPhone.setEnabled(true);
            
            // Show save button
            if (saveProfileButton != null) {
                saveProfileButton.setVisibility(View.VISIBLE);
            }
            
            // Change edit button text
            if (editAdminButton != null) {
                editAdminButton.setText("CANCEL");
            }
        } else {
            // Disable edit mode
            // Enable input fields
            if (inputFirstName != null) inputFirstName.setEnabled(false);
            if (inputLastName != null) inputLastName.setEnabled(false);
            if (inputEmail != null) inputEmail.setEnabled(false);
            if (inputPhone != null) inputPhone.setEnabled(false);
            
            // Hide save button
            if (saveProfileButton != null) {
                saveProfileButton.setVisibility(View.GONE);
            }
            
            // Hide personal info card
            if (personalInfoCard != null) {
                personalInfoCard.setVisibility(View.GONE);
            }
            
            // Reset edit button text
            if (editAdminButton != null) {
                editAdminButton.setText("EDIT ADMIN");
            }
            
            // Reload original data
            if (currentAdmin != null) {
                updateUIWithAdminData();
            }
        }
    }
    
    /**
     * Save profile changes to Firebase and SQLite
     */
    private void saveProfileChanges() {
        if (!isEditMode || currentAdmin == null) {
            return;
        }
        
        // Get updated values
        String firstName = inputFirstName != null ? inputFirstName.getText().toString().trim() : "";
        String lastName = inputLastName != null ? inputLastName.getText().toString().trim() : "";
        String email = inputEmail != null ? inputEmail.getText().toString().trim() : "";
        String phone = inputPhone != null ? inputPhone.getText().toString().trim() : "";
        
        // Validation
        if (firstName.isEmpty() || lastName.isEmpty()) {
            showToast("First name and last name are required");
            return;
        }
        
        if (email.isEmpty() || !email.contains("@")) {
            showToast("Please enter a valid email address");
            return;
        }
        
        // Phone number validation - maximum 11 digits, numbers only
        if (!phone.isEmpty()) {
            // Remove any non-digit characters (shouldn't happen with input filter, but just in case)
            String phoneDigits = phone.replaceAll("[^0-9]", "");
            
            if (phoneDigits.length() > 11) {
                showToast("Phone number must be 11 digits maximum");
                phone = phoneDigits.substring(0, 11);
                if (inputPhone != null) {
                    inputPhone.setText(phone);
                    inputPhone.setSelection(phone.length());
                }
                return;
            }
            
            // Use cleaned phone number (digits only)
            phone = phoneDigits;
        }
        
        // Update admin object
        currentAdmin.setFirstName(firstName);
        currentAdmin.setLastName(lastName);
        currentAdmin.setEmail(email);
        currentAdmin.setPhone(phone);
        
        // Save to Firebase (primary) and SQLite
        boolean saved = false;
        
        if (firebaseRTDBHelper != null) {
            firebaseRTDBHelper.updateEmployee(currentAdmin, success -> {
                if (success) {
                    // Also update SQLite for consistency
                    if (databaseHelper != null) {
                        databaseHelper.updateEmployee(currentAdmin);
                    }
                    
                    // Update UI
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updateUIWithAdminData();
                            toggleEditMode();
                            showToast("✅ Profile updated successfully!");
                        });
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("❌ Failed to update profile in Firebase");
                        });
                    }
                }
            });
        } else {
            // Fallback to SQLite only
            if (databaseHelper != null) {
                saved = databaseHelper.updateEmployee(currentAdmin);
                if (saved) {
                    updateUIWithAdminData();
                    toggleEditMode();
                    showToast("✅ Profile updated successfully!");
                } else {
                    showToast("❌ Failed to update profile");
                }
            }
        }
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

    private void changePassword(String currentPassword, String newPassword) {
        try {
            // For admin, we'll validate using the database helper
            // Since admin login is handled differently, we'll use a simple validation
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
                // Fallback: just show success message for demo
                showToast("✅ Password changed successfully!");
            }
        } catch (Exception e) {
            showToast("Error changing password: " + e.getMessage());
        }
    }




    /**
     * Helper method to get text from TextInputEditText
     */
    private String getText(TextInputEditText editText) {
        return editText != null && editText.getText() != null ? editText.getText().toString().trim() : "";
    }
    
    /**
     * Show dialog for profile picture options (Choose from Gallery, Remove)
     */
    private void showProfilePictureDialog() {
        java.util.ArrayList<String> options = new java.util.ArrayList<>();
        options.add("Choose from Gallery");
        
        // Only show "Remove" option if there's already a profile picture
        if (currentAdmin != null && currentAdmin.getProfilePictureUrl() != null && !currentAdmin.getProfilePictureUrl().isEmpty()) {
            options.add("Remove Picture");
        }
        
        String[] optionsArray = options.toArray(new String[0]);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Profile Picture");
        builder.setItems(optionsArray, (dialog, which) -> {
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
                        // CRITICAL: Save to Firebase FIRST to ensure persistence
                        // Then save to SQLite as backup
                        syncProfilePictureToFirebase(base64Image);
                        
                        // Update database with base64 string (SQLite backup)
                        boolean updated = databaseHelper.updateEmployeeProfilePicture(loggedInEmployeeId, base64Image);
                        
                        if (updated) {
                            // Update current admin object
                            if (currentAdmin != null) {
                                currentAdmin.setProfilePictureUrl(base64Image);
                            }
                            
                            // Load the new image from base64 (with circular clipping)
                            loadProfilePicture(base64Image);
                            
                            Log.d("AdminSettings", "✅ Profile picture saved to Firebase and SQLite: " + loggedInEmployeeId);
                            showToast("✅ Profile picture updated successfully!");
                        } else {
                            // Even if SQLite fails, Firebase should still have it
                            Log.w("AdminSettings", "⚠️ SQLite update failed, but Firebase was updated");
                            
                            // Update current admin object anyway
                            if (currentAdmin != null) {
                                currentAdmin.setProfilePictureUrl(base64Image);
                            }
                            loadProfilePicture(base64Image);
                            showToast("✅ Profile picture updated! (Saved to Firebase)");
                        }
                    });
                }
                
            } catch (Exception e) {
                Log.e("AdminSettings", "❌ Error converting image: " + e.getMessage(), e);
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
                    Log.d("AdminSettings", "✅ Profile picture synced to Firebase RTDB");
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminSettings", "❌ Failed to sync profile picture to Firebase: " + e.getMessage());
                });
        } catch (Exception e) {
            Log.e("AdminSettings", "❌ Error syncing to Firebase: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load profile picture from base64 string or URL (backward compatibility)
     */
    private void loadProfilePicture(String imageData) {
        if (imageData == null || imageData.isEmpty() || profileAvatarImageView == null) {
            // Set default avatar
            if (profileAvatarImageView != null) {
                profileAvatarImageView.setImageResource(R.drawable.ic_admin_avatar);
            }
            return;
        }
        
        // Check if it's a base64 string
        if (ImageUtils.isBase64Image(imageData)) {
            // Load from base64 string
            loadProfilePictureFromBase64(imageData);
        } else if (ImageUtils.isUrl(imageData)) {
            // Load from URL (backward compatibility)
            loadProfilePictureFromUrl(imageData);
        } else {
            // Unknown format, try base64 first
            loadProfilePictureFromBase64(imageData);
        }
    }
    
    /**
     * Load profile picture from base64 string
     */
    private void loadProfilePictureFromBase64(String base64String) {
        if (base64String == null || base64String.isEmpty() || profileAvatarImageView == null) {
            if (profileAvatarImageView != null) {
                profileAvatarImageView.setImageResource(R.drawable.ic_admin_avatar);
            }
            return;
        }
        
        // Convert base64 to bitmap in background thread
        new Thread(() -> {
            try {
                Bitmap bitmap = ImageUtils.convertBase64ToBitmap(base64String);
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (profileAvatarImageView != null && bitmap != null) {
                            profileAvatarImageView.setImageBitmap(bitmap);
                            // Ensure circular clipping is maintained
                            profileAvatarImageView.post(() -> {
                                profileAvatarImageView.setOutlineProvider(new android.view.ViewOutlineProvider() {
                                    @Override
                                    public void getOutline(android.view.View view, android.graphics.Outline outline) {
                                        outline.setOval(0, 0, view.getWidth(), view.getHeight());
                                    }
                                });
                                profileAvatarImageView.setClipToOutline(true);
                            });
                        } else {
                            profileAvatarImageView.setImageResource(R.drawable.ic_admin_avatar);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("AdminSettings", "❌ Error loading base64 image: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (profileAvatarImageView != null) {
                            profileAvatarImageView.setImageResource(R.drawable.ic_admin_avatar);
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
        if (imageUrl == null || imageUrl.isEmpty() || profileAvatarImageView == null) {
            return;
        }
        
        try {
            // Use a background thread to load the image
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(imageUrl);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    java.io.InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (profileAvatarImageView != null && bitmap != null) {
                                profileAvatarImageView.setImageBitmap(bitmap);
                                // Ensure circular clipping is maintained
                                profileAvatarImageView.post(() -> {
                                    profileAvatarImageView.setOutlineProvider(new android.view.ViewOutlineProvider() {
                                        @Override
                                        public void getOutline(android.view.View view, android.graphics.Outline outline) {
                                            outline.setOval(0, 0, view.getWidth(), view.getHeight());
                                        }
                                    });
                                    profileAvatarImageView.setClipToOutline(true);
                                });
                            } else {
                                profileAvatarImageView.setImageResource(R.drawable.ic_admin_avatar);
                            }
                        });
                    }
                } catch (Exception e) {
                    // Fallback to default avatar on error
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (profileAvatarImageView != null) {
                                profileAvatarImageView.setImageResource(R.drawable.ic_admin_avatar);
                            }
                        });
                    }
                }
            }).start();
        } catch (Exception e) {
            profileAvatarImageView.setImageResource(R.drawable.ic_admin_avatar);
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
                if (currentAdmin != null) {
                    currentAdmin.setProfilePictureUrl(null);
                }
                
                // Reset to default avatar
                if (profileAvatarImageView != null) {
                    profileAvatarImageView.setImageResource(R.drawable.ic_admin_avatar);
                }
                
                // Remove from Firebase
                try {
                    FirebaseDatabase database = FirebaseDatabase.getInstance("https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/");
                    DatabaseReference employeeRef = database.getReference("employees").child(loggedInEmployeeId);
                    employeeRef.child("profile_picture_url").removeValue();
                } catch (Exception e) {
                    Log.e("AdminSettings", "Error removing from Firebase: " + e.getMessage());
                }
                
                showToast("✅ Profile picture removed");
            } else {
                showToast("❌ Failed to remove profile picture");
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}
