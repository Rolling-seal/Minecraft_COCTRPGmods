package com.coc.net.packet;

import com.coc.net.ServerPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class C2SCheckAction {
    public final String action;
    public final int points;

    public C2SCheckAction(String action, int points) {
        this.action = action;
        this.points = points;
    }

    public C2SCheckAction(FriendlyByteBuf buf) {
        this.action = buf.readUtf(32);
        this.points = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(action == null ? "" : action, 32);
        buf.writeInt(points);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> ServerPacketHandler.checkAction(ctx.getSender(), action, points));
        ctx.setPacketHandled(true);
    }
}
