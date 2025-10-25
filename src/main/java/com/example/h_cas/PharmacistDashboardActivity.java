package com.example.h_cas;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Employee;

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
        } else if (itemId == R.id.nav_drug_interactions) {
            loadFragment(new DrugInteractionsFragment());
            toolbar.setTitle("Drug Interactions");
        } else if (itemId == R.id.nav_prescription_verification) {
            loadFragment(new PrescriptionVerificationFragment());
            toolbar.setTitle("Prescription Verification");
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


