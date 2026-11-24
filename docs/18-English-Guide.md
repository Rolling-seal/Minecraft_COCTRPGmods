# Call of Cthulhu Mod — English Guide (v2.3.1)

> Minecraft 1.20.1 · Forge 47.4.29 · one jar supports both 简体中文 and English.
> Language is selected by the **Minecraft client language**, not by a separate jar.

---

## 1. Language switching

1. Minecraft **Options → Language**.
2. Choose **English (US)** or **简体中文**.
3. The mod UI, buttons, story book, curtains, item rules, HUD and system chat switch automatically.

- Chinese is the original text; English uses the built-in translation `assets/coc/lang/coc_en.json`.
- Imported scenarios are displayed **as authored**. If you import an English JSON, it stays English; if you import Chinese text, it remains Chinese.
- Some dynamic text (player names, custom item names, unusual custom server messages) may remain in its original language.

---

## 2. Installation

1. Put `coc-2.3.1.jar` into the instance `mods/` folder.
2. Client and server must use the **same latest jar** (network protocol 5).
3. No other dependencies.

---

## 3. Keys

| Key | Function |
|---|---|
| `C` | Character sheet |
| `J` | Clue journal (also has **Import Scenario**) |
| `K` | Keeper panel (Keeper only) |
| Pause menu | **Call of Cthulhu Settings** (low-SAN effects) |

Players no longer have a dice key. The Keeper rolls for them.

---

## 4. Keeper panel (5 pages)

| Page | Use |
|---|---|
| Player | Select player; view sheet; status/luck adjustment; restore sanity / force madness; improvement; character-sheet JSON; delete sheet. |
| Roll | Select player + skill + difficulty + bonus/penalty; **Roll for Selected Player**; public dice. |
| Items | View selected player's inventory; grant items; item library/rules; public item-use check. |
| Story Book | Pick scenario; **Main Story / Side Story**; start/stop; import scenario. |
| Tools | Grant clue; broadcast; reload. |

### Public rolls
All Keeper skill rolls, arbitrary dice, item-use checks and SAN checks are sent to **every online player** as chat + an on-screen result overlay.

Players cannot initiate rolls themselves.

### Story flow
- Each act sends players a curtain with **What Happened** and up to **three read-only possible actions**; no option buttons.
- The Keeper reads the Story Book and adjudicates; players cannot click the listed actions.
- Main Story shows per-act summary, trigger, description, possible paths, success/failure, clues.
- Side Story shows trigger condition, description, options and consequences.
- **Show Act to Players** sends the curtain with its read-only actions.
- Clue lines in the Story Book are clickable; choose an online player to send the clue to their journal.

### Item library and rules
- `K → Items → Grant Item / Rules`.
- Enter any item ID and **Configure Custom (add to list)** to add it to the Keeper item list.
- For each item set:
  - **Custom name** (optional; kept on granted items and shown in the list)
  - **Skill ID** (optional, e.g. `spot_hidden`, `listen`, `occult`)
  - **Target number** (1–100)
- Saved to `config/coc/items.json`.
- When the item is used, the server uses the configured skill/target; if no rule exists, the temporary target on the Items page is used.

### Status / Luck
`Player → Status Adjustment / Burn Luck`:
- Set SAN / HP / MP / Luck freely.
- `-5 / +5` for HP/MP/Luck.
- Full HP/MP.

### Backstory
The character sheet’s left panel now shows belief, important person, significant place, treasured possession, trait, injury and notes inline under the injury row. Scroll the left panel; no separate Background button is needed.

### Keeper items
- **Keeper’s Book** (`coc:kp_book`): 8-page in-game guide; right-click as Keeper. **Given automatically on join** (and on respawn if missing); no crafting.
- **Global Settings** (`coc:global_settings`): configure new-investigator occupation points, interest points, total skill-point cap, whether players may allocate points, and starting Luck / HP / SAN ranges. `-1` = automatic formula. Craft: comparator + gold ingot + redstone + book. In the Keeper’s Book, **click the Global Settings entry** to open the settings panel directly.
- **Scenario Book** (`coc:scenario_book`): automatically given/updated to every online Keeper when a scenario starts. Right-click to open the act-by-act Story Book for the current scenario; one book per Keeper, no crafting.
- All three are Keeper-only and appear in the Tools creative tab.

### Character-sheet JSON
- `K → Player → select player → Character Sheet JSON`.
- **Export** sends the sheet to a client save dialog; **Import** reads a `.json` file and overwrites the selected player's sheet.
- Format `coc-character-v1`; the target keeps their Minecraft name/UUID. See [character sheet JSON](21-角色卡JSON格式.md).
- Import clears the target's temporary event/check state; export a backup first.

---

## 5. Scenario import

Players: `J → Import Scenario`.
Keeper: `K → Story Book → Import Scenario`.

1. Prepare a `.json` file in the `coc-scenario-v1` format (see `19-Scenario-Import-Format-Spec-EN.md`; a validated sample is at `示例/example_scenario.json`).
2. Click **Select JSON File** and choose the file.
3. The server validates and writes it to `config/coc/scenarios` + `config/coc/events`, then reloads.
4. The new scenario appears in the Keeper's Story Book.

Limits: 64 KB, 24 acts, 8 options and 8 side events per act. Only text, skill IDs, difficulty, success/failure/outcome and clues; no effects, items, spawns or commands.

Options can carry clues:

```json
"success": "You find an old postmark.",
"successClue": "The postmark is from Saltmarsh, twenty years ago.",
"failure": "You only see blank paper.",
"failureClue": ""
```

Blank clue = **“No clue”** in the Story Book.

---

## 6. Commands

All players:
```text
/coc sheet [player]
/coc create
/coc journal
/coc settings
```

Keeper:
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

---

## 7. Built-in scenarios

- `the_haunting` — *The Haunting* structure, 11 acts, 5 side scenes (original Chinese/English adaptation, no official text copied).
- `lightless_beacon` — *The Lightless Beacon* structure, 11 acts, 5 side scenes.
- `dark_whispers` — *Whispers in the Dark*, 12 acts, 5 side scenes.

The English text is a fan translation for play; proper names and tone may be adjusted. Chinese remains the source text.

---

## 8. Known limits

- English translations are built into one jar; no separate English build.
- Custom imported scenario text is **not machine-translated**; author it in the language you want.
- Some dynamic chat lines with custom names may mix languages.
- `SELFTEST OK · events 75 · scenarios 3` is the current dedicated-server check.
- Windows uses a PowerShell/WinForms native file dialog that does not depend on Java AWT headless detection; other desktops fall back to AWT. Truly headless environments cannot show a picker.
- Global Settings and Keeper’s Book are Keeper-only; non-Keeper players get a refusal message.
- Character-sheet import is Keeper-only and works only on online players.

---

## 9. Screenshots

- `截图/43-English-Keeper.png`
- `截图/44-English-Rolls.png`
- `截图/45-English-Story.png`
- `截图/46-English-Curtain.png`
- `截图/47-English-Sheet.png`
- `截图/48-English-Chat.png`
- `截图/49-English-Tools.png`

Chinese documentation remains in `05`, `15`, `16`, `17`, and `21`.
