package com.coc.rules;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** COC 7e 核心技能表。id 用于命令/JSON，中文名用于界面。 */
public final class SkillRegistry {
    private static final Map<String, SkillDef> SKILLS = new LinkedHashMap<>();

    static {
        fixed("accounting", "会计", 5);
        fixed("anthropology", "人类学", 1);
        fixed("appraise", "估价", 5);
        fixed("archaeology", "考古学", 1);
        fixed("charm", "魅惑", 15);
        fixed("climb", "攀爬", 20);
        fixed("credit_rating", "信用评级", 0);
        fixed("disguise", "乔装", 5);
        dex("dodge", "闪避");
        fixed("drive_auto", "汽车驾驶", 20);
        fixed("elec_repair", "电气维修", 10);
        fixed("electronics", "电子学", 1);
        fixed("fast_talk", "话术", 5);
        fixed("fighting_brawl", "格斗(斗殴)", 25);
        fixed("fighting_blade", "格斗(剑)", 20);
        fixed("firearms_handgun", "射击(手枪)", 20);
        fixed("firearms_rifle", "射击(步枪/霰弹枪)", 25);
        fixed("first_aid", "急救", 30);
        fixed("history", "历史", 5);
        fixed("intimidate", "恐吓", 15);
        fixed("jump", "跳跃", 20);
        fixed("language_other", "外语", 1);
        edu("language_own", "母语");
        fixed("law", "法律", 5);
        fixed("library_use", "图书馆使用", 20);
        fixed("listen", "聆听", 20);
        fixed("locksmith", "锁匠", 1);
        fixed("mech_repair", "机械维修", 10);
        fixed("medicine", "医学", 1);
        fixed("natural_world", "博物学", 10);
        fixed("navigate", "导航", 10);
        fixed("occult", "神秘学", 5);
        fixed("operate_heavy", "操作重型机械", 1);
        fixed("persuade", "说服", 10);
        fixed("pilot_boat", "驾驶(船)", 1);
        fixed("psychoanalysis", "精神分析", 1);
        fixed("psychology", "心理学", 10);
        fixed("ride", "骑术", 5);
        fixed("science", "科学", 1);
        fixed("sleight_of_hand", "妙手", 10);
        fixed("spot_hidden", "侦查", 25);
        fixed("stealth", "潜行", 20);
        fixed("survival", "生存", 10);
        fixed("swim", "游泳", 20);
        fixed("throw", "投掷", 20);
        fixed("track", "追踪", 10);
        fixed("diving", "潜水", 1);
        fixed("demolitions", "爆破", 1);
        fixed("lip_reading", "读唇", 1);
        fixed("hypnosis", "催眠", 1);
        fixed("artillery", "炮术", 1);
        fixed("art_craft", "技艺", 5);
        fixed("firearms_bow", "射击(弓)", 15);
        fixed("fighting_axe", "格斗(斧)", 15);
        fixed("fighting_spear", "格斗(矛)", 20);
    }

    private SkillRegistry() {
    }

    private static void fixed(String id, String name, int base) {
        SKILLS.put(id, new SkillDef(id, name, SkillDef.BaseType.FIXED, base));
    }

    private static void dex(String id, String name) {
        SKILLS.put(id, new SkillDef(id, name, SkillDef.BaseType.DEX_HALF, 0));
    }

    private static void edu(String id, String name) {
        SKILLS.put(id, new SkillDef(id, name, SkillDef.BaseType.EDU, 0));
    }

    public static Collection<SkillDef> all() {
        return SKILLS.values();
    }

    public static SkillDef get(String id) {
        return SKILLS.get(id);
    }

    /** 支持 id、英文名大小写、中文名。 */
    public static SkillDef resolve(String key) {
        if (key == null) return null;
        SkillDef byId = SKILLS.get(key);
        if (byId != null) return byId;
        for (SkillDef def : SKILLS.values()) {
            if (def.name.equals(key) || def.id.equalsIgnoreCase(key)) return def;
        }
        return null;
    }

    public static int baseValue(String id, int dex, int edu) {
        SkillDef def = SKILLS.get(id);
        return def == null ? 0 : def.baseValue(dex, edu);
    }

    public static String nameOf(String id) {
        SkillDef def = SKILLS.get(id);
        return def == null ? id : def.name;
    }
}
