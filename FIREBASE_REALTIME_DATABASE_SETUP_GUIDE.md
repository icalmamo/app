# Firebase Realtime Database Setup Guide - Step by Step

## Complete Setup Guide for H-CAS Healthcare App

This guide will walk you through setting up Firebase Realtime Database correctly for your healthcare app.

---

## Step 1: Create Firebase Project (If Not Already Created)

### 1.1 Go to Firebase Console
1. Open your browser and go to: **https://console.firebase.google.com/**
2. Sign in with your Google account

### 1.2 Create New Project (or Select Existing)
1. Click **"Add project"** (or select existing project "HCAS")
2. Enter project name: **HCAS** (or your preferred name)
3. Click **"Continue"**
4. (Optional) Disable Google Analytics if not needed
5. Click **"Create project"**
6. Wait for project creation (takes ~30 seconds)
7. Click **"Continue"**

---

## Step 2: Register Android App

### 2.1 Add Android App to Project
1. In Firebase Console, click the **Android icon** (or **"Add app"** → **Android**)
2. Enter Android package name: **com.example.h_cas**
3. Enter App nickname (optional): **H-CAS Healthcare**
4. Enter Debug signing certificate SHA-1 (optional - can skip for now)
5. Click **"Register app"**

### 2.2 Download google-services.json
1. Click **"Download google-services.json"**
2. **IMPORTANT:** Place the file in: `app/src/main/google-services.json`
3. Click **"Next"** → **"Next"** → **"Continue to console"**

---

## Step 3: Create Realtime Database

### 3.1 Navigate to Realtime Database
1. In Firebase Console, click **"Realtime Database"** in the left sidebar
2. If you see "Cloud Firestore" instead, look for **"Realtime Database"** below it

### 3.2 Create Database
1. Click **"Create Database"** button
2. **Select location:**
   - Choose **"asia-southeast1"** (Singapore) - closest to Philippines
   - Or choose your preferred region
   - Click **"Next"**
3. **Set up security rules:**
   - For now, select **"Start in test mode"** (allows read/write for 30 days)
   - Click **"Enable"**
   - Wait for database creation (~10 seconds)

### 3.3 Verify Database Created
You should see:
- Database URL: `https://your-project-default-rtdb.asia-southeast1.firebasedatabase.app`
- Status: **"Active"**
- Location: **Singapore (asia-southeast1)**

---

## Step 4: Configure Security Rules

### 4.1 Go to Rules Tab
1. In Realtime Database page, click **"Rules"** tab at the top

### 4.2 Set Security Rules

**Option A: For Testing (Temporary - Allows anyone)**
```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

**Option B: With Authentication (Recommended - After Step 5)**
```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null"
  }
}
```

### 4.3 Publish Rules
1. Click **"Publish"** button
2. Wait for confirmation: **"Rules published successfully"**

---

## Step 5: Enable Anonymous Authentication

### 5.1 Navigate to Authentication
1. In Firebase Console, click **"Authentication"** in the left sidebar
2. If first time, click **"Get started"**

### 5.2 Enable Anonymous Sign-in
1. Click **"Sign-in method"** tab
2. Find **"Anonymous"** in the providers list
3. Click **"Anonymous"**
4. Toggle **"Enable"** to ON
5. Click **"Save"**
6. You should see: **"Anonymous provider enabled"**

---

## Step 6: Verify google-services.json

### 6.1 Check File Location
1. Open your project in Android Studio/Cursor
2. Navigate to: `app/src/main/google-services.json`
3. Verify the file exists

### 6.2 Verify File Contents
The file should contain:
```json
{
  "project_info": {
    "project_number": "...",
    "project_id": "your-project-id",
    "storage_bucket": "..."
  },
  "client": [
    {
      "client_info": {
        "package_name": "com.example.h_cas"
      },
      ...
    }
  ]
}
```

---

## Step 7: Verify App Configuration

### 7.1 Check build.gradle
Open `build.gradle` and verify:
```gradle
dependencies {
    // Firebase BoM
    implementation platform(libs.firebase.bom)
    
    // Firebase products
    implementation libs.firebase.analytics
    implementation 'com.google.firebase:firebase-database'  // Realtime Database
    implementation libs.firebase.auth  // For anonymous auth
    implementation libs.firebase.storage
}
```

### 7.2 Check AndroidManifest.xml
Verify `google-services` plugin is applied:
```gradle
// In build.gradle (project level or app level)
apply plugin: 'com.google.gms.google-services'
```

---

## Step 8: Test Firebase Connection

### 8.1 Build and Run App
1. Build the app: **Build → Rebuild Project**
2. Run the app on device/emulator

### 8.2 Check Logs for Successful Connection
```bash
adb logcat -s HCasApplication:*
```

**Look for:**
```
✅ Firebase initialized successfully
✅ Firebase Anonymous Authentication successful
✅ Firebase real-time sync started
```

### 8.3 Test Patient Registration
1. Open app on Device 1
2. Register a new patient
3. Check logs:
```bash
adb logcat -s HCasDatabaseHelper:* FirebaseSyncManager:* FirebaseHelper:*
```

**Expected logs:**
```
🔄 syncToFirebase called for type: patient
📤 Starting Firebase sync for patient
✅ Patient PAT001 synced to Firebase Realtime Database successfully!
```

### 8.4 Verify Data in Firebase Console
1. Go to Firebase Console → Realtime Database → Data tab
2. Refresh the page
3. You should see: `patients → PAT001 → { patient data }`

---

## Step 9: Test Real-time Sync (Multi-Device)

### 9.1 Device 1 (Nurse Side)
1. Register a new patient
2. Check Firebase Console - data should appear

### 9.2 Device 2 (Doctor Side)
1. Open app (should already be running)
2. Go to patient list/monitoring screen
3. **Patient should appear automatically** (real-time sync)
4. Check logs:
```bash
adb logcat -s FirebaseSyncManager:* FirebaseHelper:*
```

**Expected logs:**
```
Added new patient from Firebase Realtime Database: PAT001
```

---

## Step 10: Verify Security Rules (After Testing)

### 10.1 Update Rules for Production
Once testing is complete, update rules:

```json
{
  "rules": {
    "patients": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "medicines": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "prescriptions": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

### 10.2 Publish Rules
1. Click **"Publish"**
2. Rules are now secure

---

## Troubleshooting

### Issue: "Permission denied" error

**Solution:**
1. Check if Anonymous Authentication is enabled (Step 5)
2. Check security rules allow writes (Step 4)
3. Restart app after enabling authentication

### Issue: Database is empty in Firebase Console

**Solution:**
1. Check logs for sync errors
2. Verify `google-services.json` is in correct location
3. Rebuild app after adding `google-services.json`
4. Check network connectivity

### Issue: Device 2 not receiving updates

**Solution:**
1. Verify listeners are started (check logs)
2. Check both devices are online
3. Verify both devices have same Firebase project
4. Check security rules allow reads

### Issue: "Firebase not initialized" error

**Solution:**
1. Verify `google-services.json` exists
2. Rebuild project
3. Check internet connection
4. Verify Firebase project is active

---

## Verification Checklist

Before considering setup complete, verify:

- [ ] Firebase project created
- [ ] Android app registered in Firebase
- [ ] `google-services.json` downloaded and placed correctly
- [ ] Realtime Database created
- [ ] Security rules configured
- [ ] Anonymous Authentication enabled
- [ ] App builds successfully
- [ ] Firebase connection logs show success
- [ ] Patient registration syncs to Firebase
- [ ] Data appears in Firebase Console
- [ ] Multi-device sync works (Device 2 receives updates)

---

## Quick Reference

### Firebase Console URLs:
- **Project Overview:** https://console.firebase.google.com/project/your-project-id
- **Realtime Database:** https://console.firebase.google.com/project/your-project-id/database/your-project-id-default-rtdb/data
- **Authentication:** https://console.firebase.google.com/project/your-project-id/authentication
- **Project Settings:** https://console.firebase.google.com/project/your-project-id/settings

### Important Log Tags:
```bash
# Firebase initialization
adb logcat -s HCasApplication:*

# Database sync
adb logcat -s HCasDatabaseHelper:* FirebaseSyncManager:* FirebaseHelper:*

# All Firebase logs
adb logcat -s FirebaseHelper:* FirebaseSyncManager:* HCasDatabaseHelper:* HCasApplication:*
```

---

## Next Steps

After setup is complete:
1. Test with multiple devices
2. Monitor Firebase Console for data
3. Update security rules for production
4. Consider setting up Firebase Storage for profile pictures
5. Set up Firebase Analytics for app usage tracking

---

## Support

If you encounter issues:
1. Check the troubleshooting section above
2. Review Firebase Console for errors
3. Check app logs using `adb logcat`
4. Verify all steps were completed correctly

**Setup is complete when:**
- ✅ Data syncs from Device 1 to Firebase
- ✅ Data appears in Firebase Console
- ✅ Device 2 automatically receives updates
- ✅ No errors in logs



