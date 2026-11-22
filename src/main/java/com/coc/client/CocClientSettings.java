package com.coc.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 客户端设置：config/coc-client.json。设置界面可实时修改并保存。 */
public final class CocClientSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CocClientSettings instance;

    public boolean sanityEffects = true;
    public int distortionLevel = 1; // 0=轻, 1=标准, 2=强
    public boolean cameraShake = true;
    public boolean vignette = true;
    public boolean whispers = true;
    public boolean hud = true;
    public boolean diceOverlay = true;

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("coc-client.json");
    }

    public static synchronized CocClientSettings get() {
        if (instance == null) load();
        return instance;
    }

    public static synchronized void load() {
        CocClientSettings settings = new CocClientSettings();
        Path file = path();
        if (Files.isRegularFile(file)) {
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                if (json.startsWith("\uFEFF")) json = json.substring(1);
                CocClientSettings parsed = GSON.fromJson(json, CocClientSettings.class);
                if (parsed != null) settings = parsed;
            } catch (Exception ignored) {
            }
        }
        if (settings.distortionLevel < 0) settings.distortionLevel = 0;
        if (settings.distortionLevel > 2) settings.distortionLevel = 2;
        instance = settings;
        save();
    }

    public static synchronized void save() {
        if (instance == null) return;
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    public String distortionLabel() {
        return switch (distortionLevel) {
            case 0 -> CocLang.tr("轻");
            case 2 -> CocLang.tr("强");
            default -> CocLang.tr("标准");
        };
    }
}
