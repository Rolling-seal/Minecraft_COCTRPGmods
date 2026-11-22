package com.coc.server;

import com.coc.data.CharacterSheet;
import com.coc.data.CocWorldData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** 世界数据读取的便利入口。 */
public final class CocData {
    private CocData() {
    }

    public static CocWorldData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                CocWorldData::load, CocWorldData::new, CocWorldData.DATA_NAME);
    }

    public static CharacterSheet sheet(ServerPlayer player) {
        if (player == null || player.getServer() == null) return null;
        return get(player.getServer()).getSheet(player.getUUID());
    }

    public static ServerPlayer online(MinecraftServer server, UUID uuid) {
        return server.getPlayerList().getPlayer(uuid);
    }

    public static long gameTime(ServerPlayer player) {
        return player.serverLevel().getGameTime();
    }

    public static long day(ServerPlayer player) {
        return gameTime(player) / 24000L;
    }
}
