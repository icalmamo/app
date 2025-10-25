package com.example.h_cas.utils;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * RFIDHelper handles RFID card scanning and writing operations
 */
public class RFIDHelper {
    
    private Context context;
    private NfcAdapter nfcAdapter;
    private RFIDScanListener scanListener;
    
    public interface RFIDScanListener {
        void onRFIDCardDetected(String cardId);
        void onRFIDWriteSuccess(String cardId);
        void onRFIDWriteError(String error);
    }
    
    public RFIDHelper(Context context) {
        this.context = context;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(context);
    }
    
    /**
     * Check if NFC is available on the device
     */
    public boolean isNFCAvailable() {
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }
    
    /**
     * Check if NFC is enabled
     */
    public boolean isNFCEnabled() {
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }
    
    /**
     * Get NFC status message
     */
    public String getNFCStatusMessage() {
        if (nfcAdapter == null) {
            return "NFC is not available on this device";
        } else if (!nfcAdapter.isEnabled()) {
            return "NFC is disabled. Please enable NFC in settings";
        } else {
            return "NFC is ready for RFID card scanning";
        }
    }
    
    /**
     * Simulate RFID card detection (for testing without actual hardware)
     */
    public String simulateRFIDCardScan() {
        // Simulate a realistic RFID card ID
        // Real RFID cards have specific ID formats like:
        // - UID format: 04:12:34:56:78:90:AB
        // - ISO14443A format: 04 12 34 56 78 90 AB
        // - Mifare format: 04:12:34:56
        
        long timestamp = System.currentTimeMillis();
        int random = (int)(Math.random() * 10000);
        
        // Format as realistic RFID UID
        String cardId = String.format("04:%02X:%02X:%02X:%02X", 
            (int)(timestamp % 256),
            (int)(random % 256), 
            (int)((timestamp / 1000) % 256),
            (int)((random / 100) % 256));
            
        return cardId;
    }
    
    /**
     * Simulate writing data to RFID card
     */
    public boolean simulateWriteToRFID(String cardId, String prescriptionData) {
        // In a real implementation, this would write to the actual RFID card
        // For simulation, we just return success
        try {
            // Simulate write delay
            Thread.sleep(1000);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
    
    /**
     * Get RFID card type from detected tag
     */
    public String getCardType(Tag tag) {
        if (tag == null) return "Unknown";
        
        String[] techList = tag.getTechList();
        for (String tech : techList) {
            switch (tech) {
                case "android.nfc.tech.NfcA":
                    return "ISO14443A";
                case "android.nfc.tech.NfcB":
                    return "ISO14443B";
                case "android.nfc.tech.NfcF":
                    return "FeliCa";
                case "android.nfc.tech.NfcV":
                    return "ISO15693";
                case "android.nfc.tech.Ndef":
                    return "NDEF";
                default:
                    return "Unknown";
            }
        }
        return "Unknown";
    }
    
    /**
     * Format RFID card ID for display
     */
    public String formatCardId(String cardId) {
        if (cardId == null || cardId.isEmpty()) {
            return "No card detected";
        }
        
        // Format as hex string with colons
        if (cardId.length() > 8) {
            return cardId.substring(0, 8) + "...";
        }
        
        return cardId;
    }
    
    /**
     * Validate RFID card ID format
     */
    public boolean isValidCardId(String cardId) {
        if (cardId == null || cardId.isEmpty()) {
            return false;
        }
        
        // Check if it's a valid hex format
        return cardId.matches("^[0-9A-Fa-f:]+$");
    }
    
    /**
     * Show NFC status toast
     */
    public void showNFCStatus() {
        String message = getNFCStatusMessage();
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
