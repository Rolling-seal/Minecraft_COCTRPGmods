package com.coc.client.ui;

import com.coc.client.CocLang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** 界面绘制工具与“中世纪克苏鲁”主题：深色皮革、旧羊皮纸文字、氧化黄铜边框。 */
public final class CocUi {
    // 深色旧皮革 / 亮一档的皮革
    public static final ResourceLocation TEX_PANEL_DARK =
            new ResourceLocation("coc", "textures/gui/panel_dark.png");
    public static final ResourceLocation TEX_PANEL_LIGHT =
            new ResourceLocation("coc", "textures/gui/panel_light.png");
    public static final ResourceLocation TEX_ORNAMENT =
            new ResourceLocation("coc", "textures/gui/ornament.png");

    public static final int BG = 0xF00A0806;
    public static final int PANEL = 0xF01A1410;
    public static final int PANEL_LIGHT = 0xF0241B13;
    public static final int BORDER = 0xFF8A6A2F;
    public static final int TEXT = 0xFFE7D9B8;
    public static final int MUTED = 0xFFA9977A;
    public static final int ACCENT = 0xFFC9A227;
    public static final int GOLD = 0xFFE3C56B;
    public static final int RED = 0xFFC0392B;
    public static final int GREEN = 0xFF5E7D4F;
    public static final int BLUE = 0xFF4F6D7A;
    public static final int INK = 0xFF120D08;

    private CocUi() {
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 4 || height <= 4) return;
        graphics.blit(TEX_PANEL_DARK, x + 2, y + 2, width - 4, height - 4,
                0.0F, 0.0F, 256, 256, 256, 256);
        drawFrame(graphics, x, y, width, height);
    }

    public static void panelLight(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 4 || height <= 4) return;
        graphics.blit(TEX_PANEL_LIGHT, x + 2, y + 2, width - 4, height - 4,
                0.0F, 0.0F, 256, 256, 256, 256);
        drawFrame(graphics, x, y, width, height);
    }

    private static void drawFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.renderOutline(x, y, width, height, 0xFF0B0906);
        graphics.renderOutline(x + 1, y + 1, width - 2, height - 2, 0xFF5A4426);
        graphics.renderOutline(x + 2, y + 2, width - 4, height - 4, BORDER);
        // 四角黄铜饰钉
        int c = 3;
        int gold = 0xFFB08A3E;
        graphics.fill(x + 2, y + 2, x + 2 + c, y + 3, gold);
        graphics.fill(x + 2, y + 2, x + 3, y + 2 + c, gold);
        graphics.fill(x + width - 2 - c, y + 2, x + width - 2, y + 3, gold);
        graphics.fill(x + width - 3, y + 2, x + width - 2, y + 2 + c, gold);
        graphics.fill(x + 2, y + height - 3, x + 2 + c, y + height - 2, gold);
        graphics.fill(x + 2, y + height - 2 - c, x + 3, y + height - 2, gold);
        graphics.fill(x + width - 2 - c, y + height - 3, x + width - 2, y + height - 2, gold);
        graphics.fill(x + width - 3, y + height - 2 - c, x + width - 2, y + height - 2, gold);
    }

    /** 标题 + 两侧黄铜线 + 中央菱形，作为中世纪书页式页眉。 */
    public static void title(GuiGraphics graphics, Font font, String text, int centerX, int y) {
        text = CocLang.tr(text);
        graphics.drawCenteredString(font, text, centerX, y, GOLD);
        int half = Math.min(150, font.width(text) / 2 + 24);
        graphics.fill(centerX - half - 10, y + 11, centerX - 8, y + 12, 0x998A6A2F);
        graphics.fill(centerX + 8, y + 11, centerX + half + 10, y + 12, 0x998A6A2F);
        graphics.fill(centerX - 2, y + 9, centerX + 2, y + 14, ACCENT);
    }


    /** 把本模组界面里的原版按钮/输入框统一改成皮革+黄铜风格（不改变点击逻辑）。 */
    public static void styleWidgets(Screen screen, GuiGraphics graphics) {
        if (screen == null || !screen.getClass().getName().startsWith("com.coc.client.screen.")) return;
        Minecraft mc = Minecraft.getInstance();
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button button && button.visible) {
                int x = button.getX();
                int y = button.getY();
                int w = button.getWidth();
                int h = button.getHeight();
                boolean hover = button.isHoveredOrFocused();
                graphics.fill(x, y, x + w, y + h, hover ? 0xFF2C2117 : 0xFF1A1410);
                graphics.renderOutline(x, y, w, h, hover ? GOLD : BORDER);
                graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0x55C9A227);
                int color = button.active ? (hover ? 0xFFFFE7A3 : TEXT) : 0xFF7A6B58;
                String label = CocLang.tr(button.getMessage().getString());
                graphics.drawCenteredString(mc.font, label, x + w / 2, y + (h - 8) / 2, color);
            } else if (child instanceof EditBox box && box.visible) {
                graphics.renderOutline(box.getX() - 1, box.getY() - 1,
                        box.getWidth() + 2, box.getHeight() + 2, 0xBB8A6A2F);
                graphics.fill(box.getX(), box.getY(), box.getX() + box.getWidth(), box.getY() + 1, 0x33C9A227);
            }
        }
    }

    public static void wrapped(GuiGraphics graphics, Font font, String text, int x, int y,
                               int width, int color) {
        if (text == null || text.isEmpty()) return;
        graphics.drawWordWrap(font, Component.literal(CocLang.tr(text)), x, y, width, color);
    }

    public static int wrapHeight(Font font, String text, int width) {
        if (text == null || text.isEmpty()) return 0;
        return font.split(Component.literal(CocLang.tr(text)), width).size() * 9;
    }

    public static void bar(GuiGraphics graphics, int x, int y, int width, int height,
                           double ratio, int color) {
        graphics.fill(x, y, x + width, y + height, 0xFF2A2016);
        int filled = (int) Math.round(Math.max(0, Math.min(1, ratio)) * (width - 2));
        if (filled > 0) graphics.fill(x + 1, y + 1, x + 1 + filled, y + height - 1, color);
        graphics.renderOutline(x, y, width, height, 0xAA0B0906);
        graphics.fill(x, y, x + width, y + 1, 0x33C9A227);
    }

    public static String truncate(Font font, String text, int width) {
        if (text == null) return "";
        if (font.width(text) <= width) return text;
        String ellipsis = "…";
        int limit = width - font.width(ellipsis);
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (font.width(sb.toString() + c) > limit) break;
            sb.append(c);
        }
        return sb + ellipsis;
    }

    public static int sanColor(int san, int maxSan) {
        double ratio = maxSan <= 0 ? 0 : (double) san / maxSan;
        if (ratio > 0.6) return GREEN;
        if (ratio > 0.3) return 0xFFB08A3E;
        if (ratio > 0.15) return 0xFFA85A2A;
        return RED;
    }

    public static String dayOf(long gameTime) {
        return CocLang.tr("第 ") + (gameTime / 24000L + 1) + CocLang.tr(" 天");
    }
}
