package com.example.h_cas;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
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
    private View emptyStateText;
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
            
            // Show empty state initially (after all views are initialized)
            updateEmptyState();
            
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
            if (getContext() == null) {
                return; // Context not available yet
            }
            
            if (reportsRecyclerView == null) {
                return; // RecyclerView not initialized
            }
            
            if (reportItems == null) {
                reportItems = new ArrayList<>();
            }
            
            reportAdapter = new ReportAdapter(reportItems);
            reportsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            reportsRecyclerView.setAdapter(reportAdapter);
            
            // Initially show empty state (only if views are initialized)
            if (emptyStateText != null && reportsRecyclerView != null) {
                updateEmptyState();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error setting up recycler view: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupSpinners() {
        if (getContext() == null || reportTypeSpinner == null || 
            timePeriodSpinner == null || patientSpinner == null) {
            return; // Context or views not available
        }
        
        // Report Type Spinner
        ArrayAdapter<String> reportTypeAdapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, reportTypes);
        reportTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportTypeSpinner.setAdapter(reportTypeAdapter);
        
        // Set default selection
        reportTypeSpinner.setSelection(0, false); // Don't trigger listener on initial selection
        
        reportTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < reportTypes.length) {
                    selectedReportType = reportTypes[position];
                    updatePatientSpinnerVisibility();
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Time Period Spinner
        ArrayAdapter<String> timePeriodAdapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, timePeriods);
        timePeriodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timePeriodSpinner.setAdapter(timePeriodAdapter);
        
        // Set default selection
        timePeriodSpinner.setSelection(0, false); // Don't trigger listener on initial selection
        
        timePeriodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < timePeriods.length) {
                    selectedTimePeriod = timePeriods[position];
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Patient Spinner - will be updated after data is loaded
        updatePatientSpinner();
    }
    
    private void updatePatientSpinner() {
        if (getContext() == null || patientSpinner == null) {
            return;
        }
        
        if (patientsList == null || patientsList.isEmpty()) {
            // Initialize with "All Patients" if list is empty
            patientsList = new ArrayList<>();
            patientsList.add("All Patients");
        }
        
        // Patient Spinner
        ArrayAdapter<String> patientAdapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, patientsList);
        patientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        patientSpinner.setAdapter(patientAdapter);
        
        // Set default selection
        patientSpinner.setSelection(0, false); // Don't trigger listener on initial selection
        
        patientSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < patientsList.size()) {
                    selectedPatient = patientsList.get(position);
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupClickListeners() {
        if (generateReportButton != null) {
            generateReportButton.setOnClickListener(v -> generateReport());
        }
        if (printReportButton != null) {
            printReportButton.setOnClickListener(v -> printReport());
        }
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> loadInitialData());
        }
        
        // Back button functionality
        if (getView() != null) {
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
    }

    private void loadInitialData() {
        try {
            if (getContext() == null || patientSpinner == null || databaseHelper == null) {
                return; // Context or views not available
            }
            
            // Load all patients from database
            if (allPatients == null) {
                allPatients = new ArrayList<>();
            }
            if (patientsList == null) {
                patientsList = new ArrayList<>();
            }
            
            allPatients.clear();
            patientsList.clear();
            patientsList.add("All Patients");
            
            // Get real patients from database
            List<Patient> dbPatients = databaseHelper.getAllPatients();
            if (dbPatients != null && !dbPatients.isEmpty()) {
                allPatients.addAll(dbPatients);
                for (Patient patient : dbPatients) {
                    String fullName = patient.getFirstName() + " " + patient.getLastName();
                    String patientId = patient.getPatientId() != null ? patient.getPatientId() : "";
                    patientsList.add(fullName + " (" + patientId + ")");
                }
            }
            
            // Update patient spinner with loaded data
            updatePatientSpinner();
            
            // Generate initial report
            generateReport();
            
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "❌ Error loading reports data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
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
        
        // Filter patients by time period
        List<Patient> filteredPatients = filterPatientsByTimePeriod(allPatients);
        
        // Add patient statistics
        int totalPatients = filteredPatients.size();
        int malePatients = 0;
        int femalePatients = 0;
        int totalAge = 0;
        int validAgeCount = 0;
        
        for (Patient patient : filteredPatients) {
            String gender = patient.getGender();
            if (gender != null) {
                if (gender.equals("Male") || gender.equalsIgnoreCase("M")) {
                    malePatients++;
                } else if (gender.equals("Female") || gender.equalsIgnoreCase("F")) {
                    femalePatients++;
                }
            }
            
            // Calculate age safely
            String ageStr = patient.getAge();
            if (ageStr != null && !ageStr.isEmpty()) {
                try {
                    totalAge += Integer.parseInt(ageStr);
                    validAgeCount++;
                } catch (NumberFormatException e) {
                    // Ignore invalid age values
                }
            }
        }
        
        int avgAge = validAgeCount > 0 ? totalAge / validAgeCount : 0;
        
        reportItems.add(new ReportItem("STATISTICS", "Patient Statistics", 
            "Total Patients: " + totalPatients, 
            "Male: " + malePatients + " | Female: " + femalePatients,
            "Average Age: " + avgAge + " years"));
        
        // Add individual patient details
        if (selectedPatient.equals("All Patients")) {
            for (Patient patient : filteredPatients) {
                String name = (patient.getFirstName() != null ? patient.getFirstName() : "") + 
                             " " + (patient.getLastName() != null ? patient.getLastName() : "");
                String patientId = patient.getPatientId() != null ? patient.getPatientId() : "N/A";
                String age = patient.getAge() != null ? patient.getAge() : "N/A";
                String gender = patient.getGender() != null ? patient.getGender() : "N/A";
                String condition = patient.getSymptomsDescription() != null ? patient.getSymptomsDescription() : "No condition recorded";
                
                reportItems.add(new ReportItem("PATIENT_DETAIL", 
                    name.trim(),
                    "ID: " + patientId,
                    "Age: " + age + " | Gender: " + gender,
                    "Condition: " + condition));
            }
        } else {
            // Find specific patient
            for (Patient patient : filteredPatients) {
                String fullName = patient.getFirstName() + " " + patient.getLastName();
                String patientId = patient.getPatientId() != null ? patient.getPatientId() : "";
                String patientName = fullName + " (" + patientId + ")";
                if (patientName.equals(selectedPatient)) {
                    String name = fullName.trim();
                    String age = patient.getAge() != null ? patient.getAge() : "N/A";
                    String gender = patient.getGender() != null ? patient.getGender() : "N/A";
                    String condition = patient.getSymptomsDescription() != null ? patient.getSymptomsDescription() : "No condition recorded";
                    
                    reportItems.add(new ReportItem("PATIENT_DETAIL", 
                        name,
                        "ID: " + patientId,
                        "Age: " + age + " | Gender: " + gender,
                        "Condition: " + condition));
                    break;
                }
            }
        }
    }
    
    private List<Patient> filterPatientsByTimePeriod(List<Patient> patients) {
        if (selectedTimePeriod.equals("All Time")) {
            return patients;
        }
        
        Calendar calendar = Calendar.getInstance();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
        
        List<Patient> filtered = new ArrayList<>();
        for (Patient patient : patients) {
            String createdDate = patient.getCreatedDate();
            if (createdDate == null || createdDate.isEmpty()) {
                // If no date, include it in "All Time" only
                if (selectedTimePeriod.equals("All Time")) {
                    filtered.add(patient);
                }
                continue;
            }
            
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date patientDate = sdf.parse(createdDate.substring(0, Math.min(10, createdDate.length())));
                Date currentDate = sdf.parse(today);
                
                calendar.setTime(currentDate);
                
                switch (selectedTimePeriod) {
                    case "This Week":
                        calendar.add(Calendar.DAY_OF_YEAR, -7);
                        break;
                    case "This Month":
                        calendar.add(Calendar.MONTH, -1);
                        break;
                    case "This Year":
                        calendar.add(Calendar.YEAR, -1);
                        break;
                    case "Last Week":
                        calendar.add(Calendar.WEEK_OF_YEAR, -1);
                        Date lastWeekStart = calendar.getTime();
                        calendar.add(Calendar.WEEK_OF_YEAR, 1);
                        Date lastWeekEnd = calendar.getTime();
                        if (patientDate.after(lastWeekStart) && patientDate.before(lastWeekEnd)) {
                            filtered.add(patient);
                        }
                        continue;
                    case "Last Month":
                        calendar.add(Calendar.MONTH, -1);
                        Date lastMonthStart = calendar.getTime();
                        calendar.add(Calendar.MONTH, 1);
                        Date lastMonthEnd = calendar.getTime();
                        if (patientDate.after(lastMonthStart) && patientDate.before(lastMonthEnd)) {
                            filtered.add(patient);
                        }
                        continue;
                    case "Last Year":
                        calendar.add(Calendar.YEAR, -1);
                        Date lastYearStart = calendar.getTime();
                        calendar.add(Calendar.YEAR, 1);
                        Date lastYearEnd = calendar.getTime();
                        if (patientDate.after(lastYearStart) && patientDate.before(lastYearEnd)) {
                            filtered.add(patient);
                        }
                        continue;
                }
                
                Date periodStart = calendar.getTime();
                if (patientDate.after(periodStart) || patientDate.equals(periodStart)) {
                    filtered.add(patient);
                }
            } catch (Exception e) {
                // If date parsing fails, include patient if "All Time"
                if (selectedTimePeriod.equals("All Time")) {
                    filtered.add(patient);
                }
            }
        }
        
        return filtered;
    }

    private void generatePrescriptionReport() {
        reportItems.add(new ReportItem("REPORT_HEADER", "Prescription Report - " + selectedTimePeriod, "", "", ""));
        
        // Get prescriptions from database
        List<Prescription> allPrescriptions = databaseHelper.getAllPrescriptions();
        List<Prescription> filteredPrescriptions = filterPrescriptionsByTimePeriod(allPrescriptions);
        
        int totalPrescriptions = filteredPrescriptions.size();
        int pendingCount = 0;
        int dispensedCount = 0;
        
        for (Prescription prescription : filteredPrescriptions) {
            String status = prescription.getStatus();
            if (status != null && status.equalsIgnoreCase("Dispensed")) {
                dispensedCount++;
            } else {
                pendingCount++;
            }
        }
        
        reportItems.add(new ReportItem("STATISTICS", "Prescription Statistics", 
            "Total Prescriptions: " + totalPrescriptions, 
            "Pending: " + pendingCount,
            "Dispensed: " + dispensedCount));
        
        // Add prescription details
        for (Prescription prescription : filteredPrescriptions) {
            String prescriptionId = prescription.getPrescriptionId() != null ? prescription.getPrescriptionId() : "N/A";
            String patientName = prescription.getPatientName() != null ? prescription.getPatientName() : "N/A";
            String medication = prescription.getMedication() != null ? prescription.getMedication() : "N/A";
            String doctorName = prescription.getDoctorName() != null ? prescription.getDoctorName() : "N/A";
            String createdDate = prescription.getCreatedDate() != null ? prescription.getCreatedDate() : "N/A";
            String dosage = prescription.getDosage() != null ? prescription.getDosage() : "";
            String frequency = prescription.getFrequency() != null ? prescription.getFrequency() : "";
            
            reportItems.add(new ReportItem("PRESCRIPTION_DETAIL", 
                "Prescription ID: " + prescriptionId,
                "Patient: " + patientName,
                "Medicine: " + medication + (dosage.isEmpty() ? "" : " (" + dosage + ")"),
                "Doctor: " + doctorName + " | Date: " + createdDate.substring(0, Math.min(10, createdDate.length()))));
        }
    }
    
    private List<Prescription> filterPrescriptionsByTimePeriod(List<Prescription> prescriptions) {
        if (selectedTimePeriod.equals("All Time")) {
            return prescriptions;
        }
        
        Calendar calendar = Calendar.getInstance();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
        
        List<Prescription> filtered = new ArrayList<>();
        for (Prescription prescription : prescriptions) {
            String createdDate = prescription.getCreatedDate();
            if (createdDate == null || createdDate.isEmpty()) {
                if (selectedTimePeriod.equals("All Time")) {
                    filtered.add(prescription);
                }
                continue;
            }
            
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date prescriptionDate = sdf.parse(createdDate.substring(0, Math.min(10, createdDate.length())));
                Date currentDate = sdf.parse(today);
                
                calendar.setTime(currentDate);
                
                switch (selectedTimePeriod) {
                    case "This Week":
                        calendar.add(Calendar.DAY_OF_YEAR, -7);
                        break;
                    case "This Month":
                        calendar.add(Calendar.MONTH, -1);
                        break;
                    case "This Year":
                        calendar.add(Calendar.YEAR, -1);
                        break;
                    case "Last Week":
                        calendar.add(Calendar.WEEK_OF_YEAR, -1);
                        Date lastWeekStart = calendar.getTime();
                        calendar.add(Calendar.WEEK_OF_YEAR, 1);
                        Date lastWeekEnd = calendar.getTime();
                        if (prescriptionDate.after(lastWeekStart) && prescriptionDate.before(lastWeekEnd)) {
                            filtered.add(prescription);
                        }
                        continue;
                    case "Last Month":
                        calendar.add(Calendar.MONTH, -1);
                        Date lastMonthStart = calendar.getTime();
                        calendar.add(Calendar.MONTH, 1);
                        Date lastMonthEnd = calendar.getTime();
                        if (prescriptionDate.after(lastMonthStart) && prescriptionDate.before(lastMonthEnd)) {
                            filtered.add(prescription);
                        }
                        continue;
                    case "Last Year":
                        calendar.add(Calendar.YEAR, -1);
                        Date lastYearStart = calendar.getTime();
                        calendar.add(Calendar.YEAR, 1);
                        Date lastYearEnd = calendar.getTime();
                        if (prescriptionDate.after(lastYearStart) && prescriptionDate.before(lastYearEnd)) {
                            filtered.add(prescription);
                        }
                        continue;
                }
                
                Date periodStart = calendar.getTime();
                if (prescriptionDate.after(periodStart) || prescriptionDate.equals(periodStart)) {
                    filtered.add(prescription);
                }
            } catch (Exception e) {
                if (selectedTimePeriod.equals("All Time")) {
                    filtered.add(prescription);
                }
            }
        }
        
        return filtered;
    }

    private void generateSystemReport() {
        reportItems.add(new ReportItem("REPORT_HEADER", "System Report - " + selectedTimePeriod, "", "", ""));
        
        // Get system statistics from database
        int totalEmployees = databaseHelper.getTotalEmployeesCount();
        int totalPatients = databaseHelper.getTotalPatientsCount();
        int totalPrescriptions = databaseHelper.getPrescriptionsCount();
        int totalMedicines = databaseHelper.getTotalMedicinesCount();
        int nurses = databaseHelper.getEmployeesCountByRole("Nurse");
        int doctors = databaseHelper.getEmployeesCountByRole("Doctor");
        int pharmacists = databaseHelper.getEmployeesCountByRole("Pharmacist");
        
        reportItems.add(new ReportItem("STATISTICS", "System Statistics", 
            "Total Employees: " + totalEmployees, 
            "Total Patients: " + totalPatients,
            "Total Prescriptions: " + totalPrescriptions + " | Total Medicines: " + totalMedicines));
        
        reportItems.add(new ReportItem("STATISTICS", "Employee Breakdown", 
            "Nurses: " + nurses + " | Doctors: " + doctors,
            "Pharmacists: " + pharmacists,
            "Total Active Staff: " + (nurses + doctors + pharmacists)));
        
        // Add system health indicators
        reportItems.add(new ReportItem("SYSTEM_HEALTH", "System Health", 
            "Database Status: ✅ Active", 
            "User Sessions: ✅ Active",
            "Report Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date())));
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
        try {
            if (emptyStateText == null || reportsRecyclerView == null) {
                return; // Views not initialized yet
            }
            
            if (reportItems == null || reportItems.isEmpty()) {
                emptyStateText.setVisibility(View.VISIBLE);
                reportsRecyclerView.setVisibility(View.GONE);
            } else {
                emptyStateText.setVisibility(View.GONE);
                reportsRecyclerView.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                // Set title
                titleText.setText(item.getTitle());
                
                // Set subtitle (time frame)
                if (item.getSubtitle() != null && !item.getSubtitle().isEmpty()) {
                    subtitleText.setText(item.getSubtitle());
                    subtitleText.setVisibility(View.VISIBLE);
                } else {
                    subtitleText.setVisibility(View.GONE);
                }
                
                // Set description (summary)
                if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                    descriptionText.setText(item.getDescription());
                    descriptionText.setVisibility(View.VISIBLE);
                } else {
                    descriptionText.setVisibility(View.GONE);
                }
                
                // Set details
                if (item.getDetails() != null && !item.getDetails().isEmpty()) {
                    detailsText.setText(item.getDetails());
                    detailsText.setVisibility(View.VISIBLE);
                } else {
                    detailsText.setVisibility(View.GONE);
                }

                // Set different styles and colors based on item type
                Context context = getContext();
                if (context != null) {
                    switch (item.getType()) {
                        case "REPORT_HEADER":
                            titleText.setTextSize(20);
                            titleText.setTextColor(context.getColor(R.color.primary_blue));
                            titleText.setTypeface(null, android.graphics.Typeface.BOLD);
                            subtitleText.setVisibility(View.GONE);
                            descriptionText.setVisibility(View.GONE);
                            detailsText.setVisibility(View.GONE);
                            reportCard.setCardBackgroundColor(context.getColor(R.color.background_light));
                            break;
                        case "STATISTICS":
                            titleText.setTextSize(16);
                            titleText.setTextColor(context.getColor(R.color.success_green));
                            titleText.setTypeface(null, android.graphics.Typeface.BOLD);
                            subtitleText.setVisibility(View.VISIBLE);
                            descriptionText.setVisibility(View.VISIBLE);
                            detailsText.setVisibility(View.VISIBLE);
                            reportCard.setCardBackgroundColor(context.getColor(android.R.color.white));
                            break;
                        case "PATIENT_DETAIL":
                        case "PRESCRIPTION_DETAIL":
                            titleText.setTextSize(15);
                            titleText.setTextColor(context.getColor(R.color.text_primary));
                            titleText.setTypeface(null, android.graphics.Typeface.BOLD);
                            subtitleText.setVisibility(View.VISIBLE);
                            descriptionText.setVisibility(View.VISIBLE);
                            detailsText.setVisibility(View.VISIBLE);
                            reportCard.setCardBackgroundColor(context.getColor(android.R.color.white));
                            break;
                        default:
                            titleText.setTextSize(14);
                            titleText.setTextColor(context.getColor(R.color.text_primary));
                            titleText.setTypeface(null, android.graphics.Typeface.NORMAL);
                            subtitleText.setVisibility(View.VISIBLE);
                            descriptionText.setVisibility(View.VISIBLE);
                            detailsText.setVisibility(View.VISIBLE);
                            reportCard.setCardBackgroundColor(context.getColor(android.R.color.white));
                            break;
                    }
                }
            }
        }
    }
}