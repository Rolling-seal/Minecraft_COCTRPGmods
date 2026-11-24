# Modrinth 发布清单（Call of Cthulhu Mod）

> 结论先说：**发布“模组（Mod）”不需要 JSON 清单**。Modrinth 网页上传 jar 后填写项目信息即可。
> `modrinth.index.json` 只用于“整合包（Modpack）”，不是这个模组本身的上传格式。

---

## 一、项目信息

| 字段 | 建议值 |
|---|---|
| Project type | Mod |
| Name | Call of Cthulhu |
| Summary | A Call of Cthulhu 7e tabletop RPG layer for Minecraft: investigator sheets, Keeper-only public d100 rolls, act curtains, Story Book, scenario import, item rules and character-sheet JSON. |
| Loader | Forge |
| Game version | 1.20.1 |
| Environment | Client and server (both required) |
| License | MIT |
| Source | 你的 GitHub / 仓库地址 |
| Icon | `发布素材/coc-icon-512.png`（512×512，用户提供 RealIco512） |
| Issues | 你的 issues 地址 |

依赖：无（只需要 Forge 47.x）。

---

## 二、版本文件

上传：

```text
coc-2.1.0.jar
```

当前文件信息（构建后填入）：

- Size: `657,520` bytes
- SHA1: `380245c39e49f71224d4df623a44e70b4e7f476f`
- SHA512: `3e22d8ed2f8cc6d3f342cdda2b267546cf64165ad6f9848d5872c27862445fbd44ed4b829d0cc2fedc9059a511423e2878e448a8671ba8008d481cd382f3f49c`
- Version: `2.1.0`
- Protocol: `4`
- Client/server must use the same jar.

可选附加文件：

- `示例/example_scenario.json`：可直接导入的示例剧本。
- `19-Scenario-Import-Format-Spec-EN.md`：英文剧本导入格式说明。
- `18-English-Guide.md`：英文使用说明。
- `21-角色卡JSON格式.md`：角色卡 JSON 格式与示例。

---

## 三、版本 Changelog 建议

```text
2.1.0
- New Keeper items: Keeper's Book (8-page in-game guide) and Global Settings (new-character point rules).
- Character-sheet JSON export/import for the Keeper (coc-character-v1).
- Per-item custom display names; granted items keep the name and appear with ★ in the item list.
- Global Settings toggle no longer resets edited values.
- Bilingual tooltips for the new items.
- Network protocol 4.
- Built-in scenarios: The Haunting (11 acts), The Lightless Beacon (11 acts), Whispers in the Dark (12 acts).
```

---

## 四、项目描述可复制内容（英文）

```text
Call of Cthulhu for Minecraft 1.20.1 (Forge)

Run a Keeper-led Call of Cthulhu 7e tabletop session inside Minecraft.

• Investigator sheets: characteristics, 55 skills, occupations, SAN/HP/MP/Luck, backstory.
• Keeper-only public rolls: the Keeper rolls for players; every result is shown to all online players.
• Act curtains: each act sends players only a title and description, with no option buttons. The Keeper narrates the choices.
• Keeper Story Book: main and side plots, per-act direction, triggers, success/failure, clues.
• Scenario import: import a coc-scenario-v1 JSON file; it appears in the Story Book.
• Item library: add items by ID and configure the skill and target number used for item checks.
• Low-SAN post-processing, HUD and optional visual effects.
• Chinese and English in the same jar; follows the Minecraft client language.

The built-in scenarios are original adaptations inspired by the structure of The Haunting and The Lightless Beacon, plus the original Whispers in the Dark. No official scenario text is copied.

Requires Forge 47.x. Client and server must use the same jar.
```

---

## 五、不要上传的内容

- `config/coc/items.json`（某个服务器的 KP 道具规则）
- `config/coc/scenarios/` 或 `config/coc/events/` 下的服务器导入内容
- 世界存档、日志、测试客户端目录
- `coc-mod-src.zip` 不是运行必需；可作为 source 附加文件，但 Modrinth 的 Source 字段填仓库更好

---

## 六、关于 `modrinth.index.json`

**只有发布整合包时才需要。** 格式大致是：

```json
{
  "formatVersion": 1,
  "game": "minecraft",
  "versionId": "1.0.0",
  "name": "My Modpack",
  "files": [
    {
      "path": "mods/coc-2.0.0.jar",
      "hashes": {
        "sha1": "c86a0957948cf799dbf354500847c6f177f04fd0",
        "sha512": "e3ea...4756"
      },
      "downloads": [
        "https://cdn.modrinth.com/data/<project-id>/versions/<version-id>/coc-2.0.0.jar"
      ],
      "fileSize": 608475
    }
  ],
  "dependencies": {
    "minecraft": "1.20.1",
    "forge": "47.4.29"
  }
}
```

注意：
- `downloads` 必须是可公开访问的直链；
- `hashes` 必须与实际上传文件一致；
- 发布 mod 本身不需要这个文件。

---

## 七、发布前检查

- [ ] jar 能单独启动专用服务器，`SELFTEST OK · 事件 75 · 剧本 3`
- [ ] 客户端与服务器使用同一 jar
- [ ] 项目图标（512×512）已上传：`发布素材/coc-icon-512.png`
- [ ] License 选择 MIT，并放 `LICENSE` 文件到仓库
- [ ] 版本号 2.0.0、Loader Forge、Game 1.20.1、Environment Client+Server
- [ ] 描述里写清“client and server both required”
- [ ] 不上传服务器 config/world 数据
- [ ] 示例 JSON 与英文格式说明作为附加文件
