package com.example.virtualcamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;

/**
 * License Manager - Handles activation key validation and device locking
 * 
 * This ensures each APK/activation key is tied to ONE device only.
 * Keys are generated with a specific format and validated against the device.
 */
public class LicenseManager {
    
    private static final String PREFS_NAME = "VirtualCameraLicense";
    private static final String KEY_ACTIVATION_KEY = "activation_key";
    private static final String KEY_LICENSE_TYPE = "license_type";
    private static final String KEY_EXPIRATION_DATE = "expiration_date";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_IS_ACTIVATED = "is_activated";
    
    // License types
    public static final String LICENSE_FREE_TRIAL = "free_trial";
    public static final String LICENSE_PRO_MONTHLY = "pro_monthly";
    public static final String LICENSE_PRO_YEARLY = "pro_yearly";
    public static final String LICENSE_LIFETIME = "lifetime";
    
    // Device binding constants
    private static final String KEY_INSTALLATION_SIGNATURE = "installation_signature";
    private static final String KEY_FIRST_INSTALL_TIME = "first_install_time";
    
    // Feature flags
    public static final int FEATURE_1080P = 1;
    public static final int FEATURE_60FPS = 2;
    public static final int FEATURE_UNLIMITED_VIDEO = 4;
    public static final int FEATURE_ADVANCED_FILTERS = 8;
    public static final int FEATURE_CUSTOM_OVERLAYS = 16;
    public static final int FEATURE_RTMP = 32;
    public static final int FEATURE_MULTIPLE_CAMERAS = 64;
    public static final int FEATURE_CLONE_MODE = 128;
    public static final int FEATURE_SPEED_CONTROL = 256;
    public static final int FEATURE_COMMERCIAL = 512;
    
    private Context context;
    private SharedPreferences prefs;
    
    public LicenseManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Get the unique device ID for this device
     */
    public String getDeviceId() {
        String deviceId = prefs.getString(KEY_DEVICE_ID, null);
        if (deviceId == null) {
            // Generate and store device ID
            deviceId = generateDeviceId();
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
            
            // Also store installation signature and first install time
            storeInstallationInfo();
        }
        return deviceId;
    }
    
    /**
     * Store installation information to detect APK reinstallation on different devices
     */
    private void storeInstallationInfo() {
        try {
            // Generate installation signature
            String installationSignature = generateInstallationSignature();
            prefs.edit().putString(KEY_INSTALLATION_SIGNATURE, installationSignature).apply();
            
            // Store first install time
            long firstInstallTime = System.currentTimeMillis();
            prefs.edit().putLong(KEY_FIRST_INSTALL_TIME, firstInstallTime).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Generate a unique installation signature
     */
    private String generateInstallationSignature() {
        try {
            // Combine multiple device-specific values
            String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
            );
            
            // Get package info
            String packageName = context.getPackageName();
            long installTime = context.getPackageManager()
                .getPackageInfo(packageName, 0).firstInstallTime;
            
            // Combine and hash
            String data = androidId + "|" + packageName + "|" + installTime + "|INSTALL_SIG";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            return java.util.UUID.randomUUID().toString();
        }
    }
    
    /**
     * Check if this APK installation is on the original device
     * This prevents the APK from being copied to another device
     */
    public boolean isOnOriginalDevice() {
        String storedSignature = prefs.getString(KEY_INSTALLATION_SIGNATURE, null);
        if (storedSignature == null) {
            // First run, this is the original device
            return true;
        }
        
        String currentSignature = generateInstallationSignature();
        return storedSignature.equals(currentSignature);
    }
    
    /**
     * Get the installation signature for this device
     */
    public String getInstallationSignature() {
        return prefs.getString(KEY_INSTALLATION_SIGNATURE, null);
    }
    
    /**
     * Generate a unique device identifier
     */
    private String generateDeviceId() {
        try {
            // Use Android ID as base
            String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
            );
            
            if (androidId == null || androidId.equals("9774d56d682e549c")) {
                // Fallback for devices without proper Android ID
                androidId = java.util.UUID.randomUUID().toString();
            }
            
            // Hash it for privacy
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(androidId.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to random UUID
            return java.util.UUID.randomUUID().toString();
        }
    }
    
    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * Save the activation key
     */
    public boolean saveActivationKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        // Validate the key format
        if (!isValidKeyFormat(key)) {
            return false;
        }
        
        // Extract key components
        KeyComponents components = parseKey(key);
        if (components == null) {
            return false;
        }
        
        // Verify the key is for this device
        if (!components.deviceId.equalsIgnoreCase(getDeviceId())) {
            return false; // Key is tied to a different device
        }
        
        // Save the key
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ACTIVATION_KEY, key);
        editor.putString(KEY_LICENSE_TYPE, components.licenseType);
        editor.putLong(KEY_EXPIRATION_DATE, components.expirationDate);
        editor.putBoolean(KEY_IS_ACTIVATED, true);
        editor.apply();
        
        return true;
    }
    
    /**
     * Check if the app is activated
     */
    public boolean isActivated() {
        return prefs.getBoolean(KEY_IS_ACTIVATED, false);
    }
    
    /**
     * Get the current license type
     */
    public String getLicenseType() {
        return prefs.getString(KEY_LICENSE_TYPE, LICENSE_FREE_TRIAL);
    }
    
    /**
     * Check if the license is expired
     */
    public boolean isExpired() {
        if (LICENSE_LIFETIME.equals(getLicenseType())) {
            return false; // Lifetime never expires
        }
        
        long expirationDate = prefs.getLong(KEY_EXPIRATION_DATE, 0);
        if (expirationDate == 0) {
            return true; // No expiration set, treat as expired
        }
        
        return System.currentTimeMillis() > expirationDate;
    }
    
    /**
     * Check if a specific feature is available
     */
    public boolean hasFeature(int featureFlag) {
        if (!isActivated() || isExpired()) {
            // Check free trial features
            if (LICENSE_FREE_TRIAL.equals(getLicenseType())) {
                return hasFreeTrialFeature(featureFlag);
            }
            return false;
        }
        
        String licenseType = getLicenseType();
        
        switch (licenseType) {
            case LICENSE_FREE_TRIAL:
                return hasFreeTrialFeature(featureFlag);
            case LICENSE_PRO_MONTHLY:
                return hasProMonthlyFeature(featureFlag);
            case LICENSE_PRO_YEARLY:
                return hasProYearlyFeature(featureFlag);
            case LICENSE_LIFETIME:
                return hasLifetimeFeature(featureFlag);
            default:
                return hasFreeTrialFeature(featureFlag);
        }
    }
    
    private boolean hasFreeTrialFeature(int featureFlag) {
        return featureFlag == FEATURE_1080P ||
               featureFlag == FEATURE_60FPS ||
               featureFlag == FEATURE_UNLIMITED_VIDEO;
        // Free trial has limited features
    }
    
    private boolean hasProMonthlyFeature(int featureFlag) {
        // Pro Monthly has most features
        return featureFlag != FEATURE_MULTIPLE_CAMERAS &&
               featureFlag != FEATURE_CLONE_MODE &&
               featureFlag != FEATURE_SPEED_CONTROL &&
               featureFlag != FEATURE_COMMERCIAL;
    }
    
    private boolean hasProYearlyFeature(int featureFlag) {
        // Pro Yearly has almost all features
        return featureFlag != FEATURE_COMMERCIAL;
    }
    
    private boolean hasLifetimeFeature(int featureFlag) {
        // Lifetime has ALL features
        return true;
    }
    
    /**
     * Get remaining days for the current license
     */
    public int getRemainingDays() {
        if (LICENSE_LIFETIME.equals(getLicenseType())) {
            return -1; // -1 means unlimited
        }
        
        long expirationDate = prefs.getLong(KEY_EXPIRATION_DATE, 0);
        if (expirationDate == 0) {
            return 0;
        }
        
        long diff = expirationDate - System.currentTimeMillis();
        return (int) (diff / (1000 * 60 * 60 * 24));
    }
    
    /**
     * Get the saved activation key
     */
    public String getActivationKey() {
        return prefs.getString(KEY_ACTIVATION_KEY, null);
    }
    
    /**
     * Clear the activation key (for testing or deactivation)
     */
    public void clearActivation() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_ACTIVATION_KEY);
        editor.remove(KEY_LICENSE_TYPE);
        editor.remove(KEY_EXPIRATION_DATE);
        editor.putBoolean(KEY_IS_ACTIVATED, false);
        editor.apply();
    }
    
    /**
     * Validate key format (for manual entry)
     */
    public boolean isValidKeyFormat(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        // Key format: VC-LICENSETYPE-DEVICEID-EXPIRATION-SIGNATURE
        String[] parts = key.split("-");
        return parts.length == 5 && parts[0].equals("VC");
    }
    
    /**
     * Parse a key into its components
     */
    public KeyComponents parseKey(String key) {
        try {
            String[] parts = key.split("-");
            if (parts.length != 5 || !parts[0].equals("VC")) {
                return null;
            }
            
            KeyComponents components = new KeyComponents();
            components.licenseType = parts[1];
            components.deviceId = parts[2];
            components.expirationDate = Long.parseLong(parts[3]);
            components.signature = parts[4];
            
            return components;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Generate a new activation key for a device and license type
     * This is for ADMIN USE ONLY (called from website or admin tool)
     */
    public static String generateActivationKey(String licenseType, String deviceId) {
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
     * Generate a signature for the key
     */
    private static String generateSignature(String licenseType, String deviceId, long expirationDate) {
        try {
            String data = licenseType + "|" + deviceId + "|" + expirationDate + "|SECRET_KEY_2026_VC";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            return "INVALID";
        }
    }
    
    /**
     * Verify the key signature
     */
    public boolean verifyKeySignature(String key) {
        KeyComponents components = parseKey(key);
        if (components == null) {
            return false;
        }
        
        String expectedSignature = generateSignature(
            components.licenseType,
            components.deviceId,
            components.expirationDate
        );
        
        return expectedSignature.equals(components.signature);
    }
    
    /**
     * Validate a key completely (format, signature, device match, expiration)
     */
    public boolean validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        // Check format
        if (!isValidKeyFormat(key)) {
            return false;
        }
        
        // Parse key
        KeyComponents components = parseKey(key);
        if (components == null) {
            return false;
        }
        
        // Verify signature
        if (!verifyKeySignature(key)) {
            return false;
        }
        
        // Check device match
        if (!components.deviceId.equalsIgnoreCase(getDeviceId())) {
            return false;
        }
        
        // Check that APK is on original device (prevents copying APK to another device)
        if (!isOnOriginalDevice()) {
            return false;
        }
        
        // Check expiration
        if (System.currentTimeMillis() > components.expirationDate) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Components of an activation key
     */
    public static class KeyComponents {
        String licenseType;
        String deviceId;
        long expirationDate;
        String signature;
    }
}
