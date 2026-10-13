package com.example.virtualcamera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;

/**
 * Background Remover - Removes or replaces background from images
 * Uses basic color-based segmentation for demo purposes
 * For production, consider using ML Kit or a dedicated library
 */
public class BackgroundRemover {
    
    private static final String TAG = "BackgroundRemover";
    
    // Background replacement options
    public static final int BACKGROUND_TRANSPARENT = 0;
    public static final int BACKGROUND_BLUR = 1;
    public static final int BACKGROUND_COLOR = 2;
    public static final int BACKGROUND_IMAGE = 3;
    
    private int replacementType = BACKGROUND_TRANSPARENT;
    private int replacementColor = Color.TRANSPARENT;
    private Bitmap replacementImage;
    
    // Sensitivity for background detection (0-100)
    private int sensitivity = 50;
    
    // Blur radius for blur effect
    private int blurRadius = 25;
    
    /**
     * Remove background from bitmap
     * Uses simple color thresholding for demo
     */
    public Bitmap removeBackground(Bitmap source) {
        if (source == null) {
            return null;
        }
        
        // For demo, we'll use a simple approach:
        // 1. Detect background color (assume corners are background)
        // 2. Create mask based on color similarity
        // 3. Apply mask to remove background
        
        int width = source.getWidth();
        int height = source.getHeight();
        
        // Detect background color from corners
        int backgroundColor = detectBackgroundColor(source);
        
        // Create mask
        Bitmap mask = createMask(source, backgroundColor);
        
        // Apply mask
        Bitmap result = applyMask(source, mask);
        
        // Apply replacement
        result = applyReplacement(result, mask);
        
        return result;
    }
    
    /**
     * Detect background color from image corners
     */
    private int detectBackgroundColor(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        
        // Sample colors from 4 corners
        int color1 = source.getPixel(10, 10);
        int color2 = source.getPixel(width - 10, 10);
        int color3 = source.getPixel(10, height - 10);
        int color4 = source.getPixel(width - 10, height - 10);
        
        // Average the colors
        int red = (Color.red(color1) + Color.red(color2) + Color.red(color3) + Color.red(color4)) / 4;
        int green = (Color.green(color1) + Color.green(color2) + Color.green(color3) + Color.green(color4)) / 4;
        int blue = (Color.blue(color1) + Color.blue(color2) + Color.blue(color3) + Color.blue(color4)) / 4;
        
        return Color.rgb(red, green, blue);
    }
    
    /**
     * Create mask based on color similarity to background
     */
    private Bitmap createMask(Bitmap source, int backgroundColor) {
        int width = source.getWidth();
        int height = source.getHeight();
        
        Bitmap mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mask);
        
        // Threshold for color similarity (based on sensitivity)
        float threshold = sensitivity / 100.0f * 200.0f; // 0-200 range
        
        int bgRed = Color.red(backgroundColor);
        int bgGreen = Color.green(backgroundColor);
        int bgBlue = Color.blue(backgroundColor);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getPixel(x, y);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                
                // Calculate color distance
                float distance = Math.sqrt(
                    Math.pow(red - bgRed, 2) +
                    Math.pow(green - bgGreen, 2) +
                    Math.pow(blue - bgBlue, 2)
                );
                
                // If color is similar to background, make mask transparent
                if (distance < threshold) {
                    mask.setPixel(x, y, Color.TRANSPARENT);
                } else {
                    mask.setPixel(x, y, Color.WHITE);
                }
            }
        }
        
        return mask;
    }
    
    /**
     * Apply mask to source image
     */
    private Bitmap applyMask(Bitmap source, Bitmap mask) {
        if (source == null || mask == null) {
            return source;
        }
        
        int width = source.getWidth();
        int height = source.getHeight();
        
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        
        Paint paint = new Paint();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        
        canvas.drawBitmap(source, 0, 0, null);
        canvas.drawBitmap(mask, 0, 0, paint);
        
        paint.setXfermode(null);
        
        return result;
    }
    
    /**
     * Apply background replacement
     */
    private Bitmap applyReplacement(Bitmap source, Bitmap mask) {
        if (source == null || mask == null) {
            return source;
        }
        
        int width = source.getWidth();
        int height = source.getHeight();
        
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        
        switch (replacementType) {
            case BACKGROUND_TRANSPARENT:
                // Result is already transparent where mask is transparent
                canvas.drawBitmap(source, 0, 0, null);
                break;
                
            case BACKGROUND_COLOR:
                // Fill with color
                canvas.drawColor(replacementColor);
                // Draw source with mask
                Paint paint = new Paint();
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                canvas.drawBitmap(source, 0, 0, paint);
                break;
                
            case BACKGROUND_BLUR:
                // Create blurred version
                Bitmap blurred = applyBlur(source, blurRadius);
                canvas.drawBitmap(blurred, 0, 0, null);
                // Draw source with mask
                Paint paint2 = new Paint();
                paint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                canvas.drawBitmap(source, 0, 0, paint2);
                break;
                
            case BACKGROUND_IMAGE:
                if (replacementImage != null) {
                    // Scale and draw replacement image
                    Bitmap scaled = Bitmap.createScaledBitmap(
                        replacementImage, width, height, true);
                    canvas.drawBitmap(scaled, 0, 0, null);
                    // Draw source with mask
                    Paint paint3 = new Paint();
                    paint3.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                    canvas.drawBitmap(source, 0, 0, paint3);
                }
                break;
        }
        
        return result;
    }
    
    /**
     * Apply simple box blur (for demo, use a proper blur library for production)
     */
    private Bitmap applyBlur(Bitmap source, int radius) {
        if (radius <= 0) {
            return source;
        }
        
        int width = source.getWidth();
        int height = source.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        
        int pixelCount = width * height;
        int[] pixels = new int[pixelCount];
        source.getPixels(pixels, 0, width, 0, 0, width, height);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sumRed = 0, sumGreen = 0, sumBlue = 0, sumAlpha = 0;
                int count = 0;
                
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int ny = y + dy;
                        int nx = x + dx;
                        
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            int pixel = pixels[ny * width + nx];
                            sumRed += Color.red(pixel);
                            sumGreen += Color.green(pixel);
                            sumBlue += Color.blue(pixel);
                            sumAlpha += Color.alpha(pixel);
                            count++;
                        }
                    }
                }
                
                int avgRed = sumRed / count;
                int avgGreen = sumGreen / count;
                int avgBlue = sumBlue / count;
                int avgAlpha = sumAlpha / count;
                
                result.setPixel(x, y, Color.argb(avgAlpha, avgRed, avgGreen, avgBlue));
            }
        }
        
        return result;
    }
    
    /**
     * Set replacement type
     */
    public void setReplacementType(int type) {
        this.replacementType = type;
    }
    
    /**
     * Set replacement color
     */
    public void setReplacementColor(int color) {
        this.replacementColor = color;
    }
    
    /**
     * Set replacement image
     */
    public void setReplacementImage(Bitmap image) {
        this.replacementImage = image;
    }
    
    /**
     * Set sensitivity (0-100)
     */
    public void setSensitivity(int sensitivity) {
        this.sensitivity = Math.max(0, Math.min(100, sensitivity));
    }
    
    /**
     * Set blur radius
     */
    public void setBlurRadius(int radius) {
        this.blurRadius = Math.max(0, radius);
    }
    
    /**
     * Get current settings summary
     */
    public String getSettingsSummary() {
        String type;
        switch (replacementType) {
            case BACKGROUND_TRANSPARENT:
                type = "Transparent";
                break;
            case BACKGROUND_BLUR:
                type = "Blur (" + blurRadius + "px)";
                break;
            case BACKGROUND_COLOR:
                type = "Color";
                break;
            case BACKGROUND_IMAGE:
                type = "Image";
                break;
            default:
                type = "Unknown";
        }
        
        return "Background: " + type + ", Sensitivity: " + sensitivity + "%";
    }
}
