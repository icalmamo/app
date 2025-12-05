package com.example.h_cas.models;

/**
 * MedicationItem represents a single medication with its frequency and duration
 */
public class MedicationItem {
    private String medicationName;
    private String frequency;
    private String duration;

    public MedicationItem() {
    }

    public MedicationItem(String medicationName, String frequency, String duration) {
        this.medicationName = medicationName;
        this.frequency = frequency;
        this.duration = duration;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
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
}

