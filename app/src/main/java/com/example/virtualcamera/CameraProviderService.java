package com.example.virtualcamera;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.impl.CameraDeviceImpl;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Virtual Camera Provider Service
 * 
 * This service provides a virtual camera that other apps can use.
 * It implements the CameraProvider interface to create virtual camera devices.
 * 
 * NOTE: This implementation uses reflection to access hidden Android APIs
 * for creating virtual cameras. This is necessary because Android does not
 * provide public APIs for creating virtual camera providers.
 * 
 * This approach works on Android 10+ (API 29+) where the CameraProvider
 * interface was introduced.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraProviderService extends android.hardware.camera2.CameraProvider {
    
    private static final String TAG = "CameraProviderService";
    
    // Virtual camera ID
    public static final String VIRTUAL_CAMERA_ID = "virtual_camera_0";
    
    // List of connected camera devices
    private final List<CameraDevice> cameraDevices = new CopyOnWriteArrayList<>();
    
    // Camera manager for accessing real camera characteristics
    private CameraManager cameraManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "CameraProviderService created");
        
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        
        // Initialize virtual camera
        initializeVirtualCamera();
    }
    
    /**
     * Initialize the virtual camera device
     */
    private void initializeVirtualCamera() {
        try {
            // Get characteristics from a real camera to use as template
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds.length == 0) {
                Log.e(TAG, "No real cameras found to use as template");
                return;
            }
            
            String templateCameraId = cameraIds[0];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(templateCameraId);
            
            // Create virtual camera characteristics
            CameraCharacteristics virtualCharacteristics = createVirtualCameraCharacteristics(characteristics);
            
            // Create and register virtual camera device
            VirtualCameraDevice virtualCamera = new VirtualCameraDevice(
                this, 
                VIRTUAL_CAMERA_ID, 
                virtualCharacteristics,
                cameraManager
            );
            
            cameraDevices.add(virtualCamera);
            
            // Register the virtual camera with the system
            registerVirtualCamera(VIRTUAL_CAMERA_ID, virtualCamera);
            
            Log.d(TAG, "Virtual camera initialized: " + VIRTUAL_CAMERA_ID);
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing virtual camera: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create characteristics for the virtual camera
     */
    private CameraCharacteristics createVirtualCameraCharacteristics(CameraCharacteristics template) {
        CameraCharacteristics.Builder builder = new CameraCharacteristics.Builder();
        
        // Copy basic characteristics from template
        builder.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, 
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        
        // Set camera facing (we'll use external for virtual camera)
        builder.set(CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_EXTERNAL);
        
        // Set sensor characteristics
        builder.set(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT, 
            CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGGB);
        builder.set(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE, new android.util.Size(1920, 1080));
        builder.set(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE, new android.util.SizeF(5.0f, 3.75f));
        
        // Set available capabilities
        int[] capabilities = new int[] {
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
        };
        builder.set(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, capabilities);
        
        // Set available stream configurations
        builder.set(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP, 
            createStreamConfigurationMap());
        
        // Set available request keys
        builder.set(CameraCharacteristics.REQUEST_AVAILABLE_REQUEST_KEYS, 
            getAvailableRequestKeys());
        
        // Set available result keys
        builder.set(CameraCharacteristics.REQUEST_AVAILABLE_RESULT_KEYS, 
            getAvailableResultKeys());
        
        // Set available characteristics keys
        builder.set(CameraCharacteristics.REQUEST_AVAILABLE_CHARACTERISTICS_KEYS, 
            getAvailableCharacteristicsKeys());
        
        return builder.build();
    }
    
    private android.hardware.camera2.params.StreamConfigurationMap createStreamConfigurationMap() {
        // Create a stream configuration map with common sizes and formats
        android.hardware.camera2.params.StreamConfigurationMap map = 
            new android.hardware.camera2.params.StreamConfigurationMap();
        
        // Add common output sizes
        int[] formats = new int[] {
            ImageFormat.YUV_420_888,
            ImageFormat.JPEG,
            android.graphics.ImageFormat.NV21
        };
        
        android.util.Size[] sizes = new android.util.Size[] {
            new android.util.Size(1920, 1080),
            new android.util.Size(1280, 720),
            new android.util.Size(640, 480),
            new android.util.Size(320, 240)
        };
        
        for (int format : formats) {
            for (android.util.Size size : sizes) {
                map.addOutputSize(format, size);
            }
        }
        
        // Add input sizes (for recording)
        for (android.util.Size size : sizes) {
            map.addInputSize(ImageFormat.YUV_420_888, size);
        }
        
        return map;
    }
    
    private int[] getAvailableRequestKeys() {
        return new int[] {
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AWB_MODE,
            CaptureRequest.CONTROL_MODE,
            CaptureRequest.SENSOR_FRAME_DURATION,
            CaptureRequest.SENSOR_EXPOSURE_TIME,
            CaptureRequest.SENSOR_SENSITIVITY,
            CaptureRequest.FLASH_MODE,
            CaptureRequest.LENS_APERTURE,
            CaptureRequest.LENS_FOCUS_DISTANCE,
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE
        };
    }
    
    private int[] getAvailableResultKeys() {
        return new int[] {
            CaptureResult.CONTROL_AF_STATE,
            CaptureResult.CONTROL_AE_STATE,
            CaptureResult.CONTROL_AWB_STATE,
            CaptureResult.SENSOR_FRAME_DURATION,
            CaptureResult.SENSOR_EXPOSURE_TIME,
            CaptureResult.SENSOR_SENSITIVITY,
            TotalCaptureResult.REQUEST_ID
        };
    }
    
    private int[] getAvailableCharacteristicsKeys() {
        return new int[] {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
            CameraCharacteristics.LENS_FACING,
            CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT,
            CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE,
            CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE,
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES,
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        };
    }
    
    /**
     * Register virtual camera with the system using reflection
     */
    private void registerVirtualCamera(String cameraId, CameraDevice device) {
        try {
            // Use reflection to access hidden CameraManager methods
            java.lang.reflect.Method addCameraMethod = CameraManager.class.getDeclaredMethod(
                "addCamera", String.class, CameraDevice.class, CameraCharacteristics.class
            );
            addCameraMethod.setAccessible(true);
            
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            addCameraMethod.invoke(cameraManager, cameraId, device, characteristics);
            
            Log.d(TAG, "Virtual camera registered: " + cameraId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error registering virtual camera: " + e.getMessage());
            // Fallback: try alternative method
            tryAlternativeRegistration(cameraId, device);
        }
    }
    
    /**
     * Try alternative registration method
     */
    private void tryAlternativeRegistration(String cameraId, CameraDevice device) {
        try {
            // Try using CameraService
            Object cameraService = getCameraService();
            if (cameraService != null) {
                java.lang.reflect.Method addCameraMethod = cameraService.getClass().getDeclaredMethod(
                    "addCamera", String.class, CameraDevice.class
                );
                addCameraMethod.setAccessible(true);
                addCameraMethod.invoke(cameraService, cameraId, device);
                Log.d(TAG, "Virtual camera registered via CameraService");
            }
        } catch (Exception e) {
            Log.e(TAG, "Alternative registration failed: " + e.getMessage());
            Log.w(TAG, "Virtual camera may not be visible to other apps without root");
        }
    }
    
    /**
     * Get CameraService instance using reflection
     */
    private Object getCameraService() {
        try {
            Class<?> cameraManagerClass = Class.forName("android.hardware.camera2.CameraManager");
            java.lang.reflect.Field mServiceField = cameraManagerClass.getDeclaredField("mService");
            mServiceField.setAccessible(true);
            return mServiceField.get(cameraManager);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public synchronized void onBind(@NonNull Intent intent) {
        super.onBind(intent);
        Log.d(TAG, "CameraProviderService bound");
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "CameraProviderService unbound");
        return super.onUnbind(intent);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "CameraProviderService destroyed");
        
        // Clean up camera devices
        for (CameraDevice device : cameraDevices) {
            try {
                device.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing camera device: " + e.getMessage());
            }
        }
        cameraDevices.clear();
    }
    
    /**
     * Get the list of camera IDs including our virtual camera
     */
    @Override
    public String[] getCameraIdList() {
        try {
            List<String> cameraIds = new ArrayList<>();
            
            // Add real cameras
            String[] realCameras = cameraManager.getCameraIdList();
            cameraIds.addAll(Arrays.asList(realCameras));
            
            // Add virtual camera
            cameraIds.add(VIRTUAL_CAMERA_ID);
            
            return cameraIds.toArray(new String[0]);
        } catch (CameraAccessException e) {
            return new String[0];
        }
    }
    
    /**
     * Get characteristics for a camera (including virtual)
     */
    @Override
    public CameraCharacteristics getCameraCharacteristics(String cameraId) throws CameraAccessException {
        if (VIRTUAL_CAMERA_ID.equals(cameraId)) {
            // Return our virtual camera characteristics
            for (CameraDevice device : cameraDevices) {
                if (device instanceof VirtualCameraDevice) {
                    VirtualCameraDevice vDevice = (VirtualCameraDevice) device;
                    return vDevice.getCharacteristics();
                }
            }
        }
        
        // Return real camera characteristics
        return cameraManager.getCameraCharacteristics(cameraId);
    }
    
    /**
     * Open a camera (including virtual)
     */
    @Override
    public synchronized void openCamera(String cameraId, CameraDevice.StateCallback callback, 
            Handler handler) throws CameraAccessException {
        
        if (VIRTUAL_CAMERA_ID.equals(cameraId)) {
            // Open our virtual camera
            for (CameraDevice device : cameraDevices) {
                if (device instanceof VirtualCameraDevice) {
                    VirtualCameraDevice vDevice = (VirtualCameraDevice) device;
                    callback.onOpened(vDevice);
                    return;
                }
            }
            throw new CameraAccessException(CameraAccessException.CAMERA_IN_USE, 
                "Virtual camera not available");
        }
        
        // Open real camera
        cameraManager.openCamera(cameraId, callback, handler);
    }
}
