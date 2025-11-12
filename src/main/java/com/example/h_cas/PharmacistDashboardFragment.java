package com.example.h_cas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import com.example.h_cas.database.HCasDatabaseHelper;

import java.util.List;

/**
 * PharmacistDashboardFragment displays the main dashboard for pharmacists
 * with medication statistics and quick access to pharmacy functions.
 */
public class PharmacistDashboardFragment extends Fragment {

    private RecyclerView statsRecyclerView;
    private TextView welcomeTextView;
    private TextView subtitleTextView;
    private MaterialButton inventoryButton;
    private MaterialButton dispensingButton;
    private MaterialButton interactionsButton;
    private MaterialButton verificationButton;
    private MaterialButton profileButton;
    private MaterialButton reportsButton;
    
    private HCasDatabaseHelper databaseHelper;
    private String loggedInFullName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pharmacist_dashboard, container, false);
        
        initializeViews(view);
        initializeData();
        setupStatsRecyclerView();
        setupClickListeners();
        
        return view;
    }

    private void initializeViews(View view) {
        statsRecyclerView = view.findViewById(R.id.statsRecyclerView);
        welcomeTextView = view.findViewById(R.id.welcomeTextView);
        subtitleTextView = view.findViewById(R.id.subtitleTextView);
        inventoryButton = view.findViewById(R.id.inventoryButton);
        dispensingButton = view.findViewById(R.id.dispensingButton);
        interactionsButton = view.findViewById(R.id.interactionsButton);
        verificationButton = view.findViewById(R.id.verificationButton);
        profileButton = view.findViewById(R.id.profileButton);
        reportsButton = view.findViewById(R.id.reportsButton);
    }

    private void initializeData() {
        databaseHelper = new HCasDatabaseHelper(getContext());
        
        // Get logged-in pharmacist name from parent activity
        if (getActivity() != null) {
            Intent intent = getActivity().getIntent();
            loggedInFullName = intent.getStringExtra("FULL_NAME");
        }
        
        if (loggedInFullName != null && !loggedInFullName.isEmpty()) {
            welcomeTextView.setText("Welcome, " + loggedInFullName);
        } else {
            welcomeTextView.setText("Welcome to Pharmacist Dashboard");
        }
        
        subtitleTextView.setText("Pharmacy management and medication dispensing system");
    }

    private void setupStatsRecyclerView() {
        // Get real data from database
        int totalPrescriptions = databaseHelper.getPrescriptionsCount();
        int dispensedToday = getDispensedTodayCount();
        int lowStockMedicines = getLowStockMedicinesCount();
        int expiringSoon = getExpiringSoonCount();
        int pendingReviews = getPendingReviewsCount();
        int totalMedicines = getTotalMedicinesCount();
        
        // Create pharmacist-specific stats data with real values
        String[] statsLabels = {"Total Prescriptions", "Dispensed Today", "Low Stock Alert", "Expiring Soon", "Pending Reviews", "Total Medicines"};
        String[] statsValues = {
            String.valueOf(totalPrescriptions),
            String.valueOf(dispensedToday),
            String.valueOf(lowStockMedicines),
            String.valueOf(expiringSoon),
            String.valueOf(pendingReviews),
            String.valueOf(totalMedicines)
        };
        int[] statsColors = {R.color.primary_blue, R.color.success_green, R.color.warning_orange, R.color.error_red, R.color.accent_blue, R.color.text_secondary};

        StatsAdapter adapter = new StatsAdapter(statsLabels, statsValues, statsColors);
        statsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        statsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        inventoryButton.setOnClickListener(v -> navigateToInventory());
        dispensingButton.setOnClickListener(v -> navigateToDispensing());
        interactionsButton.setOnClickListener(v -> navigateToDisposedMedicine());
        verificationButton.setOnClickListener(v -> navigateToVerification());
        profileButton.setOnClickListener(v -> navigateToProfile());
        reportsButton.setOnClickListener(v -> navigateToReports());
    }

    // Navigation methods
    private void navigateToInventory() {
        if (getActivity() instanceof PharmacistDashboardActivity) {
            ((PharmacistDashboardActivity) getActivity()).loadFragment(new NewEnhancedInventoryFragment());
        }
    }

    private void navigateToDispensing() {
        if (getActivity() instanceof PharmacistDashboardActivity) {
            ((PharmacistDashboardActivity) getActivity()).loadFragment(new MedicationDispensingFragment());
        }
    }

    private void navigateToDisposedMedicine() {
        if (getActivity() instanceof PharmacistDashboardActivity) {
            ((PharmacistDashboardActivity) getActivity()).loadFragment(new DisposedMedicineFragment());
        }
    }

    private void navigateToVerification() {
        if (getActivity() instanceof PharmacistDashboardActivity) {
            ((PharmacistDashboardActivity) getActivity()).loadFragment(new PrescriptionVerificationFragment());
        }
    }

    private void navigateToProfile() {
        if (getActivity() instanceof PharmacistDashboardActivity) {
            ((PharmacistDashboardActivity) getActivity()).loadFragment(new PharmacistProfileFragment());
        }
    }

    private void navigateToReports() {
        if (getActivity() instanceof PharmacistDashboardActivity) {
            ((PharmacistDashboardActivity) getActivity()).loadFragment(new PharmacyReportsFragment());
        }
    }

    // Helper methods for real data
    private int getDispensedTodayCount() {
        // For now, return a sample count. In real implementation, this would query dispensed prescriptions for today
        return 12;
    }

    private int getLowStockMedicinesCount() {
        // Get real count using configurable minimum stock
        if (getContext() == null || databaseHelper == null) {
            return 0;
        }
        int minimumStock = PharmacistSettingsFragment.getMinimumStockQuantity(getContext());
        // Use optimized query instead of loading all medicines
        return databaseHelper.getLowStockMedicinesCount(minimumStock);
    }

    private int getExpiringSoonCount() {
        // Get real count using configurable threshold
        if (getContext() == null || databaseHelper == null) {
            return 0;
        }
        int thresholdMonths = PharmacistSettingsFragment.getExpiryNotificationMonths(getContext());
        // Use optimized query instead of loading all medicines
        return databaseHelper.getExpiringSoonMedicinesCount(thresholdMonths);
    }
    

    /**
     * Check if a medicine is expiring soon based on the configurable threshold
     */
    private boolean isExpiringSoon(com.example.h_cas.models.Medicine medicine, int thresholdMonths) {
        if (medicine == null || medicine.getExpiryDate() == null || medicine.getExpiryDate().isEmpty()) {
            return false;
        }
        
        try {
            // Parse expiry date (format: YYYY-MM-DD)
            String expiryDateStr = medicine.getExpiryDate().trim();
            String[] dateParts = expiryDateStr.split("-");
            
            if (dateParts.length != 3) {
                return false;
            }
            
            int expiryYear = Integer.parseInt(dateParts[0]);
            int expiryMonth = Integer.parseInt(dateParts[1]);
            int expiryDay = Integer.parseInt(dateParts[2]);
            
            // Get current date
            java.util.Calendar currentCal = java.util.Calendar.getInstance();
            int currentYear = currentCal.get(java.util.Calendar.YEAR);
            int currentMonth = currentCal.get(java.util.Calendar.MONTH) + 1; // Calendar months are 0-based
            int currentDay = currentCal.get(java.util.Calendar.DAY_OF_MONTH);
            
            // Calculate months difference
            int yearDiff = expiryYear - currentYear;
            int monthDiff = (yearDiff * 12) + (expiryMonth - currentMonth);
            
            // Adjust for day difference
            if (monthDiff > 0 && expiryDay < currentDay) {
                monthDiff--;
            }
            
            // Check if within threshold
            return monthDiff >= 0 && monthDiff <= thresholdMonths;
            
        } catch (Exception e) {
            return false;
        }
    }

    private int getPendingReviewsCount() {
        // For now, return a sample count. In real implementation, this would query pending prescription reviews
        return 7;
    }

    private int getTotalMedicinesCount() {
        // Get real count from database
        return databaseHelper.getTotalMedicinesCount();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh stats when returning to dashboard
        setupStatsRecyclerView();
    }

    // Simple RecyclerView adapter for stats cards
    private class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.StatsViewHolder> {
        private String[] labels;
        private String[] values;
        private int[] colors;

        public StatsAdapter(String[] labels, String[] values, int[] colors) {
            this.labels = labels;
            this.values = values;
            this.colors = colors;
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



