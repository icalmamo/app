package com.healthcare.cas;

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

import com.healthcare.cas.database.HCasDatabaseHelper;
import com.healthcare.cas.database.FirebaseRTDBHelper;
import com.healthcare.cas.models.Employee;
import com.healthcare.cas.models.Notification;
import com.healthcare.cas.models.Patient;
import com.healthcare.cas.utils.NotificationDropdownHelper;

import com.google.firebase.database.ChildEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
                com.healthcare.cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                    try {
                        // Sort patients by createdDate descending (latest first)
                        // If dates are the same, use patient ID as secondary sort (newer IDs first)
                        Collections.sort(patients, new Comparator<com.healthcare.cas.models.Patient>() {
                            @Override
                            public int compare(com.healthcare.cas.models.Patient p1, com.healthcare.cas.models.Patient p2) {
                                String date1 = p1.getCreatedDate();
                                String date2 = p2.getCreatedDate();
                                
                                if (date1 == null && date2 == null) {
                                    // Both null - use patient ID as tiebreaker (descending)
                                    String id1 = p1.getPatientId() != null ? p1.getPatientId() : "";
                                    String id2 = p2.getPatientId() != null ? p2.getPatientId() : "";
                                    return id2.compareTo(id1);
                                }
                                if (date1 == null) return 1; // null dates go to end
                                if (date2 == null) return -1;
                                
                                try {
                                    // Try multiple date formats
                                    SimpleDateFormat[] formats = {
                                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                                        new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                                        new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault()),
                                        new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                                    };
                                    
                                    Date d1 = null, d2 = null;
                                    for (SimpleDateFormat sdf : formats) {
                                        try {
                                            if (d1 == null) d1 = sdf.parse(date1);
                                            if (d2 == null) d2 = sdf.parse(date2);
                                            if (d1 != null && d2 != null) break;
                                        } catch (ParseException e) {
                                            // Try next format
                                        }
                                    }
                                    
                                    if (d1 != null && d2 != null) {
                                        int dateCompare = d2.compareTo(d1); // Descending order (latest first)
                                        // If dates are equal, use patient ID as tiebreaker
                                        if (dateCompare == 0) {
                                            String id1 = p1.getPatientId() != null ? p1.getPatientId() : "";
                                            String id2 = p2.getPatientId() != null ? p2.getPatientId() : "";
                                            return id2.compareTo(id1); // Descending (newer IDs first)
                                        }
                                        return dateCompare;
                                    } else {
                                        // If parsing fails, compare as strings (descending)
                                        int stringCompare = date2.compareTo(date1);
                                        if (stringCompare == 0) {
                                            // Same date string - use patient ID
                                            String id1 = p1.getPatientId() != null ? p1.getPatientId() : "";
                                            String id2 = p2.getPatientId() != null ? p2.getPatientId() : "";
                                            return id2.compareTo(id1);
                                        }
                                        return stringCompare;
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e("DoctorDashboard", "Error comparing dates: " + date1 + " vs " + date2, e);
                                    // If parsing fails, compare as strings (descending)
                                    int stringCompare = date2.compareTo(date1);
                                    if (stringCompare == 0) {
                                        // Same date string - use patient ID
                                        String id1 = p1.getPatientId() != null ? p1.getPatientId() : "";
                                        String id2 = p2.getPatientId() != null ? p2.getPatientId() : "";
                                        return id2.compareTo(id1);
                                    }
                                    return stringCompare;
                                }
                            }
                        });
                        
                        android.util.Log.d("DoctorDashboard", "Sorted " + patients.size() + " patients. First patient date: " + 
                            (patients.size() > 0 && patients.get(0).getCreatedDate() != null ? patients.get(0).getCreatedDate() : "null"));
                        
                        // Convert patients to notifications
                        java.util.List<Notification> notifications = new java.util.ArrayList<>();
                        synchronized (knownPatientIds) {
                            knownPatientIds.clear();
                        }
                        
                        // Show ALL patients (not limited to 10)
                        int count = patients.size();
                        for (int i = 0; i < count; i++) {
                            com.healthcare.cas.models.Patient patient = patients.get(i);
                            if (patient.getPatientId() != null) {
                                synchronized (knownPatientIds) {
                                    knownPatientIds.add(patient.getPatientId());
                                }
                            }
                            String patientName = patient.getFullName() != null && !patient.getFullName().isEmpty() 
                                ? patient.getFullName() 
                                : (patient.getFirstName() + " " + patient.getLastName()).trim();
                            
                            String message = "New patient registered: " + patientName;
                            String originalDate = patient.getCreatedDate();
                            String timestamp = originalDate != null ? 
                                formatTimestamp(originalDate) : "Recently";
                            
                            android.util.Log.d("DoctorDashboard", "Notification for " + patientName + ": original=" + originalDate + ", formatted=" + timestamp);
                            
                            Notification notif = new Notification(
                                patient.getPatientId(),
                                "Patient Registration",
                                message,
                                timestamp
                            );
                            notifications.add(notif);
                        }
                        
                        // Update UI on main thread
                        com.healthcare.cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                            if (notificationDropdownHelper != null) {
                                notificationDropdownHelper.setNotifications(notifications);
                                // Badge should show unread count, not total count
                                updateNotificationBadge(notificationDropdownHelper.getUnreadNotificationCount());
                            } else {
                                updateNotificationBadge(notifications.size());
                            }
                            subscribeToNewPatientNotifications();
                        });
                    } catch (Exception e) {
                        android.util.Log.e("DoctorDashboard", "Error loading notifications", e);
                    }
                });
            });
        } else {
            // Fallback to SQLite if Firebase not available
            com.healthcare.cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                try {
                    // Get registered patients
                    List<com.healthcare.cas.models.Patient> patients = databaseHelper != null ? 
                        databaseHelper.getAllPatients() : new java.util.ArrayList<>();
                    
                    // Sort patients by createdDate descending (latest first)
                    // If dates are the same, use patient ID as secondary sort (newer IDs first)
                    Collections.sort(patients, new Comparator<com.healthcare.cas.models.Patient>() {
                        @Override
                        public int compare(com.healthcare.cas.models.Patient p1, com.healthcare.cas.models.Patient p2) {
                            String date1 = p1.getCreatedDate();
                            String date2 = p2.getCreatedDate();
                            
                            if (date1 == null && date2 == null) {
                                // Both null - use patient ID as tiebreaker (descending)
                                String id1 = p1.getPatientId() != null ? p1.getPatientId() : "";
                                String id2 = p2.getPatientId() != null ? p2.getPatientId() : "";
                                return id2.compareTo(id1);
                            }
                            if (date1 == null) return 1; // null dates go to end
                            if (date2 == null) return -1;
                            
                            try {
                                // Try multiple date formats
                                SimpleDateFormat[] formats = {
                                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                                    new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault()),
                                    new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                                };
                                
                                Date d1 = null, d2 = null;
                                for (SimpleDateFormat sdf : formats) {
                                    try {
                                        if (d1 == null) d1 = sdf.parse(date1);
                                        if (d2 == null) d2 = sdf.parse(date2);
                                        if (d1 != null && d2 != null) break;
                                    } catch (ParseException e) {
                                        // Try next format
                                    }
                                }
                                
                                if (d1 != null && d2 != null) {
                                    int dateCompare = d2.compareTo(d1); // Descending order (latest first)
                                    // If dates are equal, use patient ID as tiebreaker
                                    if (dateCompare == 0) {
                                        String id1 = p1.getPatientId() != null ? p1.getPatientId() : "";
                                        String id2 = p2.getPatientId() != null ? p2.getPatientId() : "";
                                        return id2.compareTo(id1); // Descending (newer IDs first)
                                    }
                                    return dateCompare;
                                } else {
                                    // If parsing fails, compare as strings (descending)
                                    int stringCompare = date2.compareTo(date1);
                                    if (stringCompare == 0) {
                                        // Same date string - use patient ID
                                        String id1 = p1.getPatientId() != null ? p1.getPatientId() : "";
                                        String id2 = p2.getPatientId() != null ? p2.getPatientId() : "";
                                        return id2.compareTo(id1);
                                    }
                                    return stringCompare;
                                }
                            } catch (Exception e) {
                                android.util.Log.e("DoctorDashboard", "Error comparing dates: " + date1 + " vs " + date2, e);
                                // If parsing fails, compare as strings (descending)
                                int stringCompare = date2.compareTo(date1);
                                if (stringCompare == 0) {
                                    // Same date string - use patient ID
                                    String id1 = p1.getPatientId() != null ? p1.getPatientId() : "";
                                    String id2 = p2.getPatientId() != null ? p2.getPatientId() : "";
                                    return id2.compareTo(id1);
                                }
                                return stringCompare;
                            }
                        }
                    });
                    
                    android.util.Log.d("DoctorDashboard", "Sorted " + patients.size() + " patients (SQLite). First patient date: " + 
                        (patients.size() > 0 && patients.get(0).getCreatedDate() != null ? patients.get(0).getCreatedDate() : "null"));
                    
                    // Convert patients to notifications
                    java.util.List<Notification> notifications = new java.util.ArrayList<>();
                    synchronized (knownPatientIds) {
                        knownPatientIds.clear();
                    }
                    
                    // Show ALL patients (not limited to 10)
                    int count = patients.size();
                    for (int i = 0; i < count; i++) {
                        com.healthcare.cas.models.Patient patient = patients.get(i);
                        if (patient.getPatientId() != null) {
                            synchronized (knownPatientIds) {
                                knownPatientIds.add(patient.getPatientId());
                            }
                        }
                        String patientName = patient.getFullName() != null && !patient.getFullName().isEmpty() 
                            ? patient.getFullName() 
                            : (patient.getFirstName() + " " + patient.getLastName()).trim();
                        
                        String message = "New patient registered: " + patientName;
                        String originalDate = patient.getCreatedDate();
                        String timestamp = originalDate != null ? 
                            formatTimestamp(originalDate) : "Recently";
                        
                        android.util.Log.d("DoctorDashboard", "Notification (SQLite) for " + patientName + ": original=" + originalDate + ", formatted=" + timestamp);
                        
                        Notification notif = new Notification(
                            patient.getPatientId(),
                            "Patient Registration",
                            message,
                            timestamp
                        );
                        // Read status will be restored by NotificationDropdownHelper from SharedPreferences
                        notifications.add(notif);
                    }
                    
                    // Update UI on main thread
                    com.healthcare.cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                        if (notificationDropdownHelper != null) {
                            // First, migrate all existing notifications to Firebase
                            notificationDropdownHelper.migrateNotificationsToFirebase(notifications);
                            
                            // Then set notifications (will load read status from Firebase)
                            notificationDropdownHelper.setNotifications(notifications);
                            // Badge should show unread count, not total count
                            updateNotificationBadge(notificationDropdownHelper.getUnreadNotificationCount());
                        } else {
                            updateNotificationBadge(notifications.size());
                        }
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
        
        try {
            // Try to parse the date string in common formats
            SimpleDateFormat[] inputFormats = {
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault()),
                new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            };
            
            Date date = null;
            for (SimpleDateFormat format : inputFormats) {
                try {
                    date = format.parse(dateString);
                    if (date != null) break;
                } catch (ParseException e) {
                    // Try next format
                }
            }
            
            if (date != null) {
                // Format for display: "yyyy-MM-dd HH:mm:ss"
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                return outputFormat.format(date);
            } else {
                // If parsing fails, return the original string
                return dateString;
            }
        } catch (Exception e) {
            android.util.Log.e("DoctorDashboard", "Error formatting timestamp: " + dateString, e);
            return dateString; // Return original if formatting fails
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
            
            String originalDate = patient.getCreatedDate();
            String timestamp = originalDate != null ? formatTimestamp(originalDate) : "Recently";
            
            android.util.Log.d("DoctorDashboard", "New patient notification: " + patientName + " - " + timestamp);
            
            Notification notification = new Notification(
                    patient.getPatientId(),
                    "Patient Registration",
                    "New patient registered: " + patientName,
                    timestamp
            );
            
            runOnUiThread(() -> {
                if (notificationDropdownHelper != null) {
                    notificationDropdownHelper.addNotification(notification);
                    // Badge should show unread count, not total count
                    updateNotificationBadge(notificationDropdownHelper.getUnreadNotificationCount());
                } else {
                    synchronized (knownPatientIds) {
                        updateNotificationBadge(knownPatientIds.size());
                    }
                }
            });
        });
    }
}
