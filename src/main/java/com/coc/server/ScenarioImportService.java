package com.coc.server;

import com.coc.config.CocServerConfig;
import com.coc.events.EventDef;
import com.coc.events.EventRegistry;
import com.coc.events.ScenarioAct;
import com.coc.events.ScenarioDef;
import com.coc.kp.KpDataBuilder;
import com.coc.kp.KpManager;
import com.coc.net.CocNetwork;
import com.coc.net.packet.S2CImportResult;
import com.coc.rules.DiceRoller;
import com.coc.rules.Difficulty;
import com.coc.rules.SkillDef;
import com.coc.rules.SkillRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 玩家剧本导入：把一份简化的单文件 JSON 转成标准 scenarios/events JSON，
 * 写入 config/coc 并重载。只生成文本/检定选项，不执行物品、生成生物或命令。
 */
public final class ScenarioImportService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern ID = Pattern.compile("^[a-z][a-z0-9_]{2,31}$");
    private static final int MAX_ACTS = 24;
    private static final int MAX_OPTIONS = 8;
    private static final int MAX_SIDES = 8;

    private ScenarioImportService() {
    }

    public static void importJson(ServerPlayer player, String raw) {
        if (player == null) return;
        if (raw == null || raw.isBlank()) {
            fail(player, "JSON 为空。请选择一个包含剧本的 .json 文件。");
            return;
        }
        raw = raw.trim();
        if (raw.startsWith("﻿")) raw = raw.substring(1).trim();
        if (raw.length() > 65536) {
            fail(player, "JSON 超过 64KB 上限，请精简文本。");
            return;
        }
        Package pkg;
        try {
            pkg = GSON.fromJson(raw, Package.class);
        } catch (Exception e) {
            fail(player, "JSON 语法错误：" + shortMessage(e));
            return;
        }
        String error = validate(pkg);
        if (error != null) {
            fail(player, error);
            return;
        }
        try {
            Imported built = build(pkg);
            writeAndReload(player, built);
        } catch (Exception e) {
            LOGGER.error("导入剧本失败", e);
            fail(player, "导入失败：" + shortMessage(e));
        }
    }

    private static String validate(Package pkg) {
        if (pkg == null) return "JSON 内容为空。";
        if (pkg.format != null && !pkg.format.isBlank()
                && !"coc-scenario-v1".equalsIgnoreCase(pkg.format.trim())) {
            return "format 必须是 coc-scenario-v1。";
        }
        String id = trim(pkg.id);
        if (!ID.matcher(id).matches()) {
            return "id 必须为 3~32 位小写字母/数字/下划线，且以字母开头（例：my_story）。";
        }
        if (EventRegistry.scenario(id) != null) {
            return "剧本 id 已存在：" + id + "，请换一个。";
        }
        if (isBlank(pkg.title)) return "缺少 title。";
        if (pkg.title.length() > 80) return "title 不能超过 80 字。";
        if (pkg.description != null && pkg.description.length() > 1000) return "description 太长（最多 1000 字）。";
        if (pkg.intro != null && pkg.intro.length() > 4000) return "intro 太长（最多 4000 字）。";
        if (pkg.outro != null && pkg.outro.length() > 4000) return "outro 太长（最多 4000 字）。";
        if (pkg.sanReward != null && !pkg.sanReward.isBlank()) {
            try {
                DiceRoller.evalNumber(pkg.sanReward, RandomSource.create(1L));
            } catch (Exception e) {
                return "sanReward 不是合法骰子表达式（例：1d10 或 0）。";
            }
        }
        if (pkg.acts == null || pkg.acts.isEmpty()) return "acts 不能为空。";
        if (pkg.acts.size() > MAX_ACTS) return "幕数太多（最多 " + MAX_ACTS + " 幕）。";
        for (int i = 0; i < pkg.acts.size(); i++) {
            Act act = pkg.acts.get(i);
            String where = "第 " + (i + 1) + " 幕";
            if (act == null) return where + "为空。";
            if (isBlank(act.title)) return where + "缺少 title。";
            if (act.title.length() > 80) return where + " title 太长。";
            if (isBlank(act.description)) return where + "缺少 description（玩家幕布介绍）。";
            if (act.description.length() > 4000) return where + " description 太长。";
            if (act.summary != null && act.summary.length() > 500) return where + " summary 太长。";
            if (act.trigger != null && act.trigger.length() > 500) return where + " trigger 太长。";
            String oe = validateOptions(act.options, where);
            if (oe != null) return oe;
            if (act.sideEvents != null) {
                if (act.sideEvents.size() > MAX_SIDES) return where + "支线太多（最多 " + MAX_SIDES + " 个）。";
                for (int j = 0; j < act.sideEvents.size(); j++) {
                    Side side = act.sideEvents.get(j);
                    String sw = where + "支线 " + (j + 1);
                    if (side == null) return sw + "为空。";
                    if (isBlank(side.title)) return sw + "缺少 title。";
                    if (side.title.length() > 80) return sw + " title 太长。";
                    if (isBlank(side.description)) return sw + "缺少 description。";
                    if (side.description.length() > 4000) return sw + " description 太长。";
                    if (side.trigger != null && side.trigger.length() > 500) return sw + " trigger 太长。";
                    String se = validateOptions(side.options, sw);
                    if (se != null) return se;
                }
            }
        }
        return null;
    }

    private static String validateOptions(List<Option> options, String where) {
        if (options == null) return null;
        if (options.size() > MAX_OPTIONS) return where + "选项太多（最多 " + MAX_OPTIONS + " 个）。";
        for (int i = 0; i < options.size(); i++) {
            Option o = options.get(i);
            String ow = where + "选项 " + (i + 1);
            if (o == null) return ow + "为空。";
            if (isBlank(o.text)) return ow + "缺少 text。";
            if (o.text.length() > 300) return ow + " text 太长。";
            if (o.skill != null && !o.skill.isBlank() && SkillRegistry.resolve(o.skill) == null) {
                return ow + "技能不存在：" + o.skill;
            }
            if (o.difficulty != null && !o.difficulty.isBlank()
                    && !validDifficulty(o.difficulty)) {
                return ow + "难度必须是 regular/hard/extreme 或 常规/困难/极难。";
            }
            if (o.success != null && o.success.length() > 2000) return ow + " success 太长。";
            if (o.failure != null && o.failure.length() > 2000) return ow + " failure 太长。";
            if (o.outcome != null && o.outcome.length() > 2000) return ow + " outcome 太长。";
            if (o.clue != null && o.clue.length() > 2000) return ow + " clue 太长。";
            if (o.successClue != null && o.successClue.length() > 2000) return ow + " successClue 太长。";
            if (o.failureClue != null && o.failureClue.length() > 2000) return ow + " failureClue 太长。";
        }
        return null;
    }

    private static boolean validDifficulty(String raw) {
        String s = raw.trim();
        return s.equalsIgnoreCase("regular") || s.equalsIgnoreCase("hard") || s.equalsIgnoreCase("extreme")
                || s.equals("常规") || s.equals("困难") || s.equals("极难");
    }

    private static Imported build(Package pkg) {
        String id = pkg.id.trim();
        String title = clean(pkg.title);
        ScenarioDef scenario = new ScenarioDef();
        scenario.id = id;
        scenario.title = title;
        scenario.description = clean(pkg.description);
        scenario.intro = clean(pkg.intro);
        scenario.outro = clean(pkg.outro);
        scenario.sanReward = isBlank(pkg.sanReward) ? "1d10" : pkg.sanReward.trim();
        List<EventDef> events = new ArrayList<>();
        for (int i = 0; i < pkg.acts.size(); i++) {
            Act act = pkg.acts.get(i);
            int actNo = i + 1;
            EventDef main = baseEvent("import_" + id + "_a" + actNo, title, actNo,
                    clean(act.title), clean(act.description), clean(act.trigger), "主线", true);
            main.options = convertOptions(act.options);
            events.add(main);

            ScenarioAct sa = new ScenarioAct();
            sa.act = actNo;
            sa.event = main.id;
            sa.title = main.title;
            sa.summary = clean(act.summary);
            sa.trigger = clean(act.trigger);
            if (act.sideEvents != null) {
                for (int j = 0; j < act.sideEvents.size(); j++) {
                    Side side = act.sideEvents.get(j);
                    String sideId = "import_" + id + "_a" + actNo + "_s" + (j + 1);
                    EventDef sideEvent = baseEvent(sideId, title, actNo,
                            clean(side.title), clean(side.description), clean(side.trigger), "支线", false);
                    sideEvent.options = convertOptions(side.options);
                    events.add(sideEvent);
                    sa.sideEvents.add(sideId);
                }
            }
            scenario.acts.add(sa);
            scenario.events.add(main.id);
        }
        return new Imported(scenario, events);
    }

    private static EventDef baseEvent(String id, String scenarioTitle, int act, String title,
                                      String description, String trigger, String category, boolean once) {
        EventDef def = new EventDef();
        def.id = id;
        def.title = title;
        def.description = description;
        def.category = category;
        def.act = act;
        def.scenarioTitle = scenarioTitle;
        def.triggerHint = trigger;
        def.weight = 0;
        def.kpOnly = true;
        def.once = once;
        def.cooldown = 0;
        def.tags = new ArrayList<>(List.of("imported", "scenario"));
        def.conditions = new EventDef.Conditions();
        def.options = new ArrayList<>();
        return def;
    }

    private static List<EventDef.Option> convertOptions(List<Option> input) {
        List<EventDef.Option> out = new ArrayList<>();
        if (input != null) {
            for (Option o : input) {
                if (o == null) continue;
                EventDef.Option dst = new EventDef.Option();
                dst.text = clean(o.text);
                SkillDef skill = isBlank(o.skill) ? null : SkillRegistry.resolve(o.skill);
                dst.skill = skill == null ? "" : skill.id;
                Difficulty diff = isBlank(o.difficulty) ? Difficulty.REGULAR : Difficulty.byName(o.difficulty);
                dst.difficulty = diff.name().toLowerCase(Locale.ROOT);
                boolean hasSkill = !dst.skill.isBlank();
                String successClue = firstNonBlank(o.successClue, hasSkill ? o.clue : "");
                String outcomeClue = hasSkill ? "" : firstNonBlank(o.clue, o.successClue);
                if (!isBlank(o.success) || !isBlank(successClue)) {
                    EventDef.Outcome outcome = new EventDef.Outcome();
                    outcome.text = isBlank(o.success) ? "由 KP 根据行动判定结果。" : clean(o.success);
                    outcome.handout = clean(successClue);
                    dst.success = outcome;
                }
                if (!isBlank(o.failure) || !isBlank(o.failureClue)) {
                    EventDef.Outcome outcome = new EventDef.Outcome();
                    outcome.text = isBlank(o.failure) ? "由 KP 根据行动判定结果。" : clean(o.failure);
                    outcome.handout = clean(o.failureClue);
                    dst.failure = outcome;
                }
                if (!isBlank(o.outcome) || !isBlank(outcomeClue)) {
                    EventDef.Outcome outcome = new EventDef.Outcome();
                    outcome.text = isBlank(o.outcome) ? "由 KP 根据行动描述结果。" : clean(o.outcome);
                    outcome.handout = clean(outcomeClue);
                    dst.outcome = outcome;
                }
                if (dst.success == null && dst.failure == null && dst.outcome == null) {
                    dst.outcome = outcome("由 KP 根据行动描述结果。");
                }
                out.add(dst);
            }
        }
        if (out.isEmpty()) {
            EventDef.Option fallback = new EventDef.Option();
            fallback.text = "（KP 自行模拟玩家行动）";
            fallback.skill = "";
            fallback.difficulty = "regular";
            fallback.outcome = outcome("由 KP 根据玩家行动裁定结果。");
            out.add(fallback);
        }
        return out;
    }

    private static EventDef.Outcome outcome(String text) {
        EventDef.Outcome o = new EventDef.Outcome();
        o.text = clean(text);
        return o;
    }

    private static void writeAndReload(ServerPlayer player, Imported imported) throws Exception {
        ScenarioDef scenario = imported.scenario;
        Path eventDir = CocServerConfig.configDir().resolve("events");
        Path scenarioDir = CocServerConfig.configDir().resolve("scenarios");
        Files.createDirectories(eventDir);
        Files.createDirectories(scenarioDir);
        Path scenarioFile = scenarioDir.resolve(scenario.id + ".json");
        if (Files.exists(scenarioFile)) {
            fail(player, "剧本 id 已存在文件：" + scenario.id + "，请换一个。");
            return;
        }
        Map<Path, String> files = new LinkedHashMap<>();
        files.put(scenarioFile, GSON.toJson(scenario));
        for (EventDef event : imported.events) {
            Path target = eventDir.resolve(event.id + ".json");
            if (Files.exists(target)) {
                fail(player, "事件 id 已存在文件：" + event.id + "，请换一个剧本 id。");
                return;
            }
            files.put(target, GSON.toJson(event));
        }
        List<Path> temps = new ArrayList<>();
        List<Path> written = new ArrayList<>();
        try {
            for (Map.Entry<Path, String> e : files.entrySet()) {
                Path temp = e.getKey().resolveSibling(e.getKey().getFileName() + ".tmp");
                Files.writeString(temp, e.getValue(), StandardCharsets.UTF_8);
                temps.add(temp);
            }
            for (Map.Entry<Path, String> e : files.entrySet()) {
                Path temp = e.getKey().resolveSibling(e.getKey().getFileName() + ".tmp");
                Files.move(temp, e.getKey(), StandardCopyOption.REPLACE_EXISTING);
                written.add(e.getKey());
            }
            EventRegistry.reload();
            ScenarioDef loaded = EventRegistry.scenario(scenario.id);
            if (loaded == null || loaded.acts == null || loaded.acts.size() != scenario.acts.size()) {
                throw new IllegalStateException("重载后未找到剧本，请检查服务器日志");
            }
            String message = "已导入剧本「" + scenario.title + "」（" + scenario.acts.size()
                    + " 幕）。请让 KP 在剧情书页刷新/重新打开。";
            CocNetwork.sendToPlayer(player, new S2CImportResult(true, message, scenario.id, scenario.title));
            for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(Component.literal("【剧本导入】" + player.getGameProfile().getName()
                        + " 添加了「" + scenario.title + "」").withStyle(ChatFormatting.AQUA));
                if (KpManager.isKp(p)) KpDataBuilder.send(p);
            }
        } catch (Exception e) {
            for (Path path : temps) Files.deleteIfExists(path);
            for (Path path : written) Files.deleteIfExists(path);
            EventRegistry.reload();
            throw e;
        }
    }

    private static void fail(ServerPlayer player, String message) {
        CocNetwork.sendToPlayer(player, new S2CImportResult(false, message, "", ""));
        player.sendSystemMessage(Component.literal("【剧本导入】" + message)
                .withStyle(ChatFormatting.RED));
    }

    private static String shortMessage(Exception e) {
        String m = e.getMessage();
        if (m == null || m.isBlank()) m = e.getClass().getSimpleName();
        return m.length() > 160 ? m.substring(0, 160) : m;
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return b == null ? "" : b;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static final class Imported {
        final ScenarioDef scenario;
        final List<EventDef> events;

        Imported(ScenarioDef scenario, List<EventDef> events) {
            this.scenario = scenario;
            this.events = events;
        }
    }

    /** 玩家导入格式：单文件、只含文本/技能/成功失败，不含 effects。 */
    public static final class Package {
        public String format = "";
        public String id = "";
        public String title = "";
        public String description = "";
        public String intro = "";
        public String outro = "";
        public String sanReward = "1d10";
        public List<Act> acts = new ArrayList<>();
    }

    public static final class Act {
        public String title = "";
        public String summary = "";
        public String trigger = "";
        public String description = "";
        public List<Option> options = new ArrayList<>();
        public List<Side> sideEvents = new ArrayList<>();
    }

    public static final class Side {
        public String title = "";
        public String trigger = "";
        public String description = "";
        public List<Option> options = new ArrayList<>();
    }

    public static final class Option {
        public String text = "";
        public String skill = "";
        public String difficulty = "regular";
        public String success = "";
        public String failure = "";
        public String outcome = "";
        public String clue = "";
        public String successClue = "";
        public String failureClue = "";
    }
}
