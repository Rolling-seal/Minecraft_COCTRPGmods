package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.ClientCache;
import com.coc.client.ui.CocList;
import com.coc.client.ui.CocUi;
import com.coc.data.CharacterSheet;
import com.coc.data.Handout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 线索日志：KP 手书与事件线索。响应小窗口。 */
public class JournalScreen extends Screen {
    private CocList<Handout> list;
    private boolean compact;

    public JournalScreen() {
        super(Component.literal(CocLang.tr("线索日志")));
    }

    @Override
    protected void init() {
        compact = height < 420 || width < 700;
        CharacterSheet sheet = ClientCache.self();
        List<Handout> handouts = new ArrayList<>();
        if (sheet != null) handouts.addAll(sheet.handouts);
        Collections.reverse(handouts);
        int listX = 12;
        int listY = compact ? 36 : 66;
        int listW = width - 24;
        int listH = Math.max(50, height - listY - (compact ? 96 : 144));
        list = new CocList<>(listX, listY, listW, listH, compact ? 16 : 18,
                (g, font, handout, x, y, w, h, hovered, selected) -> {
                    g.drawString(font, CocUi.truncate(font, CocLang.tr(handout.title()), w - 100), x, y + 2,
                            selected ? CocUi.GOLD : CocUi.TEXT, false);
                    String day = CocUi.dayOf(handout.gameTime());
                    g.drawString(font, day, x + w - font.width(day) - 4, y + 2, CocUi.MUTED, false);
                });
        list.setItems(handouts);
        if (!handouts.isEmpty()) list.setSelectedIndex(0);
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("导入剧本")),
                        b -> {
                            if (minecraft != null) minecraft.setScreen(new ImportScenarioScreen(this));
                        })
                .bounds(width / 2 - 112, height - 22, 104, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("关闭")), b -> onClose())
                .bounds(width / 2 + 8, height - 22, 104, 18).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0000000);
        CocUi.title(graphics, font, CocLang.tr("线索日志"), width / 2, compact ? 6 : 24);
        CharacterSheet sheet = ClientCache.self();
        if (sheet == null || sheet.handouts.isEmpty()) {
            graphics.drawCenteredString(font, CocLang.tr("尚未获得任何线索。"), width / 2, height / 2, CocUi.MUTED);
        } else {
            graphics.drawString(font, CocLang.tr("共 ") + sheet.handouts.size() + CocLang.tr(" 条 · 点击查看全文"),
                    12, compact ? 25 : 52, CocUi.MUTED, false);
            if (list != null) list.render(graphics, font, mouseX, mouseY);
            Handout selected = list == null ? null : list.getSelected();
            int detailY = height - (compact ? 88 : 132);
            int detailH = compact ? 58 : 92;
            CocUi.panelLight(graphics, 12, detailY, width - 24, detailH);
            if (selected != null) {
                graphics.drawString(font, CocUi.truncate(font, CocLang.tr(selected.title()), width - 110),
                        20, detailY + 6, CocUi.GOLD, false);
                graphics.drawString(font, CocUi.dayOf(selected.gameTime()), width - 72, detailY + 6, CocUi.MUTED, false);
                CocUi.wrapped(graphics, font, CocLang.tr(selected.text()), 20, detailY + 20, width - 40, CocUi.TEXT);
            } else {
                graphics.drawString(font, CocLang.tr("选择一条线索查看全文。"), 20, detailY + 8, CocUi.MUTED, false);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (list != null && list.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (list != null && list.mouseScrolled(mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
