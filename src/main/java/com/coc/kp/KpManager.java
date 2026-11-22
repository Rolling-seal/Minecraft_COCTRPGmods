package com.coc.kp;

import com.coc.config.CocServerConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** KP（守秘人）权限：单人主机、OP≥2、配置名单。 */
public final class KpManager {
    private KpManager() {
    }

    public static boolean isKp(ServerPlayer player) {
        if (player == null) return false;
        MinecraftServer server = player.getServer();
        if (server != null && server.isSingleplayer()) {
            var profile = server.getSingleplayerProfile();
            if (profile != null && player.getUUID().equals(profile.getId())) return true;
        }
        if (player.hasPermissions(2)) return true;
        String name = player.getGameProfile().getName();
        for (String kp : CocServerConfig.get().kpNames) {
            if (kp != null && kp.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public static boolean isKp(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) return isKp(player);
        return source.hasPermission(2);
    }
}
