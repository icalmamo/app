package com.example.h_cas;

import android.app.AlertDialog;
import android.os.Bundle;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Medicine;

import java.util.ArrayList;
import java.util.List;

/**
 * NewEnhancedInventoryFragment - Advanced medicine inventory management system
 * Features: Real-time stock tracking, expiry alerts, supplier management, and analytics
 */
public class NewEnhancedInventoryFragment extends Fragment {

    // UI Components
    private RecyclerView medicinesRecyclerView;
    private View emptyStateText; // Changed to View since it's a LinearLayout
    private MaterialButton addMedicineButton;
    private MaterialButton refreshButton;
    private MaterialButton lowStockButton;
    private MaterialButton expiringSoonButton;
    private MaterialButton analyticsButton;
    private MaterialButton historyButton;
    
    // Data
    private List<Medicine> allMedicines;
    private List<Medicine> filteredMedicines;
    private MedicineAdapter medicineAdapter;
    private HCasDatabaseHelper databaseHelper;
    
    // Filter states
    private boolean showingLowStock = false;
    private boolean showingExpiringSoon = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_enhanced_inventory, container, false);
        
        initializeViews(view);
        initializeDatabase();
        setupRecyclerView();
        setupClickListeners();
        loadMedicines();
        
        return view;
    }

    private void initializeViews(View view) {
        if (view == null) {
            return;
        }
        
        medicinesRecyclerView = view.findViewById(R.id.medicinesRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        addMedicineButton = view.findViewById(R.id.addMedicineButton);
        refreshButton = view.findViewById(R.id.refreshButton);
        lowStockButton = view.findViewById(R.id.lowStockButton);
        expiringSoonButton = view.findViewById(R.id.expiringSoonButton);
        analyticsButton = view.findViewById(R.id.analyticsButton);
        historyButton = view.findViewById(R.id.suppliersButton);
        
        allMedicines = new ArrayList<>();
        filteredMedicines = new ArrayList();
        
        // Initialize empty state visibility
        if (emptyStateText != null) {
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void initializeDatabase() {
        if (getContext() == null) {
            return;
        }
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupRecyclerView() {
        if (getContext() == null || medicinesRecyclerView == null) {
            return;
        }
        
        medicineAdapter = new MedicineAdapter(filteredMedicines);
        medicinesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        medicinesRecyclerView.setAdapter(medicineAdapter);
    }

    private void setupClickListeners() {
        if (addMedicineButton != null) {
            addMedicineButton.setOnClickListener(v -> showAddMedicineDialog());
        }
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> loadMedicines());
        }
        if (lowStockButton != null) {
            lowStockButton.setOnClickListener(v -> toggleLowStockFilter());
        }
        if (expiringSoonButton != null) {
            expiringSoonButton.setOnClickListener(v -> toggleExpiringSoonFilter());
        }
        if (analyticsButton != null) {
            analyticsButton.setOnClickListener(v -> showAnalyticsDialog());
        }
        if (historyButton != null) {
            historyButton.setOnClickListener(v -> navigateToMedicineHistory());
        }
        
        // Back button functionality - moved to onViewCreated where view is guaranteed to exist
    }

    private void loadMedicines() {
        try {
            if (getContext() == null || databaseHelper == null) {
                return;
            }
            
            // Load medicines from database (exclude expired medicines - they should only be in Disposed Medicine section)
            allMedicines.clear();
            List<Medicine> medicines = databaseHelper.getAllMedicines();
            if (medicines != null) {
                for (Medicine medicine : medicines) {
                    if (medicine != null && !isExpired(medicine)) {
                        allMedicines.add(medicine);
                    }
                }
            }
            
            // If no medicines in database, add sample medicines for demo
            if (allMedicines.isEmpty()) {
                addSampleMedicines();
            }
            
            // Reset filters
            showingLowStock = false;
            showingExpiringSoon = false;
            updateButtonStates();
            
            filteredMedicines.clear();
            filteredMedicines.addAll(allMedicines);
            
            if (medicineAdapter != null) {
                medicineAdapter.notifyDataSetChanged();
            }
            
            updateEmptyState();
            
            if (getContext() != null) {
                Toast.makeText(getContext(), "📦 Inventory refreshed: " + allMedicines.size() + " medicines", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "❌ Error loading inventory: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
        }
    }

    private void addSampleMedicines() {
        // Enhanced sample medicines with more variety
        String[][] sampleMedicines = {
            {"MED001", "Paracetamol 500mg", "500mg", "150", "tablets", "Analgesic", "Pain reliever and fever reducer", "2025-12-31", "25.50", "PharmaCorp"},
            {"MED002", "Amoxicillin 250mg", "250mg", "5", "capsules", "Antibiotic", "Broad-spectrum antibiotic", "2024-06-15", "45.00", "MedSupply Inc"},
            {"MED003", "Ibuprofen 400mg", "400mg", "80", "tablets", "NSAID", "Anti-inflammatory and pain reliever", "2025-08-20", "35.75", "HealthPharma"},
            {"MED004", "Loratadine 10mg", "10mg", "2", "tablets", "Antihistamine", "Allergy relief medication", "2024-03-10", "28.00", "AllerCare"},
            {"MED005", "Omeprazole 20mg", "20mg", "45", "capsules", "Proton Pump Inhibitor", "Acid reflux treatment", "2025-10-15", "52.30", "DigestHealth"},
            {"MED006", "Metformin 500mg", "500mg", "120", "tablets", "Antidiabetic", "Type 2 diabetes management", "2025-11-30", "18.75", "DiabCare"},
            {"MED007", "Atorvastatin 20mg", "20mg", "8", "tablets", "Statin", "Cholesterol management", "2024-08-25", "67.50", "CardioPharm"},
            {"MED008", "Losartan 50mg", "50mg", "95", "tablets", "ARB", "Blood pressure control", "2025-09-12", "41.20", "HypertensionCorp"}
        };

        for (String[] medicine : sampleMedicines) {
            Medicine newMedicine = new Medicine();
            newMedicine.setMedicineId(medicine[0]);
            newMedicine.setMedicineName(medicine[1]);
            newMedicine.setDosage(medicine[2]);
            newMedicine.setStockQuantity(Integer.parseInt(medicine[3]));
            newMedicine.setUnit(medicine[4]);
            newMedicine.setCategory(medicine[5]);
            newMedicine.setDescription(medicine[6]);
            newMedicine.setExpiryDate(medicine[7]);
            newMedicine.setPrice(Double.parseDouble(medicine[8]));
            newMedicine.setSupplier(medicine[9]);
            
            databaseHelper.addMedicine(newMedicine);
            allMedicines.add(newMedicine);
        }
    }

    private void toggleLowStockFilter() {
        if (getContext() == null || databaseHelper == null || medicineAdapter == null) {
            return;
        }
        
        if (showingLowStock) {
            // Show all medicines
            filteredMedicines.clear();
            filteredMedicines.addAll(allMedicines);
            showingLowStock = false;
            showingExpiringSoon = false;
            Toast.makeText(getContext(), "📦 Showing all medicines", Toast.LENGTH_SHORT).show();
        } else {
            // Show low stock medicines using configurable minimum
            filteredMedicines.clear();
            int minimumStock = PharmacistSettingsFragment.getMinimumStockQuantity(getContext());
            for (Medicine medicine : allMedicines) {
                if (medicine != null && medicine.isLowStock(minimumStock)) {
                    filteredMedicines.add(medicine);
                }
            }
            showingLowStock = true;
            showingExpiringSoon = false;
            Toast.makeText(getContext(), "⚠️ Showing " + filteredMedicines.size() + " low stock medicines (threshold: " + minimumStock + ")", Toast.LENGTH_SHORT).show();
        }
        
        updateButtonStates();
        medicineAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void toggleExpiringSoonFilter() {
        if (getContext() == null || databaseHelper == null || medicineAdapter == null) {
            return;
        }
        
        if (showingExpiringSoon) {
            // Show all medicines
            filteredMedicines.clear();
            filteredMedicines.addAll(allMedicines);
            showingLowStock = false;
            showingExpiringSoon = false;
            Toast.makeText(getContext(), "📦 Showing all medicines", Toast.LENGTH_SHORT).show();
        } else {
            // Show expiring soon medicines using configurable threshold
            filteredMedicines.clear();
            int thresholdMonths = PharmacistSettingsFragment.getExpiryNotificationMonths(getContext());
            for (Medicine medicine : allMedicines) {
                if (medicine != null && isExpiringSoon(medicine, thresholdMonths)) {
                    filteredMedicines.add(medicine);
                }
            }
            showingLowStock = false;
            showingExpiringSoon = true;
            String monthText = thresholdMonths == 1 ? "month" : "months";
            Toast.makeText(getContext(), "⏰ Showing " + filteredMedicines.size() + " medicines expiring soon (threshold: " + thresholdMonths + " " + monthText + ")", Toast.LENGTH_SHORT).show();
        }
        
        updateButtonStates();
        medicineAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    /**
     * Check if a medicine is expired (past expiry date)
     */
    private boolean isExpired(Medicine medicine) {
        if (medicine == null || medicine.getExpiryDate() == null || medicine.getExpiryDate().isEmpty()) {
            return false;
        }
        
        try {
            // Parse expiry date (format: YYYY-MM-DD)
            String expiryDateStr = medicine.getExpiryDate().trim();
            String[] dateParts = expiryDateStr.split("-");
            
            if (dateParts.length != 3) {
                return false;
            }
            
            int expiryYear = Integer.parseInt(dateParts[0]);
            int expiryMonth = Integer.parseInt(dateParts[1]);
            int expiryDay = Integer.parseInt(dateParts[2]);
            
            // Get current date
            java.util.Calendar currentCal = java.util.Calendar.getInstance();
            int currentYear = currentCal.get(java.util.Calendar.YEAR);
            int currentMonth = currentCal.get(java.util.Calendar.MONTH) + 1; // Calendar months are 0-based
            int currentDay = currentCal.get(java.util.Calendar.DAY_OF_MONTH);
            
            // Check if expired
            if (expiryYear < currentYear) {
                return true;
            } else if (expiryYear == currentYear) {
                if (expiryMonth < currentMonth) {
                    return true;
                } else if (expiryMonth == currentMonth && expiryDay < currentDay) {
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a medicine is expiring soon based on the configurable threshold
     * @param medicine The medicine to check
     * @param thresholdMonths Number of months before expiry to mark as expiring soon
     * @return true if medicine is expiring within the threshold
     */
    private boolean isExpiringSoon(Medicine medicine, int thresholdMonths) {
        if (medicine == null || medicine.getExpiryDate() == null || medicine.getExpiryDate().isEmpty()) {
            return false;
        }
        
        try {
            // Parse expiry date (format: YYYY-MM-DD)
            String expiryDateStr = medicine.getExpiryDate().trim();
            String[] dateParts = expiryDateStr.split("-");
            
            if (dateParts.length != 3) {
                return false;
            }
            
            int expiryYear = Integer.parseInt(dateParts[0]);
            int expiryMonth = Integer.parseInt(dateParts[1]);
            int expiryDay = Integer.parseInt(dateParts[2]);
            
            // Get current date
            java.util.Calendar currentCal = java.util.Calendar.getInstance();
            int currentYear = currentCal.get(java.util.Calendar.YEAR);
            int currentMonth = currentCal.get(java.util.Calendar.MONTH) + 1; // Calendar months are 0-based
            int currentDay = currentCal.get(java.util.Calendar.DAY_OF_MONTH);
            
            // Calculate months difference
            int yearDiff = expiryYear - currentYear;
            int monthDiff = (yearDiff * 12) + (expiryMonth - currentMonth);
            
            // Adjust for day difference (if expiry day is before current day in same month, count as less than a month)
            if (monthDiff > 0 && expiryDay < currentDay) {
                monthDiff--;
            }
            
            // Check if within threshold
            return monthDiff >= 0 && monthDiff <= thresholdMonths;
            
        } catch (Exception e) {
            // If date parsing fails, return false
            return false;
        }
    }

    private void updateButtonStates() {
        if (getContext() == null || lowStockButton == null || expiringSoonButton == null) {
            return;
        }
        
        // Update button appearance based on filter state
        if (showingLowStock) {
            lowStockButton.setBackgroundColor(getContext().getColor(R.color.warning_orange));
            lowStockButton.setTextColor(getContext().getColor(R.color.white));
        } else {
            lowStockButton.setBackgroundColor(getContext().getColor(R.color.background_light));
            lowStockButton.setTextColor(getContext().getColor(R.color.text_primary));
        }
        
        if (showingExpiringSoon) {
            expiringSoonButton.setBackgroundColor(getContext().getColor(R.color.error_red));
            expiringSoonButton.setTextColor(getContext().getColor(R.color.white));
        } else {
            expiringSoonButton.setBackgroundColor(getContext().getColor(R.color.background_light));
            expiringSoonButton.setTextColor(getContext().getColor(R.color.text_primary));
        }
    }

    private void updateEmptyState() {
        if (emptyStateText == null || medicinesRecyclerView == null) {
            return;
        }
        
        if (filteredMedicines == null || filteredMedicines.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            medicinesRecyclerView.setVisibility(View.GONE);
            
            // Find TextView inside LinearLayout for empty state
            TextView emptyTextView = emptyStateText.findViewById(R.id.emptyStateTextView);
            if (emptyTextView == null && emptyStateText instanceof ViewGroup) {
                // If TextView not found, try to find any TextView as child
                ViewGroup viewGroup = (ViewGroup) emptyStateText;
                int childCount = viewGroup.getChildCount();
                if (childCount > 0) {
                    // Check the last child (usually the main text)
                    View child = viewGroup.getChildAt(childCount - 1);
                    if (child instanceof TextView) {
                        emptyTextView = (TextView) child;
                    } else {
                        // Search through all children for a TextView
                        for (int i = 0; i < childCount; i++) {
                            View childView = viewGroup.getChildAt(i);
                            if (childView instanceof TextView) {
                                emptyTextView = (TextView) childView;
                                break;
                            }
                        }
                    }
                }
            }
            
            if (emptyTextView != null) {
                if (showingLowStock) {
                    emptyTextView.setText("🎉 No low stock medicines!\nAll medicines are well stocked.");
                } else if (showingExpiringSoon) {
                    emptyTextView.setText("🎉 No expiring medicines!\nAll medicines have good expiry dates.");
                } else {
                    emptyTextView.setText("📦 No medicines found\nAdd medicines to start managing your inventory");
                }
            }
        } else {
            emptyStateText.setVisibility(View.GONE);
            medicinesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddMedicineDialog() {
        if (getContext() == null) {
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("➕ Add New Medicine");
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_medicine, null);
        builder.setView(dialogView);
        
        TextInputEditText nameInput = dialogView.findViewById(R.id.medicineNameInput);
        TextInputEditText dosageInput = dialogView.findViewById(R.id.dosageInput);
        TextInputEditText stockInput = dialogView.findViewById(R.id.stockInput);
        TextInputEditText unitInput = dialogView.findViewById(R.id.unitInput);
        TextInputEditText categoryInput = dialogView.findViewById(R.id.categoryInput);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        TextInputEditText expiryInput = dialogView.findViewById(R.id.expiryInput);
        TextInputEditText priceInput = dialogView.findViewById(R.id.priceInput);
        TextInputEditText supplierInput = dialogView.findViewById(R.id.supplierInput);
        
        builder.setPositiveButton("Add Medicine", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String dosage = dosageInput.getText().toString().trim();
            String stockStr = stockInput.getText().toString().trim();
            String unit = unitInput.getText().toString().trim();
            String category = categoryInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            String expiry = expiryInput.getText().toString().trim();
            String priceStr = priceInput.getText().toString().trim();
            String supplier = supplierInput.getText().toString().trim();
            
            if (validateMedicineInput(name, dosage, stockStr, unit, category, expiry, priceStr, supplier)) {
                int stock = Integer.parseInt(stockStr);
                double price = Double.parseDouble(priceStr);
                addMedicine(name, dosage, stock, unit, category, description, expiry, price, supplier);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private boolean validateMedicineInput(String name, String dosage, String stockStr, String unit, String category, String expiry, String priceStr, String supplier) {
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "❌ Medicine name is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (dosage.isEmpty()) {
            Toast.makeText(getContext(), "❌ Dosage is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (stockStr.isEmpty()) {
            Toast.makeText(getContext(), "❌ Stock quantity is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (unit.isEmpty()) {
            Toast.makeText(getContext(), "❌ Unit is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (category.isEmpty()) {
            Toast.makeText(getContext(), "❌ Category is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (expiry.isEmpty()) {
            Toast.makeText(getContext(), "❌ Expiry date is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (priceStr.isEmpty()) {
            Toast.makeText(getContext(), "❌ Price is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (supplier.isEmpty()) {
            Toast.makeText(getContext(), "❌ Supplier is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        try {
            int stock = Integer.parseInt(stockStr);
            if (stock < 0) {
                Toast.makeText(getContext(), "❌ Stock quantity must be positive", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "❌ Invalid stock quantity", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        try {
            double price = Double.parseDouble(priceStr);
            if (price < 0) {
                Toast.makeText(getContext(), "❌ Price must be positive", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "❌ Invalid price", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }

    private void addMedicine(String name, String dosage, int stock, String unit, String category, String description, String expiry, double price, String supplier) {
        Medicine newMedicine = new Medicine();
        newMedicine.setMedicineId("MED" + System.currentTimeMillis());
        newMedicine.setMedicineName(name);
        newMedicine.setDosage(dosage);
        newMedicine.setStockQuantity(stock);
        newMedicine.setUnit(unit);
        newMedicine.setCategory(category);
        newMedicine.setDescription(description);
        newMedicine.setExpiryDate(expiry);
        newMedicine.setPrice(price);
        newMedicine.setSupplier(supplier);

        // Add to database
        boolean success = databaseHelper.addMedicine(newMedicine);
        
        if (success) {
            // Refresh the list from database
            loadMedicines();
            Toast.makeText(getContext(), "✅ Medicine added successfully: " + name, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "❌ Failed to add medicine: " + name, Toast.LENGTH_SHORT).show();
        }
    }

    private void showAnalyticsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("📊 Inventory Analytics");
        
        int totalMedicines = databaseHelper.getTotalMedicinesCount();
        // Calculate low stock count using configurable minimum
        int minimumStock = PharmacistSettingsFragment.getMinimumStockQuantity(getContext());
        int lowStockCount = 0;
        for (Medicine medicine : allMedicines) {
            if (medicine != null && medicine.isLowStock(minimumStock)) {
                lowStockCount++;
            }
        }
        // Calculate expiring soon count using configurable threshold
        int thresholdMonths = PharmacistSettingsFragment.getExpiryNotificationMonths(getContext());
        int expiringSoonCount = 0;
        for (Medicine medicine : allMedicines) {
            if (medicine != null && isExpiringSoon(medicine, thresholdMonths)) {
                expiringSoonCount++;
            }
        }
        
        String analyticsText = "📦 Total Medicines: " + totalMedicines + "\n\n" +
                             "⚠️ Low Stock Items: " + lowStockCount + "\n\n" +
                             "⏰ Expiring Soon: " + expiringSoonCount + "\n\n" +
                             "📈 Stock Health: " + (totalMedicines > 0 ? 
                                 String.format("%.1f%%", ((double)(totalMedicines - lowStockCount) / totalMedicines) * 100) : "N/A");
        
        builder.setMessage(analyticsText);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void navigateToMedicineHistory() {
        if (getActivity() instanceof PharmacistDashboardActivity) {
            ((PharmacistDashboardActivity) getActivity()).loadFragment(new DisposedMedicineFragment());
            if (((PharmacistDashboardActivity) getActivity()).getSupportActionBar() != null) {
                ((PharmacistDashboardActivity) getActivity()).getSupportActionBar().setTitle("Disposed Medicine");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh medicines when returning to this fragment
        loadMedicines();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Simplified back navigation - remove OnBackPressedCallback for now
        ImageButton backButton = view.findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (getActivity() instanceof PharmacistDashboardActivity) {
                    ((PharmacistDashboardActivity) getActivity()).loadFragment(new PharmacistDashboardFragment());
                    ((PharmacistDashboardActivity) getActivity()).getSupportActionBar().setTitle("Pharmacist Dashboard");
                }
            });
        }
    }

    // Enhanced RecyclerView Adapter for medicines
    private class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {
        private List<Medicine> medicines;

        public MedicineAdapter(List<Medicine> medicines) {
            this.medicines = medicines;
        }

        @NonNull
        @Override
        public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_enhanced_medicine, parent, false);
            return new MedicineViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
            Medicine medicine = medicines.get(position);
            holder.bind(medicine);
        }

        @Override
        public int getItemCount() {
            return medicines.size();
        }

        class MedicineViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView medicineCard;
            private TextView medicineNameText;
            private TextView dosageText;
            private TextView stockText;
            private TextView categoryText;
            private TextView expiryText;
            private TextView priceText;
            private TextView supplierText;
            private MaterialButton editButton;
            private MaterialButton deleteButton;

            public MedicineViewHolder(@NonNull View itemView) {
                super(itemView);
                medicineCard = itemView.findViewById(R.id.medicineCard);
                medicineNameText = itemView.findViewById(R.id.medicineNameText);
                dosageText = itemView.findViewById(R.id.dosageText);
                stockText = itemView.findViewById(R.id.stockText);
                categoryText = itemView.findViewById(R.id.categoryText);
                expiryText = itemView.findViewById(R.id.expiryText);
                priceText = itemView.findViewById(R.id.priceText);
                supplierText = itemView.findViewById(R.id.supplierText);
                editButton = itemView.findViewById(R.id.editButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }

            public void bind(Medicine medicine) {
                if (medicine == null || getContext() == null) {
                    return;
                }
                
                // Safely set text with null checks
                medicineNameText.setText(medicine.getMedicineName() != null ? medicine.getMedicineName() : "Unknown");
                dosageText.setText("Dosage: " + (medicine.getDosage() != null ? medicine.getDosage() : "N/A"));
                stockText.setText("Stock: " + medicine.getStockQuantity() + " " + (medicine.getUnit() != null ? medicine.getUnit() : "units"));
                categoryText.setText("Category: " + (medicine.getCategory() != null ? medicine.getCategory() : "N/A"));
                
                String expiryDate = medicine.getExpiryDate() != null ? medicine.getExpiryDate() : "N/A";
                expiryText.setText("Expires: " + expiryDate);
                priceText.setText("Price: ₱" + String.format("%.2f", medicine.getPrice()));
                supplierText.setText("Supplier: " + (medicine.getSupplier() != null ? medicine.getSupplier() : "N/A"));

                // Set stock color based on quantity using configurable minimum
                try {
                    int minimumStock = PharmacistSettingsFragment.getMinimumStockQuantity(getContext());
                    if (medicine.isLowStock(minimumStock)) {
                        stockText.setTextColor(getContext().getColor(R.color.warning_orange));
                    } else {
                        stockText.setTextColor(getContext().getColor(R.color.success_green));
                    }

                    // Set expiry color
                    if (expiryDate.contains("2024")) {
                        expiryText.setTextColor(getContext().getColor(R.color.error_red));
                    } else {
                        expiryText.setTextColor(getContext().getColor(R.color.text_secondary));
                    }
                } catch (Exception e) {
                    // Fallback to default colors if resource access fails
                    e.printStackTrace();
                }

                if (editButton != null) {
                    editButton.setOnClickListener(v -> showEditMedicineDialog(medicine));
                }
                if (deleteButton != null) {
                    deleteButton.setOnClickListener(v -> showDeleteConfirmation(medicine));
                }
            }
        }
    }

    private void showEditMedicineDialog(Medicine medicine) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("✏️ Edit Medicine");
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_medicine, null);
        builder.setView(dialogView);
        
        TextInputEditText nameInput = dialogView.findViewById(R.id.medicineNameInput);
        TextInputEditText dosageInput = dialogView.findViewById(R.id.dosageInput);
        TextInputEditText stockInput = dialogView.findViewById(R.id.stockInput);
        TextInputEditText unitInput = dialogView.findViewById(R.id.unitInput);
        TextInputEditText categoryInput = dialogView.findViewById(R.id.categoryInput);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        TextInputEditText expiryInput = dialogView.findViewById(R.id.expiryInput);
        TextInputEditText priceInput = dialogView.findViewById(R.id.priceInput);
        TextInputEditText supplierInput = dialogView.findViewById(R.id.supplierInput);
        
        // Pre-fill with current values
        nameInput.setText(medicine.getMedicineName());
        dosageInput.setText(medicine.getDosage());
        stockInput.setText(String.valueOf(medicine.getStockQuantity()));
        unitInput.setText(medicine.getUnit());
        categoryInput.setText(medicine.getCategory());
        descriptionInput.setText(medicine.getDescription());
        expiryInput.setText(medicine.getExpiryDate());
        priceInput.setText(String.valueOf(medicine.getPrice()));
        supplierInput.setText(medicine.getSupplier());
        
        builder.setPositiveButton("Update Medicine", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String dosage = dosageInput.getText().toString().trim();
            String stockStr = stockInput.getText().toString().trim();
            String unit = unitInput.getText().toString().trim();
            String category = categoryInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            String expiry = expiryInput.getText().toString().trim();
            String priceStr = priceInput.getText().toString().trim();
            String supplier = supplierInput.getText().toString().trim();
            
            if (validateMedicineInput(name, dosage, stockStr, unit, category, expiry, priceStr, supplier)) {
                int stock = Integer.parseInt(stockStr);
                double price = Double.parseDouble(priceStr);
                updateMedicine(medicine, name, dosage, stock, unit, category, description, expiry, price, supplier);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateMedicine(Medicine medicine, String name, String dosage, int stock, String unit, String category, String description, String expiry, double price, String supplier) {
        medicine.setMedicineName(name);
        medicine.setDosage(dosage);
        medicine.setStockQuantity(stock);
        medicine.setUnit(unit);
        medicine.setCategory(category);
        medicine.setDescription(description);
        medicine.setExpiryDate(expiry);
        medicine.setPrice(price);
        medicine.setSupplier(supplier);

        // Update in database
        boolean success = databaseHelper.updateMedicine(medicine);
        
        if (success) {
            // Refresh the list from database
            loadMedicines();
            Toast.makeText(getContext(), "✅ Medicine updated successfully: " + name, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "❌ Failed to update medicine: " + name, Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation(Medicine medicine) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("🗑️ Delete Medicine");
        builder.setMessage("Are you sure you want to delete " + medicine.getMedicineName() + "?\n\nThis action cannot be undone.");
        
        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Delete from database
            boolean success = databaseHelper.deleteMedicine(medicine.getMedicineId());
            
            if (success) {
                // Refresh the list from database
                loadMedicines();
                Toast.makeText(getContext(), "✅ Medicine deleted: " + medicine.getMedicineName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "❌ Failed to delete medicine: " + medicine.getMedicineName(), Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
