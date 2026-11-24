package com.coc.net;

import com.coc.data.CharacterSheet;
import com.coc.server.CocData;
import com.coc.net.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** 网络通道：协议版本 4。所有复杂数据用 NBT 传递。 */
public final class CocNetwork {
    private static final String PROTOCOL = "4";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("coc", "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private CocNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, C2SRequestSync.class,
                C2SRequestSync::encode, C2SRequestSync::new, C2SRequestSync::handle);
        CHANNEL.registerMessage(id++, C2SCreateCharacter.class,
                C2SCreateCharacter::encode, C2SCreateCharacter::new, C2SCreateCharacter::handle);
        CHANNEL.registerMessage(id++, C2SRoll.class,
                C2SRoll::encode, C2SRoll::new, C2SRoll::handle);
        CHANNEL.registerMessage(id++, C2SCheckAction.class,
                C2SCheckAction::encode, C2SCheckAction::new, C2SCheckAction::handle);
        CHANNEL.registerMessage(id++, C2SEventChoice.class,
                C2SEventChoice::encode, C2SEventChoice::new, C2SEventChoice::handle);
        CHANNEL.registerMessage(id++, C2SKpAction.class,
                C2SKpAction::encode, C2SKpAction::new, C2SKpAction::handle);
        CHANNEL.registerMessage(id++, S2CSyncSheet.class,
                S2CSyncSheet::encode, S2CSyncSheet::new, S2CSyncSheet::handle);
        CHANNEL.registerMessage(id++, S2CRollResult.class,
                S2CRollResult::encode, S2CRollResult::new, S2CRollResult::handle);
        CHANNEL.registerMessage(id++, S2CEventOffer.class,
                S2CEventOffer::encode, S2CEventOffer::new, S2CEventOffer::handle);
        CHANNEL.registerMessage(id++, S2CEventResult.class,
                S2CEventResult::encode, S2CEventResult::new, S2CEventResult::handle);
        CHANNEL.registerMessage(id++, S2CKpData.class,
                S2CKpData::encode, S2CKpData::new, S2CKpData::handle);
        CHANNEL.registerMessage(id++, S2CHandout.class,
                S2CHandout::encode, S2CHandout::new, S2CHandout::handle);
        CHANNEL.registerMessage(id++, S2COpenScreen.class,
                S2COpenScreen::encode, S2COpenScreen::new, S2COpenScreen::handle);
        CHANNEL.registerMessage(id++, C2SKpRoll.class,
                C2SKpRoll::encode, C2SKpRoll::new, C2SKpRoll::handle);
        CHANNEL.registerMessage(id++, S2CActCurtain.class,
                S2CActCurtain::encode, S2CActCurtain::new, S2CActCurtain::handle);
        CHANNEL.registerMessage(id++, S2CItemList.class,
                S2CItemList::encode, S2CItemList::new, S2CItemList::handle);
        CHANNEL.registerMessage(id++, S2CBookData.class,
                S2CBookData::encode, S2CBookData::new, S2CBookData::handle);
        CHANNEL.registerMessage(id++, C2SImportScenario.class,
                C2SImportScenario::encode, C2SImportScenario::new, C2SImportScenario::handle);
        CHANNEL.registerMessage(id++, S2CImportResult.class,
                S2CImportResult::encode, S2CImportResult::new, S2CImportResult::handle);
        CHANNEL.registerMessage(id++, C2SGlobalSettings.class,
                C2SGlobalSettings::encode, C2SGlobalSettings::new, C2SGlobalSettings::handle);
        CHANNEL.registerMessage(id++, S2CGlobalSettings.class,
                S2CGlobalSettings::encode, S2CGlobalSettings::new, S2CGlobalSettings::handle);
        CHANNEL.registerMessage(id++, C2SExportSheet.class,
                C2SExportSheet::encode, C2SExportSheet::new, C2SExportSheet::handle);
        CHANNEL.registerMessage(id++, S2CSheetJson.class,
                S2CSheetJson::encode, S2CSheetJson::new, S2CSheetJson::handle);
        CHANNEL.registerMessage(id++, C2SImportSheet.class,
                C2SImportSheet::encode, C2SImportSheet::new, C2SImportSheet::handle);
        CHANNEL.registerMessage(id++, S2CSheetResult.class,
                S2CSheetResult::encode, S2CSheetResult::new, S2CSheetResult::handle);
        CHANNEL.registerMessage(id++, C2SItemRule.class,
                C2SItemRule::encode, C2SItemRule::new, C2SItemRule::handle);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void syncSheet(ServerPlayer player, CharacterSheet sheet) {
        if (player != null && player.getServer() != null) {
            // 标记世界存档为脏，确保角色卡修改落盘
            CocData.get(player.getServer()).putSheet(sheet);
        }
        sendToPlayer(player, new S2CSyncSheet(sheet.toNbt(), true, sheet.uuid, true));
    }

    public static void syncOther(ServerPlayer player, CharacterSheet sheet) {
        sendToPlayer(player, new S2CSyncSheet(sheet.toNbt(), false, sheet.uuid, true));
    }

    public static void openScreen(ServerPlayer player, String screen, java.util.UUID target, boolean editable) {
        sendToPlayer(player, new S2COpenScreen(screen, target, editable));
    }
}
