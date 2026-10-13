package com.example.virtualcamera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Camera Switcher - Manages multiple cameras (front and back)
 */
public class CameraSwitcher {
    
    private Context context;
    private CameraManager cameraManager;
    private List<CameraInfo> availableCameras;
    private int currentCameraIndex = 0;
    
    public CameraSwitcher(Context context) {
        this.context = context;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        this.availableCameras = new ArrayList<>();
        
        // Discover available cameras
        discoverCameras();
    }
    
    /**
     * Discover all available cameras
     */
    private void discoverCameras() {
        try {
            availableCameras.clear();
            String[] cameraIds = cameraManager.getCameraIdList();
            
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                int facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                
                CameraInfo cameraInfo = new CameraInfo();
                cameraInfo.id = cameraId;
                cameraInfo.facing = facing;
                cameraInfo.isFront = (facing == CameraCharacteristics.LENS_FACING_FRONT);
                cameraInfo.isBack = (facing == CameraCharacteristics.LENS_FACING_BACK);
                
                // Get camera capabilities
                int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                cameraInfo.supportedCapabilities = capabilities;
                
                // Get max resolution
                android.util.Size[] sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(android.graphics.SurfaceTexture.class);
                if (sizes != null && sizes.length > 0) {
                    cameraInfo.maxResolution = sizes[0];
                }
                
                availableCameras.add(cameraInfo);
            }
            
            // Sort: back camera first, then front
            availableCameras.sort((c1, c2) -> {
                if (c1.isBack && !c2.isBack) return -1;
                if (!c1.isBack && c2.isBack) return 1;
                return 0;
            });
            
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Get list of available cameras
     */
    public List<CameraInfo> getAvailableCameras() {
        return availableCameras;
    }
    
    /**
     * Get current camera ID
     */
    public String getCurrentCameraId() {
        if (availableCameras.isEmpty()) return null;
        return availableCameras.get(currentCameraIndex).id;
    }
    
    /**
     * Get current camera info
     */
    public CameraInfo getCurrentCamera() {
        if (availableCameras.isEmpty()) return null;
        return availableCameras.get(currentCameraIndex);
    }
    
    /**
     * Switch to next camera
     */
    public String switchToNextCamera() {
        if (availableCameras.isEmpty()) return null;
        
        currentCameraIndex = (currentCameraIndex + 1) % availableCameras.size();
        return getCurrentCameraId();
    }
    
    /**
     * Switch to previous camera
     */
    public String switchToPreviousCamera() {
        if (availableCameras.isEmpty()) return null;
        
        currentCameraIndex = (currentCameraIndex - 1 + availableCameras.size()) % availableCameras.size();
        return getCurrentCameraId();
    }
    
    /**
     * Switch to specific camera by ID
     */
    public boolean switchToCamera(String cameraId) {
        for (int i = 0; i < availableCameras.size(); i++) {
            if (availableCameras.get(i).id.equals(cameraId)) {
                currentCameraIndex = i;
                return true;
            }
        }
        return false;
    }
    
    /**
     * Switch to front camera
     */
    public String switchToFrontCamera() {
        for (int i = 0; i < availableCameras.size(); i++) {
            if (availableCameras.get(i).isFront) {
                currentCameraIndex = i;
                return getCurrentCameraId();
            }
        }
        return null;
    }
    
    /**
     * Switch to back camera
     */
    public String switchToBackCamera() {
        for (int i = 0; i < availableCameras.size(); i++) {
            if (availableCameras.get(i).isBack) {
                currentCameraIndex = i;
                return getCurrentCameraId();
            }
        }
        return null;
    }
    
    /**
     * Get number of available cameras
     */
    public int getCameraCount() {
        return availableCameras.size();
    }
    
    /**
     * Check if front camera is available
     */
    public boolean hasFrontCamera() {
        return availableCameras.stream().anyMatch(c -> c.isFront);
    }
    
    /**
     * Check if back camera is available
     */
    public boolean hasBackCamera() {
        return availableCameras.stream().anyMatch(c -> c.isBack);
    }
    
    /**
     * Check if current camera is front
     */
    public boolean isCurrentCameraFront() {
        CameraInfo current = getCurrentCamera();
        return current != null && current.isFront;
    }
    
    /**
     * Check if current camera is back
     */
    public boolean isCurrentCameraBack() {
        CameraInfo current = getCurrentCamera();
        return current != null && current.isBack;
    }
    
    /**
     * Get camera facing description
     */
    public String getCurrentCameraDescription() {
        CameraInfo current = getCurrentCamera();
        if (current == null) return "No camera";
        return current.isFront ? "Front Camera" : "Back Camera";
    }
    
    /**
     * Camera information class
     */
    public static class CameraInfo {
        public String id;
        public int facing;
        public boolean isFront;
        public boolean isBack;
        public int[] supportedCapabilities;
        public android.util.Size maxResolution;
        
        public String getFacingDescription() {
            switch (facing) {
                case CameraCharacteristics.LENS_FACING_BACK:
                    return "Back";
                case CameraCharacteristics.LENS_FACING_FRONT:
                    return "Front";
                case CameraCharacteristics.LENS_FACING_EXTERNAL:
                    return "External";
                default:
                    return "Unknown";
            }
        }
    }
}
