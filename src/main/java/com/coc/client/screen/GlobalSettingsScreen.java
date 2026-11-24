package com.coc.client.screen;

import com.coc.client.ClientCache;
import com.coc.client.CocLang;
import com.coc.client.GlobalSettingsView;
import com.coc.client.ui.CocUi;
import com.coc.net.CocNetwork;
import com.coc.net.packet.C2SGlobalSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 全局设置：角色创建点数与初始属性范围。KP 通过物品/KP之书打开。 */
public class GlobalSettingsScreen extends Screen {
    private EditBox occBox;
    private EditBox intBox;
    private EditBox totalBox;
    private EditBox luckMinBox;
    private EditBox luckMaxBox;
    private EditBox hpMinBox;
    private EditBox hpMaxBox;
    private EditBox sanMinBox;
    private EditBox sanMaxBox;
    private Button allowButton;
    private boolean allow;
    private String status = "";
    private boolean compact;

    public GlobalSettingsScreen() {
        super(Component.literal(CocLang.tr("全局设置")));
    }

    @Override
    protected void init() {
        compact = height < 420 || width < 700;
        GlobalSettingsView view = ClientCache.globalSettings;
        allow = view == null || view.allowPoints;
        int panelW = Math.min(width - 16, 400);
        int panelH = Math.min(height - 12, 228);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        int half = panelW / 2;
        int leftX = x + 10;
        int rightX = x + half + 4;
        int groupW = half - 20;

        allowButton = addBtn((allow ? "✔ " : "✘ ") + CocLang.tr("允许玩家自行分配点数"),
                x + 10, y + 28, panelW - 20, 18, () -> {
                    allow = !allow;
                    allowButton.setMessage(Component.literal((allow ? "✔ " : "✘ ")
                            + CocLang.tr("允许玩家自行分配点数")));
                });

        int row1 = y + 60;
        int row2 = y + 92;
        int row3 = y + 124;
        occBox = box(leftX, row1, 62, view == null ? "-1" : String.valueOf(view.occupationPoints));
        intBox = box(leftX, row2, 62, view == null ? "-1" : String.valueOf(view.interestPoints));
        totalBox = box(leftX, row3, 62, view == null ? "-1" : String.valueOf(view.totalPoints));

        int boxW = Math.max(52, (groupW - 18) / 2);
        int gap = 8;
        luckMinBox = box(rightX, row1, boxW, view == null ? "-1" : String.valueOf(view.luckMin));
        luckMaxBox = box(rightX + boxW + gap, row1, boxW, view == null ? "-1" : String.valueOf(view.luckMax));
        hpMinBox = box(rightX, row2, boxW, view == null ? "-1" : String.valueOf(view.hpMin));
        hpMaxBox = box(rightX + boxW + gap, row2, boxW, view == null ? "-1" : String.valueOf(view.hpMax));
        sanMinBox = box(rightX, row3, boxW, view == null ? "-1" : String.valueOf(view.sanMin));
        sanMaxBox = box(rightX + boxW + gap, row3, boxW, view == null ? "-1" : String.valueOf(view.sanMax));
        for (EditBox box : new EditBox[]{luckMaxBox, hpMaxBox, sanMaxBox}) {
            box.setHint(Component.literal(CocLang.tr("最大")));
        }

        addBtn(CocLang.tr("保存"), x + 10, y + panelH - 24, 100, 18, this::save);
        addBtn(CocLang.tr("关闭"), x + panelW - 90, y + panelH - 24, 80, 18, this::onClose);
    }

    private EditBox box(int x, int y, int w, String value) {
        EditBox box = new EditBox(font, x, y, w, 18, Component.literal(CocLang.tr("数值")));
        box.setMaxLength(5);
        box.setValue(value);
        addRenderableWidget(box);
        return box;
    }

    private void save() {
        try {
            int occ = parseInt(occBox.getValue(), -1);
            int interest = parseInt(intBox.getValue(), -1);
            int total = parseInt(totalBox.getValue(), -1);
            int luckMin = parseInt(luckMinBox.getValue(), -1);
            int luckMax = parseInt(luckMaxBox.getValue(), -1);
            int hpMin = parseInt(hpMinBox.getValue(), -1);
            int hpMax = parseInt(hpMaxBox.getValue(), -1);
            int sanMin = parseInt(sanMinBox.getValue(), -1);
            int sanMax = parseInt(sanMaxBox.getValue(), -1);
            CocNetwork.sendToServer(new C2SGlobalSettings(allow, occ, interest, total,
                    luckMin, luckMax, hpMin, hpMax, sanMin, sanMax));
            status = CocLang.tr("已提交；等待服务器同步。");
        } catch (NumberFormatException e) {
            status = CocLang.tr("数值必须是整数；-1 表示使用默认。");
        }
    }

    private int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        return Integer.parseInt(value.trim());
    }

    public void onSettings(GlobalSettingsView view) {
        if (view == null) return;
        allow = view.allowPoints;
        if (allowButton != null) {
            allowButton.setMessage(Component.literal((allow ? "✔ " : "✘ ")
                    + CocLang.tr("允许玩家自行分配点数")));
        }
        if (occBox != null) occBox.setValue(String.valueOf(view.occupationPoints));
        if (intBox != null) intBox.setValue(String.valueOf(view.interestPoints));
        if (totalBox != null) totalBox.setValue(String.valueOf(view.totalPoints));
        if (luckMinBox != null) luckMinBox.setValue(String.valueOf(view.luckMin));
        if (luckMaxBox != null) luckMaxBox.setValue(String.valueOf(view.luckMax));
        if (hpMinBox != null) hpMinBox.setValue(String.valueOf(view.hpMin));
        if (hpMaxBox != null) hpMaxBox.setValue(String.valueOf(view.hpMax));
        if (sanMinBox != null) sanMinBox.setValue(String.valueOf(view.sanMin));
        if (sanMaxBox != null) sanMaxBox.setValue(String.valueOf(view.sanMax));
        status = CocLang.tr("设置已同步。");
    }

    private Button addBtn(String label, int x, int y, int w, int h, Runnable action) {
        Button button = Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, w, h).build();
        addRenderableWidget(button);
        return button;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0000000);
        int panelW = Math.min(width - 16, 400);
        int panelH = Math.min(height - 12, 228);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        int half = panelW / 2;
        int leftX = x + 10;
        int rightX = x + half + 4;
        int row1 = y + 60;
        int row2 = y + 92;
        int row3 = y + 124;
        CocUi.panel(graphics, x, y, panelW, panelH);
        CocUi.title(graphics, font, CocLang.tr("全局设置"), width / 2, y + 6);
        graphics.drawString(font, CocLang.tr("职业点（-1=公式）"), leftX, row1 - 10, CocUi.ACCENT, false);
        graphics.drawString(font, CocLang.tr("兴趣点（-1=INT×2）"), leftX, row2 - 10, CocUi.ACCENT, false);
        graphics.drawString(font, CocLang.tr("技能点总和上限（-1=不限）"), leftX, row3 - 10, CocUi.ACCENT, false);
        graphics.drawString(font, CocLang.tr("初始幸运范围（-1=默认）"), rightX, row1 - 10, CocUi.GOLD, false);
        graphics.drawString(font, CocLang.tr("初始血量范围（-1=公式）"), rightX, row2 - 10, CocUi.GOLD, false);
        graphics.drawString(font, CocLang.tr("初始 SAN 范围（-1=POW）"), rightX, row3 - 10, CocUi.GOLD, false);
        int boxW = Math.max(52, ((half - 20) - 18) / 2);
        int gap = 8;
        graphics.drawString(font, "~", rightX + boxW + 2, row1 + 5, CocUi.MUTED, false);
        graphics.drawString(font, "~", rightX + boxW + 2, row2 + 5, CocUi.MUTED, false);
        graphics.drawString(font, "~", rightX + boxW + 2, row3 + 5, CocUi.MUTED, false);
        if (!compact) {
            CocUi.wrapped(graphics, font, CocLang.tr("这些设置只影响新角色创建；已有角色卡不改变。"),
                    x + 10, y + 150, panelW - 20, CocUi.MUTED);
        }
        if (!status.isEmpty()) {
            graphics.drawString(font, CocUi.truncate(font, status, panelW - 20),
                    x + 10, y + 166, CocUi.GOLD, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
