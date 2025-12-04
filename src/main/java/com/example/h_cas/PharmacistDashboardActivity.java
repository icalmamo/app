package com.example.h_cas;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Employee;
import com.example.h_cas.models.Medicine;
import com.example.h_cas.models.Notification;
import com.example.h_cas.utils.NotificationDropdownHelper;

import java.util.List;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * PharmacistDashboardActivity provides the main interface for pharmacists.
 * Features include medication dispensing, inventory management, drug interactions, and prescription verification.
 */
public class PharmacistDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private MaterialToolbar toolbar;
    private TextView welcomeTextView;
    private ImageView notificationButton;
    private ImageView logoutButton;
    private TextView notificationBadge;
    private Employee currentPharmacist;
    private HCasDatabaseHelper databaseHelper;
    private String loggedInFullName;
    private String loggedInUsername;
    private String loggedInRole;
    private ImageView pharmacistAvatarImageView;
    private StorageReference storageReference;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private NotificationDropdownHelper notificationDropdownHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pharmacist_dashboard);
        
        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pharmacistMainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeDatabase();
        initializeViews();
        setupToolbar();
        setupBottomNavigation();
        setupNotificationButton();
        setupLogoutButton();
        setupImagePicker();
        initializeFirebaseStorage();
        
        // Load default dashboard fragment
        loadFragment(new PharmacistDashboardFragment());
        toolbar.setTitle("Pharmacist Dashboard");
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(this);
        
        // Get employee data from intent


        Intent intent = getIntent();
        loggedInFullName = intent.getStringExtra("FULL_NAME");
        loggedInUsername = intent.getStringExtra("USERNAME");
        loggedInRole = intent.getStringExtra("ROLE");
        
        // Create current pharmacist object from intent data
        currentPharmacist = new Employee();
        currentPharmacist.setEmployeeId(intent.getStringExtra("EMPLOYEE_ID"));
        currentPharmacist.setFirstName(intent.getStringExtra("FIRST_NAME"));
        currentPharmacist.setLastName(intent.getStringExtra("LAST_NAME"));
        currentPharmacist.setUsername(loggedInUsername);
        currentPharmacist.setRole(loggedInRole);
        currentPharmacist.setEmail(intent.getStringExtra("EMAIL"));
    }

    /**
     * Initialize all view references from the layout
     */
    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        toolbar = findViewById(R.id.toolbar);
        welcomeTextView = findViewById(R.id.welcomeTextView);
        notificationButton = findViewById(R.id.notificationButton);
        notificationBadge = findViewById(R.id.notificationBadge);
        logoutButton = findViewById(R.id.logoutButton);
    }

    /**
     * Setup the toolbar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
    }

    /**
     * Setup notification button click handler
     */
    private void setupNotificationButton() {
        if (notificationButton != null) {
            notificationDropdownHelper = new NotificationDropdownHelper(this);
            // Set listener to update badge when notifications change
            notificationDropdownHelper.setNotificationCountListener(count -> {
                updateNotificationBadge(count);
            });
            loadNotifications();
            notificationButton.setOnClickListener(v -> {
                notificationDropdownHelper.toggle(notificationButton);
            });
        }
    }

    /**
     * Setup logout button click handler
     */
    private void setupLogoutButton() {
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                handleLogout();
            });
        }
    }

    /**
     * Handle user logout with confirmation dialog
     */
    private void handleLogout() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to log out?");
        builder.setPositiveButton("Yes, Logout", (dialog, which) -> {
            Intent intent = new Intent(PharmacistDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.show();
    }
    
    @Override
    public void onBackPressed() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Exit");
        builder.setMessage("Are you sure you want to exit?");
        builder.setPositiveButton("Yes, Exit", (dialog, which) -> {
            finish();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.show();
    }

    /**
     * Load notifications for Pharmacist - shows expired/expiring medicines
     */
    private void loadNotifications() {
        // Load notifications in background thread
        com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
            try {
                // Get all medicines
                List<com.example.h_cas.models.Medicine> allMedicines = databaseHelper != null ? 
                    databaseHelper.getAllMedicines() : new java.util.ArrayList<>();
                
                // Filter expired and expiring medicines
                java.util.List<com.example.h_cas.models.Medicine> expiredMedicines = new java.util.ArrayList<>();
                java.util.List<com.example.h_cas.models.Medicine> expiringMedicines = new java.util.ArrayList<>();
                
                java.util.Calendar today = java.util.Calendar.getInstance();
                java.util.Calendar thirtyDaysLater = java.util.Calendar.getInstance();
                thirtyDaysLater.add(java.util.Calendar.DAY_OF_MONTH, 30);
                
                for (com.example.h_cas.models.Medicine medicine : allMedicines) {
                    if (medicine.getExpiryDate() != null && !medicine.getExpiryDate().isEmpty()) {
                        try {
                            // Parse expiry date (assuming format like "2024-12-31" or "31/12/2024")
                            java.util.Date expiryDate = parseDate(medicine.getExpiryDate());
                            if (expiryDate != null) {
                                java.util.Calendar expiryCal = java.util.Calendar.getInstance();
                                expiryCal.setTime(expiryDate);
                                
                                if (expiryCal.before(today)) {
                                    // Already expired
                                    expiredMedicines.add(medicine);
                                } else if (expiryCal.before(thirtyDaysLater)) {
                                    // Expiring within 30 days
                                    expiringMedicines.add(medicine);
                                }
                            }
                        } catch (Exception e) {
                            // Skip if date parsing fails
                        }
                    }
                }
                
                // Convert to notifications
                java.util.List<Notification> notifications = new java.util.ArrayList<>();
                
                // Add expired medicines first (higher priority)
                for (com.example.h_cas.models.Medicine medicine : expiredMedicines) {
                    String message = "EXPIRED: " + medicine.getMedicineName() + 
                        (medicine.getDosage() != null ? " (" + medicine.getDosage() + ")" : "") +
                        " - Expired on " + medicine.getExpiryDate();
                    Notification notif = new Notification(
                        medicine.getMedicineId(),
                        "Inventory Alert",
                        message,
                        "Urgent"
                    );
                    notifications.add(notif);
                }
                
                // Add expiring medicines
                for (com.example.h_cas.models.Medicine medicine : expiringMedicines) {
                    String message = "Expiring soon: " + medicine.getMedicineName() + 
                        (medicine.getDosage() != null ? " (" + medicine.getDosage() + ")" : "") +
                        " - Expires on " + medicine.getExpiryDate();
                    Notification notif = new Notification(
                        medicine.getMedicineId(),
                        "Inventory Alert",
                        message,
                        "Warning"
                    );
                    notifications.add(notif);
                }
                
                // Limit to 10 most recent/urgent
                final java.util.List<Notification> finalNotifications;
                if (notifications.size() > 10) {
                    finalNotifications = new java.util.ArrayList<>(notifications.subList(0, 10));
                } else {
                    finalNotifications = new java.util.ArrayList<>(notifications);
                }
                
                // Update UI on main thread
                com.example.h_cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                    if (notificationDropdownHelper != null) {
                        notificationDropdownHelper.setNotifications(finalNotifications);
                    }
                    updateNotificationBadge(finalNotifications.size());
                });
            } catch (Exception e) {
                android.util.Log.e("PharmacistDashboard", "Error loading notifications", e);
            }
        });
    }

    /**
     * Parse date string to Date object
     */
    private java.util.Date parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        
        // Try different date formats
        java.text.SimpleDateFormat[] formats = {
            new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()),
            new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()),
            new java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault()),
            new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
        };
        
        for (java.text.SimpleDateFormat format : formats) {
            try {
                return format.parse(dateString);
            } catch (Exception e) {
                // Try next format
            }
        }
        
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh notifications when activity resumes
        loadNotifications();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationDropdownHelper != null) {
            notificationDropdownHelper.cleanup();
        }
    }

    /**
     * Setup the bottom navigation bar
     */
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_pharmacist_dashboard) {
                loadFragment(new PharmacistDashboardFragment());
                toolbar.setTitle("Pharmacist Dashboard");
                return true;
            } else if (itemId == R.id.nav_medication_dispensing) {
                loadFragment(new MedicationDispensingFragment());
                toolbar.setTitle("Medication Dispensing");
                return true;
            } else if (itemId == R.id.nav_inventory) {
                loadFragment(new NewEnhancedInventoryFragment());
                toolbar.setTitle("Inventory");
                return true;
            } else if (itemId == R.id.nav_prescription_verification) {
                loadFragment(new PrescriptionVerificationFragment());
                toolbar.setTitle("Prescription Verification");
                return true;
            } else if (itemId == R.id.nav_pharmacist_settings) {
                loadFragment(new PharmacistSettingsFragment());
                toolbar.setTitle("Settings");
                return true;
            }
            
            return false;
        });
        
        // Set default selected item
        bottomNavigationView.setSelectedItemId(R.id.nav_pharmacist_dashboard);
    }
    
    /**
     * Initialize Firebase Storage
     */
    private void initializeFirebaseStorage() {
        try {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing storage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Setup image picker launcher
     */
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data != null) {
                        // Check if it's from camera (has extra data)
                        if (data.getExtras() != null && data.getExtras().get("data") != null) {
                            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                            if (bitmap != null) {
                                uploadProfilePictureFromBitmap(bitmap);
                            }
                        } else {
                            // It's from gallery
                            Uri imageUri = data.getData();
                            if (imageUri != null) {
                                uploadProfilePicture(imageUri);
                            }
                        }
                    }
                }
            }
        );
    }
    
    /**
     * Show profile picture dialog
     */
    private void showProfilePictureDialog() {
        if (currentPharmacist == null) {
            Toast.makeText(this, "Error: Pharmacist data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Change Profile Picture");
            
            // Build options list dynamically
            java.util.ArrayList<String> options = new java.util.ArrayList<>();
            options.add("Choose from Gallery");
            options.add("Take Photo");
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
                        case 1: // Take Photo
                            takePhoto();
                            break;
                        case 2: // Remove Picture (if available)
                            if (options.size() > 2 && currentPharmacist.getProfilePictureUrl() != null) {
                                removeProfilePicture();
                            }
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(PharmacistDashboardActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Pick image from gallery
     */
    private void pickImageFromGallery() {
        try {
            if (imagePickerLauncher == null) {
                Toast.makeText(this, "Error: Image picker not initialized", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening gallery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Take photo with camera
     */
    private void takePhoto() {
        try {
            if (imagePickerLauncher == null) {
                Toast.makeText(this, "Error: Image picker not initialized", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                imagePickerLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Upload profile picture to Firebase Storage
     */
    private void uploadProfilePicture(Uri imageUri) {
        if (imageUri == null || storageReference == null) {
            Toast.makeText(this, "Error: Invalid image", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "Uploading profile picture...", Toast.LENGTH_SHORT).show();
        
        try {
            // Create reference to profile pictures folder
            String fileName = "profile_" + currentPharmacist.getEmployeeId() + "_" + System.currentTimeMillis() + ".jpg";
            StorageReference profileRef = storageReference.child("profile_pictures/" + fileName);
            
            // Upload file directly from URI
            UploadTask uploadTask = profileRef.putFile(imageUri);
            
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Get download URL
                profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    
                    // Update database with profile picture URL
                    boolean updated = databaseHelper.updateEmployeeProfilePicture(currentPharmacist.getEmployeeId(), downloadUrl);
                    
                    if (updated) {
                        // Update current pharmacist object
                        currentPharmacist.setProfilePictureUrl(downloadUrl);
                        
                        // Reload pharmacist from database
                        currentPharmacist = databaseHelper.getEmployeeByUsername(loggedInUsername);
                        
                        // Load the new image
                        loadProfilePicture(downloadUrl);
                        
                        Toast.makeText(this, "✅ Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "❌ Failed to update profile picture in database", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Error getting download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "❌ Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
            
        } catch (Exception e) {
            Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Load profile picture from URL
     */
    private void loadProfilePicture(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty() || pharmacistAvatarImageView == null) {
            return;
        }
        
        try {
            // Use a background thread to load the image
            new Thread(() -> {
                try {
                    URL url = new URL(imageUrl);
                    Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    
                    // Update UI on main thread
                    runOnUiThread(() -> {
                        if (pharmacistAvatarImageView != null && bitmap != null) {
                            pharmacistAvatarImageView.setImageBitmap(bitmap);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Upload profile picture from bitmap (for camera)
     */
    private void uploadProfilePictureFromBitmap(Bitmap bitmap) {
        if (bitmap == null || storageReference == null) {
            Toast.makeText(this, "Error: Invalid image", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "Uploading profile picture...", Toast.LENGTH_SHORT).show();
        
        try {
            // Create reference to profile pictures folder
            String fileName = "profile_" + currentPharmacist.getEmployeeId() + "_" + System.currentTimeMillis() + ".jpg";
            StorageReference profileRef = storageReference.child("profile_pictures/" + fileName);
            
            // Convert bitmap to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            byte[] data = baos.toByteArray();
            UploadTask uploadTask = profileRef.putBytes(data);
            
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Get download URL
                profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    
                    // Update database with profile picture URL
                    boolean updated = databaseHelper.updateEmployeeProfilePicture(currentPharmacist.getEmployeeId(), downloadUrl);
                    
                    if (updated) {
                        // Update current pharmacist object
                        currentPharmacist.setProfilePictureUrl(downloadUrl);
                        
                        // Reload pharmacist from database
                        currentPharmacist = databaseHelper.getEmployeeByUsername(loggedInUsername);
                        
                        // Load the new image
                        loadProfilePicture(downloadUrl);
                        
                        Toast.makeText(this, "✅ Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "❌ Failed to update profile picture in database", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Error getting download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "❌ Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
            
        } catch (Exception e) {
            Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Remove profile picture
     */
    private void removeProfilePicture() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove Profile Picture");
        builder.setMessage("Are you sure you want to remove your profile picture?");
        builder.setPositiveButton("Remove", (dialog, which) -> {
            // Update database
            boolean updated = databaseHelper.updateEmployeeProfilePicture(currentPharmacist.getEmployeeId(), null);
            
            if (updated) {
                currentPharmacist.setProfilePictureUrl(null);
                
                // Reload pharmacist from database
                currentPharmacist = databaseHelper.getEmployeeByUsername(loggedInUsername);
                
                // Reset to default avatar
                if (pharmacistAvatarImageView != null) {
                    pharmacistAvatarImageView.setImageResource(R.drawable.ic_pharmacist_avatar);
                }
                
                Toast.makeText(this, "✅ Profile picture removed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ Failed to remove profile picture", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Load a fragment into the main content area
     * @param fragment The fragment to load
     */
    public void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }
    
    /**
     * Update notification badge counter
     */
    private void updateNotificationBadge(int count) {
        if (notificationBadge != null) {
            if (count > 0) {
                notificationBadge.setVisibility(android.view.View.VISIBLE);
                // Show count, but if > 99, show "99+"
                if (count > 99) {
                    notificationBadge.setText("99+");
                    // Adjust size for longer text
                    notificationBadge.getLayoutParams().width = (int) (24 * getResources().getDisplayMetrics().density);
                } else {
                    notificationBadge.setText(String.valueOf(count));
                    notificationBadge.getLayoutParams().width = (int) (18 * getResources().getDisplayMetrics().density);
                }
                notificationBadge.requestLayout();
            } else {
                notificationBadge.setVisibility(android.view.View.GONE);
            }
        }
    }

}


