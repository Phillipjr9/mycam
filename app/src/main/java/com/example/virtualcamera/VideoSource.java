package com.example.virtualcamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

/**
 * Video Source - Plays video files as the virtual camera source
 * 
 * This class handles video file playback and provides a Surface
 * that can be used as the input for the virtual camera.
 */
public class VideoSource implements CameraSource {
    
    private static final String TAG = "VideoSource";
    
    private Context context;
    private String videoPath;
    private MediaPlayer mediaPlayer;
    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private boolean isPlaying = false;
    private boolean isLooping = true;
    private float playbackSpeed = 1.0f;
    
    // Listener for source events
    private SourceListener listener;
    
    public interface SourceListener {
        void onSourcePrepared();
        void onSourceError(String error);
        void onSourceCompleted();
    }
    
    public VideoSource(Context context, String videoPath) {
        this.context = context;
        this.videoPath = videoPath;
    }
    
    @Override
    public Surface getSurface() {
        return surface;
    }
    
    @Override
    public void start() {
        if (isPlaying) {
            Log.d(TAG, "Video already playing");
            return;
        }
        
        prepareVideo();
    }
    
    @Override
    public void stop() {
        if (!isPlaying) {
            return;
        }
        
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            
            if (surface != null) {
                surface.release();
                surface = null;
            }
            
            if (surfaceTexture != null) {
                surfaceTexture.release();
                surfaceTexture = null;
            }
            
            isPlaying = false;
            Log.d(TAG, "Video stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping video: " + e.getMessage());
        }
    }
    
    @Override
    public void pause() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            Log.d(TAG, "Video paused");
        }
    }
    
    @Override
    public void resume() {
        if (mediaPlayer != null && !isPlaying) {
            mediaPlayer.start();
            isPlaying = true;
            Log.d(TAG, "Video resumed");
        }
    }
    
    @Override
    public boolean isRunning() {
        return isPlaying;
    }
    
    @Override
    public void setListener(SourceListener listener) {
        this.listener = listener;
    }
    
    private void prepareVideo() {
        try {
            // Create SurfaceTexture
            surfaceTexture = new SurfaceTexture(10);
            surface = new Surface(surfaceTexture);
            
            // Create MediaPlayer
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setSurface(surface);
            mediaPlayer.setLooping(isLooping);
            
            // Set up completion listener
            mediaPlayer.setOnCompletionListener(mp -> {
                if (listener != null) {
                    listener.onSourceCompleted();
                }
                if (isLooping) {
                    mp.seekTo(0);
                    mp.start();
                }
            });
            
            // Set up error listener
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                if (listener != null) {
                    listener.onSourceError("MediaPlayer error: " + what);
                }
                return true;
            });
            
            // Set up prepared listener
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "Video prepared");
                mp.start();
                isPlaying = true;
                if (listener != null) {
                    listener.onSourcePrepared();
                }
            });
            
            // Set data source
            if (videoPath.startsWith("http://") || videoPath.startsWith("https://")) {
                mediaPlayer.setDataSource(videoPath);
            } else {
                mediaPlayer.setDataSource(context, Uri.parse(videoPath));
            }
            
            // Prepare async
            mediaPlayer.prepareAsync();
            
            Log.d(TAG, "Video preparation started: " + videoPath);
            
        } catch (IOException e) {
            Log.e(TAG, "Error preparing video: " + e.getMessage());
            if (listener != null) {
                listener.onSourceError(e.getMessage());
            }
            cleanup();
        }
    }
    
    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
        // If already playing, restart with new video
        if (isPlaying) {
            stop();
            prepareVideo();
        }
    }
    
    public void setLooping(boolean looping) {
        this.isLooping = looping;
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(looping);
        }
    }
    
    public void setPlaybackSpeed(float speed) {
        this.playbackSpeed = speed;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mediaPlayer != null) {
                mediaPlayer.setPlaybackParams(
                    new android.media.PlaybackParams().setSpeed(speed)
                );
            }
        } else {
            Log.w(TAG, "Playback speed control requires Android 6.0+");
        }
    }
    
    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }
    
    public int getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }
    
    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }
    
    public boolean isLooping() {
        return isLooping;
    }
    
    public float getPlaybackSpeed() {
        return playbackSpeed;
    }
    
    private void cleanup() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            if (surface != null) {
                surface.release();
                surface = null;
            }
            if (surfaceTexture != null) {
                surfaceTexture.release();
                surfaceTexture = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup: " + e.getMessage());
        }
    }
}
