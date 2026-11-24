package com.coc.net.packet;

import com.coc.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 角色卡导出/导入结果。 */
public final class S2CSheetResult {
    public final boolean success;
    public final String message;

    public S2CSheetResult(boolean success, String message) {
        this.success = success;
        this.message = message == null ? "" : message;
    }

    public S2CSheetResult(FriendlyByteBuf buf) {
        this.success = buf.readBoolean();
        this.message = buf.readUtf(1024);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
        buf.writeUtf(message, 1024);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.sheetResult(this)));
        ctx.setPacketHandled(true);
    }
}
