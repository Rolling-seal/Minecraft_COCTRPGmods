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
    public final int luckMin;
    public final int luckMax;
    public final int hpMin;
    public final int hpMax;
    public final int sanMin;
    public final int sanMax;

    public C2SGlobalSettings(boolean allowPoints, int occupationPoints, int interestPoints, int totalPoints,
                             int luckMin, int luckMax, int hpMin, int hpMax, int sanMin, int sanMax) {
        this.allowPoints = allowPoints;
        this.occupationPoints = occupationPoints;
        this.interestPoints = interestPoints;
        this.totalPoints = totalPoints;
        this.luckMin = luckMin;
        this.luckMax = luckMax;
        this.hpMin = hpMin;
        this.hpMax = hpMax;
        this.sanMin = sanMin;
        this.sanMax = sanMax;
    }

    public C2SGlobalSettings(FriendlyByteBuf buf) {
        this.allowPoints = buf.readBoolean();
        this.occupationPoints = buf.readInt();
        this.interestPoints = buf.readInt();
        this.totalPoints = buf.readInt();
        this.luckMin = buf.readInt();
        this.luckMax = buf.readInt();
        this.hpMin = buf.readInt();
        this.hpMax = buf.readInt();
        this.sanMin = buf.readInt();
        this.sanMax = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(allowPoints);
        buf.writeInt(occupationPoints);
        buf.writeInt(interestPoints);
        buf.writeInt(totalPoints);
        buf.writeInt(luckMin);
        buf.writeInt(luckMax);
        buf.writeInt(hpMin);
        buf.writeInt(hpMax);
        buf.writeInt(sanMin);
        buf.writeInt(sanMax);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> ServerPacketHandler.globalSettings(ctx.getSender(), this));
        ctx.setPacketHandled(true);
    }
}
