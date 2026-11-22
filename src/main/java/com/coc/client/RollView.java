package com.coc.client;

import net.minecraft.nbt.CompoundTag;

/** 客户端检定结果视图。 */
public final class RollView {
    public String title = "";
    public int roll;
    public int target = -1;
    public String level = "";
    public int levelColor = 0xFFFFFFFF;
    public boolean success;
    public String detail = "";
    public boolean canPush;
    public int luckNeeded;
    public boolean fumble;
    public boolean critical;
    public boolean canLuck;
    public String kind = "";
    public long receivedAt;

    public static RollView fromNbt(CompoundTag tag) {
        RollView view = new RollView();
        if (tag == null) return view;
        view.title = tag.getString("title");
        view.roll = tag.getInt("roll");
        view.target = tag.contains("target") ? tag.getInt("target") : -1;
        view.level = tag.getString("level");
        view.levelColor = tag.getInt("level_color");
        view.success = tag.getBoolean("success");
        view.detail = tag.getString("detail");
        view.canPush = tag.getBoolean("can_push");
        view.luckNeeded = tag.getInt("luck_needed");
        view.fumble = tag.getBoolean("fumble");
        view.critical = tag.getBoolean("critical");
        view.canLuck = tag.getBoolean("can_luck");
        view.kind = tag.getString("kind");
        view.receivedAt = System.currentTimeMillis();
        return view;
    }
}
