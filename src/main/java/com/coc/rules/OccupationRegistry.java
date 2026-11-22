package com.coc.rules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 内置职业表。新增职业只需在此追加一条。 */
public final class OccupationRegistry {
    private static final Map<String, Occupation> OCCUPATIONS = new LinkedHashMap<>();

    static {
        add(new Occupation("detective", "侦探", "在阴影中追查真相的人。",
                2, Stat.DEX, 2,
                List.of("spot_hidden", "listen", "library_use", "psychology", "fast_talk",
                        "firearms_handgun", "law", "stealth")));
        add(new Occupation("reporter", "记者", "为头条奔走，也为真相冒险。",
                4, null, 0,
                List.of("fast_talk", "history", "library_use", "psychology", "spot_hidden",
                        "persuade", "language_own", "art_craft")));
        add(new Occupation("doctor", "医生", "见过太多不该见的伤口。",
                4, null, 0,
                List.of("first_aid", "medicine", "science", "psychology", "psychoanalysis",
                        "language_other", "persuade", "library_use")));
        add(new Occupation("professor", "教授", "图书馆是你的第二战场。",
                4, null, 0,
                List.of("library_use", "language_own", "language_other", "history",
                        "psychology", "science", "persuade", "spot_hidden")));
        add(new Occupation("police", "警察", "秩序的最后一道防线。",
                4, null, 0,
                List.of("spot_hidden", "listen", "psychology", "fighting_brawl",
                        "firearms_handgun", "law", "drive_auto", "intimidate")));
        add(new Occupation("soldier", "军人", "枪与泥，血与命令。",
                2, Stat.STR, 2,
                List.of("firearms_rifle", "fighting_brawl", "dodge", "first_aid",
                        "stealth", "survival", "climb", "throw")));
        add(new Occupation("priest", "神父", "信仰是你的盾，也可能是你的裂缝。",
                4, null, 0,
                List.of("library_use", "psychology", "persuade", "history", "occult",
                        "language_other", "language_own", "psychoanalysis")));
        add(new Occupation("writer", "作家", "你写下故事，故事也在写你。",
                4, null, 0,
                List.of("library_use", "language_own", "history", "psychology", "occult",
                        "persuade", "natural_world", "spot_hidden")));
        add(new Occupation("antiquarian", "古董商", "旧物会说话，只是没人愿意听。",
                4, null, 0,
                List.of("appraise", "history", "library_use", "spot_hidden", "persuade",
                        "accounting", "occult", "charm")));
        add(new Occupation("drifter", "流浪者", "你没有家，所以什么都不怕失去。",
                2, Stat.APP, 2,
                List.of("stealth", "survival", "listen", "spot_hidden", "climb",
                        "dodge", "throw", "natural_world")));
        add(new Occupation("engineer", "工程师", "机器不会撒谎，人会。",
                4, null, 0,
                List.of("elec_repair", "mech_repair", "operate_heavy", "science",
                        "spot_hidden", "library_use", "demolitions", "drive_auto")));
        add(new Occupation("artist", "艺术家", "你看见的颜色，别人看不见。",
                2, Stat.APP, 2,
                List.of("art_craft", "spot_hidden", "psychology", "charm", "history",
                        "language_own", "language_other", "occult")));
        add(new Occupation("occultist", "神秘学家", "知识有代价，你已经在付。",
                4, null, 0,
                List.of("occult", "library_use", "history", "anthropology", "psychology",
                        "language_other", "spot_hidden", "persuade")));
        add(new Occupation("nurse", "护士", "你比医生更常看见病人眼里的东西。",
                4, null, 0,
                List.of("first_aid", "medicine", "psychology", "listen", "science",
                        "persuade", "psychoanalysis", "spot_hidden")));
        add(new Occupation("criminal", "罪犯", "规则是给守规矩的人准备的。",
                2, Stat.DEX, 2,
                List.of("stealth", "locksmith", "sleight_of_hand", "spot_hidden",
                        "fighting_brawl", "firearms_handgun", "intimidate", "appraise")));
    }

    private OccupationRegistry() {
    }

    private static void add(Occupation occupation) {
        OCCUPATIONS.put(occupation.id, occupation);
    }

    public static List<Occupation> all() {
        return new ArrayList<>(OCCUPATIONS.values());
    }

    public static Occupation get(String id) {
        return OCCUPATIONS.get(id);
    }

    public static Occupation resolve(String key) {
        if (key == null) return null;
        Occupation byId = OCCUPATIONS.get(key);
        if (byId != null) return byId;
        for (Occupation o : OCCUPATIONS.values()) {
            if (o.name.equals(key) || o.id.equalsIgnoreCase(key)) return o;
        }
        return null;
    }
}
