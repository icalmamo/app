package com.example.h_cas;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Patient;
import com.example.h_cas.models.Prescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PatientHistoryFragment displays patients who have received prescriptions.
 */
public class PatientHistoryFragment extends Fragment {

    private RecyclerView patientHistoryRecyclerView;
    private TextView emptyStateTextView;
    private HCasDatabaseHelper databaseHelper;
    private PatientHistoryAdapter patientHistoryAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        initializeDatabase();
        setupRecyclerView();
        loadPatientHistory();
    }

    private void initializeViews(View view) {
        patientHistoryRecyclerView = view.findViewById(R.id.patientHistoryRecyclerView);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupRecyclerView() {
        patientHistoryAdapter = new PatientHistoryAdapter();
        patientHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        patientHistoryRecyclerView.setAdapter(patientHistoryAdapter);
    }

    private void loadPatientHistory() {
        // Get all patients who have received prescriptions
        List<PatientHistoryItem> patientHistoryItems = getPatientsWithPrescriptions();
        
        if (patientHistoryItems.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            patientHistoryRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            patientHistoryRecyclerView.setVisibility(View.VISIBLE);
            patientHistoryAdapter.setPatientHistoryItems(patientHistoryItems);
        }
    }

    private List<PatientHistoryItem> getPatientsWithPrescriptions() {
        List<PatientHistoryItem> historyItems = new ArrayList<>();
        
        // Get all prescriptions
        List<Prescription> prescriptions = databaseHelper.getAllPrescriptions();
        
        // Create a map to track unique patients and their prescription counts
        Map<String, PatientHistoryItem> patientMap = new HashMap<>();
        
        for (Prescription prescription : prescriptions) {
            String patientId = prescription.getPatientId();
            
            if (!patientMap.containsKey(patientId)) {
                // Get patient details
                Patient patient = databaseHelper.getPatientById(patientId);
                if (patient != null) {
                    PatientHistoryItem historyItem = new PatientHistoryItem();
                    historyItem.setPatient(patient);
                    historyItem.setPrescriptionCount(1);
                    historyItem.setLastPrescriptionDate(prescription.getCreatedDate());
                    historyItem.setLastMedication(prescription.getMedication());
                    historyItem.setLastDoctor(prescription.getDoctorName());
                    
                    patientMap.put(patientId, historyItem);
                }
            } else {
                // Update existing patient's prescription count and last prescription info
                PatientHistoryItem existingItem = patientMap.get(patientId);
                existingItem.setPrescriptionCount(existingItem.getPrescriptionCount() + 1);
                existingItem.setLastPrescriptionDate(prescription.getCreatedDate());
                existingItem.setLastMedication(prescription.getMedication());
                existingItem.setLastDoctor(prescription.getDoctorName());
            }
        }
        
        // Convert map to list
        historyItems.addAll(patientMap.values());
        
        // Sort by last prescription date (most recent first)
        historyItems.sort((a, b) -> b.getLastPrescriptionDate().compareTo(a.getLastPrescriptionDate()));
        
        return historyItems;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPatientHistory(); // Refresh when returning to this screen
    }

    // RecyclerView Adapter for patient history
    private class PatientHistoryAdapter extends RecyclerView.Adapter<PatientHistoryAdapter.PatientHistoryViewHolder> {
        private List<PatientHistoryItem> patientHistoryItems;

        public void setPatientHistoryItems(List<PatientHistoryItem> patientHistoryItems) {
            this.patientHistoryItems = patientHistoryItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PatientHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_history, parent, false);
            return new PatientHistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PatientHistoryViewHolder holder, int position) {
            PatientHistoryItem historyItem = patientHistoryItems.get(position);
            holder.bind(historyItem);
        }

        @Override
        public int getItemCount() {
            return patientHistoryItems != null ? patientHistoryItems.size() : 0;
        }

        class PatientHistoryViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView cardView;
            private TextView patientIdText;
            private TextView patientNameText;
            private TextView prescriptionCountText;
            private TextView lastPrescriptionText;
            private TextView lastMedicationText;
            private TextView lastDoctorText;

            public PatientHistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.patientHistoryCardView);
                patientIdText = itemView.findViewById(R.id.patientIdText);
                patientNameText = itemView.findViewById(R.id.patientNameText);
                prescriptionCountText = itemView.findViewById(R.id.prescriptionCountText);
                lastPrescriptionText = itemView.findViewById(R.id.lastPrescriptionText);
                lastMedicationText = itemView.findViewById(R.id.lastMedicationText);
                lastDoctorText = itemView.findViewById(R.id.lastDoctorText);
            }

            public void bind(PatientHistoryItem historyItem) {
                Patient patient = historyItem.getPatient();
                
                patientIdText.setText("Patient ID: " + patient.getPatientId());
                patientNameText.setText(patient.getFullName());
                prescriptionCountText.setText("Prescriptions: " + historyItem.getPrescriptionCount());
                lastPrescriptionText.setText("Last Rx: " + historyItem.getLastPrescriptionDate());
                lastMedicationText.setText("Last Medication: " + historyItem.getLastMedication());
                lastDoctorText.setText("Last Doctor: " + historyItem.getLastDoctor());
                
                // Make card clickable to view patient details
                cardView.setOnClickListener(v -> {
                    // Navigate to patient details with prescription history
                    showPatientDetails(patient, historyItem);
                });
            }
            
            private void showPatientDetails(Patient patient, PatientHistoryItem historyItem) {
                // Show detailed patient history dialog
                showPatientHistoryDialog(patient, historyItem);
            }
            
            private void showPatientHistoryDialog(Patient patient, PatientHistoryItem historyItem) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Patient History - " + patient.getFullName());
                
                // Inflate custom dialog layout
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_patient_history, null);
                builder.setView(dialogView);
                
                // Set patient information
                TextView dialogPatientId = dialogView.findViewById(R.id.dialogPatientId);
                TextView dialogPatientName = dialogView.findViewById(R.id.dialogPatientName);
                TextView dialogPatientAge = dialogView.findViewById(R.id.dialogPatientAge);
                TextView dialogPatientGender = dialogView.findViewById(R.id.dialogPatientGender);
                TextView dialogPatientPhone = dialogView.findViewById(R.id.dialogPatientPhone);
                TextView dialogPatientEmail = dialogView.findViewById(R.id.dialogPatientEmail);
                TextView dialogPrescriptionCount = dialogView.findViewById(R.id.dialogPrescriptionCount);
                TextView dialogLastPrescription = dialogView.findViewById(R.id.dialogLastPrescription);
                
                // Populate patient data
                dialogPatientId.setText("Patient ID: " + patient.getPatientId());
                dialogPatientName.setText(patient.getFullName());
                dialogPatientAge.setText("Age: " + (patient.getAge() != null ? patient.getAge() : "N/A"));
                dialogPatientGender.setText("Gender: " + (patient.getGender() != null ? patient.getGender() : "N/A"));
                dialogPatientPhone.setText("Phone: " + (patient.getPhone() != null ? patient.getPhone() : "N/A"));
                dialogPatientEmail.setText("Email: " + (patient.getEmail() != null ? patient.getEmail() : "N/A"));
                dialogPrescriptionCount.setText("Total Prescriptions: " + historyItem.getPrescriptionCount());
                dialogLastPrescription.setText("Last Prescription: " + historyItem.getLastPrescriptionDate());
                
                // Set up prescriptions list
                RecyclerView prescriptionsRecyclerView = dialogView.findViewById(R.id.prescriptionsRecyclerView);
                setupPrescriptionsRecyclerView(prescriptionsRecyclerView, patient.getPatientId());
                
                // Set up close button
                ImageButton closeButton = dialogView.findViewById(R.id.closeHistoryButton);
                
                AlertDialog dialog = builder.create();
                
                closeButton.setOnClickListener(v -> dialog.dismiss());
                
                dialog.show();
            }
            
            private void setupPrescriptionsRecyclerView(RecyclerView recyclerView, String patientId) {
                // Get all prescriptions for this patient
                List<Prescription> patientPrescriptions = getPrescriptionsForPatient(patientId);
                
                PrescriptionHistoryAdapter adapter = new PrescriptionHistoryAdapter(patientPrescriptions);
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                recyclerView.setAdapter(adapter);
            }
            
            private List<Prescription> getPrescriptionsForPatient(String patientId) {
                List<Prescription> allPrescriptions = databaseHelper.getAllPrescriptions();
                List<Prescription> patientPrescriptions = new ArrayList<>();
                
                for (Prescription prescription : allPrescriptions) {
                    if (prescription.getPatientId().equals(patientId)) {
                        patientPrescriptions.add(prescription);
                    }
                }
                
                // Sort by date (most recent first)
                patientPrescriptions.sort((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()));
                
                return patientPrescriptions;
            }
            
            // Adapter for prescription history within the dialog
            private class PrescriptionHistoryAdapter extends RecyclerView.Adapter<PrescriptionHistoryAdapter.PrescriptionViewHolder> {
                private List<Prescription> prescriptions;
                
                public PrescriptionHistoryAdapter(List<Prescription> prescriptions) {
                    this.prescriptions = prescriptions;
                }
                
                @NonNull
                @Override
                public PrescriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prescription_history, parent, false);
                    return new PrescriptionViewHolder(view);
                }
                
                @Override
                public void onBindViewHolder(@NonNull PrescriptionViewHolder holder, int position) {
                    Prescription prescription = prescriptions.get(position);
                    holder.bind(prescription);
                }
                
                @Override
                public int getItemCount() {
                    return prescriptions.size();
                }
                
                class PrescriptionViewHolder extends RecyclerView.ViewHolder {
                    private TextView prescriptionIdText;
                    private TextView medicationText;
                    private TextView frequencyText;
                    private TextView durationText;
                    private TextView doctorText;
                    private TextView dateText;
                    private TextView instructionsText;
                    
                    public PrescriptionViewHolder(@NonNull View itemView) {
                        super(itemView);
                        prescriptionIdText = itemView.findViewById(R.id.prescriptionIdText);
                        medicationText = itemView.findViewById(R.id.medicationText);
                        frequencyText = itemView.findViewById(R.id.frequencyText);
                        durationText = itemView.findViewById(R.id.durationText);
                        doctorText = itemView.findViewById(R.id.doctorText);
                        dateText = itemView.findViewById(R.id.dateText);
                        instructionsText = itemView.findViewById(R.id.instructionsText);
                    }
                    
                    public void bind(Prescription prescription) {
                        prescriptionIdText.setText("Prescription ID: " + prescription.getPrescriptionId());
                        medicationText.setText("Medication: " + prescription.getMedication());
                        frequencyText.setText("Frequency: " + prescription.getFrequency());
                        durationText.setText("Duration: " + prescription.getDuration());
                        doctorText.setText("Doctor: " + prescription.getDoctorName());
                        dateText.setText("Date: " + prescription.getCreatedDate());
                        instructionsText.setText("Instructions: " + (prescription.getInstructions() != null && !prescription.getInstructions().isEmpty() ? prescription.getInstructions() : "None"));
                    }
                }
            }
        }
    }

    // Data class for patient history items
    public static class PatientHistoryItem {
        private Patient patient;
        private int prescriptionCount;
        private String lastPrescriptionDate;
        private String lastMedication;
        private String lastDoctor;

        public Patient getPatient() {
            return patient;
        }

        public void setPatient(Patient patient) {
            this.patient = patient;
        }

        public int getPrescriptionCount() {
            return prescriptionCount;
        }

        public void setPrescriptionCount(int prescriptionCount) {
            this.prescriptionCount = prescriptionCount;
        }

        public String getLastPrescriptionDate() {
            return lastPrescriptionDate;
        }

        public void setLastPrescriptionDate(String lastPrescriptionDate) {
            this.lastPrescriptionDate = lastPrescriptionDate;
        }

        public String getLastMedication() {
            return lastMedication;
        }

        public void setLastMedication(String lastMedication) {
            this.lastMedication = lastMedication;
        }

        public String getLastDoctor() {
            return lastDoctor;
        }

        public void setLastDoctor(String lastDoctor) {
            this.lastDoctor = lastDoctor;
        }
    }
}
