package com.example.virtualcamera;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

/**
 * Key Generator Utility
 * 
 * This class provides a way to generate activation keys for users after payment.
 * Usage:
 * 1. Get the user's device ID (from the app or user input)
 * 2. Select the license type they purchased
 * 3. Generate a key using generateKey()
 * 4. Send the key to the user
 * 
 * Each key is cryptographically signed and tied to ONE device only.
 * The key cannot be used on any other device.
 */
public class KeyGenerator {
    
    // License types (must match LicenseManager)
    public static final String LICENSE_FREE_TRIAL = "free_trial";
    public static final String LICENSE_PRO_MONTHLY = "pro_monthly";
    public static final String LICENSE_PRO_YEARLY = "pro_yearly";
    public static final String LICENSE_LIFETIME = "lifetime";
    
    // Secret key for signing (CHANGE THIS IN PRODUCTION!)
    private static final String SECRET_KEY = "SECRET_KEY_2026_VC";
    
    /**
     * Generate an activation key for a specific device and license type
     * 
     * @param licenseType One of: free_trial, pro_monthly, pro_yearly, lifetime
     * @param deviceId The device ID from the user's app
     * @return The activation key string
     */
    public static String generateKey(String licenseType, String deviceId) {
        long expirationDate = calculateExpirationDate(licenseType);
        String signature = generateSignature(licenseType, deviceId, expirationDate);
        
        return "VC-" + licenseType + "-" + deviceId + "-" + expirationDate + "-" + signature;
    }
    
    /**
     * Calculate expiration date based on license type
     */
    private static long calculateExpirationDate(String licenseType) {
        Calendar calendar = Calendar.getInstance();
        
        switch (licenseType) {
            case LICENSE_FREE_TRIAL:
                calendar.add(Calendar.DAY_OF_YEAR, 7);
                break;
            case LICENSE_PRO_MONTHLY:
                calendar.add(Calendar.MONTH, 1);
                break;
            case LICENSE_PRO_YEARLY:
                calendar.add(Calendar.YEAR, 1);
                break;
            case LICENSE_LIFETIME:
                // Set to a far future date (99 years)
                calendar.add(Calendar.YEAR, 99);
                break;
            default:
                calendar.add(Calendar.DAY_OF_YEAR, 7);
        }
        
        return calendar.getTimeInMillis();
    }
    
    /**
     * Generate a cryptographic signature for the key
     */
    private static String generateSignature(String licenseType, String deviceId, long expirationDate) {
        try {
            String data = licenseType + "|" + deviceId + "|" + expirationDate + "|" + SECRET_KEY;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            return "INVALID";
        }
    }
    
    /**
     * Validate a key's signature
     */
    public static boolean validateKeySignature(String key) {
        try {
            String[] parts = key.split("-");
            if (parts.length != 5 || !parts[0].equals("VC")) {
                return false;
            }
            
            String licenseType = parts[1];
            String deviceId = parts[2];
            long expirationDate = Long.parseLong(parts[3]);
            String signature = parts[4];
            
            String expectedSignature = generateSignature(licenseType, deviceId, expirationDate);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the license type from a key
     */
    public static String getLicenseTypeFromKey(String key) {
        try {
            String[] parts = key.split("-");
            if (parts.length >= 2) {
                return parts[1];
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Get the device ID from a key
     */
    public static String getDeviceIdFromKey(String key) {
        try {
            String[] parts = key.split("-");
            if (parts.length >= 3) {
                return parts[2];
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Get the expiration date from a key
     */
    public static long getExpirationDateFromKey(String key) {
        try {
            String[] parts = key.split("-");
            if (parts.length >= 4) {
                return Long.parseLong(parts[3]);
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }
    
    /**
     * Format a license type for display
     */
    public static String formatLicenseType(String type) {
        if (type == null) return "Unknown";
        
        switch (type) {
            case LICENSE_FREE_TRIAL:
                return "Free Trial";
            case LICENSE_PRO_MONTHLY:
                return "Pro Monthly";
            case LICENSE_PRO_YEARLY:
                return "Pro Yearly";
            case LICENSE_LIFETIME:
                return "Lifetime";
            default:
                return type;
        }
    }
    
    /**
     * Main method for testing (can be used from command line or admin tool)
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: KeyGenerator <license_type> <device_id>");
            System.out.println("License types: free_trial, pro_monthly, pro_yearly, lifetime");
            return;
        }
        
        String licenseType = args[0];
        String deviceId = args[1];
        
        String key = generateKey(licenseType, deviceId);
        System.out.println("Generated Activation Key:");
        System.out.println(key);
        System.out.println("\nLicense Type: " + formatLicenseType(licenseType));
        System.out.println("Device ID: " + deviceId);
        
        long expDate = getExpirationDateFromKey(key);
        System.out.println("Expiration: " + new java.util.Date(expDate));
        System.out.println("\nKey is valid: " + validateKeySignature(key));
    }
}
