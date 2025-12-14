package com.healthcare.cas;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import com.healthcare.cas.database.FirebaseRTDBHelper;
import com.healthcare.cas.models.Employee;
import com.healthcare.cas.utils.DevicePerformanceUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

/**
 * AdminDashboardFragment displays the main dashboard with system overview
 * and quick access to common administrative tasks.
 */
public class AdminDashboardFragment extends Fragment {

    private RecyclerView statsRecyclerView;
    private WebView macbookAnimation;
    private TextView adminNameText;
    private FirebaseRTDBHelper firebaseRTDBHelper;
    private StatsAdapter statsAdapter;
    private com.google.firebase.database.ValueEventListener employeesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
        
        initializeViews(view);
        setupStatsRecyclerView();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Set up real-time listener when fragment becomes visible
        setupRealTimeStatisticsListener();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Remove listener when fragment is not visible to save resources
        removeRealTimeStatisticsListener();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up listener when view is destroyed
        removeRealTimeStatisticsListener();
        
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

    private void initializeViews(View view) {
        statsRecyclerView = view.findViewById(R.id.statsRecyclerView);
        adminNameText = view.findViewById(R.id.adminNameText);
        
        // Setup MacBook Animation WebView
        macbookAnimation = view.findViewById(R.id.macbookAnimation);
        if (macbookAnimation != null) {
            setupMacbookAnimation();
        }
        
        // Initialize Firebase RTDB Helper - PRIMARY AND ONLY DATA SOURCE
        try {
            firebaseRTDBHelper = new FirebaseRTDBHelper(getContext());
            Log.d("AdminDashboard", "✅ FirebaseRTDBHelper initialized - using Firebase as PRIMARY data source");
        } catch (Exception e) {
            Log.e("AdminDashboard", "❌ Failed to initialize FirebaseRTDBHelper", e);
            firebaseRTDBHelper = null;
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
            webSettings.setJavaScriptEnabled(false); // Not needed for pure CSS
            webSettings.setDomStorageEnabled(false);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            webSettings.setSupportZoom(false);
            webSettings.setBuiltInZoomControls(false);
            webSettings.setDisplayZoomControls(false);
            
            // Make WebView background transparent
            macbookAnimation.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            
            // Use hardware acceleration for better performance
            macbookAnimation.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            
            // Disable scrolling for better performance
            macbookAnimation.setVerticalScrollBarEnabled(false);
            macbookAnimation.setHorizontalScrollBarEnabled(false);
            macbookAnimation.setOverScrollMode(View.OVER_SCROLL_NEVER);
            
            // Load appropriate animation based on device performance
            String animationFile = useLiteVersion 
                ? "file:///android_asset/macbook_animation_lite.html"
                : "file:///android_asset/macbook_animation.html";
            
            macbookAnimation.loadUrl(animationFile);
            
            Log.d("AdminDashboard", "✅ MacBook animation WebView initialized");
            Log.d("AdminDashboard", "   Performance Tier: " + tier + " | Using " + (useLiteVersion ? "LITE" : "FULL") + " animation");
        } catch (Exception e) {
            Log.e("AdminDashboard", "❌ Error setting up MacBook animation: " + e.getMessage(), e);
        }
    }

    private void setupStatsRecyclerView() {
        // Initialize adapter with essential employee statistics only
        String[] statsLabels = {"Total Employees", "Nurses", "Doctors", "Pharmacists"};
        String[] defaultValues = {"0", "0", "0", "0"};
        // Use green-themed colors for admin dashboard
        int[] statsColors = {
            R.color.success_green,          // Total Employees - Green
            R.color.pharmacist_green,       // Nurses - Light Green
            R.color.login_green_primary,    // Doctors - Dark Green
            R.color.login_green_light       // Pharmacists - Medium Green
        };

        statsAdapter = new StatsAdapter(statsLabels, defaultValues, statsColors);
        statsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        statsRecyclerView.setAdapter(statsAdapter);
        
        // Firebase is the PRIMARY AND ONLY data source for admin dashboard
        // Real-time listener will be set up in onResume()
    }
    
    /**
     * Setup real-time listener for employee statistics from Firebase
     * FIREBASE IS THE PRIMARY AND ONLY DATA SOURCE
     * This ensures statistics are always up-to-date with Firebase data
     */
    private void setupRealTimeStatisticsListener() {
        if (firebaseRTDBHelper == null) {
            Log.e("AdminDashboard", "❌ FirebaseRTDBHelper is null - cannot load statistics. Dashboard requires Firebase.");
            showFirebaseError();
            return;
        }
        
        try {
            // Get Firebase database reference
            DatabaseReference rootRef = firebaseRTDBHelper.getRootRef();
            if (rootRef == null) {
                Log.e("AdminDashboard", "❌ Firebase rootRef is null - cannot load statistics. Dashboard requires Firebase.");
                showFirebaseError();
                return;
            }
            
            DatabaseReference employeesRef = rootRef.child("employees");
            
            // Remove existing listener if any
            removeRealTimeStatisticsListener();
            
            // Create real-time listener that uses FirebaseRTDBHelper's getAllEmployees for proper mapping
            employeesListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    // Use FirebaseRTDBHelper's getAllEmployees method for proper data mapping
                    // This ensures we use the same mapping logic as other parts of the app
                    firebaseRTDBHelper.getAllEmployees(employees -> {
                        if (employees == null) {
                            employees = new java.util.ArrayList<>();
                        }
                        Log.d("AdminDashboard", "🔥 Firebase data received: " + employees.size() + " employees (REAL-TIME UPDATE)");
                        // Calculate statistics from Firebase employee list
                        calculateAndUpdateStatistics(employees);
                    });
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e("AdminDashboard", "❌ Firebase listener cancelled: " + error.getMessage());
                    showFirebaseError();
                }
            };
            
            // Attach real-time listener - this will trigger immediately for initial load
            // and then on every data change
            employeesRef.addValueEventListener(employeesListener);
            Log.d("AdminDashboard", "✅ Real-time Firebase statistics listener attached - Dashboard 100% based on Firebase data");
            
        } catch (Exception e) {
            Log.e("AdminDashboard", "❌ Error setting up Firebase listener: " + e.getMessage(), e);
            showFirebaseError();
        }
    }
    
    /**
     * Show error state when Firebase is not available
     */
    private void showFirebaseError() {
        if (getActivity() != null && statsAdapter != null) {
            getActivity().runOnUiThread(() -> {
                // Show error values
                String[] errorValues = {"--", "--", "--", "--"};
                statsAdapter.updateValues(errorValues);
                Log.e("AdminDashboard", "❌ Dashboard cannot load - Firebase is required");
            });
        }
    }
    
    /**
     * Remove real-time statistics listener to prevent memory leaks
     */
    private void removeRealTimeStatisticsListener() {
        if (firebaseRTDBHelper != null && employeesListener != null) {
            try {
                DatabaseReference rootRef = firebaseRTDBHelper.getRootRef();
                if (rootRef != null) {
                    DatabaseReference employeesRef = rootRef.child("employees");
                    employeesRef.removeEventListener(employeesListener);
                    Log.d("AdminDashboard", "✅ Real-time statistics listener removed");
                }
            } catch (Exception e) {
                Log.e("AdminDashboard", "Error removing listener: " + e.getMessage());
            }
            employeesListener = null;
        }
    }
    
    /**
     * Calculate statistics from employee list and update UI
     * Ensures accurate counting with validation
     */
    private void calculateAndUpdateStatistics(List<Employee> employees) {
        if (employees == null) {
            employees = new java.util.ArrayList<>();
        }
        
        Log.d("AdminDashboard", "🔥 Calculating statistics from FIREBASE data: " + employees.size() + " total employees");
        
        // Filter active employees only (excluding administrators)
        // Use same filtering logic as ManageEmployeesFragment for consistency
        List<Employee> activeEmployees = new java.util.ArrayList<>();
        int skippedInactive = 0;
        int skippedAdmin = 0;
        
        for (Employee emp : employees) {
            if (emp == null) {
                continue;
            }
            
            // Check if employee is active - same logic as ManageEmployeesFragment
            // Note: isActive() returns boolean (primitive), so it can never be null
            // Only skip if explicitly set to false (deleted/inactive)
            boolean isActive = emp.isActive();
            if (!isActive) {
                skippedInactive++;
                Log.d("AdminDashboard", "   ⏭️ Skipped inactive employee: " + emp.getEmployeeId() + " (isActive=false)");
                continue;
            }
            // If isActive is true (default), include the employee in statistics
            
            // Check if employee is not an administrator - same logic as ManageEmployeesFragment
            String role = emp.getRole();
            if (role == null) {
                Log.w("AdminDashboard", "   ⚠️ Employee has null role: " + emp.getEmployeeId());
                continue;
            }
            
            String roleTrimmed = role.trim();
            // Use exact same check as ManageEmployeesFragment: !"Administrator".equals(emp.getRole())
            if (roleTrimmed.isEmpty() || "Administrator".equals(roleTrimmed)) {
                skippedAdmin++;
                Log.d("AdminDashboard", "   ⏭️ Skipped administrator: " + emp.getEmployeeId());
                continue;
            }
            
            activeEmployees.add(emp);
            Log.d("AdminDashboard", "   ✅ Active employee: " + emp.getEmployeeId() + " - Role: " + roleTrimmed);
        }
        
        Log.d("AdminDashboard", "   📋 Filtered: " + activeEmployees.size() + " active employees (skipped " + skippedInactive + " inactive, " + skippedAdmin + " admins)");
        Log.d("AdminDashboard", "   📊 This count should match the number of employees shown in Manage Employees list");
        
        // Count employees by role - use final variables for lambda
        final int totalEmployees = activeEmployees.size();
        final int[] nurses = {0};
        final int[] doctors = {0};
        final int[] pharmacists = {0};
        final int[] otherRoles = {0};
        
        for (Employee emp : activeEmployees) {
            if (emp != null) {
                String role = emp.getRole();
                if (role != null) {
                    String roleTrimmed = role.trim();
                    if ("Nurse".equalsIgnoreCase(roleTrimmed)) {
                        nurses[0]++;
                    } else if ("Doctor".equalsIgnoreCase(roleTrimmed)) {
                        doctors[0]++;
                    } else if ("Pharmacist".equalsIgnoreCase(roleTrimmed)) {
                        pharmacists[0]++;
                    } else {
                        otherRoles[0]++;
                        Log.w("AdminDashboard", "   ⚠️ Unknown role: " + roleTrimmed + " for employee " + emp.getEmployeeId());
                    }
                }
            }
        }
        
        // Validate: Total should equal sum of all roles
        int sumOfRoles = nurses[0] + doctors[0] + pharmacists[0] + otherRoles[0];
        if (totalEmployees != sumOfRoles) {
            Log.w("AdminDashboard", "   ⚠️ WARNING: Total (" + totalEmployees + ") doesn't match sum of roles (" + sumOfRoles + ")");
        }
        
        // Update UI on main thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                String[] statsValues = {
                    String.valueOf(totalEmployees),
                    String.valueOf(nurses[0]),
                    String.valueOf(doctors[0]),
                    String.valueOf(pharmacists[0])
                };
                
                // Update adapter with accurate values from Firebase
                if (statsAdapter != null) {
                    statsAdapter.updateValues(statsValues);
                }
                
                Log.d("AdminDashboard", "🔥 Statistics updated from FIREBASE (Real-time):");
                Log.d("AdminDashboard", "   ✅ Total Employees: " + totalEmployees + " (from Firebase)");
                Log.d("AdminDashboard", "   ✅ Nurses: " + nurses[0] + " (from Firebase)");
                Log.d("AdminDashboard", "   ✅ Doctors: " + doctors[0] + " (from Firebase)");
                Log.d("AdminDashboard", "   ✅ Pharmacists: " + pharmacists[0] + " (from Firebase)");
                if (otherRoles[0] > 0) {
                    Log.w("AdminDashboard", "   ⚠️ Other Roles: " + otherRoles[0] + " (not displayed)");
                }
                
                // Validation: Total Employees count should match Manage Employees list (when no filter)
                Log.d("AdminDashboard", "📊 VALIDATION:");
                Log.d("AdminDashboard", "   → 'Total Employees' (" + totalEmployees + ") SHOULD MATCH Manage Employees list count when no filter is applied");
                Log.d("AdminDashboard", "   → Clicking 'Total Employees' card → Manage Employees should show " + totalEmployees + " employees");
                Log.d("AdminDashboard", "   → Clicking 'Nurses' card → Manage Employees should show " + nurses[0] + " nurses");
                Log.d("AdminDashboard", "   → Clicking 'Doctors' card → Manage Employees should show " + doctors[0] + " doctors");
                Log.d("AdminDashboard", "   → Clicking 'Pharmacists' card → Manage Employees should show " + pharmacists[0] + " pharmacists");
            });
        }
    }
    

    // Simple RecyclerView adapter for stats cards
    private class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.StatsViewHolder> {
        private String[] labels;
        private String[] values;
        private int[] colors;

        public StatsAdapter(String[] labels, String[] values, int[] colors) {
            this.labels = labels;
            this.values = values.clone(); // Clone to avoid reference issues
            this.colors = colors;
        }
        
        /**
         * Update values in the adapter
         */
        public void updateValues(String[] newValues) {
            if (newValues != null && newValues.length == this.values.length) {
                this.values = newValues.clone();
                notifyDataSetChanged();
            }
        }

        @NonNull
        @Override
        public StatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stat_card, parent, false);
            return new StatsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StatsViewHolder holder, int position) {
            holder.labelText.setText(labels[position]);
            holder.valueText.setText(values[position]);
            holder.cardView.setCardBackgroundColor(getContext().getColor(colors[position]));
            
            // Make card clickable - navigate to Manage Employees with role filter
            holder.cardView.setOnClickListener(v -> {
                if (getActivity() instanceof AdminDashboardActivity) {
                    // Create ManageEmployeesFragment with role filter
                    ManageEmployeesFragment fragment = new ManageEmployeesFragment();
                    Bundle args = new Bundle();
                    
                    // Set role filter based on card position
                    // Position 0: Total Employees (no filter - show all)
                    // Position 1: Nurses (filter by "Nurse")
                    // Position 2: Doctors (filter by "Doctor")
                    // Position 3: Pharmacists (filter by "Pharmacist")
                    String roleFilter = null;
                    String title = "Manage Employees";
                    
                    if (position == 0) {
                        // Total Employees - no filter, show all active non-admin employees
                        // Explicitly set roleFilter to null to show all employees
                        roleFilter = null;
                        title = "Manage Employees";
                        Log.d("AdminDashboard", "🎯 Clicked Total Employees card - Dashboard shows " + values[0] + " total employees");
                        Log.d("AdminDashboard", "   → ManageEmployeesFragment should show exactly " + values[0] + " employees (all active non-admin)");
                        Log.d("AdminDashboard", "   → No role filter - showing ALL employees");
                    } else if (position == 1) {
                        roleFilter = "Nurse";
                        title = "Manage Nurses";
                        // Get the current nurses count from the dashboard
                        String nursesCount = values[1]; // Position 1 = Nurses count
                        Log.d("AdminDashboard", "🎯 Clicked Nurses card - Dashboard shows " + nursesCount + " nurses");
                        Log.d("AdminDashboard", "   → ManageEmployeesFragment should show exactly " + nursesCount + " nurses");
                    } else if (position == 2) {
                        roleFilter = "Doctor";
                        title = "Manage Doctors";
                        // Get the current doctors count from the dashboard
                        String doctorsCount = values[2]; // Position 2 = Doctors count
                        Log.d("AdminDashboard", "🎯 Clicked Doctors card - Dashboard shows " + doctorsCount + " doctors");
                        Log.d("AdminDashboard", "   → ManageEmployeesFragment should show exactly " + doctorsCount + " doctors");
                    } else if (position == 3) {
                        roleFilter = "Pharmacist";
                        title = "Manage Pharmacists";
                        // Get the current pharmacists count from the dashboard
                        String pharmacistsCount = values[3]; // Position 3 = Pharmacists count
                        Log.d("AdminDashboard", "🎯 Clicked Pharmacists card - Dashboard shows " + pharmacistsCount + " pharmacists");
                        Log.d("AdminDashboard", "   → ManageEmployeesFragment should show exactly " + pharmacistsCount + " pharmacists");
                    }
                    // Set filter in bundle - if roleFilter is null, don't add it (shows all employees)
                    // For position 0 (Total Employees), roleFilter is explicitly null to show all
                    if (roleFilter != null) {
                        args.putString("FILTER_ROLE", roleFilter);
                        Log.d("AdminDashboard", "🎯 Passing role filter to ManageEmployeesFragment: " + roleFilter);
                    } else {
                        // Explicitly clear any existing filter by not adding FILTER_ROLE to bundle
                        // This ensures "All" employees are shown
                        Log.d("AdminDashboard", "ℹ️ No role filter - showing all employees (Total Employees selected)");
                    }
                    fragment.setArguments(args);
                    
                    Log.d("AdminDashboard", "🔄 Navigating to ManageEmployeesFragment with filter: " + (roleFilter != null ? roleFilter : "ALL"));
                    ((AdminDashboardActivity) getActivity()).loadFragment(fragment);
                    ((AdminDashboardActivity) getActivity()).getSupportActionBar().setTitle(title);
                    // Update bottom navigation to reflect current fragment
                    ((AdminDashboardActivity) getActivity()).updateBottomNavigation(R.id.nav_manage_employees);
                }
            });
        }

        @Override
        public int getItemCount() {
            return labels.length;
        }

        class StatsViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView;
            TextView labelText;
            TextView valueText;

            public StatsViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.statCardView);
                labelText = itemView.findViewById(R.id.statLabelText);
                valueText = itemView.findViewById(R.id.statValueText);
            }
        }
    }
}
