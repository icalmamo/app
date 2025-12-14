package com.healthcare.cas.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * Utility class to detect device performance capabilities
 * Used to adjust animation quality for optimal performance
 */
public class DevicePerformanceUtils {
    private static final String TAG = "DevicePerformance";
    
    // Performance tiers
    public enum PerformanceTier {
        LOW,      // Low-end devices - use simplified animations
        MEDIUM,   // Mid-range devices - use balanced animations
        HIGH      // High-end devices - use full animations
    }
    
    /**
     * Detect device performance tier based on RAM and other factors
     * @param context Android context
     * @return Performance tier (LOW, MEDIUM, or HIGH)
     */
    public static PerformanceTier getPerformanceTier(Context context) {
        try {
            // Get available RAM
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) {
                Log.w(TAG, "ActivityManager is null, defaulting to MEDIUM tier");
                return PerformanceTier.MEDIUM;
            }
            
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);
            
            // Convert bytes to MB
            long totalRAM = memInfo.totalMem / (1024 * 1024);
            
            // Check for low RAM flag (Android 6.0+)
            boolean isLowRamDevice = memInfo.lowMemory;
            
            Log.d(TAG, "Device RAM: " + totalRAM + " MB, Low RAM: " + isLowRamDevice);
            
            // Performance tier based on RAM:
            // LOW: < 3GB RAM or flagged as low RAM device
            // MEDIUM: 3GB - 6GB RAM
            // HIGH: > 6GB RAM
            if (isLowRamDevice || totalRAM < 3072) {
                Log.d(TAG, "Device classified as LOW performance tier");
                return PerformanceTier.LOW;
            } else if (totalRAM < 6144) {
                Log.d(TAG, "Device classified as MEDIUM performance tier");
                return PerformanceTier.MEDIUM;
            } else {
                Log.d(TAG, "Device classified as HIGH performance tier");
                return PerformanceTier.HIGH;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error detecting device performance: " + e.getMessage(), e);
            // Default to MEDIUM for safety
            return PerformanceTier.MEDIUM;
        }
    }
    
    /**
     * Check if device should use simplified animations
     * @param context Android context
     * @return true if simplified animations should be used
     */
    public static boolean shouldUseSimplifiedAnimation(Context context) {
        PerformanceTier tier = getPerformanceTier(context);
        return tier == PerformanceTier.LOW;
    }
    
    /**
     * Get animation quality level (0-2)
     * 0 = Simplified (low-end devices)
     * 1 = Balanced (mid-range devices)
     * 2 = Full quality (high-end devices)
     * @param context Android context
     * @return Animation quality level (0-2)
     */
    public static int getAnimationQuality(Context context) {
        PerformanceTier tier = getPerformanceTier(context);
        switch (tier) {
            case LOW:
                return 0;
            case MEDIUM:
                return 1;
            case HIGH:
                return 2;
            default:
                return 1; // Default to balanced
        }
    }
}

