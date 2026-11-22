package com.coc.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 客户端 KP 面板数据。 */
public final class KpView {
    public boolean allowed;
    public String scenarioStatus = "";
    public boolean randomEnabled;
    public int randomInterval;
    public boolean scenarioActive;
    public String scenarioId = "";
    public int scenarioIndex;
    public final List<Player> players = new ArrayList<>();
    public final List<Event> events = new ArrayList<>();
    public final List<Scenario> scenarios = new ArrayList<>();
    public final List<ItemRule> itemRules = new ArrayList<>();
    public final List<String> errors = new ArrayList<>();

    public static final class Player {
        public UUID uuid;
        public String name = "";
        public boolean hasSheet;
        public int san, maxSan, hp, maxHp, mp, maxMp, luck, mythos;
        public final Map<String, Integer> skills = new LinkedHashMap<>();
        public String status = "";
        public boolean retired, insane;
    }

    public static final class ItemRule {
        public String id = "";
        public String skill = "";
        public int target = 50;
    }

    public static final class Event {
        public String id = "";
        public String title = "";
        public int weight;
        public boolean kpOnly, once;
        public String category = "随机";
        public int act;
        public String scenario = "";
    }

    public static final class Scenario {
        public String id = "";
        public String title = "";
        public int count;
        public final List<Act> acts = new ArrayList<>();
    }

    public static final class Act {
        public int act;
        public String event = "";
        public String title = "";
        public String summary = "";
        public String trigger = "";
        public final List<String> sideEvents = new ArrayList<>();
    }

    public ItemRule itemRule(String id) {
        if (id == null) return null;
        for (ItemRule rule : itemRules) {
            if (id.equals(rule.id)) return rule;
        }
        return null;
    }

    public static KpView fromNbt(CompoundTag tag) {
        KpView view = new KpView();
        if (tag == null) return view;
        view.allowed = tag.getBoolean("allowed");
        view.scenarioStatus = tag.getString("scenario_status");
        view.randomEnabled = tag.getBoolean("random_enabled");
        view.randomInterval = tag.getInt("random_interval");
        view.scenarioActive = tag.getBoolean("scenario_active");
        view.scenarioId = tag.getString("scenario_id");
        view.scenarioIndex = tag.getInt("scenario_index");
        ListTag players = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag t = players.getCompound(i);
            Player p = new Player();
            p.uuid = t.contains("uuid") ? t.getUUID("uuid") : null;
            p.name = t.getString("name");
            p.hasSheet = t.getBoolean("has_sheet");
            p.san = t.getInt("san");
            p.maxSan = t.getInt("max_san");
            p.hp = t.getInt("hp");
            p.maxHp = t.getInt("max_hp");
            p.mp = t.getInt("mp");
            p.maxMp = t.getInt("max_mp");
            p.luck = t.getInt("luck");
            p.mythos = t.getInt("mythos");
            CompoundTag skills = t.getCompound("skills");
            for (String key : skills.getAllKeys()) p.skills.put(key, skills.getInt(key));
            p.status = t.getString("status");
            p.retired = t.getBoolean("retired");
            p.insane = t.getBoolean("insane");
            view.players.add(p);
        }
        ListTag itemRules = tag.getList("item_rules", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemRules.size(); i++) {
            CompoundTag t = itemRules.getCompound(i);
            ItemRule r = new ItemRule();
            r.id = t.getString("id");
            r.skill = t.getString("skill");
            r.target = t.getInt("target");
            view.itemRules.add(r);
        }
        ListTag events = tag.getList("events", Tag.TAG_COMPOUND);
        for (int i = 0; i < events.size(); i++) {
            CompoundTag t = events.getCompound(i);
            Event e = new Event();
            e.id = t.getString("id");
            e.title = t.getString("title");
            e.weight = t.getInt("weight");
            e.kpOnly = t.getBoolean("kp_only");
            e.once = t.getBoolean("once");
            e.category = t.contains("category") ? t.getString("category") : "随机";
            e.act = t.getInt("act");
            e.scenario = t.getString("scenario");
            view.events.add(e);
        }
        ListTag scenarios = tag.getList("scenarios", Tag.TAG_COMPOUND);
        for (int i = 0; i < scenarios.size(); i++) {
            CompoundTag t = scenarios.getCompound(i);
            Scenario s = new Scenario();
            s.id = t.getString("id");
            s.title = t.getString("title");
            s.count = t.getInt("count");
            ListTag actList = t.getList("acts", Tag.TAG_COMPOUND);
            for (int j = 0; j < actList.size(); j++) {
                CompoundTag at = actList.getCompound(j);
                Act a = new Act();
                a.act = at.getInt("act");
                a.event = at.getString("event");
                a.title = at.getString("title");
                a.summary = at.getString("summary");
                a.trigger = at.getString("trigger");
                ListTag sides = at.getList("side_events", Tag.TAG_STRING);
                for (int k = 0; k < sides.size(); k++) a.sideEvents.add(sides.getString(k));
                s.acts.add(a);
            }
            view.scenarios.add(s);
        }
        ListTag errors = tag.getList("errors", Tag.TAG_STRING);
        for (int i = 0; i < errors.size(); i++) view.errors.add(errors.getString(i));
        return view;
    }
}
