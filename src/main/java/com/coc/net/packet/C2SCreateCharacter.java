package com.coc.net.packet;

import com.coc.net.ServerPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class C2SCreateCharacter {
    public final CompoundTag data;

    public C2SCreateCharacter(CompoundTag data) {
        this.data = data;
    }

    public C2SCreateCharacter(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> ServerPacketHandler.createCharacter(ctx.getSender(), data));
        ctx.setPacketHandled(true);
    }
}
