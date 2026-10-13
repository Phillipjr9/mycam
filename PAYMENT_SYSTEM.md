# 💰 Payment System Integration Guide

This document explains how to add **real payment functionality** to your Virtual Camera app, similar to MYCAM's payment system.

## 📋 **Overview**

MYCAM uses a **subscription-based model** with activation keys. Here's how it works:

1. **User purchases** a plan (Monthly, Yearly, Lifetime)
2. **System generates** a unique activation key
3. **User enters** key in the app
4. **App validates** key with server
5. **Pro features** are unlocked

We'll implement a similar system for your Virtual Camera app.

---

## 🎯 **Payment System Architecture**

```
┌─────────────────────────────────────────────────────────────┐
│                        USER                                   │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    WEBSITE                                  │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  1. User selects plan                                   ││
│  │  2. User chooses payment method (PayPal, Crypto, Card)  ││
│  │  3. User enters payment details                         ││
│  │  4. Payment processed                                   ││
│  │  5. Activation key generated                            ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                 PAYMENT PROCESSOR                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐  │
│  │  PayPal API     │  │  Stripe API     │  │  Crypto     │  │
│  │                 │  │                 │  │  Monitoring │  │
│  └─────────────────┘  └─────────────────┘  └─────────────┘  │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                  BACKEND SERVER                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  1. Receive payment confirmation                       ││
│  │  2. Validate payment                                     ││
│  │  3. Generate activation key                              ││
│  │  4. Store key in database                               ││
│  │  5. Send key to user (email)                            ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    ANDROID APP                                │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  1. User enters activation key                           ││
│  │  2. App calls API to validate key                        ││
│  │  3. API returns validation status                        ││
│  │  4. App unlocks Pro features if valid                    ││
│  │  5. App saves key locally for future use                 ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 **Step 1: Backend Server Setup**

You need a server to:
1. Process payments
2. Generate activation keys
3. Validate keys
4. Send emails

### **Option A: Node.js + Express (Recommended)**

#### Install dependencies:
```bash
npm init -y
npm install express body-parser cors jsonwebtoken nodemailer mongoose
npm install paypal-rest-sdk stripe crypto-js
```

#### Create `server.js`:
```javascript
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const mongoose = require('mongoose');
const crypto = require('crypto');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json());

// Connect to MongoDB (or use SQLite for simplicity)
mongoose.connect('mongodb://localhost:27017/virtualcamera', {
  useNewUrlParser: true,
  useUnifiedTopology: true
});

// Activation Key Model
const ActivationKeySchema = new mongoose.Schema({
  key: { type: String, required: true, unique: true },
  plan: { type: String, required: true, enum: ['free', 'monthly', 'yearly', 'lifetime'] },
  email: { type: String },
  paymentMethod: { type: String },
  paymentId: { type: String },
  createdAt: { type: Date, default: Date.now },
  expiresAt: { type: Date },
  used: { type: Boolean, default: false },
  usedAt: { type: Date },
  deviceId: { type: String }
});

const ActivationKey = mongoose.model('ActivationKey', ActivationKeySchema);

// Generate activation key
function generateActivationKey() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let key = '';
  
  // Add plan prefix
  const plan = 'PRO';
  key = plan + '-';
  
  // Generate random characters
  for (let i = 0; i < 16; i++) {
    key += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  
  // Format as XXXX-XXXX-XXXX-XXXX
  return key.match(/.{1,4}/g).join('-');
}

// Create activation key endpoint
app.post('/api/keys/create', async (req, res) => {
  try {
    const { plan, email, paymentMethod, paymentId } = req.body;
    
    // Calculate expiration
    let expiresAt = null;
    if (plan === 'monthly') {
      expiresAt = new Date();
      expiresAt.setMonth(expiresAt.getMonth() + 1);
    } else if (plan === 'yearly') {
      expiresAt = new Date();
      expiresAt.setFullYear(expiresAt.getFullYear() + 1);
    }
    // Lifetime has no expiration
    
    // Generate key
    let key;
    let attempts = 0;
    do {
      key = generateActivationKey();
      attempts++;
      if (attempts > 10) {
        return res.status(500).json({ error: 'Failed to generate unique key' });
      }
    } while (await ActivationKey.findOne({ key }));
    
    // Create and save key
    const activationKey = new ActivationKey({
      key,
      plan,
      email,
      paymentMethod,
      paymentId,
      expiresAt
    });
    
    await activationKey.save();
    
    res.json({ 
      success: true, 
      key,
      expiresAt,
      plan
    });
  } catch (error) {
    console.error('Error creating key:', error);
    res.status(500).json({ error: 'Failed to create activation key' });
  }
});

// Validate activation key endpoint
app.post('/api/keys/validate', async (req, res) => {
  try {
    const { key, deviceId } = req.body;
    
    const activationKey = await ActivationKey.findOne({ key });
    
    if (!activationKey) {
      return res.status(404).json({ 
        error: 'Invalid activation key',
        valid: false 
      });
    }
    
    // Check if already used
    if (activationKey.used) {
      // Check if used on this device
      if (activationKey.deviceId === deviceId) {
        return res.json({ 
          valid: true,
          plan: activationKey.plan,
          expiresAt: activationKey.expiresAt
        });
      }
      return res.status(400).json({ 
        error: 'Activation key already used on another device',
        valid: false 
      });
    }
    
    // Check expiration
    if (activationKey.expiresAt && activationKey.expiresAt < new Date()) {
      return res.status(400).json({ 
        error: 'Activation key expired',
        valid: false 
      });
    }
    
    // Mark as used
    activationKey.used = true;
    activationKey.usedAt = new Date();
    activationKey.deviceId = deviceId;
    await activationKey.save();
    
    res.json({ 
      valid: true,
      plan: activationKey.plan,
      expiresAt: activationKey.expiresAt
    });
  } catch (error) {
    console.error('Error validating key:', error);
    res.status(500).json({ error: 'Failed to validate activation key' });
  }
});

// Start server
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
```

### **Option B: Firebase (Easier, No Server)**

Firebase can handle:
- Authentication
- Firestore database (for keys)
- Cloud Functions (for key generation)
- Hosting (for website)

#### Set up Firebase:
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project
3. Enable Firestore Database
4. Enable Cloud Functions
5. Enable Authentication (Email/Password)

#### Firestore Structure:
```
activationKeys (collection)
  ├── {keyId} (document)
  │     ├── key: "PRO-ABCD-1234-EFGH-5678"
  │     ├── plan: "monthly"
  │     ├── email: "user@example.com"
  │     ├── paymentMethod: "paypal"
  │     ├── paymentId: "PAYID-123456789"
  │     ├── createdAt: timestamp
  │     ├── expiresAt: timestamp
  │     ├── used: false
  │     ├── usedAt: null
  │     └── deviceId: null
```

---

## 💳 **Step 2: Payment Processor Integration**

### **Option A: PayPal Integration**

#### 1. Create PayPal Business Account
- Go to [PayPal Developer](https://developer.paypal.com/)
- Create Sandbox accounts for testing
- Get API credentials

#### 2. Install PayPal SDK:
```bash
npm install paypal-rest-sdk
```

#### 3. Create Payment Endpoint:
```javascript
const paypal = require('paypal-rest-sdk');

// Configure PayPal
paypal.configure({
  'mode': 'live', // or 'sandbox'
  'client_id': 'YOUR_CLIENT_ID',
  'client_secret': 'YOUR_CLIENT_SECRET'
});

// Create payment
app.post('/api/paypal/create', (req, res) => {
  const { plan, email, returnUrl, cancelUrl } = req.body;
  
  // Calculate amount based on plan
  let amount;
  switch(plan) {
    case 'monthly': amount = '9.99'; break;
    case 'yearly': amount = '79.99'; break;
    case 'lifetime': amount = '149.99'; break;
    default: amount = '0.00';
  }
  
  const create_payment_json = {
    "intent": "sale",
    "payer": {
      "payment_method": "paypal"
    },
    "redirect_urls": {
      "return_url": returnUrl,
      "cancel_url": cancelUrl
    },
    "transactions": [{
      "item_list": {
        "items": [{
          "name": `Virtual Camera ${plan.charAt(0).toUpperCase() + plan.slice(1)}`,
          "sku": `VC-${plan}`,
          "price": amount,
          "currency": "USD",
          "quantity": 1
        }]
      },
      "amount": {
        "currency": "USD",
        "total": amount
      },
      "description": `Virtual Camera ${plan} subscription`
    }]
  };
  
  paypal.payment.create(create_payment_json, (error, payment) => {
    if (error) {
      return res.status(500).json({ error: error.response.details[0].issue });
    }
    
    // Redirect user to PayPal for payment
    const approvalUrl = payment.links.find(link => link.rel === 'approval_url').href;
    res.json({ approvalUrl });
  });
});

// Execute payment (after user returns from PayPal)
app.get('/api/paypal/execute', (req, res) => {
  const { paymentId, PayerID } = req.query;
  
  const execute_payment_json = {
    "payer_id": PayerID
  };
  
  paypal.payment.execute(paymentId, execute_payment_json, async (error, payment) => {
    if (error) {
      return res.redirect('/payment-failed');
    }
    
    // Payment successful - create activation key
    const plan = req.query.plan;
    const email = payment.payer.payer_info.email;
    
    // Generate and save key
    const key = generateActivationKey();
    
    // Save to database
    const activationKey = new ActivationKey({
      key,
      plan,
      email,
      paymentMethod: 'paypal',
      paymentId: payment.id
    });
    
    await activationKey.save();
    
    // Send email with activation key
    await sendActivationEmail(email, key, plan);
    
    // Redirect to success page with key
    res.redirect(`/payment-success?key=${key}&plan=${plan}`);
  });
});
```

### **Option B: Stripe Integration**

#### 1. Create Stripe Account
- Go to [Stripe Dashboard](https://dashboard.stripe.com/)
- Get API keys

#### 2. Install Stripe SDK:
```bash
npm install stripe
```

#### 3. Create Payment Endpoint:
```javascript
const stripe = require('stripe')('YOUR_STRIPE_SECRET_KEY');

app.post('/api/stripe/create-payment-intent', async (req, res) => {
  const { plan, email } = req.body;
  
  // Calculate amount based on plan (in cents)
  let amount;
  switch(plan) {
    case 'monthly': amount = 999; break; // $9.99
    case 'yearly': amount = 7999; break; // $79.99
    case 'lifetime': amount = 14999; break; // $149.99
    default: amount = 0;
  }
  
  const paymentIntent = await stripe.paymentIntents.create({
    amount,
    currency: 'usd',
    metadata: { plan, email }
  });
  
  res.json({ clientSecret: paymentIntent.client_secret });
});

// Webhook for payment confirmation
app.post('/api/stripe/webhook', bodyParser.raw({ type: 'application/json' }), (req, res) => {
  const sig = req.headers['stripe-signature'];
  let event;
  
  try {
    event = stripe.webhooks.constructEvent(req.body, sig, 'YOUR_WEBHOOK_SIGNING_SECRET');
  } catch (err) {
    return res.status(400).send(`Webhook Error: ${err.message}`);
  }
  
  // Handle successful payment
  if (event.type === 'payment_intent.succeeded') {
    const paymentIntent = event.data.object;
    const { plan, email } = paymentIntent.metadata;
    
    // Generate activation key
    const key = generateActivationKey();
    
    // Save to database
    const activationKey = new ActivationKey({
      key,
      plan,
      email,
      paymentMethod: 'stripe',
      paymentId: paymentIntent.id
    });
    
    await activationKey.save();
    
    // Send email
    await sendActivationEmail(email, key, plan);
  }
  
  res.json({ received: true });
});
```

### **Option C: Cryptocurrency (Manual)**

For crypto payments, you can:

#### 1. Generate Payment Address
- Create a unique wallet address for each user
- Or use a single address with payment tracking

#### 2. Monitor for Payments
- Use blockchain APIs (Blockchain.com, Etherscan, etc.)
- Check for incoming transactions
- Match payments to users

#### 3. Example Bitcoin Monitoring:
```javascript
const axios = require('axios');

// Check for payment
async function checkBitcoinPayment(address, amountSatoshis) {
  const response = await axios.get(`https://blockchain.info/address/${address}?format=json`);
  const transactions = response.data.txns;
  
  // Find recent transactions
  const recentTx = transactions.find(tx => {
    // Check if transaction is for the expected amount
    return tx.out.find(out => out.addr === address && out.value >= amountSatoshis);
  });
  
  return recentTx ? true : false;
}

// Periodically check for payments
setInterval(async () => {
  const pendingPayments = await PendingPayment.find({ 
    status: 'pending',
    paymentMethod: 'bitcoin'
  });
  
  for (const payment of pendingPayments) {
    const isPaid = await checkBitcoinPayment(payment.address, payment.amountSatoshis);
    
    if (isPaid) {
      // Generate activation key
      const key = generateActivationKey();
      
      // Save key
      const activationKey = new ActivationKey({
        key,
        plan: payment.plan,
        email: payment.email,
        paymentMethod: 'bitcoin',
        paymentId: payment._id
      });
      
      await activationKey.save();
      
      // Mark payment as completed
      payment.status = 'completed';
      await payment.save();
      
      // Send email
      await sendActivationEmail(payment.email, key, payment.plan);
    }
  }
}, 60000); // Check every minute
```

---

## 📧 **Step 3: Email System**

### **Option A: Nodemailer (Self-Hosted)**

```javascript
const nodemailer = require('nodemailer');

// Configure transporter
const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: 'your-email@gmail.com',
    pass: 'your-app-password' // Use App Password for Gmail
  }
});

// Send activation email
async function sendActivationEmail(email, key, plan) {
  const mailOptions = {
    from: 'Virtual Camera <noreply@virtualcamera.com>',
    to: email,
    subject: 'Your Virtual Camera Activation Key',
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <div style="background: linear-gradient(135deg, #6200EE, #03DAC6); color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0;">
          <h1 style="margin: 0;">Virtual Camera</h1>
        </div>
        <div style="padding: 20px; background: #f5f5f5; border-radius: 0 0 10px 10px;">
          <p>Hello!</p>
          <p>Thank you for purchasing Virtual Camera ${plan.charAt(0).toUpperCase() + plan.slice(1)}.</p>
          <p>Your activation key is:</p>
          <div style="background: white; padding: 15px; text-align: center; border-radius: 5px; margin: 20px 0;">
            <code style="font-family: monospace; font-size: 18px; letter-spacing: 2px;">${key}</code>
          </div>
          <p>To activate:</p>
          <ol>
            <li>Open the Virtual Camera app</li>
            <li>Go to Settings</li>
            <li>Enter this activation key</li>
            <li>Enjoy Pro features!</li>
          </ol>
          <p>If you have any questions, please contact support.</p>
          <p>Best regards,<br>The Virtual Camera Team</p>
        </div>
      </div>
    `
  };
  
  try {
    await transporter.sendMail(mailOptions);
    console.log(`Activation email sent to ${email}`);
    return true;
  } catch (error) {
    console.error(`Error sending email to ${email}:`, error);
    return false;
  }
}
```

### **Option B: SendGrid (Recommended)**

```javascript
const sgMail = require('@sendgrid/mail');

sgMail.setApiKey('YOUR_SENDGRID_API_KEY');

async function sendActivationEmail(email, key, plan) {
  const msg = {
    to: email,
    from: 'Virtual Camera <noreply@virtualcamera.com>',
    subject: 'Your Virtual Camera Activation Key',
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <div style="background: linear-gradient(135deg, #6200EE, #03DAC6); color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0;">
          <h1 style="margin: 0;">Virtual Camera</h1>
        </div>
        <div style="padding: 20px; background: #f5f5f5; border-radius: 0 0 10px 10px;">
          <p>Hello!</p>
          <p>Thank you for purchasing Virtual Camera ${plan.charAt(0).toUpperCase() + plan.slice(1)}.</p>
          <p>Your activation key is:</p>
          <div style="background: white; padding: 15px; text-align: center; border-radius: 5px; margin: 20px 0;">
            <code style="font-family: monospace; font-size: 18px; letter-spacing: 2px;">${key}</code>
          </div>
          <p>To activate:</p>
          <ol>
            <li>Open the Virtual Camera app</li>
            <li>Go to Settings</li>
            <li>Enter this activation key</li>
            <li>Enjoy Pro features!</li>
          </ol>
          <p>If you have any questions, please contact support.</p>
          <p>Best regards,<br>The Virtual Camera Team</p>
        </div>
      </div>
    `,
    text: `Your Virtual Camera activation key: ${key}`
  };
  
  try {
    await sgMail.send(msg);
    console.log(`Activation email sent to ${email}`);
    return true;
  } catch (error) {
    console.error(`Error sending email to ${email}:`, error);
    return false;
  }
}
```

---

## 📱 **Step 4: Android App Integration**

Now let's add payment functionality to the Android app.

### **1. Create Activation Activity**

Create a new file: `ActivationActivity.java`

```java
package com.example.virtualcamera;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Activation Activity - Enter and validate activation keys
 */
public class ActivationActivity extends AppCompatActivity {
    
    private static final String TAG = "ActivationActivity";
    private static final String PREFS_NAME = "VirtualCameraPrefs";
    private static final String KEY_ACTIVATION = "activation_key";
    private static final String KEY_PLAN = "plan";
    private static final String KEY_EXPIRES = "expires_at";
    
    private EditText keyInput;
    private Button activateButton;
    private TextView statusText;
    
    // Server URL - change this to your server
    private static final String SERVER_URL = "https://your-server.com/api";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation);
        
        keyInput = findViewById(R.id.keyInput);
        activateButton = findViewById(R.id.activateButton);
        statusText = findViewById(R.id.statusText);
        
        // Check if already activated
        if (isActivated()) {
            showAlreadyActivated();
        }
        
        activateButton.setOnClickListener(v -> validateKey());
    }
    
    private void validateKey() {
        String key = keyInput.getText().toString().trim();
        
        if (key.isEmpty()) {
            Toast.makeText(this, "Please enter your activation key", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading
        statusText.setText("Validating key...");
        activateButton.setEnabled(false);
        
        // Validate with server
        new ValidateKeyTask().execute(key);
    }
    
    private boolean isActivated() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.contains(KEY_ACTIVATION);
    }
    
    private void showAlreadyActivated() {
        keyInput.setEnabled(false);
        activateButton.setEnabled(false);
        statusText.setText("✅ Your app is already activated!");
        
        // Show current plan
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String plan = prefs.getString(KEY_PLAN, "Unknown");
        String expires = prefs.getString(KEY_EXPIRES, "Never");
        
        TextView planText = findViewById(R.id.planText);
        if (planText != null) {
            planText.setText("Current Plan: " + plan + " (Expires: " + expires + ")");
        }
    }
    
    private class ValidateKeyTask extends AsyncTask<String, Void, Boolean> {
        private String plan;
        private String expiresAt;
        private String errorMessage;
        
        @Override
        protected Boolean doInBackground(String... keys) {
            String key = keys[0];
            
            try {
                // Get device ID
                String deviceId = android.provider.Settings.Secure.getString(
                    getContentResolver(), 
                    android.provider.Settings.Secure.ANDROID_ID
                );
                
                // Create request
                JSONObject request = new JSONObject();
                request.put("key", key);
                request.put("deviceId", deviceId);
                
                // Send to server
                URL url = new URL(SERVER_URL + "/keys/validate");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                // Write request
                OutputStream os = connection.getOutputStream();
                os.write(request.toString().getBytes());
                os.flush();
                os.close();
                
                // Get response
                int responseCode = connection.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                    );
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    
                    // Parse response
                    JSONObject json = new JSONObject(response.toString());
                    
                    if (json.getBoolean("valid")) {
                        plan = json.getString("plan");
                        expiresAt = json.optString("expiresAt", "Never");
                        return true;
                    } else {
                        errorMessage = json.optString("error", "Invalid key");
                        return false;
                    }
                } else {
                    errorMessage = "Server error: " + responseCode;
                    return false;
                }
                
            } catch (Exception e) {
                errorMessage = e.getMessage();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            activateButton.setEnabled(true);
            
            if (success) {
                // Save activation
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_ACTIVATION, keyInput.getText().toString().trim());
                editor.putString(KEY_PLAN, plan);
                editor.putString(KEY_EXPIRES, expiresAt);
                editor.apply();
                
                statusText.setText("✅ Activation successful!");
                Toast.makeText(ActivationActivity.this, "Pro features unlocked!", Toast.LENGTH_LONG).show();
                
                // Return to main activity
                Intent intent = new Intent(ActivationActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                
            } else {
                statusText.setText("❌ " + errorMessage);
                Toast.makeText(ActivationActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
}
```

### **2. Create Activation Layout**

Create: `app/src/main/res/layout/activity_activation.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp"
    android:background="@color/colorBackground"
    tools:context=".ActivationActivity">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Activate Virtual Camera"
        android:textSize="24sp"
        android:textStyle="bold"
        android:textColor="@color/colorPrimary"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginBottom="16dp"/>

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Enter your activation key to unlock Pro features"
        android:textSize="16sp"
        android:textColor="@color/colorText"
        app:layout_constraintTop_toBottomOf="@id/titleTextView"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginBottom="32dp"/>

    <EditText
        android:id="@+id/keyInput"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:hint="Activation Key"
        android:inputType="textNoSuggestions|textMultiLine"
        android:importantForAutofill="no"
        android:textSize="16sp"
        android:padding="16dp"
        android:background="@drawable/edittext_background"
        app:layout_constraintTop_toBottomOf="@id/subtitleTextView"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginBottom="16dp"/>

    <TextView
        android:id="@+id/planText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="14sp"
        android:textColor="@color/colorText"
        app:layout_constraintTop_toBottomOf="@id/keyInput"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginBottom="16dp"
        android:visibility="gone"/>

    <TextView
        android:id="@+id/statusText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="14sp"
        android:textColor="@color/colorStatusStopped"
        app:layout_constraintTop_toBottomOf="@id/planText"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginBottom="16dp"/>

    <Button
        android:id="@+id/activateButton"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="Activate"
        android:backgroundTint="@color/colorPrimary"
        android:textColor="@color/colorBackground"
        app:layout_constraintTop_toBottomOf="@id/statusText"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Need a key? Purchase one from our website"
        android:textSize="14sp"
        android:textColor="@color/colorText"
        app:layout_constraintTop_toBottomOf="@id/activateButton"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="16dp"/>

</androidx.constraintlayout.widget.ConstraintLayout>
```

### **3. Update MainActivity to Check Activation**

Add to `MainActivity.java`:

```java
// Add at the top with other constants
private static final String PREFS_NAME = "VirtualCameraPrefs";
private static final String KEY_ACTIVATION = "activation_key";
private static final String KEY_PLAN = "plan";

// Add method to check activation
private boolean isProActivated() {
    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    return prefs.contains(KEY_ACTIVATION);
}

// Add method to get current plan
private String getCurrentPlan() {
    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    return prefs.getString(KEY_PLAN, "free");
}

// Update onCreate to check activation
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    // ... existing code ...
    
    // Check activation
    if (!isProActivated()) {
        // Show activation prompt
        showActivationPrompt();
    }
}

private void showActivationPrompt() {
    // In a real app, you might want to show this after a few uses
    // For now, we'll just log it
    Log.d(TAG, "App not activated - Pro features limited");
}

// Add menu option to activate
// In your options menu or settings
private void openActivation() {
    Intent intent = new Intent(this, ActivationActivity.class);
    startActivity(intent);
}
```

### **4. Add Purchase Activity (Optional)**

For in-app purchases, you can use Google Play Billing:

#### Add to `build.gradle`:
```gradle
implementation 'com.android.billingclient:billing:5.0.0'
```

#### Create `PurchaseActivity.java`:
```java
package com.example.virtualcamera;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Purchase Activity - In-app purchases via Google Play
 */
public class PurchaseActivity extends AppCompatActivity {
    
    private static final String TAG = "PurchaseActivity";
    
    private BillingClient billingClient;
    private Button monthlyButton, yearlyButton, lifetimeButton;
    
    // Product IDs - must match what's in Google Play Console
    private static final String PRODUCT_MONTHLY = "virtual_camera_monthly";
    private static final String PRODUCT_YEARLY = "virtual_camera_yearly";
    private static final String PRODUCT_LIFETIME = "virtual_camera_lifetime";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);
        
        monthlyButton = findViewById(R.id.monthlyButton);
        yearlyButton = findViewById(R.id.yearlyButton);
        lifetimeButton = findViewById(R.id.lifetimeButton);
        
        // Initialize billing client
        setupBillingClient();
        
        monthlyButton.setOnClickListener(v -> purchase(PRODUCT_MONTHLY));
        yearlyButton.setOnClickListener(v -> purchase(PRODUCT_YEARLY));
        lifetimeButton.setOnClickListener(v -> purchase(PRODUCT_LIFETIME));
    }
    
    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(new PurchasesUpdatedListener() {
                @Override
                public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK 
                            && purchases != null) {
                        for (Purchase purchase : purchases) {
                            handlePurchase(purchase);
                        }
                    } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                        Log.i(TAG, "Purchase cancelled");
                    } else {
                        Log.w(TAG, "Purchase error: " + billingResult.getDebugMessage());
                    }
                }
            })
            .enablePendingPurchases()
            .build();
        
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    queryProductDetails();
                }
            }
            
            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection
                setupBillingClient();
            }
        });
    }
    
    private void queryProductDetails() {
        List<String> productList = new ArrayList<>();
        productList.add(PRODUCT_MONTHLY);
        productList.add(PRODUCT_YEARLY);
        productList.add(PRODUCT_LIFETIME);
        
        QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams
            .newBuilder()
            .setProductList(productList)
            .setType(BillingClient.ProductType.SUBSCRIPTION)
            .build();
        
        billingClient.queryProductDetailsAsync(
            queryProductDetailsParams,
            new ProductDetailsResponseListener() {
                @Override
                public void onProductDetailsResponse(BillingResult billingResult, 
                        List<ProductDetails> productDetailsList) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // Process product details
                        for (ProductDetails productDetails : productDetailsList) {
                            String productId = productDetails.getProductId();
                            String price = productDetails.getOneTimePurchaseOfferDetails() != null ?
                                productDetails.getOneTimePurchaseOfferDetails().getFormattedPrice() :
                                productDetails.getSubscriptionOfferDetails().get(0).getFormattedPrice();
                            
                            Log.d(TAG, "Product: " + productId + " - " + price);
                        }
                    }
                }
            }
        );
    }
    
    private void purchase(String productId) {
        if (billingClient == null || !billingClient.isReady()) {
            Toast.makeText(this, "Billing client not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<String> skuList = new ArrayList<>();
        skuList.add(productId);
        
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                QueryProductDetailsParams.newBuilder()
                    .setProductList(skuList)
                    .setType(BillingClient.ProductType.SUBSCRIPTION)
                    .build()
            )
            .build();
        
        billingClient.launchBillingFlow(this, billingFlowParams);
    }
    
    private void handlePurchase(Purchase purchase) {
        // Verify the purchase
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge the purchase
            AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams
                .newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
            
            billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                @Override
                public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // Purchase acknowledged
                        // Generate activation key (or use purchase token as key)
                        String activationKey = purchase.getPurchaseToken();
                        
                        // Save to preferences
                        SharedPreferences prefs = getSharedPreferences("VirtualCameraPrefs", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("activation_key", activationKey);
                        editor.putString("plan", getPlanFromProduct(purchase.getProducts().get(0)));
                        editor.apply();
                        
                        Toast.makeText(PurchaseActivity.this, "Purchase successful! Pro features unlocked.", Toast.LENGTH_LONG).show();
                        
                        // Return to main activity
                        finish();
                    }
                }
            });
        }
    }
    
    private String getPlanFromProduct(String productId) {
        if (productId.equals(PRODUCT_MONTHLY)) return "monthly";
        if (productId.equals(PRODUCT_YEARLY)) return "yearly";
        if (productId.equals(PRODUCT_LIFETIME)) return "lifetime";
        return "free";
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}
```

---

## 🔐 **Step 5: Security Considerations**

### **1. Protect Your API**
- Use API keys for server endpoints
- Rate limit requests
- Validate all input
- Use HTTPS (not HTTP)

### **2. Protect Activation Keys**
- Generate unique keys for each user
- Store keys securely in database
- Validate device IDs to prevent sharing
- Set expiration for non-lifetime plans

### **3. Protect Payments**
- Use official payment processor SDKs
- Never store credit card numbers
- Use tokenization
- Verify payments before activating

### **4. Protect the App**
- Obfuscate code with ProGuard
- Check for tampering
- Verify app signature
- Use certificate pinning

---

## 📊 **Step 6: Analytics & Monitoring**

### **1. Track Activations**
- Log when keys are generated
- Log when keys are validated
- Track which plans are popular
- Monitor for abuse

### **2. Track Usage**
- Track app opens
- Track feature usage
- Track errors
- Track performance

### **3. Monitoring Dashboard**
Create a simple admin dashboard to:
- View all activation keys
- View payment history
- View active users
- View revenue
- Revoke keys if needed

---

## 🎯 **COMPLETE SYSTEM OVERVIEW**

### **User Flow:**

```
1. User visits website
   │
   ▼
2. User selects plan (Monthly, Yearly, Lifetime)
   │
   ▼
3. User chooses payment method (PayPal, Crypto, Credit Card)
   │
   ▼
4. User completes payment
   │
   ▼
5. Server generates activation key
   │
   ▼
6. Server sends key via email
   │
   ▼
7. User receives email with key
   │
   ▼
8. User opens app
   │
   ▼
9. User enters activation key
   │
   ▼
10. App validates key with server
    │
    ▼
11. App unlocks Pro features
```

### **Technical Flow:**

```
Website (HTML/JS)
    │
    ▼
Payment Processor API (PayPal/Stripe)
    │
    ▼
Backend Server (Node.js)
    │
    ├─ Generate activation key
    │
    ├─ Store key in database
    │
    ├─ Send email with key
    │
    └─ Return success to website
    │
    ▼
Android App
    │
    ├─ User enters key
    │
    ├─ App calls /api/keys/validate
    │
    ├─ Server validates key
    │
    ├─ Server marks key as used
    │
    └─ Server returns validation result
    │
    ▼
App unlocks Pro features
```

---

## 💡 **RECOMMENDED APPROACH**

For a **production-ready system**, I recommend:

### **Phase 1: MVP (Minimum Viable Product)**
1. ✅ Use **Firebase** for backend (no server needed)
2. ✅ Use **PayPal** for payments (easiest)
3. ✅ Use **SendGrid** for emails (free tier available)
4. ✅ Manual key generation (start simple)

### **Phase 2: Enhanced**
1. ⬜ Add **Stripe** for credit cards
2. ⬜ Add **Crypto** payments
3. ⬜ Add **Google Play Billing** for in-app purchases
4. ⬜ Add **admin dashboard** for monitoring

### **Phase 3: Enterprise**
1. ⬜ Add **subscription management**
2. ⬜ Add **trial periods**
3. ⬜ Add **coupon codes**
4. ⬜ Add **affiliate system**

---

## 📈 **PRICING STRATEGY**

Based on MYCAM's pricing and market analysis:

| Plan | Price | Target Users | Conversion Rate |
|------|-------|---------------|-----------------|
| Free Trial | $0 / 7 Days | Everyone | 10-20% |
| Pro Monthly | $9.99 / Month | Casual users | 5-10% |
| Pro Yearly | $79.99 / Year | Serious users | 15-25% |
| Lifetime | $149.99 / One Time | Power users | 3-8% |

**Recommended:** Start with Free Trial + Pro Monthly/Yearly

---

## 🚀 **NEXT STEPS**

### **Immediate (This Week):**
1. [ ] Set up Firebase project
2. [ ] Create Firestore database
3. [ ] Implement key generation in Cloud Functions
4. [ ] Set up SendGrid for emails
5. [ ] Deploy website
6. [ ] Test payment flow

### **Short-term (This Month):**
1. [ ] Add PayPal integration
2. [ ] Add Stripe integration
3. [ ] Add Google Play Billing
4. [ ] Create admin dashboard
5. [ ] Add analytics

### **Long-term (Next Quarter):**
1. [ ] Add subscription management
2. [ ] Add trial periods
3. [ ] Add coupon codes
4. [ ] Add affiliate system
5. [ ] Add multi-device support

---

## 📚 **RESOURCES**

### **Documentation:**
- [PayPal Developer](https://developer.paypal.com/)
- [Stripe Documentation](https://stripe.com/docs)
- [Google Play Billing](https://developer.android.com/google/play/billing)
- [Firebase](https://firebase.google.com/docs)
- [SendGrid](https://sendgrid.com/docs/)

### **Tutorials:**
- [PayPal Integration Guide](https://developer.paypal.com/docs/checkout/)
- [Stripe Payment Processing](https://stripe.com/docs/payments)
- [Firebase Cloud Functions](https://firebase.google.com/docs/functions)
- [Android In-App Purchases](https://developer.android.com/google/play/billing/integrate)

### **Code Examples:**
- [PayPal Node.js SDK](https://github.com/paypal/PayPal-node-SDK)
- [Stripe Node.js SDK](https://github.com/stripe/stripe-node)
- [Firebase Samples](https://github.com/firebase/snippets-android)

---

## 🎉 **CONCLUSION**

You now have a **complete blueprint** for implementing a payment system for your Virtual Camera app, similar to MYCAM's system.

**Key Takeaways:**
1. Use **activation keys** for license management
2. Support **multiple payment methods** (PayPal, Stripe, Crypto)
3. Use **Firebase or a backend server** for key generation/validation
4. Send **activation keys via email**
5. Validate keys **in the app** with the server
6. Store keys **securely** in a database

**Ready to implement?** Start with Phase 1 (Firebase + PayPal) and you'll have a working payment system in a few days!

---

**Need help with any specific part?** Let me know which component you want me to implement first, and I'll create the code for you!
