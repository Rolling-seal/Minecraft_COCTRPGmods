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
        int btnW = Math.max(44, Math.min(84, (width - 40) / 5 - 4));
        int gap = 4;
        int startX = Math.max(8, (width - (btnW * 5 + gap * 4)) / 2);
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

        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("背景")), b -> {
            if (minecraft != null) minecraft.setScreen(new BackstoryScreen(this, sheet));
        }).bounds(startX, btnY, btnW, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("线索")), b -> {
            if (minecraft != null) minecraft.setScreen(new JournalScreen());
        }).bounds(startX + btnW + gap, btnY, btnW, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("设置")), b -> {
            if (minecraft != null) minecraft.setScreen(new SettingsScreen(this));
        }).bounds(startX + (btnW + gap) * 2, btnY, btnW, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("刷新")), b -> {
            com.coc.net.CocNetwork.sendToServer(new com.coc.net.packet.C2SRequestSync());
            rebuildWidgets();
        }).bounds(startX + (btnW + gap) * 3, btnY, btnW, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("关闭")), b -> onClose())
                .bounds(startX + (btnW + gap) * 4, btnY, btnW, 18).build());
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
        int leftX = 12;
        int leftY = compact ? 24 : 40;
        int leftW = Math.max(150, width / 2 - 22);
        int panelBottom = height - 34;
        int leftH = Math.max(110, panelBottom - leftY);
        CocUi.panel(graphics, leftX, leftY, leftW, leftH);

        graphics.drawString(font, CocUi.truncate(font, sheet.name, leftW - 16), leftX + 8, leftY + 6, CocUi.GOLD, false);
        graphics.drawString(font, CocUi.truncate(font, CocLang.tr(sheet.occupationName) + " · " + sheet.age + CocLang.tr(" 岁"),
                leftW - 16), leftX + 8, leftY + (compact ? 18 : 22), CocUi.MUTED, false);

        Stat[] stats = {Stat.STR, Stat.CON, Stat.SIZ, Stat.DEX, Stat.APP, Stat.INT, Stat.POW, Stat.EDU, Stat.LUCK};
        int statsY = leftY + (compact ? 32 : 42);
        int cellW = Math.max(45, (leftW - 16) / 3);
        int lineH = compact ? 13 : 18;
        for (int i = 0; i < stats.length; i++) {
            int col = i % 3;
            int row = i / 3;
            int x = leftX + 8 + col * cellW;
            int y = statsY + row * lineH;
            Stat stat = stats[i];
            String label = compact ? stat.abbr() : stat.abbr() + " " + CocLang.tr(stat.cn());
            graphics.drawString(font, label, x, y, CocUi.ACCENT, false);
            String value = String.valueOf(sheet.stats.get(stat));
            graphics.drawString(font, value, x + (compact ? 40 : 56), y, CocUi.TEXT, false);
        }

        int barY = statsY + 3 * lineH + (compact ? 6 : 12);
        int barGap = compact ? 15 : 24;
        int barW = leftW - 40;
        drawBar(graphics, leftX + 8, barY, barW, "SAN", sheet.san, sheet.maxSan,
                CocUi.sanColor(sheet.san, sheet.maxSan));
        drawBar(graphics, leftX + 8, barY + barGap, barW, "HP", sheet.hp, sheet.maxHp, CocUi.RED);
        drawBar(graphics, leftX + 8, barY + barGap * 2, barW, "MP", sheet.mp, sheet.maxMp, CocUi.BLUE);
        drawBar(graphics, leftX + 8, barY + barGap * 3, barW, CocLang.tr("幸运"), sheet.stats.luck, 99, CocUi.GREEN);

        int infoY = barY + barGap * 4 + 4;
        graphics.drawString(font, CocLang.tr("移动 ") + sheet.mov + CocLang.tr("  伤害 ") + sheet.db + CocLang.tr("  体格 ") + sheet.build,
                leftX + 8, infoY, CocUi.TEXT, false);
        graphics.drawString(font, CocLang.tr("神话 ") + sheet.mythos + CocLang.tr("  最大 SAN ") + sheet.maxSan,
                leftX + 8, infoY + 11, sheet.mythos > 0 ? CocUi.GOLD : CocUi.MUTED, false);
        long gameTime = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;
        CocUi.wrapped(graphics, font, CocLang.tr("状态：") + CocLang.message(sheet.statusLine(gameTime)),
                leftX + 8, infoY + 23, leftW - 16, sheet.isInsane(gameTime) ? CocUi.RED : CocUi.GREEN);
        int phobiaY = infoY + 36;
        if (!sheet.phobias.isEmpty() || !sheet.manias.isEmpty()) {
            String phobias = String.join(" / ", sheet.phobias.stream().map(CocLang::tr).toList());
            String manias = String.join(" / ", sheet.manias.stream().map(CocLang::tr).toList());
            String mental = CocLang.tr("恐惧：") + phobias
                    + (sheet.manias.isEmpty() ? "" : CocLang.tr("  狂躁：") + manias);
            CocUi.wrapped(graphics, font, mental, leftX + 8, phobiaY, leftW - 16, CocUi.MUTED);
        }
        if (!compact && !sheet.backstory.isEmpty()) {
            int by = phobiaY + 22;
            for (String key : CharacterSheet.BACKSTORY_KEYS) {
                String value = sheet.backstory.getOrDefault(key, "");
                if (value == null || value.isBlank()) continue;
                if (by > leftY + leftH - 12) break;
                graphics.drawString(font, key + "：" + CocUi.truncate(font, value, leftW - 70),
                        leftX + 8, by, CocUi.MUTED, false);
                by += 12;
            }
        }
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
