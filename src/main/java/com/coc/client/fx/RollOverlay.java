package com.coc.client.fx;

import com.coc.client.ClientCache;
import com.coc.client.CocLang;
import com.coc.client.CocClientSettings;
import com.coc.client.RollView;
import com.coc.client.ui.CocUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** 检定结果浮层：数字滚动 0.6 秒后定格。 */
public final class RollOverlay {
    private RollOverlay() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        CocClientSettings settings = CocClientSettings.get();
        if (!settings.diceOverlay || mc.player == null) return;
        RollView roll = ClientCache.lastRoll;
        if (roll == null) return;
        long age = System.currentTimeMillis() - roll.receivedAt;
        if (age > 6000L) return;
        if (mc.screen != null) return;

        int width = 300;
        int x = mc.getWindow().getGuiScaledWidth() / 2 - width / 2;
        int y = 28;
        float alpha = age > 5000L ? (6000L - age) / 1000.0F : 1.0F;
        int background = ((int) (alpha * 0xC0) << 24);
        graphics.fill(x, y, x + width, y + 54, background);
        graphics.renderOutline(x, y, width, 54, 0xFF7C6A9A);

        String number = String.valueOf(roll.roll);
        if (age < 600L && roll.target >= 0) {
            number = String.valueOf((int) ((System.currentTimeMillis() / 45L) % 100L) + 1);
        }
        graphics.drawCenteredString(mc.font, CocLang.message(roll.title), x + width / 2, y + 5, CocUi.ACCENT);
        graphics.drawString(mc.font, number, x + 14, y + 20, roll.levelColor, true);
        String level = CocLang.tr(roll.level);
        graphics.drawString(mc.font, level, x + 70, y + 22, roll.levelColor, false);
        String target = roll.target >= 0 ? CocLang.tr("目标 ") + roll.target : "";
        graphics.drawString(mc.font, target, x + width - mc.font.width(target) - 14, y + 22, CocUi.MUTED, false);
        graphics.drawString(mc.font, CocUi.truncate(mc.font, CocLang.message(roll.detail), width - 20),
                x + 10, y + 40, CocUi.MUTED, false);
    }
}
