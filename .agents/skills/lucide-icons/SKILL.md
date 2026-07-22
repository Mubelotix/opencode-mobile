---
name: lucide-icons
description: Search and retrieve Lucide open-source SVG icons by name or keyword. Fork of Feather, 24x24 grid, ISC license, 1600+ icons with searchable tags and categories.
---

# Lucide Icons Skill

Lucide icon set from https://github.com/lucide-icons/lucide — community fork of Feather. 24x24 SVG, stroke-based, ISC license. 1600+ icons. Full list: https://lucide.dev/icons

## Icon Data Sources

### Individual metadata (JSON — tags + categories per icon)
```
https://raw.githubusercontent.com/lucide-icons/lucide/main/icons/<icon-name>.json
```
Each `.json` contains:
- `tags`: search keywords (array of strings)
- `categories`: grouping labels (array of strings)
- `contributors`: authors (array of strings)

Example: `https://raw.githubusercontent.com/lucide-icons/lucide/main/icons/accessibility.json`

### Individual SVG files
```
https://raw.githubusercontent.com/lucide-icons/lucide/main/icons/<icon-name>.svg
```
Example: `https://raw.githubusercontent.com/lucide-icons/lucide/main/icons/search.svg`

### Directory listing (GitHub API)
```
https://api.github.com/repos/lucide-icons/lucide/contents/icons
```
Returns all icon files. Filter for `.svg` entries to get icon names. Paginated — use `?page=N` for large listings.

## Search Process

### 1. By icon name (exact or prefix)

**List icons matching a prefix via GitHub API:**
```bash
curl -sL "https://api.github.com/repos/lucide-icons/lucide/contents/icons" | \
  python3 -c "
import json, sys
data = json.load(sys.stdin)
for f in data:
    if f['name'].endswith('.svg') and f['name'].startswith('bell'):
        print(f['name'].replace('.svg',''))
"
```

Names are kebab-case: `arrow-right`, `alarm-clock-check`, `user-round-plus`.

**Fallback: fetch the full listing and filter:**
```bash
# Get all icon names (non-paginated for first 1000)
curl -sL "https://api.github.com/repos/lucide-icons/lucide/contents/icons" | \
  python3 -c "
import json, sys
for f in json.load(sys.stdin):
    if f['name'].endswith('.svg'):
        print(f['name'].replace('.svg',''))
" | grep <search-term>
```

### 2. By keyword/tag

Fetch the `.json` for candidate icons matched by name, then check tags:

```bash
curl -sL https://raw.githubusercontent.com/lucide-icons/lucide/main/icons/<icon-name>.json
```

**Common tag groupings:**
- Navigation: `arrow-left`, `arrow-right`, `chevron-left`, `chevron-right`, `home`, `menu`, `navigation`
- Actions: `plus`, `edit-3`, `trash-2`, `copy`, `download`, `upload`, `search`, `settings`, `check`, `x`
- Communication: `mail`, `phone`, `message-circle`, `message-square`, `send`, `bell`, `at-sign`
- Files/data: `file`, `folder`, `image`, `archive`, `database`, `hard-drive`, `save`
- UI elements: `check`, `x`, `menu`, `grid-3x3`, `list`, `filter`, `sliders-horizontal`
- Users/social: `user`, `users`, `user-plus`, `user-check`, `heart`, `star`, `share-2`
- Media: `play`, `pause`, `volume-2`, `music`, `video`, `camera`
- Weather/time: `sun`, `moon`, `cloud`, `clock`, `calendar`
- Development: `code-2`, `terminal`, `git-branch`, `github`, `package`
- Commerce: `shopping-cart`, `credit-card`, `dollar-sign`, `receipt`, `banknote`
- Health: `heart-pulse`, `pill`, `stethoscope`, `thermometer`, `activity`
- Security: `shield`, `lock`, `key`, `fingerprint`, `scan`

When searching by keyword at scale, prefer using the official website's search (https://lucide.dev/icons/) which provides full-text search across all tags and categories, then fetch the SVG for confirmed icons.

### 3. Retrieve SVG

```bash
curl -sL https://raw.githubusercontent.com/lucide-icons/lucide/main/icons/<icon-name>.svg
```

SVG is standard 24x24 viewBox, `fill="none"`, `stroke="currentColor"`, `stroke-width="2"`. Ready to drop into HTML/JSX.

## Full Search Example

Task: "Find a Lucide icon for notifications"

```bash
# 1. List icons matching prefix 'bell'
curl -sL "https://api.github.com/repos/lucide-icons/lucide/contents/icons?ref=main" | \
  python3 -c "
import json, sys
data = json.load(sys.stdin)
for f in data:
    name = f['name']
    if name.endswith('.svg') and 'bell' in name.lower():
        print(name.replace('.svg',''))
"
```

Output (example):
```
bell
bell-dot
bell-electric
bell-minus
bell-off
bell-plus
bell-ring
```

```bash
# 2. Check tags of top candidates
for icon in bell bell-dot bell-ring; do
  echo "=== $icon ==="
  curl -sL "https://raw.githubusercontent.com/lucide-icons/lucide/main/icons/$icon.json" | \
    python3 -c "import json,sys; d=json.load(sys.stdin); print('tags:', d['tags']); print('categories:', d['categories'])"
done
```

```bash
# 3. Fetch chosen SVG
curl -sL https://raw.githubusercontent.com/lucide-icons/lucide/main/icons/bell.svg
```
