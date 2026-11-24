package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.BookView;
import com.coc.client.ClientCache;
import com.coc.client.ui.CocList;
import com.coc.client.ui.CocUi;
import com.coc.net.CocNetwork;
import com.coc.net.packet.C2SKpAction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** KP 剧情之书：主线每幕走向 / 支线触发条件与结果。 */
public class StoryBookScreen extends Screen {
    private final Screen parent;
    private final String scenarioId;
    private int mode;
    private int selectedActIndex;
    private int selectedSideIndex;
    private int leftScroll, rightScroll;
    private CocList<BookView.Act> actList;
    private CocList<BookView.Side> sideList;
    private CocList<Line> detailList;
    private boolean requested;
    private boolean initialized;
    private boolean compact;
    private BookView book;

    public StoryBookScreen(Screen parent, String scenarioId, int mode) {
        super(Component.literal(CocLang.tr("剧情之书")));
        this.parent = parent;
        this.scenarioId = scenarioId == null ? "" : scenarioId;
        this.mode = mode == 0 ? 0 : 1;
    }

    @Override
    protected void init() {
        compact = height < 420 || width < 700;
        book = ClientCache.book;
        if (!requested) {
            CocNetwork.sendToServer(new C2SKpAction("book", null, scenarioId, 0));
            requested = true;
        }
        int top = compact ? 36 : 66;
        int tabsY = compact ? 15 : 26;
        addBtn(CocLang.tr("主线剧情"), 12, tabsY, compact ? 70 : 88, compact ? 16 : 20, () -> {
            mode = 0;
            rebuildWidgets();
        });
        addBtn(CocLang.tr("支线剧情"), 16 + (compact ? 70 : 88), tabsY, compact ? 70 : 88, compact ? 16 : 20, () -> {
            mode = 1;
            rebuildWidgets();
        });
        addBtn(CocLang.tr("关闭"), width - (compact ? 66 : 82), tabsY, compact ? 54 : 68, compact ? 16 : 20, this::onClose);

        int bottom = height - (compact ? 25 : 30);
        int leftW = Math.max(120, width / 3);
        int leftH = Math.max(70, bottom - top - 4);
        int rightX = leftW + 20;
        int rightW = Math.max(140, width - rightX - 12);
        int detailTop = top + 4;
        int detailH = Math.max(60, leftH - (compact ? 28 : 32));
        if (book == null || !scenarioId.equals(book.scenarioId)) {
            return;
        }
        if (mode == 0) {
            actList = new CocList<>(12, top, leftW, leftH, compact ? 20 : 22,
                    (g, font, act, x, y, w, h, hovered, selected) -> {
                        int color = selected ? CocUi.GOLD : (act.act == book.index + 1 ? CocUi.ACCENT : CocUi.TEXT);
                        g.drawString(font, CocUi.truncate(font, CocLang.tr("第") + act.act + CocLang.tr("幕 · ")
                                        + CocLang.tr(act.title), w - 4),
                                x, y + (compact ? 5 : 3), color, false);
                        if (!compact) {
                            g.drawString(font, CocUi.truncate(font, CocLang.tr(act.summary), w - 4),
                                    x, y + 13, CocUi.MUTED, false);
                        }
                    });
            actList.setItems(book.acts);
            if (!book.acts.isEmpty()) {
                if (!initialized && book.active && book.index >= 0) {
                    selectedActIndex = Math.min(book.index, book.acts.size() - 1);
                }
                selectedActIndex = Math.max(0, Math.min(selectedActIndex, book.acts.size() - 1));
                actList.setSelectedIndex(selectedActIndex);
            }
            detailList = new CocList<>(rightX, detailTop, rightW, detailH, 10,
                    (g, font, line, x, y, w, h, hovered, selected) ->
                            g.drawString(font, line.text, x, y, line.color, false));
            rebuildDetail();
            addBtn(CocLang.tr("向玩家显示此幕"), 12, bottom + 3, Math.min(150, leftW), 18, () -> {
                BookView.Act act = actList == null ? null : actList.getSelected();
                if (act == null) return;
                CocNetwork.sendToServer(new C2SKpAction("scenario_act", null, book.scenarioId, act.act));
            });
        } else {
            sideList = new CocList<>(12, top, leftW, leftH, compact ? 20 : 22,
                    (g, font, side, x, y, w, h, hovered, selected) -> {
                        g.drawString(font, CocUi.truncate(font, CocLang.tr(side.title), w - 4), x, y + (compact ? 4 : 3),
                                selected ? CocUi.GOLD : CocUi.TEXT, false);
                        g.drawString(font, CocUi.truncate(font, CocLang.tr("第") + side.act + CocLang.tr("幕"), w - 4),
                                x, y + 13, CocUi.MUTED, false);
                    });
            sideList.setItems(book.sides);
            if (!book.sides.isEmpty()) {
                selectedSideIndex = Math.max(0, Math.min(selectedSideIndex, book.sides.size() - 1));
                sideList.setSelectedIndex(selectedSideIndex);
            }
            detailList = new CocList<>(rightX, detailTop, rightW, detailH, 10,
                    (g, font, line, x, y, w, h, hovered, selected) ->
                            g.drawString(font, line.text, x, y, line.color, false));
            rebuildDetail();
            initialized = true;
        }
        addBtn(CocLang.tr("返回"), width - 76, bottom + 3, 64, 18, this::onClose);
    }

    private void rebuildDetail() {
        if (book == null || detailList == null) return;
        List<Line> lines = new ArrayList<>();
        int width = Math.max(100, detailList.width() - 8);
        if (mode == 0) {
            BookView.Act act = actList == null ? null : actList.getSelected();
            if (act == null) {
                lines.add(new Line(CocLang.tr("选择左边的一幕。"), CocUi.MUTED));
            } else {
                addWrapped(lines, CocLang.tr("第 ") + act.act + CocLang.tr(" 幕 · ")
                        + CocLang.tr(act.title), CocUi.GOLD, width);
                addWrapped(lines, CocLang.tr("概要：") + CocLang.tr(act.summary), CocUi.MUTED, width);
                addWrapped(lines, CocLang.tr("触发：") + CocLang.tr(act.trigger), CocUi.ACCENT, width);
                addWrapped(lines, CocLang.tr(act.description), CocUi.TEXT, width);
                lines.add(new Line("", CocUi.TEXT));
                addWrapped(lines, CocLang.tr("可能走向（KP 参考，可自由改）："), CocUi.GOLD, width);
                addOptions(lines, act.options, width);
                if (!act.sideTitles.isEmpty()) {
                    lines.add(new Line("", CocUi.TEXT));
                    List<String> sideTitles = new ArrayList<>();
                    for (String sideTitle : act.sideTitles) sideTitles.add(CocLang.tr(sideTitle));
                    addWrapped(lines, CocLang.tr("可触发支线：") + String.join(" / ", sideTitles),
                            CocUi.ACCENT, width);
                }
            }
        } else {
            BookView.Side side = sideList == null ? null : sideList.getSelected();
            if (side == null) {
                lines.add(new Line(CocLang.tr("选择左边的支线。"), CocUi.MUTED));
            } else {
                addWrapped(lines, CocLang.tr(side.title), CocUi.GOLD, width);
                addWrapped(lines, CocLang.tr("所属：第 ") + side.act + CocLang.tr(" 幕"), CocUi.MUTED, width);
                addWrapped(lines, CocLang.tr("触发：") + CocLang.tr(side.trigger), CocUi.ACCENT, width);
                addWrapped(lines, CocLang.tr(side.description), CocUi.TEXT, width);
                lines.add(new Line("", CocUi.TEXT));
                addWrapped(lines, CocLang.tr("触发后可能发生："), CocUi.GOLD, width);
                addOptions(lines, side.options, width);
            }
        }
        detailList.setItems(lines);
        detailList.setScroll(0);
    }

    private void addOptions(List<Line> lines, List<BookView.Option> options, int width) {
        if (options == null || options.isEmpty()) {
            lines.add(new Line(CocLang.tr("（没有固定选项，KP 自行模拟。）"), CocUi.MUTED));
            return;
        }
        for (BookView.Option o : options) {
            addWrapped(lines, "▸ " + CocLang.tr(o.text), CocUi.TEXT, width);
            addWrapped(lines, CocLang.tr("  检定：") + CocLang.tr(o.skillName)
                    + " / " + CocLang.tr(o.difficulty), CocUi.ACCENT, width);
            boolean marked = false;
            if (!o.success.isBlank() || !o.successClue.isBlank()) {
                marked = true;
                if (!o.success.isBlank()) addWrapped(lines, CocLang.tr("  成功：")
                        + CocLang.tr(o.success), CocUi.GREEN, width);
                addClue(lines, "成功线索", o.successClue, width);
            }
            if (!o.failure.isBlank() || !o.failureClue.isBlank()) {
                marked = true;
                if (!o.failure.isBlank()) addWrapped(lines, CocLang.tr("  失败：")
                        + CocLang.tr(o.failure), CocUi.RED, width);
                addClue(lines, "失败线索", o.failureClue, width);
            }
            if (!o.outcome.isBlank() || !o.clue.isBlank()) {
                marked = true;
                if (!o.outcome.isBlank()) addWrapped(lines, CocLang.tr("  结果：")
                        + CocLang.tr(o.outcome), CocUi.TEXT, width);
                addClue(lines, "线索", o.clue, width);
            }
            if (!marked) {
                addWrapped(lines, CocLang.tr("  线索：无（本选项不给线索）"), CocUi.MUTED, width);
            }
            lines.add(new Line("", CocUi.TEXT));
        }
    }

    /** 线索行可点击：点击后选择在线玩家发送。 */
    private void addClue(List<Line> lines, String label, String clue, int width) {
        if (clue == null || clue.isBlank()) {
            addWrapped(lines, "  " + CocLang.tr(label) + "：" + CocLang.tr("无"), CocUi.MUTED, width);
            return;
        }
        addClueWrapped(lines, "  " + CocLang.tr(label) + CocLang.tr("（点击发送）：") + CocLang.tr(clue),
                clue, label, width);
    }

    private void addClueWrapped(List<Line> lines, String text, String clue, String label, int width) {
        if (text == null || text.isBlank()) return;
        for (String paragraph : text.replace("\r", "").split("\n", -1)) {
            if (paragraph.isBlank()) {
                lines.add(new Line("", CocUi.GOLD));
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (char c : paragraph.toCharArray()) {
                if (font.width(line.toString() + c) > width && line.length() > 0) {
                    lines.add(new Line(line.toString(), CocUi.GOLD, clue, label));
                    line.setLength(0);
                }
                line.append(c);
            }
            if (line.length() > 0) lines.add(new Line(line.toString(), CocUi.GOLD, clue, label));
        }
    }

    private void addWrapped(List<Line> lines, String text, int color, int width) {
        if (text == null || text.isBlank()) return;
        for (String paragraph : text.replace("\r", "").split("\n", -1)) {
            if (paragraph.isBlank()) {
                lines.add(new Line("", color));
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (char c : paragraph.toCharArray()) {
                if (font.width(line.toString() + c) > width && line.length() > 0) {
                    lines.add(new Line(line.toString(), color));
                    line.setLength(0);
                }
                line.append(c);
            }
            if (line.length() > 0) lines.add(new Line(line.toString(), color));
        }
    }

    private void addBtn(String label, int x, int y, int w, int h, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, w, h).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0000000);
        CocUi.title(graphics, font, CocLang.tr("剧情之书"), width / 2, compact ? 2 : 4);
        BookView current = ClientCache.book;
        if (current == null || !scenarioId.equals(current.scenarioId)) {
            graphics.drawCenteredString(font, CocLang.tr("正在翻阅剧情之书……"), width / 2, height / 2, CocUi.MUTED);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        int top = compact ? 36 : 66;
        if (!compact) {
            graphics.drawString(font, CocUi.truncate(font, CocLang.tr(current.title) + " · " +
                    (mode == 0 ? CocLang.tr("主线剧情") : CocLang.tr("支线剧情")), width - 24),
                    12, top - 12, CocUi.GOLD, false);
        }
        if (mode == 0) {
            if (actList != null) actList.render(graphics, font, mouseX, mouseY);
        } else {
            if (sideList != null) sideList.render(graphics, font, mouseX, mouseY);
        }
        if (detailList != null) detailList.render(graphics, font, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mode == 0 && actList != null && actList.mouseClicked(mouseX, mouseY, button)) {
            selectedActIndex = actList.getSelectedIndex();
            rebuildDetail();
            return true;
        }
        if (mode == 1 && sideList != null && sideList.mouseClicked(mouseX, mouseY, button)) {
            selectedSideIndex = sideList.getSelectedIndex();
            rebuildDetail();
            return true;
        }
        if (detailList != null && detailList.mouseClicked(mouseX, mouseY, button)) {
            Line line = detailList.getSelected();
            if (line != null && line.clue != null && !line.clue.isBlank() && minecraft != null) {
                minecraft.setScreen(new ClueTargetScreen(this, line.clue, line.clueLabel));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mode == 0 && actList != null && actList.mouseScrolled(mouseX, mouseY, delta)) return true;
        if (mode == 1 && sideList != null && sideList.mouseScrolled(mouseX, mouseY, delta)) return true;
        if (detailList != null && detailList.mouseScrolled(mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    public void refresh() {
        int act = selectedActIndex;
        int side = selectedSideIndex;
        if (minecraft != null) rebuildWidgets();
        selectedActIndex = act;
        selectedSideIndex = side;
        if (actList != null && act >= 0 && act < actList.items().size()) actList.setSelectedIndex(act);
        if (sideList != null && side >= 0 && side < sideList.items().size()) sideList.setSelectedIndex(side);
        rebuildDetail();
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Line(String text, int color, String clue, String clueLabel) {
        Line(String text, int color) {
            this(text, color, "", "");
        }
    }
}
