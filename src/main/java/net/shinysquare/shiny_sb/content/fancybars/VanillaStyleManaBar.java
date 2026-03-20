package net.shinysquare.shiny_sb.content.fancybars;

import net.minecraft.Util;
import net.neoforged.fml.common.EventBusSubscriber;
import net.shinysquare.shiny_sb.Config;
import net.shinysquare.shiny_sb.ShinysHypixelSBRemake;
import net.shinysquare.shiny_sb.skyblock.StatusBarTracker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * An alternative mana display that mimics the vanilla food-bar notch style.
 * Active only when fancy bars are explicitly disabled via config
 * (i.e. the player prefers the notch style over the full bars).
 *
 * isOnSkyblock() removed — this mod is standalone and works in any world.
 */
@EventBusSubscriber(modid = ShinysHypixelSBRemake.MOD_ID, value = Dist.CLIENT)
public class VanillaStyleManaBar {

    private static int lastManaValue;
    private static int manaValueBlinkStart;
    private static int lastOverflowValue;
    private static int overflowValueBlinkStart;
    private static long blinkEndTime;

    private static final ResourceLocation MANABAR_FOOD_HUD_ID   = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "vanilla_style_mana_bar_food");
    private static final ResourceLocation MANABAR_MOUNT_HUD_ID  = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "vanilla_style_mana_bar_mount");

    private static final ResourceLocation CONTAINER_TEXTURE              = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "bars/vanilla_mana/container");
    private static final ResourceLocation MANA_FULL_TEXTURE              = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "bars/vanilla_mana/mana_full");
    private static final ResourceLocation MANA_HALF_TEXTURE              = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "bars/vanilla_mana/mana_half");
    private static final ResourceLocation OVERFLOW_FULL_TEXTURE          = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "bars/vanilla_mana/overflow_full");
    private static final ResourceLocation OVERFLOW_HALF_TEXTURE          = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "bars/vanilla_mana/overflow_half");
    private static final ResourceLocation OVERFLOW_DARK_FULL_TEXTURE     = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "bars/vanilla_mana/overflow_dark_full");
    private static final ResourceLocation OVERFLOW_DARK_HALF_TEXTURE     = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "bars/vanilla_mana/overflow_dark_half");
    private static final ResourceLocation CONTAINER_BLINK_TEXTURE        = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "bars/vanilla_mana/container_blink");
    private static final ResourceLocation MANA_FULL_BLINK_TEXTURE        = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "bars/vanilla_mana/mana_full_blink");
    private static final ResourceLocation MANA_HALF_BLINK_TEXTURE        = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "bars/vanilla_mana/mana_half_blink");
    private static final ResourceLocation OVERFLOW_FULL_BLINK_TEXTURE    = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "bars/vanilla_mana/overflow_full_blink");
    private static final ResourceLocation OVERFLOW_HALF_BLINK_TEXTURE    = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "bars/vanilla_mana/overflow_half_blink");
    private static final ResourceLocation OVERFLOW_DARK_FULL_BLINK_TEXTURE  = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "bars/vanilla_mana/overflow_dark_full_blink");
    private static final ResourceLocation OVERFLOW_DARK_HALF_BLINK_TEXTURE  = ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "bars/vanilla_mana/overflow_dark_half_blink");

    enum NotchType { CONTAINER, MANA, OVERFLOW, OVERFLOW_DARK }

    // ── MOD BUS ───────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(VanillaStyleManaBar::onRenderGuiLayerPre);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.FOOD_LEVEL, MANABAR_FOOD_HUD_ID,
                (guiGraphics, deltaTracker) -> { if (isEnabled()) render(guiGraphics); });
        event.registerBelow(VanillaGuiLayers.VEHICLE_HEALTH, MANABAR_MOUNT_HUD_ID,
                (guiGraphics, deltaTracker) -> { if (isEnabled()) render(guiGraphics); });
    }

    // ── NEOFORGE BUS ─────────────────────────────────────────────────────────

    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (!isEnabled()) return;
        ResourceLocation name = event.getName();
        if (name.equals(VanillaGuiLayers.FOOD_LEVEL) || name.equals(VanillaGuiLayers.VEHICLE_HEALTH)) {
            event.setCanceled(true);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Visible when:
     *  - a level is actually loaded (not on the main menu)
     *  - the fancy bars are NOT active (config option)
     *  - the vanilla mana bar is enabled in config
     *
     * isOnSkyblock() is intentionally absent — the mod works standalone.
     */
    private static boolean isEnabled() {
        if (Minecraft.getInstance().level == null) return false;
        // Vanilla-style bar only shows when the player has turned off fancy bars
        return Config.ENABLE_VANILLA_MANA_BAR.get() && !Config.ENABLE_BARS.get();
    }

    private static void drawNotch(GuiGraphics context, int column, int row,
                                   NotchType notchtype, boolean isHalf, boolean isBlinking) {
        int top   = context.guiHeight() - 39;
        int right = context.guiWidth()  / 2 + 91;

        ResourceLocation texture = switch (notchtype) {
            case CONTAINER    -> isBlinking ? CONTAINER_BLINK_TEXTURE : CONTAINER_TEXTURE;
            case MANA         -> !isHalf
                    ? (isBlinking ? MANA_FULL_BLINK_TEXTURE    : MANA_FULL_TEXTURE)
                    : (isBlinking ? MANA_HALF_BLINK_TEXTURE    : MANA_HALF_TEXTURE);
            case OVERFLOW     -> !isHalf
                    ? (isBlinking ? OVERFLOW_FULL_BLINK_TEXTURE : OVERFLOW_FULL_TEXTURE)
                    : (isBlinking ? OVERFLOW_HALF_BLINK_TEXTURE : OVERFLOW_HALF_TEXTURE);
            case OVERFLOW_DARK -> !isHalf
                    ? (isBlinking ? OVERFLOW_DARK_FULL_BLINK_TEXTURE : OVERFLOW_DARK_FULL_TEXTURE)
                    : (isBlinking ? OVERFLOW_DARK_HALF_BLINK_TEXTURE : OVERFLOW_DARK_HALF_TEXTURE);
        };

        context.blitSprite(texture, right - column * 8 - 9, top - row * 10, 9, 9);
    }

    public static boolean render(GuiGraphics context) {
        StatusBarTracker.Resource mana = StatusBarTracker.getMana();

        long currentTime = Util.getMillis();
        final long BLINK_TIME_LENGTH = 1000L;
        final long BLINK_FREQUENCY   = 300L;

        if (lastManaValue + lastOverflowValue > mana.value() + mana.overflow()
                && mana.value() != mana.max()) {
            boolean justStartedBlinking = blinkEndTime <= currentTime;
            if (justStartedBlinking) {
                manaValueBlinkStart     = lastManaValue;
                overflowValueBlinkStart = lastOverflowValue;
            }
            if (blinkEndTime >= currentTime) {
                blinkEndTime = ((currentTime + BLINK_TIME_LENGTH) / BLINK_FREQUENCY * BLINK_FREQUENCY)
                        + blinkEndTime % BLINK_FREQUENCY;
            } else {
                blinkEndTime = currentTime + BLINK_TIME_LENGTH;
            }
        }

        boolean blinking = blinkEndTime > currentTime
                && (blinkEndTime - currentTime) / (BLINK_FREQUENCY / 2) % 2 == 1;

        if (blinkEndTime <= currentTime) {
            manaValueBlinkStart     = 0;
            overflowValueBlinkStart = 0;
        }

        lastManaValue     = mana.value();
        lastOverflowValue = mana.overflow();

        final int MANA_NOTCH_COUNT = 20;
        int manaHalfNotches          = mana.value()             * MANA_NOTCH_COUNT * 2 / mana.max();
        int manaNotches              = (int) Math.ceil(manaHalfNotches             / 2.0);
        int manaBlinkHalfNotches     = manaValueBlinkStart      * MANA_NOTCH_COUNT * 2 / mana.max();
        int manaBlinkNotches         = (int) Math.ceil(manaBlinkHalfNotches        / 2.0);
        int overflowHalfNotches      = mana.overflow()          * MANA_NOTCH_COUNT * 2 / mana.max();
        int overflowNotches          = (int) Math.ceil(overflowHalfNotches         / 2.0);
        int overflowBlinkHalfNotches = overflowValueBlinkStart  * MANA_NOTCH_COUNT * 2 / mana.max();
        int overflowBlinkNotches     = (int) Math.ceil(overflowBlinkHalfNotches    / 2.0);

        for (int i = 0; i < MANA_NOTCH_COUNT; i++) {
            int row    = i / 10;
            int column = i % 10;

            boolean manaNotch              = i < manaNotches;
            boolean manaNotchIsHalf        = manaNotch        && manaNotches        - 1 == i && manaHalfNotches        % 2 == 1;
            boolean manaBlinkNotch         = i < manaBlinkNotches;
            boolean manaBlinkNotchIsHalf   = manaBlinkNotch   && manaBlinkNotches   - 1 == i && manaBlinkHalfNotches   % 2 == 1;
            boolean overflowNotch          = i < overflowNotches;
            boolean overflowNotchIsHalf    = overflowNotch    && overflowNotches    - 1 == i && overflowHalfNotches    % 2 == 1;
            boolean overflowBlinkNotch     = i < overflowBlinkNotches;
            boolean overflowBlinkNotchIsHalf = overflowBlinkNotch && overflowBlinkNotches - 1 == i && overflowBlinkHalfNotches % 2 == 1;

            drawNotch(context, column, row, NotchType.CONTAINER, false, blinking);

            if (manaNotches > 0) {
                if (overflowNotch)              drawNotch(context, column, row, NotchType.OVERFLOW_DARK, overflowNotchIsHalf, blinking);
                if (manaBlinkNotch && blinking) drawNotch(context, column, row, NotchType.MANA, manaBlinkNotchIsHalf, true);
                if (manaNotch)                  drawNotch(context, column, row, NotchType.MANA, manaNotchIsHalf, false);
            } else {
                if (manaBlinkNotch && blinking)     drawNotch(context, column, row, NotchType.MANA,     manaBlinkNotchIsHalf,     true);
                if (overflowBlinkNotch && blinking) drawNotch(context, column, row, NotchType.OVERFLOW, overflowBlinkNotchIsHalf, true);
                if (overflowNotch)                  drawNotch(context, column, row, NotchType.OVERFLOW, overflowNotchIsHalf,      false);
            }
        }
        return true;
    }
}
