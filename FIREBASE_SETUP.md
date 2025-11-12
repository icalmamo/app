# Firebase Setup Guide para sa HCAS App

## Paano i-setup ang Firebase sa Android App

### Step 1: Gumawa ng Firebase Project
1. Pumunta sa [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** o **"Create a project"**
3. Lagyan ng name: `HCAS-Healthcare` (o anumang name)
4. Sundin ang setup wizard
5. Enable **Google Analytics** (optional pero recommended)

### Step 2: Mag-add ng Android App
1. Sa Firebase Console, click ang **Android icon** (🅰️)
2. Ilagay ang **Package name**: `com.example.h_cas`
   - Makikita mo ito sa `build.gradle` → `applicationId`
3. Ilagay ang **App nickname** (optional): `HCAS Healthcare`
4. Click **"Register app"**

### Step 3: Download ang google-services.json
1. Download ang `google-services.json` file
2. **IMPORTANTE**: Ilagay ang file sa:
   ```
   app/src/main/google-services.json
   ```
   O kung ang structure mo ay:
   ```
   src/main/google-services.json
   ```

### Step 4: Enable Firestore Database
1. Sa Firebase Console, pumunta sa **"Firestore Database"** sa left sidebar
2. Click **"Create database"**
3. Piliin ang **"Start in test mode"** (para sa development)
4. Piliin ang **location** (closest to your region)
5. Click **"Enable"**

### Step 5: I-sync ang Gradle
1. Sa Android Studio, click **"Sync Now"** kapag may notification
2. O i-run ang command:
   ```bash
   ./gradlew build
   ```

## ✅ Tapos na!

Ang Firebase ay naka-integrate na sa app mo. Pwede mo nang gamitin ang:
- **Firestore** - Cloud database
- **Firebase Auth** - Authentication (future use)
- **Firebase Storage** - File storage (future use)
- **Firebase Analytics** - App analytics

## Paano gamitin ang FirebaseHelper

```java
// Initialize
FirebaseHelper firebaseHelper = new FirebaseHelper();

// Sync employee data
Map<String, Object> employeeData = new HashMap<>();
employeeData.put("employee_id", "EMP001");
employeeData.put("first_name", "John");
employeeData.put("last_name", "Doe");
employeeData.put("role", "Doctor");

firebaseHelper.syncEmployeeToFirebase("EMP001", employeeData);

// Sync patient data
Map<String, Object> patientData = new HashMap<>();
patientData.put("patient_id", "PAT001");
patientData.put("first_name", "Jane");
patientData.put("last_name", "Smith");

firebaseHelper.syncPatientToFirebase("PAT001", patientData);
```

## Security Rules para sa Firestore

Kapag ready ka na para sa production, update ang Firestore security rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow read/write access to authenticated users only
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## Important Notes

⚠️ **google-services.json** - Dapat nandito ang file na ito, kung hindi, hindi magwo-work ang Firebase.

⚠️ **Internet Permission** - Dapat may internet connection para mag-work ang Firebase.

⚠️ **Test Mode** - Kapag naka-test mode ang Firestore, pwede lahat ng users mag-read/write. I-update ang rules para sa production.

## Troubleshooting

**Error: "Default FirebaseApp is not initialized"**
- Siguraduhin na naka-place ang `google-services.json` sa tamang location
- I-clean at rebuild ang project: `Build → Clean Project` then `Build → Rebuild Project`

**Error: "FirebaseApp not found"**
- I-sync ang Gradle files
- Siguraduhin na naka-add ang Google Services plugin sa `build.gradle`

## Next Steps

1. ✅ Download ang `google-services.json` mula sa Firebase Console
2. ✅ Ilagay sa `app/src/main/` o `src/main/`
3. ✅ Sync ang Gradle
4. ✅ Test ang app!











