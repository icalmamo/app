package com.example.h_cas;

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
 * NurseDashboardActivity provides the main interface for nurses.
 * Features include patient care, vital signs monitoring, medication administration, and care plans.
 */
public class NurseDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private TextView welcomeTextView;
    private Employee currentNurse;
    private HCasDatabaseHelper databaseHelper;
    private String loggedInFullName;
    private String loggedInUsername;
    private String loggedInRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nurse_dashboard);
        
        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nurseMainLayout), (v, insets) -> {
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
        loadFragment(createNurseDashboardFragment());
        toolbar.setTitle("Nurse Dashboard");
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(this);
        
        // Get employee data from intent
        Intent intent = getIntent();
        loggedInFullName = intent.getStringExtra("FULL_NAME");
        loggedInUsername = intent.getStringExtra("USERNAME");
        loggedInRole = intent.getStringExtra("ROLE");
        
        // Create current nurse object from intent data
        currentNurse = new Employee();
        currentNurse.setEmployeeId(intent.getStringExtra("EMPLOYEE_ID"));
        currentNurse.setFirstName(intent.getStringExtra("FIRST_NAME"));
        currentNurse.setLastName(intent.getStringExtra("LAST_NAME"));
        currentNurse.setUsername(loggedInUsername);
        currentNurse.setRole(loggedInRole);
        currentNurse.setEmail(intent.getStringExtra("EMAIL"));
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
        navigationView.setCheckedItem(R.id.nav_nurse_dashboard);
    }

    /**
     * Setup the navigation header with nurse info
     */
    private void setupNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView nurseNameTextView = headerView.findViewById(R.id.nurseNameTextView);
        TextView nurseRoleTextView = headerView.findViewById(R.id.nurseRoleTextView);
        
        // Use the actual logged-in nurse's name
        if (loggedInFullName != null && !loggedInFullName.isEmpty()) {
            nurseNameTextView.setText(loggedInFullName);
        } else {
            nurseNameTextView.setText(currentNurse.getFullName());
        }
        
        if (loggedInRole != null && !loggedInRole.isEmpty()) {
            nurseRoleTextView.setText(loggedInRole);
        } else {
            nurseRoleTextView.setText("Registered Nurse");
        }
    }

    /**
     * Create NurseDashboardFragment with employee data
     */
    private NurseDashboardFragment createNurseDashboardFragment() {
        NurseDashboardFragment fragment = new NurseDashboardFragment();
        Bundle args = new Bundle();
        args.putString("FULL_NAME", loggedInFullName);
        args.putString("USERNAME", loggedInUsername);
        args.putString("ROLE", loggedInRole);
        fragment.setArguments(args);
        return fragment;
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
        
        if (itemId == R.id.nav_nurse_dashboard) {
            loadFragment(createNurseDashboardFragment());
            toolbar.setTitle("Nurse Dashboard");
        } else if (itemId == R.id.nav_patient_registration) {
            loadFragment(new PatientRegistrationFragment());
            toolbar.setTitle("Registration");
        } else if (itemId == R.id.nav_patient_monitoring) {
            loadFragment(new PatientMonitoringFragment());
            toolbar.setTitle("Monitoring");
        } else if (itemId == R.id.nav_registered_patients) {
            loadFragment(new RegisteredPatientsFragment());
            toolbar.setTitle("Registered Patients");
        } else if (itemId == R.id.nav_view_prescriptions) {
            loadFragment(new ViewPrescriptionsFragment());
            toolbar.setTitle("Doctor's Prescriptions");
        } else if (itemId == R.id.nav_nurse_profile) {
            NurseProfileFragment profileFragment = new NurseProfileFragment();
            Bundle args = new Bundle();
            args.putString("FULL_NAME", loggedInFullName);
            args.putString("USERNAME", loggedInUsername);
            args.putString("ROLE", loggedInRole);
            args.putString("EMPLOYEE_ID", currentNurse.getEmployeeId());
            args.putString("FIRST_NAME", currentNurse.getFirstName());
            args.putString("LAST_NAME", currentNurse.getLastName());
            args.putString("EMAIL", currentNurse.getEmail());
            profileFragment.setArguments(args);
            loadFragment(profileFragment);
            toolbar.setTitle("Nurse Profile");
        } else if (itemId == R.id.nav_nurse_logout) {
            handleLogout();
            return true; // Don't continue if logout
        }
        
        // Always force Dashboard to remain highlighted after any menu selection
        navigationView.setCheckedItem(R.id.nav_nurse_dashboard);
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handle nurse logout
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

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
