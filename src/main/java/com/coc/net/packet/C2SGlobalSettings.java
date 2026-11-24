package com.coc.net.packet;

import com.coc.net.ServerPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** KP 修改全局角色创建设置。 */
public final class C2SGlobalSettings {
    public final boolean allowPoints;
    public final int occupationPoints;
    public final int interestPoints;
    public final int totalPoints;

    public C2SGlobalSettings(boolean allowPoints, int occupationPoints, int interestPoints, int totalPoints) {
        this.allowPoints = allowPoints;
        this.occupationPoints = occupationPoints;
        this.interestPoints = interestPoints;
        this.totalPoints = totalPoints;
    }

    public C2SGlobalSettings(FriendlyByteBuf buf) {
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
        ctx.enqueueWork(() -> ServerPacketHandler.globalSettings(ctx.getSender(), this));
        ctx.setPacketHandled(true);
    }
}
