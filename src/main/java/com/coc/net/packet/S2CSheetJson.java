package com.coc.net.packet;

import com.coc.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 导出角色卡 JSON 数据，由客户端保存为文件。 */
public final class S2CSheetJson {
    public static final int MAX_JSON = 262144;
    public final String fileName;
    public final String json;

    public S2CSheetJson(String fileName, String json) {
        this.fileName = fileName == null ? "character.json" : fileName;
        this.json = json == null ? "" : json;
    }

    public S2CSheetJson(FriendlyByteBuf buf) {
        this.fileName = buf.readUtf(128);
        this.json = buf.readUtf(MAX_JSON);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(fileName, 128);
        buf.writeUtf(json, MAX_JSON);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.sheetJson(this)));
        ctx.setPacketHandled(true);
    }
}
