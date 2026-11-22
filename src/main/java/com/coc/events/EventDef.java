package com.coc.events;

import java.util.ArrayList;
import java.util.List;

/** 事件 JSON 模型。字段与文档中的格式一一对应。 */
public final class EventDef {
    public String id = "";
    public String title = "";
    public String description = "";
    public String category = "";
    public int act = 0;
    public String scenarioTitle = "";
    public String triggerHint = "";
    public int weight = 10;
    public boolean kpOnly = false;
    public boolean once = false;
    public long cooldown = 0;
    public List<String> tags = new ArrayList<>();
    public Conditions conditions = new Conditions();
    public List<Option> options = new ArrayList<>();

    public static final class Conditions {
        public Integer minSan;
        public Integer maxSan;
        public Integer minMythos;
        public List<String> dimensions = new ArrayList<>();
        public String daytime = "any";
        public List<String> biomes = new ArrayList<>();
    }

    public static final class Option {
        public String text = "";
        public String skill = "";
        public String difficulty = "regular";
        public Integer bonus;
        public Outcome outcome;
        public Outcome success;
        public Outcome failure;
        public List<Effect> effects = new ArrayList<>();
        public String next = "";
    }

    public static final class Outcome {
        public String text = "";
        public List<Effect> effects = new ArrayList<>();
        public String handout = "";
        public String next = "";
        public String phobia = "";
        public String mania = "";
    }

    public static final class Effect {
        public String type = "";
        public String value = "";
        public String id = "";
        public int count = 1;
        public int duration = 200;
        public int amplifier = 0;
        public String text = "";
    }
}
