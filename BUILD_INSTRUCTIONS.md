# Build Instructions for Media Mate

## Quick Build (Command Line)

If you have Android SDK installed, you can build directly from command line:

```bash
cd /home/davew/claude/mobile_mate_app

# Make gradlew executable
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# APK location after build:
# app/build/outputs/apk/debug/app-debug.apk
```

## Android Studio Build

1. Open Android Studio
2. File → Open → Navigate to `/home/davew/claude/mobile_mate_app`
3. Let Gradle sync complete (may take a few minutes first time)
4. Build → Make Project (or press Ctrl+F9)
5. Build → Build Bundle(s) / APK(s) → Build APK(s)
6. Click "locate" in the notification to find your APK

## Direct Installation Methods

### Method 1: ADB (Android Debug Bridge)
```bash
# Connect your Pixel 5 via USB
# Enable USB debugging in Developer Options

# Install the APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Or reinstall if updating
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Method 2: Direct Transfer
1. Copy `app-debug.apk` to your phone via:
   - USB cable (drag to Downloads folder)
   - Google Drive
   - Email to yourself
2. On your phone, open Files app
3. Navigate to the APK
4. Tap to install (may need to enable "Install from unknown sources")

## Troubleshooting Build Issues

### Gradle sync fails
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

### SDK not found
Edit `local.properties` and ensure SDK path is correct:
```
sdk.dir=/path/to/your/Android/Sdk
```

### Permission denied on gradlew
```bash
chmod +x gradlew
```

## Testing the App

### Initial Setup
1. Ensure both phones are charged
2. Enable Bluetooth on both devices
3. Pair the phones first (Settings → Bluetooth)
4. Install Media Mate on your Pixel 5
5. Grant all requested permissions

### Test Sequence
1. Start playing music on wife's phone (Spotify/YouTube Music)
2. Lock her phone screen
3. Open Media Mate on your phone
4. Tap "Connect"
5. Try Play/Pause button
6. Test volume controls
7. Test skip forward/back

### Expected Behavior
- Connection should establish in 2-3 seconds
- Commands execute within 1 second
- No screen wake or notifications on wife's phone
- Auto-reconnects if you walk away and return

## Creating a Signed APK (Optional)

For a production-ready APK:

1. Build → Generate Signed Bundle / APK
2. Choose APK
3. Create new keystore (first time only)
4. Fill in key details
5. Select release build type
6. Build

Note: Keep your keystore file safe - you'll need it for updates!