package com.coc.item;

import com.coc.server.ScenarioBookService;
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

/** 剧本之书：开始剧本后由 KP 持有，右键快速翻阅每一幕。 */
public class ScenarioBookItem extends Item {
    public ScenarioBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            String id = stack.hasTag() ? stack.getTag().getString(ScenarioBookService.TAG_SCENARIO) : "";
            ScenarioBookService.open(serverPlayer, id);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.coc.scenario_book.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.coc.kp_only").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
