package com.healthcare.cas.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.healthcare.cas.R;
import com.healthcare.cas.adapters.NotificationAdapter;
import com.healthcare.cas.database.HCasDatabaseHelper;
import com.healthcare.cas.database.FirebaseRTDBHelper;
import com.healthcare.cas.models.Notification;
import com.healthcare.cas.models.Patient;
import com.healthcare.cas.models.Prescription;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to manage notification dropdown popup
 */
public class NotificationDropdownHelper {

    private PopupWindow popupWindow;
    private View popupView;
    private RecyclerView recyclerView;
    private TextView notificationCount;
    private TextView notificationTitle;
    private View emptyStateLayout;
    private NotificationAdapter adapter;
    private List<Notification> notifications;
    private Context context;
    private NotificationCountListener countListener;
    private HCasDatabaseHelper databaseHelper;
    private FirebaseRTDBHelper firebaseRTDBHelper;
    private DatabaseReference notificationsRef;
    
    // Keep reference to click listener to reuse
    private NotificationAdapter.OnNotificationClickListener clickListener;
    
    
    // Firebase path for notifications
    private static final String FIREBASE_PATH_NOTIFICATIONS = "notifications";
    
    /**
     * Listener interface for notification count changes
     */
    public interface NotificationCountListener {
        void onNotificationCountChanged(int count);
    }

    public NotificationDropdownHelper(Context context) {
        this.context = context;
        this.notifications = new ArrayList<>();
        this.databaseHelper = new HCasDatabaseHelper(context);
        this.firebaseRTDBHelper = new FirebaseRTDBHelper(context);
        
        // Initialize Firebase reference for notifications
        if (firebaseRTDBHelper != null) {
            DatabaseReference rootRef = firebaseRTDBHelper.getRootRef();
            if (rootRef != null) {
                this.notificationsRef = rootRef.child(FIREBASE_PATH_NOTIFICATIONS);
                android.util.Log.d("NotificationDropdown", "=== INITIALIZATION ===");
                android.util.Log.d("NotificationDropdown", "Firebase notifications reference initialized: " + FIREBASE_PATH_NOTIFICATIONS);
            } else {
                android.util.Log.e("NotificationDropdown", "ERROR: Firebase rootRef is null");
            }
        }
        
        initializePopup();
    }

    /**
     * Initialize the popup window
     */
    private void initializePopup() {
        LayoutInflater inflater = LayoutInflater.from(context);
        popupView = inflater.inflate(R.layout.dropdown_notifications, null);

        // Initialize views
        recyclerView = popupView.findViewById(R.id.notificationsRecyclerView);
        notificationCount = popupView.findViewById(R.id.notificationCount);
        notificationTitle = popupView.findViewById(R.id.notificationTitle);
        emptyStateLayout = popupView.findViewById(R.id.emptyStateLayout);

        // Create click listener once and reuse
        clickListener = notification -> {
            // Handle notification click
            android.util.Log.d("NotificationDropdown", "Adapter click listener triggered for: " + notification.getId());
            onNotificationClick(notification);
        };
        
        // Setup RecyclerView with click listener
        adapter = new NotificationAdapter(notifications, clickListener);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
        
        android.util.Log.d("NotificationDropdown", "RecyclerView adapter initialized with click listener");

        // Create PopupWindow
        popupWindow = new PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        );

        // Configure popup window
        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(8f);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        // Dismiss when clicking outside
        popupView.setOnClickListener(v -> {
            // Prevent dismissing when clicking inside
        });
    }

    /**
     * Show the dropdown below the notification button, aligned to right side
     */
    public void show(View anchorView) {
        if (popupWindow == null || popupView == null) {
            initializePopup();
        }

        // Update notification count
        updateNotificationCount();

        // Measure popup view to get its width
        int widthSpec = View.MeasureSpec.makeMeasureSpec(
            (int) (320 * context.getResources().getDisplayMetrics().density), 
            View.MeasureSpec.EXACTLY
        );
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        popupView.measure(widthSpec, heightSpec);
        int popupWidth = popupView.getMeasuredWidth();

        // Calculate position (below the notification button, aligned to right side of screen)
        int[] location = new int[2];
        anchorView.getLocationInWindow(location);
        
        // Get screen width and convert dp to pixels
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int marginRight = (int) (16 * context.getResources().getDisplayMetrics().density); // 16dp margin
        
        // Calculate x position: align right edge of popup with right edge of screen (with margin)
        int x = screenWidth - popupWidth - marginRight;
        
        // Ensure popup doesn't go off screen (fallback)
        if (x < marginRight) {
            x = marginRight;
        }
        
        // Y position: below the notification button with small margin
        int marginTop = (int) (8 * context.getResources().getDisplayMetrics().density); // 8dp margin
        int y = location[1] + anchorView.getHeight() + marginTop;

        // Show popup
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);
    }

    /**
     * Dismiss the dropdown
     */
    public void dismiss() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    /**
     * Check if dropdown is showing
     */
    public boolean isShowing() {
        return popupWindow != null && popupWindow.isShowing();
    }

    /**
     * Toggle dropdown visibility
     */
    public void toggle(View anchorView) {
        if (isShowing()) {
            dismiss();
        } else {
            show(anchorView);
        }
    }

    /**
     * Update notification count badge
     */
    private void updateNotificationCount() {
        int unreadCount = getUnreadCount();
        if (unreadCount > 0) {
            notificationCount.setVisibility(View.VISIBLE);
            notificationCount.setText(String.valueOf(unreadCount));
        } else {
            notificationCount.setVisibility(View.GONE);
        }

        // Update empty state
        if (notifications.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Get unread notification count
     */
    private int getUnreadCount() {
        int count = 0;
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Set notifications list - loads read status from Firebase
     */
    public void setNotifications(List<Notification> notifications) {
        android.util.Log.d("NotificationDropdown", "Setting " + (notifications != null ? notifications.size() : 0) + " notifications");
        
        // Clear existing notifications first to avoid duplicates
        this.notifications.clear();
        
        if (notifications != null && !notifications.isEmpty()) {
            android.util.Log.d("NotificationDropdown", "Processing " + notifications.size() + " notifications from patient list");
            
            // First, migrate all notifications to Firebase (will create if not exists, preserve status if exists)
            migrateNotificationsToFirebase(notifications);
            
            // Then load read status from Firebase (only for these specific notifications)
            loadReadStatusFromFirebase(notifications);
        } else {
            // Update adapter even if empty
            if (adapter != null) {
                adapter.updateNotifications(this.notifications);
            }
            updateNotificationCount();
        }
    }
    
    /**
     * Save notifications to Firebase (create if not exists)
     */
    private void saveNotificationsToFirebase(List<Notification> notifications) {
        if (notificationsRef == null) {
            android.util.Log.e("NotificationDropdown", "Cannot save to Firebase - notificationsRef is null");
            return;
        }
        
        android.util.Log.d("NotificationDropdown", "Saving " + notifications.size() + " notifications to Firebase");
        
        for (Notification notification : notifications) {
            if (notification.getId() == null || notification.getId().isEmpty()) {
                continue;
            }
            
            // Check if notification already exists in Firebase
            notificationsRef.child(notification.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        // Notification doesn't exist, create it with status "unread"
                        Map<String, Object> notificationData = new HashMap<>();
                        notificationData.put("id", notification.getId());
                        notificationData.put("sender", notification.getSender());
                        notificationData.put("message", notification.getMessage());
                        notificationData.put("timestamp", notification.getTimestamp());
                        notificationData.put("status", "unread"); // Default to unread
                        
                        notificationsRef.child(notification.getId()).setValue(notificationData)
                            .addOnSuccessListener(aVoid -> {
                                android.util.Log.d("NotificationDropdown", "✓ Saved notification to Firebase: " + notification.getId());
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("NotificationDropdown", "Failed to save notification to Firebase: " + notification.getId(), e);
                            });
                    } else {
                        android.util.Log.d("NotificationDropdown", "Notification already exists in Firebase: " + notification.getId());
                    }
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    android.util.Log.e("NotificationDropdown", "Error checking notification in Firebase: " + notification.getId(), error.toException());
                }
            });
        }
    }
    
    /**
     * Load read status from Firebase for all notifications
     */
    private void loadReadStatusFromFirebase(List<Notification> notifications) {
        if (notificationsRef == null) {
            android.util.Log.e("NotificationDropdown", "Cannot load from Firebase - notificationsRef is null");
            // Fallback: set all as unread
            for (Notification notification : notifications) {
                notification.setRead(false);
                this.notifications.add(notification);
            }
            updateAdapterAndCount();
            return;
        }
        
        android.util.Log.d("NotificationDropdown", "=== Loading read status from Firebase ===");
        
        // Create a set of notification IDs we're looking for (to avoid loading all notifications)
        java.util.Set<String> notificationIds = new java.util.HashSet<>();
        for (Notification notification : notifications) {
            if (notification.getId() != null && !notification.getId().isEmpty()) {
                notificationIds.add(notification.getId());
            }
        }
        
        android.util.Log.d("NotificationDropdown", "Loading status for " + notificationIds.size() + " specific notifications from Firebase");
        
        // Load only the notifications we need from Firebase
        notificationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, String> statusMap = new HashMap<>();
                
                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String notificationId = child.getKey();
                        // Only process notifications that are in our current list
                        if (notificationIds.contains(notificationId)) {
                            Map<String, Object> data = (Map<String, Object>) child.getValue();
                            if (data != null) {
                                String status = data.get("status") != null ? data.get("status").toString() : "unread";
                                statusMap.put(notificationId, status);
                                android.util.Log.d("NotificationDropdown", "Loaded status for " + notificationId + ": " + status);
                            }
                        }
                    }
                }
                
                // Clear existing notifications first to avoid duplicates
                NotificationDropdownHelper.this.notifications.clear();
                
                // Apply read status to notifications
                int restoredCount = 0;
                int nullIdCount = 0;
                
                for (Notification notification : notifications) {
                    if (notification.getId() != null && !notification.getId().isEmpty()) {
                        String status = statusMap.get(notification.getId());
                        boolean isRead = "read".equals(status);
                        notification.setRead(isRead);
                        
                        if (isRead) {
                            restoredCount++;
                            android.util.Log.d("NotificationDropdown", "✓ Restored read status for: " + notification.getId());
                        } else {
                            android.util.Log.d("NotificationDropdown", "✗ Notification " + notification.getId() + " is unread");
                        }
                    } else {
                        nullIdCount++;
                        android.util.Log.w("NotificationDropdown", "⚠ Notification has null/empty ID");
                        notification.setRead(false);
                    }
                    NotificationDropdownHelper.this.notifications.add(notification);
                }
                
                android.util.Log.d("NotificationDropdown", "=== RESTORATION SUMMARY ===");
                android.util.Log.d("NotificationDropdown", "Total notifications: " + notifications.size());
                android.util.Log.d("NotificationDropdown", "Restored as READ: " + restoredCount);
                android.util.Log.d("NotificationDropdown", "Remaining UNREAD: " + (notifications.size() - restoredCount - nullIdCount));
                
                // Sort notifications: unread first (latest first), then read (latest first)
                sortNotifications();
                
                // Update UI on main thread
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        updateAdapterAndCount();
                    });
                } else {
                    updateAdapterAndCount();
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                android.util.Log.e("NotificationDropdown", "Error loading read status from Firebase", error.toException());
                // Fallback: set all as unread and clear existing to avoid duplicates
                NotificationDropdownHelper.this.notifications.clear();
                for (Notification notification : notifications) {
                    notification.setRead(false);
                    NotificationDropdownHelper.this.notifications.add(notification);
                }
                // Sort notifications: unread first (latest first), then read (latest first)
                sortNotifications();
                updateAdapterAndCount();
            }
        });
    }
    
    /**
     * Update adapter and notification count
     */
    private void updateAdapterAndCount() {
        // Sort notifications before updating adapter
        sortNotifications();
        
        // Make sure adapter exists and has click listener
        if (adapter == null) {
            android.util.Log.d("NotificationDropdown", "Adapter is null, recreating with click listener");
            if (clickListener == null) {
                clickListener = notification -> {
                    android.util.Log.d("NotificationDropdown", "Adapter click listener triggered for: " + notification.getId());
                    onNotificationClick(notification);
                };
            }
            adapter = new NotificationAdapter(this.notifications, clickListener);
            if (recyclerView != null) {
                recyclerView.setAdapter(adapter);
            }
        } else {
            // Update the adapter's notification list
            adapter.updateNotifications(this.notifications);
        }
        
        updateNotificationCount();
        
        // Notify listener of count change - use UNREAD count, not total count
        if (countListener != null) {
            int unreadCount = getUnreadCount();
            countListener.onNotificationCountChanged(unreadCount);
            android.util.Log.d("NotificationDropdown", "Notifying count listener: unread=" + unreadCount + ", total=" + (notifications != null ? notifications.size() : 0));
        }
    }

    /**
     * Add a notification
     */
    public void addNotification(Notification notification) {
        // Save notification to Firebase with status "unread"
        if (notification.getId() != null && !notification.getId().isEmpty()) {
            saveNotificationToFirebase(notification);
        }
        
        // Add notification and sort (unread first, then read - both by latest first)
        notifications.add(notification);
        sortNotifications();
        
        if (adapter != null) {
            adapter.updateNotifications(this.notifications);
        }
        updateNotificationCount();
        
        // Notify listener of count change - use UNREAD count, not total count
        if (countListener != null) {
            int unreadCount = getUnreadCount();
            countListener.onNotificationCountChanged(unreadCount);
            android.util.Log.d("NotificationDropdown", "Notifying count listener after add: unread=" + unreadCount + ", total=" + notifications.size());
        }
    }
    
    /**
     * Save a single notification to Firebase
     */
    private void saveNotificationToFirebase(Notification notification) {
        if (notificationsRef == null || notification.getId() == null) {
            return;
        }
        
        // Check if notification already exists
        notificationsRef.child(notification.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Notification doesn't exist, create it
                    Map<String, Object> notificationData = new HashMap<>();
                    notificationData.put("id", notification.getId());
                    notificationData.put("sender", notification.getSender());
                    notificationData.put("message", notification.getMessage());
                    notificationData.put("timestamp", notification.getTimestamp());
                    notificationData.put("status", notification.isRead() ? "read" : "unread");
                    
                    notificationsRef.child(notification.getId()).setValue(notificationData)
                        .addOnSuccessListener(aVoid -> {
                            android.util.Log.d("NotificationDropdown", "✓ Saved new notification to Firebase: " + notification.getId());
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("NotificationDropdown", "Failed to save notification to Firebase: " + notification.getId(), e);
                        });
                } else {
                    android.util.Log.d("NotificationDropdown", "Notification already exists in Firebase: " + notification.getId());
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                android.util.Log.e("NotificationDropdown", "Error checking notification in Firebase: " + notification.getId(), error.toException());
            }
        });
    }

    /**
     * Handle notification click
     */
    private void onNotificationClick(Notification notification) {
        android.util.Log.d("NotificationDropdown", "Notification clicked: " + notification.getId() + ", sender: " + notification.getSender());
        
        // Mark as read
        notification.setRead(true);
        saveReadStatusToFirebase(notification.getId(), "read");
        
        // Sort notifications: unread first (latest first), then read (latest first)
        // This will move the read notification to the bottom
        sortNotifications();
        
        if (adapter != null) {
            // Update the adapter's list to ensure it sees the change
            adapter.updateNotifications(this.notifications);
            android.util.Log.d("NotificationDropdown", "Adapter updated after notification click");
        } else {
            android.util.Log.e("NotificationDropdown", "Adapter is NULL when trying to mark notification as read!");
        }
        
        updateNotificationCount();
        
        // Notify listener of count change - use UNREAD count, not total count
        if (countListener != null) {
            int unreadCount = getUnreadCount();
            countListener.onNotificationCountChanged(unreadCount);
            android.util.Log.d("NotificationDropdown", "Notifying count listener after click: unread=" + unreadCount);
        }

        // Dismiss popup
        dismiss();
        
        // Handle notification action based on sender type
        if (notification.getId() != null && notification.getSender() != null) {
            if (notification.getSender().equals("Patient Registration")) {
                // The notification ID is the patient ID
                android.util.Log.d("NotificationDropdown", "Loading patient details for ID: " + notification.getId());
                loadAndShowPatientDetails(notification.getId());
            } else if (notification.getSender().equals("New Prescription")) {
                // The notification ID is the prescription ID
                android.util.Log.d("NotificationDropdown", "Loading prescription details for ID: " + notification.getId());
                loadAndShowPrescriptionDetails(notification.getId());
            } else {
                android.util.Log.d("NotificationDropdown", "Notification not handled - ID: " + notification.getId() + ", Sender: " + notification.getSender());
            }
        } else {
            android.util.Log.d("NotificationDropdown", "Notification not handled - ID: " + notification.getId() + ", Sender: " + notification.getSender());
        }
    }
    
    /**
     * Load patient details and show dialog
     */
    private void loadAndShowPatientDetails(String patientId) {
        // Try Firebase first, then SQLite
        if (firebaseRTDBHelper != null) {
            firebaseRTDBHelper.getPatientById(patientId, patient -> {
                if (patient != null) {
                    // Check if patient already has prescription (status = "off")
                    // If so, mark notification as read
                    String patientStatus = patient.getPatientStatus();
                    if (patientStatus == null || patientStatus.isEmpty()) {
                        patientStatus = "on"; // Default to "on" if not set
                    }
                    
                    if ("off".equals(patientStatus)) {
                        // Patient already has prescription - mark notification as read
                        markNotificationAsRead(patientId);
                    }
                    
                    showPatientDetailsDialog(patient);
                } else {
                    // Fallback to SQLite
                    loadPatientFromSQLite(patientId);
                }
            });
        } else {
            loadPatientFromSQLite(patientId);
        }
    }
    
    /**
     * Load patient from SQLite database
     */
    private void loadPatientFromSQLite(String patientId) {
        if (databaseHelper != null) {
            Patient patient = databaseHelper.getPatientById(patientId);
            if (patient != null) {
                showPatientDetailsDialog(patient);
            }
        }
    }
    
    /**
     * Show patient details dialog
     */
    private void showPatientDetailsDialog(Patient patient) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        
        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_patient_details, null);
        builder.setView(dialogView);
        
        // Set patient data
        TextView dialogPatientId = dialogView.findViewById(R.id.dialogPatientId);
        TextView dialogPatientName = dialogView.findViewById(R.id.dialogPatientName);
        TextView dialogPatientAge = dialogView.findViewById(R.id.dialogPatientAge);
        TextView dialogPatientGender = dialogView.findViewById(R.id.dialogPatientGender);
        TextView dialogPatientPhone = dialogView.findViewById(R.id.dialogPatientPhone);
        TextView dialogPatientEmail = dialogView.findViewById(R.id.dialogPatientEmail);
        TextView dialogPatientAddress = dialogView.findViewById(R.id.dialogPatientAddress);
        TextView dialogPatientBirthPlace = dialogView.findViewById(R.id.dialogPatientBirthPlace);
        TextView dialogPatientAllergies = dialogView.findViewById(R.id.dialogPatientAllergies);
        TextView dialogPatientMedications = dialogView.findViewById(R.id.dialogPatientMedications);
        TextView dialogPatientMedicalHistory = dialogView.findViewById(R.id.dialogPatientMedicalHistory);
        TextView dialogPatientVitals = dialogView.findViewById(R.id.dialogPatientVitals);
        TextView dialogPatientSymptoms = dialogView.findViewById(R.id.dialogPatientSymptoms);
        
        // Populate patient data
        if (dialogPatientId != null) {
            dialogPatientId.setText(patient.getPatientId() != null ? patient.getPatientId() : "N/A");
        }
        
        String fullName = "";
        if (patient.getFullName() != null && !patient.getFullName().isEmpty()) {
            fullName = patient.getFullName();
        } else {
            fullName = (patient.getFirstName() != null ? patient.getFirstName() : "") + " " + 
                      (patient.getLastName() != null ? patient.getLastName() : "");
            if (patient.getSuffix() != null && !patient.getSuffix().isEmpty()) {
                fullName += " " + patient.getSuffix();
            }
        }
        fullName = fullName.trim();
        if (dialogPatientName != null) {
            dialogPatientName.setText(fullName.isEmpty() ? "N/A" : fullName);
        }
        
        if (dialogPatientAge != null) {
            dialogPatientAge.setText(patient.getAge() != null ? patient.getAge() : "N/A");
        }
        if (dialogPatientGender != null) {
            dialogPatientGender.setText(patient.getGender() != null ? patient.getGender() : "N/A");
        }
        if (dialogPatientPhone != null) {
            dialogPatientPhone.setText(patient.getPhoneNumber() != null ? patient.getPhoneNumber() : "N/A");
        }
        if (dialogPatientEmail != null) {
            dialogPatientEmail.setText(patient.getEmail() != null ? patient.getEmail() : "N/A");
        }
        if (dialogPatientAddress != null) {
            dialogPatientAddress.setText(patient.getFullAddress() != null ? patient.getFullAddress() : "N/A");
        }
        if (dialogPatientBirthPlace != null) {
            dialogPatientBirthPlace.setText(patient.getBirthPlace() != null ? patient.getBirthPlace() : "N/A");
        }
        if (dialogPatientAllergies != null) {
            dialogPatientAllergies.setText(patient.getAllergies() != null && !patient.getAllergies().isEmpty() ? patient.getAllergies() : "None");
        }
        if (dialogPatientMedications != null) {
            dialogPatientMedications.setText(patient.getMedications() != null && !patient.getMedications().isEmpty() ? patient.getMedications() : "None");
        }
        if (dialogPatientMedicalHistory != null) {
            dialogPatientMedicalHistory.setText(patient.getMedicalHistory() != null && !patient.getMedicalHistory().isEmpty() ? patient.getMedicalHistory() : "None");
        }
        
        // Vital Signs
        if (dialogPatientVitals != null) {
            String vitals = "";
            if (patient.getPulseRate() != null && !patient.getPulseRate().isEmpty()) {
                vitals += "Pulse: " + patient.getPulseRate() + " | ";
            }
            if (patient.getBloodPressure() != null && !patient.getBloodPressure().isEmpty()) {
                vitals += "BP: " + patient.getBloodPressure() + " | ";
            }
            if (patient.getTemperature() != null && !patient.getTemperature().isEmpty()) {
                vitals += "Temp: " + patient.getTemperature() + "°C | ";
            }
            if (patient.getBloodSugar() != null && !patient.getBloodSugar().isEmpty()) {
                vitals += "Sugar: " + patient.getBloodSugar() + " | ";
            }
            if (patient.getPainScale() != null && !patient.getPainScale().isEmpty()) {
                vitals += "Pain: " + patient.getPainScale() + "/10";
            }
            
            if (vitals.endsWith(" | ")) {
                vitals = vitals.substring(0, vitals.length() - 3);
            }
            
            dialogPatientVitals.setText(vitals.isEmpty() ? "No vital signs recorded" : vitals);
        }
        
        if (dialogPatientSymptoms != null) {
            dialogPatientSymptoms.setText(patient.getSymptomsDescription() != null && !patient.getSymptomsDescription().isEmpty() 
                ? patient.getSymptomsDescription() : "No symptoms recorded");
        }
        
        // Create dialog
        AlertDialog dialog = builder.create();
        
        // Set up button click listeners
        MaterialButton createPrescriptionButton = dialogView.findViewById(R.id.dialogCreatePrescriptionButton);
        MaterialButton createDiagnosisButton = dialogView.findViewById(R.id.dialogCreateDiagnosisButton);
        ImageButton headerCloseButton = dialogView.findViewById(R.id.dialogHeaderCloseButton);
        
        // Store patient ID and notification ID for later use
        final String patientId = patient.getPatientId();
        final String notificationId = patientId; // Notification ID is the patient ID
        
        if (createPrescriptionButton != null) {
            createPrescriptionButton.setOnClickListener(v -> {
                dialog.dismiss();
                navigateToCreatePrescription(patient, notificationId);
            });
        }
        
        if (createDiagnosisButton != null) {
            createDiagnosisButton.setOnClickListener(v -> {
                dialog.dismiss();
                navigateToCreateDiagnosis(patient, notificationId);
            });
        }
        
        if (headerCloseButton != null) {
            headerCloseButton.setOnClickListener(v -> dialog.dismiss());
        }
        
        dialog.show();
    }
    
    /**
     * Navigate to Create Prescription page with validation
     */
    private void navigateToCreatePrescription(Patient patient, String notificationId) {
        if (patient == null || patient.getPatientId() == null) {
            android.widget.Toast.makeText(context, "❌ Patient information not available", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check patient status from Firebase
        if (firebaseRTDBHelper != null) {
            firebaseRTDBHelper.getPatientById(patient.getPatientId(), firebasePatient -> {
                if (firebasePatient != null) {
                    String patientStatus = firebasePatient.getPatientStatus();
                    if (patientStatus == null || patientStatus.isEmpty()) {
                        patientStatus = "on"; // Default to "on" if not set
                    }
                    
                    if ("off".equals(patientStatus)) {
                        // Patient already has prescription - show error
                        android.widget.Toast.makeText(context, 
                            "❌ This patient already has a prescription.\nThe patient is no longer in the registered patients list.", 
                            android.widget.Toast.LENGTH_LONG).show();
                        
                        // Mark notification as read if still unread
                        markNotificationAsRead(notificationId);
                        return;
                    }
                }
                
                // Patient status is "on" - proceed with navigation
                try {
                    if (context instanceof com.healthcare.cas.DoctorDashboardActivity) {
                        com.healthcare.cas.DoctorDashboardActivity activity = (com.healthcare.cas.DoctorDashboardActivity) context;
                        com.healthcare.cas.CreatePrescriptionFragment prescriptionFragment = new com.healthcare.cas.CreatePrescriptionFragment();
                        
                        // Pass patient data to prescription fragment
                        android.os.Bundle args = new android.os.Bundle();
                        args.putString("PATIENT_ID", patient.getPatientId());
                        String patientName = patient.getFullName();
                        if (patientName == null || patientName.isEmpty()) {
                            patientName = (patient.getFirstName() != null ? patient.getFirstName() : "") + " " + 
                                         (patient.getLastName() != null ? patient.getLastName() : "");
                        }
                        args.putString("PATIENT_NAME", patientName.trim());
                        prescriptionFragment.setArguments(args);
                        
                        activity.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragmentContainer, prescriptionFragment)
                                .addToBackStack(null)
                                .commit();
                        if (activity.getSupportActionBar() != null) {
                            activity.getSupportActionBar().setTitle("Create Prescription");
                        }
                    } else {
                        android.widget.Toast.makeText(context, "❌ Cannot navigate - invalid activity context", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    android.util.Log.e("NotificationDropdown", "❌ Error navigating to prescription: " + e.getMessage(), e);
                    android.widget.Toast.makeText(context, "❌ Error: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Fallback: try navigation anyway if Firebase helper is not available
            android.widget.Toast.makeText(context, "⚠️ Cannot verify patient status. Please try again.", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Navigate to Create Diagnosis page with validation
     */
    private void navigateToCreateDiagnosis(Patient patient, String notificationId) {
        if (patient == null || patient.getPatientId() == null) {
            android.widget.Toast.makeText(context, "❌ Patient information not available", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check patient status from Firebase
        if (firebaseRTDBHelper != null) {
            firebaseRTDBHelper.getPatientById(patient.getPatientId(), firebasePatient -> {
                if (firebasePatient != null) {
                    String patientStatus = firebasePatient.getPatientStatus();
                    if (patientStatus == null || patientStatus.isEmpty()) {
                        patientStatus = "on"; // Default to "on" if not set
                    }
                    
                    if ("off".equals(patientStatus)) {
                        // Patient already has prescription - show error
                        android.widget.Toast.makeText(context, 
                            "❌ This patient already has a prescription.\nThe patient is no longer in the registered patients list.", 
                            android.widget.Toast.LENGTH_LONG).show();
                        
                        // Mark notification as read if still unread
                        markNotificationAsRead(notificationId);
                        return;
                    }
                }
                
                // Patient status is "on" - proceed with navigation
                try {
                    if (context instanceof com.healthcare.cas.DoctorDashboardActivity) {
                        com.healthcare.cas.DoctorDashboardActivity activity = (com.healthcare.cas.DoctorDashboardActivity) context;
                        com.healthcare.cas.CreateDiagnosisFragment diagnosisFragment = new com.healthcare.cas.CreateDiagnosisFragment();
                        
                        // Pass patient data to diagnosis fragment
                        android.os.Bundle args = new android.os.Bundle();
                        args.putString("PATIENT_ID", patient.getPatientId());
                        String patientName = patient.getFullName();
                        if (patientName == null || patientName.isEmpty()) {
                            patientName = (patient.getFirstName() != null ? patient.getFirstName() : "") + " " + 
                                         (patient.getLastName() != null ? patient.getLastName() : "");
                        }
                        args.putString("PATIENT_NAME", patientName.trim());
                        diagnosisFragment.setArguments(args);
                        
                        activity.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragmentContainer, diagnosisFragment)
                                .addToBackStack(null)
                                .commit();
                        if (activity.getSupportActionBar() != null) {
                            activity.getSupportActionBar().setTitle("Create Diagnosis");
                        }
                    } else {
                        android.widget.Toast.makeText(context, "❌ Cannot navigate - invalid activity context", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    android.util.Log.e("NotificationDropdown", "❌ Error navigating to diagnosis: " + e.getMessage(), e);
                    android.widget.Toast.makeText(context, "❌ Error: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Fallback: try navigation anyway if Firebase helper is not available
            android.widget.Toast.makeText(context, "⚠️ Cannot verify patient status. Please try again.", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Mark notification as read
     */
    private void markNotificationAsRead(String notificationId) {
        if (notificationId == null || notificationId.isEmpty() || firebaseRTDBHelper == null) {
            return;
        }
        
        try {
            DatabaseReference notificationRef = firebaseRTDBHelper.getRootRef().child(FIREBASE_PATH_NOTIFICATIONS).child(notificationId);
            if (notificationRef != null) {
                notificationRef.child("read").setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("NotificationDropdown", "✅ Marked notification as read: " + notificationId);
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("NotificationDropdown", "❌ Failed to mark notification as read: " + e.getMessage(), e);
                    });
            }
        } catch (Exception e) {
            android.util.Log.e("NotificationDropdown", "❌ Error marking notification as read: " + e.getMessage(), e);
        }
    }
    
    /**
     * Set nurse mode (deprecated - buttons removed)
     * Kept for backward compatibility but does nothing
     */
    @Deprecated
    public void setNurseMode(boolean nurseMode) {
        // Buttons removed - method kept for backward compatibility
    }
    
    /**
     * Load prescription details and show dialog
     */
    private void loadAndShowPrescriptionDetails(String prescriptionId) {
        // Try Firebase first, then SQLite
        if (firebaseRTDBHelper != null) {
            firebaseRTDBHelper.getAllPrescriptionsFromHistory(prescriptions -> {
                Prescription foundPrescription = null;
                for (Prescription prescription : prescriptions) {
                    if (prescription.getPrescriptionId() != null && prescription.getPrescriptionId().equals(prescriptionId)) {
                        foundPrescription = prescription;
                        break;
                    }
                }
                if (foundPrescription != null) {
                    showPrescriptionDetailsDialog(foundPrescription);
                } else {
                    // Fallback to SQLite
                    loadPrescriptionFromSQLite(prescriptionId);
                }
            });
        } else {
            loadPrescriptionFromSQLite(prescriptionId);
        }
    }
    
    /**
     * Load prescription from SQLite database
     */
    private void loadPrescriptionFromSQLite(String prescriptionId) {
        if (databaseHelper != null) {
            Prescription prescription = databaseHelper.getPrescriptionById(prescriptionId);
            if (prescription != null) {
                showPrescriptionDetailsDialog(prescription);
            } else {
                android.util.Log.w("NotificationDropdown", "Prescription not found: " + prescriptionId);
            }
        }
    }
    
    /**
     * Show prescription details dialog
     */
    private void showPrescriptionDetailsDialog(Prescription prescription) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Prescription Details");
        
        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_prescription_details, null);
        builder.setView(dialogView);
        
        // Set prescription information
        TextView dialogPrescriptionId = dialogView.findViewById(R.id.dialogPrescriptionId);
        TextView dialogPatientName = dialogView.findViewById(R.id.dialogPatientName);
        TextView dialogMedication = dialogView.findViewById(R.id.dialogMedication);
        TextView dialogDosage = dialogView.findViewById(R.id.dialogDosage);
        TextView dialogFrequency = dialogView.findViewById(R.id.dialogFrequency);
        TextView dialogDuration = dialogView.findViewById(R.id.dialogDuration);
        TextView dialogDoctor = dialogView.findViewById(R.id.dialogDoctor);
        TextView dialogDate = dialogView.findViewById(R.id.dialogDate);
        TextView dialogInstructions = dialogView.findViewById(R.id.dialogInstructions);
        
        // Populate prescription data
        if (dialogPrescriptionId != null) {
            dialogPrescriptionId.setText("Prescription ID: " + (prescription.getPrescriptionId() != null ? prescription.getPrescriptionId() : "N/A"));
        }
        if (dialogPatientName != null) {
            dialogPatientName.setText("Patient: " + (prescription.getPatientName() != null ? prescription.getPatientName() : "N/A"));
        }
        if (dialogMedication != null) {
            dialogMedication.setText("Medication: " + (prescription.getMedication() != null ? prescription.getMedication() : "N/A"));
        }
        if (dialogDosage != null) {
            dialogDosage.setText("Dosage: " + (prescription.getDosage() != null && !prescription.getDosage().isEmpty() ? prescription.getDosage() : "N/A"));
        }
        if (dialogFrequency != null) {
            dialogFrequency.setText("Frequency: " + (prescription.getFrequency() != null ? prescription.getFrequency() : "N/A"));
        }
        if (dialogDuration != null) {
            dialogDuration.setText("Duration: " + (prescription.getDuration() != null ? prescription.getDuration() : "N/A"));
        }
        if (dialogDoctor != null) {
            dialogDoctor.setText("Doctor: " + (prescription.getDoctorName() != null ? prescription.getDoctorName() : "N/A"));
        }
        if (dialogDate != null) {
            dialogDate.setText("Date: " + (prescription.getCreatedDate() != null ? prescription.getCreatedDate() : "N/A"));
        }
        if (dialogInstructions != null) {
            dialogInstructions.setText("Instructions: " + (prescription.getInstructions() != null && !prescription.getInstructions().isEmpty() ? prescription.getInstructions() : "None"));
        }
        
        // Set up close button
        ImageButton closeButton = dialogView.findViewById(R.id.closePrescriptionButton);
        MaterialButton rfidButton = dialogView.findViewById(R.id.rfidRegistrationButton);
        
        // Hide RFID button for nurse (not needed)
        if (rfidButton != null) {
            rfidButton.setVisibility(View.GONE);
        }
        
        AlertDialog dialog = builder.create();
        
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dialog.dismiss());
        }
        
        dialog.show();
    }
    
    /**
     * Set notification count listener
     */
    public void setNotificationCountListener(NotificationCountListener listener) {
        this.countListener = listener;
    }
    
    /**
     * Get total notification count
     */
    public int getTotalNotificationCount() {
        return notifications != null ? notifications.size() : 0;
    }
    
    /**
     * Get unread notification count
     */
    public int getUnreadNotificationCount() {
        return getUnreadCount();
    }

    /**
     * Mark all notifications as read
     */
    private void markAllAsRead() {
        android.util.Log.d("NotificationDropdown", "Marking all notifications as read");
        int markedCount = 0;
        List<String> notificationIds = new ArrayList<>();
        
        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            if (!notification.isRead()) {
                notification.setRead(true);
                if (notification.getId() != null) {
                    notificationIds.add(notification.getId());
                }
                markedCount++;
                android.util.Log.d("NotificationDropdown", "Marked notification at position " + i + " (ID: " + notification.getId() + ") as read");
            }
        }
        
        // Save all read notification IDs to Firebase
        if (!notificationIds.isEmpty()) {
            android.util.Log.d("NotificationDropdown", "=== MARK ALL AS READ - SAVING TO FIREBASE ===");
            android.util.Log.d("NotificationDropdown", "Marking " + notificationIds.size() + " notifications as read in Firebase");
            
            for (String notificationId : notificationIds) {
                saveReadStatusToFirebase(notificationId, "read");
            }
        }
        
        // Update adapter - refresh all items to ensure UI updates
        if (adapter != null) {
            // Update the adapter's list reference to ensure it sees the changes
            adapter.updateNotifications(this.notifications);
            android.util.Log.d("NotificationDropdown", "Adapter updated, notifying data set changed");
        } else {
            android.util.Log.e("NotificationDropdown", "Adapter is NULL when trying to mark all as read!");
        }
        
        updateNotificationCount();
        
        // Notify listener of count change
        if (countListener != null) {
            countListener.onNotificationCountChanged(getUnreadCount());
        }
        
        android.util.Log.d("NotificationDropdown", "Marked " + markedCount + " notifications as read. Unread count: " + getUnreadCount());
    }
    
    /**
     * Save read status for a notification to Firebase
     */
    private void saveReadStatusToFirebase(String notificationId, String status) {
        if (notificationId == null || notificationId.isEmpty()) {
            android.util.Log.w("NotificationDropdown", "Cannot save read status - notification ID is null or empty");
            return;
        }
        
        if (notificationsRef == null) {
            android.util.Log.e("NotificationDropdown", "Cannot save to Firebase - notificationsRef is null");
            return;
        }
        
        android.util.Log.d("NotificationDropdown", "Saving read status to Firebase: " + notificationId + " = " + status);
        
        // Update the status field in Firebase
        notificationsRef.child(notificationId).child("status").setValue(status)
            .addOnSuccessListener(aVoid -> {
                android.util.Log.d("NotificationDropdown", "✓ Successfully saved status to Firebase: " + notificationId + " = " + status);
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("NotificationDropdown", "ERROR: Failed to save status to Firebase: " + notificationId, e);
            });
    }
    
    /**
     * Handle "See All" button click
     */
    private void onSeeAllClick() {
        // TODO: Navigate to full notifications screen
        // For example: startActivity(new Intent(context, NotificationsActivity.class));
    }
    
    /**
     * Migrate all existing notifications to Firebase
     * This stores all current notifications to Firebase, preserving existing status if already exists
     */
    public void migrateNotificationsToFirebase(List<Notification> notifications) {
        if (notificationsRef == null) {
            android.util.Log.e("NotificationDropdown", "Cannot migrate to Firebase - notificationsRef is null");
            return;
        }
        
        if (notifications == null || notifications.isEmpty()) {
            android.util.Log.w("NotificationDropdown", "No notifications to migrate");
            return;
        }
        
        android.util.Log.d("NotificationDropdown", "=== MIGRATING " + notifications.size() + " NOTIFICATIONS TO FIREBASE ===");
        
        for (Notification notification : notifications) {
            if (notification.getId() == null || notification.getId().isEmpty()) {
                android.util.Log.w("NotificationDropdown", "Skipping notification with null/empty ID");
                continue;
            }
            
            // Store notification data in final variables for use in callback
            final String notificationId = notification.getId();
            final String sender = notification.getSender() != null ? notification.getSender() : "Patient Registration";
            final String message = notification.getMessage() != null ? notification.getMessage() : "";
            final String timestamp = notification.getTimestamp() != null ? notification.getTimestamp() : "";
            
            // Check if notification already exists in Firebase
            notificationsRef.child(notificationId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Map<String, Object> notificationData = new HashMap<>();
                    notificationData.put("id", notificationId);
                    notificationData.put("sender", sender);
                    notificationData.put("message", message);
                    notificationData.put("timestamp", timestamp);
                    
                    if (snapshot.exists()) {
                        // Notification exists - preserve existing status, only update other fields
                        Map<String, Object> existingData = (Map<String, Object>) snapshot.getValue();
                        if (existingData != null && existingData.containsKey("status")) {
                            notificationData.put("status", existingData.get("status"));
                            android.util.Log.d("NotificationDropdown", "Updated notification in Firebase (preserved status): " + notificationId);
                        } else {
                            // No status field, set to unread
                            notificationData.put("status", "unread");
                            android.util.Log.d("NotificationDropdown", "Updated notification in Firebase (added status): " + notificationId);
                        }
                    } else {
                        // Notification doesn't exist - create with status "unread" (all current notifications are unread)
                        notificationData.put("status", "unread");
                        android.util.Log.d("NotificationDropdown", "Created new notification in Firebase: " + notificationId);
                    }
                    
                    // Save to Firebase
                    notificationsRef.child(notificationId).setValue(notificationData)
                        .addOnSuccessListener(aVoid -> {
                            android.util.Log.d("NotificationDropdown", "✓ Successfully saved notification to Firebase: " + notificationId + " (status: " + notificationData.get("status") + ")");
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("NotificationDropdown", "Failed to save notification to Firebase: " + notificationId, e);
                        });
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    android.util.Log.e("NotificationDropdown", "Error checking notification in Firebase: " + notificationId, error.toException());
                }
            });
        }
        
        android.util.Log.d("NotificationDropdown", "=== MIGRATION INITIATED ===");
        android.util.Log.d("NotificationDropdown", "Total notifications to migrate: " + notifications.size());
        android.util.Log.d("NotificationDropdown", "Migration in progress... (check Firebase Console for results)");
    }

    /**
     * Sort notifications: unread first (sorted by timestamp descending - latest first),
     * then read notifications (sorted by timestamp descending - latest first)
     */
    private void sortNotifications() {
        if (this.notifications == null || this.notifications.isEmpty()) {
            return;
        }
        
        java.util.Collections.sort(this.notifications, new java.util.Comparator<Notification>() {
            @Override
            public int compare(Notification n1, Notification n2) {
                // First, sort by read status: unread (false) comes before read (true)
                boolean read1 = n1.isRead();
                boolean read2 = n2.isRead();
                
                if (read1 != read2) {
                    // Unread (false) should come first, so return -1 if n1 is unread
                    return read1 ? 1 : -1;
                }
                
                // Both have same read status, sort by timestamp (latest first)
                String timestamp1 = n1.getTimestamp();
                String timestamp2 = n2.getTimestamp();
                
                if (timestamp1 == null && timestamp2 == null) {
                    return 0;
                }
                if (timestamp1 == null) return 1; // null timestamps go to end
                if (timestamp2 == null) return -1;
                
                // Try to parse timestamps and compare
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                    java.util.Date date1 = sdf.parse(timestamp1);
                    java.util.Date date2 = sdf.parse(timestamp2);
                    
                    if (date1 != null && date2 != null) {
                        // Latest first (descending order)
                        return date2.compareTo(date1);
                    }
                } catch (Exception e) {
                    // If parsing fails, compare as strings (descending)
                    android.util.Log.d("NotificationDropdown", "Could not parse timestamp, comparing as strings: " + timestamp1 + " vs " + timestamp2);
                }
                
                // Fallback: compare as strings (descending - latest first)
                return timestamp2.compareTo(timestamp1);
            }
        });
        
        android.util.Log.d("NotificationDropdown", "Sorted " + this.notifications.size() + " notifications: unread first, then read (both by latest first)");
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        dismiss();
        popupWindow = null;
        popupView = null;
        recyclerView = null;
        adapter = null;
        notifications = null;
    }
}

