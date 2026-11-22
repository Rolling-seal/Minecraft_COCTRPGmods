package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.ui.CocUi;
import com.coc.net.CocNetwork;
import com.coc.net.packet.C2SKpAction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/** KP 删除角色卡的二次确认。 */
public class ConfirmDeleteScreen extends Screen {
    private final Screen parent;
    private final UUID target;
    private final String name;

    public ConfirmDeleteScreen(Screen parent, UUID target, String name) {
        super(Component.literal(CocLang.tr("删除角色卡")));
        this.parent = parent;
        this.target = target;
        this.name = name == null ? "" : name;
    }

    @Override
    protected void init() {
        int w = Math.min(320, width - 24);
        int x = width / 2 - w / 2;
        int y = height / 2 + 12;
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("确认删除")), b -> {
            CocNetwork.sendToServer(new C2SKpAction("delete", target, "confirm", 0));
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(x, y, w / 2 - 4, 20).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("取消")), b -> onClose())
                .bounds(x + w / 2 + 4, y, w / 2 - 4, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xC0000000);
        int w = Math.min(320, width - 24);
        int x = width / 2 - w / 2;
        int h = 120;
        int y = height / 2 - 70;
        CocUi.panel(graphics, x, y, w, h);
        CocUi.title(graphics, font, CocLang.tr("删除角色卡"), width / 2, y + 8);
        graphics.drawCenteredString(font, CocLang.tr("玩家：") + name, width / 2, y + 34, CocUi.TEXT);
        graphics.drawCenteredString(font, CocLang.tr("将永久删除角色卡、线索与疯狂状态。"), width / 2, y + 50, CocUi.MUTED);
        graphics.drawCenteredString(font, CocLang.tr("此操作无法撤销。"), width / 2, y + 64, CocUi.RED);
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
