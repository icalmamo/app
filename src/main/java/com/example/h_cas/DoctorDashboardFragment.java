package com.example.h_cas;

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

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

import com.example.h_cas.database.HCasDatabaseHelper;

import java.util.List;

/**
 * DoctorDashboardFragment displays the main dashboard for doctors
 * with medical statistics and quick access to medical functions.
 */
public class DoctorDashboardFragment extends Fragment {

    private RecyclerView statsRecyclerView;
    private TextView welcomeTextView;
    private HCasDatabaseHelper databaseHelper;
    private MaterialButton quickActionViewPatients;
    private MaterialButton quickActionNewDiagnosis;
    private MaterialButton quickActionWritePrescription;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_dashboard, container, false);
        
        initializeDatabase();
        initializeViews(view);
        setupStatsRecyclerView();
        setupQuickActionButtons(view);
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh stats when returning to this fragment
        if (databaseHelper != null) {
            setupStatsRecyclerView();
        }
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void initializeViews(View view) {
        statsRecyclerView = view.findViewById(R.id.statsRecyclerView);
        welcomeTextView = view.findViewById(R.id.welcomeTextView);
        quickActionViewPatients = view.findViewById(R.id.quickActionViewPatients);
        quickActionNewDiagnosis = view.findViewById(R.id.quickActionNewDiagnosis);
        quickActionWritePrescription = view.findViewById(R.id.quickActionWritePrescription);
        
        // Get doctor name from activity if available
        if (getActivity() instanceof DoctorDashboardActivity) {
            DoctorDashboardActivity activity = (DoctorDashboardActivity) getActivity();
            String doctorName = activity.getLoggedInFullName();
            if (doctorName != null && !doctorName.isEmpty()) {
                welcomeTextView.setText("Welcome, " + doctorName);
            } else {
                welcomeTextView.setText("Welcome to Doctor Dashboard");
            }
        } else {
            welcomeTextView.setText("Welcome to Doctor Dashboard");
        }
    }
    
    /**
     * Setup quick action buttons with click listeners
     */
    private void setupQuickActionButtons(View view) {
        // View Patients - Navigate to Registered Patients
        quickActionViewPatients.setOnClickListener(v -> {
            if (getActivity() instanceof DoctorDashboardActivity) {
                DoctorDashboardActivity activity = (DoctorDashboardActivity) getActivity();
                activity.loadFragment(new RegisteredPatientsFragment());
                activity.getSupportActionBar().setTitle("Registered Patients");
                // Update navigation drawer selection
                activity.updateNavigationSelection(R.id.nav_registered_patients);
            }
        });
        
        // New Diagnosis - Navigate to Create Diagnosis
        quickActionNewDiagnosis.setOnClickListener(v -> {
            if (getActivity() instanceof DoctorDashboardActivity) {
                DoctorDashboardActivity activity = (DoctorDashboardActivity) getActivity();
                CreateDiagnosisFragment diagnosisFragment = new CreateDiagnosisFragment();
                activity.loadFragment(diagnosisFragment);
                activity.getSupportActionBar().setTitle("Create Diagnosis");
            }
        });
        
        // Write Prescription - Navigate to Create Prescription
        quickActionWritePrescription.setOnClickListener(v -> {
            if (getActivity() instanceof DoctorDashboardActivity) {
                DoctorDashboardActivity activity = (DoctorDashboardActivity) getActivity();
                CreatePrescriptionFragment prescriptionFragment = new CreatePrescriptionFragment();
                activity.loadFragment(prescriptionFragment);
                activity.getSupportActionBar().setTitle("Create Prescription");
            }
        });
    }

    private void setupStatsRecyclerView() {
        // Get real statistics from database
        int activePatients = getActivePatientsCount();
        int todaysCases = databaseHelper.getTodaysCasesCount();
        int pendingDiagnoses = databaseHelper.getPendingReviewsCount();
        int prescriptionsWritten = getPrescriptionsCount();
        
        // Create doctor-specific stats data with real values (removed Follow-ups Due and Emergency Cases)
        String[] statsLabels = {"Active Patients", "Total Patients", "Pending Diagnoses", "Prescriptions Written"};
        String[] statsValues = {
            String.valueOf(activePatients),
            String.valueOf(todaysCases),
            String.valueOf(pendingDiagnoses),
            String.valueOf(prescriptionsWritten)
        };
        int[] statsColors = {R.color.primary_blue, R.color.success_green, R.color.warning_orange, R.color.accent_blue};

        StatsAdapter adapter = new StatsAdapter(statsLabels, statsValues, statsColors);
        statsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        statsRecyclerView.setAdapter(adapter);
    }
    
    /**
     * Get count of active patients (patients without prescriptions)
     */
    private int getActivePatientsCount() {
        return databaseHelper.getPendingReviewsCount();
    }
    
    /**
     * Get total prescriptions count
     */
    private int getPrescriptionsCount() {
        List<com.example.h_cas.models.Prescription> prescriptions = databaseHelper.getAllPrescriptions();
        return prescriptions != null ? prescriptions.size() : 0;
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




















