package com.coc.command;

import com.coc.config.CocServerConfig;
import com.coc.data.CharacterSheet;
import com.coc.data.CocWorldData;
import com.coc.item.CocItems;
import com.coc.events.EventRegistry;
import com.coc.events.ScenarioDef;
import com.coc.events.ScenarioManager;
import com.coc.kp.KpManager;
import com.coc.net.CocNetwork;
import com.coc.rules.Difficulty;
import com.coc.rules.SkillDef;
import com.coc.rules.SkillRegistry;
import com.coc.server.CharacterService;
import com.coc.server.CheckService;
import com.coc.server.CocData;
import com.coc.server.GrowthService;
import com.coc.server.HandoutService;
import com.coc.server.SanityService;
import com.coc.server.SelfTest;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** /coc 命令树。玩家命令与 KP 命令分开授权。 */
public final class CocCommands {
    private static final SuggestionProvider<CommandSourceStack> SKILLS = (ctx, builder) -> {
        for (SkillDef def : SkillRegistry.all()) builder.suggest(def.id);
        return builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> DIFFICULTIES = (ctx, builder) -> {
        for (Difficulty d : Difficulty.values()) builder.suggest(d.name().toLowerCase());
        return builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> SCENARIOS = (ctx, builder) -> {
        for (ScenarioDef def : EventRegistry.scenarios()) builder.suggest(def.id);
        return builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> PLAYERS = (ctx, builder) -> {
        for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            builder.suggest(p.getGameProfile().getName());
        }
        return builder.buildFuture();
    };

    private CocCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("coc")
                .then(Commands.literal("sheet")
                        .executes(ctx -> sheet(ctx, null))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(PLAYERS)
                                .executes(ctx -> sheet(ctx, StringArgumentType.getString(ctx, "player")))))
                .then(Commands.literal("create").executes(CocCommands::create))
                .then(Commands.literal("journal").executes(CocCommands::journal))
                .then(Commands.literal("settings").executes(CocCommands::settings))
                .then(Commands.literal("sancheck")
                        .requires(KpManager::isKp)
                        .then(Commands.argument("successLoss", StringArgumentType.word())
                                .then(Commands.argument("failLoss", StringArgumentType.word())
                                        .executes(ctx -> sancheck(ctx, null))
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests(PLAYERS)
                                                .executes(ctx -> sancheck(ctx,
                                                        StringArgumentType.getString(ctx, "player")))))))
                .then(Commands.literal("san")
                        .requires(KpManager::isKp)
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 99))
                                        .executes(ctx -> statSet(ctx, "san_set", IntegerArgumentType.getInteger(ctx, "value"), null))
                                        .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                                .executes(ctx -> statSet(ctx, "san_set",
                                                        IntegerArgumentType.getInteger(ctx, "value"),
                                                        StringArgumentType.getString(ctx, "player"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("value", IntegerArgumentType.integer(-99, 99))
                                        .executes(ctx -> statSet(ctx, "san_add", IntegerArgumentType.getInteger(ctx, "value"), null))
                                        .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                                .executes(ctx -> statSet(ctx, "san_add",
                                                        IntegerArgumentType.getInteger(ctx, "value"),
                                                        StringArgumentType.getString(ctx, "player"))))))
                        .then(Commands.literal("sub")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 99))
                                        .executes(ctx -> statSet(ctx, "san_sub", IntegerArgumentType.getInteger(ctx, "value"), null))
                                        .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                                .executes(ctx -> statSet(ctx, "san_sub",
                                                        IntegerArgumentType.getInteger(ctx, "value"),
                                                        StringArgumentType.getString(ctx, "player")))))))
                .then(Commands.literal("hp")
                        .requires(KpManager::isKp)
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 99))
                                        .executes(ctx -> statSet(ctx, "hp_set", IntegerArgumentType.getInteger(ctx, "value"), null))
                                        .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                                .executes(ctx -> statSet(ctx, "hp_set",
                                                        IntegerArgumentType.getInteger(ctx, "value"),
                                                        StringArgumentType.getString(ctx, "player"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("value", IntegerArgumentType.integer(-99, 99))
                                        .executes(ctx -> statSet(ctx, "hp_add", IntegerArgumentType.getInteger(ctx, "value"), null))
                                        .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                                .executes(ctx -> statSet(ctx, "hp_add",
                                                        IntegerArgumentType.getInteger(ctx, "value"),
                                                        StringArgumentType.getString(ctx, "player"))))))
                        .then(Commands.literal("sub")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 99))
                                        .executes(ctx -> statSet(ctx, "hp_sub", IntegerArgumentType.getInteger(ctx, "value"), null))
                                        .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                                .executes(ctx -> statSet(ctx, "hp_sub",
                                                        IntegerArgumentType.getInteger(ctx, "value"),
                                                        StringArgumentType.getString(ctx, "player")))))))
                .then(Commands.literal("mp")
                        .requires(KpManager::isKp)
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 99))
                                        .executes(ctx -> statSet(ctx, "mp_set", IntegerArgumentType.getInteger(ctx, "value"), null))
                                        .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                                .executes(ctx -> statSet(ctx, "mp_set",
                                                        IntegerArgumentType.getInteger(ctx, "value"),
                                                        StringArgumentType.getString(ctx, "player"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("value", IntegerArgumentType.integer(-99, 99))
                                        .executes(ctx -> statSet(ctx, "mp_add", IntegerArgumentType.getInteger(ctx, "value"), null))
                                        .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                                .executes(ctx -> statSet(ctx, "mp_add",
                                                        IntegerArgumentType.getInteger(ctx, "value"),
                                                        StringArgumentType.getString(ctx, "player")))))))
                .then(Commands.literal("luckstat")
                        .requires(KpManager::isKp)
                        .then(Commands.argument("value", IntegerArgumentType.integer(-99, 99))
                                .executes(ctx -> statSet(ctx, "luck_add", IntegerArgumentType.getInteger(ctx, "value"), null))
                                .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                        .executes(ctx -> statSet(ctx, "luck_add",
                                                IntegerArgumentType.getInteger(ctx, "value"),
                                                StringArgumentType.getString(ctx, "player"))))))
                .then(Commands.literal("mythos")
                        .requires(KpManager::isKp)
                        .then(Commands.literal("add")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 20))
                                        .executes(ctx -> mythos(ctx, IntegerArgumentType.getInteger(ctx, "value"), null))
                                        .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                                .executes(ctx -> mythos(ctx, IntegerArgumentType.getInteger(ctx, "value"),
                                                        StringArgumentType.getString(ctx, "player")))))))
                .then(Commands.literal("skill")
                        .requires(KpManager::isKp)
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .suggests(SKILLS)
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 99))
                                        .executes(ctx -> setSkill(ctx, IntegerArgumentType.getInteger(ctx, "value"), null))
                                        .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                                .executes(ctx -> setSkill(ctx, IntegerArgumentType.getInteger(ctx, "value"),
                                                        StringArgumentType.getString(ctx, "player")))))))
                .then(Commands.literal("growth")
                        .requires(KpManager::isKp)
                        .executes(ctx -> growth(ctx, null))
                        .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                .executes(ctx -> growth(ctx, StringArgumentType.getString(ctx, "player")))))
                .then(Commands.literal("givepanel")
                        .requires(KpManager::isKp)
                        .executes(ctx -> givePanel(ctx, null))
                        .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                .executes(ctx -> givePanel(ctx, StringArgumentType.getString(ctx, "player")))))
                .then(Commands.literal("delete")
                        .requires(KpManager::isKp)
                        .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                .executes(ctx -> deleteCharacter(ctx,
                                        StringArgumentType.getString(ctx, "player"), false))
                                .then(Commands.literal("confirm")
                                        .executes(ctx -> deleteCharacter(ctx,
                                                StringArgumentType.getString(ctx, "player"), true)))))
                .then(Commands.literal("scenario")
                        .requires(KpManager::isKp)
                        .then(Commands.literal("list").executes(CocCommands::scenarioList))
                        .then(Commands.literal("start")
                                .then(Commands.argument("id", StringArgumentType.word()).suggests(SCENARIOS)
                                        .executes(CocCommands::scenarioStart)))
                        .then(Commands.literal("next").executes(CocCommands::scenarioNext))
                        .then(Commands.literal("stop").executes(CocCommands::scenarioStop))
                        .then(Commands.literal("act")
                                .then(Commands.argument("id", StringArgumentType.word()).suggests(SCENARIOS)
                                        .then(Commands.argument("act", IntegerArgumentType.integer(1, 99))
                                                .executes(CocCommands::scenarioAct)))))
                .then(Commands.literal("handout")
                        .requires(KpManager::isKp)
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(CocCommands::handoutAll)))
                .then(Commands.literal("handoutto")
                        .requires(KpManager::isKp)
                        .then(Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS)
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(CocCommands::handoutTo))))
                .then(Commands.literal("broadcast")
                        .requires(KpManager::isKp)
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(CocCommands::broadcast)))
                .then(Commands.literal("reload")
                        .requires(KpManager::isKp)
                        .executes(CocCommands::reload))
                .then(Commands.literal("debug")
                        .requires(KpManager::isKp)
                        .then(Commands.literal("selftest").executes(CocCommands::selftest)))
                .then(Commands.literal("kp")
                        .requires(KpManager::isKp)
                        .then(Commands.literal("list").executes(CocCommands::kpList)))
        );
    }

    private static ServerPlayer self(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getEntity() instanceof ServerPlayer p ? p : null;
    }

    private static ServerPlayer target(CommandContext<CommandSourceStack> ctx, String name) {
        if (name == null || name.isBlank()) return self(ctx);
        return ctx.getSource().getServer().getPlayerList().getPlayerByName(name);
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendFailure(Component.literal(message));
        return 0;
    }

    private static int ok(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int sheet(CommandContext<CommandSourceStack> ctx, String name) {
        ServerPlayer viewer = self(ctx);
        if (viewer == null) return fail(ctx, "只有玩家能打开角色卡。");
        ServerPlayer target = target(ctx, name);
        if (target == null) return fail(ctx, "找不到玩家：" + name);
        boolean other = !target.getUUID().equals(viewer.getUUID());
        if (other && !KpManager.isKp(viewer)) return fail(ctx, "只有 KP 能查看别人的角色卡。");
        CharacterService.openSheet(viewer, target.getUUID(), false);
        return 1;
    }

    private static int create(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = self(ctx);
        if (player == null) return fail(ctx, "只有玩家能创建角色。");
        CharacterSheet sheet = CocData.sheet(player);
        if (sheet != null) {
            CocNetwork.openScreen(player, "sheet", player.getUUID(), false);
            return ok(ctx, "你已经有角色卡了。");
        }
        CocNetwork.openScreen(player, "creation", player.getUUID(), true);
        return 1;
    }

    private static int roll(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = self(ctx);
        if (player == null) return fail(ctx, "只有玩家能掷骰。");
        CheckService.handleRoll(player, "dice", StringArgumentType.getString(ctx, "expr"), "", 0, 0, 0);
        return 1;
    }

    private static int check(CommandContext<CommandSourceStack> ctx, String difficulty, int bonus) {
        ServerPlayer player = self(ctx);
        if (player == null) return fail(ctx, "只有玩家能检定。");
        String skill = StringArgumentType.getString(ctx, "skill");
        Difficulty d = Difficulty.byName(difficulty);
        CheckService.handleRoll(player, "skill", "", skill, d.ordinal(), bonus, 0);
        return 1;
    }

    private static int oppose(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = self(ctx);
        if (player == null) return fail(ctx, "只有玩家能进行对抗检定。");
        CheckService.handleRoll(player, "opposed", "",
                StringArgumentType.getString(ctx, "skill"), 0, 0,
                IntegerArgumentType.getInteger(ctx, "target"));
        return 1;
    }

    private static int push(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = self(ctx);
        if (player == null) return fail(ctx, "只有玩家能孤注一掷。");
        CheckService.handleAction(player, "push", 0);
        return 1;
    }

    private static int luck(CommandContext<CommandSourceStack> ctx, int points) {
        ServerPlayer player = self(ctx);
        if (player == null) return fail(ctx, "只有玩家能花费幸运。");
        CheckService.handleAction(player, "luck", points);
        return 1;
    }

    private static int journal(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = self(ctx);
        if (player == null) return fail(ctx, "只有玩家能打开线索日志。");
        CharacterService.requestSync(player);
        CocNetwork.openScreen(player, "journal", player.getUUID(), false);
        return 1;
    }

    private static int settings(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = self(ctx);
        if (player == null) return fail(ctx, "只有玩家能打开设置。");
        CocNetwork.openScreen(player, "settings", player.getUUID(), true);
        return 1;
    }

    private static int sancheck(CommandContext<CommandSourceStack> ctx, String name) {
        ServerPlayer target = target(ctx, name);
        if (target == null) return fail(ctx, "找不到玩家。");
        CheckService.sanCheck(target,
                StringArgumentType.getString(ctx, "successLoss"),
                StringArgumentType.getString(ctx, "failLoss"));
        return ok(ctx, "已对 " + target.getGameProfile().getName() + " 进行 SAN 检定。");
    }

    private static int statSet(CommandContext<CommandSourceStack> ctx, String action, int value, String name) {
        ServerPlayer target = target(ctx, name);
        if (target == null) return fail(ctx, "找不到玩家。");
        CharacterSheet sheet = CocData.sheet(target);
        if (sheet == null) return fail(ctx, "目标还没有角色卡。");
        switch (action) {
            case "san_set" -> SanityService.setSan(target, sheet, value);
            case "san_add" -> {
                if (value >= 0) SanityService.gainSan(target, sheet, value);
                else SanityService.loseSan(target, sheet, -value, "KP 调整", target.serverLevel().random);
            }
            case "san_sub" -> SanityService.loseSan(target, sheet, value, "KP 调整", target.serverLevel().random);
            case "hp_set", "hp_add", "hp_sub" -> {
                int delta = action.equals("hp_set") ? value - sheet.hp
                        : action.equals("hp_add") ? value : -value;
                sheet.hp = Math.max(0, Math.min(sheet.maxHp, sheet.hp + delta));
                sheet.dying = sheet.hp <= 0;
                CocNetwork.syncSheet(target, sheet);
            }
            case "mp_set", "mp_add" -> {
                int delta = action.equals("mp_set") ? value - sheet.mp : value;
                sheet.mp = Math.max(0, Math.min(sheet.maxMp, sheet.mp + delta));
                CocNetwork.syncSheet(target, sheet);
            }
            case "luck_add" -> {
                sheet.stats.luck = Math.max(0, Math.min(99, sheet.stats.luck + value));
                CocNetwork.syncSheet(target, sheet);
            }
            default -> {
            }
        }
        return ok(ctx, "已更新 " + target.getGameProfile().getName() + "。");
    }

    private static int mythos(CommandContext<CommandSourceStack> ctx, int value, String name) {
        ServerPlayer target = target(ctx, name);
        if (target == null) return fail(ctx, "找不到玩家。");
        CharacterSheet sheet = CocData.sheet(target);
        if (sheet == null) return fail(ctx, "目标还没有角色卡。");
        sheet.mythos = Math.max(0, Math.min(99, sheet.mythos + value));
        sheet.recalculateDerived();
        CocNetwork.syncSheet(target, sheet);
        return ok(ctx, target.getGameProfile().getName() + " 的克苏鲁神话 → " + sheet.mythos
                + "（最大 SAN " + sheet.maxSan + "）");
    }

    private static int setSkill(CommandContext<CommandSourceStack> ctx, int value, String name) {
        ServerPlayer target = target(ctx, name);
        if (target == null) return fail(ctx, "找不到玩家。");
        CharacterSheet sheet = CocData.sheet(target);
        if (sheet == null) return fail(ctx, "目标还没有角色卡。");
        SkillDef def = SkillRegistry.resolve(StringArgumentType.getString(ctx, "skill"));
        if (def == null) return fail(ctx, "未知技能。");
        sheet.setSkill(def.id, value);
        CocNetwork.syncSheet(target, sheet);
        return ok(ctx, target.getGameProfile().getName() + " 的 " + def.name + " → " + value);
    }

    private static int growth(CommandContext<CommandSourceStack> ctx, String name) {
        ServerPlayer target = target(ctx, name);
        if (target == null) return fail(ctx, "找不到玩家。");
        CharacterSheet sheet = CocData.sheet(target);
        if (sheet == null) return fail(ctx, "目标还没有角色卡。");
        GrowthService.grow(target, sheet);
        return ok(ctx, "已进行成长检定。");
    }

    private static int givePanel(CommandContext<CommandSourceStack> ctx, String name) {
        ServerPlayer target = target(ctx, name);
        if (target == null) return fail(ctx, "找不到玩家。");
        ItemStack stack = new ItemStack(CocItems.KP_PANEL.get());
        if (!target.getInventory().add(stack)) target.drop(stack, false);
        return ok(ctx, "已给予 " + target.getGameProfile().getName() + " 一个 KP面板。");
    }

    private static int deleteCharacter(CommandContext<CommandSourceStack> ctx, String name, boolean confirm) {
        CharacterSheet sheet = CharacterService.findByName(ctx.getSource().getServer(), name);
        if (sheet == null) return fail(ctx, "找不到角色卡：" + name);
        if (!confirm) {
            return ok(ctx, "将永久删除 " + sheet.name + " 的角色卡（" + sheet.occupationName
                    + "）。确认请输入：/coc delete " + name + " confirm");
        }
        CharacterService.delete(ctx.getSource().getServer(), sheet.uuid);
        return ok(ctx, "已删除角色卡：" + sheet.name);
    }

    private static int scenarioList(CommandContext<CommandSourceStack> ctx) {
        StringBuilder sb = new StringBuilder("剧本列表（" + ScenarioManager.status(ctx.getSource().getServer()) + "）：\n");
        for (ScenarioDef def : EventRegistry.scenarios()) {
            sb.append(def.id).append(" · ").append(def.title)
                    .append("（").append(def.events == null ? 0 : def.events.size()).append(" 幕）\n");
        }
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int scenarioStart(CommandContext<CommandSourceStack> ctx) {
        ScenarioManager.start(ctx.getSource().getServer(), StringArgumentType.getString(ctx, "id"));
        return 1;
    }

    private static int scenarioNext(CommandContext<CommandSourceStack> ctx) {
        ScenarioManager.next(ctx.getSource().getServer());
        return 1;
    }

    private static int scenarioStop(CommandContext<CommandSourceStack> ctx) {
        ScenarioManager.stop(ctx.getSource().getServer());
        return 1;
    }

    private static int scenarioAct(CommandContext<CommandSourceStack> ctx) {
        ScenarioManager.triggerAct(ctx.getSource().getServer(),
                StringArgumentType.getString(ctx, "id"),
                IntegerArgumentType.getInteger(ctx, "act"));
        return 1;
    }

    private static int handoutAll(CommandContext<CommandSourceStack> ctx) {
        String text = StringArgumentType.getString(ctx, "text");
        int count = 0;
        for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            if (CocData.sheet(p) != null) {
                HandoutService.send(p, "KP 手书", text);
                count++;
            }
        }
        return ok(ctx, "已向 " + count + " 名调查员发放线索。");
    }

    private static int handoutTo(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer target = target(ctx, StringArgumentType.getString(ctx, "player"));
        if (target == null) return fail(ctx, "找不到玩家。");
        HandoutService.send(target, "KP 手书", StringArgumentType.getString(ctx, "text"));
        return ok(ctx, "已发放线索给 " + target.getGameProfile().getName() + "。");
    }

    private static int broadcast(CommandContext<CommandSourceStack> ctx) {
        String text = StringArgumentType.getString(ctx, "text");
        for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.literal("【KP】" + text).withStyle(ChatFormatting.DARK_PURPLE));
        }
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        EventRegistry.reload();
        return ok(ctx, "已重载事件/剧本：" + EventRegistry.events().size() + " 个事件，"
                + EventRegistry.scenarios().size() + " 个剧本。");
    }

    private static int selftest(CommandContext<CommandSourceStack> ctx) {
        return SelfTest.run(ctx.getSource()) ? 1 : 0;
    }

    private static int kpList(CommandContext<CommandSourceStack> ctx) {
        StringBuilder sb = new StringBuilder("KP 名单（单人主机/OP 自动具备权限）：\n");
        if (CocServerConfig.get().kpNames.isEmpty()) sb.append("（配置名单为空）");
        else sb.append(String.join(", ", CocServerConfig.get().kpNames));
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }
}
