package com.example.virtualcamera;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;

/**
 * Screen Recording Source - Captures screen as camera source
 * Allows users to use screen recording as their virtual camera feed
 */
public class ScreenRecordingSource implements CameraSource {
    
    private static final String TAG = "ScreenRecordingSource";
    
    private Context context;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Surface surface;
    
    private int width = 1280;
    private int height = 720;
    private int densityDpi = DisplayMetrics.DENSITY_MEDIUM;
    private int frameRate = 30;
    
    private boolean isRunning = false;
    private SourceListener listener;
    private HandlerThread handlerThread;
    private Handler handler;
    
    private int resultCode;
    private Intent resultData;
    
    public ScreenRecordingSource(Context context, int resultCode, Intent resultData) {
        this.context = context;
        this.resultCode = resultCode;
        this.resultData = resultData;
        
        setupImageReader();
    }
    
    private void setupImageReader() {
        imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        // Process the image
                        processImage(image);
                    }
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }
            }
        }, handler);
        
        surface = imageReader.getSurface();
    }
    
    private void processImage(Image image) {
        // In a real implementation, this would send the image to the virtual camera output
        // For now, we'll just log that we received an image
        Log.d(TAG, "Screen image available: " + image.getWidth() + "x" + image.getHeight());
        
        // Notify listener that we have a frame
        if (listener != null) {
            // For demo, we'll just notify that source is prepared
            // In production, we'd pass the actual frame data
        }
    }
    
    @Override
    public void start() {
        if (isRunning) {
            Log.d(TAG, "Already running");
            return;
        }
        
        // Start handler thread
        handlerThread = new HandlerThread("ScreenRecordingThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        
        try {
            MediaProjectionManager projectionManager = 
                (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData);
            
            if (mediaProjection == null) {
                Log.e(TAG, "Failed to get MediaProjection");
                return;
            }
            
            // Create virtual display
            DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenRecordingSource",
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
            );
            
            isRunning = true;
            
            if (listener != null) {
                listener.onSourcePrepared();
            }
            
            Log.d(TAG, "Screen recording source started");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting screen recording: " + e.getMessage());
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
        
        isRunning = false;
        
        try {
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            
            if (handlerThread != null) {
                handlerThread.quitSafely();
                handlerThread = null;
            }
            
            Log.d(TAG, "Screen recording source stopped");
            
            if (listener != null) {
                listener.onSourceCompleted();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping screen recording: " + e.getMessage());
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
    
    /**
     * Set resolution
     */
    public void setResolution(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    /**
     * Set frame rate
     */
    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }
    
    /**
     * Set DPI
     */
    public void setDensityDpi(int dpi) {
        this.densityDpi = dpi;
    }
    
    /**
     * Update projection result
     */
    public void updateProjectionResult(int resultCode, Intent resultData) {
        this.resultCode = resultCode;
        this.resultData = resultData;
        
        // Restart with new result
        stop();
        setupImageReader();
        start();
    }
}
