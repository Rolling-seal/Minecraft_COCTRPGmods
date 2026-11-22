package com.coc.net.packet;

import com.coc.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class S2CSyncSheet {
    public final CompoundTag data;
    public final boolean self;
    public final UUID target;
    public final boolean hasSheet;

    public S2CSyncSheet(CompoundTag data, boolean self, UUID target, boolean hasSheet) {
        this.data = data;
        this.self = self;
        this.target = target;
        this.hasSheet = hasSheet;
    }

    public S2CSyncSheet(FriendlyByteBuf buf) {
        this.hasSheet = buf.readBoolean();
        this.data = hasSheet ? buf.readNbt() : null;
        this.self = buf.readBoolean();
        this.target = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(hasSheet && data != null);
        if (hasSheet && data != null) buf.writeNbt(data);
        buf.writeBoolean(self);
        buf.writeUUID(target);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.syncSheet(this)));
        ctx.setPacketHandled(true);
    }
}
