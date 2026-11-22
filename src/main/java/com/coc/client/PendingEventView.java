package com.coc.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/** 客户端事件卡视图。 */
public final class PendingEventView {
    public String id = "";
    public String title = "";
    public String description = "";
    public int timeout;
    public boolean fromScenario;
    public int remainingTicks;
    public final List<Option> options = new ArrayList<>();

    public static final class Option {
        public String text = "";
        public String skill = "";
        public String difficulty = "";
    }

    public static PendingEventView fromNbt(CompoundTag tag) {
        PendingEventView view = new PendingEventView();
        if (tag == null) return view;
        view.id = tag.getString("id");
        view.title = tag.getString("title");
        view.description = tag.getString("description");
        view.timeout = tag.getInt("timeout");
        view.remainingTicks = view.timeout;
        view.fromScenario = tag.getBoolean("from_scenario");
        ListTag list = tag.getList("options", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag o = list.getCompound(i);
            Option option = new Option();
            option.text = o.getString("text");
            option.skill = o.getString("skill");
            option.difficulty = o.getString("difficulty");
            view.options.add(option);
        }
        return view;
    }
}
