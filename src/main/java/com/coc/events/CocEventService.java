package com.coc.events;

import com.coc.config.CocServerConfig;
import com.coc.data.CharacterSheet;
import com.coc.data.CocWorldData;
import com.coc.net.CocNetwork;
import com.coc.net.packet.S2CEventOffer;
import com.coc.net.packet.S2CEventResult;
import com.coc.rules.DiceRoller;
import com.coc.rules.Difficulty;
import com.coc.rules.SkillDef;
import com.coc.rules.SkillRegistry;
import com.coc.server.CocData;
import com.coc.server.EffectApplier;
import com.coc.server.HandoutService;
import com.coc.server.SanityService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 事件调度与结算：随机事件、KP 指定事件、事件卡、技能检定后果。 */
public final class CocEventService {
    private static final Map<UUID, PendingOffer> PENDING = new HashMap<>();
    private static long lastRandomTick = -1;

    private CocEventService() {
    }

    public static final class PendingOffer {
        public final EventDef event;
        public final long deadline;
        public final boolean fromScenario;
        public final long offeredAt;

        PendingOffer(EventDef event, long deadline, boolean fromScenario, long offeredAt) {
            this.event = event;
            this.deadline = deadline;
            this.fromScenario = fromScenario;
            this.offeredAt = offeredAt;
        }
    }

    public static PendingOffer pending(ServerPlayer player) {
        return player == null ? null : PENDING.get(player.getUUID());
    }

    public static boolean offer(ServerPlayer player, EventDef def, boolean force, boolean fromScenario) {
        if (player == null || def == null) return false;
        CharacterSheet sheet = CocData.sheet(player);
        if (sheet == null) {
            player.sendSystemMessage(Component.literal("目标还没有角色卡，无法接收事件。")
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }
        if (PENDING.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.literal("你还有一个未处理的事件。").withStyle(ChatFormatting.YELLOW));
            return false;
        }
        long now = player.serverLevel().getGameTime();
        CocWorldData world = CocData.get(player.getServer());
        if (!force) {
            if (def.once && world.wasFiredOnce(def.id)) return false;
            if (def.cooldown > 0 && world.eventOnCooldown(def.id, now)) return false;
            if (!conditionsMatch(player, sheet, def)) return false;
        }
        int timeout = Math.max(200, CocServerConfig.get().eventTimeoutTicks);
        long deadline = now + timeout;
        PENDING.put(player.getUUID(), new PendingOffer(def, deadline, fromScenario, now));
        CocNetwork.sendToPlayer(player, new S2CEventOffer(toOfferTag(def, timeout, fromScenario)));
        player.sendSystemMessage(Component.literal("【事件】" + def.title)
                .withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    public static void choose(ServerPlayer player, int index) {
        if (player == null) return;
        PendingOffer pending = PENDING.get(player.getUUID());
        if (pending == null) {
            player.sendSystemMessage(Component.literal("没有待处理的事件。").withStyle(ChatFormatting.YELLOW));
            return;
        }
        PENDING.remove(player.getUUID());
        EventDef def = pending.event;
        if (def.options == null || def.options.isEmpty()) {
            player.sendSystemMessage(Component.literal("事件没有选项。").withStyle(ChatFormatting.RED));
            return;
        }
        int idx = Math.max(0, Math.min(def.options.size() - 1, index));
        EventDef.Option option = def.options.get(idx);
        resolve(player, def, option, pending.fromScenario);
    }

    private static void resolve(ServerPlayer player, EventDef def, EventDef.Option option,
                                boolean fromScenario) {
        CharacterSheet sheet = CocData.sheet(player);
        if (sheet == null) return;
        RandomSource random = player.serverLevel().random;
        EventDef.Outcome outcome;
        String checkText = "";
        EventDef.Outcome successOutcome = option.success;
        EventDef.Outcome failureOutcome = option.failure;

        if (option.skill != null && !option.skill.isBlank()) {
            SkillDef skill = SkillRegistry.resolve(option.skill);
            if (skill == null) {
                player.sendSystemMessage(Component.literal("事件技能不存在：" + option.skill)
                        .withStyle(ChatFormatting.RED));
                return;
            }
            Difficulty difficulty = Difficulty.byName(option.difficulty);
            int bonus = option.bonus == null ? 0 : option.bonus;
            int value = sheet.skill(skill.id);
            DiceRoller.CheckResult check = DiceRoller.check(value, difficulty, bonus, random);
            if (check.success()) sheet.markImprovement(skill.id);
            checkText = skill.name + " 检定 d100=" + check.percentile().roll()
                    + " / " + difficulty.target(value) + " → " + check.level().cn();
            outcome = check.success() ? successOutcome : failureOutcome;
            if (outcome == null) outcome = option.outcome;
            if (outcome == null) {
                outcome = new EventDef.Outcome();
                outcome.text = check.success() ? "你成功了，但空气里仍有什么东西在动。" : "你失败了，事情开始失控。";
            }
        } else {
            outcome = option.outcome;
            if (outcome == null) {
                outcome = new EventDef.Outcome();
                outcome.text = option.text == null ? "……" : option.text;
                outcome.effects = option.effects;
            }
        }

        List<String> summaries = new ArrayList<>();
        if (option.skill == null || option.skill.isBlank()) {
            applyEffects(player, sheet, option.effects, random, summaries);
        }
        applyEffects(player, sheet, outcome.effects, random, summaries);
        if (outcome.phobia != null && !outcome.phobia.isBlank()) {
            SanityService.addPhobia(sheet, outcome.phobia);
            summaries.add("恐惧症：" + outcome.phobia);
        }
        if (outcome.mania != null && !outcome.mania.isBlank()) {
            SanityService.addMania(sheet, outcome.mania);
            summaries.add("狂躁症：" + outcome.mania);
        }
        if (outcome.handout != null && !outcome.handout.isBlank()) {
            HandoutService.send(player, def.title, outcome.handout);
            summaries.add("获得线索");
        }

        String text = outcome.text == null || outcome.text.isBlank()
                ? (option.text == null ? "……" : option.text) : outcome.text;
        String next = outcome.next != null && !outcome.next.isBlank()
                ? outcome.next : option.next;
        boolean closed = next == null || next.isBlank();

        CompoundTag result = new CompoundTag();
        result.putString("title", def.title);
        result.putString("text", text);
        result.putString("effects", String.join("；", summaries));
        result.putString("check", checkText);
        result.putBoolean("closed", closed);
        CocNetwork.sendToPlayer(player, new S2CEventResult(result));
        player.sendSystemMessage(Component.literal("【" + def.title + "】" + text)
                .withStyle(ChatFormatting.GRAY));
        if (!summaries.isEmpty()) {
            player.sendSystemMessage(Component.literal(String.join("；", summaries))
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }

        long now = player.serverLevel().getGameTime();
        CocWorldData world = CocData.get(player.getServer());
        if (def.cooldown > 0) world.setEventCooldown(def.id, now + def.cooldown);
        if (def.once) world.markFiredOnce(def.id);
        if (!fromScenario) world.markRandomEvent(player.getUUID(), now);
        CocNetwork.syncSheet(player, sheet);

        if (next != null && !next.isBlank()) {
            EventDef nextDef = EventRegistry.event(next);
            if (nextDef != null) {
                offer(player, nextDef, true, fromScenario);
            }
        }
    }

    private static void applyEffects(ServerPlayer player, CharacterSheet sheet,
                                     List<EventDef.Effect> effects, RandomSource random,
                                     List<String> summaries) {
        if (effects == null) return;
        for (EventDef.Effect effect : effects) {
            String summary = EffectApplier.apply(player, sheet, effect, random);
            if (summary != null && !summary.isBlank()) summaries.add(summary);
        }
    }

    private static CompoundTag toOfferTag(EventDef def, int timeout, boolean fromScenario) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", def.id);
        tag.putString("title", def.title);
        tag.putString("description", def.description == null ? "" : def.description);
        tag.putInt("timeout", timeout);
        tag.putBoolean("from_scenario", fromScenario);
        ListTag options = new ListTag();
        for (EventDef.Option option : def.options) {
            CompoundTag o = new CompoundTag();
            o.putString("text", option.text == null ? "" : option.text);
            String skillName = "";
            String diffName = "";
            if (option.skill != null && !option.skill.isBlank()) {
                SkillDef skill = SkillRegistry.resolve(option.skill);
                skillName = skill == null ? option.skill : skill.name;
                diffName = Difficulty.byName(option.difficulty).cn();
            }
            o.putString("skill", skillName);
            o.putString("difficulty", diffName);
            options.add(o);
        }
        tag.put("options", options);
        return tag;
    }

    public static boolean conditionsMatch(ServerPlayer player, CharacterSheet sheet, EventDef def) {
        EventDef.Conditions c = def.conditions;
        if (c == null) return true;
        if (c.minSan != null && sheet.san < c.minSan) return false;
        if (c.maxSan != null && sheet.san > c.maxSan) return false;
        if (c.minMythos != null && sheet.mythos < c.minMythos) return false;
        String dimension = player.serverLevel().dimension().location().toString();
        if (c.dimensions != null && !c.dimensions.isEmpty()
                && c.dimensions.stream().noneMatch(d -> d.equalsIgnoreCase(dimension))) return false;
        if (c.daytime != null && !c.daytime.isBlank() && !"any".equalsIgnoreCase(c.daytime)) {
            boolean day = player.serverLevel().isDay();
            if ("day".equalsIgnoreCase(c.daytime) && !day) return false;
            if ("night".equalsIgnoreCase(c.daytime) && day) return false;
        }
        if (c.biomes != null && !c.biomes.isEmpty()) {
            String biome = player.serverLevel().getBiome(player.blockPosition()).unwrapKey()
                    .map(k -> k.location().toString()).orElse("");
            if (c.biomes.stream().noneMatch(b -> b.equalsIgnoreCase(biome))) return false;
        }
        return true;
    }

    public static EventDef randomEventFor(ServerPlayer player, boolean includeKpOnly) {
        CharacterSheet sheet = CocData.sheet(player);
        if (sheet == null) return null;
        CocWorldData world = CocData.get(player.getServer());
        long now = player.serverLevel().getGameTime();
        List<EventDef> candidates = new ArrayList<>();
        int total = 0;
        for (EventDef def : EventRegistry.events()) {
            if (!includeKpOnly && def.kpOnly) continue;
            if (def.once && world.wasFiredOnce(def.id)) continue;
            if (def.cooldown > 0 && world.eventOnCooldown(def.id, now)) continue;
            if (def.weight <= 0) continue;
            if (!conditionsMatch(player, sheet, def)) continue;
            candidates.add(def);
            total += def.weight;
        }
        if (candidates.isEmpty() || total <= 0) return null;
        int pick = player.serverLevel().random.nextInt(total);
        for (EventDef def : candidates) {
            pick -= def.weight;
            if (pick < 0) return def;
        }
        return candidates.get(candidates.size() - 1);
    }

    public static void trigger(ServerPlayer player, String id) {
        EventDef def = EventRegistry.event(id);
        if (def == null) {
            player.sendSystemMessage(Component.literal("未知事件：" + id).withStyle(ChatFormatting.RED));
            return;
        }
        if (!offer(player, def, true, false)) {
            player.sendSystemMessage(Component.literal("事件未能触发。").withStyle(ChatFormatting.YELLOW));
        }
    }

    public static void triggerRandom(ServerPlayer player, boolean includeKpOnly) {
        EventDef def = randomEventFor(player, includeKpOnly);
        if (def == null) {
            player.sendSystemMessage(Component.literal("没有符合条件的事件。").withStyle(ChatFormatting.YELLOW));
            return;
        }
        offer(player, def, true, false);
    }

    public static void resolveAll(MinecraftServer server) {
        for (UUID uuid : new ArrayList<>(PENDING.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.sendSystemMessage(Component.literal("事件已由 KP 自动结算。")
                        .withStyle(ChatFormatting.GRAY));
                choose(player, 0);
            } else {
                PENDING.remove(uuid);
            }
        }
    }

    public static void clear(ServerPlayer player) {
        if (player != null) PENDING.remove(player.getUUID());
    }

    public static void tick(MinecraftServer server) {
        if (server == null) return;
        long now = server.overworld().getGameTime();
        for (UUID uuid : new ArrayList<>(PENDING.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            PendingOffer offer = PENDING.get(uuid);
            if (offer == null) continue;
            if (player == null) {
                PENDING.remove(uuid);
                continue;
            }
            if (now >= offer.deadline) {
                player.sendSystemMessage(Component.literal("你犹豫了太久……")
                        .withStyle(ChatFormatting.DARK_GRAY));
                choose(player, 0);
            }
        }

        CocServerConfig config = CocServerConfig.get();
        if (!config.randomEventsEnabled) return;
        if (lastRandomTick < 0) {
            lastRandomTick = now;
            return;
        }
        int interval = Math.max(200, config.randomEventIntervalTicks);
        if (now - lastRandomTick < interval) return;
        lastRandomTick = now;

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        RandomSource random = server.overworld().random;
        for (int attempt = 0; attempt < Math.min(4, players.size()); attempt++) {
            ServerPlayer player = players.get(random.nextInt(players.size()));
            if (PENDING.containsKey(player.getUUID())) continue;
            CharacterSheet sheet = CocData.sheet(player);
            if (sheet == null) continue;
            CocWorldData world = CocData.get(server);
            if (now - world.lastRandomEvent(player.getUUID()) < Math.max(200, config.minPlayerEventIntervalTicks)) {
                continue;
            }
            EventDef def = randomEventFor(player, false);
            if (def != null && offer(player, def, true, false)) {
                world.markRandomEvent(player.getUUID(), now);
                return;
            }
        }
    }
}
