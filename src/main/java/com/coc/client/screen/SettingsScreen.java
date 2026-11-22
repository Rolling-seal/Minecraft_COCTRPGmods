package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.CocClientSettings;
import com.coc.client.ui.CocUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 克苏鲁设置：低 SAN 特效总开关与各项开关，即时保存。响应小窗口。 */
public class SettingsScreen extends Screen {
    private final Screen parent;
    private Button master;
    private Button distortion;
    private Button shake;
    private Button vignette;
    private Button whisper;
    private Button hud;
    private Button overlay;

    public SettingsScreen(Screen parent) {
        super(Component.literal(CocLang.tr("克苏鲁设置")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        boolean compact = height < 420 || width < 700;
        int w = Math.min(320, width - 20);
        int x = width / 2 - w / 2;
        int y = compact ? 30 : 66;
        int step = compact ? 22 : 24;
        int h = compact ? 18 : 20;
        CocClientSettings s = CocClientSettings.get();

        master = addRenderableWidget(Button.builder(label(CocLang.tr("低 SAN 特效"), s.sanityEffects), b -> {
            s.sanityEffects = !s.sanityEffects;
            CocClientSettings.save();
            refreshLabels();
        }).bounds(x, y, w, h).build());
        distortion = addRenderableWidget(Button.builder(Component.literal(CocLang.tr("扭曲强度：") + s.distortionLabel()), b -> {
            s.distortionLevel = (s.distortionLevel + 1) % 3;
            CocClientSettings.save();
            refreshLabels();
        }).bounds(x, y + step, w, h).build());
        shake = addRenderableWidget(Button.builder(label(CocLang.tr("镜头晃动"), s.cameraShake), b -> {
            s.cameraShake = !s.cameraShake;
            CocClientSettings.save();
            refreshLabels();
        }).bounds(x, y + step * 2, w, h).build());
        vignette = addRenderableWidget(Button.builder(label(CocLang.tr("边缘暗角"), s.vignette), b -> {
            s.vignette = !s.vignette;
            CocClientSettings.save();
            refreshLabels();
        }).bounds(x, y + step * 3, w, h).build());
        whisper = addRenderableWidget(Button.builder(label(CocLang.tr("低语与心跳"), s.whispers), b -> {
            s.whispers = !s.whispers;
            CocClientSettings.save();
            refreshLabels();
        }).bounds(x, y + step * 4, w, h).build());
        hud = addRenderableWidget(Button.builder(label(CocLang.tr("SAN/HP 状态条"), s.hud), b -> {
            s.hud = !s.hud;
            CocClientSettings.save();
            refreshLabels();
        }).bounds(x, y + step * 5, w, h).build());
        overlay = addRenderableWidget(Button.builder(label(CocLang.tr("检定结果浮层"), s.diceOverlay), b -> {
            s.diceOverlay = !s.diceOverlay;
            CocClientSettings.save();
            refreshLabels();
        }).bounds(x, y + step * 6, w, h).build());
        addRenderableWidget(Button.builder(Component.literal(CocLang.tr("返回")), b -> onClose())
                .bounds(x, y + step * 7, w, h).build());
    }

    private Component label(String name, boolean enabled) {
        return Component.literal(name + "：" + (enabled ? CocLang.tr("开") : CocLang.tr("关")));
    }

    private void refreshLabels() {
        CocClientSettings s = CocClientSettings.get();
        master.setMessage(label(CocLang.tr("低 SAN 特效"), s.sanityEffects));
        distortion.setMessage(Component.literal(CocLang.tr("扭曲强度：") + s.distortionLabel()));
        shake.setMessage(label(CocLang.tr("镜头晃动"), s.cameraShake));
        vignette.setMessage(label(CocLang.tr("边缘暗角"), s.vignette));
        whisper.setMessage(label(CocLang.tr("低语与心跳"), s.whispers));
        hud.setMessage(label(CocLang.tr("SAN/HP 状态条"), s.hud));
        overlay.setMessage(label(CocLang.tr("检定结果浮层"), s.diceOverlay));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean compact = height < 420 || width < 700;
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xC0000000);
        int w = Math.min(320, width - 20);
        int x = width / 2 - w / 2;
        int panelY = compact ? 6 : 30;
        int panelH = compact ? height - 12 : 330;
        CocUi.panel(graphics, x, panelY, w, panelH);
        CocUi.title(graphics, font, CocLang.tr("克苏鲁的呼唤 · 设置"), width / 2, panelY + (compact ? 8 : 10));
        if (compact) {
            graphics.drawString(font, CocLang.tr("关闭特效会立即恢复原版画面"), x + 6, height - 16, CocUi.MUTED, false);
        } else {
            graphics.drawString(font, CocLang.tr("关闭低 SAN 特效会立即卸下本模组的后处理，"), x + 10, 58, CocUi.MUTED, false);
            graphics.drawString(font, CocLang.tr("不会影响原版或其它模组的滤镜。"), x + 10, 68, CocUi.MUTED, false);
            graphics.drawString(font, CocLang.tr("设置保存在 config/coc-client.json"), x + 10, 320, CocUi.MUTED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (parent != null && minecraft != null) {
            minecraft.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
