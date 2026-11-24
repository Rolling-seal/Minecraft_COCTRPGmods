package com.coc.events;

import com.coc.data.CharacterSheet;
import com.coc.data.CocWorldData;
import com.coc.data.ScenarioState;
import com.coc.net.CocNetwork;
import com.coc.net.packet.S2CActCurtain;
import com.coc.server.CocData;
import com.coc.server.SanityService;
import com.coc.server.ScenarioBookService;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** 剧本推进：开始/每一幕只发“幕布介绍”，不再向玩家发自动事件卡。 */
public final class ScenarioManager {
    private ScenarioManager() {
    }

    public static void start(MinecraftServer server, String id) {
        ScenarioDef def = EventRegistry.scenario(id);
        if (def == null) {
            broadcast(server, "未知剧本：" + id, ChatFormatting.RED);
            return;
        }
        CocWorldData data = CocData.get(server);
        ScenarioState state = data.scenario();
        if (state.active) {
            broadcast(server, "已有正在进行的剧本：「" + state.id + "」。请先结束。", ChatFormatting.YELLOW);
            return;
        }
        state.id = def.id;
        state.index = -1;
        state.active = true;
        state.startedGameTime = server.overworld().getGameTime();
        data.setScenario(state);
        broadcast(server, "═══ 剧本开始：「" + def.title + "」 ═══", ChatFormatting.DARK_PURPLE);
        ScenarioBookService.giveToKeepers(server, def);
        sendCurtain(server, def, 0, def.title,
                def.intro == null || def.intro.isBlank()
                        ? "KP 正在准备第一幕。请描述你们此刻在哪里、正在做什么。"
                        : def.intro, List.of());
        for (ServerPlayer player : participants(server)) {
            player.sendSystemMessage(Component.literal("KP 已开始剧本。等待第一幕……")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public static void next(MinecraftServer server) {
        CocWorldData data = CocData.get(server);
        ScenarioState state = data.scenario();
        if (!state.active) {
            broadcast(server, "当前没有进行中的剧本。", ChatFormatting.YELLOW);
            return;
        }
        ScenarioDef def = EventRegistry.scenario(state.id);
        if (def == null) {
            broadcast(server, "剧本数据丢失：" + state.id, ChatFormatting.RED);
            state.reset();
            data.setScenario(state);
            return;
        }
        state.index++;
        if (state.index >= def.events.size()) {
            end(server, def, state, data);
            return;
        }
        data.setScenario(state);
        showAct(server, def, state.index);
    }

    /** KP 根据玩家行动直接触发某一幕（1-based）。只发幕布，不自动发选项。 */
    public static void triggerAct(MinecraftServer server, String id, int actNumber) {
        ScenarioDef def = EventRegistry.scenario(id);
        if (def == null) {
            broadcast(server, "未知剧本：" + id, ChatFormatting.RED);
            return;
        }
        CocWorldData data = CocData.get(server);
        ScenarioState state = data.scenario();
        if (state.active && !id.equals(state.id)) {
            broadcast(server, "已有其它剧本在进行，请先结束它。", ChatFormatting.YELLOW);
            return;
        }
        int index = actNumber - 1;
        if (index < 0 || index >= def.events.size()) {
            broadcast(server, "幕次不存在：" + actNumber, ChatFormatting.RED);
            return;
        }
        if (!state.active) {
            state.id = def.id;
            state.active = true;
            state.startedGameTime = server.overworld().getGameTime();
        }
        state.index = index;
        data.setScenario(state);
        showAct(server, def, index);
    }

    private static void showAct(MinecraftServer server, ScenarioDef def, int index) {
        String eventId = def.events.get(index);
        EventDef event = EventRegistry.event(eventId);
        ScenarioAct act = null;
        for (ScenarioAct candidate : def.acts) {
            if (candidate.act == index + 1) {
                act = candidate;
                break;
            }
        }
        String title = act != null && act.title != null && !act.title.isBlank()
                ? act.title : (event == null ? eventId : event.title);
        String description = event == null ? "（这一幕的数据缺失，请 KP 自行描述。）"
                : (event.description == null || event.description.isBlank()
                ? (act == null ? "" : act.summary) : event.description);
        broadcast(server, "── 第 " + (index + 1) + " 幕 / " + def.events.size()
                + " ── " + title, ChatFormatting.AQUA);
        List<String> options = new java.util.ArrayList<>();
        if (event != null && event.options != null) {
            for (EventDef.Option option : event.options) {
                if (option != null && option.text != null && !option.text.isBlank()
                        && options.size() < 3) {
                    options.add(option.text);
                }
            }
        }
        sendCurtain(server, def, index + 1, title, description, options);
    }

    /** 给所有有角色卡的玩家发一幕幕布；不发送任何选项或事件卡。 */
    private static void sendCurtain(MinecraftServer server, ScenarioDef def, int actNumber,
                                    String title, String description, List<String> options) {
        CompoundTag tag = new CompoundTag();
        tag.putString("scenario", def.title == null ? def.id : def.title);
        tag.putInt("act", actNumber);
        tag.putInt("total", def.events == null ? 0 : def.events.size());
        tag.putString("title", title == null ? "" : title);
        tag.putString("description", description == null ? "" : description);
        ListTag optionTags = new ListTag();
        if (options != null) {
            for (String option : options) {
                if (option != null && !option.isBlank()) optionTags.add(StringTag.valueOf(option));
            }
        }
        tag.put("options", optionTags);
        for (ServerPlayer player : participants(server)) {
            CocNetwork.sendToPlayer(player, new S2CActCurtain(tag));
        }
    }

    private static void end(MinecraftServer server, ScenarioDef def, ScenarioState state, CocWorldData data) {
        broadcast(server, "═══ 剧本结束：「" + def.title + "」 ═══", ChatFormatting.GOLD);
        sendCurtain(server, def, 0, def.title,
                def.outro == null || def.outro.isBlank() ? "故事到这里告一段落。" : def.outro, List.of());
        for (ServerPlayer player : participants(server)) {
            CharacterSheet sheet = CocData.sheet(player);
            if (sheet != null && def.sanReward != null && !def.sanReward.isBlank()) {
                int reward = Math.max(0, com.coc.rules.DiceRoller.evalNumber(
                        def.sanReward, player.serverLevel().random));
                if (reward > 0) {
                    SanityService.gainSan(player, sheet, reward);
                    player.sendSystemMessage(Component.literal("剧本完成，SAN 恢复奖励：+" + reward)
                            .withStyle(ChatFormatting.GREEN));
                }
            }
            player.sendSystemMessage(Component.literal("KP 可结算成长检定：使用 /coc growth 或 KP 面板。")
                    .withStyle(ChatFormatting.GRAY));
        }
        state.reset();
        data.setScenario(state);
    }

    public static void stop(MinecraftServer server) {
        CocWorldData data = CocData.get(server);
        ScenarioState state = data.scenario();
        if (!state.active) {
            broadcast(server, "当前没有进行中的剧本。", ChatFormatting.YELLOW);
            return;
        }
        String id = state.id;
        state.reset();
        data.setScenario(state);
        broadcast(server, "剧本已结束：" + id, ChatFormatting.GRAY);
    }

    public static String status(MinecraftServer server) {
        ScenarioState state = CocData.get(server).scenario();
        if (!state.active) return "无进行中的剧本";
        ScenarioDef def = EventRegistry.scenario(state.id);
        String title = def == null ? state.id : def.title;
        int total = def == null ? 0 : def.events.size();
        return "「" + title + "」 第 " + Math.max(1, state.index + 1) + " / " + total + " 幕";
    }

    private static List<ServerPlayer> participants(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream()
                .filter(p -> CocData.sheet(p) != null)
                .toList();
    }

    private static void broadcast(MinecraftServer server, String text, ChatFormatting color) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(text).withStyle(color));
        }
    }
}
