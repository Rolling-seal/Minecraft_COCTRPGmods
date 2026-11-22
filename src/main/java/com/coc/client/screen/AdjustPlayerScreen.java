package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.KpView;
import com.coc.client.ui.CocUi;
import com.coc.net.CocNetwork;
import com.coc.net.packet.C2SKpAction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/** KP 自由调整 SAN/HP/MP/幸运（含燃运）。 */
public class AdjustPlayerScreen extends Screen {
    private final Screen parent;
    private final UUID target;
    private final String name;
    private final KpView.Player player;
    private EditBox sanBox, hpBox, mpBox, luckBox;
    private String status = "";
    private boolean compact;

    public AdjustPlayerScreen(Screen parent, UUID target, String name, KpView.Player player) {
        super(Component.literal(CocLang.tr("状态调整")));
        this.parent = parent;
        this.target = target;
        this.name = name == null ? "" : name;
        this.player = player;
    }

    @Override
    protected void init() {
        compact = height < 420 || width < 700;
        int panelW = Math.min(width - 24, 340);
        int panelH = Math.min(height - 20, 240);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        int labelW = 42;
        int boxW = Math.max(50, panelW - labelW - 76);
        int rowH = compact ? 20 : 24;
        int startY = y + 34;
        sanBox = addBox(x + 10 + labelW, startY, boxW, player == null ? "" : String.valueOf(player.san));
        hpBox = addBox(x + 10 + labelW, startY + rowH, boxW, player == null ? "" : String.valueOf(player.hp));
        mpBox = addBox(x + 10 + labelW, startY + rowH * 2, boxW, player == null ? "" : String.valueOf(player.mp));
        luckBox = addBox(x + 10 + labelW, startY + rowH * 3, boxW,
                player == null ? "" : String.valueOf(player.luck));
        addBtn(CocLang.tr("设置全部"), x + panelW - 70, startY, 60, 18, () -> setAll());
        addBtn("-5", x + panelW - 58, startY + rowH, 22, 18,
                () -> send("hp_add", -5));
        addBtn("+5", x + panelW - 34, startY + rowH, 22, 18,
                () -> send("hp_add", 5));
        addBtn("-5", x + panelW - 58, startY + rowH * 2, 22, 18,
                () -> send("mp_add", -5));
        addBtn("+5", x + panelW - 34, startY + rowH * 2, 22, 18,
                () -> send("mp_add", 5));
        addBtn("-5", x + panelW - 58, startY + rowH * 3, 22, 18,
                () -> send("luck_add", -5));
        addBtn("+5", x + panelW - 34, startY + rowH * 3, 22, 18,
                () -> send("luck_add", 5));
        addBtn(CocLang.tr("HP/MP 全满"), x + 10, startY + rowH * 4 + 2, 96, 18,
                () -> send("heal", 0));
        addBtn(CocLang.tr("返回"), x + panelW - 106, startY + rowH * 4 + 2, 96, 18, this::onClose);
    }

    private EditBox addBox(int x, int y, int width, String value) {
        EditBox box = new EditBox(font, x, y, width, 17, Component.literal(CocLang.tr("数值")));
        box.setMaxLength(3);
        box.setValue(value == null ? "" : value);
        addRenderableWidget(box);
        return box;
    }

    private void setAll() {
        try {
            int san = Integer.parseInt(sanBox.getValue().trim());
            int hp = Integer.parseInt(hpBox.getValue().trim());
            int mp = Integer.parseInt(mpBox.getValue().trim());
            int luck = Integer.parseInt(luckBox.getValue().trim());
            if (target == null) {
                status = CocLang.tr("未选择玩家。");
                return;
            }
            send("san_set", san);
            send("hp_set", hp);
            send("mp_set", mp);
            send("luck_set", luck);
            status = CocLang.tr("已发送。");
        } catch (NumberFormatException e) {
            status = CocLang.tr("四个数值都必须是数字。");
        }
    }

    private void send(String action, int value) {
        if (target == null) {
            status = CocLang.tr("未选择玩家。");
            return;
        }
        CocNetwork.sendToServer(new C2SKpAction(action, target, "", value));
        status = CocLang.tr("已发送：") + action;
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
        int panelH = Math.min(height - 20, 240);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        CocUi.panel(graphics, x, y, panelW, panelH);
        CocUi.title(graphics, font, CocLang.tr("状态调整"), width / 2, y + 5);
        graphics.drawString(font, CocUi.truncate(font, name, panelW - 20), x + 10, y + 21, CocUi.GOLD, false);
        int rowH = compact ? 20 : 24;
        int startY = y + 34;
        int labelX = x + 10;
        int labelW = 42;
        graphics.drawString(font, "SAN", labelX, startY + 4, CocUi.MUTED, false);
        graphics.drawString(font, "HP", labelX, startY + rowH + 4, CocUi.MUTED, false);
        graphics.drawString(font, "MP", labelX, startY + rowH * 2 + 4, CocUi.MUTED, false);
        graphics.drawString(font, CocLang.tr("幸运"), labelX, startY + rowH * 3 + 4, CocUi.MUTED, false);
        if (!status.isEmpty()) {
            graphics.drawString(font, CocUi.truncate(font, status, panelW - 20),
                    x + 10, y + panelH - 13, CocUi.ACCENT, false);
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
