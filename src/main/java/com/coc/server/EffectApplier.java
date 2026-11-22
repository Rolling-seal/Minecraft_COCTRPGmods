package com.coc.server;

import com.coc.data.CharacterSheet;
import com.coc.events.EventDef;
import com.coc.net.CocNetwork;
import com.coc.rules.DiceRoller;
import com.coc.rules.SkillDef;
import com.coc.rules.SkillRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/** 事件效果执行器。新增效果类型时在此加分支，并在 EventRegistry 的白名单中登记。 */
public final class EffectApplier {
    private EffectApplier() {
    }

    public static String apply(ServerPlayer player, CharacterSheet sheet, EventDef.Effect effect,
                               RandomSource random) {
        if (player == null || sheet == null || effect == null || effect.type == null) return "";
        String type = effect.type.toLowerCase(Locale.ROOT);
        ServerLevel level = player.serverLevel();
        boolean sync = false;
        String summary = "";
        try {
            switch (type) {
                case "san" -> {
                    int delta = DiceRoller.evalNumber(effect.value, random);
                    if (delta < 0) {
                        SanityService.loseSan(player, sheet, -delta, effect.text, random);
                        summary = "SAN " + delta;
                    } else if (delta > 0) {
                        SanityService.gainSan(player, sheet, delta);
                        summary = "SAN +" + delta;
                    }
                }
                case "hp" -> {
                    int delta = DiceRoller.evalNumber(effect.value, random);
                    sheet.hp = Math.max(0, Math.min(sheet.maxHp, sheet.hp + delta));
                    if (sheet.hp <= 0) {
                        sheet.dying = true;
                        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS,
                                400, 1));
                        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                                400, 1));
                        summary = "HP 归零，濒死";
                    } else {
                        sheet.dying = false;
                        summary = "HP " + (delta >= 0 ? "+" : "") + delta;
                    }
                    sync = true;
                }
                case "mp" -> {
                    int delta = DiceRoller.evalNumber(effect.value, random);
                    sheet.mp = Math.max(0, Math.min(sheet.maxMp, sheet.mp + delta));
                    summary = "MP " + (delta >= 0 ? "+" : "") + delta;
                    sync = true;
                }
                case "luck" -> {
                    int delta = DiceRoller.evalNumber(effect.value, random);
                    sheet.stats.luck = Math.max(0, Math.min(99, sheet.stats.luck + delta));
                    summary = "幸运 " + (delta >= 0 ? "+" : "") + delta;
                    sync = true;
                }
                case "mythos" -> {
                    int delta = DiceRoller.evalNumber(effect.value, random);
                    sheet.mythos = Math.max(0, Math.min(99, sheet.mythos + delta));
                    sheet.recalculateDerived();
                    summary = "克苏鲁神话 " + (delta >= 0 ? "+" : "") + delta
                            + "（最大 SAN " + sheet.maxSan + "）";
                    sync = true;
                }
                case "skill" -> {
                    SkillDef def = SkillRegistry.resolve(effect.id);
                    if (def != null) {
                        int delta = DiceRoller.evalNumber(effect.value, random);
                        sheet.setSkill(def.id, sheet.skill(def.id) + delta);
                        summary = def.name + " " + (delta >= 0 ? "+" : "") + delta;
                        sync = true;
                    }
                }
                case "item" -> {
                    ResourceLocation id = ResourceLocation.tryParse(effect.id);
                    Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
                    if (item == Items.AIR) {
                        summary = "物品不存在: " + effect.id;
                    } else {
                        int count = Math.max(1, Math.min(64, effect.count));
                        ItemStack stack = new ItemStack(item, count);
                        if (!player.getInventory().add(stack)) player.drop(stack, false);
                        summary = "获得 " + stack.getHoverName().getString() + " ×" + count;
                    }
                }
                case "effect" -> {
                    ResourceLocation id = ResourceLocation.tryParse(effect.id);
                    MobEffect mobEffect = id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id);
                    if (mobEffect != null) {
                        int duration = Math.max(20, Math.min(24000, effect.duration));
                        player.addEffect(new MobEffectInstance(mobEffect, duration,
                                Math.max(0, Math.min(4, effect.amplifier))));
                        summary = "受到效果: " + mobEffect.getDisplayName().getString();
                    }
                }
                case "sound" -> {
                    ResourceLocation id = ResourceLocation.tryParse(effect.id);
                    SoundEvent sound = id == null ? null : BuiltInRegistries.SOUND_EVENT.get(id);
                    if (sound != null) {
                        level.playSound(null, player.blockPosition(), sound, SoundSource.AMBIENT, 1.0F, 1.0F);
                        summary = "声音: " + id;
                    }
                }
                case "message" -> {
                    player.sendSystemMessage(Component.literal(effect.text == null ? "" : effect.text)
                            .withStyle(ChatFormatting.GRAY));
                    summary = "提示";
                }
                case "handout" -> {
                    String title = effect.id == null || effect.id.isBlank() ? "线索" : effect.id;
                    HandoutService.send(player, title, effect.text);
                    summary = "线索: " + title;
                }
                case "broadcast" -> {
                    for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) {
                        p.sendSystemMessage(Component.literal(effect.text == null ? "" : effect.text)
                                .withStyle(ChatFormatting.DARK_PURPLE));
                    }
                    summary = "广播";
                }
                case "spawn" -> {
                    ResourceLocation id = ResourceLocation.tryParse(effect.id);
                    if (id != null) {
                        EntityType<?> entityType = EntityType.byString(id.toString()).orElse(null);
                        if (entityType != null && entityType != EntityType.PLAYER) {
                            int count = Math.max(1, Math.min(3, effect.count));
                            for (int i = 0; i < count; i++) {
                                Entity entity = entityType.create(level);
                                if (entity == null) continue;
                                double x = player.getX() + (random.nextDouble() - 0.5) * 8;
                                double z = player.getZ() + (random.nextDouble() - 0.5) * 8;
                                entity.moveTo(x, player.getY(), z,
                                        random.nextFloat() * 360.0F, 0.0F);
                                level.addFreshEntity(entity);
                            }
                            summary = "出现: " + effect.id + " ×" + count;
                        }
                    }
                }
                case "phobia" -> {
                    SanityService.addPhobia(sheet, effect.text);
                    summary = "恐惧症: " + effect.text;
                    sync = true;
                }
                case "mania" -> {
                    SanityService.addMania(sheet, effect.text);
                    summary = "狂躁症: " + effect.text;
                    sync = true;
                }
                default -> summary = "未知效果: " + type;
            }
        } catch (Exception e) {
            summary = "效果执行失败(" + type + "): " + e.getMessage();
        }
        if (sync) CocNetwork.syncSheet(player, sheet);
        return summary;
    }
}
