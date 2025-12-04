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
import com.example.h_cas.database.FirebaseRTDBHelper;
import com.example.h_cas.models.Prescription;
import com.example.h_cas.models.Employee;
import com.example.h_cas.models.Patient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * CreatePrescriptionFragment allows doctors to create prescriptions for patients.
 */
public class CreatePrescriptionFragment extends Fragment {

    private TextInputEditText patientIdInput;
    private TextInputEditText medicationInput;
    private TextInputEditText frequencyInput;
    private TextInputEditText durationInput;
    private TextInputEditText instructionsInput;
    private MaterialButton createPrescriptionButton;
    
    private HCasDatabaseHelper databaseHelper;
    private FirebaseRTDBHelper firebaseRTDBHelper;
    private String currentDoctorId;
    private String currentDoctorName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_prescription, container, false);
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
        
        // Get doctor information
        getCurrentDoctorInfo();
    }

    private void initializeViews(View view) {
        patientIdInput = view.findViewById(R.id.patientIdInput);
        medicationInput = view.findViewById(R.id.medicationInput);
        frequencyInput = view.findViewById(R.id.frequencyInput);
        durationInput = view.findViewById(R.id.durationInput);
        instructionsInput = view.findViewById(R.id.instructionsInput);
        createPrescriptionButton = view.findViewById(R.id.createPrescriptionButton);
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
        firebaseRTDBHelper = new FirebaseRTDBHelper(getContext());
    }

    private void getCurrentDoctorInfo() {
        // Get doctor information from parent activity
        if (getActivity() instanceof DoctorDashboardActivity) {
            DoctorDashboardActivity activity = (DoctorDashboardActivity) getActivity();
            currentDoctorId = activity.getCurrentDoctor().getEmployeeId();
            String firstName = activity.getCurrentDoctor().getFirstName();
            String lastName = activity.getCurrentDoctor().getLastName();
            currentDoctorName = firstName + " " + lastName;
        } else {
            // Fallback to default if not in DoctorDashboardActivity
            currentDoctorId = "DOC001";
            currentDoctorName = "Dr. Unknown";
        }
    }

    private void setupClickListeners() {
        createPrescriptionButton.setOnClickListener(v -> createPrescription());
    }

    private void createPrescription() {
        String patientId = getText(patientIdInput);
        String medication = getText(medicationInput);
        String frequency = getText(frequencyInput);
        String duration = getText(durationInput);
        String instructions = getText(instructionsInput);

        if (validateInputs(patientId, medication, frequency, duration)) {
            // Get patient name from Firebase RTDB (async)
            getPatientName(patientId, patientName -> {
                // Create prescription object
                Prescription prescription = new Prescription();
                prescription.setPrescriptionId("PRE" + System.currentTimeMillis());
                prescription.setPatientId(patientId);
                prescription.setPatientName(patientName);
                prescription.setMedication(medication);
                prescription.setDosage(""); // Empty dosage since field is removed
                prescription.setFrequency(frequency);
                prescription.setDuration(duration);
                prescription.setInstructions(instructions);
                prescription.setDoctorId(currentDoctorId);
                prescription.setDoctorName(currentDoctorName);
                prescription.setCreatedDate(getCurrentDateTime());
                prescription.setStatus("Active");
                
                // Save prescription to database
                boolean success = databaseHelper.addPrescription(prescription);
                
                if (success) {
                    showToast("✅ Prescription created successfully!");
                    clearForm();
                } else {
                    showToast("❌ Failed to create prescription. Please try again.");
                }
            });
        }
    }

    private boolean validateInputs(String patientId, String medication, String frequency, String duration) {
        if (patientId.isEmpty()) {
            showToast("Please enter Patient ID");
            return false;
        }
        
        if (medication.isEmpty()) {
            showToast("Please enter medication name");
            return false;
        }
        
        if (frequency.isEmpty()) {
            showToast("Please enter frequency");
            return false;
        }
        
        if (duration.isEmpty()) {
            showToast("Please enter duration");
            return false;
        }
        
        return true;
    }

    private void clearForm() {
        patientIdInput.setText("");
        medicationInput.setText("");
        frequencyInput.setText("");
        durationInput.setText("");
        instructionsInput.setText("");
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    private void getPatientName(String patientId, PatientNameCallback callback) {
        // Get patient name from Firebase RTDB (primary source)
        if (firebaseRTDBHelper != null) {
            firebaseRTDBHelper.getPatientById(patientId, patient -> {
                if (patient != null) {
                    String fullName = patient.getFirstName() + " " + patient.getLastName();
                    if (callback != null) callback.onResult(fullName);
                } else {
                    // Fallback to SQLite if not found in Firebase
                    if (databaseHelper != null) {
                        com.example.h_cas.models.Patient sqlitePatient = databaseHelper.getPatientById(patientId);
                        if (sqlitePatient != null) {
                            String fullName = sqlitePatient.getFirstName() + " " + sqlitePatient.getLastName();
                            if (callback != null) callback.onResult(fullName);
                        } else {
                            if (callback != null) callback.onResult("Unknown Patient");
                        }
                    } else {
                        if (callback != null) callback.onResult("Unknown Patient");
                    }
                }
            });
        } else {
            // Fallback to SQLite if Firebase not available
            if (databaseHelper != null) {
                com.example.h_cas.models.Patient patient = databaseHelper.getPatientById(patientId);
                if (patient != null) {
                    String fullName = patient.getFirstName() + " " + patient.getLastName();
                    if (callback != null) callback.onResult(fullName);
                } else {
                    if (callback != null) callback.onResult("Unknown Patient");
                }
            } else {
                if (callback != null) callback.onResult("Unknown Patient");
            }
        }
    }
    
    private interface PatientNameCallback {
        void onResult(String patientName);
    }
    
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}
