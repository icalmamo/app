package com.healthcare.cas;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import com.healthcare.cas.database.HCasDatabaseHelper;
import com.healthcare.cas.database.FirebaseRTDBHelper;
import com.healthcare.cas.models.Patient;
import com.healthcare.cas.models.Prescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PatientHistoryFragment displays patients who have received prescriptions.
 */
public class PatientHistoryFragment extends Fragment {

    private RecyclerView patientHistoryRecyclerView;
    private TextView emptyStateTextView;
    private TextInputEditText searchPatientInput;
    private HCasDatabaseHelper databaseHelper;
    private FirebaseRTDBHelper firebaseRTDBHelper;
    private PatientHistoryAdapter patientHistoryAdapter;
    private List<PatientHistoryItem> allHistoryItems;
    private Map<String, List<Prescription>> prescriptionsByPatientId;
    private boolean searchEnabled = false;
    private boolean isLoading = false; // Flag to prevent duplicate loading
    private TextWatcher searchTextWatcher; // Store text watcher to enable/disable it

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Check if search mode is enabled
        if (getArguments() != null) {
            searchEnabled = getArguments().getBoolean("enable_search", false);
        }
        
        initializeViews(view);
        initializeDatabase();
        setupRecyclerView();
        setupSearchBar();
        loadPatientHistory();
    }

    private void initializeViews(View view) {
        patientHistoryRecyclerView = view.findViewById(R.id.patientHistoryRecyclerView);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
        searchPatientInput = view.findViewById(R.id.searchPatientInput);
    }
    
    private void setupSearchBar() {
        if (searchPatientInput != null) {
            // Clear any existing text first
            searchPatientInput.setText("");
            
            // Always enable search functionality
            searchTextWatcher = new TextWatcher() {
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
                    // Only filter if we have data loaded
                    if (allHistoryItems != null) {
                        filterPatientHistory(s.toString().trim());
                    } else {
                        android.util.Log.d("PatientHistory", "⚠️ Search triggered but allHistoryItems is null, skipping filter");
                    }
                }
            };
            searchPatientInput.addTextChangedListener(searchTextWatcher);
            
            // If search is enabled from button, focus on search input and show keyboard
            if (searchEnabled) {
                searchPatientInput.post(() -> {
                    searchPatientInput.requestFocus();
                    // Show keyboard
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                        getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(searchPatientInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        }
    }
    
    private void filterPatientHistory(String searchQuery) {
        android.util.Log.d("PatientHistory", "🔍 Filtering with query: '" + searchQuery + "', allHistoryItems size: " + (allHistoryItems != null ? allHistoryItems.size() : 0));
        
        if (searchQuery.isEmpty()) {
            // Show all prescription history items if search is empty
            if (patientHistoryAdapter != null && allHistoryItems != null) {
                android.util.Log.d("PatientHistory", "🔍 Showing all " + allHistoryItems.size() + " items (empty search)");
                // Only update if the list is different to avoid unnecessary updates
                if (patientHistoryAdapter.getItemCount() != allHistoryItems.size()) {
                    patientHistoryAdapter.setPatientHistoryItems(allHistoryItems);
                } else {
                    android.util.Log.d("PatientHistory", "🔍 Adapter already has " + allHistoryItems.size() + " items, skipping update");
                }
            } else {
                android.util.Log.w("PatientHistory", "⚠️ Cannot show all items - adapter or allHistoryItems is null");
            }
            updateEmptyState(allHistoryItems == null || allHistoryItems.isEmpty());
            return;
        }
        
        // Search only through patients who have prescriptions (from allHistoryItems)
        if (allHistoryItems == null || allHistoryItems.isEmpty()) {
            updateEmptyState(true);
            return;
        }
        
        // Filter patients with prescriptions by credentials
        List<PatientHistoryItem> filteredItems = new ArrayList<>();
        String queryLower = searchQuery.toLowerCase();
        
        for (PatientHistoryItem historyItem : allHistoryItems) {
            Patient patient = historyItem.getPatient();
            
            // If patient is null, still include it if search is empty or matches patient ID from prescription
            if (patient == null) {
                // For null patients, try to match by prescription data if available
                boolean matchesNull = searchQuery.isEmpty(); // Always show if search is empty
                if (!matchesNull && historyItem.getLastMedication() != null) {
                    matchesNull = historyItem.getLastMedication().toLowerCase().contains(queryLower);
                }
                if (matchesNull) {
                    filteredItems.add(historyItem);
                }
                continue;
            }
            
            // Search in multiple fields
            boolean matches = false;
            
            // Search by full name
            if (patient.getFullName() != null && patient.getFullName().toLowerCase().contains(queryLower)) {
                matches = true;
            }
            
            // Search by first name
            if (!matches && patient.getFirstName() != null && patient.getFirstName().toLowerCase().contains(queryLower)) {
                matches = true;
            }
            
            // Search by last name
            if (!matches && patient.getLastName() != null && patient.getLastName().toLowerCase().contains(queryLower)) {
                matches = true;
            }
            
            // Search by patient ID
            if (!matches && patient.getPatientId() != null && patient.getPatientId().toLowerCase().contains(queryLower)) {
                matches = true;
            }
            
            // Search by phone number
            if (!matches && patient.getPhoneNumber() != null && patient.getPhoneNumber().contains(searchQuery)) {
                matches = true;
            }
            
            // Search by email
            if (!matches && patient.getEmail() != null && patient.getEmail().toLowerCase().contains(queryLower)) {
                matches = true;
            }
            
            if (matches) {
                filteredItems.add(historyItem);
            }
        }
        
        // Update adapter with filtered results
        if (patientHistoryAdapter != null) {
            patientHistoryAdapter.setPatientHistoryItems(filteredItems);
        }
        updateEmptyState(filteredItems.isEmpty());
    }
    
    /**
     * Convert list of Patient objects to PatientHistoryItem objects
     * This includes prescription information if available
     * Uses cached prescriptionsByPatientId map for efficiency
     */
    private List<PatientHistoryItem> convertPatientsToHistoryItems(List<Patient> patients) {
        List<PatientHistoryItem> historyItems = new ArrayList<>();
        
        // Use cached prescriptions map (loaded in loadPatientHistory)
        if (prescriptionsByPatientId == null) {
            prescriptionsByPatientId = new HashMap<>();
        }
        
        // Convert each patient to PatientHistoryItem
        for (Patient patient : patients) {
            PatientHistoryItem historyItem = new PatientHistoryItem();
            historyItem.setPatient(patient);
            
            // Get prescription info for this patient from cached map
            List<Prescription> patientPrescriptions = prescriptionsByPatientId.get(patient.getPatientId());
            if (patientPrescriptions != null && !patientPrescriptions.isEmpty()) {
                // Sort prescriptions by date (most recent first)
                patientPrescriptions.sort((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()));
                
                Prescription latestPrescription = patientPrescriptions.get(0);
                historyItem.setPrescriptionCount(patientPrescriptions.size());
                historyItem.setLastPrescriptionDate(latestPrescription.getCreatedDate());
                
                // Collect all unique medications from all prescriptions
                List<String> allMedications = new ArrayList<>();
                for (Prescription p : patientPrescriptions) {
                    if (p.getMedication() != null && !p.getMedication().isEmpty() && !allMedications.contains(p.getMedication())) {
                        allMedications.add(p.getMedication());
                    }
                }
                // Join all medications with comma
                String medicationsText = allMedications.isEmpty() ? "N/A" : String.join(", ", allMedications);
                historyItem.setLastMedication(medicationsText);
                
                historyItem.setLastDoctor(latestPrescription.getDoctorName());
            } else {
                // Patient has no prescriptions
                historyItem.setPrescriptionCount(0);
                historyItem.setLastPrescriptionDate("No prescriptions");
                historyItem.setLastMedication("N/A");
                historyItem.setLastDoctor("N/A");
            }
            
            historyItems.add(historyItem);
        }
        
        return historyItems;
    }
    
    private void updateEmptyState(boolean isEmpty) {
        if (emptyStateTextView != null && patientHistoryRecyclerView != null) {
            if (isEmpty) {
                emptyStateTextView.setVisibility(View.VISIBLE);
                patientHistoryRecyclerView.setVisibility(View.GONE);
                if (searchPatientInput != null && searchPatientInput.getText() != null && !searchPatientInput.getText().toString().trim().isEmpty()) {
                    emptyStateTextView.setText("No patients found matching your search");
                } else {
                    emptyStateTextView.setText("No patients with prescription history found");
                }
            } else {
                emptyStateTextView.setVisibility(View.GONE);
                patientHistoryRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
        firebaseRTDBHelper = new FirebaseRTDBHelper(getContext());
    }

    private void setupRecyclerView() {
        patientHistoryAdapter = new PatientHistoryAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        patientHistoryRecyclerView.setLayoutManager(layoutManager);
        patientHistoryRecyclerView.setAdapter(patientHistoryAdapter);
        
        // Ensure RecyclerView is properly configured
        patientHistoryRecyclerView.setHasFixedSize(false);
        patientHistoryRecyclerView.setNestedScrollingEnabled(false);
        
        android.util.Log.d("PatientHistory", "✅ RecyclerView setup complete");
    }

    private void loadPatientHistory() {
        // Prevent duplicate loading
        if (isLoading) {
            android.util.Log.d("PatientHistory", "⚠️ Already loading, skipping duplicate call");
            return;
        }
        
        isLoading = true;
        android.util.Log.d("PatientHistory", "🔄 Starting to load patient history...");
        
        // Load prescriptions from Firebase RTDB (more accurate and real-time)
        if (firebaseRTDBHelper != null) {
            // Load prescriptions directly from history - don't wait for patients
            // Since patients are removed from Firebase after prescription creation,
            // we'll create patient objects from prescription data
            android.util.Log.d("PatientHistory", "📥 Loading prescriptions from history...");
            firebaseRTDBHelper.getAllPrescriptionsFromHistory(prescriptions -> {
                android.util.Log.d("PatientHistory", "📥 Received " + (prescriptions != null ? prescriptions.size() : 0) + " prescriptions from history");
                
                // Create final copies for lambda expressions
                final List<Prescription> finalPrescriptions = prescriptions != null ? prescriptions : new ArrayList<>();
                
                // Process prescriptions on background thread
                com.healthcare.cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                    prescriptionsByPatientId = new HashMap<>();
                    
                    // Group prescriptions by patient ID
                    for (Prescription prescription : finalPrescriptions) {
                        String patientId = prescription.getPatientId();
                        if (patientId != null && !patientId.isEmpty()) {
                            if (!prescriptionsByPatientId.containsKey(patientId)) {
                                prescriptionsByPatientId.put(patientId, new ArrayList<>());
                            }
                            prescriptionsByPatientId.get(patientId).add(prescription);
                        }
                    }
                    
                    android.util.Log.d("PatientHistory", "📥 Grouped prescriptions by patient ID. Total patients: " + prescriptionsByPatientId.size());
                    
                    // Load patients with patient_status = "off" from Firebase (existing patients)
                    android.util.Log.d("PatientHistory", "📥 Loading patients with patient_status = 'off' from Firebase...");
                    // Call getAllPatients with callback first, then filter status "off" as second parameter
                    firebaseRTDBHelper.getAllPatients(firebasePatients -> {
                        android.util.Log.d("PatientHistory", "📥 Loaded " + (firebasePatients != null ? firebasePatients.size() : 0) + " patients with status 'off' from Firebase");
                        
                        // Create patients map from Firebase patients
                        Map<String, Patient> patientsMap = new HashMap<>();
                        if (firebasePatients != null) {
                            for (Patient patient : firebasePatients) {
                                if (patient != null && patient.getPatientId() != null) {
                                    patientsMap.put(patient.getPatientId(), patient);
                                    android.util.Log.d("PatientHistory", "✅ Added patient from Firebase: " + patient.getPatientId());
                                }
                            }
                        }
                        
                        // Only use patients from Firebase - don't load from SQLite
                        // This ensures we only show patients that exist in Firebase
                        android.util.Log.d("PatientHistory", "📥 Using only Firebase patients (no SQLite fallback)");
                        
                        // Process prescriptions to history items with patients map
                        List<PatientHistoryItem> historyItems = processPrescriptionsToHistoryItems(finalPrescriptions, patientsMap);
                        
                        android.util.Log.d("PatientHistory", "📥 Processed " + historyItems.size() + " history items");
                        
                        // Store all items for filtering (these are only patients with prescriptions)
                        allHistoryItems = historyItems;
                        
                        // Update UI on main thread
                        com.healthcare.cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                        if (getContext() == null || getView() == null) {
                            android.util.Log.w("PatientHistory", "⚠️ Fragment is detached, cannot update UI");
                            return; // Fragment is detached
                        }
                        
                        android.util.Log.d("PatientHistory", "📥 Updating UI with " + historyItems.size() + " items");
                        updateEmptyState(historyItems.isEmpty());
                        
                        if (patientHistoryAdapter != null) {
                            // Temporarily remove text watcher to prevent filter trigger
                            if (searchPatientInput != null && searchTextWatcher != null) {
                                searchPatientInput.removeTextChangedListener(searchTextWatcher);
                            }
                            
                            // Set items directly
                            patientHistoryAdapter.setPatientHistoryItems(historyItems);
                            
                            // Clear search input and re-add text watcher
                            if (searchPatientInput != null) {
                                searchPatientInput.setText("");
                                if (searchTextWatcher != null) {
                                    searchPatientInput.addTextChangedListener(searchTextWatcher);
                                }
                            }
                            android.util.Log.d("PatientHistory", "✅ UI updated successfully with " + historyItems.size() + " items");
                            android.util.Log.d("PatientHistory", "📊 Adapter item count: " + patientHistoryAdapter.getItemCount());
                            
                            // Force refresh the RecyclerView to ensure all items are displayed
                            patientHistoryRecyclerView.post(() -> {
                                // Request layout to ensure proper measurement
                                patientHistoryRecyclerView.requestLayout();
                                patientHistoryAdapter.notifyDataSetChanged();
                                android.util.Log.d("PatientHistory", "🔄 Forced RecyclerView refresh, item count: " + patientHistoryAdapter.getItemCount());
                                
                                // Log each item being displayed
                                for (int i = 0; i < patientHistoryAdapter.getItemCount(); i++) {
                                    android.util.Log.d("PatientHistory", "📋 RecyclerView item " + i + " should be visible");
                                }
                                
                                // Force invalidate to ensure redraw
                                patientHistoryRecyclerView.invalidate();
                                
                                // Post another runnable to ensure layout happens after measurement
                                patientHistoryRecyclerView.post(() -> {
                                    android.util.Log.d("PatientHistory", "🔄 Second post - RecyclerView height: " + patientHistoryRecyclerView.getHeight() + ", measured height: " + patientHistoryRecyclerView.getMeasuredHeight());
                                    android.util.Log.d("PatientHistory", "🔄 RecyclerView child count: " + patientHistoryRecyclerView.getChildCount());
                                    
                                    // Log each visible child
                                    for (int i = 0; i < patientHistoryRecyclerView.getChildCount(); i++) {
                                        View child = patientHistoryRecyclerView.getChildAt(i);
                                        android.util.Log.d("PatientHistory", "👁️ Visible child " + i + ": " + (child != null ? "exists" : "null") + ", visibility: " + (child != null ? child.getVisibility() : "N/A") + ", height: " + (child != null ? child.getHeight() : "N/A"));
                                    }
                                    
                                    // Force another layout pass to ensure all items are measured
                                    patientHistoryRecyclerView.requestLayout();
                                    
                                    // Force another layout pass
                                    patientHistoryRecyclerView.requestLayout();
                                });
                            });
                        } else {
                            android.util.Log.e("PatientHistory", "❌ Adapter is null!");
                        }
                        
                        isLoading = false; // Reset loading flag
                        });
                    }, "off"); // Filter by patient_status = "off"
                });
            });
        } else {
            // Fallback to SQLite if Firebase not available
            com.healthcare.cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                List<Prescription> prescriptions = databaseHelper.getAllPrescriptions();
                prescriptionsByPatientId = new HashMap<>();
                
                // Group prescriptions by patient ID
                for (Prescription prescription : prescriptions) {
                    String patientId = prescription.getPatientId();
                    if (!prescriptionsByPatientId.containsKey(patientId)) {
                        prescriptionsByPatientId.put(patientId, new ArrayList<>());
                    }
                    prescriptionsByPatientId.get(patientId).add(prescription);
                }
                
                // Load ONLY patients with prescription history
                List<PatientHistoryItem> historyItems = processPrescriptionsToHistoryItems(prescriptions);
                allHistoryItems = historyItems;
                
                // Update UI on main thread
                com.healthcare.cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                    if (getContext() == null || getView() == null) {
                        return; // Fragment is detached
                    }
                    
                    updateEmptyState(historyItems.isEmpty());
                    
                    if (patientHistoryAdapter != null) {
                        patientHistoryAdapter.setPatientHistoryItems(historyItems);
                    }
                });
            });
        }
    }
    
    private List<PatientHistoryItem> processPrescriptionsToHistoryItems(List<Prescription> prescriptions, Map<String, Patient> patientsMap) {
        List<PatientHistoryItem> historyItems = new ArrayList<>();
        
        android.util.Log.d("PatientHistory", "📥 Processing " + prescriptions.size() + " prescriptions to history items");
        android.util.Log.d("PatientHistory", "📥 Patients map size: " + (patientsMap != null ? patientsMap.size() : 0));
        
        // Create a map to track unique patients and their prescription counts
        Map<String, PatientHistoryItem> patientMap = new HashMap<>();
        
        for (Prescription prescription : prescriptions) {
            String patientId = prescription.getPatientId();
            
            // Handle prescriptions without patient ID - use fallback
            if (patientId == null || patientId.isEmpty()) {
                // Try to extract patient ID from prescription ID or use fallback
                String prescriptionId = prescription.getPrescriptionId();
                if (prescriptionId != null && prescriptionId.contains("_")) {
                    // Try to extract patient ID from prescription ID format
                    String[] parts = prescriptionId.split("_");
                    if (parts.length > 0) {
                        // Look for PAT prefix in parts
                        for (String part : parts) {
                            if (part.startsWith("PAT")) {
                                patientId = part;
                                prescription.setPatientId(patientId);
                                android.util.Log.d("PatientHistory", "📥 Extracted patient ID from prescription ID: " + patientId);
                                break;
                            }
                        }
                    }
                }
                
                // If still no patient ID, use fallback
                if (patientId == null || patientId.isEmpty()) {
                    patientId = "UNKNOWN_" + (prescription.getPrescriptionId() != null ? prescription.getPrescriptionId() : "UNKNOWN");
                    prescription.setPatientId(patientId);
                    android.util.Log.w("PatientHistory", "⚠️ Prescription has no patient ID, using fallback: " + patientId);
                }
            }
            
            android.util.Log.d("PatientHistory", "📥 Processing prescription: " + prescription.getPrescriptionId() + " for patient: " + patientId);
            
            if (!patientMap.containsKey(patientId)) {
                // Only get patient from Firebase map - don't use SQLite or create from prescription
                // This ensures we only show patients that exist in Firebase
                Patient patient = null;
                if (patientsMap != null && patientsMap.containsKey(patientId)) {
                    patient = patientsMap.get(patientId);
                    android.util.Log.d("PatientHistory", "✅ Found patient in Firebase map: " + patientId);
                } else {
                    android.util.Log.d("PatientHistory", "⚠️ Patient not found in Firebase: " + patientId + " - skipping (only show patients from Firebase)");
                    continue; // Skip this prescription - patient doesn't exist in Firebase
                }
                
                // Ensure patient has a valid fullName before creating history item
                if (patient != null && (patient.getFullName() == null || patient.getFullName().isEmpty())) {
                    String firstName = patient.getFirstName() != null ? patient.getFirstName() : "";
                    String lastName = patient.getLastName() != null ? patient.getLastName() : "";
                    String fullName = (firstName + " " + lastName).trim();
                    if (fullName.isEmpty()) {
                        fullName = "Patient " + patientId;
                    }
                    patient.setFullName(fullName);
                    android.util.Log.d("PatientHistory", "📥 Set fallback fullName: " + fullName);
                }
                
                // Create history item - collect all medications from prescriptions
                if (patient != null) {
                    PatientHistoryItem historyItem = new PatientHistoryItem();
                    historyItem.setPatient(patient);
                    historyItem.setPrescriptionCount(1);
                    historyItem.setLastPrescriptionDate(prescription.getCreatedDate() != null ? prescription.getCreatedDate() : "");
                    
                    // Collect all medications from all prescriptions for this patient
                    List<String> allMedications = new ArrayList<>();
                    if (prescription.getMedication() != null && !prescription.getMedication().isEmpty()) {
                        allMedications.add(prescription.getMedication());
                    }
                    // Get all prescriptions for this patient to collect all medications
                    List<Prescription> patientPrescriptions = prescriptionsByPatientId.get(patientId);
                    if (patientPrescriptions != null) {
                        for (Prescription p : patientPrescriptions) {
                            if (p.getMedication() != null && !p.getMedication().isEmpty() && !allMedications.contains(p.getMedication())) {
                                allMedications.add(p.getMedication());
                            }
                        }
                    }
                    // Join all medications with comma
                    String medicationsText = allMedications.isEmpty() ? "N/A" : String.join(", ", allMedications);
                    historyItem.setLastMedication(medicationsText);
                    
                    historyItem.setLastDoctor(prescription.getDoctorName() != null ? prescription.getDoctorName() : "");
                    
                    patientMap.put(patientId, historyItem);
                    android.util.Log.d("PatientHistory", "✅ Created history item for patient: " + patientId + " with medications: " + medicationsText);
                }
            } else {
                // Update existing patient's prescription count and collect all medications
                PatientHistoryItem existingItem = patientMap.get(patientId);
                existingItem.setPrescriptionCount(existingItem.getPrescriptionCount() + 1);
                android.util.Log.d("PatientHistory", "📥 Updated existing history item for patient: " + patientId + ", count now: " + existingItem.getPrescriptionCount());
                
                // Update to most recent prescription date
                if (prescription.getCreatedDate() != null) {
                    existingItem.setLastPrescriptionDate(prescription.getCreatedDate());
                }
                
                // Collect all medications from all prescriptions for this patient
                List<String> allMedications = new ArrayList<>();
                List<Prescription> patientPrescriptions = prescriptionsByPatientId.get(patientId);
                if (patientPrescriptions != null) {
                    for (Prescription p : patientPrescriptions) {
                        if (p.getMedication() != null && !p.getMedication().isEmpty() && !allMedications.contains(p.getMedication())) {
                            allMedications.add(p.getMedication());
                        }
                    }
                }
                // Join all medications with comma
                String medicationsText = allMedications.isEmpty() ? "N/A" : String.join(", ", allMedications);
                existingItem.setLastMedication(medicationsText);
                android.util.Log.d("PatientHistory", "📥 Updated medications for patient: " + patientId + " - " + medicationsText);
                
                if (prescription.getDoctorName() != null) {
                    existingItem.setLastDoctor(prescription.getDoctorName());
                }
            }
        }
        
        android.util.Log.d("PatientHistory", "📥 Patient map size after processing: " + patientMap.size());
        
        // Convert map to list
        historyItems.addAll(patientMap.values());
        
        android.util.Log.d("PatientHistory", "📥 History items list size: " + historyItems.size());
        
        // Sort by last prescription date (most recent first)
        historyItems.sort((a, b) -> {
            String dateA = a.getLastPrescriptionDate() != null ? a.getLastPrescriptionDate() : "";
            String dateB = b.getLastPrescriptionDate() != null ? b.getLastPrescriptionDate() : "";
            return dateB.compareTo(dateA);
        });
        
        android.util.Log.d("PatientHistory", "📥 Final history items count: " + historyItems.size());
        for (int i = 0; i < historyItems.size(); i++) {
            PatientHistoryItem item = historyItems.get(i);
            Patient p = item.getPatient();
            android.util.Log.d("PatientHistory", "📥 History item " + i + ": Patient=" + (p != null ? p.getFullName() : "null") + ", Prescriptions=" + item.getPrescriptionCount());
        }
        
        return historyItems;
    }
    
    // Overloaded method for SQLite fallback
    private List<PatientHistoryItem> processPrescriptionsToHistoryItems(List<Prescription> prescriptions) {
        return processPrescriptionsToHistoryItems(prescriptions, null);
    }
    
    /**
     * Create a minimal Patient object from prescription data
     * Used when patient is not found in Firebase or SQLite
     */
    private Patient createPatientFromPrescription(Prescription prescription) {
        Patient patient = new Patient();
        patient.setPatientId(prescription.getPatientId());
        
        // Parse patient name from prescription (format: "FirstName LastName")
        String patientName = prescription.getPatientName();
        android.util.Log.d("PatientHistory", "📥 Creating patient from prescription. Patient ID: " + prescription.getPatientId() + ", Name: " + patientName);
        
        if (patientName != null && !patientName.isEmpty()) {
            String[] nameParts = patientName.trim().split("\\s+", 2);
            if (nameParts.length >= 1) {
                patient.setFirstName(nameParts[0]);
            }
            if (nameParts.length >= 2) {
                patient.setLastName(nameParts[1]);
            }
            patient.setFullName(patientName);
            android.util.Log.d("PatientHistory", "✅ Created patient with name: " + patientName);
        } else {
            // Try to get from patient ID if name is missing
            String patientId = prescription.getPatientId();
            if (patientId != null && patientId.startsWith("PAT")) {
                patient.setFirstName("Patient");
                patient.setLastName(patientId.substring(3)); // Remove "PAT" prefix
                patient.setFullName("Patient " + patientId.substring(3));
            } else {
                patient.setFirstName("Unknown");
                patient.setLastName("Patient");
                patient.setFullName("Unknown Patient");
            }
            android.util.Log.w("PatientHistory", "⚠️ Patient name is null/empty, using fallback: " + patient.getFullName());
        }
        
        return patient;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Only reload if we don't have data yet or if fragment was recreated
        if (allHistoryItems == null || allHistoryItems.isEmpty()) {
            android.util.Log.d("PatientHistory", "🔄 onResume: Reloading history (no data)");
            loadPatientHistory();
        } else {
            android.util.Log.d("PatientHistory", "✅ onResume: Already have " + allHistoryItems.size() + " items, skipping reload");
        }
    }

    // RecyclerView Adapter for patient history
    private class PatientHistoryAdapter extends RecyclerView.Adapter<PatientHistoryAdapter.PatientHistoryViewHolder> {
        private List<PatientHistoryItem> patientHistoryItems;
        
        public PatientHistoryAdapter() {
            this.patientHistoryItems = new ArrayList<>();
        }

        public void setPatientHistoryItems(List<PatientHistoryItem> patientHistoryItems) {
            android.util.Log.d("PatientHistory", "📊 Adapter: Setting " + (patientHistoryItems != null ? patientHistoryItems.size() : 0) + " items");
            this.patientHistoryItems = patientHistoryItems;
            if (patientHistoryItems != null) {
                for (int i = 0; i < patientHistoryItems.size(); i++) {
                    PatientHistoryItem item = patientHistoryItems.get(i);
                    Patient p = item != null ? item.getPatient() : null;
                    android.util.Log.d("PatientHistory", "📊 Adapter item " + i + ": " + (p != null ? p.getFullName() : "null") + " (" + item.getPrescriptionCount() + " prescriptions)");
                }
            }
            notifyDataSetChanged();
            android.util.Log.d("PatientHistory", "📊 Adapter: After notifyDataSetChanged, getItemCount() = " + getItemCount());
        }

        @NonNull
        @Override
        public PatientHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_history, parent, false);
            return new PatientHistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PatientHistoryViewHolder holder, int position) {
            if (patientHistoryItems == null || position < 0 || position >= patientHistoryItems.size()) {
                android.util.Log.e("PatientHistory", "❌ Adapter: Invalid position " + position + " for list size " + (patientHistoryItems != null ? patientHistoryItems.size() : 0));
                return;
            }
            PatientHistoryItem historyItem = patientHistoryItems.get(position);
            Patient p = historyItem != null ? historyItem.getPatient() : null;
            android.util.Log.d("PatientHistory", "📊 Adapter: Binding position " + position + " - " + (p != null ? p.getFullName() : "null"));
            
            // Ensure the view is visible and properly sized
            holder.itemView.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(1.0f);
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            if (params != null) {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                holder.itemView.setLayoutParams(params);
            }
            
            holder.bind(historyItem);
            
            // Log after binding to confirm
            android.util.Log.d("PatientHistory", "✅ Adapter: Bound position " + position + " successfully, view visibility: " + holder.itemView.getVisibility() + ", height: " + holder.itemView.getHeight());
        }

        @Override
        public int getItemCount() {
            return patientHistoryItems != null ? patientHistoryItems.size() : 0;
        }

        class PatientHistoryViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView cardView;
            private TextView patientIdText;
            private TextView patientNameText;
            private TextView prescriptionCountText;
            private TextView lastPrescriptionText;
            private TextView lastMedicationText;
            private TextView lastDoctorText;

            public PatientHistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.patientHistoryCardView);
                patientIdText = itemView.findViewById(R.id.patientIdText);
                patientNameText = itemView.findViewById(R.id.patientNameText);
                prescriptionCountText = itemView.findViewById(R.id.prescriptionCountText);
                lastPrescriptionText = itemView.findViewById(R.id.lastPrescriptionText);
                lastMedicationText = itemView.findViewById(R.id.lastMedicationText);
                lastDoctorText = itemView.findViewById(R.id.lastDoctorText);
            }

            public void bind(PatientHistoryItem historyItem) {
                Patient patient = historyItem.getPatient();
                
                if (patient == null) {
                    android.util.Log.e("PatientHistory", "❌ Patient is null in bind()!");
                    patientIdText.setText("Patient ID: Unknown");
                    patientNameText.setText("Unknown Patient");
                    prescriptionCountText.setText("Prescriptions: " + historyItem.getPrescriptionCount());
                    lastPrescriptionText.setText("Last Rx: " + (historyItem.getLastPrescriptionDate() != null ? historyItem.getLastPrescriptionDate() : "N/A"));
                    lastMedicationText.setText("Last Medication: " + (historyItem.getLastMedication() != null ? historyItem.getLastMedication() : "N/A"));
                    lastDoctorText.setText("Last Doctor: " + (historyItem.getLastDoctor() != null ? historyItem.getLastDoctor() : "N/A"));
                    return;
                }
                
                // Ensure fullName is not null
                String fullName = patient.getFullName();
                if (fullName == null || fullName.isEmpty()) {
                    String firstName = patient.getFirstName() != null ? patient.getFirstName() : "";
                    String lastName = patient.getLastName() != null ? patient.getLastName() : "";
                    fullName = (firstName + " " + lastName).trim();
                    if (fullName.isEmpty()) {
                        fullName = "Patient " + (patient.getPatientId() != null ? patient.getPatientId() : "Unknown");
                    }
                    patient.setFullName(fullName);
                }
                
                patientIdText.setText("Patient ID: " + (patient.getPatientId() != null ? patient.getPatientId() : "Unknown"));
                patientNameText.setText(fullName);
                prescriptionCountText.setText("Prescriptions: " + historyItem.getPrescriptionCount());
                lastPrescriptionText.setText("Last Rx: " + (historyItem.getLastPrescriptionDate() != null ? historyItem.getLastPrescriptionDate() : "N/A"));
                lastMedicationText.setText("Last Medication: " + (historyItem.getLastMedication() != null ? historyItem.getLastMedication() : "N/A"));
                lastDoctorText.setText("Last Doctor: " + (historyItem.getLastDoctor() != null ? historyItem.getLastDoctor() : "N/A"));
                
                // Make card clickable to register patient again
                cardView.setOnClickListener(v -> {
                    // Navigate to registration form with existing patient data
                    navigateToRegistration(patient, historyItem);
                });
            }
            
            private void navigateToRegistration(Patient patient, PatientHistoryItem historyItem) {
                // Navigate to PatientRegistrationFragment with patient data
                if (getActivity() instanceof NurseDashboardActivity) {
                    NurseDashboardActivity activity = (NurseDashboardActivity) getActivity();
                    PatientRegistrationFragment registrationFragment = new PatientRegistrationFragment();
                    
                    // Pass patient data as arguments
                    Bundle args = new Bundle();
                    args.putString("PATIENT_ID", patient.getPatientId());
                    args.putString("FIRST_NAME", patient.getFirstName());
                    args.putString("LAST_NAME", patient.getLastName());
                    args.putString("SUFFIX", patient.getSuffix() != null ? patient.getSuffix() : "");
                    args.putString("FULL_NAME", patient.getFullName());
                    args.putString("ADDRESS", patient.getFullAddress());
                    args.putString("DOB", patient.getDateOfBirth());
                    args.putString("BIRTH_PLACE", patient.getBirthPlace() != null ? patient.getBirthPlace() : "");
                    args.putString("GENDER", patient.getGender());
                    args.putString("AGE", patient.getAge());
                    args.putString("PHONE", patient.getPhoneNumber());
                    args.putString("EMAIL", patient.getEmail());
                    args.putString("ALLERGIES", patient.getAllergies() != null ? patient.getAllergies() : "");
                    args.putString("MEDICATIONS", patient.getMedications() != null ? patient.getMedications() : "");
                    args.putString("MEDICAL_HISTORY", patient.getMedicalHistory() != null ? patient.getMedicalHistory() : "");
                    args.putString("EMERGENCY_NAME", patient.getEmergencyContactName() != null ? patient.getEmergencyContactName() : "");
                    args.putString("EMERGENCY_PHONE", patient.getEmergencyContactPhone() != null ? patient.getEmergencyContactPhone() : "");
                    args.putInt("PRESCRIPTION_COUNT", historyItem.getPrescriptionCount());
                    args.putString("LAST_PRESCRIPTION_DATE", historyItem.getLastPrescriptionDate());
                    args.putString("LAST_MEDICATION", historyItem.getLastMedication());
                    args.putString("LAST_DOCTOR", historyItem.getLastDoctor());
                    args.putBoolean("IS_EXISTING_PATIENT", true);
                    
                    registrationFragment.setArguments(args);
                    
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, registrationFragment)
                            .addToBackStack(null)
                            .commit();
                    // Update toolbar title
                    activity.getSupportActionBar().setTitle("Register Existing Patient");
                }
            }
            
            private void showPatientHistoryDialog(Patient patient, PatientHistoryItem historyItem) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Patient History - " + patient.getFullName());
                
                // Inflate custom dialog layout
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_patient_history, null);
                builder.setView(dialogView);
                
                // Set patient information
                TextView dialogPatientId = dialogView.findViewById(R.id.dialogPatientId);
                TextView dialogPatientName = dialogView.findViewById(R.id.dialogPatientName);
                TextView dialogPatientAge = dialogView.findViewById(R.id.dialogPatientAge);
                TextView dialogPatientGender = dialogView.findViewById(R.id.dialogPatientGender);
                TextView dialogPatientPhone = dialogView.findViewById(R.id.dialogPatientPhone);
                TextView dialogPatientEmail = dialogView.findViewById(R.id.dialogPatientEmail);
                TextView dialogPrescriptionCount = dialogView.findViewById(R.id.dialogPrescriptionCount);
                TextView dialogLastPrescription = dialogView.findViewById(R.id.dialogLastPrescription);
                
                // Populate patient data
                dialogPatientId.setText("Patient ID: " + patient.getPatientId());
                dialogPatientName.setText(patient.getFullName());
                dialogPatientAge.setText("Age: " + (patient.getAge() != null ? patient.getAge() : "N/A"));
                dialogPatientGender.setText("Gender: " + (patient.getGender() != null ? patient.getGender() : "N/A"));
                dialogPatientPhone.setText("Phone: " + (patient.getPhone() != null ? patient.getPhone() : "N/A"));
                dialogPatientEmail.setText("Email: " + (patient.getEmail() != null ? patient.getEmail() : "N/A"));
                dialogPrescriptionCount.setText("Total Prescriptions: " + historyItem.getPrescriptionCount());
                dialogLastPrescription.setText("Last Prescription: " + historyItem.getLastPrescriptionDate());
                
                // Set up prescriptions list
                RecyclerView prescriptionsRecyclerView = dialogView.findViewById(R.id.prescriptionsRecyclerView);
                setupPrescriptionsRecyclerView(prescriptionsRecyclerView, patient.getPatientId());
                
                // Set up close button
                ImageButton closeButton = dialogView.findViewById(R.id.closeHistoryButton);
                
                AlertDialog dialog = builder.create();
                
                closeButton.setOnClickListener(v -> dialog.dismiss());
                
                dialog.show();
            }
            
            private void setupPrescriptionsRecyclerView(RecyclerView recyclerView, String patientId) {
                // Get all prescriptions for this patient
                List<Prescription> patientPrescriptions = getPrescriptionsForPatient(patientId);
                
                PrescriptionHistoryAdapter adapter = new PrescriptionHistoryAdapter(patientPrescriptions);
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                recyclerView.setAdapter(adapter);
            }
            
            private List<Prescription> getPrescriptionsForPatient(String patientId) {
                List<Prescription> allPrescriptions = databaseHelper.getAllPrescriptions();
                List<Prescription> patientPrescriptions = new ArrayList<>();
                
                for (Prescription prescription : allPrescriptions) {
                    if (prescription.getPatientId().equals(patientId)) {
                        patientPrescriptions.add(prescription);
                    }
                }
                
                // Sort by date (most recent first)
                patientPrescriptions.sort((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()));
                
                return patientPrescriptions;
            }
            
            // Adapter for prescription history within the dialog
            private class PrescriptionHistoryAdapter extends RecyclerView.Adapter<PrescriptionHistoryAdapter.PrescriptionViewHolder> {
                private List<Prescription> prescriptions;
                
                public PrescriptionHistoryAdapter(List<Prescription> prescriptions) {
                    this.prescriptions = prescriptions;
                }
                
                @NonNull
                @Override
                public PrescriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prescription_history, parent, false);
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
                    private TextView prescriptionIdText;
                    private TextView medicationText;
                    private TextView frequencyText;
                    private TextView durationText;
                    private TextView doctorText;
                    private TextView dateText;
                    private TextView instructionsText;
                    
                    public PrescriptionViewHolder(@NonNull View itemView) {
                        super(itemView);
                        prescriptionIdText = itemView.findViewById(R.id.prescriptionIdText);
                        medicationText = itemView.findViewById(R.id.medicationText);
                        frequencyText = itemView.findViewById(R.id.frequencyText);
                        durationText = itemView.findViewById(R.id.durationText);
                        doctorText = itemView.findViewById(R.id.doctorText);
                        dateText = itemView.findViewById(R.id.dateText);
                        instructionsText = itemView.findViewById(R.id.instructionsText);
                    }
                    
                    public void bind(Prescription prescription) {
                        prescriptionIdText.setText("Prescription ID: " + prescription.getPrescriptionId());
                        medicationText.setText("Medication: " + prescription.getMedication());
                        frequencyText.setText("Frequency: " + prescription.getFrequency());
                        durationText.setText("Duration: " + prescription.getDuration());
                        doctorText.setText("Doctor: " + prescription.getDoctorName());
                        dateText.setText("Date: " + prescription.getCreatedDate());
                        instructionsText.setText("Instructions: " + (prescription.getInstructions() != null && !prescription.getInstructions().isEmpty() ? prescription.getInstructions() : "None"));
                    }
                }
            }
        }
    }

    // Data class for patient history items
    public static class PatientHistoryItem {
        private Patient patient;
        private int prescriptionCount;
        private String lastPrescriptionDate;
        private String lastMedication;
        private String lastDoctor;

        public Patient getPatient() {
            return patient;
        }

        public void setPatient(Patient patient) {
            this.patient = patient;
        }

        public int getPrescriptionCount() {
            return prescriptionCount;
        }

        public void setPrescriptionCount(int prescriptionCount) {
            this.prescriptionCount = prescriptionCount;
        }

        public String getLastPrescriptionDate() {
            return lastPrescriptionDate;
        }

        public void setLastPrescriptionDate(String lastPrescriptionDate) {
            this.lastPrescriptionDate = lastPrescriptionDate;
        }

        public String getLastMedication() {
            return lastMedication;
        }

        public void setLastMedication(String lastMedication) {
            this.lastMedication = lastMedication;
        }

        public String getLastDoctor() {
            return lastDoctor;
        }

        public void setLastDoctor(String lastDoctor) {
            this.lastDoctor = lastDoctor;
        }
    }
}
