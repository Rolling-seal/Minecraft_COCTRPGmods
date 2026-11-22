package com.coc.client.fx;

import com.coc.client.ClientCache;
import com.coc.client.CocLang;
import com.coc.client.CocClientSettings;
import com.coc.client.ui.CocUi;
import com.coc.data.CharacterSheet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** SAN/HP/MP/幸运状态条与疯狂提示。 */
public final class SanityHud {
    private SanityHud() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        CocClientSettings settings = CocClientSettings.get();
        if (!settings.hud || mc.player == null || mc.screen != null || mc.options.hideGui) return;
        CharacterSheet sheet = ClientCache.self();
        if (sheet == null) return;
        int x = 8;
        int y = 8;
        int width = 132;
        graphics.fill(x, y, x + width, y + 80, 0x80000000);
        graphics.renderOutline(x, y, width, 80, 0x667C6A9A);
        drawBar(graphics, x + 4, y + 5, width - 8, "SAN", sheet.san, sheet.maxSan,
                CocUi.sanColor(sheet.san, sheet.maxSan));
        drawBar(graphics, x + 4, y + 21, width - 8, "HP", sheet.hp, sheet.maxHp, CocUi.RED);
        drawBar(graphics, x + 4, y + 37, width - 8, "MP", sheet.mp, sheet.maxMp, CocUi.BLUE);
        drawBar(graphics, x + 4, y + 53, width - 8, CocLang.tr("幸运"), sheet.stats.luck, 99, CocUi.GREEN);

        long gameTime = mc.level == null ? 0 : mc.level.getGameTime();
        if (sheet.isInsane(gameTime)) {
            String text = sheet.retired ? CocLang.tr("永久疯狂")
                    : (sheet.indefiniteInsanity ? CocLang.tr("不定期疯狂") : CocLang.tr("临时疯狂"));
            graphics.drawString(mc.font, text, x + 4, y + 68, 0xFFE06060, false);
        } else if (sheet.dying) {
            graphics.drawString(mc.font, CocLang.tr("濒死"), x + 4, y + 68, 0xFFFF4040, false);
        } else {
            graphics.drawString(mc.font, CocLang.tr("神志清醒"), x + 4, y + 68, 0xFF8AB88A, false);
        }
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int width, String label,
                                int value, int max, int color) {
        Minecraft mc = Minecraft.getInstance();
        graphics.drawString(mc.font, label, x, y, CocUi.MUTED, false);
        int barX = x + 22;
        int barW = width - 22;
        CocUi.bar(graphics, barX, y - 1, barW, 11, max <= 0 ? 0 : (double) value / max, color);
        String text = value + "/" + max;
        graphics.drawString(mc.font, text, barX + barW - mc.font.width(text) - 2, y, 0xFFFFFFFF, true);
    }
}
