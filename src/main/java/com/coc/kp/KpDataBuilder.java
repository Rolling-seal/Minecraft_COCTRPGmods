package com.coc.kp;

import com.coc.data.CharacterSheet;
import com.coc.data.CocWorldData;
import com.coc.data.ScenarioState;
import com.coc.config.CocServerConfig;
import com.coc.events.EventDef;
import com.coc.events.EventRegistry;
import com.coc.events.ScenarioAct;
import com.coc.events.ScenarioDef;
import com.coc.net.CocNetwork;
import com.coc.net.packet.S2CKpData;
import com.coc.server.CocData;
import com.coc.server.ItemLibrary;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** 构建 KP 面板数据。 */
public final class KpDataBuilder {
    private KpDataBuilder() {
    }

    public static void send(ServerPlayer viewer) {
        if (viewer == null) return;
        MinecraftServer server = viewer.getServer();
        CompoundTag tag = new CompoundTag();
        boolean allowed = KpManager.isKp(viewer);
        tag.putBoolean("allowed", allowed);
        if (!allowed) {
            CocNetwork.sendToPlayer(viewer, new S2CKpData(tag));
            return;
        }
        tag.putString("scenario_status", com.coc.events.ScenarioManager.status(server));
        tag.putBoolean("random_enabled", CocServerConfig.get().randomEventsEnabled);
        tag.putInt("random_interval", CocServerConfig.get().randomEventIntervalTicks);

        ScenarioState state = CocData.get(server).scenario();
        tag.putBoolean("scenario_active", state.active);
        tag.putString("scenario_id", state.id == null ? "" : state.id);
        tag.putInt("scenario_index", state.index);

        ListTag players = new ListTag();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            CharacterSheet sheet = CocData.get(server).getSheet(p.getUUID());
            CompoundTag pt = new CompoundTag();
            pt.putUUID("uuid", p.getUUID());
            pt.putString("name", p.getGameProfile().getName());
            pt.putBoolean("has_sheet", sheet != null);
            if (sheet != null) {
                pt.putInt("san", sheet.san);
                pt.putInt("max_san", sheet.maxSan);
                pt.putInt("hp", sheet.hp);
                pt.putInt("max_hp", sheet.maxHp);
                pt.putInt("mp", sheet.mp);
                pt.putInt("max_mp", sheet.maxMp);
                pt.putInt("luck", sheet.stats.luck);
                pt.putInt("mythos", sheet.mythos);
                CompoundTag skills = new CompoundTag();
                for (var entry : sheet.skills.entrySet()) {
                    skills.putInt(entry.getKey(), entry.getValue());
                }
                pt.put("skills", skills);
                pt.putString("status", sheet.statusLine(p.serverLevel().getGameTime()));
                pt.putBoolean("retired", sheet.retired);
                pt.putBoolean("insane", sheet.isInsane(p.serverLevel().getGameTime()));
            }
            players.add(pt);
        }
        tag.put("players", players);

        ListTag itemRules = new ListTag();
        for (ItemLibrary.ItemRule rule : ItemLibrary.rules()) {
            CompoundTag rt = new CompoundTag();
            rt.putString("id", rule.id);
            rt.putString("skill", rule.skill == null ? "" : rule.skill);
            rt.putInt("target", rule.target);
            itemRules.add(rt);
        }
        tag.put("item_rules", itemRules);

        ListTag events = new ListTag();
        for (EventDef def : EventRegistry.events()) {
            CompoundTag et = new CompoundTag();
            et.putString("id", def.id);
            et.putString("title", def.title == null ? def.id : def.title);
            et.putInt("weight", def.weight);
            et.putBoolean("kp_only", def.kpOnly);
            et.putBoolean("once", def.once);
            et.putString("category", def.category == null || def.category.isBlank() ? "随机" : def.category);
            et.putInt("act", def.act);
            et.putString("scenario", def.scenarioTitle == null ? "" : def.scenarioTitle);
            events.add(et);
        }
        tag.put("events", events);

        ListTag scenarios = new ListTag();
        for (ScenarioDef def : EventRegistry.scenarios()) {
            CompoundTag st = new CompoundTag();
            st.putString("id", def.id);
            st.putString("title", def.title == null ? def.id : def.title);
            st.putInt("count", def.events == null ? 0 : def.events.size());
            ListTag acts = new ListTag();
            for (ScenarioAct a : def.acts) {
                CompoundTag at = new CompoundTag();
                at.putInt("act", a.act);
                at.putString("event", a.event == null ? "" : a.event);
                at.putString("title", a.title == null ? "" : a.title);
                at.putString("summary", a.summary == null ? "" : a.summary);
                at.putString("trigger", a.trigger == null ? "" : a.trigger);
                ListTag sides = new ListTag();
                if (a.sideEvents != null) {
                    for (String side : a.sideEvents) {
                        sides.add(net.minecraft.nbt.StringTag.valueOf(side));
                    }
                }
                at.put("side_events", sides);
                acts.add(at);
            }
            st.put("acts", acts);
            scenarios.add(st);
        }
        tag.put("scenarios", scenarios);

        ListTag errors = new ListTag();
        for (String error : EventRegistry.errors()) {
            errors.add(net.minecraft.nbt.StringTag.valueOf(error));
        }
        tag.put("errors", errors);
        CocNetwork.sendToPlayer(viewer, new S2CKpData(tag));
    }
}
