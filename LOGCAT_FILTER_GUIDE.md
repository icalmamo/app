# 📱 How to Filter Logcat in Android Studio

## ❌ WRONG Way (Command Line Format):
```
adb logcat | findstr "FirebaseHelper FirebaseSyncManager HCasDatabaseHelper Patient"
```
**This won't work in Android Studio Logcat filter box!**

## ✅ CORRECT Way (Android Studio Format):

### Option 1: Simple Tag Filter
Sa Logcat filter box, type:
```
FirebaseHelper
```
O kaya:
```
FirebaseSyncManager
```

### Option 2: Multiple Tags (Use OR)
```
package:FirebaseHelper | package:FirebaseSyncManager | package:HCasDatabaseHelper
```

### Option 3: Simple Text Search
Just type:
```
Firebase
```
O kaya:
```
Patient
```

---

## 🔍 Step-by-Step:

1. **Clear the current filter:**
   - Click the "x" icon sa filter box
   - Or click "Clear filter" link

2. **Enter simple filter:**
   - Type: `FirebaseHelper`
   - Or type: `Firebase` (mas broad)

3. **Add a patient** sa app

4. **Watch the logs** - dapat may lalabas na logs

---

## 📋 What to Look For:

After adding a patient, you should see logs like:
```
🔄 syncToFirebase called for type: patient
📤 Starting Firebase sync for patient
🔄 Starting to sync patient: PAT001
✅ Patient PAT001 synced to Firebase successfully!
```

---

## 🚨 If Still No Logs:

1. **Clear Logcat** (trash icon)
2. **Remove ALL filters** (click "x" sa filter box)
3. **Add patient** ulit
4. **Scroll through logs** - hanapin ang "Firebase" o "Patient"
5. **Share the logs** na may Firebase-related messages





