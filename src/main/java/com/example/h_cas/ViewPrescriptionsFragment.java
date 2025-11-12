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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Prescription;
import com.example.h_cas.models.Patient;
import com.example.h_cas.utils.RFIDHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewPrescriptionsFragment displays all prescriptions created by doctors for nurses to view.
 */
public class ViewPrescriptionsFragment extends Fragment {

    private RecyclerView prescriptionsRecyclerView;
    private TextView emptyStateTextView;
    private HCasDatabaseHelper databaseHelper;
    private PrescriptionAdapter prescriptionAdapter;
    private RFIDHelper rfidHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_prescriptions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        initializeDatabase();
        setupRecyclerView();
        loadPrescriptions();
    }

    private void initializeViews(View view) {
        prescriptionsRecyclerView = view.findViewById(R.id.prescriptionsRecyclerView);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
        rfidHelper = new RFIDHelper(getContext());
    }

    private void setupRecyclerView() {
        prescriptionAdapter = new PrescriptionAdapter();
        prescriptionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        prescriptionsRecyclerView.setAdapter(prescriptionAdapter);
        // Performance optimizations
        prescriptionsRecyclerView.setHasFixedSize(true); // RecyclerView size doesn't change
        prescriptionsRecyclerView.setItemViewCacheSize(20); // Cache more views for smoother scrolling
    }

    private void loadPrescriptions() {
        // Load prescriptions in background to avoid blocking UI
        com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
            List<Prescription> prescriptions = databaseHelper.getAllPrescriptions();
            
            // Update UI on main thread
            com.example.h_cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                if (getContext() == null || getView() == null) {
                    return; // Fragment is detached
                }
                loadPrescriptionsIntoUI(prescriptions);
            });
        });
    }
    
    private void loadPrescriptionsIntoUI(List<Prescription> prescriptions) {
        
        if (prescriptions.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            prescriptionsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            prescriptionsRecyclerView.setVisibility(View.VISIBLE);
            prescriptionAdapter.setPrescriptions(prescriptions);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPrescriptions(); // Refresh when returning to this screen
    }

    // RecyclerView Adapter for prescriptions
    private class PrescriptionAdapter extends RecyclerView.Adapter<PrescriptionAdapter.PrescriptionViewHolder> {
        private List<Prescription> prescriptions = new ArrayList<>();

        public void setPrescriptions(List<Prescription> newPrescriptions) {
            if (newPrescriptions == null) {
                newPrescriptions = new ArrayList<>();
            }
            
            if (this.prescriptions == null) {
                this.prescriptions = new ArrayList<>();
            }
            
            // Use DiffUtil for efficient updates (only updates changed items)
            androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = 
                androidx.recyclerview.widget.DiffUtil.calculateDiff(new PrescriptionDiffCallback(this.prescriptions, newPrescriptions));
            
            this.prescriptions.clear();
            this.prescriptions.addAll(newPrescriptions);
            diffResult.dispatchUpdatesTo(this);
        }
        
        // DiffUtil callback for efficient RecyclerView updates
        private class PrescriptionDiffCallback extends androidx.recyclerview.widget.DiffUtil.Callback {
            private List<Prescription> oldList;
            private List<Prescription> newList;
            
            public PrescriptionDiffCallback(List<Prescription> oldList, List<Prescription> newList) {
                this.oldList = oldList;
                this.newList = newList;
            }
            
            @Override
            public int getOldListSize() {
                return oldList.size();
            }
            
            @Override
            public int getNewListSize() {
                return newList.size();
            }
            
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return oldList.get(oldItemPosition).getPrescriptionId().equals(newList.get(newItemPosition).getPrescriptionId());
            }
            
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Prescription oldPrescription = oldList.get(oldItemPosition);
                Prescription newPrescription = newList.get(newItemPosition);
                return oldPrescription.getMedication().equals(newPrescription.getMedication()) &&
                       oldPrescription.getStatus().equals(newPrescription.getStatus());
            }
        }

        @NonNull
        @Override
        public PrescriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prescription, parent, false);
            return new PrescriptionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PrescriptionViewHolder holder, int position) {
            Prescription prescription = prescriptions.get(position);
            holder.bind(prescription);
        }

        @Override
        public int getItemCount() {
            return prescriptions != null ? prescriptions.size() : 0;
        }

        class PrescriptionViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView cardView;
            private TextView prescriptionIdText;
            private TextView patientNameText;
            private TextView medicationText;
            private TextView dosageText;
            private TextView frequencyText;
            private TextView durationText;
            private TextView doctorNameText;
            private TextView dateText;

            public PrescriptionViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.prescriptionCardView);
                prescriptionIdText = itemView.findViewById(R.id.prescriptionIdText);
                patientNameText = itemView.findViewById(R.id.patientNameText);
                medicationText = itemView.findViewById(R.id.medicationText);
                dosageText = itemView.findViewById(R.id.dosageText);
                frequencyText = itemView.findViewById(R.id.frequencyText);
                durationText = itemView.findViewById(R.id.durationText);
                doctorNameText = itemView.findViewById(R.id.doctorNameText);
                dateText = itemView.findViewById(R.id.dateText);
            }

            public void bind(Prescription prescription) {
                prescriptionIdText.setText("Prescription ID: " + prescription.getPrescriptionId());
                patientNameText.setText("Patient: " + prescription.getPatientName());
                medicationText.setText("Medication: " + prescription.getMedication());
                dosageText.setText("Dosage: " + prescription.getDosage());
                frequencyText.setText("Frequency: " + prescription.getFrequency());
                durationText.setText("Duration: " + prescription.getDuration());
                doctorNameText.setText("Doctor: " + prescription.getDoctorName());
                dateText.setText("Date: " + prescription.getCreatedDate());
                
                // Make card clickable to show prescription details
                cardView.setOnClickListener(v -> showPrescriptionDetails(prescription));
            }
            
            private void showPrescriptionDetails(Prescription prescription) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Prescription Details");
                
                // Inflate custom dialog layout
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_prescription_details, null);
                builder.setView(dialogView);
                
                // Set prescription information
                TextView dialogPrescriptionId = dialogView.findViewById(R.id.dialogPrescriptionId);
                TextView dialogPatientName = dialogView.findViewById(R.id.dialogPatientName);
                TextView dialogMedication = dialogView.findViewById(R.id.dialogMedication);
                TextView dialogDosage = dialogView.findViewById(R.id.dialogDosage);
                TextView dialogFrequency = dialogView.findViewById(R.id.dialogFrequency);
                TextView dialogDuration = dialogView.findViewById(R.id.dialogDuration);
                TextView dialogDoctor = dialogView.findViewById(R.id.dialogDoctor);
                TextView dialogDate = dialogView.findViewById(R.id.dialogDate);
                TextView dialogInstructions = dialogView.findViewById(R.id.dialogInstructions);
                
                // Populate prescription data
                dialogPrescriptionId.setText("Prescription ID: " + prescription.getPrescriptionId());
                dialogPatientName.setText("Patient: " + prescription.getPatientName());
                dialogMedication.setText("Medication: " + prescription.getMedication());
                dialogDosage.setText("Dosage: " + prescription.getDosage());
                dialogFrequency.setText("Frequency: " + prescription.getFrequency());
                dialogDuration.setText("Duration: " + prescription.getDuration());
                dialogDoctor.setText("Doctor: " + prescription.getDoctorName());
                dialogDate.setText("Date: " + prescription.getCreatedDate());
                dialogInstructions.setText("Instructions: " + (prescription.getInstructions() != null && !prescription.getInstructions().isEmpty() ? prescription.getInstructions() : "None"));
                
                // Set up buttons
                MaterialButton rfidButton = dialogView.findViewById(R.id.rfidRegistrationButton);
                ImageButton closeButton = dialogView.findViewById(R.id.closePrescriptionButton);
                
                AlertDialog dialog = builder.create();
                
                // RFID Registration button
                rfidButton.setOnClickListener(v -> {
                    registerPatientWithRFID(prescription);
                    dialog.dismiss();
                });
                
                // Close button
                closeButton.setOnClickListener(v -> dialog.dismiss());
                
                dialog.show();
            }
            
            private void registerPatientWithRFID(Prescription prescription) {
                // Show RFID scanning dialog
                AlertDialog.Builder rfidBuilder = new AlertDialog.Builder(getContext());
                rfidBuilder.setTitle("RFID Card Registration");
                rfidBuilder.setMessage("Please scan an RFID card to register " + prescription.getPatientName() + "'s prescription.\n\nTap the RFID card on your device to continue.");
                
                rfidBuilder.setPositiveButton("Scan RFID Card", (dialog, which) -> {
                    // Start RFID scanning process
                    startRFIDScanning(prescription);
                });
                
                rfidBuilder.setNegativeButton("Cancel", (dialog, which) -> {
                    // Do nothing, just close dialog
                });
                
                rfidBuilder.show();
            }
            
            private void startRFIDScanning(Prescription prescription) {
                // Check NFC availability first
                if (!rfidHelper.isNFCAvailable()) {
                    Toast.makeText(getContext(), "NFC is not available on this device", Toast.LENGTH_LONG).show();
                    return;
                }
                
                if (!rfidHelper.isNFCEnabled()) {
                    Toast.makeText(getContext(), "NFC is disabled. Please enable NFC in settings", Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Show scanning dialog
                AlertDialog.Builder scanningBuilder = new AlertDialog.Builder(getContext());
                scanningBuilder.setTitle("Scanning RFID Card...");
                scanningBuilder.setMessage("Please hold the RFID card near your device.\n\nTap 'Card Detected' when the RFID card is scanned.");
                
                scanningBuilder.setPositiveButton("Card Detected", (dialog, which) -> {
                    // Simulate RFID card detection and get unique ID
                    String rfidTagId = rfidHelper.simulateRFIDCardScan();
                    
                    if (rfidHelper.isValidCardId(rfidTagId)) {
                        // Write prescription data to RFID
                        boolean success = databaseHelper.writePrescriptionToRFID(rfidTagId, prescription);
                        
                        if (success) {
                            Toast.makeText(getContext(), "✅ Prescription data written to RFID card: " + rfidHelper.formatCardId(rfidTagId), Toast.LENGTH_LONG).show();
                            
                            // Show RFID tag details
                            showRFIDDetails(rfidTagId, prescription);
                        } else {
                            Toast.makeText(getContext(), "❌ Failed to write to RFID. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "❌ Invalid RFID card detected. Please try again.", Toast.LENGTH_LONG).show();
                    }
                });
                
                scanningBuilder.setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(getContext(), "RFID scanning cancelled.", Toast.LENGTH_SHORT).show();
                });
                
                scanningBuilder.show();
            }
            
            private void showRFIDDetails(String rfidTagId, Prescription prescription) {
                AlertDialog.Builder detailsBuilder = new AlertDialog.Builder(getContext());
                detailsBuilder.setTitle("RFID Card Successfully Programmed");
                
                String message = "RFID Card ID: " + rfidHelper.formatCardId(rfidTagId) + "\n\n" +
                               "Patient: " + prescription.getPatientName() + "\n" +
                               "Medicine: " + prescription.getMedication() + "\n" +
                               "Dosage: " + prescription.getDosage() + "\n" +
                               "Frequency: " + prescription.getFrequency() + "\n" +
                               "Duration: " + prescription.getDuration() + "\n\n" +
                               "✅ RFID card has been programmed with prescription data.\n" +
                               "Pharmacist can now scan this RFID card to dispense medication.";
                
                detailsBuilder.setMessage(message);
                detailsBuilder.setPositiveButton("OK", (dialog, which) -> {
                    // Dialog dismissed
                });
                
                detailsBuilder.show();
            }
        }
    }
}
