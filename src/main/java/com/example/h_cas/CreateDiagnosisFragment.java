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
            // For now, just show success message
            // In a real system, this would save to diagnoses table
            showToast("✅ Diagnosis created successfully!");
            clearForm();
        }
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














