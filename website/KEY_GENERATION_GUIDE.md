# Virtual Camera - Activation Key System

## Overview

The Virtual Camera app uses a **device-locked activation key system** to ensure each license is tied to ONE device only. This prevents unauthorized sharing and copying of the APK to multiple devices.

## How It Works

### 1. Device Identification
- Each device has a unique **Device ID** generated from the Android ID
- The Device ID is hashed using SHA-256 for privacy
- An **Installation Signature** is also generated based on:
  - Android ID
  - Package name
  - First install time
- This ensures the APK cannot be copied to another device

### 2. Key Generation
Activation keys have the following format:
```
VC-{LICENSE_TYPE}-{DEVICE_ID}-{EXPIRATION_DATE}-{SIGNATURE}
```

Example:
```
VC-pro_monthly-a1b2c3d4e5f6...-1735689600000-abc123def456...
```

**Components:**
- `VC` - Prefix identifying this as a Virtual Camera key
- `LICENSE_TYPE` - One of: `free_trial`, `pro_monthly`, `pro_yearly`, `lifetime`
- `DEVICE_ID` - SHA-256 hash of the device's Android ID
- `EXPIRATION_DATE` - Unix timestamp in milliseconds
- `SIGNATURE` - SHA-256 hash of: `{LICENSE_TYPE}|{DEVICE_ID}|{EXPIRATION_DATE}|SECRET_KEY_2026_VC`

### 3. Key Validation
The app validates a key by:
1. Checking the format is correct
2. Verifying the cryptographic signature
3. Confirming the Device ID matches the current device
4. Checking the APK is on the original installation device
5. Verifying the key hasn't expired

### 4. Feature Locking

| Feature | Free Trial | Pro Monthly | Pro Yearly | Lifetime |
|---------|-----------|-------------|------------|----------|
| Video File Playback | ✓ (5 min max) | ✓ | ✓ | ✓ |
| Static Image Display | ✓ | ✓ | ✓ | ✓ |
| Physical Camera | ✓ | ✓ | ✓ | ✓ |
| Text Overlays | ✓ (basic) | ✓ | ✓ | ✓ |
| Color Filters | ✓ (basic) | ✓ (advanced) | ✓ (advanced) | ✓ (advanced) |
| 1080p Full HD | ✓ | ✓ | ✓ | ✓ |
| 60 FPS | ✓ | ✓ | ✓ | ✓ |
| Custom Overlays | ✗ | ✓ | ✓ | ✓ |
| RTMP Live Streaming | ✗ | ✓ | ✓ | ✓ |
| Multiple Virtual Cameras | ✗ | ✗ | ✓ | ✓ |
| Clone Mode | ✗ | ✗ | ✓ | ✓ |
| Watch Mode | ✗ | ✗ | ✓ | ✓ |
| Freeze/Unfreeze | ✗ | ✗ | ✓ | ✓ |
| Speed Control | ✗ | ✗ | ✓ | ✓ |
| Commercial License | ✗ | ✗ | ✗ | ✓ |
| Priority Support | ✗ | ✓ | ✓ | ✓ |

## Usage Flow

### For Users:

1. **User installs the app**
   - App generates unique Device ID and Installation Signature
   - App starts in Free Trial mode (7 days)

2. **User decides to purchase Pro**
   - User selects a plan on the website
   - User contacts admin via Telegram or Email

3. **Admin generates activation key**
   - Admin uses the Key Generator tool
   - Admin enters user's Device ID (provided by user)
   - Admin selects the purchased license type
   - Admin sends the activation key to user

4. **User activates the app**
   - User opens app and taps "Activate Pro"
   - User enters the activation key
   - App validates the key
   - If valid, Pro features are unlocked

### For Admins:

1. **Receive payment proof** from user (via Telegram or Email)
2. **Ask user for their Device ID**
   - User can get it from the app by tapping "Get Device ID" button
3. **Generate activation key** using the admin tool:
   - Open `admin-keygen.html`
   - Enter user's email (for records)
   - Enter user's Device ID
   - Select the license type they purchased
   - Add payment proof reference
   - Click "Generate Key"
4. **Send key to user** via Telegram or Email
5. **Record the transaction** (automatically saved in browser localStorage)

## Security Features

### Device Locking
- Each key is cryptographically signed with the Device ID
- The key will only work on the device with matching Device ID
- Copying the APK to another device will fail validation

### Installation Binding
- The app stores an Installation Signature on first run
- This signature is based on device-specific values
- If the APK is copied to another device, the signature won't match
- This prevents APK sharing between devices

### Cryptographic Signing
- Keys are signed using SHA-256 hashing
- The signature includes a secret key (change this in production!)
- Without the secret key, it's computationally infeasible to generate valid keys

### Expiration
- Free Trial: 7 days
- Pro Monthly: 1 month
- Pro Yearly: 1 year
- Lifetime: 99 years (effectively unlimited)

## Implementation Details

### Android App Components

1. **LicenseManager.java**
   - Manages license state
   - Validates activation keys
   - Checks device binding
   - Manages feature flags

2. **KeyGenerator.java**
   - Utility for generating keys (can be used by admin tools)
   - Contains the same signing logic as LicenseManager

3. **MainActivity.java**
   - Shows license status
   - Provides activation dialog
   - Shows Device ID to user

4. **MediaProjectionService.java**
   - Enforces feature locking based on license
   - Prevents premium features without valid license

### Website Components

1. **index.html**
   - Payment modal with contact info
   - FAQ explaining activation process

2. **admin-keygen.html**
   - Admin tool for generating keys
   - Key validation tool
   - User record keeping

## Changing the Secret Key

**IMPORTANT:** Before deploying to production, change the secret key in:
- `LicenseManager.java` (line: `private static final String SECRET_KEY = 