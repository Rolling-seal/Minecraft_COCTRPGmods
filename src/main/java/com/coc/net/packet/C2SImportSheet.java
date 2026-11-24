package com.coc.net.packet;

import com.coc.net.ServerPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** KP 向指定玩家导入角色卡 JSON。 */
public final class C2SImportSheet {
    public static final int MAX_JSON = 262144;
    public final UUID target;
    public final String json;

    public C2SImportSheet(UUID target, String json) {
        this.target = target;
        this.json = json == null ? "" : json;
    }

    public C2SImportSheet(FriendlyByteBuf buf) {
        this.target = buf.readUUID();
        this.json = buf.readUtf(MAX_JSON);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(target);
        buf.writeUtf(json, MAX_JSON);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> ServerPacketHandler.importSheet(ctx.getSender(), target, json));
        ctx.setPacketHandled(true);
    }
}
