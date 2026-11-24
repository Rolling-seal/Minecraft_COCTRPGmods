package com.coc.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/** 玩家幕布内容：一幕的标题与介绍，不含任何选项。 */
public final class CurtainView {
    public String scenario = "";
    public int act;
    public int total;
    public String title = "";
    public String description = "";
    public final List<String> options = new ArrayList<>();

    public static CurtainView fromNbt(CompoundTag tag) {
        CurtainView view = new CurtainView();
        if (tag == null) return view;
        view.scenario = tag.getString("scenario");
        view.act = tag.getInt("act");
        view.total = tag.getInt("total");
        view.title = tag.getString("title");
        view.description = tag.getString("description");
        ListTag options = tag.getList("options", Tag.TAG_STRING);
        for (int i = 0; i < options.size(); i++) {
            String text = options.getString(i);
            if (text != null && !text.isBlank()) view.options.add(text);
        }
        return view;
    }
}
