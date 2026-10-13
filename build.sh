#!/bin/bash

# Build script for Virtual Camera Demo App
# This script helps build the project without needing Android Studio

echo "Virtual Camera Demo App Build Script"
echo "===================================="
echo ""

# Check if we're in the right directory
if [ ! -f "settings.gradle" ]; then
    echo "Error: Please run this script from the project root directory"
    exit 1
fi

# Check for Java
if [ -z "$JAVA_HOME" ]; then
    echo "JAVA_HOME is not set. Please set JAVA_HOME to your JDK 8+ installation."
    exit 1
fi

echo "JAVA_HOME: $JAVA_HOME"
echo ""

# Check Java version
JAVA_VERSION=$($JAVA_HOME/bin/java -version 2>&1 | head -1 | cut -d'"' -f2)
echo "Java version: $JAVA_VERSION"

if [[ "$JAVA_VERSION" != "1.8"* ]] && [[ "$JAVA_VERSION" != "9"* ]] && [[ "$JAVA_VERSION" != "10"* ]] && [[ "$JAVA_VERSION" != "11"* ]]; then
    echo "Warning: Java 8 or later is recommended"
fi

echo ""

# Check for Android SDK
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "ANDROID_HOME or ANDROID_SDK_ROOT is not set."
    echo "Please set ANDROID_HOME to your Android SDK installation."
    exit 1
fi

SDK_ROOT=${ANDROID_SDK_ROOT:-$ANDROID_HOME}
echo "Android SDK: $SDK_ROOT"
echo ""

# Set up Gradle wrapper if it doesn't exist
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "Setting up Gradle wrapper..."
    mkdir -p gradle/wrapper
    
    # Download Gradle wrapper jar
    echo "Downloading Gradle wrapper..."
    if command -v curl &> /dev/null; then
        curl -L -o gradle/wrapper/gradle-wrapper.jar https://services.gradle.org/distributions/gradle-8.2-bin.zip
    elif command -v wget &> /dev/null; then
        wget -O gradle/wrapper/gradle-wrapper.jar https://services.gradle.org/distributions/gradle-8.2-bin.zip
    else
        echo "Error: Neither curl nor wget is available. Please install one of them."
        exit 1
    fi
fi

# Make Gradle wrapper executable
chmod +x gradle/wrapper/gradle-wrapper.jar

# Build the project
echo "Building the project..."
echo ""

if [ -f "gradlew" ]; then
    ./gradlew clean assembleDebug
else
    echo "gradlew not found. Using gradle wrapper directly..."
    java -jar gradle/wrapper/gradle-wrapper.jar clean assembleDebug
fi

echo ""
echo "Build complete!"
echo ""
echo "Output APK location:"
echo "  app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "To install on a device:"
echo "  adb install app/build/outputs/apk/debug/app-debug.apk"
