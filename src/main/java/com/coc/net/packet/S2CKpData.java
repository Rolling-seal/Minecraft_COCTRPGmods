package com.coc.net.packet;

import com.coc.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class S2CKpData {
    public final CompoundTag data;

    public S2CKpData(CompoundTag data) {
        this.data = data;
    }

    public S2CKpData(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.kpData(this)));
        ctx.setPacketHandled(true);
    }
}
