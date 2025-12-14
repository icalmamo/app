package com.healthcare.cas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.healthcare.cas.models.Patient;
import com.healthcare.cas.database.HCasDatabaseHelper;
import com.healthcare.cas.database.FirebaseRTDBHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * PatientMonitoringFragment handles patient monitoring functionality for nurses.
 * Allows nurses to monitor patient vital signs, status, and care progress.
 */
public class PatientMonitoringFragment extends Fragment {
    
    // UI Components
    private RecyclerView recyclerViewPatients;
    private TextView textViewPatientCount;
    private TextView textViewMonitoringStatus;
    
    // Data
    private List<Patient> patientList;
    private PatientAdapter patientAdapter;
    private HCasDatabaseHelper databaseHelper;
    private FirebaseRTDBHelper firebaseRTDBHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_monitoring, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        android.util.Log.d("PatientMonitoring", "🔄 onViewCreated called");
        
        // Initialize UI components
        initializeViews(view);
        
        // Initialize database
        initializeDatabase();
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Load patients
        loadPatients();
        
        // Initialize patient monitoring functionality
        initializePatientMonitoring();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d("PatientMonitoring", "🔄 onResume called - refreshing patient list");
        // Refresh patient list when fragment becomes visible
        if (patientList != null && patientAdapter != null) {
            loadPatients();
        }
    }
    
    /**
     * Initialize UI components
     */
    private void initializeViews(@NonNull View view) {
        recyclerViewPatients = view.findViewById(R.id.recyclerViewPatients);
        textViewPatientCount = view.findViewById(R.id.textViewPatientCount);
        textViewMonitoringStatus = view.findViewById(R.id.textViewMonitoringStatus);
    }
    
    /**
     * Initialize database helper
     */
    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
        firebaseRTDBHelper = new FirebaseRTDBHelper(getContext());
    }
    
    /**
     * Setup RecyclerView for patient list display
     */
    private void setupRecyclerView() {
        if (recyclerViewPatients == null) {
            android.util.Log.e("PatientMonitoring", "❌ RecyclerView is null!");
            return;
        }
        
        patientList = new ArrayList<>();
        patientAdapter = new PatientAdapter(patientList);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerViewPatients.setLayoutManager(layoutManager);
        recyclerViewPatients.setAdapter(patientAdapter);
        // Performance optimizations
        recyclerViewPatients.setHasFixedSize(false); // Allow RecyclerView to resize when inside ScrollView
        recyclerViewPatients.setItemViewCacheSize(20); // Cache more views for smoother scrolling
        recyclerViewPatients.setNestedScrollingEnabled(true); // Enable nested scrolling for NestedScrollView
        
        android.util.Log.d("PatientMonitoring", "✅ RecyclerView setup complete");
    }
    
    /**
     * Load patients from Firebase RTDB (primary source)
     */
    private void loadPatients() {
        android.util.Log.d("PatientMonitoring", "🔄 Starting to load patients...");
        
        // Show loading state
        if (textViewMonitoringStatus != null) {
            textViewMonitoringStatus.setText("Status: Loading patients...");
        }
        
        // Fetch patients from Firebase RTDB
        if (firebaseRTDBHelper != null) {
            android.util.Log.d("PatientMonitoring", "📊 Fetching from Firebase RTDB...");
            firebaseRTDBHelper.getAllPatients(patients -> {
                android.util.Log.d("PatientMonitoring", "✅ Firebase callback received: " + (patients != null ? patients.size() : 0) + " patients");
                
                // Create final copy of patients list for use in lambda
                List<Patient> finalPatients = patients != null ? new ArrayList<>(patients) : new ArrayList<>();
                
                // Sort patients by created_date (newest first)
                sortPatientsByDate(finalPatients);
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (getContext() == null || getView() == null) {
                            android.util.Log.w("PatientMonitoring", "⚠️ Fragment is detached, skipping UI update");
                            return; // Fragment is detached
                        }
                        
                        android.util.Log.d("PatientMonitoring", "📋 Updating UI with " + finalPatients.size() + " patients (sorted by date)");
                        
                        // Use efficient DiffUtil update instead of notifyDataSetChanged
                        if (patientAdapter != null) {
                            patientAdapter.setPatients(finalPatients);
                        }
                        if (patientList != null) {
                            patientList.clear();
                            patientList.addAll(finalPatients);
                        }
                        
                        updatePatientCount();
                        updateMonitoringStatus();
                        
                        android.util.Log.d("PatientMonitoring", "✅ UI updated successfully");
                    });
                } else {
                    android.util.Log.w("PatientMonitoring", "⚠️ Activity is null, cannot update UI");
                }
            });
        } else {
            android.util.Log.w("PatientMonitoring", "⚠️ FirebaseRTDBHelper is null, falling back to SQLite");
            // Fallback to SQLite if Firebase not available
            com.healthcare.cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                List<Patient> patients = databaseHelper.getAllPatients();
                android.util.Log.d("PatientMonitoring", "📊 SQLite returned: " + (patients != null ? patients.size() : 0) + " patients");
                
                // Create final copy of patients list for use in lambda
                List<Patient> finalPatients = patients != null ? new ArrayList<>(patients) : new ArrayList<>();
                
                // Sort patients by created_date (newest first)
                sortPatientsByDate(finalPatients);
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (getContext() == null || getView() == null) {
                            return; // Fragment is detached
                        }
                        
                        if (patientAdapter != null) {
                            patientAdapter.setPatients(finalPatients);
                        }
                        if (patientList != null) {
                            patientList.clear();
                            patientList.addAll(finalPatients);
                        }
                        
                        updatePatientCount();
                        updateMonitoringStatus();
                    });
                }
            });
        }
    }
    
    /**
     * Update patient count display
     */
    private void updatePatientCount() {
        int count = patientList.size();
        textViewPatientCount.setText("Total Patients: " + count);
    }
    
    /**
     * Update monitoring status display
     */
    private void updateMonitoringStatus() {
        if (patientList.isEmpty()) {
            textViewMonitoringStatus.setText("Status: No patients in system");
            textViewMonitoringStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        } else {
            textViewMonitoringStatus.setText("Status: Monitoring active - " + patientList.size() + " patients");
            textViewMonitoringStatus.setTextColor(getResources().getColor(R.color.success_green));
        }
    }
    
    /**
     * Sort patients by created_date in descending order (newest first)
     * Falls back to patient ID if created_date is not available
     */
    private void sortPatientsByDate(List<Patient> patients) {
        if (patients == null || patients.isEmpty()) {
            return;
        }
        
        Collections.sort(patients, new Comparator<Patient>() {
            @Override
            public int compare(Patient p1, Patient p2) {
                // Try to compare by created_date first
                String date1 = p1.getCreatedDate();
                String date2 = p2.getCreatedDate();
                
                if (date1 != null && !date1.isEmpty() && date2 != null && !date2.isEmpty()) {
                    try {
                        // Parse date in format "yyyy-MM-dd HH:mm:ss"
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                        Date d1 = sdf.parse(date1);
                        Date d2 = sdf.parse(date2);
                        
                        // Compare in descending order (newest first)
                        return d2.compareTo(d1);
                    } catch (ParseException e) {
                        android.util.Log.w("PatientMonitoring", "⚠️ Error parsing date: " + e.getMessage());
                        // Fall through to patient ID comparison
                    }
                }
                
                // Fallback: compare by patient ID (which contains timestamp)
                // Patient IDs are like "PAT1234567890" where numbers are timestamp
                String id1 = p1.getPatientId();
                String id2 = p2.getPatientId();
                
                if (id1 != null && id2 != null) {
                    // Extract numeric part from patient ID (after "PAT")
                    try {
                        String num1 = id1.replace("PAT", "");
                        String num2 = id2.replace("PAT", "");
                        long timestamp1 = Long.parseLong(num1);
                        long timestamp2 = Long.parseLong(num2);
                        // Compare in descending order (newest first)
                        return Long.compare(timestamp2, timestamp1);
                    } catch (NumberFormatException e) {
                        // If parsing fails, just compare strings
                        return id2.compareTo(id1);
                    }
                }
                
                // Last resort: keep original order
                return 0;
            }
        });
        
        android.util.Log.d("PatientMonitoring", "✅ Sorted " + patients.size() + " patients by date (newest first)");
    }

    /**
     * Initialize patient monitoring components
     */
    private void initializePatientMonitoring() {
        // Refresh the monitoring data periodically
        refreshMonitoringData();
    }
    
    /**
     * Refresh monitoring data
     */
    private void refreshMonitoringData() {
        loadPatients();
    }
    
    /**
     * PatientAdapter for RecyclerView
     * Optimized with DiffUtil for efficient updates
     */
    private class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {
        private List<Patient> patients;
        
        public PatientAdapter(List<Patient> patients) {
            this.patients = patients != null ? new ArrayList<>(patients) : new ArrayList<>();
        }
        
        /**
         * Update patients list efficiently using DiffUtil
         */
        public void setPatients(List<Patient> newPatients) {
            if (newPatients == null) {
                newPatients = new ArrayList<>();
            }
            
            android.util.Log.d("PatientAdapter", "📋 setPatients called with " + newPatients.size() + " patients");
            android.util.Log.d("PatientAdapter", "📋 Current patients count: " + this.patients.size());
            
            // Use DiffUtil for efficient updates (only updates changed items)
            try {
                androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = 
                    androidx.recyclerview.widget.DiffUtil.calculateDiff(new PatientDiffCallback(this.patients, newPatients));
                
                this.patients.clear();
                this.patients.addAll(newPatients);
                diffResult.dispatchUpdatesTo(this);
                
                android.util.Log.d("PatientAdapter", "✅ DiffUtil update completed");
            } catch (Exception e) {
                android.util.Log.e("PatientAdapter", "❌ Error in DiffUtil, using notifyDataSetChanged: " + e.getMessage());
                // Fallback to notifyDataSetChanged if DiffUtil fails
                this.patients.clear();
                this.patients.addAll(newPatients);
                notifyDataSetChanged();
            }
            
            android.util.Log.d("PatientAdapter", "📋 Final patients count: " + this.patients.size());
        }
        
        // DiffUtil callback for efficient RecyclerView updates
        private class PatientDiffCallback extends androidx.recyclerview.widget.DiffUtil.Callback {
            private final List<Patient> oldList;
            private final List<Patient> newList;
            
            public PatientDiffCallback(List<Patient> oldList, List<Patient> newList) {
                this.oldList = oldList;
                this.newList = newList;
            }
            
            @Override
            public int getOldListSize() {
                return oldList.size();
            }
            
            @Override
            public int getNewListSize() {
                return newList.size();
            }
            
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return oldList.get(oldItemPosition).getPatientId().equals(newList.get(newItemPosition).getPatientId());
            }
            
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Patient oldPatient = oldList.get(oldItemPosition);
                Patient newPatient = newList.get(newItemPosition);
                return oldPatient.getPatientId().equals(newPatient.getPatientId()) &&
                       oldPatient.getFirstName().equals(newPatient.getFirstName()) &&
                       oldPatient.getLastName().equals(newPatient.getLastName());
            }
        }
        
        @NonNull
        @Override
        public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_monitoring, parent, false);
            return new PatientViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
            Patient patient = patients.get(position);
            holder.bind(patient);
        }
        
        @Override
        public int getItemCount() {
            int count = patients != null ? patients.size() : 0;
            android.util.Log.d("PatientAdapter", "📊 getItemCount: " + count);
            return count;
        }
        
        class PatientViewHolder extends RecyclerView.ViewHolder {
            private View viewPatientAvatar;
            private TextView textPatientIcon;
            private TextView textPatientNumber;
            private TextView textPatientName;
            private TextView textPatientGender;
            private TextView textPatientStatus;
            
            public PatientViewHolder(@NonNull View itemView) {
                super(itemView);
                viewPatientAvatar = itemView.findViewById(R.id.viewPatientAvatar);
                textPatientIcon = itemView.findViewById(R.id.textPatientIcon);
                textPatientNumber = itemView.findViewById(R.id.textPatientNumber);
                textPatientName = itemView.findViewById(R.id.textPatientName);
                textPatientGender = itemView.findViewById(R.id.textPatientGender);
                textPatientStatus = itemView.findViewById(R.id.textPatientStatus);
            }
            
            public void bind(Patient patient) {
                // Display COMPLETE registration information
                
                // 1. PERSONAL INFORMATION (from registration)
                String fullName = patient.getFirstName() + " " + patient.getLastName();
                if (patient.getSuffix() != null && !patient.getSuffix().isEmpty()) {
                    fullName += " " + patient.getSuffix();
                }
                textPatientName.setText(fullName);
                
                // Set patient number
                textPatientNumber.setText(patient.getPatientId());
                
                // Simple demographic info for compact card
                StringBuilder demographics = new StringBuilder();
                if (patient.getGender() != null && !patient.getGender().isEmpty()) {
                    demographics.append(patient.getGender());
                }
                if (patient.getAge() != null && !patient.getAge().isEmpty()) {
                    if (demographics.length() > 0) demographics.append(" • ");
                    demographics.append(patient.getAge()).append(" years");
                }
                if (patient.getDateOfBirth() != null && !patient.getDateOfBirth().isEmpty()) {
                    if (demographics.length() > 0) demographics.append(" • ");
                    demographics.append(patient.getDateOfBirth());
                }
                textPatientGender.setText(demographics.length() > 0 ? demographics.toString() : "No information");
                
                // 4. COMPREHENSIVE REGISTRATION DATA
                StringBuilder registrationData = new StringBuilder();
                
                // Contact Information
                if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isEmpty()) {
                    registrationData.append("📞 Phone: ").append(patient.getPhoneNumber());
                }
                if (patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                    if (registrationData.length() > 0) registrationData.append("\n");
                    registrationData.append("📧 Email: ").append(patient.getEmail());
                }
                
                // Address Information  
                if (patient.getAddress() != null && !patient.getAddress().isEmpty()) {
                    if (registrationData.length() > 0) registrationData.append("\n");
                    registrationData.append("🏠 Address: ").append(patient.getAddress());
                }
                
                // Health Information - Allergies
                if (patient.getAllergies() != null && !patient.getAllergies().isEmpty()) {
                    if (registrationData.length() > 0) registrationData.append("\n");
                    registrationData.append("⚠️ Allergies: ").append(patient.getAllergies());
                }
                
                // Health Information - Current Medications
                if (patient.getMedications() != null && !patient.getMedications().isEmpty()) {
                    if (registrationData.length() > 0) registrationData.append("\n");
                    registrationData.append("💊 Medications: ").append(patient.getMedications());
                }
                
                // Health Information - Medical History
                if (patient.getMedicalHistory() != null && !patient.getMedicalHistory().isEmpty()) {
                    if (registrationData.length() > 0) registrationData.append("\n");
                    registrationData.append("📋 Medical History: ").append(patient.getMedicalHistory());
                }
                
                // Vital Signs from Registration
                boolean hasVitals = false;
                if (patient.getTemperature() != null && !patient.getTemperature().isEmpty()) {
                    if (registrationData.length() > 0) registrationData.append("\n");
                    registrationData.append("🌡️ Temperature: ").append(patient.getTemperature()).append("°C");
                    hasVitals = true;
                }
                if (patient.getPulseRate() != null && !patient.getPulseRate().isEmpty()) {
                    if (registrationData.length() > 0) registrationData.append("\n");
                    registrationData.append("📊 Pulse: ").append(patient.getPulseRate()).append(" BPM");
                    hasVitals = true;
                }
                if (patient.getBloodPressure() != null && !patient.getBloodPressure().isEmpty()) {
                    if (registrationData.length() > 0) registrationData.append("\n");
                    registrationData.append("🩸 BP: ").append(patient.getBloodPressure());
                    hasVitals = true;
                }
                if (patient.getBloodSugar() != null && !patient.getBloodSugar().isEmpty()) {
                    if (registrationData.length() > 0) registrationData.append("\n");
                    registrationData.append("🧪 Blood Sugar: ").append(patient.getBloodSugar()).append(" mg/dL");
                    hasVitals = true;
                }
                if (patient.getPainScale() != null && !patient.getPainScale().isEmpty()) {
                    if (registrationData.length() > 0) registrationData.append("\n");
                    registrationData.append("😣 Pain Scale: ").append(patient.getPainScale()).append("/10");
                    hasVitals = true;
                }
                
                // Current Symptoms (main complaint)
                if (patient.getSymptomsDescription() != null && !patient.getSymptomsDescription().isEmpty()) {
                    if (registrationData.length() > 0) registrationData.append("\n");
                    registrationData.append("🤒 Symptoms: ").append(patient.getSymptomsDescription());
                }
                
                // Emergency Contact
                if (patient.getEmergencyContactName() != null && !patient.getEmergencyContactName().isEmpty()) {
                    if (registrationData.length() > 0) registrationData.append("\n");
                    registrationData.append("🚨 Emergency Contact: ").append(patient.getEmergencyContactName());
                    if (patient.getEmergencyContactPhone() != null && !patient.getEmergencyContactPhone().isEmpty()) {
                        registrationData.append(" (").append(patient.getEmergencyContactPhone()).append(")");
                    }
                }
                
                // Simplified card display - all data will be shown in dialog instead
                
                // Status based on complete registration data
                String status = getRegistrationStatus(patient);
                // Shorten status text for badge
                String shortStatus = status;
                if (status.contains("Complete Registration")) {
                    shortStatus = "Complete";
                } else if (status.contains("Nearly Complete")) {
                    shortStatus = "Nearly Complete";
                } else if (status.contains("Partial")) {
                    shortStatus = "Partial";
                } else if (status.contains("Incomplete")) {
                    shortStatus = "Incomplete";
                } else if (status.contains("Has Allergies")) {
                    shortStatus = "Allergies";
                } else if (status.contains("Urgent")) {
                    shortStatus = "Urgent";
                } else if (status.contains("Patient Status")) {
                    shortStatus = "Active";
                }
                textPatientStatus.setText(shortStatus);
                
                // Update badge color based on status
                if (status.contains("Complete") || status.contains("✅")) {
                    textPatientStatus.setBackgroundResource(R.drawable.status_badge_background);
                    textPatientStatus.setTextColor(android.graphics.Color.WHITE);
                } else if (status.contains("Urgent") || status.contains("🚨")) {
                    textPatientStatus.setBackgroundColor(0xFFF44336); // Red
                    textPatientStatus.setTextColor(android.graphics.Color.WHITE);
                } else if (status.contains("Allergies") || status.contains("⚠️")) {
                    textPatientStatus.setBackgroundColor(0xFFFF9800); // Orange
                    textPatientStatus.setTextColor(android.graphics.Color.WHITE);
                } else if (status.contains("Nearly Complete") || status.contains("🟡")) {
                    textPatientStatus.setBackgroundColor(0xFFFFC107); // Yellow
                    textPatientStatus.setTextColor(android.graphics.Color.BLACK);
                } else {
                    textPatientStatus.setBackgroundColor(0xFF9E9E9E); // Gray
                    textPatientStatus.setTextColor(android.graphics.Color.WHITE);
                }
                
                // Set up click listener for the entire card
                itemView.setOnClickListener(v -> showPatientDetails(patient));
            }
            
            // Button listeners are no longer needed - entire card is clickable
            
            private void showPatientDetails(Patient patient) {
                // Create detailed patient information dialog
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(itemView.getContext());
                builder.setTitle("📋 Complete Patient Details");
                
                // Build comprehensive patient information
                StringBuilder details = new StringBuilder();
                
                // Personal Information Section
                details.append("👤 PERSONAL INFORMATION\n");
                details.append("─────────────────────────\n");
                details.append("Name: ").append(patient.getFirstName()).append(" ").append(patient.getLastName());
                if (patient.getSuffix() != null && !patient.getSuffix().isEmpty()) {
                    details.append(" ").append(patient.getSuffix());
                }
                details.append("\n");
                
                if (patient.getGender() != null && !patient.getGender().isEmpty()) {
                    details.append("Gender: ").append(patient.getGender()).append("\n");
                }
                if (patient.getAge() != null && !patient.getAge().isEmpty()) {
                    details.append("Age: ").append(patient.getAge()).append("\n");
                }
                if (patient.getDateOfBirth() != null && !patient.getDateOfBirth().isEmpty()) {
                    details.append("Date of Birth: ").append(patient.getDateOfBirth()).append("\n");
                }
                if (patient.getBirthPlace() != null && !patient.getBirthPlace().isEmpty()) {
                    details.append("Birth Place: ").append(patient.getBirthPlace()).append("\n");
                }
                
                // Contact Information Section
                details.append("\n📞 CONTACT INFORMATION\n");
                details.append("─────────────────────────\n");
                if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isEmpty()) {
                    details.append("Phone: ").append(patient.getPhoneNumber()).append("\n");
                }
                if (patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                    details.append("Email: ").append(patient.getEmail()).append("\n");
                }
                // Check both getAddress() and getFullAddress() for address
                String address = null;
                if (patient.getFullAddress() != null && !patient.getFullAddress().isEmpty()) {
                    address = patient.getFullAddress();
                } else if (patient.getAddress() != null && !patient.getAddress().isEmpty()) {
                    address = patient.getAddress();
                }
                if (address != null && !address.isEmpty()) {
                    details.append("Address: ").append(address).append("\n");
                } else {
                    details.append("Address: N/A\n");
                }
                
                // Health Information Section
                details.append("\n🏥 HEALTH INFORMATION\n");
                details.append("─────────────────────────\n");
                String allergies = (patient.getAllergies() != null && !patient.getAllergies().isEmpty()) 
                    ? patient.getAllergies() : "None";
                details.append("Allergies: ").append(allergies).append("\n");
                
                String medications = (patient.getMedications() != null && !patient.getMedications().isEmpty()) 
                    ? patient.getMedications() : "None";
                details.append("Medications: ").append(medications).append("\n");
                
                String medicalHistory = (patient.getMedicalHistory() != null && !patient.getMedicalHistory().isEmpty()) 
                    ? patient.getMedicalHistory() : "None";
                details.append("Medical History: ").append(medicalHistory).append("\n");
                
                // Vital Signs Section
                details.append("\n🩺 VITAL SIGNS\n");
                details.append("─────────────────────────\n");
                boolean hasVitals = false;
                if (patient.getTemperature() != null && !patient.getTemperature().isEmpty()) {
                    details.append("Temperature: ").append(patient.getTemperature()).append("°C\n");
                    hasVitals = true;
                }
                if (patient.getPulseRate() != null && !patient.getPulseRate().isEmpty()) {
                    details.append("Pulse Rate: ").append(patient.getPulseRate()).append(" BPM\n");
                    hasVitals = true;
                }
                if (patient.getBloodPressure() != null && !patient.getBloodPressure().isEmpty()) {
                    details.append("Blood Pressure: ").append(patient.getBloodPressure()).append("\n");
                    hasVitals = true;
                }
                if (patient.getBloodSugar() != null && !patient.getBloodSugar().isEmpty()) {
                    details.append("Blood Sugar: ").append(patient.getBloodSugar()).append(" mg/dL\n");
                    hasVitals = true;
                }
                if (patient.getPainScale() != null && !patient.getPainScale().isEmpty()) {
                    details.append("Pain Scale: ").append(patient.getPainScale()).append("/10\n");
                    hasVitals = true;
                }
                if (!hasVitals) {
                    details.append("No vital signs recorded\n");
                }
                
                // Symptoms Section
                details.append("\n🤒 CURRENT SYMPTOMS\n");
                details.append("─────────────────────────\n");
                if (patient.getSymptomsDescription() != null && !patient.getSymptomsDescription().isEmpty()) {
                    details.append(patient.getSymptomsDescription()).append("\n");
                } else {
                    details.append("No symptoms recorded\n");
                }
                
                // Emergency Contact Section
                details.append("\n🚨 EMERGENCY CONTACT\n");
                details.append("─────────────────────────\n");
                if (patient.getEmergencyContactName() != null && !patient.getEmergencyContactName().isEmpty()) {
                    details.append("Name: ").append(patient.getEmergencyContactName()).append("\n");
                }
                if (patient.getEmergencyContactPhone() != null && !patient.getEmergencyContactPhone().isEmpty()) {
                    details.append("Phone: ").append(patient.getEmergencyContactPhone()).append("\n");
                }
                
                builder.setMessage(details.toString());
                builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
                
                android.app.AlertDialog dialog = builder.create();
                dialog.show();
            }
            
            // Vital signs update functionality removed - keeping UI simple with view-only design
            
            private String getRegistrationStatus(Patient patient) {
                // Status based on all registration data
                
                // Check if this is a complete registration
                boolean hasCompleteInfo = true;
                int mandatoryFields = 0;
                int completedFields = 0;
                
                // Check mandatory fields
                mandatoryFields++;
                if (patient.getFirstName() != null && !patient.getFirstName().isEmpty()) completedFields++;
                
                mandatoryFields++;
                if (patient.getLastName() != null && !patient.getLastName().isEmpty()) completedFields++;
                
                mandatoryFields++;
                if (patient.getGender() != null && !patient.getGender().isEmpty()) completedFields++;
                
                mandatoryFields++;
                if (patient.getDateOfBirth() != null && !patient.getDateOfBirth().isEmpty()) completedFields++;
                
                mandatoryFields++;
                if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isEmpty()) completedFields++;
                
                mandatoryFields++;
                if (patient.getAddress() != null && !patient.getAddress().isEmpty()) completedFields++;
                
                // Check for critical medical information
                if (patient.getAllergies() != null && !patient.getAllergies().isEmpty()) {
                    return "⚠️ Has Allergies";
                }
                
                // Check symptoms urgency
                if (patient.getSymptomsDescription() != null && !patient.getSymptomsDescription().isEmpty()) {
                    String symptoms = patient.getSymptomsDescription().toLowerCase();
                    if (symptoms.contains("emergency") || symptoms.contains("urgent") || 
                        symptoms.contains("severe") || symptoms.contains("critical")) {
                        return "🚨 Urgent Case";
                    }
                }
                
                // Check pain level
                try {
                    if (patient.getPainScale() != null && !patient.getPainScale().isEmpty()) {
                        int pain = Integer.parseInt(patient.getPainScale());
                        if (pain >= 7) return "⚠️ Patient Status";
                        if (pain >= 4) return "🟡 Patient Status";
                    }
                } catch (NumberFormatException e) {
                    // Handle parsing errors gracefully
                }
                
                // Calculate completion percentage
                double completionRate = (double) completedFields / mandatoryFields * 100;
                
                if (completionRate >= 100) {
                    return "✅ Complete Registration";
                } else if (completionRate >= 80) {
                    return "🟡 Nearly Complete";
                } else if (completionRate >= 60) {
                    return "🟠 Partial Registration";
                } else {
                    return "🔴 Incomplete Registration";
                }
            }
            
            // Additional status methods removed for simplicity
        }
    }
}


