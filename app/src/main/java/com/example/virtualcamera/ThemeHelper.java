package com.example.virtualcamera;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Theme Helper - Manages dark mode and theme preferences
 */
public class ThemeHelper {
    
    private static final String PREFS_NAME = "VirtualCameraPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    
    // Dark mode options
    public static final int DARK_MODE_DISABLED = 0;
    public static final int DARK_MODE_ENABLED = 1;
    public static final int DARK_MODE_AUTO = 2;
    
    /**
     * Apply the saved theme
     */
    public static void applyTheme(Context context) {
        int darkMode = getDarkModePreference(context);
        
        switch (darkMode) {
            case DARK_MODE_ENABLED:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case DARK_MODE_DISABLED:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case DARK_MODE_AUTO:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
    
    /**
     * Get the dark mode preference
     */
    public static int getDarkModePreference(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_DARK_MODE, DARK_MODE_AUTO);
    }
    
    /**
     * Set the dark mode preference
     */
    public static void setDarkModePreference(Context context, int darkMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_DARK_MODE, darkMode);
        editor.apply();
        applyTheme(context);
    }
    
    /**
     * Toggle dark mode
     */
    public static void toggleDarkMode(Context context) {
        int currentMode = getDarkModePreference(context);
        int newMode;
        
        switch (currentMode) {
            case DARK_MODE_DISABLED:
                newMode = DARK_MODE_ENABLED;
                break;
            case DARK_MODE_ENABLED:
                newMode = DARK_MODE_AUTO;
                break;
            case DARK_MODE_AUTO:
            default:
                newMode = DARK_MODE_DISABLED;
                break;
        }
        
        setDarkModePreference(context, newMode);
    }
    
    /**
     * Get dark mode description
     */
    public static String getDarkModeDescription(Context context, int darkMode) {
        switch (darkMode) {
            case DARK_MODE_DISABLED:
                return "Light Mode";
            case DARK_MODE_ENABLED:
                return "Dark Mode";
            case DARK_MODE_AUTO:
            default:
                return "System Default";
        }
    }
}
