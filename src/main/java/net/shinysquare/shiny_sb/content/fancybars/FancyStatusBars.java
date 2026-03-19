package net.shinysquare.shiny_sb.content.fancybars;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.shinysquare.shiny_sb.ShinysHypixelSBRemake;
import net.shinysquare.shiny_sb.skyblock.StatusBarTracker;
import net.shinysquare.shiny_sb.content.utils.Utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.VisibleForTesting;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Fancy status bars — ALWAYS active whenever a level is loaded.
 *
 * There is no config toggle. The vanilla health, armour, food, air, and XP bars
 * are unconditionally replaced by the fancy bars the moment a world is entered.
 *
 * The config screen (/shsbm bars) still exists so players can reposition, resize,
 * and recolor individual bars — it just no longer controls whether bars show at all.
 */
public class FancyStatusBars {

    private static final ResourceLocation HUD_LAYER =
            ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "fancy_status_bars");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path FILE;

    private static final Logger LOGGER = LoggerFactory.getLogger(FancyStatusBars.class);

    public static BarPositioner barPositioner = new BarPositioner();
    public static Map<StatusBarType, StatusBar> statusBars = new EnumMap<>(StatusBarType.class);
    private static boolean updatePositionsNextFrame;
    private static boolean hasUpdatedPositions = false;

    public static boolean isHealthFancyBarEnabled()     { return isBarEnabled(StatusBarType.HEALTH); }
    public static boolean isExperienceFancyBarEnabled() { return isBarEnabled(StatusBarType.EXPERIENCE); }

    public static boolean isBarEnabled(StatusBarType type) {
        StatusBar bar = statusBars.get(type);
        return bar != null && (bar.enabled || bar.inMouse);
    }

    // ── MOD BUS ───────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        FILE = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get()
                .resolve(ShinysHypixelSBRemake.MOD_ID)
                .resolve("status_bars.json");
        try { Files.createDirectories(FILE.getParent()); } catch (IOException ignored) {}

        statusBars.put(StatusBarType.HEALTH,       StatusBarType.HEALTH.newStatusBar());
        statusBars.put(StatusBarType.INTELLIGENCE, StatusBarType.INTELLIGENCE.newStatusBar());
        statusBars.put(StatusBarType.DEFENSE,      StatusBarType.DEFENSE.newStatusBar());
        statusBars.put(StatusBarType.EXPERIENCE,   StatusBarType.EXPERIENCE.newStatusBar());
        statusBars.put(StatusBarType.SPEED,        StatusBarType.SPEED.newStatusBar());
        statusBars.put(StatusBarType.AIR,          StatusBarType.AIR.newStatusBar());

        applyDefaultPositions();

        CompletableFuture.supplyAsync(FancyStatusBars::loadBarConfig, Executors.newVirtualThreadPerTaskExecutor())
                .thenAccept(object -> {
                    if (object != null) {
                        for (String key : object.keySet()) {
                            try {
                                StatusBarType type = StatusBarType.from(key);
                                if (statusBars.containsKey(type))
                                    statusBars.get(type).loadFromJson(object.get(key).getAsJsonObject());
                            } catch (Exception e) {
                                LOGGER.error("[shsbm] Failed to load {} status bar", key, e);
                            }
                        }
                    }
                    placeBarsInPositioner();
                    LOGGER.info("[shsbm] Bars placed, configLoaded = true");
                    configLoaded = true;
                })
                .exceptionally(t -> { LOGGER.error("[shsbm] Failed reading status bars config", t); return null; });

        NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent e) -> saveBarConfig());
        NeoForge.EVENT_BUS.addListener(FancyStatusBars::onRenderGuiLayerPre);
        NeoForge.EVENT_BUS.addListener(FancyStatusBars::onRegisterClientCommands);
    }

    /**
     * Registers the fancy bars HUD layer immediately after the hotbar.
     * No isEnabled() gate — bars always render when a level is loaded.
     */
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, (guiGraphics, deltaTracker) -> {
            if (configLoaded && !hasUpdatedPositions) {
                updatePositions(true);
                hasUpdatedPositions = true;
            }
            render(guiGraphics, Minecraft.getInstance());
        });
    }

    // ── NEOFORGE BUS ─────────────────────────────────────────────────────────

    /**
     * Unconditionally cancels all vanilla HUD layers that the fancy bars replace.
     * Only skips when no level is loaded (main menu, etc.).
     */
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (Minecraft.getInstance().level == null) return;

        ResourceLocation name = event.getName();
        if (name.equals(VanillaGuiLayers.PLAYER_HEALTH))                                      { event.setCanceled(true); }
        else if (name.equals(VanillaGuiLayers.EXPERIENCE_LEVEL) && isExperienceFancyBarEnabled()) { event.setCanceled(true); }
        else if (name.equals(VanillaGuiLayers.EXPERIENCE_BAR)   && isExperienceFancyBarEnabled()) { event.setCanceled(true); }
        else if (name.equals(VanillaGuiLayers.ARMOR_LEVEL))                                   { event.setCanceled(true); }
        else if (name.equals(VanillaGuiLayers.VEHICLE_HEALTH))                                  { event.setCanceled(true); }
        else if (name.equals(VanillaGuiLayers.FOOD_LEVEL))                                    { event.setCanceled(true); }
        else if (name.equals(VanillaGuiLayers.AIR_LEVEL))                                     { event.setCanceled(true); }
    }

    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal(ShinysHypixelSBRemake.MOD_ID)
                        .then(Commands.literal("bars").executes(ctx -> {
                            Minecraft.getInstance().execute(
                                    () -> Minecraft.getInstance().setScreen(new StatusBarsConfigScreen()));
                            return 1;
                        })));
    }

    // ── Default positions ─────────────────────────────────────────────────────

    /**
     * Sensible defaults before the saved config is read.
     *
     *   HOTBAR_TOP row 0 : HEALTH, INTELLIGENCE (mana)
     *   HOTBAR_TOP row 1 : EXPERIENCE (/ rift time)
     *   HOTBAR_RIGHT     : DEFENSE, SPEED
     *   HOTBAR_RIGHT     : AIR (hidden until underwater)
     */
    private static void applyDefaultPositions() {
        StatusBar health = statusBars.get(StatusBarType.HEALTH);
        health.anchor = BarPositioner.BarAnchor.HOTBAR_TOP; health.gridY = 0; health.gridX = 0; health.enabled = true;

        StatusBar intel = statusBars.get(StatusBarType.INTELLIGENCE);
        intel.anchor = BarPositioner.BarAnchor.HOTBAR_TOP; intel.gridY = 0; intel.gridX = 1; intel.enabled = true;

        StatusBar exp = statusBars.get(StatusBarType.EXPERIENCE);
        exp.anchor = BarPositioner.BarAnchor.HOTBAR_TOP; exp.gridY = 1; exp.gridX = 0; exp.enabled = true;

        StatusBar def = statusBars.get(StatusBarType.DEFENSE);
        def.anchor = BarPositioner.BarAnchor.HOTBAR_RIGHT; def.gridY = 0; def.gridX = 0; def.enabled = true;

        StatusBar spd = statusBars.get(StatusBarType.SPEED);
        spd.anchor = BarPositioner.BarAnchor.HOTBAR_RIGHT; spd.gridY = 0; spd.gridX = 1; spd.enabled = true;

        StatusBar air = statusBars.get(StatusBarType.AIR);
        air.anchor = BarPositioner.BarAnchor.HOTBAR_RIGHT; air.gridY = 1; air.gridX = 0;
        air.enabled = false; // shown dynamically when underwater
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private static boolean configLoaded = false;

    @VisibleForTesting
    public static void placeBarsInPositioner() {
        barPositioner.clear();
        for (BarPositioner.BarAnchor anchor : BarPositioner.BarAnchor.allAnchors()) {
            List<StatusBar> barList = statusBars.values().stream()
                    .filter(b -> b.anchor == anchor)
                    .sorted(Comparator.<StatusBar>comparingInt(b -> b.gridY).thenComparingInt(b -> b.gridX))
                    .toList();
            if (barList.isEmpty()) continue;

            int y = -1, rowNum = -1;
            for (StatusBar bar : barList) {
                if (bar.gridY > y) { barPositioner.addRow(anchor); rowNum++; y = bar.gridY; }
                barPositioner.addBar(anchor, rowNum, bar);
            }
        }
    }

    public static @Nullable JsonObject loadBarConfig() {
        try (BufferedReader r = Files.newBufferedReader(FILE)) {
            return GSON.fromJson(r, JsonObject.class);
        } catch (NoSuchFileException e) {
            LOGGER.warn("[shsbm] No status bar config found, using defaults");
        } catch (Exception e) {
            LOGGER.error("[shsbm] Failed to load status bars config", e);
        }
        return null;
    }

    public static void saveBarConfig() {
        JsonObject out = new JsonObject();
        statusBars.forEach((t, bar) -> out.add(t.getSerializedName(), bar.toJson()));
        try (BufferedWriter w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(out, w);
        } catch (IOException e) {
            LOGGER.error("[shsbm] Failed to save status bars config", e);
        }
    }

    public static void updatePositions(boolean ignoreVisibility) {
        if (!configLoaded) return;
        final int W = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        final int H = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        int offset = 0;
        for (StatusBar bar : statusBars.values()) {
            if (!bar.enabled) {
                bar.setX(5); bar.setY(50 + offset); bar.setWidth(30);
                offset += bar.getHeight();
            } else if (bar.anchor == null) {
                bar.width = Math.clamp(bar.width, 30f / W, 1);
                bar.x     = Math.clamp(bar.x, 0, 1 - bar.width);
                bar.y     = Math.clamp(bar.y, 0, 1 - (float) bar.getHeight() / H);
                bar.setX((int) (bar.x * W)); bar.setY((int) (bar.y * H));
                bar.setWidth((int) (bar.width * W));
            }
        }

        for (BarPositioner.BarAnchor anchor : BarPositioner.BarAnchor.allAnchors()) {
            ScreenPosition pos  = anchor.getAnchorPosition(W, H);
            BarPositioner.SizeRule rule = anchor.getSizeRule();
            int targetSize = rule.targetSize();
            boolean healthMoved = anchor == BarPositioner.BarAnchor.HOTBAR_TOP && !isHealthFancyBarEnabled();
            if (healthMoved) targetSize /= 2;

            if (rule.isTargetSize()) {
                for (int row = 0; row < barPositioner.getRowCount(anchor); row++) {
                    LinkedList<StatusBar> barRow = barPositioner.getRow(anchor, row);
                    if (barRow.isEmpty()) continue;
                    int total = 0;
                    for (StatusBar b : barRow) total += (b.size = Math.clamp(b.size, rule.minSize(), rule.maxSize()));
                    whileLoop:
                    while (total != targetSize) {
                        if (total > targetSize) {
                            for (StatusBar b : barRow) { if (b.size > rule.minSize()) { b.size--; total--; if (total == targetSize) break whileLoop; } }
                        } else {
                            for (StatusBar b : barRow) { if (b.size < rule.maxSize()) { b.size++; total++; if (total == targetSize) break whileLoop; } }
                        }
                    }
                }
            }

            int row = 0;
            for (int i = 0; i < barPositioner.getRowCount(anchor); i++) {
                List<StatusBar> barRow = new ArrayList<>(barPositioner.getRow(anchor, i));
                barRow.removeIf(b -> !b.visible && !ignoreVisibility);
                if (barRow.isEmpty()) continue;

                float wps = rule.isTargetSize()
                        ? (float) rule.totalWidth() / barRow.stream().mapToInt(b -> b.size).sum()
                        : rule.widthPerSize();
                if (healthMoved) wps /= 2;

                int currSize = 0, rowSize = barRow.size();
                for (int j = 0; j < rowSize; j++) {
                    int offX = 0, less = 0;
                    if (!rule.isTargetSize())  { offX = 1; less = 2; }
                    else if (rowSize > 1) {
                        if      (j == 0)           { less = 1; }
                        else if (j == rowSize - 1) { less = 1; offX = 1; }
                        else                       { less = 2; offX = 1; }
                    }
                    StatusBar b = barRow.get(j);
                    b.size = Math.clamp(b.size, rule.minSize(), rule.maxSize());
                    float x = anchor.isRight()
                            ? pos.x() + (healthMoved ? rule.totalWidth() / 2f : 0) + currSize * wps
                            : pos.x() - currSize * wps - b.size * wps;
                    b.setX(Mth.ceil(x) + offX);
                    b.setY(anchor.isUp()
                            ? pos.y() - (row + 1) * (b.getHeight() + 1)
                            : pos.y() + row * (b.getHeight() + 1));
                    b.setWidth(Mth.floor(b.size * wps) - less);
                    currSize += b.size;
                }
                if (currSize > 0) row++;
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public static boolean render(GuiGraphics context, Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return false;

        // Bars first, text on top
        for (StatusBar bar : statusBars.values()) {
            if (bar.enabled && bar.visible) bar.renderBar(context);
        }
        for (StatusBar bar : statusBars.values()) {
            if (bar.enabled && bar.visible) bar.renderText(context);
        }

        // ── Health ────────────────────────────────────────────────────────────
        if (Utils.isInTheRift()) {
            // The Rift: vanilla HP used directly (no Skyblock scaling)
            int hp    = Math.round(player.getHealth());
            int maxHp = Math.round(player.getMaxHealth());
            statusBars.get(StatusBarType.HEALTH).updateValues(hp / (float) maxHp, 0, hp, maxHp, null);
            statusBars.get(StatusBarType.DEFENSE).visible = false;
        } else {
            statusBars.get(StatusBarType.HEALTH).updateWithResource(StatusBarTracker.getHealth());
            int def = StatusBarTracker.getDefense();
            StatusBar defBar = statusBars.get(StatusBarType.DEFENSE);
            defBar.visible = true;
            defBar.updateValues(def / (def + 100f), 0, def, null, null);
        }

        // ── Mana (INTELLIGENCE bar) ───────────────────────────────────────────
        // Mana and overflow mana come entirely from StatusBarTracker.
        // To spend mana: call StatusBarTracker.addMana(-cost)
        // To set it:     call StatusBarTracker.setMana(value, max, overflow)
        StatusBarTracker.Resource mana = StatusBarTracker.getMana();
        statusBars.get(StatusBarType.INTELLIGENCE).updateValues(
                mana.value() / (float) mana.max(),
                mana.overflow() / (float) mana.max(),
                mana.value(), mana.max(),
                mana.overflow() > 0 ? mana.overflow() : null);

        // ── Speed ─────────────────────────────────────────────────────────────
        statusBars.get(StatusBarType.SPEED).updateWithResource(StatusBarTracker.getSpeed());

        // ── Experience / Rift time ────────────────────────────────────────────
        if (Utils.isInTheRift()) {
            int riftSecs = StatusBarTracker.getRiftTime();
            // Fill = seconds / 10 minutes (600s). Falls back to vanilla XP progress if unset.
            float fill = riftSecs > 0 ? Math.clamp(riftSecs / 600f, 0f, 1f) : player.experienceProgress;
            statusBars.get(StatusBarType.EXPERIENCE).updateValues(fill, 0, riftSecs > 0 ? riftSecs : player.experienceLevel, null, null);
        } else {
            statusBars.get(StatusBarType.EXPERIENCE).updateValues(player.experienceProgress, 0, player.experienceLevel, null, null);
        }

        // ── Air ───────────────────────────────────────────────────────────────
        StatusBar airBar = statusBars.get(StatusBarType.AIR);
        airBar.updateWithResource(StatusBarTracker.getAir());
        boolean shouldShowAir = player.isUnderWater();
        if (shouldShowAir != airBar.visible) {
            airBar.visible = shouldShowAir;
            updatePositionsNextFrame = true;
        }

        if (updatePositionsNextFrame) {
            updatePositions(false);
            updatePositionsNextFrame = false;
        }
        return true;
    }
}
