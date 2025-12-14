package com.healthcare.cas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import com.healthcare.cas.database.HCasDatabaseHelper;
import com.healthcare.cas.models.Employee;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * AdminDashboardActivity provides the main interface for healthcare administrators.
 * Features include employee management, system overview, and administrative controls.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private MaterialToolbar toolbar;
    private ImageView logoutButton;
    
    // Flag to prevent bottom navigation from overriding programmatic fragment loads
    private boolean isNavigatingProgrammatically = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);
        
        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminMainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupToolbar();
        setupBottomNavigation();
        setupLogoutButton();
        
        // Load default dashboard fragment
        loadFragment(new AdminDashboardFragment());
        toolbar.setTitle("Admin Dashboard");
    }

    /**
     * Initialize all view references from the layout
     */
    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        toolbar = findViewById(R.id.toolbar);
        logoutButton = findViewById(R.id.logoutButton);
    }

    /**
     * Setup the toolbar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
    }

    /**
     * Setup the bottom navigation bar
     */
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            // If we're navigating programmatically (from dashboard card click),
            // skip loading a new fragment as one was already loaded with filters
            if (isNavigatingProgrammatically) {
                isNavigatingProgrammatically = false; // Reset the flag
                return true; // Accept the selection but don't load new fragment
            }
            
            if (itemId == R.id.nav_dashboard) {
                loadFragment(new AdminDashboardFragment());
                toolbar.setTitle("Admin Dashboard");
                return true;
            } else if (itemId == R.id.nav_create_employee) {
                loadFragment(new CreateEmployeeFragment());
                toolbar.setTitle("Create Employee");
                return true;
            } else if (itemId == R.id.nav_manage_employees) {
                // When clicking Manage tab directly (not from dashboard),
                // load ManageEmployeesFragment without filter (show all employees)
                loadFragment(new ManageEmployeesFragment());
                toolbar.setTitle("Manage Employees");
                return true;
            } else if (itemId == R.id.nav_settings) {
                loadFragment(new AdminSettingsFragment());
                toolbar.setTitle("Admin Profile");
                return true;
            }
            
            return false;
        });
        
        // Set default selected item
        bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
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
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
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
     * Update bottom navigation selected item programmatically
     * This is called when navigating from dashboard cards to Manage Employees with filters
     * Sets a flag to prevent the bottom navigation listener from overriding the loaded fragment
     * @param itemId The item ID to select
     */
    public void updateBottomNavigation(int itemId) {
        if (bottomNavigationView != null) {
            // Set flag to prevent listener from loading a new fragment
            // This allows us to keep the filtered ManageEmployeesFragment
            isNavigatingProgrammatically = true;
            bottomNavigationView.setSelectedItemId(itemId);
        }
    }

}



