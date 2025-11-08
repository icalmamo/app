package com.example.h_cas;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
 * DoctorDashboardActivity provides the main interface for doctors.
 * Features include patient management, diagnosis, prescriptions, and medical records.
 */
public class DoctorDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private TextView welcomeTextView;
    private Employee currentDoctor;
    private HCasDatabaseHelper databaseHelper;
    private String loggedInFullName;
    private String loggedInUsername;
    private String loggedInRole;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_doctor_dashboard);
        
        // Initialize views first
        initializeViews();
        
        // Apply window insets for edge-to-edge display (same as NurseDashboardActivity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.doctorMainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeDatabase();
        setupToolbar();
        setupNavigationDrawer();
        setupNavigationHeader();
        
        // Load default dashboard fragment
        loadFragment(new DoctorDashboardFragment());
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(this);
        
        // Get employee data from intent
        Intent intent = getIntent();
        loggedInFullName = intent.getStringExtra("FULL_NAME");
        loggedInUsername = intent.getStringExtra("USERNAME");
        loggedInRole = intent.getStringExtra("ROLE");
        
        // Create current doctor object from intent data
        currentDoctor = new Employee();
        currentDoctor.setEmployeeId(intent.getStringExtra("EMPLOYEE_ID"));
        currentDoctor.setFirstName(intent.getStringExtra("FIRST_NAME"));
        currentDoctor.setLastName(intent.getStringExtra("LAST_NAME"));
        currentDoctor.setUsername(loggedInUsername);
        currentDoctor.setRole(loggedInRole);
        currentDoctor.setEmail(intent.getStringExtra("EMAIL"));
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
        
        // Create drawer toggle FIRST - this handles the hamburger icon
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, 
                R.string.navigation_drawer_open, 
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(drawerToggle);
        
        // CRITICAL: Enable home button AFTER creating toggle - this makes hamburger icon clickable
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        
        // Sync state to show hamburger icon - MUST be called after setDisplayHomeAsUpEnabled
        drawerToggle.syncState();
        
        // Keep drawer unlocked so hamburger button works
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START);
    }

    /**
     * Setup the navigation drawer
     */
    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this);
        
        // Always keep Dashboard highlighted by default
        navigationView.setCheckedItem(R.id.nav_doctor_dashboard);
    }

    /**
     * Setup the navigation header with doctor info
     */
    private void setupNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView doctorNameTextView = headerView.findViewById(R.id.doctorNameTextView);
        TextView doctorRoleTextView = headerView.findViewById(R.id.doctorRoleTextView);
        ImageView doctorAvatarImageView = headerView.findViewById(R.id.doctorAvatarImageView);
        
        // Use the actual logged-in doctor's name exactly as entered by admin
        if (loggedInFullName != null && !loggedInFullName.isEmpty()) {
            doctorNameTextView.setText(loggedInFullName);
        } else {
            doctorNameTextView.setText(currentDoctor.getFullName());
        }
        
        // Always set role as "Doctor" to match admin design
        doctorRoleTextView.setText("Doctor");
        
        // Load profile picture if available
        if (currentDoctor != null && currentDoctor.getProfilePictureUrl() != null && !currentDoctor.getProfilePictureUrl().isEmpty()) {
            loadProfilePicture(doctorAvatarImageView, currentDoctor.getProfilePictureUrl());
        }
        
        // Make header clickable to open profile (optional enhancement)
        View profileSection = headerView.findViewById(R.id.profileSection);
        if (profileSection != null) {
            profileSection.setOnClickListener(v -> {
                // Navigate to doctor profile
                DoctorProfileFragment profileFragment = new DoctorProfileFragment();
                Bundle args = new Bundle();
                args.putString("FULL_NAME", loggedInFullName);
                args.putString("USERNAME", loggedInUsername);
                args.putString("ROLE", loggedInRole);
                args.putString("EMPLOYEE_ID", currentDoctor.getEmployeeId());
                args.putString("FIRST_NAME", currentDoctor.getFirstName());
                args.putString("LAST_NAME", currentDoctor.getLastName());
                args.putString("EMAIL", currentDoctor.getEmail());
                profileFragment.setArguments(args);
                loadFragment(profileFragment);
                toolbar.setTitle("Doctor's Profile");
                updateNavigationSelection(R.id.nav_doctor_profile);
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }
    }
    
    /**
     * Load profile picture from URL
     */
    private void loadProfilePicture(ImageView imageView, String imageUrl) {
        // TODO: Implement image loading from URL if needed
        // For now, use default avatar
        // You can use Glide, Picasso, or similar library here
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
        
        // Show back button if not on dashboard, show hamburger menu if on dashboard
        updateToolbarNavigation(fragment instanceof DoctorDashboardFragment);
    }
    
    /**
     * Update toolbar navigation icon based on current fragment
     * @param isDashboard true if on dashboard, false otherwise
     */
    private void updateToolbarNavigation(boolean isDashboard) {
        if (isDashboard) {
            // On dashboard - let ActionBarDrawerToggle handle everything normally
            // Don't interfere with the toggle - just ensure drawer is unlocked
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START);
            drawerToggle.setDrawerIndicatorEnabled(true);
            toolbar.setNavigationIcon(null); // Let toggle draw the hamburger
            toolbar.setNavigationOnClickListener(null); // Let toggle handle clicks
            
            // Ensure home button is enabled
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
            }
            
            // Sync state - this will show the hamburger icon
            drawerToggle.syncState();
        } else {
            // On other fragments - show back button instead of hamburger
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START);
            drawerToggle.setDrawerIndicatorEnabled(false);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            toolbar.setNavigationOnClickListener(v -> {
                loadFragment(new DoctorDashboardFragment());
                toolbar.setTitle("Doctor Dashboard");
                updateNavigationSelection(R.id.nav_doctor_dashboard);
            });
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        // Let ActionBarDrawerToggle handle hamburger button - this is the standard way
        // It will automatically open/close the drawer when hamburger is clicked
        if (drawerToggle != null && drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        
        // Handle back button for non-dashboard fragments
        if (item.getItemId() == android.R.id.home) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (!(currentFragment instanceof DoctorDashboardFragment)) {
                // Not on dashboard - handle as back button
                loadFragment(new DoctorDashboardFragment());
                toolbar.setTitle("Doctor Dashboard");
                updateNavigationSelection(R.id.nav_doctor_dashboard);
                return true;
            }
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred
        if (drawerToggle != null) {
            drawerToggle.syncState();
        }
    }
    
    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Sync the toggle state after configuration change
        if (drawerToggle != null) {
            drawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        // Reset navigation scroll position to top
        resetNavigationScrollToTop();
        
        if (itemId == R.id.nav_doctor_dashboard) {
            loadFragment(new DoctorDashboardFragment());
            toolbar.setTitle("Doctor Dashboard");
        } else if (itemId == R.id.nav_registered_patients) {
            loadFragment(new RegisteredPatientsFragment());
            toolbar.setTitle("Registered Patients");
        } else if (itemId == R.id.nav_patient_history) {
            loadFragment(new PatientHistoryFragment());
            toolbar.setTitle("Patient History");
        } else if (itemId == R.id.nav_doctor_profile) {
            DoctorProfileFragment profileFragment = new DoctorProfileFragment();
            Bundle args = new Bundle();
            args.putString("FULL_NAME", loggedInFullName);
            args.putString("USERNAME", loggedInUsername);
            args.putString("ROLE", loggedInRole);
            args.putString("EMPLOYEE_ID", currentDoctor.getEmployeeId());
            args.putString("FIRST_NAME", currentDoctor.getFirstName());
            args.putString("LAST_NAME", currentDoctor.getLastName());
            args.putString("EMAIL", currentDoctor.getEmail());
            profileFragment.setArguments(args);
            loadFragment(profileFragment);
            toolbar.setTitle("Doctor's Profile");
        } else if (itemId == R.id.nav_doctor_logout) {
            handleLogout();
            return true; // Don't continue if logout
        }
        
        // Always force Dashboard to remain highlighted after any menu selection
        navigationView.setCheckedItem(R.id.nav_doctor_dashboard);
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handle doctor logout
     */
    private void handleLogout() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    /**
     * Get current doctor employee object
     */
    public Employee getCurrentDoctor() {
        return currentDoctor;
    }
    
    /**
     * Get logged in full name
     */
    public String getLoggedInFullName() {
        return loggedInFullName;
    }
    
    /**
     * Update navigation drawer selection
     */
    public void updateNavigationSelection(int itemId) {
        navigationView.setCheckedItem(itemId);
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
        // Check if drawer is open
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        
        // Check if we're on a fragment other than dashboard
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (currentFragment != null && !(currentFragment instanceof DoctorDashboardFragment)) {
            // Navigate back to dashboard
            loadFragment(new DoctorDashboardFragment());
            toolbar.setTitle("Doctor Dashboard");
            updateNavigationSelection(R.id.nav_doctor_dashboard);
        } else {
            // On dashboard, exit app or handle normally
            super.onBackPressed();
        }
    }
}


