package com.example.h_cas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import com.example.h_cas.database.HCasDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * PharmacyReportsFragment provides comprehensive reports and analytics for pharmacists
 */
public class PharmacyReportsFragment extends Fragment {

    private RecyclerView reportsRecyclerView;
    private TextView emptyStateText;
    private MaterialButton generateReportButton;
    private MaterialButton exportDataButton;
    private MaterialButton refreshReportsButton;
    
    private HCasDatabaseHelper databaseHelper;
    private ReportsAdapter reportsAdapter;
    private List<ReportItem> reportItems;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pharmacy_reports, container, false);
        
        initializeViews(view);
        initializeDatabase();
        setupRecyclerView();
        setupClickListeners();
        loadReports();
        
        return view;
    }

    private void initializeViews(View view) {
        reportsRecyclerView = view.findViewById(R.id.reportsRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        generateReportButton = view.findViewById(R.id.generateReportButton);
        exportDataButton = view.findViewById(R.id.exportDataButton);
        refreshReportsButton = view.findViewById(R.id.refreshReportsButton);
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupRecyclerView() {
        reportItems = new ArrayList<>();
        reportsAdapter = new ReportsAdapter(reportItems);
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        reportsRecyclerView.setAdapter(reportsAdapter);
    }

    private void setupClickListeners() {
        generateReportButton.setOnClickListener(v -> generateNewReport());
        exportDataButton.setOnClickListener(v -> exportReportsData());
        refreshReportsButton.setOnClickListener(v -> loadReports());
    }

    private void loadReports() {
        reportItems.clear();
        
        // Add sample reports
        reportItems.add(new ReportItem(
            "Daily Dispensing Report",
            "Medications dispensed today: 24 items",
            "Today",
            "📊",
            "Daily dispensing summary with patient details and medication information."
        ));
        
        reportItems.add(new ReportItem(
            "Low Stock Alert",
            "3 medicines are running low on stock",
            "Active",
            "⚠️",
            "Medicines with stock levels below 10 units: Amoxicillin, Loratadine, Metformin."
        ));
        
        reportItems.add(new ReportItem(
            "Expiry Report",
            "5 medicines expiring within 30 days",
            "This Month",
            "📅",
            "Medicines approaching expiry date requiring immediate attention."
        ));
        
        reportItems.add(new ReportItem(
            "Prescription Analysis",
            "Most prescribed: Paracetamol (45%), Ibuprofen (30%)",
            "This Week",
            "📈",
            "Analysis of prescription patterns and popular medications."
        ));
        
        reportItems.add(new ReportItem(
            "Inventory Value",
            "Total inventory value: ₱125,450.00",
            "Current",
            "💰",
            "Current market value of all medicines in stock."
        ));
        
        reportItems.add(new ReportItem(
            "Patient Compliance",
            "85% of patients collected their medications",
            "This Month",
            "✅",
            "Patient medication collection rate and compliance tracking."
        ));
        
        reportsAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (reportItems.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            reportsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            reportsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void generateNewReport() {
        // In a real implementation, this would generate a new report
        android.widget.Toast.makeText(getContext(), "🔄 Generating new report...", android.widget.Toast.LENGTH_SHORT).show();
        
        // Simulate adding a new report
        reportItems.add(0, new ReportItem(
            "Custom Report",
            "Generated report with current data",
            "Just Now",
            "📋",
            "Custom report generated based on current pharmacy data and metrics."
        ));
        
        reportsAdapter.notifyItemInserted(0);
        reportsRecyclerView.scrollToPosition(0);
    }

    private void exportReportsData() {
        // In a real implementation, this would export data to CSV/PDF
        android.widget.Toast.makeText(getContext(), "📤 Exporting reports data...", android.widget.Toast.LENGTH_SHORT).show();
    }

    // Report Item class
    private static class ReportItem {
        private String title;
        private String summary;
        private String timeFrame;
        private String icon;
        private String description;

        public ReportItem(String title, String summary, String timeFrame, String icon, String description) {
            this.title = title;
            this.summary = summary;
            this.timeFrame = timeFrame;
            this.icon = icon;
            this.description = description;
        }

        public String getTitle() { return title; }
        public String getSummary() { return summary; }
        public String getTimeFrame() { return timeFrame; }
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
    }

    // RecyclerView Adapter for reports
    private class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ReportViewHolder> {
        private List<ReportItem> reports;

        public ReportsAdapter(List<ReportItem> reports) {
            this.reports = reports;
        }

        @NonNull
        @Override
        public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
            return new ReportViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
            ReportItem report = reports.get(position);
            holder.bind(report);
        }

        @Override
        public int getItemCount() {
            return reports.size();
        }

        class ReportViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView cardView;
            private TextView reportTitleText;
            private TextView reportSummaryText;
            private TextView reportTimeFrameText;
            private TextView reportDescriptionText;

            public ReportViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.reportCardView);
                reportTitleText = itemView.findViewById(R.id.reportTitleText);
                reportSummaryText = itemView.findViewById(R.id.reportSummaryText);
                reportTimeFrameText = itemView.findViewById(R.id.reportTimeFrameText);
                reportDescriptionText = itemView.findViewById(R.id.reportDescriptionText);
            }

            public void bind(ReportItem report) {
                // Set title
                reportTitleText.setText(report.getTitle());
                
                // Set time frame (subtitle)
                if (report.getTimeFrame() != null && !report.getTimeFrame().isEmpty()) {
                    reportTimeFrameText.setText(report.getTimeFrame());
                    reportTimeFrameText.setVisibility(View.VISIBLE);
                } else {
                    reportTimeFrameText.setVisibility(View.GONE);
                }
                
                // Set summary (description)
                if (report.getSummary() != null && !report.getSummary().isEmpty()) {
                    reportSummaryText.setText(report.getSummary());
                    reportSummaryText.setVisibility(View.VISIBLE);
                } else {
                    reportSummaryText.setVisibility(View.GONE);
                }
                
                // Set description (details)
                if (report.getDescription() != null && !report.getDescription().isEmpty()) {
                    reportDescriptionText.setText(report.getDescription());
                    reportDescriptionText.setVisibility(View.VISIBLE);
                } else {
                    reportDescriptionText.setVisibility(View.GONE);
                }
            }
        }
    }
}







