package com.coc.net;

import com.coc.events.CocEventService;
import com.coc.kp.KpActions;
import com.coc.kp.KpManager;
import com.coc.net.packet.C2SCreateCharacter;
import com.coc.server.CharacterService;
import com.coc.server.CheckService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** 所有 C2S 包的服务端处理入口。 */
public final class ServerPacketHandler {
    private ServerPacketHandler() {
    }

    public static void requestSync(ServerPlayer player) {
        CharacterService.requestSync(player);
    }

    public static void createCharacter(ServerPlayer player, CompoundTag data) {
        CharacterService.create(player, data);
    }

    public static void roll(ServerPlayer player, String kind, String expression, String skillId,
                            int difficulty, int bonus, int target) {
        if (player == null) return;
        if (!KpManager.isKp(player)) {
            player.sendSystemMessage(Component.literal("掷骰由 KP 进行；请向 KP 说明你要做什么。")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
            return;
        }
        CheckService.handleRoll(player, kind, expression, skillId, difficulty, bonus, target);
    }

    public static void checkAction(ServerPlayer player, String action, int points) {
        if (player == null) return;
        if (!KpManager.isKp(player)) {
            player.sendSystemMessage(Component.literal("孤注一掷/花费幸运由 KP 处理。")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
            return;
        }
        CheckService.handleAction(player, action, points);
    }

    public static void kpRoll(ServerPlayer player, com.coc.net.packet.C2SKpRoll packet) {
        if (player == null || packet == null) return;
        if (!KpManager.isKp(player)) {
            player.sendSystemMessage(Component.literal("只有 KP 可以掷骰。")
                    .withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }
        CheckService.rollForKeeper(player, packet.target, packet.kind, packet.skillId,
                packet.difficulty, packet.bonus, packet.expression);
    }

    public static void eventChoice(ServerPlayer player, int index) {
        CocEventService.choose(player, index);
    }

    public static void kpAction(ServerPlayer player, String action, UUID target, String text, int value) {
        KpActions.handle(player, action, target, text, value);
    }

    public static void importScenario(ServerPlayer player, String json) {
        com.coc.server.ScenarioImportService.importJson(player, json);
    }

    public static void globalSettings(ServerPlayer player, com.coc.net.packet.C2SGlobalSettings packet) {
        if (player == null || packet == null) return;
        if (!KpManager.isKp(player)) {
            player.sendSystemMessage(Component.literal("只有 KP 可以修改全局设置。")
                    .withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }
        com.coc.server.GlobalSettingsService.save(player, packet.allowPoints,
                packet.occupationPoints, packet.interestPoints, packet.totalPoints);
    }

    public static void exportSheet(ServerPlayer player, UUID target) {
        com.coc.server.CharacterSheetJsonService.export(player, target);
    }

    public static void importSheet(ServerPlayer player, UUID target, String json) {
        com.coc.server.CharacterSheetJsonService.importJson(player, target, json);
    }

    public static void itemRule(ServerPlayer player, com.coc.net.packet.C2SItemRule packet) {
        if (player == null || packet == null) return;
        if (!KpManager.isKp(player)) return;
        // ItemLibrary will be extended with the custom name.
        boolean ok = com.coc.server.ItemLibrary.upsert(packet.itemId, packet.skillId, packet.target, packet.name);
        if (ok) {
            player.sendSystemMessage(Component.literal("已保存道具规则：" + packet.itemId)
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
        } else {
            player.sendSystemMessage(Component.literal("保存道具规则失败：物品或技能无效。")
                    .withStyle(net.minecraft.ChatFormatting.RED));
        }
        com.coc.kp.KpDataBuilder.send(player);
    }
}
