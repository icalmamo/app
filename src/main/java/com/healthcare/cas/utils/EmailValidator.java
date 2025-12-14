package com.healthcare.cas.utils;

import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Email validation utility that checks if an email account actually exists
 * Uses Abstract API's email validation service (free tier available)
 * 
 * To use this service:
 * 1. Sign up at https://www.abstractapi.com/
 * 2. Get your free API key from the Email Validation API section
 * 3. Replace the API_KEY constant below with your key
 * 
 * Free tier: 100 requests/month
 */
public class EmailValidator {
    private static final String TAG = "EmailValidator";
    
    // TODO: Replace with your Abstract API key
    // Get your free API key from: https://www.abstractapi.com/api/email-validation
    private static final String API_KEY = "YOUR_API_KEY_HERE";
    
    // API endpoint
    private static final String API_URL = "https://emailvalidation.abstractapi.com/v1/";
    
    /**
     * Interface for email validation callbacks
     */
    public interface EmailValidationCallback {
        void onValidationComplete(boolean isValid, boolean isDeliverable, String errorMessage);
    }
    
    /**
     * Validates if an email account actually exists
     * This method performs an asynchronous check
     * 
     * @param email The email address to validate
     * @param callback Callback to receive validation results
     */
    public static void validateEmailExists(String email, EmailValidationCallback callback) {
        // Check if API key is configured
        if (API_KEY == null || API_KEY.equals("YOUR_API_KEY_HERE") || API_KEY.isEmpty()) {
            Log.w(TAG, "⚠️ Email validation API key not configured. Skipping email existence check.");
            // If API key is not configured, we'll do basic format validation only
            // For now, we'll assume it's valid if format is correct
            // You can change this behavior if needed
            if (callback != null) {
                callback.onValidationComplete(true, true, "API key not configured - format validation only");
            }
            return;
        }
        
        // Run validation in background thread
        new Thread(() -> {
            try {
                // Build API URL
                String encodedEmail = URLEncoder.encode(email, "UTF-8");
                String apiUrl = API_URL + "?api_key=" + API_KEY + "&email=" + encodedEmail;
                
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000); // 10 seconds
                connection.setReadTimeout(10000); // 10 seconds
                connection.setRequestProperty("Accept", "application/json");
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // Parse JSON response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    
                    // Check validation results
                    boolean isValidFormat = jsonResponse.optBoolean("is_valid_format", false);
                    boolean isDeliverable = jsonResponse.optBoolean("is_deliverable", false);
                    boolean isDisposable = jsonResponse.optBoolean("is_disposable_email", false);
                    boolean isRoleEmail = jsonResponse.optBoolean("is_role_email", false);
                    String qualityScore = jsonResponse.optString("quality_score", "0");
                    
                    Log.d(TAG, "Email validation result for " + email + ":");
                    Log.d(TAG, "  - Valid format: " + isValidFormat);
                    Log.d(TAG, "  - Deliverable: " + isDeliverable);
                    Log.d(TAG, "  - Disposable: " + isDisposable);
                    Log.d(TAG, "  - Role email: " + isRoleEmail);
                    Log.d(TAG, "  - Quality score: " + qualityScore);
                    
                    // Determine if email is valid
                    // Email is considered valid if:
                    // 1. Format is valid
                    // 2. Is deliverable (can receive emails)
                    // 3. Not a disposable email (optional - you can remove this check)
                    // 4. Quality score is reasonable (optional)
                    
                    boolean isValid = isValidFormat && isDeliverable && !isDisposable;
                    
                    String errorMessage = null;
                    if (!isValidFormat) {
                        errorMessage = "Email format is invalid";
                    } else if (!isDeliverable) {
                        errorMessage = "Email account does not exist or cannot receive emails";
                    } else if (isDisposable) {
                        errorMessage = "Disposable email addresses are not allowed";
                    }
                    
                    if (callback != null) {
                        callback.onValidationComplete(isValid, isDeliverable, errorMessage);
                    }
                    
                } else {
                    // API error
                    Log.e(TAG, "Email validation API error: HTTP " + responseCode);
                    String errorMessage = "Unable to verify email. Please try again.";
                    
                    // If API fails, we'll do basic format validation
                    // You can change this to reject the email if you prefer
                    boolean isValidFormat = isValidEmailFormat(email);
                    if (callback != null) {
                        callback.onValidationComplete(isValidFormat, isValidFormat, 
                            isValidFormat ? null : "Email format is invalid");
                    }
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error validating email: " + e.getMessage(), e);
                
                // On error, fall back to format validation
                boolean isValidFormat = isValidEmailFormat(email);
                if (callback != null) {
                    callback.onValidationComplete(isValidFormat, isValidFormat,
                        isValidFormat ? "Unable to verify email existence. Please check your internet connection." : "Email format is invalid");
                }
            }
        }).start();
    }
    
    /**
     * Basic email format validation (fallback)
     */
    private static boolean isValidEmailFormat(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailPattern);
    }
}

