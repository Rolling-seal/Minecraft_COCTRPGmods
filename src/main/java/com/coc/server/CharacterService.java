package com.coc.server;

import com.coc.data.CharacterSheet;
import com.coc.data.CocWorldData;
import com.coc.events.CocEventService;
import com.coc.net.CocNetwork;
import com.coc.rules.CharacterRules;
import com.coc.rules.Occupation;
import com.coc.rules.OccupationRegistry;
import com.coc.rules.SkillDef;
import com.coc.rules.SkillRegistry;
import com.coc.rules.Stats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** 角色创建与查询。 */
public final class CharacterService {
    private CharacterService() {
    }

    public static void requestSync(ServerPlayer player) {
        if (player == null) return;
        CharacterSheet sheet = CocData.sheet(player);
        if (sheet == null) {
            CocNetwork.sendToPlayer(player, new com.coc.net.packet.S2CSyncSheet(
                    null, true, player.getUUID(), false));
        } else {
            CocNetwork.syncSheet(player, sheet);
        }
        GlobalSettingsService.send(player);
    }

    public static void create(ServerPlayer player, CompoundTag tag) {
        if (player == null || tag == null) return;
        CocWorldData world = CocData.get(player.getServer());
        CharacterSheet existing = world.getSheet(player.getUUID());
        if (existing != null) {
            player.sendSystemMessage(Component.literal("你已经有一张角色卡了。")
                    .withStyle(ChatFormatting.YELLOW));
            CocNetwork.syncSheet(player, existing);
            return;
        }
        int age = Math.max(15, Math.min(89, tag.contains("age") ? tag.getInt("age") : 25));
        String occupationId = tag.getString("occupation");
        Occupation occupation = OccupationRegistry.resolve(occupationId);
        if (occupation == null) {
            player.sendSystemMessage(Component.literal("未知职业：" + occupationId)
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (!tag.contains("stats")) {
            player.sendSystemMessage(Component.literal("角色数据缺少属性。").withStyle(ChatFormatting.RED));
            return;
        }
        Stats base = Stats.fromNbt(tag.getCompound("stats"));
        Stats finalStats = CharacterRules.applyAge(base, age, player.serverLevel().random, true);

        CompoundTag allocation = tag.getCompound("skills");
        int totalAdded = 0;
        for (String key : allocation.getAllKeys()) {
            totalAdded += Math.max(0, allocation.getInt(key));
        }
        com.coc.config.CocServerConfig cfg = com.coc.config.CocServerConfig.get();
        int occMax = cfg.creationOccupationPoints >= 0
                ? cfg.creationOccupationPoints : occupation.points(finalStats);
        int intMax = cfg.creationInterestPoints >= 0
                ? cfg.creationInterestPoints : Math.max(0, finalStats.intel * 2);
        int maxPoints = cfg.allowCreationPoints ? occMax + intMax : 0;
        if (cfg.creationTotalPoints >= 0) maxPoints = Math.min(maxPoints, cfg.creationTotalPoints);
        if (totalAdded > maxPoints) {
            player.sendSystemMessage(Component.literal("技能点超出上限（" + totalAdded + " / " + maxPoints + "）。")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        CharacterSheet sheet = new CharacterSheet(player.getUUID(), player.getGameProfile().getName());
        sheet.age = age;
        sheet.occupationId = occupation.id;
        sheet.occupationName = occupation.name;
        sheet.stats.str = finalStats.str;
        sheet.stats.con = finalStats.con;
        sheet.stats.siz = finalStats.siz;
        sheet.stats.dex = finalStats.dex;
        sheet.stats.app = finalStats.app;
        sheet.stats.intel = finalStats.intel;
        sheet.stats.pow = finalStats.pow;
        sheet.stats.edu = finalStats.edu;
        sheet.stats.luck = finalStats.luck;

        for (SkillDef def : SkillRegistry.all()) {
            int baseValue = def.baseValue(finalStats.dex, finalStats.edu);
            int added = Math.max(0, allocation.getInt(def.id));
            sheet.setSkill(def.id, Math.min(99, baseValue + added));
        }
        sheet.mythos = 0;
        sheet.maxSan = 99;
        sheet.san = Math.min(99, finalStats.pow);
        sheet.recalculateDerived();
        sheet.hp = sheet.maxHp;
        sheet.mp = sheet.maxMp;
        sheet.dying = false;
        sheet.lastSanDay = CocData.day(player);

        CompoundTag backstory = tag.getCompound("backstory");
        for (String key : CharacterSheet.BACKSTORY_KEYS) {
            sheet.backstory.put(key, backstory.getString(key));
        }
        sheet.notes = tag.getString("notes");

        world.putSheet(sheet);
        CocNetwork.syncSheet(player, sheet);
        CocNetwork.openScreen(player, "sheet", player.getUUID(), false);
        player.sendSystemMessage(Component.literal(
                "角色创建完成：" + sheet.name + " · " + occupation.name + " · SAN " + sheet.san + "/" + sheet.maxSan)
                .withStyle(ChatFormatting.GOLD));
    }

    /** 按名字找在线或离线角色卡。 */
    public static CharacterSheet findByName(MinecraftServer server, String name) {
        if (server == null || name == null || name.isBlank()) return null;
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) return CocData.sheet(online);
        for (CharacterSheet sheet : CocData.get(server).allSheets()) {
            if (sheet.name != null && sheet.name.equalsIgnoreCase(name)) return sheet;
        }
        return null;
    }

    /** 永久删除角色卡。返回被删除的角色卡，找不到返回 null。 */
    public static CharacterSheet delete(MinecraftServer server, UUID target) {
        if (server == null || target == null) return null;
        CocWorldData world = CocData.get(server);
        CharacterSheet sheet = world.getSheet(target);
        if (sheet == null) return null;
        ServerPlayer online = server.getPlayerList().getPlayer(target);
        if (online != null) {
            CocEventService.clear(online);
            CheckService.clear(online);
        }
        world.removeSheet(target);
        if (online != null) {
            CocNetwork.sendToPlayer(online, new com.coc.net.packet.S2CSyncSheet(
                    null, true, target, false));
            online.sendSystemMessage(Component.literal("你的角色卡已被 KP 删除，可以重新创建角色。")
                    .withStyle(ChatFormatting.YELLOW));
        }
        return sheet;
    }

    public static void openSheet(ServerPlayer viewer, UUID target, boolean editable) {
        if (viewer == null) return;
        CharacterSheet sheet = CocData.get(viewer.getServer()).getSheet(target);
        if (sheet == null) {
            viewer.sendSystemMessage(Component.literal("目标还没有角色卡。").withStyle(ChatFormatting.YELLOW));
            return;
        }
        CocNetwork.syncOther(viewer, sheet);
        CocNetwork.openScreen(viewer, "sheet", target, editable);
    }
}
