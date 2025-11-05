package com.example.h_cas;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Medicine;
import android.util.Log;

/**
 * PharmacistSettingsFragment handles pharmacy settings including minimum stock quantity
 */
public class PharmacistSettingsFragment extends Fragment {

    private TextInputEditText minimumStockInput;
    private TextInputEditText expiryNotificationDaysInput;
    private MaterialButton saveSettingsButton;
    private MaterialButton resetSettingsButton;
    private MaterialButton testFirebaseButton;
    private TextView currentMinimumStockText;
    private TextView currentExpiryNotificationDaysText;
    private MaterialCardView settingsCard;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "PharmacistSettingsPrefs";
    private static final String KEY_MINIMUM_STOCK_QUANTITY = "minimum_stock_quantity";
    private static final String KEY_EXPIRY_NOTIFICATION_MONTHS = "expiry_notification_months";
    private static final int DEFAULT_MINIMUM_STOCK = 10;
    private static final int DEFAULT_EXPIRY_NOTIFICATION_MONTHS = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pharmacist_settings, container, false);
        
        initializeViews(view);
        initializeSharedPreferences();
        loadCurrentSettings();
        setupClickListeners();
        
        return view;
    }

    private void initializeViews(View view) {
        if (view == null) {
            return;
        }
        
        settingsCard = view.findViewById(R.id.settingsCard);
        minimumStockInput = view.findViewById(R.id.minimumStockInput);
        expiryNotificationDaysInput = view.findViewById(R.id.expiryNotificationDaysInput);
        saveSettingsButton = view.findViewById(R.id.saveSettingsButton);
        resetSettingsButton = view.findViewById(R.id.resetSettingsButton);
        testFirebaseButton = view.findViewById(R.id.testFirebaseButton);
        currentMinimumStockText = view.findViewById(R.id.currentMinimumStockText);
        currentExpiryNotificationDaysText = view.findViewById(R.id.currentExpiryNotificationDaysText);
    }

    private void initializeSharedPreferences() {
        if (getContext() == null) {
            return;
        }
        sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void loadCurrentSettings() {
        if (sharedPreferences == null || currentMinimumStockText == null) {
            return;
        }
        
        int currentMinimum = sharedPreferences.getInt(KEY_MINIMUM_STOCK_QUANTITY, DEFAULT_MINIMUM_STOCK);
        currentMinimumStockText.setText("Current Minimum Stock: " + currentMinimum);
        
        if (minimumStockInput != null) {
            minimumStockInput.setText(String.valueOf(currentMinimum));
        }
        
        int currentExpiryMonths = sharedPreferences.getInt(KEY_EXPIRY_NOTIFICATION_MONTHS, DEFAULT_EXPIRY_NOTIFICATION_MONTHS);
        if (currentExpiryNotificationDaysText != null) {
            String monthText = currentExpiryMonths == 1 ? "month" : "months";
            currentExpiryNotificationDaysText.setText("Current Threshold: " + currentExpiryMonths + " " + monthText);
        }
        
        if (expiryNotificationDaysInput != null) {
            expiryNotificationDaysInput.setText(String.valueOf(currentExpiryMonths));
        }
    }

    private void setupClickListeners() {
        if (saveSettingsButton != null) {
            saveSettingsButton.setOnClickListener(v -> saveSettings());
        }

        if (resetSettingsButton != null) {
            resetSettingsButton.setOnClickListener(v -> resetSettings());
        }

        if (testFirebaseButton != null) {
            testFirebaseButton.setOnClickListener(v -> testFirebaseSync());
        }
    }

    private void saveSettings() {
        if (minimumStockInput == null || expiryNotificationDaysInput == null || sharedPreferences == null) {
            return;
        }
        
        try {
            // Validate minimum stock
            String stockInputText = minimumStockInput.getText() != null ? minimumStockInput.getText().toString().trim() : "";
            
            if (stockInputText.isEmpty()) {
                showToast("Please enter a minimum stock quantity");
                return;
            }
            
            int minimumStock = Integer.parseInt(stockInputText);
            
            if (minimumStock < 0) {
                showToast("Minimum stock quantity cannot be negative");
                return;
            }
            
            if (minimumStock > 1000) {
                showToast("Minimum stock quantity is too high (max: 1000)");
                return;
            }
            
            // Validate expiry notification months
            String expiryInputText = expiryNotificationDaysInput.getText() != null ? expiryNotificationDaysInput.getText().toString().trim() : "";
            
            if (expiryInputText.isEmpty()) {
                showToast("Please enter months before expiry");
                return;
            }
            
            int expiryMonths = Integer.parseInt(expiryInputText);
            
            if (expiryMonths < 1) {
                showToast("Months before expiry must be at least 1");
                return;
            }
            
            if (expiryMonths > 12) {
                showToast("Months before expiry is too high (max: 12)");
                return;
            }
            
            // Save to SharedPreferences
            sharedPreferences.edit()
                    .putInt(KEY_MINIMUM_STOCK_QUANTITY, minimumStock)
                    .putInt(KEY_EXPIRY_NOTIFICATION_MONTHS, expiryMonths)
                    .apply();
            
            // Update display
            if (currentMinimumStockText != null) {
                currentMinimumStockText.setText("Current Minimum Stock: " + minimumStock);
            }
            
            if (currentExpiryNotificationDaysText != null) {
                String monthText = expiryMonths == 1 ? "month" : "months";
                currentExpiryNotificationDaysText.setText("Current Threshold: " + expiryMonths + " " + monthText);
            }
            
            String monthText = expiryMonths == 1 ? "month" : "months";
            showToast("✅ Settings saved successfully!\nMinimum stock: " + minimumStock + "\nExpiring soon threshold: " + expiryMonths + " " + monthText);
            
        } catch (NumberFormatException e) {
            showToast("Please enter valid numbers");
        } catch (Exception e) {
            showToast("Error saving settings: " + e.getMessage());
        }
    }

    private void resetSettings() {
        if (sharedPreferences == null) {
            return;
        }
        
        // Reset to default
        sharedPreferences.edit()
                .putInt(KEY_MINIMUM_STOCK_QUANTITY, DEFAULT_MINIMUM_STOCK)
                .putInt(KEY_EXPIRY_NOTIFICATION_MONTHS, DEFAULT_EXPIRY_NOTIFICATION_MONTHS)
                .apply();
        
        // Update UI
        if (currentMinimumStockText != null) {
            currentMinimumStockText.setText("Current Minimum Stock: " + DEFAULT_MINIMUM_STOCK);
        }
        
        if (minimumStockInput != null) {
            minimumStockInput.setText(String.valueOf(DEFAULT_MINIMUM_STOCK));
        }
        
        if (currentExpiryNotificationDaysText != null) {
            currentExpiryNotificationDaysText.setText("Current Threshold: " + DEFAULT_EXPIRY_NOTIFICATION_MONTHS + " month");
        }
        
        if (expiryNotificationDaysInput != null) {
            expiryNotificationDaysInput.setText(String.valueOf(DEFAULT_EXPIRY_NOTIFICATION_MONTHS));
        }
        
        showToast("✅ Settings reset to default\nMinimum stock: " + DEFAULT_MINIMUM_STOCK + "\nExpiring soon threshold: " + DEFAULT_EXPIRY_NOTIFICATION_MONTHS + " month");
    }

    /**
     * Test Firebase sync by creating a test medicine
     */
    private void testFirebaseSync() {
        if (getContext() == null) {
            showToast("❌ Error: Context is null");
            return;
        }

        try {
            HCasDatabaseHelper dbHelper = new HCasDatabaseHelper(getContext());
            
            // Create a test medicine
            Medicine testMedicine = new Medicine();
            String testId = "TEST_" + System.currentTimeMillis();
            testMedicine.setMedicineId(testId);
            testMedicine.setMedicineName("Test Medicine - Firebase Sync");
            testMedicine.setDosage("500mg");
            testMedicine.setStockQuantity(100);
            testMedicine.setUnit("tablets");
            testMedicine.setCategory("Test");
            testMedicine.setDescription("This is a test medicine to verify Firebase sync");
            testMedicine.setExpiryDate("2025-12-31");
            testMedicine.setPrice(10.00);
            testMedicine.setSupplier("Test Supplier");
            
            // Add to database (this will trigger Firebase sync)
            boolean success = dbHelper.addMedicine(testMedicine);
            
            if (success) {
                showToast("✅ Test medicine added!\n\nCheck Firebase Console:\n1. Go to Firestore Database\n2. Look for 'medicines' collection\n3. Find document ID: " + testId + "\n\nOr check Logcat for sync status");
                Log.d("FirebaseTest", "✅ Test medicine created: " + testId);
                Log.d("FirebaseTest", "   Medicine Name: " + testMedicine.getMedicineName());
                Log.d("FirebaseTest", "   Check Firebase Console → Firestore → medicines collection");
            } else {
                showToast("❌ Failed to add test medicine");
            }
            
        } catch (Exception e) {
            Log.e("FirebaseTest", "Error testing Firebase sync", e);
            showToast("❌ Error: " + e.getMessage());
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Get the minimum stock quantity from SharedPreferences
     * This is a static utility method that can be used by other classes
     */
    public static int getMinimumStockQuantity(Context context) {
        if (context == null) {
            return DEFAULT_MINIMUM_STOCK;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_MINIMUM_STOCK_QUANTITY, DEFAULT_MINIMUM_STOCK);
    }

    /**
     * Get the expiring soon threshold months from SharedPreferences
     * Medicines expiring within this number of months (or less) will be marked as "Expiring Soon"
     * This is a static utility method that can be used by other classes
     * @return Number of months before expiry to mark as expiring soon
     */
    public static int getExpiryNotificationMonths(Context context) {
        if (context == null) {
            return DEFAULT_EXPIRY_NOTIFICATION_MONTHS;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_EXPIRY_NOTIFICATION_MONTHS, DEFAULT_EXPIRY_NOTIFICATION_MONTHS);
    }

    /**
     * Get the expiring soon threshold days from SharedPreferences (for backward compatibility)
     * Converts months to days (approximately 30 days per month)
     * @return Number of days before expiry to mark as expiring soon
     */
    public static int getExpiryNotificationDays(Context context) {
        int months = getExpiryNotificationMonths(context);
        return months * 30; // Approximate conversion: 1 month = 30 days
    }
}

