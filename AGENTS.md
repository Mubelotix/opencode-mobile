# AGENTS.md

## Commit conventions
- Capitalized verb, imperative mood, no scope prefix
- Examples: `Add feature X`, `Fix crash on login`, `Update dependencies`

## Build
```bash
./gradlew build                    # full build
./gradlew :shared:build            # shared module only
./gradlew :androidApp:assembleDebug # android debug APK
```

## OpenCode reference
Clone for context when implementing opencode client behavior:
```bash
git clone https://github.com/anomalyco/opencode /tmp/opencode
```
When unsure about opencode server behavior, read the cloned source in `/tmp/opencode` instead of guessing.

## Dependencies
- Prefer latest stable versions when adding or updating dependencies
- If an upgrade is available, perform it — do not leave upgrades unapplied

## Project structure
- `shared/` — Kotlin Multiplatform shared logic (commonMain, androidMain, iosMain)
- `androidApp/` — Android application
- `iosApp/` — iOS application (Swift, Xcode project)
