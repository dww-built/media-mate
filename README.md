# Media Mate - Android App MVP

## Overview
Media Mate allows you to control media playback on your wife's locked Android phone using Bluetooth AVRCP protocol. No interaction required from the sleeping person after initial pairing.

## Features
- Play/Pause control
- Skip forward/backward
- Volume up/down
- Auto-reconnect when in range
- Connection status display
- Works with locked phones
- Silent operation (no notifications on target device)

## Setup Instructions

### Prerequisites
1. Android Studio installed on your computer
2. USB debugging enabled on your Pixel 5
3. Both phones must have Bluetooth enabled

### Initial Pairing (One-time setup)
1. On wife's Pixel 9 Pro: Settings → Bluetooth
2. On your Pixel 5: Settings → Bluetooth → Pair new device
3. Select wife's phone from the list and complete pairing
4. Note: This is standard Bluetooth pairing, just like connecting headphones

### Building the APK
1. Open Android Studio
2. File → Open → Select `/home/davew/claude/mobile_mate_app`
3. Wait for Gradle sync to complete
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. APK will be in `app/build/outputs/apk/debug/app-debug.apk`

### Installing on Your Phone
1. Connect Pixel 5 via USB
2. Enable USB debugging in Developer Options
3. Run: `adb install app-debug.apk`
4. Or copy APK to phone and install directly

### Using the App
1. Launch Media Mate on your Pixel 5
2. Grant Bluetooth permissions when prompted
3. App will find wife's paired Pixel automatically
4. Tap "Connect" to establish connection
5. Use media controls to control playback on her phone
6. App will auto-reconnect if connection drops

## Technical Details
- Uses Bluetooth AVRCP (Audio/Video Remote Control Profile)
- Same protocol your car uses to control phone media
- Minimum Android 9 (API 28) for Pixel 5 compatibility
- Target Android 14 (API 34) for latest features
- Background service maintains connection

## Troubleshooting

### App doesn't find wife's phone
- Ensure phones are paired in Bluetooth settings first
- Check that Bluetooth is enabled on both devices
- Verify wife's phone name contains "Pixel"

### Commands not working
- Some media apps may not fully support AVRCP
- Try with Spotify or YouTube Music first for testing
- Ensure media is actually playing on target device

### Connection drops frequently
- Keep phones within 30 feet of each other
- Check battery optimization settings (app needs to run in background)
- Ensure Bluetooth is stable on both devices

## Next Steps for Production
1. Add device selection UI (not just auto-detect Pixel)
2. Implement full AVRCP protocol for better compatibility
3. Add connection strength indicator
4. Create custom notification controls
5. Add sleep timer feature

## Security Note
This app only controls media playback. It cannot:
- Access any data on the target phone
- Make calls or send messages
- Access notifications or private information
- Unlock the target device

The connection is equivalent to Bluetooth headphones with media buttons.