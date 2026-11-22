package com.coc.server;

import com.coc.events.EventDef;
import com.coc.events.EventRegistry;
import com.coc.events.ScenarioAct;
import com.coc.events.ScenarioDef;
import com.coc.rules.CharacterRules;
import com.coc.rules.DiceRoller;
import com.coc.rules.SanityRules;
import com.coc.rules.SkillRegistry;
import com.coc.rules.Stats;
import com.coc.rules.SuccessLevel;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 无玩家依赖的规则自检；专用服务器控制台可运行 /coc debug selftest。 */
public final class SelfTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> EFFECTS = Set.of(
            "san", "hp", "mp", "luck", "skill", "mythos", "item", "effect",
            "sound", "message", "handout", "broadcast", "spawn", "phobia", "mania"
    );

    private SelfTest() {
    }

    public static boolean run(CommandSourceStack source) {
        RandomSource random = RandomSource.create(20261118L);
        List<String> failures = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        for (int i = 0; i < 20000; i++) {
            int r = DiceRoller.d100(random);
            if (r < 1 || r > 100) {
                failures.add("d100 越界: " + r);
                break;
            }
        }
        for (int bonus = -2; bonus <= 2; bonus++) {
            for (int i = 0; i < 5000; i++) {
                int r = DiceRoller.percentile(bonus, random).roll();
                if (r < 1 || r > 100) {
                    failures.add("百分骰越界 bonus=" + bonus + ": " + r);
                    break;
                }
            }
        }
        if (DiceRoller.evaluate(1, 50) != SuccessLevel.CRITICAL) failures.add("01 不是大成功");
        if (DiceRoller.evaluate(100, 50) != SuccessLevel.FUMBLE) failures.add("100 不是大失败");
        if (DiceRoller.evaluate(96, 49) != SuccessLevel.FUMBLE) failures.add("技能<50 时 96 不是大失败");
        if (DiceRoller.evaluate(96, 50) == SuccessLevel.FUMBLE) failures.add("技能>=50 时 96 被误判为大失败");
        if (DiceRoller.evalNumber("-1d6", random) >= 0) failures.add("-1d6 没有返回负数");

        for (int age : new int[]{15, 25, 45, 65, 85}) {
            for (int i = 0; i < 200; i++) {
                Stats base = CharacterRules.rollBaseStats(random);
                Stats s = CharacterRules.applyAge(base, age, random, true);
                for (int v : new int[]{s.str, s.con, s.siz, s.dex, s.app, s.intel, s.pow, s.edu, s.luck}) {
                    if (v < 1 || v > 99) {
                        failures.add("年龄 " + age + " 属性越界: " + v);
                        break;
                    }
                }
                if (CharacterRules.maxHp(s) < 1 || CharacterRules.maxMp(s) < 1) {
                    failures.add("衍生值异常");
                }
            }
        }
        int lastBuild = -999;
        for (int total = 0; total <= 800; total += 10) {
            int build = CharacterRules.buildValue(total);
            if (build < lastBuild) failures.add("体格表不单调 total=" + total);
            lastBuild = build;
        }
        if (!"-2".equals(CharacterRules.dbText(50))) failures.add("伤害加成表 50 应为 -2");
        if (!"0".equals(CharacterRules.dbText(100))) failures.add("伤害加成表 100 应为 0");
        if (!"+1d4".equals(CharacterRules.dbText(150))) failures.add("伤害加成表 150 应为 +1d4");

        if (SanityRules.maxSan(0) != 99) failures.add("最大 SAN 计算错误");
        if (SanityRules.maxSan(10) != 89) failures.add("神话 10 时最大 SAN 应为 89");
        if (SanityRules.maxSan(99) != 0) failures.add("神话 99 时最大 SAN 应为 0");
        if (!SanityRules.causesTemporaryInsanity(5)) failures.add("单次损失 5 未触发临时疯狂");
        if (SanityRules.causesTemporaryInsanity(4)) failures.add("单次损失 4 误触发临时疯狂");

        if (BuiltInRegistries.ITEM.get(new ResourceLocation("coc", "kp_panel")) == Items.AIR) {
            failures.add("KP面板物品未注册");
        }
        if (source != null && source.getServer() != null
                && source.getServer().getRecipeManager()
                .byKey(new ResourceLocation("coc", "keeper_panel")).isEmpty()) {
            failures.add("KP面板配方未加载（coc:keeper_panel）");
        }

        EventRegistry.reload();
        int eventCount = EventRegistry.events().size();
        int scenarioCount = EventRegistry.scenarios().size();
        if (eventCount == 0) failures.add("没有加载到事件");
        if (scenarioCount == 0) failures.add("没有加载到剧本");
        for (EventDef def : EventRegistry.events()) {
            if (def.options == null || def.options.isEmpty()) {
                failures.add("事件无选项: " + def.id);
                continue;
            }
            for (EventDef.Option option : def.options) {
                if (option.skill != null && !option.skill.isBlank()
                        && SkillRegistry.resolve(option.skill) == null) {
                    failures.add("事件 " + def.id + " 技能不存在: " + option.skill);
                }
                checkEffects(def.id, option.effects, failures);
                if (option.success != null) checkEffects(def.id, option.success.effects, failures);
                if (option.failure != null) checkEffects(def.id, option.failure.effects, failures);
                if (option.outcome != null) checkEffects(def.id, option.outcome.effects, failures);
            }
        }
        for (ScenarioDef def : EventRegistry.scenarios()) {
            for (String eventId : def.events) {
                if (EventRegistry.event(eventId) == null) {
                    failures.add("剧本 " + def.id + " 引用不存在的事件: " + eventId);
                }
            }
            if (def.acts == null || def.acts.isEmpty()) {
                failures.add("剧本 " + def.id + " 没有幕次元数据");
                continue;
            }
            if (def.acts.size() != def.events.size()) {
                failures.add("剧本 " + def.id + " 幕次数与事件数不一致");
            }
            for (ScenarioAct act : def.acts) {
                if (EventRegistry.event(act.event) == null) {
                    failures.add("剧本 " + def.id + " 第 " + act.act + " 幕事件不存在: " + act.event);
                } else if (act.act >= 1 && act.act <= def.events.size()
                        && !def.events.get(act.act - 1).equals(act.event)) {
                    failures.add("剧本 " + def.id + " 第 " + act.act + " 幕事件顺序不一致");
                }
                if (act.sideEvents != null) {
                    for (String side : act.sideEvents) {
                        if (EventRegistry.event(side) == null) {
                            failures.add("剧本 " + def.id + " 第 " + act.act + " 幕支线不存在: " + side);
                        }
                    }
                }
            }
        }
        if (!EventRegistry.errors().isEmpty()) {
            notes.add("事件加载警告 " + EventRegistry.errors().size() + " 条（见日志/ KP 面板）");
        }

        String result = failures.isEmpty()
                ? "SELFTEST OK · 事件 " + eventCount + " · 剧本 " + scenarioCount
                : "SELFTEST FAILED · " + failures.size() + " 项";
        LOGGER.info(result);
        for (String failure : failures) LOGGER.error("SELFTEST: {}", failure);
        if (source != null) {
            source.sendSuccess(() -> Component.literal(result), false);
            for (String failure : failures) {
                source.sendFailure(Component.literal("SELFTEST: " + failure));
            }
            for (String note : notes) {
                source.sendSuccess(() -> Component.literal(note), false);
            }
        }
        return failures.isEmpty();
    }

    private static void checkEffects(String eventId, List<EventDef.Effect> effects, List<String> failures) {
        if (effects == null) return;
        for (EventDef.Effect effect : effects) {
            if (effect == null || effect.type == null || !EFFECTS.contains(effect.type)) {
                failures.add("事件 " + eventId + " 未知效果: " + (effect == null ? "null" : effect.type));
            }
        }
    }
}
