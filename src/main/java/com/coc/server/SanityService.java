package com.coc.server;

import com.coc.data.CharacterSheet;
import com.coc.net.CocNetwork;
import com.coc.rules.DiceRoller;
import com.coc.rules.SanityRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/** SAN 损失、临时/不定期疯狂、SAN 恢复与状态清理。 */
public final class SanityService {
    private SanityService() {
    }

    public static void loseSan(ServerPlayer player, CharacterSheet sheet, int amount,
                               String reason, RandomSource random) {
        if (player == null || sheet == null || amount <= 0) return;
        long gameTime = player.serverLevel().getGameTime();
        long day = gameTime / 24000L;
        if (sheet.lastSanDay != day) {
            sheet.lastSanDay = day;
            sheet.dailySanLoss = 0;
        }
        int before = sheet.san;
        sheet.san = Math.max(0, before - amount);
        int actual = before - sheet.san;
        sheet.dailySanLoss += actual;
        player.sendSystemMessage(Component.literal(
                "【SAN】-" + actual + "（剩余 " + sheet.san + "/" + sheet.maxSan + "）"
                        + (reason == null || reason.isBlank() ? "" : " · " + reason))
                .withStyle(ChatFormatting.DARK_PURPLE));

        if (sheet.san <= 0) {
            sheet.retired = true;
            sheet.indefiniteInsanity = true;
            sheet.tempInsanityEnd = 0;
            sheet.tempInsanity = "";
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 240, 0));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 0));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 1));
            broadcast(player.getServer(), "☠ " + sheet.name + " 的精神彻底崩溃了（SAN 归零）。");
            CocNetwork.syncSheet(player, sheet);
            return;
        }

        if (actual >= 5 && !sheet.retired) {
            int intRoll = DiceRoller.d100(random);
            boolean realized = intRoll <= sheet.stats.intel;
            int hours = SanityRules.insanityHours(random);
            long end = gameTime + hours * 1000L;
            sheet.tempInsanityEnd = Math.max(sheet.tempInsanityEnd, end);
            int idx = random.nextInt(SanityRules.BOUTS.length);
            String bout = SanityRules.BOUTS[idx];
            sheet.tempInsanity = bout;
            applyBout(player, sheet, idx, random);
            player.sendSystemMessage(Component.literal(
                    "【临时性疯狂】" + bout + " 持续约 " + hours + " 小时。INT 检定 "
                            + (realized ? "成功：你清楚地意识到自己发生了什么。" : "失败：你把这段记忆压进了潜意识。"))
                    .withStyle(ChatFormatting.RED));
        }

        if (!sheet.indefiniteInsanity
                && SanityRules.causesIndefiniteInsanity(sheet.dailySanLoss, before)) {
            sheet.indefiniteInsanity = true;
            String phobia = SanityRules.rollPhobia(random);
            String mania = SanityRules.rollMania(random);
            addPhobia(sheet, phobia);
            addMania(sheet, mania);
            player.sendSystemMessage(Component.literal(
                    "【不定期疯狂】单日 SAN 损失达到阈值。获得恐惧症「" + phobia + "」与狂躁症「" + mania + "」。")
                    .withStyle(ChatFormatting.DARK_RED));
        }
        CocNetwork.syncSheet(player, sheet);
    }

    public static void gainSan(ServerPlayer player, CharacterSheet sheet, int amount) {
        if (player == null || sheet == null || amount <= 0) return;
        int before = sheet.san;
        sheet.san = Math.min(sheet.maxSan, sheet.san + amount);
        if (sheet.san > 0 && sheet.retired) {
            sheet.retired = false;
        }
        player.sendSystemMessage(Component.literal(
                "【SAN】+" + (sheet.san - before) + "（剩余 " + sheet.san + "/" + sheet.maxSan + "）")
                .withStyle(ChatFormatting.GREEN));
        CocNetwork.syncSheet(player, sheet);
    }

    public static void setSan(ServerPlayer player, CharacterSheet sheet, int value) {
        if (player == null || sheet == null) return;
        sheet.san = Math.max(0, Math.min(sheet.maxSan, value));
        if (sheet.san > 0) sheet.retired = false;
        else {
            sheet.retired = true;
            sheet.indefiniteInsanity = true;
        }
        CocNetwork.syncSheet(player, sheet);
    }

    /** KP 直接让目标进入临时疯狂，不扣 SAN。 */
    public static void forceTemporaryInsanity(ServerPlayer player, CharacterSheet sheet) {
        if (player == null || sheet == null) return;
        RandomSource random = player.serverLevel().random;
        long gameTime = player.serverLevel().getGameTime();
        int hours = SanityRules.insanityHours(random);
        long end = gameTime + hours * 1000L;
        sheet.tempInsanityEnd = Math.max(sheet.tempInsanityEnd, end);
        int idx = random.nextInt(SanityRules.BOUTS.length);
        String bout = SanityRules.BOUTS[idx];
        sheet.tempInsanity = bout;
        applyBout(player, sheet, idx, random);
        player.sendSystemMessage(Component.literal(
                "【KP】你被推入临时性疯狂：" + bout + "，持续约 " + hours + " 小时。")
                .withStyle(ChatFormatting.DARK_RED));
        broadcast(player.getServer(), "☠ " + sheet.name + " 陷入了临时性疯狂（KP 裁定）。");
        CocNetwork.syncSheet(player, sheet);
    }

    public static void clearInsanity(ServerPlayer player, CharacterSheet sheet) {
        if (player == null || sheet == null) return;
        sheet.tempInsanityEnd = 0;
        sheet.tempInsanity = "";
        sheet.indefiniteInsanity = false;
        if (sheet.san > 0) sheet.retired = false;
        player.sendSystemMessage(Component.literal("【KP】疯狂状态已解除。")
                .withStyle(ChatFormatting.AQUA));
        CocNetwork.syncSheet(player, sheet);
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CharacterSheet sheet = CocData.sheet(player);
            if (sheet == null) continue;
            long now = player.serverLevel().getGameTime();
            if (sheet.tempInsanityEnd > 0 && now >= sheet.tempInsanityEnd) {
                sheet.tempInsanityEnd = 0;
                sheet.tempInsanity = "";
                player.sendSystemMessage(Component.literal("你从疯狂中缓过神来，但记忆里留下了一道裂痕。")
                        .withStyle(ChatFormatting.GRAY));
                CocNetwork.syncSheet(player, sheet);
            }
        }
    }

    private static void applyBout(ServerPlayer player, CharacterSheet sheet, int idx, RandomSource random) {
        switch (idx) {
            case 0 -> player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            case 1 -> {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 0));
            }
            case 2 -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0));
            case 3 -> player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 0));
            case 4 -> player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            case 5 -> {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
            case 6 -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
            case 7 -> {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 0));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
            }
            case 8 -> {
                addPhobia(sheet, SanityRules.PHOBIAS[random.nextInt(SanityRules.PHOBIAS.length)]);
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0));
            }
            case 9 -> {
                addMania(sheet, SanityRules.MANIAS[random.nextInt(SanityRules.MANIAS.length)]);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
            }
            default -> {
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
    }

    public static void addPhobia(CharacterSheet sheet, String phobia) {
        if (phobia != null && !phobia.isBlank() && !sheet.phobias.contains(phobia)) {
            sheet.phobias.add(phobia);
        }
    }

    public static void addMania(CharacterSheet sheet, String mania) {
        if (mania != null && !mania.isBlank() && !sheet.manias.contains(mania)) {
            sheet.manias.add(mania);
        }
    }

    public static void broadcast(MinecraftServer server, String text) {
        if (server == null) return;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.DARK_RED));
        }
    }
}
