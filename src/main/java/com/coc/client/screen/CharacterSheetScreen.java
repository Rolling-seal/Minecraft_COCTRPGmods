package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.ClientCache;
import com.coc.client.ui.CocList;
import com.coc.client.ui.CocUi;
import com.coc.data.CharacterSheet;
import com.coc.rules.SkillDef;
import com.coc.rules.SkillRegistry;
import com.coc.rules.Stat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 角色卡：左侧属性/状态，右侧可滚动技能列表。响应 427×240 起的 GUI 尺寸。 */
public class CharacterSheetScreen extends Screen {
    private final UUID target;
    private final boolean editable;
    private CharacterSheet sheet;
    private CocList<SkillRow> skillList;
    private boolean compact;
    private int sheetPanelX, sheetPanelY, sheetPanelW, sheetPanelH;
    private int sheetScroll;
    private int sheetMaxScroll;

    public CharacterSheetScreen(UUID target, boolean editable) {
        super(Component.literal(CocLang.tr("调查员档案")));
        this.target = target;
        this.editable = editable;
    }

    public UUID target() {
        return target;
    }

    public boolean editable() {
        return editable;
    }

    @Override
    protected void init() {
        this.compact = height < 420 || width < 700;
        this.sheet = ClientCache.sheet(target);
        this.skillList = null;
        if (sheet != null) {
            int leftW = Math.max(150, width / 2 - 22);
            sheetPanelX = 12;
            sheetPanelY = compact ? 24 : 40;
            sheetPanelW = leftW;
            sheetPanelH = Math.max(110, (height - 34) - sheetPanelY);
            int listX = Math.max(width / 2 + 6, 12 + leftW + 8);
            int listY = compact ? 48 : 78;
            int listW = Math.max(100, width - listX - 12);
            int listH = Math.max(60, height - listY - 36);
            skillList = new CocList<>(listX, listY, listW, listH, compact ? 15 : 16,
                    (g, font, row, x, y, w, h, hovered, selected) -> {
                        int color = row.marked ? CocUi.GOLD : (row.value >= 50 ? CocUi.TEXT : CocUi.MUTED);
                        g.drawString(font, CocUi.truncate(font, CocLang.tr(row.def.name), w - 40), x, y + 3, color, false);
                        String value = row.marked ? row.value + " ◈" : String.valueOf(row.value);
                        g.drawString(font, value, x + w - font.width(value), y + 3, color, false);
                    });
            List<SkillRow> rows = new ArrayList<>();
            for (SkillDef def : SkillRegistry.all()) {
                rows.add(new SkillRow(def, sheet.skill(def.id), sheet.improvementMarks.contains(def.id)));
            }
            skillList.setItems(rows);
        }

        int btnY = height - 24;
        int btnW = Math.max(44, Math.min(100, (width - 40) / 4 - 4));
        int gap = 4;
        int startX = Math.max(8, (width - (btnW * 4 + gap * 3)) / 2);
        if (sheet == null) {
            if (minecraft != null && minecraft.player != null && target.equals(minecraft.player.getUUID())) {
                addRenderableWidget(Button.builder(Component.literal(CocLang.tr("创建角色")), b -> {
                    if (minecraft != null) minecraft.setScreen(new CharacterCreationScreen());
                }).bounds(width / 2 - 50, height / 2 + 20, 100, 20).build());
            }
            addRenderableWidget(Button.builder(Component.literal(CocLang.tr("关闭")), b -> onClose())
                    .bounds(width / 2 - 40, btnY + 6, 80, 18).build());
            return;
        }

        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("线索")), b -> {
            if (minecraft != null) minecraft.setScreen(new JournalScreen());
        }).bounds(startX, btnY, btnW, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("设置")), b -> {
            if (minecraft != null) minecraft.setScreen(new SettingsScreen(this));
        }).bounds(startX + btnW + gap, btnY, btnW, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("刷新")), b -> {
            com.coc.net.CocNetwork.sendToServer(new com.coc.net.packet.C2SRequestSync());
            rebuildWidgets();
        }).bounds(startX + (btnW + gap) * 2, btnY, btnW, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("关闭")), b -> onClose())
                .bounds(startX + (btnW + gap) * 3, btnY, btnW, 18).build());
    }

    public void refresh() {
        int scroll = skillList == null ? 0 : skillList.getScroll();
        if (minecraft != null) rebuildWidgets();
        if (skillList != null) skillList.setScroll(scroll);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xA0000000);
        CocUi.title(graphics, font, CocLang.tr("调查员档案"), width / 2, compact ? 4 : 6);
        if (sheet == null) {
            CocUi.panel(graphics, width / 2 - 140, height / 2 - 50, 280, 110);
            graphics.drawCenteredString(font, CocLang.tr("尚未创建角色"), width / 2, height / 2 - 30, CocUi.TEXT);
            graphics.drawCenteredString(font, CocLang.tr("点击下方按钮，或按 C 重新打开"), width / 2, height / 2 - 14, CocUi.MUTED);
        } else {
            drawSheet(graphics);
            if (skillList != null) {
                graphics.drawString(font, CocLang.tr("技能（◈ 成长标记）"), skillList.x(), skillList.y() - 13, CocUi.ACCENT, false);
                skillList.render(graphics, font, mouseX, mouseY);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSheet(GuiGraphics graphics) {
        int leftX = sheetPanelX;
        int leftY = sheetPanelY;
        int leftW = sheetPanelW;
        int leftH = sheetPanelH;
        CocUi.panel(graphics, leftX, leftY, leftW, leftH);
        graphics.enableScissor(leftX + 2, leftY + 2, leftX + leftW - 2, leftY + leftH - 2);

        int y = leftY + 6 - sheetScroll;
        graphics.drawString(font, CocUi.truncate(font, sheet.name, leftW - 16), leftX + 8, y, CocUi.GOLD, false);
        y += compact ? 16 : 18;
        graphics.drawString(font, CocUi.truncate(font, CocLang.tr(sheet.occupationName) + " · " + sheet.age + CocLang.tr(" 岁"),
                leftW - 16), leftX + 8, y, CocUi.MUTED, false);
        y += compact ? 16 : 20;

        Stat[] stats = {Stat.STR, Stat.CON, Stat.SIZ, Stat.DEX, Stat.APP, Stat.INT, Stat.POW, Stat.EDU, Stat.LUCK};
        int cellW = Math.max(45, (leftW - 16) / 3);
        int lineH = compact ? 13 : 18;
        for (int i = 0; i < stats.length; i++) {
            int col = i % 3;
            int row = i / 3;
            int x = leftX + 8 + col * cellW;
            int sy = y + row * lineH;
            Stat stat = stats[i];
            String label = compact ? stat.abbr() : stat.abbr() + " " + CocLang.tr(stat.cn());
            graphics.drawString(font, label, x, sy, CocUi.ACCENT, false);
            String value = String.valueOf(sheet.stats.get(stat));
            graphics.drawString(font, value, x + (compact ? 40 : 56), sy, CocUi.TEXT, false);
        }
        y += 3 * lineH + (compact ? 6 : 12);

        int barGap = compact ? 15 : 24;
        int barW = leftW - 40;
        drawBar(graphics, leftX + 8, y, barW, "SAN", sheet.san, sheet.maxSan,
                CocUi.sanColor(sheet.san, sheet.maxSan));
        drawBar(graphics, leftX + 8, y + barGap, barW, "HP", sheet.hp, sheet.maxHp, CocUi.RED);
        drawBar(graphics, leftX + 8, y + barGap * 2, barW, "MP", sheet.mp, sheet.maxMp, CocUi.BLUE);
        drawBar(graphics, leftX + 8, y + barGap * 3, barW, CocLang.tr("幸运"), sheet.stats.luck, 99, CocUi.GREEN);
        y += barGap * 4 + 4;

        graphics.drawString(font, CocLang.tr("移动 ") + sheet.mov + CocLang.tr("  伤害 ") + sheet.db + CocLang.tr("  体格 ") + sheet.build,
                leftX + 8, y, CocUi.TEXT, false);
        y += 11;
        graphics.drawString(font, CocLang.tr("神话 ") + sheet.mythos + CocLang.tr("  最大 SAN ") + sheet.maxSan,
                leftX + 8, y, sheet.mythos > 0 ? CocUi.GOLD : CocUi.MUTED, false);
        y += 12;
        long gameTime = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;
        y = drawWrapped(graphics, CocLang.tr("状态：") + CocLang.message(sheet.statusLine(gameTime)),
                leftX + 8, y, leftW - 16, sheet.isInsane(gameTime) ? CocUi.RED : CocUi.GREEN) + 2;

        if (!sheet.phobias.isEmpty() || !sheet.manias.isEmpty()) {
            String phobias = String.join(" / ", sheet.phobias.stream().map(CocLang::tr).toList());
            String manias = String.join(" / ", sheet.manias.stream().map(CocLang::tr).toList());
            String mental = CocLang.tr("恐惧：") + phobias
                    + (sheet.manias.isEmpty() ? "" : CocLang.tr("  狂躁：") + manias);
            y = drawWrapped(graphics, mental, leftX + 8, y, leftW - 16, CocUi.MUTED) + 2;
        }

        y += 2;
        for (String key : CharacterSheet.BACKSTORY_KEYS) {
            String value = sheet.backstory.getOrDefault(key, "");
            boolean blank = value == null || value.isBlank();
            String text = CocLang.tr(key) + "：" + (blank ? CocLang.tr("（未填写）") : value);
            y = drawWrapped(graphics, text, leftX + 8, y, leftW - 16, blank ? CocUi.MUTED : CocUi.TEXT) + 2;
        }
        String notes = sheet.notes == null || sheet.notes.isBlank()
                ? CocLang.tr("（未填写）") : sheet.notes;
        y = drawWrapped(graphics, CocLang.tr("备注：") + notes, leftX + 8, y, leftW - 16, CocUi.TEXT) + 2;

        sheetMaxScroll = Math.max(0, (y + sheetScroll) - (leftY + leftH - 28));
        sheetScroll = Math.max(0, Math.min(sheetMaxScroll, sheetScroll));
        graphics.disableScissor();

        if (sheetMaxScroll > 0) {
            int trackX = leftX + leftW - 4;
            int viewTop = leftY + 2;
            int viewH = leftH - 4;
            graphics.fill(trackX, viewTop, trackX + 2, viewTop + viewH, 0x66000000);
            int thumbH = Math.max(12, viewH * viewH / (viewH + sheetMaxScroll));
            int thumbY = viewTop + (viewH - thumbH) * sheetScroll / sheetMaxScroll;
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, 0xFF8B7AA8);
            String hint = CocLang.tr("滚轮查看背景/备注");
            graphics.fill(leftX + 2, leftY + leftH - 11, leftX + leftW - 6, leftY + leftH - 2, 0xCC1A1410);
            graphics.drawString(font, CocUi.truncate(font, hint, leftW - 16),
                    leftX + 8, leftY + leftH - 10, CocUi.MUTED, false);
        }
    }

    private int drawWrapped(GuiGraphics graphics, String text, int x, int y, int width, int color) {
        if (text == null || text.isEmpty()) return y;
        for (String paragraph : text.replace("\r", "").split("\n", -1)) {
            if (paragraph.isEmpty()) {
                y += 9;
                continue;
            }
            for (var line : font.split(Component.literal(paragraph), width)) {
                graphics.drawString(font, line, x, y, color, false);
                y += 9;
            }
        }
        return y;
    }

    private void drawBar(GuiGraphics graphics, int x, int y, int width, String label,
                         int value, int max, int color) {
        graphics.drawString(font, label, x, y, CocUi.MUTED, false);
        int barX = x + 24;
        int barW = width - 24;
        int barH = compact ? 9 : 11;
        CocUi.bar(graphics, barX, y - 1, barW, barH, max <= 0 ? 0 : (double) value / max, color);
        String text = value + "/" + max;
        graphics.drawString(font, text, barX + barW - font.width(text) - 2, y, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (skillList != null && skillList.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (sheet != null
                && mouseX >= sheetPanelX && mouseX < sheetPanelX + sheetPanelW
                && mouseY >= sheetPanelY && mouseY < sheetPanelY + sheetPanelH) {
            sheetScroll = Math.max(0, Math.min(sheetMaxScroll,
                    sheetScroll - (int) (delta * 18)));
            return true;
        }
        if (skillList != null && skillList.mouseScrolled(mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record SkillRow(SkillDef def, int value, boolean marked) {
    }
}
