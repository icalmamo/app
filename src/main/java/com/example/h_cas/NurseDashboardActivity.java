package com.example.h_cas;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
import com.example.h_cas.models.Prescription;
import com.example.h_cas.utils.NotificationDropdownHelper;
import com.google.firebase.database.ChildEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NurseDashboardActivity provides the main interface for nurses.
 * Features include patient care, vital signs monitoring, medication administration, and care plans.
 */
public class NurseDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private MaterialToolbar toolbar;
    private TextView welcomeTextView;
    private ImageView notificationButton;
    private ImageView logoutButton;
    private TextView notificationBadge;
    private Employee currentNurse;
    private HCasDatabaseHelper databaseHelper;
    private FirebaseRTDBHelper firebaseRTDBHelper;
    private String loggedInFullName;
    private String loggedInUsername;
    private String loggedInRole;
    private NotificationDropdownHelper notificationDropdownHelper;
    private ChildEventListener newPrescriptionsListener;
    private Set<String> knownPrescriptionIds = new HashSet<>();

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
        setupBottomNavigation();
        setupNotificationButton();
        setupLogoutButton();
        
        // Load default dashboard fragment
        loadFragment(createNurseDashboardFragment());
        toolbar.setTitle("Nurse Dashboard");
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(this);
        firebaseRTDBHelper = new FirebaseRTDBHelper(this);
        
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
            // Initialize notification dropdown helper
            notificationDropdownHelper = new NotificationDropdownHelper(this);
            
            // Set listener to update badge when notifications change
            notificationDropdownHelper.setNotificationCountListener(count -> {
                updateNotificationBadge(count);
            });
            
            // Set as nurse mode (hide mark all read button)
            notificationDropdownHelper.setNurseMode(true);
            
            // Load existing prescriptions and convert to notifications
            loadExistingPrescriptionNotifications();
            
            // Subscribe to new prescription notifications from Firebase
            subscribeToNewPrescriptionNotifications();
            
            notificationButton.setOnClickListener(v -> {
                // Toggle dropdown
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
            Intent intent = new Intent(NurseDashboardActivity.this, LoginActivity.class);
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
     * Load existing prescriptions from Firebase and convert them to notifications
     */
    private void loadExistingPrescriptionNotifications() {
        if (firebaseRTDBHelper == null) {
            return;
        }
        
        // Load all prescriptions from history
        firebaseRTDBHelper.getAllPrescriptionsFromHistory(prescriptions -> {
            if (prescriptions == null || prescriptions.isEmpty()) {
                android.util.Log.d("NurseDashboard", "No existing prescriptions found");
                return;
            }
            
            android.util.Log.d("NurseDashboard", "Loading " + prescriptions.size() + " existing prescriptions as notifications");
            
            // Convert prescriptions to notifications
            List<Notification> notifications = new ArrayList<>();
            for (Prescription prescription : prescriptions) {
                if (prescription == null || prescription.getPrescriptionId() == null) {
                    continue;
                }
                
                // Add to known prescriptions to avoid duplicate notifications
                synchronized (knownPrescriptionIds) {
                    knownPrescriptionIds.add(prescription.getPrescriptionId());
                }
                
                String doctorName = prescription.getDoctorName() != null && !prescription.getDoctorName().isEmpty()
                        ? prescription.getDoctorName()
                        : "Doctor";
                String patientName = prescription.getPatientName() != null && !prescription.getPatientName().isEmpty()
                        ? prescription.getPatientName()
                        : "Patient";
                String medication = prescription.getMedication() != null ? prescription.getMedication() : "medication";
                
                String originalDate = prescription.getCreatedDate();
                String timestamp = originalDate != null ? formatTimestamp(originalDate) : "Recently";
                
                String message = "New prescription for " + patientName + ": " + medication;
                if (prescription.getFrequency() != null && !prescription.getFrequency().isEmpty()) {
                    message += " (" + prescription.getFrequency();
                    if (prescription.getDuration() != null && !prescription.getDuration().isEmpty()) {
                        message += " - " + prescription.getDuration();
                    }
                    message += ")";
                }
                
                Notification notification = new Notification(
                        prescription.getPrescriptionId(),
                        "New Prescription",
                        message,
                        timestamp
                );
                notifications.add(notification);
            }
            
            // Set notifications (this will load read status from Firebase)
            runOnUiThread(() -> {
                if (notificationDropdownHelper != null && !notifications.isEmpty()) {
                    notificationDropdownHelper.setNotifications(notifications);
                    updateNotificationBadge(notificationDropdownHelper.getUnreadNotificationCount());
                }
            });
        });
    }
    
    /**
     * Listen for new prescriptions being added to Firebase RTDB and update notifications in real-time.
     */
    private void subscribeToNewPrescriptionNotifications() {
        if (firebaseRTDBHelper == null || newPrescriptionsListener != null) {
            return;
        }
        
        newPrescriptionsListener = firebaseRTDBHelper.listenForNewPrescriptions(prescription -> {
            if (prescription == null || prescription.getPrescriptionId() == null) {
                return;
            }
            
            boolean alreadyKnown;
            synchronized (knownPrescriptionIds) {
                alreadyKnown = !knownPrescriptionIds.add(prescription.getPrescriptionId());
            }
            if (alreadyKnown) {
                return;
            }
            
            String doctorName = prescription.getDoctorName() != null && !prescription.getDoctorName().isEmpty()
                    ? prescription.getDoctorName()
                    : "Doctor";
            String patientName = prescription.getPatientName() != null && !prescription.getPatientName().isEmpty()
                    ? prescription.getPatientName()
                    : "Patient";
            String medication = prescription.getMedication() != null ? prescription.getMedication() : "medication";
            
            String originalDate = prescription.getCreatedDate();
            String timestamp = originalDate != null ? formatTimestamp(originalDate) : "Recently";
            
            android.util.Log.d("NurseDashboard", "New prescription notification: " + medication + " for " + patientName + " - " + timestamp);
            
            String message = "New prescription for " + patientName + ": " + medication;
            if (prescription.getFrequency() != null && !prescription.getFrequency().isEmpty()) {
                message += " (" + prescription.getFrequency();
                if (prescription.getDuration() != null && !prescription.getDuration().isEmpty()) {
                    message += " - " + prescription.getDuration();
                }
                message += ")";
            }
            
            Notification notification = new Notification(
                    prescription.getPrescriptionId(),
                    "New Prescription",
                    message,
                    timestamp
            );
            
            runOnUiThread(() -> {
                if (notificationDropdownHelper != null) {
                    notificationDropdownHelper.addNotification(notification);
                    // Badge should show unread count, not total count
                    updateNotificationBadge(notificationDropdownHelper.getUnreadNotificationCount());
                } else {
                    synchronized (knownPrescriptionIds) {
                        updateNotificationBadge(knownPrescriptionIds.size());
                    }
                }
            });
        });
    }

    /**
     * Format timestamp for display
     */
    private String formatTimestamp(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "Recently";
        }
        try {
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
        // Subscribe to notifications if not already subscribed
        if (newPrescriptionsListener == null) {
            subscribeToNewPrescriptionNotifications();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firebase listener
        if (firebaseRTDBHelper != null && newPrescriptionsListener != null) {
            firebaseRTDBHelper.removePrescriptionListener(newPrescriptionsListener);
            newPrescriptionsListener = null;
        }
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
            
            if (itemId == R.id.nav_nurse_dashboard) {
                loadFragment(createNurseDashboardFragment());
                toolbar.setTitle("Nurse Dashboard");
                return true;
            } else if (itemId == R.id.nav_patient_registration) {
                loadFragment(new PatientRegistrationFragment());
                toolbar.setTitle("Patient Registration");
                return true;
            } else if (itemId == R.id.nav_patient_monitoring) {
                loadFragment(new PatientMonitoringFragment());
                toolbar.setTitle("Patient Monitoring");
                return true;
            } else if (itemId == R.id.nav_registered_patients) {
                loadFragment(new RegisteredPatientsFragment());
                toolbar.setTitle("Registered Patients");
                return true;
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
                return true;
            }
            
            return false;
        });
        
        // Set default selected item
        bottomNavigationView.setSelectedItemId(R.id.nav_nurse_dashboard);
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
