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
import com.example.h_cas.models.Patient;
import com.example.h_cas.models.Prescription;

import java.util.ArrayList;
import java.util.List;

/**
 * RegisteredPatientsFragment displays all registered patients for doctors to view and manage.
 * Shows simplified patient cards with clickable functionality to view details.
 */
public class RegisteredPatientsFragment extends Fragment {

    private RecyclerView patientsRecyclerView;
    private TextView emptyStateTextView;
    private HCasDatabaseHelper databaseHelper;
    private PatientAdapter patientAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registered_patients, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        initializeDatabase();
        setupRecyclerView();
        loadPatients();
    }

    private void initializeViews(View view) {
        patientsRecyclerView = view.findViewById(R.id.patientsRecyclerView);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupRecyclerView() {
        patientAdapter = new PatientAdapter();
        patientsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        patientsRecyclerView.setAdapter(patientAdapter);
    }

    private void loadPatients() {
        // Get all patients
        List<Patient> allPatients = databaseHelper.getAllPatients();
        
        // Get all prescriptions to find patients who have received prescriptions
        List<Prescription> allPrescriptions = databaseHelper.getAllPrescriptions();
        
        // Create a set of patient IDs who have prescriptions
        List<String> patientsWithPrescriptions = new ArrayList<>();
        for (Prescription prescription : allPrescriptions) {
            if (!patientsWithPrescriptions.contains(prescription.getPatientId())) {
                patientsWithPrescriptions.add(prescription.getPatientId());
            }
        }
        
        // Filter out patients who have prescriptions (they should be in Patient History instead)
        List<Patient> patientsWithoutPrescriptions = new ArrayList<>();
        for (Patient patient : allPatients) {
            if (!patientsWithPrescriptions.contains(patient.getPatientId())) {
                patientsWithoutPrescriptions.add(patient);
            }
        }
        
        if (patientsWithoutPrescriptions.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            patientsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            patientsRecyclerView.setVisibility(View.VISIBLE);
            patientAdapter.setPatients(patientsWithoutPrescriptions);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPatients(); // Refresh when returning to this screen
    }

    // RecyclerView Adapter for patients
    private class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {
        private List<Patient> patients;

        public void setPatients(List<Patient> patients) {
            this.patients = patients;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_registered_patient, parent, false);
            return new PatientViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
            Patient patient = patients.get(position);
            holder.bind(patient);
        }

        @Override
        public int getItemCount() {
            return patients != null ? patients.size() : 0;
        }

        class PatientViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView cardView;
            private TextView patientIdText;
            private TextView patientNameText;

            public PatientViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.patientCardView);
                patientIdText = itemView.findViewById(R.id.patientIdText);
                patientNameText = itemView.findViewById(R.id.patientNameText);
            }

            public void bind(Patient patient) {
                // Basic Information
                patientIdText.setText("Patient ID: " + patient.getPatientId());
                
                String fullName = patient.getFirstName() + " " + patient.getLastName();
                if (patient.getSuffix() != null && !patient.getSuffix().isEmpty()) {
                    fullName += " " + patient.getSuffix();
                }
                patientNameText.setText(fullName);
                
                // Make entire card clickable
                cardView.setOnClickListener(v -> showPatientDetails(patient));
            }
            
            private void showPatientDetails(Patient patient) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                
                // Inflate custom dialog layout
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_patient_details, null);
                builder.setView(dialogView);
                
                // Set patient data
                TextView dialogPatientId = dialogView.findViewById(R.id.dialogPatientId);
                TextView dialogPatientName = dialogView.findViewById(R.id.dialogPatientName);
                TextView dialogPatientAge = dialogView.findViewById(R.id.dialogPatientAge);
                TextView dialogPatientGender = dialogView.findViewById(R.id.dialogPatientGender);
                TextView dialogPatientPhone = dialogView.findViewById(R.id.dialogPatientPhone);
                TextView dialogPatientEmail = dialogView.findViewById(R.id.dialogPatientEmail);
                TextView dialogPatientAddress = dialogView.findViewById(R.id.dialogPatientAddress);
                TextView dialogPatientBirthPlace = dialogView.findViewById(R.id.dialogPatientBirthPlace);
                TextView dialogPatientAllergies = dialogView.findViewById(R.id.dialogPatientAllergies);
                TextView dialogPatientMedications = dialogView.findViewById(R.id.dialogPatientMedications);
                TextView dialogPatientMedicalHistory = dialogView.findViewById(R.id.dialogPatientMedicalHistory);
                TextView dialogPatientVitals = dialogView.findViewById(R.id.dialogPatientVitals);
                TextView dialogPatientSymptoms = dialogView.findViewById(R.id.dialogPatientSymptoms);
                
                // Populate patient data
                dialogPatientId.setText(patient.getPatientId());
                
                String fullName = patient.getFirstName() + " " + patient.getLastName();
                if (patient.getSuffix() != null && !patient.getSuffix().isEmpty()) {
                    fullName += " " + patient.getSuffix();
                }
                dialogPatientName.setText(fullName);
                
                dialogPatientAge.setText(patient.getAge() != null ? patient.getAge() : "N/A");
                dialogPatientGender.setText(patient.getGender() != null ? patient.getGender() : "N/A");
                dialogPatientPhone.setText(patient.getPhone() != null ? patient.getPhone() : "N/A");
                dialogPatientEmail.setText(patient.getEmail() != null ? patient.getEmail() : "N/A");
                
                String address = patient.getFullAddress() != null ? patient.getFullAddress() : 
                               (patient.getAddress() != null ? patient.getAddress() : "N/A");
                dialogPatientAddress.setText(address);
                dialogPatientBirthPlace.setText(patient.getBirthPlace() != null ? patient.getBirthPlace() : "N/A");
                
                dialogPatientAllergies.setText(patient.getAllergies() != null ? patient.getAllergies() : "None");
                dialogPatientMedications.setText(patient.getMedications() != null ? patient.getMedications() : "None");
                dialogPatientMedicalHistory.setText(patient.getMedicalHistory() != null ? patient.getMedicalHistory() : "None");
                
                // Vital Signs
                String vitals = "";
                if (patient.getPulseRate() != null) vitals += "Pulse: " + patient.getPulseRate() + " | ";
                if (patient.getBloodPressure() != null) vitals += "BP: " + patient.getBloodPressure() + " | ";
                if (patient.getTemperature() != null) vitals += "Temp: " + patient.getTemperature() + "°C | ";
                if (patient.getBloodSugar() != null) vitals += "Sugar: " + patient.getBloodSugar() + " | ";
                if (patient.getPainScale() != null) vitals += "Pain: " + patient.getPainScale() + "/10";
                
                if (vitals.endsWith(" | ")) {
                    vitals = vitals.substring(0, vitals.length() - 3);
                }
                
                dialogPatientVitals.setText(vitals.isEmpty() ? "No vital signs recorded" : vitals);
                dialogPatientSymptoms.setText(patient.getSymptomsDescription() != null ? patient.getSymptomsDescription() : "No symptoms recorded");
                
                // Create dialog
                AlertDialog dialog = builder.create();
                
                // Set up button click listeners
                MaterialButton createPrescriptionButton = dialogView.findViewById(R.id.dialogCreatePrescriptionButton);
                MaterialButton createDiagnosisButton = dialogView.findViewById(R.id.dialogCreateDiagnosisButton);
                ImageButton headerCloseButton = dialogView.findViewById(R.id.dialogHeaderCloseButton);
                
                createPrescriptionButton.setOnClickListener(v -> {
                    dialog.dismiss();
                    navigateToCreatePrescription(patient);
                });
                
                createDiagnosisButton.setOnClickListener(v -> {
                    dialog.dismiss();
                    navigateToCreateDiagnosis(patient);
                });
                
                headerCloseButton.setOnClickListener(v -> dialog.dismiss());
                
                dialog.show();
            }
            
            private void navigateToCreatePrescription(Patient patient) {
                // Navigate to Create Prescription fragment
                if (getActivity() instanceof DoctorDashboardActivity) {
                    DoctorDashboardActivity activity = (DoctorDashboardActivity) getActivity();
                    CreatePrescriptionFragment prescriptionFragment = new CreatePrescriptionFragment();
                    
                    // Pass patient data to prescription fragment
                    Bundle args = new Bundle();
                    args.putString("PATIENT_ID", patient.getPatientId());
                    args.putString("PATIENT_NAME", patient.getFirstName() + " " + patient.getLastName());
                    prescriptionFragment.setArguments(args);
                    
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, prescriptionFragment)
                            .commit();
                    activity.getSupportActionBar().setTitle("Create Prescription");
                } else if (getActivity() instanceof NurseDashboardActivity) {
                    NurseDashboardActivity activity = (NurseDashboardActivity) getActivity();
                    CreatePrescriptionFragment prescriptionFragment = new CreatePrescriptionFragment();
                    
                    // Pass patient data to prescription fragment
                    Bundle args = new Bundle();
                    args.putString("PATIENT_ID", patient.getPatientId());
                    args.putString("PATIENT_NAME", patient.getFirstName() + " " + patient.getLastName());
                    prescriptionFragment.setArguments(args);
                    
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, prescriptionFragment)
                            .commit();
                    activity.getSupportActionBar().setTitle("Create Prescription");
                }
            }
            
            private void navigateToCreateDiagnosis(Patient patient) {
                // Navigate to Create Diagnosis fragment
                if (getActivity() instanceof DoctorDashboardActivity) {
                    DoctorDashboardActivity activity = (DoctorDashboardActivity) getActivity();
                    CreateDiagnosisFragment diagnosisFragment = new CreateDiagnosisFragment();
                    
                    // Pass patient data to diagnosis fragment
                    Bundle args = new Bundle();
                    args.putString("PATIENT_ID", patient.getPatientId());
                    args.putString("PATIENT_NAME", patient.getFirstName() + " " + patient.getLastName());
                    diagnosisFragment.setArguments(args);
                    
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, diagnosisFragment)
                            .commit();
                    activity.getSupportActionBar().setTitle("Create Diagnosis");
                } else if (getActivity() instanceof NurseDashboardActivity) {
                    NurseDashboardActivity activity = (NurseDashboardActivity) getActivity();
                    CreateDiagnosisFragment diagnosisFragment = new CreateDiagnosisFragment();
                    
                    // Pass patient data to diagnosis fragment
                    Bundle args = new Bundle();
                    args.putString("PATIENT_ID", patient.getPatientId());
                    args.putString("PATIENT_NAME", patient.getFirstName() + " " + patient.getLastName());
                    diagnosisFragment.setArguments(args);
                    
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, diagnosisFragment)
                            .commit();
                    activity.getSupportActionBar().setTitle("Create Diagnosis");
                }
            }
            
            private void showToast(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
