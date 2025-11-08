# How to View Your Firebase Realtime Database

## Current Screen (Authentication)
What you're seeing now is the **Authentication** section, which shows:
- ✅ **Anonymous user** - This is good! It means your app successfully authenticated
- This is where you manage user sign-in methods

## Where to Find Your Actual Database

### Step 1: Navigate to Realtime Database
1. In the **left sidebar**, look for **"Realtime Database"**
2. Click on it
3. You should see your database URL: `https://hcas-c83fa-default-rtdb.asia-southeast1.firebasedatabase.app/`

### Step 2: View Your Data
Once you're in Realtime Database, you should see:
- A tree structure showing your data nodes
- If patients have been synced, you'll see a **`patients`** node
- Click on **`patients`** to expand and see individual patient records

### Step 3: Check Data Structure
Your data should look like this:
```
Realtime Database
├── connectionTest: "Hello Firebase!"
├── patients
│   ├── PAT001
│   │   ├── first_name: "John"
│   │   ├── last_name: "Doe"
│   │   ├── email: "john@example.com"
│   │   └── ...
│   └── PAT002
│       └── ...
├── medicines
└── prescriptions
```

## If You Don't See Data

### Check 1: Verify Anonymous Authentication is Enabled
1. Go to **Authentication** → **Sign-in method** tab
2. Find **Anonymous** in the list
3. Make sure it shows **"Enabled"** (not "Disabled")
4. If disabled, click **Enable** and **Save**

### Check 2: Check Security Rules
1. Go to **Realtime Database** → **Rules** tab
2. Make sure rules allow authenticated writes:
```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null"
  }
}
```

Or for testing (temporary):
```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

### Check 3: Test in Your App
1. Open your app in Android Studio emulator
2. Register a new patient
3. Check Logcat for:
   - `✅ Authenticated user: [user-id]`
   - `✅ Data synced successfully to path: patients/PAT001`
4. Refresh Firebase Console → Realtime Database

### Check 4: Verify Connection Test
Your MainActivity writes to `connectionTest` node. Check if you see:
- In Firebase Console: `connectionTest: "Hello Firebase!"`
- If this appears, Firebase is connected correctly

## Navigation Path
```
Firebase Console
├── Authentication (you are here - shows users)
│   ├── Users tab (shows anonymous user ✅)
│   └── Sign-in method tab (enable Anonymous here)
│
└── Realtime Database (go here to see patient data)
    ├── Data tab (your actual database)
    └── Rules tab (security rules)
```

## Quick Checklist
- [ ] Anonymous user appears in Authentication (✅ You have this!)
- [ ] Anonymous Authentication is enabled in Sign-in method tab
- [ ] Security rules allow writes
- [ ] App logs show "✅ Data synced successfully"
- [ ] Realtime Database shows `patients` node with data

## Next Steps
1. Click **Realtime Database** in the left sidebar
2. Look for the **`patients`** node
3. If you see it, click to expand and view patient data
4. If you don't see it, register a new patient in the app and check Logcat for errors


