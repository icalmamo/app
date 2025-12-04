package com.example.h_cas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.database.FirebaseRTDBHelper;
import com.example.h_cas.models.Employee;
import com.example.h_cas.models.Notification;
import com.example.h_cas.models.Patient;
import com.example.h_cas.utils.NotificationDropdownHelper;

import com.google.firebase.database.ChildEventListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DoctorDashboardActivity provides the main interface for doctors.
 * Features include patient management, diagnosis, prescriptions, and medical records.
 */
public class DoctorDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private MaterialToolbar toolbar;
    private TextView welcomeTextView;
    private ImageView notificationButton;
    private ImageView logoutButton;
    private TextView notificationBadge;
    private Employee currentDoctor;
    private HCasDatabaseHelper databaseHelper;
    private FirebaseRTDBHelper firebaseRTDBHelper;
    private String loggedInFullName;
    private String loggedInUsername;
    private String loggedInRole;
    private NotificationDropdownHelper notificationDropdownHelper;
    private ChildEventListener newPatientsListener;
    private final Set<String> knownPatientIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_doctor_dashboard);
        
        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.doctorMainLayout), (v, insets) -> {
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
        
        // Load default dashboard fragment
        loadFragment(new DoctorDashboardFragment());
        toolbar.setTitle("Doctor Dashboard");
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(this);
        firebaseRTDBHelper = new FirebaseRTDBHelper(this);
        
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
            Intent intent = new Intent(DoctorDashboardActivity.this, LoginActivity.class);
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
     * Load notifications for Doctor - shows registered patients from Firebase RTDB
     */
    private void loadNotifications() {
        // Load patients from Firebase RTDB (primary source)
        if (firebaseRTDBHelper != null) {
            firebaseRTDBHelper.getAllPatients(patients -> {
                // Process on background thread
                com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                    try {
                        // Convert patients to notifications
                        java.util.List<Notification> notifications = new java.util.ArrayList<>();
                        synchronized (knownPatientIds) {
                            knownPatientIds.clear();
                        }
                        
                        // Limit to recent 10 patients
                        int count = Math.min(patients.size(), 10);
                        for (int i = 0; i < count; i++) {
                            com.example.h_cas.models.Patient patient = patients.get(i);
                            if (patient.getPatientId() != null) {
                                synchronized (knownPatientIds) {
                                    knownPatientIds.add(patient.getPatientId());
                                }
                            }
                            String patientName = patient.getFullName() != null && !patient.getFullName().isEmpty() 
                                ? patient.getFullName() 
                                : (patient.getFirstName() + " " + patient.getLastName()).trim();
                            
                            String message = "New patient registered: " + patientName;
                            String timestamp = patient.getCreatedDate() != null ? 
                                formatTimestamp(patient.getCreatedDate()) : "Recently";
                            
                            Notification notif = new Notification(
                                patient.getPatientId(),
                                "Patient Registration",
                                message,
                                timestamp
                            );
                            notifications.add(notif);
                        }
                        
                        // Update UI on main thread
                        com.example.h_cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                            if (notificationDropdownHelper != null) {
                                notificationDropdownHelper.setNotifications(notifications);
                            }
                            updateNotificationBadge(notifications.size());
                            subscribeToNewPatientNotifications();
                        });
                    } catch (Exception e) {
                        android.util.Log.e("DoctorDashboard", "Error loading notifications", e);
                    }
                });
            });
        } else {
            // Fallback to SQLite if Firebase not available
            com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                try {
                    // Get registered patients
                    List<com.example.h_cas.models.Patient> patients = databaseHelper != null ? 
                        databaseHelper.getAllPatients() : new java.util.ArrayList<>();
                    
                    // Convert patients to notifications
                    java.util.List<Notification> notifications = new java.util.ArrayList<>();
                    synchronized (knownPatientIds) {
                        knownPatientIds.clear();
                    }
                    
                    // Limit to recent 10 patients
                    int count = Math.min(patients.size(), 10);
                    for (int i = 0; i < count; i++) {
                        com.example.h_cas.models.Patient patient = patients.get(i);
                        if (patient.getPatientId() != null) {
                            synchronized (knownPatientIds) {
                                knownPatientIds.add(patient.getPatientId());
                            }
                        }
                        String patientName = patient.getFullName() != null && !patient.getFullName().isEmpty() 
                            ? patient.getFullName() 
                            : (patient.getFirstName() + " " + patient.getLastName()).trim();
                        
                        String message = "New patient registered: " + patientName;
                        String timestamp = patient.getCreatedDate() != null ? 
                            formatTimestamp(patient.getCreatedDate()) : "Recently";
                        
                        Notification notif = new Notification(
                            patient.getPatientId(),
                            "Patient Registration",
                            message,
                            timestamp
                        );
                        notifications.add(notif);
                    }
                    
                    // Update UI on main thread
                    com.example.h_cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                        if (notificationDropdownHelper != null) {
                            notificationDropdownHelper.setNotifications(notifications);
                        }
                        updateNotificationBadge(notifications.size());
                        subscribeToNewPatientNotifications();
                    });
                } catch (Exception e) {
                    android.util.Log.e("DoctorDashboard", "Error loading notifications", e);
                }
            });
        }
    }

    /**
     * Format timestamp for display
     */
    private String formatTimestamp(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "Recently";
        }
        // Simple formatting - you can enhance this with proper date parsing
        try {
            // If dateString contains time, use it; otherwise add "Recently"
            if (dateString.contains(" ")) {
                return dateString;
            }
            return dateString + " - Recently";
        } catch (Exception e) {
            return "Recently";
        }
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
        if (firebaseRTDBHelper != null && newPatientsListener != null) {
            firebaseRTDBHelper.removePatientListener(newPatientsListener);
            newPatientsListener = null;
        }
    }

    /**
     * Setup the bottom navigation bar
     */
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_doctor_dashboard) {
                loadFragment(new DoctorDashboardFragment());
                toolbar.setTitle("Doctor Dashboard");
                return true;
            } else if (itemId == R.id.nav_registered_patients) {
                loadFragment(new RegisteredPatientsFragment());
                toolbar.setTitle("Registered Patients");
                return true;
            } else if (itemId == R.id.nav_patient_history) {
                loadFragment(new PatientHistoryFragment());
                toolbar.setTitle("Patient History");
                return true;
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
                return true;
            }
            
            return false;
        });
        
        // Set default selected item
        bottomNavigationView.setSelectedItemId(R.id.nav_doctor_dashboard);
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
     * Get bottom navigation view (for fragments to update selection)
     */
    public BottomNavigationView getBottomNavigationView() {
        return bottomNavigationView;
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
    
    /**
     * Listen for new patients being registered in Firebase RTDB and update notifications in real-time.
     */
    private void subscribeToNewPatientNotifications() {
        if (firebaseRTDBHelper == null || newPatientsListener != null) {
            return;
        }
        
        newPatientsListener = firebaseRTDBHelper.listenForNewPatients(patient -> {
            if (patient == null || patient.getPatientId() == null) {
                return;
            }
            
            boolean alreadyKnown;
            synchronized (knownPatientIds) {
                alreadyKnown = !knownPatientIds.add(patient.getPatientId());
            }
            if (alreadyKnown) {
                return;
            }
            
            String patientName = patient.getFullName() != null && !patient.getFullName().isEmpty()
                    ? patient.getFullName()
                    : (patient.getFirstName() + " " + patient.getLastName()).trim();
            
            Notification notification = new Notification(
                    patient.getPatientId(),
                    "Patient Registration",
                    "New patient registered: " + patientName,
                    patient.getCreatedDate() != null ? formatTimestamp(patient.getCreatedDate()) : "Recently"
            );
            
            runOnUiThread(() -> {
                if (notificationDropdownHelper != null) {
                    notificationDropdownHelper.addNotification(notification);
                    updateNotificationBadge(notificationDropdownHelper.getTotalNotificationCount());
                } else {
                    synchronized (knownPatientIds) {
                        updateNotificationBadge(knownPatientIds.size());
                    }
                }
            });
        });
    }
}
