package com.coc.server;

import com.coc.data.CharacterSheet;
import com.coc.data.CocWorldData;
import com.coc.data.Handout;
import com.coc.events.CocEventService;
import com.coc.kp.KpDataBuilder;
import com.coc.kp.KpManager;
import com.coc.net.CocNetwork;
import com.coc.net.packet.S2CSheetJson;
import com.coc.net.packet.S2CSheetResult;
import com.coc.rules.Occupation;
import com.coc.rules.OccupationRegistry;
import com.coc.rules.SkillDef;
import com.coc.rules.SkillRegistry;
import com.coc.rules.Stats;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 角色卡 JSON 导出/导入。 */
public final class CharacterSheetJsonService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_JSON = 262144;

    private CharacterSheetJsonService() {
    }

    public static void export(ServerPlayer kp, UUID targetUuid) {
        if (!authorized(kp)) return;
        ServerPlayer target = target(kp, targetUuid);
        if (target == null) return;
        CharacterSheet sheet = CocData.sheet(target);
        if (sheet == null) {
            fail(kp, "目标没有角色卡。");
            return;
        }
        SheetJson json = SheetJson.from(sheet);
        String text = GSON.toJson(json);
        String file = "sheet_" + safe(sheet.name) + ".json";
        CocNetwork.sendToPlayer(kp, new S2CSheetJson(file, text));
        kp.sendSystemMessage(Component.literal("角色卡已准备导出：" + sheet.name)
                .withStyle(ChatFormatting.AQUA));
    }

    public static void importJson(ServerPlayer kp, UUID targetUuid, String raw) {
        if (!authorized(kp)) return;
        ServerPlayer target = target(kp, targetUuid);
        if (target == null) return;
        if (raw == null || raw.isBlank()) {
            fail(kp, "JSON 为空。");
            return;
        }
        if (raw.length() > MAX_JSON) {
            fail(kp, "角色卡 JSON 超过 256KB。");
            return;
        }
        SheetJson json;
        try {
            json = GSON.fromJson(raw.trim(), SheetJson.class);
        } catch (Exception e) {
            fail(kp, "JSON 语法错误：" + e.getMessage());
            return;
        }
        String error = json == null ? "JSON 为空。" : json.validate();
        if (error != null) {
            fail(kp, error);
            return;
        }
        try {
            CharacterSheet sheet = json.toSheet(target);
            CocWorldData world = CocData.get(target.getServer());
            world.putSheet(sheet);
            if (target != kp) {
                CocEventService.clear(target);
                CheckService.clear(target);
            }
            CocNetwork.syncSheet(target, sheet);
            target.sendSystemMessage(Component.literal("KP 导入了你的角色卡：" + sheet.name)
                    .withStyle(ChatFormatting.GOLD));
            kp.sendSystemMessage(Component.literal("已导入角色卡：" + sheet.name + " → "
                    + target.getGameProfile().getName()).withStyle(ChatFormatting.GOLD));
            CocNetwork.sendToPlayer(kp, new S2CSheetResult(true, "已导入角色卡：" + sheet.name));
            KpDataBuilder.send(kp);
        } catch (Exception e) {
            fail(kp, "导入失败：" + e.getMessage());
        }
    }

    private static boolean authorized(ServerPlayer player) {
        if (player == null) return false;
        if (!KpManager.isKp(player)) {
            player.sendSystemMessage(Component.literal("只有 KP 可以操作角色卡 JSON。")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    private static ServerPlayer target(ServerPlayer kp, UUID uuid) {
        if (uuid == null) {
            fail(kp, "未选择玩家。");
            return null;
        }
        ServerPlayer target = kp.getServer() == null ? null
                : kp.getServer().getPlayerList().getPlayer(uuid);
        if (target == null) {
            fail(kp, "目标玩家不在线。");
            return null;
        }
        return target;
    }

    private static void fail(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("【角色卡】" + message)
                .withStyle(ChatFormatting.RED));
        CocNetwork.sendToPlayer(player, new S2CSheetResult(false, message));
    }

    private static String safe(String name) {
        if (name == null || name.isBlank()) return "character";
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-'
                    || (c >= '一' && c <= '鿿')) {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    public static final class SheetJson {
        public String format = "coc-character-v1";
        public String name = "";
        public int age = 25;
        public String occupationId = "";
        public String occupationName = "";
        public StatsCopy stats = new StatsCopy();
        public int hp, maxHp, mp, maxMp, san, maxSan, mythos, mov, build;
        public String db = "0";
        public Map<String, Integer> skills = new LinkedHashMap<>();
        public List<String> marks = new ArrayList<>();
        public String tempInsanity = "";
        public long tempInsanityEnd;
        public boolean indefiniteInsanity, retired, dying;
        public int dailySanLoss;
        public long lastSanDay = -1;
        public List<String> phobias = new ArrayList<>();
        public List<String> manias = new ArrayList<>();
        public Map<String, String> backstory = new LinkedHashMap<>();
        public String notes = "";
        public List<HandoutJson> handouts = new ArrayList<>();

        static SheetJson from(CharacterSheet sheet) {
            SheetJson j = new SheetJson();
            j.name = sheet.name;
            j.age = sheet.age;
            j.occupationId = sheet.occupationId;
            j.occupationName = sheet.occupationName;
            j.stats = StatsCopy.from(sheet.stats);
            j.hp = sheet.hp;
            j.maxHp = sheet.maxHp;
            j.mp = sheet.mp;
            j.maxMp = sheet.maxMp;
            j.san = sheet.san;
            j.maxSan = sheet.maxSan;
            j.mythos = sheet.mythos;
            j.mov = sheet.mov;
            j.db = sheet.db;
            j.build = sheet.build;
            j.skills.putAll(sheet.skills);
            j.marks.addAll(sheet.improvementMarks);
            j.tempInsanity = sheet.tempInsanity;
            j.tempInsanityEnd = sheet.tempInsanityEnd;
            j.indefiniteInsanity = sheet.indefiniteInsanity;
            j.retired = sheet.retired;
            j.dying = sheet.dying;
            j.dailySanLoss = sheet.dailySanLoss;
            j.lastSanDay = sheet.lastSanDay;
            j.phobias.addAll(sheet.phobias);
            j.manias.addAll(sheet.manias);
            j.backstory.putAll(sheet.backstory);
            j.notes = sheet.notes;
            for (Handout h : sheet.handouts) {
                HandoutJson hh = new HandoutJson();
                hh.title = h.title();
                hh.text = h.text();
                hh.gameTime = h.gameTime();
                j.handouts.add(hh);
            }
            return j;
        }

        String validate() {
            if (format != null && !format.isBlank() && !"coc-character-v1".equalsIgnoreCase(format)) {
                return "format 必须是 coc-character-v1。";
            }
            if (occupationId != null && !occupationId.isBlank()
                    && OccupationRegistry.resolve(occupationId) == null) {
                return "职业 ID 不存在：" + occupationId;
            }
            if (name != null && name.length() > 40) return "name 太长。";
            if (notes != null && notes.length() > 4000) return "notes 太长。";
            if (skills != null && skills.size() > 100) return "skills 条目太多。";
            if (backstory != null && backstory.size() > 20) return "backstory 条目太多。";
            for (Map.Entry<String, Integer> e : skills == null ? Map.<String,Integer>of().entrySet() : skills.entrySet()) {
                if (SkillRegistry.resolve(e.getKey()) == null) return "未知技能：" + e.getKey();
                if (e.getValue() == null || e.getValue() < 0 || e.getValue() > 99) return "技能值超出范围：" + e.getKey();
            }
            for (HandoutJson h : handouts == null ? List.<HandoutJson>of() : handouts) {
                if (h != null && h.text != null && h.text.length() > 4000) return "线索文本太长。";
            }
            return null;
        }

        CharacterSheet toSheet(ServerPlayer target) {
            return toSheet(target.getUUID(), target.getGameProfile().getName());
        }

        public CharacterSheet toSheet(UUID uuid, String targetName) {
            Occupation occupation = occupationId == null || occupationId.isBlank()
                    ? null : OccupationRegistry.resolve(occupationId);
            CharacterSheet sheet = new CharacterSheet(uuid, targetName);
            sheet.age = Math.max(15, Math.min(89, age));
            sheet.occupationId = occupation == null ? "" : occupation.id;
            sheet.occupationName = occupation == null ? "" : occupation.name;
            Stats s = stats == null ? new Stats() : stats.toStats();
            sheet.stats.str = clamp(s.str, 1, 99);
            sheet.stats.con = clamp(s.con, 1, 99);
            sheet.stats.siz = clamp(s.siz, 1, 99);
            sheet.stats.dex = clamp(s.dex, 1, 99);
            sheet.stats.app = clamp(s.app, 1, 99);
            sheet.stats.intel = clamp(s.intel, 1, 99);
            sheet.stats.pow = clamp(s.pow, 1, 99);
            sheet.stats.edu = clamp(s.edu, 1, 99);
            sheet.stats.luck = clamp(s.luck, 0, 99);
            sheet.mythos = clamp(mythos, 0, 99);
            sheet.recalculateDerived();
            sheet.hp = clamp(hp, 0, sheet.maxHp);
            sheet.mp = clamp(mp, 0, sheet.maxMp);
            sheet.san = clamp(san, 0, sheet.maxSan);
            sheet.dying = dying && sheet.hp <= 0;
            sheet.improvementMarks.clear();
            if (marks != null) {
                for (String id : marks) {
                    SkillDef def = SkillRegistry.resolve(id);
                    if (def != null) sheet.improvementMarks.add(def.id);
                }
            }
            for (SkillDef def : SkillRegistry.all()) {
                int base = def.baseValue(sheet.stats.dex, sheet.stats.edu);
                int value = skills == null ? base : skills.getOrDefault(def.id, base);
                sheet.skills.put(def.id, clamp(value, 0, 99));
            }
            sheet.tempInsanity = tempInsanity == null ? "" : tempInsanity;
            sheet.tempInsanityEnd = tempInsanityEnd;
            sheet.indefiniteInsanity = indefiniteInsanity;
            sheet.retired = retired;
            sheet.dailySanLoss = Math.max(0, dailySanLoss);
            sheet.lastSanDay = lastSanDay;
            sheet.phobias.clear();
            sheet.manias.clear();
            if (phobias != null) {
                for (String value : phobias) if (value != null && sheet.phobias.size() < 50) sheet.phobias.add(value);
            }
            if (manias != null) {
                for (String value : manias) if (value != null && sheet.manias.size() < 50) sheet.manias.add(value);
            }
            for (String key : CharacterSheet.BACKSTORY_KEYS) {
                String value = backstory == null ? "" : backstory.getOrDefault(key, "");
                if (value == null) value = "";
                sheet.backstory.put(key, value.substring(0, Math.min(1000, value.length())));
            }
            sheet.notes = notes == null ? "" : notes.substring(0, Math.min(4000, notes.length()));
            sheet.handouts.clear();
            if (handouts != null) {
                for (HandoutJson h : handouts) {
                    if (h == null) continue;
                    String title = h.title == null ? "" : h.title.substring(0, Math.min(120, h.title.length()));
                    String text = h.text == null ? "" : h.text.substring(0, Math.min(4000, h.text.length()));
                    sheet.handouts.add(new Handout(title, text, h.gameTime));
                    if (sheet.handouts.size() >= 100) break;
                }
            }
            return sheet;
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    public static final class HandoutJson {
        public String title = "";
        public String text = "";
        public long gameTime;
    }

    /** Gson-friendly copy of Stats (keeps the public JSON key "intel"). */
    public static final class StatsCopy {
        public int str, con, siz, dex, app, intel, pow, edu, luck;

        static StatsCopy from(Stats s) {
            StatsCopy c = new StatsCopy();
            c.str = s.str;
            c.con = s.con;
            c.siz = s.siz;
            c.dex = s.dex;
            c.app = s.app;
            c.intel = s.intel;
            c.pow = s.pow;
            c.edu = s.edu;
            c.luck = s.luck;
            return c;
        }

        Stats toStats() {
            Stats s = new Stats();
            s.str = str;
            s.con = con;
            s.siz = siz;
            s.dex = dex;
            s.app = app;
            s.intel = intel;
            s.pow = pow;
            s.edu = edu;
            s.luck = luck;
            return s;
        }
    }
}
