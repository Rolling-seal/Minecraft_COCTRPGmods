# Changelog

## 2.2.0 — 2026-11-24

### Scenario Book
- New item **Scenario Book** (`coc:scenario_book`).
- When a scenario starts, every online Keeper receives/updates one Scenario Book bound to that scenario.
- Right-click opens the Story Book for that scenario: each act's title, summary, trigger, scene, possible paths and side scenes.
- One book per Keeper; starting another scenario updates it instead of adding clutter.
- No crafting recipe; opening without an active/bound scenario gives a clear message.
- Bilingual item name, tooltip, grant message and guide line.

### Compatibility
- Minecraft 1.20.1, Forge 47.4.29, Java 17.
- Network protocol 4; client and server must use the same jar.

## 2.1.1 — 2026-11-24

### Keeper’s Book is automatic
- The Keeper’s Book is now placed in the Keeper’s inventory automatically on join, and on respawn if missing.
- No crafting recipe; the old `keeper_book` recipe was removed.
- Existing Keepers are not given a duplicate if they already carry the book.
- Added a bilingual chat notice and server log when the book is granted.

## 2.1.0 — 2026-11-24

### New Keeper items
- **Keeper’s Book** (`coc:kp_book`): 8-page in-game guide covering session start, rolls, curtains, Story Book, items, sheets, import and global settings. Keeper-only, right-click to open.
- **Global Settings** (`coc:global_settings`): Keeper-only item to configure new-investigator occupation points, interest points, total point cap and whether players may allocate points. `-1` keeps the automatic formula.
- Recipes for both items; both appear in the Tools creative tab.

### Character-sheet JSON
- Keeper can export the selected player’s sheet to `coc-character-v1` JSON.
- Keeper can import a `.json` file and overwrite the selected player’s sheet.
- Server-side validation, clamping and derived-stat recalculation; handouts, backstory, skills and improvement marks are preserved.
- Native file picker on the client; no clipboard.

### Custom item names
- Per-item custom display name in the item library, persisted in `config/coc/items.json`.
- Granted items keep that name in the player’s inventory.
- Custom names show with `★` in the Keeper item list.

### Fixes / polish
- Global Settings toggle no longer resets numbers being edited.
- Blank occupation is allowed in imported sheets.
- New item tooltips and labels are bilingual.
- Network protocol 4; client and server must use the same jar.

### Compatibility
- Minecraft 1.20.1, Forge 47.4.29, Java 17.

## 2.0.0 — 2026-11-22

### Keeper-led redesign
- Only the Keeper can roll; players no longer have a dice key or roll commands.
- Public results for skill checks, arbitrary dice, item checks and SAN checks.
- Act curtains: each act sends only its title and description; no automatic event cards/options.
- Keeper Story Book with Main Story and Side Story, per-act direction, triggers, outcomes and clues.
- Per-option clue annotations, including explicit “no clue”.

### Items
- Keeper item library keyed by item ID; add custom items to the list.
- Per-item skill ID and target number.
- Item use removes one item and performs a public check.
- Persistent rules in `config/coc/items.json`.

### Scenarios
- Scenario import from a `.json` file using `coc-scenario-v1`.
- Server validation and standard config generation; no effects/commands executed.
- Built-in: The Haunting (11 acts), The Lightless Beacon (11 acts), Whispers in the Dark (12 acts).

### UI / quality of life
- Free HP/MP/SAN/Luck adjustment and “Burn Luck”.
- Backstory page, including in compact windows.
- Tool page layout fixes.
- Medieval Cthulhu theme retained; lists preserve scroll position.

### Languages
- English and Chinese in one jar, following the Minecraft client language.
- English UI, three built-in scenarios, HUD and system chat.
- English guide and scenario import specification included.

### Compatibility
- Minecraft 1.20.1, Forge 47.4.29, Java 17.
- Network protocol 3; client and server must use the same jar.

## 1.4.2
- Character deletion with confirmation; expanded scenario content; earlier event-card flow.

## 1.4.1
- Multi-scenario event labels.

## 1.4.0
- Added The Lightless Beacon structure adaptation.

## 1.3.0
- Added The Haunting structure adaptation.

## 1.2.0
- Expanded Whispers in the Dark to 12 acts; added event categories and act guide.

## 1.1.0
- Medieval Cthulhu theme; Keeper Panel item; scroll preservation.

## 1.0.0
- Initial release: character sheets, d100, SAN, events, scenarios, low-SAN effects.
