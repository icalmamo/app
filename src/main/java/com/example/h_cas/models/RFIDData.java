package com.example.h_cas.models;

/**
 * RFIDData model representing data stored on RFID tags.
 */
public class RFIDData {
    private String rfidTagId;
    private String patientId;
    private String patientName;
    private String prescriptionId;
    private String medicineName;
    private String dosage;
    private String frequency;
    private String duration;
    private String instructions;
    private String doctorName;
    private String prescriptionDate;
    private boolean isDispensed;
    private String dispensedDate;
    private String pharmacistName;

    // Default constructor
    public RFIDData() {}

    // Constructor with basic parameters
    public RFIDData(String rfidTagId, String patientId, String patientName, String prescriptionId, 
                   String medicineName, String dosage, String frequency, String duration) {
        this.rfidTagId = rfidTagId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.prescriptionId = prescriptionId;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.frequency = frequency;
        this.duration = duration;
        this.isDispensed = false;
    }

    // Getters and Setters
    public String getRfidTagId() {
        return rfidTagId;
    }

    public void setRfidTagId(String rfidTagId) {
        this.rfidTagId = rfidTagId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getPrescriptionDate() {
        return prescriptionDate;
    }

    public void setPrescriptionDate(String prescriptionDate) {
        this.prescriptionDate = prescriptionDate;
    }

    public boolean isDispensed() {
        return isDispensed;
    }

    public void setDispensed(boolean dispensed) {
        isDispensed = dispensed;
    }

    public String getDispensedDate() {
        return dispensedDate;
    }

    public void setDispensedDate(String dispensedDate) {
        this.dispensedDate = dispensedDate;
    }

    public String getPharmacistName() {
        return pharmacistName;
    }

    public void setPharmacistName(String pharmacistName) {
        this.pharmacistName = pharmacistName;
    }

    // Utility methods
    public String getFormattedData() {
        return "Patient: " + patientName + "\n" +
               "Medicine: " + medicineName + "\n" +
               "Dosage: " + dosage + "\n" +
               "Frequency: " + frequency + "\n" +
               "Duration: " + duration + "\n" +
               "Doctor: " + doctorName;
    }

    @Override
    public String toString() {
        return "RFIDData{" +
                "rfidTagId='" + rfidTagId + '\'' +
                ", patientId='" + patientId + '\'' +
                ", patientName='" + patientName + '\'' +
                ", medicineName='" + medicineName + '\'' +
                ", isDispensed=" + isDispensed +
                '}';
    }
}














