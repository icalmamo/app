package com.example.h_cas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Employee;
import com.example.h_cas.models.Notification;
import com.example.h_cas.utils.NotificationDropdownHelper;

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
    private TextView welcomeTextView;
    private ImageView notificationButton;
    private ImageView logoutButton;
    private TextView notificationBadge;
    private NotificationDropdownHelper notificationDropdownHelper;

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
        setupNotificationButton();
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
     * Setup the bottom navigation bar
     */
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_dashboard) {
                loadFragment(new AdminDashboardFragment());
                toolbar.setTitle("Admin Dashboard");
                return true;
            } else if (itemId == R.id.nav_create_employee) {
                loadFragment(new CreateEmployeeFragment());
                toolbar.setTitle("Create Employee");
                return true;
            } else if (itemId == R.id.nav_manage_employees) {
                loadFragment(new ManageEmployeesFragment());
                toolbar.setTitle("Manage Employees");
                return true;
            } else if (itemId == R.id.nav_reports) {
                loadFragment(new ReportsFragment());
                toolbar.setTitle("Reports & Analytics");
                return true;
            } else if (itemId == R.id.nav_settings) {
                loadFragment(new AdminSettingsFragment());
                toolbar.setTitle("Settings");
                return true;
            }
            
            return false;
        });
        
        // Set default selected item
        bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
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

    private void loadNotifications() {
        java.util.List<Notification> notifications = new java.util.ArrayList<>();
        Notification notif1 = new Notification("1", "System", 
            "New employee registration pending approval", "2:15 pm November 17");
        Notification notif2 = new Notification("2", "System", 
            "Weekly report generated successfully", "10:00 am November 17");
        notifications.add(notif1);
        notifications.add(notif2);
        if (notificationDropdownHelper != null) {
            notificationDropdownHelper.setNotifications(notifications);
        }
        updateNotificationBadge(notifications.size());
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationDropdownHelper != null) {
            notificationDropdownHelper.cleanup();
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

}



