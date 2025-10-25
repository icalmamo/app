package com.example.h_cas;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;

/**
 * PatientRegistrationFragment handles patient registration functionality for nurses.
 * Allows nurses to register new patients into the healthcare system.
 */
public class PatientRegistrationFragment extends Fragment {

    // Personal Information Fields
    private com.google.android.material.textfield.TextInputEditText inputFirstName;
    private com.google.android.material.textfield.TextInputEditText inputLastName;
    private AutoCompleteTextView inputSuffix;
    private com.google.android.material.textfield.TextInputEditText inputFullAddress;
    private com.google.android.material.textfield.TextInputEditText inputDob;
    private com.google.android.material.textfield.TextInputEditText inputBirthPlace;
    private AutoCompleteTextView inputGender;
    private com.google.android.material.textfield.TextInputEditText inputAge;
    
    // Contact Information Fields
    private com.google.android.material.textfield.TextInputEditText inputPhoneNumber;
    private com.google.android.material.textfield.TextInputEditText inputEmail;
    
    // Health Information Fields
    private com.google.android.material.textfield.TextInputEditText inputAllergies;
    private com.google.android.material.textfield.TextInputEditText inputMedications;
    private com.google.android.material.textfield.TextInputEditText inputMedicalHistory;
    
    // Vital Signs Diagnostic Fields
    private com.google.android.material.textfield.TextInputEditText inputPulseRate;
    private com.google.android.material.textfield.TextInputEditText inputBloodPressure;
    private com.google.android.material.textfield.TextInputEditText inputTemperature;
    private com.google.android.material.textfield.TextInputEditText inputBloodSugar;
    private com.google.android.material.textfield.TextInputEditText inputPainScale;
    private com.google.android.material.textfield.TextInputEditText inputSymptomsDescription;
    
    // Emergency Contact Fields
    private com.google.android.material.textfield.TextInputEditText inputEmergencyName;
    private com.google.android.material.textfield.TextInputEditText inputEmergencyPhone;

    private com.google.android.material.button.MaterialButton buttonSavePatient;

    private com.example.h_cas.database.HCasDatabaseHelper databaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_registration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize patient registration functionality
        initializeViews(view);
        initializeDatabase(view);
        setupListeners();
    }

    /**
     * Initialize patient registration components
     */
    private void initializeViews(@NonNull View view) {
        // Personal Information
        inputFirstName = view.findViewById(R.id.inputFirstName);
        inputLastName = view.findViewById(R.id.inputLastName);
        inputSuffix = view.findViewById(R.id.inputSuffix);
        inputFullAddress = view.findViewById(R.id.inputFullAddress);
        inputDob = view.findViewById(R.id.inputDob);
        inputBirthPlace = view.findViewById(R.id.inputBirthPlace);
        inputGender = view.findViewById(R.id.inputGender);
        inputAge = view.findViewById(R.id.inputAge);
        
        // Contact Information
        inputPhoneNumber = view.findViewById(R.id.inputPhoneNumber);
        inputEmail = view.findViewById(R.id.inputEmail);
        
        // Health Information
        inputAllergies = view.findViewById(R.id.inputAllergies);
        inputMedications = view.findViewById(R.id.inputMedications);
        inputMedicalHistory = view.findViewById(R.id.inputMedicalHistory);
        
        // Vital Signs Diagnostic Fields
        inputPulseRate = view.findViewById(R.id.inputPulseRate);
        inputBloodPressure = view.findViewById(R.id.inputBloodPressure);
        inputTemperature = view.findViewById(R.id.inputTemperature);
        inputBloodSugar = view.findViewById(R.id.inputBloodSugar);
        inputPainScale = view.findViewById(R.id.inputPainScale);
        inputSymptomsDescription = view.findViewById(R.id.inputSymptomsDescription);
        
        // Emergency Contact
        inputEmergencyName = view.findViewById(R.id.inputEmergencyName);
        inputEmergencyPhone = view.findViewById(R.id.inputEmergencyPhone);

        buttonSavePatient = view.findViewById(R.id.buttonSavePatient);
        
        // Set up suffix dropdown
        setupSuffixDropdown();
        
        // Set up gender dropdown
        setupGenderDropdown();
        
        // Set up date picker
        setupDatePicker();
    }

    private void initializeDatabase(@NonNull View view) {
        databaseHelper = new com.example.h_cas.database.HCasDatabaseHelper(view.getContext());
    }

    /**
     * Set up suffix dropdown with common suffix options
     */
    private void setupSuffixDropdown() {
        String[] suffixOptions = {
            "Sr.", "Jr.", "II", "III", "IV", "MD", "PhD", "Dr.", "RN"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_dropdown_item_1line, suffixOptions);
        inputSuffix.setAdapter(adapter);
        inputSuffix.setThreshold(1); // Start showing suggestions after 1 character
    }

    /**
     * Set up gender dropdown with gender options
     */
    private void setupGenderDropdown() {
        String[] genderOptions = {
            "Male", "Female", "Other"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_dropdown_item_1line, genderOptions);
        inputGender.setAdapter(adapter);
        inputGender.setThreshold(1); // Start showing suggestions after 1 character
    }

    /**
     * Set up date picker for date of birth
     */
    private void setupDatePicker() {
        inputDob.setOnClickListener(v -> showDatePickerDialog());
        
        // Make the field clickable but prevent keyboard from showing
        inputDob.setFocusable(false);
        inputDob.setClickable(true);
    }

    /**
     * Show date picker dialog
     */
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
            (DatePicker datePicker, int selectedYear, int selectedMonth, int selectedDay) -> {
                // Format the date as YYYY-MM-DD
                String formattedDate = String.format("%04d-%02d-%02d", 
                    selectedYear, selectedMonth + 1, selectedDay);
                inputDob.setText(formattedDate);
            }, year - 25, month, day); // Default to 25 years ago
        
        // Set maximum date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        
        // Set minimum date to 120 years ago
        calendar.set(Calendar.YEAR, year - 120);
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        
        datePickerDialog.show();
    }

    private void setupListeners() {
        buttonSavePatient.setOnClickListener(v -> {
            // Show loading state
            buttonSavePatient.setEnabled(false);
            buttonSavePatient.setText("Saving...");
            
            // Perform save operation
            savePatient();
            
            // Reset button state after save
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                buttonSavePatient.setEnabled(true);
                buttonSavePatient.setText("Save Patient");
            }, 2000); // Reset after 2 seconds
        });
    }

    private void savePatient() {
        // Collect all form data
        String firstName = getText(inputFirstName);
        String lastName = getText(inputLastName);
        String suffix = getText(inputSuffix);
        String fullAddress = getText(inputFullAddress);
        String dob = getText(inputDob);
        String birthPlace = getText(inputBirthPlace);
        String gender = getText(inputGender);
        String age = getText(inputAge);
        String phoneNumber = getText(inputPhoneNumber);
        String email = getText(inputEmail);
        String allergies = getText(inputAllergies);
        String medications = getText(inputMedications);
        String medicalHistory = getText(inputMedicalHistory);
        
        // Vital Signs Diagnostic Data
        String pulseRate = getText(inputPulseRate);
        String bloodPressure = getText(inputBloodPressure);
        String temperature = getText(inputTemperature);
        String bloodSugar = getText(inputBloodSugar);
        String painScale = getText(inputPainScale);
        String symptomsDescription = getText(inputSymptomsDescription);
        
        // Emergency Contact
        String emergencyName = getText(inputEmergencyName);
        String emergencyPhone = getText(inputEmergencyPhone);

        // Validate required fields
        if (firstName.isEmpty() || lastName.isEmpty()) {
            showToast("First name and last name are required");
            return;
        }
        
        if (fullAddress.isEmpty()) {
            showToast("Full address is required");
            return;
        }
        
        if (dob.isEmpty()) {
            showToast("Date of birth is required");
            return;
        }
        
        if (birthPlace.isEmpty()) {
            showToast("Birth place is required");
            return;
        }
        
        if (gender.isEmpty()) {
            showToast("Gender is required");
            return;
        }
        
        if (age.isEmpty()) {
            showToast("Age is required");
            return;
        }
        
        if (phoneNumber.isEmpty()) {
            showToast("Cellphone number is required");
            return;
        }
        
        if (email.isEmpty()) {
            showToast("Email address is required");
            return;
        }

        // Create and populate patient object
        com.example.h_cas.models.Patient patient = new com.example.h_cas.models.Patient();
        patient.setPatientId(generatePatientId());
        
        // Personal Information
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setSuffix(suffix);
        String fullName = firstName + " " + lastName;
        if (!suffix.isEmpty()) {
            fullName += " " + suffix;
        }
        patient.setFullName(fullName);
        patient.setFullAddress(fullAddress);
        patient.setDateOfBirth(dob);
        patient.setBirthPlace(birthPlace);
        patient.setGender(gender);
        patient.setAge(age);
        
        // Contact Information
        patient.setPhoneNumber(phoneNumber);
        patient.setEmail(email);
        
        // Health Information
        patient.setAllergies(allergies);
        patient.setMedications(medications);
        patient.setMedicalHistory(medicalHistory);
        
        // Vital Signs Diagnostic Data
        patient.setPulseRate(pulseRate);
        patient.setBloodPressure(bloodPressure);
        patient.setTemperature(temperature);
        patient.setBloodSugar(bloodSugar);
        patient.setPainScale(painScale);
        patient.setSymptomsDescription(symptomsDescription);
        
        // Emergency Contact
        patient.setEmergencyContactName(emergencyName);
        patient.setEmergencyContactPhone(emergencyPhone);

        boolean inserted = databaseHelper.addPatient(patient);
        if (inserted) {
            // Show simple toast notification
            showToast("✅ Patient registered successfully!");
            clearForm();
            // Navigate to Patient Monitoring after registration
            if (getActivity() instanceof NurseDashboardActivity) {
                NurseDashboardActivity activity = (NurseDashboardActivity) getActivity();
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new PatientMonitoringFragment())
                        .commit();
                // Update toolbar title
                activity.getSupportActionBar().setTitle("Monitoring");
            }
        } else {
            showToast("Failed to save patient");
        }
    }

    private String getText(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String getText(AutoCompleteTextView autoCompleteTextView) {
        return autoCompleteTextView.getText() == null ? "" : autoCompleteTextView.getText().toString().trim();
    }

    private void clearForm() {
        // Clear Personal Information fields
        inputFirstName.setText("");
        inputLastName.setText("");
        inputSuffix.setText("");
        inputFullAddress.setText("");
        inputDob.setText("");
        inputBirthPlace.setText("");
        inputGender.setText("");
        inputAge.setText("");
        
        // Clear Contact Information fields
        inputPhoneNumber.setText("");
        inputEmail.setText("");
        
        // Clear Health Information fields
        inputAllergies.setText("");
        inputMedications.setText("");
        inputMedicalHistory.setText("");
        
        // Clear Vital Signs Diagnostic fields
        inputPulseRate.setText("");
        inputBloodPressure.setText("");
        inputTemperature.setText("");
        inputBloodSugar.setText("");
        inputPainScale.setText("");
        inputSymptomsDescription.setText("");
        
        // Clear Emergency Contact fields
        inputEmergencyName.setText("");
        inputEmergencyPhone.setText("");
    }

    private void showToast(String message) {
        android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
    }
    
    // Simple toast notification method for success feedback
    private void showSuccessToast() {
        showToast("✅ Patient registered successfully!");
    }

    private String generatePatientId() {
        String prefix = "PAT";
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        return prefix + unique;
    }
}
