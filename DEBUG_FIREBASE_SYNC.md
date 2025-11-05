# Debug Guide: Firebase Realtime Database Sync Issues

## Problem: Patient registered on Device 1 but not showing on Device 2

### Step 1: Check Firebase Authentication

1. **Enable Anonymous Authentication:**
   - Firebase Console → Authentication → Sign-in method → Anonymous → Enable → Save

2. **Verify Authentication in Logs:**
   ```bash
   adb logcat -s HCasApplication:*
   ```
   Look for:
   - `✅ Firebase Anonymous Authentication successful`
   - `✅ Firebase user already authenticated`

### Step 2: Check Write Operations (Device 1)

When you register a patient on Device 1, check logs:

```bash
adb logcat -s HCasDatabaseHelper:* FirebaseSyncManager:* FirebaseHelper:*
```

**Expected logs:**
```
🔄 syncToFirebase called for type: patient
📤 Starting Firebase sync for patient
🔄 Starting to sync patient: PAT001
📤 Attempting to sync patient to path: patients/PAT001
✅ Patient PAT001 synced to Firebase Realtime Database successfully!
```

**If you see errors:**
- `❌ Error syncing patient to Firebase` → Check authentication or security rules
- `⚠️ Not authenticated` → Enable Anonymous Authentication
- `Realtime Database not available` → Check Firebase initialization

### Step 3: Check Firebase Console

After registering a patient:
1. Go to Firebase Console → Realtime Database → Data tab
2. Refresh the page
3. You should see: `patients → PAT001 → { patient data }`

**If empty:**
- Check security rules allow writes
- Check authentication is enabled
- Check logs for errors

### Step 4: Check Read Operations (Device 2)

On Device 2, check if listeners are working:

```bash
adb logcat -s FirebaseSyncManager:* FirebaseHelper:*
```

**Expected logs:**
```
Starting Firebase real-time listeners...
✅ Firebase real-time listeners started
```

**When data changes:**
```
Added new patient from Firebase Realtime Database: PAT001
```

### Step 5: Verify Security Rules

Firebase Console → Realtime Database → Rules tab

**Should be:**
```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null"
  }
}
```

**For testing (temporary):**
```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

### Step 6: Test Sync Manually

1. **Device 1:** Register a patient
2. **Check Firebase Console:** Should see data appear
3. **Device 2:** Should automatically receive the update
4. **Check Device 2 logs:** Should see "Added new patient from Firebase"

### Common Issues

1. **"Permission denied" error:**
   - Anonymous Authentication not enabled
   - Security rules blocking access

2. **Data not appearing in Firebase Console:**
   - Write operation failed
   - Check logs for errors
   - Verify authentication

3. **Device 2 not receiving updates:**
   - Listeners not started
   - Network issues
   - Authentication failed

### Quick Fixes

1. **Enable Anonymous Authentication** (Firebase Console)
2. **Check security rules** (allow reads/writes)
3. **Restart both apps** (to reinitialize Firebase)
4. **Check logs** on both devices

### Verification Checklist

- [ ] Anonymous Authentication enabled in Firebase Console
- [ ] Security rules allow `auth != null` reads/writes
- [ ] Device 1 logs show successful sync
- [ ] Data appears in Firebase Console
- [ ] Device 2 logs show listener started
- [ ] Device 2 receives updates automatically
