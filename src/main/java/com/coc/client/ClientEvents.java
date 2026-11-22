package com.coc.client;

import com.coc.CallOfCthulhu;
import com.coc.client.fx.RollOverlay;
import com.coc.client.fx.SanityEffects;
import com.coc.client.fx.SanityHud;
import com.coc.client.ui.CocUi;
import com.coc.client.screen.CharacterCreationScreen;
import com.coc.client.screen.CharacterSheetScreen;
import com.coc.client.screen.JournalScreen;
import com.coc.client.screen.KeeperScreen;
import com.coc.client.screen.SettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 客户端 Forge 事件：按键、HUD、镜头、低 SAN 特效、暂停菜单入口。 */
@Mod.EventBusSubscriber(modid = CallOfCthulhu.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            SanityEffects.reset();
            return;
        }
        if (mc.screen == null) {
            while (ClientSetup.SHEET_KEY.consumeClick()) openSheet(mc);
            while (ClientSetup.KP_KEY.consumeClick()) mc.setScreen(new KeeperScreen());
            while (ClientSetup.JOURNAL_KEY.consumeClick()) mc.setScreen(new JournalScreen());
        }
        if (ClientCache.creationPromptTicks > 0) {
            ClientCache.creationPromptTicks--;
        } else if (ClientCache.creationPromptTicks == 0) {
            ClientCache.creationPromptTicks = -1;
            if (mc.screen == null && ClientCache.syncReceived && !ClientCache.selfHasSheet) {
                mc.setScreen(new CharacterCreationScreen());
            }
        }
        SanityEffects.tick();
    }

    private static void openSheet(Minecraft mc) {
        if (mc.player == null) return;
        var uuid = mc.player.getUUID();
        if (ClientCache.sheet(uuid) != null) {
            mc.setScreen(new CharacterSheetScreen(uuid, false));
            return;
        }
        ClientPacketHandler.requestSyncIfNeeded();
        if (ClientCache.syncReceived) {
            mc.setScreen(new CharacterCreationScreen());
        } else {
            mc.player.displayClientMessage(Component.literal(CocLang.tr("正在同步角色卡……")), true);
        }
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSystemChat(ClientChatReceivedEvent event) {
        if (!(event instanceof ClientChatReceivedEvent.System)) return;
        String original = event.getMessage().getString();
        String translated = CocLang.message(original);
        if (!translated.equals(original)) {
            event.setMessage(Component.literal(translated).withStyle(event.getMessage().getStyle()));
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCache.clear();
        SanityEffects.reset();
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        SanityEffects.onCameraAngles(event);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        SanityEffects.renderVignette(event.getGuiGraphics());
        SanityHud.render(event.getGuiGraphics());
        RollOverlay.render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        CocUi.styleWidgets(event.getScreen(), event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen)) return;
        Button button = Button.builder(Component.literal(CocLang.tr("克苏鲁设置")), b -> {
            if (Minecraft.getInstance() != null) {
                Minecraft.getInstance().setScreen(new SettingsScreen(event.getScreen()));
            }
        }).bounds(6, 6, 90, 20).build();
        event.addListener(button);
    }
}
