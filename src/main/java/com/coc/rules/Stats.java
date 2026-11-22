package com.coc.rules;

import net.minecraft.nbt.CompoundTag;

/** 可变的九属性集合，便于生成、年龄修正、序列化。 */
public final class Stats {
    public int str;
    public int con;
    public int siz;
    public int dex;
    public int app;
    public int intel;
    public int pow;
    public int edu;
    public int luck;

    public Stats() {
    }

    public Stats(Stats other) {
        this.str = other.str;
        this.con = other.con;
        this.siz = other.siz;
        this.dex = other.dex;
        this.app = other.app;
        this.intel = other.intel;
        this.pow = other.pow;
        this.edu = other.edu;
        this.luck = other.luck;
    }

    public int get(Stat stat) {
        return switch (stat) {
            case STR -> str;
            case CON -> con;
            case SIZ -> siz;
            case DEX -> dex;
            case APP -> app;
            case INT -> intel;
            case POW -> pow;
            case EDU -> edu;
            case LUCK -> luck;
        };
    }

    public void set(Stat stat, int value) {
        switch (stat) {
            case STR -> str = value;
            case CON -> con = value;
            case SIZ -> siz = value;
            case DEX -> dex = value;
            case APP -> app = value;
            case INT -> intel = value;
            case POW -> pow = value;
            case EDU -> edu = value;
            case LUCK -> luck = value;
        }
    }

    public int physicalTotal() {
        return str + siz;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("str", str);
        tag.putInt("con", con);
        tag.putInt("siz", siz);
        tag.putInt("dex", dex);
        tag.putInt("app", app);
        tag.putInt("int", intel);
        tag.putInt("pow", pow);
        tag.putInt("edu", edu);
        tag.putInt("luck", luck);
        return tag;
    }

    public static Stats fromNbt(CompoundTag tag) {
        Stats s = new Stats();
        s.str = tag.getInt("str");
        s.con = tag.getInt("con");
        s.siz = tag.getInt("siz");
        s.dex = tag.getInt("dex");
        s.app = tag.getInt("app");
        s.intel = tag.getInt("int");
        s.pow = tag.getInt("pow");
        s.edu = tag.getInt("edu");
        s.luck = tag.getInt("luck");
        return s;
    }
}
