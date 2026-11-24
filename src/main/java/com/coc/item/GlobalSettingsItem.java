package com.coc.item;

import com.coc.kp.KpManager;
import com.coc.net.CocNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 全局设置：右键打开角色创建规则设置。 */
public class GlobalSettingsItem extends Item {
    public GlobalSettingsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (KpManager.isKp(serverPlayer)) {
                CocNetwork.openScreen(serverPlayer, "global_settings", null, true);
            } else {
                serverPlayer.sendSystemMessage(Component.literal("这枚齿轮只响应守秘人的意志。")
                        .withStyle(ChatFormatting.DARK_RED));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.coc.global_settings.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.coc.global_settings.detail").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.coc.kp_only").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
