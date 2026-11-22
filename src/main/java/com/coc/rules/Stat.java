package com.coc.rules;

import net.minecraft.nbt.CompoundTag;

/** 九项属性。COC 7e 的角色卡核心数值。 */
public enum Stat {
    STR("STR", "力量"),
    CON("CON", "体质"),
    SIZ("SIZ", "体型"),
    DEX("DEX", "敏捷"),
    APP("APP", "外貌"),
    INT("INT", "智力"),
    POW("POW", "意志"),
    EDU("EDU", "教育"),
    LUCK("LUCK", "幸运");

    private final String abbr;
    private final String cn;

    Stat(String abbr, String cn) {
        this.abbr = abbr;
        this.cn = cn;
    }

    public String abbr() {
        return abbr;
    }

    public String cn() {
        return cn;
    }

    public static Stat byName(String name) {
        if (name == null) return null;
        for (Stat s : values()) {
            if (s.name().equalsIgnoreCase(name) || s.abbr.equalsIgnoreCase(name) || s.cn.equals(name)) {
                return s;
            }
        }
        return null;
    }
}
