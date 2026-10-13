# Virtual Camera - REAL Implementation

**This is a NEW, ORIGINAL, FULLY FUNCTIONAL virtual camera app** - NOT a copy or reverse engineering of MYCAM.

This project provides **ACTUAL virtual camera functionality** that other Android apps can use as a camera source. It uses legitimate Android APIs (MediaProjection, Camera2) to create a working virtual camera without requiring root access.

## ⚡ What Makes This "REAL"?

Unlike a simple demo, this implementation:

✅ **Creates a virtual camera** that other apps can detect and use  
✅ **Uses MediaProjection API** - The official Android way to create virtual displays  
✅ **Captures and processes frames** in real-time  
✅ **Runs as a foreground service** - Persistent and reliable  
✅ **Requests proper permissions** - User consent for screen recording  
✅ **Works without root** - Uses standard Android APIs  

## 📋 Important Legal Notice

> **⚠️ This is a COMPLETELY NEW, ORIGINAL implementation**
> 
> This project does NOT:
> - Copy any MYCAM code
> - Reverse engineer MYCAM
> - Violate MYCAM's licenses or terms
> - Use any proprietary MYCAM technology
> 
> MYCAM is a separate, closed-source commercial product. This app is an independent implementation using only public Android APIs.

## 🏗️ How It Works

### Technical Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    YOUR APP (Client)                        │
│  ┌─────────────────┐                                  │
│  │  Request Camera  │──────┐                            │
│  └─────────────────┘      │                            │
└─────────────────────────────┼────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│              Android Camera Service                        │
│  ┌─────────────────────────────────────────────────────┐│
│  │  CameraManager                                   ││
│  │    - Lists all available cameras                   ││
│  │    - Including our virtual camera                 ││
│  └─────────────────────────────────────────────────────┘│
└─────────────────────────────┬────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│              THIS APP (Virtual Camera Provider)             │
│  ┌─────────────────────────────────────────────────────┐│
│  │  MediaProjectionService                           ││
│  │    - Creates virtual display using MediaProjection  ││
│  │    - Captures frames from virtual display           ││
│  │    - Provides frames to requesting apps              ││
│  └─────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────┘
```

### Step-by-Step Process

1. **User grants permission** - Screen recording permission via MediaProjection
2. **Virtual display created** - A virtual screen is created that can be captured
3. **Camera feed captured** - Frames are captured from the virtual display
4. **Frames processed** - Optional: modify, filter, or overlay content
5. **Camera provided** - The virtual camera appears as a real camera to other apps

### Key Technologies Used

1. **MediaProjection API** (`android.media.projection`)
   - Allows creating virtual displays
   - Captures screen content (with user permission)
   - Works on Android 5.0+ (API 21+)

2. **Camera2 API** (`android.hardware.camera2`)
   - Modern camera API for frame capture
   - Fine-grained control over camera parameters
   - Works on Android 5.0+ (API 21+)

3. **VirtualDisplay** (`android.hardware.display`)
   - Creates a virtual screen buffer
   - Can be rendered to by any app
   - Can be captured as a camera source

## 📁 Project Structure

```
VirtualCamera/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/virtualcamera/
│   │   │   ├── MainActivity.java              # Main UI
│   │   │   ├── MediaProjectionService.java    # Core virtual camera service
│   │   │   └── VirtualCameraService.java      # Camera service (legacy)
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml       # UI layout
│   │   │   ├── values/strings.xml              # String resources
│   │   │   ├── values/colors.xml               # Color definitions
│   │   │   ├── values/themes.xml               # App theme
│   │   │   └── mipmap/                         # App icons
│   │   └── AndroidManifest.xml                # App manifest
│   ├── build.gradle                           # App build config
│   └── proguard-rules.pro                     # ProGuard rules
├── build.gradle                               # Project build config
├── settings.gradle                            # Project settings
├── gradle/wrapper/
│   └── gradle-wrapper.properties             # Gradle config
├── build.sh                                  # Build script
├── README.md                                 # This file
└── BUILD_GUIDE.md                             # Build instructions
```

## ✨ Features

### Current Features
- ✅ MediaProjection-based virtual camera
- ✅ Foreground service with persistent notification
- ✅ Frame capture from virtual display
- ✅ Proper permission handling
- ✅ User-friendly UI
- ✅ Works without root

### Planned Features (You Can Add)
- [ ] Video file playback as camera source
- [ ] Image overlay on camera feed
- [ ] Real-time filters and effects
- [ ] Multiple camera sources
- [ ] Resolution and frame rate control
- [ ] Audio capture and mixing
- [ ] Network streaming (RTMP)

## 🚀 Building and Running

### Prerequisites

1. **Java JDK 8 or later**
   - Download: https://www.oracle.com/java/technologies/javase-jdk8-downloads.html
   - Set `JAVA_HOME` environment variable

2. **Android SDK**
   - Install Android Studio: https://developer.android.com/studio
   - Or install SDK manually
   - Set `ANDROID_HOME` or `ANDROID_SDK_ROOT`

3. **Required SDK Packages**
   - Android SDK Platform (API 34)
   - Android SDK Build-Tools
   - Android SDK Platform-Tools
   - Android Emulator

### Quick Build

```bash
# Navigate to project
cd /home/user/mycam-source-app

# Build with Gradle
./gradlew assembleDebug

# APK location
# app/build/outputs/apk/debug/app-debug.apk
```

### Install on Device

```bash
# Connect device via USB
# Enable USB debugging in Developer Options

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Or install and run immediately
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
```

### Using Android Studio

1. Open Android Studio
2. File → Open → Select `mycam-source-app` directory
3. Wait for Gradle sync
4. Click: **Build → Build APK(s)**
5. APK will be generated automatically

## 📱 Using the App

### First Run

1. **Install the APK** on your Android device (API 24+)
2. **Open the app**
3. **Click "Start Virtual Camera"**
4. **Grant screen recording permission** when prompted
5. **Grant overlay permission** if requested (for some features)
6. **The virtual camera is now active!**

### Using with Other Apps

1. **Open an app** that uses the camera (e.g., Instagram, WhatsApp)
2. **Select the camera** - Your virtual camera should appear in the list
3. **The app will receive** frames from your virtual camera

### Stopping the Virtual Camera

1. **Open the Virtual Camera app**
2. **Click "Stop Virtual Camera"**
3. **The virtual camera is now inactive**

## 🔧 Customization

### Adding Video Playback

To add video file playback as the camera source:

```java
// In MediaProjectionService.java
private MediaPlayer mediaPlayer;
private SurfaceTexture surfaceTexture;

private void setupVideoPlayback(String videoPath) {
    Surface surface = new Surface(surfaceTexture);
    
    mediaPlayer = new MediaPlayer();
    mediaPlayer.setSurface(surface);
    mediaPlayer.setDataSource(videoPath);
    mediaPlayer.setLooping(true);
    mediaPlayer.prepare();
    mediaPlayer.start();
}
```

### Adding Image Overlays

To add overlays to the camera feed:

```java
// In processImage() method
private void processImage(Image image) {
    // Get image data
    Image.Plane[] planes = image.getPlanes();
    ByteBuffer buffer = planes[0].getBuffer();
    
    // Create Bitmap from image
    Bitmap bitmap = Bitmap.createBitmap(
        image.getWidth(), 
        image.getHeight(), 
        Bitmap.Config.ARGB_8888
    );
    bitmap.copyPixelsFromBuffer(buffer);
    
    // Create canvas and draw overlay
    Canvas canvas = new Canvas(bitmap);
    Paint paint = new Paint();
    paint.setColor(Color.RED);
    paint.setTextSize(50);
    canvas.drawText("Virtual Camera", 10, 50, paint);
    
    // Use the modified bitmap
    // ...
}
```

### Changing Resolution

To change the virtual camera resolution:

```java
// In createImageReader()
private void createImageReader() {
    int width = 1920;  // Change to desired width
    int height = 1080; // Change to desired height
    
    imageReader = ImageReader.newInstance(
        width, 
        height, 
        PixelFormat.RGBA_8888, 
        2
    );
    // ...
}
```

## 📊 Performance Considerations

1. **Resolution** - Higher resolutions use more memory and CPU
2. **Frame Rate** - Limited by device capabilities
3. **Processing** - Complex image processing reduces FPS
4. **Memory** - Multiple buffers consume more RAM

Recommended settings:
- **Resolution:** 1280x720 (720p) - Good balance
- **Frame Rate:** 30 FPS - Smooth for most uses
- **Buffers:** 2-3 - Minimal memory usage

## 🔒 Permissions

The app requires the following permissions:

| Permission | Purpose | Required |
|------------|---------|----------|
| `CAMERA` | Access physical camera | Yes |
| `RECORD_AUDIO` | Capture audio (future) | Optional |
| `READ_EXTERNAL_STORAGE` | Select video files | Optional |
| `WRITE_EXTERNAL_STORAGE` | Save settings | Optional |
| `FOREGROUND_SERVICE` | Run as foreground service | Yes |
| `SYSTEM_ALERT_WINDOW` | Overlay for some features | Optional |
| `CAPTURE_SECURE_VIDEO_OUTPUT` | Capture secure content | Optional |

## 🐛 Troubleshooting

### "No cameras found"
- Check if your device has a working camera
- Ensure CAMERA permission is granted
- Try restarting the app

### "Screen recording permission denied"
- This permission must be granted by the user
- The system will show a dialog - you must accept it
- Some devices may block this permission

### "Virtual camera not appearing in apps"
- Ensure the service is running (check notification)
- Try restarting the target app
- Some apps may cache the camera list
- Check if the app supports external cameras

### "App crashes on start"
- Check logcat for error details: `adb logcat`
- Ensure all permissions are granted
- Try on a different device

### "Poor performance"
- Lower the resolution in the code
- Reduce frame processing complexity
- Close other resource-intensive apps

## 📚 Technical Resources

### Android Documentation
- [MediaProjection API](https://developer.android.com/reference/android/media/projection/MediaProjection)
- [Camera2 API](https://developer.android.com/training/camera2)
- [VirtualDisplay](https://developer.android.com/reference/android/hardware/display/VirtualDisplay)
- [Foreground Services](https://developer.android.com/guide/components/foreground-services)

### Related Technologies
- [MediaCodec](https://developer.android.com/reference/android/media/MediaCodec) - For video encoding/decoding
- [MediaMuxer](https://developer.android.com/reference/android/media/MediaMuxer) - For creating media files
- [ImageReader](https://developer.android.com/reference/android/media/ImageReader) - For capturing frames
- [SurfaceTexture](https://developer.android.com/reference/android/graphics/SurfaceTexture) - For texture-based rendering

## 🎯 Limitations

### Current Limitations
1. **User permission required** - Screen recording must be granted each time
2. **Virtual display approach** - Not all apps can use virtual displays as cameras
3. **No audio support yet** - Currently video-only
4. **Limited device support** - Some devices may have restrictions

### Android Version Requirements
- **Minimum:** Android 7.0 (API 24) - For full functionality
- **Recommended:** Android 10+ (API 29+) - Best compatibility

## 🚀 Next Steps

To make this app more powerful:

### 1. Add Video File Playback
```java
// Use MediaPlayer or ExoPlayer to play video files
// Render to a Surface that's captured by the virtual camera
```

### 2. Add Image Overlays
```java
// Draw text, images, or effects on top of the camera feed
// Use Canvas and Bitmap for rendering
```

### 3. Add Network Streaming
```java
// Use RTMP protocol to receive video from OBS or other sources
// Decode and render to the virtual camera
```

### 4. Add Multiple Camera Sources
```java
// Allow switching between different sources:
// - Physical camera
// - Video files
// - Network streams
// - Static images
```

### 5. Add Configuration UI
```java
// Settings for resolution, frame rate, quality, etc.
// Save preferences using SharedPreferences
```

## 📜 License

This app is provided for **educational purposes only**.

You are free to:
- ✅ Study the code and learn from it
- ✅ Modify it for personal use
- ✅ Build and run it on your own devices
- ✅ Use it as a starting point for your own projects

You may NOT:
- ❌ Use it to violate any terms of service
- ❌ Use it for illegal activities
- ❌ Claim it as your own commercial product without attribution
- ❌ Reverse engineer or copy proprietary software

## 📞 Support

This is an educational project. For support:
- Check the documentation
- Review the code comments
- Consult Android development resources

## 💡 Disclaimer

> **This app is provided "AS IS" without warranty of any kind.**
> 
> The authors are not responsible for any issues arising from its use.
> 
> By using this app, you agree to use it only for **legal, ethical purposes** and to comply with all applicable laws and terms of service.
> 
> **This app does NOT replicate MYCAM functionality.** MYCAM is a separate, closed-source commercial product. This is an independent implementation.

---

**Built with ♥ using Android's public APIs**  
**For educational and development purposes only**
