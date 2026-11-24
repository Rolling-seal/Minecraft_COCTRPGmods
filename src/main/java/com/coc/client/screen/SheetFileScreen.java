package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.ui.CocUi;
import com.coc.net.CocNetwork;
import com.coc.net.packet.C2SExportSheet;
import com.coc.net.packet.C2SImportSheet;
import com.coc.net.packet.S2CSheetJson;
import com.coc.net.packet.S2CSheetResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

/** KP 角色卡 JSON 导出/导入。 */
public class SheetFileScreen extends Screen {
    private static final int MAX_JSON = 262144;
    private final Screen parent;
    private final UUID target;
    private final String targetName;
    private String status = "";
    private boolean success;
    private boolean compact;

    public SheetFileScreen(Screen parent, UUID target, String targetName) {
        super(Component.literal(CocLang.tr("角色卡 JSON")));
        this.parent = parent;
        this.target = target;
        this.targetName = targetName == null ? "" : targetName;
    }

    @Override
    protected void init() {
        compact = height < 420 || width < 700;
        int panelW = Math.min(width - 24, 380);
        int panelH = Math.min(height - 20, 220);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        addBtn(CocLang.tr("导出角色卡 JSON"), x + 10, y + 66, panelW - 20, 22, this::export);
        addBtn(CocLang.tr("导入角色卡 JSON"), x + 10, y + 96, panelW - 20, 22, this::chooseImport);
        addBtn(CocLang.tr("返回"), x + 10, y + panelH - 24, 100, 18, this::onClose);
    }

    private void export() {
        if (target == null) {
            status = CocLang.tr("未选择玩家。");
            return;
        }
        status = CocLang.tr("正在准备导出……");
        CocNetwork.sendToServer(new C2SExportSheet(target));
    }

    private void chooseImport() {
        if (minecraft == null || target == null) return;
        if (GraphicsEnvironment.isHeadless()) {
            status = CocLang.tr("当前环境没有图形界面，无法打开文件选择器。");
            return;
        }
        status = CocLang.tr("正在选择文件……");
        Thread thread = new Thread(() -> {
            String json = null;
            String error = null;
            try {
                FileDialog dialog = new FileDialog((Frame) null, CocLang.tr("选择角色卡 JSON"), FileDialog.LOAD);
                dialog.setFile("*.json");
                dialog.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".json"));
                dialog.setAlwaysOnTop(true);
                dialog.setVisible(true);
                String name = dialog.getFile();
                String dir = dialog.getDirectory();
                dialog.dispose();
                if (name == null) {
                    error = CocLang.tr("未选择文件。");
                } else {
                    File file = new File(dir, name);
                    if (!file.isFile()) error = CocLang.tr("文件不存在。");
                    else if (file.length() > MAX_JSON) error = CocLang.tr("角色卡 JSON 超过 256KB。");
                    else json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                }
            } catch (Throwable e) {
                error = CocLang.tr("读取文件失败：") + e.getMessage();
            }
            final String data = json;
            final String err = error;
            Minecraft.getInstance().execute(() -> {
                if (data == null) {
                    success = false;
                    status = err == null ? CocLang.tr("未选择文件。") : err;
                    return;
                }
                status = CocLang.tr("正在导入角色卡……");
                CocNetwork.sendToServer(new C2SImportSheet(target, data));
            });
        }, "coc-sheet-file-picker");
        thread.setDaemon(true);
        thread.start();
    }

    public void onExport(S2CSheetJson packet) {
        if (minecraft == null) return;
        if (GraphicsEnvironment.isHeadless()) {
            status = CocLang.tr("当前环境没有图形界面，无法保存文件。");
            return;
        }
        Thread thread = new Thread(() -> {
            String err = null;
            try {
                FileDialog dialog = new FileDialog((Frame) null, CocLang.tr("保存角色卡 JSON"), FileDialog.SAVE);
                dialog.setFile(packet.fileName == null ? "character.json" : packet.fileName);
                dialog.setAlwaysOnTop(true);
                dialog.setVisible(true);
                String name = dialog.getFile();
                String dir = dialog.getDirectory();
                dialog.dispose();
                if (name == null) {
                    err = CocLang.tr("未选择保存位置。");
                } else {
                    if (!name.toLowerCase().endsWith(".json")) name += ".json";
                    File file = new File(dir, name);
                    Files.writeString(file.toPath(), packet.json, StandardCharsets.UTF_8);
                }
            } catch (Throwable e) {
                err = CocLang.tr("保存失败：") + e.getMessage();
            }
            final String error = err;
            Minecraft.getInstance().execute(() -> {
                success = error == null;
                status = error == null ? CocLang.tr("角色卡已导出。") : error;
            });
        }, "coc-sheet-save-dialog");
        thread.setDaemon(true);
        thread.start();
    }

    public void onResult(S2CSheetResult result) {
        success = result.success;
        status = result.message;
    }

    private void addBtn(String label, int x, int y, int w, int h, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, w, h).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0000000);
        int panelW = Math.min(width - 24, 380);
        int panelH = Math.min(height - 20, 220);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        CocUi.panel(graphics, x, y, panelW, panelH);
        CocUi.title(graphics, font, CocLang.tr("角色卡 JSON"), width / 2, y + 6);
        graphics.drawString(font, CocLang.tr("目标：") + targetName, x + 10, y + 28, CocUi.GOLD, false);
        graphics.drawString(font, CocLang.tr("导出为 .json / 从 .json 覆盖导入"), x + 10, y + 44, CocUi.MUTED, false);
        if (!status.isEmpty()) {
            CocUi.wrapped(graphics, font, status, x + 10, y + 126, panelW - 20,
                    success ? CocUi.GREEN : CocUi.RED);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
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
