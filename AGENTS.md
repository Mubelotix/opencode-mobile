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

## Project structure
- `shared/` — Kotlin Multiplatform shared logic (commonMain, androidMain, iosMain)
- `androidApp/` — Android application
- `iosApp/` — iOS application (Swift, Xcode project)
