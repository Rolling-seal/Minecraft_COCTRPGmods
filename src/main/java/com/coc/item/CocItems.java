package com.coc.item;

import com.coc.CallOfCthulhu;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 本模组物品注册。 */
@Mod.EventBusSubscriber(modid = CallOfCthulhu.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CocItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CallOfCthulhu.MODID);

    public static final RegistryObject<Item> KP_PANEL = ITEMS.register("kp_panel",
            () -> new KeeperPanelItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    private CocItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    @SubscribeEvent
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (CreativeModeTabs.TOOLS_AND_UTILITIES.equals(event.getTabKey())) {
            event.accept(KP_PANEL);
        }
    }
}
