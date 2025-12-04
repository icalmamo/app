package com.example.h_cas;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

/**
 * LoadingActivity displays the loading screen after successful login
 * and then navigates to the appropriate dashboard based on user role
 */
public class LoadingActivity extends AppCompatActivity {

    private static final int LOADING_DELAY = 1500; // 1.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_splash);

            // Get employee data from intent
            Intent intent = getIntent();
            if (intent == null) {
                // If no intent data, go back to login
                finish();
                return;
            }
            
            String role = intent.getStringExtra("ROLE");
            boolean isAdmin = intent.getBooleanExtra("IS_ADMIN", false);

            // Delay before navigating to dashboard
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Intent dashboardIntent = createDashboardIntent(role, isAdmin, intent);
                    
                    if (dashboardIntent != null) {
                        // Pass all employee data to dashboard
                        copyEmployeeExtras(intent, dashboardIntent);
                        
                        // Clear task and navigate
                        dashboardIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(dashboardIntent);
                        finish();
                    } else {
                        // Fallback: navigate to MainActivity if dashboard intent creation fails
                        Intent fallbackIntent = new Intent(LoadingActivity.this, MainActivity.class);
                        fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(fallbackIntent);
                        finish();
                    }
                } catch (Exception e) {
                    // Log error and navigate to login screen
                    e.printStackTrace();
                    Intent loginIntent = new Intent(LoadingActivity.this, LoginActivity.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loginIntent);
                    finish();
                }
            }, LOADING_DELAY);
        } catch (Exception e) {
            e.printStackTrace();
            // If setup fails, go back to login
            Intent loginIntent = new Intent(LoadingActivity.this, LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
        }
    }

    /**
     * Creates the appropriate dashboard intent based on user role
     * @param role The user's role
     * @param isAdmin Whether the user is an admin
     * @param originalIntent The original intent with employee data
     * @return Intent for the appropriate dashboard
     */
    private Intent createDashboardIntent(String role, boolean isAdmin, Intent originalIntent) {
        Intent intent;

        if (isAdmin) {
            intent = new Intent(LoadingActivity.this, AdminDashboardActivity.class);
        } else {
            // Route to role-specific dashboard
            switch (role != null ? role : "") {
                case "Doctor":
                    intent = new Intent(LoadingActivity.this, DoctorDashboardActivity.class);
                    break;
                case "Nurse":
                    intent = new Intent(LoadingActivity.this, NurseDashboardActivity.class);
                    break;
                case "Pharmacist":
                    intent = new Intent(LoadingActivity.this, PharmacistDashboardActivity.class);
                    break;
                default:
                    intent = new Intent(LoadingActivity.this, MainActivity.class);
                    break;
            }
        }

        return intent;
    }

    /**
     * Copies all employee-related extras from source intent to destination intent
     * @param source The source intent
     * @param destination The destination intent
     */
    private void copyEmployeeExtras(Intent source, Intent destination) {
        try {
            if (source != null && destination != null) {
                if (source.hasExtra("EMPLOYEE_ID")) {
                    String value = source.getStringExtra("EMPLOYEE_ID");
                    if (value != null) {
                        destination.putExtra("EMPLOYEE_ID", value);
                    }
                }
                if (source.hasExtra("FIRST_NAME")) {
                    String value = source.getStringExtra("FIRST_NAME");
                    if (value != null) {
                        destination.putExtra("FIRST_NAME", value);
                    }
                }
                if (source.hasExtra("LAST_NAME")) {
                    String value = source.getStringExtra("LAST_NAME");
                    if (value != null) {
                        destination.putExtra("LAST_NAME", value);
                    }
                }
                if (source.hasExtra("FULL_NAME")) {
                    String value = source.getStringExtra("FULL_NAME");
                    if (value != null) {
                        destination.putExtra("FULL_NAME", value);
                    }
                }
                if (source.hasExtra("USERNAME")) {
                    String value = source.getStringExtra("USERNAME");
                    if (value != null) {
                        destination.putExtra("USERNAME", value);
                    }
                }
                if (source.hasExtra("ROLE")) {
                    String value = source.getStringExtra("ROLE");
                    if (value != null) {
                        destination.putExtra("ROLE", value);
                    }
                }
                if (source.hasExtra("EMAIL")) {
                    String value = source.getStringExtra("EMAIL");
                    if (value != null) {
                        destination.putExtra("EMAIL", value);
                    }
                }
                // Copy IS_ADMIN flag
                if (source.hasExtra("IS_ADMIN")) {
                    destination.putExtra("IS_ADMIN", source.getBooleanExtra("IS_ADMIN", false));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Continue even if copying extras fails
        }
    }
}

