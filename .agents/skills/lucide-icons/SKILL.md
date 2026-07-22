---
name: lucide-icons
description: Search and retrieve Lucide icons. Use composables.com for pre-built Android VectorDrawable XMLs (KMP compatible via painterResource). Raw SVGs crash on Android.
---

# Lucide Icons Skill

Lucide icon set — 24x24, stroke-based, ISC license. 1600+ icons.

## For this KMP project: use composables.com (pre-built XML VectorDrawables)

**Raw SVGs DO NOT work.** Compose Multiplatform on Android throws `IllegalStateException: Android platform doesn't support SVG format`. The compose-resources plugin cannot convert SVGs with `stroke="currentColor"` etc.

### Correct workflow

1. Browse/search icons: https://composables.com/icons/icon-libraries/lucide
2. Download as XML VectorDrawable:
   ```bash
   curl -sL "https://composables.com/download/icons/lucide/<icon-name>.xml?download=vector-drawable" \
     -o shared/src/commonMain/composeResources/drawable/<icon-name>.xml
   ```
   If the icon name is not found, consult the icon index at Composables (hyphens in names from the site match the XML URL path). Search the icon index first: `https://composables.com/icons/icon-libraries/lucide`.
3. Fix color reference: replace `@color/black` → `#FF000000` (this project has no color resources). **Do this first before any other XML edits** — otherwise `sed` transformations may miss nested references.
   ```bash
   sed -i 's/@color\/black/#FF000000/g' shared/src/commonMain/composeResources/drawable/<icon-name>.xml
   ```
4. For **filled/outline variants** (e.g., star outline vs star filled): download two separate XMLs from composables.com. Do NOT try to derive one from the other via sed — fragile, often leaves `@color/black` broken.
   - Outline: star.xml (stroke only, default)
   - Filled: create star_filled.xml by copying and changing `android:strokeColor` → `android:fillColor`, removing `android:strokeWidth`/`strokeLineCap`/`strokeLineJoin`, and removing `android:fillColor="#00000000"`.
   - Fix `@color/black` on BOTH files independently.
5. **Kotlin compiler workaround (REQUIRED for new icons):** The compose-resources plugin generates `internal` extension properties on `Res.drawable` (anonymous object type). Kotlin compiler cannot resolve new `internal` extensions on anonymous types from other packages. Icons that shipped with the initial build work, but any NEW icon fails with "Unresolved reference". Fix:
   - **IMPORTANT**: After downloading a new XML, ALWAYS add a public accessor in `shared/src/commonMain/kotlin/opencodeapp/shared/generated/resources/PublicIcons.kt`:
     ```kotlin
     package opencodeapp.shared.generated.resources

     import org.jetbrains.compose.resources.DrawableResource

     val searchIcon: DrawableResource get() = Res.drawable.search
     ```
   - Use the public accessor in code instead of `Res.drawable.<name>`:
     ```kotlin
     import opencodeapp.shared.generated.resources.searchIcon
     Image(painter = painterResource(searchIcon), ...)
     ```
6. Use in Compose:
   ```kotlin
   import opencodeapp.shared.generated.resources.Res
   import opencodeapp.shared.generated.resources.<icon_name>
   import org.jetbrains.compose.resources.painterResource

   Image(painter = painterResource(Res.drawable.<icon_name>), ...)
   ```
   Hyphens in names become underscores in code: `chevron-right` → `chevron_right`

## Raw SVG reference (for web/other contexts, NOT Compose KMP)

### Metadata (tags + categories per icon)
```
https://raw.githubusercontent.com/lucide-icons/lucide/main/icons/<icon-name>.json
```

### Raw SVGs
```
https://raw.githubusercontent.com/lucide-icons/lucide/main/icons/<icon-name>.svg
```

### Directory listing
```
https://api.github.com/repos/lucide-icons/lucide/contents/icons
```

## Common icon names
- Navigation: `arrow-left`, `arrow-right`, `chevron-left`, `chevron-right`, `home`, `menu`
- Files: `file`, `folder`
- Actions: `plus`, `edit-3`, `trash-2`, `search`, `check`, `x`
- UI: `star`, `heart`, `user`, `settings`, `filter`
- Development: `code-2`, `terminal`, `git-branch`, `github`
