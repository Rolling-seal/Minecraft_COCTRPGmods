package com.coc.net.packet;

import com.coc.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 发给 KP 的玩家背包列表。 */
public final class S2CItemList {
    public final CompoundTag data;

    public S2CItemList(CompoundTag data) {
        this.data = data;
    }

    public S2CItemList(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.itemList(this)));
        ctx.setPacketHandled(true);
    }
}
