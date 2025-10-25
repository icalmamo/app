package com.example.h_cas;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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

/**
 * AdminDashboardActivity provides the main interface for healthcare administrators.
 * Features include employee management, system overview, and administrative controls.
 */
public class AdminDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private TextView welcomeTextView;

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
        setupNavigationDrawer();
        setupNavigationHeader();
        
        // Load default dashboard fragment
        loadFragment(new AdminDashboardFragment());
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
        navigationView.setCheckedItem(R.id.nav_dashboard);
    }

    /**
     * Setup the navigation header with admin info
     */
    private void setupNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView adminNameTextView = headerView.findViewById(R.id.adminNameTextView);
        TextView adminRoleTextView = headerView.findViewById(R.id.adminRoleTextView);
        
        adminNameTextView.setText("Healthcare Admin");
        adminRoleTextView.setText("System Administrator");
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
        
        if (itemId == R.id.nav_dashboard) {
            loadFragment(new AdminDashboardFragment());
            toolbar.setTitle("Admin Dashboard");
        } else if (itemId == R.id.nav_create_employee) {
            loadFragment(new CreateEmployeeFragment());
            toolbar.setTitle("Create Employee");
        } else if (itemId == R.id.nav_manage_employees) {
            loadFragment(new ManageEmployeesFragment());
            toolbar.setTitle("Manage Employees");
        } else if (itemId == R.id.nav_reports) {
            loadFragment(new ReportsFragment());
            toolbar.setTitle("Reports & Analytics");
        } else if (itemId == R.id.nav_settings) {
            loadFragment(new AdminSettingsFragment());
            toolbar.setTitle("Settings");
        } else if (itemId == R.id.nav_logout) {
            handleLogout();
            return true; // Don't continue if logout
        }
        
        // Always force Dashboard to remain highlighted after any menu selection
        navigationView.setCheckedItem(R.id.nav_dashboard);
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handle admin logout
     */
    private void handleLogout() {
        // Clear any admin session data here
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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



