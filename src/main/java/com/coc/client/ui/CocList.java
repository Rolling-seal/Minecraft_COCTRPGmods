package com.coc.client.ui;

import com.coc.client.CocLang;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** 轻量滚动列表：不依赖 ObjectSelectionList，避免版本差异。 */
public final class CocList<T> {
    public interface RowRenderer<T> {
        void render(GuiGraphics graphics, Font font, T item, int x, int y, int width, int height,
                    boolean hovered, boolean selected);

        default void onClick(T item) {
        }
    }

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int rowHeight;
    private final RowRenderer<T> renderer;
    private List<T> items = new ArrayList<>();
    private int scroll;
    private int selected = -1;

    public CocList(int x, int y, int width, int height, int rowHeight, RowRenderer<T> renderer) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rowHeight = rowHeight;
        this.renderer = renderer;
    }

    public void setItems(List<T> items) {
        this.items = items == null ? new ArrayList<>() : items;
        this.scroll = 0;
        this.selected = -1;
    }

    /** 刷新同一列表的数据时保留滚动位置与选中项，避免页面突然跳动。 */
    public void setItemsKeepState(List<T> items) {
        int oldScroll = this.scroll;
        int oldSelected = this.selected;
        this.items = items == null ? new ArrayList<>() : items;
        if (oldSelected >= 0 && oldSelected < this.items.size()) {
            this.selected = oldSelected;
        } else {
            this.selected = -1;
        }
        this.scroll = Math.max(0, Math.min(maxScroll(), oldScroll));
    }

    public int getScroll() {
        return scroll;
    }

    public void setScroll(int value) {
        this.scroll = Math.max(0, Math.min(maxScroll(), value));
    }

    private int maxScroll() {
        return Math.max(0, items.size() * rowHeight + 4 - height);
    }

    public List<T> items() {
        return items;
    }

    public T getSelected() {
        return selected >= 0 && selected < items.size() ? items.get(selected) : null;
    }

    public int getSelectedIndex() {
        return selected;
    }

    public void setSelectedIndex(int index) {
        this.selected = index >= 0 && index < items.size() ? index : -1;
        if (this.selected >= 0) ensureVisible(this.selected);
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        CocUi.panelLight(graphics, x, y, width, height);
        if (items.isEmpty()) {
            graphics.drawCenteredString(font, CocLang.tr("（空）"), x + width / 2, y + height / 2 - 4, CocUi.MUTED);
            return;
        }
        int contentHeight = items.size() * rowHeight + 4;
        int maxScroll = Math.max(0, contentHeight - height);
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;

        graphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
        int first = Math.max(0, scroll / rowHeight);
        for (int i = first; i < items.size(); i++) {
            int rowTop = y + 2 + i * rowHeight - scroll;
            if (rowTop > y + height - 2) break;
            int rowBottom = rowTop + rowHeight;
            if (rowBottom < y + 2) continue;
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= rowTop && mouseY < rowBottom;
            boolean isSelected = i == selected;
            if (isSelected) {
                graphics.fill(x + 2, rowTop, x + width - 6, rowBottom, 0x55B98ADF);
            } else if (hovered) {
                graphics.fill(x + 2, rowTop, x + width - 6, rowBottom, 0x22FFFFFF);
            }
            renderer.render(graphics, font, items.get(i), x + 5, rowTop, width - 14, rowHeight,
                    hovered, isSelected);
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackX = x + width - 5;
            graphics.fill(trackX, y + 2, trackX + 3, y + height - 2, 0x66000000);
            int thumbHeight = Math.max(12, (height - 4) * (height - 4) / contentHeight);
            int thumbY = y + 2 + (height - 4 - thumbHeight) * scroll / maxScroll;
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFF8B7AA8);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (!inside(mouseX, mouseY)) return false;
        int index = indexAt(mouseY);
        if (index < 0 || index >= items.size()) return false;
        selected = index;
        renderer.onClick(items.get(index));
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!inside(mouseX, mouseY)) return false;
        int contentHeight = items.size() * rowHeight + 4;
        int maxScroll = Math.max(0, contentHeight - height);
        scroll = (int) Math.max(0, Math.min(maxScroll, scroll - delta * rowHeight * 3));
        return true;
    }

    public boolean inside(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int indexAt(double mouseY) {
        return (int) ((mouseY - y - 2 + scroll) / rowHeight);
    }

    private void ensureVisible(int index) {
        int maxScroll = Math.max(0, items.size() * rowHeight + 4 - height);
        int top = index * rowHeight;
        if (top < scroll) scroll = top;
        if (top + rowHeight > scroll + height - 4) scroll = top + rowHeight - height + 4;
        scroll = Math.max(0, Math.min(maxScroll, scroll));
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
