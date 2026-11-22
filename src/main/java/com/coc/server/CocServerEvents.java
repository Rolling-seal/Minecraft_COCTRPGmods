package com.coc.server;

import com.coc.CallOfCthulhu;
import com.coc.command.CocCommands;
import com.coc.config.CocServerConfig;
import com.coc.events.CocEventService;
import com.coc.events.EventRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CallOfCthulhu.MODID)
public final class CocServerEvents {
    private CocServerEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CocCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        CocServerConfig.load();
        EventRegistry.reload();
        CallOfCthulhu.LOGGER.info("COC 服务端就绪：{} 个事件，{} 个剧本",
                EventRegistry.events().size(), EventRegistry.scenarios().size());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        CocServerConfig.save();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % 20 != 0) return;
        // 剧本团只由 KP 推幕；不再自动调度随机事件/事件卡。
        SanityService.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CharacterService.requestSync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CocEventService.clear(player);
            CheckService.clear(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CharacterService.requestSync(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CharacterService.requestSync(player);
        }
    }
}
