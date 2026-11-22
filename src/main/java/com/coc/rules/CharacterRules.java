package com.coc.rules;

import net.minecraft.util.RandomSource;

/** 属性生成、年龄修正、衍生属性、伤害加成表。 */
public final class CharacterRules {
    private CharacterRules() {
    }

    public record AgeBand(int strMod, int conMod, int sizMod, int dexMod, int appMod, int eduMod,
                          int eduChecks, int movPenalty) {
    }

    public static AgeBand ageBand(int age) {
        int a = Math.max(15, Math.min(89, age));
        if (a <= 19) return new AgeBand(-5, 0, -5, 0, 0, -5, 0, 0);
        if (a <= 39) return new AgeBand(0, 0, 0, 0, 0, 0, 1, 0);
        if (a <= 49) return new AgeBand(-5, -5, 0, -5, -5, 0, 1, 1);
        if (a <= 59) return new AgeBand(-10, -10, 0, -10, -10, 0, 2, 2);
        if (a <= 69) return new AgeBand(-20, -20, 0, -20, -15, 0, 3, 3);
        if (a <= 79) return new AgeBand(-40, -40, 0, -40, -20, 0, 4, 4);
        return new AgeBand(-80, -80, 0, -80, -25, 0, 4, 5);
    }

    public static Stats rollBaseStats(RandomSource random) {
        Stats s = new Stats();
        s.str = roll(3, 6, random) * 5;
        s.con = roll(3, 6, random) * 5;
        s.siz = (roll(2, 6, random) + 6) * 5;
        s.dex = roll(3, 6, random) * 5;
        s.app = roll(3, 6, random) * 5;
        s.intel = (roll(2, 6, random) + 6) * 5;
        s.pow = roll(3, 6, random) * 5;
        s.edu = (roll(3, 6, random) + 3) * 5;
        s.luck = roll(3, 6, random) * 5;
        return s;
    }

    private static int roll(int count, int sides, RandomSource random) {
        int total = 0;
        for (int i = 0; i < count; i++) total += random.nextInt(sides) + 1;
        return total;
    }

    /**
     * 应用年龄修正。doEduChecks=true 时按 COC 规则进行 EDU 成长检定（服务端创建角色时使用）。
     * 客户端预览传 false，最终以服务端同步为准。
     */
    public static Stats applyAge(Stats base, int age, RandomSource random, boolean doEduChecks) {
        Stats s = new Stats(base);
        AgeBand band = ageBand(age);
        s.str = clamp(s.str + band.strMod());
        s.con = clamp(s.con + band.conMod());
        s.siz = clamp(s.siz + band.sizMod());
        s.dex = clamp(s.dex + band.dexMod());
        s.app = clamp(s.app + band.appMod());
        s.edu = clamp(s.edu + band.eduMod());
        if (doEduChecks) {
            for (int i = 0; i < band.eduChecks(); i++) {
                if (DiceRoller.d100(random) > s.edu) {
                    s.edu = clamp(s.edu + DiceRoller.d10(random));
                }
            }
        }
        return s;
    }

    public static int clamp(int value) {
        return Math.max(1, Math.min(99, value));
    }

    public static int maxHp(Stats s) {
        return Math.max(1, (s.con + s.siz) / 10);
    }

    public static int maxMp(Stats s) {
        return Math.max(1, s.pow / 5);
    }

    public static int mov(Stats s, int age) {
        int base;
        if (s.str > s.siz && s.dex > s.siz) {
            base = 9;
        } else if (s.str < s.siz && s.dex < s.siz) {
            base = 7;
        } else {
            base = 8;
        }
        return Math.max(1, base - ageBand(age).movPenalty());
    }

    /** 伤害加成描述，如 "0"、"+1d4"、"-2"。 */
    public static String dbText(int total) {
        if (total <= 64) return "-2";
        if (total <= 84) return "-1";
        if (total <= 124) return "0";
        if (total <= 164) return "+1d4";
        if (total <= 204) return "+1d6";
        if (total <= 284) return "+2d6";
        if (total <= 364) return "+3d6";
        if (total <= 444) return "+4d6";
        if (total <= 524) return "+5d6";
        int extra = (total - 445) / 80;
        return "+" + (5 + extra) + "d6";
    }

    public static int buildValue(int total) {
        if (total <= 64) return -2;
        if (total <= 84) return -1;
        if (total <= 124) return 0;
        if (total <= 164) return 1;
        if (total <= 204) return 2;
        if (total <= 284) return 3;
        if (total <= 364) return 4;
        if (total <= 444) return 5;
        if (total <= 524) return 6;
        return 6 + (total - 445) / 80;
    }

    public static String dbBuildText(Stats s) {
        int total = s.str + s.siz;
        return dbText(total) + " / 体格 " + buildValue(total);
    }
}
