# AGENTS.md

## Commit conventions
- Capitalized verb, imperative mood, no scope prefix
- Examples: `Add feature X`, `Fix crash on login`, `Update dependencies`
- **NEVER EVER commit without the user explicitly asking you to. No exceptions.**

## Build
```bash
./gradlew build                    # full build
./gradlew :shared:build            # shared module only
./gradlew :androidApp:assembleDebug # android debug APK
```

## Deploy (Android)
```bash
# Full pipeline: build → install → launch
./gradlew :androidApp:assembleDebug && adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk && adb shell am start -n page.dera.opencode/.MainActivity

# Install only (APK already built)
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk

# Launch only (already installed)
adb shell am start -n page.dera.opencode/.MainActivity

# Read logs
adb logcat -s System.out | grep OC_DBG
```

## Dev server
```bash
# The Android device connects to 192.168.1.88:4096 (creds from shared prefs).
# You can hit the server directly with curl to test endpoints:
PASSWORD_B64=$(echo -n "opencode:mysecret" | base64) && curl -s -H "Authorization: Basic $PASSWORD_B64" "http://192.168.1.88:4096/api/session"
```

## ADB device setup
```bash
adb devices -l                    # verify connected
# WiFi pairing (Android 11+): Developer options → Wireless debugging → pair/connect
adb pair <ip>:<pairing-port> <code>
adb connect <ip>:<connect-port>
```

## OpenCode reference
Clone for context when implementing opencode client behavior:
```bash
git clone https://github.com/anomalyco/opencode /tmp/opencode
```

### Mandatory source-first rule
When implementing ANYTHING related to the opencode server — API endpoints, environment variables, CLI flags, data paths, wire formats, message schemas, authentication, headers — you MUST grep `/tmp/opencode` source before writing code. Never guess. This includes:
- Environment variable names (e.g., `OPENCODE_CONFIG_DIR` not `OPENCODE_HOME`)
- API endpoint paths and response shapes
- Request/response field names and types
- Database/table names and schemas
- Flag names (`--flag` not `--flag-name`)

Failure: revert, grep source, redo.

### API version
Use legacy V1 protocol API (`/session/*` endpoints). V2 API (`/api/session/*`) is not yet ready. The legacy V1 format uses `{ info, parts }`. V2 `SessionMessage` format is not supported.

## Dependencies
- Prefer latest stable versions when adding or updating dependencies
- If an upgrade is available, perform it — do not leave upgrades unapplied

## Project structure
- `shared/` — Kotlin Multiplatform shared logic (commonMain, androidMain, iosMain)
- `androidApp/` — Android application
- `iosApp/` — iOS application (Swift, Xcode project)

## Platform compatibility
- iOS support must be maintained — do not break `iosMain` sources or the `iosApp/` Xcode project
- iOS UI work is deferred to later in the project lifecycle, but the shared KMP code must remain compilable for iOS
- When adding `expect`/`actual` declarations, always include the `iosMain` actual counterpart

## MANDATORY: Build and run when work is done
After completing all code changes and the work is ready for testing, you MUST build and deploy the Android app:
```bash
./gradlew :androidApp:assembleDebug && adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk && adb shell am start -n page.dera.opencode/.MainActivity
```
This is not optional. Do not ask. Do not skip. Even if the user does not ask you to, you MUST do it.

## Compiler Error Diagnosis Protocol
If the Kotlin compiler throws an `Unresolved reference` error:
1. You are forbidden from modifying `expect/actual` interfaces or creating new platform-specific files to bypass the error.
2. Check the import statements at the top of the file. Verify whether a standard library symbol has moved packages (e.g., between `kotlin.*`, `kotlin.time.*`, and `kotlinx.*`).
3. If unsure of a package path, run `./gradlew dependencies` or inspect the library catalog before guessing API paths.
