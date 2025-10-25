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

/**
 * DoctorDashboardFragment displays the main dashboard for doctors
 * with medical statistics and quick access to medical functions.
 */
public class DoctorDashboardFragment extends Fragment {

    private RecyclerView statsRecyclerView;
    private TextView welcomeTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_dashboard, container, false);
        
        initializeViews(view);
        setupStatsRecyclerView();
        
        return view;
    }

    private void initializeViews(View view) {
        statsRecyclerView = view.findViewById(R.id.statsRecyclerView);
        welcomeTextView = view.findViewById(R.id.welcomeTextView);
        
        welcomeTextView.setText("Welcome to Doctor Dashboard");
    }

    private void setupStatsRecyclerView() {
        // Create doctor-specific stats data
        String[] statsLabels = {"Active Patients", "Today's Appointments", "Pending Diagnoses", "Prescriptions Written", "Emergency Cases", "Follow-ups Due"};
        String[] statsValues = {"12", "8", "3", "15", "2", "5"};
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









