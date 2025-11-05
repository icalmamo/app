package com.example.h_cas;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Prescription;

import java.util.ArrayList;
import java.util.List;

/**
 * PrescriptionVerificationFragment handles prescription verification for pharmacists
 */
public class PrescriptionVerificationFragment extends Fragment {

    private RecyclerView prescriptionsRecyclerView;
    private View emptyStateText; // Changed to View since it's a LinearLayout
    private MaterialButton verifyPrescriptionButton;
    private MaterialButton refreshPrescriptionsButton;
    private TextInputEditText patientNameInput;
    
    private HCasDatabaseHelper databaseHelper;
    private PrescriptionVerificationAdapter prescriptionAdapter;
    private List<Prescription> pendingPrescriptions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_prescription_verification, container, false);
            
            if (view == null) {
                return null;
            }
            
            initializeViews(view);
            initializeDatabase();
            setupRecyclerView();
            setupClickListeners();
            loadPendingPrescriptions();
            
            return view;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initializeViews(View view) {
        if (view == null) {
            return;
        }
        
        prescriptionsRecyclerView = view.findViewById(R.id.prescriptionsRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        verifyPrescriptionButton = view.findViewById(R.id.verifyPrescriptionButton);
        refreshPrescriptionsButton = view.findViewById(R.id.refreshPrescriptionsButton);
        patientNameInput = view.findViewById(R.id.prescriptionIdInput);
        
        pendingPrescriptions = new ArrayList<>();
    }

    private void initializeDatabase() {
        if (getContext() == null) {
            return;
        }
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupRecyclerView() {
        if (getContext() == null || prescriptionsRecyclerView == null) {
            return;
        }
        
        try {
            if (pendingPrescriptions == null) {
                pendingPrescriptions = new ArrayList<>();
            }
            
            prescriptionAdapter = new PrescriptionVerificationAdapter(pendingPrescriptions);
            prescriptionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            prescriptionsRecyclerView.setAdapter(prescriptionAdapter);
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error setting up prescriptions list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupClickListeners() {
        if (verifyPrescriptionButton != null) {
            verifyPrescriptionButton.setOnClickListener(v -> verifyPrescriptionByName());
        }
        if (refreshPrescriptionsButton != null) {
            refreshPrescriptionsButton.setOnClickListener(v -> loadPendingPrescriptions());
        }
    }

    private void loadPendingPrescriptions() {
        try {
            if (databaseHelper == null || pendingPrescriptions == null) {
                return;
            }
            
            pendingPrescriptions.clear();
            
            // Get all prescriptions from database
            List<Prescription> allPrescriptions = databaseHelper.getAllPrescriptions();
            
            if (allPrescriptions != null) {
                // Filter for pending prescriptions (not yet dispensed)
                for (Prescription prescription : allPrescriptions) {
                    if (prescription != null) {
                        String status = prescription.getStatus();
                        if ("Pending".equals(status) || status == null || status.isEmpty()) {
                            pendingPrescriptions.add(prescription);
                        }
                    }
                }
            }
            
            if (prescriptionAdapter != null) {
                prescriptionAdapter.notifyDataSetChanged();
            }
            updateEmptyState();
        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error loading prescriptions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
        }
    }

    private void updateEmptyState() {
        if (emptyStateText == null || prescriptionsRecyclerView == null) {
            return;
        }
        
        if (pendingPrescriptions == null || pendingPrescriptions.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            prescriptionsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            prescriptionsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void verifyPrescriptionByName() {
        if (getContext() == null || patientNameInput == null) {
            return;
        }
        
        try {
            String patientName = patientNameInput.getText() != null ? patientNameInput.getText().toString().trim() : "";
            
            if (patientName.isEmpty()) {
                Toast.makeText(getContext(), "Please enter patient name", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Find prescriptions by patient name (case-insensitive partial match)
            List<Prescription> matchingPrescriptions = findPrescriptionsByName(patientName);
            
            if (matchingPrescriptions == null || matchingPrescriptions.isEmpty()) {
                Toast.makeText(getContext(), "No prescriptions found for patient: " + patientName, Toast.LENGTH_SHORT).show();
            } else if (matchingPrescriptions.size() == 1) {
                // If only one match, show it directly
                showPrescriptionVerificationDialog(matchingPrescriptions.get(0));
            } else {
                // If multiple matches, show a selection dialog
                showPrescriptionSelectionDialog(matchingPrescriptions, patientName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error verifying prescription: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private List<Prescription> findPrescriptionsByName(String patientName) {
        if (patientName == null || pendingPrescriptions == null) {
            return new ArrayList<>();
        }
        
        List<Prescription> matchingPrescriptions = new ArrayList<>();
        String searchName = patientName.toLowerCase().trim();
        
        for (Prescription prescription : pendingPrescriptions) {
            if (prescription != null && prescription.getPatientName() != null) {
                String prescriptionPatientName = prescription.getPatientName().toLowerCase().trim();
                // Check if patient name contains the search term (partial match)
                if (prescriptionPatientName.contains(searchName) || searchName.contains(prescriptionPatientName)) {
                    matchingPrescriptions.add(prescription);
                }
            }
        }
        return matchingPrescriptions;
    }

    private void showPrescriptionSelectionDialog(List<Prescription> prescriptions, String patientName) {
        if (getContext() == null || prescriptions == null || prescriptions.isEmpty()) {
            return;
        }
        
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Multiple Prescriptions Found for: " + patientName);
            
            // Create a list of prescription display strings
            CharSequence[] items = new CharSequence[prescriptions.size()];
            for (int i = 0; i < prescriptions.size(); i++) {
                Prescription p = prescriptions.get(i);
                String prescriptionId = p.getPrescriptionId() != null ? p.getPrescriptionId() : "N/A";
                String medicine = p.getMedication() != null ? p.getMedication() : "N/A";
                String date = p.getCreatedDate() != null ? p.getCreatedDate() : "N/A";
                items[i] = "ID: " + prescriptionId + " - " + medicine + " (" + date + ")";
            }
            
            builder.setItems(items, (dialog, which) -> {
                if (which >= 0 && which < prescriptions.size()) {
                    showPrescriptionVerificationDialog(prescriptions.get(which));
                }
                dialog.dismiss();
            });
            
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error showing prescription selection: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPrescriptionVerificationDialog(Prescription prescription) {
        if (getContext() == null || prescription == null) {
            return;
        }
        
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Verify Prescription");

            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_prescription_verification, null);
            if (dialogView == null) {
                Toast.makeText(getContext(), "Error loading prescription details", Toast.LENGTH_SHORT).show();
                return;
            }
            builder.setView(dialogView);

            // Set prescription details with null checks
            TextView patientNameText = dialogView.findViewById(R.id.verificationPatientName);
            TextView medicineText = dialogView.findViewById(R.id.verificationMedicine);
            TextView dosageText = dialogView.findViewById(R.id.verificationDosage);
            TextView frequencyText = dialogView.findViewById(R.id.verificationFrequency);
            TextView durationText = dialogView.findViewById(R.id.verificationDuration);
            TextView doctorText = dialogView.findViewById(R.id.verificationDoctor);
            TextView instructionsText = dialogView.findViewById(R.id.verificationInstructions);
            TextView dateText = dialogView.findViewById(R.id.verificationDate);

            if (patientNameText != null) {
                patientNameText.setText("Patient: " + (prescription.getPatientName() != null ? prescription.getPatientName() : "N/A"));
            }
            if (medicineText != null) {
                medicineText.setText("Medicine: " + (prescription.getMedication() != null ? prescription.getMedication() : "N/A"));
            }
            if (dosageText != null) {
                dosageText.setText("Dosage: " + (prescription.getDosage() != null ? prescription.getDosage() : "N/A"));
            }
            if (frequencyText != null) {
                frequencyText.setText("Frequency: " + (prescription.getFrequency() != null ? prescription.getFrequency() : "N/A"));
            }
            if (durationText != null) {
                durationText.setText("Duration: " + (prescription.getDuration() != null ? prescription.getDuration() : "N/A"));
            }
            if (doctorText != null) {
                doctorText.setText("Doctor: " + (prescription.getDoctorName() != null ? prescription.getDoctorName() : "N/A"));
            }
            if (instructionsText != null) {
                instructionsText.setText("Instructions: " + (prescription.getInstructions() != null ? prescription.getInstructions() : "None"));
            }
            if (dateText != null) {
                dateText.setText("Date: " + (prescription.getCreatedDate() != null ? prescription.getCreatedDate() : "N/A"));
            }

            MaterialButton approveButton = dialogView.findViewById(R.id.approveButton);
            MaterialButton rejectButton = dialogView.findViewById(R.id.rejectButton);

            AlertDialog dialog = builder.create();

            if (approveButton != null) {
                approveButton.setOnClickListener(v -> {
                    try {
                        approvePrescription(prescription);
                        dialog.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error approving prescription: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            if (rejectButton != null) {
                rejectButton.setOnClickListener(v -> {
                    try {
                        rejectPrescription(prescription);
                        dialog.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error rejecting prescription: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error showing prescription dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void approvePrescription(Prescription prescription) {
        if (getContext() == null || prescription == null) {
            return;
        }
        
        try {
            // In a real implementation, you would update the prescription status in the database
            prescription.setStatus("Approved");
            
            // Remove from pending list
            if (pendingPrescriptions != null) {
                pendingPrescriptions.remove(prescription);
            }
            if (prescriptionAdapter != null) {
                prescriptionAdapter.notifyDataSetChanged();
            }
            updateEmptyState();
            
            Toast.makeText(getContext(), "✅ Prescription approved: " + (prescription.getPrescriptionId() != null ? prescription.getPrescriptionId() : "N/A"), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error approving prescription: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void rejectPrescription(Prescription prescription) {
        if (getContext() == null || prescription == null) {
            return;
        }
        
        try {
            // In a real implementation, you would update the prescription status in the database
            prescription.setStatus("Rejected");
            
            // Remove from pending list
            if (pendingPrescriptions != null) {
                pendingPrescriptions.remove(prescription);
            }
            if (prescriptionAdapter != null) {
                prescriptionAdapter.notifyDataSetChanged();
            }
            updateEmptyState();
            
            Toast.makeText(getContext(), "❌ Prescription rejected: " + (prescription.getPrescriptionId() != null ? prescription.getPrescriptionId() : "N/A"), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error rejecting prescription: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // RecyclerView Adapter for prescription verification
    private class PrescriptionVerificationAdapter extends RecyclerView.Adapter<PrescriptionVerificationAdapter.PrescriptionViewHolder> {
        private List<Prescription> prescriptions;

        public PrescriptionVerificationAdapter(List<Prescription> prescriptions) {
            this.prescriptions = prescriptions != null ? prescriptions : new ArrayList<>();
        }

        @NonNull
        @Override
        public PrescriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            try {
                if (parent == null) {
                    throw new IllegalArgumentException("Parent ViewGroup cannot be null");
                }
                
                android.content.Context context = parent.getContext();
                if (context == null) {
                    context = getContext();
                }
                
                if (context == null) {
                    throw new IllegalStateException("Cannot create view holder: no context available");
                }
                
                View view = LayoutInflater.from(context).inflate(R.layout.item_prescription_verification, parent, false);
                if (view == null) {
                    // Fallback to empty view if inflation fails
                    View emptyView = new View(context);
                    return new PrescriptionViewHolder(emptyView);
                }
                return new PrescriptionViewHolder(view);
            } catch (Exception e) {
                e.printStackTrace();
                // Return a basic view holder with empty view if layout inflation fails
                try {
                    android.content.Context context = null;
                    if (parent != null) {
                        context = parent.getContext();
                    }
                    if (context == null) {
                        context = getContext();
                    }
                    if (context != null) {
                        View emptyView = new View(context);
                        return new PrescriptionViewHolder(emptyView);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                // Absolute last resort - this should never happen in normal operation
                if (parent != null && parent.getContext() != null) {
                    return new PrescriptionViewHolder(new View(parent.getContext()));
                }
                // This should never execute, but provides a fallback
                throw new IllegalStateException("Cannot create PrescriptionViewHolder: no valid context");
            }
        }

        @Override
        public void onBindViewHolder(@NonNull PrescriptionViewHolder holder, int position) {
            if (prescriptions == null || position < 0) {
                return;
            }
            
            try {
                if (position >= prescriptions.size()) {
                    return;
                }
                
                Prescription prescription = prescriptions.get(position);
                if (holder != null && prescription != null) {
                    holder.bind(prescription);
                }
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            try {
                return prescriptions != null ? prescriptions.size() : 0;
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }

        class PrescriptionViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView cardView;
            private TextView prescriptionIdText;
            private TextView patientNameText;
            private TextView medicineText;
            private TextView doctorText;
            private TextView dateText;
            private MaterialButton verifyButton;

            public PrescriptionViewHolder(@NonNull View itemView) {
                super(itemView);
                try {
                    if (itemView != null) {
                        cardView = itemView.findViewById(R.id.verificationCardView);
                        prescriptionIdText = itemView.findViewById(R.id.verificationPrescriptionId);
                        patientNameText = itemView.findViewById(R.id.verificationPatientName);
                        medicineText = itemView.findViewById(R.id.verificationMedicine);
                        doctorText = itemView.findViewById(R.id.verificationDoctor);
                        dateText = itemView.findViewById(R.id.verificationDate);
                        verifyButton = itemView.findViewById(R.id.verifyButton);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Views will remain null, bind() method will handle it
                }
            }

            public void bind(Prescription prescription) {
                if (prescription == null || getContext() == null) {
                    return;
                }
                
                try {
                    // Safely set text with null checks
                    if (prescriptionIdText != null) {
                        prescriptionIdText.setText("ID: " + (prescription.getPrescriptionId() != null ? prescription.getPrescriptionId() : "N/A"));
                    }
                    if (patientNameText != null) {
                        patientNameText.setText("Patient: " + (prescription.getPatientName() != null ? prescription.getPatientName() : "N/A"));
                    }
                    if (medicineText != null) {
                        medicineText.setText("Medicine: " + (prescription.getMedication() != null ? prescription.getMedication() : "N/A"));
                    }
                    if (doctorText != null) {
                        doctorText.setText("Doctor: " + (prescription.getDoctorName() != null ? prescription.getDoctorName() : "N/A"));
                    }
                    if (dateText != null) {
                        dateText.setText("Date: " + (prescription.getCreatedDate() != null ? prescription.getCreatedDate() : "N/A"));
                    }

                    if (verifyButton != null) {
                        verifyButton.setOnClickListener(v -> {
                            try {
                                showPrescriptionVerificationDialog(prescription);
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Error showing prescription details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
        try {
            loadPendingPrescriptions();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        try {
            // Enable back navigation with safer implementation
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        try {
                            // Navigate back to dashboard
                            if (getActivity() instanceof PharmacistDashboardActivity) {
                                ((PharmacistDashboardActivity) getActivity()).loadFragment(new PharmacistDashboardFragment());
                                if (((PharmacistDashboardActivity) getActivity()).getSupportActionBar() != null) {
                                    ((PharmacistDashboardActivity) getActivity()).getSupportActionBar().setTitle("Pharmacist Dashboard");
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



