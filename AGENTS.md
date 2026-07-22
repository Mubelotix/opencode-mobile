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

### Mandatory source-first rule
When implementing ANYTHING related to the opencode server — API endpoints, environment variables, CLI flags, data paths, wire formats, message schemas, authentication, headers — you MUST grep `/tmp/opencode` source before writing code. Never guess. This includes:
- Environment variable names (e.g., `OPENCODE_CONFIG_DIR` not `OPENCODE_HOME`)
- API endpoint paths and response shapes
- Request/response field names and types
- Database/table names and schemas
- Flag names (`--flag` not `--flag-name`)

Failure: revert, grep source, redo.

### API version
Use V2 protocol API exclusively (`/api/session/*` endpoints). Never use legacy `/session/*` endpoints. The V2 API uses `SessionMessage` format with `type` discriminator field. Legacy V1 format (`{ info, parts }`) is not supported.

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

## Compiler Error Diagnosis Protocol
If the Kotlin compiler throws an `Unresolved reference` error:
1. You are forbidden from modifying `expect/actual` interfaces or creating new platform-specific files to bypass the error.
2. Check the import statements at the top of the file. Verify whether a standard library symbol has moved packages (e.g., between `kotlin.*`, `kotlin.time.*`, and `kotlinx.*`).
3. If unsure of a package path, run `./gradlew dependencies` or inspect the library catalog before guessing API paths.
