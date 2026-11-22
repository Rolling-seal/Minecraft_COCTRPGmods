package com.coc.net.packet;

import com.coc.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 玩家幕布：只显示新一幕的标题/介绍，不含选项。 */
public final class S2CActCurtain {
    public final CompoundTag data;

    public S2CActCurtain(CompoundTag data) {
        this.data = data;
    }

    public S2CActCurtain(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.curtain(this)));
        ctx.setPacketHandled(true);
    }
}
