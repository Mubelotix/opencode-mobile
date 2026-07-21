---
name: android-build
description: Build and deploy KMP app to Android devices via ADB with WiFi debugging
---

# Android Build & Deploy Skill

Self-improving skill for building and deploying this KMP app to Android devices via ADB. Update with new/corrected information as discovered.

## Machine Setup

### SDK
- ANDROID_HOME: `$HOME/android-sdk`
- sdkmanager: `$HOME/android-sdk/cmdline-tools/latest/bin/sdkmanager` (symlink → 13.0)
- local.properties: `sdk.dir=$HOME/android-sdk`
- gradle.properties: `kotlin.native.ignoreDisabledTargets=true` (suppress iOS simulator warnings on Linux)

### Required SDK Components
```bash
export ANDROID_HOME=$HOME/android-sdk
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

# Accept licenses (run once)
yes | $SDKMANAGER --sdk_root=$ANDROID_HOME --licenses

# Install missing components
$SDKMANAGER --sdk_root=$ANDROID_HOME --install "platforms;android-36" "build-tools;36.0.0"
```

### Initial SDK bootstrap (if nothing installed)
```bash
sudo apt-get install -y google-android-cmdline-tools-13.0-installer
mkdir -p ~/android-sdk/cmdline-tools
ln -sfn /usr/lib/android-sdk/cmdline-tools/13.0 ~/android-sdk/cmdline-tools/latest
```

## ADB & Device

### Connect via WiFi (Wireless Debugging, Android 11+)
1. On device: Developer options → Wireless debugging
2. Pair first: `adb pair <ip>:<pairing-port> <code>`
3. Connect: `adb connect <ip>:<connect-port>`

### Verify
```bash
adb devices -l
```

### Known Device
- Model: Xiaomi 24117RN76E (tanzanite / Redmi Note 13 Pro 5G)

## Build & Install

### Build
```bash
./gradlew :androidApp:assembleDebug
```

### Install
```bash
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### Launch
```bash
adb shell am start -n page.dera.opencode/.MainActivity
```

### Full pipeline
```bash
./gradlew :androidApp:assembleDebug && adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk && adb shell am start -n page.dera.opencode/.MainActivity
```

## Troubleshooting

### "SDK location not found"
Create `local.properties` with `sdk.dir=$HOME/android-sdk`

### "License not accepted"
Run `yes | $SDKMANAGER --sdk_root=$ANDROID_HOME --licenses`

### "No route to host" on ADB connect
- Verify device IP (can change after reconnect)
- Ensure same WiFi network
- Use Wireless Debugging pairing flow (not raw connect to port 5555)

### "Cannot parse project property android.useAndroidX"
Check for missing newline in gradle.properties

## Changelog
- 2026-07-21: Initial setup — SDK 36, build-tools 36.0.0, cmdline-tools 13.0, WiFi ADB pairing flow, Xiaomi tanzanite connected
