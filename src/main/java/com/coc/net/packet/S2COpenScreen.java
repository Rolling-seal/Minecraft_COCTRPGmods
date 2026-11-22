package com.coc.net.packet;

import com.coc.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class S2COpenScreen {
    public final String screen;
    public final UUID target;
    public final boolean editable;

    public S2COpenScreen(String screen, UUID target, boolean editable) {
        this.screen = screen;
        this.target = target;
        this.editable = editable;
    }

    public S2COpenScreen(FriendlyByteBuf buf) {
        this.screen = buf.readUtf(32);
        this.target = buf.readBoolean() ? buf.readUUID() : null;
        this.editable = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(screen == null ? "" : screen, 32);
        buf.writeBoolean(target != null);
        if (target != null) buf.writeUUID(target);
        buf.writeBoolean(editable);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.openScreen(this)));
        ctx.setPacketHandled(true);
    }
}
