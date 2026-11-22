package com.coc.rules;

import net.minecraft.util.RandomSource;

/** SAN 规则的纯数据与判定；具体效果由服务端 SanityService 应用。 */
public final class SanityRules {
    public static final String[] BOUTS = {
            "失忆：你短暂地忘记了自己是谁、做过什么。",
            "假性残疾：你的身体突然不听使唤，仿佛瘫痪。",
            "暴力倾向：愤怒从胃里升起，你必须砸碎些什么。",
            "偏执：每一道视线都像在窥探你的秘密。",
            "重要之人：你坚信某个对你重要的人就在附近，必须去找他。",
            "昏厥：世界在你眼前迅速缩成一个黑点。",
            "逃跑：你只想离开这里，越快越好。",
            "歇斯底里：笑声和哭声同时从你喉咙里涌出来。",
            "恐惧症：某种东西深深钻进你的骨头，从此无法摆脱。",
            "狂躁症：你被一种莫名的亢奋攫住，停不下来。"
    };

    public static final String[] PHOBIAS = {
            "黑暗恐惧症", "幽闭恐惧症", "深海恐惧症", "尸体恐惧症", "蜘蛛恐惧症",
            "血液恐惧症", "镜子恐惧症", "人群恐惧症", "高处恐惧症", "火焰恐惧症",
            "动物恐惧症", "声音恐惧症", "雾气恐惧症", "书籍恐惧症", "睡眠恐惧症",
            "眼睛恐惧症", "陈旧物品恐惧症", "雨夜恐惧症", "钟表恐惧症", "门缝恐惧症"
    };

    public static final String[] MANIAS = {
            "纵火癖", "偷窃癖", "说谎癖", "暴食癖", "洁癖",
            "收藏癖", "计数癖", "重复仪式癖", "自残癖", "窥视癖",
            "写日记癖", "祈祷癖", "破坏癖", "赌博癖", "囤积癖",
            "磨牙癖", "跟踪癖", "洗刷癖", "低声自语癖", "夜游癖"
    };

    private SanityRules() {
    }

    public static int maxSan(int mythos) {
        return Math.max(0, 99 - Math.max(0, mythos));
    }

    public static boolean causesTemporaryInsanity(int loss) {
        return loss >= 5;
    }

    public static boolean causesIndefiniteInsanity(int dailyLoss, int sanBefore) {
        return dailyLoss >= Math.max(1, sanBefore / 5);
    }

    public static int insanityHours(RandomSource random) {
        return 1 + random.nextInt(10);
    }

    public static String rollBout(RandomSource random) {
        return BOUTS[random.nextInt(BOUTS.length)];
    }

    public static String rollPhobia(RandomSource random) {
        return PHOBIAS[random.nextInt(PHOBIAS.length)];
    }

    public static String rollMania(RandomSource random) {
        return MANIAS[random.nextInt(MANIAS.length)];
    }

    public static int sanColor(int san, int maxSan) {
        double ratio = maxSan <= 0 ? 0 : (double) san / maxSan;
        if (ratio > 0.6) return 0xFF3FA34D;
        if (ratio > 0.3) return 0xFFD4A017;
        if (ratio > 0.15) return 0xFFD2691E;
        return 0xFFB22222;
    }
}
