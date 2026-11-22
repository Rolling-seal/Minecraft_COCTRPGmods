package com.coc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 服务端设置：config/coc-server.json。KP 面板可直接修改并保存。 */
public final class CocServerConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CocServerConfig instance;

    public boolean randomEventsEnabled = true;
    public int randomEventIntervalTicks = 24000;
    public int minPlayerEventIntervalTicks = 6000;
    public int eventTimeoutTicks = 2400;
    public boolean allowPlayerDice = true;
    public List<String> kpNames = new ArrayList<>();

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("coc-server.json");
    }

    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get().resolve("coc");
    }

    public static synchronized CocServerConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static synchronized void load() {
        Path file = path();
        CocServerConfig config = new CocServerConfig();
        if (Files.isRegularFile(file)) {
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                CocServerConfig parsed = GSON.fromJson(stripBom(json), CocServerConfig.class);
                if (parsed != null) config = parsed;
            } catch (Exception e) {
                LOGGER.error("读取 coc-server.json 失败，使用默认设置", e);
            }
        } else {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(config), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.error("写入默认 coc-server.json 失败", e);
            }
        }
        if (config.kpNames == null) config.kpNames = new ArrayList<>();
        instance = config;
    }

    public static synchronized void save() {
        if (instance == null) return;
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("保存 coc-server.json 失败", e);
        }
    }

    private static String stripBom(String s) {
        return s != null && s.startsWith("\uFEFF") ? s.substring(1) : s;
    }
}
