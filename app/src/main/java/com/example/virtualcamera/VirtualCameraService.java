package com.example.virtualcamera;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Virtual Camera Service
 * 
 * This service demonstrates how to create a virtual camera feed.
 * It captures frames from the device camera and can be used as a basis
 * for understanding virtual camera implementations.
 * 
 * NOTE: This is a simplified demo and does NOT replicate MYCAM's full functionality.
 */
public class VirtualCameraService extends Service {
    
    public static final String ACTION_START = "com.example.virtualcamera.ACTION_START";
    public static final String ACTION_STOP = "com.example.virtualcamera.ACTION_STOP";
    
    private static final String TAG = "VirtualCameraService";
    private static final String CHANNEL_ID = "VirtualCameraChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private final IBinder binder = new LocalBinder();
    
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewRequestBuilder;
    private String cameraId;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    
    private ImageReader imageReader;
    private SurfaceTexture surfaceTexture;
    private Surface previewSurface;
    
    private boolean isRunning = false;
    
    public class LocalBinder extends Binder {
        VirtualCameraService getService() {
            return VirtualCameraService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Virtual Camera Service created");
        
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START.equals(intent.getAction())) {
            startVirtualCamera();
        } else if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopVirtualCamera();
        }
        
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public void startVirtualCamera() {
        if (isRunning) {
            Log.d(TAG, "Virtual camera already running");
            return;
        }
        
        Log.d(TAG, "Starting virtual camera");
        
        try {
            // Get camera ID
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds.length == 0) {
                Log.e(TAG, "No cameras found");
                return;
            }
            
            // Use back-facing camera
            cameraId = cameraIds[0];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            
            // Check if camera supports the features we need
            int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            if (capabilities == null) {
                Log.e(TAG, "Camera capabilities not available");
                return;
            }
            
            // Start background thread
            startBackgroundThread();
            
            // Open camera
            openCamera();
            
            isRunning = true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting virtual camera: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void stopVirtualCamera() {
        if (!isRunning) {
            return;
        }
        
        Log.d(TAG, "Stopping virtual camera");
        
        try {
            // Close capture session
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }
            
            // Close camera device
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
            
            // Close image reader
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            
            // Stop background thread
            stopBackgroundThread();
            
            isRunning = false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping virtual camera: " + e.getMessage());
        }
    }
    
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
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
            
            // Create image reader for capturing frames
            Size previewSize = getPreviewSize();
            imageReader = ImageReader.newInstance(
                previewSize.getWidth(), 
                previewSize.getHeight(), 
                ImageFormat.YUV_420_888, 
                2
            );
            
            imageReader.setOnImageAvailableListener(
                new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Image image = reader.acquireLatestImage();
                        if (image != null) {
                            // Process the image frame
                            // In a real virtual camera, you would forward this to the virtual camera
                            image.close();
                        }
                    }
                }, 
                backgroundHandler
            );
            
            // Create surface for preview
            surfaceTexture = new SurfaceTexture(10);
            previewSurface = new Surface(surfaceTexture);
            
            // Open camera with callback
            final Semaphore cameraOpenSemaphore = new Semaphore(1);
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    try {
                        createCameraPreviewSession();
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error creating preview session: " + e.getMessage());
                    }
                    cameraOpenSemaphore.release();
                }
                
                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice.close();
                    cameraDevice = null;
                    cameraOpenSemaphore.release();
                }
                
                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    cameraDevice.close();
                    cameraDevice = null;
                    Log.e(TAG, "Camera error: " + error);
                    cameraOpenSemaphore.release();
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
        }
    }
    
    private void createCameraPreviewSession() throws CameraAccessException {
        if (cameraDevice == null || imageReader == null || previewSurface == null) {
            return;
        }
        
        // Create capture request builder
        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        
        // Add targets
        previewRequestBuilder.addTarget(previewSurface);
        previewRequestBuilder.addTarget(imageReader.getSurface());
        
        // Create capture session
        List<Surface> outputSurfaces = Arrays.asList(
            previewSurface,
            imageReader.getSurface()
        );
        
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
                        Log.d(TAG, "Camera preview session configured");
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error starting preview: " + e.getMessage());
                    }
                }
                
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Camera session configuration failed");
                }
            },
            backgroundHandler
        );
    }
    
    private Size getPreviewSize() {
        // Return a reasonable preview size
        return new Size(1280, 720);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Virtual Camera Service",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Virtual Camera")
            .setContentText("Virtual camera service is running")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .build();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopVirtualCamera();
        Log.d(TAG, "Virtual Camera Service destroyed");
    }
}
