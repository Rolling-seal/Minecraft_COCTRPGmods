package com.coc.kp;

import com.coc.data.CharacterSheet;
import com.coc.data.CocWorldData;
import com.coc.events.EventRegistry;
import com.coc.events.ScenarioManager;
import com.coc.net.CocNetwork;
import com.coc.server.CharacterService;
import com.coc.server.CocData;
import com.coc.server.GrowthService;
import com.coc.server.HandoutService;
import com.coc.server.ItemLibrary;
import com.coc.server.ItemService;
import com.coc.server.SanityService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** KP 面板动作的统一服务端入口。新增动作在此加分支，面板加按钮即可。 */
public final class KpActions {
    private KpActions() {
    }

    public static void handle(ServerPlayer viewer, String action, UUID targetUuid, String text, int value) {
        if (viewer == null || action == null) return;
        MinecraftServer server = viewer.getServer();
        if (server == null) return;
        if ("data".equals(action)) {
            KpDataBuilder.send(viewer);
            return;
        }
        if (!KpManager.isKp(viewer)) {
            viewer.sendSystemMessage(Component.literal("你不是 KP，无法使用这个操作。")
                    .withStyle(ChatFormatting.RED));
            KpDataBuilder.send(viewer);
            return;
        }
        ServerPlayer target = targetUuid == null ? viewer : server.getPlayerList().getPlayer(targetUuid);
        CharacterSheet sheet = target == null ? null : CocData.sheet(target);
        CocWorldData world = CocData.get(server);

        switch (action) {
            case "data" -> {
                KpDataBuilder.send(viewer);
                return;
            }
            case "sheet" -> {
                if (target != null) CharacterService.openSheet(viewer, target.getUUID(), true);
                return;
            }
            case "san_add" -> {
                if (sheet != null) {
                    if (value >= 0) SanityService.gainSan(target, sheet, value);
                    else SanityService.loseSan(target, sheet, -value, "KP 调整", target.serverLevel().random);
                }
            }
            case "san_set" -> {
                if (sheet != null) SanityService.setSan(target, sheet, value);
            }
            case "hp_add" -> {
                if (sheet != null) {
                    sheet.hp = Math.max(0, Math.min(sheet.maxHp, sheet.hp + value));
                    sheet.dying = sheet.hp <= 0;
                    CocNetwork.syncSheet(target, sheet);
                }
            }
            case "hp_set" -> {
                if (sheet != null) {
                    sheet.hp = Math.max(0, Math.min(sheet.maxHp, value));
                    sheet.dying = sheet.hp <= 0;
                    CocNetwork.syncSheet(target, sheet);
                }
            }
            case "mp_add" -> {
                if (sheet != null) {
                    sheet.mp = Math.max(0, Math.min(sheet.maxMp, sheet.mp + value));
                    CocNetwork.syncSheet(target, sheet);
                }
            }
            case "mp_set" -> {
                if (sheet != null) {
                    sheet.mp = Math.max(0, Math.min(sheet.maxMp, value));
                    CocNetwork.syncSheet(target, sheet);
                }
            }
            case "luck_add" -> {
                if (sheet != null) {
                    sheet.stats.luck = Math.max(0, Math.min(99, sheet.stats.luck + value));
                    CocNetwork.syncSheet(target, sheet);
                }
            }
            case "luck_set" -> {
                if (sheet != null) {
                    sheet.stats.luck = Math.max(0, Math.min(99, value));
                    CocNetwork.syncSheet(target, sheet);
                }
            }
            case "heal" -> {
                if (sheet != null) {
                    sheet.hp = sheet.maxHp;
                    sheet.mp = sheet.maxMp;
                    sheet.dying = false;
                    CocNetwork.syncSheet(target, sheet);
                    target.sendSystemMessage(Component.literal("KP 将你的 HP/MP 恢复了。")
                            .withStyle(ChatFormatting.GREEN));
                }
            }
            case "clear_insanity" -> {
                if (sheet != null) SanityService.clearInsanity(target, sheet);
            }
            case "madness" -> {
                if (sheet != null) SanityService.forceTemporaryInsanity(target, sheet);
            }
            case "growth" -> {
                if (sheet != null) GrowthService.grow(target, sheet);
            }
            case "delete" -> {
                if (target != null && "confirm".equals(text)) {
                    com.coc.data.CharacterSheet removed = CharacterService.delete(server, target.getUUID());
                    if (removed != null) {
                        viewer.sendSystemMessage(Component.literal("已删除角色卡：" + removed.name)
                                .withStyle(ChatFormatting.GOLD));
                    }
                }
            }
            case "scenario_start" -> ScenarioManager.start(server, text);
            case "scenario_next" -> ScenarioManager.next(server);
            case "scenario_act" -> ScenarioManager.triggerAct(server, text, value);
            case "scenario_stop" -> ScenarioManager.stop(server);
            case "book" -> BookDataBuilder.send(viewer, text);
            case "items" -> ItemService.sendItems(viewer, target);
            case "item_give" -> ItemService.give(viewer, target, text, value);
            case "item_rule" -> {
                String[] parts = (text == null ? "" : text).split("\\|", 2);
                String itemId = parts.length > 0 ? parts[0].trim() : "";
                String skillId = parts.length > 1 ? parts[1].trim() : "";
                if (ItemLibrary.upsert(itemId, skillId, value)) {
                    viewer.sendSystemMessage(Component.literal("已保存道具规则：" + itemId
                            + "（目标 " + Math.max(1, Math.min(100, value)) + "）")
                            .withStyle(ChatFormatting.AQUA));
                } else {
                    viewer.sendSystemMessage(Component.literal("保存道具规则失败：物品或技能无效。")
                            .withStyle(ChatFormatting.RED));
                }
            }
            case "item_use" -> {
                int slot;
                try {
                    slot = Integer.parseInt(text == null ? "" : text.trim());
                } catch (NumberFormatException e) {
                    viewer.sendSystemMessage(Component.literal("道具位置无效。").withStyle(ChatFormatting.RED));
                    return;
                }
                ItemService.use(viewer, target, slot, value);
            }
            case "handout" -> {
                if (target != null) HandoutService.send(target, "KP 手书", text);
            }
            case "handout_all" -> {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (CocData.sheet(p) != null) HandoutService.send(p, "KP 手书", text);
                }
            }
            case "broadcast" -> {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    p.sendSystemMessage(Component.literal("【KP】" + text)
                            .withStyle(ChatFormatting.DARK_PURPLE));
                }
            }
            case "reload" -> {
                EventRegistry.reload();
                viewer.sendSystemMessage(Component.literal("已重载事件/剧本：" + EventRegistry.events().size()
                        + " 个事件，" + EventRegistry.scenarios().size() + " 个剧本。")
                        .withStyle(ChatFormatting.AQUA));
            }
            case "dump" -> {
                int written = EventRegistry.dumpBuiltIns();
                viewer.sendSystemMessage(Component.literal("已导出内置内容到 config/coc：" + written + " 个文件。")
                        .withStyle(ChatFormatting.AQUA));
            }
            default -> viewer.sendSystemMessage(Component.literal("未知 KP 动作：" + action)
                    .withStyle(ChatFormatting.RED));
        }
        KpDataBuilder.send(viewer);
    }
}
