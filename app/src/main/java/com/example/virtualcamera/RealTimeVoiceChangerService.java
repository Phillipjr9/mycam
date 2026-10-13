package com.example.virtualcamera;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Real-Time Voice Changer Service
 * Captures microphone audio, applies voice modulation, and plays it back in real-time
 * This allows users to have live voice calls with modified voice
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class RealTimeVoiceChangerService extends Service {
    
    private static final String TAG = "RealTimeVoiceService";
    private static final String CHANNEL_ID = "RealTimeVoiceChannel";
    private static final int NOTIFICATION_ID = 3;
    
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private boolean isRunning = false;
    private boolean isMaleToFemale = true;
    private float pitchFactor = 1.5f; // Male to Female
    
    private final IBinder binder = new LocalBinder();
    
    public class LocalBinder extends Binder {
        RealTimeVoiceChangerService getService() {
            return RealTimeVoiceChangerService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RealTimeVoiceChangerService created");
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Real-Time Voice Changer",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Real-Time Voice Changer")
            .setContentText("Voice modulation active")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            boolean start = intent.getBooleanExtra("start", false);
            boolean maleToFemale = intent.getBooleanExtra("maleToFemale", true);
            
            if (start) {
                startVoiceChanger(maleToFemale);
            } else {
                stopVoiceChanger();
            }
        }
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    /**
     * Start real-time voice changer
     */
    public void startVoiceChanger(boolean maleToFemale) {
        if (isRunning) {
            Log.d(TAG, "Voice changer already running");
            return;
        }
        
        isMaleToFemale = maleToFemale;
        pitchFactor = maleToFemale ? 1.5f : 0.7f;
        
        try {
            int sampleRate = 44100;
            int channelConfig = AudioFormat.CHANNEL_IN_MONO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            
            int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            
            // Create audio recorder
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize * 2
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize AudioRecord");
                return;
            }
            
            // Create audio track for playback
            int streamType = AudioTrack.STREAM_VOICE_CALL;
            audioTrack = new AudioTrack(
                streamType,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize * 2,
                AudioTrack.MODE_STREAM
            );
            
            if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize AudioTrack");
                audioRecord.release();
                audioRecord = null;
                return;
            }
            
            // Start recording and playback
            audioRecord.startRecording();
            audioTrack.play();
            
            isRunning = true;
            
            // Start processing thread
            new Thread(() -> {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                processAudioRealTime();
            }).start();
            
            Log.d(TAG, "Real-time voice changer started");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting voice changer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Process audio in real-time with pitch shifting
     */
    private void processAudioRealTime() {
        try {
            int sampleRate = 44100;
            int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            short[] buffer = new short[bufferSize];
            short[] processedBuffer = new short[bufferSize];
            
            while (isRunning && audioRecord != null && audioTrack != null) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                
                if (read > 0) {
                    // Apply pitch shifting (simplified for real-time)
                    // For better quality, use a proper pitch shifting algorithm
                    for (int i = 0; i < read; i++) {
                        // Simple pitch shift by resampling
                        // This is a placeholder - real implementation would use proper algorithm
                        if (isMaleToFemale) {
                            // Increase pitch (higher frequency)
                            processedBuffer[i] = (short) Math.min(Short.MAX_VALUE, Math.max(Short.MIN_VALUE, buffer[i] * 1.2));
                        } else {
                            // Decrease pitch (lower frequency)
                            processedBuffer[i] = (short) Math.min(Short.MAX_VALUE, Math.max(Short.MIN_VALUE, buffer[i] * 0.8));
                        }
                    }
                    
                    // Write processed audio to track
                    audioTrack.write(processedBuffer, 0, read);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in audio processing: " + e.getMessage());
        } finally {
            // Clean up if stopped
            if (audioRecord != null) {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
            }
            if (audioTrack != null) {
                if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.stop();
                }
                audioTrack.release();
            }
        }
    }
    
    /**
     * Stop real-time voice changer
     */
    public void stopVoiceChanger() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        
        try {
            if (audioRecord != null) {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
                audioRecord = null;
            }
            
            if (audioTrack != null) {
                if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.stop();
                }
                audioTrack.release();
                audioTrack = null;
            }
            
            Log.d(TAG, "Real-time voice changer stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping voice changer: " + e.getMessage());
        }
    }
    
    /**
     * Check if voice changer is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Set gender swap mode
     */
    public void setMaleToFemale(boolean maleToFemale) {
        isMaleToFemale = maleToFemale;
        pitchFactor = maleToFemale ? 1.5f : 0.7f;
    }
    
    /**
     * Get current gender mode
     */
    public boolean isMaleToFemale() {
        return isMaleToFemale;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopVoiceChanger();
        Log.d(TAG, "RealTimeVoiceChangerService destroyed");
    }
}
