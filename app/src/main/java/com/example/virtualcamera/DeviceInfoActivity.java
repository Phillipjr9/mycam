package com.example.virtualcamera;

import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;

/**
 * Device Info Activity - Shows device and app information for support
 */
public class DeviceInfoActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        
        // Set device information
        setDeviceInfo();
    }
    
    private void setDeviceInfo() {
        LicenseManager licenseManager = new LicenseManager(this);
        
        // App Info
        TextView appName = findViewById(R.id.appName);
        TextView appVersion = findViewById(R.id.appVersion);
        TextView appPackage = findViewById(R.id.appPackage);
        
        appName.setText("Virtual Camera");
        
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            appVersion.setText(pInfo.versionName + " (" + pInfo.versionCode + ")");
            appPackage.setText(pInfo.packageName);
        } catch (Exception e) {
            appVersion.setText("Unknown");
            appPackage.setText("Unknown");
        }
        
        // Device Info
        TextView deviceModel = findViewById(R.id.deviceModel);
        TextView deviceManufacturer = findViewById(R.id.deviceManufacturer);
        TextView deviceAndroidVersion = findViewById(R.id.deviceAndroidVersion);
        TextView deviceSdkVersion = findViewById(R.id.deviceSdkVersion);
        TextView deviceId = findViewById(R.id.deviceDeviceId);
        TextView deviceInstallationSig = findViewById(R.id.deviceInstallationSig);
        
        deviceModel.setText(Build.MODEL);
        deviceManufacturer.setText(Build.MANUFACTURER);
        deviceAndroidVersion.setText(Build.VERSION.RELEASE);
        deviceSdkVersion.setText(String.valueOf(Build.VERSION.SDK_INT));
        deviceId.setText(licenseManager.getDeviceId());
        deviceInstallationSig.setText(licenseManager.getInstallationSignature());
        
        // Display Info
        TextView displayResolution = findViewById(R.id.displayResolution);
        TextView displayDensity = findViewById(R.id.displayDensity);
        
        try {
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            displayResolution.setText(width + " x " + height);
            displayDensity.setText(getDensityString(getResources().getDisplayMetrics().densityDpi));
        } catch (Exception e) {
            displayResolution.setText("Unknown");
            displayDensity.setText("Unknown");
        }
        
        // Camera Info
        TextView cameraCount = findViewById(R.id.cameraCount);
        TextView cameraFeatures = findViewById(R.id.cameraFeatures);
        
        try {
            int cameraCountValue = android.hardware.Camera.getNumberOfCameras();
            cameraCount.setText(String.valueOf(cameraCountValue));
            
            StringBuilder features = new StringBuilder();
            if (getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA)) {
                features.append("✓ Camera\n");
            }
            if (getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_FRONT)) {
                features.append("✓ Front Camera\n");
            }
            if (getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
                features.append("✓ Autofocus\n");
            }
            if (getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_FLASH)) {
                features.append("✓ Flash\n");
            }
            
            cameraFeatures.setText(features.toString().isEmpty() ? "None detected" : features.toString());
        } catch (Exception e) {
            cameraCount.setText("Unknown");
            cameraFeatures.setText("Unknown");
        }
        
        // System Info
        TextView systemLanguage = findViewById(R.id.systemLanguage);
        TextView systemCountry = findViewById(R.id.systemCountry);
        
        Locale locale = getResources().getConfiguration().locale;
        systemLanguage.setText(locale.getDisplayLanguage());
        systemCountry.setText(locale.getCountry());
        
        // License Info
        TextView licenseType = findViewById(R.id.licenseType);
        TextView licenseStatus = findViewById(R.id.licenseStatus);
        TextView licenseExpiration = findViewById(R.id.licenseExpiration);
        
        licenseType.setText(licenseManager.getLicenseType());
        licenseStatus.setText(licenseManager.isActivated() ? "Activated" : "Not Activated");
        
        if (licenseManager.getRemainingDays() == -1) {
            licenseExpiration.setText("Never expires");
        } else {
            licenseExpiration.setText(licenseManager.getRemainingDays() + " days remaining");
        }
    }
    
    private String getDensityString(int densityDpi) {
        switch (densityDpi) {
            case android.util.DisplayMetrics.DENSITY_LOW:
                return "LDPI (Low)";
            case android.util.DisplayMetrics.DENSITY_MEDIUM:
                return "MDPI (Medium)";
            case android.util.DisplayMetrics.DENSITY_HIGH:
                return "HDPI (High)";
            case android.util.DisplayMetrics.DENSITY_XHIGH:
                return "XHDPI (Extra High)";
            case android.util.DisplayMetrics.DENSITY_XXHIGH:
                return "XXHDPI (Extra Extra High)";
            case android.util.DisplayMetrics.DENSITY_XXXHIGH:
                return "XXXHDPI (Extra Extra Extra High)";
            case android.util.DisplayMetrics.DENSITY_TV:
                return "TV";
            default:
                return densityDpi + " DPI";
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
