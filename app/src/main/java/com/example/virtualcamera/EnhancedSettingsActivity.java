package com.example.virtualcamera;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Arrays;

/**
 * Enhanced Settings Activity - Configure all app features
 * Includes: Dark Mode, Watermark, Filters, Camera Switch, and more
 */
public class EnhancedSettingsActivity extends AppCompatActivity {
    
    private static final String PREFS_NAME = "VirtualCameraPrefs";
    
    // UI Elements
    private Toolbar toolbar;
    
    // Theme Settings
    private RadioGroup themeRadioGroup;
    
    // Camera Settings
    private Spinner resolutionSpinner;
    private Spinner fpsSpinner;
    private Spinner cameraSpinner;
    private CheckBox loopVideoCheckBox;
    private Spinner defaultSourceSpinner;
    
    // Watermark Settings
    private CheckBox watermarkEnabledCheckBox;
    private EditText watermarkTextEditText;
    private Spinner watermarkPositionSpinner;
    private SeekBar watermarkSizeSeekBar;
    private TextView watermarkSizeTextView;
    private Button watermarkColorButton;
    
    // Filter Settings
    private Spinner filterSpinner;
    private SeekBar filterIntensitySeekBar;
    private TextView filterIntensityTextView;
    
    // Other Settings
    private CheckBox showOverlayCheckBox;
    private EditText overlayTextEditText;
    
    private Button saveButton;
    private Button resetButton;
    
    // Managers
    private ThemeHelper themeHelper;
    private WatermarkManager watermarkManager;
    private FilterManager filterManager;
    private CameraSwitcher cameraSwitcher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enhanced_settings);
        
        // Initialize managers
        themeHelper = new ThemeHelper(this);
        watermarkManager = new WatermarkManager(this);
        filterManager = new FilterManager();
        cameraSwitcher = new CameraSwitcher(this);
        
        // Setup toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        
        // Initialize all UI elements
        initializeUI();
        
        // Load settings
        loadSettings();
        
        // Setup event listeners
        setupEventListeners();
    }
    
    private void initializeUI() {
        // Theme Settings
        themeRadioGroup = findViewById(R.id.themeRadioGroup);
        
        // Camera Settings
        resolutionSpinner = findViewById(R.id.resolutionSpinner);
        fpsSpinner = findViewById(R.id.fpsSpinner);
        cameraSpinner = findViewById(R.id.cameraSpinner);
        loopVideoCheckBox = findViewById(R.id.loopVideoCheckBox);
        defaultSourceSpinner = findViewById(R.id.defaultSourceSpinner);
        
        // Watermark Settings
        watermarkEnabledCheckBox = findViewById(R.id.watermarkEnabledCheckBox);
        watermarkTextEditText = findViewById(R.id.watermarkTextEditText);
        watermarkPositionSpinner = findViewById(R.id.watermarkPositionSpinner);
        watermarkSizeSeekBar = findViewById(R.id.watermarkSizeSeekBar);
        watermarkSizeTextView = findViewById(R.id.watermarkSizeTextView);
        watermarkColorButton = findViewById(R.id.watermarkColorButton);
        
        // Filter Settings
        filterSpinner = findViewById(R.id.filterSpinner);
        filterIntensitySeekBar = findViewById(R.id.filterIntensitySeekBar);
        filterIntensityTextView = findViewById(R.id.filterIntensityTextView);
        
        // Other Settings
        showOverlayCheckBox = findViewById(R.id.showOverlayCheckBox);
        overlayTextEditText = findViewById(R.id.overlayTextEditText);
        
        // Buttons
        saveButton = findViewById(R.id.saveButton);
        resetButton = findViewById(R.id.resetButton);
        
        // Setup spinners
        setupSpinners();
    }
    
    private void setupSpinners() {
        // Theme Radio Group - handled in setupEventListeners
        
        // Resolution Spinner
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
        
        // FPS Spinner
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
        
        // Camera Spinner
        String[] cameraOptions = new String[cameraSwitcher.getCameraCount()];
        for (int i = 0; i < cameraSwitcher.getCameraCount(); i++) {
            CameraSwitcher.CameraInfo camera = cameraSwitcher.getAvailableCameras().get(i);
            cameraOptions[i] = camera.getFacingDescription() + " Camera";
        }
        ArrayAdapter<String> cameraAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, cameraOptions
        );
        cameraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cameraSpinner.setAdapter(cameraAdapter);
        
        // Watermark Position Spinner
        String[] watermarkPositions = new String[] {
            "Top Left",
            "Top Right",
            "Bottom Left",
            "Bottom Right",
            "Center"
        };
        ArrayAdapter<String> watermarkPositionAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, watermarkPositions
        );
        watermarkPositionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        watermarkPositionSpinner.setAdapter(watermarkPositionAdapter);
        
        // Filter Spinner
        String[] filterNames = FilterManager.getAllFilterNames();
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, filterNames
        );
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(filterAdapter);
        
        // Default Source Spinner
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
        // Theme Settings
        int darkMode = ThemeHelper.getDarkModePreference(this);
        switch (darkMode) {
            case ThemeHelper.DARK_MODE_DISABLED:
                themeRadioGroup.check(R.id.themeLight);
                break;
            case ThemeHelper.DARK_MODE_ENABLED:
                themeRadioGroup.check(R.id.themeDark);
                break;
            case ThemeHelper.DARK_MODE_AUTO:
            default:
                themeRadioGroup.check(R.id.themeAuto);
                break;
        }
        
        // Camera Settings
        String resolution = SettingsActivity.getResolution(this);
        int resolutionIndex = Arrays.asList(getResources().getStringArray(R.array.resolutions))
            .indexOf(resolution + " - Recommended");
        if (resolutionIndex >= 0) {
            resolutionSpinner.setSelection(resolutionIndex);
        }
        
        String fps = SettingsActivity.getFpsPref(this);
        int fpsIndex = 0;
        switch (fps) {
            case "15": fpsIndex = 0; break;
            case "24": fpsIndex = 1; break;
            case "30": fpsIndex = 2; break;
            case "60": fpsIndex = 3; break;
        }
        fpsSpinner.setSelection(fpsIndex);
        
        // Camera selection
        String currentCameraId = cameraSwitcher.getCurrentCameraId();
        for (int i = 0; i < cameraSwitcher.getCameraCount(); i++) {
            if (cameraSwitcher.getAvailableCameras().get(i).id.equals(currentCameraId)) {
                cameraSpinner.setSelection(i);
                break;
            }
        }
        
        boolean loopVideo = SettingsActivity.shouldLoopVideo(this);
        loopVideoCheckBox.setChecked(loopVideo);
        
        String defaultSource = SettingsActivity.getDefaultSource(this);
        int sourceIndex = Arrays.asList(getResources().getStringArray(R.array.sources))
            .indexOf(defaultSource);
        if (sourceIndex >= 0) {
            defaultSourceSpinner.setSelection(sourceIndex);
        }
        
        // Watermark Settings
        watermarkEnabledCheckBox.setChecked(watermarkManager.isEnabled());
        watermarkTextEditText.setText(watermarkManager.getText());
        watermarkPositionSpinner.setSelection(watermarkManager.getPosition());
        watermarkSizeSeekBar.setProgress((int) watermarkManager.getSize());
        watermarkSizeTextView.setText("Size: " + (int) watermarkManager.getSize() + "px");
        updateWatermarkColorButton();
        
        // Filter Settings
        filterSpinner.setSelection(getFilterIndex(filterManager.getFilter()));
        filterIntensitySeekBar.setProgress((int) (filterManager.getIntensity() * 100));
        filterIntensityTextView.setText("Intensity: " + (int) (filterManager.getIntensity() * 100) + "%");
        
        // Other Settings
        boolean showOverlay = SettingsActivity.shouldShowOverlay(this);
        showOverlayCheckBox.setChecked(showOverlay);
        String overlayText = SettingsActivity.getOverlayText(this);
        overlayTextEditText.setText(overlayText);
    }
    
    private int getFilterIndex(int filterType) {
        int[] filters = FilterManager.getAllFilterTypes();
        for (int i = 0; i < filters.length; i++) {
            if (filters[i] == filterType) {
                return i;
            }
        }
        return 0;
    }
    
    private int getFilterType(int index) {
        int[] filters = FilterManager.getAllFilterTypes();
        return filters[index];
    }
    
    private void setupEventListeners() {
        // Theme Radio Group
        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.themeLight) {
                ThemeHelper.setDarkModePreference(this, ThemeHelper.DARK_MODE_DISABLED);
            } else if (checkedId == R.id.themeDark) {
                ThemeHelper.setDarkModePreference(this, ThemeHelper.DARK_MODE_ENABLED);
            } else if (checkedId == R.id.themeAuto) {
                ThemeHelper.setDarkModePreference(this, ThemeHelper.DARK_MODE_AUTO);
            }
            // Recreate activity to apply theme
            recreate();
        });
        
        // Watermark Size SeekBar
        watermarkSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                watermarkSizeTextView.setText("Size: " + progress + "px");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Watermark Color Button
        watermarkColorButton.setOnClickListener(v -> showColorPicker());
        
        // Filter Intensity SeekBar
        filterIntensitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                filterIntensityTextView.setText("Intensity: " + progress + "%");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Save Button
        saveButton.setOnClickListener(v -> saveSettings());
        
        // Reset Button
        resetButton.setOnClickListener(v -> showResetDialog());
    }
    
    private void updateWatermarkColorButton() {
        int color = watermarkManager.getColor();
        watermarkColorButton.setBackgroundColor(color);
        watermarkColorButton.setTextColor(Color.BLACK);
    }
    
    private void showColorPicker() {
        // Simple color picker dialog
        String[] colors = new String[] {
            "White", "Black", "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta"
        };
        int[] colorValues = new int[] {
            Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, 
            Color.YELLOW, Color.CYAN, Color.MAGENTA
        };
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select Watermark Color");
        
        builder.setItems(colors, (dialog, which) -> {
            watermarkManager.setColor(colorValues[which]);
            updateWatermarkColorButton();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showResetDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Reset Settings");
        builder.setMessage("Are you sure you want to reset all settings to default?");
        builder.setPositiveButton("Reset", (dialog, which) -> {
            resetSettings();
            Toast.makeText(this, "Settings reset to default", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void resetSettings() {
        // Reset theme
        ThemeHelper.setDarkModePreference(this, ThemeHelper.DARK_MODE_AUTO);
        
        // Reset watermark
        watermarkManager.resetToDefaults();
        
        // Reset filter
        filterManager.reset();
        
        // Reload settings
        loadSettings();
    }
    
    private void saveSettings() {
        // Theme - already saved in onCheckedChangeListener
        
        // Camera Settings
        String resolution = resolutionSpinner.getSelectedItem().toString();
        resolution = resolution.replace(" - Recommended", "").trim();
        SettingsActivity.saveResolution(this, resolution);
        
        String fpsText = fpsSpinner.getSelectedItem().toString();
        String fps = fpsText.replace(" FPS", "").trim();
        SettingsActivity.saveFps(this, fps);
        
        // Save camera selection
        int cameraIndex = cameraSpinner.getSelectedItemPosition();
        if (cameraIndex >= 0 && cameraIndex < cameraSwitcher.getCameraCount()) {
            String cameraId = cameraSwitcher.getAvailableCameras().get(cameraIndex).id;
            cameraSwitcher.switchToCamera(cameraId);
        }
        
        boolean loopVideo = loopVideoCheckBox.isChecked();
        SettingsActivity.saveLoopVideo(this, loopVideo);
        
        String defaultSource = defaultSourceSpinner.getSelectedItem().toString();
        SettingsActivity.saveDefaultSource(this, defaultSource);
        
        // Watermark Settings
        watermarkManager.setEnabled(watermarkEnabledCheckBox.isChecked());
        watermarkManager.setText(watermarkTextEditText.getText().toString());
        watermarkManager.setPosition(watermarkPositionSpinner.getSelectedItemPosition());
        watermarkManager.setSize(watermarkSizeSeekBar.getProgress());
        
        // Filter Settings
        int filterIndex = filterSpinner.getSelectedItemPosition();
        filterManager.setFilter(getFilterType(filterIndex));
        filterManager.setIntensity(filterIntensitySeekBar.getProgress() / 100.0f);
        
        // Other Settings
        SettingsActivity.saveShowOverlay(this, showOverlayCheckBox.isChecked());
        SettingsActivity.saveOverlayText(this, overlayTextEditText.getText().toString());
        
        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
    }
    
    // Helper methods for SettingsActivity compatibility
    private static void saveResolution(Context context, String resolution) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("resolution", resolution);
        editor.apply();
    }
    
    private static void saveFps(Context context, String fps) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("fps", fps);
        editor.apply();
    }
    
    private static void saveLoopVideo(Context context, boolean loop) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("loop_video", loop);
        editor.apply();
    }
    
    private static void saveDefaultSource(Context context, String source) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("default_source", source);
        editor.apply();
    }
    
    private static void saveShowOverlay(Context context, boolean show) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("show_overlay", show);
        editor.apply();
    }
    
    private static void saveOverlayText(Context context, String text) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("overlay_text", text);
        editor.apply();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
