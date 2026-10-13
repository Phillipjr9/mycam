package com.example.virtualcamera;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Source Manager - Manages different camera sources
 * 
 * This class provides a centralized way to create, manage, and switch
 * between different camera sources (video files, images, physical camera).
 */
public class SourceManager {
    
    private static final String TAG = "SourceManager";
    
    private Context context;
    private Map<String, CameraSource> sources;
    private CameraSource currentSource;
    
    // Source types
    public static final String SOURCE_TYPE_VIDEO = "video";
    public static final String SOURCE_TYPE_IMAGE = "image";
    public static final String SOURCE_TYPE_CAMERA = "camera";
    
    public SourceManager(Context context) {
        this.context = context;
        this.sources = new HashMap<>();
    }
    
    /**
     * Create a video source
     */
    public CameraSource createVideoSource(String videoPath) {
        String sourceId = SOURCE_TYPE_VIDEO + "_" + videoPath.hashCode();
        VideoSource source = new VideoSource(context, videoPath);
        sources.put(sourceId, source);
        Log.d(TAG, "Created video source: " + videoPath);
        return source;
    }
    
    /**
     * Create an image source
     */
    public CameraSource createImageSource(String imagePath) {
        String sourceId = SOURCE_TYPE_IMAGE + "_" + imagePath.hashCode();
        ImageSource source = new ImageSource(context, imagePath);
        sources.put(sourceId, source);
        Log.d(TAG, "Created image source: " + imagePath);
        return source;
    }
    
    /**
     * Create a physical camera source
     */
    public CameraSource createCameraSource(String cameraId) {
        String sourceId = SOURCE_TYPE_CAMERA + "_" + cameraId;
        PhysicalCameraSource source = new PhysicalCameraSource(context, cameraId);
        sources.put(sourceId, source);
        Log.d(TAG, "Created camera source: " + cameraId);
        return source;
    }
    
    /**
     * Create a physical camera source using the back camera
     */
    public CameraSource createBackCameraSource() {
        String cameraId = PhysicalCameraSource.getBackCameraId(context);
        if (cameraId != null) {
            return createCameraSource(cameraId);
        }
        return null;
    }
    
    /**
     * Create a physical camera source using the front camera
     */
    public CameraSource createFrontCameraSource() {
        String cameraId = PhysicalCameraSource.getFrontCameraId(context);
        if (cameraId != null) {
            return createCameraSource(cameraId);
        }
        return null;
    }
    
    /**
     * Set the current camera source
     */
    public void setCurrentSource(CameraSource source) {
        // Stop current source
        if (currentSource != null && currentSource.isRunning()) {
            currentSource.stop();
        }
        
        currentSource = source;
        Log.d(TAG, "Set current source: " + source.getClass().getSimpleName());
    }
    
    /**
     * Get the current camera source
     */
    public CameraSource getCurrentSource() {
        return currentSource;
    }
    
    /**
     * Start the current camera source
     */
    public void startCurrentSource() {
        if (currentSource != null) {
            currentSource.start();
            Log.d(TAG, "Started current source");
        }
    }
    
    /**
     * Stop the current camera source
     */
    public void stopCurrentSource() {
        if (currentSource != null && currentSource.isRunning()) {
            currentSource.stop();
            Log.d(TAG, "Stopped current source");
        }
    }
    
    /**
     * Pause the current camera source
     */
    public void pauseCurrentSource() {
        if (currentSource != null && currentSource.isRunning()) {
            currentSource.pause();
            Log.d(TAG, "Paused current source");
        }
    }
    
    /**
     * Resume the current camera source
     */
    public void resumeCurrentSource() {
        if (currentSource != null && !currentSource.isRunning()) {
            currentSource.resume();
            Log.d(TAG, "Resumed current source");
        }
    }
    
    /**
     * Switch to a video source
     */
    public void switchToVideo(String videoPath) {
        CameraSource source = createVideoSource(videoPath);
        setCurrentSource(source);
    }
    
    /**
     * Switch to an image source
     */
    public void switchToImage(String imagePath) {
        CameraSource source = createImageSource(imagePath);
        setCurrentSource(source);
    }
    
    /**
     * Switch to back camera
     */
    public void switchToBackCamera() {
        CameraSource source = createBackCameraSource();
        if (source != null) {
            setCurrentSource(source);
        }
    }
    
    /**
     * Switch to front camera
     */
    public void switchToFrontCamera() {
        CameraSource source = createFrontCameraSource();
        if (source != null) {
            setCurrentSource(source);
        }
    }
    
    /**
     * Clean up all sources
     */
    public void cleanup() {
        stopCurrentSource();
        
        for (CameraSource source : sources.values()) {
            if (source.isRunning()) {
                source.stop();
            }
        }
        sources.clear();
        currentSource = null;
        
        Log.d(TAG, "Cleaned up all sources");
    }
    
    /**
     * Get available camera IDs
     */
    public String[] getAvailableCameras() {
        return PhysicalCameraSource.getAvailableCameras(context);
    }
}
