package com.coc.server;

import com.coc.config.CocServerConfig;
import com.coc.net.CocNetwork;
import com.coc.net.packet.S2CGlobalSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** 全局角色创建设置的保存与同步。 */
public final class GlobalSettingsService {
    private GlobalSettingsService() {
    }

    public static void send(ServerPlayer player) {
        if (player == null) return;
        CocServerConfig cfg = CocServerConfig.get();
        CocNetwork.sendToPlayer(player, new S2CGlobalSettings(
                cfg.allowCreationPoints, cfg.creationOccupationPoints,
                cfg.creationInterestPoints, cfg.creationTotalPoints));
    }

    public static void broadcast(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player);
    }

    public static boolean save(ServerPlayer kp, boolean allow, int occupation, int interest, int total) {
        if (kp == null) return false;
        CocServerConfig cfg = CocServerConfig.get();
        cfg.allowCreationPoints = allow;
        cfg.creationOccupationPoints = clampOverride(occupation, 999);
        cfg.creationInterestPoints = clampOverride(interest, 999);
        cfg.creationTotalPoints = clampOverride(total, 9999);
        CocServerConfig.save();
        broadcast(kp.getServer());
        kp.sendSystemMessage(Component.literal("全局角色创建设置已保存。").withStyle(ChatFormatting.AQUA));
        return true;
    }

    private static int clampOverride(int value, int max) {
        if (value < 0) return -1;
        return Math.min(max, value);
    }
}
