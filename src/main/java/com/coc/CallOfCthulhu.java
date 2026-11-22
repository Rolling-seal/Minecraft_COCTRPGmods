package com.coc;

import com.coc.item.CocItems;
import com.coc.net.CocNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** 克苏鲁的呼唤 · 1.20.1 Forge。 */
@Mod(CallOfCthulhu.MODID)
public final class CallOfCthulhu {
    public static final String MODID = "coc";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CallOfCthulhu() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        CocItems.register(modBus);
        modBus.addListener(this::commonSetup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            try {
                Class.forName("com.coc.client.ClientSetup")
                        .getMethod("init", IEventBus.class)
                        .invoke(null, modBus);
            } catch (ReflectiveOperationException e) {
                LOGGER.error("客户端初始化失败", e);
            }
        });
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CocNetwork::register);
        LOGGER.info("克苏鲁的呼唤：网络通道已注册");
    }
}
