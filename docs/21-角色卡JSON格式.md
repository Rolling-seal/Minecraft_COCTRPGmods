# 角色卡 JSON 格式（coc-character-v1）

> 适用：`coc-2.1.0.jar`。
> 用途：KP 备份/迁移玩家角色卡；导出与导入都使用同一格式。
> 入口：KP 面板 → **玩家** → 选中玩家 → **角色卡 JSON** → 导出/导入。

---

## 一、导入会发生什么

- 文件只在**客户端**读取，然后发给服务端；服务端做语法、字段、技能/职业合法性校验。
- 导入会**覆盖选中玩家**的角色卡；`name`、UUID 保持为当前玩家，不使用 JSON 里的 `name`。
- 派生值会重新计算：`maxHp`、`maxMp`、`maxSan`、`mov`、`build` 以属性/神话为准；JSON 里的这些字段仅作记录。
- 技能值限制 0~99；缺失的技能按职业/属性基础值补齐。
- 背景只保留六项标准键；备注最多 4000 字；线索最多 100 条。
- 导入后清空该玩家的临时事件/检定状态；建议先导出备份。

## 二、限制

| 项目 | 限制 |
|---|---|
| 文件大小 | 256 KB |
| 职业 | 必须是已注册的职业 ID；可留空 |
| 技能 | 必须是已注册技能 ID，值 0~99，最多 100 条 |
| 背景 | 最多 20 条；只读取六项标准键 |
| 线索 | 最多 100 条，每条文本最多 4000 字 |
| 备注 | 最多 4000 字 |

## 三、字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `format` | string | 固定 `coc-character-v1`；为空也接受 |
| `name` | string | 导出时记录；导入时忽略（用当前玩家名） |
| `age` | int | 15~89，超出会夹取 |
| `occupationId` | string | 职业 ID，例如 `detective`；可空 |
| `occupationName` | string | 导出时记录；导入时按 `occupationId` 重新解析 |
| `stats` | object | 九项属性：`str` `con` `siz` `dex` `app` `intel` `pow` `edu` `luck` |
| `hp` / `maxHp` | int | 当前/最大 HP；`maxHp` 导入时重算 |
| `mp` / `maxMp` | int | 当前/最大 MP；`maxMp` 导入时重算 |
| `san` / `maxSan` | int | 当前/最大 SAN；`maxSan` 导入时重算 |
| `mythos` | int | 克苏鲁神话，0~99 |
| `mov` / `build` | int | 移动/体格；导入时重算 |
| `db` | string | 伤害加值，例如 `"0"`、`"1d4"`、`"-1"` |
| `skills` | object | 技能 ID → 数值，例如 `{"spot_hidden":65}` |
| `marks` | string[] | 已勾选的成长标记技能 ID |
| `tempInsanity` | string | 临时疯狂说明 |
| `tempInsanityEnd` | long | 临时疯狂结束的游戏时间 |
| `indefiniteInsanity` | bool | 不定期疯狂 |
| `retired` | bool | 角色已退休 |
| `dying` | bool | 濒死标记；HP>0 时导入为 false |
| `dailySanLoss` | int | 当日累计 SAN 损失 |
| `lastSanDay` | long | 上次 SAN 结算日；`-1` 表示未记录 |
| `phobias` | string[] | 恐惧症，最多 50 条 |
| `manias` | string[] | 狂躁症，最多 50 条 |
| `backstory` | object | 六项背景：`信念` `重要之人` `意义非凡之地` `宝贵之物` `特质` `伤口` |
| `notes` | string | 备注 |
| `handouts` | array | 线索：`{title, text, gameTime}` |

## 四、示例

```json
{
  "format": "coc-character-v1",
  "name": "艾琳·卡特",
  "age": 28,
  "occupationId": "detective",
  "occupationName": "侦探",
  "stats": {
    "str": 55, "con": 60, "siz": 55, "dex": 65,
    "app": 50, "intel": 70, "pow": 55, "edu": 70, "luck": 45
  },
  "hp": 11,
  "maxHp": 11,
  "mp": 11,
  "maxMp": 11,
  "san": 55,
  "maxSan": 99,
  "mythos": 0,
  "mov": 8,
  "build": 0,
  "db": "0",
  "skills": {
    "spot_hidden": 65,
    "listen": 55,
    "psychology": 60,
    "firearms_handgun": 50
  },
  "marks": ["spot_hidden"],
  "tempInsanity": "",
  "tempInsanityEnd": 0,
  "indefiniteInsanity": false,
  "retired": false,
  "dying": false,
  "dailySanLoss": 0,
  "lastSanDay": -1,
  "phobias": [],
  "manias": [],
  "backstory": {
    "信念": "真相值得付出代价。",
    "重要之人": "失踪的搭档。",
    "意义非凡之地": "旧警局档案室。",
    "宝贵之物": "搭档留下的怀表。",
    "特质": "不肯放过细节。",
    "伤口": "左肩旧枪伤。"
  },
  "notes": "正在调查阿卡姆郊外的旧宅。",
  "handouts": [
    {
      "title": "剪报",
      "text": "1925 年 3 月，又一名租客在 Corbitt 旧宅失踪。",
      "gameTime": 0
    }
  ]
}
```

## 五、English summary

- Format id: `coc-character-v1`; max file size 256 KB.
- Import overwrites the **selected player's** sheet; the player's Minecraft name/UUID are kept.
- `maxHp`, `maxMp`, `maxSan`, `mov`, `build` are recalculated on import; `occupationName` is resolved from `occupationId`.
- Skills must be registered IDs with values 0–99. Six backstory keys only: `信念`, `重要之人`, `意义非凡之地`, `宝贵之物`, `特质`, `伤口`.
- Export first if you are unsure; import clears the target's temporary event/check state.
