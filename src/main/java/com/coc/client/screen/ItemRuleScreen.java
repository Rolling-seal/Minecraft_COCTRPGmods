package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.KpView;
import com.coc.client.ui.CocUi;
import com.coc.net.CocNetwork;
import com.coc.net.packet.C2SKpAction;
import com.coc.rules.SkillDef;
import com.coc.rules.SkillRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** KP 为单个物品配置：使用技能 + 目标点数。 */
public class ItemRuleScreen extends Screen {
    private final Screen parent;
    private final String itemId;
    private final String itemName;
    private final KpView.ItemRule rule;
    private EditBox skillBox;
    private EditBox targetBox;
    private String status = "";
    private boolean compact;

    public ItemRuleScreen(Screen parent, String itemId, String itemName, KpView.ItemRule rule) {
        super(Component.literal(CocLang.tr("道具规则")));
        this.parent = parent;
        this.itemId = itemId == null ? "" : itemId;
        this.itemName = itemName == null ? itemId : itemName;
        this.rule = rule;
    }

    @Override
    protected void init() {
        compact = height < 420 || width < 700;
        int panelW = Math.min(width - 24, 340);
        int panelH = Math.min(height - 20, 200);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        int boxW = panelW - 20;
        skillBox = new EditBox(font, x + 10, y + 48, boxW, 18, Component.literal(CocLang.tr("技能 ID")));
        skillBox.setMaxLength(64);
        skillBox.setValue(rule == null ? "" : rule.skill);
        skillBox.setHint(Component.literal(CocLang.tr("可空，例如 spot_hidden / listen")));
        addRenderableWidget(skillBox);
        targetBox = new EditBox(font, x + 10, y + 88, 70, 18, Component.literal(CocLang.tr("目标点数")));
        targetBox.setMaxLength(3);
        targetBox.setValue(rule == null ? "50" : String.valueOf(rule.target));
        addRenderableWidget(targetBox);
        addBtn(CocLang.tr("保存规则"), x + 10, y + panelH - 24, 100, 18, this::save);
        addBtn(CocLang.tr("返回"), x + panelW - 110, y + panelH - 24, 100, 18, this::onClose);
    }

    private void save() {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            status = CocLang.tr("物品 ID 无效。");
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            status = CocLang.tr("未知道具：") + itemId;
            return;
        }
        String skill = skillBox.getValue().trim();
        if (!skill.isEmpty()) {
            SkillDef def = SkillRegistry.resolve(skill);
            if (def == null) {
                status = CocLang.tr("未知技能 ID：") + skill;
                return;
            }
            skill = def.id;
        }
        int target;
        try {
            target = Integer.parseInt(targetBox.getValue().trim());
        } catch (NumberFormatException e) {
            status = CocLang.tr("目标点数必须是数字。");
            return;
        }
        target = Math.max(1, Math.min(100, target));
        CocNetwork.sendToServer(new C2SKpAction("item_rule", null,
                itemId + "|" + skill, target));
        status = CocLang.tr("已提交：目标 ") + target + CocLang.tr("；返回道具列表可看到规则。");
    }

    private void addBtn(String label, int x, int y, int w, int h, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, w, h).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0000000);
        int panelW = Math.min(width - 24, 340);
        int panelH = Math.min(height - 20, 200);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        CocUi.panel(graphics, x, y, panelW, panelH);
        CocUi.title(graphics, font, CocLang.tr("道具规则"), width / 2, y + 6);
        graphics.drawString(font, CocUi.truncate(font, CocLang.tr("物品：") + itemName, panelW - 20),
                x + 10, y + 24, CocUi.GOLD, false);
        graphics.drawString(font, CocLang.tr("技能 ID（可空）"), x + 10, y + 38, CocUi.ACCENT, false);
        graphics.drawString(font, CocLang.tr("目标点数（1~100）"), x + 10, y + 78, CocUi.ACCENT, false);
        if (!status.isEmpty()) {
            CocUi.wrapped(graphics, font, status, x + 10, y + 112, panelW - 20, CocUi.MUTED);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
