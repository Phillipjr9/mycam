package com.example.virtualcamera;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.nio.ByteBuffer;

/**
 * Media Projection Service for Virtual Camera
 * 
 * This service uses MediaProjection API to create a virtual display
 * that can be used as a camera source. It supports multiple camera sources
 * including video files, static images, and physical camera.
 * 
 * How it works:
 * 1. User grants screen recording permission
 * 2. We create a virtual display
 * 3. We set a CameraSource to provide frames
 * 4. We capture frames from the source
 * 5. Other apps can capture from this virtual display
 * 
 * Limitations:
 * - Requires user permission for screen recording
 * - Virtual display appears as a separate display
 * - May not work with all apps
 * - Performance depends on device capabilities
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MediaProjectionService extends Service {
    
    private static final String TAG = "MediaProjectionSvc";
    private static final String CHANNEL_ID = "MediaProjectionChannel";
    private static final int NOTIFICATION_ID = 2;
    
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    
    private int resultCode;
    private Intent resultData;
    
    private Surface renderingSurface;
    private boolean isRunning = false;
    
    // Camera source
    private CameraSource currentSource;
    private CameraSource.SourceListener sourceListener;
    
    // License manager
    private LicenseManager licenseManager;
    
    public class LocalBinder extends Binder {
        MediaProjectionService getService() {
            return MediaProjectionService.this;
        }
    }
    
    private final IBinder binder = new LocalBinder();
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MediaProjectionService created");
        
        // Initialize license manager
        licenseManager = new LicenseManager(this);
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        startBackgroundThread();
        
        // Initialize source listener
        sourceListener = new CameraSource.SourceListener() {
            @Override
            public void onSourcePrepared() {
                Log.d(TAG, "Camera source prepared");
                startProjection();
            }
            
            @Override
            public void onSourceError(String error) {
                Log.e(TAG, "Camera source error: " + error);
                stopProjection();
            }
            
            @Override
            public void onSourceCompleted() {
                Log.d(TAG, "Camera source completed");
                // Loop the source if it's a video
                if (currentSource != null) {
                    currentSource.start();
                }
            }
        };
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            resultCode = intent.getIntExtra("resultCode", 0);
            resultData = intent.getParcelableExtra("resultData");
            
            if (resultCode != 0 && resultData != null) {
                startProjection();
            }
        }
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    public void setProjectionResult(int resultCode, Intent resultData) {
        this.resultCode = resultCode;
        this.resultData = resultData;
        startProjection();
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Set the camera source to use
     * Enforces license restrictions on certain source types
     */
    public void setCameraSource(CameraSource source) {
        // Check license for this source type
        if (!canUseSource(source)) {
            Log.e(TAG, "Cannot use this source type with current license");
            return;
        }
        
        // Stop current source
        if (currentSource != null && currentSource.isRunning()) {
            currentSource.stop();
        }
        
        currentSource = source;
        
        if (currentSource != null) {
            currentSource.setListener(sourceListener);
            // If projection is already running, start the new source
            if (isRunning && mediaProjection != null) {
                currentSource.start();
            }
        }
    }
    
    /**
     * Check if the current license allows using this camera source
     */
    private boolean canUseSource(CameraSource source) {
        if (licenseManager == null) {
            return true; // No license manager, allow everything (shouldn't happen)
        }
        
        // Free trial and non-activated users can only use basic sources
        if (!licenseManager.isActivated() || licenseManager.isExpired()) {
            // Free trial users can use basic sources
            return isBasicSource(source);
        }
        
        String licenseType = licenseManager.getLicenseType();
        
        // Lifetime users can use everything
        if (LicenseManager.LICENSE_LIFETIME.equals(licenseType)) {
            return true;
        }
        
        // Pro Yearly users can use most sources
        if (LicenseManager.LICENSE_PRO_YEARLY.equals(licenseType)) {
            return !isPremiumOnlySource(source);
        }
        
        // Pro Monthly users have limited access
        if (LicenseManager.LICENSE_PRO_MONTHLY.equals(licenseType)) {
            return isProMonthlySource(source);
        }
        
        // Free trial
        return isBasicSource(source);
    }
    
    private boolean isBasicSource(CameraSource source) {
        // Basic sources available to free trial users
        if (source instanceof PhysicalCameraSource) {
            return true;
        }
        if (source instanceof ImageSource) {
            return true;
        }
        if (source instanceof VideoSource) {
            VideoSource videoSource = (VideoSource) source;
            // Free trial can only play short videos
            return videoSource.getDuration() <= 300000; // 5 minutes
        }
        return false;
    }
    
    private boolean isProMonthlySource(CameraSource source) {
        // Pro Monthly can use everything except advanced features
        if (source instanceof VideoSource) {
            return true; // Unlimited video length
        }
        return true;
    }
    
    private boolean isPremiumOnlySource(CameraSource source) {
        // Sources only available to Lifetime license
        // For now, all sources are available to Pro Yearly
        return false;
    }
    
    /**
     * Get the current camera source
     */
    public CameraSource getCameraSource() {
        return currentSource;
    }
    
    public void startProjection() {
        if (isRunning) {
            Log.d(TAG, "Projection already running");
            return;
        }
        
        if (resultCode == 0 || resultData == null) {
            Log.e(TAG, "No projection permission");
            return;
        }
        
        try {
            MediaProjectionManager projectionManager = 
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData);
            
            if (mediaProjection == null) {
                Log.e(TAG, "Failed to get MediaProjection");
                return;
            }
            
            // Create ImageReader to capture frames
            createImageReader();
            
            // Start the camera source if set
            if (currentSource != null) {
                currentSource.start();
            } else {
                // Create virtual display with a default surface
                createVirtualDisplay();
            }
            
            isRunning = true;
            Log.d(TAG, "MediaProjection started");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting projection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void stopProjection() {
        if (!isRunning) {
            return;
        }
        
        Log.d(TAG, "Stopping projection");
        
        try {
            // Stop camera source
            if (currentSource != null && currentSource.isRunning()) {
                currentSource.stop();
            }
            
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            
            isRunning = false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping projection: " + e.getMessage());
        }
    }
    
    private void createImageReader() {
        // Create ImageReader with reasonable size
        int width = 1280;
        int height = 720;
        int maxImages = 2;
        
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, maxImages);
        
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    processImage(image);
                    image.close();
                }
            }
        }, backgroundHandler);
        
        renderingSurface = imageReader.getSurface();
        
        // If we have a camera source, use its surface
        // Otherwise, use the ImageReader's surface
        if (currentSource != null && currentSource.getSurface() != null) {
            renderingSurface = currentSource.getSurface();
        }
    }
    
    private void createVirtualDisplay() {
        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        
        // Get display metrics
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int density = metrics.densityDpi;
        int width = 1280;
        int height = 720;
        
        // Use the camera source's surface if available
        Surface displaySurface = renderingSurface;
        if (currentSource != null && currentSource.getSurface() != null) {
            displaySurface = currentSource.getSurface();
        }
        
        // Create virtual display
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "VirtualCameraDisplay",
            width, 
            height, 
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            displaySurface,
            new VirtualDisplay.Callback() {
                @Override
                public void onDisplayAdded(int displayId) {
                    Log.d(TAG, "Virtual display added: " + displayId);
                }
                
                @Override
                public void onDisplayChanged(int displayId) {
                    Log.d(TAG, "Virtual display changed: " + displayId);
                }
                
                @Override
                public void onDisplayRemoved(int displayId) {
                    Log.d(TAG, "Virtual display removed: " + displayId);
                }
            },
            backgroundHandler
        );
        
        Log.d(TAG, "Virtual display created");
    }
    
    private void processImage(Image image) {
        // This is where you would process the image
        // In a real implementation, you could:
        // 1. Modify the image (add overlays, filters, etc.)
        // 2. Save it
        // 3. Forward it to a virtual camera
        // 4. Stream it
        
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();
        
        // For now, just log that we received a frame
        Log.v(TAG, "Frame received: " + image.getWidth() + "x" + image.getHeight());
    }
    
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("MediaProjectionBackground");
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
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Media Projection Service",
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
            .setContentText("Media projection service running")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", 
                PendingIntent.getService(this, 0, 
                    new Intent(this, MediaProjectionService.class)
                        .setAction("STOP"),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
            .build();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopProjection();
        stopBackgroundThread();
        Log.d(TAG, "MediaProjectionService destroyed");
    }
}
