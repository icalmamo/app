package com.example.h_cas;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.RFIDData;
import com.example.h_cas.models.Medicine;
import com.example.h_cas.models.Patient;
import com.example.h_cas.models.Prescription;
import com.example.h_cas.utils.RFIDHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MedicationDispensingFragment handles NFC reading and medication dispensing for pharmacists
 */
public class MedicationDispensingFragment extends Fragment {

    private HCasDatabaseHelper databaseHelper;
    private RFIDHelper rfidHelper;
    private MaterialButton scanRFIDButton;
    private MaterialCardView prescriptionCard;
    private boolean isScanning = false;
    private android.app.AlertDialog scanningDialog;
    private View scanDialogView; // Store reference to scanning dialog view
    private String currentScannedRFIDUid; // Store current scanned RFID UID for deletion after dispense

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_medication_dispensing, container, false);
        
        initializeViews(view);
        initializeDatabase();
        initializeRFID();
        setupClickListeners();
        
        return view;
    }

    private void initializeViews(View view) {
        scanRFIDButton = view.findViewById(R.id.scanRFIDButton);
        prescriptionCard = view.findViewById(R.id.prescriptionCard);
        
        // Update button text to reflect RFID scanning
        if (scanRFIDButton != null) {
            scanRFIDButton.setText("Scan RFID Tag");
        }
        
        // Initially hide prescription card
        prescriptionCard.setVisibility(View.GONE);
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
    }
    
    /**
     * Initialize RFID helper for ESP32 scanning
     */
    private void initializeRFID() {
        rfidHelper = new RFIDHelper();
    }

    private void setupClickListeners() {
        scanRFIDButton.setOnClickListener(v -> scanRFIDTag());
    }

    /**
     * Scan RFID tag from ESP32 reader
     */
    private void scanRFIDTag() {
        if (isScanning) {
            Toast.makeText(getContext(), "Already scanning. Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show RFID scanning dialog with custom layout
        AlertDialog.Builder scanBuilder = new AlertDialog.Builder(getContext());
        scanDialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_rfid_scanning, null);
        scanBuilder.setView(scanDialogView);
        
        scanningDialog = scanBuilder.create();
        scanningDialog.setCancelable(true);
        
        // Set up close button
        ImageButton closeScanButton = scanDialogView.findViewById(R.id.closeScanButton);
        MaterialButton cancelScanButton = scanDialogView.findViewById(R.id.cancelScanButton);
        
        View.OnClickListener cancelListener = v -> {
            isScanning = false;
            if (rfidHelper != null) {
                rfidHelper.stopListening();
            }
            if (scanningDialog != null && scanningDialog.isShowing()) {
                scanningDialog.dismiss();
            }
        };
        
        if (closeScanButton != null) {
            closeScanButton.setOnClickListener(cancelListener);
        }
        if (cancelScanButton != null) {
            cancelScanButton.setOnClickListener(cancelListener);
        }
        
        scanningDialog.show();
        
        // Start scanning
        isScanning = true;
        
        if (rfidHelper == null) {
            initializeRFID();
        }
        
        rfidHelper.resetScanTracking();
        rfidHelper.startListening(new RFIDHelper.RFIDScanListener() {
            @Override
            public void onRFIDTagScanned(String rfidUid) {
                if (rfidUid != null && !rfidUid.isEmpty() && isScanning) {
                    isScanning = false;
                    rfidHelper.stopListening();
                    
                    // Dismiss scanning dialog
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Update status text before dismissing
                            if (scanDialogView != null) {
                                TextView statusText = scanDialogView.findViewById(R.id.scanStatusText);
                                if (statusText != null) {
                                    statusText.setText("✅ Tag detected: " + rfidUid + "\n\nReading prescription...");
                                }
                            }
                            
                            if (scanningDialog != null && scanningDialog.isShowing()) {
                                scanningDialog.dismiss();
                            }
                            
                            // Read prescription from RFID tag
                            readPrescriptionFromRFIDTag(rfidUid);
                        });
                    }
                }
            }
            
            @Override
            public void onRFIDScanError(String error) {
                android.util.Log.e("MedicationDispensing", "❌ RFID scan error: " + error);
                isScanning = false;
        if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (scanningDialog != null && scanningDialog.isShowing()) {
                            scanningDialog.dismiss();
                        }
                        Toast.makeText(getContext(), "RFID scan failed. Please try again.", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    /**
     * Read prescription data from RFID tag path in Firebase
     */
    private void readPrescriptionFromRFIDTag(String rfidUid) {
        if (rfidUid == null || rfidUid.isEmpty()) {
            Toast.makeText(getContext(), "Invalid RFID tag", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Store RFID UID for deletion after dispense
        currentScannedRFIDUid = rfidUid;
        
        android.util.Log.d("MedicationDispensing", "📖 Reading prescription from RFID tag: " + rfidUid);
        android.util.Log.d("MedicationDispensing", "📖 Firebase path: HCAS/rfid_tags/" + rfidUid + "/prescription");
        
        DatabaseReference rfidTagRef = rfidHelper.getRFIDTagRef(rfidUid);
        if (rfidTagRef == null) {
            android.util.Log.e("MedicationDispensing", "❌ Failed to get RFID tag reference for: " + rfidUid);
            Toast.makeText(getContext(), "❌ Failed to read RFID tag data", Toast.LENGTH_LONG).show();
            return;
        }
        
        // First check if the RFID tag node exists at all
        rfidTagRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot tagSnapshot) {
                if (tagSnapshot == null || !tagSnapshot.exists()) {
                    android.util.Log.w("MedicationDispensing", "⚠️ RFID tag node does not exist: HCAS/rfid_tags/" + rfidUid);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), 
                                "❌ This RFID tag has no prescription data.\n\n" +
                                "The doctor needs to write a prescription to this tag first.", 
                                Toast.LENGTH_LONG).show();
                        });
                    }
                    return;
                }
                
                        android.util.Log.d("MedicationDispensing", "✅ RFID tag node exists, checking for prescription...");
                        android.util.Log.d("MedicationDispensing", "  Tag node data: " + tagSnapshot.getValue());
                        
                        // List all children to see what's in the tag node
                        android.util.Log.d("MedicationDispensing", "  All children in tag node:");
                        for (DataSnapshot child : tagSnapshot.getChildren()) {
                            android.util.Log.d("MedicationDispensing", "    - " + child.getKey() + ": " + child.getValue());
                        }
                        
                        // Now check for prescription data
                        rfidTagRef.child("prescription").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                try {
                                    android.util.Log.d("MedicationDispensing", "📖 Reading prescription data from Firebase...");
                                    android.util.Log.d("MedicationDispensing", "  Snapshot exists: " + (snapshot != null && snapshot.exists()));
                                    
                                    if (snapshot == null || !snapshot.exists()) {
                                    android.util.Log.w("MedicationDispensing", "⚠️ No prescription data found at: HCAS/rfid_tags/" + rfidUid + "/prescription");
                                    android.util.Log.w("MedicationDispensing", "  Available children in tag node:");
                                    for (DataSnapshot child : tagSnapshot.getChildren()) {
                                        android.util.Log.w("MedicationDispensing", "    - " + child.getKey());
                                    }
                                    
                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), 
                                                "❌ No prescription found on this RFID tag.\n\n" +
                                                "Tag UID: " + rfidUid + "\n" +
                                                "The doctor needs to write a prescription to this tag first.\n\n" +
                                                "Please make sure:\n" +
                                                "1. Doctor has written prescription to this tag\n" +
                                                "2. You are scanning the correct tag", 
                                                Toast.LENGTH_LONG).show();
                                        });
                    }
                    return;
                }
                                
                                    android.util.Log.d("MedicationDispensing", "✅✅✅ Prescription data found!");
                                    android.util.Log.d("MedicationDispensing", "  Raw data: " + snapshot.getValue());
                    
                                    Map<String, Object> prescriptionData = (Map<String, Object>) snapshot.getValue();
                                    if (prescriptionData == null) {
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                Toast.makeText(getContext(), "❌ Invalid prescription data on RFID tag", Toast.LENGTH_LONG).show();
                                            });
                                        }
                                        return;
                                    }
                                    
                                    // Extract prescription information
                                    String patientId = String.valueOf(prescriptionData.get("patient_id"));
                                    String patientName = String.valueOf(prescriptionData.get("patient_name"));
                                    String doctorId = String.valueOf(prescriptionData.get("doctor_id"));
                                    String doctorName = String.valueOf(prescriptionData.get("doctor_name"));
                                    String createdDate = String.valueOf(prescriptionData.get("created_date"));
                                    String prescriptionStatus = String.valueOf(prescriptionData.get("status"));
                                    
                                    // Check if prescription is already dispensed
                                    if ("Dispensed".equals(prescriptionStatus)) {
                                        android.util.Log.w("MedicationDispensing", "⚠️ Prescription on RFID tag is already dispensed: " + rfidUid);
            if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                Toast.makeText(getContext(), 
                                                    "⚠️ This prescription has already been dispensed.\n\n" +
                                                    "The RFID tag needs a new prescription from the doctor.\n" +
                                                    "Please ask the doctor to write a new prescription to this tag.", 
                                                    Toast.LENGTH_LONG).show();
                                            });
            }
            return;
        }
        
                                    // Get medications list
                                    Object medicationsObj = prescriptionData.get("medications");
                                    if (medicationsObj == null) {
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                Toast.makeText(getContext(), "❌ No medications found in prescription", Toast.LENGTH_LONG).show();
                                            });
                                        }
                                        return;
                                    }
                                    
                                    List<Map<String, Object>> medicationsList = (List<Map<String, Object>>) medicationsObj;
                                    if (medicationsList == null || medicationsList.isEmpty()) {
            if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                Toast.makeText(getContext(), "❌ No medications found in prescription", Toast.LENGTH_LONG).show();
                                            });
            }
            return;
        }
        
                                    // Create Patient object
                                    Patient patient = new Patient();
                                    patient.setPatientId(patientId != null ? patientId : "");
                                    if (patientName != null && !patientName.isEmpty() && !patientName.equals("null")) {
                                        // Set fullName directly for proper display
                                        patient.setFullName(patientName);
                                        String[] nameParts = patientName.split(" ", 2);
                                        patient.setFirstName(nameParts[0]);
                                        if (nameParts.length > 1) {
                                            patient.setLastName(nameParts[1]);
        } else {
                                            patient.setLastName("");
                                        }
                                    } else {
                                        patient.setFirstName("Unknown");
                                        patient.setLastName("Patient");
                                        patient.setFullName("Unknown Patient");
                                    }
                    
                    // Create Prescription objects from medications
                    List<Prescription> prescriptions = new ArrayList<>();
                    for (Map<String, Object> medData : medicationsList) {
                        try {
                            Prescription prescription = new Prescription();
                            prescription.setPrescriptionId(getStringValue(medData, "prescription_id"));
                            prescription.setPatientId(patientId != null && !patientId.equals("null") ? patientId : "");
                            prescription.setPatientName(patientName != null && !patientName.equals("null") ? patientName : "Unknown");
                            prescription.setMedication(getStringValue(medData, "medication"));
                            prescription.setDosage(getStringValue(medData, "dosage")); // May not exist, but set it anyway
                            prescription.setFrequency(getStringValue(medData, "frequency"));
                            prescription.setDuration(getStringValue(medData, "duration"));
                            prescription.setInstructions(getStringValue(medData, "instructions"));
                            prescription.setDoctorId(doctorId != null && !doctorId.equals("null") ? doctorId : "");
                            prescription.setDoctorName(doctorName != null && !doctorName.equals("null") ? doctorName : "Unknown Doctor");
                            prescription.setCreatedDate(createdDate != null && !createdDate.equals("null") ? createdDate : "");
                            prescription.setStatus("Active");
                            prescriptions.add(prescription);
                        } catch (Exception e) {
                            android.util.Log.e("MedicationDispensing", "❌ Error creating prescription from medication data: " + e.getMessage(), e);
                            // Continue with next medication
                        }
                    }
                    
                    if (prescriptions.isEmpty()) {
                        android.util.Log.e("MedicationDispensing", "❌ No valid prescriptions created from medications");
        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "❌ Failed to parse prescription data", Toast.LENGTH_LONG).show();
                            });
                        }
                        return;
                    }
                    
                                    // Show all prescriptions for dispensing (no selection needed)
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            showPrescriptionForDispensing(prescriptions, patient);
                                        });
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e("MedicationDispensing", "❌ Error reading prescription from RFID tag: " + e.getMessage(), e);
                                    e.printStackTrace();
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            if (getContext() != null) {
                                                Toast.makeText(getContext(), "Cannot read prescription data", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }
                            }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        android.util.Log.e("MedicationDispensing", "❌ Failed to read prescription from RFID tag: " + error.getMessage());
                        android.util.Log.e("MedicationDispensing", "  Error code: " + error.getCode());
                        android.util.Log.e("MedicationDispensing", "  Error details: " + error.getDetails());
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Cannot read prescription", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                android.util.Log.e("MedicationDispensing", "❌ Failed to read RFID tag node: " + error.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Cannot access RFID tag", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    /**
     * Show prescription data for dispensing (handles single or multiple prescriptions)
     */
    private void showPrescriptionForDispensing(java.util.List<Prescription> prescriptions, Patient patient) {
        if (prescriptions == null || prescriptions.isEmpty()) {
            android.util.Log.e("MedicationDispensing", "❌ No prescriptions to display");
            Toast.makeText(getContext(), "❌ No prescriptions found", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (getContext() == null || getActivity() == null) {
            android.util.Log.e("MedicationDispensing", "❌ Context or Activity is null in showPrescriptionForDispensing");
            return;
        }
        
        if (patient == null) {
            android.util.Log.e("MedicationDispensing", "❌ Patient is null");
            Toast.makeText(getContext(), "❌ Patient data is missing", Toast.LENGTH_LONG).show();
            return;
        }
        
        try {
            // Use the first prescription for patient/doctor info (they should be the same for all)
            Prescription firstPrescription = prescriptions.get(0);
            
            // Show prescription details using Dialog instead of AlertDialog for better scroll support
            android.app.Dialog dialog = new android.app.Dialog(getContext());
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            dialog.setCancelable(true);
        
        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_rfid_prescription, null);
            dialog.setContentView(dialogView);
            
            // Set dialog window properties to allow proper scrolling with rounded corners
            if (dialog.getWindow() != null) {
                android.view.WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
                
                // Set width with margins for floating effect
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int marginDp = 16;
                int marginPx = (int) (marginDp * getResources().getDisplayMetrics().density);
                layoutParams.width = screenWidth - (marginPx * 2);
                
                // Use WRAP_CONTENT to fit all content, but set max height for scrolling when needed
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                int medicationCount = prescriptions != null ? prescriptions.size() : 1;
                
                // For 1-2 medications: wrap content to fit everything
                // For 3+ medications: use max height (80%) with scrolling
                if (medicationCount <= 2) {
                    layoutParams.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
                } else {
                    // For 3+ medications, use max height with scrolling
                    layoutParams.height = (int) (screenHeight * 0.80);
                }
                
                layoutParams.gravity = android.view.Gravity.CENTER;
                dialog.getWindow().setAttributes(layoutParams);
                
                // Set rounded background
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
                
                // Add elevation for floating effect
                dialog.getWindow().setElevation(16);
                
                // Clear any default background
                dialog.getWindow().setDimAmount(0.5f); // Semi-transparent background overlay
                
                // Ensure dialog can receive touch events for scrolling
                dialog.getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                );
            }
            
            // Set patient and doctor information
        TextView dialogPatientName = dialogView.findViewById(R.id.dialogPatientName);
        TextView dialogDoctor = dialogView.findViewById(R.id.dialogDoctor);
            LinearLayout medicationsContainer = dialogView.findViewById(R.id.medicationsContainer);
            
            // Get patient name
            String patientName = firstPrescription.getPatientName() != null && !firstPrescription.getPatientName().isEmpty() && !firstPrescription.getPatientName().equals("Unknown") 
                ? firstPrescription.getPatientName() 
                : (patient.getFullName() != null && !patient.getFullName().isEmpty()
                    ? patient.getFullName()
                    : (patient.getFirstName() != null && patient.getLastName() != null 
                        ? patient.getFirstName() + " " + patient.getLastName() 
                        : "Unknown Patient"));
            
            String doctorName = firstPrescription.getDoctorName() != null ? firstPrescription.getDoctorName() : "Unknown Doctor";
            
            if (dialogPatientName != null) {
                dialogPatientName.setText(patientName);
            }
            if (dialogDoctor != null) {
                dialogDoctor.setText(doctorName);
            }
            
            // Populate medications container with all prescriptions
            if (medicationsContainer != null) {
                medicationsContainer.removeAllViews();
                
                for (int i = 0; i < prescriptions.size(); i++) {
                    Prescription prescription = prescriptions.get(i);
                    
                    // Create a card for each medication
                    com.google.android.material.card.MaterialCardView medicationCard = new com.google.android.material.card.MaterialCardView(getContext());
                    com.google.android.material.card.MaterialCardView.LayoutParams cardParams = new com.google.android.material.card.MaterialCardView.LayoutParams(
                        com.google.android.material.card.MaterialCardView.LayoutParams.MATCH_PARENT,
                        com.google.android.material.card.MaterialCardView.LayoutParams.WRAP_CONTENT
                    );
                    // Responsive margins based on medication count
                    int marginDp = prescriptions.size() == 1 ? 0 : (prescriptions.size() == 2 ? 6 : 8);
                    int marginPx = (int) (marginDp * getResources().getDisplayMetrics().density);
                    cardParams.setMargins(0, 0, 0, marginPx);
                    medicationCard.setLayoutParams(cardParams);
                    // Convert dp to pixels for corner radius
                    float cornerRadius = 12 * getResources().getDisplayMetrics().density;
                    medicationCard.setRadius(cornerRadius);
                    medicationCard.setCardElevation(2);
                    medicationCard.setStrokeWidth(1);
                    medicationCard.setStrokeColor(getResources().getColor(R.color.card_border_gray));
                    medicationCard.setCardBackgroundColor(getResources().getColor(android.R.color.white));
                    
                    // Create inner layout with responsive padding
                    LinearLayout cardLayout = new LinearLayout(getContext());
                    cardLayout.setOrientation(LinearLayout.VERTICAL);
                    // Responsive padding: more padding for single medication, less for multiple
                    int paddingDp = prescriptions.size() == 1 ? 16 : (prescriptions.size() == 2 ? 14 : 12);
                    int paddingPx = (int) (paddingDp * getResources().getDisplayMetrics().density);
                    cardLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
                    
                    // Medicine name (bold, larger)
                    TextView medicineText = new TextView(getContext());
                    medicineText.setText(prescription.getMedication() != null ? prescription.getMedication() : "Unknown");
                    medicineText.setTextSize(16);
                    medicineText.setTextColor(getResources().getColor(R.color.text_primary));
                    medicineText.setTypeface(null, android.graphics.Typeface.BOLD);
                    LinearLayout.LayoutParams medicineParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    int bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
                    medicineParams.setMargins(0, 0, 0, bottomMargin);
                    medicineText.setLayoutParams(medicineParams);
                    cardLayout.addView(medicineText);
                    
                    // Frequency
                    TextView frequencyText = new TextView(getContext());
                    String freqDisplay = prescription.getFrequency() != null ? prescription.getFrequency() : "N/A";
                    frequencyText.setText("Frequency: " + freqDisplay + " times/day");
                    frequencyText.setTextSize(14);
                    frequencyText.setTextColor(getResources().getColor(R.color.text_secondary));
                    LinearLayout.LayoutParams freqParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    freqParams.setMargins(0, 0, 0, (int) (4 * getResources().getDisplayMetrics().density));
                    frequencyText.setLayoutParams(freqParams);
                    cardLayout.addView(frequencyText);
                    
                    // Duration (just show days)
                    TextView durationText = new TextView(getContext());
                    String durationDisplay = prescription.getDuration() != null ? prescription.getDuration() : "N/A";
                    // Extract just the number and add "days" suffix
                    if (!durationDisplay.equals("N/A")) {
                        // Remove "days" or "day" if already present, then add "days"
                        String cleanDuration = durationDisplay.toLowerCase().replaceAll("\\s*days?\\s*", "").trim();
                        if (!cleanDuration.isEmpty()) {
                            durationDisplay = cleanDuration + " days";
                        } else {
                            durationDisplay = "N/A";
                        }
                    }
                    durationText.setText(durationDisplay);
                    durationText.setTextSize(14);
                    durationText.setTextColor(getResources().getColor(R.color.text_secondary));
                    LinearLayout.LayoutParams durationParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    durationParams.setMargins(0, 0, 0, (int) (4 * getResources().getDisplayMetrics().density));
                    durationText.setLayoutParams(durationParams);
                    cardLayout.addView(durationText);
                    
                    // Total Doses Calculation
                    try {
                        int freq = Integer.parseInt(freqDisplay);
                        String durationStr = durationDisplay.toLowerCase().replaceAll("\\s*days?\\s*", "").trim();
                        int days = Integer.parseInt(durationStr);
                        int totalDoses = freq * days;
                        
                        TextView totalDosesText = new TextView(getContext());
                        totalDosesText.setText("Total Doses: " + totalDoses + " units");
                        totalDosesText.setTextSize(14);
                        totalDosesText.setTextColor(getResources().getColor(R.color.text_primary));
                        totalDosesText.setTypeface(null, android.graphics.Typeface.BOLD);
                        LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        totalParams.setMargins(0, (int) (4 * getResources().getDisplayMetrics().density), 0, 0);
                        totalDosesText.setLayoutParams(totalParams);
                        cardLayout.addView(totalDosesText);
                    } catch (NumberFormatException e) {
                        // Skip total doses if calculation fails
                    }
                    
                    // Stock Information (Current Stock)
                    String medicationName = prescription.getMedication();
                    if (medicationName != null && !medicationName.isEmpty() && databaseHelper != null) {
                        try {
                            Medicine medicine = databaseHelper.getMedicineByName(medicationName);
                            if (medicine != null) {
                                int stockQuantity = medicine.getStockQuantity();
                                String unit = medicine.getUnit() != null ? medicine.getUnit() : "units";
                                
                                TextView stockText = new TextView(getContext());
                                stockText.setText("Current Stock: " + stockQuantity + " " + unit);
                                stockText.setTextSize(14);
                                stockText.setTextColor(getResources().getColor(R.color.success_green)); // Green color
                                stockText.setTypeface(null, android.graphics.Typeface.BOLD);
                                LinearLayout.LayoutParams stockParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                stockParams.setMargins(0, (int) (4 * getResources().getDisplayMetrics().density), 0, 0);
                                stockText.setLayoutParams(stockParams);
                                cardLayout.addView(stockText);
                            } else {
                                // Medicine not found in inventory
                                TextView stockText = new TextView(getContext());
                                stockText.setText("Current Stock: Not in inventory");
                                stockText.setTextSize(14);
                                stockText.setTextColor(getResources().getColor(R.color.error_red));
                                stockText.setTypeface(null, android.graphics.Typeface.BOLD);
                                LinearLayout.LayoutParams stockParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                stockParams.setMargins(0, (int) (4 * getResources().getDisplayMetrics().density), 0, 0);
                                stockText.setLayoutParams(stockParams);
                                cardLayout.addView(stockText);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("MedicationDispensing", "❌ Error fetching stock for " + medicationName + ": " + e.getMessage(), e);
                            // Don't show stock if there's an error
                        }
                    }
                    
                    // Instructions (if available)
                    if (prescription.getInstructions() != null && !prescription.getInstructions().isEmpty() && !prescription.getInstructions().equals("None")) {
                        TextView instructionsText = new TextView(getContext());
                        instructionsText.setText("Instructions: " + prescription.getInstructions());
                        instructionsText.setTextSize(14);
                        instructionsText.setTextColor(getResources().getColor(R.color.text_secondary));
                        LinearLayout.LayoutParams instrParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        instrParams.setMargins(0, (int) (4 * getResources().getDisplayMetrics().density), 0, 0);
                        instructionsText.setLayoutParams(instrParams);
                        cardLayout.addView(instructionsText);
                    }
                    
                    medicationCard.addView(cardLayout);
                    medicationsContainer.addView(medicationCard);
                }
            }
        
        // Set up buttons
        MaterialButton dispenseButton = dialogView.findViewById(R.id.dispenseButton);
        ImageButton closeButton = dialogView.findViewById(R.id.closeRFIDButton);
        
            if (dispenseButton == null) {
                android.util.Log.e("MedicationDispensing", "❌ dispenseButton not found in dialog layout");
                Toast.makeText(getContext(), "Cannot load dispense button", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Dialog already created above
            
            // Configure NestedScrollView for proper scrolling (dialogView is the NestedScrollView root)
            if (dialogView instanceof androidx.core.widget.NestedScrollView) {
                androidx.core.widget.NestedScrollView scrollView = (androidx.core.widget.NestedScrollView) dialogView;
                try {
                    scrollView.setSmoothScrollingEnabled(true);
                    scrollView.setFillViewport(false);
                    scrollView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
                } catch (Exception e) {
                    android.util.Log.w("MedicationDispensing", "⚠️ Error configuring scroll view: " + e.getMessage());
                }
            }
            
            // Store prescriptions list for dispense action
            final java.util.List<Prescription> prescriptionsToDispense = new ArrayList<>(prescriptions);
            
            // Dispense medication button - dispense all medications
        dispenseButton.setOnClickListener(v -> {
                if (getContext() == null || getActivity() == null) {
                    android.util.Log.e("MedicationDispensing", "❌ Context or Activity is null when dispense button clicked");
                    return;
                }
                try {
            dialog.dismiss();
                    // Call dispense on UI thread to ensure context is available
                    getActivity().runOnUiThread(() -> {
                        // Process all prescriptions together (handles partial dispensing)
                        processAllPrescriptions(prescriptionsToDispense, patient);
                    });
                } catch (Exception e) {
                    android.util.Log.e("MedicationDispensing", "❌ Error in dispense button click: " + e.getMessage(), e);
                    e.printStackTrace();
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Dispense failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }
        });
        
        // Close button
            if (closeButton != null) {
                closeButton.setOnClickListener(v -> {
                    try {
                        dialog.dismiss();
                    } catch (Exception e) {
                        android.util.Log.e("MedicationDispensing", "❌ Error dismissing dialog: " + e.getMessage(), e);
                    }
                });
            }
        
        dialog.show();
        } catch (Exception e) {
            android.util.Log.e("MedicationDispensing", "❌ Error showing prescription dialog: " + e.getMessage(), e);
            e.printStackTrace();
            Toast.makeText(getContext(), "Cannot display prescription", Toast.LENGTH_SHORT).show();
        }
    }
    

    /**
     * Process all prescriptions together, handling partial dispensing and updating RFID tag
     */
    private void processAllPrescriptions(List<Prescription> prescriptions, Patient patient) {
        if (prescriptions == null || prescriptions.isEmpty() || patient == null) {
            Toast.makeText(getContext(), "No prescriptions found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentScannedRFIDUid == null || currentScannedRFIDUid.isEmpty()) {
            Toast.makeText(getContext(), "RFID tag not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.util.Log.d("MedicationDispensing", "🔄 Processing " + prescriptions.size() + " prescriptions");
        
        // Process each prescription and track results
        List<Prescription> remainingPrescriptions = new ArrayList<>();
        List<String> dispenseResults = new ArrayList<>();
        int fullyDispensedCount = 0;
        int partiallyDispensedCount = 0;
        int outOfStockCount = 0;
        int errorCount = 0;
        
        for (Prescription prescription : prescriptions) {
            DispenseResult result = processSinglePrescription(prescription, patient);
            
            if (result.isFullyDispensed) {
                fullyDispensedCount++;
                dispenseResults.add("✓ " + prescription.getMedication() + " - " + result.dispensedQuantity + " " + result.unit + " dispensed");
            } else if (result.isPartiallyDispensed) {
                partiallyDispensedCount++;
                dispenseResults.add("⚠ " + prescription.getMedication() + " - " + result.dispensedQuantity + " of " + result.requiredQuantity + " " + result.unit + " (insufficient stock)");
                remainingPrescriptions.add(result.remainingPrescription);
            } else if (result.isOutOfStock) {
                outOfStockCount++;
                dispenseResults.add("✗ " + prescription.getMedication() + " - Out of stock (needed: " + result.requiredQuantity + " " + result.unit + ")");
                remainingPrescriptions.add(prescription);
            } else {
                errorCount++;
                String errorMsg = getClearErrorMessage(result.errorMessage);
                dispenseResults.add("✗ " + prescription.getMedication() + " - " + errorMsg);
                remainingPrescriptions.add(prescription);
            }
        }
        
        // Update RFID tag with remaining prescriptions
        if (remainingPrescriptions.isEmpty()) {
            // All fully dispensed - delete RFID tag
            android.util.Log.d("MedicationDispensing", "✅ All prescriptions fully dispensed - deleting RFID tag");
            deletePrescriptionFromRFIDTag(currentScannedRFIDUid);
            currentScannedRFIDUid = null;
        } else {
            // Some remain - update RFID tag with remaining prescriptions
            android.util.Log.d("MedicationDispensing", "⚠️ " + remainingPrescriptions.size() + " prescriptions remain - updating RFID tag");
            updateRFIDTagWithRemainingPrescriptions(currentScannedRFIDUid, remainingPrescriptions, patient);
        }
        
        // Show clear summary
        showDispenseSummary(fullyDispensedCount, partiallyDispensedCount, outOfStockCount, errorCount, dispenseResults, remainingPrescriptions.size());
    }
    
    /**
     * Convert technical error messages to clear user-friendly messages
     */
    private String getClearErrorMessage(String technicalError) {
        if (technicalError == null || technicalError.isEmpty()) {
            return "Unable to dispense";
        }
        
        if (technicalError.contains("not found in inventory")) {
            return "Not in inventory";
        }
        if (technicalError.contains("Failed to update stock")) {
            return "Stock update failed";
        }
        if (technicalError.contains("Invalid prescription")) {
            return "Invalid prescription data";
        }
        if (technicalError.contains("Medication name is missing")) {
            return "Medication name missing";
        }
        if (technicalError.contains("Error checking medicine")) {
            return "Cannot check inventory";
        }
        
        return "Dispense failed";
    }
    
    /**
     * Show clear dispense summary dialog
     */
    private void showDispenseSummary(int fullyDispensed, int partiallyDispensed, int outOfStock, int errors, 
                                     List<String> results, int remainingCount) {
        if (getContext() == null || getActivity() == null) return;
        
        android.app.Dialog summaryDialog = new android.app.Dialog(getContext());
        summaryDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        summaryDialog.setCancelable(true);
        
        // Create main layout
        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        int paddingPx = (int) (24 * getResources().getDisplayMetrics().density);
        mainLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        mainLayout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        
        // Title - centered and green
        TextView title = new TextView(getContext());
        title.setText("Dispense Summary");
        title.setTextSize(20);
        title.setTextColor(getResources().getColor(R.color.success_green)); // Green color
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 0, 0, (int) (20 * getResources().getDisplayMetrics().density));
        title.setLayoutParams(titleParams);
        mainLayout.addView(title);
        
        // Results container - centered alignment
        LinearLayout resultsContainer = new LinearLayout(getContext());
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        resultsContainer.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        resultsContainer.setLayoutParams(containerParams);
        
        // Results - aligned and colored
        for (String result : results) {
            TextView resultText = new TextView(getContext());
            
            // Determine color based on result type
            int textColor = R.color.text_primary;
            if (result.startsWith("✅") || result.startsWith("✓")) {
                // Success - green
                textColor = R.color.success_green;
            } else if (result.startsWith("⚠️") || result.startsWith("▲")) {
                // Warning - orange (keep as is for visibility)
                textColor = R.color.warning_orange;
            } else if (result.startsWith("❌") || result.startsWith("✗")) {
                // Error - red
                textColor = R.color.error_red;
            }
            
            resultText.setText(result);
            resultText.setTextSize(15);
            resultText.setTextColor(getResources().getColor(textColor));
            resultText.setGravity(android.view.Gravity.CENTER);
            resultText.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams resultParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            resultParams.setMargins(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
            resultText.setLayoutParams(resultParams);
            resultsContainer.addView(resultText);
        }
        
        mainLayout.addView(resultsContainer);
        
        // Summary line - centered and green
        if (remainingCount > 0) {
            TextView summaryText = new TextView(getContext());
            summaryText.setText(remainingCount + " medication(s) remaining. Patient can return later.");
            summaryText.setTextSize(14);
            summaryText.setTextColor(getResources().getColor(R.color.success_green)); // Green color
            summaryText.setGravity(android.view.Gravity.CENTER);
            summaryText.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            summaryParams.setMargins(0, (int) (16 * getResources().getDisplayMetrics().density), 0, 0);
            summaryText.setLayoutParams(summaryParams);
            mainLayout.addView(summaryText);
        }
        
        // OK button - green
        MaterialButton okButton = new MaterialButton(getContext());
        okButton.setText("OK");
        okButton.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        okButton.setPadding(0, (int) (16 * getResources().getDisplayMetrics().density), 0, 0);
        okButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.success_green))); // Green button
        okButton.setTextColor(getResources().getColor(R.color.white));
        okButton.setTypeface(null, android.graphics.Typeface.BOLD);
        okButton.setCornerRadius((int) (12 * getResources().getDisplayMetrics().density));
        okButton.setOnClickListener(v -> summaryDialog.dismiss());
        mainLayout.addView(okButton);
        
        summaryDialog.setContentView(mainLayout);
        
        // Set dialog window properties
        if (summaryDialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams layoutParams = summaryDialog.getWindow().getAttributes();
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            layoutParams.width = (int) (screenWidth * 0.85);
            layoutParams.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = android.view.Gravity.CENTER;
            summaryDialog.getWindow().setAttributes(layoutParams);
            summaryDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }
        
        summaryDialog.show();
    }
    
    /**
     * Result class for dispense operation
     */
    private static class DispenseResult {
        boolean isFullyDispensed = false;
        boolean isPartiallyDispensed = false;
        boolean isOutOfStock = false;
        int dispensedQuantity = 0;
        int requiredQuantity = 0;
        String unit = "";
        Prescription remainingPrescription = null;
        String errorMessage = "";
    }
    
    /**
     * Process a single prescription - handles partial dispensing
     */
    private DispenseResult processSinglePrescription(Prescription prescription, Patient patient) {
        DispenseResult result = new DispenseResult();
        
        if (prescription == null || patient == null || databaseHelper == null) {
            result.errorMessage = "Invalid data";
            return result;
        }
        
        String medicationName = prescription.getMedication();
        if (medicationName == null || medicationName.isEmpty()) {
            result.errorMessage = "Medication name missing";
            return result;
        }
        
        // Get medicine from inventory
        Medicine medicine = null;
        try {
            medicine = databaseHelper.getMedicineByName(medicationName);
        } catch (Exception e) {
            android.util.Log.e("MedicationDispensing", "❌ Error getting medicine: " + e.getMessage(), e);
            result.errorMessage = "Cannot access inventory";
            return result;
        }
        
        if (medicine == null) {
            result.errorMessage = "Not in inventory";
            return result;
        }
        
        if (!medicine.isInStock()) {
            result.isOutOfStock = true;
            result.requiredQuantity = 0;
            result.unit = medicine.getUnit() != null ? medicine.getUnit() : "units";
            return result;
        }
        
        // Calculate required quantity
        int frequency = 1;
        int days = 1;
        try {
            String freqStr = prescription.getFrequency();
            if (freqStr != null && !freqStr.trim().isEmpty()) {
                frequency = Integer.parseInt(freqStr.trim());
            }
        } catch (NumberFormatException e) {
            android.util.Log.w("MedicationDispensing", "⚠️ Invalid frequency, using default: 1");
        }
        
        try {
            String durationStr = prescription.getDuration();
            if (durationStr != null && !durationStr.trim().isEmpty()) {
                durationStr = durationStr.trim().toLowerCase().replaceAll("\\s*days?\\s*", "");
                days = Integer.parseInt(durationStr);
            }
        } catch (NumberFormatException e) {
            android.util.Log.w("MedicationDispensing", "⚠️ Invalid duration, using default: 1");
        }
        
        int requiredQuantity = frequency * days;
        int stockQuantity = medicine.getStockQuantity();
        String unit = medicine.getUnit() != null ? medicine.getUnit() : "units";
        
        result.requiredQuantity = requiredQuantity;
        result.unit = unit;
        
        if (stockQuantity >= requiredQuantity) {
            // Fully dispense
            int newStock = stockQuantity - requiredQuantity;
            boolean stockUpdated = databaseHelper.updateMedicineStock(medicationName, newStock);
            
            if (stockUpdated) {
                // Update prescription status and add deducted amount note
                prescription.setStatus("Dispensed");
                String originalInstructions = prescription.getInstructions() != null ? prescription.getInstructions() : "";
                prescription.setInstructions(originalInstructions + "\n[Deducted: " + requiredQuantity + " " + unit + " from inventory]");
                // Update created_date to current time so it appears at top of history
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                prescription.setCreatedDate(sdf.format(new java.util.Date()));
                
                if (prescription.getDoctorId() != null && !prescription.getDoctorId().isEmpty()) {
                    try {
                        // Check if prescription exists in database first
                        Prescription existingPrescription = databaseHelper.getPrescriptionById(prescription.getPrescriptionId());
                        boolean saved = false;
                        
                        if (existingPrescription != null) {
                            // Prescription exists - update it
                            saved = databaseHelper.updatePrescription(prescription);
                            if (saved) {
                                android.util.Log.d("MedicationDispensing", "✅ Prescription updated in history: " + prescription.getPrescriptionId());
                            } else {
                                android.util.Log.e("MedicationDispensing", "❌ Failed to update prescription in history");
                            }
                        } else {
                            // Prescription doesn't exist - add it as new
                            android.util.Log.d("MedicationDispensing", "📝 Prescription not found in database, adding as new: " + prescription.getPrescriptionId());
                            saved = databaseHelper.addPrescription(prescription);
                            if (saved) {
                                android.util.Log.d("MedicationDispensing", "✅ Prescription added to history: " + prescription.getPrescriptionId());
                                android.util.Log.d("MedicationDispensing", "   Medication: " + medicationName);
                                android.util.Log.d("MedicationDispensing", "   Status: " + prescription.getStatus());
                            } else {
                                android.util.Log.e("MedicationDispensing", "❌ Failed to add prescription to history: " + prescription.getPrescriptionId());
                                android.util.Log.e("MedicationDispensing", "   Medication: " + medicationName);
                                android.util.Log.e("MedicationDispensing", "   Doctor ID: " + prescription.getDoctorId());
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("MedicationDispensing", "❌ Error saving prescription to history: " + e.getMessage(), e);
                        android.util.Log.e("MedicationDispensing", "   Prescription ID: " + prescription.getPrescriptionId());
                        android.util.Log.e("MedicationDispensing", "   Medication: " + medicationName);
                        // Try to add as fallback if update fails
                        try {
                            android.util.Log.d("MedicationDispensing", "🔄 Attempting fallback add for: " + prescription.getPrescriptionId());
                            boolean added = databaseHelper.addPrescription(prescription);
                            if (added) {
                                android.util.Log.d("MedicationDispensing", "✅ Prescription added to history (fallback): " + prescription.getPrescriptionId());
                            } else {
                                android.util.Log.e("MedicationDispensing", "❌ Fallback add also failed for: " + prescription.getPrescriptionId());
                            }
                        } catch (Exception e2) {
                            android.util.Log.e("MedicationDispensing", "❌ Fallback add exception: " + e2.getMessage(), e2);
                        }
                    }
                } else {
                    android.util.Log.w("MedicationDispensing", "⚠️ Cannot save prescription - doctor_id is missing: " + prescription.getPrescriptionId());
                    android.util.Log.w("MedicationDispensing", "   Medication: " + medicationName);
                    // Try to save anyway with fallback doctor_id
                    try {
                        prescription.setDoctorId("UNKNOWN");
                        boolean saved = databaseHelper.addPrescription(prescription);
                        if (saved) {
                            android.util.Log.d("MedicationDispensing", "✅ Prescription saved with fallback doctor_id: " + prescription.getPrescriptionId());
                        }
                    } catch (Exception e) {
                        android.util.Log.e("MedicationDispensing", "❌ Failed to save with fallback doctor_id: " + e.getMessage(), e);
                    }
                }
                
                result.isFullyDispensed = true;
                result.dispensedQuantity = requiredQuantity;
                android.util.Log.d("MedicationDispensing", "✅ Fully dispensed: " + medicationName + " (" + requiredQuantity + " " + unit + ")");
            } else {
                result.errorMessage = "Stock update failed";
            }
        } else if (stockQuantity > 0) {
            // Partially dispense
            int dispensedQuantity = stockQuantity;
            int remainingQuantity = requiredQuantity - stockQuantity;
            int remainingDays = (int) Math.ceil((double) remainingQuantity / frequency);
            
            // Update stock to 0
            boolean stockUpdated = databaseHelper.updateMedicineStock(medicationName, 0);
            
            if (stockUpdated) {
                // Create remaining prescription
                Prescription remainingPrescription = new Prescription();
                remainingPrescription.setPrescriptionId(prescription.getPrescriptionId());
                remainingPrescription.setPatientId(prescription.getPatientId());
                remainingPrescription.setPatientName(prescription.getPatientName());
                remainingPrescription.setMedication(prescription.getMedication());
                remainingPrescription.setFrequency(prescription.getFrequency());
                remainingPrescription.setDuration(remainingDays + " days");
                remainingPrescription.setInstructions(prescription.getInstructions());
                remainingPrescription.setDoctorId(prescription.getDoctorId());
                remainingPrescription.setDoctorName(prescription.getDoctorName());
                remainingPrescription.setCreatedDate(prescription.getCreatedDate());
                remainingPrescription.setStatus("Active");
                
                // Save partial dispense to history with current date/time
                Prescription partialPrescription = new Prescription();
                // Use unique ID with timestamp to avoid conflicts
                String uniqueId = prescription.getPrescriptionId() + "_PARTIAL_" + System.currentTimeMillis();
                partialPrescription.setPrescriptionId(uniqueId);
                partialPrescription.setPatientId(prescription.getPatientId() != null ? prescription.getPatientId() : "");
                partialPrescription.setPatientName(prescription.getPatientName() != null ? prescription.getPatientName() : "");
                partialPrescription.setMedication(prescription.getMedication() != null ? prescription.getMedication() : "");
                partialPrescription.setDosage(prescription.getDosage() != null ? prescription.getDosage() : "N/A"); // Ensure dosage is set
                partialPrescription.setFrequency(prescription.getFrequency() != null ? prescription.getFrequency() : "1");
                // Calculate actual days dispensed
                int daysDispensed = dispensedQuantity / frequency;
                if (daysDispensed == 0) daysDispensed = 1; // At least 1 day
                partialPrescription.setDuration(daysDispensed + " days");
                // Add deducted amount note to instructions
                String originalInstructions = prescription.getInstructions() != null ? prescription.getInstructions() : "";
                partialPrescription.setInstructions(originalInstructions + "\n[Deducted: " + dispensedQuantity + " " + unit + " from inventory]");
                partialPrescription.setDoctorId(prescription.getDoctorId() != null ? prescription.getDoctorId() : "");
                partialPrescription.setDoctorName(prescription.getDoctorName() != null ? prescription.getDoctorName() : "");
                // Set current date/time for dispensed date (so it appears at top of history)
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                partialPrescription.setCreatedDate(sdf.format(new java.util.Date()));
                partialPrescription.setStatus("Dispensed");
                
                // Always try to save partial prescription (even if doctor_id is missing, we'll use a fallback)
                try {
                    // Ensure ALL required fields are set before saving
                    if (partialPrescription.getDoctorId() == null || partialPrescription.getDoctorId().isEmpty()) {
                        partialPrescription.setDoctorId("UNKNOWN");
                        android.util.Log.w("MedicationDispensing", "⚠️ doctor_id missing for partial prescription, using fallback");
                    }
                    
                    if (partialPrescription.getPatientId() == null || partialPrescription.getPatientId().isEmpty()) {
                        partialPrescription.setPatientId("UNKNOWN_PATIENT");
                        android.util.Log.w("MedicationDispensing", "⚠️ patient_id missing for partial prescription, using fallback");
                    }
                    
                    if (partialPrescription.getPatientName() == null || partialPrescription.getPatientName().isEmpty()) {
                        partialPrescription.setPatientName("Unknown Patient");
                        android.util.Log.w("MedicationDispensing", "⚠️ patient_name missing for partial prescription, using fallback");
                    }
                    
                    if (partialPrescription.getDoctorName() == null || partialPrescription.getDoctorName().isEmpty()) {
                        partialPrescription.setDoctorName("Unknown Doctor");
                        android.util.Log.w("MedicationDispensing", "⚠️ doctor_name missing for partial prescription, using fallback");
                    }
                    
                    if (partialPrescription.getDosage() == null || partialPrescription.getDosage().isEmpty()) {
                        partialPrescription.setDosage("N/A");
                        android.util.Log.w("MedicationDispensing", "⚠️ dosage missing for partial prescription, using fallback");
                    }
                    
                    // Partial prescriptions always have unique IDs, so always add as new
                    android.util.Log.d("MedicationDispensing", "📝 Saving partial prescription to history:");
                    android.util.Log.d("MedicationDispensing", "   Prescription ID: " + partialPrescription.getPrescriptionId());
                    android.util.Log.d("MedicationDispensing", "   Medication: " + medicationName);
                    android.util.Log.d("MedicationDispensing", "   Status: " + partialPrescription.getStatus());
                    android.util.Log.d("MedicationDispensing", "   Doctor ID: " + partialPrescription.getDoctorId());
                    android.util.Log.d("MedicationDispensing", "   Doctor Name: " + partialPrescription.getDoctorName());
                    android.util.Log.d("MedicationDispensing", "   Patient ID: " + partialPrescription.getPatientId());
                    android.util.Log.d("MedicationDispensing", "   Patient Name: " + partialPrescription.getPatientName());
                    android.util.Log.d("MedicationDispensing", "   Created Date: " + partialPrescription.getCreatedDate());
                    android.util.Log.d("MedicationDispensing", "   Dosage: " + partialPrescription.getDosage());
                    android.util.Log.d("MedicationDispensing", "   Frequency: " + partialPrescription.getFrequency());
                    android.util.Log.d("MedicationDispensing", "   Duration: " + partialPrescription.getDuration());
                    
                    boolean saved = databaseHelper.addPrescription(partialPrescription);
                    
                    if (saved) {
                        android.util.Log.d("MedicationDispensing", "✅✅✅ Partial prescription saved to history: " + partialPrescription.getPrescriptionId());
                        android.util.Log.d("MedicationDispensing", "   Medication: " + medicationName);
                        android.util.Log.d("MedicationDispensing", "   Dispensed: " + dispensedQuantity + " " + unit);
                        android.util.Log.d("MedicationDispensing", "   Required: " + requiredQuantity + " " + unit);
                        android.util.Log.d("MedicationDispensing", "   Status: " + partialPrescription.getStatus());
                    } else {
                        android.util.Log.e("MedicationDispensing", "❌❌❌ Failed to save partial prescription to history: " + partialPrescription.getPrescriptionId());
                        android.util.Log.e("MedicationDispensing", "   Medication: " + medicationName);
                        android.util.Log.e("MedicationDispensing", "   Doctor ID: " + partialPrescription.getDoctorId());
                        android.util.Log.e("MedicationDispensing", "   Patient ID: " + partialPrescription.getPatientId());
                        android.util.Log.e("MedicationDispensing", "   This prescription will NOT appear in history!");
                    }
                } catch (Exception e) {
                    android.util.Log.e("MedicationDispensing", "❌ Exception saving partial prescription: " + e.getMessage(), e);
                    android.util.Log.e("MedicationDispensing", "   Prescription ID: " + partialPrescription.getPrescriptionId());
                    android.util.Log.e("MedicationDispensing", "   Medication: " + medicationName);
                    e.printStackTrace();
                }
                
                result.isPartiallyDispensed = true;
                result.dispensedQuantity = dispensedQuantity;
                result.remainingPrescription = remainingPrescription;
                android.util.Log.d("MedicationDispensing", "⚠️ Partially dispensed: " + medicationName + " (" + dispensedQuantity + " of " + requiredQuantity + " " + unit + ")");
            } else {
                result.errorMessage = "Stock update failed";
            }
        } else {
            // Out of stock
            result.isOutOfStock = true;
        }
        
        return result;
    }
    
    /**
     * Update RFID tag with remaining prescriptions
     */
    private void updateRFIDTagWithRemainingPrescriptions(String rfidUid, List<Prescription> remainingPrescriptions, Patient patient) {
        if (rfidUid == null || rfidUid.isEmpty() || remainingPrescriptions == null || remainingPrescriptions.isEmpty()) {
            android.util.Log.w("MedicationDispensing", "⚠️ Cannot update RFID tag: invalid data");
            return;
        }
        
        try {
            DatabaseReference rfidTagRef = rfidHelper.getRFIDTagRef(rfidUid);
            if (rfidTagRef == null) {
                android.util.Log.e("MedicationDispensing", "❌ Failed to get RFID tag reference for update: " + rfidUid);
                return;
            }
            
            // Get original prescription data to preserve patient/doctor info
            rfidTagRef.child("prescription").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        android.util.Log.w("MedicationDispensing", "⚠️ Original prescription data not found");
                        return;
                    }
                    
                    Map<String, Object> originalData = (Map<String, Object>) snapshot.getValue();
                    if (originalData == null) {
                        android.util.Log.w("MedicationDispensing", "⚠️ Original prescription data is null");
                        return;
                    }
                    
                    // Create updated prescription data with remaining medications
                    Map<String, Object> updatedPrescriptionData = new HashMap<>();
                    updatedPrescriptionData.put("patient_id", originalData.get("patient_id"));
                    updatedPrescriptionData.put("patient_name", originalData.get("patient_name"));
                    updatedPrescriptionData.put("doctor_id", originalData.get("doctor_id"));
                    updatedPrescriptionData.put("doctor_name", originalData.get("doctor_name"));
                    updatedPrescriptionData.put("created_date", originalData.get("created_date"));
                    updatedPrescriptionData.put("status", "Active");
                    updatedPrescriptionData.put("rfid_uid", rfidUid);
                    
                    // Create medications list from remaining prescriptions
                    List<Map<String, Object>> remainingMedicationsList = new ArrayList<>();
                    for (Prescription prescription : remainingPrescriptions) {
                        Map<String, Object> medData = new HashMap<>();
                        medData.put("medication", prescription.getMedication());
                        medData.put("frequency", prescription.getFrequency());
                        medData.put("duration", prescription.getDuration());
                        medData.put("instructions", prescription.getInstructions());
                        medData.put("prescription_id", prescription.getPrescriptionId());
                        remainingMedicationsList.add(medData);
                    }
                    updatedPrescriptionData.put("medications", remainingMedicationsList);
                    
                    // Update Firebase
                    rfidTagRef.child("prescription").setValue(updatedPrescriptionData)
                        .addOnSuccessListener(aVoid -> {
                            android.util.Log.d("MedicationDispensing", "✅ RFID tag updated with remaining prescriptions");
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("MedicationDispensing", "❌ Failed to update RFID tag: " + e.getMessage(), e);
                        });
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    android.util.Log.e("MedicationDispensing", "❌ Error reading prescription data: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            android.util.Log.e("MedicationDispensing", "❌ Error updating RFID tag: " + e.getMessage(), e);
        }
    }
    
    /**
     * Dispense medication for prescription (legacy method - kept for compatibility)
     */
    private void dispenseMedication(Prescription prescription, Patient patient) {
        // Null checks
        if (getContext() == null) {
            android.util.Log.e("MedicationDispensing", "❌ Context is null in dispenseMedication");
            return;
        }
        
        if (prescription == null) {
            Toast.makeText(getContext(), "❌ Prescription data is missing", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (patient == null) {
            Toast.makeText(getContext(), "❌ Patient data is missing", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (databaseHelper == null) {
            android.util.Log.e("MedicationDispensing", "❌ DatabaseHelper is null");
            databaseHelper = new HCasDatabaseHelper(getContext());
        }
        
        // Check if medicine is available in stock
        String medicationName = prescription.getMedication();
        if (medicationName == null || medicationName.isEmpty()) {
            Toast.makeText(getContext(), "❌ Medication name is missing", Toast.LENGTH_LONG).show();
            return;
        }
        
        Medicine medicine = null;
        try {
            medicine = databaseHelper.getMedicineByName(medicationName);
        } catch (Exception e) {
            android.util.Log.e("MedicationDispensing", "❌ Error getting medicine: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Cannot check inventory", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (medicine == null) {
            Toast.makeText(getContext(), medicationName + " not found in inventory", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
        if (!medicine.isInStock()) {
                Toast.makeText(getContext(), medicationName + " is out of stock", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            android.util.Log.e("MedicationDispensing", "❌ Error checking stock: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Cannot check stock", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get safe values for display
        String patientName = patient.getFullName() != null ? patient.getFullName() : "Unknown Patient";
        int stockQuantity = medicine.getStockQuantity();
        String unit = medicine.getUnit() != null ? medicine.getUnit() : "units";
        
        // Calculate total doses for display
        int frequency = 1;
        int days = 1;
        try {
            String freqStr = prescription.getFrequency();
            if (freqStr != null && !freqStr.trim().isEmpty()) {
                frequency = Integer.parseInt(freqStr.trim());
            }
        } catch (NumberFormatException e) {
            // Use default
        }
        
        try {
            String durationStr = prescription.getDuration();
            if (durationStr != null && !durationStr.trim().isEmpty()) {
                durationStr = durationStr.trim().toLowerCase().replaceAll("\\s*days?\\s*", "");
                days = Integer.parseInt(durationStr);
            }
        } catch (NumberFormatException e) {
            // Use default
        }
        
        int totalDoses = frequency * days;
        
        // Show dispensing confirmation with custom layout
        android.app.Dialog confirmDialog = new android.app.Dialog(getContext());
        confirmDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        confirmDialog.setCancelable(true);
        
        // Inflate custom dialog layout
        View confirmDialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_dispense_confirmation, null);
        confirmDialog.setContentView(confirmDialogView);
        
        // Set dialog window properties
        if (confirmDialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams layoutParams = confirmDialog.getWindow().getAttributes();
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int marginDp = 24;
            int marginPx = (int) (marginDp * getResources().getDisplayMetrics().density);
            layoutParams.width = screenWidth - (marginPx * 2);
            layoutParams.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = android.view.Gravity.CENTER;
            confirmDialog.getWindow().setAttributes(layoutParams);
            confirmDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
            confirmDialog.getWindow().setElevation(16);
            confirmDialog.getWindow().setDimAmount(0.5f);
        }
        
        // Populate dialog views
        TextView confirmationQuestion = confirmDialogView.findViewById(R.id.confirmationQuestion);
        TextView dialogFrequency = confirmDialogView.findViewById(R.id.dialogFrequency);
        TextView dialogDuration = confirmDialogView.findViewById(R.id.dialogDuration);
        TextView dialogTotalDoses = confirmDialogView.findViewById(R.id.dialogTotalDoses);
        TextView dialogCurrentStock = confirmDialogView.findViewById(R.id.dialogCurrentStock);
        TextView dialogStockAfter = confirmDialogView.findViewById(R.id.dialogStockAfter);
        MaterialButton cancelButton = confirmDialogView.findViewById(R.id.cancelButton);
        MaterialButton confirmDispenseButton = confirmDialogView.findViewById(R.id.confirmDispenseButton);
        
        if (confirmationQuestion != null) {
            confirmationQuestion.setText("Dispense " + medicationName + " to " + patientName + "?");
        }
        if (dialogFrequency != null) {
            dialogFrequency.setText(frequency + " times/day");
        }
        if (dialogDuration != null) {
            dialogDuration.setText(days + " days");
        }
        if (dialogTotalDoses != null) {
            dialogTotalDoses.setText(totalDoses + " " + unit);
        }
        if (dialogCurrentStock != null) {
            dialogCurrentStock.setText(stockQuantity + " " + unit);
        }
        if (dialogStockAfter != null) {
            dialogStockAfter.setText((stockQuantity - totalDoses) + " " + unit);
        }
        
        // Set up button listeners
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> confirmDialog.dismiss());
        }
        
        if (confirmDispenseButton != null) {
            confirmDispenseButton.setOnClickListener(v -> {
                confirmDialog.dismiss();
                try {
                // Use the already calculated frequency, days, and totalDoses from above
                
                if (totalDoses <= 0) {
                    Toast.makeText(getContext(), "❌ Invalid frequency or duration. Please check prescription.", Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Deduct from stock: frequency × days
                int newStock = stockQuantity - totalDoses;
                if (newStock < 0) {
                    Toast.makeText(getContext(), "❌ Cannot dispense: Insufficient stock. Need " + totalDoses + " " + unit + ", but only " + stockQuantity + " available.", Toast.LENGTH_LONG).show();
                    return;
                }
                
                boolean stockUpdated = false;
                try {
                    stockUpdated = databaseHelper.updateMedicineStock(medicationName, newStock);
                } catch (Exception e) {
                    android.util.Log.e("MedicationDispensing", "❌ Error updating stock: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Cannot update stock", Toast.LENGTH_SHORT).show();
                    return;
                }
            
            if (stockUpdated) {
                // Update prescription status to dispensed
                prescription.setStatus("Dispensed");
                    
                    // Validate required fields before updating
                    if (prescription.getDoctorId() == null || prescription.getDoctorId().isEmpty()) {
                        android.util.Log.e("MedicationDispensing", "❌ Prescription doctor_id is missing! Cannot update prescription.");
                        android.util.Log.e("MedicationDispensing", "  Prescription ID: " + prescription.getPrescriptionId());
                        android.util.Log.e("MedicationDispensing", "  Patient ID: " + prescription.getPatientId());
                        android.util.Log.e("MedicationDispensing", "  Doctor Name: " + prescription.getDoctorName());
                        Toast.makeText(getContext(), 
                            "⚠️ Medication dispensed but failed to update prescription status.\n" +
                            "Missing doctor ID. Stock remaining: " + newStock + " " + unit, 
                            Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    boolean updated = false;
                    try {
                        android.util.Log.d("MedicationDispensing", "📝 Updating prescription status to 'Dispensed'");
                        android.util.Log.d("MedicationDispensing", "  Prescription ID: " + prescription.getPrescriptionId());
                        android.util.Log.d("MedicationDispensing", "  Doctor ID: " + prescription.getDoctorId());
                        android.util.Log.d("MedicationDispensing", "  Doctor Name: " + prescription.getDoctorName());
                        updated = databaseHelper.updatePrescription(prescription);
                    } catch (Exception e) {
                        android.util.Log.e("MedicationDispensing", "❌ Error updating prescription: " + e.getMessage(), e);
                        e.printStackTrace();
                        // Stock was updated but prescription update failed - still show success for stock
                        Toast.makeText(getContext(), 
                            "⚠️ Medication dispensed but failed to update prescription status.\n" +
                            "Error: " + e.getMessage() + "\n" +
                            "Stock remaining: " + newStock + " " + unit, 
                            Toast.LENGTH_LONG).show();
                        return;
                    }
                
                if (updated) {
                        // Store deducted amount in prescription for history tracking
                        // We'll use the instructions field or add a note about deducted amount
                        String originalInstructions = prescription.getInstructions() != null ? prescription.getInstructions() : "";
                        String deductedNote = "\n[Deducted: " + totalDoses + " " + unit + " from inventory]";
                        prescription.setInstructions(originalInstructions + deductedNote);
                        
                        // Update prescription again with deducted amount info
                        try {
                            databaseHelper.updatePrescription(prescription);
                        } catch (Exception e) {
                            android.util.Log.w("MedicationDispensing", "⚠️ Could not update prescription with deducted amount: " + e.getMessage());
                        }
                        
                        Toast.makeText(getContext(), "✅ Medication dispensed successfully!\n" +
                            "Deducted: " + totalDoses + " " + unit + "\n" +
                            "Stock remaining: " + newStock + " " + unit, Toast.LENGTH_LONG).show();
                        
                        // Delete prescription from RFID tag in Firebase to make tag reusable
                        if (currentScannedRFIDUid != null && !currentScannedRFIDUid.isEmpty()) {
                            deletePrescriptionFromRFIDTag(currentScannedRFIDUid);
                            currentScannedRFIDUid = null; // Clear after deletion
                        }
                        
                        // Refresh history after successful dispense
                        // History is now in separate fragment
                } else {
                        Toast.makeText(getContext(), 
                            "⚠️ Medication dispensed but failed to update prescription status.\n" +
                            "Stock remaining: " + newStock + " " + unit, 
                            Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getContext(), "❌ Failed to update medicine stock.", Toast.LENGTH_LONG).show();
            }
            } catch (Exception e) {
                android.util.Log.e("MedicationDispensing", "❌ Error in dispense operation: " + e.getMessage(), e);
                e.printStackTrace();
                Toast.makeText(getContext(), "Dispense failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        try {
            confirmDialog.show();
        } catch (Exception e) {
            android.util.Log.e("MedicationDispensing", "❌ Error showing dispense dialog: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Cannot show confirmation", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Stop RFID listening when fragment is paused
        if (rfidHelper != null) {
            rfidHelper.stopListening();
        }
        isScanning = false;
        if (scanningDialog != null && scanningDialog.isShowing()) {
            scanningDialog.dismiss();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop RFID listening when fragment is destroyed
        if (rfidHelper != null) {
            rfidHelper.stopListening();
        }
    }
    
    /**
     * Helper method to safely get string value from map
     */
    private String getStringValue(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return "";
        }
        Object value = map.get(key);
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }
    
    /**
     * Delete prescription data from RFID tag in Firebase after successful dispense
     * This makes the RFID tag reusable for new prescriptions
     */
    private void deletePrescriptionFromRFIDTag(String rfidUid) {
        if (rfidUid == null || rfidUid.isEmpty()) {
            android.util.Log.w("MedicationDispensing", "⚠️ Cannot delete prescription: RFID UID is null or empty");
            return;
        }
        
        try {
            DatabaseReference rfidTagRef = rfidHelper.getRFIDTagRef(rfidUid);
            if (rfidTagRef == null) {
                android.util.Log.e("MedicationDispensing", "❌ Failed to get RFID tag reference for deletion: " + rfidUid);
                return;
            }
            
            // Delete the prescription data from the RFID tag
            DatabaseReference prescriptionRef = rfidTagRef.child("prescription");
            android.util.Log.d("MedicationDispensing", "🗑️ Deleting prescription from RFID tag: " + rfidUid);
            android.util.Log.d("MedicationDispensing", "🗑️ Firebase path: HCAS/rfid_tags/" + rfidUid + "/prescription");
            
            prescriptionRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("MedicationDispensing", "✅✅✅ Prescription deleted from RFID tag successfully!");
                    android.util.Log.d("MedicationDispensing", "✅ RFID tag " + rfidUid + " is now empty and ready for reuse");
                    
                    // Also delete the entire RFID tag node if it only contained prescription data
                    // This ensures a clean state for the next prescription
                    rfidTagRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot != null && snapshot.exists()) {
                                // Check if tag node is now empty or only has minimal data
                                boolean shouldDeleteTag = true;
                                for (DataSnapshot child : snapshot.getChildren()) {
                                    // If there are other children besides prescription, keep the tag node
                                    if (!child.getKey().equals("prescription")) {
                                        shouldDeleteTag = false;
                                        break;
                                    }
                                }
                                
                                if (shouldDeleteTag) {
                                    // Delete the entire tag node since it's now empty
                                    rfidTagRef.removeValue()
                                        .addOnSuccessListener(aVoid2 -> {
                                            android.util.Log.d("MedicationDispensing", "✅ RFID tag node completely removed for reuse");
                                        })
                                        .addOnFailureListener(e -> {
                                            android.util.Log.w("MedicationDispensing", "⚠️ Failed to remove RFID tag node (non-critical): " + e.getMessage());
                                        });
                                }
                            }
                        }
                        
                        @Override
                        public void onCancelled(DatabaseError error) {
                            android.util.Log.w("MedicationDispensing", "⚠️ Error checking RFID tag node (non-critical): " + error.getMessage());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MedicationDispensing", "❌ Failed to delete prescription from RFID tag: " + e.getMessage(), e);
                    // Don't show error to user as this is a background cleanup operation
                });
        } catch (Exception e) {
            android.util.Log.e("MedicationDispensing", "❌ Error deleting prescription from RFID tag: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load dispensed prescriptions history
     */
    @Override
    public void onResume() {
        super.onResume();
        // Fragment resumed
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
    
    /**
     * Adapter for dispensed history RecyclerView
     */
}



