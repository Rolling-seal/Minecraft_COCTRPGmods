package com.coc.client;

import com.coc.client.screen.CharacterCreationScreen;
import com.coc.client.screen.CharacterSheetScreen;
import com.coc.client.screen.CurtainScreen;
import com.coc.client.screen.EventScreen;
import com.coc.client.GlobalSettingsView;
import com.coc.client.screen.GlobalSettingsScreen;
import com.coc.client.screen.ImportScenarioScreen;
import com.coc.client.screen.KeeperGuideScreen;
import com.coc.client.screen.SheetFileScreen;
import com.coc.client.screen.ItemPickerScreen;
import com.coc.client.screen.JournalScreen;
import com.coc.client.screen.KeeperScreen;
import com.coc.client.screen.SettingsScreen;
import com.coc.client.screen.StoryBookScreen;
import com.coc.data.CharacterSheet;
import com.coc.data.Handout;
import com.coc.net.CocNetwork;
import com.coc.net.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** 客户端 S2C 包处理。所有界面切换都在客户端主线程执行。 */
public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void syncSheet(S2CSyncSheet packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (packet.hasSheet && packet.data != null) {
                CharacterSheet sheet = CharacterSheet.fromNbt(packet.data);
                ClientCache.SHEETS.put(packet.target, sheet);
                if (packet.self) {
                    ClientCache.selfUuid = packet.target;
                    ClientCache.selfHasSheet = true;
                    ClientCache.syncReceived = true;
                    ClientCache.creationPromptTicks = -1;
                }
            } else {
                ClientCache.SHEETS.remove(packet.target);
                if (packet.self) {
                    ClientCache.selfHasSheet = false;
                    ClientCache.syncReceived = true;
                    if (ClientCache.creationPromptTicks < 0) ClientCache.creationPromptTicks = 60;
                }
            }
            if (mc.screen instanceof CharacterSheetScreen screen && screen.target().equals(packet.target)) {
                screen.refresh();
            }
            if (packet.self && packet.hasSheet && mc.screen instanceof CharacterCreationScreen) {
                mc.setScreen(new CharacterSheetScreen(packet.target, false));
            }
        });
    }

    public static void rollResult(S2CRollResult packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            ClientCache.lastRoll = RollView.fromNbt(packet.data);
        });
    }

    public static void eventOffer(S2CEventOffer packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            PendingEventView view = PendingEventView.fromNbt(packet.data);
            ClientCache.pendingEvent = view;
            mc.setScreen(new EventScreen(view));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, 1.0F, 0.8F));
        });
    }

    public static void eventResult(S2CEventResult packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.screen instanceof EventScreen screen) {
                screen.onResult(packet.data);
            } else if (mc.player != null) {
                String text = packet.data.getString("text");
                mc.player.displayClientMessage(Component.literal(CocLang.tr("【事件】") + CocLang.message(text)), false);
            }
        });
    }

    public static void kpData(S2CKpData packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            ClientCache.kpView = KpView.fromNbt(packet.data);
            if (mc.screen instanceof KeeperScreen screen) screen.refresh();
            else if (mc.screen instanceof ItemPickerScreen screen) screen.refresh();
        });
    }

    public static void curtain(S2CActCurtain packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            ClientCache.curtain = CurtainView.fromNbt(packet.data);
            mc.setScreen(new CurtainScreen(ClientCache.curtain));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, 1.0F, 0.7F));
        });
    }

    public static void itemList(S2CItemList packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            ClientCache.items = ItemView.fromNbt(packet.data);
            if (mc.screen instanceof KeeperScreen screen) screen.refresh();
            else if (mc.screen instanceof ItemPickerScreen screen) screen.refresh();
        });
    }

    public static void bookData(S2CBookData packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            ClientCache.book = BookView.fromNbt(packet.data);
            if (mc.screen instanceof StoryBookScreen screen) screen.refresh();
        });
    }

    public static void importResult(S2CImportResult packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.screen instanceof ImportScenarioScreen screen) {
                screen.onResult(packet);
            } else if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(CocLang.tr("【剧本导入】") + CocLang.message(packet.message)),
                        false);
            }
        });
    }

    public static void globalSettings(S2CGlobalSettings packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            GlobalSettingsView view = new GlobalSettingsView();
            view.allowPoints = packet.allowPoints;
            view.occupationPoints = packet.occupationPoints;
            view.interestPoints = packet.interestPoints;
            view.totalPoints = packet.totalPoints;
            ClientCache.globalSettings = view;
            if (mc.screen instanceof GlobalSettingsScreen screen) screen.onSettings(view);
        });
    }

    public static void sheetJson(S2CSheetJson packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.screen instanceof SheetFileScreen screen) screen.onExport(packet);
            else if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("【角色卡】导出数据已收到，但当前没有文件界面。"), false);
            }
        });
    }

    public static void sheetResult(S2CSheetResult packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.screen instanceof SheetFileScreen screen) screen.onResult(packet);
            else if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("【角色卡】" + packet.message), false);
            }
        });
    }

    public static void handout(S2CHandout packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            Handout handout = Handout.fromNbt(packet.data);
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(CocLang.tr("【线索】") + CocLang.tr(handout.title())), false);
            }
            if (mc.screen instanceof JournalScreen) {
                mc.setScreen(new JournalScreen());
            }
        });
    }

    public static void openScreen(S2COpenScreen packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            switch (packet.screen) {
                case "sheet" -> {
                    if (packet.target != null) mc.setScreen(new CharacterSheetScreen(packet.target, packet.editable));
                }
                case "creation" -> mc.setScreen(new CharacterCreationScreen());
                case "kp" -> mc.setScreen(new KeeperScreen());
                case "journal" -> mc.setScreen(new JournalScreen());
                case "settings" -> mc.setScreen(new SettingsScreen(null));
                case "kp_book" -> mc.setScreen(new KeeperGuideScreen());
                case "global_settings" -> mc.setScreen(new GlobalSettingsScreen());
                default -> {
                    String screen = packet.screen == null ? "" : packet.screen;
                    if (screen.startsWith("scenario_book:")) {
                        String id = screen.substring("scenario_book:".length());
                        mc.setScreen(new StoryBookScreen(null, id, 0));
                    }
                }
            }
        });
    }

    public static void requestSyncIfNeeded() {
        if (!ClientCache.syncReceived) {
            CocNetwork.sendToServer(new C2SRequestSync());
        }
    }
}
