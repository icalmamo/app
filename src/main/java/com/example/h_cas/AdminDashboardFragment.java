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

import com.example.h_cas.database.HCasDatabaseHelper;

/**
 * AdminDashboardFragment displays the main dashboard with system overview
 * and quick access to common administrative tasks.
 */
public class AdminDashboardFragment extends Fragment {

    private RecyclerView statsRecyclerView;
    private TextView welcomeTextView;
    private HCasDatabaseHelper databaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
        
        initializeViews(view);
        setupStatsRecyclerView();
        
        return view;
    }

    private void initializeViews(View view) {
        statsRecyclerView = view.findViewById(R.id.statsRecyclerView);
        welcomeTextView = view.findViewById(R.id.welcomeTextView);
        
        welcomeTextView.setText("Welcome to H-CAS Admin Dashboard");
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupStatsRecyclerView() {
        // Get real data from database
        int totalEmployees = databaseHelper.getTotalEmployeesCount();
        int nurses = databaseHelper.getEmployeesCountByRole("Nurse");
        int doctors = databaseHelper.getEmployeesCountByRole("Doctor");
        int pharmacists = databaseHelper.getEmployeesCountByRole("Pharmacist");
        int todaysCases = databaseHelper.getTodaysCasesCount();
        int pendingReviews = databaseHelper.getPendingReviewsCount();
        
        // Create stats data from database
        String[] statsLabels = {"Total Employees", "Active Nurses", "Doctors", "Pharmacists", "Today's Cases", "Pending Reviews"};
        String[] statsValues = {
            String.valueOf(totalEmployees),
            String.valueOf(nurses),
            String.valueOf(doctors),
            String.valueOf(pharmacists),
            String.valueOf(todaysCases),
            String.valueOf(pendingReviews)
        };
        int[] statsColors = {R.color.primary_blue, R.color.success_green, R.color.warning_orange, R.color.accent_blue, R.color.error_red, R.color.text_secondary};

        StatsAdapter adapter = new StatsAdapter(statsLabels, statsValues, statsColors);
        statsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        statsRecyclerView.setAdapter(adapter);
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
