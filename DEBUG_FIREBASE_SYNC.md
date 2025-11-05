# 🔍 Debug Firebase Sync - Step by Step

## Problem: Walang lumalabas sa Firebase Console pagkatapos mag-add ng patient

## Step 1: Check Logcat Properly

### Sa Android Studio:
1. Open **Logcat** (View → Tool Windows → Logcat)
2. **Clear** the logcat (trash icon)
3. **Filter** by typing: `FirebaseHelper` OR `FirebaseSyncManager` OR `HCasDatabaseHelper`
4. **Add a patient** sa app
5. **Watch the logs** - dapat may lalabas na:
   - `🔄 syncToFirebase called for type: patient`
   - `🔄 Starting to sync patient: PAT001`
   - `✅ Patient PAT001 synced to Firebase successfully!`

### O kaya gamitin ang command line:
```bash
adb logcat | grep -E "FirebaseHelper|FirebaseSyncManager|HCasDatabaseHelper|Patient"
```

---

## Step 2: Check if Firebase is Initialized

Look for these logs:
```
✅ Firebase initialized successfully
✅ Firebase real-time sync started
```

Kung **WALANG** ganitong logs, ang Firebase ay hindi naka-initialize!

---

## Step 3: Check for Errors

Look for **red** logs na may:
- `❌ Error`
- `PERMISSION_DENIED`
- `UNAVAILABLE`
- `Exception`

---

## Step 4: Test Firebase Connection

1. Go to **Settings** → Click **"🧪 Test Firebase Sync"**
2. Check Logcat - dapat may success message
3. Check Firebase Console - dapat may test medicine

---

## Step 5: Check Firestore Rules

1. Go to Firebase Console
2. Firestore Database → **Rules** tab
3. Dapat ganito ang rules (for testing):
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /{document=**} {
         allow read, write: if true;
       }
     }
   }
   ```
4. Click **"Publish"**

---

## Step 6: Verify google-services.json

1. Check if `google-services.json` exists:
   - `app/google-services.json` ✅
   - `src/main/google-services.json` ✅
2. Make sure it's the correct file for your Firebase project

---

## Common Issues:

### Issue 1: No Firebase Logs at All
**Solution:** Baka hindi naka-initialize ang Firebase. Check `HCasApplication.java` initialization.

### Issue 2: PERMISSION_DENIED Error
**Solution:** Update Firestore rules (Step 5)

### Issue 3: Sync called pero walang success
**Solution:** Check internet connection at Firebase project settings

### Issue 4: SyncManager is null
**Solution:** Check if `HCasDatabaseHelper` is properly initialized

---

## Quick Test Command:

Run this command para makita lahat ng Firebase-related logs:
```bash
adb logcat -s FirebaseHelper:* FirebaseSyncManager:* HCasDatabaseHelper:* HCasApplication:*
```

---

## What to Look For:

### ✅ Success Pattern:
```
🔄 syncToFirebase called for type: patient
📤 Starting Firebase sync for patient
🔄 Starting to sync patient: PAT001
📤 Sending patient data to Firebase
✅ Patient PAT001 synced to Firebase successfully!
   Patient Name: Juan Dela Cruz
   Check Firebase Console → Firestore → patients collection
```

### ❌ Error Pattern:
```
❌ Error syncing patient to Firebase
   Error: PERMISSION_DENIED
```

O kaya:
```
❌ syncManager is null
❌ Context is null, cannot sync
```

---

**Run the test command above and share the output!**



