package com.coc.net.packet;

import com.coc.net.ServerPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 玩家导入剧本 JSON（从剪贴板）。 */
public final class C2SImportScenario {
    public static final int MAX_JSON = 65536;
    public final String json;

    public C2SImportScenario(String json) {
        this.json = json == null ? "" : json;
    }

    public C2SImportScenario(FriendlyByteBuf buf) {
        this.json = buf.readUtf(MAX_JSON);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(json, MAX_JSON);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> ServerPacketHandler.importScenario(ctx.getSender(), json));
        ctx.setPacketHandled(true);
    }
}
