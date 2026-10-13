# Virtual Camera - Complete Activation Key System

## 🎯 System Overview

This document describes the **complete activation key system** for the Virtual Camera app, which ensures:

1. ✅ **Each APK is tied to ONE device only** (device locking)
2. ✅ **Manual payment processing** via Telegram/Email
3. ✅ **Cryptographically signed activation keys**
4. ✅ **Feature locking** based on license type
5. ✅ **Expiration handling** for time-limited licenses

---

## 📱 Android App Implementation

### Files Modified/Created

#### 1. **LicenseManager.java** (NEW)
**Purpose:** Core license management and validation

**Key Features:**
- Generates unique Device ID from Android ID (SHA-256 hashed)
- Generates Installation Signature to prevent APK copying
- Validates activation keys (format, signature, device match, expiration)
- Manages license state (activated, expired, license type)
- Provides feature flag checking

**Important Methods:**
```java
- getDeviceId() - Returns unique device identifier
- isOnOriginalDevice() - Checks if APK is on original device
- validateKey(String key) - Validates an activation key
- saveActivationKey(String key) - Saves and activates a key
- hasFeature(int featureFlag) - Checks if a feature is available
- isActivated() - Checks if license is activated
- isExpired() - Checks if license has expired
```

#### 2. **KeyGenerator.java** (NEW)
**Purpose:** Utility for generating activation keys (mirrors LicenseManager logic)

**Usage:** Can be used by admin tools or scripts to generate keys

**Key Method:**
```java
String key = KeyGenerator.generateKey("pro_monthly", "user_device_id");
```

#### 3. **MainActivity.java** (MODIFIED)
**Changes:**
- Added license status display
- Added "Activate Pro" button
- Added "Get Device ID" button
- Added activation dialog for entering keys
- Added device ID display dialog

**New UI Elements:**
- `licenseStatusTextView` - Shows current license status
- `activateButton` - Opens activation dialog
- `deviceIdButton` - Shows user's device ID

#### 4. **MediaProjectionService.java** (MODIFIED)
**Changes:**
- Added LicenseManager integration
- Added `canUseSource()` method to enforce feature locking
- Added source type validation based on license

**Feature Locking:**
- Free Trial: Basic features only (5 min video max)
- Pro Monthly: Most features
- Pro Yearly: All features except commercial
- Lifetime: ALL features including commercial

#### 5. **SettingsActivity.java** (NO CHANGES NEEDED)
- Already handles camera settings
- No license-specific changes required

---

## 🌐 Website Implementation

### Files Modified/Created

#### 1. **index.html** (MODIFIED)
**Changes:**
- Updated payment modal with clear instructions
- Added explanation that each key is tied to ONE device
- Updated FAQ with activation key questions

**Payment Modal Flow:**
1. User selects a plan
2. Modal opens with contact info (Telegram/Email)
3. User contacts admin
4. Admin verifies payment and sends activation key

#### 2. **admin-keygen.html** (NEW)
**Purpose:** Admin tool for generating and managing activation keys

**Features:**
- Generate activation keys for users
- Validate existing keys
- User record keeping (saved in localStorage)
- Copy keys to clipboard
- View all issued keys

**How to Use:**
1. Open in browser
2. Enter user details (email, device ID, license type)
3. Click "Generate Key"
4. Copy and send key to user

#### 3. **colors.xml** (MODIFIED)
**Changes:**
- Added `colorStatusWarning` for license status display

---

## 🔐 Security Features

### 1. Device Locking
```
Device ID = SHA-256(Android_ID)
Installation Signature = SHA-256(Android_ID + Package_Name + First_Install_Time)
```
- Each key is signed with a specific Device ID
- The app checks both Device ID and Installation Signature
- Copying APK to another device **will fail**

### 2. Cryptographic Signing
```
Key Format: VC-{LICENSE_TYPE}-{DEVICE_ID}-{EXPIRATION}-{SIGNATURE}

Signature = SHA-256(LICENSE_TYPE + "|" + DEVICE_ID + "|" + EXPIRATION + "|" + SECRET_KEY)
```
- Without the secret key, generating valid keys is computationally infeasible
- **CHANGE THE SECRET KEY** before production!

### 3. Key Expiration
| License Type | Duration | Expiration Check |
|-------------|----------|------------------|
| Free Trial | 7 days | Automatic |
| Pro Monthly | 1 month | Automatic |
| Pro Yearly | 1 year | Automatic |
| Lifetime | 99 years | Never expires |

---

## 📋 Workflow

### User Journey

```
1. User installs app
   ↓
2. App generates Device ID
   ↓
3. App starts in Free Trial mode (7 days)
   ↓
4. User uses basic features
   ↓
5. User decides to upgrade
   ↓
6. User visits website, selects plan
   ↓
7. User clicks "Get Pro Monthly/Yearly/Lifetime"
   ↓
8. Payment modal opens with Telegram/Email contact
   ↓
9. User contacts admin with:
   - Payment proof
   - Device ID (from app)
   - Selected plan
   ↓
10. Admin verifies payment
    ↓
11. Admin generates activation key using admin-keygen.html
    ↓
12. Admin sends key to user
    ↓
13. User enters key in app
    ↓
14. App validates key and unlocks features
```

### Admin Journey

```
1. Receive payment proof from user
   ↓
2. Ask user for Device ID
   ↓
3. Open admin-keygen.html
   ↓
4. Enter user details:
   - Email
   - Device ID
   - License Type
   - Payment Proof
   ↓
5. Click "Generate Key"
   ↓
6. Copy generated key
   ↓
7. Send key to user
   ↓
8. Record is automatically saved
```

---

## 🎛️ Feature Access Control

### Feature Flags (in LicenseManager.java)

```java
public static final int FEATURE_1080P = 1;
public static final int FEATURE_60FPS = 2;
public static final int FEATURE_UNLIMITED_VIDEO = 4;
public static final int FEATURE_ADVANCED_FILTERS = 8;
public static final int FEATURE_CUSTOM_OVERLAYS = 16;
public static final int FEATURE_RTMP = 32;
public static final int FEATURE_MULTIPLE_CAMERAS = 64;
public static final int FEATURE_CLONE_MODE = 128;
public static final int FEATURE_SPEED_CONTROL = 256;
public static final int FEATURE_COMMERCIAL = 512;
```

### Feature Availability

| Feature | Free Trial | Pro Monthly | Pro Yearly | Lifetime |
|---------|-----------|-------------|------------|----------|
| Video File Playback | ✓ (5 min) | ✓ | ✓ | ✓ |
| Static Image | ✓ | ✓ | ✓ | ✓ |
| Physical Camera | ✓ | ✓ | ✓ | ✓ |
| Basic Text Overlays | ✓ | ✓ | ✓ | ✓ |
| Basic Color Filters | ✓ | ✓ | ✓ | ✓ |
| 720p Resolution | ✓ | ✓ | ✓ | ✓ |
| 30 FPS | ✓ | ✓ | ✓ | ✓ |
| Advanced Color Filters | ✗ | ✓ | ✓ | ✓ |
| 1080p Full HD | ✗ | ✓ | ✓ | ✓ |
| 60 FPS | ✗ | ✓ | ✓ | ✓ |
| Unlimited Video Length | ✗ | ✓ | ✓ | ✓ |
| Custom Overlays | ✗ | ✓ | ✓ | ✓ |
| RTMP Live Streaming | ✗ | ✓ | ✓ | ✓ |
| Multiple Virtual Cameras | ✗ | ✗ | ✓ | ✓ |
| Clone Mode | ✗ | ✗ | ✓ | ✓ |
| Watch Mode | ✗ | ✗ | ✓ | ✓ |
| Freeze/Unfreeze | ✗ | ✗ | ✓ | ✓ |
| Speed Control | ✗ | ✗ | ✓ | ✓ |
| Commercial License | ✗ | ✗ | ✗ | ✓ |
| Priority Support | ✗ | ✓ | ✓ | ✓ |

---

## 🔧 Configuration

### Changing the Secret Key

**BEFORE DEPLOYING TO PRODUCTION**, change the secret key in:

1. **LicenseManager.java** (line ~280):
   ```java
   private static final String SECRET_KEY = "SECRET_KEY_2026_VC";
   ```

2. **KeyGenerator.java** (line ~20):
   ```java
   private static final String SECRET_KEY = "SECRET_KEY_2026_VC";
   ```

3. **admin-keygen.html** (in JavaScript):
   ```javascript
   const data = `${licenseType}|${deviceId}|${expirationDate}|SECRET_KEY_2026_VC`;
   ```

**Important:** All three must use the **same secret key**!

### Recommended Secret Key
Use a long, random string with special characters:
```
SECRET_KEY_VC_2026_!@#$%^&*()_RANDOM_STRING_HERE
```

---

## 📊 Key Format Examples

### Free Trial (7 days)
```
VC-free_trial-a1b2c3d4e5f6...-1735689600000-abc123def456...
```

### Pro Monthly ($9.99)
```
VC-pro_monthly-a1b2c3d4e5f6...-1735689600000-abc123def456...
```

### Pro Yearly ($79.99)
```
VC-pro_yearly-a1b2c3d4e5f6...-1862169600000-abc123def456...
```

### Lifetime ($149.99)
```
VC-lifetime-a1b2c3d4e5f6...-4102444800000-abc123def456...
```

---

## 🚨 Important Warnings

### ❌ Do NOT Do This
1. **Don't expose admin-keygen.html publicly** - Password protect it!
2. **Don't use the default secret key** - Change it before production!
3. **Don't share the secret key** - Only admins should know it
4. **Don't generate keys without payment** - Always verify payment first
5. **Don't send keys via insecure channels** - Use Telegram or Email

### ✅ Do This
1. **Always verify payment** before sending a key
2. **Double-check the Device ID** the user provides
3. **Keep records** of all keys issued
4. **Monitor for abuse** - Watch for suspicious key requests
5. **Backup your records** regularly

---

## 📚 Documentation Files

1. **KEY_GENERATION_GUIDE.md** - Technical details of key generation
2. **ADMIN_GUIDE.md** - Step-by-step guide for admins
3. **ACTIVATION_SYSTEM.md** - This file (complete system overview)

---

## 🔍 Testing

### Test the System

1. **Install the app** on an emulator or device
2. **Get the Device ID** from the app
3. **Generate a test key** using admin-keygen.html
4. **Enter the key** in the app
5. **Verify features** are unlocked
6. **Try copying APK** to another emulator - it should fail

### Test Cases

| Test | Expected Result |
|------|-----------------|
| Enter valid key | Features unlocked |
| Enter invalid key | Error message |
| Enter key for different device | Error message |
| Use expired key | Error message |
| Copy APK to another device | Validation fails |
| Use Free Trial features | Works (limited) |
| Use Pro features without key | Feature locked |

---

## 🎉 Deployment Checklist

- [ ] Change secret key in all files
- [ ] Password protect admin-keygen.html
- [ ] Test key generation and validation
- [ ] Test device locking
- [ ] Test feature locking
- [ ] Update website with correct contact info
- [ ] Set up payment processing workflow
- [ ] Train admins on key generation
- [ ] Backup user records

---

## 📞 Support

For questions about this system:
- Check the documentation files
- Review the code comments
- Test with sample keys first

**Remember:** This is a manual system. For automation (online payments), you would need to:
1. Set up a payment processor (Stripe, PayPal, etc.)
2. Create a server-side key generation API
3. Integrate with the app for automatic activation

But the current manual system works perfectly for small-scale operations!
