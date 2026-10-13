package com.example.virtualcamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

/**
 * Watermark Manager - Handles custom watermarks for virtual camera feed
 */
public class WatermarkManager {
    
    private static final String PREFS_NAME = "VirtualCameraPrefs";
    private static final String KEY_WATERMARK_ENABLED = "watermark_enabled";
    private static final String KEY_WATERMARK_TEXT = "watermark_text";
    private static final String KEY_WATERMARK_COLOR = "watermark_color";
    private static final String KEY_WATERMARK_SIZE = "watermark_size";
    private static final String KEY_WATERMARK_POSITION = "watermark_position";
    
    // Watermark positions
    public static final int POSITION_TOP_LEFT = 0;
    public static final int POSITION_TOP_RIGHT = 1;
    public static final int POSITION_BOTTOM_LEFT = 2;
    public static final int POSITION_BOTTOM_RIGHT = 3;
    public static final int POSITION_CENTER = 4;
    
    private Context context;
    private SharedPreferences prefs;
    
    public WatermarkManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Check if watermark is enabled
     */
    public boolean isEnabled() {
        return prefs.getBoolean(KEY_WATERMARK_ENABLED, false);
    }
    
    /**
     * Enable or disable watermark
     */
    public void setEnabled(boolean enabled) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_WATERMARK_ENABLED, enabled);
        editor.apply();
    }
    
    /**
     * Get watermark text
     */
    public String getText() {
        return prefs.getString(KEY_WATERMARK_TEXT, "Virtual Camera");
    }
    
    /**
     * Set watermark text
     */
    public void setText(String text) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_WATERMARK_TEXT, text);
        editor.apply();
    }
    
    /**
     * Get watermark color
     */
    public int getColor() {
        return prefs.getInt(KEY_WATERMARK_COLOR, Color.WHITE);
    }
    
    /**
     * Set watermark color
     */
    public void setColor(int color) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_WATERMARK_COLOR, color);
        editor.apply();
    }
    
    /**
     * Get watermark size
     */
    public float getSize() {
        return prefs.getFloat(KEY_WATERMARK_SIZE, 40f);
    }
    
    /**
     * Set watermark size
     */
    public void setSize(float size) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_WATERMARK_SIZE, size);
        editor.apply();
    }
    
    /**
     * Get watermark position
     */
    public int getPosition() {
        return prefs.getInt(KEY_WATERMARK_POSITION, POSITION_BOTTOM_RIGHT);
    }
    
    /**
     * Set watermark position
     */
    public void setPosition(int position) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_WATERMARK_POSITION, position);
        editor.apply();
    }
    
    /**
     * Get position description
     */
    public static String getPositionDescription(int position) {
        switch (position) {
            case POSITION_TOP_LEFT:
                return "Top Left";
            case POSITION_TOP_RIGHT:
                return "Top Right";
            case POSITION_BOTTOM_LEFT:
                return "Bottom Left";
            case POSITION_BOTTOM_RIGHT:
                return "Bottom Right";
            case POSITION_CENTER:
                return "Center";
            default:
                return "Bottom Right";
        }
    }
    
    /**
     * Apply watermark to a bitmap
     */
    public Bitmap applyWatermark(Bitmap source) {
        if (!isEnabled() || source == null) {
            return source;
        }
        
        // Create mutable bitmap
        Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        
        // Create paint for text
        Paint paint = new Paint();
        paint.setColor(getColor());
        paint.setTextSize(getSize());
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setShadowLayer(5f, 3f, 3f, Color.BLACK);
        
        // Calculate text position
        String text = getText();
        float textWidth = paint.measureText(text);
        float textHeight = paint.getTextSize();
        
        int x = 0;
        int y = 0;
        
        switch (getPosition()) {
            case POSITION_TOP_LEFT:
                x = (int) (20 + paint.getTextSize() / 2);
                y = (int) (20 + textHeight);
                break;
            case POSITION_TOP_RIGHT:
                x = (int) (source.getWidth() - textWidth - 20);
                y = (int) (20 + textHeight);
                break;
            case POSITION_BOTTOM_LEFT:
                x = (int) (20 + paint.getTextSize() / 2);
                y = (int) (source.getHeight() - 20);
                break;
            case POSITION_BOTTOM_RIGHT:
                x = (int) (source.getWidth() - textWidth - 20);
                y = (int) (source.getHeight() - 20);
                break;
            case POSITION_CENTER:
                x = (int) ((source.getWidth() - textWidth) / 2);
                y = (int) ((source.getHeight() + textHeight) / 2);
                break;
        }
        
        // Draw text
        canvas.drawText(text, x, y, paint);
        
        return result;
    }
    
    /**
     * Apply watermark with custom drawable (logo)
     */
    public Bitmap applyWatermark(Bitmap source, Drawable watermarkDrawable) {
        if (!isEnabled() || source == null || watermarkDrawable == null) {
            return source;
        }
        
        // Create mutable bitmap
        Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        
        // Draw the watermark drawable
        int intrinsicWidth = watermarkDrawable.getIntrinsicWidth();
        int intrinsicHeight = watermarkDrawable.getIntrinsicHeight();
        
        if (intrinsicWidth > 0 && intrinsicHeight > 0) {
            // Scale watermark based on size preference
            float scale = getSize() / 40f;
            int width = (int) (intrinsicWidth * scale);
            int height = (int) (intrinsicHeight * scale);
            
            watermarkDrawable.setBounds(0, 0, width, height);
            
            int x = 0;
            int y = 0;
            
            switch (getPosition()) {
                case POSITION_TOP_LEFT:
                    x = 20;
                    y = 20;
                    break;
                case POSITION_TOP_RIGHT:
                    x = source.getWidth() - width - 20;
                    y = 20;
                    break;
                case POSITION_BOTTOM_LEFT:
                    x = 20;
                    y = source.getHeight() - height - 20;
                    break;
                case POSITION_BOTTOM_RIGHT:
                    x = source.getWidth() - width - 20;
                    y = source.getHeight() - height - 20;
                    break;
                case POSITION_CENTER:
                    x = (source.getWidth() - width) / 2;
                    y = (source.getHeight() - height) / 2;
                    break;
            }
            
            canvas.save();
            canvas.translate(x, y);
            watermarkDrawable.draw(canvas);
            canvas.restore();
        }
        
        return result;
    }
    
    /**
     * Reset to default settings
     */
    public void resetToDefaults() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_WATERMARK_ENABLED, false);
        editor.putString(KEY_WATERMARK_TEXT, "Virtual Camera");
        editor.putInt(KEY_WATERMARK_COLOR, Color.WHITE);
        editor.putFloat(KEY_WATERMARK_SIZE, 40f);
        editor.putInt(KEY_WATERMARK_POSITION, POSITION_BOTTOM_RIGHT);
        editor.apply();
    }
}
