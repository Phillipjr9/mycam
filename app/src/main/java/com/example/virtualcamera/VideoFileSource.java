package com.example.virtualcamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

/**
 * Video File Source - Plays video files as camera source
 * Supports looping, speed control, and other video manipulation
 */
public class VideoFileSource implements CameraSource {
    
    private static final String TAG = "VideoFileSource";
    
    private Context context;
    private String videoPath;
    private MediaPlayer mediaPlayer;
    private SurfaceTexture surfaceTexture;
    private Surface surface;
    
    private boolean isRunning = false;
    private boolean isLooping = true;
    private float playbackSpeed = 1.0f;
    private SourceListener listener;
    
    private int videoWidth = 1280;
    private int videoHeight = 720;
    private long duration = 0;
    private long currentPosition = 0;
    
    public VideoFileSource(Context context, String videoPath) {
        this.context = context;
        this.videoPath = videoPath;
        
        setupSurfaceTexture();
    }
    
    private void setupSurfaceTexture() {
        // Create surface texture for video rendering
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        
        surfaceTexture = new SurfaceTexture(textures[0]);
        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                // Frame is available, can be used for virtual camera
                if (listener != null) {
                    listener.onSourcePrepared();
                }
            }
        });
        
        surface = new Surface(surfaceTexture);
    }
    
    @Override
    public void start() {
        if (isRunning) {
            Log.d(TAG, "Already running");
            return;
        }
        
        try {
            // Create media player
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setSurface(surface);
            mediaPlayer.setLooping(isLooping);
            
            // Set data source
            if (videoPath.startsWith("http://") || videoPath.startsWith("https://")) {
                mediaPlayer.setDataSource(videoPath);
            } else {
                mediaPlayer.setDataSource(context, Uri.parse(videoPath));
            }
            
            mediaPlayer.prepare();
            
            // Get video info
            videoWidth = mediaPlayer.getVideoWidth();
            videoHeight = mediaPlayer.getVideoHeight();
            duration = mediaPlayer.getDuration();
            
            // Start playback
            mediaPlayer.start();
            isRunning = true;
            
            if (listener != null) {
                listener.onSourcePrepared();
            }
            
            Log.d(TAG, "Video source started: " + videoPath);
            
        } catch (IOException e) {
            Log.e(TAG, "Error starting video source: " + e.getMessage());
            if (listener != null) {
                listener.onSourceError(e.getMessage());
            }
        }
    }
    
    @Override
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            
            if (surfaceTexture != null) {
                surfaceTexture.release();
                surfaceTexture = null;
            }
            
            if (surface != null) {
                surface.release();
                surface = null;
            }
            
            isRunning = false;
            
            if (listener != null) {
                listener.onSourceCompleted();
            }
            
            Log.d(TAG, "Video source stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping video source: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isRunning() {
        return isRunning && mediaPlayer != null;
    }
    
    @Override
    public void setListener(SourceListener listener) {
        this.listener = listener;
    }
    
    /**
     * Set looping mode
     */
    public void setLooping(boolean looping) {
        isLooping = looping;
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(looping);
        }
    }
    
    /**
     * Check if looping
     */
    public boolean isLooping() {
        return isLooping;
    }
    
    /**
     * Set playback speed (0.5x to 2.0x)
     * Note: Not all devices support this
     */
    public void setPlaybackSpeed(float speed) {
        playbackSpeed = Math.max(0.5f, Math.min(2.0f, speed));
        if (mediaPlayer != null) {
            // MediaPlayer doesn't support speed control directly
            // For actual speed control, use ExoPlayer or AudioTrack
        }
    }
    
    /**
     * Get playback speed
     */
    public float getPlaybackSpeed() {
        return playbackSpeed;
    }
    
    /**
     * Get video width
     */
    public int getVideoWidth() {
        return videoWidth;
    }
    
    /**
     * Get video height
     */
    public int getVideoHeight() {
        return videoHeight;
    }
    
    /**
     * Get video duration in milliseconds
     */
    public long getDuration() {
        return duration;
    }
    
    /**
     * Get current position in milliseconds
     */
    public long getCurrentPosition() {
        if (mediaPlayer != null) {
            try {
                currentPosition = mediaPlayer.getCurrentPosition();
            } catch (Exception e) {
                // Ignore
            }
        }
        return currentPosition;
    }
    
    /**
     * Seek to position
     */
    public void seekTo(long position) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo((int) position);
            } catch (Exception e) {
                Log.e(TAG, "Error seeking: " + e.getMessage());
            }
        }
    }
    
    /**
     * Pause playback
     */
    public void pause() {
        if (mediaPlayer != null && isRunning) {
            try {
                mediaPlayer.pause();
            } catch (Exception e) {
                Log.e(TAG, "Error pausing: " + e.getMessage());
            }
        }
    }
    
    /**
     * Resume playback
     */
    public void resume() {
        if (mediaPlayer != null && isRunning) {
            try {
                mediaPlayer.start();
            } catch (Exception e) {
                Log.e(TAG, "Error resuming: " + e.getMessage());
            }
        }
    }
    
    /**
     * Set video path
     */
    public void setVideoPath(String path) {
        this.videoPath = path;
    }
    
    /**
     * Get video path
     */
    public String getVideoPath() {
        return videoPath;
    }
    
    /**
     * Get surface for rendering
     */
    public Surface getSurface() {
        return surface;
    }
    
    /**
     * Get surface texture
     */
    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }
}
