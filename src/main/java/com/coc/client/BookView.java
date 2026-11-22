package com.coc.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/** KP 剧情之书数据：主线幕次与支线全文。 */
public final class BookView {
    public String scenarioId = "";
    public String title = "";
    public String description = "";
    public boolean active;
    public int index;
    public final List<Act> acts = new ArrayList<>();
    public final List<Side> sides = new ArrayList<>();

    public static final class Act {
        public int act;
        public String event = "";
        public String title = "";
        public String summary = "";
        public String trigger = "";
        public String description = "";
        public final List<Option> options = new ArrayList<>();
        public final List<String> sideTitles = new ArrayList<>();
    }

    public static final class Side {
        public String id = "";
        public int act;
        public String title = "";
        public String trigger = "";
        public String description = "";
        public final List<Option> options = new ArrayList<>();
    }

    public static final class Option {
        public String text = "";
        public String skill = "";
        public String skillName = "";
        public String difficulty = "";
        public String outcome = "";
        public String success = "";
        public String failure = "";
        public String clue = "";
        public String successClue = "";
        public String failureClue = "";
    }

    public static BookView fromNbt(CompoundTag tag) {
        BookView view = new BookView();
        if (tag == null) return view;
        view.scenarioId = tag.getString("scenario_id");
        view.title = tag.getString("title");
        view.description = tag.getString("description");
        view.active = tag.getBoolean("active");
        view.index = tag.getInt("index");
        ListTag acts = tag.getList("acts", Tag.TAG_COMPOUND);
        for (int i = 0; i < acts.size(); i++) {
            CompoundTag t = acts.getCompound(i);
            Act a = new Act();
            a.act = t.getInt("act");
            a.event = t.getString("event");
            a.title = t.getString("title");
            a.summary = t.getString("summary");
            a.trigger = t.getString("trigger");
            a.description = t.getString("description");
            readOptions(t.getList("options", Tag.TAG_COMPOUND), a.options);
            ListTag sides = t.getList("side_titles", Tag.TAG_STRING);
            for (int j = 0; j < sides.size(); j++) a.sideTitles.add(sides.getString(j));
            view.acts.add(a);
        }
        ListTag sides = tag.getList("sides", Tag.TAG_COMPOUND);
        for (int i = 0; i < sides.size(); i++) {
            CompoundTag t = sides.getCompound(i);
            Side s = new Side();
            s.id = t.getString("id");
            s.act = t.getInt("act");
            s.title = t.getString("title");
            s.trigger = t.getString("trigger");
            s.description = t.getString("description");
            readOptions(t.getList("options", Tag.TAG_COMPOUND), s.options);
            view.sides.add(s);
        }
        return view;
    }

    private static void readOptions(ListTag list, List<Option> out) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Option o = new Option();
            o.text = t.getString("text");
            o.skill = t.getString("skill");
            o.skillName = t.getString("skill_name");
            o.difficulty = t.getString("difficulty");
            o.outcome = t.getString("outcome");
            o.success = t.getString("success");
            o.failure = t.getString("failure");
            o.clue = t.getString("clue");
            o.successClue = t.getString("success_clue");
            o.failureClue = t.getString("failure_clue");
            out.add(o);
        }
    }
}
