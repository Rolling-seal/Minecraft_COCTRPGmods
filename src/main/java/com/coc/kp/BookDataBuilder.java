package com.coc.kp;

import com.coc.data.ScenarioState;
import com.coc.events.EventDef;
import com.coc.events.EventRegistry;
import com.coc.events.ScenarioAct;
import com.coc.events.ScenarioDef;
import com.coc.net.CocNetwork;
import com.coc.net.packet.S2CBookData;
import com.coc.rules.Difficulty;
import com.coc.rules.SkillRegistry;
import com.coc.server.CocData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;

/** 构建 KP 的剧情之书：主线每幕走向 + 支线触发/结果。 */
public final class BookDataBuilder {
    private BookDataBuilder() {
    }

    public static void send(ServerPlayer viewer, String scenarioId) {
        if (viewer == null) return;
        ScenarioDef def = EventRegistry.scenario(scenarioId);
        if (def == null) {
            viewer.sendSystemMessage(net.minecraft.network.chat.Component.literal("未知剧本：" + scenarioId)
                    .withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }
        MinecraftServer server = viewer.getServer();
        CompoundTag tag = new CompoundTag();
        tag.putString("scenario_id", def.id);
        tag.putString("title", def.title == null ? def.id : def.title);
        tag.putString("description", def.description == null ? "" : def.description);
        ScenarioState state = server == null ? new ScenarioState() : CocData.get(server).scenario();
        tag.putBoolean("active", state.active && def.id.equals(state.id));
        tag.putInt("index", def.id.equals(state.id) ? state.index : -1);

        ListTag acts = new ListTag();
        Map<String, ScenarioAct> sideMap = new LinkedHashMap<>();
        for (ScenarioAct act : def.acts) {
            CompoundTag at = new CompoundTag();
            EventDef main = EventRegistry.event(act.event);
            at.putInt("act", act.act);
            at.putString("event", act.event == null ? "" : act.event);
            at.putString("title", first(act.title, main == null ? act.event : main.title));
            at.putString("summary", nullToEmpty(act.summary));
            at.putString("trigger", nullToEmpty(act.trigger));
            at.putString("description", main == null ? "" : nullToEmpty(main.description));
            at.put("options", options(main));
            ListTag sideTitles = new ListTag();
            if (act.sideEvents != null) {
                for (String sideId : act.sideEvents) {
                    EventDef side = EventRegistry.event(sideId);
                    if (side != null) sideTitles.add(StringTag.valueOf(first(side.title, sideId)));
                    sideMap.putIfAbsent(sideId, act);
                }
            }
            at.put("side_titles", sideTitles);
            acts.add(at);
        }
        tag.put("acts", acts);

        ListTag sides = new ListTag();
        for (Map.Entry<String, ScenarioAct> entry : sideMap.entrySet()) {
            EventDef side = EventRegistry.event(entry.getKey());
            if (side == null) continue;
            CompoundTag st = new CompoundTag();
            st.putString("id", side.id);
            st.putInt("act", entry.getValue().act);
            st.putString("title", first(side.title, side.id));
            st.putString("trigger", first(side.triggerHint, entry.getValue().trigger));
            st.putString("description", nullToEmpty(side.description));
            st.put("options", options(side));
            sides.add(st);
        }
        tag.put("sides", sides);
        CocNetwork.sendToPlayer(viewer, new S2CBookData(tag));
    }

    private static ListTag options(EventDef def) {
        ListTag list = new ListTag();
        if (def == null || def.options == null) return list;
        for (EventDef.Option option : def.options) {
            if (option == null) continue;
            CompoundTag ot = new CompoundTag();
            ot.putString("text", nullToEmpty(option.text));
            ot.putString("skill", nullToEmpty(option.skill));
            ot.putString("skill_name", option.skill == null || option.skill.isBlank()
                    ? "无检定" : SkillRegistry.nameOf(option.skill));
            Difficulty difficulty = Difficulty.byName(option.difficulty);
            ot.putString("difficulty", difficulty.cn());
            if (option.outcome != null) {
                ot.putString("outcome", nullToEmpty(option.outcome.text));
                ot.putString("clue", nullToEmpty(option.outcome.handout));
            }
            if (option.success != null) {
                ot.putString("success", nullToEmpty(option.success.text));
                ot.putString("success_clue", nullToEmpty(option.success.handout));
            }
            if (option.failure != null) {
                ot.putString("failure", nullToEmpty(option.failure.text));
                ot.putString("failure_clue", nullToEmpty(option.failure.handout));
            }
            list.add(ot);
        }
        return list;
    }

    private static String first(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return b == null ? "" : b;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
