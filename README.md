# Call of Cthulhu for Minecraft

![Call of Cthulhu icon](icon.png)

A Call of Cthulhu 7e tabletop layer for **Minecraft 1.20.1 / Forge 47.x**.

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-3C8527)
![Forge](https://img.shields.io/badge/Forge-47.4.29-DA6E00)
![Version](https://img.shields.io/badge/Version-2.0.0-C9A227)
![License](https://img.shields.io/badge/License-MIT-blue)

> Fan-made project. Not affiliated with or endorsed by Chaosium. The built-in scenarios are original adaptations inspired by the structure of *The Haunting* and *The Lightless Beacon*; no official scenario text is copied.

---

## What is this?

Run a Keeper-led Call of Cthulhu session inside Minecraft:

- investigator sheets with characteristics, 55 skills, occupations, SAN/HP/MP/Luck and backstory;
- Keeper-only public d100 rolls;
- act curtains instead of automatic event cards;
- a Keeper Story Book with main and side plots;
- player-authored scenario import;
- an item library with per-item skill checks;
- low-SAN visual effects;
- Chinese and English in one jar.

---

## Features

### Investigator sheets
- Nine characteristics, derived HP/MP/SAN/MOV/DB/Build.
- 55 core skills, 15 occupations, occupation/interest points.
- SAN, temporary/indefinite insanity, phobias, manias, improvement rolls.
- Editable backstory: belief, important person, significant place, treasured possession, trait, injury.

### Keeper-led rolls
- Only the Keeper can roll.
- The Keeper selects a player, skill, difficulty and bonus/penalty dice.
- Every result is sent to **all online players** as chat and an on-screen overlay.
- Arbitrary dice expressions are supported.
- Players cannot open a personal dice screen or use old roll commands.

### Act curtains
- Starting a scenario and advancing an act sends players only a curtain:
  - scenario name;
  - act number;
  - act title;
  - description;
  - **Continue**.
- No options, no automatic checks, no automatic rewards. The Keeper narrates.

### Keeper Story Book
- **Main Story**: per-act summary, trigger, description, possible paths, checks, success/failure and clues.
- **Side Story**: trigger condition, description, options, consequences and clues.
- “Show Act to Players” sends only the curtain.
- Every option marks its success/failure/direct clue, or **No clue**.

### Item library and item checks
- Grant items by common list or custom item ID.
- Add an item ID to the Keeper item list.
- Configure per item:
  - skill ID (optional);
  - target number (1–100).
- Using an item removes one from the player’s inventory and makes a public check using the configured rule.
- Rules persist in `config/coc/items.json`.

### Scenario import
- Players and Keepers can import a single `coc-scenario-v1` JSON file.
- The server validates it and writes standard scenario/event data into `config/coc/`.
- Imported scenarios appear in the Story Book.
- Limits: 64 KB, 24 acts, 8 options and 8 side scenes per act.
- Only text, skills, difficulty, outcomes and clues; no effects, items, spawns or commands.
- Format: [English specification](docs/19-Scenario-Import-Format-Spec-EN.md) · [example](docs/示例/example_scenario.json).

### Languages
- One jar, no separate English build.
- Follows the Minecraft client language:
  - `en_us` / `en_gb` → English;
  - `zh_cn` → Chinese.
- English guide: [docs/18-English-Guide.md](docs/18-English-Guide.md).
- Custom imported text is displayed as authored and is not machine-translated.

### Low-SAN effects
- Desaturation, chromatic distortion, vignette and optional wobble.
- Subtle camera movement, whispers, heartbeat, SAN/HP/MP/Luck HUD.
- Can be disabled in **Call of Cthulhu Settings**.

---

## Requirements

| | |
|---|---|
| Minecraft | 1.20.1 |
| Loader | Forge 47.4.29 (47.x) |
| Java | 17 |
| Side | Client **and** server |
| Dependencies | None |

The client and server must use the **same jar** (network protocol 3).

---

## Installation

1. Install Minecraft Forge 47.4.29 for 1.20.1.
2. Put [`releases/coc-2.0.0.jar`](releases/coc-2.0.0.jar) into `mods/`.
3. Install the same jar on the server if playing multiplayer.
4. Launch the game.

No other mod is required. Voice chat is optional and unrelated.

---

## Quick start

### Keeper
1. Press `K` to open the Keeper Panel.
2. Open **Story Book**, choose a scenario and press **Start Scenario**.
3. Players receive the opening curtain.
4. Describe the scene and ask what they do.
5. When a check is needed, open **Roll**, select the player and skill, and press **Roll for the Selected Player**.
6. When the group moves to a new act, select it in the Story Book and press **Show Act to Players**.
7. For side scenes, read them from **Side Story** and narrate; they are not sent automatically.

### Players
1. Press `C` to create/view a character.
2. Press `J` for clues and **Import Scenario**.
3. In each act, read the curtain and describe your actions.
4. Ask the Keeper for checks; do not roll yourself.
5. Use the **Background** button on the character sheet to review your backstory.

---

## Default keys

| Key | Action |
|---|---|
| `C` | Character sheet |
| `J` | Clue journal / scenario import |
| `K` | Keeper Panel (Keeper only) |

The pause menu has **Call of Cthulhu Settings** for visual effects.

---

## Built-in scenarios

| ID | Title | Acts | Side scenes |
|---|---|---|---|
| `the_haunting` | The Haunting (original adaptation) | 11 | 5 |
| `lightless_beacon` | The Lightless Beacon (original adaptation) | 11 | 5 |
| `dark_whispers` | Whispers in the Dark (original) | 12 | 5 |

The first two use only the published *structure* of the official beginner scenarios. All prose, NPCs, clues and outcomes in this mod are original.

---

## Keeper commands

```text
/coc sancheck <successLoss> <failLoss> [player]
/coc san set|add|sub <value> [player]
/coc hp set|add|sub <value> [player]
/coc mp set|add <value> [player]
/coc luckstat <value> [player]
/coc mythos add <value> [player]
/coc skill <skill> <value> [player]
/coc growth [player]
/coc givepanel [player]
/coc delete <player>
/coc delete <player> confirm
/coc scenario list
/coc scenario start <id>
/coc scenario next
/coc scenario act <id> <act>
/coc scenario stop
/coc handout <text>
/coc handoutto <player> <text>
/coc broadcast <text>
/coc reload
/coc debug selftest
```

Players:
```text
/coc sheet [player]
/coc create
/coc journal
/coc settings
```

---

## Build from source

The release jar is already in [`releases/`](releases/).

The source was built offline with `javac` + ForgeAutoRenamingTool. The helper script is included:

```bash
export COC_GRADLE_CACHE="$HOME/.gradle/caches/forge_gradle"
export COC_GAME_LIBS="/path/to/.minecraft/libraries"
python build_manual.py --no-deploy
```

On Windows, set the same values as environment variables. The script expects:

- a ForgeGradle cache containing the official-mapped Forge jar and SRG mappings;
- `ForgeAutoRenamingTool-1.0.2-all.jar`;
- Minecraft client/server libraries for the classpath.

Output: `build/coc-2.0.0.jar`.

A prebuilt jar is recommended for normal users.

---

## Project layout

```text
src/main/java/com/coc/       mod source
src/main/resources/          assets, lang, recipes, shaders, icon.png
tools/                       content generators and icon generator
docs/                        Chinese design/play documentation; English guide and format spec
docs/截图/                   screenshots
docs/示例/                   import example
releases/                    built jar
```

---

## Documentation

- [English guide](docs/18-English-Guide.md)
- [Scenario import specification (English)](docs/19-Scenario-Import-Format-Spec-EN.md)
- [Modrinth release checklist](docs/20-Modrinth-发布清单.md)
- [Chinese usage guide](docs/05-使用说明.md)
- [Keeper seven features and Story Book](docs/15-KP七功能与幕布剧情书.md)
- [Item library and rules](docs/17-KP道具库与规则.md)

---

## License

MIT. See [LICENSE](LICENSE).

## Credits

- Project owner: **Rolling-seal**
- Built with the ZCode development environment.
- Call of Cthulhu is a trademark of Chaosium Inc. This is an unaffiliated fan project.
