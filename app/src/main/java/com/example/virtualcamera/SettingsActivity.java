package com.example.virtualcamera;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Arrays;

/**
 * Settings Activity - Configure app behavior
 * 
 * This activity allows users to configure various settings for the virtual camera.
 */
public class SettingsActivity extends AppCompatActivity {
    
    private static final String PREFS_NAME = "VirtualCameraPrefs";
    
    // Setting keys
    private static final String KEY_RESOLUTION = "resolution";
    private static final String KEY_FPS = "fps";
    private static final String KEY_LOOP_VIDEO = "loop_video";
    private static final String KEY_SHOW_OVERLAY = "show_overlay";
    private static final String KEY_OVERLAY_TEXT = "overlay_text";
    private static final String KEY_DEFAULT_SOURCE = "default_source";
    
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;
    
    private Spinner resolutionSpinner;
    private Spinner fpsSpinner;
    private CheckBox loopVideoCheckBox;
    private CheckBox showOverlayCheckBox;
    private EditText overlayTextEditText;
    private Spinner defaultSourceSpinner;
    private Button saveButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        
        // Initialize preferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Initialize UI elements
        initializeUI();
        
        // Load current settings
        loadSettings();
        
        // Set up event listeners
        setupEventListeners();
    }
    
    private void initializeUI() {
        resolutionSpinner = findViewById(R.id.resolutionSpinner);
        fpsSpinner = findViewById(R.id.fpsSpinner);
        loopVideoCheckBox = findViewById(R.id.loopVideoCheckBox);
        showOverlayCheckBox = findViewById(R.id.showOverlayCheckBox);
        overlayTextEditText = findViewById(R.id.overlayTextEditText);
        defaultSourceSpinner = findViewById(R.id.defaultSourceSpinner);
        saveButton = findViewById(R.id.saveButton);
        
        // Setup resolution spinner
        String[] resolutions = new String[] {
            "320x240 (Low)",
            "640x480 (Medium)",
            "1280x720 (HD) - Recommended",
            "1920x1080 (Full HD)"
        };
        ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, resolutions
        );
        resolutionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolutionSpinner.setAdapter(resolutionAdapter);
        
        // Setup FPS spinner
        String[] fpsOptions = new String[] {
            "15 FPS",
            "24 FPS",
            "30 FPS - Recommended",
            "60 FPS"
        };
        ArrayAdapter<String> fpsAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, fpsOptions
        );
        fpsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fpsSpinner.setAdapter(fpsAdapter);
        
        // Setup default source spinner
        String[] sourceOptions = new String[] {
            "Video File",
            "Static Image",
            "Physical Camera (Back)",
            "Physical Camera (Front)"
        };
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, sourceOptions
        );
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        defaultSourceSpinner.setAdapter(sourceAdapter);
    }
    
    private void loadSettings() {
        // Load resolution
        String resolution = prefs.getString(KEY_RESOLUTION, "1280x720");
        int resolutionIndex = Arrays.asList(getResources().getStringArray(R.array.resolutions))
            .indexOf(resolution + " - Recommended");
        if (resolutionIndex >= 0) {
            resolutionSpinner.setSelection(resolutionIndex);
        }
        
        // Load FPS
        String fps = prefs.getString(KEY_FPS, "30");
        int fpsIndex = 0;
        switch (fps) {
            case "15": fpsIndex = 0; break;
            case "24": fpsIndex = 1; break;
            case "30": fpsIndex = 2; break;
            case "60": fpsIndex = 3; break;
        }
        fpsSpinner.setSelection(fpsIndex);
        
        // Load loop video
        boolean loopVideo = prefs.getBoolean(KEY_LOOP_VIDEO, true);
        loopVideoCheckBox.setChecked(loopVideo);
        
        // Load show overlay
        boolean showOverlay = prefs.getBoolean(KEY_SHOW_OVERLAY, false);
        showOverlayCheckBox.setChecked(showOverlay);
        
        // Load overlay text
        String overlayText = prefs.getString(KEY_OVERLAY_TEXT, "Virtual Camera");
        overlayTextEditText.setText(overlayText);
        
        // Load default source
        String defaultSource = prefs.getString(KEY_DEFAULT_SOURCE, "Video File");
        int sourceIndex = Arrays.asList(getResources().getStringArray(R.array.sources))
            .indexOf(defaultSource);
        if (sourceIndex >= 0) {
            defaultSourceSpinner.setSelection(sourceIndex);
        }
        
        // Enable/disable overlay text based on checkbox
        updateOverlayTextEnabled();
    }
    
    private void setupEventListeners() {
        // Save button
        saveButton.setOnClickListener(v -> saveSettings());
        
        // Loop video checkbox
        loopVideoCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Enable/disable overlay text based on checkbox
                updateOverlayTextEnabled();
            }
        });
        
        // Show overlay checkbox
        showOverlayCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateOverlayTextEnabled();
            }
        });
    }
    
    private void updateOverlayTextEnabled() {
        boolean enabled = showOverlayCheckBox.isChecked();
        overlayTextEditText.setEnabled(enabled);
    }
    
    private void saveSettings() {
        prefsEditor = prefs.edit();
        
        // Save resolution
        String resolution = resolutionSpinner.getSelectedItem().toString();
        // Extract just the resolution (remove the "- Recommended" part)
        resolution = resolution.replace(" - Recommended", "").trim();
        prefsEditor.putString(KEY_RESOLUTION, resolution);
        
        // Save FPS
        String fpsText = fpsSpinner.getSelectedItem().toString();
        String fps = fpsText.replace(" FPS", "").trim();
        prefsEditor.putString(KEY_FPS, fps);
        
        // Save loop video
        boolean loopVideo = loopVideoCheckBox.isChecked();
        prefsEditor.putBoolean(KEY_LOOP_VIDEO, loopVideo);
        
        // Save show overlay
        boolean showOverlay = showOverlayCheckBox.isChecked();
        prefsEditor.putBoolean(KEY_SHOW_OVERLAY, showOverlay);
        
        // Save overlay text
        String overlayText = overlayTextEditText.getText().toString();
        prefsEditor.putString(KEY_OVERLAY_TEXT, overlayText);
        
        // Save default source
        String defaultSource = defaultSourceSpinner.getSelectedItem().toString();
        prefsEditor.putString(KEY_DEFAULT_SOURCE, defaultSource);
        
        // Apply changes
        prefsEditor.apply();
        
        // Show confirmation
        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
        
        // Return to main activity
        finish();
    }
    
    /**
     * Get the selected resolution
     */
    public static String[] getResolution(String resolutionPref, Context context) {
        if (resolutionPref == null || resolutionPref.isEmpty()) {
            return new String[]{"1280", "720"};
        }
        
        String[] parts = resolutionPref.split("x");
        if (parts.length == 2) {
            return parts;
        }
        
        // Default to 1280x720
        return new String[]{"1280", "720"};
    }
    
    /**
     * Get the selected FPS
     */
    public static int getFps(String fpsPref) {
        if (fpsPref == null || fpsPref.isEmpty()) {
            return 30;
        }
        
        try {
            return Integer.parseInt(fpsPref);
        } catch (NumberFormatException e) {
            return 30;
        }
    }
    
    /**
     * Get loop video setting
     */
    public static boolean shouldLoopVideo(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LOOP_VIDEO, true);
    }
    
    /**
     * Get show overlay setting
     */
    public static boolean shouldShowOverlay(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SHOW_OVERLAY, false);
    }
    
    /**
     * Get overlay text
     */
    public static String getOverlayText(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_OVERLAY_TEXT, "Virtual Camera");
    }
    
    /**
     * Get default source
     */
    public static String getDefaultSource(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_DEFAULT_SOURCE, "Video File");
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    @Override
    public void onBackPressed() {
        // Save settings before going back
        saveSettings();
        super.onBackPressed();
    }
}
