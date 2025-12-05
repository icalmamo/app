package com.example.h_cas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.example.h_cas.adapters.MedicationPrescriptionAdapter;
import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.database.FirebaseRTDBHelper;
import com.example.h_cas.models.MedicationItem;
import com.example.h_cas.models.Prescription;
import com.example.h_cas.models.Employee;
import com.example.h_cas.models.Patient;
import com.example.h_cas.utils.RFIDHelper;
import com.google.firebase.database.DatabaseReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * CreatePrescriptionFragment allows doctors to create prescriptions for patients.
 */
public class CreatePrescriptionFragment extends Fragment {

    private TextInputEditText patientIdInput;
    private AutoCompleteTextView medicationInput;
    private TextInputEditText instructionsInput;
    private MaterialButton addMedicationButton;
    private MaterialButton createPrescriptionButton;
    private RecyclerView medicationsRecyclerView;
    private MedicationPrescriptionAdapter medicationAdapter;
    private List<MedicationItem> medicationList;
    
    private HCasDatabaseHelper databaseHelper;
    private FirebaseRTDBHelper firebaseRTDBHelper;
    private String currentDoctorId;
    private String currentDoctorName;
    private RFIDHelper rfidHelper;
    private String scannedRFIDUid = null;
    private boolean isWaitingForRFID = false;

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
        
        // Setup medication autocomplete
        setupMedicationAutocomplete();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Stop RFID listening when fragment is paused
        if (rfidHelper != null) {
            rfidHelper.stopListening();
        }
        isWaitingForRFID = false;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop RFID listening when fragment is destroyed
        if (rfidHelper != null) {
            rfidHelper.stopListening();
        }
    }

    private void initializeViews(View view) {
        patientIdInput = view.findViewById(R.id.patientIdInput);
        medicationInput = view.findViewById(R.id.medicationInput);
        instructionsInput = view.findViewById(R.id.instructionsInput);
        addMedicationButton = view.findViewById(R.id.addMedicationButton);
        createPrescriptionButton = view.findViewById(R.id.createPrescriptionButton);
        medicationsRecyclerView = view.findViewById(R.id.medicationsRecyclerView);
        
        // Initialize medication list and adapter
        medicationList = new ArrayList<>();
        medicationAdapter = new MedicationPrescriptionAdapter(medicationList);
        medicationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        medicationsRecyclerView.setAdapter(medicationAdapter);
        
        // Setup remove listener
        medicationAdapter.setOnRemoveClickListener(position -> {
            medicationAdapter.removeMedication(position);
        });
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
        firebaseRTDBHelper = new FirebaseRTDBHelper(getContext());
        rfidHelper = new RFIDHelper();
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
        addMedicationButton.setOnClickListener(v -> addMedication());
        createPrescriptionButton.setOnClickListener(v -> writePrescriptionToCard());
    }
    
    /**
     * Add medication to the list
     */
    private void addMedication() {
        String medicationName = getText(medicationInput);
        
        if (medicationName.isEmpty()) {
            showToast("Please enter a medication name");
            return;
        }
        
        // Check if medication already exists
        for (MedicationItem item : medicationList) {
            if (item.getMedicationName().equalsIgnoreCase(medicationName)) {
                showToast("This medication is already in the list");
                return;
            }
        }
        
        // Add new medication with empty frequency and duration
        MedicationItem newMedication = new MedicationItem(medicationName, "", "");
        medicationAdapter.addMedication(newMedication);
        
        // Clear medication input
        medicationInput.setText("");
    }
    
    /**
     * Setup autocomplete for medication name with common Philippine medicines
     */
    private void setupMedicationAutocomplete() {
        // List of common medicines available in the Philippines
        List<String> philippineMedicines = new ArrayList<>(Arrays.asList(
            "Bioflu", "Biogesic", "Neozep", "Decolgen", "Solmux",
            "Alaxan", "Medicol", "Tempra", "Calpol", "Paracetamol",
            "Ibuprofen", "Mefenamic Acid", "Diclofenac", "Naproxen",
            "Amoxicillin", "Azithromycin", "Cefalexin", "Ciprofloxacin",
            "Clarithromycin", "Doxycycline", "Erythromycin", "Penicillin",
            "Loratadine", "Cetirizine", "Fexofenadine", "Chlorphenamine",
            "Salbutamol", "Montelukast", "Budesonide", "Fluticasone",
            "Omeprazole", "Lansoprazole", "Pantoprazole", "Ranitidine",
            "Metformin", "Glibenclamide", "Insulin", "Glimepiride",
            "Losartan", "Amlodipine", "Captopril", "Enalapril",
            "Atorvastatin", "Simvastatin", "Rosuvastatin", "Lovastatin",
            "Warfarin", "Aspirin", "Clopidogrel", "Ticlopidine",
            "Levothyroxine", "Methimazole", "Propylthiouracil",
            "Prednisone", "Dexamethasone", "Hydrocortisone",
            "Furosemide", "Hydrochlorothiazide", "Spironolactone",
            "Metoclopramide", "Domperidone", "Loperamide",
            "Diphenhydramine", "Promethazine", "Dimenhydrinate",
            "Multivitamins", "Vitamin C", "Vitamin D", "Calcium",
            "Iron Supplements", "Folic Acid", "B Complex",
            "Mefenamic", "Tramadol", "Tramal", "Dolcet",
            "Robitussin", "Tuseran", "Ascof", "Vicks Vaporub",
            "Betadine", "Hipoglos", "Caladryl", "Calamine",
            "Diatabs", "Imodium", "Buscopan", "Dulcolax",
            "Senokot", "Lactulose", "Gaviscon", "Maalox",
            "Tums", "Rolaids", "Pepto Bismol", "Kremil-S"
        ));
        
        // Create adapter for autocomplete
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            getContext(),
            android.R.layout.simple_dropdown_item_1line,
            philippineMedicines
        );
        
        medicationInput.setAdapter(adapter);
        medicationInput.setThreshold(1); // Show suggestions after 1 character
    }

    /**
     * Write prescription to RFID card - First scan RFID tag, then save prescription
     */
    private void writePrescriptionToCard() {
        String patientId = getText(patientIdInput);
        String instructions = getText(instructionsInput);
        List<MedicationItem> medications = medicationAdapter.getMedications();

        if (!validateInputs(patientId, medications)) {
            return;
        }
        
        // If already waiting for RFID, don't start another scan
        if (isWaitingForRFID) {
            android.util.Log.w("CreatePrescription", "⚠️ Already waiting for RFID scan");
            showToast("Please wait for RFID scan to complete");
            return;
        }
        
        android.util.Log.d("CreatePrescription", "📝 writePrescriptionToCard called");
        android.util.Log.d("CreatePrescription", "  Patient ID: " + patientId);
        android.util.Log.d("CreatePrescription", "  Medications count: " + medications.size());
        
        // Show scanning dialog
        showRFIDScanningDialog();
        
        // Reset previous scan
        scannedRFIDUid = null;
        isWaitingForRFID = true;
        
        android.util.Log.d("CreatePrescription", "✅ isWaitingForRFID set to true");
        
        // Start listening for RFID scans
        if (rfidHelper != null) {
            android.util.Log.d("CreatePrescription", "🔍 Starting RFID listener...");
            rfidHelper.resetScanTracking();
            
            // Test read current Firebase value for debugging
            rfidHelper.testReadCurrentValue();
            
            rfidHelper.startListening(new RFIDHelper.RFIDScanListener() {
                @Override
                public void onRFIDTagScanned(String rfidUid) {
                    android.util.Log.d("CreatePrescription", "📞 onRFIDTagScanned callback received!");
                    android.util.Log.d("CreatePrescription", "  rfidUid: " + rfidUid);
                    android.util.Log.d("CreatePrescription", "  isWaitingForRFID: " + isWaitingForRFID);
                    
                    if (rfidUid == null || rfidUid.isEmpty()) {
                        android.util.Log.e("CreatePrescription", "❌ RFID UID is null or empty!");
                        return;
                    }
                    
                    if (!isWaitingForRFID) {
                        android.util.Log.w("CreatePrescription", "⚠️ Received scan but isWaitingForRFID is false - ignoring");
                        return;
                    }
                    
                    scannedRFIDUid = rfidUid;
                    isWaitingForRFID = false;
                    
                    // Stop listening
                    rfidHelper.stopListening();
                    
                    android.util.Log.d("CreatePrescription", "✅✅✅ RFID tag scanned successfully: " + rfidUid);
                    
                    // Update dialog message and dismiss
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            android.util.Log.d("CreatePrescription", "🖥️ Updating UI on main thread...");
                            if (scanningDialog != null && scanningDialog.isShowing()) {
                                scanningDialog.setMessage("✅ Tag detected: " + rfidUid + "\n\n💾 Saving prescription...");
                                android.util.Log.d("CreatePrescription", "✅ Dialog message updated");
                                // Keep dialog open while saving
                            } else {
                                android.util.Log.w("CreatePrescription", "⚠️ Scanning dialog is null or not showing");
                            }
                            
                            // Now save prescription to the RFID tag
                            android.util.Log.d("CreatePrescription", "💾 Calling savePrescriptionToRFIDTag...");
                            savePrescriptionToRFIDTag(rfidUid, patientId, instructions, medications);
                        });
                    } else {
                        android.util.Log.e("CreatePrescription", "❌ Activity is null!");
                    }
                }
                
                @Override
                public void onRFIDScanError(String error) {
                    android.util.Log.e("CreatePrescription", "❌ RFID scan error: " + error);
                    isWaitingForRFID = false;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (scanningDialog != null && scanningDialog.isShowing()) {
                                scanningDialog.dismiss();
                            }
                            showToast("❌ RFID scan error: " + error);
                        });
                    }
                }
            });
        }
    }
    
    private android.app.AlertDialog scanningDialog;
    
    private android.os.Handler scanningTimeoutHandler;
    private static final long SCANNING_TIMEOUT_MS = 60000; // 60 seconds timeout
    
    /**
     * Show RFID scanning dialog
     */
    private void showRFIDScanningDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Scan RFID Tag");
        builder.setMessage("Please scan a fresh RFID tag on the ESP32 reader.\n\n⏳ Waiting for tag scan...\n\n" +
                "Make sure:\n" +
                "1. ESP32 is connected to WiFi\n" +
                "2. ESP32 is connected to Firebase\n" +
                "3. RFID reader is working\n" +
                "4. Tag is placed on the reader\n\n" +
                "💡 Tip: Tap the RFID tag on the ESP32 reader now!");
        builder.setCancelable(true);
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            isWaitingForRFID = false;
            if (rfidHelper != null) {
                rfidHelper.stopListening();
            }
            if (scanningTimeoutHandler != null) {
                scanningTimeoutHandler.removeCallbacksAndMessages(null);
            }
            android.util.Log.d("CreatePrescription", "❌ User cancelled RFID scan");
            dialog.dismiss();
        });
        
        scanningDialog = builder.create();
        scanningDialog.setCanceledOnTouchOutside(false); // Prevent accidental dismissal
        scanningDialog.show();
        
        // Update message periodically to show we're still waiting
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        android.util.Log.d("CreatePrescription", "⏰ Starting periodic dialog updates...");
        
        // Update after 5 seconds
        handler.postDelayed(() -> {
            if (scanningDialog != null && scanningDialog.isShowing() && isWaitingForRFID) {
                scanningDialog.setMessage("Still waiting for RFID scan...\n\n" +
                        "Check:\n" +
                        "• ESP32 Serial Monitor for scan logs\n" +
                        "• Firebase Console at path: HCAS/rfid_scans/latest\n" +
                        "• Make sure tag is on the reader\n\n" +
                        "💡 Tap the RFID tag on the ESP32 reader now!");
                android.util.Log.d("CreatePrescription", "⏰ Dialog updated after 5 seconds");
            }
        }, 5000);
        
        // Update after 15 seconds with more detailed info
        handler.postDelayed(() -> {
            if (scanningDialog != null && scanningDialog.isShowing() && isWaitingForRFID) {
                scanningDialog.setMessage("Still waiting... (15s elapsed)\n\n" +
                        "Troubleshooting:\n" +
                        "1. Check ESP32 Serial Monitor - is it scanning?\n" +
                        "2. Check Firebase Console - is data being written?\n" +
                        "3. Try tapping the tag again\n" +
                        "4. Make sure ESP32 WiFi is connected\n\n" +
                        "💡 The app is actively listening for scans...");
                android.util.Log.d("CreatePrescription", "⏰ Dialog updated after 15 seconds");
            }
        }, 15000);
        
        // Timeout after 60 seconds
        handler.postDelayed(() -> {
            if (scanningDialog != null && scanningDialog.isShowing() && isWaitingForRFID) {
                android.util.Log.w("CreatePrescription", "⏰ Scanning timeout after 60 seconds");
                isWaitingForRFID = false;
                if (rfidHelper != null) {
                    rfidHelper.stopListening();
                }
                scanningDialog.dismiss();
                showToast("⏰ Scanning timeout. Please try again.\n\nMake sure ESP32 is connected and RFID reader is working.");
            }
        }, 60000);
    }
    
    /**
     * Save prescription data to RFID tag path in Firebase
     */
    private void savePrescriptionToRFIDTag(String rfidUid, String patientId, String instructions, List<MedicationItem> medications) {
        android.util.Log.d("CreatePrescription", "💾 Starting to save prescription for RFID: " + rfidUid);
        
        // Get patient name from Firebase RTDB (async)
        getPatientName(patientId, patientName -> {
            // Create prescription for each medication
            boolean allSuccess = true;
            long baseTimestamp = System.currentTimeMillis();
            int index = 0;
            List<Prescription> prescriptions = new ArrayList<>();
            
            for (MedicationItem medicationItem : medications) {
                // Validate each medication has frequency and duration
                if (medicationItem.getFrequency().isEmpty() || medicationItem.getDuration().isEmpty()) {
                    showToast("Please fill in frequency and duration for all medications");
                    allSuccess = false;
                    break;
                }
                
                // Create prescription object
                Prescription prescription = new Prescription();
                prescription.setPrescriptionId("PRE" + baseTimestamp + "_" + index);
                prescription.setPatientId(patientId);
                prescription.setPatientName(patientName);
                prescription.setMedication(medicationItem.getMedicationName());
                prescription.setDosage(""); // Empty dosage since field is removed
                prescription.setFrequency(medicationItem.getFrequency());
                // Ensure duration has "days" suffix
                String duration = medicationItem.getDuration();
                if (duration != null && !duration.trim().isEmpty() && !duration.toLowerCase().contains("day")) {
                    duration = duration.trim() + " days";
                }
                prescription.setDuration(duration);
                prescription.setInstructions(instructions);
                prescription.setDoctorId(currentDoctorId);
                prescription.setDoctorName(currentDoctorName);
                prescription.setCreatedDate(getCurrentDateTime());
                prescription.setStatus("Active");
                
                prescriptions.add(prescription);
                
                // Save prescription to SQLite first
                boolean sqliteSuccess = databaseHelper.addPrescription(prescription);
                
                // Also save to Firebase RTDB directly for immediate availability
                if (sqliteSuccess && firebaseRTDBHelper != null) {
                    firebaseRTDBHelper.addPrescription(prescription, success -> {
                        if (!success) {
                            android.util.Log.e("CreatePrescription", "Failed to save prescription to Firebase: " + prescription.getPrescriptionId());
                        }
                    });
                    
                    // Save to history folder for doctors
                    firebaseRTDBHelper.addPrescriptionToHistory(prescription, historySuccess -> {
                        if (!historySuccess) {
                            android.util.Log.e("CreatePrescription", "Failed to save prescription to history: " + prescription.getPrescriptionId());
                        } else {
                            android.util.Log.d("CreatePrescription", "✅ Prescription saved to history: " + prescription.getPrescriptionId());
                        }
                    });
                }
                
                if (!sqliteSuccess) {
                    allSuccess = false;
                }
                index++;
            }
            
            if (allSuccess && !prescriptions.isEmpty()) {
                // Save prescription data to RFID tag path in Firebase
                // Wait for Firebase write to complete before showing success
                savePrescriptionsToRFIDPath(rfidUid, prescriptions, patientId, patientName, new RFIDSaveCallback() {
                    @Override
                    public void onSuccess() {
                        android.util.Log.d("CreatePrescription", "✅ Prescription successfully saved to RFID tag: " + rfidUid);
                        
                        // Remove patient from Firebase (so they won't appear in registered patients list)
                        // Wait for removal to complete before navigating
                        removePatientFromFirebase(patientId, () -> {
                            // Callback after patient is removed
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    // Dismiss scanning dialog if still showing
                                    if (scanningDialog != null && scanningDialog.isShowing()) {
                                        scanningDialog.dismiss();
                                    }
                                    
                                    showToast("✅ Prescription written to RFID tag (" + rfidUid + ") successfully!");
                                    clearForm();
                                    
                                    // Navigate back to Registered Patients fragment
                                    // The list will automatically refresh and show updated data
                                    navigateToRegisteredPatients();
                                });
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        android.util.Log.e("CreatePrescription", "❌ Failed to save prescription to RFID tag: " + error);
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                // Dismiss scanning dialog if still showing
                                if (scanningDialog != null && scanningDialog.isShowing()) {
                                    scanningDialog.dismiss();
                                }
                                
                                showToast("❌ Failed to save prescription to RFID tag: " + error);
                            });
                        }
                    }
                });
            } else {
                showToast("❌ Failed to create some prescriptions. Please try again.");
            }
        });
    }
    
    /**
     * Interface for RFID save callback
     */
    private interface RFIDSaveCallback {
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * Save prescription data to Firebase under RFID tag path
     */
    private void savePrescriptionsToRFIDPath(String rfidUid, List<Prescription> prescriptions, String patientId, String patientName, RFIDSaveCallback callback) {
        if (rfidUid == null || rfidUid.isEmpty() || prescriptions == null || prescriptions.isEmpty()) {
            android.util.Log.e("CreatePrescription", "❌ Invalid RFID UID or prescriptions");
            if (callback != null) {
                callback.onError("Invalid RFID UID or prescriptions");
            }
            return;
        }
        
        try {
            DatabaseReference rfidTagRef = rfidHelper.getRFIDTagRef(rfidUid);
            if (rfidTagRef == null) {
                android.util.Log.e("CreatePrescription", "❌ Failed to get RFID tag reference");
                if (callback != null) {
                    callback.onError("Failed to get RFID tag reference");
                }
                return;
            }
            
            // Create prescription data map
            java.util.Map<String, Object> prescriptionData = new java.util.HashMap<>();
            prescriptionData.put("patient_id", patientId);
            prescriptionData.put("patient_name", patientName);
            prescriptionData.put("doctor_id", currentDoctorId);
            prescriptionData.put("doctor_name", currentDoctorName);
            prescriptionData.put("created_date", getCurrentDateTime());
            prescriptionData.put("status", "Active");
            prescriptionData.put("rfid_uid", rfidUid);
            
            // Store medications as a list
            java.util.List<java.util.Map<String, Object>> medicationsList = new java.util.ArrayList<>();
            for (Prescription prescription : prescriptions) {
                java.util.Map<String, Object> medData = new java.util.HashMap<>();
                medData.put("medication", prescription.getMedication());
                medData.put("frequency", prescription.getFrequency());
                medData.put("duration", prescription.getDuration());
                medData.put("instructions", prescription.getInstructions());
                medData.put("prescription_id", prescription.getPrescriptionId());
                medicationsList.add(medData);
            }
            prescriptionData.put("medications", medicationsList);
            
            android.util.Log.d("CreatePrescription", "💾 Saving prescription to RFID tag: " + rfidUid);
            android.util.Log.d("CreatePrescription", "📋 Firebase path: HCAS/rfid_tags/" + rfidUid + "/prescription");
            android.util.Log.d("CreatePrescription", "📋 Prescription data: " + prescriptionData.toString());
            android.util.Log.d("CreatePrescription", "📋 Medications count: " + medicationsList.size());
            
            // Save to Firebase and wait for completion
            DatabaseReference prescriptionRef = rfidTagRef.child("prescription");
            android.util.Log.d("CreatePrescription", "📋 Full Firebase path: " + prescriptionRef.toString());
            
            prescriptionRef.setValue(prescriptionData)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("CreatePrescription", "✅✅✅ Prescription saved to Firebase successfully!");
                    android.util.Log.d("CreatePrescription", "✅ Path: HCAS/rfid_tags/" + rfidUid + "/prescription");
                    android.util.Log.d("CreatePrescription", "✅ Patient: " + patientName + " (" + patientId + ")");
                    android.util.Log.d("CreatePrescription", "✅ Medications: " + medicationsList.size());
                    
                    // Verify the data was saved by reading it back
                    prescriptionRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                android.util.Log.d("CreatePrescription", "✅✅✅ VERIFICATION: Data confirmed in Firebase!");
                                android.util.Log.d("CreatePrescription", "✅ Verified data: " + snapshot.getValue().toString());
                            } else {
                                android.util.Log.e("CreatePrescription", "❌❌❌ VERIFICATION FAILED: Data not found in Firebase!");
                            }
                        }
                        
                        @Override
                        public void onCancelled(com.google.firebase.database.DatabaseError error) {
                            android.util.Log.e("CreatePrescription", "❌ Verification read cancelled: " + error.getMessage());
                        }
                    });
                    
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CreatePrescription", "❌❌❌ Failed to save prescription to Firebase!");
                    android.util.Log.e("CreatePrescription", "❌ Error: " + e.getMessage());
                    android.util.Log.e("CreatePrescription", "❌ Path: HCAS/rfid_tags/" + rfidUid + "/prescription");
                    e.printStackTrace();
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
                
        } catch (Exception e) {
            android.util.Log.e("CreatePrescription", "❌ Error saving prescription to RFID tag: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        }
    }

    private boolean validateInputs(String patientId, List<MedicationItem> medications) {
        if (patientId.isEmpty()) {
            showToast("Please enter Patient ID");
            return false;
        }
        
        if (medications == null || medications.isEmpty()) {
            showToast("Please add at least one medication");
            return false;
        }
        
        // Validate each medication has frequency and duration
        for (MedicationItem item : medications) {
            if (item.getFrequency() == null || item.getFrequency().trim().isEmpty()) {
                showToast("Please enter frequency for " + item.getMedicationName());
                return false;
            }
            
            if (item.getDuration() == null || item.getDuration().trim().isEmpty()) {
                showToast("Please enter duration for " + item.getMedicationName());
                return false;
            }
        }
        
        return true;
    }

    private void clearForm() {
        patientIdInput.setText("");
        medicationInput.setText("");
        instructionsInput.setText("");
        medicationList.clear();
        medicationAdapter.notifyDataSetChanged();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
    
    private String getText(AutoCompleteTextView autoCompleteTextView) {
        return autoCompleteTextView.getText() != null ? autoCompleteTextView.getText().toString().trim() : "";
    }
    
    /**
     * Set patient_status to "off" in Firebase after prescription is created
     * This ensures the patient won't appear in the registered patients list (soft delete)
     * @param patientId The patient ID to update
     * @param onComplete Callback when update is complete (or failed)
     */
    private void removePatientFromFirebase(String patientId, Runnable onComplete) {
        if (firebaseRTDBHelper == null || patientId == null || patientId.isEmpty()) {
            android.util.Log.w("CreatePrescription", "⚠️ Cannot update patient status: firebaseRTDBHelper or patientId is null/empty");
            if (onComplete != null) {
                onComplete.run(); // Still call callback even if update fails
            }
            return;
        }
        
        try {
            com.google.firebase.database.DatabaseReference patientRef = 
                firebaseRTDBHelper.getRootRef().child("patients").child(patientId);
            
            android.util.Log.d("CreatePrescription", "🔄 Setting patient_status to 'off' in Firebase: " + patientId);
            android.util.Log.d("CreatePrescription", "🔄 Firebase path: patients/" + patientId);
            
            // Update only patient_status field (soft delete)
            java.util.Map<String, Object> updateData = new java.util.HashMap<>();
            updateData.put("patient_status", "off");
            
            patientRef.updateChildren(updateData)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("CreatePrescription", "✅✅✅ Patient status successfully set to 'off': " + patientId);
                    android.util.Log.d("CreatePrescription", "✅ Patient will no longer appear in registered patients list");
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CreatePrescription", "❌ Failed to update patient status in Firebase: " + e.getMessage(), e);
                    // Still call callback even if update fails, so navigation can proceed
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
        } catch (Exception e) {
            android.util.Log.e("CreatePrescription", "❌ Error updating patient status in Firebase: " + e.getMessage(), e);
            e.printStackTrace();
            // Still call callback even if update fails
            if (onComplete != null) {
                onComplete.run();
            }
        }
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
    
    /**
     * Navigate back to Registered Patients fragment
     */
    private void navigateToRegisteredPatients() {
        if (getActivity() instanceof DoctorDashboardActivity) {
            DoctorDashboardActivity activity = (DoctorDashboardActivity) getActivity();
            activity.loadFragment(new RegisteredPatientsFragment());
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle("Registered Patients");
            }
        }
    }
}
