package com.coc.client;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 文件选择器：Windows 使用 PowerShell + WinForms（不依赖 Java AWT 的 headless 检测），
 * 其它桌面环境退回 AWT FileDialog。
 */
public final class NativeFileDialog {
    private NativeFileDialog() {
    }

    public static File openJson(String title) throws IOException {
        return show(title, false, "");
    }

    public static File saveJson(String title, String defaultName) throws IOException {
        return show(title, true, defaultName == null ? "character.json" : defaultName);
    }

    private static File show(String title, boolean save, String defaultName) throws IOException {
        if (isWindows()) {
            try {
                return windowsDialog(title, save, defaultName);
            } catch (IOException e) {
                if (GraphicsEnvironment.isHeadless()) throw e;
            }
        }
        if (GraphicsEnvironment.isHeadless()) {
            throw new IOException("当前环境没有可用的图形文件选择器。");
        }
        return awtDialog(title, save, defaultName);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static File windowsDialog(String title, boolean save, String defaultName) throws IOException {
        String filter = "JSON 文件 (*.json)|*.json|所有文件 (*.*)|*.*";
        StringBuilder ps = new StringBuilder();
        ps.append("$ErrorActionPreference='Stop'\n");
        ps.append("[Console]::OutputEncoding=[System.Text.Encoding]::UTF8\n");
        ps.append("Add-Type -AssemblyName System.Windows.Forms\n");
        ps.append("Add-Type -AssemblyName System.Drawing\n");
        ps.append("[System.Windows.Forms.Application]::EnableVisualStyles()\n");
        ps.append("$owner=New-Object System.Windows.Forms.Form\n");
        ps.append("$owner.TopMost=$true\n");
        ps.append("$owner.ShowInTaskbar=$false\n");
        ps.append("$owner.FormBorderStyle=[System.Windows.Forms.FormBorderStyle]::None\n");
        ps.append("$owner.Size=New-Object System.Drawing.Size(1,1)\n");
        ps.append("$owner.StartPosition=[System.Windows.Forms.FormStartPosition]::CenterScreen\n");
        ps.append("$owner.Opacity=0\n");
        ps.append("$owner.Show()\n");
        ps.append("try {\n");
        if (save) {
            ps.append("$d=New-Object System.Windows.Forms.SaveFileDialog\n");
            ps.append("$d.Title=").append(psQuote(title)).append("\n");
            ps.append("$d.Filter=").append(psQuote(filter)).append("\n");
            ps.append("$d.DefaultExt='json'\n");
            ps.append("$d.AddExtension=$true\n");
            ps.append("$d.OverwritePrompt=$true\n");
            ps.append("$d.FileName=").append(psQuote(defaultName)).append("\n");
        } else {
            ps.append("$d=New-Object System.Windows.Forms.OpenFileDialog\n");
            ps.append("$d.Title=").append(psQuote(title)).append("\n");
            ps.append("$d.Filter=").append(psQuote(filter)).append("\n");
            ps.append("$d.CheckFileExists=$true\n");
            ps.append("$d.Multiselect=$false\n");
        }
        ps.append("$r=$d.ShowDialog($owner)\n");
        ps.append("if($r -eq [System.Windows.Forms.DialogResult]::OK){[Console]::Out.Write($d.FileName)}else{[Console]::Out.Write('__CANCEL__')}\n");
        ps.append("[Console]::Out.Flush()\n");
        ps.append("} finally { $owner.Close() }\n");
        String encoded = Base64.getEncoder().encodeToString(
                ps.toString().getBytes(StandardCharsets.UTF_16LE));
        Process process;
        try {
            process = new ProcessBuilder("powershell.exe", "-NoProfile", "-STA",
                    "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden",
                    "-EncodedCommand", encoded)
                    .redirectErrorStream(false).start();
        } catch (IOException e) {
            throw new IOException("无法启动 Windows 文件选择器：" + e.getMessage(), e);
        }
        try {
            int code = process.waitFor();
            String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            String err = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (code != 0) {
                throw new IOException(err.isEmpty() ? "文件选择器退出码 " + code : err);
            }
            if (out.isEmpty() || "__CANCEL__".equals(out)) return null;
            File file = new File(out);
            return file.isAbsolute() ? file : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("文件选择器被中断。", e);
        } finally {
            process.destroy();
        }
    }

    private static String psQuote(String value) {
        return "'" + (value == null ? "" : value.replace("'", "''")) + "'";
    }

    private static File awtDialog(String title, boolean save, String defaultName) throws IOException {
        Frame frame = new Frame();
        try {
            FileDialog dialog = new FileDialog(frame, title,
                    save ? FileDialog.SAVE : FileDialog.LOAD);
            dialog.setFile(save ? defaultName : "*.json");
            if (!save) {
                dialog.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".json"));
            }
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
            String name = dialog.getFile();
            String dir = dialog.getDirectory();
            dialog.dispose();
            if (name == null) return null;
            File file = new File(dir, name);
            if (save && !name.toLowerCase().endsWith(".json")) {
                file = new File(dir, name + ".json");
            }
            return file;
        } finally {
            frame.dispose();
        }
    }
}
