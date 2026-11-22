package com.coc.server;

import com.coc.data.CharacterSheet;
import com.coc.net.CocNetwork;
import com.coc.net.packet.S2CRollResult;
import com.coc.rules.DiceRoller;
import com.coc.rules.Difficulty;
import com.coc.rules.SkillDef;
import com.coc.rules.SkillRegistry;
import com.coc.rules.SuccessLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** 检定服务：技能检定、骰子、对抗、孤注一掷、花费幸运、SAN 检定。 */
public final class CheckService {
    private static final long VALID_TICKS = 6000L;
    private static final Map<UUID, LastCheck> LAST = new HashMap<>();

    private CheckService() {
    }

    public static final class LastCheck {
        public String skillId;
        public String skillName;
        public int skillValue;
        public Difficulty difficulty;
        public int bonus;
        public int roll;
        public SuccessLevel level;
        public boolean success;
        public boolean fumble;
        public boolean pushed;
        public boolean sanCheck;
        public long gameTime;
        public int luckNeeded;
    }

    public static void handleRoll(ServerPlayer player, String kind, String expression, String skillId,
                                  int difficultyOrdinal, int bonus, int target) {
        if (player == null) return;
        CharacterSheet sheet = CocData.sheet(player);
        if (sheet == null) {
            player.sendSystemMessage(Component.literal("你还没有角色卡，无法检定。").withStyle(ChatFormatting.RED));
            return;
        }
        RandomSource random = player.serverLevel().random;
        String k = kind == null ? "skill" : kind;
        if ("dice".equals(k)) {
            try {
                DiceRoller.DiceRoll roll = DiceRoller.roll(expression, random);
                String detail = expression + " = " + roll.total();
                if (roll.rolls().size() > 1 || roll.modifier() != 0) {
                    detail += "  [" + roll.rolls().stream().map(String::valueOf).collect(Collectors.joining(", "))
                            + (roll.modifier() == 0 ? "" : (roll.modifier() > 0 ? " +" : " ") + roll.modifier())
                            + "]";
                }
                CompoundTag tag = baseResult("掷骰", roll.total(), -1, "—", SuccessLevel.REGULAR.color(),
                        true, detail, false, 0, false, false);
                tag.putString("kind", "dice");
                sendResult(player, tag, detail);
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("骰子表达式错误：" + e.getMessage())
                        .withStyle(ChatFormatting.RED));
            }
            return;
        }

        if ("opposed".equals(k)) {
            SkillDef def = SkillRegistry.resolve(skillId);
            if (def == null) {
                player.sendSystemMessage(Component.literal("未知技能：" + skillId).withStyle(ChatFormatting.RED));
                return;
            }
            int skill = sheet.skill(def.id);
            int npcTarget = Math.max(1, Math.min(999, target));
            DiceRoller.CheckResult mine = DiceRoller.check(skill, Difficulty.REGULAR, bonus, random);
            int npcRoll = DiceRoller.d100(random);
            SuccessLevel npcLevel = DiceRoller.evaluate(npcRoll, npcTarget);
            boolean win;
            if (mine.level().rank() != npcLevel.rank()) {
                win = mine.level().rank() > npcLevel.rank();
            } else {
                win = skill >= npcTarget;
            }
            String detail = "对方 d100=" + npcRoll + "/" + npcTarget + " → " + npcLevel.cn()
                    + "；" + (win ? "你赢下对抗" : "你输掉对抗");
            CompoundTag tag = baseResult("对抗：" + def.name, mine.percentile().roll(), skill,
                    mine.level().cn(), mine.level().color(), win, detail, false, 0,
                    mine.fumble(), mine.critical());
            tag.putString("kind", "opposed");
            sendResult(player, tag, detail);
            return;
        }

        SkillDef def = SkillRegistry.resolve(skillId);
        if (def == null) {
            player.sendSystemMessage(Component.literal("未知技能：" + skillId).withStyle(ChatFormatting.RED));
            return;
        }
        Difficulty difficulty = Difficulty.values()[Math.max(0, Math.min(2, difficultyOrdinal))];
        int skill = sheet.skill(def.id);
        DiceRoller.CheckResult check = DiceRoller.check(skill, difficulty, bonus, random);
        if (check.success()) sheet.markImprovement(def.id);

        LastCheck last = new LastCheck();
        last.skillId = def.id;
        last.skillName = def.name;
        last.skillValue = skill;
        last.difficulty = difficulty;
        last.bonus = bonus;
        last.roll = check.percentile().roll();
        last.level = check.level();
        last.success = check.success();
        last.fumble = check.fumble();
        last.gameTime = player.serverLevel().getGameTime();
        last.luckNeeded = (!check.success() && !check.fumble())
                ? Math.max(0, last.roll - difficulty.target(skill)) : 0;
        LAST.put(player.getUUID(), last);

        String detail = checkDetail(def.name, last, check.percentile());
        CompoundTag tag = baseResult(def.name + " · " + difficulty.cn(), last.roll, skill,
                check.level().cn(), check.level().color(), check.success(), detail,
                !check.success() && !check.fumble(), last.luckNeeded,
                check.fumble(), check.critical());
        tag.putString("kind", "skill");
        tag.putInt("bonus", bonus);
        sendResult(player, tag, detail);
    }

    /** KP 为玩家掷技能骰，或 KP 公开掷任意骰。结果广播给所有在线玩家。 */
    public static void rollForKeeper(ServerPlayer kp, UUID targetUuid, String kind, String skillId,
                                     int difficultyOrdinal, int bonus, String expression) {
        if (kp == null) return;
        String k = kind == null ? "dice" : kind;
        ServerPlayer target = targetUuid == null ? null
                : kp.getServer() == null ? null : kp.getServer().getPlayerList().getPlayer(targetUuid);
        RandomSource random = kp.serverLevel().random;
        if ("dice".equals(k)) {
            try {
                DiceRoller.DiceRoll roll = DiceRoller.roll(expression, random);
                String detail = expression + " = " + roll.total();
                if (roll.rolls().size() > 1 || roll.modifier() != 0) {
                    detail += "  [" + roll.rolls().stream().map(String::valueOf).collect(Collectors.joining(", "))
                            + (roll.modifier() == 0 ? "" : (roll.modifier() > 0 ? " +" : " ") + roll.modifier())
                            + "]";
                }
                String title = target == null ? "KP 公开掷骰"
                        : "KP 为 " + target.getGameProfile().getName() + " 掷骰";
                CompoundTag tag = baseResult(title, roll.total(), -1, "—", SuccessLevel.REGULAR.color(),
                        true, detail, false, 0, false, false);
                tag.putString("kind", "dice");
                broadcastPublic(kp, tag, detail);
            } catch (Exception e) {
                kp.sendSystemMessage(Component.literal("骰子表达式错误：" + e.getMessage())
                        .withStyle(ChatFormatting.RED));
            }
            return;
        }
        if (target == null) {
            kp.sendSystemMessage(Component.literal("请先在玩家页选择一名玩家。").withStyle(ChatFormatting.RED));
            return;
        }
        CharacterSheet sheet = CocData.sheet(target);
        if (sheet == null) {
            kp.sendSystemMessage(Component.literal("目标还没有角色卡。").withStyle(ChatFormatting.RED));
            return;
        }
        SkillDef def = SkillRegistry.resolve(skillId);
        if (def == null) {
            kp.sendSystemMessage(Component.literal("未知技能：" + skillId).withStyle(ChatFormatting.RED));
            return;
        }
        Difficulty difficulty = Difficulty.values()[Math.max(0, Math.min(2, difficultyOrdinal))];
        int skill = sheet.skill(def.id);
        DiceRoller.CheckResult check = DiceRoller.check(skill, difficulty, bonus, random);
        if (check.success()) sheet.markImprovement(def.id);

        LastCheck last = new LastCheck();
        last.skillId = def.id;
        last.skillName = def.name;
        last.skillValue = skill;
        last.difficulty = difficulty;
        last.bonus = bonus;
        last.roll = check.percentile().roll();
        last.level = check.level();
        last.success = check.success();
        last.fumble = check.fumble();
        last.gameTime = target.serverLevel().getGameTime();
        LAST.put(target.getUUID(), last);

        String detail = "KP 为 " + target.getGameProfile().getName() + " 掷骰；"
                + checkDetail(def.name, last, check.percentile());
        CompoundTag tag = baseResult(target.getGameProfile().getName() + " · " + def.name
                        + " · " + difficulty.cn(), last.roll, skill, check.level().cn(),
                check.level().color(), check.success(), detail, false, 0,
                check.fumble(), check.critical());
        tag.putString("kind", "skill");
        tag.putInt("bonus", bonus);
        broadcastPublic(kp, tag, detail);
        CocNetwork.syncSheet(target, sheet);
    }

    /** 道具使用等公开检定：广播掷骰结果与文字，不写入 LastCheck。 */
    public static void broadcastPublic(ServerPlayer source, CompoundTag tag, String detail) {
        if (source == null || source.getServer() == null) return;
        for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
            CocNetwork.sendToPlayer(p, new S2CRollResult(tag));
            ChatFormatting color = tag.getBoolean("success") ? ChatFormatting.GREEN : ChatFormatting.RED;
            p.sendSystemMessage(Component.literal("【" + tag.getString("title") + "】 "
                    + tag.getInt("roll")
                    + (tag.getInt("target") >= 0 ? " / " + tag.getInt("target") : "")
                    + " → " + tag.getString("level")).withStyle(color));
            if (detail != null && !detail.isBlank()) {
                p.sendSystemMessage(Component.literal(detail).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    public static void handleAction(ServerPlayer player, String action, int points) {
        if (player == null || action == null) return;
        LastCheck last = LAST.get(player.getUUID());
        if (last == null || player.serverLevel().getGameTime() - last.gameTime > VALID_TICKS) {
            player.sendSystemMessage(Component.literal("没有可用的最近检定。").withStyle(ChatFormatting.YELLOW));
            return;
        }
        CharacterSheet sheet = CocData.sheet(player);
        if (sheet == null) return;
        if ("push".equals(action)) {
            push(player, sheet, last);
        } else if ("luck".equals(action)) {
            spendLuck(player, sheet, last);
        }
    }

    private static void push(ServerPlayer player, CharacterSheet sheet, LastCheck last) {
        if (last.success) {
            player.sendSystemMessage(Component.literal("这次检定已经成功了，不需要孤注一掷。")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (last.pushed) {
            player.sendSystemMessage(Component.literal("同一次检定只能孤注一掷一次。").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (last.sanCheck) {
            player.sendSystemMessage(Component.literal("SAN 检定不能孤注一掷。").withStyle(ChatFormatting.YELLOW));
            return;
        }
        RandomSource random = player.serverLevel().random;
        DiceRoller.CheckResult check = DiceRoller.check(last.skillValue, last.difficulty, last.bonus, random);
        if (check.success()) sheet.markImprovement(last.skillId);
        last.pushed = true;
        last.roll = check.percentile().roll();
        last.level = check.level();
        last.success = check.success();
        last.fumble = check.fumble();
        last.luckNeeded = (!check.success() && !check.fumble())
                ? Math.max(0, last.roll - last.difficulty.target(last.skillValue)) : 0;
        last.gameTime = player.serverLevel().getGameTime();
        String detail = "孤注一掷：" + checkDetail(last.skillName, last, check.percentile());
        CompoundTag tag = baseResult(last.skillName + " · 孤注一掷", last.roll, last.skillValue,
                check.level().cn(), check.level().color(), check.success(), detail,
                !check.success() && !check.fumble(), last.luckNeeded,
                check.fumble(), check.critical());
        tag.putString("kind", "skill");
        sendResult(player, tag, detail);
        if (!check.success()) {
            player.sendSystemMessage(Component.literal("孤注一掷失败——KP 现在可以施加后果。")
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private static void spendLuck(ServerPlayer player, CharacterSheet sheet, LastCheck last) {
        if (last.success) {
            player.sendSystemMessage(Component.literal("这次检定已经成功了。").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (last.fumble) {
            player.sendSystemMessage(Component.literal("大失败不能用幸运补救。").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (last.sanCheck) {
            player.sendSystemMessage(Component.literal("SAN 检定不能花费幸运。").withStyle(ChatFormatting.YELLOW));
            return;
        }
        int needed = Math.max(0, last.roll - last.difficulty.target(last.skillValue));
        if (needed <= 0) {
            player.sendSystemMessage(Component.literal("不需要花费幸运。").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (sheet.stats.luck < needed) {
            player.sendSystemMessage(Component.literal(
                    "幸运不足：需要 " + needed + " 点，你只有 " + sheet.stats.luck + " 点。")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        sheet.stats.luck -= needed;
        int effectiveRoll = Math.max(1, last.roll - needed);
        SuccessLevel newLevel = DiceRoller.evaluate(effectiveRoll, last.skillValue);
        boolean success = newLevel == SuccessLevel.CRITICAL
                || newLevel.rank() >= last.difficulty.requiredRank();
        last.success = success;
        last.level = newLevel;
        last.roll = effectiveRoll;
        last.luckNeeded = 0;
        last.gameTime = player.serverLevel().getGameTime();
        if (success) sheet.markImprovement(last.skillId);
        String detail = "花费 " + needed + " 点幸运，检定结果变为 " + effectiveRoll + " → " + newLevel.cn();
        CompoundTag tag = baseResult(last.skillName + " · 花费幸运", effectiveRoll, last.skillValue,
                newLevel.cn(), newLevel.color(), success, detail, false, 0,
                false, newLevel == SuccessLevel.CRITICAL);
        tag.putString("kind", "skill");
        sendResult(player, tag, detail);
        CocNetwork.syncSheet(player, sheet);
    }

    public static void sanCheck(ServerPlayer player, String successLoss, String failLoss) {
        if (player == null) return;
        CharacterSheet sheet = CocData.sheet(player);
        if (sheet == null) {
            player.sendSystemMessage(Component.literal("目标还没有角色卡。").withStyle(ChatFormatting.RED));
            return;
        }
        RandomSource random = player.serverLevel().random;
        int san = sheet.san;
        DiceRoller.CheckResult check = DiceRoller.check(san, Difficulty.REGULAR, 0, random);
        int loss;
        try {
            loss = Math.abs(DiceRoller.evalNumber(check.success() ? successLoss : failLoss, random));
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("SAN 损失表达式错误：" + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return;
        }
        String detail = "SAN 检定 " + (check.success() ? "成功" : "失败") + "，损失 " + loss + " 点";
        CompoundTag tag = baseResult("SAN 检定", check.percentile().roll(), san,
                check.level().cn(), check.level().color(), check.success(), detail,
                false, 0, check.fumble(), check.critical());
        tag.putString("kind", "san");
        tag.putInt("loss", loss);
        broadcastPublic(player, tag, detail);
        SanityService.loseSan(player, sheet, loss, "SAN 检定", random);
    }

    private static String checkDetail(String name, LastCheck last, DiceRoller.PercentileRoll p) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" d100=").append(p.roll()).append(" / ").append(last.difficulty.target(last.skillValue))
                .append("（技能 ").append(last.skillValue).append("）");
        if (p.bonus() != 0) {
            sb.append("，").append(p.bonus() > 0 ? "奖励骰 +" : "惩罚骰 -").append(Math.abs(p.bonus()))
                    .append(" 十位 [")
                    .append(p.tensDice().stream().map(String::valueOf).collect(Collectors.joining(", ")))
                    .append("]");
        }
        return sb.toString();
    }

    private static CompoundTag baseResult(String title, int roll, int target, String level, int levelColor,
                                          boolean success, String detail, boolean canPush, int luckNeeded,
                                          boolean fumble, boolean critical) {
        CompoundTag tag = new CompoundTag();
        tag.putString("title", title == null ? "" : title);
        tag.putInt("roll", roll);
        tag.putInt("target", target);
        tag.putString("level", level == null ? "" : level);
        tag.putInt("level_color", levelColor);
        tag.putBoolean("success", success);
        tag.putString("detail", detail == null ? "" : detail);
        tag.putBoolean("can_push", canPush);
        tag.putInt("luck_needed", luckNeeded);
        tag.putBoolean("fumble", fumble);
        tag.putBoolean("critical", critical);
        tag.putBoolean("can_luck", !success && !fumble && luckNeeded > 0);
        return tag;
    }

    private static void sendResult(ServerPlayer player, CompoundTag tag, String detail) {
        CocNetwork.sendToPlayer(player, new S2CRollResult(tag));
        ChatFormatting color = tag.getBoolean("success") ? ChatFormatting.GREEN : ChatFormatting.RED;
        player.sendSystemMessage(Component.literal("【" + tag.getString("title") + "】 "
                + tag.getInt("roll")
                + (tag.getInt("target") >= 0 ? " / " + tag.getInt("target") : "")
                + " → " + tag.getString("level")).withStyle(color));
        if (detail != null && !detail.isBlank()) {
            player.sendSystemMessage(Component.literal(detail).withStyle(ChatFormatting.GRAY));
        }
    }

    public static LastCheck lastCheck(ServerPlayer player) {
        return player == null ? null : LAST.get(player.getUUID());
    }

    public static void clear(ServerPlayer player) {
        if (player != null) LAST.remove(player.getUUID());
    }
}
