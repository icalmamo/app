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

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Prescription;

import java.util.ArrayList;
import java.util.List;

/**
 * DispensedHistoryFragment displays all successfully dispensed medications
 */
public class DispensedHistoryFragment extends Fragment {

    private HCasDatabaseHelper databaseHelper;
    private RecyclerView dispensedHistoryRecyclerView;
    private DispensedHistoryAdapter historyAdapter;
    private List<Prescription> dispensedHistoryList;
    private TextView emptyHistoryText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dispensed_history, container, false);
        
        initializeViews(view);
        initializeDatabase();
        setupRecyclerView();
        loadDispensedHistory();
        
        return view;
    }

    private void initializeViews(View view) {
        dispensedHistoryRecyclerView = view.findViewById(R.id.dispensedHistoryRecyclerView);
        emptyHistoryText = view.findViewById(R.id.emptyHistoryText);
    }

    private void initializeDatabase() {
        if (getContext() != null) {
            databaseHelper = new HCasDatabaseHelper(getContext());
        }
    }

    private void setupRecyclerView() {
        if (getContext() == null || dispensedHistoryRecyclerView == null) {
            return;
        }
        dispensedHistoryList = new ArrayList<>();
        historyAdapter = new DispensedHistoryAdapter(dispensedHistoryList);
        dispensedHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dispensedHistoryRecyclerView.setAdapter(historyAdapter);
        dispensedHistoryRecyclerView.setNestedScrollingEnabled(false); // Disable nested scrolling since parent is NestedScrollView
    }

    private void loadDispensedHistory() {
        if (databaseHelper == null || dispensedHistoryList == null) {
            android.util.Log.e("DispensedHistory", "❌ DatabaseHelper or list is null");
            return;
        }
        
        // Load data in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                // Get all prescriptions and filter for dispensed ones
                List<Prescription> allPrescriptions = databaseHelper.getAllPrescriptions();
                android.util.Log.d("DispensedHistory", "📊 Total prescriptions in database: " + (allPrescriptions != null ? allPrescriptions.size() : 0));
                
                final List<Prescription> filteredList = new ArrayList<>();
                
                if (allPrescriptions != null) {
                    for (Prescription prescription : allPrescriptions) {
                        if (prescription != null) {
                            String status = prescription.getStatus();
                            String prescriptionId = prescription.getPrescriptionId();
                            String medication = prescription.getMedication();
                            
                            android.util.Log.d("DispensedHistory", "📋 Checking prescription: " + prescriptionId + 
                                " | Status: " + status + " | Medication: " + medication);
                            
                            if ("Dispensed".equals(status)) {
                                filteredList.add(prescription);
                                android.util.Log.d("DispensedHistory", "✅ Added to filtered list: " + prescriptionId + " (" + medication + ")");
                            } else {
                                android.util.Log.d("DispensedHistory", "⏭️ Skipped (status not 'Dispensed'): " + prescriptionId + " | Status: " + status);
                            }
                        }
                    }
                }
                
                android.util.Log.d("DispensedHistory", "📊 Filtered dispensed prescriptions: " + filteredList.size());
                
                // Sort by created date (latest first) - parse dates properly for accurate sorting
                java.util.Collections.sort(filteredList, (p1, p2) -> {
                    try {
                        String date1Str = p1.getCreatedDate() != null ? p1.getCreatedDate() : "";
                        String date2Str = p2.getCreatedDate() != null ? p2.getCreatedDate() : "";
                        
                        if (date1Str.isEmpty() && date2Str.isEmpty()) {
                            return 0;
                        }
                        if (date1Str.isEmpty()) {
                            return 1; // Empty dates go to bottom
                        }
                        if (date2Str.isEmpty()) {
                            return -1; // Empty dates go to bottom
                        }
                        
                        // Parse dates for proper comparison
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                        java.util.Date date1 = sdf.parse(date1Str);
                        java.util.Date date2 = sdf.parse(date2Str);
                        
                        if (date1 != null && date2 != null) {
                            return date2.compareTo(date1); // Descending order (newest first)
                        }
                        
                        // Fallback to string comparison if parsing fails
                        return date2Str.compareTo(date1Str);
                    } catch (Exception e) {
                        android.util.Log.w("DispensedHistory", "⚠️ Error sorting by date: " + e.getMessage());
                        // Fallback to string comparison
                        String date1 = p1.getCreatedDate() != null ? p1.getCreatedDate() : "";
                        String date2 = p2.getCreatedDate() != null ? p2.getCreatedDate() : "";
                        return date2.compareTo(date1);
                    }
                });
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            dispensedHistoryList.clear();
                            dispensedHistoryList.addAll(filteredList);
                            
                            if (historyAdapter != null) {
                                historyAdapter.notifyDataSetChanged();
                            }
                            
                            if (emptyHistoryText != null) {
                                if (dispensedHistoryList.isEmpty()) {
                                    emptyHistoryText.setVisibility(View.VISIBLE);
                                } else {
                                    emptyHistoryText.setVisibility(View.GONE);
                                }
                            }
                        } catch (Exception e) {
                            android.util.Log.e("DispensedHistory", "❌ Error updating UI: " + e.getMessage(), e);
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("DispensedHistory", "❌ Error loading dispensed history: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (emptyHistoryText != null) {
                            emptyHistoryText.setText("Error loading history");
                            emptyHistoryText.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh history when fragment becomes visible
        loadDispensedHistory();
    }

    /**
     * Adapter for displaying dispensed prescription history
     */
    private class DispensedHistoryAdapter extends RecyclerView.Adapter<DispensedHistoryAdapter.HistoryViewHolder> {
        private List<Prescription> prescriptions;
        
        public DispensedHistoryAdapter(List<Prescription> prescriptions) {
            this.prescriptions = prescriptions;
        }
        
        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dispensed_history, parent, false);
            return new HistoryViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            Prescription prescription = prescriptions.get(position);
            if (prescription == null) {
                return;
            }
            
            holder.patientNameText.setText(prescription.getPatientName() != null ? prescription.getPatientName() : "Unknown Patient");
            holder.medicationText.setText(prescription.getMedication() != null ? prescription.getMedication() : "Unknown");
            holder.frequencyText.setText("Frequency: " + (prescription.getFrequency() != null ? prescription.getFrequency() : "N/A") + " times/day");
            holder.durationText.setText("Duration: " + (prescription.getDuration() != null ? prescription.getDuration() : "N/A") + " days");
            holder.doctorNameText.setText("Doctor: " + (prescription.getDoctorName() != null ? prescription.getDoctorName() : "Unknown"));
            
            // Calculate and display deducted amount
            int frequency = 1;
            int days = 1;
            try {
                String freqStr = prescription.getFrequency();
                if (freqStr != null && !freqStr.trim().isEmpty()) {
                    frequency = Integer.parseInt(freqStr.trim());
                }
            } catch (NumberFormatException e) {
                // Use default
            }
            
            try {
                String durationStr = prescription.getDuration();
                if (durationStr != null && !durationStr.trim().isEmpty()) {
                    durationStr = durationStr.trim().toLowerCase().replaceAll("\\s*days?\\s*", "");
                    days = Integer.parseInt(durationStr);
                }
            } catch (NumberFormatException e) {
                // Use default
            }
            
            int totalDoses = frequency * days;
            
            // Try to extract deducted amount from instructions if available
            String deductedAmount = "";
            if (prescription.getInstructions() != null && prescription.getInstructions().contains("[Deducted:")) {
                String instructions = prescription.getInstructions();
                int start = instructions.indexOf("[Deducted:");
                int end = instructions.indexOf("]", start);
                if (start >= 0 && end > start) {
                    deductedAmount = instructions.substring(start + 10, end).trim();
                }
            }
            
            // If not found in instructions, calculate it
            if (deductedAmount.isEmpty()) {
                deductedAmount = totalDoses + " units";
            }
            
            holder.deductedText.setText("Deducted from Inventory: " + deductedAmount);
            
            // Format dispensed date
            String dispensedDate = prescription.getCreatedDate() != null ? prescription.getCreatedDate() : "N/A";
            holder.dateText.setText("Dispensed: " + dispensedDate);
        }
        
        @Override
        public int getItemCount() {
            return prescriptions != null ? prescriptions.size() : 0;
        }
        
        class HistoryViewHolder extends RecyclerView.ViewHolder {
            TextView patientNameText;
            TextView medicationText;
            TextView frequencyText;
            TextView durationText;
            TextView doctorNameText;
            TextView deductedText;
            TextView dateText;
            
            HistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                patientNameText = itemView.findViewById(R.id.historyPatientName);
                medicationText = itemView.findViewById(R.id.historyMedication);
                frequencyText = itemView.findViewById(R.id.historyFrequency);
                durationText = itemView.findViewById(R.id.historyDuration);
                doctorNameText = itemView.findViewById(R.id.historyDoctorName);
                deductedText = itemView.findViewById(R.id.historyDeducted);
                dateText = itemView.findViewById(R.id.historyDate);
            }
        }
    }
}

