package com.coc.server;

import com.coc.data.ScenarioState;
import com.coc.events.EventRegistry;
import com.coc.events.ScenarioDef;
import com.coc.item.CocItems;
import com.coc.kp.BookDataBuilder;
import com.coc.kp.KpManager;
import com.coc.net.CocNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** KP 的剧本之书：开始剧本时发放/更新，右键快速翻阅每一幕。 */
public final class ScenarioBookService {
    public static final String TAG_SCENARIO = "coc_scenario";

    private ScenarioBookService() {
    }

    /** 给所有在线 KP 发放或更新一本剧本之书。 */
    public static void giveToKeepers(MinecraftServer server, ScenarioDef def) {
        if (server == null || def == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (KpManager.isKp(player)) giveOrUpdate(player, def);
        }
    }

    public static void giveOrUpdate(ServerPlayer player, ScenarioDef def) {
        if (player == null || def == null) return;
        ItemStack book = findBook(player);
        if (book == null) {
            book = new ItemStack(CocItems.SCENARIO_BOOK.get());
            book.getOrCreateTag().putString(TAG_SCENARIO, def.id);
            if (!player.getInventory().add(book)) player.drop(book, false);
        } else {
            book.getOrCreateTag().putString(TAG_SCENARIO, def.id);
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("剧本之书已就绪：" + def.title)
                .withStyle(ChatFormatting.GOLD));
    }

    /** 服务端打开：先下发剧本数据，再让客户端打开剧情之书界面。 */
    public static boolean open(ServerPlayer player, String stackScenarioId) {
        if (player == null) return false;
        if (!KpManager.isKp(player)) {
            player.sendSystemMessage(Component.literal("剧本之书只对守秘人展开。")
                    .withStyle(ChatFormatting.DARK_RED));
            return false;
        }
        String id = stackScenarioId == null ? "" : stackScenarioId.trim();
        if (id.isBlank() && player.getServer() != null) {
            ScenarioState state = CocData.get(player.getServer()).scenario();
            if (state.active) id = state.id;
        }
        ScenarioDef def = EventRegistry.scenario(id);
        if (def == null) {
            player.sendSystemMessage(Component.literal("剧本之书尚未绑定剧本；先开始一个剧本。")
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }
        BookDataBuilder.send(player, def.id);
        CocNetwork.openScreen(player, "scenario_book:" + def.id, null, false);
        return true;
    }

    private static ItemStack findBook(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(CocItems.SCENARIO_BOOK.get())) return stack;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(CocItems.SCENARIO_BOOK.get())) return stack;
        }
        return null;
    }
}
