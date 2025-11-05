package com.example.h_cas.models;

/**
 * Medicine model representing available medicines in the pharmacy inventory.
 */
public class Medicine {
    private String medicineId;
    private String medicineName;
    private String dosage;
    private int stockQuantity;
    private String unit; // tablets, capsules, ml, etc.
    private String category; // pain relief, antibiotics, etc.
    private String description;
    private String expiryDate;
    private double price;
    private String supplier;

    // Default constructor
    public Medicine() {}

    // Constructor with basic parameters
    public Medicine(String medicineId, String medicineName, String dosage, int stockQuantity, String unit) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.stockQuantity = stockQuantity;
        this.unit = unit;
    }

    // Getters and Setters
    public String getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(String medicineId) {
        this.medicineId = medicineId;
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

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    // Utility methods
    public boolean isInStock() {
        return stockQuantity > 0;
    }

    /**
     * Check if medicine is low stock using default threshold (10)
     * @return true if stock is at or below default threshold
     */
    public boolean isLowStock() {
        return stockQuantity <= 10; // Default threshold
    }

    /**
     * Check if medicine is low stock using custom threshold
     * @param minimumStockQuantity The minimum stock quantity threshold
     * @return true if stock is at or below the threshold
     */
    public boolean isLowStock(int minimumStockQuantity) {
        return stockQuantity <= minimumStockQuantity;
    }

    public void reduceStock(int quantity) {
        if (stockQuantity >= quantity) {
            stockQuantity -= quantity;
        }
    }

    public void addStock(int quantity) {
        stockQuantity += quantity;
    }

    @Override
    public String toString() {
        return "Medicine{" +
                "medicineId='" + medicineId + '\'' +
                ", medicineName='" + medicineName + '\'' +
                ", dosage='" + dosage + '\'' +
                ", stockQuantity=" + stockQuantity +
                ", unit='" + unit + '\'' +
                '}';
    }
}









