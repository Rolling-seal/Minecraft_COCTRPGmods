package com.coc.net.packet;

import com.coc.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 剧本导入结果。 */
public final class S2CImportResult {
    public final boolean success;
    public final String message;
    public final String scenarioId;
    public final String title;

    public S2CImportResult(boolean success, String message, String scenarioId, String title) {
        this.success = success;
        this.message = message == null ? "" : message;
        this.scenarioId = scenarioId == null ? "" : scenarioId;
        this.title = title == null ? "" : title;
    }

    public S2CImportResult(FriendlyByteBuf buf) {
        this.success = buf.readBoolean();
        this.message = buf.readUtf(1024);
        this.scenarioId = buf.readUtf(64);
        this.title = buf.readUtf(128);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
        buf.writeUtf(message, 1024);
        buf.writeUtf(scenarioId, 64);
        buf.writeUtf(title, 128);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.importResult(this)));
        ctx.setPacketHandled(true);
    }
}
