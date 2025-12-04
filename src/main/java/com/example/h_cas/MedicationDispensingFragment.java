package com.example.h_cas;

import android.app.AlertDialog;
import android.nfc.Tag;
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
import com.example.h_cas.models.Patient;
import com.example.h_cas.models.Prescription;

/**
 * MedicationDispensingFragment handles NFC reading and medication dispensing for pharmacists
 */
public class MedicationDispensingFragment extends Fragment {

    private HCasDatabaseHelper databaseHelper;
    private com.example.h_cas.utils.NFCHelper nfcHelper;
    private MaterialButton scanRFIDButton;
    private MaterialCardView prescriptionCard;
    private TextView emptyStateText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_medication_dispensing, container, false);
        
        initializeViews(view);
        initializeDatabase();
        initializeNFC();
        setupClickListeners();
        
        return view;
    }

    private void initializeViews(View view) {
        scanRFIDButton = view.findViewById(R.id.scanRFIDButton);
        prescriptionCard = view.findViewById(R.id.prescriptionCard);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        
        // Update button text to reflect NFC
        if (scanRFIDButton != null) {
            scanRFIDButton.setText("Scan NFC Tag");
        }
        
        // Initially hide prescription card
        prescriptionCard.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
    }
    
    /**
     * Initialize NFC helper
     */
    private void initializeNFC() {
        nfcHelper = new com.example.h_cas.utils.NFCHelper(getContext());
        
        // Set up NFC scan listener
        nfcHelper.setNFCScanListener(new com.example.h_cas.utils.NFCHelper.NFCScanListener() {
            @Override
            public void onNFCTagDetected(String nfcUid, Tag tag) {
                handleNFCTagDetected(nfcUid, tag);
            }
            
            @Override
            public void onNFCWriteSuccess(String nfcUid) {
                // Not used for reading
            }
            
            @Override
            public void onNFCWriteError(String error) {
                // Not used for reading
            }
            
            @Override
            public void onNFCReadSuccess(String nfcUid, String data) {
                // Not used for reading
            }
            
            @Override
            public void onNFCReadError(String error) {
                Toast.makeText(getContext(), "Error reading NFC tag: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        scanRFIDButton.setOnClickListener(v -> scanNFCTag());
    }

    private void scanNFCTag() {
        // Check NFC availability
        if (nfcHelper == null) {
            initializeNFC();
        }
        
        if (!nfcHelper.isNFCAvailable()) {
            Toast.makeText(getContext(), "NFC is not available on this device", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (!nfcHelper.isNFCEnabled()) {
            Toast.makeText(getContext(), "NFC is disabled. Please enable NFC in settings", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Show NFC scanning dialog
        AlertDialog.Builder scanBuilder = new AlertDialog.Builder(getContext());
        scanBuilder.setTitle("Scan NFC Tag");
        scanBuilder.setMessage("Please hold the patient's NFC tag near your device.\n\nThe tag will be automatically detected.");
        scanBuilder.setCancelable(true);
        scanBuilder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        
        AlertDialog scanDialog = scanBuilder.create();
        scanDialog.show();
        
        // Enable NFC foreground dispatch
        if (getActivity() != null) {
            nfcHelper.enableForegroundDispatch(getActivity());
        }
    }
    
    /**
     * Handle NFC tag detection
     */
    private void handleNFCTagDetected(String nfcUid, Tag tag) {
        if (nfcUid == null || nfcUid.isEmpty()) {
            Toast.makeText(getContext(), "Invalid NFC tag detected", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Try reading encoded prescription data directly from the NFC tag (if available)
        if (tag != null) {
            String payload = nfcHelper.readNFCTag(tag);
            if (payload != null && !payload.isEmpty()) {
                java.util.Map<String, String> payloadData = parseNfcPayload(payload);
                Prescription payloadPrescription = null;
                Patient payloadPatient = null;
                
                if (payloadData.containsKey("PRESCRIPTION_ID")) {
                    payloadPrescription = databaseHelper.getPrescriptionById(payloadData.get("PRESCRIPTION_ID"));
                }
                
                String payloadPatientId = payloadData.get("PATIENT_ID");
                if (payloadPatientId != null) {
                    payloadPatient = databaseHelper.getPatientById(payloadPatientId);
                }
                
                if (payloadPrescription != null && payloadPatient == null) {
                    payloadPatient = databaseHelper.getPatientById(payloadPrescription.getPatientId());
                }
                
                if (payloadPrescription != null && payloadPatient != null) {
                    showPrescriptionForDispensing(payloadPrescription, payloadPatient);
                    if (getActivity() != null) {
                        nfcHelper.disableForegroundDispatch(getActivity());
                    }
                    return;
                }
            }
        }
        
        // Get patient by NFC UID
        Patient patient = databaseHelper.getPatientByNfcUid(nfcUid);
        
        if (patient == null) {
            Toast.makeText(getContext(), "❌ No patient found for this NFC tag.", Toast.LENGTH_LONG).show();
            // Disable NFC foreground dispatch
            if (getActivity() != null) {
                nfcHelper.disableForegroundDispatch(getActivity());
            }
            return;
        }
        
        // Get patient's active prescriptions
        java.util.List<Prescription> prescriptions = databaseHelper.getPrescriptionsByPatientId(patient.getPatientId());
        
        if (prescriptions == null || prescriptions.isEmpty()) {
            Toast.makeText(getContext(), "❌ No active prescriptions found for patient: " + patient.getFullName(), Toast.LENGTH_LONG).show();
            // Disable NFC foreground dispatch
            if (getActivity() != null) {
                nfcHelper.disableForegroundDispatch(getActivity());
            }
            return;
        }
        
        // Show prescription selection dialog if multiple prescriptions
        if (prescriptions.size() == 1) {
            showPrescriptionForDispensing(prescriptions.get(0), patient);
        } else {
            showPrescriptionSelectionDialog(prescriptions, patient);
        }
        
        // Disable NFC foreground dispatch
        if (getActivity() != null) {
            nfcHelper.disableForegroundDispatch(getActivity());
        }
    }
    
    /**
     * Show prescription selection dialog when multiple prescriptions exist
     */
    private void showPrescriptionSelectionDialog(java.util.List<Prescription> prescriptions, Patient patient) {
        String[] prescriptionItems = new String[prescriptions.size()];
        for (int i = 0; i < prescriptions.size(); i++) {
            Prescription p = prescriptions.get(i);
            prescriptionItems[i] = p.getMedication() + " - " + p.getDosage() + " (" + p.getCreatedDate() + ")";
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Prescription for " + patient.getFullName());
        builder.setItems(prescriptionItems, (dialog, which) -> {
            showPrescriptionForDispensing(prescriptions.get(which), patient);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Show prescription data for dispensing
     */
    private void showPrescriptionForDispensing(Prescription prescription, Patient patient) {
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
        dialogPatientName.setText("Patient: " + patient.getFullName());
        dialogMedicine.setText("Medicine: " + prescription.getMedication());
        dialogDosage.setText("Dosage: " + prescription.getDosage());
        dialogFrequency.setText("Frequency: " + prescription.getFrequency());
        dialogDuration.setText("Duration: " + prescription.getDuration());
        dialogDoctor.setText("Doctor: " + prescription.getDoctorName());
        dialogInstructions.setText("Instructions: " + (prescription.getInstructions() != null && !prescription.getInstructions().isEmpty() ? prescription.getInstructions() : "None"));
        
        // Set up buttons
        MaterialButton dispenseButton = dialogView.findViewById(R.id.dispenseButton);
        ImageButton closeButton = dialogView.findViewById(R.id.closeRFIDButton);
        
        AlertDialog dialog = prescriptionBuilder.create();
        
        // Dispense medication button
        dispenseButton.setOnClickListener(v -> {
            dispenseMedication(prescription, patient);
            dialog.dismiss();
        });
        
        // Close button
        closeButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    

    /**
     * Dispense medication for prescription
     */
    private void dispenseMedication(Prescription prescription, Patient patient) {
        // Check if medicine is available in stock
        Medicine medicine = databaseHelper.getMedicineByName(prescription.getMedication());
        
        if (medicine == null) {
            Toast.makeText(getContext(), "❌ Medicine not found in inventory: " + prescription.getMedication(), Toast.LENGTH_LONG).show();
            return;
        }
        
        if (!medicine.isInStock()) {
            Toast.makeText(getContext(), "❌ Medicine out of stock: " + prescription.getMedication(), Toast.LENGTH_LONG).show();
            return;
        }
        
        // Show dispensing confirmation
        AlertDialog.Builder dispenseBuilder = new AlertDialog.Builder(getContext());
        dispenseBuilder.setTitle("Dispense Medication");
        dispenseBuilder.setMessage("Dispense " + prescription.getMedication() + " to " + patient.getFullName() + "?\n\n" +
                                 "Current Stock: " + medicine.getStockQuantity() + " " + medicine.getUnit());
        
        dispenseBuilder.setPositiveButton("Dispense", (dialog, which) -> {
            // Deduct from stock (assuming 1 unit per prescription)
            int newStock = medicine.getStockQuantity() - 1;
            boolean stockUpdated = databaseHelper.updateMedicineStock(prescription.getMedication(), newStock);
            
            if (stockUpdated) {
                // Update prescription status to dispensed
                prescription.setStatus("Dispensed");
                boolean updated = databaseHelper.updatePrescription(prescription);
                
                if (updated) {
                    Toast.makeText(getContext(), "✅ Medication dispensed successfully!\nStock remaining: " + newStock + " " + medicine.getUnit(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "❌ Failed to update prescription status.", Toast.LENGTH_LONG).show();
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
        // Enable NFC foreground dispatch when fragment is visible
        if (nfcHelper != null && getActivity() != null) {
            nfcHelper.enableForegroundDispatch(getActivity());
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Disable NFC foreground dispatch when fragment is not visible
        if (nfcHelper != null && getActivity() != null) {
            nfcHelper.disableForegroundDispatch(getActivity());
        }
    }
    
    /**
     * Handle NFC intent from activity
     */
    public void handleNFCIntent(android.content.Intent intent) {
        if (nfcHelper != null) {
            nfcHelper.handleNFCIntent(intent);
        }
    }
    
    /**
     * Parse the NFC payload written by nurses to extract prescription metadata.
     */
    private java.util.Map<String, String> parseNfcPayload(String payload) {
        java.util.Map<String, String> dataMap = new java.util.HashMap<>();
        if (payload == null || payload.isEmpty()) {
            return dataMap;
        }
        String[] pairs = payload.split(";");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                dataMap.put(keyValue[0], keyValue[1]);
            }
        }
        return dataMap;
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



