package com.coc.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 世界级存档：所有角色卡、随机事件冷却、一次性事件、剧本进度。 */
public final class CocWorldData extends SavedData {
    public static final String DATA_NAME = "coc_world_data";

    private final Map<UUID, CharacterSheet> sheets = new LinkedHashMap<>();
    private final Map<UUID, Long> lastRandomEvent = new HashMap<>();
    private final Map<String, Long> eventCooldowns = new HashMap<>();
    private final Set<String> firedOnce = new HashSet<>();
    private ScenarioState scenario = new ScenarioState();

    public static CocWorldData load(CompoundTag tag) {
        CocWorldData data = new CocWorldData();
        ListTag sheetList = tag.getList("sheets", Tag.TAG_COMPOUND);
        for (int i = 0; i < sheetList.size(); i++) {
            CharacterSheet sheet = CharacterSheet.fromNbt(sheetList.getCompound(i));
            data.sheets.put(sheet.uuid, sheet);
        }
        CompoundTag randomTag = tag.getCompound("last_random");
        for (String key : randomTag.getAllKeys()) {
            try {
                data.lastRandomEvent.put(UUID.fromString(key), randomTag.getLong(key));
            } catch (Exception ignored) {
            }
        }
        CompoundTag cdTag = tag.getCompound("event_cooldowns");
        for (String key : cdTag.getAllKeys()) {
            data.eventCooldowns.put(key, cdTag.getLong(key));
        }
        ListTag onceList = tag.getList("fired_once", Tag.TAG_STRING);
        for (int i = 0; i < onceList.size(); i++) data.firedOnce.add(onceList.getString(i));
        data.scenario = ScenarioState.fromNbt(tag.getCompound("scenario"));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag sheetList = new ListTag();
        for (CharacterSheet sheet : sheets.values()) sheetList.add(sheet.toNbt());
        tag.put("sheets", sheetList);

        CompoundTag randomTag = new CompoundTag();
        for (Map.Entry<UUID, Long> e : lastRandomEvent.entrySet()) {
            randomTag.putLong(e.getKey().toString(), e.getValue());
        }
        tag.put("last_random", randomTag);

        CompoundTag cdTag = new CompoundTag();
        for (Map.Entry<String, Long> e : eventCooldowns.entrySet()) {
            cdTag.putLong(e.getKey(), e.getValue());
        }
        tag.put("event_cooldowns", cdTag);

        ListTag onceList = new ListTag();
        for (String id : firedOnce) onceList.add(StringTag.valueOf(id));
        tag.put("fired_once", onceList);

        tag.put("scenario", scenario.toNbt());
        return tag;
    }

    public CharacterSheet getSheet(UUID uuid) {
        return sheets.get(uuid);
    }

    public void putSheet(CharacterSheet sheet) {
        sheets.put(sheet.uuid, sheet);
        setDirty();
    }

    public void removeSheet(UUID uuid) {
        sheets.remove(uuid);
        lastRandomEvent.remove(uuid);
        setDirty();
    }

    public Collection<CharacterSheet> allSheets() {
        return sheets.values();
    }

    public long lastRandomEvent(UUID uuid) {
        return lastRandomEvent.getOrDefault(uuid, 0L);
    }

    public void markRandomEvent(UUID uuid, long gameTime) {
        lastRandomEvent.put(uuid, gameTime);
        setDirty();
    }

    public boolean eventOnCooldown(String id, long gameTime) {
        return eventCooldowns.getOrDefault(id, 0L) > gameTime;
    }

    public void setEventCooldown(String id, long untilGameTime) {
        eventCooldowns.put(id, untilGameTime);
        setDirty();
    }

    public boolean wasFiredOnce(String id) {
        return firedOnce.contains(id);
    }

    public void markFiredOnce(String id) {
        firedOnce.add(id);
        setDirty();
    }

    public ScenarioState scenario() {
        return scenario;
    }

    public void setScenario(ScenarioState state) {
        this.scenario = state;
        setDirty();
    }
}
