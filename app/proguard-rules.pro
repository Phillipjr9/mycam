# Android ProGuard configuration
-keep class androidx.** { *; }
-keep class android.support.** { *; }

# Camera2 API classes
-keep class android.hardware.camera2.** { *; }

# Keep all activities, services, and receivers
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Keep all views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep all Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep R classes
-keep class **.R$* { *; }

# Keep native methods
-keep class * extends java.lang.Object {
    native <methods>;
}
