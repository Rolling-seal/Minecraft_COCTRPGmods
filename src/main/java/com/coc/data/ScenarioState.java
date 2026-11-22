package com.coc.data;

import net.minecraft.nbt.CompoundTag;

/** 当前剧本进度（世界级，同一时间只跑一个剧本）。 */
public final class ScenarioState {
    public String id = "";
    public int index = -1;
    public boolean active;
    public long startedGameTime;

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id == null ? "" : id);
        tag.putInt("index", index);
        tag.putBoolean("active", active);
        tag.putLong("started", startedGameTime);
        return tag;
    }

    public static ScenarioState fromNbt(CompoundTag tag) {
        ScenarioState s = new ScenarioState();
        s.id = tag.getString("id");
        s.index = tag.getInt("index");
        s.active = tag.getBoolean("active");
        s.startedGameTime = tag.getLong("started");
        return s;
    }

    public void reset() {
        id = "";
        index = -1;
        active = false;
        startedGameTime = 0;
    }
}
