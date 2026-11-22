package com.coc.net.packet;

import com.coc.net.ServerPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class C2SKpAction {
    public final String action;
    public final UUID target;
    public final String text;
    public final int value;

    public C2SKpAction(String action, UUID target, String text, int value) {
        this.action = action;
        this.target = target;
        this.text = text;
        this.value = value;
    }

    public C2SKpAction(FriendlyByteBuf buf) {
        this.action = buf.readUtf(64);
        this.target = buf.readBoolean() ? buf.readUUID() : null;
        this.text = buf.readUtf(512);
        this.value = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(action == null ? "" : action, 64);
        buf.writeBoolean(target != null);
        if (target != null) buf.writeUUID(target);
        buf.writeUtf(text == null ? "" : text, 512);
        buf.writeInt(value);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> ServerPacketHandler.kpAction(ctx.getSender(), action, target, text, value));
        ctx.setPacketHandled(true);
    }
}
