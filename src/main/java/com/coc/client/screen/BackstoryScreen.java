package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.ui.CocUi;
import com.coc.data.CharacterSheet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 角色背景：KP 与玩家都能从角色卡打开，不再受小窗口布局限制。 */
public class BackstoryScreen extends Screen {
    private final Screen parent;
    private final CharacterSheet sheet;
    private final List<Line> lines = new ArrayList<>();
    private int scroll;
    private boolean compact;
    private int textX, textY, textW, textH;

    public BackstoryScreen(Screen parent, CharacterSheet sheet) {
        super(Component.literal(CocLang.tr("角色背景")));
        this.parent = parent;
        this.sheet = sheet;
    }

    @Override
    protected void init() {
        compact = height < 420 || width < 700;
        int panelW = Math.min(width - 24, 560);
        int panelH = Math.min(height - 28, 360);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        textX = x + 16;
        textY = y + 28;
        textW = panelW - 32;
        textH = panelH - (compact ? 76 : 72);
        lines.clear();
        if (sheet != null) {
            add(CocLang.tr("姓名：") + sheet.name + CocLang.tr("　职业：") + CocLang.tr(sheet.occupationName) + CocLang.tr("　年龄：") + sheet.age, CocUi.GOLD);
            lines.add(new Line("", CocUi.TEXT));
            for (String key : CharacterSheet.BACKSTORY_KEYS) {
                String value = sheet.backstory.getOrDefault(key, "");
                add(CocLang.tr(key), CocUi.ACCENT);
                add(value == null || value.isBlank() ? CocLang.tr("（未填写）") : value, CocUi.TEXT);
                lines.add(new Line("", CocUi.TEXT));
            }
            add(CocLang.tr("备注"), CocUi.ACCENT);
            add(sheet.notes == null || sheet.notes.isBlank() ? CocLang.tr("（未填写）") : sheet.notes, CocUi.TEXT);
        }
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("返回")), b -> onClose())
                .bounds(width / 2 - 40, y + panelH - 25, 80, 18).build());
        scroll = 0;
    }

    private void add(String text, int color) {
        if (text == null) return;
        for (String paragraph : text.replace("\r", "").split("\n", -1)) {
            if (paragraph.isBlank()) {
                lines.add(new Line("", color));
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (char c : paragraph.toCharArray()) {
                if (font.width(line.toString() + c) > textW && line.length() > 0) {
                    lines.add(new Line(line.toString(), color));
                    line.setLength(0);
                }
                line.append(c);
            }
            if (line.length() > 0) lines.add(new Line(line.toString(), color));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0000000);
        int panelW = Math.min(width - 24, 560);
        int panelH = Math.min(height - 28, 360);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        CocUi.panel(graphics, x, y, panelW, panelH);
        CocUi.title(graphics, font, CocLang.tr("角色背景"), width / 2, y + 6);
        graphics.enableScissor(textX, textY, textX + textW, textY + textH);
        int lh = compact ? 10 : 11;
        int maxScroll = Math.max(0, lines.size() * lh - textH);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        for (int i = 0; i < lines.size(); i++) {
            int ly = textY + i * lh - scroll;
            if (ly + lh < textY || ly > textY + textH) continue;
            graphics.drawString(font, lines.get(i).text, textX, ly, lines.get(i).color, false);
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            graphics.drawString(font, CocLang.tr("滚轮翻阅"), textX, textY + textH + 2, CocUi.MUTED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll -= (int) (delta * 18);
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Line(String text, int color) {
    }
}
