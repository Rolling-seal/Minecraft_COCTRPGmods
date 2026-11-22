package com.coc.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** KP 道具页：选中玩家背包的只读列表。 */
public final class ItemView {
    public UUID target;
    public String targetName = "";
    public final List<Entry> items = new ArrayList<>();

    public static final class Entry {
        public int slot;
        public String id = "";
        public String name = "";
        public int count;
    }

    public static ItemView fromNbt(CompoundTag tag) {
        ItemView view = new ItemView();
        if (tag == null) return view;
        if (tag.contains("target")) view.target = tag.getUUID("target");
        view.targetName = tag.getString("target_name");
        ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Entry e = new Entry();
            e.slot = t.getInt("slot");
            e.id = t.getString("id");
            e.name = t.getString("name");
            e.count = t.getInt("count");
            view.items.add(e);
        }
        return view;
    }

    public Entry selected(int slot) {
        for (Entry e : items) if (e.slot == slot) return e;
        return null;
    }
}
