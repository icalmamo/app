package com.healthcare.cas.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Utility class for image conversion and handling
 * Converts images to base64 strings for storage in Firebase Realtime Database
 */
public class ImageUtils {
    private static final String TAG = "ImageUtils";
    
    // Maximum image size in KB before compression (1MB = 1024KB)
    private static final int MAX_IMAGE_SIZE_KB = 1024;
    
    // Quality for JPEG compression (0-100)
    private static final int JPEG_QUALITY = 80;
    
    /**
     * Convert image URI to base64 string
     * @param context Android context
     * @param imageUri URI of the image
     * @return Base64 encoded string or null if conversion fails
     */
    public static String convertImageUriToBase64(Context context, Uri imageUri) {
        if (context == null || imageUri == null) {
            Log.e(TAG, "❌ Context or imageUri is null");
            return null;
        }
        
        try {
            // Load bitmap from URI
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "❌ Failed to open input stream from URI");
                return null;
            }
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            if (bitmap == null) {
                Log.e(TAG, "❌ Failed to decode bitmap from URI");
                return null;
            }
            
            // Compress and convert to base64
            return convertBitmapToBase64(bitmap);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error converting image URI to base64: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Convert bitmap to base64 string with compression
     * @param bitmap The bitmap to convert
     * @return Base64 encoded string or null if conversion fails
     */
    public static String convertBitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "❌ Bitmap is null");
            return null;
        }
        
        try {
            // Compress bitmap to reduce size
            Bitmap compressedBitmap = compressBitmap(bitmap);
            
            // Convert to byte array
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            
            // Convert to base64 string
            String base64String = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            
            // Log size for debugging
            int sizeInKB = base64String.length() / 1024;
            Log.d(TAG, "✅ Image converted to base64. Size: " + sizeInKB + " KB");
            
            return base64String;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error converting bitmap to base64: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Compress bitmap to reduce file size
     * @param bitmap Original bitmap
     * @return Compressed bitmap
     */
    private static Bitmap compressBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        
        // Calculate current size
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] imageBytes = stream.toByteArray();
        int sizeInKB = imageBytes.length / 1024;
        
        Log.d(TAG, "📊 Original image size: " + sizeInKB + " KB");
        
        // If image is already small enough, return as is
        if (sizeInKB <= MAX_IMAGE_SIZE_KB) {
            return bitmap;
        }
        
        // Calculate scale factor to reduce size
        float scaleFactor = (float) MAX_IMAGE_SIZE_KB / sizeInKB;
        scaleFactor = (float) Math.sqrt(scaleFactor); // Square root because we're scaling width and height
        
        // Limit minimum scale to 0.1 (at least 10% of original size)
        scaleFactor = Math.max(scaleFactor, 0.1f);
        
        // Limit maximum scale to 1.0 (don't enlarge)
        scaleFactor = Math.min(scaleFactor, 1.0f);
        
        int newWidth = (int) (bitmap.getWidth() * scaleFactor);
        int newHeight = (int) (bitmap.getHeight() * scaleFactor);
        
        Log.d(TAG, "📊 Scaling image from " + bitmap.getWidth() + "x" + bitmap.getHeight() + 
                   " to " + newWidth + "x" + newHeight);
        
        // Scale and compress
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        
        // Verify compressed size
        ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, compressedStream);
        byte[] compressedBytes = compressedStream.toByteArray();
        int compressedSizeKB = compressedBytes.length / 1024;
        
        Log.d(TAG, "📊 Compressed image size: " + compressedSizeKB + " KB");
        
        return scaledBitmap;
    }
    
    /**
     * Convert base64 string back to bitmap
     * @param base64String Base64 encoded image string
     * @return Bitmap or null if conversion fails
     */
    public static Bitmap convertBase64ToBitmap(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            Log.e(TAG, "❌ Base64 string is null or empty");
            return null;
        }
        
        try {
            // Remove data URL prefix if present (e.g., "data:image/jpeg;base64,")
            String base64Image = base64String;
            if (base64String.contains(",")) {
                base64Image = base64String.substring(base64String.indexOf(",") + 1);
            }
            
            // Decode base64 to byte array
            byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
            
            // Convert byte array to bitmap
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            
            if (bitmap == null) {
                Log.e(TAG, "❌ Failed to decode bitmap from base64");
                return null;
            }
            
            Log.d(TAG, "✅ Base64 converted to bitmap. Size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            return bitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error converting base64 to bitmap: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Check if string is a base64 encoded image
     * @param string String to check
     * @return true if string appears to be base64 encoded image
     */
    public static boolean isBase64Image(String string) {
        if (string == null || string.isEmpty()) {
            return false;
        }
        
        // Base64 strings are typically long and contain only base64 characters
        // Check if it's a data URL or just base64
        return string.length() > 100 && 
               (string.startsWith("data:image") || 
                string.matches("^[A-Za-z0-9+/=]+$"));
    }
    
    /**
     * Check if string is a URL
     * @param string String to check
     * @return true if string is a URL
     */
    public static boolean isUrl(String string) {
        if (string == null || string.isEmpty()) {
            return false;
        }
        
        return string.startsWith("http://") || string.startsWith("https://");
    }
}

