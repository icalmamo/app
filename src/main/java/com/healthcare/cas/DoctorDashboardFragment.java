package com.healthcare.cas;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;

import com.healthcare.cas.database.HCasDatabaseHelper;
import com.healthcare.cas.database.FirebaseRTDBHelper;
import com.healthcare.cas.utils.DevicePerformanceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DoctorDashboardFragment displays the main dashboard for doctors
 * with medical statistics and quick access to medical functions.
 */
public class DoctorDashboardFragment extends Fragment {

    private BarChart medicalOverviewChart;
    private TextView welcomeTitle;
    private TextView doctorNameText;
    private TextView welcomeSubtitle;
    private WebView macbookAnimation;
    private HCasDatabaseHelper databaseHelper;
    private FirebaseRTDBHelper firebaseRTDBHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_dashboard, container, false);
        
        initializeDatabase();
        initializeViews(view);
        setupLineChart();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh chart when returning to this fragment
        android.util.Log.d("DoctorDashboard", "📊 Fragment resumed, refreshing chart...");
        if (medicalOverviewChart != null) {
            setupLineChart();
        } else {
            android.util.Log.w("DoctorDashboard", "⚠️ Chart is null, cannot refresh");
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up WebView to prevent memory leaks
        if (macbookAnimation != null) {
            macbookAnimation.stopLoading();
            macbookAnimation.loadUrl("about:blank");
            macbookAnimation.clearHistory();
            macbookAnimation.removeAllViews();
            macbookAnimation.destroy();
            macbookAnimation = null;
        }
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
        firebaseRTDBHelper = new FirebaseRTDBHelper(getContext());
    }

    private void initializeViews(View view) {
        medicalOverviewChart = view.findViewById(R.id.medicalOverviewChart);
        welcomeTitle = view.findViewById(R.id.welcomeTitle);
        doctorNameText = view.findViewById(R.id.doctorNameText);
        welcomeSubtitle = view.findViewById(R.id.welcomeSubtitle);
        
        // Setup MacBook Animation WebView
        macbookAnimation = view.findViewById(R.id.macbookAnimation);
        if (macbookAnimation != null) {
            setupMacbookAnimation();
        }
        
        // Get doctor name from activity if available
        if (getActivity() instanceof DoctorDashboardActivity) {
            DoctorDashboardActivity activity = (DoctorDashboardActivity) getActivity();
            String doctorName = activity.getLoggedInFullName();
            if (doctorName != null && !doctorName.isEmpty()) {
                doctorNameText.setText(doctorName);
            } else {
                doctorNameText.setText("Doctor");
            }
        } else {
            doctorNameText.setText("Doctor");
        }
    }
    
    /**
     * Setup the MacBook 3D animation in WebView
     * Automatically adjusts quality based on device performance
     */
    private void setupMacbookAnimation() {
        try {
            // Detect device performance tier
            DevicePerformanceUtils.PerformanceTier tier = DevicePerformanceUtils.getPerformanceTier(getContext());
            boolean useLiteVersion = DevicePerformanceUtils.shouldUseSimplifiedAnimation(getContext());
            
            // Configure WebView for smooth animation
            WebSettings webSettings = macbookAnimation.getSettings();
            webSettings.setJavaScriptEnabled(false);
            webSettings.setDomStorageEnabled(false);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            webSettings.setSupportZoom(false);
            webSettings.setBuiltInZoomControls(false);
            webSettings.setDisplayZoomControls(false);
            
            // Make WebView background transparent
            macbookAnimation.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            macbookAnimation.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            macbookAnimation.setVerticalScrollBarEnabled(false);
            macbookAnimation.setHorizontalScrollBarEnabled(false);
            macbookAnimation.setOverScrollMode(View.OVER_SCROLL_NEVER);
            
            // Load appropriate animation based on device performance
            String animationFile = useLiteVersion 
                ? "file:///android_asset/macbook_animation_doctor_lite.html"
                : "file:///android_asset/macbook_animation_doctor.html";
            
            macbookAnimation.loadUrl(animationFile);
            
            Log.d("DoctorDashboard", "✅ MacBook animation WebView initialized");
            Log.d("DoctorDashboard", "   Performance Tier: " + tier + " | Using " + (useLiteVersion ? "LITE" : "FULL") + " animation");
        } catch (Exception e) {
            Log.e("DoctorDashboard", "❌ Error setting up MacBook animation: " + e.getMessage(), e);
        }
    }

    private void setupLineChart() {
        if (medicalOverviewChart == null || getContext() == null) {
            return;
        }

        android.util.Log.d("DoctorDashboard", "📊 Starting chart setup...");
        
        // Fetch patients from Firebase RTDB (primary source) with SQLite fallback
        if (firebaseRTDBHelper != null) {
            android.util.Log.d("DoctorDashboard", "📊 Firebase RTDB helper available, fetching patients...");
            firebaseRTDBHelper.getAllPatients(patients -> {
                android.util.Log.d("DoctorDashboard", "📊 Firebase callback received: " + (patients != null ? patients.size() : 0) + " patients");
                
                // Log sample patient data if available
                if (patients != null && !patients.isEmpty()) {
                    com.healthcare.cas.models.Patient sample = patients.get(0);
                    android.util.Log.d("DoctorDashboard", "📊 Sample patient: ID=" + sample.getPatientId() + ", CreatedDate=" + sample.getCreatedDate());
                }
                
                // If Firebase returns empty or null, fallback to SQLite
                if (patients == null || patients.isEmpty()) {
                    android.util.Log.w("DoctorDashboard", "⚠️ Firebase returned empty, falling back to SQLite");
                    com.healthcare.cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                        List<com.healthcare.cas.models.Patient> sqlitePatients = databaseHelper != null ? 
                            databaseHelper.getAllPatients() : new java.util.ArrayList<>();
                        android.util.Log.d("DoctorDashboard", "📊 SQLite patients loaded: " + sqlitePatients.size());
                        
                        // Log sample SQLite patient data
                        if (!sqlitePatients.isEmpty()) {
                            com.healthcare.cas.models.Patient sample = sqlitePatients.get(0);
                            android.util.Log.d("DoctorDashboard", "📊 Sample SQLite patient: ID=" + sample.getPatientId() + ", CreatedDate=" + sample.getCreatedDate());
                        }
                        
                        processPatientData(sqlitePatients);
                    });
                } else {
                    // Process Firebase data
                    android.util.Log.d("DoctorDashboard", "📊 Processing Firebase data...");
                    com.healthcare.cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                        processPatientData(patients);
                    });
                }
            });
        } else {
            // Fallback to SQLite if Firebase not available
            android.util.Log.w("DoctorDashboard", "⚠️ Firebase not available, using SQLite");
            com.healthcare.cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                List<com.healthcare.cas.models.Patient> patients = databaseHelper != null ? 
                    databaseHelper.getAllPatients() : new java.util.ArrayList<>();
                android.util.Log.d("DoctorDashboard", "📊 SQLite patients loaded: " + patients.size());
                
                // Log sample SQLite patient data
                if (!patients.isEmpty()) {
                    com.healthcare.cas.models.Patient sample = patients.get(0);
                    android.util.Log.d("DoctorDashboard", "📊 Sample SQLite patient: ID=" + sample.getPatientId() + ", CreatedDate=" + sample.getCreatedDate());
                }
                
                processPatientData(patients);
            });
        }
    }
    
    /**
     * Process patient data and update bar chart
     */
    private void processPatientData(List<com.healthcare.cas.models.Patient> patients) {
        // Get patient registrations grouped by date
        final java.util.Map<String, Integer> patientCountByDate = getPatientRegistrationsByDate(patients);
        android.util.Log.d("DoctorDashboard", "📊 Patient count by date: " + patientCountByDate);
        
        // Generate last 7 days (including today)
        final java.util.List<String> last7Days = generateLast7Days();
        android.util.Log.d("DoctorDashboard", "📊 Last 7 days: " + last7Days);
        
        // Create bar entries with accurate counts for each day
        final ArrayList<BarEntry> barEntries = new ArrayList<>();
        final ArrayList<String> dateLabels = new ArrayList<>();
        
        for (int i = 0; i < last7Days.size(); i++) {
            String date = last7Days.get(i);
            // Get accurate count from database based on actual registration dates (0 if no patients registered that day)
            int count = patientCountByDate.getOrDefault(date, 0);
            barEntries.add(new BarEntry(i, count));
            dateLabels.add(formatDateForDisplay(date));
            android.util.Log.d("DoctorDashboard", "📊 Bar entry " + i + ": Date=" + date + ", Count=" + count + " patients, Display=" + formatDateForDisplay(date));
        }
        
        // Log all dates with patient counts for debugging
        android.util.Log.d("DoctorDashboard", "📊 All patient counts by date: " + patientCountByDate);
        
        // Log summary
        int totalPatientsInPeriod = 0;
        for (int count : patientCountByDate.values()) {
            totalPatientsInPeriod += count;
        }
        android.util.Log.d("DoctorDashboard", "📊 Total patients in last 7 days: " + totalPatientsInPeriod);
        android.util.Log.d("DoctorDashboard", "📊 Total patients processed: " + (patients != null ? patients.size() : 0));

        // Update UI on main thread
        com.healthcare.cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
            if (getContext() == null || getView() == null || medicalOverviewChart == null) {
                return; // Fragment is detached
            }

            // Configure bar chart
            medicalOverviewChart.getDescription().setEnabled(false);
            medicalOverviewChart.setTouchEnabled(true);
            medicalOverviewChart.setDragEnabled(true);
            medicalOverviewChart.setScaleEnabled(true);
            medicalOverviewChart.setPinchZoom(true);
            medicalOverviewChart.setDrawGridBackground(false);
            medicalOverviewChart.setBackgroundColor(Color.WHITE);
            medicalOverviewChart.setDrawBarShadow(false);
            medicalOverviewChart.setDrawValueAboveBar(true);

            // X-Axis configuration (Dates - Horizontal)
            XAxis xAxis = medicalOverviewChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setGranularity(1f);
            xAxis.setTextColor(Color.parseColor("#757575"));
            xAxis.setTextSize(10f);
            xAxis.setLabelRotationAngle(-45f);
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    int index = (int) value;
                    if (index >= 0 && index < dateLabels.size()) {
                        return dateLabels.get(index);
                    }
                    return "";
                }
            });

            // Y-Axis configuration (Total Number of Patients - Vertical)
            YAxis leftAxis = medicalOverviewChart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
            leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
            leftAxis.setTextColor(Color.parseColor("#757575"));
            leftAxis.setTextSize(11f);
            leftAxis.setAxisMinimum(0f);
            leftAxis.setGranularity(1f);
            
            // Calculate max value for better Y-axis scaling
            float maxValue = 0f;
            for (BarEntry entry : barEntries) {
                if (entry.getY() > maxValue) {
                    maxValue = entry.getY();
                }
            }
            // Set max to at least 5, or max value + 2 for better visibility
            float axisMaximum = Math.max(5f, maxValue + 2f);
            leftAxis.setAxisMaximum(axisMaximum);
            leftAxis.setLabelCount(Math.min(8, (int)axisMaximum + 1), true);
            
            android.util.Log.d("DoctorDashboard", "📊 Y-Axis: Min=0, Max=" + axisMaximum + ", Max patient count=" + maxValue);

            YAxis rightAxis = medicalOverviewChart.getAxisRight();
            rightAxis.setEnabled(false);

            // Create bar dataset
            BarDataSet barDataSet = new BarDataSet(barEntries, "Total Patients Registered");
            barDataSet.setColor(getContext().getColor(R.color.doctor_navy));
            barDataSet.setValueTextSize(10f);
            barDataSet.setValueTextColor(Color.parseColor("#757575"));
            barDataSet.setDrawValues(true);

            // Create BarData and add dataset
            BarData barData = new BarData(barDataSet);
            barData.setBarWidth(0.6f);
            
            // Log final data before setting
            android.util.Log.d("DoctorDashboard", "📊 Setting chart data with " + barEntries.size() + " entries");
            for (int i = 0; i < barEntries.size(); i++) {
                android.util.Log.d("DoctorDashboard", "📊 Entry " + i + ": X=" + barEntries.get(i).getX() + ", Y=" + barEntries.get(i).getY());
            }
            
            medicalOverviewChart.setData(barData);
            medicalOverviewChart.animateY(1000);
            medicalOverviewChart.invalidate();
            
            android.util.Log.d("DoctorDashboard", "✅ Chart data set and invalidated");
        });
    }
    
    /**
     * Generate list of last 7 days (including today)
     * Returns dates in YYYY-MM-DD format, with today as the last entry
     */
    private java.util.List<String> generateLast7Days() {
        java.util.List<String> dates = new java.util.ArrayList<>();
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        
        // Start from 6 days ago and go up to today (7 days total)
        for (int i = 6; i >= 0; i--) {
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(java.util.Calendar.DAY_OF_MONTH, -i);
            String date = dateFormat.format(calendar.getTime());
            dates.add(date);
        }
        
        return dates;
    }
    
    /**
     * Get patient registrations grouped by date
     * Returns a map of date (YYYY-MM-DD) to accurate patient count
     * Only includes dates that have patient registrations
     * @param patients List of patients from Firebase (if null, uses SQLite fallback)
     */
    private java.util.Map<String, Integer> getPatientRegistrationsByDate(List<com.healthcare.cas.models.Patient> patients) {
        java.util.Map<String, Integer> dateCountMap = new java.util.HashMap<>();
        
        try {
            // Use provided patients list (from Firebase) or fetch from SQLite
            List<com.healthcare.cas.models.Patient> allPatients = patients;
            if (allPatients == null && databaseHelper != null) {
                allPatients = databaseHelper.getAllPatients();
            }
            
            if (allPatients != null && !allPatients.isEmpty()) {
                android.util.Log.d("DoctorDashboard", "📊 Processing " + allPatients.size() + " patients");
                int processedCount = 0;
                int skippedCount = 0;
                
                for (com.healthcare.cas.models.Patient patient : allPatients) {
                    String createdDate = patient.getCreatedDate();
                    android.util.Log.d("DoctorDashboard", "📊 Processing patient " + patient.getPatientId() + ", createdDate=" + createdDate);
                    
                    // Only process patients with valid created_date
                    if (createdDate != null && !createdDate.isEmpty()) {
                        // Extract date part (YYYY-MM-DD) from datetime string
                        String dateOnly = extractDateOnly(createdDate);
                        android.util.Log.d("DoctorDashboard", "📊 Extracted date: " + dateOnly + " from: " + createdDate);
                        
                        if (dateOnly != null && !dateOnly.isEmpty()) {
                            // Count patients registered on this specific date
                            int currentCount = dateCountMap.getOrDefault(dateOnly, 0);
                            dateCountMap.put(dateOnly, currentCount + 1);
                            processedCount++;
                            android.util.Log.d("DoctorDashboard", "✅ Patient " + patient.getPatientId() + " registered on " + dateOnly + " (count for this date: " + dateCountMap.get(dateOnly) + ")");
                        } else {
                            skippedCount++;
                            android.util.Log.w("DoctorDashboard", "⚠️ Could not extract date from: " + createdDate + " for patient " + patient.getPatientId());
                        }
                    } else {
                        skippedCount++;
                        android.util.Log.w("DoctorDashboard", "⚠️ Patient " + patient.getPatientId() + " has no created_date - skipping (will not appear in graph)");
                    }
                }
                android.util.Log.d("DoctorDashboard", "📊 Processed: " + processedCount + ", Skipped: " + skippedCount + ", Total dates with patients: " + dateCountMap.size());
            } else {
                android.util.Log.w("DoctorDashboard", "⚠️ allPatients is null or empty");
            }
            
        } catch (Exception e) {
            android.util.Log.e("DoctorDashboard", "Error getting patient registrations by date", e);
        }
        
        return dateCountMap;
    }
    
    /**
     * Extract date only (YYYY-MM-DD) from datetime string
     * Handles multiple date formats and ensures accurate extraction
     */
    private String extractDateOnly(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return null;
        }
        
        try {
            // Trim whitespace
            dateTimeString = dateTimeString.trim();
            
            // Handle different date formats
            // Format 1: "YYYY-MM-DD HH:MM:SS" or "YYYY-MM-DD HH:MM:SS.SSS" (most common)
            if (dateTimeString.contains(" ")) {
                // Has time component, extract date part (first part before space)
                String datePart = dateTimeString.split(" ")[0];
                // Validate YYYY-MM-DD format
                if (datePart.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    android.util.Log.d("DoctorDashboard", "📊 Extracted date from datetime: " + datePart);
                    return datePart;
                }
            }
            
            // Format 2: "YYYY-MM-DD" (already correct format)
            if (dateTimeString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                android.util.Log.d("DoctorDashboard", "📊 Date already in correct format: " + dateTimeString);
                return dateTimeString;
            }
            
            // Format 2b: SQLite datetime format "YYYY-MM-DD HH:MM:SS" (handle T separator too)
            if (dateTimeString.contains("T")) {
                String datePart = dateTimeString.split("T")[0];
                if (datePart.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    android.util.Log.d("DoctorDashboard", "📊 Extracted date from ISO format: " + datePart);
                    return datePart;
                }
            }
            
            // Format 3: "MM/DD/YYYY" or "M/D/YYYY"
            if (dateTimeString.contains("/")) {
                String[] parts = dateTimeString.split("/");
                if (parts.length == 3) {
                    try {
                        int month = Integer.parseInt(parts[0].trim());
                        int day = Integer.parseInt(parts[1].trim());
                        int year = Integer.parseInt(parts[2].trim());
                        // Convert to YYYY-MM-DD format
                        return String.format("%04d-%02d-%02d", year, month, day);
                    } catch (NumberFormatException e) {
                        android.util.Log.w("DoctorDashboard", "⚠️ Invalid date format: " + dateTimeString);
                    }
                }
            }
            
            // Format 4: Timestamp (long value)
            try {
                long timestamp = Long.parseLong(dateTimeString);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                return sdf.format(new java.util.Date(timestamp));
            } catch (NumberFormatException e) {
                // Not a timestamp, continue
            }
            
            android.util.Log.w("DoctorDashboard", "⚠️ Unrecognized date format: " + dateTimeString);
            return null;
            
        } catch (Exception e) {
            android.util.Log.e("DoctorDashboard", "Error extracting date from: " + dateTimeString, e);
            return null;
        }
    }
    
    /**
     * Format date for display (e.g., "Nov 17" or "12/17")
     */
    private String formatDateForDisplay(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "";
        }
        
        try {
            // Parse YYYY-MM-DD format
            String[] parts = dateString.split("-");
            if (parts.length == 3) {
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                
                String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                       "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                
                if (month >= 1 && month <= 12) {
                    return monthNames[month - 1] + " " + day;
                }
            }
        } catch (Exception e) {
            // If parsing fails, return original string
        }
        
        return dateString;
    }
    
    /**
     * Get count of active patients (patients without prescriptions)
     */
    private int getActivePatientsCount() {
        return databaseHelper.getPendingReviewsCount();
    }
    
    /**
     * Get total prescriptions count (optimized - uses count query instead of loading all)
     */
    private int getPrescriptionsCount() {
        return databaseHelper.getPrescriptionsCount();
    }

}




















