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
import com.example.h_cas.utils.NFCHelper;

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
    private NFCHelper nfcHelper;
    private Prescription pendingPrescriptionForNFC; // Store prescription waiting for NFC tag
    private String pendingNfcPayload;

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
        nfcHelper = new NFCHelper(getContext());
        
        // Set up NFC scan listener
        nfcHelper.setNFCScanListener(new NFCHelper.NFCScanListener() {
            @Override
            public void onNFCTagDetected(String nfcUid, Tag tag) {
                handleNFCTagDetected(nfcUid, tag);
            }
            
            @Override
            public void onNFCWriteSuccess(String nfcUid) {
                showNfcScanResultDialog("NFC Write Successful", "Medication data saved to the NFC tag (" + nfcHelper.formatNFCUid(nfcUid) + ").");
            }
            
            @Override
            public void onNFCWriteError(String error) {
                showNfcScanResultDialog("NFC Write Failed", error);
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
    
    /**
     * Handle NFC tag detection - show patient information or link prescription
     */
    private void handleNFCTagDetected(String nfcUid, Tag tag) {
        if (nfcUid == null || nfcUid.isEmpty()) {
            showNfcScanResultDialog("NFC Scan Failed", "Invalid NFC tag detected. Please try again.");
            return;
        }
        
        // If there's a pending prescription for NFC linking, handle that first
        if (pendingPrescriptionForNFC != null) {
            if (pendingNfcPayload != null && tag != null) {
                boolean wrote = nfcHelper.writeNFCTag(tag, pendingNfcPayload);
                if (!wrote) {
                    return;
                }
            }
            handlePrescriptionNFCTagForLinking(nfcUid, pendingPrescriptionForNFC);
            pendingPrescriptionForNFC = null;
            pendingNfcPayload = null;
            if (getActivity() != null) {
                nfcHelper.disableForegroundDispatch(getActivity());
            }
            return;
        }
        
        // Otherwise, show patient information
        // Get patient by NFC UID
        Patient patient = databaseHelper.getPatientByNfcUid(nfcUid);
        
        if (patient == null) {
            showNfcScanResultDialog("NFC Scan Failed", "No patient record is linked to this NFC tag.");
            // Disable NFC foreground dispatch
            if (getActivity() != null) {
                nfcHelper.disableForegroundDispatch(getActivity());
            }
            return;
        }
        
        // Get patient's prescriptions
        List<Prescription> prescriptions = databaseHelper.getPrescriptionsByPatientId(patient.getPatientId());
        
        // Show patient information dialog
        showPatientInfoDialog(patient, prescriptions);
        
        // Disable NFC foreground dispatch
        if (getActivity() != null) {
            nfcHelper.disableForegroundDispatch(getActivity());
        }
    }
    
    /**
     * Show patient information dialog
     */
    private void showPatientInfoDialog(Patient patient, List<Prescription> prescriptions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Patient Information");
        
        StringBuilder message = new StringBuilder();
        message.append("NFC UID: ").append(nfcHelper.formatNFCUid(patient.getNfcUid())).append("\n\n");
        message.append("Patient: ").append(patient.getFullName()).append("\n");
        message.append("Patient ID: ").append(patient.getPatientId()).append("\n");
        message.append("Age: ").append(patient.getAge() != null ? patient.getAge() : "N/A").append("\n");
        message.append("Gender: ").append(patient.getGender() != null ? patient.getGender() : "N/A").append("\n");
        message.append("Phone: ").append(patient.getPhoneNumber() != null ? patient.getPhoneNumber() : "N/A").append("\n\n");
        message.append("Total Prescriptions: ").append(prescriptions != null ? prescriptions.size() : 0);
        
        builder.setMessage(message.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
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
        // Show loading state
        if (emptyStateTextView != null) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            emptyStateTextView.setText("Loading prescriptions...");
        }
        if (prescriptionsRecyclerView != null) {
            prescriptionsRecyclerView.setVisibility(View.GONE);
        }
        
        // Load prescriptions in background to avoid blocking UI
        com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
            try {
                List<Prescription> prescriptions = databaseHelper != null ? databaseHelper.getAllPrescriptions() : new ArrayList<>();
                
                // Update UI on main thread
                com.example.h_cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                    if (getContext() == null || getView() == null) {
                        return; // Fragment is detached
                    }
                    loadPrescriptionsIntoUI(prescriptions);
                });
            } catch (Exception e) {
                // Log error and show message on main thread
                android.util.Log.e("ViewPrescriptionsFragment", "Error loading prescriptions: " + e.getMessage(), e);
                com.example.h_cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                    if (getContext() == null || getView() == null) {
                        return; // Fragment is detached
                    }
                    if (emptyStateTextView != null) {
                        emptyStateTextView.setVisibility(View.VISIBLE);
                        emptyStateTextView.setText("Error loading prescriptions. Please try again.");
                    }
                    if (prescriptionsRecyclerView != null) {
                        prescriptionsRecyclerView.setVisibility(View.GONE);
                    }
                    Toast.makeText(getContext(), "Error loading prescriptions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void loadPrescriptionsIntoUI(List<Prescription> prescriptions) {
        if (prescriptions == null) {
            prescriptions = new ArrayList<>();
        }
        
        if (prescriptions.isEmpty()) {
            if (emptyStateTextView != null) {
                emptyStateTextView.setVisibility(View.VISIBLE);
                emptyStateTextView.setText("No prescriptions found.\n\nDoctors can create prescriptions for patients.");
            }
            if (prescriptionsRecyclerView != null) {
                prescriptionsRecyclerView.setVisibility(View.GONE);
            }
        } else {
            if (emptyStateTextView != null) {
                emptyStateTextView.setVisibility(View.GONE);
            }
            if (prescriptionsRecyclerView != null) {
                prescriptionsRecyclerView.setVisibility(View.VISIBLE);
            }
            if (prescriptionAdapter != null) {
                prescriptionAdapter.setPrescriptions(prescriptions);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPrescriptions(); // Refresh when returning to this screen
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
                
                // Update button text to reflect NFC
                if (rfidButton != null) {
                    rfidButton.setText("Link NFC Tag");
                }
                
                AlertDialog dialog = builder.create();
                
                // NFC Registration button
                rfidButton.setOnClickListener(v -> {
                    registerPatientWithRFID(prescription);
                    dialog.dismiss();
                });
                
                // Close button
                closeButton.setOnClickListener(v -> dialog.dismiss());
                
                dialog.show();
            }
            
            private void registerPatientWithRFID(Prescription prescription) {
                // Show NFC scanning dialog
                AlertDialog.Builder nfcBuilder = new AlertDialog.Builder(getContext());
                nfcBuilder.setTitle("NFC Tag Registration");
                nfcBuilder.setMessage("Please scan the patient's NFC tag to link it with this prescription.\n\nHold the NFC tag near your device.");
                
                nfcBuilder.setPositiveButton("Scan NFC Tag", (dialog, which) -> {
                    // Start NFC scanning process
                    startNFCScanning(prescription);
                });
                
                nfcBuilder.setNegativeButton("Cancel", (dialog, which) -> {
                    // Do nothing, just close dialog
                });
                
                nfcBuilder.show();
            }
            
            private void startNFCScanning(Prescription prescription) {
                // Check NFC availability first
                if (!nfcHelper.isNFCAvailable()) {
                    Toast.makeText(getContext(), "NFC is not available on this device", Toast.LENGTH_LONG).show();
                    return;
                }
                
                if (!nfcHelper.isNFCEnabled()) {
                    Toast.makeText(getContext(), "NFC is disabled. Please enable NFC in settings", Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Store prescription for NFC tag detection callback
                pendingPrescriptionForNFC = prescription;
                pendingNfcPayload = createNfcPayloadForPrescription(prescription);
                
                // Show scanning dialog
                AlertDialog.Builder scanningBuilder = new AlertDialog.Builder(getContext());
                scanningBuilder.setTitle("Scanning NFC Tag...");
                scanningBuilder.setMessage("Please hold the patient's NFC tag near your device.\n\nThe tag will be automatically detected.");
                scanningBuilder.setCancelable(true);
                scanningBuilder.setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(getContext(), "NFC scanning cancelled.", Toast.LENGTH_SHORT).show();
                    pendingPrescriptionForNFC = null;
                    pendingNfcPayload = null;
                    if (getActivity() != null) {
                        nfcHelper.disableForegroundDispatch(getActivity());
                    }
                });
                
                AlertDialog scanDialog = scanningBuilder.create();
                scanDialog.show();
                
                // Enable NFC foreground dispatch
                if (getActivity() != null) {
                    nfcHelper.enableForegroundDispatch(getActivity());
                }
            }
        }
    }
    
    /**
     * Handle NFC tag detection for prescription linking (outer class method)
     */
    private void handlePrescriptionNFCTagForLinking(String nfcUid, Prescription prescription) {
        if (nfcUid == null || nfcUid.isEmpty()) {
            showNfcScanResultDialog("NFC Scan Failed", "Invalid NFC tag detected. Please try again.");
            return;
        }
        
        // Get patient by NFC UID
        Patient patient = databaseHelper.getPatientByNfcUid(nfcUid);
        
        if (patient == null) {
            showNfcScanResultDialog("NFC Scan Failed", "No patient found for this NFC tag. Please register the patient first.");
            return;
        }
        
        // Verify patient matches prescription
        if (!patient.getPatientId().equals(prescription.getPatientId())) {
            showNfcScanResultDialog("NFC Scan Failed", "The scanned NFC tag belongs to a different patient.");
            return;
        }
        
        // Update patient NFC UID if not already set
        if (patient.getNfcUid() == null || !patient.getNfcUid().equals(nfcUid)) {
            boolean success = databaseHelper.updatePatientNfcUid(patient.getPatientId(), nfcUid);
            if (success) {
                showNfcScanResultDialog("NFC Scan Successful", "NFC tag linked to patient: " + patient.getFullName());
                showNFCDetails(nfcUid, prescription, patient);
            } else {
                showNfcScanResultDialog("NFC Scan Failed", "Failed to link NFC tag. Please try again.");
            }
        } else {
            showNfcScanResultDialog("NFC Scan Successful", "NFC tag already linked to patient: " + patient.getFullName());
            showNFCDetails(nfcUid, prescription, patient);
        }
    }
    
    /**
     * Show a simple alert dialog for NFC scan results to give user feedback.
     */
    private void showNfcScanResultDialog(String title, String message) {
        if (getContext() == null) {
            return;
        }
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
    
    /**
     * Create a compact payload describing the prescription so it can be written to an NFC tag.
     */
    private String createNfcPayloadForPrescription(Prescription prescription) {
        if (prescription == null) {
            return "";
        }
        String safeMedication = prescription.getMedication() != null ? prescription.getMedication().replace(";", " ") : "";
        String safeDosage = prescription.getDosage() != null ? prescription.getDosage().replace(";", " ") : "";
        String safeFrequency = prescription.getFrequency() != null ? prescription.getFrequency().replace(";", " ") : "";
        String safeDuration = prescription.getDuration() != null ? prescription.getDuration().replace(";", " ") : "";
        return "PRESCRIPTION_ID=" + prescription.getPrescriptionId() +
                ";PATIENT_ID=" + prescription.getPatientId() +
                ";MED=" + safeMedication +
                ";DOSAGE=" + safeDosage +
                ";FREQ=" + safeFrequency +
                ";DURATION=" + safeDuration;
    }
    
    private void showNFCDetails(String nfcUid, Prescription prescription, Patient patient) {
        AlertDialog.Builder detailsBuilder = new AlertDialog.Builder(getContext());
        detailsBuilder.setTitle("NFC Tag Successfully Linked");
        
        String message = "NFC Tag UID: " + nfcHelper.formatNFCUid(nfcUid) + "\n\n" +
                       "Patient: " + patient.getFullName() + "\n" +
                       "Patient ID: " + patient.getPatientId() + "\n\n" +
                       "Prescription:\n" +
                       "Medicine: " + prescription.getMedication() + "\n" +
                       "Dosage: " + prescription.getDosage() + "\n" +
                       "Frequency: " + prescription.getFrequency() + "\n" +
                       "Duration: " + prescription.getDuration() + "\n\n" +
                       "✅ NFC tag has been linked to patient.\n" +
                       "Pharmacist can now scan this NFC tag to dispense medication.";
        
        detailsBuilder.setMessage(message);
        detailsBuilder.setPositiveButton("OK", (dialog, which) -> {
            // Dialog dismissed
        });
        
        detailsBuilder.show();
    }
}
