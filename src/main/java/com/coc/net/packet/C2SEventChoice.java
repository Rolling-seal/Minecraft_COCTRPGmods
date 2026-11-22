package com.coc.net.packet;

import com.coc.net.ServerPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class C2SEventChoice {
    public final int index;

    public C2SEventChoice(int index) {
        this.index = index;
    }

    public C2SEventChoice(FriendlyByteBuf buf) {
        this.index = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(index);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> ServerPacketHandler.eventChoice(ctx.getSender(), index));
        ctx.setPacketHandled(true);
    }
}
