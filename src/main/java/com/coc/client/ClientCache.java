package com.coc.client;

import com.coc.data.CharacterSheet;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 客户端只读缓存。服务端通过 S2C 包持续刷新。 */
public final class ClientCache {
    public static final Map<UUID, CharacterSheet> SHEETS = new HashMap<>();
    public static UUID selfUuid;
    public static boolean selfHasSheet;
    public static boolean syncReceived;
    public static int creationPromptTicks = -1;
    public static PendingEventView pendingEvent;
    public static RollView lastRoll;
    public static KpView kpView;
    public static GlobalSettingsView globalSettings = new GlobalSettingsView();
    public static CurtainView curtain;
    public static ItemView items;
    public static BookView book;

    private ClientCache() {
    }

    public static CharacterSheet sheet(UUID uuid) {
        return uuid == null ? null : SHEETS.get(uuid);
    }

    public static CharacterSheet self() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return SHEETS.get(mc.player.getUUID());
    }

    public static void clear() {
        SHEETS.clear();
        selfUuid = null;
        selfHasSheet = false;
        syncReceived = false;
        creationPromptTicks = -1;
        pendingEvent = null;
        lastRoll = null;
        kpView = null;
        globalSettings = new GlobalSettingsView();
        curtain = null;
        items = null;
        book = null;
    }
}
