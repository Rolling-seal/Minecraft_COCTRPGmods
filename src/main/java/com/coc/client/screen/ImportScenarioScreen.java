package com.coc.client.screen;

import com.coc.client.CocLang;
import com.coc.client.NativeFileDialog;
import com.coc.client.ui.CocUi;
import com.coc.net.CocNetwork;
import com.coc.net.packet.C2SImportScenario;
import com.coc.net.packet.S2CImportResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 玩家剧本导入：选择一份 coc-scenario-v1 JSON 文件。 */
public class ImportScenarioScreen extends Screen {
    private final Screen parent;
    private String status = "";
    private boolean success;
    private boolean submitted;
    private boolean compact;

    public ImportScenarioScreen(Screen parent) {
        super(Component.literal(CocLang.tr("导入剧本")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        compact = height < 420 || width < 700;
        int bottom = height - 24;
        addBtn(CocLang.tr("选择 JSON 文件"), 12, bottom, 120, 18, this::chooseFile);
        addBtn(CocLang.tr("生成示例文件"), 136, bottom, 120, 18, this::saveSample);
        addBtn(CocLang.tr("返回"), width - 82, bottom, 70, 18, this::onClose);
    }

    private void chooseFile() {
        if (minecraft == null) return;
        status = CocLang.tr("正在打开文件选择器……");
        success = true;
        Thread thread = new Thread(() -> {
            String json = null;
            String error = null;
            try {
                File file = NativeFileDialog.openJson(CocLang.tr("选择剧本 JSON"));
                if (file == null) {
                    error = CocLang.tr("未选择文件。");
                } else if (!file.isFile()) {
                    error = CocLang.tr("文件不存在：") + file.getAbsolutePath();
                } else if (!file.getName().toLowerCase().endsWith(".json")) {
                    error = CocLang.tr("请选择 .json 文件。");
                } else if (file.length() > C2SImportScenario.MAX_JSON) {
                    error = CocLang.tr("JSON 文件超过 64KB 上限，请精简文本。");
                } else {
                    json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                }
            } catch (Throwable e) {
                error = CocLang.tr("读取文件失败：") + e.getMessage();
            }
            final String picked = json;
            final String err = error;
            Minecraft.getInstance().execute(() -> {
                if (picked == null) {
                    status = err == null ? CocLang.tr("未选择文件。") : err;
                    success = false;
                    return;
                }
                submitted = true;
                status = CocLang.tr("正在导入文件……");
                CocNetwork.sendToServer(new C2SImportScenario(normalize(picked)));
            });
        }, "coc-scenario-file-picker");
        thread.setDaemon(true);
        thread.start();
    }

    private String normalize(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.startsWith("\uFEFF")) s = s.substring(1).trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) s = s.substring(nl + 1);
            int end = s.lastIndexOf("```");
            if (end >= 0) s = s.substring(0, end);
            s = s.trim();
        }
        return s;
    }

    private void saveSample() {
        if (minecraft == null) return;
        try {
            Path dir = new File(minecraft.gameDirectory, "config/coc").toPath();
            Files.createDirectories(dir);
            Path file = dir.resolve("example_scenario.json");
            if (Files.exists(file)) {
                status = CocLang.tr("示例已存在：") + file.toAbsolutePath();
                success = true;
                return;
            }
            Files.writeString(file, SAMPLE, StandardCharsets.UTF_8);
            status = CocLang.tr("示例已保存：") + file.toAbsolutePath() + CocLang.tr("（可编辑后选择导入）");
            success = true;
        } catch (Exception e) {
            status = CocLang.tr("保存示例失败：") + e.getMessage();
            success = false;
        }
    }

    public void onResult(S2CImportResult result) {
        submitted = false;
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
        CocUi.title(graphics, font, CocLang.tr("导入剧本"), width / 2, compact ? 4 : 8);
        int px = 12;
        int py = compact ? 26 : 46;
        int pw = width - 24;
        int ph = height - py - (compact ? 30 : 50);
        CocUi.panel(graphics, px, py, pw, ph);
        int tx = px + 12;
        int ty = py + 10;
        int tw = pw - 24;
        graphics.drawString(font, CocLang.tr("选择一份符合格式的 .json 文件导入。"), tx, ty, CocUi.TEXT, false);
        graphics.drawString(font, CocLang.tr("最大 64KB；导入成功后，KP 的剧情书会多出这个剧本。"), tx, ty + 14, CocUi.MUTED, false);
        graphics.drawString(font, CocLang.tr("必填：format / id / title / acts[].title / acts[].description"), tx, ty + 32, CocUi.ACCENT, false);
        graphics.drawString(font, CocLang.tr("可选：intro / outro / sanReward / options / sideEvents"), tx, ty + 46, CocUi.ACCENT, false);
        graphics.drawString(font, CocLang.tr("id 只能用小写字母、数字、下划线，例如 my_story。"), tx, ty + 60, CocUi.MUTED, false);
        graphics.drawString(font, CocLang.tr("options 里的 success/failure 只写文字，不执行物品或命令。"), tx, ty + 74, CocUi.MUTED, false);
        graphics.drawString(font, CocLang.tr("可点“生成示例文件”，用记事本编辑后再选择它。"), tx, ty + 94, CocUi.TEXT, false);
        if (!status.isEmpty()) {
            int color = success ? CocUi.GREEN : CocUi.RED;
            CocUi.wrapped(graphics, font, status, tx, ty + 116, tw, color);
        }
        if (submitted) {
            graphics.drawString(font, CocLang.tr("等待服务器校验……"), tx, ty + 146, CocUi.MUTED, false);
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

    private static final String SAMPLE = """
{
  "format": "coc-scenario-v1",
  "id": "my_story",
  "title": "My Scenario",
  "description": "A one-line summary.",
  "intro": "The opening curtain text the players see.",
  "outro": "The closing curtain text.",
  "sanReward": "1d10",
  "acts": [
    {
      "title": "Rainy Night",
      "summary": "Keeper reference: the investigators arrive at the village and find the church unlit.",
      "trigger": "When the investigators enter the village",
      "description": "You reach the village gate in the rain. The church is dark, and water drips somewhere in the bell tower.",
      "options": [
        {
          "text": "Inspect the church",
          "skill": "spot_hidden",
          "difficulty": "regular",
          "success": "You find a line of wet footprints in the door gap.",
          "successClue": "The old postmark came from Saltmarsh twenty years ago.",
          "failure": "You only smell damp wood."
        }
      ],
      "sideEvents": [
        {
          "title": "Side Scene: Water in the Bell Tower",
          "trigger": "When an investigator approaches the bell tower",
          "description": "The water stops. You hear someone counting upstairs.",
          "options": [
            {
              "text": "Go upstairs",
              "skill": "listen",
              "difficulty": "regular",
              "success": "The counting imitates your breathing.",
              "failure": "A stair breaks under your foot.",
              "failureClue": "A fresh claw mark runs along the handrail."
            }
          ]
        }
      ]
    },
    {
      "title": "The Cellar",
      "summary": "The investigators enter the cellar and face the final choice.",
      "trigger": "When the investigators open the cellar door",
      "description": "A lamp burns deep in the cellar, though it should not.",
      "options": [
        {
          "text": "Put it out",
          "skill": "occult",
          "difficulty": "hard",
          "success": "The flame dies. Something in the dark exhales.",
          "failure": "The wick bursts into blue fire."
        }
      ]
    }
  ]
}
""";
}
