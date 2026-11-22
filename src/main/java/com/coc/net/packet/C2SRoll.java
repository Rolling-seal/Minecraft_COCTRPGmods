package com.coc.net.packet;

import com.coc.net.ServerPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class C2SRoll {
    public final String kind;
    public final String expression;
    public final String skillId;
    public final int difficulty;
    public final int bonus;
    public final int target;

    public C2SRoll(String kind, String expression, String skillId, int difficulty, int bonus, int target) {
        this.kind = kind;
        this.expression = expression;
        this.skillId = skillId;
        this.difficulty = difficulty;
        this.bonus = bonus;
        this.target = target;
    }

    public C2SRoll(FriendlyByteBuf buf) {
        this.kind = buf.readUtf(32);
        this.expression = buf.readUtf(128);
        this.skillId = buf.readUtf(64);
        this.difficulty = buf.readInt();
        this.bonus = buf.readInt();
        this.target = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(kind == null ? "" : kind, 32);
        buf.writeUtf(expression == null ? "" : expression, 128);
        buf.writeUtf(skillId == null ? "" : skillId, 64);
        buf.writeInt(difficulty);
        buf.writeInt(bonus);
        buf.writeInt(target);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> ServerPacketHandler.roll(ctx.getSender(), kind, expression, skillId, difficulty, bonus, target));
        ctx.setPacketHandled(true);
    }
}
