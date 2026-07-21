---
name: feather-icons
description: Search and retrieve Feather open-source SVG icons by name or keyword. 24x24 grid, MIT licensed, 280+ icons with searchable tags.
---

# Feather Icons Skill

Feather icon set from https://github.com/feathericons/feather. 24x24 SVG, stroke-based, MIT license. ~280 icons. Full list: https://feathericons.com

## Icon Data Sources

### Tags (search keywords)
```
https://raw.githubusercontent.com/feathericons/feather/main/src/tags.json
```
JSON object: `{ "icon-name": ["tag1", "tag2", ...], ... }`

### Individual SVG files
```
https://raw.githubusercontent.com/feathericons/feather/main/icons/<icon-name>.svg
```
Example: `https://raw.githubusercontent.com/feathericons/feather/main/icons/search.svg`

## Search Process

### 1. Fetch tags.json for discovery
```bash
curl -sL https://raw.githubusercontent.com/feathericons/feather/main/src/tags.json
```
This returns all icon names and their search keywords. Parse to find matches.

### 2. Search strategies

**By icon name (exact or substring):**
Search the keys of tags.json. Names are kebab-case: `arrow-right`, `alert-circle`, `user-plus`.

**By keyword/tag:**
Search the values (arrays of strings) in tags.json. Example: looking for "chat" icons → match `message-circle`, `message-square` whose tags include `"chat"`.

**Common tag mappings:**
- Navigation: `arrow-left`, `arrow-right`, `chevron-left`, `chevron-right`, `home`, `menu`
- Actions: `plus`, `edit`, `trash`, `copy`, `download`, `upload`, `search`, `settings`
- Communication: `mail`, `phone`, `message-circle`, `message-square`, `send`, `bell`
- Files/data: `file`, `folder`, `image`, `archive`, `database`, `hard-drive`
- UI elements: `check`, `x`, `menu`, `grid`, `list`, `filter`, `sliders`
- Users/social: `user`, `users`, `user-plus`, `user-check`, `heart`, `star`, `share`

### 3. Retrieve SVG

Fetch raw SVG from GitHub:
```bash
curl -sL https://raw.githubusercontent.com/feathericons/feather/main/icons/<icon-name>.svg
```

## Full Search Example

Task: "Find a Feather icon for notifications"

```bash
# Fetch all tags
curl -sL https://raw.githubusercontent.com/feathericons/feather/main/src/tags.json | \
  python3 -c "
import json, sys
tags = json.load(sys.stdin)
for name, keywords in tags.items():
    if any(k in ' '.join(keywords).lower() for k in ['bell', 'notif', 'alert']):
        print(f'{name}: {keywords}')
"
```

Output:
```
bell: ["alarm", "notification", "sound"]
bell-off: ["alarm", "notification", "silent"]
alert-circle: ["warning", "alert", "danger"]
alert-triangle: ["warning", "alert", "danger"]
```

Then retrieve the SVG:
```bash
curl -sL https://raw.githubusercontent.com/feathericons/feather/main/icons/bell.svg
```
