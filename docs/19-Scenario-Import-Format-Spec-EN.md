# Call of Cthulhu Mod — Scenario Import JSON Specification

> Format ID: `coc-scenario-v1`
> Applies to: `coc-2.0.0.jar` (Minecraft 1.20.1 / Forge)
> This is **content import data**, not a Modrinth mod manifest. See the release checklist separately.

---

## 1. What this file is

A player or Keeper can import a single `.json` file into the mod. The server validates it, converts it into standard scenario/event data, writes it under `config/coc/`, and reloads the Story Book.

- Maximum file size: **64 KB**
- Maximum acts: **24**
- Maximum options per act/side scene: **8**
- Maximum side scenes per act: **8**
- The imported scenario is a **Story Book reference**. It does **not** execute effects, grant items, spawn entities, or run commands.

---

## 2. Top-level schema

```json
{
  "format": "coc-scenario-v1",
  "id": "my_story",
  "title": "My Scenario",
  "description": "A one-line summary.",
  "intro": "Opening curtain text.",
  "outro": "Closing curtain text.",
  "sanReward": "1d10",
  "acts": []
}
```

| Field | Required | Type | Notes |
|---|---|---|---|
| `format` | Recommended | string | Use `coc-scenario-v1`. |
| `id` | Yes | string | `^[a-z][a-z0-9_]{2,31}$`; unique; cannot replace built-ins. |
| `title` | Yes | string | ≤ 80 chars. |
| `description` | No | string | ≤ 1000 chars. |
| `intro` | No | string | ≤ 4000 chars; shown as the opening curtain. |
| `outro` | No | string | ≤ 4000 chars; shown as the closing curtain. |
| `sanReward` | No | string | Dice expression, default `1d10`; `0` allowed. |
| `acts` | Yes | array | 1–24 acts. |

---

## 3. Act object

```json
{
  "title": "Rainy Night",
  "summary": "Keeper reference: the investigators reach the village.",
  "trigger": "When the investigators enter the village",
  "description": "You reach the village gate in the rain.",
  "options": [],
  "sideEvents": []
}
```

| Field | Required | Notes |
|---|---|---|
| `title` | Yes | ≤ 80 chars. Do not prefix “Act 1”; the UI adds numbering. |
| `summary` | No | ≤ 500 chars; Keeper-facing direction. |
| `trigger` | No | ≤ 500 chars; when to show this act. |
| `description` | Yes | ≤ 4000 chars; **the curtain text players see**. Do not put option buttons in it. |
| `options` | No | Max 8; Keeper reference only. |
| `sideEvents` | No | Max 8; Keeper reference only. |

---

## 4. Option object

```json
{
  "text": "Inspect the church",
  "skill": "spot_hidden",
  "difficulty": "regular",
  "success": "You find wet footprints.",
  "successClue": "The postmark is from Saltmarsh, twenty years ago.",
  "failure": "You only smell damp wood.",
  "failureClue": ""
}
```

| Field | Required | Notes |
|---|---|---|
| `text` | Yes | ≤ 300 chars. |
| `skill` | No | Skill ID or Chinese skill name; e.g. `spot_hidden`, `listen`, `library_use`, `occult`, `psychology`. Omit for no check. |
| `difficulty` | No | `regular` / `hard` / `extreme`, or `常规` / `困难` / `极难`; default `regular`. |
| `success` | No | Success text, ≤ 2000 chars. |
| `failure` | No | Failure text, ≤ 2000 chars. |
| `outcome` | No | Direct/no-check result text, ≤ 2000 chars. |
| `clue` | No | Clue for success (or direct result when no skill). Blank = no clue. |
| `successClue` | No | Alias for `clue`; takes priority. |
| `failureClue` | No | Clue on failure. Blank = no failure clue. |

Clue display in the Story Book:

- `success` / `successClue` → `Success Clue: …`
- `failure` / `failureClue` → `Failure Clue: …`
- `outcome` / `clue` → `Clue: …`
- No clue → `No clue`.

Do not write “Clue:” inside the clue text; the UI adds the label.

---

## 5. Side scene object

```json
{
  "title": "Side Scene: Water in the Bell Tower",
  "trigger": "When an investigator approaches the bell tower",
  "description": "The water stops. You hear someone counting upstairs.",
  "options": []
}
```

| Field | Required | Notes |
|---|---|---|
| `title` | Yes | ≤ 80 chars. |
| `trigger` | No | ≤ 500 chars; how to trigger it. |
| `description` | Yes | ≤ 4000 chars. |
| `options` | No | Max 8, same schema as above. |

Side scenes are **Keeper narration aids**. They are not automatically sent to players.

---

## 6. Complete minimal example

See the validated file:

`开发文档/示例/example_scenario.json`

It contains two acts, one side scene, success/failure clues and a direct outcome.

---

## 7. Import flow

1. Save the JSON as a `.json` file (UTF-8).
2. In game:
   - Player: `J → Import Scenario`
   - Keeper: `K → Story Book → Import Scenario`
3. Click **Select JSON File**, choose the file.
4. The server validates, writes:
   - `config/coc/scenarios/<id>.json`
   - `config/coc/events/import_<id>_aN.json`
   - `config/coc/events/import_<id>_aN_sM.json`
5. The Story Book is reloaded; online Keepers receive refreshed data.

If the `id` already exists, the import is rejected. Change the `id` and retry.

---

## 8. Rules and safety

- Only text, skill IDs, difficulty, success/failure/outcome and clues are read.
- Unknown fields are ignored.
- No `effects`, item grants, entity spawns, commands or scripts are executed.
- IDs are restricted to lowercase letters, digits and underscores; no path traversal.
- The import is server-side world content and persists across restarts.
- Custom imported text is **not automatically translated**. Author it in the language you want players to see.

---

## 9. Common errors

| Error | Fix |
|---|---|
| `id must be ...` | Use lowercase letters/digits/underscore, start with a letter, 3–32 chars. |
| `scenario id already exists` | Choose another `id`. |
| `acts cannot be empty` | Add at least one act. |
| `act N is missing description` | Add the player-facing curtain text. |
| `skill does not exist` | Use a valid skill ID or leave `skill` empty. |
| `sanReward is not a valid dice expression` | Use `1d10`, `1d6`, `0`, etc. |
| `JSON exceeds 64KB` | Shorten descriptions or split the scenario. |

---

## 10. Modrinth note

For a **mod** release, Modrinth does **not** require a JSON manifest. Upload the jar and fill in the project metadata on the website (Forge, Minecraft 1.20.1, client+server, license, description).

A `modrinth.index.json` is only for **modpacks**, not for this mod itself. If you publish a modpack containing this mod, that file has a different format and requires uploaded file hashes/download URLs.
