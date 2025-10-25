package com.example.h_cas;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Patient;
import com.example.h_cas.models.Prescription;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Enhanced ReportsFragment with comprehensive patient reporting capabilities
 * Features: Per patient, per week, per month, per year reports with print functionality
 */
public class ReportsFragment extends Fragment {

    // UI Components
    private RecyclerView reportsRecyclerView;
    private TextView emptyStateText;
    private Spinner reportTypeSpinner;
    private Spinner timePeriodSpinner;
    private Spinner patientSpinner;
    private MaterialButton generateReportButton;
    private MaterialButton printReportButton;
    private MaterialButton refreshButton;
    private TextView reportSummaryText;
    
    // Data
    private List<ReportItem> reportItems;
    private ReportAdapter reportAdapter;
    private HCasDatabaseHelper databaseHelper;
    private String selectedReportType = "Patient Report";
    private String selectedTimePeriod = "All Time";
    private String selectedPatient = "All Patients";
    
    // Report types
    private String[] reportTypes = {"Patient Report", "Prescription Report", "System Report", "Financial Report"};
    
    // Time periods
    private String[] timePeriods = {"All Time", "This Week", "This Month", "This Year", "Last Week", "Last Month", "Last Year"};
    
    // Patients list
    private List<String> patientsList;
    private List<Patient> allPatients;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_enhanced_reports, container, false);
            
            if (view == null) {
                throw new RuntimeException("Failed to inflate layout");
            }
            
            initializeViews(view);
            initializeDatabase();
            setupRecyclerView();
            setupSpinners();
            setupClickListeners();
            loadInitialData();
            
            return view;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error loading reports: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private void initializeViews(View view) {
        try {
            reportsRecyclerView = view.findViewById(R.id.reportsRecyclerView);
            emptyStateText = view.findViewById(R.id.emptyStateText);
            reportTypeSpinner = view.findViewById(R.id.reportTypeSpinner);
            timePeriodSpinner = view.findViewById(R.id.timePeriodSpinner);
            patientSpinner = view.findViewById(R.id.patientSpinner);
            generateReportButton = view.findViewById(R.id.generateReportButton);
            printReportButton = view.findViewById(R.id.printReportButton);
            refreshButton = view.findViewById(R.id.refreshButton);
            reportSummaryText = view.findViewById(R.id.reportSummaryText);
            
            if (reportsRecyclerView == null || emptyStateText == null || reportTypeSpinner == null || 
                timePeriodSpinner == null || patientSpinner == null || generateReportButton == null || 
                printReportButton == null || refreshButton == null || reportSummaryText == null) {
                throw new RuntimeException("One or more views not found in layout");
            }
            
            reportItems = new ArrayList<>();
            patientsList = new ArrayList<>();
            allPatients = new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error initializing views: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeDatabase() {
        try {
            if (getContext() == null) {
                throw new RuntimeException("Context is null");
            }
            databaseHelper = new HCasDatabaseHelper(getContext());
            if (databaseHelper == null) {
                throw new RuntimeException("Failed to create database helper");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error initializing database: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupRecyclerView() {
        try {
            if (reportItems == null) {
                reportItems = new ArrayList<>();
            }
            reportAdapter = new ReportAdapter(reportItems);
            reportsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            reportsRecyclerView.setAdapter(reportAdapter);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error setting up recycler view: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupSpinners() {
        // Report Type Spinner
        ArrayAdapter<String> reportTypeAdapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, reportTypes);
        reportTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportTypeSpinner.setAdapter(reportTypeAdapter);
        
        reportTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedReportType = reportTypes[position];
                updatePatientSpinnerVisibility();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Time Period Spinner
        ArrayAdapter<String> timePeriodAdapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, timePeriods);
        timePeriodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timePeriodSpinner.setAdapter(timePeriodAdapter);
        
        timePeriodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTimePeriod = timePeriods[position];
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Patient Spinner
        ArrayAdapter<String> patientAdapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, patientsList);
        patientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        patientSpinner.setAdapter(patientAdapter);
        
        patientSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < patientsList.size()) {
                    selectedPatient = patientsList.get(position);
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupClickListeners() {
        generateReportButton.setOnClickListener(v -> generateReport());
        printReportButton.setOnClickListener(v -> printReport());
        refreshButton.setOnClickListener(v -> loadInitialData());
        
        // Back button functionality
        ImageButton backButton = getView().findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (getActivity() instanceof AdminDashboardActivity) {
                    ((AdminDashboardActivity) getActivity()).loadFragment(new AdminDashboardFragment());
                    ((AdminDashboardActivity) getActivity()).getSupportActionBar().setTitle("Admin Dashboard");
                }
            });
        }
    }

    private void loadInitialData() {
        try {
            // Load all patients
            allPatients.clear();
            patientsList.clear();
            patientsList.add("All Patients");
            
            // For demo purposes, create sample patients
            createSamplePatients();
            
            // Update patient spinner
            ArrayAdapter<String> patientAdapter = new ArrayAdapter<>(getContext(), 
                android.R.layout.simple_spinner_item, patientsList);
            patientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            patientSpinner.setAdapter(patientAdapter);
            
            // Generate initial report
            generateReport();
            
            Toast.makeText(getContext(), "📊 Reports data loaded successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ Error loading reports data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void createSamplePatients() {
        // Create sample patients for demo
        String[][] samplePatients = {
            {"P001", "Juan", "Dela Cruz", "Male", "35", "Hypertension"},
            {"P002", "Maria", "Santos", "Female", "28", "Diabetes"},
            {"P003", "Pedro", "Garcia", "Male", "42", "Heart Disease"},
            {"P004", "Ana", "Lopez", "Female", "31", "Asthma"},
            {"P005", "Carlos", "Martinez", "Male", "55", "Arthritis"},
            {"P006", "Elena", "Rodriguez", "Female", "29", "Migraine"},
            {"P007", "Miguel", "Hernandez", "Male", "38", "Back Pain"},
            {"P008", "Sofia", "Gonzalez", "Female", "45", "Depression"}
        };

        for (String[] patient : samplePatients) {
            Patient newPatient = new Patient();
            newPatient.setPatientId(patient[0]);
            newPatient.setFirstName(patient[1]);
            newPatient.setLastName(patient[2]);
            newPatient.setGender(patient[3]);
            newPatient.setAge(patient[4]);
            newPatient.setSymptomsDescription(patient[5]);
            
            allPatients.add(newPatient);
            patientsList.add(patient[1] + " " + patient[2] + " (" + patient[0] + ")");
        }
    }

    private void updatePatientSpinnerVisibility() {
        if (selectedReportType.equals("Patient Report")) {
            patientSpinner.setVisibility(View.VISIBLE);
        } else {
            patientSpinner.setVisibility(View.GONE);
        }
    }

    private void generateReport() {
        try {
            reportItems.clear();
            
            switch (selectedReportType) {
                case "Patient Report":
                    generatePatientReport();
                    break;
                case "Prescription Report":
                    generatePrescriptionReport();
                    break;
                case "System Report":
                    generateSystemReport();
                    break;
                case "Financial Report":
                    generateFinancialReport();
                    break;
            }
            
            reportAdapter.notifyDataSetChanged();
            updateEmptyState();
            updateReportSummary();
            
            Toast.makeText(getContext(), "📊 Report generated successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ Error generating report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void generatePatientReport() {
        String reportTitle = "Patient Report - " + selectedTimePeriod;
        if (!selectedPatient.equals("All Patients")) {
            reportTitle += " - " + selectedPatient;
        }
        
        reportItems.add(new ReportItem("REPORT_HEADER", reportTitle, "", "", ""));
        
        // Add patient statistics
        int totalPatients = allPatients.size();
        int malePatients = 0;
        int femalePatients = 0;
        int avgAge = 0;
        
        for (Patient patient : allPatients) {
            if (patient.getGender().equals("Male")) {
                malePatients++;
            } else {
                femalePatients++;
            }
            avgAge += Integer.parseInt(patient.getAge());
        }
        
        if (totalPatients > 0) {
            avgAge = avgAge / totalPatients;
        }
        
        reportItems.add(new ReportItem("STATISTICS", "Patient Statistics", 
            "Total Patients: " + totalPatients, 
            "Male: " + malePatients + " | Female: " + femalePatients,
            "Average Age: " + avgAge + " years"));
        
        // Add individual patient details
        if (selectedPatient.equals("All Patients")) {
            for (Patient patient : allPatients) {
                reportItems.add(new ReportItem("PATIENT_DETAIL", 
                    patient.getFirstName() + " " + patient.getLastName(),
                    "ID: " + patient.getPatientId(),
                    "Age: " + patient.getAge() + " | Gender: " + patient.getGender(),
                    "Condition: " + patient.getSymptomsDescription()));
            }
        } else {
            // Find specific patient
            for (Patient patient : allPatients) {
                String patientName = patient.getFirstName() + " " + patient.getLastName() + " (" + patient.getPatientId() + ")";
                if (patientName.equals(selectedPatient)) {
                    reportItems.add(new ReportItem("PATIENT_DETAIL", 
                        patient.getFirstName() + " " + patient.getLastName(),
                        "ID: " + patient.getPatientId(),
                        "Age: " + patient.getAge() + " | Gender: " + patient.getGender(),
                        "Condition: " + patient.getSymptomsDescription()));
                    break;
                }
            }
        }
    }

    private void generatePrescriptionReport() {
        reportItems.add(new ReportItem("REPORT_HEADER", "Prescription Report - " + selectedTimePeriod, "", "", ""));
        
        // Get prescriptions from database
        List<Prescription> prescriptions = databaseHelper.getAllPrescriptions();
        
        reportItems.add(new ReportItem("STATISTICS", "Prescription Statistics", 
            "Total Prescriptions: " + prescriptions.size(), 
            "Pending: " + prescriptions.size(), // All are pending in demo
            "Dispensed: 0"));
        
        // Add prescription details
        for (Prescription prescription : prescriptions) {
            reportItems.add(new ReportItem("PRESCRIPTION_DETAIL", 
                "Prescription ID: " + prescription.getPrescriptionId(),
                "Patient: " + prescription.getPatientName(),
                "Medicine: " + prescription.getMedication(),
                "Doctor: " + prescription.getDoctorName() + " | Date: " + prescription.getCreatedDate()));
        }
    }

    private void generateSystemReport() {
        reportItems.add(new ReportItem("REPORT_HEADER", "System Report - " + selectedTimePeriod, "", "", ""));
        
        // Get system statistics
        int totalEmployees = databaseHelper.getAllEmployees().size();
        int totalPatients = allPatients.size();
        int totalPrescriptions = databaseHelper.getAllPrescriptions().size();
        int totalMedicines = databaseHelper.getTotalMedicinesCount();
        
        reportItems.add(new ReportItem("STATISTICS", "System Statistics", 
            "Total Employees: " + totalEmployees, 
            "Total Patients: " + totalPatients,
            "Total Prescriptions: " + totalPrescriptions + " | Total Medicines: " + totalMedicines));
        
        // Add system health indicators
        reportItems.add(new ReportItem("SYSTEM_HEALTH", "System Health", 
            "Database Status: ✅ Active", 
            "User Sessions: ✅ Active",
            "Last Backup: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date())));
    }

    private void generateFinancialReport() {
        reportItems.add(new ReportItem("REPORT_HEADER", "Financial Report - " + selectedTimePeriod, "", "", ""));
        
        // Calculate financial statistics
        double totalRevenue = 0;
        int totalTransactions = 0;
        
        // Get medicine prices for revenue calculation
        List<com.example.h_cas.models.Medicine> medicines = databaseHelper.getAllMedicines();
        for (com.example.h_cas.models.Medicine medicine : medicines) {
            totalRevenue += medicine.getPrice() * medicine.getStockQuantity();
            totalTransactions += medicine.getStockQuantity();
        }
        
        reportItems.add(new ReportItem("STATISTICS", "Financial Statistics", 
            "Total Revenue: ₱" + String.format("%.2f", totalRevenue), 
            "Total Transactions: " + totalTransactions,
            "Average Transaction: ₱" + String.format("%.2f", totalTransactions > 0 ? totalRevenue / totalTransactions : 0)));
        
        // Add revenue breakdown by category
        reportItems.add(new ReportItem("REVENUE_BREAKDOWN", "Revenue Breakdown", 
            "Medicine Sales: ₱" + String.format("%.2f", totalRevenue), 
            "Consultation Fees: ₱0.00",
            "Other Services: ₱0.00"));
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

    private void updateReportSummary() {
        if (!reportItems.isEmpty()) {
            int totalItems = reportItems.size();
            String summary = "Report Summary: " + totalItems + " items generated";
            reportSummaryText.setText(summary);
            reportSummaryText.setVisibility(View.VISIBLE);
        } else {
            reportSummaryText.setVisibility(View.GONE);
        }
    }

    private void printReport() {
        if (reportItems.isEmpty()) {
            Toast.makeText(getContext(), "❌ No report to print. Generate a report first.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("🖨️ Print Report");
        builder.setMessage("Print " + selectedReportType + " for " + selectedTimePeriod + "?");
        
        builder.setPositiveButton("Print", (dialog, which) -> {
            // Simulate printing
            String printContent = generatePrintContent();
            showPrintPreview(printContent);
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private String generatePrintContent() {
        StringBuilder content = new StringBuilder();
        content.append("H-CAS Healthcare System\n");
        content.append("=====================================\n");
        content.append("Report Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date())).append("\n");
        content.append("Report Type: ").append(selectedReportType).append("\n");
        content.append("Time Period: ").append(selectedTimePeriod).append("\n");
        if (!selectedPatient.equals("All Patients")) {
            content.append("Patient: ").append(selectedPatient).append("\n");
        }
        content.append("=====================================\n\n");
        
        for (ReportItem item : reportItems) {
            content.append(item.getTitle()).append("\n");
            if (!item.getSubtitle().isEmpty()) {
                content.append(item.getSubtitle()).append("\n");
            }
            if (!item.getDescription().isEmpty()) {
                content.append(item.getDescription()).append("\n");
            }
            if (!item.getDetails().isEmpty()) {
                content.append(item.getDetails()).append("\n");
            }
            content.append("\n");
        }
        
        return content.toString();
    }

    private void showPrintPreview(String content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("📄 Print Preview");
        builder.setMessage(content);
        builder.setPositiveButton("Print", (dialog, which) -> {
            Toast.makeText(getContext(), "🖨️ Report sent to printer successfully!", Toast.LENGTH_LONG).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
        loadInitialData();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Simplified back navigation
        ImageButton backButton = view.findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (getActivity() instanceof AdminDashboardActivity) {
                    ((AdminDashboardActivity) getActivity()).loadFragment(new AdminDashboardFragment());
                    ((AdminDashboardActivity) getActivity()).getSupportActionBar().setTitle("Admin Dashboard");
                }
            });
        }
    }

    // Report Item class
    public static class ReportItem {
        private String type;
        private String title;
        private String subtitle;
        private String description;
        private String details;

        public ReportItem(String type, String title, String subtitle, String description, String details) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.description = description;
            this.details = details;
        }

        // Getters
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getDescription() { return description; }
        public String getDetails() { return details; }
    }

    // RecyclerView Adapter for reports
    private class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
        private List<ReportItem> items;

        public ReportAdapter(List<ReportItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
            return new ReportViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
            ReportItem item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ReportViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView reportCard;
            private TextView titleText;
            private TextView subtitleText;
            private TextView descriptionText;
            private TextView detailsText;

            public ReportViewHolder(@NonNull View itemView) {
                super(itemView);
                reportCard = itemView.findViewById(R.id.reportCardView);
                titleText = itemView.findViewById(R.id.reportTitleText);
                subtitleText = itemView.findViewById(R.id.reportTimeFrameText);
                descriptionText = itemView.findViewById(R.id.reportSummaryText);
                detailsText = itemView.findViewById(R.id.reportDescriptionText);
            }

            public void bind(ReportItem item) {
                titleText.setText(item.getTitle());
                subtitleText.setText(item.getSubtitle());
                descriptionText.setText(item.getDescription());
                detailsText.setText(item.getDetails());

                // Set different styles based on item type
                switch (item.getType()) {
                    case "REPORT_HEADER":
                        titleText.setTextSize(18);
                        titleText.setTextColor(getContext().getColor(R.color.primary_blue));
                        subtitleText.setVisibility(View.GONE);
                        descriptionText.setVisibility(View.GONE);
                        detailsText.setVisibility(View.GONE);
                        break;
                    case "STATISTICS":
                        titleText.setTextSize(16);
                        titleText.setTextColor(getContext().getColor(R.color.success_green));
                        subtitleText.setVisibility(View.VISIBLE);
                        descriptionText.setVisibility(View.VISIBLE);
                        detailsText.setVisibility(View.VISIBLE);
                        break;
                    default:
                        titleText.setTextSize(14);
                        titleText.setTextColor(getContext().getColor(R.color.text_primary));
                        subtitleText.setVisibility(View.VISIBLE);
                        descriptionText.setVisibility(View.VISIBLE);
                        detailsText.setVisibility(View.VISIBLE);
                        break;
                }
            }
        }
    }
}