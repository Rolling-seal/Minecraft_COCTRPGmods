package com.coc.client.screen;

import com.coc.client.ClientCache;
import com.coc.client.CocLang;
import com.coc.client.KpView;
import com.coc.client.ui.CocList;
import com.coc.client.ui.CocUi;
import com.coc.net.CocNetwork;
import com.coc.net.packet.C2SKpAction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 从剧情书点击线索后：选择一名在线玩家发送。 */
public class ClueTargetScreen extends Screen {
    private final Screen parent;
    private final String clue;
    private final String label;
    private final List<FormattedCharSequence> preview = new ArrayList<>();
    private CocList<KpView.Player> playerList;
    private String status = "";
    private boolean compact;
    private boolean requested;

    public ClueTargetScreen(Screen parent, String clue, String label) {
        super(Component.literal(CocLang.tr("发送线索")));
        this.parent = parent;
        this.clue = clue == null ? "" : clue;
        this.label = label == null ? "" : label;
    }

    @Override
    protected void init() {
        compact = height < 420 || width < 700;
        int panelW = Math.min(width - 20, 400);
        int panelH = Math.min(height - 16, 224);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        int textW = panelW - 24;
        preview.clear();
        for (var line : font.split(Component.literal(clue.replace("\n", " ")), textW)) {
            if (preview.size() >= 3) break;
            preview.add(line);
        }

        playerList = new CocList<>(x + 10, y + 84, panelW - 20, 76, 20,
                (g, font, p, rx, ry, rw, rh, hovered, selected) -> {
                    g.drawString(font, CocUi.truncate(font, p.name, rw - 70), rx, ry + 2,
                            selected ? CocUi.GOLD : CocUi.TEXT, false);
                    String san = "SAN " + p.san + "/" + p.maxSan;
                    g.drawString(font, san, rx + rw - font.width(san), ry + 2, CocUi.MUTED, false);
                });
        KpView view = ClientCache.kpView;
        if (view != null) {
            List<KpView.Player> players = new ArrayList<>();
            for (KpView.Player p : view.players) {
                if (p != null && p.hasSheet) players.add(p);
            }
            playerList.setItems(players);
            if (!players.isEmpty()) playerList.setSelectedIndex(0);
        } else {
            playerList.setItems(List.of());
        }
        if (!requested) {
            requested = true;
            CocNetwork.sendToServer(new C2SKpAction("data", null, "", 0));
        }
        addBtn(CocLang.tr("发送给选中"), x + 10, y + panelH - 24, 118, 18, this::send);
        addBtn(CocLang.tr("返回"), x + panelW - 90, y + panelH - 24, 80, 18, this::onClose);
    }

    private void send() {
        KpView.Player target = playerList == null ? null : playerList.getSelected();
        if (target == null) {
            status = CocLang.tr("请先选择一名有角色卡的在线玩家。");
            return;
        }
        CocNetwork.sendToServer(new C2SKpAction("handout", target.uuid, clue, 0));
        status = CocLang.tr("已发送给 ") + target.name + CocLang.tr("，线索进入其日志。");
    }

    public void refresh() {
        UUID selected = playerList == null ? null : playerList.getSelected() == null
                ? null : playerList.getSelected().uuid;
        if (minecraft != null) rebuildWidgets();
        if (selected != null && playerList != null) {
            for (int i = 0; i < playerList.items().size(); i++) {
                if (selected.equals(playerList.items().get(i).uuid)) {
                    playerList.setSelectedIndex(i);
                    break;
                }
            }
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
        int panelW = Math.min(width - 20, 400);
        int panelH = Math.min(height - 16, 224);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        CocUi.panel(graphics, x, y, panelW, panelH);
        CocUi.title(graphics, font, CocLang.tr("发送线索"), width / 2, y + 6);
        String kind = label == null || label.isBlank() ? CocLang.tr("线索") : CocLang.tr(label);
        graphics.drawString(font, kind, x + 12, y + 26, CocUi.GOLD, false);
        int py = y + 40;
        for (FormattedCharSequence line : preview) {
            graphics.drawString(font, line, x + 12, py, CocUi.MUTED, false);
            py += 9;
        }
        graphics.drawString(font, CocLang.tr("在线玩家（有角色卡）"), x + 12, y + 72, CocUi.ACCENT, false);
        if (playerList != null) playerList.render(graphics, font, mouseX, mouseY);
        if (!status.isEmpty()) {
            graphics.drawString(font, CocUi.truncate(font, status, panelW - 24),
                    x + 12, y + panelH - 42, CocUi.GREEN, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (playerList != null && playerList.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (playerList != null && playerList.mouseScrolled(mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
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
