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

/** 全局设置：角色创建点数规则。KP 通过物品打开。 */
public class GlobalSettingsScreen extends Screen {
    private EditBox occBox;
    private EditBox intBox;
    private EditBox totalBox;
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
        int panelW = Math.min(width - 24, 380);
        int panelH = Math.min(height - 16, 236);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        allowButton = addBtn((allow ? "✔ " : "✘ ") + CocLang.tr("允许玩家自行分配点数"),
                x + 10, y + 30, panelW - 20, 18, () -> {
                    allow = !allow;
                    allowButton.setMessage(Component.literal((allow ? "✔ " : "✘ ")
                            + CocLang.tr("允许玩家自行分配点数")));
                });
        occBox = box(x + 10, y + 62, 64, view == null ? "-1" : String.valueOf(view.occupationPoints));
        intBox = box(x + 10, y + 92, 64, view == null ? "-1" : String.valueOf(view.interestPoints));
        totalBox = box(x + 10, y + 122, 64, view == null ? "-1" : String.valueOf(view.totalPoints));
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
            CocNetwork.sendToServer(new C2SGlobalSettings(allow, occ, interest, total));
            status = CocLang.tr("已提交；等待服务器同步。");
        } catch (NumberFormatException e) {
            status = CocLang.tr("点数必须是整数；-1 表示自动。");
        }
    }

    private int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        return Integer.parseInt(value.trim());
    }

    public void onSettings(GlobalSettingsView view) {
        if (view == null) return;
        allow = view.allowPoints;
        if (occBox != null) occBox.setValue(String.valueOf(view.occupationPoints));
        if (intBox != null) intBox.setValue(String.valueOf(view.interestPoints));
        if (totalBox != null) totalBox.setValue(String.valueOf(view.totalPoints));
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
        int panelW = Math.min(width - 24, 380);
        int panelH = Math.min(height - 16, 236);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        CocUi.panel(graphics, x, y, panelW, panelH);
        CocUi.title(graphics, font, CocLang.tr("全局设置"), width / 2, y + 6);
        graphics.drawString(font, CocLang.tr("职业点（-1 = 职业公式）"), x + 82, y + 66, CocUi.ACCENT, false);
        graphics.drawString(font, CocLang.tr("兴趣点（-1 = INT×2）"), x + 82, y + 96, CocUi.ACCENT, false);
        graphics.drawString(font, CocLang.tr("总点数上限（-1 = 不额外限制）"), x + 82, y + 126, CocUi.ACCENT, false);
        if (!compact) {
            CocUi.wrapped(graphics, font, CocLang.tr("这些设置只影响新角色创建；已有角色卡不改变。"),
                    x + 10, y + 154, panelW - 20, CocUi.MUTED);
        }
        if (!status.isEmpty()) {
            graphics.drawString(font, CocUi.truncate(font, status, panelW - 20),
                    x + 10, y + 164, CocUi.GOLD, false);
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
