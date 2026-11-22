package com.coc.server;

import com.coc.data.CharacterSheet;
import com.coc.data.Handout;
import com.coc.net.CocNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 发放线索：写入角色卡、同步、聊天提示。 */
public final class HandoutService {
    private HandoutService() {
    }

    public static void send(ServerPlayer player, String title, String text) {
        if (player == null) return;
        CharacterSheet sheet = CocData.sheet(player);
        if (sheet == null) return;
        Handout handout = new Handout(title == null ? "线索" : title,
                text == null ? "" : text, player.serverLevel().getGameTime());
        sheet.addHandout(handout);
        CocNetwork.sendToPlayer(player, new com.coc.net.packet.S2CHandout(handout.toNbt()));
        CocNetwork.syncSheet(player, sheet);
        player.sendSystemMessage(Component.literal("【线索】" + handout.title())
                .withStyle(ChatFormatting.GOLD));
        if (!handout.text().isBlank()) {
            player.sendSystemMessage(Component.literal(handout.text())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
