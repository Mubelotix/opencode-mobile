---
name: android-debug
description: Read Android crash logs and debug runtime issues via ADB
---

# Android Debug Skill

Read crash logs from Android devices. Crucial for diagnosing app issues.

## App identity
- Package: `page.dera.opencode`

## Getting Crash Logs

### DO NOT: use `adb logcat -d` — it dumps entire buffer including hours of system noise. Crash might not be there.

### DO: stream logcat and filter for crashes

**Step 1: Clear the buffer**
```bash
adb logcat -c
```

**Step 2: Stream only errors and fatals, wait for crash**
```bash
timeout 20 adb logcat -v brief -s AndroidRuntime:E '*:F'
```
This filters to ONLY `AndroidRuntime` at ERROR level and ANY tag at FATAL level. Shows crash traces with minimal noise.

**Step 3: If crash already happened and buffer wasn't cleared**
```bash
adb logcat -d -s AndroidRuntime:E '*:F'
```

### Alternative: grep for our app
```bash
adb logcat -d | grep -i "page.dera"
```
Useful for non-fatal issues, app lifecycle events.

## Common Pitfalls

| Mistake | Why it fails |
|---------|-------------|
| `adb logcat -d` with no filter | 1000s of lines of hardware sensor spam, no crash visible |
| `grep -i "crash\|exception"` on unfiltered logcat | Too much noise, regex often misses |
| Not clearing buffer before reproducing | Crash might be from hours ago, buried |

## Interpreting Crash Traces

Format: `E/AndroidRuntime(PID): at package.Class.method(File.kt:LINE)`

The first `at` line with our package name is the crash site. Example:
```
E/AndroidRuntime(25420): at page.dera.opencode.ui.FileExplorerScreenKt.FileExplorerScreen$lambda(FILE.kt:156)
```
→ Crashed at line 156 of `FileExplorerScreen.kt`, NullPointerException.

## Reading Compose Snapshots for Leaks/Issues
```bash
adb logcat -d | grep -i "snapshot\|compose.*error\|compose.*crash"
```
Rarely needed but useful for `IllegalStateException: Reading a state that was created after the snapshot was taken`.

## App-Level Debug Logging

For non-crash issues (API failures, serialization errors, logic bugs), add `println()` with a unique prefix in KMP `commonMain`, then capture via ADB:

**Step 1: Add logging to code**
```kotlin
println("OC_DBG: something went wrong: ${e.message}")
```
`println` in KMP commonMain writes to stdout, which Android routes to logcat under tag `System.out` at INFO level.

**Step 2: Deploy, clear buffer, capture**
```bash
adb logcat -c  # clear
# ... reproduce the issue ...
timeout 20 adb logcat -s "System.out:I" 2>&1 | grep "OC_DBG"
```

**Step 3: One-liner for build+deploy+capture**
```bash
./gradlew :androidApp:assembleDebug | tail -3 && adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk && adb shell am start -n page.dera.opencode/.MainActivity && sleep 2 && adb logcat -c && echo "Reproduce now" && timeout 20 adb logcat -s "System.out:I" 2>&1 | grep "OC_DBG"
```

**Important:** Remove or quiet debug prints before merging. Use a unique prefix (e.g., `OC_DBG`) for easy filtering. The `-s` flag filters to only `System.out` tag at INFO level—no system noise.

## Changelog
- 2026-07-22: Created after discovering `adb logcat -d` with no filter hides crashes in hardware sensor noise
- 2026-07-22: Added `println` + `adb logcat -s "System.out:I"` technique for KMP app-level debug logging
