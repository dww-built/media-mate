# Media Mate - Claude Code Project Handoff

## 1. Project Scope & Objective

**THE PROBLEM**: Wife falls asleep listening to audiobooks (Audible) or music (Spotify). Husband wants to pause/control her media WITHOUT waking her up. Current solutions either don't work when phone is locked or require notifications that wake her.

**THE SOLUTION**: Use Bluetooth HID spoofing to make husband's phone pretend to be wireless headphones with remote control buttons.

**Core Use Case**: 
- **Setup Phase** (when wife is awake): Pair phones once, like connecting Bluetooth headphones
- **Runtime Phase** (wife is asleep): Husband uses his phone to send silent media commands to her locked phone
- Her phone thinks the commands come from "headphone buttons" and responds instantly
- **ZERO interaction required from sleeping wife** - no notifications, no screen wake-up, no sounds

**Key Requirements**:
- ✅ Must work when wife's phone is locked and she's asleep
- ✅ Absolutely silent operation (no notifications/wake-ups)
- ✅ Control any media app (Audible, Spotify, YouTube Music, etc.)
- ✅ Simple one-time setup when both are awake
- ✅ Works from anywhere in house (Bluetooth range)

**What This Is NOT**:
- ❌ Not an app that needs to run on wife's phone during sleep
- ❌ Not dependent on WiFi or internet connection
- ❌ Not requiring any interaction from sleeping person

**Target Platform**: Android only (sideload APK, no Play Store needed)

## 2. Technical Approach - Bluetooth HID Spoofing

**Strategy**: Make phone pretend to be a Bluetooth speaker/headphones with remote control buttons.

**Why This Works**:
- Receiving phone thinks commands are from hardware (headphones remote)
- Works even when device is locked/sleeping
- No app needed on receiving side after pairing
- Universal compatibility with any media app
- Bypasses all Android/iOS restrictions

**Core Technology**:
- Bluetooth HID (Human Interface Device) Profile
- Standard media control HID key codes
- Bluetooth RFCOMM socket communication

## 3. Architecture Overview

**SETUP PHASE** (Both awake):
- Install Media Mate app on husband's phone only
- Pair husband's phone to wife's phone (like connecting Bluetooth headphones)
- Test controls work
- Done! Wife can go to sleep, no app needed on her phone

**RUNTIME PHASE** (Wife asleep):
```
Husband's Phone (Controller)    Wife's Phone (Sleeping/Locked)
┌─────────────────┐            ┌──────────────────┐
│ Media Mate App  │     BT     │ Android System   │
│ ┌─────────────┐ │    ════>   │ (Locked Screen)  │
│ │ [Play/Pause]│ │    HID     │                  │
│ │ [Volume -] │ │  Commands   │ Media Session    │
│ │ [Volume +] │ │            │ Handler          │
│ │ [Skip >>]   │ │            │                  │
│ └─────────────┘ │            │ Audible/Spotify  │
│                 │            │ (Playing Audio)  │
│ HID Service     │            │                  │
│ (Pretends to be │            │ ← Responds like  │
│  BT Headphones) │            │   hardware button│
└─────────────────┘            └──────────────────┘
```

**Key Point**: Wife's phone thinks it's getting commands from connected Bluetooth headphones, so it responds instantly without any notifications or screen activation.

**Bluetooth HID Commands**:
```
PLAY_PAUSE = 0xCD
SCAN_NEXT = 0xB5  
SCAN_PREV = 0xB6
VOLUME_UP = 0xE9
VOLUME_DOWN = 0xEA
MUTE = 0xE2
```

## 4. Implementation Phases

### Phase 1: Bluetooth HID Foundation
**Files to Create**:
- `MainActivity.kt` - Main UI with control buttons
- `BluetoothHidService.kt` - Core HID device emulation
- `HidProfile.kt` - HID descriptor and key code definitions
- `DevicePairingActivity.kt` - Bluetooth pairing flow

**Core Functionality**:
- Advertise as HID device
- Accept incoming Bluetooth connections
- Send HID key codes for media control

### Phase 2: User Interface
**Files to Create**:
- `activity_main.xml` - Control button layout
- `PairedDevicesAdapter.kt` - Show connected devices
- `ConnectionStatusService.kt` - Monitor Bluetooth connection state

**UI Elements**:
- Large, easy-to-tap media control buttons
- Connection status indicator
- List of paired devices
- Simple pairing interface

### Phase 3: Bidirectional Control
**Files to Create**:
- `DeviceManager.kt` - Handle multiple paired devices
- `CommandProcessor.kt` - Process incoming HID commands

**Functionality**:
- Both phones can act as controller AND receiver
- Switch between controlling different paired devices
- Handle simultaneous connections

### Phase 4: Polish & Reliability
**Files to Create**:
- `BackgroundService.kt` - Keep HID service running
- `BatteryOptimizationHelper.kt` - Guide user through battery settings
- `PermissionManager.kt` - Handle Bluetooth permissions

## 5. Key Android Components & Permissions

### Required Permissions (AndroidManifest.xml):
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### Core Classes to Implement:
1. **BluetoothAdapter** - For device discovery and connections
2. **BluetoothServerSocket** - Listen for incoming connections  
3. **BluetoothSocket** - Send HID commands
4. **Service** - Background HID service
5. **BroadcastReceiver** - Monitor Bluetooth state changes

### HID Descriptor (Critical Implementation Detail):
```kotlin
// HID Report Descriptor for Consumer Control (media keys)
val hidDescriptor = byteArrayOf(
    0x05, 0x0C,        // Usage Page (Consumer)
    0x09, 0x01,        // Usage (Consumer Control)
    0xA1, 0x01,        // Collection (Application)
    0x15, 0x00,        // Logical Minimum (0)
    0x25, 0x01,        // Logical Maximum (1)
    0x09, 0xCD,        // Usage (Play/Pause)
    0x09, 0xB5,        // Usage (Scan Next Track)
    0x09, 0xB6,        // Usage (Scan Previous Track)
    0x09, 0xE9,        // Usage (Volume Up)
    0x09, 0xEA,        // Usage (Volume Down)
    0x75, 0x01,        // Report Size (1)
    0x95, 0x05,        // Report Count (5)
    0x81, 0x02,        // Input (Data,Var,Abs)
    0x75, 0x03,        // Report Size (3)
    0x95, 0x01,        // Report Count (1)
    0x81, 0x03,        // Input (Cnst,Var,Abs)
    0xC0               // End Collection
)
```

## 6. Testing & Validation Strategy

### Testing Phases:
1. **Unit Tests**: HID command generation and parsing
2. **Device Pairing**: Test pairing flow between two Android devices
3. **Media Control**: Verify commands work with various media apps
4. **Sleep Mode**: Test when receiving device is locked/sleeping
5. **Battery Impact**: Ensure minimal battery drain
6. **Edge Cases**: Handle Bluetooth disconnections, app crashes

### Test Scenarios:
- [ ] **CRITICAL**: Pair two phones, lock wife's phone, control Audible audiobook without any screen activity
- [ ] **CRITICAL**: Send commands while wife's phone is in Do Not Disturb mode - no notifications appear
- [ ] **CRITICAL**: Control media for 30+ minutes while wife's phone screen stays off
- [ ] Pair two Android phones successfully in under 2 minutes
- [ ] Send play/pause commands - execute within 1 second
- [ ] Control Audible audiobook playback while phone locked
- [ ] Control Spotify music playback while phone locked  
- [ ] Control YouTube Music while phone locked
- [ ] Volume up/down commands work silently
- [ ] Skip forward/backward commands work
- [ ] Handle Bluetooth disconnection gracefully (auto-reconnect)
- [ ] Work from 30+ feet away (across house)
- [ ] Minimal battery drain on both devices over 8 hour period
- [ ] Commands work even when wife's phone is at 5% battery with battery saver on

### Success Criteria:
- [ ] **PRIMARY GOAL**: Reliably control wife's media while she sleeps without any disturbance
- [ ] One-time pairing setup takes under 2 minutes when both awake  
- [ ] Zero interaction required from wife after pairing
- [ ] Works with major media apps (Audible, Spotify, YouTube Music)
- [ ] Husband can control from anywhere in house (30+ feet range)
- [ ] Commands execute within 1 second (instant response)
- [ ] No notifications, sounds, or screen wake-up on wife's device
- [ ] Stable connection for 8+ hours (full night's sleep)
- [ ] Minimal battery impact on both devices

---

## 7. Critical "Hands Off" Requirements

**After Initial Pairing Setup**:
- Wife's phone requires ZERO apps running in background
- Wife's phone requires ZERO user interaction 
- Wife's phone can be fully locked with screen off
- Wife can be completely asleep and unaware
- No push notifications of any kind
- No audio alerts or vibrations
- No screen lighting up or unlocking
- Commands work through Do Not Disturb mode
- Works even if wife's phone battery saver is active

**This is the core technical challenge**: The receiving phone (wife's) must respond to media commands as if they came from hardware buttons on connected headphones, requiring zero software interaction on her end.

---

**Development Notes**:
- Start with basic HID implementation in Phase 1
- Test early and often with real devices
- Focus on reliability over features
- Keep UI simple and functional
- Battery optimization is critical for acceptance