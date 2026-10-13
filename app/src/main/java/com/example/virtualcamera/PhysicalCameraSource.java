package com.example.virtualcamera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceTexture;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Physical Camera Source - Uses the device's physical camera as a source
 * 
 * This class provides access to the physical camera and can apply
 * processing to the frames before they're sent to the virtual camera.
 */
public class PhysicalCameraSource implements CameraSource {
    
    private static final String TAG = "PhysicalCameraSource";
    
    private Context context;
    private String cameraId;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    
    private boolean isRunning = false;
    private SourceListener listener;
    
    // Camera characteristics
    private CameraCharacteristics cameraCharacteristics;
    
    public PhysicalCameraSource(Context context, String cameraId) {
        this.context = context;
        this.cameraId = cameraId;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        
        try {
            this.cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error getting camera characteristics: " + e.getMessage());
        }
    }
    
    @Override
    public Surface getSurface() {
        return surface;
    }
    
    @Override
    public void start() {
        if (isRunning) {
            Log.d(TAG, "Physical camera already running");
            return;
        }
        
        startBackgroundThread();
        openCamera();
    }
    
    @Override
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        try {
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }
            
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
            
            stopBackgroundThread();
            
            if (surface != null) {
                surface.release();
                surface = null;
            }
            
            if (surfaceTexture != null) {
                surfaceTexture.release();
                surfaceTexture = null;
            }
            
            isRunning = false;
            Log.d(TAG, "Physical camera stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping physical camera: " + e.getMessage());
        }
    }
    
    @Override
    public void pause() {
        // Physical camera doesn't have a simple pause
        // We would need to stop and restart
        stop();
    }
    
    @Override
    public void resume() {
        start();
    }
    
    @Override
    public boolean isRunning() {
        return isRunning;
    }
    
    @Override
    public void setListener(SourceListener listener) {
        this.listener = listener;
    }
    
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("PhysicalCameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
    
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }
    
    private void openCamera() {
        try {
            Log.d(TAG, "Opening camera: " + cameraId);
            
            // Create SurfaceTexture
            surfaceTexture = new SurfaceTexture(10);
            surface = new Surface(surfaceTexture);
            
            final Semaphore cameraOpenSemaphore = new Semaphore(1);
            
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    try {
                        createCameraPreviewSession();
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error creating preview session: " + e.getMessage());
                        if (listener != null) {
                            listener.onSourceError(e.getMessage());
                        }
                    }
                    cameraOpenSemaphore.release();
                }
                
                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice.close();
                    cameraDevice = null;
                    cameraOpenSemaphore.release();
                    if (listener != null) {
                        listener.onSourceError("Camera disconnected");
                    }
                }
                
                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    cameraDevice.close();
                    cameraDevice = null;
                    Log.e(TAG, "Camera error: " + error);
                    cameraOpenSemaphore.release();
                    if (listener != null) {
                        listener.onSourceError("Camera error: " + error);
                    }
                }
            }, backgroundHandler);
            
            // Wait for camera to open
            try {
                if (!cameraOpenSemaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Time out waiting to open camera");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting to open camera", e);
            }
            
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error opening camera: " + e.getMessage());
            if (listener != null) {
                listener.onSourceError(e.getMessage());
            }
        }
    }
    
    private void createCameraPreviewSession() throws CameraAccessException {
        if (cameraDevice == null || surface == null) {
            return;
        }
        
        // Create capture request builder
        CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW
        );
        
        // Add surface as target
        previewRequestBuilder.addTarget(surface);
        
        // Create capture session
        List<Surface> outputSurfaces = Arrays.asList(surface);
        
        cameraDevice.createCaptureSession(
            outputSurfaces,
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        captureSession = session;
                        // Start preview
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        captureSession.setRepeatingRequest(previewRequest, null, backgroundHandler);
                        
                        isRunning = true;
                        Log.d(TAG, "Camera preview session configured");
                        
                        if (listener != null) {
                            listener.onSourcePrepared();
                        }
                        
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error starting preview: " + e.getMessage());
                        if (listener != null) {
                            listener.onSourceError(e.getMessage());
                        }
                    }
                }
                
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Camera session configuration failed");
                    if (listener != null) {
                        listener.onSourceError("Session configuration failed");
                    }
                }
            },
            backgroundHandler
        );
    }
    
    public void switchCamera(String newCameraId) {
        if (cameraId.equals(newCameraId)) {
            return;
        }
        
        stop();
        this.cameraId = newCameraId;
        try {
            this.cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error getting characteristics for camera " + newCameraId);
        }
        start();
    }
    
    public String getCameraId() {
        return cameraId;
    }
    
    public CameraCharacteristics getCameraCharacteristics() {
        return cameraCharacteristics;
    }
    
    public static String[] getAvailableCameras(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            return cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            return new String[0];
        }
    }
    
    public static String getBackCameraId(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String id : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == 
                    CameraCharacteristics.LENS_FACING_BACK) {
                    return id;
                }
            }
            // Return first camera if no back camera found
            return cameraIds.length > 0 ? cameraIds[0] : null;
        } catch (CameraAccessException e) {
            return null;
        }
    }
    
    public static String getFrontCameraId(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String id : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == 
                    CameraCharacteristics.LENS_FACING_FRONT) {
                    return id;
                }
            }
            return null;
        } catch (CameraAccessException e) {
            return null;
        }
    }
}
