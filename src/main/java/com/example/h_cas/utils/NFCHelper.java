package com.example.h_cas.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * NFCHelper handles NFC tag reading and writing operations using Android's built-in NFC
 * This replaces RFID functionality with standard NFC that works on all modern smartphones
 */
public class NFCHelper {
    
    private static final String TAG = "NFCHelper";
    private Context context;
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFilters;
    private String[][] techLists;
    private NFCScanListener scanListener;
    
    public interface NFCScanListener {
        void onNFCTagDetected(String nfcUid, android.nfc.Tag tag);
        void onNFCWriteSuccess(String nfcUid);
        void onNFCWriteError(String error);
        void onNFCReadSuccess(String nfcUid, String data);
        void onNFCReadError(String error);
    }
    
    public NFCHelper(Context context) {
        this.context = context;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        initializeNFC();
    }
    
    /**
     * Initialize NFC adapter and set up intent filters
     */
    private void initializeNFC() {
        if (nfcAdapter == null) {
            Log.w(TAG, "NFC adapter is null - device may not support NFC");
            return;
        }
        
        // Create pending intent for NFC tag detection
        Intent intent = new Intent(context, context.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_MUTABLE
        );
        
        // Set up intent filters for different NFC tag types
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter tag = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Log.e(TAG, "Error setting up NFC intent filter", e);
        }
        
        intentFilters = new IntentFilter[] { ndef, tag, tech };
        
        // Tech lists for different NFC tag technologies
        techLists = new String[][] {
            new String[] { Ndef.class.getName() },
            new String[] { NfcA.class.getName() },
            new String[] { NfcB.class.getName() },
            new String[] { NfcF.class.getName() },
            new String[] { NfcV.class.getName() }
        };
    }
    
    /**
     * Check if NFC is available on the device
     */
    public boolean isNFCAvailable() {
        return nfcAdapter != null;
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
            return "NFC is ready for scanning";
        }
    }
    
    /**
     * Enable NFC foreground dispatch (call this in onResume)
     */
    public void enableForegroundDispatch(Activity activity) {
        if (nfcAdapter != null && nfcAdapter.isEnabled() && pendingIntent != null) {
            try {
                nfcAdapter.enableForegroundDispatch(activity, pendingIntent, intentFilters, techLists);
                Log.d(TAG, "NFC foreground dispatch enabled");
            } catch (Exception e) {
                Log.e(TAG, "Error enabling NFC foreground dispatch", e);
            }
        }
    }
    
    /**
     * Disable NFC foreground dispatch (call this in onPause)
     */
    public void disableForegroundDispatch(Activity activity) {
        if (nfcAdapter != null && pendingIntent != null) {
            try {
                nfcAdapter.disableForegroundDispatch(activity);
                Log.d(TAG, "NFC foreground dispatch disabled");
            } catch (Exception e) {
                Log.e(TAG, "Error disabling NFC foreground dispatch", e);
            }
        }
    }
    
    /**
     * Handle NFC intent (call this in onNewIntent)
     */
    public void handleNFCIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                String nfcUid = getTagId(tag);
                Log.d(TAG, "NFC tag detected: " + nfcUid);
                
                if (scanListener != null) {
                    scanListener.onNFCTagDetected(nfcUid, tag);
                }
            }
        }
    }
    
    /**
     * Get NFC tag UID from Tag object
     */
    public String getTagId(Tag tag) {
        if (tag == null) {
            return null;
        }
        
        byte[] id = tag.getId();
        if (id == null || id.length == 0) {
            return null;
        }
        
        // Convert byte array to hex string
        StringBuilder hex = new StringBuilder();
        for (byte b : id) {
            hex.append(String.format("%02X", b));
        }
        
        return hex.toString();
    }
    
    /**
     * Read data from NFC tag
     */
    public String readNFCTag(Tag tag) {
        if (tag == null) {
            return null;
        }
        
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            Log.d(TAG, "Tag does not support NDEF");
            return null;
        }
        
        try {
            ndef.connect();
            NdefMessage ndefMessage = ndef.getNdefMessage();
            
            if (ndefMessage != null) {
                NdefRecord[] records = ndefMessage.getRecords();
                if (records.length > 0) {
                    byte[] payload = records[0].getPayload();
                    // Skip first byte (status byte)
                    if (payload.length > 1) {
                        return new String(payload, 1, payload.length - 1, StandardCharsets.UTF_8);
                    }
                }
            }
            
            ndef.close();
        } catch (IOException | FormatException e) {
            Log.e(TAG, "Error reading NFC tag", e);
            if (scanListener != null) {
                scanListener.onNFCReadError("Error reading NFC tag: " + e.getMessage());
            }
            return null;
        }
        
        return null;
    }
    
    /**
     * Write data to NFC tag
     */
    public boolean writeNFCTag(Tag tag, String data) {
        if (tag == null || data == null || data.isEmpty()) {
            return false;
        }
        
        NdefMessage message = createNdefMessage(data);
        if (message == null) {
            return false;
        }
        
        Ndef ndef = Ndef.get(tag);
        
        try {
            if (ndef != null) {
                // Tag is already formatted
                ndef.connect();
                if (!ndef.isWritable()) {
                    Log.e(TAG, "Tag is not writable");
                    if (scanListener != null) {
                        scanListener.onNFCWriteError("Tag is not writable");
                    }
                    return false;
                }
                
                int size = message.getByteArrayLength();
                if (ndef.getMaxSize() < size) {
                    Log.e(TAG, "Tag is too small for data");
                    if (scanListener != null) {
                        scanListener.onNFCWriteError("Tag is too small for data");
                    }
                    return false;
                }
                
                ndef.writeNdefMessage(message);
                ndef.close();
                
                String nfcUid = getTagId(tag);
                if (scanListener != null) {
                    scanListener.onNFCWriteSuccess(nfcUid);
                }
                
                Log.d(TAG, "Successfully wrote data to NFC tag: " + nfcUid);
                return true;
            } else {
                // Tag is not formatted, try to format it
                NdefFormatable formatable = NdefFormatable.get(tag);
                if (formatable != null) {
                    formatable.connect();
                    formatable.format(message);
                    formatable.close();
                    
                    String nfcUid = getTagId(tag);
                    if (scanListener != null) {
                        scanListener.onNFCWriteSuccess(nfcUid);
                    }
                    
                    Log.d(TAG, "Successfully formatted and wrote data to NFC tag: " + nfcUid);
                    return true;
                } else {
                    Log.e(TAG, "Tag is not NDEF formattable");
                    if (scanListener != null) {
                        scanListener.onNFCWriteError("Tag is not NDEF formattable");
                    }
                    return false;
                }
            }
        } catch (IOException | FormatException e) {
            Log.e(TAG, "Error writing to NFC tag", e);
            if (scanListener != null) {
                scanListener.onNFCWriteError("Error writing to NFC tag: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Create NDEF message from text data
     */
    private NdefMessage createNdefMessage(String text) {
        try {
            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
            NdefRecord textRecord = new NdefRecord(
                NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT,
                new byte[0],
                textBytes
            );
            return new NdefMessage(new NdefRecord[] { textRecord });
        } catch (Exception e) {
            Log.e(TAG, "Error creating NDEF message", e);
            return null;
        }
    }
    
    /**
     * Format NFC UID for display
     */
    public String formatNFCUid(String uid) {
        if (uid == null || uid.isEmpty()) {
            return "No NFC tag detected";
        }
        
        // Format as hex string with colons for readability
        if (uid.length() > 8) {
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < uid.length(); i += 2) {
                if (i > 0) {
                    formatted.append(":");
                }
                if (i + 2 <= uid.length()) {
                    formatted.append(uid.substring(i, i + 2));
                } else {
                    formatted.append(uid.substring(i));
                }
            }
            return formatted.toString();
        }
        
        return uid;
    }
    
    /**
     * Validate NFC UID format
     */
    public boolean isValidNFCUid(String uid) {
        if (uid == null || uid.isEmpty()) {
            return false;
        }
        
        // Check if it's a valid hex format (remove colons if present)
        String cleanUid = uid.replace(":", "").replace("-", "").replace(" ", "");
        return cleanUid.matches("^[0-9A-Fa-f]{4,}$");
    }
    
    /**
     * Get NFC tag type from detected tag
     */
    public String getTagType(Tag tag) {
        if (tag == null) {
            return "Unknown";
        }
        
        String[] techList = tag.getTechList();
        for (String tech : techList) {
            switch (tech) {
                case "android.nfc.tech.NfcA":
                    return "ISO14443A (Mifare)";
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
     * Set NFC scan listener
     */
    public void setNFCScanListener(NFCScanListener listener) {
        this.scanListener = listener;
    }
    
    /**
     * Show NFC status toast
     */
    public void showNFCStatus() {
        String message = getNFCStatusMessage();
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}

