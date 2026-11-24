package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.ClientCache;
import com.coc.client.KpView;
import com.coc.client.ui.CocList;
import com.coc.client.ui.CocUi;
import com.coc.net.CocNetwork;
import com.coc.net.packet.C2SKpAction;
import com.coc.rules.SkillRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** KP 发放道具：常见道具 + 自定义道具库；可配置物品对应技能/目标点数。 */
public class ItemPickerScreen extends Screen {
    private static final String[][] COMMON = {
            {"minecraft:torch", CocLang.tr("火把")}, {"minecraft:lantern", CocLang.tr("提灯")}, {"minecraft:candle", CocLang.tr("蜡烛")},
            {"minecraft:bread", CocLang.tr("面包")}, {"minecraft:paper", CocLang.tr("纸")}, {"minecraft:book", CocLang.tr("书")},
            {"minecraft:writable_book", CocLang.tr("书与笔")}, {"minecraft:map", CocLang.tr("地图")},
            {"minecraft:compass", CocLang.tr("指南针")}, {"minecraft:clock", CocLang.tr("怀表")},
            {"minecraft:iron_sword", CocLang.tr("铁剑")}, {"minecraft:bow", CocLang.tr("弓")}, {"minecraft:arrow", CocLang.tr("箭")},
            {"minecraft:shears", CocLang.tr("剪刀")}, {"minecraft:flint_and_steel", CocLang.tr("打火石")},
            {"minecraft:lead", CocLang.tr("拴绳")}, {"minecraft:string", CocLang.tr("绳子")}, {"minecraft:glass_bottle", CocLang.tr("玻璃瓶")},
            {"minecraft:gold_nugget", CocLang.tr("金粒")}, {"minecraft:bone", CocLang.tr("骨头")},
            {"minecraft:amethyst_shard", CocLang.tr("紫水晶碎片")}, {"minecraft:ender_pearl", CocLang.tr("末影珍珠")},
            {"minecraft:ink_sac", CocLang.tr("墨囊")}, {"minecraft:coal", CocLang.tr("煤炭")},
            {"coc:kp_book", CocLang.tr("KP之书")}, {"coc:global_settings", CocLang.tr("全局设置")}
    };
    private final Screen parent;
    private final UUID target;
    private final String targetName;
    private final List<Common> common = new ArrayList<>();
    private CocList<Common> list;
    private EditBox countBox;
    private EditBox customBox;
    private String selectedId = "";
    private String status = "";
    private boolean compact;

    public ItemPickerScreen(Screen parent, UUID target, String targetName) {
        super(Component.literal(CocLang.tr("发放道具")));
        this.parent = parent;
        this.target = target;
        this.targetName = targetName == null ? "" : targetName;
    }

    @Override
    protected void init() {
        compact = height < 420 || width < 700;
        buildItems();
        int top = compact ? 38 : 70;
        int bottom = height - 24;
        int listW = Math.max(150, width / 2 - 16);
        int listH = Math.max(80, bottom - top - 4);
        list = new CocList<>(12, top, listW, listH, compact ? 20 : 22,
                (g, font, item, x, y, w, h, hovered, selected) -> {
                    KpView view = ClientCache.kpView;
                    KpView.ItemRule rule = view == null ? null : view.itemRule(item.id);
                    String custom = rule == null ? "" : rule.name;
                    String name = (custom != null && !custom.isBlank())
                            ? "★ " + custom : CocLang.tr(item.name);
                    g.drawString(font, CocUi.truncate(font, name, w - 58), x, y + 4,
                            selected ? CocUi.GOLD : CocUi.TEXT, false);
                    if (rule != null) {
                        String right = (rule.skill == null || rule.skill.isBlank()
                                ? CocLang.tr("无技能") : CocLang.tr(SkillRegistry.nameOf(rule.skill))) + " " + rule.target;
                        g.drawString(font, CocUi.truncate(font, right, 54),
                                x + w - 56, y + 4, CocUi.ACCENT, false);
                    }
                });
        list.setItems(common);
        if (!selectedId.isBlank()) {
            for (int i = 0; i < common.size(); i++) {
                if (common.get(i).id.equals(selectedId)) {
                    list.setSelectedIndex(i);
                    break;
                }
            }
        }
        int rightX = width / 2 + 8;
        int rightW = Math.max(140, width - rightX - 12);
        countBox = new EditBox(font, rightX + 40, top + 2, 48, 18, Component.literal(CocLang.tr("数量")));
        countBox.setMaxLength(2);
        countBox.setValue("1");
        addRenderableWidget(countBox);
        addBtn(CocLang.tr("发放选中"), rightX, top + 26, rightW, compact ? 18 : 20, () -> {
            if (selected() == null) {
                status = CocLang.tr("请先选择道具。");
                return;
            }
            give(selected().id);
        });
        addBtn(CocLang.tr("配置选中规则"), rightX, top + 48, rightW, compact ? 18 : 20, () -> {
            Common item = selected();
            if (item == null) {
                status = CocLang.tr("请先选择道具。");
                return;
            }
            openRule(item.id, CocLang.tr(item.name));
        });
        customBox = new EditBox(font, rightX, top + 78, rightW, 18,
                Component.literal(CocLang.tr("自定义道具 ID")));
        customBox.setMaxLength(120);
        customBox.setHint(Component.literal("minecraft:item_id"));
        addRenderableWidget(customBox);
        addBtn(CocLang.tr("配置自定义（加入列表）"), rightX, top + 102, rightW, compact ? 18 : 20,
                () -> configureCustom());
        addBtn(CocLang.tr("发放自定义"), rightX, top + 124, rightW, compact ? 18 : 20,
                () -> give(customBox.getValue()));
        addBtn(CocLang.tr("返回"), rightX, bottom - 2, rightW, 18, this::onClose);
    }

    private void buildItems() {
        common.clear();
        Map<String, Common> map = new LinkedHashMap<>();
        for (String[] entry : COMMON) {
            ResourceLocation rl = ResourceLocation.tryParse(entry[0]);
            if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) continue;
            Common value = new Common(entry[0], entry[1]);
            map.put(value.id, value);
        }
        KpView view = ClientCache.kpView;
        if (view != null) {
            for (KpView.ItemRule rule : view.itemRules) {
                if (rule == null || rule.id == null || rule.id.isBlank()) continue;
                ResourceLocation rl = ResourceLocation.tryParse(rule.id);
                if (rl == null) continue;
                Item item = BuiltInRegistries.ITEM.get(rl);
                if (item == Items.AIR) continue;
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                if (map.containsKey(id)) continue;
                map.put(id, new Common(id, new ItemStack(item).getHoverName().getString()));
            }
        }
        common.addAll(map.values());
    }

    private Common selected() {
        return list == null ? null : list.getSelected();
    }

    private void openRule(String id, String name) {
        KpView view = ClientCache.kpView;
        KpView.ItemRule rule = view == null ? null : view.itemRule(id);
        if (minecraft != null) minecraft.setScreen(new ItemRuleScreen(this, id, name, rule));
    }

    private void configureCustom() {
        String raw = customBox.getValue().trim();
        ResourceLocation rl = ResourceLocation.tryParse(raw);
        if (rl == null) {
            status = CocLang.tr("道具 ID 格式错误，例如 minecraft:compass。");
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(rl);
        if (item == Items.AIR) {
            status = CocLang.tr("未知道具：") + raw;
            return;
        }
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        openRule(id, new ItemStack(item).getHoverName().getString());
    }

    public void refresh() {
        String id = selectedId;
        if (minecraft != null) rebuildWidgets();
        selectedId = id;
        if (list != null && !id.isBlank()) {
            for (int i = 0; i < common.size(); i++) {
                if (common.get(i).id.equals(id)) {
                    list.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void give(String id) {
        if (target == null) {
            status = CocLang.tr("未选择玩家。");
            return;
        }
        int count;
        try {
            count = Integer.parseInt(countBox.getValue().trim());
        } catch (NumberFormatException e) {
            status = CocLang.tr("数量必须是数字。");
            return;
        }
        if (count < 1 || count > 64) {
            status = CocLang.tr("数量需在 1~64。");
            return;
        }
        CocNetwork.sendToServer(new C2SKpAction("item_give", target, id == null ? "" : id.trim(), count));
        status = CocLang.tr("已请求发放：") + id + " ×" + count;
    }

    private void addBtn(String label, int x, int y, int w, int h, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, w, h).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0000000);
        CocUi.title(graphics, font, CocLang.tr("发放道具"), width / 2, compact ? 2 : 4);
        int top = compact ? 38 : 70;
        graphics.drawString(font, CocLang.tr("目标：") + targetName, 12, top - 11, CocUi.TEXT, false);
        if (list != null) list.render(graphics, font, mouseX, mouseY);
        int rightX = width / 2 + 8;
        int rightW = Math.max(140, width - rightX - 12);
        CocUi.panelLight(graphics, rightX, top, rightW, Math.max(90, height - top - 30));
        graphics.drawString(font, CocLang.tr("数量"), rightX + 4, top + 6, CocUi.MUTED, false);
        graphics.drawString(font, CocLang.tr("道具列表（★已配置规则）"), rightX + 94, top + 6, CocUi.ACCENT, false);
        if (!status.isEmpty()) {
            graphics.drawString(font, CocUi.truncate(font, status, rightW - 4),
                    rightX + 4, top + 150, CocUi.GOLD, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (list != null && list.mouseClicked(mouseX, mouseY, button)) {
            if (list.getSelected() != null) selectedId = list.getSelected().id;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (list != null && list.mouseScrolled(mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Common(String id, String name) {
    }
}
