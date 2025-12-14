package com.healthcare.cas;

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
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.Patterns;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * PatientRegistrationFragment handles patient registration functionality for nurses.
 * Allows nurses to register new patients into the healthcare system.
 */
public class PatientRegistrationFragment extends Fragment {

    // Personal Information Fields
    private com.google.android.material.textfield.TextInputEditText inputFirstName;
    private com.google.android.material.textfield.TextInputEditText inputLastName;
    private AutoCompleteTextView inputSuffix;
    private com.google.android.material.textfield.MaterialAutoCompleteTextView inputFullAddress;
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
    private AutoCompleteTextView inputPainScale;
    private com.google.android.material.textfield.TextInputEditText inputSymptomsDescription;
    
    // Emergency Contact Fields
    private com.google.android.material.textfield.TextInputEditText inputEmergencyName;
    private com.google.android.material.textfield.TextInputEditText inputEmergencyPhone;

    private com.google.android.material.button.MaterialButton buttonSavePatient;
    private com.google.android.material.button.MaterialButton buttonExistingPatient;
    private com.google.android.material.card.MaterialCardView patientHistoryCard;
    private android.widget.TextView historyFullName;
    private android.widget.TextView historyAge;
    private android.widget.TextView historyPrescriptions;
    private android.widget.TextView historyLastPrescription;
    private android.widget.TextView historyLastMedication;
    private android.widget.TextView historyPastHealth;

    private com.healthcare.cas.database.HCasDatabaseHelper databaseHelper;
    private com.healthcare.cas.database.FirebaseRTDBHelper firebaseRTDBHelper;
    private boolean isExistingPatient = false;
    private String existingPatientId = null;
    private com.healthcare.cas.utils.NFCHelper nfcHelper;
    private String scannedNfcUid = null;
    private android.widget.TextView nfcUidDisplay;
    private com.google.android.material.button.MaterialButton buttonScanNFC;
    
    // Email validation state
    private boolean isEmailValid = false;
    private boolean isEmailExisting = false;
    private boolean isEmailValidating = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_registration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Check if this is an existing patient registration
        if (getArguments() != null) {
            isExistingPatient = getArguments().getBoolean("IS_EXISTING_PATIENT", false);
            if (isExistingPatient) {
                existingPatientId = getArguments().getString("PATIENT_ID");
            }
        }
        
        // Initialize patient registration functionality
        initializeViews(view);
        initializeDatabase(view);
        initializeNFC();
        loadExistingPatientData();
        setupListeners();
    }
    
    /**
     * Load existing patient data if this is a re-registration
     */
    private void loadExistingPatientData() {
        if (!isExistingPatient || getArguments() == null) {
            return;
        }
        
        Bundle args = getArguments();
        
        // Try to load from Firebase first (primary source), then SQLite as fallback
        if (existingPatientId != null) {
            // Try Firebase first
            if (firebaseRTDBHelper != null) {
                android.util.Log.d("PatientRegistration", "🔄 Loading existing patient from Firebase: " + existingPatientId);
                firebaseRTDBHelper.getPatientById(existingPatientId, firebasePatient -> {
                    if (firebasePatient != null) {
                        // Load data from Firebase patient
                        loadPatientDataToForm(firebasePatient);
                        android.util.Log.d("PatientRegistration", "✅ Loaded existing patient data from Firebase: " + existingPatientId);
                        android.util.Log.d("PatientRegistration", "   Birth Place: '" + (firebasePatient.getBirthPlace() != null ? firebasePatient.getBirthPlace() : "null") + "'");
                        return;
                    } else {
                        // Firebase doesn't have it, try SQLite
                        android.util.Log.d("PatientRegistration", "⚠️ Patient not found in Firebase, trying SQLite: " + existingPatientId);
                        if (databaseHelper != null) {
                            com.healthcare.cas.models.Patient dbPatient = databaseHelper.getPatientById(existingPatientId);
                            if (dbPatient != null) {
                                loadPatientDataToForm(dbPatient);
                                android.util.Log.d("PatientRegistration", "✅ Loaded existing patient data from SQLite: " + existingPatientId);
                                android.util.Log.d("PatientRegistration", "   Birth Place: '" + (dbPatient.getBirthPlace() != null ? dbPatient.getBirthPlace() : "null") + "'");
                                
                                // Don't sync here - only sync when user clicks Save button
                                // This prevents empty data from being saved to Firebase
                                return;
                            }
                        }
                        // Fallback to Bundle if both fail
                        loadPatientDataFromBundle(args);
                    }
                });
            } else if (databaseHelper != null) {
                // Fallback to SQLite if Firebase helper not available
                com.healthcare.cas.models.Patient dbPatient = databaseHelper.getPatientById(existingPatientId);
                if (dbPatient != null) {
                    loadPatientDataToForm(dbPatient);
                    android.util.Log.d("PatientRegistration", "✅ Loaded existing patient data from SQLite: " + existingPatientId);
                    android.util.Log.d("PatientRegistration", "   Birth Place: '" + (dbPatient.getBirthPlace() != null ? dbPatient.getBirthPlace() : "null") + "'");
                    
                    // Don't sync here - only sync when user clicks Save button
                    // This prevents empty data from being saved to Firebase
                    return;
                }
            }
        }
        
        // Fallback to Bundle data if both Firebase and SQLite failed
        loadPatientDataFromBundle(args);
    }
    
    /**
     * Load patient data to form fields
     */
    private void loadPatientDataToForm(com.healthcare.cas.models.Patient patient) {
        if (patient == null) return;
        
        if (inputFirstName != null) inputFirstName.setText(patient.getFirstName() != null ? patient.getFirstName() : "");
        if (inputLastName != null) inputLastName.setText(patient.getLastName() != null ? patient.getLastName() : "");
        if (inputSuffix != null) inputSuffix.setText(patient.getSuffix() != null ? patient.getSuffix() : "");
        if (inputFullAddress != null) inputFullAddress.setText(patient.getFullAddress() != null ? patient.getFullAddress() : "");
        if (inputDob != null) inputDob.setText(patient.getDateOfBirth() != null ? patient.getDateOfBirth() : "");
        if (inputBirthPlace != null) inputBirthPlace.setText(patient.getBirthPlace() != null ? patient.getBirthPlace() : "");
        if (inputGender != null) inputGender.setText(patient.getGender() != null ? patient.getGender() : "");
        if (inputAge != null) inputAge.setText(patient.getAge() != null ? patient.getAge() : "");
        if (inputPhoneNumber != null) inputPhoneNumber.setText(patient.getPhoneNumber() != null ? patient.getPhoneNumber() : "");
        if (inputEmail != null) inputEmail.setText(patient.getEmail() != null ? patient.getEmail() : "");
        if (inputAllergies != null) inputAllergies.setText(patient.getAllergies() != null ? patient.getAllergies() : "");
        if (inputMedications != null) inputMedications.setText(patient.getMedications() != null ? patient.getMedications() : "");
        if (inputMedicalHistory != null) inputMedicalHistory.setText(patient.getMedicalHistory() != null ? patient.getMedicalHistory() : "");
        if (inputEmergencyName != null) inputEmergencyName.setText(patient.getEmergencyContactName() != null ? patient.getEmergencyContactName() : "");
        if (inputEmergencyPhone != null) inputEmergencyPhone.setText(patient.getEmergencyContactPhone() != null ? patient.getEmergencyContactPhone() : "");
        
        // Make personal info fields read-only for existing patients
        if (inputFirstName != null) inputFirstName.setEnabled(false);
        if (inputLastName != null) inputLastName.setEnabled(false);
        if (inputSuffix != null) inputSuffix.setEnabled(false);
        if (inputFullAddress != null) inputFullAddress.setEnabled(false);
        if (inputDob != null) inputDob.setEnabled(false);
        if (inputBirthPlace != null) inputBirthPlace.setEnabled(false);
        if (inputGender != null) inputGender.setEnabled(false);
        if (inputAge != null) inputAge.setEnabled(false);
        if (inputPhoneNumber != null) inputPhoneNumber.setEnabled(false);
        if (inputEmail != null) inputEmail.setEnabled(false);
        
        // Clear vital signs fields for new entry
        if (inputPulseRate != null) inputPulseRate.setText("");
        if (inputBloodPressure != null) inputBloodPressure.setText("");
        if (inputTemperature != null) inputTemperature.setText("");
        if (inputBloodSugar != null) inputBloodSugar.setText("");
        if (inputPainScale != null) inputPainScale.setText("");
        if (inputSymptomsDescription != null) inputSymptomsDescription.setText("");
    }
    
    /**
     * Load patient data from Bundle (fallback)
     */
    private void loadPatientDataFromBundle(Bundle args) {
        if (args == null) return;
        
        // Fallback to Bundle data if database load failed
        // Pre-fill personal information
        if (inputFirstName != null) inputFirstName.setText(args.getString("FIRST_NAME", ""));
        if (inputLastName != null) inputLastName.setText(args.getString("LAST_NAME", ""));
        if (inputSuffix != null) inputSuffix.setText(args.getString("SUFFIX", ""));
        if (inputFullAddress != null) inputFullAddress.setText(args.getString("ADDRESS", ""));
        if (inputDob != null) inputDob.setText(args.getString("DOB", ""));
        if (inputBirthPlace != null) inputBirthPlace.setText(args.getString("BIRTH_PLACE", ""));
        if (inputGender != null) inputGender.setText(args.getString("GENDER", ""));
        if (inputAge != null) inputAge.setText(args.getString("AGE", ""));
        
        // Pre-fill contact information
        if (inputPhoneNumber != null) inputPhoneNumber.setText(args.getString("PHONE", ""));
        if (inputEmail != null) inputEmail.setText(args.getString("EMAIL", ""));
        
        // Pre-fill health information (past records)
        if (inputAllergies != null) inputAllergies.setText(args.getString("ALLERGIES", ""));
        if (inputMedications != null) inputMedications.setText(args.getString("MEDICATIONS", ""));
        if (inputMedicalHistory != null) inputMedicalHistory.setText(args.getString("MEDICAL_HISTORY", ""));
        
        // Pre-fill emergency contact
        if (inputEmergencyName != null) inputEmergencyName.setText(args.getString("EMERGENCY_NAME", ""));
        if (inputEmergencyPhone != null) inputEmergencyPhone.setText(args.getString("EMERGENCY_PHONE", ""));
        
        // Clear vital signs fields for new entry
        if (inputPulseRate != null) inputPulseRate.setText("");
        if (inputBloodPressure != null) inputBloodPressure.setText("");
        if (inputTemperature != null) inputTemperature.setText("");
        if (inputBloodSugar != null) inputBloodSugar.setText("");
        if (inputPainScale != null) inputPainScale.setText("");
        if (inputSymptomsDescription != null) inputSymptomsDescription.setText("");
        
        // Make personal info fields read-only for existing patients
        if (inputFirstName != null) inputFirstName.setEnabled(false);
        if (inputLastName != null) inputLastName.setEnabled(false);
        if (inputSuffix != null) inputSuffix.setEnabled(false);
        if (inputFullAddress != null) inputFullAddress.setEnabled(false);
        if (inputDob != null) inputDob.setEnabled(false);
        if (inputBirthPlace != null) inputBirthPlace.setEnabled(false);
        if (inputGender != null) inputGender.setEnabled(false);
        if (inputAge != null) inputAge.setEnabled(false);
        if (inputPhoneNumber != null) inputPhoneNumber.setEnabled(false);
        if (inputEmail != null) inputEmail.setEnabled(false);
        
        // Display patient history records
        displayPatientHistory(args);
    }
    
    /**
     * Display patient history records in the history card
     */
    private void displayPatientHistory(Bundle args) {
        if (patientHistoryCard == null) {
            return;
        }
        
        // Show history card
        patientHistoryCard.setVisibility(android.view.View.VISIBLE);
        
        // Display history information
        String fullName = args.getString("FULL_NAME", "");
        String age = args.getString("AGE", "");
        int prescriptionCount = args.getInt("PRESCRIPTION_COUNT", 0);
        String lastPrescriptionDate = args.getString("LAST_PRESCRIPTION_DATE", "No prescriptions");
        String lastMedication = args.getString("LAST_MEDICATION", "N/A");
        String allergies = args.getString("ALLERGIES", "");
        String medications = args.getString("MEDICATIONS", "");
        String medicalHistory = args.getString("MEDICAL_HISTORY", "");
        
        // Build past health information string
        StringBuilder pastHealthInfo = new StringBuilder();
        if (!allergies.isEmpty()) {
            pastHealthInfo.append("Allergies: ").append(allergies);
        }
        if (!medications.isEmpty()) {
            if (pastHealthInfo.length() > 0) pastHealthInfo.append("\n");
            pastHealthInfo.append("Medications: ").append(medications);
        }
        if (!medicalHistory.isEmpty()) {
            if (pastHealthInfo.length() > 0) pastHealthInfo.append("\n");
            pastHealthInfo.append("Medical History: ").append(medicalHistory);
        }
        if (pastHealthInfo.length() == 0) {
            pastHealthInfo.append("No past health information recorded");
        }
        
        // Update history text views
        if (historyFullName != null) {
            historyFullName.setText("Full Name: " + fullName);
        }
        if (historyAge != null) {
            historyAge.setText("Age: " + age);
        }
        if (historyPrescriptions != null) {
            historyPrescriptions.setText("Total Prescriptions: " + prescriptionCount);
        }
        if (historyLastPrescription != null) {
            historyLastPrescription.setText("Last Prescription: " + lastPrescriptionDate);
        }
        if (historyLastMedication != null) {
            historyLastMedication.setText("Last Medication: " + lastMedication);
        }
        if (historyPastHealth != null) {
            historyPastHealth.setText("Past Health Information:\n" + pastHealthInfo.toString());
        }
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
        buttonExistingPatient = view.findViewById(R.id.buttonExistingPatient);
        
        // NFC scanning components (optional - may not exist in layout)
        // Try to find actual IDs if they exist (using reflection-safe approach)
        try {
            int buttonScanNFCId = getResources().getIdentifier("buttonScanNFC", "id", getContext().getPackageName());
            int nfcUidDisplayId = getResources().getIdentifier("nfcUidDisplay", "id", getContext().getPackageName());
            
            if (buttonScanNFCId != 0) {
                buttonScanNFC = view.findViewById(buttonScanNFCId);
            }
            if (nfcUidDisplayId != 0) {
                nfcUidDisplay = view.findViewById(nfcUidDisplayId);
            }
        } catch (Exception e) {
            // NFC components may not exist in layout yet - that's okay
            android.util.Log.d("PatientRegistration", "NFC components not found in layout - optional feature");
        }
        patientHistoryCard = view.findViewById(R.id.patientHistoryCard);
        historyFullName = view.findViewById(R.id.historyFullName);
        historyAge = view.findViewById(R.id.historyAge);
        historyPrescriptions = view.findViewById(R.id.historyPrescriptions);
        historyLastPrescription = view.findViewById(R.id.historyLastPrescription);
        historyLastMedication = view.findViewById(R.id.historyLastMedication);
        historyPastHealth = view.findViewById(R.id.historyPastHealth);
        
        // Set up suffix dropdown
        setupSuffixDropdown();
        
        // Set up gender dropdown
        setupGenderDropdown();
        
        // Set up pain scale dropdown
        setupPainScaleDropdown();
        
        // Set up address autocomplete
        setupAddressAutocomplete();
        
        // Set up date picker
        setupDatePicker();
        
        // Set up age field to be read-only
        setupAgeField();
        
        // Add listener to date of birth field to auto-calculate age
        setupDateOfBirthListener();
        
        // Set up phone number field with validation
        setupPhoneNumberField();
        
        // Set up emergency phone number field with same validation as phone number
        setupEmergencyPhoneField();
        
        // Set up email field with validation
        setupEmailField();
    }

    private void initializeDatabase(@NonNull View view) {
        databaseHelper = new com.healthcare.cas.database.HCasDatabaseHelper(view.getContext());
        firebaseRTDBHelper = new com.healthcare.cas.database.FirebaseRTDBHelper(view.getContext());
    }
    
    /**
     * Initialize NFC helper and set up NFC scanning
     */
    private void initializeNFC() {
        nfcHelper = new com.healthcare.cas.utils.NFCHelper(getContext());
        
        // Set up NFC scan listener
        nfcHelper.setNFCScanListener(new com.healthcare.cas.utils.NFCHelper.NFCScanListener() {
            @Override
            public void onNFCTagDetected(String nfcUid, android.nfc.Tag tag) {
                handleNFCTagDetected(nfcUid, tag);
            }
            
            @Override
            public void onNFCWriteSuccess(String nfcUid) {
                // Not used for patient registration
            }
            
            @Override
            public void onNFCWriteError(String error) {
                // Not used for patient registration
            }
            
            @Override
            public void onNFCReadSuccess(String nfcUid, String data) {
                // Not used for patient registration
            }
            
            @Override
            public void onNFCReadError(String error) {
                // Not used for patient registration
            }
        });
        
        // Set up NFC scan button if available
        if (buttonScanNFC != null) {
            buttonScanNFC.setOnClickListener(v -> startNFCScanning());
        }
    }
    
    /**
     * Start NFC scanning process
     */
    private void startNFCScanning() {
        if (nfcHelper == null) {
            showToast("NFC helper not initialized");
            return;
        }
        
        // Check NFC availability
        if (!nfcHelper.isNFCAvailable()) {
            showToast("NFC is not available on this device");
            return;
        }
        
        if (!nfcHelper.isNFCEnabled()) {
            showToast("NFC is disabled. Please enable NFC in settings");
            return;
        }
        
        // Show scanning dialog
        android.app.AlertDialog.Builder scanBuilder = new android.app.AlertDialog.Builder(getContext());
        scanBuilder.setTitle("Scan NFC Tag");
        scanBuilder.setMessage("Please hold the NFC tag near your device.\n\nThe tag will be automatically detected.");
        scanBuilder.setCancelable(true);
        scanBuilder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        
        android.app.AlertDialog scanDialog = scanBuilder.create();
        scanDialog.show();
        
        // Enable NFC foreground dispatch
        if (getActivity() != null) {
            nfcHelper.enableForegroundDispatch(getActivity());
        }
    }
    
    /**
     * Handle NFC tag detection
     */
    private void handleNFCTagDetected(String nfcUid, android.nfc.Tag tag) {
        if (nfcUid == null || nfcUid.isEmpty()) {
            showToast("Invalid NFC tag detected");
            return;
        }
        
        // Check if NFC UID is already assigned to another patient
        if (databaseHelper.isNfcUidAssigned(nfcUid, existingPatientId)) {
            showToast("This NFC tag is already assigned to another patient");
            return;
        }
        
        // Store scanned NFC UID
        scannedNfcUid = nfcUid;
        
        // Update UI to show scanned NFC UID
        if (nfcUidDisplay != null) {
            nfcUidDisplay.setText("NFC UID: " + nfcHelper.formatNFCUid(nfcUid));
            nfcUidDisplay.setVisibility(android.view.View.VISIBLE);
        }
        
        showToast("NFC tag scanned successfully: " + nfcHelper.formatNFCUid(nfcUid));
        
        // Disable NFC foreground dispatch
        if (getActivity() != null) {
            nfcHelper.disableForegroundDispatch(getActivity());
        }
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
     * Set up pain scale dropdown with values 1-10
     */
    private void setupPainScaleDropdown() {
        String[] painScaleOptions = {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_list_item_1, painScaleOptions);
        inputPainScale.setAdapter(adapter);
        inputPainScale.setThreshold(0); // Show all options immediately
        
        // Set click listener to show dropdown when clicked
        inputPainScale.setOnClickListener(v -> {
            inputPainScale.showDropDown();
        });
        
        // Also show dropdown when focused
        inputPainScale.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                inputPainScale.showDropDown();
            }
        });
    }

    /**
     * Set up address autocomplete with common address suggestions
     */
    private void setupAddressAutocomplete() {
        // Common Philippine addresses and locations
        String[] addressSuggestions = {
            "Manila, Metro Manila",
            "Quezon City, Metro Manila",
            "Makati City, Metro Manila",
            "Pasig City, Metro Manila",
            "Taguig City, Metro Manila",
            "Las Piñas City, Metro Manila",
            "Parañaque City, Metro Manila",
            "Muntinlupa City, Metro Manila",
            "Mandaluyong City, Metro Manila",
            "Marikina City, Metro Manila",
            "Caloocan City, Metro Manila",
            "Valenzuela City, Metro Manila",
            "Malabon City, Metro Manila",
            "Navotas City, Metro Manila",
            "San Juan City, Metro Manila",
            "Pasay City, Metro Manila",
            "Pateros, Metro Manila",
            "Cebu City, Cebu",
            "Davao City, Davao del Sur",
            "Iloilo City, Iloilo",
            "Bacolod City, Negros Occidental",
            "Baguio City, Benguet",
            "Cagayan de Oro City, Misamis Oriental",
            "Zamboanga City, Zamboanga del Sur",
            "Antipolo City, Rizal",
            "Calamba City, Laguna",
            "Los Baños, Laguna",
            "San Pablo City, Laguna",
            "Santa Rosa City, Laguna",
            "Biñan City, Laguna",
            "Cabuyao City, Laguna",
            "Sta. Cruz, Laguna",
            "Bay, Laguna",
            "Alaminos, Laguna",
            "Calauan, Laguna",
            "Liliw, Laguna",
            "Nagcarlan, Laguna",
            "Pagsanjan, Laguna",
            "Paete, Laguna",
            "Pila, Laguna",
            "Rizal, Laguna",
            "Victoria, Laguna",
            "Batangas City, Batangas",
            "Lipa City, Batangas",
            "Tanauan City, Batangas",
            "Cavite City, Cavite",
            "Tagaytay City, Cavite",
            "Dasmariñas City, Cavite",
            "Bacoor, Cavite",
            "Imus, Cavite",
            "General Trias, Cavite",
            "Trece Martires City, Cavite",
            "San Pedro, Laguna",
            "Muntinlupa, Metro Manila",
            "Alabang, Muntinlupa",
            "Ayala Alabang, Muntinlupa",
            "BF Homes, Parañaque",
            "BF Resort, Las Piñas",
            "Bel-Air, Makati",
            "BGC, Taguig",
            "Cubao, Quezon City",
            "Eastwood, Quezon City",
            "Greenhills, San Juan",
            "Makati CBD, Makati",
            "Ortigas Center, Pasig",
            "Rockwell, Makati",
            "UP Diliman, Quezon City",
            "UP Los Baños, Laguna"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_dropdown_item_1line, addressSuggestions);
        inputFullAddress.setAdapter(adapter);
        inputFullAddress.setThreshold(1); // Start showing suggestions after 1 character
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
     * Set up age field to be read-only (disabled)
     */
    private void setupAgeField() {
        if (inputAge != null) {
            inputAge.setFocusable(false);
            inputAge.setClickable(false);
            inputAge.setEnabled(false);
            // Set hint to indicate it's auto-calculated
            if (inputAge.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) inputAge.getParent())
                    .setHint("Age * (Auto-calculated)");
            }
        }
    }
    
    /**
     * Set up listener for date of birth field to auto-calculate age
     */
    private void setupDateOfBirthListener() {
        if (inputDob != null) {
            inputDob.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Not needed
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Not needed
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String dobText = s.toString().trim();
                    if (!dobText.isEmpty()) {
                        calculateAndSetAge(dobText);
                    } else {
                        // Clear age if date of birth is cleared
                        if (inputAge != null) {
                            inputAge.setText("");
                        }
                    }
                }
            });
        }
    }
    
    /**
     * Calculate age from date of birth and set it to the age field
     * @param dateOfBirth Date of birth in YYYY-MM-DD format
     */
    private void calculateAndSetAge(String dateOfBirth) {
        try {
            // Parse the date of birth
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            Date dobDate = sdf.parse(dateOfBirth);
            
            if (dobDate != null) {
                // Get current date
                Calendar today = Calendar.getInstance();
                Calendar dob = Calendar.getInstance();
                dob.setTime(dobDate);
                
                // Validate: Date of birth cannot be in the future
                if (dob.after(today)) {
                    // Future date - show error and clear fields
                    if (inputDob != null && inputDob.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                        ((com.google.android.material.textfield.TextInputLayout) inputDob.getParent())
                            .setError("Date of birth cannot be in the future");
                    }
                    if (inputAge != null) {
                        inputAge.setText("");
                    }
                    return;
                }
                
                // Clear error if date is valid
                if (inputDob != null && inputDob.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                    ((com.google.android.material.textfield.TextInputLayout) inputDob.getParent())
                        .setError(null);
                }
                
                // Calculate age
                int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
                
                // Adjust if birthday hasn't occurred this year
                if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                    age--;
                }
                
                // Set the calculated age
                if (inputAge != null && age >= 0) {
                    inputAge.setText(String.valueOf(age));
                } else if (inputAge != null) {
                    inputAge.setText("");
                }
            }
        } catch (ParseException e) {
            // Invalid date format, clear age field
            if (inputAge != null) {
                inputAge.setText("");
            }
            if (inputDob != null && inputDob.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) inputDob.getParent())
                    .setError("Invalid date format. Use YYYY-MM-DD");
            }
        } catch (Exception e) {
            // Any other error, clear age field
            if (inputAge != null) {
                inputAge.setText("");
            }
        }
    }

    /**
     * Set up phone number field with validation (11 digits, starts with 09, numbers only)
     */
    private void setupPhoneNumberField() {
        if (inputPhoneNumber != null) {
            // Set input type to number
            inputPhoneNumber.setInputType(InputType.TYPE_CLASS_PHONE);
            
            // Add input filter to limit to 11 digits and only numbers
            InputFilter[] filters = new InputFilter[] {
                new InputFilter.LengthFilter(11), // Maximum 11 digits
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end,
                                               Spanned dest, int dstart, int dend) {
                        // Only allow digits
                        for (int i = start; i < end; i++) {
                            if (!Character.isDigit(source.charAt(i))) {
                                return ""; // Reject non-digit characters
                            }
                        }
                        return null; // Accept the input
                    }
                }
            };
            inputPhoneNumber.setFilters(filters);
            
            // Add TextWatcher for real-time validation and auto-insert "09" prefix
            inputPhoneNumber.addTextChangedListener(new TextWatcher() {
                private boolean isUpdating = false;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Not needed
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Not needed
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isUpdating) {
                        return;
                    }
                    
                    String phoneNumber = s.toString().trim();
                    
                    // Auto-insert "09" prefix if user starts typing without it
                    if (!phoneNumber.isEmpty() && !phoneNumber.startsWith("09")) {
                        isUpdating = true;
                        // If user typed a digit that's not "0" or "9", prepend "09"
                        if (phoneNumber.length() == 1 && Character.isDigit(phoneNumber.charAt(0))) {
                            // User typed a single digit, prepend "09"
                            s.clear();
                            s.append("09");
                            s.append(phoneNumber);
                        } else if (phoneNumber.startsWith("0") && phoneNumber.length() > 1 && phoneNumber.charAt(1) != '9') {
                            // User typed "0" followed by non-9 digit, insert "9" after "0"
                            s.clear();
                            s.append("09");
                            s.append(phoneNumber.substring(1));
                        } else {
                            // User typed something else, prepend "09"
                            s.clear();
                            s.append("09");
                            s.append(phoneNumber);
                        }
                        isUpdating = false;
                    }
                    
                    // Prevent deletion of "09" prefix
                    if (phoneNumber.length() < 2 && !phoneNumber.isEmpty()) {
                        isUpdating = true;
                        s.clear();
                        s.append("09");
                        isUpdating = false;
                    }
                    
                    // Limit to 11 digits
                    if (phoneNumber.length() > 11) {
                        isUpdating = true;
                        s.delete(11, phoneNumber.length());
                        isUpdating = false;
                    }
                    
                    validatePhoneNumber(s.toString().trim());
                }
            });
            
            // Set initial "09" prefix when field gains focus
            inputPhoneNumber.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && inputPhoneNumber.getText() != null) {
                    String currentText = inputPhoneNumber.getText().toString().trim();
                    if (currentText.isEmpty()) {
                        inputPhoneNumber.setText("09");
                        inputPhoneNumber.setSelection(2); // Move cursor to end
                    } else if (!currentText.startsWith("09")) {
                        inputPhoneNumber.setText("09" + currentText);
                        inputPhoneNumber.setSelection(inputPhoneNumber.getText().length());
                    }
                }
            });
        }
    }
    
    /**
     * Set up emergency phone number field with validation (same as phone number - 11 digits, starts with 09, numbers only)
     */
    private void setupEmergencyPhoneField() {
        if (inputEmergencyPhone != null) {
            // Set input type to number
            inputEmergencyPhone.setInputType(InputType.TYPE_CLASS_PHONE);
            
            // Add input filter to limit to 11 digits and only numbers
            InputFilter[] filters = new InputFilter[] {
                new InputFilter.LengthFilter(11), // Maximum 11 digits
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end,
                                               Spanned dest, int dstart, int dend) {
                        // Only allow digits
                        for (int i = start; i < end; i++) {
                            if (!Character.isDigit(source.charAt(i))) {
                                return ""; // Reject non-digit characters
                            }
                        }
                        return null; // Accept the input
                    }
                }
            };
            inputEmergencyPhone.setFilters(filters);
            
            // Add TextWatcher for real-time validation and auto-insert "09" prefix
            inputEmergencyPhone.addTextChangedListener(new TextWatcher() {
                private boolean isUpdating = false;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Not needed
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Not needed
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isUpdating) {
                        return;
                    }
                    
                    String phoneNumber = s.toString().trim();
                    
                    // Auto-insert "09" prefix if user starts typing without it
                    if (!phoneNumber.isEmpty() && !phoneNumber.startsWith("09")) {
                        isUpdating = true;
                        // If user typed a digit that's not "0" or "9", prepend "09"
                        if (phoneNumber.length() == 1 && Character.isDigit(phoneNumber.charAt(0))) {
                            // User typed a single digit, prepend "09"
                            s.clear();
                            s.append("09");
                            s.append(phoneNumber);
                        } else if (phoneNumber.startsWith("0") && phoneNumber.length() > 1 && phoneNumber.charAt(1) != '9') {
                            // User typed "0" followed by non-9 digit, insert "9" after "0"
                            s.clear();
                            s.append("09");
                            s.append(phoneNumber.substring(1));
                        } else {
                            // User typed something else, prepend "09"
                            s.clear();
                            s.append("09");
                            s.append(phoneNumber);
                        }
                        isUpdating = false;
                    }
                    
                    // Prevent deletion of "09" prefix
                    if (phoneNumber.length() < 2 && !phoneNumber.isEmpty()) {
                        isUpdating = true;
                        s.clear();
                        s.append("09");
                        isUpdating = false;
                    }
                    
                    // Limit to 11 digits
                    if (phoneNumber.length() > 11) {
                        isUpdating = true;
                        s.delete(11, phoneNumber.length());
                        isUpdating = false;
                    }
                    
                    validateEmergencyPhoneNumber(s.toString().trim());
                }
            });
            
            // Set initial "09" prefix when field gains focus
            inputEmergencyPhone.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && inputEmergencyPhone.getText() != null) {
                    String currentText = inputEmergencyPhone.getText().toString().trim();
                    if (currentText.isEmpty()) {
                        inputEmergencyPhone.setText("09");
                        inputEmergencyPhone.setSelection(2); // Move cursor to end
                    } else if (!currentText.startsWith("09")) {
                        inputEmergencyPhone.setText("09" + currentText);
                        inputEmergencyPhone.setSelection(inputEmergencyPhone.getText().length());
                    }
                }
            });
        }
    }
    
    /**
     * Validate phone number format (11 digits, starts with 09)
     */
    private void validatePhoneNumber(String phoneNumber) {
        com.google.android.material.textfield.TextInputLayout phoneLayout = null;
        if (inputPhoneNumber != null && inputPhoneNumber.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
            phoneLayout = (com.google.android.material.textfield.TextInputLayout) inputPhoneNumber.getParent();
        }
        
        if (phoneNumber.isEmpty()) {
            if (phoneLayout != null) {
                phoneLayout.setError(null);
            }
            return;
        }
        
        // Check if it starts with 09
        if (!phoneNumber.startsWith("09")) {
            if (phoneLayout != null) {
                phoneLayout.setError("Phone number must start with 09");
            }
            return;
        }
        
        // Check if it's exactly 11 digits
        if (phoneNumber.length() != 11) {
            if (phoneLayout != null) {
                phoneLayout.setError("Phone number must be exactly 11 digits");
            }
            return;
        }
        
        // Valid phone number
        if (phoneLayout != null) {
            phoneLayout.setError(null);
        }
    }
    
    /**
     * Validate emergency phone number format (11 digits, starts with 09)
     */
    private void validateEmergencyPhoneNumber(String phoneNumber) {
        com.google.android.material.textfield.TextInputLayout phoneLayout = null;
        if (inputEmergencyPhone != null && inputEmergencyPhone.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
            phoneLayout = (com.google.android.material.textfield.TextInputLayout) inputEmergencyPhone.getParent();
        }
        
        if (phoneNumber.isEmpty()) {
            if (phoneLayout != null) {
                phoneLayout.setError(null);
            }
            return;
        }
        
        // Check if it starts with 09
        if (!phoneNumber.startsWith("09")) {
            if (phoneLayout != null) {
                phoneLayout.setError("Phone number must start with 09");
            }
            return;
        }
        
        // Check if it's exactly 11 digits
        if (phoneNumber.length() != 11) {
            if (phoneLayout != null) {
                phoneLayout.setError("Phone number must be exactly 11 digits");
            }
            return;
        }
        
        // Valid phone number
        if (phoneLayout != null) {
            phoneLayout.setError(null);
        }
    }

    /**
     * Set up email field with real-time validation
     */
    private void setupEmailField() {
        if (inputEmail != null) {
            // Set input type to email
            inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            
            // Add TextWatcher for real-time validation with debouncing
            inputEmail.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Not needed
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Not needed
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String email = s.toString().trim();
                    
                    // Reset validation state when email changes
                    isEmailValid = false;
                    isEmailExisting = false;
                    
                    // First do format validation
                    validateEmailFormat(email);
                    
                    // If format is valid, check if email exists (with debounce to avoid too many API calls)
                    if (isEmailValid && !email.isEmpty()) {
                        // Use debouncer to wait 1 second after user stops typing
                        com.healthcare.cas.utils.Debouncer.getInstance().debounce("email_validation", () -> {
                            if (getActivity() != null) {
                                validateEmailExists(email);
                            }
                        }, 1000); // 1 second delay
                    }
                }
            });
            
            // Add focus listener to validate when user leaves the field
            inputEmail.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    // User left the email field - validate if format is valid
                    String email = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";
                    if (!email.isEmpty() && isEmailValid && !isEmailValidating && !isEmailExisting) {
                        // Format is valid but existence hasn't been checked yet
                        validateEmailExists(email);
                    }
                }
            });
        }
    }
    
    /**
     * Validate email format (local validation only)
     */
    private void validateEmailFormat(String email) {
        com.google.android.material.textfield.TextInputLayout emailLayout = null;
        if (inputEmail != null && inputEmail.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
            emailLayout = (com.google.android.material.textfield.TextInputLayout) inputEmail.getParent();
        }
        
        if (email.isEmpty()) {
            isEmailValid = false;
            isEmailExisting = false;
            if (emailLayout != null) {
                emailLayout.setError(null);
            }
            return;
        }
        
        // Trim and normalize email
        email = email.trim().toLowerCase();
        
        // Stricter email regex pattern
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        
        if (!email.matches(emailPattern)) {
            isEmailValid = false;
            isEmailExisting = false;
            if (emailLayout != null) {
                emailLayout.setError("Please enter a valid email address");
            }
            return;
        }
        
        // Additional validation: Check for proper email structure
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex >= email.length() - 1) {
            isEmailValid = false;
            isEmailExisting = false;
            if (emailLayout != null) {
                emailLayout.setError("Please enter a valid email address");
            }
            return;
        }
        
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);
        
        // Local part validation
        if (localPart.length() > 64 || localPart.isEmpty() ||
            localPart.startsWith(".") || localPart.endsWith(".") ||
            localPart.startsWith("_") || localPart.endsWith("_") ||
            localPart.startsWith("-") || localPart.endsWith("-")) {
            isEmailValid = false;
            isEmailExisting = false;
            if (emailLayout != null) {
                emailLayout.setError("Email address is invalid");
            }
            return;
        }
        
        // Domain validation
        if (domainPart.length() > 253 || domainPart.isEmpty() ||
            !domainPart.contains(".") ||
            domainPart.startsWith(".") || domainPart.endsWith(".") ||
            domainPart.startsWith("-") || domainPart.endsWith("-")) {
            isEmailValid = false;
            isEmailExisting = false;
            if (emailLayout != null) {
                emailLayout.setError("Email domain is invalid");
            }
            return;
        }
        
        // TLD validation
        String[] domainParts = domainPart.split("\\.");
        if (domainParts.length < 2 || domainParts[domainParts.length - 1].length() < 2) {
            isEmailValid = false;
            isEmailExisting = false;
            if (emailLayout != null) {
                emailLayout.setError("Email must have a valid top-level domain (e.g., .com, .org)");
            }
            return;
        }
        
        // Format is valid, but we still need to check if email exists
        isEmailValid = true;
        // Don't clear error yet - wait for existence check
        // Show "Checking..." message
        if (emailLayout != null && !isEmailValidating) {
            emailLayout.setError("Checking if email exists...");
        }
    }
    
    /**
     * Validate if email account actually exists using API
     */
    private void validateEmailExists(String email) {
        if (email == null || email.isEmpty() || !isEmailValid) {
            return;
        }
        
        isEmailValidating = true;
        
        // Get emailLayout as final variable for use in lambda
        final com.google.android.material.textfield.TextInputLayout emailLayout;
        if (inputEmail != null && inputEmail.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
            emailLayout = (com.google.android.material.textfield.TextInputLayout) inputEmail.getParent();
        } else {
            emailLayout = null;
        }
        
        // Show checking message
        if (emailLayout != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                emailLayout.setError("Checking if email exists...");
            });
        }
        
        // Call API to validate email existence
        com.healthcare.cas.utils.EmailValidator.validateEmailExists(email, new com.healthcare.cas.utils.EmailValidator.EmailValidationCallback() {
            @Override
            public void onValidationComplete(boolean isValid, boolean isDeliverable, String errorMessage) {
                isEmailValidating = false;
                
                // Update state
                isEmailValid = isValid;
                isEmailExisting = isDeliverable;
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (emailLayout != null) {
                            if (isValid && isDeliverable) {
                                // Email is valid and exists - clear error (green/valid state)
                                emailLayout.setError(null);
                            } else {
                                // Email doesn't exist or is invalid - show error (red box)
                                String errorMsg = errorMessage != null ? errorMessage : "Email account does not exist";
                                emailLayout.setError(errorMsg);
                            }
                        }
                        
                        // If validation was triggered from save button, re-enable it
                        if (buttonSavePatient != null) {
                            buttonSavePatient.setEnabled(true);
                            buttonSavePatient.setText("Save Patient");
                            
                            // If email is valid and exists, user can try saving again
                            // (They need to click save button again)
                        }
                    });
                }
            }
        });
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
                // Age will be automatically calculated by the TextWatcher
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
            // Perform save operation (button state will be managed by savePatient method)
            savePatient();
        });
        
        buttonExistingPatient.setOnClickListener(v -> {
            // Navigate to Patient History with search enabled
            if (getActivity() instanceof NurseDashboardActivity) {
                NurseDashboardActivity activity = (NurseDashboardActivity) getActivity();
                PatientHistoryFragment historyFragment = new PatientHistoryFragment();
                // Pass a flag to enable search mode
                Bundle args = new Bundle();
                args.putBoolean("enable_search", true);
                historyFragment.setArguments(args);
                
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, historyFragment)
                        .addToBackStack(null)
                        .commit();
                // Update toolbar title
                activity.getSupportActionBar().setTitle("Search Patient History");
            }
        });
    }

    /**
     * Reset save button to normal state
     */
    private void resetSaveButton() {
        if (buttonSavePatient != null) {
            buttonSavePatient.setEnabled(true);
            buttonSavePatient.setText("Save Patient");
        }
    }
    
    private void savePatient() {
        // Ensure button is in normal state at start
        resetSaveButton();
        
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

        // Clear all previous errors first
        clearAllFieldErrors();
        
        boolean hasErrors = false;
        
        // Validate first name
        if (firstName.isEmpty()) {
            setFieldError(inputFirstName, "First name is required");
            showToast("❌ First name is required");
            hasErrors = true;
        } else if (firstName.length() < 2) {
            setFieldError(inputFirstName, "First name must be at least 2 characters");
            showToast("❌ First name must be at least 2 characters");
            hasErrors = true;
        } else if (!firstName.matches("^[a-zA-Z\\s'-]+$")) {
            setFieldError(inputFirstName, "First name can only contain letters");
            showToast("❌ First name can only contain letters");
            hasErrors = true;
        }
        
        // Validate last name
        if (lastName.isEmpty()) {
            setFieldError(inputLastName, "Last name is required");
            showToast("❌ Last name is required");
            hasErrors = true;
        } else if (lastName.length() < 2) {
            setFieldError(inputLastName, "Last name must be at least 2 characters");
            showToast("❌ Last name must be at least 2 characters");
            hasErrors = true;
        } else if (!lastName.matches("^[a-zA-Z\\s'-]+$")) {
            setFieldError(inputLastName, "Last name can only contain letters");
            showToast("❌ Last name can only contain letters");
            hasErrors = true;
        }
        
        // Validate full address
        if (fullAddress.isEmpty()) {
            setFieldError(inputFullAddress, "Full address is required");
            showToast("❌ Full address is required");
            hasErrors = true;
        } else if (fullAddress.length() < 10) {
            setFieldError(inputFullAddress, "Please provide a complete address (at least 10 characters)");
            showToast("❌ Please provide a complete address");
            hasErrors = true;
        }
        
        // Validate date of birth
        if (dob.isEmpty()) {
            setFieldError(inputDob, "Date of birth is required");
            showToast("❌ Date of birth is required");
            hasErrors = true;
        }
        
        if (hasErrors) {
            resetSaveButton();
            return;
        }
        
        // Validate date of birth is not in the future
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            Date dobDate = sdf.parse(dob);
            Date today = new Date();
            
            if (dobDate != null && dobDate.after(today)) {
                showToast("Date of birth cannot be in the future");
                if (inputDob != null && inputDob.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                    ((com.google.android.material.textfield.TextInputLayout) inputDob.getParent())
                        .setError("Date of birth cannot be in the future");
                }
                resetSaveButton();
                return;
            }
        } catch (ParseException e) {
            showToast("Invalid date of birth format");
            if (inputDob != null && inputDob.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) inputDob.getParent())
                    .setError("Invalid date format");
            }
            resetSaveButton();
            return;
        }
        
        // For existing patients, if birth place is empty but field is disabled, try to get from database
        if (birthPlace.isEmpty() && isExistingPatient && existingPatientId != null && databaseHelper != null) {
            com.healthcare.cas.models.Patient existingPatient = databaseHelper.getPatientById(existingPatientId);
            if (existingPatient != null && existingPatient.getBirthPlace() != null && !existingPatient.getBirthPlace().isEmpty()) {
                birthPlace = existingPatient.getBirthPlace();
                android.util.Log.d("PatientRegistration", "✅ Retrieved birth place from database: '" + birthPlace + "'");
                // Update the input field for display
                if (inputBirthPlace != null) {
                    inputBirthPlace.setText(birthPlace);
                }
            }
        }
        
        // Validate birth place
        if (birthPlace.isEmpty()) {
            setFieldError(inputBirthPlace, "Birth place is required");
            showToast("❌ Birth place is required");
            resetSaveButton();
            return;
        } else if (birthPlace.length() < 2) {
            setFieldError(inputBirthPlace, "Birth place must be at least 2 characters");
            showToast("❌ Please enter a valid birth place");
            resetSaveButton();
            return;
        }
        
        // Validate gender
        if (gender.isEmpty()) {
            showToast("❌ Gender is required");
            resetSaveButton();
            return;
        }
        
        // Validate age
        if (age.isEmpty()) {
            setFieldError(inputAge, "Age is required");
            showToast("❌ Age is required");
            resetSaveButton();
            return;
        } else {
            try {
                int ageValue = Integer.parseInt(age);
                if (ageValue < 0 || ageValue > 150) {
                    setFieldError(inputAge, "Age must be between 0 and 150");
                    showToast("❌ Please enter a valid age (0-150)");
                    resetSaveButton();
                    return;
                }
            } catch (NumberFormatException e) {
                setFieldError(inputAge, "Age must be a valid number");
                showToast("❌ Age must be a valid number");
                resetSaveButton();
                return;
            }
        }
        
        // Validate phone number
        if (phoneNumber.isEmpty()) {
            setFieldError(inputPhoneNumber, "Phone number is required");
            showToast("❌ Phone number is required");
            resetSaveButton();
            return;
        }
        
        // Validate phone number format
        if (!phoneNumber.matches("^[0-9]+$")) {
            setFieldError(inputPhoneNumber, "Phone number must contain only numbers");
            showToast("❌ Phone number must contain only numbers");
            resetSaveButton();
            return;
        }
        
        if (!phoneNumber.startsWith("09")) {
            setFieldError(inputPhoneNumber, "Phone number must start with 09");
            showToast("❌ Phone number must start with 09");
            resetSaveButton();
            return;
        }
        
        if (phoneNumber.length() != 11) {
            setFieldError(inputPhoneNumber, "Phone number must be exactly 11 digits");
            showToast("❌ Phone number must be exactly 11 digits");
            resetSaveButton();
            return;
        }
        
        // Validate email
        if (email.isEmpty()) {
            setFieldError(inputEmail, "Email address is required");
            showToast("❌ Email address is required");
            resetSaveButton();
            return;
        }
        
        // Validate email format with stricter validation
        email = email.trim().toLowerCase();
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        
        if (!email.matches(emailPattern)) {
            setFieldError(inputEmail, "Please enter a valid email address (e.g., name@example.com)");
            showToast("❌ Please enter a valid email address");
            resetSaveButton();
            return;
        }
        
        // Additional validation: Check for proper email structure
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex >= email.length() - 1) {
            showToast("Please enter a valid email address");
            if (inputEmail != null && inputEmail.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) inputEmail.getParent())
                    .setError("Please enter a valid email address");
            }
            resetSaveButton();
            return;
        }
        
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);
        
        // Validate local part
        if (localPart.isEmpty() || localPart.length() > 64 ||
            localPart.startsWith(".") || localPart.endsWith(".") ||
            localPart.startsWith("_") || localPart.endsWith("_") ||
            localPart.startsWith("-") || localPart.endsWith("-")) {
            showToast("Email address format is invalid");
            if (inputEmail != null && inputEmail.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) inputEmail.getParent())
                    .setError("Email address format is invalid");
            }
            resetSaveButton();
            return;
        }
        
        // Validate domain part
        if (domainPart.isEmpty() || domainPart.length() > 253 ||
            !domainPart.contains(".") ||
            domainPart.startsWith(".") || domainPart.endsWith(".") ||
            domainPart.startsWith("-") || domainPart.endsWith("-")) {
            showToast("Email domain is invalid");
            if (inputEmail != null && inputEmail.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) inputEmail.getParent())
                    .setError("Email domain is invalid");
            }
            resetSaveButton();
            return;
        }
        
        // Validate TLD
        String[] domainParts = domainPart.split("\\.");
        if (domainParts.length < 2 || domainParts[domainParts.length - 1].length() < 2) {
            showToast("Email must have a valid top-level domain (e.g., .com, .org)");
            if (inputEmail != null && inputEmail.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) inputEmail.getParent())
                    .setError("Email must have a valid top-level domain");
            }
            resetSaveButton();
            return;
        }
        
        // Check if email has been validated and exists
        if (!isEmailValid || !isEmailExisting) {
            // If email is still being validated, wait a bit
            if (isEmailValidating) {
                showToast("Please wait while we verify your email address");
                resetSaveButton();
                return;
            }
            
            // If format is valid but existence hasn't been checked yet, trigger validation
            if (isEmailValid && !isEmailValidating) {
                // Show loading state
                buttonSavePatient.setEnabled(false);
                buttonSavePatient.setText("Validating email...");
                
                // Trigger validation
                validateEmailExists(email);
                
                // Wait for validation to complete (will be handled in callback)
                // Set up a timeout to re-enable button if validation takes too long
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (isEmailValidating) {
                        resetSaveButton();
                        showToast("Email validation is taking longer than expected. Please try again.");
                    }
                }, 15000); // 15 second timeout
                
                return;
            }
            
            // Email validation failed
            String errorMsg = "Email account does not exist or is invalid. Please enter a valid email address.";
            showToast(errorMsg);
            if (inputEmail != null && inputEmail.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) inputEmail.getParent())
                    .setError(errorMsg);
            }
            resetSaveButton();
            return;
        }
        
        // Email is valid and exists, proceed with saving
        proceedWithSave();
    }
    
    /**
     * Proceeds with saving patient data after email validation passes
     */
    private void proceedWithSave() {
        // Show saving state
        buttonSavePatient.setEnabled(false);
        buttonSavePatient.setText("Saving...");
        
        // Collect all form data again (in case user changed something)
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
        
        // Validate emergency phone number format (if provided)
        if (!emergencyPhone.isEmpty()) {
            if (!emergencyPhone.startsWith("09")) {
                showToast("Emergency phone number must start with 09");
                if (inputEmergencyPhone != null && inputEmergencyPhone.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                    ((com.google.android.material.textfield.TextInputLayout) inputEmergencyPhone.getParent())
                        .setError("Phone number must start with 09");
                }
                resetSaveButton();
                return;
            }
            
            if (emergencyPhone.length() != 11) {
                showToast("Emergency phone number must be exactly 11 digits");
                if (inputEmergencyPhone != null && inputEmergencyPhone.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                    ((com.google.android.material.textfield.TextInputLayout) inputEmergencyPhone.getParent())
                        .setError("Phone number must be exactly 11 digits");
                }
                resetSaveButton();
                return;
            }
        }

        // Create and populate patient object
        com.healthcare.cas.models.Patient patient = new com.healthcare.cas.models.Patient();
        
        // Use existing patient ID if re-registering, otherwise generate new ID
        if (isExistingPatient && existingPatientId != null) {
            patient.setPatientId(existingPatientId);
        } else {
            patient.setPatientId(generatePatientId());
        }
        
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
        
        // Debug logging to verify data is being captured from form inputs
        android.util.Log.d("PatientRegistration", "📋 Form input values (before setting to patient object):");
        android.util.Log.d("PatientRegistration", "   Age (form): '" + age + "'");
        android.util.Log.d("PatientRegistration", "   Birth Place (form): '" + birthPlace + "'");
        android.util.Log.d("PatientRegistration", "   Full Name (form): '" + fullName + "'");
        android.util.Log.d("PatientRegistration", "   Allergies (form): '" + allergies + "'");
        android.util.Log.d("PatientRegistration", "   Medications (form): '" + medications + "'");
        android.util.Log.d("PatientRegistration", "   Medical History (form): '" + medicalHistory + "'");
        android.util.Log.d("PatientRegistration", "   Emergency Name (form): '" + emergencyName + "'");
        android.util.Log.d("PatientRegistration", "   Emergency Phone (form): '" + emergencyPhone + "'");
        
        // Verify values are actually set in patient object (read back immediately)
        android.util.Log.d("PatientRegistration", "📋 Patient object values (after setting):");
        android.util.Log.d("PatientRegistration", "   Age (patient.getAge()): '" + patient.getAge() + "'");
        android.util.Log.d("PatientRegistration", "   Birth Place (patient.getBirthPlace()): '" + patient.getBirthPlace() + "'");
        android.util.Log.d("PatientRegistration", "   Full Name (patient.getFullName()): '" + patient.getFullName() + "'");
        android.util.Log.d("PatientRegistration", "   Allergies (patient.getAllergies()): '" + patient.getAllergies() + "'");
        android.util.Log.d("PatientRegistration", "   Medications (patient.getMedications()): '" + patient.getMedications() + "'");
        android.util.Log.d("PatientRegistration", "   Medical History (patient.getMedicalHistory()): '" + patient.getMedicalHistory() + "'");
        android.util.Log.d("PatientRegistration", "   Emergency Name (patient.getEmergencyContactName()): '" + patient.getEmergencyContactName() + "'");
        android.util.Log.d("PatientRegistration", "   Emergency Phone (patient.getEmergencyContactPhone()): '" + patient.getEmergencyContactPhone() + "'");
        
        // NFC UID (if scanned)
        if (scannedNfcUid != null && !scannedNfcUid.isEmpty()) {
            patient.setNfcUid(scannedNfcUid);
        }
        
        // Set created date for new patients (preserve existing date for updates)
        if (!isExistingPatient || existingPatientId == null) {
            // New patient - set current date/time as registration date
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            String registrationDate = sdf.format(new java.util.Date());
            patient.setCreatedDate(registrationDate);
            android.util.Log.d("PatientRegistration", "📅 New patient registered with date: " + registrationDate);
        } else {
            // Existing patient - preserve original registration date
            // Load existing patient to get original created_date
            com.healthcare.cas.models.Patient existingPatient = databaseHelper.getPatientById(existingPatientId);
            if (existingPatient != null && existingPatient.getCreatedDate() != null && !existingPatient.getCreatedDate().isEmpty()) {
                patient.setCreatedDate(existingPatient.getCreatedDate());
                android.util.Log.d("PatientRegistration", "📅 Preserving existing registration date: " + existingPatient.getCreatedDate());
            } else {
                // If no existing date found, use current date (shouldn't happen, but safety fallback)
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                patient.setCreatedDate(sdf.format(new java.util.Date()));
                android.util.Log.w("PatientRegistration", "⚠️ No existing date found, using current date");
            }
        }

        boolean success = false;
        try {
            // Build payload map from form values BEFORE saving to database
            Map<String, Object> formDataPayload = buildFormDataPayload(patient, 
                firstName, lastName, suffix, fullAddress, dob, birthPlace, gender, age,
                phoneNumber, email, allergies, medications, medicalHistory,
                pulseRate, bloodPressure, temperature, bloodSugar, painScale, symptomsDescription,
                emergencyName, emergencyPhone, fullName);
            
            if (isExistingPatient && existingPatientId != null) {
                // Update existing patient with new vital signs
                // NOTE: We'll sync to Firebase directly, so disable automatic sync from SQLite
                success = databaseHelper.updatePatient(patient);
                if (success) {
                    showToast("✅ Patient updated successfully with new vital signs!");
                } else {
                    showToast("Failed to update patient");
                    resetSaveButton();
                    return;
                }
            } else {
                // Add new patient
                // NOTE: We'll sync to Firebase directly, so we need to prevent the automatic sync
                // from HCasDatabaseHelper.addPatient() which uses incomplete data
                // We'll save to SQLite first, then sync complete data to Firebase
                success = databaseHelper.addPatient(patient);
                if (success) {
                    showToast("✅ Patient registered successfully!");
                } else {
                    showToast("Failed to save patient");
                    resetSaveButton();
                    return;
                }
            }
            
            if (success) {
                // Push COMPLETE data to Firebase directly (this will overwrite any incomplete sync)
                // This ensures all fields are saved, including vital signs
                android.util.Log.d("PatientRegistration", "🔄 Syncing COMPLETE patient data to Firebase...");
                syncPatientToFirebase(patient, formDataPayload);
                
                // Wait a bit to ensure Firebase write completes before clearing form
                // This prevents the background sync from HCasDatabaseHelper from overwriting
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    clearForm();
                }, 500); // 500ms delay to ensure Firebase write completes
                
                // Stay on registration screen - do not navigate away
                // Reset button state after successful save
                resetSaveButton();
            }
        } catch (Exception e) {
            android.util.Log.e("PatientRegistration", "❌ Error saving patient: " + e.getMessage(), e);
            showToast("An error occurred while saving patient. Please try again.");
            resetSaveButton();
        }
    }

    private String getText(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String getText(AutoCompleteTextView autoCompleteTextView) {
        return autoCompleteTextView.getText() == null ? "" : autoCompleteTextView.getText().toString().trim();
    }

    private String getText(com.google.android.material.textfield.MaterialAutoCompleteTextView autoCompleteTextView) {
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

    /**
     * Helper method to get value or empty string (never null)
     */
    private String getValueOrEmpty(String value) {
        return value != null ? value : "";
    }
    
    /**
     * Build payload map from form data values to ensure all fields are included
     */
    private Map<String, Object> buildFormDataPayload(com.healthcare.cas.models.Patient patient, 
            String firstName, String lastName, String suffix, String fullAddress,
            String dob, String birthPlace, String gender, String age,
            String phoneNumber, String email, String allergies, String medications, String medicalHistory,
            String pulseRate, String bloodPressure, String temperature, String bloodSugar, 
            String painScale, String symptomsDescription,
            String emergencyName, String emergencyPhone, String fullName) {
        
        Map<String, Object> payload = new HashMap<>();
        
        // Personal Information
        payload.put("patient_id", getValueOrEmpty(patient.getPatientId()));
        payload.put("first_name", getValueOrEmpty(firstName));
        payload.put("last_name", getValueOrEmpty(lastName));
        payload.put("full_name", getValueOrEmpty(fullName));
        payload.put("suffix", getValueOrEmpty(suffix));
        payload.put("date_of_birth", getValueOrEmpty(dob));
        payload.put("birth_place", getValueOrEmpty(birthPlace));
        payload.put("gender", getValueOrEmpty(gender));
        payload.put("age", getValueOrEmpty(age));
        
        // Contact Information
        payload.put("address", getValueOrEmpty(fullAddress));
        payload.put("full_address", getValueOrEmpty(fullAddress));
        payload.put("phone", getValueOrEmpty(phoneNumber));
        payload.put("phone_number", getValueOrEmpty(phoneNumber));
        payload.put("email", getValueOrEmpty(email));
        
        // Health Information
        payload.put("allergies", getValueOrEmpty(allergies));
        payload.put("medications", getValueOrEmpty(medications));
        payload.put("medical_history", getValueOrEmpty(medicalHistory));
        
        // Vital Signs
        payload.put("pulse_rate", getValueOrEmpty(pulseRate));
        payload.put("blood_pressure", getValueOrEmpty(bloodPressure));
        payload.put("temperature", getValueOrEmpty(temperature));
        payload.put("blood_sugar", getValueOrEmpty(bloodSugar));
        payload.put("pain_scale", getValueOrEmpty(painScale));
        payload.put("symptoms_description", getValueOrEmpty(symptomsDescription));
        
        // Emergency Contact
        payload.put("emergency_contact_name", getValueOrEmpty(emergencyName));
        payload.put("emergency_contact_phone", getValueOrEmpty(emergencyPhone));
        
        // System Information
        if (patient.getNfcUid() != null && !patient.getNfcUid().isEmpty()) {
            payload.put("nfc_uid", patient.getNfcUid());
        }
        // Set patient_status = "on" for new patients (or when saving existing patient)
        payload.put("patient_status", "on");
        
        // Log the payload being built
        android.util.Log.d("PatientRegistration", "📦 Building payload from form data:");
        android.util.Log.d("PatientRegistration", "   age: '" + getValueOrEmpty(age) + "'");
        android.util.Log.d("PatientRegistration", "   birth_place: '" + getValueOrEmpty(birthPlace) + "'");
        android.util.Log.d("PatientRegistration", "   full_name: '" + getValueOrEmpty(fullName) + "'");
        android.util.Log.d("PatientRegistration", "   allergies: '" + getValueOrEmpty(allergies) + "'");
        android.util.Log.d("PatientRegistration", "   medications: '" + getValueOrEmpty(medications) + "'");
        android.util.Log.d("PatientRegistration", "   medical_history: '" + getValueOrEmpty(medicalHistory) + "'");
        android.util.Log.d("PatientRegistration", "   emergency_contact_name: '" + getValueOrEmpty(emergencyName) + "'");
        android.util.Log.d("PatientRegistration", "   emergency_contact_phone: '" + getValueOrEmpty(emergencyPhone) + "'");
        
        return payload;
    }
    
    /**
     * Helper method to set error on a TextInputEditText or MaterialAutoCompleteTextView field
     */
    private void setFieldError(android.view.View editText, String errorMessage) {
        if (editText != null && editText.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
            ((com.google.android.material.textfield.TextInputLayout) editText.getParent()).setError(errorMessage);
        }
    }
    
    /**
     * Clear all field errors
     */
    private void clearAllFieldErrors() {
        setFieldError(inputFirstName, null);
        setFieldError(inputLastName, null);
        setFieldError(inputFullAddress, null);
        setFieldError(inputDob, null);
        setFieldError(inputBirthPlace, null);
        setFieldError(inputAge, null);
        setFieldError(inputPhoneNumber, null);
        setFieldError(inputEmail, null);
        setFieldError(inputEmergencyName, null);
        setFieldError(inputEmergencyPhone, null);
        setFieldError(inputPulseRate, null);
        setFieldError(inputBloodPressure, null);
        setFieldError(inputTemperature, null);
        setFieldError(inputBloodSugar, null);
        setFieldError(inputSymptomsDescription, null);
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

    /**
     * Directly sync the patient to Firebase Realtime Database after local insert.
     * This ensures newly registered patients immediately appear in the cloud DB.
     * @param patient The patient object with system information
     * @param formDataPayload The payload map built directly from form fields to ensure all data is included
     */
    /**
     * Sync patient to Firebase from Patient object (for existing patients loaded from SQLite)
     */
    private void syncPatientToFirebaseFromPatient(com.healthcare.cas.models.Patient patient) {
        if (patient == null || patient.getPatientId() == null || patient.getPatientId().isEmpty()) {
            android.util.Log.w("PatientRegistration", "⚠️ Cannot sync patient to Firebase: patient or patient ID is null");
            return;
        }
        
        try {
            // Build payload from Patient object
            Map<String, Object> payload = new HashMap<>();
            payload.put("patient_id", getValueOrEmpty(patient.getPatientId()));
            payload.put("first_name", getValueOrEmpty(patient.getFirstName()));
            payload.put("last_name", getValueOrEmpty(patient.getLastName()));
            payload.put("full_name", getValueOrEmpty(patient.getFullName()));
            payload.put("suffix", getValueOrEmpty(patient.getSuffix()));
            payload.put("date_of_birth", getValueOrEmpty(patient.getDateOfBirth()));
            payload.put("birth_place", getValueOrEmpty(patient.getBirthPlace()));
            payload.put("gender", getValueOrEmpty(patient.getGender()));
            payload.put("age", getValueOrEmpty(patient.getAge()));
            payload.put("address", getValueOrEmpty(patient.getFullAddress()));
            payload.put("full_address", getValueOrEmpty(patient.getFullAddress()));
            payload.put("phone", getValueOrEmpty(patient.getPhoneNumber()));
            payload.put("phone_number", getValueOrEmpty(patient.getPhoneNumber()));
            payload.put("email", getValueOrEmpty(patient.getEmail()));
            payload.put("allergies", getValueOrEmpty(patient.getAllergies()));
            payload.put("medications", getValueOrEmpty(patient.getMedications()));
            payload.put("medical_history", getValueOrEmpty(patient.getMedicalHistory()));
            payload.put("pulse_rate", getValueOrEmpty(patient.getPulseRate()));
            payload.put("blood_pressure", getValueOrEmpty(patient.getBloodPressure()));
            payload.put("temperature", getValueOrEmpty(patient.getTemperature()));
            payload.put("blood_sugar", getValueOrEmpty(patient.getBloodSugar()));
            payload.put("pain_scale", getValueOrEmpty(patient.getPainScale()));
            payload.put("symptoms_description", getValueOrEmpty(patient.getSymptomsDescription()));
            payload.put("emergency_contact_name", getValueOrEmpty(patient.getEmergencyContactName()));
            payload.put("emergency_contact_phone", getValueOrEmpty(patient.getEmergencyContactPhone()));
            payload.put("nfc_uid", getValueOrEmpty(patient.getNfcUid()));
            payload.put("created_date", patient.getCreatedDate() != null ? patient.getCreatedDate() : 
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
            payload.put("last_updated", System.currentTimeMillis());
            // Set patient_status = "on" when saving existing patient (makes them available for prescription again)
            payload.put("patient_status", "on");
            
            // Sync to Firebase
            syncPatientToFirebase(patient, payload);
        } catch (Exception e) {
            android.util.Log.e("PatientRegistration", "❌ Error syncing patient to Firebase: " + e.getMessage(), e);
        }
    }
    
    private void syncPatientToFirebase(com.healthcare.cas.models.Patient patient, Map<String, Object> formDataPayload) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/");
            // Use "patients" path consistently
            DatabaseReference patientRef = database.getReference("patients").child(patient.getPatientId());
            patientRef.keepSynced(false);

            // Use the payload passed from proceedWithSave (built directly from form fields)
            Map<String, Object> payload = new HashMap<>(formDataPayload);
            
            // Log payload values to verify what's being sent
            android.util.Log.d("PatientRegistration", "📤 Payload values being sent to Firebase:");
            android.util.Log.d("PatientRegistration", "   age: '" + payload.get("age") + "'");
            android.util.Log.d("PatientRegistration", "   birth_place: '" + payload.get("birth_place") + "'");
            android.util.Log.d("PatientRegistration", "   full_name: '" + payload.get("full_name") + "'");
            android.util.Log.d("PatientRegistration", "   allergies: '" + payload.get("allergies") + "'");
            android.util.Log.d("PatientRegistration", "   medications: '" + payload.get("medications") + "'");
            android.util.Log.d("PatientRegistration", "   medical_history: '" + payload.get("medical_history") + "'");
            android.util.Log.d("PatientRegistration", "   emergency_contact_name: '" + payload.get("emergency_contact_name") + "'");
            android.util.Log.d("PatientRegistration", "   emergency_contact_phone: '" + payload.get("emergency_contact_phone") + "'");
            android.util.Log.d("PatientRegistration", "📤 Syncing patient to Firebase: " + patient.getPatientId());
            // Check if patient exists first to preserve created_date
            patientRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    boolean patientExists = snapshot.exists();
                    boolean dataChanged = false;
                    
                    if (!patientExists) {
                        // New patient - set created_date and last_updated
                        payload.put("created_date", patient.getCreatedDate() != null ? patient.getCreatedDate() : 
                            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                        dataChanged = true; // New patient always counts as change
                    } else {
                        // Patient exists - preserve existing created_date and check if data changed
                        Map<String, Object> existingData = (Map<String, Object>) snapshot.getValue();
                        if (existingData != null) {
                            if (existingData.containsKey("created_date")) {
                                payload.put("created_date", existingData.get("created_date"));
                            }
                            
                            // Compare data to detect actual changes (exclude last_updated and created_date from comparison)
                            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                                String key = entry.getKey();
                                if (!key.equals("last_updated") && !key.equals("created_date")) {
                                    Object newValue = entry.getValue();
                                    Object oldValue = existingData.get(key);
                                    
                                    // Compare values (handle null cases)
                                    if (newValue == null && oldValue != null) {
                                        dataChanged = true;
                                        break;
                                    } else if (newValue != null && !newValue.equals(oldValue)) {
                                        dataChanged = true;
                                        break;
                                    }
                                }
                            }
                        } else {
                            // No existing data - treat as new
                            dataChanged = true;
                        }
                    }
                    
                    // Always update to ensure all fields are saved (use setValue for complete overwrite)
                    payload.put("last_updated", System.currentTimeMillis());
                    
                    // Log final payload before sending to Firebase
                    android.util.Log.d("PatientRegistration", "📦 Final payload before Firebase setValue():");
                    android.util.Log.d("PatientRegistration", "   age: '" + payload.get("age") + "'");
                    android.util.Log.d("PatientRegistration", "   birth_place: '" + payload.get("birth_place") + "'");
                    android.util.Log.d("PatientRegistration", "   full_name: '" + payload.get("full_name") + "'");
                    android.util.Log.d("PatientRegistration", "   allergies: '" + payload.get("allergies") + "'");
                    android.util.Log.d("PatientRegistration", "   medications: '" + payload.get("medications") + "'");
                    android.util.Log.d("PatientRegistration", "   medical_history: '" + payload.get("medical_history") + "'");
                    android.util.Log.d("PatientRegistration", "   emergency_contact_name: '" + payload.get("emergency_contact_name") + "'");
                    android.util.Log.d("PatientRegistration", "   emergency_contact_phone: '" + payload.get("emergency_contact_phone") + "'");
                    android.util.Log.d("PatientRegistration", "   Total payload size: " + payload.size() + " fields");
                    
                    if (!patientExists) {
                        // New patient - use setValue() to ensure ALL fields are saved
                        android.util.Log.d("PatientRegistration", "🆕 New patient - using setValue() to save all fields");
                        patientRef.setValue(payload)
                            .addOnSuccessListener(unused -> {
                                android.util.Log.d("PatientRegistration", "✅ Patient synced to Firebase successfully (new patient)");
                                // Verify the data was actually saved by reading it back
                                patientRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                    @Override
                                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            android.util.Log.d("PatientRegistration", "✅ Verification - Data saved to Firebase:");
                                            android.util.Log.d("PatientRegistration", "   age: '" + snapshot.child("age").getValue() + "'");
                                            android.util.Log.d("PatientRegistration", "   birth_place: '" + snapshot.child("birth_place").getValue() + "'");
                                            android.util.Log.d("PatientRegistration", "   full_name: '" + snapshot.child("full_name").getValue() + "'");
                                            android.util.Log.d("PatientRegistration", "   allergies: '" + snapshot.child("allergies").getValue() + "'");
                                            android.util.Log.d("PatientRegistration", "   medications: '" + snapshot.child("medications").getValue() + "'");
                                            android.util.Log.d("PatientRegistration", "   medical_history: '" + snapshot.child("medical_history").getValue() + "'");
                                            android.util.Log.d("PatientRegistration", "   emergency_contact_name: '" + snapshot.child("emergency_contact_name").getValue() + "'");
                                            android.util.Log.d("PatientRegistration", "   emergency_contact_phone: '" + snapshot.child("emergency_contact_phone").getValue() + "'");
                                        }
                                    }
                                    
                                    @Override
                                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                                        android.util.Log.e("PatientRegistration", "❌ Error verifying saved data", error.toException());
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("PatientRegistration", "❌ Firebase sync failed (new patient)", e);
                            });
                    } else {
                        // Existing patient - use setValue() to ensure ALL fields are updated (not just changed ones)
                        android.util.Log.d("PatientRegistration", "🔄 Existing patient - using setValue() to update all fields");
                        patientRef.setValue(payload)
                            .addOnSuccessListener(unused -> {
                                android.util.Log.d("PatientRegistration", "✅ Patient synced to Firebase successfully (updated)");
                                // Verify the data was actually saved by reading it back
                                patientRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                    @Override
                                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            android.util.Log.d("PatientRegistration", "✅ Verification - Data updated in Firebase:");
                                            android.util.Log.d("PatientRegistration", "   age: '" + snapshot.child("age").getValue() + "'");
                                            android.util.Log.d("PatientRegistration", "   birth_place: '" + snapshot.child("birth_place").getValue() + "'");
                                            android.util.Log.d("PatientRegistration", "   full_name: '" + snapshot.child("full_name").getValue() + "'");
                                            android.util.Log.d("PatientRegistration", "   allergies: '" + snapshot.child("allergies").getValue() + "'");
                                            android.util.Log.d("PatientRegistration", "   medications: '" + snapshot.child("medications").getValue() + "'");
                                            android.util.Log.d("PatientRegistration", "   medical_history: '" + snapshot.child("medical_history").getValue() + "'");
                                            android.util.Log.d("PatientRegistration", "   emergency_contact_name: '" + snapshot.child("emergency_contact_name").getValue() + "'");
                                            android.util.Log.d("PatientRegistration", "   emergency_contact_phone: '" + snapshot.child("emergency_contact_phone").getValue() + "'");
                                        }
                                    }
                                    
                                    @Override
                                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                                        android.util.Log.e("PatientRegistration", "❌ Error verifying saved data", error.toException());
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("PatientRegistration", "❌ Firebase sync failed (update)", e);
                            });
                    }
                }
                
                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {
                    android.util.Log.e("PatientRegistration", "❌ Error checking patient existence", error.toException());
                    // Fallback: assume new patient and use setValue() to ensure all fields are saved
                    payload.put("created_date", patient.getCreatedDate() != null ? patient.getCreatedDate() : 
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                    payload.put("last_updated", System.currentTimeMillis());
                    
                    android.util.Log.w("PatientRegistration", "⚠️ Using fallback - saving with setValue()");
                    patientRef.setValue(payload)
                        .addOnSuccessListener(unused -> {
                            android.util.Log.d("PatientRegistration", "✅ Patient synced to Firebase (fallback)");
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("PatientRegistration", "❌ Firebase sync failed (fallback)", e);
                        });
                }
            });
        } catch (Exception e) {
            Log.e("PatientRegistration", "Error syncing patient to Firebase", e);
        }
    }
}
