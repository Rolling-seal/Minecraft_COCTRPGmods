package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.ClientCache;
import com.coc.client.CurtainView;
import com.coc.client.ui.CocUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 玩家幕布：显示本幕发生了什么，并列出最多三个只读的常规选项。 */
public class CurtainScreen extends Screen {
    private final CurtainView view;
    private final List<Line> lines = new ArrayList<>();
    private int scroll;
    private boolean compact;
    private int textX, textY, textW, textH;

    public CurtainScreen(CurtainView view) {
        super(Component.literal(CocLang.tr("幕布")));
        this.view = view == null ? new CurtainView() : view;
    }

    @Override
    protected void init() {
        compact = height < 420 || width < 700;
        lines.clear();
        int panelW = Math.min(width - 24, compact ? width - 16 : 620);
        int panelH = Math.min(height - 28, compact ? height - 16 : 380);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        textX = x + 16;
        textY = y + (compact ? 42 : 62);
        textW = panelW - 32;
        textH = panelH - (compact ? 70 : 94);
        lines.add(new Line(CocLang.tr("发生了什么"), CocUi.ACCENT));
        wrap(CocLang.tr(view.description), textW, CocUi.TEXT);
        if (!view.options.isEmpty()) {
            lines.add(new Line("", CocUi.TEXT));
            lines.add(new Line(CocLang.tr("可能的行动（仅供查看，由 KP 裁定）"), CocUi.GOLD));
            int shown = 0;
            for (String option : view.options) {
                if (shown >= 3) break;
                show(new Line((shown + 1) + ". " + CocLang.tr(option), CocUi.MUTED));
                shown++;
            }
        }
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("继续")), b -> onClose())
                .bounds(width / 2 - 40, y + panelH - 24, 80, 18).build());
        scroll = 0;
    }

    private void wrap(String text, int width, int color) {
        if (text == null) return;
        for (String paragraph : text.replace("\r", "").split("\n", -1)) {
            if (paragraph.isBlank()) {
                lines.add(new Line("", color));
                continue;
            }
            show(new Line(paragraph, color));
        }
    }

    /** 按宽度拆行，保留颜色。 */
    private void show(Line source) {
        String paragraph = source.text;
        StringBuilder line = new StringBuilder();
        for (char c : paragraph.toCharArray()) {
            if (font.width(line.toString() + c) > textW && line.length() > 0) {
                lines.add(new Line(line.toString(), source.color));
                line.setLength(0);
            }
            line.append(c);
        }
        if (line.length() > 0) lines.add(new Line(line.toString(), source.color));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xE0060403);
        int panelW = Math.min(width - 24, compact ? width - 16 : 620);
        int panelH = Math.min(height - 28, compact ? height - 16 : 380);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        // 两侧深红幕布与黄铜杆
        int curtainW = Math.max(16, x / 3);
        graphics.fill(0, 0, curtainW, height, 0xFF2A0808);
        graphics.fill(width - curtainW, 0, width, height, 0xFF2A0808);
        graphics.fill(0, 0, width, 5, 0xFF8A6A2F);
        graphics.fill(0, height - 5, width, height, 0xFF8A6A2F);
        for (int i = 0; i < 7; i++) {
            int cx = 4 + i * curtainW / 7;
            graphics.fill(cx, 5, Math.min(curtainW, cx + 2), height - 5, 0x33000000);
            int rx = width - curtainW + i * curtainW / 7 + 2;
            graphics.fill(rx, 5, Math.min(width, rx + 2), height - 5, 0x33000000);
        }
        CocUi.panel(graphics, x, y, panelW, panelH);
        String scene = view.scenario == null || view.scenario.isBlank()
                ? CocLang.tr("调查员手记") : CocLang.tr(view.scenario);
        graphics.drawCenteredString(font, scene, width / 2, y + (compact ? 8 : 12), CocUi.MUTED);
        String title = view.title == null || view.title.isBlank()
                ? CocLang.tr("新的场景") : CocLang.tr(view.title);
        graphics.drawCenteredString(font, title, width / 2, y + (compact ? 21 : 28), CocUi.GOLD);
        String act = view.act > 0
                ? CocLang.tr("第 ") + view.act + CocLang.tr(" 幕") + (view.total > 0 ? CocLang.tr(" / 共 ") + view.total + CocLang.tr(" 幕") : "")
                : CocLang.tr("序幕");
        graphics.drawCenteredString(font, act, width / 2, y + (compact ? 32 : 42), CocUi.ACCENT);
        graphics.fill(x + 20, y + (compact ? 42 : 52), x + panelW - 20, y + (compact ? 43 : 53), 0x668A6A2F);

        graphics.enableScissor(textX, textY, textX + textW, textY + textH);
        int lineHeight = compact ? 10 : 11;
        int maxScroll = Math.max(0, lines.size() * lineHeight - textH);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        for (int i = 0; i < lines.size(); i++) {
            int ly = textY + i * lineHeight - scroll;
            if (ly + lineHeight < textY || ly > textY + textH) continue;
            graphics.drawString(font, lines.get(i).text, textX, ly, lines.get(i).color, false);
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            String hint = CocLang.tr("滚轮翻阅 · ") + (scroll * 100 / Math.max(1, maxScroll)) + "%";
            graphics.drawString(font, hint, textX, textY + textH + 2, CocUi.MUTED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll -= (int) (delta * 18);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Line(String text, int color) {
    }
}
