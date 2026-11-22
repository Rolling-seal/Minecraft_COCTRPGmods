package com.coc.client;

import com.coc.CallOfCthulhu;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

/** 客户端初始化：设置加载与按键注册。 */
public final class ClientSetup {
    public static final KeyMapping SHEET_KEY = new KeyMapping("key.coc.sheet",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.categories.coc");
    public static final KeyMapping KP_KEY = new KeyMapping("key.coc.kp",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.coc");
    public static final KeyMapping JOURNAL_KEY = new KeyMapping("key.coc.journal",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.coc");

    private ClientSetup() {
    }

    public static void init(IEventBus modBus) {
        CocClientSettings.load();
        modBus.addListener(ClientSetup::registerKeys);
        CallOfCthulhu.LOGGER.info("克苏鲁的呼唤：客户端按键已注册");
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SHEET_KEY);
        event.register(KP_KEY);
        event.register(JOURNAL_KEY);
    }
}
