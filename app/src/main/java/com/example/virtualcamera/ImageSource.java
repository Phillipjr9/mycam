package com.example.virtualcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.io.InputStream;

/**
 * Image Source - Shows a static image as the virtual camera source
 * 
 * This class loads an image and renders it as a continuous camera feed.
 * Useful for testing or as a static background.
 */
public class ImageSource implements CameraSource {
    
    private static final String TAG = "ImageSource";
    
    private Context context;
    private String imagePath;
    private Bitmap bitmap;
    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private boolean isRunning = false;
    private Thread renderThread;
    private volatile boolean keepRendering = false;
    
    // Text overlay settings
    private String overlayText = "";
    private int overlayColor = Color.WHITE;
    private float overlayTextSize = 48f;
    private int overlayX = 50;
    private int overlayY = 50;
    
    private SourceListener listener;
    
    public ImageSource(Context context, String imagePath) {
        this.context = context;
        this.imagePath = imagePath;
        loadImage();
    }
    
    private void loadImage() {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                // Load from URL (would need network permission)
                // For now, just create a placeholder
                bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.BLUE);
                Paint paint = new Paint();
                paint.setColor(Color.WHITE);
                paint.setTextSize(48);
                canvas.drawText("Network Image", 50, 100, paint);
            } else if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
                // Load from URI
                InputStream input = context.getContentResolver().openInputStream(Uri.parse(imagePath));
                bitmap = BitmapFactory.decodeStream(input, null, options);
                if (input != null) input.close();
            } else {
                // Load from file path
                bitmap = BitmapFactory.decodeFile(imagePath, options);
            }
            
            if (bitmap == null) {
                // Create a placeholder if image fails to load
                bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.GRAY);
                Paint paint = new Paint();
                paint.setColor(Color.WHITE);
                paint.setTextSize(48);
                canvas.drawText("Image Not Found", 50, 100, paint);
            }
            
            Log.d(TAG, "Image loaded: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + e.getMessage());
            // Create error placeholder
            bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.RED);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(48);
            canvas.drawText("Error: " + e.getMessage(), 50, 100, paint);
        }
    }
    
    @Override
    public Surface getSurface() {
        return surface;
    }
    
    @Override
    public void start() {
        if (isRunning) {
            Log.d(TAG, "Image source already running");
            return;
        }
        
        if (bitmap == null) {
            Log.e(TAG, "No bitmap to display");
            if (listener != null) {
                listener.onSourceError("No image loaded");
            }
            return;
        }
        
        try {
            surfaceTexture = new SurfaceTexture(10);
            surface = new Surface(surfaceTexture);
            
            isRunning = true;
            keepRendering = true;
            
            // Start rendering thread
            renderThread = new Thread(() -> {
                while (keepRendering) {
                    try {
                        // Update the texture with the bitmap
                        updateTexture();
                        Thread.sleep(33); // ~30 FPS
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "Error in render thread: " + e.getMessage());
                        break;
                    }
                }
                Log.d(TAG, "Render thread stopped");
            });
            renderThread.start();
            
            if (listener != null) {
                listener.onSourcePrepared();
            }
            
            Log.d(TAG, "Image source started");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting image source: " + e.getMessage());
            if (listener != null) {
                listener.onSourceError(e.getMessage());
            }
        }
    }
    
    @Override
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        keepRendering = false;
        if (renderThread != null) {
            renderThread.interrupt();
            try {
                renderThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            renderThread = null;
        }
        
        cleanup();
        isRunning = false;
        Log.d(TAG, "Image source stopped");
    }
    
    @Override
    public void pause() {
        keepRendering = false;
        if (renderThread != null) {
            renderThread.interrupt();
        }
        isRunning = false;
        Log.d(TAG, "Image source paused");
    }
    
    @Override
    public void resume() {
        if (!isRunning) {
            start();
        }
    }
    
    @Override
    public boolean isRunning() {
        return isRunning;
    }
    
    @Override
    public void setListener(SourceListener listener) {
        this.listener = listener;
    }
    
    private void updateTexture() {
        if (surfaceTexture == null || bitmap == null) {
            return;
        }
        
        // Create a mutable copy to draw on
        Bitmap drawBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(drawBitmap);
        
        // Draw overlay text if set
        if (overlayText != null && !overlayText.isEmpty()) {
            Paint paint = new Paint();
            paint.setColor(overlayColor);
            paint.setTextSize(overlayTextSize);
            paint.setAntiAlias(true);
            canvas.drawText(overlayText, overlayX, overlayY, paint);
        }
        
        // Update the texture
        surfaceTexture.updateTexImage();
        
        // Note: The SurfaceTexture is updated, but we need to ensure
        // the VirtualDisplay sees the updates. This is handled by
        // the VirtualDisplay automatically.
    }
    
    private void cleanup() {
        try {
            if (surface != null) {
                surface.release();
                surface = null;
            }
            if (surfaceTexture != null) {
                surfaceTexture.release();
                surfaceTexture = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup: " + e.getMessage());
        }
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
        loadImage();
        if (isRunning) {
            stop();
            start();
        }
    }
    
    public void setOverlayText(String text) {
        this.overlayText = text;
    }
    
    public void setOverlayColor(int color) {
        this.overlayColor = color;
    }
    
    public void setOverlayTextSize(float size) {
        this.overlayTextSize = size;
    }
    
    public void setOverlayPosition(int x, int y) {
        this.overlayX = x;
        this.overlayY = y;
    }
    
    public Bitmap getBitmap() {
        return bitmap;
    }
}
