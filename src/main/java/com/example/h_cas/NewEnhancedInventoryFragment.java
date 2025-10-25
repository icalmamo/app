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
    private TextView emptyStateText;
    private MaterialButton addMedicineButton;
    private MaterialButton refreshButton;
    private MaterialButton lowStockButton;
    private MaterialButton expiringSoonButton;
    private MaterialButton analyticsButton;
    private MaterialButton suppliersButton;
    
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
        medicinesRecyclerView = view.findViewById(R.id.medicinesRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        addMedicineButton = view.findViewById(R.id.addMedicineButton);
        refreshButton = view.findViewById(R.id.refreshButton);
        lowStockButton = view.findViewById(R.id.lowStockButton);
        expiringSoonButton = view.findViewById(R.id.expiringSoonButton);
        analyticsButton = view.findViewById(R.id.analyticsButton);
        suppliersButton = view.findViewById(R.id.suppliersButton);
        
        allMedicines = new ArrayList<>();
        filteredMedicines = new ArrayList<>();
    }

    private void initializeDatabase() {
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupRecyclerView() {
        medicineAdapter = new MedicineAdapter(filteredMedicines);
        medicinesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        medicinesRecyclerView.setAdapter(medicineAdapter);
    }

    private void setupClickListeners() {
        addMedicineButton.setOnClickListener(v -> showAddMedicineDialog());
        refreshButton.setOnClickListener(v -> loadMedicines());
        lowStockButton.setOnClickListener(v -> toggleLowStockFilter());
        expiringSoonButton.setOnClickListener(v -> toggleExpiringSoonFilter());
        analyticsButton.setOnClickListener(v -> showAnalyticsDialog());
        suppliersButton.setOnClickListener(v -> showSuppliersDialog());
        
        // Back button functionality
        ImageButton backButton = getView().findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (getActivity() instanceof PharmacistDashboardActivity) {
                    ((PharmacistDashboardActivity) getActivity()).loadFragment(new PharmacistDashboardFragment());
                    ((PharmacistDashboardActivity) getActivity()).getSupportActionBar().setTitle("Pharmacist Dashboard");
                }
            });
        }
    }

    private void loadMedicines() {
        try {
            // Load medicines from database
            allMedicines.clear();
            allMedicines.addAll(databaseHelper.getAllMedicines());
            
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
            medicineAdapter.notifyDataSetChanged();
            
            updateEmptyState();
            
            Toast.makeText(getContext(), "📦 Inventory refreshed: " + allMedicines.size() + " medicines", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ Error loading inventory: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        if (showingLowStock) {
            // Show all medicines
            filteredMedicines.clear();
            filteredMedicines.addAll(allMedicines);
            showingLowStock = false;
            showingExpiringSoon = false;
            Toast.makeText(getContext(), "📦 Showing all medicines", Toast.LENGTH_SHORT).show();
        } else {
            // Show low stock medicines
            filteredMedicines.clear();
            filteredMedicines.addAll(databaseHelper.getLowStockMedicines());
            showingLowStock = true;
            showingExpiringSoon = false;
            Toast.makeText(getContext(), "⚠️ Showing " + filteredMedicines.size() + " low stock medicines", Toast.LENGTH_SHORT).show();
        }
        
        updateButtonStates();
        medicineAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void toggleExpiringSoonFilter() {
        if (showingExpiringSoon) {
            // Show all medicines
            filteredMedicines.clear();
            filteredMedicines.addAll(allMedicines);
            showingLowStock = false;
            showingExpiringSoon = false;
            Toast.makeText(getContext(), "📦 Showing all medicines", Toast.LENGTH_SHORT).show();
        } else {
            // Show expiring soon medicines
            filteredMedicines.clear();
            filteredMedicines.addAll(databaseHelper.getExpiringSoonMedicines());
            showingLowStock = false;
            showingExpiringSoon = true;
            Toast.makeText(getContext(), "⏰ Showing " + filteredMedicines.size() + " medicines expiring soon", Toast.LENGTH_SHORT).show();
        }
        
        updateButtonStates();
        medicineAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateButtonStates() {
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
        if (filteredMedicines.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            medicinesRecyclerView.setVisibility(View.GONE);
            
            if (showingLowStock) {
                emptyStateText.setText("🎉 No low stock medicines!\nAll medicines are well stocked.");
            } else if (showingExpiringSoon) {
                emptyStateText.setText("🎉 No expiring medicines!\nAll medicines have good expiry dates.");
            } else {
                emptyStateText.setText("📦 No medicines found\nAdd medicines to start managing your inventory");
            }
        } else {
            emptyStateText.setVisibility(View.GONE);
            medicinesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddMedicineDialog() {
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
        int lowStockCount = databaseHelper.getLowStockMedicinesCount();
        int expiringSoonCount = databaseHelper.getExpiringSoonMedicinesCount();
        
        String analyticsText = "📦 Total Medicines: " + totalMedicines + "\n\n" +
                             "⚠️ Low Stock Items: " + lowStockCount + "\n\n" +
                             "⏰ Expiring Soon: " + expiringSoonCount + "\n\n" +
                             "📈 Stock Health: " + (totalMedicines > 0 ? 
                                 String.format("%.1f%%", ((double)(totalMedicines - lowStockCount) / totalMedicines) * 100) : "N/A");
        
        builder.setMessage(analyticsText);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void showSuppliersDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("🏢 Supplier Information");
        
        // Get unique suppliers from medicines
        List<String> suppliers = new ArrayList<>();
        for (Medicine medicine : allMedicines) {
            if (!suppliers.contains(medicine.getSupplier())) {
                suppliers.add(medicine.getSupplier());
            }
        }
        
        StringBuilder supplierText = new StringBuilder("📋 Active Suppliers:\n\n");
        for (String supplier : suppliers) {
            int medicineCount = 0;
            for (Medicine medicine : allMedicines) {
                if (medicine.getSupplier().equals(supplier)) {
                    medicineCount++;
                }
            }
            supplierText.append("🏢 ").append(supplier).append(" (").append(medicineCount).append(" medicines)\n");
        }
        
        builder.setMessage(supplierText.toString());
        builder.setPositiveButton("Close", null);
        builder.show();
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
                medicineNameText.setText(medicine.getMedicineName());
                dosageText.setText("Dosage: " + medicine.getDosage());
                stockText.setText("Stock: " + medicine.getStockQuantity() + " " + medicine.getUnit());
                categoryText.setText("Category: " + medicine.getCategory());
                expiryText.setText("Expires: " + medicine.getExpiryDate());
                priceText.setText("Price: ₱" + String.format("%.2f", medicine.getPrice()));
                supplierText.setText("Supplier: " + medicine.getSupplier());

                // Set stock color based on quantity
                if (medicine.isLowStock()) {
                    stockText.setTextColor(getContext().getColor(R.color.warning_orange));
                } else {
                    stockText.setTextColor(getContext().getColor(R.color.success_green));
                }

                // Set expiry color
                if (medicine.getExpiryDate().contains("2024")) {
                    expiryText.setTextColor(getContext().getColor(R.color.error_red));
                } else {
                    expiryText.setTextColor(getContext().getColor(R.color.text_secondary));
                }

                editButton.setOnClickListener(v -> showEditMedicineDialog(medicine));
                deleteButton.setOnClickListener(v -> showDeleteConfirmation(medicine));
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
