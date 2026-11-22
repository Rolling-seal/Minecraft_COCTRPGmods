package com.coc.client;

import net.minecraft.nbt.CompoundTag;

/** 玩家幕布内容：一幕的标题与介绍，不含任何选项。 */
public final class CurtainView {
    public String scenario = "";
    public int act;
    public int total;
    public String title = "";
    public String description = "";

    public static CurtainView fromNbt(CompoundTag tag) {
        CurtainView view = new CurtainView();
        if (tag == null) return view;
        view.scenario = tag.getString("scenario");
        view.act = tag.getInt("act");
        view.total = tag.getInt("total");
        view.title = tag.getString("title");
        view.description = tag.getString("description");
        return view;
    }
}
