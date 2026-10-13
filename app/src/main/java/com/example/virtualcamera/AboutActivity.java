package com.example.virtualcamera;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * About Activity - Shows app information and links
 */
public class AboutActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        
        // Set version info
        TextView versionTextView = findViewById(R.id.versionTextView);
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            String versionCode = String.valueOf(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);
            versionTextView.setText("Version: " + versionName + " (" + versionCode + ")");
        } catch (Exception e) {
            versionTextView.setText("Version: Unknown");
        }
        
        // Set up click listeners for links
        findViewById(R.id.websiteButton).setOnClickListener(v -> 
            openUrl("https://virtualcamera.com"));
        findViewById(R.id.telegramButton).setOnClickListener(v -> 
            openUrl("https://t.me/virtualcamera_support"));
        findViewById(R.id.emailButton).setOnClickListener(v -> 
            sendEmail());
        findViewById(R.id.privacyButton).setOnClickListener(v -> 
            openUrl("https://virtualcamera.com/privacy"));
        findViewById(R.id.termsButton).setOnClickListener(v -> 
            openUrl("https://virtualcamera.com/terms"));
    }
    
    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
    
    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@virtualcamera.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Virtual Camera Support");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
