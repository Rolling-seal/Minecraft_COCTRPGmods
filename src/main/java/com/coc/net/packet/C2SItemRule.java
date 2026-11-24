package com.coc.net.packet;

import com.coc.net.ServerPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** KP 保存物品规则：物品 ID、自定义名称、技能、目标点数。 */
public final class C2SItemRule {
    public final String itemId;
    public final String name;
    public final String skillId;
    public final int target;

    public C2SItemRule(String itemId, String name, String skillId, int target) {
        this.itemId = itemId == null ? "" : itemId;
        this.name = name == null ? "" : name;
        this.skillId = skillId == null ? "" : skillId;
        this.target = target;
    }

    public C2SItemRule(FriendlyByteBuf buf) {
        this.itemId = buf.readUtf(120);
        this.name = buf.readUtf(80);
        this.skillId = buf.readUtf(64);
        this.target = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(itemId, 120);
        buf.writeUtf(name, 80);
        buf.writeUtf(skillId, 64);
        buf.writeInt(target);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> ServerPacketHandler.itemRule(ctx.getSender(), this));
        ctx.setPacketHandled(true);
    }
}
