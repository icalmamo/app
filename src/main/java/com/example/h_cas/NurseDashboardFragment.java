package com.example.h_cas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.database.FirebaseRTDBHelper;
import com.example.h_cas.models.Patient;
import java.util.List;

/**
 * NurseDashboardFragment displays the main dashboard for nurses
 * with patient care statistics and quick access to nursing functions.
 */
public class NurseDashboardFragment extends Fragment {

    private RecyclerView statsRecyclerView;
    private TextView welcomeTextView;
    private TextView subtitleTextView;
    
    private HCasDatabaseHelper databaseHelper;
    private FirebaseRTDBHelper firebaseRTDBHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nurse_dashboard, container, false);

        initializeViews(view);
        initializeDatabase();
        setupStatsRecyclerView();

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
        firebaseRTDBHelper = new FirebaseRTDBHelper(getContext());
    }

    private void setupStatsRecyclerView() {
        // Show loading state with empty values (will show loading icon)
        String[] statsLabels = {"Total Patients", "Monitoring", "Doctor's Prescription"};
        String[] statsValues = {"", "", ""}; // Empty values will trigger loading state
        int[] statsColors = {R.color.nurse_teal, R.color.nurse_teal, R.color.nurse_teal};
        
        StatsAdapter adapter = new StatsAdapter(statsLabels, statsValues, statsColors, (position, label) -> {
            if ("Doctor's Prescription".equals(label)) {
                // Navigate to ViewPrescriptionsFragment
                if (getActivity() instanceof NurseDashboardActivity) {
                    NurseDashboardActivity activity = (NurseDashboardActivity) getActivity();
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, new ViewPrescriptionsFragment())
                            .addToBackStack(null)
                            .commit();
                    activity.getSupportActionBar().setTitle("Doctor's Prescriptions");
                }
            }
        });
        statsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        statsRecyclerView.setAdapter(adapter);
        
        // Fetch accurate data from Firebase RTDB
        loadStatisticsFromFirebase(adapter);
    }
    
    /**
     * Load statistics from Firebase RTDB (source of truth)
     */
    private void loadStatisticsFromFirebase(StatsAdapter adapter) {
        if (firebaseRTDBHelper == null) {
            android.util.Log.w("NurseDashboard", "⚠️ FirebaseRTDBHelper is null, using SQLite fallback");
            loadStatisticsFromSQLite(adapter);
            return;
        }
        
        // Fetch patients from Firebase
        firebaseRTDBHelper.getAllPatients(patients -> {
            int totalPatients = patients != null ? patients.size() : 0;
            int monitoringCount = totalPatients; // All patients are being monitored
            
            // Fetch prescriptions from Firebase (both from prescriptions and history paths)
            firebaseRTDBHelper.getAllPrescriptions(prescriptions -> {
                int prescriptionCount = prescriptions != null ? prescriptions.size() : 0;
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        String[] statsLabels = {"Total Patients", "Monitoring", "Doctor's Prescription"};
                        String[] statsValues = {
                            String.valueOf(totalPatients),
                            String.valueOf(monitoringCount),
                            String.valueOf(prescriptionCount)
                        };
                        
                        // Update adapter with accurate values
                        adapter.updateValues(statsValues);
                        
                        android.util.Log.d("NurseDashboard", "📊 Statistics updated from Firebase:");
                        android.util.Log.d("NurseDashboard", "   Total Patients: " + totalPatients);
                        android.util.Log.d("NurseDashboard", "   Monitoring: " + monitoringCount);
                        android.util.Log.d("NurseDashboard", "   Prescriptions: " + prescriptionCount);
                    });
                }
            });
        });
    }
    
    /**
     * Fallback: Load statistics from SQLite if Firebase is not available
     */
    private void loadStatisticsFromSQLite(StatsAdapter adapter) {
        int totalPatients = databaseHelper.getTotalPatientsCount();
        int monitoringCount = totalPatients; // All patients are being monitored
        int prescriptionCount = databaseHelper.getPrescriptionsCount();
        
        String[] statsLabels = {"Total Patients", "Monitoring", "Doctor's Prescription"};
        String[] statsValues = {
            String.valueOf(totalPatients),
            String.valueOf(monitoringCount),
            String.valueOf(prescriptionCount)
        };
        
        adapter.updateValues(statsValues);
    }

    // Interface for item click listener
    public interface OnItemClickListener {
        void onItemClick(int position, String label);
    }

    // Simple RecyclerView adapter for stats cards
    private class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.StatsViewHolder> {
        private String[] labels;
        private String[] values;
        private int[] colors;
        private OnItemClickListener listener;

        public StatsAdapter(String[] labels, String[] values, int[] colors, OnItemClickListener listener) {
            this.labels = labels;
            this.values = values;
            this.colors = colors;
            this.listener = listener;
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
            
            // Check if loading (empty value)
            boolean isLoading = values[position] == null || values[position].isEmpty() || values[position].equals("Loading...");
            
            if (isLoading) {
                // Show loading indicator, hide value text
                holder.valueText.setVisibility(View.GONE);
                holder.loadingIndicator.setVisibility(View.VISIBLE);
            } else {
                // Show value, hide loading indicator
                holder.valueText.setText(values[position]);
                holder.valueText.setVisibility(View.VISIBLE);
                holder.loadingIndicator.setVisibility(View.GONE);
            }
            
            holder.cardView.setCardBackgroundColor(getContext().getColor(colors[position]));

            // Set click listener (disable during loading)
            holder.cardView.setOnClickListener(isLoading ? null : v -> {
                if (listener != null) {
                    listener.onItemClick(position, labels[position]);
                }
            });
        }

        @Override
        public int getItemCount() {
            return labels.length;
        }
        
        /**
         * Update the values displayed in the stats cards
         */
        public void updateValues(String[] newValues) {
            if (newValues != null && newValues.length == values.length) {
                this.values = newValues;
                notifyDataSetChanged();
            }
        }

        class StatsViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView;
            TextView labelText;
            TextView valueText;
            ProgressBar loadingIndicator;

            public StatsViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.statCardView);
                labelText = itemView.findViewById(R.id.statLabelText);
                valueText = itemView.findViewById(R.id.statValueText);
                loadingIndicator = itemView.findViewById(R.id.statLoadingIndicator);
            }
        }
    }
}
