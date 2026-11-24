package com.coc.server;

import com.coc.net.CocNetwork;
import com.coc.net.packet.S2CItemList;
import com.coc.rules.DiceRoller;
import com.coc.rules.SkillRegistry;
import com.coc.rules.SuccessLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** KP 道具工具：查看背包、发放道具、取走道具并做一次公开使用检定。 */
public final class ItemService {
    private ItemService() {
    }

    public static void sendItems(ServerPlayer viewer, ServerPlayer target) {
        if (viewer == null) return;
        if (target == null) {
            viewer.sendSystemMessage(Component.literal("目标玩家不在线。").withStyle(ChatFormatting.RED));
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putUUID("target", target.getUUID());
        tag.putString("target_name", target.getGameProfile().getName());
        ListTag list = new ListTag();
        for (int slot = 0; slot < target.getInventory().getContainerSize(); slot++) {
            ItemStack stack = target.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            CompoundTag it = new CompoundTag();
            it.putInt("slot", slot);
            it.putString("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            it.putString("name", stack.getHoverName().getString());
            it.putInt("count", stack.getCount());
            list.add(it);
        }
        tag.put("items", list);
        CocNetwork.sendToPlayer(viewer, new S2CItemList(tag));
    }

    public static void give(ServerPlayer viewer, ServerPlayer target, String itemId, int count) {
        if (viewer == null || target == null) return;
        ResourceLocation id = ResourceLocation.tryParse(itemId == null ? "" : itemId.trim());
        if (id == null) {
            viewer.sendSystemMessage(Component.literal("道具 ID 格式错误，例如 minecraft:torch。")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            viewer.sendSystemMessage(Component.literal("未知道具：" + id).withStyle(ChatFormatting.RED));
            return;
        }
        int amount = Math.max(1, Math.min(64, count));
        ItemStack stack = new ItemStack(item, amount);
        ItemLibrary.ItemRule rule = ItemLibrary.get(id.toString());
        if (rule != null && rule.name != null && !rule.name.isBlank()) {
            stack.setHoverName(Component.literal(rule.name));
        }
        String name = stack.getHoverName().getString();
        target.getInventory().add(stack);
        if (!stack.isEmpty()) target.drop(stack, false);
        target.getInventory().setChanged();
        target.containerMenu.broadcastChanges();
        viewer.sendSystemMessage(Component.literal("已发放给 " + target.getGameProfile().getName()
                + "：" + name + " ×" + amount).withStyle(ChatFormatting.GOLD));
        if (target != viewer) {
            target.sendSystemMessage(Component.literal("KP 给了你：" + name + " ×" + amount)
                    .withStyle(ChatFormatting.GREEN));
        }
        sendItems(viewer, target);
    }

    public static void use(ServerPlayer viewer, ServerPlayer target, int slot, int targetNumber) {
        if (viewer == null || target == null) return;
        if (slot < 0 || slot >= target.getInventory().getContainerSize()) {
            viewer.sendSystemMessage(Component.literal("道具位置无效，请刷新道具列表。")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        ItemStack stack = target.getInventory().getItem(slot);
        if (stack.isEmpty()) {
            viewer.sendSystemMessage(Component.literal("这个位置已经没有道具了，请刷新。")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        ItemLibrary.ItemRule rule = ItemLibrary.get(itemId);
        int goal = rule == null ? Math.max(1, Math.min(100, targetNumber)) : rule.target;
        String skillName = rule == null || rule.skill == null || rule.skill.isBlank()
                ? "" : SkillRegistry.nameOf(rule.skill);
        String itemName = stack.getHoverName().getString();
        stack.shrink(1);
        target.getInventory().setChanged();
        target.containerMenu.broadcastChanges();

        int roll = DiceRoller.d100(target.serverLevel().random);
        SuccessLevel level = DiceRoller.evaluate(roll, goal);
        boolean success = level.rank() >= SuccessLevel.REGULAR.rank();
        CompoundTag tag = new CompoundTag();
        tag.putString("title", target.getGameProfile().getName() + " · " + itemName
                + (skillName.isBlank() ? " · 道具检定" : " · " + skillName));
        tag.putInt("roll", roll);
        tag.putInt("target", goal);
        tag.putString("level", level.cn());
        tag.putInt("level_color", level.color());
        tag.putBoolean("success", success);
        tag.putString("detail", "KP 从背包取走 1 件「" + itemName + "」；"
                + (skillName.isBlank() ? "" : "技能 " + skillName + "；")
                + (rule == null ? "临时目标 " : "道具规则目标 ") + goal);
        tag.putBoolean("can_push", false);
        tag.putInt("luck_needed", 0);
        tag.putBoolean("fumble", level == SuccessLevel.FUMBLE);
        tag.putBoolean("critical", level == SuccessLevel.CRITICAL);
        tag.putBoolean("can_luck", false);
        tag.putString("kind", "item");
        String detail = "道具：" + itemName + "；"
                + (skillName.isBlank() ? "" : "技能 " + skillName + "；")
                + (rule == null ? "临时目标 " : "规则目标 ") + goal
                + "；已从 " + target.getGameProfile().getName() + " 的背包取走 1 件。";
        CheckService.broadcastPublic(viewer, tag, detail);
        sendItems(viewer, target);
    }
}
