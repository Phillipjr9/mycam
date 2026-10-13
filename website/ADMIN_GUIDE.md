# Admin Guide - Virtual Camera Key Generation

## Quick Start

1. Open `admin-keygen.html` in a web browser (Chrome, Firefox, Edge, etc.)
2. Enter the user's information:
   - **User Email**: User's email address (for your records)
   - **Device ID**: User's device ID from the app (required)
   - **License Type**: The plan they purchased (free_trial, pro_monthly, pro_yearly, lifetime)
   - **Payment Proof**: Reference to payment (Telegram username, transaction ID, etc.)
   - **Notes**: Any additional information

3. Click **"Generate Key"**
4. Copy the generated activation key and send it to the user

## How Users Get Their Device ID

Users can get their Device ID by:
1. Opening the Virtual Camera app
2. Tapping the **"Get Device ID (For Activation)"** button
3. Their Device ID will be displayed
4. They can copy it and send it to you

## Key Generation Process

### Step 1: User Contacts You
User sends you:
- Proof of payment (screenshot, transaction ID, etc.)
- Their Device ID (from the app)
- Which plan they purchased

### Step 2: You Generate the Key
1. Open `admin-keygen.html`
2. Fill in the form with user's details
3. Click "Generate Key"
4. The page will display:
   - The activation key (send this to user)
   - License type
   - Device ID
   - Expiration date
   - Instructions for the user

### Step 3: Send Key to User
Send the activation key to the user via:
- Telegram (recommended for speed)
- Email

### Step 4: User Activates
User enters the key in the app:
1. Opens Virtual Camera app
2. Taps "Activate Pro"
3. Enters the activation key
4. Taps "Activate"
5. App validates and unlocks features

## Important Notes

### ⚠️ Device Locking
- **Each key works on ONE device only**
- The key is cryptographically tied to the user's Device ID
- If the user changes their device, they need a **new key**
- Copying the APK to another device **will NOT work**

### ⚠️ Security
- **Password protect** the `admin-keygen.html` page
- Do NOT expose it publicly on the internet
- Only authorized admins should have access
- The secret key used for signing is: `SECRET_KEY_2026_VC`
  - **CHANGE THIS** before production deployment

### ⚠️ Key Expiration
- Free Trial: 7 days from activation
- Pro Monthly: 1 month from activation
- Pro Yearly: 1 year from activation
- Lifetime: 99 years (effectively unlimited)

### ⚠️ User Records
- All generated keys are saved in your browser's localStorage
- Records include: email, device ID, license type, key, date
- Records persist between browser sessions
- To clear records: Clear browser data or use browser's "Clear site data" option

## Validating a User's Key

If a user reports their key isn't working, you can validate it:

1. On the `admin-keygen.html` page, scroll to "Key Validation"
2. Paste the user's activation key
3. Click "Validate Key"
4. The tool will tell you:
   - If the key is valid
   - License type
   - Device ID it's tied to
   - Expiration date
   - If it's expired

## Common Issues

### User says key doesn't work
1. **Check if they entered it correctly** (no spaces, all characters)
2. **Validate the key** using the validation tool
3. **Check device ID** - make sure they're using the same device
4. **Check expiration** - the key might have expired

### User changed device
- They need a **new activation key**
- Generate a new key with their new Device ID
- You may want to charge them again or offer a transfer (your policy)

### User wants to upgrade
- Generate a new key with the new license type
- The old key will still work until it expires
- User can use either key

## License Types Reference

| Type | Code | Price | Duration |
|------|------|-------|----------|
| Free Trial | `free_trial` | $0 | 7 days |
| Pro Monthly | `pro_monthly` | $9.99 | 1 month |
| Pro Yearly | `pro_yearly` | $79.99 | 1 year |
| Lifetime | `lifetime` | $149.99 | 99 years |

## Feature Access by License

See `KEY_GENERATION_GUIDE.md` for complete feature matrix.

## Troubleshooting

### "Invalid key format" error
- User didn't enter the full key
- There might be spaces at the beginning or end
- Ask user to copy the key exactly as you sent it

### "Invalid signature" error
- User modified the key
- The secret key was changed after the key was generated
- Generate a new key

### "Key has expired" error
- The license period has ended
- User needs to renew and get a new key

### "Device mismatch" error (not shown to user, but validation fails)
- User is trying to use the key on a different device
- Generate a new key for their new device

## Best Practices

1. **Always verify payment** before sending a key
2. **Double-check the Device ID** the user sends you
3. **Send the key securely** (Telegram or Email)
4. **Keep records** of all keys issued
5. **Regularly backup** your user records
6. **Monitor for abuse** - if someone requests too many keys, investigate

## Exporting User Records

To export your user records:
1. Open browser Developer Tools (F12)
2. Go to the Console tab
3. Run this command:
   ```javascript
   copy(JSON.stringify(JSON.parse(localStorage.getItem('userRecords') || '[]'), null, 2));
   ```
4. Paste into a text file to save

## Importing User Records

To import previously exported records:
1. Open browser Developer Tools (F12)
2. Go to the Console tab
3. Run this command (replace with your JSON):
   ```javascript
   localStorage.setItem('userRecords', JSON.stringify(YOUR_RECORDS_JSON));
   ```
4. Refresh the page
