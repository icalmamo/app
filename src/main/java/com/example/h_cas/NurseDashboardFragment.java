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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import com.example.h_cas.database.HCasDatabaseHelper;

/**
 * NurseDashboardFragment displays the main dashboard for nurses
 * with patient care statistics and quick access to nursing functions.
 */
public class NurseDashboardFragment extends Fragment {

    private RecyclerView statsRecyclerView;
    private TextView welcomeTextView;
    private TextView subtitleTextView;
    private MaterialButton registrationButton;
    private MaterialButton monitoringButton;
    private MaterialButton profileButton;
    private MaterialButton prescriptionsButton;
    
    private HCasDatabaseHelper databaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nurse_dashboard, container, false);
        
        initializeViews(view);
        initializeDatabase();
        setupStatsRecyclerView();
        setupClickListeners();
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh statistics when returning to dashboard
        setupStatsRecyclerView();
    }

    private void initializeViews(View view) {
        statsRecyclerView = view.findViewById(R.id.statsRecyclerView);
        welcomeTextView = view.findViewById(R.id.welcomeTextView);
        subtitleTextView = view.findViewById(R.id.subtitleTextView);
        registrationButton = view.findViewById(R.id.registrationButton);
        monitoringButton = view.findViewById(R.id.monitoringButton);
        profileButton = view.findViewById(R.id.profileButton);
        prescriptionsButton = view.findViewById(R.id.prescriptionsButton);
        
        // Get employee data from arguments
        Bundle args = getArguments();
        if (args != null) {
            String fullName = args.getString("FULL_NAME");
            if (fullName != null && !fullName.isEmpty()) {
                welcomeTextView.setText("Welcome, " + fullName + "!");
            } else {
                welcomeTextView.setText("Welcome to Nurse Dashboard");
            }
        } else {
            welcomeTextView.setText("Welcome to Nurse Dashboard");
        }
        
        subtitleTextView.setText("Healthcare system overview and patient management");
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupStatsRecyclerView() {
        // Get real data from database
        int totalPatients = databaseHelper.getTotalPatientsCount();
        int monitoringCount = getMonitoringCount(); // Patients being monitored
        int prescriptionCount = getPrescriptionCount(); // Doctor prescriptions
        
        // Create healthcare system stats data with real values (removed Registration as it's same as Total Patients)
        String[] statsLabels = {"Total Patients", "Monitoring", "Doctor's Prescription"};
        String[] statsValues = {
            String.valueOf(totalPatients),
            String.valueOf(monitoringCount),
            String.valueOf(prescriptionCount)
        };
        int[] statsColors = {R.color.primary_blue, R.color.warning_orange, R.color.accent_blue};

        StatsAdapter adapter = new StatsAdapter(statsLabels, statsValues, statsColors);
        statsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        statsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Registration button click
        registrationButton.setOnClickListener(v -> {
            if (getActivity() instanceof NurseDashboardActivity) {
                NurseDashboardActivity activity = (NurseDashboardActivity) getActivity();
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new PatientRegistrationFragment())
                        .commit();
                activity.getSupportActionBar().setTitle("Registration");
            }
        });

        // Monitoring button click
        monitoringButton.setOnClickListener(v -> {
            if (getActivity() instanceof NurseDashboardActivity) {
                NurseDashboardActivity activity = (NurseDashboardActivity) getActivity();
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new PatientMonitoringFragment())
                        .commit();
                activity.getSupportActionBar().setTitle("Monitoring");
            }
        });

        // Profile button click
        profileButton.setOnClickListener(v -> {
            if (getActivity() instanceof NurseDashboardActivity) {
                NurseDashboardActivity activity = (NurseDashboardActivity) getActivity();
                Bundle args = getArguments();
                NurseProfileFragment profileFragment = new NurseProfileFragment();
                if (args != null) {
                    profileFragment.setArguments(args);
                }
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, profileFragment)
                        .commit();
                activity.getSupportActionBar().setTitle("Nurse Profile");
            }
        });

        // Prescriptions button click
        prescriptionsButton.setOnClickListener(v -> {
            if (getActivity() instanceof NurseDashboardActivity) {
                NurseDashboardActivity activity = (NurseDashboardActivity) getActivity();
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new ViewPrescriptionsFragment())
                        .commit();
                activity.getSupportActionBar().setTitle("Doctor's Prescriptions");
            }
        });
    }

    /**
     * Get count of patients being monitored
     */
    private int getMonitoringCount() {
        // For now, return a calculated value based on total patients
        // In a real system, this would query patients with monitoring status
        int totalPatients = databaseHelper.getTotalPatientsCount();
        return Math.max(1, totalPatients / 2); // Approximately 1/2 of patients being monitored
    }

    /**
     * Get count of doctor prescriptions
     */
    private int getPrescriptionCount() {
        // For now, return a calculated value based on total patients
        // In a real system, this would query prescriptions table
        int totalPatients = databaseHelper.getTotalPatientsCount();
        return Math.max(1, totalPatients / 2); // Approximately 1/2 of patients have prescriptions
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
