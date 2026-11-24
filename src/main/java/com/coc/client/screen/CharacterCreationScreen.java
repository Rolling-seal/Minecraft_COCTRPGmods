package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.ClientCache;
import com.coc.client.GlobalSettingsView;
import com.coc.client.ui.CocList;
import com.coc.client.ui.CocUi;
import com.coc.data.CharacterSheet;
import com.coc.net.CocNetwork;
import com.coc.net.packet.C2SCreateCharacter;
import com.coc.rules.CharacterRules;
import com.coc.rules.Occupation;
import com.coc.rules.OccupationRegistry;
import com.coc.rules.SkillDef;
import com.coc.rules.SkillRegistry;
import com.coc.rules.Stat;
import com.coc.rules.Stats;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 角色创建：属性/职业 -> 技能点分配 -> 背景。响应 427×240 起的 GUI 尺寸。 */
public class CharacterCreationScreen extends Screen {
    private int page;
    private int age = 25;
    private Stats baseStats;
    private Stats preview;
    private Occupation occupation;
    private final Map<String, Integer> occAlloc = new LinkedHashMap<>();
    private final Map<String, Integer> intAlloc = new LinkedHashMap<>();
    private CocList<Occupation> occList;
    private CocList<SkillAlloc> skillList;
    private final EditBox[] backstoryBoxes = new EditBox[6];
    private final String[] backstoryValues = new String[6];
    private EditBox notesBox;
    private String notesValue = "";
    private String status = "";
    private boolean compact;

    public CharacterCreationScreen() {
        super(Component.literal(CocLang.tr("创建调查员")));
    }

    @Override
    protected void init() {
        compact = height < 420 || width < 700;
        if (baseStats == null) rollStats();
        updatePreview();
        if (page == 1 && !allowPoints()) page = 2;
        occList = null;
        skillList = null;
        if (page == 0) {
            initStatsPage();
        } else if (page == 1) {
            initSkillPage();
        } else {
            initBackstoryPage();
        }
    }

    private void initStatsPage() {
        int listX = 12;
        int listY = compact ? 96 : 136;
        int listW = Math.max(140, width / 2 - 20);
        int listH = Math.max(48, height - listY - (compact ? 32 : 70));
        List<Occupation> occupations = OccupationRegistry.all();
        occList = new CocList<>(listX, listY, listW, listH, compact ? 15 : 18,
                (g, font, occ, x, y, w, h, hovered, selected) -> {
                    g.drawString(font, CocLang.tr(occ.name), x, y + 2, selected ? CocUi.GOLD : CocUi.TEXT, false);
                    g.drawString(font, occ.formulaText(), x + w - font.width(occ.formulaText()) - 2,
                            y + 2, CocUi.MUTED, false);
                });
        occList.setItems(occupations);
        if (occupation != null) {
            for (int i = 0; i < occupations.size(); i++) {
                if (occupations.get(i).id.equals(occupation.id)) {
                    occList.setSelectedIndex(i);
                    break;
                }
            }
        }

        int by = height - 24;
        addRenderableWidget(Button.builder(Component.literal("−"), b -> changeAge(-5))
                .bounds(12, by, 22, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> changeAge(5))
                .bounds(38, by, 22, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("重掷")), b -> {
            rollStats();
            resetAlloc();
            status = "";
            rebuildWidgets();
        }).bounds(64, by, 54, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("取消")), b -> onClose())
                .bounds(width - 172, by, 54, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr(
                allowPoints() ? "下一步：技能" : "下一步：背景")), b -> {
            if (occupation == null) {
                status = CocLang.tr("请先选择一个职业。");
                return;
            }
            page = allowPoints() ? 1 : 2;
            rebuildWidgets();
        }).bounds(width - 114, by, 102, 18).build());
    }

    private void initSkillPage() {
        int listY = compact ? 50 : 86;
        int listW = width - 24;
        int listH = Math.max(60, height - listY - (compact ? 32 : 76));
        skillList = new CocList<>(12, listY, listW, listH, compact ? 15 : 17,
                (g, font, row, x, y, w, h, hovered, selected) -> {
                    int color = selected ? CocUi.GOLD
                            : (row.occ > 0 ? CocUi.GOLD : (row.interest > 0 ? CocUi.ACCENT : CocUi.TEXT));
                    g.drawString(font, CocUi.truncate(font, CocLang.tr(row.def.name), 160), x, y + 2, color, false);
                    g.drawString(font, CocLang.tr("基础 ") + row.base, x + 168, y + 2, CocUi.MUTED, false);
                    if (occupation != null && occupation.canSpendOccupation(row.def.id)) {
                        g.drawString(font, CocLang.tr("职业"), x + 238, y + 2, CocUi.GOLD, false);
                    }
                    String total = String.valueOf(row.base + row.occ + row.interest);
                    g.drawString(font, total, x + w - font.width(total) - 4, y + 2, color, false);
                });
        refreshSkillList();

        int by = height - 24;
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("+5 职业")), b -> addPoints(true))
                .bounds(12, by, 64, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("+5 兴趣")), b -> addPoints(false))
                .bounds(80, by, 64, 18).build());
        addRenderableWidget(Button.builder(Component.literal("−5"), b -> removePoints())
                .bounds(148, by, 36, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("上一步")), b -> {
            page = 0;
            rebuildWidgets();
        }).bounds(width - 204, by, 48, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("下一步：背景")), b -> {
            page = 2;
            rebuildWidgets();
        }).bounds(width - 152, by, 102, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("返回")), b -> onClose())
                .bounds(width - 46, by, 34, 18).build());
    }

    private void initBackstoryPage() {
        int y = compact ? 44 : 86;
        int step = compact ? 30 : 54;
        int colW = Math.max(110, (width - 44) / 2);
        for (int i = 0; i < 6; i++) {
            int col = i % 2;
            int row = i / 2;
            EditBox box = new EditBox(font, 12 + col * (colW + 16), y + row * step, colW, 18,
                    Component.literal(CocLang.tr(CharacterSheet.BACKSTORY_KEYS[i])));
            box.setMaxLength(160);
            if (backstoryValues[i] != null) box.setValue(backstoryValues[i]);
            backstoryBoxes[i] = box;
            addRenderableWidget(box);
        }
        int notesY = y + 3 * step;
        notesBox = new EditBox(font, 12, notesY, width - 24, 18, Component.literal(CocLang.tr("备注")));
        notesBox.setMaxLength(300);
        notesBox.setValue(notesValue);
        addRenderableWidget(notesBox);

        int by = height - 24;
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("上一步")), b -> {
            saveBackstory();
            page = 1;
            rebuildWidgets();
        }).bounds(width - 190, by, 48, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("确认创建")), b -> confirmCreate())
                .bounds(width - 138, by, 86, 18).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("返回")), b -> onClose())
                .bounds(width - 48, by, 36, 18).build());
    }

    private void changeAge(int delta) {
        age = Math.max(15, Math.min(89, age + delta));
        updatePreview();
        resetAlloc();
        status = "";
        rebuildWidgets();
    }

    private void rollStats() {
        baseStats = CharacterRules.rollBaseStats(RandomSource.create());
        age = 25;
        updatePreview();
        resetAlloc();
    }

    private void updatePreview() {
        if (baseStats == null) return;
        preview = CharacterRules.applyAge(baseStats, age, RandomSource.create(), false);
    }

    private void resetAlloc() {
        occAlloc.clear();
        intAlloc.clear();
    }

    private int occSpent() {
        return occAlloc.values().stream().mapToInt(Integer::intValue).sum();
    }

    private int intSpent() {
        return intAlloc.values().stream().mapToInt(Integer::intValue).sum();
    }

    private GlobalSettingsView settings() {
        return ClientCache.globalSettings == null ? new GlobalSettingsView() : ClientCache.globalSettings;
    }

    private boolean allowPoints() {
        return settings().allowPoints;
    }

    private int occPoints() {
        if (!allowPoints()) return 0;
        GlobalSettingsView s = settings();
        if (s.occupationPoints >= 0) return s.occupationPoints;
        return occupation == null || preview == null ? 0 : occupation.points(preview);
    }

    private int intPoints() {
        if (!allowPoints()) return 0;
        GlobalSettingsView s = settings();
        if (s.interestPoints >= 0) return s.interestPoints;
        return preview == null ? 0 : preview.intel * 2;
    }

    private int totalPoints() {
        GlobalSettingsView s = settings();
        if (s.totalPoints >= 0) return s.totalPoints;
        return occPoints() + intPoints();
    }

    private void addPoints(boolean useOccupation) {
        if (skillList == null) return;
        if (!allowPoints()) {
            status = CocLang.tr("KP 已关闭玩家自行分配点数。");
            return;
        }
        SkillAlloc row = skillList.getSelected();
        if (row == null) {
            status = CocLang.tr("请先在列表中选择一个技能。");
            return;
        }
        int base = row.base;
        int current = base + occAlloc.getOrDefault(row.def.id, 0) + intAlloc.getOrDefault(row.def.id, 0);
        int max = base >= 70 ? 99 : 70;
        if (current + 5 > max) {
            status = CocLang.tr("该技能创建时不能超过 ") + max + "。";
            return;
        }
        if (occSpent() + intSpent() + 5 > totalPoints()) {
            status = CocLang.tr("总点数不足。");
            return;
        }
        if (useOccupation) {
            if (occupation == null || !occupation.canSpendOccupation(row.def.id)) {
                status = CocLang.tr("该技能不在你的职业技能列表内。");
                return;
            }
            if (occSpent() + 5 > occPoints()) {
                status = CocLang.tr("职业点不足。");
                return;
            }
            occAlloc.merge(row.def.id, 5, Integer::sum);
        } else {
            if (intSpent() + 5 > intPoints()) {
                status = CocLang.tr("兴趣点不足。");
                return;
            }
            intAlloc.merge(row.def.id, 5, Integer::sum);
        }
        status = "";
        refreshSkillList();
    }

    private void removePoints() {
        if (skillList == null) return;
        SkillAlloc row = skillList.getSelected();
        if (row == null) return;
        int occ = occAlloc.getOrDefault(row.def.id, 0);
        int interest = intAlloc.getOrDefault(row.def.id, 0);
        if (occ >= 5) occAlloc.put(row.def.id, occ - 5);
        else if (interest >= 5) intAlloc.put(row.def.id, interest - 5);
        else if (occ > 0) occAlloc.remove(row.def.id);
        else if (interest > 0) intAlloc.remove(row.def.id);
        refreshSkillList();
    }

    private void refreshSkillList() {
        if (skillList == null || preview == null) return;
        List<SkillAlloc> rows = new ArrayList<>();
        for (SkillDef def : SkillRegistry.all()) {
            int base = def.baseValue(preview.dex, preview.edu);
            rows.add(new SkillAlloc(def, base,
                    occAlloc.getOrDefault(def.id, 0), intAlloc.getOrDefault(def.id, 0)));
        }
        skillList.setItemsKeepState(rows);
    }

    private void saveBackstory() {
        for (int i = 0; i < 6; i++) {
            if (backstoryBoxes[i] != null) backstoryValues[i] = backstoryBoxes[i].getValue();
        }
        if (notesBox != null) notesValue = notesBox.getValue();
    }

    private void confirmCreate() {
        if (occupation == null || baseStats == null) {
            status = CocLang.tr("数据不完整。");
            return;
        }
        saveBackstory();
        if (!allowPoints()) {
            occAlloc.clear();
            intAlloc.clear();
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt("age", age);
        tag.putString("occupation", occupation.id);
        tag.put("stats", baseStats.toNbt());
        CompoundTag skills = new CompoundTag();
        for (String id : occAlloc.keySet()) {
            skills.putInt(id, occAlloc.getOrDefault(id, 0) + intAlloc.getOrDefault(id, 0));
        }
        for (String id : intAlloc.keySet()) {
            if (!occAlloc.containsKey(id)) skills.putInt(id, intAlloc.get(id));
        }
        tag.put("skills", skills);
        CompoundTag bg = new CompoundTag();
        for (int i = 0; i < 6; i++) {
            bg.putString(CharacterSheet.BACKSTORY_KEYS[i], backstoryValues[i] == null ? "" : backstoryValues[i]);
        }
        tag.put("backstory", bg);
        tag.putString("notes", notesValue == null ? "" : notesValue);
        CocNetwork.sendToServer(new C2SCreateCharacter(tag));
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0000000);
        switch (page) {
            case 0 -> renderStats(graphics, mouseX, mouseY);
            case 1 -> renderSkills(graphics, mouseX, mouseY);
            default -> renderBackstory(graphics);
        }
        if (!status.isEmpty()) {
            graphics.drawString(font, CocUi.truncate(font, status, width - 24), 12, height - 38, CocUi.GOLD, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderStats(GuiGraphics graphics, int mouseX, int mouseY) {
        CocUi.title(graphics, font, CocLang.tr("创建调查员 · 属性与职业"), width / 2, compact ? 8 : 18);
        int panelY = compact ? 22 : 36;
        int panelH = compact ? 64 : 90;
        CocUi.panel(graphics, 12, panelY, width - 24, panelH);
        graphics.drawString(font, CocLang.tr("年龄 ") + age, 20, panelY + 4, CocUi.MUTED, false);
        Stat[] stats = {Stat.STR, Stat.CON, Stat.SIZ, Stat.DEX, Stat.APP, Stat.INT, Stat.POW, Stat.EDU, Stat.LUCK};
        int cellW = Math.max(70, (width - 40) / 3);
        int lineH = compact ? 12 : 18;
        int statsY = panelY + (compact ? 18 : 24);
        for (int i = 0; i < stats.length; i++) {
            int col = i % 3;
            int row = i / 3;
            int x = 20 + col * cellW;
            int y = statsY + row * lineH;
            Stat stat = stats[i];
            String label = compact ? stat.abbr() : stat.abbr() + " " + CocLang.tr(stat.cn());
            graphics.drawString(font, label, x, y, CocUi.ACCENT, false);
            String value = String.valueOf(preview == null ? 0 : preview.get(stat));
            graphics.drawString(font, value, x + (compact ? 28 : 50), y, CocUi.TEXT, false);
        }
        if (!compact) {
            graphics.drawString(font, "HP " + CharacterRules.maxHp(preview) + "   MP "
                            + CharacterRules.maxMp(preview) + CocLang.tr("   移动 ") + CharacterRules.mov(preview, age),
                    20, panelY + 76, CocUi.MUTED, false);
        }

        graphics.drawString(font, CocLang.tr("选择职业"), 12, (compact ? 84 : 124), CocUi.ACCENT, false);
        if (occList != null) occList.render(graphics, font, mouseX, mouseY);

        int detailX = width / 2 + 4;
        int detailW = width - detailX - 12;
        int detailY = compact ? 96 : 136;
        int detailH = Math.max(48, height - detailY - (compact ? 32 : 70));
        CocUi.panelLight(graphics, detailX, detailY, detailW, detailH);
        if (occupation == null) {
            graphics.drawString(font, CocLang.tr("← 选择一个职业"), detailX + 8, detailY + 8, CocUi.MUTED, false);
        } else {
            graphics.drawString(font, CocLang.tr(occupation.name), detailX + 8, detailY + 6, CocUi.GOLD, false);
            graphics.drawString(font, occupation.formulaText(), detailX + 8, detailY + 20, CocUi.ACCENT, false);
            int y = detailY + 34;
            CocUi.wrapped(graphics, font, occupation.description, detailX + 8, y, detailW - 16, CocUi.TEXT);
            y += CocUi.wrapHeight(font, occupation.description, detailW - 16) + 4;
            for (String skillId : occupation.skills) {
                SkillDef def = SkillRegistry.get(skillId);
                if (def == null) continue;
                if (y > detailY + detailH - 11) break;
                graphics.drawString(font, "· " + CocLang.tr(def.name), detailX + 10, y, CocUi.TEXT, false);
                y += 11;
            }
        }
    }

    private void renderSkills(GuiGraphics graphics, int mouseX, int mouseY) {
        CocUi.title(graphics, font, CocLang.tr("分配技能点 · ") + (occupation == null ? "" : CocLang.tr(occupation.name)),
                width / 2, compact ? 8 : 18);
        String points = CocLang.tr("职业点 ") + occSpent() + " / " + occPoints()
                + CocLang.tr("    兴趣点 ") + intSpent() + " / " + intPoints();
        if (totalPoints() >= 0) points += CocLang.tr("    总点数 ") + (occSpent() + intSpent()) + " / " + totalPoints();
        graphics.drawString(font, points, 12, compact ? 24 : 42, CocUi.ACCENT, false);
        if (!compact) {
            graphics.drawString(font, CocLang.tr("选择技能后点击下方按钮；创建时技能上限 70（母语可更高）"),
                    12, 62, CocUi.MUTED, false);
        }
        if (skillList != null) skillList.render(graphics, font, mouseX, mouseY);
    }

    private void renderBackstory(GuiGraphics graphics) {
        CocUi.title(graphics, font, CocLang.tr("背景与备注"), width / 2, compact ? 8 : 18);
        if (!compact) {
            graphics.drawString(font, CocLang.tr("这些内容会写入角色卡；可以留空。"), 12, 42, CocUi.MUTED, false);
        }
        int colW = Math.max(110, (width - 44) / 2);
        int y = compact ? 44 : 86;
        int step = compact ? 30 : 54;
        for (int i = 0; i < 6; i++) {
            int col = i % 2;
            int row = i / 2;
            graphics.drawString(font, CharacterSheet.BACKSTORY_KEYS[i],
                    12 + col * (colW + 16), y + row * step - 9, CocUi.ACCENT, false);
        }
        graphics.drawString(font, CocLang.tr("备注"), 12, y + 3 * step - 9, CocUi.ACCENT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page == 0 && occList != null && occList.mouseClicked(mouseX, mouseY, button)) {
            Occupation selected = occList.getSelected();
            if (selected != null) {
                occupation = selected;
                resetAlloc();
                status = "";
                rebuildWidgets();
            }
            return true;
        }
        if (page == 1 && skillList != null && skillList.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (page == 0 && occList != null && occList.mouseScrolled(mouseX, mouseY, delta)) return true;
        if (page == 1 && skillList != null && skillList.mouseScrolled(mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record SkillAlloc(SkillDef def, int base, int occ, int interest) {
    }
}
