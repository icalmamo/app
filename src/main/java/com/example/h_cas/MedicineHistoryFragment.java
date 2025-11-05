package com.example.h_cas;

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
import com.example.h_cas.models.Prescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * MedicineHistoryFragment shows complete history of all medicines including:
 * - Entered/Added medicines
 * - Disposed medicines
 * - Dispensed medicines
 * - Expired medicines
 */
public class MedicineHistoryFragment extends Fragment {

    private RecyclerView historyRecyclerView;
    private View emptyStateText;
    private MaterialButton refreshButton;
    private MaterialButton filterAllButton;
    private MaterialButton filterActiveButton;
    private MaterialButton filterExpiredButton;
    private MaterialButton filterDisposedButton;
    private MaterialButton filterDispensedButton;
    
    private HCasDatabaseHelper databaseHelper;
    private MedicineHistoryAdapter historyAdapter;
    private List<MedicineHistoryEvent> allHistoryEvents;
    private List<MedicineHistoryEvent> filteredHistoryEvents;
    
    private String currentFilter = "all"; // all, active, expired, disposed, dispensed

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_medicine_history, container, false);
            
            if (view == null) {
                return null;
            }
            
            initializeViews(view);
            initializeDatabase();
            setupRecyclerView();
            setupClickListeners();
            loadMedicineHistory();
            
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
        
        historyRecyclerView = view.findViewById(R.id.historyRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        refreshButton = view.findViewById(R.id.refreshButton);
        filterAllButton = view.findViewById(R.id.filterAllButton);
        filterActiveButton = view.findViewById(R.id.filterActiveButton);
        filterExpiredButton = view.findViewById(R.id.filterExpiredButton);
        filterDisposedButton = view.findViewById(R.id.filterDisposedButton);
        filterDispensedButton = view.findViewById(R.id.filterDispensedButton);
        
        allHistoryEvents = new ArrayList<>();
        filteredHistoryEvents = new ArrayList<>();
    }

    private void initializeDatabase() {
        if (getContext() == null) {
            return;
        }
        databaseHelper = new HCasDatabaseHelper(getContext());
    }

    private void setupRecyclerView() {
        if (getContext() == null || historyRecyclerView == null) {
            return;
        }
        
        try {
            if (filteredHistoryEvents == null) {
                filteredHistoryEvents = new ArrayList<>();
            }
            
            historyAdapter = new MedicineHistoryAdapter(filteredHistoryEvents);
            historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            historyRecyclerView.setAdapter(historyAdapter);
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error setting up history list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupClickListeners() {
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> loadMedicineHistory());
        }
        if (filterAllButton != null) {
            filterAllButton.setOnClickListener(v -> filterHistory("all"));
        }
        if (filterActiveButton != null) {
            filterActiveButton.setOnClickListener(v -> filterHistory("active"));
        }
        if (filterExpiredButton != null) {
            filterExpiredButton.setOnClickListener(v -> filterHistory("expired"));
        }
        if (filterDisposedButton != null) {
            filterDisposedButton.setOnClickListener(v -> filterHistory("disposed"));
        }
        if (filterDispensedButton != null) {
            filterDispensedButton.setOnClickListener(v -> filterHistory("dispensed"));
        }
    }

    private void loadMedicineHistory() {
        try {
            if (databaseHelper == null || allHistoryEvents == null) {
                return;
            }
            
            allHistoryEvents.clear();
            
            // Get all medicines from database (including expired ones)
            List<Medicine> allMedicines = databaseHelper.getAllMedicines();
            
            // Add medicine events
            if (allMedicines != null) {
                for (Medicine medicine : allMedicines) {
                    if (medicine != null) {
                        // Check status
                        String status = getMedicineStatus(medicine);
                        String eventType = getEventType(status);
                        String description = getEventDescription(medicine, status);
                        String date = getEventDate(medicine, status);
                        
                        allHistoryEvents.add(new MedicineHistoryEvent(
                            medicine.getMedicineName(),
                            medicine.getDosage(),
                            medicine.getStockQuantity(),
                            medicine.getUnit(),
                            medicine.getExpiryDate(),
                            status,
                            eventType,
                            description,
                            date,
                            medicine.getMedicineId()
                        ));
                    }
                }
            }
            
            // Get dispensed medicines from RFID data (more accurate for dispensed medicines)
            try {
                android.database.sqlite.SQLiteDatabase db = databaseHelper.getReadableDatabase();
                String query = "SELECT * FROM rfid_data WHERE is_dispensed = 1";
                android.database.Cursor cursor = db.rawQuery(query, null);
                
                if (cursor.moveToFirst()) {
                    do {
                        String medicineName = cursor.getString(4); // medicine_name column
                        String dosage = cursor.getString(5);
                        String patientName = cursor.getString(2);
                        String dispensedDate = cursor.getString(12);
                        String pharmacistName = cursor.getString(13);
                        String duration = cursor.getString(7); // duration column
                        
                        // Calculate quantity based on duration if available
                        int quantity = 1; // Default to 1 unit
                        try {
                            if (duration != null && !duration.isEmpty()) {
                                // Try to extract number from duration (e.g., "5 days" -> 5, "1 week" -> 7)
                                String[] parts = duration.trim().split("\\s+");
                                if (parts.length > 0) {
                                    int days = Integer.parseInt(parts[0]);
                                    // Estimate quantity based on typical daily frequency
                                    // Assuming 1 unit per day (can be adjusted)
                                    quantity = days;
                                }
                            }
                        } catch (Exception e) {
                            // If parsing fails, use default quantity
                        }
                        
                        if (medicineName != null && !medicineName.isEmpty()) {
                            String patientDisplayName = patientName != null && !patientName.isEmpty() ? patientName : "Unknown Patient";
                            String description = "Given to " + patientDisplayName;
                            if (pharmacistName != null && !pharmacistName.isEmpty()) {
                                description += " by " + pharmacistName;
                            }
                            
                            allHistoryEvents.add(new MedicineHistoryEvent(
                                medicineName,
                                dosage != null ? dosage : "N/A",
                                quantity,
                                "units",
                                null,
                                "Dispensed",
                                "DISPENSED",
                                description,
                                dispensedDate != null ? dispensedDate : "N/A",
                                cursor.getString(3) // prescription_id
                            ));
                        }
                    } while (cursor.moveToNext());
                }
                
                cursor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Sort by date (most recent first)
            Collections.sort(allHistoryEvents, (e1, e2) -> {
                // Simple date comparison - newer first
                if (e1.getDate() == null && e2.getDate() == null) return 0;
                if (e1.getDate() == null) return 1;
                if (e2.getDate() == null) return -1;
                return e2.getDate().compareTo(e1.getDate());
            });
            
            // Apply current filter
            filterHistory(currentFilter);
            
            if (getContext() != null) {
                Toast.makeText(getContext(), "📋 Loaded " + allHistoryEvents.size() + " medicine events", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error loading medicine history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
        }
    }

    private String getMedicineStatus(Medicine medicine) {
        if (medicine == null || medicine.getExpiryDate() == null || medicine.getExpiryDate().isEmpty()) {
            return "Active";
        }
        
        if (isExpired(medicine)) {
            return "Expired";
        }
        
        if (isExpiringSoon(medicine)) {
            return "Expiring Soon";
        }
        
        return "Active";
    }

    private boolean isExpired(Medicine medicine) {
        if (medicine == null || medicine.getExpiryDate() == null || medicine.getExpiryDate().isEmpty()) {
            return false;
        }
        
        try {
            String expiryDateStr = medicine.getExpiryDate().trim();
            String[] dateParts = expiryDateStr.split("-");
            
            if (dateParts.length != 3) {
                return false;
            }
            
            int expiryYear = Integer.parseInt(dateParts[0]);
            int expiryMonth = Integer.parseInt(dateParts[1]);
            int expiryDay = Integer.parseInt(dateParts[2]);
            
            java.util.Calendar currentCal = java.util.Calendar.getInstance();
            int currentYear = currentCal.get(java.util.Calendar.YEAR);
            int currentMonth = currentCal.get(java.util.Calendar.MONTH) + 1;
            int currentDay = currentCal.get(java.util.Calendar.DAY_OF_MONTH);
            
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

    private boolean isExpiringSoon(Medicine medicine) {
        if (medicine == null || medicine.getExpiryDate() == null || medicine.getExpiryDate().isEmpty()) {
            return false;
        }
        
        try {
            int thresholdMonths = PharmacistSettingsFragment.getExpiryNotificationMonths(getContext());
            String expiryDateStr = medicine.getExpiryDate().trim();
            String[] dateParts = expiryDateStr.split("-");
            
            if (dateParts.length != 3) {
                return false;
            }
            
            int expiryYear = Integer.parseInt(dateParts[0]);
            int expiryMonth = Integer.parseInt(dateParts[1]);
            int expiryDay = Integer.parseInt(dateParts[2]);
            
            java.util.Calendar currentCal = java.util.Calendar.getInstance();
            int currentYear = currentCal.get(java.util.Calendar.YEAR);
            int currentMonth = currentCal.get(java.util.Calendar.MONTH) + 1;
            int currentDay = currentCal.get(java.util.Calendar.DAY_OF_MONTH);
            
            int yearDiff = expiryYear - currentYear;
            int monthDiff = (yearDiff * 12) + (expiryMonth - currentMonth);
            
            if (monthDiff > 0 && expiryDay < currentDay) {
                monthDiff--;
            }
            
            return monthDiff >= 0 && monthDiff <= thresholdMonths;
        } catch (Exception e) {
            return false;
        }
    }

    private String getEventType(String status) {
        switch (status) {
            case "Active":
                return "ENTERED";
            case "Expired":
                return "EXPIRED";
            case "Expiring Soon":
                return "EXPIRING_SOON";
            case "Dispensed":
                return "DISPENSED";
            default:
                return "ENTERED";
        }
    }

    private String getEventDescription(Medicine medicine, String status) {
        if (medicine == null) {
            return "";
        }
        
        switch (status) {
            case "Active":
                return "Medicine entered into inventory";
            case "Expired":
                return "Medicine expired - ready for disposal";
            case "Expiring Soon":
                return "Medicine expiring soon - monitor closely";
            case "Dispensed":
                return "Medicine dispensed to patient";
            default:
                return "Medicine in inventory";
        }
    }

    private String getEventDate(Medicine medicine, String status) {
        // For now, use current date or expiry date
        // In a real system, you'd have created_date field
        if (medicine != null && medicine.getExpiryDate() != null) {
            return medicine.getExpiryDate();
        }
        return java.text.SimpleDateFormat.getDateInstance().format(new java.util.Date());
    }

    private void filterHistory(String filter) {
        currentFilter = filter;
        
        if (allHistoryEvents == null || filteredHistoryEvents == null) {
            return;
        }
        
        filteredHistoryEvents.clear();
        
        for (MedicineHistoryEvent event : allHistoryEvents) {
            if (event == null) continue;
            
            boolean shouldInclude = false;
            switch (filter) {
                case "all":
                    shouldInclude = true;
                    break;
                case "active":
                    shouldInclude = "Active".equals(event.getStatus()) || "Expiring Soon".equals(event.getStatus());
                    break;
                case "expired":
                    shouldInclude = "Expired".equals(event.getStatus());
                    break;
                case "disposed":
                    shouldInclude = "Expired".equals(event.getStatus());
                    break;
                case "dispensed":
                    shouldInclude = "Dispensed".equals(event.getStatus());
                    break;
                default:
                    shouldInclude = true;
            }
            
            if (shouldInclude) {
                filteredHistoryEvents.add(event);
            }
        }
        
        if (historyAdapter != null) {
            historyAdapter.notifyDataSetChanged();
        }
        
        updateButtonStates();
        updateEmptyState();
        
        String filterText = filter.equals("all") ? "All" : filter.substring(0, 1).toUpperCase() + filter.substring(1);
        if (getContext() != null) {
            Toast.makeText(getContext(), "Showing " + filteredHistoryEvents.size() + " " + filterText.toLowerCase() + " medicines", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateButtonStates() {
        if (getContext() == null) {
            return;
        }
        
        try {
            int activeColor = getContext().getColor(R.color.primary_blue);
            int inactiveColor = getContext().getColor(R.color.background_light);
            int activeTextColor = getContext().getColor(R.color.white);
            int inactiveTextColor = getContext().getColor(R.color.text_primary);
            
            filterAllButton.setBackgroundColor("all".equals(currentFilter) ? activeColor : inactiveColor);
            filterAllButton.setTextColor("all".equals(currentFilter) ? activeTextColor : inactiveTextColor);
            
            filterActiveButton.setBackgroundColor("active".equals(currentFilter) ? activeColor : inactiveColor);
            filterActiveButton.setTextColor("active".equals(currentFilter) ? activeTextColor : inactiveTextColor);
            
            filterExpiredButton.setBackgroundColor("expired".equals(currentFilter) ? activeColor : inactiveColor);
            filterExpiredButton.setTextColor("expired".equals(currentFilter) ? activeTextColor : inactiveTextColor);
            
            filterDisposedButton.setBackgroundColor("disposed".equals(currentFilter) ? activeColor : inactiveColor);
            filterDisposedButton.setTextColor("disposed".equals(currentFilter) ? activeTextColor : inactiveTextColor);
            
            filterDispensedButton.setBackgroundColor("dispensed".equals(currentFilter) ? activeColor : inactiveColor);
            filterDispensedButton.setTextColor("dispensed".equals(currentFilter) ? activeTextColor : inactiveTextColor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateEmptyState() {
        if (emptyStateText == null || historyRecyclerView == null) {
            return;
        }
        
        if (filteredHistoryEvents == null || filteredHistoryEvents.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            historyRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            historyRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    // Medicine History Event class
    private static class MedicineHistoryEvent {
        private String medicineName;
        private String dosage;
        private int quantity;
        private String unit;
        private String expiryDate;
        private String status;
        private String eventType;
        private String description;
        private String date;
        private String id;

        public MedicineHistoryEvent(String medicineName, String dosage, int quantity, String unit,
                                   String expiryDate, String status, String eventType, String description,
                                   String date, String id) {
            this.medicineName = medicineName;
            this.dosage = dosage;
            this.quantity = quantity;
            this.unit = unit;
            this.expiryDate = expiryDate;
            this.status = status;
            this.eventType = eventType;
            this.description = description;
            this.date = date;
            this.id = id;
        }

        public String getMedicineName() { return medicineName; }
        public String getDosage() { return dosage; }
        public int getQuantity() { return quantity; }
        public String getUnit() { return unit; }
        public String getExpiryDate() { return expiryDate; }
        public String getStatus() { return status; }
        public String getEventType() { return eventType; }
        public String getDescription() { return description; }
        public String getDate() { return date; }
        public String getId() { return id; }
    }

    // RecyclerView Adapter
    private class MedicineHistoryAdapter extends RecyclerView.Adapter<MedicineHistoryAdapter.HistoryViewHolder> {
        private List<MedicineHistoryEvent> events;

        public MedicineHistoryAdapter(List<MedicineHistoryEvent> events) {
            this.events = events != null ? events : new ArrayList<>();
        }

        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            try {
                android.content.Context context = parent.getContext();
                if (context == null) {
                    context = getContext();
                }
                
                if (context == null) {
                    throw new IllegalStateException("Cannot create view holder: no context available");
                }
                
                View view = LayoutInflater.from(context).inflate(R.layout.item_medicine_history, parent, false);
                if (view == null) {
                    return new HistoryViewHolder(new View(context));
                }
                return new HistoryViewHolder(view);
            } catch (Exception e) {
                e.printStackTrace();
                if (parent != null && parent.getContext() != null) {
                    return new HistoryViewHolder(new View(parent.getContext()));
                }
                throw new IllegalStateException("Cannot create HistoryViewHolder: no valid context");
            }
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            if (events == null || position < 0 || position >= events.size()) {
                return;
            }
            
            try {
                MedicineHistoryEvent event = events.get(position);
                if (holder != null && event != null) {
                    holder.bind(event);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return events != null ? events.size() : 0;
        }

        class HistoryViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView cardView;
            private TextView medicineNameText;
            private TextView eventTypeText;
            private TextView descriptionText;
            private TextView dateText;
            private TextView statusText;
            private TextView quantityText;

            public HistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                try {
                    if (itemView != null) {
                        cardView = itemView.findViewById(R.id.historyCardView);
                        medicineNameText = itemView.findViewById(R.id.historyMedicineNameText);
                        eventTypeText = itemView.findViewById(R.id.historyEventTypeText);
                        descriptionText = itemView.findViewById(R.id.historyDescriptionText);
                        dateText = itemView.findViewById(R.id.historyDateText);
                        statusText = itemView.findViewById(R.id.historyStatusText);
                        quantityText = itemView.findViewById(R.id.historyQuantityText);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void bind(MedicineHistoryEvent event) {
                if (event == null || getContext() == null) {
                    return;
                }
                
                try {
                    if (medicineNameText != null) {
                        medicineNameText.setText(event.getMedicineName() != null ? event.getMedicineName() : "N/A");
                    }
                    
                    if (eventTypeText != null) {
                        String eventType = event.getEventType();
                        String displayText = "";
                        switch (eventType) {
                            case "ENTERED":
                                displayText = "➕ Entered";
                                break;
                            case "DISPENSED":
                                displayText = "📤 Dispensed";
                                break;
                            case "EXPIRED":
                                displayText = "⏰ Expired";
                                break;
                            case "EXPIRING_SOON":
                                displayText = "⚠️ Expiring Soon";
                                break;
                            default:
                                displayText = "📋 " + eventType;
                        }
                        eventTypeText.setText(displayText);
                    }
                    
                    if (descriptionText != null) {
                        descriptionText.setText(event.getDescription() != null ? event.getDescription() : "");
                    }
                    
                    if (dateText != null) {
                        String dateStr = event.getDate() != null ? event.getDate() : "N/A";
                        if (event.getExpiryDate() != null && !event.getExpiryDate().isEmpty()) {
                            dateStr += " | Expires: " + event.getExpiryDate();
                        }
                        dateText.setText(dateStr);
                    }
                    
                    if (statusText != null) {
                        String status = event.getStatus() != null ? event.getStatus() : "Unknown";
                        statusText.setText("Status: " + status);
                        
                        // Color code status
                        try {
                            if ("Expired".equals(status)) {
                                statusText.setTextColor(getContext().getColor(R.color.error_red));
                            } else if ("Expiring Soon".equals(status)) {
                                statusText.setTextColor(getContext().getColor(R.color.warning_orange));
                            } else if ("Active".equals(status)) {
                                statusText.setTextColor(getContext().getColor(R.color.success_green));
                            } else if ("Dispensed".equals(status)) {
                                statusText.setTextColor(getContext().getColor(R.color.primary_blue));
                            } else {
                                statusText.setTextColor(getContext().getColor(R.color.text_secondary));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    if (quantityText != null) {
                        String qtyText = "Quantity: " + event.getQuantity() + " " + (event.getUnit() != null ? event.getUnit() : "units");
                        quantityText.setText(qtyText);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMedicineHistory();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        try {
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

