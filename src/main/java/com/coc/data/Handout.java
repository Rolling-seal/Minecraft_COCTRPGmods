package com.coc.data;

import net.minecraft.nbt.CompoundTag;

/** 一条线索/手书。 */
public record Handout(String title, String text, long gameTime) {
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("title", title == null ? "" : title);
        tag.putString("text", text == null ? "" : text);
        tag.putLong("time", gameTime);
        return tag;
    }

    public static Handout fromNbt(CompoundTag tag) {
        return new Handout(tag.getString("title"), tag.getString("text"), tag.getLong("time"));
    }
}
