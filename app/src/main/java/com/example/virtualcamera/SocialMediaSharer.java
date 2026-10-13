package com.example.virtualcamera;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Social Media Sharer - Direct sharing to social media apps
 */
public class SocialMediaSharer {
    
    private static final String TAG = "SocialMediaSharer";
    
    private Context context;
    
    public SocialMediaSharer(Context context) {
        this.context = context;
    }
    
    /**
     * Social media app packages
     */
    public static class SocialApp {
        public String name;
        public String packageName;
        public String mimeType;
        public int iconResId;
        
        public SocialApp(String name, String packageName, String mimeType, int iconResId) {
            this.name = name;
            this.packageName = packageName;
            this.mimeType = mimeType;
            this.iconResId = iconResId;
        }
    }
    
    /**
     * Get list of supported social apps
     */
    public List<SocialApp> getSupportedApps() {
        List<SocialApp> apps = new ArrayList<>();
        
        // Instagram
        apps.add(new SocialApp(
            "Instagram",
            "com.instagram.android",
            "image/*",
            android.R.drawable.ic_menu_gallery
        ));
        
        // WhatsApp
        apps.add(new SocialApp(
            "WhatsApp",
            "com.whatsapp",
            "image/*",
            android.R.drawable.ic_menu_send
        ));
        
        // TikTok
        apps.add(new SocialApp(
            "TikTok",
            "com.zhiliaoapp.musically",
            "video/*",
            android.R.drawable.ic_menu_camera
        ));
        
        // Snapchat
        apps.add(new SocialApp(
            "Snapchat",
            "com.snapchat.android",
            "image/*",
            android.R.drawable.ic_menu_camera
        ));
        
        // Facebook
        apps.add(new SocialApp(
            "Facebook",
            "com.facebook.katana",
            "image/*",
            android.R.drawable.ic_menu_gallery
        ));
        
        // Twitter
        apps.add(new SocialApp(
            "Twitter",
            "com.twitter.android",
            "image/*",
            android.R.drawable.ic_menu_send
        ));
        
        // Telegram
        apps.add(new SocialApp(
            "Telegram",
            "org.telegram.messenger",
            "image/*",
            android.R.drawable.ic_menu_send
        ));
        
        // YouTube
        apps.add(new SocialApp(
            "YouTube",
            "com.google.android.youtube",
            "video/*",
            android.R.drawable.ic_menu_camera
        ));
        
        // Messenger
        apps.add(new SocialApp(
            "Messenger",
            "com.facebook.orca",
            "image/*",
            android.R.drawable.ic_menu_send
        ));
        
        return apps;
    }
    
    /**
     * Check if a social app is installed
     */
    public boolean isAppInstalled(String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Get list of installed social apps
     */
    public List<SocialApp> getInstalledApps() {
        List<SocialApp> installed = new ArrayList<>();
        for (SocialApp app : getSupportedApps()) {
            if (isAppInstalled(app.packageName)) {
                installed.add(app);
            }
        }
        return installed;
    }
    
    /**
     * Share image to specific social app
     */
    public void shareImageToApp(String packageName, Uri imageUri, String caption) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.setPackage(packageName);
        intent.putExtra(Intent.EXTRA_STREAM, imageUri);
        
        if (caption != null && !caption.isEmpty()) {
            intent.putExtra(Intent.EXTRA_TEXT, caption);
        }
        
        // Check if there's an app to handle this intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(intent, 0);
        
        if (!resolveInfoList.isEmpty()) {
            context.startActivity(intent);
        } else {
            Log.e(TAG, "No app found for package: " + packageName);
            // Fall back to chooser
            shareImage(imageUri, caption);
        }
    }
    
    /**
     * Share video to specific social app
     */
    public void shareVideoToApp(String packageName, Uri videoUri, String caption) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/*");
        intent.setPackage(packageName);
        intent.putExtra(Intent.EXTRA_STREAM, videoUri);
        
        if (caption != null && !caption.isEmpty()) {
            intent.putExtra(Intent.EXTRA_TEXT, caption);
        }
        
        // Check if there's an app to handle this intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(intent, 0);
        
        if (!resolveInfoList.isEmpty()) {
            context.startActivity(intent);
        } else {
            Log.e(TAG, "No app found for package: " + packageName);
            // Fall back to chooser
            shareVideo(videoUri, caption);
        }
    }
    
    /**
     * Share image to any app (shows chooser)
     */
    public void shareImage(Uri imageUri, String caption) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_STREAM, imageUri);
        
        if (caption != null && !caption.isEmpty()) {
            intent.putExtra(Intent.EXTRA_TEXT, caption);
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Image"));
    }
    
    /**
     * Share video to any app (shows chooser)
     */
    public void shareVideo(Uri videoUri, String caption) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_STREAM, videoUri);
        
        if (caption != null && !caption.isEmpty()) {
            intent.putExtra(Intent.EXTRA_TEXT, caption);
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Video"));
    }
    
    /**
     * Share to Instagram Story
     * Instagram requires special handling for stories
     */
    public void shareToInstagramStory(Uri imageUri) {
        try {
            Intent intent = new Intent("com.instagram.share.ADD_TO_STORY");
            intent.setType("image/*");
            intent.setPackage("com.instagram.android");
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sharing to Instagram Story: " + e.getMessage());
            // Fall back to regular share
            shareImage(imageUri, null);
        }
    }
    
    /**
     * Share to WhatsApp Status
     */
    public void shareToWhatsAppStatus(Uri imageUri) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.setPackage("com.whatsapp");
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sharing to WhatsApp Status: " + e.getMessage());
            shareImage(imageUri, null);
        }
    }
    
    /**
     * Share to TikTok
     */
    public void shareToTikTok(Uri videoUri) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/*");
            intent.setPackage("com.zhiliaoapp.musically");
            intent.putExtra(Intent.EXTRA_STREAM, videoUri);
            
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sharing to TikTok: " + e.getMessage());
            shareVideo(videoUri, null);
        }
    }
    
    /**
     * Create share intent for story templates
     */
    public Intent createStoryTemplateIntent(String templateType, Uri mediaUri, String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        
        switch (templateType) {
            case "instagram_story":
                intent.setType("image/*");
                intent.setPackage("com.instagram.android");
                intent.putExtra(Intent.EXTRA_STREAM, mediaUri);
                break;
            case "whatsapp_status":
                intent.setType("image/*");
                intent.setPackage("com.whatsapp");
                intent.putExtra(Intent.EXTRA_STREAM, mediaUri);
                break;
            case "tiktok":
                intent.setType("video/*");
                intent.setPackage("com.zhiliaoapp.musically");
                intent.putExtra(Intent.EXTRA_STREAM, mediaUri);
                break;
            default:
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_STREAM, mediaUri);
        }
        
        if (text != null && !text.isEmpty()) {
            intent.putExtra(Intent.EXTRA_TEXT, text);
        }
        
        return intent;
    }
    
    /**
     * Save media to temporary file for sharing
     */
    public File saveToTempFile(Uri uri, String prefix, String extension) {
        try {
            File tempDir = context.getExternalCacheDir();
            String timeStamp = String.valueOf(System.currentTimeMillis());
            String fileName = prefix + "_" + timeStamp + extension;
            
            File tempFile = new File(tempDir, fileName);
            
            // In a real implementation, copy the file from uri to tempFile
            // For now, just return the temp file path
            
            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Error creating temp file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get shareable URI for a file
     */
    public Uri getShareableUri(File file) {
        if (file == null) {
            return null;
        }
        
        // For API 24+, use FileProvider
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.virtualcamera.fileprovider",
                file
            );
        } else {
            return Uri.fromFile(file);
        }
    }
}
