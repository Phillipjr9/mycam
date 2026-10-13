package com.example.virtualcamera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

/**
 * Filter Manager - Applies various filters to camera feed
 */
public class FilterManager {
    
    // Filter types
    public static final int FILTER_NONE = 0;
    public static final int FILTER_GRAYSCALE = 1;
    public static final int FILTER_SEPIA = 2;
    public static final int FILTER_NEGATIVE = 3;
    public static final int FILTER_BLUR = 4;
    public static final int FILTER_SHARPEN = 5;
    public static final int FILTER_BRIGHTNESS = 6;
    public static final int FILTER_CONTRAST = 7;
    public static final int FILTER_SATURATION = 8;
    public static final int FILTER_HUE = 9;
    public static final int FILTER_WARM = 10;
    public static final int FILTER_COOL = 11;
    public static final int FILTER_VINTAGE = 12;
    public static final int FILTER_INFRARED = 13;
    
    private int currentFilter = FILTER_NONE;
    private float filterIntensity = 1.0f;
    
    // Filter parameters
    private float brightness = 1.0f;
    private float contrast = 1.0f;
    private float saturation = 1.0f;
    private float hue = 0.0f;
    
    /**
     * Apply filter to bitmap
     */
    public Bitmap applyFilter(Bitmap source) {
        if (source == null || currentFilter == FILTER_NONE) {
            return source;
        }
        
        Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        
        ColorMatrix colorMatrix = new ColorMatrix();
        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorFilter);
        
        switch (currentFilter) {
            case FILTER_GRAYSCALE:
                colorMatrix.setSaturation(0);
                break;
            case FILTER_SEPIA:
                applySepia(colorMatrix);
                break;
            case FILTER_NEGATIVE:
                colorMatrix.setScale(-1, -1, -1, 1);
                colorMatrix.postTranslate(1, 1, 1, 0);
                break;
            case FILTER_BRIGHTNESS:
                colorMatrix.setScale(brightness, brightness, brightness, 1);
                break;
            case FILTER_CONTRAST:
                float scale = contrast;
                float translate = (1.0f - contrast) / 2.0f;
                colorMatrix.setScale(scale, scale, scale, 1);
                colorMatrix.postTranslate(translate, translate, translate, 0);
                break;
            case FILTER_SATURATION:
                colorMatrix.setSaturation(saturation);
                break;
            case FILTER_HUE:
                colorMatrix.setRotate(0, hue * 360);
                break;
            case FILTER_WARM:
                colorMatrix.setScale(1.1f, 1.0f, 0.9f, 1);
                break;
            case FILTER_COOL:
                colorMatrix.setScale(0.9f, 1.0f, 1.1f, 1);
                break;
            case FILTER_VINTAGE:
                applyVintage(colorMatrix);
                break;
            case FILTER_INFRARED:
                applyInfrared(colorMatrix);
                break;
        }
        
        paint.setColorFilter(colorFilter);
        canvas.drawBitmap(source, 0, 0, paint);
        
        return result;
    }
    
    private void applySepia(ColorMatrix matrix) {
        matrix.set(new float[]{
            0.393f, 0.349f, 0.272f, 0, 0,
            0.769f, 0.686f, 0.534f, 0, 0,
            0.189f, 0.168f, 0.131f, 0, 0,
            0,    0,    0,    1, 0
        });
    }
    
    private void applyVintage(ColorMatrix matrix) {
        matrix.set(new float[]{
            0.62793f, 0.32171f, -0.03966f, 0, 0,
            0.02578f, 0.64411f, 0.03247f, 0, 0,
            0.04651f, 0.12894f, 0.44314f, 0, 0,
            0,      0,      0,      1, 0
        });
    }
    
    private void applyInfrared(ColorMatrix matrix) {
        // Red channel only (infrared effect)
        matrix.set(new float[]{
            1, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 1, 0
        });
    }
    
    /**
     * Set current filter
     */
    public void setFilter(int filter) {
        this.currentFilter = filter;
    }
    
    /**
     * Get current filter
     */
    public int getFilter() {
        return currentFilter;
    }
    
    /**
     * Set filter intensity (0-2)
     */
    public void setIntensity(float intensity) {
        this.filterIntensity = Math.max(0, Math.min(2, intensity));
    }
    
    /**
     * Get filter intensity
     */
    public float getIntensity() {
        return filterIntensity;
    }
    
    /**
     * Set brightness (-1 to 2)
     */
    public void setBrightness(float brightness) {
        this.brightness = Math.max(0, Math.min(2, brightness));
        if (currentFilter == FILTER_BRIGHTNESS) {
            // Force re-apply
        }
    }
    
    /**
     * Set contrast (0 to 2)
     */
    public void setContrast(float contrast) {
        this.contrast = Math.max(0, Math.min(2, contrast));
    }
    
    /**
     * Set saturation (0 to 2)
     */
    public void setSaturation(float saturation) {
        this.saturation = Math.max(0, Math.min(2, saturation));
    }
    
    /**
     * Set hue (0 to 1)
     */
    public void setHue(float hue) {
        this.hue = Math.max(0, Math.min(1, hue));
    }
    
    /**
     * Get filter name
     */
    public static String getFilterName(int filter) {
        switch (filter) {
            case FILTER_NONE:
                return "None";
            case FILTER_GRAYSCALE:
                return "Grayscale";
            case FILTER_SEPIA:
                return "Sepia";
            case FILTER_NEGATIVE:
                return "Negative";
            case FILTER_BLUR:
                return "Blur";
            case FILTER_SHARPEN:
                return "Sharpen";
            case FILTER_BRIGHTNESS:
                return "Brightness";
            case FILTER_CONTRAST:
                return "Contrast";
            case FILTER_SATURATION:
                return "Saturation";
            case FILTER_HUE:
                return "Hue";
            case FILTER_WARM:
                return "Warm";
            case FILTER_COOL:
                return "Cool";
            case FILTER_VINTAGE:
                return "Vintage";
            case FILTER_INFRARED:
                return "Infrared";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Get all filter names
     */
    public static String[] getAllFilterNames() {
        return new String[]{
            "None",
            "Grayscale",
            "Sepia",
            "Negative",
            "Warm",
            "Cool",
            "Vintage",
            "Infrared",
            "Brightness",
            "Contrast",
            "Saturation"
        };
    }
    
    /**
     * Get all filter types
     */
    public static int[] getAllFilterTypes() {
        return new int[]{
            FILTER_NONE,
            FILTER_GRAYSCALE,
            FILTER_SEPIA,
            FILTER_NEGATIVE,
            FILTER_WARM,
            FILTER_COOL,
            FILTER_VINTAGE,
            FILTER_INFRARED,
            FILTER_BRIGHTNESS,
            FILTER_CONTRAST,
            FILTER_SATURATION
        };
    }
    
    /**
     * Reset all filters
     */
    public void reset() {
        currentFilter = FILTER_NONE;
        filterIntensity = 1.0f;
        brightness = 1.0f;
        contrast = 1.0f;
        saturation = 1.0f;
        hue = 0.0f;
    }
}
