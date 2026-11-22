# Changelog

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
