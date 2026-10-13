package com.example.virtualcamera;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

/**
 * License Management Activity
 * Shows detailed license information and allows activation
 */
public class LicenseActivity extends AppCompatActivity {
    
    private LicenseManager licenseManager;
    private TextView licenseTypeTextView;
    private TextView deviceIdTextView;
    private TextView statusTextView;
    private TextView expirationTextView;
    private TextView featuresTextView;
    private TextView daysRemainingTextView;
    private Button activateButton;
    private Button copyDeviceIdButton;
    private Button backButton;
    
    private CountDownTimer trialCountdownTimer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        
        // Initialize license manager
        licenseManager = new LicenseManager(this);
        
        // Initialize UI elements
        licenseTypeTextView = findViewById(R.id.licenseTypeTextView);
        deviceIdTextView = findViewById(R.id.deviceIdTextView);
        statusTextView = findViewById(R.id.statusTextView);
        expirationTextView = findViewById(R.id.expirationTextView);
        featuresTextView = findViewById(R.id.featuresTextView);
        daysRemainingTextView = findViewById(R.id.daysRemainingTextView);
        activateButton = findViewById(R.id.activateButton);
        copyDeviceIdButton = findViewById(R.id.copyDeviceIdButton);
        backButton = findViewById(R.id.backButton);
        
        // Set up button listeners
        activateButton.setOnClickListener(v -> showActivationDialog());
        copyDeviceIdButton.setOnClickListener(v -> copyDeviceId());
        backButton.setOnClickListener(v -> finish());
        
        // Update UI
        updateLicenseUI();
    }
    
    private void updateLicenseUI() {
        String licenseType = licenseManager.getLicenseType();
        boolean isActivated = licenseManager.isActivated();
        boolean isExpired = licenseManager.isExpired();
        
        // Set license type
        licenseTypeTextView.setText(formatLicenseType(licenseType));
        
        // Set device ID
        String deviceId = licenseManager.getDeviceId();
        deviceIdTextView.setText(deviceId != null ? deviceId : "Not available");
        
        // Set status
        if (isActivated && !isExpired) {
            statusTextView.setText("Activated");
            statusTextView.setTextColor(ContextCompat.getColor(this, R.color.colorStatusRunning));
        } else if (isActivated && isExpired) {
            statusTextView.setText("Expired");
            statusTextView.setTextColor(ContextCompat.getColor(this, R.color.colorStatusStopped));
        } else {
            statusTextView.setText("Not Activated (Free Trial)");
            statusTextView.setTextColor(ContextCompat.getColor(this, R.color.colorStatusWarning));
        }
        
        // Set expiration
        if (LicenseManager.LICENSE_LIFETIME.equals(licenseType)) {
            expirationTextView.setText("Never expires");
            daysRemainingTextView.setText("Unlimited access");
        } else {
            int daysRemaining = licenseManager.getRemainingDays();
            if (daysRemaining > 0) {
                expirationTextView.setText(formatDate(licenseManager.getExpirationDateFromKey(
                    licenseManager.getActivationKey())));
                daysRemainingTextView.setText(daysRemaining + " days remaining");
                
                // Start countdown timer for trial
                if (LicenseManager.LICENSE_FREE_TRIAL.equals(licenseType)) {
                    startTrialCountdown(daysRemaining);
                }
            } else if (daysRemaining == 0) {
                expirationTextView.setText("Expired today");
                daysRemainingTextView.setText("Please renew");
            } else {
                expirationTextView.setText("Expired");
                daysRemainingTextView.setText("Please renew");
            }
        }
        
        // Set features
        featuresTextView.setText(getAvailableFeaturesText());
        
        // Update button visibility
        activateButton.setVisibility(isActivated && !isExpired ? View.GONE : View.VISIBLE);
    }
    
    private void startTrialCountdown(int daysRemaining) {
        // Cancel any existing timer
        if (trialCountdownTimer != null) {
            trialCountdownTimer.cancel();
        }
        
        // Convert days to milliseconds
        long millisInFuture = daysRemaining * 24 * 60 * 60 * 1000;
        
        trialCountdownTimer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int days = (int) (millisUntilFinished / (1000 * 60 * 60 * 24));
                int hours = (int) ((millisUntilFinished % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
                int minutes = (int) ((millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60));
                
                String timeLeft;
                if (days > 0) {
                    timeLeft = days + " days, " + hours + " hours";
                } else if (hours > 0) {
                    timeLeft = hours + " hours, " + minutes + " minutes";
                } else {
                    timeLeft = minutes + " minutes";
                }
                
                daysRemainingTextView.setText(timeLeft + " remaining");
                
                // Show warning when trial is about to expire
                if (days == 0 && hours <= 24) {
                    daysRemainingTextView.setTextColor(ContextCompat.getColor(
                        LicenseActivity.this, R.color.colorStatusStopped));
                }
            }
            
            @Override
            public void onFinish() {
                daysRemainingTextView.setText("Trial expired - Please activate");
                daysRemainingTextView.setTextColor(ContextCompat.getColor(
                    LicenseActivity.this, R.color.colorStatusStopped));
            }
        }.start();
    }
    
    private String formatLicenseType(String type) {
        if (type == null) return "Unknown";
        
        switch (type) {
            case LicenseManager.LICENSE_FREE_TRIAL:
                return "Free Trial";
            case LicenseManager.LICENSE_PRO_MONTHLY:
                return "Pro Monthly";
            case LicenseManager.LICENSE_PRO_YEARLY:
                return "Pro Yearly";
            case LicenseManager.LICENSE_LIFETIME:
                return "Lifetime";
            default:
                return type;
        }
    }
    
    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "Not set";
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        return String.format("%d-%02d-%02d %02d:%02d", year, month, day, hour, minute);
    }
    
    private String getAvailableFeaturesText() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Available Features:\n\n");
        
        // Basic features
        sb.append("✓ Video File Playback\n");
        sb.append("✓ Static Image Display\n");
        sb.append("✓ Physical Camera\n");
        sb.append("✓ Basic Text Overlays\n");
        sb.append("✓ Basic Color Filters\n\n");
        
        // Check for Pro features
        if (licenseManager.hasFeature(LicenseManager.FEATURE_UNLIMITED_VIDEO)) {
            sb.append("✓ Unlimited Video Length\n");
        } else {
            sb.append("✗ Unlimited Video Length (Pro only)\n");
        }
        
        if (licenseManager.hasFeature(LicenseManager.FEATURE_1080P)) {
            sb.append("✓ 1080p Full HD\n");
        } else {
            sb.append("✗ 1080p Full HD (Pro only)\n");
        }
        
        if (licenseManager.hasFeature(LicenseManager.FEATURE_60FPS)) {
            sb.append("✓ 60 FPS\n");
        } else {
            sb.append("✗ 60 FPS (Pro only)\n");
        }
        
        if (licenseManager.hasFeature(LicenseManager.FEATURE_ADVANCED_FILTERS)) {
            sb.append("✓ Advanced Color Filters\n");
        } else {
            sb.append("✗ Advanced Color Filters (Pro only)\n");
        }
        
        if (licenseManager.hasFeature(LicenseManager.FEATURE_CUSTOM_OVERLAYS)) {
            sb.append("✓ Custom Overlays\n");
        } else {
            sb.append("✗ Custom Overlays (Pro only)\n");
        }
        
        if (licenseManager.hasFeature(LicenseManager.FEATURE_RTMP)) {
            sb.append("✓ RTMP Live Streaming\n");
        } else {
            sb.append("✗ RTMP Live Streaming (Pro only)\n");
        }
        
        if (licenseManager.hasFeature(LicenseManager.FEATURE_MULTIPLE_CAMERAS)) {
            sb.append("✓ Multiple Virtual Cameras\n");
        } else {
            sb.append("✗ Multiple Virtual Cameras (Pro Yearly/Lifetime only)\n");
        }
        
        if (licenseManager.hasFeature(LicenseManager.FEATURE_CLONE_MODE)) {
            sb.append("✓ Clone Mode\n");
        } else {
            sb.append("✗ Clone Mode (Pro Yearly/Lifetime only)\n");
        }
        
        if (licenseManager.hasFeature(LicenseManager.FEATURE_COMMERCIAL)) {
            sb.append("✓ Commercial License\n");
        } else {
            sb.append("✗ Commercial License (Lifetime only)\n");
        }
        
        return sb.toString();
    }
    
    private void showActivationDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Activate License");
        
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter your activation key");
        input.setPadding(16, 16, 16, 16);
        builder.setView(input);
        
        builder.setPositiveButton("Activate", (dialog, which) -> {
            String key = input.getText().toString().trim();
            if (key.isEmpty()) {
                Toast.makeText(this, "Please enter a key", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (licenseManager.validateKey(key)) {
                licenseManager.saveActivationKey(key);
                Toast.makeText(this, "License activated successfully!", Toast.LENGTH_LONG).show();
                updateLicenseUI();
                
                // Notify MainActivity to update its UI
                setResult(RESULT_OK);
            } else {
                Toast.makeText(this, "Invalid activation key. Make sure you entered it correctly and it's for this device.", 
                    Toast.LENGTH_LONG).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.setMessage("Get your activation key from Telegram or Email after payment.\n\nEach key is tied to ONE device only.");
        builder.show();
    }
    
    private void copyDeviceId() {
        String deviceId = licenseManager.getDeviceId();
        if (deviceId != null) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) 
                getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Device ID", deviceId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Device ID copied to clipboard!", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel countdown timer
        if (trialCountdownTimer != null) {
            trialCountdownTimer.cancel();
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
