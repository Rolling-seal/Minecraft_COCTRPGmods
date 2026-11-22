package com.coc.net.packet;

import com.coc.net.ServerPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** KP 为玩家掷骰 / 公开掷骰。 */
public final class C2SKpRoll {
    public final UUID target;
    public final String kind;
    public final String skillId;
    public final int difficulty;
    public final int bonus;
    public final String expression;

    public C2SKpRoll(UUID target, String kind, String skillId, int difficulty, int bonus, String expression) {
        this.target = target;
        this.kind = kind;
        this.skillId = skillId;
        this.difficulty = difficulty;
        this.bonus = bonus;
        this.expression = expression;
    }

    public C2SKpRoll(FriendlyByteBuf buf) {
        this.target = buf.readBoolean() ? buf.readUUID() : null;
        this.kind = buf.readUtf(16);
        this.skillId = buf.readUtf(64);
        this.difficulty = buf.readInt();
        this.bonus = buf.readInt();
        this.expression = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(target != null);
        if (target != null) buf.writeUUID(target);
        buf.writeUtf(kind == null ? "" : kind, 16);
        buf.writeUtf(skillId == null ? "" : skillId, 64);
        buf.writeInt(difficulty);
        buf.writeInt(bonus);
        buf.writeUtf(expression == null ? "" : expression, 64);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> ServerPacketHandler.kpRoll(ctx.getSender(), this));
        ctx.setPacketHandled(true);
    }
}
