package com.example.h_cas;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.h_cas.models.Medicine;

import java.util.ArrayList;
import java.util.List;

/**
 * DisposedMedicineFragment handles expired medicines that need to be disposed
 */
public class DisposedMedicineFragment extends Fragment {

    private RecyclerView expiredMedicinesRecyclerView;
    private View emptyStateText;
    private MaterialButton refreshButton;
    private MaterialButton disposeAllButton;
    
    private HCasDatabaseHelper databaseHelper;
    private ExpiredMedicineAdapter expiredMedicineAdapter;
    private List<Medicine> expiredMedicines;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_disposed_medicine, container, false);
            
            if (view == null) {
                return null;
            }
            
            initializeViews(view);
            initializeDatabase();
            setupRecyclerView();
            setupClickListeners();
            loadExpiredMedicines();
            
            return view;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initializeViews(View view) {
        if (view == null) {
            return;
        }
        
        expiredMedicinesRecyclerView = view.findViewById(R.id.expiredMedicinesRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        refreshButton = view.findViewById(R.id.refreshButton);
        disposeAllButton = view.findViewById(R.id.disposeAllButton);
        
        expiredMedicines = new ArrayList<>();
    }

    private void initializeDatabase() {
        if (getContext() == null) {
            return;
        }
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupRecyclerView() {
        if (getContext() == null || expiredMedicinesRecyclerView == null) {
            return;
        }
        
        try {
            if (expiredMedicines == null) {
                expiredMedicines = new ArrayList<>();
            }
            
            expiredMedicineAdapter = new ExpiredMedicineAdapter(expiredMedicines);
            expiredMedicinesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            expiredMedicinesRecyclerView.setAdapter(expiredMedicineAdapter);
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error setting up expired medicines list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupClickListeners() {
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> loadExpiredMedicines());
        }
        if (disposeAllButton != null) {
            disposeAllButton.setOnClickListener(v -> showDisposeAllConfirmation());
        }
    }

    private void loadExpiredMedicines() {
        try {
            if (databaseHelper == null || expiredMedicines == null) {
                return;
            }
            
            expiredMedicines.clear();
            
            // Get all medicines from database
            List<Medicine> allMedicines = databaseHelper.getAllMedicines();
            
            if (allMedicines != null) {
                // Filter for expired medicines
                for (Medicine medicine : allMedicines) {
                    if (medicine != null && isExpired(medicine)) {
                        expiredMedicines.add(medicine);
                    }
                }
            }
            
            if (expiredMedicineAdapter != null) {
                expiredMedicineAdapter.notifyDataSetChanged();
            }
            updateEmptyState();
            
            if (getContext() != null) {
                Toast.makeText(getContext(), "📋 Found " + expiredMedicines.size() + " expired medicines", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error loading expired medicines: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
        }
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

    private void showDisposeAllConfirmation() {
        if (getContext() == null || expiredMedicines == null || expiredMedicines.isEmpty()) {
            Toast.makeText(getContext(), "No expired medicines to dispose", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("⚠️ Dispose All Expired Medicines");
        builder.setMessage("Are you sure you want to delete all " + expiredMedicines.size() + " expired medicines from inventory?\n\nThis action cannot be undone!");
        
        builder.setPositiveButton("Yes, Dispose All", (dialog, which) -> {
            disposeAllExpiredMedicines();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.show();
    }

    private void disposeAllExpiredMedicines() {
        try {
            if (databaseHelper == null || expiredMedicines == null) {
                return;
            }
            
            int disposedCount = 0;
            for (Medicine medicine : expiredMedicines) {
                if (medicine != null && medicine.getMedicineId() != null) {
                    boolean deleted = databaseHelper.deleteMedicine(medicine.getMedicineId());
                    if (deleted) {
                        disposedCount++;
                    }
                }
            }
            
            // Reload the list
            loadExpiredMedicines();
            
            if (getContext() != null) {
                Toast.makeText(getContext(), "✅ Disposed " + disposedCount + " expired medicines", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error disposing medicines: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
        }
    }

    private void updateEmptyState() {
        if (emptyStateText == null || expiredMedicinesRecyclerView == null) {
            return;
        }
        
        if (expiredMedicines == null || expiredMedicines.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            expiredMedicinesRecyclerView.setVisibility(View.GONE);
            if (disposeAllButton != null) {
                disposeAllButton.setEnabled(false);
            }
        } else {
            emptyStateText.setVisibility(View.GONE);
            expiredMedicinesRecyclerView.setVisibility(View.VISIBLE);
            if (disposeAllButton != null) {
                disposeAllButton.setEnabled(true);
            }
        }
    }

    // RecyclerView Adapter for expired medicines
    private class ExpiredMedicineAdapter extends RecyclerView.Adapter<ExpiredMedicineAdapter.ExpiredMedicineViewHolder> {
        private List<Medicine> medicines;

        public ExpiredMedicineAdapter(List<Medicine> medicines) {
            this.medicines = medicines != null ? medicines : new ArrayList<>();
        }

        @NonNull
        @Override
        public ExpiredMedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            try {
                android.content.Context context = parent.getContext();
                if (context == null) {
                    context = getContext();
                }
                
                if (context == null) {
                    throw new IllegalStateException("Cannot create view holder: no context available");
                }
                
                View view = LayoutInflater.from(context).inflate(R.layout.item_expired_medicine, parent, false);
                if (view == null) {
                    View emptyView = new View(context);
                    return new ExpiredMedicineViewHolder(emptyView);
                }
                return new ExpiredMedicineViewHolder(view);
            } catch (Exception e) {
                e.printStackTrace();
                if (parent != null && parent.getContext() != null) {
                    return new ExpiredMedicineViewHolder(new View(parent.getContext()));
                }
                throw new IllegalStateException("Cannot create ExpiredMedicineViewHolder: no valid context");
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ExpiredMedicineViewHolder holder, int position) {
            if (medicines == null || position < 0 || position >= medicines.size()) {
                return;
            }
            
            try {
                Medicine medicine = medicines.get(position);
                if (holder != null && medicine != null) {
                    holder.bind(medicine);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return medicines != null ? medicines.size() : 0;
        }

        class ExpiredMedicineViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView cardView;
            private TextView medicineNameText;
            private TextView expiryDateText;
            private TextView stockQuantityText;
            private TextView dosageText;
            private MaterialButton disposeButton;

            public ExpiredMedicineViewHolder(@NonNull View itemView) {
                super(itemView);
                try {
                    if (itemView != null) {
                        cardView = itemView.findViewById(R.id.expiredMedicineCardView);
                        medicineNameText = itemView.findViewById(R.id.expiredMedicineNameText);
                        expiryDateText = itemView.findViewById(R.id.expiredExpiryDateText);
                        stockQuantityText = itemView.findViewById(R.id.expiredStockQuantityText);
                        dosageText = itemView.findViewById(R.id.expiredDosageText);
                        disposeButton = itemView.findViewById(R.id.disposeButton);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void bind(Medicine medicine) {
                if (medicine == null || getContext() == null) {
                    return;
                }
                
                try {
                    if (medicineNameText != null) {
                        medicineNameText.setText(medicine.getMedicineName() != null ? medicine.getMedicineName() : "N/A");
                    }
                    if (expiryDateText != null) {
                        expiryDateText.setText("Expired: " + (medicine.getExpiryDate() != null ? medicine.getExpiryDate() : "N/A"));
                    }
                    if (stockQuantityText != null) {
                        stockQuantityText.setText("Stock: " + medicine.getStockQuantity() + " " + (medicine.getUnit() != null ? medicine.getUnit() : "units"));
                    }
                    if (dosageText != null) {
                        dosageText.setText("Dosage: " + (medicine.getDosage() != null ? medicine.getDosage() : "N/A"));
                    }

                    if (disposeButton != null) {
                        disposeButton.setOnClickListener(v -> showDisposeConfirmation(medicine));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void showDisposeConfirmation(Medicine medicine) {
        if (getContext() == null || medicine == null) {
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("⚠️ Dispose Medicine");
        builder.setMessage("Are you sure you want to dispose:\n\n" +
                          medicine.getMedicineName() + "\n" +
                          "Expiry Date: " + medicine.getExpiryDate() + "\n" +
                          "Stock: " + medicine.getStockQuantity() + " " + medicine.getUnit() + "\n\n" +
                          "This action cannot be undone!");
        
        builder.setPositiveButton("Yes, Dispose", (dialog, which) -> {
            disposeMedicine(medicine);
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.show();
    }

    private void disposeMedicine(Medicine medicine) {
        try {
            if (databaseHelper == null || medicine == null || medicine.getMedicineId() == null) {
                return;
            }
            
            boolean deleted = databaseHelper.deleteMedicine(medicine.getMedicineId());
            
            if (deleted) {
                loadExpiredMedicines();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "✅ Medicine disposed: " + medicine.getMedicineName(), Toast.LENGTH_SHORT).show();
                }
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "❌ Failed to dispose medicine", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error disposing medicine: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh expired medicines when returning to this fragment
        loadExpiredMedicines();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        try {
            // Enable back navigation
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        try {
                            if (getActivity() instanceof PharmacistDashboardActivity) {
                                ((PharmacistDashboardActivity) getActivity()).loadFragment(new PharmacistDashboardFragment());
                                if (((PharmacistDashboardActivity) getActivity()).getSupportActionBar() != null) {
                                    ((PharmacistDashboardActivity) getActivity()).getSupportActionBar().setTitle("Pharmacist Dashboard");
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}









