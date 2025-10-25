package com.example.h_cas;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.RFIDData;
import com.example.h_cas.models.Medicine;

/**
 * MedicationDispensingFragment handles RFID reading and medication dispensing for pharmacists
 */
public class MedicationDispensingFragment extends Fragment {

    private HCasDatabaseHelper databaseHelper;
    private MaterialButton scanRFIDButton;
    private MaterialCardView prescriptionCard;
    private TextView emptyStateText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_medication_dispensing, container, false);
        
        initializeViews(view);
        initializeDatabase();
        setupClickListeners();
        
        return view;
    }

    private void initializeViews(View view) {
        scanRFIDButton = view.findViewById(R.id.scanRFIDButton);
        prescriptionCard = view.findViewById(R.id.prescriptionCard);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        
        // Initially hide prescription card
        prescriptionCard.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupClickListeners() {
        scanRFIDButton.setOnClickListener(v -> scanRFIDTag());
    }

    private void scanRFIDTag() {
        // Show RFID scanning dialog
        AlertDialog.Builder scanBuilder = new AlertDialog.Builder(getContext());
        scanBuilder.setTitle("Scan RFID Tag");
        scanBuilder.setMessage("Please scan the patient's RFID tag to read prescription data.");
        
        scanBuilder.setPositiveButton("Simulate Scan", (dialog, which) -> {
            // For demo purposes, simulate scanning an RFID tag
            // In real implementation, this would read from actual RFID reader
            simulateRFIDScan();
        });
        
        scanBuilder.setNegativeButton("Cancel", (dialog, which) -> {
            // Do nothing, just close dialog
        });
        
        scanBuilder.show();
    }

    private void simulateRFIDScan() {
        // Simulate scanning an RFID tag (in real implementation, this would come from RFID reader)
        String simulatedRFIDTagId = "RFID1759590975147"; // Use a realistic RFID tag ID
        
        // Read prescription data from RFID
        RFIDData rfidData = databaseHelper.readPrescriptionFromRFID(simulatedRFIDTagId);
        
        if (rfidData != null) {
            showPrescriptionData(rfidData);
        } else {
            Toast.makeText(getContext(), "❌ No prescription found for this RFID tag or already dispensed.", Toast.LENGTH_LONG).show();
        }
    }

    private void showPrescriptionData(RFIDData rfidData) {
        // Show prescription details
        AlertDialog.Builder prescriptionBuilder = new AlertDialog.Builder(getContext());
        prescriptionBuilder.setTitle("Prescription Found");
        
        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_rfid_prescription, null);
        prescriptionBuilder.setView(dialogView);
        
        // Set prescription information
        TextView dialogPatientName = dialogView.findViewById(R.id.dialogPatientName);
        TextView dialogMedicine = dialogView.findViewById(R.id.dialogMedicine);
        TextView dialogDosage = dialogView.findViewById(R.id.dialogDosage);
        TextView dialogFrequency = dialogView.findViewById(R.id.dialogFrequency);
        TextView dialogDuration = dialogView.findViewById(R.id.dialogDuration);
        TextView dialogDoctor = dialogView.findViewById(R.id.dialogDoctor);
        TextView dialogInstructions = dialogView.findViewById(R.id.dialogInstructions);
        
        // Populate prescription data
        dialogPatientName.setText("Patient: " + rfidData.getPatientName());
        dialogMedicine.setText("Medicine: " + rfidData.getMedicineName());
        dialogDosage.setText("Dosage: " + rfidData.getDosage());
        dialogFrequency.setText("Frequency: " + rfidData.getFrequency());
        dialogDuration.setText("Duration: " + rfidData.getDuration());
        dialogDoctor.setText("Doctor: " + rfidData.getDoctorName());
        dialogInstructions.setText("Instructions: " + (rfidData.getInstructions() != null && !rfidData.getInstructions().isEmpty() ? rfidData.getInstructions() : "None"));
        
        // Set up buttons
        MaterialButton dispenseButton = dialogView.findViewById(R.id.dispenseButton);
        ImageButton closeButton = dialogView.findViewById(R.id.closeRFIDButton);
        
        AlertDialog dialog = prescriptionBuilder.create();
        
        // Dispense medication button
        dispenseButton.setOnClickListener(v -> {
            dispenseMedication(rfidData);
            dialog.dismiss();
        });
        
        // Close button
        closeButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void dispenseMedication(RFIDData rfidData) {
        // Check if medicine is available in stock
        Medicine medicine = databaseHelper.getMedicineByName(rfidData.getMedicineName());
        
        if (medicine == null) {
            Toast.makeText(getContext(), "❌ Medicine not found in inventory: " + rfidData.getMedicineName(), Toast.LENGTH_LONG).show();
            return;
        }
        
        if (!medicine.isInStock()) {
            Toast.makeText(getContext(), "❌ Medicine out of stock: " + rfidData.getMedicineName(), Toast.LENGTH_LONG).show();
            return;
        }
        
        // Show dispensing confirmation
        AlertDialog.Builder dispenseBuilder = new AlertDialog.Builder(getContext());
        dispenseBuilder.setTitle("Dispense Medication");
        dispenseBuilder.setMessage("Dispense " + rfidData.getMedicineName() + " to " + rfidData.getPatientName() + "?\n\n" +
                                 "Current Stock: " + medicine.getStockQuantity() + " " + medicine.getUnit());
        
        dispenseBuilder.setPositiveButton("Dispense", (dialog, which) -> {
            // Deduct from stock (assuming 1 unit per prescription)
            int newStock = medicine.getStockQuantity() - 1;
            boolean stockUpdated = databaseHelper.updateMedicineStock(rfidData.getMedicineName(), newStock);
            
            if (stockUpdated) {
                // Mark prescription as dispensed
                String pharmacistName = "Pharmacist"; // In real implementation, get from logged-in user
                boolean dispensed = databaseHelper.markPrescriptionAsDispensed(rfidData.getRfidTagId(), pharmacistName);
                
                if (dispensed) {
                    Toast.makeText(getContext(), "✅ Medication dispensed successfully!\nStock remaining: " + newStock + " " + medicine.getUnit(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "❌ Failed to mark prescription as dispensed.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getContext(), "❌ Failed to update medicine stock.", Toast.LENGTH_LONG).show();
            }
        });
        
        dispenseBuilder.setNegativeButton("Cancel", (dialog, which) -> {
            // Do nothing, just close dialog
        });
        
        dispenseBuilder.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Enable back navigation with safer implementation
        if (getActivity() != null) {
            getActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    // Navigate back to dashboard
                    if (getActivity() instanceof PharmacistDashboardActivity) {
                        ((PharmacistDashboardActivity) getActivity()).loadFragment(new PharmacistDashboardFragment());
                        ((PharmacistDashboardActivity) getActivity()).getSupportActionBar().setTitle("Pharmacist Dashboard");
                    }
                }
            });
        }
    }
}



