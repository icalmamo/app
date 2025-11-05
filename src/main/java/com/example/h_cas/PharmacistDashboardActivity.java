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
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Employee;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * PharmacistDashboardActivity provides the main interface for pharmacists.
 * Features include medication dispensing, inventory management, drug interactions, and prescription verification.
 */
public class PharmacistDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private TextView welcomeTextView;
    private Employee currentPharmacist;
    private HCasDatabaseHelper databaseHelper;
    private String loggedInFullName;
    private String loggedInUsername;
    private String loggedInRole;
    private ImageView pharmacistAvatarImageView;
    private StorageReference storageReference;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

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
        setupNavigationDrawer();
        setupImagePicker();
        initializeFirebaseStorage();
        setupNavigationHeader();
        
        // Load default dashboard fragment
        loadFragment(new PharmacistDashboardFragment());
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
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        welcomeTextView = findViewById(R.id.welcomeTextView);
    }

    /**
     * Setup the toolbar with navigation toggle
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, 
                R.string.navigation_drawer_open, 
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    /**
     * Setup the navigation drawer
     */
    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this);
        
        // Always keep Dashboard highlighted by default
        navigationView.setCheckedItem(R.id.nav_pharmacist_dashboard);
    }

    /**
     * Setup the navigation header with pharmacist info
     */
    private void setupNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        pharmacistAvatarImageView = headerView.findViewById(R.id.pharmacistAvatarImageView);
        TextView pharmacistNameTextView = headerView.findViewById(R.id.pharmacistNameTextView);
        TextView pharmacistRoleTextView = headerView.findViewById(R.id.pharmacistRoleTextView);
        
        // Use the actual logged-in pharmacist's name exactly as entered by admin
        if (loggedInFullName != null && !loggedInFullName.isEmpty()) {
            pharmacistNameTextView.setText(loggedInFullName);
        } else {
            pharmacistNameTextView.setText(currentPharmacist.getFullName());
        }
        
        if (loggedInRole != null && !loggedInRole.isEmpty()) {
            pharmacistRoleTextView.setText(loggedInRole);
        } else {
            pharmacistRoleTextView.setText("Licensed Pharmacist");
        }
        
        // Load profile picture if available
        if (currentPharmacist.getProfilePictureUrl() != null && !currentPharmacist.getProfilePictureUrl().isEmpty()) {
            loadProfilePicture(currentPharmacist.getProfilePictureUrl());
        }
        
        // Make profile picture clickable
        if (pharmacistAvatarImageView != null) {
            pharmacistAvatarImageView.setOnClickListener(v -> {
                try {
                    showProfilePictureDialog();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(PharmacistDashboardActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        // Reset navigation scroll position to top
        resetNavigationScrollToTop();
        
        if (itemId == R.id.nav_pharmacist_dashboard) {
            loadFragment(new PharmacistDashboardFragment());
            toolbar.setTitle("Pharmacist Dashboard");
        } else if (itemId == R.id.nav_medication_dispensing) {
            loadFragment(new MedicationDispensingFragment());
            toolbar.setTitle("Medication Dispensing");
        } else if (itemId == R.id.nav_inventory) {
            loadFragment(new NewEnhancedInventoryFragment());
            toolbar.setTitle("Enhanced Inventory");
        } else if (itemId == R.id.nav_medicine_history) {
            loadFragment(new MedicineHistoryFragment());
            toolbar.setTitle("History of Medicine");
        } else if (itemId == R.id.nav_prescription_verification) {
            loadFragment(new PrescriptionVerificationFragment());
            toolbar.setTitle("Prescription Verification");
        } else if (itemId == R.id.nav_pharmacist_settings) {
            loadFragment(new PharmacistSettingsFragment());
            toolbar.setTitle("Settings");
        } else if (itemId == R.id.nav_pharmacist_logout) {
            handleLogout();
            return true; // Don't continue if logout
        }
        
        // Always force Dashboard to remain highlighted after any menu selection
        navigationView.setCheckedItem(R.id.nav_pharmacist_dashboard);
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handle pharmacist logout
     */
    private void handleLogout() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Reset navigation scroll position to top
     */
    private void resetNavigationScrollToTop() {
        try {
            // Find the ScrollView or RecyclerView inside NavigationView
            ViewGroup navContainer = (ViewGroup) navigationView.getChildAt(0);
            for (int i = 0; i < navContainer.getChildCount(); i++) {
                View child = navContainer.getChildAt(i);
                if (child instanceof android.widget.ScrollView) {
                    ((android.widget.ScrollView) child).fullScroll(View.FOCUS_UP);
                    break;
                }
            }
        } catch (Exception e) {
            // Fallback: ignore scroll reset if not available
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}


