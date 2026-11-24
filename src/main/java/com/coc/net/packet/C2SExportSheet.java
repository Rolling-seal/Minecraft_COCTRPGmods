package com.coc.net.packet;

import com.coc.net.ServerPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** KP 请求导出角色卡 JSON。 */
public final class C2SExportSheet {
    public final UUID target;

    public C2SExportSheet(UUID target) {
        this.target = target;
    }

    public C2SExportSheet(FriendlyByteBuf buf) {
        this.target = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(target);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> ServerPacketHandler.exportSheet(ctx.getSender(), target));
        ctx.setPacketHandled(true);
    }
}
