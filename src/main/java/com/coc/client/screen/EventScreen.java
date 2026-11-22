package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.ClientCache;
import com.coc.client.PendingEventView;
import com.coc.client.ui.CocUi;
import com.coc.net.CocNetwork;
import com.coc.net.packet.C2SEventChoice;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** 事件卡：叙事 + 选项 + 结果。 */
public class EventScreen extends Screen {
    private final PendingEventView event;
    private String resultText = "";
    private String resultEffects = "";
    private String resultCheck = "";
    private boolean closed;
    private boolean waiting;

    public EventScreen(PendingEventView event) {
        super(Component.literal(event == null ? CocLang.tr("事件") : event.title));
        this.event = event == null ? new PendingEventView() : event;
    }

    @Override
    protected void init() {
        int panelW = Math.min(460, width - 40);
        int panelX = width / 2 - panelW / 2;
        if (resultText.isEmpty() && !waiting) {
            int count = event.options.size();
            int buttonW = panelW - 40;
            int startY = height - 50 - Math.max(0, count) * 24;
            for (int i = 0; i < count; i++) {
                PendingEventView.Option option = event.options.get(i);
                String label = option.text;
                if (!option.skill.isEmpty()) {
                    label += "  [" + option.skill + " · " + option.difficulty + "]";
                }
                label = CocUi.truncate(font, label, buttonW - 10);
                int index = i;
                addRenderableWidget(Button.builder(Component.literal(label), b -> choose(index))
                        .bounds(panelX + 20, startY + i * 24, buttonW, 20).build());
            }
        }
        if (!resultText.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal(CocLang.tr("关闭")), b -> {
                if (closed) {
                    ClientCache.pendingEvent = null;
                }
                onClose();
            }).bounds(panelX + panelW - 90, height - 34, 70, 20).build());
        }
    }

    private void choose(int index) {
        if (waiting) return;
        waiting = true;
        CocNetwork.sendToServer(new C2SEventChoice(index));
        rebuildWidgets();
    }

    public void onResult(CompoundTag tag) {
        resultText = tag.getString("text");
        resultEffects = tag.getString("effects");
        resultCheck = tag.getString("check");
        closed = tag.getBoolean("closed");
        waiting = false;
        if (closed) ClientCache.pendingEvent = null;
        rebuildWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        if (resultText.isEmpty() && !waiting && event.remainingTicks > 0) {
            event.remainingTicks--;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xC0000000);
        int panelW = Math.min(460, width - 40);
        int panelH = Math.min(420, height - 40);
        int panelX = width / 2 - panelW / 2;
        int panelY = 30;
        CocUi.panel(graphics, panelX, panelY, panelW, panelH);
        graphics.drawCenteredString(font, CocLang.tr("事 件"), width / 2, panelY + 8, CocUi.MUTED);
        graphics.drawCenteredString(font, event.title, width / 2, panelY + 22, CocUi.GOLD);
        int textY = panelY + 42;
        CocUi.wrapped(graphics, font, event.description, panelX + 20, textY, panelW - 40, CocUi.TEXT);

        int optionsTop = height - 50 - event.options.size() * 24;
        if (resultText.isEmpty()) {
            if (waiting) {
                graphics.drawCenteredString(font, CocLang.tr("已做出选择，等待世界回应……"),
                        width / 2, optionsTop + 4, CocUi.MUTED);
            } else {
                int seconds = Math.max(0, event.remainingTicks / 20);
                graphics.drawCenteredString(font, CocLang.tr("剩余时间 ") + seconds + CocLang.tr(" 秒"),
                        width / 2, optionsTop - 14, CocUi.MUTED);
            }
        } else {
            int y = optionsTop - 8;
            CocUi.wrapped(graphics, font, resultText, panelX + 20, y, panelW - 40, CocUi.TEXT);
            y += CocUi.wrapHeight(font, resultText, panelW - 40) + 6;
            if (!resultCheck.isEmpty()) {
                CocUi.wrapped(graphics, font, CocLang.tr("检定：") + resultCheck, panelX + 20, y, panelW - 40, CocUi.ACCENT);
                y += CocUi.wrapHeight(font, resultCheck, panelW - 40) + 4;
            }
            if (!resultEffects.isEmpty()) {
                CocUi.wrapped(graphics, font, CocLang.tr("结果：") + resultEffects, panelX + 20, y, panelW - 40, CocUi.GOLD);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return closed || resultText.isEmpty() && event.options.isEmpty();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
