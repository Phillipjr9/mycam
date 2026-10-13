# Virtual Camera - Build Guide

## 📦 **HOW TO BUILD THE APK**

Since this is a sandbox environment without Android SDK, you'll need to build the APK on your local machine. Here's a complete guide:

---

## 📋 **PREREQUISITES**

### **1. Java Development Kit (JDK)**
- **Version:** JDK 8 or later (JDK 17 recommended)
- **Download:** https://www.oracle.com/java/technologies/javase-jdk17-downloads.html
- **Verify:**
  ```bash
  java -version
  ```

### **2. Android SDK**
- **Version:** Android SDK 34 (for Android 14 support)
- **Install via:** Android Studio or standalone SDK tools
- **Required packages:**
  - Android SDK Platform 34
  - Android SDK Build-Tools
  - Android Emulator
  - Android SDK Platform-Tools

### **3. Android Studio (Recommended)**
- **Version:** Latest stable version
- **Download:** https://developer.android.com/studio
- **Alternative:** Use command line tools only

### **4. Node.js (Optional)**
- Only needed if you want to run a local web server for the website
- **Download:** https://nodejs.org/

---

## 🛠️ **BUILD METHODS**

### **Method 1: Using Android Studio (EASIEST)**

1. **Open Android Studio**
2. **Import Project:**
   - Select `File > Open...`
   - Navigate to `/home/user/mycam-source-app`
   - Click "Open"

3. **Sync Gradle:**
   - Android Studio will automatically sync the project
   - Wait for "Gradle build finished" message

4. **Build APK:**
   - Click `Build > Build Bundle(s) / APK(s) > Build APK`
   - OR click the "Build APK" button in the toolbar

5. **Find APK:**
   - After build completes, click the "locate" link in the notification
   - OR navigate to: `app/build/outputs/apk/release/app-release.apk`

6. **Install on Device:**
   - Copy APK to your Android device
   - Open file manager and tap the APK
   - OR use ADB: `adb install app-release.apk`

---

### **Method 2: Using Command Line**

#### **Step 1: Set Environment Variables**

**Windows:**
```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17
set ANDROID_HOME=C:\Users\YourName\AppData\Local\Android\Sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\tools;%PATH%
```

**macOS/Linux:**
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$PATH
```

#### **Step 2: Navigate to Project**
```bash
cd /path/to/mycam-source-app
```

#### **Step 3: Set Up Gradle Wrapper**

If `gradlew` doesn't exist:
```bash
mkdir -p gradle/wrapper
gradle wrapper --gradle-version 8.2
```

Or manually download:
```bash
# Download Gradle wrapper JAR
curl -L -o gradle/wrapper/gradle-wrapper.jar \
  https://services.gradle.org/distributions/gradle-8.2-bin.zip

# Download Gradle wrapper properties
curl -L -o gradle/wrapper/gradle-wrapper.properties \
  https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.properties

# Make executable
chmod +x gradle/wrapper/gradle-wrapper.jar
```

#### **Step 4: Build Debug APK**
```bash
./gradlew clean assembleDebug
```

#### **Step 5: Build Release APK**
```bash
./gradlew clean assembleRelease
```

#### **Step 6: Find APK**
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

---

### **Method 3: Using the Build Script**

A build script is included in the project:

**Windows:**
```cmd
build.bat
```

**macOS/Linux:**
```bash
chmod +x build.sh
./build.sh
```

Note: The build script will:
1. Check for Java and Android SDK
2. Set up Gradle wrapper if needed
3. Build the debug APK

---

## 📁 **OUTPUT FILES**

After building, you'll find:

### **Debug Build:**
- Location: `app/build/outputs/apk/debug/app-debug.apk`
- Features: Full debugging, logging
- Use: Testing and development

### **Release Build:**
- Location: `app/build/outputs/apk/release/app-release.apk`
- Features: Optimized, no debugging
- Use: Production distribution

### **Bundle (Optional):**
- Build command: `./gradlew bundleRelease`
- Location: `app/build/outputs/bundle/release/app-release.aab`
- Use: Google Play Store upload

---

## 📱 **INSTALLATION**

### **Method 1: Direct Install**
1. Copy APK to your Android device
2. Open file manager
3. Tap the APK file
4. Allow installation from unknown sources if prompted
5. Complete installation

### **Method 2: ADB Install**
```bash
# Connect device
adb devices

# Install APK
adb install app/build/outputs/apk/release/app-release.apk

# Or for debug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### **Method 3: QR Code**
1. Upload APK to a file hosting service
2. Generate QR code for the download link
3. Scan with Android device
4. Download and install

---

## ⚙️ **BUILD CONFIGURATION**

### **Change App Name**
Edit `app/src/main/AndroidManifest.xml`:
```xml
<application
    android:label="Your App Name"
    ...>
```

### **Change Package Name**
Edit `app/build.gradle`:
```gradle
android {
    defaultConfig {
        applicationId "com.yourcompany.virtualcamera"
        ...
    }
}
```

### **Change Version**
Edit `app/build.gradle`:
```gradle
android {
    defaultConfig {
        versionCode 3
        versionName "2.1.0"
        ...
    }
}
```

### **Change Secret Key**
Before production, change the secret key in:
- `LicenseManager.java`
- `KeyGenerator.java`
- `website/admin-keygen.html`

---

## 🔧 **TROUBLESHOOTING**

### **Error: Gradle not found**
- Solution: Install Gradle or use the wrapper
- Command: `./gradlew` instead of `gradle`

### **Error: Java not found**
- Solution: Install JDK 8 or later
- Verify: `java -version`

### **Error: Android SDK not found**
- Solution: Install Android SDK
- Set ANDROID_HOME environment variable

### **Error: Failed to find Platform SDK**
- Solution: Install Android SDK Platform 34
- In Android Studio: Tools > SDK Manager > SDK Platforms > Android 14

### **Error: Could not find com.android.tools.build:gradle:8.1.2**
- Solution: Update Gradle plugin
- Edit `build.gradle` to use a newer version

### **Error: Duplicate class**
- Solution: Clean and rebuild
- Command: `./gradlew clean assembleRelease`

### **Error: Keystore not found**
- For release builds, you need a signing key
- Solution 1: Create one (for production)
- Solution 2: Use debug keystore (for testing)

---

## 🔐 **SIGNING FOR RELEASE**

### **Create a Keystore (One-time setup)**
```bash
keytool -genkey -v -keystore my-release-key.jks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias my-alias
```

### **Configure Signing**
Edit `app/build.gradle`:
```gradle
android {
    signingConfigs {
        release {
            storeFile file('my-release-key.jks')
            storePassword 'yourpassword'
            keyAlias 'my-alias'
            keyPassword 'yourpassword'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

---

## 📊 **BUILD OPTIONS**

| Command | Output | Use Case |
|---------|--------|----------|
| `./gradlew assembleDebug` | Debug APK | Testing & Development |
| `./gradlew assembleRelease` | Release APK | Production |
| `./gradlew bundleRelease` | App Bundle | Google Play Store |
| `./gradlew clean` | Clean build | Fix build issues |
| `./gradlew clean assembleRelease` | Clean + Release | Full rebuild |

---

## 🎯 **RECOMMENDED WORKFLOW**

### **For Testing:**
```bash
# Build debug APK
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# Run app
adb shell am start -n com.example.virtualcamera/.MainActivity
```

### **For Production:**
```bash
# Clean build
./gradlew clean

# Build release APK
./gradlew assembleRelease

# Copy APK to website
cp app/build/outputs/apk/release/app-release.apk website/downloads/VirtualCamera-v2.0.0.apk

# Deploy website
# (Upload to your hosting provider)
```

---

## 📝 **BUILD NOTES**

### **Dependencies:**
The app uses these key dependencies:
- AndroidX libraries
- CardView
- Google Play Billing (for in-app purchases)
- All are automatically downloaded by Gradle

### **Minimum SDK:**
- `minSdk 24` (Android 7.0 Nougat)
- Covers ~95% of active Android devices

### **Target SDK:**
- `targetSdk 34` (Android 14)
- Latest features and security

### **Build Tools:**
- `buildToolsVersion 34.0.0`
- `compileSdk 34`

---

## 💡 **TIPS**

1. **First Build:** Use `./gradlew clean assembleDebug` for first build
2. **Subsequent Builds:** Use `./gradlew assembleDebug` (faster)
3. **Release Builds:** Always clean before release: `./gradlew clean assembleRelease`
4. **Check Dependencies:** Use `./gradlew dependencies` to see all dependencies
5. **Update Gradle:** Periodically update Gradle wrapper for security

---

## 🎉 **YOU'RE READY TO BUILD!**

Follow the steps above to build your APK on your local machine. If you encounter any issues, check the troubleshooting section or ask for help!

**Your Virtual Camera app is ready for production!** 🚀
