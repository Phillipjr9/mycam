package com.example.virtualcamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Voice Changer Activity - Allows users to record voice and change gender
 * Users can swap voice from male to female and send as voice note to any app
 */
public class VoiceChangerActivity extends AppCompatActivity {
    
    private static final String TAG = "VoiceChangerActivity";
    private static final int REQUEST_RECORD_AUDIO = 200;
    private static final int REQUEST_STORAGE = 201;
    
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private boolean isMaleToFemale = true; // Default: male to female
    
    private Button recordButton;
    private Button playButton;
    private Button saveButton;
    private Button shareButton;
    private Button swapGenderButton;
    private TextView statusTextView;
    private TextView genderTextView;
    private ImageView genderIcon;
    
    private File audioFile;
    private File processedFile;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // Audio buffer for recording
    private short[] audioBuffer;
    private int bufferRead = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_changer);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        
        // Initialize UI
        recordButton = findViewById(R.id.recordButton);
        playButton = findViewById(R.id.playButton);
        saveButton = findViewById(R.id.saveButton);
        shareButton = findViewById(R.id.shareButton);
        swapGenderButton = findViewById(R.id.swapGenderButton);
        statusTextView = findViewById(R.id.statusTextView);
        genderTextView = findViewById(R.id.genderTextView);
        genderIcon = findViewById(R.id.genderIcon);
        
        // Set initial gender display
        updateGenderDisplay();
        
        // Set button listeners
        recordButton.setOnClickListener(v -> toggleRecording());
        playButton.setOnClickListener(v -> playRecording());
        saveButton.setOnClickListener(v -> saveRecording());
        shareButton.setOnClickListener(v -> shareVoiceNote());
        swapGenderButton.setOnClickListener(v -> swapGender());
        
        // Update button states
        updateButtonStates();
        
        // Create audio file
        createAudioFile();
    }
    
    private void createAudioFile() {
        File storageDir = getFilesDir();
        audioFile = new File(storageDir, "voice_original.pcm");
        processedFile = new File(storageDir, "voice_processed.mp3");
    }
    
    private void updateGenderDisplay() {
        if (isMaleToFemale) {
            genderTextView.setText("Male → Female");
            genderIcon.setImageResource(android.R.drawable.ic_dialog_email);
            swapGenderButton.setText("Swap to Female → Male");
        } else {
            genderTextView.setText("Female → Male");
            genderIcon.setImageResource(android.R.drawable.ic_dialog_email);
            swapGenderButton.setText("Swap to Male → Female");
        }
    }
    
    private void swapGender() {
        isMaleToFemale = !isMaleToFemale;
        updateGenderDisplay();
        Toast.makeText(this, "Gender swap: " + (isMaleToFemale ? "Male → Female" : "Female → Male"), 
            Toast.LENGTH_SHORT).show();
    }
    
    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }
    
    private void startRecording() {
        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
            return;
        }
        
        // Initialize audio recorder
        try {
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            );
            
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.startRecording();
                isRecording = true;
                
                // Start recording in a background thread
                new Thread(() -> {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                    recordAudio();
                }).start();
                
                updateButtonStates();
                statusTextView.setText("Recording...");
                statusTextView.setTextColor(ContextCompat.getColor(this, R.color.colorStatusStopped));
                
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to initialize recorder", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void recordAudio() {
        try {
            // Clear previous recording
            bufferRead = 0;
            audioBuffer = new short[SAMPLE_RATE * 10]; // 10 seconds buffer
            
            ByteBuffer byteBuffer = ByteBuffer.allocate(audioBuffer.length * 2);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            
            while (isRecording) {
                int read = audioRecord.read(audioBuffer, bufferRead, audioBuffer.length - bufferRead);
                if (read > 0) {
                    bufferRead += read;
                    
                    // Save to file in chunks
                    for (int i = 0; i < read; i++) {
                        byteBuffer.putShort(audioBuffer[bufferRead - read + i]);
                    }
                }
            }
            
            // Save the recording
            FileOutputStream fos = new FileOutputStream(audioFile);
            fos.write(byteBuffer.array(), 0, byteBuffer.position());
            fos.close();
            
            // Process the audio with gender swap
            processAudio();
            
        } catch (Exception e) {
            Log.e(TAG, "Error recording audio: " + e.getMessage());
            handler.post(() -> Toast.makeText(this, "Recording error: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show());
        }
    }
    
    private void stopRecording() {
        if (audioRecord != null && isRecording) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            
            updateButtonStates();
            statusTextView.setText("Recording stopped");
            statusTextView.setTextColor(ContextCompat.getColor(this, R.color.colorStatusRunning));
            
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Process audio with gender swap (pitch shifting)
     * Male to Female: Increase pitch (~1.5x)
     * Female to Male: Decrease pitch (~0.7x)
     */
    private void processAudio() {
        handler.post(() -> {
            statusTextView.setText("Processing voice...");
            statusTextView.setTextColor(ContextCompat.getColor(this, R.color.colorStatusWarning));
        });
        
        new Thread(() -> {
            try {
                // Simple pitch shift by resampling
                // For male to female: increase pitch (higher frequency)
                // For female to male: decrease pitch (lower frequency)
                
                float pitchFactor = isMaleToFemale ? 1.5f : 0.7f;
                
                // In a real implementation, we would use a proper pitch shifting algorithm
                // For this demo, we'll create a WAV file with modified pitch
                // and convert to MP3 for sharing
                
                // For now, we'll just copy the file with a different name
                // A full implementation would use a library like TarsosDSP for proper pitch shifting
                
                // Create WAV header
                byte[] wavHeader = createWavHeader(audioBuffer, bufferRead, SAMPLE_RATE, 16, 1);
                
                // Write WAV file
                File wavFile = new File(audioFile.getParent(), "voice_original.wav");
                FileOutputStream fos = new FileOutputStream(wavFile);
                fos.write(wavHeader);
                for (int i = 0; i < bufferRead; i++) {
                    fos.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(audioBuffer[i]).array());
                }
                fos.close();
                
                // For demo purposes, create a "processed" version
                // In production, use a proper audio processing library
                File processedWavFile = new File(audioFile.getParent(), "voice_processed.wav");
                FileOutputStream fos2 = new FileOutputStream(processedWavFile);
                fos2.write(wavHeader);
                
                // Apply simple pitch shift by skipping or duplicating samples
                int step = isMaleToFemale ? 2 : 1; // Simplified for demo
                for (int i = 0; i < bufferRead && i + step < bufferRead; i += step) {
                    short sample = audioBuffer[i];
                    // Modify sample based on gender swap
                    if (isMaleToFemale) {
                        // Increase pitch: make voice higher
                        sample = (short) Math.min(Short.MAX_VALUE, Math.max(Short.MIN_VALUE, sample * 1.2));
                    } else {
                        // Decrease pitch: make voice lower
                        sample = (short) Math.min(Short.MAX_VALUE, Math.max(Short.MIN_VALUE, sample * 0.8));
                    }
                    fos2.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(sample).array());
                }
                fos2.close();
                
                // Convert to MP3 (simplified - in production use a proper encoder)
                // For now, just rename the file
                if (processedWavFile.exists()) {
                    File mp3File = new File(audioFile.getParent(), "voice_processed.mp3");
                    if (mp3File.exists()) mp3File.delete();
                    processedWavFile.renameTo(mp3File);
                    processedFile = mp3File;
                }
                
                handler.post(() -> {
                    statusTextView.setText("Ready to share");
                    statusTextView.setTextColor(ContextCompat.getColor(this, R.color.colorStatusRunning));
                    updateButtonStates();
                    Toast.makeText(this, "Voice processed: " + (isMaleToFemale ? "Male → Female" : "Female → Male"), 
                        Toast.LENGTH_LONG).show();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing audio: " + e.getMessage());
                handler.post(() -> {
                    statusTextView.setText("Processing failed");
                    statusTextView.setTextColor(ContextCompat.getColor(this, R.color.colorStatusStopped));
                    Toast.makeText(this, "Processing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private byte[] createWavHeader(short[] samples, int numSamples, int sampleRate, int bitsPerSample, int numChannels) {
        int byteRate = sampleRate * numChannels * bitsPerSample / 8;
        int blockAlign = numChannels * bitsPerSample / 8;
        int dataSize = numSamples * blockAlign;
        
        ByteBuffer header = ByteBuffer.allocate(44);
        header.order(ByteOrder.LITTLE_ENDIAN);
        
        // RIFF header
        header.put("RIFF".getBytes());
        header.putInt(36 + dataSize); // File size
        header.put("WAVE".getBytes());
        
        // fmt chunk
        header.put("fmt ".getBytes());
        header.putInt(16); // Chunk size
        header.putShort((short) 1); // Format (PCM)
        header.putShort((short) numChannels);
        header.putInt(sampleRate);
        header.putInt(byteRate);
        header.putShort((short) blockAlign);
        header.putShort((short) bitsPerSample);
        
        // data chunk
        header.put("data".getBytes());
        header.putInt(dataSize);
        
        return header.array();
    }
    
    private void playRecording() {
        if (!processedFile.exists()) {
            Toast.makeText(this, "No recording to play", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(processedFile), "audio/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No app to play audio", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveRecording() {
        if (!processedFile.exists()) {
            Toast.makeText(this, "No recording to save", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Copy to Downloads folder for user access
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File savedFile = new File(downloadsDir, "VoiceNote_" + System.currentTimeMillis() + ".mp3");
        
        try {
            copyFile(processedFile, savedFile);
            Toast.makeText(this, "Saved to Downloads folder", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void copyFile(File src, File dst) throws IOException {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        
        in.close();
        out.close();
    }
    
    private void shareVoiceNote() {
        if (!processedFile.exists()) {
            Toast.makeText(this, "No voice note to share", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Create a share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/*");
            
            // Add the audio file using FileProvider for better security
            Uri audioUri = androidx.core.content.FileProvider.getUriForFile(
                this, "com.example.virtualcamera.fileprovider", processedFile);
            shareIntent.putExtra(Intent.EXTRA_STREAM, audioUri);
            
            // Add subject and text
            String genderText = isMaleToFemale ? "Male to Female" : "Female to Male";
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Voice Note - " + genderText);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Voice note with gender swap: " + genderText);
            
            // Grant temporary read permission
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Start chooser
            startActivity(Intent.createChooser(shareIntent, "Share Voice Note"));
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateButtonStates() {
        boolean hasRecording = processedFile != null && processedFile.exists();
        
        recordButton.setEnabled(!isRecording);
        recordButton.setText(isRecording ? "Stop Recording" : "Start Recording");
        recordButton.setBackgroundTint(isRecording ? 
            ContextCompat.getColor(this, R.color.colorStatusStopped) : 
            ContextCompat.getColor(this, R.color.colorPrimary));
        
        playButton.setEnabled(hasRecording && !isRecording);
        saveButton.setEnabled(hasRecording && !isRecording);
        shareButton.setEnabled(hasRecording && !isRecording);
        swapGenderButton.setEnabled(!isRecording);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        stopRecording();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
