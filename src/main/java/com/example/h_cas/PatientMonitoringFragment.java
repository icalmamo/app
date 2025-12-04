package com.example.h_cas;

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

import com.example.h_cas.models.Patient;
import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.database.FirebaseRTDBHelper;

import java.util.ArrayList;
import java.util.List;

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
        patientList = new ArrayList<>();
        patientAdapter = new PatientAdapter(patientList);
        
        recyclerViewPatients.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewPatients.setAdapter(patientAdapter);
        // Performance optimizations
        recyclerViewPatients.setHasFixedSize(true); // RecyclerView size doesn't change
        recyclerViewPatients.setItemViewCacheSize(20); // Cache more views for smoother scrolling
    }
    
    /**
     * Load patients from Firebase RTDB (primary source)
     */
    private void loadPatients() {
        // Fetch patients from Firebase RTDB
        if (firebaseRTDBHelper != null) {
            firebaseRTDBHelper.getAllPatients(patients -> {
                // Update UI on main thread
                com.example.h_cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                    if (getContext() == null || getView() == null) {
                        return; // Fragment is detached
                    }
                    // Use efficient DiffUtil update instead of notifyDataSetChanged
                    patientAdapter.setPatients(patients);
                    patientList.clear();
                    patientList.addAll(patients);
                    
                    updatePatientCount();
                    updateMonitoringStatus();
                });
            });
        } else {
            // Fallback to SQLite if Firebase not available
            com.example.h_cas.utils.DatabaseExecutor.getInstance().execute(() -> {
                List<Patient> patients = databaseHelper.getAllPatients();
                
                // Update UI on main thread
                com.example.h_cas.utils.DatabaseExecutor.getInstance().executeOnMainThread(() -> {
                    if (getContext() == null || getView() == null) {
                        return; // Fragment is detached
                    }
                    patientAdapter.setPatients(patients);
                    patientList.clear();
                    patientList.addAll(patients);
                    
                    updatePatientCount();
                    updateMonitoringStatus();
                });
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
            
            // Use DiffUtil for efficient updates (only updates changed items)
            androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = 
                androidx.recyclerview.widget.DiffUtil.calculateDiff(new PatientDiffCallback(this.patients, newPatients));
            
            this.patients.clear();
            this.patients.addAll(newPatients);
            diffResult.dispatchUpdatesTo(this);
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
            return patients.size();
        }
        
        class PatientViewHolder extends RecyclerView.ViewHolder {
            private TextView textPatientIcon;
            private TextView textPatientNumber;
            private TextView textPatientName;
            private TextView textPatientGender;
            private TextView textPatientStatus;
            
            public PatientViewHolder(@NonNull View itemView) {
                super(itemView);
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
                    demographics.append("Age: ").append(patient.getAge());
                }
                if (patient.getDateOfBirth() != null && !patient.getDateOfBirth().isEmpty()) {
                    if (demographics.length() > 0) demographics.append(" • ");
                    demographics.append("DOB: ").append(patient.getDateOfBirth());
                }
                textPatientGender.setText(demographics.toString());
                
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
                textPatientStatus.setText(status);
                
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
                if (patient.getAddress() != null && !patient.getAddress().isEmpty()) {
                    details.append("Address: ").append(patient.getAddress()).append("\n");
                }
                
                // Health Information Section
                details.append("\n🏥 HEALTH INFORMATION\n");
                details.append("─────────────────────────\n");
                if (patient.getAllergies() != null && !patient.getAllergies().isEmpty()) {
                    details.append("Allergies: ").append(patient.getAllergies()).append("\n");
                }
                if (patient.getMedications() != null && !patient.getMedications().isEmpty()) {
                    details.append("Medications: ").append(patient.getMedications()).append("\n");
                }
                if (patient.getMedicalHistory() != null && !patient.getMedicalHistory().isEmpty()) {
                    details.append("Medical History: ").append(patient.getMedicalHistory()).append("\n");
                }
                
                // Vital Signs Section
                details.append("\n🩺 VITAL SIGNS\n");
                details.append("─────────────────────────\n");
                if (patient.getTemperature() != null && !patient.getTemperature().isEmpty()) {
                    details.append("Temperature: ").append(patient.getTemperature()).append("°C\n");
                }
                if (patient.getPulseRate() != null && !patient.getPulseRate().isEmpty()) {
                    details.append("Pulse Rate: ").append(patient.getPulseRate()).append(" BPM\n");
                }
                if (patient.getBloodPressure() != null && !patient.getBloodPressure().isEmpty()) {
                    details.append("Blood Pressure: ").append(patient.getBloodPressure()).append("\n");
                }
                if (patient.getBloodSugar() != null && !patient.getBloodSugar().isEmpty()) {
                    details.append("Blood Sugar: ").append(patient.getBloodSugar()).append(" mg/dL\n");
                }
                if (patient.getPainScale() != null && !patient.getPainScale().isEmpty()) {
                    details.append("Pain Scale: ").append(patient.getPainScale()).append("/10\n");
                }
                
                // Symptoms Section
                if (patient.getSymptomsDescription() != null && !patient.getSymptomsDescription().isEmpty()) {
                    details.append("\n🤒 CURRENT SYMPTOMS\n");
                    details.append("─────────────────────────\n");
                    details.append(patient.getSymptomsDescription()).append("\n");
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


