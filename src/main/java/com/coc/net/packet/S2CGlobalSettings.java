package com.coc.net.packet;

import com.coc.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端角色创建设置。 */
public final class S2CGlobalSettings {
    public final boolean allowPoints;
    public final int occupationPoints;
    public final int interestPoints;
    public final int totalPoints;

    public S2CGlobalSettings(boolean allowPoints, int occupationPoints, int interestPoints, int totalPoints) {
        this.allowPoints = allowPoints;
        this.occupationPoints = occupationPoints;
        this.interestPoints = interestPoints;
        this.totalPoints = totalPoints;
    }

    public S2CGlobalSettings(FriendlyByteBuf buf) {
        this.allowPoints = buf.readBoolean();
        this.occupationPoints = buf.readInt();
        this.interestPoints = buf.readInt();
        this.totalPoints = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(allowPoints);
        buf.writeInt(occupationPoints);
        buf.writeInt(interestPoints);
        buf.writeInt(totalPoints);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.globalSettings(this)));
        ctx.setPacketHandled(true);
    }
}
