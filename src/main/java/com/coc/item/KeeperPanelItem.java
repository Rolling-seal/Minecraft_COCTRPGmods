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

/** KP面板：右键打开守秘人面板，仅 KP 可用。 */
public class KeeperPanelItem extends Item {
    public KeeperPanelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (KpManager.isKp(serverPlayer)) {
                CocNetwork.openScreen(serverPlayer, "kp", null, true);
            } else {
                serverPlayer.sendSystemMessage(Component.literal("这块面板只响应守秘人的意志。")
                        .withStyle(ChatFormatting.DARK_RED));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("右键打开守秘人面板").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("仅限 KP 使用").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
