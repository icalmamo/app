package com.example.h_cas;

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

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.database.FirebaseRTDBHelper;
import com.example.h_cas.models.Patient;
import com.example.h_cas.models.Prescription;

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
            // Always enable search functionality
            searchPatientInput.addTextChangedListener(new TextWatcher() {
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
                    filterPatientHistory(s.toString().trim());
                }
            });
            
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
        if (searchQuery.isEmpty()) {
            // Show all prescription history items if search is empty
            if (patientHistoryAdapter != null) {
                patientHistoryAdapter.setPatientHistoryItems(allHistoryItems);
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
            if (patient == null) {
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
                historyItem.setLastMedication(latestPrescription.getMedication());
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
        patientHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        patientHistoryRecyclerView.setAdapter(patientHistoryAdapter);
    }

    private void loadPatientHistory() {
        // Load prescriptions from Firebase RTDB (more accurate and real-time)
        if (firebaseRTDBHelper != null) {
            // First load all patients from Firebase, then load prescriptions
            firebaseRTDBHelper.getAllPatients(patients -> {
                // Create a map of patients by ID for quick lookup
                Map<String, Patient> patientsMap = new HashMap<>();
                for (Patient patient : patients) {
                    if (patient != null && patient.getPatientId() != null) {
                        patientsMap.put(patient.getPatientId(), patient);
                    }
                }
                
                // Now load prescriptions from Firebase
                firebaseRTDBHelper.getAllPrescriptions(prescriptions -> {
                    // Process prescriptions on background thread
                    com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                        prescriptionsByPatientId = new HashMap<>();
                        
                        // Group prescriptions by patient ID
                        for (Prescription prescription : prescriptions) {
                            String patientId = prescription.getPatientId();
                            if (patientId != null && !patientId.isEmpty()) {
                                if (!prescriptionsByPatientId.containsKey(patientId)) {
                                    prescriptionsByPatientId.put(patientId, new ArrayList<>());
                                }
                                prescriptionsByPatientId.get(patientId).add(prescription);
                            }
                        }
                        
                        // Load ONLY patients with prescription history from Firebase
                        List<PatientHistoryItem> historyItems = processPrescriptionsToHistoryItems(prescriptions, patientsMap);
                        
                        // Store all items for filtering (these are only patients with prescriptions)
                        allHistoryItems = historyItems;
                        
                        // Update UI on main thread
                        com.example.h_cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                            if (getContext() == null || getView() == null) {
                                return; // Fragment is detached
                            }
                            
                            updateEmptyState(historyItems.isEmpty());
                            
                            if (patientHistoryAdapter != null) {
                                patientHistoryAdapter.setPatientHistoryItems(historyItems);
                            }
                        });
                    });
                });
            });
        } else {
            // Fallback to SQLite if Firebase not available
            com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
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
                com.example.h_cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
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
        
        // Create a map to track unique patients and their prescription counts
        Map<String, PatientHistoryItem> patientMap = new HashMap<>();
        
        for (Prescription prescription : prescriptions) {
            String patientId = prescription.getPatientId();
            if (patientId == null || patientId.isEmpty()) {
                continue;
            }
            
            if (!patientMap.containsKey(patientId)) {
                // Get patient details from Firebase map (already loaded from Firebase RTDB)
                Patient patient = null;
                if (patientsMap != null && patientsMap.containsKey(patientId)) {
                    patient = patientsMap.get(patientId);
                }
                
                if (patient != null) {
                    PatientHistoryItem historyItem = new PatientHistoryItem();
                    historyItem.setPatient(patient);
                    historyItem.setPrescriptionCount(1);
                    historyItem.setLastPrescriptionDate(prescription.getCreatedDate() != null ? prescription.getCreatedDate() : "");
                    historyItem.setLastMedication(prescription.getMedication() != null ? prescription.getMedication() : "");
                    historyItem.setLastDoctor(prescription.getDoctorName() != null ? prescription.getDoctorName() : "");
                    
                    patientMap.put(patientId, historyItem);
                }
            } else {
                // Update existing patient's prescription count and last prescription info
                PatientHistoryItem existingItem = patientMap.get(patientId);
                existingItem.setPrescriptionCount(existingItem.getPrescriptionCount() + 1);
                // Update to most recent prescription
                if (prescription.getCreatedDate() != null) {
                    existingItem.setLastPrescriptionDate(prescription.getCreatedDate());
                }
                if (prescription.getMedication() != null) {
                    existingItem.setLastMedication(prescription.getMedication());
                }
                if (prescription.getDoctorName() != null) {
                    existingItem.setLastDoctor(prescription.getDoctorName());
                }
            }
        }
        
        // Convert map to list
        historyItems.addAll(patientMap.values());
        
        // Sort by last prescription date (most recent first)
        historyItems.sort((a, b) -> {
            String dateA = a.getLastPrescriptionDate() != null ? a.getLastPrescriptionDate() : "";
            String dateB = b.getLastPrescriptionDate() != null ? b.getLastPrescriptionDate() : "";
            return dateB.compareTo(dateA);
        });
        
        return historyItems;
    }
    
    // Overloaded method for SQLite fallback
    private List<PatientHistoryItem> processPrescriptionsToHistoryItems(List<Prescription> prescriptions) {
        return processPrescriptionsToHistoryItems(prescriptions, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPatientHistory(); // Refresh when returning to this screen
    }

    // RecyclerView Adapter for patient history
    private class PatientHistoryAdapter extends RecyclerView.Adapter<PatientHistoryAdapter.PatientHistoryViewHolder> {
        private List<PatientHistoryItem> patientHistoryItems;

        public void setPatientHistoryItems(List<PatientHistoryItem> patientHistoryItems) {
            this.patientHistoryItems = patientHistoryItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PatientHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_history, parent, false);
            return new PatientHistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PatientHistoryViewHolder holder, int position) {
            PatientHistoryItem historyItem = patientHistoryItems.get(position);
            holder.bind(historyItem);
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
                
                patientIdText.setText("Patient ID: " + patient.getPatientId());
                patientNameText.setText(patient.getFullName());
                prescriptionCountText.setText("Prescriptions: " + historyItem.getPrescriptionCount());
                lastPrescriptionText.setText("Last Rx: " + historyItem.getLastPrescriptionDate());
                lastMedicationText.setText("Last Medication: " + historyItem.getLastMedication());
                lastDoctorText.setText("Last Doctor: " + historyItem.getLastDoctor());
                
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
                    args.putString("BIRTH_PLACE", patient.getBirthPlace());
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
