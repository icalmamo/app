package com.healthcare.cas.utils;

import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Map;

/**
 * RFIDHelper handles RFID tag scanning via ESP32 through Firebase Realtime Database
 * Listens to /HCAS/rfid_scans/latest for new RFID scans from ESP32
 */
public class RFIDHelper {
    
    private static final String TAG = "RFIDHelper";
    private static final String RFID_SCANS_PATH = "HCAS/rfid_scans/latest";
    private static final String RFID_TAGS_PATH = "HCAS/rfid_tags";
    
    private DatabaseReference rfidScansRef;
    private ValueEventListener scanListener;
    private long lastScanId = -1;
    private long lastTimestamp = 0;
    private long listenerStartTime = 0; // Track when listener started
    private boolean isFirstDataLoad = true; // Track first data load
    private boolean pathWasEmpty = false; // Track if path was empty when listener started
    private RFIDScanListener scanListenerCallback;
    
    public interface RFIDScanListener {
        void onRFIDTagScanned(String rfidUid);
        void onRFIDScanError(String error);
    }
    
    public RFIDHelper() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/");
        rfidScansRef = database.getReference(RFID_SCANS_PATH);
    }
    
    /**
     * Start listening for RFID scans from ESP32
     * @param listener Callback when RFID tag is detected
     */
    public void startListening(RFIDScanListener listener) {
        if (listener == null) {
            Log.e(TAG, "❌ RFIDScanListener is null");
            return;
        }
        
        this.scanListenerCallback = listener;
        
        // Remove existing listener if any
        stopListening();
        
        // Reset tracking
        listenerStartTime = System.currentTimeMillis();
        isFirstDataLoad = true;
        
        Log.d(TAG, "🔍 Starting RFID scan listener on path: " + RFID_SCANS_PATH);
        
        scanListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot == null || !snapshot.exists()) {
                    // Mark that path was empty when we started listening
                    if (isFirstDataLoad) {
                        pathWasEmpty = true;
                        Log.d(TAG, "⚠️ Path is empty - will accept first data received as new scan");
                    } else {
                        Log.d(TAG, "⚠️ Snapshot is null or doesn't exist - waiting for ESP32 to write data...");
                    }
                    return;
                }
                
                try {
                    Map<String, Object> scanData = (Map<String, Object>) snapshot.getValue();
                    if (scanData == null) {
                        Log.d(TAG, "⚠️ Scan data is null");
                        return;
                    }
                    
                    // Log all received data for debugging
                    Log.d(TAG, "📥 Received scan data: " + scanData.toString());
                    
                    // Handle first data load
                    if (isFirstDataLoad) {
                        isFirstDataLoad = false;
                        
                        // If path was empty, accept this first data as a new scan
                        if (pathWasEmpty) {
                            Log.d(TAG, "✅✅✅ Path was empty - accepting first data as new scan!");
                            
                            // Extract UID
                            String rfidUid = null;
                            if (scanData.containsKey("uid")) {
                                rfidUid = String.valueOf(scanData.get("uid"));
                            } else if (scanData.containsKey("card")) {
                                rfidUid = String.valueOf(scanData.get("card"));
                            } else if (scanData.containsKey("tag")) {
                                rfidUid = String.valueOf(scanData.get("tag"));
                            } else if (scanData.containsKey("value")) {
                                rfidUid = String.valueOf(scanData.get("value"));
                            } else if (scanData.containsKey("rfid")) {
                                rfidUid = String.valueOf(scanData.get("rfid"));
                            }
                            
                            if (rfidUid != null && !rfidUid.isEmpty()) {
                                rfidUid = rfidUid.toUpperCase().trim();
                                
                                // Store current values for future scans
                                if (scanData.containsKey("scan_id")) {
                                    Object scanIdObj = scanData.get("scan_id");
                                    if (scanIdObj instanceof Number) {
                                        lastScanId = ((Number) scanIdObj).longValue();
                                    }
                                }
                                if (scanData.containsKey("scan_time")) {
                                    Object scanTimeObj = scanData.get("scan_time");
                                    if (scanTimeObj instanceof Number) {
                                        lastTimestamp = ((Number) scanTimeObj).longValue();
                                    }
                                } else if (scanData.containsKey("timestamp")) {
                                    Object timestampObj = scanData.get("timestamp");
                                    if (timestampObj instanceof Number) {
                                        lastTimestamp = ((Number) timestampObj).longValue();
                                    }
                                }
                                
                                Log.d(TAG, "✅✅✅ NEW RFID TAG DETECTED (first data after empty path)! ✅✅✅");
                                Log.d(TAG, "  UID: " + rfidUid);
                                
                                if (scanListenerCallback != null) {
                                    Log.d(TAG, "📞 Calling onRFIDTagScanned callback with UID: " + rfidUid);
                                    scanListenerCallback.onRFIDTagScanned(rfidUid);
                                }
                                return; // Processed the scan
                            }
                        } else {
                            // Path had existing data - skip it and wait for new scan
                            Log.d(TAG, "⏭️ Skipping initial data load, waiting for new scan...");
                            
                            // Store current values to detect new scans
                            if (scanData.containsKey("scan_id")) {
                                Object scanIdObj = scanData.get("scan_id");
                                if (scanIdObj instanceof Number) {
                                    lastScanId = ((Number) scanIdObj).longValue();
                                }
                            }
                            if (scanData.containsKey("timestamp")) {
                                Object timestampObj = scanData.get("timestamp");
                                if (timestampObj instanceof Number) {
                                    lastTimestamp = ((Number) timestampObj).longValue();
                                }
                            }
                            if (scanData.containsKey("scan_time")) {
                                Object scanTimeObj = scanData.get("scan_time");
                                if (scanTimeObj instanceof Number) {
                                    // Use scan_time (millis) for more precise tracking
                                    lastTimestamp = ((Number) scanTimeObj).longValue();
                                }
                            }
                            
                            Log.d(TAG, "📌 Initial state - Scan ID: " + lastScanId + ", Timestamp: " + lastTimestamp);
                            Log.d(TAG, "✅ Now listening for new scans...");
                            
                            // Extract UID from initial data for logging
                            String initialUid = null;
                            if (scanData.containsKey("uid")) {
                                initialUid = String.valueOf(scanData.get("uid"));
                            } else if (scanData.containsKey("card")) {
                                initialUid = String.valueOf(scanData.get("card"));
                            } else if (scanData.containsKey("tag")) {
                                initialUid = String.valueOf(scanData.get("tag"));
                            } else if (scanData.containsKey("value")) {
                                initialUid = String.valueOf(scanData.get("value"));
                            } else if (scanData.containsKey("rfid")) {
                                initialUid = String.valueOf(scanData.get("rfid"));
                            }
                            if (initialUid != null && !initialUid.isEmpty()) {
                                Log.d(TAG, "📌 Initial UID found: " + initialUid + " (will ignore this, waiting for new scan)");
                            }
                            
                            return; // Don't process initial data
                        }
                    }
                    
                    // Extract RFID UID from various possible fields
                    String rfidUid = null;
                    if (scanData.containsKey("uid")) {
                        rfidUid = String.valueOf(scanData.get("uid"));
                    } else if (scanData.containsKey("card")) {
                        rfidUid = String.valueOf(scanData.get("card"));
                    } else if (scanData.containsKey("tag")) {
                        rfidUid = String.valueOf(scanData.get("tag"));
                    } else if (scanData.containsKey("value")) {
                        rfidUid = String.valueOf(scanData.get("value"));
                    } else if (scanData.containsKey("rfid")) {
                        rfidUid = String.valueOf(scanData.get("rfid"));
                    }
                    
                    // Get scan ID and timestamp to detect new scans
                    long currentScanId = -1;
                    long currentTimestamp = 0;
                    long currentScanTime = 0;
                    
                    if (scanData.containsKey("scan_id")) {
                        Object scanIdObj = scanData.get("scan_id");
                        if (scanIdObj instanceof Number) {
                            currentScanId = ((Number) scanIdObj).longValue();
                        }
                    }
                    
                    if (scanData.containsKey("timestamp")) {
                        Object timestampObj = scanData.get("timestamp");
                        if (timestampObj instanceof Number) {
                            currentTimestamp = ((Number) timestampObj).longValue();
                        }
                    }
                    
                    if (scanData.containsKey("scan_time")) {
                        Object scanTimeObj = scanData.get("scan_time");
                        if (scanTimeObj instanceof Number) {
                            currentScanTime = ((Number) scanTimeObj).longValue();
                        }
                    }
                    
                    // Check if this is a new scan (different scan_id or newer timestamp/scan_time)
                    boolean isNewScan = false;
                    String detectionReason = "";
                    
                    // Priority 1: Check scan_id (most reliable)
                    if (currentScanId > 0 && currentScanId != lastScanId) {
                        isNewScan = true;
                        detectionReason = "scan_id changed from " + lastScanId + " to " + currentScanId;
                        lastScanId = currentScanId;
                    }
                    // Priority 2: Check scan_time (millis, more precise)
                    else if (currentScanTime > 0 && currentScanTime > lastTimestamp) {
                        isNewScan = true;
                        detectionReason = "scan_time increased from " + lastTimestamp + " to " + currentScanTime;
                        lastTimestamp = currentScanTime;
                    }
                    // Priority 3: Check timestamp (seconds)
                    else if (currentTimestamp > 0 && currentTimestamp > lastTimestamp) {
                        isNewScan = true;
                        detectionReason = "timestamp increased from " + lastTimestamp + " to " + currentTimestamp;
                        lastTimestamp = currentTimestamp;
                    }
                    // Fallback: If we have a UID but no tracking data, accept it if it's been at least 2 seconds since listener started
                    // This is more lenient - accepts any scan after listener has been active
                    else if (rfidUid != null && !rfidUid.isEmpty() && (System.currentTimeMillis() - listenerStartTime) > 2000) {
                        // Accept scan if listener has been active for more than 2 seconds
                        // This handles cases where scan_id/timestamp don't change but a new tag is scanned
                        isNewScan = true;
                        detectionReason = "fallback: UID present and listener active for >2s";
                        Log.d(TAG, "⚠️ Using fallback detection - scan_id/timestamp unchanged but UID present");
                        Log.d(TAG, "  Time since listener started: " + (System.currentTimeMillis() - listenerStartTime) + "ms");
                    }
                    
                    // ULTRA FALLBACK: If no data exists initially and we get ANY data, accept it
                    // This handles the case where Firebase path is empty initially
                    else if (rfidUid != null && !rfidUid.isEmpty() && lastScanId == -1 && lastTimestamp == 0 && (System.currentTimeMillis() - listenerStartTime) > 500) {
                        isNewScan = true;
                        detectionReason = "ultra fallback: first data received after empty path";
                        Log.d(TAG, "🆕 ULTRA FALLBACK: First data received after empty path - accepting scan");
                    }
                    
                    Log.d(TAG, "🔍 Scan detection result:");
                    Log.d(TAG, "  isNewScan: " + isNewScan);
                    Log.d(TAG, "  rfidUid: " + rfidUid);
                    Log.d(TAG, "  currentScanId: " + currentScanId + " (last: " + lastScanId + ")");
                    Log.d(TAG, "  currentTimestamp: " + currentTimestamp + " (last: " + lastTimestamp + ")");
                    Log.d(TAG, "  currentScanTime: " + currentScanTime);
                    
                    if (isNewScan && rfidUid != null && !rfidUid.isEmpty()) {
                        // Convert to uppercase and remove any whitespace
                        rfidUid = rfidUid.toUpperCase().trim();
                        
                        Log.d(TAG, "✅✅✅ NEW RFID TAG DETECTED! ✅✅✅");
                        Log.d(TAG, "  Detection reason: " + detectionReason);
                        Log.d(TAG, "  UID: " + rfidUid);
                        Log.d(TAG, "  Scan ID: " + currentScanId);
                        Log.d(TAG, "  Timestamp: " + currentTimestamp);
                        Log.d(TAG, "  Scan Time: " + currentScanTime);
                        
                        if (scanListenerCallback != null) {
                            Log.d(TAG, "📞 Calling onRFIDTagScanned callback with UID: " + rfidUid);
                            scanListenerCallback.onRFIDTagScanned(rfidUid);
                        } else {
                            Log.e(TAG, "❌ scanListenerCallback is NULL!");
                        }
                    } else {
                        if (!isNewScan) {
                            Log.d(TAG, "⏭️ Skipping - not a new scan. Reason: " + (detectionReason.isEmpty() ? "no detection criteria met" : detectionReason));
                        }
                        if (rfidUid == null || rfidUid.isEmpty()) {
                            Log.w(TAG, "⚠️ No RFID UID found in scan data. Available keys: " + scanData.keySet());
                        }
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error processing RFID scan data: " + e.getMessage(), e);
                    if (scanListenerCallback != null) {
                        scanListenerCallback.onRFIDScanError("Error processing scan: " + e.getMessage());
                    }
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "❌ RFID scan listener cancelled: " + error.getMessage());
                if (scanListenerCallback != null) {
                    scanListenerCallback.onRFIDScanError("Listener cancelled: " + error.getMessage());
                }
            }
        };
        
        rfidScansRef.addValueEventListener(scanListener);
        Log.d(TAG, "✅ RFID scan listener attached");
    }
    
    /**
     * Stop listening for RFID scans
     */
    public void stopListening() {
        if (scanListener != null && rfidScansRef != null) {
            rfidScansRef.removeEventListener(scanListener);
            scanListener = null;
            Log.d(TAG, "🛑 RFID scan listener removed");
        }
    }
    
    /**
     * Get reference to RFID tags path for storing/reading prescription data
     * @param rfidUid The RFID UID
     * @return DatabaseReference to the RFID tag's path
     */
    public DatabaseReference getRFIDTagRef(String rfidUid) {
        if (rfidUid == null || rfidUid.isEmpty()) {
            return null;
        }
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/");
        return database.getReference(RFID_TAGS_PATH).child(rfidUid);
    }
    
    /**
     * Reset scan tracking (useful when starting a new scan session)
     */
    public void resetScanTracking() {
        lastScanId = -1;
        lastTimestamp = 0;
        listenerStartTime = 0;
        isFirstDataLoad = true;
        pathWasEmpty = false;
        Log.d(TAG, "🔄 RFID scan tracking reset");
    }
    
    /**
     * Test method: Read current value from Firebase to debug
     */
    public void testReadCurrentValue() {
        if (rfidScansRef == null) {
            Log.e(TAG, "❌ rfidScansRef is null");
            return;
        }
        
        Log.d(TAG, "🧪 Testing read from Firebase path: " + RFID_SCANS_PATH);
        Log.d(TAG, "🧪 Full Firebase URL: https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/" + RFID_SCANS_PATH);
        
        rfidScansRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot == null || !snapshot.exists()) {
                    Log.w(TAG, "⚠️⚠️⚠️ NO DATA FOUND at path: " + RFID_SCANS_PATH);
                    Log.w(TAG, "⚠️ This means ESP32 hasn't written any data yet or path is wrong");
                    Log.w(TAG, "⚠️ Check ESP32 Serial Monitor to see if it's scanning and writing");
                    Log.w(TAG, "⚠️ Check Firebase Console at: https://console.firebase.google.com/project/hcas-c83fa/database/hcas-c83fa-default-rtdb/data/~2FHCAS~2Frfid_scans~2Flatest");
                    return;
                }
                
                Object value = snapshot.getValue();
                Log.d(TAG, "📊✅ DATA FOUND at Firebase path!");
                Log.d(TAG, "  Full data: " + value.toString());
                
                if (value instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) value;
                    Log.d(TAG, "  Keys: " + data.keySet());
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        Log.d(TAG, "    " + entry.getKey() + " = " + entry.getValue());
                    }
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "❌ Test read cancelled: " + error.getMessage());
                Log.e(TAG, "  Error code: " + error.getCode());
                Log.e(TAG, "  Error details: " + error.getDetails());
            }
        });
    }
}
