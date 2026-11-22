package com.coc.server;

import com.coc.config.CocServerConfig;
import com.coc.rules.SkillDef;
import com.coc.rules.SkillRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** KP 道具库：物品 ID + 使用技能 + 目标点数，持久化到 config/coc/items.json。 */
public final class ItemLibrary {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static List<ItemRule> rules;

    private ItemLibrary() {
    }

    public static synchronized List<ItemRule> rules() {
        load();
        return List.copyOf(rules);
    }

    public static synchronized ItemRule get(String itemId) {
        load();
        String id = canonical(itemId);
        if (id == null) return null;
        for (ItemRule rule : rules) {
            if (id.equals(rule.id)) return rule;
        }
        return null;
    }

    public static synchronized boolean upsert(String itemId, String skill, int target) {
        if (!load()) return false;
        String id = canonical(itemId);
        if (id == null) return false;
        String skillId = "";
        if (skill != null && !skill.isBlank()) {
            SkillDef def = SkillRegistry.resolve(skill);
            if (def == null) return false;
            skillId = def.id;
        }
        int goal = Math.max(1, Math.min(100, target));
        ItemRule found = null;
        for (ItemRule rule : rules) {
            if (id.equals(rule.id)) {
                found = rule;
                break;
            }
        }
        if (found == null) {
            found = new ItemRule();
            found.id = id;
            rules.add(found);
        }
        found.skill = skillId;
        found.target = goal;
        save();
        return true;
    }

    public static synchronized boolean remove(String itemId) {
        if (!load()) return false;
        String id = canonical(itemId);
        if (id == null) return false;
        boolean removed = rules.removeIf(rule -> id.equals(rule.id));
        if (removed) save();
        return removed;
    }

    private static String canonical(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id.trim());
        if (rl == null) return null;
        Item item = BuiltInRegistries.ITEM.get(rl);
        if (item == Items.AIR) return null;
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static boolean load() {
        if (rules != null) return true;
        rules = new ArrayList<>();
        Path file = file();
        if (!Files.isRegularFile(file)) return true;
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            Data data = GSON.fromJson(json, Data.class);
            if (data != null && data.items != null) {
                for (ItemRule rule : data.items) {
                    if (rule == null || rule.id == null) continue;
                    String id = canonical(rule.id);
                    if (id == null) continue;
                    if (rule.skill != null && !rule.skill.isBlank()) {
                        SkillDef def = SkillRegistry.resolve(rule.skill);
                        rule.skill = def == null ? "" : def.id;
                    } else {
                        rule.skill = "";
                    }
                    rule.id = id;
                    rule.target = Math.max(1, Math.min(100, rule.target));
                    rules.add(rule);
                }
            }
        } catch (Exception e) {
            LOGGER.error("读取 config/coc/items.json 失败，使用空道具库", e);
            rules.clear();
        }
        return true;
    }

    private static void save() {
        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            Data data = new Data();
            data.items = rules;
            Files.writeString(file, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("保存 config/coc/items.json 失败", e);
        }
    }

    private static Path file() {
        return CocServerConfig.configDir().resolve("items.json");
    }

    public static final class ItemRule {
        public String id = "";
        public String skill = "";
        public int target = 50;
    }

    private static final class Data {
        List<ItemRule> items = new ArrayList<>();
    }
}
