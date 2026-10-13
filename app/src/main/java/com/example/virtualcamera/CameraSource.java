package com.example.virtualcamera;

import android.view.Surface;

/**
 * Camera Source Interface
 * 
 * This interface defines the contract for different camera sources.
 * Implementations can provide video from various sources:
 * - Video files
 * - Static images
 * - Physical camera
 * - Network streams
 */
public interface CameraSource {
    
    /**
     * Get the Surface that this source renders to.
     * This Surface will be used as the input for the virtual camera.
     * 
     * @return The Surface to render to, or null if not ready
     */
    Surface getSurface();
    
    /**
     * Start the camera source.
     * This should begin providing frames to the Surface.
     */
    void start();
    
    /**
     * Stop the camera source.
     * This should stop providing frames and release resources.
     */
    void stop();
    
    /**
     * Pause the camera source.
     * Frames should stop being provided, but resources should be retained.
     */
    void pause();
    
    /**
     * Resume the camera source after pausing.
     */
    void resume();
    
    /**
     * Check if the source is currently running.
     * 
     * @return true if the source is active, false otherwise
     */
    boolean isRunning();
    
    /**
     * Set a listener for source events.
     * 
     * @param listener The listener to notify of source events
     */
    void setListener(SourceListener listener);
    
    /**
     * Listener for camera source events.
     */
    interface SourceListener {
        /**
         * Called when the source is prepared and ready to provide frames.
         */
        void onSourcePrepared();
        
        /**
         * Called when an error occurs in the source.
         * 
         * @param error The error message
         */
        void onSourceError(String error);
        
        /**
         * Called when the source has completed (e.g., video ended).
         */
        void onSourceCompleted();
    }
}
