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
    private com.google.android.material.textfield.TextInputEditText inputPainScale;
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

    private com.example.h_cas.database.HCasDatabaseHelper databaseHelper;
    private boolean isExistingPatient = false;
    private String existingPatientId = null;
    private com.example.h_cas.utils.NFCHelper nfcHelper;
    private String scannedNfcUid = null;
    private android.widget.TextView nfcUidDisplay;
    private com.google.android.material.button.MaterialButton buttonScanNFC;

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
        
        // Set up email field with validation
        setupEmailField();
    }

    private void initializeDatabase(@NonNull View view) {
        databaseHelper = new com.example.h_cas.database.HCasDatabaseHelper(view.getContext());
    }
    
    /**
     * Initialize NFC helper and set up NFC scanning
     */
    private void initializeNFC() {
        nfcHelper = new com.example.h_cas.utils.NFCHelper(getContext());
        
        // Set up NFC scan listener
        nfcHelper.setNFCScanListener(new com.example.h_cas.utils.NFCHelper.NFCScanListener() {
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
     * Set up email field with validation (valid email format only)
     */
    private void setupEmailField() {
        if (inputEmail != null) {
            // Set input type to email
            inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            
            // Add TextWatcher for real-time validation
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
                    validateEmail(email);
                }
            });
        }
    }
    
    /**
     * Validate email format
     */
    private void validateEmail(String email) {
        com.google.android.material.textfield.TextInputLayout emailLayout = null;
        if (inputEmail != null && inputEmail.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
            emailLayout = (com.google.android.material.textfield.TextInputLayout) inputEmail.getParent();
        }
        
        if (email.isEmpty()) {
            if (emailLayout != null) {
                emailLayout.setError(null);
            }
            return;
        }
        
        // Check if email format is valid
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (emailLayout != null) {
                emailLayout.setError("Please enter a valid email address");
            }
            return;
        }
        
        // Valid email
        if (emailLayout != null) {
            emailLayout.setError(null);
        }
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
        
        // Validate phone number format
        if (!phoneNumber.startsWith("09")) {
            showToast("Phone number must start with 09");
            if (inputPhoneNumber != null && inputPhoneNumber.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) inputPhoneNumber.getParent())
                    .setError("Phone number must start with 09");
            }
            return;
        }
        
        if (phoneNumber.length() != 11) {
            showToast("Phone number must be exactly 11 digits");
            if (inputPhoneNumber != null && inputPhoneNumber.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) inputPhoneNumber.getParent())
                    .setError("Phone number must be exactly 11 digits");
            }
            return;
        }
        
        if (email.isEmpty()) {
            showToast("Email address is required");
            return;
        }
        
        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email address");
            if (inputEmail != null && inputEmail.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) inputEmail.getParent())
                    .setError("Please enter a valid email address");
            }
            return;
        }

        // Create and populate patient object
        com.example.h_cas.models.Patient patient = new com.example.h_cas.models.Patient();
        
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
            com.example.h_cas.models.Patient existingPatient = databaseHelper.getPatientById(existingPatientId);
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
        if (isExistingPatient && existingPatientId != null) {
            // Update existing patient with new vital signs
            success = databaseHelper.updatePatient(patient);
            if (success) {
                showToast("✅ Patient updated successfully with new vital signs!");
            } else {
                showToast("Failed to update patient");
            }
        } else {
            // Add new patient
            success = databaseHelper.addPatient(patient);
            if (success) {
                showToast("✅ Patient registered successfully!");
            } else {
                showToast("Failed to save patient");
            }
        }
        
        if (success) {
            // Push to Firebase to ensure real-time database stays in sync
            syncPatientToFirebase(patient);
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
     */
    private void syncPatientToFirebase(com.example.h_cas.models.Patient patient) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/");
            DatabaseReference patientRef = database.getReference("patients").child(patient.getPatientId());
            patientRef.keepSynced(false);

            Map<String, Object> payload = new HashMap<>();
            payload.put("patient_id", patient.getPatientId());
            payload.put("first_name", patient.getFirstName());
            payload.put("last_name", patient.getLastName());
            payload.put("full_name", patient.getFullName());
            payload.put("suffix", patient.getSuffix());
            payload.put("date_of_birth", patient.getDateOfBirth());
            payload.put("birth_place", patient.getBirthPlace());
            payload.put("gender", patient.getGender());
            payload.put("age", patient.getAge());
            payload.put("address", patient.getFullAddress());
            payload.put("full_address", patient.getFullAddress());
            payload.put("phone", patient.getPhoneNumber());
            payload.put("phone_number", patient.getPhoneNumber());
            payload.put("email", patient.getEmail());
            payload.put("allergies", patient.getAllergies());
            payload.put("medications", patient.getMedications());
            payload.put("medical_history", patient.getMedicalHistory());
            payload.put("pulse_rate", patient.getPulseRate());
            payload.put("blood_pressure", patient.getBloodPressure());
            payload.put("temperature", patient.getTemperature());
            payload.put("blood_sugar", patient.getBloodSugar());
            payload.put("pain_scale", patient.getPainScale());
            payload.put("symptoms_description", patient.getSymptomsDescription());
            payload.put("emergency_contact_name", patient.getEmergencyContactName());
            payload.put("emergency_contact_phone", patient.getEmergencyContactPhone());
            payload.put("nfc_uid", patient.getNfcUid());
            payload.put("created_date", patient.getCreatedDate() != null ? patient.getCreatedDate() : 
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
            payload.put("last_updated", System.currentTimeMillis());

            patientRef.updateChildren(payload)
                    .addOnSuccessListener(unused -> Log.d("PatientRegistration", "Patient synced to Firebase"))
                    .addOnFailureListener(e -> Log.e("PatientRegistration", "Firebase sync failed", e));
        } catch (Exception e) {
            Log.e("PatientRegistration", "Error syncing patient to Firebase", e);
        }
    }
}
