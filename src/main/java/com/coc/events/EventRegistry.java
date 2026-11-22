package com.coc.events;

import com.coc.config.CocServerConfig;
import com.coc.rules.SkillRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 事件/剧本注册表：内置资源 + config/coc 下自定义 JSON，同 id 覆盖。 */
public final class EventRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, EventDef> EVENTS = new LinkedHashMap<>();
    private static final Map<String, ScenarioDef> SCENARIOS = new LinkedHashMap<>();
    private static final List<String> ERRORS = new ArrayList<>();
    private static final Set<String> KNOWN_EFFECTS = Set.of(
            "san", "hp", "mp", "luck", "skill", "mythos", "item", "effect",
            "sound", "message", "handout", "broadcast", "spawn", "phobia", "mania"
    );

    private EventRegistry() {
    }

    public static synchronized void reload() {
        EVENTS.clear();
        SCENARIOS.clear();
        ERRORS.clear();
        loadBuiltInEvents();
        loadConfigEvents();
        loadBuiltInScenarios();
        loadConfigScenarios();
        applyScenarioMetadata();
        if (!ERRORS.isEmpty()) {
            LOGGER.warn("事件/剧本加载有 {} 条问题：", ERRORS.size());
            for (String error : ERRORS) LOGGER.warn(" - {}", error);
        }
        LOGGER.info("COC 事件加载完成：{} 个事件，{} 个剧本", EVENTS.size(), SCENARIOS.size());
    }

    public static synchronized void ensureLoaded() {
        if (EVENTS.isEmpty()) reload();
    }

    public static EventDef event(String id) {
        ensureLoaded();
        return EVENTS.get(id);
    }

    public static ScenarioDef scenario(String id) {
        ensureLoaded();
        return SCENARIOS.get(id);
    }

    public static Collection<EventDef> events() {
        ensureLoaded();
        return new ArrayList<>(EVENTS.values());
    }

    public static Collection<ScenarioDef> scenarios() {
        ensureLoaded();
        return new ArrayList<>(SCENARIOS.values());
    }

    public static List<String> errors() {
        return new ArrayList<>(ERRORS);
    }

    private static void loadBuiltInEvents() {
        JsonObject index = readResourceJson("events/index.json");
        if (index == null) return;
        JsonArray array = index.has("events") ? index.getAsJsonArray("events") : new JsonArray();
        for (JsonElement element : array) {
            String name = element.getAsString();
            JsonObject json = readResourceJson("events/" + name);
            if (json == null) {
                ERRORS.add("内置事件资源缺失: " + name);
                continue;
            }
            parseEvent(json.toString(), "内置:" + name, false);
        }
    }

    private static void loadBuiltInScenarios() {
        JsonObject index = readResourceJson("scenarios/index.json");
        if (index == null) return;
        JsonArray array = index.has("scenarios") ? index.getAsJsonArray("scenarios") : new JsonArray();
        for (JsonElement element : array) {
            String name = element.getAsString();
            JsonObject json = readResourceJson("scenarios/" + name);
            if (json == null) {
                ERRORS.add("内置剧本资源缺失: " + name);
                continue;
            }
            parseScenario(json.toString(), "内置:" + name, false);
        }
    }

    private static JsonObject readResourceJson(String relative) {
        String path = "/assets/coc/" + relative;
        try (InputStream stream = EventRegistry.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception e) {
            ERRORS.add("读取资源失败 " + path + ": " + e.getMessage());
            return null;
        }
    }

    private static void loadConfigEvents() {
        Path dir = CocServerConfig.configDir().resolve("events");
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            ERRORS.add("无法创建事件目录 " + dir + ": " + e.getMessage());
            return;
        }
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            String json = Files.readString(path, StandardCharsets.UTF_8);
                            parseEvent(stripBom(json), path.getFileName().toString(), true);
                        } catch (Exception e) {
                            ERRORS.add("事件文件解析失败 " + path.getFileName() + ": " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            ERRORS.add("遍历事件目录失败: " + e.getMessage());
        }
    }

    private static void loadConfigScenarios() {
        Path dir = CocServerConfig.configDir().resolve("scenarios");
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            ERRORS.add("无法创建剧本目录 " + dir + ": " + e.getMessage());
            return;
        }
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            String json = Files.readString(path, StandardCharsets.UTF_8);
                            parseScenario(stripBom(json), path.getFileName().toString(), true);
                        } catch (Exception e) {
                            ERRORS.add("剧本文件解析失败 " + path.getFileName() + ": " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            ERRORS.add("遍历剧本目录失败: " + e.getMessage());
        }
    }

    private static void parseEvent(String json, String source, boolean override) {
        try {
            EventDef def = GSON.fromJson(json, EventDef.class);
            if (def == null) throw new IllegalArgumentException("空文件");
            if (def.conditions == null) def.conditions = new EventDef.Conditions();
            if (def.options == null) def.options = new ArrayList<>();
            if (def.tags == null) def.tags = new ArrayList<>();
            validateEvent(def, source);
            if (def.id == null || def.id.isBlank()) return;
            if (!override && EVENTS.containsKey(def.id)) {
                ERRORS.add("内置事件 id 重复: " + def.id + " (" + source + ")");
            }
            EVENTS.put(def.id, def);
        } catch (Exception e) {
            ERRORS.add("事件解析失败 " + source + ": " + e.getMessage());
        }
    }

    private static void parseScenario(String json, String source, boolean override) {
        try {
            ScenarioDef def = GSON.fromJson(json, ScenarioDef.class);
            if (def == null) throw new IllegalArgumentException("空文件");
            if (def.events == null) def.events = new ArrayList<>();
            if (def.acts == null) def.acts = new ArrayList<>();
            if (def.id == null || def.id.isBlank()) throw new IllegalArgumentException("缺少 id");
            if (def.title == null || def.title.isBlank()) throw new IllegalArgumentException("缺少 title");
            if (def.events.isEmpty()) throw new IllegalArgumentException("events 为空");
            if (def.acts.isEmpty()) {
                for (int i = 0; i < def.events.size(); i++) {
                    ScenarioAct act = new ScenarioAct();
                    act.act = i + 1;
                    act.event = def.events.get(i);
                    act.title = "第 " + (i + 1) + " 幕";
                    def.acts.add(act);
                }
            }
            for (ScenarioAct act : def.acts) {
                if (act == null || act.event == null || act.event.isBlank()) {
                    ERRORS.add("剧本 " + def.id + " 有幕次缺少 event");
                    continue;
                }
                if (EVENTS.get(act.event) == null) {
                    ERRORS.add("剧本 " + def.id + " 第 " + act.act + " 幕事件不存在: " + act.event);
                }
                if (act.sideEvents != null) {
                    for (String side : act.sideEvents) {
                        if (EVENTS.get(side) == null) {
                            ERRORS.add("剧本 " + def.id + " 第 " + act.act + " 幕支线不存在: " + side);
                        }
                    }
                }
            }
            if (!override && SCENARIOS.containsKey(def.id)) {
                ERRORS.add("内置剧本 id 重复: " + def.id + " (" + source + ")");
            }
            SCENARIOS.put(def.id, def);
        } catch (Exception e) {
            ERRORS.add("剧本解析失败 " + source + ": " + e.getMessage());
        }
    }

    private static void validateEvent(EventDef def, String source) {
        if (def.id == null || def.id.isBlank()) {
            ERRORS.add("事件缺少 id: " + source);
            return;
        }
        if (def.title == null || def.title.isBlank()) {
            ERRORS.add("事件 " + def.id + " 缺少 title");
        }
        if (def.options == null || def.options.isEmpty()) {
            ERRORS.add("事件 " + def.id + " 没有 options");
            return;
        }
        for (int i = 0; i < def.options.size(); i++) {
            EventDef.Option option = def.options.get(i);
            if (option == null) {
                ERRORS.add("事件 " + def.id + " 选项 " + i + " 为空");
                continue;
            }
            if (option.text == null || option.text.isBlank()) {
                ERRORS.add("事件 " + def.id + " 选项 " + i + " 缺少 text");
            }
            if (option.skill != null && !option.skill.isBlank()
                    && SkillRegistry.resolve(option.skill) == null) {
                ERRORS.add("事件 " + def.id + " 选项 " + i + " 未知技能: " + option.skill);
            }
            validateEffects(def.id + " 选项 " + i, option.effects);
            if (option.outcome != null) validateOutcome(def.id + " 选项 " + i, option.outcome);
            if (option.success != null) validateOutcome(def.id + " 成功", option.success);
            if (option.failure != null) validateOutcome(def.id + " 失败", option.failure);
        }
    }

    private static void validateOutcome(String where, EventDef.Outcome outcome) {
        validateEffects(where, outcome.effects);
        if (outcome.next != null && !outcome.next.isBlank() && EVENTS.get(outcome.next) == null) {
            // 允许指向稍后加载的事件；最终 reload 后仍找不到会在使用时提示
        }
    }

    private static void validateEffects(String where, List<EventDef.Effect> effects) {
        if (effects == null) return;
        for (EventDef.Effect effect : effects) {
            if (effect == null) continue;
            if (effect.type == null || effect.type.isBlank()) {
                ERRORS.add(where + " 存在空效果");
            } else if (!KNOWN_EFFECTS.contains(effect.type)) {
                ERRORS.add(where + " 未知效果类型: " + effect.type);
            }
        }
    }

    private static void applyScenarioMetadata() {
        for (EventDef def : EVENTS.values()) {
            if (def.category == null || def.category.isBlank()) def.category = "随机";
        }
        for (ScenarioDef scenario : SCENARIOS.values()) {
            for (ScenarioAct act : scenario.acts) {
                EventDef main = EVENTS.get(act.event);
                if (main != null) {
                    main.category = "主线";
                    main.act = act.act;
                    main.scenarioTitle = scenario.title == null ? scenario.id : scenario.title;
                    if (act.trigger != null && !act.trigger.isBlank()) main.triggerHint = act.trigger;
                }
                if (act.sideEvents != null) {
                    for (String sideId : act.sideEvents) {
                        EventDef side = EVENTS.get(sideId);
                        if (side != null) {
                            if (side.category == null || side.category.isBlank() || "随机".equals(side.category)) {
                                side.category = "支线";
                            }
                            if (side.act <= 0) side.act = act.act;
                            side.scenarioTitle = scenario.title == null ? scenario.id : scenario.title;
                            if ((side.triggerHint == null || side.triggerHint.isBlank())
                                    && act.trigger != null && !act.trigger.isBlank()) {
                                side.triggerHint = act.trigger;
                            }
                        }
                    }
                }
            }
        }
    }

    private static String stripBom(String s) {
        return s != null && s.startsWith("\uFEFF") ? s.substring(1) : s;
    }

    /** 把内置事件/剧本导出到 config/coc，便于 KP 修改；不覆盖已有文件。 */
    public static synchronized int dumpBuiltIns() {
        int written = 0;
        written += dumpCategory("events", "events/index.json", "events");
        written += dumpCategory("scenarios", "scenarios/index.json", "scenarios");
        return written;
    }

    private static int dumpCategory(String key, String indexPath, String subDir) {
        JsonObject index = readResourceJson(indexPath);
        if (index == null || !index.has(key)) return 0;
        JsonArray array = index.getAsJsonArray(key);
        Path dir = CocServerConfig.configDir().resolve(subDir);
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            ERRORS.add("导出目录创建失败: " + e.getMessage());
            return 0;
        }
        int count = 0;
        for (JsonElement element : array) {
            String name = element.getAsString();
            JsonObject json = readResourceJson(subDir + "/" + name);
            if (json == null) continue;
            Path target = dir.resolve(name);
            if (Files.exists(target)) continue;
            try {
                Files.writeString(target, GSON.toJson(json), StandardCharsets.UTF_8);
                count++;
            } catch (Exception e) {
                ERRORS.add("导出失败 " + target.getFileName() + ": " + e.getMessage());
            }
        }
        return count;
    }
}
