package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.ClientCache;
import com.coc.client.ItemView;
import com.coc.client.KpView;
import com.coc.client.RollView;
import com.coc.client.ui.CocList;
import com.coc.client.ui.CocUi;
import com.coc.net.CocNetwork;
import com.coc.net.packet.C2SKpAction;
import com.coc.net.packet.C2SKpRoll;
import com.coc.rules.SkillDef;
import com.coc.rules.SkillRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;

/** 新 KP 面板：玩家 / 掷骰 / 道具 / 剧情之书 / 工具。 */
public class KeeperScreen extends Screen {
    private int tab;
    private UUID selectedUuid;
    private String selectedSkillId = "spot_hidden";
    private String selectedScenarioId;
    private int difficulty;
    private int bonus;
    private int selectedItemSlot = -1;
    private CocList<KpView.Player> playerList;
    private CocList<SkillDef> skillList;
    private CocList<ItemView.Entry> itemList;
    private CocList<KpView.Scenario> scenarioList;
    private EditBox diceExpr;
    private EditBox itemTarget;
    private EditBox handoutBox;
    private EditBox broadcastBox;
    private String status = "";
    private boolean requested;
    private UUID itemRequestedFor;
    private boolean compact;

    public KeeperScreen() {
        super(Component.literal(CocLang.tr("守秘人面板")));
    }

    @Override
    protected void init() {
        compact = height < 420 || width < 700;
        KpView view = ClientCache.kpView;
        if (!requested) {
            send("data", null, "", 0);
            requested = true;
        }
        String[] names = {CocLang.tr("玩家"), CocLang.tr("掷骰"), CocLang.tr("道具"), CocLang.tr("剧情书"), CocLang.tr("工具")};
        int tabW = compact ? 50 : 64;
        int tabX = compact ? 4 : 12;
        int tabGap = compact ? 3 : 4;
        int tabY = compact ? 16 : 24;
        int tabH = compact ? 18 : 20;
        for (int i = 0; i < names.length; i++) {
            int index = i;
            String label = (tab == i ? "▶" : "") + names[i];
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                tab = index;
                rebuildWidgets();
            }).bounds(tabX + i * (tabW + tabGap), tabY, tabW, tabH).build());
        }
        int bottomY = height - 22;
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("刷新")), b -> {
            send("data", null, "", 0);
            if (tab == 2) requestItems(true);
        }).bounds(width - 132, bottomY, 56, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("关闭")), b -> onClose())
                .bounds(width - 72, bottomY, 60, 18).build());

        if (view == null || !view.allowed) return;
        int contentY = compact ? 40 : 76;
        int contentH = Math.max(80, height - contentY - (compact ? 26 : 52));
        int listX = 12;
        int listW = Math.max(150, width / 2 - 18);
        int rightX = width / 2 + 6;
        int rightW = Math.max(150, width - rightX - 12);
        int step = compact ? 20 : 24;
        int bh = compact ? 16 : 20;
        if (selectedUuid == null && !view.players.isEmpty()) {
            selectedUuid = view.players.get(0).uuid;
        }
        if ((selectedScenarioId == null || selectedScenarioId.isBlank()) && !view.scenarios.isEmpty()) {
            selectedScenarioId = view.scenarioId != null && !view.scenarioId.isBlank()
                    ? view.scenarioId : view.scenarios.get(0).id;
        }
        switch (tab) {
            case 0 -> initPlayers(view, contentY, contentH, listX, listW, rightX, rightW, step, bh);
            case 1 -> initDice(view, contentY, contentH, listX, listW, rightX, rightW, step, bh);
            case 2 -> initItems(view, contentY, contentH, listX, listW, rightX, rightW, step, bh);
            case 3 -> initBook(view, contentY, contentH, listX, listW, rightX, rightW, step, bh);
            default -> initTools(view, contentY, contentH, step, bh);
        }
    }

    private void initPlayers(KpView view, int cy, int ch, int lx, int lw, int rx, int rw,
                             int step, int bh) {
        playerList = new CocList<>(lx, cy, lw, ch, compact ? 18 : 20,
                (g, font, p, x, y, w, h, hovered, selected) -> {
                    g.drawString(font, CocUi.truncate(font, p.name, w - 4), x, y + 2,
                            p.insane ? CocUi.RED : (selected ? CocUi.GOLD : CocUi.TEXT), false);
                    String info = p.hasSheet
                            ? (compact ? "SAN " + p.san + "/" + p.maxSan
                            : "SAN " + p.san + "/" + p.maxSan + "  HP " + p.hp + "/" + p.maxHp
                            + "  MP " + p.mp + "/" + p.maxMp)
                            : CocLang.tr("无角色卡");
                    g.drawString(font, info, x + (compact ? w - font.width(info) - 2 : 90),
                            y + (compact ? 10 : 2), CocUi.MUTED, false);
                });
        playerList.setItems(view.players);
        restorePlayerSelection(view);
        int y = cy + (compact ? 50 : 64);
        addBtn(CocLang.tr("查看角色卡"), rx, y, rw, bh, () -> send("sheet", selectedUuid, "", 0));
        addBtn(CocLang.tr("状态调整 / 燃运"), rx, y + step, rw, bh,
                () -> openAdjust(view));
        addBtn(CocLang.tr("恢复神志"), rx, y + step * 2, rw, bh, () -> send("clear_insanity", selectedUuid, "", 0));
        addBtn(CocLang.tr("成长检定"), rx, y + step * 3, rw, bh, () -> send("growth", selectedUuid, "", 0));
        addBtn(CocLang.tr("删除角色卡"), rx, y + step * 4, rw, bh, () -> {
            KpView.Player p = selectedPlayer(view);
            if (p == null) {
                status = CocLang.tr("请先选择玩家。");
                return;
            }
            if (minecraft != null) minecraft.setScreen(new ConfirmDeleteScreen(this, p.uuid, p.name));
        });
    }

    private void initDice(KpView view, int cy, int ch, int lx, int lw, int rx, int rw,
                          int step, int bh) {
        skillList = new CocList<>(lx, cy, lw, ch, compact ? 14 : 16,
                (g, font, skill, x, y, w, h, hovered, selected) -> {
                    KpView.Player p = selectedPlayer(view);
                    int value = p == null ? 0 : p.skills.getOrDefault(skill.id, 0);
                    g.drawString(font, CocUi.truncate(font, CocLang.tr(skill.name), w - 34), x, y + 1,
                            selected ? CocUi.GOLD : CocUi.TEXT, false);
                    String v = String.valueOf(value);
                    g.drawString(font, v, x + w - font.width(v), y + 1,
                            value >= 50 ? CocUi.TEXT : CocUi.MUTED, false);
                });
        skillList.setItems(List.copyOf(SkillRegistry.all()));
        for (int i = 0; i < skillList.items().size(); i++) {
            if (skillList.items().get(i).id.equals(selectedSkillId)) {
                skillList.setSelectedIndex(i);
                break;
            }
        }
        int y = cy + 42;
        addBtn(CocLang.tr("难度：") + difficultyLabel(), rx, y, rw, bh, () -> {
            difficulty = (difficulty + 1) % 3;
            rebuildWidgets();
        });
        addBtn(CocLang.tr("奖励/惩罚：") + bonusLabel(), rx, y + step, rw, bh, () -> {
            bonus = bonus >= 2 ? -2 : bonus + 1;
            rebuildWidgets();
        });
        addBtn(CocLang.tr("为选中玩家掷骰"), rx, y + step * 2, rw, bh, () -> rollSkill());
        diceExpr = new EditBox(font, rx, y + step * 3 + 4, rw - 76, 18, Component.literal(CocLang.tr("骰子")));
        diceExpr.setMaxLength(32);
        diceExpr.setValue("1d100");
        addRenderableWidget(diceExpr);
        addBtn(CocLang.tr("公开掷骰"), rx + rw - 72, y + step * 3 + 4, 72, 18, () -> rollDice());
    }

    private void initItems(KpView view, int cy, int ch, int lx, int lw, int rx, int rw,
                           int step, int bh) {
        KpView.Player p = selectedPlayer(view);
        requestItems(false);
        ItemView data = ClientCache.items;
        itemList = new CocList<>(lx, cy, lw, ch, compact ? 18 : 20,
                (g, font, item, x, y, w, h, hovered, selected) -> {
                    g.drawString(font, CocUi.truncate(font, itemName(item) + " ×" + item.count, w - 4),
                            x, y + 2, selected ? CocUi.GOLD : CocUi.TEXT, false);
                    g.drawString(font, CocUi.truncate(font, item.id, w - 4), x, y + 11,
                            CocUi.MUTED, false);
                });
        if (data != null && selectedUuid != null && selectedUuid.equals(data.target)) {
            itemList.setItems(data.items);
            for (int i = 0; i < data.items.size(); i++) {
                if (data.items.get(i).slot == selectedItemSlot) {
                    itemList.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            itemList.setItems(List.of());
        }
        int y = cy + 58;
        addBtn(CocLang.tr("发放道具 / 规则"), rx, y, rw, bh, () -> {
            KpView.Player target = selectedPlayer(view);
            if (target == null) {
                status = CocLang.tr("请先选择玩家。");
                return;
            }
            if (minecraft != null) {
                minecraft.setScreen(new ItemPickerScreen(this, target.uuid, target.name));
            }
        });
        addBtn(CocLang.tr("使用选中道具（公开检定）"), rx, y + step, rw, bh, () -> useItem());
        itemTarget = new EditBox(font, rx + 42, y + step * 2 + 4, 50, 18, Component.literal(CocLang.tr("目标值")));
        itemTarget.setMaxLength(3);
        itemTarget.setValue("50");
        addRenderableWidget(itemTarget);
        addBtn(CocLang.tr("刷新背包"), rx + 96, y + step * 2 + 4, Math.max(50, rw - 96), 18,
                () -> requestItems(true));
        addBtn(CocLang.tr("配置选中道具规则"), rx, y + step * 3 + 4, rw, 16, () -> configureSelectedItem(view));
        if (p == null) {
            status = CocLang.tr("请先选择玩家。");
        }
    }

    private void initBook(KpView view, int cy, int ch, int lx, int lw, int rx, int rw,
                          int step, int bh) {
        scenarioList = new CocList<>(lx, cy, lw, ch, compact ? 18 : 20,
                (g, font, s, x, y, w, h, hovered, selected) -> {
                    g.drawString(font, CocUi.truncate(font, CocLang.tr(s.title), w - 44), x, y + 2,
                            selected ? CocUi.GOLD : CocUi.TEXT, false);
                    String right = s.count + CocLang.tr(" 幕");
                    g.drawString(font, right, x + w - font.width(right) - 4, y + 2, CocUi.MUTED, false);
                });
        scenarioList.setItems(view.scenarios);
        for (int i = 0; i < view.scenarios.size(); i++) {
            if (view.scenarios.get(i).id.equals(selectedScenarioId)) {
                scenarioList.setSelectedIndex(i);
                break;
            }
        }
        int y = cy + 72;
        addBtn(CocLang.tr("主线剧情"), rx, y, rw, bh, () -> openBook(0));
        addBtn(CocLang.tr("支线剧情"), rx, y + step, rw, bh, () -> openBook(1));
        addBtn(CocLang.tr("开始剧本"), rx, y + step * 2, rw, bh, () -> {
            KpView.Scenario s = selectedScenario(view);
            if (s != null) send("scenario_start", null, s.id, 0);
        });
        addBtn(CocLang.tr("结束当前剧本"), rx, y + step * 3, rw, bh, () -> send("scenario_stop", null, "", 0));
        addBtn(CocLang.tr("导入剧本"), rx, y + step * 4, rw, bh, () -> {
            if (minecraft != null) minecraft.setScreen(new ImportScenarioScreen(this));
        });
    }

    private void initTools(KpView view, int cy, int ch, int step, int bh) {
        handoutBox = new EditBox(font, 12, cy + 14, width - 24, 18, Component.literal(CocLang.tr("KP 手书内容")));
        handoutBox.setMaxLength(300);
        handoutBox.setHint(Component.literal(CocLang.tr("KP 手书 / 线索内容")));
        addRenderableWidget(handoutBox);
        addBtn(CocLang.tr("发给选中"), 12, cy + 36, 96, 18,
                () -> send("handout", selectedUuid, handoutBox.getValue(), 0));
        addBtn(CocLang.tr("发给所有人"), 112, cy + 36, 96, 18,
                () -> send("handout_all", null, handoutBox.getValue(), 0));
        broadcastBox = new EditBox(font, 12, cy + 70, Math.max(120, width - 96), 18,
                Component.literal(CocLang.tr("广播内容")));
        broadcastBox.setMaxLength(300);
        broadcastBox.setHint(Component.literal(CocLang.tr("广播内容（只聊天）")));
        addRenderableWidget(broadcastBox);
        addBtn(CocLang.tr("广播"), width - 76, cy + 70, 64, 18,
                () -> send("broadcast", null, broadcastBox.getValue(), 0));
        addBtn(CocLang.tr("重载事件/剧本"), 12, cy + 94, 138, 18, () -> send("reload", null, "", 0));
    }

    private void restorePlayerSelection(KpView view) {
        if (playerList == null || selectedUuid == null) return;
        for (int i = 0; i < view.players.size(); i++) {
            KpView.Player p = view.players.get(i);
            if (p.uuid != null && p.uuid.equals(selectedUuid)) {
                playerList.setSelectedIndex(i);
                return;
            }
        }
        if (!view.players.isEmpty()) {
            selectedUuid = view.players.get(0).uuid;
            playerList.setSelectedIndex(0);
        }
    }

    private KpView.Player selectedPlayer(KpView view) {
        if (playerList != null && playerList.getSelected() != null) return playerList.getSelected();
        if (view != null && selectedUuid != null) {
            for (KpView.Player p : view.players) {
                if (p.uuid != null && p.uuid.equals(selectedUuid)) return p;
            }
        }
        return null;
    }

    private KpView.Scenario selectedScenario(KpView view) {
        if (scenarioList != null && scenarioList.getSelected() != null) return scenarioList.getSelected();
        if (view != null && selectedScenarioId != null) {
            for (KpView.Scenario s : view.scenarios) {
                if (s.id.equals(selectedScenarioId)) return s;
            }
        }
        return view == null || view.scenarios.isEmpty() ? null : view.scenarios.get(0);
    }

    private SkillDef selectedSkill() {
        return skillList == null ? null : skillList.getSelected();
    }

    private ItemView.Entry selectedItem() {
        return itemList == null ? null : itemList.getSelected();
    }

    /** 用客户端语言显示物品名；服务端默认语言可能把火把发成 Torch。 */
    private String itemName(ItemView.Entry entry) {
        if (entry == null) return "";
        ResourceLocation id = ResourceLocation.tryParse(entry.id);
        if (id != null) {
            var item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) return new ItemStack(item).getHoverName().getString();
        }
        return entry.name == null ? entry.id : entry.name;
    }

    private void openAdjust(KpView view) {
        KpView.Player p = selectedPlayer(view);
        if (p == null) {
            status = CocLang.tr("请先选择玩家。");
            return;
        }
        if (minecraft != null) {
            minecraft.setScreen(new AdjustPlayerScreen(this, p.uuid, p.name, p));
        }
    }

    private void openBook(int mode) {
        KpView view = ClientCache.kpView;
        KpView.Scenario s = selectedScenario(view);
        if (s == null) {
            status = CocLang.tr("请先选择剧本。");
            return;
        }
        if (minecraft != null) {
            minecraft.setScreen(new StoryBookScreen(this, s.id, mode));
        }
    }

    private void requestItems(boolean force) {
        if (selectedUuid == null) return;
        ItemView current = ClientCache.items;
        if (!force && current != null && selectedUuid.equals(current.target)) return;
        if (!force && selectedUuid.equals(itemRequestedFor)) return;
        itemRequestedFor = selectedUuid;
        send("items", selectedUuid, "", 0);
    }

    private void rollSkill() {
        KpView view = ClientCache.kpView;
        KpView.Player p = selectedPlayer(view);
        SkillDef skill = selectedSkill();
        if (p == null) {
            status = CocLang.tr("请先选择玩家。");
            return;
        }
        if (skill == null) {
            status = CocLang.tr("请先选择技能。");
            return;
        }
        if (!p.hasSheet) {
            status = CocLang.tr("这个玩家还没有角色卡。");
            return;
        }
        CocNetwork.sendToServer(new C2SKpRoll(p.uuid, "skill", skill.id, difficulty, bonus, ""));
        status = CocLang.tr("已公开掷骰：") + p.name + " · " + skill.name;
    }

    private void rollDice() {
        String expr = diceExpr == null ? "1d100" : diceExpr.getValue().trim();
        CocNetwork.sendToServer(new C2SKpRoll(null, "dice", "", 0, 0, expr));
        status = CocLang.tr("已公开掷骰：") + expr;
    }

    private void configureSelectedItem(KpView view) {
        ItemView.Entry item = selectedItem();
        if (item == null) {
            status = CocLang.tr("请先选择背包道具。");
            return;
        }
        KpView.ItemRule rule = view == null ? null : view.itemRule(item.id);
        if (minecraft != null) {
            minecraft.setScreen(new ItemRuleScreen(this, item.id, itemName(item), rule));
        }
    }

    private void useItem() {
        ItemView.Entry item = selectedItem();
        if (item == null) {
            status = CocLang.tr("请先在背包列表里选择道具。");
            return;
        }
        if (selectedUuid == null) {
            status = CocLang.tr("请先选择玩家。");
            return;
        }
        int target = 50;
        try {
            target = Integer.parseInt(itemTarget == null ? "50" : itemTarget.getValue().trim());
        } catch (NumberFormatException ignored) {
        }
        target = Math.max(1, Math.min(100, target));
        send("item_use", selectedUuid, String.valueOf(item.slot), target);
        status = CocLang.tr("使用 ") + itemName(item) + CocLang.tr("，公开检定目标 ") + target;
    }

    private String difficultyLabel() {
        return switch (difficulty) {
            case 1 -> CocLang.tr("困难");
            case 2 -> CocLang.tr("极难");
            default -> CocLang.tr("常规");
        };
    }

    private String bonusLabel() {
        if (bonus == 0) return CocLang.tr("无");
        return (bonus > 0 ? CocLang.tr("奖励 +") : CocLang.tr("惩罚 -")) + Math.abs(bonus);
    }

    private void addBtn(String label, int x, int y, int w, int h, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, w, h).build());
    }

    private void send(String action, UUID target, String text, int value) {
        CocNetwork.sendToServer(new C2SKpAction(action, target, text == null ? "" : text, value));
    }

    public void refresh() {
        int ps = playerList == null ? 0 : playerList.getScroll();
        int ss = skillList == null ? 0 : skillList.getScroll();
        int is = itemList == null ? 0 : itemList.getScroll();
        int bs = scenarioList == null ? 0 : scenarioList.getScroll();
        if (minecraft != null) rebuildWidgets();
        if (playerList != null) playerList.setScroll(ps);
        if (skillList != null) skillList.setScroll(ss);
        if (itemList != null) itemList.setScroll(is);
        if (scenarioList != null) scenarioList.setScroll(bs);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0000000);
        CocUi.title(graphics, font, CocLang.tr("守秘人面板"), width / 2, compact ? 2 : 4);
        KpView view = ClientCache.kpView;
        if (view == null) {
            graphics.drawCenteredString(font, CocLang.tr("正在请求 KP 数据……"), width / 2, height / 2, CocUi.MUTED);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        if (!view.allowed) {
            graphics.drawCenteredString(font, CocLang.tr("你不具备 KP 权限。"), width / 2, height / 2 - 10, CocUi.RED);
            graphics.drawCenteredString(font, CocLang.tr("单人主机 / OP / config/coc-server.json 的 kpNames 可用。"),
                    width / 2, height / 2 + 6, CocUi.MUTED);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        int cy = compact ? 40 : 76;
        int ch = Math.max(80, height - cy - (compact ? 26 : 52));
        int lx = 12;
        int lw = Math.max(150, width / 2 - 18);
        int rx = width / 2 + 6;
        int rw = Math.max(150, width - rx - 12);
        switch (tab) {
            case 0 -> renderPlayers(graphics, view, cy, ch, lx, lw, rx, rw, mouseX, mouseY);
            case 1 -> renderDice(graphics, view, cy, ch, lx, lw, rx, rw, mouseX, mouseY);
            case 2 -> renderItems(graphics, view, cy, ch, lx, lw, rx, rw, mouseX, mouseY);
            case 3 -> renderBook(graphics, view, cy, ch, lx, lw, rx, rw, mouseX, mouseY);
            default -> renderTools(graphics, view, cy, ch);
        }
        if (!status.isEmpty()) {
            graphics.drawString(font, CocUi.truncate(font, status, width - 24), 12, height - 38,
                    CocUi.GOLD, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPlayers(GuiGraphics g, KpView view, int cy, int ch, int lx, int lw,
                               int rx, int rw, int mx, int my) {
        if (!compact) g.drawString(font, CocLang.tr("在线玩家"), lx, cy - 11, CocUi.ACCENT, false);
        if (playerList != null) playerList.render(g, font, mx, my);
        CocUi.panelLight(g, rx, cy, rw, ch);
        KpView.Player p = selectedPlayer(view);
        if (p == null) {
            g.drawString(font, CocLang.tr("← 选择一名玩家"), rx + 8, cy + 6, CocUi.MUTED, false);
            return;
        }
        g.drawString(font, p.name, rx + 8, cy + 4, CocUi.GOLD, false);
        String line = p.hasSheet ? "SAN " + p.san + "/" + p.maxSan + "  HP " + p.hp + "/" + p.maxHp
                : CocLang.tr("尚未创建角色卡");
        g.drawString(font, CocUi.truncate(font, line, rw - 16), rx + 8, cy + 18, CocUi.TEXT, false);
        if (!compact) {
            g.drawString(font, "MP " + p.mp + "/" + p.maxMp + CocLang.tr("  幸运 ") + p.luck + CocLang.tr("  神话 ") + p.mythos,
                    rx + 8, cy + 32, CocUi.MUTED, false);
            CocUi.wrapped(g, font, CocLang.tr("状态：") + CocLang.message(p.status), rx + 8, cy + 46, rw - 16,
                    p.insane ? CocUi.RED : CocUi.GREEN);
        }
    }

    private void renderDice(GuiGraphics g, KpView view, int cy, int ch, int lx, int lw,
                            int rx, int rw, int mx, int my) {
        if (!compact) g.drawString(font, CocLang.tr("技能列表"), lx, cy - 11, CocUi.ACCENT, false);
        if (skillList != null) skillList.render(g, font, mx, my);
        CocUi.panelLight(g, rx, cy, rw, ch);
        KpView.Player p = selectedPlayer(view);
        SkillDef skill = selectedSkill();
        g.drawString(font, CocLang.tr("目标：") + (p == null ? CocLang.tr("未选择") : p.name), rx + 8, cy + 4, CocUi.TEXT, false);
        if (p != null && skill != null) {
            int value = p.skills.getOrDefault(skill.id, 0);
            g.drawString(font, CocLang.tr("技能：") + CocLang.tr(skill.name) + "  " + value, rx + 8, cy + 18, CocUi.ACCENT, false);
            int target = switch (difficulty) {
                case 1 -> Math.max(1, value / 2);
                case 2 -> Math.max(1, value / 5);
                default -> value;
            };
            g.drawString(font, CocLang.tr("目标值：") + target + "（" + difficultyLabel() + "）",
                    rx + 8, cy + 31, CocUi.MUTED, false);
        }
        RollView roll = ClientCache.lastRoll;
        if (roll != null && ch > 150) {
            int ry = cy + ch - 46;
            g.drawString(font, CocUi.truncate(font, CocLang.tr("最近公开：") + roll.title, rw - 16),
                    rx + 8, ry, CocUi.GOLD, false);
            g.drawString(font, roll.roll + (roll.target >= 0 ? " / " + roll.target : "")
                            + " → " + roll.level, rx + 8, ry + 12, roll.levelColor, false);
            g.drawString(font, CocUi.truncate(font, roll.detail, rw - 16),
                    rx + 8, ry + 24, CocUi.MUTED, false);
        }
    }

    private void renderItems(GuiGraphics g, KpView view, int cy, int ch, int lx, int lw,
                             int rx, int rw, int mx, int my) {
        if (!compact) g.drawString(font, CocLang.tr("选中玩家背包"), lx, cy - 11, CocUi.ACCENT, false);
        if (itemList != null) itemList.render(g, font, mx, my);
        CocUi.panelLight(g, rx, cy, rw, ch);
        KpView.Player p = selectedPlayer(view);
        ItemView.Entry item = selectedItem();
        g.drawString(font, CocLang.tr("目标：") + (p == null ? CocLang.tr("未选择") : p.name), rx + 8, cy + 4, CocUi.TEXT, false);
        g.drawString(font, item == null ? CocLang.tr("未选择道具") : itemName(item) + " ×" + item.count,
                rx + 8, cy + 18, item == null ? CocUi.MUTED : CocUi.GOLD, false);
        int step = compact ? 20 : 24;
        int targetY = cy + 58 + step * 2 + 9;
        g.drawString(font, CocLang.tr("目标"), rx + 8, targetY, CocUi.MUTED, false);
        if (item == null) {
            g.drawString(font, CocLang.tr("← 先点一件背包道具"), rx + 8, cy + 34, CocUi.MUTED, false);
        } else {
            KpView.ItemRule rule = view.itemRule(item.id);
            String line;
            if (rule == null) {
                line = CocLang.tr("未配置规则：使用下方临时目标");
            } else {
                String sk = rule.skill == null || rule.skill.isBlank()
                        ? CocLang.tr("无技能") : CocLang.tr(SkillRegistry.nameOf(rule.skill));
                line = CocLang.tr("规则：") + sk + CocLang.tr(" / 目标 ") + rule.target;
            }
            g.drawString(font, CocUi.truncate(font, line, rw - 16), rx + 8, cy + 34, CocUi.ACCENT, false);
        }
    }

    private void renderBook(GuiGraphics g, KpView view, int cy, int ch, int lx, int lw,
                            int rx, int rw, int mx, int my) {
        if (!compact) g.drawString(font, CocLang.tr("剧本"), lx, cy - 11, CocUi.ACCENT, false);
        if (scenarioList != null) scenarioList.render(g, font, mx, my);
        CocUi.panelLight(g, rx, cy, rw, ch);
        g.drawString(font, CocUi.truncate(font, CocLang.tr("当前：") + CocLang.message(view.scenarioStatus), rw - 16),
                rx + 8, cy + 4, CocUi.TEXT, false);
        KpView.Scenario s = selectedScenario(view);
        if (s != null) {
            g.drawString(font, s.title + "（" + s.count + CocLang.tr(" 幕）"), rx + 8, cy + 20, CocUi.GOLD, false);
        }
        g.drawString(font, CocLang.tr("剧情之书：主线走向 / 支线触发与后果"), rx + 8, cy + 34, CocUi.MUTED, false);
        g.drawString(font, CocLang.tr("显示此幕只会给玩家发幕布，不发选项。"), rx + 8, cy + 46, CocUi.MUTED, false);
        if (!compact) {
            g.drawString(font, CocLang.tr("剧本开始后，玩家只收到每一幕的介绍。"), rx + 8, cy + 58, CocUi.ACCENT, false);
        }
    }

    private void renderTools(GuiGraphics g, KpView view, int cy, int ch) {
        CocUi.panelLight(g, 12, cy - 6, width - 24, ch + 4);
        g.drawString(font, CocLang.tr("发放线索（写入角色卡与日志）"), 18, cy + 2, CocUi.ACCENT, false);
        g.drawString(font, CocLang.tr("广播（只聊天，不写入角色卡）"), 18, cy + 58, CocUi.ACCENT, false);
        if (!view.errors.isEmpty()) {
            int ey = cy + 120;
            g.drawString(font, CocLang.tr("数据警告（前 3 条）："), 18, ey, CocUi.RED, false);
            for (int i = 0; i < Math.min(3, view.errors.size()); i++) {
                g.drawString(font, CocUi.truncate(font, view.errors.get(i), width - 40),
                        18, ey + 12 + i * 11, CocUi.MUTED, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == 0 && playerList != null && playerList.mouseClicked(mouseX, mouseY, button)) {
            if (playerList.getSelected() != null) {
                selectedUuid = playerList.getSelected().uuid;
                selectedItemSlot = -1;
            }
            return true;
        }
        if (tab == 1 && skillList != null && skillList.mouseClicked(mouseX, mouseY, button)) {
            if (skillList.getSelected() != null) selectedSkillId = skillList.getSelected().id;
            return true;
        }
        if (tab == 2 && itemList != null && itemList.mouseClicked(mouseX, mouseY, button)) {
            if (itemList.getSelected() != null) selectedItemSlot = itemList.getSelected().slot;
            return true;
        }
        if (tab == 3 && scenarioList != null && scenarioList.mouseClicked(mouseX, mouseY, button)) {
            if (scenarioList.getSelected() != null) selectedScenarioId = scenarioList.getSelected().id;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tab == 0 && playerList != null && playerList.mouseScrolled(mouseX, mouseY, delta)) return true;
        if (tab == 1 && skillList != null && skillList.mouseScrolled(mouseX, mouseY, delta)) return true;
        if (tab == 2 && itemList != null && itemList.mouseScrolled(mouseX, mouseY, delta)) return true;
        if (tab == 3 && scenarioList != null && scenarioList.mouseScrolled(mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
