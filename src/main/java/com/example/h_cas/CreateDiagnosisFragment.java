package com.example.h_cas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Patient;

/**
 * CreateDiagnosisFragment allows doctors to create diagnoses for patients.
 */
public class CreateDiagnosisFragment extends Fragment {

    private TextInputEditText patientIdInput;
    private TextInputEditText diagnosisInput;
    private TextInputEditText symptomsInput;
    private TextInputEditText treatmentPlanInput;
    private TextInputEditText followUpInput;
    private TextInputEditText notesInput;
    private MaterialButton createDiagnosisButton;
    
    private HCasDatabaseHelper databaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_diagnosis, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        initializeDatabase();
        setupClickListeners();
        
        // Get patient data from arguments if available
        Bundle args = getArguments();
        if (args != null) {
            String patientId = args.getString("PATIENT_ID");
            String patientName = args.getString("PATIENT_NAME");
            if (patientId != null) {
                patientIdInput.setText(patientId);
            }
        }
    }

    private void initializeViews(View view) {
        patientIdInput = view.findViewById(R.id.patientIdInput);
        diagnosisInput = view.findViewById(R.id.diagnosisInput);
        symptomsInput = view.findViewById(R.id.symptomsInput);
        treatmentPlanInput = view.findViewById(R.id.treatmentPlanInput);
        followUpInput = view.findViewById(R.id.followUpInput);
        notesInput = view.findViewById(R.id.notesInput);
        createDiagnosisButton = view.findViewById(R.id.createDiagnosisButton);
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupClickListeners() {
        createDiagnosisButton.setOnClickListener(v -> createDiagnosis());
    }

    private void createDiagnosis() {
        String patientId = getText(patientIdInput);
        String diagnosis = getText(diagnosisInput);
        String symptoms = getText(symptomsInput);
        String treatmentPlan = getText(treatmentPlanInput);
        String followUp = getText(followUpInput);
        String notes = getText(notesInput);

        if (validateInputs(patientId, diagnosis, symptoms, treatmentPlan)) {
            // Get patient from database
            com.example.h_cas.models.Patient patient = databaseHelper.getPatientById(patientId);
            if (patient == null) {
                showToast("❌ Patient not found. Please check Patient ID.");
                return;
            }
            
            // Update patient's medical history with diagnosis information
            StringBuilder medicalHistory = new StringBuilder();
            if (patient.getMedicalHistory() != null && !patient.getMedicalHistory().isEmpty()) {
                medicalHistory.append(patient.getMedicalHistory()).append("\n\n");
            }
            
            medicalHistory.append("=== DIAGNOSIS ===\n");
            medicalHistory.append("Date: ").append(getCurrentDateTime()).append("\n");
            medicalHistory.append("Diagnosis: ").append(diagnosis).append("\n");
            medicalHistory.append("Symptoms: ").append(symptoms).append("\n");
            medicalHistory.append("Treatment Plan: ").append(treatmentPlan).append("\n");
            if (followUp != null && !followUp.isEmpty()) {
                medicalHistory.append("Follow-up: ").append(followUp).append("\n");
            }
            if (notes != null && !notes.isEmpty()) {
                medicalHistory.append("Notes: ").append(notes).append("\n");
            }
            
            // Update patient's medical history in database
            patient.setMedicalHistory(medicalHistory.toString());
            
            // Update symptoms description as well
            if (patient.getSymptomsDescription() == null || patient.getSymptomsDescription().isEmpty()) {
                patient.setSymptomsDescription(symptoms);
            } else {
                patient.setSymptomsDescription(patient.getSymptomsDescription() + "\n" + symptoms);
            }
            
            // Save updated patient to database
            boolean success = databaseHelper.updatePatient(patient);
            
            if (success) {
                showToast("✅ Diagnosis recorded successfully!");
                clearForm();
            } else {
                showToast("❌ Failed to save diagnosis. Please try again.");
            }
            
            // Note: For better organization, consider creating a separate diagnoses table
            // For now, diagnosis information is stored in patient's medical history
        }
    }
    
    private String getCurrentDateTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    private boolean validateInputs(String patientId, String diagnosis, String symptoms, String treatmentPlan) {
        if (patientId.isEmpty()) {
            showToast("Please enter Patient ID");
            return false;
        }
        
        if (diagnosis.isEmpty()) {
            showToast("Please enter diagnosis");
            return false;
        }
        
        if (symptoms.isEmpty()) {
            showToast("Please enter symptoms");
            return false;
        }
        
        if (treatmentPlan.isEmpty()) {
            showToast("Please enter treatment plan");
            return false;
        }
        
        return true;
    }

    private void clearForm() {
        patientIdInput.setText("");
        diagnosisInput.setText("");
        symptomsInput.setText("");
        treatmentPlanInput.setText("");
        followUpInput.setText("");
        notesInput.setText("");
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}















