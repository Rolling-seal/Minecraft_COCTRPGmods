package com.coc.client.fx;

import com.coc.client.ClientCache;
import com.coc.client.CocLang;
import com.coc.client.CocClientSettings;
import com.coc.data.CharacterSheet;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;

/** 低 SAN 视觉/声音特效：后处理、镜头、暗角、低语、心跳。 */
public final class SanityEffects {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation LOW_SAN =
            new ResourceLocation("coc", "shaders/post/low_san.json");
    private static final ResourceLocation INSANE =
            new ResourceLocation("coc", "shaders/post/insane.json");
    private static final String[] WHISPERS = {
            "你听见有人用你的声音在数数。",
            "墙后有什么东西在模仿你的呼吸。",
            "别回头。",
            "海水的味道越来越近了。",
            "它们记得你的名字。",
            "你确定刚才那里有一扇门吗？",
            "影子比你慢了一拍。",
            "有人在梦里等你。",
            "你数不清自己的手指。",
            "钟表停在 3:17。",
            "雾里有人在鼓掌。",
            "那不是你的倒影。",
            "地下有什么在翻身。",
            "你听见母亲在叫你，但她已经不在了。",
            "星星的位置不对。",
            "你开始理解那些螺旋了。",
            "有人在你的皮肤下面写字。",
            "它醒了。",
            "你确定这是第一次经历这一切吗？",
            "别相信镜子里的那个。"
    };

    private static ResourceLocation appliedId;
    private static PostChain ourEffect;
    private static int ticks;
    private static int nextWhisper = 300;
    private static int nextHeartbeat = 200;
    private static final RandomSource RANDOM = RandomSource.create();

    private SanityEffects() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            removeEffect();
            return;
        }
        ticks++;
        CharacterSheet sheet = ClientCache.self();
        CocClientSettings settings = CocClientSettings.get();
        if (sheet == null || !settings.sanityEffects) {
            removeEffect();
            return;
        }
        double ratio = sheet.sanRatio();
        boolean insane = sheet.isInsane(currentGameTime(mc));
        boolean low = isLowSan(ratio, settings);
        ResourceLocation desired = null;
        if (insane) {
            desired = INSANE;
        } else if (low) {
            desired = LOW_SAN;
        }
        if (desired == null) {
            removeEffect();
        } else if (!desired.equals(appliedId)) {
            PostChain current = mc.gameRenderer.currentEffect();
            if (current == null || current == ourEffect) {
                if (current == ourEffect) mc.gameRenderer.shutdownEffect();
                applyEffect(mc, desired);
            }
        } else if (ourEffect != null && mc.gameRenderer.currentEffect() == null && ticks % 100 == 0) {
            applyEffect(mc, desired);
        }

        if (settings.whispers && (insane || low)) {
            if (--nextWhisper <= 0) {
                nextWhisper = 500 + RANDOM.nextInt(900);
                String whisper = WHISPERS[RANDOM.nextInt(WHISPERS.length)];
                mc.player.displayClientMessage(Component.literal(whisper)
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
                play(mc, SoundEvents.AMBIENT_CAVE.value(), 0.35F, 0.6F);
            }
            if (ratio < 0.22 || insane) {
                if (--nextHeartbeat <= 0) {
                    nextHeartbeat = 50 + RANDOM.nextInt(50);
                    play(mc, SoundEvents.WARDEN_HEARTBEAT, 0.55F, 0.8F);
                }
            }
        }
    }

    private static void applyEffect(Minecraft mc, ResourceLocation id) {
        try {
            mc.gameRenderer.loadEffect(id);
            ourEffect = mc.gameRenderer.currentEffect();
            if (ourEffect != null) {
                appliedId = id;
            } else {
                appliedId = null;
                LOGGER.warn("COC 后处理未生效：{}", id);
            }
        } catch (Throwable t) {
            LOGGER.warn("COC 后处理加载失败：{}（已禁用本模组特效）", id, t);
            appliedId = null;
            ourEffect = null;
        }
    }

    private static void removeEffect() {
        Minecraft mc = Minecraft.getInstance();
        if (ourEffect != null && mc.gameRenderer.currentEffect() == ourEffect) {
            mc.gameRenderer.shutdownEffect();
        }
        ourEffect = null;
        appliedId = null;
    }

    public static void reset() {
        removeEffect();
        ticks = 0;
        nextWhisper = 300;
        nextHeartbeat = 200;
    }

    public static boolean isLowSan(double ratio, CocClientSettings settings) {
        if (!settings.sanityEffects) return false;
        double threshold = settings.distortionLevel >= 2 ? 0.45
                : (settings.distortionLevel == 1 ? 0.30 : 0.15);
        return ratio <= threshold;
    }

    public static boolean lowSanActive() {
        CharacterSheet sheet = ClientCache.self();
        if (sheet == null) return false;
        return isLowSan(sheet.sanRatio(), CocClientSettings.get())
                || sheet.isInsane(currentGameTime(Minecraft.getInstance()));
    }

    public static void onCameraAngles(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        CharacterSheet sheet = ClientCache.self();
        CocClientSettings settings = CocClientSettings.get();
        if (mc.player == null || sheet == null || !settings.sanityEffects || !settings.cameraShake) return;
        boolean insane = sheet.isInsane(currentGameTime(mc));
        boolean low = isLowSan(sheet.sanRatio(), settings);
        if (!insane && !low) return;
        float t = (System.currentTimeMillis() % 100000L) / 1000.0F;
        float amp = insane ? 1.4F : 0.55F;
        event.setRoll(event.getRoll() + Mth.sin(t * 2.1F) * amp);
        event.setYaw(event.getYaw() + Mth.sin(t * 1.7F) * amp * 0.35F);
        event.setPitch(event.getPitch() + Mth.cos(t * 2.3F) * amp * 0.25F);
    }

    public static void renderVignette(net.minecraft.client.gui.GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        CharacterSheet sheet = ClientCache.self();
        CocClientSettings settings = CocClientSettings.get();
        if (mc.player == null || sheet == null || !settings.sanityEffects || !settings.vignette) return;
        double ratio = sheet.sanRatio();
        boolean insane = sheet.isInsane(currentGameTime(mc));
        boolean low = isLowSan(ratio, settings);
        if (!insane && !low) return;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        double strength = insane ? 0.55 : Math.max(0.15, (0.45 - ratio) * 1.4);
        int alpha = (int) Math.min(190, 60 + strength * 220);
        int edge = Math.min(120, Math.max(40, (int) (h * 0.16)));
        int dark = (alpha << 24);
        int purple = (alpha << 24) | 0x1A0714;
        graphics.fillGradient(0, 0, w, edge, dark, 0x00000000);
        graphics.fillGradient(0, h - edge, w, h, 0x00000000, purple);
        int side = Math.min(90, Math.max(24, w / 16));
        graphics.fillGradient(0, 0, side, h, 0xAA000000, 0x00000000);
        graphics.fillGradient(w - side, 0, w, h, 0x00000000, 0xAA14060F);
    }

    private static void play(Minecraft mc, SoundEvent sound, float volume, float pitch) {
        if (sound == null || mc.getSoundManager() == null) return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private static long currentGameTime(Minecraft mc) {
        return mc.level == null ? 0L : mc.level.getGameTime();
    }
}
