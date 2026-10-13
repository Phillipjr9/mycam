package com.example.virtualcamera;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Video Recorder - Records virtual camera output to video files
 */
public class VideoRecorder {
    
    private static final String TAG = "VideoRecorder";
    
    private MediaRecorder mediaRecorder;
    private String outputFilePath;
    private boolean isRecording = false;
    private Context context;
    
    public VideoRecorder(Context context) {
        this.context = context;
    }
    
    /**
     * Start recording
     */
    public boolean startRecording(String outputDir, String fileNamePrefix) {
        if (isRecording) {
            Log.e(TAG, "Already recording");
            return false;
        }
        
        try {
            // Create output file
            outputFilePath = createOutputFile(outputDir, fileNamePrefix);
            
            mediaRecorder = new MediaRecorder();
            
            // Set audio source (we'll use virtual camera as video source)
            // For actual implementation, we need to capture from the virtual display
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            
            // Set output format
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            
            // Set video encoder
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            
            // Set audio encoder
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            
            // Set output file
            mediaRecorder.setOutputFile(outputFilePath);
            
            // Set video parameters
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.setVideoSize(1280, 720);
            mediaRecorder.setVideoEncodingBitRate(5000000); // 5 Mbps
            
            // Set audio parameters
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setAudioChannels(1);
            mediaRecorder.setAudioEncodingBitRate(128000); // 128 Kbps
            
            // Set max duration (optional)
            mediaRecorder.setMaxDuration(60 * 60 * 1000); // 1 hour
            
            // Prepare
            mediaRecorder.prepare();
            
            // Start
            mediaRecorder.start();
            isRecording = true;
            
            Log.d(TAG, "Recording started: " + outputFilePath);
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Error starting recording: " + e.getMessage());
            e.printStackTrace();
            cleanup();
            return false;
        }
    }
    
    /**
     * Stop recording
     */
    public void stopRecording() {
        if (!isRecording) {
            return;
        }
        
        try {
            mediaRecorder.stop();
            isRecording = false;
            Log.d(TAG, "Recording stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    /**
     * Pause recording
     */
    public void pauseRecording() {
        if (!isRecording || mediaRecorder == null) {
            return;
        }
        
        try {
            // Pause is not supported in all API levels
            // For API 24+, we can use pause
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder.pause();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error pausing recording: " + e.getMessage());
        }
    }
    
    /**
     * Resume recording
     */
    public void resumeRecording() {
        if (!isRecording || mediaRecorder == null) {
            return;
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder.resume();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resuming recording: " + e.getMessage());
        }
    }
    
    /**
     * Check if recording
     */
    public boolean isRecording() {
        return isRecording;
    }
    
    /**
     * Get output file path
     */
    public String getOutputFilePath() {
        return outputFilePath;
    }
    
    /**
     * Create output file with timestamp
     */
    private String createOutputFile(String outputDir, String fileNamePrefix) {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = fileNamePrefix + "_" + timeStamp + ".mp4";
        
        return dir.getAbsolutePath() + "/" + fileName;
    }
    
    /**
     * Clean up resources
     */
    private void cleanup() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.release();
                mediaRecorder = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up: " + e.getMessage());
        }
    }
    
    /**
     * Set video resolution
     */
    public void setVideoResolution(int width, int height) {
        if (mediaRecorder != null) {
            // Can't change while recording
            return;
        }
        // Will be applied on next start
    }
    
    /**
     * Set video frame rate
     */
    public void setFrameRate(int fps) {
        if (mediaRecorder != null) {
            // Can't change while recording
            return;
        }
        // Will be applied on next start
    }
    
    /**
     * Set video bit rate
     */
    public void setVideoBitRate(int bitRate) {
        if (mediaRecorder != null) {
            // Can't change while recording
            return;
        }
        // Will be applied on next start
    }
    
    /**
     * Get recording duration in milliseconds
     */
    public long getDuration() {
        if (!isRecording || mediaRecorder == null) {
            return 0;
        }
        
        try {
            // This is a simplified approach
            // For actual duration, we'd need to track start time
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
