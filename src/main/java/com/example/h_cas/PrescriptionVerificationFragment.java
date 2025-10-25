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
    private TextView emptyStateText;
    private MaterialButton verifyPrescriptionButton;
    private MaterialButton refreshPrescriptionsButton;
    private TextInputEditText prescriptionIdInput;
    
    private HCasDatabaseHelper databaseHelper;
    private PrescriptionVerificationAdapter prescriptionAdapter;
    private List<Prescription> pendingPrescriptions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_prescription_verification, container, false);
        
        initializeViews(view);
        initializeDatabase();
        setupRecyclerView();
        setupClickListeners();
        loadPendingPrescriptions();
        
        return view;
    }

    private void initializeViews(View view) {
        prescriptionsRecyclerView = view.findViewById(R.id.prescriptionsRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        verifyPrescriptionButton = view.findViewById(R.id.verifyPrescriptionButton);
        refreshPrescriptionsButton = view.findViewById(R.id.refreshPrescriptionsButton);
        prescriptionIdInput = view.findViewById(R.id.prescriptionIdInput);
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupRecyclerView() {
        pendingPrescriptions = new ArrayList<>();
        prescriptionAdapter = new PrescriptionVerificationAdapter(pendingPrescriptions);
        prescriptionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        prescriptionsRecyclerView.setAdapter(prescriptionAdapter);
    }

    private void setupClickListeners() {
        verifyPrescriptionButton.setOnClickListener(v -> verifyPrescriptionById());
        refreshPrescriptionsButton.setOnClickListener(v -> loadPendingPrescriptions());
    }

    private void loadPendingPrescriptions() {
        pendingPrescriptions.clear();
        
        // Get all prescriptions from database
        List<Prescription> allPrescriptions = databaseHelper.getAllPrescriptions();
        
        // Filter for pending prescriptions (not yet dispensed)
        for (Prescription prescription : allPrescriptions) {
            if ("Pending".equals(prescription.getStatus()) || prescription.getStatus() == null) {
                pendingPrescriptions.add(prescription);
            }
        }
        
        prescriptionAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (pendingPrescriptions.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            prescriptionsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            prescriptionsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void verifyPrescriptionById() {
        String prescriptionId = prescriptionIdInput.getText().toString().trim();
        
        if (prescriptionId.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a prescription ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Find prescription by ID
        Prescription prescription = findPrescriptionById(prescriptionId);
        
        if (prescription != null) {
            showPrescriptionVerificationDialog(prescription);
        } else {
            Toast.makeText(getContext(), "Prescription not found: " + prescriptionId, Toast.LENGTH_SHORT).show();
        }
    }

    private Prescription findPrescriptionById(String prescriptionId) {
        for (Prescription prescription : pendingPrescriptions) {
            if (prescriptionId.equals(prescription.getPrescriptionId())) {
                return prescription;
            }
        }
        return null;
    }

    private void showPrescriptionVerificationDialog(Prescription prescription) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Verify Prescription");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_prescription_verification, null);
        builder.setView(dialogView);

        // Set prescription details
        TextView patientNameText = dialogView.findViewById(R.id.verificationPatientName);
        TextView medicineText = dialogView.findViewById(R.id.verificationMedicine);
        TextView dosageText = dialogView.findViewById(R.id.verificationDosage);
        TextView frequencyText = dialogView.findViewById(R.id.verificationFrequency);
        TextView durationText = dialogView.findViewById(R.id.verificationDuration);
        TextView doctorText = dialogView.findViewById(R.id.verificationDoctor);
        TextView instructionsText = dialogView.findViewById(R.id.verificationInstructions);
        TextView dateText = dialogView.findViewById(R.id.verificationDate);

        patientNameText.setText("Patient: " + prescription.getPatientName());
        medicineText.setText("Medicine: " + prescription.getMedication());
        dosageText.setText("Dosage: " + prescription.getDosage());
        frequencyText.setText("Frequency: " + prescription.getFrequency());
        durationText.setText("Duration: " + prescription.getDuration());
        doctorText.setText("Doctor: " + prescription.getDoctorName());
        instructionsText.setText("Instructions: " + (prescription.getInstructions() != null ? prescription.getInstructions() : "None"));
        dateText.setText("Date: " + prescription.getCreatedDate());

        MaterialButton approveButton = dialogView.findViewById(R.id.approveButton);
        MaterialButton rejectButton = dialogView.findViewById(R.id.rejectButton);

        AlertDialog dialog = builder.create();

        approveButton.setOnClickListener(v -> {
            approvePrescription(prescription);
            dialog.dismiss();
        });

        rejectButton.setOnClickListener(v -> {
            rejectPrescription(prescription);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void approvePrescription(Prescription prescription) {
        // In a real implementation, you would update the prescription status in the database
        prescription.setStatus("Approved");
        
        // Remove from pending list
        pendingPrescriptions.remove(prescription);
        prescriptionAdapter.notifyDataSetChanged();
        updateEmptyState();
        
        Toast.makeText(getContext(), "✅ Prescription approved: " + prescription.getPrescriptionId(), Toast.LENGTH_SHORT).show();
    }

    private void rejectPrescription(Prescription prescription) {
        // In a real implementation, you would update the prescription status in the database
        prescription.setStatus("Rejected");
        
        // Remove from pending list
        pendingPrescriptions.remove(prescription);
        prescriptionAdapter.notifyDataSetChanged();
        updateEmptyState();
        
        Toast.makeText(getContext(), "❌ Prescription rejected: " + prescription.getPrescriptionId(), Toast.LENGTH_SHORT).show();
    }

    // RecyclerView Adapter for prescription verification
    private class PrescriptionVerificationAdapter extends RecyclerView.Adapter<PrescriptionVerificationAdapter.PrescriptionViewHolder> {
        private List<Prescription> prescriptions;

        public PrescriptionVerificationAdapter(List<Prescription> prescriptions) {
            this.prescriptions = prescriptions;
        }

        @NonNull
        @Override
        public PrescriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prescription_verification, parent, false);
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
            private MaterialCardView cardView;
            private TextView prescriptionIdText;
            private TextView patientNameText;
            private TextView medicineText;
            private TextView doctorText;
            private TextView dateText;
            private MaterialButton verifyButton;

            public PrescriptionViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.verificationCardView);
                prescriptionIdText = itemView.findViewById(R.id.verificationPrescriptionId);
                patientNameText = itemView.findViewById(R.id.verificationPatientName);
                medicineText = itemView.findViewById(R.id.verificationMedicine);
                doctorText = itemView.findViewById(R.id.verificationDoctor);
                dateText = itemView.findViewById(R.id.verificationDate);
                verifyButton = itemView.findViewById(R.id.verifyButton);
            }

            public void bind(Prescription prescription) {
                prescriptionIdText.setText("ID: " + prescription.getPrescriptionId());
                patientNameText.setText("Patient: " + prescription.getPatientName());
                medicineText.setText("Medicine: " + prescription.getMedication());
                doctorText.setText("Doctor: " + prescription.getDoctorName());
                dateText.setText("Date: " + prescription.getCreatedDate());

                verifyButton.setOnClickListener(v -> showPrescriptionVerificationDialog(prescription));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
        loadPendingPrescriptions();
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



