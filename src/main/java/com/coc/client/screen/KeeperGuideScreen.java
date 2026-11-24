package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.ui.CocList;
import com.coc.client.ui.CocUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** KP之书：游戏内操作指南。 */
public class KeeperGuideScreen extends Screen {
    private final List<Page> pages = new ArrayList<>();
    private int selected;
    private CocList<Page> pageList;
    private CocList<Line> lineList;
    private Button settingsButton;
    private boolean compact;

    public KeeperGuideScreen() {
        super(Component.literal(CocLang.tr("KP之书")));
        pages.add(page("开局", "Before the Session",
                "玩家先建卡；纯 KP 不需要建卡。",
                "按 K 打开 KP 面板。",
                "剧情书 → 选择剧本 → 开始剧本。",
                "玩家只会收到幕布介绍；场景与选项由你口述。"));
        pages.add(page("掷骰", "Rolling",
                "只有 KP 能掷骰。",
                "玩家页选中玩家 → 掷骰页选技能/难度/奖惩骰。",
                "点“为选中玩家掷骰”，结果对所有玩家公开。",
                "玩家不能自己按 R 或使用旧掷骰命令。"));
        pages.add(page("幕布", "Curtains",
                "开始剧本、换幕、显示此幕，只发标题与介绍。",
                "玩家界面没有选项按钮。",
                "不要连续点幕；等玩家行动和讨论。",
                "失败也给代价、线索或更糟的选择，不要卡关。"));
        pages.add(page("剧情书", "Story Book",
                "主线：每幕概要、触发、场景、可能走向与线索。",
                "支线：触发条件、所属幕、描述、选项与后果。",
                "支线由你口述，不自动发给玩家。",
                "开始剧本后你会获得《剧本之书》，右键快速翻幕。",
                "“向玩家显示此幕”只发幕布，不发选项。"));
        pages.add(page("道具", "Items",
                "道具页可发放常见或自定义物品。",
                "用物品 ID 加入道具库，并配置名称、技能、目标点数。",
                "使用道具会取走 1 件并做公开检定。",
                "没有规则的物品使用页面临时目标。"));
        pages.add(page("角色卡", "Sheets",
                "玩家页可调整 SAN/HP/MP/幸运、恢复神志、成长。",
                "“状态调整/燃运”可自由设置数值。",
                "角色卡 JSON 可导出/导入。",
                "导入会覆盖选中玩家的角色卡；谨慎操作。"));
        pages.add(page("导入", "Import",
                "剧本：J → 导入剧本，或剧情书 → 导入剧本；选择 .json。",
                "角色卡：玩家页 → 角色卡 JSON → 导入。",
                "剧本只接受 coc-scenario-v1；角色卡只接受 coc-character-v1。",
                "导入前先备份重要角色卡。"));
        pages.add(page("全局设置", "Global Settings",
                "全局设置物品可调整新角色的职业点、兴趣点、总点数。",
                "-1 表示自动公式：职业点按职业，兴趣点为 INT×2。",
                "关闭“允许玩家自行分配点数”后，新角色只保留基础技能。",
                "还可设置初始幸运、初始 HP、初始 SAN 的随机范围。",
                "设置只影响新角色，不修改已有角色卡。"));
    }

    private Page page(String cn, String en, String... lines) {
        return new Page(cn, en, List.of(lines));
    }

    @Override
    protected void init() {
        compact = height < 420 || width < 700;
        int top = compact ? 38 : 70;
        int bottom = height - 24;
        int leftW = Math.max(100, width / 3 - 4);
        int listH = Math.max(70, bottom - top - 4);
        pageList = new CocList<>(12, top, leftW, listH, compact ? 20 : 22,
                (g, font, page, x, y, w, h, hovered, selected) -> {
                    g.drawString(font, CocUi.truncate(font, CocLang.tr(page.cn), w - 4), x, y + 4,
                            selected ? CocUi.GOLD : CocUi.TEXT, false);
                });
        pageList.setItems(pages);
        selected = Math.max(0, Math.min(selected, pages.size() - 1));
        pageList.setSelectedIndex(selected);
        int rightX = leftW + 20;
        int rightW = Math.max(140, width - rightX - 12);
        lineList = new CocList<>(rightX, top, rightW, listH, 10,
                (g, font, line, x, y, w, h, hovered, selected) ->
                        g.drawString(font, line.text, x, y, line.color, false));
        rebuildDetail();
        settingsButton = addBtn(CocLang.tr("打开全局设置"), 12, bottom, 132, 18,
                () -> {
                    if (minecraft != null) minecraft.setScreen(new GlobalSettingsScreen());
                });
        settingsButton.visible = selected == 7;
        settingsButton.active = selected == 7;
        addBtn(CocLang.tr("关闭"), width - 80, bottom, 68, 18, this::onClose);
    }

    private void rebuildDetail() {
        if (lineList == null || pages.isEmpty()) return;
        Page page = pages.get(selected);
        List<Line> lines = new ArrayList<>();
        int width = Math.max(100, lineList.width() - 10);
        String cn = CocLang.tr(page.cn());
        addWrapped(lines, cn, CocUi.GOLD, width);
        // English mode already shows the translated title above; do not add a second title line.
        lines.add(new Line("", CocUi.TEXT));
        for (String text : page.lines) {
            addWrapped(lines, "• " + CocLang.tr(text), CocUi.TEXT, width);
            lines.add(new Line("", CocUi.TEXT));
        }
        lineList.setItems(lines);
        lineList.setScroll(0);
        if (settingsButton != null) {
            settingsButton.visible = selected == 7;
            settingsButton.active = selected == 7;
        }
    }

    private void addWrapped(List<Line> lines, String text, int color, int width) {
        if (text == null || text.isBlank()) return;
        if (CocLang.english()) {
            String line = "";
            for (String word : text.split(" ")) {
                String next = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && font.width(next) > width) {
                    lines.add(new Line(line, color));
                    line = word;
                } else {
                    line = next;
                }
            }
            if (!line.isEmpty()) lines.add(new Line(line, color));
            return;
        }
        StringBuilder line = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (font.width(line.toString() + c) > width && line.length() > 0) {
                lines.add(new Line(line.toString(), color));
                line.setLength(0);
            }
            line.append(c);
        }
        if (line.length() > 0) lines.add(new Line(line.toString(), color));
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
        CocUi.title(graphics, font, CocLang.tr("KP之书"), width / 2, compact ? 2 : 4);
        if (!compact) {
            graphics.drawString(font, CocLang.tr("给新手 KP 的操作顺序；详细文档见开发文档。"),
                    12, 42, CocUi.MUTED, false);
        }
        if (pageList != null) pageList.render(graphics, font, mouseX, mouseY);
        if (lineList != null) lineList.render(graphics, font, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (pageList != null && pageList.mouseClicked(mouseX, mouseY, button)) {
            selected = pageList.getSelectedIndex();
            rebuildDetail();
            return true;
        }
        if (lineList != null && lineList.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (lineList != null && lineList.mouseScrolled(mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Page(String cn, String en, List<String> lines) {
    }

    private record Line(String text, int color) {
    }
}
